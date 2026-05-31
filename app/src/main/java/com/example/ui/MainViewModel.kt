package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val context = application.applicationContext
    private val preferencesManager = PreferencesManager(context)
    val audioEngine = AudioEngine(context)
    private val mediaStoreScanner = MediaStoreScanner(context)

    // Cached library songs
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // Filter statuses
    val searchQuery = MutableStateFlow("")
    val activeMoodFilter = MutableStateFlow<String?>(null)
    val activeArtistFilter = MutableStateFlow<String?>(null)

    // Filtered song outputs based on interactive UI states
    val displaySongs: StateFlow<List<Song>> = combine(
        _allSongs,
        searchQuery,
        activeMoodFilter,
        activeArtistFilter
    ) { songs, query, mood, artist ->
        songs.filter { song ->
            val matchesQuery = song.title.lowercase().contains(query.lowercase()) ||
                    song.artist.lowercase().contains(query.lowercase())
            val matchesMood = mood == null || song.mood.equals(mood, ignoreCase = true)
            val matchesArtist = artist == null || song.artist.equals(artist, ignoreCase = true)
            matchesQuery && matchesMood && matchesArtist
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Customization Observables
    val activeSpace = preferencesManager.activeSpaceFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "cyberpunk")
    val dspVolumeBoost = preferencesManager.dspVolumeBoostFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val boostWarningShown = preferencesManager.boostWarningShownFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val eqEnabled = preferencesManager.eqEnabledFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val eqPreset = preferencesManager.eqPresetFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "Hi-Fi")
    val eqBands = preferencesManager.eqBandValuesFlow.stateIn(viewModelScope, SharingStarted.Eagerly, listOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f))
    val bassBoostStrength = preferencesManager.bassBoostFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 300)
    val virtualizerStrength = preferencesManager.virtualizerFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 250)
    val blurIntensity = preferencesManager.blurIntensityFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 0.35f)
    val animationSpeed = preferencesManager.animationSpeedFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val visualizerType = preferencesManager.visualizerTypeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, "circular")
    val favoriteSongIds = preferencesManager.favoriteSongsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val historySongIds = preferencesManager.historySongsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Dynamic queue states
    private val _playQueue = MutableStateFlow<List<Song>>(emptyList())
    val playQueue: StateFlow<List<Song>> = _playQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    // Sleep system properties
    private val _sleepTimeRemaining = MutableStateFlow(0L) // in milliseconds
    val sleepTimeRemaining: StateFlow<Long> = _sleepTimeRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Ambient background white noise layer strength (procedural generation sliders)
    val rainVolume = MutableStateFlow(0f)
    val spaceWindVolume = MutableStateFlow(0f)
    val natureForestVolume = MutableStateFlow(0f)

    // Drag-And-Drop Widgets configuration settings (custom arrangements stored)
    private val _widgetLayoutPriorities = MutableStateFlow(listOf("visualizer", "lyrics", "effects_panel", "queue"))
    val widgetLayoutPriorities: StateFlow<List<String>> = _widgetLayoutPriorities.asStateFlow()

    // Physical shake controls (accelerometer)
    private var sensorManager: SensorManager? = null
    private var lastShakeTime = 0L

    init {
        loadSongs()
        observePreferencesToEngine()
        setupShakeHardware()
    }

    fun loadSongs() {
        viewModelScope.launch {
            val scanned = mediaStoreScanner.scanLocalSongs()
            _allSongs.value = scanned
            if (scanned.isNotEmpty() && _playQueue.value.isEmpty()) {
                _playQueue.value = scanned
                _currentQueueIndex.value = 0
            }
        }
    }

    private fun observePreferencesToEngine() {
        // Automatically tie changes inside preferences datastore to native AudioEngine properties on the fly
        viewModelScope.launch {
            combine(eqEnabled, eqBands) { enabled, bands ->
                if (enabled) {
                    audioEngine.setEqualizerBands(bands)
                } else {
                    audioEngine.setEqualizerBands(listOf(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f))
                }
            }.collect()
        }

        viewModelScope.launch {
            bassBoostStrength.collect { value ->
                audioEngine.setBassBoostStrength(value)
            }
        }

        viewModelScope.launch {
            virtualizerStrength.collect { value ->
                audioEngine.setVirtualizerStrength(value)
            }
        }

        viewModelScope.launch {
            dspVolumeBoost.collect { value ->
                audioEngine.setVolumeBoost(value)
            }
        }
    }

    private fun setupShakeHardware() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Playback control interfaces
    fun selectAndPlaySong(song: Song) {
        val q = _playQueue.value
        val index = q.indexOfFirst { it.id == song.id }
        if (index != -1) {
            _currentQueueIndex.value = index
        } else {
            // Append to current queue
            val updatedQueue = q.toMutableList().apply { add(song) }
            _playQueue.value = updatedQueue
            _currentQueueIndex.value = updatedQueue.size - 1
        }

        audioEngine.playSong(song)
        viewModelScope.launch {
            preferencesManager.addSongToHistory(song.id)
        }
    }

    fun togglePlayPause() {
        audioEngine.togglePlayback()
    }

    fun playNext() {
        val q = _playQueue.value
        if (q.isEmpty()) return
        var nextIndex = _currentQueueIndex.value + 1
        if (nextIndex >= q.size) nextIndex = 0
        _currentQueueIndex.value = nextIndex
        audioEngine.playSong(q[nextIndex])
    }

    fun playPrevious() {
        val q = _playQueue.value
        if (q.isEmpty()) return
        var prevIndex = _currentQueueIndex.value - 1
        if (prevIndex < 0) prevIndex = q.size - 1
        _currentQueueIndex.value = prevIndex
        audioEngine.playSong(q[prevIndex])
    }

    fun shuffleQueue() {
        val q = _playQueue.value.toMutableList()
        if (q.size > 1) {
            q.shuffle()
            _playQueue.value = q
            _currentQueueIndex.value = 0
            audioEngine.playSong(q[0])
        }
    }

    // DSP adjustments called from sliders
    fun updateVolumeBoost(percent: Int) {
        viewModelScope.launch {
            preferencesManager.saveDspVolumeBoost(percent)
        }
    }

    fun acknowledgeBoostWarning() {
        viewModelScope.launch {
            preferencesManager.saveBoostWarningShown(true)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            preferencesManager.toggleFavoriteSong(song.id)
            // Reload local songs to match updated favorite checks
            _allSongs.value = _allSongs.value.map {
                if (it.id == song.id) it.copy(isFavorite = !song.isFavorite) else it
            }
            _playQueue.value = _playQueue.value.map {
                if (it.id == song.id) it.copy(isFavorite = !song.isFavorite) else it
            }
            if (audioEngine.currentSong.value?.id == song.id) {
                audioEngine.updateCurrentSongFavorite(!song.isFavorite)
            }
        }
    }

    fun updateEqBand(index: Int, level: Float) {
        val bands = eqBands.value.toMutableList()
        if (index in bands.indices) {
            bands[index] = level
            viewModelScope.launch {
                preferencesManager.saveEqBandValues(bands)
                audioEngine.setEqualizerBands(bands)
            }
        }
    }

    fun toggleEq(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveEqEnabled(enabled)
        }
    }

    fun applyEqPreset(presetName: String, levels: List<Float>) {
        viewModelScope.launch {
            preferencesManager.saveEqPreset(presetName)
            preferencesManager.saveEqBandValues(levels)
            audioEngine.setEqualizerBands(levels)
        }
    }

    fun updateBassBoostStrength(strength: Int) {
        viewModelScope.launch {
            preferencesManager.saveBassBoost(strength)
            audioEngine.setBassBoostStrength(strength)
        }
    }

    fun updateVirtualizerStrength(strength: Int) {
        viewModelScope.launch {
            preferencesManager.saveVirtualizer(strength)
            audioEngine.setVirtualizerStrength(strength)
        }
    }

    // Sleep System
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = minutes * 60 * 1000L
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimeRemaining.value > 0) {
                delay(1000)
                _sleepTimeRemaining.value -= 1000L

                // Gradually fade out ExoPlayer and synth volume during the last 30 seconds
                if (_sleepTimeRemaining.value <= 30000L) {
                    val fadeFactor = _sleepTimeRemaining.value.toFloat() / 30000f
                    // Slow volume reduction to sound natural
                    audioEngine.setVolumeBoost((dspVolumeBoost.value * fadeFactor).toInt())
                }
            }
            // Trigger pause when expired
            if (audioEngine.isPlaying.value) {
                audioEngine.togglePlayback()
            }
            // Reset volume back to original
            audioEngine.setVolumeBoost(dspVolumeBoost.value)
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = 0L
        audioEngine.setVolumeBoost(dspVolumeBoost.value)
    }

    // Visual Customizations Hub
    fun changeSpaceMode(space: MusicSpace) {
        viewModelScope.launch {
            preferencesManager.saveActiveSpace(space.id)

            // Dynamic music environment triggers:
            val bbPower = when (space) {
                MusicSpace.CYBERPUNK -> 950
                MusicSpace.SPACE -> 400
                MusicSpace.RAIN -> 150
                MusicSpace.NIGHT_DRIVE -> 600
                MusicSpace.MEDITATION -> 100
                MusicSpace.RGB_REACTIVE -> 750
                else -> 300
            }

            val virtPower = when (space) {
                MusicSpace.SPACE -> 1000
                MusicSpace.MEDITATION -> 850
                MusicSpace.STUDIO -> 0
                MusicSpace.CYBERPUNK -> 600
                else -> 250
            }

            audioEngine.applyMusicSpace(space.id, bbPower, virtPower)
            // Save settings for bass and virtualizer in preferences too
            preferencesManager.saveBassBoost(bbPower)
            preferencesManager.saveVirtualizer(virtPower)
        }
    }

    fun changeVisualizerType(type: String) {
        viewModelScope.launch {
            preferencesManager.saveVisualizerType(type)
        }
    }

    fun changeGlassmorphismBlur(blur: Float) {
        viewModelScope.launch {
            preferencesManager.saveBlurIntensity(blur)
        }
    }

    fun updateAnimationSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesManager.saveAnimationSpeed(speed)
        }
    }

    fun editWidgetLayoutPriorities(priorities: List<String>) {
        _widgetLayoutPriorities.value = priorities
    }

    // Accelerometer Shake events
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val G = 9.81
        val force = sqrt((x * x + y * y + z * z).toDouble()) / G
        val now = System.currentTimeMillis()

        // Detect a deliberate shake gesture (force above threshold)
        if (force > 1.6 && (now - lastShakeTime > 1500)) {
            lastShakeTime = now
            // Skip to next track on physical shake!
            playNext()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // unused
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(this)
        audioEngine.release()
    }
}
