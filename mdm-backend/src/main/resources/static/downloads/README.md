# APK Download Strategy

The provisioning endpoint remains:

- `/downloads/devora-mdm-latest.apk`

For production memory safety, host the APK outside the Spring Boot JAR and set:

- `APP_APK_EXTERNAL_URL=https://your-cdn-or-bucket/devora-mdm-latest.apk`

When this variable is set, the backend endpoint returns an HTTP redirect to the
external APK URL. This keeps the backend artifact small and avoids loading a
large APK file into memory.

## Update checklist

1. Build APK (`debug` or `release`) from the Android app.
2. Upload `devora-mdm-latest.apk` to your external storage/CDN.
3. Set Railway variable `APP_APK_EXTERNAL_URL` to that file URL.
4. Generate SHA-256 checksum in Base64 URL-safe format and update
   `DEFAULT_CHECKSUM` in `QrProvisioningHelper.kt`.
5. Regenerate a fresh provisioning QR code (old QR can fail).
