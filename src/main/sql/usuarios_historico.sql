-- Script para agregar tablas de autenticación e histórico
-- Ejecutar esto en la BD inventario_dtic_2026 después del script inicial

USE inventario_dtic_2026;

-- Tabla de Usuarios para autenticación
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(255) NULL,
    rol VARCHAR(50) NOT NULL, -- 'ADMINISTRADOR', 'TECNICO'
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO', -- 'ACTIVO', 'INACTIVO'
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_usuarios_username (username)
) ENGINE=InnoDB;

-- Insertar usuarios de prueba (contraseñas en texto para validación):
-- admin / admin123
-- tecnico / tecnico123
INSERT IGNORE INTO usuarios (username, password, nombre_completo, rol, estado) VALUES
('admin', MD5('admin123'), 'Administrador del Sistema', 'ADMINISTRADOR', 'ACTIVO'),
('tecnico', MD5('tecnico123'), 'Técnico del Sistema', 'TECNICO', 'ACTIVO');

-- Tabla de Histórico de Cambios
CREATE TABLE IF NOT EXISTS historico_cambios (
    id_cambio INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tabla_afectada VARCHAR(100) NOT NULL, -- 'pcs', 'laptops', 'perifericos', etc.
    id_bien VARCHAR(120) NOT NULL, -- codigo_sbai del bien
    campo VARCHAR(100) NOT NULL, -- Nombre del campo modificado
    valor_anterior LONGTEXT NULL,
    valor_nuevo LONGTEXT NULL,
    usuario VARCHAR(100) NOT NULL, -- username del usuario
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    motivo TEXT NULL, -- Razón de la modificación
    tipo_operacion VARCHAR(20) NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE'
    KEY idx_historico_bien (tabla_afectada, id_bien),
    KEY idx_historico_usuario (usuario),
    KEY idx_historico_fecha (fecha_cambio),
    KEY idx_historico_tabla (tabla_afectada)
) ENGINE=InnoDB;
