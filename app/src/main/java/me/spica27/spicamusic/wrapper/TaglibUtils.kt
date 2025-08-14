package me.spica27.spicamusic.wrapper

import com.kyant.taglib.TagLib


object TaglibUtils {


  fun retrieveMetadataWithFD(fd: Int): Metadata {
    val metadata = TagLib.getMetadata(fd, false)
    return Metadata(
      title = metadata?.propertyMap?.get("TITLE")?.first() ?: "",
      album = metadata?.propertyMap?.get("ALBUM")?.first() ?: "",
      artist = metadata?.propertyMap?.get("ARTIST")?.first() ?: "",
      albumArtist = metadata?.propertyMap?.get("ARTIST")?.first() ?: "",
      composer = metadata?.propertyMap?.get("ALBUMARTIST")?.first() ?: "",
      lyricist = metadata?.propertyMap?.get("LYRICS")?.first() ?: "",
      comment = metadata?.propertyMap?.get("COMMENT")?.first() ?: "",
      genre = metadata?.propertyMap?.get("GENRE")?.first() ?: "",
      track = metadata?.propertyMap?.get("TRACKNUMBER")?.first() ?: "",
      disc = metadata?.propertyMap?.get("DISCNUMBER")?.first() ?: "",
      date = metadata?.propertyMap?.get("DATE")?.first() ?: "",
      duration = 0,
      dateAdded = 0,
      dateModified = 0,
    )
  }
}


