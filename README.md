# ISBN Book Sorter

Android APK for scanning ISBN barcodes and organizing books by category.

## API keys

API keys are intentionally blank for now:

- `app/src/main/res/values/api_keys.xml`

Lookup order:

1. National Library of Korea ISBN API, when `national_library_key` is set.
2. Aladin Open API, when `aladin_ttb_key` is set.
3. Google Books fallback without a key, or with `google_books_key` if Google quota requires it later.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```
