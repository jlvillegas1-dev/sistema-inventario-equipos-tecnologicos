package com.mycompany.sistemainventariov3.bean;

import com.mycompany.sistemainventariov3.model.Usuario;
import com.mycompany.sistemainventariov3.util.LDAP;
import com.mycompany.sistemainventariov3.util.LDAPConfig;
import com.mycompany.sistemainventariov3.util.SesionUsuario;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.primefaces.PrimeFaces;

@ManagedBean(name = "loginBean")
@SessionScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String usuario;
    private String clave;
    private boolean logeado = false;
    private boolean shake = true;

    private String nombreCompleto;
    private String grupoActual;
    private List<String> gruposDisponibles = new ArrayList<>();
    private Map<String, Boolean> permisos = new HashMap<>();
    private boolean esAdmin = false;
    private boolean esTecnico = false;
    private boolean esCustodio = false;
    private boolean lectura = false;

    public void login() {
        login(null);
    }

    public void login(ActionEvent actionEvent) {
        FacesMessage msg;
        LDAP ldap = new LDAP();

        if (usuario == null || usuario.trim().isEmpty() || clave == null || clave.trim().isEmpty()) {
            msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Usuario y contrasena requeridos");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            shake = true;
            return;
        }

        usuario = usuario.trim();
        reiniciarEstado();
        LDAPConfig.printConfig();

        int resultado = ldap.validarIngresoLDAP_FlexibleGroups(usuario, clave);
        if (resultado == 1) {
            List<String> roles = ldap.obtenerRolesLDAP(usuario, clave);
            if (roles.isEmpty()) {
                roles.add("CUSTODIO");
            }

            Map<String, String> info = ldap.obtenerInfoUsuarioLDAP(usuario, clave);
            nombreCompleto = info.getOrDefault("displayName", usuario);
            gruposDisponibles.addAll(roles);
            grupoActual = roles.get(0);

            shake = false;
            logeado = true;

            procesarAccesoAutenticado();
            msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Bienvenid@", nombreCompleto);
        } else if (resultado == -1) {
            shake = true;
            msg = new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "No autorizado",
                    "Usuario no pertenece a SC_Inventario, SC_Admin, SC_Tecnico o SC_Custodio");
        } else {
            shake = true;
            msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Login Error",
                    "Credenciales incorrectas o no hay conexion con el AD");
        }

        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    private void reiniciarEstado() {
        gruposDisponibles.clear();
        permisos.clear();
        esAdmin = false;
        esTecnico = false;
        esCustodio = false;
        logeado = false;
        lectura = false;
        nombreCompleto = null;
        grupoActual = null;
    }

    private void procesarAccesoAutenticado() {
        establecerPermisos();

        Usuario usuarioSesion = new Usuario(usuario, null, grupoActual, nombreCompleto, null, true);
        usuarioSesion.setRolesDisponibles(gruposDisponibles);
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequest();
        SesionUsuario.setUsuarioActual(request, usuarioSesion);

        PrimeFaces.current().ajax().addCallbackParam("estaLogeado", logeado);
        PrimeFaces.current().ajax().addCallbackParam("view", "dashboard.xhtml");
    }

    private void establecerPermisos() {
        permisos.clear();
        permisos.putAll(LDAP.RolPermisos.obtenerPermisos(grupoActual));

        // Asignación estricta según el grupo activo que el usuario ha seleccionado
        esAdmin = "ADMINISTRADOR".equals(grupoActual);
        esTecnico = "TECNICO".equals(grupoActual);
        esCustodio = "CUSTODIO".equals(grupoActual);
        lectura = !esAdmin && !esTecnico;
    }

    public void cambiarGrupo(String nuevoGrupo) {
        if (gruposDisponibles.contains(nuevoGrupo)) {
            grupoActual = nuevoGrupo;
            establecerPermisos();
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Exito", "Perfil cambiado a " + nuevoGrupo);
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void logout() {
        usuario = null;
        clave = null;
        logeado = false;
        shake = true;
        nombreCompleto = null;
        grupoActual = null;
        gruposDisponibles.clear();
        permisos.clear();
        esAdmin = false;
        esTecnico = false;
        esCustodio = false;
        SesionUsuario.limpiar();
    }

    public boolean tienePermiso(String nombrePermiso) {
        return permisos.getOrDefault(nombrePermiso, false);
    }

    public boolean tieneGrupo(String grupo) {
        return gruposDisponibles.contains(grupo);
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public boolean isLogeado() {
        return logeado;
    }

    public void setLogeado(boolean logeado) {
        this.logeado = logeado;
    }

    public boolean isShake() {
        return shake;
    }

    public void setShake(boolean shake) {
        this.shake = shake;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getGrupoActual() {
        return grupoActual;
    }

    public void setGrupoActual(String grupoActual) {
        this.grupoActual = grupoActual;
    }

    public List<String> getGruposDisponibles() {
        return gruposDisponibles;
    }

    public void setGruposDisponibles(List<String> gruposDisponibles) {
        this.gruposDisponibles = gruposDisponibles;
    }

    public Map<String, Boolean> getPermisos() {
        return permisos;
    }

    public void setPermisos(Map<String, Boolean> permisos) {
        this.permisos = permisos;
    }

    public boolean isEsAdmin() {
        return esAdmin;
    }

    public boolean isEsTecnico() {
        return esTecnico;
    }

    public boolean isEsCustodio() {
        return esCustodio;
    }

    public boolean isLectura() {
        return lectura;
    }

    public void setLectura(boolean lectura) {
        this.lectura = lectura;
    }
}
