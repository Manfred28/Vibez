# Game Design Bible — Core Vision, Theme & Feel

> **Mandatory Reference:** Every new feature, mechanic, visual change, audio/haptic cue, and narrative addition **must** be evaluated against this document before implementation. Do not add generic incremental-game tropes (cheerful confetti, upbeat gamification, bright arcade reward loops) unless they are intentionally subverted to feel uncanny, hollow, or wrong.

---

## 1. Core Goal & Chosen Narrative Direction

Build a long-form mobile incremental game following **Direction 1: *The Tending of the Unnamed Organ*** (alternative archived directions live in `/THE_ROAD_NOT_TAKEN.md`).

The player begins in a damp, windowless chamber facing a single pale valve embedded in cracked tile. Pressing it draws out dark **Ichor**. Nothing confirms what lives behind the wall—whether it is an injured cosmic organism, a machine imitating flesh, or something the player once belonged to. Patchy, contradictory fragments slip out of cracks in the grout, leaving wide room for interpretation.

Unlike conventional clickers that celebrate endless growth as triumphant power fantasy, this game uses incremental escalation as an engine for **existential dread, emotional heaviness, and quiet despair**. The player keeps pressing, tending creatures, bottling residues, grafting appendages, and severing the overgrown mass not because the world gets brighter, but because stopping feels impossible and continuing only reveals a colder, stranger chamber underneath.

---

## 2. General Feel & Aesthetic Pillars

The app should feel **surreal, grotesque, weird, and out of this world**.

