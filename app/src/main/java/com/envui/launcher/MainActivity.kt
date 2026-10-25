package com.envui.launcher

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.BatteryManager
import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF07101F)
private val Ice = Color(0xFFEDF5FF)
private val Muted = Color(0xFF9EB1CC)
private val Accent = Color(0xFF70C8FF)
private val Glass = Color(0xCC172A45)

data class LauncherApp(val label: String, val packageName: String, val activityName: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 16, 31)
        window.navigationBarColor = android.graphics.Color.rgb(7, 16, 31)
        setContent { EnvDesktop() }
    }
}

private fun getApps(pm: PackageManager): List<LauncherApp> {
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    return pm.queryIntentActivities(query, 0).map {
        LauncherApp(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name)
    }.distinctBy { it.packageName to it.activityName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
}

@Composable
private fun EnvDesktop() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("envui", 0) }
    val apps = remember { getApps(ctx.packageManager) }
    val batteryManager = remember { ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    var battery by remember { mutableIntStateOf(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)) }
    var showMenu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var light by remember { mutableStateOf(prefs.getBoolean("light", false)) }
    var wallpaperUri by remember { mutableStateOf(prefs.getString("wallpaper_uri", null)) }
    var wallpaperBitmap by remember(wallpaperUri) {
        mutableStateOf(wallpaperUri?.let { uri ->
            runCatching {
                ctx.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    // Read bounds first to avoid decoding full-resolution camera images.
                    val bounds = ctx.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                        options.outWidth to options.outHeight
                    } ?: (0 to 0)
                    val sample = generateSequence(1) { it * 2 }.takeWhile {
                        bounds.first / it > 2200 || bounds.second / it > 2200
                    }.lastOrNull()?.times(2) ?: 1
                    BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }
            }.getOrNull()
        })
    }
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            wallpaperUri = uri.toString()
            prefs.edit().putString("wallpaper_uri", uri.toString()).apply()
        }
    }
    var clock by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { clock = Date(); battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY); delay(15_000) } }
    BackHandler(showMenu || showSettings) { showMenu = false; showSettings = false }
    val surface = if (light) Color(0xDDE9F2FF) else Glass
    val foreground = if (light) Color(0xFF10223B) else Ice
    val secondary = if (light) Color(0xFF526780) else Muted
    val start = if (light) Color(0xFFB6D7F7) else Color(0xFF071224)
    val end = if (light) Color(0xFFF4F7FF) else Color(0xFF101B38)

    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(start, end, if (light) Color(0xFFD4E9FA) else Color(0xFF183F68))))) {
        val wide = maxWidth > maxHeight
        if (wallpaperBitmap != null) {
            Image(bitmap = wallpaperBitmap!!, contentDescription = "Fondo de pantalla personalizado",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (light) 0.24f else 0.40f)))
        }
        // Abstract ambient lights are shown only when no photo has been selected.
        if (wallpaperBitmap == null) Box(Modifier.size(if (wide) 390.dp else 290.dp).offset(x = maxWidth * .36f, y = (-90).dp)
            .background(Brush.radialGradient(listOf(Color(0x664EA6F5), Color.Transparent)), CircleShape))
        if (wallpaperBitmap == null) Box(Modifier.size(280.dp).offset(x = (-90).dp, y = maxHeight * .45f)
            .background(Brush.radialGradient(listOf(Color(0x664C65D9), Color.Transparent)), CircleShape))

        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 30.dp else 18.dp, vertical = 17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(9.dp))
                Text("ENV", color = foreground, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 2.sp)
                Text(" UI", color = Accent, fontWeight = FontWeight.Medium, fontSize = 19.sp)
                Spacer(Modifier.weight(1f))
                Text("WORKSPACE  /  01", color = secondary, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.height(if (wide) 24.dp else 16.dp))
            if (wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1.5f)) {
                        Hero(clock, foreground, secondary, wide)
                        Spacer(Modifier.height(24.dp))
                        Text("TU ESPACIO", color = secondary, fontSize = 10.sp, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
                        QuickApps(apps, foreground, surface) { app -> launchApp(ctx, app) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Widget("CENTRO DE PRODUCTIVIDAD", Icons.Default.Dashboard, surface, foreground, secondary) {
                            Text("Todo listo para empezar.", color = foreground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tu escritorio, a tu manera.", color = secondary, fontSize = 12.sp)
                        }
                        Widget("HOY", Icons.Default.CalendarMonth, surface, foreground, secondary) {
                            Text(SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "EC")).format(clock).replaceFirstChar { it.uppercase() }, color = foreground, fontSize = 16.sp)
                            Text("Un gran día para crear algo nuevo.", color = secondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Hero(clock, foreground, secondary, wide)
                Spacer(Modifier.height(26.dp))
                Text("ACCESOS RÁPIDOS", color = secondary, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                QuickApps(apps, foreground, surface) { app -> launchApp(ctx, app) }
                Spacer(Modifier.height(20.dp))
                Widget("CENTRO DE PRODUCTIVIDAD", Icons.Default.Dashboard, surface, foreground, secondary) {
                    Text("Todo listo para empezar.", color = foreground, fontSize = 16.sp)
                    Text("Tu escritorio, a tu manera.", color = secondary, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
            }
            Taskbar(surface, foreground, secondary, clock, battery, { showMenu = true }, { showSettings = true })
        }
        if (showMenu) {
            Dialog(onDismissRequest = { showMenu = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Column(Modifier.fillMaxWidth(if (wide) .68f else .94f).fillMaxHeight(.82f)
                    .clip(RoundedCornerShape(28.dp)).background(if (light) Color(0xFFF2F7FF) else Color(0xFF14233A))
                    .border(1.dp, Color(0x448CCEFF), RoundedCornerShape(28.dp)).padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Accent)
                        Spacer(Modifier.width(10.dp))
                        Text("Inicio ENV", color = foreground, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showMenu = false }) { Icon(Icons.Default.Close, "Cerrar", tint = foreground) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = secondary)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(query, { query = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = foreground, fontSize = 15.sp),
                            decorationBox = { inner -> Box { if (query.isEmpty()) Text("Buscar aplicaciones...", color = secondary); inner() } }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("TODAS LAS APLICACIONES  ·  ${apps.size}", color = secondary, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    val filtered = remember(query, apps) { apps.filter { it.label.contains(query, true) } }
                    LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), verticalArrangement = Arrangement.spacedBy(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered, key = { it.packageName + it.activityName }) { app ->
                            AppTile(app, foreground, surface) { launchApp(ctx, app); showMenu = false }
                        }
                    }
                }
            }
        }
        if (showSettings) {
            AlertDialog(onDismissRequest = { showSettings = false }, title = { Text("Personalizar ENV UI") }, text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tema claro", modifier = Modifier.weight(1f))
                        Switch(checked = light, onCheckedChange = { light = it; prefs.edit().putBoolean("light", it).apply() })
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Fondo de pantalla", fontWeight = FontWeight.SemiBold)
                    Text("Escoge una imagen de tu tablet. Se ajustará a horizontal y vertical.", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { wallpaperPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Elegir imagen")
                    }
                    if (wallpaperUri != null) {
                        TextButton(onClick = {
                            wallpaperUri = null
                            prefs.edit().remove("wallpaper_uri").apply()
                        }) { Text("Restablecer fondo original") }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Para elegir ENV UI como inicio, abre los ajustes de aplicaciones predeterminadas.", fontSize = 13.sp)
                }
            }, confirmButton = {
                TextButton(onClick = { showSettings = false; ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) { Text("Elegir launcher") }
            }, dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Cerrar") } })
        }
    }
}

@Composable
private fun Hero(now: Date, fg: Color, muted: Color, wide: Boolean) {
    Text("BIENVENIDO A TU ESPACIO", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(12.dp))
    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), color = fg, fontSize = (if (wide) 76 else 64).sp, fontWeight = FontWeight.Light, lineHeight = (if (wide) 80 else 68).sp)
    Text(SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "EC")).format(now).replaceFirstChar { it.uppercase() }, color = muted, fontSize = 15.sp)
    Spacer(Modifier.height(17.dp))
    Box(Modifier.width(54.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
}

@Composable
private fun QuickApps(apps: List<LauncherApp>, fg: Color, surface: Color, launch: (LauncherApp) -> Unit) {
    val selected = apps.filter { app -> listOf("chrome", "settings", "files", "camera", "youtube", "gmail").any { app.packageName.contains(it, true) } }.take(6)
        .ifEmpty { apps.take(6) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        selected.forEach { app -> AppTile(app, fg, surface) { launch(app) } }
    }
}

@Composable
private fun AppTile(app: LauncherApp, fg: Color, surface: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.width(90.dp).clip(RoundedCornerShape(19.dp)).background(surface).clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val drawable = remember(app.packageName) { runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull() }
        if (drawable != null) {
            androidx.compose.foundation.Image(bitmap = remember(drawable) { drawableToBitmap(drawable) }, contentDescription = null, modifier = Modifier.size(36.dp))
        } else Icon(Icons.Default.Apps, null, tint = Accent, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text(app.label, color = fg, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): androidx.compose.ui.graphics.ImageBitmap {
    val bmp = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    drawable.setBounds(0, 0, 96, 96)
    drawable.draw(canvas)
    return bmp.asImageBitmap()
}

@Composable
private fun Widget(title: String, icon: ImageVector, surface: Color, fg: Color, muted: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(surface).border(1.dp, Color(0x338CCEFF), RoundedCornerShape(23.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(9.dp))
            Text(title, color = muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
        }
        Spacer(Modifier.height(17.dp))
        content()
    }
}

@Composable
private fun Taskbar(surface: Color, fg: Color, muted: Color, now: Date, battery: Int, openMenu: () -> Unit, openSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(66.dp).shadow(16.dp, RoundedCornerShape(23.dp)).clip(RoundedCornerShape(23.dp)).background(surface)
        .border(1.dp, Color(0x448CCEFF), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = openMenu) { Icon(Icons.Default.Window, "Inicio", tint = Accent, modifier = Modifier.size(27.dp)) }
        IconButton(onClick = openMenu) { Icon(Icons.Default.Search, "Buscar", tint = fg) }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.width(1.dp).height(24.dp).background(muted.copy(alpha = .35f)))
        IconButton(onClick = openMenu) { Icon(Icons.Default.Apps, "Aplicaciones", tint = fg) }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Wifi, "Wi-Fi", tint = muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Default.BatteryFull, "Batería", tint = muted, modifier = Modifier.size(17.dp))
        Text("${battery.coerceIn(0,100)}%", color = muted, fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), color = fg, fontSize = 12.sp)
        IconButton(onClick = openSettings) { Icon(Icons.Default.Tune, "Personalizar", tint = fg) }
    }
}

private fun launchApp(context: android.content.Context, app: LauncherApp) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        })
    }.onFailure { android.widget.Toast.makeText(context, "No se pudo abrir ${app.label}", android.widget.Toast.LENGTH_SHORT).show() }
}
