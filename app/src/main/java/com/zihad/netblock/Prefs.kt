package com.zihad.netblock

import android.content.Context

/** Stores which apps are blocked, so the choice survives restarts. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("netblock", Context.MODE_PRIVATE)

    var blocked: Set<String>
        get() = sp.getStringSet("blocked", emptySet())?.toSet() ?: emptySet()
        set(value) { sp.edit().putStringSet("blocked", HashSet(value)).apply() }
}
