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
const deviceClockCanvas = document.getElementById("device-clock-canvas");
const cityTimeEl = document.getElementById("city-time");

const COORDS_PATTERN = /^-?\d+\.?\d*,-?\d+\.?\d*$/;

let cityTimezoneOffsetSeconds = null;
let cityName = null;

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

  cityTimezoneOffsetSeconds = data.timezone;
  cityName = data.name || locationInput.value.trim();
  cityTimeEl.classList.remove("hidden");
  updateCityTime();

  const isDay = !data.weather[0].icon.endsWith("n");
  applyBackground(data.weather[0].main, windSpeedMs * 3.6, isDay);
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

function drawHand(ctx, angle, length, width, color) {
  ctx.beginPath();
  ctx.lineWidth = width;
  ctx.lineCap = "round";
  ctx.strokeStyle = color;
  ctx.moveTo(0, 0);
  ctx.lineTo(length * Math.sin(angle), -length * Math.cos(angle));
  ctx.stroke();
}

function drawDeviceClock() {
  const ctx = deviceClockCanvas.getContext("2d");
  const size = deviceClockCanvas.width;
  const radius = size / 2;
  const now = new Date();

  ctx.clearRect(0, 0, size, size);
  ctx.save();
  ctx.translate(radius, radius);

  ctx.beginPath();
  ctx.arc(0, 0, radius - 4, 0, 2 * Math.PI);
  ctx.fillStyle = "rgba(255, 255, 255, 0.9)";
  ctx.fill();
  ctx.lineWidth = 3;
  ctx.strokeStyle = "#2c3e50";
  ctx.stroke();

  for (let i = 0; i < 12; i++) {
    const angle = (i * Math.PI) / 6;
    const isMajor = i % 3 === 0;
    const outerR = radius - 6;
    const innerR = isMajor ? radius - 16 : radius - 12;
    ctx.beginPath();
    ctx.moveTo(outerR * Math.sin(angle), -outerR * Math.cos(angle));
    ctx.lineTo(innerR * Math.sin(angle), -innerR * Math.cos(angle));
    ctx.lineWidth = isMajor ? 3 : 1.5;
    ctx.strokeStyle = "#2c3e50";
    ctx.stroke();
  }

  const hours = now.getHours() % 12;
  const minutes = now.getMinutes();
  const seconds = now.getSeconds();

  drawHand(ctx, ((hours + minutes / 60) * Math.PI) / 6, radius * 0.5, 5, "#2c3e50");
  drawHand(ctx, ((minutes + seconds / 60) * Math.PI) / 30, radius * 0.72, 3, "#2c3e50");
  drawHand(ctx, (seconds * Math.PI) / 30, radius * 0.8, 1.5, "#e74c3c");

  ctx.beginPath();
  ctx.arc(0, 0, 4, 0, 2 * Math.PI);
  ctx.fillStyle = "#2c3e50";
  ctx.fill();

  ctx.restore();
}

function updateCityTime() {
  if (cityTimezoneOffsetSeconds === null) return;
  const cityDate = new Date(Date.now() + cityTimezoneOffsetSeconds * 1000);
  const hours = cityDate.getUTCHours();
  const minutes = cityDate.getUTCMinutes();
  const seconds = cityDate.getUTCSeconds();
  const period = hours >= 12 ? "PM" : "AM";
  const hour12 = hours % 12 === 0 ? 12 : hours % 12;
  const pad = (n) => String(n).padStart(2, "0");
  cityTimeEl.textContent = `Local time in ${cityName}: ${hour12}:${pad(minutes)}:${pad(seconds)} ${period}`;
}

function classifyCondition(main, windKmh) {
  const m = main.toLowerCase();
  if (m === "thunderstorm" || m === "tornado") return "stormy";
  if (m === "snow") return "snowy";
  if (m === "rain" || m === "drizzle") return "rainy";
  if (windKmh > 30) return "windy";
  if (m === "clear") return "sunny";
  return "cloudy";
}

function applyBackground(main, windKmh, isDay) {
  const condition = classifyCondition(main, windKmh);
  document.body.className = `bg-${isDay ? "day" : "night"}-${condition}`;
}

searchBtn.addEventListener("click", fetchWeatherData);
locationInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") fetchWeatherData();
});

drawDeviceClock();
setInterval(drawDeviceClock, 1000);
setInterval(updateCityTime, 1000);
