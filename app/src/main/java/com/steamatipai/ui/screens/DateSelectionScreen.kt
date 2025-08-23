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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

@Composable
fun DateSelectionScreen(
    onDateSelected: (String) -> Unit
) {
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val dayAfter = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    var connectionTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    
    val scrapingService = remember { com.steamatipai.network.ScrapingService() }
    
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
            verticalArrangement = Arrangement.Center
        ) {
                    // App Title
        Text(
            text = "SteamaTip AI",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700), // Gold color
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Horse Racing Analysis",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFFFD700), // Gold color
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Test Connection Button
        Button(
            onClick = {
                isTestingConnection = true
                connectionTestResult = null
                
                // Test connection in background
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = scrapingService.testRacingAustraliaAccess()
                        connectionTestResult = result
                    } catch (e: Exception) {
                        connectionTestResult = "ERROR: ${e.message}"
                    } finally {
                        isTestingConnection = false
                    }
                }
            },
            enabled = !isTestingConnection,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0066CC), // Blue color for test button
                contentColor = Color.White
            )
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Connection...")
            } else {
                Text("Test Racing Australia Connection")
            }
        }
        
        // Connection Test Result
        connectionTestResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.startsWith("SUCCESS")) 
                        Color(0xFF4CAF50).copy(alpha = 0.9f) // Green for success
                    else 
                        Color(0xFFF44336).copy(alpha = 0.9f) // Red for failure
                )
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
            
                    // Date Selection Buttons
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
            modifier = Modifier.padding(bottom = 16.dp)
        )
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color(0xFFFFD700) // Gold color
        ),
        border = BorderStroke(2.dp, Color(0xFFFFD700)) // Gold border
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700) // Gold color
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFD700).copy(alpha = 0.8f) // Slightly transparent gold
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



