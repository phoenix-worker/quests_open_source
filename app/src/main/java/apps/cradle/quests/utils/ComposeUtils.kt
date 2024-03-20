package apps.cradle.quests.utils

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import apps.cradle.quests.App

object ComposeUtils {

    object TextSize {

        val HUGE: TextUnit
            get() {
                val width = App.instance.resources.configuration.smallestScreenWidthDp
                return when {
                    width < 360 -> 24.sp
                    width < 390 -> 30.sp
                    else -> 32.sp
                }
            }

        val LARGE: TextUnit
            get() {
                val width = App.instance.resources.configuration.smallestScreenWidthDp
                return when {
                    width < 360 -> 16.sp
                    width < 390 -> 18.sp
                    else -> 20.sp
                }
            }
    }
}