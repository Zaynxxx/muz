package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Song
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.NovaBeatTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Acoustic scanner activated.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Perms declined. Running immersive synthetic cores only.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAndRequestStoragePermissions()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val activeSpace by viewModel.activeSpace.collectAsState()

            NovaBeatTheme(spaceId = activeSpace) {
                MainAppLayout(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf("dashboard") } // "dashboard", "now_playing", "equalizer"
    var activeDashboardTab by remember { mutableStateOf(0) } // 0: Home, 1: Moods, 2: Folders, 3: Customization

    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val sleepRemaining by viewModel.sleepTimeRemaining.collectAsState()

    var showSensoryMixDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Glassmorphic top navigation header
            TopAppBarSegment(
                onOpenEqualizer = { currentScreen = if (currentScreen == "equalizer") "dashboard" else "equalizer" },
                onOpenSensoryHub = { showSensoryMixDialog = true },
                currentScreen = currentScreen,
                sleepRemaining = sleepRemaining,
                onCloseNowPlaying = { currentScreen = "dashboard" }
            )
        },
        bottomBar = {
            if (currentScreen == "dashboard") {
                DashboardTabs(
                    activeTab = activeDashboardTab,
                    onTabSelected = { activeDashboardTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Screen rendering container with sliding transitions
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState == "now_playing" || targetState == "equalizer") {
                        (slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut())
                    }
                }
            ) { screen ->
                when (screen) {
                    "dashboard" -> {
                        when (activeDashboardTab) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToNowPlaying = { currentScreen = "now_playing" }
                            )
                            1 -> MoodExplorerScreen(viewModel = viewModel)
                            2 -> FolderExplorerScreen(viewModel = viewModel)
                            3 -> CustomizationHubScreen(viewModel = viewModel)
                        }
                    }
                    "now_playing" -> {
                        NowPlayingScreen(viewModel = viewModel)
                    }
                    "equalizer" -> {
                        EqualizerScreen(viewModel = viewModel)
                    }
                }
            }

            // High-fidelity Floating Mini Player (visible always on other screens)
            if (currentScreen != "now_playing" && currentSong != null) {
                FloatingMiniPlayer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (currentScreen == "dashboard") 14.dp else 16.dp, start = 12.dp, end = 12.dp),
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.playNext() },
                    onClickOpen = { currentScreen = "now_playing" }
                )
            }

            // Sensuous Offline Mixer & Focus Sleep Dialog UI
            if (showSensoryMixDialog) {
                SensoryMixDialog(
                    onDismiss = { showSensoryMixDialog = false },
                    viewModel = viewModel,
                    sleepRemaining = sleepRemaining
                )
            }
        }
    }
}

@Composable
fun TopAppBarSegment(
    onOpenEqualizer: () -> Unit,
    onOpenSensoryHub: () -> Unit,
    currentScreen: String,
    sleepRemaining: Long,
    onCloseNowPlaying: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(64.dp)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentScreen != "dashboard") {
                    IconButton(
                        onClick = onCloseNowPlaying,
                        modifier = Modifier.testTag("back_to_dashboard")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Icon(Icons.Filled.MotionPhotosOn, contentDescription = "Logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (currentScreen) {
                        "equalizer" -> "EQ_CALIBRATOR // v2.0"
                        "now_playing" -> "NOW_PLAYING // CORE_ACTIVE"
                        else -> "NOVABEAT_X // SYSTEM_v2.0"
                    },
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sleek System Status Badge (Sleek Interface)
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .drawBehind {
                                    drawCircle(
                                        color = Color(0xFF22D3EE).copy(alpha = 0.4f),
                                        radius = 4.dp.toPx()
                                    )
                                }
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "OFFLINE",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Active Sleep indicator icon
                if (sleepRemaining > 0) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HourglassEmpty, "Timer", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%02d:%02d", (sleepRemaining / 60000), (sleepRemaining / 1000) % 60),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenSensoryHub,
                    modifier = Modifier.testTag("sensory_mix_button")
                ) {
                    Icon(Icons.Filled.WbSunny, "Offline Sensory Mixer", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier.testTag("equalizer_button")
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        "Equalizer",
                        tint = if (currentScreen == "equalizer") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingMiniPlayer(
    modifier: Modifier = Modifier,
    song: Song,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onClickOpen: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClickOpen() }
            .testTag("floating_mini_player"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rotating visual badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = "Mini Wave",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = song.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_player_next")
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensoryMixDialog(
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    sleepRemaining: Long
) {
    val rainVol by viewModel.rainVolume.collectAsState()
    val spaceWindVol by viewModel.spaceWindVolume.collectAsState()
    val natureForestVol by viewModel.natureForestVolume.collectAsState()

    var sleepMinutes by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE CALIBRATOR_ ", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                "SLEEP & FOCUS SOUND MIXER",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Mix dynamic ambient sound layers directly with playing track stems for focused reading, coding alignment, or deep relaxation:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Rain Noise Row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Misty Tokyo Rainfall Volume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%.0f%%", rainVol * 100), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = rainVol,
                        onValueChange = { viewModel.rainVolume.value = it }
                    )
                }

                // Space Wind Row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Void Stellar Wind Volume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%.0f%%", spaceWindVol * 100), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = spaceWindVol,
                        onValueChange = { viewModel.spaceWindVolume.value = it }
                    )
                }

                // Nature Forest Row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deep Ancient Forest Spires", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(String.format("%.0f%%", natureForestVol * 100), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = natureForestVol,
                        onValueChange = { viewModel.natureForestVolume.value = it }
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Chrono sleep setup
                Column {
                    Text("CHRONO SLIDE SLEEP SHUTDOWN", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                    Text("Fades music volume dynamically down before automatic core bypass completes.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (sleepRemaining > 0) {
                        Button(
                            onClick = { viewModel.stopSleepTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ABORT CHRONO TIMER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = sleepMinutes.toFloat(),
                                onValueChange = { sleepMinutes = it.toInt() },
                                valueRange = 5f..120f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("$sleepMinutes min", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.startSleepTimer(sleepMinutes)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("START SLEEP COOLDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
