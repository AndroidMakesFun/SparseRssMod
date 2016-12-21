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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import com.bumptech.glide.Glide;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import de.bernd.shandschuh.sparserss.EntryPagerAdapter.DtoEntry;
import de.bernd.shandschuh.sparserss.handler.PictureFilenameFilter;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

public class EntryActivity extends AppCompatActivity implements android.widget.SeekBar.OnSeekBarChangeListener {

	public static final int AUFRUFART_FEED = 0;
	public static final int AUFRUFART_BROWSER = 1;
	public static final int AUFRUFART_MOBILIZE = 2;
	public static final int AUFRUFART_INSTAPAPER = 3;
	public static final int AUFRUFART_READABILITY = 4;
	public static final int AUFRUFART_AMP = 5;
	private static final int AUFRUFART_WEBVIEW = 6; // Leiche ?

	private int mAufrufart = 0;
	private EntryActivity mActivity = null;

	EntryPagerAdapter mEntryPagerAdapter;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Util.isLightTheme(this)) {
			// setTheme(R.style.Theme_Light);
			setTheme(R.style.MyTheme_Light);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry);
		mActivity = this;
		
		Uri mUri = mActivity.getIntent().getData();
		
		String sFeedId=mUri.getPath();
		int pos=sFeedId.indexOf("/feeds/");
		pos+=7;
		int ende=sFeedId.indexOf("/", pos);
		sFeedId=sFeedId.substring(pos, ende);
		feedId = Integer.parseInt(sFeedId);

		mAufrufart = getIntent().getIntExtra(EntriesListActivity.EXTRA_AUFRUFART, 0);
		mAufrufart = Util.getViewerPrefs(mActivity, sFeedId);
		int anzahlFeedeintraege = getIntent().getIntExtra(EntriesListActivity.EXTRA_ANZAHL, 1);
		int positionInListe = getIntent().getIntExtra(EntriesListActivity.EXTRA_POSITION, 0);


//		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//		if(toolbar==null){
//			System.out.println("toolbar ist null!!");
//		}
//		setSupportActionBar(toolbar);
//		setHomeButtonActive();
		
		SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		mIntScalePercent = prefs.getInt(PREFERENCE_SCALE + mAufrufart, 50);

		final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		mEntryPagerAdapter = new EntryPagerAdapter(this,positionInListe, anzahlFeedeintraege);
		viewPager.setAdapter(mEntryPagerAdapter);
		viewPager.setCurrentItem(positionInListe, true);
		
		if(Util.isLightTheme(mActivity)){
			viewPager.setBackgroundColor( Color.parseColor("#f6f6f6"));  // Grau Weiss des CSS
		}else{
			viewPager.setBackgroundColor(Color.BLACK);
		}

	}

	/*
	 * private static final String NEWLINE = "\n";
	 * 
	 * private static final String BR = "<br/>";
	 */

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
	private static final String TEXT_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#000000" : "#C0C0C0";
	private static final String BACKGROUND_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#f6f6f6" : "#000000";
	private static final String QUOTE_LEFT_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#a6a6a6" : "#686b6f";
	private static final String QUOTE_BACKGROUND_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#e6e6e6"
			: "#383b3f";
	private static final String SUBTITLE_BORDER_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "solid #ddd"
			: "solid #303030";
	private static final String SUBTITLE_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#666666" : "#8c8c8c";
	private static final String BUTTON_COLOR = Util.isLightTheme(RSSOverview.INSTANCE) ? "#52A7DF" : "#1A5A81";

	public static final String CSS = "<head><style type='text/css'> " + "body {max-width: 100%; margin: 0.3cm; "
			+ FONT_SANS_SERIF + " color: " + TEXT_COLOR + "; background-color:" + BACKGROUND_COLOR
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
	
	public static String getCSS(){
		return CSS;
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

	private View content;

	private SharedPreferences preferences;

	private boolean localPictures;

	private TextView titleTextView;

	private long timestamp;
	private String abstractText;

	private boolean isFirstEntry = true;


	// @Override
	protected void onCreate_2(Bundle savedInstanceState) {
		if (Util.isLightTheme(this)) {
			setTheme(R.style.Theme_Light);
		}
		mAufrufart = getIntent().getIntExtra(EntriesListActivity.EXTRA_AUFRUFART, 0);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.entry);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
		appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
		imageView = (ImageView) findViewById(R.id.backdrop);

		setSupportActionBar(toolbar);
		setHomeButtonActive();

		progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
		zeigeProgressBar(true);

		mActivity = this;

		int titleId = -1;

		// canShowIcon = requestWindowFeature(Window.FEATURE_LEFT_ICON);
		canShowIcon = false;

		titleId = android.R.id.title;

		try {
			titleTextView = (TextView) findViewById(titleId);
			titleTextView.setSingleLine(true);
			titleTextView.setHorizontallyScrolling(true);
			titleTextView.setMarqueeRepeatLimit(1);
			titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			titleTextView.setFocusable(true);
			titleTextView.setFocusableInTouchMode(true);
		} catch (Exception e) {
			// just in case for non standard android, nullpointer etc
		}

		uri = getIntent().getData();
		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
		showRead = getIntent().getBooleanExtra(EntriesListActivity.EXTRA_SHOWREAD, true);
		iconBytes = getIntent().getByteArrayExtra(FeedData.FeedColumns.ICON);
		feedId = 0;

		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);

		titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		feedIdPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FEED_ID);
		favoritePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FAVORITE);
		readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);

		enclosurePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ENCLOSURE);
		authorPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.AUTHOR);

		if (entryCursor.moveToFirst()) {
			link = entryCursor.getString(linkPosition);
			link = fixLink(link);
			timestamp = entryCursor.getLong(datePosition);
			abstractText = entryCursor.getString(abstractPosition);
			feedId = entryCursor.getInt(feedIdPosition); // bah

			// hierher kopiert - tilte immer ermitteln
			Date date = new Date(timestamp);

			StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(this).format(date)).append(' ')
					.append(DateFormat.getTimeFormat(this).format(date));

			String txtTitel = entryCursor.getString(titlePosition);
