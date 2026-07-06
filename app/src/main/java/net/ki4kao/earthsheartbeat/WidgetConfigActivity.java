package net.ki4kao.earthsheartbeat;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shown by the launcher when a Heartbeat widget is dropped on the home screen.
 * Lets the user choose which feed the widget displays, saves the choice, and
 * kicks off the first image fetch.
 */
public class WidgetConfigActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the user backs out, the widget must not be added.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_widget_config);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        ListView list = findViewById(R.id.feedList);
        list.setAdapter(new FeedAdapter());
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
                FeedCatalog.Feed feed = FeedCatalog.FEEDS[pos];
                WidgetPrefs.saveFeed(getApplicationContext(), appWidgetId, feed.url);

                // First render.
                WidgetUpdater.update(getApplicationContext(), appWidgetId);

                Intent result = new Intent();
                result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
    }

    private static final class FeedAdapter extends BaseAdapter {
        @Override public int getCount() { return FeedCatalog.FEEDS.length; }
        @Override public Object getItem(int i) { return FeedCatalog.FEEDS[i]; }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int pos, View convert, ViewGroup parent) {
            if (convert == null) {
                convert = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_feed, parent, false);
            }
            FeedCatalog.Feed f = FeedCatalog.FEEDS[pos];
            ((TextView) convert.findViewById(R.id.rowName)).setText(f.name);
            ((TextView) convert.findViewById(R.id.rowStation)).setText(f.station);
            return convert;
        }
    }
}
