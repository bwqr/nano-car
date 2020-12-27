package com.example.nanocar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.View

class Line(c: Context) : View(c) {

    private var paint: Paint = Paint()
    private var startPoint: Point = Point(0, 0)
    private var endPoint: Point = Point(0, 0)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawLine(
            startPoint.x.toFloat(),
            startPoint.y.toFloat(),
            endPoint.x.toFloat(),
            endPoint.y.toFloat(),
            paint
        )
    }

    fun setStartPoint(p: Point) {
        startPoint = p
    }

    fun setEndPoint(p: Point) {
        endPoint = p
    }
}