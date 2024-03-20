package apps.cradle.quests.ui.fragments.closeTask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentCloseTaskBinding
import apps.cradle.quests.ui.fragments.tasks.newTask.NewTaskFragment
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.orThrowIAE

class CloseTaskFragment : Fragment() {

    private lateinit var binding: FragmentCloseTaskBinding
    private val closeTaskVm by viewModels<CloseTaskViewModel>()

    private val taskId by lazy { arguments?.getString(EXTRA_TASK_ID).orThrowIAE("Wrong task id.") }
    private val questId by lazy {
        arguments?.getString(EXTRA_QUEST_ID).orThrowIAE("Wrong quest id.")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCloseTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
    }

    private fun setListeners() {
        binding.apply {
            finishAndCreateNew.setOnClickListener { goToNewTaskFragment() }
            finishAndCopy.setOnClickListener { onFinishAndCopyTask() }
            justFinish.setOnClickListener { closeTaskVm.finishTaskAndExit(taskId) }
        }
    }

    private fun onFinishAndCopyTask() {
        val bundle = Bundle()
        bundle.putString(NewTaskFragment.EXTRA_COPYING_TASK_ID, taskId)
        goToNewTaskFragment(bundle)
    }

    private fun goToNewTaskFragment(parameters: Bundle? = null) {
        val bundle = parameters ?: Bundle()
        bundle.putString(NewTaskFragment.EXTRA_CLOSING_TASK_ID, taskId)
        bundle.putString(NewTaskFragment.EXTRA_QUEST_ID, questId)
        findNavController().navigate(R.id.action_closeTaskFragment_to_newTaskFragment, bundle)
    }

    private fun setObservers() {
        closeTaskVm.run {
            exitEvent.observe(viewLifecycleOwner, onExitEvent)
        }
    }

    private val onExitEvent = EmptyEvent.Observer {
        findNavController().popBackStack()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_QUEST_ID = "quest_id"
    }
}