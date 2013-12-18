import java.net.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

public class Rextester {
     static String apiKey = "your_api_key_here";
     static String httpsURL =
         "https://test.openapi.starbucks.com/v1/oauth/token?api_key=";
     static String clientId = "your_client_id";
     static String clientSecret = "your_client_secret_here";


    public static void main(String[] args) throws Exception {

    URL myurl = new URL(httpsURL+apiKey);
    HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
    con.setRequestMethod("POST");

    String query = "grant_type=" + "client_credentials"; 
    query += "&";
    query += "client_id=" + clientId;
    query += "&";
    query += "client_secret=" + clientSecret;
    query += "&";
    query += "scope=" + "some_scope";
    query += "&";
    query += "state=" + "some_state";

    con.setRequestProperty("Content-length", String.valueOf(query.length())); 
    con.setRequestProperty("Content-Type","application/x-www- form-urlencoded"); 
    con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)"); 
    con.setDoOutput(true); 
    con.setDoInput(true); 


    DataOutputStream output = new DataOutputStream(con.getOutputStream());  


    output.writeBytes(query);

    output.close();

    DataInputStream input = new DataInputStream( con.getInputStream() ); 



    for( int c = input.read(); c != -1; c = input.read() ) 
    System.out.print( (char)c ); 
    input.close(); 

    System.out.println("Resp Code:"+con .getResponseCode()); 
    System.out.println("Resp Message:"+ con .getResponseMessage()); 
   }
}
