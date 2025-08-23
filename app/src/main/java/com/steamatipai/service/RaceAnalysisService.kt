package com.steamatipai.service

import com.steamatipai.data.models.*
import com.steamatipai.network.ScrapingService
import com.steamatipai.scoring.ScoringEngine
import kotlinx.coroutines.*
import java.util.*

class RaceAnalysisService(
    private val scrapingService: ScrapingService = ScrapingService(),
    private val scoringEngine: ScoringEngine = ScoringEngine()
) {
    
    /**
     * Main analysis function that processes selected tracks for a given date
     */
    suspend fun analyzeRaces(
        selectedTracks: List<Track>,
        selectedDate: Date,
        includeDetailedForm: Boolean = true
    ): AnalysisResult = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<TrackResult>()
        var totalHorsesAnalyzed = 0
        
        try {
            // Scrape premiership tables and combination data in parallel
            val (jockeyRankings, trainerRankings, combinationData) = scrapingService.scrapePremiershipTablesWithCombinations()
            
            // Set combination data in scoring engine
            scoringEngine.setCombinationData(combinationData)
            
            // Process each track in parallel for better performance
            val trackResults = coroutineScope {
                val trackJobs = selectedTracks.map { track ->
                    async {
                        try {
                            analyzeTrack(track, selectedDate, jockeyRankings, trainerRankings, includeDetailedForm)
                        } catch (e: Exception) {
                            // Return error result for this track
                            TrackResult(
                                trackName = track.name,
                                trackKey = track.key,
                                races = emptyList(),
                                error = e.message ?: "Unknown error"
                            )
                        }
                    }
                }
                
                // Wait for all tracks to complete analysis
                trackJobs.awaitAll()
            }
            
            trackResults.forEach { trackResult ->
                results.add(trackResult)
                totalHorsesAnalyzed += trackResult.races.sumOf { it.topSelections.size }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            AnalysisResult(
                results = results,
                processingTime = processingTime,
                totalHorsesAnalyzed = totalHorsesAnalyzed,
                success = true
            )
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            
            AnalysisResult(
                results = results,
                processingTime = processingTime,
                totalHorsesAnalyzed = totalHorsesAnalyzed,
                success = false,
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
                        analyzeRace(race, jockeyRankings, trainerRankings, includeDetailedForm)
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
        jockeyRankings: List<JockeyPremiership>,
        trainerRankings: List<TrainerPremiership>,
        includeDetailedForm: Boolean
    ): RaceResult {
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Scrape detailed horse form data if requested
            val horseForms = if (includeDetailedForm) {
                scrapingService.scrapeHorseForms(race.horses)
            } else {
                emptyMap()
            }
            
            // Score all horses in the race
            val scoredHorses = race.horses.map { horse ->
                val horseForm = horseForms[horse.id] ?: createEmptyHorseForm(horse.id)
                scoringEngine.calculateTotalScore(horse, race, horseForm, jockeyRankings, trainerRankings)
            }
            
            // Sort horses by score (highest first) and resolve ties
            val rankedHorses = scoringEngine.resolveTies(scoredHorses)
                .sortedByDescending { it.score }
            
            // Apply betting classifications
            val bettingRecommendations = scoringEngine.classifyBettingRecommendation(rankedHorses)
            
            // Update horses with their bet types
            val horsesWithBetTypes = rankedHorses.mapIndexed { index, scoredHorse ->
                val betType = if (index < bettingRecommendations.size) {
                    bettingRecommendations[index].betType.name
                } else {
                    "CONSIDER"
                }
                
                scoredHorse.copy(betType = betType)
            }
            
            // Take top 5 selections
            val topSelections = horsesWithBetTypes.take(5)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            return RaceResult(
                race = race,
                topSelections = topSelections,
                processingTime = processingTime
            )
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            
            return RaceResult(
                race = race,
                topSelections = emptyList(),
                processingTime = processingTime,
                error = e.message ?: "Race analysis failed"
            )
        }
    }
    
    /**
     * Create empty horse form for horses without detailed data
     */
    private fun createEmptyHorseForm(horseId: String): HorseForm {
        return HorseForm(
            horseId = horseId,
            last5Races = emptyList(),
            trackDistanceHistory = emptyList(),
            upResults = UpResult(0, 0, 0),
            trialSectionalTimes = emptyList()
        )
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
    val results: List<TrackResult>,
    val processingTime: Long,
    val totalHorsesAnalyzed: Int,
    val success: Boolean,
    val error: String? = null
)

data class TrackResult(
    val trackName: String,
    val trackKey: String,
    val races: List<RaceResult>,
    val error: String? = null
)

// Extended RaceResult to include error handling
data class RaceResult(
    val race: Race,
    val topSelections: List<ScoredHorse>,
    val processingTime: Long,
    val error: String? = null
)
