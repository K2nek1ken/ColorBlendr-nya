package com.drdisagree.colorblendr.data.enums

import android.os.Build
import com.drdisagree.colorblendr.R

enum class MONET {
    SPRITZ,
    MONOCHROMATIC,
    TONAL_SPOT,
    VIBRANT,
    RAINBOW,
    EXPRESSIVE,
    FIDELITY,
    CONTENT,
    FRUIT_SALAD,
    CMF,

    // схемы nyashell (utils/colors/NyaPalette.kt): палитра строится нашим алгоритмом и ставится оверлеем, поэтому — только root
    // nyashell schemes (utils/colors/NyaPalette.kt): the palette is built by our algorithm and applied as an overlay, so root only
    NYA_TONAL,
    NYA_NEUTRAL,
    NYA_VIBRANT,
    NYA_EXPRESSIVE,
    NYA_FIDELITY,
    NYA_CONTENT,
    NYA_RAINBOW,
    NYA_FRUIT_SALAD;

    override fun toString(): String {
        return name
    }

    fun isAvailable(rootMode: Boolean, colorSpecVersion: Int): Boolean {
        val a13 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val a14 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        return when (this) {
            TONAL_SPOT -> true
            SPRITZ, VIBRANT, RAINBOW, EXPRESSIVE, FRUIT_SALAD -> a13 || rootMode
            MONOCHROMATIC -> a14 || rootMode
            FIDELITY, CONTENT -> rootMode
            CMF -> rootMode && colorSpecVersion == 2
            NYA_TONAL, NYA_NEUTRAL, NYA_VIBRANT, NYA_EXPRESSIVE,
            NYA_FIDELITY, NYA_CONTENT, NYA_RAINBOW, NYA_FRUIT_SALAD -> rootMode
        }
    }

    fun disabledReason(rootMode: Boolean, colorSpecVersion: Int): Int {
        if (isAvailable(rootMode, colorSpecVersion)) return 0

        return when (this) {
            MONOCHROMATIC -> R.string.android_14_required
            FIDELITY, CONTENT -> R.string.root_required
            NYA_TONAL, NYA_NEUTRAL, NYA_VIBRANT, NYA_EXPRESSIVE,
            NYA_FIDELITY, NYA_CONTENT, NYA_RAINBOW, NYA_FRUIT_SALAD -> R.string.root_required
            CMF -> if (!rootMode) R.string.root_required
            else R.string.colorspec_2026_required

            else -> R.string.android_13_required
        }
    }

    companion object {
        fun String?.toEnumMonet(): MONET {
            return entries.find { it.name.equals(this, ignoreCase = true) } ?: TONAL_SPOT
        }
    }
}