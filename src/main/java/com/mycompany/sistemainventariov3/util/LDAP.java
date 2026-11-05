package com.mycompany.sistemainventariov3.util;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Utilidad LDAP/Active Directory para el login de Sistema Inventario.
 *
 * Permite el ingreso si el usuario pertenece directa o indirectamente a:
 * SC_Inv_Admin, SC_Inv_Tecnico o SC_Inv_Custodio.
 */
public class LDAP {

    private static final String LDAP_SERVER = LDAPConfig.getLdapServer();
    private static final int LDAP_PORT = LDAPConfig.getLdapPort();
    private static final String SEARCH_BASE = LDAPConfig.getSearchBase();

    private static final String grupoadm = "SC_Inv_Admin";
    private static final String grupotec = "SC_Inv_Tecnico";
    private static final String grupopcus = "SC_Inv_Custodio";

    private static final String SEARCH_BY_SAM_ACCOUNT_NAME = "(sAMAccountName={0})";
    private static final String SEARCH_GROUP_BY_CN = "(&(objectCategory=group)(cn={0}))";
    private static final String DISTINGUISHED_NAME = LDAPConfig.getAttrDn();
    private static final String CN = "cn";
    private static final String MEMBER_OF = LDAPConfig.getAttrMemberOf();

    // Se agregan los nombres originales como alias para no perder acceso con el AD actual
    private static final List<String> ADMIN_GROUP_ALIASES = Arrays.asList(grupoadm, "Admins. del dominio", "Administradores");
    private static final List<String> TECNICO_GROUP_ALIASES = Arrays.asList(grupotec, "SC_Tecnico", "Admins. del dominio", "Administradores");
    private static final List<String> CUSTODIO_GROUP_ALIASES = Arrays.asList(grupopcus, "SC_Custodio", "SC_Inventario", "Usuarios del dominio");

    public boolean validarIngresoLDAPSinRestriccion(String user, String pass) {
        if (!validarEntrada(user, pass) || !validarConexion()) {
            return false;
        }

        LDAPConnection conn = null;
        try {
            conn = abrirConexionAutenticada(user, pass);
            System.out.println("LDAP: credenciales validas para " + user);
            return true;
        } catch (LDAPException ex) {
            logLdapException("LDAP: credenciales invalidas o error autenticando " + user, ex);
            return false;
        } finally {
            cerrar(conn);
        }
    }

    /**
     * Retorna 1 si el usuario esta autorizado, -1 si autentica pero no esta en
     * grupos permitidos, y 0 si las credenciales/conexion fallan.
     */
    public int validarIngresoLDAP_FlexibleGroups(String user, String pass) {
        if (!validarEntrada(user, pass) || !validarConexion()) {
            return 0;
        }

        LDAPConnection conn = null;
        try {
            conn = abrirConexionAutenticada(user, pass);
            LDAPEntry userEntry = buscarUsuario(conn, user);
            if (userEntry == null) {
                System.out.println("LDAP: usuario no encontrado: " + user);
                return -1;
            }

            Set<String> grupos = obtenerGruposUsuario(conn, userEntry);
            if (contieneGrupo(grupos, ADMIN_GROUP_ALIASES)
                    || contieneGrupo(grupos, TECNICO_GROUP_ALIASES)
                    || contieneGrupo(grupos, CUSTODIO_GROUP_ALIASES)) {
                System.out.println("LDAP: usuario autorizado " + user + " grupos=" + grupos);
                return 1;
            }

            System.out.println("LDAP: usuario sin autorizacion para inventario " + user + " grupos=" + grupos);
            return -1;
        } catch (LDAPException ex) {
            if (ex.getResultCode() == LDAPException.INVALID_CREDENTIALS) {
                System.out.println("LDAP: credenciales incorrectas para " + user);
            } else {
                logLdapException("LDAP: error validando ingreso de " + user, ex);
            }
            return 0;
        } catch (Exception ex) {
            System.out.println("LDAP: error validando ingreso de " + user + ": " + ex);
            return 0;
        } finally {
            cerrar(conn);
        }
    }

