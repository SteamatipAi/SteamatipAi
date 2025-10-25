package com.steamatipai.scoring

import com.steamatipai.data.models.*
import java.util.*
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.math.max
import kotlin.math.min
import java.io.File
import java.io.FileWriter

class ScoringEngine {
    
    companion object {
        const val TOTAL_POINTS = 127.0  // Updated for 14 laws (normal horses)
        const val RECENT_FORM_WEIGHT = 25.0
        const val FIRST_UP_WEIGHT = 8.0  // NEW LAW 1 for spell horses
        const val SECOND_UP_WEIGHT = 8.0 // NEW LAW 2 for spell horses
        const val CLASS_SUITABILITY_WEIGHT = 25.0
        // Law 4-6: Track/Distance split into 3 separate laws
        const val DISTANCE_SUCCESS_WEIGHT = 8.0    // Law 4: Win/Place at this distance
        const val TRACK_SUCCESS_WEIGHT = 8.0       // Law 5: Win/Place at this track
        const val TRACK_DISTANCE_COMBINED_WEIGHT = 9.0 // Law 6: Win/Place at track+distance (includes pattern bonus)
        const val SECTIONAL_TIME_WEIGHT = 8.0
        const val BARRIER_WEIGHT = 6.0
        const val JOCKEY_WEIGHT = 8.0
        const val TRAINER_WEIGHT = 8.0
        const val COMBINATION_WEIGHT = 8.0
        const val TRACK_CONDITION_WEIGHT = 8.0
        const val WEIGHT_ADVANTAGE_WEIGHT = 8.0  // Law 13
        const val FRESHNESS_WEIGHT = 3.0         // Law 14
        
        const val SPELL_THRESHOLD_WEEKS = 12
        
        // CHAMPION JOCKEYS: Always receive maximum points regardless of current premiership standing
        val CHAMPION_JOCKEYS = listOf(
            "Craig Williams",
            "C Williams", 
            "CRAIG WILLIAMS",
            "C.Williams"
        )
    }
    
    // Store combination data for lookup
    private var combinationData: List<JockeyTrainerCombination> = emptyList()
    
    /**
     * Set combination data for scoring calculations
     */
    fun setCombinationData(combinations: List<JockeyTrainerCombination>) {
        this.combinationData = combinations
        println("🏇 Loaded ${combinations.size} jockey-trainer combinations for scoring")
    }
    
    /**
     * Find a specific jockey-trainer combination
     */
    private fun findCombination(jockeyName: String, trainerName: String): JockeyTrainerCombination? {
        return combinationData.find { combination ->
            combination.jockeyName.equals(jockeyName, ignoreCase = true) &&
            combination.trainerName.equals(trainerName, ignoreCase = true)
        }
    }
    
    /**
     * Main scoring function that applies all laws
     */
    fun calculateTotalScore(
        horse: Horse,
        race: Race,
        horseForm: HorseForm?,
        jockeyRankings: List<JockeyPremiership>,
        trainerRankings: List<TrainerPremiership>
    ): ScoredHorse? { // Changed return type to nullable
        
        println("🏆 STARTING SCORING FOR: ${horse.name} in Race ${race.raceNumber} at ${race.venue}")
        
        // COMPREHENSIVE DEBUG LOGGING for Miss Lola
        if (horse.name.contains("MISS LOLA", ignoreCase = true)) {
            try {
                val debugFile = File("/sdcard/Download/MISS_LOLA_COMPLETE_DEBUG.txt")
                val writer = FileWriter(debugFile, false) // Overwrite to start fresh
                writer.appendLine("=" .repeat(100))
                writer.appendLine("MISS LOLA COMPLETE DEBUG TRACE")
                writer.appendLine("=" .repeat(100))
                writer.appendLine("Horse Name: ${horse.name}")
                writer.appendLine("Horse ID: ${horse.id}")
                writer.appendLine("Race: ${race.raceNumber} at ${race.venue}")
                writer.appendLine("Analysis Date: ${race.date}")
                writer.appendLine("Horse Form Available: ${horseForm != null}")
                
                if (horseForm != null) {
                    writer.appendLine("Horse Form ID: ${horseForm.horseId}")
                    writer.appendLine("Total Races in Form: ${horseForm.last5Races.size}")
                    writer.appendLine("")
                    writer.appendLine("ALL RACES IN HORSE FORM:")
                    horseForm.last5Races.forEachIndexed { index, raceResult ->
                        writer.appendLine("  Race ${index + 1}: Position=${raceResult.position}, Date=${raceResult.date}, Track=${raceResult.track}")
                        writer.appendLine("    Margin=${raceResult.margin}, Distance=${raceResult.distance}")
                        writer.appendLine("    TrackCondition=${raceResult.trackCondition}, SectionalTime=${raceResult.sectionalTime}")
                        writer.appendLine("    Jockey=${raceResult.jockey}, Trainer=${raceResult.trainer}")
                        writer.appendLine("    RaceClass=${raceResult.raceClass}")
                        writer.appendLine("")
                    }
                }
                writer.close()
            } catch (e: Exception) {
                println("Error writing Miss Lola debug: ${e.message}")
            }
        }
        
        // SPECIAL DEBUGGING for Kings Valley and Mr Monaco
        val isDebugHorse = horse.name.contains("KINGS VALLEY", ignoreCase = true) || 
                          horse.name.contains("MR MONACO", ignoreCase = true)
        
        if (isDebugHorse) {
            println("🚨🚨🚨 DEBUG HORSE DETECTED: ${horse.name} 🚨🚨🚨")
            println("🚨 Analysis Date: ${race.date}")
            println("🚨 Horse Form Available: ${horseForm != null}")
            if (horseForm != null) {
                println("🚨 Total Races in Form: ${horseForm.last5Races.size}")
                horseForm.last5Races.forEachIndexed { index, raceResult ->
                    println("🚨 Race ${index + 1}: Position=${raceResult.position}, Date=${raceResult.date}, Track=${raceResult.track}")
                }
            }
        }
        
        // FILE LOGGING for Race 1 to avoid logcat buffer overflow
        val isRace1Debug = race.raceNumber == 1
        if (isRace1Debug) {
            try {
                val logFile = File("/sdcard/Download/race1_${horse.name.replace(" ", "_").replace("/", "_")}.txt")
                val writer = FileWriter(logFile, false) // Overwrite to start fresh
                writer.appendLine("=" .repeat(80))
                writer.appendLine("🏆 STARTING SCORING FOR: ${horse.name} in Race ${race.raceNumber}")
                writer.appendLine("🗓️ Analysis Date: ${race.date}")
                writer.appendLine("📊 Horse Form Available: ${horseForm != null}")
                if (horseForm != null) {
                    writer.appendLine("📊 Total Races in Form: ${horseForm.last5Races.size}")
                    horseForm.last5Races.forEachIndexed { index, raceResult ->
                        writer.appendLine("📊 Race ${index + 1}: Position=${raceResult.position}, Date=${raceResult.date}, Track=${raceResult.track}")
                    }
                }
                writer.appendLine("=" .repeat(80))
                writer.close()
            } catch (e: Exception) {
                println("⚠️ Could not write to file: ${e.message}")
            }
        }
        
        if (horseForm == null) {
            println("❌ ${horse.name} - NO REAL FORM DATA AVAILABLE - EXCLUDING FROM SCORING")
            // FILE LOGGING for debugging
            try {
                val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                val writer = FileWriter(logFile, true)
                writer.appendLine("❌ COSMIC VIXEN - NO REAL FORM DATA AVAILABLE - EXCLUDING FROM SCORING")
                writer.appendLine("This means the scraper failed to get any form data for this horse")
                writer.close()
            } catch (e: Exception) {
                println("⚠️ Could not write to debug file: ${e.message}")
            }
            return null
        }
        
        // Check if horse is returning from spell based on form string
        val currentSpellStatus = getCurrentSpellStatus(horse, race.date, horseForm)
        val isReturningFromSpell = currentSpellStatus == "1ST_UP" || currentSpellStatus == "2ND_UP"
        
        // DEBUG LOGGING for Miss Lola spell status
        if (horse.name.contains("MISS LOLA", ignoreCase = true)) {
            try {
                val debugFile = File("/sdcard/Download/MISS_LOLA_COMPLETE_DEBUG.txt")
                val writer = FileWriter(debugFile, true) // Append mode
                writer.appendLine("SPELL STATUS DETECTION:")
                writer.appendLine("  Current Spell Status: $currentSpellStatus")
                writer.appendLine("  Is Returning From Spell: $isReturningFromSpell")
                writer.appendLine("  Form String: ${horse.form}")
                writer.appendLine("")
                writer.close()
            } catch (e: Exception) {
                println("Error writing Miss Lola spell debug: ${e.message}")
            }
        }
        
        // Check if this is a true first-starter (no historical races)
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, race.date)
        
        // CRITICAL FIX: If horse is returning from spell, only use races AFTER the most recent X
        val racesForScoring = if (isReturningFromSpell) {
            getRacesAfterMostRecentSpell(historicalRaces, horse.form, race.date)
        } else {
            historicalRaces
        }
        
        println("🔍 ${horse.name} - Races for scoring: ${racesForScoring.size} (from ${historicalRaces.size} historical races)")
        
        val isFirstUp = isFirstUpHorse(racesForScoring)
        
