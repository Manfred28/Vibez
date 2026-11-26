package com.example.game

import kotlin.math.floor
import kotlin.math.pow

/**
 * Pure domain models, 5-Case Slow Boil progression, 10 Exhibits, 10 Curated Corroborations,
 * Lamp On/Off dual-lens economy, Acoustic Reel Scrubbing, and 4 Case IV Audit Redactions
 * for Arc 2 ("The Inquiry").
 */
enum class CaseAngle(
  val number: Int,
  val id: String,
  val title: String,
  val subtitle: String,
  val leadsCost: Long,
  val silenceCost: Long,
  val requiredCorroborations: Int,
  val requiredReelScrubs: Int,
  val requiredRedactions: Int,
  val keystoneReward: CaseExhibit?,
) {
  UNCLAIMED_ARREARS(
    number = 1,
    id = "unclaimed_arrears",
    title = "CASE I · UNCLAIMED ARREARS",
    subtitle = "Unsigned plea and twelve unpaid municipal water notices",
    leadsCost = 10_000L,
    silenceCost = 90L,
    requiredCorroborations = 1,
    requiredReelScrubs = 0,
    requiredRedactions = 0,
    keystoneReward = CaseExhibit.TAX_PLAT_1998,
  ),
  THE_SECOND_CHAIR(
    number = 2,
    id = "the_second_chair",
    title = "CASE II · THE SECOND CHAIR",
    subtitle = "Blank brick on the 1998 plat and damp scuffs across the floorboards",
    leadsCost = 115_000L,
    silenceCost = 360L,
    requiredCorroborations = 2,
    requiredReelScrubs = 0,
    requiredRedactions = 0,
    keystoneReward = CaseExhibit.SECOND_CHAIR_POLAROID,
  ),
  STATIC_ON_REEL_4(
    number = 3,
    id = "static_on_reel_4",
    title = "CASE III · STATIC ON REEL 4",
    subtitle = "Forty minutes of basement room tone hiding a slow metallic thump",
    leadsCost = 1_100_000L,
    silenceCost = 1_500L,
    requiredCorroborations = 3,
    requiredReelScrubs = 1,
    requiredRedactions = 0,
    keystoneReward = CaseExhibit.REEL_FOUR_WAVEFORM,
  ),
  SUB_SURFACE_SURVEY(
    number = 4,
    id = "sub_surface_survey",
    title = "CASE IV · SUB-SURFACE SURVEY",
    subtitle = "Looping drainage blueprints and four blacked-out audit clauses",
    leadsCost = 10_500_000L,
    silenceCost = 4_800L,
    requiredCorroborations = 4,
    requiredReelScrubs = 1,
    requiredRedactions = 2,
    keystoneReward = CaseExhibit.DRAINAGE_LOOP_BLUEPRINT,
  ),
  INTERNAL_AUDIT_FILE_ZERO(
    number = 5,
    id = "internal_audit_file_zero",
    title = "CASE V · INTERNAL AUDIT",
    subtitle = "Uncatalogued folder sweating beneath the desk lamp",
    leadsCost = 85_000_000L,
    silenceCost = 11_500L,
    requiredCorroborations = 5,
    requiredReelScrubs = 1,
    requiredRedactions = 4,
    keystoneReward = null,
  );

  companion object {
    fun byNumber(num: Int): CaseAngle = entries.firstOrNull { it.number == num } ?: UNCLAIMED_ARREARS
  }
}

enum class InvestigatorContact(
  val id: String,
  val baseCost: Double,
  val growthFactor: Double,
  val baseLeadsPerSec: Double,
  val unlockCaseAngle: Int,
) {
  NIGHT_ARCHIVIST("night_archivist", 20.0, 1.17, 0.6, 1),
  BRIBED_ORDERLY("bribed_orderly", 140.0, 1.19, 3.2, 1),
  MICROFILM_SCANNER("microfilm_scanner", 950.0, 1.21, 16.0, 2),
  AUDIO_TRANSCRIBER("audio_transcriber", 6_500.0, 1.23, 78.0, 3);

  companion object {
    fun byId(id: String): InvestigatorContact? = entries.firstOrNull { it.id == id }
  }
}

