package com.example.catfacts.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.catfacts.model.CatFact
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

class CatFactsService : Service() {
    private val apiUrl = "https://catfact.ninja/facts?limit=13"

    private val broadcastIntent = Intent("com.example.catfacts")

    private fun fetchCatFacts() {
        Thread {
            var catFacts: List<CatFact> = emptyList()
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                val json = Gson().fromJson(response, Map::class.java)
                catFacts = (json["data"] as List<Map<String, Any>>).map {
                    CatFact(
                        fact = it["fact"] as String,
                        length = (it["length"] as Double).toInt()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Thread.sleep(5000)
            broadcastIntent.putExtra("catFacts", Gson().toJson(catFacts))
            sendBroadcast(broadcastIntent)
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fetchCatFacts()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}