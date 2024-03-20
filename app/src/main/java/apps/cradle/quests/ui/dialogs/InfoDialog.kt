package apps.cradle.quests.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import apps.cradle.quests.databinding.DialogInfoBinding

class InfoDialog(
    private val title: String,
    private val message: String
) : BaseDialog() {

    private lateinit var binding: DialogInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogInfoBinding.inflate(inflater, container, false)
        binding.run {
            title.text = this@InfoDialog.title
            message.text = this@InfoDialog.message
            button.setOnClickListener { dismiss() }
        }
        return binding.root
    }

}