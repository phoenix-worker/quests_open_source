package apps.cradle.quests.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import apps.cradle.quests.R
import apps.cradle.quests.databinding.DialogEnterValueBinding
import apps.cradle.quests.utils.hideKeyboard
import apps.cradle.quests.utils.showKeyboard

@Suppress("unused")
class EnterValueDialog(
    private val titleText: String,
    private val messageText: String,
    private val hintText: String,
    private val negativeButtonText: String,
    private val positiveButtonText: String,
    private val initialInput: String? = null,
    private val onPositiveButtonClicked: (String) -> Unit,
) : BaseDialog() {

    private lateinit var binding: DialogEnterValueBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogEnterValueBinding.inflate(inflater, container, false)
        setFields()
        setListeners()
        showKeyboard(binding.textInputEditText, requireActivity())
        return binding.root
    }

    private fun setListeners() {
        binding.run {
            negativeButton.setOnClickListener { dismiss() }
            positiveButton.setOnClickListener { onValueEntered() }
            textInputEditText.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    onValueEntered()
                    true
                } else false
            }
        }
    }

    private fun onValueEntered() {
        binding.textInputEditText.text?.run {
            val maxLength = resources.getInteger(R.integer.maxTitleTextLength)
            if (isNotBlank() && length <= maxLength) {
                onPositiveButtonClicked.invoke(this.toString())
                dismiss()
            }
        }
    }

    private fun setFields() {
        binding.run {
            title.text = titleText
            message.text = messageText
            textInputLayout.hint = hintText
            negativeButton.text = negativeButtonText
            positiveButton.text = positiveButtonText
            initialInput?.let { textInputEditText.setText(it) }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        hideKeyboard(requireActivity().window.decorView.windowToken, requireActivity())
        super.onDismiss(dialog)
    }

}