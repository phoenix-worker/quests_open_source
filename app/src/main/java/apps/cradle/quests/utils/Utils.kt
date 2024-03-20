package apps.cradle.quests.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnWindowFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.App
import apps.cradle.quests.R
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import java.text.DecimalFormat
import java.util.*

const val EMPTY = ""

@Suppress("unused")
fun createSnackbar(
    anchorView: View,
    text: String?,
    buttonText: String? = null,
    onButtonClicked: (() -> Unit)? = null
): Snackbar {
    val snackbar = Snackbar.make(
        anchorView,
        text.toString(),
        Snackbar.LENGTH_SHORT
    )
    snackbar.setBackgroundTint(ContextCompat.getColor(App.instance, R.color.amber800))
    snackbar.setTextColor(ContextCompat.getColor(App.instance, android.R.color.white))
    snackbar.setActionTextColor(ContextCompat.getColor(App.instance, android.R.color.white))
    val textView =
        snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    textView.maxLines = 10
    if (buttonText != null) {
        snackbar.setAction(buttonText) {
            onButtonClicked?.invoke() ?: snackbar.dismiss()
        }
        snackbar.duration = Snackbar.LENGTH_INDEFINITE
    }
    return snackbar
}

val String.getValue: String
    get() = this

fun resetTimeInMillis(timeInMillis: Long): Long {
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = timeInMillis
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    return calendar.timeInMillis
}

fun withUtcOffset(timeInMillis: Long): Long {
    val timezone = TimeZone.getDefault()
    return timeInMillis + timezone.getOffset(timeInMillis)
}

fun withoutUtcOffset(timeInMillis: Long): Long {
    val timezone = TimeZone.getDefault()
    return timeInMillis - timezone.getOffset(timeInMillis)
}

const val fullDateFormat = "d MMMM yyyy"

class LinearRvItemDecorations(
    sideMarginsDimension: Int? = null,
    marginBetweenElementsDimension: Int? = null,
    private val drawTopMarginForFirstElement: Boolean = true
) : RecyclerView.ItemDecoration() {

    private val res = App.instance.resources
    private val sideMargins =
        if (sideMarginsDimension != null)
            res.getDimension(sideMarginsDimension).toInt()
        else 0
    private val verticalMargin =
        if (marginBetweenElementsDimension != null)
            res.getDimension(marginBetweenElementsDimension).toInt()
        else 0

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        outRect.set(
            sideMargins,
            if (drawTopMarginForFirstElement && position == 0) sideMargins else 0,
            sideMargins,
            if (position + 1 == parent.adapter?.itemCount) sideMargins else verticalMargin
        )
    }

}

fun showKeyboard(viewToFocus: View? = null, activity: FragmentActivity) {
    if (viewToFocus?.hasWindowFocus() == true) {
        showKeyboardOnWindowFocusedView(viewToFocus, activity)
    } else {
        val windowFocusChangeListener = object : OnWindowFocusChangeListener {
            override fun onWindowFocusChanged(p0: Boolean) {
                viewToFocus?.viewTreeObserver?.removeOnWindowFocusChangeListener(this)
                showKeyboardOnWindowFocusedView(viewToFocus, activity)
            }
        }
        viewToFocus?.viewTreeObserver?.addOnWindowFocusChangeListener(windowFocusChangeListener)
    }
}

private fun showKeyboardOnWindowFocusedView(viewToFocus: View? = null, activity: FragmentActivity) {
    viewToFocus?.setOnFocusChangeListener { view, hasFocus ->
        if (hasFocus) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE)
            (imm as InputMethodManager).showSoftInput(
                viewToFocus,
                InputMethodManager.SHOW_IMPLICIT
            )
            view.onFocusChangeListener = null
        }
    }
    viewToFocus?.requestFocus()
}

fun hideKeyboard(windowToken: IBinder, activity: FragmentActivity) {
    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun getDatePicker(
    currentSelection: Long,
    onPositiveButtonClicked: (Long) -> Unit
): MaterialDatePicker<Long> {
    val builder = MaterialDatePicker.Builder.datePicker()
    val selectedDate = withUtcOffset(currentSelection)
    builder.setSelection(selectedDate)
    val picker = builder.build()
    picker.addOnPositiveButtonClickListener {
        onPositiveButtonClicked.invoke(withoutUtcOffset(it))
    }
    return picker
}

fun getTimePicker(
    currentHour: Int,
    currentMinute: Int,
    onPositiveButtonClicked: (Long) -> Unit
): MaterialTimePicker {
    val builder = MaterialTimePicker.Builder()
    builder.setHour(currentHour)
    builder.setMinute(currentMinute)
    builder.setInputMode(INPUT_MODE_CLOCK)
    builder.setTimeFormat(CLOCK_24H)
    val picker = builder.build()
    picker.addOnPositiveButtonClickListener {
        onPositiveButtonClicked.invoke(hoursAndMinutesToMillis(picker.hour, picker.minute))
    }
    return picker
}

fun hoursAndMinutesToMillis(hours: Int, minutes: Int): Long {
    return (hours.toLong() * 60L + minutes.toLong()) * 60L * 1000L
}

fun getHoursFromMillis(timeMillis: Long): Int {
    return (timeMillis / (60L * 60L * 1000L)).toInt()
}

fun getMinutesFromMillis(timeMillis: Long): Int {
    val hourMillis = 60L * 60L * 1000L
    val minutesMillis = timeMillis % hourMillis
    return (minutesMillis / (60L * 1000L)).toInt()
}

val minutesFormat = DecimalFormat("00")
val hoursFormat = DecimalFormat("00")

fun formatTimeFromMillis(timeMillis: Long): String {
    val hours = getHoursFromMillis(timeMillis)
    val minutes = getMinutesFromMillis(timeMillis)
    return "${hoursFormat.format(hours)}:${minutesFormat.format(minutes)}"
}

fun getTomorrow(date: Date): Date {
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.time = date
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    return calendar.time
}

fun RecyclerView.addDefaultItemDecorations() {
    addItemDecoration(
        LinearRvItemDecorations(
            sideMarginsDimension = R.dimen.normalMargin,
            marginBetweenElementsDimension = R.dimen.smallMargin
        )
    )
}

fun <T> MutableLiveData<T>.toLiveData(): LiveData<T> = this

object UniqueIdGenerator {

    private var uniqueId = 0L

    fun nextId() = ++uniqueId

}

fun logException(exc: Exception) {
    if (App.LOG_ENABLED) Log.d(App.DEBUG_TAG, exc.toString())
}

fun <T> T?.orThrowIAE(message: String? = null): T {
    return this ?: throw IllegalArgumentException(message)
}

fun updateWidgets() {
    val context = App.instance.applicationContext
    val intent = Intent()
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    context.sendBroadcast(intent)
    App.log("Update widgets broadcast sent.")
}