package com.steamatipai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch
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
import com.steamatipai.data.models.BetType
import com.steamatipai.service.RaceAnalysisService
import com.steamatipai.data.models.Track
import com.steamatipai.R
import java.text.SimpleDateFormat
import java.util.*
import com.steamatipai.network.NetworkConfig
import com.steamatipai.utils.ExcelExporter
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
    var selectedRaceIndex by remember { mutableStateOf(0) }
    var analysisCompleted by remember { mutableStateOf(false) } // Track if analysis is done
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val raceAnalysisService = remember { RaceAnalysisService() }
    val context = LocalContext.current

    // Perform real analysis when the screen loads (only once)
    LaunchedEffect(selectedTracks) {
        // Only run analysis if it hasn't been completed yet
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
                analysisCompleted = true // Mark analysis as completed successfully
            } else {
                error = analysisResult.error ?: "Analysis failed"
                results = emptyList()
                processingTime = 0
                analysisCompleted = true // Mark as completed even if failed to prevent re-running
            }
        } catch (e: Exception) {
            error = "Failed to analyze races: ${e.message}"
            results = emptyList()
            processingTime = 0
            analysisCompleted = true // Mark as completed even if failed to prevent re-running
        } finally {
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
                    text = "Race Analysis Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
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
                        contentDescription = "Share Results",
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
                            shareResults(context, results, selectedDate, selectedTracks, processingTime)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export to Professional Excel") },
                        onClick = {
                            showShareMenu = false
                            exportFullResultsToExcel(context, results, selectedDate)
                        }
                    )
                }
            }
        }

            // Show horse detail screen if a horse is selected
            if (selectedHorse != null) {
                HorseScoringDetailScreen(
                    scoredHorse = selectedHorse!!,
                    onBackClick = { 
                        selectedHorse = null
                        // Scroll back to the race that was selected
                        if (selectedRaceIndex >= 0) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(selectedRaceIndex)
                            }
                        }
                    }
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
                        text = "Analysing races...",
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
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                    // Sort results by race number
                    val sortedResults = results.sortedBy { it.race.raceNumber }
                    
                    // Display each race separately
                    itemsIndexed(sortedResults) { index, raceResult ->
                        RaceResultCard(
                            raceResult = raceResult,
                            onHorseClick = { horse -> 
                                selectedRaceIndex = index
                                selectedHorse = horse 
                            }
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
                // Only the TOP horse (index 0) gets special colors if it qualifies
                val horseBetType = if (index == 0 && raceResult.bettingRecommendations.isNotEmpty()) {
                    val topRecommendation = raceResult.bettingRecommendations[0]
                    // Only apply special colors if it's not CONSIDER (minimal gap)
                    if (topRecommendation.betType != BetType.CONSIDER) {
                        topRecommendation.betType
                    } else {
                        null
                    }
                } else {
                    null
                }
                
                HorseSelectionItem(
                    horse = horse,
                    position = index + 1,
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { onHorseClick(horse) },
                    betType = horseBetType
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
    onClick: () -> Unit = {},
    betType: BetType? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = when (betType) {
            BetType.SUPER_BET -> BorderStroke(
                width = 3.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF00FF00), Color(0xFF00FF80), Color(0xFF00FF00))
                )
            )
            BetType.BEST_BET -> BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF0080FF), Color(0xFF00BFFF), Color(0xFF0080FF))
                )
            )
            BetType.GOOD_BET -> BorderStroke(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF8000FF), Color(0xFFBF00FF), Color(0xFF8000FF))
                )
            )
            else -> BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f))
        }
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

