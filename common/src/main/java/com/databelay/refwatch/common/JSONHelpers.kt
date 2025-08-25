package com.databelay.refwatch.common

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.collections.component1
import kotlin.collections.component2
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

// If gameEventModule is also defined in the common module, you can refer to it directly.
// Otherwise, ensure it's accessible or pass it as a parameter if it varies.
// For simplicity, let's assume gameEventModule is also a top-level val in the common module.

/**
 * The globally shared Json instance for serializing and deserializing
 * game data and events consistently across all modules (mobile, wear, common).
 *
 * It is configured for polymorphic serialization of GameEvent using a
 * class discriminator and includes the necessary serializers module.
 */
val tag = "JSONHelpers"
val AppJsonConfiguration: Json = Json {
    // prettyPrint is useful for debugging, can be false for release builds/data transfer
    // to save space. Consider making this configurable if needed.
    prettyPrint = true // Set to false if you want to optimize for size in transfers

    // This MUST match on both serialization (sending) and deserialization (receiving) ends.
    classDiscriminator = "eventType"

    // Important for compatibility if new fields are added to data classes
    ignoreUnknownKeys = true

    // Ensures that properties with default values are included in the JSON output.
    // Useful if the receiving end relies on these defaults being present.
    encodeDefaults = true

    // The module that contains all serializers for GameEvent subclasses
    // and any other custom serializers needed by Game or its properties.
    serializersModule = gameEventModule // Make sure gameEventModule is accessible here
}

// This function is now correctly placed and used by getGamesFlow
fun parseGameEventsFromDocument(document: DocumentSnapshot): List<GameEvent> {
    val eventMapsFirestore = document.get("events") as? List<Map<String, Any?>> ?: run {
        Log.d(tag, "parseGameEventsFromDocument: Document ${document.id} has no 'events' field, or it's not a list. Returning empty list.")
        return emptyList()
    }

    if (eventMapsFirestore.isEmpty()) {
        Log.d(tag, "parseGameEventsFromDocument: Document ${document.id} has an empty 'events' list (read from Firestore).")
        return emptyList()
    }
    Log.d(tag, "parseGameEventsFromDocument: Document ${document.id} has ${eventMapsFirestore.size} event maps to parse from Firestore.")

    return eventMapsFirestore.mapNotNull { eventMapFromFirestore ->
        // LOG 3: Log the individual event map from Firestore
        Log.v(tag, "parseGameEventsFromDocument: Attempting to decode event map for doc ${document.id}: $eventMapFromFirestore")
        try {
            // 1. Convert Map<String, Any?> from Firestore TO a kotlinx.serialization.json.JsonObject
            val jsonObject = mapToJsonObject(eventMapFromFirestore)
            // LOG 4: Log the JsonObject before ktx.serialization decodes it to GameEvent
            Log.v(tag, "parseGameEventsFromDocument: Converted Firestore map to JsonObject for doc ${document.id}: $jsonObject")

            // 2. Deserialize the JsonObject TO a GameEvent object
            // ktxJson needs classDiscriminator and SerializersModule configured to work here.
            // This also works if GameEvent is a sealed class and setup for polymorphism
            val event: GameEvent = AppJsonConfiguration.decodeFromJsonElement(jsonObject)
            // LOG 5: Log the successfully decoded event
            Log.d(tag, "parseGameEventsFromDocument: Successfully decoded event for doc ${document.id}: $event")
            event
        } catch (e: Exception) {
            // LOG 6: THIS IS VERY IMPORTANT IF EVENTS ARE MISSING or parsing fails
            Log.e(tag, "parseGameEventsFromDocument: Error decoding a single GameEvent from map for doc ${document.id}. Map from Firestore: $eventMapFromFirestore. Error: ", e)
            // Optionally log the JsonObject if the mapToJsonObject conversion seems problematic:
            // try { val problematicJson = mapToJsonObject(eventMapFromFirestore); Log.e(TAG, "Problematic JsonObject: $problematicJson") } catch (jsonEx: Exception) { Log.e(TAG, "Failed to convert map to JsonObject for error logging", jsonEx) }
            null
        }
    }
}
// Helper function to convert a Map<String, Any?> from Firestore to JsonObject
// This needs to handle various types Firestore might return
fun mapToJsonObject(map: Map<String, Any?>): JsonObject {
    val elements = map.mapValues { (_, value) ->
        when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value) // This might turn Int 1 into JsonPrimitive(1.0) if value is Double 1.0
            is Boolean -> JsonPrimitive(value)
            is List<*> -> JsonArray(value.map { anyToJsonElement(it) }) // mapToJsonElement needed
            is Map<*, *> -> mapToJsonObject(value as Map<String, Any?>)
            else -> throw IllegalArgumentException("Unsupported type in map: ${value::class.simpleName}")
        }
    }
    return JsonObject(elements)
}
// Helper needed for mapToJsonElement if not already present
// Helper function to convert JsonElement to Any? for Firestore compatibility
// Make sure this is part of your GameRepository class or accessible to it.
fun jsonElementToAny(jsonElement: JsonElement): Any? {
    return when (jsonElement) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (jsonElement.isString) jsonElement.content
            else if (jsonElement.content == "true" || jsonElement.content == "false") jsonElement.content.toBoolean()
            else jsonElement.content.toDoubleOrNull() ?: jsonElement.content.toLongOrNull() ?: jsonElement.content // Fallback to string if not clearly number/boolean
        }
        is JsonObject -> jsonObjectToMap(jsonElement)
        is JsonArray -> jsonArrayToList(jsonElement)
    }
}

// Helper function to convert JsonObject to Map<String, Any?>
fun jsonObjectToMap(jsonObject: JsonObject): Map<String, Any?> {
    return jsonObject.entries.associate { (key, jsonElement) ->
        key to jsonElementToPrimitive(jsonElement)
    }
}

private fun jsonElementToPrimitive(jsonElement: JsonElement): Any? {
    return when (jsonElement) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (jsonElement.isString) jsonElement.content
            //  CRITICAL PART
            else if (jsonElement.booleanOrNull != null) jsonElement.boolean
            else {
                // Try to preserve integer types if possible
                jsonElement.longOrNull ?: jsonElement.doubleOrNull ?: jsonElement.content
            }
            // END CRITICAL PART
        }
        is JsonObject -> jsonObjectToMap(jsonElement)
        is JsonArray -> jsonArrayToList(jsonElement)
    }
}
// Helper function to convert JsonArray to List<Any?>
fun jsonArrayToList(jsonArray: JsonArray): List<Any?> {
    return jsonArray.map { jsonElementToAny(it) }
}

// Helper function to convert Any? from Firestore map value to JsonElement
fun anyToJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value) // Handles Int, Long, Double, Float
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> {
            // Ensure keys are Strings for JsonObject
            @Suppress("UNCHECKED_CAST")
            mapToJsonObject(value as? Map<String, Any?> ?: emptyMap())
        }
        is List<*> -> buildJsonArray {
            value.forEach { item -> add(anyToJsonElement(item)) }
        }
        else -> {
            // Fallback for unknown types: try converting to string.
            // This might not be ideal for all complex types but can prevent crashes.
            // Consider logging a warning here if you hit this case often.
//                Log.w(TAG, "anyToJsonElement: Encountered an unknown type (${value::class.java.name}), converting to JsonPrimitive string: $value")
            JsonPrimitive(value.toString())
        }
    }
}
