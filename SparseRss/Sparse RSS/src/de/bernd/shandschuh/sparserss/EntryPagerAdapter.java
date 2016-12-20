package de.bernd.shandschuh.sparserss;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import com.bumptech.glide.Glide;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.CoordinatorLayout;
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
    private int mAktuellePosition;
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
		mParentUri = FeedData.EntryColumns.PARENT_URI(mUri.getPath());
		
        ermittleAlleIds();
        setAktuellePosition(position);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
    	System.out.println("instantiateItem " + position);
//        CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.entry_pager, collection, false);
        
		Toolbar toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
		mContext.setSupportActionBar(toolbar);
		setHomeButtonActive();

		// Titel, Text und Grafik laden - je Pager
		String id=ermittleIdZuPosition(position);
		DtoEntry dtoEntry =ladeDtoEntry(id);
		refreshLayout(dtoEntry, layout);
        
        collection.addView(layout);
        return layout;
    }

	public DtoEntry ladeDtoEntry(String id) {

		if(id==null){
			System.err.println("ladeDtoEntry id is null");
			// knallt sowieso
		}
		
		Uri selectUri= FeedData.EntryColumns.ENTRY_CONTENT_URI(id);
		Cursor entryCursor = mContext.getContentResolver().query(selectUri, null, null, null, null);
		
		final int linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		final int abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		final int datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		final int titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);
		final int readDatePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.READDATE);

		if (entryCursor.moveToFirst()) {
			DtoEntry aAsyncDtoEntry=new DtoEntry();
			aAsyncDtoEntry.id=id;
			String link = entryCursor.getString(linkPosition);
			link = EntryActivity.fixLink(link);
			aAsyncDtoEntry.link=link;

			String abstractText = entryCursor.getString(abstractPosition);
			aAsyncDtoEntry.text=abstractText;

			if (entryCursor.isNull(readDatePosition)){
				aAsyncDtoEntry.isRead=false;
			}else{
				aAsyncDtoEntry.isRead=true;
			}

			long timestamp = entryCursor.getLong(datePosition);
			String txtTitel = entryCursor.getString(titlePosition);					
			Date date = new Date(timestamp);
			StringBuilder dateStringBuilder = new StringBuilder("");
			if(aAsyncDtoEntry.isRead){
//				dateStringBuilder.append(txtTitel + "<br>");
				dateStringBuilder.append(txtTitel );
			}else{
//				dateStringBuilder.append("<b>" + txtTitel + "</b><br>");
				dateStringBuilder.append("<b>" + txtTitel + "</b>");
			}			
			// + date align right
//			dateStringBuilder.append("<p align=\"right\">");
			dateStringBuilder.append("<div style=\"text-align:right;\">");
			
			dateStringBuilder.append(mContext.getmAufrufart() + " "); //DEBUG
			
			dateStringBuilder.append(DateFormat.getDateFormat(mContext).format(date))
				.append(' ').append(DateFormat.getTimeFormat(mContext).format(date));
//			dateStringBuilder.append("</p>");			
			dateStringBuilder.append("</div>");			
			aAsyncDtoEntry.titel=dateStringBuilder.toString();
			
			return aAsyncDtoEntry;
		}
		return null;
	}

    
	private void refreshLayout(DtoEntry dtoEntry, ViewGroup layout) {
		// TODO welche Aufrufart?
		if(dtoEntry==null || dtoEntry==null){
			System.err.println("" + dtoEntry + layout);
		}
		
		checkViews(dtoEntry, layout);

	        
//	        TextView textView =(TextView) layout.findViewById(R.id.entry_date);
//	        textView.setText(dtoEntry.titel);
//	        dtoEntry.titelView=textView;

	        
//	        if(getAktuellePosition()==0){
//		        CoordinatorLayout cCoordinatorLayout = (CoordinatorLayout) mContext.findViewById(R.id.coordinatorLayout);
//		        ViewCompat.requestApplyInsets(cCoordinatorLayout);
//	        }
	        
//			if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_FEED) {
//			} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_MOBILIZE) {
//				loadMoblize();
//			} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_INSTAPAPER) {
//				onClickInstapaper(null);
			if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_READABILITY) {
				new AsyncVeryNewReadability().execute(dtoEntry);			
			} else if (mContext.getmAufrufart() == EntryActivity.AUFRUFART_AMP) {
				new AsyncAmpRead().execute(dtoEntry);			
			}else{  
				// Default: AUFRUFART_FEED
				reload(dtoEntry);
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
		}
        
//        MyWebViewClient myWebViewClient = new MyWebViewClient();
//        webView.setWebViewClient(myWebViewClient);
		dtoEntry.viewWeb.setBackgroundColor(getBackgroundColor(mContext));
		
		if(dtoEntry.viewImage==null){
			if(layout!=null){
				dtoEntry.viewImage = (ImageView) layout.findViewById(R.id.backdrop);
			}
			if(dtoEntry.viewImage==null){
				dtoEntry.viewImage = (ImageView) mContext.findViewById(R.id.backdrop);
			}
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
    	
    	// entriesListAdapter = new EntriesListAdapter(this, uri, intent.getBooleanExtra(EXTRA_SHOWFEEDINFO, false), intent.getBooleanExtra(EXTRA_AUTORELOAD, false));
    	
		Cursor cursor = mContext.getContentResolver().query(mParentUri, null, null, null, "date DESC");
		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			final String id = cursor.getString(0);
			mListeIdsAsString.add(id);
			cursor.moveToNext();
		}
		System.out.println("mListeIdsAsString " + mListeIdsAsString.size() + " " + (mListeIdsAsString.size() == mAnzahlFeedeintraege));
		if(!(mListeIdsAsString.size() == mAnzahlFeedeintraege)){
			Util.toastMessageLong(mContext, "Wrong mListeIdsAsString " + mListeIdsAsString.size() + " " + mAnzahlFeedeintraege);
		}
    }
    
    
    public String ermittleIdZuPosition(int position) {
    		return mListeIdsAsString.get(position);
	}

	@Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
    	System.out.println("destroyItem " + position);
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

    // Ermittlung der aktuell angezeigten Position
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
    	super.setPrimaryItem(container, position, object);
    	System.out.println("setPrimaryItem " + position);
    	setAktuellePosition(position);
    	String id = ermittleIdZuPosition(position);
