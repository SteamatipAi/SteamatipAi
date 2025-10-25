package com.steamatipai.network

import com.steamatipai.data.models.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.io.File
import java.io.FileWriter
import java.util.Calendar

class ScrapingService {
    private val httpClient: OkHttpClient = NetworkConfig.createHttpClient()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val racingDateFormat = SimpleDateFormat("yyyyMMMdd", Locale.ENGLISH) // Use ENGLISH locale to get "Sep" instead of "Sept"
    
    /**
     * Scrape available tracks for a given date
     */
    suspend fun scrapeAvailableTracks(date: Date): List<Track> {
        return try {
            println("🔍 Scraping available tracks for ${dateFormat.format(date)}")
            
            // Convert date to Racing Australia format (e.g., 2025Aug30)
            val dateStr = racingDateFormat.format(date)
            println("🔍 Date format for Racing Australia: $dateStr")
            
            val calendarUrl = NetworkConfig.buildCalendarUrl(dateStr)
            println("🌐 Calendar URL: $calendarUrl")
            
            val request = NetworkConfig.createRequestBuilder(calendarUrl).build()
            val response = httpClient.newCall(request).execute()
            
            println("🔍 HTTP Response: ${response.code} ${response.message}")
            
            if (!response.isSuccessful) {
                println("❌ Failed to fetch calendar: ${response.code}")
                return emptyList()
            }
            
            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                println("❌ Empty response from calendar")
                return emptyList()
            }
            
            println("🔍 HTML response length: ${html.length} characters")
            
            val doc = Jsoup.parse(html)
            val tracks = mutableListOf<Track>()
            
            // CRITICAL: Find tracks from BOTH Form.aspx and Results.aspx links
            // (Racing Australia shows Results.aspx for tracks where racing has started)
            // BUT we'll always construct Form.aspx URLs to use the pre-race form data
            val trackElements = doc.select("a[href*='Form.aspx'], a[href*='Results.aspx']")
            println("🔍 Found ${trackElements.size} track link elements (will convert all to Form.aspx)")
            
            // If no tracks found, try alternative selectors
            if (trackElements.isEmpty()) {
                println("⚠️ No tracks found with standard selectors, trying alternatives...")
                val altSelectors = listOf(
                    "a[href*='form']",
                    "a[href*='Form']",
                    "a[href*='Results']",
                    "a[href*='results']",
                    "a[href*='FreeFields']",
                    "a[href*='race']",
                    "a[href*='Race']",
                    "a[href*='track']",
                    "a[href*='Track']"
                )
                
                altSelectors.forEach { selector ->
                    val elements = doc.select(selector)
                    println("🔍 Selector '$selector': ${elements.size} elements")
                    if (elements.isNotEmpty()) {
                        println("   First element: ${elements.first().text()} -> ${elements.first().attr("href")}")
                    }
                }
            }
            
            trackElements.forEach { element ->
                val href = element.attr("href")
                val trackName = element.text().trim()
                
                println("🔍 Processing track element: '$trackName' -> $href")
                
                if (trackName.isNotEmpty() && href.contains("Key=")) {
                    // Extract state and track from the URL
                    val keyMatch = Regex("Key=([^&]+)").find(href)
                    if (keyMatch != null) {
                        val encodedKey = keyMatch.groupValues[1]
                        // Decode URL encoding: %2C becomes comma, %20 becomes space
                        val key = encodedKey.replace("%2C", ",").replace("%20", " ")
                        val parts = key.split(",")
                        
                        println("🔍 Encoded key: $encodedKey")
                        println("🔍 Decoded key: $key")
                        println("🔍 Key parts: ${parts.joinToString(" | ")}")
                        
                        if (parts.size >= 3) {
                            val trackDate = parts[0]
                            val state = parts[1]
                            val fullTrackName = parts[2]
                            
                            // Normalize dates for comparison - handle both "Sept" and "Sep" formats
                            val normalizedTrackDate = normalizeDate(trackDate)
                            val normalizedDateStr = normalizeDate(dateStr)
                            
                            println("🔍 Normalized dates - Track: $normalizedTrackDate, Selected: $normalizedDateStr")
                            
                            // Date match with normalized comparison
                            if (normalizedTrackDate == normalizedDateStr) {
                                println("✅ Date match found: $trackDate matches selected date $dateStr")
                                // Count races for this track
                                val raceCount = countRacesForTrack(doc, fullTrackName)
                                
                                // CRITICAL: Always construct Form.aspx URL, even if we found this track via Results.aspx
                                // This ensures we use pre-race form data, never results with today's races
                                val encodedKey = key.replace(",", "%2C").replace(" ", "%20")
                                val formUrl = "${NetworkConfig.BASE_URL}/FreeFields/Form.aspx?Key=$encodedKey&recentForm=Y"
                                
                                val track = Track(
                                    key = key, // This is the decoded key: "2025Aug30,NSW,Grafton"
                                    name = fullTrackName,
                                    state = state,
                                    raceCount = raceCount,
                                    url = formUrl  // Always use Form.aspx, never Results.aspx
                                )
                                
                                tracks.add(track)
                                println("🏁 Found track: ${track.name} (${track.state}) - ${track.raceCount} races")
                                println("   Key: $key")
                                println("   URL: $formUrl (Form.aspx - pre-race form)")
                            } else {
                                println("⏰ Skipping track $fullTrackName - date $trackDate doesn't match selected date $dateStr")
                            }
                        } else {
                            println("⚠️ Invalid key format: $key (${parts.size} parts)")
                        }
                    } else {
                        println("⚠️ No key found in href: $href")
                    }
                } else {
                    println("⚠️ Skipping element: name='$trackName', href='$href'")
                }
            }
            
            if (tracks.isEmpty()) {
                println("❌ No tracks found for date $dateStr")
                println("🔍 Document HTML preview:")
                println(doc.html().take(2000))
            }
            
            println("✅ Found ${tracks.size} tracks for $dateStr")
            tracks
            
        } catch (e: Exception) {
            println("❌ Error scraping tracks: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Normalize date strings to handle both "Sept" and "Sep" formats
     * This makes the app more robust against format changes
     */
    private fun normalizeDate(dateStr: String): String {
        return dateStr
            .replace("Sept", "Sep") // Convert "Sept" to "Sep" for consistent comparison
            .replace("Sept", "Sep") // Handle any remaining "Sept" variations
    }
    
    /**
     * Count races for a specific track from the calendar
     */
    private fun countRacesForTrack(doc: Document, trackName: String): Int {
        var raceCount = 0
        
        // Look for race information near the track name
        val trackSection = doc.select("td, div").find { element ->
            element?.text()?.contains(trackName, ignoreCase = true) == true
        }
        
        if (trackSection != null) {
            // Count race numbers in the same section
            val raceNumbers = trackSection.parent()?.select("td")?.filter { element ->
                element?.text()?.matches(Regex("\\d+")) == true
            } ?: emptyList()
            
            raceCount = raceNumbers.size
        }
        
        return raceCount
    }
    
    /**
     * Debug function to dump HTML structure for analysis
     */
    private fun debugHtmlStructure(doc: Document, track: Track) {
        println("🔍 DEBUG: Analysing HTML structure for ${track.name}")
        println("🔍 Document title: ${doc.title()}")
        
        // Look for all tables
        val allTables = doc.select("table")
        println("🔍 Found ${allTables.size} tables")
        
        allTables.forEachIndexed { index, table ->
            println("🔍 Table $index:")
            println("   Class: ${table.className()}")
            println("   ID: ${table.id()}")
            println("   Rows: ${table.select("tr").size}")
            println("   Text preview: ${table.text().take(100)}...")
        }
        
        // Look for all divs with class names
        val allDivs = doc.select("div[class]")
        println("🔍 Found ${allDivs.size} divs with classes")
        
        allDivs.take(10).forEachIndexed { index, div ->
            println("🔍 Div $index:")
            println("   Class: ${div.className()}")
            println("   ID: ${div.id()}")
            println("   Text preview: ${div.text().take(50)}...")
        }
        
        // Look for race-related elements
        val raceElements = doc.select("[class*='race'], [class*='field'], [id*='race'], [id*='field']")
        println("🔍 Found ${raceElements.size} race/field related elements")
        
        raceElements.take(5).forEachIndexed { index, element ->
            println("🔍 Race element $index:")
            println("   Tag: ${element.tagName()}")
            println("   Class: ${element.className()}")
            println("   ID: ${element.id()}")
            println("   Text preview: ${element.text().take(100)}...")
        }
    }

    /**
     * Scrape races from a specific track
     */
    suspend fun scrapeTrackRaces(track: Track, date: Date): List<Race> {
        return try {
            val url = buildTrackUrl(track, date)
            println("🔗 Scraping track races from: $url")
            
            val html = makeHttpRequest(url)
            
            if (html.isEmpty()) {
                println("❌ Empty response from track")
                return emptyList()
            }
            
            val doc = Jsoup.parse(html)
            val races = mutableListOf<Race>()
            
            // Debug: Print the first 2000 characters of HTML to see structure
            println("🔍 HTML Preview (first 2000 chars): ${doc.html().take(2000)}...")
            
            // Extract track condition from the <b> element with "Track Condition:" text
            val trackConditionElement = doc.select("b").find { element ->
                element.text().contains("Track Condition:", ignoreCase = true)
            }
            val overallTrackCondition = if (trackConditionElement != null) {
                val trackConditionText = trackConditionElement.parent()?.text() ?: trackConditionElement.text()
                val trackConditionMatch = Regex("Track Condition:\\s*([^\\s]+(?:\\s+\\d+)?)").find(trackConditionText)
                trackConditionMatch?.groupValues?.get(1)?.trim() ?: "Good"
            } else {
                "Good"
            }
            println("🔍 Overall track condition: $overallTrackCondition")
            
            // Call debug function to analyze HTML structure
            debugHtmlStructure(doc, track)
            
            // Racing Australia specific selectors - look for actual race containers
            var raceElements: List<Element> = emptyList()
            
            // First, try to find the actual race containers based on logcat analysis
            // Look for table.race-title elements first, then their corresponding table.race-strip-fields
            val raceTitleTables = doc.select("table.race-title")
            println("🔍 Found ${raceTitleTables.size} race title tables")
            
            if (raceTitleTables.isNotEmpty()) {
                // For each race title table, find its corresponding race-strip-fields table
                raceTitleTables.forEachIndexed { index, titleTable ->
                    // Look for the next table.race-strip-fields after this race title
                    val nextElements = titleTable.nextElementSiblings()
                    val raceStripTable = nextElements.find { it.hasClass("race-strip-fields") }
                    
                    if (raceStripTable != null) {
                        // Create a combined element that contains both title and field data
                        val combinedElement = titleTable.clone()
                        combinedElement.appendChild(raceStripTable.clone())
                        raceElements = raceElements + combinedElement
                        println("✅ Found race ${index + 1}: title + field data")
                    }
                }
            }
            
            // If no race title tables found, try the original selectors as fallback
            if (raceElements.isEmpty()) {
                // Look for race containers that contain horse data - Racing Australia specific
                // Racing Australia typically uses tables with class names containing 'field' or 'race'
                raceElements = doc.select("table.race-strip-fields, table[class*='field'], table[class*='race'], div[class*='field'], div[class*='race']")
                println("🔍 Using original selectors, found ${raceElements.size} potential race elements")
            }
            
            if (raceElements.isEmpty()) {
                // Try more specific Racing Australia selectors based on logcat analysis
                raceElements = doc.select("table.SNSTable, table.race-strip-fields, table.horse-form-table, table[class*='SNSTable'], table[class*='race-strip'], table[class*='horse-form']")
                println("🔍 Using Racing Australia specific selectors, found ${raceElements.size} potential race tables")
            }
            
            if (raceElements.isEmpty()) {
                // Try looking for tables that contain race information
                val allTables = doc.select("table")
                raceElements = allTables.filter { table ->
                    val tableText = table.text()
                    // Look for tables that contain race-related text
                    tableText.contains("Race", ignoreCase = true) ||
                    tableText.contains("Field", ignoreCase = true) ||
                    tableText.contains("Horse", ignoreCase = true) ||
                    tableText.contains("Runner", ignoreCase = true)
                }
                println("🔍 Using table filtering, found ${raceElements.size} potential race tables")
            }
            
            if (raceElements.isEmpty()) {
                // Look for divs that might contain race fields
                val allDivs = doc.select("div")
                raceElements = allDivs.filter { div ->
                    val divText = div.text()
                    // Look for divs that contain multiple horse-like entries
                    val lines = divText.split("\n").filter { it.trim().isNotEmpty() }
                    lines.size >= 3 && lines.any { line ->
                        line.contains(Regex("\\d+")) && // Has numbers
                        line.contains(Regex("[A-Za-z]{3,}")) // Has text
                    }
                }
                println("🔍 Using div filtering, found ${raceElements.size} potential race divs")
            }
            
            // Limit to maximum 10 races (reasonable limit for a race day)
            if (raceElements.size > 10) {
                println("⚠️ Found ${raceElements.size} potential race elements, limiting to first 10")
                raceElements = raceElements.take(10)
            }
            
            println("🔍 Found ${raceElements.size} race elements")
            
            // Debug: Print details of first few elements
            raceElements.take(3).forEachIndexed { index, element ->
                println("🔍 Element $index: tag=${element.tagName()}, class=${element.className()}, id=${element.id()}")
                println("   Text preview: ${element.text().take(100)}...")
            }
            
            raceElements.forEachIndexed { index, raceElement ->
                try {
                    val race = parseRaceFromElement(raceElement, index + 1, track, date, overallTrackCondition)
                    if (race != null) {
                        races.add(race)
                        println("🏁 Parsed Race ${race.raceNumber}: ${race.name} (${race.horses.size} horses)")
                    } else {
                        println("⚠️ Failed to parse race element ${index + 1}")
                    }
                } catch (e: Exception) {
                    println("❌ Error parsing race ${index + 1}: ${e.message}")
                }
            }
            
            if (races.isEmpty()) {
                println("⚠️ No races found, trying alternative parsing...")
                races.addAll(parseRacesAlternative(doc, track, date))
            }
            
            println("✅ Found ${races.size} races for ${track.name}")
            races
            
        } catch (e: Exception) {
            println("❌ Error scraping track races: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Alternative parsing method for races
     */
    private fun parseRacesAlternative(doc: Document, track: Track, date: Date): List<Race> {
        val races = mutableListOf<Race>()
        
        println("🔍 Attempting alternative race parsing...")
        
        // Look for tables that clearly contain race fields
        val potentialRaceContainers = doc.select("table[class*='field'], div[class*='field'], div[id*='field'], .field-container")
        
        if (potentialRaceContainers.isEmpty()) {
            // Try looking for any table that might contain race data
            val allTables = doc.select("table")
            val raceTables = allTables.filter { table ->
                val tableText = table.text()
                tableText.contains("Race", ignoreCase = true) ||
                tableText.contains("Field", ignoreCase = true) ||
                tableText.contains("Horse", ignoreCase = true) ||
                tableText.contains("Runner", ignoreCase = true)
            }
            println("🔍 Alternative: Found ${raceTables.size} potential race tables")
            
            // Limit to maximum 10 races for alternative parsing
            val limitedTables = raceTables.take(10)
            
            limitedTables.forEachIndexed { index, table ->
                try {
                    // Look for horse information with more specific criteria
                    val horseElements = table.select("tr")
                    
                    // Only create race if we have at least 3 horses (reasonable for a race)
                    if (horseElements.size >= 3) {
                        // Parse horses first to ensure we have real data
                        val horses = parseHorsesFromContainer(table)
                        
                        // Only create race if we have real horses with real names and numbers
                        if (horses.size >= 3 && horses.all { it.name.length < 50 && it.number > 0 }) {
                            // Try to extract real race information instead of using defaults
                            val raceInfo = extractRaceInfoFromContainer(table)
                            
                            if (raceInfo.name.isNotEmpty() && raceInfo.distance > 0) {
                                val race = Race(
                                    id = "race_${index + 1}",
                                    raceNumber = index + 1,
                                    name = raceInfo.name,
                                    distance = raceInfo.distance,
                                    surface = raceInfo.surface,
                                    trackCondition = raceInfo.trackCondition,
                                    raceClass = raceInfo.raceClass,
                                    time = raceInfo.time,
                                    venue = track.name,
                                    horses = horses,
                                    date = date,
                                    raceEntryCode = generateRaceEntryCode(track, index + 1)
                                )
                                
                                races.add(race)
                                println("🏁 Alternative parsing: Race ${race.raceNumber} with ${race.horses.size} horses - ${race.name} ${race.distance}m")
                            } else {
                                println("⚠️ Skipping race ${index + 1} - no real race information found")
                            }
                        } else {
                            println("⚠️ Skipping race ${index + 1} - insufficient real horse data (${horses.size} horses)")
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error in alternative race parsing: ${e.message}")
                }
            }
        } else {
            // Use the field-specific containers
            val limitedContainers = potentialRaceContainers.take(10)
            
            limitedContainers.forEachIndexed { index, container ->
                try {
                    // Look for horse information with more specific criteria
                    val horseElements = container.select("tr")
                    
                    // Only create race if we have at least 3 horses (reasonable for a race)
                    if (horseElements.size >= 3) {
                        // Parse horses first to ensure we have real data
                        val horses = parseHorsesFromContainer(container)
                        
                        // Only create race if we have real horses with real names and numbers
                        if (horses.size >= 3 && horses.all { it.name.length < 50 && it.number > 0 }) {
                            // Try to extract real race information instead of using defaults
                            val raceInfo = extractRaceInfoFromContainer(container)
                            
                            if (raceInfo.name.isNotEmpty() && raceInfo.distance > 0) {
                                val race = Race(
                                    id = "race_${index + 1}",
                                    raceNumber = index + 1,
                                    name = raceInfo.name,
                                    distance = raceInfo.distance,
                                    surface = raceInfo.surface,
                                    trackCondition = raceInfo.trackCondition,
                                    raceClass = raceInfo.raceClass,
                                    time = raceInfo.time,
                                    venue = track.name,
                                    horses = horses,
                                    date = date,
                                    raceEntryCode = generateRaceEntryCode(track, index + 1)
                                )
                                
                                races.add(race)
                                println("🏁 Alternative parsing: Race ${race.raceNumber} with ${race.horses.size} horses - ${race.name} ${race.distance}m")
                            } else {
                                println("⚠️ Skipping race ${index + 1} - no real race information found")
                            }
                        } else {
                            println("⚠️ Skipping race ${index + 1} - insufficient real horse data (${horses.size} horses)")
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error in alternative race parsing: ${e.message}")
                }
            }
        }
        
        return races
    }
    
    /**
     * Extract race information from a container element
     */
    private fun extractRaceInfoFromContainer(container: Element): RaceInfo {
        try {
            // Try to find race name
            var raceName = container.select("h1, h2, h3, h4, h5, h6, .race-header, [class*='race'], [class*='field']").firstOrNull()?.text()?.trim()
            if (raceName.isNullOrEmpty()) {
                // Look for any text that might be a race name
                val allText = container.text().trim()
                val lines = allText.split("\n").filter { it.trim().isNotEmpty() }
                val potentialName = lines.firstOrNull { line ->
                    line.contains("Race", ignoreCase = true) || 
                    line.contains("Handicap", ignoreCase = true) ||
                    line.contains("Stakes", ignoreCase = true) ||
                    line.contains("Cup", ignoreCase = true) ||
                    line.contains("Plate", ignoreCase = true) ||
                    line.contains("Maiden", ignoreCase = true) ||
                    line.contains("Class", ignoreCase = true) ||
                    line.contains("Benchmark", ignoreCase = true) ||
                    line.contains("BM", ignoreCase = true)
                }
                raceName = potentialName?.trim() ?: ""
            }
            
            // Try to find distance
            var distance = 0
            val distanceText = container.select(".distance, .race-distance, [class*='distance'], [class*='metres'], [class*='m']").firstOrNull()?.text()?.trim()
            distance = parseDistance(distanceText) ?: 0
            if (distance == 0) {
                // Try to find distance in the text
                val allText = container.text()
                val distanceMatch = Regex("(\\d{3,4})\\s*m").find(allText)
                distance = distanceMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }
            
            // Try to find surface
            var surface = container.select(".surface, .track-type, [class*='surface'], [class*='track']").firstOrNull()?.text()?.trim() ?: "Turf"
            
            // Try to find track condition
            var trackCondition = container.select(".condition, .track-condition, [class*='condition']").firstOrNull()?.text()?.trim() ?: "Good"
            
            // Try to find race class
            var raceClass = container.select(".class, .race-class, [class*='class']").firstOrNull()?.text()?.trim()
            if (raceClass.isNullOrEmpty()) {
                raceClass = when {
                    raceName.contains("Maiden", ignoreCase = true) -> "Maiden"
                    raceName.contains("Class", ignoreCase = true) -> "Class"
                    raceName.contains("Open", ignoreCase = true) -> "Open"
                    raceName.contains("Benchmark", ignoreCase = true) || raceName.contains("BM", ignoreCase = true) -> "Benchmark"
                    else -> "Open"
                }
            }
            
            // Try to find time
            var time = container.select(".time, .race-time, [class*='time']").firstOrNull()?.text()?.trim() ?: "TBA"
            
            return RaceInfo(
                name = raceName,
                distance = distance,
                surface = surface,
                trackCondition = trackCondition,
                raceClass = raceClass,
                time = time
            )
            
        } catch (e: Exception) {
            println("❌ Error extracting race info from container: ${e.message}")
            return RaceInfo("", 0, "Turf", "Good", "Open", "TBA")
        }
    }
    
    /**
     * Data class for race information
     */
    private data class RaceInfo(
        val name: String,
        val distance: Int,
        val surface: String,
        val trackCondition: String,
        val raceClass: String,
        val time: String
    )
    
    /**
     * Parse a race from a DOM element
     */
    private fun parseRaceFromElement(raceElement: Element, raceNumber: Int, track: Track, date: Date, overallTrackCondition: String): Race? {
        try {
            // Debug: Print what we're trying to parse
            println("🔍 Parsing race element $raceNumber:")
            println("   Tag: ${raceElement.tagName()}")
            println("   Class: ${raceElement.className()}")
            println("   ID: ${raceElement.id()}")
            println("   Text preview: ${raceElement.text().take(200)}...")
            
            // Look for the race header element with the specific format
            // <th><span class='raceNum'>Race 1</span> - 1:25PM RACE NAME (1200 METRES)
            val raceHeaderElement = raceElement.select("th").firstOrNull()
            
            if (raceHeaderElement == null) {
                println("❌ No race header (th) element found for Race $raceNumber - skipping")
                return null
            }
            
            val headerText = raceHeaderElement.text()
            println("🔍 Race header text: $headerText")
            
            // Extract race number, time, name, and distance using regex
            // Pattern: "Race 1 - 1:25PM RACE NAME (1200 METRES)"
            val racePattern = Regex("Race\\s+(\\d+)\\s+-\\s+(\\d{1,2}:\\d{2}[AP]M)\\s+(.*?)\\s+\\((\\d+)\\s+METRES\\)")
            val raceMatch = racePattern.find(headerText)
            
            if (raceMatch == null) {
                println("❌ Could not parse race pattern for Race $raceNumber - skipping")
                return null
            }
            
            val extractedRaceNumber = raceMatch.groupValues[1].toIntOrNull()
            val time = raceMatch.groupValues[2].trim()
            val raceName = raceMatch.groupValues[3].trim()
            val distance = raceMatch.groupValues[4].toIntOrNull() ?: 0
            
            // Verify we got the expected race number
            if (extractedRaceNumber != raceNumber) {
                println("⚠️ Race number mismatch: expected $raceNumber, found $extractedRaceNumber")
            }
            
            println("✅ Found race time: $time")
            println("✅ Found race name: $raceName")
            println("✅ Found distance: ${distance}m")
            
            // 4. Use the overall track condition passed from scrapeTrackRaces
            println("✅ Using overall track condition: $overallTrackCondition")
            
            // Parse horses - use the race-strip-fields table if available, otherwise use the whole element
            val hasRaceStripFields = raceElement.select("table.race-strip-fields").isNotEmpty()
            val horses = if (hasRaceStripFields) {
                val raceStripTable = raceElement.select("table.race-strip-fields").first()
                if (raceStripTable != null) {
                    parseHorsesFromContainer(raceStripTable)
                } else {
                    parseHorsesFromContainer(raceElement)
                }
            } else {
                parseHorsesFromContainer(raceElement)
            }
            
            if (horses.isEmpty()) {
                println("⚠️ No horses found for Race $raceNumber")
                return null
            }
            
            val race = Race(
                id = "race_$raceNumber",
                raceNumber = raceNumber,
                name = raceName,
                distance = distance,
                surface = "Turf", // Default surface
                trackCondition = overallTrackCondition,
                raceClass = "Open", // Default race class
                time = time,
                venue = track.name,
                horses = horses,
                date = date,
                raceEntryCode = generateRaceEntryCode(track, raceNumber)
            )
            
            return race
            
        } catch (e: Exception) {
            println("❌ Error parsing race element: ${e.message}")
            return null
        }
    }
    
    /**
     * Parse horses from a container element
     */
    private fun parseHorsesFromContainer(container: Element): List<Horse> {
        val horses = mutableListOf<Horse>()
        
        // Debug: Print container info
        println("🔍 Parsing horses from container:")
        println("   Tag: ${container.tagName()}")
        println("   Class: ${container.className()}")
        println("   ID: ${container.id()}")
        
        // Racing Australia specific selectors - look for actual horse rows
        var horseElements: List<Element> = emptyList()
        
        // First, try Racing Australia specific selectors
        horseElements = container.select("tr[class*='horse'], tr[class*='runner'], tr[class*='entry'], tr[class*='field']").toList()
        
        if (horseElements.isEmpty()) {
            // Try looking for table rows that contain horse data
            horseElements = container.select("tr").filter { row ->
                val rowText = row.text()
                // Look for rows that contain horse-like data
                rowText.contains(Regex("\\d+")) && // Has numbers (horse numbers)
                rowText.contains(Regex("[A-Za-z]{3,}")) && // Has text (horse names)
                rowText.length > 20 // Reasonable length for horse data
            }
            println("🔍 Using filtered table rows, found ${horseElements.size} potential horse rows")
        }
        
        // If still no horses found, try more generic selectors
        if (horseElements.isEmpty()) {
            println("⚠️ No horses found with standard selectors, trying generic elements...")
            horseElements = container.select("div, span, p").filter { element ->
                val elementText = element.text()
                elementText.contains(Regex("\\d+")) && // Has numbers
                elementText.contains(Regex("[A-Za-z]{3,}")) && // Has text
                elementText.length > 15 // Reasonable length
            }
        }
        
        // Filter out header rows and empty rows
        horseElements = horseElements.filter { element ->
            val text = element.text().trim()
            // Skip header rows that contain column names - use AND logic to identify headers
            !(text.contains("No", ignoreCase = true) &&
              text.contains("Horse", ignoreCase = true) &&
              text.contains("Trainer", ignoreCase = true) &&
              text.contains("Jockey", ignoreCase = true) &&
              text.contains("Barrier", ignoreCase = true) &&
              text.contains("Weight", ignoreCase = true))
        }
        
        println("🔍 Found ${horseElements.size} potential horse elements after filtering")
        
        // Debug: Print details of first few elements
        horseElements.take(3).forEachIndexed { index, element ->
            println("🔍 Horse element $index: tag=${element.tagName()}, class=${element.className()}")
            println("   Text preview: ${element.text().take(100)}...")
        }
        
        horseElements.forEachIndexed { index, horseElement ->
            try {
                val horse = parseHorseFromElement(horseElement, index + 1)
                if (horse != null) {
                    horses.add(horse)
                    println("✅ Parsed horse: ${horse.name} (Saddle #${horse.number})")
                } else {
                    println("⚠️ Failed to parse horse element ${index + 1}")
                }
            } catch (e: Exception) {
                println("❌ Error parsing horse ${index + 1}: ${e.message}")
            }
        }
        
        if (horses.isEmpty()) {
            println("❌ No horses could be parsed from container")
            println("🔍 Container HTML preview: ${container.html().take(500)}...")
        }
        
        return horses
    }
    
    /**
     * Parse a horse from an HTML element - NO FALLBACK VALUES
     */
    private fun parseHorseFromElement(horseElement: Element, horseNumber: Int): Horse? {
        try {
            // Debug: Print what we're trying to parse
            println("🔍 Parsing horse element $horseNumber:")
            println("   Tag: ${horseElement.tagName()}")
            println("   Class: ${horseElement.className()}")
            println("   Text preview: ${horseElement.text().take(150)}...")
            
            // First, check if this horse is scratched
            if (isHorseScratched(horseElement)) {
                println("❌ Horse is scratched - skipping")
                return null
            }
            
            // Extract horse name - NO FALLBACK
            val name = horseElement.select(".horse-name, .runner-name, .name, [class*='name'], [class*='horse']").firstOrNull()?.text()?.trim()
            if (name.isNullOrEmpty() || name == "Horse $horseNumber") {
                println("❌ No real horse name found for horse $horseNumber - skipping")
                return null
            }
            
            println("🔍 Found horse name: '$name'")
            
            // Extract SADDLE NUMBER (not sequential number) - this is the actual race number
            val saddleNumber = extractSaddleNumber(horseElement)
            if (saddleNumber == null || saddleNumber <= 0) {
                println("❌ No real saddle number found for $name - skipping")
                return null
            }
            
            println("🔍 Found saddle number: $saddleNumber")
            
            // Extract jockey - NO FALLBACK - Clean up name by removing apprentice claims and weights
            val jockeyRaw = horseElement.select(".jockey, .rider, [class*='jockey'], [class*='rider']").firstOrNull()?.text()?.trim()
            if (jockeyRaw.isNullOrEmpty()) {
                println("❌ No real jockey found for $name - skipping")
                return null
            }
            
            // Clean up jockey name by removing apprentice claims and weights
            val jockey = cleanJockeyName(jockeyRaw)
            println("🔍 Found jockey: '$jockeyRaw' -> cleaned: '$jockey'")
            
            // Extract trainer - NO FALLBACK - Clean up name by removing additional info
            val trainerRaw = horseElement.select(".trainer, [class*='trainer']").firstOrNull()?.text()?.trim()
            if (trainerRaw.isNullOrEmpty()) {
                println("❌ No real trainer found for $name - skipping")
                return null
            }
            
            // Clean up trainer name by removing additional info
            val trainer = cleanTrainerName(trainerRaw)
            println("🔍 Found trainer: '$trainerRaw' -> cleaned: '$trainer'")
            
            // Extract weight - NO FALLBACK
            val weightText = horseElement.select(".weight, .carry, [class*='weight'], [class*='carry']").firstOrNull()?.text()?.trim()
            val weight = weightText?.replace("kg", "")?.trim()?.toDoubleOrNull()
            if (weight == null || weight <= 0) {
                println("❌ No real weight found for $name - skipping")
                return null
            }
            
            // Extract BARRIER (gate number) - this is different from saddle number
            val barrier = extractBarrierNumber(horseElement)
            if (barrier == null || barrier <= 0) {
                println("❌ No real barrier found for $name - skipping")
                return null
            }
            
            println("🔍 Found barrier number: $barrier")
            
            val oddsText = horseElement.select(".odds, .price, [class*='odds'], [class*='price']").firstOrNull()?.text()?.trim()
            val odds = oddsText?.toDoubleOrNull()
            
            // DEBUG: Print all available form-related elements to understand the HTML structure
            println("🔍 DEBUG: Looking for form data in horse element for $name:")
            val allFormElements = horseElement.select("[class*='form'], [id*='form'], .form-line, .recent-form, .race-form")
            println("🔍 DEBUG: Found ${allFormElements.size} form-related elements")
            allFormElements.forEachIndexed { index, element ->
                println("🔍 DEBUG: Form element $index: class='${element.className()}', id='${element.id()}', text='${element.text().take(50)}...'")
            }
            
            // Also check if form data is in the full text but not in a specific element
            val fullElementText = horseElement.text()
            println("🔍 DEBUG: Full element text for $name: ${fullElementText.take(200)}...")
            
            // Try multiple approaches to extract form string
            var form = ""
            
            // Approach 1: Look for specific form-related CSS classes
            val formElement = horseElement.select(".form, .form-line, [class*='form'], [class*='recent'], [class*='race']").firstOrNull()
            if (formElement != null) {
                form = formElement.text().trim()
                println("🔍 DEBUG: Found form via CSS selector: '$form'")
            }
            
            // Approach 2: If no form found via CSS, try to extract from full text using regex
            if (form.isEmpty()) {
                // Look for patterns like "32x1x2212x" (numbers and x's)
                val formPattern = Regex("([0-9x]+)")
                val formMatches = formPattern.findAll(fullElementText).toList()
                
                // Find the longest match that contains both numbers and x's
                val potentialForms = formMatches.map { it.value }.filter { formStr ->
                    formStr.contains("x") && formStr.contains(Regex("[0-9]")) && formStr.length >= 3
                }
                
                if (potentialForms.isNotEmpty()) {
                    // Take the longest form string that looks valid
                    form = potentialForms.maxByOrNull { it.length } ?: ""
                    println("🔍 DEBUG: Found form via regex pattern: '$form'")
                }
            }
            
            // Approach 3: Look for form data in table cells or specific positions
            if (form.isEmpty()) {
                val cells = horseElement.select("td, span, div")
                for (cell in cells) {
                    val cellText = cell.text().trim()
                    // Look for strings that contain both numbers and x's
                    if (cellText.contains("x") && cellText.contains(Regex("[0-9]")) && 
                        cellText.length >= 3 && cellText.length <= 20) {
                        form = cellText
                        println("🔍 DEBUG: Found form in cell: '$form'")
                        break
                    }
                }
            }
            
            println("🔍 DEBUG: Final extracted form string for $name: '$form'")
            
            // Extract horse code from Racing Australia link
            val (horseCode, raceEntry) = extractHorseCodeAndRaceEntry(horseElement)
            if (horseCode == null) {
                println("❌ No real horse code found for $name - skipping")
                return null
            }
            
            val horse = Horse(
                id = "horse_${saddleNumber}_${name.replace(" ", "_")}",
                number = saddleNumber, // Use SADDLE NUMBER, not sequential number
                name = name,
                jockey = jockey,
                trainer = trainer,
                weight = weight,
                barrier = barrier,
                odds = odds,
                form = form,
                age = 0, // Will be filled from real form data
                sex = "", // Will be filled from real form data
                color = "", // Will be filled from real form data
                sire = "", // Will be filled from real form data
                dam = "", // Will be filled from real form data
                rating = null,
                lastStart = null,
                wins = 0,
                places = 0,
                earnings = 0.0,
                horseCode = horseCode,
                raceEntry = raceEntry
            )
            
            return horse
            
        } catch (e: Exception) {
            println("❌ Error parsing horse element: ${e.message}")
            return null
        }
    }
    
    /**
     * Validate that a name looks like a jockey name (not a horse name)
     */
    private fun isValidJockeyName(name: String): Boolean {
        // Common horse name patterns that we want to exclude
        val horseNamePatterns = listOf(
            "Kyokushin", "Gemini Dancer", "Gift Of Oratory", "Mystic Vamp", "Corona Lad",
            "Until Valhalla", "Outta Compton", "Princess Que", "Keep Your Cool", "Choice",
            "De Bergerac", "Campari Twist", "Forget Jack", "Tassron", "Autumnheat",
            "Brave Miss", "Bird", "Lake Vostok", "Pariah Pearl", "Cafe Millenium",
            "Too Darn Discreet", "Neil", "Extreme Virtue", "Raikoke", "Thedoctoroflove",
            "Moonlight Circus", "Cafe", "Dubai Watch", "Bold Strike", "Sigiriya Rock",
            "Barrier", "Weight", "Rtg", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th"
        )
        
        // Check if it's a known horse name or non-jockey word
        if (horseNamePatterns.any { horseName -> name.equals(horseName, ignoreCase = true) }) {
            return false
        }
        
        // Basic validation
        if (name.length < 3 || name.length > 25) return false // Jockey names are typically 3-25 characters
        if (name.matches(Regex("[A-Z\\s]+"))) return false // All caps suggests horse name
        if (name.contains("(") || name.contains(")")) return false // Parentheses suggest horse name
        if (name.contains("&")) return false // "&" suggests trainer partnership, not jockey
        
        // Check for common horse name patterns
        if (name.contains(" Of ") || name.contains(" The ") || name.contains(" And ")) return false
        if (name.matches(Regex(".*\\s+(Miss|Sir|Lady|Lord|Prince|Princess|King|Queen).*"))) return false
        
        // Check for common jockey name patterns (First Last format, typically 2-3 words)
        val jockeyPattern = Regex("^[A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,2}$")
        val isValidFormat = jockeyPattern.matches(name) && name.split(" ").size in 2..3
        
        // Additional check: make sure it doesn't look like a horse name
        val wordCount = name.split(" ").size
        val hasProperCase = name.matches(Regex("^[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*$"))
        
        return isValidFormat && hasProperCase && wordCount >= 2
    }
    
    /**
     * Validate that a name looks like a trainer name (not a horse name)
     */
    private fun isValidTrainerName(name: String): Boolean {
        // Common horse name patterns that we want to exclude
        val horseNamePatterns = listOf(
            "Kyokushin", "Gemini Dancer", "Gift Of Oratory", "Mystic Vamp", "Corona Lad",
            "Until Valhalla", "Outta Compton", "Princess Que", "Keep Your Cool", "Choice",
            "De Bergerac", "Campari Twist", "Forget Jack", "Tassron", "Autumnheat",
            "Brave Miss", "Bird", "Lake Vostok", "Pariah Pearl", "Cafe Millenium",
            "Too Darn Discreet", "Neil", "Extreme Virtue", "Raikoke", "Thedoctoroflove",
            "Moonlight Circus", "Cafe", "Dubai Watch", "Bold Strike", "Sigiriya Rock",
            "Barrier", "Weight", "Rtg", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th"
        )
        
        // Check if it's a known horse name or non-trainer word
        if (horseNamePatterns.any { horseName -> name.equals(horseName, ignoreCase = true) }) {
            return false
        }
        
        // Basic validation
        if (name.length < 3 || name.length > 40) return false // Trainer names are typically 3-40 characters
        if (name.matches(Regex("[A-Z\\s]+"))) return false // All caps suggests horse name
        if (name.contains("(") || name.contains(")")) return false // Parentheses suggest horse name
        
        // Check for common horse name patterns
        if (name.contains(" Of ") || name.contains(" The ") || name.contains(" And ")) return false
        if (name.matches(Regex(".*\\s+(Miss|Sir|Lady|Lord|Prince|Princess|King|Queen).*"))) return false
        
        // Check for common trainer name patterns (can include "&" for partnerships)
        val trainerPattern = Regex("^[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*(?:\\s+&\\s+[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)*$")
        val isValidFormat = trainerPattern.matches(name)
        
        // Additional check: make sure it doesn't look like a horse name
        val wordCount = name.split(" ").size
        val hasProperCase = name.matches(Regex("^[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*(?:\\s+&\\s+[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)*$"))
        
        return isValidFormat && hasProperCase && wordCount >= 2
    }
    
    /**
     * Clean up jockey name by removing apprentice claims and weights
     */
    private fun cleanJockeyName(jockeyRaw: String): String {
        // Remove apprentice claims like "(a2/52.5kg)" or "(a3/50kg)"
        var cleaned = Regex("\\(a\\d+/\\d+\\.?\\d*kg\\)").replace(jockeyRaw, "").trim()
        
        // Remove any remaining parentheses with weights
        cleaned = Regex("\\(\\d+\\.?\\d*kg\\)").replace(cleaned, "").trim()
        
        // Remove any remaining parentheses with numbers
        cleaned = Regex("\\(\\d+\\)").replace(cleaned, "").trim()
        
        // Remove common prefixes like Ms, Mr, etc.
        cleaned = cleaned.replace(Regex("^(Mr|Ms|Mrs|Miss|Dr|Prof)\\.?\\s+", RegexOption.IGNORE_CASE), "").trim()
        
        return cleaned
    }
    
    /**
     * Clean up trainer name by removing additional info
     */
    private fun cleanTrainerName(trainerRaw: String): String {
        // Remove any parentheses with additional info
        var cleaned = Regex("\\([^)]*\\)").replace(trainerRaw, "").trim()
        
        // Remove common suffixes like jnr, snr, etc.
        cleaned = cleaned.replace(Regex("\\s+(jnr|snr|jr|sr|j\\.|s\\.)\\.?$", RegexOption.IGNORE_CASE), "").trim()
        
        return cleaned
    }
    
    /**
     * Check if a horse is scratched (not running)
     */
    private fun isHorseScratched(horseElement: Element): Boolean {
        val elementText = horseElement.text().lowercase()
        
        // Check for scratched indicators in text - be more specific
        val scratchedTextIndicators = listOf(
            "scratched", "scr", "withdrawn", "wd", "late scratching", "late scr"
        )
        
        if (scratchedTextIndicators.any { indicator -> elementText.contains(indicator) }) {
            println("🔍 Horse marked as scratched in text")
            return true
        }
        
        // Check for CSS classes that specifically indicate scratched
        val scratchedClasses = listOf("scratched", "scr", "withdrawn", "wd", "late")
        if (scratchedClasses.any { cls -> horseElement.hasClass(cls) }) {
            println("🔍 Horse has scratched CSS class")
            return true
        }
        
        // Check if this is a header row (contains column headers)
        if (elementText.contains("no", ignoreCase = true) && 
            elementText.contains("horse", ignoreCase = true) && 
            elementText.contains("trainer", ignoreCase = true)) {
            println("🔍 This is a header row, not a horse")
            return true
        }
        
        return false
    }

    /**
     * Extract the actual saddle number from the race field
     */
    private fun extractSaddleNumber(horseElement: Element): Int? {
        // First try to find saddle number in specific elements - Racing Australia specific
        val saddleNumberSelectors = listOf(
            "td.no", ".saddle-number", ".horse-number", ".runner-number", ".number", 
            "[class*='number']", "[class*='saddle']", "[class*='runner']"
        )
        
        for (selector in saddleNumberSelectors) {
            val element = horseElement.select(selector).firstOrNull()
            if (element != null) {
                val text = element.text().trim()
                val number = text.toIntOrNull()
                if (number != null && number > 0) {
                    println("✅ Found saddle number from selector '$selector': $number")
                    return number
                }
            }
        }
        
        // If no specific element found, try to extract from the full text
        val allText = horseElement.text()
        
        // Look for the first number that appears to be a saddle number
        // Saddle numbers are typically 1-20 for most races
        val numberMatches = Regex("(\\d+)").findAll(allText).toList()
        
        for (match in numberMatches) {
            val number = match.groupValues[1].toIntOrNull()
            if (number != null && number in 1..20) {
                // Check if this number appears to be a saddle number by context
                val context = allText.substring(maxOf(0, match.range.first - 10), 
                                             minOf(allText.length, match.range.last + 10))
                
                // If the number appears near the start of the text or near horse-related terms, it's likely the saddle number
                if (context.contains("horse", ignoreCase = true) || 
                    context.contains("runner", ignoreCase = true) ||
                    match.range.first < allText.length / 3) { // Number appears in first third of text
                    println("✅ Found saddle number from text analysis: $number")
                    return number
                }
            }
        }
        
        return null
    }
    
    /**
     * Extract barrier number (gate number) from horse element
     */
    private fun extractBarrierNumber(horseElement: Element): Int? {
        // First try to find barrier in specific elements
        val barrierSelectors = listOf(
            ".barrier", ".gate", "[class*='barrier']", "[class*='gate']"
        )
        
        for (selector in barrierSelectors) {
            val element = horseElement.select(selector).firstOrNull()
            if (element != null) {
                val text = element.text().trim()
                val number = text.toIntOrNull()
                if (number != null && number > 0) {
                    println("✅ Found barrier number from selector '$selector': $number")
                    return number
                }
            }
        }
        
        // If no specific element found, try to extract from the full text
        val allText = horseElement.text()
        
        // Look for barrier-related text followed by numbers
        val barrierPatterns = listOf(
            Regex("barrier\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("gate\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("b\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("g\\s*(\\d+)", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in barrierPatterns) {
            val match = pattern.find(allText)
            if (match != null) {
                val number = match.groupValues[1].toIntOrNull()
                if (number != null && number > 0) {
                    println("✅ Found barrier number from pattern '$pattern': $number")
                    return number
                }
            }
        }
        
        // If still no barrier found, look for numbers that could be barriers
        // Barriers are typically 1-20, but we need to distinguish from saddle numbers
        val numberMatches = Regex("(\\d+)").findAll(allText).toList()
        
        // If we have multiple numbers, the second one might be the barrier
        if (numberMatches.size >= 2) {
            val secondNumber = numberMatches[1].groupValues[1].toIntOrNull()
            if (secondNumber != null && secondNumber in 1..20) {
                println("✅ Found barrier number (second number): $secondNumber")
                return secondNumber
            }
        }
        
        println("⚠️ Could not determine barrier number")
        return null
    }

    /**
     * Extract horse code and race entry from Racing Australia link
     */
    private fun extractHorseCodeAndRaceEntry(horseElement: Element): Pair<String?, String?> {
        val horseLink = horseElement.select("a[href*='HorseFullForm']").firstOrNull()
        if (horseLink != null) {
            val href = horseLink.attr("href")
            println("🔍 Horse link href: $href")
            
            // Extract horse code
            val horseCodeMatch = Regex("horsecode=([^&]+)").find(href)
            val horseCode = horseCodeMatch?.groupValues?.get(1)
            
            // Extract race entry (this is the crucial parameter for getting recent races)
            val raceEntryMatch = Regex("raceentry=([^&]+)").find(href)
            val raceEntry = raceEntryMatch?.groupValues?.get(1)
            
            println("🔍 Extracted - Horse code: $horseCode, Race entry: $raceEntry")
            
            return Pair(horseCode, raceEntry)
        }
        return Pair(null, null)
    }
    
    /**
     * Generate race entry code for horse form URLs
     */
    private fun generateRaceEntryCode(track: Track, raceNumber: Int): String {
        // Use the track key which is already in the correct format: "2025Aug27,VIC,Sportsbet Sandown Hillside"
        return "${track.key}_race${raceNumber}"
    }
    
    /**
     * Parse distance from text
     */
    private fun parseDistance(distanceText: String?): Int? {
        if (distanceText == null) return null
        
        // Try multiple patterns to handle different distance formats
        // Pattern 1: "1000m" or "1000 m"
        var distanceMatch = Regex("(\\d+)\\s*m").find(distanceText)
        if (distanceMatch != null) {
            return distanceMatch.groupValues[1].toIntOrNull()
        }
        
        // Pattern 2: "1000 METRES" or "1000 METRE"
        distanceMatch = Regex("(\\d+)\\s*METRES?").find(distanceText.uppercase())
        if (distanceMatch != null) {
            return distanceMatch.groupValues[1].toIntOrNull()
        }
        
        // No fallback - return null if no valid pattern found
        return null
    }
    
    /**
     * Scrape horse form data from Racing Australia
     */
    suspend fun scrapeHorseForm(
        horseCode: String,
        stage: String,
        key: String,
        raceEntry: String,
        formString: String = ""
    ): HorseForm? {
        return try {
            println("🔍 Scraping horse form for code: $horseCode")
            println("🔍 Stage: $stage, Key: $key, Race Entry: $raceEntry")
            println("🔍 Form string provided: '$formString'")
            
            val horseFormUrl = NetworkConfig.buildHorseFormUrl(horseCode, stage, key, raceEntry)
            println("🌐 Horse form URL: $horseFormUrl")
            
            // FILE LOGGING for debugging
            if (horseCode.contains("COSMIC VIXEN", ignoreCase = true)) {
                try {
                    val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                    val writer = FileWriter(logFile, true)
                    writer.appendLine("🔍 SCRAPER START: $horseCode")
                    writer.appendLine("🔍 Stage: $stage, Key: $key, Race Entry: $raceEntry")
                    writer.appendLine("🌐 Horse form URL: $horseFormUrl")
                    writer.close()
                } catch (e: Exception) {
                    println("⚠️ Could not write to debug file: ${e.message}")
                }
            }
            
            val request = NetworkConfig.createRequestBuilder(horseFormUrl).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                println("❌ Failed to fetch horse form: ${response.code}")
                // FILE LOGGING for debugging
                if (horseCode.contains("COSMIC VIXEN", ignoreCase = true)) {
                    try {
                        val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                        val writer = FileWriter(logFile, true)
                        writer.appendLine("❌ HTTP ERROR: ${response.code}")
                        writer.close()
                    } catch (e: Exception) {
                        println("⚠️ Could not write to debug file: ${e.message}")
                    }
                }
                return null
            }
            
            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                println("❌ Empty response from horse form")
                // FILE LOGGING for debugging
                if (horseCode.contains("COSMIC VIXEN", ignoreCase = true)) {
                    try {
                        val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                        val writer = FileWriter(logFile, true)
                        writer.appendLine("❌ EMPTY RESPONSE BODY")
                        writer.close()
                    } catch (e: Exception) {
                        println("⚠️ Could not write to debug file: ${e.message}")
                    }
                }
                return null
            }
            
            println("🔍 Horse form HTML response length: ${html.length} characters")
            
            // FILE LOGGING for debugging
            if (horseCode.contains("COSMIC VIXEN", ignoreCase = true)) {
                try {
                    val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                    val writer = FileWriter(logFile, true)
                    writer.appendLine("✅ HTTP SUCCESS: Got HTML response (${html.length} characters)")
                    writer.close()
                } catch (e: Exception) {
                    println("⚠️ Could not write to debug file: ${e.message}")
                }
            }
            println("🔍 HTML preview (first 1000 chars): ${html.take(1000)}...")
            
            val doc = Jsoup.parse(html)
            println("🔍 Document title: ${doc.title()}")
            
            // Debug: Look for any tables or race-related content
            val allTables = doc.select("table")
            println("🔍 Found ${allTables.size} tables in horse form")
            
            allTables.take(3).forEachIndexed { index, table ->
                println("🔍 Table $index:")
                println("   Class: ${table.className()}")
                println("   Rows: ${table.select("tr").size}")
                println("   Text preview: ${table.text().take(200)}...")
            }
            
            val horseForm = parseHorseFormFromDocument(doc, horseCode, formString)
            
            if (horseForm != null) {
                println("✅ Successfully parsed horse form with ${horseForm.last5Races.size} recent races")
                // Debug: Print details of each race found
                horseForm.last5Races.forEachIndexed { index, race ->
                    println("🔍 Horse Form Race ${index + 1}: Position=${race.position}, Margin=${race.margin}, Track=${race.track}")
                }
            } else {
                println("❌ Failed to parse horse form data")
            }
            
            horseForm
            
        } catch (e: Exception) {
            println("❌ Error scraping horse form: ${e.message}")
            e.printStackTrace()
            // FILE LOGGING for debugging
            if (horseCode.contains("COSMIC VIXEN", ignoreCase = true)) {
                try {
                    val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                    val writer = FileWriter(logFile, true)
                    writer.appendLine("❌ SCRAPER ERROR for COSMIC VIXEN: ${e.message}")
                    writer.appendLine("Stack trace: ${e.stackTraceToString()}")
                    writer.close()
                } catch (fileError: Exception) {
                    println("⚠️ Could not write to debug file: ${fileError.message}")
                }
            }
            null
        }
    }
    
    /**
     * Parse horse form from HTML document
     */
    private fun parseHorseFormFromDocument(doc: Document, horseCode: String, formString: String = ""): HorseForm? {
        try {
            // Parse last 5 races
            val last5Races = parseRecentRaces(doc)
            
            // Parse track/distance history
            val trackDistanceHistory = parseTrackDistanceHistory(doc)
            
            // Parse up results (first, second, third up performance)
            val upResults = parseUpResults(doc)
            
            // Parse trial sectional times
            val trialSectionalTimes = parseTrialSectionalTimes(doc)
            
            // Parse track/distance statistics
            val trackDistanceStats = parseTrackDistanceStats(doc)
            
            // CRITICAL FIX: Parse form string into individual races
            // If form string is provided, use form string races but enhance them with HTML data (dates, track conditions)
            // Otherwise fall back to HTML-parsed races
            val allRaces = if (formString.isNotEmpty()) {
                println("🔍 FORM PARSER: Using provided form string '$formString'")
                val formRaces = parseFormStringToRaces(formString)
                println("🔍 FORM PARSER: Parsed ${formRaces.size} races from form string")
                
                // ENHANCEMENT: Try to enhance form string races with HTML data (dates, track conditions)
                val enhancedRaces = enhanceFormRacesWithHtmlData(formRaces, last5Races)
                println("🔍 FORM PARSER: Enhanced ${enhancedRaces.size} races with HTML data")
                enhancedRaces
            } else {
                println("🔍 FORM PARSER: No form string provided, using ${last5Races.size} HTML races")
                last5Races
            }
            
            println("🔍 Form parsing result: Total races=${allRaces.size}")
            
            // CRITICAL FALLBACK: If HTML parsing failed, build stats from race history
            // But only build general stats, NOT track-specific stats since we don't know current race details
            val finalTrackDistanceStats = trackDistanceStats ?: buildGeneralStatsFromRaces(allRaces)
            
            return HorseForm(
                horseId = horseCode,
                last5Races = allRaces,
                trackDistanceHistory = trackDistanceHistory,
                upResults = upResults,
                trialSectionalTimes = trialSectionalTimes,
                trackDistanceStats = finalTrackDistanceStats
            )
            
        } catch (e: Exception) {
            println("❌ Error parsing horse form document: ${e.message}")
            // FILE LOGGING for debugging
            if (horseCode.contains("COSMIC VIXEN", ignoreCase = true)) {
                try {
                    val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                    val writer = FileWriter(logFile, true)
                    writer.appendLine("❌ PARSING ERROR for COSMIC VIXEN: ${e.message}")
                    writer.appendLine("Stack trace: ${e.stackTraceToString()}")
                    writer.close()
                } catch (fileError: Exception) {
                    println("⚠️ Could not write to debug file: ${fileError.message}")
                }
            }
            return null
        }
    }
    
    /**
     * Parse recent race results
     */
    private fun parseRecentRaces(doc: Document): List<RaceResultDetail> {
        val races = mutableListOf<RaceResultDetail>()
        
        println("🔍 Parsing recent races from horse form document...")
        
        // First, specifically look for the interactive-race-fields table that contains the actual race results
        val interactiveRaceFieldsTable = doc.select("table.interactive-race-fields").firstOrNull()
        
        if (interactiveRaceFieldsTable == null) {
            println("⚠️ No interactive-race-fields table found.")
            return emptyList()
        }
        
        // Select all rows within this table, skipping the header row (assuming the first row is the header)
        val raceRows = interactiveRaceFieldsTable.select("tr").drop(1) // Skip header row
        println("🔍 Found ${raceRows.size} potential race result rows in interactive-race-fields table")
        
        if (raceRows.isEmpty()) {
            println("⚠️ No actual race result rows found in interactive-race-fields table.")
            return emptyList()
        }
        
        // Limit to the last 5 races as per the HorseForm model
        raceRows.take(5).forEachIndexed { index, rowElement ->
            try {
                println("🔍 Processing race result row ${index + 1}:")
                println("   Tag: ${rowElement.tagName()}")
                println("   Class: ${rowElement.className()}")
                println("   Text preview: ${rowElement.text().take(200)}...")
                
                val race = parseRaceResultFromElement(rowElement)
                if (race != null) {
                    races.add(race)
                    println("✅ Successfully parsed race result ${index + 1}")
                    
                    // CRITICAL DEBUG: Print the entire RaceResultDetail object
                    println("🔍 DEBUG: Full RaceResultDetail object for race ${index + 1}:")
                    println("   Position: ${race.position}")
                    println("   Margin: ${race.margin}")
                    println("   Track: ${race.track}")
                    println("   Distance: ${race.distance}")
                    println("   Track Condition: ${race.trackCondition}")
                    println("   Sectional Time: ${race.sectionalTime}")
                    println("   Date: ${race.date}")
                    println("   Jockey: ${race.jockey}")
                    println("   Trainer: ${race.trainer}")
                    println("   Race Class: ${race.raceClass}")
                } else {
                    println("⚠️ Failed to parse race result ${index + 1}")
                }
            } catch (e: Exception) {
                println("❌ Error parsing race result row ${index + 1}: ${e.message}")
                e.printStackTrace()
            }
        }
        
        println("✅ Parsed ${races.size} recent races")
        
        // CRITICAL FIX: Filter out races with null dates (these are likely today's results)
        val filteredRaces = races.filter { race ->
            if (race.date == null) {
                println("🗓️ SCRAPER FILTER: Excluding race with NULL date (Position=${race.position}, Track=${race.track}) - likely today's result")
                false
            } else {
                true
            }
        }
        
        println("✅ After filtering null dates: ${filteredRaces.size} races remaining (excluded ${races.size - filteredRaces.size} races)")
        return filteredRaces
    }
    
    /**
     * CRITICAL FALLBACK: Build only general distance statistics from race history when HTML parsing fails
     * This ensures distance success calculations work, but does NOT create fake track-specific stats
     */
    private fun buildGeneralStatsFromRaces(races: List<RaceResultDetail>): TrackDistanceStats {
        println("🔧 FALLBACK: Building general distance stats from ${races.size} races (NO track-specific stats)")
        
        // Only build distance statistics, NOT track-specific or combined stats
        var distanceRuns = 0
        var distanceWins = 0
        var distancePlaces = 0
        
        val distanceGroups = races.groupBy { it.distance }
        
        // Calculate distance statistics only
        for ((distance, distanceRaces) in distanceGroups) {
            distanceRuns += distanceRaces.size
            distanceWins += distanceRaces.count { it.position == 1 }
            distancePlaces += distanceRaces.count { it.position <= 3 }
        }
        
        val distanceStats = PerformanceStats(
            runs = distanceRuns,
            wins = distanceWins,
            seconds = distancePlaces - distanceWins,
            thirds = 0
        )
        
        // Return stats with NO track-specific or combined data (use empty stats)
        return TrackDistanceStats(
            trackStats = PerformanceStats(0, 0, 0, 0), // NO track stats - horse hasn't raced at current track
            distanceStats = distanceStats, // General distance stats only
            combinedStats = PerformanceStats(0, 0, 0, 0), // NO combined stats - no track+distance history
            conditionStats = null // NO condition stats - no track-specific history
        )
    }
    
    /**
     * DEPRECATED: This function was creating incorrect track-specific stats
     * Use buildGeneralStatsFromRaces instead
     */
    private fun buildTrackDistanceStatsFromRaces(races: List<RaceResultDetail>): TrackDistanceStats {
        println("🔧 FALLBACK: Building track/distance stats from ${races.size} races")
        
        // Count wins, places, and runs for each category
        var trackRuns = 0
        var trackWins = 0
        var trackPlaces = 0
        
        var distanceRuns = 0
        var distanceWins = 0
        var distancePlaces = 0
        
        var combinedRuns = 0
        var combinedWins = 0
        var combinedPlaces = 0
        
        var conditionRuns = 0
        var conditionWins = 0
        var conditionPlaces = 0
        
        // Group races by track, distance, track+distance combination, and track condition
        val trackGroups = races.groupBy { it.track }
        val distanceGroups = races.groupBy { it.distance }
        val combinedGroups = races.groupBy { "${it.track}_${it.distance}" }
        val conditionGroups = races.groupBy { it.trackCondition }
        
        // Calculate track statistics
        for ((track, trackRaces) in trackGroups) {
            trackRuns += trackRaces.size
            trackWins += trackRaces.count { it.position == 1 }
            trackPlaces += trackRaces.count { it.position <= 3 }
        }
        
        // Calculate distance statistics  
        for ((distance, distanceRaces) in distanceGroups) {
            distanceRuns += distanceRaces.size
            distanceWins += distanceRaces.count { it.position == 1 }
            distancePlaces += distanceRaces.count { it.position <= 3 }
        }
        
        // Calculate combined track+distance statistics
        for ((combined, combinedRaces) in combinedGroups) {
            combinedRuns += combinedRaces.size
            combinedWins += combinedRaces.count { it.position == 1 }
            combinedPlaces += combinedRaces.count { it.position <= 3 }
        }
        
        // Calculate track condition statistics
        for ((condition, conditionRaces) in conditionGroups) {
            conditionRuns += conditionRaces.size
            conditionWins += conditionRaces.count { it.position == 1 }
            conditionPlaces += conditionRaces.count { it.position <= 3 }
        }
        
        val trackStats = PerformanceStats(
            runs = trackRuns,
            wins = trackWins,
            seconds = trackPlaces - trackWins, // places minus wins = 2nd+3rd
            thirds = 0 // We don't track 3rd separately, just places
        )
        
        val distanceStats = PerformanceStats(
            runs = distanceRuns,
            wins = distanceWins,
            seconds = distancePlaces - distanceWins,
            thirds = 0
        )
        
        val combinedStats = PerformanceStats(
            runs = combinedRuns,
            wins = combinedWins,
            seconds = combinedPlaces - combinedWins,
            thirds = 0
        )
        
        val conditionStats = PerformanceStats(
            runs = conditionRuns,
            wins = conditionWins,
            seconds = conditionPlaces - conditionWins,
            thirds = 0
        )
        
        println("🔧 FALLBACK STATS: Track=${trackStats.runs}:${trackStats.wins}-${trackStats.seconds}, Distance=${distanceStats.runs}:${distanceStats.wins}-${distanceStats.seconds}, Combined=${combinedStats.runs}:${combinedStats.wins}-${combinedStats.seconds}, Condition=${conditionStats.runs}:${conditionStats.wins}-${conditionStats.seconds}")
        
        return TrackDistanceStats(
            trackStats = trackStats,
            distanceStats = distanceStats,
            combinedStats = combinedStats,
            conditionStats = conditionStats
        )
    }
    
    /**
     * Parse individual race result
     */
    private fun parseRaceResultFromElement(element: Element): RaceResultDetail? {
        try {
            println("🔍 Parsing race result element:")
            println("   Text content: ${element.text().take(300)}...")
            
            val allText = element.text()
            
            // CRITICAL FIX: Exclude trials and jump outs - they are NOT real races!
            // Log the full text for debugging
            println("🔍 FULL TEXT FOR FILTERING: ${allText}")
            
            // Check for trial/jumpout indicators
            val isTrial = allText.contains("Trial", ignoreCase = true) || 
                         allText.contains("JumpOut", ignoreCase = true) ||
                         allText.matches(Regex("^[TJ]\\s+\\d+.*", RegexOption.IGNORE_CASE))
            
            if (isTrial) {
                println("⚠️ SKIPPING: This is a Trial or Jump Out, not a real race")
                println("🔍 Text: ${allText.take(100)}...")
                return null
            } else {
                println("✅ INCLUDING: This appears to be a real race")
            }
            
            // Parse the specific Racing Australia format:
            // "7th of 11 BRAT 29Jun25 1500m Soft7 MDN-SW $37,500 ($750) John Allen 57.5kg Barrier 7 1st Tassron 59.5kg, 2nd Autumnheat 59.5kg 1:34.04 (600m 36.89), 7.9L, 5th@800m, 6th@400m, $10/$16/$15/$16"
            
            var position = 0
            var margin: Double? = null
            var raceClass = ""
            var track = ""
            var distance = 0
            var trackCondition = ""
            var sectionalTime: Double? = null
            var date: Date? = null
            var jockey = ""
            var trainer = ""
            
            // Extract position with multiple format support
            // Pattern 1: "7th of 11" format (original)
            val positionMatch1 = Regex("(\\d+)(?:st|nd|rd|th)\\s+of\\s+\\d+").find(allText)
            if (positionMatch1 != null) {
                position = positionMatch1.groupValues[1].toIntOrNull() ?: 0
                println("✅ Found position (format 1): $position")
            } else {
                // Pattern 2: "7/11" or "7 of 11" format
                val positionMatch2 = Regex("(\\d+)\\s*(?:/|of)\\s*\\d+").find(allText)
                if (positionMatch2 != null) {
                    position = positionMatch2.groupValues[1].toIntOrNull() ?: 0
                    println("✅ Found position (format 2): $position")
                } else {
                    // Pattern 3: "7th/11" format
                    val positionMatch3 = Regex("(\\d+)(?:st|nd|rd|th)\\s*/\\s*\\d+").find(allText)
                    if (positionMatch3 != null) {
                        position = positionMatch3.groupValues[1].toIntOrNull() ?: 0
                        println("✅ Found position (format 3): $position")
                    } else {
                        // Pattern 4: Just "7th" format (original fallback)
                        val positionMatch4 = Regex("(\\d+)(?:st|nd|rd|th)").find(allText)
                        if (positionMatch4 != null) {
                            position = positionMatch4.groupValues[1].toIntOrNull() ?: 0
                            println("✅ Found position (format 4): $position")
                        } else {
                            // Pattern 5: Look for standalone position numbers at start of text
                            // This catches cases where position might be just "7" at the beginning
                            val positionMatch5 = Regex("^(\\d{1,2})\\s").find(allText.trim())
                            if (positionMatch5 != null) {
                                val potentialPosition = positionMatch5.groupValues[1].toIntOrNull() ?: 0
                                // Only accept if it's a reasonable position (1-20)
                                if (potentialPosition in 1..20) {
                                    position = potentialPosition
                                    println("✅ Found position (format 5): $position")
                                }
                            }
                        }
                    }
                }
            }
            
            if (position == 0) {
                println("⚠️ Could not extract position, skipping race result")
                println("🔍 DEBUG: Text that failed to parse position: '${allText.take(200)}...'")
                println("🔍 DEBUG: Attempted patterns: '7th of 11', '7/11', '7 of 11', '7th/11', '7th', '7 '")
                return null
            }
            
            // Extract track (3-4 letter code like "W FM") - comes after position
            val trackMatch = Regex("\\d+(?:st|nd|rd|th)\\s+of\\s+\\d+\\s+([A-Z]{3,4})\\s+").find(allText)
            if (trackMatch != null) {
                track = trackMatch.groupValues[1].trim()
                println("✅ Found track: $track")
            }
            
            // Extract date (format like "24Sep25") - comes after track code
            val dateMatch = Regex("([A-Z]{3,4})\\s+(\\d{1,2}[A-Za-z]{3}\\d{2})").find(allText)
            if (dateMatch != null) {
                val dateStr = dateMatch.groupValues[2].trim()
                date = parseRacingAustraliaDate(dateStr)
                println("✅ Found date: $dateStr -> $date")
            } else {
                println("⚠️ Could not find date in race result")
                println("🔍 DEBUG: Text being searched: '${allText.take(200)}...'")
            }
            
            // Extract distance (format like "1500m")
            val distanceMatch = Regex("(\\d{1,4})m").find(allText)
            if (distanceMatch != null) {
                distance = distanceMatch.groupValues[1].toIntOrNull() ?: 0
                println("✅ Found distance: ${distance}m")
            }
            
            // Extract track condition (format like "Soft7")
            val conditionMatch = Regex("(Good|Soft|Heavy|Firm|Synthetic|Wet|Slow|Dead)(\\d+)?").find(allText)
            if (conditionMatch != null) {
                trackCondition = conditionMatch.groupValues[1].trim()
                val conditionNumber = conditionMatch.groupValues[2]
                if (conditionNumber.isNotEmpty()) {
                    trackCondition += conditionNumber
                }
                println("✅ Found track condition: $trackCondition")
            }
            
            // Extract race class (format like "MDN-SW")
            val classMatch = Regex("(MDN-SW|MDN|CL[0-9]+|BM[0-9]+|OPEN|HCP|STAKES|CUP|PLATE)").find(allText)
            if (classMatch != null) {
                raceClass = classMatch.groupValues[1].trim()
                println("✅ Found race class: $raceClass")
            }
            
            // Extract margin (format like "7.9L")
            val marginMatch = Regex("(\\d+(?:\\.\\d+)?)L").find(allText)
            if (marginMatch != null) {
                margin = marginMatch.groupValues[1].toDoubleOrNull()
                println("✅ Found margin: ${margin}L")
            }
            
            // DEBUG: Print the full text to understand the actual format
            println("🔍 DEBUG: Full race result text: $allText")
            
            // Extract jockey and trainer using a more comprehensive approach
            // Let's try multiple patterns to find jockey and trainer names
            
            // Pattern 1: Look for jockey name after prize money and before weight - more specific
            val jockeyPattern1 = Regex("\\$\\d{1,3}(?:,\\d{3})*(?:\\s+\\([^)]+\\))?\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,2})\\s+\\d+\\.?\\d*kg")
            val jockeyMatch1 = jockeyPattern1.find(allText)
            if (jockeyMatch1 != null) {
                val potentialJockey = jockeyMatch1.groupValues[1].trim()
                println("🔍 DEBUG: Pattern 1 found potential jockey: '$potentialJockey'")
                if (isValidJockeyName(potentialJockey)) {
                    jockey = cleanJockeyName(potentialJockey)
                    println("✅ Found jockey (pattern 1): $jockey")
                } else {
                    println("⚠️ Pattern 1: '$potentialJockey' doesn't look like a jockey name")
                }
            }
            
            // Pattern 2: Look for jockey name before weight - more specific (2-3 words, proper case)
            if (jockey.isEmpty()) {
                val jockeyPattern2 = Regex("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,2})\\s+\\d+\\.?\\d*kg")
                val jockeyMatch2 = jockeyPattern2.find(allText)
                if (jockeyMatch2 != null) {
                    val potentialJockey = jockeyMatch2.groupValues[1].trim()
                    println("🔍 DEBUG: Pattern 2 found potential jockey: '$potentialJockey'")
                    if (isValidJockeyName(potentialJockey)) {
                        jockey = cleanJockeyName(potentialJockey)
                        println("✅ Found jockey (pattern 2): $jockey")
                    } else {
                        println("⚠️ Pattern 2: '$potentialJockey' doesn't look like a jockey name")
                    }
                }
            }
            
            // Pattern 3: Look for jockey name in the format "Name (a) Weight" or "Name Weight" - more specific
            if (jockey.isEmpty()) {
                val jockeyPattern3 = Regex("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,2})(?:\\s+\\([^)]+\\))?\\s+\\d+\\.?\\d*kg")
                val jockeyMatch3 = jockeyPattern3.find(allText)
                if (jockeyMatch3 != null) {
                    val potentialJockey = jockeyMatch3.groupValues[1].trim()
                    println("🔍 DEBUG: Pattern 3 found potential jockey: '$potentialJockey'")
                    if (isValidJockeyName(potentialJockey)) {
                        jockey = cleanJockeyName(potentialJockey)
                        println("✅ Found jockey (pattern 3): $jockey")
                    } else {
                        println("⚠️ Pattern 3: '$potentialJockey' doesn't look like a jockey name")
                    }
                }
            }
            
            // Extract trainer - try multiple patterns
            // Pattern 1: Look for trainer name after weight and before "Barrier" - more specific
            val trainerPattern1 = Regex("\\d+\\.?\\d*kg\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})\\s+Barrier")
            val trainerMatch1 = trainerPattern1.find(allText)
            if (trainerMatch1 != null) {
                val potentialTrainer = trainerMatch1.groupValues[1].trim()
                println("🔍 DEBUG: Pattern 1 found potential trainer: '$potentialTrainer'")
                if (isValidTrainerName(potentialTrainer)) {
                    trainer = cleanTrainerName(potentialTrainer)
                    println("✅ Found trainer (pattern 1): $trainer")
                } else {
                    println("⚠️ Pattern 1: '$potentialTrainer' doesn't look like a trainer name")
                }
            }
            
            // Pattern 2: Look for trainer name after jockey and weight - more specific
            if (trainer.isEmpty() && jockey.isNotEmpty()) {
                val trainerPattern2 = Regex("$jockey\\s+\\d+\\.?\\d*kg\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})(?:\\s+Barrier|\\s+\\d+)")
                val trainerMatch2 = trainerPattern2.find(allText)
                if (trainerMatch2 != null) {
                    val potentialTrainer = trainerMatch2.groupValues[1].trim()
                    println("🔍 DEBUG: Pattern 2 found potential trainer: '$potentialTrainer'")
                    if (isValidTrainerName(potentialTrainer)) {
                        trainer = cleanTrainerName(potentialTrainer)
                        println("✅ Found trainer (pattern 2): $trainer")
                    } else {
                        println("⚠️ Pattern 2: '$potentialTrainer' doesn't look like a trainer name")
                    }
                }
            }
            
            // Pattern 3: Look for trainer name in the general area after weight - more specific
            if (trainer.isEmpty()) {
                val trainerPattern3 = Regex("\\d+\\.?\\d*kg\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})(?:\\s+(?:Barrier|\\d+))")
                val trainerMatch3 = trainerPattern3.find(allText)
                if (trainerMatch3 != null) {
                    val potentialTrainer = trainerMatch3.groupValues[1].trim()
                    println("🔍 DEBUG: Pattern 3 found potential trainer: '$potentialTrainer'")
                    if (isValidTrainerName(potentialTrainer)) {
                        trainer = cleanTrainerName(potentialTrainer)
                        println("✅ Found trainer (pattern 3): $trainer")
                    } else {
                        println("⚠️ Pattern 3: '$potentialTrainer' doesn't look like a trainer name")
                    }
                }
            }
            
            // Pattern 4: Look for trainer name after the jockey name and weight - more specific
            if (trainer.isEmpty() && jockey.isNotEmpty()) {
                val trainerPattern4 = Regex("$jockey\\s+\\d+\\.?\\d*kg\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})(?:\\s+Barrier|\\s+\\d+)")
                val trainerMatch4 = trainerPattern4.find(allText)
                if (trainerMatch4 != null) {
                    val potentialTrainer = trainerMatch4.groupValues[1].trim()
                    println("🔍 DEBUG: Pattern 4 found potential trainer: '$potentialTrainer'")
                    if (isValidTrainerName(potentialTrainer)) {
                        trainer = cleanTrainerName(potentialTrainer)
                        println("✅ Found trainer (pattern 4): $trainer")
                    } else {
                        println("⚠️ Pattern 4: '$potentialTrainer' doesn't look like a trainer name")
                    }
                }
            }
            
            // Extract sectional time (format like "36.89" in parentheses)
            val sectionalMatch = Regex("\\(600m\\s+(\\d+(?:\\.\\d+)?)\\)").find(allText)
            if (sectionalMatch != null) {
                sectionalTime = sectionalMatch.groupValues[1].toDoubleOrNull()
                println("✅ Found sectional time: ${sectionalTime}s")
            }
            
            println("✅ Extracted race result: Position=$position, Track=$track, Distance=$distance, Condition=$trackCondition, Margin=$margin")
            
            return RaceResultDetail(
                position = position,
                margin = margin,
                raceClass = raceClass,
                track = track,
                distance = distance,
                trackCondition = trackCondition,
                sectionalTime = sectionalTime,
                date = date,
                jockey = jockey,
                trainer = trainer
            )
            
        } catch (e: Exception) {
            println("❌ Error parsing race result element: ${e.message}")
            return null
        }
    }
    
    /**
     * Parse track/distance history
     */
    private fun parseTrackDistanceHistory(doc: Document): List<RaceResultDetail> {
        // For now, return the same as recent races
        // This could be enhanced to look for specific track/distance combinations
        return parseRecentRaces(doc)
    }
    
    /**
     * Parse up results (first, second, third up performance)
     */
    private fun parseUpResults(doc: Document): UpResult {
        try {
            // Get all text from the document to search for 1st Up and 2nd Up patterns
            val allText = doc.text()
            
            // Parse 1st Up statistics (e.g., "1st Up: 3:1-0-0")
            val firstUpStats = parseSpellPerformance(allText, "1st Up")
            
            // Parse 2nd Up statistics (e.g., "2nd Up: 2:2-0-0")
            val secondUpStats = parseSpellPerformance(allText, "2nd Up")
            
            // For backward compatibility, calculate total wins, places, runs
            val totalWins = (firstUpStats?.wins ?: 0) + (secondUpStats?.wins ?: 0)
            val totalPlaces = (firstUpStats?.seconds ?: 0) + (firstUpStats?.thirds ?: 0) + 
                             (secondUpStats?.seconds ?: 0) + (secondUpStats?.thirds ?: 0)
            val totalRuns = (firstUpStats?.runs ?: 0) + (secondUpStats?.runs ?: 0)
            
            println("🏇 Parsed Up Results:")
            println("   1st Up: ${firstUpStats?.runs ?: 0} runs, ${firstUpStats?.wins ?: 0} wins, ${firstUpStats?.seconds ?: 0} seconds, ${firstUpStats?.thirds ?: 0} thirds")
            println("   2nd Up: ${secondUpStats?.runs ?: 0} runs, ${secondUpStats?.wins ?: 0} wins, ${secondUpStats?.seconds ?: 0} seconds, ${secondUpStats?.thirds ?: 0} thirds")
            
            return UpResult(
                wins = totalWins,
                places = totalPlaces,
                runs = totalRuns,
                firstUpStats = firstUpStats,
                secondUpStats = secondUpStats
            )
        } catch (e: Exception) {
            println("❌ Error parsing up results: ${e.message}")
        }
        
        // Default values if parsing fails
        return UpResult(0, 0, 0, null, null)
    }
    
    /**
     * Parse spell performance statistics from text (e.g., "1st Up: 3:1-0-0")
     */
    private fun parseSpellPerformance(text: String, label: String): SpellPerformance? {
        try {
            // Look for patterns like "1st Up: 3:1-0-0" or "2nd Up: 2:2-0-0"
            val pattern = Regex("$label:\\s*(\\d+):(\\d+)-(\\d+)-(\\d+)")
            val match = pattern.find(text)
            
            if (match != null) {
                val runs = match.groupValues[1].toInt()
                val wins = match.groupValues[2].toInt()
                val seconds = match.groupValues[3].toInt()
                val thirds = match.groupValues[4].toInt()
                
                println("🏇 Found $label stats: $runs runs, $wins wins, $seconds seconds, $thirds thirds")
                
                return SpellPerformance(
                    runs = runs,
                    wins = wins,
                    seconds = seconds,
                    thirds = thirds
                )
            }
        } catch (e: Exception) {
            println("❌ Error parsing $label performance: ${e.message}")
        }
        
        return null
    }
    
    /**
     * Parse trial sectional times
     */
    private fun parseTrialSectionalTimes(doc: Document): List<Double> {
        val sectionalTimes = mutableListOf<Double>()
        
        try {
            // Look for trial results
            val trialElements = doc.select(".trial, .trial-result, [class*='trial']")
            
            trialElements.forEach { trialElement ->
                val sectionalText = trialElement.select(".sectional, .600m, .time").firstOrNull()?.text()?.trim()
                val sectionalTime = parseSectionalTime(sectionalText)
                if (sectionalTime != null) {
                    sectionalTimes.add(sectionalTime)
                }
            }
        } catch (e: Exception) {
            println("❌ Error parsing trial sectional times: ${e.message}")
        }
        
        return sectionalTimes
    }
    
    /**
     * Parse margin from text
     */
    private fun parseMargin(marginText: String?): Double? {
        if (marginText == null) return null
        
        val marginMatch = Regex("(\\d+(?:\\.\\d+)?)").find(marginText)
        return marginMatch?.groupValues?.get(1)?.toDoubleOrNull()
    }
    
    /**
     * Parse sectional time from text
     */
    private fun parseSectionalTime(sectionalText: String?): Double? {
        if (sectionalText == null) return null
        
        val sectionalMatch = Regex("(\\d+(?:\\.\\d+)?)").find(sectionalText)
        return sectionalMatch?.groupValues?.get(1)?.toDoubleOrNull()
    }
    
    /**
     * Parse date from text
     */
    private fun parseDate(dateText: String?): Date? {
        if (dateText == null) return null
        
        return try {
            // Try different date formats
            val formats = listOf(
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy",
                "MMM dd, yyyy"
            )
            
            for (format in formats) {
                try {
                    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                    return dateFormat.parse(dateText)
                } catch (e: Exception) {
                    // Try next format
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse Racing Australia specific date format (e.g., "29Jun25")
     */
    private fun parseRacingAustraliaDate(dateStr: String): Date? {
        return try {
            // Racing Australia format: "29Jun25" -> "29 Jun 2025"
            val dateFormat = SimpleDateFormat("ddMMMyy", Locale.ENGLISH)
            return dateFormat.parse(dateStr)
        } catch (e: Exception) {
            println("❌ Error parsing Racing Australia date '$dateStr': ${e.message}")
            null
        }
    }

    /**
     * CRITICAL FIX: Parse form string (like "7x3", "2x1x4") into individual RaceResultDetail objects
     * This fixes the issue where horses like Miss Lola only show 1 race instead of multiple races
     */
    private fun parseRacesFromFormString(horseCode: String, existingRaces: List<RaceResultDetail>): List<RaceResultDetail> {
        println("🔍 FORM STRING PARSER: Starting for $horseCode")
        
        // We need to get the form string from the horse data, but we don't have direct access here
        // For now, let's create a simple parser that can handle common patterns
        // This is a temporary solution - we need to pass the form string to this function
        
        val parsedRaces = mutableListOf<RaceResultDetail>()
        
        // TODO: We need to modify the function signature to accept the form string
        // For now, let's implement the parsing logic structure
        
        println("🔍 FORM STRING PARSER: Need form string parameter to parse")
        
        return parsedRaces
    }
    
    /**
     * Extract form string from the horse form document
     * Looks for patterns like "7x3", "2x1x4" in the HTML
     */
    private fun extractFormStringFromDocument(doc: Document): String {
        println("🔍 FORM EXTRACTOR: Looking for form string in document")
        
        // Look for form strings in various places in the document
        val allText = doc.text()
        
        // Pattern to match form strings: numbers and x's
        val formPattern = Regex("([0-9xX]+)")
        val matches = formPattern.findAll(allText).toList()
        
        // Find the longest match that looks like a form string
        val potentialForms = matches.map { it.value }.filter { formStr ->
            formStr.contains("x", ignoreCase = true) && 
            formStr.contains(Regex("[0-9]")) && 
            formStr.length >= 2 && 
            formStr.length <= 10
        }
        
        val formString = potentialForms.maxByOrNull { it.length } ?: ""
        
        if (formString.isNotEmpty()) {
            println("🔍 FORM EXTRACTOR: Found form string '$formString'")
        } else {
            println("🔍 FORM EXTRACTOR: No form string found")
        }
        
        return formString
    }
    
    /**
     * Parse a form string like "7x3" into individual race positions
     * Example: "7x3" -> [RaceResultDetail(position=7), RaceResultDetail(position=3)]
     */
    private fun parseFormStringToRaces(formStr: String): List<RaceResultDetail> {
        println("🔍 FORM PARSER: Parsing form string '$formStr'")
        
        val races = mutableListOf<RaceResultDetail>()
        
        if (formStr.isEmpty()) {
            println("🔍 FORM PARSER: Empty form string")
            return races
        }
        
        // CRITICAL: Only use races AFTER the last spell (X)
        // Example: "214X3361X652" -> only use "652" (after last X)
        // Find the last occurrence of 'x' or 'X'
        val lastSpellIndex = maxOf(
            formStr.lastIndexOf('x'),
            formStr.lastIndexOf('X')
        )
        
        val relevantForm = if (lastSpellIndex >= 0) {
            // Extract everything after the last spell
            val afterLastSpell = formStr.substring(lastSpellIndex + 1)
            println("🔍 FORM PARSER: Found spell at index $lastSpellIndex, using races after: '$afterLastSpell'")
            afterLastSpell
        } else {
            // No spell found, use entire form string
            println("🔍 FORM PARSER: No spell found, using entire form: '$formStr'")
            formStr
        }
        
        // Now parse each character as a race position
        // The rightmost character is the most recent race
        // Example: "652" means 2 (most recent), 5, 6 (oldest)
        relevantForm.reversed().forEachIndexed { index, char ->
            if (char.isDigit()) {
                val position = char.toString().toInt()
                if (position in 1..9) {
                    val race = RaceResultDetail(
                        position = position,
                        margin = null,
                        track = "UNKNOWN",
                        distance = 0,
                        trackCondition = "UNKNOWN",
                        sectionalTime = null,
                        date = null, // We don't have dates from form string
                        jockey = "UNKNOWN",
                        trainer = "UNKNOWN",
                        raceClass = "UNKNOWN"
                    )
                    races.add(race)
                    println("🔍 FORM PARSER: Added race ${index + 1} with position $position")
                }
            }
        }
        
        println("🔍 FORM PARSER: Created ${races.size} races from form string '$formStr' (relevant part: '$relevantForm')")
        return races
    }
    
    /**
     * Build track URL for Racing Australia
     */
    private fun buildTrackUrl(track: Track, date: Date): String {
        val dateStr = dateFormat.format(date)
        val encodedKey = track.key.replace(",", "%2C").replace(" ", "%20")
        val url = "${NetworkConfig.BASE_URL}/FreeFields/Form.aspx?Key=$encodedKey&recentForm=Y"
        println("🔗 Built track URL: $url")
        return url
    }

    /**
     * Make HTTP request with proper error handling
     */
    private suspend fun makeHttpRequest(url: String): String {
        return try {
            val request = NetworkConfig.createRequestBuilder(url).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                println("❌ HTTP request failed: ${response.code} - ${response.message}")
                return ""
            }
            
            val html = response.body?.string() ?: ""
            println("✅ HTTP Response: ${response.code}, HTML response length: ${html.length} characters")
            html
            
        } catch (e: Exception) {
            println("❌ HTTP request error: ${e.message}")
            ""
        }
    }

    /**
     * Fetch state jockey premiership data
     */
    suspend fun fetchJockeyPremiership(state: String): List<JockeyPremiership> {
        return try {
            println("🏆 Fetching jockey premiership for state: $state")
            
            val url = "https://www.racingaustralia.horse/FreeServices/Premierships.aspx?State=$state"
            println("🌐 Jockey premiership URL: $url")
            
            val request = NetworkConfig.createRequestBuilder(url).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                println("❌ Failed to fetch jockey premiership: ${response.code}")
                return emptyList()
            }
            
            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                println("❌ Empty response from jockey premiership")
                return emptyList()
            }
            
            val doc = Jsoup.parse(html)
            val jockeys = mutableListOf<JockeyPremiership>()
            
            // Parse the premiership table
            val tableRows = doc.select("table tr")
            println("🔍 Found ${tableRows.size} table rows in jockey premiership")
            
            tableRows.forEachIndexed { index, row ->
                if (index == 0) return@forEachIndexed // Skip header row
                
                // Only process top 20 jockeys from each state's premiership
                if (index > 20) return@forEachIndexed
                
                val cells = row.select("td")
                if (cells.size >= 8) {
                    try {
                        val jockeyName = cells[0].text().trim()
                        val wins = cells[1].text().trim().toIntOrNull() ?: 0
                        val seconds = cells[2].text().trim().toIntOrNull() ?: 0
                        val thirds = cells[3].text().trim().toIntOrNull() ?: 0
                        val fourths = cells[4].text().trim().toIntOrNull() ?: 0
                        val fifths = cells[5].text().trim().toIntOrNull() ?: 0
                        val prizeMoney = cells[6].text().trim()
                        val strikeRate = cells[7].text().trim()
                        val starts = cells[8].text().trim().toIntOrNull() ?: 0
                        
                        val jockey = JockeyPremiership(
                            name = jockeyName,
                            rank = index,
                            wins = wins,
                            places = seconds + thirds, // Combine 2nd and 3rd as places
                            points = wins * 3 + seconds * 2 + thirds, // Calculate points
                            totalRides = starts,
                            winPercentage = if (starts > 0) (wins.toDouble() / starts) * 100 else 0.0
                        )
                        
                        jockeys.add(jockey)
                        println("✅ Parsed jockey: $jockeyName (Rank ${index})")
                        
                    } catch (e: Exception) {
                        println("❌ Error parsing jockey row $index: ${e.message}")
                    }
                }
            }
            
            println("🏆 Successfully fetched ${jockeys.size} jockeys from $state premiership")
            jockeys
            
        } catch (e: Exception) {
            println("❌ Error fetching jockey premiership: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Fetch state trainer premiership data
     */
    suspend fun fetchTrainerPremiership(state: String): List<TrainerPremiership> {
        return try {
            println("🏆 Fetching trainer premiership for state: $state")
            
            val url = "https://www.racingaustralia.horse/FreeServices/Premierships.aspx?State=$state&Season=2025&Table=Trainer"
            println("🌐 Trainer premiership URL: $url")
            
            val request = NetworkConfig.createRequestBuilder(url).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                println("❌ Failed to fetch trainer premiership: ${response.code}")
                return emptyList()
            }
            
            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                println("❌ Empty response from trainer premiership")
                return emptyList()
            }
            
            val doc = Jsoup.parse(html)
            val trainers = mutableListOf<TrainerPremiership>()
            
            // Parse the premiership table
            val tableRows = doc.select("table tr")
            println("🔍 Found ${tableRows.size} table rows in trainer premiership")
            
            tableRows.forEachIndexed { index, row ->
                if (index == 0) return@forEachIndexed // Skip header row
                
                // Only process top 20 trainers from each state's premiership
                if (index > 20) return@forEachIndexed
                
                val cells = row.select("td")
                if (cells.size >= 8) {
                    try {
                        val trainerName = cells[0].text().trim()
                        val wins = cells[1].text().trim().toIntOrNull() ?: 0
                        val seconds = cells[2].text().trim().toIntOrNull() ?: 0
                        val thirds = cells[3].text().trim().toIntOrNull() ?: 0
                        val fourths = cells[4].text().trim().toIntOrNull() ?: 0
                        val fifths = cells[5].text().trim().toIntOrNull() ?: 0
                        val prizeMoney = cells[6].text().trim()
                        val strikeRate = cells[7].text().trim()
                        val starts = cells[8].text().trim().toIntOrNull() ?: 0
                        
                        val trainer = TrainerPremiership(
                            name = trainerName,
                            rank = index,
                            wins = wins,
                            places = seconds + thirds, // Combine 2nd and 3rd as places
                            points = wins * 3 + seconds * 2 + thirds, // Calculate points
                            totalRunners = starts,
                            winPercentage = if (starts > 0) (wins.toDouble() / starts) * 100 else 0.0
                        )
                        
                        trainers.add(trainer)
                        println("✅ Parsed trainer: $trainerName (Rank ${index})")
                        
                    } catch (e: Exception) {
                        println("❌ Error parsing trainer row $index: ${e.message}")
                    }
                }
            }
            
            println("🏆 Successfully fetched ${trainers.size} trainers from $state premiership")
            trainers
            
        } catch (e: Exception) {
            println("❌ Error fetching trainer premiership: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Parse track/distance statistics from horse form page
     * Looks for Track:, Dist:, and Track/Dist: statistics
     */
    private fun parseTrackDistanceStats(doc: Document): TrackDistanceStats? {
        try {
            println("🔍 Parsing track/distance statistics...")
            
            // Debug: Print all available sections and their text content
            println("🔍 DEBUG: Looking for track/distance statistics in HTML...")
            
            // Try multiple selectors to find the statistics section
            val possibleSelectors = listOf(
                ".career-stats",
                ".horse-stats", 
                "[class*='stat']",
                "[class*='career']",
                "[class*='track']",
                "[class*='distance']",
                "table",
                "div",
                "span"
            )
            
            var statsSection: Element? = null
            var foundInSection = ""
            
            for (selector in possibleSelectors) {
                val elements = doc.select(selector)
                println("🔍 DEBUG: Selector '$selector' found ${elements.size} elements")
                
                for (element in elements) {
                    val text = element.text()
                    if (text.contains("Track:") || text.contains("Dist:") || text.contains("Track/Dist:")) {
                        statsSection = element
                        foundInSection = selector
                        println("✅ DEBUG: Found statistics in selector '$selector'")
                        println("🔍 DEBUG: Section text preview: ${text.take(200)}...")
                        break
                    }
                }
                if (statsSection != null) break
            }
            
            if (statsSection == null) {
                println("⚠️ No statistics section found with Track:/Dist:/Track/Dist: patterns")
                println("🔍 DEBUG: Searching entire document for these patterns...")
                
                // Search the entire document text for the patterns
                val fullText = doc.text()
                if (fullText.contains("Track:") || fullText.contains("Dist:") || fullText.contains("Track/Dist:")) {
                    println("✅ DEBUG: Found Track:/Dist:/Track/Dist: patterns in document text")
                    println("🔍 DEBUG: Full document text preview: ${fullText.take(500)}...")
                    
                    // Try to parse directly from the full text
                    val trackStats = parsePerformanceStatsFromText(fullText, "Track:")
                    val distanceStats = parsePerformanceStatsFromText(fullText, "Dist:")
                    val combinedStats = parsePerformanceStatsFromText(fullText, "Track/Dist:")
                    
                    if (trackStats != null && distanceStats != null && combinedStats != null) {
                        println("✅ Successfully parsed track/distance statistics from document text:")
                        println("   Track: ${trackStats.runs}:${trackStats.wins}-${trackStats.seconds}-${trackStats.thirds}")
                        println("   Distance: ${distanceStats.runs}:${distanceStats.wins}-${distanceStats.seconds}-${distanceStats.thirds}")
                        println("   Combined: ${combinedStats.runs}:${combinedStats.wins}-${combinedStats.seconds}-${combinedStats.thirds}")
                        
                        return TrackDistanceStats(
                            trackStats = trackStats,
                            distanceStats = distanceStats,
                            combinedStats = combinedStats,
                            conditionStats = null
                        )
                    }
                } else {
                    println("❌ DEBUG: No Track:/Dist:/Track/Dist: patterns found anywhere in document")
                }
                return null
            }
            
            println("✅ DEBUG: Using statistics section found with selector '$foundInSection'")
            
            // Parse Track: X:Y-Z-W format
            val trackStats = parsePerformanceStats(statsSection, "Track:")
            
            // Parse Dist: X:Y-Z-W format  
            val distanceStats = parsePerformanceStats(statsSection, "Dist:")
            
            // Parse Track/Dist: X:Y-Z-W format
            val combinedStats = parsePerformanceStats(statsSection, "Track/Dist:")
            
            // NEW: Parse track condition statistics (Good:, Heavy:, Soft:, Firm:, Synthetic:)
            val conditionStats = parseTrackConditionStats(statsSection)
            
            if (trackStats != null && distanceStats != null && combinedStats != null) {
                println("✅ Successfully parsed track/distance statistics:")
                println("   Track: ${trackStats.runs}:${trackStats.wins}-${trackStats.seconds}-${trackStats.thirds}")
                println("   Distance: ${distanceStats.runs}:${distanceStats.wins}-${distanceStats.seconds}-${distanceStats.thirds}")
                println("   Combined: ${combinedStats.runs}:${combinedStats.wins}-${combinedStats.seconds}-${combinedStats.thirds}")
                if (conditionStats != null) {
                    println("   Track Conditions: ${conditionStats.runs}:${conditionStats.wins}-${conditionStats.seconds}-${conditionStats.thirds}")
                }
                
                return TrackDistanceStats(
                    trackStats = trackStats,
                    distanceStats = distanceStats,
                    combinedStats = combinedStats,
                    conditionStats = conditionStats
                )
            } else {
                println("⚠️ Could not parse all track/distance statistics")
                return null
            }
            
        } catch (e: Exception) {
            println("❌ Error parsing track/distance statistics: ${e.message}")
            return null
        }
    }
    
    /**
     * Parse performance statistics in format "X:Y-Z-W" (runs:wins-seconds-thirds)
     */
    private fun parsePerformanceStats(section: Element, label: String): PerformanceStats? {
        try {
            val text = section.text()
            val pattern = Regex("$label\\s*(\\d+):(\\d+)-(\\d+)-(\\d+)")
            val match = pattern.find(text)
            
            if (match != null) {
                val runs = match.groupValues[1].toInt()
                val wins = match.groupValues[2].toInt()
                val seconds = match.groupValues[3].toInt()
                val thirds = match.groupValues[4].toInt()
                
                return PerformanceStats(
                    runs = runs,
                    wins = wins,
                    seconds = seconds,
                    thirds = thirds
                )
            } else {
                println("⚠️ Could not find $label statistics in format X:Y-Z-W")
                return null
            }
        } catch (e: Exception) {
            println("❌ Error parsing $label statistics: ${e.message}")
            return null
        }
    }
    
    /**
     * Parse performance statistics from raw text in format "X:Y-Z-W" (runs:wins-seconds-thirds)
     */
    private fun parsePerformanceStatsFromText(text: String, label: String): PerformanceStats? {
        try {
            val pattern = Regex("$label\\s*(\\d+):(\\d+)-(\\d+)-(\\d+)")
            val match = pattern.find(text)
            
            if (match != null) {
                val runs = match.groupValues[1].toInt()
                val wins = match.groupValues[2].toInt()
                val seconds = match.groupValues[3].toInt()
                val thirds = match.groupValues[4].toInt()
                
                return PerformanceStats(
                    runs = runs,
                    wins = wins,
                    seconds = seconds,
                    thirds = thirds
                )
            } else {
                println("⚠️ Could not find $label statistics in format X:Y-Z-W in text")
                return null
            }
        } catch (e: Exception) {
            println("❌ Error parsing $label statistics from text: ${e.message}")
            return null
        }
    }
    
    /**
     * Parse track condition statistics from HTML element
     * Looks for patterns like "Good: 2:0-1-1", "Heavy: 1:0-0-0", etc.
     * Returns stats for the most common condition (highest number of runs)
     */
    private fun parseTrackConditionStats(element: Element): PerformanceStats? {
        try {
            println("🔍 Parsing track condition statistics...")
            
            val text = element.text()
            println("🔍 DEBUG: Searching for track condition patterns in text: ${text.take(200)}...")
            
            // Track conditions to look for
            val trackConditions = listOf("Good", "Heavy", "Soft", "Firm", "Synthetic")
            
            var bestCondition: String? = null
            var bestStats: PerformanceStats? = null
            var maxRuns = 0
            
            for (condition in trackConditions) {
                val pattern = Regex("$condition:\\s*(\\d+):(\\d+)-(\\d+)-(\\d+)")
                val match = pattern.find(text)
                
                if (match != null) {
                    val runs = match.groupValues[1].toInt()
                    val wins = match.groupValues[2].toInt()
                    val seconds = match.groupValues[3].toInt()
                    val thirds = match.groupValues[4].toInt()
                    
                    println("✅ Found $condition: $runs:$wins-$seconds-$thirds")
                    
                    // Keep track of the condition with the most runs
                    if (runs > maxRuns) {
                        maxRuns = runs
                        bestCondition = condition
                        bestStats = PerformanceStats(
                            runs = runs,
                            wins = wins,
                            seconds = seconds,
                            thirds = thirds
                        )
                    }
                }
            }
            
            if (bestStats != null) {
                println("✅ Using track condition stats for '$bestCondition': ${bestStats.runs}:${bestStats.wins}-${bestStats.seconds}-${bestStats.thirds}")
                return bestStats
            } else {
                println("⚠️ No track condition statistics found")
                return null
            }
            
        } catch (e: Exception) {
            println("❌ Error parsing track condition statistics: ${e.message}")
            return null
        }
    }
    
    /**
     * Enhance form string races with HTML data (dates, track conditions, etc.)
     * This combines the accuracy of form string positions with the richness of HTML data
     */
    private fun enhanceFormRacesWithHtmlData(formRaces: List<RaceResultDetail>, htmlRaces: List<RaceResultDetail>): List<RaceResultDetail> {
        try {
            println("🔍 ENHANCER: Starting enhancement of ${formRaces.size} form races with ${htmlRaces.size} HTML races")
            
            val enhancedRaces = mutableListOf<RaceResultDetail>()
            
            // For each form race, try to find a matching HTML race
            formRaces.forEachIndexed { index, formRace ->
                println("🔍 ENHANCER: Processing form race ${index + 1} (Position=${formRace.position})")
                
                // Try to find HTML race with matching position
                val matchingHtmlRace = htmlRaces.find { htmlRace ->
                    htmlRace.position == formRace.position
                }
                
                if (matchingHtmlRace != null) {
                    println("✅ ENHANCER: Found matching HTML race for position ${formRace.position}")
                    
                    // Create enhanced race with form position but HTML data
                    val enhancedRace = RaceResultDetail(
                        position = formRace.position, // Use form position (correct)
                        margin = matchingHtmlRace.margin, // Use HTML margin
                        track = matchingHtmlRace.track, // Use HTML track
                        distance = matchingHtmlRace.distance, // Use HTML distance
                        trackCondition = matchingHtmlRace.trackCondition, // Use HTML track condition
                        sectionalTime = matchingHtmlRace.sectionalTime, // Use HTML sectional time
                        date = matchingHtmlRace.date, // CRITICAL: Use HTML date for freshness calculation
                        jockey = matchingHtmlRace.jockey, // Use HTML jockey
                        trainer = matchingHtmlRace.trainer, // Use HTML trainer
                        raceClass = matchingHtmlRace.raceClass // Use HTML race class
                    )
                    
                    enhancedRaces.add(enhancedRace)
                    println("✅ ENHANCER: Enhanced race ${index + 1} with HTML data (Date=${enhancedRace.date})")
                } else {
                    println("⚠️ ENHANCER: No matching HTML race found for position ${formRace.position}, using form race as-is")
                    enhancedRaces.add(formRace) // Use form race without enhancement
                }
            }
            
            println("✅ ENHANCER: Enhanced ${enhancedRaces.size} races total")
            return enhancedRaces
            
        } catch (e: Exception) {
            println("❌ Error enhancing form races with HTML data: ${e.message}")
            return formRaces // Return original form races if enhancement fails
        }
    }
} 
