package com.example.data

import com.example.game.Arc2DeskState
import com.example.game.CaseDeduction
import com.example.game.CreatureType
import com.example.game.CultDossierLine
import com.example.game.DeskAnomalyType
import com.example.game.DredgedRelic
import com.example.game.InvestigatorContact
import com.example.game.OrganGameState
import com.example.game.RepeatableScarGraft
import com.example.game.ScarNodeId
import com.example.game.SpecimenJar
import com.example.game.WallAnomalyType
import org.json.JSONArray
import org.json.JSONObject

/** Decoded Arc 1 (Chamber) snapshot produced by [Arc1SaveCodec]. */
internal data class Arc1DecodedSnapshot(
  val ichor: Double,
  val cycleLifetimeIchor: Double,
  val stillness: Double,
  val cycleLifetimeStillness: Double,
  val scarTissue: Long,
  val availableScarTissue: Long,
  val highestExcisionMilestone: Int,
  val unlockedScarNodes: Set<ScarNodeId>,
  val repeatableScarRanks: Map<RepeatableScarGraft, Int>,
  val cycle: Int,
  val creatures: Map<CreatureType, Int>,
  val graftedTendrils: Int,
  val choirLungs: Int,
  val graftCooldownRemainingSeconds: Double,
  val choirLungCooldownRemainingSeconds: Double,
  val ownedJars: Set<SpecimenJar>,
  val anomalyInteractions: Map<WallAnomalyType, Int>,
  val drainSealed: Boolean,
  val drainPressure: Double,
  val dredgedRelics: Set<DredgedRelic>,
  val lifetimeRelicsDredged: Int,
  val echoDiversionPercent: Int,
  val echoPulsesTriggered: Int,
  val marrowBloomCount: Int,
  val unredactedDossierLines: Set<CultDossierLine>,
)

/** Isolated Arc 1 (Chamber) save codec — untouched when Arc 2 expands to schema v3+. */
internal object Arc1SaveCodec {

  fun encodeInto(root: JSONObject, state: OrganGameState) {
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
  }

  fun decode(root: JSONObject, schemaVersion: Int): Arc1DecodedSnapshot {
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

    val cycle = root.optInt("cycle", 0).coerceAtLeast(0)
    val scarTissue = root.optLong("scarTissue", 0L).coerceAtLeast(0L)
    val availableScarTissue =
      if (root.has("availableScarTissue")) {
        root.optLong("availableScarTissue", 0L).coerceAtLeast(0L)
      } else if (schemaVersion < 2) {
        scarTissue
      } else {
        0L
      }
    val highestExcisionMilestone =
      if (root.has("highestExcisionMilestone")) {
        root.optInt("highestExcisionMilestone", 0).coerceIn(0, 6)
      } else if (schemaVersion < 2 && cycle > 0) {
        1
      } else {
        0
      }

    return Arc1DecodedSnapshot(
      ichor = root.optDouble("ichor", 0.0).coerceAtLeast(0.0),
      cycleLifetimeIchor = root.optDouble("cycleLifetimeIchor", 0.0).coerceAtLeast(0.0),
      stillness = root.optDouble("stillness", 0.0).coerceAtLeast(0.0),
      cycleLifetimeStillness = root.optDouble("cycleLifetimeStillness", 0.0).coerceAtLeast(0.0),
      scarTissue = scarTissue,
      availableScarTissue = availableScarTissue,
      highestExcisionMilestone = highestExcisionMilestone,
      unlockedScarNodes = unlockedNodes,
      repeatableScarRanks = repeatableRanks,
      cycle = cycle,
      creatures = creatures,
      graftedTendrils = root.optInt("graftedTendrils", 0).coerceAtLeast(0),
      choirLungs = root.optInt("choirLungs", 0).coerceAtLeast(0),
      graftCooldownRemainingSeconds =
        root.optDouble("graftCooldownRemainingSeconds", 0.0).coerceAtLeast(0.0),
      choirLungCooldownRemainingSeconds =
        root.optDouble("choirLungCooldownRemainingSeconds", 0.0).coerceAtLeast(0.0),
      ownedJars = ownedJars,
      anomalyInteractions = anomalyInteractions,
      drainSealed = root.optBoolean("drainSealed", false),
      drainPressure = root.optDouble("drainPressure", 0.0).coerceIn(0.0, 100.0),
      dredgedRelics = relics,
      lifetimeRelicsDredged = root.optInt("lifetimeRelicsDredged", 0).coerceAtLeast(0),
      echoDiversionPercent = root.optInt("echoDiversionPercent", 0),
      echoPulsesTriggered = root.optInt("echoPulsesTriggered", 0).coerceAtLeast(0),
      marrowBloomCount = root.optInt("marrowBloomCount", 0).coerceAtLeast(0),
      unredactedDossierLines = dossierLines,
    )
  }
}

/**
 * Isolated Arc 2 (Haunted Investigator Desk) save codec.
 * Accepts [schemaVersion] so future v3 Corkboard/Lamp/Forensic fields route cleanly here without
 * coupling to Arc 1 persistence.
 */
internal object Arc2SaveCodec {

  fun encode(desk: Arc2DeskState): JSONObject {
    val arc2Obj = JSONObject()
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
    return arc2Obj
  }

  fun decode(rawArc2: JSONObject?, schemaVersion: Int): Arc2DeskState {
    val arc2Obj = rawArc2 ?: return Arc2DeskState()
    val contacts = mutableMapOf<InvestigatorContact, Int>()
    arc2Obj.optJSONObject("contacts")?.let { obj ->
      InvestigatorContact.entries.forEach { contact ->
        val count = obj.optInt(contact.id, 0)
        if (count > 0) contacts[contact] = count
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

    val v2State =
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
    return if (schemaVersion >= 3) {
      decodeV3Extension(v2State, arc2Obj)
    } else {
      v2State
    }
  }

  /** Forward-compatible v3 Arc 2 migration/extension seam (no-op until Arc 2 overhaul lands). */
  private fun decodeV3Extension(base: Arc2DeskState, @Suppress("UNUSED_PARAMETER") arc2Obj: JSONObject): Arc2DeskState =
    base
}
