package com.gabbonk.anitrack

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

private val Bg = Color(0xFF101116)
private val Panel = Color(0xFF1B1D26)
private val Accent = Color(0xFFB69CFF)
private val Muted = Color(0xFFA5A6B3)

data class Entry(val title: String, val subtitle: String, val url: String, val tag: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AniTrackApp() }
    }
}

@Composable
fun AniTrackApp() {
    var tab by remember { mutableIntStateOf(0) }
    var news by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var airing by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var saved by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Tap refresh to load the latest updates.") }
    val context = LocalContext.current

    suspend fun refresh() {
        loading = true
        message = "Getting the latest updates…"
        try {
            val result = withContext(Dispatchers.IO) {
                Triple(loadNews(), loadAiring(), loadChapters())
            }
            news = result.first
            airing = result.second
            chapters = result.third
            message = "Updated just now"
        } catch (e: Exception) {
            message = "Some feeds could not load. Check your connection and try again."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(refreshKey) { refresh() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg, surface = Panel, primary = Accent,
            onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Bg,
            topBar = {
                Column(Modifier.fillMaxWidth().background(Bg).padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ani", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text("Track", color = Accent, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { if (!loading) refreshKey++ }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AniTrack", tint = Accent)
                        }
                    }
                    Text("Your anime, manga & manhwa tracker", color = Muted, fontSize = 12.sp)
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Article, null) }, label = { Text("News") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Event, null) }, label = { Text("Schedule") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Bookmark, null) }, label = { Text("Library") })
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(message, color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (!loading) { /* state refresh via key */ } }) { }
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                    else TextButton(onClick = { tab = tab }) { Text("Refresh", color = Accent) }
                }
                when (tab) {
                    0 -> NewsScreen(news, onOpen = { openUrl(context, it.url) }, onSave = { item -> if (saved.none { it.url == item.url }) saved = saved + item })
                    1 -> ScheduleScreen(airing, chapters, onOpen = { openUrl(context, it.url) }, onSave = { item -> if (saved.none { it.url == item.url }) saved = saved + item })
                    else -> LibraryScreen(saved, onOpen = { openUrl(context, it.url) }, onRemove = { item -> saved = saved.filterNot { it.url == item.url } })
                }
            }
        }
    }
}

@Composable
fun NewsScreen(items: List<Entry>, onOpen: (Entry) -> Unit, onSave: (Entry) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Latest news", "Updates from anime and manga publishers") }
        if (items.isEmpty()) item { EmptyCard("News feed is loading or temporarily unavailable.") }
        items(items) { item -> EntryCard(item, onOpen, onSave) }
        item { Text("Sources: Anime News Network and Crunchyroll News RSS feeds. Open articles on the publisher's website.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
fun ScheduleScreen(airing: List<Entry>, chapters: List<Entry>, onOpen: (Entry) -> Unit, onSave: (Entry) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Release schedule", "Upcoming episodes and recent chapter releases") }
        item { Text("ANIME EPISODES", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        if (airing.isEmpty()) item { EmptyCard("No airing schedule loaded. AniList may be temporarily unavailable.") }
        items(airing) { EntryCard(it, onOpen, onSave) }
        item { Spacer(Modifier.height(8.dp)); Text("LATEST MANGA / MANHWA CHAPTERS", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        item { Text("These are recent chapter releases, not guaranteed future release dates. Many series do not publish a confirmed schedule.", color = Muted, fontSize = 12.sp) }
        if (chapters.isEmpty()) item { EmptyCard("No chapter releases loaded right now.") }
        items(chapters) { EntryCard(it, onOpen, onSave) }
    }
}

@Composable
fun LibraryScreen(items: List<Entry>, onOpen: (Entry) -> Unit, onRemove: (Entry) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Your library", "Stories and articles you saved on this device") }
        if (items.isEmpty()) item { EmptyCard("Nothing saved yet. Use the bookmark icon on a news or schedule item.") }
        items(items) { item ->
            EntryCard(item, onOpen, onRemove, removeMode = true)
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(subtitle, fontSize = 12.sp, color = Muted)
    }
}

@Composable
fun EmptyCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Text(text, Modifier.padding(18.dp), color = Muted, fontSize = 13.sp)
    }
}

@Composable
fun EntryCard(item: Entry, onOpen: (Entry) -> Unit, onSave: (Entry) -> Unit, removeMode: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(item) },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(item.tag.uppercase(), color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(item.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (item.subtitle.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(item.subtitle, color = Muted, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onSave(item) }) {
                    Icon(if (removeMode) Icons.Default.Delete else Icons.Default.BookmarkAdd, null, tint = Accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (removeMode) "Remove" else "Save", color = Accent)
                }
                TextButton(onClick = { onOpen(item) }) { Text("Open", color = Accent) }
            }
        }
    }
}

private fun openUrl(context: android.content.Context, value: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value))) }
}

