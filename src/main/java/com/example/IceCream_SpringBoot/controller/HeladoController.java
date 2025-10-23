package com.example.IceCream_SpringBoot.controller;

import com.example.IceCream_SpringBoot.model.HeladoDocument;
import com.example.IceCream_SpringBoot.service.HeladoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class HeladoController {

    @Autowired
    private HeladoService heladoService;

    // 1) Agregar al almacén
    @GetMapping("/agregarAlAlmacen")
    public String agregarAlAlmacen(Model model) {
        List<HeladoDocument> helados = heladoService.getListaAlmacen();
        int totalHelados = helados.size();
        int totalUnidades = helados.stream().mapToInt(HeladoDocument::getUnidades).sum();

        model.addAttribute("totalHelados", totalHelados);
        model.addAttribute("totalUnidades", totalUnidades);
        return "agregarAlAlmacen";
    }

    @PostMapping("/agregarAlAlmacen")
    public String registerHelado(@RequestParam String nombre,
            @RequestParam String sabor,
            @RequestParam String tipo,
            @RequestParam double precio,
            @RequestParam int unidades,
            RedirectAttributes redirectAttributes) {

        if (heladoService.heladoExiste(nombre)) {
            redirectAttributes.addFlashAttribute("error", "El nombre del helado ya existe.");
            return "redirect:/agregarAlAlmacen";
        }

        boolean success = heladoService.agregarAlAlmacen(nombre, sabor, tipo, precio, unidades);
        if (success) {
            redirectAttributes.addFlashAttribute("mensaje", "Helado agregado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Error al registrar el helado. Inténtalo de nuevo.");
        }

        return "redirect:/agregarAlAlmacen";
    }

    // 2) Mover helado del almacén a la heladería
    @GetMapping("/moverHelado")
    public String mostrarFormularioMoverHelado(Model model) {
        model.addAttribute("helados", heladoService.getListaAlmacen());
        return "MoverAheladeria";
    }

    @PostMapping("/moverHelado")
    public String moverHelado(@RequestParam String nombreHelado,
            @RequestParam int unidadesMover,
            RedirectAttributes redirectAttributes) {

        boolean success = heladoService.moverHeladoAHeladeria(nombreHelado, unidadesMover);

        if (success) {
            redirectAttributes.addFlashAttribute("mensaje", "Helado movido correctamente a la heladería.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Error al mover el helado. Verifica las unidades disponibles.");
        }

        return "redirect:/moverHelado";
    }

    // 3) Listados de inventario
    @GetMapping("/heladosAlmacen")
    public String mostrarHeladosAlmacen(Model model) {
        model.addAttribute("helados", heladoService.getListaAlmacen());
        return "listaHelados";
    }

    @GetMapping("/heladosHeladeria")
    public String mostrarHeladosHeladeria(Model model) {
        model.addAttribute("helados", heladoService.getListaHeladeria());
        return "listaHelados";
    }

    // 4) Endpoints AJAX
    @PostMapping("/obtenerHeladosPorUbicacion")
    @ResponseBody
    public List<HeladoDocument> obtenerHeladosPorUbicacion(@RequestParam String ubicacion) {
        if ("almacen".equals(ubicacion)) {
            return heladoService.getListaAlmacen();
        } else if ("heladeria".equals(ubicacion)) {
            return heladoService.getListaHeladeria();
        }
        return List.of();
    }

    @PostMapping("/obtenerDatosHelado")
    @ResponseBody
    public HeladoDocument obtenerDatosHelado(@RequestParam String nombreHelado,
            @RequestParam String ubicacion) {
        return heladoService.obtenerHeladoPorNombreYUbicacion(nombreHelado, ubicacion);
    }

    // 5) Editar helado
    @GetMapping("/editarHelado")
    public String mostrarFormularioEditar() {
        return "EditarHelado";
    }

    @PostMapping("/editarHelado")
    public String editarHelado(@RequestParam String ubicacion,
            @RequestParam String nombreOriginal,
            @RequestParam String nombreNuevo,
            @RequestParam String sabor,
            @RequestParam String tipo,
            @RequestParam int unidades,
            @RequestParam double precio,
            RedirectAttributes redirectAttributes) {

        boolean success = heladoService.editarHelado(
                ubicacion, nombreOriginal, nombreNuevo, sabor, tipo, unidades, precio);

        if (success) {
            redirectAttributes.addFlashAttribute("mensaje", "Helado editado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Error al editar el helado.");
        }

        return "redirect:/editarHelado";
    }

    // 6) Eliminar helado
    @GetMapping("/eliminarHelado")
    public String mostrarEliminarHelado() {
        return "EliminarHelado";
    }

    @PostMapping("/eliminarHelado")
    public String eliminarHelado(@RequestParam String ubicacion,
            @RequestParam String nombre,
            RedirectAttributes redirectAttributes) {

        boolean success = heladoService.eliminarHelado(ubicacion, nombre);

        if (success) {
            redirectAttributes.addFlashAttribute("mensaje", "El helado \"" + nombre + "\" fue eliminado exitosamente.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo eliminar el helado \"" + nombre + "\". Intenta nuevamente.");
        }

        return "redirect:/eliminarHelado";
    }

    // 7) Manejo de parámetros faltantes
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleMissingParams(MissingServletRequestParameterException ex,
            HttpServletRequest request,
            Model model) {
        model.addAttribute("error", "Faltan campos requeridos");
        String uri = request.getRequestURI();
        if (uri.contains("/editarHelado"))
            return "EditarHelado";
        if (uri.contains("/eliminarHelado"))
            return "EliminarHelado";
        if (uri.contains("/moverHelado"))
            return "MoverAheladeria";
        return "ErrorGeneral";
    }

    // 8) Reportes
    @GetMapping("/reporte")
    public String verReporte() {
        return "reporte";
    }

    @GetMapping("/reporteOpciones")
    public String verReporteOpciones() {
        return "reporteOpciones";
    }
}
