# BarangayGo — Android Studio Setup Guide

## Project Structure
```
BarangayGo/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/barangaygo/
│   │   │   ├── activities/
│   │   │   │   ├── SplashActivity.java      ← Launch screen
│   │   │   │   ├── LoginActivity.java       ← Login + Register tabs
│   │   │   │   ├── RegisterActivity.java    ← Registration logic
│   │   │   │   ├── MainActivity.java        ← Resident home
│   │   │   │   ├── QueueTicketActivity.java ← Live queue ticket
│   │   │   │   └── AdminActivity.java       ← Admin dashboard
│   │   │   └── models/
│   │   │       ├── User.java
│   │   │       ├── Booking.java
│   │   │       └── Service.java
│   │   └── res/
│   │       ├── layout/                      ← All XML screen layouts
│   │       ├── drawable/                    ← Icons + shape backgrounds
│   │       ├── values/                      ← Colors, strings, themes, dimens
│   │       ├── values-night/                ← Dark mode theme overrides
│   │       └── font/                        ← Nunito font files (see below)
│   └── build.gradle
└── build.gradle
```

---

## Step 1 — Open in Android Studio
1. Open Android Studio → **File > Open** → select the `BarangayGo` folder
2. Wait for Gradle sync to finish

---

## Step 2 — Add Nunito Font
1. Go to https://fonts.google.com/specimen/Nunito
2. Download: Regular (400), Bold (700), ExtraBold (800)
3. Rename the files:
   - `Nunito-Regular.ttf`    → `nunito_regular.ttf`
   - `Nunito-Bold.ttf`       → `nunito_bold.ttf`
   - `Nunito-ExtraBold.ttf`  → `nunito_extrabold.ttf`
4. Place them in: `app/src/main/res/font/`

---

## Step 3 — Firebase Setup
1. Go to https://console.firebase.google.com
2. Create a new project called **BarangayGo**
3. Add an **Android app** with package name: `com.example.barangaygo`
4. Download `google-services.json` and place it in: `app/`
5. Enable **Authentication** → Email/Password
6. Enable **Firestore Database** → Start in test mode
7. Enable **Cloud Messaging** (for push notifications)

---

## Step 4 — Firestore Collections to Create
```
/users/{uid}
  - name: string
  - email: string
  - role: "resident" | "admin"
  - contact: string
  - createdAt: timestamp

/services/{serviceId}
  - name: string
  - description: string
  - requirements: array
  - estimatedMinutes: number
  - isAvailable: boolean

/bookings/{bookingId}
  - userId: string
  - serviceId: string
  - serviceName: string
  - queueNumber: string
  - residentName: string
  - status: "waiting" | "serving" | "done" | "cancelled"
  - aheadCount: number
  - createdAt: timestamp

/queue_slots/{slotId}
  - serviceId: string
  - date: string (YYYY-MM-DD)
  - timeRange: string
  - maxCapacity: number
  - currentCount: number
  - currentServing: string
  - status: "open" | "closed"
```

---

## Step 5 — Set Admin Role
To make a user an admin, go to Firestore Console:
- Open `users/{uid}` document
- Change `role` field from `"resident"` to `"admin"`

---

## Screens in First Stage
| Screen | Activity | Status |
|--------|----------|--------|
| Splash | SplashActivity | ✅ Done |
| Login / Register | LoginActivity | ✅ Done |
| Resident Home | MainActivity | ✅ Done |
| Queue Ticket | QueueTicketActivity | ✅ Done |
| Admin Dashboard | AdminActivity | ✅ Done |

## Coming in Phase 2
- Book a Queue screen (date + time slot picker)
- Service Detail screen (requirements list)
- Profile screen

## Coming in Phase 3
- Admin queue management
- Announcements screen
- Push notifications (FCM)
- Analytics / peak hours chart
