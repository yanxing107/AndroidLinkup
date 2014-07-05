package com.znv.linkup;

import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.widget.LinearLayout;

import com.znv.linkup.core.config.LevelCfg;
import com.znv.linkup.core.config.RankCfg;
import com.znv.linkup.view.indicator.CirclePageIndicator;
import com.znv.linkup.view.indicator.Rank;
import com.znv.linkup.view.indicator.RankAdapter;

/**
 * 关卡选择界面活动处理类
 * 
 * @author yzb
 * 
 */
public class RankActivity extends BaseActivity implements OnPageChangeListener {

    private int modeIndex = -1;
    private RankAdapter rankAdapter = null;
    private List<RankCfg> rankCfgs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_rank);

        // 初始化游戏等级
        initRank();

        new RankAsyncTask().execute();
    }

    @Override
    protected void playMusic() {

    }

    @Override
    protected void stopMusic() {

    }

    /**
     * 初始化游戏等级
     */
    private void initRank() {

        LinearLayout root = (LinearLayout) RankActivity.this.findViewById(R.id.rankBg);

        int index = getIntent().getIntExtra("modeIndex", 0);
        if (index != modeIndex) {
            rankCfgs = modeCfgs.get(index).getRankInfos();
        }
        root.setBackgroundResource(ViewSettings.GameBgImageIds[rankCfgs.get(0).getRankBackground()]);
    }

    /**
     * 切换背景图片
     */
    @Override
    public void onPageSelected(int arg0) {
        soundMgr.pageChanged();
        LinearLayout root = (LinearLayout) RankActivity.this.findViewById(R.id.rankBg);
        root.setBackgroundResource(ViewSettings.GameBgImageIds[rankCfgs.get(arg0).getRankBackground()]);
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onResume() {
        super.onResume();

        if (rankAdapter != null) {
            rankAdapter.changeRankCfgs(rankCfgs, false);
        }
    }

    /**
     * 异步加载关卡
     * 
     * @author yzb
     * 
     */
    private class RankAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int index = getIntent().getIntExtra("modeIndex", 0);
            if (index != modeIndex) {
                modeIndex = index;
                if (rankAdapters == null || rankAdapters.length <= modeIndex) {
                    // 缓存中没有时创建，主要是容错，一般用不上
                    rankAdapter = new RankAdapter(RankActivity.this, modeCfgs.get(index).getRankInfos());
                } else {
                    // 使用缓存数据
                    rankAdapter = rankAdapters[modeIndex];
                }

                rankAdapter.setLevelListener(new Rank.ISelectedLevel() {

                    @Override
                    public void onSelectedLevel(LevelCfg levelCfg) {
                        if (levelCfg.isActive()) {
                            soundMgr.select();
                            Intent intent = new Intent(RankActivity.this, GameActivity.class);
                            intent.putExtra("levelIndex", levelCfg.getLevelId());
                            startActivity(intent);
                        }
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            ViewPager pager = (ViewPager) findViewById(R.id.pager);
            pager.setAdapter(rankAdapter);

            // 设置Indicator
            CirclePageIndicator mIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
            mIndicator.setViewPager(pager);
            mIndicator.setOnPageChangeListener(RankActivity.this);

            // 更新
            rankAdapter.changeRankCfgs(rankCfgs, false);
        }
    }
}