/**
 * 10 collectible Corkboard Exhibits: 5 Case Keystones (pinned automatically at Case entry/pivot)
 * and 5 Silence-unlocked Exhibits (acquired in the Dossier tab).
 */
enum class CaseExhibit(
  val id: String,
  val shortLabel: String,
  val title: String,
  val caption: String,
  val isKeystone: Boolean,
  val unlockCaseAngle: Int,
  val silenceCost: Double,
  val boardX: Float,
  val boardY: Float,
) {
  WATER_STAINED_PETITION(
    id = "water_stained_petition",
    shortLabel = "EX-01",
    title = "Water-Stained Petition",
    caption = "Unsigned plea for salvation left beneath your blotter in rain-warped ink.",
    isKeystone = true,
    unlockCaseAngle = 1,
    silenceCost = 0.0,
    boardX = 0.16f,
    boardY = 0.22f,
  ),
  UNLISTED_PAY_STUB(
    id = "unlisted_pay_stub",
    shortLabel = "EX-02",
    title = "Unlisted Timecard",
    caption = "Municipal meter clerk timecard stamped on the fourteenth with no surname.",
    isKeystone = false,
    unlockCaseAngle = 1,
    silenceCost = 45.0,
    boardX = 0.42f,
    boardY = 0.18f,
  ),
  TAX_PLAT_1998(
    id = "tax_plat_1998",
    shortLabel = "EX-03",
    title = "1998 Tax Plat",
    caption = "Parcel map marking an annex door where only blank windowless brick stands.",
    isKeystone = true,
    unlockCaseAngle = 2,
    silenceCost = 0.0,
    boardX = 0.68f,
    boardY = 0.22f,
  ),
  INSIDE_CONDENSATION_SWAB(
    id = "inside_condensation_swab",
    shortLabel = "EX-04",
    title = "Inside Glass Smear",
    caption = "Cold moisture lifted from the inside of a fourth-floor window painted shut.",
    isKeystone = false,
    unlockCaseAngle = 2,
    silenceCost = 120.0,
    boardX = 0.86f,
    boardY = 0.36f,
  ),
  TUESDAY_STATEMENT_COPY(
    id = "tuesday_statement_copy",
    shortLabel = "EX-05",
    title = "Tuesday Carbon Copy",
    caption = "Witness sheet insisting you already took and signed this statement Tuesday.",
    isKeystone = false,
    unlockCaseAngle = 2,
    silenceCost = 260.0,
    boardX = 0.28f,
    boardY = 0.48f,
  ),
  SECOND_CHAIR_POLAROID(
    id = "second_chair_polaroid",
    shortLabel = "EX-06",
    title = "Second Chair Scuffs",
    caption = "Damp floorboard marks where a second wooden chair faces your blotter.",
    isKeystone = true,
    unlockCaseAngle = 3,
    silenceCost = 0.0,
    boardX = 0.56f,
    boardY = 0.48f,
  ),
  COAT_CLAIM_TICKET(
    id = "coat_claim_ticket",
    shortLabel = "EX-07",
    title = "Coat Claim Ticket",
    caption = "Dry-cleaning stub matching the damp grey wool coat behind your door.",
    isKeystone = false,
    unlockCaseAngle = 3,
    silenceCost = 750.0,
    boardX = 0.82f,
    boardY = 0.68f,
  ),
  REEL_FOUR_WAVEFORM(
    id = "reel_four_waveform",
    shortLabel = "EX-08",
    title = "Reel 4 Trace",
    caption = "Paper oscillograph showing an unhurried rhythmic thump beneath forty minutes of static.",
    isKeystone = true,
    unlockCaseAngle = 4,
    silenceCost = 0.0,
    boardX = 0.18f,
    boardY = 0.78f,
  ),
  INTAKE_THUMB_CAST(
    id = "intake_thumb_cast",
    shortLabel = "EX-09",
    title = "Grey Wax Cast",
    caption = "Cold wax impression bearing a faint transverse ridge across the left thumb.",
    isKeystone = false,
    unlockCaseAngle = 4,
    silenceCost = 2_200.0,
    boardX = 0.46f,
    boardY = 0.80f,
  ),
  DRAINAGE_LOOP_BLUEPRINT(
    id = "drainage_loop_blueprint",
    shortLabel = "EX-10",
    title = "Closed Drainage Plat",
    caption = "Sub-surface storm schematic folding inward into a single windowless square.",
    isKeystone = true,
    unlockCaseAngle = 5,
    silenceCost = 0.0,
    boardX = 0.72f,
    boardY = 0.84f,
  );

  companion object {
    fun byId(id: String): CaseExhibit? = entries.firstOrNull { it.id == id }
  }
}

