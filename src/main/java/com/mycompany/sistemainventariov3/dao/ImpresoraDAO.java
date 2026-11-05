package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.Impresora;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * DAO para la entidad Impresora
 */
public class ImpresoraDAO extends DAOGenerico<Impresora, Integer> {
    
    public ImpresoraDAO() {
        super(Impresora.class);
    }
    
    /**
     * Obtener todas las impresoras
     */
    public List<Impresora> obtenerTodas() {
        EntityManager em = getEM();
        return em.createQuery("SELECT i FROM Impresora i", Impresora.class).getResultList();
    }
    
    /**
     * Buscar impresoras por custodio
     */
    public List<Impresora> obtenerPorCustodio(Integer idCustodio) {
        EntityManager em = getEM();
        TypedQuery<Impresora> query = em.createQuery(
                "SELECT i FROM Impresora i WHERE i.custodioActual.idCustodio = :idCustodio",
                Impresora.class);
        query.setParameter("idCustodio", idCustodio);
        return query.getResultList();
    }
    
    /**
     * Buscar impresoras por estado
     */
    public List<Impresora> obtenerPorEstado(String estado) {
        EntityManager em = getEM();
        TypedQuery<Impresora> query = em.createQuery(
                "SELECT i FROM Impresora i WHERE i.estado = :estado",
                Impresora.class);
        query.setParameter("estado", estado);
        return query.getResultList();
    }
    
    /**
     * Buscar impresoras por IP
     */
    public Impresora obtenerPorIp(String ip) {
        EntityManager em = getEM();
        TypedQuery<Impresora> query = em.createQuery(
                "SELECT i FROM Impresora i WHERE i.ip = :ip",
                Impresora.class);
        query.setParameter("ip", ip);
        List<Impresora> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
