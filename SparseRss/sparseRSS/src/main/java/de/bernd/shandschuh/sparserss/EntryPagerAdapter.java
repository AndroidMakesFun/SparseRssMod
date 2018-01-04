package de.bernd.shandschuh.sparserss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.text.format.DateFormat;
import android.util.Xml.Encoding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.bernd.shandschuh.sparserss.provider.FeedDataContentProvider;
import de.bernd.shandschuh.sparserss.service.FetcherService;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

public class EntryPagerAdapter extends PagerAdapter {

	final private EntryActivity mContext;
	private int mAktuellePosition = -1;
	private int mAnzahlFeedeintraege;

	private DtoEntry mDtoEntry;

	/**
	 * content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/2/entries
	 * /8242 alle:
	 * content://de.bernd.shandschuh.sparserss.provider.FeedData/entries/458
	 */
	Uri mUri;

	/**
	 * content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/2/entries
	 * alle: content://de.bernd.shandschuh.sparserss.provider.FeedData/entries
	 */
	Uri mParentUri;

	private ArrayList<String> mListeIdsAsString = new ArrayList<String>();

	// private boolean showRead; // EntriesListActivity gelesene anzeigen
	private String sortOrder = "date DESC";
	private int startPosition = -1;
	private String mSelectionFilter = null;

	class DtoEntry {
		String id;
		String link;
		String linkGrafik;
		String linkAmp;
		String titel;
		String text;
		boolean isRead;
		boolean isFulltext;
		TextView viewTitel;
		WebView viewWeb;
		ImageView viewImage;
		ProgressBar progressBar;
	}

	public EntryPagerAdapter(EntryActivity context, int position, int anzahlFeedeintraege) {
		mContext = context;
		mAnzahlFeedeintraege = anzahlFeedeintraege;
		mUri = mContext.getIntent().getData();
		// showRead =
		// mContext.getIntent().getBooleanExtra(EntriesListActivity.EXTRA_SHOWREAD,
		// true);
		mSelectionFilter = mContext.getIntent().getStringExtra(EntriesListActivity.EXTRA_SELECTION_FILTER);
		startPosition = position;

		boolean bPriorize = PreferenceManager.getDefaultSharedPreferences(mContext)
				.getBoolean(Strings.SETTINGS_PRIORITIZE, false);
		if (bPriorize) {
			sortOrder = "length(readdate) ASC, date DESC";
		}

		mParentUri = FeedData.EntryColumns.PARENT_URI(mUri.getPath());

		ermittleAlleIds(); // setzt mAnzahlFeedeintraege auf
							// mListeIdsAsString.size()

		// Position immer aus DB neu ermitteln
		String id = mUri.getLastPathSegment();
		// int position2=ermittlePositionZuIdInDb(id); //oder aus Array ???
		int position2 = ermittlePositionZuIdInArray(id); // aus Array ! Faster
		if (position != position2) {
			position = position2;
		}
		setAktuellePosition(position);
	}

	@Override
	public Object instantiateItem(ViewGroup collection, int position) {

		LayoutInflater inflater = LayoutInflater.from(mContext);
		ViewGroup layout;
		layout = (ViewGroup) inflater.inflate(R.layout.entry_pager, collection, false);

		DtoEntry dtoEntry = ladeDtoEntry(position);
		if (dtoEntry == null) {
			dtoEntry = new DtoEntry();
			dtoEntry.titel = "No Entry on Position " + position;
			dtoEntry.link = "";
			dtoEntry.text = "";
		}
		if (getAktuellePosition() == position) {
			mDtoEntry = dtoEntry;
		}

		Drawable drawable = getDrawableForEntry(dtoEntry);
		android.support.v7.app.ActionBar actionBar7 = mContext.getSupportActionBar();
		actionBar7.setHomeAsUpIndicator(drawable);

		refreshLayout(dtoEntry, layout);
		collection.addView(layout);
		return layout;
	}

