package com.baccarat.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/** Conservative color reader for the Big Road bead grid in the supplied Stake layout. */
object RoadmapRecognizer {
    data class Bead(val x: Int, val y: Int, val result: String)

    fun read(bitmap: Bitmap): List<Bead> {
        val scale = 2
        val w = max(1, bitmap.width / scale)
        val h = max(1, bitmap.height / scale)
        val left = (w * 0.025f).toInt()
        val right = min(w, (w * 0.825f).toInt())
        // The Big Road is the upper six-row grid. Exclude the derived roads below it.
        val top = (h * 0.378f).toInt()
        val bottom = min(h, (h * 0.458f).toInt())
        if (right <= left || bottom <= top) return emptyList()
        val rw = right - left
        val rh = bottom - top
        val sample = IntArray(rw * rh)
        for (y in 0 until rh) for (x in 0 until rw) {
            val sx = min(bitmap.width - 1, (left + x) * scale)
            val sy = min(bitmap.height - 1, (top + y) * scale)
            sample[y * rw + x] = bitmap.getPixel(sx, sy)
        }
        val seen = BooleanArray(sample.size)
        val queue = IntArray(sample.size)
        val beads = ArrayList<Bead>()
        for (i in sample.indices) {
            if (seen[i]) continue
            val color = beadColor(sample[i]) ?: continue
            var head = 0; var tail = 0
            queue[tail++] = i; seen[i] = true
            var minX = rw; var maxX = 0; var minY = rh; var maxY = 0; var count = 0
            while (head < tail) {
                val at = queue[head++]
                val x = at % rw; val y = at / rw
                count++
                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)
                for (dy in -1..1) for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx; val ny = y + dy
                    if (nx !in 0 until rw || ny !in 0 until rh) continue
                    val ni = ny * rw + nx
                    if (!seen[ni] && beadColor(sample[ni]) == color) {
                        seen[ni] = true
                        queue[tail++] = ni
                    }
                }
            }
            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            if (bw in 7..18 && bh in 7..18 && count in 18..180) {
                beads.add(Bead((left + (minX + maxX) / 2) * scale,
                    (top + (minY + maxY) / 2) * scale, color))
            }
        }
        return beads.distinctBy { (it.x / 12) to (it.y / 12) }
            .sortedWith(compareBy<Bead> { it.x }.thenBy { it.y })
    }

    private fun beadColor(pixel: Int): String? {
        val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
        return when {
            b > 135 && b > r * 1.28 && b > g * 1.08 -> "P"
            r > 145 && r > b * 1.30 && r > g * 1.08 -> "B"
            else -> null
        }
    }
}