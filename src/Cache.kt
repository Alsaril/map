class Cache(val size: Int, val a: Double, val b: Double, f: (Double) -> Double, val compact: ((Double) -> Double)? = null) {
    private val arr = Array(size) {
        f(transform(it, 0, size, a, b))
    }

    private inline fun transform(value: Int, orA: Int, orB: Int, a: Double, b: Double) = a + (value.toDouble() - orA) / (orB - orA) * (b - a)
    private inline fun back(value: Double, orA: Int, orB: Int, a: Double, b: Double) = (orA + (value - a) / (b - a) * (orB - orA)).toInt()

    operator fun invoke(x: Double): Double {
        val value = compact?.invoke(x) ?: x

        var i = back(value, 0, size, a, b)
        if (i < 0) {
            i = 0
        }

        if (i >= size) {
            i = size - 1
        }
        try {
            return arr[i]
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}