package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object MediaAndVoiceHelper {

    fun encodeImageUriToBase64(context: Context, uri: Uri, maxDim: Int = 640, quality: Int = 72): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(stream)
            stream.close()
            if (original == null) return null

            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val w = (original.width * ratio).toInt().coerceAtLeast(1)
            val h = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = if (w != original.width || h != original.height) {
                Bitmap.createScaledBitmap(original, w, h, true)
            } else original

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size > 320_000) return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun readDocumentToBase64(context: Context, uri: Uri): Triple<String, String, String>? {
        return try {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            var name = "attachment"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(idx) ?: "attachment"
                }
            }
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.size > 320_000) return null
            Triple(name, mime, Base64.encodeToString(bytes, Base64.NO_WRAP))
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}

class KryptVoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): Boolean {
        return try {
            val file = File(context.cacheDir, "krypt_voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioSamplingRate(16000)
            rec.setAudioEncodingBitRate(24000)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            startTimeMs = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            recorder = null
            false
        }
    }

    fun stopAndEncode(): Pair<String, Long>? {
        return try {
            val duration = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(500L)
            recorder?.stop()
            recorder?.release()
            recorder = null
            val file = outputFile ?: return null
            val bytes = file.readBytes()
            file.delete()
            if (bytes.size > 320_000) return null
            Base64.encodeToString(bytes, Base64.NO_WRAP) to duration
        } catch (_: Exception) {
            recorder = null
            null
        }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile?.delete()
    }
}

class KryptVoicePlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun playBase64(base64: String, onFinished: () -> Unit) {
        stop()
        try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val temp = File(context.cacheDir, "krypt_play_${System.currentTimeMillis()}.m4a")
            FileOutputStream(temp).use { it.write(bytes) }
            player = MediaPlayer().apply {
                setDataSource(temp.absolutePath)
                setOnCompletionListener {
                    temp.delete()
                    onFinished()
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            onFinished()
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
