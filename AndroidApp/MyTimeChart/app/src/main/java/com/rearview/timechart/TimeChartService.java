//package com.rearview.timechart;
//
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.graphics.Color;
//import android.os.IBinder;
//import android.support.v7.app.NotificationCompat;
//import android.widget.RemoteViews;
//
//
//import com.rearview.timechart.settings.Settings;
//import com.rearview.timechart.settings.SettingsHelper;
//import java.nio.ByteBuffer;
//import java.util.Locale;
//
//public class TimeChartService extends Service implements ipcClientListener {
//  private static final int NOTIFYID = 20201226;
//  private IpcService ipcService = null;
//  private int UpdateInterval = 2;
//
//  private boolean isRegistered = false;
//  private NotificationManager nManager = null;
//  private NotificationCompat.Builder nBuilder = null;
//
//    private Settings settings = null;
//
//  @Override
//  public IBinder onBind(Intent intent) {
//    return null;
//  }
//
//  @Override
//  public void onCreate() {
//
//    super.onCreate();
//
//    settings = Settings.getInstance(this);
//
//    IpcService.Initialize(this);
//    ipcService = IpcService.getInstance();
//
//    refreshSettings();
//    initializeNotification();
//
//  }
//
//  private void refreshSettings() {
//    ipcService.createConnection();
//  }
//
//  @Override
//  public void onDestroy() {
//    endNotification();
//    endService();
//    super.onDestroy();
//  }
//
//  private void endNotification() {
//    nManager.cancel(NOTIFYID);
//    stopForeground(true);
//  }
//
//  private void initializeNotification() {
//
//    Intent notificationIntent = new Intent(this, MainActivity.class);
//    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
//    PendingIntent contentIntent = PendingIntent.getActivity(
//        this.getBaseContext(), 0, notificationIntent, 0);
//
//    nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//    nBuilder = new NotificationCompat.Builder(this);
//    nBuilder.setContentTitle(getResources().getText(R.string.ui_appname));
//    nBuilder.setContentText(getResources().getText(R.string.ui_shortcut_detail));
//    nBuilder.setOnlyAlertOnce(true);
//    nBuilder.setOngoing(true);
//    nBuilder.setContentIntent(contentIntent);
//    nBuilder.setCategory(Notification.CATEGORY_STATUS);
//
//    // Set infos as public
//    nBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
//
//    // Use new style icon for Lollipop
//    if (CoreUtil.isGreaterThanMarshmallow())
//      nBuilder.setSmallIcon(R.drawable.ic_stat_notify);
//    else
//      nBuilder.setSmallIcon(R.drawable.ic_launcher);
//
//    if (isSetTop)
//      nBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
//
//    Notification osNotification = nBuilder.build();
//    nManager.notify(NOTIFYID, osNotification);
//
//    // set foreground to avoid recycling
//    startForeground(NOTIFYID, osNotification);
//
//  }
//
//  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//    public void onReceive(Context context, Intent intent) {
//
//      if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
//        goSleep();
//      else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
//        wakeUp();
//    }
//  };
//
//  private void registerScreenEvent() {
//    IntentFilter filterScreenON = new IntentFilter(Intent.ACTION_SCREEN_ON);
//    registerReceiver(mReceiver, filterScreenON);
//
//    IntentFilter filterScreenOFF = new IntentFilter(Intent.ACTION_SCREEN_OFF);
//    registerReceiver(mReceiver, filterScreenOFF);
//  }
//
//  private void initService() {
//    if (!isRegistered) {
//      registerScreenEvent();
//      isRegistered = true;
//    }
//
//    wakeUp();
//  }
//
//  private void endService() {
//    if (isRegistered) {
//      unregisterReceiver(mReceiver);
//      isRegistered = false;
//    }
//
//    goSleep();
//  }
//
//  private void wakeUp() {
//    UpdateInterval = settings.getInterval();
//    byte newCommand[] = getReceiveDataType();
//    ipcService.removeRequest(this);
//    ipcService.addRequest(newCommand, 0, this);
//  }
//
//  private void goSleep() {
//    ipcService.removeRequest(this);
//  }
//
//  private void refreshNotification() {
//    Notification osNotification;
//
//    // set current iconLevel
//    // Fix: Android 6.0 issue
//    if (CoreUtil.isGreaterThanMarshmallow()) {
//      int iconLevel = 0;
//      osNotification = nBuilder.setSmallIcon(iconColor, iconLevel).build();
//    }
//    else {
//      osNotification = nBuilder.build();
//    }
//
//    osNotification.contentView = new RemoteViews(getPackageName(), R.layout.ui_notification);
//
//    // set contentIntent to fix
//    // "android.app.RemoteServiceException: Bad notification posted from package"
//    Intent notificationIntent = new Intent(this, MainActivity.class);
//    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
//    osNotification.contentIntent = PendingIntent.getActivity( this.getBaseContext(), 0, notificationIntent, 0);
//    nManager.notify(NOTIFYID, osNotification);
//  }
//
//}
