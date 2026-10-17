package com.drdisagree.colorblendr.utils.colors

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.drdisagree.colorblendr.data.enums.MONET
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Цветовая система nyashell (github.com/K2nek1ken/nyashell), перенесённая из шелла один в один:
 *  1) из цвета-источника (главный цвет обоев) по схеме строятся цвета шелла — акцент, второй цвет и фон
 *     (Wallpaper.qml → build): оттенок и насыщенность в HSL, у каждой схемы своя яркость акцента,
 *     сдвиг второго цвета и окрашенность фонов;
 *  2) из них — тональная палитра Material You (Palette.js): 5 столбцов × 13 оттенков, оттенок 0…1000 —
 *     это светлота L* в CIELCh (как «тон» у Material), цвета вне sRGB мягко теряют насыщенность.
 * Accent 1 — акцент, Accent 2 — второй цвет, Accent 3 — третичный (оттенок акцента +60°), Neutral 1–2 — фоны.
 *
 * The nyashell colour system (github.com/K2nek1ken/nyashell), ported from the shell one to one:
 *  1) from the source colour (the wallpaper's main colour) the shell colours are built per scheme: the accent,
 *     the second colour and the background (Wallpaper.qml → build): hue and saturation in HSL, each scheme has its
 *     own accent brightness, second-colour shift and background tint;
 *  2) from those, a Material You tonal palette (Palette.js): 5 columns × 13 shades, shade 0…1000 is
 *     lightness L* in CIELCh (like Material's "tone"); colours outside sRGB gently lose chroma.
 * Accent 1 is the accent, Accent 2 the second colour, Accent 3 the tertiary (accent hue +60°), Neutral 1–2 the backgrounds.
 */
object NyaPalette {

    // оттенки Android (0, 10, 50, 100 … 1000) → светлота L* / Android shades (0, 10, 50, 100 … 1000) → lightness L*
    private val TONES = doubleArrayOf(100.0, 99.0, 95.0, 90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 30.0, 20.0, 10.0, 0.0)

    fun isNya(style: MONET): Boolean = style.name.startsWith("NYA_")

    // имя схемы шелла для стиля / the shell scheme name for a style
    private fun scheme(style: MONET): String = when (style) {
        MONET.NYA_NEUTRAL -> "neutral"
        MONET.NYA_VIBRANT -> "vibrant"
        MONET.NYA_EXPRESSIVE -> "expressive"
        MONET.NYA_FIDELITY -> "fidelity"
        MONET.NYA_CONTENT -> "content"
        MONET.NYA_RAINBOW -> "rainbow"
        MONET.NYA_FRUIT_SALAD -> "fruit"
        else -> "tonal"
    }

    /**
     * Пять палитр по 13 цветов (Accent 1–3, Neutral 1–2). secondColor — второй главный цвет обоев
     * (для «точной» и «содержательной» схем), null — оттенок акцента +60°.
     * Five palettes of 13 colours (Accent 1–3, Neutral 1–2). secondColor is the wallpaper's second main colour
     * (for the "fidelity" and "content" schemes); null means the accent hue +60°.
     */
    fun generate(style: MONET, @ColorInt seed: Int, @ColorInt secondColor: Int?): ArrayList<ArrayList<Int>> {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seed, hsl)
        val h = hsl[0].toDouble()
        val s = hsl[1].toDouble()
        val h2 = secondColor?.let {
            val o = FloatArray(3); ColorUtils.colorToHSL(it, o); o[0].toDouble()
        } ?: (h + 60.0)

        val shell = shellColors(h, s, h2, scheme(style))
        val a1 = lch(shell.accent)
        val a2 = lch(shell.accent2)
        val n = lch(shell.surface)
        val nc = min(8.0, n.c)

        return arrayListOf(
            ramp(a1.h, max(a1.c, 8.0)),
            ramp(a2.h, max(a2.c, 6.0)),
            ramp((a1.h + 60.0) % 360.0, max(a2.c, 8.0)),
            ramp(n.h, nc),
            ramp(n.h, min(12.0, nc * 1.6 + 1.0))
        )
    }

    // ─── 1) цвета шелла (Wallpaper.qml → build) / the shell colours (Wallpaper.qml → build) ───
    private data class Shell(val accent: Int, val accent2: Int, val surface: Int)

    private class Spec(val a: Double, val a2h: Double, val a2s: Double, val n: Double, val ah: Double? = null, val nh: Double? = null)

    private fun shellColors(h: Double, s: Double, h2: Double, scheme: String): Shell {
        val base = max(0.22, min(0.6, s * 0.8))
        val c = when (scheme) {
            "neutral" -> Spec(0.14, h + 20, 0.10, 0.04)
            "vibrant" -> Spec(0.78, h + 25, 0.62, 0.16)
            "expressive" -> Spec(0.55, h + 60, 0.5, 0.14, ah = h + 240, nh = h + 15)
            "fidelity" -> Spec(max(0.35, min(0.85, s)), h2, 0.45, 0.10)
            "content" -> Spec(max(0.3, min(0.75, s)), h2, 0.5, 0.12)
            "rainbow" -> Spec(0.5, h + 120, 0.5, 0.0)
            "fruit" -> Spec(0.55, h + 70, 0.55, 0.10, ah = h - 50)
            else -> Spec(base, h + 35, base * 0.7, 0.12)
        }
        val ah = c.ah ?: h
        val nh = c.nh ?: h
        return Shell(
            accent = hsl(ah, c.a, 0.80),
            accent2 = hsl(c.a2h, c.a2s, 0.76),
            surface = hsl(nh, c.n * 1.1, 0.135)
        )
    }

    private fun hsl(h: Double, s: Double, l: Double): Int {
        val hh = (((h % 360.0) + 360.0) % 360.0).toFloat()
        return ColorUtils.HSLToColor(floatArrayOf(hh, s.coerceIn(0.0, 1.0).toFloat(), l.coerceIn(0.0, 1.0).toFloat()))
    }

    // ─── 2) тональная палитра в CIELCh (Palette.js) / the tonal palette in CIELCh (Palette.js) ───
    private data class Lch(val l: Double, val c: Double, val h: Double)

    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883

    private fun lin(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun gam(c: Double): Double = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1.0 / 2.4) - 0.055
    private fun f(t: Double): Double = if (t > 216.0 / 24389.0) Math.cbrt(t) else (24389.0 / 27.0 * t + 16.0) / 116.0
    private fun fi(t: Double): Double = if (t * t * t > 216.0 / 24389.0) t * t * t else (116.0 * t - 16.0) / (24389.0 / 27.0)

    private fun lch(color: Int): Lch {
        val r = lin(Color.red(color))
        val g = lin(Color.green(color))
        val b = lin(Color.blue(color))
        val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / XN
        val y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / YN
        val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / ZN
        val l = 116.0 * f(y) - 16.0
        val aa = 500.0 * (f(x) - f(y))
        val bb = 200.0 * (f(y) - f(z))
        return Lch(l, sqrt(aa * aa + bb * bb), (atan2(bb, aa) * 180.0 / PI + 360.0) % 360.0)
    }

    private fun rgbOf(l: Double, c: Double, h: Double): DoubleArray {
        val a = c * cos(h * PI / 180.0)
        val b = c * sin(h * PI / 180.0)
        val fy = (l + 16.0) / 116.0
        val x = XN * fi(fy + a / 500.0)
        val y = YN * fi(fy)
        val z = ZN * fi(fy - b / 200.0)
        return doubleArrayOf(
            3.2406 * x - 1.5372 * y - 0.4986 * z,
            -0.9689 * x + 1.8758 * y + 0.0415 * z,
            0.0557 * x - 0.2040 * y + 1.0570 * z
        )
    }

    private fun inGamut(v: DoubleArray): Boolean = v.all { it >= -0.0001 && it <= 1.0001 }

    // не влезает в sRGB — уменьшаем насыщенность, пока не влезет / doesn't fit in sRGB: lower the chroma until it does
    private fun fromLch(l: Double, c: Double, h: Double): Int {
        var v = rgbOf(l, c, h)
        if (!inGamut(v)) {
            var lo = 0.0
            var hi = c
            repeat(18) {
                val m = (lo + hi) / 2.0
                if (inGamut(rgbOf(l, m, h))) lo = m else hi = m
            }
            v = rgbOf(l, lo, h)
        }
        val ch = v.map { (gam(max(0.0, it)).coerceIn(0.0, 1.0) * 255.0).roundToInt() }
        return Color.rgb(ch[0], ch[1], ch[2])
    }

    private fun ramp(h: Double, c: Double): ArrayList<Int> {
        val out = ArrayList<Int>(TONES.size)
        for (t in TONES) out.add(fromLch(t, if (t == 100.0 || t == 0.0) 0.0 else c, h))
        return out
    }

    // второй главный цвет обоев: первый из списка, который хотя бы на 30° отличается от источника по оттенку
    // the wallpaper's second main colour: the first in the list at least 30° away from the source in hue
    fun secondColor(@ColorInt seed: Int, wallpaperColors: List<Int>): Int? {
        val a = FloatArray(3); ColorUtils.colorToHSL(seed, a)
        return wallpaperColors.firstOrNull {
            val b = FloatArray(3); ColorUtils.colorToHSL(it, b)
            val d = abs(a[0] - b[0]) % 360f
            min(d, 360f - d) >= 30f && b[1] > 0.08f
        }
    }
}
