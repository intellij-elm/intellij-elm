package org.elm.ide.intentions

import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmTypeDeclaration
import org.elm.lang.core.resolve.scope.ModuleScope
import org.elm.lang.core.types.*

class MakeDecoderIntention : BaseTyGeneratorIntention() {
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
) : TyFunctionGenerator(file, root, functionName) {
    /** Code to insert after the type annotation */
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
                // TODO these are the only two lines that are different
                append("${f.name} : Decoder ${f.qualifier}${f.paramTy.renderedText(false, false)}\n")
                append("${f.name} =\n")
                append(f.body)
            }
        }
    }

    /** Imports to add because they are referenced by generated code. */
    override val imports by lazy {
        // TODO this is identical
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
        is TyVar -> "(Debug.todo \"Can't generate decoders for type variables\")"
        is TyTuple -> generateTuple(ty)
        is TyUnit -> "(Decode.succeed ())"
        is TyFunction, TyInProgressBinding, is MutableTyRecord, is TyUnknown -> {
            "(Debug.todo \"Can't generate encoder for type ${ty.renderedText(false, false)}\")"
        }
    }


    // TODO
    private fun generateUnion(ty: TyUnion): String = when {
        ty.isTyInt -> "Decode.int"
        ty.isTyFloat -> "Decode.float"
        ty.isTyBool -> "Decode.bool"
        ty.isTyString -> "Decode.string"
        ty.isTyChar -> "(String.fromChar >> Decode.string)"
        ty.isTyList -> "(Decode.list ${gen(ty.parameters[0])})"
        ty.module == "Set" && ty.name == "Set" -> "(Decode.map Set.fromList (Decode.list ${gen(ty.parameters[0])}))"
        ty.module == "Array" && ty.name == "Array" -> "(Decode.array ${gen(ty.parameters[0])})"
        ty.module == "Maybe" && ty.name == "Maybe" -> "(Decode.nullable ${gen(ty.parameters[0])})"
        ty.module == "Dict" && ty.name == "Dict" -> generateDict(ty)
        else -> generateUnionFunc(ty)
    }

    private fun generateRecord(ty: TyRecord): String {
        val cached = /*findExistingEncoder(ty, ty.toRef()) ?:*/ funcsByTy[ty]?.name
        // TODO      ^
        if (cached != null) return cached

        val alias = ty.alias ?: return "Debug.todo \"Cannot decode records without aliases\""
        val qualifier = qualifierFor(alias.toRef())
        val name = "decode${funcNames[alias.toRef()]}"
        val body = buildString {
            append("    Decode.succeed $qualifier${alias.name}")
            // TODO import `required`
            for ((field, fieldTy) in ty.fields) {
                append("\n        |> required \"$field\" ${gen(fieldTy)}")
            }
        }
        val func = GeneratedFunction(name = name, paramTy = ty, paramName = "", body = body, qualifier = qualifier)
        funcsByTy[ty] = func
        return name
    }

    private fun generateUnionFunc(ty: TyUnion): String {
        val cached = /*findExistingEncoder(ty, ty.toRef()) ?:*/ funcsByTy[ty]?.name
        // TODO      ^
        if (cached != null) return cached

        val decl: ElmTypeDeclaration? = ElmLookup.findFirstByNameAndModule(ty.name, ty.module, file)
        val body = if (decl == null) {
            "Debug.todo \"Can't generate decoder for ${ty.name}\""
        } else {
            unionsToExpose += ty.toRef()
            val variants = decl.variantInference().value
            if (variants.size == 1 && variants.values.first().size == 1) {
                "    Decode.map ${variants.keys.first()} ${gen(variants.values.first().first())}"
            } else {
                val branches = variants.entries.joinToString("\n\n                ") { (variant, params) ->
                    val expr = when {
                        params.isEmpty() -> "Decode.succeed $variant"
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
                |                    Decode.fail ("unknown value for ${ty.renderedText(false, false)}: " ++ id)
                |     in
                |        Decode.string |> Decode.andThen get
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
            "(Decode.dict ${gen(ty.parameters[1])})"
        }
    }

    private fun generateTuple(ty: TyTuple): String {
        val decoders = ty.types.mapIndexed { i, it -> "(Decode.index $i ${gen(it)})" }.joinToString(" ")
        return if (ty.types.size == 2) {
            "(Decode.map2 Tuple.pair $decoders)"
        } else {
            "(Decode.map3 (\\a b c -> (a, b, c)) $decoders)"
        }
    }
}
