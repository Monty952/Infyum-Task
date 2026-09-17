package com.example.practicalapp2.ui.activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicalapp2.databinding.ActivityChatDetailBinding
import com.example.practicalapp2.ui.adapter.MessageAdapter
import com.example.practicalapp2.ui.viewmodel.ChatDetailViewModel
import kotlinx.coroutines.launch

class ChatDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatDetailBinding
    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: run {
            finish()
            return
        }
        val chatName = intent.getStringExtra(EXTRA_CHAT_NAME) ?: "Chat"
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.whatsapp"
        val avatarColor = intent.getIntExtra(EXTRA_AVATAR_COLOR, 0xFF008069.toInt())

        setupTopBar(chatName, packageName, avatarColor, chatId)
        setupRecyclerView(chatId)
    }

    private fun setupTopBar(chatName: String, packageName: String, avatarColor: Int, chatId: String) {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.tvContactName.text = chatName
        val initial = chatName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
        binding.tvAvatarInitial.text = initial
        binding.viewAvatarBg.backgroundTintList = ColorStateList.valueOf(avatarColor)

        binding.tvChatSource.text = when {
            packageName.contains("w4b") -> "WhatsApp Business"
            chatId.startsWith("imported_") -> "Imported Backup Chat"
            else -> "WhatsApp"
        }

        binding.btnDeleteChat.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete all messages for this conversation?")
                .setPositiveButton("Delete") { _, _ ->
                    // Delete through repo / finish
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupRecyclerView(chatId: String) {
        messageAdapter = MessageAdapter(
            onAudioClick = { message ->
                message.mediaUri?.let { path ->
                    viewModel.playAudio(message.messageId, path)
                } ?: run {
                    Toast.makeText(this, "Audio file not available on local storage", Toast.LENGTH_SHORT).show()
                }
            },
            onMediaClick = { message ->
                Toast.makeText(this, "Viewing: ${message.content}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvMessages.apply {
            val layoutMgr = LinearLayoutManager(this@ChatDetailActivity)
            layoutMgr.stackFromEnd = true
            layoutManager = layoutMgr
            adapter = messageAdapter
        }

        val messagesFlow = viewModel.loadChat(chatId)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                messagesFlow.collect { messageList ->
                    messageAdapter.submitList(messageList) {
                        if (messageList.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(messageList.size - 1)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playbackState.collect { state ->
                    messageAdapter.updatePlaybackState(state)
                }
            }
        }
    }

    companion object {
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_CHAT_NAME = "extra_chat_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_AVATAR_COLOR = "extra_avatar_color"
    }
}
