package com.routineos.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.routineos.databinding.FragmentCalendarBinding
import com.routineos.ui.calendar.adapters.EventAdapter
import com.routineos.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {
    
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Initialize calendar
        updateCalendar()
    }
    
    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            // Handle event click
            Toast.makeText(requireContext(), "Event: ${event.title}", Toast.LENGTH_SHORT).show()
        }
        
        binding.eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.previousMonthButton.setOnClickListener {
            // Navigate to previous month
            updateCalendar()
        }
        
        binding.nextMonthButton.setOnClickListener {
            // Navigate to next month
            updateCalendar()
        }
        
        binding.todayButton.setOnClickListener {
            // Navigate to today
            updateCalendar()
        }
        
        binding.addEventButton.setOnClickListener {
            // Show add event dialog
            Toast.makeText(requireContext(), "Add Event", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            eventAdapter.submitList(events)
            
            // Update calendar with event indicators
            updateCalendar()
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun updateCalendar() {
        val calendar = Calendar.getInstance()
        binding.monthYearText.text = dateFormat.format(calendar.time)
        
        // Generate calendar days (simplified)
        val days = mutableListOf<String>()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (day in 1..daysInMonth) {
            days.add(day.toString())
        }
        
        // Update calendar grid (you would implement a proper calendar grid here)
        // For now, just show the current month
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
