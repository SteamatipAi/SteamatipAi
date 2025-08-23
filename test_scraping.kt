import kotlinx.coroutines.runBlocking
import java.util.*
import java.text.SimpleDateFormat

// Simple test to check what tracks are found for today
fun main() {
    runBlocking {
        println("🧪 Testing track scraping for today's date...")
        
        val today = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        println("📅 Today's date: ${dateFormat.format(today)}")
        
        try {
            // Test the scraping logic directly
            val scrapingService = com.steamatipai.network.ScrapingService()
            val tracks = scrapingService.scrapeAvailableTracks(today)
            
            println("🏁 Found ${tracks.size} tracks for today:")
            tracks.forEach { track ->
                println("  📍 ${track.name} (${track.state}) - Key: ${track.key}")
            }
            
            if (tracks.isEmpty()) {
                println("⚠️ No tracks found for today's date")
            }
            
        } catch (e: Exception) {
            println("❌ Error testing scraping: ${e.message}")
            e.printStackTrace()
        }
    }
}
