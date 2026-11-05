package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Ubicacion;
import javax.persistence.EntityManager;

/**
 * DAO para la entidad Ubicación
 */
public class UbicacionDAO extends DAOGenerico<Ubicacion, Integer> {
    
    public UbicacionDAO() {
        super(Ubicacion.class);
    }
    
    /**
     * Buscar ubicación por clave
     */
    public Ubicacion buscarPorClave(String clave) {
        EntityManager em = getEM();
        try {
            return (Ubicacion) em.createQuery("FROM Ubicacion WHERE direccion = :clave")
                    .setParameter("clave", clave)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    /**
     * Buscar ubicaciones por edificio
     */
    public java.util.List<Ubicacion> buscarPorEdificio(String edificio) {
        EntityManager em = getEM();
        return em.createQuery("FROM Ubicacion WHERE edificio = :edificio")
                .setParameter("edificio", edificio)
                .getResultList();
    }
}
