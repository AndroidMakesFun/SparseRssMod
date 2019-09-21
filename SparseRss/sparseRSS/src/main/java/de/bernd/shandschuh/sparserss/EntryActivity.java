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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.view.MenuItemCompat;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;

import de.bernd.shandschuh.sparserss.EntryPagerAdapter.DtoEntry;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.provider.FeedDataContentProvider;
import de.jetwick.snacktory.OutputFormatter;

public class EntryActivity extends AppCompatActivity implements android.widget.SeekBar.OnSeekBarChangeListener {

    private static final String TAG = EntryActivity.class.getSimpleName();

    public static final int AUFRUFART_FEED = 0;
    public static final int AUFRUFART_BROWSER = 1;
    public static final int AUFRUFART_MOBILIZE = 2;
    public static final int AUFRUFART_INSTAPAPER = 3;
    public static final int AUFRUFART_READABILITY = 4;
    public static final int AUFRUFART_AMP = 5;
    public static final int AUFRUFART_GOOGLEWEBLIGHT = 6;
    public static final int AUFRUFART_READABILITY4J = 7;

    public static final float NIGHT_ALPHA_FACTOR = 0.6f;

    private int mAufrufart = 0;
    private EntryActivity mActivity = null;

    EntryPagerAdapter mEntryPagerAdapter;

