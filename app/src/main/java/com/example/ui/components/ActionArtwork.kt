package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.DredgedRelic
import com.example.game.SpecimenJar

internal enum class RiteArtworkKind {
  FLOOR_DRAIN,
  NEIGHBOR_APERTURE,
  VALVE_CLAMP,
  OSSIFIED_THROAT,
}

/** Framed 40dp clinical-anatomical medallion for each of the 6 Specimen Jars. */
@Composable
internal fun JarArtworkEmblem(
  jar: SpecimenJar,
  owned: Boolean,
  affordable: Boolean,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val path = remember { Path() }
  val borderColor =
    when {
      owned -> cs.tertiary
      affordable -> cs.primary.copy(alpha = 0.72f)
      else -> cs.outlineVariant
    }

  Surface(
    modifier = modifier.size(40.dp).testTag("art_jar_${jar.id}"),
    shape = MaterialTheme.shapes.extraSmall,
    color = cs.background.copy(alpha = 0.90f),
    border = BorderStroke(1.dp, borderColor),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width * 0.5f
      val glassColor = if (owned) cs.tertiary else if (affordable) cs.primary else cs.outline
      val fillTone =
        when {
          owned -> cs.tertiary.copy(alpha = 0.42f)
          affordable -> cs.primary.copy(alpha = 0.34f)
          else -> cs.secondary.copy(alpha = 0.22f)
        }

      // Wax cork lid
      drawRoundRect(
        color = if (owned) cs.tertiary else cs.onSurfaceVariant.copy(alpha = 0.72f),
        topLeft = Offset(cx - 5.5.dp.toPx(), 4.5.dp.toPx()),
        size = Size(11.dp.toPx(), 4.dp.toPx()),
        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
      )

      // Apothecary jar body
      path.rewind()
      path.moveTo(cx - 4.5.dp.toPx(), 9.dp.toPx())
      path.lineTo(cx - 10.dp.toPx(), 15.dp.toPx())
      path.lineTo(cx - 10.dp.toPx(), 32.dp.toPx())
      path.quadraticTo(cx - 10.dp.toPx(), 35.5.dp.toPx(), cx - 6.dp.toPx(), 35.5.dp.toPx())
      path.lineTo(cx + 6.dp.toPx(), 35.5.dp.toPx())
      path.quadraticTo(cx + 10.dp.toPx(), 35.5.dp.toPx(), cx + 10.dp.toPx(), 32.dp.toPx())
      path.lineTo(cx + 10.dp.toPx(), 15.dp.toPx())
      path.lineTo(cx + 4.5.dp.toPx(), 9.dp.toPx())
      path.close()
      drawPath(path, cs.surfaceVariant.copy(alpha = 0.55f))

      // Unique specimen interior per jar tier
      when (jar) {
        SpecimenJar.YESTERDAYS_DUST -> {
          drawRect(
            color = fillTone,
            topLeft = Offset(cx - 8.5.dp.toPx(), 25.dp.toPx()),
            size = Size(17.dp.toPx(), 9.dp.toPx()),
          )
          for (i in -2..2) {
            drawCircle(
              color = glassColor.copy(alpha = 0.65f),
              radius = 1.dp.toPx(),
              center = Offset(cx + i * 3.dp.toPx(), (21 + (i * i) % 3).dp.toPx()),
            )
          }
        }
        SpecimenJar.UNSPENT_BREATH -> {
          path.rewind()
          path.moveTo(cx - 5.dp.toPx(), 30.dp.toPx())
          path.cubicTo(cx - 7.dp.toPx(), 22.dp.toPx(), cx + 7.dp.toPx(), 23.dp.toPx(), cx + 1.dp.toPx(), 15.dp.toPx())
          drawPath(path, glassColor.copy(alpha = 0.78f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        SpecimenJar.ALMOST_A_NAME -> {
          // Folded blurred paper tag inside fluid
          drawRect(
            color = cs.onBackground.copy(alpha = 0.35f),
            topLeft = Offset(cx - 5.dp.toPx(), 18.dp.toPx()),
            size = Size(10.dp.toPx(), 13.dp.toPx()),
          )
          drawLine(glassColor, Offset(cx - 3.dp.toPx(), 22.dp.toPx()), Offset(cx + 3.dp.toPx(), 22.dp.toPx()), 1.2.dp.toPx())
          drawLine(glassColor, Offset(cx - 3.dp.toPx(), 26.dp.toPx()), Offset(cx + 1.5.dp.toPx(), 26.dp.toPx()), 1.2.dp.toPx())
        }
        SpecimenJar.SALT_FROM_THE_GROUT -> {
          // Jagged crystalline brine shards rising from bottom
          path.rewind()
          path.moveTo(cx - 8.dp.toPx(), 34.dp.toPx())
          path.lineTo(cx - 4.dp.toPx(), 21.dp.toPx())
          path.lineTo(cx, 28.dp.toPx())
          path.lineTo(cx + 4.dp.toPx(), 18.dp.toPx())
          path.lineTo(cx + 8.dp.toPx(), 34.dp.toPx())
          path.close()
          drawPath(path, fillTone)
          drawPath(path, glassColor.copy(alpha = 0.75f), style = Stroke(1.dp.toPx()))
        }
        SpecimenJar.A_BORROWED_PULSE -> {
          // Suspended pulsing clot bead with concentric beat ring
          drawCircle(cs.tertiary.copy(alpha = 0.78f), radius = 4.2.dp.toPx(), center = Offset(cx, 24.dp.toPx()))
          drawCircle(
            color = glassColor.copy(alpha = 0.65f),
            radius = 7.dp.toPx(),
            center = Offset(cx, 24.dp.toPx()),
            style = Stroke(1.dp.toPx()),
          )
        }
        SpecimenJar.THE_UNCOUNTED_HOUR -> {
          // Tilted hourglass inside sealed jar
          path.rewind()
          path.moveTo(cx - 5.dp.toPx(), 16.dp.toPx())
          path.lineTo(cx + 5.dp.toPx(), 16.dp.toPx())
          path.lineTo(cx, 24.dp.toPx())
          path.lineTo(cx + 5.dp.toPx(), 32.dp.toPx())
          path.lineTo(cx - 5.dp.toPx(), 32.dp.toPx())
          path.lineTo(cx, 24.dp.toPx())
          path.close()
          drawPath(path, fillTone)
          drawPath(path, glassColor.copy(alpha = 0.85f), style = Stroke(1.1.dp.toPx(), join = StrokeJoin.Round))
        }
      }

      // Jar outline
      path.rewind()
      path.moveTo(cx - 4.5.dp.toPx(), 9.dp.toPx())
      path.lineTo(cx - 10.dp.toPx(), 15.dp.toPx())
      path.lineTo(cx - 10.dp.toPx(), 32.dp.toPx())
      path.quadraticTo(cx - 10.dp.toPx(), 35.5.dp.toPx(), cx - 6.dp.toPx(), 35.5.dp.toPx())
      path.lineTo(cx + 6.dp.toPx(), 35.5.dp.toPx())
      path.quadraticTo(cx + 10.dp.toPx(), 35.5.dp.toPx(), cx + 10.dp.toPx(), 32.dp.toPx())
      path.lineTo(cx + 10.dp.toPx(), 15.dp.toPx())
      path.lineTo(cx + 4.5.dp.toPx(), 9.dp.toPx())
      drawPath(path, glassColor.copy(alpha = 0.90f), style = Stroke(1.2.dp.toPx()))
    }
  }
}

/** Framed 40dp procedural medallion for Choir-Tendril & Choir-Lung Grafting actions. */
@Composable
internal fun GraftArtworkEmblem(
  isLung: Boolean,
  ready: Boolean,
  onCooldown: Boolean,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val path = remember { Path() }
  val borderColor =
    when {
      onCooldown -> cs.outline
      ready -> cs.tertiary
      else -> cs.outlineVariant
    }

  Surface(
    modifier = modifier.size(40.dp).testTag(if (isLung) "art_graft_lung" else "art_graft_tendril"),
    shape = MaterialTheme.shapes.extraSmall,
    color = cs.background.copy(alpha = 0.90f),
    border = BorderStroke(1.dp, borderColor),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width * 0.5f
      val cy = size.height * 0.5f
      val accent = if (ready && !onCooldown) cs.tertiary else cs.onSurfaceVariant

      if (!isLung) {
        // Braided twin-strand suture tendril
        path.rewind()
        path.moveTo(8.dp.toPx(), 33.dp.toPx())
        path.cubicTo(14.dp.toPx(), 10.dp.toPx(), 26.dp.toPx(), 30.dp.toPx(), 32.dp.toPx(), 7.dp.toPx())
        drawPath(path, accent.copy(alpha = 0.92f), style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))

        path.rewind()
        path.moveTo(11.dp.toPx(), 35.dp.toPx())
        path.cubicTo(22.dp.toPx(), 28.dp.toPx(), 16.dp.toPx(), 12.dp.toPx(), 29.dp.toPx(), 6.dp.toPx())
        drawPath(path, cs.primary.copy(alpha = 0.72f), style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round))

        // Suture cross-ticks binding the braid
        for (i in 0..2) {
          val tx = (14 + i * 6).dp.toPx()
          val ty = (26 - i * 6).dp.toPx()
          drawLine(cs.onBackground.copy(alpha = 0.8f), Offset(tx - 3.dp.toPx(), ty - 2.dp.toPx()), Offset(tx + 3.dp.toPx(), ty + 2.dp.toPx()), 1.1.dp.toPx())
        }
      } else {
        // Paired ribbed Choir-Lung lobes around central windpipe
        drawLine(cs.onBackground.copy(alpha = 0.75f), Offset(cx, 6.dp.toPx()), Offset(cx, 20.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        path.rewind()
        path.moveTo(cx - 2.dp.toPx(), 14.dp.toPx())
        path.cubicTo(cx - 14.dp.toPx(), 13.dp.toPx(), cx - 15.dp.toPx(), 32.dp.toPx(), cx - 3.dp.toPx(), 33.dp.toPx())
        path.close()
        drawPath(path, accent.copy(alpha = 0.42f))
        drawPath(path, accent.copy(alpha = 0.92f), style = Stroke(1.3.dp.toPx()))

        path.rewind()
        path.moveTo(cx + 2.dp.toPx(), 14.dp.toPx())
        path.cubicTo(cx + 14.dp.toPx(), 13.dp.toPx(), cx + 15.dp.toPx(), 32.dp.toPx(), cx + 3.dp.toPx(), 33.dp.toPx())
        path.close()
        drawPath(path, accent.copy(alpha = 0.42f))
        drawPath(path, accent.copy(alpha = 0.92f), style = Stroke(1.3.dp.toPx()))

        // Hanging stillness droplet below center
        drawCircle(cs.primary.copy(alpha = 0.85f), radius = 2.dp.toPx(), center = Offset(cx, cy + 12.dp.toPx()))
      }
    }
  }
}

/** Framed 38dp procedural medallion for Excision II–V Chamber Rites. */
@Composable
internal fun RiteArtworkEmblem(
  kind: RiteArtworkKind,
  active: Boolean,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  val path = remember { Path() }
  Surface(
    modifier = modifier.size(38.dp).testTag("art_rite_${kind.name.lowercase()}"),
    shape = MaterialTheme.shapes.extraSmall,
    color = cs.background.copy(alpha = 0.92f),
    border = BorderStroke(1.dp, if (active) cs.primary else cs.outlineVariant),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width * 0.5f
      val cy = size.height * 0.5f
      val strokeColor = if (active) cs.primary else cs.onSurfaceVariant

      when (kind) {
        RiteArtworkKind.FLOOR_DRAIN -> {
          drawRect(
            color = strokeColor.copy(alpha = 0.85f),
            topLeft = Offset(7.dp.toPx(), 9.dp.toPx()),
            size = Size(24.dp.toPx(), 20.dp.toPx()),
            style = Stroke(1.3.dp.toPx()),
          )
          for (i in 1..4) {
            val bx = (7 + i * 4.8f).dp.toPx()
            drawLine(strokeColor, Offset(bx, 9.dp.toPx()), Offset(bx, 29.dp.toPx()), 1.3.dp.toPx())
          }
          if (active) {
            drawCircle(cs.tertiary.copy(alpha = 0.45f), radius = 5.dp.toPx(), center = Offset(cx, cy))
          }
        }
        RiteArtworkKind.NEIGHBOR_APERTURE -> {
          drawLine(cs.background, Offset(cx, 6.dp.toPx()), Offset(cx, 32.dp.toPx()), 4.dp.toPx(), StrokeCap.Round)
          drawLine(strokeColor, Offset(cx, 6.dp.toPx()), Offset(cx, 32.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
          for (r in 1..2) {
            drawCircle(
              color = strokeColor.copy(alpha = 0.55f / r),
              radius = (5f + r * 5f).dp.toPx(),
              center = Offset(cx, cy),
              style = Stroke(1.1.dp.toPx()),
            )
          }
        }
        RiteArtworkKind.VALVE_CLAMP -> {
          drawCircle(cs.outline, radius = 9.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
          // Diagonal iron clamp bar + bolts
          drawLine(if (active) cs.tertiary else strokeColor, Offset(7.dp.toPx(), 10.dp.toPx()), Offset(31.dp.toPx(), 28.dp.toPx()), 2.8.dp.toPx(), StrokeCap.Round)
          drawCircle(cs.onBackground, radius = 2.dp.toPx(), center = Offset(10.dp.toPx(), 12.dp.toPx()))
          drawCircle(cs.onBackground, radius = 2.dp.toPx(), center = Offset(28.dp.toPx(), 26.dp.toPx()))
        }
        RiteArtworkKind.OSSIFIED_THROAT -> {
          path.rewind()
          path.moveTo(cx, 6.dp.toPx())
          path.cubicTo(31.dp.toPx(), 12.dp.toPx(), 31.dp.toPx(), 26.dp.toPx(), cx, 32.dp.toPx())
          path.cubicTo(7.dp.toPx(), 26.dp.toPx(), 7.dp.toPx(), 12.dp.toPx(), cx, 6.dp.toPx())
          path.close()
          drawPath(path, cs.tertiaryContainer.copy(alpha = 0.72f))
          drawPath(path, strokeColor, style = Stroke(1.3.dp.toPx()))
          // Calcified inner teeth ridges
          for (t in -1..1) {
            val ty = cy + t * 5.dp.toPx()
            drawLine(cs.onBackground.copy(alpha = 0.72f), Offset(cx - 5.dp.toPx(), ty), Offset(cx - 2.dp.toPx(), ty), 1.3.dp.toPx())
            drawLine(cs.onBackground.copy(alpha = 0.72f), Offset(cx + 2.dp.toPx(), ty), Offset(cx + 5.dp.toPx(), ty), 1.3.dp.toPx())
          }
        }
      }
    }
  }
}

/** Framed 26dp clinical icon emblem for Dredged Relics (tappable inspection chips). */
@Composable
internal fun RelicArtworkEmblem(
  relic: DredgedRelic,
  owned: Boolean,
  modifier: Modifier = Modifier,
) {
  val cs = MaterialTheme.colorScheme
  Box(modifier = modifier.size(22.dp)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width * 0.5f
      val cy = size.height * 0.5f
      val ink = if (owned) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.55f)

      when (relic) {
        DredgedRelic.PATIENT_WRISTBAND -> {
          drawRoundRect(
            color = ink,
            topLeft = Offset(2.dp.toPx(), 6.dp.toPx()),
            size = Size(18.dp.toPx(), 10.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(1.4.dp.toPx()),
          )
          drawLine(ink, Offset(7.dp.toPx(), 11.dp.toPx()), Offset(15.dp.toPx(), 11.dp.toPx()), 1.2.dp.toPx())
        }
        DredgedRelic.BRASS_WARD_KEY -> {
          drawCircle(ink, radius = 4.dp.toPx(), center = Offset(7.dp.toPx(), cy), style = Stroke(1.4.dp.toPx()))
          drawLine(ink, Offset(11.dp.toPx(), cy), Offset(19.dp.toPx(), cy), 1.5.dp.toPx(), StrokeCap.Round)
          drawLine(ink, Offset(15.dp.toPx(), cy), Offset(15.dp.toPx(), cy + 3.5.dp.toPx()), 1.4.dp.toPx())
          drawLine(ink, Offset(18.dp.toPx(), cy), Offset(18.dp.toPx(), cy + 2.8.dp.toPx()), 1.4.dp.toPx())
        }
        DredgedRelic.BLEEDING_LOGBOOK -> {
          drawRect(ink, topLeft = Offset(4.dp.toPx(), 3.dp.toPx()), size = Size(14.dp.toPx(), 16.dp.toPx()), style = Stroke(1.4.dp.toPx()))
          drawLine(ink, Offset(7.dp.toPx(), 7.dp.toPx()), Offset(15.dp.toPx(), 7.dp.toPx()), 1.1.dp.toPx())
          drawLine(ink, Offset(7.dp.toPx(), 11.dp.toPx()), Offset(15.dp.toPx(), 11.dp.toPx()), 1.1.dp.toPx())
          drawLine(ink, Offset(7.dp.toPx(), 15.dp.toPx()), Offset(12.dp.toPx(), 15.dp.toPx()), 1.1.dp.toPx())
        }
      }
    }
  }
}
