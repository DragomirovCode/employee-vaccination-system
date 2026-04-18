# Frontend

React + TypeScript + Vite frontend.

## Run

```bash
npm install
npm run dev
```

Dev server runs on `http://localhost:5173`.

## Backend connection

- Frontend API base URL defaults to `http://localhost:8080`
- Override with `VITE_API_BASE_URL=http://localhost:8080`
- All API requests use `credentials: "include"` and rely on the backend `JSESSIONID` cookie

## Auth flow

- Login page posts `email` and `password` to `POST /auth/login`
- Backend creates a server-side session and sets `JSESSIONID`
- Frontend restores auth state from the active cookie-backed session and calls `POST /auth/logout` to sign out
- If backend and frontend run on different origins, CORS must allow credentials

## Seeded local accounts

- `admin@evs.local` / `admin123`
- `hr@evs.local` / `hr123`
- `medical@evs.local` / `medical123`
- `petr.orlov@evs.local` / `employee123`
- `polina.smirnova@evs.local` / `employee123`

## Build

```bash
npm run build
```