//    	if(!dto.isRead){
    		mContext.getContentResolver().update(ContentUris.withAppendedId(mParentUri,Long.parseLong(id)), RSSOverview.getReadContentValues(), null, null);
//    	}
    	
    		
    }

    synchronized public int getAktuellePosition() {
		return mAktuellePosition;
	}
    
    synchronized public void setAktuellePosition(int pos){
    	mAktuellePosition=pos;
    }

    
	public void setHomeButtonActive() {
		android.support.v7.app.ActionBar actionBar7 = mContext.getSupportActionBar();
		actionBar7.setHomeButtonEnabled(true);
		
		android.app.ActionBar actionBar = mContext.getActionBar();
		if (actionBar != null) {
			actionBar.hide(); // immer weil doppelt...
		}

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

				HtmlFetcher fetcher2 = new HtmlFetcher();
				fetcher2.setMaxTextLength(50000);
				JResult res = fetcher2.fetchAndExtract(dto.link, 10000, true);
				String text = res.getText();
				String title = res.getTitle();
				String imageUrl = res.getImageUrl();

				if (imageUrl != null && !"".equals(imageUrl)) {
					dto.linkGrafik = imageUrl;
				}

				if (text != null) {
					dto.text = text + "<br>";
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
			
			checkViews(dto, null);
		
			if (dto.text != null) {

				// Bilder auf 100% runter sizen
				String stringToAdd = "width=\"100%\" height=\"auto\" ";
				StringBuilder sb = new StringBuilder(dto.text);
				int i = 0;
				int cont = 0;
				while (i != -1) {
					i = dto.text.indexOf("src", i + 1);
					if (i != -1)
						sb.insert(i + (cont * stringToAdd.length()), stringToAdd);
					++cont;
				}
				
				dto.text = dto.titel + sb.toString();
				
				dto.viewWeb.loadData(dto.text, "text/html; charset=UTF-8", null);

				if (dto.linkGrafik != null) {

					URL url;
					try {
						url = new URL(dto.linkGrafik );
						Glide.with(mContext).load(url).centerCrop().into(dto.viewImage);
						;
					} catch (Exception e) {
						e.printStackTrace();
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
				System.out.println("posAmphtml " + posAmphtml + " " + posHref);
				posHref = posHref + 6;
				int posEnd = bahtml.indexOf("\"", posHref);
				String ampLink = bahtml.substring(posHref, posEnd);
				if (ampLink.startsWith("/")) {
					dto.linkAmp = baseUrl + ampLink;
				} else {
					dto.linkAmp = ampLink;
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
//				Util.toastMessage(mContext, "No amphtml");
				reload(dto);
				// no reload();
			} // else text leer - nix machen, ggf. feed (neu) laden ?!
			dto.progressBar.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Für onPageFinished um ProzessBar an/aus zu knipsen da webview aSyncron
	 * die animation trasht
	 */
	private class MyWebViewClient extends WebViewClient {

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
		System.out.println(" " + akt + " " + dtoEntry.titel);
		return dtoEntry;
	}

	public void reload(DtoEntry dto) {

//		new AsyncReload().execute(dto);
		
		
		checkViews(dto, null);
		
		int posImg = dto.text.indexOf("src=\"");
		if (posImg > 0) {
			posImg += 5;
			int posImgEnde = dto.text.indexOf('"', posImg);
			if (posImgEnde > 0) {
				dto.linkGrafik = dto.text.substring(posImg, posImgEnde);
				System.out.println("gliedeHeader:" + dto.linkGrafik);
				URL url;
				try {
					url = new URL(dto.linkGrafik);
					Glide.with(mContext).load(url).centerCrop().into(dto.viewImage);
					;
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
			}
		}
		
		// Bilder auf 100% runter sizen
		String stringToAdd = "width=\"100%\" height=\"auto\" ";
		StringBuilder sb = new StringBuilder(dto.text);
		int i = 0;
		int cont = 0;
		while (i != -1) {
			i = dto.text.indexOf("src", i + 1);
			if (i != -1)
				sb.insert(i + (cont * stringToAdd.length()), stringToAdd);
			++cont;
		}
		
		dto.text = dto.titel + sb.toString();
		
		checkViews(dto, null);
		dto.viewWeb.loadData(dto.text, "text/html; charset=UTF-8", "utf-8");
		
		dto.progressBar.setVisibility(View.INVISIBLE);
		
	}

	public class AsyncReload extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dto;
		
		@Override
		protected Void doInBackground(DtoEntry... params) {
			dto=params[0];
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			

		}
	}

	// Rigeroses neu laden nach notifyDataSetChanged() - um ggf. sofortigen viewer wechsel zu erzwingen
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



}
