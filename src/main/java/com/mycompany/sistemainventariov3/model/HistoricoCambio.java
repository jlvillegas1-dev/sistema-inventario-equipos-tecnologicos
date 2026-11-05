package com.mycompany.sistemainventariov3.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Entidad para registrar histórico de cambios en bienes
 */
@Entity
@Table(name = "historico_cambios")
public class HistoricoCambio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cambio")
    private Integer idCambio;
    
    @Column(name = "tabla_afectada", nullable = false, length = 100)
    private String tablaAfectada; // pcs, laptops, perifericos, etc.
    
    @Column(name = "id_bien", nullable = false)
    private String idBien; // codigo_sbai del bien
    
    @Column(name = "campo", nullable = false, length = 100)
    private String campo; // Nombre del campo modificado
    
    @Column(name = "valor_anterior", columnDefinition = "LONGTEXT")
    private String valorAnterior;
    
    @Column(name = "valor_nuevo", columnDefinition = "LONGTEXT")
    private String valorNuevo;
    
    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario; // Username del usuario que realizó el cambio
    
    @Column(name = "fecha_cambio", nullable = false)
    private Date fechaCambio;
    
    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo; // Razón del cambio (obligatorio para ciertos cambios)
    
    @Column(name = "tipo_operacion", nullable = false, length = 20)
    private String tipoOperacion; // INSERT, UPDATE, DELETE

    // Getters y Setters
    public Integer getIdCambio() {
        return idCambio;
    }

    public void setIdCambio(Integer idCambio) {
        this.idCambio = idCambio;
    }

    public String getTablaAfectada() {
        return tablaAfectada;
    }

    public void setTablaAfectada(String tablaAfectada) {
        this.tablaAfectada = tablaAfectada;
    }

    public String getIdBien() {
        return idBien;
    }

    public void setIdBien(String idBien) {
        this.idBien = idBien;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(String valorNuevo) {
        this.valorNuevo = valorNuevo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Date getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(Date fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }
}
