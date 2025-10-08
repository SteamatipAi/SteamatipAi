package com.steamatipai.data.models

import java.util.Date

// Core horse data model
data class Horse(
    val id: String,
    val number: Int,
    val name: String,
    val jockey: String,
    val trainer: String,
    val weight: Double,
    val barrier: Int,
    val odds: Double?,
    val form: String,
    val age: Int,
    val sex: String,
    val color: String,
    val sire: String,
    val dam: String,
    val rating: Int?,
    val lastStart: Date?,
    val wins: Int,
    val places: Int,
    val earnings: Double,
    val score: Double = 0.0,
    val rank: Int = 0,
    val isStandout: Boolean = false,
    val horseCode: String? = null, // Store the extracted horse code from Racing Australia
    val raceEntry: String? = null // Store the actual race entry parameter from Racing Australia link
)

// Extended horse with scoring breakdown
data class ScoredHorse(
    val horse: Horse,
    val score: Double,
    val rank: Int = 0,
    val isStandout: Boolean = false,
    val scoreBreakdown: ScoreBreakdown,
    val betType: String = "CONSIDER" // Will be populated by betting classification
)

// Detailed scoring breakdown
data class ScoreBreakdown(
    val type: ScoringType? = null,
    val recentForm: Double = 0.0,        // Law 1 for normal horses
    val firstUp: Double = 0.0,           // Law 1 for spell horses
    val secondUp: Double = 0.0,          // Law 2 for spell horses
    val classSuitability: Double = 0.0,  // Law 3 (was Law 2)
    val trackDistance: Double = 0.0,     // Law 4 (was Law 3)
    val sectionalTime: Double = 0.0,     // Law 5 (was Law 4)
    val barrier: Double = 0.0,           // Law 6 (was Law 5)
    val jockey: Double = 0.0,            // Law 7 (was Law 6)
    val trainer: Double = 0.0,           // Law 8 (was Law 7)
    val combination: Double = 0.0,       // Law 9 (was Law 8)
    val trackCondition: Double = 0.0,    // Law 10 (was Law 9)
    val weightAdvantage: Double = 0.0,   // Law 11 (NEW)
    val freshness: Double = 0.0,         // Law 12 (NEW)
    val totalScore: Double = 0.0
)

enum class ScoringType {
    NORMAL,
    RETURNING_FROM_SPELL,
    FIRST_UP
}

// Race information
data class Race(
    val id: String,
    val raceNumber: Int,
    val name: String,
    val distance: Int,
    val surface: String,
    val trackCondition: String,
    val raceClass: String,
    val time: String,
    val venue: String,
    val horses: List<Horse> = emptyList(),
    val date: Date,
    val raceEntryCode: String? = null // Racing Australia race entry code for horse form URLs
)

// Race result with top selections
data class RaceResult(
    val race: Race,
    val topSelections: List<ScoredHorse>,
    val processingTime: Long = 0,
    val allHorses: List<ScoredHorse> = emptyList(),
    val error: String? = null,
    val bettingRecommendations: List<BettingRecommendation> = emptyList()
)

// Track information
data class Track(
    val key: String,
    val name: String,
    val state: String,
    val raceCount: Int,
    val url: String
)

// Jockey premiership data
data class JockeyPremiership(
    val name: String,
    val rank: Int,
    val wins: Int,
    val places: Int,
    val points: Int,
    val totalRides: Int,
    val winPercentage: Double
)

// Trainer premiership data
data class TrainerPremiership(
    val name: String,
    val rank: Int,
    val wins: Int,
    val places: Int,
    val points: Int,
    val totalRunners: Int,
    val winPercentage: Double
)

// Jockey-Trainer combination statistics
data class JockeyTrainerCombination(
    val jockeyName: String,
    val trainerName: String,
    val totalRuns: Int,
    val wins: Int,
    val places: Int,
    val winPercentage: Double,
    val placePercentage: Double,
    val recentForm: List<CombinationResult>, // Last 10 results together
    val trackSpecificStats: Map<String, TrackCombinationStats> // Stats by track
)

// Individual result for jockey-trainer combination
data class CombinationResult(
    val date: Date,
    val horseName: String,
    val position: Int,
    val track: String,
    val distance: Int,
    val raceClass: String,
    val odds: Double?
)

// Track-specific combination statistics
data class TrackCombinationStats(
    val track: String,
    val totalRuns: Int,
    val wins: Int,
    val places: Int,
    val winPercentage: Double
)

// Race result from form
data class RaceResultDetail(
    val position: Int,
    val margin: Double?,
    val raceClass: String?,
    val track: String?,
    val distance: Int?,
    val trackCondition: String?,  // Added for Law 9
    val sectionalTime: Double?,
    val date: Date?,
    val jockey: String?,
    val trainer: String?
)

// Up results for spell horses
data class UpResult(
    val wins: Int,
    val places: Int,
    val runs: Int,
    val firstUpStats: SpellPerformance? = null,  // 1st Up performance (e.g., "3:1-0-0")
    val secondUpStats: SpellPerformance? = null  // 2nd Up performance (e.g., "2:2-0-0")
)

// Spell performance statistics
data class SpellPerformance(
    val runs: Int,    // Total starts after spell
    val wins: Int,    // Wins after spell
    val seconds: Int, // 2nd places after spell
    val thirds: Int   // 3rd places after spell
)

// Track/Distance performance statistics
data class TrackDistanceStats(
    val trackStats: PerformanceStats,      // Performance at this track
    val distanceStats: PerformanceStats,   // Performance at this distance
    val combinedStats: PerformanceStats    // Performance at this track AND distance
)

data class PerformanceStats(
    val runs: Int,
    val wins: Int,
    val seconds: Int,
    val thirds: Int
) {
    val places: Int get() = wins + seconds + thirds
}

// Horse form history
data class HorseForm(
    val horseId: String,
    val last5Races: List<RaceResultDetail>,
    val trackDistanceHistory: List<RaceResultDetail>,
    val upResults: UpResult,
    val trialSectionalTimes: List<Double>,
    val trackDistanceStats: TrackDistanceStats? = null
)

// Betting recommendation types
enum class BetType {
    SUPER_BET,    // 8+ points clear of 2nd place
    BEST_BET,     // 5-7 points clear of 2nd place  
    GOOD_BET,     // 3-4 points clear of 2nd place
    CONSIDER      // 1-2 points clear of 2nd place
}

// Betting recommendation data
data class BettingRecommendation(
    val betType: BetType,
    val pointGap: Double,
    val confidence: String
)



