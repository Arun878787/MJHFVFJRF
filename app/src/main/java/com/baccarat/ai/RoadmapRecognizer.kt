package com.baccarat.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Portrait-layout reader calibrated against the supplied Stake Baccarat A recording.
 * Reads the six-row Big Road only; it ignores non-red/blue pixels and ambiguous cells.
 */
object RoadmapRecognizer {
    data class Bead(val column: Int, val row: Int, val result: String)

    fun read(bitmap: Bitmap): List<Bead> {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return emptyList()

        // Reference recording: 572x1280. Scale all measurements proportionally.
        val firstCenterX = width * 0.058f
        val firstCenterY = height * 0.306f
        val columnStep = width * 0.0350f
        val rowStep = height * 0.0156f
        val radius = width * 0.0140f
        val output = ArrayList<Bead>()

        for (column in 0 until 24) {
            for (row in 0 until 6) {
                val cx = (firstCenterX + column * columnStep).toInt()
                val cy = (firstCenterY + row * rowStep).toInt()
                var blue = 0
                var red = 0
                for (angle in 0 until 360 step 8) {
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
                if (total >= 10) {
                    val dominant = maxOf(blue, red)
                    if (dominant >= 8 && dominant.toFloat() / total >= 0.62f) {
                        output.add(Bead(column, row, if (blue > red) "P" else "B"))
                    }
                }
            }
        }
        return output.sortedWith(compareBy<Bead> { it.column }.thenBy { it.row })
    }
}