/** Canonical unordered pair of pinned Corkboard Exhibits linked by one Red String Spool. */
data class ExhibitPair(val first: CaseExhibit, val second: CaseExhibit) {
  init {
    require(first != second) { "Cannot wire an exhibit to itself" }
  }

  val key: String
    get() = "${first.id}|${second.id}"

  companion object {
    fun canonical(a: CaseExhibit, b: CaseExhibit): ExhibitPair =
      if (a.id <= b.id) ExhibitPair(a, b) else ExhibitPair(b, a)

    fun parse(raw: String): ExhibitPair? {
      val parts = raw.split("|")
      if (parts.size != 2) return null
      val a = CaseExhibit.byId(parts[0]) ?: return null
      val b = CaseExhibit.byId(parts[1]) ?: return null
      if (a == b) return null
      return canonical(a, b)
    }
  }
}

/** 10 curated Corkboard Corroboration pairs granting targeted narrative and mechanical bonuses. */
enum class CorroborationId(
  val id: String,
  val title: String,
  val effectSummary: String,
  val exhibitA: CaseExhibit,
  val exhibitB: CaseExhibit,
) {
  SMUDGED_LEDGER_DATE(
    id = "smudged_ledger_date",
    title = "Smudged Ledger Date",
    effectSummary = "+65% Stamp Leads",
    exhibitA = CaseExhibit.WATER_STAINED_PETITION,
    exhibitB = CaseExhibit.UNLISTED_PAY_STUB,
  ),
  UNLISTED_ANNEX(
    id = "unlisted_annex",
    title = "Unlisted Annex",
    effectSummary = "+65% Contact Leads/s",
    exhibitA = CaseExhibit.TAX_PLAT_1998,
    exhibitB = CaseExhibit.SECOND_CHAIR_POLAROID,
  ),
  WARM_ROOM_TONE(
    id = "warm_room_tone",
    title = "Warm Room Tone",
    effectSummary = "+70% Lamp-Off Silence/s",
    exhibitA = CaseExhibit.INSIDE_CONDENSATION_SWAB,
    exhibitB = CaseExhibit.TUESDAY_STATEMENT_COPY,
  ),
  PHANTOM_ROSTER(
    id = "phantom_roster",
    title = "Phantom Roster",
    effectSummary = "+55% Contact Leads/s & +35% Stamp",
    exhibitA = CaseExhibit.UNLISTED_PAY_STUB,
    exhibitB = CaseExhibit.TUESDAY_STATEMENT_COPY,
  ),
  SUB_SURFACE_CADENCE(
    id = "sub_surface_cadence",
    title = "Sub-Surface Cadence",
    effectSummary = "+75% Desk Oddity Yield & +40% Silence",
    exhibitA = CaseExhibit.INSIDE_CONDENSATION_SWAB,
    exhibitB = CaseExhibit.REEL_FOUR_WAVEFORM,
  ),
  WITNESS_IN_TWO_PLACES(
    id = "witness_in_two_places",
    title = "Witness In Two Places",
    effectSummary = "Stronger Reel Scrubbing & +45% Silence",
    exhibitA = CaseExhibit.COAT_CLAIM_TICKET,
    exhibitB = CaseExhibit.REEL_FOUR_WAVEFORM,
  ),
  BORROWED_COAT(
    id = "borrowed_coat",
    title = "Borrowed Coat",
    effectSummary = "Darkness dims Leads less & +50% Contacts",
    exhibitA = CaseExhibit.COAT_CLAIM_TICKET,
    exhibitB = CaseExhibit.SECOND_CHAIR_POLAROID,
  ),
  CLOSED_MUNICIPAL_LOOP(
    id = "closed_municipal_loop",
    title = "Closed Municipal Loop",
    effectSummary = "×2.1 All Leads & Stamp Yield",
    exhibitA = CaseExhibit.TAX_PLAT_1998,
    exhibitB = CaseExhibit.DRAINAGE_LOOP_BLUEPRINT,
  ),
  HYDRO_WARD_SUB_BASEMENT(
    id = "hydro_ward_sub_basement",
    title = "Sub-Basement Seam",
    effectSummary = "×2.2 Silence/s & +60% Oddity Yield",
    exhibitA = CaseExhibit.DRAINAGE_LOOP_BLUEPRINT,
    exhibitB = CaseExhibit.INTAKE_THUMB_CAST,
  ),
  MATCHING_HAND(
    id = "matching_hand",
    title = "Matching Hand",
    effectSummary = "×2.4 Stamp & ×1.8 All Leads/s",
    exhibitA = CaseExhibit.WATER_STAINED_PETITION,
    exhibitB = CaseExhibit.INTAKE_THUMB_CAST,
  );

  val pair: ExhibitPair
    get() = ExhibitPair.canonical(exhibitA, exhibitB)

  companion object {
    fun byId(id: String): CorroborationId? = entries.firstOrNull { it.id == id }

    fun matchPair(pair: ExhibitPair): CorroborationId? = entries.firstOrNull { it.pair == pair }
  }
}

