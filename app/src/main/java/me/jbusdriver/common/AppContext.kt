// 定义包名
package me.jbusdriver.common

// 导入所需的 Android 及第三方库类
import android.app.Application
import android.content.Context
import android.os.Environment
import android.support.multidex.MultiDex // 用于支持 65K 以上方法数
import com.billy.cc.core.component.CC // 组件化框架
import com.orhanobut.logger.AndroidLogAdapter // 日志库
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.squareup.leakcanary.LeakCanary // 内存泄漏检测
import com.umeng.analytics.MobclickAgent // 友盟统计
import com.umeng.commonsdk.UMConfigure
import com.wlqq.phantom.library.PhantomCore // 插件化框架
import com.wlqq.phantom.library.log.ILogReporter
import io.reactivex.plugins.RxJavaPlugins // RxJava 错误处理
import me.jbusdriver.BuildConfig // 构建配置
import me.jbusdriver.base.GSON // JSON 解析
import me.jbusdriver.base.JBusManager // 全局管理器
import me.jbusdriver.base.arrayMapof // 自定义工具
import me.jbusdriver.http.JAVBusService // 网络服务接口
import java.io.File
import java.util.*


// 全局 ApplicationContext 实例，使用 lateinit 延迟初始化
lateinit var JBus: AppContext


// 自定义 Application 类，作为应用的入口点和全局上下文
class AppContext : Application() {

    // 使用 lazy 委托懒加载 JAVBusService 实例缓存，以域名为 key
    val JBusServices by lazy { arrayMapof<String, JAVBusService>() }
    // 判断当前是否为调试模式
    private val isDebug by lazy {
        // 判定条件：1. BuildConfig.DEBUG 为 true；2. 或者在外部存储的应用目录下存在名为 'debug' 的文件
        BuildConfig.DEBUG || File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    packageName
                    + File.separator + "debug"

        ).exists()
    }

    // 配置 Phantom 插件化框架
    private val phantomHostConfig by lazy {
        PhantomCore.Config()
            .setCheckSignature(!isDebug) // 非 debug 模式下检查插件签名
            .setCheckVersion(!BuildConfig.DEBUG) // 非 debug 模式下检查插件版本
            .setDebug(isDebug) // 设置 debug 模式
            .setLogLevel(if (isDebug) android.util.Log.VERBOSE else android.util.Log.WARN) // 设置日志级别
            .setLogReporter(LogReporterImpl()) // 设置日志报告器，用于上报 Phantom 内部的日志和异常
    }

    // 在 Application 创建之前调用，用于初始化 MultiDex
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this); // 启用 MultiDex
    }

    // 在 Application 创建时调用，用于执行各种初始化操作
    override fun onCreate() {
        super.onCreate()
        JBusManager.setContext(this) // 设置全局上下文到 JBusManager
        JBus = this // 初始化全局 AppContext 实例

        // 如果当前进程是 LeakCanary 用于堆分析的进程，则不执行应用初始化
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        // 尽早初始化 Phantom 插件化框架
        PhantomCore.getInstance().init(this, phantomHostConfig)

        // 仅在 debug 模式下执行的初始化操作
        if (isDebug) {
            LeakCanary.install(this) // 初始化 LeakCanary

            // 配置 Logger 日志库
            val formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(true)  // 显示线程信息
                .methodCount(2)         // 显示的方法行数
                .methodOffset(0)        // 隐藏内部方法调用
                .tag("old_driver")   // 设置全局日志标签
                .build()

            // 添加日志适配器，并重写 isLoggable 方法以确保只在 debug 模式下打印日志
            Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
                override fun isLoggable(priority: Int, tag: String?) = isDebug
            })

            // 启用 CC 组件化框架的调试功能
            CC.enableVerboseLog(isDebug)
            CC.enableDebug(isDebug)
            CC.enableRemoteCC(isDebug)
        }

        // 初始化友盟统计 SDK
        UMConfigure.init(this, UMConfigure.DEVICE_TYPE_PHONE, null)
        UMConfigure.setLogEnabled(isDebug) // debug 模式下启用友盟日志
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO) // 自动页面采集
        MobclickAgent.setCatchUncaughtExceptions(true) // 捕获未处理的异常

        // 设置全局 RxJava 错误处理器
        RxJavaPlugins.setErrorHandler {
            try {
                // 在非 debug 模式下，通过友盟上报 RxJava 的错误
                if (!isDebug) MobclickAgent.reportError(this, it)
            } catch (e: Exception) {
                // 忽略上报错误时可能发生的异常
            }
        }


        // 注册 Activity 生命周期回调，由 JBusManager 统一管理
        this.registerActivityLifecycleCallbacks(JBusManager)


    }


    // 当系统内存不足时调用
    override fun onLowMemory() {
        super.onLowMemory()
        JBusServices.clear() // 清理网络服务实例缓存，释放内存
    }

    // 当系统内存紧张，需要清理内存时调用
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        JBusServices.clear() // 清理网络服务实例缓存，释放内存
    }


    // 伴生对象，用于定义静态成员
    companion object {

        // Phantom 插件框架的日志报告器实现
        private class LogReporterImpl : ILogReporter {

            // 上报 Phantom 内部捕获的异常
            override fun reportException(throwable: Throwable, message: HashMap<String, Any>) {
                // 使用友盟统计上报异常
                MobclickAgent.reportError(JBus, throwable)
                MobclickAgent.reportError(JBus, GSON.toJson(message)) // 同时上报附带信息
            }

            // 上报 Phantom 内部的自定义事件（当前未实现）
            override fun reportEvent(eventId: String, label: String, params: HashMap<String, Any>) {
            }

            // 上报 Phantom 内部的日志（当前未实现）
            override fun reportLog(tag: String, message: String) {
            }
        }
    }
}