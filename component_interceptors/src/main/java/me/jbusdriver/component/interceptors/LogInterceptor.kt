// 定义包名
package me.jbusdriver.component.interceptors

// 导入 CC 组件化框架的相关类
import com.billy.cc.core.component.CCResult // CC 调用结果类
import com.billy.cc.core.component.Chain // 拦截器链，用于传递调用
import com.billy.cc.core.component.IGlobalCCInterceptor // 全局拦截器接口
// 导入项目基础库中的类
import me.jbusdriver.base.JBusManager // 全局管理器，用于获取上下文
import me.jbusdriver.base.KLog // 日志工具类

/**
 * 这是一个示例性的全局拦截器，其功能是在组件调用前后打印日志。
 * 全局拦截器会对每一次 CC 调用都生效。
 * @author billy.qi
 * @since 18/5/26 11:42
 */
class LogInterceptor : IGlobalCCInterceptor { // 实现 IGlobalCCInterceptor 接口
    // 定义一个 TAG，通常用于日志过滤
    val TAG = "LogInterceptor"

    /**
     * 定义拦截器的优先级，数字越小，优先级越高，越先执行。
     * @return 返回优先级数值
     */
    override fun priority() = 1

    /**
     * 拦截器核心方法，所有的组件调用都会经过这里。
     * @param chain 拦截器链对象，包含了本次调用的所有信息 (CC object)，并提供了 chain.proceed() 方法以继续执行调用。
     * @return 返回组件调用的结果 (CCResult)。
     */
    override fun intercept(chain: Chain): CCResult {
        // 在组件调用执行前打印日志
        // JBusManager.context.applicationContext.packageName 获取当前应用的包名
        // chain.cc 获取当前的 CC 调用对象，包含了调用方、组件名、action名、参数等信息
        KLog.d("${JBusManager.context.applicationContext.packageName} log before:" + chain.cc)
        
        // 调用 chain.proceed() 将调用传递给下一个拦截器或最终的组件
        // 如果不调用此方法，组件调用将被中断
        val result = chain.proceed()
        
        // 在组件调用执行后打印日志
        // result 是组件执行后返回的 CCResult 对象
        KLog.d("${JBusManager.context.applicationContext.packageName} log after:$result")
        
        // 必须将组件执行的结果返回，以确保调用方能收到正确的响应
        return result
    }
}
