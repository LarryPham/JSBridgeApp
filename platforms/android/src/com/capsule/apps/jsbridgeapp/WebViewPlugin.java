package com.capsule.apps.jsbridgeapp;

import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Larry Pham
 * @since June.08.2016
 */
public class WebViewPlugin extends CordovaPlugin {
    protected static final String LOG_TAG = WebViewPlugin.class.getCanonicalName();

    public static final String SHOW_ACTIVITY_AS_SLIDE_PANEL = "show_activity_as_slide_panel";

    private CallbackContext mCallbackContext;
    private static CallbackContext mSubscribeCallbackContext = null;

    public WebViewPlugin() {

    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("show") && args.length() > 0) {
            LOG.d(LOG_TAG, "Show Web View");
            final String url = args.getString(0);

            if (!"".equals(url)) {
                showWebView(url);
                JSONObject r = new JSONObject();
                r.put("responseCode", "ok");
                callbackContext.success(r);
            } else {
                callbackContext.error("Empty Parameter url");
            }

        } else if (action.equals("hide")) {
            LOG.d(LOG_TAG, "Hide Web View");
            hideWebView();
            JSONObject r = new JSONObject();
            r.put("responseCode", "ok");
            callbackContext.success(r);
        } else if (action.equals("subscribeCallback")) {
            LOG.d(LOG_TAG, "Subscribing Cordova CallbackContext");
            mSubscribeCallbackContext = callbackContext;
        } else {
            return false;
        }

        return true;
    }

    private void showWebView(final String url) {
        LOG.d(LOG_TAG, "Url: " + url);
        final Intent intent = new Intent();
        //MainActivity.mApp.sendBroadCastShowOrHidePanel(intent, MainActivity.SHOW_MESSENGER_PANEL, url);
    }


    private void hideWebView() {
        LOG.d(LOG_TAG, "hideWebView");

        /*MainActivity.mApp.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.mApp.loadMessengerWebViewContent("");
                MainActivity.mApp.setMessengerVisibility(View.INVISIBLE);
                MainActivity.mApp.clearMessengerHistory();
            }
        });*/

        final Intent intent = new Intent();
        //MainActivity.mApp.sendBroadCastShowOrHidePanel(intent, MainActivity.CLOSE_MESSENGER_PANEL, null);

        if (mSubscribeCallbackContext != null) {
            LOG.d(LOG_TAG, "Calling subscribeCallbackContext success");
            mSubscribeCallbackContext.success();
            mSubscribeCallbackContext = null;
        }
    }
}
