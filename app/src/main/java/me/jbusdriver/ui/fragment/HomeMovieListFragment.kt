package me.jbusdriver.ui.fragment

import android.os.Bundle
import me.jbusdriver.http.JAVBusService
import me.jbusdriver.mvp.LinkListContract
import me.jbusdriver.mvp.bean.PageLink
import me.jbusdriver.mvp.presenter.HomeMovieListPresenterImpl
import me.jbusdriver.ui.data.enums.DataSourceType


/**
 * 首页电影列表 Fragment，用于展示不同数据源（如 JAVBus, JavLibrary）的电影列表。
 * 继承自 AbsMovieListFragment，实现了通用的列表展示逻辑。
 */
class HomeMovieListFragment : AbsMovieListFragment(), LinkListContract.LinkListView {
    /**
     * 创建并返回一个 HomeMovieListPresenterImpl 实例。
     * Presenter 负责处理业务逻辑，例如从网络加载数据。
     * @return HomeMovieListPresenterImpl 实例
     */
    override fun createPresenter() = HomeMovieListPresenterImpl(
        type, //数据源类型，决定从哪个网站抓取数据
        // PageLink 携带了分页信息和基础 URL，这里 PageLink 本身作用不大，主要是为了传递一个默认的 URL
        PageLink(1, "", JAVBusService.defaultFastUrl) /*PageLink没什么用,默认设置JAVBusService.defaultFastUrl就可以*/
    )

    /*================================================*/
    companion object {
        /**
         * 创建 HomeMovieListFragment 的新实例。
         * 使用工厂方法模式，通过 Bundle 传递必要的参数。
         * @param type 数据源类型 (DataSourceType)
         * @return HomeMovieListFragment 的一个新实例
         */
        fun newInstance(type: DataSourceType) = HomeMovieListFragment().apply {
            arguments = Bundle().apply { putSerializable(MOVIE_LIST_DATA_TYPE, type) }
        }
    }

}