    boolean showPics;    // Prefs Bilder laden und anzeigen -> f?r Adapter
    boolean showCover;    // Prefs Cover laden und anzeigen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.setTheme(this);
        CSS = null;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry);
        mActivity = this;

        Uri mUri = mActivity.getIntent().getData(); // aus EntriesListActivity
        String sFeedId = mUri.getPath();
        int pos = sFeedId.indexOf("/feeds/");
        if (pos > -1) {
            pos += 7;
            int ende = sFeedId.indexOf("/", pos);
            sFeedId = sFeedId.substring(pos, ende);
            feedId = Integer.parseInt(sFeedId);
        } else {
            // Aufruf vom Widget
//			_id = mUri.getLastPathSegment();
//			feedId = getFeedIdZuEntryId(_id);
//			sFeedId=""+feedId;
//			mUri = FeedData.EntryColumns.FULL_CONTENT_URI(sFeedId, _id);

            feedId = 0;    // Default f?r alle
            sFeedId = "0"; // Default f?r alle
        }

        mAufrufart = Util.getViewerPrefs(mActivity, sFeedId);
        int anzahlFeedeintraege = getIntent().getIntExtra(EntriesListActivity.EXTRA_ANZAHL, 1);
        int positionInListe = getIntent().getIntExtra(EntriesListActivity.EXTRA_POSITION, -1);  // !!

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mIntScalePercent = prefs.getInt(PREFERENCE_SCALE, 60);

        // jetzt hier
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        androidx.appcompat.app.ActionBar actionBar7 = getSupportActionBar();
        actionBar7.setDisplayHomeAsUpEnabled(true);

        if (Util.getColorMode(this)==2){
            toolbar.setAlpha(EntryActivity.NIGHT_ALPHA_FACTOR);
        }

        View viewBottomBar = findViewById(R.id.button_layout);
        if (!Util.showBottomBar(this)) {
            viewBottomBar.setVisibility(View.GONE);
        }else{
            viewBottomBar.setAlpha(0.4f);  // etwas durchsichtig
        }

        showPics = Util.showPics(this);
        showCover = Util.showCover(this, "" + feedId);

        mEntryPagerAdapter = new EntryPagerAdapter(this, positionInListe, anzahlFeedeintraege);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(mEntryPagerAdapter);
        if (positionInListe < 0) {
            positionInListe = mEntryPagerAdapter.getAktuellePosition();  // dort neu ermittelt
        }
        viewPager.setCurrentItem(positionInListe, true);
    }

    private static final String TEXT_HTML = "text/html";

    private static final String UTF8 = "utf-8";

    private static final String OR_DATE = " or date ";

    private static final String DATE = "(date=";

    private static final String AND_ID = " and _id";

    private static final String ASC = "date asc, _id desc limit 1";

    private static final String DESC = "date desc, _id asc limit 1";

    // private static final String CSS = "<head><style type=\"text/css\">body
    // {max-width: 100%}\nimg {max-width: 100%; height: auto;}\ndiv[style]
    // {max-width: 100%;}\npre {white-space: pre-wrap;}</style></head>";
    // aus /sparss/src/net/etuldan/sparss/view/EntryView.java
    private static final String FONT_SANS_SERIF = "font-family: sans-serif;";
    public static String BACKGROUND_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#f6f6f6" : "#000000";
    //private static final String QUOTE_LEFT_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#a6a6a6" : "#686b6f";
    //private static final String QUOTE_BACKGROUND_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#e6e6e6" : "#383b3f";
    //private static final String SUBTITLE_BORDER_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "solid #ddd" : "solid #303030";
    //private static final String SUBTITLE_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#666666" : "#8c8c8c";
    //private static final String BUTTON_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#52A7DF" : "#1A5A81";

    private static String CSS = null;

    public static String getCSS() {
        if(CSS==null){
            BACKGROUND_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#f6f6f6" : "#000000";
            String QUOTE_LEFT_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#a6a6a6" : "#686b6f";
            String QUOTE_BACKGROUND_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#e6e6e6" : "#383b3f";
            String SUBTITLE_BORDER_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "solid #ddd" : "solid #303030";
            String SUBTITLE_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#666666" : "#8c8c8c";
            String BUTTON_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#52A7DF" : "#1A5A81";
            CSS="<head><style type='text/css'> " + "body {max-width: 100%; margin: 0.3cm; "
                    + FONT_SANS_SERIF + " color: " + getTextColor() + "; background-color:" + BACKGROUND_COLOR
                    + "; line-height: 150%} " + "* {max-width: 100%; word-break: break-word}"
                    + "h1, h2 {font-weight: normal; line-height: 130%} " + "h1 {font-size: 140%; margin-bottom: 0.1em} "
                    + "h2 {font-size: 120%} " + "a {color: #0099CC}" + "h1 a {color: inherit; text-decoration: none}"
                    + "img {height: auto} " + "pre {white-space: pre-wrap;} " + "blockquote {border-left: thick solid "
                    + QUOTE_LEFT_COLOR + "; background-color:" + QUOTE_BACKGROUND_COLOR
                    + "; margin: 0.5em 0 0.5em 0em; padding: 0.5em} " + "p {margin: 0.8em 0 0.8em 0} " + "p.subtitle {color: "
                    + SUBTITLE_COLOR + "; border-top:1px " + SUBTITLE_BORDER_COLOR + "; border-bottom:1px "
                    + SUBTITLE_BORDER_COLOR + "; padding-top:2px; padding-bottom:2px; font-weight:800 } "
                    + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} "
                    + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} "
                    + "div.button-section {padding: 0.4cm 0; margin: 0; text-align: center} "
                    + ".button-section p {margin: 0.1cm 0 0.2cm 0}" + ".button-section p.marginfix {margin: 0.5cm 0 0.5cm 0}"
                    + ".button-section input, .button-section a {font-family: sans-serif-light; font-size: 100%; color: #FFFFFF; background-color: "
                    + BUTTON_COLOR + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm} "
                    + "</style><meta name='viewport' content='width=device-width, initial-scale=1'/></head>";
        }
        return CSS;
    }

    public static String getTextColor() {
        if(Util.getColorMode(RSSOverview.INSTANCE)==0){
            return "#000000";
        }
        if(Util.getColorMode(RSSOverview.INSTANCE)==1){
            return "#999999"; // colGrey
        }
        return "#737373"; // colDarkGrey
    }

    private static final String FONT_START = CSS + "<body link=\"#97ACE5\" text=\"#C0C0C0\">";

    private static final String FONT_FONTSIZE_START = CSS + "<body link=\"#97ACE5\" text=\"#C0C0C0\"><font size=\"+";

    private static final String FONTSIZE_START = "<font size=\"+";

    private static final String FONTSIZE_MIDDLE = "\">";

    private static final String FONTSIZE_END = "</font>";

    private static final String FONT_END = "</font><br/><br/><br/><br/></body>";

    private static final String BODY_START = "<body>";

    private static final String BODY_END = "<br/><br/><br/><br/></body>";

    private static final int BUTTON_ALPHA = 180;

    private static final String IMAGE_ENCLOSURE = "[@]image/";

    private static final String TEXTPLAIN = "text/plain";

    private static final String BRACKET = " (";

    private int titlePosition;

    private int datePosition;

    private int abstractPosition;

    private int linkPosition;

    private int feedIdPosition;

    private int favoritePosition;

    private int readDatePosition;

    private int enclosurePosition;

    private int authorPosition;

    private String _id;

    private String _nextId;

    private String _previousId;

    private Uri uri;

    private Uri parentUri;

    private int feedId;

    boolean favorite;

    private boolean showRead;

    private boolean canShowIcon;

    private byte[] iconBytes;

    private WebView webView;
    private ImageView imageView;

    private ImageButton nextButton;
    private ImageButton markAsReadButton;

    private ImageButton urlButton;

    private ImageButton previousButton;

    int scrollX;

    int scrollY;

    private String link;

    private LayoutParams layoutParams;

