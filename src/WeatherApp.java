import netscape.javascript.JSObject;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Scanner;

//retrieve weather data from api - this backend logic will fetch the latest weather
// data from the external API and return it. The GUI will display this data
// to the user
public class WeatherApp {

    //fetch weather data for the given location
    public static JSObject getWeatherData(String locationName){

        //get location coordinated using geolocation API
        JSONArray locationData = getLocationData(locationName);
    }
    //retrieves geographic coordinates for given location name
    private  static JSONArray getLocationData(String locationName)
    {
        //replace any whitespace in location name to + to adhere to API request format
        locationName = locationName.replaceAll("", "+");

        //build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try
        {
            //Call the API and get response
            HttpURLConnection connection = fetchAPIResponse(urlString);

            //check response status
            // 200 means successful connection

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                System.out.println("Error: Couldn't connect to API");
                return null;
            }else {
                //store API results
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(connection.getInputStream());
                while (scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                //close scanner
                scanner.close();

                //close url connection
                connection.disconnect();

                //parse json
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static HttpURLConnection fetchAPIResponse(String urlString) {
        try {
            //attempt to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            //set request method to get
            conn.setRequestMethod("GET");


            //connect to API
            conn.connect();
            return conn;
        }catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
