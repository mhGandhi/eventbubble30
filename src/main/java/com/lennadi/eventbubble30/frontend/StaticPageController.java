package com.lennadi.eventbubble30.frontend;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class StaticPageController {

    private static final Set<String> ALLOWED = Set.of(
            "reset-password",
            "verify-email",
            "event",
            "profile",
            "me",
            "map"
    );

    @GetMapping("/")
    public String index() {
        return "index"; //--> templates/index.html
    }

    @GetMapping("/{page}")
    public String page(@PathVariable String page) {
        if (!ALLOWED.contains(page)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return page;
    }
}

