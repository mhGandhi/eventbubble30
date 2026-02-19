Unser Backend verwendet JWT-Tokens zur Authentifizierung.
2 Arten von Tokens:

1) ACCESS TOKEN
- Gültigkeit: ca. 15 Minuten
- Wird NUR im Arbeitsspeicher gehalten (State/Context/Variable)
- Wird NICHT persistent gespeichert
- Wird bei jeder API-Anfrage im HTTP-Header gesendet:
    Authorization: Bearer <ACCESS_TOKEN>

2) REFRESH TOKEN
- Gültigkeit: ca. 30 Tage
- Wird persistent gespeichert (z. B. SecureStore bei React Native)
- Wird NICHT automatisch an API-Endpunkte geschickt
- Wird NUR benutzt, um ein neues Access Token zu holen

-------------------------------------------------------------

LOGIN
POST /api/auth/login

Body:
{
  "username": "...", //username oder email
  "password": "..."
}

Response:
{
  "accessToken": "...",
  "accessTokenExpiry" : "...",
  "refreshToken": "...",
  "refreshTokenExpiry" : "...",
  "user": {
    "id": ...,
    "username": "..."
  }
}

FRONTEND-AUFGABE:
- accessToken im Arbeitsspeicher speichern
- refreshToken sicher speichern (SecureStore / AsyncStorage)
- user-Daten speichern

-------------------------------------------------------------

API-ANFRAGEN (mit Access Token)
Jede Anfrage an geschützte Endpunkte MUSS den Header enthalten:

Authorization: Bearer <ACCESS_TOKEN>

Wenn die API "401 Unauthorized" zurückgibt bedeutet das:
→ Das Access Token ist abgelaufen.
→ Jetzt einen Refresh durchführen.

-------------------------------------------------------------

REFRESH (neues Access Token holen)
POST /api/auth/refresh

Body:
{
  "refreshToken": "<REFRESH_TOKEN>"
}

Response:
{
  "accessToken": "NEU",
  "refreshToken": "GLEICH ODER NEU",
  "user": { ... }
}

FRONTEND-AUFGABE:
- neues Access Token speichern
- Action erneut ausführen
- refreshToken weiterhin speichern

Falls /refresh ebenfalls 401 zurückgibt:
→ Refresh Token ist ungültig oder abgelaufen
→ Benutzer muss sich neu einloggen

-------------------------------------------------------------

LOGOUT (alle Tokens ungültig machen)

FRONTEND-AUFGABE:
- accessToken aus dem Arbeitsspeicher löschen
- refreshToken aus SecureStore löschen

-------------------------------------------------------------

SESSION PRÜFEN (beim App Start)
Zwei Möglichkeiten:

1. Direkt refreshen:
- refreshToken laden
- /api/auth/refresh aufrufen
→ Wenn Antwort ok: eingeloggt
→ Wenn 401: Login-Screen

oder

2. validate:
- accessToken laden
- GET /api/auth/validate aufrufen
→ Wenn 200 ok → eingeloggt
→ Wenn 401 → refresh versuchen

-------------------------------------------------------------

FEHLERCODES
401 Unauthorized
- Access Token ist ungültig oder abgelaufen
- Refresh Token ist ungültig oder abgelaufen
→ neu einloggen falls Refresh nicht geht

403 Forbidden
- Benutzer ist eingeloggt, hat aber keine Rechte

-------------------------------------------------------------

ZUSAMMENFASSUNG FÜR EILIGE

1. Login → accessToken & refreshToken speichern
2. API-Requests → immer accessToken im Header senden
3. Bei 401 → /refresh aufrufen
4. Wenn refresh auch 401 → Benutzer neu einloggen
5. Logout → /invalidate-tokens + Tokens löschen

-------------------------------------------------------------

Refresh gibt auch expiry für tokens mit. Anhand dieser kann man automatisch refreshen oder überprüfen ob bereits ein Refresh vonnöten ist.

eif mit Postman testen