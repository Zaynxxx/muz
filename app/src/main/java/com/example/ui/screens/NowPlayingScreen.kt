package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Song
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.audioEngine.currentSong.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val progress by viewModel.audioEngine.playbackProgress.collectAsState()
    val duration by viewModel.audioEngine.playbackDuration.collectAsState()
    val fftValues by viewModel.audioEngine.visualizerFrequencies.collectAsState()

    val dspBoost by viewModel.dspVolumeBoost.collectAsState()
    val warningAcknowledged by viewModel.boostWarningShown.collectAsState()
    val visualizerType by viewModel.visualizerType.collectAsState()
    val blurIntensity by viewModel.blurIntensity.collectAsState()

    val scope = rememberCoroutineScope()
    var activeModuleTab by remember { mutableStateOf("controls") } // "controls", "lyrics", "effects"

    // Halo beat-reactive multiplier based on low sub bass bands (indexes 0 to 3)
    val bassPeak = if (fftValues.isNotEmpty()) (fftValues[0] + fftValues[1] + fftValues[2]) / 3f else 0f
    val scaleFactor by animateFloatAsState(
        targetValue = 1f + (bassPeak * 0.12f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    // Endless visual rotation for spinning tech disk
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    if (currentSong == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.MusicNote, "Music", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("NO ACTIVE CORES PLAYING", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        return
    }

    // Dynamic color glowing edge pulse
    val edgeGlowBrush = Brush.sweepGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = bassPeak * 0.4f),
            MaterialTheme.colorScheme.secondary.copy(alpha = bassPeak * 0.2f),
            Color.Transparent,
            MaterialTheme.colorScheme.tertiary.copy(alpha = bassPeak * 0.4f),
            MaterialTheme.colorScheme.primary.copy(alpha = bassPeak * 0.4f)
        )
    )

    val primaryGlowColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor.copy(alpha = 0.08f * bassPeak),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width
                    )
                )
            }
            .pointerInput(Unit) {
                // Interactive Gesture zone: horizontal swipes skip songs
                detectDragGestures(
                    onDragEnd = { /* custom actions if swipes exceed threshold */ },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.x > 35f) {
                            viewModel.playPrevious()
                        } else if (dragAmount.x < -35f) {
                            viewModel.playNext()
                        }
                    }
                )
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Screen Title header
            HeaderSegment(currentSong = currentSong!!)

            // Holographic Spectrum Canvas block
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Background visualizer
                AudioVisualizerCanvas(
                    fft = fftValues,
                    type = visualizerType,
                    color = MaterialTheme.colorScheme.primary,
                    accent = MaterialTheme.colorScheme.tertiary,
                    scale = scaleFactor
                )

                // Holographic Frame Container (Sleek Interface)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(scaleFactor)
                        .clip(RoundedCornerShape(38.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                )
                            ),
                            RoundedCornerShape(38.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft blurred background gradient ring inside
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Central spinning record disk
                    Box(
                        modifier = Modifier
                            .size(175.dp)
                            .rotate(if (isPlaying) rotationAngle else 0f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                            .border(4.dp, edgeGlowBrush, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Central futuristic orbits
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = size.width / 2.5f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.04f),
                                radius = size.width / 4f,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Rotating disc texture with vinyl simulators
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }

                    // Floating Metadata badge "HI-RES" in bottom-left
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "HI-RES",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Info & Toggles Segments Tab Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "controls" to "PLAY_CORE",
                    "lyrics" to "SYNC_LYRIC",
                    "effects" to "AMPLIFIER"
                ).forEach { (tab, label) ->
                    val active = activeModuleTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { activeModuleTab = tab }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Central dynamic module drawer
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = activeModuleTab,
                    transitionSpec = { (fadeIn() + scaleIn()) togetherWith fadeOut() }
                ) { state ->
                    when (state) {
                        "controls" -> {
                            SongDetailsItem(song = currentSong!!)
                        }
                        "lyrics" -> {
                            SynchronizedLyricsView(
                                rawLyrics = currentSong!!.lyrics,
                                activeProgressMs = progress
                            )
                        }
                        "effects" -> {
                            DspAmplifierView(
                                dspValue = dspBoost,
                                warningAcknowledge = warningAcknowledged,
                                onBoostChange = { viewModel.updateVolumeBoost(it) },
                                onWarningAcknowledgeClick = { viewModel.acknowledgeBoostWarning() }
                            )
                        }
                    }
                }
            }

            // Seekbar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Slider(
                    value = progress.toFloat(),
                    onValueChange = { viewModel.audioEngine.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_seekbar")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMillis(progress),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "${if (currentSong!!.isDemo) "PRO_SYNTH" else "OFFLINE"} // DSP_READY",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = formatMillis(duration),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Bottom Player main controllers row
            ControlsRow(
                isPlaying = isPlaying,
                isFavorite = currentSong!!.isFavorite,
                onPlayPauseToggle = { viewModel.togglePlayPause() },
                onSkipPrev = { viewModel.playPrevious() },
                onSkipNext = { viewModel.playNext() },
                onShuffleToggle = { viewModel.shuffleQueue() },
                onFavoriteToggle = { viewModel.toggleFavorite(currentSong!!) }
            )
        }
    }
}

