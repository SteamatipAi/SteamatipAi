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
                    // Pass the full track objects as a serializable format
                    val trackData = tracks.joinToString("|") { "${it.key}::${it.name}::${it.state}" }
                    navController.navigate("results/$date/$trackData")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("results/{date}/{tracks}") { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val tracksData = backStackEntry.arguments?.getString("tracks") ?: ""
            
            // Parse the track data back into proper format
            val selectedTracks = tracksData.split("|").filter { it.isNotEmpty() }.map { trackData ->
                val parts = trackData.split("::")
                if (parts.size >= 3) {
                    parts[0] // Return the full key (e.g., "2025Aug30,NSW,Rosehill Gardens")
                } else {
                    trackData // Fallback to original data
                }
            }
            
            ResultsScreen(
                selectedDate = date,
                selectedTracks = selectedTracks,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}