    public int validarIngresoLDAPRestringido(String user, String pass, String groupName) {
        if (!validarEntrada(user, pass) || groupName == null || groupName.trim().isEmpty() || !validarConexion()) {
            return 0;
        }

        LDAPConnection conn = null;
        try {
            conn = abrirConexionAutenticada(user, pass);
            LDAPEntry userEntry = buscarUsuario(conn, user);
            if (userEntry == null) {
                return -1;
            }

            Set<String> grupos = obtenerGruposUsuario(conn, userEntry);
            
            List<String> gruposBuscados = Arrays.asList(groupName);
            if (grupoadm.equalsIgnoreCase(groupName)) gruposBuscados = ADMIN_GROUP_ALIASES;
            else if (grupotec.equalsIgnoreCase(groupName)) gruposBuscados = TECNICO_GROUP_ALIASES;
            else if (grupopcus.equalsIgnoreCase(groupName)) gruposBuscados = CUSTODIO_GROUP_ALIASES;

            return contieneGrupo(grupos, gruposBuscados) ? 1 : -1;
        } catch (LDAPException ex) {
            logLdapException("LDAP: error validando grupo " + groupName + " para " + user, ex);
            return 0;
        } catch (Exception ex) {
            System.out.println("LDAP: error validando grupo " + groupName + " para " + user + ": " + ex);
            return 0;
        } finally {
            cerrar(conn);
        }
    }

    public int validarIngresoLDAPRestringido(String user, String pass) {
        return validarIngresoLDAPRestringido(user, pass, grupopcus);
    }

    public List<String> obtenerRolesLDAP(String user, String pass) {
        List<String> roles = new ArrayList<>();
        if (!validarEntrada(user, pass) || !validarConexion()) {
            return roles;
        }

        LDAPConnection conn = null;
        try {
            conn = abrirConexionAutenticada(user, pass);
            LDAPEntry userEntry = buscarUsuario(conn, user);
            if (userEntry == null) {
                return roles;
            }

            Set<String> grupos = obtenerGruposUsuario(conn, userEntry);
            if (contieneGrupo(grupos, ADMIN_GROUP_ALIASES)) {
                roles.add("ADMINISTRADOR");
            }
            if (contieneGrupo(grupos, TECNICO_GROUP_ALIASES)) {
                roles.add("TECNICO");
            }

            // Todo usuario autorizado de inventario tambien puede entrar como custodio.
            if (contieneGrupo(grupos, ADMIN_GROUP_ALIASES)
                    || contieneGrupo(grupos, TECNICO_GROUP_ALIASES)
                    || contieneGrupo(grupos, CUSTODIO_GROUP_ALIASES)) {
                roles.add("CUSTODIO");
            }

            System.out.println("LDAP: roles de " + user + " = " + roles);
            return roles;
        } catch (Exception ex) {
            System.out.println("LDAP: error obteniendo roles de " + user + ": " + ex);
            return roles;
        } finally {
            cerrar(conn);
        }
    }

    public Map<String, String> obtenerInfoUsuarioLDAP(String user, String pass) {
        Map<String, String> info = new HashMap<>();
        if (!validarEntrada(user, pass) || !validarConexion()) {
            return info;
        }

        LDAPConnection conn = null;
        try {
            conn = abrirConexionAutenticada(user, pass);
            LDAPEntry entry = buscarUsuario(conn, user,
                    LDAPConfig.getAttrSam(),
                    LDAPConfig.getAttrDisplayName(),
                    LDAPConfig.getAttrMail(),
                    LDAPConfig.getAttrPhone(),
                    LDAPConfig.getAttrTitle(),
                    LDAPConfig.getAttrDepartment(),
                    DISTINGUISHED_NAME);

            if (entry == null) {
                return info;
            }

            LDAPAttributeSet attributeSet = entry.getAttributeSet();
            for (Object attr : attributeSet) {
                LDAPAttribute attribute = (LDAPAttribute) attr;
                info.put(attribute.getName(), attribute.getStringValue());
            }
            return info;
        } catch (Exception ex) {
            System.out.println("LDAP: error obteniendo informacion de " + user + ": " + ex);
            return info;
        } finally {
            cerrar(conn);
        }
    }

