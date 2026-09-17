package com.example.practicalapp2.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.practicalapp2.R
import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.databinding.ItemMessageBinding
import com.example.practicalapp2.ui.viewmodel.AudioPlaybackState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val onAudioClick: (MessageEntity) -> Unit,
    private val onMediaClick: ((MessageEntity) -> Unit)? = null
) : ListAdapter<MessageEntity, MessageAdapter.MessageViewHolder>(DiffCallback) {

    private var currentPlaybackState = AudioPlaybackState()

    fun updatePlaybackState(state: AudioPlaybackState) {
        currentPlaybackState = state
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: MessageEntity) {
            val context = binding.root.context

            // Alignment: Outgoing vs Incoming
            val isOutgoing = msg.isFromMe
            val rootParams = binding.cardBubble.layoutParams as LinearLayout.LayoutParams
            if (isOutgoing) {
                rootParams.gravity = Gravity.END
                binding.cardBubble.setCardBackgroundColor(ContextCompat.getColor(context, R.color.wa_bubble_outgoing))
            } else {
                rootParams.gravity = Gravity.START
                binding.cardBubble.setCardBackgroundColor(ContextCompat.getColor(context, R.color.wa_bubble_incoming))
            }
            binding.cardBubble.layoutParams = rootParams

            // Content & Time
            binding.tvMessageContent.text = msg.content
            binding.tvMessageTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

            // Deleted Message Indicator
            if (msg.isDeleted || msg.messageType == MessageType.DELETED.name) {
                binding.layoutDeletedAlert.visibility = View.VISIBLE
                binding.cardBubble.setCardBackgroundColor(ContextCompat.getColor(context, R.color.wa_delete_highlight))
                if (msg.content.contains("This message was deleted", ignoreCase = true) && !msg.originalContent.isNullOrBlank()) {
                    binding.tvMessageContent.text = "Preserved: ${msg.originalContent}"
                }
            } else {
                binding.layoutDeletedAlert.visibility = View.GONE
            }

            // Image Preview via Coil
            val isImage = msg.messageType == MessageType.IMAGE.name || msg.messageType == MessageType.STICKER.name
            if (isImage && !msg.mediaUri.isNullOrBlank() && File(msg.mediaUri).exists()) {
                binding.ivMediaPreview.visibility = View.VISIBLE
                binding.ivMediaPreview.load(File(msg.mediaUri)) {
                    crossfade(true)
                }
                binding.ivMediaPreview.setOnClickListener {
                    onMediaClick?.invoke(msg)
                }
            } else {
                binding.ivMediaPreview.visibility = View.GONE
            }

            // Voice Note / Audio Player Widget
            val isAudio = msg.messageType == MessageType.VOICE_NOTE.name || msg.messageType == MessageType.AUDIO.name
            if (isAudio && !msg.mediaUri.isNullOrBlank() && File(msg.mediaUri).exists()) {
                binding.layoutAudioPlayer.visibility = View.VISIBLE
                val isPlaying = currentPlaybackState.isPlaying && currentPlaybackState.messageId == msg.messageId
                binding.btnAudioPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
                binding.tvAudioStatus.text = if (isPlaying) "Playing audio..." else "Tap to play voice note"

                binding.btnAudioPlayPause.setOnClickListener {
                    onAudioClick(msg)
                }
            } else {
                binding.layoutAudioPlayer.visibility = View.GONE
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
