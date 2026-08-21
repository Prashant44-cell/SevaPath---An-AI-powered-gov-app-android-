# SevaPath implementation plan

## 1. Completed vs pending

| Feature | Status | Priority | Owner layer | Current gap | MVP action | Phase 2 action |
|---|---|---:|---|---|---|---|
| Signup/login | Prototype complete | P0 | Android | Local credentials only | Keep local demo flow and validation | Backend identity, reset, expiry and deletion |
| Fresh-account isolation | Complete | P0 | Android | Device-local only | Preserve empty requests/news/likes on signup | Enforce ownership in API and database |
| Session/data persistence | Prototype complete | P0 | Android | SharedPreferences is not a relational store | Move structured data to Room | Server sync and encrypted token storage |
| Text complaint | Complete | P0 | Android | No remote submission | Keep editable form and local draft | API-backed complaint lifecycle |
| Voice intake | Simulated | P1 | Android + language service | No microphone/STT | Add permission and voice-ready draft state | Bhashini/AI4Bharat ASR and language detection |
| Image complaint drafting | Pending | P1 | Android + ML | No picker, camera or analyzer | Single image, metadata, OCR, editable draft | Backend verification and multi-image evidence |
| Complaint tracking | Prototype complete | P0 | Android | Static status history | Store status events locally | Department assignment and official workflow |
| News/activity feed | Prototype-local | P0 | Android | Not shared across users | Keep account pulse and likes | WebSocket/SSE/FCM event feed |
| Area insights | Static demo | P1 | Android + analytics | Hardcoded hotspots | Define aggregation API and local adapter | GIS, demographic and infrastructure joins |
| Multilingual UI | Partial | P1 | Android + language service | Only selector exists; strings are inline | Extract English/Hindi resources | Add Odia and language-service responses |
| WhatsApp/SMS/IVR | Pending | P1 | Backend + providers | No provider credentials/webhooks | Define webhook contracts | Meta Cloud API, SMS and IVR adapters |
| Admin/policymaker path | Pending | P1 | Web/backend | No roles or dashboard | Define RBAC and insight endpoints | Build dashboard, exports and review queue |
| Impact measurement | Pending | P2 | Backend/data | No outcome data | Add event fields to complaint model | Join budget, asset and beneficiary outcomes |
| Accessibility/release QA | Partial | P0 | Android/QA | No device test run | Add Compose smoke tests and semantics | Device matrix, CI and release hardening |

## 2. Target architecture

```text
Android Compose
  ├─ Auth, Report, Image Draft, Track, News, Insights
  ├─ ViewModels + StateFlow
  ├─ Room cache + WorkManager sync queue
  └─ Camera, picker, GPS, OCR/classifier adapters
          │ HTTPS / WebSocket / FCM
Backend API
  ├─ Auth and RBAC
  ├─ Complaint and status service
  ├─ Evidence/object storage
  ├─ WhatsApp/SMS/IVR webhook adapters
  ├─ Image analysis and moderation workers
  ├─ Feed/likes/trend aggregation
  └─ Admin/policymaker API
          │
Data platform
  ├─ PostgreSQL + PostGIS
  ├─ Object storage
  ├─ Search/vector index for similar complaints
  ├─ ETL for demographic/infrastructure/scheme data
  └─ Analytics tables/materialized views
```

### Responsibility split

- Android: capture, permissions, local preview, draft editing, offline queue, cached status and accessible UI.
- Backend: identity, ownership, canonical complaint state, webhook ingestion, moderation, fan-out events and audit trail.
- On-device ML: fast OCR, metadata extraction and an initial category suggestion.
- Backend ML: heavier verification, multilingual embeddings, duplicate detection and retraining metrics.
- Policy analytics: only aggregated or consent-approved area data leaves the citizen scope.

The current single `CivicViewModel` is acceptable for the visual prototype. The next code split should be `data`, `domain` and `ui` packages with one repository per data source; no multi-module build is needed yet.

## 3. Build-ready implementation plan

### Phase 1 — local MVP hardening

1. Replace structured SharedPreferences records with Room entities and DAOs.
2. Add a repository that keeps account ownership in every query.
3. Add a `ComplaintDraft` state and single-image picker/camera flow.
4. Add metadata extraction, OCR adapter and a low-confidence/manual fallback.
5. Extract English/Hindi strings into resources and add content descriptions.
6. Add Compose tests for signup, restart persistence, image draft editing, submit and scrolling.

### Phase 2 — backend vertical slice

1. Implement auth and complaint endpoints.
2. Add PostgreSQL/PostGIS and object storage.
3. Add presigned evidence upload and server-side validation.
4. Add complaint status events and feed fan-out.
5. Add FCM notifications; use WebSocket/SSE only for foreground live feed updates.
6. Replace local account store with a repository that is cache-first and syncs through WorkManager.

### Phase 3 — civic intelligence

1. Add duplicate/similar complaint search.
2. Build area/category/time aggregation jobs.
3. Add GIS boundary mapping and national-data ETL connectors.
4. Add explainable scheme matching and configurable priority weights.
5. Add moderator and policymaker dashboards.

## 4. Kotlin models

