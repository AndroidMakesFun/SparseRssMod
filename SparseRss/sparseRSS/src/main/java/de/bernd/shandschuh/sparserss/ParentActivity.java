package de.bernd.shandschuh.sparserss;

import com.amulyakhare.textdrawable.TextDrawable;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.provider.FeedData.FeedColumns;

public class ParentActivity extends AppCompatActivity {

	protected static final int CONTEXTMENU_MARKASREAD_ID = 6;

	protected static final int CONTEXTMENU_MARKASUNREAD_ID = 7;

	protected static final int CONTEXTMENU_DELETE_ID = 8;

	protected static final int CONTEXTMENU_COPYURL = 9;

	public static final String EXTRA_SHOWREAD = "show_read";

	public static final String EXTRA_SHOWFEEDINFO = "show_feedinfo";
	public static final String EXTRA_SHOWFEEDFILTER = "show_feedfilter";
	public static final int EXTRA_FILTER_ALL=1;
	public static final int EXTRA_FILTER_OFFLINE=2;
	public static final int EXTRA_FILTER_TOP_FEEDS=3;

	public static final String EXTRA_AUTORELOAD = "autoreload";

	public static final String EXTRA_AUFRUFART = "aufrufart";
	public static final String EXTRA_ANZAHL = "anzahl";
	public static final String EXTRA_POSITION = "position";
	public static final String EXTRA_SELECTION_FILTER = "EXTRA_SELECTION_FILTER";

	public static final String[] FEED_PROJECTION = { FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL,
			FeedData.FeedColumns.ICON };	

	protected byte[] iconBytes;
	protected Uri uri;

	protected ListView listview;
	protected TextView emptyview;

	protected long mDateFromFirst;

	protected EntriesListAdapter mAdapter;

