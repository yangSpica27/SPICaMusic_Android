package me.spica27.spicamusic.db.entity

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.util.*

@JsonClass(generateAdapter = true)
@Parcelize
data class Crash(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "",
  val message: String = "",
  val causeClass: String = "",
  val causeMethod: String = "",
  val causeFile: String = "",
  val causeLine: String = "",
  val stackTrace: String = "",
  val deviceInfo: String = "",
  val buildVersion: String = ""
): Parcelable{



  override fun toString(): String {
    return "Crash(id='$id', title='$title', message='$message', causeClass='$causeClass', causeMethod='$causeMethod', causeFile='$causeFile', causeLine='$causeLine', stackTrace='$stackTrace', deviceInfo='$deviceInfo', buildVersion='$buildVersion')"
  }
}