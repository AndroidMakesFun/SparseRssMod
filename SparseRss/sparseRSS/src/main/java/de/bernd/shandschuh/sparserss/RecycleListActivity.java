package de.bernd.shandschuh.sparserss;

/**
 * Card View
 */
public class RecycleListActivity extends ParentActivity {


	public void mySetContentView() {
		setContentView(R.layout.activity_recycle_list);
	}
	protected void createAdapter(){
		
		boolean bShowFeedInfo = getIntent().getBooleanExtra(EXTRA_SHOWFEEDINFO, false);
		boolean bAutoreload = getIntent().getBooleanExtra(EXTRA_AUTORELOAD, false);
		int iFeedFilter=getIntent().getIntExtra(ParentActivity.EXTRA_SHOWFEEDFILTER, ParentActivity.EXTRA_FILTER_ALL);
		
		mAdapter = new RecycleListAdapter(this, uri, bShowFeedInfo, bAutoreload, R.layout.recyclelistitem, iFeedFilter);
		
	}
}
