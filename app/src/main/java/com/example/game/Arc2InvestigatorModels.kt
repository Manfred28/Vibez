package com.example.game

import kotlin.math.floor
import kotlin.math.pow

enum class InvestigatorContact(
  val id: String,
  val baseCost: Double,
  val costGrowth: Double,
  val baseLeadsPerSec: Double,
  val unlockLeadsThreshold: Long,
) {
  NIGHT_ARCHIVIST(
    id = "night_archivist",
    baseCost = 20.0,
    costGrowth = 1.22,
    baseLeadsPerSec = 1.2,
    unlockLeadsThreshold = 15L,
  ),
  BRIBED_ORDERLY(
    id = "bribed_orderly",
    baseCost = 150.0,
    costGrowth = 1.25,
    baseLeadsPerSec = 6.5,
    unlockLeadsThreshold = 90L,
  ),
  MICROFILM_SCANNER(
    id = "microfilm_scanner",
    baseCost = 950.0,
    costGrowth = 1.28,
    baseLeadsPerSec = 28.0,
    unlockLeadsThreshold = 700L,
  ),
  AUDIO_TRANSCRIBER(
    id = "audio_transcriber",
    baseCost = 5_500.0,
    costGrowth = 1.30,
    baseLeadsPerSec = 120.0,
    unlockLeadsThreshold = 4_000L,
  );

  companion object {
    fun byId(id: String): InvestigatorContact? = entries.firstOrNull { it.id == id }
  }
}

enum class CaseDeduction(val id: String, val silenceCost: Double) {
  HYDRO_WARD_INVOICES(id = "hydro_ward_invoices", silenceCost = 18.0),
  MISSING_CARETAKER_ROSTER(id = "missing_caretaker_roster", silenceCost = 50.0),
  MORTAR_SAMPLE_MATCH(id = "mortar_sample_match", silenceCost = 115.0);

  companion object {
    fun byId(id: String): CaseDeduction? = entries.firstOrNull { it.id == id }
  }
}

enum class DeskAnomalyType(val id: String, val minChapter: Int) {
  EVIDENCE_ENVELOPE(id = "evidence_envelope", minChapter = 0),
  FOGGED_SPECTACLES(id = "fogged_spectacles", minChapter = 1),
  RED_STRING(id = "red_string", minChapter = 2),
  REEL_TAPE(id = "reel_tape", minChapter = 3);

  companion object {
    fun byId(id: String): DeskAnomalyType? = entries.firstOrNull { it.id == id }
  }
}

data class ActiveDeskAnomaly(
  val id: Long,
  val type: DeskAnomalyType,
  val xFraction: Float,
  val yFraction: Float,
)

enum class InvestigationChapter(
  val number: Int,
  val leadsCost: Long,
  val silenceCost: Long,
) {
  MISSING_CARETAKERS(number = 1, leadsCost = 400L, silenceCost = 0L),
  HYDRO_WARD_CULT(number = 2, leadsCost = 4_500L, silenceCost = 20L),
  BRICK_WORK_BLUEPRINT(number = 3, leadsCost = 35_000L, silenceCost = 65L),
  FILE_ZERO_SUBJECT_INTAKE(number = 4, leadsCost = 180_000L, silenceCost = 140L);

  companion object {
    fun nextAfter(completedChapters: Int): InvestigationChapter? =
      entries.firstOrNull { it.number == completedChapters + 1 }
  }
}

enum class InvestigatorDeckTab {
  CONTACTS,
  DEDUCTIONS,
  EVIDENCE_BOARD,
}

object Arc2Balance {
  const val SILENCE_IDLE_DELAY_SEC: Double = 2.5
  const val BASE_DESK_SILENCE_PER_SEC: Double = 0.65
  const val CONTACT_SILENCE_PER_UNIT: Double = 0.25
  const val FOCUS_DURATION_SEC: Double = 14.0
  const val FOCUS_MULTIPLIER: Double = 1.8
  const val INVOICES_TAP_BONUS: Double = 4.0
  const val ROSTER_CONTACT_MULTIPLIER: Double = 2.0
  const val MORTAR_SILENCE_AND_ANOMALY_MULTIPLIER: Double = 2.2
  const val FIRST_DESK_ANOMALY_DELAY_SEC: Double = 10.0
  const val DESK_ANOMALY_INTERVAL_SEC: Double = 20.0
  const val MAX_DESK_ANOMALIES: Int = 3
  const val ENVELOPE_BASE_LEADS: Double = 40.0
  const val RED_STRING_BASE_LEADS: Double = 320.0
  const val RED_STRING_BASE_SILENCE: Double = 9.0
  const val REEL_TAPE_BASE_LEADS: Double = 2_400.0
  const val REEL_TAPE_BASE_SILENCE: Double = 20.0
}

