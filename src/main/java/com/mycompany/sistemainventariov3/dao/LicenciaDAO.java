package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Licencia;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad Licencia
 */
public class LicenciaDAO extends DAOGenerico<Licencia, Integer> {
    
    public LicenciaDAO() {
        super(Licencia.class);
    }
    
    /**
     * Obtener todas las licencias
     */
    public List<Licencia> obtenerTodas() {
        EntityManager em = getEM();
        return em.createQuery("SELECT l FROM Licencia l", Licencia.class).getResultList();
    }
    
    /**
     * Buscar licencias por custodio
     */
    public List<Licencia> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<Licencia> query = em.createQuery(
                "SELECT l FROM Licencia l WHERE l.custodioActual.idCustodio = :idCustodio",
                Licencia.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar licencias por estado
     */
    public List<Licencia> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<Licencia> query = em.createQuery(
                "SELECT l FROM Licencia l WHERE l.estado = :estado",
                Licencia.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
    
    /**
     * Buscar licencias por descripción
     */
    public List<Licencia> obtenerPorDescripcion(String descripcion) {
        EntityManager em = getEM();
        TypedQuery<Licencia> query = em.createQuery(
                "SELECT l FROM Licencia l WHERE l.descripcion LIKE :descripcion",
                Licencia.class);
        query.setParameter("descripcion", "%" + descripcion + "%");
        return query.getResultList();
    }
}
