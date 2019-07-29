package org.elm.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmImportClause
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.endOffset
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.*
import org.elm.openapiext.runWriteCommandAction

class MakeEncoderIntention : ElmAtCaretIntentionActionBase<MakeEncoderIntention.Context>() {

    data class Context(val file: ElmFile, val ty: Ty, val name: String, val endOffset: Int)

    override fun getText() = "Generate Encoder"
    override fun getFamilyName() = text

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        val file = element.containingFile as? ElmFile ?: return null
        val typeAnnotation = element.parentOfType<ElmTypeAnnotation>()
                ?: return null

        if (typeAnnotation.reference.resolve() != null) {
            // the target declaration already exists; nothing to do
            return null
        }

        val ty = typeAnnotation.typeExpressionInference()?.ty as? TyFunction ?: return null
        val param = ty.parameters.singleOrNull() ?: return null
        val ret = ty.ret as? TyUnion ?: return null
        if (ret.module != "Json.Encode" || ret.name != "Value") return null
        return Context(file, param, typeAnnotation.referenceName, typeAnnotation.endOffset)
    }

    override fun invoke(project: Project, editor: Editor, context: Context) {
        val encoder = EncoderGenerator(context.file, context.ty, context.name)
        project.runWriteCommandAction {
            editor.document.insertString(context.endOffset, encoder.code)
            if (encoder.imports.isNotEmpty()) {
                // Commit the string changes so we can work with the new PSI
                PsiDocumentManager.getInstance(context.file.project).commitDocument(editor.document)
                for (import in encoder.imports) {
                    ImportAdder.addImportForCandidate(import, context.file, import.nameToBeExposed.isEmpty())
                }
            }
        }
    }
}

private data class GeneratedFunction(
        val name: String,
        val paramTy: Ty,
        val paramName: String,
        val body: String,
        val qualifier: String
)

private data class Ref(val module: String, val name: String)

private fun TyUnion.toRef() = Ref(module, name)
private fun AliasInfo.toRef() = Ref(module, name)
private fun DeclarationInTy.toRef() = Ref(module, name)

