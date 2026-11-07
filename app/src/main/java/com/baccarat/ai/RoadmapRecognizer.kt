package com.baccarat.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Samples the annular outline at each cell in the six-row Big Road grid.
 * The grid proportions are calibrated from the supplied Stake Speed Baccarat A screenshot.
 */
object RoadmapRecognizer {
    data class Bead(val column: Int, val row: Int, val result: String)

    fun read(bitmap: Bitmap): List<Bead> {
        val width = bitmap.width
        if (width < bitmap.height) return emptyList()
        val height = bitmap.height
        val firstCenterX = width * 0.0435f
        val firstCenterY = height * 0.315f
        val columnStep = width * 0.0229f
        val rowStep = height * 0.0235f
        val radius = width * 0.0067f
        val output = ArrayList<Bead>()

        for (column in 0 until 36) {
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
                    if (b > 125 && b > r * 1.18 && b > g * 1.03) blue++
                    if (r > 130 && r > b * 1.18 && r > g * 1.03) red++
                }
                val total = blue + red
                if (total >= 18) {
                    val result = if (blue > red) "P" else "B"
                    val dominant = max(blue, red)
                    if (dominant >= 15 && dominant.toFloat() / total >= 0.72f) {
                        output.add(Bead(column, row, result))
                    }
                }
            }
        }
        // Baccarat Big Road is read column by column, top to bottom within a column.
        return output.sortedWith(compareBy<Bead> { it.column }.thenBy { it.row })
    }
}