# ISBN Book Sorter

휴대폰 카메라로 ISBN 바코드를 스캔하고, 조회된 책을 카테고리별로 저장하는 Android APK입니다.

## 주요 기능

- ISBN 바코드 스캔
- ISBN 직접 입력
- 카테고리별 도서 저장
- 로컬 SQLite 저장
- 국내 서지 API 키를 나중에 넣을 수 있는 빈칸 제공
- 국내 API 키가 비어 있으면 Google Books와 Open Library fallback 조회

## API 키 설정

API 키는 현재 비워둔 상태입니다.

설정 파일:

```text
app/src/main/res/values/api_keys.xml
```

조회 순서:

1. `national_library_key`가 있으면 국립중앙도서관 ISBN API를 먼저 사용합니다.
2. `aladin_ttb_key`가 있으면 알라딘 Open API를 다음으로 사용합니다.
3. 국내 API 키가 없으면 Google Books를 fallback으로 사용합니다.
4. Google Books 무키 요청이 quota 문제를 내면 Open Library ISBN API를 추가 fallback으로 사용합니다.
5. Google Books quota를 안정적으로 쓰려면 `google_books_key`를 나중에 넣을 수 있습니다.

## APK 빌드

```bash
./gradlew assembleDebug
```

빌드 결과:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Release

배포 APK는 GitHub Release에 업로드합니다.