private class EncoderGenerator(
        private val file: ElmFile,
        private val root: Ty,
        private val functionName: String
) {
    /** All types and aliases referenced in the root ty */
    private val declarations by lazy { root.allDeclarations().toList() }
    /** Additional encoder functions to generate */
    private val funcsByTy = mutableMapOf<Ty, GeneratedFunction>()
    /** Unions that need their variants exposed. */
    private val unionsToExpose = mutableSetOf<Ref>()
    /** Counter used to prevent name collision of generated functions */
    private var i = 1

    /** Code to insert after the type annotation */
    val code by lazy {
        val genBody = gen(root)
        val rootFunc = funcsByTy[root]
        funcsByTy.remove(root)
        val param = rootFunc?.paramName ?: root.renderParam()
        val body = rootFunc?.body ?: "    $genBody $param"

        buildString {
            append("\n$functionName $param =\n")
            append(body)

            for (f in funcsByTy.values) {
                append("\n\n\n")
                append("${f.name} : ${f.qualifier}${f.paramTy.renderedText(false, false)} -> Encode.Value\n")
                append("${f.name} ${f.paramName} =\n")
                append(f.body)
            }
        }
    }

    /** Imports to add because they are referenced by generated code. */
    val imports by lazy {
        val visibleTypes = ModuleScope.getVisibleTypes(file).all
                .mapNotNullTo(mutableSetOf()) { it.name?.let { n -> Ref(it.moduleName, n) } }
        val visibleModules = importedModules + setOf("", "List")
        declarations
                .filter {
                    it.module != moduleName &&
                            (it.toRef() in unionsToExpose || it.module !in visibleModules && it.toRef() !in visibleTypes)
                }
                .map {
                    Candidate(
                            moduleName = it.module,
                            moduleAlias = null,
                            nameToBeExposed = if (it.isUnion) "${it.name}(..)" else ""
                    )
                }
    }

    /** The name of the module in the current file */
    private val moduleName by lazy { file.getModuleDecl()?.name ?: "" }


    /** The name to use for the encoder function for each type (does not include the "encoder" prefix) */
    // There might be multiple types with the same name in different modules, so add the module
    // name the function for any type with a conflict that isn't defined in this module
    private val funcNames by lazy {
        declarations.groupBy { it.name }
                .map { (_, decls) ->
                    decls.map {
                        it.toRef() to when {
                            decls.size == 1 -> it.name
                            else -> it.module.replace(".", "") + it.name
                        }
                    }
                }.flatten().toMap()
    }

    /** Module qualifier prefixes to add to type names (does not include types not already imported) */
    private val typeQualifiers by lazy {
        // If this ends up being slow, we might be able to speed it up by caching
        // ImportScope.getExposedTypes
        declarations.associate {
            it.toRef() to ModuleScope.getQualifierForTypeName(file, it.module, it.name)
        }
    }

    /** Qualified names of all imported modules */
    private val importedModules: Set<String> by lazy {
        file.findChildrenByClass(ElmImportClause::class.java).mapTo(mutableSetOf()) { it.referenceName }
    }

    /** Get the module qualifier prefix to add to a type name */
    private fun qualifierFor(ref: Ref): String {
        return when (ref.module) {
            moduleName -> ""
            // We always fully qualify types from modules that we add imports for
            !in importedModules -> "${ref.module}."
            else -> typeQualifiers[ref] ?: ""
        }
    }

    /** Return a unary callable expression that will encode [ty] */
    private fun gen(ty: Ty): String = when (ty) {
        is TyRecord -> generateRecordFunc(ty).name
        is TyUnion -> generateUnion(ty)
        is TyVar -> "Debug.todo \"Can't generate encoders for type variables\""
        is TyTuple -> generateTuple(ty)
        is TyUnit -> "(\\_ -> Encode.null)"
        is TyFunction, TyInProgressBinding, is MutableTyRecord, is TyUnknown -> {
            "Debug.todo \"Can't generate encoder for type ${ty.renderedText(false, false)}\""
        }
    }

    private fun generateUnion(ty: TyUnion): String = when {
        ty.isTyInt -> "Encode.int"
        ty.isTyFloat -> "Encode.float"
        ty.isTyBool -> "Encode.bool"
        ty.isTyString -> "Encode.string"
        ty.isTyChar -> "(String.fromChar >> Encode.string)"
        ty.isTyList -> "Encode.list ${gen(ty.parameters[0])}"
        ty.module == "Set" && ty.name == "Set" -> "Encode.set ${gen(ty.parameters[0])}"
        ty.module == "Array" && ty.name == "Array" -> "Encode.array ${gen(ty.parameters[0])}"
        ty.module == "Maybe" && ty.name == "Maybe" -> "(Maybe.map ${gen(ty.parameters[0])} >> Maybe.withDefault Encode.null)"
        ty.module == "Dict" && ty.name == "Dict" -> generateDict(ty)
        else -> generateUnionFunc(ty).name
    }


    private fun generateRecordFunc(ty: TyRecord): GeneratedFunction {
        if (ty in funcsByTy) return funcsByTy[ty]!!

        val name = "encode${ty.alias?.let { funcNames[it.toRef()] } ?: "Record${i++}"}"
        val param = ty.renderParam()
        val qualifier = ty.alias?.let { qualifierFor(it.toRef()) } ?: ""
        val body = buildString {
            append("    Encode.object <|\n        [ ")
            ty.fields.entries.joinTo(this, separator = "\n        , ") { (k, v) ->
                "( \"$k\", ${gen(v)} $param.$k )"
            }
            append("\n        ]")
        }

        val func = GeneratedFunction(name = name, paramTy = ty, paramName = param, body = body, qualifier = qualifier)
        funcsByTy[ty] = func
        return func
    }

    private fun generateUnionFunc(ty: TyUnion): GeneratedFunction {
        if (ty in funcsByTy) return funcsByTy[ty]!!

        val name = "encode${ty.alias?.let { funcNames[it.toRef()] } ?: ty.name}"
        val param = ty.renderParam()
        val decl = ElmLookup.findByFileAndTy(file, ty)
        val body = if (decl == null) {
            "Debug.todo \"Can't generate encoder for ${ty.name}\""
        } else {
            unionsToExpose += ty.toRef()
            val variants = decl.variantInference().value
            buildString {
                append("    case $param of\n")

                val singleOrNull = variants.values.singleOrNull()
                if (singleOrNull?.size == 1) {
                    // For type wrappers like Time.Posix, encode their wrapped type
                    val (k, v) = variants.entries.single()
                    val p = v[0].renderParam()
                    append("""
                    |        $k $p ->
                    |            ${gen(v[0])} $p
                    """.trimMargin())
                } else {
                    // Otherwise treat them as enums. This is invalid code for variants with
                    // parameters, but it's a helpful starting point.
                    variants.keys.joinTo(this, separator = "\n\n") { variant ->
                        """
                        |        $variant ->
                        |            Encode.string "$variant"
                        """.trimMargin()
                    }
                }
            }
        }
        val func = GeneratedFunction(name = name, paramTy = ty, paramName = param, body = body, qualifier = qualifierFor(ty.toRef()))
        funcsByTy[ty] = func
        return func
    }

    private fun generateDict(ty: TyUnion): String {
        val k = ty.parameters[0]
        return if (k !is TyUnion || !k.isTyString) {
            "(\\_ -> Debug.todo \"Can't generate encoder for Dict with non-String keys\")"
        } else {
            "(Dict.toList >> List.map (\\( k, v ) -> ( k, ${gen(ty.parameters[1])} v )) >> Encode.object)"
        }
    }

    private fun generateTuple(ty: TyTuple): String {
        return if (ty.types.size == 2) {
            "(\\( a, b ) -> Encode.list identity [ ${gen(ty.types[0])} a, ${gen(ty.types[1])} b ])"
        } else {
            "(\\( a, b, c ) -> Encode.list identity [ ${gen(ty.types[0])} a, ${gen(ty.types[1])} b, ${gen(ty.types[2])} c ])"
        }
    }
}
