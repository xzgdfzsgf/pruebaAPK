package com.envui.launcher

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class AgendaEvent(val title: String, val begin: Long, val end: Long, val allDay: Boolean)

fun hasCalendarPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

/** Eventos de hoy y mañana desde el calendario del dispositivo. Debe llamarse fuera del hilo principal. */
fun loadAgenda(context: Context, max: Int = 5): List<AgendaEvent> {
    if (!hasCalendarPermission(context)) return emptyList()
    val now = System.currentTimeMillis()
    val end = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 2)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }.timeInMillis
    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, now - 6 * 60 * 60 * 1000L)
    ContentUris.appendId(builder, end)
    val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
    )
    val result = mutableListOf<AgendaEvent>()
    runCatching {
        context.contentResolver.query(builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext() && result.size < max) {
                val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "Sin título"
                val begin = c.getLong(1)
                val finish = c.getLong(2)
                val allDay = c.getInt(3) == 1
                if (finish >= now) result.add(AgendaEvent(title, begin, finish, allDay))
            }
        }
    }
    return result
}

private fun dayLabel(millis: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val sameDay = target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) "Hoy" else "Mañana"
}

@Composable
fun AgendaWidget(
    events: List<AgendaEvent>,
    permission: Boolean,
    s: EnvSettings,
    p: EnvPalette,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
) {
    GlassCard(s, p, modifier, title = "Agenda") {
        when {
            !permission -> {
                Text("Permite el acceso al calendario para ver tus eventos de hoy y mañana.", color = p.fg, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(p.accent).clickable(onClick = onRequestPermission)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) { Text("Permitir acceso", color = p.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
            events.isEmpty() -> Text("No tienes eventos para hoy ni para mañana.", color = p.muted, fontSize = 14.sp)
            else -> events.forEach { e ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(p.accent))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(e.title, color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val time = if (e.allDay) "Todo el día" else SimpleDateFormat("HH:mm", SpanishEc).format(Date(e.begin)) +
                            " a " + SimpleDateFormat("HH:mm", SpanishEc).format(Date(e.end))
                        Text("${dayLabel(e.begin)}  ·  $time", color = p.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WorldClockWidget(now: Long, s: EnvSettings, p: EnvPalette, modifier: Modifier = Modifier) {
    val zoneId = s.worldZone ?: "America/New_York"
    val zone = TimeZone.getTimeZone(zoneId)
    val city = WorldZones.firstOrNull { it.first == zoneId }?.second ?: zoneId.substringAfterLast('/').replace('_', ' ')
    val fmt = SimpleDateFormat(if (s.use24h) "HH:mm" else "h:mm a", SpanishEc).apply { timeZone = zone }
    val diffHours = (zone.getOffset(now) - TimeZone.getDefault().getOffset(now)) / 3_600_000.0
    val diffText = when {
        diffHours == 0.0 -> "Misma hora que tú"
        diffHours > 0 -> "${formatHours(diffHours)} h más que tú"
        else -> "${formatHours(-diffHours)} h menos que tú"
    }
    GlassCard(s, p, modifier, title = "Reloj mundial") {
        Text(fmt.format(Date(now)), color = p.fg, fontSize = 30.sp, fontWeight = FontWeight.Light)
        Text(city, color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(diffText, color = p.muted, fontSize = 12.sp)
    }
}

private fun formatHours(h: Double): String = if (h % 1.0 == 0.0) h.toInt().toString() else String.format(SpanishEc, "%.1f", h)
