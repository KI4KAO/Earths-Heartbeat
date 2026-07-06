package net.ki4kao.earthsheartbeat;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class HeartbeatWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "net.ki4kao.earthsheartbeat.ACTION_REFRESH";
    private static final String WORK_NAME = "heartbeat_widget_refresh";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            WidgetUpdater.update(context, id);
        }
        scheduleHourly(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                WidgetUpdater.update(context, id);
            } else {
                WidgetUpdater.updateAll(context);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) WidgetPrefs.clearFeed(context, id);
    }

    @Override
    public void onDisabled(Context context) {
        // Last widget removed — stop the periodic refresh.
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    /** Reliable hourly refresh backstop (updatePeriodMillis is best-effort). */
    private void scheduleHourly(Context context) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                WidgetRefreshWorker.class, 1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req);
    }
}
