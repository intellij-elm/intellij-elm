package org.elm.ide.intentions

import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.psi.elements.*
import org.elm.lang.core.resolve.scope.ImportScope
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.*

class MakeEncoderIntention : BaseTyGeneratorIntention() {
    override fun getText() = "Generate Encoder"

    override fun getRootIfApplicable(annotationTy: Ty): Ty? {
        if (annotationTy !is TyFunction) return null
        val param = annotationTy.parameters.singleOrNull() ?: return null
        val ret = annotationTy.ret as? TyUnion ?: return null
        if (ret.module != "Json.Encode" || ret.name != "Value") return null
        return param
    }

    override fun generator(context: Context): TyFunctionGenerator {
        return EncoderGenerator(context.file, context.ty, context.name)
    }
}

private class EncoderGenerator(
        file: ElmFile,
        root: Ty,
        functionName: String
): TyFunctionGenerator(file, root, functionName) {
    /** Counter used to prevent name collision of generated functions */
    protected var i = 1

    override val code by lazy {
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

    override val imports by lazy {
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

    /** Return a unary callable expression that will encode [ty] */
    private fun gen(ty: Ty): String = when (ty) {
        is TyRecord -> generateRecord(ty)
        is TyUnion -> generateUnion(ty)
        is TyVar -> "(\\_ -> Debug.todo \"Can't generate encoders for type variables\")"
        is TyTuple -> generateTuple(ty)
        is TyUnit -> "(\\_ -> Encode.null)"
        is TyFunction, TyInProgressBinding, is MutableTyRecord, is TyUnknown -> {
            "(\\_ -> Debug.todo \"Can't generate encoder for type ${ty.renderedText(false, false)}\")"
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
        else -> generateUnionFunc(ty)
    }

    private fun generateUnionFunc(ty: TyUnion): String {
        val cached = findExistingEncoder(ty, ty.toRef()) ?: funcsByTy[ty]?.name
        if (cached != null) return cached

        val renderedTy = ty.renderParam()
        val decl: ElmTypeDeclaration? = ElmLookup.findFirstByNameAndModule(ty.name, ty.module, file)
        val (param, body) = if (decl == null) {
            renderedTy to "Debug.todo \"Can't generate encoder for ${ty.name}\""
        } else {
            unionsToExpose += ty.toRef()
            val variants = decl.variantInference().value
            val patternsAndExprs = variants.map { (variant, params) ->
                val pattern = (listOf(variant) + params.map { it.renderParam() }).joinToString(" ")
                val expr = when (params.size) {
                    0 -> "Encode.string \"$variant\""
                    1 -> "${gen(params[0])} ${params[0].renderParam()}"
                    else -> "Debug.todo \"Cannot generate encoder for variant with multiple parameters\""
                }
                pattern to expr
            }
            if (variants.size == 1) {
                // For a single variant, we can pattern match it in the function parameter
                "(${patternsAndExprs[0].first})" to "    ${patternsAndExprs[0].second}"
            } else {
                // Otherwise we have to use a case expression
                renderedTy to patternsAndExprs.joinToString("\n\n", prefix = "    case $renderedTy of\n") { (pattern, expr) ->
                    """
                    |        $pattern ->
                    |            $expr
                    """.trimMargin()
                }
            }
        }

        val name = "encode${ty.alias?.let { funcNames[it.toRef()] } ?: ty.name}"
        val func = GeneratedFunction(name = name, paramTy = ty, paramName = param, body = body, qualifier = qualifierFor(ty.toRef()))
        funcsByTy[ty] = func
        return name
    }

    private fun generateRecord(ty: TyRecord): String {
        val cached = ty.alias?.let { findExistingEncoder(ty, it.toRef()) } ?: funcsByTy[ty]?.name
        if (cached != null) return cached

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
        return name
    }

    private fun findExistingEncoder(ty: Ty, ref: Ref): String? {
        if (ty in encodersByTy) return encodersByTy[ty]!!
        val declaration = ElmLookup.findByNameAndModule<ElmNamedElement>(ref.name, ref.module, file)
                .firstOrNull { it is ElmTypeDeclaration || it is ElmTypeAliasDeclaration } ?: return null

        val possibleValues =
                ModuleScope.getVisibleValues(file).all + ImportScope(declaration.elmFile).getExposedValues()

        possibleValues
                .filterIsInstance<ElmFunctionDeclarationLeft>()
                .forEach {
                    val t = it.findTy()
                    if (t is TyFunction &&
                            t.parameters == listOf(ty) &&
                            t.ret is TyUnion &&
                            t.ret.module == "Json.Encode" &&
                            t.ret.name == "Value") {
                        val code = qualifierFor(ref) + it.name
                        encodersByTy[ty] = code
                        return code
                    }
                }
        return null
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
