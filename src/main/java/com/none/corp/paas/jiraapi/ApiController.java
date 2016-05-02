package jiraapi;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

@RestController
public class ApiController {
   static String CLOUD_API_AUTH_URL = "https://jira:35357/v3/auth/tokens";
   static String CLOUD_API_URL = "https://jira:5000";
   static String CLOUD_API_ADMIN_URL = "https://jira:35357";
   static String CLOUD_API_ADMIN = "";
   static String CLOUD_API_PASSWORD = "";
   static String CLOUD_API_TENANT = "";

    private static final Logger logger = Logger.getLogger(Application.class);
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/api")
    public Api api(@RequestParam(value="name", defaultValue="World") String name) {
        return new Api(counter.incrementAndGet(),
                            String.format(template, name));
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
    @RequestMapping("/user")
    public String createUser(@RequestParam(value="email", defaultValue="noone@none.com") String email,

           @RequestParam(value="username", defaultValue="rajamani") String username,
           @RequestParam(value="password", defaultValue="") String password,
           @RequestParam(value="tenantname", defaultValue="RajamaniDev") String tenantname) {
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

    @RequestMapping("/project")
    public String createProject(@RequestParam(value="name", defaultValue="newproject") String name,
           @RequestParam(value="description", defaultValue="rajamani") String description,
           @RequestParam(value="enabled", defaultValue="") String ifenabled,
           @RequestParam(value="username", defaultValue="username") String username) {
    boolean enabled = ifenabled != "";
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

    @RequestMapping("/userinfo")
    public String getUserInfo(@RequestParam(value="name", defaultValue="rajamani") String name) {
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

    @RequestMapping("/projectinfo")
    public String getProjectInfo(@RequestParam(value="name", defaultValue="RajamaniDev") String name) {
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
