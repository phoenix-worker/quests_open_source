package apps.cradle.quests.ui.fragments.quests.quest

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentTasksBinding
import apps.cradle.quests.models.DividerElement
import apps.cradle.quests.models.TaskItem
import apps.cradle.quests.ui.adapters.TaskItemsAdapter
import apps.cradle.quests.ui.dialogs.ConfirmationDialog
import apps.cradle.quests.ui.dialogs.SelectQuestDialog
import apps.cradle.quests.ui.dialogs.SlideToPerformDialog
import apps.cradle.quests.ui.dialogs.closedTaskDialog.ClosedTaskDialog
import apps.cradle.quests.ui.fragments.tasks.task.TaskFragment
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.logException

class TasksFragment : Fragment() {

    private lateinit var binding: FragmentTasksBinding
    private val questVM by activityViewModels<QuestViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTasksBinding.inflate(inflater, container, false)
        setupEmptyView()
        setupRecyclerView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
    }

    private fun setObservers() {
        questVM.tasks.observe(viewLifecycleOwner) { onTasksChanged(it) }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.addItemDecoration(ItemDecoration())
        binding.recyclerView.adapter =
            TaskItemsAdapter(
                TaskItemsAdapter.MODE.QUEST,
                onTaskClicked,
                onTaskLongClicked = { currentQuestId, taskId ->
                    SelectQuestDialog().apply {
                        setCurrentQuestId(currentQuestId)
                        setOnQuestSelected { newQuestId ->
                            TasksUtils.changeTaskQuest(taskId, newQuestId)
                            questVM.update(currentQuestId)
                        }
                        customizeDialog(
                            title = this@TasksFragment.getString(R.string.cqdTransferTitle),
                            buttonText = this@TasksFragment.getString(R.string.cqdTransferButtonText),
                            emptyViewTitle = this@TasksFragment.getString(R.string.cqdTransferEmptyViewTitle),
                            emptyViewHint = this@TasksFragment.getString(R.string.cqdTransferEmptyViewHint),
                        )
                    }.show(childFragmentManager, "change_quest_dialog")
                },
                onDividerClicked = { questVM.switchShowFinishedTasks() },
                onArchiveClicked = onArchiveClicked,
                onFinishedTaskClicked = onFinishedTaskClicked
            )
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                questVM.sendContentScrolling(newState != RecyclerView.SCROLL_STATE_IDLE)
            }
        })
    }

    private val onFinishedTaskClicked: (String) -> Unit = { taskId ->
        if (!questVM.isQuestArchived) {
            val bundle = Bundle()
            bundle.putString(ClosedTaskDialog.EXTRA_TASK_ID, taskId)
            bundle.putBoolean(ClosedTaskDialog.EXTRA_IS_FROM_QUEST, true)
            ClosedTaskDialog().apply {
                arguments = bundle
                setOnDeleteTaskClicked(onDeleteClosedTaskClicked)
            }.show(childFragmentManager, "closed_task_dialog")
        }
    }

    private val onDeleteClosedTaskClicked: (String) -> Unit = { taskId ->
        SlideToPerformDialog(
            titleText = getString(R.string.deleteQuickTaskDialogTitle),
            messageText = getString(R.string.deleteQuickTaskDialogMessage),
            action = getString(R.string.delete),
            onSliderFinishedListener = {
                TasksUtils.deleteTaskAndAllItsData(taskId)
            }
        ).show(childFragmentManager, "delete_task_dialog")
    }

    private val onArchiveClicked: (String) -> Unit = { questId ->
        ConfirmationDialog(
            title = getString(R.string.archiveQuestDialogTitle),
            message = getString(R.string.archiveQuestDialogMessage),
            positiveButtonText = getString(R.string.buttonYes),
            negativeButtonText = getString(R.string.buttonCancel),
            onPositiveButtonClicked = {
                questVM.archiveQuestAndExit(questId)
            }
        ).show(childFragmentManager, "archive_dialog")
    }

    private fun onTasksChanged(data: List<TaskItem>?) {
        data?.let { taskItems ->
            when {
                taskItems.isEmpty() -> {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.root.visibility = View.VISIBLE
                }

                else -> {
                    (binding.recyclerView.adapter as TaskItemsAdapter).submitList(taskItems)
                    binding.emptyView.root.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private val onTaskClicked: (String) -> Unit = { questId ->
        val bundle = Bundle()
        bundle.putString(TaskFragment.EXTRA_TASK_ID, questId)
        bundle.putBoolean(TaskFragment.EXTRA_IS_FROM_QUEST, true)
        findNavController().navigate(R.id.action_questFragment_to_taskFragment, bundle)
    }

    private class ItemDecoration : RecyclerView.ItemDecoration() {

        private val res = App.instance.resources
        val normalMargin = res.getDimension(R.dimen.normalMargin).toInt()
        val smallMargin = res.getDimension(R.dimen.smallMargin).toInt()

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            recyclerView: RecyclerView,
            state: RecyclerView.State
        ) {
            try {
                val adapter = recyclerView.adapter as TaskItemsAdapter
                val position = recyclerView.getChildAdapterPosition(view)
                val itemsCount = adapter.currentList.size
                val isLast = position == itemsCount - 1
                var left = normalMargin
                var top = if (position == 0) normalMargin else smallMargin
                var right = normalMargin
                var bottom = if (isLast) normalMargin else 0
                if (adapter.currentList[position] is DividerElement) {
                    top = normalMargin
                    bottom = normalMargin - smallMargin
                    right = 0
                    left = 0
                }
                outRect.set(left, top, right, bottom)
            } catch (exc: Exception) {
                logException(exc)
            }
        }

    }

    private fun setupEmptyView() {
        val isHeap = arguments?.getBoolean(QuestFragment.EXTRA_IS_HEAP) ?: false
        binding.emptyView.run {
            image.setImageResource(R.drawable.image_empty)
            title.text = getString(R.string.emptyViewTitle)
            hint.text = getString(
                if (!isHeap) R.string.tasksFragmentEmptyViewHint
                else R.string.heapTasksFragmentEmptyViewHint
            )
        }
    }
}