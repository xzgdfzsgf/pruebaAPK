package com.envui.launcher

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val Tabs = listOf("Apariencia", "Reloj", "Disposición", "Iconos y barra", "Fondo", "Bloqueo y reposo")

/** Panel lateral de personalización. Los cambios se aplican en vivo sobre el escritorio. */
@Composable
fun SettingsPanel(
    s: EnvSettings,
    p: EnvPalette,
    apps: List<LauncherApp>,
    modifier: Modifier = Modifier,
    onRequestCalendar: () -> Unit,
    onPickWallpaper: () -> Unit,
    onPreviewDream: () -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val shape = RoundedCornerShape((s.cornerRadius + 6f).dp)
    Column(modifier.clip(shape).background(p.surfaceStrong).border(1.dp, p.border, shape).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Personalizar", color = p.fg, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("Los cambios se ven al instante en tu escritorio.", color = p.muted, fontSize = 13.sp)
            }
            RoundButton(Icons.Default.Close, "Cerrar", p, onClose)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tabs.forEachIndexed { i, name -> Chip(name, i == tab, p) { tab = i } }
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (tab) {
                0 -> AppearanceTab(s, p)
                1 -> ClockTab(s, p)
                2 -> LayoutTab(s, p, onRequestCalendar)
                3 -> IconsTab(s, p, apps)
                4 -> WallpaperTab(s, p, onPickWallpaper)
                else -> LockTab(s, p, onPreviewDream)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppearanceTab(s: EnvSettings, p: EnvPalette) {
    Group("Tema", p) {
        ChipRow(listOf("Oscuro", "Claro", "Según el sistema"), s.themeMode, p) { s.themeMode = it }
    }
    Group("Color de acento", p) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentOptions.forEachIndexed { i, option ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(option.color)
                            .border(if (i == s.accentIndex) 3.dp else 0.dp, p.fg, CircleShape)
                            .clickable { s.accentIndex = i },
                        contentAlignment = Alignment.Center
                    ) { if (i == s.accentIndex) Icon(Icons.Default.Check, null, tint = Color(0xFF06101E)) }
                    Spacer(Modifier.height(4.dp))
                    Text(option.name, color = p.muted, fontSize = 11.sp)
                }
            }
        }
    }
    Group("Paneles", p) {
        SliderRow("Opacidad del cristal", s.glassOpacity, 0.3f..0.98f, "${(s.glassOpacity * 100).roundToInt()} %", p) { s.glassOpacity = it }
        SliderRow("Redondez de esquinas", s.cornerRadius, 6f..36f, "${s.cornerRadius.roundToInt()} dp", p) { s.cornerRadius = it }
    }
    Group("Restablecer", p) {
        Text("Vuelve a la configuración original sin perder tu fondo, tus notas ni tus apps fijadas.", color = p.muted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        ActionButton("Restablecer ajustes", p, primary = false) { s.resetAll() }
    }
}

