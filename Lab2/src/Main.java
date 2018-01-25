import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static String SERVICE_ADDRESS = "https://api.openweathermap.org/data/2.5/";
    public static String API_KEY = "&APPID=499ac99f0e38ebb0a4295e38bbb55c9a";
    public static String WEATHER_PARAMETER = "weather?q=London";

    public static void main(String[] args) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(SERVICE_ADDRESS + WEATHER_PARAMETER + API_KEY);
        try {
            HttpResponse response = client.execute(request);
            JsonParser parser = new JsonParser();
            JsonObject result = parser.parse(new InputStreamReader(response.getEntity().getContent(), "UTF-8")).getAsJsonObject();
            System.out.println(result);
        } catch (IOException ex) {
            System.out.println("Exception");
        }
    }
}