private fun httpGet(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    connection.setRequestProperty("User-Agent", "AniTrack-Android/1.0")
    return try { connection.inputStream.bufferedReader().use { it.readText() } }
    finally { connection.disconnect() }
}

private fun loadAiring(): List<Entry> {
    val query = """{"query":"query { Page(page:1,perPage:25) { airingSchedules(notYetAired:true, sort: TIME) { airingAt episode media { title { romaji english } siteUrl } } } }"}"""
    val conn = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.connectTimeout = 12000
    conn.readTimeout = 12000
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("Accept", "application/json")
    conn.outputStream.use { it.write(query.toByteArray()) }
    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
    conn.disconnect()
    val array = json.getJSONObject("data").getJSONObject("Page").getJSONArray("airingSchedules")
    val out = mutableListOf<Entry>()
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val media = obj.getJSONObject("media")
        val titleObj = media.getJSONObject("title")
        val title = titleObj.optString("english").ifBlank { titleObj.optString("romaji") }
        val date = SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault()).format(Date(obj.getLong("airingAt") * 1000))
        out += Entry("$title — Episode ${obj.optInt("episode")}", date, media.optString("siteUrl"), "Upcoming anime")
    }
    return out
}

private fun loadChapters(): List<Entry> {
    val body = httpGet("https://api.mangadex.org/chapter?limit=25&order%5BreadableAt%5D=desc&includes%5B%5D=manga&contentRating%5B%5D=safe&contentRating%5B%5D=suggestive")
    val array = JSONObject(body).optJSONArray("data") ?: JSONArray()
    val out = mutableListOf<Entry>()
    for (i in 0 until array.length()) {
        val chapter = array.getJSONObject(i)
        val attrs = chapter.getJSONObject("attributes")
        val rels = chapter.optJSONArray("relationships") ?: JSONArray()
        var mangaTitle = "Manga / manhwa chapter"
        var mangaId = ""
        for (j in 0 until rels.length()) {
            val rel = rels.getJSONObject(j)
            if (rel.optString("type") == "manga") { mangaId = rel.optString("id"); break }
        }
        if (mangaId.isNotBlank()) {
            runCatching {
                val mangaJson = JSONObject(httpGet("https://api.mangadex.org/manga/$mangaId"))
                val titleMap = mangaJson.getJSONObject("data").getJSONObject("attributes").getJSONObject("title")
                mangaTitle = titleMap.optString("en").ifBlank { titleMap.keys().asSequence().map { titleMap.optString(it) }.firstOrNull().orEmpty() }
            }
        }
        val chapterNo = attrs.optString("chapter").ifBlank { "?" }
        val group = attrs.optString("translatedLanguage", "unknown")
        val id = chapter.optString("id")
        out += Entry("$mangaTitle — Chapter $chapterNo", "Language: $group • ${attrs.optString("readableAt")}", "https://mangadex.org/chapter/$id", "Recent chapter")
    }
    return out
}

private fun loadNews(): List<Entry> {
    val feeds = listOf(
        "https://www.animenewsnetwork.com/all/rss.xml",
        "https://www.crunchyroll.com/news/rss"
    )
    val results = mutableListOf<Entry>()
    for (feed in feeds) {
        runCatching {
            val stream = URL(feed).openConnection().getInputStream()
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
            val nodes = doc.getElementsByTagName("item")
            for (i in 0 until minOf(nodes.length, 15)) {
                val node = nodes.item(i)
                fun value(tag: String) = node.childNodes.let { children ->
                    (0 until children.length).map { children.item(it) }.firstOrNull { it.nodeName == tag }?.textContent?.trim().orEmpty()
                }
                val title = value("title")
                val link = value("link")
                val date = value("pubDate")
                if (title.isNotBlank() && link.startsWith("http")) {
                    results += Entry(title, date, link, if (feed.contains("animenewsnetwork")) "Anime News Network" else "Crunchyroll News")
                }
            }
            stream.close()
        }
    }
    return results
}
