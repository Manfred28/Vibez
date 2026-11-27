package com.example.data

import android.content.Context
import com.example.game.GameArc
import com.example.game.OrganBalance
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import org.json.JSONArray
import org.json.JSONObject

interface OrganSaveRepository {
  fun load(catalog: DialogueCatalog): OrganGameState?

  fun save(state: OrganGameState)

  fun clear()
}

/**
 * Pure JSON serializer/deserializer for multi-arc local save persistence (Android & test safe).
 * Routes explicitly by schema version (`v1 -> v2 -> v3` forward-compatible seam) and delegates
 * Arc 1 and Arc 2 payloads to [Arc1SaveCodec] and [Arc2SaveCodec].
 */
object OrganStateSerializer {
  internal const val SCHEMA_VERSION = 4
  internal const val LEGACY_SCHEMA_VERSION = 1
  internal const val V2_SCHEMA_VERSION = 2
  internal const val FORWARD_SCHEMA_VERSION = 4

  fun serialize(state: OrganGameState): String {
    val root = JSONObject()
    root.put("schemaVersion", SCHEMA_VERSION)
    root.put("activeArc", state.activeArc.id)

    Arc1SaveCodec.encodeInto(root, state)

    val triggeredArr = JSONArray()
    state.triggeredDialogueIds.forEach { triggeredArr.put(it) }
    root.put("triggeredDialogueIds", triggeredArr)

    root.put("arc2", Arc2SaveCodec.encode(state.arc2))
    root.put("arc3", Arc3SaveCodec.encode(state.arc3))
    return root.toString()
  }

  internal fun detectSchemaVersion(root: JSONObject): Int {
    val rawVersion =
      when {
        root.has("schemaVersion") -> root.optInt("schemaVersion", LEGACY_SCHEMA_VERSION)
        root.has("version") -> root.optInt("version", LEGACY_SCHEMA_VERSION)
        else -> LEGACY_SCHEMA_VERSION
      }
    return rawVersion.coerceAtLeast(LEGACY_SCHEMA_VERSION)
  }

  internal fun migrateRootPayload(root: JSONObject, detectedVersion: Int): Pair<JSONObject, Int> =
    when {
      detectedVersion <= LEGACY_SCHEMA_VERSION -> migrateV1ToV2(root) to V2_SCHEMA_VERSION
      detectedVersion == V2_SCHEMA_VERSION -> root to V2_SCHEMA_VERSION
      else -> root to detectedVersion.coerceAtLeast(SCHEMA_VERSION)
    }

  private fun migrateV1ToV2(v1Root: JSONObject): JSONObject {
    val migrated = JSONObject(v1Root.toString())
    migrated.put("schemaVersion", SCHEMA_VERSION)
    if (!migrated.has("activeArc")) {
      migrated.put("activeArc", GameArc.ARC_1_CHAMBER.id)
    }
    val scarTissue = migrated.optLong("scarTissue", 0L).coerceAtLeast(0L)
    if (!migrated.has("availableScarTissue")) {
      migrated.put("availableScarTissue", scarTissue)
    }
    val cycle = migrated.optInt("cycle", 0).coerceAtLeast(0)
    if (!migrated.has("highestExcisionMilestone")) {
      migrated.put("highestExcisionMilestone", if (cycle > 0) 1 else 0)
    }
    return migrated
  }

  fun deserialize(raw: String, catalog: DialogueCatalog): OrganGameState? {
    return try {
      val parsedRoot = JSONObject(raw)
      val detectedVersion = detectSchemaVersion(parsedRoot)
      val (root, effectiveVersion) = migrateRootPayload(parsedRoot, detectedVersion)

      val activeArc =
        GameArc.entries.firstOrNull { it.id == root.optString("activeArc") }
          ?: GameArc.ARC_1_CHAMBER

      val arc1 = Arc1SaveCodec.decode(root, effectiveVersion)
      val desk = Arc2SaveCodec.decode(root.optJSONObject("arc2"), effectiveVersion)
      val breach = Arc3SaveCodec.decode(root.optJSONObject("arc3"))

      val triggeredIds = mutableSetOf<String>()
      root.optJSONArray("triggeredDialogueIds")?.let { arr ->
        for (i in 0 until arr.length()) {
          val id = arr.optString(i)
          if (id.isNotBlank()) triggeredIds.add(id)
        }
      }
      val discovered = catalog.entries.filter { it.id in triggeredIds }

      val rawState =
        OrganGameState(
          activeArc = activeArc,
          ichor = arc1.ichor,
          cycleLifetimeIchor = arc1.cycleLifetimeIchor,
          stillness = arc1.stillness,
          cycleLifetimeStillness = arc1.cycleLifetimeStillness,
          scarTissue = arc1.scarTissue,
          availableScarTissue = arc1.availableScarTissue,
          highestExcisionMilestone = arc1.highestExcisionMilestone,
          unlockedScarNodes = arc1.unlockedScarNodes,
          repeatableScarRanks = arc1.repeatableScarRanks,
          cycle = arc1.cycle,
          creatures = arc1.creatures,
          graftedTendrils = arc1.graftedTendrils,
          choirLungs = arc1.choirLungs,
          graftCooldownRemainingSeconds = arc1.graftCooldownRemainingSeconds,
          choirLungCooldownRemainingSeconds = arc1.choirLungCooldownRemainingSeconds,
          ownedJars = arc1.ownedJars,
          anomalyInteractions = arc1.anomalyInteractions,
          secondsUntilNextAnomaly = OrganBalance.FIRST_ANOMALY_DELAY_SEC,
          drainSealed = arc1.drainSealed,
          drainPressure = arc1.drainPressure,
          dredgedRelics = arc1.dredgedRelics,
          lifetimeRelicsDredged = arc1.lifetimeRelicsDredged,
          echoDiversionPercent = arc1.echoDiversionPercent,
          echoPulsesTriggered = arc1.echoPulsesTriggered,
          marrowBloomCount = arc1.marrowBloomCount,
          unredactedDossierLines = arc1.unredactedDossierLines,
          arc2 = desk,
          arc3 = breach,
          activeDialogue = discovered.lastOrNull(),
          discoveredDialogues = discovered,
          triggeredDialogueIds = triggeredIds,
        )

      OrganGameEngine.finalizeExternalTransition(rawState, catalog)
    } catch (_: Exception) {
      null
    }
  }
}

class SharedPreferencesOrganSaveRepository(context: Context) : OrganSaveRepository {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun load(catalog: DialogueCatalog): OrganGameState? {
    val raw = prefs.getString(KEY_SAVE_JSON, null) ?: return null
    return OrganStateSerializer.deserialize(raw, catalog)
  }

  override fun save(state: OrganGameState) {
    prefs.edit().putString(KEY_SAVE_JSON, OrganStateSerializer.serialize(state)).apply()
  }

  override fun clear() {
    prefs.edit().remove(KEY_SAVE_JSON).apply()
  }

  companion object {
    private const val PREFS_NAME = "unnamed_organ_local_save"
    private const val KEY_SAVE_JSON = "organ_save_v2"
  }
}
