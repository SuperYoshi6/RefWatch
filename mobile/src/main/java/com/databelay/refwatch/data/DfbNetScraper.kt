package com.databelay.refwatch.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

data class ScrapedMatchData(
    val homeTeam: String? = null,
    val awayTeam: String? = null,
    val competition: String? = null,
    val venue: String? = null,
    val dateTimeEpochMillis: Long? = null
)

class DfbNetScraper {
    private val TAG = "DfbNetScraper"

    /**
     * Scrapes match data from fussball.de based on the match ID.
     * Note: This is a robust but fragile approach as it depends on the site's HTML structure.
     */
    suspend fun scrapeMatchData(matchId: String): ScrapedMatchData? = withContext(Dispatchers.IO) {
        try {
            // fussball.de match URL format
            val urlString = "https://www.fussball.de/spiel/-/spiel-id/$matchId"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            
            if (connection.responseCode != 200) {
                Log.e(TAG, "Failed to fetch match data: ${connection.responseCode}")
                return@withContext null
            }

            val html = Scanner(connection.inputStream).useDelimiter("\\A").next()
            
            // Simple regex-based parsing (extracting meta tags or specific classes)
            // Team Names usually in <meta property="og:title" ...> or specific spans
            val homeTeam = Regex("class=\"td-team-name\">([^<]+)</span>").find(html)?.groupValues?.get(1)?.trim()
            val awayTeam = Regex("class=\"td-team-name\">([^<]+)</span>").findAll(html).lastOrNull()?.groupValues?.get(1)?.trim()
            
            val competition = Regex("class=\"td-competition-name\">([^<]+)</a>").find(html)?.groupValues?.get(1)?.trim()
            
            // Date extraction (requires more complex regex or date parsing)
            // For now, return what we found
            ScrapedMatchData(
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                competition = competition
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping match data", e)
            null
        }
    }
}
