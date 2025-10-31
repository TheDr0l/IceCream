package com.example.IceCream_SpringBoot.service;

import com.example.IceCream_SpringBoot.model.HeladoDocument;
import com.example.IceCream_SpringBoot.repository.HeladoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HeladoService {

    @Autowired
    private HeladoRepository heladoRepository;

    public boolean heladoExiste(String nombre) {
        return heladoRepository.existsByNombre(nombre);
    }

    public boolean agregarAlAlmacen(String nombre, String sabor, String tipo, double precio, int unidades) {
        if (heladoExiste(nombre)) {
            return false;
        }
        HeladoDocument helado = new HeladoDocument(nombre, sabor, tipo, precio, unidades, "almacen");
        heladoRepository.save(helado);
        return true;
    }

    public boolean moverHeladoAHeladeria(String nombre, int unidadesMover) {
        Optional<HeladoDocument> optionalAlmacen = heladoRepository.findByUbicacion("almacen")
            .stream()
            .filter(h -> h.getNombre().equals(nombre))
            .findFirst();

        if (optionalAlmacen.isEmpty() || optionalAlmacen.get().getUnidades() < unidadesMover) {
            return false;
        }

        // Quitar unidades del almacén
        HeladoDocument heladoAlmacen = optionalAlmacen.get();
        heladoAlmacen.setUnidades(heladoAlmacen.getUnidades() - unidadesMover);
        heladoRepository.save(heladoAlmacen);

        // Sumar o crear en heladería
        Optional<HeladoDocument> optionalHeladeria = heladoRepository.findByUbicacion("heladeria")
            .stream()
            .filter(h -> h.getNombre().equals(nombre))
            .findFirst();

        if (optionalHeladeria.isPresent()) {
            HeladoDocument heladoHeladeria = optionalHeladeria.get();
            heladoHeladeria.setUnidades(heladoHeladeria.getUnidades() + unidadesMover);
            heladoRepository.save(heladoHeladeria);
        } else {
            HeladoDocument nuevo = new HeladoDocument(
                nombre,
                heladoAlmacen.getSabor(),
                heladoAlmacen.getTipo(),
                heladoAlmacen.getPrecio(),
                unidadesMover,
                "heladeria"
            );
            heladoRepository.save(nuevo);
        }

        return true;
    }

    public HeladoDocument obtenerHeladoPorNombreYUbicacion(String nombre, String ubicacion) {
        return heladoRepository.findByUbicacion(ubicacion)
                .stream()
                .filter(h -> h.getNombre().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    public boolean editarHelado(String ubicacion,
                                String nombreOriginal,
                                String nombreNuevo,
                                String sabor,
                                String tipo,
                                int unidades,
                                double precio) {
        Optional<HeladoDocument> optionalHelado = heladoRepository.findByUbicacion(ubicacion)
            .stream()
            .filter(h -> h.getNombre().equals(nombreOriginal))
            .findFirst();

        if (optionalHelado.isPresent()) {
            HeladoDocument helado = optionalHelado.get();
            helado.setUnidades(unidades);
            helado.setPrecio(precio);
            heladoRepository.save(helado);

            // actualizar nombre, sabor y tipo en todas las ubicaciones
            actualizarEnTodasUbicaciones(nombreOriginal, nombreNuevo, sabor, tipo);
            return true;
        }

        return false;
    }

    private void actualizarEnTodasUbicaciones(String nombreOriginal,
                                              String nombreNuevo,
                                              String sabor,
                                              String tipo) {
        List<HeladoDocument> helados = heladoRepository.findByNombreIgnoreCase(nombreOriginal);
        if (!helados.isEmpty()) {
            for (HeladoDocument h : helados) {
                h.setNombre(nombreNuevo);
                h.setSabor(sabor);
                h.setTipo(tipo);
            }
            heladoRepository.saveAll(helados);
        }
    }

    public boolean eliminarHelado(String ubicacion, String nombre) {
        Optional<HeladoDocument> optionalHelado = heladoRepository.findByUbicacion(ubicacion)
            .stream()
            .filter(h -> h.getNombre().equals(nombre))
            .findFirst();

        if (optionalHelado.isPresent()) {
            heladoRepository.delete(optionalHelado.get());
            return true;
        }
        return false;
    }

    // Listados de inventario
    public List<HeladoDocument> getListaAlmacen() {
        return heladoRepository.findByUbicacion("almacen");
    }

    public List<HeladoDocument> getListaHeladeria() {
        return heladoRepository.findByUbicacion("heladeria");
    }
}
