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
- **Beyond Buttons — Living Surfaces, Creatures & Objects:** Clicking begins on a recessed valve, then expands directly onto the chamber wall and counter monolith:
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

## 4. Escalation Curve & Prototype Arc (Start → First Excision)

The emotional weight and surreal grotesquerie **must intensify as the game progresses**:

| Phase | Working Name | Cycle Ichor Threshold | Mechanics & Chamber Transformation |
| :--- | :--- | :--- | :--- |
| **0** | *The Counting Room* | `0` | Stark tiled wall, recessed valve (`+1` Ichor), silent ledger. Early unlock of **Suture-Hands** (`15` Ichor). |
| **1** | *Uncanny Drift* | `25` | Hairline fissures widen. **Pale Teeth** protrude from plaster (tap on wall to pull). **Leech-Clerks** latch onto the counter digits. |
| **2** | *Apparatus & Flesh* | `150` | **Weeping Lamps** hang overhead; pausing taps for 3s triggers **Stillness** harvesting. A **Blinking Glass Eye** fogs over (tap to wipe for `2×` gaze boost). **Specimen Jars** unlock (*Yesterday's Dust*, *Unspent Breath*, *A Name You Almost Remember*). |
| **3** | *Collapse of Meaning* | `600` | Bruised fissures pulse with **Swollen Nodules** (tap to prod). **Grafting** unlocks: sacrifice 2 Suture-Hands + 2 Leech-Clerks to form 1 **Grafted Choir-Tendril**. |
| **4** | *The Endless Hollow* | `2,000` | Overgrowth encroaches on the chamber borders. **Excision / Molting** unlocks: sever the overgrown mass to reset into the next **Cycle**, retaining permanent **Scar Tissue** multipliers and uncovering post-reset notes. |

---

## 5. Dialogue & Narrative Storage Contract

Dialogues are patchy, ambiguous fragments and **must always be stored separately** from Kotlin UI/business logic.

- **Canonical Storage Location:** `app/src/main/assets/dialogues.json`
- **Strict Separation Rule:**
  - Do **not** hardcode dialogue lines inside Jetpack Compose screens, ViewModels, game logic classes, or `strings.xml`.
  - Keep `res/values/strings.xml` strictly for UI chrome, telemetry headings, accessibility labels, and system strings.
- **Supported Progress-Binding Trigger Types:**
  - `counter_reached` — Cycle lifetime Ichor reaches `threshold`.
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
