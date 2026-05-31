package com.example.audio

import android.content.Context
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

@OptIn(UnstableApi::class)
class AudioEngine(private val context: Context) {

    private val tag = "NovaBeatAudioEngine"

    // ExoPlayer for standard audio files
    private var exoPlayer: ExoPlayer? = null

    // Native DSP effects
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Thread-safe playback states
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private val _volumeBoost = MutableStateFlow(0) // 0 to 100 extra percent (translates to 0 to 20dB)
    val volumeBoost: StateFlow<Int> = _volumeBoost.asStateFlow()

    private val _currentSpaceId = MutableStateFlow("cyberpunk")
    val currentSpaceId: StateFlow<String> = _currentSpaceId.asStateFlow()

    // Interactive GPU-equivalent reactive FFT values for visuals
    private val _visualizerFrequencies = MutableStateFlow(FloatArray(32))
    val visualizerFrequencies: StateFlow<FloatArray> = _visualizerFrequencies.asStateFlow()

    // Real-time Procedural Synth Engine for offline demos
    private var proceduralSynthTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var progressTrackerJob: Job? = null

    init {
        initializeExoPlayer()
    }

    private fun initializeExoPlayer() {
        try {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 1.0f
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            startTrackProgressAndFft()
                        } else {
                            stopTrackProgressAndFft()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            _playbackDuration.value = duration
                        }
                    }

