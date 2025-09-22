package com.steamatipai.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aspose.cells.*
import com.steamatipai.data.models.BetType
import com.steamatipai.data.models.RaceResult
import com.steamatipai.data.models.ScoredHorse
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional Excel exporter using Aspose.Cells for Android
 * Creates real .xlsx files with ALL your formatting requirements automatically applied
 */
class ExcelExporter {
    
    fun exportBestBetsToExcel(
        context: Context,
        bestBets: List<Pair<RaceResult, ScoredHorse>>,
        selectedDate: String
    ) {
        try {
            // Initialize Aspose.Cells workbook
            val workbook = Workbook()
            val worksheet = workbook.worksheets[0]
            worksheet.name = "Best Bets Analysis"
            
            // Create all professional styles with your EXACT requirements
            val styles = createCompleteStyles(workbook, bestBets)
            
            // Create header with professional formatting
            createProfessionalHeader(worksheet, styles)
            
            // Add all data with EXACT formatting as you requested
            addDataWithExactFormatting(worksheet, bestBets, styles)
            
            // Format columns EXACTLY as you specified
            formatColumnsExactly(worksheet, bestBets)
            
            // Enable filtering on ALL rows
            enableFiltering(worksheet)
            
            // Save as true Excel file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SteamaTip_BestBets_Professional_$timestamp.xlsx"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            workbook.save(file.absolutePath, SaveFormat.XLSX)
            workbook.dispose()
            
            shareExcelFile(context, file)
            
        } catch (e: Exception) {
            println("❌ Excel creation failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun createCompleteStyles(workbook: Workbook, bestBets: List<Pair<RaceResult, ScoredHorse>>): ExcelStyles {
        // Header style - Bold white on dark blue
        val headerStyle = workbook.createStyle()
        headerStyle.font.isBold = true
        headerStyle.font.color = Color.getWhite()
        headerStyle.font.size = 12
        headerStyle.foregroundColor = Color.getDarkBlue()
        headerStyle.pattern = BackgroundType.SOLID
        headerStyle.horizontalAlignment = TextAlignmentType.CENTER
        headerStyle.verticalAlignment = TextAlignmentType.CENTER
        addBorders(headerStyle)
        
        // Track color styles (avoiding green/blue/purple as requested)
        val trackNames = bestBets.map { it.first.race.venue }.distinct()
        val trackColors = listOf(
            Color.getOrange(),
            Color.getYellow(),
            Color.getTurquoise(),
            Color.getPink(),
            Color.getLightGray(),
            Color.getLavender()
        )
        
        val trackStyleMap = mutableMapOf<String, Style>()
        trackNames.forEachIndexed { index, trackName ->
            val style = workbook.createStyle()
            style.font.isBold = true
            style.foregroundColor = trackColors[index % trackColors.size]
            style.pattern = BackgroundType.SOLID
            style.horizontalAlignment = TextAlignmentType.CENTER
            style.verticalAlignment = TextAlignmentType.CENTER
            addBorders(style)
            trackStyleMap[trackName] = style
        }
        
        // Bet type color styles (Green/Blue/Purple as you specified)
        val superBetStyle = workbook.createStyle()
        superBetStyle.font.isBold = true
        superBetStyle.font.color = Color.getWhite()
        superBetStyle.foregroundColor = Color.getGreen()  // Super Bet = Green
        superBetStyle.pattern = BackgroundType.SOLID
        superBetStyle.horizontalAlignment = TextAlignmentType.CENTER
        superBetStyle.verticalAlignment = TextAlignmentType.CENTER
        addBorders(superBetStyle)
        
        val bestBetStyle = workbook.createStyle()
        bestBetStyle.font.isBold = true
        bestBetStyle.font.color = Color.getWhite()
        bestBetStyle.foregroundColor = Color.getBlue()   // Best Bet = Blue
        bestBetStyle.pattern = BackgroundType.SOLID
        bestBetStyle.horizontalAlignment = TextAlignmentType.CENTER
        bestBetStyle.verticalAlignment = TextAlignmentType.CENTER
        addBorders(bestBetStyle)
        
        val goodBetStyle = workbook.createStyle()
        goodBetStyle.font.isBold = true
        goodBetStyle.font.color = Color.getWhite()
        goodBetStyle.foregroundColor = Color.getPurple()  // Good Bet = Purple
        goodBetStyle.pattern = BackgroundType.SOLID
        goodBetStyle.horizontalAlignment = TextAlignmentType.CENTER
        goodBetStyle.verticalAlignment = TextAlignmentType.CENTER
        addBorders(goodBetStyle)
        
        // Centered style for specific columns
        val centeredStyle = workbook.createStyle()
        centeredStyle.horizontalAlignment = TextAlignmentType.CENTER
        centeredStyle.verticalAlignment = TextAlignmentType.CENTER
        addBorders(centeredStyle)
        
        // Text wrap style for race names
        val wrapStyle = workbook.createStyle()
        wrapStyle.isTextWrapped = true
        wrapStyle.verticalAlignment = TextAlignmentType.CENTER
        addBorders(wrapStyle)
        
        // Regular style with borders
        val regularStyle = workbook.createStyle()
        regularStyle.verticalAlignment = TextAlignmentType.CENTER
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
    
fun exportFullResultsToExcel(
    context: Context,
    exportData: List<Array<String>>,
    selectedDate: String
) {
    try {
        // Initialize Aspose.Cells workbook
        val workbook = Workbook()
        val worksheet = workbook.worksheets[0]
        worksheet.name = "Complete Race Analysis"
        
        // Create all professional styles
        val styles = createFullResultsStyles(workbook, exportData)
        
        // Create header with professional formatting
        createFullResultsHeader(worksheet, styles)
        
        // Add all data with EXACT formatting
        addFullResultsData(worksheet, exportData, styles)
        
        // Format columns EXACTLY as specified
        formatFullResultsColumns(worksheet, exportData)
        
        // Enable filtering on ALL rows
        enableFullResultsFiltering(worksheet)
        
        // Save as true Excel file with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SteamaTip_FullResults_Professional_$timestamp.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        workbook.save(file.absolutePath, SaveFormat.XLSX)
        
        // Check file size before sharing
        val fileSizeMB = file.length() / (1024.0 * 1024.0)
        println("📊 Excel file created: ${String.format("%.2f", fileSizeMB)} MB")
        
        workbook.dispose()
        
        shareFullResultsExcelFile(context, file, selectedDate, fileSizeMB)
        
    } catch (e: Exception) {
        println("❌ Full Results Excel creation failed: ${e.message}")
        e.printStackTrace()
    }
}

private fun createFullResultsStyles(workbook: Workbook, exportData: List<Array<String>>): FullResultsStyles {
    // Header style - Bold white on dark blue
    val headerStyle = workbook.createStyle()
    headerStyle.font.isBold = true
    headerStyle.font.color = Color.getWhite()
    headerStyle.font.size = 12
    headerStyle.foregroundColor = Color.getDarkBlue()
    headerStyle.pattern = BackgroundType.SOLID
    headerStyle.horizontalAlignment = TextAlignmentType.CENTER
    headerStyle.verticalAlignment = TextAlignmentType.CENTER
    addBorders(headerStyle)
    
    // Track color styles (avoiding green/blue/purple as requested)
    val trackNames = exportData.map { it[0] }.distinct()
    val trackColors = listOf(
        Color.getOrange(),
        Color.getYellow(),
        Color.getTurquoise(),
        Color.getPink(),
        Color.getLightGray(),
        Color.getLavender()
    )
    
    val trackStyleMap = mutableMapOf<String, Style>()
    trackNames.forEachIndexed { index, trackName ->
        val style = workbook.createStyle()
        style.font.isBold = true
        style.foregroundColor = trackColors[index % trackColors.size]
        style.pattern = BackgroundType.SOLID
        style.horizontalAlignment = TextAlignmentType.CENTER
        style.verticalAlignment = TextAlignmentType.CENTER
        addBorders(style)
        trackStyleMap[trackName] = style
    }
    
    // Bet type color styles (Green/Blue/Purple as you specified)
    val superBetStyle = workbook.createStyle()
    superBetStyle.font.isBold = true
    superBetStyle.font.color = Color.getWhite()
    superBetStyle.foregroundColor = Color.getGreen()  // Super Bet = Green
    superBetStyle.pattern = BackgroundType.SOLID
    superBetStyle.horizontalAlignment = TextAlignmentType.CENTER
    superBetStyle.verticalAlignment = TextAlignmentType.CENTER
    addBorders(superBetStyle)
    
    val bestBetStyle = workbook.createStyle()
    bestBetStyle.font.isBold = true
    bestBetStyle.font.color = Color.getWhite()
    bestBetStyle.foregroundColor = Color.getBlue()   // Best Bet = Blue
    bestBetStyle.pattern = BackgroundType.SOLID
    bestBetStyle.horizontalAlignment = TextAlignmentType.CENTER
    bestBetStyle.verticalAlignment = TextAlignmentType.CENTER
    addBorders(bestBetStyle)
    
    val goodBetStyle = workbook.createStyle()
    goodBetStyle.font.isBold = true
    goodBetStyle.font.color = Color.getWhite()
    goodBetStyle.foregroundColor = Color.getPurple()  // Good Bet = Purple
    goodBetStyle.pattern = BackgroundType.SOLID
    goodBetStyle.horizontalAlignment = TextAlignmentType.CENTER
    goodBetStyle.verticalAlignment = TextAlignmentType.CENTER
    addBorders(goodBetStyle)
    
    // Centered style for specific columns
    val centeredStyle = workbook.createStyle()
    centeredStyle.horizontalAlignment = TextAlignmentType.CENTER
    centeredStyle.verticalAlignment = TextAlignmentType.CENTER
    addBorders(centeredStyle)
    
    // Text wrap style for race names
    val wrapStyle = workbook.createStyle()
    wrapStyle.isTextWrapped = true
    wrapStyle.verticalAlignment = TextAlignmentType.CENTER
    addBorders(wrapStyle)
    
    // Regular style with borders
    val regularStyle = workbook.createStyle()
    regularStyle.verticalAlignment = TextAlignmentType.CENTER
    addBorders(regularStyle)
    
    return FullResultsStyles(
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

private fun createFullResultsHeader(worksheet: Worksheet, styles: FullResultsStyles) {
    val headers = arrayOf(
        "Track", "Race #", "Race Name", "Time", "Distance", 
        "Horse #", "Horse Name", "Score", "Position", "Bet Type", 
        "Jockey", "Trainer", "Barrier", "Weight"
    )
    
    headers.forEachIndexed { index, header ->
        val cell = worksheet.cells[0, index]
        cell.putValue(header)
        cell.setStyle(styles.header)
    }
}

private fun addFullResultsData(
    worksheet: Worksheet, 
    exportData: List<Array<String>>, 
    styles: FullResultsStyles
) {
    exportData.forEachIndexed { index, row ->
        val rowIndex = index + 1
        
        // Determine bet type style
        val betTypeText = row[9] // Column J: Bet Type
        val betTypeStyle = when (betTypeText) {
            "Super Bet" -> styles.superBet
            "Best Bet" -> styles.bestBet
            "Good Bet" -> styles.goodBet
            else -> styles.centered
        }
        
        val trackStyle = styles.trackStyles[row[0]] ?: styles.regular
        
        // Apply EXACT formatting as you specified:
        val cellConfigs = listOf(
            CellConfig(0, row[0], trackStyle),                      // A: Track - color coded
            CellConfig(1, row[1], styles.centered),                // B: Race # - centered
            CellConfig(2, row[2], styles.textWrap),                // C: Race name - wrapped
            CellConfig(3, row[3], styles.centered),                // D: Time - centered
            CellConfig(4, row[4], styles.centered),                // E: Distance - centered
            CellConfig(5, row[5], styles.centered),                // F: Horse # - centered
            CellConfig(6, row[6], styles.regular),                 // G: Horse name - auto-width
            CellConfig(7, row[7], styles.centered),                // H: Score - centered
            CellConfig(8, row[8], styles.centered),                // I: Position - centered
            CellConfig(9, row[9], betTypeStyle),                   // J: Bet type - color coded
            CellConfig(10, row[10], styles.regular),               // K: Jockey - auto-width
            CellConfig(11, row[11], styles.regular),               // L: Trainer - auto-width
            CellConfig(12, row[12], styles.centered),              // M: Barrier - centered
            CellConfig(13, row[13], styles.centered)               // N: Weight - centered
        )
        
        cellConfigs.forEach { config ->
            val cell = worksheet.cells[rowIndex, config.columnIndex]
            cell.putValue(config.value)
            cell.setStyle(config.style)
        }
    }
    
    // Add summary footer
    val summaryRow = exportData.size + 2
    val summaryCell = worksheet.cells[summaryRow, 0]
    summaryCell.putValue("Total Horses: ${exportData.size}")
    summaryCell.setStyle(styles.header)
    
    val timestampCell = worksheet.cells[summaryRow + 1, 0]
    timestampCell.putValue("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
}

private fun formatFullResultsColumns(worksheet: Worksheet, exportData: List<Array<String>>) {
    // Calculate maximum lengths for auto-width columns
    val trackNames = exportData.map { it[0] }
    val raceNames = exportData.map { it[2] }
    val horseNames = exportData.map { it[6] }
    val jockeyNames = exportData.map { it[10] }
    val trainerNames = exportData.map { it[11] }
    
    // Auto-fit all columns first
    worksheet.autoFitColumns()
    
    // Set specific widths as you requested:
    // A: Wide enough for longest track name + color coding
    val maxTrackWidth = (trackNames.maxOfOrNull { it.length } ?: 15) * 1.5
    worksheet.cells.setColumnWidthPixel(0, (maxTrackWidth * 8).toInt().coerceAtLeast(120))
    
    // C: Wide enough for longest race name with wrapping
    val maxRaceWidth = (raceNames.maxOfOrNull { it.length } ?: 25) * 1.2
    worksheet.cells.setColumnWidthPixel(2, (maxRaceWidth * 8).toInt().coerceAtLeast(200))
    
    // G: Wide enough for longest horse name
    val maxHorseWidth = (horseNames.maxOfOrNull { it.length } ?: 20) * 1.2
    worksheet.cells.setColumnWidthPixel(6, (maxHorseWidth * 8).toInt().coerceAtLeast(150))
    
    // J: Wide enough for bet type wording + color coding
    worksheet.cells.setColumnWidthPixel(9, 120)
    
    // K: Wide enough for longest jockey name
    val maxJockeyWidth = (jockeyNames.maxOfOrNull { it.length } ?: 15) * 1.2
    worksheet.cells.setColumnWidthPixel(10, (maxJockeyWidth * 8).toInt().coerceAtLeast(120))
    
    // L: Wide enough for longest trainer name
    val maxTrainerWidth = (trainerNames.maxOfOrNull { it.length } ?: 15) * 1.2
    worksheet.cells.setColumnWidthPixel(11, (maxTrainerWidth * 8).toInt().coerceAtLeast(120))
}

private fun enableFullResultsFiltering(worksheet: Worksheet) {
    val lastRow = worksheet.cells.maxDataRow
    if (lastRow > 0) {
        worksheet.autoFilter.range = "A1:N${lastRow + 1}"
    }
}

private fun shareFullResultsExcelFile(context: Context, file: File, selectedDate: String, fileSizeMB: Double) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // Check if file is too large for email (most email providers limit to 10-25MB)
        val isLargeFile = fileSizeMB > 10.0
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SteamaTip AI - Complete Professional Race Analysis - $selectedDate")
            putExtra(Intent.EXTRA_TEXT, "Complete Professional Excel Analysis with ALL formatting automatically applied:\n" +
                    "✅ All rows with filtering enabled\n" +
                    "✅ Track names auto-width + color coded\n" +
                    "✅ Race numbers centered\n" +
                    "✅ Race names auto-width with text wrapping\n" +
                    "✅ Time, Distance, Horse# centered\n" +
                    "✅ Horse names auto-width\n" +
                    "✅ Scores and positions centered\n" +
                    "✅ Bet types color coded (Green=Super, Blue=Best, Purple=Good)\n" +
                    "✅ Jockey/Trainer names auto-width\n" +
                    "✅ Barrier/Weight centered\n" +
                    "✅ True Excel .xlsx format with ALL formatting automatically applied\n" +
                    "✅ Complete field analysis with ALL horses included\n" +
                    "📊 File size: ${String.format("%.0f", fileSizeMB * 1024)} KB")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (isLargeFile) {
            println("⚠️ Large Excel file (${String.format("%.1f", fileSizeMB)}MB) - may have email attachment issues")
        }
        
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Professional Complete Analysis"))
            println("✅ Excel file shared successfully (${String.format("%.0f", fileSizeMB * 1024)} KB)")
        } catch (e: Exception) {
            println("❌ Primary Excel sharing failed: ${e.message}")
            // Fallback: Try with generic application type
            try {
                val fallbackIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "application/octet-stream" // Generic binary file type
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "SteamaTip AI - Complete Race Analysis Excel - $selectedDate")
                    putExtra(Intent.EXTRA_TEXT, "Professional Excel Analysis (${String.format("%.0f", fileSizeMB * 1024)} KB)\n\nComplete race analysis with professional formatting.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Share Excel File"))
                println("✅ Excel file shared via fallback method")
            } catch (e2: Exception) {
                println("❌ All Excel sharing methods failed: ${e2.message}")
                e2.printStackTrace()
            }
        }
        
    } catch (e: Exception) {
        println("❌ Error sharing Full Results Excel file: ${e.message}")
        e.printStackTrace()
    }
}

private data class FullResultsStyles(
    val header: Style,
    val trackStyles: Map<String, Style>,
    val superBet: Style,
    val bestBet: Style,
    val goodBet: Style,
    val centered: Style,
    val textWrap: Style,
    val regular: Style
)

private fun createProfessionalHeader(worksheet: Worksheet, styles: ExcelStyles) {
    val headers = arrayOf(
        "Track", "Race #", "Race Name", "Time", "Distance", 
        "Horse #", "Horse Name", "Score", "Bet Type", 
        "Jockey", "Trainer", "Barrier", "Weight"
    )
    
    headers.forEachIndexed { index, header ->
        val cell = worksheet.cells[0, index]
        cell.putValue(header)
        cell.setStyle(styles.header)
    }
}
    
    private fun addDataWithExactFormatting(
        worksheet: Worksheet, 
        bestBets: List<Pair<RaceResult, ScoredHorse>>, 
        styles: ExcelStyles
    ) {
        // Sort data by track then race number for organized display
        val sortedBets = bestBets.sortedWith(
            compareBy<Pair<RaceResult, ScoredHorse>> { it.first.race.venue }
                .thenBy { it.first.race.raceNumber }
        )
        
        sortedBets.forEachIndexed { index, (raceResult, horse) ->
            val rowIndex = index + 1
            
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
            
            // Apply EXACT formatting as you specified:
            val cellConfigs = listOf(
                CellConfig(0, raceResult.race.venue, trackStyle),                    // A: Track - color coded
                CellConfig(1, raceResult.race.raceNumber.toString(), styles.centered), // B: Race # - centered
                CellConfig(2, raceResult.race.name, styles.textWrap),                // C: Race name - wrapped
                CellConfig(3, raceResult.race.time, styles.centered),               // D: Time - centered
                CellConfig(4, "${raceResult.race.distance}m", styles.centered),     // E: Distance - centered
                CellConfig(5, horse.horse.number.toString(), styles.centered),      // F: Horse # - centered
                CellConfig(6, horse.horse.name, styles.regular),                    // G: Horse name - auto-width
                CellConfig(7, String.format("%.1f", horse.score), styles.centered), // H: Score - centered
                CellConfig(8, betTypeText, betTypeStyle),                           // I: Bet type - color coded
                CellConfig(9, horse.horse.jockey, styles.regular),                  // J: Jockey - auto-width
                CellConfig(10, horse.horse.trainer, styles.regular),                // K: Trainer - auto-width
                CellConfig(11, horse.horse.barrier.toString(), styles.centered),    // L: Barrier - centered
                CellConfig(12, "${horse.horse.weight}kg", styles.centered)          // M: Weight - centered
            )
            
            cellConfigs.forEach { config ->
                val cell = worksheet.cells[rowIndex, config.columnIndex]
                cell.putValue(config.value)
                cell.setStyle(config.style)
            }
        }
        
        // Add summary footer
        val summaryRow = bestBets.size + 2
        val summaryCell = worksheet.cells[summaryRow, 0]
        summaryCell.putValue("Total Best Bets: ${bestBets.size}")
        summaryCell.setStyle(styles.header)
        
        val timestampCell = worksheet.cells[summaryRow + 1, 0]
        timestampCell.putValue("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
    }
    
    private fun formatColumnsExactly(worksheet: Worksheet, bestBets: List<Pair<RaceResult, ScoredHorse>>) {
        // Calculate maximum lengths for auto-width columns
        val trackNames = bestBets.map { it.first.race.venue }
        val raceNames = bestBets.map { it.first.race.name }
        val horseNames = bestBets.map { it.second.horse.name }
        val jockeyNames = bestBets.map { it.second.horse.jockey }
        val trainerNames = bestBets.map { it.second.horse.trainer }
        
        // Auto-fit all columns first
        worksheet.autoFitColumns()
        
        // Set specific widths as you requested:
        // A: Wide enough for longest track name + color coding
        val maxTrackWidth = (trackNames.maxOfOrNull { it.length } ?: 15) * 1.5
        worksheet.cells.setColumnWidthPixel(0, (maxTrackWidth * 8).toInt().coerceAtLeast(120))
        
        // C: Wide enough for longest race name with wrapping
        val maxRaceWidth = (raceNames.maxOfOrNull { it.length } ?: 25) * 1.2
        worksheet.cells.setColumnWidthPixel(2, (maxRaceWidth * 8).toInt().coerceAtLeast(200))
        
        // G: Wide enough for longest horse name
        val maxHorseWidth = (horseNames.maxOfOrNull { it.length } ?: 20) * 1.2
        worksheet.cells.setColumnWidthPixel(6, (maxHorseWidth * 8).toInt().coerceAtLeast(150))
        
        // I: Wide enough for bet type wording + color coding
        worksheet.cells.setColumnWidthPixel(8, 120)
        
        // J: Wide enough for longest jockey name
        val maxJockeyWidth = (jockeyNames.maxOfOrNull { it.length } ?: 15) * 1.2
        worksheet.cells.setColumnWidthPixel(9, (maxJockeyWidth * 8).toInt().coerceAtLeast(120))
        
        // K: Wide enough for longest trainer name
        val maxTrainerWidth = (trainerNames.maxOfOrNull { it.length } ?: 15) * 1.2
        worksheet.cells.setColumnWidthPixel(10, (maxTrainerWidth * 8).toInt().coerceAtLeast(120))
    }
    
    private fun enableFiltering(worksheet: Worksheet) {
        val lastRow = worksheet.cells.maxDataRow
        if (lastRow > 0) {
            worksheet.autoFilter.range = "A1:M${lastRow + 1}"
        }
    }
    
    private fun addBorders(style: Style) {
        style.setBorder(BorderType.BOTTOM_BORDER, CellBorderType.THIN, Color.getBlack())
        style.setBorder(BorderType.TOP_BORDER, CellBorderType.THIN, Color.getBlack())
        style.setBorder(BorderType.LEFT_BORDER, CellBorderType.THIN, Color.getBlack())
        style.setBorder(BorderType.RIGHT_BORDER, CellBorderType.THIN, Color.getBlack())
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
                putExtra(Intent.EXTRA_TEXT, "Complete Professional Excel Analysis with ALL formatting automatically applied:\n" +
                        "✅ All rows with filtering enabled\n" +
                        "✅ Track names auto-width + color coded (avoiding green/blue/purple)\n" +
                        "✅ Race numbers centered\n" +
                        "✅ Race names auto-width with text wrapping\n" +
                        "✅ Time, Distance, Horse# centered\n" +
                        "✅ Horse names auto-width\n" +
                        "✅ Scores centered\n" +
                        "✅ Bet types color coded (Green=Super, Blue=Best, Purple=Good)\n" +
                        "✅ Jockey/Trainer names auto-width\n" +
                        "✅ Barrier/Weight centered\n" +
                        "✅ True Excel .xlsx format with ALL formatting automatically applied")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Professional Excel Analysis"))
            
        } catch (e: Exception) {
            println("❌ Error sharing Excel file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private data class CellConfig(
        val columnIndex: Int,
        val value: String,
        val style: Style
    )
    
    private data class ExcelStyles(
        val header: Style,
        val trackStyles: Map<String, Style>,
        val superBet: Style,
        val bestBet: Style,
        val goodBet: Style,
        val centered: Style,
        val textWrap: Style,
        val regular: Style
    )
}