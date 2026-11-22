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
- **Handcrafted Surreal Graphic Design & Phase Wall Mutations:**
  - Damp, cracked institutional tiled chamber wall (`img_wall_phase1`) that **visually transforms across Phases 0–4**: cold sterile fluorescent slate (Phase 0) → exposed cracked damp tile with corner seepage & fractures (Phase 1) → sickly olive-lichen condensation & pulsing veins (Phase 2) → inward-buckling warped tile grid & bruised tissue pockets (Phase 3) → encroaching chitinous rib arches over arterial fissures (Phase 4).
  - **Excision II–V Chamber Mutations** layered directly onto the wall canvas as milestones unlock: iron floor drain grate with rising pressure seepage (M2), unbricked neighbor aperture slit bleeding rhythmic amber resonance (M3), bolted iron valve clamp jaws & arterial Marrow Bloom aura (M4), and the ossified throat opening with calcified teeth (M5).
  - Tarnished iron-and-calcified-bone recessed mechanical wall valve (`img_recessed_valve`) serving as the physical centerpiece, gripped directly by grotesque 5-fingered anatomical **Suture-Hands** (emaciated palms with exposed dorsal tendons emerging from bruised masonry cavities, jagged wrist incisions held by crooked black-clot X-sutures and loose threads, scarred opposable thumbs, asynchronously twitching bony fingers with dark-stained hooked nails, plus intertwined **Grafted Choir-Tendrils**).
  - Wet-lab **Organism Specimen Dossier Cards** (`SPEC-01..04`) with framed `64dp` observation viewports (corner registration reticles, painterly `img_suture_hand` / `img_leech_clerk` portraits, an anatomical bell-vessel `Weeping Lamp` Canvas portrait, and a rusted submersible `Sump-Siphon` Canvas portrait unlocked at Excision II), live physiological status badges (`DORMANT`, `PRESSING VALVE`, `GRAZING GLASS`, `IDLE 3s TO WEEP`, `WEEPING ×2`, `SIPHONING SUMP`), live effective per-unit & colony telemetry, and silent affordability creep bars.
  - Detailed shaded anatomical Canvas art for interactive wall anomalies (**Pale Tooth**, **Blinking Glass Eye**, **Swollen Nodule**, **Split Chitin Rib**) and living chamber organisms: 7-segment undulating **Leech-Clerks** with pulsing anterior suction mouths grazing the recessed counter glass plate, and suspended pear-vessel **Weeping Lamps** on braided cords that glow sickly lichen and weep strictly downward-falling droplets during Stillness.
  - **Arc 2 Haunted Investigator Desk (`InvestigatorDeskCanvas`):** Shifts to a rain-streaked nocturnal slate/tungsten case desk haunted by Arc 1—wet tile grout lines bleed through the leather blotter scaled by **Complicity**, the tungsten desk lamp weeps downward grey condensation during Late-Night Silence, corner glass echoes real Arc 1 Scar Tissue counts & claw scratches, and **File #00** peels back the desk corner to expose damp masonry beneath.
- **Beyond Buttons — Living Surfaces, Creatures & Objects:** Clicking begins on the recessed valve, then expands directly onto the chamber wall, counter monolith, Chamber Rites apparatus, and eventually the haunted case desk:
  - Pulling **Pale Teeth** from hairline plaster cracks.
  - Wiping grey condensation off a **Blinking Glass Eye** embedded in masonry.
  - Prodding **Swollen Nodules** pulsing along bruised fissures.
  - Prying **Split Chitin Ribs** (`PRY`) forcing their way through Phase 4 masonry seams.
  - Sealing/venting the **Floor Drain Grate**, routing **Neighbor Aperture Echoes**, engaging the **Valve Clamp** for 4-anomaly **Marrow Blooms**, and feeding colonies to **The Ossified Throat**.
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
3. **Hopelessness**
   - A creeping realization that the climb has no summit. Every **Excision** only cuts the overgrowth back down to a colder, scarred chamber where the cycle repeats faster—and even breaching the wall only leads to a desk where the investigator signs their own intake form.

> **Tone Guardrail:** Explore these emotions through surrealism, cosmic alienation, melancholic philosophy, and strange metaphor. Never depict, encourage, or trivialize real-world self-harm.

---

## 4. Escalation Curve, 6-Excision Arc 1 Progression & Arc 2–3 Architecture

### 4.1 First-Cycle Pacing (Cycle 0: Start → Excision I, ~15 Minutes)

Cycle 0 is balanced for **~15 minutes with moderate activity** from the first press to Excision I:

