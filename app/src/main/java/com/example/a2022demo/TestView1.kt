package com.example.a2022demo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.TextureView
import android.view.View
import android.widget.TextView
import java.lang.Exception

class TestView1 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): View(context, attrs) {
    private var paint = Paint()
    private lateinit var rectBoundsTest: RectF
    private var outputToOverlay: Matrix = Matrix()

    fun setRectBounds(rectBounds: RectF) {
        rectBoundsTest = rectBounds
        outputToOverlay.mapRect(rectBoundsTest)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.apply {
            color = Color.RED
            strokeWidth = 10f
            style = Paint.Style.STROKE
        }

        try {
            canvas.drawRect(rectBoundsTest, paint)
        } catch (exc: Exception) {
            println("Fail: ${exc.toString()}")
        }

    }
}