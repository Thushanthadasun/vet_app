package com.example.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundEffectPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun playCorrect(isMuted: Boolean) {
        if (isMuted) return
        playNotes(listOf(523.25 to 75, 659.25 to 95))
    }

    fun playWrong(isMuted: Boolean) {
        if (isMuted) return
        playNotes(listOf(280.0 to 90, 220.0 to 120))
    }

    fun playWin(isMuted: Boolean) {
        if (isMuted) return
        playNotes(listOf(523.25 to 70, 659.25 to 70, 783.99 to 130))
    }

    fun playTap(isMuted: Boolean) {
        if (isMuted) return
        playNotes(listOf(600.0 to 40))
    }

    private fun playNotes(notes: List<Pair<Double, Int>>) {
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (_, ms) -> (sampleRate * ms) / 1000 }
                if (totalSamples <= 0) return@runCatching
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for ((freq, ms) in notes) {
                    val count = (sampleRate * ms) / 1000
                    for (i in 0 until count) {
                        val env = when {
                            i < count / 8 -> i.toDouble() / (count / 8).coerceAtLeast(1)
                            i > count * 3 / 4 -> (count - i).toDouble() / (count / 4).coerceAtLeast(1)
                            else -> 1.0
                        }
                        val sample = sin(2.0 * PI * freq * i / sampleRate) * env * 0.25
                        buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    offset += count
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                val totalMs = notes.sumOf { it.second }.toLong() + 40L
                kotlinx.coroutines.delay(totalMs)
                track.release()
            }
        }
    }
}
