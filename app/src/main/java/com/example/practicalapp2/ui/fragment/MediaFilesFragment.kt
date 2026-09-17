package com.example.practicalapp2.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.practicalapp2.R
import com.example.practicalapp2.data.model.MessageType
import com.example.practicalapp2.databinding.FragmentMediaFilesBinding
import com.example.practicalapp2.ui.activity.ChatDetailActivity
import com.example.practicalapp2.ui.adapter.MediaAdapter
import com.example.practicalapp2.ui.viewmodel.WhatsDeleteViewModel
import kotlinx.coroutines.launch

class MediaFilesFragment : Fragment() {
    private var _binding: FragmentMediaFilesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WhatsDeleteViewModel by activityViewModels()
    private lateinit var mediaAdapter: MediaAdapter

    private lateinit var allChips: List<Pair<TextView, MessageType>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allChips = listOf(
            Pair(binding.chipPhoto, MessageType.IMAGE),
            Pair(binding.chipVideo, MessageType.VIDEO),
            Pair(binding.chipAudio, MessageType.AUDIO),
            Pair(binding.chipVoiceNote, MessageType.VOICE_NOTE),
            Pair(binding.chipDocument, MessageType.DOCUMENT),
            Pair(binding.chipZip, MessageType.DOCUMENT),
            Pair(binding.chipGif, MessageType.GIF),
            Pair(binding.chipSticker, MessageType.STICKER)
        )

        setupChipListeners()

        mediaAdapter = MediaAdapter { message ->
            val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
                putExtra(ChatDetailActivity.EXTRA_CHAT_ID, message.chatId)
                putExtra(ChatDetailActivity.EXTRA_CHAT_NAME, message.senderName)
            }
            startActivity(intent)
        }

        binding.rvMedia.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = mediaAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mediaMessages.collect { list ->
                    mediaAdapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.layoutEmptyMedia.visibility = View.VISIBLE
                        binding.rvMedia.visibility = View.GONE
                    } else {
                        binding.layoutEmptyMedia.visibility = View.GONE
                        binding.rvMedia.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupChipListeners() {
        for ((chipView, type) in allChips) {
            chipView.setOnClickListener {
                selectChip(chipView, type)
            }
        }
    }

    private fun selectChip(selectedView: TextView, type: MessageType) {
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_chip_selected)
        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_chip_unselected)
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.wa_chip_text_unselected)

        for ((chip, _) in allChips) {
            if (chip == selectedView) {
                chip.background = selectedBg
                chip.setTextColor(selectedTextColor)
                chip.paint.isFakeBoldText = true
            } else {
                chip.background = unselectedBg
                chip.setTextColor(unselectedTextColor)
                chip.paint.isFakeBoldText = false
            }
        }

        updateEmptyIllustration(type)
        viewModel.setSelectedMediaType(type)
    }

    private fun updateEmptyIllustration(type: MessageType) {
        when (type) {
            MessageType.IMAGE -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_empty_photo)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_photo)
            }
            MessageType.VIDEO -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_empty_video)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_video)
            }
            MessageType.AUDIO -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_empty_audio)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_audio)
            }
            MessageType.VOICE_NOTE -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_empty_audio)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_voice_note)
            }
            MessageType.DOCUMENT -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_empty_document)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_document)
            }
            MessageType.GIF -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_empty_photo)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_gif)
            }
            MessageType.STICKER -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_nav_sticker)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_sticker)
            }
            else -> {
                binding.ivEmptyMediaIllustration.setImageResource(R.drawable.ic_empty_photo)
                binding.tvEmptyMediaMessage.text = getString(R.string.empty_photo)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