//			((TextView) findViewById(R.id.entry_date)).setText(txtTitel + "  " + dateStringBuilder);
		}

		entryCursor.close();
		if (RSSOverview.notificationManager == null) {
			RSSOverview.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}

		nextButton = (ImageButton) findViewById(R.id.next_button);
		markAsReadButton = (ImageButton) findViewById(R.id.menu_markasread2);
		markAsReadButton.setAlpha(BUTTON_ALPHA);
		urlButton = (ImageButton) findViewById(R.id.url_button);
		urlButton.setAlpha(BUTTON_ALPHA + 30);
		previousButton = (ImageButton) findViewById(R.id.prev_button);

//		nestedScrollView = (View) findViewById(R.id.nested_scroll_view);

		webView = new WebView(this);

		OnKeyListener onKeyEventListener = new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == 92 || keyCode == 94) {
						scrollUp();
						return true;
					} else if (keyCode == 93 || keyCode == 95) {
						scrollDown();
						return true;
					}
				}
				return false;
			}
		};
		webView.setOnKeyListener(onKeyEventListener);

		content = findViewById(R.id.entry_content);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		final boolean gestures = preferences.getBoolean(Strings.SETTINGS_GESTURESENABLED, true);

		scrollX = 0;
		scrollY = 0;

		SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		mIntScalePercent = prefs.getInt(PREFERENCE_SCALE + mAufrufart, 50);

		setZoomsScale(null);
		MyWebViewClient myWebViewClient = new MyWebViewClient();

		webView.setWebViewClient(myWebViewClient);

		// 1 Browser schon aus Liste herraus
		if (mAufrufart == AUFRUFART_FEED) {
			reload();
		} else if (mAufrufart == AUFRUFART_MOBILIZE) {
			loadMoblize();
		} else if (mAufrufart == AUFRUFART_INSTAPAPER) {
			onClickInstapaper(null);
		} else if (mAufrufart == AUFRUFART_READABILITY) {
			loadReadability();
		} else if (mAufrufart == AUFRUFART_WEBVIEW) {
			loadWebview(null);
		} else if (mAufrufart == AUFRUFART_AMP) {
			onClickLoadAmp(null);
		}

		markAsReadButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		isFirstEntry = true;
	}// onCreate


	public void loadWebview(View view) {
		zeigeProgressBar(true);
		webView.loadUrl(link);
	}


	private void loadReadability() {
		zeigeProgressBar(true);
		// webView.loadUrl("http://www.readability.com/m?url=" + link);
		new AsyncNewReadability().execute();
	}

	private void loadMoblize() {
		zeigeProgressBar(true);
		readUrl();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		webView.restoreState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (RSSOverview.notificationManager != null) {
			RSSOverview.notificationManager.cancel(0);
		}
//		uri = getIntent().getData();
//		parentUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
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

	private void reload() {
		if (_id != null && _id.equals(uri.getLastPathSegment())) {
			return;
		}

		_id = uri.getLastPathSegment();

		ContentValues values = new ContentValues();

		values.put(FeedData.EntryColumns.READDATE, System.currentTimeMillis());

		Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);

		if (entryCursor.moveToFirst()) {
			String abstractText = entryCursor.getString(abstractPosition);

			if (entryCursor.isNull(readDatePosition)) {
				getContentResolver().update(uri, values,
						new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);
			}
			if (abstractText == null) {
				String link = entryCursor.getString(linkPosition);

				entryCursor.close();
				finish();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));

			} else {
				String txtTitel = entryCursor.getString(titlePosition);
				// setTitle(txtTitel);
				if (titleTextView != null) {
					titleTextView.requestFocus(); // restart ellipsize
				}

				int _feedId = entryCursor.getInt(feedIdPosition);

				if (feedId != _feedId) {
					if (feedId != 0) {
						iconBytes = null; // triggers re-fetch of the icon
					}
					feedId = _feedId;
				}

				if (canShowIcon) {
					if (iconBytes == null || iconBytes.length == 0) {
						Cursor iconCursor = getContentResolver().query(
								FeedData.FeedColumns.CONTENT_URI(Integer.toString(feedId)),
								new String[] { FeedData.FeedColumns._ID, FeedData.FeedColumns.ICON }, null, null, null);

						if (iconCursor.moveToFirst()) {
							iconBytes = iconCursor.getBlob(1);
						}
						iconCursor.close();
					}

					if (iconBytes != null && iconBytes.length > 0) {
						int bitmapSizeInDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f,
								getResources().getDisplayMetrics());
						Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
						if (bitmap != null) {
							if (bitmap.getHeight() != bitmapSizeInDip) {
								bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
							}
							// setFeatureDrawable(Window.FEATURE_LEFT_ICON, new
							// BitmapDrawable(bitmap));
							// getSupportActionBar().setIcon(new
							// BitmapDrawable(bitmap));
							// getSupportActionBar().setLogo(new
							// BitmapDrawable(bitmap));
							// int flags = 0;
							// int change =
							// getSupportActionBar().getDisplayOptions() ^
							// flags;
							// getSupportActionBar().setDisplayOptions(change,
							// flags);
						}
					}
				}

				timestamp = entryCursor.getLong(datePosition);

				Date date = new Date(timestamp);

				StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(this).format(date))
						.append(' ').append(DateFormat.getTimeFormat(this).format(date));

				String author = entryCursor.getString(authorPosition);

				if (author != null) {
					dateStringBuilder.append(BRACKET).append(author).append(')');
				}

