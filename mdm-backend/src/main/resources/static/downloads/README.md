# APK Downloads Directory

Place the signed `devora-mdm-latest.apk` file in this directory.

The backend serves it at `/downloads/devora-mdm-latest.apk` for Device Owner
provisioning. Android's setup wizard downloads the APK from this URL during
factory-reset QR code provisioning.

## How to update the APK

1. Build the release APK: `./gradlew :app:assembleRelease`
2. Copy it here as `devora-mdm-latest.apk`
3. Generate the SHA-256 checksum:
   ```
   cat devora-mdm-latest.apk | openssl dgst -binary -sha256 | openssl base64 | tr '+/' '-_' | tr -d '='
   ```
4. Update the checksum in `QrProvisioningHelper.kt` (`DEFAULT_CHECKSUM`)
