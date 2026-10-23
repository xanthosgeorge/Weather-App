package widget.repository;

import widget.model.WeatherData;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Retains the original schema and imports a legacy database once without altering it. */
public class WeatherRepository {
    private final Path path;
    public WeatherRepository(Path path) { this.path = path; }
    private Connection open() throws Exception {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path legacy = Path.of("identifier.sqlite");
        if (!Files.exists(path) && Files.isRegularFile(legacy)) Files.copy(legacy, path);
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS WeatherSearch (CountryName TEXT, Weather TEXT, Temperature REAL)");
        } catch (Exception e) { c.close(); throw e; }
        return c;
    }
    public void save(WeatherData data) throws Exception {
        try (Connection c = open(); PreparedStatement p = c.prepareStatement("INSERT INTO WeatherSearch VALUES (?, ?, ?)")) {
            p.setString(1, data.query()); p.setString(2, WeatherData.condition(data.code())); p.setDouble(3, data.temperature()); p.executeUpdate();
        }
    }
    public record Recent(String query, int searches, double average) {
        @Override public String toString() { return query; }
    }
    public List<Recent> recent() throws Exception {
        List<Recent> result = new ArrayList<>();
        try (Connection c = open(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                "SELECT CountryName, COUNT(*), AVG(Temperature) FROM WeatherSearch WHERE TRIM(CountryName) <> '' GROUP BY CountryName COLLATE NOCASE ORDER BY MAX(rowid) DESC LIMIT 12")) {
            while (r.next()) result.add(new Recent(r.getString(1), r.getInt(2), r.getDouble(3)));
        }
        return result;
    }
}
