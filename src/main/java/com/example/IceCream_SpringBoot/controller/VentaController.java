package com.example.IceCream_SpringBoot.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.IceCream_SpringBoot.model.Cliente;
import com.example.IceCream_SpringBoot.model.HeladoDocument;
import com.example.IceCream_SpringBoot.model.VentaDocument;
import com.example.IceCream_SpringBoot.repository.ClienteRepository;
import com.example.IceCream_SpringBoot.repository.VentaRepository;
import com.example.IceCream_SpringBoot.service.HeladoService;
import com.example.IceCream_SpringBoot.service.VentaService;
import com.example.IceCream_SpringBoot.repository.HeladoRepository;

import com.example.IceCream_SpringBoot.service.GenerateFacturaPDF;
import com.example.IceCream_SpringBoot.service.CorreoService;

@Controller
public class VentaController {

    @Autowired
    private HeladoService heladoService;

    @Autowired
    private HeladoRepository heladoRepository;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private GenerateFacturaPDF gnerateFacturapdf;

    @Autowired
    private CorreoService correoService;

    @GetMapping("/venderHelados")
    public String mostrarFormularioVender(Model model) {
        List<HeladoDocument> listaHelados = heladoService.getListaHeladeria();

        // Obtener el atributo "desdeVenta" si fue pasado como flash
        Boolean desdeVenta = (Boolean) model.getAttribute("desdeVenta");

        if (desdeVenta == null || !desdeVenta) {
            List<Map<String, Object>> heladosPocoStock = listaHelados.stream()
                    .filter(h -> h.getUnidades() <= 5)
                    .map(h -> {
                        Map<String, Object> mapa = new HashMap<>();
                        mapa.put("nombre", h.getNombre());
                        mapa.put("Unidades", h.getUnidades());
                        return mapa;
                    })
                    .collect(Collectors.toList());

            model.addAttribute("heladosPocoStock", heladosPocoStock);
        }

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
            @RequestParam String Email,
            @RequestParam String telefono,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
            RedirectAttributes redirectAttributes,
            Principal principal) {

        String vendedor = principal.getName();

        // Validacion basica
        if (nombresHelados == null || unidadesVenderLista == null ||
                nombresHelados.size() != unidadesVenderLista.size() || nombresHelados.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Error en los datos de los helados enviados. Intente de nuevo.");
            return "redirect:/venderHelados";
        }

        List<String> nombresLimpios = new ArrayList<>();
        for (String nombre : nombresHelados) {
            if (nombre != null) {
                nombresLimpios.add(nombre.trim());
            }
        }

        System.out.println("Helados a procesar (limpios): " + nombresLimpios);

        // Procesar la venta
        boolean success = ventaService.procesarVenta(
                nombresHelados,
                unidadesVenderLista,
                metodoPago,
                totalAPagar,
                nombreCliente,
                cedula,
                Email,
                telefono,
                fechaNacimiento.atStartOfDay(),
                vendedor);

        if (success) {
            try {
                // Generar PDF
                byte[] pdfBytes = gnerateFacturapdf.generarFacturaPDF(
                        nombreCliente, nombresHelados, unidadesVenderLista, Email, telefono, metodoPago, totalAPagar);

                // Enviar el correo
                correoService.enviarFactura(
                        Email,
                        "Factura de su compra 🍦",
                        "Gracias por comprar en nuestra heladeria. Adjuntamos su factura.",
                        pdfBytes);

                redirectAttributes.addFlashAttribute("mensaje", "¡Venta realizada con exito!");

                // Verificar el stock de los helados vendidos
                List<Map<String, Object>> heladosPocoStock = new ArrayList<>();

                for (int i = 0; i < nombresHelados.size(); i++) {
                    String nombreHelado = nombresHelados.get(i);
                    int cantidadVendida = unidadesVenderLista.get(i);

                    // Busca el helado en la base de datos
                    HeladoDocument helado = heladoService.obtenerHeladoPorNombreYUbicacion(nombreHelado, "heladeria");
                    if (helado != null) {
                        int nuevoStock = helado.getUnidades() - cantidadVendida;
                        if (nuevoStock < 0) {
                            nuevoStock = 0; // evita que baje de 0
                        }
                        helado.setUnidades(nuevoStock);
                        heladoRepository.save(helado);

                        // Si queda poco stock, lo añadimos a la lista
                        if (nuevoStock <= 5) {
                            Map<String, Object> mapaHelado = new HashMap<>();
                            mapaHelado.put("nombre", helado.getNombre());
                            mapaHelado.put("stock", nuevoStock);
                            heladosPocoStock.add(mapaHelado);

                        }
                    }
                }

                // Enviar la lista de helados con poco stock a la vista
                if (!heladosPocoStock.isEmpty()) {
                    redirectAttributes.addFlashAttribute("heladosPocoStock", heladosPocoStock);
                }

            } catch (Exception e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Error al enviar la factura: " + e.getMessage());
            }
            redirectAttributes.addFlashAttribute("desdeVenta", true);
            return "redirect:/venderHelados";
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Error al procesar la venta. Verifique el stock, los helados seleccionados o el total general.");
            return "redirect:/venderHelados";
        }
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