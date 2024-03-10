package com.example.catfacts.worker

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.catfacts.model.CatFact
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

class CatFactsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val apiUrl = "https://catfact.ninja/facts?limit=13"

    @WorkerThread
    override fun doWork(): Result {
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
        val outputData = Data.Builder()
            .putString("catFacts", Gson().toJson(catFacts))
            .build()
        return Result.success(outputData)
    }
}