package com.example.practicalapp2.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicalapp2.R
import com.example.practicalapp2.databinding.ActivityChatBackupBinding
import com.example.practicalapp2.ui.adapter.ChatAdapter
import com.example.practicalapp2.ui.viewmodel.BackupUiEvent
import com.example.practicalapp2.ui.viewmodel.ChatBackupViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ChatBackupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBackupBinding
    private val viewModel: ChatBackupViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    // File picker for WhatsApp export ZIP / TXT
    private val openZipFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(uri)
            if (fileName.endsWith(".txt", ignoreCase = true)) {
                viewModel.importTextFile(uri, fileName)
            } else {
                viewModel.importZipFile(uri)
            }
        }
    }

    // SAF Create Document for Database Backup
    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createBackup(uri)
        }
    }

    // SAF Open Document for Database Restore
    private val restoreBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackup(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopBar()
        setupRecyclerView()
        setupActionButtons()
        observeViewModel()
        handleIncomingShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingShareIntent(it) }
    }

    private fun setupTopBar() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCreateBackup.setOnClickListener {
            val timestamp = System.currentTimeMillis()
            createBackupLauncher.launch("WT_Chat_Backup_${timestamp}.json")
        }

        binding.btnRestoreBackup.setOnClickListener {
            restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onChatClick = { chat ->
                val intent = Intent(this, ChatDetailActivity::class.java).apply {
                    putExtra(ChatDetailActivity.EXTRA_CHAT_ID, chat.chatId)
                    putExtra(ChatDetailActivity.EXTRA_CHAT_NAME, chat.senderName)
                    putExtra(ChatDetailActivity.EXTRA_PACKAGE_NAME, chat.packageName)
                    putExtra(ChatDetailActivity.EXTRA_AVATAR_COLOR, chat.avatarColor)
                }
                startActivity(intent)
            },
            onChatLongClick = { chat ->
                showDeleteChatDialog(chat.chatId, chat.senderName)
            }
        )

        binding.rvImportedChats.apply {
            layoutManager = LinearLayoutManager(this@ChatBackupActivity)
            adapter = chatAdapter
        }
    }

    private fun setupActionButtons() {
        binding.btnImportChat.setOnClickListener {
            showChooseAppBottomSheet()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.importedChats.collect { list ->
                    chatAdapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvImportedChats.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvImportedChats.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    binding.btnImportChat.isEnabled = !loading
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is BackupUiEvent.Success -> Toast.makeText(this@ChatBackupActivity, event.message, Toast.LENGTH_LONG).show()
                        is BackupUiEvent.Error -> Toast.makeText(this@ChatBackupActivity, event.message, Toast.LENGTH_LONG).show()
                        is BackupUiEvent.Info -> Toast.makeText(this@ChatBackupActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Shows "Choose App" bottom sheet matching screenshot:
     * - WhatsApp
     * - WhatsApp Business
     * - File Manager
     */
    private fun showChooseAppBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_choose_app, null)
        bottomSheet.setContentView(sheetView)

        sheetView.findViewById<Button>(R.id.btnOptionWhatsApp).setOnClickListener {
            bottomSheet.dismiss()
            openExternalApp("com.whatsapp")
        }

        sheetView.findViewById<Button>(R.id.btnOptionWaBusiness).setOnClickListener {
            bottomSheet.dismiss()
            openExternalApp("com.whatsapp.w4b")
        }

        sheetView.findViewById<Button>(R.id.btnOptionFileManager).setOnClickListener {
            bottomSheet.dismiss()
            showZipInstructionsDialog()
        }

        bottomSheet.show()
    }

    /**
     * Shows instructions dialog matching screenshot:
     * "Select WhatsApp Chat ZIP"
     * "Please select a WhatsApp chat export ZIP file..."
     */
    private fun showZipInstructionsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_zip_instructions, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnOpenFilePicker).setOnClickListener {
            dialog.dismiss()
            openZipFileLauncher.launch(arrayOf("application/zip", "application/octet-stream", "text/plain", "*/*"))
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun openExternalApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            Toast.makeText(this, "Export your chat from WhatsApp using Export Chat, then share or select the ZIP!", Toast.LENGTH_LONG).show()
            startActivity(launchIntent)
        } else {
            // Open Play Store if app is not installed
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }
    }

    private fun handleIncomingShareIntent(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action) {
            val uri = androidx.core.content.IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            if (uri != null) {
                val fileName = getFileName(uri)
                if (fileName.endsWith(".txt", ignoreCase = true)) {
                    viewModel.importTextFile(uri, fileName)
                } else {
                    viewModel.importZipFile(uri)
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "whatsapp_export.zip"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun showDeleteChatDialog(chatId: String, senderName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Do you want to delete the imported chat with $senderName?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteChat(chatId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
