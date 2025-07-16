// 定义包名
package me.jbusdriver.common

// 导入所需类
import me.jbusdriver.base.glide.GlideNoHostUrl // Glide 自定义 URL 模型
import me.jbusdriver.http.JAVBusService // JAVBus 网络服务接口


/**
 * 将一个不包含 host 的 URL 字符串转换为 Glide 可识别的 GlideNoHostUrl 对象。
 * 这通常用于处理那些 URL 路径相同但域名可能变化的图片链接。
 * @return GlideNoHostUrl 实例，包含了原始 URL、备选域名列表和默认域名。
 */
val String.toGlideNoHostUrl: GlideNoHostUrl
    inline get() = GlideNoHostUrl(this, JAVBusService.xyzHostDomains, JAVBusService.defaultFastUrl)

/**
 * 检查字符串（通常是 URL）是否以已知的 xyz 域名之一结尾。
 * @return 如果字符串以任何一个 xyz 域名结尾，则返回 true，否则返回 false。
 */
val String.isEndWithXyzHost
    get() = JAVBusService.xyzHostDomains.any { this.endsWith(it) }