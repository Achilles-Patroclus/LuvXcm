package com.fenji.scorcetrace.ui.music

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.fenji.scorcetrace.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页音乐播放器。
 * 播放器实例跟随 ViewModel 生命周期，页面离开时由 UI 主动 pause()，销毁时 release()。
 */
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    val currentTrackTitle: String = Constants.DEFAULT_MUSIC_TITLE
    val currentTrackArtist: String = Constants.DEFAULT_MUSIC_ARTIST

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 播放进度 0f..1f，用于卡片上的进度条 */
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        exoPlayer.setMediaItem(MediaItem.fromUri(Constants.DEFAULT_MUSIC_URL))
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                // 链接失效等致命错误：暂停并提示，不自动重试
                _errorMessage.value = "音乐加载失败（${error.errorCodeName}）"
                _isPlaying.value = false
                _isBuffering.value = false
                exoPlayer.pause()
            }
        })
        exoPlayer.prepare()

        // 轮询播放进度；ExoPlayer 要求在其创建线程（主线程）访问，viewModelScope 正好是 Main
        viewModelScope.launch {
            while (isActive) {
                val duration = exoPlayer.duration
                if (duration > 0L) {
                    _progress.value = (exoPlayer.currentPosition.toFloat() / duration)
                        .coerceIn(0f, 1f)
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    fun play() {
        _errorMessage.value = null
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) pause() else play()
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val PROGRESS_POLL_INTERVAL_MS = 500L
    }
}
