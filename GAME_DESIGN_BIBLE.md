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
  - Tarnished iron-and-calcified-bone recessed mechanical wall valve (`img_recessed_valve`) serving as the physical centerpiece, gripped directly by shaded anatomical **Suture-Hands** (curved metacarpal silhouettes, dark surgical wrist sutures with dried-clot punctures, articulated fingers with pale nails, and intertwined **Grafted Choir-Tendrils**).
  - Wet-lab **Organism Specimen Dossier Cards** (`SPEC-01..03`) with framed `64dp` observation viewports (corner registration reticles, painterly `img_suture_hand` / `img_leech_clerk` portraits, and an anatomical bell-vessel `Weeping Lamp` Canvas portrait), live physiological status badges (`DORMANT`, `PRESSING VALVE`, `GRAZING GLASS`, `IDLE 3s TO WEEP`, `WEEPING ×2`), live effective per-unit & colony telemetry, and silent affordability creep bars.
  - Detailed shaded anatomical Canvas art for interactive wall anomalies (**Pale Tooth**, **Blinking Glass Eye**, **Swollen Nodule**) and living chamber organisms: 7-segment undulating **Leech-Clerks** with pulsing anterior suction mouths grazing the recessed counter glass plate, and suspended pear-vessel **Weeping Lamps** on braided cords that glow sickly lichen and weep falling droplets during Stillness.
- **Beyond Buttons — Living Surfaces, Creatures & Objects:** Clicking begins on the recessed valve, then expands directly onto the chamber wall and counter monolith:
  - Pulling **Pale Teeth** from hairline plaster cracks.
  - Wiping grey condensation off a **Blinking Glass Eye** embedded in masonry.
  - Prodding **Swollen Nodules** pulsing along bruised fissures.
  - Watching **Suture-Hands**, **Leech-Clerks**, **Weeping Lamps**, and **Grafted Choir-Tendrils** physically occupy and alter the chamber.
- **Tactile Weight:** Haptics, springs, and animations feel heavy, reluctant, breathing, or unnervingly alive.

### What to Avoid
- Generic "happy mobile clicker" polish (rainbow coins, cheerful badges, upbeat motivational copy).
- Cliché cheap horror (loud jumpscares, cartoonish gore). The horror here is **weird, surreal, melancholic, and existential**.
- Static stagnation: the chamber must visibly *mutate* across phases and remember previous resets through **Scar Tissue**.

---

## 3. Core Emotional Resonators

Three primary emotional threads anchor the entire experience:

1. **Existential Crisis**
   - Questioning what the wall contains, who authored the contradictory notes, and whether the player is tending the organ or being slowly absorbed by it.
2. **Depression**
   - Emotional heaviness, exhaustion, numbness, and quiet inertia—expressed mechanically through **Weeping Lamps** and **Stillness**, which drip only when the player stops pressing and sits with the dim screen.
3. **Hopelessness**
   - A creeping realization that the climb has no summit. Even the climax—**Excision / Molting**—only cuts the overgrowth back down to a colder, scarred chamber where the cycle repeats faster.

> **Tone Guardrail:** Explore these emotions through surrealism, cosmic alienation, melancholic philosophy, and strange metaphor. Never depict, encourage, or trivialize real-world self-harm.

---

## 4. Escalation Curve & ~15-Minute First-Cycle Pacing (Start → First Excision)

Cycle 0 is balanced for **~15 minutes with moderate activity** from the first press to First Excision:

