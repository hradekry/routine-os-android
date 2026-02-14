package com.routineos.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.routineos.databinding.FragmentTasksBinding
import com.routineos.ui.tasks.adapters.TaskAdapter
import com.routineos.viewmodels.MainViewModel

class TasksFragment : Fragment() {
    
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var taskAdapter: TaskAdapter
    
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
                "delete" -> deleteTask(task)
                "edit" -> editTask(task)
            }
        }
        
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.addTaskButton.setOnClickListener {
            Toast.makeText(requireContext(), "Add Task", Toast.LENGTH_SHORT).show()
        }
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
    
    private fun toggleTask(task: com.routineos.data.models.Task) {
        val updatedTasks = viewModel.tasks.value?.toMutableList() ?: mutableListOf()
        val index = updatedTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            updatedTasks[index] = task.copy(completed = !task.completed)
            viewModel.saveTasks(updatedTasks)
        }
    }
    
    private fun skipTask(task: com.routineos.data.models.Task) {
        Toast.makeText(requireContext(), "Skip task: ${task.title}", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteTask(task: com.routineos.data.models.Task) {
        val updatedTasks = viewModel.tasks.value?.toMutableList() ?: mutableListOf()
        updatedTasks.removeIf { it.id == task.id }
        viewModel.saveTasks(updatedTasks)
    }
    
    private fun editTask(task: com.routineos.data.models.Task) {
        Toast.makeText(requireContext(), "Edit task: ${task.title}", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
