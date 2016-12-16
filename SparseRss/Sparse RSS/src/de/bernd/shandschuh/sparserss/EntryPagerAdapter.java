package de.bernd.shandschuh.sparserss;

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
    	String titel;
    	String text;
    	TextView titelView;
    	WebView webview;
    	ImageView imageView;
    	ProgressBar progressBar;
    }


	public EntryPagerAdapter(EntryActivity context, int anzahlFeedeintraege) {
        mContext = context;
        mAktuellePosition=-1;
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
        
//		Toolbar toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
//		mContext.setSupportActionBar(toolbar);
//		setHomeButtonActive();

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
			aAsyncDtoEntry.titel=txtTitel + "  " + dateStringBuilder;
			return aAsyncDtoEntry;
		}
		return null;
	}

    
	private void refreshLayout(DtoEntry dtoEntry, ViewGroup layout) {
		// TODO welche Aufrufart?
		if(dtoEntry==null || dtoEntry==null){
			System.err.println("" + dtoEntry + layout);
		}
		
		ProgressBar progressBar=(ProgressBar) mContext.findViewById(R.id.progress_spinner);
		progressBar.setVisibility(View.VISIBLE);
		

	        WebView webView = (WebView) layout.findViewById(R.id.web_view);
//	        MyWebViewClient myWebViewClient = new MyWebViewClient();
//	        webView.setWebViewClient(myWebViewClient);
	        dtoEntry.webview=webView;
	        
	        TextView textView =(TextView) layout.findViewById(R.id.entry_date);
	        textView.setText(dtoEntry.titel);
	        dtoEntry.titelView=textView;

	        ImageView imageView = (ImageView) mContext.findViewById(R.id.backdrop);
	        dtoEntry.imageView=imageView;
	        
	        dtoEntry.progressBar=progressBar;
			
			new AsyncVeryNewReadability().execute(dtoEntry);			
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
    	mAktuellePosition=position;
    }

    public int getAktuellePosition() {
		return mAktuellePosition;
	}

    
	public void setHomeButtonActive() {
		android.support.v7.app.ActionBar actionBar7 = mContext.getSupportActionBar();
		actionBar7.setHomeButtonEnabled(true);
		// durchsichtige Actionbar == default !
//		actionBar7.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#51000000")));
		android.app.ActionBar actionBar = mContext.getActionBar();
		if (actionBar != null) {
			actionBar.hide(); // immer weil doppelt...
		}

		// Up Button, kein Titel
		//{@link android.view.Window#FEATURE_ACTION_BAR FEATURE_SUPPORT_ACTION_BAR}.</p>
		int flags = ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE;
		int change = actionBar7.getDisplayOptions() ^ flags;
		actionBar7.setDisplayOptions(change, flags);

	}

	
	class AsyncVeryNewReadability extends AsyncTask<DtoEntry, Void, Void> {

		DtoEntry aAsyncDatensatz;
		
		@Override
		protected Void doInBackground(DtoEntry... params) {

			try {
				aAsyncDatensatz=params[0];
				
				
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
			
			aAsyncDatensatz.titelView.setText(aAsyncDatensatz.titel);

			if (aAsyncDatensatz.text != null) {

				// Bilder auf 100% runter sizen
				// content is the content of the HTML or XML.
				String stringToAdd = "width=\"100%\" height=\"auto\" ";
				// Create a StringBuilder to insert string in the middle of
				// content.
				StringBuilder sb = new StringBuilder(aAsyncDatensatz.text);
				int i = 0;
				int cont = 0;
				// Check for the "src" substring, if it exists, take the index
				// where
				// it appears and insert the stringToAdd there, then increment a
				// counter
				// because the string gets altered and you should sum the length
				// of the inserted substring
				while (i != -1) {
					i = aAsyncDatensatz.text.indexOf("src", i + 1);
					if (i != -1)
						sb.insert(i + (cont * stringToAdd.length()), stringToAdd);
					++cont;
				}
				aAsyncDatensatz.text = sb.toString();
				
//				WebView webView = new WebView(mContext);
//				WebView webView = (WebView) aAsyncDatensatz.layout.findViewById(R.id.web_view);

				aAsyncDatensatz.webview.loadData(aAsyncDatensatz.text, "text/html; charset=UTF-8", null);
				// webView.loadDataWithBaseURL(mNewLink, bahtml, "text/html;
				// charset=UTF-8", null, null);

				if (aAsyncDatensatz.linkGrafik != null) {

					URL url;
					try {
						url = new URL(aAsyncDatensatz.linkGrafik );
//						ImageView imageView = (ImageView) aAsyncDatensatz.layout.findViewById(R.id.backdrop);
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
			view.scrollTo(0, 0);
//			zeigeProgressBar(false);
			// kopiert
			view.setVisibility(View.VISIBLE);
			super.onPageFinished(view, url);
		};
	}

	public DtoEntry getAktuellenEntry() {
		DtoId dtoId = this.ermittleIdZuPosition(mAktuellePosition);
		return this.ladeDtoEntry(dtoId);
	}
	
}
