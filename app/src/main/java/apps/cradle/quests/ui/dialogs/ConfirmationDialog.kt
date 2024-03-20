package apps.cradle.quests.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import apps.cradle.quests.databinding.DialogConfirmationBinding

class ConfirmationDialog(
    private val title: String,
    private val message: String,
    private val positiveButtonText: String? = null,
    private val negativeButtonText: String? = null,
    private val onPositiveButtonClicked: () -> Unit,
    private val onNegativeButtonClicked: (() -> Unit)? = null,
    private val onDismissNotByButton: (() -> Unit)? = null
) : BaseDialog() {

    private lateinit var binding: DialogConfirmationBinding
    private var shouldInvokeOnDismissNotByButton = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogConfirmationBinding.inflate(inflater, container, false)
        binding.run {
            title.text = this@ConfirmationDialog.title
            message.text = this@ConfirmationDialog.message
            if (positiveButtonText != null) buttonPositive.text = positiveButtonText
            if (negativeButtonText != null) buttonNegative.text = negativeButtonText
            buttonPositive.setOnClickListener {
                shouldInvokeOnDismissNotByButton = false
                onPositiveButtonClicked.invoke()
                dismiss()
            }
            buttonNegative.setOnClickListener {
                shouldInvokeOnDismissNotByButton = false
                onNegativeButtonClicked?.invoke()
                dismiss()
            }
        }
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (shouldInvokeOnDismissNotByButton) onDismissNotByButton?.invoke()
    }

}