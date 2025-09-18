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
import androidx.compose.material.icons.filled.Check
import com.steamatipai.R
import com.steamatipai.data.models.Track
import com.steamatipai.service.RaceAnalysisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackSelectionScreen(
    selectedDate: String,
    onTracksSelected: (List<Track>) -> Unit, // Changed from List<String> to List<Track>
    onBestBetsSelected: (List<Track>) -> Unit, // New callback for Best Bets
    onBack: () -> Unit
) {
    var availableTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var selectedTracks by remember { mutableStateOf<Set<String>>(emptySet()) } // Keep as keys for UI state
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val raceAnalysisService = remember { RaceAnalysisService() }

    // Fetch real data for the selected date
    LaunchedEffect(selectedDate) {
        try {
            println("🔄 TrackSelectionScreen: Starting to fetch tracks for date: $selectedDate")
            isLoading = true
            error = null
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(selectedDate) ?: Date()
            println("📅 Parsed date: $date")
            
            println("🔍 Calling raceAnalysisService.getAvailableTracks...")
            val tracks = withContext(Dispatchers.IO) {
                raceAnalysisService.getAvailableTracks(date)
            }
            println("📊 Received ${tracks.size} tracks from service")
            
            availableTracks = tracks
            
            if (tracks.isEmpty()) {
                println("⚠️ No tracks found, setting error")
                error = "No tracks available for this date. This usually means:\n\n1. No racing scheduled for today\n2. Network connection issue\n3. Racing Australia website is down\n\nPlease check your internet connection and try again."
            } else {
                println("✅ Successfully loaded ${availableTracks.size} tracks")
                // Don't fetch race counts here - this was causing the long delay
                // Race counts will be fetched when actually analyzing the selected tracks
            }
        } catch (e: Exception) {
            println("❌ Error in TrackSelectionScreen: ${e.message}")
            e.printStackTrace()
            error = "Failed to load tracks: ${e.message}\n\nThis could be due to:\n• Network connection issues\n• Racing Australia website being down\n• Anti-scraping measures\n\nPlease check your internet connection and try again later."
            // NO FALLBACK - Use real data only
            availableTracks = emptyList()
        } finally {
            println("🏁 Setting isLoading to false")
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
                .padding(top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack, 
                    contentDescription = "Back",
                    tint = Color(0xFFFFD700) // Gold color
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Tracks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700), // Gold color
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Racing date: $selectedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD700), // Gold color
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        text = "Loading available tracks...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (error != null && availableTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
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
                            // Retry loading
                            isLoading = true
                            error = null
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else if (availableTracks.isEmpty()) {
            // No tracks and no error - this means no real tracks were found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Racing Tracks Available",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFFD700) // Gold color
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No real racing tracks were found for the selected date.\n\nThis could mean:\n• No racing scheduled for today\n• Racing Australia website is down\n• Network connection issues\n\nPlease check your internet connection and try again later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Retry loading
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
                            "Retry",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700) // Gold color
                        )
                    }
                }
            }
        } else {
            // Track List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val groupedTracks = availableTracks.groupBy { it.state }

                groupedTracks.forEach { (state, tracks) ->
                    item {
                        Text(
                            text = state,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700), // Gold color
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(tracks) { track ->
                        TrackItem(
                            track = track,
                            isSelected = selectedTracks.contains(track.key),
                            onSelectionChanged = { isSelected ->
                                selectedTracks = if (isSelected) {
                                    selectedTracks + track.key
                                } else {
                                    selectedTracks - track.key
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(bottom = 32.dp), // Extra bottom padding to move away from phone footer
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Analyze Button (existing functionality)
                Button(
                    onClick = { 
                        // Convert selected track keys to actual track objects
                        val selectedTrackObjects = availableTracks.filter { track ->
                            selectedTracks.contains(track.key)
                        }
                        onTracksSelected(selectedTrackObjects) // Pass actual track objects
                    },
                    enabled = selectedTracks.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color(0xFFFFD700) // Gold color
                    ),
                    border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-4).dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "Analyse ${selectedTracks.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track${if (selectedTracks.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Best Bets Button (new functionality)
                Button(
                    onClick = { 
                        // Convert selected track keys to actual track objects
                        val selectedTrackObjects = availableTracks.filter { track ->
                            selectedTracks.contains(track.key)
                        }
                        onBestBetsSelected(selectedTrackObjects) // Pass to Best Bets flow
                    },
                    enabled = selectedTracks.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black, // Same as Analyse button
                        contentColor = Color(0xFFFFD700) // Gold text
                    ),
                    border = BorderStroke(2.dp, Color(0xFF00FF00)) // Bright green border (same as Super Bet)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-4).dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "BEST",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "BETS",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) }, // Make entire card clickable
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color.Black
            else
                Color.Black.copy(alpha = 0.7f)
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFFFFD700) // Gold color
                    )
                } else {
                    Text(
                        text = "○",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFD700) // Gold color
                    )
                }
            }

            // Track info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFD700) // Gold color
                )
            }
        }
    }
}


