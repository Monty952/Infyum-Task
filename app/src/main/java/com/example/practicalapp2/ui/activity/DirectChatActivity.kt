package com.example.practicalapp2.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.practicalapp2.databinding.ActivityDirectChatBinding
import com.example.practicalapp2.ui.viewmodel.DirectChatViewModel

class DirectChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDirectChatBinding
    private val viewModel: DirectChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDirectChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDirectSend.setOnClickListener {
            val countryCode = binding.etCountryCode.text?.toString() ?: ""
            val phone = binding.etPhoneNumber.text?.toString() ?: ""
            val message = binding.etMessage.text?.toString() ?: ""

            if (phone.isBlank()) {
                binding.etPhoneNumber.error = "Please enter a phone number"
                return@setOnClickListener
            }

            try {
                val intent = viewModel.createWhatsAppIntent(countryCode, phone, message)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp is not installed on this device", Toast.LENGTH_LONG).show()
            }
        }
    }
}