	// Ermittlung der aktuell angezeigten Position
	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {

		int lastPosition = getAktuellePosition();
		if (getAktuellePosition() == position && startPosition != position) {
			// && startPosition!=position wegen startPosition gleich auf gelesen
			// setzen
			return;
		}
		setAktuellePosition(position);
		DtoEntry dto;
		if (lastPosition != position) {
			// erst ab dem 2. durchlauf/Entry
			dto = ladeDtoEntry(position);
			mDtoEntry = dto;
		} else {
			dto = mDtoEntry;
		}

		String id = dto.id;
		if (!dto.isRead) {
			mContext.getContentResolver().update(ContentUris.withAppendedId(mParentUri, Long.parseLong(id)),
					RSSOverview.getReadContentValues(), null, null);
		}
	}

	/**
	 * ermittelt position in allen entries und Anzahl Aller Entries
	 */
	public int ermittlePositionZuIdInDb(String id) {
		if (id == null) {
			System.err.println("ermittlePositionZuId id is null");
			return -1;
		}
		Cursor entryCursor;

		// if(showRead){
		// entryCursor = mContext.getContentResolver().query(mParentUri, null,
		// null, null, sortOrder);
		// }else{
		// entryCursor = mContext.getContentResolver().query(mParentUri, null,
		// EntriesListAdapter.READDATEISNULL, null, sortOrder);
		// }
		entryCursor = mContext.getContentResolver().query(mParentUri, null, mSelectionFilter, null, sortOrder);

		int position = -1;
		if (entryCursor.moveToFirst()) {
			// mAnzahlFeedeintraege=entryCursor.getCount();
			while (entryCursor.isAfterLast() == false) {
				++position;
				final String idEntry = entryCursor.getString(0);
				if (id.equals(idEntry)) {
					break;
				}
				entryCursor.moveToNext();
			}
			entryCursor.close();
		}
		return position;
	}

