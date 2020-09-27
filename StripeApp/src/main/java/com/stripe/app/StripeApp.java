
package com.stripe.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;



public class StripeApp {
    private static final Logger logger = LoggerFactory.getLogger(StripeApp.class);
    private static String apiPath = "v1/payment-intents/";
    private static String httpsURL = "https://api.stripe.com/";
    private static String clientId = Constants.DEFAULT_PUBLISHABLE;
    private static String clientSecret = Constants.DEFAULT_SECRET + ":";
    private static String accountId = Constants.DEFAULT_ACCOUNT;


    public static void main(String[] args) throws Exception {
    URL myurl = new URL(httpsURL+apiPath);
    HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("Content-Type","application/x-www- form-urlencoded"); 
    con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)"); 
    con.setRequestProperty("Authentication", "Bearer " + Base64.getEncoder().encodeToString(new String(clientSecret+":").getBytes()));
    con.setRequestProperty("Stripe-Account", accountId);
    con.setDoInput(true); 
    DataInputStream input = new DataInputStream( con.getInputStream() ); 
    for( int c = input.read(); c != -1; c = input.read() ) 
    System.out.print( (char)c ); 
    input.close(); 

    System.out.println("Resp Code:"+con .getResponseCode()); 
    System.out.println("Resp Message:"+ con .getResponseMessage()); 
   }

}

/*
Exception in thread "main" java.io.IOException: Server returned HTTP response code: 401 for URL: https://api.stripe.com/v1/payment-intents/
        at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1924)
        at java.base/sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1520)
        at java.base/sun.net.www.protocol.https.HttpsURLConnectionImpl.getInputStream(HttpsURLConnectionImpl.java:250)
        at com.stripe.app.StripeApp.main(StripeApp.java:33)
*/
