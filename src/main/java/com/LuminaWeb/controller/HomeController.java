package com.LuminaWeb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @GetMapping({"/","/login"})
    public String login() {
        return "Inicio_Sesion"; // templates/Inicio_Sesion.html
    }
    @GetMapping("/{area}/{page}.html")
    public String areaPage(@PathVariable("area") String area, @PathVariable("page") String page) {
        // sanitize inputs si lo deseas (evitar ..). Aquí asumimos áreas controladas: admin, doc, est, secr
        return area + "/" + page;
    }

    // Opcional: para páginas planas en la raíz como /about.html
    @GetMapping("/{page}.html")
    public String rootPage(@PathVariable("page") String page) {
        return page;
    }
}
