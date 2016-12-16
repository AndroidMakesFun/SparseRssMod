package de.bernd.shandschuh.sparserss;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import com.bumptech.glide.Glide;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
    
    Uri mUri;
    Uri mParentUri;
    
    private ArrayList<DtoId> mListeIDS = new ArrayList<DtoId>();
    
    class DtoId{
    	String id;
    	long date;
    }

    class DtoEntry{
    	String id;
    	String link;
    	String linkGrafik;
    	String linkAmp;
    	String titel;
    	String text;
    	TextView titelView;
    	WebView webview;
    	ImageView imageView;
    	ProgressBar progressBar;
    }


	public EntryPagerAdapter(EntryActivity context, int anzahlFeedeintraege) {
        mContext = context;
        setAktuellePosition(-1);
        mAnzahlFeedeintraege=anzahlFeedeintraege;
        mListeIDS.clear();
		mUri = mContext.getIntent().getData();
		mParentUri = FeedData.EntryColumns.PARENT_URI(mUri.getPath());
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
		DtoId dtoId=ermittleIdZuPosition(position);
		DtoEntry dtoEntry =ladeDtoEntry(dtoId);
		refreshLayout(dtoEntry, layout);
        
        collection.addView(layout);
        return layout;
    }

	public DtoEntry ladeDtoEntry(DtoId idEntry) {
		// TODO welche Aufrufart?
		if(idEntry==null){
			System.err.println("" + idEntry);
			// knallt sowieso
		}
		
		String id=idEntry.id;
		Uri selectUri= FeedData.EntryColumns.ENTRY_CONTENT_URI(id);
		Cursor entryCursor = mContext.getContentResolver().query(selectUri, null, null, null, null);
		
		final int linkPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.LINK);
		final int abstractPosition = entryCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT);
		final int datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);
		final int titlePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.TITLE);

		if (entryCursor.moveToFirst()) {
			String link = entryCursor.getString(linkPosition);
			link = mContext.fixLink(link);
			long timestamp = entryCursor.getLong(datePosition);
			String txtTitel = entryCursor.getString(titlePosition);
			String abstractText = entryCursor.getString(abstractPosition);
			DtoEntry aAsyncDtoEntry=new DtoEntry();
			aAsyncDtoEntry.id=id;
			aAsyncDtoEntry.link=link;
			aAsyncDtoEntry.text=abstractText;
			
			Date date = new Date(timestamp);
			StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getDateFormat(mContext).format(date))
					.append(' ').append(DateFormat.getTimeFormat(mContext).format(date));
			aAsyncDtoEntry.titel="<b>" + txtTitel + "</b><br>" + dateStringBuilder;
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
		
		if(dtoEntry.webview==null){
			if(layout!=null){
				dtoEntry.webview = (WebView) layout.findViewById(R.id.web_view);
			}
			if(dtoEntry.webview ==null){
				dtoEntry.webview=(WebView) mContext.findViewById(R.id.web_view);
			}
	        mContext.setZoomsScale(dtoEntry.webview);
		}
        
