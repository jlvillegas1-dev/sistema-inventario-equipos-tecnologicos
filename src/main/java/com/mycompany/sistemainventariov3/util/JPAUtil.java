package com.mycompany.sistemainventariov3.util;

import javax.persistence.*;

/**
 * Utilidad para gestionar EntityManager y EntityManagerFactory
 */
public class JPAUtil {
    private static final EntityManagerFactory emf;
    
    static {
        try {
            emf = Persistence.createEntityManagerFactory("my_persistence_unit");
        } catch (Throwable ex) {
            System.err.println("EntityManagerFactory creation failed.");
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }
    
    /**
     * Obtiene un EntityManager
     */
    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
    
    /**
     * Cierra el EntityManagerFactory (llamar al shutting down de la app)
     */
    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
