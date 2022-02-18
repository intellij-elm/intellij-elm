package org.elm.ide.color

import com.github.ajalt.colormath.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.elm.lang.core.psi.ElmPsiFactory
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.ElmTypes.REGULAR_STRING_PART
import org.elm.lang.core.psi.elementType
import org.elm.lang.core.psi.elements.*
import java.awt.Color
import kotlin.math.roundToInt

private val colorRegex = Regex("""#[0-9a-fA-F]{3,8}\b|\b(?:rgb|hsl)a?\([^)]+\)""")

/** Adds color blocks to the gutter when hex colors exist in a string */
class ElmColorProvider : ElementColorProvider {
    override fun getColorFrom(element: PsiElement): Color? {
        // Like all line markers, we should only provide colors on leaf elements
        if (element.firstChild != null) return null
        return getCssColorFromString(element)
            ?: getColorFromFuncCall(element)
    }

    // Parse a CSS color from any string that contains one, since "1px solid #1a2b3c" probably
    // contains a color. We don't parse color keywords, since "The red fire truck" is probably not
    // supposed to contain a color.
    private fun getCssColorFromString(element: PsiElement): Color? {
        if (element.elementType != REGULAR_STRING_PART) return null
        return colorRegex.find(element.text)
            ?.let { runCatching { ConvertibleColor.fromCss(it.value) }.getOrNull() }
            ?.toAwtColor()
    }

    private fun getColorFromFuncCall(element: PsiElement): Color? {
        val call = getFuncCall(element) ?: return null
        val color = runCatching {
            // color constructors will throw if the args are out of bounds
            when (call.name) {
                "rgb", "rgba" -> {
                    if (call.a == null && call.name == "rgba") return null
                    if (call.useFloat) RGB(call.c1, call.c2, call.c3, call.a ?: 1f)
                    else RGB(call.c1.toInt(), call.c2.toInt(), call.c3.toInt(), call.a ?: 1f)
                }
                "rgb255" -> RGB(call.c1.toInt(), call.c2.toInt(), call.c3.toInt())
                "rgba255" -> RGB(call.c1.toInt(), call.c2.toInt(), call.c3.toInt(), call.a ?: return null)
                "hsl" -> HSL(call.c1, call.c2, call.c3)
                "hsla" -> HSL(call.c1, call.c2, call.c3, call.a ?: return null)
                else -> return null
            }
        }.getOrNull()
        return color?.toAwtColor()
    }

    private fun getFuncCall(element: PsiElement): FuncCall? {
        if (element.elementType != LOWER_CASE_IDENTIFIER) return null
        if (element.parent !is ElmValueQID) return null

        val valueExpr = element.parent.parent as? ElmValueExpr ?: return null
        val callExpr = valueExpr.parent as? ElmFunctionCallExpr ?: return null
        if (callExpr.target != valueExpr) return null
        if (valueExpr.referenceName !in listOf("rgb", "rgba", "hsl", "hsla", "rgb255", "rgba255")) return null

        val args = callExpr.arguments.toList()
        if ((args.size != 3 && args.size != 4) || args.any { it !is ElmNumberConstantExpr }) return null

        @Suppress("UNCHECKED_CAST")
        args as List<ElmNumberConstantExpr>

        return FuncCall(
            c1 = args[0].text.toFloatOrNull() ?: return null,
            c2 = args[1].text.toFloatOrNull() ?: return null,
            c3 = args[2].text.toFloatOrNull() ?: return null,
            // the alpha channel is optional
            a = args.getOrNull(3)?.text?.let { it.toFloatOrNull() ?: return null },
            name = valueExpr.referenceName,
            containsFloats = args.take(3).any { it.isFloat },
            target = valueExpr,
            args = args
        )
    }

    override fun setColorTo(element: PsiElement, color: Color) {
        if (element.firstChild != null) return
        val command = stringColorSettingRunnable(element, color)
            ?: functionColorSettingRunnable(element, color)
            ?: return

        val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
        CommandProcessor.getInstance().executeCommand(
            element.project,
            command,
            // This is the message that the JavaColorProvider uses, value copied from JavaBundle.properties
            "Change color",
            null, // groupId
            document
        )
    }

