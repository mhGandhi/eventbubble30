package com.lennadi.eventbubble30;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Eventbubble30Application {

    public static void main(String[] args) {
        SpringApplication.run(Eventbubble30Application.class, args);
    }

    /*todo
    Inspo:
        Bookmarking/Going

        embeds (events, builder for params)

        change req logging

        JacksonEntitySerializationBlocker: Dass man nd ausversehen ALLES zurückschickt

        Soft-Delete: Gelöschte Entitäten mit "boolean: deleted" o.Ä. flaggen + timestamp setzen um Wiederherstellung
        zu erlauben. Nach bestimmter Zeit dann löschen.

        Rate-Limiting: login, refresh und signup pro IP + pro Username limitieren

        Moderation: Nutzer suspendieren, bannen, löschen. Beiträge melden. Moderator Ansicht für gemeldete Beiträge

        Captcha: Altcha o.Ä. (free und am besten self hosted)

        Event-Erinnerungen, Tags+Kategorien für Events, einfache Suche +Filter, Privatsphäreeinstellungen Profile+Events, Webhooks, Session Overview, MFA, Cache Layer, Telemetrie
     */
}
