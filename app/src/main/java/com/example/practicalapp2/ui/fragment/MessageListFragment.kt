package com.example.practicalapp2.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicalapp2.R
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.databinding.FragmentMessageListBinding
import com.example.practicalapp2.service.WhatsAppNotificationListener
import com.example.practicalapp2.ui.activity.ChatDetailActivity
import com.example.practicalapp2.ui.adapter.CapturedMessageAdapter
import com.example.practicalapp2.ui.viewmodel.WhatsDeleteViewModel
import kotlinx.coroutines.launch

class MessageListFragment : Fragment() {
    private var _binding: FragmentMessageListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatsDeleteViewModel by activityViewModels()
    private lateinit var messageAdapter: CapturedMessageAdapter

    private lateinit var allFilterChips: List<Pair<TextView, String>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupServiceStatusBar()
        setupFilterChips()
        setupRecyclerView()
        setupSimulationButton()
        observeMessages()
    }

    override fun onResume() {
        super.onResume()
        checkServiceStatus()
        // Request rebind to ensure listener service is connected
        WhatsAppNotificationListener.requestRebindIfNecessary(requireContext())
    }

    private fun checkServiceStatus() {
        val isPermissionGranted = NotificationManagerCompat.getEnabledListenerPackages(requireContext())
            .contains(requireContext().packageName)

        if (isPermissionGranted) {
            binding.layoutServiceStatus.setBackgroundColor(0xFFECFDF5.toInt())
            binding.viewStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF10B981.toInt())
            binding.tvServiceStatus.text = "Notification Listener: Active & Listening for WhatsApp"
            binding.tvServiceStatus.setTextColor(0xFF065F46.toInt())
            binding.layoutServiceStatus.setOnClickListener(null)
        } else {
            binding.layoutServiceStatus.setBackgroundColor(0xFFFEF2F2.toInt())
            binding.viewStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFEF4444.toInt())
            binding.tvServiceStatus.text = "⚠️ Notification Access Disabled (Tap here to enable)"
            binding.tvServiceStatus.setTextColor(0xFF991B1B.toInt())
            binding.layoutServiceStatus.setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Open Settings to grant Notification Access", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupServiceStatusBar() {
        checkServiceStatus()
    }

    private fun setupFilterChips() {
        allFilterChips = listOf(
            Pair(binding.chipFilterAll, "ALL"),
            Pair(binding.chipFilterText, "TEXT"),
            Pair(binding.chipFilterDeleted, "DELETED"),
            Pair(binding.chipFilterPhoto, "IMAGE"),
            Pair(binding.chipFilterVideo, "VIDEO"),
            Pair(binding.chipFilterVoice, "VOICE_NOTE"),
            Pair(binding.chipFilterVideoNote, "VIDEO_NOTE"),
            Pair(binding.chipFilterDocument, "DOCUMENT"),
            Pair(binding.chipFilterSticker, "STICKER")
        )

        for ((chip, typeKey) in allFilterChips) {
            chip.setOnClickListener {
                selectFilterChip(chip, typeKey)
            }
        }
    }

    private fun selectFilterChip(selectedChip: TextView, typeKey: String) {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_chip_selected)
        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_chip_unselected)
        val selectedText = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedText = ContextCompat.getColor(requireContext(), R.color.wa_chip_text_unselected)

        for ((chip, _) in allFilterChips) {
            if (chip == selectedChip) {
                chip.background = selectedBg
                chip.setTextColor(selectedText)
                chip.paint.isFakeBoldText = true
            } else {
                chip.background = unselectedBg
                chip.setTextColor(unselectedText)
                chip.paint.isFakeBoldText = false
            }
        }

        viewModel.setFilterType(typeKey)
    }

    private fun setupRecyclerView() {
        messageAdapter = CapturedMessageAdapter { message ->
            val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
                putExtra(ChatDetailActivity.EXTRA_CHAT_ID, message.chatId)
                putExtra(ChatDetailActivity.EXTRA_CHAT_NAME, message.senderName)
            }
            startActivity(intent)
        }

        binding.rvCapturedMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messageAdapter
        }
    }

    private fun setupSimulationButton() {
        binding.btnSimulateTest.setOnClickListener {
            val options = arrayOf(
                "💬 Text Message",
                "📷 Photo",
                "🎥 Video",
                "🎤 Voice Note",
                "📹 Video Note",
                "📄 Document (.pdf)",
                "👾 Sticker",
                "🖼️ GIF",
                "⚠️ Deleted Message Notice",
                "🚀 Generate All Types at Once"
            )

            AlertDialog.Builder(requireContext())
                .setTitle("Test WhatsApp Notification")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> viewModel.simulateSampleNotification(MessageType.TEXT)
                        1 -> viewModel.simulateSampleNotification(MessageType.IMAGE)
                        2 -> viewModel.simulateSampleNotification(MessageType.VIDEO)
                        3 -> viewModel.simulateSampleNotification(MessageType.VOICE_NOTE)
                        4 -> viewModel.simulateSampleNotification(MessageType.VIDEO_NOTE)
                        5 -> viewModel.simulateSampleNotification(MessageType.DOCUMENT)
                        6 -> viewModel.simulateSampleNotification(MessageType.STICKER)
                        7 -> viewModel.simulateSampleNotification(MessageType.GIF)
                        8 -> viewModel.simulateSampleNotification(MessageType.DELETED)
                        9 -> {
                            viewModel.simulateSampleNotification(MessageType.TEXT)
                            viewModel.simulateSampleNotification(MessageType.IMAGE)
                            viewModel.simulateSampleNotification(MessageType.VOICE_NOTE)
                            viewModel.simulateSampleNotification(MessageType.VIDEO_NOTE)
                            viewModel.simulateSampleNotification(MessageType.DOCUMENT)
                            viewModel.simulateSampleNotification(MessageType.DELETED)
                        }
                    }
                    Toast.makeText(requireContext(), "Test message added to captured messages!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.capturedMessages.collect { list ->
                    messageAdapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvCapturedMessages.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvCapturedMessages.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
