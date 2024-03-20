package apps.cradle.quests.ui.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbAction
import apps.cradle.quests.databinding.RvActionBinding
import apps.cradle.quests.models.Action

class ActionsAdapter(
    private val onCheckBoxClicked: ((Action) -> Unit)?,
    private val onDeleteClicked: (String) -> Unit
) : ListAdapter<Action, ActionsAdapter.ActionVH>(ActionDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionVH {
        return ActionVH.getViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ActionVH, position: Int) {
        holder.bind(getItem(position), onCheckBoxClicked, onDeleteClicked)
    }

    class ActionVH(
        private val binding: RvActionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val white = ContextCompat.getColor(App.instance, android.R.color.white)
        private val hint = ContextCompat.getColor(App.instance, R.color.textHintWhite)

        private var animator: AnimatorSet? = null

        fun bind(
            action: Action,
            onCheckBoxClicked: ((Action) -> Unit)?,
            onDeleteClicked: (String) -> Unit
        ) {
            binding.run {
                title.imeOptions = EditorInfo.IME_ACTION_DONE
                title.setRawInputType(EditorInfo.TYPE_CLASS_TEXT)
                title.setText(action.title)
                title.doOnTextChanged { text, _, _, _ ->
                    action.title =
                        if (text?.isBlank() == true) action.title else text.toString()
                }
                checkbox.setOnClickListener { onCheckBoxClicked?.invoke(action) }
                when (action.state) {
                    DbAction.STATE_ACTIVE -> {
                        checkbox.setImageDrawable(null)
                        checkbox.setBackgroundResource(R.drawable.background_action_checkbox_unchecked)
                        title.setTextColor(white)
                    }

                    DbAction.STATE_FINISHED -> {
                        checkbox.setImageResource(R.drawable.ic_done)
                        checkbox.setBackgroundResource(R.drawable.background_action_checkbox_checked)
                        title.setTextColor(hint)
                    }
                }
                delete.setOnClickListener { onDeleteClicked.invoke(action.id) }
                if (onCheckBoxClicked != null) title.setOnClickListener { showDeleteButton() }
                else binding.delete.isVisible = true
                if (onCheckBoxClicked == null) binding.checkbox.isVisible = false
            }
        }

        private fun showDeleteButton() {
            if (animator == null) {
                val show = ObjectAnimator.ofFloat(0f, 1f).apply {
                    addUpdateListener { binding.delete.alpha = it.animatedValue as Float }
                    start()
                }
                val hide = ObjectAnimator.ofFloat(1f, 0f).apply {
                    addUpdateListener { binding.delete.alpha = it.animatedValue as Float }
                    startDelay = 3000L
                    start()
                }
                animator = AnimatorSet().apply {
                    playSequentially(show, hide)
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            binding.delete.alpha = 0f
                            binding.delete.isInvisible = false
                            binding.delete.isClickable = true
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            binding.delete.isInvisible = true
                            binding.delete.isClickable = false
                            animator = null
                        }
                    })
                    start()
                }
            }
        }

        companion object {

            fun getViewHolder(parent: ViewGroup): ActionVH {
                val binding = RvActionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ActionVH(binding)
            }

        }

    }

    class ActionDiffUtilCallback : DiffUtil.ItemCallback<Action>() {

        override fun areItemsTheSame(oldItem: Action, newItem: Action): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Action, newItem: Action): Boolean {
            return oldItem == newItem
        }

    }

}