/** 4 Case IV municipal audit clauses dissolved with Solvent (Silence) while the Lamp is Off. */
enum class AuditRedaction(
  val id: String,
  val label: String,
  val revealedText: String,
  val silenceCost: Double,
  val effectSummary: String,
) {
  CLAUSE_SEEPAGE(
    id = "clause_seepage",
    label = "CLAUSE I · SUB-SURFACE",
    revealedText = "Municipal maintenance ceases at the fourth floor; below grade, the tenant maintains the masonry.",
    silenceCost = 750.0,
    effectSummary = "+40% Leads/s · +25% Silence/s",
  ),
  CLAUSE_SHIFT(
    id = "clause_shift",
    label = "CLAUSE II · UNRELIEVED WATCH",
    revealedText = "Relief clerk reassigned indefinitely; attendance continues until the count stabilizes.",
    silenceCost = 1_700.0,
    effectSummary = "+50% Stamp Leads · +35% Silence/s",
  ),
  CLAUSE_QUIET(
    id = "clause_quiet",
    label = "CLAUSE III · QUIET COLLECTOR",
    revealedText = "Where speech disturbs the wall, keep quiet until the glass collects what is owed.",
    silenceCost = 3_400.0,
    effectSummary = "+60% Leads/s · +45% Silence · 15% Lamp-On trickle",
  ),
  CLAUSE_INTAKE(
    id = "clause_intake",
    label = "CLAUSE IV · SUBJECT INTAKE",
    revealedText = "File #00 remains open under the investigator's own signature; no transfer out was ever countersigned.",
    silenceCost = 6_200.0,
    effectSummary = "+80% Leads/s · +60% Silence · Unseals Audit",
  );

  companion object {
    fun byId(id: String): AuditRedaction? = entries.firstOrNull { it.id == id }
  }
}

