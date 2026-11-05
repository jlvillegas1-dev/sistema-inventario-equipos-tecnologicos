package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Proyector;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad Proyector
 */
public class ProyectorDAO extends DAOGenerico<Proyector, Integer> {
    
    public ProyectorDAO() {
        super(Proyector.class);
    }
    
    /**
     * Obtener todos los proyectores
     */
    public List<Proyector> obtenerTodos() {
        EntityManager em = getEM();
        return em.createQuery("SELECT p FROM Proyector p", Proyector.class).getResultList();
    }
    
    /**
     * Buscar proyectores por custodio actual
     */
    public List<Proyector> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<Proyector> query = em.createQuery(
                "SELECT p FROM Proyector p WHERE p.custodioActual.idCustodio = :idCustodio",
                Proyector.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar proyectores por estado
     */
    public List<Proyector> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<Proyector> query = em.createQuery(
                "SELECT p FROM Proyector p WHERE p.estado = :estado",
                Proyector.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
    
    /**
     * Buscar proyectores por ubicación
     */
    public List<Proyector> obtenerPorUbicacion(Integer idUbicacion) {
        EntityManager em = getEM();
        TypedQuery<Proyector> query = em.createQuery(
                "SELECT p FROM Proyector p WHERE p.ubicacion.idUbicacion = :idUbicacion",
                Proyector.class);
        query.setParameter("idUbicacion", idUbicacion);
        return query.getResultList();
    }
}
