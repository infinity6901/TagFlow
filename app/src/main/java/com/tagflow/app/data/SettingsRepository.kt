package com.tagflow.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val theme: Int = 0, // 0 system, 1 light, 2 dark
    val dynamic: Boolean = true,
    val amoled: Boolean = false,
    val accent: Int = 0,
    val haptics: Boolean = true,
    val runInApp: Boolean = true
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val ctx: Context) {
    private object K {
        val theme = intPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic")
        val amoled = booleanPreferencesKey("amoled")
        val accent = intPreferencesKey("accent")
        val haptics = booleanPreferencesKey("haptics")
        val runInApp = booleanPreferencesKey("run_in_app")
    }

    val flow: Flow<AppSettings> = ctx.dataStore.data.map { p ->
        AppSettings(
            theme = p[K.theme] ?: 0,
            dynamic = p[K.dynamic] ?: true,
            amoled = p[K.amoled] ?: false,
            accent = p[K.accent] ?: 0,
            haptics = p[K.haptics] ?: true,
            runInApp = p[K.runInApp] ?: true
        )
    }

    suspend fun setTheme(v: Int) { ctx.dataStore.edit { it[K.theme] = v } }
    suspend fun setDynamic(v: Boolean) { ctx.dataStore.edit { it[K.dynamic] = v } }
    suspend fun setAmoled(v: Boolean) { ctx.dataStore.edit { it[K.amoled] = v } }
    suspend fun setAccent(v: Int) { ctx.dataStore.edit { it[K.accent] = v } }
    suspend fun setHaptics(v: Boolean) { ctx.dataStore.edit { it[K.haptics] = v } }
    suspend fun setRunInApp(v: Boolean) { ctx.dataStore.edit { it[K.runInApp] = v } }
}
