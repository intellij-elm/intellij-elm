package org.elm.ide.intentions

import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.types.*

class MakeEncoderIntention : AnnotationBasedGeneratorIntention() {
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
        private val functionName: String
) : TyFunctionGenerator(file, root) {
    /** Counter used to prevent name collision of generated functions */
    private var i = 1

    override fun generateCode(): String {
        // run gen on the root to kick off generation
        val genBody = gen(root)
        val rootFunc = funcsByTy[root]
        funcsByTy.remove(root)
        val param = rootFunc?.paramName ?: root.renderParam()
        val body = rootFunc?.body ?: "    $genBody $param"

        return buildString {
            append("\n$functionName $param =\n")
            append(body)

            for (f in funcsByTy.values) {
                append("\n\n\n")
                append("-- TODO: double-check generated code\n")
                append("${f.name} : ${f.qualifier}${f.paramTy.renderedText()} -> ${qual("Value")}\n")
                append("${f.name} ${f.paramName} =\n")
                append(f.body)
            }
        }
    }

    /** Return a unary callable expression that will encode [ty] */
    private fun gen(ty: Ty): String = when (ty) {
        is TyRecord -> generateRecord(ty)
        is TyUnion -> generateUnion(ty)
        is TyVar -> "(\\_ -> Debug.todo \"Can't generate encoders for type variables\")"
        is TyTuple -> generateTuple(ty)
        is TyUnit -> "(\\_ -> ${qual("null")})"
        is TyFunction, TyInProgressBinding, is MutableTyRecord, is TyUnknown -> {
            "(\\_ -> Debug.todo \"Can't generate encoder for type ${ty.renderedText()}\")"
        }
    }

    private fun generateUnion(ty: TyUnion): String = when {
        ty.isTyInt -> qual("int")
        ty.isTyFloat -> qual("float")
        ty.isTyBool -> qual("bool")
        ty.isTyString -> qual("string")
        ty.isTyChar -> "(${qual("String", "fromChar")} >> ${qual("string")})"
        ty.isTyList -> existing(ty) ?: "${qual("list")} ${gen(ty.parameters[0])}"
        ty.module == "Set" && ty.name == "Set" -> existing(ty) ?: "${qual("set")} ${gen(ty.parameters[0])}"
        ty.module == "Array" && ty.name == "Array" -> existing(ty)
                ?: "${qual("array")} ${gen(ty.parameters[0])}"
        ty.module == "Maybe" && ty.name == "Maybe" -> generateMaybe(ty)
        ty.module == "Dict" && ty.name == "Dict" -> generateDict(ty)
        else -> generateUnionFunc(ty)
    }

    private fun generateUnionFunc(ty: TyUnion): String {
        val cached = existing(ty) ?: funcsByTy[ty]?.name
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
                    0 -> "${qual("string")} \"$variant\""
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
        val cached = ty.alias?.let { existing(ty) } ?: funcsByTy[ty]?.name
        if (cached != null) return cached

        val name = "encode${ty.alias?.let { funcNames[it.toRef()] } ?: "Record${i++}"}"
        val param = ty.renderParam()
        val qualifier = ty.alias?.let { qualifierFor(it.toRef()) } ?: ""
        val body = buildString {
            append("    ${qual("object")} <|\n        [ ")
            ty.fields.entries.joinTo(this, separator = "\n        , ") { (k, v) ->
                "( \"$k\", ${gen(v)} $param.$k )"
            }
            append("\n        ]")
        }

        val func = GeneratedFunction(name = name, paramTy = ty, paramName = param, body = body, qualifier = qualifier)
        funcsByTy[ty] = func
        return name
    }

    private fun generateDict(ty: TyUnion): String {
        val k = ty.parameters[0]
        return if (k !is TyUnion || !k.isTyString) {
            "(\\_ -> Debug.todo \"Can't generate encoder for Dict with non-String keys\")"
        } else {
            "(${qual("Dict", "toList")} >> ${qual("List", "map")} (\\( k, v ) -> ( k, ${gen(ty.parameters[1])} v )) >> ${qual("object")})"
        }
    }

    private fun generateTuple(ty: TyTuple): String {
        return if (ty.types.size == 2) {
            "(\\( a, b ) -> ${qual("list")} identity [ ${gen(ty.types[0])} a, ${gen(ty.types[1])} b ])"
        } else {
            "(\\( a, b, c ) -> ${qual("list")} identity [ ${gen(ty.types[0])} a, ${gen(ty.types[1])} b, ${gen(ty.types[2])} c ])"
        }
    }

    private fun generateMaybe(ty: TyUnion): String {
        return existing(ty) ?: run {
            "(${qual("Maybe", "map")} ${gen(ty.parameters[0])} >> ${qual("Maybe", "withDefault")} ${qual("null")})"
        }
    }

    override fun isExistingFunction(needle: Ty, function: Ty): Boolean {
        return function.run {
            this is TyFunction &&
                    parameters == listOf(needle) &&
                    ret is TyUnion &&
                    ret.module == "Json.Encode" &&
                    ret.name == "Value"
        }
    }

    private fun qual(name: String) = qual("Json.Encode", name)
}