| Phase | Working Name | Cycle Ichor Threshold | Target Window | Mechanics & Chamber Transformation |
| :--- | :--- | :--- | :--- | :--- |
| **0** | *The Counting Room* | `0` | `0:00 – 1:00` | Cold sterile slate tiled wall, recessed iron-and-bone valve (`+1` Ichor), silent ledger. **Suture-Hands** unfold at `25` Ichor (cost `25`, `+0.6/s`) and visibly reach onto the valve rim. |
| **1** | *Uncanny Drift* | `75` | `1:00 – 4:15` | Cracked damp tiled wall (`img_wall_phase1`) surfaces with corner seepage & fractures. **Pale Teeth** protrude from plaster seams (`+25` Ichor, `+0.5` tap yield). **Leech-Clerks** unlock (cost `180`, `+2.4/s`) and cling to the counter glass. |
| **2** | *Apparatus & Flesh* | `1,600` | `4:15 – 8:30` | Sickly olive tint, condensation streaks & pulsing veins. **Weeping Lamps** unlock at `1,000` (`1,190` Ichor, `+6/s`) and hang overhead; pausing taps for 3s triggers **Stillness** harvesting (`+0.45/s` & `2×` lamp Ichor). **Blinking Glass Eye** fogs over (wipe for 12s `1.6×` gaze boost). **Specimen Jars** unlock (`24 / 65 / 140` Stillness). |
| **3** | *Collapse of Meaning* | `16,000` | `8:30 – 12:45` | Tile grid warps and buckles inward around bruised tissue pockets. **Swollen Nodules** (`+220` Ichor, `+6` Stillness). **Grafting** unlocks: fuse 2 Suture-Hands + 2 Leech-Clerks into 1 **Grafted Choir-Tendril** (`+18/s` Ichor, `+0.7/s` Stillness). |
| **4** | *The Endless Hollow* | `85,000` | `12:45 – 15:15` | Chitinous rib arches encroach inward over deep arterial fissures. Interactive **Split Chitin Ribs** (`PRY`: `+1,400` Ichor, `+14` Stillness, `6s` `1.6×` Gaze without breaking Stillness) emerge through the seams alongside mid-phase narrative beats (`145,000`). At **`210,000` Cycle Ichor**, **Excision I** unlocks. |

### 4.2 Arc 1 — Six Distinct Excision Milestones (`M1..M6`) & Branching Scar Tissue Tree

Prestiges scale steeply enough that reaching each new milestone requires permanent **Scar Tissue** upgrades and the new chamber feature unlocked by the previous milestone. Repeat harvest runs compress toward **5–7 minutes** through Suture/Stillness upgrades, while pushing a fresh milestone extends runs to **10–15 minutes**.

- **Passive Scar Resonance:** `ichor = (1 + highestMilestone × 0.25 + lifetimeScar × 0.15) × FIRST_INCISION(1.5×) × BONE_RESONANCE(4.0×)`; `stillness = (1 + highestMilestone × 0.20 + lifetimeScar × 0.10) × FIRST_INCISION(1.25×) × SHORTER_SILENCE(1.6×)`.
- **10-Node Branching Scar Tissue Surgical Skill Tree (unlocked at `M1`):**
  - **Root:** `FIRST_INCISION` (1 Scar — `×1.5` Ichor, `×1.25` Stillness).
  - **Suture Branch (run compression):** `CALLOUSED_PALM` (2 Scar — start runs with 3 Suture-Hands + 1 Leech-Clerk, `×2.2` manual yield), `TWIN_STITCH` (5 Scar — `×2.5` Suture/Leech yield, valve taps echo `+3s` passive Ichor), `COLONY_INERTIA` (12 Scar — `×3.2` all organisms, `−0.05` cost growth exponent).
  - **Stillness Branch (depressive idle depth):** `SHORTER_SILENCE` (2 Scar — Stillness triggers in `1.5s`, `×1.6` Stillness), `WAX_SEALED_MEMORY` (5 Scar — retain bottled Specimen Jars across Excision resets, `×1.6` Jar potency), `DEEP_WEEPING` (12 Scar — Weeping Lamps & Choir-Lungs `×3.5` Ichor; Lamps drip `40%` Stillness even while tapping).
  - **Marrow Branch (wall mutations & rites):** `SOFTENED_MORTAR` (2 Scar — `2×` anomaly spawn speed, cap `4`, `×3` rewards), `AUTO_TENDRIL_SUTURE` (6 Scar — auto-graft Choir-Tendrils when 4+ Hands & 4+ Leeches exist in Phase 3+, `×3` Tendrils & Siphons), `BONE_RESONANCE` (14 Scar — `×4.0` global Ichor & amplifies Excision II–V rites & `×3.5` Marrow Bloom surge).

