# Weather App

Current weather and a 5-day forecast for any city or lat/lon coordinate pair, via the [OpenWeatherMap API](https://openweathermap.org/api). Built four ways from the same design: a JavaFX desktop app, a static web app, a browser extension, and native installers for macOS/Windows/Linux.

**Live web app**: https://peppy-vacherin-18f5c3.netlify.app
**Downloads (installers + extension)**: https://github.com/Onyedikachika/weather-app/releases/tag/v1.0.0

## What's here

| Target | Path | Tech |
|---|---|---|
| Desktop app | [`src/`](src/main/java/weatherapp/WeatherApp.java) | JavaFX + Maven, packaged with `org.json` |
| Web app | [`webapp/`](webapp/) | Static HTML/CSS/JS, calls the API directly from the browser |
| Browser extension | [`browser-extension/`](browser-extension/) | Manifest V3 popup, same API logic as the web app |
| Installers | [`.github/workflows/build-installers.yml`](.github/workflows/build-installers.yml) | GitHub Actions matrix build using `jpackage` for `.dmg`/`.msi`/`.deb` |

## Features

- Search by city name (`London`) or coordinates (`40.7128,-74.0060`)
- Current temperature, feels-like, humidity, wind speed, pressure, and condition icon
- 5-day forecast strip
- Desktop app's background gradient shifts with the time of day

Unit conversion and search history are described in the original assignment write-up but aren't implemented yet — tracked in [issues #2](https://github.com/Onyedikachika/weather-app/issues/2) and [#3](https://github.com/Onyedikachika/weather-app/issues/3).

## Running the desktop app

Requires JDK 17+ and Maven.

```bash
mvn clean javafx:run
```

## Running the web app locally

```bash
cd webapp
python3 -m http.server 8000
# open http://localhost:8000
```

## Loading the browser extension

1. Go to `chrome://extensions`, enable Developer mode
2. "Load unpacked" → select the `browser-extension/` folder

## Building the installers yourself

The CI workflow does this per-OS since `jpackage` can't cross-compile. To build locally on your own machine:

```bash
mvn -Pmac clean package      # or -Pwindows / -Plinux to match your OS
mkdir -p target/dist && cp target/weather-app-1.0.jar target/dist/
cp -r target/dependency target/dist/dependency
jpackage --type dmg --input target/dist --main-jar weather-app-1.0.jar \
  --main-class weatherapp.WeatherApp --name "Weather App" --app-version 1.0.0 \
  --vendor "Chika N.O." --dest installer-output
```

(swap `--type dmg` for `msi`/`deb` to match the active profile)

## A note on the API key

The OpenWeatherMap key is hardcoded in the source (`WeatherApp.java`, `webapp/app.js`, `browser-extension/popup.js`) and this repo is public, so the key is public too. It's a free-tier key with no billing exposure, but see [issue #1](https://github.com/Onyedikachika/weather-app/issues/1) before relying on this in anything beyond a personal/class project.

## Acknowledgements

Built from a Programming Unit 8 assignment, referencing:

- Chua, E. H. (n.d.). *Java Programming Tutorial: Programming Graphical User Interface (GUI)*. Nanyang Technological University.
- Eck, D. J. (2022). *Introduction to Programming Using Java, version 9, JavaFX edition*. Licensed under CC 4.0.
- Morelli, R. & Wade, R. (n.d.). *Java, Java, Java - Object-Oriented Programming*. LibreTexts Engineering. Licensed under CC 4.0.
- M.G.A. (2022, November 20). *Java GUI programming: An overview for beginners*. Entri Blog.