	public DtoEntry ladeDtoEntry(int position) {

		if (position < 0) {
			return null;
		}

		Cursor entryCursor;
		String id = ermittleIdZuPositionInArray(position);
		if (id == null) {
			return null;
		}
		Uri uri = ContentUris.withAppendedId(mParentUri, Long.parseLong(id));
		entryCursor = mContext.getContentResolver().query(uri, null, null, null, null);

		if (entryCursor.moveToFirst()) {
			final int linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
			final int abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
			final int fulltextPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.FULLTEXT);
			final int grafikPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.GRAFIKLINK);
			final int datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
			final int titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
			final int readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);
			final int readIdPosition = entryCursor.getColumnIndex(android.provider.BaseColumns._ID);

			DtoEntry dto = new DtoEntry();
			long _id = entryCursor.getLong(readIdPosition);
			dto.id = "" + _id;
			String link = entryCursor.getString(linkPosition);
			link = EntryActivity.fixLink(link);
			dto.link = link;

			String abstractText = entryCursor.getString(abstractPosition);
			String fullText = entryCursor.getString(fulltextPosition);

			String linkGrafik = entryCursor.getString(grafikPosition);
			dto.linkGrafik = linkGrafik;
			if (dto.linkGrafik==null && abstractText != null){
				dto.linkGrafik = Util.takeFirstSrc(abstractText);
			}

			if (fullText != null) {
				// eigentlich reicht dann fullText
				if (abstractText == null || fullText.length() > abstractText.length()) {
					dto.text = fullText;
					dto.isFulltext = true;
				}
			} else {
				if(abstractText==null){
					dto.text = "";
				}else{
					dto.text = abstractText;
				}
			}

			if (!mContext.showPics) {
				// webview Block Images scheint nicht zu ziehen...
				if(dto.text!=null){
					dto.text = dto.text.replaceAll(Strings.HTML_IMG_REGEX, Strings.EMPTY);
				}
			}
			dto.text += "<br><br>"; // Verschieben zur Anzeige !!

			if (entryCursor.isNull(readDatePosition)) {
				dto.isRead = false;
			} else {
				dto.isRead = true;
			}

			long timestamp = entryCursor.getLong(datePosition);
			String txtTitel = entryCursor.getString(titlePosition);
			Date date = new Date(timestamp);
			StringBuilder dateStringBuilder = new StringBuilder("");

			dateStringBuilder.append(mContext.getCSS()); // immer
			dateStringBuilder.append("<body>");
			dateStringBuilder.append("<b>" + txtTitel + "</b>");

			// + date align right
			if (!dto.isRead) {
				dateStringBuilder.append("<b>");
			}
			dateStringBuilder.append("<div style=\"text-align:right;\">");

			dateStringBuilder.append(DateFormat.getDateFormat(mContext).format(date)).append(' ')
					.append(DateFormat.getTimeFormat(mContext).format(date));
			dateStringBuilder.append("</div>");
			if (!dto.isRead) {
				dateStringBuilder.append("</b>");
			}
			dto.titel = dateStringBuilder.toString();

			entryCursor.close();

			return dto;
		}
		entryCursor.close();
		return null;
	}

	private void refreshLayout(DtoEntry dtoEntry, ViewGroup layout) {
		if (dtoEntry == null || dtoEntry == null) {
			System.err.println("" + dtoEntry + layout);
		}

		checkViews(dtoEntry, layout);

		if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_READABILITY) {
			new AsyncVeryNewReadability().execute(dtoEntry);
		} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_AMP) {
			new AsyncAmpRead().execute(dtoEntry);
		} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_MOBILIZE) {
			new AsyncMobilizeBody().execute(dtoEntry);
		} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_GOOGLEWEBLIGHT) {
			new AsyncGoogleRead().execute(dtoEntry);
		} else {
			// Default: AUFRUFART_FEED
			reload(dtoEntry, layout);
		}

	}

	/**
	 * Sucht ggf. nochmal den WebView, etc.
	 */
	private void checkViews(DtoEntry dtoEntry, ViewGroup layout) {

		if (dtoEntry.viewWeb == null) {
			if (layout != null) {
				dtoEntry.viewWeb = (WebView) layout.findViewById(R.id.web_view);
			}
			if (dtoEntry.viewWeb == null) {
				dtoEntry.viewWeb = (WebView) mContext.findViewById(R.id.web_view);
			}
			mContext.setZoomsScale(dtoEntry.viewWeb);
			if (!mContext.showPics) {
				dtoEntry.viewWeb.getSettings().setBlockNetworkImage(false);
			}
		}

		if (dtoEntry.viewImage == null) {
			if (layout != null) {
				dtoEntry.viewImage = (ImageView) layout.findViewById(R.id.backdrop);
			}
			if (dtoEntry.viewImage == null) {
				dtoEntry.viewImage = (ImageView) mContext.findViewById(R.id.backdrop);
			}
		}
		if (dtoEntry.viewImage != null) {
			// black / grey
			dtoEntry.viewImage.setBackgroundColor(Color.parseColor(EntryActivity.BACKGROUND_COLOR));
		}

		if (dtoEntry.progressBar == null) {
			if (layout != null) {
				dtoEntry.progressBar = (ProgressBar) layout.findViewById(R.id.progress_spinner);
			}
			if (dtoEntry.progressBar == null) {
				dtoEntry.progressBar = (ProgressBar) mContext.findViewById(R.id.progress_spinner);
			}
			dtoEntry.progressBar.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public int getCount() {
		return mAnzahlFeedeintraege;
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	public int getAktuellePosition() {
		return mAktuellePosition;
	}

	public void setAktuellePosition(int pos) {
		mAktuellePosition = pos;
	}

	public class AsyncVeryNewReadability extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dto;
		File  aimageFile=null;

		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				dto = params[0];
				if (!dto.isFulltext) {
					fetchHtmlSeite(dto);
				}
				if(mContext.shouldShowCover(dto)){
				
					new Runnable() {
						
						@Override
						public void run() {
							String mImageFolder = Util.getImageFolderFile(mContext).toString();
							String pathToImage = mImageFolder + "/" + dto.id + "_cover.jpg";
							File imageFile = new File(pathToImage);
							if(imageFile.exists()){
								aimageFile = imageFile;
							}else{
								try {
									byte[] data = FetcherService.getBytes(new URL(dto.linkGrafik).openStream());
									FileOutputStream fos = new FileOutputStream(pathToImage);
									fos.write(data);
									fos.close();
									imageFile = new File(pathToImage);
									aimageFile = imageFile;
								} catch (Exception e) {
									System.err.println("Err Run loading " + dto.linkGrafik + " " + e);
								}
							}
						}
						
					}.run();
					
				}// if linkGrafik
				
			} catch (Exception e) {
				Util.toastMessage(mContext, "" + e);
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			checkViews(dto, null);

			if (dto.text != null) {

				dto.text = dto.titel + dto.text;

				String baseUrl = EntryActivity.getBaseUrl(dto.link);
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
					// BaseUrl unter android 4 zeigt html als Text
					dto.viewWeb.loadData(dto.text, "text/html; charset=UTF-8", "utf-8");
				} else {
					dto.viewWeb.loadDataWithBaseURL(baseUrl, dto.text, "text/html", Encoding.UTF_8.toString(), null);
				}

				EntryActivity mContext2 = mContext;
				if (mContext2.showCover && dto.linkGrafik != null) {

					URL url;
					try {
//						url = new URL(dto.linkGrafik);
//						Glide.with(mContext).load(url).centerCrop().into(dto.viewImage);
						
						if(aimageFile!=null){
							Glide.with(mContext).load(aimageFile).centerCrop().into(dto.viewImage);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					if (dto.viewImage != null) {
						int pixel = 1;
						dto.viewImage.setLayoutParams(new LinearLayout.LayoutParams(1, pixel));
					}
				}

			} // else text leer - nix machen, ggf. feed (neu) laden ?!

			dto.progressBar.setVisibility(View.INVISIBLE);
		}
	}

	
	
	public class AsyncAmpRead extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dto;

		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				dto = params[0];

				String bahtml = "";
				HttpURLConnection connection = null;
				URL url = new URL(dto.link);
				connection = (HttpURLConnection) url.openConnection();

				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), "UTF8"));

				// baseUrl wechselt ab con.getInputStream (open?)
				String baseUrl = connection.getURL().getProtocol() + "://" + connection.getURL().getHost();

				String line = bufferedReader.readLine();
				while (line != null) {
					line = bufferedReader.readLine();
					bahtml += line;
				}
				bufferedReader.close();

				int posAmphtml = bahtml.indexOf("\"amphtml\"");
				if (posAmphtml < 0) {
					// kein AMP -> Default == reload/Feed
					return null;
				}
				int posHref = bahtml.indexOf("href=\"", posAmphtml);
				posHref = posHref + 6;
				int posEnd = bahtml.indexOf("\"", posHref);
				String ampLink = bahtml.substring(posHref, posEnd);
				if (ampLink.startsWith("/")) {
					dto.linkAmp = baseUrl + ampLink;
				} else {
					dto.linkAmp = ampLink;
				}

				if (dto.linkAmp != null) {

					dto.link = dto.linkAmp;

					fetchHtmlSeite(dto);

					dto.linkAmp = null; // reload() erzwingen
				}

			} catch (Exception e) {

				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (dto.linkAmp != null) {
				checkViews(dto, null);
				dto.viewWeb.loadUrl(dto.linkAmp);
			} else {
				reload(dto, null);
			}
			dto.progressBar.setVisibility(View.INVISIBLE);
		}
	}

	public DtoEntry getAktuellenEntry() {
		if (mDtoEntry != null) {
			return mDtoEntry;
		}
		int akt = getAktuellePosition();
		DtoEntry dtoEntry = this.ladeDtoEntry(akt);
		return dtoEntry;
	}

	public void reload(DtoEntry dto, ViewGroup layout) {
		// mit layout nur aus Create:instantiateItem
		// reload wird von allen, ausser Readability genutzt!
		// d.g. kein Immage !!! -> alles raus !!

		dto.text = dto.titel + dto.text;

		checkViews(dto, null);
		String baseUrl = EntryActivity.getBaseUrl(dto.link);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			// BaseUrl unter android 4 zeigt html als Text
			dto.viewWeb.loadData(dto.text, "text/html; charset=UTF-8", "utf-8");
		} else {
			dto.viewWeb.loadDataWithBaseURL(baseUrl, dto.text, "text/html", Encoding.UTF_8.toString(), null);
		}

		// reload ohne immage
		if (dto.viewImage != null) {
			// int pixel = Util.getButtonSizeInPixel(mContext)*2;
			int pixel = 1;
			dto.viewImage.setLayoutParams(new LinearLayout.LayoutParams(1, pixel));
		}

		dto.progressBar.setVisibility(View.INVISIBLE);

	}

	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}

	static private int backgroundColor = -1;
	static private int textForgroundColor = -1;

	int getBackgroundColor(Context context) {
		if (backgroundColor < 0) {
			TypedArray array = context.getTheme().obtainStyledAttributes(
					new int[] { android.R.attr.colorBackground, android.R.attr.textColorPrimary, });
			backgroundColor = array.getColor(0, 0xFF00FF);
			textForgroundColor = array.getColor(1, 0xFF00FF);
			array.recycle();
		}
		return backgroundColor;
	}

	int getTextForgroundColor(Context context) {
		if (backgroundColor < 0) {
			TypedArray array = context.getTheme().obtainStyledAttributes(
					new int[] { android.R.attr.colorBackground, android.R.attr.textColorPrimary, });
			backgroundColor = array.getColor(0, 0xFF00FF);
			textForgroundColor = array.getColor(1, 0xFF00FF);
			array.recycle();
		}
		return backgroundColor;
	}

	/**
	 * Liest dto.link aus. ?berschreibt dto.text und dto.linkGrafik
	 */
	public void fetchHtmlSeite(DtoEntry dto) throws Exception {

		HtmlFetcher fetcher2 = new HtmlFetcher();
		fetcher2.setMaxTextLength(50000);
		JResult res = fetcher2.fetchAndExtract(dto.link, 10000, true);
		String text = res.getText();
		// String title = res.getTitle();

		if(dto.linkGrafik==null){ // ggf. schon oben aus RSS gelsesen

			// nochmal RSS bevorzugen
			if (dto.text!=null && dto.text.startsWith("<")){
				dto.linkGrafik = Util.takeFirstSrc(dto.text);
			}
			if(dto.linkGrafik==null){
				String imageUrl = res.getImageUrl();
				if (imageUrl != null && !"".equals(imageUrl) && !imageUrl.contains("leer.") && !imageUrl.contains("empty.")) {
					dto.linkGrafik = imageUrl;
				}
			}
		}

		if (text != null) {
			text += "<br>";
			if (dto.text == null || text.length() > dto.text.length()) {
				dto.text = text;
				dto.isFulltext = true;
				// update Readability Fulltext to DB
				ContentValues values = FeedDataContentProvider.createContentValuesForFulltext(dto.text, dto.linkGrafik);
				mContext.getContentResolver().update(ContentUris.withAppendedId(mParentUri, Long.parseLong(dto.id)),
						values, null, null);
			}
		}
	}

	public class AsyncMobilizeBody extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dto;

		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				dto = params[0];

				String bahtml = "";
				HttpURLConnection connection = null;
				URL url = new URL(dto.link);
				connection = (HttpURLConnection) url.openConnection();

				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), "UTF8"));

				// baseUrl wechselt ab con.getInputStream (open?)
				String baseUrl = connection.getURL().getProtocol() + "://" + connection.getURL().getHost();

				String line = bufferedReader.readLine();
				while (line != null) {
					line = bufferedReader.readLine();
					bahtml += line;
				}
				bufferedReader.close();

				int posBody = bahtml.indexOf("<body>");
				if (posBody < 0) {
					// -> Default == reload/Feed
					return null;
				}
				bahtml = bahtml.substring(posBody + 6);

				// todo strip body: images, youtube,...?!

				StringBuilder stringBuilder = new StringBuilder("");
				stringBuilder.append(EntryActivity.getCSS()); // immer
				stringBuilder.append(dto.titel);
				stringBuilder.append(bahtml);

				dto.text = stringBuilder.toString();

			} catch (Exception e) {

				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			// checkViews(dto, null);

			reload(dto, null);

			dto.progressBar.setVisibility(View.INVISIBLE);
		}
	}

	static final String googleweblight = "https://googleweblight.com/?lite_url=";

	public class AsyncGoogleRead extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dto;

		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				dto = params[0];

				// dto.linkAmp=googleweblight+dto.link; // ansicht unver?ndert
				dto.link = googleweblight + dto.link;

				fetchHtmlSeite(dto);

			} catch (Exception e) {

				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (dto.linkAmp != null) {
				checkViews(dto, null);
				dto.viewWeb.loadUrl(dto.linkAmp);
			} else {
				reload(dto, null);
			}
			dto.progressBar.setVisibility(View.INVISIBLE);
		}
	}

	private Drawable getDrawableForEntry(DtoEntry dto) {
		Drawable ret = null;
		int feedId = Util.getFeedIdZuEntryId(mContext, dto.id);
		if (feedId > 0) {
			// Query only for FEED_PROJECTION
			Cursor cursor = mContext.getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedId),
					EntriesListActivity.FEED_PROJECTION, null, null, null);

			int buttonSize = Util.getButtonSizeInPixel(mContext);

			String title = null;

			if (cursor.moveToFirst()) {
				title = cursor.isNull(0) ? cursor.getString(1) : cursor.getString(0);
				String link = cursor.getString(1);
				if (!link.contains(".feedburner.com")) {
					byte[] iconBytes = null;
					iconBytes = cursor.getBlob(2);
					if (iconBytes != null && iconBytes.length > 0) {
						try {
							Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
							bitmap = Bitmap.createScaledBitmap(bitmap, buttonSize, buttonSize, false);
							bitmap = Util.getRoundedBitmap(bitmap);
							BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
							int densityDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
							bitmapDrawable.setTargetDensity(densityDpi);
							ret = bitmapDrawable;
						} catch (Exception e) {
							System.err.println("Catched Exception for createScaledBitmap in EntriesListActivity");
							TextDrawable textDrawable = Util.getRoundButtonImage(mContext, Long.valueOf(feedId), "X");
							ret = textDrawable;
						}
					}
				}
				if (ret == null && title != null) {
					TextDrawable textDrawable = Util.getRoundButtonImage(mContext, Long.valueOf(feedId), title);
					ret = textDrawable;
				}

			}
			cursor.close();
		}
		if (ret == null) {
			ret = Util.getRoundButtonImage(mContext, null, "Y");
		}
		return ret;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

	@Override
	public void destroyItem(ViewGroup collection, int position, Object view) {
		collection.removeView((View) view);
	}

	@Override
	public void destroyItem(View container, int position, Object object) {
		super.destroyItem(container, position, object);
	}

	// public void refreshCount() {
	// Cursor entryCursor;
	//
	// if(showRead){
	// entryCursor = mContext.getContentResolver().query(mParentUri, null, null,
	// null, sortOrder);
	// }else{
	// entryCursor = mContext.getContentResolver().query(mParentUri, null,
	// EntriesListAdapter.READDATEISNULL, null, sortOrder);
	// }
	//
	//// if (entryCursor.moveToFirst()) {
	// mAnzahlFeedeintraege=entryCursor.getCount();
	// entryCursor.close();
	// }

	public void ermittleAlleIds() {
		mListeIdsAsString.clear();
		Cursor cursor;
		// if(showRead){
		// cursor = mContext.getContentResolver().query(mParentUri, null, null,
		// null, sortOrder);
		// }else{
		// cursor = mContext.getContentResolver().query(mParentUri, null,
		// EntriesListAdapter.READDATEISNULL, null, sortOrder);
		// }
		cursor = mContext.getContentResolver().query(mParentUri, null, mSelectionFilter, null, sortOrder);

		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			final String id = cursor.getString(0);
			mListeIdsAsString.add(id);
			cursor.moveToNext();
		}
		cursor.close();
		mAnzahlFeedeintraege = mListeIdsAsString.size();
	}

	public String ermittleIdZuPositionInArray(int position) {
		if (mListeIdsAsString == null) {
			System.err.println("ermittleIdZuPosition mListeIdsAsString is empty");
			return null;
		}
		if (mListeIdsAsString.size() <= position) {
			System.err.println("ermittleIdZuPosition mListeIdsAsString has not " + position);
			return null;
		}
		return mListeIdsAsString.get(position);
	}

	/**
	 * Durchsucht mListeIdsAsString nach primaryKey ID und gibt seine Postiion
	 * in der Liste bzw. DB zur?ck
	 */
	public int ermittlePositionZuIdInArray(String id) {
		for (int i = 0; i < mListeIdsAsString.size(); i++) {
			if (id.equals(mListeIdsAsString.get(i))) {
				return i;
			}
		}
		return -1;
	}

}
