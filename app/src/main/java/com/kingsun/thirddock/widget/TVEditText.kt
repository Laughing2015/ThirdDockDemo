package com.kingsun.thirddock.widget

import android.content.Context
import android.util.AttributeSet
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.R

/**
 * @Description: 自定义输入框（兼容tv和手机）
 * @Author: xiaolong.li
 * @CreateDate: 2022/6/2
 */
class TVEditText : AppCompatEditText {
    constructor(context: Context?) : this(context,null)
    constructor(context: Context?, attrs: AttributeSet?) : this(
        context, attrs, R.attr.editTextStyle
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    )


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isDirectKeyCode(keyCode)) {
            var direction = FOCUS_DOWN
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> direction = FOCUS_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> direction = FOCUS_DOWN
                KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> direction = FOCUS_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> direction = FOCUS_RIGHT
            }
            val nextFocus =
                FocusFinder.getInstance().findNextFocus(parent as ViewGroup, this, direction)
            if (nextFocus != null) {
                nextFocus.requestFocus()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isDirectKeyCode(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
    }
}