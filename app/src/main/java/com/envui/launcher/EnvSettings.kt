package com.envui.launcher

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Preferencias persistentes que además son observables por Compose. */
private class PrefInt(private val sp: SharedPreferences, private val key: String, default: Int) : ReadWriteProperty<Any?, Int> {
    private var state by mutableIntStateOf(sp.getInt(key, default))
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int = state
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        state = value
        sp.edit().putInt(key, value).apply()
    }
}

private class PrefFloat(private val sp: SharedPreferences, private val key: String, default: Float) : ReadWriteProperty<Any?, Float> {
    private var state by mutableFloatStateOf(sp.getFloat(key, default))
    override fun getValue(thisRef: Any?, property: KProperty<*>): Float = state
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        state = value
        sp.edit().putFloat(key, value).apply()
    }
}

private class PrefBool(private val sp: SharedPreferences, private val key: String, default: Boolean) : ReadWriteProperty<Any?, Boolean> {
    private var state by mutableStateOf(sp.getBoolean(key, default))
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = state
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        state = value
        sp.edit().putBoolean(key, value).apply()
    }
}

private class PrefString(private val sp: SharedPreferences, private val key: String, default: String?) : ReadWriteProperty<Any?, String?> {
    private var state by mutableStateOf(sp.getString(key, default))
    override fun getValue(thisRef: Any?, property: KProperty<*>): String? = state
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        state = value
        if (value == null) sp.edit().remove(key).apply() else sp.edit().putString(key, value).apply()
    }
}

private class PrefLong(private val sp: SharedPreferences, private val key: String, default: Long) : ReadWriteProperty<Any?, Long> {
    private var state by mutableLongStateOf(sp.getLong(key, default))
    override fun getValue(thisRef: Any?, property: KProperty<*>): Long = state
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        state = value
        sp.edit().putLong(key, value).apply()
    }
}

class EnvSettings(private val sp: SharedPreferences) {
    // Apariencia
    var themeMode by PrefInt(sp, "theme_mode", 0)          // 0 oscuro, 1 claro, 2 automático
    var accentIndex by PrefInt(sp, "accent_index", 0)
    var glassOpacity by PrefFloat(sp, "glass_opacity", 0.72f)
    var cornerRadius by PrefFloat(sp, "corner_radius", 24f)

    // Reloj
    var clockStyle by PrefInt(sp, "clock_style", 0)        // 0 digital, 1 analógico, 2 apilado, 3 fino, 4 neón
    var clockFont by PrefInt(sp, "clock_font", 0)          // 0 moderna, 1 clásica, 2 técnica, 3 manuscrita
    var clockWeight by PrefInt(sp, "clock_weight", 1)      // 0 fina, 1 ligera, 2 normal, 3 negrita
    var clockScale by PrefFloat(sp, "clock_scale", 1f)
    var clockUseAccent by PrefBool(sp, "clock_accent", false)
    var clockCentered by PrefBool(sp, "clock_centered", false)
    var use24h by PrefBool(sp, "use_24h", true)
    var showSeconds by PrefBool(sp, "show_seconds", false)
    var showDate by PrefBool(sp, "show_date", true)
    var showGreeting by PrefBool(sp, "show_greeting", true)
    var userName by PrefString(sp, "user_name", "")

    // Disposición
    var layoutMode by PrefInt(sp, "layout_mode", 0)        // 0 escritorio, 1 minimalista, 2 productivo
    var showCalendar by PrefBool(sp, "w_calendar", true)
    var showBattery by PrefBool(sp, "w_battery", true)
    var showNotes by PrefBool(sp, "w_notes", true)
    var showPinned by PrefBool(sp, "w_pinned", true)
    var notes by PrefString(sp, "notes_text", "")
    var showWeather by PrefBool(sp, "w_weather", true)
    var showAgenda by PrefBool(sp, "w_agenda", false)
    var showWorldClock by PrefBool(sp, "w_world", false)
    var worldZone by PrefString(sp, "world_zone", "America/New_York")

    // Clima (Open-Meteo, sin clave). Quito por defecto.
    var weatherCity by PrefString(sp, "weather_city", "Quito")
    var weatherLat by PrefFloat(sp, "weather_lat", -0.2299f)
    var weatherLon by PrefFloat(sp, "weather_lon", -78.5249f)
    var weatherCache by PrefString(sp, "weather_cache", null)
    var weatherTime by PrefLong(sp, "weather_time", 0L)

    // Menú Inicio
    var drawerMode by PrefInt(sp, "drawer_mode", 0)         // 0 alfabético, 1 por categorías
    var hidden by PrefString(sp, "hidden_apps", null)
    var launchCounts by PrefString(sp, "launch_counts", null)

