package com.mycompany.sistemainventariov3.model;

import javax.persistence.*;

/**
 * Entidad Ubicacion del modelo final normalizado.
 */
@Entity
@Table(name = "ubicacion")
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ubicacion")
    private Integer idUbicacion;

    @Column(name = "edificio", length = 100)
    private String edificio;

    @Column(name = "piso", length = 50)
    private String piso;

    @Column(name = "direccion", length = 150)
    private String direccion;

    public Integer getIdUbicacion() {
        return idUbicacion;
    }

    public void setIdUbicacion(Integer idUbicacion) {
        this.idUbicacion = idUbicacion;
    }

    public String getEdificio() {
        return edificio;
    }

    public void setEdificio(String edificio) {
        this.edificio = edificio;
    }

    public String getPiso() {
        return piso;
    }

    public void setPiso(String piso) {
        this.piso = piso;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getClaveUbicacion() {
        return getUbicacionCompleta();
    }

    public void setClaveUbicacion(String claveUbicacion) {
        this.direccion = claveUbicacion;
    }

    public String getDetalle() {
        return direccion;
    }

    public void setDetalle(String detalle) {
        this.direccion = detalle;
    }

    public String getUbicacionCompleta() {
        StringBuilder sb = new StringBuilder();
        appendParte(sb, edificio);
        appendParte(sb, piso);
        appendParte(sb, direccion);
        return sb.toString();
    }

    private void appendParte(StringBuilder sb, String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" - ");
        }
        sb.append(valor.trim());
    }

    @Override
    public String toString() {
        return getUbicacionCompleta();
    }
}
