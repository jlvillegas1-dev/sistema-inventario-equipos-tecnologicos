package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.model.Usuario;
import com.mycompany.sistemainventariov3.util.EncriptacionUtil;
import com.mycompany.sistemainventariov3.util.SesionUsuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de autenticacion contra base de datos.
 */
public class UsuarioService {

    private static final String PASSWORD_TEMPORAL = "Temporal123";

    /**
     * Autenticar usuario desde base de datos.
     * Si rolElegido es no nulo, valida que el usuario tenga ese rol disponible.
     */
    public Usuario autenticar(String usuario, String password, String rolElegido) throws Exception {
        if (usuario == null || usuario.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new Exception("Usuario y contrasena son requeridos");
        }

        try (Connection conn = DatabaseService.getConnection()) {
            provisionarCustodios(conn);
            UsuarioAuthRecord record = buscarUsuario(conn, usuario.trim());

            if (record == null) {
                throw new Exception("Credenciales invalidas");
            }
            if (!record.activo) {
                throw new Exception("El usuario esta inactivo");
            }
            if (!validarPassword(password, record.passwordHash)) {
                throw new Exception("Credenciales invalidas");
            }

            List<String> rolesDisponibles = obtenerRolesDisponibles(record);

            String rolFinal;
            if (rolElegido != null && !rolElegido.trim().isEmpty()) {
                String rolNormalizado = normalizarRol(rolElegido.trim());
                if (!rolesDisponibles.contains(rolNormalizado)) {
                    throw new Exception("No tiene permiso para el rol seleccionado: " + rolNormalizado);
                }
                rolFinal = rolNormalizado;
            } else {
                rolFinal = rolesDisponibles.get(0);
            }

            Usuario u = new Usuario(record.username, null, rolFinal, record.nombreCompleto, record.idCustodio, true);
            u.setRolesDisponibles(rolesDisponibles);
            return u;
        }
    }

    /** Mantener compatibilidad con llamadas sin rolElegido */
    public Usuario autenticar(String usuario, String password) throws Exception {
        return autenticar(usuario, password, null);
    }

    /**
     * Completa un usuario autenticado por LDAP con el id_custodio de la base local.
     *
     * LDAP valida identidad y grupos; la base local sabe que equipos pertenecen a
     * cada custodio. Sin este enlace, un rol CUSTODIO queda con idCustodio null.
     */
    public void enlazarUsuarioLDAPConCustodioLocal(Usuario usuarioLDAP, Map<String, String> infoLDAP) throws Exception {
        if (usuarioLDAP == null) {
            return;
        }

        try (Connection conn = DatabaseService.getConnection()) {
            provisionarCustodios(conn);

            UsuarioAuthRecord local = buscarUsuarioPorCandidatos(conn, construirCandidatosUsuario(usuarioLDAP, infoLDAP));
            if (local != null) {
                usuarioLDAP.setIdCustodio(local.idCustodio);
                if (usuarioLDAP.getNombreCompleto() == null || usuarioLDAP.getNombreCompleto().trim().isEmpty()) {
                    usuarioLDAP.setNombreCompleto(local.nombreCompleto);
                }
                System.out.println("[UsuarioService] Usuario LDAP enlazado por usuario local: "
                        + usuarioLDAP.getUsuario() + " -> id_custodio=" + local.idCustodio);
            }

            if (usuarioLDAP.getIdCustodio() == null) {
                String displayName = nombreLDAP(usuarioLDAP, infoLDAP);
                CustodioLocal custodio = buscarCustodioPorNombre(conn, displayName);
                if (custodio == null) {
                    // Si el custodio no existe en la base de datos, lo creamos automáticamente
                    custodio = crearCustodioLocal(conn, displayName);
                    if (custodio != null) {
                        System.out.println("[UsuarioService] Custodio creado automáticamente para LDAP: " + displayName);
                    }
                }
                
                if (custodio != null) {
                    // Crear el registro de usuario asociado automáticamente
                    crearUsuarioLocal(conn, usuarioLDAP.getUsuario(), custodio.idCustodio, usuarioLDAP.getRol());
                    
                    usuarioLDAP.setIdCustodio(custodio.idCustodio);
                    usuarioLDAP.setNombreCompleto(custodio.nombre);
                    System.out.println("[UsuarioService] Usuario LDAP autoprovisionado y enlazado: "
                            + usuarioLDAP.getUsuario() + " -> id_custodio=" + custodio.idCustodio);
                }
            }

            if (usuarioLDAP.getIdCustodio() == null) {
                System.out.println("[UsuarioService] ADVERTENCIA: No se pudo enlazar ni autoprovisionar al usuario LDAP: "
                        + usuarioLDAP.getUsuario());
            }
        }
    }