@Composable
private fun ClockTab(s: EnvSettings, p: EnvPalette) {
    Group("Estilo", p) { ChipRow(ClockStyles, s.clockStyle, p) { s.clockStyle = it } }
    if (s.clockStyle != 1) {
        Group("Tipografía", p) {
            ChipRow(ClockFonts, s.clockFont, p) { s.clockFont = it }
            Spacer(Modifier.height(10.dp))
            ChipRow(ClockWeights, s.clockWeight, p) { s.clockWeight = it }
        }
    }
    Group("Tamaño y posición", p) {
        SliderRow("Tamaño", s.clockScale, 0.6f..1.6f, "${(s.clockScale * 100).roundToInt()} %", p) { s.clockScale = it }
        ToggleRow("Centrar reloj", "Ubica el reloj en el centro en lugar de la izquierda.", s.clockCentered, p) { s.clockCentered = it }
        ToggleRow("Usar color de acento", "Pinta la hora con el color que elegiste en Apariencia.", s.clockUseAccent, p) { s.clockUseAccent = it }
    }
    Group("Formato", p) {
        ToggleRow("Formato de 24 horas", "Desactívalo para ver la hora con a. m. y p. m.", s.use24h, p) { s.use24h = it }
        ToggleRow("Mostrar segundos", "Consume un poco más de batería.", s.showSeconds, p) { s.showSeconds = it }
        ToggleRow("Mostrar fecha", null, s.showDate, p) { s.showDate = it }
        ToggleRow("Mostrar saludo", "Buenos días, buenas tardes o buenas noches según la hora.", s.showGreeting, p) { s.showGreeting = it }
        if (s.showGreeting) {
            Spacer(Modifier.height(6.dp))
            InputField(s.userName ?: "", "Tu nombre para el saludo", p) { s.userName = it.take(30) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Toca el reloj del escritorio para abrir tus alarmas.", color = p.muted, fontSize = 12.sp)
    }
}

@Composable
private fun LayoutTab(s: EnvSettings, p: EnvPalette, onRequestCalendar: () -> Unit) {
    Group("Disposición del escritorio", p) {
        val descriptions = listOf(
            "Reloj grande, apps fijadas y una columna de widgets.",
            "Solo el reloj y la barra. Ideal para fondos con mucha presencia.",
            "Reloj compacto, widgets en fila y una cuadrícula amplia de apps de trabajo."
        )
        LayoutNames.forEachIndexed { i, name ->
            val selected = s.layoutMode == i
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (selected) p.accent.copy(alpha = 0.16f) else p.surface)
                    .border(1.dp, if (selected) p.accent else p.border, RoundedCornerShape(16.dp))
                    .clickable { s.layoutMode = i }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(descriptions[i], color = p.muted, fontSize = 13.sp)
                }
                if (selected) Icon(Icons.Default.Check, null, tint = p.accent)
            }
        }
    }
    if (s.layoutMode != 1) {
        Group("Widgets visibles", p) {
            ToggleRow("Clima", "Datos de Open-Meteo, sin cuentas ni claves.", s.showWeather, p) { s.showWeather = it }
            ToggleRow("Agenda", "Tus eventos de hoy y mañana del calendario de la tablet.", s.showAgenda, p) {
                s.showAgenda = it
                if (it) onRequestCalendar()
            }
            ToggleRow("Reloj mundial", null, s.showWorldClock, p) { s.showWorldClock = it }
            ToggleRow("Calendario del mes", null, s.showCalendar, p) { s.showCalendar = it }
            ToggleRow("Batería", null, s.showBattery, p) { s.showBattery = it }
            ToggleRow("Notas rápidas", "Se guardan automáticamente en la tablet.", s.showNotes, p) { s.showNotes = it }
            ToggleRow("Apps fijadas", "Mantén presionada una app en Inicio para fijarla.", s.showPinned, p) { s.showPinned = it }
        }
        if (s.showWeather) WeatherCityGroup(s, p)
        if (s.showWorldClock) {
            Group("Ciudad del reloj mundial", p) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorldZones.forEach { (id, name) -> Chip(name, s.worldZone == id, p) { s.worldZone = id } }
                }
            }
        }
    }
}

