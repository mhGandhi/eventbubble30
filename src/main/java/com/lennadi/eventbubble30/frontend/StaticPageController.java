package com.lennadi.eventbubble30.frontend;

import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import com.lennadi.eventbubble30.features.service.TelegramNotifier;
import com.lennadi.eventbubble30.features.service.VeranstaltungService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RequiredArgsConstructor
@Controller
public class StaticPageController {

    private final VeranstaltungService veranstaltungService;
    private final TelegramNotifier telegramNotifier;

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

    @GetMapping(value = "/hase_meme", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> haseBild() throws IOException {
        telegramNotifier.send("HASE");

        ClassPathResource imgFile = new ClassPathResource("static/images/hase.jpg");
        byte[] bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    @GetMapping("/event/{id}")
    public String eventDetail(
            @PathVariable String id,
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

    @GetMapping("/moderation")
    public String moderation(){
        return "moderation";
    }

    @GetMapping("/ticket")
    public String ticket(){
        return "ticket";
    }
}
