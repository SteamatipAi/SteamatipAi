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
import com.steamatipai.service.RaceResult
import com.steamatipai.service.RaceAnalysisService
import com.steamatipai.data.models.Track
import com.steamatipai.R
import java.text.SimpleDateFormat
import java.util.*

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
    var totalHorsesAnalyzed by remember { mutableStateOf(0) }

    val raceAnalysisService = remember { RaceAnalysisService() }
    val context = LocalContext.current

    // Perform real analysis when the screen loads
    LaunchedEffect(selectedTracks) {
        try {
            isLoading = true
            error = null

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(selectedDate) ?: Date()

            // Create Track objects from the selected track keys
            val tracks = selectedTracks.map { trackKey ->
                val parts = trackKey.split("_")
                val state = parts.firstOrNull() ?: "Unknown"
                val nameRaw = parts.drop(1).joinToString("_")
                val name = nameRaw.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace("_", " ").trim()
                Track(
                    key = trackKey,
                    name = name,
                    state = state,
                    raceCount = 0,
                    url = ""
                )
            }

            // Perform real analysis
            val analysisResult = raceAnalysisService.analyzeRaces(tracks, date, true)

            if (analysisResult.success) {
                // Flatten all race results from all tracks
                val allRaceResults = analysisResult.results.flatMap { trackResult ->
                    trackResult.races
                }

                results = allRaceResults
                processingTime = analysisResult.processingTime
                totalHorsesAnalyzed = analysisResult.totalHorsesAnalyzed
            } else {
                error = analysisResult.error ?: "Analysis failed"
                // NO FALLBACK - Use real data only
                results = emptyList()
                processingTime = 0
                totalHorsesAnalyzed = 0
            }
        } catch (e: Exception) {
            error = "Failed to analyze races: ${e.message}"
            // NO FALLBACK - Use real data only
            results = emptyList()
            processingTime = 0
            totalHorsesAnalyzed = 0
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
                    tint = Color(0xFFFFD700) // Gold color
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Race Analysis Results",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700) // Gold color
                )
            }
            
            // Share Button
            IconButton(onClick = { shareResults(context, results, selectedDate, selectedTracks) }) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share Results",
                    tint = Color(0xFFFFD700) // Gold color
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
                            // Retry analysis
                            isLoading = true
                            error = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color(0xFFFFD700) // Gold color
                        ),
                        border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
                    ) {
                        Text(
                            "Retry Analysis",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700) // Gold color
                        )
                    }
                }
            }
        } else {


            // Results List - Grouped by Track
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Debug: Print total results count
                println("🏁 DEBUG: Total results count: ${results.size}")
                results.forEachIndexed { index, raceResult ->
                    println("🏁 DEBUG [$index]: Race ${raceResult.race.raceNumber} - ${raceResult.race.name} - ${raceResult.race.venue} - ${raceResult.topSelections.size} horses")
                }
                
                // Group results by track
                val groupedByTrack = results.groupBy { it.race.venue }
                
                groupedByTrack.forEach { (trackName, trackRaces) ->
                    // Track Header
                    item {
                        TrackHeader(
                            trackName = trackName,
                            raceCount = trackRaces.size,
                            state = extractStateFromVenue(trackName)
                        )
                    }
                    
                    // Races for this track
                    items(trackRaces) { raceResult ->
                    RaceResultCard(raceResult = raceResult)
                    }
                    
                    // Spacer between tracks
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                }
            }
        }
    }
}

