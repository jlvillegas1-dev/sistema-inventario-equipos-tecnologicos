package com.mycompany.sistemainventariov3.model;

import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Date;

/**
 * Entidad Impresora sobre el modelo final equipo + impresora.
 */
@Entity
@Table(name = "equipo")
@SecondaryTable(name = "impresora", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_equipo", referencedColumnName = "id_equipo"))
@Where(clause = "tipo_equipo = 'impresora'")
public class Impresora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Integer idEquipo;

    @Column(name = "tipo_equipo", nullable = false)
    private String tipoEquipo = "impresora";

    @Column(name = "codigo_sbye", length = 120)
    private String codigoSbaiOriginal;

    @Transient
    private Integer item;

    @Column(name = "codigo_megan", length = 120)
    private String codigoMegan;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "marca", length = 120)
    private String marca;

    @Column(name = "modelo", length = 200)
    private String modelo;

    @Column(name = "sn", length = 150)
    private String numeroSerie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_custodio_actual")
    private Custodio custodioActual;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ubicacion")
    private Ubicacion ubicacion;

    @Column(name = "ultima_actualizacion")
    @Temporal(TemporalType.DATE)
    private Date ultimaActualizacion;

    @Column(name = "tipo_impresora", table = "impresora", length = 100)
    private String tipoImpresora;

    @Column(name = "caracteristicas", table = "impresora", length = 255)
    private String caracteristicas;

    @Column(name = "estado", length = 100)
    private String estado = "OPERATIVO";

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "ip", table = "impresora", length = 45)
    private String ip;

    @Transient
    private Integer filaExcel;

    public Impresora() {
    }

    public Impresora(String codigoSbaiOriginal, Integer item) {
        this.codigoSbaiOriginal = codigoSbaiOriginal;
        this.item = item;
    }

    public Integer getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Integer idEquipo) {
        this.idEquipo = idEquipo;
    }

    public String getTipoEquipo() {
        return tipoEquipo;
    }

    public void setTipoEquipo(String tipoEquipo) {
        this.tipoEquipo = tipoEquipo;
    }

    public String getCodigoSbaiOriginal() {
        return codigoSbaiOriginal;
    }

    public void setCodigoSbaiOriginal(String codigoSbaiOriginal) {
        this.codigoSbaiOriginal = codigoSbaiOriginal;
    }

    public Integer getItem() {
        return item;
    }

    public void setItem(Integer item) {
        this.item = item;
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

    public Custodio getCustodioActual() {
        return custodioActual;
    }

    public void setCustodioActual(Custodio custodioActual) {
        this.custodioActual = custodioActual;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }

    public Date getUltimaActualizacion() {
        return ultimaActualizacion;
    }

    public void setUltimaActualizacion(Date ultimaActualizacion) {
        this.ultimaActualizacion = ultimaActualizacion;
    }

    public String getTipoImpresora() {
        return tipoImpresora;
    }

    public void setTipoImpresora(String tipoImpresora) {
        this.tipoImpresora = tipoImpresora;
    }

    public String getCaracteristicas() {
        return caracteristicas;
    }

    public void setCaracteristicas(String caracteristicas) {
        this.caracteristicas = caracteristicas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getFilaExcel() {
        return filaExcel;
    }

    public void setFilaExcel(Integer filaExcel) {
        this.filaExcel = filaExcel;
    }
}
