package apps.cradle.quests.ui.fragments.tasks.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.databinding.FragmentTaskBinding
import apps.cradle.quests.models.Action
import apps.cradle.quests.ui.adapters.ActionsAdapter
import apps.cradle.quests.ui.dialogs.EnterValueDialog
import apps.cradle.quests.ui.dialogs.SlideToPerformDialog
import apps.cradle.quests.ui.fragments.quests.quest.QuestFragment
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.addDefaultItemDecorations
import apps.cradle.quests.utils.fullDateFormat
import apps.cradle.quests.utils.getDatePicker
import apps.cradle.quests.utils.getHoursFromMillis
import apps.cradle.quests.utils.getMinutesFromMillis
import apps.cradle.quests.utils.getTimePicker
import apps.cradle.quests.utils.minutesFormat
import apps.cradle.quests.utils.resetTimeInMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskFragment : Fragment() {

    private lateinit var binding: FragmentTaskBinding
    private val taskVM by viewModels<TaskViewModel>()
    private var sheetColor = 0
    private var backgroundColor = 0
    private val taskId by lazy { arguments?.getString(EXTRA_TASK_ID).orEmpty() }
    private val dateFormat = SimpleDateFormat(fullDateFormat, Locale.getDefault())
    private val isFromQuest by lazy { arguments?.getBoolean(EXTRA_IS_FROM_QUEST) ?: false }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskBinding.inflate(inflater, container, false)
        sheetColor = ContextCompat.getColor(requireActivity(), R.color.sheetBackground)
        backgroundColor = ContextCompat.getColor(requireActivity(), R.color.windowBackground)
        setupRecyclerView()
        binding.chipGroup.isVisible = false
        binding.toolbar.menu.removeItem(R.id.done)
        QuestFragment.setupTitle(binding.title)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
        taskVM.initialize(taskId)
    }

    override fun onStart() {
        super.onStart()
        requireActivity().window.statusBarColor = sheetColor
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.statusBarColor = backgroundColor
        saveChanges()
    }

    private fun setListeners() {
        binding.run {
            toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            toolbar.setOnMenuItemClickListener(onMenuItemClicked)
            date.setOnClickListener { onDateButtonClicked() }
            deadline.setOnClickListener { onDeadlineButtonClicked() }
            clearDeadline.setOnClickListener { onClearDeadlineClicked() }
            time.setOnClickListener { onTimeButtonClicked() }
            clearTime.setOnClickListener { onClearTimeClicked() }
            fab.setOnClickListener {
                onAddActionClicked(childFragmentManager) { taskVM.addActions(it) }
            }
        }
    }

    private fun onClearDeadlineClicked() {
        taskVM.changeDeadline(0L)
    }

    private fun onClearTimeClicked() {
        taskVM.changeTime(DbTask.NO_TIME)
        binding.reminder.setReminder(DbTask.REMINDER_DEFAULT)
    }

    private fun setObservers() {
        taskVM.run {
            task.observe(viewLifecycleOwner) { onTaskChanged(it) }
            date.observe(viewLifecycleOwner) { onDateChanged(it) }
            time.observe(viewLifecycleOwner) { onTimeChanged(it) }
            quest.observe(viewLifecycleOwner) { onQuestChanged(it) }
            actions.observe(viewLifecycleOwner) { onActionsChanged(it) }
            deadline.observe(viewLifecycleOwner) { onDeadlineChanged(it) }
            exitEvent.observe(viewLifecycleOwner) { findNavController().popBackStack() }
        }
    }

    private fun onDeadlineChanged(deadline: Long?) {
        deadline?.let { date ->
            if (date == 0L) {
                binding.deadline.text = getString(R.string.setDeadline)
                binding.clearDeadline.visibility = View.INVISIBLE
            } else {
                val dateString = dateFormat.format(Date(date))
                binding.deadline.text = getString(R.string.deadlineDate, dateString)
                binding.clearDeadline.visibility = View.VISIBLE
            }
        }
    }

    private fun onQuestChanged(quest: DbQuest?) {
        quest?.let {
            binding.quest.run {
                text = it.title
                if (isFromQuest) setOnClickListener { findNavController().popBackStack() }
                else setOnClickListener { _ ->
                    val bundle = Bundle()
                    bundle.putString(QuestFragment.EXTRA_QUEST_ID, it.id)
                    findNavController().navigate(R.id.action_taskFragment_to_questFragment, bundle)
                }
            }
        }
    }

    private fun onDateChanged(dateMillis: Long) {
        val dateString = dateFormat.format(Date(dateMillis))
        binding.date.text = getString(R.string.whenDate, dateString)
    }

    private fun onTimeChanged(timeMillis: Long) {
        if (timeMillis == DbTask.NO_TIME) {
            binding.time.text = getString(R.string.buttonTime)
            binding.clearTime.visibility = View.INVISIBLE
            binding.reminder.visibility = View.GONE
        } else {
            binding.time.text = getString(
                R.string.buttonTimeWithArgs,
                getHoursFromMillis(timeMillis).toString(),
                minutesFormat.format(getMinutesFromMillis(timeMillis))
            )
            binding.clearTime.visibility = View.VISIBLE
            binding.reminder.visibility = View.VISIBLE
        }
    }

    private fun onDateButtonClicked() {
        val currentDate = taskVM.date.value ?: System.currentTimeMillis()
        getDatePicker(currentDate) {
            taskVM.changeDate(it)
        }.show(childFragmentManager, "date_picker_dialog")
    }

    private fun onTimeButtonClicked() {
        val currentTime = taskVM.time.value ?: 0L
        getTimePicker(getHoursFromMillis(currentTime), getMinutesFromMillis(currentTime)) {
            taskVM.changeTime(it)
        }.show(childFragmentManager, "time_picker_dialog")
    }

    private fun onDeadlineButtonClicked() {
        val today = resetTimeInMillis(System.currentTimeMillis())
        val deadline = taskVM.deadline.value ?: today
        val currentSelection = if (deadline == 0L) today else deadline
        getDatePicker(currentSelection) {
            taskVM.changeDeadline(it)
        }.show(childFragmentManager, "date_picker_dialog")
    }

    private fun onTaskChanged(task: DbTask?) {
        if (task == null) {
            Toast.makeText(
                requireActivity(),
                getString(R.string.toastTaskNotFound),
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }
        task?.let {
            if (task.state == DbTask.STATE_FINISHED) binding.toolbar.title =
                getString(R.string.finishedTask)
            else binding.toolbar.title = getString(R.string.activeTask)
            binding.title.setText(task.title)
            binding.reminder.setReminder(task.reminder)
            if (task.questId == DbQuest.HEAP_QUEST_ID) binding.quest.visibility = View.GONE
        }
    }

    private fun saveChanges() {
        taskVM.task.value?.let {
            val title = binding.title.text.toString()
            val maxLength = resources.getInteger(R.integer.maxTitleTextLength)
            val shouldRename = title != it.title && title.isNotBlank() && title.length <= maxLength
            if (shouldRename) taskVM.renameTask(title)
            if (taskVM.date.value != taskVM.task.value?.date) taskVM.updateTaskDate()
            if (taskVM.time.value != taskVM.task.value?.time) taskVM.updateTaskTime()
            if (taskVM.deadline.value != taskVM.task.value?.deadline) taskVM.updateTaskDeadline()
            val reminder = binding.reminder.getReminder()
            if (reminder != taskVM.task.value?.reminder) taskVM.updateTaskReminder(reminder)
            taskVM.updateTaskActions(it.id)
        }
    }

    private val onMenuItemClicked: (MenuItem) -> Boolean = {
        when (it.itemId) {
            R.id.delete -> onDeleteMenuItemClicked()
        }
        true
    }

    private fun onDeleteMenuItemClicked() {
        SlideToPerformDialog(
            titleText = getString(R.string.deleteQuickTaskDialogTitle),
            messageText = getString(R.string.deleteQuickTaskDialogMessage),
            action = getString(R.string.delete),
            onSliderFinishedListener = {
                TasksUtils.deleteTaskAndAllItsData(taskId)
                taskVM.sendExitEvent()
            }
        ).show(childFragmentManager, "delete_task_dialog")
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = ActionsAdapter(
            onCheckBoxClicked = { taskVM.toggleAction(it) },
            onDeleteClicked = { taskVM.deleteAction(it) }
        )
        binding.recyclerView.addDefaultItemDecorations()
    }

    private fun onActionsChanged(actions: List<Action>?) {
        actions?.let { list ->
            (binding.recyclerView.adapter as ActionsAdapter).submitList(list)
            if (list.isEmpty()) {
                binding.taskActionsHintNotEmpty.visibility = View.GONE
                binding.taskActionsHint.visibility = View.VISIBLE
            } else {
                binding.taskActionsHint.visibility = View.GONE
                binding.taskActionsHintNotEmpty.visibility = View.VISIBLE
            }
        }
    }

    companion object {

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_IS_FROM_QUEST = "extra_is_from_quest"

        fun onAddActionClicked(fm: FragmentManager, onActionCreated: (String) -> Unit) {
            val res = App.instance.resources
            EnterValueDialog(
                titleText = res.getString(R.string.addActionDialogTitle),
                messageText = res.getString(R.string.addActionDialogMessage),
                hintText = res.getString(R.string.addActionDialogHint),
                negativeButtonText = res.getString(R.string.buttonCancel),
                positiveButtonText = res.getString(R.string.buttonAdd),
                onPositiveButtonClicked = onActionCreated
            ).show(fm, "add_action_dialog")
        }
    }
}