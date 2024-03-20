package apps.cradle.quests.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbTask

class ReminderView : ChipGroup {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        inflate(context, R.layout.view_reminder, this)
        isSingleSelection = true
    }

    fun getReminder(): Long {
        return when (checkedChipId) {
            View.NO_ID -> DbTask.REMINDER_DEFAULT
            R.id.reminder5Min -> DbTask.REMINDER_5_MIN
            R.id.reminder10Min -> DbTask.REMINDER_10_MIN
            R.id.reminder15Min -> DbTask.REMINDER_15_MIN
            R.id.reminder30Min -> DbTask.REMINDER_30_MIN
            R.id.reminder1Hour -> DbTask.REMINDER_1_HOUR
            R.id.reminder2Hours -> DbTask.REMINDER_2_HOURS
            else -> throw IllegalArgumentException()
        }
    }

    fun setReminder(reminder: Long) {
        if (reminder == DbTask.REMINDER_DEFAULT) {
            children.forEach { (it as Chip).isChecked = false }
        } else check(
            when (reminder) {
                DbTask.REMINDER_5_MIN -> R.id.reminder5Min
                DbTask.REMINDER_10_MIN -> R.id.reminder10Min
                DbTask.REMINDER_15_MIN -> R.id.reminder15Min
                DbTask.REMINDER_30_MIN -> R.id.reminder30Min
                DbTask.REMINDER_1_HOUR -> R.id.reminder1Hour
                DbTask.REMINDER_2_HOURS -> R.id.reminder2Hours
                else -> throw IllegalArgumentException("Wrong reminder value.")
            }
        )
    }

}