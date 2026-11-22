package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val TendonOffsetsDp = floatArrayOf(-5.2f, -1.8f, 1.8f, 5.0f)
private val StitchSlotsDp = floatArrayOf(-3.4f, 0f, 3.3f)
private val FingerSpreadsDp = floatArrayOf(-6.2f, -2.0f, 2.3f, 6.4f)
private val FingerLengthBonusDp = floatArrayOf(1.5f, 4.8f, 2.4f, -2.6f)
private val FingerCrookDp = floatArrayOf(-1.6f, 0.9f, -1.2f, 2.3f)
private val FingerWidthsDp = floatArrayOf(2.15f, 2.30f, 2.10f, 1.75f)

/**
 * Rendered directly over the valve rim (non-clickable so touches pass straight to the valve).
 * Grotesque anatomical Suture-Hands: emaciated, gnarled 5-fingered pale hands emerging from raw
 * torn-masonry bruise cavities, bound at the severed wrist by crooked black-clot X-sutures with
 * oozing puncture stains and loose thread ends, exposed metacarpal tendons, an opposable scarred
 * thumb, and independently twitching spindly fingers with ragged dark-stained hooked nails clawing
 * the valve rim — plus braided Grafted Choir-Tendrils.
 */
@Composable
internal fun SutureHandsValveOverlay(
  breathPhaseProvider: () -> Float,
  sutureCount: Int,
  graftCount: Int,
  boneColor: Color,
  stitchColor: Color,
  shadowColor: Color,
  modifier: Modifier = Modifier,
) {
  if (sutureCount <= 0 && graftCount <= 0) return

  val knuckleTone = MaterialTheme.colorScheme.onSurfaceVariant
  val bruiseTone = MaterialTheme.colorScheme.tertiaryContainer
  val bileTone = MaterialTheme.colorScheme.secondary
  val nailHighlight = MaterialTheme.colorScheme.onBackground

  // Reusable scratch paths avoid ~50+ Path allocations per 60fps animation frame.
  val primaryStrand = remember { Path() }
  val secondaryStrand = remember { Path() }
  val palmPath = remember { Path() }
  val incisionPath = remember { Path() }
  val thumbPath = remember { Path() }
  val fingerPath = remember { Path() }

  Canvas(modifier = modifier.size(228.dp).testTag("suture_hands_overlay")) {
    val breath = breathPhaseProvider()
    val center = Offset(size.width / 2f, size.height / 2f)
    val valveRimRadius = 64.dp.toPx()
    val armStartRadius = size.minDimension * 0.495f

    // Thick intertwined double-strand Grafted Choir-Tendrils arching onto the valve collar
    val visibleGrafts = min(graftCount, 4)
    for (g in 0 until visibleGrafts) {
      val angle = (g * (PI / 2.0)) + PI / 4.0 + (breath - 0.5) * 0.10
      val dirX = cos(angle).toFloat()
      val dirY = sin(angle).toFloat()
      val perpX = -dirY
      val perpY = dirX

      val start = Offset(center.x + dirX * armStartRadius, center.y + dirY * armStartRadius)
      val midR = (armStartRadius + valveRimRadius) * 0.52f
      val midA =
        Offset(
          center.x + dirX * midR + perpX * 8.5.dp.toPx(),
          center.y + dirY * midR + perpY * 8.5.dp.toPx(),
        )
      val midB =
        Offset(
          center.x + dirX * midR - perpX * 6.5.dp.toPx(),
          center.y + dirY * midR - perpY * 6.5.dp.toPx(),
        )
      val end =
        Offset(
          center.x + dirX * (valveRimRadius - 4.dp.toPx()),
          center.y + dirY * (valveRimRadius - 4.dp.toPx()),
        )

      primaryStrand.rewind()
      primaryStrand.moveTo(start.x, start.y)
      primaryStrand.quadraticTo(midA.x, midA.y, end.x, end.y)

      secondaryStrand.rewind()
      secondaryStrand.moveTo(start.x - perpX * 2.dp.toPx(), start.y - perpY * 2.dp.toPx())
      secondaryStrand.quadraticTo(midB.x, midB.y, end.x, end.y)

      drawPath(
        primaryStrand,
        shadowColor.copy(alpha = 0.90f),
        style = Stroke(8.2.dp.toPx(), cap = StrokeCap.Round),
      )
      drawPath(
        secondaryStrand,
        shadowColor.copy(alpha = 0.82f),
        style = Stroke(5.6.dp.toPx(), cap = StrokeCap.Round),
      )
      drawPath(
        primaryStrand,
        stitchColor.copy(alpha = 0.92f),
        style = Stroke(4.2.dp.toPx(), cap = StrokeCap.Round),
      )
      drawPath(
        secondaryStrand,
        knuckleTone.copy(alpha = 0.72f),
        style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round),
      )
      drawCircle(color = shadowColor, radius = 4.6.dp.toPx(), center = end)
      drawCircle(color = boneColor.copy(alpha = 0.84f), radius = 3.1.dp.toPx(), center = end)
    }

    // Grotesque, emaciated 5-fingered stitched Suture-Hands clawing and drumming the valve rim
    val visibleHands = min(sutureCount, 8)
    for (i in 0 until visibleHands) {
      val baseAngle = (i * (2.0 * PI / visibleHands)) - PI / 2.0 + (PI / 8.0)
      val handedness = if (i % 2 == 0) 1f else -1f
      val dirX = cos(baseAngle).toFloat()
      val dirY = sin(baseAngle).toFloat()
      val perpX = -dirY * handedness
      val perpY = dirX * handedness

      // Unsettling irregular tremor & slow creeping press wave
      val pressWave =
        ((sin(breath * 2.0 * PI + i * 1.15) * 0.78 + sin(breath * 6.0 * PI + i * 2.3) * 0.22 + 1.0) *
            0.5)
          .toFloat()
      val pressInset = pressWave * 6.8.dp.toPx()

      val wristR = armStartRadius
      val seamR = armStartRadius - 7.5.dp.toPx()
      val palmR = valveRimRadius + 15.5.dp.toPx() - pressInset * 0.55f
      val tipBaseR = valveRimRadius - 7.5.dp.toPx() - pressInset

      val wrist = Offset(center.x + dirX * wristR, center.y + dirY * wristR)
      val seamCenter = Offset(center.x + dirX * seamR, center.y + dirY * seamR)
      val palm = Offset(center.x + dirX * palmR, center.y + dirY * palmR)

      // 1. Raw dark bruised masonry wound cavity where the severed stump pushes through
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                shadowColor.copy(alpha = 0.96f),
                bruiseTone.copy(alpha = 0.72f),
                Color.Transparent,
              ),
            center = wrist,
            radius = 12.dp.toPx(),
          ),
        radius = 12.dp.toPx(),
        center = wrist,
      )

      // 2. Asymmetric skeletal wrist-to-metacarpal palm silhouette (pinched narrow wrist, knobby
      //    side carpals, sunken hollowed palm)
      val wristHalfW = 4.6.dp.toPx()
      val pinchHalfW = 3.4.dp.toPx()
      val palmLeftW = 8.4.dp.toPx()
      val palmRightW = 7.2.dp.toPx()
      val midR = (wristR + palmR) * 0.52f
      val midWrist = Offset(center.x + dirX * midR, center.y + dirY * midR)

      palmPath.rewind()
      palmPath.moveTo(wrist.x - perpX * wristHalfW, wrist.y - perpY * wristHalfW)
      palmPath.cubicTo(
        midWrist.x - perpX * pinchHalfW,
        midWrist.y - perpY * pinchHalfW,
        palm.x + dirX * 5.dp.toPx() - perpX * (palmLeftW * 0.95f),
        palm.y + dirY * 5.dp.toPx() - perpY * (palmLeftW * 0.95f),
        palm.x - perpX * palmLeftW,
        palm.y - perpY * palmLeftW,
      )
      // Knobby uneven knuckle ridge across the metacarpal heads
      palmPath.quadraticTo(
        palm.x - dirX * 4.2.dp.toPx() - perpX * 2.dp.toPx(),
        palm.y - dirY * 4.2.dp.toPx() - perpY * 2.dp.toPx(),
        palm.x - dirX * 2.6.dp.toPx() + perpX * 2.5.dp.toPx(),
        palm.y - dirY * 2.6.dp.toPx() + perpY * 2.5.dp.toPx(),
      )
      palmPath.quadraticTo(
        palm.x - dirX * 1.2.dp.toPx() + perpX * (palmRightW * 0.82f),
        palm.y - dirY * 1.2.dp.toPx() + perpY * (palmRightW * 0.82f),
        palm.x + perpX * palmRightW,
        palm.y + perpY * palmRightW,
      )
      palmPath.cubicTo(
        palm.x + dirX * 5.dp.toPx() + perpX * (palmRightW * 0.88f),
        palm.y + dirY * 5.dp.toPx() + perpY * (palmRightW * 0.88f),
        midWrist.x + perpX * (pinchHalfW * 1.15f),
        midWrist.y + perpY * (pinchHalfW * 1.15f),
        wrist.x + perpX * wristHalfW,
        wrist.y + perpY * wristHalfW,
      )
      palmPath.close()

      // Deep dark outline shadow + mottled corpse-pale & bruised-clot flesh gradient
      drawPath(
        palmPath,
        shadowColor.copy(alpha = 0.96f),
        style = Stroke(3.6.dp.toPx(), join = StrokeJoin.Round),
      )
      drawPath(
        path = palmPath,
        brush =
          Brush.linearGradient(
            colors =
              listOf(
                bruiseTone.copy(alpha = 0.95f),
                bileTone.copy(alpha = 0.76f),
                knuckleTone.copy(alpha = 0.88f),
                boneColor.copy(alpha = 0.94f),
              ),
            start = wrist,
            end = Offset(palm.x - dirX * 3.dp.toPx(), palm.y - dirY * 3.dp.toPx()),
          ),
      )

      // Sunken hollow shadow between dorsal tendons
      val hollowCenter = Offset(palm.x + dirX * 4.5.dp.toPx(), palm.y + dirY * 4.5.dp.toPx())
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(shadowColor.copy(alpha = 0.44f), Color.Transparent),
            center = hollowCenter,
            radius = 5.5.dp.toPx(),
          ),
        radius = 5.5.dp.toPx(),
        center = hollowCenter,
      )

      // Exposed pale dorsal metacarpal tendon cords stretching from stitched wrist to knuckles
      for (tIdx in TendonOffsetsDp.indices) {
        val off = TendonOffsetsDp[tIdx].dp.toPx()
        val tStart = Offset(seamCenter.x + perpX * (off * 0.42f), seamCenter.y + perpY * (off * 0.42f))
        val tEnd =
          Offset(
            palm.x - dirX * 1.5.dp.toPx() + perpX * off,
            palm.y - dirY * 1.5.dp.toPx() + perpY * off,
          )
        drawLine(
          color = shadowColor.copy(alpha = 0.55f),
          start = tStart,
          end = tEnd,
          strokeWidth = 1.9.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = boneColor.copy(alpha = 0.64f),
          start = tStart,
          end = tEnd,
          strokeWidth = 0.85.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // 3. Raw jagged incision line across wrist + crooked black-clot X-sutures & loose thread ends
      val seamLeft = Offset(seamCenter.x - perpX * 5.2.dp.toPx(), seamCenter.y - perpY * 5.2.dp.toPx())
      val seamMid =
        Offset(
          seamCenter.x + dirX * 1.4.dp.toPx() - perpX * 0.5.dp.toPx(),
          seamCenter.y + dirY * 1.4.dp.toPx() - perpY * 0.5.dp.toPx(),
        )
      val seamRight = Offset(seamCenter.x + perpX * 5.2.dp.toPx(), seamCenter.y + perpY * 5.2.dp.toPx())
      incisionPath.rewind()
      incisionPath.moveTo(seamLeft.x, seamLeft.y)
      incisionPath.lineTo(seamMid.x, seamMid.y)
      incisionPath.lineTo(seamRight.x, seamRight.y)
      drawPath(
        incisionPath,
        shadowColor.copy(alpha = 0.96f),
        style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round),
      )
      drawPath(
        incisionPath,
        stitchColor.copy(alpha = 0.94f),
        style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round),
      )

      // Crooked criss-cross X-stitches with weeping clot punctures & frayed protruding ends
      for (slotIdx in StitchSlotsDp.indices) {
        val sxOff = StitchSlotsDp[slotIdx].dp.toPx()
        val sc = Offset(seamCenter.x + perpX * sxOff, seamCenter.y + perpY * sxOff)
        val diagAlong = (2.6f + (slotIdx % 2) * 0.6f).dp.toPx()
        val diagAcross = 1.55.dp.toPx()
        val a1 = Offset(sc.x + dirX * diagAlong - perpX * diagAcross, sc.y + dirY * diagAlong - perpY * diagAcross)
        val a2 = Offset(sc.x - dirX * diagAlong + perpX * diagAcross, sc.y - dirY * diagAlong + perpY * diagAcross)
        val b1 = Offset(sc.x + dirX * diagAlong + perpX * diagAcross, sc.y + dirY * diagAlong + perpY * diagAcross)
        val b2 = Offset(sc.x - dirX * diagAlong - perpX * diagAcross, sc.y - dirY * diagAlong - perpY * diagAcross)

        drawLine(shadowColor.copy(alpha = 0.95f), a1, a2, strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
        drawLine(shadowColor.copy(alpha = 0.95f), b1, b2, strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
        drawLine(stitchColor.copy(alpha = 0.96f), a1, a2, strokeWidth = 0.95.dp.toPx(), cap = StrokeCap.Round)
        drawLine(stitchColor.copy(alpha = 0.96f), b1, b2, strokeWidth = 0.95.dp.toPx(), cap = StrokeCap.Round)

        // Tiny weeping dark-clot needle punctures
        drawCircle(stitchColor.copy(alpha = 0.92f), radius = 1.15.dp.toPx(), center = a1)
        drawCircle(stitchColor.copy(alpha = 0.92f), radius = 1.15.dp.toPx(), center = a2)
      }
      // Stray loose black surgical thread tail jutting off the wrist margin
      val looseEnd =
        Offset(
          seamRight.x + perpX * 3.8.dp.toPx() + dirX * (2.2f + pressWave * 1.4f).dp.toPx(),
          seamRight.y + perpY * 3.8.dp.toPx() + dirY * (2.2f + pressWave * 1.4f).dp.toPx(),
        )
      drawLine(
        color = shadowColor.copy(alpha = 0.90f),
        start = seamRight,
        end = looseEnd,
        strokeWidth = 1.1.dp.toPx(),
        cap = StrokeCap.Round,
      )

      // 4. Opposable crooked scarred thumb gripping from the side (echoes dlg_p0_suture_hand)
      val thumbTwitch = sin(breath * 2.0 * PI + i * 1.7 + 0.5).toFloat() * 1.6.dp.toPx()
      val thumbBase =
        Offset(
          palm.x + dirX * 5.2.dp.toPx() - perpX * (palmLeftW * 0.78f),
          palm.y + dirY * 5.2.dp.toPx() - perpY * (palmLeftW * 0.78f),
        )
      val thumbJoint =
        Offset(
          palm.x + dirX * 0.6.dp.toPx() - perpX * (palmLeftW + 4.4.dp.toPx()),
          palm.y + dirY * 0.6.dp.toPx() - perpY * (palmLeftW + 4.4.dp.toPx()),
        )
      val thumbTip =
        Offset(
          palm.x - dirX * (5.6.dp.toPx() + thumbTwitch) - perpX * (palmLeftW + 1.2.dp.toPx()),
          palm.y - dirY * (5.6.dp.toPx() + thumbTwitch) - perpY * (palmLeftW + 1.2.dp.toPx()),
        )
      thumbPath.rewind()
      thumbPath.moveTo(thumbBase.x, thumbBase.y)
      thumbPath.lineTo(thumbJoint.x, thumbJoint.y)
      thumbPath.lineTo(thumbTip.x, thumbTip.y)
      drawPath(
        thumbPath,
        shadowColor.copy(alpha = 0.95f),
        style = Stroke(4.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
      drawPath(
        thumbPath,
        boneColor.copy(alpha = 0.92f),
        style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
      // Swollen thumb knuckle + pale jagged transverse scar matching dlg_p0_suture_hand
      drawCircle(knuckleTone.copy(alpha = 0.88f), radius = 1.65.dp.toPx(), center = thumbJoint)
      drawLine(
        color = nailHighlight.copy(alpha = 0.92f),
        start =
          Offset(
            thumbJoint.x - dirX * 1.3.dp.toPx() - perpX * 1.1.dp.toPx(),
            thumbJoint.y - dirY * 1.3.dp.toPx() - perpY * 1.1.dp.toPx(),
          ),
        end =
          Offset(
            thumbJoint.x + dirX * 1.3.dp.toPx() + perpX * 1.1.dp.toPx(),
            thumbJoint.y + dirY * 1.3.dp.toPx() + perpY * 1.1.dp.toPx(),
          ),
        strokeWidth = 0.95.dp.toPx(),
        cap = StrokeCap.Round,
      )
      // Dark ragged hooked thumbnail
      drawCircle(shadowColor.copy(alpha = 0.96f), radius = 1.55.dp.toPx(), center = thumbTip)
      drawCircle(stitchColor.copy(alpha = 0.85f), radius = 0.95.dp.toPx(), center = thumbTip)

      // 5. Four gaunt, over-long, crooked multi-jointed fingers with asynchronous spider-like twitch
      for (f in FingerSpreadsDp.indices) {
        val spreadPx = FingerSpreadsDp[f].dp.toPx()
        val crookPx = FingerCrookDp[f].dp.toPx()
        val lengthBonus = FingerLengthBonusDp[f]
        // Independent creeping drum/scratch per finger
        val fingerPhase = breath * 2.0 * PI + i * 1.37 + f * 1.18
        val fingerCurl = ((sin(fingerPhase) + 1.0) * 0.5).toFloat()
        val fingerJitter = sin(fingerPhase * 2.0 + i).toFloat() * 0.9.dp.toPx()

        val mcpInset = if (f == 1 || f == 2) 2.2.dp.toPx() else 0.8.dp.toPx()
        val mcp =
          Offset(
            palm.x - dirX * mcpInset + perpX * (spreadPx * 0.84f),
            palm.y - dirY * mcpInset + perpY * (spreadPx * 0.84f),
          )
        val pip =
          Offset(
            mcp.x - dirX * (6.8.dp.toPx() + lengthBonus * 0.35f.dp.toPx()) +
              perpX * (spreadPx * 0.24f + crookPx + fingerJitter * 0.4f),
            mcp.y - dirY * (6.8.dp.toPx() + lengthBonus * 0.35f.dp.toPx()) +
              perpY * (spreadPx * 0.24f + crookPx + fingerJitter * 0.4f),
          )
        val dip =
          Offset(
            pip.x - dirX * (5.6.dp.toPx() + lengthBonus * 0.35f.dp.toPx()) +
              perpX * (-crookPx * 0.65f + fingerJitter),
            pip.y - dirY * (5.6.dp.toPx() + lengthBonus * 0.35f.dp.toPx()) +
              perpY * (-crookPx * 0.65f + fingerJitter),
          )

        val tipR = tipBaseR - lengthBonus.dp.toPx() - fingerCurl * 3.8.dp.toPx()
        val tip =
          Offset(
            center.x + dirX * tipR + perpX * (spreadPx * 0.62f + crookPx * 0.35f),
            center.y + dirY * tipR + perpY * (spreadPx * 0.62f + crookPx * 0.35f),
          )

        fingerPath.rewind()
        fingerPath.moveTo(mcp.x, mcp.y)
        fingerPath.lineTo(pip.x, pip.y)
        fingerPath.lineTo(dip.x, dip.y)
        fingerPath.lineTo(tip.x, tip.y)

        // Dark outline + gaunt skeletal phalange shading
        val w = FingerWidthsDp[f].dp.toPx()
        drawPath(
          fingerPath,
          shadowColor.copy(alpha = 0.96f),
          style = Stroke(w + 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
          fingerPath,
          Brush.linearGradient(
            colors = listOf(knuckleTone.copy(alpha = 0.90f), boneColor.copy(alpha = 0.96f)),
            start = mcp,
            end = tip,
          ),
          style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Swollen bony knuckle joints (MCP + PIP + DIP)
        drawCircle(knuckleTone.copy(alpha = 0.88f), radius = w * 0.72f, center = mcp)
        drawCircle(boneColor.copy(alpha = 0.92f), radius = w * 0.68f, center = pip)
        drawCircle(shadowColor.copy(alpha = 0.42f), radius = w * 0.32f, center = pip)
        drawCircle(knuckleTone.copy(alpha = 0.86f), radius = w * 0.56f, center = dip)

        // Hooked, dark-clot-stained ragged fingernail claw digging into the iron valve collar
        val nailTip = Offset(tip.x - dirX * 2.1.dp.toPx(), tip.y - dirY * 2.1.dp.toPx())
        drawLine(
          color = shadowColor.copy(alpha = 0.98f),
          start = tip,
          end = nailTip,
          strokeWidth = 2.0.dp.toPx(),
          cap = StrokeCap.Round,
        )
        drawLine(
          color = stitchColor.copy(alpha = 0.88f),
          start = tip,
          end = nailTip,
          strokeWidth = 1.0.dp.toPx(),
          cap = StrokeCap.Round,
        )

        // Faint wet dark-ichor contact smear when a claw presses deep into the rim
        if (fingerCurl > 0.76f) {
          val smearAlpha = ((fingerCurl - 0.76f) / 0.24f) * 0.56f
          drawCircle(
            color = stitchColor.copy(alpha = smearAlpha),
            radius = (2.8f + fingerCurl * 3.8f).dp.toPx(),
            center = nailTip,
            style = Stroke(width = 1.1.dp.toPx()),
          )
        }
      }
    }
  }
}
