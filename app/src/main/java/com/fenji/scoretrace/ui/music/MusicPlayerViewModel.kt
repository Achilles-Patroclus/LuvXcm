package com.fenji.scoretrace.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.fenji.scoretrace.data.player.DEFAULT_PLAYLIST
import com.fenji.scoretrace.data.player.MusicPlayerManager
import com.fenji.scoretrace.data.player.TrackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 循环模式的 UI 三态：关闭 / 列表循环 / 单曲循环。 */
enum class RepeatMode { OFF, ALL, ONE }

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

    val playlist: StateFlow<List<TrackInfo>> = playerManager.playlist

    private val player: Player get() = playerManager.exoPlayer

    private val _currentIndex = MutableStateFlow(player.currentMediaItemIndex)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentTrack = MutableStateFlow(
        playerManager.playlist.value.getOrElse(player.currentMediaItemIndex) {
            playerManager.playlist.value.firstOrNull() ?: DEFAULT_PLAYLIST.first()
        },
    )
    val currentTrack: StateFlow<TrackInfo> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(player.isPlaying)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(player.repeatMode.toUiRepeatMode())
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(player.shuffleModeEnabled)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = player.currentMediaItemIndex
            _currentIndex.value = index
            _currentTrack.value = playerManager.playlist.value.getOrElse(index) { _currentTrack.value }
            _playbackPosition.value = 0L
            val total = player.duration
            _duration.value = if (total > 0L) total else 0L
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode.toUiRepeatMode()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleMode.value = shuffleModeEnabled
        }

        override fun onPlayerError(error: PlaybackException) {
            // 链接失效/断网等致命错误：暂停并给出可恢复的友好提示，不自动重试
            _errorMessage.value = "网络异常，请重试"
            _isPlaying.value = false
            playerManager.pause()
        }
    }

    /** 本次界面存活期间是否已经处理过「进入应用自动播放」，避免切回首页时重复触发 */
    private var autoPlayHandled = false

    init {
        player.addListener(listener)
        // 500ms 轮询播放进度/总时长；进度条只在叶子 composable 读值，不会拖累整页重组。
        // ExoPlayer 必须在主线程访问，viewModelScope 默认就是 Main。
        viewModelScope.launch {
            while (true) {
                _playbackPosition.value = player.currentPosition.coerceAtLeast(0L)
                val total = player.duration
                if (total > 0L) _duration.value = total
                delay(PROGRESS_POLL_MILLIS)
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
        // 错误态下点播放键即重试，避免用户卡在错误提示上无从恢复
        if (_errorMessage.value != null) {
            retry()
            return
        }
        if (player.isPlaying) pause() else play()
    }

    /** 播放失败后重试：重新准备数据源并播放。 */
    fun retry() {
        player.prepare()
        play()
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

    /** 拖动进度条跳转。立即回写一次进度，避免要等下一次轮询才有反馈。 */
    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        player.seekTo(target)
        _playbackPosition.value = target
    }

    /** 循环模式三态轮换：关闭 → 列表循环 → 单曲循环。 */
    fun toggleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun toggleFavorite(trackId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(trackId)) current.remove(trackId) else current.add(trackId)
        _favorites.value = current
    }

    /** 从播放列表直接选曲播放。 */
    fun playTrack(index: Int) {
        val tracks = playerManager.playlist.value
        if (index !in tracks.indices) return
        playerManager.setPlaylist(tracks, index)
        play()
    }

    override fun onCleared() {
        // 只摘掉自己的监听；播放器是应用级单例，绝不在这里释放
        player.removeListener(listener)
        super.onCleared()
    }

    private fun Int.toUiRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    private companion object {
        const val PROGRESS_POLL_MILLIS = 500L
    }
}
