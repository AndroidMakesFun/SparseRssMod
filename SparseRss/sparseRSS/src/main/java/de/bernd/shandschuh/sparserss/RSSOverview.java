/**
 * Sparse rss
 * <p>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.bernd.shandschuh.sparserss;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FilenameFilter;

import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.provider.OPML;
import de.bernd.shandschuh.sparserss.service.RssJobService;
import de.bernd.shandschuh.sparserss.util.NavigationDrawerAdapter;
import de.bernd.shandschuh.sparserss.util.NavigationDrawerAdapter.NavDrawerLineEntry;

/**
 * Main Class
 */
public class RSSOverview<onRequestPermissionsResult> extends AppCompatActivity {
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

    private static final Uri CANGELOG_URI = Uri.parse("https://github.com/AndroidMakesFun/SparseRssMod/blob/master/SparseRss/README.md");

    boolean feedSort;

    private RSSOverviewListAdapter listAdapter;
    private ListView listview;
    private TextView emptyview;
    private ProgressBar progressBar;

    public static RSSOverview INSTANCE;

    private static final String TAG = RSSOverview.class.getSimpleName();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        INSTANCE = this;
        Util.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_grey600_36dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        int col = 0xFF737373;
        toolbar.setTitleTextColor(col);

        if (Util.getColorMode(this)==2){
            toolbar.setAlpha(EntryActivity.NIGHT_ALPHA_FACTOR);
        }

        setupDrawerContent();

        // setHomeButtonActive();
        progressBar = (ProgressBar) findViewById(R.id.progress_spinner);

        mServiceComponent = new ComponentName(this, RssJobService.class);