    // Obtiene roles desde la base y deja un punto claro para reemplazar esta logica por Active Directory.
    private List<String> obtenerRolesDisponibles(UsuarioAuthRecord record) {
        List<String> roles = new ArrayList<>();
        String rolPrincipal = normalizarRol(record.rol);
        agregarRolSiNoExiste(roles, rolPrincipal);
        if (record.rolesExtra != null && !record.rolesExtra.trim().isEmpty()) {
            for (String extra : record.rolesExtra.split(",")) {
                String r = normalizarRol(extra.trim());
                agregarRolSiNoExiste(roles, r);
            }
        }
        aplicarRolesDinamicosTransitorios(record, roles);
        return roles;
    }

    // Simula reglas que luego vendran desde grupos de Active Directory mediante LDAP.
    private void aplicarRolesDinamicosTransitorios(UsuarioAuthRecord record, List<String> roles) {
        String username = record.username == null ? "" : record.username.trim().toLowerCase(Locale.ROOT);
        if ("porozco".equals(username) || "eceracapa".equals(username)) {
            agregarRolSiNoExiste(roles, "ADMINISTRADOR");
            agregarRolSiNoExiste(roles, "CUSTODIO");
        }
        if (roles.contains("TECNICO")) {
            agregarRolSiNoExiste(roles, "CUSTODIO");
        }
    }

    // Evita roles duplicados y mantiene el orden de prioridad recibido.
    private void agregarRolSiNoExiste(List<String> roles, String rol) {
        if (rol == null || rol.trim().isEmpty()) {
            return;
        }
        String rolNormalizado = normalizarRol(rol);
        if (!rolNormalizado.isEmpty() && !roles.contains(rolNormalizado)) {
            roles.add(rolNormalizado);
        }
    }

    /**
     * Obtener usuario actual de la sesion.
     */
    public Usuario getUsuarioActual() {
        return SesionUsuario.getUsuarioActual();
    }

    /**
     * Logout.
     */
    public void logout() {
        SesionUsuario.limpiar();
    }

    public boolean esAdministrador() {
        return SesionUsuario.esAdministrador();
    }

