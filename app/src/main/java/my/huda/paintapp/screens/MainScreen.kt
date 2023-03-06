package my.huda.paintapp.screens

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import my.huda.paintapp.R
import my.huda.paintapp.components.PaintButton
import my.huda.paintapp.components.PaintView.Companion.currentBrush
import my.huda.paintapp.components.PaintView.Companion.currentStroke
import my.huda.paintapp.components.PaintView.Companion.selectedPathIndex


class MainScreen : AppCompatActivity() {
    private var isExtended = false
    private var selectedColor = Color.BLACK
    private var selectedStroke = 12f
    companion object{
        var path = Path()
        var paintBrush = Paint()
        var eraserMode = false
        var selectMode = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)
        supportActionBar?.hide()

        val panel = findViewById<FrameLayout>(R.id.fullPanel)
        val paintTools = panel.findViewById<View>(R.id.paint_tools)
        val strokeTools = panel.findViewById<View>(R.id.paint_strokes)
        val colorTools = panel.findViewById<View>(R.id.paint_colors)
        val penButton = paintTools.findViewById<Button>(R.id.pen)
        val eraserButton = paintTools.findViewById<Button>(R.id.eraser)
        val selectButton = paintTools.findViewById<Button>(R.id.selector)

        val blackColorClick = colorTools.findViewById<FrameLayout>(R.id.blackButton)
        val blackColor = blackColorClick.findViewById<PaintButton>(R.id.blackButtonCircle)
        val brownColorClick = colorTools.findViewById<FrameLayout>(R.id.brownButton)
        val brownColor = brownColorClick.findViewById<PaintButton>(R.id.brownButtonCircle)
        val orangeColorClick = colorTools.findViewById<FrameLayout>(R.id.orangeButton)
        val orangeColor = orangeColorClick.findViewById<PaintButton>(R.id.orangeButtonCircle)
        val yellowColorClick = colorTools.findViewById<FrameLayout>(R.id.yellowButton)
        val yellowColor = yellowColorClick.findViewById<PaintButton>(R.id.yellowButtonCircle)
        val greenColorClick = colorTools.findViewById<FrameLayout>(R.id.greenButton)
        val greenColor = greenColorClick.findViewById<PaintButton>(R.id.greenButtonCircle)
        val blueColorClick = colorTools.findViewById<FrameLayout>(R.id.blueButton)
        val blueColor = colorTools.findViewById<PaintButton>(R.id.blueButtonCircle)
        val purpleColorClick = colorTools.findViewById<FrameLayout>(R.id.purpleButton)
        val purpleColor = purpleColorClick.findViewById<PaintButton>(R.id.purpleButtonCircle)

        val colorList = listOf(blackColor, brownColor, orangeColor, yellowColor, greenColor, blueColor, purpleColor)
        val colorClickList = listOf(blackColorClick, brownColorClick, orangeColorClick, yellowColorClick, greenColorClick, blueColorClick, purpleColorClick)

        for(colorClick in colorClickList){
            colorClick.setOnClickListener {
                for(color in colorList){
                    if(color.parent != colorClick)
                    {
                        color.setSize(105, 32)
                    }
                    else {
                        color.setSize(150, 9)
                        paintBrush.color = color.getColor()
                        selectedColor = color.getColor()
                        currentBrush = color.getColor()
                        path = Path()
                    }
                }
            }
        }

        val stroke10dpClick = strokeTools.findViewById<FrameLayout>(R.id.brushButton10dp)
        val stroke10dp = stroke10dpClick.findViewById<PaintButton>(R.id.brushButton10dpCircle)
        val stroke14dpClick = strokeTools.findViewById<FrameLayout>(R.id.brushButton14dp)
        val stroke14dp = stroke14dpClick.findViewById<PaintButton>(R.id.brushButton14dpCircle)
        val stroke18dpClick = strokeTools.findViewById<FrameLayout>(R.id.brushButton18dp)
        val stroke18dp = stroke18dpClick.findViewById<PaintButton>(R.id.brushButton18dpCircle)
        val stroke22dpClick = strokeTools.findViewById<FrameLayout>(R.id.brushButton22dp)
        val stroke22dp = stroke22dpClick.findViewById<PaintButton>(R.id.brushButton22dpCircle)
        val stroke26dpClick = strokeTools.findViewById<FrameLayout>(R.id.brushButton26dp)
        val stroke26dp = stroke26dpClick.findViewById<PaintButton>(R.id.brushButton26dpCircle)
        val stroke30dpClick = strokeTools.findViewById<FrameLayout>(R.id.brushButton30dp)
        val stroke30dp = stroke30dpClick.findViewById<PaintButton>(R.id.brushButton30dpCircle)
        val stroke34dpClick = strokeTools.findViewById<FrameLayout>(R.id.brushButton34dp)
        val stroke34dp = stroke34dpClick.findViewById<PaintButton>(R.id.brushButton34dpCircle)

        val strokeList = listOf(stroke10dp, stroke14dp, stroke18dp, stroke22dp, stroke26dp, stroke30dp, stroke34dp)
        val strokeClickList = listOf(stroke10dpClick, stroke14dpClick, stroke18dpClick, stroke22dpClick, stroke26dpClick, stroke30dpClick, stroke34dpClick)

        for(strokeClick in strokeClickList){
            strokeClick.setOnClickListener {
                for(stroke in strokeList){
                    if(stroke.parent != strokeClick)
                    {
                        stroke.setColor(Color.BLACK)
                    }
                    else {
                        stroke.setColor(Color.WHITE)
                        paintBrush.strokeWidth = stroke.getStroke()
                        currentStroke = stroke.getStroke()
                        selectedStroke = stroke.getStroke()
                        path = Path()
                    }
                }
            }
        }



        penButton.setOnClickListener {
            // do something
            eraserMode = false
            selectMode = false
            selectedPathIndex = -1
            extendPanel(panel, paintTools, strokeTools, colorTools)
            paintBrush.color = selectedColor
            paintBrush.strokeWidth = selectedStroke
            currentBrush = selectedColor
            currentStroke = selectedStroke
            path = Path()
        }

        eraserButton.setOnClickListener {
            isExtended = true
            eraserMode = true
            selectMode = false
            selectedPathIndex = -1
            extendPanel(panel, paintTools, strokeTools, colorTools)
        }

        selectButton.setOnClickListener {
            isExtended = true
            eraserMode = false
            selectMode = true
            extendPanel(panel, paintTools, strokeTools, colorTools)
        }

    }

    private fun extendPanel(panel: FrameLayout, paintTools: View, strokeTools: View, colorTools: View){
        if (!isExtended) {
            isExtended = true
            panel.layoutParams.height = 650
            paintTools.visibility = View.VISIBLE
            strokeTools.visibility = View.VISIBLE
            colorTools.visibility = View.VISIBLE

        }
        else {
            panel.layoutParams.height = 350
            paintTools.visibility = View.VISIBLE
            strokeTools.visibility = View.GONE
            colorTools.visibility = View.GONE
            isExtended = false
        }

    }
}