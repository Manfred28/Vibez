# Tap Counter — *The Tending of the Unnamed Organ*

A surreal, grotesque incremental game built as a **dual-platform project**:
1. **Playable Webapp on GitHub Pages (`/docs`)** — zero-build static HTML5/Canvas/ES-Module edition ready to host directly on a GitHub Pages landing page, with `localStorage` autosave and pointer/touch support.
2. **Native Android App (`/app`)** — Kotlin + Jetpack Compose (Material 3) mobile edition.

Both platforms share the exact same **Escalation Curve (Phases 0–4)**, ~15-minute Cycle 0 → First Excision pacing, wet-lab **Organism Specimen Dossier Cards**, living anatomical chamber visuals (**Suture-Hands**, **Leech-Clerks**, **Weeping Lamps**, **Grafted Choir-Tendrils**), interactive wall anomalies, and progress-bound fragments in `dialogues.json`.

---

## Hosting the Playable Webapp on GitHub Pages

No build step is required. After pushing this repository to GitHub, choose **either** option:

- **Option A — Branch folder (fastest):**
  1. Open **Settings → Pages** in your GitHub repository.
  2. Under **Build and deployment → Source**, choose **Deploy from a branch**.
  3. Select branch **`main`** and folder **`/docs`**, then click **Save**.
- **Option B — GitHub Actions (automatic via `.github/workflows/pages.yml`):**
  1. Open **Settings → Pages**.
  2. Set **Source** to **GitHub Actions**. Every push to `main` automatically publishes `./docs`.

### Previewing the Webapp Locally
Serve `/docs` from any static HTTP server (required for ES modules and `fetch('./assets/dialogues.json')`):
```bash
python3 -m http.server 8080 --directory docs
```
Then open `http://localhost:8080`.

---

## Running Automated Tests

- **Web Engine & GitHub Pages Bundle Tests (Node.js):**
  ```bash
  node --test docs/tests/engine.test.mjs
  ```
- **Android Unit & Robolectric Compose UI Tests:**
  ```bash
  gradle :app:testDebugUnitTest
  ```
