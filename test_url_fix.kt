fun main() {
    println("🧪 Testing Fixed URL Construction...")
    
    // Test the URL construction logic
    val date = "2025Aug27"
    val state = "VIC"
    val trackName = "Sportsbet Sandown Hillside"
    
    // Build the key
    val key = "$date,$state,$trackName"
    println("🔍 Key: $key")
    
    // URL encode the key parameter (spaces become %20, commas become %2C)
    val encodedKey = key.replace(" ", "%20").replace(",", "%2C")
    val url = "https://www.racingaustralia.horse/FreeFields/Form.aspx?Key=$encodedKey&recentForm=Y"
    
    println("🔍 Date: $date")
    println("🔍 State: $state") 
    println("🔍 Track: $trackName")
    println("🔍 Encoded key: $encodedKey")
    println("🌐 Generated URL: $url")
    
    // Expected format from your example
    val expectedUrl = "https://www.racingaustralia.horse/FreeFields/Form.aspx?Key=2025Aug27%2CVIC%2CSportsbet%20Sandown%20Hillside"
    println("🎯 Expected URL: $expectedUrl")
    
    // Check if they match
    val match = url == expectedUrl
    println("✅ URLs match: $match")
    
    if (match) {
        println("🎉 SUCCESS: URL construction is working correctly!")
    } else {
        println("❌ FAILED: URL construction is not working correctly")
        println("Difference:")
        println("Generated: $url")
        println("Expected:  $expectedUrl")
    }
}
