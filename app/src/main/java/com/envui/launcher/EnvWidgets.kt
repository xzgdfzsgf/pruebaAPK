package com.envui.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

fun envShape(s: EnvSettings) = RoundedCornerShape(s.cornerRadius.dp)

@Composable
fun GlassCard(s: EnvSettings, p: EnvPalette, modifier: Modifier = Modifier, title: String? = null,
              content: @Composable ColumnScope.() -> Unit) {
    val shape = envShape(s)
    Column(
        modifier.clip(shape).background(p.surface).border(1.dp, p.border, shape).padding(18.dp)
    ) {
        if (title != null) {
            Text(title, color = p.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

/** Calendario del mes con el día actual resaltado. Semana iniciando en lunes. */
@Composable
fun CalendarWidget(now: Long, s: EnvSettings, p: EnvPalette, modifier: Modifier = Modifier) {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val monthName = java.text.SimpleDateFormat("MMMM yyyy", SpanishEc).format(cal.time).replaceFirstChar { it.uppercase() }
    val first = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    GlassCard(s, p, modifier) {
        Text(monthName, color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach {
                Text(it, color = p.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        val cells = offset + days
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).padding(1.dp).height(26.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val isToday = day == today
                            if (isToday) Box(Modifier.size(26.dp).clip(CircleShape).background(p.accent))
                            Text(day.toString(), fontSize = 12.sp,
                                color = if (isToday) p.onAccent else p.fg,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryWidget(battery: BatteryInfo, s: EnvSettings, p: EnvPalette, modifier: Modifier = Modifier) {
    GlassCard(s, p, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(64.dp)) {
                    val stroke = 7.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(p.border, -90f, 360f, false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round))
                    val color = when {
                        battery.level <= 15 -> androidx.compose.ui.graphics.Color(0xFFFF6B6B)
                        battery.charging -> androidx.compose.ui.graphics.Color(0xFF5EE6B5)
                        else -> p.accent
                    }
                    drawArc(color, -90f, 360f * battery.level / 100f, false, topLeft = Offset(inset, inset),
                        size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text("${battery.level}", color = p.fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Batería", color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (battery.charging) {
                        Icon(Icons.Default.BatteryChargingFull, null, tint = p.accent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        when {
                            battery.charging && battery.level >= 100 -> "Carga completa"
                            battery.charging -> "Cargando"
                            battery.level <= 15 -> "Conecta el cargador pronto"
                            else -> "Con energía para seguir"
                        }, color = p.muted, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NotesWidget(s: EnvSettings, p: EnvPalette, modifier: Modifier = Modifier, minHeight: Int = 90) {
    GlassCard(s, p, modifier, title = "Notas rápidas") {
        val text = s.notes ?: ""
        BasicTextField(
            value = text,
            onValueChange = { s.notes = it.take(2000) },
            textStyle = TextStyle(color = p.fg, fontSize = 15.sp, lineHeight = 21.sp),
            cursorBrush = SolidColor(p.accent),
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("Escribe un pendiente o una idea…", color = p.muted, fontSize = 15.sp)
                    inner()
                }
            }
        )
    }
}

fun iconShapeFor(s: EnvSettings): Shape = when (s.iconShape) {
    1 -> CircleShape
    else -> RoundedCornerShape((s.iconSize * 0.3f).dp)
}

/** Icono de aplicación con menú contextual al mantener presionado. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: LauncherApp,
    s: EnvSettings,
    p: EnvPalette,
    modifier: Modifier = Modifier,
    sizeOverride: Float? = null,
    labels: Boolean = s.showLabels,
    onOpen: () -> Unit,
) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val iconSize = sizeOverride ?: s.iconSize
    val bitmap = remember(app.key) { IconCache.get(ctx, app) }
    val isPinned = s.pinnedList().contains(app.key)
    val inDock = s.dockList().contains(app.key)
    val isHidden = s.hiddenList().contains(app.key)
    val haptics = LocalHapticFeedback.current
    Box(modifier) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onOpen, onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu = true
                })
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val shape = iconShapeFor(s)
            val plate = if (s.iconShape == 2) Modifier else Modifier.clip(shape).background(p.surfaceStrong).border(1.dp, p.border, shape)
            Box(Modifier.size((iconSize * 1.3f).dp).then(plate), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap, contentDescription = app.label, modifier = Modifier.size(iconSize.dp))
                else Icon(Icons.Default.Apps, app.label, tint = p.accent, modifier = Modifier.size(iconSize.dp))
            }
            if (labels) {
                Spacer(Modifier.height(6.dp))
                Text(app.label, color = p.fg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center, modifier = Modifier.width((iconSize * 1.3f + 18f).dp))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Quitar del escritorio" else "Fijar en el escritorio") },
                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = { s.togglePinned(app.key); menu = false })
            DropdownMenuItem(
                text = { Text(if (inDock) "Quitar de la barra" else "Anclar a la barra") },
                leadingIcon = { Icon(Icons.Default.ViewWeek, null) },
                onClick = { s.toggleDock(app.key); menu = false })
            DropdownMenuItem(
                text = { Text(if (isHidden) "Mostrar en Inicio" else "Ocultar de Inicio") },
                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = { s.toggleHidden(app.key); menu = false })
            DropdownMenuItem(
                text = { Text("Información de la app") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                onClick = { openAppInfo(ctx, app); menu = false })
            DropdownMenuItem(
                text = { Text("Desinstalar") },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { uninstallApp(ctx, app); menu = false })
        }
    }
}

@Composable
fun SectionLabel(text: String, p: EnvPalette, modifier: Modifier = Modifier) {
    Text(text, color = p.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = modifier)
}

@Composable
fun PinnedGrid(apps: List<LauncherApp>, s: EnvSettings, p: EnvPalette, columns: Int, onOpen: (LauncherApp) -> Unit) {
    val rows = apps.chunked(columns.coerceAtLeast(1))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { app ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) { AppIcon(app, s, p) { onOpen(app) } }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
