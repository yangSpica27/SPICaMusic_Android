package me.spica27.spicamusic.player.impl.utils

import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

fun MediaSessionCompat.isPlaying(): Boolean = PlaybackStateCompat.STATE_PLAYING == controller.playbackState?.state
