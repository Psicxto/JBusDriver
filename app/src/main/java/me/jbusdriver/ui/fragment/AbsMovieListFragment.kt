package me.jbusdriver.ui.fragment

import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.util.MultiTypeDelegate
import me.jbusdriver.R
import me.jbusdriver.base.*
import me.jbusdriver.base.common.C
import me.jbusdriver.common.bean.ILink
import me.jbusdriver.common.isEndWithXyzHost
import me.jbusdriver.common.toGlideNoHostUrl
import me.jbusdriver.mvp.bean.Movie
import me.jbusdriver.mvp.bean.convertDBItem
import me.jbusdriver.mvp.model.CollectModel
import me.jbusdriver.ui.activity.MovieDetailActivity
import me.jbusdriver.ui.data.AppConfiguration
import me.jbusdriver.ui.data.contextMenu.LinkMenu
import me.jbusdriver.ui.data.enums.DataSourceType


/**
 * 电影列表的抽象基类 Fragment，封装了列表展示的通用逻辑。
 * 包括布局管理、数据适配、点击事件处理等。
 */
abstract class AbsMovieListFragment : LinkableListFragment<Movie>() {

    /**
     * 根据传入的参数动态确定数据源类型 (DataSourceType)。
     * 优先从 arguments 中直接获取类型，如果获取不到，则根据 ILink 的 URL 路径进行判断。
     * 这是实现不同数据源（有码、无码、女优、类别等）列表展示的关键。
     */
    override val type: DataSourceType by lazy {
        // 尝试直接从 arguments 中获取 DataSourceType
        arguments?.getSerializable(MOVIE_LIST_DATA_TYPE) as? DataSourceType ?: let {
            // 如果获取不到，则尝试从 ILink 对象中解析
            (arguments?.getSerializable(C.BundleKey.Key_1) as? ILink)?.let { link ->

                val path = link.link.urlPath
                val type = when {
                    // 判断是否为 xyz host
                    link.link.urlHost.isEndWithXyzHost -> {
                        //xyz
                        when {
                            path.startsWith("genre") -> DataSourceType.GENRE //类别
                            path.startsWith("star") -> DataSourceType.ACTRESSES //女优
                            else -> DataSourceType.CENSORED //有码
                        }

                    }
                    else -> {
                        when {
                            path.startsWith("uncensored") -> {
                                //无码
                                when {
                                    path.startsWith("uncensored/genre") -> DataSourceType.GENRE
                                    path.startsWith("uncensored/star") -> DataSourceType.ACTRESSES
                                    else -> DataSourceType.CENSORED
                                }
                            }
                            else -> {
                                //有码
                                when {
                                    path.startsWith("genre") -> DataSourceType.GENRE
                                    path.startsWith("star") -> DataSourceType.ACTRESSES
                                    else -> DataSourceType.CENSORED
                                }
                            }
                        }

                    }

                }
                type

            } ?: DataSourceType.CENSORED // 默认返回有码类型
        }
    }


