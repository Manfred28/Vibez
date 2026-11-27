package com.example.data

import com.example.game.Arc2DeskState
import com.example.game.Arc3Balance
import com.example.game.Arc3BreachState
import com.example.game.Arc3Stratum
import com.example.game.Arc3UnmakeAct
import com.example.game.BreachRuptureType
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
 * Encodes v3 Corkboard/Lamp/Forensic fields and migrates legacy v2 payloads cleanly without
 * touching Arc 1 persistence.
 */
internal object Arc2SaveCodec {

  fun encode(desk: Arc2DeskState): JSONObject {
    val arc2Obj = JSONObject()
    arc2Obj.put("leads", desk.leads)
    arc2Obj.put("lifetimeLeads", desk.lifetimeLeads)
    arc2Obj.put("silence", desk.silence)
    arc2Obj.put("lifetimeSilence", desk.lifetimeSilence)
    arc2Obj.put("caseAngle", desk.caseAngle)
    arc2Obj.put("completedCasePivots", desk.completedCasePivots)
    arc2Obj.put("completedChapters", desk.completedCasePivots)
    arc2Obj.put("lampOn", desk.lampOn)
    arc2Obj.put("lampToggleCount", desk.lampToggleCount)
    arc2Obj.put("lampOffCount", desk.lampOffCount)
    arc2Obj.put("totalLampOffSeconds", desk.totalLampOffSeconds)
    arc2Obj.put("reelScrubRemainingSeconds", desk.reelScrubRemainingSeconds)
    arc2Obj.put("reelScrubCooldownSeconds", desk.reelScrubCooldownSeconds)
    arc2Obj.put("reelScrubCount", desk.reelScrubCount)
    arc2Obj.put("fileZeroRevealed", desk.fileZeroRevealed)

    val contactsObj = JSONObject()
    desk.contacts.forEach { (contact, count) -> contactsObj.put(contact.id, count) }
    arc2Obj.put("contacts", contactsObj)

    val exhibitsArr = JSONArray()
    desk.pinnedExhibits.forEach { exhibitsArr.put(it.id) }
    arc2Obj.put("pinnedExhibits", exhibitsArr)

    val wiredArr = JSONArray()
    desk.wiredPairs.forEach { wiredArr.put(it.key) }
    arc2Obj.put("wiredPairs", wiredArr)

    val corrArr = JSONArray()
    desk.discoveredCorroborations.forEach { corrArr.put(it.id) }
    arc2Obj.put("discoveredCorroborations", corrArr)

    val redactionsArr = JSONArray()
    desk.dissolvedRedactions.forEach { redactionsArr.put(it.id) }
    arc2Obj.put("dissolvedRedactions", redactionsArr)

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

    val deskAnomalies = mutableMapOf<DeskAnomalyType, Int>()
    arc2Obj.optJSONObject("deskAnomalyInteractions")?.let { obj ->
      val keys = obj.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val type = DeskAnomalyType.byId(key) ?: continue
        val count = obj.optInt(key, 0)
        if (count > 0) deskAnomalies[type] = (deskAnomalies[type] ?: 0) + count
      }
    }

    val leads = arc2Obj.optDouble("leads", 0.0).coerceAtLeast(0.0)
    val lifetimeLeads = arc2Obj.optDouble("lifetimeLeads", leads).coerceAtLeast(leads)
    val silence = arc2Obj.optDouble("silence", 0.0).coerceAtLeast(0.0)
    val lifetimeSilence = arc2Obj.optDouble("lifetimeSilence", silence).coerceAtLeast(silence)

