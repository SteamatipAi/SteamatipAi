package com.steamatipai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.foundation.Image
import com.steamatipai.data.models.ScoredHorse
import com.steamatipai.data.models.Race
import com.steamatipai.data.models.RaceResult
import com.steamatipai.service.RaceAnalysisService
import com.steamatipai.data.models.Track
import com.steamatipai.R
import java.text.SimpleDateFormat
import java.util.*
import com.steamatipai.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ResultsScreen(
    selectedDate: String,
    selectedTracks: List<String>,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<RaceResult>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var processingTime by remember { mutableStateOf(0L) }
    var selectedHorse by remember { mutableStateOf<ScoredHorse?>(null) }

    val raceAnalysisService = remember { RaceAnalysisService() }
    val context = LocalContext.current

    // Perform real analysis when the screen loads
    LaunchedEffect(selectedTracks) {
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
                        
                Track(
                    key = trackKey,
                            name = trackName,
                    state = state,
                    raceCount = 0,
                            url = NetworkConfig.buildTrackFormUrl(datePart, state, trackName)
                        )
                    } else {
                        println("⚠️ Invalid track key format: $trackKey")
                        null
                    }
                } catch (e: Exception) {
                    println("❌ Error parsing track key $trackKey: ${e.message}")
                    null
                }
            }

            if (tracks.isEmpty()) {
                error = "Failed to parse track information. Please try selecting tracks again."
                results = emptyList()
                processingTime = 0
                return@LaunchedEffect
            }

            println("🏁 Reconstructed ${tracks.size} tracks for analysis:")
            tracks.forEach { track ->
                println("   - ${track.name} (${track.state}) - URL: ${track.url}")
            }

            // Perform real analysis with proper track objects
            val analysisResult = withContext(Dispatchers.IO) {
                raceAnalysisService.analyzeRaces(tracks, date, true)
            }

            if (analysisResult.error == null) {
                results = analysisResult.results
                processingTime = analysisResult.processingTime
            } else {
                error = analysisResult.error ?: "Analysis failed"
                results = emptyList()
                processingTime = 0
            }
        } catch (e: Exception) {
            error = "Failed to analyze races: ${e.message}"
            results = emptyList()
            processingTime = 0
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Backdrop Image
        Image(
            painter = painterResource(id = R.drawable.app_backdrop),
            contentDescription = "Racing backdrop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Dark overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // Content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack, 
                    contentDescription = "Back",
                        tint = Color(0xFFFFD700)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Race Analysis Results",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            // Show horse detail screen if a horse is selected
            if (selectedHorse != null) {
                HorseScoringDetailScreen(
                    scoredHorse = selectedHorse!!,
                    onBackClick = { selectedHorse = null }
                )
                return@Column
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing races...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "This may take a few minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (error != null && results.isEmpty()) {
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
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            error = null
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
            } else if (results.isEmpty()) {
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
                            text = "No real racing data was found for the selected date and tracks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                error = null
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
        } else {
                // Results List - Organized by Race Number
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                    // Sort results by race number
                    val sortedResults = results.sortedBy { it.race.raceNumber }
                    
                    // Display each race separately
                    items(sortedResults) { raceResult ->
                        RaceResultCard(
                            raceResult = raceResult,
                            onHorseClick = { horse -> selectedHorse = horse }
                        )
                    }
                    
                    // Add processing time info at the end
                    if (processingTime > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black.copy(alpha = 0.8f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Analysis Complete",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Processing time: ${processingTime}ms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun RaceResultCard(
    raceResult: RaceResult,
    onHorseClick: (ScoredHorse) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Race Header
            Text(
                text = "Race ${raceResult.race.raceNumber}: ${raceResult.race.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Track Info
                Text(
                    text = raceResult.race.venue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "${raceResult.race.time} • ${raceResult.race.distance}m",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Top 5 Selections
            Text(
                text = "Top 5 Horses:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            raceResult.topSelections.take(5).forEachIndexed { index, horse ->
                HorseSelectionItem(
                    horse = horse,
                    position = index + 1,
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { onHorseClick(horse) }
                )
            }
        }
    }
}

@Composable
fun HorseSelectionItem(
    horse: ScoredHorse,
    position: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = when (position) {
                            1 -> Color(0xFFFFD700) // Gold for 1st
                            2 -> Color(0xFFC0C0C0) // Silver for 2nd
                            3 -> Color(0xFFCD7F32) // Bronze for 3rd
                            else -> Color(0xFF666666) // Gray for others
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = horse.horse.number.toString(), // Show actual saddle number instead of position
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (position <= 3) Color.Black else Color.White
                )
            }

            // Horse Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = horse.horse.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score: ${String.format("%.1f", horse.score)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (horse.isStandout) {
                        Spacer(modifier = Modifier.width(8.dp))
                Text(
                            text = "⭐",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Text(
                    text = "J: ${horse.horse.jockey}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "T: ${horse.horse.trainer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            
                Text(
                    text = "Barrier: ${horse.horse.barrier} • Weight: ${horse.horse.weight}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}


