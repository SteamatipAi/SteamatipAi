package com.steamatipai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.content.Intent
import androidx.compose.foundation.Image
import com.steamatipai.data.models.ScoredHorse
import com.steamatipai.data.models.RaceResult
import com.steamatipai.data.models.BetType
import com.steamatipai.R

@Composable
fun SingleRaceResultsScreen(
    raceResult: RaceResult,
    onHorseClick: (ScoredHorse) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedHorse by remember { mutableStateOf<ScoredHorse?>(null) }

    // Show horse detail screen if a horse is selected
    if (selectedHorse != null) {
        HorseScoringDetailScreen(
            scoredHorse = selectedHorse!!,
            onBackClick = { selectedHorse = null }
        )
        return
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
                .padding(top = 16.dp, bottom = 12.dp),
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
                    text = "Race ${raceResult.race.raceNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = raceResult.race.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            // Share button
            IconButton(
                onClick = {
                    shareRaceResult(context, raceResult)
                }
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share Race",
                    tint = Color(0xFFFFD700)
                )
            }
        }

        // Race Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = raceResult.race.venue,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = raceResult.race.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${raceResult.race.distance}m • ${raceResult.race.surface} • ${raceResult.race.trackCondition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // All Horses List
        val horsesToShow = if (raceResult.allHorses.isNotEmpty()) raceResult.allHorses else raceResult.topSelections
        Text(
            text = "All Horses (${horsesToShow.size} runners):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp) // Add bottom padding for system UI
        ) {
            itemsIndexed(horsesToShow) { index, horse ->
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
                    onClick = { 
                        selectedHorse = horse
                        onHorseClick(horse)
                    },
                    betType = horseBetType
                )
            }
        }
        }
    }
}

private fun shareRaceResult(context: android.content.Context, raceResult: RaceResult) {
    val shareText = buildString {
        appendLine("🏇 ${raceResult.race.venue} - Race ${raceResult.race.raceNumber}")
        appendLine("📍 ${raceResult.race.name}")
        appendLine("⏰ ${raceResult.race.time} • ${raceResult.race.distance}m")
        appendLine()
        appendLine("🏆 Top Selections:")
        raceResult.topSelections.take(6).forEachIndexed { index, horse ->
            appendLine("${index + 1}. ${horse.horse.name} (${String.format("%.1f", horse.score)} pts)")
        }
        appendLine()
        appendLine("📱 Generated by SteamaTip AI")
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Race ${raceResult.race.raceNumber} Analysis - ${raceResult.race.venue}")
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Race Analysis"))
}
