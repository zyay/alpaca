package com.alpaca.app

import com.alpaca.app.data.update.UpdateClient
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateClientTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun newerVersionDetected() {
        assertTrue(UpdateClient.isNewerVersion("v0.7.0", "0.6.0"))
        assertTrue(UpdateClient.isNewerVersion("v1.0.0", "0.9.9"))
        assertTrue(UpdateClient.isNewerVersion("0.6.10", "0.6.9"))
        assertTrue(UpdateClient.isNewerVersion("v0.7", "0.6.5"))
    }

    @Test
    fun sameOrOlderVersionNotDetected() {
        assertFalse(UpdateClient.isNewerVersion("v0.6.0", "0.6.0"))
        assertFalse(UpdateClient.isNewerVersion("0.5.0", "0.6.0"))
        assertFalse(UpdateClient.isNewerVersion("v0.6", "0.6.0"))
    }

    @Test
    fun malformedTagsDoNotTriggerUpdate() {
        // Unparseable segments count as 0 — garbage tags must never prompt an update.
        assertFalse(UpdateClient.isNewerVersion("vX.Y.Z", "0.6.0"))
        assertFalse(UpdateClient.isNewerVersion("", "0.6.0"))
        assertFalse(UpdateClient.isNewerVersion("v0.6.0-beta", "0.6.0"))
    }

    @Test
    fun releasePayloadParsesAndFindsApk() {
        val payload = """
            {
              "tag_name": "v0.7.0",
              "name": "v0.7.0 — Self-updater",
              "body": "## What's new\n- In-app updater",
              "html_url": "https://github.com/zyay/alpaca/releases/tag/v0.7.0",
              "assets": [
                {"name": "app-debug.apk", "browser_download_url": "https://github.com/zyay/alpaca/releases/download/v0.7.0/app-debug.apk", "size": 12345678, "content_type": "application/vnd.android.package-archive"},
                {"name": "source.zip", "browser_download_url": "https://example.com/src.zip", "size": 1}
              ],
              "unknown_future_field": true
            }
        """.trimIndent()

        val release = json.decodeFromString<UpdateClient.ReleaseInfo>(payload)
        assertEquals("v0.7.0", release.tagName)
        assertEquals("app-debug.apk", release.apkAsset?.name)
        assertTrue(release.apkAsset!!.browserDownloadUrl.endsWith(".apk"))
    }

    @Test
    fun releaseWithoutApkAssetHandled() {
        val payload = """{"tag_name": "v9.9.9", "assets": []}"""
        val release = json.decodeFromString<UpdateClient.ReleaseInfo>(payload)
        assertNull(release.apkAsset)
    }
}
