package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Telefono;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad Telefono
 */
public class TelefonoDAO extends DAOGenerico<Telefono, Integer> {
    
    public TelefonoDAO() {
        super(Telefono.class);
    }
    
    /**
     * Obtener todos los teléfonos
     */
    public List<Telefono> obtenerTodos() {
        EntityManager em = getEM();
        return em.createQuery("SELECT t FROM Telefono t", Telefono.class).getResultList();
    }
    
    /**
     * Buscar teléfonos por custodio
     */
    public List<Telefono> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<Telefono> query = em.createQuery(
                "SELECT t FROM Telefono t WHERE t.custodioActual.idCustodio = :idCustodio",
                Telefono.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar teléfonos por estado
     */
    public List<Telefono> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<Telefono> query = em.createQuery(
                "SELECT t FROM Telefono t WHERE t.estado = :estado",
                Telefono.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
    
    /**
     * Buscar teléfonos por numero de serie
     */
    public Telefono obtenerPorNumeroSerie(String numeroSerie) {
        EntityManager em = getEM();
        TypedQuery<Telefono> query = em.createQuery(
                "SELECT t FROM Telefono t WHERE t.numeroSerie = :numeroSerie",
                Telefono.class);
        query.setParameter("numeroSerie", numeroSerie);
        List<Telefono> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
