# SpotOn

SpotOn is sunset. Keep it working and on Google Play for the existing Android and Wear OS
devices: make only the changes Play requires, and add no new features.

- `app` is the phone app and `spotonwear` the Wear OS app. Both use the package
  `com.prof18.spoton`.
- Run Gradle with `-q --console=plain`.

## Release

Bump `versionCode` and `versionName` in `app/build.gradle.kts` and
`spotonwear/build.gradle.kts`. Each version code must be higher than any code already on
Play for that form factor.

Build and sign both bundles with `tools/signing/build-release.sh`. To upload them too, add
`--upload`: the phone bundle goes to the `internal` track and the Wear OS bundle to
`wear:internal`, through gplay. Promote them to production from the Play Console.

The script reads the keystore (`release.jks`) and the `key_alias`, `store_password` and
`key_password` fields from the 1Password item that `SPOTON_SIGNING_ITEM` points at. That
variable is exported in `~/.zshrc`; agent shells read it with
`zsh -ic 'printf %s "$SPOTON_SIGNING_ITEM"'`. While Gradle runs, the script writes the
signing lines into `local.properties` and keeps `sdk.dir`. When it exits, it restores the
original file. Don't keep a keystore or passwords in `local.properties`.
