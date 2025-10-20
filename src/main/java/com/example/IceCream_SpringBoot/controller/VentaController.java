package com.example.IceCream_SpringBoot.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.IceCream_SpringBoot.model.Cliente;
import com.example.IceCream_SpringBoot.model.HeladoDocument;
import com.example.IceCream_SpringBoot.model.VentaDocument;
import com.example.IceCream_SpringBoot.repository.ClienteRepository;
import com.example.IceCream_SpringBoot.repository.VentaRepository;
import com.example.IceCream_SpringBoot.service.HeladoService;
import com.example.IceCream_SpringBoot.service.VentaService;

@Controller
public class VentaController {

    @Autowired
    private HeladoService heladoService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @GetMapping("/venderHelados")
    public String mostrarFormularioVender(Model model) {
        List<HeladoDocument> listaHelados = heladoService.getListaHeladeria();
        model.addAttribute("helados", listaHelados);
        return "VenderHelados";
    }

    @PostMapping("/venderHelados")
    public String procesarVenta(
            @RequestParam("nombresHelados") List<String> nombresHelados,
            @RequestParam("unidadesVenderLista") List<Integer> unidadesVenderLista,
            @RequestParam String metodoPago,
            @RequestParam double totalAPagar,
            @RequestParam String nombreCliente,
            @RequestParam String cedula,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
            @RequestParam String telefono,
            Model model,
            Principal principal) {

        String vendedor = principal.getName();

        // Validacion basica en el controlador
        if (nombresHelados == null || unidadesVenderLista == null || nombresHelados.size() != unidadesVenderLista.size()
                || nombresHelados.isEmpty()) {
            model.addAttribute("error", "Error en los datos de los helados enviados. Intente de nuevo.");
            List<HeladoDocument> listaHelados = heladoService.getListaHeladeria();
            model.addAttribute("helados", listaHelados);
            return "VenderHelados";
        }

        boolean success = ventaService.procesarVenta(
                nombresHelados,
                unidadesVenderLista,
                metodoPago,
                totalAPagar,
                nombreCliente,
                cedula,
                fechaNacimiento.atStartOfDay(),
                telefono,
                vendedor);

        if (success) {
            model.addAttribute("mensaje", "¡Venta realizada con éxito!");
        } else {
            model.addAttribute("error",
                    "Error al procesar la venta. Verifique el stock, los helados seleccionados o el total general.");
        }

        List<HeladoDocument> listaHeladosActualizada = heladoService.getListaHeladeria();
        model.addAttribute("helados", listaHeladosActualizada);

        return "VenderHelados";
    }

    @GetMapping("/buscarCliente")
    @ResponseBody
    public ResponseEntity<?> buscarCliente(@RequestParam String cedula) {
        Cliente cliente = clienteRepository.findByCedula(cedula);
        if (cliente != null) {
            return ResponseEntity.ok(cliente);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Cliente no encontrado"));
        }
    }

    @GetMapping("/registroVentas")
    public String mostrarVentas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Model model) {

        int pageSize = 10;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("fechaVenta").descending());
        Page<VentaDocument> ventasPage;

        if (fechaDesde != null && fechaHasta != null) {
            ventasPage = ventaRepository.findByFechaVentaBetween(
                    fechaDesde.atStartOfDay(),
                    fechaHasta.atTime(23, 59, 59),
                    pageable);
        } else {
            ventasPage = ventaRepository.findAllByOrderByFechaVentaDesc(pageable);
        }

        model.addAttribute("ventasPage", ventasPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ventasPage.getTotalPages());

        return "ventas";
    }

}