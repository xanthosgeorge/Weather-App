import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

//retrieve weather data from api - this backend logic will fetch the latest weather
// data from the external API and return it. The GUI will display this data
// to the user
public class WeatherApp {

    //fetch weather data for the given location
    public static JSONObject getWeatherData(String locationName){

        //get location coordinated using geolocation API
        JSONArray locationData = getLocationData(locationName);

        //extract latitude and longtitude data
        JSONObject locationObject = (JSONObject) locationData.get(0);
        double latitude = (Double) locationObject.get("latitude");
        double longitude = (Double) locationObject.get("longitude");

        //build API request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&daily=uv_index_max&timezone=Africa%2FCairo";
        try {
            //Call Api and get response
            HttpURLConnection conn = fetchAPIResponse(urlString);
            if (conn.getResponseCode() != 200) {
                System.out.println("Error couldn't connect to API");
                return  null;
            }
             //store resulting Json Data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }

            scanner.close();
            conn.disconnect();

            //parse through our data
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(resultJson.toString());

            JSONObject hourly = (JSONObject) jsonObject.get("hourly");
            JSONObject daily = (JSONObject) jsonObject.get("daily");


            //we want to get current hour data
            // se we need to wed the index of current hour

            //fetch uv index from daily weather report
            JSONArray time = (JSONArray) hourly.get("time");
            int index =  findIndexOfCurrentTime(time);
            JSONArray time2 = (JSONArray)daily.get("time");
            int index2 = findIndexOfCurrentTime(time2);
            JSONArray uv_index_max = (JSONArray)daily.get("uv_index_max");
            double uv = (double)uv_index_max.get(index2);

            //ger temperature
            JSONArray tempratureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (Double) tempratureData.get(index);

            //get the weather code
            JSONArray weathercode =  (JSONArray) hourly.get("weather_code");
            String weatherCondition  = convertWeatherCode((long) weathercode.get(index));

            //get humidity
            JSONArray relativeHumidityData = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (Long) relativeHumidityData.get(index);

            //get windspeed
            JSONArray windSpeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windSpeed = (Double) windSpeedData.get(index);

            //build the weather json data object that we are getting to access in our frontend
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("wind_speed", windSpeed);
            weatherData.put("uv_index", uv);

            return weatherData;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    //retrieves geographic coordinates for given location name
    public  static JSONArray getLocationData(String locationName)
    {
        //replace any whitespace in location name to + to adhere to API request format
        locationName = locationName.replaceAll(" ", "+");

        //build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try
        {
            //Call the API and get response
            HttpURLConnection connection = fetchAPIResponse(urlString);

            //check response status
            // 200 means successful connection


            if (connection.getResponseCode() != 200){
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

                //parse the json string into Json obj
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(resultJson.toString());

                //get the list of location data the api generated form the location name
                return (JSONArray) jsonObject.get("results");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HttpURLConnection fetchAPIResponse(String urlString) {
        try {
            //attempt to create connection
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //set request method to get
            conn.setRequestMethod("GET");


            //connect to API
            conn.connect();
            return conn;
        }catch (IOException e)
        {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();

        //iterate throught the time list and see which one matches our current time
        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                return i;
            }
        }
        return 0;
    }

    public static String getCurrentTime() {
        // get currect date and Time
        LocalDateTime now = LocalDateTime.now();

        //format date to be 2025-01-26T00:00 (this is how it is read in the API)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        //format and print the currunt date and time

        String formattedDate = now.format(formatter);
        return formattedDate;
    }

    //convert the weather code to something more readable
    private static String convertWeatherCode(long weatherCode) {
        String weatherCondition = "";
        if (weatherCode ==0){
            weatherCondition = "Clear";
        }else if (weatherCode >0L && weatherCode <=3L){
            weatherCondition = "Cloudy";
        }else if (weatherCode >=51L && weatherCode <=67L ||
        weatherCode >=80L && weatherCode <=99L){
            weatherCondition = "Rain";

        }else if (weatherCode >=71L && weatherCode <=77L){
            weatherCondition = "Snow";
        }
        return weatherCondition;
    }
}
