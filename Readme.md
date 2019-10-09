# BouncingFloatingViewLayout —— 一个好看的浮动按钮底栏
可以指定一个控件作为浮动控件，然后就可以拖拽他，松手就会弹回，模拟物理效果
还支持重力感应，可以晃动手机，来将他甩走，然后再弹回来


![效果图](https://github.com/gtf35/BouncingFloatingButton/blob/master/demo1.jpg)

![效果图](https://github.com/gtf35/BouncingFloatingButton/blob/master/demo2.jpg)

## 使用：
### 在 app 级别的 build.gradle 添加

```Gradle
dependencies {
   ....
   implementation 'top.gtf35.lib:BouncingFloatingButton:1.1'
   ....
}
```

### layout 的使用参考 demo 的 [layout](https://github.com/gtf35/BouncingFloatingButton/blob/master/app/src/main/res/layout/activity_main.xml) 里面有详细的说明


### api 的使用可参考 demo 的 [MainActivity.kt](https://github.com/gtf35/BouncingFloatingButton/blob/master/app/src/main/java/top/gtf35/bouncingfloatingbutton/MainActivity.kt)

获取浮动按钮ID BouncingFloatingViewLayout.getFloatViewID()
底栏主布局ID BouncingFloatingViewLayout.getMainLayoutID()
获取是否支持重力感应 BouncingFloatingViewLayout.getIsSupportSensor()
设置是否支持重力感应 BouncingFloatingViewLayout.setIsSupportSensor(Boolean)
获取是否启用了拖拽 BouncingFloatingViewLayout.getIsEnableBounce()
设置是否启用了拖拽 BouncingFloatingViewLayout.setIsEnableBounce(Boolean)
更改背景颜色 BouncingFloatingViewLayout.setMainLayoutBackgroundColor(int color)

## 注意
作为悬浮按钮的 View 在启用拖拽的情况下，将无法接受除了 onClick 之外的任何用户触发

## 作者 gtf35
## 邮箱 gtfdeyouxiang@gmail.com

娱乐的项目，我觉得通用性略下，就当练手了