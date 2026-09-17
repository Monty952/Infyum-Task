package com.example.practicalapp2.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.practicalapp2.data.local.entity.ChatEntity
import com.example.practicalapp2.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onChatClick: (ChatEntity) -> Unit,
    private val onChatLongClick: ((ChatEntity) -> Unit)? = null
) : ListAdapter<ChatEntity, ChatAdapter.ChatViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatEntity) {
            binding.tvSenderName.text = chat.senderName
            binding.tvLastMessage.text = chat.lastMessage

            // Format timestamp
            binding.tvTimestamp.text = formatTimestamp(chat.lastTimestamp)

            // Avatar initial and background color
            val initial = chat.senderName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
            binding.tvAvatarInitial.text = initial
            binding.viewAvatarBg.backgroundTintList = android.content.res.ColorStateList.valueOf(chat.avatarColor)

            // Unread badge
            if (chat.unreadCount > 0) {
                binding.tvUnreadBadge.visibility = View.VISIBLE
                binding.tvUnreadBadge.text = chat.unreadCount.toString()
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onChatClick(chat)
            }

            binding.root.setOnLongClickListener {
                onChatLongClick?.invoke(chat)
                true
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val sdf = if (diff < 24 * 60 * 60 * 1000) {
                SimpleDateFormat("hh:mm a", Locale.getDefault())
            } else {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            }
            return sdf.format(Date(timestamp))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ChatEntity>() {
        override fun areItemsTheSame(oldItem: ChatEntity, newItem: ChatEntity): Boolean =
            oldItem.chatId == newItem.chatId

        override fun areContentsTheSame(oldItem: ChatEntity, newItem: ChatEntity): Boolean =
            oldItem == newItem
    }
}
