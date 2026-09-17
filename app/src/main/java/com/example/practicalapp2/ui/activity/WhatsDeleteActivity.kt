package com.example.practicalapp2.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.practicalapp2.R
import com.example.practicalapp2.databinding.ActivityWhatsDeleteBinding
import com.example.practicalapp2.ui.fragment.MediaFilesFragment
import com.example.practicalapp2.ui.fragment.MessageListFragment
import com.example.practicalapp2.ui.viewmodel.WhatsDeleteViewModel
import com.google.android.material.tabs.TabLayoutMediator

class WhatsDeleteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWhatsDeleteBinding
    private val viewModel: WhatsDeleteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhatsDeleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopBar()
        setupViewPagerAndTabs()
        setupSearch()
    }

    private fun setupTopBar() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnHelp.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun setupViewPagerAndTabs() {
        val fragments = listOf<Fragment>(
            MessageListFragment(),
            MediaFilesFragment()
        )

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        val titles = listOf(
            getString(R.string.tab_message),
            getString(R.string.tab_media_files)
        )

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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
}
