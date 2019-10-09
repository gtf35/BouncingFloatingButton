package top.gtf35

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.BounceInterpolator
import android.animation.Animator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.Sensor
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import top.gtf35.bouncingfloatingviewlayout.R

class BouncingFloatingViewLayout @JvmOverloads constructor(var mContext: Context,
                                                           var attributes: AttributeSet?= null,
                                                           defAttr: Int = 0):
    ViewGroup(mContext, attributes, defAttr) {
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        log("onLayout")

        var mainLayoutHeight = mMainLayout.measuredHeight.toFloat()
        var mainLayoutWidth = mMainLayout.measuredWidth.toFloat()
        log("主布局宽/高：[" + mainLayoutWidth + "/" + mainLayoutHeight + "]")
        var floatViewHeight = mFloatView.measuredHeight.toFloat()
        var floatViewWidth = mFloatView.measuredWidth.toFloat()
        mFloatViewHeight = floatViewHeight
        mFloatViewWidth = floatViewWidth
        log("浮动按钮宽/高：[" + floatViewWidth + "/" + floatViewHeight + "]")

        /*计算 浮动按钮 的位置*/
        var floatViewL: Float = (mainLayoutWidth - floatViewWidth) / 2
        var floatViewT: Float = height - mainLayoutHeight - (floatViewWidth / 2)
        var floatViewR: Float = floatViewL + floatViewWidth
        var floatViewB: Float = floatViewT + floatViewHeight
        log("浮动按钮LTRB：[" + floatViewL + "/" + floatViewT + "/" + floatViewR + "/" + floatViewB +"]")
        mFloatView.layout(floatViewL.toInt(), floatViewT.toInt(), floatViewR.toInt(), floatViewB.toInt())

        /*计算悬浮按钮中心点坐标*/
        mFloatViewAddrXOnStart = (floatViewL + (floatViewWidth/2)).toInt()
        mFloatViewAddrYOnStart = (floatViewT + (floatViewHeight/2)).toInt()

        /*计算 主布局 的位置*/
        var mainLayoutL: Float = 0.toFloat();
        //var mainLayoutT: Float = floatViewHeight / 2;
        var mainLayoutT: Float = height - mainLayoutHeight
        var mainLayoutR: Float = mainLayoutL + mainLayoutWidth
        //var mainLayoutB: Float = mainLayoutT + mainLayoutHeight
        var mainLayoutB: Float = height.toFloat()
        log("主布局LTRB：[" + mainLayoutL + "/" + mainLayoutT + "/" + mainLayoutR + "/" + mainLayoutB +"]")
        mMainLayout.layout(mainLayoutL.toInt(), mainLayoutT.toInt(), mainLayoutR.toInt(), mainLayoutB.toInt())

        /*计算贝塞尔的起点，控制点，终点*/
        mBSRStartPointX = mainLayoutL
        mBSRStartPointY = mainLayoutT
        mBSREndPointX = mainLayoutR
        mBSREndPointY = mainLayoutT
        mBSRFlagPointX = mainLayoutWidth / 2
        mBSRFlagPointY = mainLayoutT
        log("起点($mBSRStartPointX,$mBSRStartPointY), 终点($mBSREndPointX,$mBSREndPointY), 控制点($mBSRFlagPointX,$mBSREndPointY)")
    }

    private val sMinMoveLong = ViewConfiguration.get(mContext).scaledTouchSlop//可识别的最小移动距离
    private val sSingleClickTime = 200//最短点击时间间隔

    private var mOutputLog = false//输出日志
    private var mEnableBouncing = true//支持弹跳

    private var mMainLayoutID = 0//主 layout ID
    private var mMainLayoutBackgroundColor = 0//主布局的背景颜色
    private var mFloatViewID = 0//悬浮按钮的 ID
    private lateinit var mMainLayout: View//主 layout 实例
    private lateinit var mFloatView: View//悬浮按钮的实例
    private var mFloatViewHeight = 0.toFloat()//浮动按钮的高
    private var mFloatViewWidth = 0.toFloat()//浮动按钮的宽

    private var mEatTouchEvent = false//是否消费点击事件
    get() {
        if (mEnableBouncing == false) return false
        return field
    }
    private var mFloatTempX = 0//手指按下的x坐标，临时保存
    private var mFloatTempY = 0//手指按下的y坐标，临时保存
    private var mFloatViewAddrXOnStart = 0//浮动按钮的在启动时默认的x值
    private var mFloatViewAddrYOnStart = 0//浮动按钮的在启动时默认的y值
    private var mFloatAnimaRunning = false//是否正在播放动画
    private var mFloatTouchDownTime = 0.toLong()//手指落下时的时间戳

    //贝塞尔起点
    private var mBSRStartPointX: Float = 0.toFloat()
    private var mBSRStartPointY: Float = 0.toFloat()
    //贝塞尔终点
    private var mBSREndPointX: Float = 0.toFloat()
    private var mBSREndPointY: Float = 0.toFloat()
    //贝塞尔控制点
    private var mBSRFlagPointX: Float = 0.toFloat()
    private var mBSRFlagPointY: Float = 0.toFloat()

    private var mPaint = Paint()//画背景颜色的画笔
    private var mBSRPath = Path()//画背景颜色的路径

    /*加速度传感器*/
    private var mSupportSensor = false//支持重力传感器
    private var mSensorMgr =  mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
    private var mSensor = mSensorMgr?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private lateinit var mSensorListener: SensorEventListener
    private var mLatestShuaShuaMoveTime = 0.toLong()//上次摇动手机的时间

    init {
        /*获取 xml 设置的属性*/
        var typedArray = mContext.obtainStyledAttributes(attributes,
            R.styleable.BouncingFloatingViewLayout
        )
        mMainLayoutID = typedArray.getResourceId(
            R.styleable.BouncingFloatingViewLayout_main_layout,
            0)//主 layout id
        mFloatViewID = typedArray.getResourceId(
            R.styleable.BouncingFloatingViewLayout_float_view,
            0)//浮动按钮 id
        mMainLayoutBackgroundColor = typedArray.getColor(
            R.styleable.BouncingFloatingViewLayout_main_layout_background_color,
            Color.CYAN)//主布局背景颜色
        mSupportSensor = typedArray.getBoolean(
            R.styleable.BouncingFloatingViewLayout_support_sensor,
            false)//支持重力感应
        mEnableBouncing = typedArray.getBoolean(
            R.styleable.BouncingFloatingViewLayout_enable_bounce,
            true)//启用弹跳

        setWillNotDraw(false)//关闭不绘制

        log( "mainLayoutID=" + mMainLayoutID + "floutViewID=" + mFloatViewID)
    }

    /*
    * 打印 log
    * */
    fun log(text: String, isError: Boolean = false, isShowToast: Boolean = false){
        var text = "日志==>" + text
        if (isShowToast) Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show()
        if (!mOutputLog) return
        if (!isError)Log.d(javaClass.simpleName, text)
        if (isError) Log.e(javaClass.simpleName, text)

    }

    /*
    * 设置输出日志
    * */
    fun setIsLogOutputable(outputable: Boolean){
        mOutputLog = outputable
    }

    /*
    * 设置支持重力传感器
    * */
    fun setIsSupportSensor(isSupport: Boolean){
        mSupportSensor = isSupport
        if (isSupport) {
            initShuaShuaShua()
        } else {
            if (mSupportSensor != null && mSupportSensor != null)
                mSensorMgr?.unregisterListener(mSensorListener)
        }
    }

    /*
    * 获取是否设置支持重力传感器
    * */
    fun getIsSupportSensor():Boolean{
        return mSupportSensor
    }

    /*
    * 获取主布局的实例
    * */
    fun getMainLayoutID(): Int{
        return mMainLayoutID
    }

    /*
    * 获取浮动按钮实例
    * */
    fun getFloatViewID(): Int{
        return mFloatViewID
    }

    /*
    * 获取背景颜色
    * */
    fun getMainLayloutBackgroundColor():Int{
        return mMainLayoutBackgroundColor
    }

    /*
    * 设置背景颜色
    * */
    fun setMainLayoutBackgroundColor(color: Int){
        mMainLayoutBackgroundColor = color
        invalidate()
    }

    /*
    * 设置是否启用弹跳
    * */
    fun setIsEnableBounce(enable: Boolean){
        mEnableBouncing = enable
    }

    /*
    * 获取是否启用弹跳
    * */
    fun getIsEnableBounce(): Boolean{
        return mEnableBouncing
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        log("onAttachedToWindow")
        mMainLayout = findViewById(mMainLayoutID) //获取主布局的实例
        mFloatView = findViewById(mFloatViewID)//获取浮动按钮的实例
        //mFloatView.setOnClickListener {log("点击了浮动按钮", isShowToast = true)}
        //mMainLayout.setOnClickListener {log("点击了主布局", isShowToast = true)}
        if (mSupportSensor)initShuaShuaShua()
    }

    /*画背景的贝塞尔*/
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setColor(mMainLayoutBackgroundColor)
        mPaint.setStyle(Paint.Style.FILL)
        mPaint.setColor(mMainLayoutBackgroundColor)
        mBSRPath.reset();//重置上一次
        mBSRPath.moveTo(mBSRStartPointX, mBSRStartPointY)//移动到贝塞尔的起始点，准备画
        mBSRPath.quadTo(mBSRFlagPointX, mBSRFlagPointY, mBSREndPointX, mBSREndPointY)//画一次贝塞尔曲线
        mBSRPath.lineTo(width.toFloat(), height.toFloat())//连接到右下角（现在应该在贝塞尔的结束点）
        mBSRPath.lineTo(0.toFloat(), height.toFloat())//连接到左下角
        mBSRPath.lineTo(mBSRStartPointX, mBSRStartPointY)//连接到贝塞尔的起始点，形成封闭的图形
        mBSRPath.close()//封闭路径
        canvas?.drawPath(mBSRPath, mPaint);//画路径
        log("on draw")
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
    }

    /*
    * 给定中心的 xy 坐标来设置悬浮按钮的位置
    * */
    fun setFloatViewAddr(x: Int, y: Int){
        setFloatViewAddrX(x)
        setFloatViewAddrY(y)
    }
    /*用于属性动画的getset*/
    /*
    * 给定中心的 x 坐标来设置悬浮按钮的X
    * */
    fun setFloatViewAddrX(x: Int){
        mFloatView.left = x - (mFloatViewWidth/2).toInt()
        mFloatView.right = x + (mFloatViewWidth/2).toInt()
    }

    /*
    * 给定中心的 y 坐标来设置悬浮按钮的Y
    * */
    fun setFloatViewAddrY(y: Int){
        mFloatView.top = y - (mFloatViewHeight/2).toInt()
        mFloatView.bottom = y + (mFloatViewHeight/2).toInt()
    }

    /*
    * 获取悬浮按钮中心点的 X
    * */
    fun getFloatViewAddrX():Int{
        return mFloatView.left + (mFloatViewWidth/2).toInt()
    }

    /*
    * 获取悬浮按钮中心点的 Y
    * */
    fun getFloatViewAddrY(): Int{
        return mFloatView.top + (mFloatViewHeight/2).toInt()
    }


    /*
    * 设置主布局贝塞尔的控制点
    * */
    fun setMainBSRPointAddr(x: Int, y: Int){
        setMainBSRPointAddrX(x)
        setMainBSRPointAddrY(y)
    }

    /*用于属性动画的getset*/
    /*
    * 给定中心的 y 坐标来设置主布局贝塞尔的控制点的Y
    * */
    fun setMainBSRPointAddrX(x: Int){
        mBSRFlagPointX = x.toFloat()
    }

    /*
    * 给定中心的 x 坐标来设置主布局贝塞尔的控制点的X
    * */
    fun setMainBSRPointAddrY(y: Int){
        mBSRFlagPointY = y.toFloat()
    }

    /*
    * 获取主布局贝塞尔的控制点的 X
    * */
    fun getMainBSRPointAddrX():Int{
        return mBSRFlagPointX.toInt()
    }

    /*
    * 获取主布局贝塞尔的控制点的 Y
    * */
    fun getMainBSRPointAddrY(): Int{
        return mBSRFlagPointY.toInt()
    }

    /*
    * 根据是不是点击在悬浮按钮上来决定是否拦截
    * */
    override fun onInterceptTouchEvent (event: MotionEvent?): Boolean {
        when (event?.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                mEatTouchEvent = isInFloatView(event.x.toInt(), event.y.toInt())
                log("手指放下，在浮动按钮上:$mEatTouchEvent" )
            }
        }
        return mEatTouchEvent
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.getAction()) {
            MotionEvent.ACTION_MOVE -> {
                /*手指这次移动 XY 的绝对值*/
                var moveX = Math.abs(event.x.toInt() - mFloatTempX)
                var moveY = Math.abs(event.y.toInt() - mFloatTempY)
                //这次是不是滑动
                var isMove = (moveX > sMinMoveLong || moveY > sMinMoveLong)
                log("手指在移动，在浮动按钮上$mEatTouchEvent, 是滑动$isMove" )
                //动画不在播放的过程中，需要消费且移动距离大于防抖
                if (!mFloatAnimaRunning && isMove && mEatTouchEvent){
                    setFloatViewAddr(event.x.toInt(),event.y.toInt())//浮动按钮的位置
                    setMainBSRPointAddr(event.x.toInt(),event.y.toInt())//贝塞尔控制点的位置
                }
            }
            MotionEvent.ACTION_DOWN -> {
                //根据手指在不在按钮上判断需不需要消费事件
                mEatTouchEvent = isInFloatView(event.x.toInt(), event.y.toInt())
                //记录按下的坐标和时间
                mFloatTempX = event.x.toInt()
                mFloatTempY = event.y.toInt()
                mFloatTouchDownTime = System.currentTimeMillis()
                log("手指放下，在浮动按钮上:$mEatTouchEvent" )
            }
            MotionEvent.ACTION_UP -> {
                /*手指这次移动 XY 的绝对值*/
                var moveX = Math.abs(event.x.toInt() - mFloatTempX)
                var moveY = Math.abs(event.y.toInt() - mFloatTempY)
                //这次是不是滑动
                var isMove = (moveX > sMinMoveLong || moveY > sMinMoveLong)
                log("手指抬起,isMove$isMove,isTouchTimeLikeClick${isTouchTimeLikeClick()}")
                if(mEatTouchEvent){
                    //需要消费事件
                    if(!isMove && isTouchTimeLikeClick()){
                        mFloatView.callOnClick()//需要消费事件且间隔时间看起来像点击，距离也符合点击
                    } else {
                        if (!mFloatAnimaRunning)floatViewMoveBackAnima()//不是点击而是滑动，显示松手回原位动画
                    }
                }
                mEatTouchEvent = false
            }
        }
        return mEatTouchEvent
    }

    /*
    * 判断 x y 坐标是不是在悬浮按钮上
    * */
    fun isInFloatView(x: Int, y: Int): Boolean{
        log("触摸点：x：" + x + "   y：" + y )
        if (x < mFloatView.right && x > mFloatView.left){
            log("触摸点x在边界内")
            if (y < mFloatView.bottom && y > mFloatView.top) {
                log("触摸点y在边界内")
                log("触摸在浮动按钮上")
                return true
            }
        }
        return false
    }

    /*
    * 根据时间判断是不是点击
    * */
    private fun isTouchTimeLikeClick(): Boolean{
        var oneTime = System.currentTimeMillis()//当前时间戳
        var clickTime = oneTime - mFloatTouchDownTime//点击的耗时
        //log("clickTime = $clickTime")
        if( clickTime < sSingleClickTime ) return true
        return false
    }

    /*
    * 弹回原处的动画
    * */
    fun floatViewMoveBackAnima(){
        /*浮动按钮的属性动画*/
        var xPropertyValuesHolder = PropertyValuesHolder.ofInt("floatViewAddrX", mFloatViewAddrXOnStart)
        var yPropertyValuesHolder = PropertyValuesHolder.ofInt("floatViewAddrY", mFloatViewAddrYOnStart)
        /*主布局背景的贝塞尔动画*/
        var BSRXPropertyValuesHolder = PropertyValuesHolder.ofInt("mainBSRPointAddrX", mFloatViewAddrXOnStart)
        var BSRYPropertyValuesHolder = PropertyValuesHolder.ofInt("mainBSRPointAddrY", mFloatViewAddrYOnStart)

        var floatViewAnimator = ObjectAnimator.ofPropertyValuesHolder(this,
            xPropertyValuesHolder,
            yPropertyValuesHolder,
            BSRXPropertyValuesHolder,
            BSRYPropertyValuesHolder)
        floatViewAnimator.setDuration(700)
        floatViewAnimator.setInterpolator(BounceInterpolator())
        /*添加监听，同步布局状态*/
        floatViewAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator) {
                mFloatAnimaRunning = true
            }

            override fun onAnimationEnd(animator: Animator) {
                mFloatAnimaRunning = false
            }
        })
        floatViewAnimator.start()
    }


    /*
    * 总理感应甩动的动画
    * */
    fun shuashuashuaAnima(x: Int, y: Int){

        /*浮动按钮的属性动画*/
        var xPropertyValuesHolder = PropertyValuesHolder.ofInt("floatViewAddrX", getFloatViewAddrX() + x)
        var yPropertyValuesHolder = PropertyValuesHolder.ofInt("floatViewAddrY", getFloatViewAddrY() - y)
        /*主布局背景的贝塞尔动画*/
        var BSRXPropertyValuesHolder = PropertyValuesHolder.ofInt("mainBSRPointAddrX", getMainBSRPointAddrX() + x)
        var BSRYPropertyValuesHolder = PropertyValuesHolder.ofInt("mainBSRPointAddrY", getMainBSRPointAddrY() - y)

        var floatViewAnimator = ObjectAnimator.ofPropertyValuesHolder(this,
            xPropertyValuesHolder,
            yPropertyValuesHolder,
            BSRXPropertyValuesHolder,
            BSRYPropertyValuesHolder)
        floatViewAnimator.setDuration(300)
        floatViewAnimator.setInterpolator(AccelerateInterpolator())//一直加速，结束突然停止
        /*添加监听，同步布局状态*/
        floatViewAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator) {
                mFloatAnimaRunning = true
            }

            override fun onAnimationEnd(animator: Animator) {
                mFloatAnimaRunning = false
                floatViewMoveBackAnima()
            }
        })
        floatViewAnimator.start()
    }


    private fun initShuaShuaShua(){

        //保存上一次 x y z 的坐标
        var bx = 0f
        var by = 0f
        var bz = 0f
        var btime: Long = 0//这一次的时间
        mSensorListener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[SensorManager.DATA_X]
                val y = e.values[SensorManager.DATA_Y]
                val z = e.values[SensorManager.DATA_Z]
                log("距离x$x, y$y, z$z")
                //计算出 X Y Z的数值下面可以根据这个数值来计算摇晃的速度了
                //速度 = 路程/时间
                //X轴的速度
                val speadX = (x - bx) / (System.currentTimeMillis() - btime) * 300
                //y轴的速度
                val speadY = (y - by) / (System.currentTimeMillis() - btime) * 300
                //z轴的速度
                val speadZ = (z - bz) / (System.currentTimeMillis() - btime) * 300
                //这样简单的速度就可以计算出来，如果你想计算加速度也可以，在运动学里，加速度a与速度，
                //位移都有关系：Vt=V0+at，S=V0*t+1/2at^2， S=（Vt^2-V0^2）/(2a),根据这些信息也可以求解a
                log("加速度x$speadX, y$speadY, z$speadZ")

                var nowTime = System.currentTimeMillis()
                if ((nowTime - mLatestShuaShuaMoveTime) > 1000){
                    if (Math.abs(speadX) > 50 && Math.abs(speadX) < 1500){
                        if (Math.abs(speadY) < 1500 && Math.abs(speadY) > 50){
                            log("加速度验证通过:X$speadX,Y$speadY")
                            shuashuashuaAnima(speadX.toInt(), speadY.toInt())
                            mLatestShuaShuaMoveTime = System.currentTimeMillis()
                        }
                    }
                }
                bx = x
                by = y
                bz = z
                btime = System.currentTimeMillis()
            }

            override fun onAccuracyChanged(s: Sensor, accuracy: Int) {}
        }
        // 注册listener，第三个参数是检测的精确度
        mSensorMgr?.registerListener(mSensorListener, mSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mSupportSensor)mSensorMgr?.unregisterListener(mSensorListener)
    }
}