	protected long mLongFeedId = 0l;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Util.isLightTheme(this)) {
			setTheme(R.style.MyTheme_Light);
		}
		super.onCreate(savedInstanceState);

		String title = null;

		iconBytes = null;

		Intent intent = getIntent();

		mySetContentView();

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		int col=0xFF737373;
		toolbar.setTitleTextColor(col);

		mLongFeedId = intent.getLongExtra(FeedData.FeedColumns._ID, 0);

		if (mLongFeedId > 0) {
			Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(mLongFeedId), FEED_PROJECTION, null,
					null, null);

			int buttonSize = Util.getButtonSizeInPixel(this);

			if (cursor.moveToFirst()) {
				title = cursor.isNull(0) ? cursor.getString(1) : cursor.getString(0);
				String link = cursor.getString(1);
				if (!link.contains(".feedburner.com")) {
					iconBytes = cursor.getBlob(2);
					if (iconBytes != null && iconBytes.length > 0) {
						try {
							Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
							bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);
							bitmap = Util.getRoundedBitmap(bitmap);
							BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
							int densityDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
							bitmapDrawable.setTargetDensity(densityDpi);
							getSupportActionBar().setHomeAsUpIndicator(bitmapDrawable);
						} catch (Exception e) {
							System.err.println("Catched Exception for createScaledBitmap in EntriesListActivity");
							TextDrawable textDrawable = Util.getRoundButtonImage(this, Long.valueOf(mLongFeedId), "X");
							getSupportActionBar().setHomeAsUpIndicator(textDrawable);
						}
					} else {
						if (title != null) {
							TextDrawable textDrawable = Util.getRoundButtonImage(this, Long.valueOf(mLongFeedId), title);
							getSupportActionBar().setHomeAsUpIndicator(textDrawable);
						}
					}
				} else {
					if (title != null) {
						TextDrawable textDrawable = Util.getRoundButtonImage(this, Long.valueOf(mLongFeedId), title);
						getSupportActionBar().setHomeAsUpIndicator(textDrawable);
					}
				}
			}
			cursor.close();
		}

		uri = intent.getData();

		createAdapter();

		createListView();

		mDateFromFirst = ((EntriesListAdapter) mAdapter).getDateFromFirst();

		emptyview = (TextView) findViewById(android.R.id.empty);
		if (mAdapter.getCount() > 0) {
			emptyview.setVisibility(View.INVISIBLE);
		}

		if (title != null) {
			setTitle(title);
		}

	} // onCreate

	protected void mySetContentView() {
		System.err.println("OVERWRITTEN CONTENT VIEW");
	}

	protected void createAdapter() {
	}

	public void createListView() {
		listview = (ListView) findViewById(android.R.id.list);
		if (!Util.isLightTheme(this)) {
			listview.setBackgroundColor(Color.BLACK);
		}
		listview.setAdapter(mAdapter);
		listview.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView
						.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread)
						.setIcon(android.R.drawable.ic_menu_manage);
				menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread)
						.setIcon(android.R.drawable.ic_menu_manage);
				menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete)
						.setIcon(android.R.drawable.ic_menu_delete);
				menu.add(0, CONTEXTMENU_COPYURL, Menu.NONE, R.string.contextmenu_copyurl)
						.setIcon(android.R.drawable.ic_menu_share);
			}
		});

		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				TextView textView = (TextView) view.findViewById(android.R.id.text1);

				textView.setTypeface(Typeface.DEFAULT);
				textView.setEnabled(false);
				view.findViewById(android.R.id.text2).setEnabled(false);
				((EntriesListAdapter) mAdapter).neutralizeReadState();

				int aufrufart = 0;

				// Link aus Content
				Uri contenUri = ContentUris.withAppendedId(uri, id);
				Cursor entryCursor = getContentResolver().query(contenUri, null, null, null, null);
				String link = "";
				if (entryCursor.moveToFirst()) {
					int linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
					link = entryCursor.getString(linkPosition);

					int feedNr = entryCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
					long feedid = entryCursor.getLong(feedNr);

					entryCursor.close();

					// AUFRUFART
					// String
					// feedUri="content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/"+
					// feedid;
					Uri feedUri = FeedColumns.CONTENT_URI(feedid);
					try {
						Cursor cursor = getContentResolver().query(feedUri, FeedConfigActivity.PROJECTION, null, null,
								null);
						if (cursor.moveToNext()) {
							aufrufart = cursor.getInt(3); // 0.. {"Feed",
															// "Browser",
															// "Mobilize",
															// "Instapaper"};
							cursor.close();
						}
					} catch (Exception e) {
						Util.toastMessageLong(ParentActivity.this, "feedUri:" + e);
						e.printStackTrace();
					}

					// aufrufart nur noch aus Prefs... TODO DB Part entfernen
					aufrufart = Util.getViewerPrefs(ParentActivity.this, "" + feedid);

					// mark read 2
					((EntriesListAdapter) mAdapter).markAsRead(id);

					link = EntryActivity.fixLink(link);
					if (aufrufart == 1) {
						// // Browser ?ffnen
						getContentResolver().update(ContentUris.withAppendedId(uri, id),
								RSSOverview.getReadContentValues(), null, null);
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
						return;
					}
				}

				startActivity(new Intent(Intent.ACTION_VIEW, contenUri)
						.putExtra(EXTRA_SHOWREAD, ((EntriesListAdapter) mAdapter).isShowRead())
						.putExtra(FeedData.FeedColumns.ICON, iconBytes).putExtra(EXTRA_POSITION, position)
						.putExtra(EXTRA_ANZAHL, mAdapter.getCount()).putExtra(EXTRA_AUFRUFART, aufrufart)
						.putExtra(EXTRA_SELECTION_FILTER, mAdapter.getSelectionFilter())
						);

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.entrylist, menu);
		
		// Menues in die Toolbar, SHOW_AS_ACTION_ALWAYS zieht nur hier
		MenuItem markAsRead = menu.add(0, R.id.menu_markasread, 0, R.string.contextmenu_markasread);
		MenuItemCompat.setShowAsAction(markAsRead, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		markAsRead.setIcon(android.R.drawable.ic_menu_revert);

		MenuItem item = menu.findItem(R.id.menu_cardview);
		if (item != null) {
			if (Util.getTestListPrefs(getApplicationContext())) {
				item.setChecked(false);
			} else {
				item.setChecked(true);
			}
		}
		MenuItem itemTop = menu.findItem(R.id.menu_topfeed);
		if (itemTop != null) {
			if (getTopFeed(mLongFeedId)) {
				itemTop.setChecked(true);
			} else {
				itemTop.setChecked(false);
			}
		}
		MenuItem itemOffline = menu.findItem(R.id.menu_offline);
		if (itemOffline != null) {
			if (getOfflineFeed(mLongFeedId)) {
				itemOffline.setChecked(true);
			} else {
				itemOffline.setChecked(false);
			}
		}
		
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setOnQueryTextListener(mOnQueryTextListener);
		searchView.setQueryHint(this.getString(R.string.action_bar_search));
		
		return true;
	}

	private final SearchView.OnQueryTextListener mOnQueryTextListener = new SearchView.OnQueryTextListener() {
		@Override
		public boolean onQueryTextChange(String newText) {
			mAdapter.filter(newText);
			return true;
		}

		@Override
		public boolean onQueryTextSubmit(String query) {
			mAdapter.filter(query);
			return true;
		}
	};

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(R.id.menu_group_0, mAdapter.getCount() > 0);
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// Popup: Bearbeiten, etc.
		return onOptionsItemSelected(item);
	}

	public void clickMarkAsRead(View view) {
		new Thread() { // the update process takes some time
			public void run() {
//				String where = FeedData.EntryColumns.DATE + "<=" + ParentActivity.this.mDateFromFirst;
				String where = FeedData.EntryColumns.DATE + "<=" + ParentActivity.this.mDateFromFirst;
				where+= " AND " + mAdapter.getSelectionFilter(); //readdate is null AND _id in (1,2,3)
				getContentResolver().update(uri, RSSOverview.getReadContentValues(), where, null);
			}
		}.start();
		mAdapter.markAsRead();
		finish();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		
		case android.R.id.home:
			finish();
			break;
			
		case R.id.menu_markasread: {
			clickMarkAsRead(null);
			break;
		}
		case R.id.menu_markasunread: {
			new Thread() { // the update process takes some time
				public void run() {
					getContentResolver().update(uri, RSSOverview.getUnreadContentValues(), null, null);
				}
			}.start();
			mAdapter.markAsUnread();
			break;
		}
		case R.id.menu_hideread: {
			if (item.isChecked()) {
				item.setChecked(false).setTitle(R.string.contextmenu_hideread)
						.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
				mAdapter.showRead(true);
			} else {
				item.setChecked(true).setTitle(R.string.contextmenu_showread).setIcon(android.R.drawable.ic_menu_view);
				mAdapter.showRead(false);
			}
			break;
		}
		case R.id.menu_deleteread: {
			new Thread() { // the delete process takes some time
				public void run() {
					String selection = Strings.READDATE_GREATERZERO + Strings.DB_AND + " (" + Strings.DB_EXCUDEFAVORITE
							+ ")";

					getContentResolver().delete(uri, selection, null);
					FeedData.deletePicturesOfFeed(ParentActivity.this, uri, selection);
					runOnUiThread(new Runnable() {
						public void run() {
							mAdapter.getCursor().requery();
						}
					});
				}
			}.start();
			break;
		}
		case R.id.menu_deleteallentries: {
			Builder builder = new AlertDialog.Builder(this);

			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string.contextmenu_deleteallentries);
			builder.setMessage(R.string.question_areyousure);
			builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					new Thread() {
						public void run() {
							getContentResolver().delete(uri, Strings.DB_EXCUDEFAVORITE, null);
							runOnUiThread(new Runnable() {
								public void run() {
									mAdapter.getCursor().requery();
								}
							});
						}
					}.start();
				}
			});
			builder.setNegativeButton(android.R.string.no, null);
			builder.show();
			break;
		}
		case CONTEXTMENU_MARKASREAD_ID: {
			long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;

			getContentResolver().update(ContentUris.withAppendedId(uri, id), RSSOverview.getReadContentValues(), null,
					null);
			mAdapter.markAsRead(id);
			break;
		}
		case CONTEXTMENU_MARKASUNREAD_ID: {
			long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;

			getContentResolver().update(ContentUris.withAppendedId(uri, id), RSSOverview.getUnreadContentValues(), null,
					null);
			mAdapter.markAsUnread(id);
			break;
		}
		case CONTEXTMENU_DELETE_ID: {
			long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;

			getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
			FeedData.deletePicturesOfEntry(Long.toString(id));
			mAdapter.getCursor().requery(); // we have no other choice
			break;
		}
		case CONTEXTMENU_COPYURL: {
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
					.setText(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).targetView.getTag().toString());
			break;
		}
		case R.id.menu_cardview: {

			String str = this.getClass().getName();
			if ("de.bernd.shandschuh.sparserss.RecycleListActivity".equals(str)) {
				Util.setTestListPrefs(this, true);
				Intent intent = new Intent(getApplicationContext(), EntriesListActivity.class);
//				Intent intent = new Intent(Intent.ACTION_VIEW,FeedData.EntryColumns.CONTENT_URI(Long.toString(mLongFeedId)));
				intent.setData(FeedData.EntryColumns.CONTENT_URI(Long.toString(mLongFeedId)));
				intent.putExtra(FeedData.FeedColumns._ID, mLongFeedId);
				startActivity(intent);
			} else {
				Util.setTestListPrefs(this, false);
				Intent intent = new Intent(getApplicationContext(), RecycleListActivity.class);
				intent.setData(FeedData.EntryColumns.CONTENT_URI(Long.toString(mLongFeedId)));
				intent.putExtra(FeedData.FeedColumns._ID, mLongFeedId);
				startActivity(intent);
			}
			 finish();
			break;
		}
		case R.id.menu_topfeed: {
			if (getTopFeed(mLongFeedId)) {
				setTopFeed(mLongFeedId,false);
				item.setChecked(false);
			}else{
				setTopFeed(mLongFeedId,true);
				item.setChecked(true);
			}
			break;
		}
		case R.id.menu_offline: {
			if (getOfflineFeed(mLongFeedId)) {
				setOfflineFeed(mLongFeedId,false);
				item.setChecked(false);
			}else{
				setOfflineFeed(mLongFeedId,true);
				item.setChecked(true);
			}
			break;
		}

		}
		return super.onOptionsItemSelected(item);
	}

	public EntriesListAdapter getEntriesListAdapter() {
		return mAdapter;
	}

	public String[]TOPFEED_PROJECTION={FeedData.FeedColumns.TOPFEED};
	
	public boolean getTopFeed(long feedId){
		boolean ret=false;
		Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedId), TOPFEED_PROJECTION, null,
				null, null);
		if (cursor.moveToFirst()) {
			ret = (cursor.getInt(0)==1);
		}
		cursor.close();		
		return ret;
	}
	
	public void setTopFeed(long feedId, boolean bLeadgroup){
		ContentValues values = new ContentValues();
		values.put(FeedData.FeedColumns.TOPFEED, bLeadgroup ? 1 : 0);

		getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(feedId),
				values, null, null);
	}
	
	// Die Offline Spalte heist SYNC !
	public String[]OFFLINE_PROJECTION={FeedData.FeedColumns.SYNC};

	public boolean getOfflineFeed(long feedId){
		boolean ret=false;
		Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedId), OFFLINE_PROJECTION, null,
				null, null);
		if (cursor.moveToFirst()) {
			ret = (cursor.getInt(0)==1);
		}
		cursor.close();		
		return ret;
	}
	
	public void setOfflineFeed(long feedId, boolean bLeadgroup){
		ContentValues values = new ContentValues();
		values.put(FeedData.FeedColumns.SYNC, bLeadgroup ? 1 : 0);

		getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(feedId),
				values, null, null);
	}
}
