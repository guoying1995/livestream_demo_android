package com.easemob.livedemo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.easemob.livedemo.R;
import com.easemob.livedemo.data.model.LiveSettings;
import com.easemob.livedemo.data.restapi.ApiManager;
import com.easemob.livedemo.data.restapi.LiveException;
import com.easemob.livedemo.utils.Log2FileUtil;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.controller.EaseUI;
import com.ucloud.common.util.DeviceUtils;
import com.ucloud.live.UEasyStreaming;
import com.ucloud.live.UStreamingProfile;
import com.ucloud.live.widget.UAspectFrameLayout;

public class LiveAnchorActivity extends LiveBaseActivity
        implements UEasyStreaming.UStreamingStateListener {
    private static final String TAG = LiveAnchorActivity.class.getSimpleName();
    @BindView(R.id.container) UAspectFrameLayout mPreviewContainer;
    @BindView(R.id.countdown_txtv) TextView countdownView;
    @BindView(R.id.finish_frame) ViewStub liveEndLayout;
    @BindView(R.id.cover_image) ImageView coverImage;
    //@BindView(R.id.img_bt_switch_light) ImageButton lightSwitch;
    //@BindView(R.id.img_bt_switch_voice) ImageButton voiceSwitch;

    protected UEasyStreaming mEasyStreaming;
    protected String rtmpPushStreamDomain = "publish3.cdn.ucloud.com.cn";
    public static final int MSG_UPDATE_COUNTDOWN = 1;

    public static final int COUNTDOWN_DELAY = 1000;

    public static final int COUNTDOWN_START_INDEX = 3;
    public static final int COUNTDOWN_END_INDEX = 1;
    protected boolean isShutDownCountdown = false;
    private LiveSettings mSettings;
    private UStreamingProfile mStreamingProfile;
    UEasyStreaming.UEncodingType encodingType;

    boolean isStarted;

    private Handler handler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_COUNTDOWN:
                    handleUpdateCountdown(msg.arg1);
                    break;
            }
        }
    };

    //203138620012364216
    @Override protected void onActivityCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_live_anchor);
        ButterKnife.bind(this);
        initEnv();

        startLive();
    }

    public void initEnv() {
        mSettings = new LiveSettings(this);
        if (mSettings.isOpenLogRecoder()) {
            Log2FileUtil.getInstance().setLogCacheDir(mSettings.getLogCacheDir());
            Log2FileUtil.getInstance().startLog(); //
        }

        String pushUrl = liveRoom.getLivePushUrl();
        pushUrl = pushUrl.substring(pushUrl.indexOf("://") + 3);
        String pushDomain = pushUrl.substring(0, pushUrl.indexOf("/"));
        String pushId = pushUrl.substring(pushUrl.indexOf("/") + 1);

        //        UStreamingProfile.Stream stream = new UStreamingProfile.Stream(rtmpPushStreamDomain, "ucloud/" + mSettings.getPusblishStreamId());
        //hardcode
        UStreamingProfile.Stream stream = new UStreamingProfile.Stream(pushDomain, pushId);

        mStreamingProfile = new UStreamingProfile.Builder().setVideoCaptureWidth(
                mSettings.getVideoCaptureWidth())
                .setVideoCaptureHeight(mSettings.getVideoCaptureHeight())
                .setVideoEncodingBitrate(
                        mSettings.getVideoEncodingBitRate()) //UStreamingProfile.VIDEO_BITRATE_NORMAL
                .setVideoEncodingFrameRate(mSettings.getVideoFrameRate())
                .setStream(stream)
                .build();

        encodingType = UEasyStreaming.UEncodingType.MEDIA_X264;
        if (DeviceUtils.hasJellyBeanMr2()) {
            encodingType = UEasyStreaming.UEncodingType.MEDIA_CODEC;
        }
        mEasyStreaming = new UEasyStreaming(this, encodingType);
        mEasyStreaming.setStreamingStateListener(this);
        mEasyStreaming.setAspectWithStreamingProfile(mPreviewContainer, mStreamingProfile);
    }

    @Override public void onStateChanged(int type, Object event) {
        switch (type) {
            case UEasyStreaming.State.MEDIA_INFO_SIGNATRUE_FAILED:
                Toast.makeText(this, event.toString(), Toast.LENGTH_LONG).show();
                break;
            case UEasyStreaming.State.START_RECORDING:
                break;
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        mEasyStreaming.stopRecording();
        super.onBackPressed();
    }

    /**
     * 切换摄像头
     */
    @OnClick(R.id.switch_camera_image) void switchCamera() {
        mEasyStreaming.switchCamera();
    }

    @OnClick(R.id.user_manager_image) void showUserList() {
        RoomUserManagementDialog managementDialog =
                (RoomUserManagementDialog) getSupportFragmentManager().findFragmentByTag(
                        "RoomUserManagementDialog");
        if (managementDialog == null) {
            managementDialog = new RoomUserManagementDialog(chatroomId);
        }
        managementDialog.show(getSupportFragmentManager(), "RoomUserManagementDialog");
    }

    /**
     * 开始直播
     */
    private void startLive() {
        //Utils.hideKeyboard(titleEdit);
        new Thread() {
            public void run() {
                int i = COUNTDOWN_START_INDEX;
                do {
                    Message msg = Message.obtain();
                    msg.what = MSG_UPDATE_COUNTDOWN;
                    msg.arg1 = i;
                    handler.sendMessage(msg);
                    i--;
                    try {
                        Thread.sleep(COUNTDOWN_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (i >= COUNTDOWN_END_INDEX);
            }
        }.start();
    }

    /**
     * 关闭直播显示直播成果
     */
    @OnClick(R.id.img_bt_close) void closeLive() {
        mEasyStreaming.stopRecording();
        if (!isStarted) {
            finish();
            return;
        }
        showConfirmCloseLayout();
    }

    //@OnClick(R.id.img_bt_switch_voice) void toggleMicrophone(){
    //  AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    //  if(audioManager.isMicrophoneMute()){
    //    audioManager.setMicrophoneMute(false);
    //    voiceSwitch.setSelected(false);
    //  }else{
    //    audioManager.setMicrophoneMute(true);
    //    voiceSwitch.setSelected(true);
    //  }
    //}

    /**
     * 打开或关闭闪关灯
     */
    //@OnClick(R.id.img_bt_switch_light) void switchLight() {
    //  boolean succeed = mEasyStreaming.toggleFlashMode();
    //  if(succeed){
    //    if(lightSwitch.isSelected()){
    //      lightSwitch.setSelected(false);
    //    }else{
    //      lightSwitch.setSelected(true);
    //    }
    //  }
    //}
    private void showConfirmCloseLayout() {
        View view = liveEndLayout.inflate();
        Button liveContinueBtn = (Button) view.findViewById(R.id.live_close_confirm);
        TextView usernameView = (TextView) view.findViewById(R.id.tv_username);
        ImageView closeConfirmView = (ImageView) view.findViewById(R.id.img_finish_confirmed);
        TextView watchedCountView = (TextView) view.findViewById(R.id.txt_watched_count);
        usernameView.setText(EMClient.getInstance().getCurrentUser());
        watchedCountView.setText(watchedCount + "人看过");

        liveContinueBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(LiveAnchorActivity.this, CreateLiveRoomActivity.class));
                finish();
            }
        });
        closeConfirmView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });
    }

    //@Override void onChatImageClick() {
    //  ConversationListFragment fragment = ConversationListFragment.newInstance(null, false);
    //  getSupportFragmentManager().beginTransaction()
    //      .replace(R.id.message_container, fragment)
    //      .commit();
    //}

    protected void setListItemClickListener() {
    }

    void handleUpdateCountdown(final int count) {
        if (countdownView != null) {
            countdownView.setVisibility(View.VISIBLE);
            countdownView.setText(String.format("%d", count));
            ScaleAnimation scaleAnimation =
                    new ScaleAnimation(1.0f, 0f, 1.0f, 0f, Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(COUNTDOWN_DELAY);
            scaleAnimation.setFillAfter(false);
            scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {
                }

                @Override public void onAnimationEnd(Animation animation) {
                    countdownView.setVisibility(View.GONE);
                    EMClient.getInstance()
                            .chatroomManager()
                            .joinChatRoom(chatroomId, new EMValueCallBack<EMChatRoom>() {
                                @Override public void onSuccess(EMChatRoom emChatRoom) {
                                    chatroom = emChatRoom;
                                    addChatRoomChangeListener();
                                    onMessageListInit();
                                }

                                @Override public void onError(int i, String s) {
                                    showToast("加入聊天室失败");
                                }
                            });

                    if (count == COUNTDOWN_END_INDEX
                            && mEasyStreaming != null
                            && !isShutDownCountdown) {
                        showToast("直播开始！");
                        mEasyStreaming.startRecording();
                        isStarted = true;
                    }
                }

                @Override public void onAnimationRepeat(Animation animation) {

                }
            });
            if (!isShutDownCountdown) {
                countdownView.startAnimation(scaleAnimation);
            } else {
                countdownView.setVisibility(View.GONE);
            }
        }
    }

    @Override protected void onPause() {
        super.onPause();
        mEasyStreaming.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        mEasyStreaming.onResume();
        if (isMessageListInited) messageView.refresh();
        EaseUI.getInstance().pushActivity(this);
        // register the event listener when enter the foreground
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
    }

    @Override public void onStop() {
        super.onStop();
        // unregister this event listener when this activity enters the
        // background
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);

        // 把此activity 从foreground activity 列表里移除
        EaseUI.getInstance().popActivity(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (mSettings.isOpenLogRecoder()) {
            Log2FileUtil.getInstance().stopLog();
        }
        mEasyStreaming.onDestroy();

        if (chatRoomChangeListener != null) {
            EMClient.getInstance()
                    .chatroomManager()
                    .removeChatRoomChangeListener(chatRoomChangeListener);
        }
        EMClient.getInstance().chatroomManager().leaveChatRoom(chatroomId);


        executeRunnable(new Runnable() {
            @Override public void run() {
                try {
                    ApiManager.get().terminateLiveRoom(liveId);
                } catch (LiveException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
