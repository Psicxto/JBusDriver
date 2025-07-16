package me.jbusdriver.ui.holder

import android.content.Context
import android.graphics.Paint
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.layout_detail_header.view.*
import me.jbusdriver.R
import me.jbusdriver.base.inflate
import me.jbusdriver.mvp.bean.Header
import me.jbusdriver.mvp.bean.convertDBItem
import me.jbusdriver.mvp.bean.des
import me.jbusdriver.mvp.model.CollectModel
import me.jbusdriver.ui.activity.MovieListActivity
import me.jbusdriver.ui.data.AppConfiguration
import me.jbusdriver.ui.data.contextMenu.LinkMenu

/**
 * 用于详情页面展示头部信息的 Holder，例如演员、类别、系列等。
 * 它内部使用一个 RecyclerView 来展示一个 Header 列表。
 */
class HeaderHolder(context: Context) : BaseHolder(context) {

    // 懒加载并初始化 Holder 的视图
    val view by lazy {
        weakRef.get()?.let {
            // 使用 context 填充布局
            it.inflate(R.layout.layout_detail_header).apply {
                // 设置 RecyclerView 的布局管理器为线性布局
                rv_recycle_header.layoutManager = LinearLayoutManager(this.context)
                // 将适配器绑定到 RecyclerView
                headAdapter.bindToRecyclerView(rv_recycle_header)
                // 启用嵌套滚动，使其在 CoordinatorLayout 等布局中滚动更流畅
                rv_recycle_header.isNestedScrollingEnabled = true
            }
        } ?: error("context ref is finish") // 如果 context 已被回收，则抛出异常
    }

    // 内部 RecyclerView 的适配器
    private val headAdapter = object : BaseQuickAdapter<Header, BaseViewHolder>(R.layout.layout_header_item) {
        override fun convert(holder: BaseViewHolder, item: Header) {
            // 获取头信息的值对应的 TextView
            holder.getView<TextView>(R.id.tv_head_value)?.apply {
                // 如果 Header 带有链接
                if (!TextUtils.isEmpty(item.link)) {
                    // 设置文本颜色为主题色，并添加下划线
                    setTextColor(ResourcesCompat.getColor(this@apply.resources, R.color.colorPrimaryDark, null))
                    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG

                    // 设置点击监听，点击后跳转到电影列表页面
                    setOnClickListener {
                        MovieListActivity.start(it.context, item)
                    }

                } else {
                    // 如果没有链接，设置为普通文本样式
                    setTextColor(ResourcesCompat.getColor(this@apply.resources, R.color.secondText, null))
                    paintFlags = 0
                    setOnClickListener(null)
                }
                // 设置长按监听
                setOnLongClickListener {
                    // 根据链接是否存在、是否已收藏等条件，过滤上下文菜单的操作
                    val action = LinkMenu.linkActions.filter {
                        when {
                            TextUtils.isEmpty(item.link) -> it.key == "复制" // 没有链接只显示复制
                            CollectModel.has(item.convertDBItem()) -> it.key != "收藏" // 已收藏则不显示收藏
                            else -> it.key != "取消收藏" // 未收藏则不显示取消收藏
                        }
                    }.toMutableMap()

                    // 如果开启了分类功能，将“收藏”替换为“收藏到分类...”
                    if (AppConfiguration.enableCategory) {
                        val ac = action.remove("收藏")
                        if (ac != null) {
                            action["收藏到分类..."] = ac
                        }
                    }

                    // 使用 MaterialDialog 显示上下文菜单
                    MaterialDialog.Builder(holder.itemView.context).title(item.name).content(item.des)
                        .items(action.keys)
                        .itemsCallback { _, _, _, text ->
                            // 执行选中操作
                            action[text]?.invoke(item)
                        }.show()
                    return@setOnLongClickListener true

                }
            }
            // 设置头信息的名称和值
            holder.setText(R.id.tv_head_name, item.name)
                .setText(R.id.tv_head_value, item.value)
        }
    }

    /**
     * 初始化 Holder，加载头部数据。
     * @param data Header 数据列表
     */
    fun init(data: List<Header>) {
        // 如果数据为空，显示提示信息
        if (data.isEmpty()) view.tv_movie_head_none_tip.visibility = View.VISIBLE
        else {
            // 否则，加载数据到适配器
            headAdapter.setNewData(data)
        }
    }

}