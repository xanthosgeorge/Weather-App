package widget.model;

import java.time.*;
import java.util.List;

public record WeatherData(String location, String query, ZoneId zone, LocalDateTime observed,
        double temperature, Double feelsLike, double humidity, double wind, Double uv, int code,
        List<Hour> forecast) {
    public record Hour(LocalDateTime time, double temperature, int code) {}
    public static String condition(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55, 56, 57 -> "Drizzle";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain";
            case 71, 73, 75, 77, 85, 86 -> "Snow";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown conditions";
        };
    }
    public static String icon(int code) {
        if (code == 0) return "clear";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "snow";
        if (code >= 51 && code <= 67 || code >= 80) return "rain";
        return "cloudy";
    }
}
