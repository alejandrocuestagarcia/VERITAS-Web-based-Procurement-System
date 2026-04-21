package com.veritas.backend.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    // Regex explained: Match any path that does NOT contain a dot (.)
    // This allows requests for .js, .css, and .png to bypass this and hit the files directly
    @GetMapping(value = "/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}