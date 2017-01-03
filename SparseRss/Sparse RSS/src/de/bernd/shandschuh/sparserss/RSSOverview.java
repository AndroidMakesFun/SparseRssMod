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

import java.io.File;
import java.io.FilenameFilter;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.bernd.shandschuh.sparserss.endlessscroll.RecycleListActivity;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.provider.OPML;
import de.bernd.shandschuh.sparserss.service.RefreshService;
import de.bernd.shandschuh.sparserss.util.NavigationDrawerAdapter;
import de.bernd.shandschuh.sparserss.util.NavigationDrawerAdapter.NavDrawerLineEntry;

public class RSSOverview extends AppCompatActivity {
	private static final int DIALOG_ERROR_FEEDIMPORT = 3;

	private static final int DIALOG_ERROR_FEEDEXPORT = 4;

	private static final int DIALOG_ERROR_INVALIDIMPORTFILE = 5;

	private static final int DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE = 6;

	private static final int DIALOG_ABOUT = 7;

	private static final int CONTEXTMENU_EDIT_ID = 3;

	private static final int CONTEXTMENU_REFRESH_ID = 4;

	private static final int CONTEXTMENU_DELETE_ID = 5;

	private static final int CONTEXTMENU_MARKASREAD_ID = 6;

	private static final int CONTEXTMENU_MARKASUNREAD_ID = 7;

	private static final int CONTEXTMENU_DELETEREAD_ID = 8;

	private static final int CONTEXTMENU_DELETEALLENTRIES_ID = 9;

	private static final int CONTEXTMENU_RESETUPDATEDATE_ID = 10;

	private static final int ACTIVITY_APPLICATIONPREFERENCES_ID = 1;

	private static final Uri CANGELOG_URI = Uri.parse("http://code.google.com/p/sparserss/wiki/Changelog");

	public static NotificationManager notificationManager; // package scope

	boolean feedSort;

	private RSSOverviewListAdapter listAdapter;
	private ListView listview;
	private TextView emptyview;
	private ProgressBar progressBar;

	public static RSSOverview INSTANCE;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (Util.isLightTheme(this)) {
			setTheme(R.style.MyTheme_Light);
		} else {
			setTheme(R.style.Theme_AppCompat_NoActionBar); // @style/Theme.AppCompat.NoActionBar
		}
		super.onCreate(savedInstanceState);

		if (notificationManager == null) {
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
		setContentView(R.layout.main);
		INSTANCE = this;

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_grey600_36dp);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		setupDrawerContent();

		// setHomeButtonActive();
		progressBar = (ProgressBar) findViewById(R.id.progress_spinner);

		listview = (ListView) findViewById(android.R.id.list);
		listAdapter = new RSSOverviewListAdapter(this);
		listview.setAdapter(listAdapter);
		if (!Util.isLightTheme(this)) {
			listview.setBackgroundColor(Color.BLACK);
		}

		emptyview = (TextView) findViewById(android.R.id.empty);
		if (listAdapter.getCount() > 0) {
			emptyview.setVisibility(View.INVISIBLE);
		}