    private fun functionColorSettingRunnable(element: PsiElement, color: Color): Runnable? {
        val funcCall = getFuncCall(element)
        val call = funcCall ?: return null
        return Runnable { setColorInFunctionCall(element, color, call) }
    }

    private fun setColorInFunctionCall(element: PsiElement, color: Color, call: FuncCall) {
        val factory = ElmPsiFactory(element.project)

        fun ElmNumberConstantExpr.replace(c: Int, float: Boolean) {
            val rendered = if (float) (c / 255f).render() else c.toString()
            replace(factory.createNumberConstant(rendered))
        }

        if (call.name.startsWith("hsl")) {
            val hsl = color.toRGB().toHSL()
            call.args[0].replace(factory.createNumberConstant(hsl.h.toFloat().render()))
            call.args[1].replace(factory.createNumberConstant((hsl.s / 100f).render()))
            call.args[2].replace(factory.createNumberConstant((hsl.l / 100f).render()))
        } else {
            call.args[0].replace(color.red, call.useFloat)
            call.args[1].replace(color.green, call.useFloat)
            call.args[2].replace(color.blue, call.useFloat)
        }

        call.args.getOrNull(3)?.replace(color.alpha, true)
    }

    private fun stringColorSettingRunnable(element: PsiElement, color: Color): Runnable? {
        if (element.elementType != REGULAR_STRING_PART) return null
        return Runnable { setCssColorInString(element, color) }
    }

    private fun setCssColorInString(element: PsiElement, color: Color) {
        val parent = element.parent as? ElmStringConstantExpr ?: return
        val match = colorRegex.find(element.text)?.value ?: return

        val rgb = color.toRGB()
        val percentCount = match.count { it == '%' }
        val commas = ',' in match

        val newColor = when {
            match.startsWith("#") -> rgb.toHex()
            match.startsWith("rgb") -> rgb.toCssRgb(
                commas = commas,
                namedRgba = match.startsWith("rgba"),
                rgbPercent = percentCount > 1,
                alphaPercent = percentCount == 1 || percentCount == 4
            )
            match.startsWith("hsl") -> rgb.toCssHsl(
                commas = commas,
                namedHsla = match.startsWith("hsla"),
                hueUnit = when {
                    "deg" in match -> AngleUnit.DEGREES
                    "grad" in match -> AngleUnit.GRADIANS
                    "rad" in match -> AngleUnit.RADIANS
                    "turn" in match -> AngleUnit.TURNS
                    else -> AngleUnit.AUTO
                },
                alphaPercent = percentCount == 1 || percentCount == 3
            )
            else -> return
        }

        val factory = ElmPsiFactory(element.project)
        val newText = colorRegex.replaceFirst(parent.text, newColor)
        val newElement = factory.createStringConstant(newText)
        parent.replace(newElement)
    }
}

private data class FuncCall(
    val c1: Float,
    val c2: Float,
    val c3: Float,
    val a: Float?,
    val name: String,
    private val containsFloats: Boolean,
    val target: ElmValueExpr,
    val args: List<ElmNumberConstantExpr>
) {
    val colors = listOf(c1, c2, c3)
    val useFloat = when {
        containsFloats -> true
        colors.all { it == 0f } -> false // literal `0` can be used for both
        colors.any { it > 1 } -> false
        else -> true // Args are a mix of `0` and `1`. Assume a float, but we could be wrong.
    }
}

fun com.github.ajalt.colormath.Color.toAwtColor(): Color = toRGB().let {
    Color(it.r, it.g, it.b, (it.a * 255).roundToInt())
}

private fun Color.toRGB() = RGB(red, green, blue, alpha / 255f)

private fun Float.render(): String = when (this) {
    0f -> "0"
    1f -> "1"
    else -> String.format("%.4f", this).trimEnd('0').trimEnd('.')
}
