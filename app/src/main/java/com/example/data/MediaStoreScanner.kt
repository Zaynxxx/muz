package com.example.data

import android.content.Context
import android.provider.MediaStore
import android.util.Log

class MediaStoreScanner(private val context: Context) {

    private val tag = "NovaBeatMediaStoreScanner"

    fun scanLocalSongs(): List<Song> {
        val songList = mutableListOf<Song>()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn).toString()
                    val title = c.getString(titleColumn) ?: "Unknown Title"
                    val artist = c.getString(artistColumn) ?: "Unknown Artist"
                    val album = c.getString(albumColumn) ?: "Unknown Album"
                    val duration = c.getLong(durationColumn)
                    val path = c.getString(pathColumn) ?: ""
                    val contentUri = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$id"

                    // Offline mood analysis and BPM generation based on title metadata
                    val detectedMood = OfflineMoodDetector.detectMood(title, artist)
                    val bpm = OfflineMoodDetector.generateBpm(title)
                    val synchedLyrics = OfflineMoodDetector.generateEmbeddedLyrics(title, artist)

                    songList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = if (duration > 0) duration else 180000L, // Fallback duration
                            uri = contentUri,
                            path = path,
                            mood = detectedMood,
                            bpm = bpm,
                            lyrics = synchedLyrics,
                            isFavorite = false,
                            isDemo = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to query MediaStore: ${e.message}")
        }

        // Combine scanned local physical media with premium high-tech synthetic demos
        val demoSongs = OfflineMoodDetector.getDemoSongs()
        Log.d(tag, "Scanned: ${songList.size} physical songs. Merging with ${demoSongs.size} synthesised demo tracks.")
        
        return songList + demoSongs
    }
}
