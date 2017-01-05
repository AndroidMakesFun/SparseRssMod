package de.bernd.shandschuh.sparserss;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bernd.shandschuh.sparserss.handler.PictureFilenameFilter;
import de.bernd.shandschuh.sparserss.provider.FeedData;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

public class EntryPagerAdapter extends PagerAdapter {

    final private EntryActivity mContext;
    private int mAktuellePosition=-1;
    private int mAnzahlFeedeintraege;
    
    /**
     * content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/2/entries/8242
     */
    Uri mUri;
    
    /**
     * content://de.bernd.shandschuh.sparserss.provider.FeedData/feeds/2/entries
     */
    Uri mParentUri;
    
    private ArrayList<String> mListeIdsAsString = new ArrayList<String>();
	private boolean showPics; // Prefs Bilder laden und anzeigen
	private boolean showRead; // EntriesListActivity gelesene anzeigen
    
    class DtoEntry{
    	String id;
    	String link;
    	String linkGrafik;
    	String linkAmp;
    	String titel;
    	String text;
    	boolean isRead;
    	TextView viewTitel;
    	WebView viewWeb;
    	ImageView viewImage;
    	ProgressBar progressBar;
    }


	public EntryPagerAdapter(EntryActivity context, int position, int anzahlFeedeintraege) {
        mContext = context;
        mAnzahlFeedeintraege=anzahlFeedeintraege;
		mUri = mContext.getIntent().getData();
		showRead = mContext.getIntent().getBooleanExtra(EntriesListActivity.EXTRA_SHOWREAD, true);
		mParentUri = FeedData.EntryColumns.PARENT_URI(mUri.getPath());
		showPics = Util.showPics(context);
		
        ermittleAlleIds();
        
        if(position<0){
            mAnzahlFeedeintraege=mListeIdsAsString.size(); // statt anzahlFeedeintraege
        	String id = mUri.getLastPathSegment();
        	position=ermittlePositionZuId(id);
        }
        setAktuellePosition(position);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
    	
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.entry_pager, collection, false);
        
		Toolbar toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
		mContext.setSupportActionBar(toolbar);
		setHomeButtonActive(); // noch für noTitel
		
//		android.support.v7.app.ActionBar actionBar7 = mContext.getSupportActionBar();
//		actionBar7.setDisplayHomeAsUpEnabled(true);		
//		TextDrawable textDrawable = Util.getRoundButtonImage(mContext, null, "Hallo");
//		actionBar7.setHomeAsUpIndicator(textDrawable);
		
		if(!showPics){
			AppBarLayout aAppBarLayout =(AppBarLayout) layout.findViewById(R.id.appBarLayout);
			if(aAppBarLayout!=null){
				aAppBarLayout.setExpanded(false,false);
			}
		}

		String id=ermittleIdZuPosition(position);
		DtoEntry dtoEntry =ladeDtoEntry(id);
		refreshLayout(dtoEntry, layout);
		

        
        collection.addView(layout);
        return layout;
    }

    // Ermittlung der aktuell angezeigten Position
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
//    	super.setPrimaryItem(container, position, object);
//    	if (getAktuellePosition()!=position){
    		// never, da schon im Contructor - Wer setzt gelesen ?
        	System.out.println("setPrimaryItem " + position + " " + object);
        	setAktuellePosition(position);
        	String id = ermittleIdZuPosition(position);
//        	if(!dto.isRead){
       		mContext.getContentResolver().update(ContentUris.withAppendedId(mParentUri,Long.parseLong(id)), RSSOverview.getReadContentValues(), null, null);
