package apps.cradle.quests.ui.fragments.schedule

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentScheduleBinding
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.models.TaskItem
import apps.cradle.quests.ui.adapters.TaskItemsAdapter
import apps.cradle.quests.ui.adapters.TaskItemsAdapter.MODE
import apps.cradle.quests.ui.dialogs.SelectQuestDialog
import apps.cradle.quests.ui.fragments.closeTask.CloseTaskFragment
import apps.cradle.quests.ui.fragments.moveTask.MoveTaskFragment
import apps.cradle.quests.ui.fragments.notes.newNote.NewNoteFragment
import apps.cradle.quests.ui.fragments.tasks.newTask.NewTaskFragment
import apps.cradle.quests.ui.fragments.tasks.task.TaskFragment
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.addDefaultItemDecorations
import apps.cradle.quests.utils.events.EventObserver
import apps.cradle.quests.utils.resetTimeInMillis

class ScheduleFragment : Fragment() {

    private lateinit var binding: FragmentScheduleBinding
    private val scheduleVM: ScheduleViewModel by activityViewModels()
    private lateinit var itemsAdapter: TaskItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScheduleBinding.inflate(inflater, container, false)
        setupRecyclerView()
        return binding.root
    }

    @Suppress("DEPRECATION")
    private fun setupSystemUi() {
        val flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        requireActivity().window.decorView.systemUiVisibility = flags
        requireActivity().window.statusBarColor = Color.parseColor("#00000000")
    }

    @Suppress("DEPRECATION")
    private fun resetSystemUi() {
        val flags = View.SYSTEM_UI_FLAG_VISIBLE
        requireActivity().window.decorView.systemUiVisibility = flags
        val statusBarColor = ContextCompat.getColor(requireActivity(), R.color.windowBackground)
        requireActivity().window.statusBarColor = statusBarColor
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setListener()
    }

    override fun onStart() {
        super.onStart()
        setupSystemUi()
        scheduleVM.updateSchedule()
    }

    override fun onStop() {
        super.onStop()
        resetSystemUi()
    }

    private fun setObservers() {
        scheduleVM.run {
            taskItems.observe(viewLifecycleOwner) { onTasksItemsChanged(it) }
            scrollToBottomEvent.observe(viewLifecycleOwner, onScrollToBottomEvent)
            menuItemEvent.observe(viewLifecycleOwner, onMenuItemEvent)
        }
    }

    private fun setListener() {
        binding.fabScrollDown.setOnClickListener { scheduleVM.sendScrollToBottomEvent() }
    }

    private val onScrollToBottomEvent = EventObserver<Boolean> { smoothScroll ->
        scheduleVM.taskItems.value?.size?.let {
            if (it > 0) {
                if (smoothScroll) binding.recyclerView.smoothScrollToPosition(it - 1)
                else binding.recyclerView.scrollToPosition(it - 1)
            }
        }
    }

    private val onMenuItemEvent = EventObserver<ScheduleViewModel.MenuItemEvent> {
        when (it) {
            ScheduleViewModel.MenuItemEvent.QUICK_TASK -> onQuickTaskEvent()
            ScheduleViewModel.MenuItemEvent.QUICK_NOTE -> onQuickNoteEvent()
            ScheduleViewModel.MenuItemEvent.DATABASE -> onDatabaseEvent()
        }
    }

    private fun showSelectQuestDialogForCreation(
        title: String,
        buttonText: String,
        onQuestSelected: (String) -> Unit
    ) {
        SelectQuestDialog().apply {
            customizeDialog(
                title = title,
                buttonText = buttonText,
                emptyViewTitle = this@ScheduleFragment.getString(R.string.sqdTransferEmptyViewTitle),
                emptyViewHint = this@ScheduleFragment.getString(R.string.sqdTransferEmptyViewHint),
            )
            setOnQuestSelected(onQuestSelected)
        }.show(childFragmentManager, "select_quest_dialog")
    }

    private fun onQuickTaskEvent() {
        showSelectQuestDialogForCreation(
            getString(R.string.sqdCreationTaskTitle),
            getString(R.string.sqdCreationTaskButtonText)
        ) { questId ->
            val bundle = Bundle()
            bundle.putString(NewTaskFragment.EXTRA_QUEST_ID, questId)
            findNavController().navigate(
                R.id.action_scheduleFragment_to_newTaskFragment,
                bundle
            )
        }
    }

    private fun onQuickNoteEvent() {
        showSelectQuestDialogForCreation(
            getString(R.string.sqdCreationNoteTitle),
            getString(R.string.sqdCreationNoteButtonText)
        ) { questId ->
            val bundle = Bundle()
            bundle.apply { putString(NewNoteFragment.EXTRA_QUEST_ID, questId) }
            findNavController().navigate(R.id.action_scheduleFragment_to_newNoteFragment, bundle)
        }
    }

    private fun onDatabaseEvent() {
        findNavController().navigate(R.id.action_global_databaseFragment)
    }

    private var isFirstLayout = true

    private fun onTasksItemsChanged(taskItems: List<TaskItem>) {
        if (taskItems.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.root.visibility = View.VISIBLE
        } else {
            binding.emptyView.root.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
        (binding.recyclerView.adapter as TaskItemsAdapter).submitList(taskItems)
        if (isFirstLayout) {
            isFirstLayout = false
            binding.recyclerView.scrollToPosition(taskItems.size - 1)
        }
    }

    private fun setupRecyclerView() {
        if (!::itemsAdapter.isInitialized)
            itemsAdapter = TaskItemsAdapter(
                mode = MODE.SCHEDULE,
                onTaskClicked = onTaskClicked,
                onTaskLongClicked = onTaskLongClicked,
                scheduleVM = scheduleVM,
                onFinishedTaskClicked = {}
            )
        binding.recyclerView.run {
            addDefaultItemDecorations()
            adapter = itemsAdapter
            val itemTouchHelper = ItemTouchHelper(
                TouchHelperCallback(this@ScheduleFragment, itemsAdapter)
            )
            itemTouchHelper.attachToRecyclerView(this)
            addOnScrollListener(onScrollListener)
        }
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val adapter = binding.recyclerView.adapter as TaskItemsAdapter
            val manager = (binding.recyclerView.layoutManager as LinearLayoutManager)
            val position = manager.findLastVisibleItemPosition()
            when {
                adapter.itemCount - position > ITEMS_TO_SCROLL_DOWN -> {
                    if (!binding.fabScrollDown.isShown) binding.fabScrollDown.show()
                }

                else -> {
                    if (!binding.fabScrollDown.isOrWillBeHidden) binding.fabScrollDown.hide()
                }
            }
        }
    }

    private val onTaskClicked: (String) -> Unit = { taskId ->
        val bundle = Bundle()
        bundle.putString(TaskFragment.EXTRA_TASK_ID, taskId)
        findNavController().navigate(R.id.action_scheduleFragment_to_taskFragment, bundle)
    }

    private val onTaskLongClicked: (String, String) -> Unit = { currentQuestId, taskId ->
        SelectQuestDialog().apply {
            setCurrentQuestId(currentQuestId)
            setOnQuestSelected { newQuestId ->
                TasksUtils.changeTaskQuest(taskId, newQuestId)
                scheduleVM.updateSchedule()
            }
            customizeDialog(
                title = this@ScheduleFragment.getString(R.string.cqdTransferTitle),
                buttonText = this@ScheduleFragment.getString(R.string.cqdTransferButtonText),
                emptyViewTitle = this@ScheduleFragment.getString(R.string.cqdTransferEmptyViewTitle),
                emptyViewHint = this@ScheduleFragment.getString(R.string.cqdTransferEmptyViewHint),
            )
        }.show(childFragmentManager, "change_quest_dialog")
    }

    inner class TouchHelperCallback(
        private val fragment: Fragment,
        private val adapter: TaskItemsAdapter
    ) : ItemTouchHelper.SimpleCallback(0, 0) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = true

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val task = adapter.currentList[viewHolder.adapterPosition] as TaskElement
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    findNavController().navigate(
                        R.id.action_scheduleFragment_to_closeTaskFragment,
                        Bundle().apply {
                            putString(CloseTaskFragment.EXTRA_TASK_ID, task.id)
                            putString(CloseTaskFragment.EXTRA_QUEST_ID, task.questId)
                        }
                    )
                }

                ItemTouchHelper.LEFT -> {
                    fragment.findNavController().navigate(
                        R.id.action_scheduleFragment_to_moveTaskFragment,
                        Bundle().apply { putString(MoveTaskFragment.EXTRA_TASK_ID, task.id) }
                    )
                }
            }
        }

        override fun getSwipeDirs(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return when (viewHolder) {
                is TaskItemsAdapter.TaskElementVH -> {
                    val adapter = (recyclerView.adapter as TaskItemsAdapter)
                    val task = adapter.currentList[viewHolder.adapterPosition] as TaskElement
                    val taskDate = resetTimeInMillis(task.date)
                    val today = resetTimeInMillis(System.currentTimeMillis())
                    val isFutureTask = taskDate > today
                    val both = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                    val left = ItemTouchHelper.LEFT
                    when {
                        isFutureTask -> left
                        task.actionsCount == 0 -> both
                        task.doneActionsCount == task.actionsCount -> both
                        else -> left
                    }
                }

                else -> 0
            }
        }

    }

    companion object {
        private const val ITEMS_TO_SCROLL_DOWN = 30
    }

}