fun shareResults(
    context: android.content.Context,
    results: List<RaceResult>,
    selectedDate: String,
    selectedTracks: List<String>,
    processingTime: Long
) {
    if (results.isEmpty()) {
        return
    }
    
    try {
        val shareText = buildString {
            appendLine("🏇 STEAMA TIP AI - RACE ANALYSIS RESULTS")
            appendLine("═══════════════════════════════════════════")
            appendLine("📅 Date: $selectedDate")
            appendLine("🏁 Tracks: ${selectedTracks.size} selected")
            appendLine("⏱️ Analysis Time: ${processingTime}ms")
            appendLine()
            
            // Group by track for better organization
            val resultsByTrack = results.groupBy { it.race.venue }
            
            resultsByTrack.forEach { (trackName, trackResults) ->
                appendLine("🏟️ $trackName".uppercase())
                appendLine("▔".repeat(30))
                appendLine()
                
                trackResults.sortedBy { it.race.raceNumber }.forEach { raceResult ->
                    // Make race numbers bigger and bolder to stand out
                    appendLine("🏇 ═══ RACE ${raceResult.race.raceNumber} ═══ ${raceResult.race.name}")
                    appendLine("⏰ ${raceResult.race.time} • 📏 ${raceResult.race.distance}m")
                    appendLine()
                    
                    // Show top 6 horses to keep size manageable
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
                                BetType.SUPER_BET -> " 🟢"
                                BetType.BEST_BET -> " 🔵"
                                BetType.GOOD_BET -> " 🟣"
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
            putExtra(Intent.EXTRA_SUBJECT, "Race Analysis Results - $selectedDate")
            // Add flags to prevent issues
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Use try-catch for the actual share action
        try {
            val chooserIntent = Intent.createChooser(shareIntent, "Share Race Analysis Results")
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            println("❌ Error sharing results: ${e.message}")
            // Fallback: try with even shorter text
            val shortText = "🏇 STEAMA TIP AI - RACE ANALYSIS\n📅 $selectedDate\n🏁 ${results.size} races analyzed\n\n📱 Full results available in app"
            val fallbackIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shortText)
                putExtra(Intent.EXTRA_SUBJECT, "Race Analysis - $selectedDate")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val fallbackChooser = Intent.createChooser(fallbackIntent, "Share Race Analysis")
            context.startActivity(fallbackChooser)
        }
        
    } catch (e: Exception) {
        println("❌ Critical error in ResultsScreen share function: ${e.message}")
        e.printStackTrace()
    }
}

fun exportFullResultsToExcel(
    context: android.content.Context,
    results: List<RaceResult>,
    selectedDate: String
) {
    if (results.isEmpty()) {
        return
    }
    
    try {
        // Prepare data for Excel export
        val exportData = mutableListOf<Array<String>>()
        
        // Group results by track for better organization
        val resultsByTrack = results.groupBy { it.race.venue }
        
        resultsByTrack.forEach { (trackName, trackResults) ->
            trackResults.sortedBy { it.race.raceNumber }.forEach { raceResult ->
                // Get all horses for this race
                val allHorses = if (raceResult.allHorses.isNotEmpty()) {
                    raceResult.allHorses
                } else {
                    raceResult.topSelections
                }
                
                allHorses.forEachIndexed { index, horse ->
                    val position = index + 1
                    
                    // Determine bet type for this horse (only top horse gets betting recommendation)
                    val betType = if (index == 0 && raceResult.bettingRecommendations.isNotEmpty()) {
                        val topRecommendation = raceResult.bettingRecommendations[0]
                        when (topRecommendation.betType) {
                            BetType.SUPER_BET -> "Super Bet"
                            BetType.BEST_BET -> "Best Bet"
                            BetType.GOOD_BET -> "Good Bet"
                            else -> ""
                        }
                    } else ""
                    
                    val row = arrayOf(
                        trackName,                                    // Column A: Track
                        raceResult.race.raceNumber.toString(),       // Column B: Race #
                        raceResult.race.name,                        // Column C: Race Name
                        raceResult.race.time,                        // Column D: Time
                        "${raceResult.race.distance}m",             // Column E: Distance
                        horse.horse.number.toString(),              // Column F: Horse #
                        horse.horse.name,                           // Column G: Horse Name
                        String.format("%.1f", horse.score),        // Column H: Score
                        position.toString(),                        // Column I: Position
                        betType,                                    // Column J: Bet Type
                        horse.horse.jockey,                         // Column K: Jockey
                        horse.horse.trainer,                        // Column L: Trainer
                        horse.horse.barrier.toString(),            // Column M: Barrier
                        "${horse.horse.weight}kg"                  // Column N: Weight
                    )
                    exportData.add(row)
                }
            }
        }
        
        // Call the ExcelExporter with the prepared data
        ExcelExporter().exportFullResultsToExcel(context, exportData, selectedDate)
        
    } catch (e: Exception) {
        println("❌ Error exporting full results to Excel: ${e.message}")
        e.printStackTrace()
    }
}