### Visual & Sensory Direction
- **Dark, Desaturated, Monospaced (`BleakTheme`):** Always-dark void/ash/slate surfaces, bleached-bone typography, sickly lichen/olive/dried-clot undertones, and bundled monospaced typography (`JetBrains Mono`) throughout.
- **Handcrafted Surreal Graphic Design & Living Phase Wall Mutations:**
  - Softened outer radial auras and scanlines keep the background atmospheric and uncluttered while the **chamber wall itself transforms across Phases 0–4**: cold sterile fluorescent slate (Phase 0) → exposed cracked damp tile with corner seepage & fractures (`img_wall_phase1`, Phase 1) → sickly olive-lichen condensation with progressive bruised fleshy wall texture (`img_wall_fleshy`) & branching capillaries (Phase 2) → living Phase 3 wall animations with peristaltic tile-seam creep, contracting bruised tissue pockets, and slow subcutaneous weeping seam pores → encroaching chitinous rib arches over arterial fissures with distinct Phase 4 systolic/diastolic cardiac double-beat arterial pulse waves radiating outward from the valve.
  - **Excision II–V Chamber Mutations** layered directly onto the wall canvas as milestones unlock: iron floor drain grate with rising pressure seepage (M2), unbricked neighbor aperture slit emitting concentric amber echo ripples on every pulse (M3), bolted iron valve clamp jaws & arterial Marrow Bloom aura (M4), and the ossified throat opening with calcified teeth (M5).
  - Tarnished iron-and-calcified-bone recessed mechanical wall valve (`img_recessed_valve`) serving as the physical centerpiece: manual taps ratchet the wheel clockwise (`+24°`), while grotesque 5-fingered anatomical **Suture-Hands** continuously turn it clockwise at a speed scaling with colony count (capped at a visual ceiling) while keeping the center tap label upright, intertwined with **Grafted Choir-Tendrils**.
  - Wet-lab **Organism Specimen Dossier Cards** (`SPEC-01..04`) with framed `64dp` observation viewports, **procedural clinical-anatomical medallions** on every Specimen Jar, Graft action, Chamber Rite, and **tappable inspectable Dredged Relic** chip, live physiological status badges, live effective per-unit & colony telemetry, and silent affordability creep bars.
  - Detailed shaded anatomical Canvas art for interactive wall anomalies (**Pale Tooth**, **Blinking Glass Eye**, **Swollen Nodule**, **Split Chitin Rib**) and living chamber organisms: 7-segment undulating **Leech-Clerks** actively patrolling the perimeter track around the counter glass bezel, and suspended pear-vessel **Weeping Lamps** layered in the foreground **over** the counter plate (flanking-weighted so digits stay clear) that glow sickly lichen and weep strictly downward-falling droplets during Stillness.
  - **Grafting Sacrifice Animation & Cooldowns:** Performing Choir-Tendril or Choir-Lung grafting plays a brief (~1.4s) creature dissolution/stitching animation on the chamber canvas and starts a visible cooldown bar (`6s` Tendril / `10s` Lung, halved by *Wax-Sealed Memory*).
  - **Dual-Lane Side-Margin Dripping Transmissions (Strict Story Priority):** Newly unlocked story fragments seep slowly down the **right margin** (`~6.8s`) as a priority condensation slip while pulsing the pinned bottom ledger border. Wall anomalies, Neighbor Aperture echoes, and graft sacrifices emit slower, readable thematic notes down the **subordinate left margin** (`~4.8s`), gently dimming whenever a story fragment is descending so nothing ever interrupts or overwrites narrative beats.
  - **Decluttered Top Telemetry & Next Scar Indicator:** The top header shows only the Phase name, phase badge/`[DEV]`, progress bar, and Next Threshold. Cycle, Milestone, Permanent Resonance, and a **Next Scar Tissue visual progress bar** (inverted from the smooth power curve `floor(2.0 × (cycleLifetimeIchor / 210,000)^0.30 × hookMultiplier)`) live inside the Scars and Excision tabs.
  - **Theme Over Descriptiveness:** All upgrade, organism, jar, graft, scar node, rite, relic, anomaly, and deduction descriptions favor brief, cryptic, atmospheric copy over mechanical walkthroughs while preserving clear purchase costs, owned counts, and live `/s` readouts.
  - **Arc 2 Haunted Investigator Desk (`InvestigatorDeskCanvas`):** Shifts to a rain-streaked nocturnal slate/tungsten case desk haunted by Arc 1—wet tile grout lines bleed through the leather blotter scaled by **Complicity**, the tungsten desk lamp weeps downward grey condensation during Late-Night Silence, corner glass echoes real Arc 1 Scar Tissue counts & claw scratches, and **File #00** peels back the desk corner to expose damp masonry beneath.
- **Beyond Buttons — Living Surfaces, Creatures & Objects:** Clicking begins on the recessed valve, then expands directly onto the chamber wall, counter monolith, Chamber Rites apparatus, and eventually the haunted case desk:
  - Pulling **Pale Teeth** from hairline plaster cracks.
  - Wiping grey condensation off a **Blinking Glass Eye** embedded in masonry.
  - Prodding **Swollen Nodules** pulsing along bruised fissures.
  - Prying **Split Chitin Ribs** (`PRY`) forcing their way through Phase 4 masonry seams.
  - Sealing/venting the **Floor Drain Grate** and tapping **Dredged Relics** to inspect their thematic notes, routing **Neighbor Aperture Echoes**, engaging the **Valve Clamp** for 4-anomaly **Marrow Blooms**, and feeding colonies to **The Ossified Throat**.
  - Unsealing **Evidence Envelopes**, wiping **Fogged Spectacles**, untangling **Red String**, and playing **Reel-to-Reel Tapes** on the Arc 2 Investigator Desk.
- **Tactile Weight:** Haptics, springs, and animations feel heavy, reluctant, breathing, or unnervingly alive.

### What to Avoid
- Generic "happy mobile clicker" polish (rainbow coins, cheerful badges, upbeat motivational copy).
- Cliché cheap horror (loud jumpscares, cartoonish gore). The horror here is **weird, surreal, melancholic, and existential**.
- Static stagnation: the chamber must visibly *mutate* across phases, remember previous resets through **Scar Tissue** and the **Surgical Skill Tree**, and bleed through into Arc 2.

