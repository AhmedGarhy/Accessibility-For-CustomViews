package com.garhy.acessbilityforcustomviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import java.util.*


class ModulesCustomView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val INVALID_VALUE = -1

    val diameterDp = 48f
    val spacingWidthDp = 10f
    val strokeWidthDp = 2f

    var moduleCount = 5

    lateinit var modulesRectangles: ArrayList<Rect>
    var paintComplete: Paint
    var paintOutline: Paint

    var density: Float = 0f
    var diameter: Float = 0f
    var strokeWidth: Float = 0f
    var spacingWidth: Float = 0f

    var mostRecentKeyCode: Int = 0

    var exploreByTouchHelper: ModuleExploreByTouch


    var modules: Array<Boolean> = arrayOf(false, false, false, false, false)


    var willFitInWidth: Int = 0

    init {

        exploreByTouchHelper = ModuleExploreByTouch(this)
        ViewCompat.setAccessibilityDelegate(this, exploreByTouchHelper)

        this.isFocusable = true
        val displayMetrics = context.resources.displayMetrics
        density = displayMetrics.density
        diameter = diameterDp * density
        strokeWidth = strokeWidthDp * density
        spacingWidth = spacingWidthDp * density

        paintComplete = Paint(Paint.ANTI_ALIAS_FLAG)
        paintComplete.style = Paint.Style.FILL
        paintComplete.setARGB(255, 240, 90, 39)

        paintOutline = Paint(Paint.ANTI_ALIAS_FLAG)
        paintOutline.style = Paint.Style.STROKE
        paintOutline.strokeWidth = strokeWidth
        paintOutline.color = Color.BLACK


    }

    override fun dispatchHoverEvent(event: MotionEvent?): Boolean {
        return exploreByTouchHelper.dispatchHoverEvent(event!!) || super.dispatchHoverEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthNoPadding = measuredWidth - paddingLeft - paddingRight
        willFitInWidth =
            ((widthNoPadding + spacingWidth) / (diameter + strokeWidth + spacingWidth)).toInt()

        var width = 0
        var height = 0
        if (moduleCount <= willFitInWidth) {
            width = ((moduleCount * (diameter + strokeWidth + spacingWidth)) - spacingWidth).toInt()
            height = (diameter + strokeWidth).toInt()
        } else {
            width =
                ((willFitInWidth * (diameter + strokeWidth + spacingWidth)) - spacingWidth).toInt()
            val rows = ((moduleCount - 1) / willFitInWidth) + 1
            height = ((rows * (diameter + strokeWidth + spacingWidth)) - spacingWidth).toInt()
        }

        calcPositions()
        width += paddingLeft + paddingRight
        height += paddingTop + paddingBottom

        setMeasuredDimension(width, height)
    }

    private fun calcPositions() {
        modulesRectangles = ArrayList<Rect>(moduleCount)
        modulesRectangles.add(Rect())
        modulesRectangles.add(Rect())
        modulesRectangles.add(Rect())
        modulesRectangles.add(Rect())
        modulesRectangles.add(Rect())
        for (i in 0 until moduleCount) {
            val yFactor = i / willFitInWidth
            val xFactor = i % willFitInWidth
            val x = paddingLeft + (xFactor * (diameter + strokeWidth + spacingWidth)).toInt()
            val y = paddingTop + (yFactor * (diameter + strokeWidth + spacingWidth)).toInt()
            modulesRectangles.set(i, Rect(x, y, x + diameter.toInt(), y + diameter.toInt()))
        }
    }

    override fun onDraw(canvas: Canvas?) {
        val radius = diameter / 2
        for (i in 0 until moduleCount) {
            val x = modulesRectangles[i].centerX().toFloat()
            val y = modulesRectangles[i].centerY().toFloat()
            if (modules[i])
                canvas?.drawCircle(x, y, radius, paintComplete)
            canvas?.drawCircle(x, y, radius, paintOutline)

        }


    }

    fun getDescriptionForModule(moduleId: Int): String {
        return "module ${moduleId}"
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_UP -> {
                val selectedIndex = findModuleAtPoint(
                    event.x,
                    event.y
                )
                onSelected(selectedIndex)
            }


        }
        return super.onTouchEvent(event)
    }

    private fun onSelected(selectedIndex: Int) {
        if (selectedIndex == INVALID_VALUE)
            return
        modules[selectedIndex] = !modules[selectedIndex]
        invalidate()
        exploreByTouchHelper.invalidateVirtualView(selectedIndex)
        exploreByTouchHelper.sendEventForVirtualView(
            selectedIndex,
            AccessibilityEvent.TYPE_VIEW_CLICKED
        )
    }

    private fun findModuleAtPoint(x: Float, y: Float): Int {
        var selectedIndex = INVALID_VALUE
        for (i in 0 until moduleCount) {
            if (modulesRectangles[i].contains(x.toInt(), y.toInt())) {
                selectedIndex = i
                break
            }
        }
        return selectedIndex
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        mostRecentKeyCode = event!!.keyCode
        return exploreByTouchHelper.dispatchKeyEvent(event)
                || super.dispatchKeyEvent(event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        exploreByTouchHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }


    inner class ModuleExploreByTouch(host: View) : ExploreByTouchHelper(host) {
        override fun getVirtualViewAt(x: Float, y: Float): Int {
            val viewId = this@ModulesCustomView.findModuleAtPoint(x, y)
            return if (viewId == INVALID_VALUE) INVALID_ID else viewId
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>?) {
            for (i in 0 until moduleCount) {
                virtualViewIds?.add(i)
            }
        }

        override fun onVirtualViewKeyboardFocusChanged(virtualViewId: Int, hasFocus: Boolean) {
            if (virtualViewId == INVALID_ID && hasFocus) {
                var direction = View.FOCUS_DOWN
                when (mostRecentKeyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        direction = View.FOCUS_LEFT
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        direction = View.FOCUS_RIGHT
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        direction = View.FOCUS_UP
                    }
                }
                val viewParent = parentForAccessibility
                val nextView = viewParent.focusSearch(this@ModulesCustomView, direction)
                nextView?.let { it.requestFocus(direction) }
            }
        }

        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat
        ) {
            throwExceptionForInvalidIds(virtualViewId)

            node.contentDescription = this@ModulesCustomView.getDescriptionForModule(virtualViewId)
            node.setBoundsInParent(this@ModulesCustomView.modulesRectangles[virtualViewId])

            node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
            node.isCheckable = true
            node.isChecked = modules[virtualViewId]

            node.isFocusable = true
        }

        private fun throwExceptionForInvalidIds(virtualViewId: Int) {
            if (virtualViewId == INVALID_ID) throw Exception("Invalid virtual id !!")
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            when (action) {
                AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                    throwExceptionForInvalidIds(virtualViewId)
                    onSelected(virtualViewId)
                    return true
                }
            }

            return false
        }

    }


}
