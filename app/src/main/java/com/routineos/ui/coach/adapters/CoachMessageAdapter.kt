package com.routineos.ui.coach.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.routineos.databinding.ItemCoachMessageBinding

data class CoachMessage(
    val text: String,
    val type: String, // "user", "coach", "motivate", "error"
    val timestamp: String = ""
)

class CoachMessageAdapter : ListAdapter<CoachMessage, CoachMessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemCoachMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun addMessage(text: String, type: String) {
        val message = CoachMessage(text, type, java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
        submitList(currentList + message)
    }

    inner class MessageViewHolder(
        private val binding: ItemCoachMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: CoachMessage) {
            binding.messageText.text = message.text
            binding.timestampText.text = message.timestamp

            // Set appearance based on message type
            when (message.type) {
                "user" -> {
                    binding.messageCard.setCardBackgroundColor(com.routineos.R.color.routine_purple)
                    binding.messageText.setTextColor(itemView.context.getColor(com.routineos.R.color.black))
                }
                "coach" -> {
                    binding.messageCard.setCardBackgroundColor(com.routineos.R.color.dark_surface)
                    binding.messageText.setTextColor(itemView.context.getColor(com.routineos.R.color.white))
                }
                "motivate" -> {
                    binding.messageCard.setCardBackgroundColor(com.routineos.R.color.success_green)
                    binding.messageText.setTextColor(itemView.context.getColor(com.routineos.R.color.black))
                }
                "error" -> {
                    binding.messageCard.setCardBackgroundColor(com.routineos.R.color.error_red)
                    binding.messageText.setTextColor(itemView.context.getColor(com.routineos.R.color.white))
                }
            }

            // Align user messages to the right
            val layoutParams = binding.messageCard.layoutParams as ViewGroup.MarginLayoutParams
            if (message.type == "user") {
                layoutParams.marginStart = 48
                layoutParams.marginEnd = 8
            } else {
                layoutParams.marginStart = 8
                layoutParams.marginEnd = 48
            }
            binding.messageCard.layoutParams = layoutParams
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<CoachMessage>() {
        override fun areItemsTheSame(oldItem: CoachMessage, newItem: CoachMessage): Boolean {
            return oldItem.text == newItem.text && oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: CoachMessage, newItem: CoachMessage): Boolean {
            return oldItem == newItem
        }
    }
}
