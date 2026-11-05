package com.mycompany.sistemainventariov3.util;

import com.mycompany.sistemainventariov3.model.Usuario;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Contexto de Sesión del Usuario
 * Guarda el usuario actual en la sesión HTTP.
 */
public class SesionUsuario {
    private static final String USER_SESSION_KEY = "USUARIO_ACTUAL";
    private static final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

    public static void setCurrentRequest(HttpServletRequest request) {
        currentRequest.set(request);
    }

    public static void clearCurrentRequest() {
        currentRequest.remove();
    }

    public static void setUsuarioActual(HttpServletRequest request, Usuario usuario) {
        if (request != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute(USER_SESSION_KEY, usuario);
        }
    }

    public static void setUsuarioActual(Usuario usuario) {
        setUsuarioActual(currentRequest.get(), usuario);
    }

    public static Usuario getUsuarioActual(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        return session != null ? (Usuario) session.getAttribute(USER_SESSION_KEY) : null;
    }

    public static Usuario getUsuarioActual() {
        return getUsuarioActual(currentRequest.get());
    }

    public static String getNombreUsuarioActual() {
        Usuario usuario = getUsuarioActual();
        return usuario != null ? usuario.getUsuario() : "ANONIMO";
    }

    public static boolean estaAutenticado() {
        return getUsuarioActual() != null;
    }

    public static boolean esAdministrador() {
        Usuario usuario = getUsuarioActual();
        return usuario != null && "ADMINISTRADOR".equals(usuario.getRol());
    }

    public static boolean esTecnico() {
        Usuario usuario = getUsuarioActual();
        return usuario != null && "TECNICO".equals(usuario.getRol());
    }

    public static boolean esCustodio() {
        Usuario usuario = getUsuarioActual();
        return usuario != null && "CUSTODIO".equals(usuario.getRol());
    }

    public static boolean tieneRol(String... roles) {
        Usuario usuario = getUsuarioActual();
        if (usuario == null || usuario.getRol() == null || roles == null) {
            return false;
        }
        for (String rol : roles) {
            if (usuario.getRol().equals(rol)) {
                return true;
            }
        }
        return false;
    }

    public static void limpiar(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    public static void limpiar() {
        limpiar(currentRequest.get());
    }
}
