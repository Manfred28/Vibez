---
name: story-writing
description: Guides authoring and editing patchy, surreal, progress-bound narrative fragments in app/src/main/assets/dialogues.json. Always audits all previous fragments first so new entries build coherently on established threads or stand as eerie one-off snippets without glaring contradictions.
---

# Story Writing Skill — *The Tending of the Unnamed Organ*

Use this skill whenever adding, rewriting, or expanding any dialogue, note, ledger entry, speaker attribution, or prestige/cycle narrative fragment in `/app/src/main/assets/dialogues.json`.

---

## 1. Mandatory Pre-Writing Audit (Never Write Blind)

Before drafting a single new line:

1. **Read `/GAME_DESIGN_BIBLE.md`** to verify emotional resonators (**existential crisis, depression, hopelessness**), surreal/grotesque tone, and phase boundaries.
2. **Read the ENTIRE `/app/src/main/assets/dialogues.json`** from top to bottom.
3. **Extract a Continuity Ledger** of existing canonical motifs, physical invariants, speakers, and open threads before writing:
   - **Physical Invariants of the Chamber:**
     - Windowless, damp, tiled room with grout seams and a cold iron-and-bone recessed wall valve.
     - No confirmed identity for what lies behind the wall (never definitively name it as an alien, demon, god, or specific sci-fi machine).
     - The corridor behind the door was bricked over three winters ago (`dlg_p3_collapse_meaning`).
     - Excision cuts the overgrowth back to cold bare tile, leaving permanent pale stitched **Scar Tissue** seams across the wall (`dlg_cycle_1_excision`).
   - **Recurring Motifs & Open Threads (Safe to Build On):**
     - **The Caretaker's Left Thumb Scar:** Suture-Hands share the same pale scar on their knuckles (`dlg_p0_suture_hand`).
     - **Bled-Ink Date / Previous Caretaker:** "Day 14 (or 41—the ink bled)" warning about white enamel molars pushing through softening mortar (`dlg_p1_cracked_grout`, `dlg_p1_first_tooth`).
     - **Blind Leech-Clerks:** Segmented creatures grazing on the tally behind the counter glass (`dlg_p1_leech_clerk`).
     - **Stillness & Unaddressed Letter:** Sitting untouched while Weeping Lamps sweat and specimen jars fill; forgetting what was planned outside (`dlg_p2_weeping_lamp`, `dlg_p2_stillness`, `dlg_p2_first_jar`).
     - **The Masonry Eye & Rhythm:** Something behind the brick recognizes the rhythm, not the person (`dlg_p2_glass_eye`).
     - **Initials Under the Valve:** Written long before the player arrived, discovered after Excision (`dlg_cycle_1_excision`).
     - **Arc 1 Surreal Drift (Leaving ↔ Acceptance → *Plead for Salvation*):** Arc 1 moves along an **ontological axis**, never a literal prison-break plot. Cycle 0 opens with the hazy assumption you only stepped in briefly before going home, then discovers mortar older than your arrival. Excisions I–V dissolve the boundary between leaving and belonging: circular sump plumbing returning relics dated tomorrow (M2), shared scarred-thumb cadence across the hairline seam (M3), clamping the valve to remember the smell of rain and feeling shameful relief when the warm plaster heals shut (M4), and *Rite of the Quiet Vessel* Clauses I–IV framing tenancy as calm municipal routine (M5). Excision VI (**Plead for Salvation**) sits on the knife-edge between praying to be pulled out and asking forgiveness for wanting to stay—cutting directly into Arc 2's rain-streaked Case Folder #14-B as if one half of the Caretaker stepped outside to inspect what remained, preserving complete ambiguity until *File #00*.

---

## 2. Thread Continuation vs. One-Off Snippets

Every new fragment must deliberately choose **one** of two narrative roles:

1. **Thread Echo / Escalation (Recommended for major phase milestones & Cycle 1+ prestige beats):**
   - Pick **1–2** concrete details from earlier entries (e.g., the bled-ink caretaker slip, the unaddressed letter, the bricked corridor, the left-thumb scar, or the initials under the valve) and twist or deepen them.
   - Example: A Cycle 1 fragment noticing that the "Previous Caretaker's" handwriting matches your own—deepening existential vertigo without over-explaining how the loop began.
2. **Standalone One-Off Snippet (Recommended for new anomalies, creatures, or minor side objects):**
   - An isolated institutional slip, clinical margin note, or strange sensory observation that widens the world's texture without tying into a central plot.
   - Must still respect all Physical Invariants above.

---

## 3. Surreal Overarching Direction & Anti-Cliché Guardrails

Patchy, ambiguous storytelling thrives on **gaps** and **ontological drift**, never locked-room thriller logistics or sloppy continuity errors.

