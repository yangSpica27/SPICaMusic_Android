package me.spica27.spicamusic.comm

sealed class NetworkState {
  object IDLE : NetworkState()
  object LOADING : NetworkState()
  object SUCCESS : NetworkState()
  data class ERROR(val message: String) : NetworkState()
}