package widget.api;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import widget.model.WeatherData;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.io.IOException;

public class OpenMeteoClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public WeatherData fetch(String query) throws Exception {
        JSONObject geo = get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&format=json&name=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        Object results = geo.get("results");
        if (!(results instanceof JSONArray locations) || locations.isEmpty()) throw new IOException("Location not found. Try a city name.");
        try {
            JSONObject place = (JSONObject) locations.getFirst();
            JSONObject data = get("https://api.open-meteo.com/v1/forecast?latitude=" + number(place.get("latitude"))
                    + "&longitude=" + number(place.get("longitude"))
                    + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
                    + "&hourly=temperature_2m,weather_code&daily=uv_index_max&timezone=auto&forecast_days=2");
            return parse(data, place.get("name") + ", " + place.getOrDefault("country", ""), query);
        } catch (RuntimeException e) {
            throw new IOException("The weather service returned incomplete data. Please try again.", e);
        }
    }
    private JSONObject get(String url) throws Exception {
        HttpResponse<String> response;
        try {
            response = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) { throw new IOException("Cannot reach weather service. Check your connection and retry.", e); }
        if (response.statusCode() != 200) throw new IOException("Weather service unavailable (HTTP " + response.statusCode() + "). Try again shortly.");
        try {
            JSONObject data = (JSONObject) new JSONParser().parse(response.body());
            if (Boolean.TRUE.equals(data.get("error"))) throw new IllegalArgumentException();
            return data;
        } catch (Exception e) { throw new IOException("The weather service returned an invalid response. Please retry.", e); }
    }
    public static WeatherData parse(JSONObject data, String location, String query) {
        JSONObject c = (JSONObject) data.get("current"), h = (JSONObject) data.get("hourly"), d = (JSONObject) data.get("daily");
        ZoneId zone = ZoneId.of((String) data.get("timezone"));
        LocalDateTime observed = LocalDateTime.parse((String) c.get("time"));
        JSONArray times = (JSONArray) h.get("time"), temps = (JSONArray) h.get("temperature_2m"), codes = (JSONArray) h.get("weather_code");
        List<WeatherData.Hour> hours = new ArrayList<>();
        for (int i = 0; i < times.size() && hours.size() < 5; i++) {
            LocalDateTime time = LocalDateTime.parse((String) times.get(i));
            if (time.isAfter(observed) && temps.get(i) instanceof Number && codes.get(i) instanceof Number)
                hours.add(new WeatherData.Hour(time, number(temps.get(i)), (int) number(codes.get(i))));
        }
        Double uv = null;
        if (d != null && d.get("time") instanceof JSONArray dates && d.get("uv_index_max") instanceof JSONArray values) {
            int index = dates.indexOf(observed.toLocalDate().toString());
            if (index >= 0 && index < values.size()) uv = optional(values.get(index));
        }
        return new WeatherData(location, query, zone, observed, number(c.get("temperature_2m")), optional(c.get("apparent_temperature")),
                number(c.get("relative_humidity_2m")), number(c.get("wind_speed_10m")), uv, (int) number(c.get("weather_code")), List.copyOf(hours));
    }
    private static Double optional(Object value) { return value instanceof Number ? number(value) : null; }
    private static double number(Object value) {
        double result = ((Number) value).doubleValue();
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Invalid number");
        return result;
    }
}
