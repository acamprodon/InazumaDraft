package com.inazumadraft.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.inazumadraft.data.local.InazumaDatabase
import com.inazumadraft.data.local.PlayerDao
import com.inazumadraft.data.local.PlayerEntity
import com.inazumadraft.model.Player
import com.inazumadraft.model.PlayerImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.jvm.Volatile
import org.json.JSONArray
import org.json.JSONObject

object PlayerRepository {

 private const val DATABASE_NAME = "inazuma_players.db"
 private const val SEED_DATA_URL = "https://acamprodon.github.io/InazumaDraft-data/players.json"
 private const val REMOTE_RETRY_INTERVAL_MS = 60_000L

 private lateinit var applicationContext: Context
 private lateinit var database: InazumaDatabase
 private lateinit var dao: PlayerDao

 private val seedMutex = Mutex()

 @Volatile
 private var remoteSeedSynced = false

 @Volatile
 private var lastRemoteSeedAttempt = 0L

 @Volatile
 private var remoteSeedInFlight = false

 fun initialize(context: Context) {
  if (::dao.isInitialized) return

  applicationContext = context.applicationContext
  database = Room.databaseBuilder(
   applicationContext,
   InazumaDatabase::class.java,
   DATABASE_NAME
  ).fallbackToDestructiveMigration().build()
  dao = database.playerDao()
 }

 suspend fun getPlayers(selectedSeasons: List<String> = emptyList()): List<Player> {
  ensureInitialized()
  ensureSeedData()
  val entities = seedMutex.withLock {
   withContext(Dispatchers.IO) { dao.getAll() }
  }
  val filter = selectedSeasons.map { it.uppercase() }.toSet()
  return entities
   .asSequence()
   .filter { filter.isEmpty() || it.seasons.any { season -> season.uppercase() in filter } }
   .map { it.toDomain(applicationContext) }
   .toList()
 }

 suspend fun deletePlayer(playerId: Long) {
  ensureInitialized()
  seedMutex.withLock {
   withContext(Dispatchers.IO) { dao.deleteById(playerId) }
  }
 }

 @VisibleForTesting
 internal suspend fun overwritePlayers(players: List<PlayerEntity>) {
  ensureInitialized()
  seedMutex.withLock {
   withContext(Dispatchers.IO) {
    dao.insertAll(players)
   }
  }
 }

 private fun ensureInitialized() {
  check(::dao.isInitialized) { "PlayerRepository.initialize(context) must be called before use" }
 }

 private fun PlayerEntity.toDomain(context: Context): Player {
  return Player(
   id = id,
   name = name,
   nickname = nickname,
   position = position,
   element = context.resolveDrawable(elementRef),
   kick = kick,
   speed = speed,
   control = control,
   defense = defense,
   image = context.resolvePlayerImage(imageRef),
   season = seasons,
   secondaryPositions = secondaryPositions
  )
 }

 private fun Context.resolveDrawable(reference: String): Int {
  val trimmed = reference.trim()
  if (trimmed.isEmpty()) return 0
  trimmed.toIntOrNull()?.let { return it }
  val identifier = resources.getIdentifier(trimmed, "drawable", packageName)
  if (identifier == 0) {
   Log.w("PlayerRepository", "Drawable not found for reference: $reference")
  }
  return identifier
 }
 private fun Context.resolvePlayerImage(reference: String): PlayerImage {
  val trimmed = reference.trim()
  if (trimmed.isEmpty()) return PlayerImage()

  trimmed.toIntOrNull()?.let { return PlayerImage(resourceId = it) }

  val identifier = resources.getIdentifier(trimmed, "drawable", packageName)
  if (identifier != 0) {
   return PlayerImage(resourceId = identifier)
  }

  if (trimmed.startsWith("http", ignoreCase = true)) {
   return PlayerImage(url = trimmed)
  }

  val resolvedUrl = runCatching {
   val base = java.net.URL(SEED_DATA_URL)
   java.net.URL(base, trimmed).toString()
  }.getOrElse {
   Log.w("PlayerRepository", "Image not found for reference: $reference", it)
   null
  }

  return if (resolvedUrl != null) {
   PlayerImage(url = resolvedUrl)
  } else {
   PlayerImage()
  }
 }
 private suspend fun ensureSeedData() {
  var attemptRemote = false
  var initialCount = 0
  var skip = false

  seedMutex.withLock {
   initialCount = withContext(Dispatchers.IO) { dao.count() }
   if (remoteSeedSynced && initialCount > 0) {
    skip = true
    return@withLock
   }
   if (remoteSeedSynced && initialCount == 0) {
    remoteSeedSynced = false
   }

   val now = SystemClock.elapsedRealtime()
   if (!remoteSeedInFlight && (initialCount == 0 || now - lastRemoteSeedAttempt >= REMOTE_RETRY_INTERVAL_MS)) {
    lastRemoteSeedAttempt = now
    attemptRemote = true
    remoteSeedInFlight = true
   }
  }

  if (skip) {
   return
  }

  var shouldResetBackoff = initialCount == 0 && !attemptRemote
  var remoteSeedPersisted = false
  val remoteResult = if (attemptRemote) downloadRemoteSeed() else null

  if (remoteResult != null) {
   val players = remoteResult.getOrNull().orEmpty()
   if (players.isNotEmpty()) {
    persistRemoteSeed(players)
    remoteSeedPersisted = true
   }

   if (players.isEmpty() && remoteResult.isSuccess) {
    Log.w("PlayerRepository", "Remote dataset contained no players")
   }
   shouldResetBackoff = shouldResetBackoff || remoteResult.isFailure || players.isEmpty()
  }

  if (attemptRemote) {
   seedMutex.withLock { remoteSeedInFlight = false }
  }

  if (remoteSeedPersisted) {
   return
  }

  if (initialCount == 0) {
   seedMutex.withLock {
    val currentCount = withContext(Dispatchers.IO) { dao.count() }
    if (currentCount == 0) {
     if (shouldResetBackoff) {
      lastRemoteSeedAttempt = 0L
     }
     Log.w(
      "PlayerRepository",
      "No player data available; unable to reach remote dataset at $SEED_DATA_URL"
     )
    }
   }
  }
 }

