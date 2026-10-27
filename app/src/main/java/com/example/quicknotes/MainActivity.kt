package com.example.quicknotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF17212B)
private val Paper = Color(0xFFF6F7FB)
private val Blue = Color(0xFF2864E8)

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val updated: Long,
    val pinned: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuickNotesApp() }
    }
}

@Composable
fun QuickNotesApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("quick_notes", 0) }
    fun loadNotes(): List<Note> {
        return try {
            val arr = JSONArray(prefs.getString("notes", "[]"))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Note(o.optLong("id"), o.optString("title"), o.optString("body"),
                    o.optLong("updated"), o.optBoolean("pinned"))
            }
        } catch (_: Exception) { emptyList() }
    }
    var notes by remember { mutableStateOf(loadNotes()) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(false) }

    fun persist(items: List<Note>) {
        val arr = JSONArray()
        items.forEach { n ->
            arr.put(JSONObject().put("id", n.id).put("title", n.title)
                .put("body", n.body).put("updated", n.updated).put("pinned", n.pinned))
        }
        prefs.edit().putString("notes", arr.toString()).apply()
    }
    fun save(note: Note) {
        val newList = (notes.filterNot { it.id == note.id } + note)
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updated })
        notes = newList
        persist(newList)
        showEditor = false
        editing = null
    }
    val bg = if (dark) Color(0xFF111820) else Paper
    val card = if (dark) Color(0xFF202B36) else Color.White
    val text = if (dark) Color(0xFFF1F4F8) else Ink

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF8BAAFF), background = bg, surface = card)
                  else lightColorScheme(primary = Blue, background = bg, surface = card)) {
        Surface(modifier = Modifier.fillMaxSize(), color = bg) {
            if (showEditor) {
                NoteEditor(
                    initial = editing,
                    dark = dark,
                    onCancel = { showEditor = false; editing = null },
                    onSave = { save(it) }
                )
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("QUICK NOTES", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("Your thoughts,\nall in one place.", color = text, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                        }
                        TextButton(onClick = { dark = !dark }) { Text(if (dark) "☀" else "☾", fontSize = 24.sp) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth().background(card, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.weight(1f), singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = text, fontSize = 15.sp),
                            decorationBox = { inner -> if (query.isEmpty()) Text("Search your notes...", color = Color.Gray); inner() }
                        )
                        if (query.isNotEmpty()) Icon(Icons.Default.Close, "Clear search",
                            Modifier.clickable { query = "" }, tint = Color.Gray)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("All notes", color = text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text("${notes.size} notes", color = Color.Gray, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    val filtered = notes.filter { it.title.contains(query, true) || it.body.contains(query, true) }
                    if (filtered.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✎", fontSize = 48.sp, color = Blue)
                                Spacer(Modifier.height(8.dp))
                                Text(if (query.isBlank()) "Nothing here yet" else "No matching notes", color = text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (query.isBlank()) "Tap + to capture your first thought." else "Try another search.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)) {
                            items(filtered, key = { it.id }) { note ->
                                NoteCard(note, card, text,
                                    onOpen = { editing = note; showEditor = true },
                                    onPin = {
                                        val updated = notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it }
                                            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updated })
                                        notes = updated; persist(updated)
                                    },
                                    onDelete = {
                                        notes = notes.filterNot { it.id == note.id }; persist(notes)
                                    })
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(bottom = 18.dp), contentAlignment = Alignment.CenterEnd) {
                        FloatingActionButton(onClick = {
                            editing = null; showEditor = true
                        }, containerColor = Blue, contentColor = Color.White) {
                            Icon(Icons.Default.Add, contentDescription = "Create note")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note, card: Color, text: Color,
    onOpen: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(card, RoundedCornerShape(18.dp))
        .clickable(onClick = onOpen).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(note.title.ifBlank { "Untitled note" }, color = text, fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1)
            if (note.pinned) Icon(Icons.Default.PushPin, "Pinned", tint = Blue, modifier = Modifier.size(17.dp))
        }
        if (note.body.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(note.body, color = text.copy(alpha = .72f), fontSize = 14.sp, maxLines = 3, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(note.updated)),
                color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onPin, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.PushPin, "Pin note", tint = if (note.pinned) Blue else Color.Gray, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, "Delete note", tint = Color(0xFFE05B61), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NoteEditor(
    initial: Note?, dark: Boolean, onCancel: () -> Unit, onSave: (Note) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var body by remember(initial?.id) { mutableStateOf(initial?.body ?: "") }
    val text = if (dark) Color(0xFFF1F4F8) else Ink
    val card = if (dark) Color(0xFF202B36) else Color.White
    Column(Modifier.fillMaxSize().background(if (dark) Color(0xFF111820) else Paper).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (title.isNotBlank() || body.isNotBlank()) {
                    onSave(Note(initial?.id ?: System.currentTimeMillis(), title.trim(), body.trim(),
                        System.currentTimeMillis(), initial?.pinned ?: false))
                }
            }) { Text("Save note") }
        }
        Spacer(Modifier.height(22.dp))
        Text("NOTE TITLE", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        BasicTextField(value = title, onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(12.dp)).padding(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = text, fontSize = 23.sp, fontWeight = FontWeight.Bold),
            decorationBox = { inner -> if (title.isEmpty()) Text("Give your note a title", color = Color.Gray, fontSize = 23.sp); inner() })
        Spacer(Modifier.height(20.dp))
        Text("YOUR THOUGHTS", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        BasicTextField(value = body, onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth().weight(1f).background(card, RoundedCornerShape(12.dp)).padding(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = text, fontSize = 16.sp, lineHeight = 24.sp),
            decorationBox = { inner -> if (body.isEmpty()) Text("Start writing...", color = Color.Gray, fontSize = 16.sp); inner() })
        Text("Saved only on this device", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    }
}