//				((TextView) findViewById(R.id.entry_date)).setText(txtTitel + "  " + dateStringBuilder);

				favorite = entryCursor.getInt(favoritePosition) == 1;

				// loadData does not recognize the encoding without correct
				// html-header
				localPictures = abstractText.indexOf(Strings.IMAGEID_REPLACEMENT) > -1;

				if (localPictures) {
					abstractText = abstractText.replace(Strings.IMAGEID_REPLACEMENT,
							_id + Strings.IMAGEFILE_IDSEPARATOR);
				}

				if (preferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false)) {
					abstractText = abstractText.replaceAll(Strings.HTML_IMG_REGEX, Strings.EMPTY);
					webView.getSettings().setBlockNetworkImage(true);
				} else {
					if (webView.getSettings().getBlockNetworkImage()) {
						/*
						 * setBlockNetwortImage(false) calls postSync, which
						 * takes time, so we clean up the html first and change
						 * the value afterwards
						 */
						webView.loadData(Strings.EMPTY, TEXT_HTML, UTF8);
						webView.getSettings().setBlockNetworkImage(false);
					}
				}

				boolean keineBilder = true;
				// alt erstmal online _erstes_ Bild
				int posImg = abstractText.indexOf("src=\"");
				if (posImg > 0) {
					posImg += 5;
					int posImgEnde = abstractText.indexOf('"', posImg);
					if (posImgEnde > 0) {
						mNewLink = abstractText.substring(posImg, posImgEnde);
						System.out.println("gliedeHeader:" + mNewLink);
						URL url;
						try {
							url = new URL(mNewLink);
							Glide.with(EntryActivity.this).load(url).centerCrop().into(imageView);
							keineBilder = false;
							;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// sonst ein anderes aus dem Artikel, wenn Bilder geladen
					// wurden...
				} else if (Util.getImageFolderFile(this) != null && Util.getImageFolderFile(this).exists()) {
					PictureFilenameFilter filenameFilter = new PictureFilenameFilter(_id);

					File[] files = Util.getImageFolderFile(this).listFiles(filenameFilter);
					if (files != null && files.length > 0) {
						Glide.with(EntryActivity.this).load(files[0]).centerCrop().into(imageView);
						keineBilder = false;
					}
				}

				if (keineBilder) {
					// System.out.println(appBarLayout.getMinimumHeight());
					// appBarLayout.setMinimumHeight(100);
					// System.out.println(imageView.getMinimumHeight());
					// imageView.setMinimumHeight(2);
				}

				int fontsize = Integer.parseInt(preferences.getString(Strings.SETTINGS_FONTSIZE, Strings.ONE));

				/*
				 * if (abstractText.indexOf('<') > -1 &&
				 * abstractText.indexOf('>') > -1) { abstractText =
				 * abstractText.replace(NEWLINE, BR); }
				 */

				if (Util.isLightTheme(this) || preferences.getBoolean(Strings.SETTINGS_BLACKTEXTONWHITE, false)) {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(
								null, new StringBuilder(CSS).append(FONTSIZE_START).append(fontsize)
										.append(FONTSIZE_MIDDLE).append(abstractText).append(FONTSIZE_END).toString(),
								TEXT_HTML, UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null, new StringBuilder(CSS).append(BODY_START).append(abstractText)
								.append(BODY_END).toString(), TEXT_HTML, UTF8, null);
					}
					webView.setBackgroundColor(Color.WHITE);
					content.setBackgroundColor(Color.WHITE);
				} else {
					if (fontsize > 0) {
						webView.loadDataWithBaseURL(null, new StringBuilder(FONT_FONTSIZE_START).append(fontsize)
								.append(FONTSIZE_MIDDLE).append(abstractText).append(FONT_END).toString(), TEXT_HTML,
								UTF8, null);
					} else {
						webView.loadDataWithBaseURL(null,
								new StringBuilder(FONT_START).append(abstractText).append(BODY_END).toString(),
								TEXT_HTML, UTF8, null);
					}
					webView.setBackgroundColor(Color.BLACK);
					content.setBackgroundColor(Color.BLACK);
				}

				link = entryCursor.getString(linkPosition);
				link = fixLink(link);

				if (link != null && link.length() > 0) {
					urlButton.setEnabled(true);
					urlButton.setAlpha(BUTTON_ALPHA + 20);
					urlButton.setOnClickListener(new OnClickListener() {
						public void onClick(View view) {
							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
						}
					});

				} else {
					urlButton.setEnabled(false);
					urlButton.setAlpha(80);
				}

				setupButton(previousButton, false, timestamp);
				setupButton(nextButton, true, timestamp);
				// webView.scrollTo(scrollX, scrollY); // resets the scrolling
			}
		} else {
			entryCursor.close();
		}

		/*
		 * new Thread() { public void run() { sendBroadcast(new
		 * Intent(Strings.ACTION_UPDATEWIDGET)); // this is slow } }.start();
		 */
	}

	private void setupButton(ImageButton button, final boolean successor, long date) {
		StringBuilder queryString = new StringBuilder(DATE).append(date).append(AND_ID).append(successor ? '>' : '<')
				.append(_id).append(')').append(OR_DATE).append(successor ? '<' : '>').append(date);

		if (!showRead) {
			queryString.append(Strings.DB_AND).append(EntriesListAdapter.READDATEISNULL);
		}

		Cursor cursor = getContentResolver().query(parentUri, new String[] { FeedData.EntryColumns._ID },
				queryString.toString(), null, successor ? DESC : ASC);

		if (cursor.moveToFirst()) {
			button.setEnabled(true);
			button.setAlpha(BUTTON_ALPHA);

			final String id = cursor.getString(0);

			if (successor) {
				_nextId = id;
			} else {
				_previousId = id;
			}
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					if (successor) {
						nextEntry(true);
					} else {
						previousEntry(true);
					}
				}
			});
		} else {
			button.setEnabled(false);
			button.setAlpha(60);
		}
		cursor.close();
	}

	private void nextEntry(boolean animate) {
		isFirstEntry = false;
		mAnimationDirection = android.R.anim.slide_out_right;
		switchEntry(_nextId, animate, Animations.SLIDE_IN_RIGHT, Animations.SLIDE_OUT_LEFT);
	}

	private void previousEntry(boolean animate) {
		isFirstEntry = false;
		mAnimationDirection = android.R.anim.fade_out;
		switchEntry(_previousId, animate, Animations.SLIDE_IN_LEFT, Animations.SLIDE_OUT_RIGHT);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// scrollX = webView.getScrollX();
		// scrollY = webView.getScrollY();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
//		webView.saveState(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		System.out.println("onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.entry, menu);
//		MenuItem markasreadItem = menu.add(0, R.id.menu_markasread, 0, R.string.contextmenu_markasread);
//		MenuItemCompat.setShowAsAction(markasreadItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
//		markasreadItem.setIcon(android.R.drawable.ic_menu_revert);
//
//		MenuItem browserItem = menu.add(0, R.id.url_button, 0, R.string.contextmenu_browser);
//		MenuItemCompat.setShowAsAction(browserItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
//		browserItem.setIcon(android.R.drawable.ic_menu_view);
//
//		MenuItem readabilityItem = menu.add(0, R.id.menu_readability, 0, "Readability");
//		MenuItemCompat.setShowAsAction(readabilityItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
//		readabilityItem.setIcon(R.drawable.readability_400);
//
//		MenuItem feedItem = menu.add(0, R.id.menu_feed, 0, "Feed");
//		MenuItemCompat.setShowAsAction(feedItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
//		feedItem.setIcon(R.drawable.ic_action_crop);

		return true;
	}

	public String bahtml = null;
	public String mNewLink = null;

	private void readUrl() {
		zeigeProgressBar(true);
		new AsyncReadUrl().execute();
	}

	class AsyncReadUrl extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			try {
				long start = System.currentTimeMillis();// 0
				URL url = new URL(link);
				System.out.println("a" + (System.currentTimeMillis() - start));
				// InputStream is = url.openStream();
				URLConnection con = url.openConnection();
				System.out.println("b" + (System.currentTimeMillis() - start));
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF8")); // 950ms
				System.out.println("c" + (System.currentTimeMillis() - start));
				// baseUrl wechselt ab con.getInputStream (open?)
				String baseUrl = con.getURL().getProtocol() + "://" + con.getURL().getHost();
				if (con.getURL().getHost().endsWith("golem.de")) {
					String[] arr = con.getURL().getPath().split("-");
					String newLink = "http://www.golem.de/pda/pda-" + arr[arr.length - 1];
					mNewLink = newLink;
					return null;
				}
				if (con.getURL().getHost().endsWith("heise.de")) {
					String newLink = baseUrl + con.getURL().getPath() + "?view=print";
					mNewLink = newLink;
					return null;
				}
				if (con.getURL().getHost().endsWith("wiwo.de")) {
					String newLink = baseUrl + con.getURL().getPath();
					int pos = newLink.lastIndexOf('/');
					if (pos > 0) {
						newLink = newLink.substring(0, pos) + "/v_detail_tab_print/"
								+ newLink.substring(pos, newLink.length());
					}
					// bufferedReader = new BufferedReader(new
					// InputStreamReader(con.getInputStream(), "UTF8"));
					// webView.loadUrl(newLink);
					// Wiwo print im Browser öffnen
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(newLink)));
					finish();
					return null;
				}

				// BufferedReader bufferedReader = new BufferedReader(new
				// InputStreamReader(is));
				String line = bufferedReader.readLine();
				while (line != null) {
					line = bufferedReader.readLine();
					bahtml += line;
				}
				bufferedReader.close();
				System.out.println("d" + (System.currentTimeMillis() - start));

				// bahtml = Jsoup.clean(bahtml, Whitelist.basicWithImages());
				// String baseUrl = con.getURL().getProtocol() + "://" +
				// con.getURL().getHost();
				System.out.println("e" + (System.currentTimeMillis() - start));

				// long u=System.currentTimeMillis();
				// int pBody=bahtml.indexOf("<body");
				// System.out.println("pBody"+(System.currentTimeMillis() - u));
				// int classCon=bahtml.indexOf("class=\"con\"",pBody);
				// System.out.println("classCon"+(System.currentTimeMillis() -
				// u));
				// bahtml=bahtml.substring(classCon);
				// System.out.println("f0 " + (System.currentTimeMillis() -
				// start));

				Document dirty = Jsoup.parseBodyFragment(bahtml, baseUrl); // 2300ms
																			// !!
				System.out.println("f" + (System.currentTimeMillis() - start));
				Elements elements = dirty.getElementsByClass("content");
				System.out.println("g" + (System.currentTimeMillis() - start));
				if (elements.size() > 0) {
					// dirty auf content setzen, 2.parse
					bahtml = elements.first().html();
					dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
				} else {
					elements = dirty.getElementsByClass("con"); // Tagesschau
					if (elements.size() > 0) {
						bahtml = elements.first().html();
						dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
					} else {
						elements = dirty.getElementsByAttributeValue("id", "content"); // Heise
						if (elements.size() > 0) {
							bahtml = elements.first().html();
							dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
							// } else {
							// elements =
							// dirty.getElementsByClass("hcf-content"); // Wiwo
							// - nope, unsused!!
							// if (elements.size() > 0) {
							// bahtml = elements.first().html();
							// dirty = Jsoup.parseBodyFragment(bahtml, baseUrl);
							// }
						}
					}
				}
				Whitelist whitelist = Whitelist.basic(); // 500ms
				// Whitelist whitelist = Whitelist.basicWithImages(); // 500ms
				whitelist.addTags("h1", "h2", "h3");
				System.out.println("x" + (System.currentTimeMillis() - start));
				Cleaner cleaner2 = new Cleaner(whitelist);
				System.out.println("h" + (System.currentTimeMillis() - start));
				Document clean = cleaner2.clean(dirty);
				System.out.println("i" + (System.currentTimeMillis() - start));
				// clean.body().removeAttr(attributeKey)
				bahtml = clean.body().html();

			} catch (Exception e) {
				e.printStackTrace();
				Util.toastMessageLong(EntryActivity.this, "" + e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (mNewLink != null) {
				webView.loadUrl(mNewLink);
			} else if (bahtml != null) {
				webView.loadData(bahtml, "text/html; charset=UTF-8", null);
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == 92 || keyCode == 94 || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				scrollUp();
				return true;
			} else if (keyCode == 93 || keyCode == 95 || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				scrollDown();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void scrollUp() {
		if (webView != null) {
			webView.pageUp(false);
		}
	}

	private void scrollDown() {
		if (webView != null) {
			webView.pageDown(false);
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

	
	//// Für setZoomsScale und Dlg dafür ////
	
	private int mIntScalePercent = 50; // 0..100

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
				SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(PREFERENCE_SCALE + mAufrufart, mIntScalePercent);
				editor.commit();
			}
		});
		mAlertDialog.show();
	}

	/**
	 * Für onPageFinished um ProzessBar an/aus zu knipsen da webview aSyncron
	 * die animation trasht
	 */
	private class MyWebViewClient extends WebViewClient {

		@Override
		public void onPageFinished(WebView view, String url) {
			if (!isFirstEntry) {
				nestedScrollView.scrollTo(0, 0);
			}
			zeigeProgressBar(false);
			// kopiert
			view.setVisibility(View.VISIBLE);
			super.onPageFinished(view, url);
		};
	}

	int mAnimationDirection = android.R.anim.slide_out_right;

	private ProgressBar progressBar;
	private CollapsingToolbarLayout collapsingToolbar;
	private AppBarLayout appBarLayout;

	private View nestedScrollView;

	private void switchEntry(String id, boolean animate, Animation inAnimation, Animation outAnimation) {

		// webView.setVisibility(View.GONE);
		zeigeProgressBar(true);

		uri = parentUri.buildUpon().appendPath(id).build();
		getIntent().setData(uri);
		scrollX = 0;
		scrollY = 0;

		if (mAufrufart > 0) {
			// link laden
			Cursor entryCursor = getContentResolver().query(uri, null, null, null, null);
			if (entryCursor.moveToFirst()) {
				link = entryCursor.getString(linkPosition);
				link = fixLink(link);
				timestamp = entryCursor.getLong(datePosition);
				abstractText = entryCursor.getString(abstractPosition);

				Date date = new Date(timestamp);
				StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(this).format(date))
						.append(' ').append(DateFormat.getTimeFormat(this).format(date));
				String txtTitel = entryCursor.getString(titlePosition);
//				((TextView) findViewById(R.id.entry_date)).setText(txtTitel + "  " + dateStringBuilder);

			}
			entryCursor.close();

			if (mAufrufart == AUFRUFART_MOBILIZE) {
				loadMoblize();
			} else if (mAufrufart == AUFRUFART_INSTAPAPER) {
				onClickInstapaper(null); 
			} else if (mAufrufart == AUFRUFART_READABILITY) {
				loadReadability();
			}

			// markRead 2
			getContentResolver().update(uri, RSSOverview.getReadContentValues(),
					new StringBuilder(FeedData.EntryColumns.READDATE).append(Strings.DB_ISNULL).toString(), null);

			setupButton(previousButton, false, timestamp);
			setupButton(nextButton, true, timestamp);
		} else {
			reload();
		}

	}

	public void setHomeButtonActive() {
		android.support.v7.app.ActionBar actionBar7 = getSupportActionBar();
		actionBar7.setHomeButtonEnabled(true);
		// durchsichtige Actionbar
		actionBar7.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#51000000")));
		android.app.ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.hide(); // immer weil doppelt...
		}

		// Up Button, funkt per Default automatisch
		int flags = 0;
		flags = ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE;
		int change = actionBar7.getDisplayOptions() ^ flags;
		actionBar7.setDisplayOptions(change, flags);

	}

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
		case R.id.url_button: {
			onClickLoadBrowser(null);
			break;
		}

		case R.id.menu_feed: {
			_id = null;
			Util.setViewerPrefs(this, "" + feedId, AUFRUFART_FEED);
			onClickReload(null);
			// readUrl(); // TODO ???
			break;
		}