    public List<Map<String, String>> obtenerTodosLosUsuariosDelAD() {
        List<Map<String, String>> usuarios = new ArrayList<>();
        LDAPConnection conn = null;
        try {
            conn = abrirConexionLectura();
            String filter = "(&(objectCategory=person)(sAMAccountName=*))";
            String[] attributes = {
                "sAMAccountName", "displayName", "mail", "telephoneNumber",
                "title", "department", "distinguishedName", "memberOf"
            };

            LDAPSearchResults results = conn.search(SEARCH_BASE, LDAPConnection.SCOPE_SUB, filter, attributes, false);
            while (results.hasMore()) {
                LDAPEntry entry = results.next();
                Set<String> grupos = obtenerGruposUsuario(conn, entry);
                if (contieneGrupo(grupos, ADMIN_GROUP_ALIASES)
                        || contieneGrupo(grupos, TECNICO_GROUP_ALIASES)
                        || contieneGrupo(grupos, CUSTODIO_GROUP_ALIASES)) {
                    usuarios.add(entryToMap(entry));
                }
            }
        } catch (Exception ex) {
            System.out.println("LDAP: error obteniendo usuarios del AD: " + ex);
        } finally {
            cerrar(conn);
        }
        return usuarios;
    }

    public List<Map<String, String>> obtenerUsuariosDelGrupo(String nombreGrupo) {
        List<Map<String, String>> usuarios = new ArrayList<>();
        if (nombreGrupo == null || nombreGrupo.trim().isEmpty()) {
            return usuarios;
        }

        LDAPConnection conn = null;
        try {
            conn = abrirConexionLectura();
            LDAPEntry grupo = buscarGrupo(conn, nombreGrupo);
            if (grupo == null) {
                return usuarios;
            }

            LDAPAttribute memberAttr = grupo.getAttribute("member");
            if (memberAttr == null) {
                return usuarios;
            }

            for (String memberDN : memberAttr.getStringValueArray()) {
                LDAPEntry userEntry = buscarPorDn(conn, memberDN,
                        "sAMAccountName", "displayName", "mail", "telephoneNumber", "title", "department");
                if (userEntry != null) {
                    usuarios.add(entryToMap(userEntry));
                }
            }
        } catch (Exception ex) {
            System.out.println("LDAP: error obteniendo usuarios del grupo " + nombreGrupo + ": " + ex);
        } finally {
            cerrar(conn);
        }
        return usuarios;
    }

    public List<Map<String, String>> buscarUsuarioPorNombre(String nombreBusqueda) {
        List<Map<String, String>> usuarios = new ArrayList<>();
        if (nombreBusqueda == null || nombreBusqueda.trim().isEmpty()) {
            return usuarios;
        }

        LDAPConnection conn = null;
        try {
            conn = abrirConexionLectura();
            String value = escapeFilterValue(nombreBusqueda.trim());
            String filter = "(&(objectCategory=person)(|(displayName=*" + value + "*)(sAMAccountName=*" + value + "*)))";
            String[] attributes = {
                "sAMAccountName", "displayName", "mail", "telephoneNumber",
                "title", "department", "distinguishedName", "memberOf"
            };

            LDAPSearchResults results = conn.search(SEARCH_BASE, LDAPConnection.SCOPE_SUB, filter, attributes, false);
            while (results.hasMore()) {
                LDAPEntry entry = results.next();
                Set<String> grupos = obtenerGruposUsuario(conn, entry);
                if (contieneGrupo(grupos, ADMIN_GROUP_ALIASES)
                        || contieneGrupo(grupos, TECNICO_GROUP_ALIASES)
                        || contieneGrupo(grupos, CUSTODIO_GROUP_ALIASES)) {
                    usuarios.add(entryToMap(entry));
                }
            }
        } catch (Exception ex) {
            System.out.println("LDAP: error buscando usuario: " + ex);
        } finally {
            cerrar(conn);
        }
        return usuarios;
    }

