package com.envui.launcher

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class StorageInfo(val usedGb: Double, val totalGb: Double)

fun readStorage(): StorageInfo = runCatching {
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.blockCountLong * stat.blockSizeLong
    val free = stat.availableBlocksLong * stat.blockSizeLong
    StorageInfo((total - free) / 1e9, total / 1e9)
}.getOrDefault(StorageInfo(0.0, 0.0))

private fun torchCameraId(context: Context): String? = runCatching {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    cm.cameraIdList.firstOrNull { id ->
        cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
}.getOrNull()

private fun setTorch(context: Context, on: Boolean): Boolean = runCatching {
    val id = torchCameraId(context) ?: return false
    (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).setTorchMode(id, on)
    true
}.getOrDefault(false)

/** Centro de control rápido, similar al panel de Windows 11. */
@Composable
fun ControlCenter(s: EnvSettings, p: EnvPalette, battery: BatteryInfo, dark: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var torch by remember { mutableStateOf(false) }
    val hasTorch = remember { torchCameraId(ctx) != null }
    val storage = remember { readStorage() }
    val shape = RoundedCornerShape((s.cornerRadius + 6f).dp)

    Column(modifier.clip(shape).background(p.surfaceStrong).border(1.dp, p.border, shape).padding(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(Icons.Default.Wifi, "Wi‑Fi", false, p, Modifier.weight(1f)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) openSystemScreen(ctx, Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS)
                else openSystemScreen(ctx, Settings.ACTION_WIFI_SETTINGS)
            }
            QuickTile(Icons.Default.Bluetooth, "Bluetooth", false, p, Modifier.weight(1f)) {
                openSystemScreen(ctx, Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            QuickTile(if (dark) Icons.Default.DarkMode else Icons.Default.LightMode, if (dark) "Oscuro" else "Claro", dark, p, Modifier.weight(1f)) {
                s.themeMode = if (dark) 1 else 0
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(if (torch) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff, "Linterna", torch, p, Modifier.weight(1f)) {
                if (!hasTorch) toast(ctx, "Esta tablet no tiene linterna")
                else if (setTorch(ctx, !torch)) torch = !torch
                else toast(ctx, "No se pudo usar la linterna")
            }
            QuickTile(Icons.Default.NotificationsOff, "No molestar", false, p, Modifier.weight(1f)) {
                openSystemScreen(ctx, "android.settings.ZEN_MODE_SETTINGS", Settings.ACTION_SOUND_SETTINGS)
            }
            QuickTile(Icons.Default.BrightnessMedium, "Pantalla", false, p, Modifier.weight(1f)) {
                openSystemScreen(ctx, Settings.ACTION_DISPLAY_SETTINGS)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VolumeUp, "Volumen", tint = p.fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0) }
                },
                valueRange = 0f..maxVol.toFloat(),
                colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.border),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text("${(volume / maxVol * 100).roundToInt()} %", color = p.muted, fontSize = 12.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(12.dp))
        InfoBar("Batería", battery.level / 100f, "${battery.level} %" + if (battery.charging) " · cargando" else "", p)
        Spacer(Modifier.height(10.dp))
        if (storage.totalGb > 0) {
            InfoBar("Almacenamiento", (storage.usedGb / storage.totalGb).toFloat(),
                "${storage.usedGb.roundToInt()} de ${storage.totalGb.roundToInt()} GB", p)
            Spacer(Modifier.height(14.dp))
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                openSystemScreen(ctx, Settings.ACTION_SETTINGS)
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, null, tint = p.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Todos los ajustes del sistema", color = p.fg, fontSize = 14.sp)
        }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, active: Boolean, p: EnvPalette, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (active) p.accent else p.surface)
            .border(1.dp, if (active) p.accent else p.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (active) p.onAccent else p.fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (active) p.onAccent else p.fg, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoBar(label: String, fraction: Float, value: String, p: EnvPalette) {
    Column {
        Row {
            Text(label, color = p.fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(value, color = p.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(p.border)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(p.accent))
        }
    }
}

/** Abre una app de reloj o alarmas si el sistema la ofrece. */
fun openClockApp(context: Context) {
    runCatching {
        context.startActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(context, "No hay una app de reloj disponible") }
}
