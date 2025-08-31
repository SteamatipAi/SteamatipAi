package com.steamatipai.scoring

import com.steamatipai.data.models.*
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ScoringEngine {
    
    companion object {
        const val TOTAL_POINTS = 116.0  // Updated for 9 laws
        const val RECENT_FORM_WEIGHT = 25.0
        const val CLASS_SUITABILITY_WEIGHT = 25.0
        const val TRACK_DISTANCE_WEIGHT = 20.0
        const val SECTIONAL_TIME_WEIGHT = 8.0
        const val BARRIER_WEIGHT = 6.0
        const val JOCKEY_WEIGHT = 8.0
        const val TRAINER_WEIGHT = 8.0
        const val COMBINATION_WEIGHT = 8.0
        const val TRACK_CONDITION_WEIGHT = 8.0  // NEW LAW 9
        
        const val SPELL_THRESHOLD_WEEKS = 12
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
        
        // Check if horse is returning from spell or first-up
        val isReturningFromSpell = isReturningFromSpell(horse, horseForm)
        val isFirstUp = isFirstUpHorse(horseForm)
        
        when {
            isReturningFromSpell -> {
                println("🏆 ${horse.name} - USING SPELL HORSE SCORING (returning from spell)")
                val score = calculateSpellHorseScore(horse, race, horseForm, jockeyRankings, trainerRankings)
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.RETURNING_FROM_SPELL,
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
                val scoreBreakdown = ScoreBreakdown(
                    type = ScoringType.FIRST_UP,
                    totalScore = score
                )
                return ScoredHorse(
                    horse = horse,
                    score = score,
                    scoreBreakdown = scoreBreakdown
                )
            }
            else -> {
                println("🏆 ${horse.name} - USING NORMAL HORSE SCORING (all 9 laws)")
                // Apply all normal laws
                val recentForm = calculateRecentFormScore(horseForm)
                val classSuitability = calculateClassSuitabilityScore(horse, race, horseForm)
                val trackDistance = calculateTrackDistanceScore(horse, race, horseForm)
                val sectionalTime = calculateSectionalTimeScore(horseForm)
                val barrier = calculateBarrierScore(horse)
                val jockey = calculateJockeyScore(horse, jockeyRankings)
                val trainer = calculateTrainerScore(horse, trainerRankings)
                val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, horseForm)
                val jockeyTrainerPartnership = calculateJockeyTrainerPartnershipScore(horse, horseForm)
                val combination = jockeyHorseRelationship + jockeyTrainerPartnership
                val trackCondition = calculateTrackConditionScore(horse, race, horseForm)  // NEW LAW 9
                
                val totalScore = recentForm + classSuitability + trackDistance + 
                    sectionalTime + barrier + jockey + trainer + combination + trackCondition
                
                // COMPREHENSIVE SCORING LOGGING
                println("🏆 ${horse.name} SCORING BREAKDOWN:")
                println("   📊 Law 1 - Recent Form: ${String.format("%.1f", recentForm)} points")
                println("   🎯 Law 2 - Class Suitability: ${String.format("%.1f", classSuitability)} points")
                println("   🏁 Law 3 - Track/Distance History: ${String.format("%.1f", trackDistance)} points")
                println("   ⚡ Law 4 - Sectional Time: ${String.format("%.1f", sectionalTime)} points")
                println("   🚪 Law 5 - Barrier: ${String.format("%.1f", barrier)} points")
                println("   🏇 Law 6 - Jockey: ${String.format("%.1f", jockey)} points")
                println("   👨‍🏫 Law 7 - Trainer: ${String.format("%.1f", trainer)} points")
                println("   🤝 Law 8 - Jockey-Horse Relationship: ${String.format("%.1f", jockeyHorseRelationship)} points")
                println("   🤝 Law 8 - Jockey-Trainer Partnership: ${String.format("%.1f", jockeyTrainerPartnership)} points")
                println("   🔗 Law 8 - Total Combination: ${String.format("%.1f", combination)} points")
                println("   🌦️ Law 9 - Track Condition: ${String.format("%.1f", trackCondition)} points")
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
                    jockeyHorseRelationship = jockeyHorseRelationship,
                    jockeyTrainerPartnership = jockeyTrainerPartnership,
                    combinationScore = combination,
                    trackCondition = trackCondition,  // NEW LAW 9
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
     * Law 1: Recent Form (25 points)
     * Considers last 5 races for wins, places, and last start margin bonus
     */
    private fun calculateRecentFormScore(horseForm: HorseForm): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        var score = 0.0
        val last5Races = horseForm.last5Races.take(5)
        
        last5Races.forEachIndexed { index, race ->
            val recency = 5.0 - index // More recent = higher multiplier
            val multiplier = recency / 5.0
            
            when (race.position) {
                1 -> score += 5.0 * multiplier // Win bonus
                2, 3 -> score += 3.0 * multiplier // Place bonus
            }
        }
        
        // SPECIAL BONUS: Last start margin bonus (within 4 lengths of winner)
        // Only applies to the most recent race (index 0)
        if (last5Races.isNotEmpty()) {
            val lastStart = last5Races[0]
            lastStart.margin?.let { margin ->
                // Bonus for being close to winner regardless of position
                if (margin <= 4.0) {
                    score += 3.0 // Fixed bonus for last start performance
                    println("🏇 ${lastStart.position}${getPositionSuffix(lastStart.position)} place within ${margin} lengths - margin bonus applied")
                }
            }
        }
        
        return min(score, RECENT_FORM_WEIGHT)
    }
    
    /**
     * Law 2: Class Suitability (25 points)
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
     * Law 3: Track/Distance Suitability (20 points)
     * Points for good performance at this track and distance
     */
    private fun calculateTrackDistanceScore(horse: Horse, race: Race, horseForm: HorseForm): Double {
        val trackDistanceHistory = horseForm.trackDistanceHistory.filter { 
            it.track == race.venue && it.distance == race.distance 
        }
        
        if (trackDistanceHistory.isEmpty()) return 0.0 // No score for first time
        
        var score = 0.0
        
        // Performance at this track/distance
        val wins = trackDistanceHistory.count { it.position == 1 }
        val places = trackDistanceHistory.count { it.position <= 3 }
        val totalRuns = trackDistanceHistory.size
        
        // Win rate bonus
        val winRate = wins.toDouble() / totalRuns
        score += winRate * 10.0
        
        // Place rate bonus
        val placeRate = places.toDouble() / totalRuns
        score += placeRate * 8.0
        
        // Average finishing position bonus
        val avgPosition = trackDistanceHistory.sumOf { it.position } / totalRuns.toDouble()
        if (avgPosition <= 4.0) {
            score += (5.0 - avgPosition) * 2.0
        }
        
        return min(score, TRACK_DISTANCE_WEIGHT)
    }
    
    /**
     * Law 4: Finishing Speed Bonus (8 points)
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
     * Law 5: Barrier (6 points)
     * Points for barriers 1-8
     */
    private fun calculateBarrierScore(horse: Horse): Double {
        return if (horse.barrier in 1..8) BARRIER_WEIGHT else 0.0
    }
    
    /**
     * Law 6: Jockey (8 points)
     * Points based on jockey's premiership ranking
     */
    private fun calculateJockeyScore(horse: Horse, jockeyRankings: List<JockeyPremiership>): Double {
        val jockeyRank = getJockeyRank(horse.jockey, jockeyRankings)
        
        return when {
            jockeyRank in 1..5 -> JOCKEY_WEIGHT
            jockeyRank in 6..10 -> 5.0
            jockeyRank in 11..20 -> 2.0
            else -> 0.0
        }
    }
    
    /**
     * Law 7: Trainer (8 points)
     * Points based on trainer's premiership ranking
     */
    private fun calculateTrainerScore(horse: Horse, trainerRankings: List<TrainerPremiership>): Double {
        val trainerRank = getTrainerRank(horse.trainer, trainerRankings)
        
        return when {
            trainerRank in 1..5 -> TRAINER_WEIGHT
            trainerRank in 6..10 -> 5.0
            trainerRank in 11..20 -> 2.0
            else -> 0.0
        }
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
        
        // Trial sectional times
        val sectionalScore = calculateSectionalTimeScore(horseForm)
        score += sectionalScore
        
        // Jockey/trainer combination success
        val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, horseForm)
        val jockeyTrainerPartnership = calculateJockeyTrainerPartnershipScore(horse, horseForm)
        val combinationScore = jockeyHorseRelationship + jockeyTrainerPartnership
        score += combinationScore
        
        // Barrier still applies
        val barrierScore = calculateBarrierScore(horse)
        score += barrierScore
        
        // NEW: Penalty for lack of recent form (spell horses should not score too high)
        val formPenalty = 5.0
        score -= formPenalty
        println("🏇 Spell Horse Form Penalty: -$formPenalty points (lack of recent form)")
        
        // COMPREHENSIVE SPELL HORSE SCORING LOGGING
        println("🏆 ${horse.name} SPELL HORSE SCORING BREAKDOWN:")
        println("   👨‍🏫 Trainer Premiership (x1.5): ${String.format("%.1f", trainerScore)} points")
        println("   🏇 Jockey Premiership (x1.2): ${String.format("%.1f", jockeyScore)} points")
        println("   📈 Up Results: ${String.format("%.1f", upScore)} points")
        println("   ⚡ Trial Sectional Time: ${String.format("%.1f", sectionalScore)} points")
        println("   🤝 Jockey-Horse Relationship: ${String.format("%.1f", jockeyHorseRelationship)} points")
        println("   🤝 Jockey-Trainer Partnership: ${String.format("%.1f", jockeyTrainerPartnership)} points")
        println("   🔗 Total Combination: ${String.format("%.1f", combinationScore)} points")
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
        val barrierScore = calculateBarrierScore(horse)
        score += barrierScore
        println("🏇 First Starter ${horse.name} - Barrier Score: $barrierScore")
        
        // 5. TRIAL SECTIONAL TIMES (10 points max)
        val sectionalScore = calculateTrialSectionalScore(horseForm, race)
        score += sectionalScore
        println("🏇 First Starter ${horse.name} - Sectional Score: $sectionalScore")
        
        // 6. JOCKEY-TRAINER COMBINATION SUCCESS (4 points max)
        val jockeyHorseRelationship = calculateJockeyHorseRelationshipScore(horse, horseForm)
        val jockeyTrainerPartnership = calculateJockeyTrainerPartnershipScore(horse, horseForm)
        val combinationScore = (jockeyHorseRelationship + jockeyTrainerPartnership) * 0.5
        score += combinationScore
        println("🏇 First Starter ${horse.name} - Combination Score: $combinationScore")
        
        println("🏇 First Starter ${horse.name} - TOTAL SCORE: $score")
        
        // Cap first starters at reasonable level but allow good ones to score well
        return min(score, 50.0) // Increased cap to 50 points for good first starters
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
    private fun isReturningFromSpell(horse: Horse, horseForm: HorseForm): Boolean {
        // Only mark as spell if we have actual form data showing a gap
        if (horseForm.last5Races.isEmpty()) return false // Don't assume spell if no data
        
        val lastRace = horseForm.last5Races.first()
        lastRace.date?.let { lastRaceDate ->
            val weeksAgo = (Date().time - lastRaceDate.time) / (1000 * 60 * 60 * 24 * 7)
            return weeksAgo >= SPELL_THRESHOLD_WEEKS
        }
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
    
    private fun getJockeyRank(jockeyName: String, rankings: List<JockeyPremiership>): Int {
        return rankings.find { it.name.equals(jockeyName, ignoreCase = true) }?.rank ?: Int.MAX_VALUE
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
        return rankings.find { it.name.equals(trainerName, ignoreCase = true) }?.rank ?: Int.MAX_VALUE
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
     * Based on how many times the jockey has ridden this horse in last 5 starts
     */
    private fun calculateJockeyHorseRelationshipScore(horse: Horse, horseForm: HorseForm): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        val currentJockey = horse.jockey
        if (currentJockey.isEmpty()) return 0.0
        
        // Count how many times this jockey has ridden this horse in last 5 starts
        val jockeyRides = horseForm.last5Races.count { race ->
            race.jockey?.equals(currentJockey, ignoreCase = true) == true
        }
        
        val score = when (jockeyRides) {
            3, 4, 5 -> 4.0  // Strong relationship (3+ rides)
            2 -> 2.5         // Moderate relationship (2 rides)
            1 -> 1.0         // Weak relationship (1 ride)
            else -> 0.0      // No relationship (0 rides)
        }
        
        println("🏇 Jockey-Horse Relationship: $currentJockey has ridden ${horse.name} $jockeyRides times in last 5 starts = $score points")
        
        return score
    }
    
    /**
     * Calculate jockey-trainer partnership score (4 points max)
     * Based on how many times the jockey has ridden for this trainer in last 5 starts
     */
    private fun calculateJockeyTrainerPartnershipScore(horse: Horse, horseForm: HorseForm): Double {
        if (horseForm.last5Races.isEmpty()) return 0.0
        
        val currentTrainer = horse.trainer
        if (currentTrainer.isEmpty()) return 0.0
        
        // Count how many times this jockey has ridden for this trainer in last 5 starts
        val trainerRides = horseForm.last5Races.count { race ->
            race.trainer?.equals(currentTrainer, ignoreCase = true) == true
        }
        
        val score = when (trainerRides) {
            3, 4, 5 -> 4.0  // Strong partnership (3+ rides)
            2 -> 2.5         // Moderate partnership (2 rides)
            1 -> 1.0         // Weak partnership (1 ride)
            else -> 0.0      // No partnership (0 rides)
        }
        
        println("🏇 Jockey-Trainer Partnership: ${horse.jockey} has ridden for $currentTrainer $trainerRides times in last 5 starts = $score points")
        
        return score
    }
    
    /**
     * Law 9: Track Condition Suitability (8 points)
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
        
        val betType = when {
            pointGap >= 8.0 -> BetType.SUPER_BET
            pointGap >= 5.0 -> BetType.BEST_BET
            pointGap >= 3.0 -> BetType.GOOD_BET
            else -> BetType.CONSIDER
        }
        
        val confidence = when (betType) {
            BetType.SUPER_BET -> "Highest confidence - significant advantage"
            BetType.BEST_BET -> "High confidence - clear advantage"
            BetType.GOOD_BET -> "Moderate confidence - some advantage"
            BetType.CONSIDER -> "Lower confidence - minimal advantage"
        }
        
        recommendations.add(BettingRecommendation(betType, pointGap, confidence))
        
        // Add recommendations for other horses if they have significant gaps
        for (i in 1 until horses.size - 1) {
            val currentHorse = horses[i]
            val nextHorse = horses[i + 1]
            val gap = currentHorse.score - nextHorse.score
            
            if (gap >= 3.0) {
                val horseBetType = when {
                    gap >= 8.0 -> BetType.SUPER_BET
                    gap >= 5.0 -> BetType.BEST_BET
                    gap >= 3.0 -> BetType.GOOD_BET
                    else -> BetType.CONSIDER
                }
                
                val horseConfidence = when (horseBetType) {
                    BetType.SUPER_BET -> "Strong contender - significant gap"
                    BetType.BEST_BET -> "Good contender - clear gap"
                    BetType.GOOD_BET -> "Decent contender - some gap"
                    BetType.CONSIDER -> "Possible contender - minimal gap"
                }
                
                recommendations.add(BettingRecommendation(horseBetType, gap, horseConfidence))
            }
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

    enum class BetType {
        SUPER_BET,    // 8+ points clear of 2nd place
        BEST_BET,     // 5-7 points clear of 2nd place  
        GOOD_BET,     // 3-4 points clear of 2nd place
        CONSIDER      // 1-2 points clear of 2nd place
    }

    data class BettingRecommendation(
        val betType: BetType,
        val pointGap: Double,
        val confidence: String
    )
}
