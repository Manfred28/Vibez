package com.example.data

import android.content.Context
import org.json.JSONObject

interface DialogueRepository {
  fun loadCatalog(): DialogueCatalog
}

/**
 * Pure parser for `app/src/main/assets/dialogues.json` so dialogue loading stays decoupled from
 * UI/ViewModels and remains directly unit-testable.
 */
fun parseDialogueCatalog(rawJson: String): DialogueCatalog {
  val root = JSONObject(rawJson)
  val schemaVersion = root.optInt("schemaVersion", 1)

  val phasesArray = root.optJSONArray("phases")
  val phases = buildList {
    if (phasesArray != null) {
      for (i in 0 until phasesArray.length()) {
        val obj = phasesArray.getJSONObject(i)
        add(
          PhaseMetadata(
            id = obj.getInt("id"),
            name = obj.getString("name"),
            intensity = obj.getString("intensity"),
          )
        )
      }
    }
  }

  val entriesArray = root.optJSONArray("entries")
  val entries = buildList {
    if (entriesArray != null) {
      for (i in 0 until entriesArray.length()) {
        val obj = entriesArray.getJSONObject(i)
        val triggerObj = obj.getJSONObject("trigger")
        val target = triggerObj.optString("target", "").ifBlank { null }
        val linesArray = obj.getJSONArray("lines")
        val lines = buildList {
          for (lineIndex in 0 until linesArray.length()) {
            add(linesArray.getString(lineIndex))
          }
        }
        add(
          DialogueEntry(
            id = obj.getString("id"),
            phase = obj.getInt("phase"),
            mood = obj.getString("mood"),
            trigger =
              DialogueTrigger(
                type = triggerObj.getString("type"),
                threshold = triggerObj.getLong("threshold"),
                target = target,
              ),
            once = obj.optBoolean("once", true),
            speaker = obj.getString("speaker"),
            lines = lines,
          )
        )
      }
    }
  }

  return DialogueCatalog(schemaVersion = schemaVersion, phases = phases, entries = entries)
}

class AssetDialogueRepository(
  private val context: Context,
  private val assetPath: String = "dialogues.json",
) : DialogueRepository {
  override fun loadCatalog(): DialogueCatalog {
    val raw = context.assets.open(assetPath).bufferedReader().use { it.readText() }
    return parseDialogueCatalog(raw)
  }
}
