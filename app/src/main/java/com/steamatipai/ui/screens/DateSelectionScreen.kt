package com.steamatipai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import java.text.SimpleDateFormat
import java.util.*
import com.steamatipai.R

@Composable
fun DateSelectionScreen(
    onDateSelected: (String) -> Unit
) {
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val dayAfter = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - App Title Card (smaller proportion)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(0.7f),
                verticalArrangement = Arrangement.Center
            ) {
                // App Title Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(2.dp, Color(0xFFFFD700))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SteamaTip AI",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Horse Racing Analysis",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFFFD700),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Middle section - Date Selection Buttons (larger proportion)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.Center
            ) {
                DateButton(
                    text = formatDateDisplay(today),
                    subtitle = "",
                    onClick = { onDateSelected(dateFormat.format(today.time)) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                DateButton(
                    text = formatDateDisplay(tomorrow),
                    subtitle = "",
                    onClick = { onDateSelected(dateFormat.format(tomorrow.time)) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                DateButton(
                    text = formatDateDisplay(dayAfter),
                    subtitle = "",
                    onClick = { onDateSelected(dateFormat.format(dayAfter.time)) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Bottom section - Info Text (smaller proportion)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(0.8f),
                verticalArrangement = Arrangement.Center
            ) {
                // Info Text
                Text(
                    text = "Select a racing date to begin analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD700), // Gold color
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DateButton(
    text: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color(0xFFFFD700) // Gold color
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700), // Gold color
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD700).copy(alpha = 0.8f), // Slightly transparent gold
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun formatDateDisplay(calendar: Calendar): String {
    val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
    return "$dayOfWeek, $day $month"
}