        listview = (ListView) findViewById(android.R.id.list);
        listAdapter = new RSSOverviewListAdapter(this);
        listview.setAdapter(listAdapter);

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
                    Intent intent = new Intent(getApplicationContext(), EntriesListActivity.class);
                    intent.setData(FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));
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

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHONPENENABLED, false)) {
            Util.scheduleJob(this, false);
        } else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHENABLED, false)) {
            Util.scheduleJob(this, true);
        }

    } // onCreate

    @Override
    protected void onResume() {
        //setColorMode(this);
        super.onResume();

        zeigeProgressBar(Util.isCurrentlyRefreshing(this));
        registerReceiver(refreshReceiver, new IntentFilter("de.bernd.shandschuh.sparserss.REFRESH"));

        if (RssJobService.mNotificationManagerCompat != null) {
            RssJobService.mNotificationManagerCompat.cancelAll();
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(refreshReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Menues in die Toolbar, SHOW_AS_ACTION_ALWAYS zieht nur hier
        MenuItem addColorMenu = menu.add(0, R.id.menu_color, 0, R.string.menu_color);
        addColorMenu.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        addColorMenu.setIcon(R.drawable.ic_action_brightness_medium);

        /**
         SubMenu subMenu=menu.addSubMenu( 0, R.id.menu_color_mode, 0, "SubMenu");
         subMenu.add(0,R.id.menu_light_mode,1, "Light Mode");
         subMenu.add(0,R.id.menu_dark_mode,1, "Dark Mode");
         subMenu.add(0,R.id.menu_night_mode,1, "Night Mode");
         //subMenu.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
         subMenu.setIcon(android.R.drawable.ic_menu_compass);
         **/

        MenuItem addFeedMenu = menu.add(0, R.id.menu_addfeed, 0, R.string.menu_addfeed);
        addFeedMenu.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        addFeedMenu.setIcon(android.R.drawable.ic_menu_add);

        MenuItem refreshMenu = menu.add(0, R.id.menu_refresh, 0, R.string.menu_refresh);
        refreshMenu.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
        return onOptionsItemSelected(item);
    }

    private int mJobId = 1;
    private ComponentName mServiceComponent;

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

            case R.id.menu_color: {
                Intent intent = new Intent(INSTANCE, RSSOverview.class);
                chooseColorDialog(INSTANCE,intent );
                break;
            }

            case R.id.menu_refresh: {

                refreshAllFeeds();
                break;
            }
            case CONTEXTMENU_EDIT_ID: {
                startActivity(new Intent(Intent.ACTION_EDIT).setData(
                        FeedData.FeedColumns.CONTENT_URI(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)));
                break;
            }
            case CONTEXTMENU_REFRESH_ID: {

                // TODO WLAN an/aus auswerten/berücksichtigen

                final String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                        Context.CONNECTIVITY_SERVICE);

                final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {

                    JobInfo.Builder jobBuilder = new JobInfo.Builder(mJobId, mServiceComponent);

                    //boolean requiresUnmetered = mWiFiConnectivityRadioButton.isChecked();
                    //boolean requiresAnyConnectivity = mAnyConnectivityRadioButton.isChecked();
                    //if (requiresUnmetered) {
                    //	builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
                    //} else if (requiresAnyConnectivity) {
                    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                    //}

                    PersistableBundle extras = new PersistableBundle();
                    extras.putString(Strings.FEEDID, id);

                    jobBuilder.setExtras(extras);

                    // Schedule job
                    Log.d(TAG, "Scheduling job");
                    JobScheduler tm = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    tm.schedule(jobBuilder.build());

                }
                break;
            }
            case CONTEXTMENU_DELETE_ID: {
                String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);

                Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(id),
                        new String[]{FeedData.FeedColumns.NAME}, null, null, null);

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
                                sendBroadcast(new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE));
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
            case R.id.menu_load_last: {
                String entryid = Util.getLastEntryId(this);
                if (entryid == null) {
                    Util.toastMessage(this, "No Last Entry");
                    return true;
                }
                int feedId = Util.getFeedIdZuEntryId(this, entryid);
                Uri contenUri = FeedData.EntryColumns.FULL_CONTENT_URI("" + feedId, entryid);
                //mit content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/0/entries/134522
                startActivity(new Intent(Intent.ACTION_VIEW, contenUri));
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

                try {

                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                        String title = "Download Folder";  //+this.getExternalFilesDir("rss");
                        builder.setTitle(title);

                        final String[] fileNames = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).list(new FilenameFilter() {
                            public boolean accept(File dir, String filename) {
                                return new File(dir, filename).isFile();
                            }
                        });

                        final RSSOverview rssOverview = this;
                        builder.setItems(fileNames, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    OPML.importFromFile(
                                            new StringBuilder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString())
                                                    .append(File.separator).append(fileNames[which]).toString(),
                                            RSSOverview.this);
                                } catch (Exception e) {
                                    showDialog(DIALOG_ERROR_FEEDIMPORT);
                                }
                            }
                        });
                        if (fileNames.length == 0) {
                            Util.msgBox(RSSOverview.this, "Empty:\n" + title);
                        } else {
                            builder.show();
                        }

                    } else {
                        requestPermission();
                    }

                } catch (Exception e) {
                    showDialog(DIALOG_ERROR_FEEDIMPORT);
                }
                break;
            }
            case R.id.menu_export: {
                try {

                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        String folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                        String filename = new StringBuilder(folder).append("/sparse_rss_").append(System.currentTimeMillis())
                                .append(".opml").toString();

                        OPML.exportToFile(filename, this);
                        Util.msgBox(this, String.format(getString(R.string.message_exportedto), filename));
                    } else {
                        requestPermission();
                    }

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
            case R.id.menu_alle_top_feeds: {
                clickShowAlleTopFeads(null);
                break;
            }
            case R.id.menu_alle_offline: {
                clickShowOffline(null);
                break;
            }
            case R.id.menu_favorites: {
                clickShowFav(null);
                break;
            }
        }
        return true;
    }


    private static final int PERMISSION_REQUEST = 0;

    private void requestPermission() {

//		ActivityCompat.requestPermissions(this,
//				new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
//				PERMISSION_REQUEST);

        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(listview, R.string.message_permission_download_folder,
                    Snackbar.LENGTH_INDEFINITE).setAction("ok", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(RSSOverview.INSTANCE,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST);
                }
            }).show();

        } else {
            Snackbar.make(listview, R.string.message_permission_download_folder, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(RSSOverview.INSTANCE,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        }

    }

    /**
     // access Downloads
     @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
     switch (requestCode) {
     case Manifest.:
     if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
     //Granted.


     } else {
     //Denied.
     }
     break;
     }
     }
     **/

    private void refreshAllFeeds() {

        //boolean reShed=PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_REFRESHENABLED, false);
        //Util.scheduleJob(this, reShed);

        Util.scheduleJob(this, false);
        /**
         JobInfo.Builder jobBuilder = new JobInfo.Builder(mJobId, mServiceComponent);
         jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
         PersistableBundle extras = new PersistableBundle();
         //extras.putString(Strings.FEEDID, id);
         jobBuilder.setExtras(extras);

         // Schedule job
         Log.d(TAG, "refreshAllFeeds Scheduling job");
         JobScheduler tm = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
         tm.schedule(jobBuilder.build());

         // Sched wieder anwerfen
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RSSOverview.this);
         Log.d(TAG, "refreshAllFeeds Scheduling job BOOL " + prefs.getBoolean(Strings.SETTINGS_REFRESHENABLED, false));
         if (prefs.getBoolean(Strings.SETTINGS_REFRESHENABLED, false)) {
         Util.scheduleJob(this);
         }
         */

        //new Thread() {
        //	public void run() {
        //		sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY,
        //				PreferenceManager.getDefaultSharedPreferences(RSSOverview.this)
        //						.getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false)));
        //	}
        //}.start();
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
        ActionBar actionBar7 = getSupportActionBar();
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
            Log.d(TAG, "zeigeProgressBar: true");
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            Log.d(TAG, "zeigeProgressBar: false");
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
        intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true); // f?r icon
        intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDFILTER, EntriesListActivity.EXTRA_FILTER_ALL);
        startActivity(intent);
    }

    private void clickShowAlleTopFeads(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI);
        intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true);
        intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDFILTER, EntriesListActivity.EXTRA_FILTER_TOP_FEEDS);
        startActivity(intent);
    }

    public void clickShowOffline(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI);
        intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true);
        intent.putExtra(EntriesListActivity.EXTRA_SHOWFEEDFILTER, EntriesListActivity.EXTRA_FILTER_OFFLINE);
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
//		case R.id.cancel_action:
//			// do nothing
//			break;

            case R.id.menu_overview: {
                // do nothing
                break;
            }
            case R.id.menu_alle: {
                clickShowAll(null);
                break;
            }
            case R.id.menu_alle_top_feeds: {
                clickShowAlleTopFeads(null);
                break;
            }
            case R.id.menu_alle_offline: {
                clickShowOffline(null);
                break;
            }

            case R.id.menu_favorites: {
                clickShowFav(null);
                break;
            }

            default:
                Intent intent = new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI(Integer.toString(navDrawerLineEntry.ID)));
                long longID = navDrawerLineEntry.ID;
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
                if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                    mDrawerLayout.closeDrawer(mDrawerList);
                } else {
                    finish();
                }
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // menu / feedoverview.xml android:onClick="clickJobs"
    public void clickJobs(MenuItem menu) {
        Log.d(TAG, "clickJobs");
        Util.jobInfos(this);
    }

    static android.app.AlertDialog dlgColor=null;

    public static void chooseColorDialog(Activity activity, Intent intent) {

        if(activity.isDestroyed() || activity.isFinishing()){
            Util.toastMessage(RSSOverview.INSTANCE,"Activity is Dead");
            System.exit(0);
            return;
        }
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(activity);
        alert.setTitle("Choose color");
        String[] strAction = new String[]{"Light Mode", "Dark Mode","Night Mode"};
        int colMode=Util.getColorMode(activity);
        alert.setSingleChoiceItems(strAction,colMode, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 1:
                        //dark
                        Util.setColorMode(activity,1);
                        break;

                    case 2:
                        //night
                        Util.setColorMode(activity,2);
                        break;

                    default:
                        //light
                        Util.setColorMode(activity,0);
                        break;
                }
                dlgColor.dismiss();
                activity.finish();
                activity.startActivity(intent);
            }
        });
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dlgColor =alert.show();
    }

}
