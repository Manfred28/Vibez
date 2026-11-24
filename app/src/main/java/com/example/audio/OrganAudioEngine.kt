package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.GameArc
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Independent client-only preference store for audio mute state (survives save resets). */
interface AudioPreferenceStore {
  fun isMuted(): Boolean

  fun setMuted(muted: Boolean)
}

class SharedPreferencesAudioPreferenceStore(context: Context) : AudioPreferenceStore {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun isMuted(): Boolean = prefs.getBoolean(KEY_MUTED, false)

  override fun setMuted(muted: Boolean) {
    prefs.edit().putBoolean(KEY_MUTED, muted).apply()
  }

  private companion object {
    const val PREFS_NAME = "unnamed_organ_audio_prefs"
    const val KEY_MUTED = "audio_muted_v1"
  }
}

/** Platform audio engine abstraction allowing deterministic fake/no-op injection in JVM tests. */
interface OrganAudioEngine {
  val isMuted: StateFlow<Boolean>

  fun setMuted(muted: Boolean)

  fun toggleMuted(): Boolean {
    val next = !isMuted.value
    setMuted(next)
    return next
  }

  fun playCues(cues: List<SoundCue>, phase: Int)

  fun updateAmbience(profile: AmbientSoundProfile)

  fun setForegroundActive(active: Boolean)

  fun release()
}

/** Lightweight silent engine used for Compose previews and unit tests without audio hardware. */
open class InMemoryAudioEngine(initialMuted: Boolean = false) : OrganAudioEngine {
  private val _isMuted = MutableStateFlow(initialMuted)
  override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  override fun setMuted(muted: Boolean) {
    _isMuted.value = muted
  }

  override fun playCues(cues: List<SoundCue>, phase: Int) = Unit

  override fun updateAmbience(profile: AmbientSoundProfile) = Unit

  override fun setForegroundActive(active: Boolean) = Unit

  override fun release() = Unit
}

/**
 * Lifecycle-aware Android procedural audio engine backed by [AudioTrack].
 * - Pre-caches common phase-invariant PCM waveforms on demand
 * - Throttles rapid taps with per-cue cooldowns and caps concurrent voices
 * - Runs a single low-CPU background ambience stream that pauses automatically when muted or
 *   when the Activity leaves the foreground.
 */
