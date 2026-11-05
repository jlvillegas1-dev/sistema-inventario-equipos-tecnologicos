package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Laptop;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * DAO para la entidad Laptop
 */
public class LaptopDAO extends DAOGenerico<Laptop, Integer> {
    
    public LaptopDAO() {
        super(Laptop.class);
    }
    
    /**
     * Buscar por Código MEGAN
     */
    public Laptop buscarPorCodigoMegan(String codigoMegan) {
        EntityManager em = getEM();
        try {
            return (Laptop) em.createQuery("FROM Laptop WHERE codigoMegan = :codigo")
                    .setParameter("codigo", codigoMegan)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    /**
     * Buscar por Código SBAI
     */
    public Laptop buscarPorCodigoSbai(String codigoSbai) {
        EntityManager em = getEM();
        try {
            return (Laptop) em.createQuery("FROM Laptop WHERE codigoSbaiOriginal = :codigo ORDER BY idEquipo ASC")
                    .setParameter("codigo", codigoSbai)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    /**
     * Buscar Laptops por estado
     */
    public List<Laptop> buscarPorEstado(String estado) {
        EntityManager em = getEM();
        return em.createQuery("FROM Laptop WHERE estado = :estado")
                .setParameter("estado", estado)
                .getResultList();
    }
    
    /**
     * Buscar Laptops por custodio
     */
    public List<Laptop> buscarPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        return em.createQuery("FROM Laptop WHERE custodioActual.idCustodio = :id")
                .setParameter("id", idCustodio)
                .getResultList();
    }
    
    /**
     * Buscar Laptops por marca
     */
    public List<Laptop> buscarPorMarca(String marca) {
        EntityManager em = getEM();
        return em.createQuery("FROM Laptop WHERE marca LIKE :marca")
                .setParameter("marca", "%" + marca + "%")
                .getResultList();
    }
}
