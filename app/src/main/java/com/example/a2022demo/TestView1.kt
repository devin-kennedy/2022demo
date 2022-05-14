package com.example.a2022demo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TestView1 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
): View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 6f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    fun draw(canvas: Canvas, rectBounds: RectF) {
        super.draw(canvas)
        canvas.drawRect(
                rectBounds.left,
                rectBounds.top,
                rectBounds.right,
                rectBounds.bottom,
                paint
            )
    }
}