package com.envui.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Protector de pantalla del sistema con el estilo de ENV UI. */
class EnvDream : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        setContentView(ReposoView(this))
    }
}

/** Vista dibujada a mano para el modo reposo. La misma vista se usa como vista previa dentro del launcher. */
@SuppressLint("ViewConstructor")
class ReposoView(context: Context) : View(context) {
    private val prefs = context.getSharedPreferences(EnvSettings.PREFS, Context.MODE_PRIVATE)
    private val tickHandler = Handler(Looper.getMainLooper())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var wallpaper: Bitmap? = null
    private var shiftX = 0f
    private var shiftY = 0f
    private var lastShiftMinute = -1

    private val ticker = object : Runnable {
        override fun run() {
            invalidate()
            val now = System.currentTimeMillis()
            val seconds = prefs.getBoolean("show_seconds", false)
            tickHandler.postDelayed(this, if (seconds) 1000L - now % 1000L else 60_000L - now % 60_000L)
        }
    }

    init {
        prefs.getString("wallpaper_uri", null)?.let { wallpaper = decodeSampled(context, it, 1600) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        tickHandler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        tickHandler.removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    private fun accentColor(): Int {
        val index = prefs.getInt("accent_index", 0).coerceIn(0, AccentOptions.size - 1)
        val c = AccentOptions[index].color
        return android.graphics.Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }

    private fun typeface(): Typeface {
        val family = when (prefs.getInt("clock_font", 0)) {
            1 -> Typeface.SERIF
            2 -> Typeface.MONOSPACE
            3 -> Typeface.create("cursive", Typeface.NORMAL)
            else -> Typeface.SANS_SERIF
        }
        return when (prefs.getInt("clock_weight", 1)) {
            3 -> Typeface.create(family, Typeface.BOLD)
            2 -> family
            else -> Typeface.create(if (family == Typeface.SANS_SERIF) Typeface.create("sans-serif-light", Typeface.NORMAL) else family, Typeface.NORMAL)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val accent = accentColor()

        // Fondo: imagen recortada o degradado nocturno.
        val bmp = wallpaper
        if (bmp != null) {
            val scale = maxOf(w / bmp.width, h / bmp.height)
            val sw = (w / scale).toInt()
            val sh = (h / scale).toInt()
            val left = (bmp.width - sw) / 2
            val top = (bmp.height - sh) / 2
            paint.reset()
            paint.isFilterBitmap = true
            canvas.drawBitmap(bmp, Rect(left, top, left + sw, top + sh), Rect(0, 0, width, height), paint)
        } else {
            paint.reset()
            paint.shader = LinearGradient(0f, 0f, w, h, android.graphics.Color.rgb(6, 13, 26), android.graphics.Color.rgb(18, 32, 61), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }
        paint.reset()
        val dim = prefs.getFloat("dream_dim", 0.55f).coerceIn(0f, 0.95f)
        paint.color = android.graphics.Color.argb((dim * 255).toInt(), 0, 0, 0)
        canvas.drawRect(0f, 0f, w, h, paint)

        // Desplazamiento suave cada minuto para evitar marcas en la pantalla.
        val minuteNow = cal.get(Calendar.MINUTE)
        if (minuteNow != lastShiftMinute) {
            lastShiftMinute = minuteNow
            shiftX = ((minuteNow * 37) % 61 - 30).toFloat() * resources.displayMetrics.density
            shiftY = ((minuteNow * 23) % 41 - 20).toFloat() * resources.displayMetrics.density
        }
        val cx = w / 2f + shiftX
        val cy = h / 2f + shiftY
        val base = min(w, h)
        val use24 = prefs.getBoolean("use_24h", true)
        val seconds = prefs.getBoolean("show_seconds", false)
        val style = prefs.getInt("clock_style", 0)
        val useAccent = prefs.getBoolean("clock_accent", false)
        val white = android.graphics.Color.rgb(237, 244, 255)
        val muted = android.graphics.Color.argb(200, 200, 214, 232)
        var bottom: Float

        paint.reset()
        paint.isAntiAlias = true
        if (style == 1) {
            val r = base * 0.26f
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0 - 90)
                paint.color = muted
                paint.strokeWidth = r * 0.03f
                canvas.drawLine(cx + (cos(a) * r * 0.82).toFloat(), cy + (sin(a) * r * 0.82).toFloat(),
                    cx + (cos(a) * r * 0.94).toFloat(), cy + (sin(a) * r * 0.94).toFloat(), paint)
            }
            val sec = cal.get(Calendar.SECOND)
            val minute = cal.get(Calendar.MINUTE) + sec / 60.0
            val hour = cal.get(Calendar.HOUR) + minute / 60.0
            fun hand(fraction: Double, length: Float, stroke: Float, color: Int) {
                val a = fraction * 2 * Math.PI - Math.PI / 2
                paint.color = color
                paint.strokeWidth = stroke
                canvas.drawLine(cx, cy, cx + (cos(a) * length).toFloat(), cy + (sin(a) * length).toFloat(), paint)
            }
            hand(hour / 12.0, r * 0.5f, r * 0.07f, if (useAccent) accent else white)
            hand(minute / 60.0, r * 0.75f, r * 0.045f, if (useAccent) accent else white)
            if (seconds) hand(sec / 60.0, r * 0.82f, r * 0.018f, accent)
            paint.style = Paint.Style.FILL
            paint.color = accent
            canvas.drawCircle(cx, cy, r * 0.05f, paint)
            bottom = cy + r + base * 0.06f
        } else {
            val text = formatTime(now, use24, seconds)
            paint.typeface = if (style == 3) Typeface.create("sans-serif-thin", Typeface.NORMAL) else typeface()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = base * (if (seconds) 0.20f else 0.26f) * prefs.getFloat("clock_scale", 1f).coerceIn(0.6f, 1.6f)
            paint.color = if (useAccent || style == 4) accent else white
            if (style == 4) paint.setShadowLayer(paint.textSize * 0.25f, 0f, 0f, accent)
            canvas.drawText(text, cx, cy + paint.textSize * 0.35f, paint)
            paint.clearShadowLayer()
            bottom = cy + paint.textSize * 0.35f + base * 0.07f
        }

        paint.reset()
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.color = muted
        paint.textSize = base * 0.035f
        if (prefs.getBoolean("dream_date", true)) {
            canvas.drawText(formatLongDate(now), cx, bottom, paint)
            bottom += paint.textSize * 1.6f
        }
        if (prefs.getBoolean("dream_battery", true)) {
            val b = readBattery(context)
            paint.color = accent
            canvas.drawText("${b.level} %" + if (b.charging) "  ·  cargando" else "", cx, bottom, paint)
        }
    }
}
