package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.dao.*;
import com.mycompany.sistemainventariov3.model.*;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Servicio para gestión de bienes (Genérico para todos los tipos)
 */
public class BienService {
    
    private PCDAO pcDAO;
    private LaptopDAO laptopDAO;
    private TelefonoDAO telefonoDAO;
    private EscanerDAO escanerDAO;
    private ImpresoraDAO impresoraDAO;
    private ProyectorDAO proyectorDAO;
    private InfraestructuraDAO infraestructuraDAO;
    private LicenciaDAO licenciaDAO;
    private PeriferícoDAO periferícoDAO;
    private BienControlAdministrativoDAO bienControlAdministrativoDAO;
    private HistoricoService historicoService;
    private CustodioDAO custodioDAO;
    private UbicacionDAO ubicacionDAO;
    
    public BienService() {
        this.pcDAO = new PCDAO();
        this.laptopDAO = new LaptopDAO();
        this.telefonoDAO = new TelefonoDAO();
        this.escanerDAO = new EscanerDAO();
        this.impresoraDAO = new ImpresoraDAO();
        this.proyectorDAO = new ProyectorDAO();
        this.infraestructuraDAO = new InfraestructuraDAO();
        this.licenciaDAO = new LicenciaDAO();
        this.periferícoDAO = new PeriferícoDAO();
        this.bienControlAdministrativoDAO = new BienControlAdministrativoDAO();
        this.historicoService = new HistoricoService();
        this.custodioDAO = new CustodioDAO();
        this.ubicacionDAO = new UbicacionDAO();
    }
    
    // ==================== PC ====================
    
    /**
     * Crear nuevo PC
     */
    public PC crearPC(PC pc) throws Exception {
        pc = pcDAO.guardar(pc);
        historicoService.registrarCambio("pcs", pc.getCodigoSbaiOriginal(), "REGISTRO", "", "Nuevo equipo registrado", "", "INSERT");
        return pc;
    }
    
    /**
     * Actualizar PC con histórico automático
     */
    public PC actualizarPC(PC pcActualizado, String motivo) throws Exception {
        registrarCambiosEntidad(null, pcActualizado, "pcs", motivo);
        PC pcActualizada = pcDAO.guardar(pcActualizado);
        return pcActualizada;
    }
    
    /**
     * Eliminar PC
     */
    public void eliminarPC(String codigoSbai) throws Exception {
        historicoService.registrarCambio("pcs", codigoSbai, "ELIMINACION", "", "", "", "DELETE");
    }
    
    /**
     * Listar todas las PCs
     */
    public List<PC> listarPCs() {
        return pcDAO.obtenerTodas();
    }

    /**
     * Obtener PC por codigo SBAI original.
     * Con el nuevo modelo puede haber varios items por codigo, por lo que
     * se devuelve el primer registro asociado.
     */
    public PC obtenerPC(String codigoSbai) {
        return pcDAO.buscarPorCodigoSbai(codigoSbai);
    }
    
    /**
     * Buscar PCs por estado
     */
    public List<PC> buscarPCsPorEstado(String estado) {
        return pcDAO.buscarPorEstado(estado);
    }

    /**
     * Buscar PCs por marca.
     */
    public List<PC> buscarPCsPorMarca(String marca) {
        return pcDAO.buscarPorMarca(marca);
    }
    
    // ==================== LAPTOP ====================
    
    /**
     * Crear nuevo Laptop
     */
    public Laptop crearLaptop(Laptop laptop) throws Exception {
        laptop = laptopDAO.guardar(laptop);
        historicoService.registrarCambio("laptops", laptop.getCodigoSbaiOriginal(), "REGISTRO", "", "Nuevo equipo registrado", "", "INSERT");
        return laptop;
    }
    
    /**
     * Actualizar Laptop con histórico automático
     */
    public Laptop actualizarLaptop(Laptop laptopActualizado, String motivo) throws Exception {
        registrarCambiosEntidad(null, laptopActualizado, "laptops", motivo);
        Laptop laptopActualizada = laptopDAO.guardar(laptopActualizado);
        return laptopActualizada;
    }
    
    /**
     * Eliminar Laptop
     */
    public void eliminarLaptop(String codigoSbai) throws Exception {
        historicoService.registrarCambio("laptops", codigoSbai, "ELIMINACION", "", "", "", "DELETE");
    }
    
    /**
     * Listar todas las Laptops
     */
    public List<Laptop> listarLaptops() {
        return laptopDAO.obtenerTodas();
    }

    /**
     * Obtener Laptop por codigo SBAI original.
     * Con el nuevo modelo puede haber varios items por codigo, por lo que
     * se devuelve el primer registro asociado.
     */
    public Laptop obtenerLaptop(String codigoSbai) {
        return laptopDAO.buscarPorCodigoSbai(codigoSbai);
    }
    
