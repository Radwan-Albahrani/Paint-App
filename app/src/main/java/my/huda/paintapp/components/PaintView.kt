package my.huda.paintapp.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.transform
import my.huda.paintapp.screens.MainScreen.Companion.eraserMode
import my.huda.paintapp.screens.MainScreen.Companion.paintBrush
import my.huda.paintapp.screens.MainScreen.Companion.path
import my.huda.paintapp.screens.MainScreen.Companion.selectMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.atan2
import kotlin.math.sqrt

class PaintView : View {
    private var scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var translateX = 0f
    private var translateY = 0f
    private var focusX = 0f
    private var focusY = 0f

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var selectedPathBounds = RectF()
    private var selectedPathPaint = Paint()
    private var isScaling = false


    companion object{
        var pathList = ArrayList<Path>()
        var colorList = ArrayList<Int>()
        var strokeList = ArrayList<Float>()
        var currentBrush = Color.BLACK
        var currentStroke = 12f
        var selectedPathIndex = -1
    }

    constructor(context: Context) : super(context) {
        setupDrawing()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setupDrawing()
    }

    // Setting up brushes
    private fun setupDrawing() {
        // Paint for selected path
        selectedPathPaint.isAntiAlias = true
        selectedPathPaint.color = Color.RED
        selectedPathPaint.style = Paint.Style.STROKE
        selectedPathPaint.strokeJoin = Paint.Join.ROUND
        selectedPathPaint.strokeWidth = 12f
        selectedPathPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)

