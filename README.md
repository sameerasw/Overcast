# Overcast ☁️

Weather is also Essential

An immersive weather app for Android built with Material 3 Expressive. Further integrate with [Essentials](https://github.com/sameerasw/essentials) island and brief.

<p align="center">
  <a href="https://github.com/sameerasw/Overcast/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/sameerasw/Overcast?style=for-the-badge&logo=android&logoColor=%23fff&labelColor=%2348C&color=%2348C"></a>
  <a href="https://github.com/sameerasw/Overcast/issues/new?template=bug_report.yml"><img alt="Report a bug" src="https://img.shields.io/badge/Report-a%20bug-2a6?style=for-the-badge&logo=openbugbounty&logoColor=%23fff&labelColor=%232a6"></a>
  <a href="https://github.com/sameerasw/Overcast/issues/new?template=feature_request.yml"><img alt="Request a feature" src="https://img.shields.io/badge/Request-a%20feature-a26?style=for-the-badge&logo=apachespark&logoColor=%23fff&labelColor=%23a26"></a>
</p>

---

## Navigation

- [Forecast sources](#forecast-sources)
- [Essentials integration](#essentials-integration)
- [Installation](#installation)
- [Building](#building)
- [Contributing](#contributing)
- [Credits](#credits)


## Forecast sources

Overcast reads from the source you choose in settings. API keys are kept on the device.

| Source | API key | Notes |
| --- | --- | --- |
| Open-Meteo | No | Default. Forecast model can be pinned (ECMWF, GFS, ICON, UK Met Office and others). |
| MET Norway (Yr) | No | |
| US National Weather Service | No | United States only. |
| Pirate Weather | Yes | |
| Tomorrow.io | Yes | |
| Visual Crossing | Yes | |
| OpenWeatherMap | Yes | |
| WeatherAPI.com | Yes | |

Alert coverage depends on the source. Not every provider returns alerts for every region.

## Essentials integration

Overcast can share its latest forecast with [Essentials](https://github.com/sameerasw/essentials) for the Dynamic Island and the Brief card.

- Overcast declares a `READ_WEATHER` permission. Essentials has to be granted it before any data is shared.
- Essentials is notified when Overcast stores a new forecast, so the two stay in sync without polling.

## Installation

Download the latest APK from the [releases page](https://github.com/sameerasw/Overcast/releases/latest) and install it.

Requires Android 13 or newer.

## Building

You need Android Studio with a recent Android SDK (the project compiles against API 37.1) and JDK 21.

```bash
git clone https://github.com/sameerasw/Overcast.git
cd Overcast
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Contributing

Bug reports, feature requests and pull requests are welcome. Use the [issue templates](https://github.com/sameerasw/Overcast/issues/new/choose) to report a problem or suggest something.

Please read the [code of conduct](CODE_OF_CONDUCT.md) and the [security policy](SECURITY.md) before contributing.

## Credits

- [Open-Meteo](https://open-meteo.com) for the default forecast and geocoding data
- Everyone behind the other forecast sources listed above.

## License

[MIT](LICENSE)


<p align="center">
  <a href="https://www.reddit.com/r/MadebySameerasw"><img  width="49%"  alt=" reddit-banner" src="https://github.com/user-attachments/assets/a5197458-d64a-4c6a-a6a3-9e1f36030205" /></a>
  <a href="https://t.me/tidwib"><img  width="49%"  alt=" telegram-banner" src="https://github.com/user-attachments/assets/425b3cc1-9ac6-46ec-8f48-71c7af9c9ca2" /></a>
</p>

