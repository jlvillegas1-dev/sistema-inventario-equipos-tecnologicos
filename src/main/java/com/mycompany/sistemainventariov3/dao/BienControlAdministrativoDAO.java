package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.BienControlAdministrativo;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad BienControlAdministrativo
 */
public class BienControlAdministrativoDAO extends DAOGenerico<BienControlAdministrativo, Integer> {
    
    public BienControlAdministrativoDAO() {
        super(BienControlAdministrativo.class);
    }
    
    /**
     * Obtener todos los bienes de control administrativo
     */
    public List<BienControlAdministrativo> obtenerTodos() {
        EntityManager em = getEM();
        return em.createQuery("SELECT b FROM BienControlAdministrativo b", BienControlAdministrativo.class).getResultList();
    }
    
    /**
     * Buscar bienes por custodio
     */
    public List<BienControlAdministrativo> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<BienControlAdministrativo> query = em.createQuery(
                "SELECT b FROM BienControlAdministrativo b WHERE b.custodioActual.idCustodio = :idCustodio",
                BienControlAdministrativo.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar bienes por estado
     */
    public List<BienControlAdministrativo> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<BienControlAdministrativo> query = em.createQuery(
                "SELECT b FROM BienControlAdministrativo b WHERE b.estado = :estado",
                BienControlAdministrativo.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
    
    /**
     * Buscar bienes por ubicación
     */
    public List<BienControlAdministrativo> obtenerPorUbicacion(Integer idUbicacion) {
        EntityManager em = getEM();
        TypedQuery<BienControlAdministrativo> query = em.createQuery(
                "SELECT b FROM BienControlAdministrativo b WHERE b.ubicacion.idUbicacion = :idUbicacion",
                BienControlAdministrativo.class);
        query.setParameter("idUbicacion", idUbicacion);
        return query.getResultList();
    }
}
