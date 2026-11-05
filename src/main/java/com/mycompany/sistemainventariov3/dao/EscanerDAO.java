package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Escaner;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad Escaner
 */
public class EscanerDAO extends DAOGenerico<Escaner, Integer> {
    
    public EscanerDAO() {
        super(Escaner.class);
    }
    
    /**
     * Obtener todos los escáneres
     */
    public List<Escaner> obtenerTodos() {
        EntityManager em = getEM();
        return em.createQuery("SELECT e FROM Escaner e", Escaner.class).getResultList();
    }
    
    /**
     * Buscar escáneres por custodio
     */
    public List<Escaner> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<Escaner> query = em.createQuery(
                "SELECT e FROM Escaner e WHERE e.custodioActual.idCustodio = :idCustodio",
                Escaner.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar escáneres por estado
     */
    public List<Escaner> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<Escaner> query = em.createQuery(
                "SELECT e FROM Escaner e WHERE e.estado = :estado",
                Escaner.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
}
