package my.huda.paintapp.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.TouchDelegate
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import my.huda.paintapp.R

/**
 * TODO: document your custom view class.
 */
class PaintButton(context: Context, attrs: AttributeSet) : AppCompatButton(context, attrs) {

    private var color: Int = Color.BLACK

    init {
        // Set default background to transparent
        setBackgroundColor(Color.TRANSPARENT)
        // Set default text color to white
        setTextColor(Color.WHITE)

        // Read the custom attribute "color"
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PaintButton)
        color = typedArray.getColor(R.styleable.PaintButton_color, Color.BLACK)
        typedArray.recycle()

        // Set the button's color
        setColor(color)
    }

    override fun onDraw(canvas: Canvas) {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = color
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, paint)
    }

    fun setColor(color: Int) {
        this.color = color
        invalidate()
    }

    fun getColor(): Int {
        return color
    }

    fun getStroke(): Float {
        return width / 2f
    }

    fun setSize(size: Int, margin: Int) {
        // Set the button's size
        val lp = layoutParams as ViewGroup.MarginLayoutParams
        lp.width = size
        lp.height = size

        // Set the button's margins
        lp.leftMargin = margin
        lp.topMargin = margin
        lp.rightMargin = margin
        lp.bottomMargin = margin

        // Update the layout parameters
        layoutParams = lp

        // Redraw the view
        invalidate()
    }

}
