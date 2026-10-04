package com.fenji.scoretrace.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** 播放列表中的一首曲目。 */
data class TrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
)

/** 默认播放列表（列表循环）。 */
val DEFAULT_PLAYLIST = listOf(
    TrackInfo(
        id = "wu-xian",
        title = "无限",
        artist = "周深",
        url = "https://www.lequxiang.com.cn/view.php/43566d645a97e595dd047c7089194558.ogg",
    ),
    TrackInfo(
        id = "chong-qi",
        title = "重启",
        artist = "周深",
        url = "https://www.lequxiang.com.cn/view.php/4a15c67dfc3011f82ef52282c1728d59.ogg",
    ),
    TrackInfo(
        id = "whisper-of-hope",
        title = "Whisper Of Hope",
        artist = "Gothic Storm",
        url = "https://www.lequxiang.com.cn/view.php/80e6affab3677c4e9264648b4cb150b2.mp3",
    ),
)

/**
 * 应用级音乐播放器。
 *
 * 播放器实例不再跟随页面/ViewModel 生命周期，切换底部 Tab 时不会被暂停或释放；
 * 懒加载创建，整个进程内只创建一次。
 *
 * 前后台切换由 [com.fenji.scoretrace.ScoreTraceApp] 的 ActivityLifecycleCallbacks 驱动
 * （不用 ProcessLifecycleOwner，它自带约 700ms 防抖，按 Home 键后要等一会儿才停）。
 */
@Singleton
class MusicPlayerManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val musicCache: MusicCacheRepository,
) {

    private var _exoPlayer: ExoPlayer? = null
    private var _audioCache: Cache? = null

    /** 当前播放列表（可增删）；与播放器的 media items 保持一致 */
    private val _playlist = MutableStateFlow(DEFAULT_PLAYLIST)
    val playlist: StateFlow<List<TrackInfo>> = _playlist.asStateFlow()

    val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                // 按上次退出的歌曲与进度恢复起始位置
                val snapshot = musicCache.load()
                val startIndex = snapshot
                    ?.let { s -> _playlist.value.indexOfFirst { it.id == s.songId }.takeIf { it >= 0 } }
                    ?: 0
                val startPosition = snapshot?.positionMs?.coerceAtLeast(0L) ?: 0L

                val mediaSourceFactory = DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(OkHttpDataSource.Factory(createAudioHttpClient()))
                _exoPlayer = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()
                    .apply {
                        setMediaItems(_playlist.value.map { MediaItem.fromUri(it.url) }, startIndex, startPosition)
                        repeatMode = Player.REPEAT_MODE_ALL
                        volume = DEFAULT_VOLUME
                        // 显式声明：只有调用 play() 才出声，不依赖 ExoPlayer 的默认值，
                        // 以免绕过「进入应用自动播放音乐」开关
                        playWhenReady = false
                        prepare()
                    }
            }
            return _exoPlayer!!
        }

    /**
     * 音频磁盘缓存用独立 OkHttpClient：不复用 Retrofit 那个，免得 BODY 级日志把音频字节全打出来。
     * 服务端未返回缓存头时用网络拦截器补上 Cache-Control，保证音频能落盘、二次播放走缓存。
     */
    private fun createAudioHttpClient(): OkHttpClient {
        val cache = Cache(File(context.cacheDir, AUDIO_CACHE_DIR), AUDIO_CACHE_BYTES)
        _audioCache = cache
        return OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "public, max-age=$AUDIO_CACHE_MAX_AGE_SECONDS")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()
    }

    /** 载入播放列表并从 [startIndex] 开始（只 prepare，不自动播放，由调用方决定）。 */
    fun setPlaylist(tracks: List<TrackInfo>, startIndex: Int = 0) {
        _playlist.value = tracks
        exoPlayer.setMediaItems(tracks.map { MediaItem.fromUri(it.url) }, startIndex, 0L)
        exoPlayer.prepare()
    }

    fun skipToNext() = exoPlayer.seekToNextMediaItem()

    fun skipToPrevious() = exoPlayer.seekToPreviousMediaItem()

    /** 切到后台前是否正在播放——决定回前台要不要自动恢复 */
    private var wasPlayingBeforeBackground = false

    /**
     * 应用整体退到后台（所有 Activity 都已 stopped）。
     * 用 `_exoPlayer` 判空：从没播过就退后台时，不要凭空创建播放器。
     */
    fun onAppBackground() {
        val player = _exoPlayer
        wasPlayingBeforeBackground = player?.isPlaying == true
        // 记住歌曲与进度，供下次冷启动恢复
        if (player != null) {
            val songId = _playlist.value.getOrNull(player.currentMediaItemIndex)?.id
            if (songId != null) {
                musicCache.save(
                    songId = songId,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                )
            }
        }
        if (wasPlayingBeforeBackground) pause()
    }

    /** 应用回到前台：只恢复"上次是被自动暂停的"播放，用户手动暂停过的保持暂停。 */
    fun onAppForeground() {
        if (wasPlayingBeforeBackground) {
            wasPlayingBeforeBackground = false
            play()
        }
    }

    fun play() {
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    /**
     * 释放底层播放器。当前没有调用方——播放器按设计活到进程结束；
     * 若将来需要「退出应用就释放」，应在这里接上前台服务或生命周期钩子。
     */
    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
        _audioCache?.close()
        _audioCache = null
    }

    private companion object {
        const val AUDIO_CACHE_DIR = "audio_cache"
        const val AUDIO_CACHE_BYTES = 100L * 1024 * 1024
        const val AUDIO_CACHE_MAX_AGE_SECONDS = 604800L

        /** 默认音量：0.65（原 1.0 偏大） */
        const val DEFAULT_VOLUME = 0.65f
    }
}
