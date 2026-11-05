package com.mycompany.sistemainventariov3.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

        private static final String URL = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/inventario_dtic_2026?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true");
        private static final String USER = System.getenv().getOrDefault("DB_USER", "TU_USUARIO");
        private static final String PASSWORD = System.getenv().getOrDefault("DB_PASS", "TU_CLAVE");


    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error cargando driver MySQL: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static JsonObject getDatabaseStatus() {
        JsonObject status = new JsonObject();

        try (Connection conn = getConnection()) {
            status.addProperty("conexion", "OK");
            status.addProperty("base_datos", "inventario_dtic_2026");
            status.addProperty("modelo", "equipo + tablas hijas");
            status.addProperty("custodios", countRecords(conn, "custodio"));
            status.addProperty("ubicaciones", countRecords(conn, "ubicacion"));
            status.addProperty("equipos", countRecords(conn, "equipo"));
            status.addProperty("pcs", countByType(conn, "pc"));
            status.addProperty("laptops", countByType(conn, "laptop"));
            status.addProperty("perifericos", countByType(conn, "periferico"));
            status.addProperty("infraestructura", countByType(conn, "infraestructura"));
            status.addProperty("licencias", countByType(conn, "licencia"));
            status.addProperty("modems", countByType(conn, "modem"));
        } catch (SQLException e) {
            status.addProperty("conexion", "ERROR: " + e.getMessage());
        }

        return status;
    }

    public static JsonArray getCustodios() {
        JsonArray result = new JsonArray();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_custodio, nombre FROM custodio ORDER BY nombre LIMIT 20")) {

            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", rs.getInt("id_custodio"));
                obj.addProperty("nombre", rs.getString("nombre"));
                result.add(obj);
            }
        } catch (SQLException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            result.add(error);
        }

        return result;
    }

    public static JsonArray getPCs() {
        return getEquiposPorTipo("pc");
    }

    public static JsonArray getLaptops() {
        return getEquiposPorTipo("laptop");
    }

    private static JsonArray getEquiposPorTipo(String tipo) {
        JsonArray result = new JsonArray();
        String sql = "SELECT e.codigo_sbye, e.descripcion, e.marca, e.modelo, " +
                "COALESCE(pc.procesador, l.procesador) AS procesador, " +
                "COALESCE(pc.ram, l.ram) AS ram " +
                "FROM equipo e " +
                "LEFT JOIN pc ON pc.id_equipo = e.id_equipo " +
                "LEFT JOIN laptop l ON l.id_equipo = e.id_equipo " +
                "WHERE e.tipo_equipo = ? " +
                "ORDER BY e.id_equipo LIMIT 20";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("codigo", rs.getString("codigo_sbye"));
                    obj.addProperty("descripcion", rs.getString("descripcion"));
                    obj.addProperty("marca", rs.getString("marca"));
                    obj.addProperty("modelo", rs.getString("modelo"));
                    obj.addProperty("procesador", rs.getString("procesador"));
                    obj.addProperty("ram", rs.getString("ram"));
                    result.add(obj);
                }
            }
        } catch (SQLException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            result.add(error);
        }

        return result;
    }

    private static int countRecords(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Error contando registros en " + tableName + ": " + e.getMessage());
        }
        return 0;
    }

    private static int countByType(Connection conn, String tipo) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS total FROM equipo WHERE tipo_equipo = ?")) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error contando tipo " + tipo + ": " + e.getMessage());
        }
        return 0;
    }
}
