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
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .method(original.method, original.body)
                
                chain.proceed(requestBuilder.build())
            }
            .build()
    }
    
    // Build track form URL
    fun buildTrackFormUrl(date: String, state: String, trackName: String): String {
        // Racing Australia expects: Key=2025Aug13,VIC,Sportsbet Sandown Lakeside
        // The %2C in the URL is actually a comma (,)
        val key = "$date,$state,$trackName"
        println("🔗 Building track URL with key: $key")
        
        val url = "$BASE_URL/FreeFields/Form.aspx?Key=$key&recentForm=Y"
        println("🌐 Generated track URL: $url")
        
        return url
    }
    
    // Build horse form URL
    fun buildHorseFormUrl(horseCode: String, stage: String, key: String, raceEntry: String): String {
        return "$BASE_URL/InteractiveForm/HorseFullForm.aspx?horsecode=${horseCode}&stage=${stage}&Key=${key}&src=horseform&raceentry=${raceEntry}"
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
            .header("Accept-Encoding", "gzip, deflate")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
    }
}
