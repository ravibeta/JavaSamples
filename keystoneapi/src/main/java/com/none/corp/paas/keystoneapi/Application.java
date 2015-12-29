package keystoneapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

@SpringBootApplication
public class Application {
   private static final Logger logger = Logger.getLogger(Application.class);
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
