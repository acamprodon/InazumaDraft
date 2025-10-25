package com.inazumadraft.data

import android.content.Context
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
import org.json.JSONArray

object PlayerRepository {

 private const val DATABASE_NAME = "inazuma_players.db"
 private const val SEED_DATA_URL = "https://acamprodon.github.io/InazumaDraft-data/players.json"
 private const val SEED_ASSET_BASE_URL = "https://acamprodon.github.io/InazumaDraft-data/images"

 private lateinit var applicationContext: Context
 private lateinit var database: InazumaDatabase
 private lateinit var dao: PlayerDao

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
  val players = withContext(Dispatchers.IO) { dao.getAll() }
  val filter = selectedSeasons.map { it.uppercase() }.toSet()
  return players
   .asSequence()
   .filter { filter.isEmpty() || it.seasons.any { season -> season.uppercase() in filter } }
   .map { it.toDomain(applicationContext) }
   .toList()
 }

 suspend fun deletePlayer(playerId: Long) {
  ensureInitialized()
  withContext(Dispatchers.IO) { dao.deleteById(playerId) }
 }

 @VisibleForTesting
 internal suspend fun overwritePlayers(players: List<PlayerEntity>) {
  ensureInitialized()
  withContext(Dispatchers.IO) {
   dao.insertAll(players)
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

  var relativePath = when {
   trimmed.startsWith('/') -> trimmed.drop(1)
   trimmed.startsWith("./") -> trimmed.drop(2)
   else -> trimmed
  }

  while (relativePath.startsWith("../")) {
   relativePath = relativePath.drop(3)
  }

  if (relativePath.contains('/') || relativePath.contains('.')) {
   return PlayerImage(url = SEED_ASSET_BASE_URL + relativePath)
  }

  Log.w("PlayerRepository", "Image not found for reference: $reference")
  return PlayerImage()
 }
 private suspend fun ensureSeedData() {
  val currentCount = withContext(Dispatchers.IO) { dao.count() }
  if (currentCount > 0) return

  val seeded = withContext(Dispatchers.IO) { loadSeedPlayers() }
  if (seeded.isEmpty()) return

  runCatching {
   withContext(Dispatchers.IO) { dao.insertAll(seeded) }
  }.onFailure { Log.e("PlayerRepository", "Failed to seed database", it) }
 }

 private fun loadSeedPlayers(): List<PlayerEntity> {
  return runCatching {
   fetchRemoteSeed()
  }.recoverCatching {
   Log.e("PlayerRepository", "Unable to download seed data", it)
   emptyList()
  }.getOrDefault(emptyList())
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
  val array = JSONArray(json)
  val players = mutableListOf<PlayerEntity>()
  for (i in 0 until array.length()) {
   val obj = array.getJSONObject(i)
   val seasons = obj.getJSONArray("seasons").toStringList()
   val secondary = obj.optJSONArray("secondaryPositions")?.toStringList().orEmpty()
   players += PlayerEntity(
    id = (i + 1).toLong(),
    name = obj.getString("name"),
    nickname = obj.getString("nickname"),
    position = obj.getString("position"),
    elementRef = obj.getString("element"),
    kick = obj.getInt("kick"),
    speed = obj.getInt("speed"),
    control = obj.getInt("control"),
    defense = obj.getInt("defense"),
    imageRef = obj.getString("image"),
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
