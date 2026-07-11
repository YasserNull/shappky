package com.yn.shappky.ui.activities.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R

@Composable
fun MainFab(
  hasSelection: Boolean,
  onKillSelected: () -> Unit,
) {
  AnimatedVisibility(
    visible = hasSelection,
    enter = scaleIn() + fadeIn(),
    exit = scaleOut() + fadeOut(),
  ) {
    FloatingActionButton(
      onClick = onKillSelected,
      shape = CircleShape,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
      Icon(Icons.Outlined.Cancel, contentDescription = stringResource(R.string.force_stop_selected))
    }
  }
}
