package com.fenji.scorcetrace.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.fenji.scorcetrace.data.player.DEFAULT_PLAYLIST
import com.fenji.scorcetrace.data.player.MusicPlayerManager
import com.fenji.scorcetrace.data.player.TrackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 音乐播放器的状态层。
 *
 * 底层 ExoPlayer 由应用级单例 [MusicPlayerManager] 持有，本类只做状态同步与转发，
 * 因此它随页面销毁也不会打断播放；切底部 Tab 时音乐继续。
 */
@HiltViewModel
class MusicPlayerViewModel @Inject constructor(
    private val playerManager: MusicPlayerManager,
) : ViewModel() {

    val playlist: List<TrackInfo> = DEFAULT_PLAYLIST

    private val player: Player get() = playerManager.exoPlayer

    private val _currentIndex = MutableStateFlow(player.currentMediaItemIndex)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentTrack = MutableStateFlow(
        DEFAULT_PLAYLIST.getOrElse(player.currentMediaItemIndex) { DEFAULT_PLAYLIST.first() },
    )
    val currentTrack: StateFlow<TrackInfo> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(player.isPlaying)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(player.playbackState == Player.STATE_BUFFERING)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 播放进度 0f..1f，用于卡片上的进度条 */
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = player.currentMediaItemIndex
            _currentIndex.value = index
            _currentTrack.value = DEFAULT_PLAYLIST.getOrElse(index) { _currentTrack.value }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
        }

        override fun onPlayerError(error: PlaybackException) {
            // 链接失效等致命错误：暂停并提示，不自动重试
            _errorMessage.value = "音乐加载失败（${error.errorCodeName}）"
            _isPlaying.value = false
            _isBuffering.value = false
            playerManager.pause()
        }
    }

    /** 本次界面存活期间是否已经处理过「进入应用自动播放」，避免切回首页时重复触发 */
    private var autoPlayHandled = false

    init {
        player.addListener(listener)

        // 轮询播放进度；ExoPlayer 要求在其创建线程（主线程）访问，viewModelScope 正好是 Main
        viewModelScope.launch {
            while (isActive) {
                val duration = player.duration
                if (duration > 0L) {
                    _progress.value = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                }
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * 按偏好决定是否自动播放，且**每个 ViewModel 实例只处理一次**：
     * 已在播放或已处理过就什么都不做，因此切回首页不会重启/打断当前播放。
     */
    fun autoPlayOnce(enabled: Boolean) {
        if (autoPlayHandled) return
        autoPlayHandled = true
        if (!enabled || player.isPlaying) return
        play()
    }

    fun play() {
        _errorMessage.value = null
        playerManager.play()
    }

    fun pause() {
        playerManager.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    /** 下一首：切歌后直接续播，符合「点一下就能听」的直觉。 */
    fun skipToNext() {
        playerManager.skipToNext()
        play()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
        play()
    }

    /** 从播放列表直接选曲播放。 */
    fun playTrack(index: Int) {
        playerManager.setPlaylist(DEFAULT_PLAYLIST, index)
        play()
    }

    override fun onCleared() {
        // 只摘掉自己的监听；播放器是应用级单例，绝不在这里释放
        player.removeListener(listener)
        super.onCleared()
    }

    private companion object {
        const val PROGRESS_POLL_INTERVAL_MS = 500L
    }
}
