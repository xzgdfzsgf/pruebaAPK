package com.envui.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val resumeTick: MutableIntState = mutableIntStateOf(0)
    private val homeTick: MutableIntState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(EnvSettings.PREFS, MODE_PRIVATE)
        // Migración desde la versión 0.3, que guardaba el tema como booleano.
        if (prefs.contains("light")) {
            val wasLight = prefs.getBoolean("light", false)
            prefs.edit().remove("light").putInt("theme_mode", if (wasLight) 1 else 0).apply()
        }
        val settings = EnvSettings(prefs)
        setContent { EnvRoot(settings, resumeTick.intValue, homeTick.intValue) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue += 1
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pulsar Inicio estando en el launcher cierra los paneles abiertos.
        homeTick.intValue += 1
    }
}

@Composable
private fun EnvRoot(s: EnvSettings, resumeTick: Int, homeTick: Int) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var battery by remember { mutableStateOf(BatteryInfo(100, false)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showStart by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDream by remember { mutableStateOf(false) }
    var showControl by remember { mutableStateOf(false) }
    var calendarAllowed by remember { mutableStateOf(hasCalendarPermission(ctx)) }
    var agenda by remember { mutableStateOf(emptyList<AgendaEvent>()) }
    var agendaTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(resumeTick) {
        val loaded = withContext(Dispatchers.Default) { loadApps(ctx.packageManager, ctx.packageName) }
        apps = loaded
        if (s.pinned == null && loaded.isNotEmpty()) {
            s.pinned = suggestedKeys(loaded, listOf("chrome", "gmail", "youtube", "files", "documents", "camera", "photos", "gallery", "calendar", "maps", "drive", "settings"), 8).joinToString("|")
        }
        if (s.dock == null && loaded.isNotEmpty()) {
            s.dock = suggestedKeys(loaded, listOf("chrome", "files", "gmail", "camera"), 4).joinToString("|")
        }
        while (true) {
            battery = readBattery(ctx)
            delay(30_000)
        }
    }
    LaunchedEffect(s.showSeconds, resumeTick) {
        while (true) {
            now = System.currentTimeMillis()
            val step = if (s.showSeconds) 1000L - now % 1000L else 60_000L - now % 60_000L
            delay(step.coerceAtLeast(50L))
        }
    }
    LaunchedEffect(homeTick) { showStart = false; showSettings = false; showDream = false; showControl = false }

    // Clima: se actualiza cada 30 minutos, o de inmediato si cambió la ciudad.
    LaunchedEffect(resumeTick, s.showWeather, s.weatherLat, s.weatherLon) {
        if (!s.showWeather) return@LaunchedEffect
        while (true) {
            if (System.currentTimeMillis() - s.weatherTime > 30 * 60 * 1000L || s.weatherCache == null) {
                val json = withContext(Dispatchers.IO) { fetchWeatherJson(s.weatherLat, s.weatherLon) }
                if (parseWeather(json) != null) {
                    s.weatherCache = json
                    s.weatherTime = System.currentTimeMillis()
                }
            }
            delay(5 * 60 * 1000L)
        }
    }

    // Agenda: se recarga al volver al launcher y cada 10 minutos.
    LaunchedEffect(resumeTick, s.showAgenda, calendarAllowed, agendaTick) {
        calendarAllowed = hasCalendarPermission(ctx)
        if (!s.showAgenda || !calendarAllowed) return@LaunchedEffect
        while (true) {
            agenda = withContext(Dispatchers.IO) { loadAgenda(ctx) }
            delay(10 * 60 * 1000L)
        }
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarAllowed = granted
        agendaTick += 1
        if (!granted) toast(ctx, "Sin permiso no puedo mostrar tus eventos")
    }
    val requestCalendar: () -> Unit = {
        if (hasCalendarPermission(ctx)) { calendarAllowed = true; agendaTick += 1 }
        else calendarPermission.launch(android.Manifest.permission.READ_CALENDAR)
    }

    val wallpaper by produceState<ImageBitmap?>(initialValue = null, s.wallpaperUri) {
        val uri = s.wallpaperUri
        value = if (uri == null) null else withContext(Dispatchers.IO) { decodeSampled(ctx, uri, 2200)?.asImageBitmap() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            s.wallpaperUri = uri.toString()
        }
    }

    val dark = when (s.themeMode) {
        1 -> false
        2 -> isSystemInDarkTheme()
        else -> true
    }
    val accent = AccentOptions[s.accentIndex.coerceIn(0, AccentOptions.size - 1)].color
    val p = remember(dark, accent, s.glassOpacity) { buildPalette(dark, accent, s.glassOpacity) }
    val pinnedApps = remember(apps, s.pinned) { s.pinnedList().mapNotNull { k -> apps.firstOrNull { it.key == k } } }
    val dockApps = remember(apps, s.dock) { s.dockList().mapNotNull { k -> apps.firstOrNull { it.key == k } } }
    val open: (LauncherApp) -> Unit = { app ->
        launchApp(ctx, app)
        s.registerLaunch(app.key)
        showStart = false
    }

    BackHandler(enabled = true) {
        when {
            showDream -> showDream = false
            showControl -> showControl = false
            showSettings -> showSettings = false
            showStart -> showStart = false
        }
    }

    val scheme = if (dark) darkColorScheme(primary = accent, surface = Color(0xFF14233A), surfaceContainer = Color(0xFF14233A))
    else lightColorScheme(primary = accent, surface = Color(0xFFF7FAFF), surfaceContainer = Color(0xFFF7FAFF))

    MaterialTheme(colorScheme = scheme) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    GradientPresets[s.wallpaperPreset.coerceIn(0, GradientPresets.size - 1)].let { if (dark) it.darkColors else it.lightColors }
                )
            )
        ) {
            val wide = maxWidth > maxHeight
            val screenWidth = maxWidth
            val bmp = wallpaper
            if (bmp != null) {
                Image(
                    bmp, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().then(if (s.wallpaperBlur > 0.5f) Modifier.blur(s.wallpaperBlur.dp) else Modifier)
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = s.wallpaperDim)))
            } else {
                Box(
                    Modifier.size(if (wide) 520.dp else 380.dp).offset(x = maxWidth * 0.45f, y = (-120).dp)
                        .background(Brush.radialGradient(listOf(p.glow, Color.Transparent)), CircleShape)
                )
                Box(
                    Modifier.size(420.dp).offset(x = (-140).dp, y = maxHeight * 0.5f)
                        .background(Brush.radialGradient(listOf(p.glow.copy(alpha = 0.22f), Color.Transparent)), CircleShape)
                )
            }

            Column(
                Modifier.fillMaxSize().systemBarsPadding()
                    .padding(horizontal = if (wide) 36.dp else 20.dp, vertical = 14.dp)
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { if (total < -120f) showStart = true },
                            onVerticalDrag = { _, amount -> total += amount }
                        )
                    }
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    DesktopContent(
                        DesktopData(now, battery, pinnedApps, agenda, calendarAllowed, open, requestCalendar),
                        s, p, wide, screenWidth - (if (wide) 72.dp else 40.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Taskbar(now, s, p, battery, dockApps,
                    onStart = { showSettings = false; showControl = false; showStart = !showStart },
                    onSettings = { showStart = false; showControl = false; showSettings = !showSettings },
                    onControl = { showStart = false; showSettings = false; showControl = !showControl },
                    onOpen = open)
            }

            // Capa oscura detrás de los paneles.
            val anyPanel = showStart || showSettings || showControl
            AnimatedVisibility(anyPanel, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            showStart = false; showSettings = false; showControl = false
                        }
                )
            }

            AnimatedVisibility(
                showStart,
                enter = slideInVertically { it / 3 } + fadeIn(),
                exit = slideOutVertically { it / 3 } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    Modifier.fillMaxWidth().systemBarsPadding().imePadding().padding(start = 14.dp, end = 14.dp, bottom = 96.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    StartMenu(
                        apps, s, p,
                        modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth().fillMaxHeight(if (wide) 0.86f else 0.78f),
                        onOpen = open,
                        onClose = { showStart = false },
                        onPersonalize = { showStart = false; showSettings = true }
                    )
                }
            }

            AnimatedVisibility(
                showSettings,
                enter = if (wide) slideInHorizontally { it / 2 } + fadeIn() else slideInVertically { it / 3 } + fadeIn(),
                exit = if (wide) slideOutHorizontally { it / 2 } + fadeOut() else slideOutVertically { it / 3 } + fadeOut(),
                modifier = Modifier.align(if (wide) Alignment.CenterEnd else Alignment.BottomCenter)
            ) {
                Box(Modifier.systemBarsPadding().padding(12.dp)) {
                    SettingsPanel(
                        s, p, apps,
                        modifier = if (wide) Modifier.width((screenWidth * 0.42f).coerceIn(340.dp, 480.dp)).fillMaxHeight()
                        else Modifier.fillMaxWidth().fillMaxHeight(0.72f),
                        onRequestCalendar = requestCalendar,
                        onPickWallpaper = { picker.launch(arrayOf("image/*")) },
                        onPreviewDream = { showSettings = false; showDream = true },
                        onClose = { showSettings = false }
                    )
                }
            }

            AnimatedVisibility(
                showControl,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Box(Modifier.systemBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 96.dp)) {
                    ControlCenter(s, p, battery, dark, Modifier.width(if (wide) 400.dp else (screenWidth - 28.dp).coerceAtMost(420.dp)))
                }
            }

            if (showDream) {
                Box(Modifier.fillMaxSize().clickable { showDream = false }) {
                    AndroidView(factory = { ReposoView(it) }, modifier = Modifier.fillMaxSize())
                    Text(
                        "Toca para salir de la vista previa", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 18.dp)
                    )
                }
            }
        }
    }
}
