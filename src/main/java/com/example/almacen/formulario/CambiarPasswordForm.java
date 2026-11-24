package com.example.almacen.formulario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CambiarPasswordForm {

    @NotBlank(message = "Ingresa tu contraseña actual.")
    private String passwordActual;

    @NotBlank(message = "Ingresa tu nueva contraseña.")
    @Size(min = 6, max = 72, message = "La nueva contraseña debe tener al menos 6 caracteres.")
    private String nuevoPassword;

    @NotBlank(message = "Confirma tu nueva contraseña.")
    private String confirmarNuevoPassword;

    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }
    public String getNuevoPassword() { return nuevoPassword; }
    public void setNuevoPassword(String nuevoPassword) { this.nuevoPassword = nuevoPassword; }
    public String getConfirmarNuevoPassword() { return confirmarNuevoPassword; }
    public void setConfirmarNuevoPassword(String confirmarNuevoPassword) { this.confirmarNuevoPassword = confirmarNuevoPassword; }
}