    // Iconos y barra
    var iconShape by PrefInt(sp, "icon_shape", 0)          // 0 redondeado, 1 círculo, 2 sin fondo
    var iconSize by PrefFloat(sp, "icon_size", 48f)
    var showLabels by PrefBool(sp, "show_labels", true)
    var taskbarFloating by PrefBool(sp, "taskbar_floating", true)
    var taskbarCentered by PrefBool(sp, "taskbar_centered", true)
    var taskbarClock by PrefInt(sp, "taskbar_clock", 0)    // 0 automático, 1 siempre, 2 nunca
    var pinned by PrefString(sp, "pinned_apps", null)
    var dock by PrefString(sp, "dock_apps", null)

    // Fondo
    var wallpaperUri by PrefString(sp, "wallpaper_uri", null)
    var wallpaperPreset by PrefInt(sp, "wallpaper_preset", 0)
    var wallpaperDim by PrefFloat(sp, "wallpaper_dim", 0.30f)
    var wallpaperBlur by PrefFloat(sp, "wallpaper_blur", 0f)

    // Reposo
    var dreamShowDate by PrefBool(sp, "dream_date", true)
    var dreamShowBattery by PrefBool(sp, "dream_battery", true)
    var dreamDim by PrefFloat(sp, "dream_dim", 0.55f)

    fun pinnedList(): List<String> = pinned?.split('|')?.filter { it.isNotBlank() } ?: emptyList()
    fun dockList(): List<String> = dock?.split('|')?.filter { it.isNotBlank() } ?: emptyList()

    fun togglePinned(key: String) {
        val list = pinnedList().toMutableList()
        if (!list.remove(key)) list.add(key)
        pinned = list.joinToString("|")
    }

    fun toggleDock(key: String) {
        val list = dockList().toMutableList()
        if (!list.remove(key)) { if (list.size >= 7) list.removeAt(0); list.add(key) }
        dock = list.joinToString("|")
    }

    fun hiddenList(): List<String> = hidden?.split('|')?.filter { it.isNotBlank() } ?: emptyList()

    fun toggleHidden(key: String) {
        val list = hiddenList().toMutableList()
        if (!list.remove(key)) list.add(key)
        hidden = list.joinToString("|")
    }

    fun counts(): Map<String, Int> = launchCounts?.split('|')?.mapNotNull { entry ->
        val i = entry.lastIndexOf('=')
        if (i <= 0) null else entry.substring(0, i) to (entry.substring(i + 1).toIntOrNull() ?: 0)
    }?.toMap() ?: emptyMap()

    fun registerLaunch(key: String) {
        val map = counts().toMutableMap()
        map[key] = (map[key] ?: 0) + 1
        // Conserva solo las 40 apps más usadas para que la preferencia no crezca sin límite.
        launchCounts = map.entries.sortedByDescending { it.value }.take(40).joinToString("|") { "${it.key}=${it.value}" }
    }

    fun resetAll() {
        val keepWallpaper = wallpaperUri
        val keepPinned = pinned
        val keepDock = dock
        val keepNotes = notes
        val keepHidden = hidden
        val keepCounts = launchCounts
        val keepName = userName
        val keepCity = weatherCity
        val keepLat = weatherLat
        val keepLon = weatherLon
        sp.edit().clear().apply()
        themeMode = 0; accentIndex = 0; glassOpacity = 0.72f; cornerRadius = 24f
        clockStyle = 0; clockFont = 0; clockWeight = 1; clockScale = 1f; clockUseAccent = false
        clockCentered = false; use24h = true; showSeconds = false; showDate = true; showGreeting = true
        layoutMode = 0; showCalendar = true; showBattery = true; showNotes = true; showPinned = true
        iconShape = 0; iconSize = 48f; showLabels = true; taskbarFloating = true; taskbarCentered = true
        taskbarClock = 0; wallpaperDim = 0.30f; wallpaperBlur = 0f
        dreamShowDate = true; dreamShowBattery = true; dreamDim = 0.55f
        showWeather = true; showAgenda = false; showWorldClock = false; worldZone = "America/New_York"
        drawerMode = 0; wallpaperPreset = 0
        wallpaperUri = keepWallpaper; pinned = keepPinned; dock = keepDock; notes = keepNotes
        hidden = keepHidden; launchCounts = keepCounts; userName = keepName
        weatherCity = keepCity; weatherLat = keepLat; weatherLon = keepLon
    }

    companion object {
        const val PREFS = "envui"
    }
}
