package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Infraestructura;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad Infraestructura
 */
public class InfraestructuraDAO extends DAOGenerico<Infraestructura, Integer> {
    
    public InfraestructuraDAO() {
        super(Infraestructura.class);
    }
    
    /**
     * Obtener todos los bienes de infraestructura
     */
    public List<Infraestructura> obtenerTodos() {
        EntityManager em = getEM();
        return em.createQuery("SELECT i FROM Infraestructura i", Infraestructura.class).getResultList();
    }
    
    /**
     * Buscar infraestructura por custodio
     */
    public List<Infraestructura> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<Infraestructura> query = em.createQuery(
                "SELECT i FROM Infraestructura i WHERE i.custodioActual.idCustodio = :idCustodio",
                Infraestructura.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar infraestructura por estado
     */
    public List<Infraestructura> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<Infraestructura> query = em.createQuery(
                "SELECT i FROM Infraestructura i WHERE i.estado = :estado",
                Infraestructura.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
}
