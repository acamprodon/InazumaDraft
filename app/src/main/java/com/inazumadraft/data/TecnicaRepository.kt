package com.inazumadraft.data

import android.os.SystemClock
import android.util.Log
import com.inazumadraft.InazumaDraftApp
import com.inazumadraft.model.Tecnica
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object TecnicaRepository {

    private const val SEED_DATA_URL = "https://acamprodon.github.io/InazumaDraft-data/tecnica.json"
    private const val REMOTE_RETRY_INTERVAL_MS = 60_000L

    private val initializer = AtomicBoolean(false)
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val seedMutex = Mutex()

    @Volatile
    private var cachedTecnicas: List<Tecnica> = emptyList()

    @Volatile
    private var remoteSeedSynced = false

    @Volatile
    private var lastRemoteSeedAttempt = 0L

    @Volatile
    private var remoteSeedInFlight = false

    val tecnicas: List<Tecnica>
        get() = cachedTecnicas

    fun initialize(app: InazumaDraftApp) {
        if (!initializer.compareAndSet(false, true)) return
        scope.launch {
            ensureSeedData(forceRefresh = false)
        }
    }

    suspend fun getTechniques(forceRefresh: Boolean = false): List<Tecnica> {
        ensureSeedData(forceRefresh)
        return cachedTecnicas
    }

    private suspend fun ensureSeedData(forceRefresh: Boolean) {
        var attemptRemote = forceRefresh

        seedMutex.withLock {
            if (forceRefresh) {
                remoteSeedSynced = false
            }

            if (remoteSeedSynced && !forceRefresh) {
                return
            }

            if (remoteSeedInFlight) {
                return
            }

            val now = SystemClock.elapsedRealtime()
            if (!forceRefresh) {
                if (now - lastRemoteSeedAttempt < REMOTE_RETRY_INTERVAL_MS) {
                    return
                }
            }

            lastRemoteSeedAttempt = now
            remoteSeedInFlight = true
            attemptRemote = true
        }

        if (!attemptRemote) return

        val result = downloadRemoteSeed()

        seedMutex.withLock {
            remoteSeedInFlight = false
            result.onSuccess { list ->
                if (list.isNotEmpty()) {
                    cachedTecnicas = list
                    remoteSeedSynced = true
                } else {
                    Log.w("TecnicaRepository", "Remote dataset contained no techniques")
                    if (forceRefresh) {
                        lastRemoteSeedAttempt = 0L
                    }
                }
            }.onFailure {
                Log.e("TecnicaRepository", "Unable to download techniques dataset", it)
                if (forceRefresh) {
                    lastRemoteSeedAttempt = 0L
                }
            }
        }
    }

    private suspend fun downloadRemoteSeed(): Result<List<Tecnica>> {
        return withContext(Dispatchers.IO) {
            runCatching { fetchRemoteSeed() }
        }
    }

    private fun fetchRemoteSeed(): List<Tecnica> {
        val connection = java.net.URL(SEED_DATA_URL).openConnection() as java.net.HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "InazumaDraft/1.0 (Android)")

            val code = connection.responseCode
            if (code != java.net.HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Techniques download failed with HTTP $code")
            }

            connection.inputStream.use { stream ->
                val json = stream.bufferedReader().use { it.readText() }
                parseSeed(json)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSeed(json: String): List<Tecnica> {
        val trimmed = json.trim()
        val array = when {
            trimmed.isEmpty() -> JSONArray()
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("techniques")
                    ?: obj.optJSONArray("tecnicas")
                    ?: obj.optJSONArray("data")
                    ?: throw IllegalStateException("Seed JSON does not contain a techniques array")
            }
            else -> throw IllegalStateException("Unrecognized techniques seed format")
        }

        val techniques = mutableListOf<Tecnica>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name", obj.optString("nombre"))
            if (name.isBlank()) continue

            val players = obj.optFlexibleStringList("players", "jugadores", "members")
            val power = obj.optInt("power", obj.optInt("poder"))
            val combined = obj.optBoolean("combined", obj.optBoolean("combinada", false))

            techniques += Tecnica(
                name = name,
                power = power,
                players = players,
                combined = combined
            )
        }
        return techniques
    }

}

private fun JSONObject.optFlexibleStringList(vararg keys: String): List<String> {
    for (key in keys) {
        if (!has(key)) continue
        val value = opt(key)
        when (value) {
            is JSONArray -> return value.toStringList()
            is String -> {
                val parts = value
                    .split(',', '|')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) {
                    return parts
                }
            }
            is Number -> return listOf(value.toString())
        }
    }
    return emptyList()
}

private fun JSONArray.toStringList(): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until length()) {
        result += optString(i)
    }
    return result
}
