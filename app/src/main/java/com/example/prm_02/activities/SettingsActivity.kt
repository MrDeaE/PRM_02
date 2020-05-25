package com.example.prm_02.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.prm_02.R
import com.example.prm_02.SettingsViewModel
import com.example.prm_02.databinding.SettingsBinder
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding : SettingsBinder
    private lateinit var viewModel : SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        binding=DataBindingUtil.setContentView<SettingsBinder>(this, R.layout.activity_settings)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this@SettingsActivity

        setLiveDataObservers()
        setButtonsListeners()
    }

    private fun setLiveDataObservers() {

        viewModel.range.observe(this, Observer {
            viewModel.setRange(it)
        })
        viewModel.size.observe(this, Observer {
            if (it < 1){
                viewModel.setFontSize(1)
            } else {
                viewModel.setFontSize(it)
            }
        })
        viewModel.color.observe(this, Observer {
            viewModel.setFontColor(it)
        })

    }

    private fun setButtonsListeners(){
        button_back_settings.setOnClickListener { view ->
            finish()
        }
    }

}
