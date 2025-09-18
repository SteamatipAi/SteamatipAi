package com.steamatipai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.steamatipai.R
import com.steamatipai.data.models.RaceResult
import com.steamatipai.service.RaceAnalysisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RaceSelectionScreen(
    selectedDate: String,
    selectedTracks: List<String>,
    onRaceSelected: (RaceResult) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var raceResults by remember { mutableStateOf<List<RaceResult>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var analysisCompleted by remember { mutableStateOf(false) }
    var selectedRace by remember { mutableStateOf<RaceResult?>(null) }

    val raceAnalysisService = remember { RaceAnalysisService() }

    // Show single race results if a race is selected
    if (selectedRace != null) {
        SingleRaceResultsScreen(
            raceResult = selectedRace!!,
            onHorseClick = { horse ->
                // Handle horse click - this will show the horse detail screen
            },
            onBack = { selectedRace = null }
        )
        return
    }

    // Perform race analysis when the screen loads
    LaunchedEffect(selectedTracks) {
        if (!analysisCompleted) {
            try {
                isLoading = true
                error = null

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(selectedDate) ?: Date()

                // Create proper Track objects from the selected track keys
                val tracks = selectedTracks.mapNotNull { trackKey ->
                    try {
                        val parts = trackKey.split(",")
                        if (parts.size >= 3) {
                            val datePart = parts[0]
                            val state = parts[1]
                            val trackName = parts[2].replace("%20", " ")
                            
                            com.steamatipai.data.models.Track(
                                key = trackKey,
                                name = trackName,
                                state = state,
                                raceCount = 0,
                                url = ""
                            )
                        } else null
                    } catch (e: Exception) {
                        println("❌ Error parsing track key: $trackKey - ${e.message}")
                        null
                    }
                }

                println("🏁 Starting race analysis for ${tracks.size} tracks...")
                val analysisResult = withContext(Dispatchers.IO) {
                    raceAnalysisService.analyzeRaces(tracks, date)
                }

                raceResults = analysisResult.results
                analysisCompleted = true
                isLoading = false

                println("✅ Race analysis completed. Found ${analysisResult.results.size} races total")

            } catch (e: Exception) {
                println("❌ Error in RaceSelectionScreen: ${e.message}")
                error = "Failed to load race data: ${e.message}"
                isLoading = false
            }
        }
    }

    // Main UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack, 
                    contentDescription = "Back",
                    tint = Color(0xFFFFD700)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Race",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = selectedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Spacer to balance the back button
            Spacer(modifier = Modifier.width(48.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analysing races...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Text(
                        text = "This may take a few minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Analysis Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFF6B6B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            error = null
                            analysisCompleted = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color(0xFFFFD700)
                        ),
                        border = BorderStroke(2.dp, Color(0xFFFFD700))
                    ) {
                        Text(
                            "Retry Analysis",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        } else if (raceResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Racing Data Available",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No real racing data was found for the selected tracks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Race List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Group races by track and sort by race number
                val racesByTrack = raceResults.groupBy { it.race.venue }
                
                racesByTrack.forEach { (trackName, races) ->
                    item {
                        // Track header
                        Text(
                            text = trackName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(races.sortedBy { it.race.raceNumber }) { raceResult ->
                        RaceSelectionCard(
                            raceResult = raceResult,
                            onClick = { selectedRace = raceResult }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RaceSelectionCard(
    raceResult: RaceResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Race Number Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${raceResult.race.raceNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Race Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = raceResult.race.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${raceResult.race.time} • ${raceResult.race.distance}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "${raceResult.topSelections.size} horses",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFD700).copy(alpha = 0.8f)
                )
            }

            // Arrow indicator
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_more),
                contentDescription = "View Race",
                tint = Color(0xFFFFD700).copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
