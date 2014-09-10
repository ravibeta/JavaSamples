package net.ravi.spring3;

import java.io.*; 
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.beans.factory.annotation.*;

@Configuration 
public class App {
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"SpringBeans.xml");
 
		ZipCode obj = (ZipCode) context.getBean("zipCode");
		System.out.println(obj.GetWeather()); 
	}


}