package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.entity.TipoArma;
import com.faculdade.estandetiro.model.service.ArmaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD das armas do minigame. */
@Controller
@RequestMapping("/armas")
public class ArmaController {

    private final ArmaService armaService;

    public ArmaController(ArmaService armaService) {
        this.armaService = armaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("armas", armaService.listarTodas());
        return "armas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("form", new ArmaForm());
        return formulario(model, null);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") ArmaForm form, BindingResult validacao, Model model,
                        RedirectAttributes ra) {
        if (validacao.hasErrors()) {
            return formulario(model, null);
        }
        Arma criada = armaService.criar(form.paraEntidade());
        ra.addFlashAttribute("sucesso", "Arma \"" + criada.getNome() + "\" cadastrada.");
        return "redirect:/armas";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Arma arma = armaService.buscarPorId(id);
        model.addAttribute("form", ArmaForm.de(arma));
        return formulario(model, arma);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ArmaForm form,
                            BindingResult validacao, Model model, RedirectAttributes ra) {
        if (validacao.hasErrors()) {
            return formulario(model, armaService.buscarPorId(id));
        }
        armaService.atualizar(id, form.paraEntidade());
        ra.addFlashAttribute("sucesso", "Arma atualizada.");
        return "redirect:/armas";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        String nome = armaService.buscarPorId(id).getNome();
        armaService.excluir(id);
        ra.addFlashAttribute("sucesso", "Arma \"" + nome + "\" excluída.");
        return "redirect:/armas";
    }

    private String formulario(Model model, Arma arma) {
        model.addAttribute("arma", arma);
        model.addAttribute("tipos", TipoArma.values());
        return "armas/form";
    }
}
