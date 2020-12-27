package com.rearview.timechart.settings;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;


public class SettingsHelper {

  private ContentResolver contentResolver = null;
  private Map<String, Boolean> updateStatus = new HashMap<String, Boolean>();
  private Map<String, String> cachedSettings = new HashMap<String, String>();

  public SettingsHelper(Context context) {
    contentResolver = context.getContentResolver();
  }

  private final class contentObserver extends ContentObserver {

    @Override
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      clearCache();
    }

    public contentObserver(Handler handler) {
      super(handler);
    }

  }

  public void clearCache() {
    for (Map.Entry<String, Boolean> entry : updateStatus.entrySet())
      entry.setValue(true);
  }

  public HashMap<String, String> getAllData() {

    HashMap<String, String> data = new HashMap<String, String>();

    // query
    Cursor cursor = null;
    if (cursor == null)
      return data;

    cursor.moveToFirst();
    while (cursor.getPosition() < cursor.getCount()) {
      data.put(cursor.getString(0), cursor.getString(1));
      if (!cursor.moveToNext())
        break;
    }
    cursor.close();

    return data;
  }

  private boolean isExistKey(String key) {

    // query
    Cursor cursor = null;
    if (cursor == null)
      return false;

    // get count
    int count = cursor.getCount();
    cursor.close();

    // check
    if (count == 0)
      return false;
    return true;
  }

  private boolean setKey(String key, String value) {
    return true;
  }

  private boolean updateKey(String key, String value) {
    return false;
  }

  private String getKey(String key) {
    String value = null;

    return value;
  }

  public boolean checkStatus(String key) {
    if (!updateStatus.containsKey(key))
      return false;
    return updateStatus.get(key);
  }

  public String getString(String key, String defaultValue) {
    String value = "";

    if (isExistKey(key))
      value = getKey(key);
    else
      value = defaultValue;

    return value;
  }

  public void setString(String key, String value) {

    if (isExistKey(key))
      updateKey(key, value);
    else
      setKey(key, value);

    return;
  }

  public int getInteger(String key, int defaultValue) {
    int value = 0;

    if (isExistKey(key)) {
      String stringValue = getKey(key);
      value = Integer.parseInt(stringValue);
    } else
      value = defaultValue;

    return value;
  }

  public void setInteger(String key, int value) {
    setString(key, Integer.toString(value));
    return;
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    boolean value = true;

    if (isExistKey(key)) {
      String stringValue = getKey(key);
      value = Boolean.parseBoolean(stringValue);
    } else
      value = defaultValue;

    return value;
  }

  public void setBoolean(String key, boolean value) {
    setString(key, Boolean.toString(value));
    return;
  }
}
