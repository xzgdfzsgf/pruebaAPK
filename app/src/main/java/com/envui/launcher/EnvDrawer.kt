package com.envui.launcher

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer
import java.util.Locale

private fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.getDefault())

/** Menú Inicio: búsqueda, índice alfabético y accesos a ajustes. */
@Composable
fun StartMenu(
    apps: List<LauncherApp>,
    s: EnvSettings,
    p: EnvPalette,
    modifier: Modifier = Modifier,
    onOpen: (LauncherApp) -> Unit,
    onClose: () -> Unit,
    onPersonalize: () -> Unit,
) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val hiddenKeys = s.hiddenList()
    val visible = remember(apps, s.hidden) { apps.filter { it.key !in hiddenKeys } }
    val filtered = remember(query, visible) {
        val q = normalize(query.trim())
        if (q.isEmpty()) visible else visible.filter { normalize(it.label).contains(q) || it.packageName.contains(q, true) }
    }
    val mostUsed = remember(visible, s.launchCounts) {
        val counts = s.counts()
        visible.filter { (counts[it.key] ?: 0) > 0 }.sortedByDescending { counts[it.key] ?: 0 }.take(6)
    }
    val grouped: Map<String, List<LauncherApp>> = remember(filtered, query, s.drawerMode) {
        when {
            query.isNotBlank() -> mapOf("" to filtered)
            s.drawerMode == 1 -> filtered.groupBy { categoryOf(it) }.toSortedMap(compareBy<String> { if (it == "Otras") 1 else 0 }.thenBy { it })
            else -> filtered.groupBy { app ->
                val first = normalize(app.label).firstOrNull()?.uppercaseChar() ?: '#'
                if (first.isLetter()) first.toString() else "#"
            }.toSortedMap()
        }
    }
    val shape = RoundedCornerShape((s.cornerRadius + 6f).dp)
    Column(modifier.clip(shape).background(p.surfaceStrong).border(1.dp, p.border, shape).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Inicio", color = p.fg, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text("${visible.size} aplicaciones", color = p.muted, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            RoundButton(Icons.Default.Palette, "Personalizar", p, onPersonalize)
            Spacer(Modifier.width(6.dp))
            RoundButton(Icons.Default.Settings, "Ajustes del sistema", p) { openSystemScreen(ctx, Settings.ACTION_SETTINGS) }
            Spacer(Modifier.width(6.dp))
            RoundButton(Icons.Default.Close, "Cerrar", p, onClose)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(p.surface).border(1.dp, p.border, CircleShape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = p.muted)
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = p.fg, fontSize = 16.sp),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { filtered.firstOrNull()?.let(onOpen) }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Buscar aplicaciones", color = p.muted, fontSize = 16.sp)
                        inner()
                    }
                }
            )
            if (query.isNotEmpty()) {
                Icon(Icons.Default.Close, "Borrar búsqueda", tint = p.muted,
                    modifier = Modifier.size(20.dp).clip(CircleShape).clickable { query = "" })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("A a la Z", s.drawerMode == 0, p) { s.drawerMode = 0 }
            Chip("Por categorías", s.drawerMode == 1, p) { s.drawerMode = 1 }
        }
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Text("No hay aplicaciones que coincidan con “$query”.", color = p.muted, fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 24.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive((s.iconSize * 1.3f + 34f).dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (query.isBlank() && mostUsed.isNotEmpty()) {
                item(key = "header_most_used", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Más usadas", color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp))
                }
                items(mostUsed, key = { "most_" + it.key }) { app ->
                    Box(contentAlignment = Alignment.TopCenter) { AppIcon(app, s, p) { onOpen(app) } }
                }
            }
            grouped.forEach { (letter, list) ->
                if (letter.isNotEmpty()) {
                    item(key = "header_group_$letter", span = { GridItemSpan(maxLineSpan) }) {
                        Text(letter, color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp))
                    }
                }
                items(list, key = { it.key }) { app ->
                    Box(contentAlignment = Alignment.TopCenter) { AppIcon(app, s, p) { onOpen(app) } }
                }
            }
        }
        Text("Mantén presionada una aplicación para fijarla en el escritorio o anclarla a la barra.",
            color = p.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, p: EnvPalette, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(p.surface).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, label, tint = p.fg, modifier = Modifier.size(20.dp)) }
}
