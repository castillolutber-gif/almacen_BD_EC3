package com.example.almacen.controlador;

import com.example.almacen.Repository.UsuarioRepository;
import com.example.almacen.formulario.RegistroUsuarioForm;
import com.example.almacen.modelo.RolUsuario;
import com.example.almacen.modelo.Usuario;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistroController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/registro")
    public String mostrarFormulario(Model model) {
        model.addAttribute("form", new RegistroUsuarioForm());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("form") RegistroUsuarioForm form,
                                   BindingResult errores,
                                   RedirectAttributes mensajes) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmarPassword())) {
            errores.rejectValue("confirmarPassword", "noCoincide", "Las contraseñas no coinciden.");
        }
        if (form.getCorreo() != null && usuarioRepository.existsByCorreoIgnoreCase(form.getCorreo().trim())) {
            errores.rejectValue("correo", "duplicado", "Ese correo ya se encuentra registrado.");
        }

        if (errores.hasErrors()) {
            return "registro";
        }

        // Se crea el usuario con estado activo y rol ADMIN para que tenga acceso completo
        Usuario nuevoUsuario = new Usuario(
                form.getNombre().trim(),
                form.getCorreo().trim(),
                passwordEncoder.encode(form.getPassword()),
                RolUsuario.ADMIN
        );
        usuarioRepository.save(nuevoUsuario);

        mensajes.addFlashAttribute("cuentaCreada", true);
        return "redirect:/login";
    }
}
