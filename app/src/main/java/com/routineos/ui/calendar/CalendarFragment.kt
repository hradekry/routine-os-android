package com.routineos.ui.calendar

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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.routineos.R
import com.routineos.data.models.Event
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
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var displayedCalendar = Calendar.getInstance()

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
        updateCalendar()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            showDeleteEventDialog(event)
        }
        binding.eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }
    }

    private fun setupClickListeners() {
        binding.previousMonthButton.setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }
        binding.nextMonthButton.setOnClickListener {
            displayedCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
        binding.todayButton.setOnClickListener {
            displayedCalendar = Calendar.getInstance()
            updateCalendar()
        }
        binding.addEventButton.setOnClickListener {
            showAddEventDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            val todayKey = dateKeyFormat.format(Date())
            val todayEvents = events.filter { it.date == todayKey || it.type == "recurring" }
            eventAdapter.submitList(todayEvents)
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateCalendar() {
        binding.monthYearText.text = monthFormat.format(displayedCalendar.time)
    }

    fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.eventTitleInput)
        val descInput = dialogView.findViewById<EditText>(R.id.eventDescInput)
        val timeInput = dialogView.findViewById<EditText>(R.id.eventTimeInput)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.eventTypeGroup)
        val alarmSwitch = dialogView.findViewById<SwitchMaterial>(R.id.eventAlarmSwitch)

        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Add Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val desc = descInput.text.toString().trim()
                val time = timeInput.text.toString().trim().ifEmpty { null }
                val type = if (typeGroup.checkedRadioButtonId == R.id.typeRecurring) "recurring" else "onetime"
                val alarmEnabled = alarmSwitch.isChecked
                val todayKey = dateKeyFormat.format(Date())

                val newEvent = Event(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = desc,
                    time = time,
                    type = type,
                    date = todayKey,
                    alarmEnabled = alarmEnabled,
                    alarmTimestamp = null,
                    alarmNotified = false
                )
                val currentEvents = viewModel.events.value?.toMutableList() ?: mutableListOf()
                currentEvents.add(newEvent)
                viewModel.saveEvents(currentEvents)
                Toast.makeText(requireContext(), "Event added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteEventDialog(event: Event) {
        AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("Delete Event")
            .setMessage("Delete \"${event.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                val currentEvents = viewModel.events.value?.toMutableList() ?: mutableListOf()
                currentEvents.removeAll { it.id == event.id }
                viewModel.saveEvents(currentEvents)
                Toast.makeText(requireContext(), "Event deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
