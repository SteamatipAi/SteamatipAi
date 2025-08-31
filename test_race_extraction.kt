// Test race extraction logic
fun main() {
    println("🧪 Testing Race Extraction Logic")
    println("=" * 50)
    
    // Simulate what we're getting from Racing Australia
    val testRaceData = listOf(
        "Race 1: 1200m Good4 MDN-SW",
        "Race 2: 1400m Good4 BM64", 
        "Race 3: 1600m Good4 BM70"
    )
    
    println("📊 Test Race Data:")
    testRaceData.forEach { println("   $it") }
    
    // Simulate race number extraction
    val racePattern = Regex("Race\\s*(\\d+)", RegexOption.IGNORE_CASE)
    val raceMatches = racePattern.findAll(testRaceData.joinToString("\n"))
    val raceNumbers = raceMatches.map { it.groupValues[1].toInt() }.distinct().sorted().toList()
    
    println("\n🔍 Extracted Race Numbers: $raceNumbers")
    
    // Simulate what happens when we process each race
    raceNumbers.forEach { raceNumber ->
        println("\n🏇 Processing Race $raceNumber:")
        
        // Simulate horse extraction for each race
        val horses = simulateHorseExtraction(raceNumber)
        println("   Horses found: ${horses.size}")
        horses.forEach { horse ->
            println("     - ${horse.name} (No. ${horse.number}, Barrier ${horse.barrier})")
        }
        
        // Simulate race info extraction
        val raceInfo = simulateRaceInfoExtraction(raceNumber)
        if (raceInfo != null) {
            println("   Race Info: ${raceInfo.distance}m, ${raceInfo.surface}, ${raceInfo.trackCondition}")
        } else {
            println("   Race Info: NULL")
        }
    }
}

fun simulateHorseExtraction(raceNumber: Int): List<TestHorse> {
    // Simulate different horses for each race
    return when (raceNumber) {
        1 -> listOf(
            TestHorse("Horse A", 1, 3),
            TestHorse("Horse B", 2, 5),
            TestHorse("Horse C", 3, 7)
        )
        2 -> listOf(
            TestHorse("Horse D", 1, 2),
            TestHorse("Horse E", 2, 4),
            TestHorse("Horse F", 3, 6)
        )
        3 -> listOf(
            TestHorse("Horse G", 1, 1),
            TestHorse("Horse H", 2, 8),
            TestHorse("Horse I", 3, 10)
        )
        else -> emptyList()
    }
}

fun simulateRaceInfoExtraction(raceNumber: Int): TestRaceInfo? {
    return when (raceNumber) {
        1 -> TestRaceInfo(1200, "Turf", "Good 4", "MDN-SW")
        2 -> TestRaceInfo(1400, "Turf", "Good 4", "BM64")
        3 -> TestRaceInfo(1600, "Turf", "Good 4", "BM70")
        else -> null
    }
}

data class TestHorse(val name: String, val number: Int, val barrier: Int)
data class TestRaceInfo(val distance: Int, val surface: String, val trackCondition: String, val raceClass: String)
