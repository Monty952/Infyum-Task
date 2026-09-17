package com.example.practicalapp2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.practicalapp2.databinding.ActivityMainBinding
import com.example.practicalapp2.ui.activity.ChatBackupActivity
import com.example.practicalapp2.ui.activity.DirectChatActivity
import com.example.practicalapp2.ui.activity.WhatsDeleteActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolCards()
        setupTopBar()
        setupBottomNav()
        handleDirectShare(intent)
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDirectShare(it) }
    }

    private fun checkNotificationPermission() {
        val isGranted = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        if (isGranted) {
            binding.cardPermissionBanner.visibility = View.GONE
            com.example.practicalapp2.service.WhatsAppNotificationListener.requestRebindIfNecessary(this)
        } else {
            binding.cardPermissionBanner.visibility = View.VISIBLE
            binding.btnGrantPermission.setOnClickListener {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Please enable Notification Access in Settings", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupToolCards() {
        // WA Delete -> WhatsDelete Activity
        binding.cardToolWaDelete.setOnClickListener {
            startActivity(Intent(this, WhatsDeleteActivity::class.java))
        }

        // Chat Backup -> Chat Backup Activity (Import Chat)
        binding.cardToolChatBackup.setOnClickListener {
            startActivity(Intent(this, ChatBackupActivity::class.java))
        }

        // Direct Chat -> Direct Chat Activity
        binding.cardToolDirectChat.setOnClickListener {
            startActivity(Intent(this, DirectChatActivity::class.java))
        }

        // Status Saver -> Status Information / Helper
        binding.cardToolStatusSaver.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Status Saver")
                .setMessage("To view and save WhatsApp statuses, open WhatsApp, view any status story, and they will be indexed locally for quick saving.")
                .setPositiveButton("Open WhatsApp") { _, _ ->
                    val launchIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Close", null)
                .show()
        }

        // Tele Web Card
        binding.cardTeleWeb.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tele Web")
                .setMessage("Tele Web allows you to scan QR codes and open multi-device web sessions instantly.")
                .setPositiveButton("OK", null)
                .show()
        }

        binding.cardPremium.setOnClickListener {
            Toast.makeText(this, "Ad-Free & Unlimited Recovery Active!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTopBar() {
        binding.btnHelp.setOnClickListener {
            showInfoDialog()
        }

        binding.btnSettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Open System Settings to manage permissions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        binding.navItemHome.setOnClickListener {
            // Already on home
        }

        binding.navItemTools.setOnClickListener {
            binding.nestedScrollView.smoothScrollTo(0, binding.cardToolWaDelete.top)
        }

        binding.navItemSticker.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("WhatsApp Stickers")
                .setMessage("Explore and import custom sticker packs to WhatsApp.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info_limitations, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.widget.Button>(R.id.btnDismiss).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun handleDirectShare(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action || Intent.ACTION_SEND_MULTIPLE == intent.action) {
            val forwardIntent = Intent(this, ChatBackupActivity::class.java).apply {
                action = intent.action
                type = intent.type
                putExtras(intent)
            }
            startActivity(forwardIntent)
        }
    }
}