package com.example.androidproject;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.example.androidproject.R;

import java.util.ArrayList;
import java.util.Map;

public class WebViewActivity extends AppCompatActivity {

    WebView webView;
    ArrayList<Map<String, String>> scoreList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        scoreList = (ArrayList<Map<String, String>>) getIntent().getSerializableExtra("scoreList");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String dataJson = convertScoreListToJson(scoreList);
                String script = "javascript:data = " + dataJson + "; " +
                        "graph = Flotr.draw(container, data, {" +
                        "yaxis: {min: 0, max: 100}," +
                        "points: {show: true}," +
                        "lines: {show: true}" +
                        "});";
                view.loadUrl(script);
            }
        });

        webView.loadUrl("file:///android_asset/test.html");
    }

    private String convertScoreListToJson(ArrayList<Map<String, String>> scoreList) {
        StringBuilder builder = new StringBuilder();
        builder.append("[{data:[");

        for (int i = 0; i < scoreList.size(); i++) {
            int score = Integer.parseInt(scoreList.get(i).get("score"));
            builder.append("[").append(i).append(",").append(score).append("]");
            if (i != scoreList.size() - 1) builder.append(",");
        }

        builder.append("]}]");
        return builder.toString();
    }
}
