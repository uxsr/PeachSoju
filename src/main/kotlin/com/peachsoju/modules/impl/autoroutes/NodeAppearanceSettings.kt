package com.peachsoju.modules.impl.autoroutes

import com.peachsoju.config
import com.peachsoju.modules.impl.autoroutes.data.WPType
import com.odtheking.odin.utils.Color

enum class RenderStyle(val displayName: String) {
    PULSE_PYRAMID("Pulse Pyramid"),
    WIREFRAME("Wireframe"),
    FILLED("Filled"),
    CORNER_BOX("Corner Box"),
    DASHED("Dashed"),
    DIAMOND("Diamond"),
    PULSE_BOX("Pulse Box"),
    X_BOX("X Box");

    companion object {
        fun fromString(str: String): RenderStyle =
            entries.find { it.name.equals(str, ignoreCase = true) } ?: PULSE_PYRAMID
    }
}

object NodeAppearanceSettings {

    fun getColor(type: WPType): Color = config.getNodeAppearance(type).let { Color(it.color.r, it.color.g, it.color.b, it.color.a) }

    fun getStyle(type: WPType): RenderStyle = RenderStyle.fromString(config.getNodeAppearance(type).style)

    fun setColor(type: WPType, r: Int, g: Int, b: Int, a: Float) = config.setNodeColor(type, r, g, b, a)

    fun setStyle(type: WPType, style: RenderStyle) = config.setNodeStyle(type, style.name)

    fun resetToDefaults() = config.resetNodeAppearances()
}
