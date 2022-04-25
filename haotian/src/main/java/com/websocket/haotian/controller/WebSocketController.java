package com.websocket.haotian.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
public class WebSocketController {

    @GetMapping("/websocket/{username}")
    public String webSocket(@PathVariable String username, Model model) {
        try {
            log.info("Go to socket pages");
            model.addAttribute("username", username);
            return "websocket";
        } catch(Exception e) {
            log.info("error when redirect to websocket page, error detail: " + e.getMessage());
            return "error";
        }
    }
}
