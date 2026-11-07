package com.baccarat.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reads the six-row Big Road on the supplied portrait Stake Speed Baccarat A screen.
 *
 * The previous calibration was based on a different crop/scale and scanned above
 * the actual roadmap. These normalized coordinates are calibrated to the latest
 * 917x2048 screenshot: first bead centre ~= (45, 736), horizontal pitch ~= 27.6,
 * vertical pitch ~= 29.3. Coordinates scale with the captured display dimensions.
 *
 * This reader reports only visible Player/Banker circles. A tie is a green mark
 * over an existing Big Road bead, not a separate new bead, so it is not counted
 * as an independent chronological outcome here.
 */
object RoadmapRecognizer {
    data class Bead(val column: Int, val row: Int, val result: String)

    fun read(bitmap: Bitmap): List<Bead> {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return emptyList()

        // Latest supplied screen: Big Road occupies x~34..744, y~718..899.
        val firstCenterX = width * 0.0491f
        val firstCenterY = height * 0.3594f
        val columnStep = width * 0.0301f
        val rowStep = height * 0.0143f
        val radius = width * 0.0104f
        val output = ArrayList<Bead>()

        for (column in 0 until 26) {
            for (row in 0 until 6) {
                val cx = (firstCenterX + column * columnStep).toInt()
                val cy = (firstCenterY + row * rowStep).toInt()
                var blue = 0
                var red = 0
                for (angle in 0 until 360 step 12) {
                    val radians = Math.toRadians(angle.toDouble())
                    val x = (cx + radius * cos(radians)).toInt()
                    val y = (cy + radius * sin(radians)).toInt()
                    if (x !in 0 until width || y !in 0 until height) continue
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    if (b > 115 && b > r * 1.18 && b > g * 1.02) blue++
                    if (r > 120 && r > b * 1.18 && r > g * 1.02) red++
                }
                val total = blue + red
                if (total >= 8) {
                    val dominant = maxOf(blue, red)
                    if (dominant >= 6 && dominant.toFloat() / total >= 0.65f) {
                        output.add(Bead(column, row, if (blue > red) "P" else "B"))
                    }
                }
            }
        }
        return output.sortedWith(compareBy<Bead> { it.column }.thenBy { it.row })
    }
}
