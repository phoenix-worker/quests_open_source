package apps.cradle.quests.ui.dialogs

import android.animation.ValueAnimator
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.text.parseAsHtml
import apps.cradle.quests.databinding.DialogSlideToPerformBinding

class SlideToPerformDialog(
    private val titleText: String,
    private val messageText: String,
    private val action: String? = null,
    private val onDismissNotByButton: (() -> Unit)? = null,
    private val onSliderFinishedListener: (() -> Unit)
) : BaseDialog() {

    private lateinit var binding: DialogSlideToPerformBinding
    private var shouldInvokeOnDismissNotByButton = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSlideToPerformBinding.inflate(inflater, container, false)
        setDataToDialog()
        setListeners()
        return binding.root
    }

    private fun setDataToDialog() {
        binding.apply {
            title.text = titleText
            message.text = messageText.parseAsHtml()
            action?.run { actionText.text = this }
        }
    }

    private var canSendEvents = true

    private fun setListeners() {
        var oldProgress = binding.slider.progress
        binding.slider.apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (progress - 25 > oldProgress) {
                        setProgress(10)
                        binding.actionText.alpha = 1f
                        return
                    } else {
                        setProgress(progress)
                        oldProgress = progress
                    }
                    when (progress) {
                        11 -> binding.actionText.alpha = 0.9f
                        12 -> binding.actionText.alpha = 0.8f
                        13 -> binding.actionText.alpha = 0.7f
                        14 -> binding.actionText.alpha = 0.6f
                        15 -> binding.actionText.alpha = 0.5f
                        16 -> binding.actionText.alpha = 0.4f
                        17 -> binding.actionText.alpha = 0.3f
                        18 -> binding.actionText.alpha = 0.2f
                        19 -> binding.actionText.alpha = 0.1f
                        20 -> binding.actionText.alpha = 0.0f
                    }
                    if (progress < 10) setProgress(10)
                    if (progress > 20) binding.actionText.alpha = 0.0f
                    if (progress >= 91) {
                        if (canSendEvents) {
                            shouldInvokeOnDismissNotByButton = false
                            onSliderFinishedListener.invoke()
                            canSendEvents = false
                        }
                        this@SlideToPerformDialog.dismiss()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (seekBar != null) {
                        val animator = ValueAnimator.ofInt(seekBar.progress, 10)
                        animator.duration = 300L
                        animator.addUpdateListener {
                            seekBar.progress = it.animatedValue as Int
                        }
                        animator.start()
                    }
                }

            })
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (shouldInvokeOnDismissNotByButton) onDismissNotByButton?.invoke()
    }

}