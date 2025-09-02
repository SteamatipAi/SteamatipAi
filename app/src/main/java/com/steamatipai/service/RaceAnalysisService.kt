package com.steamatipai.service

import com.steamatipai.data.models.*
import com.steamatipai.network.ScrapingService
import com.steamatipai.scoring.ScoringEngine
import kotlinx.coroutines.*
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale

class RaceAnalysisService(
    private val scrapingService: ScrapingService = ScrapingService(),
    private val scoringEngine: ScoringEngine = ScoringEngine()
) {
    
    /**
     * Main analysis function that processes selected tracks for a given date
     */
    suspend fun analyzeRaces(
        tracks: List<Track>,
        date: Date,
        includeDetailedForm: Boolean = true
    ): AnalysisResult {
        println("🚀 STARTING RACE ANALYSIS")
        println("📅 Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)}")
        println("🏁 Tracks to analyze: ${tracks.size}")
        tracks.forEach { track ->
            println("   - ${track.name} (${track.state})")
        }
        
        val startTime = System.currentTimeMillis()
        val allResults = mutableListOf<RaceResult>()
        
        try {
            // Analyze each track
            tracks.forEach { track ->
                println("\n🏁 ANALYZING TRACK: ${track.name} (${track.state})")
                
                // Fetch premiership data for this track's state
                val jockeyRankings = scrapingService.fetchJockeyPremiership(track.state)
                val trainerRankings = scrapingService.fetchTrainerPremiership(track.state)
                
                println("👥 Jockey rankings for ${track.state}: ${jockeyRankings.size}")
                println("👨‍🏫 Trainer rankings for ${track.state}: ${trainerRankings.size}")
                
                // Show top 5 jockeys and trainers for debugging
                jockeyRankings.take(5).forEachIndexed { index, jockey ->
                    println("   Jockey ${index + 1}: ${jockey.name} (${jockey.wins} wins)")
                }
                trainerRankings.take(5).forEachIndexed { index, trainer ->
                    println("   Trainer ${index + 1}: ${trainer.name} (${trainer.wins} wins)")
                }
                
                try {
                    val races = scrapingService.scrapeTrackRaces(track, date)
                    println("📊 Found ${races.size} races for ${track.name}")
                    
                    races.forEach { race ->
                        println("   Race ${race.raceNumber}: ${race.name} (${race.horses.size} horses) - ID: ${race.id}")
                    }
                    
                    // Analyze each race
                    races.forEach { race ->
                        println("\n🔍 Starting analysis of Race ${race.raceNumber} (ID: ${race.id})")
                        val raceResult = analyzeRace(race, track, jockeyRankings, trainerRankings, includeDetailedForm)
                        allResults.add(raceResult)
                        
                        println("✅ Race ${race.raceNumber} analysis complete")
                        println("   Top 5 selections:")
                        raceResult.topSelections.take(5).forEachIndexed { index, scoredHorse ->
                            println("     ${index + 1}. ${scoredHorse.horse.name}: ${String.format("%.1f", scoredHorse.score)} points")
                        }
                    }
                    
                } catch (e: Exception) {
                    println("❌ Error analyzing track ${track.name}: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            println("\n🏁 ANALYSIS COMPLETE")
            println("⏱️ Total processing time: ${processingTime}ms")
            println("📊 Total races analyzed: ${allResults.size}")
            
            // Show summary of all results
            allResults.forEachIndexed { index, raceResult ->
                println("\n🏆 RACE ${index + 1} SUMMARY:")
                println("   Race ID: ${raceResult.race.id}")
                println("   Track: ${raceResult.race.venue}")
                println("   Race: ${raceResult.race.name}")
                println("   Horses analyzed: ${raceResult.topSelections.size}")
                println("   Top horse: ${raceResult.topSelections.firstOrNull()?.horse?.name ?: "None"} - ${String.format("%.1f", raceResult.topSelections.firstOrNull()?.score ?: 0.0)} points")
            }
            
            return AnalysisResult(
                tracks = tracks,
                date = date,
                results = allResults,
                processingTime = processingTime
            )
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            println("❌ ANALYSIS FAILED: ${e.message}")
            e.printStackTrace()
            
            return AnalysisResult(
                tracks = tracks,
                date = date,
                results = emptyList(),
                processingTime = processingTime,
                error = e.message ?: "Analysis failed"
            )
        }
    }
    
    /**
     * Analyze a single track
     */
    private suspend fun analyzeTrack(
        track: Track,
        date: Date,
        jockeyRankings: List<JockeyPremiership>,
        trainerRankings: List<TrainerPremiership>,
        includeDetailedForm: Boolean
    ): TrackResult {
        
        // Scrape race details for this track
        val races = scrapingService.scrapeTrackRaces(track, date)
        
        if (races.isEmpty()) {
            return TrackResult(
                trackName = track.name,
                trackKey = track.key,
                races = emptyList(),
                error = "No races found for this track"
            )
        }
        
        val raceResults = mutableListOf<RaceResult>()
        
        // Process each race in parallel
        val raceResultsList = coroutineScope {
            val raceJobs = races.map { race ->
                async {
                    try {
                        analyzeRace(race, track, jockeyRankings, trainerRankings, includeDetailedForm)
                    } catch (e: Exception) {
                        // Return error result for this race
                        RaceResult(
                            race = race,
                            topSelections = emptyList(),
                            processingTime = 0,
                            error = e.message ?: "Race analysis failed"
                        )
                    }
                }
            }
            
            // Wait for all races to complete analysis
            raceJobs.awaitAll()
        }
        
        raceResults.addAll(raceResultsList)
        
        return TrackResult(
            trackName = track.name,
            trackKey = track.key,
            races = raceResults,
            error = null
        )
    }
    
    /**
     * Analyze a single race
     */
    private suspend fun analyzeRace(
        race: Race,
        track: Track,
        jockeyRankings: List<JockeyPremiership>,
        trainerRankings: List<TrainerPremiership>,
        includeDetailedForm: Boolean
    ): RaceResult {
        println("🏁 ANALYZING RACE ${race.raceNumber}: ${race.name}")
        println("📊 Race details: ${race.distance}m, ${race.surface}, ${race.trackCondition}, ${race.raceClass}")
        println("🐎 Horses in race: ${race.horses.size}")
        
        val scoredHorses = mutableListOf<ScoredHorse>()
        
        // Process ALL horses in the race - but only score horses with real form data
        race.horses.forEachIndexed { index, horse ->
            try {
                println("🔍 Processing horse ${index + 1}/${race.horses.size}: ${horse.name}")
                println("   Horse details: No. ${horse.number}, Barrier ${horse.barrier}, Weight ${horse.weight}kg")
                println("   Jockey: ${horse.jockey}, Trainer: ${horse.trainer}")
                println("   Form: ${horse.form}")

                // Get detailed form data for this horse if requested
                if (includeDetailedForm) {
                    val horseForm = getRealHorseForm(horse, race, track)
                    if (horseForm != null) {
                        println("📊 Using real form data for ${horse.name}")
                        println("   Last 5 races: ${horseForm.last5Races.size}")
                        println("   Up results: ${horseForm.upResults.wins}W-${horseForm.upResults.places}P-${horseForm.upResults.runs}R")
                        
                        // Score the horse using the scoring engine - ONLY horses with real form data
                        val scoredHorse = scoringEngine.calculateTotalScore(
                            horse = horse,
                            race = race,
                            horseForm = horseForm,
                            jockeyRankings = jockeyRankings,
                            trainerRankings = trainerRankings
                        )
                        
                        if (scoredHorse != null) {
                            println("🏆 Horse ${horse.name} scored: ${String.format("%.1f", scoredHorse.score)} points")
                            println("   Score breakdown:")
                            println("     Recent Form: ${String.format("%.1f", scoredHorse.scoreBreakdown.recentForm)}")
                            println("     Class Suitability: ${String.format("%.1f", scoredHorse.scoreBreakdown.classSuitability)}")
                            println("     Track Distance: ${String.format("%.1f", scoredHorse.scoreBreakdown.trackDistance)}")
                            println("     Sectional Time: ${String.format("%.1f", scoredHorse.scoreBreakdown.sectionalTime)}")
                            println("     Barrier: ${String.format("%.1f", scoredHorse.scoreBreakdown.barrier)}")
                            println("     Jockey: ${String.format("%.1f", scoredHorse.scoreBreakdown.jockey)}")
                            println("     Trainer: ${String.format("%.1f", scoredHorse.scoreBreakdown.trainer)}")
                            println("     Jockey-Horse & Jockey-Trainer Combination: ${String.format("%.1f", scoredHorse.scoreBreakdown.combination)}")
                            println("     Track Condition: ${String.format("%.1f", scoredHorse.scoreBreakdown.trackCondition)}")
                            
                            scoredHorses.add(scoredHorse)
                        } else {
                            println("❌ Horse ${horse.name} could not be scored - excluding from results")
                        }
                    } else {
                        println("❌ No real form data available for ${horse.name} - EXCLUDING from scoring")
                        // Horse is excluded - no artificial data, no scoring
                    }
                } else {
                    println("⚠️ Detailed form not requested - EXCLUDING ${horse.name} from scoring")
                    // Horse is excluded when detailed form is not requested
                }

            } catch (e: Exception) {
                println("❌ Error processing horse ${horse.name}: ${e.message}")
                e.printStackTrace()
                // Horse is excluded due to error - no artificial data, no scoring
            }
        }
        
        // Check if we have any horses with real form data
        if (scoredHorses.isEmpty()) {
            println("❌ No horses with real form data found for Race ${race.raceNumber}")
            return RaceResult(
                race = race,
                topSelections = emptyList(),
                allHorses = emptyList(),
                error = "No horses with real form data found - all horses excluded"
            )
        }
        
        println("✅ Successfully scored ${scoredHorses.size} horses with real form data")
        
        // Sort horses by score (highest first)
        scoredHorses.sortByDescending { it.score }
        
        println("🏁 RACE ${race.raceNumber} ANALYSIS COMPLETE")
        println("📊 Top 5 horses:")
        scoredHorses.take(5).forEachIndexed { index, scoredHorse ->
            println("   ${index + 1}. ${scoredHorse.horse.name}: ${String.format("%.1f", scoredHorse.score)} points (${scoredHorse.betType})")
        }
        println("   Total horses analyzed: ${scoredHorses.size}")
        
        return RaceResult(
            race = race,
            topSelections = scoredHorses.take(5), // Return TOP 5 horses as required
            allHorses = scoredHorses
        )
    }
    
    /**
     * Get real horse form data from Racing Australia
     */
    private suspend fun getRealHorseForm(horse: Horse, race: Race, track: Track): HorseForm? {
        return try {
            println("🔍 Fetching real form data for ${horse.name}...")

            // Use the horse code that was extracted from the Racing Australia link
            val horseCode = horse.horseCode

            if (horseCode != null) {
                // Use the track key which is already in the correct format: "2025Aug27,VIC,Sportsbet Sandown Hillside"
                val key = track.key
                println("🔑 Using track key: $key")
                
                // Use the real race entry from the Horse object (extracted from the actual link)
                val raceEntry = horse.raceEntry
                if (raceEntry != null) {
                    println("🔑 Using real race entry from horse link: $raceEntry")
                } else {
                    println("⚠️ No race entry found for ${horse.name}")
                    println("🔍 This will cause the horse form request to fail")
                    return null
                }
                
                // Scrape the actual horse form data
                val horseForm = scrapingService.scrapeHorseForm(
                    horseCode = horseCode,
                    stage = "FinalFields",
                    key = key,
                    raceEntry = raceEntry
                )

                if (horseForm != null) {
                    println("✅ Successfully scraped real form data for ${horse.name}")
                    return horseForm
                } else {
                    println("❌ Could not scrape form data for ${horse.name} - NO FAKE DATA")
                    return null
                }
            } else {
                println("❌ No horse code available for ${horse.name} - NO FAKE DATA")
                return null
            }

        } catch (e: Exception) {
            println("❌ Error getting real horse form for ${horse.name}: ${e.message} - NO FAKE DATA")
            return null
        }
    }
    

    
    /**
     * Get available tracks for a given date
     */
    suspend fun getAvailableTracks(date: Date): List<Track> {
        return scrapingService.scrapeAvailableTracks(date)
    }
    
    /**
     * Get the number of races for a specific track on a given date
     */
    suspend fun getTrackRaceCount(track: Track, date: Date): Int {
        return try {
            println("🏁 Getting race count for ${track.name} on ${date}")
            val races = scrapingService.scrapeTrackRaces(track, date)
            val raceCount = races.size
            println("🏁 Track ${track.name} has $raceCount races")
            raceCount
        } catch (e: Exception) {
            println("⚠️ Failed to get race count for ${track.name}: ${e.message}")
            0
        }
    }
}

// Data classes for analysis results
data class AnalysisResult(
    val tracks: List<Track>,
    val date: Date,
    val results: List<RaceResult>,
    val processingTime: Long,
    val error: String? = null
)

data class TrackResult(
    val trackName: String,
    val trackKey: String,
    val races: List<RaceResult>,
    val error: String? = null
)
