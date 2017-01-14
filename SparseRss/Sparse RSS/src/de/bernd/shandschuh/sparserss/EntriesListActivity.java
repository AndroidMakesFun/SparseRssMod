/**
 * Sparse rss
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.util.TypedValue;
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

public class EntriesListActivity extends AppCompatActivity {
	private static final int CONTEXTMENU_MARKASREAD_ID = 6;

	private static final int CONTEXTMENU_MARKASUNREAD_ID = 7;

	private static final int CONTEXTMENU_DELETE_ID = 8;

	private static final int CONTEXTMENU_COPYURL = 9;

	public static final String EXTRA_SHOWREAD = "show_read";

	public static final String EXTRA_SHOWFEEDINFO = "show_feedinfo";

	public static final String EXTRA_AUTORELOAD = "autoreload";

	public static final String EXTRA_AUFRUFART = "aufrufart";
	public static final String EXTRA_ANZAHL = "anzahl";
	public static final String EXTRA_POSITION = "position";

	public static final String[] FEED_PROJECTION = { FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL, FeedData.FeedColumns.ICON };

	private Uri uri;

	private EntriesListAdapter entriesListAdapter;

	private byte[] iconBytes;

	private ListView listview;
	private TextView emptyview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Util.isLightTheme(this)) {
			setTheme(R.style.MyTheme_Light);
		}
		super.onCreate(savedInstanceState);

		String title = null;

		iconBytes = null;

		Intent intent = getIntent();


		setContentView(R.layout.entries);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		long feedId = intent.getLongExtra(FeedData.FeedColumns._ID, 0);

		if (feedId > 0) {
			Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedId), FEED_PROJECTION, null, null, null);

	        int buttonSize=Util.getButtonSizeInPixel(this);
			
			if (cursor.moveToFirst()) {
				title = cursor.isNull(0) ? cursor.getString(1) : cursor.getString(0);
				String link=cursor.getString(1);
				if(!link.contains(".feedburner.com")){
					iconBytes = cursor.getBlob(2);
					if(iconBytes!=null  && iconBytes.length>0){
						try {
							Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);			
							bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);
							BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
							int densityDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
							bitmapDrawable.setTargetDensity(densityDpi);	
							getSupportActionBar().setHomeAsUpIndicator(bitmapDrawable);
						} catch (Exception e) {
							System.err.println("Catched Exception for createScaledBitmap in EntriesListActivity");
							TextDrawable textDrawable = Util.getRoundButtonImage(this, Long.valueOf(feedId), "X");
							getSupportActionBar().setHomeAsUpIndicator(textDrawable);
						}
					}else{
						if (title != null) {
							TextDrawable textDrawable = Util.getRoundButtonImage(this, Long.valueOf(feedId), title);
							getSupportActionBar().setHomeAsUpIndicator(textDrawable);
						}
					}
				}else{
					if (title != null) {
						TextDrawable textDrawable = Util.getRoundButtonImage(this, Long.valueOf(feedId), title);
						getSupportActionBar().setHomeAsUpIndicator(textDrawable);
					}
				}
			}
			cursor.close();
		}

		uri = intent.getData();

		listview = (ListView) findViewById(android.R.id.list);
		if (!Util.isLightTheme(this)) {
			listview.setBackgroundColor(Color.BLACK);
		}
		entriesListAdapter = new EntriesListAdapter(this, uri, intent.getBooleanExtra(EXTRA_SHOWFEEDINFO, false), intent.getBooleanExtra(EXTRA_AUTORELOAD, false));
		listview.setAdapter(entriesListAdapter);
		
		emptyview = (TextView) findViewById(android.R.id.empty);
		if(entriesListAdapter.getCount()>0){
			emptyview.setVisibility(View.INVISIBLE);
		}
		
		if (title != null) {
			setTitle(title);
		}
		if (RSSOverview.notificationManager != null) {
			RSSOverview.notificationManager.cancel(0);
		}

		listview.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread).setIcon(android.R.drawable.ic_menu_manage);
				menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread).setIcon(android.R.drawable.ic_menu_manage);
				menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete).setIcon(android.R.drawable.ic_menu_delete);
				menu.add(0, CONTEXTMENU_COPYURL, Menu.NONE, R.string.contextmenu_copyurl).setIcon(android.R.drawable.ic_menu_share);
			}
		});
		
		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				TextView textView = (TextView) view.findViewById(android.R.id.text1);

				textView.setTypeface(Typeface.DEFAULT);
				textView.setEnabled(false);
				view.findViewById(android.R.id.text2).setEnabled(false);
				entriesListAdapter.neutralizeReadState();

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
					
					//mark read
//					ContentValues values = new ContentValues();
//					values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
//					int readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);
//					if (entryCursor.isNull(readDatePosition)) {
//						getContentResolver().update(contenUri, values, new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
//					}
					
					
					entryCursor.close();
					
					// AUFRUFART
//					String feedUri="content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/"+ feedid;
					Uri feedUri = FeedColumns.CONTENT_URI(feedid);
					try {
						Cursor cursor = getContentResolver().query(feedUri, FeedConfigActivity.PROJECTION, null, null, null);
						if (cursor.moveToNext()) {
							aufrufart = cursor.getInt(3); // 0.. {"Feed", "Browser", "Mobilize", "Instapaper"};
							cursor.close();
						}
					} catch (Exception e) {
						Util.toastMessageLong(EntriesListActivity.this, "feedUri:"+e);
						e.printStackTrace();
					}

					// aufrufart nur noch aus Prefs... TODO DB Part entfernen 
					aufrufart=Util.getViewerPrefs(EntriesListActivity.this, ""+feedid);
					
					//mark read 2
					entriesListAdapter.markAsRead(id);

					link=EntryActivity.fixLink(link);
					if (aufrufart == 1) {
						// // Browser öffnen
						getContentResolver().update(ContentUris.withAppendedId(uri, id), RSSOverview.getReadContentValues(), null, null);
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
						return;
					}
//					if (aufrufart == 3) {
//						//Problem Lesestatus !!
//						// // Browser öffnen
//						link = "http://www.instapaper.com/m?u=" + link;
//						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
//						return;
//					}

				}

				startActivity(new Intent(Intent.ACTION_VIEW, contenUri)
						.putExtra(EXTRA_SHOWREAD, entriesListAdapter.isShowRead())
						.putExtra(FeedData.FeedColumns.ICON, iconBytes)
						.putExtra(EXTRA_POSITION, position)
						.putExtra(EXTRA_ANZAHL, entriesListAdapter.getCount())
						.putExtra(EXTRA_AUFRUFART, aufrufart));
				
			}
		});

		
	} // onCreate

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Menues in die Toolbar, SHOW_AS_ACTION_ALWAYS zieht nur hier
        MenuItem markAsRead = menu.add(0, R.id.menu_markasread, 0, R.string.contextmenu_markasread); 
        MenuItemCompat.setShowAsAction(markAsRead, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        markAsRead.setIcon(android.R.drawable.ic_menu_revert);
		
		getMenuInflater().inflate(R.menu.entrylist, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(R.id.menu_group_0, entriesListAdapter.getCount() > 0);
		return true;
	}

	public void clickMarkAsRead(View view) { 
		new Thread() { // the update process takes some time
			public void run() {
				getContentResolver().update(uri, RSSOverview.getReadContentValues(), null, null);
			}
		}.start();
		entriesListAdapter.markAsRead();
		finish();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// Popup: Bearbeiten, etc.
		return onOptionsItemSelected(item);
	}

	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;

		default:
			break;
		}
		return onMenuItemSelected(item);
	}

		
	public boolean onMenuItemSelected(MenuItem item) {
		switch (item.getItemId()) {
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
			entriesListAdapter.markAsUnread();
			break;
		}
		case R.id.menu_hideread: {
			if (item.isChecked()) {
				item.setChecked(false).setTitle(R.string.contextmenu_hideread).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
				entriesListAdapter.showRead(true);
			} else {
				item.setChecked(true).setTitle(R.string.contextmenu_showread).setIcon(android.R.drawable.ic_menu_view);
				entriesListAdapter.showRead(false);
			}
			break;
		}
		case R.id.menu_deleteread: {
			new Thread() { // the delete process takes some time
				public void run() {
					String selection = Strings.READDATE_GREATERZERO + Strings.DB_AND + " (" + Strings.DB_EXCUDEFAVORITE + ")";

					getContentResolver().delete(uri, selection, null);
					FeedData.deletePicturesOfFeed(EntriesListActivity.this, uri, selection);
					runOnUiThread(new Runnable() {
						public void run() {
							entriesListAdapter.getCursor().requery();
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
									entriesListAdapter.getCursor().requery();
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

			getContentResolver().update(ContentUris.withAppendedId(uri, id), RSSOverview.getReadContentValues(), null, null);
			entriesListAdapter.markAsRead(id);
			break;
		}
		case CONTEXTMENU_MARKASUNREAD_ID: {
			long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;

			getContentResolver().update(ContentUris.withAppendedId(uri, id), RSSOverview.getUnreadContentValues(), null, null);
			entriesListAdapter.markAsUnread(id);
			break;
		}
		case CONTEXTMENU_DELETE_ID: {
			long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;

			getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
			FeedData.deletePicturesOfEntry(Long.toString(id));
			entriesListAdapter.getCursor().requery(); // we have no other choice
			break;
		}
		case CONTEXTMENU_COPYURL: {
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).targetView.getTag().toString());
			break;
		}

		}
		return true;
	}
	
	public EntriesListAdapter getEntriesListAdapter() {
		return entriesListAdapter;
	}


}
