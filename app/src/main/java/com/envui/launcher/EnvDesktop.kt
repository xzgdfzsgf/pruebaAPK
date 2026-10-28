package com.envui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DesktopData(
    val now: Long,
    val battery: BatteryInfo,
    val pinned: List<LauncherApp>,
    val agenda: List<AgendaEvent>,
    val calendarAllowed: Boolean,
    val onOpen: (LauncherApp) -> Unit,
    val onRequestCalendar: () -> Unit,
)

/** Lista de widgets activos, en el orden en que se muestran. */
fun widgetList(d: DesktopData, s: EnvSettings, p: EnvPalette, notesMin: Int): List<@Composable (Modifier) -> Unit> {
    val list = mutableListOf<@Composable (Modifier) -> Unit>()
    if (s.showWeather) list.add { m -> WeatherWidget(s, p, m) }
    if (s.showAgenda) list.add { m -> AgendaWidget(d.agenda, d.calendarAllowed, s, p, m, d.onRequestCalendar) }
    if (s.showCalendar) list.add { m -> CalendarWidget(d.now, s, p, m) }
    if (s.showWorldClock) list.add { m -> WorldClockWidget(d.now, s, p, m) }
    if (s.showBattery) list.add { m -> BatteryWidget(d.battery, s, p, m) }
    if (s.showNotes) list.add { m -> NotesWidget(s, p, m, minHeight = notesMin) }
    return list
}

/** Acomoda los widgets en filas con el número de columnas indicado. */
@Composable
fun WidgetFlow(widgets: List<@Composable (Modifier) -> Unit>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        widgets.chunked(columns.coerceAtLeast(1)).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { widget -> widget(Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Contenido del escritorio según la disposición elegida. */
@Composable
fun DesktopContent(d: DesktopData, s: EnvSettings, p: EnvPalette, wide: Boolean, width: Dp) {
    when (s.layoutMode) {
        1 -> Box(Modifier.fillMaxSize(), contentAlignment = if (s.clockCentered) Alignment.Center else Alignment.CenterStart) {
            EnvClock(d.now, s, p, wide)
        }
        2 -> ProductiveLayout(d, s, p, wide, width)
        else -> ClassicLayout(d, s, p, wide, width)
    }
}

@Composable
private fun ClassicLayout(d: DesktopData, s: EnvSettings, p: EnvPalette, wide: Boolean, width: Dp) {
    val widgets = widgetList(d, s, p, notesMin = if (wide) 90 else 60)
    if (wide) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(Modifier.weight(1.45f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = if (s.clockCentered) Alignment.Center else Alignment.CenterStart) {
                    EnvClock(d.now, s, p, wide)
                }
                if (s.showPinned && d.pinned.isNotEmpty()) {
                    Spacer(Modifier.height(36.dp))
                    SectionLabel("Fijadas", p)
                    Spacer(Modifier.height(10.dp))
                    PinnedGrid(d.pinned, s, p, columnsFor(width * 0.58f, s), d.onOpen)
                }
            }
            if (widgets.isNotEmpty()) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    WidgetFlow(widgets, 1)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = if (s.clockCentered) Alignment.Center else Alignment.CenterStart) {
                EnvClock(d.now, s, p, wide)
            }
            Spacer(Modifier.height(28.dp))
            if (widgets.isNotEmpty()) {
                WidgetFlow(widgets, 2)
                Spacer(Modifier.height(22.dp))
            }
            if (s.showPinned && d.pinned.isNotEmpty()) {
                SectionLabel("Fijadas", p)
                Spacer(Modifier.height(10.dp))
                PinnedGrid(d.pinned, s, p, columnsFor(width, s), d.onOpen)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProductiveLayout(d: DesktopData, s: EnvSettings, p: EnvPalette, wide: Boolean, width: Dp) {
    val widgets = widgetList(d, s, p, notesMin = if (wide) 150 else 80)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { EnvClock(d.now, s, p, wide, compact = true) }
            if (!s.showBattery) BatteryPill(d.battery, p)
        }
        Spacer(Modifier.height(22.dp))
        if (widgets.isNotEmpty()) WidgetFlow(widgets, if (wide) 3 else 2)
        if (s.showPinned && d.pinned.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionLabel("Aplicaciones de trabajo", p)
            Spacer(Modifier.height(10.dp))
            GlassCard(s, p, Modifier.fillMaxWidth()) {
                PinnedGrid(d.pinned, s, p, columnsFor(width - 36.dp, s), d.onOpen)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BatteryPill(battery: BatteryInfo, p: EnvPalette) {
    Row(Modifier.clip(CircleShape).background(p.surface).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(if (battery.charging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, null,
            tint = p.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("${battery.level}%", color = p.fg, fontSize = 13.sp)
    }
}

fun columnsFor(width: Dp, s: EnvSettings): Int {
    val cell = s.iconSize * 1.3f + 30f
    return (width.value / cell).toInt().coerceIn(3, 10)
}

/** Barra de tareas: botón Inicio, apps ancladas y área de estado. */
@Composable
fun Taskbar(
    now: Long,
    s: EnvSettings,
    p: EnvPalette,
    battery: BatteryInfo,
    dockApps: List<LauncherApp>,
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onControl: () -> Unit,
    onOpen: (LauncherApp) -> Unit,
) {
    val radius = if (s.taskbarFloating) s.cornerRadius.dp else 0.dp
    val shape = RoundedCornerShape(radius)
    val showClock = when (s.taskbarClock) {
        1 -> true
        2 -> false
        else -> s.layoutMode == 2
    }
    val outer = if (s.taskbarFloating) Modifier.padding(horizontal = 6.dp).shadow(18.dp, shape) else Modifier
    Row(
        outer.fillMaxWidth().height(68.dp).clip(shape).background(p.surfaceStrong).border(1.dp, p.border, shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (s.taskbarCentered) Spacer(Modifier.weight(1f))
        Row(
            Modifier.weight(4f, fill = false).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(p.accent.copy(alpha = 0.18f))
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.GridView, "Inicio", tint = p.accent, modifier = Modifier.size(24.dp)) }
            dockApps.forEach { app ->
                AppIcon(app, s, p, sizeOverride = 30f, labels = false) { onOpen(app) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onControl).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Wifi, null, tint = p.muted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Icon(if (battery.charging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, null,
                tint = p.muted, modifier = Modifier.size(17.dp))
            Text("${battery.level}%", color = p.muted, fontSize = 12.sp)
            if (showClock) {
                Spacer(Modifier.width(10.dp))
                Text(formatTime(now, s.use24h, false), color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Tune, "Personalizar", tint = p.fg) }
    }
}
