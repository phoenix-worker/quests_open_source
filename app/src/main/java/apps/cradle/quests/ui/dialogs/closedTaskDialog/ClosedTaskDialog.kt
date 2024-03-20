package apps.cradle.quests.ui.dialogs.closedTaskDialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.databinding.DialogClosedTaskBinding
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.ui.adapters.ClosedTaskActionsAdapter
import apps.cradle.quests.ui.adapters.TaskItemsAdapter
import apps.cradle.quests.ui.dialogs.BaseDialog
import apps.cradle.quests.ui.fragments.tasks.newTask.NewTaskFragment
import apps.cradle.quests.utils.Locator
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.fullDateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class ClosedTaskDialog : BaseDialog() {

    private lateinit var binding: DialogClosedTaskBinding
    private val closedTaskDialogVM by viewModels<ClosedTaskDialogViewModel>()
    private val taskId by lazy { arguments?.getString(EXTRA_TASK_ID).orEmpty() }

    private var onDeleteTaskClicked: ((String) -> Unit)? = null

    fun setOnDeleteTaskClicked(action: ((String) -> Unit)?) {
        onDeleteTaskClicked = action
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogClosedTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        closedTaskDialogVM.loadTask(taskId)
        setObservers()
        setListeners()
    }

    private fun setObservers() {
        closedTaskDialogVM.task.observe(viewLifecycleOwner) { onTaskLoaded(it) }
        closedTaskDialogVM.actions.observe(viewLifecycleOwner) { onActionsLoaded(it) }
    }

    private fun setListeners() {
        binding.apply {
            resume.setOnClickListener {
                TasksUtils.resumeTask(taskId)
                dismiss()
            }
            copy.setOnClickListener {
                val bundle = Bundle()
                closedTaskDialogVM.task.value?.questId?.let { questId ->
                    bundle.putString(NewTaskFragment.EXTRA_QUEST_ID, questId)
                    bundle.putString(NewTaskFragment.EXTRA_COPYING_TASK_ID, taskId)
                    findNavController().navigate(R.id.action_global_newTaskFragment, bundle)
                    dismiss()
                }
            }
            delete.setOnClickListener {
                dismiss()
                onDeleteTaskClicked?.invoke(taskId)
            }
        }
    }

    private fun onTaskLoaded(task: TaskElement?) {
        if (task == null) {
            Toast.makeText(
                requireActivity(),
                getString(R.string.toastTaskNotFound),
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        } else {
            setupUI(task)
        }
    }

    private fun setupUI(task: TaskElement) {
        val dateString = TaskItemsAdapter.FinishedTaskElementVH.getTaskDateString(task)
        binding.date.text = getString(R.string.closedTaskDate, dateString)
        binding.title.text = task.title
        setDeadline(task)
        setQuestInfo(task)
    }

    private fun setQuestInfo(task: TaskElement) {
        binding.questInfo.text = task.questTitle
        binding.questInfo.setOnClickListener { Locator.sendOpenQuestEvent(task.questId) }
        binding.questInfo.isVisible = arguments?.getBoolean(EXTRA_IS_FROM_QUEST) != true
    }

    private fun setDeadline(task: TaskElement) {
        val dateFormat = SimpleDateFormat(fullDateFormat, Locale.getDefault())
        val dateString = dateFormat.format(Date(task.deadline))
        binding.deadline.text = getString(R.string.deadlineDate, dateString)
        binding.deadline.isVisible = task.deadline != DbTask.NO_DEADLINE
    }

    private fun onActionsLoaded(actions: List<String>) {
        binding.recyclerView.isVisible = actions.isNotEmpty()
        val adapter = ClosedTaskActionsAdapter(actions)
        binding.recyclerView.adapter = adapter
        val itemHeight = resources.getDimension(R.dimen.closedTaskActionHeight).toInt()
        val maxHeight = itemHeight * MAX_RV_ITEMS_HEIGHT
        val height = itemHeight * actions.size
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            min(height, maxHeight)
        )
        binding.recyclerView.layoutParams = params
        val padding = resources.getDimension(R.dimen.largeMargin).toInt()
        binding.recyclerView.setPadding(padding, 0, padding, 0)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_IS_FROM_QUEST = "extra_is_from_quest"

        const val MAX_RV_ITEMS_HEIGHT = 8
    }

}