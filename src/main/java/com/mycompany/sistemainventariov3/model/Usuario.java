package com.mycompany.sistemainventariov3.model;

/**
 * Modelo de sesion del usuario autenticado.
 */
public class Usuario {

    private String usuario;
    private String password;
    private String rol; // ADMINISTRADOR, TECNICO, CUSTODIO
    private String nombreCompleto;
    private Integer idCustodio;
    private boolean activo = true;
    private java.util.List<String> rolesDisponibles;

    public Usuario() {
    }

    public Usuario(String usuario, String password, String rol) {
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
    }

    public Usuario(String usuario, String password, String rol, String nombreCompleto, Integer idCustodio, boolean activo) {
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
        this.nombreCompleto = nombreCompleto;
        this.idCustodio = idCustodio;
        this.activo = activo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Integer getIdCustodio() {
        return idCustodio;
    }

    public void setIdCustodio(Integer idCustodio) {
        this.idCustodio = idCustodio;
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public java.util.List<String> getRolesDisponibles() { return rolesDisponibles; }
    public void setRolesDisponibles(java.util.List<String> rolesDisponibles) { this.rolesDisponibles = rolesDisponibles; }
}
