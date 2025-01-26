import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WeatherAppGui extends JFrame {

    private JSONObject weatherData;
    public WeatherAppGui() {
        //set up our gui and add a title
        super("Weather App");

        //configure gui to end the program's process once it has been closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        //set the size of gui in (in pixels)
        setSize(450, 650);


        //load our gui at the center of the screen
        setLocationRelativeTo(null);
        setLayout(null);

        //prevent any resize of the gui
        setResizable(false);


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

                double windspeed = (double) weatherData.get("windspeed");
                winspeedText.setText("<html><b>Wind</b> "+ windspeed + "km/h</html>");
            }
        });
        add(searchButton);


        //

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
