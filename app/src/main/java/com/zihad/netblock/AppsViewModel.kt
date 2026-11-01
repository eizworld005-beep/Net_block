package com.zihad.netblock

import android.app.Application
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap)

class AppsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _blocked = MutableStateFlow(prefs.blocked)
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { loadApps() }
    }

    private fun loadApps() {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val items = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != ctx.packageName }
            .mapNotNull { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    AppItem(
                        label = pm.getApplicationLabel(info).toString(),
                        pkg = pkg,
                        icon = pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
        _apps.value = items
        _loading.value = false
    }

    fun setBlocked(pkg: String, block: Boolean) {
        val next = _blocked.value.toMutableSet()
        if (block) next.add(pkg) else next.remove(pkg)
        _blocked.value = next
        prefs.blocked = next
    }
}