    private LDAPConnection abrirConexionAutenticada(String user, String pass) throws LDAPException {
        LDAPConnection conn = new LDAPConnection();
        conn.connect(LDAP_SERVER, LDAP_PORT);
        conn.bind(LDAPConnection.LDAP_V3, LDAPConfig.buildPrincipal(user), pass);
        return conn;
    }

    private LDAPConnection abrirConexionLectura() throws LDAPException {
        LDAPConnection conn = new LDAPConnection();
        conn.connect(LDAP_SERVER, LDAP_PORT);
        if (!LDAPConfig.getLdapUserAdmin().isEmpty()) {
            conn.bind(LDAPConnection.LDAP_V3,
                    LDAPConfig.buildPrincipal(LDAPConfig.getLdapUserAdmin()),
                    LDAPConfig.getLdapPassAdmin());
        }
        return conn;
    }

    private LDAPEntry buscarUsuario(LDAPConnection conn, String user, String... attributes) throws LDAPException {
        String[] attrs = attributes == null || attributes.length == 0
                ? new String[]{DISTINGUISHED_NAME, CN, MEMBER_OF}
                : attributes;

        LDAPSearchResults results = conn.search(SEARCH_BASE,
                LDAPConnection.SCOPE_SUB,
                formatSearchFilter(SEARCH_BY_SAM_ACCOUNT_NAME, limpiarUsuario(user)),
                attrs,
                false);

        return results.hasMore() ? results.next() : null;
    }

    private LDAPEntry buscarGrupo(LDAPConnection conn, String groupName) throws LDAPException {
        LDAPSearchResults results = conn.search(SEARCH_BASE,
                LDAPConnection.SCOPE_SUB,
                formatSearchFilter(SEARCH_GROUP_BY_CN, groupName),
                new String[]{DISTINGUISHED_NAME, CN, MEMBER_OF, "member"},
                false);

        return results.hasMore() ? results.next() : null;
    }

    private LDAPEntry buscarPorDn(LDAPConnection conn, String dn, String... attributes) throws LDAPException {
        LDAPSearchResults results = conn.search(dn,
                LDAPConnection.SCOPE_BASE,
                "(objectClass=*)",
                attributes,
                false);

        return results.hasMore() ? results.next() : null;
    }

    private Set<String> obtenerGruposUsuario(LDAPConnection conn, LDAPEntry userEntry) {
        Set<String> grupos = new HashSet<>();
        Queue<String> pendientes = new ArrayDeque<>();
        Set<String> visitados = new HashSet<>();

        LDAPAttribute memberOfAttr = userEntry.getAttribute(MEMBER_OF);
        if (memberOfAttr != null) {
            pendientes.addAll(Arrays.asList(memberOfAttr.getStringValueArray()));
        }

        while (!pendientes.isEmpty()) {
            String groupDn = pendientes.poll();
            if (groupDn == null || !visitados.add(groupDn.toLowerCase())) {
                continue;
            }

            String groupCn = extractCN(groupDn);
            if (!groupCn.isEmpty()) {
                grupos.add(groupCn);
            }

            try {
                LDAPEntry groupEntry = buscarPorDn(conn, groupDn, CN, DISTINGUISHED_NAME, MEMBER_OF);
                if (groupEntry == null) {
                    continue;
                }
                LDAPAttribute parentGroups = groupEntry.getAttribute(MEMBER_OF);
                if (parentGroups != null) {
                    pendientes.addAll(Arrays.asList(parentGroups.getStringValueArray()));
                }
            } catch (Exception ex) {
                System.out.println("LDAP: no se pudo leer grupo " + groupDn + ": " + ex.getMessage());
            }
        }

        return grupos;
    }

