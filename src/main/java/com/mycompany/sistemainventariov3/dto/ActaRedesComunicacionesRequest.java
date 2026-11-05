package com.mycompany.sistemainventariov3.dto;

import java.util.ArrayList;
import java.util.List;

public class ActaRedesComunicacionesRequest {

    private Funcionario funcionario = new Funcionario();
    private EquipoRow centralTelefonica = new EquipoRow();
    private EquipoRow equiposCisco = new EquipoRow();
    private EquipoRow controladoraWifiAccessPoints = new EquipoRow();
    private EquipoRow servidores = new EquipoRow();
    private EquipoRow switches = new EquipoRow();
    private List<ActividadMantenimiento> actividades = new ArrayList<>();
    private String certificacion;
    private FirmaRecepcion entrega = new FirmaRecepcion();
    private FirmaRecepcion recibe = new FirmaRecepcion();

    // Campos para transferir equipos y actividades manuales
    private List<EquipoRow> equiposManuales = new ArrayList<>();
    private List<ActividadMantenimiento> actividadesManuales = new ArrayList<>();

    public List<EquipoRow> getEquiposManuales() {
        return equiposManuales;
    }

    public void setEquiposManuales(List<EquipoRow> equiposManuales) {
        this.equiposManuales = equiposManuales;
    }

    public List<ActividadMantenimiento> getActividadesManuales() {
        return actividadesManuales;
    }

    public void setActividadesManuales(List<ActividadMantenimiento> actividadesManuales) {
        this.actividadesManuales = actividadesManuales;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public EquipoRow getCentralTelefonica() {
        return centralTelefonica;
    }

    public void setCentralTelefonica(EquipoRow centralTelefonica) {
        this.centralTelefonica = centralTelefonica;
    }

    public EquipoRow getEquiposCisco() {
        return equiposCisco;
    }

    public void setEquiposCisco(EquipoRow equiposCisco) {
        this.equiposCisco = equiposCisco;
    }

    public EquipoRow getControladoraWifiAccessPoints() {
        return controladoraWifiAccessPoints;
    }

    public void setControladoraWifiAccessPoints(EquipoRow controladoraWifiAccessPoints) {
        this.controladoraWifiAccessPoints = controladoraWifiAccessPoints;
    }

    public EquipoRow getServidores() {
        return servidores;
    }

    public void setServidores(EquipoRow servidores) {
        this.servidores = servidores;
    }

    public EquipoRow getSwitches() {
        return switches;
    }

    public void setSwitches(EquipoRow switches) {
        this.switches = switches;
    }

    public List<ActividadMantenimiento> getActividades() {
        return actividades;
    }

    public void setActividades(List<ActividadMantenimiento> actividades) {
        this.actividades = actividades;
    }

    public String getCertificacion() {
        return certificacion;
    }

    public void setCertificacion(String certificacion) {
        this.certificacion = certificacion;
    }

    public FirmaRecepcion getEntrega() {
        return entrega;
    }

    public void setEntrega(FirmaRecepcion entrega) {
        this.entrega = entrega;
    }

    public FirmaRecepcion getRecibe() {
        return recibe;
    }

    public void setRecibe(FirmaRecepcion recibe) {
        this.recibe = recibe;
    }

    public static class Funcionario {
        private String nombre;
        private String cargo;
        private String extension;
        private String correo;
        private String area;
        private String edificio;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getCargo() {
            return cargo;
        }

        public void setCargo(String cargo) {
            this.cargo = cargo;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public String getCorreo() {
            return correo;
        }

        public void setCorreo(String correo) {
            this.correo = correo;
        }

        public String getArea() {
            return area;
        }

        public void setArea(String area) {
            this.area = area;
        }

        public String getEdificio() {
            return edificio;
        }

        public void setEdificio(String edificio) {
            this.edificio = edificio;
        }
    }

    public static class EquipoRow {
        private String tipo;
        private String marca;
        private String modelo;
        private String serial;
        private String codigo;

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
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

        public String getSerial() {
            return serial;
        }

        public void setSerial(String serial) {
            this.serial = serial;
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }
    }

    public static class ActividadMantenimiento {
        private String actividad;
        private String fecha;
        private String estado;
        private String observacion;
        private String tipoEquipo;

        public String getTipoEquipo() {
            return tipoEquipo;
        }

        public void setTipoEquipo(String tipoEquipo) {
            this.tipoEquipo = tipoEquipo;
        }

        public String getActividad() {
            return actividad;
        }

        public void setActividad(String actividad) {
            this.actividad = actividad;
        }

        public String getFecha() {
            return fecha;
        }

        public void setFecha(String fecha) {
            this.fecha = fecha;
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
    }

    public static class FirmaRecepcion {
        private String nombre;
        private String firma;
        private String fecha;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getFirma() {
            return firma;
        }

        public void setFirma(String firma) {
            this.firma = firma;
        }

        public String getFecha() {
            return fecha;
        }

        public void setFecha(String fecha) {
            this.fecha = fecha;
        }
    }
}
