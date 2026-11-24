package com.example.almacen.controlador;

import com.example.almacen.Repository.UsuarioRepository;
import com.example.almacen.formulario.CambiarPasswordForm;
import com.example.almacen.modelo.Usuario;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/perfil")
    public String verPerfil(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + authentication.getName()));

        model.addAttribute("usuario", usuario);
        model.addAttribute("form", new CambiarPasswordForm());
        return "perfil";
    }

    @PostMapping("/perfil/password")
    public String cambiarPassword(Authentication authentication,
                                  @Valid @ModelAttribute("form") CambiarPasswordForm form,
                                  BindingResult errores,
                                  Model model,
                                  RedirectAttributes mensajes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + authentication.getName()));

        // Validar que la contraseña actual sea correcta
        if (form.getPasswordActual() != null && !passwordEncoder.matches(form.getPasswordActual(), usuario.getPasswordHash())) {
            errores.rejectValue("passwordActual", "incorrecta", "Tu contraseña actual no es correcta.");
        }

        // Validar que la nueva contraseña coincida con la confirmación
        if (form.getNuevoPassword() != null && !form.getNuevoPassword().equals(form.getConfirmarNuevoPassword())) {
            errores.rejectValue("confirmarNuevoPassword", "noCoincide", "Las nuevas contraseñas no coinciden.");
        }

        if (errores.hasErrors()) {
            model.addAttribute("usuario", usuario);
            return "perfil";
        }

        // Encriptar y guardar la nueva contraseña
        usuario.setPasswordHash(passwordEncoder.encode(form.getNuevoPassword()));
        usuarioRepository.save(usuario);

        mensajes.addFlashAttribute("exito", "¡Contraseña actualizada correctamente!");
        return "redirect:/perfil";
    }
}
