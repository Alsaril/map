/*
import kotlin.math.roundToInt

class Cluster<T>(val objects: List<T>,
                 val dist: (T, T) -> Double,
                 val r: Double,
                 val minX: Double,
                 val minY: Double,
                 val maxX: Double,
                 val maxY: Double,
                 val xf: (T) -> Double,
                 val yf: (T) -> Double) {
    init {
        val sx = ((maxX - minX) / r).toInt() + 1
        val sy = ((maxY - minY) / r).toInt() + 1

        val clusters = Array(sx) {
            Array(sy) {
                mutableListOf<T>()
            }
        }



    }

    fun round(x: Double, y: Double): Sequence<T> {

    }
}*/