---

## 3. Core Emotional Resonators

Three primary emotional threads anchor the entire experience:

1. **Existential Crisis**
   - Questioning what the wall contains, who authored the contradictory notes, and whether the player is tending the organ or being slowly absorbed by it—culminating in Arc 2's **File #00** realization that the Investigator and the subject in the room are the same person.
2. **Depression**
   - Emotional heaviness, exhaustion, numbness, and quiet inertia—expressed mechanically through **Weeping Lamps / Stillness** in Arc 1 and **Late-Night Silence** in Arc 2, which drip only when the player stops pressing and sits with the dim screen.
3. **Hopelessness & Quiet Acceptance**
   - Not the frantic panic of a locked-room thriller, but the slow dissolution of the boundary between *wanting to leave* and *having always belonged to the room*. Across Arc 1, every check of the door, circular drain, shared neighbor cadence, clamped breath, and municipal clause blurs whether an outside still waits—culminating at Excision VI in **Plead for Salvation**, an ambiguous petition caught on the knife-edge between praying to be rescued and asking forgiveness for wanting to stay, which splits straight into Arc 2's rain-streaked Inquiry.

> **Tone Guardrail:** Explore these emotions through surrealism, cosmic alienation, melancholic philosophy, and strange metaphor. Never depict, encourage, or trivialize real-world self-harm.

---

## 4. Escalation Curve, 6-Excision Arc 1 Progression & Arc 2–3 Architecture

### 4.1 First-Cycle Pacing (Cycle 0: Start → Excision I, ~15 Minutes)

Cycle 0 is balanced for **~15 minutes with moderate activity** from the first press to Excision I:

| Phase | Working Name | Cycle Ichor Threshold | Target Window | Mechanics & Chamber Transformation |
| :--- | :--- | :--- | :--- | :--- |
| **0** | *The Counting Room* | `0` | `0:00 – 1:00` | Cold sterile slate tiled wall, recessed iron-and-bone valve (`+1` Ichor), silent ledger. **Suture-Hands** unfold at `25` Ichor (cost `25`, `+0.6/s`) and visibly reach onto the valve rim. |
| **1** | *Uncanny Drift* | `75` | `1:00 – 4:15` | Cracked damp tiled wall (`img_wall_phase1`) surfaces with corner seepage & fractures. **Pale Teeth** protrude from plaster seams (`+25` Ichor, `+0.5` tap yield). **Leech-Clerks** unlock (cost `180`, `+2.4/s`) and cling to the counter glass. |
| **2** | *Apparatus & Flesh* | `1,600` | `4:15 – 8:30` | Sickly olive tint, progressive fleshy wall texture (`img_wall_fleshy`), condensation streaks & branching veins. **Weeping Lamps** unlock at `1,000` (`1,190` Ichor, `+6/s`) and hang in the foreground; pausing taps for 3s triggers **Stillness** harvesting (`+0.45/s` & `2×` lamp Ichor). **Blinking Glass Eye** fogs over (wipe for 12s `1.6×` gaze boost). Foundational **Specimen Jars** unlock (`24 / 65 / 140` Stillness). |
| **3** | *Collapse of Meaning* | `16,000` | `8:30 – 12:45` | Living peristaltic tile-seam creep, contracting bruised tissue pockets, and subcutaneous weeping pores. **Swollen Nodules** (`+220` Ichor, `+6` Stillness). **Salt from the Grout** Jar (`320` Stillness) unlocks. **Grafting** unlocks: fuse 2 Suture-Hands + 2 Leech-Clerks + scaling Stillness (`35 × 1.55^n`, `6s` cooldown) into 1 **Grafted Choir-Tendril** (`+18/s` Ichor, `+0.7/s` Stillness). |
| **4** | *The Endless Hollow* | `85,000` | `12:45 – 15:15` | Chitinous rib arches encroach inward over deep arterial fissures with cardiac double-beat pulse waves radiating from the valve. **A Borrowed Pulse** Jar (`750` Stillness) unlocks. Interactive **Split Chitin Ribs** (`PRY`: `+1,400` Ichor, `+14` Stillness, `6s` `1.6×` Gaze without breaking Stillness) emerge through the seams alongside mid-phase narrative beats (`145,000`). At **`210,000` Cycle Ichor**, **Excision I** unlocks (and post-Severance **The Uncounted Hour** Jar at `1,800` Stillness). |

