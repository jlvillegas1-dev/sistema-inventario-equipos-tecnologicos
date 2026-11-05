package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Periferico;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad Periferico
 */
public class PeriferícoDAO extends DAOGenerico<Periferico, Integer> {
    
    public PeriferícoDAO() {
        super(Periferico.class);
    }
    
    /**
     * Obtener todos los periféricos
     */
    public List<Periferico> obtenerTodos() {
        EntityManager em = getEM();
        return em.createQuery("SELECT p FROM Periferico p", Periferico.class).getResultList();
    }
    
    /**
     * Buscar periféricos por custodio
     */
    public List<Periferico> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<Periferico> query = em.createQuery(
                "SELECT p FROM Periferico p WHERE p.custodioActual.idCustodio = :idCustodio",
                Periferico.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar periféricos por estado
     */
    public List<Periferico> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<Periferico> query = em.createQuery(
                "SELECT p FROM Periferico p WHERE p.estado = :estado",
                Periferico.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
    
    /**
     * Buscar periféricos por tipo (descripción)
     */
    public List<Periferico> obtenerPorDescripcion(String descripcion) {
        EntityManager em = getEM();
        TypedQuery<Periferico> query = em.createQuery(
                "SELECT p FROM Periferico p WHERE p.descripcion LIKE :descripcion",
                Periferico.class);
        query.setParameter("descripcion", "%" + descripcion + "%");
        return query.getResultList();
    }
}
