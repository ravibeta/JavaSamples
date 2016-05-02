package MyTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class MyTest {
   static String CLOUD_API_AUTH_URL = "https://localkeystone:35357/v3/auth/tokens";
   static String CLOUD_API_URL = "https://localkeystone:5000";
   static String CLOUD_API_ADMIN_URL = "https://localkeystone:35357";
   static String CLOUD_API_ADMIN = "some_user";
   static String CLOUD_API_PASSWORD = "some_password";
   static String CLOUD_API_TENANT = "some_tenant";
   private static final Logger logger = Logger.getLogger(MyTest.class);

   public static void main(String[] args) {
      PropertyConfigurator.configure("log4j.properties");
      try {
          System.out.println(getUserInfo("some_user"));
      }catch(HttpClientErrorException e){
          assert e.toString().contains("403 Forbidden");
      }
      try {
          System.out.println(getProjectInfo("some_userDev"));
      }catch(HttpClientErrorException e){
          assert e.toString().contains("403 Forbidden");
      }
      try {
          System.out.println(createUser("some_user@xyz.com", CLOUD_API_ADMIN, CLOUD_API_PASSWORD, CLOUD_API_TENANT));
      }catch(HttpClientErrorException e){
          assert e.toString().contains("403 Forbidden");
      }
      try {
          System.out.println(createProject("some_userDev", "Existing project", true, "some_user"));
      }catch(HttpClientErrorException e){
          assert e.toString().contains("403 Forbidden");
      }
      System.out.println("Success");
      //System.out.println("Hello, World");
      //System.out.println(getClusters());
      //System.out.println(getUserInfo("some_user"));
      //System.out.println(getProjectInfo("some_userDev"));
      //System.out.println(createUser("some_user@xyz.com", CLOUD_API_ADMIN, CLOUD_API_PASSWORD, CLOUD_API_TENANT));
      //System.out.println(createProject("some_userDev", "Existing project", true, "some_user"));
   }

private static String getToken(String tenantname, String username, String password){
    String url = CLOUD_API_ADMIN_URL + "/v2.0/tokens";
    String params = "{\"auth\": {\"tenantName\": \"" + tenantname  + "\", \"passwordCredentials\": {\"username\": \"" + username + "\", \"password\": \"" + password + "\"}}}";
    System.out.println(params);
    HttpHeaders requestHeaders=new HttpHeaders();
    String auth = username + ":" + password;
    Base64.Encoder encoder = Base64.getEncoder();
    String encodedAuth = encoder.encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    String authHeader = "Basic " + encodedAuth; //new String( encodedAuth );
    requestHeaders.set( "Authorization", authHeader );
    requestHeaders.set("Content-Type","application/json");
    RestTemplate rest = new RestTemplate();
    String text = rest.postForObject(url,
            new HttpEntity(params, requestHeaders), String.class);
    System.out.println("text="+text);
    ReadContext response = JsonPath.parse(text);
    return text;
}
private static String createUser(String email, String username, String password, String tenantname){
    String tokentext  = getToken(CLOUD_API_TENANT, CLOUD_API_ADMIN, CLOUD_API_PASSWORD);
    ReadContext tokenresponse  =  JsonPath.parse(tokentext);
    String token = tokenresponse.read("$.access.token.id");
    String url = CLOUD_API_ADMIN_URL + "/v2.0/users";
    String params= "{\"user\": {\"email\": \"" + email + "\",\"password\": \"" + password + "\",\"enabled\": true,\"name\": \"" + username + "\"}}";
    HttpHeaders requestHeaders=new HttpHeaders();
    requestHeaders.set("Content-Type","application/json");
    requestHeaders.set("X-Auth-Token",token);
    RestTemplate rest = new RestTemplate();
    ReadContext response = JsonPath.parse(rest.postForObject(url,
            new HttpEntity(params, requestHeaders), String.class));
    return response.toString();
}

private static String createProject(String name, String description, boolean enabled, String username){
    String tokentext  = getToken(CLOUD_API_TENANT, CLOUD_API_ADMIN, CLOUD_API_PASSWORD);
    ReadContext tokenresponse  =  JsonPath.parse(tokentext);
    String token = tokenresponse.read("$.access.token.id");
    String url = CLOUD_API_ADMIN_URL + "/v2.0/tenants";
    String params="{\"tenant\":{\"name\": \"" + name + "\",\"description\": \"" + description + "\",\"enabled\": \"" + enabled + "\"}}";
    HttpHeaders requestHeaders=new HttpHeaders();
    requestHeaders.set("Content-Type","application/json");
    requestHeaders.set("X-Auth-Token",token);
    RestTemplate rest = new RestTemplate();
    ReadContext response = JsonPath.parse(rest.postForObject(url,
            new HttpEntity(params, requestHeaders), String.class));
    return response.toString();
} 

private static String getUserInfo(String name){
    String tokentext  = getToken(CLOUD_API_TENANT, CLOUD_API_ADMIN, CLOUD_API_PASSWORD);
    System.out.println("tokentext="+tokentext);
    ReadContext tokenresponse  =  JsonPath.parse(tokentext);
    String token = tokenresponse.read("$.access.token.id");
    String url = CLOUD_API_ADMIN_URL + "/v2.0/users?name="+name;
    HttpHeaders requestHeaders=new HttpHeaders();
    requestHeaders.set("Content-Type","application/json");
    requestHeaders.set("X-Auth-Token",token);
    RestTemplate rest = new RestTemplate();
    HttpEntity entity = new HttpEntity(requestHeaders);
    //RestTemplate rest = new RestTemplate();
    HttpEntity<String> response = rest.exchange(
                   url, HttpMethod.GET, entity, String.class);
    return response.getBody().toString();
}

private static String getProjectInfo(String name){
    String tokentext  = getToken(CLOUD_API_TENANT, CLOUD_API_ADMIN, CLOUD_API_PASSWORD);
    ReadContext tokenresponse  =  JsonPath.parse(tokentext);
    String token = tokenresponse.read("$.access.token.id");
    String url = CLOUD_API_ADMIN_URL + "/v2.0/tenants?name="+name;
    HttpHeaders requestHeaders=new HttpHeaders();
    requestHeaders.set("Content-Type","application/json");
    requestHeaders.set("X-Auth-Token",token);
    RestTemplate rest = new RestTemplate();
    HttpEntity entity = new HttpEntity(requestHeaders);
    HttpEntity<String> response = rest.exchange(
                   url, HttpMethod.GET, entity, String.class);
    return response.getBody().toString();
}

} // class
