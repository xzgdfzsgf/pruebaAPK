package com.envui.launcher

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale

data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val category: Int = -1,
    val system: Boolean = false,
) {
    val key: String get() = "$packageName/$activityName"
}

data class BatteryInfo(val level: Int, val charging: Boolean)

fun loadApps(pm: PackageManager, ownPackage: String): List<LauncherApp> {
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    return pm.queryIntentActivities(query, 0)
        .map {
            val info = it.activityInfo.applicationInfo
            LauncherApp(
                it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name,
                category = info.category,
                system = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }
        .filter { it.packageName != ownPackage }
        .distinctBy { it.key }
        .sortedBy { it.label.lowercase(Locale.getDefault()) }
}

/** Sugerencias iniciales para el escritorio y la barra en el primer arranque. */
fun suggestedKeys(apps: List<LauncherApp>, hints: List<String>, max: Int): List<String> {
    val picked = hints.mapNotNull { hint -> apps.firstOrNull { it.packageName.contains(hint, true) } }
        .distinctBy { it.key }
    return (picked.ifEmpty { apps }).take(max).map { it.key }
}

object IconCache {
    private val cache = HashMap<String, ImageBitmap>()

    fun get(context: Context, app: LauncherApp): ImageBitmap? {
        cache[app.key]?.let { return it }
        val drawable = runCatching {
            context.packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName))
        }.getOrNull() ?: runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
        ?: return null
        val size = 160
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        val image = bmp.asImageBitmap()
        cache[app.key] = image
        return image
    }

    fun clear() = cache.clear()
}

fun launchApp(context: Context, app: LauncherApp) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        })
    }.onFailure { toast(context, "No se pudo abrir ${app.label}") }
}

fun openAppInfo(context: Context, app: LauncherApp) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(context, "No se pudo abrir la información de la aplicación") }
}

fun uninstallApp(context: Context, app: LauncherApp) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(context, "Esta aplicación no se puede desinstalar") }
}

fun openSystemScreen(context: Context, action: String, fallback: String = Settings.ACTION_SETTINGS) {
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .recoverCatching { context.startActivity(Intent(fallback).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { toast(context, "Tu tablet no permite abrir esta pantalla de ajustes") }
}

fun readBattery(context: Context): BatteryInfo {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(percent.coerceIn(0, 100), charging)
}

/** Decodifica una imagen reduciéndola para no agotar la memoria con fotos de cámara. */
fun decodeSampled(context: Context, uriString: String, maxSide: Int): Bitmap? = runCatching {
    val uri = Uri.parse(uriString)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

/** Aplica la imagen elegida al fondo del sistema y/o a la pantalla de bloqueo. Se ejecuta fuera del hilo principal. */
fun applySystemWallpaper(context: Context, uriString: String, lock: Boolean, home: Boolean): Boolean = runCatching {
    val manager = WallpaperManager.getInstance(context)
    var flags = 0
    if (lock) flags = flags or WallpaperManager.FLAG_LOCK
    if (home) flags = flags or WallpaperManager.FLAG_SYSTEM
    if (flags == 0) return@runCatching false
    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
        manager.setStream(stream, null, true, flags)
    } ?: return@runCatching false
    true
}.getOrDefault(false)

fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

fun categoryOf(app: LauncherApp): String =
    CategoryNames[app.category] ?: if (app.system) "Sistema y herramientas" else "Otras"