```kotlin
enum class LocationSource { CAMERA_GPS, IMAGE_EXIF, OCR_INFERRED, USER_SELECTED_MAP, MANUAL_ENTRY }

enum class ComplaintCategory {
    POTHOLE, ROAD_DAMAGE, GARBAGE_DUMP, BROKEN_STREETLIGHT,
    FLOODING, WATER_LEAKAGE, OPEN_DRAIN, DAMAGED_PUBLIC_PROPERTY,
    FALLEN_TREE, OTHER
}

data class LocationEvidence(
    val latitude: Double?,
    val longitude: Double?,
    val locality: String?,
    val ward: String?,
    val city: String?,
    val district: String?,
    val state: String?,
    val source: LocationSource,
    val confidence: Float
)

data class ImageComplaintDraft(
    val localUri: String,
    val remoteUrl: String? = null,
    val capturedAt: Instant?,
    val width: Int,
    val height: Int,
    val recognizedText: String,
    val inferredAddress: String?,
    val category: ComplaintCategory,
    val issueConfidence: Float,
    val ocrConfidence: Float,
    val location: LocationEvidence?,
    val humanReviewRequired: Boolean,
    val analysisState: AnalysisState
)

enum class AnalysisState { LOCAL_PENDING, ANALYZING, READY, NEEDS_REVIEW, FAILED }

data class Complaint(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String,
    val category: ComplaintCategory,
    val language: String,
    val location: LocationEvidence?,
    val evidence: List<ImageComplaintDraft>,
    val status: ComplaintStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class ComplaintStatus { DRAFT, RECEIVED, UNDER_REVIEW, VERIFIED, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED }

data class ActivityEvent(
    val id: String,
    val complaintId: String?,
    val type: String,
    val title: String,
    val body: String,
    val likes: Int,
    val likedByMe: Boolean,
    val createdAt: Instant
)
```

`Instant` should come from `kotlinx.datetime` or `java.time.Instant` consistently across Android and backend. Do not mix wall-clock strings with event ordering.

## 5. API contracts

All endpoints use HTTPS, bearer access tokens, request IDs and an idempotency key for writes.

```text
POST /v1/auth/signup
POST /v1/auth/login
POST /v1/auth/refresh
POST /v1/auth/logout
POST /v1/auth/password-reset/request
DELETE /v1/me

POST /v1/evidence/uploads              -> presigned upload URL
POST /v1/evidence/{id}/analyze         -> analysis job/status
GET  /v1/evidence/{id}

POST /v1/complaint-drafts
PATCH /v1/complaint-drafts/{id}
POST /v1/complaints
GET  /v1/me/complaints?cursor=...
GET  /v1/complaints/{id}
GET  /v1/complaints/{id}/events

GET  /v1/me/feed?cursor=...
POST /v1/feed/{eventId}/like
DELETE /v1/feed/{eventId}/like
GET  /v1/areas/{areaId}/insights

POST /v1/webhooks/whatsapp/meta
POST /v1/webhooks/sms/{provider}
POST /v1/webhooks/ivr/{provider}
GET  /v1/events/stream                    -> authenticated SSE/WebSocket
```

Example draft response:

```json
{
  "id": "draft_123",
  "status": "NEEDS_REVIEW",
  "category": "ROAD_DAMAGE",
  "issueConfidence": 0.82,
  "ocrConfidence": 0.71,
  "location": {
    "source": "OCR_INFERRED",
    "locality": "Bhairavpur",
    "ward": "4",
    "confidence": 0.64
  },
  "humanReviewRequired": true
}
```

Webhook handlers must verify provider signatures, deduplicate by provider event ID, store the raw event in a restricted table, and enqueue parsing. They must not create a complaint directly on an unverified or duplicate event.

## 6. Image-intelligence MVP

### Android flow

1. Select one image using the system picker or capture one with the camera.
2. Validate MIME type, size and dimensions before copying into app storage.
3. Show the local preview immediately; never block the form on analysis.
4. Extract dimensions, timestamp, orientation and EXIF GPS where available.
5. Run OCR on-device for Latin and Devanagari text.
6. Run a small classifier or label-to-category rule set.
7. Resolve location with this precedence: in-app GPS, EXIF GPS, OCR, map pin, manual entry.
8. Populate an editable complaint draft.
9. Set `humanReviewRequired` when category confidence is below `0.70`, OCR conflicts with location, or location confidence is below `0.60`.
10. Upload only after the citizen confirms the draft.

### Practical model choice

- OCR MVP: bundled Google ML Kit Text Recognition for Latin and Devanagari, with a backend fallback for difficult images.
- Category MVP: a small TFLite classifier trained on labelled Indian civic images; use `OTHER` below the confidence threshold.
- Metadata: Android media/EXIF parsing plus a server-side metadata check.
- Reverse geocoding: backend/GIS service, never a guessed ward label on-device.
- Training data: thousands of diverse images per category, Indian lighting/monsoon/road conditions, hard negatives, locality balance and a held-out state-level test split.

### Pseudocode

