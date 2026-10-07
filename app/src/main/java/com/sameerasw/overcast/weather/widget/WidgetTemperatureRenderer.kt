package com.sameerasw.overcast.weather.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.sameerasw.overcast.R
import java.io.File
import kotlin.math.ceil
import kotlin.math.min

// Widgets can't load custom fonts, so the temperature is drawn here with Google Sans Flex and shown as an image.
// The degree sign hangs off the right of the digits and is balanced by empty space on the left, so only the digits
// count towards centring, the same way the in-app header does it.
object WidgetTemperatureRenderer {
    private const val VARIATIONS = "'wdth' 52, 'wght' 1, 'ROND' 100"
    private const val MEASURE_SIZE = 300f
    private const val DEGREE_SCALE = 0.42f
    private const val DEGREE_GAP = 0.05f
    private const val MAX_SIDE_PX = 700
    private const val DEGREE = "°"

    @Volatile
    private var typeface: Typeface? = null

    private fun typeface(context: Context): Typeface =
        typeface ?: synchronized(this) {
            typeface ?: build(context.applicationContext).also { typeface = it }
        }

    private fun build(context: Context): Typeface {
        val file = File(context.cacheDir, "widget_google_sans_flex.ttf")
        return try {
            if (!file.exists() || file.length() == 0L) {
                context.resources.openRawResource(R.font.google_sans_flex).use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
            }
            Typeface.Builder(file).setFontVariationSettings(VARIATIONS).build() ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun paint(face: Typeface, color: Int, size: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        typeface = face
        letterSpacing = -0.03f
        textSize = size
    }

    fun render(context: Context, temperature: String, maxWidthPx: Int, maxHeightPx: Int, color: Int): Bitmap {
        val maxW = maxWidthPx.coerceIn(1, MAX_SIDE_PX)
        val maxH = maxHeightPx.coerceIn(1, MAX_SIDE_PX)
        val face = typeface(context)
        val hasDegree = temperature.endsWith(DEGREE)
        val digits = temperature.removeSuffix(DEGREE)

        fun measure(size: Float): Triple<Rect, Rect, Float> {
            val digitBounds = Rect()
            paint(face, color, size).getTextBounds(digits, 0, digits.length, digitBounds)
            val degreeBounds = Rect()
            if (hasDegree) paint(face, color, size * DEGREE_SCALE).getTextBounds(DEGREE, 0, DEGREE.length, degreeBounds)
            val side = if (hasDegree) degreeBounds.width() + size * DEGREE_GAP else 0f
            return Triple(digitBounds, degreeBounds, side)
        }

        val (baseDigits, _, baseSide) = measure(MEASURE_SIZE)
        val totalW = baseDigits.width() + 2 * baseSide
        val scale = min(maxW / totalW.coerceAtLeast(1f), maxH.toFloat() / baseDigits.height().coerceAtLeast(1))
        val size = MEASURE_SIZE * scale
        val (digitBounds, degreeBounds, side) = measure(size)

        val digitPaint = paint(face, color, size)
        val degreePaint = paint(face, color, size * DEGREE_SCALE)
        val shadow = size * 0.02f
        digitPaint.setShadowLayer(shadow, 0f, shadow, 0x55000000)
        degreePaint.setShadowLayer(shadow, 0f, shadow, 0x55000000)
        val pad = ceil(shadow * 2).toInt() + 1

        val width = ceil(digitBounds.width() + 2 * side).toInt() + pad * 2
        val height = digitBounds.height() + pad * 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val digitsLeft = pad + side
        canvas.drawText(digits, digitsLeft - digitBounds.left, (pad - digitBounds.top).toFloat(), digitPaint)
        if (hasDegree) {
            val degreeLeft = digitsLeft + digitBounds.width() + size * DEGREE_GAP
            canvas.drawText(DEGREE, degreeLeft - degreeBounds.left, (pad - degreeBounds.top).toFloat(), degreePaint)
        }
        return bitmap
    }
}
