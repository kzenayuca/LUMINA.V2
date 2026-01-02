package com.LuminaWeb.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        // devolver una plantilla de error si la tienes: templates/error.html
        return "error"; // crea templates/error.html o cámbialo por "index"
    }
}
