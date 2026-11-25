package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.game.SpecimenJar
import kotlin.math.exp

internal val FOUNDATIONAL_SHELF_JARS: List<SpecimenJar> =
  listOf(
    SpecimenJar.YESTERDAYS_DUST,
    SpecimenJar.UNSPENT_BREATH,
    SpecimenJar.ALMOST_A_NAME,
  )

internal val DEEP_SHELF_JARS: List<SpecimenJar> =
  listOf(
    SpecimenJar.SALT_FROM_THE_GROUT,
    SpecimenJar.A_BORROWED_PULSE,
    SpecimenJar.THE_UNCOUNTED_HOUR,
  )

/**
 * Splits owned Specimen Jars into the left foundational wall shelf and right deep wall shelf
 * in canonical order. Mirrored 1:1 on Web (`partitionChamberShelfJars` in `ui.js`).
 */
internal fun partitionChamberShelfJars(
  ownedJars: Set<SpecimenJar>,
): Pair<List<SpecimenJar>, List<SpecimenJar>> {
  val left = FOUNDATIONAL_SHELF_JARS.filter { it in ownedJars }
  val right = DEEP_SHELF_JARS.filter { it in ownedJars }
  return left to right
}

/**
 * Twin recessed dark cast-iron specimen ledges flanking the lower chamber wall beside the floor sump.
 * Left ledge holds the 3 foundational jars (which persist eerily in an emptied room under Catatonic Tide);
 * right ledge holds the 3 deeper/post-Severance jars. Non-interactive and seated behind anomalies.
 */
@Composable
internal fun ChamberSpecimenShelves(
  ownedJars: Set<SpecimenJar>,
  isStillnessActive: Boolean,
  breathPhaseProvider: () -> Float,
  arterialPulsePhaseProvider: () -> Float,
  modifier: Modifier = Modifier,
) {
  if (ownedJars.isEmpty()) return
  val (leftJars, rightJars) = partitionChamberShelfJars(ownedJars)

  Box(modifier = modifier.fillMaxSize()) {
    if (leftJars.isNotEmpty()) {
      SpecimenWallLedge(
        jars = leftJars,
        isStillnessActive = isStillnessActive,
        breathPhaseProvider = breathPhaseProvider,
        arterialPulsePhaseProvider = arterialPulsePhaseProvider,
        shelfTag = "chamber_jar_shelf_left",
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp),
      )
    }
    if (rightJars.isNotEmpty()) {
      SpecimenWallLedge(
        jars = rightJars,
        isStillnessActive = isStillnessActive,
        breathPhaseProvider = breathPhaseProvider,
        arterialPulsePhaseProvider = arterialPulsePhaseProvider,
        shelfTag = "chamber_jar_shelf_right",
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 5.dp),
      )
    }
  }
}

@Composable
private fun SpecimenWallLedge(
  jars: List<SpecimenJar>,
  isStillnessActive: Boolean,
  breathPhaseProvider: () -> Float,
  arterialPulsePhaseProvider: () -> Float,
  shelfTag: String,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val ledgeWidth = (jars.size * 27 + 12).dp

  Column(
    modifier = modifier.testTag(shelfTag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.Bottom,
      modifier = Modifier.padding(horizontal = 4.dp),
    ) {
      jars.forEach { jar ->
        ChamberShelfJarSpecimen(
          jar = jar,
          isStillnessActive = isStillnessActive,
          breathPhaseProvider = breathPhaseProvider,
          arterialPulsePhaseProvider = arterialPulsePhaseProvider,
        )
      }
    }

    // Heavy dark cast-iron bracket shelf lip with corner rivets & cast drop shadow
    Canvas(modifier = Modifier.width(ledgeWidth).height(8.dp)) {
      // Soft shadow beneath iron ledge
      drawRoundRect(
        color = Color(0xFF040505).copy(alpha = 0.85f),
        topLeft = Offset(2.dp.toPx(), 3.dp.toPx()),
        size = Size(size.width - 4.dp.toPx(), 5.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
      )
      // Forged iron top plate
      drawRoundRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0xFF2B2F2D),
                Color(0xFF151717),
                Color(0xFF090A0A),
              )
          ),
        topLeft = Offset.Zero,
        size = Size(size.width, 4.5.dp.toPx()),
        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
      )
      // Faint damp cold top edge glint
      drawLine(
        color =
          if (isStillnessActive) colorScheme.primary.copy(alpha = 0.40f)
          else Color(0xFFC8CBC3).copy(alpha = 0.22f),
        start = Offset(2.dp.toPx(), 0.7.dp.toPx()),
        end = Offset(size.width - 2.dp.toPx(), 0.7.dp.toPx()),
        strokeWidth = 0.8.dp.toPx(),
      )
      // Twin wall mounting brackets & rivets
      val bracketInset = 6.dp.toPx()
      for (bx in listOf(bracketInset, size.width - bracketInset)) {
        drawRect(
          color = Color(0xFF1A1D1C),
          topLeft = Offset(bx - 1.5.dp.toPx(), 4.dp.toPx()),
          size = Size(3.dp.toPx(), 4.dp.toPx()),
        )
        drawCircle(
          color = Color(0xFF474C49).copy(alpha = 0.7f),
          radius = 1.dp.toPx(),
          center = Offset(bx, 2.3.dp.toPx()),
        )
      }
    }
  }
}

