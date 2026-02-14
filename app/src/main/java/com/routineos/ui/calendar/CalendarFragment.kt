package com.routineos.ui.calendar

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
    private var selectedDate = Calendar.getInstance()

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
            selectedDate = Calendar.getInstance()
            updateCalendar()
            filterEventsForDate(selectedDate)
        }
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { _ ->
            filterEventsForDate(selectedDate)
            updateCalendar()
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun filterEventsForDate(cal: Calendar) {
        val dateKey = dateKeyFormat.format(cal.time)
        val events = viewModel.events.value ?: emptyList()
        val filtered = events.filter { it.date == dateKey || it.type == "recurring" }.toList()
        eventAdapter.submitList(filtered)

        val dayFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
        binding.eventsHeaderText.text = "Events for ${dayFormat.format(cal.time)}"
    }

    private fun updateCalendar() {
        binding.monthYearText.text = monthFormat.format(displayedCalendar.time)
        buildCalendarGrid()
    }

    private fun buildCalendarGrid() {
        val grid = binding.calendarGrid
        grid.removeAllViews()

        val cal = displayedCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Monday=1 ... Sunday=7
        var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to Mon=0..Sun=6
        val startOffset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        grid.rowCount = rows

        val today = Calendar.getInstance()
        val todayKey = dateKeyFormat.format(today.time)
        val selectedKey = dateKeyFormat.format(selectedDate.time)
        val eventDates = (viewModel.events.value ?: emptyList()).map { it.date }.toSet()
        val cellSize = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density.toInt()) / 7

        for (i in 0 until rows * 7) {
            val dayNum = i - startOffset + 1
            val tv = TextView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = (36 * resources.displayMetrics.density).toInt()
                    columnSpec = GridLayout.spec(i % 7, 1f)
                    rowSpec = GridLayout.spec(i / 7)
                }
                gravity = Gravity.CENTER
                textSize = 13f
            }

            if (dayNum in 1..daysInMonth) {
                tv.text = dayNum.toString()

                val thisCal = displayedCalendar.clone() as Calendar
                thisCal.set(Calendar.DAY_OF_MONTH, dayNum)
                val thisKey = dateKeyFormat.format(thisCal.time)

                when {
                    thisKey == selectedKey -> {
                        tv.setBackgroundResource(R.drawable.badge_background)
                        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        tv.setTypeface(null, Typeface.BOLD)
                    }
                    thisKey == todayKey -> {
                        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.routine_purple))
                        tv.setTypeface(null, Typeface.BOLD)
                    }
                    eventDates.contains(thisKey) -> {
                        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.routine_purple_light))
                    }
                    else -> {
                        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    }
                }

                tv.setOnClickListener {
                    selectedDate = thisCal.clone() as Calendar
                    filterEventsForDate(selectedDate)
                    buildCalendarGrid()
                }
            } else {
                tv.text = ""
            }

            grid.addView(tv)
        }
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
                val dateKey = dateKeyFormat.format(selectedDate.time)

                // Calculate alarm timestamp from date + time
                var alarmTimestamp: Long? = null
                if (alarmEnabled && time != null) {
                    try {
                        val parts = time.split(":")
                        if (parts.size == 2) {
                            val alarmCal = selectedDate.clone() as Calendar
                            alarmCal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                            alarmCal.set(Calendar.MINUTE, parts[1].toInt())
                            alarmCal.set(Calendar.SECOND, 0)
                            alarmCal.set(Calendar.MILLISECOND, 0)
                            alarmTimestamp = alarmCal.timeInMillis
                        }
                    } catch (_: Exception) {}
                }

                val newEvent = Event(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = desc,
                    time = time,
                    type = type,
                    date = dateKey,
                    alarmEnabled = alarmEnabled,
                    alarmTimestamp = alarmTimestamp,
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
