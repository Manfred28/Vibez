package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DialogueEntry
import kotlin.random.Random
import kotlinx.coroutines.delay

@Immutable
internal data class ThematicDripEvent(
  val id: Long,
  val speaker: String,
  val text: String,
  val eventKey: String = "$speaker:$text",
)

internal const val STORY_DRIP_SWELL_MS = 1600
internal const val STORY_DRIP_HOLD_MS = 6200L
internal const val STORY_DRIP_DESCEND_MS = 8200
internal const val STORY_DRIP_TOTAL_MS =
  STORY_DRIP_SWELL_MS + STORY_DRIP_HOLD_MS + STORY_DRIP_DESCEND_MS

internal const val REPEAT_EVENT_DRIP_CHANCE = 0.30

internal object EventDripGate {
  fun shouldShow(
    eventKey: String,
    seenKeys: Set<String>,
    roll: () -> Double = { Random.nextDouble() },
  ): Boolean {
    if (eventKey.isBlank()) return false
    if (eventKey !in seenKeys) return true
    val sample = roll()
    return !sample.isNaN() && sample < REPEAT_EVENT_DRIP_CHANCE
  }
}

/**
 * Dual-lane non-blocking side-margin dripping seepage overlay with strict story priority:
 *
 * 1. **Primary Priority Story Lane (Right Margin, `dripping_story_overlay`)**:
 *    Dedicated exclusively to newly unlocked `DialogueEntry` story fragments, seeping slowly
 *    (~16s) for comfortable reading. Wall anomalies, Neighbor Aperture echoes, or sacrifice notes
 *    can NEVER overwrite, interrupt, or overlap it. Skips initial mount/hydration so restoring a
 *    save does not falsely trigger a drip.
 *
 * 2. **Subordinate Event Lane (Left Margin, `dripping_event_overlay`)**:
 *    Hosts readable thematic descriptions for wall-anomaly interactions, Neighbor Aperture echo
 *    answers, and graft sacrifices (~4.8s). Completely hidden and suppressed whenever a primary
 *    story fragment is actively descending so story transmissions command undivided attention.
 *    Each distinct event key always drips the first time it is actually shown (100%), then triggers
 *    at a 30% random chance (`REPEAT_EVENT_DRIP_CHANCE`) on subsequent repeats.
 */
@Composable
internal fun DrippingTransmissionOverlay(
  activeDialogue: DialogueEntry?,
  latestEvent: ThematicDripEvent?,
  modifier: Modifier = Modifier,
  eventDripRoll: () -> Double = { Random.nextDouble() },
) {
  // Primary story state: never evicted by events, only updates on genuinely new dialogue IDs.
  var initializedDialogue by remember { mutableStateOf(false) }
  var lastSeenDialogueId by remember { mutableStateOf<String?>(null) }
  var activeStoryDrip by remember { mutableStateOf<DialogueEntry?>(null) }
  val storyProgress = remember { Animatable(0f) }

  // Subordinate chamber event state (anomalies / echoes / sacrifices) on the opposite left edge.
  var lastSeenEventId by remember { mutableStateOf<Long?>(null) }
  var seenEventKeys by remember { mutableStateOf(emptySet<String>()) }
  var activeEventDrip by remember { mutableStateOf<ThematicDripEvent?>(null) }
  val eventProgress = remember { Animatable(0f) }

  LaunchedEffect(activeDialogue == null && latestEvent == null) {
    if (activeDialogue == null && latestEvent == null) {
      seenEventKeys = emptySet()
    }
  }

  LaunchedEffect(activeDialogue?.id) {
    val currentId = activeDialogue?.id
    if (!initializedDialogue) {
      initializedDialogue = true
      lastSeenDialogueId = currentId
      return@LaunchedEffect
    }
    if (currentId != null && currentId != lastSeenDialogueId) {
      lastSeenDialogueId = currentId
      // Story takes exclusive priority — immediately clear any active chamber event drip
      activeEventDrip = null
      activeStoryDrip = activeDialogue
      storyProgress.snapTo(0f)
      // Swell bead slowly, pause for unhurried reading, then descend down the right margin (~16s total)
      storyProgress.animateTo(
        targetValue = 0.20f,
        animationSpec = tween(durationMillis = STORY_DRIP_SWELL_MS, easing = FastOutSlowInEasing),
      )
      delay(STORY_DRIP_HOLD_MS)
      storyProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = STORY_DRIP_DESCEND_MS, easing = LinearEasing),
      )
      activeStoryDrip = null
    }
  }

  LaunchedEffect(latestEvent?.id) {
    val event = latestEvent ?: return@LaunchedEffect
    if (event.id == lastSeenEventId) return@LaunchedEffect
    lastSeenEventId = event.id
    // Suppress chamber event drips whenever a story fragment is actively dripping (without marking seen)
    if (activeStoryDrip != null) return@LaunchedEffect
    if (!EventDripGate.shouldShow(event.eventKey, seenEventKeys, eventDripRoll)) {
      return@LaunchedEffect
    }
    seenEventKeys = seenEventKeys + event.eventKey
    activeEventDrip = event
    eventProgress.snapTo(0f)
    eventProgress.animateTo(
      targetValue = 0.24f,
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
    )
    delay(1500)
    eventProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 2600, easing = LinearEasing),
    )
    activeEventDrip = null
  }

  val storyActive = activeStoryDrip != null
  if (!storyActive && activeEventDrip == null) return

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val travelPx = (constraints.maxHeight * 0.56f).coerceAtLeast(120f)

    // Subordinate Left Margin Lane — completely hidden while a story drip is active
    if (!storyActive) {
      activeEventDrip?.let { event ->
        val p = eventProgress.value
        val fadeAlpha =
          when {
            p < 0.08f -> (p / 0.08f)
            p > 0.80f -> ((1f - p) / 0.20f)
            else -> 1f
          }.coerceIn(0f, 1f) * 0.90f

        DripMarginSlip(
          speaker = event.speaker,
          body = event.text,
          progress = p,
          travelPx = travelPx * 0.85f,
          alpha = fadeAlpha,
          alignRight = false,
          primaryPriority = false,
          testTag = "dripping_event_overlay",
          modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 74.dp),
        )
      }
    }

    // Primary Right Margin Lane (Story Fragments — strict top priority)
    activeStoryDrip?.let { entry ->
      val p = storyProgress.value
      val fadeAlpha =
        when {
          p < 0.07f -> (p / 0.07f)
          p > 0.84f -> ((1f - p) / 0.16f)
          else -> 1f
        }.coerceIn(0f, 1f)
      val excerpt = entry.lines.joinToString(" ").trim()

      DripMarginSlip(
        speaker = stringResource(R.string.transmission_speaker_format, entry.speaker.uppercase()),
        body = excerpt,
        progress = p,
        travelPx = travelPx,
        alpha = fadeAlpha,
        alignRight = true,
        primaryPriority = true,
        testTag = "dripping_story_overlay",
        modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 56.dp),
      )
    }
  }
}