| Phase | Working Name | Cycle Ichor Threshold | Target Window | Mechanics & Chamber Transformation |
| :--- | :--- | :--- | :--- | :--- |
| **0** | *The Counting Room* | `0` | `0:00 – 1:00` | Cold sterile slate tiled wall, recessed iron-and-bone valve (`+1` Ichor), silent ledger. **Suture-Hands** unfold at `25` Ichor (cost `25`, `+0.6/s`) and visibly reach onto the valve rim. |
| **1** | *Uncanny Drift* | `75` | `1:00 – 4:15` | Cracked damp tiled wall (`img_wall_phase1`) surfaces with corner seepage & fractures. **Pale Teeth** protrude from plaster seams (`+25` Ichor, `+0.5` tap yield). **Leech-Clerks** unlock (cost `180`, `+2.4/s`) and cling to the counter glass. |
| **2** | *Apparatus & Flesh* | `1,600` | `4:15 – 8:30` | Sickly olive tint, condensation streaks & pulsing veins. **Weeping Lamps** unlock at `1,000` (`1,190` Ichor, `+6/s`) and hang overhead; pausing taps for 3s triggers **Stillness** harvesting (`+0.45/s` & `2×` lamp Ichor). **Blinking Glass Eye** fogs over (wipe for 12s `1.6×` gaze boost). **Specimen Jars** unlock (`24 / 65 / 140` Stillness). |
| **3** | *Collapse of Meaning* | `16,000` | `8:30 – 12:45` | Tile grid warps and buckles inward around bruised tissue pockets. **Swollen Nodules** (`+220` Ichor, `+6` Stillness). **Grafting** unlocks: fuse 2 Suture-Hands + 2 Leech-Clerks into 1 **Grafted Choir-Tendril** (`+18/s` Ichor, `+0.7/s` Stillness). |
| **4** | *The Endless Hollow* | `85,000` | `12:45 – 15:15` | Chitinous rib arches encroach inward over deep arterial fissures. At **`210,000` Cycle Ichor**, **Excision / Molting** unlocks: sever the mass to enter **Cycle 01**, keeping permanent **Scar Tissue** (`+75%` Ichor & `+25%` Stillness passive multiplier per unit) and unlocking Cycle 1+ narrative fragments. |

---

## 5. Dialogue & Narrative Storage Contract

Dialogues are patchy, ambiguous fragments and **must always be stored separately** from Kotlin UI/business logic. Follow `/skills/story-writing/SKILL.md` whenever authoring or editing fragments so new entries build coherently on earlier fragments without contradictions.

- **Canonical Storage Location:** `app/src/main/assets/dialogues.json` (mirrored byte-for-byte to `docs/assets/dialogues.json` for the GitHub Pages playable web version and verified by automated parity tests).
- **Strict Separation Rule:**
  - Do **not** hardcode dialogue lines inside Jetpack Compose screens, ViewModels, game logic classes, `strings.xml`, or web HTML/JS modules.
  - Keep `res/values/strings.xml` (and clinical web UI chrome) strictly for UI telemetry headings, accessibility labels, and system strings.
- **Supported Progress-Binding Trigger Types:**
  - `counter_reached` — Cycle lifetime Ichor reaches `threshold`.
  - `cycle_counter_reached` — Player is in Cycle `>= target` AND Cycle lifetime Ichor reaches `threshold` (post-prestige story beats).
  - `creature_owned` — Owned count of `target` (`suture_hand`, `leech_clerk`, `weeping_lamp`, `grafted_tendril`) reaches `threshold`.
  - `anomaly_interacted` — Lifetime interactions with `target` (`pale_tooth`, `glass_eye`, `swollen_nodule`) reach `threshold`.
  - `stillness_reached` — Accumulated Stillness reaches `threshold`.
  - `jar_bottled` — Number of Specimen Jars bottled reaches `threshold`.
  - `excision_completed` — Completed Excision/Molt cycles reach `threshold`.

---

## 6. Mandatory Feature Alignment Checklist

Before designing or implementing **any** new feature (upgrades, creatures, objects, prestige loops, visuals, sounds, UI screens, or dialogue triggers), verify:

1. **Theme Fit:** Does this feature feel surreal, grotesque, weird, or otherworldly rather than generic?
2. **Emotional Resonance:** Does it touch existential crisis, depression, or hopelessness—either directly or by making an initially mundane reward feel hollow?
3. **Escalation Hook:** Does its presentation, behavior, or chamber presence intensify across Phases 0–4 and across Cycles?
4. **Dialogue Separation:** Are all new narrative fragments stored in `app/src/main/assets/dialogues.json` and bound to progression triggers?
5. **Cohesion:** Does it preserve the uncluttered, atmospheric focus of the mobile experience?
