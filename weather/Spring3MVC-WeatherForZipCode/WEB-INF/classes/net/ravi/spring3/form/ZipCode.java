package net.ravi.spring3.form;

import java.io.*;
import net.ravi.spring3.controller;
import org.springframework.beans.factory.annotation.Autowired;

public class ZipCode {
	private String zipCode;	
	
	public String getZipCode() {
		return zipCode;
	}

	@Autowired
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}
        public string GetWeather()
        {

                String cityStateTemp = "";
		try {
            
		int zip = Integer.parseInt(zipCode);
		if (zip < 501 || zip > 99950)
			throw new Exception("zip code not found");
		if (zipCode.length() != 5)
			throw new NumberFormatException();

            String weatherData = WeatherProxy.getWeatherForZip(zipcode);
            
            cityStateTemp = Parser.GetCityStateTemperature(weatherData);
		} catch (NumberFormatException e) {
			cityStateTemp = "invalid zip code format";
		} catch (Exception e) {
			cityStateTemp = "zip code not found";
		}
               return cityStateTemp
        }	
}
