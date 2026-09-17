package com.example.practicalapp2.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import java.net.URLEncoder

class DirectChatViewModel : ViewModel() {
    fun createWhatsAppIntent(countryCode: String, phoneNumber: String, message: String): Intent {
        val cleanCountryCode = countryCode.replace("+", "").trim()
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "").trim()
        val fullNumber = "$cleanCountryCode$cleanNumber"

        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            ""
        }

        val url = if (encodedMessage.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$fullNumber&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?phone=$fullNumber"
        }

        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            `package` = "com.whatsapp"
        }
    }
}
