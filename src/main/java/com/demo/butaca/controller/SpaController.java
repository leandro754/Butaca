package com.demo.butaca.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/buscar",
        "/eventos/**",
        "/checkout",
        "/confirmacion",
        "/login",
        "/registro",
        "/dashboard",
        "/dashboard/**"
    })
    public String forwardClientRoutes() {
        return "forward:/index.html";
    }
}