@Composable
private fun IconsTab(s: EnvSettings, p: EnvPalette, apps: List<LauncherApp>) {
    Group("Iconos", p) {
        ChipRow(IconShapes, s.iconShape, p) { s.iconShape = it }
        Spacer(Modifier.height(6.dp))
        SliderRow("Tamaño de iconos", s.iconSize, 36f..72f, "${s.iconSize.roundToInt()} dp", p) { s.iconSize = it }
        ToggleRow("Mostrar nombres", null, s.showLabels, p) { s.showLabels = it }
    }
    Group("Barra de tareas", p) {
        ToggleRow("Barra flotante", "Desactívala para una barra anclada al borde inferior.", s.taskbarFloating, p) { s.taskbarFloating = it }
        ToggleRow("Iconos centrados", "Al estilo de Windows 11. Desactívalo para alinearlos a la izquierda.", s.taskbarCentered, p) { s.taskbarCentered = it }
        Spacer(Modifier.height(6.dp))
        Text("Hora en la barra", color = p.fg, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        ChipRow(listOf("Automática", "Siempre", "Nunca"), s.taskbarClock, p) { s.taskbarClock = it }
        Spacer(Modifier.height(6.dp))
        Text("En automático solo aparece cuando el reloj grande no está en pantalla, para no repetir la hora.",
            color = p.muted, fontSize = 13.sp)
    }
    Group("Apps ocultas", p) {
        val hiddenApps = s.hiddenList().mapNotNull { k -> apps.firstOrNull { it.key == k } }
        if (hiddenApps.isEmpty()) {
            Text("No tienes apps ocultas. Mantén presionada una app en Inicio y elige Ocultar de Inicio.", color = p.muted, fontSize = 13.sp)
        } else {
            hiddenApps.forEach { app ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(app.label, color = p.fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Chip("Mostrar", false, p) { s.toggleHidden(app.key) }
                }
            }
        }
    }
}

@Composable
private fun WallpaperTab(s: EnvSettings, p: EnvPalette, onPick: () -> Unit) {
    Group("Fondos de ENV UI", p) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientPresets.forEachIndexed { i, preset ->
                val selected = s.wallpaperUri == null && s.wallpaperPreset == i
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(width = 64.dp, height = 88.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(if (p.dark) preset.darkColors else preset.lightColors))
                            .border(if (selected) 3.dp else 1.dp, if (selected) p.accent else p.border, RoundedCornerShape(14.dp))
                            .clickable { s.wallpaperPreset = i; s.wallpaperUri = null },
                        contentAlignment = Alignment.Center
                    ) { if (selected) Icon(Icons.Default.Check, null, tint = p.accent) }
                    Spacer(Modifier.height(4.dp))
                    Text(preset.name, color = p.muted, fontSize = 11.sp)
                }
            }
        }
    }
    Group("Imagen propia", p) {
        ActionButton(if (s.wallpaperUri == null) "Elegir imagen" else "Cambiar imagen", p, primary = true, onClick = onPick)
        if (s.wallpaperUri != null) {
            Spacer(Modifier.height(8.dp))
            ActionButton("Quitar imagen y usar un fondo de ENV UI", p, primary = false) { s.wallpaperUri = null }
        }
    }
    Group("Ajustes de la imagen", p) {
        SliderRow("Oscurecer", s.wallpaperDim, 0f..0.8f, "${(s.wallpaperDim * 100).roundToInt()} %", p) { s.wallpaperDim = it }
        SliderRow("Desenfoque", s.wallpaperBlur, 0f..30f, "${s.wallpaperBlur.roundToInt()}", p) { s.wallpaperBlur = it }
        Text("El desenfoque funciona en Android 12 o superior.", color = p.muted, fontSize = 12.sp)
    }
}