    private boolean validarConexion() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LDAP_SERVER, LDAP_PORT), LDAPConfig.getTimeoutConexion());
            return true;
        } catch (Exception ex) {
            System.out.println("LDAP: no se puede conectar a " + LDAP_SERVER + ":" + LDAP_PORT + " - " + ex.getMessage());
            return false;
        }
    }

    private boolean validarEntrada(String user, String pass) {
        return user != null && !user.trim().isEmpty() && pass != null && !pass.trim().isEmpty();
    }

    private boolean contieneGrupo(Set<String> gruposUsuario, List<String> gruposEsperados) {
        for (String grupoUsuario : gruposUsuario) {
            for (String esperado : gruposEsperados) {
                if (esperado != null && esperado.equalsIgnoreCase(grupoUsuario)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String limpiarUsuario(String user) {
        String clean = user == null ? "" : user.trim();
        int slash = clean.indexOf('\\');
        if (slash >= 0 && slash < clean.length() - 1) {
            clean = clean.substring(slash + 1);
        }
        int at = clean.indexOf('@');
        if (at > 0) {
            clean = clean.substring(0, at);
        }
        return clean;
    }

    private static String extractCN(String dn) {
        if (dn == null || !dn.toUpperCase().startsWith("CN=")) {
            return "";
        }
        String cn = dn.substring(3);
        int position = cn.indexOf(',');
        return position == -1 ? cn : cn.substring(0, position);
    }

    private static String formatSearchFilter(String filter, String value) {
        return filter.replace("{0}", escapeFilterValue(value));
    }

    private static String escapeFilterValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\5c")
                .replace("*", "\\2a")
                .replace("(", "\\28")
                .replace(")", "\\29")
                .replace("\u0000", "\\00");
    }

    private Map<String, String> entryToMap(LDAPEntry entry) {
        Map<String, String> data = new HashMap<>();
        LDAPAttributeSet attributeSet = entry.getAttributeSet();
        for (Object attr : attributeSet) {
            LDAPAttribute attribute = (LDAPAttribute) attr;
            data.put(attribute.getName(), attribute.getStringValue());
        }
        return data;
    }

    private void cerrar(LDAPConnection conn) {
        if (conn == null || !conn.isConnected()) {
            return;
        }
        try {
            conn.disconnect();
        } catch (Exception ignored) {
            // No debe ocultar el resultado real de autenticacion.
        }
    }

    private void logLdapException(String message, LDAPException ex) {
        System.out.println(message + ": codigo=" + ex.getResultCode() + " detalle=" + ex.getMessage());
    }

    public static class RolPermisos {
        public static Map<String, Boolean> obtenerPermisos(String rol) {
            Map<String, Boolean> permisos = new HashMap<>();
            String rolNormalizado = rol == null ? "" : rol.toUpperCase();

            if ("ADMINISTRADOR".equals(rolNormalizado)) {
                permisos.put("puedeEditarTodos", true);
                permisos.put("puedeActualizarEstado", true);
                permisos.put("puedeVer", true);
                permisos.put("puedeCrearEquipo", true);
                permisos.put("puedeEditarCustodio", true);
                permisos.put("puedeExportarInventario", true);
                permisos.put("puedeVerHistorial", true);
            } else if ("TECNICO".equals(rolNormalizado)) {
                permisos.put("puedeEditarTodos", false);
                permisos.put("puedeActualizarEstado", false);
                permisos.put("puedeVer", true);
                permisos.put("puedeCrearEquipo", true);
                permisos.put("puedeEditarCustodio", true);
                permisos.put("puedeExportarInventario", true);
                permisos.put("puedeVerHistorial", true);
            } else {
                permisos.put("puedeEditarTodos", false);
                permisos.put("puedeActualizarEstado", false);
                permisos.put("puedeVer", true);
                permisos.put("puedeCrearEquipo", false);
                permisos.put("puedeEditarCustodio", false);
                permisos.put("puedeExportarInventario", false);
                permisos.put("puedeVerHistorial", true);
            }

            return permisos;
        }
    }
}
