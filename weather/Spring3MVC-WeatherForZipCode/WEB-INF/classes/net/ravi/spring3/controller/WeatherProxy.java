package net.ravi.spring3.controller;
import java.io.*;
import java.net.*;

public class WeatherProxy {
	static String apiKey = "ed044d75b91fb500";
	static String httpURL = "http://api.wunderground.com/api/";
	static String resource = "/conditions/q/";
	
	public static String getWeatherForZip(String zip) throws Exception
	{
		URL myurl = new URL(httpURL + apiKey + resource + zip + ".json");
		HttpURLConnection con = (HttpURLConnection) myurl
				.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent",
				"Mozilla/4.0 (compatible; MSIE 5.0;Windows98;DigExt)");
		con.setDoOutput(true);
		con.setDoInput(true);
		if (con.getResponseCode() != 200)
		{
			throw new Exception("zip code not found");
		}
		
		InputStream is = con.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            		is, "iso-8859-1"), 8);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
             sb.append(line + "\n");
        }
        is.close();
        String jsonContent = sb.toString();
        return jsonContent;
	}
}
