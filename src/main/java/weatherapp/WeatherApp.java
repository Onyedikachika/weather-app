package weatherapp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class WeatherApp extends Application {

    private static final String API_KEY = "d55dfe757a8520f2bb8c27a183d3ee7b";
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "http://api.openweathermap.org/data/2.5/forecast";
    private static final String ICON_BASE_URL = "http://openweathermap.org/img/wn/";

    private TextField locationField;
    private Label titleLabel;
    private Label temperatureLabel;
    private Label descriptionLabel;
    private Label humidityLabel;
    private Label windSpeedLabel;
    private Label pressureLabel;
    private Label feelsLikeLabel;
    private ImageView weatherIcon;
    private HBox forecastContainer;
    private Label forecastTitleLabel;
    private Label footerLabel;
    private VBox root;

    private Canvas deviceClockCanvas;
    private Label clockLabel;
    private Label cityTimeLabel;
    private Integer cityTimezoneOffsetSeconds;
    private String cityDisplayName;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Weather App");

        root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // Create UI components
        createHeaderSection();
        createClockSection();
        createCurrentWeatherSection();
        createForecastSection();
        createFooterSection();

        Scene scene = new Scene(root, 800, 600);

        // Load CSS styles
        try {
            scene.getStylesheets().add(getClass().getResource("/weatherapp/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load CSS file: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        root.setStyle("-fx-background-color: skyblue;");

        drawDeviceClock();
        Timeline clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            drawDeviceClock();
            updateCityTimeLabel();
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void createHeaderSection() {
        titleLabel = new Label("Weather App");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        locationField = new TextField();
        locationField.setPromptText("Enter city name or coordinates (lat,lon)");
        locationField.setPrefWidth(300);
        locationField.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        Button searchButton = new Button("Get Weather");
        searchButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-border-radius: 5px;");
        searchButton.setOnAction(e -> fetchWeatherData());

        // Enter key support
        locationField.setOnAction(e -> fetchWeatherData());

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.getChildren().addAll(locationField, searchButton);

        VBox headerBox = new VBox(15);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.getChildren().addAll(titleLabel, searchBox);

        root.getChildren().add(headerBox);
    }

    private void createClockSection() {
        deviceClockCanvas = new Canvas(140, 140);

        clockLabel = new Label("Your time");
        clockLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        VBox deviceClockBox = new VBox(4);
        deviceClockBox.setAlignment(Pos.CENTER);
        deviceClockBox.getChildren().addAll(deviceClockCanvas, clockLabel);

        cityTimeLabel = new Label();
        cityTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
                "-fx-background-color: rgba(255,255,255,0.7); -fx-padding: 6px 14px; -fx-background-radius: 14px;");
        cityTimeLabel.setVisible(false);
        cityTimeLabel.setManaged(false);

        VBox clockSection = new VBox(8);
        clockSection.setAlignment(Pos.CENTER);
        clockSection.getChildren().addAll(deviceClockBox, cityTimeLabel);

        root.getChildren().add(clockSection);
    }

    private void createCurrentWeatherSection() {
        // Weather icon
        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(100);
        weatherIcon.setFitHeight(100);
        weatherIcon.setPreserveRatio(true);

        // Temperature and description
        temperatureLabel = new Label("--°C");
        temperatureLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        descriptionLabel = new Label("Select a location");
        descriptionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #7f8c8d;");

        VBox tempDescBox = new VBox(5);
        tempDescBox.setAlignment(Pos.CENTER);
        tempDescBox.getChildren().addAll(temperatureLabel, descriptionLabel);

        HBox mainWeatherBox = new HBox(20);
        mainWeatherBox.setAlignment(Pos.CENTER);
        mainWeatherBox.getChildren().addAll(weatherIcon, tempDescBox);

        // Additional weather details
        feelsLikeLabel = new Label("Feels like: --°C");
        humidityLabel = new Label("Humidity: --%");
        windSpeedLabel = new Label("Wind: -- km/h");
        pressureLabel = new Label("Pressure: -- hPa");

        String detailStyle = "-fx-font-size: 14px; -fx-text-fill: #34495e; -fx-padding: 5px;";
        feelsLikeLabel.setStyle(detailStyle);
        humidityLabel.setStyle(detailStyle);
        windSpeedLabel.setStyle(detailStyle);
        pressureLabel.setStyle(detailStyle);

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(30);
        detailsGrid.setVgap(10);
        detailsGrid.setAlignment(Pos.CENTER);

        detailsGrid.add(feelsLikeLabel, 0, 0);
        detailsGrid.add(humidityLabel, 1, 0);
        detailsGrid.add(windSpeedLabel, 0, 1);
        detailsGrid.add(pressureLabel, 1, 1);

        VBox currentWeatherBox = new VBox(20);
        currentWeatherBox.setAlignment(Pos.CENTER);
        currentWeatherBox.getChildren().addAll(mainWeatherBox, detailsGrid);

        root.getChildren().add(currentWeatherBox);
    }

    private void createForecastSection() {
        forecastTitleLabel = new Label("5-Day Forecast");
        forecastTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        forecastContainer = new HBox(15);
        forecastContainer.setAlignment(Pos.CENTER);

        ScrollPane forecastScroll = new ScrollPane(forecastContainer);
        forecastScroll.setFitToHeight(true);
        forecastScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        forecastScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        forecastScroll.setStyle("-fx-background-color: transparent;");

        VBox forecastSection = new VBox(10);
        forecastSection.setAlignment(Pos.CENTER);
        forecastSection.getChildren().addAll(forecastTitleLabel, forecastScroll);

        root.getChildren().add(forecastSection);
    }

    private void createFooterSection() {
        footerLabel = new Label("Developed by Onyedika Chika Nwankwo\nAll rights reserved, 2026.");
        footerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #34495e; -fx-text-alignment: center;");
        footerLabel.setAlignment(Pos.CENTER);

        VBox footerBox = new VBox();
        footerBox.setAlignment(Pos.CENTER);
        footerBox.getChildren().add(footerLabel);

        root.getChildren().add(footerBox);
    }

    private void fetchWeatherData() {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            showAlert("Error", "Please enter a location");
            return;
        }

        // Show loading state
        temperatureLabel.setText("Loading...");
        descriptionLabel.setText("Fetching weather data");

        Task<String> weatherTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return getWeatherData(location);
            }
        };

        weatherTask.setOnSucceeded(e -> {
            try {
                String result = weatherTask.getValue();
                if (result != null) {
                    updateWeatherDisplay(result);
                    fetchForecastData(location);
                }
            } catch (Exception ex) {
                showAlert("Error", "Failed to parse weather data: " + ex.getMessage());
            }
        });

        weatherTask.setOnFailed(e -> {
            Throwable exception = weatherTask.getException();
            showAlert("Error", "Failed to fetch weather data: " + exception.getMessage());
            temperatureLabel.setText("--°C");
            descriptionLabel.setText("Select a location");
        });

        Thread thread = new Thread(weatherTask);
        thread.setDaemon(true);
        thread.start();
    }

    private String getWeatherData(String location) throws Exception {
        String urlString;

        // Check if input is coordinates (lat,lon format)
        if (location.matches("-?\\d+\\.?\\d*,-?\\d+\\.?\\d*")) {
            String[] coords = location.split(",");
            urlString = BASE_URL + "?lat=" + coords[0].trim() + "&lon=" + coords[1].trim() +
                       "&appid=" + API_KEY + "&units=metric";
        } else {
            // Treat as city name
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            urlString = BASE_URL + "?q=" + encodedLocation + "&appid=" + API_KEY + "&units=metric";
        }

        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("City not found or API error (Code: " + responseCode + ")");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private void updateWeatherDisplay(String jsonString) {
        try {
            JSONObject data = new JSONObject(jsonString);

            // Temperature
            double temp = data.getJSONObject("main").getDouble("temp");
            temperatureLabel.setText(Math.round(temp) + "°C");

            // Description
            String description = data.getJSONArray("weather").getJSONObject(0).getString("description");
            descriptionLabel.setText(capitalizeWords(description));

            // Additional details
            double feelsLike = data.getJSONObject("main").getDouble("feels_like");
            feelsLikeLabel.setText("Feels like: " + Math.round(feelsLike) + "°C");

            int humidity = data.getJSONObject("main").getInt("humidity");
            humidityLabel.setText("Humidity: " + humidity + "%");

            // Convert wind speed from m/s to km/h
            double windSpeedMs = data.getJSONObject("wind").optDouble("speed", 0);
            double windSpeedKmh = windSpeedMs * 3.6;
            windSpeedLabel.setText("Wind: " + Math.round(windSpeedKmh) + " km/h");

            int pressure = data.getJSONObject("main").getInt("pressure");
            pressureLabel.setText("Pressure: " + pressure + " hPa");

            // Weather icon
            String iconCode = data.getJSONArray("weather").getJSONObject(0).getString("icon");
            loadWeatherIcon(iconCode);

            cityTimezoneOffsetSeconds = data.optInt("timezone", 0);
            cityDisplayName = data.optString("name", locationField.getText().trim());
            cityTimeLabel.setVisible(true);
            cityTimeLabel.setManaged(true);
            updateCityTimeLabel();

            String conditionMain = data.getJSONArray("weather").getJSONObject(0).getString("main");
            boolean isDay = !iconCode.endsWith("n");
            applyBackground(conditionMain, windSpeedKmh, isDay);

        } catch (Exception e) {
            showAlert("Error", "Failed to parse weather data: " + e.getMessage());
        }
    }

    private void fetchForecastData(String location) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return getForecastData(location);
            } catch (Exception e) {
                Platform.runLater(() -> System.err.println("Failed to fetch forecast: " + e.getMessage()));
                return null;
            }
        }).thenAccept(jsonString -> {
            if (jsonString != null) {
                Platform.runLater(() -> updateForecastDisplay(jsonString));
            }
        });
    }

    private String getForecastData(String location) throws Exception {
        String urlString;

        // Check if input is coordinates
        if (location.matches("-?\\d+\\.?\\d*,-?\\d+\\.?\\d*")) {
            String[] coords = location.split(",");
            urlString = FORECAST_URL + "?lat=" + coords[0].trim() + "&lon=" + coords[1].trim() +
                       "&appid=" + API_KEY + "&units=metric";
        } else {
            String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
            urlString = FORECAST_URL + "?q=" + encodedLocation + "&appid=" + API_KEY + "&units=metric";
        }

        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private void updateForecastDisplay(String jsonString) {
        try {
            JSONObject data = new JSONObject(jsonString);
            JSONArray forecasts = data.getJSONArray("list");

            forecastContainer.getChildren().clear();

            // Show forecast for next 5 days (every 24 hours)
            for (int i = 0; i < Math.min(forecasts.length(), 40); i += 8) {
                JSONObject forecast = forecasts.getJSONObject(i);

                // Date
                long timestamp = forecast.getLong("dt");
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
                String dayName = dateTime.format(DateTimeFormatter.ofPattern("EEE"));

                // Temperature
                double temp = forecast.getJSONObject("main").getDouble("temp");

                // Weather description
                String description = forecast.getJSONArray("weather").getJSONObject(0).getString("main");

                // Icon
                String iconCode = forecast.getJSONArray("weather").getJSONObject(0).getString("icon");

                VBox forecastItem = createForecastItem(dayName, Math.round(temp) + "°C", description, iconCode);
                forecastContainer.getChildren().add(forecastItem);
            }

        } catch (Exception e) {
            System.err.println("Failed to update forecast display: " + e.getMessage());
        }
    }

    private VBox createForecastItem(String day, String temp, String description, String iconCode) {
        Label dayLabel = new Label(day);
        dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ImageView iconView = new ImageView();
        iconView.setFitWidth(50);
        iconView.setFitHeight(50);
        iconView.setPreserveRatio(true);

        // Load forecast icon
        try {
            String iconUrl = ICON_BASE_URL + iconCode + "@2x.png";
            Image icon = new Image(iconUrl, true);
            iconView.setImage(icon);
        } catch (Exception e) {
            System.err.println("Failed to load forecast icon: " + e.getMessage());
        }

        Label tempLabel = new Label(temp);
        tempLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        VBox item = new VBox(5);
        item.setAlignment(Pos.CENTER);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-border-radius: 10px; -fx-background-radius: 10px;");
        item.getChildren().addAll(dayLabel, iconView, tempLabel, descLabel);

        return item;
    }

    private void loadWeatherIcon(String iconCode) {
        try {
            String iconUrl = ICON_BASE_URL + iconCode + "@2x.png";
            Image icon = new Image(iconUrl, true);
            weatherIcon.setImage(icon);
        } catch (Exception e) {
            System.err.println("Failed to load weather icon: " + e.getMessage());
        }
    }

    private void drawDeviceClock() {
        GraphicsContext gc = deviceClockCanvas.getGraphicsContext2D();
        double size = deviceClockCanvas.getWidth();
        double radius = size / 2;
        LocalTime now = LocalTime.now();

        gc.clearRect(0, 0, size, size);
        gc.save();
        gc.translate(radius, radius);

        gc.setFill(Color.rgb(255, 255, 255, 0.9));
        gc.fillOval(-(radius - 4), -(radius - 4), (radius - 4) * 2, (radius - 4) * 2);
        gc.setStroke(Color.web("#2c3e50"));
        gc.setLineWidth(3);
        gc.strokeOval(-(radius - 4), -(radius - 4), (radius - 4) * 2, (radius - 4) * 2);

        for (int i = 0; i < 12; i++) {
            double angle = (i * Math.PI) / 6;
            boolean isMajor = i % 3 == 0;
            double outerR = radius - 6;
            double innerR = isMajor ? radius - 16 : radius - 12;
            gc.setLineWidth(isMajor ? 3 : 1.5);
            gc.setStroke(Color.web("#2c3e50"));
            gc.strokeLine(outerR * Math.sin(angle), -outerR * Math.cos(angle),
                    innerR * Math.sin(angle), -innerR * Math.cos(angle));
        }

        int hours = now.getHour() % 12;
        int minutes = now.getMinute();
        int seconds = now.getSecond();

        drawHand(gc, ((hours + minutes / 60.0) * Math.PI) / 6, radius * 0.5, 5, "#2c3e50");
        drawHand(gc, ((minutes + seconds / 60.0) * Math.PI) / 30, radius * 0.72, 3, "#2c3e50");
        drawHand(gc, (seconds * Math.PI) / 30, radius * 0.8, 1.5, "#e74c3c");

        gc.setFill(Color.web("#2c3e50"));
        gc.fillOval(-4, -4, 8, 8);

        gc.restore();
    }

    private void drawHand(GraphicsContext gc, double angle, double length, double width, String color) {
        gc.setLineWidth(width);
        gc.setStroke(Color.web(color));
        gc.strokeLine(0, 0, length * Math.sin(angle), -length * Math.cos(angle));
    }

    private void updateCityTimeLabel() {
        if (cityTimezoneOffsetSeconds == null) {
            return;
        }
        OffsetDateTime cityTime = OffsetDateTime.now(ZoneOffset.ofTotalSeconds(cityTimezoneOffsetSeconds));
        String formatted = cityTime.format(DateTimeFormatter.ofPattern("h:mm:ss a"));
        cityTimeLabel.setText("Local time in " + cityDisplayName + ": " + formatted);
    }

    private String classifyCondition(String main, double windKmh) {
        String m = main.toLowerCase();
        if (m.equals("thunderstorm") || m.equals("tornado")) {
            return "stormy";
        }
        if (m.equals("snow")) {
            return "snowy";
        }
        if (m.equals("rain") || m.equals("drizzle")) {
            return "rainy";
        }
        if (windKmh > 30) {
            return "windy";
        }
        if (m.equals("clear")) {
            return "sunny";
        }
        return "cloudy";
    }

    private String backgroundGradient(boolean isDay, String condition) {
        if (!isDay) {
            switch (condition) {
                case "sunny": return "linear-gradient(to bottom, #0f2027, #203a43, #2c5364)";
                case "rainy": return "linear-gradient(to bottom, #0f2027, #16222a, #3a4a5a)";
                case "snowy": return "linear-gradient(to bottom, #33415c, #99a8c2)";
                case "stormy": return "linear-gradient(to bottom, #232526, #0f2027)";
                case "windy": return "linear-gradient(to bottom, #232526, #2c5364)";
                default: return "linear-gradient(to bottom, #232526, #414345)";
            }
        }
        switch (condition) {
            case "sunny": return "linear-gradient(to bottom, #4facfe, #fceabb)";
            case "rainy": return "linear-gradient(to bottom, #4b6cb7, #182848)";
            case "snowy": return "linear-gradient(to bottom, #83a4d4, #e6f0fa)";
            case "stormy": return "linear-gradient(to bottom, #232526, #0f2027)";
            case "windy": return "linear-gradient(to bottom, #4ca1af, #c4e0e5)";
            default: return "linear-gradient(to bottom, #757f9a, #d7dde8)";
        }
    }

    private void applyBackground(String main, double windKmh, boolean isDay) {
        String condition = classifyCondition(main, windKmh);
        root.setStyle("-fx-background-color: " + backgroundGradient(isDay, condition) + ";");
        updateTextContrast(!isDay);
    }

    private void updateTextContrast(boolean light) {
        String primary = light ? "#f0f4f8" : "#2c3e50";
        String secondary = light ? "#dfe6ec" : "#7f8c8d";
        String tertiary = light ? "#e8edf2" : "#34495e";

        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + primary + ";");
        temperatureLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + primary + ";");
        descriptionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + secondary + ";");

        String detailStyle = "-fx-font-size: 14px; -fx-text-fill: " + tertiary + "; -fx-padding: 5px;";
        feelsLikeLabel.setStyle(detailStyle);
        humidityLabel.setStyle(detailStyle);
        windSpeedLabel.setStyle(detailStyle);
        pressureLabel.setStyle(detailStyle);

        forecastTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + primary + ";");
        clockLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + tertiary + ";");
        footerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + tertiary + "; -fx-text-alignment: center;");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String capitalizeWords(String text) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
