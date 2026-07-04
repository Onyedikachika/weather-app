const API_KEY = "d55dfe757a8520f2bb8c27a183d3ee7b";
const BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
const FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";
const ICON_BASE_URL = "https://openweathermap.org/img/wn/";

const locationInput = document.getElementById("location-input");
const searchBtn = document.getElementById("search-btn");
const errorBanner = document.getElementById("error-banner");
const weatherIcon = document.getElementById("weather-icon");
const temperatureEl = document.getElementById("temperature");
const descriptionEl = document.getElementById("description");
const feelsLikeEl = document.getElementById("feels-like");
const humidityEl = document.getElementById("humidity");
const windSpeedEl = document.getElementById("wind-speed");
const pressureEl = document.getElementById("pressure");
const forecastContainer = document.getElementById("forecast-container");

const COORDS_PATTERN = /^-?\d+\.?\d*,-?\d+\.?\d*$/;

function buildUrl(base, location) {
  if (COORDS_PATTERN.test(location)) {
    const [lat, lon] = location.split(",").map((s) => s.trim());
    return `${base}?lat=${lat}&lon=${lon}&appid=${API_KEY}&units=metric`;
  }
  return `${base}?q=${encodeURIComponent(location)}&appid=${API_KEY}&units=metric`;
}

function capitalizeWords(text) {
  return text.replace(/\b\w/g, (c) => c.toUpperCase());
}

function showError(message) {
  errorBanner.textContent = message;
  errorBanner.classList.remove("hidden");
}

function clearError() {
  errorBanner.classList.add("hidden");
}

async function fetchWeatherData() {
  const location = locationInput.value.trim();
  if (!location) {
    showError("Please enter a location");
    return;
  }

  clearError();
  temperatureEl.textContent = "Loading...";
  descriptionEl.textContent = "Fetching weather data";

  try {
    const response = await fetch(buildUrl(BASE_URL, location));
    if (!response.ok) {
      throw new Error(`City not found or API error (Code: ${response.status})`);
    }
    const data = await response.json();
    updateWeatherDisplay(data);
    fetchForecastData(location);
  } catch (err) {
    showError(`Failed to fetch weather data: ${err.message}`);
    temperatureEl.textContent = "--°C";
    descriptionEl.textContent = "Select a location";
  }
}

function updateWeatherDisplay(data) {
  const temp = data.main.temp;
  temperatureEl.textContent = `${Math.round(temp)}°C`;

  const description = data.weather[0].description;
  descriptionEl.textContent = capitalizeWords(description);

  feelsLikeEl.textContent = `Feels like: ${Math.round(data.main.feels_like)}°C`;
  humidityEl.textContent = `Humidity: ${data.main.humidity}%`;

  const windSpeedMs = data.wind?.speed ?? 0;
  windSpeedEl.textContent = `Wind: ${Math.round(windSpeedMs * 3.6)} km/h`;

  pressureEl.textContent = `Pressure: ${data.main.pressure} hPa`;

  loadWeatherIcon(data.weather[0].icon);
}

function loadWeatherIcon(iconCode) {
  weatherIcon.src = `${ICON_BASE_URL}${iconCode}@2x.png`;
}

async function fetchForecastData(location) {
  try {
    const response = await fetch(buildUrl(FORECAST_URL, location));
    if (!response.ok) return;
    const data = await response.json();
    updateForecastDisplay(data);
  } catch (err) {
    console.error("Failed to fetch forecast:", err.message);
  }
}

function updateForecastDisplay(data) {
  const forecasts = data.list;
  forecastContainer.innerHTML = "";

  for (let i = 0; i < Math.min(forecasts.length, 40); i += 8) {
    const forecast = forecasts[i];
    const dayName = new Date(forecast.dt * 1000).toLocaleDateString("en-US", { weekday: "short" });
    const temp = Math.round(forecast.main.temp);
    const description = forecast.weather[0].main;
    const iconCode = forecast.weather[0].icon;

    forecastContainer.appendChild(createForecastItem(dayName, `${temp}°C`, description, iconCode));
  }
}

function createForecastItem(day, temp, description, iconCode) {
  const item = document.createElement("div");
  item.className = "forecast-day";
  item.innerHTML = `
    <span class="day-name">${day}</span>
    <img src="${ICON_BASE_URL}${iconCode}@2x.png" alt="${description}" />
    <span class="day-temp">${temp}</span>
    <span class="day-desc">${description}</span>
  `;
  return item;
}

searchBtn.addEventListener("click", fetchWeatherData);
locationInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") fetchWeatherData();
});
