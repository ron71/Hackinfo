package com.behindinfinity.hackinfo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.HttpURLConnection;
import java.net.URL;

public class WebViewActivity extends AppCompatActivity {
    WebView wb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        wb = findViewById(R.id.webView);
        hideNavigationBar();
        Intent i =getIntent();
        Bundle b = i.getExtras();
        String url = b.getString("url");
        Log.i("WEB VIEW URL : ",url);

        try{
//            URL url = new URL(urlString);
//            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            // Above lines are not necssory as we can use URL directly as an String in load method

            wb.getSettings().setJavaScriptEnabled(true);
            wb.setWebViewClient(new WebViewClient());
            wb.loadUrl(url);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
    public void hideNavigationBar() {
        this.getWindow().getDecorView().
                setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
    }
}
