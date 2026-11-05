package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Custodio;
import javax.persistence.EntityManager;

/**
 * DAO para la entidad Custodio
 */
public class CustodioDAO extends DAOGenerico<Custodio, Integer> {
    
    public CustodioDAO() {
        super(Custodio.class);
    }
    
    /**
     * Buscar custodio por nombre
     */
    public Custodio buscarPorNombre(String nombre) {
        EntityManager em = getEM();
        try {
            return (Custodio) em.createQuery("FROM Custodio WHERE nombre = :nombre")
                    .setParameter("nombre", nombre)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
}
