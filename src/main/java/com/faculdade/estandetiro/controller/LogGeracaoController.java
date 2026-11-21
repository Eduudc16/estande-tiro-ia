package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.service.LogGeracaoImagemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tela de log das gerações de imagem (acompanha o gasto da chave faturada). */
@Controller
public class LogGeracaoController {

    private final LogGeracaoImagemService logService;

    public LogGeracaoController(LogGeracaoImagemService logService) {
        this.logService = logService;
    }

    @GetMapping("/logs")
    public String listar(Model model) {
        model.addAttribute("logs", logService.listarTodos());
        model.addAttribute("resumo", logService.resumo());
        return "logs/lista";
    }
}