/**
 * 4 interactive Desk Anomalies gated strictly by Case Angle & Lamp posture.
 * Grotesque enamel/molar anomaly only surfaces from Case IV+ once Slow Boil reaches self-implication.
 */
enum class DeskAnomalyType(val id: String, val unlockCaseAngle: Int) {
  EVIDENCE_ENVELOPE("evidence_envelope", 1),
  FOGGED_SPECTACLES("fogged_spectacles", 2),
  REEL_TAPE("reel_tape", 3),
  ENAMEL_EXHIBIT("enamel_exhibit", 4);

  companion object {
    fun byId(id: String): DeskAnomalyType? =
      entries.firstOrNull { it.id == id }
        ?: if (id == "red_string") ENAMEL_EXHIBIT else null
  }
}

data class ActiveDeskAnomaly(
  val id: Long,
  val type: DeskAnomalyType,
  val xFraction: Float,
  val yFraction: Float,
  val remainingSeconds: Double,
)

enum class InvestigatorDeckTab {
  CONTACTS,
  BOARD,
  DOSSIER,
  REVISION,
}

object Arc2Balance {
  const val LAMP_OFF_BASE_LEADS_FACTOR = 0.25
  const val BORROWED_COAT_LAMP_OFF_LEADS_FACTOR = 0.3875
  const val BASE_LAMP_OFF_SILENCE_PER_SEC = 0.45
  const val SILENCE_PER_CONTACT_PER_SEC = 0.035
  const val SPECULATIVE_THREAD_BONUS = 0.15
  const val FOCUS_DURATION_SEC = 18.0
  const val FOCUS_MULTIPLIER = 1.8
  const val REEL_SCRUB_DURATION_SEC = 24.0
  const val REEL_SCRUB_COOLDOWN_SEC = 45.0
  const val REEL_SCRUB_SILENCE_MULT = 2.6
  const val REEL_SCRUB_WITNESS_SILENCE_MULT = 3.4
  const val FIRST_DESK_ANOMALY_DELAY_SEC = 16.0
  const val LAMP_ON_ANOMALY_INTERVAL_SEC = 32.0
  const val LAMP_OFF_ANOMALY_INTERVAL_SEC = 20.0
  const val DESK_ANOMALY_LIFETIME_SEC = 14.0
  const val MAX_ACTIVE_DESK_ANOMALIES = 2
}

