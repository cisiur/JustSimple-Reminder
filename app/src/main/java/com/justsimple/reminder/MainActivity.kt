package com.justsimple.reminder

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.justsimple.reminder.reliability.OemReliabilityGuide
import com.justsimple.reminder.ui.navigation.JustSimpleReminderNavHost
import com.justsimple.reminder.ui.theme.JustSimpleReminderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var reliabilityGuide: OemReliabilityGuide

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JustSimpleReminderTheme {
                JustSimpleReminderNavHost(reliabilityGuide = reliabilityGuide)
            }
        }
    }
}
