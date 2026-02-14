package com.routineos.ui.coach.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.routineos.R
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
            val ctx = itemView.context
            when (message.type) {
                "user" -> {
                    binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.routine_purple))
                    binding.messageText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                "coach" -> {
                    binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.dark_surface))
                    binding.messageText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                "motivate" -> {
                    binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.success_green))
                    binding.messageText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                "error" -> {
                    binding.messageCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.error_red))
                    binding.messageText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
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
