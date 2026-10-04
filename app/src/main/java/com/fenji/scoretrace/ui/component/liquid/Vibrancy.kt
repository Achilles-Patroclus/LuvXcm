// Vendored from KernelSU (tiann/KernelSU), originally adapted from Kyant0/AndroidLiquidGlass (Apache 2.0).
package com.fenji.scoretrace.ui.component.liquid

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls

fun BackdropEffectScope.vibrancy() {
    colorControls(
        brightness = 0f,
        contrast = 1f,
        saturation = 1.5f,
    )
}
