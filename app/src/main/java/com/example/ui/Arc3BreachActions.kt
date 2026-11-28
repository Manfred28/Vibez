package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.game.Arc3UnmakeAct

/**
 * Grouped immutable action contract for Arc 3 ("The Unmaking").
 * Encapsulates counter-clockwise wrenching, 18 chamber-dismantling Unmaking acts, urgent wall
 * rupture tearing, Memory Wedge locking, and 5 interactive Final Pulls.
 */
@Immutable
data class Arc3BreachActions(
  val onWrenchBreach: () -> Unit = {},
  val onPerformUnmake: (Arc3UnmakeAct) -> Unit = {},
  val onTearRupture: (Long) -> Unit = {},
  val onDriveWedge: () -> Unit = {},
  val onPerformFinalPull: () -> Unit = {},
)
