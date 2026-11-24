package com.example.data

import android.content.Context
import com.example.game.Arc2DeskState
import com.example.game.CaseDeduction
import com.example.game.CreatureType
import com.example.game.CultDossierLine
import com.example.game.DeskAnomalyType
import com.example.game.DredgedRelic
import com.example.game.GameArc
import com.example.game.InvestigatorContact
import com.example.game.OrganBalance
import com.example.game.OrganGameEngine
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import org.json.JSONArray
import org.json.JSONObject

interface OrganSaveRepository {
  fun load(catalog: DialogueCatalog): OrganGameState?

  fun save(state: OrganGameState)

  fun clear()
}

/** Pure JSON serializer/deserializer for multi-arc local save persistence (Android & test safe). */
object OrganStateSerializer {
  private const val SCHEMA_VERSION = 2

  fun serialize(state: OrganGameState): String {
    val root = JSONObject()
    root.put("schemaVersion", SCHEMA_VERSION)
    root.put("activeArc", state.activeArc.id)
    root.put("ichor", state.ichor)
    root.put("cycleLifetimeIchor", state.cycleLifetimeIchor)
    root.put("stillness", state.stillness)
    root.put("cycleLifetimeStillness", state.cycleLifetimeStillness)
    root.put("scarTissue", state.scarTissue)
    root.put("availableScarTissue", state.availableScarTissue)
    root.put("highestExcisionMilestone", state.highestExcisionMilestone)
    root.put("cycle", state.cycle)
    root.put("graftedTendrils", state.graftedTendrils)
    root.put("choirLungs", state.choirLungs)
    root.put("graftCooldownRemainingSeconds", state.graftCooldownRemainingSeconds)
    root.put("choirLungCooldownRemainingSeconds", state.choirLungCooldownRemainingSeconds)
    root.put("drainSealed", state.drainSealed)
    root.put("drainPressure", state.drainPressure)
    root.put("lifetimeRelicsDredged", state.lifetimeRelicsDredged)
    root.put("echoDiversionPercent", state.echoDiversionPercent)
    root.put("echoPulsesTriggered", state.echoPulsesTriggered)
    root.put("marrowBloomCount", state.marrowBloomCount)

    val creaturesObj = JSONObject()
    state.creatures.forEach { (type, count) -> creaturesObj.put(type.id, count) }
    root.put("creatures", creaturesObj)

    val jarsArr = JSONArray()
    state.ownedJars.forEach { jarsArr.put(it.id) }
    root.put("ownedJars", jarsArr)

    val nodesArr = JSONArray()
    state.unlockedScarNodes.forEach { nodesArr.put(it.id) }
    root.put("unlockedScarNodes", nodesArr)

    val repeatablesObj = JSONObject()
    state.repeatableScarRanks.forEach { (graft, rank) ->
      if (rank > 0) repeatablesObj.put(graft.id, rank)
    }
    root.put("repeatableScarRanks", repeatablesObj)

    val relicsArr = JSONArray()
    state.dredgedRelics.forEach { relicsArr.put(it.id) }
    root.put("dredgedRelics", relicsArr)

    val dossierArr = JSONArray()
    state.unredactedDossierLines.forEach { dossierArr.put(it.id) }
    root.put("unredactedDossierLines", dossierArr)

    val anomaliesObj = JSONObject()
    state.anomalyInteractions.forEach { (type, count) -> anomaliesObj.put(type.id, count) }
    root.put("anomalyInteractions", anomaliesObj)

    val triggeredArr = JSONArray()
    state.triggeredDialogueIds.forEach { triggeredArr.put(it) }
    root.put("triggeredDialogueIds", triggeredArr)

    val arc2Obj = JSONObject()
    val desk = state.arc2
    arc2Obj.put("leads", desk.leads)
    arc2Obj.put("lifetimeLeads", desk.lifetimeLeads)
    arc2Obj.put("silence", desk.silence)
    arc2Obj.put("lifetimeSilence", desk.lifetimeSilence)
    arc2Obj.put("complicity", desk.complicity.toDouble())
    arc2Obj.put("completedChapters", desk.completedChapters)
    arc2Obj.put("fileZeroRevealed", desk.fileZeroRevealed)

    val contactsObj = JSONObject()
    desk.contacts.forEach { (contact, count) -> contactsObj.put(contact.id, count) }
    arc2Obj.put("contacts", contactsObj)

    val deductionsArr = JSONArray()
    desk.ownedDeductions.forEach { deductionsArr.put(it.id) }
    arc2Obj.put("ownedDeductions", deductionsArr)

    val deskAnomaliesObj = JSONObject()
    desk.deskAnomalyInteractions.forEach { (type, count) -> deskAnomaliesObj.put(type.id, count) }
    arc2Obj.put("deskAnomalyInteractions", deskAnomaliesObj)

    root.put("arc2", arc2Obj)
    return root.toString()
  }

