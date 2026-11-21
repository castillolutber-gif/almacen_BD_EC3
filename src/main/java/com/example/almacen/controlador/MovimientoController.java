package com.example.almacen.controlador;

import com.example.almacen.Repository.MovimientoRepository;
import com.example.almacen.Repository.ProductoRepository;
import com.example.almacen.Repository.ProveedorRepository;
import com.example.almacen.formulario.DetalleMovimientoForm;
import com.example.almacen.formulario.MovimientoForm;
import com.example.almacen.modelo.Movimiento;
import com.example.almacen.modelo.TipoMovimiento;
import com.example.almacen.servicio.MovimientoServicio;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/movimientos")
public class MovimientoController {

    private final MovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoServicio movimientoServicio;

    public MovimientoController(MovimientoRepository movimientoRepository,
                                ProductoRepository productoRepository,
                                ProveedorRepository proveedorRepository,
                                MovimientoServicio movimientoServicio) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.movimientoServicio = movimientoServicio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("movimientos", movimientoRepository.findAllByOrderByFechaDesc());
        return "movimientos/lista";
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Integer id, Model model) {
        Movimiento movimiento = movimientoRepository.findWithDetallesById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado: " + id));
        model.addAttribute("movimiento", movimiento);
        return "movimientos/detalle";
    }

    @GetMapping("/{id}/detalles-json")
    @ResponseBody
    public ResponseEntity<?> verDetallesJson(@PathVariable Integer id) {
        return movimientoRepository.findWithDetallesById(id)
                .map(mov -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", mov.getId());
                    data.put("tipo", mov.getTipo().name());
                    data.put("fecha", mov.getFecha() != null ? mov.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
                    data.put("usuario", mov.getUsuario() != null ? mov.getUsuario().getNombre() : "—");
                    data.put("proveedor", mov.getProveedor() != null ? mov.getProveedor().getRazonSocial() : "—");
                    data.put("observacion", mov.getObservacion() != null ? mov.getObservacion() : "");
                    data.put("total", String.format(java.util.Locale.US, "%.2f", mov.getTotal()));
                    data.put("totalUnidades", mov.getTotalUnidades());

                    List<Map<String, Object>> listaDetalles = mov.getDetalles().stream().map(d -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("codigo", d.getProducto().getCodigo());
                        item.put("nombre", d.getProducto().getNombre());
                        item.put("cantidad", d.getCantidad());
                        item.put("costoUnitario", String.format(java.util.Locale.US, "%.2f", d.getCostoUnitario()));
                        item.put("subtotal", String.format(java.util.Locale.US, "%.2f", d.getSubtotal()));
                        return item;
                    }).toList();

                    data.put("detalles", listaDetalles);
                    return ResponseEntity.ok(data);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new MovimientoForm());
        cargarOpciones(model);
        return "movimientos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("form") MovimientoForm form,
                          BindingResult errores, Model model,
                          org.springframework.security.core.Authentication autenticacion,
                          RedirectAttributes mensajes) {
        List<DetalleMovimientoForm> detalles = validarDetalles(form, errores);
        if (form.getTipo() == null) {
            errores.rejectValue("tipo", "requerido", "Selecciona entrada o salida.");
        }
        if (form.getTipo() == TipoMovimiento.ENTRADA && form.getProveedorId() == null) {
            errores.rejectValue("proveedorId", "requerido", "Selecciona el proveedor de la entrada.");
        }

        if (errores.hasErrors()) {
            cargarOpciones(model);
            return "movimientos/formulario";
        }

        try {
            movimientoServicio.registrar(autenticacion.getName(), form.getTipo(), form.getProveedorId(),
                    form.getObservacion(), detalles);
        } catch (RuntimeException ex) {
            errores.reject("movimiento", "No se registró el movimiento. Revisa el stock disponible y los datos ingresados.");
            cargarOpciones(model);
            return "movimientos/formulario";
        }

        mensajes.addFlashAttribute("exito", "Movimiento registrado y stock actualizado.");
        return "redirect:/movimientos";
    }

    private List<DetalleMovimientoForm> validarDetalles(MovimientoForm form, BindingResult errores) {
        List<DetalleMovimientoForm> validos = new ArrayList<>();
        List<DetalleMovimientoForm> detalles = form.getDetalles() == null ? List.of() : form.getDetalles();
        for (int i = 0; i < detalles.size(); i++) {
            DetalleMovimientoForm detalle = detalles.get(i);
            if (detalle == null || detalle.estaVacio()) {
                continue;
            }
            boolean filaValida = true;
            if (detalle.getProductoId() == null) {
                errores.rejectValue("detalles[" + i + "].productoId", "requerido", "Selecciona un producto.");
                filaValida = false;
            }
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                errores.rejectValue("detalles[" + i + "].cantidad", "requerido", "La cantidad debe ser mayor que cero.");
                filaValida = false;
            }
            if (detalle.getCostoUnitario() == null || detalle.getCostoUnitario().signum() < 0) {
                errores.rejectValue("detalles[" + i + "].costoUnitario", "requerido", "Indica un costo igual o mayor que cero.");
                filaValida = false;
            }
            if (filaValida) {
                validos.add(detalle);
            }
        }
        if (validos.isEmpty()) {
            errores.reject("movimiento.detalles", "Agrega al menos un producto al movimiento.");
        }
        return validos;
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("productos", productoRepository.findByEstadoTrueOrderByNombreAsc());
        model.addAttribute("proveedores", proveedorRepository.findAllByOrderByRazonSocialAsc());
        model.addAttribute("tiposMovimiento", TipoMovimiento.values());
    }
}
