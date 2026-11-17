package com.example.data.network

import android.util.Xml
import androidx.core.text.HtmlCompat
import com.example.data.local.MemeEntity
import com.example.data.local.QuizQuestionEntity
import com.example.data.local.ScrambleWordEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.StringReader
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenTriviaTokenResponse(
    @param:Json(name = "response_code") val responseCode: Int = 0,
    val token: String = "",
)

@JsonClass(generateAdapter = true)
data class OpenTriviaResponse(
    @param:Json(name = "response_code") val responseCode: Int = 0,
    val results: List<OpenTriviaItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OpenTriviaItem(
    val difficulty: String = "medium",
    val question: String = "",
    @param:Json(name = "correct_answer") val correctAnswer: String = "",
    @param:Json(name = "incorrect_answers") val incorrectAnswers: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class TriviaApiV2QuestionText(
    val text: String = "",
)

@JsonClass(generateAdapter = true)
data class TriviaApiV2Item(
    val difficulty: String = "medium",
    val question: TriviaApiV2QuestionText = TriviaApiV2QuestionText(),
    val correctAnswer: String = "",
    val incorrectAnswers: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CustomMemeDto(
    val title: String = "",
    val imageUrl: String = "",
    val sourceName: String = "Friend Meme Feed",
    val sourceUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class CustomMemeWrapper(
    val memes: List<CustomMemeDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CustomQuizDto(
    val question: String = "",
    val correctAnswer: String = "",
    val incorrectAnswers: List<String> = emptyList(),
    val source: String = "Custom Vet JSON",
    val difficulty: String = "medium",
)

@JsonClass(generateAdapter = true)
data class CustomWordDto(
    val word: String = "",
    val clue: String = "",
    val tier: Int = 2,
)

@JsonClass(generateAdapter = true)
data class CustomQuizWrapper(
    val questions: List<CustomQuizDto> = emptyList(),
    val words: List<CustomWordDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class MemeApiResponse(
    val count: Int = 0,
    val memes: List<MemeApiItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class MemeApiItem(
    val postLink: String = "",
    val subreddit: String = "",
    val title: String = "",
    val url: String = "",
    val nsfw: Boolean = false,
    val spoiler: Boolean = false,
    val author: String = "",
    val ups: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MastodonAccountDto(
    val acct: String = "",
)

@JsonClass(generateAdapter = true)
data class MastodonMediaDto(
    val type: String = "",
    val url: String = "",
    @param:Json(name = "preview_url") val previewUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class MastodonStatusDto(
    val url: String = "",
    val content: String = "",
    val sensitive: Boolean = false,
    val account: MastodonAccountDto = MastodonAccountDto(),
    @param:Json(name = "media_attachments") val mediaAttachments: List<MastodonMediaDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RedditListingResponse(
    val data: RedditListingData = RedditListingData(),
)

@JsonClass(generateAdapter = true)
data class RedditListingData(
    val children: List<RedditChild> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RedditChild(
    val data: RedditPostData = RedditPostData(),
)

@JsonClass(generateAdapter = true)
data class RedditPreviewImageSource(
    val url: String = "",
)

@JsonClass(generateAdapter = true)
data class RedditPreviewImage(
    val source: RedditPreviewImageSource = RedditPreviewImageSource(),
)

@JsonClass(generateAdapter = true)
data class RedditPreview(
    val images: List<RedditPreviewImage> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RedditPostData(
    val title: String = "",
    val url: String = "",
    @param:Json(name = "post_hint") val postHint: String = "",
    @param:Json(name = "over_18") val over18: Boolean = false,
    val stickied: Boolean = false,
    val permalink: String = "",
    val subreddit: String = "",
    val ups: Int = 0,
    val preview: RedditPreview? = null,
)

interface OpenTriviaApi {
    @GET("api_token.php")
    suspend fun requestToken(
        @Query("command") command: String = "request",
    ): OpenTriviaTokenResponse

    @GET("api.php")
    suspend fun getAnimalQuestions(
        @Query("amount") amount: Int = 20,
        @Query("category") category: Int = 27,
        @Query("type") type: String = "multiple",
        @Query("token") token: String? = null,
    ): OpenTriviaResponse
}

interface TheTriviaV2Api {
    @GET("v2/questions")
    suspend fun getScienceAnimalQuestions(
        @Query("categories") categories: String = "science",
        @Query("limit") limit: Int = 15,
    ): List<TriviaApiV2Item>
}

data class OpenTdbFetchResult(
    val questions: List<QuizQuestionEntity>,
    val tokenExpired: Boolean,
)

data class CustomQuizAndWordsResult(
    val questions: List<QuizQuestionEntity>,
    val words: List<ScrambleWordEntity>,
)

data class SocialMemeFetchOutcome(
    val memes: List<MemeEntity>,
    val sourcesReached: Int,
    val lastError: String? = null,
)

class FeedNetworkClient {
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val triviaApi: OpenTriviaApi = Retrofit.Builder()
        .baseUrl("https://opentdb.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenTriviaApi::class.java)

    private val triviaV2Api: TheTriviaV2Api = Retrofit.Builder()
        .baseUrl("https://the-trivia-api.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TheTriviaV2Api::class.java)

    suspend fun requestOpenTdbToken(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resp = triviaApi.requestToken()
            if (resp.responseCode == 0 && resp.token.isNotBlank()) resp.token else null
        }.getOrNull()
    }

    suspend fun fetchOpenTriviaAnimalQuestions(token: String?): OpenTdbFetchResult =
        withContext(Dispatchers.IO) {
            val response = triviaApi.getAnimalQuestions(
                amount = 20,
                category = 27,
                type = "multiple",
                token = token?.ifBlank { null },
            )
            if (response.responseCode == 3 || response.responseCode == 4) {
                return@withContext OpenTdbFetchResult(emptyList(), tokenExpired = true)
            }
            if (response.responseCode != 0) {
                return@withContext OpenTdbFetchResult(emptyList(), tokenExpired = false)
            }
            val list = response.results.mapNotNull { item ->
                if (item.incorrectAnswers.size < 3 || item.question.isBlank()) {
                    null
                } else {
                    toQuizEntity(
                        questionText = decodeHtml(item.question),
                        correct = decodeHtml(item.correctAnswer),
                        incorrect = item.incorrectAnswers.take(3).map(::decodeHtml),
                        sourceLabel = "Open Trivia DB (Animals)",
                        difficulty = item.difficulty.ifBlank { "medium" },
                    )
                }
            }
            OpenTdbFetchResult(list, tokenExpired = false)
        }

    suspend fun fetchTheTriviaApiQuestions(): List<QuizQuestionEntity> =
        withContext(Dispatchers.IO) {
            val items = triviaV2Api.getScienceAnimalQuestions()
            items.mapNotNull { item ->
                if (item.question.text.isBlank() || item.correctAnswer.isBlank() || item.incorrectAnswers.size < 3) {
                    null
                } else {
                    toQuizEntity(
                        questionText = item.question.text.trim(),
                        correct = item.correctAnswer.trim(),
                        incorrect = item.incorrectAnswers.take(3).map { it.trim() },
                        sourceLabel = "The Trivia API (Science)",
                        difficulty = item.difficulty.ifBlank { "medium" },
                    )
                }
            }
        }

    /**
     * Fetches live trending memes from multiple public non-scraped social media endpoints:
     * 1) meme-api.com (Reddit trending meme aggregator for SriLankaMemes, wholesomememes, Catmemes, Veterinary, memes)
     * 2) Mastodon public hashtag timelines (#memes, #cats, #srilanka)
     * 3) Reddit JSON fallback with preview-image extraction
     */
    suspend fun fetchPublicSocialMemes(): SocialMemeFetchOutcome = withContext(Dispatchers.IO) {
        val collected = mutableListOf<MemeEntity>()
        val now = System.currentTimeMillis()
        var sourcesReached = 0
        var lastError: String? = null

        // 1. Live Reddit Trending Aggregator (meme-api.com)
        val memeApiSubs = listOf(
            "SriLankaMemes" to 12,
            "Catmemes" to 8,
            "wholesomememes" to 8,
            "Veterinary" to 6,
            "memes" to 8,
        )
        for ((sub, count) in memeApiSubs) {
            try {
                val url = "https://meme-api.com/gimme/$sub/$count"
                val body = fetchRawText(url)
                val parsed = moshi.adapter(MemeApiResponse::class.java).fromJson(body)
                sourcesReached++
                parsed?.memes.orEmpty().forEachIndexed { idx, item ->
                    val imgUrl = item.url.trim()
                    if (!item.nsfw && !item.spoiler && isSupportedImageUrl(imgUrl) && item.title.isNotBlank()) {
                        val subLabel = item.subreddit.ifBlank { sub }
                        val upBadge = if (item.ups > 0) " • ${item.ups}▲" else ""
                        collected += MemeEntity(
                            title = decodeHtml(item.title).trim(),
                            imageUrlOrPath = imgUrl,
                            drawableResName = "",
                            sourceName = "Reddit • r/$subLabel$upBadge",
                            sourceUrl = item.postLink.trim(),
                            isFavorite = false,
                            isUserAdded = false,
                            createdAt = now - collected.size * 30L - idx,
                        )
                    }
                }
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        // 2. Mastodon Public Social Media Tag Timelines (official unauthenticated API)
        val mastodonTags = listOf("memes", "cats", "srilanka")
        val mastodonType = Types.newParameterizedType(List::class.java, MastodonStatusDto::class.java)
        val mastodonAdapter = moshi.adapter<List<MastodonStatusDto>>(mastodonType)

        for (tag in mastodonTags) {
            try {
                val url = "https://mastodon.social/api/v1/timelines/tag/$tag?limit=12"
                val body = fetchRawText(url)
                val statuses = mastodonAdapter.fromJson(body).orEmpty()
                sourcesReached++
                statuses.forEachIndexed { idx, status ->
                    if (status.sensitive) return@forEachIndexed
                    val imageMedia = status.mediaAttachments.firstOrNull {
                        it.type.equals("image", ignoreCase = true) &&
                            (it.url.startsWith("http") || it.previewUrl.startsWith("http"))
                    } ?: return@forEachIndexed
                    val cleanCaption = decodeHtml(status.content)
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .take(180)
                        .ifBlank { "Trending #$tag post" }
                    val imgUrl = imageMedia.url.ifBlank { imageMedia.previewUrl }.trim()
                    collected += MemeEntity(
                        title = cleanCaption,
                        imageUrlOrPath = imgUrl,
                        drawableResName = "",
                        sourceName = "Mastodon • #$tag",
                        sourceUrl = status.url.trim(),
                        isFavorite = false,
                        isUserAdded = false,
                        createdAt = now - collected.size * 30L - idx,
                    )
                }
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        // 3. Direct Reddit JSON fallback (especially for SriLankaMemes & animalsbeingderps)
        val redditSubs = listOf("SriLankaMemes", "SinhalaMemes", "animalsbeingderps")
        for (sub in redditSubs) {
            try {
                val url = "https://www.reddit.com/r/$sub/hot.json?limit=12&raw_json=1"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) PastelPaws/1.0")
                    .header("Accept", "application/json")
                    .get()
                    .build()
                okHttp.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    sourcesReached++
                    val body = resp.body?.string().orEmpty()
                    val parsed = moshi.adapter(RedditListingResponse::class.java).fromJson(body)
                    parsed?.data?.children.orEmpty().forEachIndexed { idx, child ->
                        val post = child.data
                        val previewImg = post.preview?.images?.firstOrNull()?.source?.url?.let(::decodeHtml).orEmpty()
                        val candidateUrl = post.url.trim().let { raw ->
                            if (isSupportedImageUrl(raw)) raw else previewImg
                        }
                        if (!post.over18 && !post.stickied && candidateUrl.startsWith("http") && post.title.isNotBlank()) {
                            val upBadge = if (post.ups > 0) " • ${post.ups}▲" else ""
                            collected += MemeEntity(
                                title = decodeHtml(post.title).trim(),
                                imageUrlOrPath = candidateUrl,
                                drawableResName = "",
                                sourceName = "Reddit • r/${post.subreddit.ifBlank { sub }}$upBadge",
                                sourceUrl = if (post.permalink.startsWith("/")) {
                                    "https://www.reddit.com${post.permalink}"
                                } else "",
                                isFavorite = false,
                                isUserAdded = false,
                                createdAt = now - collected.size * 30L - idx,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        SocialMemeFetchOutcome(
            memes = collected.distinctBy { it.imageUrlOrPath },
            sourcesReached = sourcesReached,
            lastError = lastError,
        )
    }

    /**
     * Supports both Custom JSON feeds AND standard RSS/Atom XML feeds (e.g., RSS.app / FetchRSS bridges
     * for Facebook/Instagram meme pages).
     */
    suspend fun fetchCustomMemeFeed(url: String): List<MemeEntity> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return@withContext emptyList()
        }
        val body = fetchRawText(cleanUrl).trimStart()
        val now = System.currentTimeMillis()

        if (body.startsWith("<")) {
            return@withContext parseRssOrAtomMemes(body, now)
        }

        val dtos: List<CustomMemeDto> = if (body.startsWith("[")) {
            val type = Types.newParameterizedType(List::class.java, CustomMemeDto::class.java)
            moshi.adapter<List<CustomMemeDto>>(type).fromJson(body).orEmpty()
        } else {
            moshi.adapter(CustomMemeWrapper::class.java).fromJson(body)?.memes.orEmpty()
        }
        dtos.mapIndexedNotNull { index, dto ->
            if (dto.title.isBlank() && dto.imageUrl.isBlank()) null
            else MemeEntity(
                title = dto.title.trim(),
                imageUrlOrPath = dto.imageUrl.trim(),
                drawableResName = "",
                sourceName = dto.sourceName.ifBlank { "Custom Sinhala Feed" },
                sourceUrl = dto.sourceUrl.trim(),
                isFavorite = false,
                isUserAdded = false,
                createdAt = now - index,
            )
        }
    }

    private fun parseRssOrAtomMemes(xmlBody: String, now: Long): List<MemeEntity> {
        val results = mutableListOf<MemeEntity>()
        runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlBody))

            var eventType = parser.eventType
            var insideItem = false
            var feedTitle = "Social RSS Feed"
            var title = ""
            var link = ""
            var imageUrl = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name.orEmpty().lowercase()
                        if (tag == "item" || tag == "entry") {
                            insideItem = true
                            title = ""
                            link = ""
                            imageUrl = ""
                        } else if (!insideItem && tag == "title") {
                            val candidate = parser.nextText().trim()
                            if (candidate.isNotBlank()) feedTitle = candidate.take(36)
                        } else if (insideItem) {
                            when (tag) {
                                "title" -> title = decodeHtml(parser.nextText()).trim()
                                "link" -> {
                                    val href = parser.getAttributeValue(null, "href").orEmpty()
                                    val textLink = if (href.isBlank()) parser.nextText().trim() else href.trim()
                                    if (textLink.startsWith("http")) link = textLink
                                }
                                "enclosure", "media:content", "media:thumbnail" -> {
                                    val attrUrl = parser.getAttributeValue(null, "url").orEmpty().trim()
                                    if (attrUrl.startsWith("http") && imageUrl.isBlank()) {
                                        imageUrl = attrUrl
                                    }
                                }
                                "description", "content", "content:encoded", "summary" -> {
                                    val html = parser.nextText()
                                    if (imageUrl.isBlank()) {
                                        val match = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                                            .find(html)
                                        val found = match?.groupValues?.getOrNull(1).orEmpty().trim()
                                        if (found.startsWith("http")) imageUrl = found
                                    }
                                    if (title.isBlank()) {
                                        title = decodeHtml(html).replace(Regex("\\s+"), " ").trim().take(180)
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name.orEmpty().lowercase()
                        if ((tag == "item" || tag == "entry") && insideItem) {
                            insideItem = false
                            if (title.isNotBlank() || imageUrl.isNotBlank()) {
                                results += MemeEntity(
                                    title = title.ifBlank { "Shared post from $feedTitle" },
                                    imageUrlOrPath = imageUrl,
                                    drawableResName = "",
                                    sourceName = feedTitle,
                                    sourceUrl = link,
                                    isFavorite = false,
                                    isUserAdded = false,
                                    createdAt = now - results.size,
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return results
    }

    suspend fun fetchCustomQuizAndWords(url: String): CustomQuizAndWordsResult =
        withContext(Dispatchers.IO) {
            val cleanUrl = url.trim()
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                return@withContext CustomQuizAndWordsResult(emptyList(), emptyList())
            }
            val body = fetchRawText(cleanUrl)
            val questionDtos: List<CustomQuizDto>
            val wordDtos: List<CustomWordDto>
            if (body.trimStart().startsWith("[")) {
                val type = Types.newParameterizedType(List::class.java, CustomQuizDto::class.java)
                questionDtos = moshi.adapter<List<CustomQuizDto>>(type).fromJson(body).orEmpty()
                wordDtos = emptyList()
            } else {
                val wrapper = moshi.adapter(CustomQuizWrapper::class.java).fromJson(body)
                questionDtos = wrapper?.questions.orEmpty()
                wordDtos = wrapper?.words.orEmpty()
            }

            val questions = questionDtos.mapNotNull { dto ->
                if (dto.question.isBlank() || dto.correctAnswer.isBlank() || dto.incorrectAnswers.size < 3) {
                    null
                } else {
                    toQuizEntity(
                        questionText = dto.question.trim(),
                        correct = dto.correctAnswer.trim(),
                        incorrect = dto.incorrectAnswers.take(3).map { it.trim() },
                        sourceLabel = dto.source.ifBlank { "Custom Vet JSON" },
                        difficulty = dto.difficulty.ifBlank { "medium" },
                    )
                }
            }
            val words = wordDtos.mapNotNull { dto ->
                val cleanWord = dto.word.trim().uppercase().filter { it in 'A'..'Z' }
                if (cleanWord.length < 4 || dto.clue.isBlank()) null
                else ScrambleWordEntity(
                    word = cleanWord,
                    clue = dto.clue.trim(),
                    tier = dto.tier.coerceIn(1, 3),
                    isBuiltIn = false,
                )
            }
            CustomQuizAndWordsResult(questions, words)
        }

    private fun isSupportedImageUrl(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val lower = url.lowercase()
        return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".webp") ||
            lower.contains("i.redd.it") ||
            lower.contains("i.imgur.com")
    }

    private fun fetchRawText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) PastelPaws/1.0")
            .get()
            .build()
        okHttp.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}")
            }
            return resp.body?.string().orEmpty()
        }
    }

    private fun toQuizEntity(
        questionText: String,
        correct: String,
        incorrect: List<String>,
        sourceLabel: String,
        difficulty: String,
    ): QuizQuestionEntity {
        val options = (incorrect + correct).shuffled()
        val correctIdx = options.indexOf(correct).coerceAtLeast(0)
        return QuizQuestionEntity(
            question = questionText,
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctIndex = correctIdx,
            sourceLabel = sourceLabel,
            isBuiltIn = false,
            difficulty = difficulty,
        )
    }

    private fun decodeHtml(input: String): String =
        HtmlCompat.fromHtml(input, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
}
