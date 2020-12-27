package com.rearview.timechart.settings;

import android.content.Context;

public class Settings {
  // singleton
  private static Settings instance = null;

  private SettingsHelper helper = null;
  public final static String SESSION_SECTION = "session_storage";

  /**
   * get an instance for settings
   * 
   * @param context
   * @return settings object
   */
  public static Settings getInstance(Context context) {
    if (instance == null) {
      instance = new Settings(context);
    }
    return instance;
  }

  /**
   * construct
   * 
   * @param context
   */
  private Settings(Context context) {
    helper = new SettingsHelper(context);
  }

  /**
   * set a security token
   * 
   * @param token
   */
  public void setToken(String token) {
    helper.setString("token", token);
  }

  /**
   * get the security token
   * 
   * @return token
   */
  public String getToken() {
    if (helper.getString("token", "").length() == 0)
      setToken(java.util.UUID.randomUUID().toString());
    return helper.getString("token", "");
  }
  /**
   * set session value
   * 
   * @param value
   */
  public void setSessionValue(String value) {
    helper.setString(SESSION_SECTION, value);
  }

  /**
   * get session value
   * 
   * @return value
   */
  public String getSessionValue() {
    String value = helper.getString(SESSION_SECTION, "");
    helper.setString(SESSION_SECTION, "");
    return value;
  }
}
