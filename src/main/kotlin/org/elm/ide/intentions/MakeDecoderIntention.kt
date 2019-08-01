package org.elm.ide.intentions

import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.types.*

class MakeDecoderIntention : AnnotationBasedGeneratorIntention() {
    override fun getText() = "Generate Decoder"

    override fun getRootIfApplicable(annotationTy: Ty): Ty? {
        if (annotationTy !is TyUnion) return null
        if (annotationTy.module != "Json.Decode" || annotationTy.name != "Decoder") return null
        return annotationTy.parameters.singleOrNull()
    }

    override fun generator(context: Context): TyFunctionGenerator {
        return DecoderGenerator(context.file, context.ty, context.name)
    }
}

private class DecoderGenerator(
        file: ElmFile,
        root: Ty,
        functionName: String
) : TyFunctionGenerator(file, root) {
    /** true if we need to import Json.Decode.Pipeline */
    private var usedPipeline = false

    override val code by lazy {
        // run gen on the root to kick off generation
        val genBody = gen(root)
        val rootFunc = funcsByTy[root]
        funcsByTy.remove(root)
        val body = rootFunc?.body ?: "    $genBody"

        buildString {
            append("\n$functionName =\n")
            append(body)

            for (f in funcsByTy.values) {
                append("\n\n\n")
                append("${f.name} : ${qual("Decoder")} ${f.qualifier}${f.paramTy.renderedText(false, false)}\n")
                append("${f.name} =\n")
                append(f.body)
            }
        }
    }

    override fun calculateImports(): List<Candidate> {
        return when {
            usedPipeline -> super.calculateImports() + listOf(
                    Candidate(
                            moduleName = "Json.Decode.Pipeline",
                            moduleAlias = null,
                            nameToBeExposed = "required"
                    )
            )
            else -> super.calculateImports()
        }
    }

    private fun gen(ty: Ty): String = when (ty) {
        is TyRecord -> generateRecord(ty)
        is TyUnion -> generateUnion(ty)
        is TyVar -> "(Debug.todo \"Can't generate decoders for type variables\")"
        is TyTuple -> generateTuple(ty)
        is TyUnit -> "(${qual("succeed")} ())"
        is TyFunction, TyInProgressBinding, is MutableTyRecord, is TyUnknown -> {
            "(Debug.todo \"Can't generate encoder for type ${ty.renderedText(false, false)}\")"
        }
    }

    private fun generateUnion(ty: TyUnion): String = when {
        ty.isTyInt -> qual("int")
        ty.isTyFloat -> qual("float")
        ty.isTyBool -> qual("bool")
        ty.isTyString -> qual("string")
        ty.isTyChar -> "(${qual("string")} |> ${qual("map")} (String.toList >> List.head >> Maybe.withDefault '?'))"
        ty.isTyList -> "(${qual("list")} ${gen(ty.parameters[0])})"
        ty.module == "Set" && ty.name == "Set" -> "(${qual("map")} Set.fromList (${qual("list")} ${gen(ty.parameters[0])}))"
        ty.module == "Array" && ty.name == "Array" -> "(${qual("array")} ${gen(ty.parameters[0])})"
        ty.module == "Maybe" && ty.name == "Maybe" -> "(${qual("nullable")} ${gen(ty.parameters[0])})"
        ty.module == "Dict" && ty.name == "Dict" -> generateDict(ty)
        else -> generateUnionFunc(ty)
    }

    private fun generateRecord(ty: TyRecord): String {
        val alias = ty.alias ?: return "(Debug.todo \"Cannot decode records without aliases\")"
        val cached = findExistingFunction(ty) ?: funcsByTy[ty]?.name
        if (cached != null) return cached

        val qualifier = qualifierFor(alias.toRef())
        val name = "decode${funcNames[alias.toRef()]}"
        val body = buildString {
            append("    ${qual("succeed")} $qualifier${alias.name}")
            for ((field, fieldTy) in ty.fields) {
                append("\n        |> required \"$field\" ${gen(fieldTy)}")
            }
        }
        val func = GeneratedFunction(name = name, paramTy = ty, paramName = "", body = body, qualifier = qualifier)
        funcsByTy[ty] = func
        usedPipeline = true
        return name
    }

    private fun generateUnionFunc(ty: TyUnion): String {
        val cached = findExistingFunction(ty) ?: funcsByTy[ty]?.name
        if (cached != null) return cached

        val decl: ElmTypeDeclaration? = ElmLookup.findFirstByNameAndModule(ty.name, ty.module, file)
        val body = if (decl == null) {
            "Debug.todo \"Can't generate decoder for ${ty.name}\""
        } else {
            unionsToExpose += ty.toRef()
            val variants = decl.variantInference().value
            if (variants.size == 1 && variants.values.first().size == 1) {
                "    ${qual("map")} ${variants.keys.first()} ${gen(variants.values.first().first())}"
            } else {
                val branches = variants.entries.joinToString("\n\n                ") { (variant, params) ->
                    val expr = when {
                        params.isEmpty() -> "Decode.succeed ${qual(ty.module, variant)}"
                        else -> "Debug.todo \"Cannot decode variant with params: $variant\""
                    }
                    """
                    |"$variant" ->
                    |                    $expr
                    """.trimMargin()
                }
                """
                |    let
                |        get id =
                |            case id of
                |                $branches
                |
                |                _ ->
                |                    ${qual("fail")} ("unknown value for ${ty.renderedText(false, false)}: " ++ id)
                |    in
                |    ${qual("string")} |> ${qual("andThen")} get
                """.trimMargin()
            }
        }

        val name = "decode${ty.alias?.let { funcNames[it.toRef()] } ?: ty.name}"
        val func = GeneratedFunction(name = name, paramTy = ty, paramName = "", body = body, qualifier = qualifierFor(ty.toRef()))
        funcsByTy[ty] = func
        return name
    }

    private fun generateDict(ty: TyUnion): String {
        val k = ty.parameters[0]
        return if (k !is TyUnion || !k.isTyString) {
            "(\\_ -> Debug.todo \"Can't generate encoder for Dict with non-String keys\")"
        } else {
            "(${qual("dict")} ${gen(ty.parameters[1])})"
        }
    }

    private fun generateTuple(ty: TyTuple): String {
        val decoders = ty.types.mapIndexed { i, it -> "(Decode.index $i ${gen(it)})" }.joinToString(" ")
        return if (ty.types.size == 2) {
            "(${qual("map2")} ${qual("Tuple", "pair")} $decoders)"
        } else {
            "(${qual("map3")} (\\a b c -> ( a, b, c )) $decoders)"
        }
    }

    override fun isExistingFunction(needle: Ty, function: Ty): Boolean {
        return function.run {
            this is TyUnion &&
                    module == "Json.Decode" &&
                    name == "Decoder" &&
                    parameters.singleOrNull() == needle
        }
    }

    private fun qual(name: String) = qual("Json.Decode", name)
}
