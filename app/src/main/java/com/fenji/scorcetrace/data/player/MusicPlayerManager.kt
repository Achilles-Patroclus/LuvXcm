package com.fenji.scorcetrace.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.fenji.scorcetrace.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用级音乐播放器。
 *
 * 播放器实例不再跟随页面/ViewModel 生命周期，切换底部 Tab 时不会被暂停或释放；
 * 懒加载创建，整个进程内只创建一次。
 *
 * 前后台切换由 [com.fenji.scorcetrace.ScoreTraceApp] 的 ActivityLifecycleCallbacks 驱动
 * （不用 ProcessLifecycleOwner，它自带约 700ms 防抖，按 Home 键后要等一会儿才停）。
 */
@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var _exoPlayer: ExoPlayer? = null

    val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                _exoPlayer = ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Constants.DEFAULT_MUSIC_URL))
                    repeatMode = Player.REPEAT_MODE_ALL
                    // 显式声明：只有调用 play() 才出声，不依赖 ExoPlayer 的默认值，
                    // 以免绕过「进入应用自动播放音乐」开关
                    playWhenReady = false
                    prepare()
                }
            }
            return _exoPlayer!!
        }

    /** 切到后台前是否正在播放——决定回前台要不要自动恢复 */
    private var wasPlayingBeforeBackground = false

    /**
     * 应用整体退到后台（所有 Activity 都已 stopped）。
     * 用 `_exoPlayer` 判空：从没播过就退后台时，不要凭空创建播放器。
     */
    fun onAppBackground() {
        wasPlayingBeforeBackground = _exoPlayer?.isPlaying == true
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
    }
}
