package com.lennadi.eventbubble30;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Eventbubble30Application {

    public static void main(String[] args) {
        SpringApplication.run(Eventbubble30Application.class, args);
    }

    /*todo
    Inspo:
        Audit-Log: Neues Entity das speichert WER WANN WAS(+Details) MIT WAS gemacht hat.
        Bekommt ein Repo und einen Manager mit einer .log([...]) Methode

        Ortschaften: Orte von Veranstaltungen irgendwie Speichern. Dazu vlt OpenStreetmapAPI oder so

        iCal Export: api/events/{id}/export.ics -> Endpunkt für Kalenderexport

        api/admin endpoint: das bisschen von dem regulären Betrieb trennen.
        Extra Endpunkte wie active-users, user-count und Audit-Log Abruf etc. -> später im Frontend Admin Dashboard

        Email Verifikation: an bestehende, unimplementierte Endpunkte in AuthController anknüpfen.
        Email Schicken + Token Speichern (mit Ablauf)->Token Abgleichen und Löschen (+pw ändern)
     */
}
