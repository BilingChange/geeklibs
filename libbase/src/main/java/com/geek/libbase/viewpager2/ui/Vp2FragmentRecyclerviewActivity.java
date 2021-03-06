package com.geek.libbase.viewpager2.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.geek.libbase.R;
import com.geek.libbase.viewpager2.adapter.ImageAdapter;
import com.geek.libbase.viewpager2.bean.DataBean;
import com.geek.libbase.viewpager2.util.TabLayoutMediator;
import com.google.android.material.tabs.TabLayout;
import com.youth.banner.Banner;
import com.youth.banner.indicator.CircleIndicator;

public class Vp2FragmentRecyclerviewActivity extends AppCompatActivity {

    ViewPager2 viewPager2;
    TabLayout mTabLayout;
    Banner mBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lunbo_vp2_fragment_recyclerview);
        viewPager2 = findViewById(R.id.vp2);
        mTabLayout = findViewById(R.id.tab_layout);
        mBanner = findViewById(R.id.banner);

        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return BannerListFragment.newInstance(position);
                } else if (position == 1) {
                    return BlankFragment.newInstance();
                } else {
                    return BannerFragment.newInstance();
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        new TabLayoutMediator(mTabLayout, viewPager2, new TabLayoutMediator.OnConfigureTabCallback() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText("页面" + position);
            }
        }).attach();


        mBanner.addBannerLifecycleObserver(this)
                .setAdapter(new ImageAdapter(DataBean.getTestData()))
                .setIntercept(false)
                .setIndicator(new CircleIndicator(this));
    }
}