| Milestone | Cycle Ichor Goal | First-Clear Bonus | Distinct Feature Unlocked |
| :--- | :--- | :--- | :--- |
| **Excision I** (*First Severance*) | `210,000` | `+2` Scar | Unlocks the **SCARS** tab & 3-branch Scar Tissue Surgical Skill Tree. |
| **Excision II** (*The Sunken Drain*) | `1,200,000` | `+5` Scar | Unlocks **RITES** tab: **Floor Drain Grate** Seal/Vent (`+6.5%/s` pressure → Dredge 3 water-damaged Relics: *Waterlogged Wristband*, *Brass Ward Key*, *Bled-Ink Logbook*) + 4th Organism **`SUMP_SIPHON`** (`SPEC-04`). |
| **Excision III** (*The Unbricked Seam*) | `8,500,000` | `+9` Scar | Unlocks **Neighbor Aperture Echo Routing** (`0% / 25% / 50%` diversion → `12s` **Resonance Echo** bursts + amplified anomalies). |
| **Excision IV** (*The Breath-Hold*) | `60,000,000` | `+15` Scar | Unlocks **Valve Clamp** (`20s` manual lock → 4-anomaly **Marrow Bloom** + `2.5×`/`3.5×` surge) & **Choir-Lung Grafting** (`2 Weeping Lamps + 1 Tendril` → `+220/s` Ichor, `+8/s` Stillness). |
| **Excision V** (*The Membrane Audit*) | `450,000,000` | `+24` Scar | Unlocks **The Ossified Throat** & 4-line Redacted Cult Dossier (*Rite of the Quiet Vessel* — feed colonies to permanently unseal compounding multipliers up to `~55.9×`). |
| **Excision VI** (*The Final Severance*) | `3,500,000,000` | `+40` Scar | Unlocks **The Breach Ritual** → crosses out of the chamber into **Arc 2 (*The Inquiry*)**. |

### 4.3 Arc 2 — *The Inquiry* (Haunted Investigator Desk) & *File #00* Twist

- **Phase 5 (*The Inquiry*):** Replaces the chamber UI with a rain-streaked slate/tungsten **Investigator Desk** (`InvestigatorScreen`) while keeping the shared pinned **Story Ledger** at the bottom.
- **Mechanics:** **Stamp Case File** (`Leads`), **Late-Night Silence** (`Silence` after `2.5s` idle), 4 **Desk Anomalies** (`evidence_envelope`, `fogged_spectacles`, `red_string`, `reel_tape`), 4 **Contacts** (*Night Archivist*, *Bribed Orderly*, *Microfilm Reader*, *Reel Transcriber*), 3 **Deductions** (*Hydro-Ward Invoices*, *Caretaker Roster*, *Mortar Sample Match*), and 4 **Red-String Chapters** (*Ch. I: Missing Caretakers*, *Ch. II: The Hydro-Ward Cult*, *Ch. III: Brick-Work Blueprint*, *Ch. IV: File #00 — Subject Intake*).
- **Haunting & Twist:** As **Complicity** (`0.05..1.00`) climbs, Arc 1 bleeds visually into the desk. Completing Chapter 4 opens **File #00**, revealing the Investigator's own left-thumb seam scar and intake signature—proving they are the subject who never left the chamber—and unlocking **Phase 6: Arc 3 (*The Convergence*)**, currently preserved as a clean open threshold screen allowing return to Arc 1 or Arc 2.

### 4.4 Clinical Dev Console (Testing & QA Tool)

- Accessible via the discrete `[DEV]` header pill across all arcs on both Android and Web (`OrganDevEngine` / `DevToolsModal` / `#dev-modal`).
- Allows arbitrary Arc switching (`ARC 1..3`), unlocking any Excision Milestone (`M0..M6`), unlocking/clearing all 10 Scar Nodes, assigning exact or preset Arc 1 (`Ichor`, `Stillness`, `Scar`) & Arc 2 (`Leads`, `Silence`, `CH0..4`) values, spawning Wall/Desk anomalies, fast-forwarding `+60s`, and wiping local save data (`SharedPreferences` / `localStorage`).

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

1. **Theme Fit:** Does this feature feel surreal, grotesque, weird, or otherworldly rather than generic?
2. **Emotional Resonance:** Does it touch existential crisis, depression, or hopelessness—either directly or by making an initially mundane reward feel hollow?
3. **Escalation Hook:** Does its presentation, behavior, or chamber presence intensify across Phases 0–4, Excisions I–VI, and into Arc 2?
4. **Dialogue Separation:** Are all new narrative fragments stored in `app/src/main/assets/dialogues.json` and bound to progression triggers?
5. **Cohesion:** Does it preserve the uncluttered, atmospheric focus of the mobile experience?
