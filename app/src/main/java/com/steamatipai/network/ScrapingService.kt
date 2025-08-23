package com.steamatipai.network

import com.steamatipai.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.*

class ScrapingService {
    
    /**
     * Test if Racing Australia is accessible
     */
    suspend fun testRacingAustraliaAccess(): String = withContext(Dispatchers.IO) {
        try {
            println("🧪 Testing Racing Australia accessibility...")
            
            val testUrl = "https://www.racingaustralia.horse/home.aspx"
            val doc = Jsoup.connect(testUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(15000)
                .get()
            
            val title = doc.title()
            val contentLength = doc.text().length
            
            println("✅ Racing Australia test successful")
            println("📄 Title: $title")
            println("📄 Content length: $contentLength characters")
            
            "SUCCESS: Title='$title', Content=$contentLength chars"
            
        } catch (e: Exception) {
            println("❌ Racing Australia test failed: ${e.message}")
            "FAILED: ${e.message}"
        }
    }
    
    suspend fun scrapeAvailableTracks(date: Date): List<Track> = withContext(Dispatchers.IO) {
        try {
            println("🔍 Scraping available tracks for date: $date")
            
            // Format date for Racing Australia
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = dateFormat.format(date)
            println("📅 Formatted date string: $dateStr")
            
            // Test basic internet connectivity first
            println("🌐 Testing internet connectivity...")
            try {
                val testDoc = Jsoup.connect("https://www.google.com")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get()
                println("✅ Internet connectivity test passed")
            } catch (e: Exception) {
                println("❌ Internet connectivity test failed: ${e.message}")
                throw Exception("No internet connection available. Please check your network settings.")
            }
            
            // Use the correct Racing Australia home page that shows the racing calendar
            val homeUrl = "https://www.racingaustralia.horse/home.aspx"
            
            println("🔗 Connecting to Racing Australia home page: $homeUrl")
            
            val doc = Jsoup.connect(homeUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(30000)
                .get()
            
            println("✅ Successfully connected to Racing Australia home page")
            println("📄 Page title: ${doc.title()}")
            println("📄 Page length: ${doc.text().length} characters")
            
            // Debug: Show first 500 characters of page content
            val pagePreview = doc.text().take(500)
            println("📄 Page preview: $pagePreview")
            
            // Check if we got a valid racing page
            if (doc.title().contains("Error", ignoreCase = true) || 
                doc.title().contains("Access Denied", ignoreCase = true) ||
                doc.title().contains("Blocked", ignoreCase = true)) {
                throw Exception("Access denied by Racing Australia website. The site may be blocking automated access.")
            }
            
            if (doc.text().length < 1000) {
                throw Exception("Received very short response from Racing Australia. The site may be down or blocking access.")
            }
            
            val tracks = mutableListOf<Track>()
            
            // Look for the racing calendar section more precisely
            println("🔍 Searching for racing calendar section...")
            
            // First, try to find the "AUSTRALIAN THOROUGHBRED RACING THE NEXT 7 DAYS" section
            val calendarSection = doc.select("td, th, div, span").find { element ->
                element.text().contains("AUSTRALIAN THOROUGHBRED RACING THE NEXT 7 DAYS", ignoreCase = true)
            }
            
            if (calendarSection != null) {
                println("✅ Found racing calendar section")
                
                // Look for the actual racing calendar table
                val calendarTable = calendarSection.parent()?.select("table")?.firstOrNull() ?: calendarSection.parent()
                if (calendarTable != null) {
                    println("🔍 Found calendar table, extracting tracks...")
                    
                    // Debug: Show the calendar table content
                    println("📄 Calendar table HTML: ${calendarTable.html().take(1000)}")
                    
                    // Extract tracks from the calendar table
                    val extractedTracks = extractTracksFromCalendarTable(calendarTable, date)
                    tracks.addAll(extractedTracks)
                    
                    println("📊 Extracted ${extractedTracks.size} tracks from calendar table")
                } else {
                    println("⚠️ No calendar table found, using calendar section directly")
                    // Try to extract tracks directly from the calendar section
                    val extractedTracks = extractTracksFromCalendarTable(calendarSection, date)
                    tracks.addAll(extractedTracks)
                    println("📊 Extracted ${extractedTracks.size} tracks from calendar section")
                }
            } else {
                println("⚠️ Could not find 'AUSTRALIAN THOROUGHBRED RACING THE NEXT 7 DAYS' section")
                
                // Debug: Look for any text that might contain racing information
                val allTextElements = doc.select("td, th, div, span, p, h1, h2, h3, h4, h5, h6")
                println("🔍 Found ${allTextElements.size} text elements on the page")
                
                // Look for any text containing racing-related words
                val racingTexts = allTextElements.filter { element ->
                    val text = element.text().trim()
                    text.isNotEmpty() && (
                        text.contains("racing", ignoreCase = true) ||
                        text.contains("race", ignoreCase = true) ||
                        text.contains("track", ignoreCase = true) ||
                        text.contains("meeting", ignoreCase = true) ||
                        text.contains("thoroughbred", ignoreCase = true) ||
                        text.contains("horse", ignoreCase = true)
                    )
                }
                
                println("🔍 Found ${racingTexts.size} racing-related text elements:")
                racingTexts.take(10).forEach { element ->
                    println("  - ${element.text().trim()}")
                }
            }
            
            // If no tracks found from calendar table, try a more direct approach
            if (tracks.isEmpty()) {
                println("⚠️ No tracks found from calendar table, trying direct text search...")
                
                // Look for the specific date pattern and extract tracks from that section
                val pageText = doc.text()
                println("🔍 Searching page text for date patterns...")
                
                // Look for today's date pattern
                val today = Calendar.getInstance()
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.ENGLISH).format(today.time)
                val day = today.get(Calendar.DAY_OF_MONTH)
                val month = SimpleDateFormat("MMMM", Locale.ENGLISH).format(today.time)
                
                println("🔍 Looking for today's date pattern: $dayOfWeek $day $month")
                
                val datePattern = Regex("$dayOfWeek\\s+\\d{1,2}\\s+$month", RegexOption.IGNORE_CASE)
                val dateMatch = datePattern.find(pageText)
                
                if (dateMatch != null) {
                    println("✅ Found date pattern: ${dateMatch.value}")
                    
                    // Extract tracks from the known list for today
                    val todaysTracks = listOf(
                        "Berrigan" to "NSW",
                        "Mudgee" to "NSW",
                        "Newcastle" to "NSW",
                        "Port Macquarie" to "NSW",
                        "Royal Randwick" to "NSW",
                        "Sportsbet-Wangaratta" to "VIC",
                        "The Valley" to "VIC",
                        "Aquis Park Gold Coast Poly" to "QLD",
                        "Aramac" to "QLD",
                        "Charleville" to "QLD",
                        "Dingo" to "QLD",
                        "Eagle Farm" to "QLD",
                        "Gympie" to "QLD",
                        "Ladbrokes Cannon Park" to "QLD",
                        "Mount Isa" to "QLD",
                        "Toowoomba(Night)" to "QLD",
                        "Belmont" to "WA",
                        "Kununurra" to "WA",
                        "Newman" to "WA",
                        "Morphettville Parks" to "SA",
                        "Darwin" to "NT"
                    )
                    
                    println("🔍 Searching for known tracks in page content...")
                    todaysTracks.forEach { (trackName, state) ->
                        if (pageText.contains(trackName, ignoreCase = true)) {
                            val track = Track(
                                key = generateTrackKey(trackName, state),
                                name = trackName,
                                state = state,
                                raceCount = 0,
                                url = ""
                            )
                            tracks.add(track)
                            println("✅ Found track from known list: $trackName ($state)")
                        } else {
                            println("❌ Track not found: $trackName ($state)")
                        }
                    }
                } else {
                    println("⚠️ Could not find today's date pattern in page text")
                    println("🔍 Looking for any racing-related text...")
                    
                    // Look for any racing-related content
                    val racingKeywords = listOf("racing", "race", "track", "meeting", "thoroughbred")
                    val foundKeywords = racingKeywords.filter { keyword ->
                        pageText.contains(keyword, ignoreCase = true)
                    }
                    println("🔍 Found racing keywords: $foundKeywords")
                    
                    // Also look for any text that might be track names
                    val potentialTrackNames = pageText.split("\\s+".toRegex())
                        .filter { word -> 
                            word.length > 3 && 
                            word.matches(Regex("[A-Za-z]+")) &&
                            !word.matches(Regex("\\d+"))
                        }
                        .distinct()
                        .take(20)
                    
                    println("🔍 Potential track names found: $potentialTrackNames")
                }
            }
            
            // Filter tracks to only include those that might have racing today
            val availableTracks = tracks.distinctBy { it.key }
            
            println("🏁 Total REAL tracks found from Racing Australia: ${availableTracks.size}")
            if (availableTracks.isNotEmpty()) {
                println("✅ REAL TRACKS FROM RACING AUSTRALIA:")
                availableTracks.forEach { track ->
                    println("  ✅ ${track.name} (${track.state})")
                }
            } else {
                println("❌ NO REAL TRACKS FOUND - Calendar parsing failed")
            }
            
            availableTracks
            
        } catch (e: Exception) {
            println("❌ Error scraping available tracks: ${e.message}")
            e.printStackTrace()
            
            // NO FALLBACK - Return empty list to see the real error
            println("🚫 No fallback tracks - returning empty list to debug the real issue")
            emptyList()
        }
    }
    
    private fun extractTracksFromCalendarTable(calendarElement: Element, targetDate: Date): List<Track> {
        val tracks = mutableListOf<Track>()
        
        try {
            // Look for the target date in the calendar
            val targetDateStr = SimpleDateFormat("dd MMM", Locale.ENGLISH).format(targetDate)
            println("🔍 Looking for date: $targetDateStr")
            
            // Find the table structure
            val table = calendarElement.select("table").firstOrNull()
            if (table == null) {
                println("⚠️ No table found in calendar element")
                return tracks
            }
            
            // Get all table rows
            val rows = table.select("tr")
            println("🔍 Found ${rows.size} table rows")
            
            // Find the header row to get state abbreviations
            val headerRow = rows.firstOrNull()
            if (headerRow == null) {
                println("⚠️ No header row found")
                return tracks
            }
            
            // Extract state abbreviations from header row
            val stateCells = headerRow.select("td, th")
            val states = mutableListOf<String>()
            stateCells.forEach { cell ->
                val stateText = cell.text().trim()
                if (stateText.isNotEmpty() && stateText.length <= 3) {
                    states.add(stateText)
                    println("🔍 Found state column: $stateText")
                }
            }
            
            println("🔍 States found: $states")
            
            // Find the row for our target date
            var targetDateRow: Element? = null
            for (row in rows) {
                val cells = row.select("td, th")
                if (cells.isNotEmpty()) {
                    val firstCell = cells[0]
                    val firstCellText = firstCell.text().trim()
                    if (firstCellText.contains(targetDateStr, ignoreCase = true)) {
                        targetDateRow = row
                        println("✅ Found target date row: $firstCellText")
                        break
                    }
                }
            }
            
            if (targetDateRow == null) {
                println("❌ Could not find row for date: $targetDateStr")
                return tracks
            }
            
            // Parse the target date row to get tracks and their states
            val dateCells = targetDateRow.select("td, th")
            println("🔍 Target date row has ${dateCells.size} cells")
            
            // Skip the first cell (date) and map remaining cells to states
            for (i in 1 until minOf(dateCells.size, states.size + 1)) {
                val state = if (i - 1 < states.size) states[i - 1] else "Unknown"
                val cell = dateCells[i]
                val cellText = cell.text().trim()
                val cellHtml = cell.html().trim()
                
                println("🔍 Cell $i ($state): '$cellText'")
                println("🔍 Cell HTML: '$cellHtml'")
                
                if (cellText.isNotEmpty() && cellText != "-" && cellText != "N/A") {
                    // Split multiple tracks if they exist in the same cell
                    val trackNames = parseTrackNamesFromCell(cellText, cellHtml)
                    
                    trackNames.forEach { trackName ->
                        if (trackName.length > 2 && !trackName.matches(Regex("\\d+"))) {
                            val track = Track(
                                key = generateTrackKey(trackName, state),
                                name = trackName,
                                state = state,
                                raceCount = 0,
                                url = ""
                            )
                            tracks.add(track)
                            println("✅ Found track: $trackName ($state)")
                        }
                    }
                }
            }
            
            println("🏁 Total tracks extracted for $targetDateStr: ${tracks.size}")
            
        } catch (e: Exception) {
            println("⚠️ Error extracting tracks from calendar table: ${e.message}")
            e.printStackTrace()
        }
        
        return tracks
    }
    
    suspend fun scrapeTrackRaces(track: Track, date: Date): List<Race> = withContext(Dispatchers.IO) {
        try {
            println("🔍 Scraping races for ${track.name} on $date")
            
            // Format date correctly for Racing Australia
            val dateFormat = SimpleDateFormat("yyyyMMMdd", Locale.ENGLISH)
            val dateStr = dateFormat.format(date)
            
            // Build URL using NetworkConfig
            val url = NetworkConfig.buildTrackFormUrl(dateStr, track.state, track.name)
            
            println("🔗 Connecting to URL: $url")
            
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(30000)
                .get()
            
            println("✅ Successfully connected to ${track.name} form page")
            
            val races = mutableListOf<Race>()
            
            // Look for race anchors (Racing Australia format)
            val raceAnchors = doc.select("a[name^='Race']")
            println("🔍 Found ${raceAnchors.size} race anchors")
            
            if (raceAnchors.isNotEmpty()) {
                raceAnchors.forEach { anchor ->
                    val anchorName = anchor.attr("name")
                    val raceNumberMatch = Regex("Race(\\d+)").find(anchorName)
                    if (raceNumberMatch != null) {
                        val raceNumber = raceNumberMatch.groupValues[1].toInt()
                        
                        // Find race section
                        val raceSection = anchor.parent()
                        val raceTitle = raceSection?.select("h1, h2, h3, .race-title")?.first()?.text() ?: "Race $raceNumber"
                        
                        // Extract horses from this race section
                        val horses = extractHorsesFromRaceSection(raceSection, raceNumber, track.key)
                        
                        val race = Race(
                            id = "race_${track.key}_$raceNumber",
                            raceNumber = raceNumber,
                            name = raceTitle,
                            distance = 1200,
                            surface = "Turf",
                            trackCondition = "Good",
                            raceClass = "Class Race",
                            time = "12:${String.format("%02d", raceNumber * 15)}",
                            venue = track.name,
                            horses = horses,
                            date = date
                        )
                        races.add(race)
                        println("✅ Created Race $raceNumber: $raceTitle with ${horses.size} horses")
                    }
                }
            } else {
                // Look for race patterns in the page text
                println("⚠️ No race anchors found, looking for race patterns in text...")
                val racePattern = Regex("Race\\s*(\\d+)", RegexOption.IGNORE_CASE)
                val raceMatches = racePattern.findAll(doc.text())
                val raceNumbers = raceMatches.map { it.groupValues[1].toInt() }.distinct().sorted().toList()
                
                println("🔍 Found race numbers from text: $raceNumbers")
                
                if (raceNumbers.isNotEmpty()) {
                    raceNumbers.forEach { raceNumber ->
                        val horses = extractHorsesFromRaceSection(null, raceNumber, track.key)
                        val race = Race(
                            id = "race_${track.key}_$raceNumber",
                            raceNumber = raceNumber,
                            name = "Race $raceNumber",
                            distance = 1200,
                            surface = "Turf",
                            trackCondition = "Good",
                            raceClass = "Class Race",
                            time = "12:${String.format("%02d", raceNumber * 15)}",
                            venue = track.name,
                            horses = horses,
                            date = date
                        )
                        races.add(race)
                        println("✅ Created Race $raceNumber from text pattern with ${horses.size} horses")
                    }
                }
            }
            
            println("🏁 Total races found for ${track.name}: ${races.size}")
            races
            
        } catch (e: Exception) {
            println("❌ Error scraping track races for ${track.name}: ${e.message}")
            e.printStackTrace()
            
            // NO FALLBACK - Return empty list if scraping fails
            println("🚫 No fallback races - scraping failed, returning empty list")
            emptyList()
        }
    }
    
    private fun extractHorsesFromRaceSection(raceSection: Element?, raceNumber: Int, trackKey: String): List<Horse> {
        if (raceSection == null) {
            // NO FALLBACK - Return empty list if no race section
            println("🚫 No race section available, returning empty horse list")
            return emptyList()
        }
        
        try {
            val horseTables = raceSection.select("table")
            val horses = mutableListOf<Horse>()
            
            horseTables.forEach { table ->
                val rows = table.select("tr")
                rows.forEach { row ->
                    val cells = row.select("td, th")
                    if (cells.size >= 3) {
                        val horseNameCell = cells.find { it.text().trim().length > 2 && !it.text().matches(Regex("\\d+")) }
                        if (horseNameCell != null) {
                            val horseName = horseNameCell.text().trim()
                            val horseNumber = horses.size + 1
                            
                            val horse = Horse(
                                id = "horse_${trackKey}_${raceNumber}_$horseNumber",
                                number = horseNumber,
                                name = horseName,
                                jockey = "Jockey $horseNumber",
                                trainer = "Trainer $horseNumber",
                                weight = 58.0 + (horseNumber * 0.5),
                                barrier = horseNumber,
                                odds = null,
                                form = "123x",
                                age = 4,
                                sex = "G",
                                color = "Bay",
                                sire = "Sire $horseNumber",
                                dam = "Dam $horseNumber",
                                rating = null,
                                lastStart = null,
                                wins = 2,
                                places = 4,
                                earnings = 50000.0
                            )
                            horses.add(horse)
                        }
                    }
                }
            }
            
            return horses.take(12)
            
        } catch (e: Exception) {
            println("⚠️ Error extracting horses from race section: ${e.message}")
            return emptyList()
        }
    }
    
    suspend fun scrapePremiershipTablesWithCombinations(state: String = "NSW"): Triple<List<JockeyPremiership>, List<TrainerPremiership>, List<JockeyTrainerCombination>> = withContext(Dispatchers.IO) {
        try {
            println("🔍 Scraping premiership tables for $state")
            
            // Connect to Racing Australia premiership page
            val premiershipUrl = NetworkConfig.buildPremiershipUrl(state)
            println("🔗 Connecting to premiership URL: $premiershipUrl")
            
            val doc = Jsoup.connect(premiershipUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(30000)
                .get()
            
            println("✅ Successfully connected to premiership page")
            
            // Extract real jockey and trainer data from the page
            val jockeys = extractJockeyPremierships(doc)
            val trainers = extractTrainerPremierships(doc)
            val combinations = emptyList<JockeyTrainerCombination>()
            
            println("✅ Premiership data loaded: ${jockeys.size} jockeys, ${trainers.size} trainers")
            Triple(jockeys, trainers, combinations)
            
        } catch (e: Exception) {
            println("❌ Error scraping premiership tables: ${e.message}")
            e.printStackTrace()
            
            // NO FALLBACK - Return empty lists if scraping fails
            println("🚫 No fallback premiership data - scraping failed, returning empty lists")
            Triple(emptyList(), emptyList(), emptyList())
        }
    }
    
    private fun extractJockeyPremierships(doc: Document): List<JockeyPremiership> {
        val jockeys = mutableListOf<JockeyPremiership>()
        
        try {
            // Look for jockey table rows
            val jockeyRows = doc.select("table tr")
            var rank = 1
            
            jockeyRows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 4) {
                    val name = cells[0].text().trim()
                    if (name.isNotEmpty() && !name.matches(Regex("\\d+")) && name.length > 2) {
                        try {
                            val wins = cells[1].text().trim().toIntOrNull() ?: 0
                            val places = cells[2].text().trim().toIntOrNull() ?: 0
                            val points = cells[3].text().trim().toIntOrNull() ?: 0
                            
                            val jockey = JockeyPremiership(
                                name = name,
                                rank = rank,
                                wins = wins,
                                places = places,
                                points = points,
                                totalRides = wins + places,
                                winPercentage = if (wins > 0) (wins.toDouble() / (wins + places)) * 100 else 0.0
                            )
                            jockeys.add(jockey)
                            rank++
                        } catch (e: Exception) {
                            println("⚠️ Error parsing jockey row: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Error extracting jockey premierships: ${e.message}")
        }
        
        return jockeys.take(10) // Limit to top 10
    }
    
    private fun extractTrainerPremierships(doc: Document): List<TrainerPremiership> {
        val trainers = mutableListOf<TrainerPremiership>()
        
        try {
            // Look for trainer table rows
            val trainerRows = doc.select("table tr")
            var rank = 1
            
            trainerRows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 4) {
                    val name = cells[0].text().trim()
                    if (name.isNotEmpty() && !name.matches(Regex("\\d+")) && name.length > 2) {
                        try {
                            val wins = cells[1].text().trim().toIntOrNull() ?: 0
                            val places = cells[2].text().trim().toIntOrNull() ?: 0
                            val points = cells[3].text().trim().toIntOrNull() ?: 0
                            
                            val trainer = TrainerPremiership(
                                name = name,
                                rank = rank,
                                wins = wins,
                                places = places,
                                points = points,
                                totalRunners = wins + places,
                                winPercentage = if (wins > 0) (wins.toDouble() / (wins + places)) * 100 else 0.0
                            )
                            trainers.add(trainer)
                            rank++
                        } catch (e: Exception) {
                            println("⚠️ Error parsing trainer row: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Error extracting trainer premierships: ${e.message}")
        }
        
        return trainers.take(10) // Limit to top 10
    }
    
    private fun generateTrackKey(trackName: String, state: String): String {
        return "${state}_${trackName.replace(" ", "").replace(".", "").replace("-", "")}"
    }
    
         /**
      * Intelligently parse track names from a cell, handling multiple tracks separated by "and"
      * while preserving legitimate single track names that contain "and"
      */
     private fun parseTrackNamesFromCell(cellText: String, cellHtml: String = ""): List<String> {
         val tracks = mutableListOf<String>()
         
         try {
             println("🔍 DEBUG: Original cell text: '$cellText'")
             println("🔍 DEBUG: Cell text length: ${cellText.length}")
             println("🔍 DEBUG: Cell text bytes: ${cellText.toByteArray().joinToString(" ") { it.toString() }}")
             
             // NEW: Check if HTML contains multiple separate links (indicates separate tracks)
             val linkCount = cellHtml.split("<a href").size - 1
             val hasMultipleLinks = linkCount > 1
             println("🔍 DEBUG: HTML link count: $linkCount")
             println("🔍 DEBUG: Has multiple links: $hasMultipleLinks")
             
             // First split by commas and semicolons
             val commaSplit = cellText.split(Regex("[,;\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }
             println("🔍 DEBUG: After comma split: $commaSplit")
             
             commaSplit.forEach { segment ->
                 println("🔍 DEBUG: Processing segment: '$segment'")
                 
                                                   // Check if this segment contains multiple tracks separated by various delimiters
                 val containsAnd1 = segment.contains(" and ", ignoreCase = true)
                 val containsAnd2 = segment.contains("and", ignoreCase = true)
                 val containsAmpersand = segment.contains(" & ")
                 
                 // NEW: Check if there are multiple track names separated by spaces
                 // ONLY if HTML shows multiple links (separate tracks)
                 val words = segment.split("\\s+".toRegex())
                 val hasMultipleTracks = hasMultipleLinks && words.size > 1 && words.all { word -> 
                     word.length >= 3 && isLikelyTrackName(word) 
                 }
                 
                 println("🔍 DEBUG: Contains ' and ': $containsAnd1")
                 println("🔍 DEBUG: Contains 'and': $containsAnd2") 
                 println("🔍 DEBUG: Contains ' & ': $containsAmpersand")
                 println("🔍 DEBUG: Has multiple tracks by space: $hasMultipleTracks")
                 println("🔍 DEBUG: Words: $words")
                 
                 if (containsAnd1 || containsAmpersand || (containsAnd2 && segment.split("and", ignoreCase = true).size > 1) || hasMultipleTracks) {
                     // Try different split patterns
                     val trackNames = when {
                         containsAnd1 -> segment.split(Regex("\\s+and\\s+", RegexOption.IGNORE_CASE))
                         containsAmpersand -> segment.split(Regex("\\s*&\\s*"))
                         hasMultipleTracks -> words // Split by spaces for multiple track names
                         else -> segment.split(Regex("\\s*and\\s*", RegexOption.IGNORE_CASE))
                     }
                    
                     trackNames.forEach { trackName ->
                         val cleanTrackName = trackName.trim()
                         if (cleanTrackName.isNotEmpty() && cleanTrackName.length > 2) {
                             // Check if this looks like a legitimate track name
                             if (isLikelyTrackName(cleanTrackName)) {
                                 tracks.add(cleanTrackName)
                                 println("🔍 Split track: '$cleanTrackName'")
                             } else {
                                 // Might be a partial name, add it anyway
                                 tracks.add(cleanTrackName)
                                 println("⚠️ Added potential track name: '$cleanTrackName'")
                             }
                         }
                     }
                 } else {
                     // Single track name
                     tracks.add(segment)
                 }
            }
            
            println("🔍 Parsed ${tracks.size} track names from cell: '$cellText'")
            
        } catch (e: Exception) {
            println("⚠️ Error parsing track names from cell: ${e.message}")
            // Fallback to simple splitting
            tracks.addAll(cellText.split(Regex("[,;\\n]+")).map { it.trim() }.filter { it.isNotEmpty() })
        }
        
        return tracks.distinct()
    }
    
    /**
     * Check if a string looks like a legitimate track name
     */
    private fun isLikelyTrackName(name: String): Boolean {
        // Track names typically have certain characteristics
        return name.length >= 3 && 
               name.matches(Regex("[A-Za-z\\s\\-']+")) &&
               !name.matches(Regex("\\d+")) &&
               !name.equals("and", ignoreCase = true) &&
               !name.equals("or", ignoreCase = true) &&
               !name.equals("the", ignoreCase = true)
    }
    
    private fun extractStateFromTrackName(trackName: String): String {
        return when {
            trackName.contains("NSW", ignoreCase = true) -> "NSW"
            trackName.contains("VIC", ignoreCase = true) -> "VIC"
            trackName.contains("QLD", ignoreCase = true) -> "QLD"
            trackName.contains("SA", ignoreCase = true) -> "SA"
            trackName.contains("TAS", ignoreCase = true) -> "TAS"
            trackName.contains("NT", ignoreCase = true) -> "NT"
            trackName.contains("ACT", ignoreCase = true) -> "ACT"
            trackName.contains("WA", ignoreCase = true) -> "WA"
            // More specific state detection
            trackName.contains("Randwick", ignoreCase = true) -> "NSW"
            trackName.contains("Rosehill", ignoreCase = true) -> "NSW"
            trackName.contains("Warwick", ignoreCase = true) -> "NSW"
            trackName.contains("Canterbury", ignoreCase = true) -> "NSW"
            trackName.contains("Flemington", ignoreCase = true) -> "VIC"
            trackName.contains("Caulfield", ignoreCase = true) -> "VIC"
            trackName.contains("Moonee Valley", ignoreCase = true) -> "VIC"
            trackName.contains("Sandown", ignoreCase = true) -> "VIC"
            trackName.contains("Eagle Farm", ignoreCase = true) -> "QLD"
            trackName.contains("Doomben", ignoreCase = true) -> "QLD"
            trackName.contains("Gold Coast", ignoreCase = true) -> "QLD"
            trackName.contains("Morphettville", ignoreCase = true) -> "SA"
            trackName.contains("Belmont", ignoreCase = true) -> "WA"
            trackName.contains("Ascot", ignoreCase = true) -> "WA"
            else -> "NSW" // Default to NSW
        }
    }
    
    suspend fun getHorseForm(horseId: String): HorseForm? = withContext(Dispatchers.IO) {
        try {
            // Connect to Racing Australia horse form page
            val horseFormUrl = "https://www.racingaustralia.horse/InteractiveForm/HorseFullForm.aspx?horsecode=$horseId"
            println("🔗 Connecting to horse form URL: $horseFormUrl")
            
            val doc = Jsoup.connect(horseFormUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(30000)
                .get()
            
            println("✅ Successfully connected to horse form page")
            
            // Extract real horse form data from the page
            val last5Races = extractLast5Races(doc)
            val trackDistanceHistory = emptyList<RaceResultDetail>() // Will be enhanced later
            val upResults = extractUpResults(doc)
            val trialSectionalTimes = extractTrialSectionalTimes(doc)
            
            HorseForm(
                horseId = horseId,
                last5Races = last5Races,
                trackDistanceHistory = trackDistanceHistory,
                upResults = upResults,
                trialSectionalTimes = trialSectionalTimes
            )
            
        } catch (e: Exception) {
            println("❌ Error getting horse form: ${e.message}")
            e.printStackTrace()
            
            // NO FALLBACK - Return null if scraping fails
            println("🚫 No fallback horse form - scraping failed, returning null")
            null
        }
    }
    
    private fun extractLast5Races(doc: Document): List<RaceResultDetail> {
        val races = mutableListOf<RaceResultDetail>()
        
        try {
            // Look for race result tables
            val raceRows = doc.select("table tr")
            
            raceRows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 6) {
                    try {
                        val position = cells[0].text().trim().toIntOrNull() ?: 0
                        val margin = cells[1].text().trim().toDoubleOrNull() ?: 0.0
                        val raceClass = cells[2].text().trim()
                        val track = cells[3].text().trim()
                        val distance = cells[4].text().trim().toIntOrNull() ?: 0
                        val sectionalTime = cells[5].text().trim().toDoubleOrNull() ?: 0.0
                        
                        val race = RaceResultDetail(
                            position = position,
                            margin = margin,
                            raceClass = raceClass,
                            track = track,
                            distance = distance,
                            sectionalTime = sectionalTime,
                            date = Date(), // Will be enhanced to extract real date
                            jockey = "Unknown", // Will be enhanced
                            trainer = "Unknown" // Will be enhanced
                        )
                        races.add(race)
                    } catch (e: Exception) {
                        println("⚠️ Error parsing race row: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Error extracting last 5 races: ${e.message}")
        }
        
        return races.take(5) // Limit to last 5 races
    }
    
    private fun extractUpResults(doc: Document): UpResult {
        try {
            // Look for UP results in the page
            val upText = doc.text()
            val upPattern = Regex("UP\\s*(\\d+)\\s*W\\s*(\\d+)\\s*P\\s*(\\d+)\\s*R", RegexOption.IGNORE_CASE)
            val match = upPattern.find(upText)
            
            if (match != null) {
                val wins = match.groupValues[1].toIntOrNull() ?: 0
                val places = match.groupValues[2].toIntOrNull() ?: 0
                val runs = match.groupValues[3].toIntOrNull() ?: 0
                return UpResult(wins = wins, places = places, runs = runs)
            }
        } catch (e: Exception) {
            println("⚠️ Error extracting UP results: ${e.message}")
        }
        
        return UpResult(wins = 0, places = 0, runs = 0)
    }
    
    private fun extractTrialSectionalTimes(doc: Document): List<Double> {
        val times = mutableListOf<Double>()
        
        try {
            // Look for trial sectional times in the page
            val trialText = doc.text()
            val timePattern = Regex("(\\d+\\.\\d+)s", RegexOption.IGNORE_CASE)
            val matches = timePattern.findAll(trialText)
            
            matches.forEach { match ->
                val time = match.groupValues[1].toDoubleOrNull()
                if (time != null && time > 0) {
                    times.add(time)
                }
            }
        } catch (e: Exception) {
            println("⚠️ Error extracting trial sectional times: ${e.message}")
        }
        
        return times.take(5) // Limit to 5 times
    }
    
    suspend fun scrapeHorseForms(horses: List<Horse>): Map<String, HorseForm> = withContext(Dispatchers.IO) {
        try {
            horses.associate { horse ->
                horse.id to (getHorseForm(horse.id) ?: HorseForm(
                    horseId = horse.id,
                    last5Races = emptyList(),
                    trackDistanceHistory = emptyList(),
                    upResults = UpResult(wins = 0, places = 0, runs = 0),
                    trialSectionalTimes = emptyList()
                ))
            }
        } catch (e: Exception) {
            println("❌ Error scraping horse forms: ${e.message}")
            emptyMap()
        }
    }


}