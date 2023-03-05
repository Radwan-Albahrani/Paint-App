package my.huda.paintapp.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import my.huda.paintapp.screens.MainScreen.Companion.eraserMode
import my.huda.paintapp.screens.MainScreen.Companion.paintBrush
import my.huda.paintapp.screens.MainScreen.Companion.path
import my.huda.paintapp.screens.MainScreen.Companion.selectMode
import kotlin.math.max
import kotlin.math.min

class PaintView : View {
    private var scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var translateX = 0f
    private var translateY = 0f


    companion object{
        var pathList = ArrayList<Path>()
        var colorList = ArrayList<Int>()
        var strokeList = ArrayList<Float>()
        var currentBrush = Color.BLACK
        var currentStroke = 12f
    }

    constructor(context: Context) : super(context) {
        setupDrawing()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setupDrawing()
    }

    private fun setupDrawing() {
        // Get drawing area setup for interaction
        paintBrush.isAntiAlias = true
        paintBrush.color = currentBrush
        paintBrush.style = Paint.Style.STROKE
        paintBrush.strokeJoin = Paint.Join.ROUND
        paintBrush.strokeWidth = currentStroke
    }

    override fun onDraw(canvas: Canvas) {
        // Scale the canvas based on the current scale factor
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)
        for (currentPathIndex in pathList.indices) {
            paintBrush.color = colorList[currentPathIndex]
            paintBrush.strokeWidth = strokeList[currentPathIndex]
            canvas.drawPath(pathList[currentPathIndex], paintBrush)
        }
        paintBrush.color = currentBrush
        paintBrush.strokeWidth = currentStroke
        canvas.drawPath(path, paintBrush)
        canvas.restore()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = (event.x - translateX) / scaleFactor
        val touchY = (event.y - translateY) / scaleFactor
        scaleGestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                path.moveTo(touchX, touchY)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 2) {
                    // Two-finger gesture: translate the canvas
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y
                    translateX += dx / scaleFactor
                    translateY += dy / scaleFactor
                    invalidate()
                    return true
                }
                if(event.pointerCount >= 3){
                    return true
                }
                if(eraserMode)
                {
                    // find out the path that is being touched
                    for(currentPathIndex in pathList.indices){
                        val bounds = RectF()
                        pathList[currentPathIndex].computeBounds(bounds, false)

                        if(bounds.contains(touchX, touchY)){
                            pathList.removeAt(currentPathIndex)
                            colorList.removeAt(currentPathIndex)
                            strokeList.removeAt(currentPathIndex)
                            break
                        }
                    }
                    return true
                }
                path.lineTo(touchX, touchY)

            }
            MotionEvent.ACTION_UP -> {
                if(event.pointerCount >= 2){
                    return true
                }
                pathList.add(path)
                colorList.add(currentBrush)
                strokeList.add(currentStroke)
                path = Path()
            }
            else -> return false
        }
        postInvalidate()
        return false
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Scale with constraints. Not too big. Not too small
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.1f, min(scaleFactor, 10.0f))
            invalidate()
            return true
        }
    }
}