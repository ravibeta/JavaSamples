
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
    private static String apiPath = "v1/payment_intents";
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
    con.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(new String(clientSecret+":").getBytes()));
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
java -jar build//libs/StripeApp-1.0.jar
{
  "object": "list",
  "data": [

  ],
  "has_more": false,
  "url": "/v1/payment_intents"
}
Resp Code:200
Resp Message:OK
*/