### 4.2 Arc 1 — Six Sequential Excision Milestones (`M1..M6`), Smooth Scar Harvest & 5-Tier Surgical Scar Tree

Prestiges are tuned around a deliberate **2–3 runs per milestone** rhythm from `M2` through `M6` (1–2 brisk Scar-farming runs to invest in the Surgical Scar Tree & Repeatable Grafts, followed by a milestone push run using the latest chamber mechanics). **Hard Anti-Skip Sequential Cap:** every Excision advances `highestExcisionMilestone` by at most **`+1`** regardless of excess cycle Ichor (and **Excision VI // Plead for Salvation** into Arc 2 strictly requires `M5` already cleared plus `12,000,000,000` Cycle Ichor).

- **Smooth Diminishing-Returns Scar Harvest (Zero Milestone Bonus Scars):** First-clear milestone bonuses are `0` across all `M1..M6` and on Breach. Every Excision at `>= 210,000` Cycle Ichor yields `max(2, floor(2.0 × (cycleLifetimeIchor / 210,000)^0.30 × (1 + 0.12 × scarHarvestHookRank)))`, rewarding both brisk farming runs and deep milestone pushes without collapsing the tree.
- **Root-Softcapped Passive Scar Resonance:** `ichor = (1 + highestMilestone × 0.20 + sqrt(lifetimeScar) × 0.25) × FIRST_INCISION(1.45×) × BONE_RESONANCE(1.85×) × KELOID(1 + 0.18 × rank)`; `stillness = (1 + highestMilestone × 0.15 + sqrt(lifetimeScar) × 0.18) × FIRST_INCISION(1.20×) × SHORTER_SILENCE(1.40×) × SEDIMENT(1 + 0.16 × rank)`.
- **6-Tier Specimen Jars (Stillness Sink):** Foundational jars (`YESTERDAYS_DUST` 24, `UNSPENT_BREATH` 65, `ALMOST_A_NAME` 140), deeper phase jars (`SALT_FROM_THE_GROUT` 320 at P3+: `+45%` organisms; `A_BORROWED_PULSE` 750 at P4+: `+55%` graft/lung Ichor & all Stillness), and post-Severance seal (`THE_UNCOUNTED_HOUR` 1,800 at Cycle 1+ / M1+: `+75%` global Ichor & all Stillness). All jars clear on Excision so deep seals remain a meaningful Stillness sink every cycle.
- **16-Node 5-Tier Branching Scar Tissue Surgical Skill Tree (unlocked at `M1`):**
  - **Root:** `FIRST_INCISION` (1 Scar — `×1.45` Ichor, `×1.20` Stillness; opens all 3 branches and Repeatable Surgical Grafts).
  - **Suture Branch (T1–T5, run compression & valve automation):** `CALLOUSED_PALM` (2 Scar — start runs with 3 Suture-Hands + 1 Leech-Clerk, `×1.8` manual yield), `TWIN_STITCH` (5 Scar — `×1.6` Suture/Leech yield, valve taps echo `+1.5s` passive Ichor), `COLONY_INERTIA` (11 Scar — `×1.75` all organisms, `−0.04` cost growth exponent), `MEMBRANE_RECALL` (22 Scar — start runs with +1 Weeping Lamp & +1 Sump-Siphon once `M2+` cleared, `×1.6` Siphons & Tendrils), `PERPETUAL_SEAM` (45 Scar — auto-ratchets valve every `1.2s` without breaking Stillness, `×1.8` organisms & manual taps).
  - **Stillness Branch (T1–T5, depressive idle depth):** `SHORTER_SILENCE` (2 Scar — Stillness triggers in `1.5s`, `×1.40` Stillness), `WAX_SEALED_MEMORY` (5 Scar — `−30%` Stillness costs across all Specimen Jars & Grafts, halves Grafting cooldowns, `+0.35` Jar potency), `DEEP_WEEPING` (11 Scar — Weeping Lamps `×1.8` Ichor, drip `40%` Stillness while tapping), `HOLLOW_LUNG_CHOIR` (22 Scar — Choir-Lungs `×1.85` Ichor & Stillness, `2.0×` idle boost; Lamps `2.4×` Ichor in Stillness), `CATATONIC_TIDE` (45 Scar — `×1.75` global Ichor & Stillness while Stillness is active, `+0.35` Jar potency).
  - **Marrow Branch (T1–T5, wall mutations & chamber rites):** `SOFTENED_MORTAR` (2 Scar — `1.6×` anomaly spawn speed, cap `4`, `×1.8` anomaly rewards), `AUTO_TENDRIL_SUTURE` (6 Scar — auto-graft Choir-Tendrils when 4+ Hands & 4+ Leeches & Stillness/cooldown ready in Phase 3+, `×1.55` Tendrils & Siphons), `ECHO_OSSUARY` (12 Scar — Drain pressure builds `1.45×` faster, Relic Ichor bonus `×1.45`, Neighbor Aperture Echo interval `9s` with `×1.35` yield), `BLOOM_INCISION` (24 Scar — Valve Clamp hold drops to `15s`, Marrow Bloom lasts `32s` at `2.9×` Ichor, `+40%` anomaly yield during Bloom), `BONE_RESONANCE` (48 Scar — `×1.85` global Ichor, `×1.25` Relic & Echo synergy, `×1.12` per Unsealed Dossier Clause).
- **3 Repeatable Surgical Grafts (unlocked once `FIRST_INCISION` is sutured):**
  - `KELOID_THICKENING` (`ceil(3 × 1.45^r)` Scar — `+18%` multiplicative Ichor per rank).
  - `SEDIMENT_WELL` (`ceil(3 × 1.45^r)` Scar — `+16%` Stillness per rank & `0.96^r` Stillness cost multiplier floored at `0.68`).
  - `SCAR_HARVEST_HOOK` (`ceil(4 × 1.55^r)` Scar — `+12%` Scar Tissue harvested per Excision per rank).

| Milestone | Cycle Ichor Goal | First-Clear Bonus | Distinct Feature Unlocked |
| :--- | :--- | :--- | :--- |
| **Excision I** (*First Severance*) | `210,000` | `0` Scar | Unlocks the **SCARS** tab, 5-tier Scar Tissue Surgical Skill Tree & Repeatable Grafts. |
| **Excision II** (*The Sunken Drain*) | `1,800,000` | `0` Scar | Unlocks **RITES** tab: **Floor Drain Grate** Seal/Vent (`+4% + 1.5%/siphon` pressure → Dredge 3 Relics: *Wristband* `1.28×`, *Brass Key* `1.35×`, *Logbook* `1.45×`) + 4th Organism **`SUMP_SIPHON`** (`SPEC-04`, `42/s`). |
| **Excision III** (*The Unbricked Seam*) | `16,000,000` | `0` Scar | Unlocks **Neighbor Aperture Echo Routing** (`0% / 25% / 50%` diversion → `12s`/`9s` **Resonance Echo** bursts `18×`/`30×` + amplified anomalies). |
| **Excision IV** (*The Breath-Hold*) | `140,000,000` | `0` Scar | Unlocks **Valve Clamp** (`20s`/`15s` manual lock → 4-anomaly **Marrow Bloom** at `2.2×`/`2.9×` surge) & **Choir-Lung Grafting** (`2 Weeping Lamps + 1 Tendril` → `+160/s` Ichor, `+3.5/s` Stillness). |
| **Excision V** (*The Membrane Audit*) | `1,250,000,000` | `0` Scar | Unlocks **The Ossified Throat** & 4-line Redacted Cult Dossier (*Rite of the Quiet Vessel* — feed colonies to permanently unseal compounding multipliers `1.35× / 1.40× / 1.45× / 1.55×`, up to `~4.25×` base / `~6.7×` with *Bone Resonance*). |
| **Excision VI** (*Plead for Salvation*) | `12,000,000,000` | `0` Scar | Strictly requires `M5` already cleared; unlocks **Plead for Salvation** → lays an ambiguous petition (half prayer for rescue, half quiet surrender) against the last wet seam, cutting into **Arc 2 (*The Inquiry*)**. |

### 4.3 Arc 2 — *The Inquiry* (Haunted Investigator Desk) & *File #00* Twist

- **Phase 5 (*The Inquiry*):** Replaces the chamber UI with a rain-streaked slate/tungsten **Investigator Desk** (`InvestigatorScreen`) while keeping the shared pinned **Story Ledger** at the bottom. Opens on **Case Folder #14-B** with an unsigned, water-stained **plea for salvation** smelling faintly of damp grout—leaving completely ambiguous whether the Investigator is an outside rescuer answering a call or the half of the Caretaker that stepped into the rain to inspect their own acceptance, until Chapter 4.
- **Mechanics:** **Stamp Case File** (`Leads`), **Late-Night Silence** (`Silence` after `2.5s` idle), 4 **Desk Anomalies** (`evidence_envelope`, `fogged_spectacles`, `red_string`, `reel_tape`), 4 **Contacts** (*Night Archivist*, *Bribed Orderly*, *Microfilm Reader*, *Reel Transcriber*), 3 **Deductions** (*Hydro-Ward Invoices*, *Caretaker Roster*, *Mortar Sample Match*), and 4 **Red-String Chapters** (*Ch. I: Missing Caretakers*, *Ch. II: The Hydro-Ward Cult*, *Ch. III: Brick-Work Blueprint*, *Ch. IV: File #00 — Subject Intake*).
- **Haunting & Twist:** As **Complicity** (`0.05..1.00`) climbs, Arc 1 bleeds visually into the desk. Completing Chapter 4 opens **File #00**, revealing the Investigator's own left-thumb seam scar and intake signature—proving they are the subject who sent the plea and never left the chamber—and unlocking **Phase 6: Arc 3 (*The Convergence*)**, currently preserved as a clean open threshold screen allowing return to Arc 1 or Arc 2.

### 4.4 Clinical Dev Console (Testing & QA Tool)

- Accessible via the discrete `[DEV]` header pill across all arcs on both Android and Web (`OrganDevEngine` / `DevToolsModal` / `#dev-modal`).
- Allows arbitrary Arc switching (`ARC 1..3`), unlocking any Excision Milestone (`M0..M6`), unlocking/clearing all 16 Scar Nodes (and clearing Repeatable Grafts), assigning exact or preset Arc 1 (`Ichor`, `Stillness`, `Scar`) & Arc 2 (`Leads`, `Silence`, `CH0..4`) values, spawning Wall/Desk anomalies, fast-forwarding `+60s`, and wiping local save data (`SharedPreferences` / `localStorage`).

---

## 5. Dialogue & Narrative Storage Contract

Dialogues are patchy, ambiguous fragments and **must always be stored separately** from Kotlin UI/business logic. Follow `/skills/story-writing/SKILL.md` whenever authoring or editing fragments so new entries build coherently on earlier fragments without contradictions.

- **Canonical Storage Location:** `app/src/main/assets/dialogues.json` (`schemaVersion: 3`, 7 phases `0..6`, mirrored byte-for-byte to `docs/assets/dialogues.json` for the GitHub Pages playable web version and verified by automated parity tests).
- **Strict Separation Rule:**
  - Do **not** hardcode dialogue lines inside Jetpack Compose screens, ViewModels, game logic classes, `strings.xml`, or web HTML/JS modules.
  - Keep `res/values/strings.xml` (and clinical web UI chrome) strictly for UI telemetry headings, accessibility labels, and system strings.
- **Supported Progress-Binding Trigger Types:**
  - `counter_reached` — Cycle lifetime Ichor reaches `threshold`.
  - `cycle_counter_reached` — Player is in Cycle `>= target` AND Cycle lifetime Ichor reaches `threshold` (post-prestige story beats).
  - `creature_owned` — Owned count of `target` (`suture_hand`, `leech_clerk`, `weeping_lamp`, `sump_siphon`, `grafted_tendril`, `choir_lung`) reaches `threshold`.
  - `anomaly_interacted` — Lifetime interactions with `target` (`pale_tooth`, `glass_eye`, `swollen_nodule`, `chitin_rib`) reach `threshold`.
  - `stillness_reached` — Accumulated Stillness reaches `threshold`.
  - `jar_bottled` — Number of Specimen Jars bottled reaches `threshold`.
  - `excision_completed` — Completed Excision/Molt cycles reach `threshold`.
  - `excision_milestone_reached` — `highestExcisionMilestone` reaches `threshold` (`1..6`).
  - `scar_node_unlocked` — Count of unlocked Scar Tissue nodes reaches `threshold`.
  - `scar_node_specific` — Specific Scar Tissue node `target` (e.g. `membrane_recall`, `perpetual_seam`, `hollow_lung_choir`, `catatonic_tide`, `bloom_incision`, `bone_resonance`) is unlocked (`threshold: 1`).
  - `repeatable_graft_total` — Combined ranks across all Repeatable Surgical Grafts reach `threshold`.
  - `relic_dredged` — Lifetime dredged relics count reaches `threshold`.
  - `echo_routed` — Triggered Neighbor Aperture echo pulses reach `threshold`.
  - `marrow_bloom` — Completed Valve Clamp Marrow Blooms reach `threshold`.
  - `dossier_line_unredacted` — Unsealed *Rite of the Quiet Vessel* dossier lines reach `threshold` (`1..4`).
  - `arc_entered` — Active arc equals `target` (`arc_2`, `arc_3`) (`threshold: 1`).
  - `case_chapter_completed` — Arc 2 completed chapters reach `threshold` (`1..4`, with `4` triggering *File #00*).
  - `desk_anomaly_interacted` — Arc 2 interactions with `target` (`evidence_envelope`, `fogged_spectacles`, `red_string`, `reel_tape`) reach `threshold`.

---

## 6. Mandatory Feature Alignment Checklist

Before designing or implementing **any** new feature (upgrades, creatures, objects, prestige loops, visuals, sounds, UI screens, or dialogue triggers), verify:

1. **Theme Fit:** Does this feature feel surreal, grotesque, weird, or otherworldly rather than generic or locked-room thriller cliché?
2. **Emotional Resonance:** Does it touch existential crisis, depression, hopelessness, or quiet acceptance—either directly or by making an initially mundane reward feel hollow?
3. **Escalation Hook:** Does its presentation, behavior, or chamber presence intensify across Phases 0–4, Excisions I–VI, and into Arc 2 along the dissolving line between *leaving* and *having always been here*?
4. **Dialogue Separation:** Are all new narrative fragments stored in `app/src/main/assets/dialogues.json` and bound to progression triggers?
5. **Cohesion:** Does it preserve the uncluttered, atmospheric focus of the mobile experience?
