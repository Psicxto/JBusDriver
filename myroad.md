# JBusDriver 开发路线图

## 第一阶段：基础架构搭建 (MVP)

### 1. 项目初始化
- [x] `/.gitignore`
- [x] `/build.gradle`
- [x] `/gradle.properties`
- [x] `/settings.gradle`
- [x] `/version.gradle`
- [x] `/gradlew.bat`
- [x] `/gradle/wrapper/gradle-wrapper.jar`
- [x] `/gradle/wrapper/gradle-wrapper.properties`

### 2. 基础库模块 (`library_base`)
- [x] `/libraries/library_base/build.gradle`
- [x] `/libraries/library_base/src/main/AndroidManifest.xml`
- [x] **MVP 基础**
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/mvp/BaseView.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/mvp/presenter/BasePresenter.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/mvp/presenter/BasePresenterImpl.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/mvp/model/BaseModel.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/mvp/model/AbstractBaseModel.kt`
- [x] **通用 UI 基类**
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/common/BaseActivity.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/common/BaseFragment.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/common/AppBaseActivity.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/common/AppBaseFragment.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/common/AppBaseRecycleFragment.kt`
- [x] **网络框架**
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/http/NetClient.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/http/LoggerInterceptor.java`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/http/OkHttpDownloadProgressManager.kt`
- [x] **工具类**
    - [x] /libraries/library_base/src/main/java/me/jbusdriver/base/ACache.java`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/CacheLoader.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/Gobal.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/JBusManager.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/RxBus.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/SchedulersCompat.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/SimpleSubscriber.kt`
    - [x] `/libraries/library_base/src/main/java/me/jbusdriver/base/BaseExtension.kt`

### 3. 通用数据模型模块 (`library_common_bean`)
- [x] `/libraries/library_common_bean/build.gradle`
- [x] `/libraries/library_common_bean/src/main/AndroidManifest.xml`
- [x] **接口定义**
    - [x] `/libraries/library_common_bean/src/main/java/me/jbusdriver/common/bean/ILink.kt`
- [x] **数据库模型**
    - [x] /libraries/library_common_bean/src/main/java/me/jbusdriver/common/bean/db/Category.kt`
    - [x] `/libraries/library_common_bean/src/main/java/me/jbusdriver/common/bean/db/CategoryTable.kt`
- [x] **插件模型**
    - [x] `/libraries/library_common_bean/src/main/java/me/jbusdriver/common/bean/plugin/PluginBean.kt`
    - [x] `/libraries/library_common_bean/src/main/java/me/jbusdriver/common/bean/plugin/Plugins.kt`

## 第二阶段：核心功能开发

### 1. 主应用模块 (`app`)
- [x] `/app/build.gradle`
- [x] `/app/src/main/AndroidManifest.xml`
- [x] **网络服务接口**
    - [x] `/app/src/main/java/me/jbusdriver/http/JAVBusService.kt`
    - [x] `/app/src/main/java/me/jbusdriver/http/GitHub.kt`
- [x] **数据库**
    - [x] `/app/src/main/java/me/jbusdriver/db/DB.kt`
    - [x] /app/src/main/java/me/jbusdriver/db/AppDBOPenHelper.kt`
    - [x] /app/src/main/java/me/jbusdriver/db/bean/DBBean.kt`
    - [x] `/app/src/main/java/me/jbusdriver/db/dao/CategoryDao.kt`
    - [x] `/app/src/main/java/me/jbusdriver/db/dao/HistoryDao.kt`
    - [x] `/app/src/main/java/me/jbusdriver/db/dao/LinkItemDao.kt`
    - [x] `/app/src/main/java/me/jbusdriver/db/service/Service.kt`
- [x] **MVP 实现**
    - [x] `/app/src/main/java/me/jbusdriver/mvp/Contract.kt`
    - [x] **Beans**
        - [x] `/app/src/main/java/me/jbusdriver/mvp/bean/Bean.kt`
        - [x] `/app/src/main/java/me/jbusdriver/mvp/bean/Movie.kt`
        - [x] `/app/src/main/java/me/jbusdriver/mvp/bean/MovieDetail.kt`
        - [x] /app/src/main/java/me/jbusdriver/mvp/bean/BusEvent.kt`
        - [x] `/app/src/main/java/me/jbusdriver/mvp/bean/Menu.kt`
    - [x] **Presenters**
        - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/HomeMovieListPresenterImpl.kt`
        - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/MovieDetailPresenterImpl.kt`
        - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/MainPresenterImpl.kt`
- [x] **UI 实现**
    - [x] **Activities**
        - [x] `/app/src/main/java/me/jbusdriver/ui/activity/SplashActivity.kt`
        - [x] `/app/src/main/java/me/jbusdriver/ui/activity/MainActivity.kt`
        - [x] `/app/src/main/java/me/jbusdriver/ui/activity/MovieListActivity.kt`
        - [x] `/app/src/main/java/me/jbusdriver/ui/activity/MovieDetailActivity.kt`
        - [x] `/app/src/main/java/me/jbusdriver/ui/activity/SettingActivity.kt`
        - [x] `/app/src/main/java/me/jbusdriver/ui/activity/WatchLargeImageActivity.kt`
    - [x] **Fragments**
        - [x] /app/src/main/java/me/jbusdriver/ui/fragment/HomeMovieListFragment.kt`
        - [x] /app/src/main/java/me/jbusdriver/ui/fragment/AbsMovieListFragment.kt`
    - [x] **Adapters & Holders**
        - [x] /app/src/main/java/me/jbusdriver/ui/holder/BaseHolder.kt`
        - [x] /app/src/main/java/me/jbusdriver/ui/holder/HeaderHolder.kt`
        - [x] /app/src/main/java/me/jbusdriver/ui/holder/RelativeMovieHolder.kt`
    - [x] **通用**
        - [x] `/app/src/main/java/me/jbusdriver/common/AppContext.kt`
        - [x] `/app/src/main/java/me/jbusdriver/common/AppExtension.kt`