                    override fun onAudioSessionIdChanged(id: Int) {
                        Log.d(tag, "Audio session ID received: $id")
                        initializeDspEffects(id)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize ExoPlayer: ${e.message}")
        }
    }

    private fun initializeDspEffects(sessionId: Int) {
        if (sessionId == 0) return
        try {
            // Re-release past instances
            loudnessEnhancer?.release()
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()

            // 1. Amplification Booster (Loudness Enhancer)
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = true
                setTargetGain(_volumeBoost.value * 20) // 0 to 100 percent map to 0 to +2000 millibels
            }

            // 2. 10-Band EQ
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }

            // 3. Ultra Bass Booster
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
                setStrength(300.toShort()) // starting 30% bass
            }

            // 4. Stereo Virtualizer
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = true
                setStrength(250.toShort())
            }

            Log.d(tag, "All DSP Audio Effects successfully initialized on session $sessionId")
        } catch (e: Exception) {
            Log.e(tag, "DSP Effects initialization failed: ${e.message}")
        }
    }

    fun updateCurrentSongFavorite(isFav: Boolean) {
        _currentSong.value = _currentSong.value?.copy(isFavorite = isFav)
    }

    // Playback Controller API
    fun playSong(song: Song) {
        // Stop any running synth demos first
        stopProceduralSynth()

        _currentSong.value = song
        _playbackProgress.value = 0L
        _playbackDuration.value = song.duration

        if (song.isDemo) {
            // If it is a demo song, we leverage our offline real-time procedural synthesizer
            exoPlayer?.pause()
            playProceduralSynth(song)
        } else {
            // Standard media playback
            exoPlayer?.let { player ->
                player.clearMediaItems()
                val mediaItem = MediaItem.fromUri(Uri.parse(song.uri))
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    fun togglePlayback() {
        val song = _currentSong.value ?: return
        if (song.isDemo) {
            if (_isPlaying.value) {
                _isPlaying.value = false
                synthJob?.cancel()
            } else {
                _isPlaying.value = true
                playProceduralSynth(song)
            }
        } else {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
        }
    }

    fun seekTo(position: Long) {
        val song = _currentSong.value ?: return
        if (song.isDemo) {
            _playbackProgress.value = position.coerceIn(0, song.duration)
        } else {
            exoPlayer?.seekTo(position)
            _playbackProgress.value = position
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val params = PlaybackParameters(speed, 1.0f)
        exoPlayer?.playbackParameters = params
    }

    fun applyMusicSpace(space: String, bbPower: Int, virtPower: Int) {
        _currentSpaceId.value = space
        // Apply Space specific environmental configurations
        setBassBoostStrength(bbPower)
        setVirtualizerStrength(virtPower)
    }

    // DSP Engine API
    fun setVolumeBoost(extraPercent: Int) {
        _volumeBoost.value = extraPercent.coerceIn(0, 100)
        try {
            loudnessEnhancer?.apply {
                enabled = true
                setTargetGain(_volumeBoost.value * 20) // Up to +2000mB (+20dB)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to apply Loudness Enhancer gain: ${e.message}")
        }
    }

    fun setBassBoostStrength(strength: Int) { // 0 to 1000
        try {
            bassBoost?.apply {
                enabled = strength > 0
                setStrength(strength.toShort())
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to apply Bass Boost: ${e.message}")
        }
    }

    fun setVirtualizerStrength(strength: Int) { // 0 to 1000
        try {
            virtualizer?.apply {
                enabled = strength > 0
                setStrength(strength.toShort())
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to apply Virtualizer: ${e.message}")
        }
    }

    fun setEqualizerBands(bandValues: List<Float>) { // expect values from -15dB to +15dB
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt().coerceAtMost(bandValues.size)
            val minRange = eq.bandLevelRange[0]
            val maxRange = eq.bandLevelRange[1]

            for (i in 0 until numBands) {
                val dbVal = bandValues[i]
                // Scale value to millibel
                val scaledMilliBel = (dbVal * 100).toInt().coerceIn(minRange.toInt(), maxRange.toInt())
                eq.setBandLevel(i.toShort(), scaledMilliBel.toShort())
            }
            Log.v(tag, "EQ band levels applied: $bandValues")
        } catch (e: Exception) {
            Log.e(tag, "Failed to apply EQ Bands: ${e.message}")
        }
    }

    // Direct offline procedural sound synthesize routine (PCM audio writer)
    private fun playProceduralSynth(song: Song) {
        _isPlaying.value = true
        synthJob?.cancel()

        // Configure real-time synthetic melody parameters depending on song metadata
        val bpm = song.bpm
        val beatDelayMs = (60000 / bpm).toLong()
        val mood = song.mood

        // Choose synthesizer notes frequency map
        val notes = when (mood) {
            "Party", "Workout" -> listOf(110.0, 130.81, 146.83, 164.81, 196.0, 220.0) // Aggressive Pentatonic
            "Focus", "Chill" -> listOf(220.0, 261.63, 293.66, 329.63, 392.00, 440.0)  // Lo-Fi Minor Pentatonic
            "Meditation", "Sleep" -> listOf(146.83, 164.81, 220.0, 246.94, 293.66, 440.0) // Healing frequencies
            else -> listOf(261.63, 293.66, 329.63, 349.23, 392.00, 440.0) // Happier Major scale
        }

        // Buffer configurations
        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            proceduralSynthTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            proceduralSynthTrack?.play()
        } catch (e: Exception) {
            Log.e(tag, "AudioTrack synthesis build failed: ${e.message}")
        }

        // Run synthesis stream thread
        synthJob = scope.launch(Dispatchers.Default) {
            val track = proceduralSynthTrack ?: return@launch
            var phase = 0.0
            val buffer = ShortArray(1024)
            var noteIndex = 0
            var noteTimeRemaining = 0L

            var currentFreq = notes[noteIndex]
            val synthVolume = 0.4f // safe standard volume

            while (isActive && _isPlaying.value) {
                // Pick next note according to BPM cadence
                if (noteTimeRemaining <= 0) {
                    noteIndex = (noteIndex + 1) % notes.size
                    currentFreq = notes[noteIndex]
                    noteTimeRemaining = beatDelayMs * 8 // each note plays for 1/8 measure
                }

                // Fill synthetic audio bytes (Sine wave combined with a pulsing subbass envelope)
                for (i in buffer.indices) {
                    val angle = 2.0 * Math.PI * currentFreq / sampleRate
                    phase += angle
                    if (phase > 2.0 * Math.PI) {
                        phase -= 2.0 * Math.PI
                    }

                    // Dynamic wave envelope (fades out at end of note to feel smooth)
                    val envelope = (noteTimeRemaining.toFloat() / (beatDelayMs * 8)).coerceIn(0f, 1f)
                    val value = (sin(phase) * Short.MAX_VALUE * synthVolume * envelope).toInt()
                    buffer[i] = value.toShort()
                }

                // Write short PCM buffer to headphones/device speaker
                try {
                    track.write(buffer, 0, buffer.size)
                } catch (e: Exception) {
                    break
                }

                // Increment positions
                val writtenMs = (buffer.size.toFloat() / sampleRate * 1000).toLong()
                _playbackProgress.value = (_playbackProgress.value + writtenMs).coerceAtMost(song.duration)
                noteTimeRemaining -= writtenMs

                // Generate active reactive FFT parameters based on synthesized melody to animate the canvas 60FPS
                updateSynthesizedVisualSpectrum(currentFreq, _playbackProgress.value)

                if (_playbackProgress.value >= song.duration) {
                    // Start next track or loop
                    withContext(Dispatchers.Main) {
                        seekTo(0)
                    }
                    noteTimeRemaining = 0
                }

                // Tiny sleep to avoid blocking CPU
                delay(writtenMs)
            }
        }
    }

    private fun updateSynthesizedVisualSpectrum(freq: Double, time: Long) {
        val fft = FloatArray(32)
        // Generate beautifully pulsing spectral peaks based on synth note
        val basePos = ((freq / 440.0) * 16).toInt().coerceIn(0, 31)
        val timeFactor = time.toFloat() / 500f

        for (i in 0 until 32) {
            val dist = Math.abs(i - basePos)
            val envelope = Math.max(0.0, 1.0 - (dist * 0.25))
            val ripple = sin(timeFactor + i.toFloat() * 0.4f) * 0.15f + 0.5f
            fft[i] = (envelope.toFloat() * ripple * 0.9f).coerceIn(0.02f, 1.0f)
        }
        _visualizerFrequencies.value = fft
    }

    private fun stopProceduralSynth() {
        synthJob?.cancel()
        synthJob = null
        try {
            proceduralSynthTrack?.stop()
            proceduralSynthTrack?.release()
        } catch (e: Exception) {
            // ignore cleanup errors
        }
        proceduralSynthTrack = null
    }

    // Media progress and FFT generation loops for physical file playback
    private fun startTrackProgressAndFft() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            val randomFft = FloatArray(32)
            while (isActive) {
                if (!_currentSong.value?.isDemo!!) {
                    // Update physical ExoPlayer progress
                    withContext(Dispatchers.Main) {
                        exoPlayer?.let { player ->
                            _playbackProgress.value = player.currentPosition
                        }
                    }

                    // Generate dynamic audio-reactive waveform variables mimicking physical sound streams
                    val time = System.currentTimeMillis() / 200f
                    for (i in 0 until 32) {
                        val base = sin(time + i * 0.35f) * 0.4f + 0.5f
                        // Add some randomized jitter to feel organic and reactive to high beats
                        val jitter = (Math.random() * 0.18).toFloat()
                        randomFft[i] = (base.toFloat() + jitter).coerceIn(0.05f, 1.0f)
                    }
                    _visualizerFrequencies.value = randomFft
                }
                delay(16) // ~60 FPS rate refresh
            }
        }
    }

    private fun stopTrackProgressAndFft() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun release() {
        stopProceduralSynth()
        stopTrackProgressAndFft()
        loudnessEnhancer?.release()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()

        exoPlayer?.release()
        exoPlayer = null
    }
}
