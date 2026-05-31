package com.example.data

import android.content.Context
import android.net.Uri
import java.util.UUID

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val path: String,
    var mood: String,
    val bpm: Int,
    val lyrics: String = "",
    val isFavorite: Boolean = false,
    val isDemo: Boolean = false
) {
    val durationFormatted: String
        get() {
            val seconds = (duration / 1000) % 60
            val minutes = (duration / (1000 * 60)) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val songIds: List<String> = emptyList()
)

enum class MusicSpace(val id: String, val displayName: String, val description: String) {
    CYBERPUNK("cyberpunk", "Cyberpunk Space", "Neon streets, amplified bass, high-contrast neon visualizer"),
    SPACE("space", "Interstellar Hub", "Deep cosmic echoes, spatial surround audio, stellar orbit visualizer"),
    RAIN("rain", "Rain Oasis", "Muted lo-fi beats blended with soothing dynamic offline rain audio"),
    NIGHT_DRIVE("night_drive", "Night Drive", "Warm analog sunset tones, smooth tempo, amber neon trails"),
    MEDITATION("meditation", "Zen Sanctuary", "Slowed pitch-shifted tempo, deep spatial reverb, breathing particle guide"),
    STUDIO("studio", "Studio Console", "Flat response Hi-Fi audio effects, standard technical waveform spectrum"),
    RGB_REACTIVE("rgb_reactive", "Chroma-Glow", "Dynamic spectrum-coupled color sweeping, active neon pulse"),
    RETRO_CASSETTE("retro_cassette", "Vintage Cassette", "Analog tape flutter simulator, vintage warmth, physical rolling wheel UI")
}

object OfflineMoodDetector {
    fun detectMood(title: String, artist: String): String {
        val combined = "$title $artist".lowercase()
        return when {
            combined.contains("sleep") || combined.contains("lullaby") || combined.contains("night") || combined.contains("dream") -> "Sleep"
            combined.contains("calm") || combined.contains("relax") || combined.contains("ambient") || combined.contains("peace") || combined.contains("zen") -> "Meditation"
            combined.contains("workout") || combined.contains("run") || combined.contains("gym") || combined.contains("power") || combined.contains("fit") -> "Workout"
            combined.contains("sad") || combined.contains("cry") || combined.contains("tear") || combined.contains("blue") || combined.contains("lone") -> "Sad"
            combined.contains("focus") || combined.contains("study") || combined.contains("code") || combined.contains("mind") || combined.contains("alpha") -> "Focus"
            combined.contains("party") || combined.contains("club") || combined.contains("dance") || combined.contains("disco") || combined.contains("groove") -> "Party"
            combined.contains("heavy") || combined.contains("rage") || combined.contains("beast") || combined.contains("hard") || combined.contains("doom") -> "Aggressive"
            combined.contains("love") || combined.contains("heart") || combined.contains("romantic") || combined.contains("kiss") -> "Romantic"
            combined.contains("happy") || combined.contains("joy") || combined.contains("sunny") || combined.contains("smile") || combined.contains("bright") -> "Happy"
            else -> "Chill" // Default mood
        }
    }

    fun generateBpm(title: String): Int {
        val hash = title.hashCode()
        return 70 + (Math.abs(hash) % 110) // Returns realistic values between 70 and 180 BPM
    }

    fun generateEmbeddedLyrics(songTitle: String, artist: String): String {
        return """
            [00:00.00] // NovaBeat X AI Synced Lyrics //
            [00:03.00] Currently playing $songTitle - $artist
            [00:08.50] Floating offline inside NovaBeat premium audio studio
            [00:15.00] Enjoy the immersive 60fps GPU reactive visualizer
            [00:22.00] Amplify your hearing up to 200% volume safely
            [00:30.00] Anti-distortion dynamic clippers are shielding your speakers
            [00:40.00] Feeling the beautiful rhythm and the pulse of the bass
            [00:52.00] Seamlessly transitioning across deep local directories
            [01:05.00] Offline smart algorithms have detected the track mood is ${detectMood(songTitle, artist)}
            [01:15.00] Fully private. No servers. No telemetry. Fully yours.
            [01:30.00] Under the cosmic glow of the virtualizer space
            [01:45.00] Tuning the 10-band pro frequency curve sliders
            [02:00.00] NovaBeat X — Redefining offline mobile audio craft
            [02:15.00] Thank you for carrying NovaBeat X inside your pocket!
            [02:30.00] [Outro - Waves dissolving into absolute silence]
        """.trimIndent()
    }

    fun getDemoSongs(): List<Song> {
        return listOf(
            Song(
                id = "demo_neon_rise",
                title = "Cyberpunk Neon Rise",
                artist = "NovaBeat Synthesizer Group",
                album = "Cosmic Cities OST",
                duration = 184000L,
                uri = "demo_neon_rise",
                path = "/internal/assets/demo_neon_rise.wav",
                mood = "Party",
                bpm = 124,
                lyrics = generateEmbeddedLyrics("Cyberpunk Neon Rise", "NovaBeat Synthesizer Group"),
                isDemo = true
            ),
            Song(
                id = "demo_cosmic_pulse",
                title = "Cosmic Star Pulse",
                artist = "Nebula Project",
                album = "Interstellar Odyssey",
                duration = 210000L,
                uri = "demo_cosmic_pulse",
                path = "/internal/assets/demo_cosmic_pulse.wav",
                mood = "Focus",
                bpm = 95,
                lyrics = generateEmbeddedLyrics("Cosmic Star Pulse", "Nebula Project"),
                isDemo = true
            ),
            Song(
                id = "demo_rain_chill",
                title = "Tokyo Rain Chill",
                artist = "Lofi Shogun",
                album = "Midnight Coffee Loops",
                duration = 160000L,
                uri = "demo_rain_chill",
                path = "/internal/assets/demo_rain_chill.wav",
                mood = "Chill",
                bpm = 78,
                lyrics = generateEmbeddedLyrics("Tokyo Rain Chill", "Lofi Shogun"),
                isDemo = true
            ),
            Song(
                id = "demo_zen_garden",
                title = "Zen Sanctuary Meditation",
                artist = "Master Hayashi",
                album = "Inner Tranquility Sessions",
                duration = 240000L,
                uri = "demo_zen_garden",
                path = "/internal/assets/demo_zen_garden.wav",
                mood = "Meditation",
                bpm = 60,
                lyrics = generateEmbeddedLyrics("Zen Sanctuary Meditation", "Master Hayashi"),
                isDemo = true
            ),
            Song(
                id = "demo_bass_workout",
                title = "Hyper-Drive Speed Workout",
                artist = "Viper Force",
                album = "Extreme Audio Overload",
                duration = 145000L,
                uri = "demo_bass_workout",
                path = "/internal/assets/demo_bass_workout.wav",
                mood = "Workout",
                bpm = 140,
                lyrics = generateEmbeddedLyrics("Hyper-Drive Speed Workout", "Viper Force"),
                isDemo = true
            ),
            Song(
                id = "demo_analog_sunset",
                title = "Retro Sunset Drive",
                artist = "80s Arcade Rider",
                album = "VHS Nostalgia Vol. 2",
                duration = 195000L,
                uri = "demo_analog_sunset",
                path = "/internal/assets/demo_analog_sunset.wav",
                mood = "Happy",
                bpm = 110,
                lyrics = generateEmbeddedLyrics("Retro Sunset Drive", "80s Arcade Rider"),
                isDemo = true
            )
        )
    }
}
