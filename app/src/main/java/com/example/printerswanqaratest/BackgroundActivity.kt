package com.example.printerswanqaratest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.printerswanqaratest.ui.theme.PrintersWanqaraTestTheme

class BackgroundActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrintersWanqaraTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BackgroundConfigScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BackgroundConfigScreen(modifier: Modifier = Modifier) {
    // TODO: Implement background printer configuration UI
}
