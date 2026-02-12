
//ty leo <3

package com.peachsoju.handlers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import java.io.File

object FileHandler {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configDir: File by lazy {
        FabricLoader.getInstance().configDir.resolve("peachsoju").toFile().also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun writeToFile(fileName: String, json: JsonObject) {
        try {
            val file = File(configDir, fileName)
            val tmp = File(configDir, "$fileName.tmp")

            tmp.writeText(gson.toJson(json))
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun readFromFile(fileName: String): JsonObject {
        return try {
            val file = File(configDir, fileName)
            if (!file.exists()) return JsonObject()

            val text = file.readText()
            if (text.isBlank()) return JsonObject()

            JsonParser.parseString(text).asJsonObject
        } catch (e: Exception) {
            e.printStackTrace()
            JsonObject()
        }
    }

}