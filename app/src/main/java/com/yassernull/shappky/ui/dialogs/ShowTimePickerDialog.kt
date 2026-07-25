package com.yassernull.shappky.ui.dialogs

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@Composable
fun ShowTimePickerDialog(
  onDismiss: () -> Unit,
  onConfirm: (Int, Int) -> Unit,
) {
  val context = LocalContext.current
  val calendar = Calendar.getInstance()
  val timePickerDialog = remember {
    TimePickerDialog(
      context,
      { _, selectedHour, selectedMinute ->
        onConfirm(selectedHour, selectedMinute)
      },
      calendar.get(Calendar.HOUR_OF_DAY),
      calendar.get(Calendar.MINUTE),
      DateFormat.is24HourFormat(context),
    ).apply {
      setOnCancelListener { onDismiss() }
    }
  }
  timePickerDialog.show()
}
