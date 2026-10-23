package com.tangyuanlongniang.pet

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.rgb(255, 245, 235))

        val title = TextView(this)
        title.text = "汤圆龙娘桌宠"
        title.textSize = 28f
        title.setTextColor(Color.rgb(100, 55, 35))
        title.gravity = Gravity.CENTER

        root.addView(title)
        setContentView(root)
    }
}
