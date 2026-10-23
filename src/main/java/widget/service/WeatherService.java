package widget.service;

import widget.api.OpenMeteoClient;
import widget.model.WeatherData;
import widget.repository.WeatherRepository;

public class WeatherService {
    private final OpenMeteoClient api;
    private final WeatherRepository repository;
    public WeatherService(OpenMeteoClient api, WeatherRepository repository) { this.api = api; this.repository = repository; }
    public record Result(WeatherData weather, String warning) {}
    public Result search(String query) throws Exception {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Enter a city to search.");
        WeatherData data = api.fetch(query.strip());
        String warning = "";
        try { repository.save(data); } catch (Exception e) { warning = "Weather loaded. Search history could not be saved."; }
        return new Result(data, warning);
    }
}
