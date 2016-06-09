package com.capsule.apps.jsbridgeapp;


public interface WebViewJavascriptBridge {
	void send(String data);
	void send(String data, CallBackFunction responseCallback);
}