### 2. 资源文件
- [x] `/app/src/main/res/layout/*.xml`
- [x] `/app/src/main/res/drawable/*.xml` & images
- [x] `/app/src/main/res/values/*.xml`
- [x] `/app/src/main/res/menu/*.xml`

## 第三阶段：功能完善与组件化

### 1. 更多功能模块
- [ ] **搜索**
    - [x] `/app/src/main/java/me/jbusdriver/ui/activity/SearchResultActivity.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/SearchResultPagesFragment.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/data/enums/SearchType.kt`
- [ ] **分类与演员**
    - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/GenreListPresenterImpl.kt`
    - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/GenrePagePresenterImpl.kt`
    - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/ActressLinkPresenterImpl.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/GenreListFragment.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/GenrePagesFragment.kt`
    - [x] /app/src/main/java/me/jbusdriver/ui/fragment/ActressListFragment.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/adapter/GenreAdapter.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/adapter/ActressInfoAdapter.kt`
- [ ] **收藏与历史**
    - [x] `/app/src/main/java/me/jbusdriver/mvp/model/CollectModel.kt`
    - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/HistoryPresenterImpl.kt`
    - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/MineCollectPresenterImpl.kt`
    - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/MovieCollectPresenterImpl.kt`
    - [x] /app/src/main/java/me/jbusdriver/mvp/presenter/ActressCollectPresenterImpl.kt`
    - [x] `/app/src/main/java/me/jbusdriver/mvp/presenter/LinkCollectPresenterImpl.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/HistoryFragment.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/MineCollectFragment.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/MovieCollectFragment.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/ActressCollectFragment.kt`
    - [x] `/app/src/main/java/me/jbusdriver/ui/fragment/LinkCollectFragment.kt`

### 2. 组件化改造
- [x] `/buildscripts/cc-settings-2.gradle`
- [x] /buildscripts/cc-settings-2-app.gradle`
- [x] /buildscripts/plugin-common.gradle`
- [x] **拦截器组件 (`component_interceptors`)**
    - [x] `/component_interceptors/build.gradle`
    - [x] `/component_interceptors/src/main/AndroidManifest.xml`
    - [x] `/component_interceptors/src/main/java/me/jbusdriver/component/interceptors/LogInterceptor.kt`

## 第四阶段：插件化开发

### 1. 磁力链接组件 (`component_magnet`)
- [x] `/component_magnet/build.gradle`
- [x] `/component_magnet/src/main/AndroidManifest.xml`
- [ ] **MVP**
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/mvp/MagnetContract.kt`
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/mvp/bean/Magnet.kt`
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/mvp/presenter/MagnetListPresenterImpl.kt`
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/mvp/presenter/MagnetPagerPresenterImpl.kt`
- [ ] **UI**
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/ui/activity/MagnetPagerListActivity.kt`
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/ui/fragment/MagnetListFragment.kt`
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/ui/fragment/MagnetPagersFragment.kt`
- [ ] **组件入口**
    - [x] `/component_magnet/src/main/java/me/jbusdriver/component/magnet/ComponentMagnet.kt`

### 2. 插件管理组件 (`component_plugin_manager`)
- [x] `/component_plugin_manager/build.gradle`
- [x] `/component_plugin_manager/src/main/AndroidManifest.xml`
- [x] `/component_plugin_manager/src/main/java/me/jbusdriver/component/plugin/manager/PluginManagerComponent.kt`
- [x] `/component_plugin_manager/src/main/java/me/jbusdriver/component/plugin/manager/task/DownloadService.kt`
- [x] `/component_plugin_manager/src/main/java/me/jbusdriver/component/plugin/manager/task/PluginService.kt`

### 3. 磁力链接插件 (`plugin_magnet`)
- [x] `/plugins/plugin_magnet/build.gradle`
- [x] `/plugins/plugin_magnet/src/main/AndroidManifest.xml`
- [ ] **接口与服务**
    - [x] `/plugins/plugin_magnet/src/main/java/me/jbusdriver/plugin/magnet/IMagnetLoader.kt`
    - [x] `/plugins/plugin_magnet/src/main/java/me/jbusdriver/plugin/magnet/MagnetService.kt`
- [ ] **加载器实现**
    - [x] `/plugins/plugin_magnet/src/main/java/me/jbusdriver/plugin/magnet/loaders/*.kt` (all loaders)
- [ ] **插件入口**
    - [x] `/plugins/plugin_magnet/src/main/java/me/jbusdriver/plugin/magnet/app/PluginMagnetApp.kt`

## 第五阶段：收尾与发布

- [ ] **调试工具**
    - [x] `/app/src/main/java/me/jbusdriver/debug/stetho/StethoProvider.kt`
- [ ] **API 配置**
    - [x] `/api/announce.json`
- [ ] **文档**
    - [x] `/README.md`
    - [x] `/BUGFIX_IMAGE_PREVIEW.md`
- [x] **ProGuard 混淆规则**
      - [x] `/*/proguard-rules.pro` (all modules)
- [x] **测试**
    - [x] `/*/src/androidTest/**`
     - [x] `/*/src/test/**`
- [ ] **发布应用**