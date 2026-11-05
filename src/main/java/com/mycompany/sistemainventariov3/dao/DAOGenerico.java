package com.mycompany.sistemainventariov3.dao;

import javax.persistence.*;
import java.util.List;

/**
 * DAO Genérico para operaciones CRUD básicas
 */
public abstract class DAOGenerico<T, ID> {
    
    protected Class<T> claseEntidad;
    protected EntityManager em;
    
    public DAOGenerico(Class<T> claseEntidad) {
        this.claseEntidad = claseEntidad;
    }
    
    /**
     * Obtener EntityManager
     */
    protected EntityManager getEM() {
        if (em == null || !em.isOpen()) {
            em = com.mycompany.sistemainventariov3.util.JPAUtil.getEntityManager();
        }
        return em;
    }
    
    /**
     * Crear o actualizar una entidad
     */
    public T guardar(T entidad) {
        EntityManager em = getEM();
        try {
            em.getTransaction().begin();
            if (em.contains(entidad)) {
                entidad = em.merge(entidad);
            } else {
                em.persist(entidad);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
        return entidad;
    }
    
    /**
     * Buscar por ID
     */
    public T buscarPorId(ID id) {
        EntityManager em = getEM();
        return em.find(claseEntidad, id);
    }
    
    /**
     * Obtener todas las entidades
     */
    public List<T> obtenerTodas() {
        EntityManager em = getEM();
        return em.createQuery("FROM " + claseEntidad.getSimpleName())
                .getResultList();
    }
    
    /**
     * Eliminar una entidad
     */
    public void eliminar(T entidad) {
        EntityManager em = getEM();
        try {
            em.getTransaction().begin();
            if (!em.contains(entidad)) {
                entidad = em.merge(entidad);
            }
            em.remove(entidad);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }
    
    /**
     * Ejecutar query personalizado
     */
    protected List<T> ejecutarQuery(String jpql) {
        EntityManager em = getEM();
        return em.createQuery(jpql).getResultList();
    }
    
    /**
     * Cerrar EntityManager
     */
    public void cerrar() {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }
}
