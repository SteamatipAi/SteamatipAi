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
import com.steamatipai.data.models.ScoredHorse
import com.steamatipai.data.models.BetType
import com.steamatipai.service.RaceAnalysisService
import com.steamatipai.utils.ExcelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BestBetsScreen(
    selectedDate: String,
    selectedTracks: List<String>,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var raceResults by remember { mutableStateOf<List<RaceResult>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var analysisCompleted by remember { mutableStateOf(false) }
    var selectedHorse by remember { mutableStateOf<ScoredHorse?>(null) }
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

    // Show horse detail screen if a horse is selected
    if (selectedHorse != null) {
        HorseScoringDetailScreen(
            scoredHorse = selectedHorse!!,
            onBackClick = { selectedHorse = null }
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

                println("🎯 Starting Best Bets analysis for ${tracks.size} tracks...")
                val analysisResult = withContext(Dispatchers.IO) {
                    raceAnalysisService.analyzeRaces(tracks, date)
                }

                raceResults = analysisResult.results
                analysisCompleted = true
                isLoading = false

                println("✅ Best Bets analysis completed. Found ${analysisResult.results.size} races total")

            } catch (e: Exception) {
                println("❌ Error in BestBetsScreen: ${e.message}")
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
                        text = "Best Bets",
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

                // Share button with dropdown menu
                var showShareMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = {
                            showShareMenu = true
                        }
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share Best Bets",
                            tint = Color(0xFFFFD700)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showShareMenu,
                        onDismissRequest = { showShareMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share as Text") },
                            onClick = {
                                showShareMenu = false
                                shareBestBets(context, raceResults, selectedDate)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to Professional Excel") },
                            onClick = {
                                showShareMenu = false
                                val bestBets = filterBestBets(raceResults)
                                ExcelExporter().exportBestBetsToExcel(context, bestBets, selectedDate)
                            }
                        )
                    }
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
                            text = "Finding your best bets...",
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
                    }
                }
            } else {
                // Filter and display best bets only
                val bestBetsResults = filterBestBets(raceResults)
                
                if (bestBetsResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Best Bets Found",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No horses met the criteria for Super Bets, Best Bets, or Good Bets today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Best Bets List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // Add bottom padding for system UI
                    ) {
                        // Group by track
                        val bestBetsByTrack = bestBetsResults.groupBy { it.first.race.venue }
                        
                        bestBetsByTrack.forEach { (trackName, bestBets) ->
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
                            
                            items(bestBets) { (raceResult, bestBetHorse) ->
                                BestBetHorseCard(
                                    raceResult = raceResult,
                                    horse = bestBetHorse,
                                    onClick = { selectedHorse = bestBetHorse }
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
}

// Filter races to only include horses with best bet recommendations
private fun filterBestBets(raceResults: List<RaceResult>): List<Pair<RaceResult, ScoredHorse>> {
    val bestBets = mutableListOf<Pair<RaceResult, ScoredHorse>>()
    
    raceResults.forEach { raceResult ->
        // Check all horses in the race for best bet recommendations
        val allHorses = if (raceResult.allHorses.isNotEmpty()) raceResult.allHorses else raceResult.topSelections
        
        allHorses.forEachIndexed { index, horse ->
            // Check if this is the top horse with a betting recommendation
            if (index == 0 && raceResult.bettingRecommendations.isNotEmpty()) {
                val topRecommendation = raceResult.bettingRecommendations[0]
                when (topRecommendation.betType) {
                    BetType.SUPER_BET, BetType.BEST_BET, BetType.GOOD_BET -> {
                        bestBets.add(Pair(raceResult, horse))
                    }
                    else -> { /* Not a best bet */ }
                }
            }
        }
    }
    
    return bestBets
}

@Composable
fun BestBetHorseCard(
    raceResult: RaceResult,
    horse: ScoredHorse,
    onClick: () -> Unit
) {
    // Determine bet type for coloring
    val betType = if (raceResult.bettingRecommendations.isNotEmpty()) {
        raceResult.bettingRecommendations[0].betType
    } else BetType.CONSIDER
    
    val borderColor = when (betType) {
        BetType.SUPER_BET -> Color(0xFF00FF00) // Green
        BetType.BEST_BET -> Color(0xFF0080FF) // Blue
        BetType.GOOD_BET -> Color(0xFF8000FF) // Purple
        else -> Color(0xFFFFD700) // Gold fallback
    }
    
    val betTypeText = when (betType) {
        BetType.SUPER_BET -> "⭐ SUPER BET"
        BetType.BEST_BET -> "🔵 BEST BET"
        BetType.GOOD_BET -> "🟣 GOOD BET"
        else -> "CONSIDER"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Race info
            Text(
                text = "Race ${raceResult.race.raceNumber}: ${raceResult.race.name}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "${raceResult.race.time} • ${raceResult.race.distance}m",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Bet type indicator
            Text(
                text = betTypeText,
                style = MaterialTheme.typography.labelSmall,
                color = borderColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Horse details
            Text(
                text = "#${horse.horse.number} ${horse.horse.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Score: ${String.format("%.1f", horse.score)} points",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "J: ${horse.horse.jockey}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Text(
                text = "T: ${horse.horse.trainer}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Text(
                text = "Barrier: ${horse.horse.barrier} • Weight: ${horse.horse.weight}kg",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

fun shareBestBets(
    context: android.content.Context,
    raceResults: List<RaceResult>,
    selectedDate: String
) {
    val bestBets = filterBestBets(raceResults)
    
    if (bestBets.isEmpty()) {
        return
    }
    
    try {
        val shareText = buildString {
            appendLine("🎯 STEAMA TIP AI - BEST BETS")
            appendLine("═══════════════════════════════════════════")
            appendLine("📅 Date: $selectedDate")
            appendLine("🏆 Total Best Bets: ${bestBets.size}")
            appendLine()
            
            // Group by track
            val bestBetsByTrack = bestBets.groupBy { it.first.race.venue }
            
            bestBetsByTrack.forEach { (trackName, trackBestBets) ->
                appendLine("🏟️ $trackName".uppercase())
                appendLine("▔".repeat(30))
                appendLine()
                
                trackBestBets.forEach { (raceResult, horse) ->
                    val betType = if (raceResult.bettingRecommendations.isNotEmpty()) {
                        raceResult.bettingRecommendations[0].betType
                    } else BetType.CONSIDER
                    
                    val betTypeText = when (betType) {
                        BetType.SUPER_BET -> "⭐ SUPER BET ⭐"
                        BetType.BEST_BET -> "🔵 BEST BET"
                        BetType.GOOD_BET -> "🟣 GOOD BET"
                        else -> "CONSIDER"
                    }
                    
                    appendLine("🏇 Race ${raceResult.race.raceNumber}: ${raceResult.race.name}")
                    appendLine("⏰ ${raceResult.race.time} • 📏 ${raceResult.race.distance}m")
                    appendLine()
                    appendLine("$betTypeText")
                    appendLine("#${horse.horse.number} ${horse.horse.name}")
                    appendLine("💯 Score: ${String.format("%.1f", horse.score)} points")
                    appendLine("🏇 J: ${horse.horse.jockey}")
                    appendLine("👨‍🏫 T: ${horse.horse.trainer}")
                    appendLine("🚪 Barrier: ${horse.horse.barrier} • ⚖️ Weight: ${horse.horse.weight}kg")
                    appendLine()
                    appendLine("═".repeat(30))
                    appendLine()
                }
            }
            
            appendLine("📱 Generated by SteamaTip AI")
            appendLine("🎯 Best Bets Analysis - Only the strongest recommendations")
        }
        
        // Check text size and truncate if necessary (Android has ~1MB limit for Intent extras)
        val finalText = if (shareText.length > 50000) {
            shareText.take(47000) + "\n\n... (Content truncated for sharing)\n📱 Generated by SteamaTip AI"
        } else {
            shareText
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, finalText)
            putExtra(Intent.EXTRA_SUBJECT, "Best Bets Analysis - $selectedDate")
            // Add flags to prevent issues
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Use try-catch for the actual share action
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Best Bets"))
        } catch (e: Exception) {
            println("❌ Error sharing Best Bets: ${e.message}")
            // Fallback: try with even shorter text
            val shortText = "🎯 STEAMA TIP AI - BEST BETS\n📅 $selectedDate\n🏆 ${bestBets.size} best bets found\n\n📱 Full results available in app"
            val fallbackIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shortText)
                putExtra(Intent.EXTRA_SUBJECT, "Best Bets - $selectedDate")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share Best Bets"))
        }
        
    } catch (e: Exception) {
        println("❌ Critical error in Best Bets share function: ${e.message}")
        e.printStackTrace()
    }
}
