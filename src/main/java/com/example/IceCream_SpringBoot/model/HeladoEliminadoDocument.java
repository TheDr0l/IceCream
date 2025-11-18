package com.example.IceCream_SpringBoot.model;

import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Document(collection = "heladosEliminados")
public class HeladoEliminadoDocument {

    @Id
    private String id;
    private String nombre;
    private String sabor;
    private String tipo;
    private double precio;
    private int unidades;
    private String ubicacion;
    private LocalDateTime fechaEliminacion;

    public HeladoEliminadoDocument() {
    }

    public HeladoEliminadoDocument(HeladoDocument helado) {
        this.id = helado.getId();
        this.nombre = helado.getNombre();
        this.sabor = helado.getSabor();
        this.tipo = helado.getTipo();
        this.precio = helado.getPrecio();
        this.unidades = helado.getUnidades();
        this.ubicacion = helado.getUbicacion();
        this.fechaEliminacion = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getUnidades() {
        return unidades;
    }

    public void setUnidades(int unidades) {
        this.unidades = unidades;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public LocalDateTime getFechaEliminacion() {
        return fechaEliminacion;
    }

    public void setFechaEliminacion(LocalDateTime fechaEliminacion) {
        this.fechaEliminacion = fechaEliminacion;
    }
}
