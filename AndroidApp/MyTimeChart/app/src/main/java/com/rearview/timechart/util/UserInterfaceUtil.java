package com.rearview.timechart.util;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;

import com.rearview.timechart.R;
import com.rearview.timechart.core.connectionStatus;
import com.rearview.timechart.core.connectionType;
import com.rearview.timechart.ipc.ipcCategory;
import com.rearview.timechart.settings.Settings;

public class UserInterfaceUtil {

  // internal variable 
  private static Settings settings = null;
  private static Resources resource = null;

  /**
   * Initialize rearview.timechart
   * @param activity
   */
  public static void Initialize(Activity activity) {
    if (settings == null)
      settings = Settings.getInstance(activity.getApplicationContext());
    if (resource == null)
      resource = activity.getResources();
  }
  
  /**
   * get connection type by connectionType
   * @param byte
   * @return String connection type 
   */
  public static String getConnectionType(byte type) {
    switch (type) {
    case connectionType.TCPv4:
      return "TCP4";
    case connectionType.TCPv6:
      return "TCP6";
    case connectionType.UDPv4:
      return "UDP4";
    case connectionType.UDPv6:
      return "UDP6";
    case connectionType.RAWv4:
      return "RAW4";
    case connectionType.RAWv6:
      return "RAW6";
    }
    return "????";
  }
  
  /**
   * get connection status by connectionStatus
   * @param byte
   * @return connection status
   */
  public static String getConnectionStatus(byte status) {
    switch (status) {
    case connectionStatus.CLOSE:
      return "CLOSE";
    case connectionStatus.CLOSE_WAIT:
      return "CLOSE_WAIT";
    case connectionStatus.CLOSING:
      return "CLOSING";
    case connectionStatus.ESTABLISHED:
      return "ESTABLISHED";
    case connectionStatus.FIN_WAIT1:
      return "FIN_WAIT1";
    case connectionStatus.FIN_WAIT2:
      return "FIN_WAIT2";
    case connectionStatus.LAST_ACK:
      return "LAST_ACK";
    case connectionStatus.LISTEN:
      return "LISTEN";
    case connectionStatus.SYN_RECV:
      return "SYN_RECV";
    case connectionStatus.SYN_SENT:
      return "SYN_SENT";
    case connectionStatus.TIME_WAIT:
      return "TIME_WAIT";
    }
    return "UNKNOWN";
  }
  
  /**
   * combine IP and port as a string
   * @param String ip
   * @param integer port
   * @return String
   */
  public static String convertToIPv4(String ip, int port) {
    // replace IPv6 to IPv4
    ip = ip.replace("::ffff:", "");
    if (port == 0)
      return ip + ":*";
    return ip + ":" + port;
  }


  /**
   * convert string as Integer
   * 
   * @param string
   * @return int
   */
  public static int convertToInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
    }
    return 0;
  }

  /**
   * remove string from array , if it can't be converted to int
   * 
   * @param string
   *          []
   * @return string []
   */
  public static String[] eraseNonIntegarString(String[] data) {
    ArrayList<String> checked = new ArrayList<String>();
    for (int index = 0; index < data.length; index++) {
      if (convertToInt(data[index]) != 0)
        checked.add(data[index]);
    }
    return checked.toArray(new String[checked.size()]);
  }

  /**
   * remove empty string from array
   * 
   * @param string
   *          []
   * @return string []
   */
  public static String[] eraseEmptyString(String[] data) {
    ArrayList<String> checked = new ArrayList<String>();
    for (int index = 0; index < data.length; index++) {
      if (!data[index].trim().isEmpty())
        checked.add(data[index]);
    }
    return checked.toArray(new String[checked.size()]);
  }

}
