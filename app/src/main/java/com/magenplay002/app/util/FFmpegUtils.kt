package com.magenplay002.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe
import com.arthenica.mobileffmpeg.MediaInformation
import com.arthenica.mobileffmpeg.StreamInformation
import java.io.File

object FFmpegUtils {

    private const val TAG = "FFmpegUtils"

    fun trimVideo(
        inputPath: String,
        outputPath: String,
        startTimeMs: Long,
        endTimeMs: Long,
        onProgress: (Double) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val startTime = formatTimeFFmpeg(startTimeMs)
        val duration = formatTimeFFmpeg(endTimeMs - startTimeMs)

        val command = arrayOf(
            "-i", inputPath,
            "-ss", startTime,
            "-t", duration,
            "-c", "copy",
            "-avoid_negative_ts", "1",
            outputPath
        )

        executeFFmpeg(command, onProgress, onComplete)
    }

    fun convertVideoToMp3(
        inputPath: String,
        outputPath: String,
        bitrate: String = "192k",
        sampleRate: String = "44100",
        onProgress: (Double) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val command = arrayOf(
            "-i", inputPath,
            "-vn",
            "-acodec", "libmp3lame",
            "-ab", bitrate,
            "-ar", sampleRate,
            "-ac", "2",
            outputPath
        )

        executeFFmpeg(command, onProgress, onComplete)
    }

    fun convertVideoToAudio(
        inputPath: String,
        outputPath: String,
        format: String = "mp3",
        bitrate: String = "192k",
        onProgress: (Double) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val codec = when (format) {
            "mp3" -> "libmp3lame"
            "aac" -> "aac"
            "wav" -> "pcm_s16le"
            "flac" -> "flac"
            "ogg" -> "libvorbis"
            "m4a" -> "aac"
            else -> "libmp3lame"
        }

        val command = arrayOf(
            "-i", inputPath,
            "-vn",
            "-acodec", codec,
            "-ab", bitrate,
            "-ar", "44100",
            "-ac", "2",
            outputPath
        )

        executeFFmpeg(command, onProgress, onComplete)
    }

    fun getMediaInfo(inputPath: String): MediaInfo? {
        try {
            val mediaInfo: MediaInformation? = FFprobe.getMediaInformation(inputPath)

            if (mediaInfo != null) {
                val duration = mediaInfo.duration?.toLongOrNull() ?: 0L
                val format = mediaInfo.format ?: ""
                val bitrate = mediaInfo.bitrate?.toLongOrNull() ?: 0L

                val streams = mediaInfo.streams
                var width = 0
                var height = 0
                var videoCodec = ""
                var audioCodec = ""
                var sampleRate = ""
                var channels = ""

                streams?.forEach { stream ->
                    val streamType = stream.type
                    when {
                        streamType == "video" -> {
                            width = stream.width?.toIntOrNull() ?: 0
                            height = stream.height?.toIntOrNull() ?: 0
                            videoCodec = stream.codec ?: ""
                        }
                        streamType == "audio" -> {
                            audioCodec = stream.codec ?: ""
                            sampleRate = stream.sampleRate ?: ""
                            channels = stream.channelLayout ?: ""
                        }
                    }
                }

                return MediaInfo(
                    duration = duration * 1000, // Convert to ms
                    format = format,
                    bitrate = bitrate,
                    width = width,
                    height = height,
                    videoCodec = videoCodec,
                    audioCodec = audioCodec,
                    sampleRate = sampleRate,
                    channels = channels
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting media info", e)
        }
        return null
    }

    private fun executeFFmpeg(
        command: Array<String>,
        onProgress: (Double) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val rc = FFmpeg.execute(command)

        when (rc) {
            com.arthenica.mobileffmpeg.ReturnCode.SUCCESS -> {
                onComplete(true, null)
            }
            com.arthenica.mobileffmpeg.ReturnCode.CANCEL -> {
                onComplete(false, "Operation cancelled")
            }
            else -> {
                val output = com.arthenica.mobileffmpeg.Config.getLastCommandOutput()
                Log.e(TAG, "FFmpeg command failed with rc=$rc: $output")
                onComplete(false, "FFmpeg error: rc=$rc")
            }
        }
    }

    private fun formatTimeFFmpeg(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = (timeMs % 1000) / 10

        return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, millis)
    }

    fun ensureOutputDir(path: String): Boolean {
        val dir = File(path)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else {
            true
        }
    }
}

data class MediaInfo(
    val duration: Long,
    val format: String,
    val bitrate: Long,
    val width: Int,
    val height: Int,
    val videoCodec: String,
    val audioCodec: String,
    val sampleRate: String,
    val channels: String
)
