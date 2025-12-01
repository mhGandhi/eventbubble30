package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.service.ProfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfilController {

    private final ProfilService profilService;

    //todo
}
