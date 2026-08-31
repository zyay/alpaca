package com.alpaca.app.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Checks GitHub releases for a newer APK. Anonymous public-API call, no user
 * data leaves the device; the download itself goes to the release CDN.
 */
class UpdateClient {

    @Serializable
    data class ReleaseInfo(
        @SerialName("tag_name") val tagName: String,
        @SerialName("name") val name: String? = null,
        @SerialName("body") val body: String? = null,
        @SerialName("html_url") val htmlUrl: String? = null,
        @SerialName("assets") val assets: List<Asset> = emptyList()
    ) {
        val apkAsset: Asset? get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

        @Serializable
        data class Asset(
            val name: String,
            @SerialName("browser_download_url") val browserDownloadUrl: String,
            val size: Long = 0
        )
    }

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatestRelease(): Result<ReleaseInfo> = kotlin.runCatching {
        val request = Request.Builder()
            .url("$GITHUB_API_URL/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("GitHub API returned ${response.code}: ${body.take(200)}")
            }
            json.decodeFromString<ReleaseInfo>(body)
        }
    }

    companion object {
        const val GITHUB_API_URL = "https://api.github.com/repos/zyay/alpaca"

        /**
         * Semantic-ish comparison of "v0.7.0" style tags against the local
         * versionName; unknown segments count as 0, equal versions are not newer.
         */
        fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
            val remote = parse(remoteTag)
            val local = parse(localVersion)
            for (i in 0 until maxOf(remote.size, local.size)) {
                val r = remote.getOrElse(i) { 0 }
                val l = local.getOrElse(i) { 0 }
                if (r != l) return r > l
            }
            return false
        }

        private fun parse(version: String): List<Int> =
            version.trim().removePrefix("v").removePrefix("V")
                .split('.')
                .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    }
}
