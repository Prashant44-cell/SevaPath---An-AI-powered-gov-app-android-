# SevaPath

### Citizen-powered infrastructure intelligence for India

SevaPath is an Android-first civic reporting platform that turns local citizen observations into structured development signals. Citizens can report infrastructure needs, follow their civic history, and understand which issues are receiving attention. The long-term platform combines multilingual intake with demographic, infrastructure and public-investment data to help decision-makers prioritize action.

This repository currently contains a native Android prototype with local authentication, account-scoped persistence, complaint tracking, civic news, likes, hotspot insights and a liquid-glass civic dashboard.

> Current boundary: SevaPath is a device-local prototype. External authentication, WhatsApp/SMS/IVR, live APIs, national datasets and cross-user real-time events are intentionally not claimed as connected.

## Product snapshot

| Area | Current prototype |
|---|---|
| Platform | Native Android, Kotlin, Jetpack Compose |
| Account flow | Local signup, login, logout and account isolation |
| Persistence | Local SQLite database with salted PBKDF2 password hashes |
| Citizen input | Text complaint form, language selector and simulated voice interaction |
| Civic history | Account-specific request timeline with status and evidence indicators |
| Civic news | Account activity, top complaint type, likes and 25+ viral marker |
| Insights | Static hotspot map and explainable priority score |
| Languages | English, Hindi and Odia selector; full localization is next phase |
| Visual system | Liquid-glass Dashboard and History / Indication Report sections |

## Why SevaPath

Citizen requests often remain fragmented across portals, phone calls and messaging channels. SevaPath provides a single citizen-facing entry point and a future-ready data model for:

- capturing needs in the citizen’s preferred language;
- grouping similar complaints by issue and area;
- showing transparent priority factors;
- tracking requests from submission to outcome;
- surfacing demand hotspots for public planning; and
- measuring whether infrastructure investment responds to observed need.

## Current user journey

1. Open SevaPath and log in or create an account.
2. A new account begins with zero requests, zero likes and zero personal news items.
3. Use **Report** to submit a text complaint and choose a language and issue category.
4. Use **Insights** to inspect the current demonstration hotspots and transparent score factors.
5. Use **News** to view account activity, complaint types, likes and viral indicators.
6. Use **Track** to view the account’s History / Indication Report.

Demo account:

```text
Email:    demo@sevapath.app
Password: demo123
```

## Architecture today

```text
MainActivity.kt
  ├─ Compose theme and app shell
  ├─ Auth, Dashboard, Report, Insights, News and Track screens
  ├─ CivicViewModel for prototype state and actions
  └─ liquid-glass presentation for Dashboard and History

SevaPathDb.kt
  ├─ users
  ├─ requests
  └─ news

PriorityScorer
  └─ transparent demand/gap/vulnerability/alignment score
```

The current single ViewModel is deliberate prototype scope. The next backend-ready split is `data`, `domain` and `ui`; a multi-module build is not required for the next phase.

## Project structure

```text
app/
├── src/main/java/com/sevapath/app/
│   ├── MainActivity.kt       # Compose UI and prototype ViewModel
│   └── SevaPathDb.kt         # Local SQLite persistence
├── src/test/java/com/sevapath/app/
│   └── PriorityScorerTest.kt # Priority and fresh-account checks
└── build/outputs/apk/debug/
    └── app-debug.apk         # Generated debug APK

SEVAPATH_IMPLEMENTATION_PLAN.md # Detailed architecture and API plan
```

## Run locally

### Prerequisites

- Android Studio with Kotlin and Jetpack Compose support.
- JDK 17.
- Android SDK Platform 36 and Build Tools 36.
- Android API 26 or newer emulator, or a USB-debuggable Android device.

### Android Studio

1. Open `E:\Hackathon\Build with AI` in Android Studio.
2. Wait for Gradle sync to complete.
3. Start an emulator or connect a physical device.
4. Select the `app` configuration and press **Run**.
5. Use the demo account above or create a fresh local account.

### Command line

From the project root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Verification checklist

### Functional smoke test

- Login with the demo account.
- Create a new account and confirm that its Dashboard and History show zero personal data.
- Submit a complaint and confirm it appears in Track and News.
- Like a news item and confirm the count updates.
- Log out, log in again and confirm the account data remains isolated.
- Scroll Dashboard and History to the end without content being hidden behind the bottom navigation.

### Verified workspace state

- Debug APK built successfully.
- Unit tests pass with `:app:testDebugUnitTest`.
- APK installed successfully on an Android emulator.
- Dashboard and History / Indication Report inspected at 1080×2424 resolution.

## Next-phase implementation

The next phase should be delivered as a small, testable vertical slice. It should not begin with a national data platform or a WhatsApp automation layer.

### Phase 2A — structured local MVP

| Work item | Priority | Dependencies | Acceptance criteria |
|---|---:|---|---|
| Move complaint/news records to Room | P0 | Room and migration setup | Restart preserves users, complaints, events and likes; every query is account-scoped |
| Extract repository boundary | P0 | Room schema | UI no longer reads or writes storage directly; repository tests cover ownership |
| Add complaint status events | P0 | Room entities | Each status change is append-only and renders in History |
| Add image picker and camera entry | P1 | Android picker, camera permission, app storage | One image previews locally, validates type/size and can be removed before submit |
| Add image draft state | P1 | Complaint draft model | Citizen can edit category, location and description before submission |
| Add OCR and metadata adapter | P1 | Bundled ML Kit / EXIF support | OCR text and image metadata populate the draft; failures leave an editable fallback |
| Extract English/Hindi resources | P1 | Android resources | Core auth/report/history strings render from resources and remain accessible |
| Add Compose smoke tests | P0 | Compose test dependencies | Auth, report, news like, History scroll and fresh-account flows are automated |

