import java.text.SimpleDateFormat
import java.util.*

fun main() {
    println("🧪 Testing Racing Australia URL construction...")
    
    // Test date formatting
    val testDate = Calendar.getInstance().apply { 
        set(2025, Calendar.AUGUST, 27) 
    }.time
    
    val dateFormat = SimpleDateFormat("yyyyMMMdd", Locale.ENGLISH)
    val dateStr = dateFormat.format(testDate)
    println("📅 Formatted date: $dateStr")
    
    // Test URL construction
    val state = "VIC"
    val trackName = "Sportsbet Sandown Hillside"
    val key = "$dateStr,$state,$trackName"
    val url = "https://www.racingaustralia.horse/FreeFields/Form.aspx?Key=$key&recentForm=Y"
    
    println("🔍 State: $state")
    println("🔍 Track: $trackName")
    println("🔍 Key: $key")
    println("🌐 Generated URL: $url")
    
    // Expected format from your example
    val expectedUrl = "https://www.racingaustralia.horse/FreeFields/Form.aspx?Key=2025Aug27%2CVIC%2CSportsbet%20Sandown%20Hillside"
    println("🎯 Expected URL: $expectedUrl")
    
    // Check if they match (ignoring URL encoding)
    val decodedExpected = expectedUrl.replace("%2C", ",").replace("%20", " ")
    val decodedGenerated = url.replace(" ", "%20").replace(",", "%2C")
    
    println("\n🔍 URL Comparison:")
    println("Generated (decoded): ${url.replace(" ", "%20").replace(",", "%2C")}")
    println("Expected: $expectedUrl")
    println("Match: ${decodedGenerated == expectedUrl}")
}
