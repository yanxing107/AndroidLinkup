package com.znv.linkup;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.znv.linkup.core.Game;
import com.znv.linkup.core.IGameAction;
import com.znv.linkup.core.card.Piece;
import com.znv.linkup.core.card.PiecePair;
import com.znv.linkup.core.card.path.LinkInfo;
import com.znv.linkup.core.config.GameMode;
import com.znv.linkup.core.config.LevelCfg;
import com.znv.linkup.db.DbScore;
import com.znv.linkup.db.LevelScore;
import com.znv.linkup.util.AnimatorUtil;
import com.znv.linkup.util.StringUtil;
import com.znv.linkup.util.ToastUtil;
import com.znv.linkup.view.CardsView;
import com.znv.linkup.view.PathView;
import com.znv.linkup.view.dialog.FailDialog;
import com.znv.linkup.view.dialog.SuccessDialog;
import com.znv.linkup.view.dialog.TaskDialog;
import com.znv.linkup.view.dialog.TimeDialog;
import com.znv.linkup.view.handler.GameMsgHandler;

/**
 * 游戏主界面活动处理类
 */
public class GameActivity extends BaseActivity implements IGameAction {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_linkup);

        Display mDisplay = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        mDisplay.getSize(size);
        curLevelCfg = levelCfgs.get(getIntent().getIntExtra("levelIndex", 0));

        holder.tvLevel = (TextView) findViewById(R.id.tvLevel);
        holder.tvMaxScore = (TextView) findViewById(R.id.maxScore);
        holder.pbTime = (ProgressBar) findViewById(R.id.pbTime);
        holder.tsScore = (TextSwitcher) findViewById(R.id.scoreText);
        holder.flBackground = (FrameLayout) findViewById(R.id.rootFrame);
        holder.startCoin = (ImageView) findViewById(R.id.startCoin);
        holder.endCoin = (ImageView) findViewById(R.id.endCoin);
        holder.tvCombo = (TextView) findViewById(R.id.tvCombo);
        holder.tvAnimMsg = (TextView) findViewById(R.id.tvAnimMsg);
        holder.screenWidth = size.x;
        holder.screenHeight = size.y;
        holder.screenCenter = new Point((int) (size.x * 0.5), (int) (size.y * 0.5));
        holder.tools = findViewById(R.id.tools);
        holder.btnPrompt = (Button) findViewById(R.id.prompt);
        holder.btnRefresh = (Button) findViewById(R.id.refresh);
        holder.btnAddTime = (Button) findViewById(R.id.addTime);
        holder.tsScore.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                TextView tv = new TextView(GameActivity.this);
                tv.setTextSize(30);
                tv.setTextColor(0xffff6347);
                tv.setGravity(Gravity.CENTER);
                return tv;
            }
        });
        if (curLevelCfg.getLevelMode() == GameMode.Level || curLevelCfg.getLevelMode() == GameMode.Task) {
            holder.tsScore.setInAnimation(this, R.anim.slide_in_up);
            holder.tsScore.setOutAnimation(this, R.anim.slide_out_up);
        }

        pathView = new PathView(this);
        holder.flBackground.addView(pathView, -1, -1);
        cardsView = (CardsView) findViewById(R.id.cardsView);
        failDialog = new FailDialog(this);
        successDialog = new SuccessDialog(this);
        timeDialog = new TimeDialog(this);
        taskDialog = new TaskDialog(this);

        start();
    }

    /**
     * 随机播放背景音乐
     */
    @Override
    protected void playMusic() {
        if (musicMgr != null) {
            int bgmusic = (int) (Math.random() * ViewSettings.BgMusics.length);
            musicMgr.setBgMusicRes(ViewSettings.BgMusics[bgmusic]);
            musicMgr.play();
        }
    }

    /**
     * 开始游戏
     */
    public void start() {
        if (game != null) {
            game.finish();
        }

        // 根据屏幕动态调整卡片大小及位置
        adjustLevelCfg(curLevelCfg);

        holder.btnPrompt.setText(String.valueOf(LevelCfg.globalCfg.getPromptNum()));
        holder.btnRefresh.setText(String.valueOf(LevelCfg.globalCfg.getRefreshNum()));
        holder.btnAddTime.setText(String.valueOf(LevelCfg.globalCfg.getAddTimeNum()));
        holder.tvLevel.setText(curLevelCfg.getRankName() + "-" + curLevelCfg.getLevelName());
        holder.pbTime.setMax(curLevelCfg.getLevelTime());
        holder.tvMaxScore.setText(getString(R.string.game_level_record) + String.valueOf(curLevelCfg.getMaxScore()));
        holder.flBackground.setBackgroundResource(ViewSettings.RankBgImageIds[curLevelCfg.getLevelBackground()]);
        if (curLevelCfg.getLevelMode() == GameMode.Level) {
            holder.tsScore.setText("0");
        } else if (curLevelCfg.getLevelMode() == GameMode.Time) {
            holder.pbTime.setVisibility(View.GONE);
            holder.btnAddTime.setVisibility(View.GONE);
            holder.tsScore.setText("00:00");
            if (curLevelCfg.getMaxScore() == 0) {
                holder.tvMaxScore.setText(getString(R.string.game_level_norecord));
            } else {
                holder.tvMaxScore.setText(getString(R.string.game_level_record) + StringUtil.secondToString(curLevelCfg.getMaxScore()));
            }
        } else if (curLevelCfg.getLevelMode() == GameMode.Task) {
            holder.pbTime.setVisibility(View.GONE);
            holder.btnAddTime.setVisibility(View.GONE);
            holder.tsScore.setText("0");
            holder.tvMaxScore.setText(getString(R.string.game_level_task) + curLevelCfg.getTask());
        }
        // 工具条动画
        AnimatorUtil.animTranslate(holder.tools, holder.tools.getX(), holder.tools.getX(), holder.tools.getY() + 100, holder.tools.getY());

        game = new Game(curLevelCfg, this);
        cardsView.setGame(game);

        // 播放声音和动画
        showAnimMsg(getString(R.string.game_ready_go), 30);
        soundMgr.readyGo();

        game.start();
    }

    /**
     * 根据屏幕动态调整卡片大小及位置
     * 
     * @param levelCfg
     *            关卡配置信息
     */
    private void adjustLevelCfg(LevelCfg levelCfg) {
        // 上下保留一部分空间
        int heightReserve = 60;
        int levelWidth = holder.screenWidth / (levelCfg.getXSize() - 1);
        int levelHeight = (holder.screenHeight - heightReserve) / levelCfg.getYSize();
        // 像素调整为8的倍数
        int newSize = (Math.min(levelWidth, levelHeight) / 8) * 8;
        int beginX = (holder.screenWidth - newSize * levelCfg.getXSize()) / 2;
        int beginY = (holder.screenHeight - newSize * levelCfg.getYSize()) / 2;
        levelCfg.setPieceWidth(newSize);
        levelCfg.setPieceHeight(newSize);
        levelCfg.setBeginImageX(beginX);
        levelCfg.setBeginImageY(beginY);
    }

    /**
     * 下一关
     */
    public void next() {
        game.finish();

        int nextLevelId = curLevelCfg.getLevelId() + 1;
        curLevelCfg = levelCfgs.get(nextLevelId);
        if (curLevelCfg != null) {
            start();
        } else {
            // 通过所有关卡
            showAnimMsg(getString(R.string.game_success), 30);
            onBackPressed();
        }
    }

    /**
     * 游戏失败时的处理
     */
    @Override
    public void onGameFail() {
        handler.sendEmptyMessage(ViewSettings.FailMessage);
    }

    /**
     * 游戏胜利时的处理
     */
    @Override
    public void onGameWin() {
        // 激活下一关
        int nextLevelId = curLevelCfg.getLevelId() + 1;
        LevelCfg nextCfg = levelCfgs.get(nextLevelId);
        if (nextCfg != null) {
            nextCfg.setActive(true);

            LevelScore nls = new LevelScore(nextLevelId);
            nls.setIsActive(1);
            DbScore.updateActive(nls);
        }

        int isRecord = 0;
        int stars = 0;
        if (curLevelCfg.getLevelMode() == GameMode.Level) {
            // 保存记录和星级
            if (game.getTotalScore() > curLevelCfg.getMaxScore()) {
                // 新纪录
                LevelScore cls = new LevelScore(curLevelCfg.getLevelId());
                cls.setMaxScore(game.getTotalScore());
                cls.setStar(curLevelCfg.getStar(game.getTotalScore()));
                DbScore.updateScore(cls);

                // 更新缓存
                curLevelCfg.setLevelStar(cls.getStar());
                curLevelCfg.setMaxScore(cls.getMaxScore());
                isRecord = 1;
                stars = cls.getStar();
            }
        } else if (curLevelCfg.getLevelMode() == GameMode.Time) {
            if (curLevelCfg.getMaxScore() == 0 || game.getGameTime() < curLevelCfg.getMaxScore()) {
                // 更新时间记录
                LevelScore cls = new LevelScore(curLevelCfg.getLevelId());
                cls.setMaxScore(game.getGameTime());
                DbScore.updateScore(cls);

                // 更新缓存
                curLevelCfg.setMaxScore(cls.getMaxScore());
                isRecord = 1;
            }
        } else if (curLevelCfg.getLevelMode() == GameMode.Task) {
            if (game.getTotalScore() >= curLevelCfg.getTask() && game.getTotalScore() > curLevelCfg.getMaxScore()) {
                // 更新任务完成记录
                LevelScore cls = new LevelScore(curLevelCfg.getLevelId());
                cls.setMaxScore(game.getTotalScore());
                DbScore.updateScore(cls);

                // 更新缓存
                curLevelCfg.setMaxScore(cls.getMaxScore());
                isRecord = 1;
            }
        }

        handler.sendMessage(handler.obtainMessage(ViewSettings.WinMessage, isRecord, stars));
    }

    /**
     * 游戏提示
     */
    public void onPrompt(PiecePair pair) {
        if (pair == null) {
            // 没有可消除时给出提示
            Toast toast = ToastUtil.getToast(this, R.string.game_prompt_none);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 160);
            toast.show();
        } else {
            cardsView.prompt(pair);
            // 减少提示一次
            LevelCfg.globalCfg.setPromptNum(LevelCfg.globalCfg.getPromptNum() - 1);
            setGlobalCfg();
            handler.sendEmptyMessage(ViewSettings.PromptMessage);
            soundMgr.prompt();
        }
    }

    /**
     * 取消游戏提示
     */
    public void onUnPrompt(PiecePair pair) {
        if (pair != null) {
            cardsView.unPrompt(pair);
        }
    }

    /**
     * 连击时的处理
     */
    @Override
    public void onCombo() {
        String msg = String.valueOf(game.getGameCombo());
        holder.tvCombo.setText(msg);
        int msgWidth = msg.length() * holder.screenWidth / 50 + getResources().getDrawable(R.drawable.combo).getMinimumWidth() / 2;
        Point startPoint = new Point(holder.screenCenter.x - msgWidth, holder.screenCenter.y);
        Point endPoint = new Point(startPoint.x, startPoint.y - 50);
        animTranslate(holder.tvCombo, startPoint, endPoint, 1500);

        soundMgr.combo();
    }

    /**
     * 屏幕中心显示文字动画
     * 
     * @param msg
     *            文字信息
     * @param textSize
     *            字体大小
     */
    private void showAnimMsg(String msg, int textSize) {
        setAnimMsgStyle(msg, textSize);
        int msgWidth = msg.length() * holder.screenWidth / 50;
        Point startPoint = new Point(holder.screenCenter.x - msgWidth, holder.screenCenter.y);
        Point endPoint = new Point(startPoint.x, startPoint.y - 50);
        animTranslate(holder.tvAnimMsg, startPoint, endPoint, 1500);
    }

    /**
     * 显示增加分数动画
     * 
     * @param msg
     *            文字信息
     * @param textSize
     *            字体大小
     */
    private void showAddScore(String msg, int textSize) {
        setAnimMsgStyle(msg, textSize);
        int msgWidth = msg.length() * holder.screenWidth / 60;
        Point startPoint = new Point(holder.screenCenter.x - msgWidth, (int) holder.tsScore.getY() + 100);
        Point endPoint = new Point(startPoint.x, startPoint.y - 40);
        animTranslate(holder.tvAnimMsg, startPoint, endPoint, 500);
    }

    /**
     * 设置动画文字的样式
     * 
     * @param msg
     *            文字信息
     * @param textSize
     *            字体大小
     */
    private void setAnimMsgStyle(String msg, int textSize) {
        holder.tvAnimMsg.setText(msg);
        holder.tvAnimMsg.setTextSize(textSize);
    }

    /**
     * 获取游戏得分
     * 
     * @param isSuccess
     *            是否胜利
     * @return 游戏总得分
     */
    public int getGameScore(boolean isSuccess) {
        if (isSuccess) {
            return game.getGameScore() + game.getRewardScore();
        } else {
            return game.getGameScore();
        }
    }

    public int getGameTime() {
        return game.getGameTime();
    }

    /**
     * 重排时的处理
     */
    @Override
    public void onRefresh() {
        cardsView.createCards(false);

        soundMgr.refresh();
        // 减少重排一次
        LevelCfg.globalCfg.setRefreshNum(LevelCfg.globalCfg.getRefreshNum() - 1);
        setGlobalCfg();
        handler.sendEmptyMessage(ViewSettings.RefreshMessage);
    }

    /**
     * 选中时的处理
     */
    public void onCheck(Piece piece) {
        if (piece != null) {
            cardsView.check(piece);
            soundMgr.select();
        }
    }

    /**
     * 取消选中时的处理
     */
    public void onUnCheck(Piece piece) {
        if (piece != null) {
            cardsView.unCheck(piece);
        }
    }

    /**
     * 变换时的处理
     */
    @Override
    public void onTranslate() {
        cardsView.createCards(false);

        soundMgr.translate();
    }

    /**
     * 处理游戏消除路径
     */
    @Override
    public void onLinkPath(LinkInfo linkInfo) {
        pathView.showLines(linkInfo.getLinkPieces());

        if (curLevelCfg.getLevelMode() == GameMode.Level || curLevelCfg.getLevelMode() == GameMode.Task) {
            // 收集金币的动画
            Point startPoint = linkInfo.getLinkPieces().get(0).getCenter();
            Point endPoint = new Point((int) (holder.tsScore.getLeft() + holder.tsScore.getWidth() * 0.5),
                    (int) (holder.tsScore.getTop() + holder.tsScore.getHeight() * 0.5));
            animTranslate(holder.startCoin, startPoint, endPoint, 400);
            startPoint = linkInfo.getLinkPieces().get(linkInfo.getLinkPieces().size() - 1).getCenter();
            animTranslate(holder.endCoin, startPoint, endPoint, 400);
        }

        soundMgr.erase();
    }

    /**
     * 视图移动的动画
     * 
     * @param view
     *            要移动的视图
     * @param start
     *            起点
     * @param end
     *            终点
     * @param duration
     *            动画时长
     */
    private void animTranslate(View view, Point start, Point end, int duration) {
        AnimatorUtil.animTranslate(view, start.x, end.x, start.y, end.y, duration, true);
    }

    /**
     * 时间改变时的处理
     */
    @Override
    public void onTimeChanged(int time) {
        handler.sendMessage(handler.obtainMessage(ViewSettings.TimeMessage, time, 0));
    }

    /**
     * 分数改变时的处理
     */
    @Override
    public void onScoreChanged(int score) {
        handler.sendMessage(handler.obtainMessage(ViewSettings.ScoreMessage, score, 0));
    }

    /**
     * 暂停游戏时的处理
     */
    @Override
    public void onGamePause() {

    }

    /**
     * 恢复游戏时的处理
     */
    @Override
    public void onGameResume() {

    }

    /**
     * 处理back键
     */
    @Override
    public void onBackPressed() {
        game.finish();
        super.onBackPressed();
    }

    /**
     * 暂停游戏
     */
    @Override
    protected void onPause() {
        game.pause();
        super.onPause();
    }

    /**
     * 恢复游戏
     */
    @Override
    protected void onResume() {
        game.resume();
        super.onResume();
    }

    /**
     * 提示按钮按下时的处理
     * 
     * @param v
     */
    public void prompt(View v) {
        if (LevelCfg.globalCfg.getPromptNum() > 0) {
            game.prompt();
        }
    }

    /**
     * 重排按钮按下时的处理
     * 
     * @param v
     */
    public void refresh(View v) {
        if (LevelCfg.globalCfg.getRefreshNum() > 0) {
            game.refresh();
        }
    }

    /**
     * 重排按钮按下时的处理
     * 
     * @param v
     */
    public void addTime(View v) {
        if (LevelCfg.globalCfg.getAddTimeNum() > 0) {
            // 减少加时间一次
            LevelCfg.globalCfg.setAddTimeNum(LevelCfg.globalCfg.getAddTimeNum() - 1);
            setGlobalCfg();
            game.addGameTime(ViewSettings.AddTimeSeconds);

            showAddTime();
        }
    }

    /**
     * 重排按钮按下时的处理
     * 
     * @param v
     */
    public void stop(View v) {
        game.stop();
    }

    /**
     * Handler的消息处理--显示时间
     * 
     * @param seconds
     *            显示的时间秒数
     */
    public void showTime(int seconds) {
        if (curLevelCfg.getLevelMode() == GameMode.Level) {
            holder.pbTime.setProgress(seconds);
        } else if (curLevelCfg.getLevelMode() == GameMode.Time) {
            holder.tsScore.setText(StringUtil.secondToString(seconds));
        }
    }

    /**
     * Handler的消息处理--显示分数
     */
    public void showScore(int score) {
        if (curLevelCfg.getLevelMode() == GameMode.Level || curLevelCfg.getLevelMode() == GameMode.Task) {
            // 显示增加分数动画
            int lastScore = Integer.parseInt((String) ((TextView) holder.tsScore.getCurrentView()).getText());
            String msg = "+" + String.valueOf(score - lastScore);
            showAddScore(msg, 20);

            holder.tsScore.setText(String.valueOf(score));
        }
    }

    /**
     * Handler的消息处理--显示失败
     */
    public void showFail() {
        if (curLevelCfg.getLevelMode() == GameMode.Level) {
            failDialog.showDialog();
            soundMgr.fail();
        }
    }

    /**
     * Handler的消息处理--显示胜利
     */
    public void showSuccess(int isRecord, int stars) {
        if (curLevelCfg.getLevelMode() == GameMode.Level) {
            successDialog.showDialog(isRecord == 1, stars);
        } else if (curLevelCfg.getLevelMode() == GameMode.Time) {
            timeDialog.showDialog(isRecord == 1);
        } else if (curLevelCfg.getLevelMode() == GameMode.Task) {
            taskDialog.showDialog(isRecord == 1);
        }
        soundMgr.win();
    }

    /**
     * Handler的消息处理--显示提示数
     */
    public void showPrompt() {
        holder.btnPrompt.setText(String.valueOf(LevelCfg.globalCfg.getPromptNum()));
    }

    /**
     * Handler的消息处理--显示重排数
     */
    public void showRefresh() {
        holder.btnRefresh.setText(String.valueOf(LevelCfg.globalCfg.getRefreshNum()));
    }

    /**
     * Handler的消息处理--显示加时间数
     */
    public void showAddTime() {
        if (curLevelCfg.getLevelMode() == GameMode.Level) {
            holder.btnAddTime.setText(String.valueOf(LevelCfg.globalCfg.getAddTimeNum()));
        }
    }

    /**
     * 获取当前游戏关卡配置
     * 
     * @return 当前游戏关卡配置
     */
    public LevelCfg getLevelCfg() {
        return curLevelCfg;
    }

    private Game game;
    private CardsView cardsView;
    private FailDialog failDialog;
    private SuccessDialog successDialog;
    private TimeDialog timeDialog;
    private TaskDialog taskDialog;
    private LevelCfg curLevelCfg = null;
    private LevelHolder holder = new LevelHolder();
    private Handler handler = new GameMsgHandler(this);
    private PathView pathView = null;

    /**
     * 界面信息缓存类
     * 
     * @author yzb
     * 
     */
    class LevelHolder {
        TextView tvLevel;
        TextView tvMaxScore;
        ProgressBar pbTime;
        TextSwitcher tsScore;
        FrameLayout flBackground;
        ImageView startCoin;
        ImageView endCoin;
        TextView tvCombo;
        TextView tvAnimMsg;
        int screenWidth;
        int screenHeight;
        Point screenCenter;
        View tools;
        Button btnPrompt;
        Button btnRefresh;
        Button btnAddTime;
    }

}
