package com.yankaibang.plugin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.yankaibang.secondlibrary.MyOnClickListener
import com.yankaibang.secondlibrary.TestOnClickListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        text.setOnClickListener {

        }

        text.setOnClickListener(TestOnClickListener())

        Log.d("MainActivity", View.OnClickListener::class.java.name)
    }
}
