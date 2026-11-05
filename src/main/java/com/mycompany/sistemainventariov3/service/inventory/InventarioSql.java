package com.mycompany.sistemainventariov3.service.inventory;

import java.util.Arrays;
import java.util.List;

/**
 * Constantes SQL y catálogos estáticos usados por el servicio JDBC de inventario.
 */
public final class InventarioSql {
    private InventarioSql() {
    }

    public static final List<String> TIPOS_FORMULARIO = Arrays.asList(
            "pc",
            "laptop",
            "periferico",
            "impresora",
            "escaner",
            "telefono",
            "proyector",
            "infraestructura",
            "licencia",
            "bien_control_admin",
            "modem"
    );

    public static final List<String> CAMPOS_COMUNES_EQUIPO = Arrays.asList(
            "codigo_megan",
            "codigo_sbye",
            "descripcion",
            "marca",
            "modelo",
            "sn",
            "fecha_ingreso",
            "estado",
            "observacion",
            "ultima_actualizacion",
            "ultimo_mantenimiento"
    );

    public static final String BASE_QUERY =
            "SELECT e.id_equipo, e.tipo_equipo, e.codigo_sbye, e.codigo_megan, e.descripcion, " +
            "e.marca, e.modelo, e.sn, e.estado, e.observacion, e.fecha_ingreso, e.ultima_actualizacion, e.ultimo_mantenimiento, " +
            "c.nombre AS custodio_nombre, " +
            "u.edificio, u.piso, u.direccion, " +
            "pc.procesador AS pc_procesador, pc.ram AS pc_ram, pc.disco_duro AS pc_disco_duro, pc.so AS pc_so, pc.ip AS pc_ip, " +
            "l.procesador AS laptop_procesador, l.ram AS laptop_ram, l.disco_duro AS laptop_disco_duro, l.so AS laptop_so, l.ip AS laptop_ip, " +
            "p.tipo_periferico, p.caracteristicas AS periferico_caracteristicas, " +
            "i.tipo_impresora, i.ip AS impresora_ip, i.caracteristicas AS impresora_caracteristicas, " +
            "es.caracteristicas AS escaner_caracteristicas, " +
            "t.caracteristicas AS telefono_caracteristicas, " +
            "pr.caracteristicas AS proyector_caracteristicas, pr.anotaciones AS proyector_anotaciones, " +
            "inf.subtipo AS infraestructura_subtipo, inf.caracteristicas AS infraestructura_caracteristicas, inf.anotaciones AS infraestructura_anotaciones, " +
            "li.caracteristicas AS licencia_caracteristicas, li.anotaciones AS licencia_anotaciones, li.id_computador AS licencia_id_computador, " +
            "m.numero_contrato AS modem_numero_contrato, m.numero_servicio AS modem_numero_servicio, " +
            "m.plan_comercial AS modem_plan_comercial, m.estado_servicio AS modem_estado_servicio, m.megas AS modem_megas, " +
            "m.acreditacion AS modem_acreditacion, m.caracteristicas AS modem_caracteristicas, m.anotaciones AS modem_anotaciones, " +
            "bca.codigo_anterior " +
            "FROM equipo e " +
            "LEFT JOIN custodio c ON c.id_custodio = e.id_custodio_actual " +
            "LEFT JOIN ubicacion u ON u.id_ubicacion = e.id_ubicacion " +
            "LEFT JOIN pc pc ON pc.id_equipo = e.id_equipo " +
            "LEFT JOIN laptop l ON l.id_equipo = e.id_equipo " +
            "LEFT JOIN periferico p ON p.id_equipo = e.id_equipo " +
            "LEFT JOIN impresora i ON i.id_equipo = e.id_equipo " +
            "LEFT JOIN escaner es ON es.id_equipo = e.id_equipo " +
            "LEFT JOIN telefono t ON t.id_equipo = e.id_equipo " +
            "LEFT JOIN proyector pr ON pr.id_equipo = e.id_equipo " +
            "LEFT JOIN infraestructura inf ON inf.id_equipo = e.id_equipo " +
            "LEFT JOIN licencia li ON li.id_equipo = e.id_equipo " +
            "LEFT JOIN modem m ON m.id_equipo = e.id_equipo " +
            "LEFT JOIN bien_control_admin bca ON bca.id_equipo = e.id_equipo ";

    public static final String INVENTARIO_TYPES_FILTER =
            "WHERE e.tipo_equipo IN ('pc','laptop','periferico','impresora','escaner','telefono','proyector','infraestructura','licencia','bien_control_admin','modem') ";

    public static final String ORDER_BY_ID_EQUIPO = "ORDER BY e.id_equipo ASC";
}
