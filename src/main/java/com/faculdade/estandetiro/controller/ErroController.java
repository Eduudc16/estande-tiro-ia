package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.service.RecursoNaoEncontradoException;
import com.faculdade.estandetiro.model.service.RegraNegocioException;
import com.faculdade.estandetiro.model.service.ia.IaException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Mensagens amigáveis para erros que escapam dos controllers (usa a página templates/error.html). */
@ControllerAdvice
public class ErroController {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(RecursoNaoEncontradoException e, Model model) {
        return pagina(model, 404, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String parametroInvalido(Model model) {
        return pagina(model, 404, "Endereço inválido.");
    }

    @ExceptionHandler({RegraNegocioException.class, IaException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String regra(RuntimeException e, Model model) {
        return pagina(model, 400, e.getMessage());
    }

    private static String pagina(Model model, int status, String mensagem) {
        model.addAttribute("status", status);
        model.addAttribute("mensagem", mensagem);
        return "error";
    }
}
