# JBusDriver

一个基于现代Android架构的视频资源浏览应用，采用组件化和插件化设计。

## 项目概述

[加入telegram群](https://t.me/joinchat/HBJbEA-ka9TcWzaxjmD4hw) 感谢网友的帮助!

1. 受到[JAViewer](https://github.com/SplashCodes/JAViewer) 启发写的JAVBUS的APP. 感谢原作者
2. 采用流行的 kotlin+mvp+rxjava2+retrofit2+okhttp3 搭建
3. 尽情使用,有相关bug,问题或者好的需求可以issues,同样欢迎pull request
4. 自1.2.14采用[组件化](https://github.com/luckybilly/CC)构建,1.2.16采用[插件化](https://github.com/ManbangGroup/Phantom)功能

## 版本信息

[最新版本1.2.18](https://github.com/Ccixyj/JBusDriver/releases)

## 技术架构

### 核心技术栈

- **开发语言**: Kotlin + Java
- **架构模式**: MVP (Model-View-Presenter)
- **响应式编程**: RxJava2 + RxAndroid + RxKotlin
- **网络框架**: Retrofit2 + OkHttp3
- **图片加载**: Glide 4.9.0
- **数据解析**: Gson + Jsoup
- **数据库**: SQLBrite
- **组件化框架**: [CC (Component Caller)](https://github.com/luckybilly/CC)
- **插件化框架**: [Phantom](https://github.com/ManbangGroup/Phantom)
- **UI框架**: Android Support Library 28.0.0

### 架构设计特点

#### 1. MVP架构模式
- **BaseView**: 定义视图层基础接口，包含加载状态管理
- **BasePresenter**: 定义业务逻辑层基础接口，管理视图生命周期
- **Contract**: 定义各模块的MVP契约接口
- **Model**: 数据层，负责网络请求和数据处理

#### 2. 组件化架构
采用CC框架实现组件化，主要组件包括：
- **app**: 主应用模块，负责整体架构和主要业务逻辑
- **component_magnet**: 磁力链接组件，处理磁力资源相关功能
- **component_plugin_manager**: 插件管理组件，负责插件的下载和管理
- **component_interceptors**: 拦截器组件，处理全局拦截逻辑

#### 3. 插件化架构
基于Phantom框架实现插件化：
- **plugin_magnet**: 磁力链接插件，可独立运行和动态加载
- 支持插件的动态下载、安装和卸载
- 插件与宿主应用通过标准化接口通信

#### 4. 基础库设计
- **library_base**: 核心基础库，包含MVP基础类、网络框架、工具类等
- **library_common_bean**: 通用数据模型库，定义共享的数据结构

## 📁 项目结构

```
JBusDriver/
├── app/                                    # 主应用模块
│   ├── src/main/
│   │   ├── java/me/jbusdriver/
│   │   │   ├── common/                     # 应用通用类
│   │   │   │   ├── AppContext.kt           # 应用上下文管理
│   │   │   │   ├── AppExtension.kt         # 应用扩展函数
│   │   │   │   └── JBusApplicationLike.java # 应用入口类(已弃用Tinker)
│   │   │   ├── db/                         # 数据库相关
│   │   │   │   ├── AppDBOPenHelper.kt      # 数据库打开助手
│   │   │   │   ├── DB.kt                   # 数据库配置
│   │   │   │   ├── bean/                   # 数据库实体
│   │   │   │   │   └── DBBean.kt           # 数据库Bean定义
│   │   │   │   ├── dao/                    # 数据访问对象
│   │   │   │   │   ├── CategoryDao.kt      # 分类数据访问
│   │   │   │   │   ├── DaoExtend.kt        # DAO扩展
│   │   │   │   │   ├── HistoryDao.kt       # 历史记录数据访问
│   │   │   │   │   └── LinkItemDao.kt      # 链接项数据访问
│   │   │   │   └── service/                # 数据库服务
│   │   │   │       └── Service.kt          # 数据库服务实现
│   │   │   ├── debug/                      # 调试相关
│   │   │   │   └── stetho/                 # Stetho调试工具
│   │   │   │       └── StethoProvider.kt   # Stetho提供者
│   │   │   ├── http/                       # 网络请求
│   │   │   │   ├── GitHub.kt               # GitHub API接口
│   │   │   │   └── JAVBusService.kt        # JAVBus服务接口
│   │   │   ├── mvp/                        # MVP架构实现
│   │   │   │   ├── Contract.kt             # MVP契约接口定义
│   │   │   │   ├── bean/                   # 数据模型
│   │   │   │   │   ├── Bean.kt             # 基础Bean
│   │   │   │   │   ├── BeanTransform.kt    # Bean转换
│   │   │   │   │   ├── BusEvent.kt         # 事件总线事件
│   │   │   │   │   ├── Menu.kt             # 菜单数据模型
│   │   │   │   │   ├── Movie.kt            # 电影数据模型
│   │   │   │   │   ├── MovieDetail.kt      # 电影详情模型
│   │   │   │   │   ├── RecommendBean.kt    # 推荐数据模型
│   │   │   │   │   └── WapperBean.kt       # 包装Bean
│   │   │   │   ├── model/                  # 数据模型层
│   │   │   │   │   └── CollectModel.kt     # 收藏模型
│   │   │   │   └── presenter/              # 表现层实现
│   │   │   │       ├── ActressCollectPresenterImpl.kt    # 女优收藏Presenter
│   │   │   │       ├── ActressLinkPresenterImpl.kt       # 女优链接Presenter
│   │   │   │       ├── BaseAbsCollectPresenter.kt        # 抽象收藏Presenter基类
│   │   │   │       ├── BaseCollectPresenter.kt           # 收藏Presenter基类
│   │   │   │       ├── GenreListPresenterImpl.kt         # 类型列表Presenter
│   │   │   │       ├── GenrePagePresenterImpl.kt         # 类型页面Presenter
│   │   │   │       ├── HistoryPresenterImpl.kt           # 历史记录Presenter
│   │   │   │       ├── HomeMovieListPresenterImpl.kt     # 首页电影列表Presenter
│   │   │   │       ├── HotRecommendPresenterImpl.kt      # 热门推荐Presenter
│   │   │   │       ├── LinkAbsPresenterImpl.kt           # 抽象链接Presenter
│   │   │   │       ├── LinkCollectPresenterImpl.kt       # 链接收藏Presenter
│   │   │   │       ├── MainPresenterImpl.kt              # 主页Presenter
│   │   │   │       ├── MineCollectPresenterImpl.kt       # 我的收藏Presenter
│   │   │   │       ├── MovieCollectPresenterImpl.kt      # 电影收藏Presenter
│   │   │   │       ├── MovieDetailPresenterImpl.kt       # 电影详情Presenter
│   │   │   │       ├── MovieLinkPresenterImpl.kt         # 电影链接Presenter
│   │   │   │       └── MovieParsePresenterImpl.kt        # 电影解析Presenter
│   │   │   └── ui/                         # UI相关
│   │   │       ├── activity/               # Activity
│   │   │       │   ├── MainActivity.kt                   # 主Activity
│   │   │       │   ├── MovieDetailActivity.kt            # 电影详情Activity
│   │   │       │   ├── MovieListActivity.kt              # 电影列表Activity
│   │   │       │   ├── SearchResultActivity.kt          # 搜索结果Activity
│   │   │       │   ├── SettingActivity.kt                # 设置Activity
│   │   │       │   ├── SplashActivity.kt                 # 启动页Activity
│   │   │       │   └── WatchLargeImageActivity.kt        # 大图查看Activity
│   │   │       ├── adapter/                # 适配器
│   │   │       │   ├── ActressInfoAdapter.kt             # 女优信息适配器
│   │   │       │   ├── GenreAdapter.kt                   # 类型适配器
│   │   │       │   ├── GridSpacingItemDecoration.java    # 网格间距装饰器
│   │   │       │   └── MenuOpAdapter.kt                  # 菜单操作适配器
│   │   │       ├── data/                   # 数据配置
│   │   │       │   ├── AppConfiguration.kt              # 应用配置
│   │   │       │   ├── contextMenu/        # 上下文菜单
│   │   │       │   │   └── LinkMenu.kt                   # 链接菜单
│   │   │       │   └── enums/              # 枚举类型
│   │   │       │       ├── DataSourceType.kt            # 数据源类型
│   │   │       │       └── SearchType.kt                # 搜索类型
│   │   │       ├── fragment/               # Fragment
│   │   │       │   ├── AbsMovieListFragment.kt          # 抽象电影列表Fragment
│   │   │       │   ├── ActressCollectFragment.kt        # 女优收藏Fragment
│   │   │       │   ├── ActressListFragment.kt           # 女优列表Fragment
│   │   │       │   ├── GenreListFragment.kt             # 类型列表Fragment
│   │   │       │   ├── GenrePagesFragment.kt            # 类型页面Fragment
│   │   │       │   ├── HistoryFragment.kt               # 历史记录Fragment
│   │   │       │   ├── HomeMovieListFragment.kt         # 首页电影列表Fragment
│   │   │       │   ├── LinkCollectFragment.kt           # 链接收藏Fragment
│   │   │       │   ├── LinkableListFragment.kt          # 可链接列表Fragment
│   │   │       │   ├── LinkedMovieListFragment.kt       # 已链接电影列表Fragment
│   │   │       │   ├── MineCollectFragment.kt           # 我的收藏Fragment
│   │   │       │   ├── MovieCollectFragment.kt          # 电影收藏Fragment
│   │   │       │   └── SearchResultPagesFragment.kt     # 搜索结果页面Fragment
│   │   │       ├── holder/                 # ViewHolder
│   │   │       │   ├── ActressListHolder.kt             # 女优列表ViewHolder
│   │   │       │   ├── BaseHolder.kt                    # 基础ViewHolder
│   │   │       │   ├── CollectDirEditHolder.kt          # 收藏目录编辑ViewHolder
│   │   │       │   ├── GenresHolder.kt                  # 类型ViewHolder
│   │   │       │   ├── HeaderHolder.kt                  # 头部ViewHolder
│   │   │       │   ├── ImageSampleHolder.kt             # 图片样本ViewHolder
│   │   │       │   └── RelativeMovieHolder.kt           # 相关电影ViewHolder
│   │   │       ├── task/                   # 后台任务
│   │   │       │   └── LoadCollectService.kt            # 加载收藏服务
│   │   │       └── widget/                 # 自定义控件
│   │   │           ├── BaseZoomableImageView.java       # 基础可缩放图片视图
│   │   │           ├── ImageGestureListener.java        # 图片手势监听器
│   │   │           ├── MultiTouchZoomableImageView.java # 多点触控缩放图片视图
│   │   │           ├── SampleSizeUtil.java              # 采样大小工具
│   │   │           └── ViewPagerFixed.java              # 修复版ViewPager
│   │   └── res/                            # 资源文件
│   └── build.gradle                        # 应用构建配置
├── component_magnet/                       # 磁力链接组件
│   └── src/main/java/me/jbusdriver/component/magnet/
│       ├── ComponentMagnet.kt              # 磁力链接组件入口
│       ├── ComponentPluginMagnet.kt        # 磁力链接插件组件
│       ├── MagnetPluginHelper.kt           # 磁力链接插件助手
│       ├── mvp/                            # MVP架构
│       │   ├── MagnetContract.kt           # 磁力链接契约接口
│       │   ├── bean/                       # 数据模型
│       │   │   └── Magnet.kt               # 磁力链接数据模型
│       │   └── presenter/                  # 表现层
│       │       ├── MagnetListPresenterImpl.kt    # 磁力链接列表Presenter
│       │       └── MagnetPagerPresenterImpl.kt   # 磁力链接分页Presenter
│       └── ui/                             # UI层
│           ├── activity/                   # Activity
│           │   └── MagnetPagerListActivity.kt    # 磁力链接分页列表Activity
│           ├── config/                     # 配置
│           │   └── Configuration.kt       # 组件配置
│           └── fragment/                   # Fragment
│               ├── MagnetListFragment.kt  # 磁力链接列表Fragment
│               └── MagnetPagersFragment.kt # 磁力链接分页Fragment
├── component_plugin_manager/               # 插件管理组件
│   └── src/main/java/me/jbusdriver/component/plugin/manager/
│       ├── PluginManagerComponent.kt       # 插件管理组件入口
│       └── task/                           # 后台任务
│           ├── DownloadService.kt          # 下载服务
│           └── PluginService.kt            # 插件服务
├── libraries/                              # 基础库模块
│   ├── library_base/                       # 基础功能库
│   │   └── src/main/java/me/jbusdriver/base/
│   │       ├── ACache.java                 # 缓存工具类
│   │       ├── AppGlideOptions.kt          # Glide配置选项
│   │       ├── BaseExtension.kt            # 基础扩展函数
│   │       ├── CacheLoader.kt              # 缓存加载器
│   │       ├── Gobal.kt                    # 全局配置
│   │       ├── JBusManager.kt              # JBus管理器
│   │       ├── RxBus.kt                    # RxBus事件总线
│   │       ├── SchedulersCompat.kt         # 调度器兼容
│   │       ├── SimpleSubscriber.kt         # 简单订阅者
│   │       ├── cc/                         # CC组件化框架
│   │       │   ├── GsonParamConverter.java # Gson参数转换器
│   │       │   └── debug/                  # 调试相关
│   │       │       └── BaseApp.kt          # 基础应用类
│   │       ├── common/                     # 通用基础类
│   │       │   ├── AppBaseActivity.kt      # 应用基础Activity
│   │       │   ├── AppBaseFragment.kt      # 应用基础Fragment
│   │       │   ├── AppBaseRecycleFragment.kt # 应用基础RecyclerView Fragment
│   │       │   ├── BaseActivity.kt         # 基础Activity
│   │       │   ├── BaseFragment.kt         # 基础Fragment
│   │       │   └── C.java                  # 常量类
│   │       ├── db/                         # 数据库相关
│   │       │   └── SDCardDatabaseContext.kt # SD卡数据库上下文
│   │       ├── glide/                      # Glide图片加载
│   │       │   └── NoHostUrlLoader.kt      # 无主机URL加载器
│   │       ├── http/                       # 网络请求
│   │       │   ├── LoggerInterceptor.java  # 日志拦截器
│   │       │   ├── NetClient.kt            # 网络客户端
│   │       │   └── OkHttpDownloadProgressManager.kt # 下载进度管理器
│   │       ├── mvp/                        # MVP基础架构
│   │       │   ├── BaseView.kt             # 基础视图接口
│   │       │   ├── bean/                   # 数据模型
│   │       │   │   ├── PageInfo.kt         # 分页信息
│   │       │   │   └── ResultPageBean.kt   # 结果分页Bean
│   │       │   ├── model/                  # 模型层
│   │       │   │   ├── AbstractBaseModel.kt # 抽象基础模型
│   │       │   │   └── BaseModel.kt        # 基础模型
│   │       │   └── presenter/              # 表现层
│   │       │       ├── AbstractRefreshLoadMorePresenterImpl.kt # 抽象刷新加载更多Presenter
│   │       │       ├── BasePresenter.kt    # 基础Presenter接口
│   │       │       ├── BasePresenterImpl.kt # 基础Presenter实现
│   │       │       └── loader/             # 加载器
│   │       │           ├── PresenterFactory.kt # Presenter工厂
│   │       │           └── PresenterLoader.kt  # Presenter加载器
│   │       ├── phantom/                    # Phantom插件框架
│   │       │   └── Helpers.kt              # 插件助手
│   │       └── ui/                         # UI基础组件
│   │           └── fragment/               # Fragment基础组件
│   │               └── TabViewPagerFragment.kt # Tab ViewPager Fragment
│   └── library_common_bean/                # 通用数据模型库
│       └── src/main/java/me/jbusdriver/common/bean/
│           ├── ICollectCategory.kt         # 收藏分类接口
│           ├── ILink.kt                    # 链接接口
│           ├── db/                         # 数据库模型
│           │   ├── Category.kt             # 分类实体
│           │   └── CategoryTable.kt        # 分类表定义
│           └── plugin/                     # 插件模型
│               ├── PluginBean.kt           # 插件Bean
│               └── Plugins.kt              # 插件集合
├── plugins/                                # 插件模块
│   └── plugin_magnet/                      # 磁力链接插件
│       └── src/main/java/me/jbusdriver/plugin/magnet/
│           ├── IMagnetLoader.kt            # 磁力链接加载器接口
│           ├── MagnetService.kt            # 磁力链接服务
│           ├── app/                        # 插件应用
│           │   └── PluginMagnetApp.kt      # 磁力链接插件应用
│           └── loaders/                    # 磁力链接加载器实现
│               ├── BTBCMagnetLoaderImpl.kt         # BTBC磁力链接加载器
│               ├── BTCherryMagnetLoaderImpl.kt     # BTCherry磁力链接加载器
│               ├── BTDBMagnetLoaderImpl.kt         # BTDB磁力链接加载器
│               ├── BTSOWMagnetLoaderImpl.kt        # BTSOW磁力链接加载器
│               ├── BtAntMagnetLoaderImpl.kt        # BtAnt磁力链接加载器
│               ├── BtdiggsMagnetLoaderImpl.kt      # Btdiggs磁力链接加载器
│               ├── BtsoPWMagnetLoaderImpl.kt       # BtsoPW磁力链接加载器
│               ├── CNBtkittyMangetLoaderImpl.kt    # CNBtkitty磁力链接加载器
│               ├── ChaoRenLoaderImpl.kt            # 超人磁力链接加载器
│               ├── CiLiDaoLoaderImpl.kt            # 磁力岛磁力链接加载器
│               ├── DefaultLoaderImpl.kt            # 默认磁力链接加载器
│               ├── EncodeHelper.kt                 # 编码助手
│               ├── MagnetLoaders.kt                # 磁力链接加载器集合
│               ├── TorrentKittyMangetLoaderImpl.kt # TorrentKitty磁力链接加载器
│               ├── WebViewHtmlContentLoader.kt     # WebView HTML内容加载器
│               └── ZZJDMagnetLoaderImpl.kt         # ZZJD磁力链接加载器
├── buildscripts/                           # 构建脚本
│   ├── cc-settings-2-app.gradle           # CC组件化应用配置
│   └── cc-settings-2-lib.gradle           # CC组件化库配置
├── gradle/                                 # Gradle配置
├── build.gradle                            # 项目构建配置
├── settings.gradle                         # 项目设置
└── README.md                               # 项目说明文档
```

## 核心功能模块

### 1. 主要功能
- **视频浏览**: 支持视频列表浏览、详情查看、图片预览
- **搜索功能**: 支持多种搜索类型和条件筛选
- **收藏管理**: 支持视频、演员、链接的收藏和分类管理
- **历史记录**: 自动记录浏览历史，支持历史清理
- **磁力链接**: 集成磁力链接功能，支持资源下载
- **插件系统**: 支持动态插件加载和管理

### 2. 数据管理
- **本地缓存**: 使用ACache实现本地数据缓存
- **数据库存储**: 基于SQLBrite的响应式数据库操作
- **网络请求**: Retrofit2 + OkHttp3实现网络数据获取
- **图片加载**: Glide实现高效图片加载和缓存

### 3. 用户界面
- **Material Design**: 遵循Material Design设计规范
- **响应式布局**: 支持不同屏幕尺寸和方向
- **沉浸式体验**: 支持状态栏沉浸和全屏模式
- **手势操作**: 支持图片缩放、滑动等手势操作

## 开发配置

### 环境要求
- Android Studio 3.1.4+
- Gradle 3.1.4+
- Kotlin 1.3.70+
- Android SDK 28+
- 最低支持 Android 5.0 (API 21)

### 构建配置
- **编译SDK版本**: 28
- **目标SDK版本**: 28
- **最小SDK版本**: 21
- **构建工具版本**: 28.0.3
- **Java版本**: 1.8

### 依赖管理
项目使用统一的版本管理文件 `version.gradle`，主要依赖版本：
- Kotlin: 1.3.70
- RxJava: 2.2.19
- Retrofit: 2.7.2
- OkHttp: 3.12.12
- Glide: 4.9.0
- CC: 2.1.6
- Phantom: 3.1.2

## 组件化特性

### CC组件化框架
- **组件自动注册**: 支持组件的自动发现和注册
- **组件间通信**: 通过CC框架实现组件间的解耦通信
- **组件独立运行**: 支持组件作为独立应用运行和调试
- **多进程支持**: 支持跨进程的组件调用

### 插件化特性
- **动态加载**: 支持插件的动态下载和加载
- **热更新**: 支持插件的热更新和版本管理
- **沙箱隔离**: 插件运行在独立的沙箱环境中
- **标准化接口**: 插件与宿主通过标准化接口通信

## 性能优化

### 内存优化
- **LeakCanary**: 集成内存泄漏检测工具
- **图片优化**: Glide实现图片内存和磁盘缓存
- **对象池**: 复用频繁创建的对象

### 网络优化
- **请求缓存**: OkHttp实现网络请求缓存
- **连接池**: 复用HTTP连接减少延迟
- **压缩传输**: 支持Gzip压缩减少流量

### 启动优化
- **懒加载**: Fragment和组件支持懒加载
- **异步初始化**: 非关键组件异步初始化
- **MultiDex**: 支持多Dex优化启动速度

## 调试和测试

### 调试工具
- **Stetho**: 集成Chrome调试工具
- **日志系统**: 基于Logger的分级日志系统
- **网络监控**: 支持网络请求的实时监控

### 代码质量
- **Kotlin代码规范**: 遵循Kotlin官方代码规范
- **ProGuard混淆**: 发布版本启用代码混淆和优化
- **Lint检查**: 启用Android Lint静态代码检查

## 如何使用

> 番号+[神奇磁力](https://www.coolapk.com/apk/com.magicmagnet)+[mxplayer](https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad)/[aria2](https://github.com/aria2/aria2)

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用开源许可证，具体请查看 LICENSE 文件。

## 支持项目

如果这个项目对你有帮助，欢迎支持开发者：

|  支付宝    |微信  |
| :-----:  | :----:  |
|<img src="https://raw.githubusercontent.com/Ccixyj/Ccixyj.github.io/master/assets/pay/alipay.png" width = "160px" />|<img src="https://raw.githubusercontent.com/Ccixyj/Ccixyj.github.io/master/assets/pay/wechatpay.png" width = "168px" />|

## 免责声明

本项目仅供学习和研究使用，请勿用于商业用途。使用本项目所产生的任何法律责任由使用者自行承担。
