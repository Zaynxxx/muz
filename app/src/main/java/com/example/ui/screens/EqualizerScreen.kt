package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@Composable
fun EqualizerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val eqPreset by viewModel.eqPreset.collectAsState()
    val bands by viewModel.eqBands.collectAsState()
    val bassBoostStrength by viewModel.bassBoostStrength.collectAsState()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsState()

    val presets = listOf(
        "Hi-Fi" to listOf(4f, 3f, 1f, 0f, 1f, 2f, 4f, 5f, 3f, 1f),
        "Bass Boost" to listOf(14f, 12f, 8f, 4f, 0f, 0f, -1f, -2f, -1f, 0f),
        "Vocal Boost" to listOf(-4f, -2f, -1f, 1f, 4f, 8f, 10f, 8f, 3f, 1f),
        "Club" to listOf(6f, 5f, 3f, 0f, 2f, 3f, 5f, 4f, 2f, 0f),
        "Cinema" to listOf(8f, 6f, 3f, -1f, -2f, 2f, 4f, 6f, 3f, -1f),
        "Gaming" to listOf(2f, 4f, 6f, 3f, 0f, 1f, 3f, 5f, 8f, 6f),
        "Car Audio" to listOf(10f, 8f, 4f, 1f, -1f, 2f, 4f, 5f, 2f, -1f),
        "Hi-Res Air" to listOf(-2f, -1f, 0f, 0f, 2f, 4f, 8f, 12f, 14f, 8f),
        "Podcast" to listOf(-10f, -6f, 1f, 5f, 10f, 12f, 8f, 4f, -1f, -6f),
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    )

    val freqLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Equalizer Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "NOVABEAT PRO DSP EQUALIZER",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Frequency Response Calibration Console",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (eqEnabled) "BYPASS_OFF" else "BYPASS_ON",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (eqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { viewModel.toggleEq(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("eq_bypass_switch")
                    )
                }
            }
        }

        // Live Curved Canvas Spline Graph Representation
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Back gridlines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridPath = Path()
                        val rowStep = size.height / 6
                        for (r in 1..5) {
                            gridPath.moveTo(0f, r * rowStep)
                            gridPath.lineTo(size.width, r * rowStep)
                        }
                        val colStep = size.width / 11
                        for (c in 1..10) {
                            gridPath.moveTo(c * colStep, 0f)
                            gridPath.lineTo(c * colStep, size.height)
                        }
                        drawPath(
                            gridPath,
                            color = Color.White.copy(alpha = 0.05f),
                            style = Stroke(width = 0.5.dp.toPx())
                        )
                    }

                    // Active spline curvature of frequency lines
                    val pulseAccent = MaterialTheme.colorScheme.primary
                    val pulseAccentSec = MaterialTheme.colorScheme.tertiary
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val path = Path()
                        val stepX = size.width / 9f
                        val midY = size.height / 2f
                        // Map -15dB to +15dB ranges into vertical constraints
                        val factorY = (size.height / 2f) / 15f

                        if (bands.isNotEmpty()) {
                            val startY = midY - (bands[0] * factorY)
                            path.moveTo(0f, startY)

                            for (i in 1..9) {
                                val destX = i * stepX
                                val destY = midY - (bands[i] * factorY)
                                val previousX = (i - 1) * stepX
                                val previousY = midY - (bands[i - 1] * factorY)
                                // Standard Bezier curve anchors for beautiful sound aesthetics
                                path.cubicTo(
                                    (previousX + destX) / 2f, previousY,
                                    (previousX + destX) / 2f, destY,
                                    destX, destY
                                )
                            }
                            drawPath(
                                path,
                                brush = Brush.horizontalGradient(listOf(pulseAccent, pulseAccentSec)),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }

                    Text(
                        "+15dB_ ",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(6.dp).align(Alignment.TopStart)
                    )
                    Text(
                        "0dB_ ",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(6.dp).align(Alignment.CenterStart)
                    )
                    Text(
                        "-15dB_ ",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(6.dp).align(Alignment.BottomStart)
                    )
                }
            }
        }

        // Horizontal scrolling preset indicators
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(presets) { (name, values) ->
                    val selected = eqPreset == name
                    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    val textCol = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable {
                                viewModel.applyEqPreset(name, values)
                            }
                            .testTag("eq_preset_$name")
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = name.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textCol,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 10 vertical sliders
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    bands.forEachIndexed { i, dbs ->
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Band Slider decibel display
                            Text(
                                text = String.format("%+.0f", dbs),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (eqPreset == "Hi-Fi") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )

                            // Vertical Slider track
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Background slider track line
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                )

                                // Custom slider drag mapping
                                Slider(
                                    value = dbs,
                                    onValueChange = { viewModel.updateEqBand(i, it) },
                                    valueRange = -15f..15f,
                                    enabled = eqEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .width(180.dp)
                                        .graphicsLayerRotated()
                                        .testTag("eq_slider_$i")
                                )
                            }

                            // Frequency Label display (e.g. 500)
                            Text(
                                text = freqLabels[i],
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Dual rotating dials section (Sub-bass amplification & 3D Surround Spatialization)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ADVANCED ACOUSTIC DSP CHANNELS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Bass dial slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BASS EXPLOSION GAIN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Inject clean harmonic sub-bass pressure into playing lines", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Slider(
                            value = bassBoostStrength.toFloat(),
                            onValueChange = { viewModel.updateBassBoostStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            modifier = Modifier.width(140.dp).testTag("bass_boost_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Virtualizer Dial slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("3D SPATIAL SURROUND", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Simulate cinematic wide stellar room reverberations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Slider(
                            value = virtualizerStrength.toFloat(),
                            onValueChange = { viewModel.updateVirtualizerStrength(it.toInt()) },
                            valueRange = 0f..1000f,
                            modifier = Modifier.width(140.dp).testTag("virtual_spatial_slider")
                        )
                    }
                }
            }
        }
    }
}

// Custom extension to rotate standard horizontal sliders vertically for the classic visualizer layout
@Composable
private fun Modifier.graphicsLayerRotated(): Modifier {
    return this.graphicsLayer(
        rotationZ = 270f
    )
}
