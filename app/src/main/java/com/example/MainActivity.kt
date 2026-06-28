package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SubscriptionDatabase
import com.example.data.SubscriptionRepository
import com.example.ui.SubscriptionApp
import com.example.ui.SubscriptionViewModel
import com.example.ui.SubscriptionViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize Database and Repository
                val database = SubscriptionDatabase.getDatabase(applicationContext)
                val repository = SubscriptionRepository(database.subscriptionDao())
                val factory = SubscriptionViewModelFactory(application, repository)
                val viewModel: SubscriptionViewModel = viewModel(factory = factory)

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SubscriptionApp(viewModel = viewModel)
                }
            }
        }
    }
}

