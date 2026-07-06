package net.ki4kao.earthsheartbeat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Hourly background refresh of every placed Heartbeat widget. */
public class WidgetRefreshWorker extends Worker {

    public WidgetRefreshWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        WidgetUpdater.updateAll(getApplicationContext());
        return Result.success();
    }
}