//    	}
    }


	public DtoEntry ladeDtoEntry(String id) {

		if(id==null){
			System.err.println("ladeDtoEntry id is null");
		}
		
		Uri selectUri= FeedData.EntryColumns.ENTRY_CONTENT_URI(id);
		Cursor entryCursor = mContext.getContentResolver().query(selectUri, null, null, null, null);
		
		final int linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		final int abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		final int datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		final int titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		final int readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);

		if (entryCursor.moveToFirst()) {
			DtoEntry dto=new DtoEntry();
			dto.id=id;
			String link = entryCursor.getString(linkPosition);
			link = EntryActivity.fixLink(link);
			dto.link=link;

			String abstractText = entryCursor.getString(abstractPosition);
			
			if(!showPics){
				// webview Block Images scheint nicht zu ziehen...
				abstractText = abstractText.replaceAll(Strings.HTML_IMG_REGEX, Strings.EMPTY);
			}
			
			dto.text=abstractText + "<br><br>";

			if (entryCursor.isNull(readDatePosition)){
				dto.isRead=false;
			}else{
				dto.isRead=true;
			}

			long timestamp = entryCursor.getLong(datePosition);
			String txtTitel = entryCursor.getString(titlePosition);					
			Date date = new Date(timestamp);
			StringBuilder dateStringBuilder = new StringBuilder("");
			
			dateStringBuilder.append(mContext.getCSS());  // immer
			
//			if(aAsyncDtoEntry.isRead){
//				dateStringBuilder.append(txtTitel );
//			}else{
				// immer fett als Überschrift
				dateStringBuilder.append("<b>" + txtTitel + "</b>");
//			}
				
			// + date align right
			if(!dto.isRead){
				dateStringBuilder.append("<b>");
			}
			dateStringBuilder.append("<div style=\"text-align:right;\">");
			
			dateStringBuilder.append(DateFormat.getDateFormat(mContext).format(date))
				.append(' ').append(DateFormat.getTimeFormat(mContext).format(date));
			dateStringBuilder.append("</div>");			
			if(!dto.isRead){
				dateStringBuilder.append("</b>");
			}
			dto.titel=dateStringBuilder.toString();
			
			entryCursor.close();
			
			return dto;
		}
		return null;
	}

    
	private void refreshLayout(DtoEntry dtoEntry, ViewGroup layout) {
		if(dtoEntry==null || dtoEntry==null){
			System.err.println("" + dtoEntry + layout);
		}
		
		checkViews(dtoEntry, layout);

		if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_READABILITY) {
			new AsyncVeryNewReadability().execute(dtoEntry);			
		} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_AMP) {
			new AsyncAmpRead().execute(dtoEntry);			
		} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_MOBILIZE) {
			new AsyncMobilizeBody().execute(dtoEntry);			
		}else{  
			// Default: AUFRUFART_FEED
			reload(dtoEntry, layout);
		}

	}

	/**
	 * Sucht ggf. nochmal den WebView, etc.
	 */
	private void checkViews(DtoEntry dtoEntry, ViewGroup layout) {
		
		if(dtoEntry.viewWeb==null){
			if(layout!=null){
				dtoEntry.viewWeb = (WebView) layout.findViewById(R.id.web_view);
			}
			if(dtoEntry.viewWeb ==null){
				dtoEntry.viewWeb=(WebView) mContext.findViewById(R.id.web_view);
			}
	        mContext.setZoomsScale(dtoEntry.viewWeb);
	        if(!showPics){
	        	dtoEntry.viewWeb.getSettings().setBlockNetworkImage(false);
	        }
		}
        
//        MyWebViewClient myWebViewClient = new MyWebViewClient();
//        webView.setWebViewClient(myWebViewClient);
//		if (Util.isLightTheme(mContext)) {
//			dtoEntry.viewWeb.setBackgroundColor(getBackgroundColor(mContext));
//		}else{
//			dtoEntry.viewWeb.setBackgroundColor(Color.BLACK);
//		}
		if(dtoEntry.viewImage==null){
			if(layout!=null){
				dtoEntry.viewImage = (ImageView) layout.findViewById(R.id.backdrop);
			}
			if(dtoEntry.viewImage==null){
				dtoEntry.viewImage = (ImageView) mContext.findViewById(R.id.backdrop);
			}
		}
		if(dtoEntry.viewImage!=null){
			// black / grey
			dtoEntry.viewImage.setBackgroundColor(Color.parseColor(EntryActivity.BACKGROUND_COLOR) );
		}
		
		if(dtoEntry.progressBar==null){
			if(layout!=null){
				dtoEntry.progressBar=(ProgressBar) layout.findViewById(R.id.progress_spinner);
			}
			if(dtoEntry.progressBar==null){
				dtoEntry.progressBar=(ProgressBar) mContext.findViewById(R.id.progress_spinner);
			}
			dtoEntry.progressBar.setVisibility(View.VISIBLE);
		}
		
	}


    public void ermittleAlleIds() {
    	mListeIdsAsString.clear();
    	Cursor cursor;
    	if(showRead){
    		cursor = mContext.getContentResolver().query(mParentUri, null, null, null, "date DESC");    		
    	}else{
    		String READDATEISNULL = "readdate is null";
    		cursor = mContext.getContentResolver().query(mParentUri, null, READDATEISNULL, null, "date DESC");
    	}
		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			final String id = cursor.getString(0);
			mListeIdsAsString.add(id);
			cursor.moveToNext();
		}
		cursor.close();
		if(!(mListeIdsAsString.size() == mAnzahlFeedeintraege)){
			System.out.println("Wrong mListeIdsAsString " + mListeIdsAsString.size() + " " + mAnzahlFeedeintraege);
//			Util.toastMessageLong(mContext, "Wrong mListeIdsAsString " + mListeIdsAsString.size() + " " + mAnzahlFeedeintraege);
		}
    }
    
    
    public String ermittleIdZuPosition(int position) {
    		return mListeIdsAsString.get(position);
	}

    /**
     * Durchsucht mListeIdsAsString nach primaryKey ID und gibt seine Postiion in der Liste bzw. DB zurück
     */
    public int ermittlePositionZuId(String id) {
    	for (int i = 0; i < mListeIdsAsString.size(); i++) {
			if(id.equals(mListeIdsAsString.get(i))){
				return i;
			}
		}
    	return -1;
}

	@Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
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
    
    public void setAktuellePosition(int pos){
    	mAktuellePosition=pos;
    }

    
	public void setHomeButtonActive() {
		android.support.v7.app.ActionBar actionBar7 = mContext.getSupportActionBar();
		actionBar7.setHomeButtonEnabled(true);
		
		// Up Button, kein Titel
		int flags = ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE;
		int change = actionBar7.getDisplayOptions() ^ flags;
		actionBar7.setDisplayOptions(change, flags);
	}

	
	public class AsyncVeryNewReadability extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dto;
		
		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				dto=params[0];
				fetchHtmlSeite(dto);

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

