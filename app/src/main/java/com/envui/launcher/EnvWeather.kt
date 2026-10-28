package com.envui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

data class WeatherNow(
    val temp: Double,
    val feels: Double,
    val humidity: Int,
    val code: Int,
    val isDay: Boolean,
    val max: Double,
    val min: Double,
)

data class GeoResult(val name: String, val country: String, val lat: Double, val lon: Double)

private fun httpGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    return try {
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
        conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

/** Consulta el clima actual en Open-Meteo. Debe llamarse fuera del hilo principal. */
fun fetchWeatherJson(lat: Float, lon: Float): String? = runCatching {
    httpGet(
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto"
    )
}.getOrNull()

fun parseWeather(json: String?): WeatherNow? = runCatching {
    val root = JSONObject(json ?: return null)
    val current = root.getJSONObject("current")
    val daily = root.getJSONObject("daily")
    WeatherNow(
        temp = current.getDouble("temperature_2m"),
        feels = current.optDouble("apparent_temperature", current.getDouble("temperature_2m")),
        humidity = current.optInt("relative_humidity_2m", 0),
        code = current.optInt("weather_code", 0),
        isDay = current.optInt("is_day", 1) == 1,
        max = daily.getJSONArray("temperature_2m_max").getDouble(0),
        min = daily.getJSONArray("temperature_2m_min").getDouble(0),
    )
}.getOrNull()

/** Busca una ciudad por nombre con el geocodificador de Open-Meteo. */
fun geocodeCity(name: String): GeoResult? = runCatching {
    val q = URLEncoder.encode(name.trim(), "UTF-8")
    val root = JSONObject(httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=es&format=json"))
    val results = root.optJSONArray("results") ?: return null
    if (results.length() == 0) return null
    val first = results.getJSONObject(0)
    GeoResult(first.getString("name"), first.optString("country", ""), first.getDouble("latitude"), first.getDouble("longitude"))
}.getOrNull()

fun weatherText(code: Int): String = when (code) {
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Neblina"
    51, 53, 55, 56, 57 -> "Llovizna"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Lluvia"
    71, 73, 75, 77, 85, 86 -> "Nieve"
    95, 96, 99 -> "Tormenta"
    else -> "Variable"
}

fun weatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay
    2 -> Icons.Default.WbCloudy
    3, 45, 48 -> Icons.Default.Cloud
    in 51..67, in 80..82 -> Icons.Default.Umbrella
    in 71..77, 85, 86 -> Icons.Default.AcUnit
    95, 96, 99 -> Icons.Default.Thunderstorm
    else -> Icons.Default.WbCloudy
}

@Composable
fun WeatherWidget(s: EnvSettings, p: EnvPalette, modifier: Modifier = Modifier) {
    val w = parseWeather(s.weatherCache)
    GlassCard(s, p, modifier) {
        if (w == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudOff, null, tint = p.muted, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(s.weatherCity ?: "Clima", color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Sin datos todavía. Se actualiza al tener conexión.", color = p.muted, fontSize = 13.sp)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(weatherIcon(w.code, w.isDay), null, tint = p.accent, modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${w.temp.roundToInt()}°", color = p.fg, fontSize = 34.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp)
                    Text(weatherText(w.code), color = p.fg, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(s.weatherCity ?: "", color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Máx. ${w.max.roundToInt()}°  Mín. ${w.min.roundToInt()}°", color = p.muted, fontSize = 12.sp)
                    Text("Sensación ${w.feels.roundToInt()}°  ·  ${w.humidity} %", color = p.muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
