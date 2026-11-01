package com.zihad.netblock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zihad.netblock.AppItem
import com.zihad.netblock.AppsViewModel

private enum class Filter { All, Blocked }

@Composable
fun HomeScreen(
    vm: AppsViewModel,
    running: Boolean,
    onToggleProtection: () -> Unit,
    onSetBlocked: (String, Boolean) -> Unit
) {
    val apps by vm.apps.collectAsState()
    val blocked by vm.blocked.collectAsState()
    val loading by vm.loading.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.All) }

    val visible = remember(apps, blocked, query, filter) {
        apps.filter { app ->
            (filter == Filter.All || app.pkg in blocked) &&
                (query.isBlank() || app.label.contains(query, ignoreCase = true))
        }
    }

    Scaffold(containerColor = Ink) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Net Block", style = MaterialTheme.typography.headlineMedium, color = TextMain)
                Spacer(Modifier.height(16.dp))
                StatusCard(running = running, blockedCount = blocked.size, onToggle = onToggleProtection)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Panel,
                        unfocusedContainerColor = Panel,
                        focusedBorderColor = Signal,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        cursorColor = Signal
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filter == Filter.All,
                        onClick = { filter = Filter.All },
                        label = { Text("All apps (${apps.size})") },
                        colors = chipColors()
                    )
                    FilterChip(
                        selected = filter == Filter.Blocked,
                        onClick = { filter = Filter.Blocked },
                        label = { Text("Blocked (${blocked.size})") },
                        colors = chipColors()
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            when {
                loading -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Signal)
                    }
                }
                visible.isEmpty() -> item {
                    val noBlocked = filter == Filter.Blocked && query.isBlank()
                    EmptyState(
                        title = if (noBlocked) "No blocked apps yet" else "No apps found",
                        body = if (noBlocked) "Switch to All apps and turn on the apps you want to cut off."
                        else "Try a different name."
                    )
                }
                else -> items(visible, key = { it.pkg }) { app ->
                    AppRow(
                        app = app,
                        isBlocked = app.pkg in blocked,
                        onChange = { onSetBlocked(app.pkg, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Panel,
    labelColor = TextMuted,
    selectedContainerColor = Signal,
    selectedLabelColor = Ink
)

@Composable
private fun StatusCard(running: Boolean, blockedCount: Int, onToggle: () -> Unit) {
    val accent by animateColorAsState(if (running) Coral else Signal, label = "accent")
    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (running) "Blocking is on" else "Blocking is off",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMain
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        running -> "$blockedCount ${if (blockedCount == 1) "app has" else "apps have"} no internet."
                        blockedCount == 0 -> "Choose the apps to block, then turn it on."
                        else -> "$blockedCount ${if (blockedCount == 1) "app is" else "apps are"} ready to block."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = if (running) "Turn blocking off" else "Turn blocking on",
                    tint = Ink,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: AppItem, isBlocked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Panel,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!isBlocked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isBlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Block, null, tint = Coral, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Internet blocked", style = MaterialTheme.typography.bodySmall, color = Coral)
                    }
                } else {
                    Text(
                        app.pkg,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(
                checked = isBlocked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Ink,
                    checkedTrackColor = Coral,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = PanelHigh,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextMain)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}
