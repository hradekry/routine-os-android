package com.routineos.ui.tasks

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.routineos.R
import com.routineos.data.models.Task
import com.routineos.databinding.FragmentTasksBinding
import com.routineos.ui.tasks.adapters.TaskAdapter
import com.routineos.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter { task, action ->
            when (action) {
                "toggle" -> toggleTask(task)
                "skip" -> skipTask(task)
                "delete" -> showDeleteTaskDialog(task)
                "edit" -> showEditTaskDialog(task)
            }
        }
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun setupClickListeners() {
        // Add task is triggered via FAB in MainActivity
    }

    private fun observeViewModel() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.taskTitleInput)
        val descInput = dialogView.findViewById<EditText>(R.id.taskDescInput)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.taskTypeGroup)

        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val desc = descInput.text.toString().trim()
                val type = if (typeGroup.checkedRadioButtonId == R.id.taskTypeRecurring) "recurring" else "onetime"
                val todayKey = dateKeyFormat.format(Date())

                val newTask = Task(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = desc,
                    completed = false,
                    date = todayKey,
                    type = type,
                    skippedDates = emptyList(),
                    trackable = false,
                    trackTarget = null,
                    trackUnit = null,
                    trackIncrement = null,
                    trackProgress = emptyMap()
                )
                val currentTasks = viewModel.tasks.value?.toMutableList() ?: mutableListOf()
                currentTasks.add(newTask)
                viewModel.saveTasks(currentTasks)
                Toast.makeText(requireContext(), "Task added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.taskTitleInput)
        val descInput = dialogView.findViewById<EditText>(R.id.taskDescInput)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.taskTypeGroup)

        titleInput.setText(task.title)
        descInput.setText(task.description)
        if (task.type == "recurring") typeGroup.check(R.id.taskTypeRecurring)

        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isEmpty()) return@setPositiveButton
                val desc = descInput.text.toString().trim()
                val type = if (typeGroup.checkedRadioButtonId == R.id.taskTypeRecurring) "recurring" else "onetime"

                val updatedTasks = viewModel.tasks.value?.toMutableList() ?: mutableListOf()
                val index = updatedTasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    updatedTasks[index] = task.copy(title = title, description = desc, type = type)
                    viewModel.saveTasks(updatedTasks)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteTaskDialog(task: Task) {
        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Delete Task")
            .setMessage("Delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                val updatedTasks = viewModel.tasks.value?.toMutableList() ?: mutableListOf()
                updatedTasks.removeAll { it.id == task.id }
                viewModel.saveTasks(updatedTasks)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleTask(task: Task) {
        val updatedTasks = viewModel.tasks.value?.toMutableList() ?: mutableListOf()
        val index = updatedTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            updatedTasks[index] = task.copy(completed = !task.completed)
            viewModel.saveTasks(updatedTasks)
        }
    }

    private fun skipTask(task: Task) {
        val todayKey = dateKeyFormat.format(Date())
        val updatedTasks = viewModel.tasks.value?.toMutableList() ?: mutableListOf()
        val index = updatedTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            val newSkipped = task.skippedDates.toMutableList().apply { add(todayKey) }
            updatedTasks[index] = task.copy(skippedDates = newSkipped)
            viewModel.saveTasks(updatedTasks)
            Toast.makeText(requireContext(), "Task skipped for today", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
