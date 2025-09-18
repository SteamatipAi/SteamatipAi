package com.steamatipai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.steamatipai.data.models.ScoredHorse
import com.steamatipai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseScoringDetailScreen(
    scoredHorse: ScoredHorse,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Backdrop Image
        androidx.compose.foundation.Image(
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
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 80.dp) // Add bottom padding for system UI
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back button on its own line, centered
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(2.dp, Color(0xFFFFD700)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        "← Back to Horses", 
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                // Horse name on its own line with full width
                Text(
                    text = scoredHorse.horse.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            // Horse Basic Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Horse Information",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    
                    Text("Saddle #${scoredHorse.horse.number}", color = Color.White, fontSize = 12.sp)
                    Text("Barrier ${scoredHorse.horse.barrier}", color = Color.White, fontSize = 12.sp)
                    Text("Weight: ${scoredHorse.horse.weight}kg", color = Color.White, fontSize = 12.sp)
                    
                    Text("Jockey: ${scoredHorse.horse.jockey}", color = Color.White, fontSize = 12.sp)
                    Text("Trainer: ${scoredHorse.horse.trainer}", color = Color.White, fontSize = 12.sp)
                }
            }
            
            // Total Score Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(2.dp, Color(0xFFFFD700))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL SCORE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = String.format("%.1f", scoredHorse.score),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "out of 116.0 points",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Scoring Breakdown
            Text(
                text = "Scoring Breakdown by Law",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
            
            // Always show 1st Up and 2nd Up laws first, then all other laws
            // Law 1: 1st Up Performance
            ScoringLawCard(
                lawNumber = 1,
                lawName = "1st Up Performance",
                score = scoredHorse.scoreBreakdown.firstUp,
                maxScore = 8.0,
                description = "Historical 1st Up performance after a spell (active if horse is currently 1st Up)"
            )
            
            // Law 2: 2nd Up Performance
            ScoringLawCard(
                lawNumber = 2,
                lawName = "2nd Up Performance",
                score = scoredHorse.scoreBreakdown.secondUp,
                maxScore = 8.0,
                description = "Historical 2nd Up performance after a spell (active if horse is currently 2nd Up)"
            )
            
            // Law 3: Recent Form
            ScoringLawCard(
                lawNumber = 3,
                lawName = "Recent Form",
                score = scoredHorse.scoreBreakdown.recentForm,
                maxScore = 25.0,
                description = "Performance in last 5 races with recency weighting (not used for spell horses)"
            )
            
            // Law 4: Class Suitability
            ScoringLawCard(
                lawNumber = 4,
                lawName = "Class Suitability",
                score = scoredHorse.scoreBreakdown.classSuitability,
                maxScore = 25.0,
                description = "How well the horse fits the race class"
            )
            
            // Law 5: Track/Distance History
            ScoringLawCard(
                lawNumber = 5,
                lawName = "Track/Distance History",
                score = scoredHorse.scoreBreakdown.trackDistance,
                maxScore = 20.0,
                description = "Past performance at this track and distance"
            )
            
            // Law 6: Sectional Time
            ScoringLawCard(
                lawNumber = 6,
                lawName = "Sectional Time",
                score = scoredHorse.scoreBreakdown.sectionalTime,
                maxScore = 8.0,
                description = "Speed in final 600m of recent races (only for normal/2nd up horses)"
            )
            
            // Law 7: Barrier
            ScoringLawCard(
                lawNumber = 7,
                lawName = "Barrier",
                score = scoredHorse.scoreBreakdown.barrier,
                maxScore = 6.0,
                description = "Starting position advantage"
            )
            
            // Law 8: Jockey
            ScoringLawCard(
                lawNumber = 8,
                lawName = "Jockey",
                score = scoredHorse.scoreBreakdown.jockey,
                maxScore = 8.0,
                description = "Jockey's current form and premiership ranking"
            )
            
            // Law 9: Trainer
            ScoringLawCard(
                lawNumber = 9,
                lawName = "Trainer",
                score = scoredHorse.scoreBreakdown.trainer,
                maxScore = 8.0,
                description = "Trainer's current form and premiership ranking"
            )
            
            // Law 10: Jockey-Horse Relationship
            ScoringLawCard(
                lawNumber = 10,
                lawName = "Jockey-Horse Relationship",
                score = scoredHorse.scoreBreakdown.combination,
                maxScore = 8.0,
                description = "Historical success of jockey with this horse"
            )
            
            // Law 11: Track Condition
            ScoringLawCard(
                lawNumber = 11,
                lawName = "Track Condition",
                score = scoredHorse.scoreBreakdown.trackCondition,
                maxScore = 8.0,
                description = "How well the horse performs on current track condition"
            )
            
            // Additional Info
            if (scoredHorse.scoreBreakdown.type != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFFFFA500).copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = "Scoring Type",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = scoredHorse.scoreBreakdown.type.toString().replace("_", " "),
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringLawCard(
    lawNumber: Int,
    lawName: String,
    score: Double,
    maxScore: Double,
    description: String
) {
    val percentage = if (maxScore > 0) (score / maxScore) * 100 else 0.0
    val color = when {
        percentage >= 80 -> Color(0xFFFFD700) // Gold
        percentage >= 60 -> Color(0xFFFFA500) // Orange
        else -> Color(0xFFFF6B6B) // Light Red
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "L$lawNumber: $lawName",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${String.format("%.1f", score)}/${String.format("%.1f", maxScore)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            // Progress bar
            LinearProgressIndicator(
                progress = (score / maxScore).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = color,
                trackColor = color.copy(alpha = 0.3f)
            )
            
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 12.sp
            )
        }
    }
}