    if (schemaVersion >= 3 && arc2Obj.has("caseAngle")) {
      val caseAngle = arc2Obj.optInt("caseAngle", 1).coerceIn(1, 5)
      val pivots = arc2Obj.optInt("completedCasePivots", (caseAngle - 1).coerceAtLeast(0)).coerceIn(0, 5)
      val fileZero = arc2Obj.optBoolean("fileZeroRevealed", false) || pivots >= 5

      val pinned = mutableSetOf(com.example.game.CaseExhibit.WATER_STAINED_PETITION)
      arc2Obj.optJSONArray("pinnedExhibits")?.let { arr ->
        for (i in 0 until arr.length()) {
          com.example.game.CaseExhibit.byId(arr.optString(i))?.let { pinned.add(it) }
        }
      }
      // Guarantee keystones earned by pivots are always pinned
      com.example.game.CaseAngle.entries.forEach { angle ->
        if (pivots >= angle.number && angle.keystoneReward != null) {
          pinned.add(angle.keystoneReward)
        }
      }

      val maxSpools = (1 + pivots).coerceIn(1, 5)
      val wired = mutableSetOf<com.example.game.ExhibitPair>()
      arc2Obj.optJSONArray("wiredPairs")?.let { arr ->
        for (i in 0 until arr.length()) {
          if (wired.size >= maxSpools) break
          val pair = com.example.game.ExhibitPair.parse(arr.optString(i)) ?: continue
          if (pair.first in pinned && pair.second in pinned) wired.add(pair)
        }
      }

      val discoveredCorr = mutableSetOf<com.example.game.CorroborationId>()
      arc2Obj.optJSONArray("discoveredCorroborations")?.let { arr ->
        for (i in 0 until arr.length()) {
          com.example.game.CorroborationId.byId(arr.optString(i))?.let { discoveredCorr.add(it) }
        }
      }
      wired.mapNotNull { com.example.game.CorroborationId.matchPair(it) }.forEach { discoveredCorr.add(it) }

      val redactions = mutableSetOf<com.example.game.AuditRedaction>()
      arc2Obj.optJSONArray("dissolvedRedactions")?.let { arr ->
        for (i in 0 until arr.length()) {
          com.example.game.AuditRedaction.byId(arr.optString(i))?.let { redactions.add(it) }
        }
      }

      return Arc2DeskState(
        leads = leads,
        lifetimeLeads = lifetimeLeads,
        silence = silence,
        lifetimeSilence = lifetimeSilence,
        caseAngle = caseAngle,
        completedCasePivots = pivots,
        lampOn = arc2Obj.optBoolean("lampOn", true),
        lampToggleCount = arc2Obj.optInt("lampToggleCount", 0).coerceAtLeast(0),
        lampOffCount = arc2Obj.optInt("lampOffCount", 0).coerceAtLeast(0),
        totalLampOffSeconds = arc2Obj.optDouble("totalLampOffSeconds", 0.0).coerceAtLeast(0.0),
        contacts = contacts,
        pinnedExhibits = pinned,
        wiredPairs = wired,
        discoveredCorroborations = discoveredCorr,
        reelScrubRemainingSeconds = arc2Obj.optDouble("reelScrubRemainingSeconds", 0.0).coerceAtLeast(0.0),
        reelScrubCooldownSeconds = arc2Obj.optDouble("reelScrubCooldownSeconds", 0.0).coerceAtLeast(0.0),
        reelScrubCount = arc2Obj.optInt("reelScrubCount", 0).coerceAtLeast(0),
        dissolvedRedactions = redactions,
        deskAnomalyInteractions = deskAnomalies,
        fileZeroRevealed = fileZero,
      )
    }

    // Legacy v2 -> v3 migration: preserve progress gracefully without wiping player effort
    val legacyChapters = arc2Obj.optInt("completedChapters", 0).coerceIn(0, 4)
    val legacyFileZero = arc2Obj.optBoolean("fileZeroRevealed", false) || legacyChapters >= 4
    val migratedPivots = if (legacyFileZero) 5 else legacyChapters
    val migratedAngle = (legacyChapters + 1).coerceIn(1, 5)
    val migratedExhibits = mutableSetOf(com.example.game.CaseExhibit.WATER_STAINED_PETITION)
    com.example.game.CaseAngle.entries.forEach { angle ->
      if (migratedPivots >= angle.number && angle.keystoneReward != null) {
        migratedExhibits.add(angle.keystoneReward)
      }
    }
    // Map legacy ownedDeductions to equivalent Silence-unlocked Exhibits
    arc2Obj.optJSONArray("ownedDeductions")?.let { arr ->
      for (i in 0 until arr.length()) {
        when (arr.optString(i)) {
          "hydro_ward_invoices" -> migratedExhibits.add(com.example.game.CaseExhibit.UNLISTED_PAY_STUB)
          "missing_caretaker_roster" -> migratedExhibits.add(com.example.game.CaseExhibit.INSIDE_CONDENSATION_SWAB)
          "mortar_sample_match" -> migratedExhibits.add(com.example.game.CaseExhibit.TUESDAY_STATEMENT_COPY)
        }
      }
    }

