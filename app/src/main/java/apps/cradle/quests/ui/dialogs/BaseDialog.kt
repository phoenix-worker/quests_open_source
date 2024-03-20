package apps.cradle.quests.ui.dialogs

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import apps.cradle.quests.R

open class BaseDialog : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

}