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
import kotlin.math.*

class PaintView : View {
    // ==================== Variables ====================
    private var scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

    // ============ Canvas Variables ============
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var translateX = 0f
    private var translateY = 0f
    private var focusX = 0f
    private var focusY = 0f

    // ============ Select mode variables ============
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var selectedPathBounds = RectF()
    private var selectedPathPaint = Paint()
    private var scalingCount = 0
    private val scaleFactorObject = 1.001f

    // ============ Booleans ============
    private var isScaling = false

    // ==================== Companion Object ====================
    companion object{
        var pathList = ArrayList<Path>()
        var colorList = ArrayList<Int>()
        var strokeList = ArrayList<Float>()
        var currentBrush = Color.BLACK
        var currentStroke = 12f
        var selectedPathIndex = -1
        var hasBounds = false
    }

    // ==================== Constructors ====================
    constructor(context: Context) : super(context) {
        setupDrawing()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setupDrawing()
    }

    // ==================== Setup ====================
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

    // ==================== Drawing ====================
    override fun onDraw(canvas: Canvas) {
        focusX = (super.getWidth() * 0.5f) - translateX
        focusY = (super.getHeight() * 0.5f) - translateY

        // Draw the path that is currently being drawn
        paintBrush.color = currentBrush
        paintBrush.strokeWidth = currentStroke * scaleFactor
        canvas.drawPath(path, paintBrush)

        // Scale the canvas based on the current scale factor
        canvas.save()
        canvas.apply {
            translate(translateX, translateY)
            scale(scaleFactor, scaleFactor, focusX, focusY)
        }

        // Draw the paths
        for (currentPathIndex in pathList.indices) {
            paintBrush.color = colorList[currentPathIndex]
            paintBrush.strokeWidth = strokeList[currentPathIndex]
            canvas.drawPath(pathList[currentPathIndex], paintBrush)
        }
        canvas.restore()

        // Draw the selected path if any
        if (selectedPathIndex >= 0) {
            drawSelectedPathBounds(canvas)
        }
    }

    // simple function to draw the selected path bounds
    private fun drawSelectedPathBounds(canvas: Canvas) {
        canvas.drawRect(selectedPathBounds, selectedPathPaint)
    }

    // ==================== Touch Events ====================
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Get the touch coordinates based on any applied canvas translations
//        val touchX = (event.x - focusX - translateX) / scaleFactor + focusX
//        val touchY = (event.y - focusY - translateY) / scaleFactor + focusY
        val touchX = event.x
        val touchY = event.y
        println("touchX: $touchX, touchY: $touchY")

        // If not select mode, allow scaling
        if(!selectMode)
        {
            scaleGestureDetector.onTouchEvent(event)
        }

        // Handle the touch event based on the action
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