@Composable
private fun LockTab(s: EnvSettings, p: EnvPalette, onPreviewDream: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Group("Pantalla de bloqueo", p) {
        Text(
            "Android no permite que un launcher reemplace la pantalla de bloqueo del sistema, porque es la que protege tu PIN o huella. " +
                "Lo que sí puedes hacer desde aquí es poner tu fondo de ENV UI en la pantalla de bloqueo y en el fondo del sistema, " +
                "para que todo se vea coherente al desbloquear.",
            color = p.muted, fontSize = 13.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(12.dp))
        val uri = s.wallpaperUri
        if (uri == null) {
            Text("Primero elige una imagen en la pestaña Fondo.", color = p.fg, fontSize = 14.sp)
        } else {
            ActionButton(if (busy) "Aplicando…" else "Usar mi fondo en la pantalla de bloqueo", p, primary = true) {
                if (!busy) {
                    busy = true
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { applySystemWallpaper(ctx, uri, lock = true, home = false) }
                        busy = false
                        toast(ctx, if (ok) "Fondo aplicado a la pantalla de bloqueo" else "Tu tablet no permitió cambiar el fondo de bloqueo")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ActionButton("Usar mi fondo también en el sistema", p, primary = false) {
                if (!busy) {
                    busy = true
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { applySystemWallpaper(ctx, uri, lock = true, home = true) }
                        busy = false
                        toast(ctx, if (ok) "Fondo aplicado al sistema y al bloqueo" else "No se pudo aplicar el fondo")
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ActionButton("Abrir ajustes de bloqueo del sistema", p, primary = false) {
            openSystemScreen(ctx, Settings.ACTION_SECURITY_SETTINGS)
        }
    }
    Group("Pantalla de reposo ENV", p) {
        Text(
            "Es un reloj a pantalla completa que Android muestra mientras la tablet se carga o está en su base. " +
                "Usa el mismo estilo de reloj, tu fondo y tu color de acento.",
            color = p.muted, fontSize = 13.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(8.dp))
        ToggleRow("Mostrar fecha", null, s.dreamShowDate, p) { s.dreamShowDate = it }
        ToggleRow("Mostrar batería", null, s.dreamShowBattery, p) { s.dreamShowBattery = it }
        SliderRow("Oscurecer fondo", s.dreamDim, 0.2f..0.9f, "${(s.dreamDim * 100).roundToInt()} %", p) { s.dreamDim = it }
        Spacer(Modifier.height(6.dp))
        ActionButton("Ver vista previa", p, primary = true, onClick = onPreviewDream)
        Spacer(Modifier.height(8.dp))
        ActionButton("Activar en Ajustes del sistema", p, primary = false) {
            openSystemScreen(ctx, Settings.ACTION_DREAM_SETTINGS, Settings.ACTION_DISPLAY_SETTINGS)
        }
        Spacer(Modifier.height(6.dp))
        Text("En Ajustes, elige \"Reposo ENV UI\" como protector de pantalla.", color = p.muted, fontSize = 12.sp)
    }
}

@Composable
private fun WeatherCityGroup(s: EnvSettings, p: EnvPalette) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf(s.weatherCity ?: "") }
    var searching by remember { mutableStateOf(false) }
    Group("Ciudad del clima", p) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { InputField(city, "Escribe tu ciudad", p) { city = it.take(60) } }
            Spacer(Modifier.width(8.dp))
            Chip(if (searching) "Buscando…" else "Buscar", true, p) {
                if (!searching && city.isNotBlank()) {
                    searching = true
                    scope.launch {
                        val found = withContext(Dispatchers.IO) { geocodeCity(city) }
                        searching = false
                        if (found == null) {
                            toast(ctx, "No encontré esa ciudad o no hay conexión")
                        } else {
                            s.weatherCity = found.name
                            s.weatherLat = found.lat.toFloat()
                            s.weatherLon = found.lon.toFloat()
                            s.weatherCache = null
                            s.weatherTime = 0L
                            city = found.name
                            toast(ctx, "Clima configurado para ${found.name}" + if (found.country.isNotEmpty()) ", ${found.country}" else "")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Se actualiza cada 30 minutos mientras uses el launcher.", color = p.muted, fontSize = 12.sp)
    }
}

@Composable
private fun InputField(value: String, placeholder: String, p: EnvPalette, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = p.fg, fontSize = 15.sp),
        cursorBrush = SolidColor(p.accent),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface)
            .border(1.dp, p.border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = p.muted, fontSize = 15.sp)
                inner()
            }
        }
    )
}

// Controles reutilizables

@Composable
private fun Group(title: String, p: EnvPalette, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        Text(title, color = p.fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun Chip(label: String, selected: Boolean, p: EnvPalette, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) p.accent else p.surface)
            .border(1.dp, if (selected) p.accent else p.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (selected) p.onAccent else p.fg, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: Int, p: EnvPalette, onSelect: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label -> Chip(label, i == selected, p) { onSelect(i) } }
    }
}

@Composable
private fun ToggleRow(title: String, description: String?, checked: Boolean, p: EnvPalette, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.fg, fontSize = 15.sp)
            if (description != null) Text(description, color = p.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.onAccent, checkedTrackColor = p.accent,
                uncheckedThumbColor = p.muted, uncheckedTrackColor = p.surface, uncheckedBorderColor = p.border
            )
        )
    }
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, p: EnvPalette, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(title, color = p.fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = p.muted, fontSize = 13.sp)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.border)
        )
    }
}

@Composable
private fun ActionButton(label: String, p: EnvPalette, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (primary) p.accent else p.surface)
            .border(1.dp, if (primary) p.accent else p.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (primary) p.onAccent else p.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
