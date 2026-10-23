package widget.service;

import java.util.prefs.Preferences;

public class SettingsService {
    private final Preferences prefs = Preferences.userRoot().node("dev/weather/widget");
    public String get(String key, String fallback) { return prefs.get(key, fallback); }
    public void put(String key, String value) { prefs.put(key, value); }
    public double number(String key, double fallback) {
        return Math.max(0, Math.min(1, prefs.getDouble(key, fallback)));
    }
    public void setNumber(String key, double value) { prefs.putDouble(key, value); }
}
