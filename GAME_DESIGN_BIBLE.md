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
- **Handcrafted Surreal Graphic Design & Five-Stage Photographic Phase Wall Mutations:**
  - Softened outer radial auras and subtle volumetric organic overlays keep the chamber grounded and atmospheric (zero flat wireframe grids, diagonal red lines, or CAD rings) while the **chamber wall itself transforms dramatically across 5 photographic biomechanical layers**: cold sterile fluorescent slate (Phase 0) → exposed cracked damp tile with corner seepage (`img_wall_phase1`, Phase 1) → lichen-stained damp tiles splitting with creeping fibrous veins (`img_wall_fleshy`, Phase 2) → buckling masonry heaving apart over glistening dark muscular fascia & sinew with subtle living breath scale (`img_wall_phase3`, Phase 3) → encroaching calcified rib arches over a deep arterial hollow pulsing with a Phase 4 systolic/diastolic cardiac double-beat (`img_wall_phase4`, Phase 4) → progressive **Late-Excision surgical wall scars, bone-thread sutures, riveted iron plates & ossified membranes** (`img_wall_excision_late`) scaling in across Excisions III–VI. Volumetric shadowed arterial root-cords emerge from under the valve socket in Phases 2–4, and recessed stapled keloid flank clefts mark permanent Scar Tissue.
  - **Excision II–V Architectural Chamber Mutations** layered physically onto the wall canvas as milestones unlock: a heavy recessed cast-iron & mortared **floor sump basin** with corner hex rivets, dark runoff pool, and bubbling pressurized ichor meniscus (M2), a jagged chiseled masonry **Neighbor Aperture breach** at upper-right emitting concentric amber echo ripples on every pulse (M3), arterial Marrow Bloom tension glow (M4), and stretched translucent calcified cartilage & tendon corner webs for **The Ossified Throat** (M5).
  - Tarnished iron-and-calcified-bone recessed mechanical wall valve (`img_recessed_valve`) seated inside a heavy 8-bolt cast-iron mounting flange and bruised wet tissue socket collar: manual taps ratchet the wheel clockwise (`+24°`), while grotesque 5-fingered anatomical **Suture-Hands** continuously turn it clockwise at a speed scaling with colony count (capped at a visual ceiling) while keeping the center tap label upright, intertwined with sculpted 9-segment muscular **Grafted Choir-Tendrils** (with helical pale bone-sinew & hooked bony clasps) and visibly breathing translucent ribbed pleural **Grafted Choir-Lungs** (M4+) along the outer diagonals.
  - Wet-lab **Organism Specimen Dossier Cards** (`SPEC-01..04`) with framed `64dp` observation viewports, **framed `44dp` macro specimen photographs** (`img_jar_yesterdays_dust`, `img_jar_unspent_breath`, `img_jar_almost_a_name`, `img_jar_salt_from_the_grout`, `img_jar_a_borrowed_pulse`, `img_jar_the_uncounted_hour`) on every Specimen Jar row, procedural clinical-anatomical medallions on Graft actions, Chamber Rites, and **tappable inspectable Dredged Relic** chips, live physiological status badges, live effective per-unit & colony telemetry, and silent affordability creep bars.
  - **Twin Recessed Cast-Iron Chamber Specimen Shelves:** Bottled Specimen Jars physically accumulate along the damp lower chamber baseboards flanking the floor sump (`pointer-events: none`, seated behind HUD and wall anomalies so clicks are never blocked) — the **left shelf** holds the 3 foundational jars (*Yesterday's Dust*, *Unspent Breath*, *Almost a Name*, which remain standing in an emptied room after Excision once *Catatonic Tide* is sutured), while the **right shelf** holds the 3 deeper seals (*Salt from the Grout*, a subtly pulsing *A Borrowed Pulse*, and a taller *The Uncounted Hour*). Each jar feathers organically into the wet masonry via radial edge masking and catches a pale lichen sheen during Stillness; shelves stay hidden until at least one jar for that flank is bottled.
  - **Photographic Macro Wall Anomalies & Living Organisms:** Interactive wall anomalies (**Pale Tooth**, **Blinking Glass Eye**, **Swollen Nodule**, **Split Chitin Rib**) render as borderless circular macro photographs (`img_anomaly_pale_tooth`, `img_anomaly_glass_eye`, `img_anomaly_swollen_nodule`, `img_anomaly_chitin_rib`) radially feathered seamlessly into the damp chamber wall with subtle out-of-phase living-wall breathing and arterial pulsing motion (`wall-anomaly-breathe` / `wall-anomaly-pulse` CSS keyframes on Web, mirrored via `computeWallAnomalyBreathScale` on Android) — eliminating rigid square icon boxes — alongside living chamber organisms: 7-segment undulating **Leech-Clerks** actively patrolling the perimeter track around the counter glass bezel, and suspended pear-vessel **Weeping Lamps** hanging on staggered left/right ceiling flanks **behind** the foreground counter/income HUD (zero center lamp so the income readout and digits stay unobstructed) that glow sickly lichen and weep strictly downward-falling droplets during Stillness.
  - **Grafting inside `ORGANISMS`, Sacrifice Animation & Cooldowns:** Both **Grafted Choir-Tendril** (Phase 3+) and **Grafted Choir-Lung** (M4+) live directly inside the `ORGANISMS` tab alongside living colonies so players never have to switch tabs to graft, while `JARS` stays dedicated strictly to unlocked Specimen Jars. Performing Choir-Tendril or Choir-Lung grafting plays a brief (~1.4s) creature dissolution/stitching animation on the chamber canvas and starts a visible cooldown bar (`6s` Tendril / `10s` Lung, halved by *Wax-Sealed Memory*).
  - **Dual-Lane Side-Margin Dripping Transmissions (Queued Slow Story + Chamber Suppression + 30% Repeat Chance):** Newly unlocked story fragments seep slowly down the **right margin** (`~16s`) as a priority condensation slip while pulsing the pinned bottom ledger border, and are strictly **queued (FIFO)** with a brief `300ms` bead-forming breather between consecutive slips so a new story beat never dismisses or interrupts an ongoing drip. Wall anomalies, Neighbor Aperture echoes, and graft sacrifices emit brisk thematic notes down the **subordinate left margin** (`~4.8s`), and are **completely hidden and suppressed whenever a story drip is active or queued** so nothing ever competes with narrative beats. Each distinct wall/chamber event key always shows its dripping text the first time it is seen unsuppressed (`100%`), then triggers at a **30% random chance** on subsequent repeats so recurring events stay sparse and uncanny rather than repetitive.
  - **Diegetic Naming, Restored Threshold Progress & Deep-Red Tab Pulses:** Player-facing UI uses in-world names exclusively—never clinical meta-jargon like `"Phase"`, `P0..P4`, `"Milestone"`, `M1/6..M5`, `EXCISION I..VI`, `ARC 1..3`, `CH 0/4`, `PREREQ LOCKED`, or `RANK`. Excisions display their own names directly (*First Severance*, *The Sunken Drain*, *The Unbricked Seam*, *The Breath-Hold*, *The Membrane Audit*, *Plead for Salvation*), Chamber Rites omit `// M2..M5`, Scar nodes name their required incision (`REQUIRES: FIRST INCISION`), and Investigation threads omit chapter counters. The top header shows the current room/stage name, `[SFX]`/`[MUTE]`, `[DEV]`, a thin progress bar, and a compact diegetic readout tracking progress toward the next room stage (`NEXT: UNCANNY DRIFT · 0 / 75`) or next Excision milestone (`NEXT: THE SUNKEN DRAIN · ...`)—switching to `SEAM READY // <NAME>` or `SEAM UNSEALED // PLEAD FOR SALVATION` when the seam is ready—while locked jars, locked layered grafts, Cycle 0 Excision, and future Inquiry threads stay hidden until reached. Inactive deck tabs containing newly unlocked unseen content or ready actions pulse with an **opaque dried-clot red glow** (`#341315` ↔ `#611E22`); **SCARS** and **EXCISION** silence immediately after the first click even if the player saves unspent Scars or stays on-run to farm, re-pulsing only when fresh Scars are harvested, a new milestone is crossed, or a new cycle reaches Excision. Cycle, Last Severance, Permanent Resonance, and a **Next Scar Tissue visual progress bar** (inverted from the smooth power curve `floor(2.0 × (cycleLifetimeIchor / 210,000)^0.30 × hookMultiplier)`) live inside the Scars and Excision tabs.
  - **Theme Over Descriptiveness:** All upgrade, organism, jar, graft, scar node, rite, relic, anomaly, and deduction descriptions favor brief, cryptic, atmospheric copy over mechanical walkthroughs while preserving clear purchase costs, owned counts, and live `/s` readouts.
  - **Arc 2 Slow-Boil Investigator Desk (`InvestigatorDeskCanvas`):** Opens as a cold, rain-streaked nocturnal municipal case desk (**Cases I–III strictly quarantined from grotesque hospital-room imagery**): pulling the brass lamp chain flips between warm amber **Lamp On** (full Daylight Leads) and moonlit **Lamp Off** (After-Hours Silence, second-chair shadow in Cases II–IV). Only in **Case IV (Lamp Off)** do faint blotter tile seams surface, escalating in **Case V → File #00** to persistent hospital tile grout, grey oil weep, corner scratches, and the blotter peeling back from wet masonry.
- **Beyond Buttons — Living Surfaces, Creatures & Objects:** Clicking begins on the recessed valve, then expands directly onto the chamber wall, counter monolith, Chamber Rites apparatus, and eventually the investigator's case desk:
  - Pulling **Pale Teeth** from hairline plaster cracks.
  - Wiping grey condensation off a **Blinking Glass Eye** embedded in masonry.
  - Prodding **Swollen Nodules** pulsing along bruised fissures.
  - Prying **Split Chitin Ribs** (`PRY`) forcing their way through Phase 4 masonry seams.
  - Sealing/venting the **Floor Drain Grate** and tapping **Dredged Relics** to inspect their thematic notes, routing **Neighbor Aperture Echoes**, engaging the **Valve Clamp** for 4-anomaly **Marrow Blooms**, and feeding colonies to **The Ossified Throat**.
  - Pulling the brass **Desk Lamp Chain**, slitting **Evidence Envelopes** (Case I+), wiping **Fogged Spectacles** (Case II+), playing **Reel-to-Reel Tapes** (Case III+), bagging **Enamel Exhibits** (Case IV+ only), and stringing **Exhibit Polaroids** across the interactive **Red-String Corkboard**.
- **Tactile Weight:** Haptics, springs, and animations feel heavy, reluctant, breathing, or unnervingly alive.
- **Procedural Cast-Iron & Visceral Organic Soundscape (Android + Web Parity):** Zero-asset physical-modeling foley and phase-escalating subterranean ambience (`SoundCueResolver` + `ProceduralFoleySynth` on Android; Web Audio `audio.js` on Web) grounded inside a damp tiled room (`19ms` / `35ms` damped early slapback reflections, zero generic synth clicks or tab-switch sounds): a **deep bolted cast-iron-in-masonry valve thump** (`108→36Hz` sub-bass chest thump + `152→62Hz` wall-housing body punch + low-passed impact mass layered over a darkened `154Hz` 6-mode cast-iron under-ring with restrained clang transients, trailing wet seepage in Phase 1+) alongside full-clang cast-iron modal banks for the floor sump grate, Valve Clamp, and Excision shears; **stochastic fibrous tendon/sinew tearing** through dual sweeping formant resonators (`320–780Hz` cavity + `1150–2350Hz` fascia rasp + stretched ligament flutter) and **LFO-swept crackle-injected wet mucosa squelches** + irregular cavity bubble clusters for wall anomalies, scar suturing, Throat feedings, a long (`~1.15s`) dual-wave visceral grafting ritual, and four distinct per-organism signatures (*Suture-Hand* tearing fascia + iron grip, *Leech-Clerk* viscous suction drag & bubbles, *Weeping Lamp* dark hollow glass toll + heavy drops, *Sump-Siphon* submerged bellows gulp + iron seat clank). Room ambience escalates subtly across Phases 0–4 (muted 50Hz ballast hum → low subterranean cistern drips → 36Hz peristaltic rumble → Phase 4 44Hz cardiac double-beat) and shifts in Arc 2 to muffled cold-glass rain with Slow-Boil-quarantined Room bleed (zero flesh/cardiac bleed in Cases I–III) plus crisp brass lamp pull-chain, taut crimson twine pluck, Reel 4 capstan scrub, and chemical solvent dissolve foley. Controlled by a compact top-bar `[SFX]`/`[MUTE]` toggle (unmuted by default, persisted locally independently of save wipes, and auto-suspended when backgrounded).

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
| **2** | *Apparatus & Flesh* | `1,600` | `4:15 – 8:30` | Sickly olive tint, frontal lichen-and-vein tiled wall texture (`img_wall_fleshy`), sliding damp condensation glints, and volumetric arterial root-cords emerging under the bolted valve flange. **Weeping Lamps** unlock at `1,000` (`1,190` Ichor, `+6/s`) and hang on staggered ceiling flanks behind the counter HUD; pausing taps for 3s triggers **Stillness** harvesting (`+0.45/s` & `2×` lamp Ichor). **Blinking Glass Eye** fogs over (wipe for 12s `1.6×` gaze boost). Foundational **Specimen Jars** unlock (`24 / 65 / 140` Stillness). |
| **3** | *Collapse of Meaning* | `16,000` | `8:30 – 12:45` | Buckling tiles splitting apart over glistening dark muscular fascia & sinew (`img_wall_phase3`) with subtle breathing expansion and flank subsurface swells. **Swollen Nodules** (`+220` Ichor, `+6` Stillness). **Salt from the Grout** Jar (`320` Stillness) unlocks. **Grafting** unlocks: fuse 2 Suture-Hands + 2 Leech-Clerks + scaling Stillness (`35 × 1.55^n`, `6s` cooldown) into 1 sculpted **Grafted Choir-Tendril** (`+18/s` Ichor, `+0.7/s` Stillness). |
| **4** | *The Endless Hollow* | `85,000` | `12:45 – 15:15` | Calcified rib arches & deep arterial hollow (`img_wall_phase4`) breathing with a cardiac double-beat pulse wave radiating from the valve (and overlaid by surgical bone-thread scar plates `img_wall_excision_late` across Excisions III–VI). **A Borrowed Pulse** Jar (`750` Stillness) unlocks. Interactive **Split Chitin Ribs** (`PRY`: `+1,400` Ichor, `+14` Stillness, `6s` `1.6×` Gaze without breaking Stillness) emerge through the seams alongside mid-phase narrative beats (`145,000`). At **`210,000` Cycle Ichor**, **Excision I** unlocks (and post-Severance **The Uncounted Hour** Jar at `1,800` Stillness). |

### 4.2 Arc 1 — Six Sequential Excision Milestones (`M1..M6`), Smooth Scar Harvest & 5-Tier Surgical Scar Tree

Prestiges are tuned around a deliberate **2–3 runs per milestone** rhythm from `M2` through `M6` (1–2 brisk Scar-farming runs to invest in the Surgical Scar Tree & Repeatable Grafts, followed by a milestone push run using the latest chamber mechanics). **Hard Anti-Skip Sequential Cap:** every Excision advances `highestExcisionMilestone` by at most **`+1`** regardless of excess cycle Ichor (and **Excision VI // Plead for Salvation** into Arc 2 strictly requires `M5` already cleared plus `160,000,000,000` Cycle Ichor).

- **Smooth Diminishing-Returns Scar Harvest (Milestone-Clamped Next Scar, First Severance +1 Bonus):** Excision I (*First Severance*) awards a one-time `+1` first-clear bonus Scar (`2 + 1 = 3` Scars at `210,000` Cycle Ichor so the player can immediately suture `FIRST_INCISION` plus any Tier-1 branch starter); all subsequent milestones `M2..M6` and Breach have `0` bonus Scars. Every Excision at `>= 210,000` Cycle Ichor yields `max(2, floor(2.0 × (cycleLifetimeIchor / 210,000)^0.30 × (1 + 0.12 × scarHarvestHookRank)))` (+ `1` on first clear of `M1`), and whenever the next Scar along the curve would sit beyond the active Excision milestone threshold, that Scar is pulled down to the milestone threshold itself so "Next Scar Tissue" never overshoots the current milestone and reaching the milestone grants that Scar early without shifting later Scars.
- **Root-Softcapped Passive Scar Resonance:** `ichor = (1 + highestMilestone × 0.20 + sqrt(lifetimeScar) × 0.25) × FIRST_INCISION(1.45×) × BONE_RESONANCE(2.10×) × KELOID(1 + 0.18 × rank)`; `stillness = (1 + highestMilestone × 0.15 + sqrt(lifetimeScar) × 0.18) × FIRST_INCISION(1.20×) × SHORTER_SILENCE(1.50×) × SEDIMENT(1 + 0.16 × rank)`.
- **6-Tier Specimen Jars (Stillness Sink):** Foundational jars (`YESTERDAYS_DUST` 24, `UNSPENT_BREATH` 65, `ALMOST_A_NAME` 140), deeper phase jars (`SALT_FROM_THE_GROUT` 320 at P3+: `+45%` organisms; `A_BORROWED_PULSE` 750 at P4+: `+55%` graft/lung Ichor & all Stillness), and post-Severance seal (`THE_UNCOUNTED_HOUR` 1,800 at Cycle 1+ / M1+: `+75%` global Ichor & all Stillness). Deep jars always clear on Excision so late seals remain a meaningful Stillness sink every cycle, while *Catatonic Tide* preserves the 3 foundational jars once bottled.
- **16-Node 5-Tier Branching Scar Tissue Surgical Skill Tree (unlocked at `M1`):**
  - **Root:** `FIRST_INCISION` (1 Scar — `×1.45` Ichor, `×1.20` Stillness; opens all 3 branches and Repeatable Surgical Grafts).
  - **Suture Branch (T1–T5, run compression & valve automation):** `CALLOUSED_PALM` (2 Scar — start runs with 3 Suture-Hands + 1 Leech-Clerk, `×1.8` manual yield), `TWIN_STITCH` (5 Scar — `×1.6` Suture/Leech yield, valve taps echo `+1.5s` passive Ichor), `COLONY_INERTIA` (8 Scar — `×1.90` all organisms, `−0.04` cost growth exponent), `MEMBRANE_RECALL` (12 Scar — start runs with 2 Weeping Lamps, 2 Sump-Siphons once `M2+` cleared, and 1 Grafted Choir-Tendril; `×2.20` Siphons & Tendrils), `PERPETUAL_SEAM` (36 Scar — auto-ratchets valve every `0.75s` without breaking Stillness, `×2.50` organisms, Grafted Tendrils, Choir-Lungs & manual taps).
  - **Stillness Branch (T1–T5, depressive idle depth & retention):** `SHORTER_SILENCE` (1 Scar — Stillness triggers in `1.5s`, `×1.50` Stillness generation), `WAX_SEALED_MEMORY` (2 Scar — `−30%` Stillness costs across all Specimen Jars & Grafts, halves Grafting cooldowns, `+0.35` Jar potency), `DEEP_WEEPING` (3 Scar — Weeping Lamps `×2.0` Ichor, drip `50%` Stillness & `×1.25` Ichor while tapping), `HOLLOW_LUNG_CHOIR` (9 Scar — permanently retains all dredged sump Relics across Excisions; Choir-Lungs `×2.10` Ichor & Stillness, `2.0×` idle boost; Lamps `2.6×` Ichor in Stillness), `CATATONIC_TIDE` (36 Scar — Stillness triggers in `0.8s`, `×3.20` global Ichor & Stillness while Stillness is active with an unbroken `×1.75` ambient floor while tapping, `+0.65` Jar potency, and retains the 3 foundational Specimen Jars across Excisions once bottled).
  - **Marrow Branch (T1–T5, wall mutations & chamber rites):** `SOFTENED_MORTAR` (2 Scar — `1.6×` anomaly spawn speed, cap `4`, `×1.8` anomaly rewards), `AUTO_TENDRIL_SUTURE` (5 Scar — auto-graft Choir-Tendrils when 4+ Hands & 4+ Leeches & Stillness/cooldown ready in Phase 3+, `×1.65` Tendrils & Siphons), `ECHO_OSSUARY` (9 Scar — Drain pressure builds `1.45×` faster, Relic Ichor bonus `×1.35`, Neighbor Aperture Echo interval `9s` with `×1.35` yield), `BLOOM_INCISION` (16 Scar — Valve Clamp hold drops to `15s`, Marrow Bloom lasts `32s` at `3.1×` Ichor, `+50%` anomaly yield during Bloom), `BONE_RESONANCE` (40 Scar — `×2.10` global Ichor, `×1.40` Relic & Echo synergy, `×1.18` per Unsealed Dossier Clause).
- **3 Repeatable Surgical Grafts (unlocked once `FIRST_INCISION` is sutured; `floor(1 × growth^r)` starting `1, 1, 2, 3...`):**
  - `KELOID_THICKENING` (`floor(1 × 1.45^r)` Scar → `1, 1, 2, 3, 4, 6...` — `+18%` multiplicative Ichor per rank).
  - `SEDIMENT_WELL` (`floor(1 × 1.45^r)` Scar → `1, 1, 2, 3, 4, 6...` — `+16%` Stillness per rank & `0.96^r` Stillness cost multiplier floored at `0.68`).
  - `SCAR_HARVEST_HOOK` (`floor(1 × 1.55^r)` Scar → `1, 1, 2, 3, 5, 8...` — `+12%` Scar Tissue harvested per Excision per rank).

| Milestone | Cycle Ichor Goal | First-Clear Bonus | Distinct Feature Unlocked |
| :--- | :--- | :--- | :--- |
| **Excision I** (*First Severance*) | `210,000` | `+1` Scar (`3` total at `210k`) | Unlocks the **SCARS** tab, 5-tier Scar Tissue Surgical Skill Tree & Repeatable Grafts. |
| **Excision II** (*The Sunken Drain*) | `1,800,000` | `0` Scar | Unlocks **RITES** tab: **Floor Drain Grate** Seal/Vent (`+4% + 1.5%/siphon` pressure → Dredge 3 Relics: *Wristband* `1.28×`, *Brass Key* `1.35×`, *Logbook* `1.45×`) + 4th Organism **`SUMP_SIPHON`** (`SPEC-04`, `42/s`). |
| **Excision III** (*The Unbricked Seam*) | `24,000,000` | `0` Scar | Unlocks **Neighbor Aperture Echo Routing** (`0% / 25% / 50%` diversion → `12s`/`9s` **Resonance Echo** bursts `10.5×`/`18×` + amplified anomalies) and **Colony Proliferation** (`PROLIFERATE ×N` exact bulk-spawning on every organism dossier card). |
| **Excision IV** (*The Breath-Hold*) | `320,000,000` | `0` Scar | Unlocks **Valve Clamp** (`20s`/`15s` manual lock → 4-anomaly **Marrow Bloom** at `2.2×`/`3.1×` surge) & **Choir-Lung Grafting** (`2 Weeping Lamps + 1 Tendril` → `+160/s` Ichor, `+3.5/s` Stillness). |
| **Excision V** (*The Membrane Audit*) | `4,500,000,000` | `0` Scar | Unlocks **The Ossified Throat** & 4-line Redacted Cult Dossier (*Rite of the Quiet Vessel* — feed colonies to permanently unseal compounding multipliers `1.35× / 1.40× / 1.45× / 1.55×`, up to `~4.25×` base / `~8.2×` with *Bone Resonance*). |
| **Excision VI** (*Plead for Salvation*) | `160,000,000,000` | `0` Scar | Strictly requires `M5` already cleared; unlocks **Plead for Salvation** → lays an ambiguous petition (half prayer for rescue, half quiet surrender) against the last wet seam, cutting into **Arc 2 (*The Inquiry*)**. |

### 4.3 Arc 2 — *The Inquiry* (4-Act Slow-Boil Investigation) & *File #00* Twist

- **Phase 5 (*The Inquiry*, ~50–75 min Slow Boil):** Replaces the chamber UI with a rain-streaked nocturnal municipal case desk (`InvestigatorScreen`) while keeping the shared pinned **Story Ledger** at the bottom. Opens cold on **Case I · Unclaimed Arrears** with an unsigned, water-stained **plea for salvation**—strictly quarantining all grotesque Room vocabulary (`hydro-ward`, `molar`, `grout`, `valve`, `ichor`, `suture`, `caretaker`, `tile`, `vessel`, `scar`) out of Cases I–III so the realization builds from mundane municipal noir → eerie domestic haunting → uncanny self-implication → grotesque Room intrusion → **File #00**.
- **Lamp On / Lamp Off Dual-Lens (`lampOn`):** Pulling the brass lamp chain toggles between **Lamp On** (full `1.0×` Daylight Leads, `0` Silence until Case IV *Clause III* adds a `15%` trickle) and **Lamp Off** (dimmed `0.25×` Leads or `0.3875×` with *Borrowed Coat*, active **After-Hours Silence/s**, faster haunted Desk Anomalies, Acoustic Reel Scrubbing, and Redaction Solvent access).
- **5 Escalating Case Angles & Soft-Reset Pivots (`caseAngle: 1..5`):**
  1. *Case I · Unclaimed Arrears* → pins Keystone `1998 Tax Plat`.
  2. *Case II · The Second Chair* → pins Keystone `Second Chair Scuffs`, unlocks *Microfilm Reader*.
  3. *Case III · Static on Reel 4* → pins Keystone `Reel 4 Trace`, unlocks *Reel Transcriber*, **Bulk Contact Warrants (`WARRANT ×N`)**, and **Acoustic Reel Scrubbing** (`24s` Lamp-Off burst / `45s` cooldown, `×2.6` or `×3.4` Silence/s + `1.65×` anomaly spawn speed).
  4. *Case IV · Sub-Surface Survey* → pins Keystone `Closed Drainage Plat`, unlocks **4 Case IV Audit Redaction Clauses** (`clause_seepage`, `clause_shift`, `clause_quiet`, `clause_intake`, dissolved with Silence while Lamp Off) and the first grotesque Desk Anomaly (`enamel_exhibit`).
  5. *Case V · Internal Audit* → requires all 4 dissolved Redactions, unsealing **File #00** and unlocking **Phase 6: Arc 3 (*The Convergence*)**.
  - Filing a Dead End (`pivotNextCase`) wipes transient Leads & Contacts to `0`, keeps Silence, Exhibits, Wired Corkboard, Discovered Corroborations, and Redactions, pins the new Keystone Exhibit, grants `+1 Red String Spool` (`1..5`), and permanently raises Case Mastery (`+45%` Leads / `+35%` Silence per pivot).
- **Interactive Red-String Corkboard:** 10 Exhibits (5 Case Keystones + 5 Silence-unlocked Exhibits in `DOSSIER`), up to 5 Red String Spools, **10 Curated Corroboration pairs** with targeted mechanical & story payoffs, plus a `+15%` *Speculative Thread* multiplier for any non-curated wired pair.
- **4 Slow-Boil-Gated Desk Anomalies:** `evidence_envelope` (Case I+), `fogged_spectacles` (Case II+), `reel_tape` (Case III+), and `enamel_exhibit` (Case IV+ only).

### 4.4 Clinical Dev Console (Testing & QA Tool)

- Accessible via the discrete `[DEV]` header pill across all arcs on both Android and Web (`OrganDevEngine` / `DevToolsModal` / `#dev-modal`).
- Allows arbitrary Arc switching (`ARC 1..3`), unlocking any Excision Milestone (`M0..M6`), unlocking/clearing all 16 Scar Nodes (and clearing Repeatable Grafts), assigning exact or preset Arc 1 (`Ichor`, `Stillness`, `Scar`) & Arc 2 (`Leads`, `Silence`, `CASE I..V`, `FILE #00`, Lamp Toggle, All Exhibits, Reset Arc 2) values, spawning Wall/Desk anomalies, fast-forwarding `+60s`, and wiping local save data (`SharedPreferences` / `localStorage`, schema `v3` with automatic `v2 → v3` migration).

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
  - `case_angle_reached` — Arc 2 active `caseAngle` reaches `threshold` (`2..5`).
  - `case_chapter_completed` — Arc 2 `completedCasePivots` reaches `threshold` (`1..5`, with `5` triggering *File #00*).
  - `corroboration_discovered` — Specific curated Corkboard corroboration `target` is wired & discovered (`threshold: 1`).
  - `lamp_toggled` — Arc 2 desk lamp toggles (`target: "off"` counts Lamp-Off flips; otherwise total toggles) reach `threshold`.
  - `arc2_silence_reached` — Arc 2 `lifetimeSilence` reaches `threshold`.
  - `reel_scrubbed` — Arc 2 Case III+ `reelScrubCount` reaches `threshold`.
  - `redaction_dissolved` — Arc 2 Case IV+ `dissolvedRedactions.size` reaches `threshold` (`1..4`).
  - `desk_anomaly_interacted` — Arc 2 interactions with `target` (`evidence_envelope`, `fogged_spectacles`, `reel_tape`, `enamel_exhibit`) reach `threshold`.

---

## 6. Mandatory Feature Alignment Checklist

Before designing or implementing **any** new feature (upgrades, creatures, objects, prestige loops, visuals, sounds, UI screens, or dialogue triggers), verify:

1. **Theme Fit:** Does this feature feel surreal, grotesque, weird, or otherworldly rather than generic or locked-room thriller cliché?
2. **Emotional Resonance:** Does it touch existential crisis, depression, hopelessness, or quiet acceptance—either directly or by making an initially mundane reward feel hollow?
3. **Escalation Hook:** Does its presentation, behavior, or chamber presence intensify across Phases 0–4, Excisions I–VI, and into Arc 2 along the dissolving line between *leaving* and *having always been here*?
4. **Dialogue Separation:** Are all new narrative fragments stored in `app/src/main/assets/dialogues.json` and bound to progression triggers?
5. **Cohesion:** Does it preserve the uncluttered, atmospheric focus of the mobile experience?
