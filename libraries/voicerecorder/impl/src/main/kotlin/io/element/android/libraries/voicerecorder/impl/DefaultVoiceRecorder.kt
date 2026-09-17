/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.voicerecorder.impl

import android.Manifest
import androidx.annotation.RequiresPermission
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.appconfig.VoiceMessageConfig
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.coroutine.childScope
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.voicerecorder.api.VoiceRecorder
import io.element.android.libraries.voicerecorder.api.VoiceRecorderState
import io.element.android.libraries.voicerecorder.impl.audio.Audio
import io.element.android.libraries.voicerecorder.impl.audio.AudioConfig
import io.element.android.libraries.voicerecorder.impl.audio.AudioLevelCalculator
import io.element.android.libraries.voicerecorder.impl.audio.AudioReader
import io.element.android.libraries.voicerecorder.impl.audio.Encoder
import io.element.android.libraries.voicerecorder.impl.audio.resample
import io.element.android.libraries.voicerecorder.impl.file.VoiceFileConfig
import io.element.android.libraries.voicerecorder.impl.file.VoiceFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import timber.log.Timber
import java.io.File
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@SingleIn(RoomScope::class)
@ContributesBinding(RoomScope::class)
class DefaultVoiceRecorder(
    private val dispatchers: CoroutineDispatchers,
    private val timeSource: TimeSource,
    private val audioReaderFactory: AudioReader.Factory,
    private val encoder: Encoder,
    private val fileManager: VoiceFileManager,
    private val config: AudioConfig,
    private val fileConfig: VoiceFileConfig,
    private val audioLevelCalculator: AudioLevelCalculator,
    private val matrixClient: MatrixClient,
    @SessionCoroutineScope
    sessionCoroutineScope: CoroutineScope,
) : VoiceRecorder {
    /**
     * The longest note the server will accept: the app limit, or shorter when
     * the homeserver's upload cap says so. A mesh node caps media at a few
     * hundred KiB per hop, and a note that cannot be uploaded is worse than
     * one that stops early. Resolved once per recording, in the background.
     */
    @Volatile
    private var maxDuration: Duration = VoiceMessageConfig.maxVoiceMessageDuration

    private val voiceCoroutineScope by lazy {
        sessionCoroutineScope.childScope(dispatchers.io, "VoiceRecorder-${UUID.randomUUID()}")
    }

    private var outputFile: File? = null
    private var audioReader: AudioReader? = null
    private var recordingJob: Job? = null

    // List of Float between 0 and 1 representing the audio levels
    private val levels: MutableList<Float> = mutableListOf()
    private val lock = Mutex()

    private val _state = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    override val state: StateFlow<VoiceRecorderState> = _state

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun startRecord() {
        Timber.i("Voice recorder started recording")
        outputFile = fileManager.createFile()
            .also(encoder::init)

        lock.withLock {
            levels.clear()
        }

        val audioRecorder = audioReaderFactory.create(config, dispatchers).also { audioReader = it }

        maxDuration = VoiceMessageConfig.maxVoiceMessageDuration
        voiceCoroutineScope.launch {
            matrixClient.getMaxFileUploadSize().onSuccess { bytes ->
                maxDuration = minOf(maxDuration, durationFor(bytes, config.bitRate))
            }
        }

        recordingJob = voiceCoroutineScope.launch {
            val startedAt = timeSource.markNow()
            audioRecorder.record { audio ->
                yield()

                val elapsedTime = startedAt.elapsedNow()

                if (elapsedTime > maxDuration) {
                    Timber.w("Voice message time limit reached")
                    stopRecord(false)
                    return@record
                }

                when (audio) {
                    is Audio.Data -> {
                        val audioLevel = audioLevelCalculator.calculateAudioLevel(audio.buffer)

                        lock.withLock {
                            levels.add(audioLevel)
                            _state.emit(VoiceRecorderState.Recording(elapsedTime, levels.toList()))
                        }
                        encoder.encode(audio.buffer, audio.readSize)
                    }
                    is Audio.Error -> {
                        Timber.e("Voice message error: code=${audio.audioRecordErrorCode}")
                        _state.emit(VoiceRecorderState.Recording(elapsedTime, listOf()))
                    }
                }
            }
        }
    }

    /**
     * Stop the current recording.
     *
     * Call [deleteRecording] to delete any recorded audio.
     */
    override suspend fun stopRecord(
        cancelled: Boolean
    ) {
        recordingJob?.cancel()?.also {
            Timber.i("Voice recorder stopped recording")
        }
        recordingJob = null

        audioReader?.stop()
        audioReader = null
        encoder.release()

        lock.withLock {
            if (cancelled) {
                deleteRecording()
                levels.clear()
            }

            _state.emit(
                when (val file = outputFile) {
                    null -> VoiceRecorderState.Idle
                    else -> {
                        val duration = (state.value as? VoiceRecorderState.Recording)?.elapsedTime
                        VoiceRecorderState.Finished(
                            file = file,
                            mimeType = fileConfig.mimeType,
                            waveform = levels.resample(100),
                            duration = duration ?: 0.milliseconds
                        )
                    }
                }
            )
        }
    }

    /**
     * Stop the current recording and delete the output file.
     */
    override suspend fun deleteRecording() {
        outputFile?.let(fileManager::deleteFile)?.also {
            Timber.i("Voice recorder deleted recording")
        }
        outputFile = null
        _state.emit(VoiceRecorderState.Idle)
    }
}

/**
 * Seconds of audio that fit in [maxUploadBytes] at [bitRate], keeping a tenth
 * back for the Ogg container. Whole seconds, so the limit reads cleanly.
 */
internal fun durationFor(maxUploadBytes: Long, bitRate: Int): Duration {
    val appLimit = VoiceMessageConfig.maxVoiceMessageDuration
    if (maxUploadBytes <= 0 || bitRate <= 0) return appLimit
    // Floating point: a homeserver with no real cap reports something near Long.MAX_VALUE.
    val seconds = maxUploadBytes.toDouble() * 8 * 0.9 / bitRate
    return if (seconds >= appLimit.inWholeSeconds) appLimit else seconds.toLong().seconds
}