 private suspend fun downloadRemoteSeed(): Result<List<PlayerEntity>> {
  return withContext(Dispatchers.IO) {
   runCatching { fetchRemoteSeed() }
    .onFailure { Log.e("PlayerRepository", "Unable to download seed data", it) }
  }
 }

 private suspend fun persistRemoteSeed(players: List<PlayerEntity>) {
  seedMutex.withLock {
   runCatching {
    withContext(Dispatchers.IO) {
     dao.deleteAll()
     dao.insertAll(players)
    }
   }.onSuccess {
    remoteSeedSynced = true
   }.onFailure {
    Log.e("PlayerRepository", "Failed to persist remote seed", it)
    lastRemoteSeedAttempt = 0L
   }
  }
 }

 private fun fetchRemoteSeed(): List<PlayerEntity> {
  val connection = java.net.URL(SEED_DATA_URL).openConnection() as java.net.HttpURLConnection
  return try {
   connection.connectTimeout = 15_000
   connection.readTimeout = 15_000
   connection.requestMethod = "GET"
   connection.setRequestProperty("Accept", "application/json")
   connection.setRequestProperty("User-Agent", "InazumaDraft/1.0 (Android)")

   val code = connection.responseCode
   if (code != java.net.HttpURLConnection.HTTP_OK) {
    throw IllegalStateException("Seed download failed with HTTP $code")
   }

   connection.inputStream.use { stream ->
    val json = stream.bufferedReader().use { it.readText() }
    parseSeed(json)
   }
  } finally {
   connection.disconnect()
  }
 }

 private fun parseSeed(json: String): List<PlayerEntity> {
  val array = when (val trimmed = json.trim()) {
   "" -> JSONArray()
   else -> when {
    trimmed.startsWith("[") -> JSONArray(trimmed)
    trimmed.startsWith("{") -> {
     val obj = JSONObject(trimmed)
     obj.optJSONArray("players")
      ?: obj.optJSONArray("data")
      ?: throw IllegalStateException("Seed JSON does not contain a players array")
    }
    else -> throw IllegalStateException("Unrecognized seed format")
   }
  }
  val players = mutableListOf<PlayerEntity>()
  for (i in 0 until array.length()) {
   val obj = array.getJSONObject(i)
   val seasons = obj.optFlexibleStringList("seasons", "season")
   val secondary = obj.optFlexibleStringList("secondaryPositions", "secondary")
   val rawId = sequenceOf(
    obj.optLong("id"),
    obj.optLong("playerId"),
    obj.optLong("uid")
   ).firstOrNull { it != 0L }
   players += PlayerEntity(
    id = rawId ?: (i + 1).toLong(),
    name = obj.optString("name"),
    nickname = obj.optString("nickname", obj.optString("name")),
    position = obj.optString("position"),
    elementRef = obj.optString("element"),
    kick = obj.optInt("kick"),
    speed = obj.optInt("speed"),
    control = obj.optInt("control"),
    defense = obj.optInt("defense"),
    imageRef = obj.optString("image"),
    seasons = seasons,
    secondaryPositions = secondary
   )
  }
  return players

 }
}

private fun JSONArray.toStringList(): List<String> {
 val result = mutableListOf<String>()
 for (i in 0 until length()) {
  result += optString(i)
 }
 return result
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