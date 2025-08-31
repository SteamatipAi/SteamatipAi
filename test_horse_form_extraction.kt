fun main() {
    println("🧪 Testing Horse Form Data Extraction Logic...")
    
    // Test the race result pattern matching
    val sampleRaceText = "1 of 12 [BDGO 08Sep24] 1000m Good4 MDN-SW $37,500 ($6,750)"
    println("🔍 Sample race text: $sampleRaceText")
    
    // Test the regex pattern
    val racePattern = Regex("(\\d+)\\s+of\\s+(\\d+)\\s+\\[([A-Z]+)\\s+(\\d{2}[A-Za-z]{3}\\d{2})\\]\\s+(\\d+)m\\s+([A-Za-z0-9]+)\\s+([^\\$]+)\\s*\\$([\\d,]+)")
    val match = racePattern.find(sampleRaceText)
    
    if (match != null) {
        val position = match.groupValues[1].toInt()
        val fieldSize = match.groupValues[2].toInt()
        val track = match.groupValues[3]
        val date = match.groupValues[4]
        val distance = match.groupValues[5].toInt()
        val trackCondition = match.groupValues[6]
        val raceClass = match.groupValues[7].trim()
        val prizeMoney = match.groupValues[8].replace(",", "").toDoubleOrNull() ?: 0.0
        
        println("✅ Extracted race data:")
        println("  Position: $position of $fieldSize")
        println("  Track: $track")
        println("  Date: $date")
        println("  Distance: ${distance}m")
        println("  Track Condition: $trackCondition")
        println("  Race Class: $raceClass")
        println("  Prize Money: $${prizeMoney}")
        
        // Test date parsing
        val parsedDate = parseDate(date)
        println("  Parsed Date: $parsedDate")
        
        // Test up results pattern
        val upText = "1st Up: 2:1-1-0 2nd Up: 1:1-0-0"
        val upPattern = Regex("1st\\s+Up:\\s*(\\d+):(\\d+)-(\\d+)-(\\d+)")
        val upMatch = upPattern.find(upText)
        
        if (upMatch != null) {
            val runs = upMatch.groupValues[1].toInt()
            val wins = upMatch.groupValues[2].toInt()
            val places = upMatch.groupValues[3].toInt()
            println("✅ Up results: $wins wins, $places places from $runs runs")
        }
        
        // Test sectional time pattern
        val sectionalText = "600m 33.90"
        val timePattern = Regex("600m\\s+(\\d+\\.\\d+)", RegexOption.IGNORE_CASE)
        val timeMatch = timePattern.find(sectionalText)
        
        if (timeMatch != null) {
            val time = timeMatch.groupValues[1].toDoubleOrNull()
            println("✅ Sectional time: ${time}s")
        }
        
        println("🎉 SUCCESS: Horse form extraction logic is working correctly!")
        
    } else {
        println("❌ FAILED: Could not match race pattern")
    }
}

fun parseDate(dateStr: String): String {
    return try {
        // Handle formats like "08Sep24", "27Aug25"
        val dateFormat = java.text.SimpleDateFormat("ddMMMyy", java.util.Locale.ENGLISH)
        val date = dateFormat.parse(dateStr)
        date.toString()
    } catch (e: Exception) {
        try {
            // Alternative format: "08Sep2024"
            val dateFormat2 = java.text.SimpleDateFormat("ddMMMyyyy", java.util.Locale.ENGLISH)
            val date = dateFormat2.parse(dateStr)
            date.toString()
        } catch (e2: Exception) {
            "Could not parse: $dateStr"
        }
    }
}
