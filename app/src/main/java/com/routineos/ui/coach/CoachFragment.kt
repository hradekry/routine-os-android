package com.routineos.ui.coach

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.routineos.databinding.FragmentCoachBinding
import com.routineos.ui.coach.adapters.CoachMessageAdapter
import com.routineos.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class CoachFragment : Fragment() {
    
    private var _binding: FragmentCoachBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var messageAdapter: CoachMessageAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoachBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Add welcome message
        addCoachMessage("Welcome to The Coach. I'm here to help you stay disciplined and focused.", "coach")
    }
    
    private fun setupRecyclerView() {
        messageAdapter = CoachMessageAdapter()
        
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
        }
    }
    
    private fun sendMessage() {
        val message = binding.messageInput.text?.toString()?.trim() ?: ""
        if (message.isNotEmpty()) {
            processCommand(message)
            binding.messageInput.setText("")
        }
    }

    private fun setupClickListeners() {
        binding.sendButton.setOnClickListener { sendMessage() }

        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
        
        binding.progressButton.setOnClickListener {
            processCommand("progress")
        }
        
        binding.recommendButton.setOnClickListener {
            processCommand("recommend")
        }
        
        binding.motivateButton.setOnClickListener {
            processCommand("motivate me")
        }
        
        binding.exportButton.setOnClickListener {
            exportData()
        }
        
        binding.importButton.setOnClickListener {
            Toast.makeText(requireContext(), "Import feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                addCoachMessage("Error: $it", "error")
                viewModel.clearError()
            }
        }
    }
    
    private fun processCommand(command: String) {
        addCoachMessage(command, "user")
        
        // Process command (simplified version)
        when {
            command.contains("progress") -> {
                val tasks = viewModel.tasks.value ?: emptyList()
                val completed = tasks.count { it.completed }
                val total = tasks.size
                addCoachMessage("Progress: $completed/$total tasks completed today. Keep pushing!", "coach")
            }
            command.contains("recommend") -> {
                addCoachMessage("Focus on completing your most important task first. Quality over quantity.", "coach")
            }
            command.contains("motivate") -> {
                addCoachMessage("Remember: Discipline is choosing between what you want now and what you want most. Stay focused!", "motivate")
            }
            else -> {
                addCoachMessage("I understand you want to: $command. Keep working on your goals!", "coach")
            }
        }
    }
    
    private fun addCoachMessage(message: String, type: String) {
        messageAdapter.addMessage(message, type)
        binding.messagesRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
    }
    
    private fun exportData() {
        lifecycleScope.launch {
            try {
                val data = viewModel.exportData()
                addCoachMessage("Data exported successfully! Copy this data to backup:\n\n$data", "coach")
            } catch (e: Exception) {
                addCoachMessage("Failed to export data: ${e.message}", "error")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