    return Arc2DeskState(
      leads = leads,
      lifetimeLeads = lifetimeLeads,
      silence = silence,
      lifetimeSilence = lifetimeSilence,
      caseAngle = migratedAngle,
      completedCasePivots = migratedPivots,
      lampOn = true,
      contacts = contacts,
      pinnedExhibits = migratedExhibits,
      deskAnomalyInteractions = deskAnomalies,
      fileZeroRevealed = legacyFileZero,
    )
  }
}

/**
 * Isolated Arc 3 ("The Unmaking") save codec introduced in schema v4.
 * Defaults cleanly to a fresh [Arc3BreachState] when loading legacy v1–v3 saves.
 */
internal object Arc3SaveCodec {

  fun encode(breach: Arc3BreachState): JSONObject {
    val obj = JSONObject()
    obj.put("currentStratum", breach.currentStratum)
    obj.put("fracture", breach.fracture)
    obj.put("hemorrhage", breach.hemorrhage)
    obj.put("lifetimeHemorrhage", breach.lifetimeHemorrhage)
    obj.put("wrenchCount", breach.wrenchCount)
    obj.put("finalPullsCompleted", breach.finalPullsCompleted)
    obj.put("submersionComplete", breach.submersionComplete)

    val wedgesArr = JSONArray()
    breach.lockedWedges.forEach { wedgesArr.put(it.wedgeId) }
    obj.put("lockedWedges", wedgesArr)

    val unmakesArr = JSONArray()
    breach.completedUnmakes.forEach { unmakesArr.put(it.id) }
    obj.put("completedUnmakes", unmakesArr)

    val rupturesObj = JSONObject()
    breach.ruptureInteractions.forEach { (type, count) ->
      if (count > 0) rupturesObj.put(type.id, count)
    }
    obj.put("ruptureInteractions", rupturesObj)
    return obj
  }

  fun decode(rawArc3: JSONObject?): Arc3BreachState {
    val obj = rawArc3 ?: return Arc3BreachState()
    val lockedWedges = mutableSetOf<Arc3Stratum>()
    obj.optJSONArray("lockedWedges")?.let { arr ->
      for (i in 0 until arr.length()) {
        Arc3Stratum.byWedgeId(arr.optString(i))?.let { lockedWedges.add(it) }
      }
    }
    val completedUnmakes = mutableSetOf<Arc3UnmakeAct>()
    obj.optJSONArray("completedUnmakes")?.let { arr ->
      for (i in 0 until arr.length()) {
        Arc3UnmakeAct.byId(arr.optString(i))?.let { completedUnmakes.add(it) }
      }
    }
    val ruptureInteractions = mutableMapOf<BreachRuptureType, Int>()
    obj.optJSONObject("ruptureInteractions")?.let { mapObj ->
      BreachRuptureType.entries.forEach { type ->
        val count = mapObj.optInt(type.id, 0)
        if (count > 0) ruptureInteractions[type] = count
      }
    }

    val hem = obj.optDouble("hemorrhage", 0.0).coerceAtLeast(0.0)
    val lifetimeHem = obj.optDouble("lifetimeHemorrhage", hem).coerceAtLeast(hem)
    val stratum = obj.optInt("currentStratum", 1).coerceIn(1, Arc3Stratum.entries.size)
    val finalPulls = obj.optInt("finalPullsCompleted", 0).coerceIn(0, Arc3Balance.FINAL_PULLS_TOTAL)
    val submerged = obj.optBoolean("submersionComplete", false) || finalPulls >= Arc3Balance.FINAL_PULLS_TOTAL
    val goal = Arc3Stratum.byNumber(stratum).fractureGoal

    return Arc3BreachState(
      currentStratum = stratum,
      lockedWedges = lockedWedges,
      fracture = obj.optDouble("fracture", 0.0).coerceIn(0.0, goal),
      hemorrhage = hem,
      lifetimeHemorrhage = lifetimeHem,
      wrenchCount = obj.optInt("wrenchCount", 0).coerceAtLeast(0),
      completedUnmakes = completedUnmakes,
      ruptureInteractions = ruptureInteractions,
      finalPullsCompleted = finalPulls,
      submersionComplete = submerged,
    )
  }
}
