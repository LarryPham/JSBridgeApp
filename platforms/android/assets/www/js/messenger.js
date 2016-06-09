function onShowInfo() {
    var str = document.getElementById("required-param").innerHTML;
    var data = { id: 1, content: str};

    window.WebViewJavascriptBridge.callHandler('messageSender', {'param': str}, function(responseData) {
        console.log('Sending message from messenger to native side');
        document.getElementById("feedback").innerHTML = responseData;
    });
}

function connectWebViewJavascriptBridge(callback) {
    if (window.WebViewJavascriptBridge) {
        callback(WebViewJavascriptBridge)
    } else {
        document.addEventListener('WebViewJavascriptBridgeReady', function() {
            callback(WebViewJavascriptBridge)
        },false);
    }
}

connectWebViewJavascriptBridge(function(bridge) {
    bridge.init(function(message, responseCallback) {
        console.log('JS got a message', message);
        var data = { 'Javascript responds': 'Responded Result'};
        console.log('JS responding with', data);
        responseCallback(data);
    });

    bridge.registerHandler("handlerInJs", function(data, responseCallback) {
        document.getElementById("show").innerHTML = "Data From Java" + data;
        var responseData = "JS says right back aka";
        responseCallback(responseData);
    });
})