# aseado-server

Relay server sitting between the ASEADO desktop app and the ASEADO Android
scanner app, for when they're both online. It intentionally does **not**
contain any attendance/scan business logic — that stays on desktop
(`BruteForceService`). This server only:

- lets desktop publish/edit/delete **buckets** (mirrors desktop's own
  `Profile` — one bucket per profile, e.g. "1st Sem")
- lets desktop open a bucket for **receiving** (creates a `ReceivingSession`
  with its own event metadata + a fresh access key, closes = kills the key)
- lets Android **discover** currently-open buckets, verify a key, and
  **upload** a batch of scanned records into one
- lets desktop **pull pending batches** and mark them accepted/refused
  after it has actually processed them locally

## Data model

```
Bucket              — permanent profile identity (name, mode, department,
                       roster CSV — see "Roster sync" below)
 └─ ReceivingSession — one open-for-scanning cycle: just a key with a
                       lifetime, carries NO event data
     └─ BatchUpload  — one Android upload: the event Android decided on
                       its own (name, date, cutoff, filters, logout
                       enabled) + the scan records, bundled together
```

**Important correction from an earlier version of this design:** opening
a bucket for receiving does NOT create an event, and doesn't take any
event metadata at all — it's a bare toggle. Android decides the event
entirely on its own, offline, and sends that decision *with the batch
upload* (`UploadBatchRequest.eventMeta`). Desktop only creates the actual
local event once it accepts a batch, built from whatever Android sent —
this server never invents or pre-supplies event data itself.

A bucket can be opened/closed many times over its life; each open is a
fresh `ReceivingSession` with its own key, so an old session's key stops
working the moment it's closed, and old pending batches stay attached to
whichever session they arrived under.

## Roster sync (offline source of truth for Android)

A bucket can carry a raw student-list CSV — the same file desktop would
otherwise import locally. It's stored as an opaque blob (this server never
parses it, same as `filterJson`) and is only downloadable by Android while
a session is actually open, using the session's key:

1. Desktop publishes/replaces the roster: `POST /api/buckets/{id}/roster`
2. Desktop opens the bucket for receiving (fresh key generated) — this is
   the ON/OFF toggle, nothing else
3. Android verifies the key, sees `rosterAvailable: true`, downloads it:
   `POST /api/buckets/{id}/roster/download`
4. Android now has a real source of truth to scan against **entirely
   offline** — known IDs match against the downloaded roster; anything
   that doesn't match goes into a separate local "unknown" list with full
   details captured on the spot (name, year, program), kept apart from
   the synced roster so it's never at risk of corrupting it
5. Android decides the event (name/date/cutoff/filters) on its own,
   whenever it starts an offline scanning session — completely
   independent of desktop, no round-trip needed
6. On upload, the event Android decided rides along with the batch
   (`eventMeta`), and unknowns ride along too, landing in desktop's
   existing "Unknown history" resolve flow, pre-filled with whatever
   Android captured

This is also the natural gate for "can't start an offline event without a
profile selected" — no downloaded roster means no source of truth, which
means Android shouldn't let you start scanning yet.

## Auth

Two separate mechanisms, deliberately not unified (see the design
discussion this came out of — Android and desktop are different trust
levels):

- **Desktop**: `X-Admin-Key` header, checked against `ADMIN_KEY` env var.
  Simple shared secret for v1 — a real per-install identity is planned
  for the "move licensing online" follow-up, not this pass.
- **Android**: no header auth at all. `GET /discover` is public (department
  names aren't sensitive). Upload requires the session's own key *inside
  the request body*, checked against whichever session is currently open
  for that bucket.

## Endpoints

| Method | Path | Caller | Auth |
|---|---|---|---|
| POST | `/api/buckets` | Desktop | Admin key |
| GET | `/api/buckets` | Desktop | Admin key |
| GET | `/api/buckets/{id}` | Desktop | Admin key |
| PATCH | `/api/buckets/{id}` | Desktop | Admin key |
| DELETE | `/api/buckets/{id}` | Desktop | Admin key |
| POST | `/api/buckets/{id}/open` | Desktop | Admin key |
| POST | `/api/buckets/{id}/close` | Desktop | Admin key |
| GET | `/api/buckets/discover` | Android | none |
| POST | `/api/buckets/{id}/verify-key` | Android | none (checks key in body) |
| POST | `/api/buckets/{id}/roster` | Desktop | Admin key |
| POST | `/api/buckets/{id}/roster/download` | Android | none (checks key in body) |
| POST | `/api/buckets/{id}/batches` | Android | none (checks key in body) |
| GET | `/api/buckets/{id}/batches/pending` | Desktop | Admin key |
| POST | `/api/batches/{id}/accept` | Desktop | Admin key |
| POST | `/api/batches/{id}/refuse` | Desktop | Admin key |

Full request/response shapes are in `web/dto/BucketDtos.java` and
`web/dto/BatchDtos.java` — they're plain Java records, easiest to just
read directly.

### Example: desktop opens a bucket for receiving

```
POST /api/buckets/3/open
X-Admin-Key: <your admin key>
```
→
```json
{ "sessionId": 12, "key": "K7M9QXWZ" }
```

No body — opening doesn't decide anything about an event.

### Example: Android uploads a batch

```
POST /api/buckets/3/batches
Content-Type: application/json

{
  "key": "K7M9QXWZ",
  "eventMeta": {
    "eventName": "Freshmen Orientation",
    "eventDate": "2026-08-01",
    "loginTimeLimit": "08:00",
    "hasLogout": true,
    "filterJson": "{\"years\":[\"1\"],\"programs\":[]}"
  },
  "records": [
    {
      "studentId": "21-0001",
      "proofName": "Juan Dela Cruz",
      "proofYear": "1",
      "proofProgram": null,
      "loginTime": "2026-08-01T07:52:11Z",
      "logoutTime": null
    }
  ]
}
```

Android decided every field in `eventMeta` on its own, offline — desktop
only sees it once this batch is pulled and accepted.

## Running it

```bash
cp .env.example .env   # then fill in real values
docker compose --env-file .env up --build
```

Server listens on `:8081` (override with `PORT`).

Without Docker:

```bash
export ADMIN_KEY=...
export DATABASE_URL=jdbc:postgresql://your-neon-host/your-db?sslmode=require
export DATABASE_USERNAME=...
export DATABASE_PASSWORD=...
./mvnw spring-boot:run
```

**Security note:** the Neon connection details used during initial setup
were shared in plaintext in a chat and should be treated as compromised —
rotate the database password from the Neon dashboard once you've
confirmed the server connects, and update `DATABASE_PASSWORD` afterward.
Never commit a real `.env` file (already covered by `.gitignore`).

## Why PostgreSQL (Neon)

Managed, always-on, reachable from anywhere the server itself is deployed
— unlike the desktop app's per-profile SQLite files (which are meant to
live locally on one machine) or this server's earlier H2-file-mode setup
(fine for a single container instance, but doesn't survive redeploys
without persistent storage, and can't be shared across multiple server
instances if this ever needs to scale out).

## Known gaps (intentional, see design discussion)

- No online licensing yet — desktop's licence check stays local, exactly
  as it already works. Planned as the next update.
- No conflict resolution yet for "this student already has an
  `AttendanceRecord` for this event from somewhere else" — that's a
  desktop-side `BruteForceService` concern to solve next, not this
  server's job (it just relays whatever it's handed).
- Admin auth is a single shared secret for all desktop installs talking
  to one server instance. Fine for a v1/single-institution deployment,
  not fine for multi-tenant hosting — revisit alongside online licensing.
