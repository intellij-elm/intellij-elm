package org.elm.ide.intentions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmTypeAnnotation
import org.elm.lang.core.psi.endOffset
import org.elm.lang.core.psi.parentOfType
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.*

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
        val declarations = context.ty.allDeclarations().toList()
        val code = EncoderGenerator.generate(context.file, context.ty, context.name, declarations)
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            editor.document.insertString(context.endOffset, code)
        }
    }

    // TODO: call this
    private fun imports(file: ElmFile, declarations: List<DeclarationInTy>): List<Candidate> {
        val moduleName = file.getModuleDecl()?.name ?: ""
        val visibleTypes = ModuleScope.getVisibleTypes(file).all
                .mapNotNullTo(mutableSetOf()) { it.name?.let { n -> Ref(it.moduleName, n) } }
        return declarations
                .filter { it.module != moduleName && it.toRef() !in visibleTypes }
                .map {
                    Candidate(
                            moduleName = it.name,
                            moduleAlias = null,
                            nameToBeExposed = if (it.isUnion) "${it.name}(..)" else it.name
                    )
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

private class EncoderGenerator private constructor(
        private val file: ElmFile,
        private val funcNames: Map<Ref, String>,
        private val typeQualifiers: Map<Ref, String?>
) {
    companion object {
        fun generate(file: ElmFile, root: Ty, name: String, declarations: List<DeclarationInTy>): String {
            val funcNames = funcNames(declarations)
            val typeQualifiers = typeQualifiers(file, declarations)

            val generator = EncoderGenerator(file, funcNames, typeQualifiers)
            val genBody = generator.gen(root)
            val rootFunc = generator.funcsByTy[root]
            generator.funcsByTy.remove(root)
            val param = rootFunc?.paramName ?: root.renderParam()
            val body = rootFunc?.body ?: "    $genBody $param"

            return buildString {
                append("\n$name $param =\n")
                append(body)

                for (f in generator.funcsByTy.values) {
                    append("\n\n\n")
                    append("${f.name} : ${f.qualifier}${f.paramTy.renderedText(false, false)} -> Encode.Value\n")
                    append("${f.name} ${f.paramName} =\n")
                    append(f.body)
                }
            }
        }

        // There might be multiple types with the same name in different modules, so add the module
        // name the function for any type with a conflict that isn't defined in this module
        private fun funcNames(declarations: List<DeclarationInTy>): Map<Ref, String> {
            return declarations.groupBy { it.name }
                    .map { (_, decls) ->
                        decls.map {
                            it.toRef() to when {
                                decls.size == 1 -> it.name
                                else -> it.module.replace(".", "") + it.name
                            }
                        }
                    }.flatten().toMap()
        }

        private fun typeQualifiers(file: ElmFile, declarations: List<DeclarationInTy>): Map<Ref, String?> {
            // If this ends of being slow, we might be able to speed it up by caching
            // ImportScope.getExposedTypes
            return declarations.associate {
                it.toRef() to ModuleScope.getQualifierForTypeName(file, it.module, it.name)
            }
        }
    }

    private val funcsByTy = mutableMapOf<Ty, GeneratedFunction>()
    private var i = 1

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
        val qualifier = ty.alias?.let { typeQualifiers[it.toRef()] } ?: ""
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
        val qualifier = typeQualifiers[ty.toRef()] ?: ""
        val body = if (decl == null) {
            "Debug.todo \"Can't generate encoder for ${ty.name}\""
        } else {
            val variants = decl.variantInference().value
            buildString {
                append("    case $param of\n")

                variants.keys.joinTo(this, separator = "\n\n") { variant ->
                    """
                    |        $variant ->
                    |            Encode.string "$variant"
                    """.trimMargin()
                }
            }
        }
        val func = GeneratedFunction(name = name, paramTy = ty, paramName = param, body = body, qualifier = qualifier)
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