@Composable
private fun ChamberShelfJarSpecimen(
  jar: SpecimenJar,
  isStillnessActive: Boolean,
  breathPhaseProvider: () -> Float,
  arterialPulsePhaseProvider: () -> Float,
) {
  val colorScheme = MaterialTheme.colorScheme
  val isTallSeal = jar == SpecimenJar.THE_UNCOUNTED_HOUR
  val isPulseJar = jar == SpecimenJar.A_BORROWED_PULSE
  val jarWidth = if (isTallSeal) 28.dp else 25.dp
  val jarHeight = if (isTallSeal) 40.dp else 35.dp

  Box(
    modifier =
      Modifier.size(width = jarWidth, height = jarHeight)
        .testTag("chamber_jar_${jar.id}")
        .graphicsLayer {
          if (isPulseJar) {
            val arterial = arterialPulsePhaseProvider()
            val lub = exp(-((arterial - 0.22f) * (arterial - 0.22f)) / 0.006f)
            val s = 1f + lub * 0.045f
            scaleX = s
            scaleY = s
          }
        },
    contentAlignment = Alignment.BottomCenter,
  ) {
    // Contact shadow grounding the jar base onto the iron shelf + subtle internal aura
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawOval(
        color = Color(0xFF040505).copy(alpha = 0.85f),
        topLeft = Offset(size.width * 0.14f, size.height - 4.dp.toPx()),
        size = Size(size.width * 0.72f, 4.5.dp.toPx()),
      )
      if (isPulseJar) {
        val arterial = arterialPulsePhaseProvider()
        val lub = exp(-((arterial - 0.22f) * (arterial - 0.22f)) / 0.008f)
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  colorScheme.tertiary.copy(alpha = 0.18f + lub * 0.30f),
                  Color.Transparent,
                ),
              center = Offset(size.width * 0.5f, size.height * 0.58f),
              radius = size.width * 0.72f,
            ),
          radius = size.width * 0.72f,
          center = Offset(size.width * 0.5f, size.height * 0.58f),
        )
      } else if (isStillnessActive) {
        val breath = breathPhaseProvider()
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  colorScheme.primary.copy(alpha = 0.10f + breath * 0.12f),
                  Color.Transparent,
                ),
              center = Offset(size.width * 0.5f, size.height * 0.54f),
              radius = size.width * 0.65f,
            ),
          radius = size.width * 0.65f,
          center = Offset(size.width * 0.5f, size.height * 0.54f),
        )
      }
    }

    // Feathered photographic specimen jar blending seamlessly into the chamber gloom
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(bottom = 1.dp)
          .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
          .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
      Image(
        painter = painterResource(id = specimenJarDrawableRes(jar)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      // Elliptical edge feather mask dissolves the dark photographic margins seamlessly into the wall
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
          brush =
            Brush.radialGradient(
              colorStops =
                arrayOf(
                  0.0f to Color.Black,
                  0.66f to Color.Black,
                  0.88f to Color.Black.copy(alpha = 0.52f),
                  1.0f to Color.Transparent,
                ),
              center = Offset(size.width * 0.5f, size.height * 0.53f),
              radius = size.maxDimension * 0.56f,
            ),
          blendMode = BlendMode.DstIn,
        )
      }
    }
  }
}
