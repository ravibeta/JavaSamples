package net.ravi.spring3.controller;

import net.ravi.spring3.form.ZipCode;
import java.io.*;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

@Controller
@SessionAttributes
public class WeatherController {

	@RequestMapping(value = "/addZipCode", method = RequestMethod.POST)
	public String addZipCode(@ModelAttribute("zipcode")
							ZipCode zipcode, BindingResult result) {
		
		WeatherReport report = new WeatherReport();
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

		report.setWeatherReport(cityStateTemp);
		return new ModelAndView("WeatherReport", "WeatherReport", report);
	}	
}
