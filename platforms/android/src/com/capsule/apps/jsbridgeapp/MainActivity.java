package com.capsule.apps.jsbridgeapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.*;
import android.os.Message;
import android.support.annotation.LayoutRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import org.apache.cordova.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements Animation.AnimationListener, CordovaInterface, Handler.Callback {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String SHOW_MESSENGER_PANEL = "show_messenger_panel";
    public static final String CLOSE_MESSENGER_PANEL = "close_messenger_panel";
    public static final String MATCHED_PATTERN = "^(.*://|javascript:)[\\s\\S]*$";

    public static final int SENDING_MESSAGE_FROM_MESSENGER_MSG = 10001;
    public static final int RECEIVE_MESSAGE_FROM_HOME_MSG = 10002;

    public RelativeLayout mMainContent;
    public DrawerLayout mFrameContent;
    public RelativeLayout mMessengerContainer;
    public Toolbar mAppToolbar;

    public static MainActivity mApp;
    private final ExecutorService mThreadPool = Executors.newCachedThreadPool();

    public boolean mIsLoadedWebView = false;

    private Animation mFadeInAnim;
    private Animation mFadeOutAnim;

    private BridgeWebView mHomeWebView;
    private BridgeWebView mMessengerWebView;

    public BridgeWebViewClient mHomeWebViewClient;
    public MessengerWebViewClient mMessengerWebViewClient;
    public Toolbar mActionBarToolbar;

    public Handler mServiceHandler;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mApp = this;
        mServiceHandler = new Handler(this);

        setContentView(R.layout.activity_main_layout);
        onInitializedUI();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.ic_menu));
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getResources().getString(R.string.activity_name));
        }

        mShowMessengerIntentFilter.addAction(SHOW_MESSENGER_PANEL);
        mShowMessengerIntentFilter.addAction(CLOSE_MESSENGER_PANEL);
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mShowMessengerDialogReceiver, mShowMessengerIntentFilter);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onInitializedUI() {
        mMainContent = (RelativeLayout) findViewById(R.id.main_content);
        mFrameContent = (DrawerLayout) findViewById(R.id.frame_content);
        mMessengerContainer = (RelativeLayout) findViewById(R.id.messenger_container);
        mAppToolbar = (Toolbar) findViewById(R.id.app_toolbar);

        mHomeWebView = (BridgeWebView) findViewById(R.id.home_webview);
        mMessengerWebView = (BridgeWebView) findViewById(R.id.messenger_webview);

        mHomeWebViewClient = new BridgeWebViewClient(mHomeWebView);
        mMessengerWebViewClient = new MessengerWebViewClient(mMessengerWebView);

        mFrameContent.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mFrameContent.closeDrawers();

        mFadeInAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        mFadeOutAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);

        mHomeWebView.setBridgeWebViewClient(new BridgeWebViewClient(mHomeWebView));
        mHomeWebView.getSettings().setJavaScriptEnabled(true);
        mHomeWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        mMessengerWebView.setBridgeWebViewClient(new BridgeWebViewClient(mMessengerWebView));
        mMessengerWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mMessengerWebView.getSettings().setJavaScriptEnabled(true);
        mMessengerWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        mMessengerWebView.setDefaultHandler(new DefaultHandler());
        mHomeWebView.setDefaultHandler(new DefaultHandler());

        mHomeWebView.loadUrl("file:///android_asset/www/index.html");
        mMessengerWebView.loadUrl("file:///android_asset/www/messenger.html");

        mMessengerWebView.registerHandler("messageSender", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                Log.d(TAG, "Handler MessageSender Data From Web: " + data);
                function.onCallBack("MessengerSender feedback success from java");

                Log.d(TAG, "After Getting Message From Messenger, Then Sending Message To Home WebView");
                final Message msg = new Message();
                msg.what = SENDING_MESSAGE_FROM_MESSENGER_MSG;
                msg.obj = data;
                mServiceHandler.sendMessage(msg);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Log.d(TAG, "Opening WebView");
                mFrameContent.openDrawer(GravityCompat.START);
                break;
            }
            default: {
                Log.d(TAG, "Locking every drawer layout");
                mFrameContent.closeDrawers();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    public Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.app_toolbar);
            if (mActionBarToolbar != null) {
                mActionBarToolbar.setNavigationContentDescription(getResources().getString(R.string.activity_name));
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    public IntentFilter mShowMessengerIntentFilter = new IntentFilter(SHOW_MESSENGER_PANEL);

    public BroadcastReceiver mShowMessengerDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String url = intent.getStringExtra("url");

            if (action.equalsIgnoreCase(SHOW_MESSENGER_PANEL)) {
                if (!MainActivity.this.mIsLoadedWebView) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadMessengerWebViewContent(url);
                        }
                    });
                }
                mFrameContent.openDrawer(GravityCompat.END);
            } else if (action.equalsIgnoreCase(CLOSE_MESSENGER_PANEL)) {
                mFrameContent.closeDrawer(GravityCompat.END);
            }
        }
    };

    public void loadMessengerWebViewContent(String url) {
        mMessengerWebView.setWebViewClient(mMessengerWebViewClient);
        mMessengerWebView.setWebChromeClient(new MessengerChromeClient(mMessengerWebView));
        mMessengerWebView.loadUrl(((url.matches(MATCHED_PATTERN) ? "" : "file:///android_asset/www/") + url));
    }

    public void sendBroadcastShowOrHidePanel(Intent intent, String action, String url) {
        intent.setAction(action);
        intent.putExtra("url", url);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcastSync(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == mFadeOutAnim) {
            mMessengerWebView.startAnimation(mFadeInAnim);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    @Override
    public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
        Log.d(TAG, "startActivityForResult is un-implemented");
    }

    @Override
    public void setActivityResultCallback(CordovaPlugin plugin) {
        Log.d(TAG, "setActivityResultCallback is un-implemented");
    }

    @Override
    public Activity getActivity() {
        return (Activity) MainActivity.this;
    }

    @Override
    public Object onMessage(String msg, Object obj) {
        Log.d(TAG, msg);
        if (msg.equalsIgnoreCase("exit")) {
            super.finish();
        }
        return null;
    }

    @Override
    public ExecutorService getThreadPool() {
        return mThreadPool;
    }

    @Override
    public void requestPermission(CordovaPlugin plugin, int requestCode, String permission) {

    }

    @Override
    public void requestPermissions(CordovaPlugin plugin, int requestCode, String[] permissions) {

    }

    @Override
    public boolean hasPermission(String permission) {
        return false;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case SENDING_MESSAGE_FROM_MESSENGER_MSG: {
                final String msgData = (String) msg.obj;
                Log.d(TAG, "Sending Messenger Via Native To Home WebView");
                mHomeWebView.callHandler("handlerInJs", msgData, new CallBackFunction() {
                    @Override
                    public void onCallBack(String data) {
                        Log.d(TAG, "Response Data From JS" + data);
                    }
                });
                break;
            }
        }
        return true;
    }

    public class MessengerWebViewClient extends BridgeWebViewClient {

        public MessengerWebViewClient(BridgeWebView webView) {
            super(webView);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(((url.matches(MATCHED_PATTERN) ? "" : "file:///android_asset/www/") + url));
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }
    }

    public class MessengerChromeClient extends WebChromeClient {
        public MessengerChromeClient(BridgeWebView webView) {
            super();
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }
    }
}