@Composable
private fun DripMarginSlip(
  speaker: String,
  body: String,
  progress: Float,
  travelPx: Float,
  alpha: Float,
  alignRight: Boolean,
  primaryPriority: Boolean,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val beadPath = remember { Path() }
  val yOffsetPx = (progress * travelPx).toInt()
  val accentColor = if (primaryPriority) cs.primary else cs.tertiary

  Box(modifier = modifier.fillMaxHeight().alpha(alpha).testTag(testTag)) {
    // Vertical seepage streak anchored at the edge
    Canvas(
      modifier =
        Modifier.width(12.dp)
          .fillMaxHeight()
          .align(if (alignRight) Alignment.TopEnd else Alignment.TopStart)
    ) {
      val railX = if (alignRight) size.width - 3.dp.toPx() else 3.dp.toPx()
      val streakEndY = (yOffsetPx + 28.dp.toPx()).coerceAtMost(size.height)
      drawLine(
        brush =
          Brush.verticalGradient(
            colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.65f), accentColor),
            startY = 0f,
            endY = streakEndY.coerceAtLeast(1f),
          ),
        start = Offset(railX, 0f),
        end = Offset(railX, streakEndY),
        strokeWidth = if (primaryPriority) 2.dp.toPx() else 1.4.dp.toPx(),
        cap = StrokeCap.Round,
      )

      // Swelling condensation teardrop bead riding at the tip of the drip
      val tipY = streakEndY
      val w = if (primaryPriority) 3.4.dp.toPx() else 2.6.dp.toPx()
      beadPath.rewind()
      beadPath.moveTo(railX, tipY - 6.dp.toPx())
      beadPath.quadraticTo(railX - w, tipY + 1.5.dp.toPx(), railX, tipY + 4.dp.toPx())
      beadPath.quadraticTo(railX + w, tipY + 1.5.dp.toPx(), railX, tipY - 6.dp.toPx())
      beadPath.close()
      drawPath(beadPath, accentColor.copy(alpha = 0.90f))
    }

    // Translucent clinical slip hanging beside the seepage streak
    Row(
      modifier =
        Modifier.align(if (alignRight) Alignment.TopEnd else Alignment.TopStart)
          .padding(end = if (alignRight) 10.dp else 0.dp, start = if (alignRight) 0.dp else 10.dp)
          .offset { IntOffset(0, yOffsetPx) },
      verticalAlignment = Alignment.Top,
    ) {
      // Plain Box (not Material3 Surface) so pointer events pass completely through to underlying controls
      val slipShape = MaterialTheme.shapes.extraSmall
      Box(
        modifier =
          Modifier.widthIn(min = 136.dp, max = if (primaryPriority) 224.dp else 196.dp)
            .clip(slipShape)
            .background(cs.surface.copy(alpha = if (primaryPriority) 0.94f else 0.88f))
            .border(
              border =
                BorderStroke(
                  width = if (primaryPriority) 1.2.dp else 1.dp,
                  color = accentColor.copy(alpha = if (primaryPriority) 0.80f else 0.58f),
                ),
              shape = slipShape,
            )
      ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
          Text(
            text = speaker,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = body,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = cs.onSurface.copy(alpha = if (primaryPriority) 0.96f else 0.88f),
            maxLines = if (primaryPriority) 4 else 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
