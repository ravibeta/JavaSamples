//package com.rearview.timechart;
//
//import java.util.HashMap;
//
//import android.annotation.SuppressLint;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.Bundle;
//
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentManager;
//import android.support.v4.app.FragmentPagerAdapter;
//import android.support.v4.app.FragmentTransaction;
//import android.support.v4.content.LocalBroadcastManager;
//import android.support.v4..view.ViewPager;
//import android.support.v7.app.ActionBar.Tab;
//import android.support.v7.app.ActionBarActivity;
//import android.support.v7.app.ActionBar;
//import android.view.Menu;
//
//import com.rearview.timechart.settings.Settings;
//import com.rearview.timechart.settings.SettingsHelper;
//
//@SuppressWarnings("deprecation")
//public class TimeChart extends MainActivity implements
//    ActionBar.TabListener, ViewPager.OnPageChangeListener {
//
//  private ViewPager mViewPager = null;
//
//  @Override
//  public void onStop() {
//    super.onStop();
//
//    if (mViewPager == null)
//      return;
//
//    // end self
//    if (isFinishing()) {
//      IpcService.getInstance().disconnect();
//      android.os.Process.killProcess(android.os.Process.myPid());
//    }
//  }
//
//  /** Called when the activity is first created. */
//  @Override
//  public void onCreate(Bundle savedInstanceState) {
//    // create view
//    super.onCreate(savedInstanceState);
//
//    IpcService.Initialize(this);
//    UserInterfaceUtil.Initialize(this);
//
//    // start background service
//    final Settings setting = Settings.getInstance(this);
//    if ((setting.isEnableCPUMeter() || setting.isAddShortCut())
//        && !CoreUtil.isServiceRunning(this))
//      startService(new Intent(this, TimeChartService.class));
//
//    // prepare exit
//    LocalBroadcastManager.getInstance(this).registerReceiver(ExitReceiver,
//        new IntentFilter("Exit"));
//  }
//
//  private BroadcastReceiver ExitReceiver = new BroadcastReceiver() {
//    @Override
//    public void onReceive(Context context, Intent intent) {
//      IpcService.getInstance().forceExit();
//      context.stopService(new Intent(context, TimeChartService.class));
//      finish();
//    }
//  };
//
//  @Override
//  protected void onDestroy() {
//    LocalBroadcastManager.getInstance(this).unregisterReceiver(ExitReceiver);
//    super.onDestroy();
//  }
//
//  @Override
//  protected void onSaveInstanceState(Bundle outState) {
//    // No call for super(). Bug on API Level > 11.
//  }
//
//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    /* prepare option on action bar */
//    super.onCreateOptionsMenu(menu);
//    return true;
//  }
//
//  @Override
//  public void onRestart() {
//    super.onRestart();
//
//    if (mViewPager == null)
//      return;
//  }
//
//}
