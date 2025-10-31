package com.example.IceCream_SpringBoot.model;

import java.time.LocalDate;

public class Cliente {
    
    private String nombre;
    private String cedula;
    private String Email;
    private String telefono;
    private LocalDate fechaNacimiento;

    public Cliente() {}

    public Cliente(String nombre, String cedula, String Email, String telefono, LocalDate fechaNacimiento) {
        this.nombre = nombre;
        this.cedula = cedula;
        this.Email = Email;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String Email) {
        this.Email = Email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

}