/**
 * Immutable state for Arc 2 ("The Inquiry" — Haunted Investigator Desk).
 * Complicity (0f..1f) governs how strongly the Arc 1 chamber bleeds into the desk visuals.
 */
data class Arc2DeskState(
  val leads: Double = 0.0,
  val lifetimeLeads: Double = 0.0,
  val silence: Double = 0.0,
  val lifetimeSilence: Double = 0.0,
  val complicity: Float = 0.05f,
  val contacts: Map<InvestigatorContact, Int> = emptyMap(),
  val ownedDeductions: Set<CaseDeduction> = emptySet(),
  val completedChapters: Int = 0,
  val activeDeskAnomalies: List<ActiveDeskAnomaly> = emptyList(),
  val deskAnomalyInteractions: Map<DeskAnomalyType, Int> = emptyMap(),
  val secondsSinceLastAction: Double = 0.0,
  val isSilenceActive: Boolean = false,
  val focusRemainingSeconds: Double = 0.0,
  val secondsUntilNextAnomaly: Double = Arc2Balance.FIRST_DESK_ANOMALY_DELAY_SEC,
  val nextAnomalyId: Long = 1L,
  val fileZeroRevealed: Boolean = false,
) {
  val leadsDisplay: Long
    get() = floor(leads).toLong()

  val lifetimeLeadsDisplay: Long
    get() = floor(lifetimeLeads).toLong()

  val silenceDisplay: Long
    get() = floor(silence).toLong()

  fun contactCount(contact: InvestigatorContact): Int = contacts[contact] ?: 0

  fun contactCost(contact: InvestigatorContact): Long {
    val owned = contactCount(contact)
    return floor(contact.baseCost * contact.costGrowth.pow(owned)).toLong()
  }

  val focusMultiplier: Double
    get() = if (focusRemainingSeconds > 0.0) Arc2Balance.FOCUS_MULTIPLIER else 1.0

  val chapterMultiplier: Double
    get() = 1.0 + completedChapters * 0.65

  val inspectYield: Double
    get() {
      val invoiceBonus =
        if (CaseDeduction.HYDRO_WARD_INVOICES in ownedDeductions) Arc2Balance.INVOICES_TAP_BONUS
        else 0.0
      val envelopeBonus = (deskAnomalyInteractions[DeskAnomalyType.EVIDENCE_ENVELOPE] ?: 0) * 1.0
      return (1.0 + invoiceBonus + envelopeBonus) * chapterMultiplier
    }

  fun effectiveContactLeadsPerSec(contact: InvestigatorContact): Double {
    val rosterMult =
      if (CaseDeduction.MISSING_CARETAKER_ROSTER in ownedDeductions) {
        Arc2Balance.ROSTER_CONTACT_MULTIPLIER
      } else {
        1.0
      }
    return contact.baseLeadsPerSec * rosterMult * chapterMultiplier * focusMultiplier
  }

  val leadsPerSecond: Double
    get() = InvestigatorContact.entries.sumOf { contactCount(it) * effectiveContactLeadsPerSec(it) }

  val silencePerSecond: Double
    get() {
      if (!isSilenceActive) return 0.0
      val totalContacts = contacts.values.sum()
      val mortarMult =
        if (CaseDeduction.MORTAR_SAMPLE_MATCH in ownedDeductions) {
          Arc2Balance.MORTAR_SILENCE_AND_ANOMALY_MULTIPLIER
        } else {
          1.0
        }
      val base = Arc2Balance.BASE_DESK_SILENCE_PER_SEC + totalContacts * Arc2Balance.CONTACT_SILENCE_PER_UNIT
      return base * mortarMult * focusMultiplier
    }

  val nextChapter: InvestigationChapter?
    get() = InvestigationChapter.nextAfter(completedChapters)

  val canConnectNextChapter: Boolean
    get() {
      val chapter = nextChapter ?: return false
      return leads >= chapter.leadsCost.toDouble() && silence >= chapter.silenceCost.toDouble()
    }
}
