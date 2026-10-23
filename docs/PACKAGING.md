# Desktop packaging

Build on the target operating system using JDK 21+ and Maven 3.9+. Maven resolves platform-specific JavaFX dependencies, so native packages should be built separately on each target OS.

```sh
mvn clean package
mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package/lib
```

Copy the application JAR into the input directory:

```sh
# macOS / Linux
cp target/weather-widget-1.0.0.jar target/package/
```

On Windows, use PowerShell:

```powershell
Copy-Item target/weather-widget-1.0.0.jar target/package/
```

Create an app image using the JDK's `jpackage` tool:

```sh
jpackage --type app-image --name WeatherWidget --input target/package --main-jar weather-widget-1.0.0.jar --main-class widget.app.AppLauncher --dest target/native --add-modules java.sql,java.net.http,java.prefs,java.desktop,jdk.unsupported
```

The image contains a Java runtime, so end users do not need to install Java separately. On macOS, launch `target/native/WeatherWidget.app`.

Run the resulting image before producing an installer. To create an installer, replace `--type app-image` with an OS-supported format (`dmg`, `msi`, or `deb`) and use a fresh output directory. Installer formats may require additional platform tools. Signing, notarization, and cross-platform installer verification are separate distribution steps.

[Back to README](../README.md)
