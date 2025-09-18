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
            // Create professional CSV file that opens beautifully in Excel
            val fileName = "SteamaTip_BestBets_${selectedDate.replace("-", "")}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { fileWriter ->
                // Write header with proper Excel formatting hints
                fileWriter.write("sep=,\n") // Tell Excel to use comma separator
                
                // Professional header row
                val header = listOf(
                    "Track Name", "Race Number", "Race Name", "Race Time", "Distance", 
                    "Horse Number", "Horse Name", "Score Points", "Bet Classification", 
                    "Jockey Name", "Trainer Name", "Barrier Position", "Horse Weight"
                )
                fileWriter.write(header.joinToString(",") { "\"$it\"" } + "\n")
                
                // Group by track for better organization
                val bestBetsByTrack = bestBets.groupBy { it.first.race.venue }
                
                // Sort tracks alphabetically for consistent presentation
                bestBetsByTrack.toSortedMap().forEach { (trackName, trackBestBets) ->
                    
                    // Sort by race number within each track
                    trackBestBets.sortedBy { it.first.race.raceNumber }.forEach { (raceResult, horse) ->
                        
                        // Determine bet type
                        val betType = if (raceResult.bettingRecommendations.isNotEmpty()) {
                            raceResult.bettingRecommendations[0].betType
                        } else BetType.CONSIDER
                        
                        val betTypeText = when (betType) {
                            BetType.SUPER_BET -> "⭐ SUPER BET"
                            BetType.BEST_BET -> "🔵 BEST BET"
                            BetType.GOOD_BET -> "🟣 GOOD BET"
                            else -> "CONSIDER"
                        }
                        
                        // Create professional data row - properly escape commas and quotes
                        val data = listOf(
                            trackName,
                            raceResult.race.raceNumber.toString(),
                            raceResult.race.name,
                            raceResult.race.time,
                            "${raceResult.race.distance}m",
                            horse.horse.number.toString(),
                            horse.horse.name,
                            String.format("%.1f", horse.score),
                            betTypeText,
                            horse.horse.jockey,
                            horse.horse.trainer,
                            horse.horse.barrier.toString(),
                            "${horse.horse.weight}kg"
                        )
                        
                        // Properly format CSV with quotes to handle commas in names
                        val csvRow = data.joinToString(",") { field ->
                            "\"${field.replace("\"", "\"\"")}\""
                        }
                        fileWriter.write(csvRow + "\n")
                    }
                }
                
                // Add summary footer
                fileWriter.write("\n")
                fileWriter.write("\"Summary\",\"${bestBets.size} Total Best Bets\",\"Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n")
                fileWriter.write("\"Source\",\"SteamaTip AI\",\"Professional Racing Analysis\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"\n")
            }
            
            // Share the file
            shareExcelFile(context, file)
            
        } catch (e: Exception) {
            println("❌ Error creating professional CSV file: ${e.message}")
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
                putExtra(Intent.EXTRA_SUBJECT, "SteamaTip AI - Professional Best Bets Analysis")
                putExtra(Intent.EXTRA_TEXT, "Professional Best Bets analysis exported as Excel-compatible CSV.\n\nFeatures:\n• Color-coded bet types\n• Organized by track\n• Complete horse details\n• Professional formatting\n\nOpen in Excel for best viewing experience.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Professional Excel Report"))
            
        } catch (e: Exception) {
            println("❌ Error sharing Excel file: ${e.message}")
            e.printStackTrace()
        }
    }
}