    /**
     * 懒加载创建列表的适配器 (Adapter)。
     * 使用了 BaseQuickAdapter，并配置了多类型布局 (MultiTypeDelegate) 以支持不同的 item 样式。
     */
    override val adapter: BaseQuickAdapter<Movie, in BaseViewHolder>  by lazy {
        object : BaseQuickAdapter<Movie, BaseViewHolder>(null) {

            // 判断 Movie 对象是否无效（没有番号和链接）
            private val Movie.isInValid
                inline get() = TextUtils.isEmpty(code) && TextUtils.isEmpty(link)

            init {
                // 配置多类型布局代理
                multiTypeDelegate = object : MultiTypeDelegate<Movie>() {
                    override fun getItemType(t: Movie): Int {
                        return when {
                            t.isInValid -> -1 // 无效数据，使用特殊布局
                            // 根据 LayoutManager 的类型判断是垂直列表还是水平（交错网格）列表
                            recyclerView?.layoutManager is LinearLayoutManager -> OrientationHelper.VERTICAL
                            recyclerView?.layoutManager is StaggeredGridLayoutManager -> OrientationHelper.HORIZONTAL
                            else -> 1
                        }
                    }
                }

                // 注册不同 item 类型对应的布局文件
                multiTypeDelegate
                    .registerItemType(-1, R.layout.layout_pager_section_item) // 分页指示器 item
                    .registerItemType(OrientationHelper.VERTICAL, R.layout.layout_page_line_movie_item) // 垂直列表 item
                    .registerItemType(OrientationHelper.HORIZONTAL, R.layout.layout_page_line_movie_item_hor) // 水平（网格）列表 item

            }

            private val dp8 by lazy { this@AbsMovieListFragment.viewContext.dpToPx(8f) }
            private val backColors = listOf(0xff2195f3.toInt(), 0xff4caf50.toInt(), 0xffff0030.toInt()) //蓝,绿,红

            // 生成标签 TextView 的布局参数
            private fun genLp() = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                this@AbsMovieListFragment.viewContext.dpToPx(24f)
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }

            /**
             * 在此方法中实现数据到视图的绑定。
             */
            override fun convert(holder: BaseViewHolder, item: Movie) {
                when (holder.itemViewType) {
                    -1 -> { // 分页指示器 item
                        setFullSpan(holder) // 设置占满一行
                        holder.setText(R.id.tv_page_num, item.title)
                        val currentPage = item.title.toIntOrNull()
                        if (currentPage != null) {
                            // 控制“加载上一页”按钮的显示与否
                            holder.setGone(
                                R.id.tv_load_prev, mBasePresenter?.isPrevPageLoaded(currentPage)
                                    ?: true
                            )
                            // 设置点击事件，加载上一页
                            holder.getView<View>(R.id.tv_load_prev)?.setOnClickListener {
                                mBasePresenter?.jumpToPage(currentPage - 1)
                            }
                        }
                    }
                    OrientationHelper.HORIZONTAL, OrientationHelper.VERTICAL -> { // 电影 item

                        // 根据页面模式决定是否显示分割线
                        when (pageMode) {
                            AppConfiguration.PageMode.Page -> {
                                holder.setGone(R.id.v_line, true)
                            }
                            AppConfiguration.PageMode.Normal -> {
                                holder.setGone(R.id.v_line, false)
                            }
                        }

                        // 绑定电影标题、日期、番号
                        holder.setText(R.id.tv_movie_title, item.title)
                            .setText(R.id.tv_movie_date, item.date)
                            .setText(R.id.tv_movie_code, item.code)

                        // 使用 Glide 加载封面图片
                        GlideApp.with(this@AbsMovieListFragment).load(item.imageUrl.toGlideNoHostUrl)
                            .placeholder(R.drawable.ic_place_holder)
                            .error(R.drawable.ic_place_holder).centerCrop()
                            .into(DrawableImageViewTarget(holder.getView(R.id.iv_movie_img)))

                        // 动态添加电影标签
                        with(holder.getView<LinearLayout>(R.id.ll_movie_hot)) {
                            this.removeAllViews()
                            item.tags?.mapIndexed { index, tag ->
                                (viewContext.inflate(R.layout.tv_movie_tag) as TextView).let {
                                    it.text = tag
                                    if (holder.itemViewType == OrientationHelper.HORIZONTAL) {
                                        it.textSize = 11f
                                    }
                                    it.setPadding(dp8, 0, dp8, 0)
                                    // 设置标签背景色和圆角
                                    it.background = GradientDrawable().apply {
                                        setColor(
                                            backColors.getOrNull(index % 3)
                                                ?: backColors.first()
                                        )
                                        cornerRadius = if (holder.itemViewType == OrientationHelper.HORIZONTAL) {
                                            dp8 * 1.5f
                                        } else {
                                            dp8 * 2f
                                        }
                                    }
                                    // 设置布局参数
                                    it.layoutParams = genLp().apply {
                                        if (holder.itemViewType == OrientationHelper.VERTICAL) {
                                            leftMargin = dp8
                                        } else {
                                            rightMargin = dp8
                                        }
                                    }
                                    this.addView(it)
                                }
                            }

                        }
                        // 设置 item 的点击和长按事件
                        holder.getView<View>(R.id.card_movie_item)?.let {
                            it.setOnClickListener {
                                MovieDetailActivity.start(viewContext, item)
                            }
                            it.setOnLongClickListener {
                                val menus = mutableListOf("收藏")
                                if (CollectModel.has(item.convertDBItem())) menus.remove("收藏") else menus.add("取消收藏")
                                MaterialDialog.Builder(viewContext).title(item.title)
                                    .items(menus)
                                    .itemsCallback { _, _, which, _ ->
                                        if (which == 0) {
                                            //收藏
                                            CollectModel.addToCollect(item.convertDBItem()).subscribeBy(onError = {
                                                this@AbsMovieListFragment.viewContext.toast("收藏失败")
                                            }, onComplete = {
                                                this@AbsMovieListFragment.viewContext.toast("收藏成功")
                                            })
                                        } else {
                                            //取消收藏
                                            CollectModel.removeCollect(item.convertDBItem()).subscribeBy(onError = {
                                                this@AbsMovieListFragment.viewContext.toast("取消收藏失败")
                                            }, onComplete = {
                                                this@AbsMovieListFragment.viewContext.toast("取消收藏成功")
                                            })
                                        }
                                    }.show()
                                true
                            }
                        }

                    }
                }
            }
        }
    }

    override fun onRecyclerViewCreated() {
        super.onRecyclerViewCreated()
        //根据页面模式设置不同的 LayoutManager
        when (pageMode) {
            AppConfiguration.PageMode.Page -> {
                //瀑布流
                val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
                mRecyclerView.layoutManager = layoutManager
                mRecyclerView.addOnScrollListener(object : EndlessStaggeredGridRecyclerOnScrollListener(layoutManager) {
                    override fun onLoadMore(currentPage: Int) {
                        mBasePresenter?.onLoadMore()
                    }
                })
            }
            AppConfiguration.PageMode.Normal -> {
                //线性布局
                val layoutManager = LinearLayoutManager(viewContext)
                mRecyclerView.layoutManager = layoutManager
                mRecyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
                    override fun onLoadMore(currentPage: Int) {
                        mBasePresenter?.onLoadMore()
                    }
                })
            }
        }
    }


    override fun itemClickListener(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        //交给adapter处理
    }

    override fun itemLongClickListener(adapter: BaseQuickAdapter<*, *>, view: View, position: Int): Boolean {
        //长按事件，弹出菜单
        (adapter.data.getOrNull(position) as? ILink)?.let {
            LinkMenu.show(viewContext, it)
            return true
        }
        return super.itemLongClickListener(adapter, view, position)
    }

    companion object {
        const val MOVIE_LIST_DATA_TYPE = "MOVIE_LIST_DATA_TYPE"
    }
}

}