### Phase 2B — backend vertical slice

| Work item | Priority | Dependencies | Acceptance criteria |
|---|---:|---|---|
| Auth API | P0 | Identity provider, HTTPS | Signup, login, logout, reset, expiry and account deletion work across devices |
| Complaint API | P0 | Auth, PostgreSQL | Complaint ownership and status history are enforced server-side |
| Evidence upload | P1 | Object storage | Presigned upload, MIME/size validation and retention metadata work end-to-end |
| Feed and likes API | P0 | Complaint events | Likes are unique per user and feed changes are idempotent |
| Near-real-time updates | P0 | Backend events, FCM | Foreground feed refreshes from server events; background users receive notifications |
| Moderation queue | P1 | Roles and review states | Low-confidence or abusive reports are reviewable without data loss |

### Phase 2C — civic intelligence

- multilingual embeddings for similar-request detection;
- area/category/time aggregation with population normalization;
- GIS boundary and ward mapping;
- national demographic and infrastructure ETL;
- government scheme and investment-plan matching;
- explainable, configurable priority scoring;
- department workflow and policymaker dashboard; and
- outcome, budget and beneficiary impact measurement.

## Image-to-complaint design

The first image release is single-image-first and citizen-confirmed.

```text
Gallery / Camera
       ↓
Validate and preview locally
       ↓
EXIF + dimensions + timestamp
       ↓
On-device OCR: English + Hindi
       ↓
Category suggestion + confidence
       ↓
GPS → EXIF → OCR → map pin → manual location
       ↓
Editable complaint draft
       ↓
Citizen confirmation and upload
```

Initial civic categories:

`POTHOLE`, `ROAD_DAMAGE`, `GARBAGE_DUMP`, `BROKEN_STREETLIGHT`, `FLOODING`, `WATER_LEAKAGE`, `OPEN_DRAIN`, `DAMAGED_PUBLIC_PROPERTY`, `FALLEN_TREE`, `OTHER`.

The MVP should require human confirmation when classification confidence is below `0.70`, location confidence is below `0.60`, OCR conflicts with the selected location, or the image is unreadable. Multi-image, video and automated severity estimation come later.

## Backend contract baseline

```text
POST   /v1/auth/signup
POST   /v1/auth/login
POST   /v1/auth/refresh
POST   /v1/auth/logout
DELETE /v1/me

POST   /v1/evidence/uploads
POST   /v1/evidence/{id}/analyze
GET    /v1/evidence/{id}

POST   /v1/complaint-drafts
PATCH  /v1/complaint-drafts/{id}
POST   /v1/complaints
GET    /v1/me/complaints?cursor=...
GET    /v1/complaints/{id}/events

GET    /v1/me/feed?cursor=...
POST   /v1/feed/{eventId}/like
DELETE /v1/feed/{eventId}/like
GET    /v1/areas/{areaId}/insights

POST   /v1/webhooks/whatsapp/meta
POST   /v1/webhooks/sms/{provider}
POST   /v1/webhooks/ivr/{provider}
```

All write requests need HTTPS, authentication, request IDs and idempotency keys. Provider webhooks must verify signatures and deduplicate provider event IDs before creating or updating a complaint.

## WhatsApp integration boundary

WhatsApp should be implemented through an official business messaging backend with webhook verification and server-side credentials. The Android app must not contain provider secrets or attempt to maintain a personal WhatsApp Web session. The connection work begins only after the Meta/provider account, backend callback URL, permissions and message-routing contract exist.

## Data and privacy boundary

The prototype stores data on-device. Uninstalling the app or clearing app data removes the local database. There is no cross-device sync, password reset or production account recovery yet.

The production design must add:

- server-side ownership enforcement;
- encrypted transport and protected token storage;
- consent for precise location and media processing;
- retention and deletion policies for images, audio and PII;
- face/vehicle-plate handling for public evidence;
- moderation and audit events; and
- aggregated area reporting that does not expose individual citizens.

## Production readiness checklist

- [ ] Backend identity and session lifecycle
- [ ] Room migration and repository tests
- [ ] Complaint API with ownership and status events
- [ ] Evidence storage and image analysis worker
- [ ] Verified WhatsApp/SMS/IVR webhooks
- [ ] FCM notifications and offline sync queue
- [ ] English/Hindi resource extraction and accessibility audit
- [ ] Compose UI and physical-device test matrix
- [ ] Crash and ANR monitoring
- [ ] CI/CD, production signing and staged release
- [ ] Privacy policy, deletion workflow and data-retention controls
- [ ] Public API/schema/model documentation for Digital Public Good release

## Detailed design reference

For the full Kotlin models, persistence schema, pseudocode, risk register and architecture rationale, read [SEVAPATH_IMPLEMENTATION_PLAN.md](SEVAPATH_IMPLEMENTATION_PLAN.md).

## License and contribution

SevaPath is being developed as a Digital Public Good concept. Before public release, add the final open-source license, contribution guide, API license, model cards, dataset provenance and deployment documentation.
#   S e v a P a t h - - - A n - A I - p o w e r e d - g o v - a p p - a n d r o i d -  
 