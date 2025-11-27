package com.lennadi.eventbubble30;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Eventbubble30Application {
    public static final String DOMAIN = "eventbubble.lennadi.com";

    public static void main(String[] args) {
        SpringApplication.run(Eventbubble30Application.class, args);
    }

    /*todo
    Inspo:
        Ortschaften: Orte von Veranstaltungen irgendwie Speichern. Dazu vlt OpenStreetmapAPI oder so, (Nominatim?)

        -> später im Frontend Admin Dashboard

        @PreAuthorize 401: PreAuthorize gibt stets 403 zurück, auch wenn eigentlich 401 angebracht wäre :/

        JacksonEntitySerializationBlocker: Dass man nd ausversehen ALLES zurückschickt

        Gelöschte Nutzer nicht Authentifizieren: Gültige Tokens für nicht existierende Benutzer sind gefährlich!

        Email Verifikation: an bestehende, unimplementierte Endpunkte in AuthController anknüpfen.
        Email Schicken + Token Speichern (mit Ablauf @Scheduled)->Token Abgleichen und Löschen (wenn pw geändert)

        Profile: Entity getrennt von Benutzer, verbunden mit 1zu1 Beziehung, separate Endpunkte aber dennoch über
        Id des Users identifiziert -> Bio, Name, Events

        Medien: Object Storage, URLs in Entities speichern, ans Frontend nur temporär signierte URLs
        -> Avatare, Bilder für Veranstaltungen, Banner etc

        Soft-Delete: Gelöschte Entitäten mit "boolean: deleted" o.Ä. flaggen + timestamp setzen um Wiederherstellung
        zu erlauben. Nach bestimmter Zeit dann löschen.

        Rate-Limiting: login, refresh und signup pro IP + pro Username limitieren

        Moderation: Nutzer suspendieren, bannen, löschen. Beiträge melden. Moderator Ansicht für gemeldete Beiträge

        Captcha: Altcha o.Ä. (free und am besten self hosted)

        Event-Erinnerungen, Tags+Kategorien für Events, einfache Suche +Filter, Privatsphäreeinstellungen Profile+Events, Webhooks, Session Overview, MFA, Cache Layer, Telemetrie
     */
}
