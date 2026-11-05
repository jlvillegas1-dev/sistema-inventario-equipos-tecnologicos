package com.mycompany.sistemainventariov3.dao;

import com.mycompany.sistemainventariov3.model.HistoricoCambio;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * DAO para la entidad HistóricoCambio
 */
public class HistoricoCambioDAO extends DAOGenerico<HistoricoCambio, Integer> {
    
    public HistoricoCambioDAO() {
        super(HistoricoCambio.class);
    }
    
    /**
     * Obtener histórico de cambios de un bien
     */
    public List<HistoricoCambio> buscarPorBien(String tablaAfectada, String idBien) {
        EntityManager em = getEM();
        return em.createQuery("FROM HistoricoCambio WHERE tablaAfectada = :tabla AND idBien = :id ORDER BY fechaCambio DESC")
                .setParameter("tabla", tablaAfectada)
                .setParameter("id", idBien)
                .getResultList();
    }
    
    /**
     * Obtener histórico de cambios hechos por un usuario
     */
    public List<HistoricoCambio> buscarPorUsuario(String usuario) {
        EntityManager em = getEM();
        return em.createQuery("FROM HistoricoCambio WHERE usuario = :user ORDER BY fechaCambio DESC")
                .setParameter("user", usuario)
                .getResultList();
    }
    
    /**
     * Obtener histórico de una tabla específica
     */
    public List<HistoricoCambio> buscarPorTabla(String tablaAfectada) {
        EntityManager em = getEM();
        return em.createQuery("FROM HistoricoCambio WHERE tablaAfectada = :tabla ORDER BY fechaCambio DESC")
                .setParameter("tabla", tablaAfectada)
                .getResultList();
    }
    
    /**
     * Obtener cambios de un campo específico
     */
    public List<HistoricoCambio> buscarPorCampo(String tablaAfectada, String idBien, String campo) {
        EntityManager em = getEM();
        return em.createQuery("FROM HistoricoCambio WHERE tablaAfectada = :tabla AND idBien = :id AND campo = :campo ORDER BY fechaCambio DESC")
                .setParameter("tabla", tablaAfectada)
                .setParameter("id", idBien)
                .setParameter("campo", campo)
                .getResultList();
    }
}
