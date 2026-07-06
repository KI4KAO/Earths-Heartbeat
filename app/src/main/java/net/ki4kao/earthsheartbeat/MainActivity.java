package net.ki4kao.earthsheartbeat;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Full-page WebView of https://ki4kao.net/eh/index.html with:
 *   - pull-to-refresh
 *   - automatic reload every hour
 *   - overflow menu: manual refresh, open ki4kao.net in a browser, add widget
 */
public class MainActivity extends AppCompatActivity {

    private static final String PAGE_URL   = "https://ki4kao.net/eh/index.html";
    private static final String SITE_URL   = "https://ki4kao.net";
    private static final long   HOUR_MS    = 60L * 60L * 1000L;

    private WebView web;
    private SwipeRefreshLayout swipe;
    private ProgressBar progress;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hourlyReload = new Runnable() {
        @Override public void run() {
            if (web != null) web.reload();
            handler.postDelayed(this, HOUR_MS);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        web = findViewById(R.id.web);
        swipe = findViewById(R.id.swipe);
        progress = findViewById(R.id.progress);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                swipe.setRefreshing(false);
                progress.setVisibility(View.GONE);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
                progress.setProgress(p);
            }
        });

        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() { web.reload(); }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(PAGE_URL);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        handler.postDelayed(hourlyReload, HOUR_MS);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(hourlyReload);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            web.reload();
            return true;
        } else if (id == R.id.action_browser) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SITE_URL)));
            return true;
        } else if (id == R.id.action_add_widget) {
            requestPinWidget();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Ask the launcher to pin the widget (API 26+); otherwise guide the user. */
    private void requestPinWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppWidgetManager mgr = getSystemService(AppWidgetManager.class);
            ComponentName provider = new ComponentName(this, HeartbeatWidget.class);
            if (mgr != null && mgr.isRequestPinAppWidgetSupported()) {
                mgr.requestPinAppWidget(provider, null, null);
                return;
            }
        }
        Toast.makeText(this,
                "Long-press your home screen → Widgets → Earth's Heartbeat",
                Toast.LENGTH_LONG).show();
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
