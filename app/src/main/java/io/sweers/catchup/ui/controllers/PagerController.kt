/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.controllers

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.design.widget.AppBarLayout
import android.support.design.widget.TabLayout
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import butterknife.BindView
import butterknife.Unbinder
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.jakewharton.processphoenix.ProcessPhoenix
import com.jakewharton.rxbinding2.support.design.widget.RxAppBarLayout
import com.uber.autodispose.kotlin.autoDisposeWith
import io.sweers.catchup.P
import io.sweers.catchup.R
import io.sweers.catchup.rx.PredicateConsumer
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.activity.SettingsActivity
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.util.clearLightStatusBar
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.resolveAttribute
import io.sweers.catchup.util.setLightStatusBar
import java.util.Arrays

class PagerController : ButterKnifeController {

  companion object {

    private const val PAGE_TAG = "PagerController.pageTag"
    private val PAGE_DATA = arrayOf(
        intArrayOf(R.drawable.logo_hn, R.string.hacker_news, R.color.hackerNewsAccent),
        intArrayOf(R.drawable.logo_reddit, R.string.reddit, R.color.redditAccent),
        intArrayOf(R.drawable.logo_medium, R.string.medium, R.color.mediumAccent),
        intArrayOf(R.drawable.logo_ph, R.string.product_hunt, R.color.productHuntAccent),
        intArrayOf(R.drawable.logo_sd, R.string.slashdot, R.color.slashdotAccent),
        intArrayOf(R.drawable.logo_dn, R.string.designer_news, R.color.designerNewsAccent),
        intArrayOf(R.drawable.logo_dribbble, R.string.dribbble, R.color.dribbbleAccent),
        intArrayOf(R.drawable.logo_github, R.string.github, R.color.githubAccent))
  }

  private val resolvedColorCache = IntArray(PAGE_DATA.size)
  private val argbEvaluator = ArgbEvaluator()

  @BindView(R.id.tab_layout) lateinit var tabLayout: TabLayout
  @BindView(R.id.view_pager) lateinit var viewPager: ViewPager
  @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
  @BindView(R.id.appbarlayout) lateinit var appBarLayout: AppBarLayout
  private var statusBarColorAnimator: ValueAnimator? = null
  private var colorNavBar = false
  private var tabLayoutIsPinned = false
  private var pagerAdapter: RouterPagerAdapter

  constructor() : super()

  constructor(args: Bundle) : super(args)

