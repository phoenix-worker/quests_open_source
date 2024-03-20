package apps.cradle.quests.ui.fragments.moveTask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentMoveTaskBinding
import apps.cradle.quests.utils.getDatePicker
import apps.cradle.quests.utils.resetTimeInMillis
import java.util.*

class MoveTaskFragment : Fragment() {

    private lateinit var binding: FragmentMoveTaskBinding
    private val moveTaskVM: MoveTaskViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMoveTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val taskId = arguments?.getString(EXTRA_TASK_ID)
        if (taskId == null) {
            Toast.makeText(
                requireActivity(),
                getString(R.string.toastWrongTaskId),
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        } else moveTaskVM.initializeTask(taskId)
        setupUI()
        setListeners()
        setObservers()
    }

    private fun setupUI() {
        val today = resetTimeInMillis(Date().time)
        val taskDate = resetTimeInMillis(moveTaskVM.task.date)
        if (taskDate == today) binding.today.visibility = View.GONE
        else binding.today.visibility = View.VISIBLE
    }

    private fun setListeners() {
        binding.run {
            today.setOnClickListener { moveTaskVM.moveTaskToToday() }
            tomorrow.setOnClickListener { moveTaskVM.moveTaskToTomorrow() }
            dayAfterTomorrow.setOnClickListener { moveTaskVM.moveTaskToDayAfterTomorrow() }
            nextWeek.setOnClickListener { moveTaskVM.moveTaskToNextWeek() }
            anotherDate.setOnClickListener {
                getDatePicker(resetTimeInMillis(moveTaskVM.task.date)) {
                    moveTaskVM.moveTaskToDate(it)
                }.show(childFragmentManager, "date_picker_dialog")
            }
        }
    }

    private fun setObservers() {
        moveTaskVM.taskMovedEvent.observe(viewLifecycleOwner) { findNavController().popBackStack() }
    }

    companion object {

        const val EXTRA_TASK_ID = "extra_task_id"

    }

}