  fun deserialize(raw: String, catalog: DialogueCatalog): OrganGameState? {
    return try {
      val root = JSONObject(raw)
      val activeArc =
        GameArc.entries.firstOrNull { it.id == root.optString("activeArc") }
          ?: GameArc.ARC_1_CHAMBER

      val creatures = mutableMapOf<CreatureType, Int>()
      root.optJSONObject("creatures")?.let { obj ->
        CreatureType.entries.forEach { type ->
          val count = obj.optInt(type.id, 0)
          if (count > 0) creatures[type] = count
        }
      }

      val ownedJars = mutableSetOf<SpecimenJar>()
      root.optJSONArray("ownedJars")?.let { arr ->
        for (i in 0 until arr.length()) {
          SpecimenJar.byId(arr.optString(i))?.let { ownedJars.add(it) }
        }
      }

      val unlockedNodes = mutableSetOf<ScarNodeId>()
      root.optJSONArray("unlockedScarNodes")?.let { arr ->
        for (i in 0 until arr.length()) {
          ScarNodeId.byId(arr.optString(i))?.let { unlockedNodes.add(it) }
        }
      }

      val repeatableRanks = mutableMapOf<RepeatableScarGraft, Int>()
      root.optJSONObject("repeatableScarRanks")?.let { obj ->
        RepeatableScarGraft.entries.forEach { graft ->
          val rank = obj.optInt(graft.id, 0)
          if (rank > 0) repeatableRanks[graft] = rank
        }
      }

      val relics = mutableSetOf<DredgedRelic>()
      root.optJSONArray("dredgedRelics")?.let { arr ->
        for (i in 0 until arr.length()) {
          DredgedRelic.byId(arr.optString(i))?.let { relics.add(it) }
        }
      }

      val dossierLines = mutableSetOf<CultDossierLine>()
      root.optJSONArray("unredactedDossierLines")?.let { arr ->
        for (i in 0 until arr.length()) {
          CultDossierLine.byId(arr.optString(i))?.let { dossierLines.add(it) }
        }
      }

      val anomalyInteractions = mutableMapOf<WallAnomalyType, Int>()
      root.optJSONObject("anomalyInteractions")?.let { obj ->
        WallAnomalyType.entries.forEach { type ->
          val count = obj.optInt(type.id, 0)
          if (count > 0) anomalyInteractions[type] = count
        }
      }

      val triggeredIds = mutableSetOf<String>()
      root.optJSONArray("triggeredDialogueIds")?.let { arr ->
        for (i in 0 until arr.length()) {
          val id = arr.optString(i)
          if (id.isNotBlank()) triggeredIds.add(id)
        }
      }
      val discovered = catalog.entries.filter { it.id in triggeredIds }

      val desk =
        root.optJSONObject("arc2")?.let { arc2Obj ->
          val contacts = mutableMapOf<InvestigatorContact, Int>()
          arc2Obj.optJSONObject("contacts")?.let { obj ->
            InvestigatorContact.entries.forEach { c ->
              val count = obj.optInt(c.id, 0)
              if (count > 0) contacts[c] = count
            }
          }
          val deductions = mutableSetOf<CaseDeduction>()
          arc2Obj.optJSONArray("ownedDeductions")?.let { arr ->
            for (i in 0 until arr.length()) {
              CaseDeduction.byId(arr.optString(i))?.let { deductions.add(it) }
            }
          }
          val deskAnomalies = mutableMapOf<DeskAnomalyType, Int>()
          arc2Obj.optJSONObject("deskAnomalyInteractions")?.let { obj ->
            DeskAnomalyType.entries.forEach { type ->
              val count = obj.optInt(type.id, 0)
              if (count > 0) deskAnomalies[type] = count
            }
          }
          Arc2DeskState(
            leads = arc2Obj.optDouble("leads", 0.0).coerceAtLeast(0.0),
            lifetimeLeads = arc2Obj.optDouble("lifetimeLeads", 0.0).coerceAtLeast(0.0),
            silence = arc2Obj.optDouble("silence", 0.0).coerceAtLeast(0.0),
            lifetimeSilence = arc2Obj.optDouble("lifetimeSilence", 0.0).coerceAtLeast(0.0),
            complicity = arc2Obj.optDouble("complicity", 0.05).toFloat().coerceIn(0.05f, 1.0f),
            contacts = contacts,
            ownedDeductions = deductions,
            completedChapters = arc2Obj.optInt("completedChapters", 0).coerceIn(0, 4),
            deskAnomalyInteractions = deskAnomalies,
            fileZeroRevealed = arc2Obj.optBoolean("fileZeroRevealed", false),
          )
        } ?: Arc2DeskState()

      val rawState =
        OrganGameState(
          activeArc = activeArc,
          ichor = root.optDouble("ichor", 0.0).coerceAtLeast(0.0),
          cycleLifetimeIchor = root.optDouble("cycleLifetimeIchor", 0.0).coerceAtLeast(0.0),
          stillness = root.optDouble("stillness", 0.0).coerceAtLeast(0.0),
          cycleLifetimeStillness = root.optDouble("cycleLifetimeStillness", 0.0).coerceAtLeast(0.0),
          scarTissue = root.optLong("scarTissue", 0L).coerceAtLeast(0L),
          availableScarTissue = root.optLong("availableScarTissue", 0L).coerceAtLeast(0L),
          highestExcisionMilestone = root.optInt("highestExcisionMilestone", 0).coerceIn(0, 6),
          unlockedScarNodes = unlockedNodes,
          repeatableScarRanks = repeatableRanks,
          cycle = root.optInt("cycle", 0).coerceAtLeast(0),
          creatures = creatures,
          graftedTendrils = root.optInt("graftedTendrils", 0).coerceAtLeast(0),
          choirLungs = root.optInt("choirLungs", 0).coerceAtLeast(0),
          graftCooldownRemainingSeconds =
            root.optDouble("graftCooldownRemainingSeconds", 0.0).coerceAtLeast(0.0),
          choirLungCooldownRemainingSeconds =
            root.optDouble("choirLungCooldownRemainingSeconds", 0.0).coerceAtLeast(0.0),
          ownedJars = ownedJars,
          anomalyInteractions = anomalyInteractions,
          secondsUntilNextAnomaly = OrganBalance.FIRST_ANOMALY_DELAY_SEC,
          drainSealed = root.optBoolean("drainSealed", false),
          drainPressure = root.optDouble("drainPressure", 0.0).coerceIn(0.0, 100.0),
          dredgedRelics = relics,
          lifetimeRelicsDredged = root.optInt("lifetimeRelicsDredged", 0).coerceAtLeast(0),
          echoDiversionPercent = root.optInt("echoDiversionPercent", 0),
          echoPulsesTriggered = root.optInt("echoPulsesTriggered", 0).coerceAtLeast(0),
          marrowBloomCount = root.optInt("marrowBloomCount", 0).coerceAtLeast(0),
          unredactedDossierLines = dossierLines,
          arc2 = desk,
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
