package com.mycompany.sistemainventariov3.dto;

import java.util.ArrayList;
import java.util.List;

public class ActaSoftwareRequest {

    private Funcionario funcionario = new Funcionario();
    private EquipoInfo equipo = new EquipoInfo();
    private List<SoftwareItem> softwareItems = new ArrayList<>();
    private List<String> driversAdicionales = new ArrayList<>();
    private String certificacion;
    private FirmaRecepcion entrega = new FirmaRecepcion();
    private FirmaRecepcion recibe = new FirmaRecepcion();

    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }

    public EquipoInfo getEquipo() { return equipo; }
    public void setEquipo(EquipoInfo equipo) { this.equipo = equipo; }

    public List<SoftwareItem> getSoftwareItems() { return softwareItems; }
    public void setSoftwareItems(List<SoftwareItem> softwareItems) { this.softwareItems = softwareItems; }

    public List<String> getDriversAdicionales() { return driversAdicionales; }
    public void setDriversAdicionales(List<String> driversAdicionales) { this.driversAdicionales = driversAdicionales; }

    public String getCertificacion() { return certificacion; }
    public void setCertificacion(String certificacion) { this.certificacion = certificacion; }

    public FirmaRecepcion getEntrega() { return entrega; }
    public void setEntrega(FirmaRecepcion entrega) { this.entrega = entrega; }

    public FirmaRecepcion getRecibe() { return recibe; }
    public void setRecibe(FirmaRecepcion recibe) { this.recibe = recibe; }

    public static class Funcionario {
        private String nombre;
        private String cargo;
        private String extension;
        private String correo;
        private String area;
        private String edificio;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getCargo() { return cargo; }
        public void setCargo(String cargo) { this.cargo = cargo; }
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
        public String getEdificio() { return edificio; }
        public void setEdificio(String edificio) { this.edificio = edificio; }
    }

    public static class EquipoInfo {
        private String tipo;
        private String marca;
        private String modelo;
        private String serial;
        private String codigo;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getMarca() { return marca; }
        public void setMarca(String marca) { this.marca = marca; }
        public String getModelo() { return modelo; }
        public void setModelo(String modelo) { this.modelo = modelo; }
        public String getSerial() { return serial; }
        public void setSerial(String serial) { this.serial = serial; }
        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
    }

    public static class SoftwareItem {
        private String categoria;
        private String programa;
        private String instalado; // "SI", "NO", or ""

        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getPrograma() { return programa; }
        public void setPrograma(String programa) { this.programa = programa; }
        public String getInstalado() { return instalado; }
        public void setInstalado(String instalado) { this.instalado = instalado; }
    }

    public static class FirmaRecepcion {
        private String nombre;
        private String firma;
        private String fecha;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getFirma() { return firma; }
        public void setFirma(String firma) { this.firma = firma; }
        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }
    }
}
