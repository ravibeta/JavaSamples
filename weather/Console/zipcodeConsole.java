import java.io.*;
import java.net.*;
import org.json.*;

/* Name of the class has to be "Main" only if the class is public. */
class Weather {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws java.lang.Exception {
		try {
		
			String zipCode = ReadInput();
			
			WeatherProxy proxy = new WeatherProxy();
			String weatherData = proxy.getWeatherForZip(zipCode);
			
			String cityStateTemp = Parser.GetCityStateTemperature(weatherData);
			System.out.println(cityStateTemp);
			
		} catch (NumberFormatException e) {
			System.out.println("invalid zip code format");
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	private static String ReadInput() throws Exception
	{
		String zipCode;
		DataInputStream inData = new DataInputStream(System.in);
		zipCode = inData.readLine();
		int zip = Integer.parseInt(zipCode);
		if (zip < 501 || zip > 99950)
			throw new Exception("zip code not found");
		if (zipCode.length() != 5)
			throw new NumberFormatException();
		return zipCode;
	}
}
