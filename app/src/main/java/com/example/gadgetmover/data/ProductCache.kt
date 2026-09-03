package com.example.gadgetmover.data

import android.content.Context
import com.example.gadgetmover.model.Product
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the last-fetched product list to disk so Home can render something instantly on a
 * cold app start, before the network refresh in `GadgetMoverNavGraph`'s startup `LaunchedEffect`
 * has a chance to complete — that refresh then overwrites both the in-memory list and this cache
 * once it succeeds, so the cache is always at most one session old.
 */
object ProductCache {

    private const val PREFS_NAME = "gadget_mover_prefs"
    private const val KEY_PRODUCTS_JSON = "cached_products_json"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<Product> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRODUCTS_JSON, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, products: List<Product>) {
        val raw = try {
            json.encodeToString(products)
        } catch (e: Exception) {
            return
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRODUCTS_JSON, raw)
            .apply()
    }
}
