package com.steamatipai.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.steamatipai.data.models.BetType
import com.steamatipai.data.models.RaceResult
import com.steamatipai.data.models.ScoredHorse
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ExcelExporter {
    
    fun exportBestBetsToExcel(
        context: Context,
        bestBets: List<Pair<RaceResult, ScoredHorse>>,
        selectedDate: String
    ) {
        try {
            // Create CSV file that can be opened in Excel
            val fileName = "SteamaTip_BestBets_${selectedDate.replace("-", "")}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { fileWriter ->
                // Write header
                val header = listOf(
                    "Track", "Race #", "Race Name", "Time", "Distance", 
                    "Horse #", "Horse Name", "Score", "Bet Type", 
                    "Jockey", "Trainer", "Barrier", "Weight"
                )
                fileWriter.write(header.joinToString(",") + "\n")
                
                // Group by track for better organization
                val bestBetsByTrack = bestBets.groupBy { it.first.race.venue }
                
                bestBetsByTrack.forEach { (trackName, trackBestBets) ->
                    trackBestBets.forEach { (raceResult, horse) ->
                        // Determine bet type
                        val betType = if (raceResult.bettingRecommendations.isNotEmpty()) {
                            raceResult.bettingRecommendations[0].betType
                        } else BetType.CONSIDER
                        
                        val betTypeText = when (betType) {
                            BetType.SUPER_BET -> "SUPER BET"
                            BetType.BEST_BET -> "BEST BET"
                            BetType.GOOD_BET -> "GOOD BET"
                            else -> "CONSIDER"
                        }
                        
                        // Write data row - escape commas in text fields
                        val data = listOf(
                            trackName.replace(",", ";"),
                            raceResult.race.raceNumber.toString(),
                            raceResult.race.name.replace(",", ";"),
                            raceResult.race.time,
                            "${raceResult.race.distance}m",
                            horse.horse.number.toString(),
                            horse.horse.name.replace(",", ";"),
                            String.format("%.1f", horse.score),
                            betTypeText,
                            horse.horse.jockey.replace(",", ";"),
                            horse.horse.trainer.replace(",", ";"),
                            horse.horse.barrier.toString(),
                            "${horse.horse.weight}kg"
                        )
                        
                        fileWriter.write(data.joinToString(",") + "\n")
                    }
                }
            }
            
            // Share the file
            shareExcelFile(context, file)
            
        } catch (e: Exception) {
            println("❌ Error creating CSV file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun shareExcelFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SteamaTip AI - Best Bets Analysis")
                putExtra(Intent.EXTRA_TEXT, "Best Bets analysis exported as CSV. Can be opened in Excel.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Best Bets (Excel Compatible)"))
            
        } catch (e: Exception) {
            println("❌ Error sharing CSV file: ${e.message}")
            e.printStackTrace()
        }
    }
}