    public boolean esTecnico() {
        return SesionUsuario.esTecnico();
    }
// Busca el usuario en la base de datos, primero intentando en la tabla "usuario" y luego en "usuarios" para mantener compatibilidad con diferentes esquemas. Devuelve un registro con la información necesaria para autenticación y autorización.
    private UsuarioAuthRecord buscarUsuario(Connection conn, String username) throws SQLException {
        if (existeTabla(conn, "usuario")) {
            boolean tieneRolesExtra = existeColumna(conn, "usuario", "roles_extra");
            String sql = "SELECT u.username, u.password_hash, u.rol, u.id_custodio, u.activo, " +
                    "COALESCE(c.nombre, u.username) AS nombre_completo" +
                    (tieneRolesExtra ? ", u.roles_extra" : "") +
                    " FROM usuario u " +
                    "LEFT JOIN custodio c ON c.id_custodio = u.id_custodio " +
                    "WHERE u.username = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UsuarioAuthRecord record = new UsuarioAuthRecord();
                        record.username = rs.getString("username");
                        record.passwordHash = rs.getString("password_hash");
                        record.rol = rs.getString("rol");
                        record.idCustodio = (Integer) rs.getObject("id_custodio");
                        record.activo = rs.getInt("activo") == 1;
                        record.nombreCompleto = rs.getString("nombre_completo");
                        record.rolesExtra = tieneRolesExtra ? rs.getString("roles_extra") : null;
                        return record;
                    }
                }
            }
        }

        if (existeTabla(conn, "usuarios")) {
            boolean tieneIdCustodio = existeColumna(conn, "usuarios", "id_custodio");
            String sql = "SELECT username, password, rol, nombre_completo, estado"
                    + (tieneIdCustodio ? ", id_custodio" : "")
                    + " FROM usuarios WHERE username = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UsuarioAuthRecord record = new UsuarioAuthRecord();
                        record.username = rs.getString("username");
                        record.passwordHash = rs.getString("password");
                        record.rol = rs.getString("rol");
                        record.nombreCompleto = rs.getString("nombre_completo");
                        String estado = rs.getString("estado");
                        record.activo = estado == null || "ACTIVO".equalsIgnoreCase(estado);
                        record.idCustodio = tieneIdCustodio ? (Integer) rs.getObject("id_custodio") : null;
                        return record;
                    }
                }
            }
        }

        return null;
    }

    private boolean validarPassword(String passwordIngresado, String passwordPersistido) {
        if (passwordPersistido == null) {
            return false;
        }
        if (passwordPersistido.equals(passwordIngresado)) {
            return true;
        }
        String md5 = EncriptacionUtil.encriptarMD5(passwordIngresado);
        return passwordPersistido.equalsIgnoreCase(md5);
    }

    private UsuarioAuthRecord buscarUsuarioPorCandidatos(Connection conn, Set<String> candidatos) throws SQLException {
        for (String candidato : candidatos) {
            if (candidato == null || candidato.trim().isEmpty()) {
                continue;
            }
            UsuarioAuthRecord record = buscarUsuario(conn, candidato.trim());
            if (record != null && record.activo) {
                return record;
            }
        }
        return null;
    }

    private Set<String> construirCandidatosUsuario(Usuario usuarioLDAP, Map<String, String> infoLDAP) {
        Set<String> candidatos = new LinkedHashSet<>();
        agregarCandidato(candidatos, usuarioLDAP.getUsuario());
        if (infoLDAP != null) {
            agregarCandidato(candidatos, infoLDAP.get("sAMAccountName"));
            agregarCandidato(candidatos, infoLDAP.get("userPrincipalName"));
            agregarCandidato(candidatos, infoLDAP.get("mail"));
            agregarCandidato(candidatos, generarClaveNombre(infoLDAP.get("displayName")));
        }
        agregarCandidato(candidatos, generarClaveNombre(usuarioLDAP.getNombreCompleto()));
        return candidatos;
    }

    private void agregarCandidato(Set<String> candidatos, String valor) {
        String limpio = limpiarUsuario(valor);
        if (!limpio.isEmpty()) {
            candidatos.add(limpio);
        }
        if (limpio.contains(".")) {
            String[] partes = limpio.split("\\.");
            if (partes.length >= 2 && !partes[0].isEmpty() && !partes[partes.length - 1].isEmpty()) {
                candidatos.add((partes[0].substring(0, 1) + partes[partes.length - 1]).toLowerCase(Locale.ROOT));
            }
        }
    }

    private String limpiarUsuario(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.trim();
        int slash = limpio.indexOf('\\');
        if (slash >= 0 && slash < limpio.length() - 1) {
            limpio = limpio.substring(slash + 1);
        }
        int at = limpio.indexOf('@');
        if (at > 0) {
            limpio = limpio.substring(0, at);
        }
        return normalizarTexto(limpio).replaceAll("[^a-z0-9.]", "");
    }

    private String nombreLDAP(Usuario usuarioLDAP, Map<String, String> infoLDAP) {
        if (infoLDAP != null) {
            String displayName = infoLDAP.get("displayName");
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }
        }
        return usuarioLDAP.getNombreCompleto();
    }

    private CustodioLocal buscarCustodioPorNombre(Connection conn, String nombre) throws SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }

        String nombreNormalizado = normalizarTexto(nombre);
        String claveNombre = generarClaveNombre(nombre);
        String sql = "SELECT id_custodio, nombre FROM custodio WHERE activo = 1 ORDER BY nombre";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nombreLocal = rs.getString("nombre");
                if (nombreNormalizado.equals(normalizarTexto(nombreLocal))
                        || (!claveNombre.isEmpty() && claveNombre.equals(generarClaveNombre(nombreLocal)))) {
                    CustodioLocal custodio = new CustodioLocal();
                    custodio.idCustodio = rs.getInt("id_custodio");
                    custodio.nombre = nombreLocal;
                    return custodio;
                }
            }
        }
        return null;
    }

    private String generarClaveNombre(String nombreCompleto) {
        String limpio = normalizarTexto(nombreCompleto).replaceAll("[^a-z0-9 ]", " ").trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            return "";
        }
        String[] partes = limpio.split(" ");
        if (partes.length == 1) {
            return partes[0];
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1]).replaceAll("[^a-z0-9]", "");
    }

    private String normalizarTexto(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

// Normaliza el rol ingresado para compararlo con los roles disponibles. Si no reconoce el rol, devuelve "TECNICO" por defecto.
    private String normalizarRol(String rol) {
        String valor = rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT);
        // Se pueden agregar más roles y sinónimos aquí según sea necesario
        if ("ADMIN".equals(valor) || "ADMINISTRADOR".equals(valor)) {
            return "ADMINISTRADOR";
        }
        if ("TECNICO".equals(valor)) {
            return "TECNICO";
        }
        if ("CUSTODIO".equals(valor)) {
            return "CUSTODIO";
        }
        return "TECNICO";
    }

    private void provisionarCustodios(Connection conn) {
        try {
            if (existeTabla(conn, "usuario")) {
                provisionarCustodiosTablaUsuario(conn);
                return;
            }
            if (existeTabla(conn, "usuarios") && existeColumna(conn, "usuarios", "id_custodio")) {
                provisionarCustodiosTablaUsuarios(conn);
            }
        } catch (Exception e) {
            System.err.println("[UsuarioService] No se pudo provisionar custodios: " + e.getMessage());
        }
    }

    private void provisionarCustodiosTablaUsuario(Connection conn) throws SQLException {
        Set<String> existentes = cargarUsernames(conn, "usuario");
        String sql = "SELECT c.id_custodio, c.nombre " +
                "FROM custodio c " +
                "LEFT JOIN usuario u ON u.id_custodio = c.id_custodio AND u.rol = 'CUSTODIO' " +
                "WHERE c.activo = 1 AND u.id_usuario IS NULL " +
                "ORDER BY c.nombre";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Integer idCustodio = rs.getInt("id_custodio");
                String nombre = rs.getString("nombre");
                String username = generarUsername(nombre, existentes);

                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO usuario (username, password_hash, rol, id_custodio, activo) VALUES (?, ?, 'CUSTODIO', ?, 1)")) {
                    insert.setString(1, username);
                    insert.setString(2, EncriptacionUtil.encriptarMD5(PASSWORD_TEMPORAL));
                    insert.setInt(3, idCustodio);
                    insert.executeUpdate();
                }
            }
        }
    }

    private void provisionarCustodiosTablaUsuarios(Connection conn) throws SQLException {
        Set<String> existentes = cargarUsernames(conn, "usuarios");
        String sql = "SELECT c.id_custodio, c.nombre " +
                "FROM custodio c " +
                "LEFT JOIN usuarios u ON u.id_custodio = c.id_custodio AND UPPER(u.rol) = 'CUSTODIO' " +
                "WHERE c.activo = 1 AND u.id_usuario IS NULL " +
                "ORDER BY c.nombre";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Integer idCustodio = rs.getInt("id_custodio");
                String nombre = rs.getString("nombre");
                String username = generarUsername(nombre, existentes);

                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO usuarios (username, password, nombre_completo, rol, estado, id_custodio, fecha_creacion) " +
                                "VALUES (?, ?, ?, 'CUSTODIO', 'ACTIVO', ?, NOW())")) {
                    insert.setString(1, username);
                    insert.setString(2, EncriptacionUtil.encriptarMD5(PASSWORD_TEMPORAL));
                    insert.setString(3, nombre);
                    insert.setInt(4, idCustodio);
                    insert.executeUpdate();
                }
            }
        }
    }

    private Set<String> cargarUsernames(Connection conn, String tabla) throws SQLException {
        Set<String> usernames = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM " + tabla);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("username");
                if (username != null) {
                    usernames.add(username.toLowerCase(Locale.ROOT));
                }
            }
        }
        return usernames;
    }

    private String generarUsername(String nombreCompleto, Set<String> existentes) {
        String limpio = Normalizer.normalize(nombreCompleto == null ? "" : nombreCompleto, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");

        String[] partes = limpio.isEmpty() ? new String[0] : limpio.split(" ");
        String base;
        if (partes.length == 0) {
            base = "custodio";
        } else if (partes.length == 1) {
            base = partes[0].toLowerCase(Locale.ROOT);
        } else {
            String inicial = partes[0].substring(0, 1).toLowerCase(Locale.ROOT);
            String apellido = partes[partes.length - 1].toLowerCase(Locale.ROOT);
            base = (inicial + apellido).replaceAll("[^a-z0-9]", "");
        }

        if (base.isEmpty()) {
            base = "custodio";
        }
        if (base.length() > 75) {
            base = base.substring(0, 75);
        }

        String candidato = base;
        int contador = 1;
        while (existentes.contains(candidato)) {
            String sufijo = String.valueOf(contador++);
            int limite = Math.max(1, 80 - sufijo.length());
            String prefijo = base.length() > limite ? base.substring(0, limite) : base;
            candidato = prefijo + sufijo;
        }
        existentes.add(candidato);
        return candidato;
    }

    private boolean existeTabla(Connection conn, String tabla) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existeColumna(Connection conn, String tabla, String columna) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private CustodioLocal crearCustodioLocal(Connection conn, String nombre) throws SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String sql = "INSERT INTO custodio (nombre, activo) VALUES (?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre.trim().toUpperCase());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    CustodioLocal cl = new CustodioLocal();
                    cl.idCustodio = rs.getInt(1);
                    cl.nombre = nombre.trim().toUpperCase();
                    return cl;
                }
            }
        }
        return null;
    }

    private void crearUsuarioLocal(Connection conn, String username, int idCustodio, String rol) throws SQLException {
        String sql = "INSERT INTO usuario (username, password_hash, rol, id_custodio, activo) VALUES (?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, EncriptacionUtil.encriptarMD5(PASSWORD_TEMPORAL));
            ps.setString(3, rol != null ? rol : "CUSTODIO");
            ps.setInt(4, idCustodio);
            ps.executeUpdate();
        }
    }

    private static class UsuarioAuthRecord {
        String username;
        String passwordHash;
        String rol;
        String rolesExtra;
        String nombreCompleto;
        Integer idCustodio;
        boolean activo;
    }

    private static class CustodioLocal {
        Integer idCustodio;
        String nombre;
    }
}
