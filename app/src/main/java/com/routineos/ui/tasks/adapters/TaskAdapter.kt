package com.routineos.ui.tasks.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.routineos.data.models.Task
import com.routineos.databinding.ItemTaskBinding

class TaskAdapter(
    private val onTaskAction: (Task, String) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.titleText.text = task.title
            binding.descriptionText.text = task.description
            
            // Update appearance based on completion status
            if (task.completed) {
                binding.titleText.alpha = 0.5f
                binding.descriptionText.alpha = 0.5f
                binding.completeButton.setImageResource(android.R.drawable.ic_menu_revert)
            } else {
                binding.titleText.alpha = 1.0f
                binding.descriptionText.alpha = 1.0f
                binding.completeButton.setImageResource(android.R.drawable.ic_menu_save)
            }

            // Show recurring badge
            binding.recurringBadge.visibility = if (task.type == "recurring") {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Set click listeners
            binding.completeButton.setOnClickListener {
                onTaskAction(task, "toggle")
            }

            binding.skipButton.setOnClickListener {
                onTaskAction(task, "skip")
            }

            binding.editButton.setOnClickListener {
                onTaskAction(task, "edit")
            }

            binding.deleteButton.setOnClickListener {
                onTaskAction(task, "delete")
            }

            binding.root.setOnClickListener {
                onTaskAction(task, "toggle")
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
