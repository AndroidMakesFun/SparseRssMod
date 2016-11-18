package de.bernd.shandschuh.sparserss.endlessscroll;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import de.bernd.shandschuh.sparserss.EntriesListAdapter;
import de.bernd.shandschuh.sparserss.R;
import de.bernd.shandschuh.sparserss.RSSOverview;
import de.bernd.shandschuh.sparserss.provider.FeedData;

public class RecycleListActivity extends AppCompatActivity {

	private static final String[] FEED_PROJECTION = { FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL, FeedData.FeedColumns.ICON };
	public static final String EXTRA_SHOWFEEDINFO = "show_feedinfo";
	public static final String EXTRA_AUTORELOAD = "autoreload";

	private byte[] iconBytes;
	private Uri uri;
	
	private ListView listview;
	private TextView emptyview;
	private EntriesListAdapter entriesListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recycle_list);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		String title = null;
		iconBytes = null;

		long feedId = getIntent().getLongExtra(FeedData.FeedColumns._ID, 0);
		if (feedId > 0) {
			Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedId), FEED_PROJECTION, null, null, null);

			if (cursor.moveToFirst()) {
				title = cursor.isNull(0) ? cursor.getString(1) : cursor.getString(0);
				iconBytes = cursor.getBlob(2);
			}
			cursor.close();
		}

		uri = getIntent().getData();
		listview = (ListView) findViewById(android.R.id.list);

		entriesListAdapter = new EntriesListAdapter(this, uri, getIntent().getBooleanExtra(EXTRA_SHOWFEEDINFO, false), getIntent().getBooleanExtra(EXTRA_AUTORELOAD, false));
		listview.setAdapter(entriesListAdapter);
		
//		listview.setOnScrollListener(new EndlessScrollListener() {
//	          @Override
//	          public boolean onLoadMore(int page, int totalItemsCount) {
//	              // Triggered only when new data needs to be appended to the list
//	              // Add whatever code is needed to append new items to your AdapterView
//	              customLoadMoreDataFromApi(page); 
//	              // or customLoadMoreDataFromApi(totalItemsCount); 
//	              return true; // ONLY if more data is actually being loaded; false otherwise.
//	          }
//	        });
		
		emptyview = (TextView) findViewById(android.R.id.empty);
		if(entriesListAdapter.getCount()>0){
			emptyview.setVisibility(View.INVISIBLE);
		}
		if (title != null) {
			setTitle("endless " + title);
		}
		if (iconBytes != null && iconBytes.length > 0) {
			int bitmapSizeInDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics());
			Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
			if (bitmap != null) {
				if (bitmap.getHeight() != bitmapSizeInDip) {
					bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
				}

//				setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(bitmap));
			}
		}
		if (RSSOverview.notificationManager != null) {
			RSSOverview.notificationManager.cancel(0);
		}

		
	} // onCreate

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recycle_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
