package com.example.maptest.utils

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap =
        Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
    drawable.draw(canvas)
    return bitmap
}

fun scaleBitmapAndKeepRation(
    targetBmp: Bitmap,
    reqHeightInPixels: Int,
    reqWidthInPixels: Int
): Bitmap {
    val matrix = Matrix()
    matrix.setRectToRect(
        RectF(
            0F, 0F, targetBmp.width.toFloat(),
            targetBmp.height.toFloat()
        ),
        RectF(0F, 0F, reqWidthInPixels.toFloat(), reqHeightInPixels.toFloat()),
        Matrix.ScaleToFit.CENTER
    )
    val b = Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.width, targetBmp.height, matrix, true)
    return getRoundedCornerBitmap(b, b.width)
}

fun getRoundedCornerBitmap(bitmap: Bitmap, radius: Int): Bitmap {
    val output = Bitmap.createBitmap(
        bitmap.width, bitmap
            .height, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    val paint = Paint()
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    val rectF = RectF(rect)
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return output
}