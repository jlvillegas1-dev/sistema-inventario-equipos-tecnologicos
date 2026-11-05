package com.mycompany.sistemainventariov3;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * ARCHIVO DESACTIVADO - No usar como ejecutable.
 * Solo contiene utilidades de prueba de conexion para el modelo final.
 */
public class ConnectionTest {
    private static void mainDesactivado(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver MySQL cargado correctamente");

            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/inventario_dtic_2026?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    "root",
                    "admin"
            );

            System.out.println("CONEXION EXITOSA A LA BASE DE DATOS");
            System.out.println("================================================");

            Statement stmt = con.createStatement();

            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) as total FROM custodio");
            if (rs1.next()) {
                System.out.println("Total de custodios: " + rs1.getInt("total"));
            }

            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) as total FROM equipo WHERE tipo_equipo = 'pc'");
            if (rs2.next()) {
                System.out.println("Total de PCs: " + rs2.getInt("total"));
            }

            ResultSet rs3 = stmt.executeQuery("SELECT COUNT(*) as total FROM equipo WHERE tipo_equipo = 'laptop'");
            if (rs3.next()) {
                System.out.println("Total de laptops: " + rs3.getInt("total"));
            }

            ResultSet rs4 = stmt.executeQuery("SELECT COUNT(*) as total FROM ubicacion");
            if (rs4.next()) {
                System.out.println("Total de ubicaciones: " + rs4.getInt("total"));
            }

            System.out.println("\n================================================");
            System.out.println("PRIMEROS 5 CUSTODIOS:");
            System.out.println("================================================");

            ResultSet rs5 = stmt.executeQuery("SELECT id_custodio, nombre FROM custodio LIMIT 5");
            while (rs5.next()) {
                System.out.println(rs5.getInt("id_custodio") + " - " + rs5.getString("nombre"));
            }

            System.out.println("\n================================================");
            System.out.println("PRIMERAS 5 COMPUTADORAS:");
            System.out.println("================================================");

            ResultSet rs6 = stmt.executeQuery(
                    "SELECT e.codigo_sbye, e.descripcion, e.marca, e.modelo, pc.procesador, pc.ram " +
                    "FROM equipo e INNER JOIN pc ON pc.id_equipo = e.id_equipo LIMIT 5"
            );
            while (rs6.next()) {
                System.out.println("Codigo: " + rs6.getString("codigo_sbye"));
                System.out.println("  Marca: " + rs6.getString("marca"));
                System.out.println("  Modelo: " + rs6.getString("modelo"));
                System.out.println("  Procesador: " + rs6.getString("procesador"));
                System.out.println("  RAM: " + rs6.getString("ram"));
                System.out.println("---");
            }

            System.out.println("================================================");
            System.out.println("PRUEBAS COMPLETADAS");
            System.out.println("================================================");

            con.close();
        } catch (ClassNotFoundException e) {
            System.out.println("Error: Driver MySQL no encontrado");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error de conexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
