package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.dto.ActaRedesComunicacionesRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;

import javax.xml.namespace.QName;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActaRedesComunicacionesDocumentService {

    private static final String NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final QName QGRID = new QName(NS, "tblGrid");
    private static final QName QTBLPR = new QName(NS, "tblPr");
    private static final QName QLAYOUT = new QName(NS, "tblLayout");

    private static final String TITULO_1 = "SERVICIO NACIONAL DE DERECHOS INTELECTUALES";
    private static final String TITULO_2 = "DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN";
    private static final String TITULO_3 = "FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS DE REDES Y COMUNICACIONES";
    private static final String CERTIFICACION_DEFAULT =
            "Certifico que los elementos detallados en el presente documento, me han sido instalados para mi cuidado y custodia " +
            "con el proposito de cumplir con las tareas y asignaciones propias de mi cargo en la Institucion, siendo estos de mi " +
            "unica y exclusiva responsabilidad. Me comprometo a usar correctamente los recursos, y solo para los fines establecidos, " +
            "a no instalar ni permitir la instalacion de software por personal ajeno al area de soporte de DTIC, dado cualquier " +
            "novedad dar conocimiento a los tecnicos de DTIC.";

    private static final String[] EQUIPO_TITULOS = {
            "CENTRAL TELEFONICA",
            "EQUIPOS CISCO",
            "CONTROLADORA WIFI Y ACCESS POINTS",
            "SERVIDORES",
            "SWITCHES"
    };

    private static final String[][] CENTRAL_ACTIVIDADES = {
            {"MONITOREO CONTINUO"},
            {"RESPALDO DE CONFIGURACIÓN"},
            {"ACTUALIZACIONES Y PARCHES"},
            {"PLAN DE CONTINUIDAD"}
    };

    private static final String[][] CISCO_ACTIVIDADES = {
            {"CONFIGURACIÓN ÓPTIMA"},
            {"GESTION DE TRAFICO"},
            {"MONITOREO AVANZADO"}
    };

    private static final String[][] WIFI_ACTIVIDADES = {
            {"OPTIMIZACIÓN DE COBERTURA"},
            {"GESTION DE USUARIOS"},
            {"BALANCEO DE CARGAS"}
    };

    private static final String[][] SERVIDOR_ACTIVIDADES = {
            {"SUPERVISIÓN Y MANTENIMIENTO"},
            {"GESTION DE RECURSOS"},
            {"SEGURIDAD"}
    };

    private static final String[][] SWITCH_ACTIVIDADES = {
            {"GESTIÓN DE REDES"},
            {"SUPERVISIÓN DE TRÁFICO"},
            {"SEGURIDAD"}
    };

    private static final int DX_HDR = 340;
    private static final int DX_DATA = 380;
    private static final int DX_ACT = 360;
    private static final int DX_SIGN = 1600;

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MARGIN = 40f;
    private static final float FOOTER_Y = 78f;
    private static final float H_HDR = 18f;
    private static final float H_ROW = 16f;
    private static final float H_SECTION = 20f;
    private static final float H_ACT = 24f;
    private static final float H_SIGN = 65f;

    public byte[] generarDocx(ActaRedesComunicacionesRequest request) throws IOException {
        ensureDefaults(request);
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            configurePage(doc);
            addFondoPagina(doc);

            addPageHeader(doc);
            addFuncionarioTable(doc, request);
            
            // Tabla de equipos (Central Telefonica, Cisco, APs, Servidores, Switches, o Manuales)
            boolean hasManualEquipos = request.getEquiposManuales() != null && !request.getEquiposManuales().isEmpty();
            boolean hasEquiposData = isEquipoRowNotEmpty(request.getCentralTelefonica()) ||
                                     isEquipoRowNotEmpty(request.getEquiposCisco()) ||
                                     isEquipoRowNotEmpty(request.getControladoraWifiAccessPoints()) ||
                                     isEquipoRowNotEmpty(request.getServidores()) ||
                                     isEquipoRowNotEmpty(request.getSwitches());
            if (hasManualEquipos) {
                doc.createParagraph().createRun().setText("");
                addEquiposManualesTable(doc, request.getEquiposManuales());
            } else if (hasEquiposData) {
                doc.createParagraph().createRun().setText("");
                addEquiposTable(doc, request);
            }

            // Tablas de Actividades condicionales
            if (request.getActividades() != null && !request.getActividades().isEmpty()) {
                if (hasManualEquipos) {
                    for (ActaRedesComunicacionesRequest.EquipoRow eq : request.getEquiposManuales()) {
                        String tipo = eq.getTipo();
                        if (tipo == null || tipo.trim().isEmpty()) continue;
                        List<ActaRedesComunicacionesRequest.ActividadMantenimiento> filteredActs = new ArrayList<>();
                        for (ActaRedesComunicacionesRequest.ActividadMantenimiento act : request.getActividades()) {
                            if (act != null && act.getTipoEquipo() != null && act.getTipoEquipo().trim().equalsIgnoreCase(tipo.trim())) {
                                filteredActs.add(act);
                            }
                        }
                        if (!filteredActs.isEmpty()) {
                            doc.createParagraph().createRun().setText("");
                            addActivitySection(doc, "ACTIVIDADES DE " + tipo.trim().toUpperCase(), filteredActs);
                        }
                    }
                } else {
                    // Segmentar actividades por tipo si existen (modo estático)
                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> centralActs = filterActivities(request.getActividades(), CENTRAL_ACTIVIDADES);
                    if (!centralActs.isEmpty()) {
                        doc.createParagraph().createRun().setText("");
                        addActivitySection(doc, "CENTRAL TELEFÓNICA", centralActs);
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> ciscoActs = filterActivities(request.getActividades(), CISCO_ACTIVIDADES);
                    if (!ciscoActs.isEmpty()) {
                        doc.createParagraph().createRun().setText("");
                        addActivitySection(doc, "EQUIPOS CISCO", ciscoActs);
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> wifiActs = filterActivities(request.getActividades(), WIFI_ACTIVIDADES);
                    if (!wifiActs.isEmpty()) {
                        doc.createParagraph().createRun().setText("");
                        addActivitySection(doc, "CONTROLADORA Wifi Y ACCESS POINTS", wifiActs);
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> servidorActs = filterActivities(request.getActividades(), SERVIDOR_ACTIVIDADES);
                    if (!servidorActs.isEmpty()) {
                        doc.createParagraph().createRun().setText("");
                        addActivitySection(doc, "SERVIDORES", servidorActs);
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> switchActs = filterActivities(request.getActividades(), SWITCH_ACTIVIDADES);
                    if (!switchActs.isEmpty()) {
                        doc.createParagraph().createRun().setText("");
                        addActivitySection(doc, "SWITCHES", switchActs);
                    }
                }
            }

            addCertificacion(doc, request);
            addEntregaRecepcionTable(doc, request);

            doc.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarPdf(ActaRedesComunicacionesRequest request) throws IOException {
        ensureDefaults(request);
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildPdfSinglePage(doc, request);
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void ensureDefaults(ActaRedesComunicacionesRequest request) {
        if (request.getFuncionario() == null) {
            request.setFuncionario(new ActaRedesComunicacionesRequest.Funcionario());
        }
        if (request.getCentralTelefonica() == null) request.setCentralTelefonica(new ActaRedesComunicacionesRequest.EquipoRow());
        if (request.getEquiposCisco() == null) request.setEquiposCisco(new ActaRedesComunicacionesRequest.EquipoRow());
        if (request.getControladoraWifiAccessPoints() == null) request.setControladoraWifiAccessPoints(new ActaRedesComunicacionesRequest.EquipoRow());
        if (request.getServidores() == null) request.setServidores(new ActaRedesComunicacionesRequest.EquipoRow());
        if (request.getSwitches() == null) request.setSwitches(new ActaRedesComunicacionesRequest.EquipoRow());
        if (request.getActividades() == null) request.setActividades(new ArrayList<>());
        if (request.getEntrega() == null) request.setEntrega(new ActaRedesComunicacionesRequest.FirmaRecepcion());
        if (request.getRecibe() == null) request.setRecibe(new ActaRedesComunicacionesRequest.FirmaRecepcion());
    }

    private void configurePage(XWPFDocument doc) {
        CTBody body = doc.getDocument().getBody();
        if (!body.isSetSectPr()) body.addNewSectPr();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sp = body.getSectPr();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz pgSz =
                sp.isSetPgSz() ? sp.getPgSz() : sp.addNewPgSz();
        pgSz.setW(java.math.BigInteger.valueOf(11906));
        pgSz.setH(java.math.BigInteger.valueOf(16838));
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar pgMar =
                sp.isSetPgMar() ? sp.getPgMar() : sp.addNewPgMar();
        pgMar.setTop(java.math.BigInteger.valueOf(1700)); // espacio del encabezado del fondo
        pgMar.setRight(java.math.BigInteger.valueOf(720));
        pgMar.setBottom(java.math.BigInteger.valueOf(2000)); // espacio del pie del fondo
        pgMar.setLeft(java.math.BigInteger.valueOf(720));
        pgMar.setFooter(java.math.BigInteger.valueOf(360));
    }

    private void addPageHeader(XWPFDocument doc) throws IOException {
        addCenteredParagraph(doc, TITULO_1, true, 13, false);
        addCenteredParagraph(doc, TITULO_2, true, 11, false);
        addCenteredParagraph(doc, TITULO_3, true, 12, true);
    }

    private void addFuncionarioTable(XWPFDocument doc, ActaRedesComunicacionesRequest req) {
        int[] grid = {1100, 3050, 1100, 2550, 1100, 1566};
        XWPFTable t = createStyledTable(doc, 3, 6, grid);
        setRowHeight(t.getRow(0), DX_HDR);
        setRowHeight(t.getRow(1), DX_DATA);
        setRowHeight(t.getRow(2), DX_DATA);
        mergeCellsH(t, 0, 0, 5);
        setCellText(t.getRow(0).getCell(0), "DATOS DEL FUNCIONARIO SENADI", true, ParagraphAlignment.CENTER, "D9D9D9");
        fillLabelValue(t.getRow(1), 0, "NOMBRE", req.getFuncionario().getNombre());
        fillLabelValue(t.getRow(1), 2, "CARGO", req.getFuncionario().getCargo());
        fillLabelValue(t.getRow(1), 4, "N° EXT.", req.getFuncionario().getExtension());
        fillLabelValue(t.getRow(2), 0, "CORREO", req.getFuncionario().getCorreo());
        fillLabelValue(t.getRow(2), 2, "ÁREA", req.getFuncionario().getArea());
        fillLabelValue(t.getRow(2), 4, "EDIFICIO", req.getFuncionario().getEdificio());
    }

    private void addEquiposTable(XWPFDocument doc, ActaRedesComunicacionesRequest req) {
        int[] grid = {2200, 2000, 2000, 2000, 2000};
        XWPFTable t = createStyledTable(doc, 7, 5, grid);
        setRowHeight(t.getRow(0), DX_HDR);
        setRowHeight(t.getRow(1), DX_DATA);
        for (int i = 2; i < 7; i++) setRowHeight(t.getRow(i), DX_DATA);
        mergeCellsH(t, 0, 0, 4);
        setCellText(t.getRow(0).getCell(0), "EQUIPOS", true, ParagraphAlignment.CENTER, "D9D9D9");
        String[] hdrs = {"TIPO", "MARCA", "MODELO", "SERIAL", "CODIGO"};
        for (int i = 0; i < hdrs.length; i++) {
            setCellText(t.getRow(1).getCell(i), hdrs[i], true, ParagraphAlignment.CENTER, "E6E6E6");
        }
        fillEquipoRow(t.getRow(2), EQUIPO_TITULOS[0], req.getCentralTelefonica());
        fillEquipoRow(t.getRow(3), EQUIPO_TITULOS[1], req.getEquiposCisco());
        fillEquipoRow(t.getRow(4), EQUIPO_TITULOS[2], req.getControladoraWifiAccessPoints());
        fillEquipoRow(t.getRow(5), EQUIPO_TITULOS[3], req.getServidores());
        fillEquipoRow(t.getRow(6), EQUIPO_TITULOS[4], req.getSwitches());
    }

    private void addEquiposManualesTable(XWPFDocument doc, List<ActaRedesComunicacionesRequest.EquipoRow> equipos) {
        int[] grid = {2200, 2000, 2000, 2000, 2000};
        XWPFTable t = createStyledTable(doc, 2 + equipos.size(), 5, grid);
        setRowHeight(t.getRow(0), DX_HDR);
        setRowHeight(t.getRow(1), DX_DATA);
        for (int i = 0; i < equipos.size(); i++) setRowHeight(t.getRow(i + 2), DX_DATA);
        mergeCellsH(t, 0, 0, 4);
        setCellText(t.getRow(0).getCell(0), "EQUIPOS", true, ParagraphAlignment.CENTER, "D9D9D9");
        String[] hdrs = {"TIPO", "MARCA", "MODELO", "SERIAL", "CODIGO"};
        for (int i = 0; i < hdrs.length; i++) {
            setCellText(t.getRow(1).getCell(i), hdrs[i], true, ParagraphAlignment.CENTER, "E6E6E6");
        }
        for (int i = 0; i < equipos.size(); i++) {
            fillEquipoRow(t.getRow(i + 2), n(equipos.get(i).getTipo()), equipos.get(i));
        }
    }

    private void addActivitySection(XWPFDocument doc, String title,
                                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> values) {
        int activityRows = 3;
        int totalRows = 3 + activityRows;
        int[] grid = {4200, 1700, 1400, 3166};
        XWPFTable t = createStyledTable(doc, totalRows, 4, grid);
        setRowHeight(t.getRow(0), 480);
        setRowHeight(t.getRow(1), 360);
        setRowHeight(t.getRow(2), 360);
        for (int i = 3; i < totalRows; i++) setRowHeight(t.getRow(i), 420);
        mergeCellsH(t, 0, 0, 3);
        setCellText(t.getRow(0).getCell(0), title, true, ParagraphAlignment.CENTER, "7F7F7F", "FFFFFF", 12);
        mergeCellsV(t, 1, 2, 0);
        setCellText(t.getRow(1).getCell(0), "ACTIVIDADES DE\nMANTENIMIENTOS", true, ParagraphAlignment.CENTER, "D9D9D9");
        mergeCellsH(t, 1, 1, 3);
        setCellText(t.getRow(1).getCell(1), "INSTALADO", true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(2).getCell(1), "FECHA", true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(2).getCell(2), "ESTADO", true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(2).getCell(3), "OBSERVACION", true, ParagraphAlignment.CENTER, "D9D9D9");

        List<ActaRedesComunicacionesRequest.ActividadMantenimiento> normalized = normalizeActivities(values, activityRows);
        for (int i = 0; i < activityRows; i++) {
            XWPFTableRow row = t.getRow(i + 3);
            ActaRedesComunicacionesRequest.ActividadMantenimiento actividad = normalized.get(i);
            setCellText(row.getCell(0), actividad.getActividad(), true, ParagraphAlignment.LEFT, "F2F2F2");
            setCellText(row.getCell(1), actividad.getFecha(), false, ParagraphAlignment.CENTER, "FFFFFF");
            setCellText(row.getCell(2), actividad.getEstado(), false, ParagraphAlignment.CENTER, "FFFFFF");
            setCellText(row.getCell(3), actividad.getObservacion(), false, ParagraphAlignment.LEFT, "FFFFFF");
        }
    }

    private void addCertificacion(XWPFDocument doc, ActaRedesComunicacionesRequest req) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.setSpacingBefore(70);
        p.setSpacingAfter(70);
        XWPFRun r = p.createRun();
        r.setFontSize(9);
        r.setText(normalize(req.getCertificacion().isEmpty() ? CERTIFICACION_DEFAULT : req.getCertificacion()));
    }

    private void addEntregaRecepcionTable(XWPFDocument doc, ActaRedesComunicacionesRequest req) {
        int[] grid = {5233, 5233};
        XWPFTable t = createStyledTable(doc, 5, 2, grid);
        setRowHeight(t.getRow(0), 380);
        setRowHeight(t.getRow(1), 340);
        setRowHeight(t.getRow(2), 420);
        setRowHeight(t.getRow(3), DX_SIGN);
        setRowHeight(t.getRow(4), 420);
        mergeCellsH(t, 0, 0, 1);
        setCellText(t.getRow(0).getCell(0), "ENTREGA RECEPCIÓN DEL EQUIPO", true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(1).getCell(0), "ENTREGA", false, ParagraphAlignment.CENTER, "EDEDED");
        setCellText(t.getRow(1).getCell(1), "RECIBE", false, ParagraphAlignment.CENTER, "EDEDED");
        fillEntregaCell(t.getRow(2).getCell(0), req.getEntrega().getNombre(), "Nombre:");
        fillEntregaCell(t.getRow(2).getCell(1), req.getRecibe().getNombre(), "Nombre:");
        fillEntregaCell(t.getRow(3).getCell(0), req.getEntrega().getFirma(), "Firma:");
        fillEntregaCell(t.getRow(3).getCell(1), req.getRecibe().getFirma(), "Firma:");
        fillEntregaCell(t.getRow(4).getCell(0), req.getEntrega().getFecha(), "Fecha:");
        fillEntregaCell(t.getRow(4).getCell(1), req.getRecibe().getFecha(), "Fecha:");
    }

    private void buildPdfSinglePage(PDDocument doc, ActaRedesComunicacionesRequest req) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            drawPdfHeader(doc, cs);
            float y = 706f;
            y = drawFuncionarioPdf(cs, MARGIN, y, req);
            
            // Dibujar equipos si al menos uno tiene datos o si es dinámico
            boolean hasManualEquipos = req.getEquiposManuales() != null && !req.getEquiposManuales().isEmpty();
            boolean hasEquiposData = isEquipoRowNotEmpty(req.getCentralTelefonica()) ||
                                     isEquipoRowNotEmpty(req.getEquiposCisco()) ||
                                     isEquipoRowNotEmpty(req.getControladoraWifiAccessPoints()) ||
                                     isEquipoRowNotEmpty(req.getServidores()) ||
                                     isEquipoRowNotEmpty(req.getSwitches());
            if (hasManualEquipos) {
                y -= 8f;
                y = drawEquiposManualesPdf(cs, MARGIN, y, req.getEquiposManuales());
            } else if (hasEquiposData) {
                y -= 8f;
                y = drawEquiposPdf(cs, MARGIN, y, req);
            }

            // Dibujar secciones de actividades condicionalmente
            if (req.getActividades() != null && !req.getActividades().isEmpty()) {
                if (hasManualEquipos) {
                    for (ActaRedesComunicacionesRequest.EquipoRow eq : req.getEquiposManuales()) {
                        String tipo = eq.getTipo();
                        if (tipo == null || tipo.trim().isEmpty()) continue;
                        List<ActaRedesComunicacionesRequest.ActividadMantenimiento> filteredActs = new ArrayList<>();
                        for (ActaRedesComunicacionesRequest.ActividadMantenimiento act : req.getActividades()) {
                            if (act != null && act.getTipoEquipo() != null && act.getTipoEquipo().trim().equalsIgnoreCase(tipo.trim())) {
                                filteredActs.add(act);
                            }
                        }
                        if (!filteredActs.isEmpty()) {
                            y -= 8f;
                            y = drawActivityManualesPdf(cs, MARGIN, y, "ACTIVIDADES DE " + tipo.trim().toUpperCase(), filteredActs);
                        }
                    }
                } else {
                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> centralActs = filterActivities(req.getActividades(), CENTRAL_ACTIVIDADES);
                    if (!centralActs.isEmpty()) {
                        y -= 8f;
                        y = drawActivitySectionPdf(cs, MARGIN, y, "CENTRAL TELEFÓNICA", CENTRAL_ACTIVIDADES, centralActs, centralActs.size());
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> ciscoActs = filterActivities(req.getActividades(), CISCO_ACTIVIDADES);
                    if (!ciscoActs.isEmpty()) {
                        y -= 8f;
                        y = drawActivitySectionPdf(cs, MARGIN, y, "EQUIPOS CISCO", CISCO_ACTIVIDADES, ciscoActs, ciscoActs.size());
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> wifiActs = filterActivities(req.getActividades(), WIFI_ACTIVIDADES);
                    if (!wifiActs.isEmpty()) {
                        y -= 8f;
                        y = drawActivitySectionPdf(cs, MARGIN, y, "CONTROLADORA Wifi Y ACCESS POINTS", WIFI_ACTIVIDADES, wifiActs, wifiActs.size());
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> servidorActs = filterActivities(req.getActividades(), SERVIDOR_ACTIVIDADES);
                    if (!servidorActs.isEmpty()) {
                        y -= 8f;
                        y = drawActivitySectionPdf(cs, MARGIN, y, "SERVIDORES", SERVIDOR_ACTIVIDADES, servidorActs, servidorActs.size());
                    }

                    List<ActaRedesComunicacionesRequest.ActividadMantenimiento> switchActs = filterActivities(req.getActividades(), SWITCH_ACTIVIDADES);
                    if (!switchActs.isEmpty()) {
                        y -= 8f;
                        y = drawActivitySectionPdf(cs, MARGIN, y, "SWITCHES", SWITCH_ACTIVIDADES, switchActs, switchActs.size());
                    }
                }
            }

            y -= 8f;
            String cert = normalize(req.getCertificacion().isEmpty() ? CERTIFICACION_DEFAULT : req.getCertificacion());
            float certH = 52f;
            drawFilledRect(cs, MARGIN, y - certH, PAGE_W - 2 * MARGIN, certH, new Color(245, 245, 245));
            drawBorder(cs, MARGIN, y - certH, PAGE_W - 2 * MARGIN, certH);
            drawParagraphInBox(cs, cert, MARGIN + 4f, y - 5f, PAGE_W - 2 * MARGIN - 8f, certH - 8f, 7.5f, false);
            y -= certH + 8f;
            drawSignaturePdf(cs, MARGIN, y, req);
        }
    }

    private void drawPdfHeader(PDDocument doc, PDPageContentStream cs) throws IOException {
        float logoY = PAGE_H - 28f;
        // Fondo de página (plantilla institucional)
        drawImage(doc, cs, "actas/Fondo_senadi.png", 0f, 0f, PAGE_W, PAGE_H);
        float y = logoY - 68f;
        y = drawCenteredText(cs, TITULO_1, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 12f, Color.BLACK);
        y -= 4f;
        y = drawCenteredText(cs, TITULO_2, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 10f, Color.BLACK);
        y -= 8f;
        drawCenteredText(cs, TITULO_3, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 11f, Color.BLACK, true);
    }

    private float drawFuncionarioPdf(PDPageContentStream cs, float x, float y, ActaRedesComunicacionesRequest req) throws IOException {
        float[] widths = {55f, 155f, 55f, 140f, 55f, PAGE_W - 2 * x - 460f};
        y = drawSectionHeader(cs, "DATOS DEL FUNCIONARIO SENADI", x, y, PAGE_W - 2 * x, H_HDR, new Color(217, 217, 217), Color.BLACK, 9f);
        y = drawKVRow(cs, x, y, widths, new String[]{"NOMBRE", n(req.getFuncionario().getNombre()), "CARGO", n(req.getFuncionario().getCargo()), "N EXT.", n(req.getFuncionario().getExtension())}, H_ROW);
        y = drawKVRow(cs, x, y, widths, new String[]{"CORREO", n(req.getFuncionario().getCorreo()), "AREA", n(req.getFuncionario().getArea()), "EDIFICIO", n(req.getFuncionario().getEdificio())}, H_ROW);
        return y;
    }

    private float drawEquiposPdf(PDPageContentStream cs, float x, float y, ActaRedesComunicacionesRequest req) throws IOException {
        float[] widths = {180f, 90f, 110f, 110f, 25f};
        y = drawSectionHeader(cs, "EQUIPOS", x, y, PAGE_W - 2 * x, H_HDR, new Color(217, 217, 217), Color.BLACK, 9f);
        y = drawRow(cs, x, y, widths, new String[]{"TIPO", "MARCA", "MODELO", "SERIAL", "CODIGO"}, H_ROW, true, new Color(230, 230, 230));
        y = drawRow(cs, x, y, widths, new String[]{n(req.getCentralTelefonica().getTipo()), n(req.getCentralTelefonica().getMarca()), n(req.getCentralTelefonica().getModelo()), n(req.getCentralTelefonica().getSerial()), n(req.getCentralTelefonica().getCodigo())}, H_ROW, false, Color.WHITE);
        y = drawRow(cs, x, y, widths, new String[]{n(req.getEquiposCisco().getTipo()), n(req.getEquiposCisco().getMarca()), n(req.getEquiposCisco().getModelo()), n(req.getEquiposCisco().getSerial()), n(req.getEquiposCisco().getCodigo())}, H_ROW, false, Color.WHITE);
        y = drawRow(cs, x, y, widths, new String[]{n(req.getControladoraWifiAccessPoints().getTipo()), n(req.getControladoraWifiAccessPoints().getMarca()), n(req.getControladoraWifiAccessPoints().getModelo()), n(req.getControladoraWifiAccessPoints().getSerial()), n(req.getControladoraWifiAccessPoints().getCodigo())}, H_ROW, false, Color.WHITE);
        y = drawRow(cs, x, y, widths, new String[]{n(req.getServidores().getTipo()), n(req.getServidores().getMarca()), n(req.getServidores().getModelo()), n(req.getServidores().getSerial()), n(req.getServidores().getCodigo())}, H_ROW, false, Color.WHITE);
        y = drawRow(cs, x, y, widths, new String[]{n(req.getSwitches().getTipo()), n(req.getSwitches().getMarca()), n(req.getSwitches().getModelo()), n(req.getSwitches().getSerial()), n(req.getSwitches().getCodigo())}, H_ROW, false, Color.WHITE);
        return y;
    }

    private float drawEquiposManualesPdf(PDPageContentStream cs, float x, float y, List<ActaRedesComunicacionesRequest.EquipoRow> equipos) throws IOException {
        float[] widths = {180f, 90f, 110f, 110f, PAGE_W - 2 * x - 490f};
        y = drawSectionHeader(cs, "EQUIPOS", x, y, PAGE_W - 2 * x, H_HDR, new Color(217, 217, 217), Color.BLACK, 9f);
        y = drawRow(cs, x, y, widths, new String[]{"TIPO", "MARCA", "MODELO", "SERIAL", "CODIGO"}, H_ROW, true, new Color(230, 230, 230));
        for (ActaRedesComunicacionesRequest.EquipoRow eq : equipos) {
            y = drawRow(cs, x, y, widths, new String[]{n(eq.getTipo()), n(eq.getMarca()), n(eq.getModelo()), n(eq.getSerial()), n(eq.getCodigo())}, H_ROW, false, Color.WHITE);
        }
        return y;
    }

    private float drawActivityManualesPdf(PDPageContentStream cs, float x, float y, String title,
                                         List<ActaRedesComunicacionesRequest.ActividadMantenimiento> values) throws IOException {
        float sectionW = PAGE_W - 2 * x;
        float[] widths = {200f, 80f, 80f, sectionW - 360f};
        y = drawDarkHeader(cs, title, x, y, sectionW, H_SECTION);
        int rows = values.size();
        float sectionH = 3 * H_ROW + rows * H_ACT;
        drawFilledRect(cs, x, y - sectionH, sectionW, sectionH, Color.WHITE);
        drawBorder(cs, x, y - sectionH, sectionW, sectionH);
        drawFilledRect(cs, x, y - H_ROW, 130f, H_ROW, new Color(217, 217, 217));
        drawBorder(cs, x, y - H_ROW, 130f, H_ROW);
        drawParagraphInBox(cs, "ACTIVIDADES DE MANTENIMIENTOS", x + 4f, y - 5f, 120f, H_ROW - 4f, 8f, true);
        drawFilledRect(cs, x + 130f, y - H_ROW, sectionW - 130f, H_ROW, new Color(217, 217, 217));
        drawBorder(cs, x + 130f, y - H_ROW, sectionW - 130f, H_ROW);
        drawText(cs, "INSTALADO", x + 130f + (sectionW - 130f) / 2f, y - 11f, PDType1Font.HELVETICA_BOLD, 8.5f, Color.BLACK, true);
        float headerY = y - H_ROW;
        drawFilledRect(cs, x + 130f, headerY - H_ROW, widths[1], H_ROW, new Color(217, 217, 217));
        drawFilledRect(cs, x + 130f + widths[1], headerY - H_ROW, widths[2], H_ROW, new Color(217, 217, 217));
        drawFilledRect(cs, x + 130f + widths[1] + widths[2], headerY - H_ROW, widths[3], H_ROW, new Color(217, 217, 217));
        drawBorder(cs, x + 130f, headerY - H_ROW, widths[1], H_ROW);
        drawBorder(cs, x + 130f + widths[1], headerY - H_ROW, widths[2], H_ROW);
        drawBorder(cs, x + 130f + widths[1] + widths[2], headerY - H_ROW, widths[3], H_ROW);
        drawText(cs, "FECHA", x + 130f + widths[1] + 4f, headerY - 11f, PDType1Font.HELVETICA_BOLD, 8f, Color.BLACK, false);
        drawText(cs, "ESTADO", x + 130f + widths[1] + widths[2] + 4f, headerY - 11f, PDType1Font.HELVETICA_BOLD, 8f, Color.BLACK, false);
        drawText(cs, "OBSERVACIÓN", x + 130f + widths[1] + widths[2] + 4f, headerY - 11f, PDType1Font.HELVETICA_BOLD, 8f, Color.BLACK, false);

        float rowY = y - 3 * H_ROW;
        for (int i = 0; i < rows; i++) {
            float currentY = rowY - i * H_ACT;
            drawFilledRect(cs, x, currentY - H_ACT, 130f, H_ACT, new Color(242, 242, 242));
            drawBorder(cs, x, currentY - H_ACT, 130f, H_ACT);
            drawParagraphInBox(cs, values.get(i).getActividad(), x + 4f, currentY - 5f, 122f, H_ACT - 6f, 7.5f, true);
            drawFilledRect(cs, x + 130f, currentY - H_ACT, widths[1], H_ACT, Color.WHITE);
            drawFilledRect(cs, x + 130f + widths[1], currentY - H_ACT, widths[2], H_ACT, Color.WHITE);
            drawFilledRect(cs, x + 130f + widths[1] + widths[2], currentY - H_ACT, widths[3], H_ACT, Color.WHITE);
            drawBorder(cs, x + 130f, currentY - H_ACT, widths[1], H_ACT);
            drawBorder(cs, x + 130f + widths[1], currentY - H_ACT, widths[2], H_ACT);
            drawBorder(cs, x + 130f + widths[1] + widths[2], currentY - H_ACT, widths[3], H_ACT);
            ActaRedesComunicacionesRequest.ActividadMantenimiento actividad = values.get(i);
            drawParagraphInBox(cs, actividad.getFecha(), x + 130f + 4f, currentY - 5f, widths[1] - 8f, H_ACT - 6f, 7.5f, false);
            drawParagraphInBox(cs, actividad.getEstado(), x + 130f + widths[1] + 4f, currentY - 5f, widths[2] - 8f, H_ACT - 6f, 7.5f, false);
            drawParagraphInBox(cs, actividad.getObservacion(), x + 130f + widths[1] + widths[2] + 4f, currentY - 5f, widths[3] - 8f, H_ACT - 6f, 7.5f, false);
        }
        return y - sectionH;
    }

    private float drawActivitySectionPdf(PDPageContentStream cs, float x, float y, String title,
                                         String[][] activities, List<ActaRedesComunicacionesRequest.ActividadMantenimiento> values,
                                         int rows) throws IOException {
        float sectionW = PAGE_W - 2 * x;
        float[] widths = {200f, 80f, 80f, sectionW - 360f};
        y = drawDarkHeader(cs, title, x, y, sectionW, H_SECTION);
        float sectionH = 3 * H_ROW + rows * H_ACT;
        drawFilledRect(cs, x, y - sectionH, sectionW, sectionH, Color.WHITE);
        drawBorder(cs, x, y - sectionH, sectionW, sectionH);
        drawFilledRect(cs, x, y - H_ROW, 130f, H_ROW, new Color(217, 217, 217));
        drawBorder(cs, x, y - H_ROW, 130f, H_ROW);
        drawParagraphInBox(cs, "ACTIVIDADES DE MANTENIMIENTOS", x + 4f, y - 5f, 120f, H_ROW - 4f, 8f, true);
        drawFilledRect(cs, x + 130f, y - H_ROW, sectionW - 130f, H_ROW, new Color(217, 217, 217));
        drawBorder(cs, x + 130f, y - H_ROW, sectionW - 130f, H_ROW);
        drawText(cs, "INSTALADO", x + 130f + (sectionW - 130f) / 2f, y - 11f, PDType1Font.HELVETICA_BOLD, 8.5f, Color.BLACK, true);
        float headerY = y - H_ROW;
        drawFilledRect(cs, x + 130f, headerY - H_ROW, widths[1], H_ROW, new Color(217, 217, 217));
        drawFilledRect(cs, x + 130f + widths[1], headerY - H_ROW, widths[2], H_ROW, new Color(217, 217, 217));
        drawFilledRect(cs, x + 130f + widths[1] + widths[2], headerY - H_ROW, widths[3], H_ROW, new Color(217, 217, 217));
        drawBorder(cs, x + 130f, headerY - H_ROW, widths[1], H_ROW);
        drawBorder(cs, x + 130f + widths[1], headerY - H_ROW, widths[2], H_ROW);
        drawBorder(cs, x + 130f + widths[1] + widths[2], headerY - H_ROW, widths[3], H_ROW);
        drawText(cs, "FECHA", x + 130f + widths[1] + 4f, headerY - 11f, PDType1Font.HELVETICA_BOLD, 8f, Color.BLACK, false);
        drawText(cs, "ESTADO", x + 130f + widths[1] + widths[2] + 4f, headerY - 11f, PDType1Font.HELVETICA_BOLD, 8f, Color.BLACK, false);
        drawText(cs, "OBSERVACIÓN", x + 130f + widths[1] + widths[2] + 4f, headerY - 11f, PDType1Font.HELVETICA_BOLD, 8f, Color.BLACK, false);

        List<ActaRedesComunicacionesRequest.ActividadMantenimiento> normalized = normalizeActivities(values, activities);
        float rowY = y - 3 * H_ROW;
        for (int i = 0; i < rows; i++) {
            float currentY = rowY - i * H_ACT;
            drawFilledRect(cs, x, currentY - H_ACT, 130f, H_ACT, new Color(242, 242, 242));
            drawBorder(cs, x, currentY - H_ACT, 130f, H_ACT);
            drawParagraphInBox(cs, activities[i][0], x + 4f, currentY - 5f, 122f, H_ACT - 6f, 7.5f, true);
            drawFilledRect(cs, x + 130f, currentY - H_ACT, widths[1], H_ACT, Color.WHITE);
            drawFilledRect(cs, x + 130f + widths[1], currentY - H_ACT, widths[2], H_ACT, Color.WHITE);
            drawFilledRect(cs, x + 130f + widths[1] + widths[2], currentY - H_ACT, widths[3], H_ACT, Color.WHITE);
            drawBorder(cs, x + 130f, currentY - H_ACT, widths[1], H_ACT);
            drawBorder(cs, x + 130f + widths[1], currentY - H_ACT, widths[2], H_ACT);
            drawBorder(cs, x + 130f + widths[1] + widths[2], currentY - H_ACT, widths[3], H_ACT);
            ActaRedesComunicacionesRequest.ActividadMantenimiento actividad = normalized.get(i);
            drawParagraphInBox(cs, actividad.getFecha(), x + 130f + 4f, currentY - 5f, widths[1] - 8f, H_ACT - 6f, 7.5f, false);
            drawParagraphInBox(cs, actividad.getEstado(), x + 130f + widths[1] + 4f, currentY - 5f, widths[2] - 8f, H_ACT - 6f, 7.5f, false);
            drawParagraphInBox(cs, actividad.getObservacion(), x + 130f + widths[1] + widths[2] + 4f, currentY - 5f, widths[3] - 8f, H_ACT - 6f, 7.5f, false);
        }
        return y - sectionH;
    }

    private void drawSignaturePdf(PDPageContentStream cs, float x, float y, ActaRedesComunicacionesRequest req) throws IOException {
        float half = (PAGE_W - 2 * x) / 2f;
        float[] cols = {half, half};
        y = drawSectionHeader(cs, "ENTREGA RECEPCIÓN DEL EQUIPO", x, y, PAGE_W - 2 * x, H_HDR, new Color(217, 217, 217), Color.BLACK, 9f);
        y = drawRow(cs, x, y, cols, new String[]{"ENTREGA", "RECIBE"}, 14f, false, new Color(237, 237, 237));
        y = drawPairRow(cs, x, y, cols, "Nombre:", n(req.getEntrega().getNombre()), n(req.getRecibe().getNombre()), H_ROW);
        y = drawPairRow(cs, x, y, cols, "Firma:", n(req.getEntrega().getFirma()), n(req.getRecibe().getFirma()), H_SIGN);
        drawPairRow(cs, x, y, cols, "Fecha:", n(req.getEntrega().getFecha()), n(req.getRecibe().getFecha()), H_ROW);
    }

    private float drawSectionHeader(PDPageContentStream cs, String txt, float x, float y,
                                    float w, float h, Color fill, Color tc, float fs) throws IOException {
        drawFilledRect(cs, x, y - h, w, h, fill);
        drawBorder(cs, x, y - h, w, h);
        drawText(cs, txt, x + w / 2f, y - h * 0.65f, PDType1Font.HELVETICA_BOLD, fs, tc, true);
        return y - h;
    }

    private float drawDarkHeader(PDPageContentStream cs, String txt, float x, float y,
                                 float w, float h) throws IOException {
        drawFilledRect(cs, x, y - h, w, h, new Color(46, 64, 87));
        drawBorder(cs, x, y - h, w, h);
        drawText(cs, txt, x + w / 2f, y - h * 0.65f, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE, true);
        return y - h;
    }

    private float drawKVRow(PDPageContentStream cs, float x, float y, float[] ws, String[] vs, float h) throws IOException {
        float cx = x;
        for (int i = 0; i < ws.length; i++) {
            boolean lbl = (i % 2 == 0);
            drawFilledRect(cs, cx, y - h, ws[i], h, lbl ? new Color(242, 242, 242) : Color.WHITE);
            drawBorder(cs, cx, y - h, ws[i], h);
            drawParagraphInBox(cs, vs[i], cx + 3f, y - 5f, ws[i] - 6f, h - 7f, 8f, lbl);
            cx += ws[i];
        }
        return y - h;
    }

    private float drawRow(PDPageContentStream cs, float x, float y, float[] ws, String[] vs, float h, boolean bold, Color fill) throws IOException {
        float cx = x;
        for (int i = 0; i < ws.length; i++) {
            drawFilledRect(cs, cx, y - h, ws[i], h, fill);
            drawBorder(cs, cx, y - h, ws[i], h);
            drawParagraphInBox(cs, vs[i], cx + 3f, y - 5f, ws[i] - 6f, h - 7f, 8f, bold);
            cx += ws[i];
        }
        return y - h;
    }

    private float drawPairRow(PDPageContentStream cs, float x, float y, float[] ws, String label, String lv, String rv, float h) throws IOException {
        drawBorder(cs, x, y - h, ws[0], h);
        drawBorder(cs, x + ws[0], y - h, ws[1], h);
        if (h < 30f) {
            float fs = 8f, ascent = fs * 0.718f, descent = fs * 0.207f;
            float baseline = y - h / 2f - (ascent - descent) / 2f;
            drawText(cs, label + " " + normalize(lv), x + 4f, baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
            drawText(cs, label + " " + normalize(rv), x + ws[0] + 4f, baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
        } else {
            drawParagraphInBox(cs, label, x + 4f, y - 5f, ws[0] - 8f, 9f, 8.5f, true);
            drawParagraphInBox(cs, lv, x + 4f, y - 16f, ws[0] - 8f, h - 18f, 8f, false);
            drawParagraphInBox(cs, label, x + ws[0] + 4f, y - 5f, ws[1] - 8f, 9f, 8.5f, true);
            drawParagraphInBox(cs, rv, x + ws[0] + 4f, y - 16f, ws[1] - 8f, h - 18f, 8f, false);
        }
        return y - h;
    }

    private float drawCenteredText(PDPageContentStream cs, String text, float cx, float y,
                                   PDType1Font font, float fs, Color color) throws IOException {
        return drawCenteredText(cs, text, cx, y, font, fs, color, false);
    }

    private float drawCenteredText(PDPageContentStream cs, String text, float cx, float y,
                                   PDType1Font font, float fs, Color color, boolean underline) throws IOException {
        String s = normalize(text);
        float tw = font.getStringWidth(s) / 1000f * fs;
        float sx = cx - tw / 2f;
        drawText(cs, s, sx, y, font, fs, color, false);
        if (underline) {
            cs.setStrokingColor(color);
            cs.moveTo(sx, y - 2f);
            cs.lineTo(sx + tw, y - 2f);
            cs.stroke();
        }
        return y - fs;
    }

    private void drawText(PDPageContentStream cs, String text, float x, float y,
                          PDType1Font font, float fs, Color color, boolean centered) throws IOException {
        String s = normalize(text);
        if (s.isEmpty()) return;
        float sx = centered ? x - font.getStringWidth(s) / 1000f * fs / 2f : x;
        cs.beginText();
        cs.setFont(font, fs);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(sx, y);
        cs.showText(s);
        cs.endText();
    }

    private void drawParagraphInBox(PDPageContentStream cs, String text, float x, float y,
                                    float w, float h, float fs, boolean bold) throws IOException {
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        List<String> lines = wrap(normalize(text), w, fs, font);
        float ascent = fs * 0.718f;
        float lh = fs * 1.3f;
        float cy = y - ascent;
        int max = Math.max(1, (int) Math.floor(h / lh));
        for (int i = 0; i < lines.size() && i < max; i++) {
            drawText(cs, lines.get(i), x, cy, font, fs, Color.BLACK, false);
            cy -= lh;
        }
    }

    private void drawFilledRect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void drawBorder(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private void drawFooterLine(PDPageContentStream cs) throws IOException {
        cs.setStrokingColor(new Color(180, 180, 180));
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, FOOTER_Y - 2f);
        cs.lineTo(PAGE_W - MARGIN, FOOTER_Y - 2f);
        cs.stroke();
        Color grey = new Color(96, 96, 96);
        drawText(cs, "Dirección: Av. República E7-197 y Diego de Almagro - Edificio FORUM 300 | Quito - Ecuador", MARGIN, FOOTER_Y - 14f, PDType1Font.HELVETICA, 7f, grey, false);
        drawText(cs, "Teléfono: +539-2 394 0000  |  www.derechosintelectuales.gob.ec", MARGIN, FOOTER_Y - 24f, PDType1Font.HELVETICA, 7f, grey, false);
    }

    private void drawImage(PDDocument doc, PDPageContentStream cs, String path, float x, float y, float w, float h) {
        try {
            byte[] data = readBinaryResource(path);
            if (data.length == 0) return;
            cs.drawImage(PDImageXObject.createFromByteArray(doc, data, path), x, y, w, h);
        } catch (Exception ignored) {
        }
    }

    private XWPFTable createStyledTable(XWPFDocument doc, int rows, int cols, int[] grid) {
        XWPFTable t = doc.createTable(rows, cols);
        t.setWidth("100%");
        setTableBorders(t);
        applyTableGrid(t, grid);
        setTableFixed(t);
        return t;
    }

    private void applyTableGrid(XWPFTable table, int... colWidths) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = table.getCTTbl();
        for (org.apache.xmlbeans.XmlObject obj : ctTbl.selectChildren(QGRID)) {
            XmlCursor xc = obj.newCursor();
            xc.removeXml();
            xc.dispose();
        }
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid grid = ctTbl.addNewTblGrid();
        for (int w : colWidths) grid.addNewGridCol().setW(java.math.BigInteger.valueOf(w));
    }

    private void setTableFixed(XWPFTable table) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = table.getCTTbl();
        org.apache.xmlbeans.XmlObject[] tblPrArr = ctTbl.selectChildren(QTBLPR);
        if (tblPrArr.length == 0) return;
        org.apache.xmlbeans.XmlObject tblPr = tblPrArr[0];
        for (org.apache.xmlbeans.XmlObject obj : tblPr.selectChildren(QLAYOUT)) {
            XmlCursor xc = obj.newCursor();
            xc.removeXml();
            xc.dispose();
        }
        XmlCursor c = tblPr.newCursor();
        c.toEndToken();
        c.insertElement(QLAYOUT);
        c.toPrevToken();
        c.insertAttributeWithValue(new QName(NS, "type"), "fixed");
        c.dispose();
    }

    private void setTableBorders(XWPFTable t) {
        t.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
    }

    private void configureCell(XWPFTableCell cell, ParagraphAlignment align, String bg, boolean clear) {
        if (clear) {
            while (cell.getParagraphs().size() > 1) cell.removeParagraph(1);
            if (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        }
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(align);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        if (bg != null) cell.setColor(bg);
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        if (tcPr.isSetVAlign()) tcPr.getVAlign().setVal(STVerticalJc.CENTER);
        else tcPr.addNewVAlign().setVal(STVerticalJc.CENTER);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold, ParagraphAlignment align, String bg) {
        setCellText(cell, text, bold, align, bg, null, 9);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold, ParagraphAlignment align, String bg, String fc, int fs) {
        configureCell(cell, align, bg, true);
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setAlignment(align);
        p.setVerticalAlignment(TextAlignment.CENTER);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        XWPFRun run = p.createRun();
        run.setBold(bold);
        run.setFontSize(fs);
        if (fc != null) run.setColor(fc);
        String[] parts = normalize(text).split("\n", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) run.addBreak();
            run.setText(parts[i]);
        }
    }

    private void fillLabelValue(XWPFTableRow row, int labelIdx, String label, String value) {
        setCellText(row.getCell(labelIdx), label, false, ParagraphAlignment.LEFT, "F2F2F2");
        setCellText(row.getCell(labelIdx + 1), value, false, ParagraphAlignment.LEFT, "FFFFFF");
    }

    private void fillEquipoRow(XWPFTableRow row, String tipo, ActaRedesComunicacionesRequest.EquipoRow equipo) {
        setCellText(row.getCell(0), tipo, false, ParagraphAlignment.LEFT, "F2F2F2");
        setCellText(row.getCell(1), n(equipo.getMarca()), false, ParagraphAlignment.LEFT, "FFFFFF");
        setCellText(row.getCell(2), n(equipo.getModelo()), false, ParagraphAlignment.LEFT, "FFFFFF");
        setCellText(row.getCell(3), n(equipo.getSerial()), false, ParagraphAlignment.LEFT, "FFFFFF");
        setCellText(row.getCell(4), n(equipo.getCodigo()), false, ParagraphAlignment.LEFT, "FFFFFF");
    }

    private void fillEntregaCell(XWPFTableCell cell, String value, String label) {
        configureCell(cell, ParagraphAlignment.LEFT, "FFFFFF", true);
        XWPFParagraph lp = cell.getParagraphs().get(0);
        lp.setSpacingBefore(0);
        lp.setSpacingAfter(0);
        XWPFRun lr = lp.createRun();
        lr.setBold(true);
        lr.setFontSize(9);
        lr.setText(label);
        XWPFParagraph vp = cell.addParagraph();
        vp.setSpacingBefore(0);
        vp.setSpacingAfter(0);
        vp.createRun().setText(normalize(value));
    }

    private void addCenteredParagraph(XWPFDocument doc, String text, boolean bold, int size, boolean underline) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(0);
        p.setSpacingAfter(underline ? 100 : 20);
        XWPFRun r = p.createRun();
        r.setBold(bold);
        r.setFontSize(size);
        if (underline) r.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
        r.setText(text);
    }

    private void addImageRun(XWPFTableCell cell, String path, int wPx, int hPx) throws IOException {
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        byte[] img = readBinaryResource(path);
        if (img.length == 0) return;
        try {
            p.createRun().addPicture(new ByteArrayInputStream(img), XWPFDocument.PICTURE_TYPE_PNG, path, Units.toEMU(wPx), Units.toEMU(hPx));
        } catch (InvalidFormatException e) {
            throw new IOException("Cannot insert image " + path, e);
        }
    }

    private void addCompactText(XWPFTableCell cell, String text, int fontSize) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setFontSize(fontSize);
        r.setText(text);
    }

    private void addPageBreak(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setPageBreak(true);
    }

    private void mergeCellsH(XWPFTable table, int rowIdx, int from, int to) {
        XWPFTableRow row = table.getRow(rowIdx);
        XWPFTableCell first = row.getCell(from);
        CTTcPr tcPr = first.getCTTc().isSetTcPr() ? first.getCTTc().getTcPr() : first.getCTTc().addNewTcPr();
        if (tcPr.isSetGridSpan()) tcPr.getGridSpan().setVal(java.math.BigInteger.valueOf(to - from + 1L));
        else tcPr.addNewGridSpan().setVal(java.math.BigInteger.valueOf(to - from + 1L));
        for (int i = to; i > from; i--) row.removeCell(i);
    }

    private void mergeCellsV(XWPFTable table, int fromRow, int toRow, int col) {
        for (int r = fromRow; r <= toRow; r++) {
            XWPFTableCell cell = table.getRow(r).getCell(col);
            CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
            CTVMerge vm = tcPr.isSetVMerge() ? tcPr.getVMerge() : tcPr.addNewVMerge();
            vm.setVal(r == fromRow ? STMerge.RESTART : STMerge.CONTINUE);
        }
    }

    private void setRowHeight(XWPFTableRow row, int twips) {
        row.setHeight(twips);
    }

    private List<ActaRedesComunicacionesRequest.ActividadMantenimiento> normalizeActivities(
            List<ActaRedesComunicacionesRequest.ActividadMantenimiento> values, int rowCount) {
        List<ActaRedesComunicacionesRequest.ActividadMantenimiento> normalized = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            ActaRedesComunicacionesRequest.ActividadMantenimiento activity = new ActaRedesComunicacionesRequest.ActividadMantenimiento();
            if (values != null && i < values.size() && values.get(i) != null) {
                activity.setActividad(n(values.get(i).getActividad()));
                activity.setFecha(n(values.get(i).getFecha()));
                activity.setEstado(n(values.get(i).getEstado()));
                activity.setObservacion(n(values.get(i).getObservacion()));
            } else {
                activity.setActividad("");
                activity.setFecha("");
                activity.setEstado("");
                activity.setObservacion("");
            }
            normalized.add(activity);
        }
        return normalized;
    }

    private List<ActaRedesComunicacionesRequest.ActividadMantenimiento> normalizeActivities(
            List<ActaRedesComunicacionesRequest.ActividadMantenimiento> values, String[][] labels) {
        List<ActaRedesComunicacionesRequest.ActividadMantenimiento> normalized = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            ActaRedesComunicacionesRequest.ActividadMantenimiento activity = new ActaRedesComunicacionesRequest.ActividadMantenimiento();
            activity.setActividad(labels[i][0]);
            if (values != null && i < values.size() && values.get(i) != null) {
                activity.setFecha(n(values.get(i).getFecha()));
                activity.setEstado(n(values.get(i).getEstado()));
                activity.setObservacion(n(values.get(i).getObservacion()));
            } else {
                activity.setFecha("");
                activity.setEstado("");
                activity.setObservacion("");
            }
            normalized.add(activity);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String n(String value) {
        return normalize(value);
    }

    private List<String> wrap(String text, float width, float fontSize, PDType1Font font) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            float measured = font.getStringWidth(candidate) / 1000f * fontSize;
            if (measured > width && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private boolean isEquipoRowNotEmpty(ActaRedesComunicacionesRequest.EquipoRow row) {
        if (row == null) return false;
        return (row.getTipo() != null && !row.getTipo().trim().isEmpty()) ||
               (row.getMarca() != null && !row.getMarca().trim().isEmpty()) ||
               (row.getModelo() != null && !row.getModelo().trim().isEmpty()) ||
               (row.getSerial() != null && !row.getSerial().trim().isEmpty()) ||
               (row.getCodigo() != null && !row.getCodigo().trim().isEmpty());
    }

    private List<ActaRedesComunicacionesRequest.ActividadMantenimiento> filterActivities(
            List<ActaRedesComunicacionesRequest.ActividadMantenimiento> all, String[][] activityLabels) {
        List<ActaRedesComunicacionesRequest.ActividadMantenimiento> filtered = new ArrayList<>();
        if (all == null) return filtered;
        for (String[] label : activityLabels) {
            String target = label[0].trim().toLowerCase();
            for (ActaRedesComunicacionesRequest.ActividadMantenimiento act : all) {
                if (act != null && act.getActividad() != null && act.getActividad().trim().toLowerCase().equals(target)) {
                    // Solo agregar si tiene datos cargados (fecha, estado u observaciones no vacias)
                    if ((act.getFecha() != null && !act.getFecha().trim().isEmpty()) ||
                        (act.getEstado() != null && !act.getEstado().trim().isEmpty()) ||
                        (act.getObservacion() != null && !act.getObservacion().trim().isEmpty())) {
                        filtered.add(act);
                    }
                }
            }
        }
        return filtered;
    }

    /**
     * Fondo de página (plantilla institucional Fondo_senadi.png): se coloca en el
     * encabezado de Word como imagen detrás del texto, ocupando toda la hoja A4,
     * con sus colores originales y repetida en todas las páginas.
     */
    private void addFondoPagina(XWPFDocument doc) throws IOException {
        byte[] img = readBinaryResource("actas/Fondo_senadi.png");
        if (img.length == 0) return;
        XWPFHeader header = doc.createHeader(HeaderFooterType.DEFAULT);
        XWPFParagraph p = header.getParagraphs().isEmpty() ? header.createParagraph() : header.getParagraphs().get(0);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        XWPFRun run = p.createRun();
        int ancho = Units.toEMU(595.3);
        int alto = Units.toEMU(841.9);
        try {
            run.addPicture(new ByteArrayInputStream(img), XWPFDocument.PICTURE_TYPE_PNG,
                    "Fondo_senadi.png", ancho, alto);
        } catch (InvalidFormatException e) {
            throw new IOException("Cannot insert image actas/Fondo_senadi.png", e);
        }
        CTDrawing drawing = run.getCTR().getDrawingArray(0);
        CTInline inline = drawing.getInlineArray(0);
        String xml = "<xml-fragment xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\""
                + " distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\" simplePos=\"0\" relativeHeight=\"0\""
                + " behindDoc=\"1\" locked=\"1\" layoutInCell=\"1\" allowOverlap=\"1\">"
                + "<wp:simplePos x=\"0\" y=\"0\"/>"
                + "<wp:positionH relativeFrom=\"page\"><wp:posOffset>0</wp:posOffset></wp:positionH>"
                + "<wp:positionV relativeFrom=\"page\"><wp:posOffset>0</wp:posOffset></wp:positionV>"
                + "<wp:extent cx=\"" + ancho + "\" cy=\"" + alto + "\"/>"
                + "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>"
                + "<wp:wrapNone/>"
                + "<wp:docPr id=\"1001\" name=\"Fondo SENADI\"/>"
                + "<wp:cNvGraphicFramePr/>"
                + "</xml-fragment>";
        try {
            CTAnchor anchor = CTAnchor.Factory.parse(xml);
            anchor.setGraphic(inline.getGraphic());
            drawing.setAnchorArray(new CTAnchor[]{anchor});
            drawing.removeInline(0);
        } catch (XmlException e) {
            throw new IOException("Cannot place background image", e);
        }
    }

    private byte[] readBinaryResource(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) return new byte[0];
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
