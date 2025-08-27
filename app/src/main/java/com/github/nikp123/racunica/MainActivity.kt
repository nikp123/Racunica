package com.github.nikp123.racunica

import android.os.Bundle

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.nikp123.racunica.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : FragmentActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pager = binding.pager
        val tabLayout = binding.tabLayout

        val adapter = MainAdapter(this)
        pager.adapter = adapter

        TabLayoutMediator(tabLayout, pager) { tab, position ->
            tab.text = when(position) {
                0 -> getString(R.string.receipts)
                1 -> getString(R.string.settings)
                2 -> getString(R.string.statistics)
                else -> "This should not be here"
            }
        }.attach()
    }
}
class MainAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> return ReceiptsFragment()
            1 -> return SettingsFragment()
            2 -> return StatisticsFragment()
            else -> ReceiptsFragment()
        }
    }
}