//        MyWebViewClient myWebViewClient = new MyWebViewClient();
//        webView.setWebViewClient(myWebViewClient);
		
		if(dtoEntry.imageView==null){
			if(layout!=null){
				dtoEntry.imageView = (ImageView) layout.findViewById(R.id.backdrop);
			}
			if(dtoEntry.imageView==null){
				dtoEntry.imageView = (ImageView) mContext.findViewById(R.id.backdrop);
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


	private static final String DATE = "(date=";
	private static final String AND_ID = " and _id";
	private static final String OR_DATE = " or date ";
	private static final String ASC = "date asc, _id desc limit 1";
	private static final String DESC = "date desc, _id asc limit 1";

    public DtoId ermittleIdZuPosition(int position) {
    	if(position < mListeIDS.size()){
    		return mListeIDS.get(position);
    	}
    	if(position== 0){
    		
    		// _id = uri.getLastPathSegment();
    		Cursor entryCursor = mContext.getContentResolver().query(mUri, null, null, null, null);
    		
    		int entryIdPosition = entryCursor.getColumnIndex(FeedData.EntryColumns._ID);
    		int datePosition = entryCursor.getColumnIndex(FeedData.EntryColumns.DATE);

    		if (entryCursor.moveToFirst()) {
        		DtoId adapterDatensatz = new DtoId();
        		adapterDatensatz.id = "" + entryCursor.getInt(entryIdPosition);
        		adapterDatensatz.date = entryCursor.getLong(datePosition);
    			mListeIDS.add(adapterDatensatz);
    		}
    		entryCursor.close();
    		
    	}else{
    		
    		DtoId oldAdapterDatensatz = mListeIDS.get(mListeIDS.size()-1);
    		boolean successor=true; // vorwärts
    		long date=oldAdapterDatensatz.date;
    		String id=oldAdapterDatensatz.id;
    		StringBuilder queryString = new StringBuilder(DATE).append(date).append(AND_ID).append(successor ? '>' : '<')
    				.append(id).append(')').append(OR_DATE).append(successor ? '<' : '>').append(date);
    		
    		Cursor cursor = mContext.getContentResolver()
    				.query(mParentUri, new String[] { FeedData.EntryColumns._ID,FeedData.EntryColumns.DATE }, queryString.toString(), null, successor ? DESC : ASC);

    		if (cursor.moveToFirst()) {
    			final String nextId = cursor.getString(0);
    			final String strDate = cursor.getString(1);
        		DtoId adapterDatensatz = new DtoId();
        		adapterDatensatz.id = nextId;
        		adapterDatensatz.date = Long.parseLong(strDate);
    			mListeIDS.add(adapterDatensatz);
    		}
    		cursor.close();
    	}
    	if(position < mListeIDS.size()){
    		return mListeIDS.get(position);
    	}
    	System.err.println("Kein Entry für position " + position);
    	return null;
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

		DtoEntry aAsyncDatensatz;
		
		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				aAsyncDatensatz=params[0];

				aAsyncDatensatz.progressBar.setVisibility(View.VISIBLE);

				HtmlFetcher fetcher2 = new HtmlFetcher();
				fetcher2.setMaxTextLength(50000);
				JResult res = fetcher2.fetchAndExtract(aAsyncDatensatz.link, 10000, true);
				String text = res.getText();
				String title = res.getTitle();
				String imageUrl = res.getImageUrl();
				// System.out.println("image " + imageUrl);

				// collapsingToolbar.setTitle(title);
				// collapsingToolbar.setTitle("");

				if (imageUrl != null && !"".equals(imageUrl)) {
					aAsyncDatensatz.linkGrafik = imageUrl;
				}

				if (text != null) {
					aAsyncDatensatz.text = text + "<br>";
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
			
			if (aAsyncDatensatz.text != null) {

				// Bilder auf 100% runter sizen
				String stringToAdd = "width=\"100%\" height=\"auto\" ";
				StringBuilder sb = new StringBuilder(aAsyncDatensatz.text);
				int i = 0;
				int cont = 0;
				while (i != -1) {
					i = aAsyncDatensatz.text.indexOf("src", i + 1);
					if (i != -1)
						sb.insert(i + (cont * stringToAdd.length()), stringToAdd);
					++cont;
				}
				
				aAsyncDatensatz.text = aAsyncDatensatz.titel + "<br>" + sb.toString();
				
				aAsyncDatensatz.webview.loadData(aAsyncDatensatz.text, "text/html; charset=UTF-8", null);

				if (aAsyncDatensatz.linkGrafik != null) {

					URL url;
					try {
						url = new URL(aAsyncDatensatz.linkGrafik );
						Glide.with(mContext).load(url).centerCrop().into(aAsyncDatensatz.imageView);
						;
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}

			} // else text leer - nix machen, ggf. feed (neu) laden ?!
			aAsyncDatensatz.progressBar.setVisibility(View.INVISIBLE);
		}
	}

	public class AsyncAmpRead extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry aAsyncDatensatz;
		
		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				aAsyncDatensatz=params[0];

				aAsyncDatensatz.progressBar.setVisibility(View.VISIBLE);

				String bahtml = "";
				HttpURLConnection connection = null;
				URL url = new URL(aAsyncDatensatz.link);
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
					aAsyncDatensatz.linkAmp = baseUrl + ampLink;
				} else {
					aAsyncDatensatz.linkAmp = ampLink;
				}

			} catch (Exception e) {

				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if (aAsyncDatensatz.linkAmp != null) {
				aAsyncDatensatz.webview.loadUrl(aAsyncDatensatz.linkAmp);
			} else {
				Util.toastMessage(mContext, "No amphtml");
				// no reload();
			} // else text leer - nix machen, ggf. feed (neu) laden ?!
			aAsyncDatensatz.progressBar.setVisibility(View.INVISIBLE);
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
		DtoId dtoId = this.ermittleIdZuPosition(akt);
		DtoEntry dtoEntry = this.ladeDtoEntry(dtoId);
		System.out.println(" " + akt + " " + dtoEntry.titel);
		return dtoEntry;
	}

	public void reload(DtoEntry dtoEntry) {
		new AsyncReload().execute(dtoEntry);
	}

	public class AsyncReload extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry dtoEntry;
		
		@Override
		protected Void doInBackground(DtoEntry... params) {
			dtoEntry=params[0];
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			int posImg = dtoEntry.text.indexOf("src=\"");
			if (posImg > 0) {
				posImg += 5;
				int posImgEnde = dtoEntry.text.indexOf('"', posImg);
				if (posImgEnde > 0) {
					dtoEntry.linkGrafik = dtoEntry.text.substring(posImg, posImgEnde);
					System.out.println("gliedeHeader:" + dtoEntry.linkGrafik);
					URL url;
					try {
						url = new URL(dtoEntry.linkGrafik);
						Glide.with(mContext).load(url).centerCrop().into(dtoEntry.imageView);
						;
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}

				// sonst ein anderes aus dem Artikel, wenn Bilder geladen
				// wurden...
			} else if (Util.getImageFolderFile(mContext) != null && Util.getImageFolderFile(mContext).exists()) {
				PictureFilenameFilter filenameFilter = new PictureFilenameFilter(dtoEntry.id);

				File[] files = Util.getImageFolderFile(mContext).listFiles(filenameFilter);
				if (files != null && files.length > 0) {
					Glide.with(mContext).load(files[0]).centerCrop().into(dtoEntry.imageView);
				}
			}
			
			// Bilder auf 100% runter sizen
			String stringToAdd = "width=\"100%\" height=\"auto\" ";
			StringBuilder sb = new StringBuilder(dtoEntry.text);
			int i = 0;
			int cont = 0;
			while (i != -1) {
				i = dtoEntry.text.indexOf("src", i + 1);
				if (i != -1)
					sb.insert(i + (cont * stringToAdd.length()), stringToAdd);
				++cont;
			}
			
			dtoEntry.text = dtoEntry.titel + "<br>" + sb.toString();
			
			checkViews(dtoEntry, null);
			dtoEntry.webview.loadData(dtoEntry.text, "text/html; charset=UTF-8", "utf-8");
//			dtoEntry.webview.loadDataWithBaseURL(dtoEntry.link, dtoEntry.text, "text/html", "utf-8", null);

			
			dtoEntry.progressBar.setVisibility(View.INVISIBLE);
		}
	}

}
