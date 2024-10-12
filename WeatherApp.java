import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.Desktop;


public class WeatherApp {

    private String latitude;
    private String longitude;

    public WeatherApp() {
        // Load saved coordinates or ask user for input
        loadCoordinates();
        
        if (latitude == null || longitude == null) {
            openLocationPage();
            getUserCoordinates();
        }

        // Fetch weather data in a separate thread to avoid blocking the UI
        new Thread(this::fetchWeatherData).start();
    }

    private void openLocationPage() {
        try {
            Desktop.getDesktop().browse(new URI("http://127.0.0.1:5500/index.html"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getUserCoordinates() {
        String lat = JOptionPane.showInputDialog(null, "Please enter your Latitude:");
        String lon = JOptionPane.showInputDialog(null, "Please enter your Longitude:");

        if (lat != null && lon != null) {
            this.latitude = lat;
            this.longitude = lon;
            saveCoordinates(lat, lon);
        }
    }

    private void saveCoordinates(String lat, String lon) {
        try (FileWriter writer = new FileWriter("coordinates.txt")) {
            writer.write(lat + "\n" + lon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCoordinates() {
        try (BufferedReader reader = new BufferedReader(new FileReader("coordinates.txt"))) {
            this.latitude = reader.readLine();
            this.longitude = reader.readLine();
        } catch (Exception e) {
            // File not found or other I/O error
            this.latitude = null;
            this.longitude = null;
        }
    }

    private void fetchWeatherData() {
        String baseUrl = "https://api.open-meteo.com/v1/forecast";

        // Constructing the URL for hourly precipitation data
        String url = String.format("%s?latitude=%s&longitude=%s&hourly=precipitation_probability", baseUrl, latitude, longitude);

        try {
            // Create a URL object
            URL apiUrl = new URI(url).toURL();
            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");

            // Get the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray hourlyData = jsonResponse.getJSONObject("hourly").getJSONArray("precipitation_probability");

                // Check the precipitation probability for the next hour
                if (hourlyData.length() > 0) {
                    int precipitationProbability = hourlyData.getInt(0);
                    if (precipitationProbability > 60) {
                        showRainAlert("There is a chance of rain in the next hour. Precipitation Probability: " + precipitationProbability + "%");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRainAlert(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, "Weather Alert", JOptionPane.INFORMATION_MESSAGE));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherApp::new);
    }
}