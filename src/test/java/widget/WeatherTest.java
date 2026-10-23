package widget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import widget.api.OpenMeteoClient;
import widget.model.WeatherData;
import widget.repository.WeatherRepository;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WeatherTest {
    @TempDir Path temp;
    @Test void parsesLocationTimeAndFutureForecastWithMissingOptionalValues() throws Exception {
        JSONObject json = (JSONObject) new JSONParser().parse("""
            {"timezone":"Asia/Tokyo", "current":{"time":"2026-10-24T00:15","temperature_2m":18,
            "relative_humidity_2m":70,"wind_speed_10m":2.5,"weather_code":45},
            "hourly":{"time":["2026-10-24T00:00","2026-10-24T01:00"],"temperature_2m":[17,18.5],"weather_code":[0,3]},
            "daily":{"time":["2026-10-23","2026-10-24"],"uv_index_max":[9,2.5]}}
            """);
        WeatherData d = OpenMeteoClient.parse(json, "Tokyo, Japan", "Tokyo");
        assertEquals(ZoneId.of("Asia/Tokyo"), d.zone());
        assertEquals(2.5, d.uv()); assertNull(d.feelsLike());
        assertEquals(1, d.forecast().size()); assertEquals(1, d.forecast().getFirst().time().getHour());
        assertEquals("Fog", WeatherData.condition(d.code()));
    }
    @Test void readsLegacySchemaAndPreservesFractionalTemperatures() throws Exception {
        Path db = temp.resolve("history.sqlite");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE WeatherSearch (CountryName VARCHAR(100), Weather VARCHAR(100), Temperature INT)");
            s.execute("INSERT INTO WeatherSearch VALUES ('Athens', 'Clear', 20)");
        }
        WeatherRepository repo = new WeatherRepository(db);
        repo.save(new WeatherData("Athens", "Athens", ZoneId.of("Europe/Athens"), LocalDateTime.now(), 21.5, null, 50, 5, null, 0, List.of()));
        var recent = repo.recent();
        assertEquals(1, recent.size()); assertEquals(2, recent.getFirst().searches());
        assertEquals(20.75, recent.getFirst().average());
    }
    @Test void distinguishesSnowShowersAndThunderstorms() {
        assertEquals("Snow", WeatherData.condition(86)); assertEquals("snow", WeatherData.icon(86));
        assertEquals("Thunderstorm", WeatherData.condition(95)); assertEquals("Unknown conditions", WeatherData.condition(-1));
    }
}
