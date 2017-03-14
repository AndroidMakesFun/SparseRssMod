package de.bernd.shandschuh.sparserss;

public class RecycleListActivity extends ParentActivity {


	public void mySetContentView() {
		setContentView(R.layout.activity_recycle_list);
	}
	protected void createAdapter(){
		mAdapter = new RecycleListAdapter(this, uri, getIntent().getBooleanExtra(EXTRA_SHOWFEEDINFO, false), getIntent().getBooleanExtra(EXTRA_AUTORELOAD, false), R.layout.recyclelistitem);
	}
}