```text
onImageSelected(uri):
  localFile = copyAndValidate(uri)
  preview(localFile)
  state = ANALYZING

  metadata = readMetadata(localFile)
  ocr = runBundledOcr(localFile)
  category = classify(localFile, ocr.text)
  location = chooseLocation(deviceGps, metadata.exifGps, inferAddress(ocr.text), userMapPin)

  draft = ImageComplaintDraft(
    localUri = localFile.uri,
    capturedAt = metadata.timestamp,
    width = metadata.width,
    height = metadata.height,
    recognizedText = ocr.text,
    inferredAddress = location.address,
    category = category.labelOrOther,
    issueConfidence = category.confidence,
    ocrConfidence = ocr.confidence,
    location = location.evidence,
    humanReviewRequired = category.confidence < 0.70 || location.confidence < 0.60,
    analysisState = if (needsReview) NEEDS_REVIEW else READY
  )
  showEditableDraft(draft)
```

Multi-image upload, video analysis and automatic severity estimation are later features.

## 7. Persistence schema

### Android MVP: Room

Room is the correct next local database because complaints, status events, feed events and evidence have relationships, indexes, migrations and account-scoped queries. SharedPreferences remains only for small UI settings and should not hold the complaint dataset.

Tables:

```text
users(id, email, display_name, created_at, deleted_at)
complaints(id, owner_id, title, description, category, language, status, lat, lon, area_id, created_at, updated_at)
complaint_events(id, complaint_id, actor_id, from_status, to_status, note, created_at)
evidence(id, complaint_id, local_uri, remote_url, mime_type, sha256, analysis_state, created_at)
image_analysis(evidence_id, recognized_text, inferred_address, category, issue_confidence, ocr_confidence, location_source, human_review_required)
feed_events(id, owner_id, complaint_id, type, title, body, created_at)
feed_likes(event_id, user_id, created_at, UNIQUE(event_id, user_id))
sync_queue(id, entity_type, entity_id, operation, payload, attempt_count, next_attempt_at)
```

Every user-owned query must include `owner_id = currentUserId`; a UI filter is not an ownership boundary.

### Future backend

Use PostgreSQL + PostGIS for canonical data, object storage for evidence, Redis or a queue for event fan-out, and a search/vector index for similar complaints. Store raw provider events separately from normalized complaints for audit and replay.

## 8. Core-flow pseudocode

```text
signup(email, password):
  validate fields
  backend creates user and session
  local Room transaction creates empty user profile
  navigate to Home with zero counters

submitComplaint(draft):
  validate ownership, consent, size and category
  save local complaint + sync_queue transactionally
  upload evidence when network is available
  create remote complaint with idempotency key
  observe status events and update local Room

like(eventId):
  insert/delete unique local like
  enqueue idempotent API mutation
  update feed count optimistically
  reconcile with server response

receiveEvent(event):
  verify server event and version
  upsert event by event ID
  update complaint/feed projection in one transaction
  notify the active screen and FCM if backgrounded
```

## 9. MVP vs Phase 2

### Build now

- Room migration from the current local store.
- Single-image picker/camera flow with preview and editable draft.
- Metadata + OCR + category suggestion + confidence fallback.
- English/Hindi resource extraction.
- Backend contract and a small auth/complaint service.
- Account-owned complaint list, status events and likes.
- FCM notifications and near-real-time feed refresh.
- Compose UI tests and one physical-device smoke pass.

### Build later

- WhatsApp/SMS/IVR production adapters.
- Multi-image/video evidence.
- National ETL and public investment matching.
- Vector duplicate detection and retraining pipeline.
- Department workflow and policymaker dashboard.
- Impact/budget/beneficiary measurement.
- Cross-device conflict resolution and advanced offline sync.

## 10. Risks and edge cases

- Uninstall or data-clear removes the local Room database; only backend sync restores data.
- Local authentication cannot provide password reset, cross-device login, revocation or account recovery.
- Provider webhook retries can duplicate complaints unless event IDs and idempotency are enforced.
- EXIF GPS may be absent, stale or intentionally edited; always show source and confidence.
- OCR can read signs incorrectly, especially in glare, rain, low light and mixed scripts.
- A classifier can confuse garbage, road damage and flooding; low confidence must remain editable.
- GPS can be spoofed or unavailable; do not silently convert it into a precise ward claim.
- Likes need rate limits, abuse controls and server-side uniqueness.
- Viral thresholds must be normalized by area population and time, not raw likes alone.
- Offline queues need backoff, payload expiry, conflict rules and visible retry state.
- PII, faces, vehicle plates and audio need retention/deletion rules before public analytics.
- National datasets differ in identifiers, update schedules, granularity and licensing; LGD-style stable keys are essential.
- Android OEM background limits can delay uploads and notifications; WorkManager constraints are required.

## 11. Final recommendation

Build the image-to-complaint draft and Room migration next. They are the highest-value features that can be implemented and tested without pretending that WhatsApp, national data, real-time events or production identity already exist. In parallel, freeze the API contracts and backend ownership rules. Add WhatsApp and policy analytics only after the authenticated complaint API, webhook verification and audit model are working.

Current production boundary: SevaPath remains a local prototype until the backend, provider integrations, device QA and security review are connected and tested.
