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
            
            // Look for track links in the calendar - both Form.aspx (upcoming races) and Results.aspx (completed races)
            val trackElements = doc.select("a[href*='Form.aspx'], a[href*='Results.aspx']")
            println("🔍 Found ${trackElements.size} track link elements")
            
            // If no tracks found, try alternative selectors
            if (trackElements.isEmpty()) {
                println("⚠️ No tracks found with 'Form.aspx' or 'Results.aspx' selector, trying alternatives...")
                val altSelectors = listOf(
                    "a[href*='form']",
                    "a[href*='Form']",
                    "a[href*='Results']",
                    "a[href*='results']",
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
                                
                                val track = Track(
                                    key = key, // This is the decoded key: "2025Aug30,NSW,Grafton"
                                    name = fullTrackName,
                                    state = state,
                                    raceCount = raceCount,
                                    url = href
                                )
                                
                                tracks.add(track)
                                println("🏁 Found track: ${track.name} (${track.state}) - ${track.raceCount} races")
                                println("   Key: $key")
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
        println("🔍 DEBUG: Analyzing HTML structure for ${track.name}")
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
            
            // Limit to maximum 8 races (reasonable limit for a race day)
            if (raceElements.size > 8) {
                println("⚠️ Found ${raceElements.size} potential race elements, limiting to first 8")
                raceElements = raceElements.take(8)
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
            
            // Limit to maximum 8 races for alternative parsing
            val limitedTables = raceTables.take(8)
            
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
            val limitedContainers = potentialRaceContainers.take(8)
            
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
            
            // Extract jockey - NO FALLBACK
            val jockey = horseElement.select(".jockey, .rider, [class*='jockey'], [class*='rider']").firstOrNull()?.text()?.trim()
            if (jockey.isNullOrEmpty()) {
                println("❌ No real jockey found for $name - skipping")
                return null
            }
            
            // Extract trainer - NO FALLBACK
            val trainer = horseElement.select(".trainer, [class*='trainer']").firstOrNull()?.text()?.trim()
            if (trainer.isNullOrEmpty()) {
                println("❌ No real trainer found for $name - skipping")
                return null
            }
            
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
            
            val form = horseElement.select(".form, .form-line, [class*='form']").firstOrNull()?.text()?.trim() ?: ""
            
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
        raceEntry: String
    ): HorseForm? {
        return try {
            println("🔍 Scraping horse form for code: $horseCode")
            println("🔍 Stage: $stage, Key: $key, Race Entry: $raceEntry")
            
            val horseFormUrl = NetworkConfig.buildHorseFormUrl(horseCode, stage, key, raceEntry)
            println("🌐 Horse form URL: $horseFormUrl")
            
            val request = NetworkConfig.createRequestBuilder(horseFormUrl).build()
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                println("❌ Failed to fetch horse form: ${response.code}")
                return null
            }
            
            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                println("❌ Empty response from horse form")
                return null
            }
            
            println("🔍 Horse form HTML response length: ${html.length} characters")
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
            
            val horseForm = parseHorseFormFromDocument(doc, horseCode)
            
            if (horseForm != null) {
                println("✅ Successfully parsed horse form with ${horseForm.last5Races.size} recent races")
            } else {
                println("❌ Failed to parse horse form data")
            }
            
            horseForm
            
        } catch (e: Exception) {
            println("❌ Error scraping horse form: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Parse horse form from HTML document
     */
    private fun parseHorseFormFromDocument(doc: Document, horseCode: String): HorseForm? {
        try {
            // Parse last 5 races
            val last5Races = parseRecentRaces(doc)
            
            // Parse track/distance history
            val trackDistanceHistory = parseTrackDistanceHistory(doc)
            
            // Parse up results (first, second, third up performance)
            val upResults = parseUpResults(doc)
            
            // Parse trial sectional times
            val trialSectionalTimes = parseTrialSectionalTimes(doc)
            
            return HorseForm(
                horseId = horseCode,
                last5Races = last5Races,
                trackDistanceHistory = trackDistanceHistory,
                upResults = upResults,
                trialSectionalTimes = trialSectionalTimes
            )
            
        } catch (e: Exception) {
            println("❌ Error parsing horse form document: ${e.message}")
            return null
        }
    }
    
    /**
     * Parse recent race results
     */
    private fun parseRecentRaces(doc: Document): List<RaceResultDetail> {
        val races = mutableListOf<RaceResultDetail>()
        
        println("🔍 Parsing recent races from horse form document...")
        
        // Racing Australia specific selectors for recent race results
        // Look for tables or divs that contain race result information
        val raceElements = doc.select("table[class*='form'], table[class*='result'], .form-table, .result-table, [class*='form-line'], [class*='race-result']")
        
        if (raceElements.isEmpty()) {
            println("⚠️ No race result tables found with specific selectors, trying broader search...")
            // Try looking for any table that might contain race results
            val allTables = doc.select("table")
            println("🔍 Found ${allTables.size} total tables")
            
            // Look for tables that contain race-like data
            val potentialRaceTables = allTables.filter { table ->
                val tableText = table.text()
                tableText.contains("Race", ignoreCase = true) ||
                tableText.contains("Finish", ignoreCase = true) ||
                tableText.contains("Position", ignoreCase = true) ||
                tableText.contains("Margin", ignoreCase = true) ||
                tableText.contains("Track", ignoreCase = true) ||
                tableText.contains("Distance", ignoreCase = true) ||
                tableText.contains("Date", ignoreCase = true)
            }
            
            println("🔍 Found ${potentialRaceTables.size} potential race result tables")
            
            // Use the first few potential race tables
            raceElements.addAll(potentialRaceTables.take(3))
        }
        
        println("🔍 Processing ${raceElements.size} race result elements")
        
        raceElements.take(5).forEachIndexed { index, raceElement ->
            try {
                println("🔍 Processing race result element $index:")
                println("   Tag: ${raceElement.tagName()}")
                println("   Class: ${raceElement.className()}")
                println("   Text preview: ${raceElement.text().take(200)}...")
                
                val race = parseRaceResultFromElement(raceElement)
                if (race != null) {
                    races.add(race)
                    println("✅ Successfully parsed race result $index")
                } else {
                    println("⚠️ Failed to parse race result $index")
                }
            } catch (e: Exception) {
                println("❌ Error parsing race result $index: ${e.message}")
            }
        }
        
        println("✅ Parsed ${races.size} recent races")
        return races
    }
    
    /**
     * Parse individual race result
     */
    private fun parseRaceResultFromElement(element: Element): RaceResultDetail? {
        try {
            println("🔍 Parsing race result element:")
            println("   Text content: ${element.text().take(300)}...")
            
            // Try multiple selectors for each field with text analysis fallbacks
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
            
            // Extract position (finishing position)
            val positionText = element.select(".position, .finish, [class*='position'], [class*='finish']").firstOrNull()?.text()?.trim()
            if (positionText != null) {
                position = positionText.toIntOrNull() ?: 0
            } else {
                // Try to find position in the text using regex
                val positionMatch = Regex("(\\d+)(?:st|nd|rd|th)?").find(element.text())
                if (positionMatch != null) {
                    position = positionMatch.groupValues[1].toIntOrNull() ?: 0
                }
            }
            
            if (position == 0) {
                println("⚠️ Could not extract position, skipping race result")
                return null
            }
            
            // Extract margin
            val marginText = element.select(".margin, .lengths, [class*='margin'], [class*='length']").firstOrNull()?.text()?.trim()
            margin = parseMargin(marginText)
            
            // Extract race class
            val raceClassText = element.select(".class, .race-class, [class*='class']").firstOrNull()?.text()?.trim()
            raceClass = raceClassText ?: ""
            
            // Extract track/venue - try multiple approaches
            var trackText = element.select(".track, .venue, [class*='track'], [class*='venue']").firstOrNull()?.text()?.trim()
            if (trackText.isNullOrEmpty()) {
                // Try to find track name in the text using regex patterns
                val allText = element.text()
                val trackMatch = Regex("([A-Za-z]{3,}(?:\\s+[A-Za-z]{3,})*?)\\s+\\d{1,4}m").find(allText)
                if (trackMatch != null) {
                    trackText = trackMatch.groupValues[1].trim()
                    println("✅ Found track from regex: $trackText")
                }
            }
            track = trackText ?: ""
            
            // Extract distance - try multiple approaches
            var distanceText = element.select(".distance, [class*='distance']").firstOrNull()?.text()?.trim()
            var extractedDistance = parseDistance(distanceText)
            if (extractedDistance == null || extractedDistance == 0) {
                // Try to find distance in the text using regex
                val allText = element.text()
                val distanceMatch = Regex("(\\d{1,4})\\s*m").find(allText)
                if (distanceMatch != null) {
                    extractedDistance = distanceMatch.groupValues[1].toIntOrNull()
                    if (extractedDistance != null && extractedDistance > 0) {
                        println("✅ Found distance from regex: ${extractedDistance}m")
                    }
                }
            }
            distance = extractedDistance ?: 0
            
            // Extract track condition - try multiple approaches
            var conditionText = element.select(".condition, .track-condition, [class*='condition']").firstOrNull()?.text()?.trim()
            if (conditionText.isNullOrEmpty()) {
                // Try to find condition in the text using regex
                val allText = element.text()
                val conditionMatch = Regex("(Good|Soft|Heavy|Firm|Synthetic|Wet|Slow|Dead)\\d?").find(allText)
                if (conditionMatch != null) {
                    conditionText = conditionMatch.groupValues[1].trim()
                    println("✅ Found condition from regex: $conditionText")
                }
            }
            trackCondition = conditionText ?: ""
            
            // Extract sectional time
            val sectionalText = element.select(".sectional, .600m, [class*='sectional']").firstOrNull()?.text()?.trim()
            sectionalTime = parseSectionalTime(sectionalText)
            
            // Extract date
            val dateText = element.select(".date, [class*='date']").firstOrNull()?.text()?.trim()
            date = parseDate(dateText)
            
            // Extract jockey
            val jockeyText = element.select(".jockey, .rider, [class*='jockey'], [class*='rider']").firstOrNull()?.text()?.trim()
            jockey = jockeyText ?: ""
            
            // Extract trainer
            val trainerText = element.select(".trainer, [class*='trainer']").firstOrNull()?.text()?.trim()
            trainer = trainerText ?: ""
            
            println("✅ Extracted race result: Position=$position, Track=$track, Distance=$distance, Condition=$trackCondition")
            
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
            // Look for up results section
            val upResultsElement = doc.select(".up-results, .spell-stats, [class*='up']").firstOrNull()
            
            if (upResultsElement != null) {
                val winsText = upResultsElement.select(".wins, .first-up").firstOrNull()?.text()?.trim()
                val wins = winsText?.toIntOrNull() ?: 0
                
                val placesText = upResultsElement.select(".places, .second-up, .third-up").firstOrNull()?.text()?.trim()
                val places = placesText?.toIntOrNull() ?: 0
                
                val runsText = upResultsElement.select(".runs, .total-up").firstOrNull()?.text()?.trim()
                val runs = runsText?.toIntOrNull() ?: 0
                
                return UpResult(wins, places, runs)
            }
        } catch (e: Exception) {
            println("❌ Error parsing up results: ${e.message}")
        }
        
        // Default values if parsing fails
        return UpResult(0, 0, 0)
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
} 
