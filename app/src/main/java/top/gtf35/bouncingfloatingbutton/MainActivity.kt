package top.gtf35.bouncingfloatingbutton

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //获取浮动按钮实例
        var mFloatingActionButton = float_bottom_layout.findViewById(float_bottom_layout.getFloatViewID()) as FloatingActionButton
        //获取底栏主布局实例
        var mBottomMainLayout = float_bottom_layout.findViewById(float_bottom_layout.getMainLayoutID()) as ConstraintLayout

        mFloatingActionButton.setOnClickListener { Toast.makeText(this@MainActivity, "点击了浮动按钮", Toast.LENGTH_SHORT).show() }
        mBottomMainLayout.setOnClickListener { Toast.makeText(this@MainActivity, "点击了底栏", Toast.LENGTH_SHORT).show() }

        initBtnTexts()//设置当前的设置到主界面的三个按钮上

        /*设置按钮点击事件*/
        btn_sensor.setOnClickListener {
            //设置是否支持重力感应
            float_bottom_layout.setIsSupportSensor(!float_bottom_layout.getIsSupportSensor())
            initBtnTexts()//刷新文字
        }

        btn_enable_bounce.setOnClickListener {
            //设置是否启用了拖拽
            float_bottom_layout.setIsEnableBounce(!float_bottom_layout.getIsEnableBounce())
            initBtnTexts()//刷新文字
        }

        btn_changecolor.setOnClickListener {
            //更改背景颜色
            float_bottom_layout.setMainLayoutBackgroundColor(Color.BLACK)
        }
    }

    /*
    * 设置当前的设置到主界面的三个按钮上
    * */
    fun initBtnTexts(){
        //获取是否支持重力感应
        if (float_bottom_layout.getIsSupportSensor()){
            //当前支持重力感应，提供关闭选项
            btn_sensor.setText("关闭重力感应")
        } else {
            //当前不支持重力感应，提供支持选项
            btn_sensor.setText("开启重力感应")
        }

        //获取是否启用了拖拽
        if (float_bottom_layout.getIsEnableBounce()){
            //当前启用拖拽，提供禁用选项
            btn_enable_bounce.setText("禁用拖拽")
        } else {
            //当前禁用拖拽，提供启用选项
            btn_enable_bounce.setText("启用拖拽")
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