class AndroidOrganAudioEngine(
  private val prefStore: AudioPreferenceStore,
) : OrganAudioEngine {

  private val _isMuted = MutableStateFlow(prefStore.isMuted())
  override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val released = AtomicBoolean(false)
  private val foregroundActive = AtomicBoolean(true)
  private val activeVoices = AtomicInteger(0)
  private val lastPlayedMs = ConcurrentHashMap<SoundCue, Long>()
  private val ambientProfileRef =
    AtomicReference(
      AmbientSoundProfile(
        activeArc = GameArc.ARC_1_CHAMBER,
        phase = 0,
        humGain = 0.24f,
        dripRate = 0.08f,
        fleshDepth = 0f,
        cardiacPulse = 0f,
        stillnessFocus = 0f,
        clampTension = 0f,
        rainMix = 0f,
        complicityBleed = 0f,
      )
    )

  private val sfxExecutor: ExecutorService =
    Executors.newFixedThreadPool(MAX_CONCURRENT_VOICES) { runnable ->
      Thread(runnable, "organ-sfx-voice").apply { isDaemon = true }
    }

  private val ambientThread: Thread =
    Thread({ runAmbientLoop() }, "organ-ambient-bed").apply {
      isDaemon = true
      priority = Thread.NORM_PRIORITY - 1
      start()
    }

  override fun setMuted(muted: Boolean) {
    if (_isMuted.value == muted) return
    _isMuted.value = muted
    prefStore.setMuted(muted)
  }

  override fun playCues(cues: List<SoundCue>, phase: Int) {
    if (cues.isEmpty() || released.get() || _isMuted.value || !foregroundActive.get()) return
    val now = System.currentTimeMillis()
    for (cue in cues) {
      val prev = lastPlayedMs[cue] ?: 0L
      if (now - prev < cue.minIntervalMs) continue
      if (activeVoices.get() >= MAX_CONCURRENT_VOICES) continue
      lastPlayedMs[cue] = now
      val pitchJitter =
        when (cue) {
          SoundCue.VALVE_PRESS, SoundCue.ARC2_STAMP, SoundCue.CREATURE_BUY ->
            0.93f + Random.nextFloat() * 0.14f
          else -> 0.97f + Random.nextFloat() * 0.06f
        }
      try {
        sfxExecutor.execute { playOneShot(cue, phase, pitchJitter) }
      } catch (_: Throwable) {
        // Executor already shut down
      }
    }
  }

  override fun updateAmbience(profile: AmbientSoundProfile) {
    ambientProfileRef.set(profile)
  }

  override fun setForegroundActive(active: Boolean) {
    foregroundActive.set(active)
  }

  override fun release() {
    if (!released.compareAndSet(false, true)) return
    try {
      sfxExecutor.shutdownNow()
    } catch (_: Throwable) {
    }
    try {
      ambientThread.interrupt()
    } catch (_: Throwable) {
    }
  }

  private fun playOneShot(cue: SoundCue, phase: Int, pitchFactor: Float) {
    if (released.get() || _isMuted.value || !foregroundActive.get()) return
    activeVoices.incrementAndGet()
    var track: AudioTrack? = null
    try {
      val pcm = ProceduralFoleySynth.synthesizeCue(cue, phase, pitchFactor)
      if (pcm.isEmpty()) return
      val byteCount = pcm.size * 2
      val minBuf =
        AudioTrack.getMinBufferSize(
          ProceduralFoleySynth.SAMPLE_RATE,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
      if (minBuf <= 0) return
      val attrs =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      val format =
        AudioFormat.Builder()
          .setSampleRate(ProceduralFoleySynth.SAMPLE_RATE)
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
          .build()
      track =
        AudioTrack.Builder()
          .setAudioAttributes(attrs)
          .setAudioFormat(format)
          .setBufferSizeInBytes(maxOf(byteCount, minBuf))
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
      if (track.state != AudioTrack.STATE_INITIALIZED) return
      track.write(pcm, 0, pcm.size)
      track.setVolume(SFX_MASTER_VOLUME)
      track.play()
      val durationMs = ((pcm.size * 1000L) / ProceduralFoleySynth.SAMPLE_RATE) + 35L
      Thread.sleep(durationMs)
    } catch (_: Throwable) {
      // Safe fallback on headless / Robolectric environments without native AudioTrack
    } finally {
      try {
        track?.stop()
      } catch (_: Throwable) {
      }
      try {
        track?.release()
      } catch (_: Throwable) {
      }
      activeVoices.decrementAndGet()
    }
  }

  private fun runAmbientLoop() {
    val generator = ProceduralFoleySynth.AmbientBedGenerator(ProceduralFoleySynth.SAMPLE_RATE)
    val chunk = ShortArray(AMBIENT_CHUNK_SAMPLES)
    var track: AudioTrack? = null
    try {
      while (!released.get()) {
        val shouldPlay = !_isMuted.value && foregroundActive.get()
        if (!shouldPlay) {
          if (track != null) {
            try {
              track.pause()
              track.flush()
            } catch (_: Throwable) {
            }
          }
          Thread.sleep(160L)
          continue
        }

        if (track == null) {
          track = createAmbientTrack()
          if (track == null) {
            Thread.sleep(500L)
            continue
          }
        }

        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
          track.play()
        }
        generator.fillChunk(chunk, ambientProfileRef.get())
        val written = track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
        if (written < 0) {
          Thread.sleep(150L)
        }
      }
    } catch (_: InterruptedException) {
      // Normal shutdown
    } catch (_: Throwable) {
      // Safe fallback when AudioTrack is unavailable
    } finally {
      try {
        track?.stop()
      } catch (_: Throwable) {
      }
      try {
        track?.release()
      } catch (_: Throwable) {
      }
    }
  }

  private fun createAmbientTrack(): AudioTrack? =
    try {
      val minBuf =
        AudioTrack.getMinBufferSize(
          ProceduralFoleySynth.SAMPLE_RATE,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
      if (minBuf <= 0) {
        null
      } else {
        val attrs =
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format =
          AudioFormat.Builder()
            .setSampleRate(ProceduralFoleySynth.SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val created =
          AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(AMBIENT_CHUNK_SAMPLES * 4, minBuf * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        if (created.state == AudioTrack.STATE_INITIALIZED) {
          created.setVolume(AMBIENT_MASTER_VOLUME)
          created
        } else {
          created.release()
          null
        }
      }
    } catch (_: Throwable) {
      null
    }

  private companion object {
    const val MAX_CONCURRENT_VOICES = 4
    const val AMBIENT_CHUNK_SAMPLES = 2205 // 100ms chunks at 22.05kHz
    const val SFX_MASTER_VOLUME = 0.68f
    const val AMBIENT_MASTER_VOLUME = 0.24f
  }
}
