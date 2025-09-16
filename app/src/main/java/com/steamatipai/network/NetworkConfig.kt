package com.steamatipai.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetworkConfig {
    
    // Base URLs for Racing Australia
    const val BASE_URL = "https://www.racingaustralia.horse"
    const val CALENDAR_URL = "$BASE_URL/"
    const val PREMIERSHIP_URL = "$BASE_URL/FreeServices/Premierships.aspx"
    
    // Create configured HTTP client
    fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .method(original.method, original.body)
                
                chain.proceed(requestBuilder.build())
            }
            .build()
    }
    
    // Build track form URL
    fun buildTrackFormUrl(date: String, state: String, trackName: String): String {
        // Racing Australia expects: Key=2025Aug27,VIC,Sportsbet Sandown Hillside
        // URL encode the parameters properly
        val key = "$date,$state,$trackName"
        println("🔗 Building track URL with key: $key")
        println("🔍 Date: $date")
        println("🔍 State: $state") 
        println("🔍 Track: $trackName")
        
        // URL encode the key parameter (spaces become %20, commas become %2C)
        val encodedKey = key.replace(" ", "%20").replace(",", "%2C")
        val url = "$BASE_URL/FreeFields/Form.aspx?Key=$encodedKey&recentForm=Y"
        
        println("🌐 Generated track URL: $url")
        println("🔍 Encoded key: $encodedKey")
        
        return url
    }
    
    /**
     * Build calendar URL for Racing Australia
     */
    fun buildCalendarUrl(date: String): String {
        // Racing Australia calendar URL format - this is the main page that lists all tracks
        val url = "$BASE_URL/home.aspx"
        println("🔗 Building calendar URL: $url")

        // Also try alternative URL formats that Racing Australia might use
        val altUrl1 = "$BASE_URL/FreeFields/Calendar.aspx?Date=$date"
        val altUrl2 = "$BASE_URL/FreeServices/Calendar.aspx?Date=$date"
        val altUrl3 = "$BASE_URL/Calendar.aspx?Date=$date"

        println("🔍 Alternative calendar URLs:")
        println("   Primary: $altUrl1")
        println("   Alt 1: $altUrl2")
        println("   Alt 2: $altUrl3")

        return url
    }
    
    // Build horse form URL
    fun buildHorseFormUrl(horseCode: String, stage: String, key: String, raceEntry: String): String {
        // URL encode the key and raceEntry parameters (spaces become %20, commas become %2C)
        val encodedKey = key.replace(" ", "%20").replace(",", "%2C")
        val encodedRaceEntry = raceEntry.replace(" ", "%20").replace(",", "%2C")
        
        val url = "$BASE_URL/InteractiveForm/HorseFullForm.aspx?horsecode=${horseCode}&stage=${stage}&Key=${encodedKey}&src=horseform&raceentry=${encodedRaceEntry}"
        
        println("🔗 Building horse form URL:")
        println("🔍 Horse code: $horseCode")
        println("🔍 Stage: $stage")
        println("🔍 Key: $key")
        println("🔍 Encoded key: $encodedKey")
        println("🔍 Race entry: $raceEntry")
        println("🔍 Encoded race entry: $encodedRaceEntry")
        println("🌐 Generated horse form URL: $url")
        
        return url
    }
    
    // Build premiership URL
    fun buildPremiershipUrl(state: String = "Undefined", season: String = "2025", table: String = "Trainer"): String {
        return "$PREMIERSHIP_URL?State=${state}&Season=${season}&Table=${table}"
    }
    
    // Create request builder with common headers
    fun createRequestBuilder(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
    }
}