//	private View content;

    private SharedPreferences preferences;

    private boolean localPictures;

    private TextView titleTextView;

    private long timestamp;


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public static String fixLink(String strLink) {
        if (strLink == null)
            return null;
        if (strLink.endsWith("feed/atom/")) {
            strLink = strLink.substring(0, strLink.length() - "feed/atom/".length());
        }
        return strLink;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.entry, menu);

        switch (mAufrufart) {
            case AUFRUFART_READABILITY4J:
                menu.findItem(R.id.menu_readability4j).setChecked(true);
                break;
            case AUFRUFART_READABILITY:
                menu.findItem(R.id.menu_readability).setChecked(true);
                break;
            case AUFRUFART_GOOGLEWEBLIGHT:
                menu.findItem(R.id.menu_googleweblight).setChecked(true);
                break;
            case AUFRUFART_AMP:
                menu.findItem(R.id.menu_amp).setChecked(true);
                break;

            default:
                menu.findItem(R.id.menu_feed).setChecked(true);
                break;
        }

        if (showCover) {
            menu.findItem(R.id.menu_cover).setChecked(true);
        }

        MenuItem addColorMenu = menu.add(0, R.id.menu_color, 0, R.string.menu_color);
        addColorMenu.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        addColorMenu.setIcon(R.drawable.ic_action_brightness_medium);

        MenuItem markasreadItem = menu.add(0, R.id.menu_markasread, 0, R.string.contextmenu_markasread);
        MenuItemCompat.setShowAsAction(markasreadItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        markasreadItem.setIcon(android.R.drawable.ic_menu_revert);

        MenuItem browserItem = menu.add(0, R.id.url_button, 0, R.string.contextmenu_browser);
        MenuItemCompat.setShowAsAction(browserItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        browserItem.setIcon(android.R.drawable.ic_menu_view);

        MenuItem item=menu.findItem(R.id.menu_lighter_darkmode);
        if(Util.isLightTheme(this)){
            item.setVisible(false);
        }else{
            if(Util.getColorMode(this)==2){
                item.setChecked(true);
            }else{
                item.setChecked(false);
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /**
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == 92 || keyCode == 94 || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                scrollUp();
                return true;
            } else if (keyCode == 93 || keyCode == 95 || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                scrollDown();
                return true;
            }
        }
        **/
        return super.onKeyDown(keyCode, event);
    }

    private void scrollUp() {
        NestedScrollView view= (NestedScrollView) mEntryPagerAdapter.getCurrentView();
        if(view!=null){
            view.pageScroll(View.FOCUS_UP);
        }
    }

    private void scrollDown() {
        //mEntryPagerAdapter.getAktuellenEntry().viewWeb.pageDown(true);
        //mEntryPagerAdapter.setPrimaryItem(ViewGroup container, int position, Object object) {
        NestedScrollView view= (NestedScrollView) mEntryPagerAdapter.getCurrentView();
        if(view!=null){
            view.pageScroll(View.FOCUS_DOWN);
        }
    }

    /**
     * Works around android issue 6191
     */
    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            super.unregisterReceiver(receiver);
        } catch (Exception e) {
            // do nothing
        }
    }

    boolean mNavVisible = true;


    //// F?r setZoomsScale und Dlg daf?r ////

    private int mIntScalePercent = 60; // 0..100

    SeekBar mSeekBar;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // progress 1..100
        mIntScalePercent = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setZoomsScale(null);
    }

    AlertDialog.Builder mAlertDialog = null;
    public static final String PREFERENCE_SCALE = "preference_scale_readability";
    public final static String PREFS_NAME = "de.bernd.sparse.rss.preferences";

    // Scale the Text
    public void onClickShowSeekBarDialog(View viewD) {
        View view = getLayoutInflater().inflate(R.layout.entry_seek_bar, null);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(mIntScalePercent);
        mAlertDialog = new AlertDialog.Builder(this);
        mAlertDialog.setTitle("Scale the Text");
        mAlertDialog.setView(view);
        mAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(PREFERENCE_SCALE, mIntScalePercent);
                editor.commit();
                Util.toastMessage(mActivity, "Scale " + (mIntScalePercent * 2));
            }
        });
        mAlertDialog.show();
    }

    int mAnimationDirection = android.R.anim.slide_out_right;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
            case R.id.menu_markasread:
            case R.id.menu_markasread2: {
                finish();
                break;
            }

            case R.id.menu_color: {
                CSS = null;//reset
                String entryid = Util.getLastEntryId(this);
                int feedId = Util.getFeedIdZuEntryId(this, entryid);
                Uri contenUri = FeedData.EntryColumns.FULL_CONTENT_URI("" + feedId, entryid);
                Intent intent = new Intent(Intent.ACTION_VIEW, contenUri);
                RSSOverview.chooseColorDialog(mActivity,intent);
                break;
            }

            case R.id.url_button: {
                onClickLoadBrowser(null);
                break;
            }
            case R.id.menu_browser: {
                onClickMenuBrowser(null);
                break;
            }
            case R.id.menu_chrome: {
                Util.setBrowserPackagePrefs(this,"com.android.chrome");
                onClickLoadBrowser(null);
                break;
            }
            case R.id.menu_default: {
                Util.setBrowserPackagePrefs(this,null);
                onClickLoadBrowser(null);
                break;
            }
            case R.id.menu_firefox_klar: {
                Util.setBrowserPackagePrefs(this,"org.mozilla.klar");
                onClickLoadBrowser(null);
                break;
            }
            case R.id.menu_edge: {
                Util.setBrowserPackagePrefs(this,"com.microsoft.emmx");
                onClickLoadBrowser(null);
                break;
            }

            case R.id.menu_feed: {
                _id = null;
                onClickReload(null);
                // readUrl(); // TODO ???
                break;
            }

            case R.id.menu_mobilize: {
                onClickLoadMobilize(null);
                break;
            }

            case R.id.menu_readability: {
                onClickReadability(null);
                break;
            }
            case R.id.menu_readability4j: {
                onClickReadability4J(null);
                break;
            }

            case R.id.menu_amp: {
                onClickLoadAmp(null);
                break;
            }
            case R.id.menu_googleweblight: {
                onClickLoadGoogleweblight(null);
                break;
            }

            case R.id.menu_copytoclipboard: {
                if (link != null) {
                    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(link);
                }
                break;
            }

            case R.id.menu_share: {
                onClickShare(null);
                break;
            }
            case R.id.menu_share_entry: {
                onClickShareEntry(null);
                break;
            }
            case R.id.menu_share_source: {
                onClickShareSource(null);
                break;
            }
            case R.id.menu_text_scale: {
                onClickShowSeekBarDialog(null);
                break;
            }
            case R.id.menu_cover: {
                if (showCover) {
                    showCover = false;
                    item.setChecked(false);
                    Util.setShowCover(this, "" + feedId, false);
                    mEntryPagerAdapter.notifyDataSetChanged();
                } else {
                    showCover = true;
                    item.setChecked(true);
                    Util.setShowCover(this, "" + feedId, true);
                    mEntryPagerAdapter.notifyDataSetChanged();
                }
                break;
            }
            case R.id.menu_lighter_darkmode:{
                if(item.isChecked()){
                //if(Util.isLighterDarkMode(this)){
                    Log.d(TAG, "isLighterDarkMode to FALSE");
                    Util.setLighterDarkMode(this, false);
                    item.setChecked(false);
                }else{
                    Log.d(TAG, "isLighterDarkMode to TRUE");
                    Util.setLighterDarkMode(this, true);
                    item.setChecked(true);
                }
                CSS=null;
                this.finish();
                break;
            }


        }//switch
        return super.onOptionsItemSelected(item);
    }

    // aus net.etuldan.sparss.utils.NetworkUtils
    public static String getBaseUrl(String link) {
        String baseUrl = link;
        int index = link.indexOf('/', 8); // this also covers https://
        if (index > -1) {
            baseUrl = link.substring(0, index);
        }

        return baseUrl;
    }

    public void clickMarkAsRead(View view) {
        finish();
    }

    public void onClickLoadBrowser(View view) {
        // Browser ?ffnen
        DtoEntry dtoEntry = mEntryPagerAdapter.getAktuellenEntry();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dtoEntry.link));

        String browser=Util.getBrowserPackagePrefs(this);
        //boolean forcechrome = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("forcechrome", true);
        if (browser!=null) {
            intent.setPackage(browser);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(dtoEntry.link));
            startActivity(intent2);
        }
    }

    public void onClickReadability(View view) {
        Util.setViewerPrefs(this, "" + feedId, AUFRUFART_READABILITY);
        resetFulltextForCurrentEntry();
        mAufrufart = AUFRUFART_READABILITY;
        mEntryPagerAdapter.notifyDataSetChanged();
    }

    private void onClickReadability4J(View view) {
        Util.setViewerPrefs(this, "" + feedId, AUFRUFART_READABILITY4J);
        resetFulltextForCurrentEntry();
        mAufrufart = AUFRUFART_READABILITY4J;
        mEntryPagerAdapter.notifyDataSetChanged();
    }


    // PopupMenu aus ButtomBar
    public void onClickMenu2(View view) {

        if (view == null) {
            view = this.getCurrentFocus();
        }

        PopupMenu popup = new PopupMenu(EntryActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        switch (mAufrufart) {
            case AUFRUFART_READABILITY4J:
                popup.getMenu().findItem(R.id.menu_readability4j).setChecked(true);
                break;
            case AUFRUFART_READABILITY:
                popup.getMenu().findItem(R.id.menu_readability).setChecked(true);
                break;
            case AUFRUFART_GOOGLEWEBLIGHT:
                popup.getMenu().findItem(R.id.menu_googleweblight).setChecked(true);
                break;
            case AUFRUFART_AMP:
                popup.getMenu().findItem(R.id.menu_amp).setChecked(true);
                break;

            default:
                popup.getMenu().findItem(R.id.menu_feed).setChecked(true);
                break;
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                EntryActivity.this.onOptionsItemSelected(item);
                return true;
            }
        });
        popup.show();
    }


    /**
     *     Popup Menue for Browser selection
     *     copy from onClickMenu2
     */
    public void onClickMenuBrowser(View view) {

        if (view == null) {
            //view = this.getCurrentFocus();  // aus vorherigem PopupMenu ungueltig!
            if (Util.showBottomBar(this)) {
                //Popup unten bei der ButtomBar
                View viewBottomBar = findViewById(R.id.button_layout);
                view=viewBottomBar;
            }else{
                // Popup oben
                view=mEntryPagerAdapter.getAktuellenEntry().viewWeb;
            }
         }

        PopupMenu popup = new PopupMenu(EntryActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.popup_menu_browser, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                System.out.println("onClickMenuBrowser " + item);
                EntryActivity.this.onOptionsItemSelected(item);
                return true;
            }
        });
        popup.show();
    }

    public void onClickLoadAmp(View view) {
        Util.setViewerPrefs(this, "" + feedId, AUFRUFART_AMP);
        resetFulltextForCurrentEntry();
        mAufrufart = AUFRUFART_AMP;
        mEntryPagerAdapter.notifyDataSetChanged();
    }

    public void onClickLoadGoogleweblight(View view) {
        Util.setViewerPrefs(this, "" + feedId, AUFRUFART_GOOGLEWEBLIGHT);
        resetFulltextForCurrentEntry();
        mAufrufart = AUFRUFART_GOOGLEWEBLIGHT;
        mEntryPagerAdapter.notifyDataSetChanged();
    }

    public void onClickReload(View view) {
        Util.setViewerPrefs(this, "" + feedId, AUFRUFART_FEED);
        mAufrufart = AUFRUFART_FEED;
        mEntryPagerAdapter.notifyDataSetChanged();

    }


    public void onClickShareEntry(View view) {
        try {
            DtoEntry dto = mEntryPagerAdapter.getAktuellenEntry();
            OutputFormatter out = new OutputFormatter();
            Document document = Jsoup.parse(dto.titel + dto.text);
            String share = dto.link + "\n\n" + out.getFormattedText(document);
            document = Jsoup.parse(dto.titel);
            String titel=document.text();
            startActivity(Intent.createChooser(
                    new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_HTML_TEXT, dto.text).putExtra(Intent.EXTRA_SUBJECT, titel).putExtra(Intent.EXTRA_TEXT, share).setType(TEXTPLAIN),
                    getString(R.string.menu_share)));
        } catch (Exception e) {
            e.printStackTrace();
            Util.toastMessage(this,e.toString());
        }
    }

    public void onClickShare(View view) {
        DtoEntry dtoEntry = mEntryPagerAdapter.getAktuellenEntry();
        Document document = Jsoup.parse(dtoEntry.titel);
        String titel=document.text();
        startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, dtoEntry.link).putExtra(Intent.EXTRA_SUBJECT, titel).setType(TEXTPLAIN),
                getString(R.string.menu_share)));
    }

    public void onClickShareSource(View view) {
        try {
            DtoEntry dto = mEntryPagerAdapter.getAktuellenEntry();
            Document document = Jsoup.parse(dto.titel);
            String titel=document.text();
            startActivity(Intent.createChooser(
                    new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_HTML_TEXT, dto.text).putExtra(Intent.EXTRA_SUBJECT, titel).putExtra(Intent.EXTRA_TEXT, dto.text).setType(TEXTPLAIN),
                    getString(R.string.menu_share)));
        } catch (Exception e) {
            e.printStackTrace();
            Util.toastMessage(this,e.toString());
        }
    }


    public void setZoomsScale(WebView nWebView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            try {
                if (nWebView == null) {
                    View view = this.getCurrentFocus();
                    if (view == null || !(view instanceof WebView)) {
                        nWebView = (WebView) view;
                    } else {
                        nWebView = (WebView) this.findViewById(R.id.web_view);
                    }
                }
                nWebView.getSettings().setTextZoom(mIntScalePercent * 2);
            } catch (Exception e) {
                Util.toastMessage(this, "Select WebView");
            }
        }
    }

    public int getmAufrufart() {
        return mAufrufart;
    }

    public void onClickNext(View view) {
        int aktuellePosition = mEntryPagerAdapter.getAktuellePosition();
        aktuellePosition++;
        if (aktuellePosition == mEntryPagerAdapter.getCount()) {
            return;
        }
        ViewPager viewPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
        viewPager.setCurrentItem(aktuellePosition, true);
    }

    public void onClickPrevious(View view) {
        int aktuellePosition = mEntryPagerAdapter.getAktuellePosition();
        aktuellePosition--;
        if (aktuellePosition < 0) {
            return;
        }
        ViewPager viewPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
        viewPager.setCurrentItem(aktuellePosition, true);
    }

    private void onClickLoadMobilize(View view) {
        Util.setViewerPrefs(this, "" + feedId, AUFRUFART_MOBILIZE);
        mAufrufart = AUFRUFART_MOBILIZE;
        mEntryPagerAdapter.notifyDataSetChanged();
    }

    /**
     * ret false wenn !showPics !showCoder !linkGrafilk
     */
    public boolean shouldShowCover(DtoEntry dto) {
        if (!showCover) {
            return false;
        }
        if (dto.linkGrafik != null && !"".equals(dto.linkGrafik)) {
            return true;
        }
        return false;
    }

    /**
     * Fills fulltext in db with null
     */
    public void resetFulltextForCurrentEntry(){

        Uri uri= FeedData.EntryColumns.PARENT_URI(mActivity.getIntent().getData().getPath());
        DtoEntry dto=mEntryPagerAdapter.getAktuellenEntry();
        ContentValues values = FeedDataContentProvider.createContentValuesForFulltext(null, null);
        this.getContentResolver().update(ContentUris.withAppendedId(uri, Long.parseLong( dto.id)),
                values, null, null);
        dto.text=null;
        dto.linkGrafik=null;
        String mImageFolder = Util.getImageFolderFile(mActivity).toString();
        String pathToImage = mImageFolder + "/" + dto.id + "_cover.jpg";
        File imageFile = new File(pathToImage);
        if(imageFile.exists()){
            imageFile.delete();
        }
    }
}