        when {
            isReturningFromSpell -> {
                println("🏆 ${horse.name} - USING SPELL HORSE SCORING (returning from spell)")
                println("🏆 ${horse.name} - Current spell status: $currentSpellStatus")
                
                // Calculate individual law scores based on current spell status
                val firstUp = if (currentSpellStatus == "1ST_UP") calculateFirstUpScore(horseForm) else 0.0
                val secondUp = if (currentSpellStatus == "2ND_UP") calculateSecondUpScore(horseForm) else 0.0
                val secondUpRecentFormBonus = if (currentSpellStatus == "2ND_UP") {
                    val bonus = calculateSecondUpRecentFormBonus(horseForm, race.date)
                    println("🔍 ${horse.name} - 2ND_UP Recent Form Bonus calculated: ${String.format("%.1f", bonus)}")
                    bonus
                } else {
                    println("🔍 ${horse.name} - Not 2ND_UP (status: $currentSpellStatus), Recent Form Bonus: 0.0")
                    0.0
                }
                val classSuitability = calculateClassSuitabilityScore(horse, race, horseForm) // Use historical class performance
                // NEW: Split track/distance into 3 separate laws
                val distanceSuccess = calculateDistanceSuccessScore(horse, race, horseForm) // Law 4
                val trackSuccess = calculateTrackSuccessScore(horse, race, horseForm) // Law 5
                val trackDistanceCombined = calculateTrackDistanceCombinedScore(horse, race, horseForm) // Law 6
                val trackConditionSuccess = calculateTrackConditionSuccessScore(horse, race, horseForm) // Law 7
                val sectionalTime = if (currentSpellStatus == "2ND_UP") calculateSectionalTimeScore(horseForm, race.date) else 0.0 // Only 2nd Up horses get sectional time
                val barrier = calculateBarrierScore(horse, race) // UPDATED: now distance-aware
                val jockey = calculateJockeyScore(horse, jockeyRankings)
                val trainer = calculateTrainerScore(horse, trainerRankings)
                val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, race, horseForm) // Use historical relationship
                val jockeyTrainerPartnership = 0.0 // Removed jockey-trainer partnership scoring
                val combination = jockeyHorseRelationship // Only jockey-horse relationship now
                val trackCondition = trackConditionSuccess // Use track condition success calculation
                val weightAdvantage = calculateWeightAdvantageScore(horse, race.horses) // NEW LAW 13
                // CRITICAL FIX: Only 1ST_UP horses are "fresh from spell". 2ND_UP horses should get freshness based on their 1st up run
                val freshness = if (currentSpellStatus == "1ST_UP") {
                    0.0 // 1st up horses don't get freshness bonus (they're fresh from spell)
                } else {
                    calculateFreshnessScore(horseForm, race.date, horse.name) // 2nd up horses get normal freshness calculation
                }
                
                // DEBUG LOGGING for Miss Lola freshness calculation
                if (horse.name.contains("MISS LOLA", ignoreCase = true)) {
                    try {
                        val debugFile = File("/sdcard/Download/MISS_LOLA_COMPLETE_DEBUG.txt")
                        val writer = FileWriter(debugFile, true) // Append mode
                        writer.appendLine("FRESHNESS CALCULATION:")
                        writer.appendLine("  Current Spell Status: $currentSpellStatus")
                        writer.appendLine("  Freshness Score: $freshness")
                        writer.appendLine("  Calculation Logic: ${if (currentSpellStatus == "1ST_UP") "1ST_UP = 0.0 (fresh from spell)" else "2ND_UP = calculateFreshnessScore()"}")
                        writer.appendLine("")
                        writer.close()
                    } catch (e: Exception) {
                        println("Error writing Miss Lola freshness debug: ${e.message}")
                    }
                }
                
                // Calculate total score (including second up recent form bonus and new laws)
                val score = firstUp + secondUp + secondUpRecentFormBonus + classSuitability + 
                    distanceSuccess + trackSuccess + trackDistanceCombined + 
                    sectionalTime + barrier + jockey + trainer + combination + trackCondition +
                    weightAdvantage + freshness
                
                // COMPREHENSIVE SPELL HORSE SCORING LOGGING
                println("🏆 ${horse.name} SPELL HORSE SCORING BREAKDOWN (Status: $currentSpellStatus):")
                println("   🎯 L1: 1st Up Performance: ${String.format("%.1f", firstUp)} points ${if (currentSpellStatus == "1ST_UP") "(ACTIVE)" else "(inactive)"}")
                println("   🎯 L2: 2nd Up Performance: ${String.format("%.1f", secondUp)} points ${if (currentSpellStatus == "2ND_UP") "(ACTIVE)" else "(inactive)"}")
                println("   📊 L3: Recent Form: ${String.format("%.1f", secondUpRecentFormBonus)} points ${if (currentSpellStatus == "2ND_UP") "(2nd up bonus)" else "(not used)"}")
                println("   🎯 L4: Class Suitability: ${String.format("%.1f", classSuitability)} points")
                println("   📏 L5: Distance Success: ${String.format("%.1f", distanceSuccess)} points")
                println("   🏟️ L6: Track Success: ${String.format("%.1f", trackSuccess)} points")
                println("   🎯 L7: Track+Distance Combined: ${String.format("%.1f", trackDistanceCombined)} points")
                println("   ⚡ L8: Sectional Time: ${String.format("%.1f", sectionalTime)} points ${if (currentSpellStatus == "2ND_UP") "(from 1st Up race)" else "(no recent data)"}")
                println("   🚪 L9: Barrier: ${String.format("%.1f", barrier)} points")
                println("   🏇 L10: Jockey: ${String.format("%.1f", jockey)} points")
                println("   👨‍🏫 L11: Trainer: ${String.format("%.1f", trainer)} points")
                println("   🤝 L12: Jockey-Horse Relationship: ${String.format("%.1f", jockeyHorseRelationship)} points")
                println("   🌦️ L13: Track Condition: ${String.format("%.1f", trackCondition)} points")
                println("   ⚖️ L14: Weight Advantage: ${String.format("%.1f", weightAdvantage)} points")
                println("   📅 L15: Freshness: ${String.format("%.1f", freshness)} points (spell horses exempt)")
                println("   💯 TOTAL SCORE: ${String.format("%.1f", score)} points")
                println("   ─".repeat(50))
                
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.RETURNING_FROM_SPELL,
                    recentForm = secondUpRecentFormBonus, // Use second up recent form bonus for spell horses
                    firstUp = firstUp,
                    secondUp = secondUp,
                    classSuitability = classSuitability,
                    trackDistance = 0.0, // DEPRECATED - now split into 3 laws
                    distanceSuccess = distanceSuccess,
                    trackSuccess = trackSuccess,
                    trackDistanceCombined = trackDistanceCombined,
                    sectionalTime = sectionalTime,
                    barrier = barrier,
                    jockey = jockey,
                    trainer = trainer,
                    combination = combination,
                    trackCondition = trackCondition,
                    weightAdvantage = weightAdvantage,  // LAW 14
                    freshness = freshness,              // LAW 15 (0 for spell horses)
                    totalScore = score
                )
                return ScoredHorse(
                    horse = horse,
                    score = score,
                    scoreBreakdown = scoreBreakdown
                )
            }
            isFirstUp -> {
                println("🏆 ${horse.name} - USING FIRST UP HORSE SCORING (first starter)")
                println("🏆 ${horse.name} - Historical races: ${historicalRaces.size} (should be 0 for true first-starter)")
                val score = calculateFirstUpHorseScore(horse, race, horseForm, jockeyRankings, trainerRankings)
                
                // Calculate individual law scores for display
                val recentForm = 0.0 // First-up horses get 0 for recent form
                val classSuitability = 0.0 // First-up horses get 0 for class suitability
                val distanceSuccess = 0.0 // First-up horses get 0 for distance history
                val trackSuccess = 0.0 // First-up horses get 0 for track history
                val trackDistanceCombined = 0.0 // First-up horses get 0 for track+distance history
                val trackConditionSuccess = 0.0 // First-up horses get 0 for track condition history
                val sectionalTime = 0.0 // First-up horses get 0 for sectional time (no historical races)
                val barrier = calculateBarrierScore(horse, race) // UPDATED: now distance-aware
                val jockey = calculateJockeyScore(horse, jockeyRankings)
                val trainer = calculateTrainerScore(horse, trainerRankings)
                val jockeyHorseRelationship = 0.0 // First-up horses get 0 for jockey-horse relationship
                val jockeyTrainerPartnership = 0.0 // First-up horses get 0 for jockey-trainer partnership
                val combination = jockeyHorseRelationship // Only jockey-horse relationship now
                val trackCondition = trackConditionSuccess // Use track condition success calculation
                val weightAdvantage = calculateWeightAdvantageScore(horse, race.horses) // NEW LAW 13
                val freshness = 0.0 // First starters don't get freshness bonus (no race history)
                
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.FIRST_UP,
                    recentForm = recentForm,
                    classSuitability = classSuitability,
                    trackDistance = 0.0, // DEPRECATED
                    distanceSuccess = distanceSuccess,
                    trackSuccess = trackSuccess,
                    trackDistanceCombined = trackDistanceCombined,
                    sectionalTime = sectionalTime,
                    barrier = barrier,
                    jockey = jockey,
                    trainer = trainer,
                    combination = combination,
                    trackCondition = trackCondition,
                    weightAdvantage = weightAdvantage,  // NEW LAW 13
                    freshness = freshness,              // NEW LAW 14 (0 for first starters)
                    totalScore = score
                )
                return ScoredHorse(
                    horse = horse,
                    score = score,
                    scoreBreakdown = scoreBreakdown
                )
            }
            else -> {
                println("🏆 ${horse.name} - USING NORMAL HORSE SCORING (all 14 laws)")
                // Apply all normal laws
                val recentForm = calculateRecentFormScore(horse, horseForm, race.date)
                val classSuitability = calculateClassSuitabilityScore(horse, race, horseForm)
                // NEW: Split track/distance into 3 separate laws
                val distanceSuccess = calculateDistanceSuccessScore(horse, race, horseForm) // Law 4
                val trackSuccess = calculateTrackSuccessScore(horse, race, horseForm) // Law 5
                val trackDistanceCombined = calculateTrackDistanceCombinedScore(horse, race, horseForm) // Law 6
                val trackConditionSuccess = calculateTrackConditionSuccessScore(horse, race, horseForm) // Law 7
                val sectionalTime = calculateSectionalTimeScore(horseForm, race.date)
                val barrier = calculateBarrierScore(horse, race) // UPDATED: now distance-aware
                val jockey = calculateJockeyScore(horse, jockeyRankings)
                val trainer = calculateTrainerScore(horse, trainerRankings)
                val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, race, horseForm)
                val jockeyTrainerPartnership = 0.0 // Removed jockey-trainer partnership scoring
                val combination = jockeyHorseRelationship // Only jockey-horse relationship now
                val trackCondition = trackConditionSuccess // Use track condition success calculation
                val weightAdvantage = calculateWeightAdvantageScore(horse, race.horses) // NEW LAW 13
                val freshness = calculateFreshnessScore(horseForm, race.date, horse.name) // NEW LAW 14
                
                val totalScore = recentForm + classSuitability + 
                    distanceSuccess + trackSuccess + trackDistanceCombined + 
                    sectionalTime + barrier + jockey + trainer + combination + trackCondition +
                    weightAdvantage + freshness
                
                // COMPREHENSIVE SCORING LOGGING
                println("🏆 ${horse.name} NORMAL HORSE SCORING BREAKDOWN:")
                println("   🎯 L1: 1st Up Performance: ${String.format("%.1f", 0.0)} points (not used for normal horses)")
                println("   🎯 L2: 2nd Up Performance: ${String.format("%.1f", 0.0)} points (not used for normal horses)")
                println("   📊 L3: Recent Form: ${String.format("%.1f", recentForm)} points")
                println("   🎯 L4: Class Suitability: ${String.format("%.1f", classSuitability)} points")
                println("   📏 L5: Distance Success: ${String.format("%.1f", distanceSuccess)} points")
                println("   🏟️ L6: Track Success: ${String.format("%.1f", trackSuccess)} points")
                println("   🎯 L7: Track+Distance Combined: ${String.format("%.1f", trackDistanceCombined)} points")
                println("   ⚡ L8: Sectional Time: ${String.format("%.1f", sectionalTime)} points")
                println("   🚪 L9: Barrier: ${String.format("%.1f", barrier)} points")
                println("   🏇 L10: Jockey: ${String.format("%.1f", jockey)} points")
                println("   👨‍🏫 L11: Trainer: ${String.format("%.1f", trainer)} points")
                println("   🤝 L12: Jockey-Horse Relationship: ${String.format("%.1f", jockeyHorseRelationship)} points")
                println("   🌦️ L13: Track Condition: ${String.format("%.1f", trackCondition)} points")
                println("   ⚖️ L14: Weight Advantage: ${String.format("%.1f", weightAdvantage)} points")
                println("   📅 L15: Freshness: ${String.format("%.1f", freshness)} points")
                println("   💯 TOTAL SCORE: ${String.format("%.1f", totalScore)} points")
                println("   ─".repeat(50))
                
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.NORMAL,
                    recentForm = recentForm,
                    classSuitability = classSuitability,
                    trackDistance = 0.0, // DEPRECATED - now split into 3 laws
                    distanceSuccess = distanceSuccess,
                    trackSuccess = trackSuccess,
                    trackDistanceCombined = trackDistanceCombined,
                    sectionalTime = sectionalTime,
                    barrier = barrier,
                    jockey = jockey,
                    trainer = trainer,
                    combination = combination,
                    trackCondition = trackCondition,
                    weightAdvantage = weightAdvantage,  // NEW LAW 13
                    freshness = freshness,              // NEW LAW 14
                    totalScore = totalScore
                )
                
                // SPECIAL DEBUGGING for Kings Valley and Mr Monaco
                if (isDebugHorse) {
                    println("🚨🚨🚨 FINAL SCORE FOR ${horse.name}: ${String.format("%.1f", totalScore)} 🚨🚨🚨")
                    println("🚨 Score Breakdown:")
                    println("🚨   Recent Form: ${String.format("%.1f", recentForm)}")
                    println("🚨   Class Suitability: ${String.format("%.1f", classSuitability)}")
                    println("🚨   Distance Success: ${String.format("%.1f", distanceSuccess)}")
                    println("🚨   Track Success: ${String.format("%.1f", trackSuccess)}")
                    println("🚨   Track+Distance Combined: ${String.format("%.1f", trackDistanceCombined)}")
                    println("🚨   Track Condition: ${String.format("%.1f", trackCondition)}")
                    println("🚨   Jockey: ${String.format("%.1f", jockey)}")
                    println("🚨   Trainer: ${String.format("%.1f", trainer)}")
                    println("🚨   Barrier: ${String.format("%.1f", barrier)}")
                    println("🚨   Sectional Time: ${String.format("%.1f", sectionalTime)}")
                    println("🚨   Weight Advantage: ${String.format("%.1f", weightAdvantage)}")
                    println("🚨   Freshness: ${String.format("%.1f", freshness)}")
                }
                
                return ScoredHorse(
                    horse = horse,
                    score = totalScore,
                    scoreBreakdown = scoreBreakdown
                )
            }
        }
    }
    
    /**
     * Law 1: Recent Form for Normal Horses (25 points)
     * Considers last 5 races for wins, places, and last start margin bonus
     */
    private fun calculateRecentFormScore(horse: Horse, horseForm: HorseForm, analysisDate: Date): Double {
        println("🔍 Law 1 DEBUG: Starting Law 1 calculation for ${horse.name}")
        println("🔍 Law 1 DEBUG: HorseForm.last5Races.size = ${horseForm.last5Races.size}")
        
        if (horseForm.last5Races.isEmpty()) {
            println("🏇 Law 1: No recent races found - returning 0.0")
            return 0.0
        }
        
        // CRITICAL FIX: Filter out races from today (analysis date) to avoid using today's results
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, analysisDate)
        
        if (historicalRaces.isEmpty()) {
            println("⚠️ Law 1: No historical races available after filtering today's races - returning 0.0")
            return 0.0
        }
        
        var score = 0.0
        val last5Races = historicalRaces.take(5)
        println("🏇 Law 1: Processing ${last5Races.size} recent races (filtered from ${horseForm.last5Races.size} total)")
        
        // Debug: Print all race details
        last5Races.forEachIndexed { index, race ->
            println("🔍 Law 1 DEBUG: Race ${index + 1} details:")
            println("   Position: ${race.position}")
            println("   Margin: ${race.margin}")
            println("   Track: ${race.track}")
            println("   Distance: ${race.distance}")
            println("   Date: ${race.date}")
            println("   Jockey: ${race.jockey}")
            println("   Trainer: ${race.trainer}")
        }
        
        // DEBUG: Print detailed information about race filtering
        println("🔍 Law 1 DEBUG: Horse=${horse.name}, Original races: ${horseForm.last5Races.size}, Historical races: ${last5Races.size}")
        horseForm.last5Races.forEachIndexed { index, race ->
            println("🔍 Law 1 DEBUG: ${horse.name} Original race $index: Position=${race.position}, Date=${race.date}")
        }
        last5Races.forEachIndexed { index, race ->
            println("🔍 Law 1 DEBUG: ${horse.name} Historical race $index: Position=${race.position}, Date=${race.date}")
        }
        
        // SPECIAL CASE: For horses with only 1 historical race, use simplified scoring
        if (last5Races.size == 1) {
            val singleRace = last5Races[0]
            val score = when (singleRace.position) {
                1 -> 8.0 // Win gets 8 points (same as 1st up performance)
                2, 3 -> 4.0 // Place gets 4 points
                4, 5 -> 2.0 // Close finish gets 2 points
                else -> 0.0
            }
            println("🏇 Law 1: ${horse.name} - Single historical race - Position ${singleRace.position}, Score: ${String.format("%.1f", score)}")
            println("🔍 Law 1 DEBUG: ${horse.name} - SPECIAL CASE HIT - returning ${String.format("%.1f", score)} points")
            println("🚨🚨🚨 COSMIC VIXEN CHECK: If this is COSMIC VIXEN, score should be 8.0, actual=${String.format("%.1f", score)} 🚨🚨🚨")
            return min(score, RECENT_FORM_WEIGHT)
        }
        
        println("🔍 Law 1 DEBUG: ${horse.name} - SPECIAL CASE NOT HIT (has ${last5Races.size} races) - proceeding with regular scoring logic")
        
        // IMPROVED: Better handling for horses with fewer than 5 races
        val availableRaces = last5Races.size
        val scalingFactor = if (availableRaces < 5) {
            // Scale up scoring for horses with fewer races, but cap it to prevent excessive scoring
            // Maximum scaling factor of 2.0 to prevent a single win from getting 25 points
            min(5.0 / availableRaces.toDouble(), 2.0)
        } else {
            1.0
        }
        
        if (availableRaces < 5) {
            println("🏇 Law 1: Horse has only $availableRaces recent races (scaling factor: ${String.format("%.2f", scalingFactor)})")
        }
        
        last5Races.forEachIndexed { index, race ->
            val recency = 5.0 - index // More recent = higher multiplier
            val multiplier = recency / 5.0
            
            val positionPoints = when (race.position) {
                1 -> 5.0 * multiplier // Win bonus
                2, 3 -> 3.0 * multiplier // Place bonus
                4, 5 -> 1.5 * multiplier // Close finish bonus
                6, 7, 8 -> 1.0 * multiplier // Decent finish bonus
                else -> 0.0
            }
            
            // Apply scaling factor for horses with fewer races
            val scaledPoints = positionPoints * scalingFactor
            score += scaledPoints
            
            if (scalingFactor > 1.0) {
                println("🏇 Law 1: Race ${index + 1} - Position ${race.position}, Base Points: ${String.format("%.1f", positionPoints)}, Scaled Points: ${String.format("%.1f", scaledPoints)} (multiplier: ${String.format("%.1f", multiplier)}, scaling: ${String.format("%.2f", scalingFactor)})")
            } else {
                println("🏇 Law 1: Race ${index + 1} - Position ${race.position}, Margin ${race.margin}, Points: ${String.format("%.1f", positionPoints)} (multiplier: ${String.format("%.1f", multiplier)})")
            }
        }
        
        // SPECIAL BONUS: Last start margin bonus (within 4 lengths of winner)
        // Only applies to the most recent race (index 0)
        if (last5Races.isNotEmpty()) {
            val lastStart = last5Races[0]
            println("🔍 Law 1 DEBUG: Checking last start margin bonus for position ${lastStart.position}")
            lastStart.margin?.let { margin ->
                println("🔍 Law 1 DEBUG: Last start margin = $margin")
                // Bonus for being close to winner regardless of position
                if (margin <= 4.0) {
                    score += 3.0 // Fixed bonus for last start performance
                    println("🏇 ${lastStart.position}${getPositionSuffix(lastStart.position)} place within ${margin} lengths - margin bonus applied (+3.0)")
                } else {
                    println("🔍 Law 1 DEBUG: Margin $margin > 4.0, no bonus applied")
                }
            } ?: run {
                println("🔍 Law 1 DEBUG: Last start margin is null, no bonus applied")
            }
        }
        
        val finalScore = min(score, RECENT_FORM_WEIGHT)
        println("🏇 Law 1: ${horse.name} - Total score: ${String.format("%.1f", finalScore)} (capped at ${RECENT_FORM_WEIGHT})")
        println("🔍 Law 1 DEBUG: ${horse.name} - REGULAR SCORING PATH - returning ${String.format("%.1f", finalScore)} points")
        if (horse.name.contains("COSMIC VIXEN", ignoreCase = true)) {
            println("🚨🚨🚨 COSMIC VIXEN ALERT: Recent Form score = ${String.format("%.1f", finalScore)} (expected 8.0 for single win) 🚨🚨🚨")
            
            // Write detailed debug to file
            try {
                val logFile = File("/sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
                val writer = FileWriter(logFile, false) // Overwrite each time
                writer.appendLine("=" .repeat(80))
                writer.appendLine("COSMIC VIXEN - Recent Form Scoring Debug")
                writer.appendLine("=" .repeat(80))
                writer.appendLine("Original races: ${horseForm.last5Races.size}")
                writer.appendLine("Historical races (after filtering): ${last5Races.size}")
                writer.appendLine("Scaling factor: ${String.format("%.2f", scalingFactor)}")
                writer.appendLine("\nHistorical Races:")
                last5Races.forEachIndexed { index, race ->
                    writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}, Distance=${race.distance}")
                }
                writer.appendLine("\nFinal Score: ${String.format("%.1f", finalScore)}")
                writer.appendLine("Expected: 8.0 (for single win)")
                writer.appendLine("=" .repeat(80))
                writer.close()
                println("📝 Wrote Cosmic Vixen debug to /sdcard/Download/COSMIC_VIXEN_DEBUG.txt")
            } catch (e: Exception) {
                println("⚠️ Could not write Cosmic Vixen debug file: ${e.message}")
            }
        }
        return finalScore
    }
    
    /**
     * Determine if horse is currently 1st up or 2nd up from current spell
     * Parses the form string to check recent entries (reading right-to-left)
     */
    private fun getCurrentSpellStatus(horse: Horse, analysisDate: Date, horseForm: HorseForm): String {
        val form = horse.form.trim()
        println("🔍 Analysing form string: '$form' for ${horse.name}")
        
        if (form.isEmpty()) {
            println("🔍 No form data available")
            return "UNKNOWN"
        }
        
        // CRITICAL FIX: The form string may include today's race result if it's already on the form page
        // We need to filter out today's race from the form string before analyzing spell status
        
        // Check if the most recent race in last5Races is from today
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, analysisDate)
        val todayRaceCount = horseForm.last5Races.size - historicalRaces.size
        
        // If there are races from today, we need to strip them from the form string
        val historicalForm = if (todayRaceCount > 0) {
            // Remove the first N characters from the form string (most recent races)
            if (form.length > todayRaceCount) {
                form.substring(todayRaceCount)
            } else {
                "" // All races are from today, no historical form
            }
        } else {
            form
        }
        
        println("🔍 Original form: '$form', Historical form (excluding today): '$historicalForm', Races from today: $todayRaceCount")
        
        if (historicalForm.isEmpty()) {
            println("🔍 No historical form data available after filtering today's races")
            return "UNKNOWN"
        }
        
        // Read form string right-to-left (most recent HISTORICAL first)
        // Example: "32x1x2212x" -> read as: x, 2, 1, 2, x, 1, x, 2, 3
        
        val reversedForm = historicalForm.reversed()
        println("🔍 Reversed historical form string: '$reversedForm'")
        
        // Check first character (most recent HISTORICAL race)
        val firstChar = reversedForm.firstOrNull()
        
        when {
            firstChar == 'x' || firstChar == 'X' -> {
                println("🔍 Most recent HISTORICAL race is 'X' - horse is 1st Up from spell")
                return "1ST_UP"
            }
            firstChar != null && firstChar.isDigit() -> {
                // Most recent is a number, check if second character is X (spell)
                if (reversedForm.length >= 2 && (reversedForm[1] == 'x' || reversedForm[1] == 'X')) {
                    println("🔍 Pattern: ${firstChar}X - horse is 2nd Up from spell")
                    return "2ND_UP"
                } else {
                    println("🔍 Most recent HISTORICAL race is number but no spell pattern - normal horse")
                    return "NORMAL"
                }
            }
            else -> {
                println("🔍 Unknown form pattern: $firstChar")
                return "UNKNOWN"
            }
        }
    }
    
    /**
     * Law 1: 1st Up Performance for Spell Horses (up to 8 points)
     * Simplified scoring: 5 points if ever won 1st Up, 3 points if ever placed 1st Up
     */
    private fun calculateFirstUpScore(horseForm: HorseForm): Double {
        println("🔍 Law 1: Starting 1st Up performance calculation (simplified)")
        
        val upResults = horseForm.upResults
        var score = 0.0
        
        upResults.firstUpStats?.let { firstUp ->
            println("🏇 Law 1: 1st Up stats - ${firstUp.runs} runs, ${firstUp.wins} wins, ${firstUp.seconds} seconds, ${firstUp.thirds} thirds")
            
            // Simplified scoring system
            if (firstUp.wins > 0) {
                score += 5.0 // 5 points if has ever won 1st Up (regardless of how many wins)
                println("🏇 Law 1: +5.0 points for having won 1st Up")
            }
            if (firstUp.seconds > 0 || firstUp.thirds > 0) {
                score += 3.0 // 3 points if has ever placed 1st Up (2nd or 3rd, regardless of how many)
                println("🏇 Law 1: +3.0 points for having placed 1st Up")
            }
        }
        
        val finalScore = min(score, FIRST_UP_WEIGHT)
        println("🏇 Law 1: 1st Up performance score: ${String.format("%.1f", finalScore)} (simplified system, max ${FIRST_UP_WEIGHT})")
        return finalScore
    }
    
    /**
     * Law 2: 2nd Up Performance for Spell Horses (up to 8 points)
     * Uses historical 2nd Up performance when returning from previous spells
     */
    private fun calculateSecondUpScore(horseForm: HorseForm): Double {
        println("🔍 Law 2: Starting 2nd Up performance calculation")
        
        val upResults = horseForm.upResults
        var score = 0.0
        
        upResults.secondUpStats?.let { secondUp ->
            println("🏇 Law 2: 2nd Up stats - ${secondUp.runs} runs, ${secondUp.wins} wins, ${secondUp.seconds} seconds, ${secondUp.thirds} thirds")
            
            // Award points based on 2nd Up performance
            if (secondUp.wins > 0) {
                score += 5.0 // 5 points for winning in second starts after spells
                println("🏇 Law 2: +5.0 points for 2nd Up wins")
            }
            if (secondUp.seconds > 0) {
                score += 3.0 // 3 points for 2nd places in second starts after spells
                println("🏇 Law 2: +3.0 points for 2nd Up seconds")
            }
            if (secondUp.thirds > 0) {
                score += 1.0 // 1 point for 3rd places in second starts after spells
                println("🏇 Law 2: +1.0 points for 2nd Up thirds")
            }
        }
        
        val finalScore = min(score, SECOND_UP_WEIGHT)
        println("🏇 Law 2: 2nd Up performance score: ${String.format("%.1f", finalScore)} (capped at ${SECOND_UP_WEIGHT})")
        return finalScore
    }
    
    /**
     * Law 2b: Second Up Recent Form Bonus (up to 8 points)
     * Awards bonus points for the first-up performance when horse is second up
     * This fixes the issue where second up horses get 0 for recent form despite good first-up runs
     */
    private fun calculateSecondUpRecentFormBonus(horseForm: HorseForm, analysisDate: Date): Double {
        println("🔍 Law 2b: Starting Second Up Recent Form Bonus calculation")
        
        // FILE LOGGING for debugging
        try {
            val logFile = File("/sdcard/Download/SECOND_UP_DEBUG_${horseForm.horseId.replace(" ", "_")}.txt")
            val writer = FileWriter(logFile, false) // Overwrite mode
            writer.appendLine("=" .repeat(80))
            writer.appendLine("SECOND UP RECENT FORM BONUS DEBUG")
            writer.appendLine("Horse ID: ${horseForm.horseId}")
            writer.appendLine("Analysis Date: $analysisDate")
            writer.appendLine("Total races in form: ${horseForm.last5Races.size}")
            writer.appendLine("\nAll races BEFORE filtering:")
            horseForm.last5Races.forEachIndexed { index, race ->
                writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
            }
            writer.close()
        } catch (e: Exception) {
            println("⚠️ Could not write to debug file: ${e.message}")
        }
        
        if (horseForm.last5Races.isEmpty()) {
            println("🏇 Law 2b: No recent races found - returning 0.0")
            return 0.0
        }
        
        // CRITICAL FIX: Filter out races from today (analysis date) to avoid using today's results
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, analysisDate)
        
        // FILE LOGGING - after filtering
        try {
            val logFile = File("/sdcard/Download/SECOND_UP_DEBUG_${horseForm.horseId.replace(" ", "_")}.txt")
            val writer = FileWriter(logFile, true) // Append mode
            writer.appendLine("\nHistorical races AFTER filtering: ${historicalRaces.size}")
            historicalRaces.forEachIndexed { index, race ->
                writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
            }
            writer.close()
        } catch (e: Exception) {
            println("⚠️ Could not write to debug file: ${e.message}")
        }
        
        if (historicalRaces.isEmpty()) {
            println("⚠️ Law 2b: No historical races available after filtering today's races - returning 0.0")
            return 0.0
        }
        
        // CRITICAL FIX: Sort historical races by date (most recent first) because last5Races may not be in date order
        val sortedHistoricalRaces = historicalRaces.sortedByDescending { it.date }
        
        // FILE LOGGING - after sorting
        try {
            val logFile = File("/sdcard/Download/SECOND_UP_DEBUG_${horseForm.horseId.replace(" ", "_")}.txt")
            val writer = FileWriter(logFile, true) // Append mode
            writer.appendLine("\nSorted historical races (most recent first):")
            sortedHistoricalRaces.forEachIndexed { index, race ->
                writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
            }
            writer.appendLine("\nLogic check:")
            writer.appendLine("  sortedHistoricalRaces.size = ${sortedHistoricalRaces.size}")
            writer.appendLine("  Needs >= 2 races? ${sortedHistoricalRaces.size >= 2}")
            writer.appendLine("  Will use: ${if (sortedHistoricalRaces.size >= 2) "sortedHistoricalRaces[1]" else "sortedHistoricalRaces.firstOrNull()"}")
            writer.close()
        } catch (e: Exception) {
            println("⚠️ Could not write to debug file: ${e.message}")
        }
        
        println("🏇 Law 2b: DEBUG - All historical races (UNSORTED): ${historicalRaces.map { "Pos:${it.position} Date:${it.date}" }}")
        println("🏇 Law 2b: DEBUG - All historical races (SORTED by date): ${sortedHistoricalRaces.map { "Pos:${it.position} Date:${it.date}" }}")
        
        // For second up horses, we specifically look at their first-up run (SECOND most recent HISTORICAL race BY DATE)
        // The most recent race is their current 2nd up run, the second most recent is their 1st up run
        val firstUpRun = if (sortedHistoricalRaces.size >= 2) {
            sortedHistoricalRaces[1] // Second most recent race (first-up run)
        } else {
            sortedHistoricalRaces.firstOrNull() // Fallback to most recent if only one race
        }
        
        if (firstUpRun == null) {
            println("🏇 Law 2b: No first-up run data available - returning 0.0")
            println("🔍 Law 2b DEBUG: sortedHistoricalRaces.size = ${sortedHistoricalRaces.size}")
            return 0.0
        }
        
        println("🔍 Law 2b DEBUG: Found first-up run - Position: ${firstUpRun.position}, Date: ${firstUpRun.date}, Track: ${firstUpRun.track}")
        
        // FILE LOGGING for Miss Lola debugging
        if (horseForm.horseId.contains("MISS LOLA", ignoreCase = true)) {
            try {
                val logFile = File("/sdcard/Download/MISS_LOLA_DEBUG.txt")
                val writer = FileWriter(logFile, false) // Overwrite each time
                writer.appendLine("=" .repeat(80))
                writer.appendLine("MISS LOLA - Recent Form Bonus Debug")
                writer.appendLine("=" .repeat(80))
                writer.appendLine("Historical races count: ${historicalRaces.size}")
                writer.appendLine("Sorted historical races count: ${sortedHistoricalRaces.size}")
                writer.appendLine("\nAll Historical Races:")
                historicalRaces.forEachIndexed { index, race ->
                    writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
                }
                writer.appendLine("\nSorted Historical Races:")
                sortedHistoricalRaces.forEachIndexed { index, race ->
                    writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
                }
                writer.appendLine("\nFirst-Up Run Details:")
                writer.appendLine("  Position: ${firstUpRun.position}")
                writer.appendLine("  Date: ${firstUpRun.date}")
                writer.appendLine("  Track: ${firstUpRun.track}")
                writer.appendLine("  Margin: ${firstUpRun.margin}")
                writer.close()
                println("📝 Wrote Miss Lola debug to /sdcard/Download/MISS_LOLA_DEBUG.txt")
            } catch (e: Exception) {
                println("⚠️ Could not write Miss Lola debug file: ${e.message}")
            }
        }
        
        var score = 0.0
        
        println("🏇 Law 2b: Analyzing first-up run (MOST RECENT BY DATE) - Position: ${firstUpRun.position}, Margin: ${firstUpRun.margin}, Date: ${firstUpRun.date}")
        
        // Award bonus points based on first-up performance
        val positionBonus = when (firstUpRun.position) {
            1 -> 8.0 // 1st place first up = 8 points
            2 -> 5.0 // 2nd place first up = 5 points  
            3 -> 3.0 // 3rd place first up = 3 points
            4 -> 2.0 // 4th place first up = 2 points
            else -> 0.0
        }
        
        score += positionBonus
        if (positionBonus > 0) {
            println("🏇 Law 2b: +${String.format("%.1f", positionBonus)} points for ${firstUpRun.position}${getPositionSuffix(firstUpRun.position)} place first-up")
        } else {
            println("⚠️ Law 2b: 0.0 points for ${firstUpRun.position}${getPositionSuffix(firstUpRun.position)} place first-up (position 5th or worse)")
        }
        
        // Additional bonus for being within 4 lengths of winner (regardless of position)
        firstUpRun.margin?.let { margin ->
            if (margin <= 4.0 && firstUpRun.position > 4) {
                score += 1.0 // Small bonus for competitive first-up run
                println("🏇 Law 2b: +1.0 points for being within ${margin} lengths first-up (competitive effort)")
            }
        }
        
        val finalScore = min(score, 8.0) // Cap at 8 points (same as other spell horse laws)
        println("🏇 Law 2b: Second Up Recent Form Bonus: ${String.format("%.1f", finalScore)} points (capped at 8.0)")
        return finalScore
    }
    
    /**
     * Law 3: Class Suitability (25 points)
     * Compares current race class to horse's past races
     */
    private fun calculateClassSuitabilityScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        // Filter out races from today
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, race.date)
        if (historicalRaces.isEmpty()) return 0.0
        
        var score = 0.0
        val currentClass = parseRaceClass(race.raceClass)
        val last3Classes = historicalRaces.take(3).mapNotNull { it.raceClass?.let { parseRaceClass(it) } }
        
        if (last3Classes.isNotEmpty()) {
            val classComparison = compareClasses(currentClass, last3Classes)
            
            when (classComparison) {
                ClassComparison.DROPPING -> {
                    score += 15.0 // Dropping in class bonus
                    
                    // ENHANCED: Check for declining horse pattern (repeatedly dropping class)
                    val consecutiveClassDrops = countConsecutiveClassDrops(historicalRaces)
                    if (consecutiveClassDrops >= 3) {
                        score -= 5.0 // Warning: this horse is in decline, not just tactically dropping
                        println("⚠️ ${horse.name} - Declining pattern detected (${consecutiveClassDrops} consecutive class drops), penalty applied")
                    }
                    
                    // Extra bonus if performed well last start
                    if (historicalRaces.isNotEmpty() && historicalRaces[0].position <= 3) {
                        score += 5.0 // Class drop + good last start
                    }
                }
                ClassComparison.SIMILAR -> {
                    // Check performance in similar classes (last 5 starts)
                    val goodPerformancesInClass = historicalRaces.count { race ->
                        race.position <= 3 && race.raceClass?.let { isSimilarClass(parseRaceClass(it), currentClass) } == true
                    }
                    score += goodPerformancesInClass * 3.0
                }
                ClassComparison.RISING -> {
                    // Rising in class - check if horse has form in lesser classes
                    val hasFormInLesserClasses = historicalRaces.any { race ->
                        race.position <= 2 && race.raceClass?.let { isLowerClass(parseRaceClass(it), currentClass) } == true
                    }
                    if (hasFormInLesserClasses) {
                        score += 10.0
                    } else {
                        score -= 5.0 // Penalty for rising without form
                    }
                }
            }
        }
        
        return min(max(score, 0.0), CLASS_SUITABILITY_WEIGHT)
    }
    
    /**
     * Law 4: Distance Success (8 points)
     * Awards points for winning or placing at this distance previously
     */
    private fun calculateDistanceSuccessScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        val trackDistanceStats = horseForm.trackDistanceStats
        
        if (trackDistanceStats == null) {
            println("🏇 Law 4: No distance statistics available for ${horse.name}")
            return 0.0
        }
        
        val distanceStats = trackDistanceStats.distanceStats
        
        if (distanceStats.runs == 0) {
            println("🏇 Law 4: No distance history available for ${horse.name}")
            return 0.0
        }
        
        var score = 0.0
        
        // Win rate bonus (0-4 points)
        val winRate = distanceStats.wins.toDouble() / distanceStats.runs
        score += winRate * 4.0
        
        // Place rate bonus (0-4 points)
        val placeRate = distanceStats.places.toDouble() / distanceStats.runs
        score += placeRate * 4.0
        
        println("🏇 Law 4 Distance (${horse.name}): ${distanceStats.runs} runs, ${distanceStats.wins} wins, ${distanceStats.places} places, WinRate=${String.format("%.2f", winRate)}, PlaceRate=${String.format("%.2f", placeRate)}, Score=${String.format("%.1f", score)}")
        
        return min(score, DISTANCE_SUCCESS_WEIGHT)
    }
    
    /**
     * Law 5: Track Success (8 points)
     * Awards points for winning or placing at this track previously
     */
    private fun calculateTrackSuccessScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        val trackDistanceStats = horseForm.trackDistanceStats
        
        if (trackDistanceStats == null) {
            println("🏇 Law 5: No track statistics available for ${horse.name}")
            return 0.0
        }
        
        val trackStats = trackDistanceStats.trackStats
        
        if (trackStats.runs == 0) {
            println("🏇 Law 5: No track history available for ${horse.name}")
            return 0.0
        }
        
        var score = 0.0
        
        // Win rate bonus (0-4 points)
        val winRate = trackStats.wins.toDouble() / trackStats.runs
        score += winRate * 4.0
        
        // Place rate bonus (0-4 points)
        val placeRate = trackStats.places.toDouble() / trackStats.runs
        score += placeRate * 4.0
        
        println("🏇 Law 5 Track (${horse.name}): ${trackStats.runs} runs, ${trackStats.wins} wins, ${trackStats.places} places, WinRate=${String.format("%.2f", winRate)}, PlaceRate=${String.format("%.2f", placeRate)}, Score=${String.format("%.1f", score)}")
        
        return min(score, TRACK_SUCCESS_WEIGHT)
    }
    
    /**
     * Law 6: Track+Distance Combined (9 points)
     * Awards points for winning or placing at this track AND distance combination
     * Includes distance pattern bonus for comfort zone
     */
    private fun calculateTrackDistanceCombinedScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        val trackDistanceStats = horseForm.trackDistanceStats
        
        if (trackDistanceStats == null) {
            println("🏇 Law 6: No track/distance statistics available for ${horse.name}")
            return 0.0
        }
        
        val trackStats = trackDistanceStats.trackStats
        val distanceStats = trackDistanceStats.distanceStats
        val combinedStats = trackDistanceStats.combinedStats
        
        var score = 0.0
        
        // Only award points if horse has actually run at this specific track+distance combination
        if (combinedStats.runs > 0) {
            // Win rate bonus (0-2 points)
            val combinedWinRate = combinedStats.wins.toDouble() / combinedStats.runs
            score += combinedWinRate * 2.0
            
            // Place rate bonus (0-2 points)
            val combinedPlaceRate = combinedStats.places.toDouble() / combinedStats.runs
            score += combinedPlaceRate * 2.0
            
            println("🏇 Law 6 Combined (${horse.name}): ${combinedStats.runs} runs at track+distance, ${combinedStats.wins} wins, ${combinedStats.places} places, Score=${String.format("%.1f", score)}")
        } else {
            println("🏇 Law 6 Combined (${horse.name}): No history at this specific track+distance combination")
        }
        
        // Distance Pattern Bonus (0-5 points) - comfort zone or previous wins at distance
        val distancePatternBonus = calculateDistancePatternBonus(horse, race, horseForm)
        score += distancePatternBonus
        
        // DEBUG for Banyan Jenni
        if (horse.name.contains("BANYAN JENNI", ignoreCase = true)) {
            println("🚨 BANYAN JENNI DEBUG:")
            println("🚨   Combined Stats: ${combinedStats.runs} runs, ${combinedStats.wins} wins")
            println("🚨   Base Combined Score: ${String.format("%.1f", score - distancePatternBonus)}")
            println("🚨   Distance Pattern Bonus: ${String.format("%.1f", distancePatternBonus)}")
            println("🚨   TOTAL Track+Distance Combined: ${String.format("%.1f", score)}")
            
            // FILE LOGGING for Banyan Jenni debug
            try {
                val logFile = File("/sdcard/Download/BANYAN_JENNI_DEBUG.txt")
                val writer = FileWriter(logFile, false) // Overwrite to start fresh
                writer.appendLine("🔍 BANYAN JENNI - Track/Distance Debug (NEW BUILD)")
                writer.appendLine("Current Race: ${race.venue} ${race.distance}m")
                writer.appendLine("Combined Stats: ${combinedStats.runs} runs, ${combinedStats.wins} wins")
                writer.appendLine("Base Combined Score: ${String.format("%.1f", score - distancePatternBonus)}")
                writer.appendLine("Distance Pattern Bonus: ${String.format("%.1f", distancePatternBonus)}")
                writer.appendLine("TOTAL Track+Distance Combined: ${String.format("%.1f", score)}")
                writer.appendLine("")
                writer.appendLine("DETAILED DISTANCE PATTERN DEBUG:")
                val historicalRaces = filterHistoricalRaces(horseForm.last5Races, race.date)
                val distances = historicalRaces.mapNotNull { it.distance }
                if (distances.isNotEmpty()) {
                    val averageRecentDistance = distances.average()
                    val distanceChange = kotlin.math.abs(race.distance - averageRecentDistance)
                    writer.appendLine("Historical Distances: ${distances.joinToString(", ")}m")
                    writer.appendLine("Average Distance: ${String.format("%.1f", averageRecentDistance)}m")
                    writer.appendLine("Distance Change: ${String.format("%.1f", distanceChange)}m")
                    writer.appendLine("Has Similar Distance History: ${historicalRaces.any { it.distance != null && kotlin.math.abs(it.distance - race.distance) <= 200 }}")
                } else {
                    writer.appendLine("No historical distances found")
                }
                writer.close()
            } catch (e: Exception) {
                println("⚠️ Could not write to Banyan Jenni debug file: ${e.message}")
            }
        }
        
        println("🏇 Law 6 Total (${horse.name}): Combined=${String.format("%.1f", score - distancePatternBonus)}, Pattern=${String.format("%.1f", distancePatternBonus)}, Total=${String.format("%.1f", score)}")
        
        return min(score, TRACK_DISTANCE_COMBINED_WEIGHT)
    }
    
    /**
     * Law 7: Track Condition Success (8 points)
     * Awards points for winning or placing on this track condition previously
     */
    private fun calculateTrackConditionSuccessScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        val trackDistanceStats = horseForm.trackDistanceStats
        
        if (trackDistanceStats == null || trackDistanceStats.conditionStats == null) {
            println("🏇 Law 7: No track condition statistics available for ${horse.name}")
            return 0.0
        }
        
        val conditionStats = trackDistanceStats.conditionStats
        
        if (conditionStats.runs == 0) {
            println("🏇 Law 7: No track condition history available for ${horse.name}")
            return 0.0
        }
        
        var score = 0.0
        
        // Win rate bonus (0-4 points)
        val winRate = conditionStats.wins.toDouble() / conditionStats.runs
        score += winRate * 4.0
        
        // Place rate bonus (0-4 points)
        val placeRate = (conditionStats.wins + conditionStats.seconds).toDouble() / conditionStats.runs
        score += placeRate * 4.0
        
        println("🏇 Law 7 Track Condition (${horse.name}): ${conditionStats.runs} runs, ${conditionStats.wins} wins, ${conditionStats.seconds} places, WinRate=${String.format("%.2f", winRate)}, PlaceRate=${String.format("%.2f", placeRate)}, Score=${String.format("%.1f", score)}")
        
        return min(score, 8.0) // Cap at 8 points
    }
    
    
    /**
     * Law 8: Finishing Speed Bonus (8 points)
     * Rewards horses with the fastest last 600m times from their last start
     */
    private fun calculateSectionalTimeScore(horseForm: HorseForm, analysisDate: Date): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        // Filter out races from today
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, analysisDate)
        if (historicalRaces.isEmpty()) return 0.0
        
        // Get the last start (most recent HISTORICAL race)
        val lastStart = historicalRaces[0]
        val lastStartSectional = lastStart.sectionalTime
        
        if (lastStartSectional == null) {
            println("🏇 No sectional time data available for last start")
            return 0.0
        }
        
        println("🏇 Last start 600m sectional: ${lastStartSectional}s")
        
        // For now, we'll use a simple scoring system based on the sectional time
        // In the future, this could be enhanced to compare against the field's average
        var score = 0.0
        
        // Score based on finishing speed (lower time = faster = higher score)
        when {
            lastStartSectional <= 33.0 -> score = 8.0  // Very fast finishing
            lastStartSectional <= 34.0 -> score = 6.0  // Fast finishing  
            lastStartSectional <= 35.0 -> score = 4.0  // Good finishing
            lastStartSectional <= 36.0 -> score = 2.0  // Average finishing
            else -> score = 0.0                        // Slow finishing
        }
        
        println("🏇 Finishing speed score: $score (${lastStartSectional}s)")
        return score
    }
    
    /**
     * Law 8: Barrier (6 points) - ENHANCED: Distance-Aware
     * Points based on barrier position relative to race distance
     * Short races favor inside barriers more than longer races
     */
    private fun calculateBarrierScore(horse: Horse, race: Race): Double {
        val distance = race.distance
        val barrier = horse.barrier
        
        return when {
            // Short races (≤1200m) - tight turning tracks, inside barriers are crucial
            distance <= 1200 -> {
                when {
                    barrier in 1..4 -> 6.0  // Inside barriers are gold
                    barrier in 5..8 -> 3.0  // Mid barriers acceptable
                    else -> 0.0             // Wide barriers disadvantaged
                }
            }
            // Medium/Long races (>1200m) - straighter tracks, barriers less critical
            else -> {
                when {
                    barrier in 1..6 -> 4.0  // Inside still good but not crucial
                    barrier in 7..12 -> 2.0 // Mid barriers fine
                    else -> 0.0             // Very wide barriers slight disadvantage
                }
            }
        }
    }
    
    /**
     * Law 9: Jockey (8 points)
     * Points based on jockey's premiership ranking
     */
    private fun calculateJockeyScore(horse: Horse, jockeyRankings: List<JockeyPremiership>): Double {
        println("🏇 DEBUG: Looking for jockey '${horse.jockey}' in ${jockeyRankings.size} rankings")
        
        // CHAMPION JOCKEY CHECK: Check if this is a champion jockey first
        val isChampionJockey = CHAMPION_JOCKEYS.any { championName ->
            horse.jockey.equals(championName, ignoreCase = true) || 
            horse.jockey.contains(championName, ignoreCase = true) ||
            championName.contains(horse.jockey, ignoreCase = true)
        }
        
        if (isChampionJockey) {
            println("🏆 CHAMPION JOCKEY DETECTED: '${horse.jockey}' receives maximum points!")
            println("🏇 DEBUG: Champion jockey score: $JOCKEY_WEIGHT points (maximum)")
            return JOCKEY_WEIGHT
        }
        
        // Regular premiership-based scoring
        val jockeyRank = getJockeyRank(horse.jockey, jockeyRankings)
        println("🏇 DEBUG: Jockey '${horse.jockey}' rank: $jockeyRank (max rank: ${jockeyRankings.maxOfOrNull { it.rank } ?: 0})")
        
        val score = when {
            jockeyRank in 1..5 -> JOCKEY_WEIGHT
            jockeyRank in 6..10 -> 5.0
            jockeyRank in 11..20 -> 2.0
            else -> 0.0
        }
        
        println("🏇 DEBUG: Regular jockey score for '${horse.jockey}': $score points")
        return score
    }
    
    /**
     * Law 10: Trainer (8 points)
     * Points based on trainer's premiership ranking
     */
    private fun calculateTrainerScore(horse: Horse, trainerRankings: List<TrainerPremiership>): Double {
        println("👨‍🏫 DEBUG: Looking for trainer '${horse.trainer}' in ${trainerRankings.size} rankings")
        val trainerRank = getTrainerRank(horse.trainer, trainerRankings)
        println("👨‍🏫 DEBUG: Trainer '${horse.trainer}' rank: $trainerRank (max rank: ${trainerRankings.maxOfOrNull { it.rank } ?: 0})")
        
        val score = when {
            trainerRank in 1..5 -> TRAINER_WEIGHT
            trainerRank in 6..10 -> 5.0
            trainerRank in 11..20 -> 2.0
            else -> 0.0
        }
        
        println("👨‍🏫 DEBUG: Trainer score for '${horse.trainer}': $score points")
        return score
    }
    
    /**
     * Special scoring for horses returning from spell (12+ weeks)
     * BALANCED APPROACH: Spell horses get some bonuses but also penalties for lack of recent form
     * This prevents them from unfairly scoring higher than horses with good recent performance
     */
    private fun calculateSpellHorseScore(
        horse: Horse,
        race: Race,
        horseForm: HorseForm,
        jockeyRankings: List<JockeyPremiership>,
        trainerRankings: List<TrainerPremiership>
    ): Double {
        var score = 0.0
        
        // Class suitability (use historical class performance)
        val classSuitabilityScore = calculateClassSuitabilityScore(horse, race, horseForm)
        score += classSuitabilityScore
        
        // Track/distance suitability (use historical track/distance performance)
        val distanceSuccess = calculateDistanceSuccessScore(horse, race, horseForm)
        val trackSuccess = calculateTrackSuccessScore(horse, race, horseForm)
        val trackDistanceCombined = calculateTrackDistanceCombinedScore(horse, race, horseForm)
        val trackDistanceScore = distanceSuccess + trackSuccess + trackDistanceCombined
        score += trackDistanceScore
        
        // Track condition suitability (use historical track condition performance)
        val trackConditionScore = calculateTrackConditionScore(horse, race, horseForm)
        score += trackConditionScore
        
        // Focus on trainer premiership (reduced multiplier for balance)
        val trainerScore = calculateTrainerScore(horse, trainerRankings) * 1.5
        score += trainerScore
        
        // Jockey premiership (reduced multiplier for balance)
        val jockeyScore = calculateJockeyScore(horse, jockeyRankings) * 1.2
        score += jockeyScore
        
        // First, second, third up results (reduced bonus)
        val upResults = horseForm.upResults
        var upScore = 0.0
        if (upResults.wins > 0 || upResults.places > 0) {
            upScore = 8.0 // Reduced from 15.0 for balance
            score += upScore
        }
        
        // Jockey/trainer combination success
        val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, race, horseForm)
        val jockeyTrainerPartnership = 0.0 // Removed jockey-trainer partnership scoring
        val combinationScore = jockeyHorseRelationship + jockeyTrainerPartnership
        score += combinationScore
        
        // Barrier still applies
        val barrierScore = calculateBarrierScore(horse, race)
        score += barrierScore
        
        // NEW: Penalty for lack of recent form (spell horses should not score too high)
        val formPenalty = 5.0
        score -= formPenalty
        println("🏇 Spell Horse Form Penalty: -$formPenalty points (lack of recent form)")
        
        // COMPREHENSIVE SPELL HORSE SCORING LOGGING
        println("🏆 ${horse.name} SPELL HORSE SCORING BREAKDOWN:")
        println("   🏆 Class Suitability: ${String.format("%.1f", classSuitabilityScore)} points")
        println("   🏁 Track/Distance History: ${String.format("%.1f", trackDistanceScore)} points")
        println("   🌧️ Track Condition History: ${String.format("%.1f", trackConditionScore)} points")
        println("   👨‍🏫 Trainer Premiership (x1.5): ${String.format("%.1f", trainerScore)} points")
        println("   🏇 Jockey Premiership (x1.2): ${String.format("%.1f", jockeyScore)} points")
        println("   📈 Up Results: ${String.format("%.1f", upScore)} points")
        println("   🤝 Jockey-Horse Relationship: ${String.format("%.1f", jockeyHorseRelationship)} points")
        // Removed jockey-trainer partnership logging
        println("   🔗 Law 8 Total: ${String.format("%.1f", combinationScore)} points")
        println("   🚪 Barrier: ${String.format("%.1f", barrierScore)} points")
        println("   ⚠️ Form Penalty: -${String.format("%.1f", formPenalty)} points")
        println("   💯 TOTAL SCORE: ${String.format("%.1f", score)} points")
        println("   ─".repeat(50))
        
        return min(score, TOTAL_POINTS)
    }
    
    /**
     * Special scoring for first-up horses (first starters)
     * Enhanced to consider trial performance, trainer/jockey quality, and barrier
     */
    private fun calculateFirstUpHorseScore(
        horse: Horse,
        race: Race,
        horseForm: HorseForm,
        jockeyRankings: List<JockeyPremiership>,
        trainerRankings: List<TrainerPremiership>
    ): Double {
        var score = 0.0
        
        // 1. JOCKEY PREMIERSHIP (15 points max)
        val jockeyScore = calculateJockeyScore(horse, jockeyRankings)
        score += jockeyScore
        println("🏇 First Starter ${horse.name} - Jockey Score: $jockeyScore")
        
        // 2. TRAINER PREMIERSHIP (15 points max) 
        val trainerScore = calculateTrainerScore(horse, trainerRankings)
        score += trainerScore
        println("🏇 First Starter ${horse.name} - Trainer Score: $trainerScore")
        
        // 3. TRIAL PERFORMANCE - REMOVED (was giving fake 5.0 points)
        // No trial performance score - only real trial sectional times are scored
        
        // 4. BARRIER POSITION (6 points max)
        val barrierScore = calculateBarrierScore(horse, race) // UPDATED: now distance-aware
        score += barrierScore
        println("🏇 First Starter ${horse.name} - Barrier Score: $barrierScore")
        
        // 5. TRIAL SECTIONAL TIMES (10 points max)
        val sectionalScore = calculateTrialSectionalScore(horseForm, race)
        score += sectionalScore
        println("🏇 First Starter ${horse.name} - Sectional Score: $sectionalScore")
        
        // 6. JOCKEY-TRAINER COMBINATION SUCCESS (4 points max)
        val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, race, horseForm)
        val jockeyTrainerPartnership = 0.0 // Removed jockey-trainer partnership scoring
        val combinationScore = (jockeyHorseRelationship + jockeyTrainerPartnership) * 0.5
        score += combinationScore
        println("🏇 First Starter ${horse.name} - Combination Score: $combinationScore")
        
        // 7. WEIGHT ADVANTAGE (8 points max) - NEW LAW 11
        val weightAdvantageScore = calculateWeightAdvantageScore(horse, race.horses)
        score += weightAdvantageScore
        println("🏇 First Starter ${horse.name} - Weight Advantage Score: $weightAdvantageScore")
        
        println("🏇 First Starter ${horse.name} - TOTAL SCORE: $score")
        
        // Cap first starters at reasonable level but allow good ones to score well
        return min(score, 58.0) // Increased cap from 50 to 58 (added 8 for weight advantage)
    }
    
    /**
     * Resolve ties using win/place ratio
     */
    fun resolveTies(horses: List<ScoredHorse>): List<ScoredHorse> {
        // Log the final ranking before resolving ties
        println("🏆 FINAL HORSE RANKING (before tie resolution):")
        horses.sortedByDescending { it.score }.forEachIndexed { index, horse ->
            println("   ${index + 1}. ${horse.horse.name} - ${String.format("%.1f", horse.score)} points")
        }
        println("   ─".repeat(50))
        
        return horses.map { horse ->
            val sameScoreHorses = horses.filter { it.score == horse.score }
            
            if (sameScoreHorses.size > 1) {
                // Only mark as standout if there are actual wins/places to compare
                val horseWins = horse.horse.wins
                val horsePlaces = horse.horse.places
                val horseTotal = horseWins + horsePlaces
                
                val bestTotal = sameScoreHorses.maxOf { h -> h.horse.wins + h.horse.places }
                
                // Only mark as standout if this horse has the best win/place record AND it's not 0/0
                val isStandout = horseTotal == bestTotal && horseTotal > 0
                
                horse.copy(isStandout = isStandout)
            } else {
                horse.copy(isStandout = false)
            }
        }
    }
    
    // Helper methods
    private fun isReturningFromSpell(horse: Horse, horseForm: HorseForm, analysisDate: Date): Boolean {
        // Only mark as spell if we have actual form data showing a gap
        if (horseForm.last5Races.isEmpty()) return false // Don't assume spell if no data
        
        // CRITICAL FIX: Filter out races from today to avoid using today's results
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, analysisDate)
        
        if (historicalRaces.isEmpty()) return false
        
        // Sort by date to get the most recent historical race
        val sortedHistoricalRaces = historicalRaces.sortedByDescending { it.date }
        val lastRace = sortedHistoricalRaces.firstOrNull()
        
        lastRace?.date?.let { lastRaceDate ->
            // Add detailed debug logging for date calculation
            println("🔍 SPELL DEBUG: analysisDate = $analysisDate (${analysisDate.time})")
            println("🔍 SPELL DEBUG: lastRaceDate = $lastRaceDate (${lastRaceDate.time})")
            
            val timeDifferenceMs = analysisDate.time - lastRaceDate.time
            val timeDifferenceDays = timeDifferenceMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
            val weeksAgo = timeDifferenceDays / 7.0
            
            println("🔍 SPELL DEBUG: Time difference = ${timeDifferenceMs}ms = ${timeDifferenceDays} days = ${weeksAgo} weeks")
            println("🔍 Spell check: Last race was ${weeksAgo} weeks ago (threshold: ${SPELL_THRESHOLD_WEEKS})")
            return weeksAgo >= SPELL_THRESHOLD_WEEKS
        }
        
        // If we have recent races but can't parse the date, this is a parsing error
        // For now, treat as not a spell horse but log the issue
        println("⚠️ CRITICAL: Horse ${horse.name} has ${historicalRaces.size} historical races but last race date is null - this is a parsing error!")
        return false // Default to not spell if we can't determine
    }
    
    private fun isFirstUpHorse(historicalRaces: List<RaceResultDetail>): Boolean {
        // Only mark as first-up if we have no historical races at all
        // This means the horse has never raced before today
        if (historicalRaces.isEmpty()) {
            return true // No historical races = true first starter
        }
        return false // Default to not first-up if we can't determine
    }
    
    /**
     * Get only races that occurred AFTER the most recent spell (X)
     * Example: Form "2,3,X,1,3" should only return races corresponding to "1,3"
     */
    private fun getRacesAfterMostRecentSpell(
        historicalRaces: List<RaceResultDetail>, 
        formString: String, 
        analysisDate: Date
    ): List<RaceResultDetail> {
        if (historicalRaces.isEmpty()) {
            return historicalRaces
        }
        
        // Filter out today's races from form string first
        val todayRaceCount = historicalRaces.size - filterHistoricalRaces(historicalRaces, analysisDate).size
        val historicalForm = if (todayRaceCount > 0 && formString.length > todayRaceCount) {
            formString.substring(todayRaceCount)
        } else {
            formString
        }
        
        println("🔍 SPELL FILTER: Original form='$formString', Historical form='$historicalForm'")
        
        // Find the position of the most recent X in the form string
        val lastXIndex = historicalForm.lastIndexOf('X')
        val lastXIndexLower = historicalForm.lastIndexOf('x')
        val mostRecentSpellIndex = maxOf(lastXIndex, lastXIndexLower)
        
        if (mostRecentSpellIndex == -1) {
            println("🔍 SPELL FILTER: No X found in form - using all ${historicalRaces.size} races")
            return historicalRaces
        }
        
        // Count races after the most recent X
        val racesAfterSpell = historicalForm.substring(mostRecentSpellIndex + 1).length
        println("🔍 SPELL FILTER: Most recent X at position $mostRecentSpellIndex, races after spell: $racesAfterSpell")
        
        // Return only the most recent N races (where N = races after spell)
        val filteredRaces = historicalRaces.takeLast(racesAfterSpell)
        println("🔍 SPELL FILTER: Using ${filteredRaces.size} races after spell (from ${historicalRaces.size} total)")
        
        return filteredRaces
    }
    
    private fun parseRaceClass(classStr: String): Int {
        return classStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
    }
    
    private fun compareClasses(current: Int, previous: List<Int>): ClassComparison {
        val avgPrevious = previous.average()
        return when {
            current < avgPrevious -> ClassComparison.DROPPING
            current > avgPrevious -> ClassComparison.RISING
            else -> ClassComparison.SIMILAR
        }
    }
    
    private fun isSimilarClass(class1: Int, class2: Int): Boolean {
        return abs(class1 - class2) <= 1
    }
    
    private fun isLowerClass(class1: Int, class2: Int): Boolean {
        return class1 < class2
    }
    
    /**
     * Count consecutive class drops in recent races
     * Returns number of races where class decreased from previous race
     */
    private fun countConsecutiveClassDrops(races: List<RaceResultDetail>): Int {
        if (races.size < 2) return 0
        
        var consecutiveDrops = 0
        for (i in 0 until races.size - 1) {
            val currentClass = races[i].raceClass?.let { parseRaceClass(it) } ?: 0
            val previousClass = races[i + 1].raceClass?.let { parseRaceClass(it) } ?: 0
            
            if (currentClass > 0 && previousClass > 0 && currentClass < previousClass) {
                consecutiveDrops++
            } else {
                break // Stop counting if pattern breaks
            }
        }
        
        return consecutiveDrops
    }
    
    /**
     * Calculate distance pattern bonus for Track/Distance law
     * Rewards horses staying within their comfort zone or having won at this exact distance
     */
    private fun calculateDistancePatternBonus(horse: Horse, race: Race, horseForm: HorseForm): Double {
        val raceDistance = race.distance
        
        // CRITICAL FIX: Filter out races from today to avoid using today's results
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, race.date)
        val recentDistances = historicalRaces.mapNotNull { it.distance }
        
        if (recentDistances.isEmpty()) return 0.0
        
        val averageRecentDistance = recentDistances.average()
        val distanceChange = kotlin.math.abs(raceDistance - averageRecentDistance)
        
        // CRITICAL FIX: Only give comfort zone bonus if horse has actually raced at similar distances
        val hasSimilarDistanceHistory = historicalRaces.any { 
            it.distance != null && kotlin.math.abs(it.distance - raceDistance) <= 200 
        }
        
        // Bonus for staying within comfort zone (±200m) AND having raced at similar distances before
        if (distanceChange <= 200 && hasSimilarDistanceHistory) {
            println("🏁 ${horse.name} - Distance comfort zone bonus (+3.0 points): ${raceDistance}m vs avg ${String.format("%.0f", averageRecentDistance)}m")
            return 3.0
        } else if (distanceChange <= 200) {
            println("🏁 ${horse.name} - No similar distance history (${raceDistance}m), no comfort zone bonus")
        }
        
        // Check if horse has won at this exact distance before (±50m tolerance)
        val hasWonAtDistance = historicalRaces.any { 
            it.position == 1 && it.distance != null && kotlin.math.abs(it.distance - raceDistance) <= 50 
        }
        
        if (hasWonAtDistance) {
            println("🏁 ${horse.name} - Won at this distance before (+5.0 points): ${raceDistance}m")
            return 5.0
        }
        
        return 0.0
    }
    
    /**
     * Normalize names for matching by removing common prefixes and suffixes
     * Handles cases like "Ms Jamie Melham" vs "Jamie Melham" and "Mick Price & Michael Kent jnr" vs "Mick Price & Michael Kent"
     */
    private fun normalizeNameForMatching(name: String): String {
        return name.trim()
            // Remove common prefixes
            .replace(Regex("^(Mr|Ms|Mrs|Miss|Dr|Prof)\\.?\\s+", RegexOption.IGNORE_CASE), "")
            // Remove common suffixes
            .replace(Regex("\\s+(jnr|snr|jr|sr|j\\.|s\\.)\\.?$", RegexOption.IGNORE_CASE), "")
            // Remove parenthetical content like (Jnr), (Snr), etc.
            .replace(Regex("\\s*\\([^)]*\\)", RegexOption.IGNORE_CASE), "")
            // Remove extra whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun getJockeyRank(jockeyName: String, rankings: List<JockeyPremiership>): Int {
        val found = rankings.find { 
            normalizeNameForMatching(it.name).equals(normalizeNameForMatching(jockeyName), ignoreCase = true) 
        }
        if (found != null) {
            println("🏇 DEBUG: Found jockey '${jockeyName}' (normalized: '${normalizeNameForMatching(jockeyName)}') at rank ${found.rank}")
        } else {
            println("🏇 DEBUG: Jockey '${jockeyName}' (normalized: '${normalizeNameForMatching(jockeyName)}') NOT FOUND in rankings")
            // Show first few ranking names for debugging
            println("🏇 DEBUG: Available jockeys (first 10): ${rankings.take(10).map { "${it.name} (normalized: ${normalizeNameForMatching(it.name)})" }}")
        }
        return found?.rank ?: Int.MAX_VALUE
    }
    
    // TRIAL PERFORMANCE SCORE FUNCTION REMOVED
    // Was giving fake 5.0 points regardless of actual trial performance
    
    /**
     * Calculate trial sectional time score for first starters
     * Compares trial times to race distance benchmarks
     */
    private fun calculateTrialSectionalScore(horseForm: HorseForm, race: Race): Double {
        var score = 0.0
        
        if (horseForm.trialSectionalTimes.isNotEmpty()) {
            val averageTrialTime = horseForm.trialSectionalTimes.average()
            
            // For now, use a simple scoring system based on trial performance
            // In the future, this could be enhanced with real trial benchmarks
            score = when {
                averageTrialTime <= 33.0 -> 10.0  // Very fast trial
                averageTrialTime <= 34.0 -> 8.0   // Fast trial
                averageTrialTime <= 35.0 -> 6.0   // Good trial
                averageTrialTime <= 36.0 -> 4.0   // Average trial
                else -> 2.0                       // Slow trial
            }
            
            println("🏇 Trial Sectional: Average=$averageTrialTime, Score=$score")
        }
        
        return score
    }
    
    private fun getTrainerRank(trainerName: String, rankings: List<TrainerPremiership>): Int {
        println("👨‍🏫 DEBUG: Searching for trainer: '$trainerName'")
        println("👨‍🏫 DEBUG: Normalized search name: '${normalizeNameForMatching(trainerName)}'")
        
        val found = rankings.find { 
            val normalizedRankingName = normalizeNameForMatching(it.name)
            val normalizedSearchName = normalizeNameForMatching(trainerName)
            val matches = normalizedRankingName.equals(normalizedSearchName, ignoreCase = true)
            println("👨‍🏫 DEBUG: Comparing '${it.name}' (normalized: '$normalizedRankingName') with '$trainerName' (normalized: '$normalizedSearchName') = $matches")
            matches
        }
        
        if (found != null) {
            println("👨‍🏫 DEBUG: Found trainer '${trainerName}' (normalized: '${normalizeNameForMatching(trainerName)}') at rank ${found.rank}")
        } else {
            println("👨‍🏫 DEBUG: Trainer '${trainerName}' (normalized: '${normalizeNameForMatching(trainerName)}') NOT FOUND in rankings")
            // Show first few ranking names for debugging
            println("👨‍🏫 DEBUG: Available trainers (first 10): ${rankings.take(10).map { "${it.name} (normalized: ${normalizeNameForMatching(it.name)})" }}")
        }
        return found?.rank ?: Int.MAX_VALUE
    }
    
    /**
     * Calculate combination score for normal horses
     * This score is based on how well the jockey and trainer have performed together
     * in races at this venue in the past.
     */
    private fun calculateCombinationScore(jockeyName: String, trainerName: String, venue: String): Double {
        // For now, this is a placeholder that will be enhanced with real data
        // In the future, this will use actual combination statistics
        
        if (jockeyName.isEmpty() || trainerName.isEmpty()) {
            return 0.0
        }
        
        // TODO: When we have real combination data, this will:
        // 1. Look up the jockey-trainer combination statistics
        // 2. Check their performance at this specific venue
        // 3. Consider recent form together (last 10 starts)
        // 4. Factor in win rate, place rate, and class performance
        
        // Placeholder scoring logic (will be replaced with real data):
        // - Base score for having both jockey and trainer
        // - Bonus for common combinations (frequently work together)
        // - Venue-specific performance bonus
        
        var score = 0.0
        
        // Base score for having both jockey and trainer
        score += 2.0
        
        // TODO: Enhanced scoring with real data:
        // val combination = findCombination(jockeyName, trainerName)
        // if (combination != null) {
        //     // Win rate bonus (0-3 points)
        //     score += min(combination.winPercentage / 20.0, 3.0)
        //     
        //     // Venue-specific bonus (0-2 points)
        //     val venueStats = combination.trackSpecificStats[venue]
        //     if (venueStats != null && venueStats.totalRuns >= 3) {
        //         score += min(venueStats.winPercentage / 25.0, 2.0)
        //     }
        //     
        //     // Recent form bonus (0-1 point)
        //     val recentWins = combination.recentForm.take(5).count { it.position == 1 }
        //     score += min(recentWins * 0.2, 1.0)
        // }
        
        println("🏇 Jockey-Trainer Combination: $jockeyName + $trainerName at $venue = $score points")
        
        return min(score, COMBINATION_WEIGHT)
    }
    
    /**
     * Calculate jockey-horse relationship score (4 points max)
     * Based on previous wins with this jockey on this horse
     */
    private fun calculateJockeyHorseRelationshipScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        println("🔍 LAW 11 DEBUG: Starting Jockey-Horse Relationship calculation for ${horse.name}")
        
        if (horseForm.last5Races.isEmpty()) {
            println("🔍 LAW 11 DEBUG: No historical races available for ${horse.name}")
            return 0.0
        }
        
        // CRITICAL FIX: Filter out races from today to avoid using today's results
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, race.date)
        
        if (historicalRaces.isEmpty()) {
            println("🔍 LAW 11 DEBUG: No historical races after filtering today's races for ${horse.name}")
            return 0.0
        }
        
        val currentJockey = horse.jockey
        if (currentJockey.isEmpty()) {
            println("🔍 LAW 11 DEBUG: No current jockey for ${horse.name}")
            return 0.0
        }
        
        println("🔍 LAW 11 DEBUG: Current jockey: '$currentJockey'")
        println("🔍 LAW 11 DEBUG: Historical races (${historicalRaces.size}):")
        
        historicalRaces.forEachIndexed { index, raceResult ->
            println("   Race ${index + 1}: Position=${raceResult.position}, Jockey='${raceResult.jockey}', Trainer='${raceResult.trainer}'")
        }
        
        // Look for previous wins with this jockey on this horse
        // Use normalized names for comparison to handle prefixes/suffixes
        val normalizedCurrentJockey = normalizeNameForMatching(currentJockey)
        println("🔍 LAW 11 DEBUG: Normalized current jockey: '$currentJockey' -> '$normalizedCurrentJockey'")
        
        val jockeyWins = historicalRaces.count { raceResult ->
            val historicalJockey = raceResult.jockey ?: ""
            val normalizedHistoricalJockey = normalizeNameForMatching(historicalJockey)
            val jockeyMatch = normalizedHistoricalJockey.equals(normalizedCurrentJockey, ignoreCase = true)
            val isWin = raceResult.position == 1
            println("🔍 LAW 11 DEBUG: Race jockey='$historicalJockey' (normalized: '$normalizedHistoricalJockey') matches current='$normalizedCurrentJockey'? $jockeyMatch, Position=${raceResult.position} is win? $isWin")
            jockeyMatch && isWin
        }
        
        val jockeyPlaces = historicalRaces.count { raceResult ->
            val historicalJockey = raceResult.jockey ?: ""
            val normalizedHistoricalJockey = normalizeNameForMatching(historicalJockey)
            val jockeyMatch = normalizedHistoricalJockey.equals(normalizedCurrentJockey, ignoreCase = true)
            val isPlace = raceResult.position in 2..3
            println("🔍 LAW 11 DEBUG: Race jockey='$historicalJockey' (normalized: '$normalizedHistoricalJockey') matches current='$normalizedCurrentJockey'? $jockeyMatch, Position=${raceResult.position} is place? $isPlace")
            jockeyMatch && isPlace
        }
        
        val score = when {
            jockeyWins >= 2 -> 4.0  // Multiple wins together
            jockeyWins == 1 -> 2.0  // One win together
            jockeyPlaces >= 2 -> 1.0  // Multiple places together
            jockeyPlaces == 1 -> 0.5  // One place together
            else -> 0.0  // No successful results together
        }
        
        println("🏇 Jockey-Horse Relationship: $currentJockey has ${jockeyWins} wins and ${jockeyPlaces} places with ${horse.name} = $score points")
        
        return score
    }
    
    /**
     * Calculate jockey-trainer partnership score (4 points max)
     * Based on previous wins with this jockey for this trainer
     */
    // Removed jockey-trainer partnership scoring - focusing only on jockey-horse relationship
    
    /**
     * Law 12: Track Condition Suitability (8 points)
     * Assesses horse's success on similar track conditions
     */
    private fun calculateTrackConditionScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        // CRITICAL FIX: Filter out races from today to avoid using today's results
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, race.date)
        
        if (historicalRaces.isEmpty()) return 0.0
        
        val currentCondition = race.trackCondition ?: return 0.0
        val conditionCategory = getTrackConditionCategory(currentCondition)
        
        if (conditionCategory == null) {
            println("🌦️ ${horse.name} - Unknown track condition: $currentCondition")
            return 0.0
        }
        
        var bestResult = 0.0
        
        // Check last 5 historical races for similar track conditions
        historicalRaces.forEach { raceResult ->
            val raceCondition = raceResult.trackCondition ?: return@forEach
            val raceConditionCategory = getTrackConditionCategory(raceCondition)
            
            if (raceConditionCategory == conditionCategory) {
                // Same condition category - score based on position
                val positionScore = when (raceResult.position) {
                    1 -> 8.0  // Win on similar condition
                    2 -> 5.0  // 2nd on similar condition  
                    3 -> 3.0  // 3rd on similar condition
                    else -> 0.0
                }
                
                // Take the highest score achieved
                if (positionScore > bestResult) {
                    bestResult = positionScore
                    println("🌦️ ${horse.name} - ${raceResult.position}${getPositionSuffix(raceResult.position)} on ${raceCondition} (${conditionCategory}) = ${positionScore} points")
                }
            }
        }
        
        println("🌦️ ${horse.name} - Track condition score: ${bestResult} points (${conditionCategory} conditions)")
        return bestResult
    }
    
    /**
     * Law 13: Weight Advantage (8 points)
     * Compares horse's weight to field average
     * Lower weight = advantage in handicap races
     */
    private fun calculateWeightAdvantageScore(horse: Horse, allHorses: List<Horse>): Double {
        val allWeights = allHorses.map { it.weight }.filter { it > 0.0 }
        
        if (allWeights.isEmpty() || horse.weight <= 0.0) {
            println("⚖️ ${horse.name} - No weight data available")
            return 0.0
        }
        
        val averageWeight = allWeights.average()
        val weightDifference = averageWeight - horse.weight
        
        println("⚖️ ${horse.name} - Weight: ${horse.weight}kg, Field Average: ${String.format("%.1f", averageWeight)}kg, Difference: ${String.format("%.1f", weightDifference)}kg")
        
        val score = when {
            weightDifference >= 4.0 -> 8.0  // Carrying 4kg+ less than average - huge advantage
            weightDifference >= 2.0 -> 5.0  // Carrying 2-3kg less - good advantage
            weightDifference >= 0.0 -> 2.0  // At or below average - slight advantage
            weightDifference >= -2.0 -> 0.0 // Slightly above average - neutral
            else -> -2.0                     // Penalty for carrying significant extra weight
        }
        
        println("⚖️ ${horse.name} - Weight advantage score: ${String.format("%.1f", score)} points")
        return max(score, 0.0) // Don't allow negative scores to affect total
    }
    
    /**
     * Law 14: Freshness (3 points)
     * Awards bonus for optimal time between runs
     * 2-4 weeks is ideal, backing up (<2 weeks) or too long (8+ weeks without being spell) penalized
     */
    private fun calculateFreshnessScore(horseForm: HorseForm, analysisDate: Date, horseName: String = "Unknown"): Double {
        // FILE LOGGING for debugging
        try {
            val logFile = File("/sdcard/Download/FRESHNESS_DEBUG.txt")
            val writer = FileWriter(logFile, true) // Append mode
            
            // Add searchable header for easy finding
            writer.appendLine("\n" + "=" .repeat(100))
            writer.appendLine("FRESHNESS CALCULATION DEBUG - $horseName")
            writer.appendLine("=" .repeat(100))
            writer.appendLine("Horse Name: $horseName")
            writer.appendLine("Horse ID: ${horseForm.horseId}")
            writer.appendLine("Analysis Date: $analysisDate")
            writer.appendLine("Total races in form: ${horseForm.last5Races.size}")
            writer.appendLine("")
            writer.appendLine("All races BEFORE filtering:")
            horseForm.last5Races.forEachIndexed { index, race ->
                writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
            }
            writer.appendLine("")
            writer.close()
        } catch (e: Exception) {
            println("⚠️ Could not write to debug file: ${e.message}")
        }
        
        if (horseForm.last5Races.isEmpty()) {
            println("📅 Freshness - No race history available")
            return 0.0
        }
        
        // CRITICAL FIX: Filter out races from today to avoid using today's results
        val historicalRaces = filterHistoricalRaces(horseForm.last5Races, analysisDate)
        
        // FILE LOGGING - after filtering
        try {
            val logFile = File("/sdcard/Download/FRESHNESS_DEBUG.txt")
            val writer = FileWriter(logFile, true) // Append mode
            writer.appendLine("Historical races AFTER filtering: ${historicalRaces.size}")
            historicalRaces.forEachIndexed { index, race ->
                writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
            }
            writer.appendLine("")
            writer.close()
        } catch (e: Exception) {
            println("⚠️ Could not write to debug file: ${e.message}")
        }
        
        if (historicalRaces.isEmpty()) {
            println("📅 Freshness - No historical races available after filtering today's races")
            return 0.0
        }
        
        // Sort by date to get the most recent historical race
        val sortedHistoricalRaces = historicalRaces.sortedByDescending { it.date }
        val lastRaceDate = sortedHistoricalRaces.firstOrNull()?.date
        
        // FILE LOGGING - final calculation
        try {
            val logFile = File("/sdcard/Download/FRESHNESS_DEBUG.txt")
            val writer = FileWriter(logFile, true) // Append mode
            writer.appendLine("Sorted historical races (most recent first):")
            sortedHistoricalRaces.forEachIndexed { index, race ->
                writer.appendLine("  Race ${index + 1}: Position=${race.position}, Date=${race.date}, Track=${race.track}")
            }
            writer.appendLine("")
            writer.appendLine("Last race date: $lastRaceDate")
            writer.appendLine("")
            writer.close()
        } catch (e: Exception) {
            println("⚠️ Could not write to debug file: ${e.message}")
        }
        
        if (lastRaceDate == null) {
            println("📅 Freshness - Last race date not available")
            return 0.0
        }
        
        val daysSinceRun = ((analysisDate.time - lastRaceDate.time) / (1000 * 60 * 60 * 24)).toInt()
        
        val score = when {
            daysSinceRun in 14..28 -> 3.0  // Optimal freshness (2-4 weeks) - perfect timing
            daysSinceRun in 29..56 -> 1.0  // Freshened up (4-8 weeks) - acceptable
            daysSinceRun in 7..13 -> 0.0   // Backing up (1-2 weeks) - may be tired
            daysSinceRun < 7 -> 0.0        // Quick backup (<1 week) - likely tired
            else -> 0.0                     // Too long without being spell horse (handled separately)
        }
        
        // FILE LOGGING - final result
        try {
            val logFile = File("/sdcard/Download/FRESHNESS_DEBUG.txt")
            val writer = FileWriter(logFile, true) // Append mode
            writer.appendLine("Final calculation:")
            writer.appendLine("  Days since last run: $daysSinceRun")
            writer.appendLine("  Score: $score points")
            writer.appendLine("=" .repeat(100))
            writer.appendLine("")
            writer.close()
        } catch (e: Exception) {
            println("⚠️ Could not write to debug file: ${e.message}")
        }
        
        println("📅 Freshness - Days since last run: ${daysSinceRun}, Score: ${String.format("%.1f", score)} points")
        return score
    }
    
    /**
     * Filter out races that occurred on or after the analysis date
     * This prevents using today's results when analyzing today's races
     */
    private fun filterHistoricalRaces(races: List<RaceResultDetail>, analysisDate: Date): List<RaceResultDetail> {
        val analysisDateOnly = java.util.Calendar.getInstance().apply {
            time = analysisDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        
        val filtered = races.filter { race ->
            race.date?.let { raceDate ->
                val raceDateOnly = java.util.Calendar.getInstance().apply {
                    time = raceDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.time
                
                // CRITICAL: Only include races that are STRICTLY BEFORE the analysis date
                // This excludes races from the same day (today)
                raceDateOnly.before(analysisDateOnly)
            } ?: true // FIXED: Include races with null dates - these are from form string parsing and are valid historical races
        }
        
        if (filtered.size != races.size) {
            println("🗓️ Filtered historical races: ${races.size} -> ${filtered.size} (excluded ${races.size - filtered.size} same-day/future races)")
        }
        
        return filtered
    }
    
    /**
     * Categorize track conditions for comparison
     */
    private fun getTrackConditionCategory(condition: String): String? {
        return when {
            condition.contains("Firm") || condition.contains("Good") -> "Firm/Good"
            condition.contains("Soft") -> "Soft"
            condition.contains("Heavy") -> "Heavy"
            condition.contains("Synthetic") -> "Synthetic"
            else -> null
        }
    }
    
    /**
     * Classify betting recommendation based on point differential
     */
    fun classifyBettingRecommendation(horses: List<ScoredHorse>): List<BettingRecommendation> {
        if (horses.size < 2) return emptyList()
        
        val recommendations = mutableListOf<BettingRecommendation>()
        val topHorse = horses[0]
        val secondHorse = horses[1]
        val pointGap = topHorse.score - secondHorse.score
        
        // Only the TOP horse gets a betting recommendation based on its gap from 2nd place
        val betType = when {
            pointGap >= 8.0 -> BetType.SUPER_BET
            pointGap >= 5.0 -> BetType.BEST_BET
            pointGap >= 3.0 -> BetType.GOOD_BET
            else -> null // No special color for minimal gaps
        }
        
        if (betType != null) {
        val confidence = when (betType) {
            BetType.SUPER_BET -> "Highest confidence - significant advantage"
            BetType.BEST_BET -> "High confidence - clear advantage"
            BetType.GOOD_BET -> "Moderate confidence - some advantage"
                else -> "Lower confidence - minimal advantage"
        }
        
        recommendations.add(BettingRecommendation(betType, pointGap, confidence))
        }
        
        // All other horses get no recommendation (null betType = standard gold border)
        for (i in 1 until horses.size) {
            recommendations.add(BettingRecommendation(BetType.CONSIDER, 0.0, ""))
        }
        
        return recommendations
    }
    

    
    private fun abs(value: Int): Int = if (value < 0) -value else value
    
    /**
     * Helper function to get position suffix (1st, 2nd, 3rd, 4th, etc.)
     */
    private fun getPositionSuffix(position: Int): String {
        return when {
            position == 1 -> "st"
            position == 2 -> "nd"
            position == 3 -> "rd"
            else -> "th"
        }
    }
    
    enum class ClassComparison {
        DROPPING, SIMILAR, RISING
    }

}
