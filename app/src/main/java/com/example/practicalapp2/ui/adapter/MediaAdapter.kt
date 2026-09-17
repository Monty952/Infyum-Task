package com.example.practicalapp2.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.practicalapp2.R
import com.example.practicalapp2.data.local.entity.MessageEntity
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.databinding.ItemMediaBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaAdapter(
    private val onMediaClick: (MessageEntity) -> Unit
) : ListAdapter<MessageEntity, MediaAdapter.MediaViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: MessageEntity) {
            binding.tvMediaSender.text = msg.senderName
            binding.tvMediaTimestamp.text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))

            val file = if (!msg.mediaUri.isNullOrBlank()) File(msg.mediaUri) else null

            if (file != null && file.exists() && (msg.messageType == MessageType.IMAGE.name || msg.messageType == MessageType.STICKER.name)) {
                binding.ivMediaThumbnail.visibility = View.VISIBLE
                binding.ivMediaTypeIcon.visibility = View.GONE
                binding.ivMediaThumbnail.load(file) {
                    crossfade(true)
                }
            } else {
                binding.ivMediaThumbnail.visibility = View.GONE
                binding.ivMediaTypeIcon.visibility = View.VISIBLE
                val iconRes = when (MessageType.fromString(msg.messageType)) {
                    MessageType.IMAGE -> R.drawable.ic_empty_photo
                    MessageType.VIDEO -> R.drawable.ic_empty_video
                    MessageType.AUDIO, MessageType.VOICE_NOTE -> R.drawable.ic_empty_audio
                    MessageType.DOCUMENT -> R.drawable.ic_empty_document
                    else -> R.drawable.ic_folder
                }
                binding.ivMediaTypeIcon.setImageResource(iconRes)
            }

            binding.root.setOnClickListener {
                onMediaClick(msg)
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
