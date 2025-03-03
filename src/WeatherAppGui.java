import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class WeatherAppGui extends JFrame {

    private JSONObject weatherData;
    private Connection connection;
    private List<WeatherData> weatherDataList = new ArrayList<WeatherData>();

    private static  WeatherAppGui instance;

    public static WeatherAppGui getInstance() {
        if (instance == null) {
            // If the instance doesn't exist, create it
            instance = new WeatherAppGui();
        }
        return instance;
    }

    private WeatherAppGui() {
        //set up our gui and add a title
        super("Weather App");

        //configure gui to end the program's process once it has been closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        //set the size of gui in (in pixels)
        setSize(450, 732);


        //load our gui at the center of the screen
        setLocationRelativeTo(null);
        setLayout(null);

        //prevent any resize of the gui
        setResizable(false);


        // Initialize database connection
        initializeDatabaseConnection();


        addGuidComponents();
    }

    private void addGuidComponents()
    {
        //search field
        JTextField searchField = new JTextField();

        //set the location and the size of the component
        searchField.setBounds(15,15,351,45);

        searchField.setFont(new Font("Dialog", Font.PLAIN, 24));
        add(searchField);




        JLabel weatherConditionImage = new JLabel(loadImage("src/assets/cloudy.png"));
        weatherConditionImage.setBounds(0,125,450,217);
        add(weatherConditionImage);


        //temperature text
        JLabel temperatureText = new JLabel("10 C");
        temperatureText.setBounds(0,350,450,45);
        temperatureText.setFont(new Font("Dialog", Font.BOLD, 48));

        //center the text
        temperatureText.setHorizontalAlignment(SwingConstants.CENTER);
        add(temperatureText);

        //weather condition description
        JLabel weatherConditionsDescription = new JLabel("Cloudy");
        weatherConditionsDescription.setBounds(0,405,450,36);
        weatherConditionsDescription.setFont(new Font("Dialog", Font.PLAIN, 32));
        weatherConditionsDescription.setHorizontalAlignment(SwingConstants.CENTER);
        add(weatherConditionsDescription);


        //humidity image
        JLabel humidityImage = new JLabel(loadImage("src/assets/humidity.png"));
        humidityImage.setBounds(15,500,74,66);
        add(humidityImage);


        //application icon
        ImageIcon icon = new ImageIcon("src/assets/cloudy.png");
        this.setIconImage(icon.getImage());

        //humidity Text
        JLabel humidityText = new JLabel("<html><b>Humidity</b> 100%</html>");
        humidityText.setBounds(90, 500, 85,55);
        humidityText.setFont(new Font("Dialog", Font.PLAIN, 16));
        add(humidityText);


        //windspeed inmage
        JLabel windspeedImage = new JLabel(loadImage("src/assets/windspeed.png"));
        windspeedImage.setBounds(220,500,74,66);
        add(windspeedImage);


        //windspeed text
        JLabel winspeedText = new JLabel("<html><b>Wind</b> 15km</html>");
        winspeedText.setBounds(310,500, 83,55);
        winspeedText.setFont(new Font("Dialog", Font.PLAIN, 16));
        add(winspeedText);


        // UV index text
        JLabel uvIndexText = new JLabel("<html><b>UV Index</b> 5</html>");
        uvIndexText.setBounds(150, 600, 85, 55);
        uvIndexText.setFont(new Font("Dialog", Font.PLAIN, 16));
        add(uvIndexText);


        //search button
        JButton searchButton = new JButton(loadImage("src/assets/search.png"));


        //change the cursor to a hand cursor when hovering over this button
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.setBounds(375,15,47,45);
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // get location from user
                String userInput = searchField.getText();
                // validate input  - remove whitespace to ensure not-empty text
                if (userInput.replaceAll("\\s","").length() <=0) {
                    return;
                }

                //retrieve weather data
                weatherData = WeatherApp.getWeatherData(userInput);
                if (weatherData == null) {
                    return ;
                }

                //update gui
                //update weather image
                String weatherCondition = weatherData.get("weather_condition").toString();

                //depending on the condition we will update the weather image
                switch (weatherCondition) {
                    case "Clear":
                        weatherConditionImage.setIcon(loadImage("src/assets/clear.png"));
                        break;
                    case "Cloudy":
                        weatherConditionImage.setIcon(loadImage("src/assets/cloudy.png"));
                        break;
                    case "Rain":
                        weatherConditionImage.setIcon(loadImage("src/assets/rain.png"));
                        break;
                    case "Snow":
                        weatherConditionImage.setIcon(loadImage("src/assets/snow.png"));
                        break;
                }

                //update temperature text
                double temperature = Double.parseDouble(weatherData.get("temperature").toString());
                temperatureText.setText(temperature + " C");

                //update waether condition text
                weatherConditionsDescription.setText(weatherCondition);

                //update humidity
                long humidity  =(long) weatherData.get("humidity");
                humidityText.setText("<html><b>Humidity</b> "+ humidity + "%</html>");

                //update windspeed text

                double windspeed = (double) weatherData.get("wind_speed");
                winspeedText.setText("<html><b>Wind</b> "+ windspeed + "km/h</html>");

                double uv = Double.parseDouble(weatherData.get("uv_index").toString());
                uvIndexText.setText("<html><b>UV Index</b> "+ uv + "</html>");


                //Insert data to database
                insertWeatherData(userInput, weatherCondition, temperature);


            }

        });
        selectWeatherData();
        System.out.println("Weather App Statistics:");
        List<String> countryNames = weatherDataList.stream()
                .map(WeatherData::getCountry)
                .distinct()
                .toList();

        for (String countryName : countryNames) {
           // System.out.println(countryName);
            double avgTemperature = weatherDataList.stream()
                    .filter(data -> data.getCountry().equalsIgnoreCase(countryName))
                    .collect(Collectors.averagingDouble(WeatherData::getTemperature));

            String result = String.format("%s average temperature is %.2f degrees", countryName,avgTemperature);
            System.out.println(result);

        }
        System.out.println("");
        for (String countryName : countryNames) {
            int countryTimes = weatherDataList.stream()
                    .filter(data -> data.getCountry().equalsIgnoreCase(countryName))
                    .toList().size();

            double countryPercentage = (countryTimes / (double) weatherDataList.size()) * 100;

            String result = String.format("%s makes up %.2f%% of Weather Searches",countryName,countryPercentage);
            System.out.println(result);

        }
        add(searchButton);


        //

    }

    private void initializeDatabaseConnection(){
        try {
            // Connect to SQLite database
            String dbPath = "identifier.sqlite"; // Update this path
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // Create table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS WeatherSearch (CountryName TEXT, Weather TEXT, Temperature INT)";
            connection.createStatement().execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void selectWeatherData(){
        String selectSQL = "SELECT * FROM WeatherSearch";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)){
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                weatherDataList.add(new WeatherData(
                        rs.getString("CountryName"),
                        rs.getString("Weather"),
                        rs.getDouble("Temperature")
                ));

            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }



    private void insertWeatherData(String country, String weather, double temperature) {
        String insertSQL = "INSERT INTO WeatherSearch (CountryName, Weather, Temperature) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, country);
            pstmt.setString(2, weather);
            pstmt.setDouble(3, temperature);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    private ImageIcon loadImage(String resourcePath)
    {
        //used to create images in our gui components
        try
        {
            //Read the image file from the path given
            BufferedImage image = ImageIO.read(new File(resourcePath));
            return new ImageIcon(image);
        }catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("Could not load image");
        return null;
    }
}

 class  WeatherData {
     String country;
     String weather;
     double temperature;

     public String getCountry(){
         return country;
     }

     public double getTemperature(){
         return temperature;
     }

     WeatherData(String country, String weather, double temperature) {
         this.country = country;
         this.weather = weather;
         this.temperature = temperature;
     }

     // Constructor with country and weather only
     public WeatherData(String country, String weather) {
         this(country, weather, 0.0);  // Default temperature set to 0.0
     }

     // Constructor with country only
     public WeatherData(String country) {
         this(country, "Unknown", 0.0);  // Default weather set to "Unknown", temperature to 0.0
     }


 }
