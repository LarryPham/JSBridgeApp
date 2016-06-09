function connectWebViewJavascriptBridge(callback) {
        if (window.WebViewJavascriptBridge) {
            callback(WebViewJavascriptBridge)
        } else {
            document.addEventListener('WebViewJavascriptBridgeReady', function() {
            callback(WebViewJavascriptBridge) }, false);
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
            document.getElementById("show").innerHTML = "Data From Java: " + data;
            var responseData = "JS says right back aka";
            responseCallback(responseData);
        });
 })