  init {
    pagerAdapter = object : RouterPagerAdapter(this) {
      override fun configureRouter(router: Router, position: Int) {
        if (!router.hasRootController()) {
          val page: Controller = when (position) {
            0 -> HackerNewsController()
            1 -> RedditController()
            2 -> MediumController()
            3 -> ProductHuntController()
            4 -> SlashdotController()
            5 -> DesignerNewsController()
            6 -> DribbbleController()
            7 -> GitHubController()
            else -> RedditController()
          }
          router.setRoot(RouterTransaction.with(page)
              .tag(PAGE_TAG))
        }
      }

      override fun getCount(): Int {
        return PAGE_DATA.size
      }

      override fun getPageTitle(position: Int): CharSequence {
        return ""
      }
    }

    // Invalidate the color cache up front
    Arrays.fill(resolvedColorCache, R.color.no_color)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_pager, container, false)
  }

  override fun bind(view: View): Unbinder {
    return PagerController_ViewBinding(this, view)
  }

  override fun onViewBound(view: View) {
    super.onViewBound(view)

    @ColorInt val colorPrimaryDark = view.context.resolveAttribute(R.attr.colorPrimaryDark)
    val isInNightMode = view.context.isInNightMode()
    if (!isInNightMode) {
      // Start with a light status bar in normal mode
      appBarLayout.setLightStatusBar()
    }
    RxAppBarLayout.offsetChanges(appBarLayout)
        .distinctUntilChanged()
        .doOnNext(object : PredicateConsumer<Int>() {
          @Throws(Exception::class)
          override fun test(verticalOffset: Int): Boolean {
            return verticalOffset == -toolbar.height
          }

          @Throws(Exception::class)
          override fun acceptActual(value: Int) {
            statusBarColorAnimator?.cancel()
            tabLayoutIsPinned = true
            val newStatusColor = this@PagerController.getAndSaveColor(
                tabLayout.selectedTabPosition)
            statusBarColorAnimator = ValueAnimator.ofArgb(colorPrimaryDark, newStatusColor)
                .apply {
                  addUpdateListener { animation ->
                    this@PagerController.activity!!
                        .window.statusBarColor = animation.animatedValue as Int
                  }
                  duration = 200
                  interpolator = LinearOutSlowInInterpolator()
                  start()
                }
            appBarLayout.clearLightStatusBar()
          }
        })
        .doOnNext(object : PredicateConsumer<Int>() {
          @Throws(Exception::class)
          override fun acceptActual(value: Int) {
            val wasPinned = tabLayoutIsPinned
            tabLayoutIsPinned = false
            if (wasPinned) {
              statusBarColorAnimator?.cancel()
              statusBarColorAnimator = ValueAnimator.ofArgb(this@PagerController.activity!!
                  .window
                  .statusBarColor, colorPrimaryDark).apply {
                addUpdateListener { animation ->
                  this@PagerController.activity!!
                      .window.statusBarColor = animation.animatedValue as Int
                }
                duration = 200
                interpolator = DecelerateInterpolator()
                if (!isInNightMode) {
                  appBarLayout.setLightStatusBar()
                }
                start()
              }
            }
          }

          @Throws(Exception::class)
          override fun test(verticalOffset: Int): Boolean {
            return verticalOffset != -toolbar.height
          }
        })
        .autoDisposeWith(this)
        .subscribe()
    toolbar.inflateMenu(R.menu.main)
    toolbar.setOnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.toggle_daynight -> {
          P.daynightAuto.put(false)
              .commit()
          if (activity?.isInNightMode() ?: false) {
            P.daynightNight.put(false)
                .commit()
          } else {
            P.daynightNight.put(true)
                .commit()
          }
          ProcessPhoenix.triggerRebirth(activity!!)
          return@setOnMenuItemClickListener true
        }
        R.id.settings -> {
          startActivity(Intent(activity, SettingsActivity::class.java))
          return@setOnMenuItemClickListener true
        }
      }
      false
    }

    // Initial title
    toolbar.title = resources!!.getString(PAGE_DATA[0][1])

    // Set the initial color
    @ColorInt val initialColor = getAndSaveColor(0)
    tabLayout.setBackgroundColor(initialColor)
    viewPager.adapter = pagerAdapter
    tabLayout.setupWithViewPager(viewPager, false)

    // Set icons
    for (i in PAGE_DATA.indices) {
      val vals = PAGE_DATA[i]
      val d = VectorDrawableCompat.create(resources!!, vals[0], null)
      tabLayout.getTabAt(i)!!.icon = d
    }

    // Animate color changes
    // adapted from http://kubaspatny.github.io/2014/09/18/viewpager-background-transition/
    viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        val color: Int
        if (position < pagerAdapter.count - 1 && position < PAGE_DATA.size - 1) {
          color = argbEvaluator.evaluate(positionOffset,
              getAndSaveColor(position),
              getAndSaveColor(position + 1)) as Int
        } else {
          color = getAndSaveColor(PAGE_DATA.size - 1)
        }
        tabLayout.setBackgroundColor(color)
        if (tabLayoutIsPinned) {
          activity?.window?.statusBarColor = color
        }
        if (colorNavBar) {
          activity?.window?.navigationBarColor = color
        }
      }

      override fun onPageSelected(position: Int) {
        toolbar.setTitle(PAGE_DATA[position][1])
      }

      override fun onPageScrollStateChanged(state: Int) {
        // NO-OP.
      }
    })

    tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab) {

      }

      override fun onTabUnselected(tab: TabLayout.Tab) {

      }

      override fun onTabReselected(tab: TabLayout.Tab) {
        val controller = pagerAdapter.getRouter(tab.position)!!
            .getControllerWithTag(PAGE_TAG)
        if (controller is Scrollable) {
          controller.onRequestScrollToTop()
          appBarLayout.setExpanded(true, true)
        }
      }
    })
  }

  @ColorInt private fun getAndSaveColor(position: Int): Int {
    if (resolvedColorCache[position] == R.color.no_color) {
      resolvedColorCache[position] = ContextCompat.getColor(activity!!, PAGE_DATA[position][2])
    }
    return resolvedColorCache[position]
  }
}
