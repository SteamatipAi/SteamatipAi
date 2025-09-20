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
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
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
    var startTime by remember { mutableStateOf(0L) }
    var elapsedTime by remember { mutableStateOf(0L) }

    val raceAnalysisService = remember { RaceAnalysisService() }
    val context = LocalContext.current

    // Timer effect to update elapsed time during loading
    LaunchedEffect(isLoading, startTime) {
        if (isLoading && startTime > 0) {
            while (isLoading) {
                elapsedTime = System.currentTimeMillis() - startTime
                kotlinx.coroutines.delay(100) // Update every 100ms
            }
        }
    }

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
                startTime = System.currentTimeMillis()
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

            // Share button
            IconButton(
                onClick = {
                    shareAllRaceResults(context, raceResults, selectedDate)
                }
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share All Results",
                    tint = Color(0xFFFFD700)
                )
            }
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
                        text = "Elapsed time: ${String.format("%.1f", elapsedTime / 1000.0)}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This may take a few minutes",
                        style = MaterialTheme.typography.bodySmall,
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // Add bottom padding for system UI
            ) {
                // Group races by track and sort by race number
                val racesByTrack = raceResults.groupBy { it.race.venue }
                
                racesByTrack.forEach { (trackName, races) ->
                    item {
                        // Track header
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = trackName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700))
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
                    .size(32.dp)
                    .background(
                        color = Color(0xFFFFD700), // Solid gold background
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${raceResult.race.raceNumber}",
                    style = MaterialTheme.typography.titleSmall,
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
                val actualHorseCount = if (raceResult.allHorses.isNotEmpty()) raceResult.allHorses.size else raceResult.topSelections.size
                Text(
                    text = "$actualHorseCount horses",
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

fun shareAllRaceResults(
    context: android.content.Context,
    raceResults: List<RaceResult>,
    selectedDate: String
) {
    if (raceResults.isEmpty()) {
        return
    }
    
    try {
        // Build a more concise share text to prevent memory issues
        val shareText = buildString {
            appendLine("🏇 STEAMA TIP AI - COMPLETE RACE ANALYSIS")
            appendLine("═══════════════════════════════════════════")
            appendLine("📅 Date: $selectedDate")
            appendLine("🏁 Total Races: ${raceResults.size}")
            appendLine()
            
            // Group by track
            val racesByTrack = raceResults.groupBy { it.race.venue }
            
            racesByTrack.forEach { (trackName, races) ->
                appendLine("🏟️ $trackName".uppercase())
                appendLine("▔".repeat(30))
                appendLine()
                
                races.sortedBy { it.race.raceNumber }.forEach { raceResult ->
                    appendLine("🏇 RACE ${raceResult.race.raceNumber}: ${raceResult.race.name}")
                    appendLine("⏰ ${raceResult.race.time} • 📏 ${raceResult.race.distance}m")
                    
                    // Show only TOP 6 horses to keep size manageable
                    val topHorses = if (raceResult.allHorses.isNotEmpty()) {
                        raceResult.allHorses.take(6)
                    } else {
                        raceResult.topSelections.take(6)
                    }
                    
                    appendLine("🐎 TOP SELECTIONS (${topHorses.size} of ${if (raceResult.allHorses.isNotEmpty()) raceResult.allHorses.size else raceResult.topSelections.size}):")
                    
                    topHorses.forEachIndexed { index, horse ->
                        val position = index + 1
                        
                        // Add betting indicator for top horse if applicable
                        val bettingIndicator = if (index == 0 && raceResult.bettingRecommendations.isNotEmpty()) {
                            val topRecommendation = raceResult.bettingRecommendations[0]
                            when (topRecommendation.betType) {
                                com.steamatipai.data.models.BetType.SUPER_BET -> " ⭐"
                                com.steamatipai.data.models.BetType.BEST_BET -> " 🔵"
                                com.steamatipai.data.models.BetType.GOOD_BET -> " 🟣"
                                else -> ""
                            }
                        } else ""
                        
                        appendLine("${position}. #${horse.horse.number} ${horse.horse.name}$bettingIndicator")
                        appendLine("   💯 ${String.format("%.1f", horse.score)} • J: ${horse.horse.jockey}")
                    }
                    
                    appendLine("═".repeat(30))
                    appendLine()
                }
            }
            
            appendLine("📱 Generated by SteamaTip AI")
        }
        
        // Check text size and truncate if necessary (Android has ~1MB limit for Intent extras)
        val finalText = if (shareText.length > 100000) {
            shareText.take(95000) + "\n\n... (Content truncated for sharing)\n📱 Generated by SteamaTip AI"
        } else {
            shareText
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, finalText)
            putExtra(Intent.EXTRA_SUBJECT, "Complete Race Analysis - $selectedDate")
            // Add flags to prevent issues
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Use try-catch for the actual share action
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Complete Race Analysis"))
        } catch (e: Exception) {
            println("❌ Error sharing results: ${e.message}")
            // Fallback: try with even shorter text
            val shortText = "🏇 STEAMA TIP AI - RACE ANALYSIS\n📅 $selectedDate\n🏁 ${raceResults.size} races analyzed\n\n📱 Full results available in app"
            val fallbackIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shortText)
                putExtra(Intent.EXTRA_SUBJECT, "Race Analysis - $selectedDate")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share Race Analysis"))
        }
        
    } catch (e: Exception) {
        println("❌ Critical error in share function: ${e.message}")
        e.printStackTrace()
    }
}
