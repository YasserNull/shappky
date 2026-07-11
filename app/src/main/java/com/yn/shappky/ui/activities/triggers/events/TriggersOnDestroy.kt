package com.yn.shappky.ui.activities.triggers.events

import com.yn.shappky.ui.activities.triggers.TriggersActivity

fun TriggersActivity.handleOnDestroy() {
  executor.shutdownNow()
}