//				dto.viewWeb.loadData(dto.text, "text/html; charset=UTF-8", null);
				String baseUrl=EntryActivity.getBaseUrl(dto.link);
				dto.viewWeb.loadDataWithBaseURL(baseUrl, dto.text, "text/html; charset=UTF-8", "utf-8", null);

				if (showPics && dto.linkGrafik != null) {

					URL url;
					try {
						url = new URL(dto.linkGrafik );
						Glide.with(mContext).load(url).centerCrop().into(dto.viewImage);
						;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else{
					AppBarLayout aAppBarLayout =(AppBarLayout) mContext.findViewById(R.id.appBarLayout);
					if(aAppBarLayout!=null){
						aAppBarLayout.setExpanded(false,false);
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
				dto=params[0];

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
					
					dto.link=dto.linkAmp;
					
					fetchHtmlSeite(dto);
					
					dto.linkAmp=null; // reload() erzwingen
				}


			} catch (Exception e) {

				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			checkViews(dto, null);

			if (dto.linkAmp != null) {
				dto.viewWeb.loadUrl(dto.linkAmp);
			} else {
				reload(dto, null);
			} 
			dto.progressBar.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Für onPageFinished um ProzessBar an/aus zu knipsen da webview aSyncron
	 * die animation trasht
	 */
	class MyWebViewClient extends WebViewClient {

		@Override
		public void onPageFinished(WebView view, String url) {
//			if (!isFirstEntry) {
//				nestedScrollView.scrollTo(0, 0);
//			}
//			view.scrollTo(0, 0);
//			NestedScrollView nestedScrollView =(NestedScrollView) mContext.findViewById(R.id.nested_scroll_view);
//			if(nestedScrollView!=null){
//				System.out.println("nestedScrollView.scrollTo(0, 300);");
//				nestedScrollView.scrollTo(0, -300);
//			}
//			zeigeProgressBar(false);
			// kopiert
			view.setVisibility(View.VISIBLE);
			super.onPageFinished(view, url);
		};
	}

	public DtoEntry getAktuellenEntry() {
		int akt=getAktuellePosition();
		String id = this.ermittleIdZuPosition(akt);
		DtoEntry dtoEntry = this.ladeDtoEntry(id);
		return dtoEntry;
	}

	public void reload(DtoEntry dto, ViewGroup layout) {

		checkViews(dto, null);

		if(layout!=null){  // nur Feed - kein Cover !
			AppBarLayout aAppBarLayout =(AppBarLayout) layout.findViewById(R.id.appBarLayout);
			if(aAppBarLayout!=null){
				aAppBarLayout.setExpanded(false,false);
			}
		}else{
			if(showPics){
				boolean noPic=true;
				int posImg = dto.text.indexOf("src=\"");
				if (posImg > 0) {
					posImg += 5;
					int posImgEnde = dto.text.indexOf('"', posImg);
					if (posImgEnde > 0) {
						dto.linkGrafik = dto.text.substring(posImg, posImgEnde);
						URL url;
						try {
							url = new URL(dto.linkGrafik);
							Glide.with(mContext).load(url).centerCrop().into(dto.viewImage);
							noPic=false;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
		
					// sonst ein anderes aus dem Artikel, wenn Bilder geladen
					// wurden...
				} else if (Util.getImageFolderFile(mContext) != null && Util.getImageFolderFile(mContext).exists()) {
					PictureFilenameFilter filenameFilter = new PictureFilenameFilter(dto.id);
		
					File[] files = Util.getImageFolderFile(mContext).listFiles(filenameFilter);
					if (files != null && files.length > 0) {
						Glide.with(mContext).load(files[0]).centerCrop().into(dto.viewImage);
						noPic=false;
					}
				}
				if(noPic){
					AppBarLayout aAppBarLayout =(AppBarLayout) mContext.findViewById(R.id.appBarLayout);
					if(aAppBarLayout!=null){
						aAppBarLayout.setExpanded(false,false);
					}
				}
			} // showPics	
		}	
		
		dto.text = dto.titel + dto.text;
		
		checkViews(dto, null);
		String baseUrl=EntryActivity.getBaseUrl(dto.link);
		dto.viewWeb.loadDataWithBaseURL(baseUrl, dto.text, "text/html; charset=UTF-8", "utf-8", null);
		
		dto.progressBar.setVisibility(View.INVISIBLE);
		
	}


	@Override
	public int getItemPosition(Object object) {
	    return POSITION_NONE;
	}
	
	static private int backgroundColor=-1;
	static private int textForgroundColor=-1;
	
	int getBackgroundColor(Context context){
		if(backgroundColor < 0 ){
			TypedArray array = context.getTheme().obtainStyledAttributes(new int[] {  
			    android.R.attr.colorBackground, 
			    android.R.attr.textColorPrimary, 
			}); 
			backgroundColor = array.getColor(0, 0xFF00FF); 
			textForgroundColor = array.getColor(1, 0xFF00FF); 
			array.recycle();
		}
		return backgroundColor;
	}

	int getTextForgroundColor(Context context){
		if(backgroundColor < 0 ){
			TypedArray array = context.getTheme().obtainStyledAttributes(new int[] {  
			    android.R.attr.colorBackground, 
			    android.R.attr.textColorPrimary, 
			}); 
			backgroundColor = array.getColor(0, 0xFF00FF); 
			textForgroundColor = array.getColor(1, 0xFF00FF); 
			array.recycle();
		}
		return backgroundColor;
	}


	/**
	 * Liest dto.link aus.
	 * Überschreibt dto.text und dto.linkGrafik 
	 */
	public void fetchHtmlSeite(DtoEntry dto) throws Exception {

		HtmlFetcher fetcher2 = new HtmlFetcher();
		fetcher2.setMaxTextLength(50000);
		JResult res = fetcher2.fetchAndExtract(dto.link, 10000, true);
		String text = res.getText();
//		String title = res.getTitle();
		String imageUrl = res.getImageUrl();

		if (imageUrl != null && !"".equals(imageUrl) && !imageUrl.contains("leer.") && !imageUrl.contains("empty.")) {
			dto.linkGrafik = imageUrl;
		}

		if (text != null) {
			dto.text = text + "<br>";
		}
	}

	
	public class AsyncMobilizeBody extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dto;
		
		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				dto=params[0];

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
				bahtml=bahtml.substring(posBody+6);

				// todo strip body: images, youtube,...?!

				StringBuilder stringBuilder = new StringBuilder("");				
				stringBuilder.append(EntryActivity.getCSS());  // immer				
				stringBuilder.append(dto.titel);
				stringBuilder.append(bahtml);
				
				dto.text=stringBuilder.toString();

			} catch (Exception e) {

				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			checkViews(dto, null);
			
			reload(dto, null);
			
			dto.progressBar.setVisibility(View.INVISIBLE);
		}
	}
	
	
}
