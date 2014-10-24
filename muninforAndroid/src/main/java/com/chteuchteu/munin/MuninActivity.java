package com.chteuchteu.munin;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuIcon;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.ui.Activity_About;
import com.chteuchteu.munin.ui.Activity_Settings;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

public class MuninActivity extends Activity {
	protected MuninFoo      muninFoo;
	protected DrawerHelper  dh;
	protected Context       context;
	protected Activity      activity;
	protected ActionBar     actionBar;
	private MaterialMenuIcon materialMenu;
	protected Menu          menu;
	protected String        activityName;

	private Runnable    onDrawerOpen;
	private Runnable    onDrawerClose;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.context = this;
		this.activity = this;
		this.muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);

		// setContentView...
	}

	public void onContentViewSet() {
		Util.UI.applySwag(this);
		Crashlytics.start(this);
		this.actionBar = getActionBar();
		this.actionBar.setDisplayShowHomeEnabled(false);
		this.dh = new DrawerHelper(this, muninFoo);
		this.materialMenu = new MaterialMenuIcon(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
		this.materialMenu.setNeverDrawTouch(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home)
			dh.closeDrawerIfOpened();

		switch (item.getItemId()) {
			case android.R.id.home:
				if (dh.isOpened())
					materialMenu.animatePressedState(MaterialMenuDrawable.IconState.BURGER);
				else
					materialMenu.animatePressedState(MaterialMenuDrawable.IconState.ARROW);
				dh.toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(context, Activity_Settings.class));
				Util.setTransition(context, Util.TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(context, Activity_About.class));
				Util.setTransition(context, Util.TransitionStyle.DEEPER);
				return true;
		}

		return true;
	}

	protected void createOptionsMenu() { menu.clear(); }

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		dh.getDrawer().setOnOpenListener(new SlidingMenu.OnOpenListener() {
			@Override
			public void onOpen() {
				dh.setIsOpened(true);
				activityName = getActionBar().getTitle().toString();
				getActionBar().setTitle(R.string.app_name);

				if (onDrawerOpen != null)
					onDrawerOpen.run();

				menu.clear();
				getMenuInflater().inflate(R.menu.main, menu);
			}
		});
		dh.getDrawer().setOnCloseListener(new SlidingMenu.OnCloseListener() {
			@Override
			public void onClose() {
				dh.setIsOpened(false);
				getActionBar().setTitle(activityName);

				if (onDrawerClose != null)
					onDrawerClose.run();

				createOptionsMenu();
			}
		});

		createOptionsMenu();

		return true;
	}

	protected void setOnDrawerOpen(Runnable val) { this.onDrawerOpen = val; }
	protected void setOnDrawerClose(Runnable val) { this.onDrawerClose = val; }

	@Override
	public void onStart() {
		super.onStart();

		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
	}

	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		materialMenu.syncState(savedInstanceState);
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		materialMenu.onSaveInstanceState(outState);
	}
}