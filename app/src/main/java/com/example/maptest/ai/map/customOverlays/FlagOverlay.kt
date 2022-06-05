package org.osmdroid.views.overlay.mylocation

import android.graphics.*
import android.view.MotionEvent
import com.example.maptest.App
import com.example.maptest.R
import com.example.maptest.utils.scaleBitmapAndKeepRation
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay


class FlagOverlay(private val avatarBitmap: Bitmap?, var location: GeoPoint) : Overlay() {

    private var flagBitmap: Bitmap = BitmapFactory.decodeResource(App.instance.resources, R.drawable.ic_tracker_75dp)
    private val mPaint = Paint()
    private val flagWidth = flagBitmap.width
    private val flagHeight = flagBitmap.height
    private val avatarWidth = (flagBitmap.width) * 0.7
    private val avatarHeight = (flagBitmap.height) * 0.7
    private val deltaWidth = (flagWidth - avatarWidth) / 2
    private val deltaHeight = (flagHeight * 0.86 - avatarHeight) / 2
    private lateinit var clickListener: () -> Unit

     var actualPositionInCanvasX = 0F
     var actualPositionInCanvasY = 0F

    private var personHotspot = Point(24, 39)
    private val screenCoords = Point()

    override fun draw(c: Canvas, pj: Projection) {
        pj.toPixels(location, screenCoords)

        actualPositionInCanvasX = (screenCoords.x - personHotspot.x).toFloat()
        actualPositionInCanvasY = (screenCoords.y - personHotspot.y).toFloat()

        c.drawBitmap(flagBitmap, actualPositionInCanvasX, actualPositionInCanvasY, mPaint)

        this.avatarBitmap?.let {
            val circleAvatarBitmap =
                scaleBitmapAndKeepRation(this.avatarBitmap, avatarHeight.toInt(), avatarWidth.toInt())
            c.drawBitmap(
                circleAvatarBitmap,
                (actualPositionInCanvasX + deltaWidth).toFloat(),
                (actualPositionInCanvasY + deltaHeight).toFloat(),
                mPaint
            )
        }
    }

    fun setClickListener(listener: () -> Unit) {
        this.clickListener = listener
    }

    override fun onSingleTapUp(e: MotionEvent?, mapView: MapView?): Boolean {
        if (e != null) {
            mapView?.overlays?.forEach {
                if (it is FlagOverlay &&
                    e.x in it.actualPositionInCanvasX..it.actualPositionInCanvasX + flagBitmap.width &&
                    e.y in it.actualPositionInCanvasY..it.actualPositionInCanvasY + flagBitmap.height &&
                    it == this
                ) {
                    clickListener()
                    return true
                }
            }
            return false
        } else return false
    }
}