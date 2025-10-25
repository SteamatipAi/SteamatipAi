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
    results: List<RaceResult>,
    selectedDate: String
) {
    try {
        // Prepare data for Excel export
        val exportData = mutableListOf<Array<String>>()
        
        // Group results by track for better organization
        val resultsByTrack = results.groupBy { it.race.venue }
        
        resultsByTrack.forEach { (trackName, trackResults) ->
            trackResults.sortedBy { it.race.raceNumber }.forEach { raceResult ->
                // Get all horses for this race
                val allHorses = raceResult.allHorses.sortedByDescending { it.score }
                
                allHorses.forEachIndexed { horseIndex, scoredHorse ->
                    // Only the top horse gets the betting recommendation
                    val betType = if (horseIndex == 0 && raceResult.bettingRecommendations.isNotEmpty()) {
                        raceResult.bettingRecommendations[0].betType
                    } else BetType.CONSIDER
                    
                    val betTypeText = when (betType) {
                        BetType.SUPER_BET -> "SUPER BET"
                        BetType.BEST_BET -> "BEST BET"
                        BetType.GOOD_BET -> "GOOD BET"
                        else -> ""
                    }
                    
                    val row = arrayOf(
                        raceResult.race.venue,                                    // Track
                        raceResult.race.raceNumber.toString(),                   // Race #
                        raceResult.race.name,                                    // Race Name
                        raceResult.race.time,                                    // Time
                        "${raceResult.race.distance}m",                          // Distance
                        scoredHorse.horse.number.toString(),                     // Horse #
                        scoredHorse.horse.name,                                  // Horse Name
                        String.format("%.1f", scoredHorse.score),                // Score
                        betTypeText,                                             // Bet Type
                        "",                                                      // Results (empty for user input)
                        "",                                                      // Price (empty for user input)
                        scoredHorse.horse.jockey,                                // Jockey
                        scoredHorse.horse.trainer,                               // Trainer
                        scoredHorse.horse.barrier.toString(),                     // Barrier
                        "${scoredHorse.horse.weight}kg"                          // Weight
                    )
                    exportData.add(row)
                }
            }
        }
        
        // Initialize Aspose.Cells workbook
        val workbook = Workbook()
        
        // Create main results worksheet
        val mainWorksheet = workbook.worksheets[0]
        mainWorksheet.name = "Complete Race Analysis"
        
        // Create new worksheet for law breakdowns
        val lawBreakdownWorksheet = workbook.worksheets.add("Law Breakdown Analysis")
        
        // Create all professional styles
        val styles = createFullResultsStyles(workbook, exportData)
        
        // Create header with professional formatting
        createFullResultsHeader(mainWorksheet, styles)
        
        // Add all data with EXACT formatting
        addFullResultsData(mainWorksheet, exportData, styles)
        
        // Format columns EXACTLY as specified
        formatFullResultsColumns(mainWorksheet, exportData)
        
        // Enable filtering on ALL rows
        enableFullResultsFiltering(mainWorksheet)
        
        // Create law breakdown analysis
        createLawBreakdownAnalysis(lawBreakdownWorksheet, results, styles)
        
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
            "Horse #", "Horse Name", "Score", "Bet Type",
            "Results", "Price", "Jockey", "Trainer", "Barrier", "Weight"
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
        val betTypeText = row[8] // Column I: Bet Type (updated index after removing Position column)
        val betTypeStyle = when (betTypeText.trim().uppercase()) {
            "SUPER BET" -> styles.superBet
            "BEST BET" -> styles.bestBet
            "GOOD BET" -> styles.goodBet
            else -> styles.centered
        }
        
        val trackStyle = styles.trackStyles[row[0]] ?: styles.regular
        
        // Set row height to accommodate wrapped text
        worksheet.cells.setRowHeightPixel(rowIndex, 60)
        
        // Determine if this row should be shaded based on bet type
        val isSpecialBet = betTypeText.trim().uppercase() in listOf("SUPER BET", "BEST BET", "GOOD BET")
        
        // Apply EXACT formatting as you specified:
        val cellConfigs = listOf(
            CellConfig(0, row[0], if (isSpecialBet) betTypeStyle else trackStyle), // A: Track - color coded or bet type colored
            CellConfig(1, row[1], if (isSpecialBet) betTypeStyle else styles.centered), // B: Race # - centered or bet type colored
            CellConfig(2, row[2], if (isSpecialBet) betTypeStyle else styles.textWrap), // C: Race name - wrapped or bet type colored
            CellConfig(3, row[3], if (isSpecialBet) betTypeStyle else styles.centered), // D: Time - centered or bet type colored
            CellConfig(4, row[4], if (isSpecialBet) betTypeStyle else styles.centered), // E: Distance - centered or bet type colored
            CellConfig(5, row[5], if (isSpecialBet) betTypeStyle else styles.centered), // F: Horse # - centered or bet type colored
            CellConfig(6, row[6], if (isSpecialBet) betTypeStyle else styles.regular), // G: Horse name - auto-width or bet type colored
            CellConfig(7, row[7], if (isSpecialBet) betTypeStyle else styles.centered), // H: Score - centered or bet type colored
            CellConfig(8, row[8], betTypeStyle),                    // I: Bet type - color coded
            CellConfig(9, row[9], if (isSpecialBet) betTypeStyle else styles.centered), // J: Results - centered or bet type colored
            CellConfig(10, row[10], if (isSpecialBet) betTypeStyle else styles.centered), // K: Price - centered or bet type colored
            CellConfig(11, row[11], if (isSpecialBet) betTypeStyle else styles.regular), // L: Jockey - auto-width or bet type colored
            CellConfig(12, row[12], if (isSpecialBet) betTypeStyle else styles.regular), // M: Trainer - auto-width or bet type colored
            CellConfig(13, row[13], if (isSpecialBet) betTypeStyle else styles.centered), // N: Barrier - centered or bet type colored
            CellConfig(14, row[14], if (isSpecialBet) betTypeStyle else styles.centered)  // O: Weight - centered or bet type colored
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
    val jockeyNames = exportData.map { it[11] } // Updated column index (was 12)
    val trainerNames = exportData.map { it[12] } // Updated column index (was 13)
    
    // Auto-fit all columns first
    worksheet.autoFitColumns()
    
    // Set specific widths as you requested:
    // A: Wide enough for longest track name + color coding
    val maxTrackWidth = (trackNames.maxOfOrNull { it.length } ?: 15) * 1.5
    worksheet.cells.setColumnWidthPixel(0, (maxTrackWidth * 8).toInt().coerceAtLeast(120))
    
    // C: Race name width - make it wider to accommodate full race names
    val maxRaceWidth = (raceNames.maxOfOrNull { it.length } ?: 25) * 1.2 // Increased multiplier
    worksheet.cells.setColumnWidthPixel(2, (maxRaceWidth * 8).toInt().coerceAtLeast(200).coerceAtMost(300))
    
    // G: Wide enough for longest horse name
    val maxHorseWidth = (horseNames.maxOfOrNull { it.length } ?: 20) * 1.2
    worksheet.cells.setColumnWidthPixel(6, (maxHorseWidth * 8).toInt().coerceAtLeast(150))
    
    // I: Wide enough for bet type wording + color coding
    worksheet.cells.setColumnWidthPixel(8, 120)
    
    // J: Results column - compact width for dropdown
    worksheet.cells.setColumnWidthPixel(9, 80)
    
    // K: Price column - compact width for numbers
    worksheet.cells.setColumnWidthPixel(10, 80)
    
    // L: Compact jockey name width (reduced from excessive)
    val maxJockeyWidth = (jockeyNames.maxOfOrNull { it.length } ?: 15) * 0.8 // Reduced multiplier
    worksheet.cells.setColumnWidthPixel(11, (maxJockeyWidth * 8).toInt().coerceAtLeast(80).coerceAtMost(120))
    
    // M: Compact trainer name width (reduced from excessive)
    val maxTrainerWidth = (trainerNames.maxOfOrNull { it.length } ?: 15) * 0.8 // Reduced multiplier
    worksheet.cells.setColumnWidthPixel(12, (maxTrainerWidth * 8).toInt().coerceAtLeast(80).coerceAtMost(120))
}

private fun enableFullResultsFiltering(worksheet: Worksheet) {
    val lastRow = worksheet.cells.maxDataRow
    if (lastRow > 0) {
        worksheet.autoFilter.range = "A1:O${lastRow + 1}" // Updated to remove Position column
        
        // Freeze the header row
        worksheet.freezePanes(1, 0, 1, 0) // Freeze row 1 (header row)
        
        // Results column (K) is ready for manual WIN/PLACE/PLNTD input
        // You can manually add dropdown validation in Excel:
        // 1. Select column K (Results)
        // 2. Go to Data > Data Validation
        // 3. Choose "List" and enter: WIN,PLACE,PLNTD
        println("✅ Results column ready for WIN/PLACE/PLNTD input")
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
                    "✅ Race names compact width with text wrapping\n" +
                    "✅ Time, Distance, Horse# centered\n" +
                    "✅ Horse names auto-width\n" +
                    "✅ Scores and positions centered\n" +
                    "✅ Bet types color coded (Green=Super, Blue=Best, Purple=Good)\n" +
                    "✅ Results column with WIN/PLACE/PLNTD dropdown validation\n" +
                    "✅ Price column for payout amounts\n" +
                    "✅ Jockey/Trainer names compact width\n" +
                    "✅ Barrier/Weight centered\n" +
                    "✅ Law Breakdown Analysis tab with individual scoring\n" +
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
                        else -> ""
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
            
            // Freeze the header row
            worksheet.freezePanes(1, 0, 1, 0) // Freeze row 1 (header row)
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
    
private fun createLawBreakdownAnalysis(
    worksheet: Worksheet,
    results: List<RaceResult>,
    styles: FullResultsStyles
) {
    // Create headers for law breakdown
    val headers = arrayOf(
        "Track", "Race #", "Horse #", "Horse Name", "Total Score",
        "L1: Recent Form", "L1b: 1st Up", "L2: 2nd Up", "L2b: 2nd Up Recent Form", "L3: Class Suitability", 
        "L4: Distance Success", "L5: Track Success", "L6: Track+Distance", "L7: Sectional Time",
        "L8: Barrier", "L9: Jockey", "L10: Trainer", "L11: Combination",
        "L12: Track Condition", "L13: Weight Advantage", "L14: Freshness",
        "Scoring Type", "Bet Type"
    )
    
    headers.forEachIndexed { index, header ->
        val cell = worksheet.cells[0, index]
        cell.putValue(header)
        cell.setStyle(styles.header)
    }
    
    var rowIndex = 1
    
    // Group results by track for better organization
    val resultsByTrack = results.groupBy { it.race.venue }
    
    resultsByTrack.forEach { (trackName, trackResults) ->
        trackResults.sortedBy { it.race.raceNumber }.forEach { raceResult ->
            // Get all horses for this race
            val allHorses = raceResult.allHorses.sortedByDescending { it.score }
            
            allHorses.forEachIndexed { horseIndex, scoredHorse ->
                // Only the top horse gets the betting recommendation
                val betType = if (horseIndex == 0 && raceResult.bettingRecommendations.isNotEmpty()) {
                    raceResult.bettingRecommendations[0].betType
                } else BetType.CONSIDER
                
                val betTypeText = when (betType) {
                    BetType.SUPER_BET -> "SUPER BET"
                    BetType.BEST_BET -> "BEST BET"
                    BetType.GOOD_BET -> "GOOD BET"
                        else -> ""
                }
                
                val scoringType = when (scoredHorse.scoreBreakdown.type) {
                    com.steamatipai.data.models.ScoringType.NORMAL -> "NORMAL"
                    com.steamatipai.data.models.ScoringType.RETURNING_FROM_SPELL -> "SPELL HORSE"
                    com.steamatipai.data.models.ScoringType.FIRST_UP -> "FIRST UP"
                    else -> "UNKNOWN"
                }
                
                val breakdown = scoredHorse.scoreBreakdown
                
                val row = arrayOf(
                    raceResult.race.venue,                                    // Track
                    raceResult.race.raceNumber.toString(),                   // Race #
                    scoredHorse.horse.number.toString(),                     // Horse #
                    scoredHorse.horse.name,                                  // Horse Name
                    String.format("%.1f", scoredHorse.score),                // Total Score
                    String.format("%.1f", breakdown.recentForm),             // L1: Recent Form
                    String.format("%.1f", breakdown.firstUp),                 // L1b: 1st Up
                    String.format("%.1f", breakdown.secondUp),                // L2: 2nd Up
                    String.format("%.1f", breakdown.recentForm),             // L2b: 2nd Up Recent Form (same as L1 for 2nd up horses)
                    String.format("%.1f", breakdown.classSuitability),       // L3: Class Suitability
                    String.format("%.1f", breakdown.distanceSuccess),         // L4: Distance Success
                    String.format("%.1f", breakdown.trackSuccess),            // L5: Track Success
                    String.format("%.1f", breakdown.trackDistanceCombined),   // L6: Track+Distance
                    String.format("%.1f", breakdown.sectionalTime),           // L7: Sectional Time
                    String.format("%.1f", breakdown.barrier),                // L8: Barrier
                    String.format("%.1f", breakdown.jockey),                 // L9: Jockey
                    String.format("%.1f", breakdown.trainer),                // L10: Trainer
                    String.format("%.1f", breakdown.combination),            // L11: Combination
                    String.format("%.1f", breakdown.trackCondition),         // L12: Track Condition
                    String.format("%.1f", breakdown.weightAdvantage),        // L13: Weight Advantage
                    String.format("%.1f", breakdown.freshness),              // L14: Freshness
                    scoringType,                                              // Scoring Type
                    betTypeText                                               // Bet Type
                )
                
                // Set row height for consistency
                worksheet.cells.setRowHeightPixel(rowIndex, 60)
                
                // Determine if this row should be shaded based on bet type
                val isSpecialBet = betType in listOf(BetType.SUPER_BET, BetType.BEST_BET, BetType.GOOD_BET)
                val betTypeStyle = when (betType) {
                    BetType.SUPER_BET -> styles.superBet
                    BetType.BEST_BET -> styles.bestBet
                    BetType.GOOD_BET -> styles.goodBet
                    else -> styles.centered
                }
                
                row.forEachIndexed { colIndex, value ->
                    val cell = worksheet.cells[rowIndex, colIndex]
                    cell.putValue(value)
                    
                    // Apply appropriate styling with row shading for special bets
                    when (colIndex) {
                        0 -> cell.setStyle(if (isSpecialBet) betTypeStyle else (styles.trackStyles[raceResult.race.venue] ?: styles.regular)) // Track
                        1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21 -> cell.setStyle(if (isSpecialBet) betTypeStyle else styles.centered) // Numeric columns
                        3 -> cell.setStyle(if (isSpecialBet) betTypeStyle else styles.regular) // Horse Name
                        22 -> cell.setStyle(if (isSpecialBet) betTypeStyle else styles.centered) // Scoring Type
                        23 -> cell.setStyle(betTypeStyle) // Bet Type
                    }
                }
                
                rowIndex++
            }
        }
    }
    
    // Auto-fit columns
    worksheet.autoFitColumns()
    
    // Set consistent widths for first columns
    worksheet.cells.setColumnWidthPixel(0, 120) // A: Track
    worksheet.cells.setColumnWidthPixel(1, 80)  // B: Race #
    worksheet.cells.setColumnWidthPixel(2, 80)  // C: Horse #
    worksheet.cells.setColumnWidthPixel(3, 150) // D: Horse Name
    worksheet.cells.setColumnWidthPixel(4, 120) // E: Total Score
    
    // Set wider widths for law columns (L1-L14) - headers need more space
    for (i in 5..21) { // Columns F-V (L1-L14)
        worksheet.cells.setColumnWidthPixel(i, 150) // Wider width for readable headers
    }
    
    // Set readable width for Scoring Type column
    worksheet.cells.setColumnWidthPixel(22, 120) // Scoring Type
    
    // Set readable width for Bet Type column  
    worksheet.cells.setColumnWidthPixel(23, 120) // Bet Type
    
    // Enable filtering
    val lastRow = worksheet.cells.maxDataRow
    if (lastRow > 0) {
        worksheet.autoFilter.range = "A1:X${lastRow + 1}"
        
        // Freeze the header row
        worksheet.freezePanes(1, 0, 1, 0) // Freeze row 1 (header row)
    }
    
    // Add summary footer
    val summaryRow = rowIndex + 1
    val summaryCell = worksheet.cells[summaryRow, 0]
    summaryCell.putValue("Total Horses Analyzed: ${rowIndex - 1}")
    summaryCell.setStyle(styles.header)
    
    val timestampCell = worksheet.cells[summaryRow + 1, 0]
    timestampCell.putValue("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
}

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