//		case R.id.menu_mobilize: {
//			Util.setViewerPrefs(this, "" + feedId, AUFRUFART_MOBILIZE);
////			loadMoblize();
//			break;
//		}

		case R.id.menu_readability: {
			Util.setViewerPrefs(this, "" + feedId, AUFRUFART_READABILITY);
			onClickReadability(null);
			break;
		}

		case R.id.menu_amp: {
			Util.setViewerPrefs(this, "" + feedId, AUFRUFART_AMP);
			onClickLoadAmp(null);
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
		case R.id.menu_text_scale: {
			onClickShowSeekBarDialog(null);
		}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void zeigeProgressBar(boolean zeigen) {
		progressBar = (ProgressBar) findViewById(R.id.progress_spinner);
		if (progressBar == null) {
			System.out.println("progressBar NULL");
			return;
		}

		if (zeigen) {
			progressBar.setVisibility(View.VISIBLE);
		} else {
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	class AsyncNewReadability extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			try {

				HtmlFetcher fetcher2 = new HtmlFetcher();
				fetcher2.setMaxTextLength(50000);
				JResult res = fetcher2.fetchAndExtract(link, 10000, true);
				String text = res.getText();
				String title = res.getTitle();
				String imageUrl = res.getImageUrl();
				// System.out.println("image " + imageUrl);

				// collapsingToolbar.setTitle(title);
				// collapsingToolbar.setTitle("");

				if (imageUrl != null && !"".equals(imageUrl)) {
					mNewLink = imageUrl;
				}

				if (text != null) {
					bahtml = text + "<br>";
				}
				return null;

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (bahtml != null) {

				// Bilder auf 100% runter sizen
				// content is the content of the HTML or XML.
				String stringToAdd = "width=\"100%\" height=\"auto\" ";
				// Create a StringBuilder to insert string in the middle of
				// content.
				StringBuilder sb = new StringBuilder(bahtml);
				int i = 0;
				int cont = 0;
				// Check for the "src" substring, if it exists, take the index
				// where
				// it appears and insert the stringToAdd there, then increment a
				// counter
				// because the string gets altered and you should sum the length
				// of the inserted substring
				while (i != -1) {
					i = bahtml.indexOf("src", i + 1);
					if (i != -1)
						sb.insert(i + (cont * stringToAdd.length()), stringToAdd);
					++cont;
				}
				bahtml = sb.toString();

				webView.loadData(bahtml, "text/html; charset=UTF-8", null);
				// webView.loadDataWithBaseURL(mNewLink, bahtml, "text/html;
				// charset=UTF-8", null, null);

				if (mNewLink != null) {

					URL url;
					try {
						url = new URL(mNewLink);
						Glide.with(EntryActivity.this).load(url).centerCrop().into(imageView);
						;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			} // else bahatml leer - nix machen, ggf. feed (neu) laden ?!
		}
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

	public void onClickMarkAsRead(View view) {
		finish();
	}
	
	public void onClickLoadBrowser(View view) {
		// Browser öffnen
		DtoEntry dtoEntry = mEntryPagerAdapter.getAktuellenEntry();
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(dtoEntry.link)));
	}

	public void onClickReadability(View view) {
		Util.setViewerPrefs(this, "" + feedId, AUFRUFART_READABILITY);
		mAufrufart=AUFRUFART_READABILITY;		
		mEntryPagerAdapter.notifyDataSetChanged();
	}
	
	public void onClickMenu2(View view) {

		if(view==null){
			view=this.getCurrentFocus();
		}
		
		PopupMenu popup = new PopupMenu(EntryActivity.this, view);
		popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
            	EntryActivity.this.onOptionsItemSelected( item);
                return true;
            }
        });
		popup.show();
		
	}

	public void onClickInstapaper(View view) {
		DtoEntry dtoEntry = mEntryPagerAdapter.getAktuellenEntry();
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.instapaper.com/m?u=" + dtoEntry.link)));
	}

	public void onClickLoadAmp(View view) {
		Util.setViewerPrefs(this, "" + feedId, AUFRUFART_AMP);
		mAufrufart=AUFRUFART_AMP;
		mEntryPagerAdapter.notifyDataSetChanged();
	}

	public void onClickReload(View view) {
		Util.setViewerPrefs(this, "" + feedId, AUFRUFART_FEED);
		mAufrufart=AUFRUFART_FEED;
		mEntryPagerAdapter.notifyDataSetChanged();
		
	}

	public void onClickShare(View view) {
		DtoEntry dtoEntry = mEntryPagerAdapter.getAktuellenEntry();
		startActivity(Intent.createChooser(
				new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, dtoEntry.link).setType(TEXTPLAIN),
				getString(R.string.menu_share)));
	}
	
	
	public void setZoomsScale(WebView nWebView) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if(nWebView==null){
				nWebView=(WebView) this.getCurrentFocus();
			}
			nWebView.getSettings().setTextZoom(mIntScalePercent * 2);
		}
	}

	public int getmAufrufart() {
		return mAufrufart;
	}

	public void onClickNext(View view) {
		int aktuellePosition = mEntryPagerAdapter.getAktuellePosition();
		aktuellePosition++;
		if(aktuellePosition==mEntryPagerAdapter.getCount()){
			return;
		}
		ViewPager viewPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(aktuellePosition, true);
	}

	public void onClickPrevious(View view) {
		int aktuellePosition = mEntryPagerAdapter.getAktuellePosition();
		aktuellePosition--;
		if(aktuellePosition<0){
			return;
		}
		ViewPager viewPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(aktuellePosition, true);
	}

}
