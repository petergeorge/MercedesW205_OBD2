# Mercedes-Benz C180 (W205) ELM327 OBD-II Android App

A dedicated Android automotive telemetry and diagnostics application tailored for the **2019 Mercedes-Benz C180 (W205 Facelift - Petrol M264/M274)** communicating wirelessly over **ELM327 Bluetooth (Classic & BLE)**.

---

## 🌟 Key Features

1. **🏎️ AMG Digital Cockpit Dashboard:**
   * **Circular Tachometer (0–7000 RPM)** with smooth 60fps needle interpolation and dynamic 6200+ RPM redline illumination.
   * **Large Center Digital Speedometer** (km/h).
   * **Turbo Boost Gauge:** Displays live manifold pressure in **BAR** (-0.8 bar vacuum to +1.8 bar boost) calculated from MAP (`010B`) and Barometric (`0133`) sensors.
   * **Engine Oil Temperature (`015C`)** & **Coolant Temperature (`0105`)** dials.
   * **Intake Air Temperature (`010F`)** & **Throttle Position (`0111`)**.
   * **Alternator / Battery Voltage Meter** with live color health status.

2. **🔍 Mercedes DTC Scanner & Clear Tool:**
   * Reads Stored (`Mode 03`) and Pending (`Mode 07`) fault codes over ISO 15765-4 CAN 11-bit / 500kbaud.
   * Built-in Mercedes-Benz fault code database (e.g., `P0299 - Turbo Underboost`, `P052E - Crankcase Ventilation Valve`, `P0128 - Coolant Thermostat`).
   * One-touch **Clear Fault Codes / Reset MIL (`Mode 04`)** with double-confirmation dialog.

3. **📈 Trip Telemetry Logger & CSV Export:**
   * Continuous background logging of sensor streams during drives.
   * Exports standard `.csv` files with one-click Android Share Sheet (Gmail, WhatsApp, Google Drive, Local File).

4. **⚡ Dual-Mode Bluetooth Architecture:**
   * **Classic Bluetooth (SPP / RFCOMM):** Standard blue ELM327 mini dongles, OBDLink MX+, Konnwei, etc.
   * **Bluetooth Low Energy (BLE):** vGate iCar Pro BLE, Veepeak BLE, Carista, etc.

---

## 🚗 Target Vehicle & Hardware Setup

* **Vehicle:** 2019 Mercedes-Benz C180 (W205 MoPf / Facelift Petrol)
* **OBD-II Port Location:** Directly under the dashboard on the driver-side footwell, protected by a small flip-down cover.
* **Compatible Adapters:** Any ELM327 v1.5 or v2.1 Bluetooth adapter, vGate iCar Pro, or OBDLink MX+.

### Pairing Your Adapter:
1. Plug your ELM327 dongle into your C180 OBD-II port.
2. Turn the vehicle ignition ON (Engine Start button $\rightarrow$ Position 2, engine can be running or off).
3. On your Android phone, go to **Settings $\rightarrow$ Bluetooth** and pair the adapter (usually named `OBDII`, `OBD2`, or `vGate`, pairing code `1234` or `0000`).

---

## 🛠️ How to Open & Build in Android Studio

### Prerequisites:
* Android Studio (Hedgehog 2023.1.1, Iguana, or Ladybug 2024+)
* Android SDK 34 (Android 14) installed
* JDK 17

### Steps:
1. Open **Android Studio**.
2. Select **File $\rightarrow$ Open...** and navigate to:
   ```text
   C:\workset\TVeMID\MercedesW205_OBD2_App
   ```
3. Android Studio will automatically sync the Gradle build files.
4. Connect your Android phone via USB (with **USB Debugging** enabled) or start an Android Emulator.
5. Click the green **Run (▶)** button or press `Shift + F10`.

### Generating Standalone APK:
In Android Studio:
1. Go to **Build $\rightarrow$ Build Bundle(s) / APK(s) $\rightarrow$ Build APK(s)**.
2. Once finished, click **"locate"** to find `app-debug.apk`.
3. Transfer the APK to your phone and install directly.

---

## 📂 Source Code Structure

```text
MercedesW205_OBD2_App/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/mercedes/w205/obd/
│   │   │   ├── data/
│   │   │   │   ├── bluetooth/
│   │   │   │   │   ├── BluetoothTransport.kt
│   │   │   │   │   ├── ClassicBluetoothTransport.kt
│   │   │   │   │   └── BleTransport.kt
│   │   │   │   ├── obd/
│   │   │   │   │   ├── W205Decoder.kt
│   │   │   │   │   ├── DtcDatabase.kt
│   │   │   │   │   └── Elm327Protocol.kt
│   │   │   │   └── logger/
│   │   │   │       └── TripDataLogger.kt
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   └── Theme.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── CircularTachometer.kt
│   │   │   │   │   ├── BoostBarGauge.kt
│   │   │   │   │   └── CompactTempDial.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── CockpitScreen.kt
│   │   │   │   │   ├── DtcScreen.kt
│   │   │   │   │   ├── LoggerScreen.kt
│   │   │   │   │   └── DeviceScanDialog.kt
│   │   │   │   └── viewmodel/
│   │   │   │       └── ObdViewModel.kt
│   │   │   └── MainActivity.kt
│   │   └── res/
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```
