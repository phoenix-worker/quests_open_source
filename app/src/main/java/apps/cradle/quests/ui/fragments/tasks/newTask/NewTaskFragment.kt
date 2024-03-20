package apps.cradle.quests.ui.fragments.tasks.newTask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.databinding.FragmentTaskBinding
import apps.cradle.quests.models.Action
import apps.cradle.quests.ui.adapters.ActionsAdapter
import apps.cradle.quests.ui.dialogs.ConfirmationDialog
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getDayAfterTomorrow
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getFourWeeksLater
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getNextMonday
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getNextMonth
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getNextYear
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getToday
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getTomorrow
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getTwoWeeksLater
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskViewModel.Companion.getWeekLater
import apps.cradle.quests.ui.fragments.quests.quest.QuestFragment
import apps.cradle.quests.ui.fragments.tasks.task.TaskFragment
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.addDefaultItemDecorations
import apps.cradle.quests.utils.events.EventObserver
import apps.cradle.quests.utils.fullDateFormat
import apps.cradle.quests.utils.getDatePicker
import apps.cradle.quests.utils.getHoursFromMillis
import apps.cradle.quests.utils.getMinutesFromMillis
import apps.cradle.quests.utils.getTimePicker
import apps.cradle.quests.utils.hideKeyboard
import apps.cradle.quests.utils.minutesFormat
import apps.cradle.quests.utils.orThrowIAE
import apps.cradle.quests.utils.resetTimeInMillis
import apps.cradle.quests.utils.showKeyboard
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewTaskFragment : Fragment() {

    private lateinit var binding: FragmentTaskBinding
    private val newTaskVM: NewTaskViewModel by viewModels()
    private val dateFormat = SimpleDateFormat(fullDateFormat, Locale.getDefault())
    private lateinit var args: Bundle
    private val questId by lazy { args.getString(EXTRA_QUEST_ID).orThrowIAE("Wrong questId.") }
    private val closingTaskId by lazy { args.getString(EXTRA_CLOSING_TASK_ID) }
    private val copyingTaskId by lazy { args.getString(EXTRA_COPYING_TASK_ID) }
    private var prevStatusBarColor = 0
    private var sheetColor = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prevStatusBarColor = requireActivity().window.statusBarColor
        sheetColor = ContextCompat.getColor(requireActivity(), R.color.sheetBackground)
        args = arguments.orThrowIAE("Empty arguments.")
        binding = FragmentTaskBinding.inflate(inflater, container, false)
        setupRecyclerView()
        binding.quest.isVisible = false
        binding.toolbar.menu.removeItem(R.id.delete)
        binding.toolbar.title = getString(R.string.fragmentNewTaskTitle)
        QuestFragment.setupTitle(binding.title)
        showKeyboard(binding.title, requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setListeners()
        newTaskVM.loadQuestTitle(questId)
        if (copyingTaskId != null) copyingTaskId?.let(newTaskVM::loadTaskToCopy)
    }

    override fun onStart() {
        super.onStart()
        requireActivity().window.statusBarColor = sheetColor
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showSubmitExitDialog()
        }
    }

    private fun showSubmitExitDialog() {
        ConfirmationDialog(
            title = getString(R.string.dialogSubmitExitNewTaskTitle),
            message = getString(R.string.dialogSubmitExitNewTaskMessage),
            positiveButtonText = getString(R.string.buttonExit),
            negativeButtonText = getString(R.string.buttonCancel),
            onPositiveButtonClicked = { newTaskVM.sendExitEvent() }
        ).show(childFragmentManager, "submit_exit_dialog")
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.statusBarColor = prevStatusBarColor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity().window.decorView.windowToken, requireActivity())
    }

    private fun setObservers() {
        newTaskVM.run {
            date.observe(viewLifecycleOwner) { onDateChanged(it) }
            time.observe(viewLifecycleOwner) { onTimeChanged(it) }
            deadline.observe(viewLifecycleOwner) { onDeadlineChanged(it) }
            taskToCopy.observe(viewLifecycleOwner, onTaskToCopyLoaded)
            questTitle.observe(viewLifecycleOwner) { onQuestTitleLoaded(it) }
            actions.observe(viewLifecycleOwner) { onActionsChanged(it) }
            exitEvent.observe(viewLifecycleOwner) { findNavController().popBackStack() }
        }
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

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = ActionsAdapter(
            onCheckBoxClicked = null,
            onDeleteClicked = { newTaskVM.deleteAction(it) }
        )
        binding.recyclerView.addDefaultItemDecorations()
    }

    private fun onQuestTitleLoaded(questTitle: String) {
        binding.toolbar.subtitle = questTitle
    }

    private val onTaskToCopyLoaded = EventObserver<DbTask> {
        binding.title.setText(it.title)
        hideKeyboard(binding.title.windowToken, requireActivity())
        newTaskVM.changeDate(it.date)
        newTaskVM.changeTime(it.time)
        binding.reminder.setReminder(it.reminder)
        newTaskVM.changeDeadline(it.deadline)
    }

    private fun setListeners() {
        binding.run {
            toolbar.setNavigationOnClickListener { showSubmitExitDialog() }
            toolbar.setOnMenuItemClickListener(onMenuItemClicked)
            title.doOnTextChanged { text, _, _, _ -> onTaskTitleTextChanged(text) }
            date.setOnClickListener { onDateButtonClicked() }
            time.setOnClickListener { onTimeButtonClicked() }
            clearTime.setOnClickListener { onClearTimeClicked() }
            deadline.setOnClickListener { onDeadlineButtonClicked() }
            clearDeadline.setOnClickListener { onClearDeadlineClicked() }
            setDateChipsListeners()
            fab.setOnClickListener {
                TaskFragment.onAddActionClicked(childFragmentManager) {
                    newTaskVM.addActions(it)
                }
            }
        }
    }

    private val onMenuItemClicked: (MenuItem) -> Boolean = {
        when (it.itemId) {
            R.id.done -> onCreateButtonClicked()
        }
        true
    }

    private fun onTimeChanged(timeMillis: Long) {
        if (timeMillis == DbTask.NO_TIME) {
            binding.time.text = getString(R.string.buttonTime)
            binding.reminder.setReminder(DbTask.REMINDER_DEFAULT)
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

    private fun onClearTimeClicked() {
        newTaskVM.changeTime(DbTask.NO_TIME)
    }

    private fun onTimeButtonClicked() {
        val currentTime = newTaskVM.time.value ?: 0L
        getTimePicker(getHoursFromMillis(currentTime), getMinutesFromMillis(currentTime)) {
            newTaskVM.changeTime(it)
        }.show(childFragmentManager, "time_picker_dialog")
    }

    private fun setDateChipsListeners() {
        binding.run {
            chipToday.enableListener(getToday())
            chipTomorrow.enableListener(getTomorrow())
            chipDayAfterTomorrow.enableListener(getDayAfterTomorrow())
            chipWeekLater.enableListener(getWeekLater())
            chipTwoWeeksLater.enableListener(getTwoWeeksLater())
            chipFourWeeksLater.enableListener(getFourWeeksLater())
            chipNextMonday.enableListener(getNextMonday())
            chipNextMonth.enableListener(getNextMonth())
            chipNextYear.enableListener(getNextYear())
        }
    }

    private fun setDateChipsSelection(dateMillis: Long) {
        removeDateChipsListeners()
        for (child in binding.chipGroup.children) {
            (child as Chip).apply {
                isChecked = false
                isClickable = true
            }
        }
        val chipToCheck = when (resetTimeInMillis(dateMillis)) {
            getToday() -> binding.chipToday
            getTomorrow() -> binding.chipTomorrow
            getDayAfterTomorrow() -> binding.chipDayAfterTomorrow
            getWeekLater() -> binding.chipWeekLater
            getTwoWeeksLater() -> binding.chipTwoWeeksLater
            getFourWeeksLater() -> binding.chipFourWeeksLater
            getNextMonday() -> binding.chipNextMonday
            getNextMonth() -> binding.chipNextMonth
            getNextYear() -> binding.chipNextYear
            else -> null
        }
        chipToCheck?.apply {
            isChecked = true
            isClickable = false
        }
        setDateChipsListeners()
    }

    private fun Chip.enableListener(dateMs: Long) {
        setOnCheckedChangeListener { chip, checked ->
            onChipCheckedChange(chip, checked, dateMs)
        }
    }

    private fun removeDateChipsListeners() {
        binding.run {
            chipToday.setOnCheckedChangeListener(null)
            chipTomorrow.setOnCheckedChangeListener(null)
            chipDayAfterTomorrow.setOnCheckedChangeListener(null)
            chipWeekLater.setOnCheckedChangeListener(null)
            chipTwoWeeksLater.setOnCheckedChangeListener(null)
            chipFourWeeksLater.setOnCheckedChangeListener(null)
            chipNextMonday.setOnCheckedChangeListener(null)
            chipNextMonth.setOnCheckedChangeListener(null)
            chipNextYear.setOnCheckedChangeListener(null)
        }
    }

    private fun onChipCheckedChange(chip: CompoundButton, checked: Boolean, chipDate: Long) {
        for (child in binding.chipGroup.children) child.isClickable = true
        if (checked) {
            chip.isClickable = false
            newTaskVM.changeDate(chipDate)
        }
    }

    private fun onCreateButtonClicked() {
        if (binding.title.text.isNullOrBlank()) {
            binding.titleInputLayout.helperText = getString(R.string.errorNoTextInEditText)
            return
        }
        binding.title.text.toString()
        newTaskVM.createNewTask(
            questId = questId,
            title = binding.title.text.toString(),
            reminder = binding.reminder.getReminder()
        )
        closingTaskId?.let { taskId -> if (taskId.isNotBlank()) TasksUtils.finishTask(taskId) }
        findNavController().popBackStack()
    }

    private fun onTaskTitleTextChanged(text: CharSequence?) {
        if (!text.isNullOrBlank()) binding.titleInputLayout.helperText = null
    }

    private fun onDateChanged(dateMillis: Long) {
        val dateString = dateFormat.format(Date(dateMillis))
        binding.date.text = getString(R.string.whenDate, dateString)
        setDateChipsSelection(dateMillis)
    }

    private fun onDateButtonClicked() {
        val currentDate = newTaskVM.date.value ?: System.currentTimeMillis()
        getDatePicker(currentDate) {
            newTaskVM.changeDate(it)
        }.show(childFragmentManager, "date_picker_dialog")
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

    private fun onDeadlineButtonClicked() {
        val today = resetTimeInMillis(System.currentTimeMillis())
        val deadline = newTaskVM.deadline.value ?: today
        val currentSelection = if (deadline == 0L) today else deadline
        getDatePicker(currentSelection) {
            newTaskVM.changeDeadline(it)
        }.show(childFragmentManager, "date_picker_dialog")
    }

    private fun onClearDeadlineClicked() {
        newTaskVM.changeDeadline(0L)
    }

    companion object {
        const val EXTRA_QUEST_ID = "extra_quest_id"
        const val EXTRA_CLOSING_TASK_ID = "extra_closing_task_id"
        const val EXTRA_COPYING_TASK_ID = "extra_should_copy_task"
    }

}