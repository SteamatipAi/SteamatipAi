package com.steamatipai.scoring

import com.steamatipai.data.models.*
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ScoringEngine {
    
    companion object {
        const val TOTAL_POINTS = 127.0  // Updated for 12 laws (normal horses: 11 points added)
        const val RECENT_FORM_WEIGHT = 25.0
        const val FIRST_UP_WEIGHT = 8.0  // NEW LAW 1 for spell horses
        const val SECOND_UP_WEIGHT = 8.0 // NEW LAW 2 for spell horses
        const val CLASS_SUITABILITY_WEIGHT = 25.0
        const val TRACK_DISTANCE_WEIGHT = 20.0
        const val SECTIONAL_TIME_WEIGHT = 8.0
        const val BARRIER_WEIGHT = 6.0
        const val JOCKEY_WEIGHT = 8.0
        const val TRAINER_WEIGHT = 8.0
        const val COMBINATION_WEIGHT = 8.0
        const val TRACK_CONDITION_WEIGHT = 8.0
        const val WEIGHT_ADVANTAGE_WEIGHT = 8.0  // NEW LAW 11
        const val FRESHNESS_WEIGHT = 3.0         // NEW LAW 12
        
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
        
        // NO FALLBACK - Only score horses with real form data
        if (horseForm == null) {
            println("❌ ${horse.name} - NO REAL FORM DATA AVAILABLE - EXCLUDING FROM SCORING")
            return null
        }
        
        // Check if horse is returning from spell based on form string
        val currentSpellStatus = getCurrentSpellStatus(horse)
        val isReturningFromSpell = currentSpellStatus == "1ST_UP" || currentSpellStatus == "2ND_UP"
        val isFirstUp = isFirstUpHorse(horseForm)
        
        when {
            isReturningFromSpell -> {
                println("🏆 ${horse.name} - USING SPELL HORSE SCORING (returning from spell)")
                println("🏆 ${horse.name} - Current spell status: $currentSpellStatus")
                
                // Calculate individual law scores based on current spell status
                val firstUp = if (currentSpellStatus == "1ST_UP") calculateFirstUpScore(horseForm) else 0.0
                val secondUp = if (currentSpellStatus == "2ND_UP") calculateSecondUpScore(horseForm) else 0.0
                val secondUpRecentFormBonus = if (currentSpellStatus == "2ND_UP") calculateSecondUpRecentFormBonus(horseForm) else 0.0
                val classSuitability = calculateClassSuitabilityScore(horse, race, horseForm) // Use historical class performance
                val trackDistance = calculateTrackDistanceScore(horse, race, horseForm) // Use historical track/distance performance
                val sectionalTime = if (currentSpellStatus == "2ND_UP") calculateSectionalTimeScore(horseForm) else 0.0 // Only 2nd Up horses get sectional time
                val barrier = calculateBarrierScore(horse, race) // UPDATED: now distance-aware
                val jockey = calculateJockeyScore(horse, jockeyRankings)
                val trainer = calculateTrainerScore(horse, trainerRankings)
                val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, horseForm) // Use historical relationship
                val jockeyTrainerPartnership = 0.0 // Removed jockey-trainer partnership scoring
                val combination = jockeyHorseRelationship // Only jockey-horse relationship now
                val trackCondition = calculateTrackConditionScore(horse, race, horseForm) // Use historical track condition performance
                val weightAdvantage = calculateWeightAdvantageScore(horse, race.horses) // NEW LAW 11
                val freshness = 0.0 // Spell horses don't get freshness bonus (they're already fresh from spell)
                
                // Calculate total score (including second up recent form bonus and new laws)
                val score = firstUp + secondUp + secondUpRecentFormBonus + classSuitability + trackDistance + 
                    sectionalTime + barrier + jockey + trainer + combination + trackCondition +
                    weightAdvantage + freshness
                
                // COMPREHENSIVE SPELL HORSE SCORING LOGGING
                println("🏆 ${horse.name} SPELL HORSE SCORING BREAKDOWN (Status: $currentSpellStatus):")
                println("   🎯 L1: 1st Up Performance: ${String.format("%.1f", firstUp)} points ${if (currentSpellStatus == "1ST_UP") "(ACTIVE)" else "(inactive)"}")
                println("   🎯 L2: 2nd Up Performance: ${String.format("%.1f", secondUp)} points ${if (currentSpellStatus == "2ND_UP") "(ACTIVE)" else "(inactive)"}")
                println("   📊 L3: Recent Form: ${String.format("%.1f", secondUpRecentFormBonus)} points ${if (currentSpellStatus == "2ND_UP") "(2nd up bonus)" else "(not used)"}")
                println("   🎯 L4: Class Suitability: ${String.format("%.1f", classSuitability)} points")
                println("   🏁 L5: Track/Distance History: ${String.format("%.1f", trackDistance)} points")
                println("   ⚡ L6: Sectional Time: ${String.format("%.1f", sectionalTime)} points ${if (currentSpellStatus == "2ND_UP") "(from 1st Up race)" else "(no recent data)"}")
                println("   🚪 L7: Barrier: ${String.format("%.1f", barrier)} points")
                println("   🏇 L8: Jockey: ${String.format("%.1f", jockey)} points")
                println("   👨‍🏫 L9: Trainer: ${String.format("%.1f", trainer)} points")
                println("   🤝 L10: Jockey-Horse Relationship: ${String.format("%.1f", jockeyHorseRelationship)} points")
                println("   🌦️ L11: Track Condition: ${String.format("%.1f", trackCondition)} points")
                println("   ⚖️ L12: Weight Advantage: ${String.format("%.1f", weightAdvantage)} points")
                println("   📅 L13: Freshness: ${String.format("%.1f", freshness)} points (spell horses exempt)")
                println("   💯 TOTAL SCORE: ${String.format("%.1f", score)} points")
                println("   ─".repeat(50))
                
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.RETURNING_FROM_SPELL,
                    recentForm = secondUpRecentFormBonus, // Use second up recent form bonus for spell horses
                    firstUp = firstUp,
                    secondUp = secondUp,
                    classSuitability = classSuitability,
                    trackDistance = trackDistance,
                    sectionalTime = sectionalTime,
                    barrier = barrier,
                    jockey = jockey,
                    trainer = trainer,
                    combination = combination,
                    trackCondition = trackCondition,
                    weightAdvantage = weightAdvantage,  // NEW LAW 11
                    freshness = freshness,              // NEW LAW 12 (0 for spell horses)
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
                val score = calculateFirstUpHorseScore(horse, race, horseForm, jockeyRankings, trainerRankings)
                
                // Calculate individual law scores for display
                val recentForm = 0.0 // First-up horses get 0 for recent form
                val classSuitability = 0.0 // First-up horses get 0 for class suitability
                val trackDistance = 0.0 // First-up horses get 0 for track/distance history
                val sectionalTime = calculateSectionalTimeScore(horseForm)
                val barrier = calculateBarrierScore(horse, race) // UPDATED: now distance-aware
                val jockey = calculateJockeyScore(horse, jockeyRankings)
                val trainer = calculateTrainerScore(horse, trainerRankings)
                val jockeyHorseRelationship = 0.0 // First-up horses get 0 for jockey-horse relationship
                val jockeyTrainerPartnership = 0.0 // First-up horses get 0 for jockey-trainer partnership
                val combination = jockeyHorseRelationship // Only jockey-horse relationship now
                val trackCondition = 0.0 // First-up horses get 0 for track condition history
                val weightAdvantage = calculateWeightAdvantageScore(horse, race.horses) // NEW LAW 11
                val freshness = 0.0 // First starters don't get freshness bonus (no race history)
                
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.FIRST_UP,
                    recentForm = recentForm,
                    classSuitability = classSuitability,
                    trackDistance = trackDistance,
                    sectionalTime = sectionalTime,
                    barrier = barrier,
                    jockey = jockey,
                    trainer = trainer,
                    combination = combination,
                    trackCondition = trackCondition,
                    weightAdvantage = weightAdvantage,  // NEW LAW 11
                    freshness = freshness,              // NEW LAW 12 (0 for first starters)
                    totalScore = score
                )
                return ScoredHorse(
                    horse = horse,
                    score = score,
                    scoreBreakdown = scoreBreakdown
                )
            }
            else -> {
                println("🏆 ${horse.name} - USING NORMAL HORSE SCORING (all 12 laws)")
                // Apply all normal laws
                val recentForm = calculateRecentFormScore(horseForm)
                val classSuitability = calculateClassSuitabilityScore(horse, race, horseForm)
                val trackDistance = calculateTrackDistanceScore(horse, race, horseForm)
                val sectionalTime = calculateSectionalTimeScore(horseForm)
                val barrier = calculateBarrierScore(horse, race) // UPDATED: now distance-aware
                val jockey = calculateJockeyScore(horse, jockeyRankings)
                val trainer = calculateTrainerScore(horse, trainerRankings)
                val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, horseForm)
                val jockeyTrainerPartnership = 0.0 // Removed jockey-trainer partnership scoring
                val combination = jockeyHorseRelationship // Only jockey-horse relationship now
                val trackCondition = calculateTrackConditionScore(horse, race, horseForm)
                val weightAdvantage = calculateWeightAdvantageScore(horse, race.horses) // NEW LAW 11
                val freshness = calculateFreshnessScore(horseForm, race.date) // NEW LAW 12
                
                val totalScore = recentForm + classSuitability + trackDistance + 
                    sectionalTime + barrier + jockey + trainer + combination + trackCondition +
                    weightAdvantage + freshness
                
                // COMPREHENSIVE SCORING LOGGING
                println("🏆 ${horse.name} NORMAL HORSE SCORING BREAKDOWN:")
                println("   🎯 L1: 1st Up Performance: ${String.format("%.1f", 0.0)} points (not used for normal horses)")
                println("   🎯 L2: 2nd Up Performance: ${String.format("%.1f", 0.0)} points (not used for normal horses)")
                println("   📊 L3: Recent Form: ${String.format("%.1f", recentForm)} points")
                println("   🎯 L4: Class Suitability: ${String.format("%.1f", classSuitability)} points")
                println("   🏁 L5: Track/Distance History: ${String.format("%.1f", trackDistance)} points")
                println("   ⚡ L6: Sectional Time: ${String.format("%.1f", sectionalTime)} points")
                println("   🚪 L7: Barrier: ${String.format("%.1f", barrier)} points")
                println("   🏇 L8: Jockey: ${String.format("%.1f", jockey)} points")
                println("   👨‍🏫 L9: Trainer: ${String.format("%.1f", trainer)} points")
                println("   🤝 L10: Jockey-Horse Relationship: ${String.format("%.1f", jockeyHorseRelationship)} points")
                println("   🌦️ L11: Track Condition: ${String.format("%.1f", trackCondition)} points")
                println("   ⚖️ L12: Weight Advantage: ${String.format("%.1f", weightAdvantage)} points")
                println("   📅 L13: Freshness: ${String.format("%.1f", freshness)} points")
                println("   💯 TOTAL SCORE: ${String.format("%.1f", totalScore)} points")
                println("   ─".repeat(50))
                
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.NORMAL,
                    recentForm = recentForm,
                    classSuitability = classSuitability,
                    trackDistance = trackDistance,
                    sectionalTime = sectionalTime,
                    barrier = barrier,
                    jockey = jockey,
                    trainer = trainer,
                    combination = combination,
                    trackCondition = trackCondition,
                    weightAdvantage = weightAdvantage,  // NEW LAW 11
                    freshness = freshness,              // NEW LAW 12
                    totalScore = totalScore
                )
                
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
    private fun calculateRecentFormScore(horseForm: HorseForm): Double {
        println("🔍 Law 1 DEBUG: Starting Law 1 calculation")
        println("🔍 Law 1 DEBUG: HorseForm.last5Races.size = ${horseForm.last5Races.size}")
        
        if (horseForm.last5Races.isEmpty()) {
            println("🏇 Law 1: No recent races found - returning 0.0")
            return 0.0
        }
        
        var score = 0.0
        val last5Races = horseForm.last5Races.take(5)
        println("🏇 Law 1: Processing ${last5Races.size} recent races")
        
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
        
        // IMPROVED: Better handling for horses with fewer than 5 races
        val availableRaces = last5Races.size
        val scalingFactor = if (availableRaces < 5) {
            // Scale up scoring for horses with fewer races to be fair
            5.0 / availableRaces.toDouble()
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
        println("🏇 Law 1: Total score: ${String.format("%.1f", finalScore)} (capped at ${RECENT_FORM_WEIGHT})")
        return finalScore
    }
    
    /**
     * Determine if horse is currently 1st up or 2nd up from current spell
     * Parses the form string to check recent entries (reading right-to-left)
     */
    private fun getCurrentSpellStatus(horse: Horse): String {
        val form = horse.form.trim()
        println("🔍 Analysing form string: '$form' for ${horse.name}")
        
        if (form.isEmpty()) {
            println("🔍 No form data available")
            return "UNKNOWN"
        }
        
        // Read form string right-to-left (most recent first)
        // Example: "32x1x2212x" -> read as: x, 2, 1, 2, x, 1, x, 2, 3
        
        val reversedForm = form.reversed()
        println("🔍 Reversed form string: '$reversedForm'")
        
        // Check first character (most recent race)
        val firstChar = reversedForm.firstOrNull()
        
        when {
            firstChar == 'x' || firstChar == 'X' -> {
                println("🔍 Most recent race is 'X' - horse is 1st Up from spell")
                return "1ST_UP"
            }
            firstChar != null && firstChar.isDigit() -> {
                // Most recent is a number, check if second character is X (spell)
                if (reversedForm.length >= 2 && (reversedForm[1] == 'x' || reversedForm[1] == 'X')) {
                    println("🔍 Pattern: ${firstChar}X - horse is 2nd Up from spell")
                    return "2ND_UP"
                } else {
                    println("🔍 Most recent race is number but no spell pattern - normal horse")
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
    private fun calculateSecondUpRecentFormBonus(horseForm: HorseForm): Double {
        println("🔍 Law 2b: Starting Second Up Recent Form Bonus calculation")
        
        if (horseForm.last5Races.isEmpty()) {
            println("🏇 Law 2b: No recent races found - returning 0.0")
            return 0.0
        }
        
        // For second up horses, we specifically look at their first-up run (most recent race)
        val firstUpRun = horseForm.last5Races.firstOrNull()
        if (firstUpRun == null) {
            println("🏇 Law 2b: No first-up run data available - returning 0.0")
            return 0.0
        }
        
        var score = 0.0
        
        println("🏇 Law 2b: Analyzing first-up run - Position: ${firstUpRun.position}, Margin: ${firstUpRun.margin}")
        
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
        
        var score = 0.0
        val currentClass = parseRaceClass(race.raceClass)
        val last3Classes = horseForm.last5Races.take(3).mapNotNull { it.raceClass?.let { parseRaceClass(it) } }
        
        if (last3Classes.isNotEmpty()) {
            val classComparison = compareClasses(currentClass, last3Classes)
            
            when (classComparison) {
                ClassComparison.DROPPING -> {
                    score += 15.0 // Dropping in class bonus
                    
                    // ENHANCED: Check for declining horse pattern (repeatedly dropping class)
                    val consecutiveClassDrops = countConsecutiveClassDrops(horseForm.last5Races)
                    if (consecutiveClassDrops >= 3) {
                        score -= 5.0 // Warning: this horse is in decline, not just tactically dropping
                        println("⚠️ ${horse.name} - Declining pattern detected (${consecutiveClassDrops} consecutive class drops), penalty applied")
                    }
                    
                    // Extra bonus if performed well last start
                    if (horseForm.last5Races.isNotEmpty() && horseForm.last5Races[0].position <= 3) {
                        score += 5.0 // Class drop + good last start
                    }
                }
                ClassComparison.SIMILAR -> {
                    // Check performance in similar classes (last 5 starts)
                    val goodPerformancesInClass = horseForm.last5Races.count { race ->
                        race.position <= 3 && race.raceClass?.let { isSimilarClass(parseRaceClass(it), currentClass) } == true
                    }
                    score += goodPerformancesInClass * 3.0
                }
                ClassComparison.RISING -> {
                    // Rising in class - check if horse has form in lesser classes
                    val hasFormInLesserClasses = horseForm.last5Races.any { race ->
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
     * Law 4: Track/Distance Suitability (20 points) - ENHANCED with Distance Pattern Bonus
     * Separate scoring for track performance, distance performance, combination bonus, and distance pattern
     */
    private fun calculateTrackDistanceScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        val trackDistanceStats = horseForm.trackDistanceStats
        
        if (trackDistanceStats == null) {
            println("🏇 Law 4: No track/distance statistics available for ${horse.name}")
            return 0.0
        }
        
        var score = 0.0
        
        // Track Suitability (0-8 points)
        val trackScore = calculatePerformanceScore(trackDistanceStats.trackStats, "Track")
        score += trackScore
        
        // Distance Suitability (0-8 points)  
        val distanceScore = calculatePerformanceScore(trackDistanceStats.distanceStats, "Distance")
        score += distanceScore
        
        // Combination Bonus (0-4 points) - if horse has success in BOTH track AND distance
        val combinationScore = calculateCombinationBonus(trackDistanceStats)
        score += combinationScore
        
        // ENHANCED: Distance Pattern Bonus (0-5 points) - comfort zone or previous wins at distance
        val distancePatternBonus = calculateDistancePatternBonus(horse, race, horseForm)
        score += distancePatternBonus
        
        println("🏇 Law 4 (${horse.name}): Track=${String.format("%.1f", trackScore)}, Distance=${String.format("%.1f", distanceScore)}, Combination=${String.format("%.1f", combinationScore)}, Pattern=${String.format("%.1f", distancePatternBonus)}, Total=${String.format("%.1f", score)}")
        
        // Allow up to 25 points now (20 original + 5 pattern bonus)
        return min(score, TRACK_DISTANCE_WEIGHT + 5.0)
    }
    
    /**
     * Calculate performance score for track or distance (0-8 points)
     */
    private fun calculatePerformanceScore(stats: PerformanceStats, type: String): Double {
        if (stats.runs == 0) {
            println("🏇 Law 4: No $type history available")
            return 0.0
        }
        
        var score = 0.0
        
        // Win rate bonus (0-4 points)
        val winRate = stats.wins.toDouble() / stats.runs
        score += winRate * 4.0
        
        // Place rate bonus (0-4 points)
        val placeRate = stats.places.toDouble() / stats.runs
        score += placeRate * 4.0
        
        println("🏇 Law 3 $type: ${stats.runs} runs, ${stats.wins} wins, ${stats.places} places, WinRate=${String.format("%.2f", winRate)}, PlaceRate=${String.format("%.2f", placeRate)}")
        
        return min(score, 8.0)
    }
    
    /**
     * Calculate combination bonus (0-4 points) for track AND distance success
     */
    private fun calculateCombinationBonus(stats: TrackDistanceStats): Double {
        val trackStats = stats.trackStats
        val distanceStats = stats.distanceStats
        val combinedStats = stats.combinedStats
        
        // Must have some success in both track AND distance to get bonus
        if (trackStats.runs == 0 || distanceStats.runs == 0) {
            return 0.0
        }
        
        val trackHasSuccess = trackStats.wins > 0 || trackStats.places > 0
        val distanceHasSuccess = distanceStats.wins > 0 || distanceStats.places > 0
        
        if (!trackHasSuccess || !distanceHasSuccess) {
            return 0.0
        }
        
        // Bonus based on combined performance
        if (combinedStats.runs > 0) {
            val combinedWinRate = combinedStats.wins.toDouble() / combinedStats.runs
            val combinedPlaceRate = combinedStats.places.toDouble() / combinedStats.runs
            return (combinedWinRate * 2.0) + (combinedPlaceRate * 2.0)
        } else {
            // No combined history, but has success in both separately - small bonus
            return 1.0
        }
    }
    
    /**
     * Law 5: Finishing Speed Bonus (8 points)
     * Rewards horses with the fastest last 600m times from their last start
     */
    private fun calculateSectionalTimeScore(horseForm: HorseForm): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        // Get the last start (most recent race)
        val lastStart = horseForm.last5Races[0]
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
     * Law 6: Barrier (6 points) - ENHANCED: Distance-Aware
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
     * Law 7: Jockey (8 points)
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
     * Law 8: Trainer (8 points)
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
        val trackDistanceScore = calculateTrackDistanceScore(horse, race, horseForm)
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
        val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, horseForm)
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
        val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, horseForm)
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
        
        val lastRace = horseForm.last5Races.last() // Use LAST race (most recent) not first
        lastRace.date?.let { lastRaceDate ->
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
        println("⚠️ CRITICAL: Horse ${horse.name} has ${horseForm.last5Races.size} recent races but last race date is null - this is a parsing error!")
        return false // Default to not spell if we can't determine
    }
    
    private fun isFirstUpHorse(horseForm: HorseForm): Boolean {
        // Only mark as first-up if we have actual form data showing no races
        // AND we have some indication this is a first starter (like trial data)
        if (horseForm.last5Races.isEmpty() && horseForm.trialSectionalTimes.isNotEmpty()) {
            return true // Has trials but no races = first starter
        }
        return false // Default to not first-up if we can't determine
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
        val recentDistances = horseForm.last5Races.mapNotNull { it.distance }
        
        if (recentDistances.isEmpty()) return 0.0
        
        val averageRecentDistance = recentDistances.average()
        val distanceChange = kotlin.math.abs(raceDistance - averageRecentDistance)
        
        // Bonus for staying within comfort zone (±200m)
        if (distanceChange <= 200) {
            println("🏁 ${horse.name} - Distance comfort zone bonus (+3.0 points): ${raceDistance}m vs avg ${String.format("%.0f", averageRecentDistance)}m")
            return 3.0
        }
        
        // Check if horse has won at this exact distance before (±50m tolerance)
        val hasWonAtDistance = horseForm.last5Races.any { 
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
    private fun calculateJockeyHorseRelationshipScore(horse: Horse, horseForm: HorseForm): Double {
        println("🔍 LAW 8 DEBUG: Starting Jockey-Horse Relationship calculation for ${horse.name}")
        
        if (horseForm.last5Races.isEmpty()) {
            println("🔍 LAW 8 DEBUG: No historical races available for ${horse.name}")
            return 0.0
        }
        
        val currentJockey = horse.jockey
        if (currentJockey.isEmpty()) {
            println("🔍 LAW 8 DEBUG: No current jockey for ${horse.name}")
            return 0.0
        }
        
        println("🔍 LAW 8 DEBUG: Current jockey: '$currentJockey'")
        println("🔍 LAW 8 DEBUG: Historical races (${horseForm.last5Races.size}):")
        
        horseForm.last5Races.forEachIndexed { index, race ->
            println("   Race ${index + 1}: Position=${race.position}, Jockey='${race.jockey}', Trainer='${race.trainer}'")
        }
        
        // Look for previous wins with this jockey on this horse
        // Use normalized names for comparison to handle prefixes/suffixes
        val normalizedCurrentJockey = normalizeNameForMatching(currentJockey)
        println("🔍 LAW 8 DEBUG: Normalized current jockey: '$currentJockey' -> '$normalizedCurrentJockey'")
        
        val jockeyWins = horseForm.last5Races.count { race ->
            val historicalJockey = race.jockey ?: ""
            val normalizedHistoricalJockey = normalizeNameForMatching(historicalJockey)
            val jockeyMatch = normalizedHistoricalJockey.equals(normalizedCurrentJockey, ignoreCase = true)
            val isWin = race.position == 1
            println("🔍 LAW 8 DEBUG: Race jockey='$historicalJockey' (normalized: '$normalizedHistoricalJockey') matches current='$normalizedCurrentJockey'? $jockeyMatch, Position=${race.position} is win? $isWin")
            jockeyMatch && isWin
        }
        
        val jockeyPlaces = horseForm.last5Races.count { race ->
            val historicalJockey = race.jockey ?: ""
            val normalizedHistoricalJockey = normalizeNameForMatching(historicalJockey)
            val jockeyMatch = normalizedHistoricalJockey.equals(normalizedCurrentJockey, ignoreCase = true)
            val isPlace = race.position in 2..3
            println("🔍 LAW 8 DEBUG: Race jockey='$historicalJockey' (normalized: '$normalizedHistoricalJockey') matches current='$normalizedCurrentJockey'? $jockeyMatch, Position=${race.position} is place? $isPlace")
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
     * Law 10: Track Condition Suitability (8 points)
     * Assesses horse's success on similar track conditions
     */
    private fun calculateTrackConditionScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        val currentCondition = race.trackCondition ?: return 0.0
        val conditionCategory = getTrackConditionCategory(currentCondition)
        
        if (conditionCategory == null) {
            println("🌦️ ${horse.name} - Unknown track condition: $currentCondition")
            return 0.0
        }
        
        var bestResult = 0.0
        
        // Check last 5 races for similar track conditions
        horseForm.last5Races.forEach { raceResult ->
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
     * Law 11: Weight Advantage (8 points) - NEW
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
     * Law 12: Freshness (3 points) - NEW
     * Awards bonus for optimal time between runs
     * 2-4 weeks is ideal, backing up (<2 weeks) or too long (8+ weeks without being spell) penalized
     */
    private fun calculateFreshnessScore(horseForm: HorseForm, analysisDate: Date): Double {
        if (horseForm.last5Races.isEmpty()) {
            println("📅 Freshness - No race history available")
            return 0.0
        }
        
        val lastRaceDate = horseForm.last5Races[0].date
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
        
        println("📅 Freshness - Days since last run: ${daysSinceRun}, Score: ${String.format("%.1f", score)} points")
        return score
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
