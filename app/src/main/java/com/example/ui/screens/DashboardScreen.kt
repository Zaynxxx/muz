package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MusicSpace
import com.example.data.Song
import com.example.ui.MainViewModel

@Composable
fun DashboardTabs(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(68.dp)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.05f)
            ),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("Library", Icons.Filled.Home, 0),
                Triple("Moods", Icons.Filled.GraphicEq, 1),
                Triple("Folders", Icons.Filled.Folder, 2),
                Triple("Space X", Icons.Filled.ColorLens, 3)
            )

            tabs.forEach { (title, icon, index) ->
                val selected = activeTab == index
                val alpha = if (selected) 1f else 0.4f
                val activeColor = if (selected) MaterialTheme.colorScheme.primary else Color.White

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Small glowing indicator dot
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f)
                            )
                            .drawBehind {
                                if (selected) {
                                    drawCircle(
                                        color = activeColor.copy(alpha = 0.4f),
                                        radius = 8.dp.toPx()
                                    )
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = activeColor.copy(alpha = alpha),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = title.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = activeColor.copy(alpha = alpha),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.displaySongs.collectAsState()
    val totalSongs by viewModel.allSongs.collectAsState()
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeSpace by viewModel.activeSpace.collectAsState()

    var activeQuickFilter by remember { mutableStateOf("All") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Futuristic Glow Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HeaderPanel(totalCount = totalSongs.size, spaceId = activeSpace)
        }

        // Search Engine Box
        item {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                placeholder = { Text("Search songs, artists...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "SearchIcon") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        // Dynamic Quick Suggestion Mix Horizontal Cards
        item {
            SectionTitle(title = "Daily Mood Mixes")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                val mixes = listOf(
                    Pair("Cyberpunk Ride", "Party"),
                    Pair("Deep Sleep Orb", "Sleep"),
                    Pair("BPM Cardio Force", "Workout"),
                    Pair("Quantum Focus Draft", "Focus"),
                    Pair("Sunset Lounge", "Chill")
                )
                items(mixes) { (name, mood) ->
                    val color = when (mood) {
                        "Party" -> MaterialTheme.colorScheme.primary
                        "Sleep" -> MaterialTheme.colorScheme.secondary
                        "Workout" -> MaterialTheme.colorScheme.tertiary
                        "Focus" -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                    MixCard(
                        title = name,
                        mood = mood,
                        color = color,
                        onClick = {
                            viewModel.activeMoodFilter.value = mood
                            viewModel.searchQuery.value = ""
                        }
                    )
                }
            }
        }

        // Quick Tab Switcher: "All Songs" vs "Favorites"
        item {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Favorites", "Demos").forEach { filter ->
                    val selected = activeQuickFilter == filter
                    Button(
                        onClick = {
                            activeQuickFilter = filter
                            when (filter) {
                                "All" -> {
                                    viewModel.activeMoodFilter.value = null
                                    viewModel.activeArtistFilter.value = null
                                }
                                "Favorites" -> {
                                    // Filter by favorite songs only
                                    viewModel.activeMoodFilter.value = null
                                }
                                "Demos" -> {
                                    // Custom visual flag
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(filter, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Main Audio Library Song Cards
        val filteredList = songs.filter {
            when (activeQuickFilter) {
                "Favorites" -> it.isFavorite
                "Demos" -> it.isDemo
                else -> true
            }
        }

        if (filteredList.isEmpty()) {
            item {
                EmptyStateView()
            }
        } else {
            items(filteredList, key = { it.id }) { song ->
                val isActive = currentSong?.id == song.id
                SongItemRow(
                    song = song,
                    isPlaying = isActive && isPlaying,
                    isActive = isActive,
                    onPlayClick = { viewModel.selectAndPlaySong(song) },
                    onFavoriteClick = { viewModel.toggleFavorite(song) }
                )
            }
        }
    }
}

@Composable
fun HeaderPanel(totalCount: Int, spaceId: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "NOVABEAT X_ ",
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = spaceId.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Futuristic Offline DSP Synthesizer Hub running locally. Active core detects $totalCount local and demo tracks loaded successfully.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun MixCard(
    title: String,
    mood: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp)
            .clickable { onClick() }
            .testTag("mix_card_$mood"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = mood.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SongItemRow(
    song: Song,
    isPlaying: Boolean,
    isActive: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val outlineColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    val cardBg = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .border(1.dp, outlineColor, RoundedCornerShape(12.dp))
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Wave indicator or demo badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isActive && isPlaying) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = "Playing Animation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        if (song.isDemo) Icons.Filled.Build else Icons.Filled.MusicNote,
                        contentDescription = "Song Icon",
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.isDemo) {
                        Text(
                            text = "DEMO SYNTH • ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        text = "${song.artist} • ${song.mood}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "${song.bpm} BPM",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.testTag("favorite_button_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.HourglassEmpty,
            contentDescription = "Empty Library",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "NO DISCOVERED TRACES",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            "Grant search permission or toggle the DEMO state above.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

// 2. Mood Spaces UI View
@Composable
fun MoodExplorerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentMoodFilter by viewModel.activeMoodFilter.collectAsState()

    val moodsList = listOf(
        Pair("Chill", "Quiet serene frequencies"),
        Pair("Workout", "Ultra upbeat tempos"),
        Pair("Focus", "Coding study triggers"),
        Pair("Sad", "Deep heavy feelings"),
        Pair("Happy", "Glowing golden vibrations"),
        Pair("Sleep", "Sub-low slow waves"),
        Pair("Party", "Neon synth and lead rhythm"),
        Pair("Aggressive", "Heavily distorted beats"),
        Pair("Romantic", "Soft tender acoustics"),
        Pair("Meditation", "Cosmic harmonic bowls")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 100.dp)
    ) {
        Text(
            "OFFLINE AI_SPACE SENSORY DETECTOR",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Tune into custom cerebral audio mood states dynamically identified offline:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(moodsList) { (mood, desc) ->
                val active = currentMoodFilter == mood
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clickable {
                            viewModel.activeMoodFilter.value = if (active) null else mood
                        }
                        .border(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = mood.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        if (currentMoodFilter != null) {
            Button(
                onClick = { viewModel.activeMoodFilter.value = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RESET SENSORY FILTER [${currentMoodFilter?.uppercase()}]")
            }
        }
    }
}

// 3. Folders Explorer View
@Composable
fun FolderExplorerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val directoryRoots = listOf(
        "/storage/emulated/0/Music" to "Main Storage",
        "/storage/emulated/0/Download" to "Web Downloads",
        "/sdcard" to "External SD Card slot",
        "/otg_usb_drive" to "OTG USB Audio DAC",
        "/novabeat_synthetic_assets" to "Simulated Synths Assets"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Text(
                "DEEP FOLDER BROWSER",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Access local partitions, attached SD Cards, OTG USB storage, or synthesizer libraries directly with secure zero-tracking sandbox permissions:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
            )
        }

        items(directoryRoots) { (path, label) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // In emulator or generic environment we fetch music from scanner
                        viewModel.loadSongs()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Folder Item",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = path,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// 4. Customization Hub View
@Composable
fun CustomizationHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeSpace by viewModel.activeSpace.collectAsState()
    val blurIntensity by viewModel.blurIntensity.collectAsState()
    val animationSpeed by viewModel.animationSpeed.collectAsState()
    val visualizerType by viewModel.visualizerType.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Text(
                "NOVABEAT CUSTOMIZATION CORNER",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configure visual aesthetics, skin rendering styles, and ambient audio spaces below.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // Space Theme Grids
        item {
            Text("SELECT DYNAMIC ENVIRONMENT_SPACE", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MusicSpace.values().forEach { space ->
                    val selected = activeSpace == space.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.changeSpaceMode(space) }
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.changeSpaceMode(space) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    space.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    space.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visualizer Styles Selectors
        item {
            Text("VISUALIZER REPRESENTATION", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "circular" to "Circular Wave",
                    "waveform" to "Flat Bars",
                    "neon_particle" to "Neon Particle",
                    "reactive_lighting" to "Ambient Glow"
                ).forEach { (type, label) ->
                    val selected = visualizerType == type
                    Button(
                        onClick = { viewModel.changeVisualizerType(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Layout Sliders (Glassmorphism Opacity)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "GLASSMORPHISM BLUR OPACITY",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = blurIntensity,
                            onValueChange = { viewModel.changeGlassmorphismBlur(it) },
                            valueRange = 0.1f..0.8f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.0f%%", blurIntensity * 100),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "SYSTEM ANIMATION CALIBRATION",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = animationSpeed,
                            onValueChange = { viewModel.updateAnimationSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.1fx", animationSpeed),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Prioritize arrangements (drag layout simulation)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "INTERFACE ELEMENT PRIORITY (DRAG MOCK)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Custom order the Now Playing dashboard segments. Tap element to move to top:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val priorities by viewModel.widgetLayoutPriorities.collectAsState()
                    priorities.forEach { widget ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .clickable {
                                    val updated = priorities.toMutableList()
                                    updated.remove(widget)
                                    updated.add(0, widget)
                                    viewModel.editWidgetLayoutPriorities(updated)
                                }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Menu, "Drag Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = widget.replace("_", " ").uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
