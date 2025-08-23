package com.steamatipai

import com.steamatipai.network.ScrapingService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*
import java.text.SimpleDateFormat

class ScrapingTest {
    
    @Test
    fun testTodaysTrackScraping() {
        runBlocking {
            println("🧪 Testing track scraping for today's date...")
            
            val today = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            println("📅 Today's date: ${dateFormat.format(today)}")
            
            try {
                val scrapingService = ScrapingService()
                val tracks = scrapingService.scrapeAvailableTracks(today)
                
                println("🏁 Found ${tracks.size} tracks for today:")
                tracks.forEach { track ->
                    println("  📍 ${track.name} (${track.state}) - Key: ${track.key}")
                }
                
                if (tracks.isEmpty()) {
                    println("⚠️ No tracks found for today's date - this could be normal if no racing today")
                } else {
                    println("✅ Successfully found tracks for today")
                    
                    // Test race scraping for first track
                    val firstTrack = tracks.first()
                    println("\n🔍 Testing race scraping for ${firstTrack.name}...")
                    val races = scrapingService.scrapeTrackRaces(firstTrack, today)
                    println("🏁 Found ${races.size} races for ${firstTrack.name}")
                    
                    races.forEach { race ->
                        println("  🏇 Race ${race.raceNumber}: ${race.name} (${race.horses.size} horses)")
                    }
                }
                
            } catch (e: Exception) {
                println("❌ Error testing scraping: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
