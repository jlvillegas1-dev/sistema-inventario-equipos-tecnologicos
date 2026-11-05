package com.mycompany.sistemainventariov3.dto;

/**
 * DTO normalizado para exponer el inventario completo al frontend.
 */
public class InventoryItemDTO {

    private Integer id;
    private String tipo;
    private String subtipo;
    private String codigoSbai;
    private String codigoMegan;
    private String descripcion;
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String custodio;
    private String ubicacion;
    private String ubicacionEdificio;
    private String ubicacionPiso;
    private String ubicacionDireccion;
    private String estado;
    private String procesador;
    private String ram;
    private String discoDuro;
    private String sistemaOperativo;
    private String observacion;
    private String caracteristicas;
    private String ip;
    private String fechaIngreso;
    private String ultimaActualizacion;
    private String ultimoMantenimiento;
    private Integer idComputador;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getSubtipo() {
        return subtipo;
    }

    public void setSubtipo(String subtipo) {
        this.subtipo = subtipo;
    }

    public String getCodigoSbai() {
        return codigoSbai;
    }

    public void setCodigoSbai(String codigoSbai) {
        this.codigoSbai = codigoSbai;
    }

    public String getCodigoMegan() {
        return codigoMegan;
    }

    public void setCodigoMegan(String codigoMegan) {
        this.codigoMegan = codigoMegan;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public String getCustodio() {
        return custodio;
    }

    public void setCustodio(String custodio) {
        this.custodio = custodio;
    }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getUbicacionEdificio() { return ubicacionEdificio; }
    public void setUbicacionEdificio(String ubicacionEdificio) { this.ubicacionEdificio = ubicacionEdificio; }

    public String getUbicacionPiso() { return ubicacionPiso; }
    public void setUbicacionPiso(String ubicacionPiso) { this.ubicacionPiso = ubicacionPiso; }

    public String getUbicacionDireccion() { return ubicacionDireccion; }
    public void setUbicacionDireccion(String ubicacionDireccion) { this.ubicacionDireccion = ubicacionDireccion; }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getProcesador() {
        return procesador;
    }

    public void setProcesador(String procesador) {
        this.procesador = procesador;
    }

    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram;
    }

    public String getDiscoDuro() {
        return discoDuro;
    }

    public void setDiscoDuro(String discoDuro) {
        this.discoDuro = discoDuro;
    }

    public String getSistemaOperativo() {
        return sistemaOperativo;
    }

    public void setSistemaOperativo(String sistemaOperativo) {
        this.sistemaOperativo = sistemaOperativo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getCaracteristicas() {
        return caracteristicas;
    }

    public void setCaracteristicas(String caracteristicas) {
        this.caracteristicas = caracteristicas;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(String fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public String getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(String ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    public String getUltimoMantenimiento() { return ultimoMantenimiento; }
    public void setUltimoMantenimiento(String ultimoMantenimiento) { this.ultimoMantenimiento = ultimoMantenimiento; }

    public Integer getIdComputador() { return idComputador; }
    public void setIdComputador(Integer idComputador) { this.idComputador = idComputador; }
}
