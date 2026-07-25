package com.yassernull.shappky.ui.activities.addTrigger

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.ui.activities.addTrigger.events.handleOnCreate

class AddTriggerActivity : ComponentActivity() {
  internal var triggerId: String? = null
  internal var initialTrigger: TriggerModel? = null
  internal var triggerCount: Int = 0

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(com.yassernull.shappky.core.managers.LocaleManager.getLanguageContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOnCreate(savedInstanceState)
  }

  companion object {
    const val EXTRA_TRIGGER_ID = "extra_trigger_id"
  }
}