    /**
     * Buscar Laptops por estado
     */
    public List<Laptop> buscarLaptopsPorEstado(String estado) {
        return laptopDAO.buscarPorEstado(estado);
    }

    /**
     * Buscar Laptops por marca.
     */
    public List<Laptop> buscarLaptopsPorMarca(String marca) {
        return laptopDAO.buscarPorMarca(marca);
    }
    
    // ==================== TELÉFONOS ====================
    
    public Telefono crearTelefono(Telefono telefono) throws Exception {
        telefono = telefonoDAO.guardar(telefono);
        historicoService.registrarCambio("telefonos", telefono.getCodigoSbaiOriginal(), "REGISTRO", "", "Nuevo teléfono registrado", "", "INSERT");
        return telefono;
    }
    
    public List<Telefono> listarTelefonos() {
        return telefonoDAO.obtenerTodos();
    }
    
    public List<Telefono> buscarTelefonosPorEstado(String estado) {
        return telefonoDAO.obtenerPorEstado(estado);
    }
    
    public List<Telefono> buscarTelefonosPorCustodio(Integer idCustodio) {
        return telefonoDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== ESCÁNERES ====================
    
    public Escaner crearEscaner(Escaner escaner) throws Exception {
        escaner = escanerDAO.guardar(escaner);
        historicoService.registrarCambio("escaners", escaner.getCodigoSbaiOriginal(), "REGISTRO", "", "Nuevo escáner registrado", "", "INSERT");
        return escaner;
    }
    
    public List<Escaner> listarEscaners() {
        return escanerDAO.obtenerTodos();
    }
    
    public List<Escaner> buscarEscanersPorEstado(String estado) {
        return escanerDAO.obtenerPorEstado(estado);
    }
    
    public List<Escaner> buscarEscanersPorCustodio(Integer idCustodio) {
        return escanerDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== IMPRESORAS ====================
    
    public Impresora crearImpresora(Impresora impresora) throws Exception {
        impresora = impresoraDAO.guardar(impresora);
        historicoService.registrarCambio("impresoras", impresora.getCodigoSbaiOriginal(), "REGISTRO", "", "Nueva impresora registrada", "", "INSERT");
        return impresora;
    }
    
    public List<Impresora> listarImpresoras() {
        return impresoraDAO.obtenerTodas();
    }
    
    public List<Impresora> buscarImpresorasPorEstado(String estado) {
        return impresoraDAO.obtenerPorEstado(estado);
    }
    
    public List<Impresora> buscarImpresorasPorCustodio(Integer idCustodio) {
        return impresoraDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== PROYECTORES ====================
    
    public Proyector crearProyector(Proyector proyector) throws Exception {
        proyector = proyectorDAO.guardar(proyector);
        historicoService.registrarCambio("proyectores", proyector.getCodigoSbaiOriginal(), "REGISTRO", "", "Nuevo proyector registrado", "", "INSERT");
        return proyector;
    }
    
    public List<Proyector> listarProyectores() {
        return proyectorDAO.obtenerTodos();
    }
    
    public List<Proyector> buscarProyectoresPorEstado(String estado) {
        return proyectorDAO.obtenerPorEstado(estado);
    }
    
    public List<Proyector> buscarProyectoresPorCustodio(Integer idCustodio) {
        return proyectorDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== INFRAESTRUCTURA ====================
    
    public Infraestructura crearInfraestructura(Infraestructura infraestructura) throws Exception {
        infraestructura = infraestructuraDAO.guardar(infraestructura);
        historicoService.registrarCambio("infraestructura", infraestructura.getCodigoSbaiOriginal(), "REGISTRO", "", "Nueva infraestructura registrada", "", "INSERT");
        return infraestructura;
    }
    
    public List<Infraestructura> listarInfraestructura() {
        return infraestructuraDAO.obtenerTodos();
    }
    
    public List<Infraestructura> buscarInfraestructuraPorEstado(String estado) {
        return infraestructuraDAO.obtenerPorEstado(estado);
    }
    
    public List<Infraestructura> buscarInfraestructuraPorCustodio(Integer idCustodio) {
        return infraestructuraDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== LICENCIAS ====================
    
    public Licencia crearLicencia(Licencia licencia) throws Exception {
        licencia = licenciaDAO.guardar(licencia);
        historicoService.registrarCambio("licencias", licencia.getCodigoSbaiOriginal(), "REGISTRO", "", "Nueva licencia registrada", "", "INSERT");
        return licencia;
    }
    
    public List<Licencia> listarLicencias() {
        return licenciaDAO.obtenerTodas();
    }
    
    public List<Licencia> buscarLicenciasPorEstado(String estado) {
        return licenciaDAO.obtenerPorEstado(estado);
    }
    
    public List<Licencia> buscarLicenciasPorCustodio(Integer idCustodio) {
        return licenciaDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== PERIFÉRICOS ====================
    
    public Periferico crearPeriferico(Periferico periferico) throws Exception {
        periferico = periferícoDAO.guardar(periferico);
        historicoService.registrarCambio("perifericos", periferico.getCodigoSbaiOriginal(), "REGISTRO", "", "Nuevo periférico registrado", "", "INSERT");
        return periferico;
    }
    
    public List<Periferico> listarPerifericos() {
        return periferícoDAO.obtenerTodos();
    }
    
    public List<Periferico> buscarPerifericosPorEstado(String estado) {
        return periferícoDAO.obtenerPorEstado(estado);
    }
    
    public List<Periferico> buscarPerifericosPorCustodio(Integer idCustodio) {
        return periferícoDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== BIENES DE CONTROL ADMINISTRATIVO ====================
    
    public BienControlAdministrativo crearBienControlAdministrativo(BienControlAdministrativo bien) throws Exception {
        bien = bienControlAdministrativoDAO.guardar(bien);
        historicoService.registrarCambio("bienes_control_administrativo", bien.getCodigoSbaiOriginal(), "REGISTRO", "", "Nuevo bien de control registrado", "", "INSERT");
        return bien;
    }
    
    public List<BienControlAdministrativo> listarBienesControlAdministrativo() {
        return bienControlAdministrativoDAO.obtenerTodos();
    }
    
    public List<BienControlAdministrativo> buscarBienesControlAdministrativoPorEstado(String estado) {
        return bienControlAdministrativoDAO.obtenerPorEstado(estado);
    }
    
    public List<BienControlAdministrativo> buscarBienesControlAdministrativoPorCustodio(Integer idCustodio) {
        return bienControlAdministrativoDAO.obtenerPorCustodio(idCustodio);
    }
    
    // ==================== VALIDACIONES Y UTILIDADES ====================
    
    /**
     * Validar que los códigos Megan y SBAI sean únicos
     */
    private void validarCodigosUnicos(String codigoMegan, String codigoSbai, String tabla) throws Exception {
        if ("pcs".equals(tabla)) {
            if (codigoMegan != null && !codigoMegan.isEmpty() && pcDAO.buscarPorCodigoMegan(codigoMegan) != null) {
                throw new Exception("Ya existe un PC con el Código Megan: " + codigoMegan);
            }
            if (codigoSbai != null && !codigoSbai.isEmpty() && pcDAO.buscarPorCodigoSbai(codigoSbai) != null) {
                throw new Exception("Ya existe un PC con el Código SBAI: " + codigoSbai);
            }
        } else if ("laptops".equals(tabla)) {
            if (codigoMegan != null && !codigoMegan.isEmpty() && laptopDAO.buscarPorCodigoMegan(codigoMegan) != null) {
                throw new Exception("Ya existe una Laptop con el Código Megan: " + codigoMegan);
            }
            if (codigoSbai != null && !codigoSbai.isEmpty() && laptopDAO.buscarPorCodigoSbai(codigoSbai) != null) {
                throw new Exception("Ya existe una Laptop con el Código SBAI: " + codigoSbai);
            }
        }
    }
    
    /**
     * Registrar cambios entre dos versiones de una entidad
     */
    private void registrarCambiosEntidad(Object entidadAntigua, Object entidadNueva, String tabla, String motivo) {
        try {
            Field[] fields = entidadAntigua.getClass().getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object valorAntiguo = field.get(entidadAntigua);
                Object valorNuevo = field.get(entidadNueva);
                
                // Si los valores son diferentes, registrar el cambio
                if ((valorAntiguo == null && valorNuevo != null) ||
                    (valorAntiguo != null && !valorAntiguo.equals(valorNuevo))) {
                    
                    String nombreCampo = field.getName();
                    String valorAntiguoStr = valorAntiguo != null ? valorAntiguo.toString() : "";
                    String valorNuevoStr = valorNuevo != null ? valorNuevo.toString() : "";
                    
                    // Obtener el ID del bien
                    Field idField = entidadAntigua.getClass().getDeclaredField("codigoSbai");
                    idField.setAccessible(true);
                    String idBien = (String) idField.get(entidadAntigua);
                    
                    historicoService.registrarCambio(tabla, idBien, nombreCampo, valorAntiguoStr, valorNuevoStr, motivo, "UPDATE");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al registrar cambios: " + e.getMessage());
        }
    }
}