@Composable
fun HeaderSegment(currentSong: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "NOW ACTIVE_ ",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${currentSong.bpm} BPM // SEAMLESS CHANNELS",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "60FPS DIRECTX_ ",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun SongDetailsItem(song: Song) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = song.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.artist,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "ALBUM CORE: ${song.album.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// 1. Synced Karaoke Lyrics Component
@Composable
fun SynchronizedLyricsView(
    rawLyrics: String,
    activeProgressMs: Long
) {
    // Basic LRC Timing parser: parses "[mm:ss.SS] Text" lines
    val lines = remember(rawLyrics) {
        val parsed = mutableListOf<Pair<Long, String>>()
        rawLyrics.split("\n").forEach { rawLine ->
            val clean = rawLine.trim()
            if (clean.startsWith("[") && clean.contains("]")) {
                val bracketEnd = clean.indexOf("]")
                val timePart = clean.substring(1, bracketEnd)
                val lyricPart = clean.substring(bracketEnd + 1).trim()
                val parts = timePart.split(":")
                if (parts.size == 2) {
                    val minutes = parts[0].toLongOrNull() ?: 0L
                    val secondsParts = parts[1].split(".")
                    val seconds = secondsParts[0].toLongOrNull() ?: 0L
                    val fraction = if (secondsParts.size > 1) secondsParts[1].toLongOrNull() ?: 0L else 0L
                    val totalMs = (minutes * 60 + seconds) * 1000L + fraction * 10L
                    parsed.add(Pair(totalMs, lyricPart))
                }
            }
        }
        parsed.sortedBy { it.first }
    }

    // Active line identifier
    val activeIndex = remember(lines, activeProgressMs) {
        var index = -1
        for (i in lines.indices) {
            if (activeProgressMs >= lines[i].first) {
                index = i
            } else {
                break
            }
        }
        index
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (activeIndex != -1) {
            // Smoothly auto scroll lyrics columns to focus the karaoke indicator
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lines.size) { i ->
                val (time, text) = lines[i]
                val isActive = i == activeIndex
                val sizeVal = if (isActive) 16.sp else 13.sp
                val colorVal = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                val weightVal = if (isActive) FontWeight.ExtraBold else FontWeight.Normal

                Text(
                    text = text,
                    fontSize = sizeVal,
                    fontWeight = weightVal,
                    color = colorVal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }
    }
}

// 2. 200% Volume Booster DSP Drawer panel
@Composable
fun DspAmplifierView(
    dspValue: Int,
    warningAcknowledge: Boolean,
    onBoostChange: (Int) -> Unit,
    onWarningAcknowledgeClick: () -> Unit
) {
    val needsWarning = dspValue > 25 && !warningAcknowledge

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "NOVABEAT 200%_ SIGNAL BOOST DSP",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "AMPLITUDE SCALE_ ",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "${100 + dspValue}%",
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (dspValue > 25) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = dspValue.toFloat(),
            onValueChange = {
                onBoostChange(it.toInt())
            },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth().testTag("amp_boost_slider")
        )

        AnimatedVisibility(
            visible = dspValue > 0,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (needsWarning) {
                // Glow warning card alerting speakers blast out risk
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "🚨 ACOUSTIC WARNING: HIGH AMPLIFIER DETECTED",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Extra gain above 125% (+5dB) risks severe clipping distortion, and might permanently blow out sensitive phone/headphone elements. Use with care.",
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onWarningAcknowledgeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("ACKNOWLEDGE & MUTILITY RISK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🛡️ ANTI-CLIPPING DYNAMIC DIGITAL CLIPPER CORES RUNNING SECURELY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// 3. Complete Drawing Canvases details
@Composable
fun AudioVisualizerCanvas(
    fft: FloatArray,
    type: String,
    color: Color,
    accent: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("audio_visualizer_canvas")
    ) {
        val count = fft.size.coerceAtMost(32)
        if (count == 0 || fft.isEmpty()) return@Canvas

        when (type) {
            "circular" -> {
                // Drawing 60fps circular orbital spectrum rays
                val centerOffset = center
                val baseRadius = (size.width / 3.4f) * scale
                val maxBarHeight = 60.dp.toPx()

                for (i in 0 until count) {
                    val angleDegree = (i * (360f / count))
                    val angleRad = Math.toRadians(angleDegree.toDouble())
                    val normalizedVal = fft[i]

                    val startX = (centerOffset.x + baseRadius * cos(angleRad)).toFloat()
                    val startY = (centerOffset.y + baseRadius * sin(angleRad)).toFloat()

                    val endRadius = baseRadius + (normalizedVal * maxBarHeight)
                    val endX = (centerOffset.x + endRadius * cos(angleRad)).toFloat()
                    val endY = (centerOffset.y + endRadius * sin(angleRad)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(color, accent),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = (5.dp.toPx() * scale),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            "waveform" -> {
                // Classic technical spectrum bars at baseline
                val barSpacing = size.width / count
                val bottomY = size.height * 0.9f
                val barWidth = barSpacing * 0.7f

                for (i in 0 until count) {
                    val valH = fft[i] * (size.height * 0.7f)
                    val posX = i * barSpacing + (barSpacing * 0.15f)

                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(color, accent)),
                        topLeft = Offset(posX, bottomY - valH),
                        size = androidx.compose.ui.geometry.Size(barWidth, valH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
            "neon_particle" -> {
                // Bouncing particle dots exploding towards corners
                val baseRadius = (size.width / 4.5f) * scale
                for (i in 0 until count) {
                    val angleDegree = (i * (360f / count)) + (System.currentTimeMillis() / 80f)
                    val angleRad = Math.toRadians(angleDegree.toDouble())
                    val strength = fft[i]

                    // Particle shifts dynamic position outwards relative to beat frequency
                    val drift = baseRadius + (strength * 100.dp.toPx())
                    val posX = (center.x + drift * cos(angleRad)).toFloat()
                    val posY = (center.y + drift * sin(angleRad)).toFloat()

                    drawCircle(
                        color = if (i % 2 == 0) color else accent,
                        radius = (strength * 10.dp.toPx() * scale).coerceAtLeast(3.dp.toPx()),
                        center = Offset(posX, posY)
                    )
                }
            }
            "reactive_lighting" -> {
                // Soft sweeping cinematic overlay spotlights
                drawCircle(
                    color = color.copy(alpha = 0.15f * scale),
                    radius = size.width / 2.5f,
                    center = center
                )
                drawCircle(
                    color = accent.copy(alpha = 0.1f * scale),
                    radius = size.width / 3.5f,
                    center = Offset(center.x + 30.dp.toPx(), center.y - 30.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun ControlsRow(
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.testTag("shuffle_button")
        ) {
            Icon(Icons.Default.Shuffle, "Shuffle", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        IconButton(
            onClick = onSkipPrev,
            modifier = Modifier
                .size(48.dp)
                .testTag("skip_prev_button")
        ) {
            Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface)
        }

        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable { onPlayPauseToggle() }
                .drawBehind {
                    // Sleek Interface soft glowing background shadow wrapper
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = size.width / 2f + 8.dp.toPx()
                    )
                }
                .testTag("play_pause_fab"),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause Toggle",
                    modifier = Modifier.size(36.dp),
                    tint = Color.Black
                )
            }
        }

        IconButton(
            onClick = onSkipNext,
            modifier = Modifier
                .size(48.dp)
                .testTag("skip_next_button")
        ) {
            Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface)
        }

        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier.testTag("fav_button_np")
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite Toggle",
                tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatMillis(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
