# Project Agent & Contributor Guidelines

> **Owner Workflow Context:** The project owner will **not** be reading the code directly. Quality, correctness, theme fidelity, and regressions must therefore be guaranteed through disciplined automated testing and mandatory post-implementation review.

Before designing, modifying, or implementing **any** new feature, UI change, mechanic, or narrative content in this repository:

1. **Read `/GAME_DESIGN_BIBLE.md` first.**
   - Every feature must align with the game's core goal, surreal/grotesque/otherworldly aesthetic, and primary emotional resonators (**existential crisis, depression, and hopelessness**).
   - Features should respect the **Escalation Curve (Phases 0–4)** so unease, weirdness, and emotional weight intensify as the player progresses.
   - Verify your design against the **Mandatory Feature Alignment Checklist** in `/GAME_DESIGN_BIBLE.md` before writing code.

2. **Keep all dialogues strictly separated and progress-bound (and follow `/skills/story-writing/SKILL.md`).**
   - Before writing or editing any story/dialogue fragments, **read `/skills/story-writing/SKILL.md`** and audit all existing entries in `/app/src/main/assets/dialogues.json` to preserve continuity and avoid contradictions.
   - Store all dialogue/narrative entries in `/app/src/main/assets/dialogues.json` (never hardcoded in Composables, ViewModels, or `strings.xml`).
   - Every dialogue entry must bind to a progression condition (such as counter threshold, phase, cycle, or milestone) and reflect the escalating tone of its phase.

3. **Cover all behavior thoroughly with automated tests (Mandatory).**
   - Every new or changed behavior must have meaningful automated test coverage before being considered done:
     - **Unit tests** for ViewModels, game math, state transitions, progression/escalation logic, and asset integrity (e.g., validating `/app/src/main/assets/dialogues.json` structure and triggers).
     - **Robolectric Compose UI tests** for Critical User Journeys (CUJs), interactive controls (`Modifier.testTag`), rendered values, and accessibility semantics (always calling `composeTestRule.waitForIdle()`).
   - Run the full local test suite (`gradle :app:testDebugUnitTest`) and verify every test passes before finishing. Never ship broken, skipped, or vacuous tests.

4. **Mandatory Post-Implementation Review by a Same-Model Subagent.**
   - Whenever a change is considered done, it **must** be reviewed by a **subagent running the exact same model as the implementer** before finalizing the response.
   - The reviewer subagent must audit:
     - Behavioral correctness, edge cases, state safety, and potential regressions.
     - Test coverage depth and whether tests actually assert meaningful user-visible behavior.
     - Alignment with `/GAME_DESIGN_BIBLE.md` and dialogue separation rules.
   - Any issues raised by the reviewer must be fixed and re-verified (re-running tests and build) before concluding.
   - **Environment Limitation Fallback:** If the active agent runtime does not provide a tool to spawn a separate subagent, the agent must (a) explicitly state this limitation to the user rather than claiming a subagent ran, and (b) perform a strict, independent checklist audit of every changed file, test, and edge case before reporting completion.

5. **Keep documentation in sync.**
   - If a major new game system, progression phase, or dialogue trigger type is introduced, update `/GAME_DESIGN_BIBLE.md` so it remains the single source of truth.
