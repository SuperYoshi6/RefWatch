# Passwort-Reset-E-Mail einrichten

Diese Anleitung zeigt, wie du die in `docs/email-templates/` mitgelieferten
HTML-Vorlagen in der Firebase Console aktivierst, damit die
"Passwort vergessen?"-Mail in **Englisch und Deutsch** verfügbar ist,
**ansprechend aussieht** und **seltener im Spam landet**.

## Was im Code bereits erledigt ist

- Der Client setzt `ActionCodeSettings.url` auf die gebrandete
  RefWatch-Website (`https://superyoshi6.github.io/RefWatch/auth/action?mode=resetPassword`).
  Der Reset-Link zeigt also auf die eigene Domain – das erhöht das
  Vertrauen von Spam-Filtern erheblich.
- Der Client setzt `setLanguageCode(...)` anhand des Geräte-Locale,
  sodass Firebase automatisch das passende Template wählt.
- Eine Cloud Function `requestPasswordReset` (in `functions/index.js`)
  erzeugt zusätzlich einen lokalisierten Reset-Link per Admin SDK
  (nützlich für Tests oder eine spätere Anbindung an einen
  Transaktional-E-Mail-Dienst wie SendGrid/Mailgun).
- Demo-Login wurde entfernt.

## Was du noch in der Firebase Console machen musst

### 1. Absender / Branding konfigurieren (gegen Spam)

Firebase Console → **Authentication → Settings → User account**:
- **From name**: `RefWatch` (statt `no-reply@…firebaseapp.com`).
- **Reply-to address**: z. B. `support@refwatch.app` (optional, aber
  empfohlen – erhöht die Zustellbarkeit).
- **Default language for email templates**: `en`.

> Branded Absender + Reply-to sind der größte Hebel gegen den
> Spam-Ordner, weil Webmail-Anbieter (Gmail, Outlook, GMX, Web.de)
> zuerst die "From"-Domain gegen SPF/DKIM prüfen.

### 2. Deutsches Template anlegen

Firebase Console → **Authentication → Templates** → **Password reset** →
**Edit pen**:
1. Sprache oben rechts auf **Deutsch (de)** umstellen.
2. **Subject (Betreff)**: `Setze dein RefWatch-Passwort zurück`
3. **From name**: `RefWatch` (überschreibt die Einstellung aus Schritt 1
   für diese Sprache).
4. Im Text-Editor in der Symbolleiste auf **`<>`** (HTML-Quelltext) klicken.
5. Den kompletten Inhalt von `docs/email-templates/password_reset_de.html`
   einfügen.
6. **Speichern**.

### 3. Englisches Template anlegen

Dasselbe wie in Schritt 2, aber:
- Sprache: **English (en)**
- Subject: `Reset your RefWatch password`
- Inhalt von `docs/email-templates/password_reset_en.html`

### 4. Testen

1. Im Template-Editor auf **"Preview email"** klicken.
2. In der App: **Passwort vergessen?** mit der eigenen Adresse tippen.
3. Mail im Postfach **und im Spam-Ordner** prüfen.
4. Bei Bedarf in Gmail: *"Not spam"* markieren und als
   *"Always allow from refwatch@…"* speichern – das hilft auch
   anderen Empfängern mit gleichem Absender.

## Optional: Eigene Domain für ausgehende Mails

Für produktiven Einsatz empfehlenswert:

1. **Eigene Absender-Domain** z. B. `mail.refwatch.app` mit MX-Eintrag
   auf Firebase / SendGrid / Mailgun.
2. **SPF** (`v=spf1 include:_spf.firebasemail.com ~all`) und
   **DKIM**-Records im DNS setzen (Firebase zeigt die passenden
   Einträge nach Klick auf *"Custom domains"*).
3. In Firebase Console unter **Authentication → Settings** den
   Custom Mailer eintragen.

Damit wandert die Mail praktisch nie mehr in den Spam.

## Optional: Trigger-Email-Extension

Wenn du den Reset-Link *komplett selbst* gestalten willst (z. B. mit
demselben HTML wie in den Vorlagen), installiere die Firebase-Extension
**"Trigger Email"** und hänge in `functions/index.js` an den
`requestPasswordReset`-Aufruf ein `firestore.document(...).onCreate(...)`
an, das den generierten `link` per SMTP verschickt. Der
`generatePasswordResetLink`-Aufruf ist dafür schon eingebaut.
