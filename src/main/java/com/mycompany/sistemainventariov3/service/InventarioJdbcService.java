package com.mycompany.sistemainventariov3.service;

import com.google.gson.Gson;
import com.mycompany.sistemainventariov3.dto.InventoryItemDTO;
import com.mycompany.sistemainventariov3.service.inventory.InventarioSql;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Consultas JDBC contra el modelo final inventario_dtic_2026.
 */
public class InventarioJdbcService {
    private static final Gson GSON = new Gson();

    // --- Sugerencia: Centralizar nombres de columnas, tablas y valores fijos como constantes ---
    private static final class DbConstants {
        // Tablas
        static final String TABLE_EQUIPO = "equipo";
        static final String TABLE_CUSTODIO = "custodio";
        static final String TABLE_UBICACION = "ubicacion";
        static final String TABLE_AUDITORIA_CUSTODIO = "auditoria_custodio";

        // Columnas
        static final String COL_ID_EQUIPO = "id_equipo";
        static final String COL_ID_CUSTODIO_ACTUAL = "id_custodio_actual";

        // Roles
        static final String ROLE_ADMINISTRADOR = "ADMINISTRADOR";
        static final String ROLE_SISTEMA = "SISTEMA";
    }
    // --- Fin de la sugerencia ---

//Consulta 
    public List<InventoryItemDTO> obtenerInventarioCompleto() throws Exception {
        return consultarInventario(null);
    }

    public List<InventoryItemDTO> obtenerInventarioPorTipo(String tipo) throws Exception {
        return consultarInventario(tipo);
    }

    public List<InventoryItemDTO> obtenerInventarioPorCustodio(Integer idCustodio) throws Exception {
        return consultarInventarioRestringido(null, idCustodio);
    }

    public List<InventoryItemDTO> obtenerInventarioPorTipoYCustodio(String tipo, Integer idCustodio) throws Exception {
        return consultarInventarioRestringido(tipo, idCustodio);
    }

    public Map<String, Object> obtenerCatalogoCamposFormulario() throws Exception {
        Map<String, Object> catalogo = new LinkedHashMap<>();
        try (Connection conn = DatabaseService.getConnection()) {
            String schema = conn.getCatalog();
            for (String tipo : InventarioSql.TIPOS_FORMULARIO) {
                Map<String, Object> config = new LinkedHashMap<>();
                config.put("label", etiquetaTipo(tipo));
                config.put("hint", construirHintTipo(tipo));
                config.put("fields", obtenerCamposPorTipo(conn, schema, tipo));
                catalogo.put(tipo, config);
            }
        }
        return catalogo;
    }

    public List<Map<String, Object>> obtenerCustodiosActivos() throws Exception {
        return obtenerCustodiosActivos(null, null);
    }

