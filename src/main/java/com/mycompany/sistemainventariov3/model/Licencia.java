package com.mycompany.sistemainventariov3.model;

import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Date;

/**
 * Entidad Licencia sobre el modelo final equipo + licencia.
 */
@Entity
@Table(name = "equipo")
@SecondaryTable(name = "licencia", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id_equipo", referencedColumnName = "id_equipo"))
@Where(clause = "tipo_equipo = 'licencia'")
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Integer idEquipo;

    @Column(name = "tipo_equipo", nullable = false)
    private String tipoEquipo = "licencia";

    @Column(name = "codigo_sbye", length = 120)
    private String codigoSbaiOriginal;

    @Transient
    private Integer item;

    @Column(name = "codigo_megan", length = 120)
    private String codigoMegan;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "fecha_ingreso")
    @Temporal(TemporalType.DATE)
    private Date fechaIngreso;

    @Column(name = "marca", length = 120)
    private String marca;

    @Column(name = "modelo", length = 200)
    private String modelo;

    @Column(name = "sn", length = 150)
    private String numeroSerie;

    @Column(name = "anterior_custodio", table = "licencia", length = 300)
    private String custodioAnterior;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_custodio_actual")
    private Custodio custodioActual;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ubicacion")
    private Ubicacion ubicacion;

    @Column(name = "ultima_actualizacion")
    @Temporal(TemporalType.DATE)
    private Date ultimaActualizacion;

    @Column(name = "caracteristicas", table = "licencia", columnDefinition = "TEXT")
    private String caracteristicas;

    @Column(name = "estado", length = 100)
    private String estado = "OPERATIVO";

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "acta_ugdt", table = "licencia", length = 100)
    private String actaUgdt;

    @Column(name = "acta_ugad", table = "licencia", length = 100)
    private String actaUgad;

    @Column(name = "anotaciones", table = "licencia", columnDefinition = "TEXT")
    private String anotaciones;

    @Transient
    private Integer filaExcel;

    public Licencia() {
    }

    public Licencia(String codigoSbaiOriginal, Integer item) {
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

    public Date getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Date fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
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

    public String getCustodioAnterior() {
        return custodioAnterior;
    }

    public void setCustodioAnterior(String custodioAnterior) {
        this.custodioAnterior = custodioAnterior;
    }

    public String getCustodioanterior() {
        return custodioAnterior;
    }

    public void setCustodioanterior(String custodioAnterior) {
        this.custodioAnterior = custodioAnterior;
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

    public String getActaUgdt() {
        return actaUgdt;
    }

    public void setActaUgdt(String actaUgdt) {
        this.actaUgdt = actaUgdt;
    }

    public String getActaUgad() {
        return actaUgad;
    }

    public void setActaUgad(String actaUgad) {
        this.actaUgad = actaUgad;
    }

    public String getAnotaciones() {
        return anotaciones;
    }

    public void setAnotaciones(String anotaciones) {
        this.anotaciones = anotaciones;
    }

    public Integer getFilaExcel() {
        return filaExcel;
    }

    public void setFilaExcel(Integer filaExcel) {
        this.filaExcel = filaExcel;
    }
}
