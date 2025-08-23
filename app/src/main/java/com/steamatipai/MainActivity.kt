package com.steamatipai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.steamatipai.ui.screens.DateSelectionScreen
import com.steamatipai.ui.screens.TrackSelectionScreen
import com.steamatipai.ui.screens.ResultsScreen
import com.steamatipai.ui.theme.SteamaTipAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SteamaTipAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SteamaTipAiApp()
                }
            }
        }
    }
}

@Composable
fun SteamaTipAiApp() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "date_selection") {
        composable("date_selection") {
            DateSelectionScreen(
                onDateSelected = { date ->
                    navController.navigate("track_selection/$date")
                }
            )
        }
        
        composable("track_selection/{date}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            TrackSelectionScreen(
                selectedDate = date,
                onTracksSelected = { tracks ->
                    navController.navigate("results/$date/${tracks.joinToString(",")}")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("results/{date}/{tracks}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val tracks = backStackEntry.arguments?.getString("tracks") ?: ""
            ResultsScreen(
                selectedDate = date,
                selectedTracks = tracks.split(",").filter { it.isNotEmpty() },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}