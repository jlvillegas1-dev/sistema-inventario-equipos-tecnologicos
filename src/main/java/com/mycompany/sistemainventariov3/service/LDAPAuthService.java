package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.model.Usuario;
import com.mycompany.sistemainventariov3.util.LDAP;

import java.util.List;
import java.util.Map;

/**
 * Adapta la validacion LDAP al modelo de sesion de la aplicacion.
 */
public class LDAPAuthService {

    private final LDAP ldap = new LDAP();
    private final UsuarioService usuarioService = new UsuarioService();

    /**
     * Mantiene compatibilidad con llamadas sin rolElegido.
     */
    public Usuario autenticarLDAP(String usuario, String password) throws Exception {
        return autenticarLDAP(usuario, password, null);
    }

    public Usuario autenticarLDAP(String usuario, String password, String rolElegido) throws Exception {
        if (usuario == null || usuario.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new Exception("Usuario y contrasena son requeridos");
        }

        usuario = usuario.trim();
        int resultadoValidacion = ldap.validarIngresoLDAP_FlexibleGroups(usuario, password);
        if (resultadoValidacion != 1) {
            if (resultadoValidacion == -1) {
                throw new Exception("El usuario no esta autorizado para acceder al sistema. "
                        + "Verifique que pertenece a SC_Inv_Admin, SC_Inv_Tecnico o SC_Inv_Custodio (o sus equivalentes anteriores).");
            }
            throw new Exception("Credenciales invalidas o error de conexion con LDAP");
        }

        List<String> rolesLDAP = ldap.obtenerRolesLDAP(usuario, password);
        if (rolesLDAP.isEmpty()) {
            rolesLDAP.add("CUSTODIO");
        }

        String rolFinal;
        if (rolElegido != null && !rolElegido.trim().isEmpty()) {
            String rolNormalizado = rolElegido.trim().toUpperCase();
            if (!rolesLDAP.contains(rolNormalizado)) {
                throw new Exception("No tiene permiso para el rol seleccionado: " + rolNormalizado);
            }
            rolFinal = rolNormalizado;
        } else {
            rolFinal = rolesLDAP.get(0);
        }

        Map<String, String> infoLDAP = ldap.obtenerInfoUsuarioLDAP(usuario, password);
        String nombreCompleto = infoLDAP.getOrDefault("displayName", usuario);

        Usuario u = new Usuario(usuario, null, rolFinal, nombreCompleto, null, true);
        u.setRolesDisponibles(rolesLDAP);
        enlazarUsuarioConBDLocal(u, infoLDAP);

        System.out.println("LDAP login OK: " + usuario
                + " roles=" + rolesLDAP
                + " idCustodio=" + u.getIdCustodio());
        return u;
    }

    public boolean autenticarLDAPSinRestriccion(String usuario, String password) throws Exception {
        if (usuario == null || usuario.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new Exception("Usuario y contrasena son requeridos");
        }
        return ldap.validarIngresoLDAPSinRestriccion(usuario.trim(), password);
    }

    public List<String> obtenerRolesLDAP(String usuario, String password) throws Exception {
        if (usuario == null || usuario.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new Exception("Usuario y contrasena son requeridos");
        }
        return ldap.obtenerRolesLDAP(usuario.trim(), password);
    }

    public Map<String, String> obtenerInfoUsuarioLDAP(String usuario, String password) throws Exception {
        if (usuario == null || usuario.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new Exception("Usuario y contrasena son requeridos");
        }
        return ldap.obtenerInfoUsuarioLDAP(usuario.trim(), password);
    }

    private void enlazarUsuarioConBDLocal(Usuario usuarioLDAP, Map<String, String> infoLDAP) {
        try {
            usuarioService.enlazarUsuarioLDAPConCustodioLocal(usuarioLDAP, infoLDAP);
        } catch (Exception e) {
            System.out.println("No se pudo enlazar usuario LDAP con custodio local: " + e.getMessage());
        }
    }

    public static Map<String, Boolean> obtenerPermisosDelRol(String rol) {
        return LDAP.RolPermisos.obtenerPermisos(rol);
    }
}