		listview.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView
						.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_REFRESH_ID, Menu.NONE, R.string.contextmenu_refresh);
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread);
				menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread);
				menu.add(0, CONTEXTMENU_DELETEREAD_ID, Menu.NONE, R.string.contextmenu_deleteread);
				menu.add(0, CONTEXTMENU_DELETEALLENTRIES_ID, Menu.NONE, R.string.contextmenu_deleteallentries);
				menu.add(0, CONTEXTMENU_EDIT_ID, Menu.NONE, R.string.contextmenu_edit);
				menu.add(0, CONTEXTMENU_RESETUPDATEDATE_ID, Menu.NONE, R.string.contextmenu_resetupdatedate);
				menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete);
			}
		});
		listview.setOnTouchListener(new OnTouchListener() {
			private int dragedItem = -1;

			private ImageView dragedView;

			private WindowManager windowManager = RSSOverview.this.getWindowManager();

			private LayoutParams layoutParams;

			private int minY = 25; // is the header size --> needs to be changed

			// private ListView listView = getListView();

			public boolean onTouch(View v, MotionEvent event) {
				if (feedSort) {
					int action = event.getAction();

					switch (action) {
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_MOVE: {
						// this is the drag action
						if (dragedItem == -1) {
							dragedItem = listview.pointToPosition((int) event.getX(), (int) event.getY());
							if (dragedItem > -1) {
								dragedView = new ImageView(listview.getContext());

								View item = listview.getChildAt(dragedItem - listview.getFirstVisiblePosition());

								if (item != null) {
									View sortView = item.findViewById(R.id.sortitem);

									if (sortView.getLeft() <= event.getX()) {
										item.setDrawingCacheEnabled(true);
										dragedView.setImageBitmap(Bitmap.createBitmap(item.getDrawingCache()));

										layoutParams = new LayoutParams();
										layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
										layoutParams.gravity = Gravity.TOP;
										layoutParams.y = (int) event.getY();
										windowManager.addView(dragedView, layoutParams);
									} else {
										dragedItem = -1;
										return false; // do not comsume
									}

								} else {
									dragedItem = -1;
								}
							}
						} else if (dragedView != null) {
							layoutParams.y = Math.max(minY,
									Math.max(0, Math.min((int) event.getY(), listview.getHeight() - minY)));
							windowManager.updateViewLayout(dragedView, layoutParams);
						}
						break;
					}
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL: {
						// this is the drop action
						if (dragedItem > -1) {
							windowManager.removeView(dragedView);

							int newPosition = listview.pointToPosition((int) event.getX(), (int) event.getY());

							if (newPosition == -1) {
								newPosition = listview.getCount() - 1;
							}
							if (newPosition != dragedItem) {
								ContentValues values = new ContentValues();

								values.put(FeedData.FeedColumns.PRIORITY, newPosition);
								getContentResolver().update(
										FeedData.FeedColumns.CONTENT_URI(listview.getItemIdAtPosition(dragedItem)),
										values, null, null);
							}
							dragedItem = -1;
							return true;
						} else {
							return false;
						}
					}
					}
					return true;
				} else {
					return false;
				}
			}
		});

		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				setFeedSortEnabled(false);

				if (Util.getTestListPrefs(getApplicationContext())) {
					Intent intent = new Intent(Intent.ACTION_VIEW,FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));
					intent.putExtra(FeedData.FeedColumns._ID, id);
					startActivity(intent);
				} else {
					Intent intent = new Intent(getApplicationContext(), RecycleListActivity.class);
					intent.setData(FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));
					intent.putExtra(FeedData.FeedColumns._ID, id);
					startActivity(intent);
				}
			}

		});

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHENABLED, false)) {
			startService(new Intent(this, RefreshService.class)); // starts the
																	// service
																	// independent
																	// to this
																	// activity
		} else {
			stopService(new Intent(this, RefreshService.class));
		}
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHONPENENABLED,
				false)) {
			new Thread() {
				public void run() {
					sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
				}
			}.start();
		}

		// //ab api21!
		// listview.setNestedScrollingEnabled(true);

		// setHomeButtonActive();
		// myActionBar().setDisplayShowTitleEnabled(false);
		// myActionBar().setNavigationMode(android.support.v7.app.ActionBar.NAVIGATION_MODE_LIST);

	} // onCreate

	@Override
	protected void onResume() {
		super.onResume();

		zeigeProgressBar(Util.isCurrentlyRefreshing(this));
		registerReceiver(refreshReceiver, new IntentFilter("de.bernd.shandschuh.sparserss.REFRESH"));

		// if (RSSOverview.notificationManager != null) {
		// notificationManager.cancel(0);
		// }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Menues in die Toolbar, SHOW_AS_ACTION_ALWAYS zieht nur hier
		MenuItem addFeedMenu = menu.add(0, R.id.menu_addfeed, 0, R.string.menu_addfeed);
		MenuItemCompat.setShowAsAction(addFeedMenu, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		addFeedMenu.setIcon(android.R.drawable.ic_menu_add);

		MenuItem refreshMenu = menu.add(0, R.id.menu_refresh, 0, R.string.menu_refresh);
		MenuItemCompat.setShowAsAction(refreshMenu, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		refreshMenu.setIcon(android.R.drawable.ic_menu_rotate);

		// MenuItem settingsMenu = menu.add(0, R.id.menu_settings, 0,
		// R.string.menu_settings);
		// MenuItemCompat.setShowAsAction(settingsMenu,
		// MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		// settingsMenu.setIcon(android.R.drawable.ic_menu_preferences);

		getMenuInflater().inflate(R.menu.feedoverview, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupVisible(R.id.menu_group_0, !feedSort);
		menu.setGroupVisible(R.id.menu_group_1, feedSort);
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// return super.onContextItemSelected(item);
		// Popup: Bearbeiten, etc.
		return onOptionsItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		
		setFeedSortEnabled(false);
		
		switch (item.getItemId()) {
		
        case android.R.id.home:
            mDrawerLayout.openDrawer(GravityCompat.START);
            return true;
		
		case R.id.menu_addfeed: {
			startActivity(new Intent(Intent.ACTION_INSERT).setData(FeedData.FeedColumns.CONTENT_URI));
			break;
		}
		case R.id.menu_refresh: {
			new Thread() {
				public void run() {
					sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY,
							PreferenceManager.getDefaultSharedPreferences(RSSOverview.this)
									.getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false)));
				}
			}.start();
			break;
		}
		case CONTEXTMENU_EDIT_ID: {
			startActivity(new Intent(Intent.ACTION_EDIT).setData(
					FeedData.FeedColumns.CONTENT_URI(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)));
			break;
		}
		case CONTEXTMENU_REFRESH_ID: {
			final String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

			ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
					Context.CONNECTIVITY_SERVICE);

			final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

			if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) { // since
																								// we
																								// have
																								// acquired
																								// the
																								// networkInfo,
																								// we
																								// use
																								// it
																								// for
																								// basic
																								// checks
				final Intent intent = new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.FEEDID, id);

				final Thread thread = new Thread() {
					public void run() {
						sendBroadcast(intent);
					}
				};

				if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
						|| PreferenceManager.getDefaultSharedPreferences(RSSOverview.this)
								.getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false)) {
					intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
					thread.start();
				} else {
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(id),
							new String[] { FeedData.FeedColumns.WIFIONLY }, null, null, null);

					cursor.moveToFirst();

					if (cursor.isNull(0) || cursor.getInt(0) == 0) {
						thread.start();
					} else {
						Builder builder = new AlertDialog.Builder(RSSOverview.this);

						builder.setIcon(android.R.drawable.ic_dialog_alert);
						builder.setTitle(R.string.dialog_hint);
						builder.setMessage(R.string.question_refreshwowifi);
						builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
								thread.start();
							}
						});
						builder.setNeutralButton(R.string.button_alwaysokforall, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								PreferenceManager.getDefaultSharedPreferences(RSSOverview.this).edit()
										.putBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, true).commit();
								intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
								thread.start();
							}
						});
						builder.setNegativeButton(android.R.string.no, null);
						builder.show();
					}
					cursor.close();
				}

			}
			break;
		}
		case CONTEXTMENU_DELETE_ID: {
			String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

			Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(id),
					new String[] { FeedData.FeedColumns.NAME }, null, null, null);

			cursor.moveToFirst();

			Builder builder = new AlertDialog.Builder(RSSOverview.this);

			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(cursor.getString(0));
			builder.setMessage(R.string.question_deletefeed);
			builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					new Thread() {
						public void run() {
							getContentResolver().delete(
									FeedData.FeedColumns.CONTENT_URI(Long
											.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)),
									null, null);
							sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
						}
					}.start();
				}
			});
			builder.setNegativeButton(android.R.string.no, null);
			cursor.close();
			builder.show();
			break;
		}
		case CONTEXTMENU_MARKASREAD_ID: {
			new Thread() {
				public void run() {
					String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

					if (getContentResolver().update(FeedData.EntryColumns.CONTENT_URI(id), getReadContentValues(),
							new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(),
							null) > 0) {
						getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI(id), null);
					}
				}
			}.start();
			break;
		}
		case CONTEXTMENU_MARKASUNREAD_ID: {
			new Thread() {
				public void run() {
					String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

					if (getContentResolver().update(FeedData.EntryColumns.CONTENT_URI(id), getUnreadContentValues(),
							null, null) > 0) {
						getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI(id), null);
						;
					}
				}
			}.start();
			break;
		}
		case CONTEXTMENU_DELETEREAD_ID: {
			new Thread() {
				public void run() {
					String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

					Uri uri = FeedData.EntryColumns.CONTENT_URI(id);

					String selection = Strings.READDATE_GREATERZERO + Strings.DB_AND + " (" + Strings.DB_EXCUDEFAVORITE
							+ ")";

					FeedData.deletePicturesOfFeed(RSSOverview.this, uri, selection);
					if (getContentResolver().delete(uri, selection, null) > 0) {
						getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI(id), null);
					}
				}
			}.start();
			break;
		}
		case CONTEXTMENU_DELETEALLENTRIES_ID: {
			showDeleteAllEntriesQuestion(this, FeedData.EntryColumns
					.CONTENT_URI(Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)));
			break;
		}
		case CONTEXTMENU_RESETUPDATEDATE_ID: {
			ContentValues values = new ContentValues();

			values.put(FeedData.FeedColumns.LASTUPDATE, 0);
			values.put(FeedData.FeedColumns.REALLASTUPDATE, 0);
			getContentResolver().update(
					FeedData.FeedColumns
							.CONTENT_URI(Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)),
					values, null, null);
			break;
		}

		case R.id.menu_settings: {
			startActivityForResult(new Intent(this, ApplicationPreferencesActivity.class),
					ACTIVITY_APPLICATIONPREFERENCES_ID);
			break;
		}
		case R.id.menu_allread: {
			new Thread() {
				public void run() {
					if (getContentResolver().update(FeedData.EntryColumns.CONTENT_URI, getReadContentValues(),
							new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(),
							null) > 0) {
						getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI, null);
					}
				}
			}.start();
			break;
		}
		case R.id.menu_about: {
			showDialog(DIALOG_ABOUT);
			break;
		}
		case R.id.menu_import: {
			final AlertDialog.Builder builder = new AlertDialog.Builder(RSSOverview.this);

			// builder.setTitle(R.string.select_file);
			builder.setTitle("From " + this.getExternalFilesDir("rss"));

			try {
				final String[] fileNames = this.getExternalFilesDir("rss").list(new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						return new File(dir, filename).isFile();
					}
				});

				final RSSOverview rssOverview = this;
				builder.setItems(fileNames, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						try {
							OPML.importFromFile(
									new StringBuilder(rssOverview.getExternalFilesDir("rss").toString())
											.append(File.separator).append(fileNames[which]).toString(),
									RSSOverview.this);
						} catch (Exception e) {
							showDialog(DIALOG_ERROR_FEEDIMPORT);
						}
					}
				});
				builder.show();
			} catch (Exception e) {
				showDialog(DIALOG_ERROR_FEEDIMPORT);
			}
			break;
		}
		case R.id.menu_export: {
			try {
				String folder = this.getExternalFilesDir("rss").toString();
				String filename = new StringBuilder(folder).append("/sparse_rss_").append(System.currentTimeMillis())
						.append(".opml").toString();

				OPML.exportToFile(filename, this);
				Toast.makeText(this, String.format(getString(R.string.message_exportedto), filename), Toast.LENGTH_LONG)
						.show();
			} catch (Exception e) {
				showDialog(DIALOG_ERROR_FEEDEXPORT);
			}
			break;
		}
		case R.id.menu_enablefeedsort: {
			setFeedSortEnabled(true);
			break;
		}
		case R.id.menu_deleteread: {
			FeedData.deletePicturesOfFeedAsync(this, FeedData.EntryColumns.CONTENT_URI, Strings.READDATE_GREATERZERO);
			getContentResolver().delete(FeedData.EntryColumns.CONTENT_URI, Strings.READDATE_GREATERZERO, null);
			// ((RSSOverviewListAdapter)
			// getListAdapter()).notifyDataSetChanged();
			listAdapter.notifyDataSetChanged();
			break;
		}
		case R.id.menu_deleteallentries: {
			showDeleteAllEntriesQuestion(this, FeedData.EntryColumns.CONTENT_URI);
			break;
		}
		case R.id.menu_disablefeedsort: {
			// do nothing as the feed sort gets disabled anyway
			break;
		}
		case R.id.menu_log: {
			Intent intent = new Intent(this, SendLogActivity.class);
			startActivity(intent);
			break;
		}
		case R.id.menu_alle: {
			clickShowAll(null);
			break;
		}
		case R.id.menu_favorites: {
			clickShowFav(null);
			break;
		}
		}
		return true;
	}

	public static final ContentValues getReadContentValues() {
		ContentValues values = new ContentValues();

		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());
		return values;
	}

	public static final ContentValues getUnreadContentValues() {
		ContentValues values = new ContentValues();

		values.putNull(FeedData.EntryColumns.READDATE);
		return values;
	}

	// @Override
	// protected void onListItemClick(ListView listView, View view, int
	// position, long id) {
	// setFeedSortEnabled(false);
	//
	// Intent intent = new Intent(Intent.ACTION_VIEW,
	// FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));
	//
	// intent.putExtra(FeedData.FeedColumns._ID, id);
	// startActivity(intent);
	// }

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
		case DIALOG_ERROR_FEEDIMPORT: {
			dialog = createErrorDialog(R.string.error_feedimport);
			break;
		}
		case DIALOG_ERROR_FEEDEXPORT: {
			dialog = createErrorDialog(R.string.error_feedexport);
			break;
		}
		case DIALOG_ERROR_INVALIDIMPORTFILE: {
			dialog = createErrorDialog(R.string.error_invalidimportfile);
			break;
		}
		case DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE: {
			dialog = createErrorDialog(R.string.error_externalstoragenotavailable);
			break;
		}
		case DIALOG_ABOUT: {
			AlertDialog.Builder builder = new AlertDialog.Builder(RSSOverview.this);

			builder.setIcon(android.R.drawable.ic_dialog_info);
			builder.setTitle(R.string.menu_about);
			// TODO
			// MainTabActivity.INSTANCE.setupLicenseText(builder);
			setupLicenseText(builder);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			builder.setNeutralButton(R.string.changelog, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(Intent.ACTION_VIEW, CANGELOG_URI));
				}
			});
			return builder.create();
		}
		default:
			dialog = null;
		}
		return dialog;
	}

	private Dialog createErrorDialog(int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(RSSOverview.this);

		builder.setMessage(messageId);
		builder.setTitle(R.string.error);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton(android.R.string.ok, null);
		return builder.create();
	}

	private void showDeleteAllEntriesQuestion(final Context context, final Uri uri) {
		// Builder builder = new AlertDialog.Builder(context);
		Builder builder = new AlertDialog.Builder(RSSOverview.this);

		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.contextmenu_deleteallentries);
		builder.setMessage(R.string.question_areyousure);
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new Thread() {
					public void run() {
						FeedData.deletePicturesOfFeed(context, uri, Strings.DB_EXCUDEFAVORITE);
						if (context.getContentResolver().delete(uri, Strings.DB_EXCUDEFAVORITE, null) > 0) {
							context.getContentResolver().notifyChange(FeedData.FeedColumns.CONTENT_URI, null);
						}
					}
				}.start();
			}
		});
		builder.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	private void setFeedSortEnabled(boolean enabled) {
		if (enabled != feedSort) {
			listAdapter.setFeedSortEnabled(enabled);
			feedSort = enabled;
		}
	}

	public void setHomeButtonActive() {
		android.support.v7.app.ActionBar actionBar7 = getSupportActionBar();
		actionBar7.setHomeButtonEnabled(true);
		// durchsichtige Actionbar
		// actionBar7.setBackgroundDrawable(new
		// ColorDrawable(Color.parseColor("#51000000")));

		// Up Button, funkt per Default automatisch
		int flags = 0;
		flags = ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE;
		int change = actionBar7.getDisplayOptions() ^ flags;
		actionBar7.setDisplayOptions(change, flags);
	}

	public void zeigeProgressBar(boolean zeigen) {
		if (zeigen) {
			progressBar.setVisibility(View.VISIBLE);
		} else {
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			zeigeProgressBar(true);
		}
	};

	void setupLicenseText(AlertDialog.Builder builder) {
		View view = getLayoutInflater().inflate(R.layout.license, null);

		final TextView textView = (TextView) view.findViewById(R.id.license_text);

		textView.setTextColor(textView.getTextColors().getDefaultColor()); // disables
																			// color
																			// change
																			// on
																			// selection
		StringBuilder sb = new StringBuilder();
		sb.append("Version: " + Util.getVersionNumber(this) + "\n");
		sb.append("Playstore:\nhttps://play.google.com/store/apps/details?id=de.bernd.shandschuh.sparserss\n\n");
		sb.append(getString(R.string.license_intro)).append(Strings.THREENEWLINES).append(getString(R.string.license));
		textView.setText(sb);

		final TextView contributorsTextView = (TextView) view.findViewById(R.id.contributors_togglebutton);

		contributorsTextView.setOnClickListener(new OnClickListener() {
			boolean showingLicense = true;

			@Override
			public void onClick(View view) {
				if (showingLicense) {
					textView.setText(R.string.contributors_list);
					contributorsTextView.setText(R.string.license_word);
				} else {
					textView.setText(new StringBuilder(getString(R.string.license_intro)).append(Strings.THREENEWLINES)
							.append(getString(R.string.license)));
					contributorsTextView.setText(R.string.contributors);
				}
				showingLicense = !showingLicense;
			}

		});
		builder.setView(view);
	}

	public void clickShowAll(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI);
		intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true); // ?
		startActivity(intent);
	}

	public void clickShowFav(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI);
		intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true); // ?
		startActivity(intent);
	}

	
	public void selectItem(int position) {
		mDrawerList.setItemChecked(position, true);
		mDrawerLayout.closeDrawer(mDrawerList);
		NavDrawerLineEntry navDrawerLineEntry = (NavDrawerLineEntry) mDrawerList.getAdapter().getItem(position);
		if (navDrawerLineEntry == null) {
			Util.toastMessageLong(this, "navDrawerLineEntry is Empty for position " + position);
			return;
		}
		switch (navDrawerLineEntry.ID) {
		case R.id.cancel_action:
			// do nothing
			break;
			
		case R.id.menu_overview:{
			// do nothing
			break;
		}
		case R.id.menu_alle:{
			clickShowAll(null);
			break;
		}
		case R.id.menu_favorites:{
			clickShowFav(null);
			break;
		}

		default:
			Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI(Integer.toString(navDrawerLineEntry.ID)));
			long longID=navDrawerLineEntry.ID;
			intent.putExtra(FeedData.FeedColumns._ID, longID);
			startActivity(intent);
			break;
		}
	}

	private DrawerLayout mDrawerLayout;
	protected ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;


	private void setupDrawerContent() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_main);
		mDrawerList = (ListView) findViewById(R.id.navigation_drawer_left_main);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerList.setAdapter(new NavigationDrawerAdapter(this));
		
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				 selectItem(position);				
			}
		});
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			mDrawerLayout.closeDrawers();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	
//	// für Hamburger Home Icon
//	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
//	protected Context getActionBarThemedContextCompat() {
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//			return getSupportActionBar().getThemedContext();
//		} else {
//			return this;
//		}
//	}

}
