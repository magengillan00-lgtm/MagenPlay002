package com.magenplay002.app.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

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
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndices = mutableListOf<Int>()
            val muxerTrackIndices = mutableListOf<Int>()

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrackIndex = muxer.addTrack(format)
                    trackIndices.add(i)
                    muxerTrackIndices.add(muxerTrackIndex)
                }
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            val totalDuration = endTimeMs - startTimeMs

            for (trackIdx in trackIndices.indices) {
                val extractorTrack = trackIndices[trackIdx]
                val muxerTrack = muxerTrackIndices[trackIdx]

                extractor.selectTrack(extractorTrack)
                extractor.seekTo(startTimeMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                var currentProgress = 0.0

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val presentationTimeUs = extractor.sampleTime
                    val presentationTimeMs = presentationTimeUs / 1000

                    if (presentationTimeMs > endTimeMs) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.flags = extractor.sampleFlags
                    bufferInfo.presentationTimeUs = presentationTimeUs - (startTimeMs * 1000)

                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)

                    val progress = (presentationTimeMs - startTimeMs).toDouble() / totalDuration.toDouble()
                    if (progress > currentProgress) {
                        currentProgress = progress.coerceIn(0.0, 1.0)
                        onProgress(currentProgress)
                    }

                    extractor.advance()
                }

                extractor.unselectTrack(extractorTrack)
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            onComplete(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming video", e)
            onComplete(false, e.message)
        }
    }

    fun convertVideoToMp3(
        inputPath: String,
        outputPath: String,
        bitrate: String = "192k",
        sampleRate: String = "44100",
        onProgress: (Double) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        extractAudioFromVideo(inputPath, outputPath, onProgress, onComplete)
    }

    fun convertVideoToAudio(
        inputPath: String,
        outputPath: String,
        format: String = "mp3",
        bitrate: String = "192k",
        onProgress: (Double) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        // For simplicity, extract audio as AAC which Android natively supports
        val actualOutputPath = if (format == "mp3" || format == "wav" || format == "flac" || format == "ogg") {
            // For formats not natively supported, save as m4a/aac instead
            outputPath.replace(".${format}", ".m4a")
        } else {
            outputPath
        }

        extractAudioFromVideo(inputPath, actualOutputPath, onProgress, onComplete)
    }

    private fun extractAudioFromVideo(
        inputPath: String,
        outputPath: String,
        onProgress: (Double) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                onComplete(false, "No audio track found in video")
                return
            }

            val audioMime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"

            // Use MUXER_OUTPUT_MPEG_4 for AAC audio, or MUXER_OUTPUT_3GPP for AMR
            val outputFormat = when {
                audioMime.contains("mp4a") || audioMime.contains("aac") ->
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                else ->
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }

            val muxer = MediaMuxer(outputPath, outputFormat)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            extractor.selectTrack(audioTrackIndex)
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            val duration = audioFormat.getLong(MediaFormat.KEY_DURATION)
            var sampleCount = 0L

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.flags = extractor.sampleFlags
                bufferInfo.presentationTimeUs = extractor.sampleTime

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                sampleCount++

                if (duration > 0) {
                    val progress = (bufferInfo.presentationTimeUs.toDouble() / duration.toDouble()).coerceIn(0.0, 1.0)
                    onProgress(progress)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            onComplete(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting audio", e)
            onComplete(false, e.message)
        }
    }

    fun getMediaInfo(inputPath: String): MediaInfo? {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            var duration = 0L
            var width = 0
            var height = 0
            var videoCodec = ""
            var audioCodec = ""
            var sampleRate = ""
            var channels = ""
            var mimeType = ""

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                when {
                    mime.startsWith("video/") -> {
                        duration = try { format.getLong(MediaFormat.KEY_DURATION) / 1000 } catch (_: Exception) { 0L }
                        width = try { format.getInteger(MediaFormat.KEY_WIDTH) } catch (_: Exception) { 0 }
                        height = try { format.getInteger(MediaFormat.KEY_HEIGHT) } catch (_: Exception) { 0 }
                        videoCodec = mime
                    }
                    mime.startsWith("audio/") -> {
                        audioCodec = mime
                        sampleRate = try { format.getInteger(MediaFormat.KEY_SAMPLE_RATE).toString() } catch (_: Exception) { "" }
                        channels = try { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).toString() } catch (_: Exception) { "" }
                    }
                }
            }

            if (duration == 0L) {
                // Try to get duration from the first track
                if (extractor.trackCount > 0) {
                    val format = extractor.getTrackFormat(0)
                    duration = try { format.getLong(MediaFormat.KEY_DURATION) / 1000 } catch (_: Exception) { 0L }
                }
            }

            mimeType = try { extractor.getTrackFormat(0).getString(MediaFormat.KEY_MIME) ?: "" } catch (_: Exception) { "" }

            extractor.release()

            return MediaInfo(
                duration = duration,
                format = mimeType,
                bitrate = 0L,
                width = width,
                height = height,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                sampleRate = sampleRate,
                channels = channels
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting media info", e)
        }
        return null
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
