package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.spica27.spicamusic.utils.DataStoreUtil
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
  private val dataStoreUtil: DataStoreUtil
) : ViewModel() {

  val autoPlay = dataStoreUtil.getAutoPlay

  val autoScanner = dataStoreUtil.getAutoScanner

  val forceDarkTheme = dataStoreUtil.getForceDarkTheme

  fun saveAutoPlay(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreUtil.saveAutoPlay(value)
    }
  }

  fun saveAutoScanner(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreUtil.saveAutoScanner(value)
    }
  }

  fun saveForceDarkTheme(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreUtil.saveForceDarkTheme(value)
    }
  }


}