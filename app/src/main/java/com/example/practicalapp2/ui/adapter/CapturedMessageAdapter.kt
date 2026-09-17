package com.example.practicalapp2.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.practicalapp2.R
import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.data.repository.ChatRepository
import com.example.practicalapp2.databinding.ItemCapturedMessageBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CapturedMessageAdapter(
    private val onMessageClick: (MessageEntity) -> Unit
) : ListAdapter<MessageEntity, CapturedMessageAdapter.MessageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemCapturedMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemCapturedMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: MessageEntity) {
            val context = binding.root.context
            binding.tvSenderName.text = msg.senderName
            binding.tvTimestamp.text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

            // Avatar initial and color
            val initial = msg.senderName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
            binding.tvAvatarInitial.text = initial
            val color = ChatRepository.getAvatarColor(msg.senderName)
            binding.viewAvatarBg.backgroundTintList = ColorStateList.valueOf(color)

            val type = MessageType.fromString(msg.messageType)
            binding.tvMessageTypeBadge.text = type.displayName

            // Message Type Badge Styling
            if (msg.isDeleted || type == MessageType.DELETED) {
                binding.tvMessageTypeBadge.text = "Deleted"
                binding.tvMessageTypeBadge.backgroundTintList = ColorStateList.valueOf(0xFFFEE2E2.toInt())
                binding.tvMessageTypeBadge.setTextColor(0xFFDC2626.toInt())
                binding.layoutDeletedAlert.visibility = View.VISIBLE
            } else {
                binding.tvMessageTypeBadge.backgroundTintList = null
                binding.tvMessageTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.wa_primary))
                binding.layoutDeletedAlert.visibility = View.GONE
            }

            // Message preview content
            binding.tvMessageContent.text = msg.content

            // Image Preview via Coil
            val isImage = type == MessageType.IMAGE || type == MessageType.STICKER
            if (isImage && !msg.mediaUri.isNullOrBlank() && File(msg.mediaUri).exists()) {
                binding.ivMediaPreview.visibility = View.VISIBLE
                binding.ivMediaPreview.load(File(msg.mediaUri)) {
                    crossfade(true)
                }
            } else {
                binding.ivMediaPreview.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onMessageClick(msg)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean =
            oldItem.messageId == newItem.messageId

        override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean =
            oldItem == newItem
    }
}
