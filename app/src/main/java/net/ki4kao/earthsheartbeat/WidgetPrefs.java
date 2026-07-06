package net.ki4kao.earthsheartbeat;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores which feed URL each placed widget should display. */
public final class WidgetPrefs {
    private static final String FILE = "heartbeat_widgets";
    private static final String KEY = "feed_url_";

    public static void saveFeed(Context ctx, int appWidgetId, String url) {
        prefs(ctx).edit().putString(KEY + appWidgetId, url).apply();
    }

    public static String loadFeed(Context ctx, int appWidgetId) {
        return prefs(ctx).getString(KEY + appWidgetId, FeedCatalog.FEEDS[0].url);
    }

    public static void clearFeed(Context ctx, int appWidgetId) {
        prefs(ctx).edit().remove(KEY + appWidgetId).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private WidgetPrefs() {}
}
