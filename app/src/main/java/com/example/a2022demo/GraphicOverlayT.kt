package com.example.a2022demo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View


class GraphicOverlayT constructor(context: Context): View(context) {
    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    fun drawBounds(canvas: Canvas, rectBounds: RectF) {
        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        paint.style = Paint.Style.STROKE
        paint.setColor(Color.RED)
        paint.strokeWidth = 10f
        canvas.apply {
            drawRect(
                rectBounds.left,
                rectBounds.top,
                rectBounds.right,
                rectBounds.bottom,
                paint
            )
        }
    }
}