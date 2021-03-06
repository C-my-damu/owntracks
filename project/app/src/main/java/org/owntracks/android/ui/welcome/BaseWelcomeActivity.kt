package org.owntracks.android.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import org.owntracks.android.R
import org.owntracks.android.databinding.UiWelcomeBinding
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.navigator.Navigator
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.welcome.finish.FinishFragment
import org.owntracks.android.ui.welcome.intro.IntroFragment
import org.owntracks.android.ui.welcome.version.VersionFragment
import timber.log.Timber
import javax.inject.Inject

abstract class BaseWelcomeActivity : BaseActivity<UiWelcomeBinding?, WelcomeViewModel?>(),
    WelcomeMvvm.View {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var requirementsChecker: RequirementsChecker

    @Inject
    lateinit var introFragment: IntroFragment

    @Inject
    lateinit var versionFragment: VersionFragment

    @Inject
    lateinit var finishFragment: FinishFragment

    private var welcomeAdapter: WelcomeAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requirementsChecker.areRequirementsMet()) {
            navigator.startActivity(MapActivity::class.java, null, Intent.FLAG_ACTIVITY_NEW_TASK)
            finish()
            return
        }
        bindAndAttachContentView(R.layout.ui_welcome, savedInstanceState)
        setHasEventBus(false)

        welcomeAdapter = WelcomeAdapter(this, requirementsChecker).apply { addFragmentsToAdapter(this) }

        binding!!.viewPager.adapter = welcomeAdapter
        binding!!.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding!!.vm!!.currentFragmentPosition.value = position
            }
        })

        binding!!.vm!!.currentFragmentPosition.observe({ this.lifecycle }, { position: Int ->
            binding!!.viewPager.currentItem = position
            setPagerIndicator(position)
        })

        Timber.v("pager setup with %s fragments", welcomeAdapter!!.itemCount)
        buildPagerIndicator()
    }

    abstract fun addFragmentsToAdapter(welcomeAdapter: WelcomeAdapter)

    override fun setPagerIndicator(position: Int) {
        if (position < welcomeAdapter!!.itemCount) {
            for (i in 0 until welcomeAdapter!!.itemCount) {
                val circle = binding!!.circles.getChildAt(i) as ImageView
                if (i == position) {
                    circle.alpha = 1f
                } else {
                    circle.alpha = 0.5f
                }
            }
        }
    }


    private fun buildPagerIndicator() {
        val scale = resources.displayMetrics.density
        val padding = (5 * scale + 0.5f).toInt()
        for (i in 0 until welcomeAdapter!!.itemCount) {
            val circle = ImageView(this)
            circle.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_fiber_manual_record_24
                )
            )
            circle.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            circle.adjustViewBounds = true
            circle.setPadding(padding, 0, padding, 0)
            binding!!.circles.addView(circle)
        }
        setPagerIndicator(0)
    }

    override fun onBackPressed() {
        if (binding!!.viewPager.currentItem == 0) {
            finish()
        } else {
            binding!!.vm!!.moveBack()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.onResume()
    }
}