package me.spica27.spicamusic.widget.particles


import kotlin.random.Random


/**
 * @author Adib Faramarzi (adibfara@gmail.com)
 */

fun randomInt(start: Int, end: Int, random: Random): Int {
  return start + (random.nextFloat() * (end - start)).toInt()
}

fun randomFloat(start: Float, end: Float, random: Random = Random.Default): Float {
  return start + random.nextFloat() * (end.toInt() - start.toInt())
}

fun randomFloatInRange(ranges: List<ClosedRange<Float>>): Float {
  val random = Random.Default
  return ranges.random().let {
    randomFloat(it.start, it.endInclusive, random)
  }
}