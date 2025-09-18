package com.steamatipai.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.steamatipai.data.models.BetType
import com.steamatipai.data.models.RaceResult
import com.steamatipai.data.models.ScoredHorse
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.util.CellRangeAddress
import java.io.File
import java.io.FileOutputStream

class ExcelExporter {
    
    fun exportBestBetsToExcel(
        context: Context,
        bestBets: List<Pair<RaceResult, ScoredHorse>>,
        selectedDate: String
    ) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Best Bets Analysis")
            
            // Create all required styles
            val styles = createCompleteStyles(workbook, bestBets)
            
            // Create header with filtering enabled
            createProfessionalHeader(sheet, styles.header)
            
            // Add all data with EXACT formatting as requested
            addDataWithExactFormatting(sheet, bestBets, styles)
            
            // Format columns EXACTLY as you specified
            formatColumnsExactly(sheet, bestBets)
            
            // Enable filtering on ALL rows
            enableFiltering(sheet)
            
            // Save as true Excel file
            val fileName = "SteamaTip_BestBets_${selectedDate.replace("-", "")}.xlsx"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            
            shareExcelFile(context, file)
            
        } catch (e: Exception) {
            println("❌ Excel creation failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun createCompleteStyles(workbook: Workbook, bestBets: List<Pair<RaceResult, ScoredHorse>>): ExcelStyles {
        // Header style
        val headerStyle = workbook.createCellStyle()
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerFont.color = IndexedColors.WHITE.index
        headerFont.fontHeightInPoints = 12
        headerStyle.setFont(headerFont)
        headerStyle.fillForegroundColor = IndexedColors.DARK_BLUE.index
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        headerStyle.alignment = HorizontalAlignment.CENTER
        headerStyle.verticalAlignment = VerticalAlignment.CENTER
        addBorders(headerStyle)
        
        // Track color styles (NOT Green/Blue/Purple)
        val trackNames = bestBets.map { it.first.race.venue }.distinct()
        val trackColors = listOf(
            IndexedColors.ORANGE.index,
            IndexedColors.YELLOW.index,
            IndexedColors.TURQUOISE.index,
            IndexedColors.PINK.index,
            IndexedColors.LIME.index,
            IndexedColors.CORAL.index
        )
        
        val trackStyleMap = mutableMapOf<String, CellStyle>()
        trackNames.forEachIndexed { index, trackName ->
            val style = workbook.createCellStyle()
            style.fillForegroundColor = trackColors[index % trackColors.size]
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
            addBorders(style)
            trackStyleMap[trackName] = style
        }
        
        // Bet type color styles (Green/Blue/Purple as requested)
        val superBetStyle = workbook.createCellStyle()
        superBetStyle.fillForegroundColor = IndexedColors.LIGHT_GREEN.index
        superBetStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        superBetStyle.alignment = HorizontalAlignment.CENTER
        addBorders(superBetStyle)
        
        val bestBetStyle = workbook.createCellStyle()
        bestBetStyle.fillForegroundColor = IndexedColors.LIGHT_BLUE.index
        bestBetStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        bestBetStyle.alignment = HorizontalAlignment.CENTER
        addBorders(bestBetStyle)
        
        val goodBetStyle = workbook.createCellStyle()
        goodBetStyle.fillForegroundColor = IndexedColors.LAVENDER.index
        goodBetStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        goodBetStyle.alignment = HorizontalAlignment.CENTER
        addBorders(goodBetStyle)
        
        // Centered style
        val centeredStyle = workbook.createCellStyle()
        centeredStyle.alignment = HorizontalAlignment.CENTER
        centeredStyle.verticalAlignment = VerticalAlignment.CENTER
        addBorders(centeredStyle)
        
        // Text wrap style
        val wrapStyle = workbook.createCellStyle()
        wrapStyle.wrapText = true
        addBorders(wrapStyle)
        
        // Regular style
        val regularStyle = workbook.createCellStyle()
        addBorders(regularStyle)
        
        return ExcelStyles(
            header = headerStyle,
            trackStyles = trackStyleMap,
            superBet = superBetStyle,
            bestBet = bestBetStyle,
            goodBet = goodBetStyle,
            centered = centeredStyle,
            textWrap = wrapStyle,
            regular = regularStyle
        )
    }
    
    private fun createProfessionalHeader(sheet: Sheet, headerStyle: CellStyle) {
        val headerRow = sheet.createRow(0)
        val headers = arrayOf(
            "Track", "Race #", "Race Name", "Time", "Distance", 
            "Horse #", "Horse Name", "Score", "Bet Type", 
            "Jockey", "Trainer", "Barrier", "Weight"
        )
        
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
    }
    
    private fun addDataWithExactFormatting(
        sheet: Sheet, 
        bestBets: List<Pair<RaceResult, ScoredHorse>>, 
        styles: ExcelStyles
    ) {
        var rowIndex = 1
        
        bestBets.sortedWith(compareBy<Pair<RaceResult, ScoredHorse>> { it.first.race.venue }
            .thenBy { it.first.race.raceNumber }).forEach { (raceResult, horse) ->
            
            val row = sheet.createRow(rowIndex++)
            
            val betType = if (raceResult.bettingRecommendations.isNotEmpty()) {
                raceResult.bettingRecommendations[0].betType
            } else BetType.CONSIDER
            
            val betTypeText = when (betType) {
                BetType.SUPER_BET -> "SUPER BET"
                BetType.BEST_BET -> "BEST BET"
                BetType.GOOD_BET -> "GOOD BET"
                else -> "CONSIDER"
            }
            
            val betTypeStyle = when (betType) {
                BetType.SUPER_BET -> styles.superBet
                BetType.BEST_BET -> styles.bestBet
                BetType.GOOD_BET -> styles.goodBet
                else -> styles.centered
            }
            
            val trackStyle = styles.trackStyles[raceResult.race.venue] ?: styles.regular
            
            // EXACT formatting as requested
            val cellConfigs = listOf(
                CellConfig(0, raceResult.race.venue, trackStyle), // A: Track - color coded
                CellConfig(1, raceResult.race.raceNumber.toString(), styles.centered), // B: Centered
                CellConfig(2, raceResult.race.name, styles.textWrap), // C: Wrap text
                CellConfig(3, raceResult.race.time, styles.centered), // D: Centered
                CellConfig(4, "${raceResult.race.distance}m", styles.centered), // E: Centered
                CellConfig(5, horse.horse.number.toString(), styles.centered), // F: Centered
                CellConfig(6, horse.horse.name, styles.regular), // G: Auto-width
                CellConfig(7, String.format("%.1f", horse.score), styles.centered), // H: Centered
                CellConfig(8, betTypeText, betTypeStyle), // I: Color coded bet type
                CellConfig(9, horse.horse.jockey, styles.regular), // J: Auto-width
                CellConfig(10, horse.horse.trainer, styles.regular), // K: Auto-width
                CellConfig(11, horse.horse.barrier.toString(), styles.centered), // L: Centered
                CellConfig(12, "${horse.horse.weight}kg", styles.centered) // M: Centered
            )
            
            cellConfigs.forEach { config ->
                val cell = row.createCell(config.columnIndex)
                cell.setCellValue(config.value)
                cell.cellStyle = config.style
            }
        }
    }
    
    private fun formatColumnsExactly(sheet: Sheet, bestBets: List<Pair<RaceResult, ScoredHorse>>) {
        // Auto-size all columns first
        for (i in 0..12) {
            sheet.autoSizeColumn(i)
        }
        
        // Get maximum lengths for auto-width columns
        val trackNames = bestBets.map { it.first.race.venue }
        val raceNames = bestBets.map { it.first.race.name }
        val horseNames = bestBets.map { it.second.horse.name }
        val jockeyNames = bestBets.map { it.second.horse.jockey }
        val trainerNames = bestBets.map { it.second.horse.trainer }
        
        // Set column widths EXACTLY as requested
        // A: Wide enough for longest track name
        val maxTrackWidth = (trackNames.maxOfOrNull { it.length } ?: 10) * 350
        sheet.setColumnWidth(0, Math.max(4000, maxTrackWidth))
        
        // C: Wide enough for longest race name with wrapping
        val maxRaceNameWidth = (raceNames.maxOfOrNull { it.length } ?: 20) * 300
        sheet.setColumnWidth(2, Math.max(6000, maxRaceNameWidth))
        
        // G: Wide enough for longest horse name
        val maxHorseNameWidth = (horseNames.maxOfOrNull { it.length } ?: 15) * 350
        sheet.setColumnWidth(6, Math.max(4000, maxHorseNameWidth))
        
        // I: Wide enough for longest bet type wording
        sheet.setColumnWidth(8, 4000)
        
        // J: Wide enough for longest jockey name
        val maxJockeyWidth = (jockeyNames.maxOfOrNull { it.length } ?: 15) * 350
        sheet.setColumnWidth(9, Math.max(4000, maxJockeyWidth))
        
        // K: Wide enough for longest trainer name
        val maxTrainerWidth = (trainerNames.maxOfOrNull { it.length } ?: 15) * 350
        sheet.setColumnWidth(10, Math.max(4000, maxTrainerWidth))
    }
    
    private fun enableFiltering(sheet: Sheet) {
        val lastRow = sheet.lastRowNum
        if (lastRow > 0) {
            sheet.setAutoFilter(CellRangeAddress(0, lastRow, 0, 12))
        }
    }
    
    private fun addBorders(style: CellStyle) {
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
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
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SteamaTip AI - Professional Best Bets Analysis")
                putExtra(Intent.EXTRA_TEXT, "Complete Best Bets analysis with ALL formatting:\n• Color-coded tracks\n• Color-coded bet types\n• Auto-sized columns\n• Filtering enabled\n• Professional layout")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Complete Excel Analysis"))
            
        } catch (e: Exception) {
            println("❌ Error sharing Excel file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private data class CellConfig(
        val columnIndex: Int,
        val value: String,
        val style: CellStyle
    )
    
    private data class ExcelStyles(
        val header: CellStyle,
        val trackStyles: Map<String, CellStyle>,
        val superBet: CellStyle,
        val bestBet: CellStyle,
        val goodBet: CellStyle,
        val centered: CellStyle,
        val textWrap: CellStyle,
        val regular: CellStyle
    )
}