data class Arc2DeskState(
  val leads: Double = 0.0,
  val lifetimeLeads: Double = 0.0,
  val silence: Double = 0.0,
  val lifetimeSilence: Double = 0.0,
  val caseAngle: Int = 1,
  val completedCasePivots: Int = 0,
  val lampOn: Boolean = true,
  val lampToggleCount: Int = 0,
  val lampOffCount: Int = 0,
  val totalLampOffSeconds: Double = 0.0,
  val contacts: Map<InvestigatorContact, Int> = emptyMap(),
  val pinnedExhibits: Set<CaseExhibit> = setOf(CaseExhibit.WATER_STAINED_PETITION),
  val wiredPairs: Set<ExhibitPair> = emptySet(),
  val discoveredCorroborations: Set<CorroborationId> = emptySet(),
  val reelScrubRemainingSeconds: Double = 0.0,
  val reelScrubCooldownSeconds: Double = 0.0,
  val reelScrubCount: Int = 0,
  val dissolvedRedactions: Set<AuditRedaction> = emptySet(),
  val activeDeskAnomalies: List<ActiveDeskAnomaly> = emptyList(),
  val deskAnomalyInteractions: Map<DeskAnomalyType, Int> = emptyMap(),
  val focusRemainingSeconds: Double = 0.0,
  val secondsUntilNextAnomaly: Double = Arc2Balance.FIRST_DESK_ANOMALY_DELAY_SEC,
  val nextAnomalyId: Long = 1L,
  val fileZeroRevealed: Boolean = false,
) {
  val currentCase: CaseAngle
    get() = CaseAngle.byNumber(caseAngle.coerceIn(1, 5))

  /** Backward-compatible chapter count alias mapped to completed Case Angle pivots (0..5). */
  val completedChapters: Int
    get() = completedCasePivots

  /** Lamp Off intentionally puts the office into After-Hours Silence. */
  val isSilenceActive: Boolean
    get() = !lampOn

  val isReelScrubbing: Boolean
    get() = reelScrubRemainingSeconds > 0.0

  val isBulkWarrantUnlocked: Boolean
    get() = caseAngle >= 3 || completedCasePivots >= 2

  val isReelScrubUnlocked: Boolean
    get() = caseAngle >= 3 || completedCasePivots >= 2

  val isRedactionSolventUnlocked: Boolean
    get() = caseAngle >= 4 || completedCasePivots >= 3

  val maxStringSpools: Int
    get() = (1 + completedCasePivots).coerceIn(1, 5)

  val activeCorroborations: Set<CorroborationId>
    get() = wiredPairs.mapNotNull { CorroborationId.matchPair(it) }.toSet()

  val speculativeThreadCount: Int
    get() = wiredPairs.count { CorroborationId.matchPair(it) == null }

  val totalContactsOwned: Int
    get() = contacts.values.sum()

  val leadsDisplay: Long
    get() = floor(leads).toLong().coerceAtLeast(0L)

  val silenceDisplay: Long
    get() = floor(silence).toLong().coerceAtLeast(0L)

  /**
   * Smooth Slow-Boil haunting depth (`0.0..1.0`):
   * - `0.00..0.55` (Cases I–III): strictly cold noir & eerie haunting — zero grotesque Room bleed.
   * - `0.60..0.82` (Case IV): uncanny self-implication — Lamp Off surfaces faint blotter grout.
   * - `0.85..1.00` (Case V → File #00): grotesque intrusion & File #00 peel-back.
   */
  val hauntDepth: Float
    get() {
      if (fileZeroRevealed) return 1.0f
      val base =
        when (caseAngle) {
          1 -> 0.04f
          2 -> 0.22f
          3 -> 0.42f
          4 -> 0.62f
          else -> 0.86f
        }
      val cap =
        when (caseAngle) {
          1 -> 0.18f
          2 -> 0.38f
          3 -> 0.55f
          4 -> 0.82f
          else -> 0.98f
        }
      val leadRatio = (leads / currentCase.leadsCost.toDouble().coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
      val lampBonus = ((totalLampOffSeconds / 360.0).coerceIn(0.0, 1.0) * 0.05f).toFloat()
      val redactionBonus = if (caseAngle >= 4) dissolvedRedactions.size * 0.03f else 0f
      val span = (cap - base).coerceAtLeast(0f)
      return (base + leadRatio * span * 0.75f + lampBonus + redactionBonus).coerceIn(base, cap)
    }

  /** Legacy alias kept synchronized with [hauntDepth] for audio/save compatibility. */
  val complicity: Float
    get() = hauntDepth.coerceIn(0.05f, 1.0f)

  val caseProgressFraction: Float
    get() {
      if (fileZeroRevealed) return 1f
      val leadsPart = (leads / currentCase.leadsCost.toDouble()).coerceIn(0.0, 1.0)
      val silPart = (silence / currentCase.silenceCost.toDouble()).coerceIn(0.0, 1.0)
      return ((leadsPart * 0.65) + (silPart * 0.35)).toFloat().coerceIn(0f, 1f)
    }

  val isRevisionTabVisible: Boolean
    get() =
      completedCasePivots > 0 ||
        fileZeroRevealed ||
        leads >= currentCase.leadsCost * 0.35 ||
        discoveredCorroborations.isNotEmpty()

  fun isContactUnlocked(contact: InvestigatorContact): Boolean =
    caseAngle >= contact.unlockCaseAngle || contactCount(contact) > 0

  fun contactCount(contact: InvestigatorContact): Int = contacts[contact] ?: 0

  fun contactCost(contact: InvestigatorContact): Long =
    kotlin.math.ceil(contact.baseCost * contact.growthFactor.pow(contactCount(contact))).toLong()

  /** Exact geometric series bulk recruit calculation (mirrors Arc 1 Colony Proliferation). */
  fun maxAffordableContacts(contact: InvestigatorContact): Pair<Int, Long> {
    if (!isContactUnlocked(contact)) return 0 to 0L
    var count = 0
    var totalCost = 0L
    var owned = contactCount(contact)
    while (count < 250) {
      val nextCost = kotlin.math.ceil(contact.baseCost * contact.growthFactor.pow(owned)).toLong()
      if (leads < (totalCost + nextCost).toDouble()) break
      totalCost += nextCost
      owned += 1
      count += 1
    }
    return count to totalCost
  }

  private val caseMasteryLeadsMult: Double
    get() = 1.0 + completedCasePivots * 0.45

  private val caseMasterySilenceMult: Double
    get() = 1.0 + completedCasePivots * 0.35

  private val speculativeMultiplier: Double
    get() = 1.0 + speculativeThreadCount * Arc2Balance.SPECULATIVE_THREAD_BONUS

  private val focusMultiplier: Double
    get() = if (focusRemainingSeconds > 0.0) Arc2Balance.FOCUS_MULTIPLIER else 1.0

  val lampLeadsMultiplier: Double
    get() =
      if (lampOn) {
        1.0
      } else if (CorroborationId.BORROWED_COAT in activeCorroborations) {
        Arc2Balance.BORROWED_COAT_LAMP_OFF_LEADS_FACTOR
      } else {
        Arc2Balance.LAMP_OFF_BASE_LEADS_FACTOR
      }

  val anomalyPotencyMultiplier: Double
    get() {
      var mult = 1.0 + completedCasePivots * 0.25
      if (CorroborationId.SUB_SURFACE_CADENCE in activeCorroborations) mult *= 1.75
      if (CorroborationId.HYDRO_WARD_SUB_BASEMENT in activeCorroborations) mult *= 1.60
      return mult
    }

  val inspectYield: Double
    get() {
      var mult = caseMasteryLeadsMult * speculativeMultiplier * focusMultiplier
      if (CorroborationId.SMUDGED_LEDGER_DATE in activeCorroborations) mult *= 1.65
      if (CorroborationId.PHANTOM_ROSTER in activeCorroborations) mult *= 1.35
      if (CorroborationId.CLOSED_MUNICIPAL_LOOP in activeCorroborations) mult *= 2.10
      if (CorroborationId.MATCHING_HAND in activeCorroborations) mult *= 2.40
      if (AuditRedaction.CLAUSE_SHIFT in dissolvedRedactions) mult *= 1.50
      val raw = (1.0 + totalContactsOwned * 0.12) * mult * lampLeadsMultiplier
      return raw.coerceAtLeast(0.25)
    }

  fun effectiveContactLeadsPerSec(contact: InvestigatorContact): Double {
    var mult = caseMasteryLeadsMult * speculativeMultiplier * focusMultiplier
    if (CorroborationId.UNLISTED_ANNEX in activeCorroborations) mult *= 1.65
    if (CorroborationId.PHANTOM_ROSTER in activeCorroborations) mult *= 1.55
    if (CorroborationId.BORROWED_COAT in activeCorroborations) mult *= 1.50
    if (CorroborationId.CLOSED_MUNICIPAL_LOOP in activeCorroborations) mult *= 2.10
    if (CorroborationId.MATCHING_HAND in activeCorroborations) mult *= 1.80
    if (AuditRedaction.CLAUSE_SEEPAGE in dissolvedRedactions) mult *= 1.40
    if (AuditRedaction.CLAUSE_QUIET in dissolvedRedactions) mult *= 1.60
    if (AuditRedaction.CLAUSE_INTAKE in dissolvedRedactions) mult *= 1.80
    return contact.baseLeadsPerSec * mult * lampLeadsMultiplier
  }

  val leadsPerSecond: Double
    get() =
      InvestigatorContact.entries.sumOf { contact ->
        contactCount(contact) * effectiveContactLeadsPerSec(contact)
      }

  val silencePerSecond: Double
    get() {
      val lampOnTrickle =
        if (lampOn) {
          if (AuditRedaction.CLAUSE_QUIET in dissolvedRedactions) 0.15 else 0.0
        } else {
          1.0
        }
      if (lampOnTrickle <= 0.0) return 0.0
      var mult = caseMasterySilenceMult * speculativeMultiplier * focusMultiplier
      if (CorroborationId.WARM_ROOM_TONE in activeCorroborations) mult *= 1.70
      if (CorroborationId.SUB_SURFACE_CADENCE in activeCorroborations) mult *= 1.40
      if (CorroborationId.WITNESS_IN_TWO_PLACES in activeCorroborations) mult *= 1.45
      if (CorroborationId.HYDRO_WARD_SUB_BASEMENT in activeCorroborations) mult *= 2.20
      if (AuditRedaction.CLAUSE_SEEPAGE in dissolvedRedactions) mult *= 1.25
      if (AuditRedaction.CLAUSE_SHIFT in dissolvedRedactions) mult *= 1.35
      if (AuditRedaction.CLAUSE_QUIET in dissolvedRedactions) mult *= 1.45
      if (AuditRedaction.CLAUSE_INTAKE in dissolvedRedactions) mult *= 1.60
      if (isReelScrubbing) {
        mult *=
          if (CorroborationId.WITNESS_IN_TWO_PLACES in activeCorroborations) {
            Arc2Balance.REEL_SCRUB_WITNESS_SILENCE_MULT
          } else {
            Arc2Balance.REEL_SCRUB_SILENCE_MULT
          }
      }
      val base =
        Arc2Balance.BASE_LAMP_OFF_SILENCE_PER_SEC +
          totalContactsOwned * Arc2Balance.SILENCE_PER_CONTACT_PER_SEC
      return base * mult * lampOnTrickle
    }

  fun isExhibitAvailableToUnlock(exhibit: CaseExhibit): Boolean =
    !exhibit.isKeystone &&
      exhibit !in pinnedExhibits &&
      caseAngle >= exhibit.unlockCaseAngle

  fun canUnlockExhibit(exhibit: CaseExhibit): Boolean =
    isExhibitAvailableToUnlock(exhibit) && silence >= exhibit.silenceCost

  val canTriggerReelScrub: Boolean
    get() =
      isReelScrubUnlocked &&
        !lampOn &&
        reelScrubRemainingSeconds <= 0.0 &&
        reelScrubCooldownSeconds <= 0.0

  fun canDissolveRedaction(redaction: AuditRedaction): Boolean =
    isRedactionSolventUnlocked &&
      !lampOn &&
      redaction !in dissolvedRedactions &&
      silence >= redaction.silenceCost

  val canPivotNextCase: Boolean
    get() {
      if (fileZeroRevealed) return false
      val c = currentCase
      return leads >= c.leadsCost.toDouble() &&
        silence >= c.silenceCost.toDouble() &&
        discoveredCorroborations.size >= c.requiredCorroborations &&
        reelScrubCount >= c.requiredReelScrubs &&
        dissolvedRedactions.size >= c.requiredRedactions
    }

  /** Backward-compatible alias for legacy callers. */
  val canConnectNextChapter: Boolean
    get() = canPivotNextCase
}
