package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.model.ActaMantenimiento;
import com.mycompany.sistemainventariov3.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ActaMantenimientoService {

    public ActaMantenimientoService() {
        try {
            verificarTabla();
        } catch (SQLException e) {
            System.err.println("[ActaMantenimientoService] Error inicializando tabla: " + e.getMessage());
        }
    }

    private void verificarTabla() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS acta_mantenimiento (" +
                "id_acta INT AUTO_INCREMENT PRIMARY KEY," +
                "tipo_acta VARCHAR(50) NOT NULL," +
                "codigo_equipo VARCHAR(150) NULL," +
                "nombre_custodio VARCHAR(300) NOT NULL," +
                "id_custodio INT NULL," +
                "creado_por VARCHAR(150) NOT NULL," +
                "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "json_payload LONGTEXT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = DatabaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[ActaMantenimientoService] Tabla acta_mantenimiento verificada/creada.");
        }
    }

    public boolean guardarActa(ActaMantenimiento acta) {
        String sql = "INSERT INTO acta_mantenimiento (tipo_acta, codigo_equipo, nombre_custodio, id_custodio, creado_por, json_payload) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, acta.getTipoActa());
            ps.setString(2, acta.getCodigoEquipo());
            ps.setString(3, acta.getNombreCustodio());
            if (acta.getIdCustodio() != null) {
                ps.setInt(4, acta.getIdCustodio());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, acta.getCreadoPor());
            ps.setString(6, acta.getJsonPayload());

            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("[ActaMantenimientoService] Error guardando acta: " + e.getMessage());
            return false;
        }
    }

    public List<ActaMantenimiento> listarActas(Usuario usuario) {
        List<ActaMantenimiento> list = new ArrayList<>();
        if (usuario == null) return list;

        String rol = usuario.getRol() == null ? "" : usuario.getRol().toUpperCase();
        boolean esCustodio = "CUSTODIO".equals(rol);

        String sql;
        if (esCustodio) {
            sql = "SELECT id_acta, tipo_acta, codigo_equipo, nombre_custodio, id_custodio, creado_por, fecha_creacion " +
                  "FROM acta_mantenimiento " +
                  "WHERE id_custodio = ? OR UPPER(nombre_custodio) = ? OR UPPER(nombre_custodio) LIKE ? " +
                  "ORDER BY id_acta DESC";
        } else {
            sql = "SELECT id_acta, tipo_acta, codigo_equipo, nombre_custodio, id_custodio, creado_por, fecha_creacion " +
                  "FROM acta_mantenimiento " +
                  "ORDER BY id_acta DESC";
        }

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (esCustodio) {
                int idCustodio = usuario.getIdCustodio() != null ? usuario.getIdCustodio() : -1;
                String nombreComp = usuario.getNombreCompleto() != null ? usuario.getNombreCompleto().trim().toUpperCase() : "";
                ps.setInt(1, idCustodio);
                ps.setString(2, nombreComp);
                ps.setString(3, "%" + nombreComp + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActaMantenimiento a = new ActaMantenimiento();
                    a.setIdActa(rs.getInt("id_acta"));
                    a.setTipoActa(rs.getString("tipo_acta"));
                    a.setCodigoEquipo(rs.getString("codigo_equipo"));
                    a.setNombreCustodio(rs.getString("nombre_custodio"));
                    a.setIdCustodio((Integer) rs.getObject("id_custodio"));
                    a.setCreadoPor(rs.getString("creado_por"));
                    a.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ActaMantenimientoService] Error listando actas: " + e.getMessage());
        }

        return list;
    }

    public ActaMantenimiento obtenerPorId(int idActa) {
        String sql = "SELECT id_acta, tipo_acta, codigo_equipo, nombre_custodio, id_custodio, creado_por, fecha_creacion, json_payload " +
                "FROM acta_mantenimiento WHERE id_acta = ?";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idActa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ActaMantenimiento a = new ActaMantenimiento();
                    a.setIdActa(rs.getInt("id_acta"));
                    a.setTipoActa(rs.getString("tipo_acta"));
                    a.setCodigoEquipo(rs.getString("codigo_equipo"));
                    a.setNombreCustodio(rs.getString("nombre_custodio"));
                    a.setIdCustodio((Integer) rs.getObject("id_custodio"));
                    a.setCreadoPor(rs.getString("creado_por"));
                    a.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
                    a.setJsonPayload(rs.getString("json_payload"));
                    return a;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ActaMantenimientoService] Error obteniendo acta por id: " + e.getMessage());
        }
        return null;
    }
}
