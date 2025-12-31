package com.lennadi.eventbubble30.frontend;

import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import com.lennadi.eventbubble30.features.service.VeranstaltungService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Controller
public class StaticPageController {

    private final VeranstaltungService veranstaltungService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/verify-email")
    public String verifyEmail() {
        return "verify-email";
    }

    @GetMapping("/event")
    public String eventIndex(Model model) {
        model.addAttribute("event", null);
        return "event";
    }

    @GetMapping("/event/{id}")
    public String eventDetail(
            @PathVariable Long id,
            Model model
    ) {
        Veranstaltung ev = veranstaltungService
                .getVeranstaltungById(id);

        if (ev == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("event", ev);
        return "event";
    }


    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/map")
    public String map() {
        return "map";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