- **Move Along an Internal Axis, Not a Prison-Break Axis:** Overarching momentum comes from the thinning line between *wanting to leave* and *having always belonged to the room* (temporary errand → strange familiarity → circular remnants → shared cadence → shameful relief in warmth → ambiguous surrender/plea).
- **Keep Two Readings Alive Simultaneously:** Every major milestone should hold both readings in tension: *(A)* an outside world still exists and you are reaching toward it, AND *(B)* you have always belonged to the chamber and "outside" is a comforting story you rehearse while tending the valve.
- **Bureaucratic Calm Over Thriller Urgency:** Prefer calm, municipal, damp, dream-wrong gestures (setting a smooth key politely by brick, listening to finished silence, writing a name in grey wax) over action-horror verbs.
- **DO:** Use redacted dates, water-stained words, contradictory institutional jargon ("Patient / Facility"), circular time (pages dated tomorrow), and subjective doubt (*"perhaps"*, *"you think"*, *"unable to tell"*).
- **DO NOT:**
  - Use literal escape-room / survival-horror clichés: rattling locked doorknobs, climbing vents for air, tapping Morse code SOS, hoping for crowbars, hauling on jail bars, trying to blow walls open, or literal "message-in-a-bottle" storm-pipe logistics.
  - Contradict established physical facts (e.g., do not mention looking out a window, walking down an open hallway after the door was bricked over, or the valve suddenly being made of gold/plastic).
  - Contradict established mechanic rules (e.g., Weeping Lamps harvest Stillness when idle, not when tapping rapidly; Excision resets the chamber back to bare scarred tile, not a new building).
  - Resolve the core mystery cleanly ("Project 9 was an experiment by Dr. X..."). Always leave room for interpretation.
  - Break tone with humor, heroic optimism, arcade hype, or generic jumpscare clichés.
  - Depict, encourage, or trivialize real-world self-harm.

---

## 4. Voice & Escalation Matrix

Match diction, rhythm, and emotional weight strictly to the entry's `phase` and `cycle`:

| Phase / State | Mood Palette | Stylistic Direction |
| :--- | :--- | :--- |
| **Phase 0** (*The Counting Room*) | `clinical`, `uncanny` | Sparse, institutional, slightly off. Short declarative sentences. |
| **Phase 1** (*Uncanny Drift*) | `uncanny`, `grotesque` | Softening mortar, misplaced anatomy (teeth, scars, segmented clerks), quiet doubt. |
| **Phase 2** (*Apparatus & Flesh*) | `heavy`, `depressive`, `surreal` | Warmth without affection, exhaustion harvested as Stillness, memories losing labels. |
| **Phase 3** (*Collapse of Meaning*) | `existential`, `grotesque` | Futility of optimization, sealed exits, surgical compromise for marginally better yield. |
| **Phase 4** (*The Endless Hollow*) | `hopeless` | Claustrophobic overgrowth, ribs of tile, quiet resignation before the blade. |
| **Cycle 1+ (Post-Prestige)** | `hopeless`, `existential` | Déjà vu, overwritten records, realizing Scar Tissue makes the room learn your rhythm faster—not freer. |

---

## 5. Technical Storage & Validation Contract

- **File:** `/app/src/main/assets/dialogues.json` ONLY. Never hardcode narrative lines in Kotlin Composables, ViewModels, or `strings.xml`.
- **Length:** 1–3 concise, evocative lines per `lines` array (optimized for mobile legibility in the Grout Ledger panel).
- **ID Convention:** Unique `snake_case` IDs (`dlg_p<phase>_<slug>` or `dlg_cycle_<n>_<slug>`).
- **Supported Trigger Types:**
  - `counter_reached` (`threshold`: Cycle lifetime Ichor)
  - `cycle_counter_reached` (`target`: minimum cycle as integer string e.g. `"1"`, `threshold`: Cycle lifetime Ichor)
  - `creature_owned` (`target`: `suture_hand` | `leech_clerk` | `weeping_lamp` | `sump_siphon` | `grafted_tendril` | `choir_lung`, `threshold`)
  - `anomaly_interacted` (`target`: `pale_tooth` | `glass_eye` | `swollen_nodule` | `chitin_rib`, `threshold`)
  - `stillness_reached` (`threshold`: Cycle lifetime Stillness)
  - `jar_bottled` (`threshold`: count of owned Specimen Jars `1..6`)
  - `excision_completed` (`threshold`: completed Excision cycle count)
  - `excision_milestone_reached` (`threshold`: `highestExcisionMilestone` `1..6`)
  - `scar_node_unlocked` (`threshold`: unlocked Scar Tree node count `1..16`)
  - `scar_node_specific` (`target`: exact `ScarNodeId.id`, `threshold`: `1`)
  - `repeatable_graft_total` (`threshold`: sum of ranks across all Repeatable Surgical Grafts)
  - `relic_dredged` (`threshold`: lifetime dredged relics count)
  - `echo_routed` (`threshold`: Neighbor Aperture echo pulse count)
  - `marrow_bloom` (`threshold`: Valve Clamp Marrow Bloom count)
  - `dossier_line_unredacted` (`threshold`: unsealed *Rite of the Quiet Vessel* clauses `1..4`)
  - `arc_entered` (`target`: `arc_2` | `arc_3`, `threshold`: `1`)
  - `case_chapter_completed` (`threshold`: Arc 2 chapters `1..4`)
  - `desk_anomaly_interacted` (`target`: `evidence_envelope` | `fogged_spectacles` | `red_string` | `reel_tape`, `threshold`)

---

## 6. Pre-Commit Story Audit Checklist

Before finalizing any story edit, verify every new entry against these 6 checks:

1. [ ] Did I read all previous entries in `dialogues.json` first?
2. [ ] Is this entry intentionally either a **Thread Echo** (explicitly reinforcing an existing motif) or a **Standalone Snippet**?
3. [ ] Does it have **zero glaring contradictions** with established physical invariants, mechanics, or earlier notes?
4. [ ] Does its tone escalate appropriately for its `phase` / `cycle` and resonate with existential crisis, depression, or hopelessness?
5. [ ] Does it preserve ambiguity rather than over-explaining the Unnamed Organ?
6. [ ] Does `DialogueAssetValidationTest` pass cleanly after the edit?
