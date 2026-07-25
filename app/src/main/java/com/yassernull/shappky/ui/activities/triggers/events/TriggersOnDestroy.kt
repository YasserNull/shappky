package com.yassernull.shappky.ui.activities.triggers.events

import com.yassernull.shappky.ui.activities.triggers.TriggersActivity

fun TriggersActivity.handleOnDestroy() {
  executor.shutdownNow()
}
