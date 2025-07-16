package me.jbusdriver.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.billy.cc.core.component.CC
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.umeng.analytics.MobclickAgent
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_collect_back_edit_item.view.*
import kotlinx.android.synthetic.main.layout_menu_op_item.view.*
import me.jbusdriver.R
import me.jbusdriver.base.*
import me.jbusdriver.base.common.BaseActivity
import me.jbusdriver.base.common.C
import me.jbusdriver.common.JBus
import me.jbusdriver.common.bean.plugin.PluginBean
import me.jbusdriver.db.service.LinkService
import me.jbusdriver.mvp.bean.BackUpEvent
import me.jbusdriver.mvp.bean.Expand_Type_Head
import me.jbusdriver.mvp.bean.MenuOp
import me.jbusdriver.mvp.bean.MenuOpHead
import me.jbusdriver.ui.adapter.MenuOpAdapter
import me.jbusdriver.ui.data.AppConfiguration
import me.jbusdriver.ui.task.LoadCollectService
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 设置页面 Activity
 */
class SettingActivity : BaseActivity() {

    //持有页面模式的临时变量，用于在用户确认前保存选择
    private var pageModeHolder = AppConfiguration.pageMode
    //懒加载获取菜单操作的配置，并转换为可变 Map
    private val menuOpValue by lazy { AppConfiguration.menuConfig.toMutableMap() }


