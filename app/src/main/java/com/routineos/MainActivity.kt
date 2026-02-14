package com.routineos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.routineos.databinding.ActivityMainBinding
import com.routineos.ui.calendar.CalendarFragment
import com.routineos.ui.coach.CoachFragment
import com.routineos.ui.tasks.TasksFragment
import com.routineos.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            viewModel.onPermissionsGranted()
        } else {
            Toast.makeText(this, "Some permissions denied. Alarms may not work properly.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        observeViewModel()
        checkAndRequestPermissions()
        
        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(CalendarFragment())
        }
    }
    
    private var currentTab = R.id.nav_calendar

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            currentTab = item.itemId
            when (item.itemId) {
                R.id.nav_calendar -> {
                    loadFragment(CalendarFragment())
                    binding.titleText.text = getString(R.string.calendar)
                    binding.subtitleText.text = getString(R.string.today)
                    binding.fab.isVisible = true
                    true
                }
                R.id.nav_tasks -> {
                    loadFragment(TasksFragment())
                    binding.titleText.text = getString(R.string.tasks)
                    binding.subtitleText.text = getString(R.string.today)
                    binding.fab.isVisible = true
                    true
                }
                R.id.nav_coach -> {
                    loadFragment(CoachFragment())
                    binding.titleText.text = getString(R.string.coach)
                    binding.subtitleText.text = "Active"
                    binding.fab.isVisible = false
                    true
                }
                else -> false
            }
        }

        binding.fab.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (fragment) {
                is CalendarFragment -> fragment.showAddEventDialog()
                is TasksFragment -> fragment.showAddTaskDialog()
            }
        }

        // Set default selection
        binding.bottomNavigation.selectedItemId = R.id.nav_calendar
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun observeViewModel() {
        viewModel.currentContext.observe(this) { context ->
            when (context) {
                "calendar" -> binding.bottomNavigation.selectedItemId = R.id.nav_calendar
                "tasks" -> binding.bottomNavigation.selectedItemId = R.id.nav_tasks
                "coach" -> binding.bottomNavigation.selectedItemId = R.id.nav_coach
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!com.routineos.services.AlarmScheduler.requestExactAlarmPermission(this)) {
                // This permission needs to be granted via system settings
                Toast.makeText(this, "Please enable 'Alarms & reminders' permission in system settings", Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            viewModel.onPermissionsGranted()
        }
    }
    
}
