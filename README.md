# Spoton

Spoton allows you to enable the hotspot on your phone directly from your Android Wear watch.

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.prof18.spoton)

## Featured On

<a href="https://www.androidpolice.com/spoton-app-toggle-hotspot-from-wear-os-smartwatch/">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/6/62/Android_Police_Logo_2024.svg/2560px-Android_Police_Logo_2024.svg.png" width="200" alt="Android Police" />
</a>

Read the article on [Android Police](https://www.androidpolice.com/spoton-app-toggle-hotspot-from-wear-os-smartwatch/).

## ⚠️ End of Support on Android 16+

**Spoton continues to work on Android 15 and below.**

However, **Android 16+ is no longer supported**.

With Android 16, Google has significantly tightened security restrictions around tethering/hotspot control. Specifically:

- The `startTethering` and `stopTethering` APIs now strictly require the `TETHER_PRIVILEGED` permission.
- This permission is **only granted to system apps** (pre-installed by the device manufacturer).
- Previous workarounds using reflection are now blocked by Android's hidden API regulations.

I spent time investigating solutions including `TetheringManager` and various reflection techniques, but all paths lead to `TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION` (Error 14). Unless you have a rooted device, programmatic hotspot control is effectively gone for third-party apps on Android 16+.

## How it works

Spoton consists of two components:
1.  **Wear OS App**: A simple interface to toggle the hotspot.
2.  **Android Phone App**: A companion app that handles the background communication and executes the hotspot toggle command.

The app uses **reflection** on private APIs to enable the hotspot, which is why it is sensitive to Android system updates.

### Technical Details (The "Hack")

For those interested in the code, the app uses **reflection** to access private APIs in [`ConnectivityManager`](app/src/main/java/com/prof18/spoton/HotspotManager.kt).

To make it work, I had to:
- Use **reflection** to invoke the private `startTethering` and `stopTethering` methods on `ConnectivityManager`.
- Use `ProxyBuilder` (from `com.android.dx.stock`) to dynamically create an instance of the private `ConnectivityManager$OnStartTetheringCallback` abstract class, which is required as an argument for `startTethering`.
- Use **reflection** on `WifiManager` to check if the hotspot is enabled (`isWifiApEnabled`).

This approach bypasses the standard public API limitations but is fragile and subject to breakage when Android internal APIs change (as seen with Android 16).

Despite these efforts, the new permission enforcement in Android 16 makes it impossible to continue without system privileges.

## Disclaimer

This tool was originally developed for myself to make my life easier. The code might not be the most polished or follow all best practices, but it did the job!
