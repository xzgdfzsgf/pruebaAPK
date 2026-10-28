package com.envui.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

val SpanishEc: Locale = Locale("es", "EC")

fun formatTime(now: Long, use24h: Boolean, seconds: Boolean): String {
    val pattern = buildString {
        append(if (use24h) "HH:mm" else "h:mm")
        if (seconds) append(":ss")
    }
    return SimpleDateFormat(pattern, SpanishEc).format(Date(now))
}

fun formatAmPm(now: Long): String =
    if (Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.AM_PM) == Calendar.AM) "a. m." else "p. m."

fun formatLongDate(now: Long): String =
    SimpleDateFormat("EEEE, d 'de' MMMM", SpanishEc).format(Date(now)).replaceFirstChar { it.uppercase() }

fun greetingFor(now: Long): String {
    val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

/** Reloj principal del escritorio con los estilos configurables. */
@Composable
fun EnvClock(now: Long, s: EnvSettings, p: EnvPalette, wide: Boolean, compact: Boolean = false) {
    val align = if (s.clockCentered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (s.clockCentered) TextAlign.Center else TextAlign.Start
    val base = (if (compact) 46f else if (wide) 92f else 76f) * s.clockScale
    val color = if (s.clockUseAccent) p.accent else p.fg
    val family = clockFontFamily(s.clockFont)
    val weight = clockFontWeight(s.clockWeight)

    val ctx = LocalContext.current
    val name = s.userName?.trim().orEmpty()
    Column(
        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { openClockApp(ctx) },
        horizontalAlignment = align
    ) {
        if (s.showGreeting && !compact) {
            Text(if (name.isEmpty()) greetingFor(now) else "${greetingFor(now)}, $name", color = p.muted, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = textAlign)
            Spacer(Modifier.height(6.dp))
        }
        when (s.clockStyle) {
            1 -> AnalogClock(now, p, color, (base * 2.1f).dp, s.showSeconds)
            2 -> {
                val c = Calendar.getInstance().apply { timeInMillis = now }
                val h = if (s.use24h) c.get(Calendar.HOUR_OF_DAY) else (c.get(Calendar.HOUR).let { if (it == 0) 12 else it })
                val style = TextStyle(fontFamily = family, fontWeight = weight, fontSize = (base * 1.15f).sp,
                    lineHeight = (base * 1.05f).sp, color = color, textAlign = textAlign)
                Text(h.toString().padStart(2, '0'), style = style)
                Text(c.get(Calendar.MINUTE).toString().padStart(2, '0'), style = style.copy(color = p.accent))
            }
            3 -> Text(
                formatTime(now, s.use24h, s.showSeconds),
                style = TextStyle(fontFamily = family, fontWeight = FontWeight.Thin, fontSize = (base * 1.1f).sp,
                    letterSpacing = (base * 0.04f).sp, color = color, textAlign = textAlign)
            )
            4 -> Text(
                formatTime(now, s.use24h, s.showSeconds),
                style = TextStyle(fontFamily = family, fontWeight = weight, fontSize = base.sp, color = p.accent,
                    textAlign = textAlign, shadow = Shadow(color = p.accent.copy(alpha = 0.85f), blurRadius = base * 0.55f))
            )
            else -> Text(
                formatTime(now, s.use24h, s.showSeconds),
                style = TextStyle(fontFamily = family, fontWeight = weight, fontSize = base.sp,
                    lineHeight = (base * 1.08f).sp, color = color, textAlign = textAlign)
            )
        }
        if (!s.use24h && s.clockStyle != 1) {
            Text(formatAmPm(now), color = p.muted, fontSize = 14.sp, textAlign = textAlign)
        }
        if (s.showDate) {
            Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
            Text(formatLongDate(now), color = p.muted, fontSize = if (compact) 13.sp else 17.sp, textAlign = textAlign)
        }
    }
}

@Composable
fun AnalogClock(now: Long, p: EnvPalette, handColor: Color, size: androidx.compose.ui.unit.Dp, seconds: Boolean) {
    val c = Calendar.getInstance().apply { timeInMillis = now }
    val sec = c.get(Calendar.SECOND)
    val minute = c.get(Calendar.MINUTE) + sec / 60f
    val hour = (c.get(Calendar.HOUR) + minute / 60f)
    Canvas(Modifier.size(size)) {
        val r = min(this.size.width, this.size.height) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(p.surfaceStrong, radius = r)
        drawCircle(p.border, radius = r, style = Stroke(width = r * 0.02f))
        for (i in 0 until 60) {
            val a = (i / 60f) * 2 * PI - PI / 2
            val major = i % 5 == 0
            val outer = r * 0.92f
            val inner = if (major) r * 0.80f else r * 0.87f
            drawLine(
                if (major) p.fg.copy(alpha = 0.85f) else p.muted.copy(alpha = 0.45f),
                Offset(center.x + (cos(a) * inner).toFloat(), center.y + (sin(a) * inner).toFloat()),
                Offset(center.x + (cos(a) * outer).toFloat(), center.y + (sin(a) * outer).toFloat()),
                strokeWidth = if (major) r * 0.03f else r * 0.012f,
                cap = StrokeCap.Round
            )
        }
        fun hand(fraction: Float, length: Float, width: Float, color: Color) {
            val a = fraction * 2 * PI - PI / 2
            drawLine(color, center,
                Offset(center.x + (cos(a) * length).toFloat(), center.y + (sin(a) * length).toFloat()),
                strokeWidth = width, cap = StrokeCap.Round)
        }
        hand(hour / 12f, r * 0.50f, r * 0.065f, handColor)
        hand(minute / 60f, r * 0.72f, r * 0.045f, handColor)
        if (seconds) hand(sec / 60f, r * 0.80f, r * 0.018f, p.accent)
        drawCircle(p.accent, radius = r * 0.05f, center = center)
    }
}
