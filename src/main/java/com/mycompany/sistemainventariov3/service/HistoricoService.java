package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.dao.HistoricoCambioDAO;
import com.mycompany.sistemainventariov3.model.HistoricoCambio;
import com.mycompany.sistemainventariov3.util.SesionUsuario;

import java.util.Date;
import java.util.List;

/**
 * Servicio para gestión del histórico de cambios
 */
public class HistoricoService {
    
    private HistoricoCambioDAO historicoCambioDAO;
    
    public HistoricoService() {
        this.historicoCambioDAO = new HistoricoCambioDAO();
    }
    
    /**
     * Registrar un cambio en el histórico
     */
    public void registrarCambio(String tablaAfectada, String idBien, String campo, 
                                String valorAnterior, String valorNuevo, String motivo, String tipoOperacion) {
        HistoricoCambio cambio = new HistoricoCambio();
        cambio.setTablaAfectada(tablaAfectada);
        cambio.setIdBien(idBien);
        cambio.setCampo(campo);
        cambio.setValorAnterior(valorAnterior);
        cambio.setValorNuevo(valorNuevo);
        cambio.setUsuario(SesionUsuario.getNombreUsuarioActual());
        cambio.setFechaCambio(new Date());
        cambio.setMotivo(motivo);
        cambio.setTipoOperacion(tipoOperacion);
        
        historicoCambioDAO.guardar(cambio);
    }
    
    /**
     * Obtener histórico de un bien
     */
    public List<HistoricoCambio> obtenerHistoricoBien(String tablaAfectada, String idBien) {
        return historicoCambioDAO.buscarPorBien(tablaAfectada, idBien);
    }
    
    /**
     * Obtener histórico de cambios hechos por un usuario
     */
    public List<HistoricoCambio> obtenerHistoricoUsuario(String usuario) {
        return historicoCambioDAO.buscarPorUsuario(usuario);
    }
    
    /**
     * Obtener histórico de una tabla
     */
    public List<HistoricoCambio> obtenerHistoricoTabla(String tablaAfectada) {
        return historicoCambioDAO.buscarPorTabla(tablaAfectada);
    }
    
    /**
     * Obtener cambios de un campo específico
     */
    public List<HistoricoCambio> obtenerCambiosCampo(String tablaAfectada, String idBien, String campo) {
        return historicoCambioDAO.buscarPorCampo(tablaAfectada, idBien, campo);
    }
}