@Composable
fun RaceResultCard(raceResult: RaceResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Race Header
            Text(
                text = "Race ${raceResult.race.raceNumber}: ${raceResult.race.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
                color = Color.White
            )

            // Track and State Info
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = raceResult.race.venue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )
                
                // Extract state from venue and display in gold
                val state = extractStateFromVenue(raceResult.race.venue)
                if (state.isNotEmpty()) {
                    Text(
                        text = state,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700), // Gold color
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = "${raceResult.race.time} • ${raceResult.race.distance}m • ${raceResult.race.raceClass}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Top Selections
            raceResult.topSelections.forEachIndexed { index, horse ->
                println("🔍 DEBUG: Horse '${horse.horse.name}' has number: ${horse.horse.number}")
                HorseSelectionItem(
                    horse = horse,
                    position = horse.horse.number, // Use the horse's actual race number
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Processing Info
            Text(
                text = "Processed in ${raceResult.processingTime}ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun HorseSelectionItem(
    horse: ScoredHorse,
    position: Int,
    modifier: Modifier = Modifier
) {
    var showScoringBreakdown by remember { mutableStateOf(false) }
    
    // Debug logging
    LaunchedEffect(Unit) {
        println("🔍 HorseSelectionItem: '${horse.horse.name}' position=$position, horse.number=${horse.horse.number}")
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showScoringBreakdown = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Position Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color.Black,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFD700), // Gold color
                    fontWeight = FontWeight.Bold
                )
            }

            // Horse Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = horse.horse.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700) // Gold color for horse name
                )
                Text(
                    text = "J; ${horse.horse.jockey}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "W; ${if (horse.horse.weight > 0) "${horse.horse.weight}kg" else "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "T; ${horse.horse.trainer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                if (horse.isStandout) {
                    Text(
                        text = "⭐ Standout (Tie-breaker)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700), // Gold color
                        fontWeight = FontWeight.Medium
                    )
                }

            }

            // Score and Barrier
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format("%.1f", horse.score),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700) // Gold color
                )
                Text(
                    text = "Barrier ${horse.horse.barrier}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
    
    // Show scoring breakdown dialog when tapped
    if (showScoringBreakdown) {
        ScoringBreakdownDialog(
            horse = horse,
            onDismiss = { showScoringBreakdown = false }
        )
    }
}

@Composable
private fun TrackHeader(
    trackName: String,
    raceCount: Int,
    state: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFFFFD700),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏟️",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Track Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = trackName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700) // Gold color
                )
                Text(
                    text = "$raceCount race${if (raceCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // State Badge
            if (state.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = state,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// Scoring Breakdown Dialog
@Composable
private fun ScoringBreakdownDialog(
    horse: ScoredHorse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🏆 ${horse.horse.name} - Scoring Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp) // Limit height to make it scrollable
                    .verticalScroll(rememberScrollState()) // Make it scrollable
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Score Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "💯 TOTAL SCORE: ${String.format("%.1f", horse.score)} points",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // Individual Law Scores
                val breakdown = horse.scoreBreakdown
                
                // Law 1: Recent Form
                ScoringLawRow(
                    lawName = "📊 Recent Form",
                    points = breakdown.recentForm,
                    maxPoints = 25.0,
                    description = "Last 5 races performance"
                )
                
                // Law 2: Class Suitability
                ScoringLawRow(
                    lawName = "🎯 Class Suitability", 
                    points = breakdown.classSuitability,
                    maxPoints = 25.0,
                    description = "Class comparison & performance"
                )
                
                // Law 3: Track/Distance History
                ScoringLawRow(
                    lawName = "🏁 Track/Distance",
                    points = breakdown.trackDistance,
                    maxPoints = 20.0,
                    description = "Performance at this track/distance"
                )
                
                // Law 4: Sectional Time
                ScoringLawRow(
                    lawName = "⚡ Sectional Time",
                    points = breakdown.sectionalTime,
                    maxPoints = 8.0,
                    description = "600m finishing speed bonus"
                )
                
                // Law 5: Barrier
                ScoringLawRow(
                    lawName = "🚪 Barrier",
                    points = breakdown.barrier,
                    maxPoints = 6.0,
                    description = "Barrier position (1-8)"
                )
                
                // Law 6: Jockey
                ScoringLawRow(
                    lawName = "🏇 Jockey",
                    points = breakdown.jockey,
                    maxPoints = 8.0,
                    description = "Jockey premiership ranking"
                )
                
                // Law 7: Trainer
                ScoringLawRow(
                    lawName = "👨‍🏫 Trainer",
                    points = breakdown.trainer,
                    maxPoints = 8.0,
                    description = "Trainer premiership ranking"
                )
                
                // Law 8: Jockey-Trainer Combination
                ScoringLawRow(
                    lawName = "🤝 Combination",
                    points = breakdown.combinationScore,
                    maxPoints = 8.0,
                    description = "Jockey-trainer partnership"
                )
                
                // Scoring Type Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFFFFD700)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "📋 Scoring Type: ${breakdown.type.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.Black,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// Individual Law Score Row
@Composable
private fun ScoringLawRow(
    lawName: String,
    points: Double,
    maxPoints: Double,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, Color(0xFFFFD700)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lawName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", points)} pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "max ${String.format("%.0f", maxPoints)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Helper function to extract state abbreviation from venue name
private fun extractStateFromVenue(venue: String): String {
    return when {
        venue.contains("NSW", ignoreCase = true) -> "NSW"
        venue.contains("VIC", ignoreCase = true) -> "VIC"
        venue.contains("QLD", ignoreCase = true) -> "QLD"
        venue.contains("SA", ignoreCase = true) -> "SA"
        venue.contains("TAS", ignoreCase = true) -> "TAS"
        venue.contains("NT", ignoreCase = true) -> "NT"
        venue.contains("ACT", ignoreCase = true) -> "ACT"
        // More specific WA detection to avoid false matches
        venue.contains("Western Australia", ignoreCase = true) -> "WA"
        venue.contains("Perth", ignoreCase = true) -> "WA"
        venue.contains("Ascot", ignoreCase = true) && venue.contains("WA", ignoreCase = true) -> "WA"
        venue.contains("Belmont", ignoreCase = true) && venue.contains("WA", ignoreCase = true) -> "WA"
        else -> ""
    }
}

// Share functionality
private fun shareResults(
    context: android.content.Context,
    results: List<RaceResult>,
    selectedDate: String,
    selectedTracks: List<String>
) {
    
    // Create professional, readable text content
    val shareText = buildString {
        appendLine("🏇 SteamaTip AI Race Analysis")
        appendLine("📅 Date: $selectedDate")
        appendLine("═".repeat(40))
        appendLine("")
        
        // Group results by track
        val groupedByTrack = results.groupBy { it.race.venue }
        
        groupedByTrack.forEach { (trackName, trackRaces) ->
            val state = extractStateFromVenue(trackName)
            appendLine("🏟️ $trackName ($state)")
            appendLine("─".repeat(30))
            appendLine("Total Races: ${trackRaces.size}")
            appendLine("")
            
            trackRaces.sortedBy { it.race.raceNumber }.forEach { raceResult ->
                appendLine("🏁 RACE ${raceResult.race.raceNumber}")
                appendLine("${raceResult.race.name}")
                appendLine("${raceResult.race.distance}m • ${raceResult.race.time}")
                appendLine("")
                
                appendLine("🎯 TOP SELECTIONS:")
                val top5Horses = raceResult.topSelections.take(5)
                top5Horses.forEachIndexed { index, horse ->
                    val position = when (index) {
                        0 -> "🥇"
                        1 -> "🥈" 
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }
                    val standoutIcon = if (horse.isStandout) " ⭐" else ""
                    appendLine("$position ${horse.horse.name} (${String.format("%.1f", horse.score)} pts)$standoutIcon")
                    appendLine("   J: ${horse.horse.jockey} • T: ${horse.horse.trainer}")
                    appendLine("   Barrier: ${horse.horse.barrier} • Weight: ${horse.horse.weight}kg")
                    horse.horse.odds?.let { odds ->
                        if (odds > 0.0) {
                            appendLine("   Odds: $${String.format("%.2f", odds)}")
                        }
                    }
                    appendLine("")
                }
                appendLine("─".repeat(25))
                appendLine("")
            }
            appendLine("")
        }
        
        appendLine("💡 Analysis powered by SteamaTip AI")
        appendLine("🔬 Using 8 comprehensive racing laws")
        appendLine("")
        appendLine("⭐ = Standout selection based on form")
        appendLine("📊 Scores based on form, class, track record, finishing speed & more")
    }
    
    // Create and launch share intent
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "🏇 SteamaTip AI Analysis - $selectedDate")
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Share Race Analysis"))
}


