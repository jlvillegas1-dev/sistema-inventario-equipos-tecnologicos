package com.mycompany.sistemainventariov3.util;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Carga datos iniciales al iniciar la aplicación
 * Nota: Los usuarios ahora son quemados (hardcoded) en UsuarioService
 */
@Singleton
@Startup
public class DataLoader {
    
    @PostConstruct
    public void init() {
        System.out.println("===== INICIALIZANDO SISTEMA =====");
        System.out.println("✓ Sistema de Inventario SENADI iniciado");
        System.out.println("✓ Usuarios disponibles: admin/admin123 (ADMINISTRADOR), tecnico/tecnico123 (TECNICO)");
        System.out.println("===== INICIALIZACIÓN COMPLETADA ====");
    }
}
