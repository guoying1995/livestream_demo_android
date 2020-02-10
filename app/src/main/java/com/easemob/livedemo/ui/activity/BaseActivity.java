package com.easemob.livedemo.ui.activity;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.view.View;
import com.easemob.livedemo.R;
import com.easemob.livedemo.ThreadPoolManager;
import com.easemob.livedemo.utils.StatusBarCompat;
import com.easemob.livedemo.utils.Utils;

/**
 * Created by wei on 2016/5/30.
 */
public class BaseActivity extends AppCompatActivity{
    private Toolbar mActionBarToolbar;
    private BaseActivity mContext;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }
    /**
     * getInstance the actionbar(toolbar) which view id is R.id.toolbar_actionbar
     */
    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                mActionBarToolbar.setTitleTextColor(getResources().getColor(R.color.colorTextPrimary));
                if(allowBack){
                    mActionBarToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            finish();
                        }
                    });
                }
            }
        }
        return mActionBarToolbar;
    }

    protected <T> void executeTask(ThreadPoolManager.Task<T> task){
        ThreadPoolManager.getInstance().executeTask(task);
    }

    protected void executeRunnable(Runnable runnable){
        ThreadPoolManager.getInstance().executeRunnable(runnable);
    }

    protected void showToast(final String toastContent){
        Utils.showToast(this, toastContent);
    }

    protected void showLongToast(final String toastContent){
        Utils.showLongToast(this, toastContent);
    }

    protected boolean allowBack = true;

    private ProgressDialog progressDialog;
    protected void showProgressDialog(String message){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage(message);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }
    protected void dismissProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    /**
     * 通用页面，需要设置沉浸式
     * @param fitSystemForTheme
     */
    public void setFitSystemForTheme(boolean fitSystemForTheme) {
        setFitSystemForTheme(fitSystemForTheme, R.color.white);
        setStatusBarTextColor(false);
    }

    /**
     * 设置是否是沉浸式，并可设置状态栏颜色
     * @param fitSystemForTheme
     * @param colorId 颜色资源路径
     */
    public void setFitSystemForTheme(boolean fitSystemForTheme, @ColorRes int colorId) {
        StatusBarCompat.setFitSystemForTheme(this, fitSystemForTheme, ContextCompat.getColor(mContext, colorId));
    }

    /**
     * 修改状态栏文字颜色
     * @param isLight 是否是浅色字体
     */
    public void setStatusBarTextColor(boolean isLight) {
        StatusBarCompat.setLightStatusBar(mContext, !isLight);
    }

}