    public List<Map<String, Object>> obtenerUbicaciones(String query, Integer limit) throws Exception {
        List<Map<String, Object>> ubicaciones = new ArrayList<>();
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean tieneFiltro = !term.isEmpty();
        int limite = limit != null && limit > 0 ? Math.min(limit, 500) : 0;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id_ubicacion, edificio, piso, direccion FROM ubicacion ");
        if (tieneFiltro) {
            sql.append("WHERE LOWER(CONCAT_WS(' ', edificio, piso, direccion)) LIKE ? ");
        }
        sql.append("ORDER BY edificio, piso, direccion");
        if (limite > 0) {
            sql.append(" LIMIT ?");
        }

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            if (tieneFiltro) {
                ps.setString(index++, "%" + term + "%");
            }
            if (limite > 0) {
                ps.setInt(index, limite);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getInt("id_ubicacion"));
                    item.put("edificio", rs.getString("edificio"));
                    item.put("piso", rs.getString("piso"));
                    item.put("direccion", rs.getString("direccion"));
                    item.put("nombre", unirNoVacios(rs.getString("edificio"), rs.getString("piso"), rs.getString("direccion")));
                    ubicaciones.add(item);
                }
            }
        }
        return ubicaciones;
    }

    public List<String> obtenerValoresDistintos(String campo, String query, Integer limit) throws Exception {
        if (!"marca".equals(campo) && !"modelo".equals(campo)) {
            throw new IllegalArgumentException("Catálogo no permitido para el campo: " + campo);
        }
        List<String> valores = new ArrayList<>();
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean tieneFiltro = !term.isEmpty();
        int limite = limit != null && limit > 0 ? Math.min(limit, 500) : 50;
        String sql = "SELECT DISTINCT " + campo + " FROM equipo " +
                "WHERE " + campo + " IS NOT NULL AND TRIM(" + campo + ") <> '' " +
                (tieneFiltro ? "AND LOWER(" + campo + ") LIKE ? " : "") +
                "ORDER BY " + campo + " LIMIT ?";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            if (tieneFiltro) {
                ps.setString(index++, "%" + term + "%");
            }
            ps.setInt(index, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    valores.add(rs.getString(1));
                }
            }
        }
        return valores;
    }

    public List<Map<String, Object>> obtenerCustodiosActivos(String query, Integer limit) throws Exception {
        List<Map<String, Object>> custodios = new ArrayList<>();
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean tieneFiltro = !term.isEmpty();
        int limite = limit != null && limit > 0 ? Math.min(limit, 500) : 0;

        try (Connection conn = DatabaseService.getConnection()) {
            boolean tieneTablaUsuario = existeTabla(conn, "usuario");
            StringBuilder sql = new StringBuilder();
            if (tieneTablaUsuario) {
                sql.append("SELECT c.id_custodio, c.nombre, u.username AS username, u.email AS email, ")
                        .append("MAX(ub.edificio) AS edificio, MAX(ub.direccion) AS area ")
                        .append("FROM custodio c ")
                        .append("LEFT JOIN usuario u ON u.id_custodio = c.id_custodio AND u.rol = 'CUSTODIO' AND u.activo = 1 ")
                        .append("LEFT JOIN equipo e ON e.id_custodio_actual = c.id_custodio ")
                        .append("LEFT JOIN ubicacion ub ON ub.id_ubicacion = e.id_ubicacion ")
                        .append("WHERE c.activo = 1 ");
                if (tieneFiltro) {
                    sql.append("AND (LOWER(c.nombre) LIKE ? OR LOWER(u.username) LIKE ?) ");
                }
                sql.append("GROUP BY c.id_custodio, c.nombre, u.username, u.email ");
                sql.append("ORDER BY c.nombre");
                if (limite > 0) {
                    sql.append(" LIMIT ?");
                }
            } else {
                sql.append("SELECT c.id_custodio, c.nombre, MAX(ub.edificio) AS edificio, MAX(ub.direccion) AS area ")
                        .append("FROM custodio c ")
                        .append("LEFT JOIN equipo e ON e.id_custodio_actual = c.id_custodio ")
                        .append("LEFT JOIN ubicacion ub ON ub.id_ubicacion = e.id_ubicacion ")
                        .append("WHERE c.activo = 1 ");
                if (tieneFiltro) {
                    sql.append("AND LOWER(c.nombre) LIKE ? ");
                }
                sql.append("GROUP BY c.id_custodio, c.nombre ");
                sql.append("ORDER BY c.nombre");
                if (limite > 0) {
                    sql.append(" LIMIT ?");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int index = 1;
                if (tieneFiltro) {
                    String like = "%" + term + "%";
                    ps.setString(index++, like);
                    if (tieneTablaUsuario) {
                        ps.setString(index++, like);
                    }
                }
                if (limite > 0) {
                    ps.setInt(index, limite);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", rs.getInt("id_custodio"));
                        item.put("nombre", rs.getString("nombre"));
                        item.put("edificio", rs.getString("edificio"));
                        item.put("area", rs.getString("area"));
                        if (tieneTablaUsuario) {
                            item.put("username", rs.getString("username"));
                            item.put("correo", rs.getString("email"));
                        }
                        custodios.add(item);
                    }
                }
            }
        }

        return custodios;
    }

    private boolean existeTabla(Connection conn, String tabla) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ? LIMIT 1"; // Nota: Esto es correcto para MySQL
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean equipoPerteneceACustodio(Integer idEquipo, Integer idCustodio) throws Exception {
        String sql = "SELECT 1 FROM " + DbConstants.TABLE_EQUIPO + " WHERE " + DbConstants.COL_ID_EQUIPO + " = ? AND " + DbConstants.COL_ID_CUSTODIO_ACTUAL + " = ? LIMIT 1";
        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            ps.setInt(2, idCustodio);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public InventoryItemDTO actualizarCustodioEquipo(
            Integer idEquipo,
            Integer nuevoIdCustodio,
            String usuario,
            String rol,
            boolean registrarAuditoria) throws Exception {
        try (Connection conn = DatabaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer idCustodioAnterior = obtenerIdCustodioActual(conn, idEquipo);
                if (idCustodioAnterior == null) {
                    throw new IllegalArgumentException("No se encontro el equipo indicado.");
                }

                String nombreAnterior = obtenerNombreCustodio(conn, idCustodioAnterior);
                String nombreNuevo = obtenerNombreCustodio(conn, nuevoIdCustodio);
                if (nombreNuevo == null || nombreNuevo.trim().isEmpty()) {
                    throw new IllegalArgumentException("Custodio no valido.");
                }
                List<Map<String, Object>> registroAnterior = construirCamposRegistroHistorial(conn, idEquipo);

                String update = "UPDATE " + DbConstants.TABLE_EQUIPO + " SET id_custodio_actual = ?, ultima_actualizacion = CURDATE() WHERE id_equipo = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setInt(1, nuevoIdCustodio);
                    ps.setInt(2, idEquipo);
                    ps.executeUpdate();
                }

                // Siempre registrar auditoría para todos los roles
                asegurarTablaAuditoria(conn);
                List<Map<String, Object>> registroNuevo = construirCamposRegistroHistorial(conn, idEquipo);
                registrarAuditoria(conn, idEquipo, usuario, rol, "Cambio de custodio",
                        nombreAnterior != null ? nombreAnterior : "(sin custodio)",
                        nombreNuevo,
                        "Custodio actual",
                        registroAnterior,
                        registroNuevo);

                conn.commit();
                return obtenerInventarioPorId(conn, idEquipo);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public InventoryItemDTO actualizarEstadoEquipo(Integer idEquipo, String nuevoEstado) throws Exception {
        return actualizarEstadoEquipo(idEquipo, nuevoEstado, null, null);
    }

    public InventoryItemDTO actualizarEstadoEquipo(Integer idEquipo, String nuevoEstado, String usuario, String rol) throws Exception {
        String estado = normalizarEstado(nuevoEstado);
        if (!"OPERATIVO".equals(estado) && !"NO OPERATIVO".equals(estado) && !"REPORTADO PARA DAR DE BAJA".equals(estado)) {
            throw new IllegalArgumentException("Estado no permitido.");
        }

        try (Connection conn = DatabaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Obtener estado anterior
                String estadoAnterior = "";
                String sqlSelect = "SELECT estado FROM equipo WHERE id_equipo = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                    ps.setInt(1, idEquipo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) estadoAnterior = rs.getString("estado");
                    }
                }

                List<Map<String, Object>> registroAnterior = construirCamposRegistroHistorial(conn, idEquipo);
                String update = "UPDATE equipo SET estado = ?, ultima_actualizacion = CURDATE() WHERE id_equipo = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, estado);
                    ps.setInt(2, idEquipo);
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        throw new IllegalArgumentException("No se encontro el equipo indicado.");
                    }
                }

                // Registrar auditoría
                asegurarTablaAuditoria(conn);
                List<Map<String, Object>> registroNuevo = construirCamposRegistroHistorial(conn, idEquipo);
                registrarAuditoria(conn, idEquipo,
                        usuario != null ? usuario : "SISTEMA",
                        rol != null ? rol : "ADMINISTRADOR",
                        "Cambio de estado", estadoAnterior, estado,
                        "Estado",
                        registroAnterior,
                        registroNuevo);

                conn.commit();
                return obtenerInventarioPorId(conn, idEquipo);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Map<String, Object>> obtenerHistorialEquipo(Integer idEquipo) throws Exception {
        try (Connection conn = DatabaseService.getConnection()) {
            asegurarTablaAuditoria(conn);
            // Incluir historial de custodio desde historial_custodio si existe
            List<Map<String, Object>> historial = new ArrayList<>();
            List<Map<String, Object>> camposRegistro = construirCamposRegistroHistorial(conn, idEquipo);

            // Fuente 1: tabla de auditoría general (cambios de custodio y estado)
            String sql1 = "SELECT id, usuario, rol, accion, valor_anterior, valor_nuevo, fecha, " +
                    "campo_modificado, registro_anterior, registro_nuevo " +
                    "FROM auditoria_custodio WHERE id_equipo = ? ORDER BY fecha DESC, id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setInt(1, idEquipo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", rs.getInt("id"));
                        item.put("usuario", rs.getString("usuario"));
                        item.put("rol", rs.getString("rol"));
                        item.put("accion", rs.getString("accion"));
                        item.put("valorAnterior", rs.getString("valor_anterior"));
                        item.put("valorNuevo", rs.getString("valor_nuevo"));
                        item.put("fecha", rs.getTimestamp("fecha"));
                        String campoModificado = coalesce(rs.getString("campo_modificado"), campoDesdeAccion(rs.getString("accion")));
                        item.put("campoModificado", campoModificado);
                        List<Map<String, Object>> camposAntes = leerCamposHistorial(rs.getString("registro_anterior"));
                        List<Map<String, Object>> camposDespues = leerCamposHistorial(rs.getString("registro_nuevo"));
                        item.put("camposAntes", camposAntes.isEmpty() ? construirCamposComparativosFallback(camposRegistro, campoModificado, rs.getString("valor_anterior")) : camposAntes);
                        item.put("camposDespues", camposDespues.isEmpty() ? construirCamposComparativosFallback(camposRegistro, campoModificado, rs.getString("valor_nuevo")) : camposDespues);
                        item.put("camposRegistro", camposDespues.isEmpty() ? camposRegistro : camposDespues);
                        item.put("fuente", "auditoria");
                        historial.add(item);
                    }
                }
            }

            // Fuente 2: historial_custodio (si existe)
            boolean tieneHistorialCustodio = existeTabla(conn, "historial_custodio");
            if (tieneHistorialCustodio) {
                String sql2 = "SELECT hc.id_historial AS id, " +
                        "COALESCE(c.nombre, hc.custodio_nombre_libre) AS custodio_nuevo, " +
                        "hc.fecha_inicio AS fecha, hc.motivo " +
                        "FROM historial_custodio hc " +
                        "LEFT JOIN custodio c ON c.id_custodio = hc.id_custodio " +
                        "WHERE hc.id_equipo = ? ORDER BY hc.fecha_inicio DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                    ps.setInt(1, idEquipo);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("id", "hc-" + rs.getInt("id"));
                            item.put("usuario", "(registro historico)");
                            item.put("rol", "SISTEMA");
                            item.put("accion", "Historial de custodio");
                            item.put("valorAnterior", "");
                            item.put("valorNuevo", rs.getString("custodio_nuevo"));
                            item.put("fecha", rs.getTimestamp("fecha"));
                            item.put("campoModificado", "Custodio actual");
                            item.put("camposAntes", construirCamposComparativosFallback(camposRegistro, "Custodio actual", ""));
                            item.put("camposDespues", construirCamposComparativosFallback(camposRegistro, "Custodio actual", rs.getString("custodio_nuevo")));
                            item.put("camposRegistro", camposRegistro);
                            item.put("fuente", "historial_custodio");
                            historial.add(item);
                        }
                    }
                }
            }

            historial.sort((a, b) -> {
                java.sql.Timestamp fa = (java.sql.Timestamp) a.get("fecha");
                java.sql.Timestamp fb = (java.sql.Timestamp) b.get("fecha");
                if (fa == null && fb == null) return 0;
                if (fa == null) return 1;
                if (fb == null) return -1;
                return fb.compareTo(fa);
            });
            return historial;
        }
    }

    public InventoryItemDTO actualizarEquipoCompleto(
            Integer idEquipo,
            Map<String, Object> payload,
            String usuario,
            String rol) throws Exception {
        if (payload == null) {
            payload = new LinkedHashMap<>();
        }
        try (Connection conn = DatabaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String tipo = obtenerTipoEquipo(conn, idEquipo);
                if (tipo == null) {
                    throw new IllegalArgumentException("Equipo no encontrado.");
                }

                resolverRelacionesCatalogo(conn, payload);

                String schema = conn.getCatalog();
                Set<String> columnasEquipo = obtenerColumnasPermitidas(conn, schema, "equipo",
                        new HashSet<>(Arrays.asList("id_equipo", "tipo_equipo", "creado_en", "actualizado_en")));
                Set<String> columnasHija = obtenerColumnasPermitidas(conn, schema, tablaHijaPorTipo(tipo),
                        new HashSet<>(Arrays.asList("id_equipo")));
                Map<String, Object> valoresAnteriores = new LinkedHashMap<>();
                cargarFilaComoMapa(conn, "equipo", "id_equipo", idEquipo, valoresAnteriores, false);
                cargarFilaComoMapa(conn, tablaHijaPorTipo(tipo), "id_equipo", idEquipo, valoresAnteriores, true);
                List<Map<String, Object>> registroAnterior = construirCamposRegistroHistorialDesdeValores(conn, idEquipo, tipo, valoresAnteriores);

                // Construir SET dinámico para equipo
                List<String> sets = new ArrayList<>();
                List<Object[]> params = new ArrayList<>();
                for (String col : columnasEquipo) {
                    if (payload.containsKey(col) && !"costo".equals(col)) {
                        params.add(new Object[]{col, payload.get(col)});
                    }
                }
                params = filtrarCambiosAuditoria(conn, valoresAnteriores, params);
                for (Object[] param : params) {
                    sets.add(param[0] + " = ?");
                }
                if (!params.isEmpty()) {
                    sets.add("ultima_actualizacion = CURDATE()");
                }

                if (!sets.isEmpty()) {
                    String updateSql = "UPDATE equipo SET " + String.join(", ", sets) + " WHERE id_equipo = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        int idx = 1;
                        for (Object[] p : params) {
                            setValorParametro(ps, idx++, (String) p[0], p[1]);
                        }
                        ps.setInt(idx, idEquipo);
                        ps.executeUpdate();
                    }
                }

                // Actualizar tabla hija
                List<String> setsHija = new ArrayList<>();
                List<Object[]> paramsHija = new ArrayList<>();
                for (String col : columnasHija) {
                    if (payload.containsKey(col)) {
                        paramsHija.add(new Object[]{col, payload.get(col)});
                    }
                }
                paramsHija = filtrarCambiosAuditoria(conn, valoresAnteriores, paramsHija);
                for (Object[] param : paramsHija) {
                    setsHija.add(param[0] + " = ?");
                }
                if (!setsHija.isEmpty()) {
                    String tabla = tablaHijaPorTipo(tipo);
                    String updateHija = "UPDATE " + tabla + " SET " + String.join(", ", setsHija) + " WHERE id_equipo = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateHija)) {
                        int idx = 1;
                        for (Object[] p : paramsHija) {
                            setValorParametro(ps, idx++, (String) p[0], p[1]);
                        }
                        ps.setInt(idx, idEquipo);
                        ps.executeUpdate();
                    }
                }

                asegurarTablaAuditoria(conn);
                if ("licencia".equals(tipo) && payload.containsKey("computador_valor")) {
                    asignarComputadorLicencia(conn, idEquipo,
                            texto(payload.get("computador_criterio")),
                            texto(payload.get("computador_valor")),
                            usuario != null ? usuario : "SISTEMA",
                            rol != null ? rol : "ADMINISTRADOR");
                }
                List<Map<String, Object>> registroNuevo = construirCamposRegistroHistorial(conn, idEquipo);
                registrarAuditoriaCambios(conn, idEquipo,
                        usuario != null ? usuario : "SISTEMA",
                        rol != null ? rol : "ADMINISTRADOR",
                        valoresAnteriores,
                        params,
                        registroAnterior,
                        registroNuevo);
                registrarAuditoriaCambios(conn, idEquipo,
                        usuario != null ? usuario : "SISTEMA",
                        rol != null ? rol : "ADMINISTRADOR",
                        valoresAnteriores,
                        paramsHija,
                        registroAnterior,
                        registroNuevo);
                registrarAuditoria(conn, idEquipo,
                        usuario != null ? usuario : "SISTEMA",
                        rol != null ? rol : "ADMINISTRADOR",
                        "Edición completa del equipo", "", "Campos actualizados: " + String.join(", ", sets));

                conn.commit();
                return obtenerInventarioPorId(conn, idEquipo);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Map<String, Object> obtenerDatosEdicionEquipo(Integer idEquipo) throws Exception {
        try (Connection conn = DatabaseService.getConnection()) {
            String tipo = obtenerTipoEquipo(conn, idEquipo);
            if (tipo == null) {
                throw new IllegalArgumentException("Equipo no encontrado.");
            }

            Map<String, Object> datos = new LinkedHashMap<>();
            cargarFilaComoMapa(conn, "equipo", "id_equipo", idEquipo, datos, false);
            cargarFilaComoMapa(conn, tablaHijaPorTipo(tipo), "id_equipo", idEquipo, datos, true);

            Integer idCustodioActual = obtenerIdCustodioActual(conn, idEquipo);
            datos.put("custodio_nombre", idCustodioActual != null ? obtenerNombreCustodio(conn, idCustodioActual) : "");

            Integer idUbicacion = leerEnteroSeguro(datos.get("id_ubicacion"));
            Map<String, String> ubicacion = obtenerUbicacionPorId(conn, idUbicacion);
            datos.put("ubicacion_edificio", ubicacion.get("edificio"));
            datos.put("ubicacion_piso", ubicacion.get("piso"));
            datos.put("ubicacion_direccion", ubicacion.get("direccion"));
            datos.put("tipo", tipo);
            if ("licencia".equals(tipo)) {
                Map<String, String> computador = obtenerComputadorLicencia(conn, idEquipo);
                datos.put("computador_codigo_sbye", computador.get("codigo_sbye"));
                datos.put("computador_codigo_megan", computador.get("codigo_megan"));
                datos.put("computador_ip", computador.get("ip"));
            }

            return datos;
        }
    }

    private List<Map<String, Object>> construirCamposRegistroHistorial(Connection conn, Integer idEquipo) throws Exception {
        String tipo = obtenerTipoEquipo(conn, idEquipo);
        if (tipo == null) {
            return Collections.emptyList();
        }

        Map<String, Object> equipo = new LinkedHashMap<>();
        Map<String, Object> hija = new LinkedHashMap<>();
        cargarFilaComoMapa(conn, "equipo", "id_equipo", idEquipo, equipo, false);
        cargarFilaComoMapa(conn, tablaHijaPorTipo(tipo), "id_equipo", idEquipo, hija, true);

        List<Map<String, Object>> campos = new ArrayList<>();
        agregarCampoHistorial(campos, "id_equipo", "ID equipo", idEquipo);
        agregarCampoHistorial(campos, "tipo_equipo", "Tipo de equipo", etiquetaTipo(tipo));

        for (String campo : camposEquipoHistorial()) {
            agregarCampoHistorial(campos, campo, etiquetaCampo(campo), equipo.get(campo));
        }

        Integer idCustodio = leerEnteroSeguro(equipo.get("id_custodio_actual"));
        agregarCampoHistorial(campos, "id_custodio_actual", "Custodio actual",
                idCustodio != null ? obtenerNombreCustodio(conn, idCustodio) : "");

        Map<String, String> ubicacion = obtenerUbicacionPorId(conn, leerEnteroSeguro(equipo.get("id_ubicacion")));
        agregarCampoHistorial(campos, "id_ubicacion", "Ubicación",
                unirNoVacios(ubicacion.get("edificio"), ubicacion.get("piso"), ubicacion.get("direccion")));
        agregarCampoHistorial(campos, "ubicacion_edificio", "Edificio", ubicacion.get("edificio"));
        agregarCampoHistorial(campos, "ubicacion_piso", "Piso", ubicacion.get("piso"));
        agregarCampoHistorial(campos, "ubicacion_direccion", "Dirección / Área", ubicacion.get("direccion"));

        for (Map.Entry<String, Object> entry : hija.entrySet()) {
            agregarCampoHistorial(campos, entry.getKey(), etiquetaCampo(entry.getKey()), entry.getValue());
        }

        return campos;
    }

    private List<Map<String, Object>> construirCamposRegistroHistorialDesdeValores(
            Connection conn,
            Integer idEquipo,
            String tipo,
            Map<String, Object> valores) throws Exception {
        List<Map<String, Object>> campos = new ArrayList<>();
        agregarCampoHistorial(campos, "id_equipo", "ID equipo", idEquipo);
        agregarCampoHistorial(campos, "tipo_equipo", "Tipo de equipo", etiquetaTipo(tipo));

        for (String campo : camposEquipoHistorial()) {
            agregarCampoHistorial(campos, campo, etiquetaCampo(campo), valores.get(campo));
        }

        Integer idCustodio = leerEnteroSeguro(valores.get("id_custodio_actual"));
        agregarCampoHistorial(campos, "id_custodio_actual", "Custodio actual",
                idCustodio != null ? obtenerNombreCustodio(conn, idCustodio) : "");

        Map<String, String> ubicacion = obtenerUbicacionPorId(conn, leerEnteroSeguro(valores.get("id_ubicacion")));
        agregarCampoHistorial(campos, "id_ubicacion", "Ubicación",
                unirNoVacios(ubicacion.get("edificio"), ubicacion.get("piso"), ubicacion.get("direccion")));
        agregarCampoHistorial(campos, "ubicacion_edificio", "Edificio", ubicacion.get("edificio"));
        agregarCampoHistorial(campos, "ubicacion_piso", "Piso", ubicacion.get("piso"));
        agregarCampoHistorial(campos, "ubicacion_direccion", "Dirección / Área", ubicacion.get("direccion"));

        Set<String> omitidos = new HashSet<>(camposEquipoHistorial());
        omitidos.addAll(Arrays.asList("id_equipo", "tipo_equipo", "id_custodio_actual", "id_ubicacion"));
        for (Map.Entry<String, Object> entry : valores.entrySet()) {
            if (!omitidos.contains(entry.getKey())) {
                agregarCampoHistorial(campos, entry.getKey(), etiquetaCampo(entry.getKey()), entry.getValue());
            }
        }

        return campos;
    }

    private List<String> camposEquipoHistorial() {
        return Arrays.asList(
                "codigo_megan",
                "codigo_sbye",
                "descripcion",
                "marca",
                "modelo",
                "sn",
                "fecha_ingreso",
                "costo",
                "estado",
                "observacion",
                "ultima_actualizacion",
                "ultimo_mantenimiento",
                "creado_en",
                "actualizado_en"
        );
    }

    private void agregarCampoHistorial(List<Map<String, Object>> campos, String campo, String label, Object valor) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("campo", campo);
        item.put("label", label);
        item.put("valor", texto(valor));
        campos.add(item);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> leerCamposHistorial(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Object parsed = GSON.fromJson(json, Object.class);
            if (!(parsed instanceof List)) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> campos = new ArrayList<>();
            for (Object entry : (List<Object>) parsed) {
                if (entry instanceof Map) {
                    Map<String, Object> campo = new LinkedHashMap<>();
                    ((Map<String, Object>) entry).forEach(campo::put);
                    campos.add(campo);
                }
            }
            return campos;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> construirCamposComparativosFallback(
            List<Map<String, Object>> base,
            String campoModificado,
            String valorCampoModificado) {
        List<Map<String, Object>> copia = new ArrayList<>();
        String campoNormalizado = normalizarEtiquetaHistorial(campoModificado);
        for (Map<String, Object> original : base) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.putAll(original);
            String label = texto(item.get("label"));
            if (normalizarEtiquetaHistorial(label).equals(campoNormalizado)) {
                item.put("valor", valorCampoModificado != null ? valorCampoModificado : "");
            }
            copia.add(item);
        }
        return copia;
    }

    private String normalizarEtiquetaHistorial(String valor) {
        return valor == null ? "" : valor.toLowerCase(Locale.ROOT)
                .replace("id custodio actual", "custodio actual")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String campoDesdeAccion(String accion) {
        String textoAccion = accion != null ? accion.trim() : "";
        String textoNormalizado = textoAccion.toLowerCase(Locale.ROOT);
        int separadorCampo = textoAccion.indexOf(':');
        if (textoNormalizado.startsWith("edici") && separadorCampo >= 0) {
            String campo = textoAccion.substring(separadorCampo + 1).trim();
            if (campo.toLowerCase(Locale.ROOT).contains("id custodio")) {
                return "Custodio actual";
            }
            return campo;
        }
        if (textoAccion.toLowerCase(Locale.ROOT).contains("custodio")) {
            return "Custodio actual";
        }
        if (textoAccion.toLowerCase(Locale.ROOT).contains("estado")) {
            return "Estado";
        }
        return "Equipo";
    }

    private String obtenerTipoEquipo(Connection conn, Integer idEquipo) throws Exception {
        String sql = "SELECT tipo_equipo FROM equipo WHERE id_equipo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("tipo_equipo");
            }
        }
        return null;
    }

    // ===== Relación licencia -> computador (licencia.id_computador) =====

    private static final String SQL_COMPUTADOR_BASE =
            "SELECT e.id_equipo, e.codigo_sbye, e.codigo_megan, COALESCE(p.ip, lp.ip) AS ip " +
            "FROM equipo e LEFT JOIN pc p ON p.id_equipo = e.id_equipo " +
            "LEFT JOIN laptop lp ON lp.id_equipo = e.id_equipo ";

    // Criterios con los que el usuario puede indicar la PC/laptop de una licencia
    private String columnaCriterioComputador(String criterio) {
        switch (criterio) {
            case "codigo_sbye":
                return "TRIM(e.codigo_sbye)";
            case "codigo_megan":
                return "TRIM(e.codigo_megan)";
            case "ip":
                return "TRIM(COALESCE(p.ip, lp.ip))";
            default:
                throw new IllegalArgumentException("Opción de búsqueda de computador no válida.");
        }
    }

    private String etiquetaCriterioComputador(String criterio) {
        switch (criterio) {
            case "codigo_megan":
                return "Código Megan";
            case "ip":
                return "IP";
            default:
                return "Código SBYE";
        }
    }

    // Datos de la PC/laptop a la que pertenece la licencia (valores vacíos si no tiene)
    private Map<String, String> obtenerComputadorLicencia(Connection conn, Integer idLicencia) throws Exception {
        Map<String, String> computador = new LinkedHashMap<>();
        computador.put("codigo_sbye", "");
        computador.put("codigo_megan", "");
        computador.put("ip", "");
        String sql = SQL_COMPUTADOR_BASE +
                "WHERE e.id_equipo = (SELECT id_computador FROM licencia WHERE id_equipo = ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idLicencia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    computador.put("codigo_sbye", texto(rs.getString("codigo_sbye")));
                    computador.put("codigo_megan", texto(rs.getString("codigo_megan")));
                    computador.put("ip", texto(rs.getString("ip")));
                }
            }
        }
        return computador;
    }

    private String describirComputador(Map<String, String> computador) {
        return unirNoVacios(
                computador.get("codigo_sbye").isEmpty() ? "" : "SBYE " + computador.get("codigo_sbye"),
                computador.get("codigo_megan").isEmpty() ? "" : "Megan " + computador.get("codigo_megan"),
                computador.get("ip").isEmpty() ? "" : "IP " + computador.get("ip"));
    }

    // Une la licencia con la PC/laptop que coincida con el valor según el criterio elegido
    // (Código SBYE, Código Megan o IP); un valor vacío quita la unión.
    private void asignarComputadorLicencia(
            Connection conn,
            Integer idLicencia,
            String criterio,
            String valor,
            String usuario,
            String rol) throws Exception {
        // Sin opción elegida en "Asignar equipo": la asignación actual se mantiene
        if (criterio.isEmpty()) {
            return;
        }
        String columna = columnaCriterioComputador(criterio);
        Map<String, String> anterior = obtenerComputadorLicencia(conn, idLicencia);
        // Sin cambios: evita volver a validar un valor repetido que ya estaba asignado
        if (valor.equals(anterior.get(criterio))) {
            return;
        }

        Integer idComputador = null;
        if (!valor.isEmpty()) {
            List<Integer> encontrados = new ArrayList<>();
            String buscar = SQL_COMPUTADOR_BASE + "WHERE e.tipo_equipo IN ('pc', 'laptop') AND " + columna + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(buscar)) {
                ps.setString(1, valor);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        encontrados.add(rs.getInt("id_equipo"));
                    }
                }
            }
            String etiqueta = etiquetaCriterioComputador(criterio);
            // "la IP" / "el Código SBYE" / "el Código Megan"
            String articulo = "ip".equals(criterio) ? "La" : "El";
            String asignado = "ip".equals(criterio) ? "asignada" : "asignado";
            if (encontrados.isEmpty()) {
                throw new IllegalArgumentException("No existe una PC o laptop con " + etiqueta + " " + valor + ".");
            }
            if (encontrados.size() > 1) {
                throw new IllegalArgumentException(articulo + " " + etiqueta + " " + valor + " está " + asignado
                        + " a más de un equipo (" + encontrados.size() + " equipos). "
                        + "Seleccione otra opción en \"Asignar equipo\" para identificar el equipo correcto.");
            }
            idComputador = encontrados.get(0);
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE licencia SET id_computador = ? WHERE id_equipo = ?")) {
            if (idComputador != null) {
                ps.setInt(1, idComputador);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, idLicencia);
            ps.executeUpdate();
        }

        String textoAnterior = describirComputador(anterior);
        String textoNuevo = describirComputador(obtenerComputadorLicencia(conn, idLicencia));
        if (!textoAnterior.equals(textoNuevo)) {
            registrarAuditoria(conn, idLicencia, usuario, rol, "Edición: Computador asignado", textoAnterior, textoNuevo);
        }
    }

    public InventoryItemDTO crearEquipo(String tipo, Map<String, Object> payload) throws Exception {
        return crearEquipo(tipo, payload, null, null);
    }

    public InventoryItemDTO crearEquipo(String tipo, Map<String, Object> payload, String usuario, String rol) throws Exception {
        validarTipoFormulario(tipo);
        if (payload == null) {
            payload = new LinkedHashMap<>();
        }
        try (Connection conn = DatabaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String schema = conn.getCatalog();
                Set<String> columnasEquipo = obtenerColumnasPermitidas(conn, schema, "equipo", new HashSet<>(Arrays.asList(
                        "id_equipo", "tipo_equipo", "creado_en", "actualizado_en"
                )));
                Set<String> columnasHija = obtenerColumnasPermitidas(conn, schema, tablaHijaPorTipo(tipo), new HashSet<>(Arrays.asList("id_equipo")));
                payload.remove("costo");
                resolverRelacionesCatalogo(conn, payload);

                Integer idEquipo = insertarEquipo(conn, tipo, payload, columnasEquipo);
                insertarTablaHija(conn, tipo, idEquipo, payload, columnasHija);
                if ("licencia".equals(tipo) && payload.containsKey("computador_valor")) {
                    asegurarTablaAuditoria(conn);
                    asignarComputadorLicencia(conn, idEquipo,
                            texto(payload.get("computador_criterio")),
                            texto(payload.get("computador_valor")),
                            usuario != null ? usuario : "SISTEMA",
                            rol != null ? rol : "ADMINISTRADOR");
                }

                conn.commit();
                return obtenerInventarioPorId(conn, idEquipo);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void eliminarEquipo(Integer idEquipo) throws Exception {
        try (Connection conn = DatabaseService.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String tipo = obtenerTipoEquipo(conn, idEquipo);
                if (tipo == null) {
                    throw new IllegalArgumentException("El equipo indicado no existe.");
                }

                // 1. Borrar auditoria
                String deleteAuditoria = "DELETE FROM auditoria_custodio WHERE id_equipo = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteAuditoria)) {
                    ps.setInt(1, idEquipo);
                    ps.executeUpdate();
                }

                // 2. Borrar historial_custodio si existe la tabla
                if (existeTabla(conn, "historial_custodio")) {
                    String deleteHistorial = "DELETE FROM historial_custodio WHERE id_equipo = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteHistorial)) {
                        ps.setInt(1, idEquipo);
                        ps.executeUpdate();
                    }
                }

                // 3. Borrar de la tabla hija
                String tablaHija = tablaHijaPorTipo(tipo);
                String deleteHija = "DELETE FROM " + tablaHija + " WHERE id_equipo = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteHija)) {
                    ps.setInt(1, idEquipo);
                    ps.executeUpdate();
                }

                // 4. Borrar de la tabla principal equipo
                String deleteEquipo = "DELETE FROM equipo WHERE id_equipo = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteEquipo)) {
                    ps.setInt(1, idEquipo);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Consulta principal de inventario, con opción de filtrar por tipo
    private List<InventoryItemDTO> consultarInventario(String tipo) throws Exception {
        List<InventoryItemDTO> items = new ArrayList<>();
        String sql = InventarioSql.BASE_QUERY
                + InventarioSql.INVENTARIO_TYPES_FILTER
                + (tipo != null && !tipo.trim().isEmpty() ? "AND e.tipo_equipo = ? " : "")
                + InventarioSql.ORDER_BY_ID_EQUIPO;

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (tipo != null && !tipo.trim().isEmpty()) {
                ps.setString(1, tipo);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapearItem(rs));
                }
            }
        }

        return items;
    }

    private List<InventoryItemDTO> consultarInventarioRestringido(String tipo, Integer idCustodio) throws Exception {
        if (idCustodio == null) {
            return Collections.emptyList();
        }

        List<InventoryItemDTO> items = new ArrayList<>();
        String sql = InventarioSql.BASE_QUERY
                + InventarioSql.INVENTARIO_TYPES_FILTER
                + "AND e.id_custodio_actual = ? "
                + (tipo != null && !tipo.trim().isEmpty() ? "AND e.tipo_equipo = ? " : "")
                + InventarioSql.ORDER_BY_ID_EQUIPO;

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCustodio);
            if (tipo != null && !tipo.trim().isEmpty()) {
                ps.setString(2, tipo);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapearItem(rs));
                }
            }
        }
        return items;
    }

    private InventoryItemDTO mapearItem(ResultSet rs) throws Exception {
        InventoryItemDTO item = new InventoryItemDTO();
        String tipo = valor(rs, "tipo_equipo");

        item.setId(rs.getInt("id_equipo"));
        item.setTipo(tipo);
        item.setSubtipo(valor(rs, "infraestructura_subtipo"));
        item.setCodigoSbai(valor(rs, "codigo_sbye"));
        item.setCodigoMegan(valor(rs, "codigo_megan"));
        item.setDescripcion(valor(rs, "descripcion"));
        item.setMarca(valor(rs, "marca"));
        item.setModelo(valor(rs, "modelo"));
        item.setNumeroSerie(valor(rs, "sn"));
        item.setCustodio(valor(rs, "custodio_nombre"));
        item.setUbicacion(construirUbicacion(rs));
        item.setUbicacionEdificio(valor(rs, "edificio"));
        item.setUbicacionPiso(valor(rs, "piso"));
        item.setUbicacionDireccion(valor(rs, "direccion"));
        item.setEstado(normalizarEstado(valor(rs, "estado")));
        item.setObservacion(valor(rs, "observacion"));
        item.setProcesador(coalesce(valor(rs, "pc_procesador"), valor(rs, "laptop_procesador")));
        item.setRam(coalesce(valor(rs, "pc_ram"), valor(rs, "laptop_ram")));
        item.setDiscoDuro(coalesce(valor(rs, "pc_disco_duro"), valor(rs, "laptop_disco_duro")));
        item.setSistemaOperativo(coalesce(valor(rs, "pc_so"), valor(rs, "laptop_so")));
        item.setIp(coalesce(valor(rs, "pc_ip"), valor(rs, "laptop_ip"), valor(rs, "impresora_ip")));
        item.setCaracteristicas(construirCaracteristicas(rs, tipo));
        item.setFechaIngreso(valor(rs, "fecha_ingreso"));
        item.setUltimaActualizacion(valor(rs, "ultima_actualizacion"));
        item.setUltimoMantenimiento(valor(rs, "ultimo_mantenimiento"));
        int idComputador = rs.getInt("licencia_id_computador");
        item.setIdComputador(rs.wasNull() ? null : idComputador);
        return item;
    }

    private String construirUbicacion(ResultSet rs) throws Exception {
        List<String> partes = new ArrayList<>();
        agregarSiTieneTexto(partes, valor(rs, "edificio"));
        agregarSiTieneTexto(partes, valor(rs, "piso"));
        agregarSiTieneTexto(partes, valor(rs, "direccion"));
        return String.join(" - ", partes);
    }

    private String construirCaracteristicas(ResultSet rs, String tipo) throws Exception {
        if ("periferico".equals(tipo)) {
            return unirNoVacios(valor(rs, "tipo_periferico"), valor(rs, "periferico_caracteristicas"));
        }
        if ("impresora".equals(tipo)) {
            return unirNoVacios(valor(rs, "tipo_impresora"), valor(rs, "impresora_caracteristicas"));
        }
        if ("escaner".equals(tipo)) {
            return valor(rs, "escaner_caracteristicas");
        }
        if ("telefono".equals(tipo)) {
            return valor(rs, "telefono_caracteristicas");
        }
        if ("proyector".equals(tipo)) {
            return unirNoVacios(valor(rs, "proyector_caracteristicas"), valor(rs, "proyector_anotaciones"));
        }
        if ("infraestructura".equals(tipo)) {
            return unirNoVacios(valor(rs, "infraestructura_subtipo"), valor(rs, "infraestructura_caracteristicas"), valor(rs, "infraestructura_anotaciones"));
        }
        if ("licencia".equals(tipo)) {
            return unirNoVacios(valor(rs, "licencia_caracteristicas"), valor(rs, "licencia_anotaciones"));
        }
        if ("bien_control_admin".equals(tipo)) {
            return valor(rs, "codigo_anterior");
        }
        if ("modem".equals(tipo)) {
            return unirNoVacios(
                    valor(rs, "modem_numero_contrato"),
                    valor(rs, "modem_numero_servicio"),
                    valor(rs, "modem_plan_comercial"),
                    valor(rs, "modem_estado_servicio"),
                    valor(rs, "modem_megas"),
                    valor(rs, "modem_acreditacion"),
                    valor(rs, "modem_caracteristicas"),
                    valor(rs, "modem_anotaciones"));
        }
        return unirNoVacios(itemProceso(rs), itemMemoria(rs), itemDisco(rs), itemSistema(rs));
    }

    private List<Map<String, Object>> obtenerCamposPorTipo(Connection conn, String schema, String tipo) throws Exception {
        List<Map<String, Object>> campos = new ArrayList<>();
        campos.addAll(obtenerCamposEquipo(conn, schema));
        campos.addAll(obtenerCamposTabla(conn, schema, tablaHijaPorTipo(tipo), true));
        return campos;
    }

    private List<Map<String, Object>> obtenerCamposEquipo(Connection conn, String schema) throws Exception {
        List<Map<String, Object>> campos = new ArrayList<>();
        String sql = "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = 'equipo' ORDER BY ordinal_position";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String columna = rs.getString("column_name");
                    if (!InventarioSql.CAMPOS_COMUNES_EQUIPO.contains(columna)) {
                        continue;
                    }
                    campos.add(crearCampoMeta(columna, rs.getString("data_type"), rs.getString("is_nullable"), "equipo"));
                }
            }
        }
        agregarCamposRelacion(campos);
        return campos;
    }

    private void agregarCamposRelacion(List<Map<String, Object>> campos) {
        campos.add(crearCampoMeta("custodio_nombre", "varchar", "YES", "custodio"));
        campos.add(crearCampoMeta("ubicacion_edificio", "varchar", "YES", "ubicacion"));
        campos.add(crearCampoMeta("ubicacion_piso", "varchar", "YES", "ubicacion"));
        campos.add(crearCampoMeta("ubicacion_direccion", "varchar", "YES", "ubicacion"));
    }

    private List<Map<String, Object>> obtenerCamposTabla(Connection conn, String schema, String tabla, boolean excluirIdEquipo) throws Exception {
        List<Map<String, Object>> campos = new ArrayList<>();
        String sql = "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String columna = rs.getString("column_name");
                    if (excluirIdEquipo && "id_equipo".equals(columna)) {
                        continue;
                    }
                    // id_computador se asigna con el buscador de computador del editor de licencias, no como campo numérico
                    if ("id_computador".equals(columna)) {
                        continue;
                    }
                    campos.add(crearCampoMeta(columna, rs.getString("data_type"), rs.getString("is_nullable"), tabla));
                }
            }
        }
        return campos;
    }

    private Set<String> obtenerColumnasPermitidas(Connection conn, String schema, String tabla, Set<String> excluidas) throws Exception {
        Set<String> columnas = new HashSet<>();
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nombre = rs.getString("column_name");
                    if (!excluidas.contains(nombre)) {
                        columnas.add(nombre);
                    }
                }
            }
        }
        return columnas;
    }

    private void resolverRelacionesCatalogo(Connection conn, Map<String, Object> payload) throws Exception {
        if (!payload.containsKey("id_custodio_actual")) {
            String nombreCustodio = texto(payload.get("custodio_nombre"));
            if (!nombreCustodio.isEmpty()) {
                payload.put("id_custodio_actual", obtenerOCrearCustodio(conn, nombreCustodio));
            }
        }

        if (!payload.containsKey("id_ubicacion")) {
            String edificio = texto(payload.get("ubicacion_edificio"));
            String piso = texto(payload.get("ubicacion_piso"));
            String direccion = texto(payload.get("ubicacion_direccion"));
            if (!edificio.isEmpty() || !piso.isEmpty() || !direccion.isEmpty()) {
                payload.put("id_ubicacion", obtenerOCrearUbicacion(conn, edificio, piso, direccion));
            }
        }
    }

    private Integer obtenerOCrearCustodio(Connection conn, String nombre) throws Exception {
        String select = "SELECT id_custodio FROM custodio WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_custodio");
                }
            }
        }

        String insert = "INSERT INTO custodio (nombre, activo) VALUES (?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo crear el custodio.");
    }

    private Integer obtenerOCrearUbicacion(Connection conn, String edificio, String piso, String direccion) throws Exception {
        String select = "SELECT id_ubicacion FROM ubicacion " +
                "WHERE COALESCE(LOWER(TRIM(edificio)), '') = LOWER(TRIM(?)) " +
                "AND COALESCE(LOWER(TRIM(piso)), '') = LOWER(TRIM(?)) " +
                "AND COALESCE(LOWER(TRIM(direccion)), '') = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, edificio);
            ps.setString(2, piso);
            ps.setString(3, direccion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_ubicacion");
                }
            }
        }

        String insert = "INSERT INTO ubicacion (edificio, piso, direccion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            setNullableString(ps, 1, edificio);
            setNullableString(ps, 2, piso);
            setNullableString(ps, 3, direccion);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo crear la ubicacion.");
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            ps.setNull(index, Types.VARCHAR);
            return;
        }
        ps.setString(index, value.trim());
    }

    private Integer insertarEquipo(Connection conn, String tipo, Map<String, Object> payload, Set<String> columnasEquipo) throws Exception {
        List<String> columnas = new ArrayList<>();
        columnas.add("tipo_equipo");
        for (String columna : columnasEquipo) {
            if (payload.containsKey(columna) && !esValorVacio(payload.get(columna))) {
                columnas.add(columna);
            }
        }

        String sql = "INSERT INTO equipo (" + String.join(", ", columnas) + ") VALUES (" +
                String.join(", ", Collections.nCopies(columnas.size(), "?")) + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            ps.setString(index++, tipo);
            for (int i = 1; i < columnas.size(); i++) {
                setValorParametro(ps, index++, columnas.get(i), payload.get(columnas.get(i)));
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el id del equipo creado.");
    }

    private void insertarTablaHija(Connection conn, String tipo, Integer idEquipo, Map<String, Object> payload, Set<String> columnasHija) throws Exception {
        String tabla = tablaHijaPorTipo(tipo);
        List<String> columnas = new ArrayList<>();
        columnas.add("id_equipo");
        for (String columna : columnasHija) {
            if (payload.containsKey(columna) && !esValorVacio(payload.get(columna))) {
                columnas.add(columna);
            }
        }

        String sql = "INSERT INTO " + tabla + " (" + String.join(", ", columnas) + ") VALUES (" +
                String.join(", ", Collections.nCopies(columnas.size(), "?")) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            for (int i = 1; i < columnas.size(); i++) {
                setValorParametro(ps, i + 1, columnas.get(i), payload.get(columnas.get(i)));
            }
            ps.executeUpdate();
        }
    }

    private InventoryItemDTO obtenerInventarioPorId(Connection conn, Integer idEquipo) throws Exception {
        String sql = InventarioSql.BASE_QUERY + "WHERE e.id_equipo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearItem(rs);
                }
            }
        }
        throw new SQLException("No se pudo recuperar el equipo creado.");
    }

    private Integer obtenerIdCustodioActual(Connection conn, Integer idEquipo) throws Exception {
        String sql = "SELECT id_custodio_actual FROM equipo WHERE id_equipo = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object valor = rs.getObject("id_custodio_actual");
                    return valor == null ? null : ((Number) valor).intValue();
                }
            }
        }
        return null;
    }

    private void cargarFilaComoMapa(Connection conn, String tabla, String columnaId, Integer id, Map<String, Object> destino, boolean ignorarLlave) throws Exception {
        String sql = "SELECT * FROM " + tabla + " WHERE " + columnaId + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                int totalColumnas = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= totalColumnas; i++) {
                    String nombre = rs.getMetaData().getColumnLabel(i);
                    if (ignorarLlave && columnaId.equalsIgnoreCase(nombre)) {
                        continue;
                    }
                    destino.put(nombre, rs.getObject(i));
                }
            }
        }
    }

    private Map<String, String> obtenerUbicacionPorId(Connection conn, Integer idUbicacion) throws Exception {
        Map<String, String> ubicacion = new LinkedHashMap<>();
        ubicacion.put("edificio", "");
        ubicacion.put("piso", "");
        ubicacion.put("direccion", "");
        if (idUbicacion == null) {
            return ubicacion;
        }

        String sql = "SELECT edificio, piso, direccion FROM ubicacion WHERE id_ubicacion = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUbicacion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ubicacion.put("edificio", coalesce(rs.getString("edificio"), ""));
                    ubicacion.put("piso", coalesce(rs.getString("piso"), ""));
                    ubicacion.put("direccion", coalesce(rs.getString("direccion"), ""));
                }
            }
        }
        return ubicacion;
    }

    private Integer leerEnteroSeguro(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        String texto = String.valueOf(valor).trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String obtenerNombreCustodio(Connection conn, Integer idCustodio) throws Exception {
        if (idCustodio == null) {
            return "";
        }
        String sql = "SELECT nombre FROM custodio WHERE id_custodio = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCustodio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }
        }
        return null;
    }

    private void asegurarTablaAuditoria(Connection conn) throws Exception {
        String ddl = "CREATE TABLE IF NOT EXISTS auditoria_custodio (" +
                "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "id_equipo INT NOT NULL," +
                "usuario VARCHAR(120) NOT NULL," +
                "rol VARCHAR(40) NOT NULL," +
                "accion VARCHAR(160) NOT NULL," +
                "valor_anterior TEXT NULL," +
                "valor_nuevo TEXT NULL," +
                "fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_auditoria_equipo (id_equipo)," +
                "KEY idx_auditoria_usuario (usuario)," +
                "KEY idx_auditoria_fecha (fecha)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (PreparedStatement ps = conn.prepareStatement(ddl)) {
            ps.execute();
        }
        asegurarColumnaAuditoria(conn, "campo_modificado", "VARCHAR(160) NULL");
        asegurarColumnaAuditoria(conn, "registro_anterior", "LONGTEXT NULL");
        asegurarColumnaAuditoria(conn, "registro_nuevo", "LONGTEXT NULL");
    }

    private void asegurarColumnaAuditoria(Connection conn, String columna, String definicion) throws Exception {
        String schema = conn.getCatalog();
        String sql = "SELECT 1 FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = 'auditoria_custodio' AND column_name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE auditoria_custodio ADD COLUMN " + columna + " " + definicion)) {
            ps.execute();
        }
    }

    private void registrarAuditoria(
            Connection conn,
            Integer idEquipo,
            String usuario,
            String rol,
            String accion,
            String valorAnterior,
            String valorNuevo) throws Exception {
        registrarAuditoria(conn, idEquipo, usuario, rol, accion, valorAnterior, valorNuevo,
                campoDesdeAccion(accion), null, null);
    }

    private void registrarAuditoria(
            Connection conn,
            Integer idEquipo,
            String usuario,
            String rol,
            String accion,
            String valorAnterior,
            String valorNuevo,
            String campoModificado,
            List<Map<String, Object>> registroAnterior,
            List<Map<String, Object>> registroNuevo) throws Exception {
        if (accion != null && accion.toLowerCase(Locale.ROOT).contains("completa del equipo")) {
            return;
        }
        String sql = "INSERT INTO auditoria_custodio " +
                "(id_equipo, usuario, rol, accion, valor_anterior, valor_nuevo, campo_modificado, registro_anterior, registro_nuevo, fecha) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEquipo);
            ps.setString(2, usuario != null ? usuario : "ANONIMO");
            ps.setString(3, rol != null ? rol : "DESCONOCIDO");
            ps.setString(4, accion);
            ps.setString(5, valorAnterior);
            ps.setString(6, valorNuevo);
            ps.setString(7, campoModificado);
            ps.setString(8, registroAnterior != null ? GSON.toJson(registroAnterior) : null);
            ps.setString(9, registroNuevo != null ? GSON.toJson(registroNuevo) : null);
            ps.executeUpdate();
        }
    }

    private void registrarAuditoriaCambios(
            Connection conn,
            Integer idEquipo,
            String usuario,
            String rol,
            Map<String, Object> valoresAnteriores,
            List<Object[]> cambios,
            List<Map<String, Object>> registroAnterior,
            List<Map<String, Object>> registroNuevo) throws Exception {
        for (Object[] cambio : cambios) {
            String columna = (String) cambio[0];
            if ("ultima_actualizacion".equals(columna)) {
                continue;
            }
            String anterior = valorAuditoriaCampo(conn, columna, valoresAnteriores.get(columna));
            String nuevo = valorAuditoriaCampo(conn, columna, cambio[1]);
            if (anterior.equals(nuevo)) {
                continue;
            }
            registrarAuditoria(conn, idEquipo, usuario, rol,
                    "Edición de campo: " + etiquetaCampo(columna),
                    anterior,
                    nuevo,
                    etiquetaCampo(columna),
                    registroAnterior,
                    registroNuevo);
        }
    }

    private List<Object[]> filtrarCambiosAuditoria(
            Connection conn,
            Map<String, Object> valoresAnteriores,
            List<Object[]> cambios) throws Exception {
        List<Object[]> filtrados = new ArrayList<>();
        for (Object[] cambio : cambios) {
            String columna = (String) cambio[0];
            if ("ultima_actualizacion".equals(columna)) {
                continue;
            }
            String anterior = valorAuditoriaCampo(conn, columna, valoresAnteriores.get(columna));
            String nuevo = valorAuditoriaCampo(conn, columna, cambio[1]);
            if (!anterior.equals(nuevo)) {
                filtrados.add(cambio);
            }
        }
        return filtrados;
    }

    private String valorAuditoriaCampo(Connection conn, String columna, Object valor) throws Exception {
        if ("id_custodio_actual".equals(columna)) {
            Integer idCustodio = leerEnteroSeguro(valor);
            return idCustodio != null ? coalesce(obtenerNombreCustodio(conn, idCustodio), "") : "";
        }
        if ("id_ubicacion".equals(columna)) {
            Map<String, String> ubicacion = obtenerUbicacionPorId(conn, leerEnteroSeguro(valor));
            return unirNoVacios(ubicacion.get("edificio"), ubicacion.get("piso"), ubicacion.get("direccion"));
        }
        return texto(valor);
    }

    private void setValorParametro(PreparedStatement ps, int index, String columna, Object valor) throws Exception {
        if (esValorVacio(valor)) {
            ps.setNull(index, Types.NULL);
            return;
        }

        String texto = String.valueOf(valor).trim();
        if ("costo".equals(columna)) {
            ps.setBigDecimal(index, new BigDecimal(texto));
            return;
        }
        if ("megas".equals(columna) || "id_custodio_actual".equals(columna) || "id_ubicacion".equals(columna)) {
            ps.setInt(index, Integer.parseInt(texto));
            return;
        }
        if (columna.startsWith("fecha_") || "ultima_actualizacion".equals(columna) || "ultimo_mantenimiento".equals(columna)) {
            ps.setDate(index, Date.valueOf(texto));
            return;
        }
        ps.setString(index, texto);
    }

    private boolean esValorVacio(Object valor) {
        return valor == null || String.valueOf(valor).trim().isEmpty();
    }

    private void validarTipoFormulario(String tipo) {
        if (!InventarioSql.TIPOS_FORMULARIO.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de equipo no permitido: " + tipo);
        }
    }

    private Map<String, Object> crearCampoMeta(String columna, String dataType, String nullable, String origen) {
        Map<String, Object> campo = new LinkedHashMap<>();
        campo.put("name", columna);
        campo.put("label", etiquetaCampo(columna));
        campo.put("dataType", dataType);
        campo.put("required", "NO".equalsIgnoreCase(nullable));
        campo.put("source", origen);
        campo.put("inputType", sugerirInputType(columna, dataType));
        return campo;
    }

    private String tablaHijaPorTipo(String tipo) {
        if ("bien_control_admin".equals(tipo)) {
            return "bien_control_admin";
        }
        return tipo;
    }

    private String sugerirInputType(String columna, String dataType) {
        if ("estado".equals(columna)) {
            return "select";
        }
        if (columna.startsWith("fecha_") || "ultima_actualizacion".equals(columna) || "ultimo_mantenimiento".equals(columna)) {
            return "date";
        }
        if ("costo".equals(columna) || "megas".equals(columna)) {
            return "number";
        }
        if (dataType != null && ("text".equalsIgnoreCase(dataType) || "mediumtext".equalsIgnoreCase(dataType) || "longtext".equalsIgnoreCase(dataType))) {
            return "textarea";
        }
        return "text";
    }

    private String etiquetaTipo(String tipo) {
        switch (tipo) {
            case "pc": return "PC";
            case "laptop": return "Laptop";
            case "periferico": return "Periférico";
            case "impresora": return "Impresora";
            case "escaner": return "Escáner";
            case "telefono": return "Teléfono";
            case "proyector": return "Proyector";
            case "infraestructura": return "Infraestructura";
            case "licencia": return "Licencia";
            case "bien_control_admin": return "Bien de control administrativo";
            case "modem": return "Módem";
            default: return tipo;
        }
    }

    private String construirHintTipo(String tipo) {
        return "";
    }

    private String etiquetaCampo(String columna) {
        switch (columna) {
            case "id_equipo": return "ID equipo";
            case "tipo_equipo": return "Tipo de equipo";
            case "codigo_megan": return "Código Megan";
            case "codigo_sbye": return "Código SBYE";
            case "sn": return "Número de serie";
            case "fecha_ingreso": return "Fecha de ingreso";
            case "creado_en": return "Creado en";
            case "actualizado_en": return "Actualizado en";
            case "ultima_actualizacion": return "Última actualización";
            case "ultimo_mantenimiento": return "Último mantenimiento";
            case "id_custodio_actual": return "Custodio actual";
            case "id_ubicacion": return "Ubicación";
            case "custodio_nombre": return "Custodio actual";
            case "ubicacion_edificio": return "Edificio";
            case "ubicacion_piso": return "Piso";
            case "ubicacion_direccion": return "Dirección / Área";
            case "tipo_periferico": return "Tipo de periférico";
            case "tipo_impresora": return "Tipo de impresora";
            case "codigo_anterior": return "Código anterior";
            case "numero_contrato": return "Número de contrato";
            case "numero_servicio": return "Número de servicio";
            case "plan_comercial": return "Plan comercial";
            case "estado_servicio": return "Estado del servicio";
            case "anterior_custodio": return "Custodio anterior";
            default:
                String normalizado = columna.replace('_', ' ');
                return Character.toUpperCase(normalizado.charAt(0)) + normalizado.substring(1);
        }
    }

    private String normalizarEstado(String estadoOriginal) {
        String valor = estadoOriginal != null ? estadoOriginal.trim().toUpperCase() : "";
        if ("OPERATIVO".equals(valor) || "ACTIVO".equals(valor)) {
            return "OPERATIVO";
        }
        if ("NO OPERATIVO".equals(valor) || "INACTIVO".equals(valor) || "DAÑADO".equals(valor) || "DANADO".equals(valor)) {
            return "NO OPERATIVO";
        }
        if ("REPORTADO PARA DAR DE BAJA".equals(valor) || "REPORTADO PARA BAJA".equals(valor) || "BAJA".equals(valor)) {
            return "REPORTADO PARA DAR DE BAJA";
        }
        return "NO OPERATIVO";
    }

    private String itemProceso(ResultSet rs) throws Exception {
        return coalesce(valor(rs, "pc_procesador"), valor(rs, "laptop_procesador"));
    }

    private String itemMemoria(ResultSet rs) throws Exception {
        return coalesce(valor(rs, "pc_ram"), valor(rs, "laptop_ram"));
    }

    private String itemDisco(ResultSet rs) throws Exception {
        return coalesce(valor(rs, "pc_disco_duro"), valor(rs, "laptop_disco_duro"));
    }

    private String itemSistema(ResultSet rs) throws Exception {
        return coalesce(valor(rs, "pc_so"), valor(rs, "laptop_so"));
    }

    private String valor(ResultSet rs, String columna) throws Exception {
        String valor = rs.getString(columna);
        return valor != null ? valor.trim() : "";
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    private String coalesce(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.trim().isEmpty()) {
                return valor.trim();
            }
        }
        return "";
    }

    private String unirNoVacios(String... valores) {
        List<String> partes = new ArrayList<>();
        for (String valor : valores) {
            agregarSiTieneTexto(partes, valor);
        }
        return String.join(" | ", partes);
    }

    private void agregarSiTieneTexto(List<String> partes, String valor) {
        if (valor != null && !valor.trim().isEmpty()) {
            partes.add(valor.trim());
        }
    }
}
