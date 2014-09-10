import org.json.JSONObject;


public class Parser {

	public static String GetCityStateTemperature(String weatherJson) throws Exception
	{
        // System.out.println(jsonContent);
        if (weatherJson.contains("error")) throw new Exception("zip code not found");
        JSONObject jObj = new JSONObject(weatherJson);
        if (jObj == null) throw new Exception("invalid zip code format"); 
        JSONObject obsv = jObj.getJSONObject("current_observation");
        if (obsv == null) throw new Exception("invalid zip code format");
        JSONObject location = obsv.getJSONObject("observation_location");
        if (location == null) throw new Exception("invalid zip code format");
        String res = "";
        res = "City:" + location.getString("city") + "\r\n" + "State:" + location.getString("state") + "\r\n" + "Temperature:" + obsv.getDouble("temp_f") + " F";
        return res;
	}
}
