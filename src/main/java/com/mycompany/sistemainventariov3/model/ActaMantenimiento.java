package com.mycompany.sistemainventariov3.model;

import java.sql.Timestamp;

public class ActaMantenimiento {
    private Integer idActa;
    private String tipoActa;
    private String codigoEquipo;
    private String nombreCustodio;
    private Integer idCustodio;
    private String creadoPor;
    private Timestamp fechaCreacion;
    private String jsonPayload;

    public ActaMantenimiento() {
    }

    public Integer getIdActa() {
        return idActa;
    }

    public void setIdActa(Integer idActa) {
        this.idActa = idActa;
    }

    public String getTipoActa() {
        return tipoActa;
    }

    public void setTipoActa(String tipoActa) {
        this.tipoActa = tipoActa;
    }

    public String getCodigoEquipo() {
        return codigoEquipo;
    }

    public void setCodigoEquipo(String codigoEquipo) {
        this.codigoEquipo = codigoEquipo;
    }

    public String getNombreCustodio() {
        return nombreCustodio;
    }

    public void setNombreCustodio(String nombreCustodio) {
        this.nombreCustodio = nombreCustodio;
    }

    public Integer getIdCustodio() {
        return idCustodio;
    }

    public void setIdCustodio(Integer idCustodio) {
        this.idCustodio = idCustodio;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getJsonPayload() {
        return jsonPayload;
    }

    public void setJsonPayload(String jsonPayload) {
        this.jsonPayload = jsonPayload;
    }
}