        // Get drawing area setup for interaction
        paintBrush.isAntiAlias = true
        paintBrush.color = currentBrush
        paintBrush.style = Paint.Style.STROKE
        paintBrush.strokeJoin = Paint.Join.ROUND
        paintBrush.strokeWidth = currentStroke
    }

    // Drawing on the canvas
    override fun onDraw(canvas: Canvas) {
        // Scale the canvas based on the current scale factor
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY)
        for (currentPathIndex in pathList.indices) {
            paintBrush.color = colorList[currentPathIndex]
            paintBrush.strokeWidth = strokeList[currentPathIndex]
            canvas.drawPath(pathList[currentPathIndex], paintBrush)
        }
        if (selectedPathIndex >= 0) {
            drawSelectedPathBounds(canvas)
        }
        paintBrush.color = currentBrush
        paintBrush.strokeWidth = currentStroke
        canvas.drawPath(path, paintBrush)
        canvas.restore()
        invalidate()
    }

    // Handling touch gestures
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = (event.x - translateX) / scaleFactor
        val touchY = (event.y - translateY) / scaleFactor
        if(!selectMode)
        {
            scaleGestureDetector.onTouchEvent(event)
        }
        if(isScaling)
        {
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return onTouchDown(event, touchX, touchY)
            }

            MotionEvent.ACTION_MOVE -> {
                onTouchMove(event, touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                return onTouchUp(event)
            }
            else -> return false
        }
        postInvalidate()
        return false
    }

    // Function to handle the touch down event based on the mode
    private fun onTouchDown(event: MotionEvent, touchX: Float, touchY: Float) : Boolean {
        return if (selectMode) {
            selectModeTouchDown(touchX, touchY)
        } else {
            // Select mode is disabled, start drawing a new path
            selectedPathIndex = -1
            selectedPathBounds = RectF()
            lastTouchX = event.x
            lastTouchY = event.y
            path.moveTo(touchX, touchY)
            true
        }
    }

    // Function to handle moving the finger on the screen based on the mode
    private fun onTouchMove(event: MotionEvent, touchX: Float, touchY: Float) : Boolean
    {
        if (selectMode && selectedPathIndex >= 0) {
            // A path is being selected, perform the appropriate transformation
            val selectedPath = pathList[selectedPathIndex]
            val bounds = RectF()
            selectedPath.computeBounds(bounds, false)
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            when (event.pointerCount) {
                1 -> {
                    selectModeMove(touchX, touchY, selectedPath)
                }
                2 -> {
                    selectModeScale(event, selectedPath, centerX, centerY)
                }
                3 -> {
                    selectModeRotate(event, selectedPath, centerX, centerY)
                }
            }
            invalidate()
            return true
        }
        if (event.pointerCount == 2 && !selectMode && !isScaling){
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
        return true
    }

    // Function to handle removing the finger from the screen
    private fun onTouchUp(event: MotionEvent) : Boolean
    {
        if(event.pointerCount >= 2){
            return true
        }
        pathList.add(path)
        colorList.add(currentBrush)
        strokeList.add(currentStroke)
        path = Path()
        return true
    }

    private fun selectModeTouchDown(touchX: Float, touchY: Float): Boolean {
        if(selectedPathIndex == -1){
            // Select mode is enabled, check if the touch point is inside any existing path
            for (i in pathList.indices) {
                val bounds = RectF()
                pathList[i].computeBounds(bounds, false)
                if (bounds.contains(touchX, touchY)) {
                    // Found a path, store the initial touch point and select it
                    selectedPathIndex = i
                    bounds.inset(-bounds.width() / 4, -bounds.height() / 4)
                    selectedPathBounds = bounds
                    initialTouchX = touchX
                    initialTouchY = touchY
                    return true
                }
            }
        }
        else
        {
            // A path is already selected, check if the touch point is inside the selected path
            if (selectedPathBounds.contains(touchX, touchY)) {
                // The touch point is inside the selected path, store the initial touch point
                initialTouchX = touchX
                initialTouchY = touchY
                return true
            }
        }

        // No path is selected, return false to let other touch events handle this
        return false
    }

    private fun selectModeRotate(
        event: MotionEvent,
        selectedPath: Path,
        centerX: Float,
        centerY: Float
    ) {
        // angle between the three fingers
        val angle = PointF(event.getX(0), event.getY(0)).angleTo(
            PointF(event.getX(1), event.getY(1)),
            PointF(event.getX(2), event.getY(2))
        )

        // angle between the initial three fingers
        val initialAngle = PointF(initialTouchX, initialTouchY).angleTo(
            PointF(event.getX(1), event.getY(1)),
            PointF(event.getX(2), event.getY(2))
        )

        // rotate the path
        selectedPath.transform(Matrix().apply {
            postTranslate(-centerX, -centerY)
            postRotate(angle - initialAngle)
            postTranslate(centerX, centerY)
        })
        selectedPathBounds.transform(Matrix().apply {
            postTranslate(-centerX, -centerY)
            postRotate(angle - initialAngle)
            postTranslate(centerX, centerY)
        })
    }

    private fun selectModeScale(
        event: MotionEvent,
        selectedPath: Path,
        centerX: Float,
        centerY: Float
    ) {
        // distance between two fingers
        val distance = PointF(event.getX(0), event.getY(0)).distanceTo(
            PointF(event.getX(1), event.getY(1))
        )

        // initial distance between two fingers
        val initialDistance = PointF(initialTouchX, initialTouchY).distanceTo(
            PointF(event.getX(1), event.getY(1))
        )

        // scale the path
        val scale = (distance / initialDistance) / scaleFactor
        selectedPath.transform(Matrix().apply {
            postTranslate(-centerX, -centerY)
            postScale(scale, scale)
            postTranslate(centerX, centerY)
        })
        selectedPathBounds.transform(Matrix().apply {
            postTranslate(-centerX, -centerY)
            postScale(scale, scale)
            postTranslate(centerX, centerY)
        })
    }

    private fun selectModeMove(
        touchX: Float,
        touchY: Float,
        selectedPath: Path
    ) {
        // One-finger gesture: move the path
        val distanceX = touchX - initialTouchX
        val distanceY = touchY - initialTouchY
        selectedPath.offset(distanceX / scaleFactor, distanceY / scaleFactor)
        selectedPathBounds.offset((distanceX) / scaleFactor, (distanceY) / scaleFactor)
        initialTouchX = touchX
        initialTouchY = touchY
    }

    private fun drawSelectedPathBounds(canvas: Canvas) {
        canvas.drawRect(selectedPathBounds, selectedPathPaint)
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private val zoomAmount = 0.5f // adjust this to change zoom speed
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Scale with constraints. Not too big. Not too small
            focusX = detector.focusX
            focusY = detector.focusY
            scaleFactor += (detector.scaleFactor - 1) * zoomAmount
            scaleFactor = max(0.1f, min(scaleFactor, 10.0f))
            invalidate()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
            super.onScaleEnd(detector)
        }
    }
}

fun PointF.angleTo(p1: PointF, p2: PointF): Float {
    val angle1 = atan2(p1.y - y, p1.x - x)
    val angle2 = atan2(p2.y - y, p2.x - x)
    return angle2 - angle1
}

fun PointF.distanceTo(other: PointF): Float {
    val dx = other.x - x
    val dy = other.y - y
    return sqrt(dx * dx + dy * dy)
}