    //懒加载获取备份目录
    private val backDir by lazy {
        //定义备份目录的路径后缀
        val pathSuffix = File.separator + "collect" + File.separator + "backup" + File.separator
        //尝试在外部存储创建目录，如果失败则在应用内部文件目录创建
        val dir: String =
            createDir(Environment.getExternalStorageDirectory().absolutePath + File.separator + JBus.packageName + pathSuffix)
                ?: createDir(JBus.filesDir.absolutePath + pathSuffix)
                ?: error("cant not create collect dir in anywhere") //如果都失败则抛出异常
        File(dir) //返回 File 对象
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting) //设置布局
        setToolBar() //设置 Toolbar
        initSettingView() //初始化设置项视图
        //订阅备份事件，用于更新备份进度UI
        RxBus.toFlowable(BackUpEvent::class.java).throttleLast(100, TimeUnit.MILLISECONDS) //100ms内只取最后一个事件
            .observeOn(AndroidSchedulers.mainThread()) //切换到主线程
            .subscribe({
                tv_collect_backup.text = "正在加载备份${it.path}的第${it.index}/${it.total}个" //更新备份进度文本
                if (it.total == it.index) { //如果备份完成
                    tv_collect_backup.text = "点击备份" //恢复按钮文本
                    tv_collect_backup.isClickable = it.total == it.index //恢复点击
                }

            }, {
                //发生错误时恢复按钮状态
                tv_collect_backup.text = "点击备份"
                tv_collect_backup.isClickable = true
            }).addTo(rxManager) //添加到 rxManager 以便在 Activity 销毁时自动取消订阅
    }

    @SuppressLint("ResourceAsColor")
    private fun initSettingView() {

        //检查已安装插件
        ll_check_plugins.setOnClickListener {
            //通过 CC 组件化框架调用 PluginManager 组件的 'plugins.info' action
            val plugins = CC.obtainBuilder(C.Components.PluginManager)
                .setActionName("plugins.info")
                .build()
                .call()
                .getDataItemWithNoKey<List<PluginBean>>()
            //将插件信息格式化为字符串列表
            val pluginInfos = plugins?.map { "${it.name} : ${it.versionName}" } ?: emptyList()
            //使用 MaterialDialog 显示插件信息
            MaterialDialog.Builder(this)
                .title(if (plugins.isNullOrEmpty()) "没有插件信息!" else "已安装插件")
                .items(pluginInfos)
                .show()
        }


        //页面模式设置
        changePageMode(AppConfiguration.pageMode) //初始化显示当前页面模式
        ll_page_mode_page.setOnClickListener { //点击“分页模式”
            pageModeHolder = AppConfiguration.PageMode.Page
            changePageMode(AppConfiguration.PageMode.Page)
        }
        ll_page_mode_normal.setOnClickListener { //点击“普通模式”
            pageModeHolder = AppConfiguration.PageMode.Normal
            changePageMode(AppConfiguration.PageMode.Normal)
        }

        //菜单操作项设置
        //构建菜单项数据，包含多个头部和子项
        val data: List<MultiItemEntity> = arrayListOf(
            MenuOpHead("个人").apply { MenuOp.mine.forEach { addSubItem(it) } },
            MenuOpHead("有碼").apply { MenuOp.nav_ma.forEach { addSubItem(it) } },
            MenuOpHead("無碼").apply { MenuOp.nav_uncensore.forEach { addSubItem(it) } },
            MenuOpHead("欧美").apply { MenuOp.nav_xyz.forEach { addSubItem(it) } },
            MenuOpHead("其他").apply { MenuOp.nav_other.forEach { addSubItem(it) } }
        )
        val adapter = MenuOpAdapter(data) //创建适配器
        adapter.bindToRecyclerView(rv_menu_op) //绑定到 RecyclerView
        rv_menu_op.layoutManager = GridLayoutManager(viewContext, viewContext.spanCount).apply {
            //设置网格布局，并使头部项占据整行
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (adapter.getItemViewType(position) == Expand_Type_Head) spanCount else 1
            }
        }
        //默认展开包含已选中子项的头部
        val expandItems = data.filterIndexed { _, multiItemEntity ->
            multiItemEntity is MenuOpHead && multiItemEntity.subItems.any { it.isHow }
        }
        expandItems.forEach {
            adapter.expand(data.indexOf(it))
        }
        //设置子项点击事件
        adapter.setOnItemClickListener { _, view, position ->
            (adapter.data.getOrNull(position) as? MenuOp)?.let {
                view.cb_nav_menu?.let { cb ->
                    //同步修改 CheckBox 状态和配置值
                    synchronized(cb) {
                        cb.isChecked = !cb.isChecked
                        menuOpValue[it.name] = cb.isChecked
                    }
                }
            }

        }

        //磁力链接源设置
        loadMagNetConfig()


        //收藏分类开关
        sw_collect_category.isChecked = AppConfiguration.enableCategory //初始化开关状态
        sw_collect_category.setOnCheckedChangeListener { _, isChecked ->
            AppConfiguration.enableCategory = isChecked //监听变化并保存配置
        }

        //备份收藏
        tv_collect_backup.setOnClickListener {
            //显示加载对话框
            val loading = MaterialDialog.Builder(viewContext).content("正在备份...").progress(true, 0).show()
            Flowable.fromCallable { backDir } //获取备份目录
                .flatMap { file ->
                    //查询所有链接并写入备份文件
                    return@flatMap LinkService.queryAll().doOnNext {
                        File(file, "backup${System.currentTimeMillis()}.json").writeText(it.toJsonString())
                    }
                }.compose(SchedulersCompat.single()) //使用 single 调度器执行
                .doAfterTerminate { loading.dismiss() } //结束后关闭对话框
                .subscribeBy(onError = { toast("备份失败,请重新打开app") }, onNext = {
                    toast("备份成功")
                    loadBackUp() //重新加载备份列表
                })
                .addTo(rxManager)
        }

        loadBackUp() //加载已有的备份文件列表

    }

    /**
     * 加载磁力链接配置
     */
    private fun loadMagNetConfig() {
        //通过 CC 异步获取所有可用的磁力链接源 key
        CC.obtainBuilder(C.Components.Magnet)
            .setActionName("allKeys")
            .setTimeout(3000L)
            .build().callAsyncCallbackOnMainThread { cc, result ->
                val allMagnetKeys = if (result.isSuccess) {
                    result.getDataItem<List<String>>("keys")
                } else emptyList()

                //通过 CC 同步获取当前已配置的磁力链接源 key
                val configKeyRes = CC.obtainBuilder(C.Components.Magnet)
                    .setActionName("config.getKeys")
                    .build().call()
                val configMagnetKeys = if (configKeyRes.isSuccess) {
                    configKeyRes.getDataItem<List<String>>("keys")
                } else emptyList()

                //如果获取任一配置失败，显示错误信息
                //if get config failed
                if (!result.isSuccess || !configKeyRes.isSuccess) {
                    tv_magnet_source.text = "初始化配置失败"
                    return@callAsyncCallbackOnMainThread
                }

                //设置磁力链接源点击事件
                ll_magnet_source.setOnClickListener {
                    //获取当前已选中的 key
                    val selectItems = AppConfiguration.magnetKeys.toMutableSet()
                    //显示多选对话框
                    MaterialDialog.Builder(this)
                        .title("磁力源选择")
                        .items(allMagnetKeys)
                        .itemsCallbackMultiChoice(selectItems.map { allMagnetKeys.indexOf(it) }.filter { it >= 0 }.toTypedArray()) { _, which, _ ->
                            //更新选中项
                            val choices = which.map { allMagnetKeys[it] }
                            AppConfiguration.magnetKeys = choices.toSet()
                            //通过 CC 保存配置
                            CC.obtainBuilder(C.Components.Magnet)
                                .setActionName("config.setKeys")
                                .addParam("keys", choices)
                                .build().callAsync()
                            //更新UI显示
                            tv_magnet_source.text = choices.joinToString(separator = ",")
                            true
                        }
                        .positiveText("确定")
                        .show()

                }
                //初始化UI显示
                tv_magnet_source.text = configMagnetKeys.joinToString(separator = ",")

            }
    }

    /**
     * 加载备份文件列表
     */
    private fun loadBackUp() {
        ll_collect_backup_files.removeAllViews() //清空现有列表
        val backFiles = backDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList() //获取并排序备份文件
        if (backFiles.isEmpty()) {
            ll_collect_backup_files.visibility = View.GONE //如果没有备份文件则隐藏列表
            return
        }
        ll_collect_backup_files.visibility = View.VISIBLE
        backFiles.forEach { file ->
            //为每个备份文件创建视图项
            val view = layoutInflater.inflate(R.layout.layout_collect_back_edit_item, ll_collect_backup_files, false)
            view.tv_back_file_name.text = file.name
            view.tv_back_file_date.text = DateUtils.getRelativeTimeSpanString(file.lastModified())

            //恢复按钮点击事件
            view.tv_backup_restore.setOnClickListener {
                MaterialDialog.Builder(viewContext).title("恢复备份")
                    .content("是否恢复备份文件:${file.name},此操作会覆盖现有收藏,请谨慎操作")
                    .positiveText("确定").onPositive { _, _ ->
                        val loading = MaterialDialog.Builder(viewContext).content("正在恢复...").progress(true, 0).show()
                        //启动服务执行恢复操作
                        LoadCollectService.start(this, file.absolutePath)
                        loading.dismiss()
                    }.negativeText("取消").show()
            }
            //删除按钮点击事件
            view.tv_backup_delete.setOnClickListener {
                MaterialDialog.Builder(viewContext).title("删除备份")
                    .content("是否删除备份文件:${file.name}")
                    .positiveText("确定").onPositive { _, _ ->
                        if (file.delete()) {
                            toast("删除成功")
                            loadBackUp() //删除后重新加载列表
                        } else {
                            toast("删除失败")
                        }
                    }.negativeText("取消
                    ").show()

            }
            ll_collect_backup_files.addView(view) //将视图项添加到布局
        }

    }

    /**
     * 切换页面模式的UI显示
     */
    private fun changePageMode(mode: AppConfiguration.PageMode) {
        iv_page_mode_normal_check.visibility = if (mode == AppConfiguration.PageMode.Normal) View.VISIBLE else View.INVISIBLE
        iv_page_mode_page_check.visibility = if (mode == AppConfiguration.PageMode.Page) View.VISIBLE else View.INVISIBLE
    }


    /**
     * 设置 Toolbar
     */
    private fun setToolBar() {
        val mToolBar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(mToolBar)
        mToolBar.setNavigationOnClickListener { onBackPressed() }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val title = SpannableStringBuilder("设置")
        title.setSpan(
            ForegroundColorSpan(R.color.white.toColor()),
            0,
            title.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        supportActionBar?.title = title
    }

    /**
     * 退出时保存设置
     */
    override fun onDestroy() {
        AppConfiguration.pageMode = pageModeHolder //保存页面模式
        if (AppConfiguration.menuConfig != menuOpValue) { //如果菜单配置有变动
            AppConfiguration.menuConfig = menuOpValue //保存菜单配置
            RxBus.post(AppConfiguration.MenuOpChanged)
        }
        MobclickAgent.onEvent(this, "setting_exit") //友盟统计事件
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java))
        }
    }
}