    // ================== TOUCH EVENT HANDLERS ==================
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
            path = Path()
            if(event.pointerCount == 1)
            {
                path.moveTo(touchX, touchY)
            }
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
                    selectModeScaleAndRotate(event, selectedPath, centerX, centerY)
                }
            }
            invalidate()
            return true
        }
        // Panning the canvas
        if (event.pointerCount == 2 && !selectMode){
            // Two-finger gesture: translate the canvas
            val dx = event.x - lastTouchX
            val dy = event.y - lastTouchY
            println("dx: $dx, dy: $dy")
            lastTouchX = event.x
            lastTouchY = event.y
            translateX += dx / scaleFactor
            translateY += dy / scaleFactor
            invalidate()
            return true
        }
        // check to not enable any drawing if more than one finger
        if(event.pointerCount > 1){
            return true
        }

        // Eraser mode
        if(eraserMode)
        {
            // find out the path that is being touched
            for(currentPathIndex in pathList.indices){
                val bounds = RectF()
                pathList[currentPathIndex].computeBounds(bounds, true)

                bounds.apply {
                    transform(Matrix().apply { postScale(scaleFactor, scaleFactor, focusX, focusY) })
                    offset(translateX, translateY)
                }
                println("bounds: $bounds")

                if(bounds.contains(touchX, touchY)){
                    pathList.removeAt(currentPathIndex)
                    colorList.removeAt(currentPathIndex)
                    strokeList.removeAt(currentPathIndex)
                    break
                }
            }
            return true
        }

        // Allow drawing if pointers are 1 and not scaling
        if(event.pointerCount == 1 && !isScaling && !eraserMode && !selectMode)
        {
            path.lineTo(touchX, touchY)
        }
        return true
    }

    // Function to handle removing the finger from the screen
    private fun onTouchUp(event: MotionEvent) : Boolean
    {
        // Do nothing if touch up is from 2 fingers
        if(event.pointerCount >= 2){
            return true
        }

        // Otherwise, add drawn path to the path list
        if(event.pointerCount == 1 && !selectMode && !isScaling && !eraserMode)
        {
            path.apply {
                offset(-translateX, -translateY)
                transform(Matrix().apply {
                    postScale(1 / scaleFactor, 1 / scaleFactor, focusX, focusY)
                })
            }
            pathList.add(path)
            colorList.add(currentBrush)
            strokeList.add(currentStroke)
            path = Path()
        }
        return true
    }

    // ================== SELECT MODE TOUCH EVENT HANDLERS ==================
    private fun selectModeTouchDown(touchX: Float, touchY: Float): Boolean {

        // If bounds are already set, check if the touch point is inside the bounds
        if(hasBounds)
        {
            return if (selectedPathBounds.contains(touchX, touchY)) {
                // The touch point is inside the selected path, store the initial touch point
                initialTouchX = touchX
                initialTouchY = touchY
                true
            } else{
                // The touch point is outside the selected path, deselect it
                selectedPathIndex = -1
                selectedPathBounds = RectF()
                hasBounds = false
                false
            }
        }

        // Select mode is enabled, check if the touch point is inside any existing path
        for (i in pathList.indices) {
            val bounds = RectF()
            pathList[i].computeBounds(bounds, false)
            bounds.apply {
                transform(Matrix().apply { postScale(scaleFactor, scaleFactor, focusX, focusY) })
                offset(translateX, translateY)
            }
            if (bounds.contains(touchX, touchY)) {
                // Found a path, store the initial touch point and select it
                selectedPathIndex = i
                bounds.inset(-bounds.width() / 6, -bounds.height() / 6)
                selectedPathBounds = bounds
                initialTouchX = touchX
                initialTouchY = touchY
                hasBounds = true
                return true
            }
        }

        // No path is selected, return false to let other touch events handle this
        return false
    }

    // TODO: Make this actually work
    private fun selectModeScaleAndRotate(
        event: MotionEvent,
        selectedPath: Path,
        centerX: Float,
        centerY: Float
    ) {
        // distance between two fingers
        val distance = PointF(event.getX(0), event.getY(0)).distanceTo(
            PointF(event.getX(1), event.getY(1))
        )

        // angle between the two fingers
        val angle = PointF(event.getX(0), event.getY(0)).angleTo(
            PointF(event.getX(1), event.getY(1))
        )

        // initial distance between two fingers
        val initialDistance = PointF(initialTouchX, initialTouchY).distanceTo(
            PointF(event.getX(1), event.getY(1))
        )

        // initial angle between the two fingers
        val initialAngle = PointF(initialTouchX, initialTouchY).angleTo(
            PointF(event.getX(1), event.getY(1))
        )
        if(initialDistance > distance)
        {
            if(scalingCount > 0)
            {
                scalingCount = 0
            }
            if(scalingCount <= -50)
            {
                scalingCount = -50
            }
            // fingers are moving away from each other
            scalingCount--
            println("Zooming Out: $scalingCount")
            println(initialDistance)
            println(distance)
        }
        else
        {
            // fingers are moving closer to each other
            if(scalingCount < 0)
            {
                scalingCount = 0
            }
            if (scalingCount >= 50)
            {
                scalingCount = 50
            }
            scalingCount++
            println("Zooming in: $scalingCount")
        }

        val scale = (scaleFactorObject.pow(scalingCount))

        // scale and rotate the path
        selectedPath.transform(Matrix().apply {
            postTranslate(-centerX, -centerY)
            postScale(scale, scale)
            postRotate(angle - initialAngle, distance / 2, 0f)
            postTranslate(centerX, centerY)
        })
        val bounds = RectF()
        selectedPath.computeBounds(bounds, false)
        bounds.inset(-bounds.width() / 6, -bounds.height() / 6)
        bounds.apply {
            transform(Matrix().apply { postScale(scaleFactor, scaleFactor, focusX, focusY) })
            offset(translateX, translateY)
        }
        selectedPathBounds = bounds

    }


    // Move the selected path
    private fun selectModeMove(
        touchX: Float,
        touchY: Float,
        selectedPath: Path
    ) {
        // One-finger gesture: move the path
        val distanceX = touchX - initialTouchX
        val distanceY = touchY - initialTouchY
        selectedPath.offset(distanceX / scaleFactor, distanceY / scaleFactor)
        val bounds = RectF()
        selectedPath.computeBounds(bounds, false)
        bounds.inset(-bounds.width() / 6, -bounds.height() / 6)
        bounds.apply {
            transform(Matrix().apply { postScale(scaleFactor, scaleFactor, focusX, focusY) })
            offset(translateX, translateY)
        }
        selectedPathBounds = bounds

        initialTouchX = touchX
        initialTouchY = touchY
    }

    // ================== HELPER FUNCTIONS ==================
    // Function to scale the canvas, inside the inner class
    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private val zoomAmount = 1.5f // adjust this to change zoom speed
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Scale with constraints. Not too big. Not too small
            scaleFactor += (detector.scaleFactor - 1) * zoomAmount
            scaleFactor = max(0.1f, min(scaleFactor, 10.0f))
            println("scaleFactor: $scaleFactor")
            invalidate()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            // check if any thread is running
            if (isScaling) {
                return false
            }
            isScaling = true
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // start a thread to wait 300 ms then change scaling value
            Thread {
                try {
                    Thread.sleep(300)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                isScaling = false
            }.start()
            super.onScaleEnd(detector)
        }
    }
}

// Function to find angle between two points
fun PointF.angleTo(p: PointF): Float {
    val angle1 = atan2(y, x)
    val angle2 = atan2(p.y - y, p.x - x)
    return angle2 - angle1
}

// Function to find distance between two points
fun PointF.distanceTo(other: PointF): Float {
    val dx = other.x - x
    val dy = other.y - y
    return sqrt(dx * dx + dy * dy)
}
