package com.mycompany.sistemainventariov3.model;

import javax.persistence.*;

/**
 * Entidad Custodio del modelo final normalizado.
 */
@Entity
@Table(name = "custodio")
public class Custodio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_custodio")
    private Integer idCustodio;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public Integer getIdCustodio() {
        return idCustodio;
    }

    public void setIdCustodio(Integer idCustodio) {
        this.idCustodio = idCustodio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
