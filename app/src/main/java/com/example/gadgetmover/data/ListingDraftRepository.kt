package com.example.gadgetmover.data

import android.content.Context
import com.example.gadgetmover.screen.listing.ListingDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One save-as-draft snapshot from the listing wizard — see [ListingDraftRepository]. Internal (not private) since [ListingDraft] itself is internal to the app module. */
@Serializable
internal data class SavedListingDraft(
    val id: String,
    val savedAt: Long,
    val draft: ListingDraft,
    /** Which wizard step the seller was on when this was saved — resuming jumps straight back there instead of restarting at step 0. */
    val step: Int = 0
)

/**
 * Persists in-progress "List an item" drafts across app restarts, so a seller who saves a draft
 * (explicitly, or automatically when the app is backgrounded mid-listing — see
 * `ListingWizardScreen`'s lifecycle observer) can resume it from the draft picker shown the next
 * time they start a new listing. Same SharedPreferences + kotlinx.serialization JSON approach as
 * [ProductCache], just keyed to a list of drafts instead of a list of products.
 */
internal object ListingDraftRepository {

    private const val PREFS_NAME = "gadget_mover_prefs"
    private const val KEY_DRAFTS_JSON = "listing_drafts_json"
    private val json = Json { ignoreUnknownKeys = true }

    fun loadAll(context: Context): List<SavedListingDraft> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DRAFTS_JSON, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedListingDraft>>(raw).sortedByDescending { it.savedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Upserts by [SavedListingDraft.id] — saving the same draft again (e.g. tapping "Save as draft" twice) overwrites rather than duplicates. */
    fun save(context: Context, draft: SavedListingDraft) {
        val updated = loadAll(context).filterNot { it.id == draft.id } + draft
        persist(context, updated)
    }

    fun delete(context: Context, id: String) {
        persist(context, loadAll(context).filterNot { it.id == id })
    }

    private fun persist(context: Context, drafts: List<SavedListingDraft>) {
        val raw = try {
            json.encodeToString(drafts)
        } catch (e: Exception) {
            return
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DRAFTS_JSON, raw)
            .apply()
    }
}
