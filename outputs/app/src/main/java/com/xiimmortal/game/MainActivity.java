package com.xiimmortal.game;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(0);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new TokenProvider(), "AndroidBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/www/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    class TokenProvider {
        @JavascriptInterface
        public String getToken() {
            try {
                return new String(Base64.decode("Z2hwX0pKSTN1aUtqRFMxOW5UOEdydllsTUc0ZGZHQlIwUDEweFhX", Base64.DEFAULT));
            } catch (Exception e) {
                return "";
            }
        }
    }
}
