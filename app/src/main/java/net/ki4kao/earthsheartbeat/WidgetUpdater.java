package net.ki4kao.earthsheartbeat;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads the selected feed image (sending the correct Referer to bypass
 * hotlink protection), scales it down to keep the RemoteViews transaction
 * small, and pushes it into the widget. All network work happens on a
 * background executor; RemoteViews updates are marshalled by the framework.
 */
public final class WidgetUpdater {

    // Max width we hand to RemoteViews. Full feed images can blow past the
    // ~1 MB IPC transaction limit, so we cap and re-encode.
    private static final int MAX_W = 720;

    private static final ExecutorService IO = Executors.newFixedThreadPool(3);

    /** Refresh a single widget asynchronously. */
    public static void update(final Context appCtx, final int appWidgetId) {
        final Context ctx = appCtx.getApplicationContext();
        showLoading(ctx, appWidgetId);
        IO.execute(new Runnable() {
            @Override public void run() {
                String url = WidgetPrefs.loadFeed(ctx, appWidgetId);
                Bitmap bmp = fetch(url);
                render(ctx, appWidgetId, bmp);
            }
        });
    }

    /** Refresh every placed widget (used by the periodic worker). */
    public static void updateAll(Context appCtx) {
        Context ctx = appCtx.getApplicationContext();
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, HeartbeatWidget.class));
        if (ids == null) return;
        for (int id : ids) update(ctx, id);
    }

    private static void showLoading(Context ctx, int appWidgetId) {
        RemoteViews rv = baseViews(ctx, appWidgetId);
        rv.setViewVisibility(R.id.widgetProgress, android.view.View.VISIBLE);
        rv.setViewVisibility(R.id.widgetStatus, android.view.View.GONE);
        // Full update (not partial) so this works on the widget's first render.
        AppWidgetManager.getInstance(ctx).updateAppWidget(appWidgetId, rv);
    }

    private static void render(Context ctx, int appWidgetId, Bitmap bmp) {
        RemoteViews rv = baseViews(ctx, appWidgetId);
        rv.setViewVisibility(R.id.widgetProgress, android.view.View.GONE);
        if (bmp != null) {
            rv.setImageViewBitmap(R.id.widgetImage, bmp);
            rv.setViewVisibility(R.id.widgetStatus, android.view.View.GONE);
            String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
            rv.setTextViewText(R.id.widgetUpdated, "updated " + t);
        } else {
            rv.setViewVisibility(R.id.widgetStatus, android.view.View.VISIBLE);
            rv.setTextViewText(R.id.widgetStatus, "signal lost — tap ↻");
            rv.setTextViewText(R.id.widgetUpdated, "");
        }
        AppWidgetManager.getInstance(ctx).updateAppWidget(appWidgetId, rv);
    }

    /** Builds the RemoteViews with title + pending intents wired up. */
    private static RemoteViews baseViews(Context ctx, int appWidgetId) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_heartbeat);
        FeedCatalog.Feed feed = FeedCatalog.byUrl(WidgetPrefs.loadFeed(ctx, appWidgetId));
        rv.setTextViewText(R.id.widgetTitle, feed.name);

        // Refresh button -> broadcast back to the provider for this widget id.
        Intent refresh = new Intent(ctx, HeartbeatWidget.class);
        refresh.setAction(HeartbeatWidget.ACTION_REFRESH);
        refresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        refresh.setData(Uri.parse("heartbeat://widget/" + appWidgetId));
        rv.setOnClickPendingIntent(R.id.widgetRefresh,
                PendingIntent.getBroadcast(ctx, appWidgetId, refresh, piFlags()));

        // Tapping the image opens the app.
        Intent open = new Intent(ctx, SplashActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        rv.setOnClickPendingIntent(R.id.widgetImage,
                PendingIntent.getActivity(ctx, 1000 + appWidgetId, open, piFlags()));
        return rv;
    }

    private static int piFlags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }

    // ---- networking --------------------------------------------------------

    private static Bitmap fetch(String urlStr) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("Referer", refererFor(url.getHost()));
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Android) EarthsHeartbeat/1.0");
            c.setRequestProperty("Accept", "image/*,*/*;q=0.8");
            int code = c.getResponseCode();
            if (code >= 400) return null;

            InputStream in = new BufferedInputStream(c.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
            in.close();
            byte[] bytes = buf.toByteArray();
            if (bytes.length < 100) return null;
            return decodeScaled(bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Decode with an appropriate sample size, then hard-cap width to MAX_W. */
    private static Bitmap decodeScaled(byte[] bytes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        int sample = 1;
        while (bounds.outWidth / sample > MAX_W * 2) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        if (bmp == null) return null;
        if (bmp.getWidth() > MAX_W) {
            int h = Math.round(bmp.getHeight() * (MAX_W / (float) bmp.getWidth()));
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, MAX_W, Math.max(1, h), true);
            if (scaled != bmp) bmp.recycle();
            return scaled;
        }
        return bmp;
    }

    /** Per-host Referer matching the proxy.php whitelist. */
    private static String refererFor(String host) {
        host = host == null ? "" : host.toLowerCase(Locale.US);
        if (host.endsWith("vlf.it"))      return "http://www.vlf.it/";
        if (host.endsWith("etna-ero.it")) return "http://www.etna-ero.it/";
        if (host.endsWith("sos70.ru"))    return "https://sos70.ru/";
        if (host.endsWith("nckobs.hu"))   return "https://nckobs.hu/";
        return "https://ki4kao.net/";
    }

    private WidgetUpdater() {}
}
