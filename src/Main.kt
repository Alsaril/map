import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.io.File
import java.lang.Math.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.JPanel


const val R = 6400
const val DR = 1

fun Double.toRadians(): Double = this / 180 * PI

val acosCache = Cache(10000000, -1.0, 1.0, Math::acos)
val sinCache = Cache(100000, 0.0, 2 * PI, Math::sin) {
    val v = it % (2 * PI)
    return@Cache if (v < 0) v + 2 * PI else v
}
val cosCache = Cache(100000, 0.0, 2 * PI, Math::cos) {
    val v = it % (2 * PI)
    return@Cache if (v < 0) v + 2 * PI else v
}

fun acos(x: Double) = acosCache(x)
fun sin(x: Double) = sinCache(x)
fun cos(x: Double) = cosCache(x)

class GeoPoint(lat: Double, lon: Double) {
    val lat = lat.toRadians()
    val lon = lon.toRadians()
}

class PlanePoint(val x: Double, val y: Double)

class Projection(val width: Int, val height: Int, latFrom: Double, lonFrom: Double, latTo: Double, lonTo: Double) {
    val lonFrom = lonFrom.toRadians()
    val lonTo = lonTo.toRadians()
    val tFrom = c(latFrom.toRadians())
    val tTo = c(latTo.toRadians())
}

fun dist(p1: GeoPoint, p2: GeoPoint): Double = R * acos(sin(p1.lat) * sin(p2.lat) + cos(p1.lat) * cos(p2.lat) * cos(p1.lon - p2.lon))

fun alpha(dist: Double) = if (dist < DR) 1.0 else 1.0 / pow((dist - DR) * 2, 1.5)

fun render(points: List<GeoPoint>, projection: Projection, callback: (Array<Array<AtomicReference<Double>>>) -> Unit) {
    val alpha = Array(projection.width) { Array(projection.height) { AtomicReference<Double>() } }
    val pool = Executors.newFixedThreadPool(8)
    val f = mutableListOf<Future<out Any>>()
    repeat(projection.width) { x ->
        f.add(pool.submit {
            repeat(projection.height) { y ->
                alpha[x][y].set(-1.0)
                renderPixel(alpha, x, y, points, projection)
            }
            println("line $x completed")
        })
    }

    while (true) {
        Thread.sleep(10)
        callback(alpha)
        if (f.all { it.isDone }) {
            break
        }
    }

}

fun color(v: Float): Color {
    return when {
        v < 1 -> Color(1 - v, 1f, 1 - v)
        v < 2 -> Color(v - 1, 1f, 2 - v)
        v < 3 -> Color(1f, 3 - v, 0f)
        else -> Color.RED
    }
}

fun renderPixel(image: Array<Array<AtomicReference<Double>>>, x: Int, y: Int, points: List<GeoPoint>, projection: Projection) {
    val point = toGeo(PlanePoint(x.toDouble(), y.toDouble()), projection)
    val alpha = points.fold(0.0) { acc, it -> acc + alpha(dist(point, it)) }
    image[x][y].set(alpha)
}

fun toGeo(point: PlanePoint, projection: Projection): GeoPoint {
    val lon = projection.lonFrom + point.x / projection.width.toDouble() * (projection.lonTo - projection.lonFrom)
    val c = projection.height.toDouble() / log(projection.tTo / projection.tFrom)
    val a = -c * log(projection.tFrom)
    val lat = 2 * atan(exp((point.y - a) / c)) - PI / 2
    return GeoPoint(lat / PI * 180, lon / PI * 180)
}

fun toPlane(point: GeoPoint, projection: Projection): PlanePoint {
    val x = projection.width.toDouble() * (point.lon - projection.lonFrom) / (projection.lonTo - projection.lonFrom)
    val y = projection.height.toDouble() * log(projection.tFrom / projection.tTo) * log(c(point.lat) / projection.tFrom)
    return PlanePoint(x, y)
}

fun c(x: Double) = tan(x / 2 + PI / 4)

fun main(args: Array<String>) {
    val points = mutableListOf<GeoPoint>()
    File("/home/iyakovlev/lat_lon").forEachLine {
        if (it.isEmpty()) return@forEachLine
        val spl = it.split('\t')
        if (spl.isEmpty() || spl[0].isEmpty() || spl[1].isEmpty()) return@forEachLine
        points.add(GeoPoint(spl[0].toDouble(), spl[1].toDouble()))
    }

    val cell = 1
    val width = 600
    val height = 450

    var currImage: Array<Array<AtomicReference<Double>>>? = null

    val frame = JFrame()
    val panel = object : JPanel() {
        override fun paint(g: Graphics) {
            g.color = Color.WHITE
            g.fillRect(0, 0, width * cell, height * cell)
            currImage?.let {
                it.forEachIndexed { x, arr ->
                    arr.forEachIndexed { y, ref ->
                        val v = ref.get() ?: 0.0
                        g.color = if (v < 0) Color(0f, 0f, 0f) else color(v.toFloat())
                        g.fillRect(x * cell, (height - y) * cell, cell, cell)
                    }
                }
            }
        }
    }
    frame.add(panel)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    panel.preferredSize = Dimension(width * cell, height * cell)
    frame.pack()
    frame.isVisible = true

    render(points.shuffled().take(1000), Projection(width, height, 50.0, 32.0, 60.0, 50.0)) {
        currImage = it
        panel.repaint()
        //ImageIO.write(it, "png", File("/home/iyakovlev/a1.png"))
        /*   val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
           it.forEachIndexed { x, arr ->
               arr.forEachIndexed { y, ref ->
                   val v = ref.get() ?: 0.0
                   val color = if (v < 0) Color(1f, 1f, 1f) else Color(0f, 1f, 0f, pow(v, 0.3).toFloat())
                   image.setRGB(x, y, color.rgb)
               }
           }
           ImageIO.write(image, "png", File("/home/iyakovlev/c.png"))*/
    }

    //System.exit(0)
}