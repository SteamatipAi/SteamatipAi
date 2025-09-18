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
            val sheet = workbook.createSheet("Best Bets - $selectedDate")
            
            // Create styles
            val styles = createStyles(workbook)
            
            // Create and format header row
            createHeaderRow(sheet, styles.header)
            
            // Add data rows with formatting
            addDataRows(sheet, bestBets, styles)
            
            // Apply column formatting
            formatColumns(sheet, bestBets)
            
            // Enable filtering
            val lastRow = sheet.lastRowNum
            if (lastRow > 0) {
                sheet.setAutoFilter(CellRangeAddress(0, lastRow, 0, 12))
            }
            
            // Save and share file
            val fileName = "SteamaTip_BestBets_${selectedDate.replace("-", "")}.xlsx"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            
            workbook.close()
            shareExcelFile(context, file)
            
        } catch (e: Exception) {
            println("❌ Error creating Excel file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun createStyles(workbook: Workbook): ExcelStyles {
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
        
        // Track color styles (avoiding Green/Blue/Purple)
        val trackColors = mapOf(
            0 to IndexedColors.ORANGE.index,
            1 to IndexedColors.YELLOW.index,
            2 to IndexedColors.LIGHT_TURQUOISE.index,
            3 to IndexedColors.PINK.index,
            4 to IndexedColors.LIME.index,
            5 to IndexedColors.CORAL.index
        )
        
        val trackStyles = trackColors.mapValues { (_, colorIndex) ->
            val style = workbook.createCellStyle()
            style.fillForegroundColor = colorIndex
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
            addBorders(style)
            style
        }
        
        // Bet type styles
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
        
        // Centered style for data
        val centeredStyle = workbook.createCellStyle()
        centeredStyle.alignment = HorizontalAlignment.CENTER
        centeredStyle.verticalAlignment = VerticalAlignment.CENTER
        addBorders(centeredStyle)
        
        // Regular style for text data
        val regularStyle = workbook.createCellStyle()
        addBorders(regularStyle)
        
        return ExcelStyles(
            header = headerStyle,
            trackStyles = trackStyles,
            superBet = superBetStyle,
            bestBet = bestBetStyle,
            goodBet = goodBetStyle,
            centered = centeredStyle,
            regular = regularStyle
        )
    }
    
    private fun addBorders(style: CellStyle) {
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
    }
    
    private fun createHeaderRow(sheet: Sheet, headerStyle: CellStyle) {
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
    
    private fun addDataRows(
        sheet: Sheet, 
        bestBets: List<Pair<RaceResult, ScoredHorse>>, 
        styles: ExcelStyles
    ) {
        val trackNames = bestBets.map { it.first.race.venue }.distinct()
        var rowIndex = 1
        
        bestBets.forEach { (raceResult, horse) ->
            val row = sheet.createRow(rowIndex++)
            
            // Determine bet type and colors
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
            
            val trackIndex = trackNames.indexOf(raceResult.race.venue)
            val trackStyle = styles.trackStyles[trackIndex % styles.trackStyles.size] ?: styles.regular
            
            // Fill data with appropriate styles
            val data = arrayOf(
                Triple(raceResult.race.venue, trackStyle, false), // A - Track (colored)
                Triple(raceResult.race.raceNumber.toString(), styles.centered, false), // B - Race # (centered)
                Triple(raceResult.race.name, styles.regular, true), // C - Race Name (wrap text)
                Triple(raceResult.race.time, styles.centered, false), // D - Time (centered)
                Triple("${raceResult.race.distance}m", styles.centered, false), // E - Distance (centered)
                Triple(horse.horse.number.toString(), styles.centered, false), // F - Horse # (centered)
                Triple(horse.horse.name, styles.regular, false), // G - Horse Name (auto-width)
                Triple(String.format("%.1f", horse.score), styles.centered, false), // H - Score (centered)
                Triple(betTypeText, betTypeStyle, false), // I - Bet Type (colored)
                Triple(horse.horse.jockey, styles.regular, false), // J - Jockey (auto-width)
                Triple(horse.horse.trainer, styles.regular, false), // K - Trainer (auto-width)
                Triple(horse.horse.barrier.toString(), styles.centered, false), // L - Barrier (centered)
                Triple("${horse.horse.weight}kg", styles.centered, false) // M - Weight (centered)
            )
            
            data.forEachIndexed { cellIndex, (value, cellStyle, wrapText) ->
                val cell = row.createCell(cellIndex)
                cell.setCellValue(value)
                cell.cellStyle = cellStyle
                if (wrapText) {
                    val wrappedStyle = sheet.workbook.createCellStyle()
                    wrappedStyle.cloneStyleFrom(cellStyle)
                    wrappedStyle.wrapText = true
                    cell.cellStyle = wrappedStyle
                }
            }
        }
    }
    
    private fun formatColumns(sheet: Sheet, bestBets: List<Pair<RaceResult, ScoredHorse>>) {
        // Auto-size columns based on content
        for (i in 0..12) {
            sheet.autoSizeColumn(i)
        }
        
        // Set minimum widths for specific columns
        val minWidths = mapOf(
            0 to 4000, // Track - wide enough for longest track name
            2 to 6000, // Race Name - wide enough for longest race name
            6 to 4000, // Horse Name - wide enough for longest horse name
            8 to 3000, // Bet Type - wide enough for "SUPER BET"
            9 to 4000, // Jockey - wide enough for longest jockey name
            10 to 4000 // Trainer - wide enough for longest trainer name
        )
        
        minWidths.forEach { (columnIndex, minWidth) ->
            val currentWidth = sheet.getColumnWidth(columnIndex)
            if (currentWidth < minWidth) {
                sheet.setColumnWidth(columnIndex, minWidth)
            }
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
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "SteamaTip AI - Best Bets Analysis")
                putExtra(Intent.EXTRA_TEXT, "Professional Best Bets analysis with color-coded formatting and filtering enabled.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Professional Excel Report"))
            
        } catch (e: Exception) {
            println("❌ Error sharing Excel file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private data class ExcelStyles(
        val header: CellStyle,
        val trackStyles: Map<Int, CellStyle>,
        val superBet: CellStyle,
        val bestBet: CellStyle,
        val goodBet: CellStyle,
        val centered: CellStyle,
        val regular: CellStyle
    )
}