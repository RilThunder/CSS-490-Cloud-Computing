/**
 * @author Thuan Tran
 * @date January 25th, 2018
 * Lab 2: This lab is to create a client that consumes a REST API and display the result about the weather
 */

// Parse the response received from the service

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

// Rest client to make the request
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

// Exceptions that might happen for bad input/output
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

public class WeatherClient {

    private static final String SERVICE_ADDRESS = "https://api.openweathermap.org/data/2.5/weather";
    private static final String API_KEY = "499ac99f0e38ebb0a4295e38bbb55c9a";

    /**
     * WeatherClient entry point of the program. Takes in a city id and provide information about the city
     *
     * @param args a command line argument that has 1 argument which is the city id
     */
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Invalid input detected. Please only enter a city id number");
                return;
            }
            HttpClient client = HttpClientBuilder.create().build();
            URIBuilder uriBuilder = new URIBuilder(SERVICE_ADDRESS);
            uriBuilder.setParameter("id", args[0])
                    .setParameter("units", "metric") // Nice feature, using Celsius
                    .setParameter("APPID", API_KEY);
            HttpGet request = new HttpGet(uriBuilder.build());
            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                System.out.println("Unsuccessful request. Status code is not 200");
                return;
            }
            displayWeather(response);
        } catch (IOException ex) {
            System.out.println("Unable to make request to the service");
        } catch (URISyntaxException exception) {
            System.out.println("Bad URI created to provide to the request");
        } catch (IllegalArgumentException exception) {
            System.out.println("Invalid URI created by having bad input");
        } // end catch
    } // end main method

    /**
     * This is a private helper method to display the result of the request
     * It will parse the result and print out the temperature of the city in Celsius
     *
     * @param response the HTTPResponse that the client received
     */
    private static void displayWeather(HttpResponse response) {
        try {
            JsonParser responseParser = new JsonParser();
            InputStreamReader responseJsonStream = new InputStreamReader(response.getEntity()
                    .getContent(), "UTF-8");
            JsonObject result = responseParser.parse(responseJsonStream)
                    .getAsJsonObject();
            String cityName = result.get("name").getAsString();
            // Within the "main" elements, there are also different results. We are only interested in the
            // temperature. In addition, the "weather" element is a weird array (instead of Json key value)
            // and we are only interested in the weather description
            JsonObject temperatureInfo = result.get("main").getAsJsonObject();
            String temperature = temperatureInfo.get("temp").getAsString() + " degree Celsius ";
            String weatherDescription = result.get("weather").getAsJsonArray().get(0).getAsJsonObject()
                    .get("description").getAsString();
            System.out.println("The weather for " + cityName + " is " + temperature + "with " + weatherDescription);
        } catch (IOException ex) {
            System.out.println("Unable to create a stream to read the response");
        } catch (NullPointerException exception) {
            System.out.println("Elements do not exist in the parsed result");
        } catch (JsonParseException exception) {
            System.out.println("Unable to parse the result that received into a Json Object");
        } // end catch
    } // end displayWeather method
} // end WeatherClient class
