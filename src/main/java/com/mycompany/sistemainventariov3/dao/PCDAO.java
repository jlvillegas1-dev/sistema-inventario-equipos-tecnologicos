package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.PC;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * DAO para la entidad PC
 */
public class PCDAO extends DAOGenerico<PC, Integer> {
    
    public PCDAO() {
        super(PC.class);
    }
    
    /**
     * Buscar por Código MEGAN
     */
    public PC buscarPorCodigoMegan(String codigoMegan) {
        EntityManager em = getEM();
        try {
            return (PC) em.createQuery("FROM PC WHERE codigoMegan = :codigo")
                    .setParameter("codigo", codigoMegan)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    /**
     * Buscar por Código SBAI
     */
    public PC buscarPorCodigoSbai(String codigoSbai) {
        EntityManager em = getEM();
        try {
            return (PC) em.createQuery("FROM PC WHERE codigoSbaiOriginal = :codigo ORDER BY idEquipo ASC")
                    .setParameter("codigo", codigoSbai)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    /**
     * Buscar PCs por estado
     */
    public List<PC> buscarPorEstado(String estado) {
        EntityManager em = getEM();
        return em.createQuery("FROM PC WHERE estado = :estado")
                .setParameter("estado", estado)
                .getResultList();
    }
    
    /**
     * Buscar PCs por custodio
     */
    public List<PC> buscarPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        return em.createQuery("FROM PC WHERE custodioActual.idCustodio = :id")
                .setParameter("id", idCustodio)
                .getResultList();
    }
    
    /**
     * Buscar PCs por marca
     */
    public List<PC> buscarPorMarca(String marca) {
        EntityManager em = getEM();
        return em.createQuery("FROM PC WHERE marca LIKE :marca")
                .setParameter("marca", "%" + marca + "%")
                .getResultList();
    }
}
