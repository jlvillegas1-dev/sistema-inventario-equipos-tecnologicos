package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.dto.ActaSoftwareRequest;
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

import javax.xml.namespace.QName;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActaSoftwareDocumentService {

    // ── XML namespace constants (same as PC service) ───────────────────────────
    private static final String NS      = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final QName  QGRID   = new QName(NS, "tblGrid");
    private static final QName  QTBLPR  = new QName(NS, "tblPr");
    private static final QName  QLAYOUT = new QName(NS, "tblLayout");

    // ── Document titles ────────────────────────────────────────────────────────
    private static final String TITULO_1 = "SERVICIO NACIONAL DE DERECHOS INTELECTUALES";
    private static final String TITULO_2 = "DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN";
    private static final String TITULO_3 = "FORMULARIO DE PROGRAMAS Y APLICACIONES INSTALADAS";

    private static final String CERTIFICACION_DEFAULT =
            "Certifico que los programas y aplicaciones detallados en el presente documento se encuentran " +
            "debidamente instalados y licenciados en el equipo institucional asignado a mi cargo. Me comprometo " +
            "a utilizar correctamente los recursos tecnologicos, a no instalar software no autorizado y a " +
            "reportar cualquier novedad al area de soporte de DTIC.";

    // ── Software catalogue ─────────────────────────────────────────────────────
    private static final String[][] SOFTWARE_PAGE1 = {
        {"SISTEMA OPERATIVO",               "MICROSOFT WINDOWS 10 PRO"},
        {"SISTEMA OPERATIVO",               "MICROSOFT WINDOWS 11 PRO"},
        {"PAQUETE OFIMATICO",               "MICROSOFT OFFICE 365 PRO PLUS"},
        {"SOFTWARE ANTIVIRUS",              "ESET ENDPOINT SECURITY"},
        {"NAVEGADORES",                     "MOZILLA FIREFOX"},
        {"NAVEGADORES",                     "GOOGLE CHROME"},
        {"NAVEGADORES",                     "MICROSOFT EDGE"},
        {"NAVEGADORES",                     "SAFARI"},
        {"SOFTWARE SOPORTE REMOTO",         "TIGHT VNC SERVICE"},
        {"SOFTWARE SOPORTE REMOTO",         "ANYDESK"},
        {"SOFTWARE DE FIRMA ELECTRONICA",   "FIRMA EC"},
        {"SOFTWARE DE FIRMA ELECTRONICA",   "FIRMA MASIVA SENADI"},
        {"SOFTWARE PARA VISUALIZACION PDF", "PDF 24"},
        {"SOFTWARE PARA VISUALIZACION PDF", "ADOBE ACROBAT READER"},
        {"SOFTWARE PARA VISUALIZACION PDF", "NITRO PDF"},
        {"COMPRESION",                      "WINRAR"},
        {"CORREOS",                         "ZIMBRA DESKTOP"},
        {"VIDEO CONFERENCIA",               "ZOOM MEETINGS"}
    };

    private static final String[] DRIVERS_PAGE2 = {
        "HP LASER JET COLOR",
        "HP LASER JET B/N",
        "LEXMARK LASER B/N",
        "ESCANER EPSON",
        "ESCANER HP",
        "ESCANER KODAK"
    };

    // ── DOCX row heights (twips) ───────────────────────────────────────────────
    private static final int DX_HDR  = 340;
    private static final int DX_DATA = 380;
    private static final int DX_SW   = 320;
    private static final int DX_SIGN = 1600;

    // ── PDF layout (pt) ───────────────────────────────────────────────────────
    private static final float PAGE_W  = PDRectangle.A4.getWidth();
    private static final float PAGE_H  = PDRectangle.A4.getHeight();
    private static final float MARGIN  = 40f;
    private static final float CONT_W  = PAGE_W - 2 * MARGIN;
    private static final float FOOTER_Y = 78f;
    private static final float H_HDR  = 18f;
    private static final float H_ROW  = 16f;
    private static final float H_DARK = 20f;
    private static final float H_SW   = 14f;
    private static final float H_SIGN = 65f;

    // ══════════════════════════ PUBLIC ENTRY POINTS ═══════════════════════════

    public byte[] generarDocx(ActaSoftwareRequest request) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ensureDefaults(request);
            configurePage(doc);
            addFondoPagina(doc);
            addDocumentHeader(doc);
            addFuncionarioTable(doc, request);
            addEquipoTable(doc, request);
            addSoftwarePage1Table(doc, request);
            addDriversPage2Table(doc, request);
            addCertificacion(doc, request);
            addEntregaRecepcionTable(doc, request);
            doc.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarPdf(ActaSoftwareRequest request) throws IOException {
        ensureDefaults(request);
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            buildPdfPage1(doc, request);
            buildPdfPage2(doc, request);
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ══════════════════════════ DOCX SECTION BUILDERS ════════════════════════

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

    private void addDocumentHeader(XWPFDocument doc) throws IOException {
        addCenteredParagraph(doc, TITULO_1, true, 13, false);
        addCenteredParagraph(doc, TITULO_2, true, 11, false);
        addCenteredParagraph(doc, TITULO_3, true, 12, true);
    }

    private void addFuncionarioTable(XWPFDocument doc, ActaSoftwareRequest req) {
        ActaSoftwareRequest.Funcionario f = req.getFuncionario();
        int[] grid = {1100, 3050, 1100, 2550, 1100, 1566};
        XWPFTable t = createStyledTable(doc, 3, 6, grid);
        t.getRow(0).setHeight(DX_HDR);
        t.getRow(1).setHeight(DX_DATA);
        t.getRow(2).setHeight(DX_DATA);
        mergeCellsH(t, 0, 0, 5);
        setCellText(t.getRow(0).getCell(0), "DATOS DEL FUNCIONARIO SENADI", true, ParagraphAlignment.CENTER, "D9D9D9");
        fillLabelValue(t.getRow(1), 0, "NOMBRE",   n(f.getNombre()));
        fillLabelValue(t.getRow(1), 2, "CARGO",    n(f.getCargo()));
        fillLabelValue(t.getRow(1), 4, "N DEG EXT.", n(f.getExtension()));
        fillLabelValue(t.getRow(2), 0, "CORREO",   n(f.getCorreo()));
        fillLabelValue(t.getRow(2), 2, "AREA",     n(f.getArea()));
        fillLabelValue(t.getRow(2), 4, "EDIFICIO", n(f.getEdificio()));
    }

    private void addEquipoTable(XWPFDocument doc, ActaSoftwareRequest req) {
        ActaSoftwareRequest.EquipoInfo eq = req.getEquipo();
        int[] grid = {2092, 2092, 2092, 2092, 2098};
        XWPFTable t = createStyledTable(doc, 3, 5, grid);
        t.getRow(0).setHeight(DX_HDR);
        t.getRow(1).setHeight(DX_DATA);
        t.getRow(2).setHeight(DX_DATA);
        mergeCellsH(t, 0, 0, 4);
        setCellText(t.getRow(0).getCell(0), "EQUIPOS", true, ParagraphAlignment.CENTER, "D9D9D9");
        String[] hdrs = {"TIPO", "MARCA", "MODELO", "SERIAL", "CODIGO"};
        for (int i = 0; i < hdrs.length; i++)
            setCellText(t.getRow(1).getCell(i), hdrs[i], true, ParagraphAlignment.CENTER, "E6E6E6");
        String[] vals = {n(eq.getTipo()), n(eq.getMarca()), n(eq.getModelo()), n(eq.getSerial()), n(eq.getCodigo())};
        for (int i = 0; i < vals.length; i++)
            setCellText(t.getRow(2).getCell(i), vals[i], false, ParagraphAlignment.LEFT, "FFFFFF");
    }

    private void addSoftwarePage1Table(XWPFDocument doc, ActaSoftwareRequest req) {
        List<ActaSoftwareRequest.SoftwareItem> items = req.getSoftwareItems();

        // Collect only SI items
        List<String[]> siItems = new ArrayList<>();
        for (int i = 0; i < SOFTWARE_PAGE1.length; i++) {
            String ins = (items != null && i < items.size()) ? n(items.get(i).getInstalado()) : "";
            if ("SI".equalsIgnoreCase(ins)) siItems.add(SOFTWARE_PAGE1[i]);
        }

        int totalRows = 2 + Math.max(1, siItems.size()); // at least 1 data row
        int[] grid = {3872, 4919, 1675};
        XWPFTable t = createStyledTable(doc, totalRows, 3, grid);
        t.getRow(0).setHeight(DX_HDR);
        t.getRow(1).setHeight(DX_DATA);
        for (int i = 2; i < totalRows; i++) t.getRow(i).setHeight(DX_SW);

        mergeCellsH(t, 0, 0, 2);
        setCellText(t.getRow(0).getCell(0), "PROGRAMAS Y APLICACIONES INSTALADAS", true, ParagraphAlignment.CENTER, "7F7F7F", "FFFFFF", 9);
        setCellText(t.getRow(1).getCell(0), "CATEGORIA",             true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(1).getCell(1), "PROGRAMA / APLICACION", true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(1).getCell(2), "INSTALADO",             true, ParagraphAlignment.CENTER, "D9D9D9");

        for (int i = 0; i < siItems.size(); i++) {
            String bg = (i % 2 == 0) ? "FFFFFF" : "F5F5F8";
            XWPFTableRow row = t.getRow(2 + i);
            setCellText(row.getCell(0), siItems.get(i)[0], false, ParagraphAlignment.LEFT,   bg);
            setCellText(row.getCell(1), siItems.get(i)[1], false, ParagraphAlignment.LEFT,   bg);
            setCellText(row.getCell(2), "SI",               true,  ParagraphAlignment.CENTER, bg);
        }
    }

    private void addDriversPage2Table(XWPFDocument doc, ActaSoftwareRequest req) {
        List<ActaSoftwareRequest.SoftwareItem> items = req.getSoftwareItems();
        int offset = SOFTWARE_PAGE1.length;

        // Collect only SI drivers
        List<String> siDrivers = new ArrayList<>();
        for (int i = 0; i < DRIVERS_PAGE2.length; i++) {
            int idx = offset + i;
            String ins = (items != null && idx < items.size()) ? n(items.get(idx).getInstalado()) : "";
            if ("SI".equalsIgnoreCase(ins)) siDrivers.add(DRIVERS_PAGE2[i]);
        }

        // Collect non-empty adicionales
        List<String> adicionales = req.getDriversAdicionales();
        List<String> siAdicionales = new ArrayList<>();
        if (adicionales != null) {
            for (String val : adicionales) {
                if (val != null && !val.trim().isEmpty()) siAdicionales.add(val.trim());
            }
        }

        int[] grid = {3872, 4919, 1675};

        // ── Drivers table ────────────────────────────────────────────────────
        if (!siDrivers.isEmpty()) {
            int drvRows = 2 + siDrivers.size();
            XWPFTable td = createStyledTable(doc, drvRows, 3, grid);
            td.getRow(0).setHeight(DX_HDR);
            td.getRow(1).setHeight(DX_DATA);
            for (int i = 2; i < drvRows; i++) td.getRow(i).setHeight(DX_SW);
            mergeCellsH(td, 0, 0, 2);
            setCellText(td.getRow(0).getCell(0), "CONTROLADORES / DRIVERS", true, ParagraphAlignment.CENTER, "7F7F7F", "FFFFFF", 9);
            setCellText(td.getRow(1).getCell(0), "CATEGORIA",             true, ParagraphAlignment.CENTER, "D9D9D9");
            setCellText(td.getRow(1).getCell(1), "PROGRAMA / APLICACION", true, ParagraphAlignment.CENTER, "D9D9D9");
            setCellText(td.getRow(1).getCell(2), "INSTALADO",             true, ParagraphAlignment.CENTER, "D9D9D9");
            for (int i = 0; i < siDrivers.size(); i++) {
                String bg = (i % 2 == 0) ? "FFFFFF" : "F5F5F8";
                XWPFTableRow row = td.getRow(2 + i);
                setCellText(row.getCell(0), "CONTROLADORES / DRIVERS", false, ParagraphAlignment.LEFT,   bg);
                setCellText(row.getCell(1), siDrivers.get(i),           false, ParagraphAlignment.LEFT,   bg);
                setCellText(row.getCell(2), "SI",                        true,  ParagraphAlignment.CENTER, bg);
            }
        }

        // ── Adicionales table (only if there are entries) ─────────────────────
        if (!siAdicionales.isEmpty()) {
            int adicRows = 2 + siAdicionales.size();
            int[] adicGrid = {8360, 2106};
            XWPFTable ta = createStyledTable(doc, adicRows, 2, adicGrid);
            ta.getRow(0).setHeight(DX_HDR);
            ta.getRow(1).setHeight(DX_DATA);
            for (int i = 2; i < adicRows; i++) ta.getRow(i).setHeight(DX_SW + 80);
            mergeCellsH(ta, 0, 0, 1);
            setCellText(ta.getRow(0).getCell(0), "SOFTWARE Y DRIVERS ADICIONAL", true, ParagraphAlignment.CENTER, "7F7F7F", "FFFFFF", 9);
            setCellText(ta.getRow(1).getCell(0), "DESCRIPCION",          true, ParagraphAlignment.CENTER, "D9D9D9");
            setCellText(ta.getRow(1).getCell(1), "INSTALADO",             true, ParagraphAlignment.CENTER, "D9D9D9");
            for (int i = 0; i < siAdicionales.size(); i++) {
                XWPFTableRow row = ta.getRow(2 + i);
                setCellText(row.getCell(0), siAdicionales.get(i), false, ParagraphAlignment.LEFT,   "FFFFFF");
                setCellText(row.getCell(1), "SI",                 true,  ParagraphAlignment.CENTER, "FFFFFF");
            }
        }
    }

    private void addCertificacion(XWPFDocument doc, ActaSoftwareRequest req) {
        String cert = n(req.getCertificacion()).isEmpty() ? CERTIFICACION_DEFAULT : n(req.getCertificacion());
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.setSpacingBefore(80);
        p.setSpacingAfter(80);
        XWPFRun r = p.createRun();
        r.setFontSize(9);
        r.setText(normalize(cert));
    }

    private void addEntregaRecepcionTable(XWPFDocument doc, ActaSoftwareRequest req) {
        ActaSoftwareRequest.FirmaRecepcion entrega = req.getEntrega();
        ActaSoftwareRequest.FirmaRecepcion recibe  = req.getRecibe();
        int[] grid = {5233, 5233};
        XWPFTable t = createStyledTable(doc, 5, 2, grid);
        t.getRow(0).setHeight(380);
        t.getRow(1).setHeight(340);
        t.getRow(2).setHeight(420);
        t.getRow(3).setHeight(DX_SIGN);
        t.getRow(4).setHeight(420);
        mergeCellsH(t, 0, 0, 1);
        setCellText(t.getRow(0).getCell(0), "ENTREGA RECEPCION DEL EQUIPO", true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(1).getCell(0), "ENTREGA", false, ParagraphAlignment.CENTER, "EDEDED");
        setCellText(t.getRow(1).getCell(1), "RECIBE",  false, ParagraphAlignment.CENTER, "EDEDED");
        fillEntregaCell(t.getRow(2).getCell(0), n(entrega.getNombre()), "Nombre:");
        fillEntregaCell(t.getRow(2).getCell(1), n(recibe.getNombre()),  "Nombre:");
        fillEntregaCell(t.getRow(3).getCell(0), n(entrega.getFirma()),  "Firma:");
        fillEntregaCell(t.getRow(3).getCell(1), n(recibe.getFirma()),   "Firma:");
        fillEntregaCell(t.getRow(4).getCell(0), n(entrega.getFecha()),  "Fecha:");
        fillEntregaCell(t.getRow(4).getCell(1), n(recibe.getFecha()),   "Fecha:");
    }

    // ══════════════════════════ PDF GENERATION ════════════════════════════════

    private void buildPdfPage1(PDDocument doc, ActaSoftwareRequest req) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float logoY = PAGE_H - 28f;
            // Fondo de página (plantilla institucional)
            drawImage(cs, doc, "actas/Fondo_senadi.png", 0f, 0f, PAGE_W, PAGE_H);

            float y = logoY - 68f;
            y = drawCenteredText(cs, TITULO_1, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 12f, Color.BLACK);
            y -= 4f;
            y = drawCenteredText(cs, TITULO_2, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 10f, Color.BLACK);
            y -= 8f;
            y = drawCenteredText(cs, TITULO_3, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 11f, Color.BLACK, true);
            y -= 12f;

            // Funcionario
            ActaSoftwareRequest.Funcionario f = req.getFuncionario();
            float[] fw = {55f, 155f, 55f, 140f, 55f, CONT_W - 460f};
            y = drawSectionHeader(cs, "DATOS DEL FUNCIONARIO SENADI", MARGIN, y, CONT_W, H_HDR, new Color(217,217,217), Color.BLACK, 9f);
            y = drawKVRow(cs, MARGIN, y, fw, new String[]{"NOMBRE", n(f.getNombre()), "CARGO", n(f.getCargo()), "N EXT.", n(f.getExtension())}, H_ROW);
            y = drawKVRow(cs, MARGIN, y, fw, new String[]{"CORREO", n(f.getCorreo()), "AREA",  n(f.getArea()),   "EDIFICIO", n(f.getEdificio())}, H_ROW);

            // Equipo
            ActaSoftwareRequest.EquipoInfo eq = req.getEquipo();
            float eW = CONT_W / 5f;
            y = drawSectionHeader(cs, "EQUIPOS", MARGIN, y, CONT_W, H_HDR, new Color(217,217,217), Color.BLACK, 9f);
            y = drawRow(cs, MARGIN, y, new float[]{eW,eW,eW,eW,eW}, new String[]{"TIPO","MARCA","MODELO","SERIAL","CODIGO"}, H_ROW, true, new Color(230,230,230));
            y = drawRow(cs, MARGIN, y, new float[]{eW,eW,eW,eW,eW}, new String[]{n(eq.getTipo()),n(eq.getMarca()),n(eq.getModelo()),n(eq.getSerial()),n(eq.getCodigo())}, H_ROW, false, Color.WHITE);

            // Software table — only SI items
            float cW = CONT_W * 0.37f, pW = CONT_W * 0.47f, iW = CONT_W * 0.16f;
            List<ActaSoftwareRequest.SoftwareItem> swItems = req.getSoftwareItems();
            List<String[]> siSw = new ArrayList<>();
            for (int i = 0; i < SOFTWARE_PAGE1.length; i++) {
                String ins = (swItems != null && i < swItems.size()) ? n(swItems.get(i).getInstalado()) : "";
                if ("SI".equalsIgnoreCase(ins)) siSw.add(SOFTWARE_PAGE1[i]);
            }
            if (!siSw.isEmpty()) {
                y = drawDarkHeader(cs, "PROGRAMAS Y APLICACIONES INSTALADAS", MARGIN, y, CONT_W, H_DARK);
                y = drawRow(cs, MARGIN, y, new float[]{cW,pW,iW}, new String[]{"CATEGORIA","PROGRAMA / APLICACION","INSTALADO"}, H_ROW, true, new Color(217,217,217));
                for (int i = 0; i < siSw.size(); i++) {
                    Color bg = (i % 2 == 0) ? Color.WHITE : new Color(245,245,248);
                    y = drawSwRow(cs, MARGIN, y, cW, pW, iW, H_SW, siSw.get(i)[0], siSw.get(i)[1], "SI", bg);
                }
            }

        }
    }

    private void buildPdfPage2(PDDocument doc, ActaSoftwareRequest req) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float logoY = PAGE_H - 28f;
            // Fondo de página (plantilla institucional)
            drawImage(cs, doc, "actas/Fondo_senadi.png", 0f, 0f, PAGE_W, PAGE_H);

            float y = logoY - 68f;
            y = drawCenteredText(cs, TITULO_1, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 12f, Color.BLACK);
            y -= 4f;
            y = drawCenteredText(cs, TITULO_2, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 10f, Color.BLACK);
            y -= 8f;
            y = drawCenteredText(cs, TITULO_3, PAGE_W / 2f, y, PDType1Font.HELVETICA_BOLD, 11f, Color.BLACK, true);
            y -= 12f;

            float cW = CONT_W * 0.37f, pW = CONT_W * 0.47f, iW = CONT_W * 0.16f;

            // Drivers — only SI
            List<ActaSoftwareRequest.SoftwareItem> swItems = req.getSoftwareItems();
            int offset = SOFTWARE_PAGE1.length;
            List<String> siDrv = new ArrayList<>();
            for (int i = 0; i < DRIVERS_PAGE2.length; i++) {
                int idx = offset + i;
                String ins = (swItems != null && idx < swItems.size()) ? n(swItems.get(idx).getInstalado()) : "";
                if ("SI".equalsIgnoreCase(ins)) siDrv.add(DRIVERS_PAGE2[i]);
            }
            if (!siDrv.isEmpty()) {
                y = drawDarkHeader(cs, "CONTROLADORES / DRIVERS", MARGIN, y, CONT_W, H_DARK);
                y = drawRow(cs, MARGIN, y, new float[]{cW,pW,iW}, new String[]{"CATEGORIA","PROGRAMA / APLICACION","INSTALADO"}, H_ROW, true, new Color(217,217,217));
                for (int i = 0; i < siDrv.size(); i++) {
                    Color bg = (i % 2 == 0) ? Color.WHITE : new Color(245,245,248);
                    y = drawSwRow(cs, MARGIN, y, cW, pW, iW, H_SW, "CONTROLADORES / DRIVERS", siDrv.get(i), "SI", bg);
                }
            }

            // Adicionales — only non-empty
            List<String> adicionales = req.getDriversAdicionales();
            List<String> siAdic = new ArrayList<>();
            if (adicionales != null) {
                for (String v : adicionales) { if (v != null && !v.trim().isEmpty()) siAdic.add(v.trim()); }
            }
            if (!siAdic.isEmpty()) {
                y = drawDarkHeader(cs, "SOFTWARE Y DRIVERS ADICIONAL", MARGIN, y, CONT_W, H_DARK);
                y = drawRow(cs, MARGIN, y, new float[]{cW,pW,iW}, new String[]{"DESCRIPCION","PROGRAMA / APLICACION","INSTALADO"}, H_ROW, true, new Color(217,217,217));
                for (String v : siAdic) {
                    y = drawSwRow(cs, MARGIN, y, cW, pW, iW, H_SW + 4f, "", v, "SI", Color.WHITE);
                }
            }
            y -= 4f;

            // Certificacion
            String cert = n(req.getCertificacion()).isEmpty() ? CERTIFICACION_DEFAULT : n(req.getCertificacion());
            float certH = 50f;
            drawFilledRect(cs, MARGIN, y - certH, CONT_W, certH, new Color(245,245,245));
            drawBorder(cs, MARGIN, y - certH, CONT_W, certH);
            drawParagraphInBox(cs, cert, MARGIN + 4f, y - 5f, CONT_W - 8f, certH - 8f, 7.5f, false);
            y -= certH + 8f;

            // Entrega / Recibe
            ActaSoftwareRequest.FirmaRecepcion entrega = req.getEntrega();
            ActaSoftwareRequest.FirmaRecepcion recibe  = req.getRecibe();
            float half = CONT_W / 2f;
            float[] cols = {half, half};
            y = drawSectionHeader(cs, "ENTREGA RECEPCION DEL EQUIPO", MARGIN, y, CONT_W, H_HDR, new Color(217,217,217), Color.BLACK, 9f);
            y = drawRow(cs, MARGIN, y, cols, new String[]{"ENTREGA","RECIBE"}, 14f, false, new Color(237,237,237));
            y = drawPairRow(cs, MARGIN, y, cols, "Nombre:", n(entrega.getNombre()), n(recibe.getNombre()), H_ROW);
            y = drawPairRow(cs, MARGIN, y, cols, "Firma:",  n(entrega.getFirma()),  n(recibe.getFirma()),  H_SIGN);
            y = drawPairRow(cs, MARGIN, y, cols, "Fecha:",  n(entrega.getFecha()),  n(recibe.getFecha()),  H_ROW);

        }
    }

    // ══════════════════════════ PDF PRIMITIVES (same as PC service) ══════════

    private float drawSectionHeader(PDPageContentStream cs, String txt, float x, float y,
                                    float w, float h, Color fill, Color tc, float fs) throws IOException {
        drawFilledRect(cs, x, y - h, w, h, fill);
        drawBorder(cs, x, y - h, w, h);
        drawText(cs, txt, x + w / 2f, y - h * 0.65f, PDType1Font.HELVETICA_BOLD, fs, tc, true);
        return y - h;
    }

    private float drawDarkHeader(PDPageContentStream cs, String txt, float x, float y,
                                  float w, float h) throws IOException {
        drawFilledRect(cs, x, y - h, w, h, new Color(46,64,87));
        drawBorder(cs, x, y - h, w, h);
        drawText(cs, txt, x + w / 2f, y - h * 0.65f, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE, true);
        return y - h;
    }

    private float drawKVRow(PDPageContentStream cs, float x, float y, float[] ws,
                             String[] vs, float h) throws IOException {
        float cx = x;
        for (int i = 0; i < ws.length; i++) {
            boolean lbl = (i % 2 == 0);
            drawFilledRect(cs, cx, y - h, ws[i], h, lbl ? new Color(242,242,242) : Color.WHITE);
            drawBorder(cs, cx, y - h, ws[i], h);
            drawParagraphInBox(cs, vs[i], cx + 3f, y - 5f, ws[i] - 6f, h - 7f, 8f, lbl);
            cx += ws[i];
        }
        return y - h;
    }

    private float drawRow(PDPageContentStream cs, float x, float y, float[] ws,
                          String[] vs, float h, boolean bold, Color fill) throws IOException {
        float cx = x;
        for (int i = 0; i < ws.length; i++) {
            drawFilledRect(cs, cx, y - h, ws[i], h, fill);
            drawBorder(cs, cx, y - h, ws[i], h);
            drawParagraphInBox(cs, vs[i], cx + 3f, y - 5f, ws[i] - 6f, h - 7f, 8f, bold);
            cx += ws[i];
        }
        return y - h;
    }

    private float drawSwRow(PDPageContentStream cs, float x, float y,
                             float cW, float pW, float iW, float h,
                             String cat, String prog, String ins, Color bg) throws IOException {
        drawFilledRect(cs, x,        y - h, cW, h, bg);
        drawFilledRect(cs, x + cW,   y - h, pW, h, bg);
        drawFilledRect(cs, x+cW+pW,  y - h, iW, h, bg);
        drawBorder(cs, x,       y - h, cW, h);
        drawBorder(cs, x + cW,  y - h, pW, h);
        drawBorder(cs, x+cW+pW, y - h, iW, h);
        if (!cat.isEmpty())  drawParagraphInBox(cs, cat,  x + 3f,      y - 5f, cW - 6f, h - 7f, 7.5f, false);
        if (!prog.isEmpty()) drawParagraphInBox(cs, prog, x+cW + 3f,   y - 5f, pW - 6f, h - 7f, 7.5f, false);
        if (!ins.isEmpty()) {
            float fs = 7.5f;
            float tw;
            try { tw = PDType1Font.HELVETICA_BOLD.getStringWidth(ins) / 1000f * fs; } catch (IOException e) { tw = 0f; }
            float ix = x + cW + pW + (iW - tw) / 2f;
            float iy = y - h / 2f - fs * 0.718f / 2f;
            drawText(cs, ins, ix, iy, PDType1Font.HELVETICA_BOLD, fs, Color.BLACK, false);
        }
        return y - h;
    }

    private float drawPairRow(PDPageContentStream cs, float x, float y, float[] ws,
                               String label, String lv, String rv, float h) throws IOException {
        drawBorder(cs, x,        y - h, ws[0], h);
        drawBorder(cs, x + ws[0], y - h, ws[1], h);
        if (h < 30f) {
            float fs = 8f, ascent = fs * 0.718f, descent = fs * 0.207f;
            float baseline = y - h / 2f - (ascent - descent) / 2f;
            drawText(cs, label + " " + normalize(lv), x + 4f,         baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
            drawText(cs, label + " " + normalize(rv), x + ws[0] + 4f, baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
        } else {
            drawParagraphInBox(cs, label, x + 4f,           y - 5f,  ws[0] - 8f, 9f,    8.5f, true);
            drawParagraphInBox(cs, lv,    x + 4f,           y - 16f, ws[0] - 8f, h-18f, 8f,   false);
            drawParagraphInBox(cs, label, x + ws[0] + 4f,   y - 5f,  ws[1] - 8f, 9f,    8.5f, true);
            drawParagraphInBox(cs, rv,    x + ws[0] + 4f,   y - 16f, ws[1] - 8f, h-18f, 8f,   false);
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
        float lh     = fs * 1.3f;
        float cy     = y - ascent;
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
        drawText(cs, "Direccion: Av. Republica E7-197 y Diego de Almagro - Edificio FORUM 300 | Quito - Ecuador",
                MARGIN, FOOTER_Y - 14f, PDType1Font.HELVETICA, 7f, grey, false);
        drawText(cs, "Telefono: +539-2 394 0000  |  www.derechosintelectuales.gob.ec",
                MARGIN, FOOTER_Y - 24f, PDType1Font.HELVETICA, 7f, grey, false);
    }

    private void drawImage(PDPageContentStream cs, PDDocument doc, String path,
                            float x, float y, float w, float h) {
        try {
            byte[] data = readBinaryResource(path);
            if (data.length == 0) return;
            cs.drawImage(PDImageXObject.createFromByteArray(doc, data, path), x, y, w, h);
        } catch (Exception ignored) {}
    }

    // ══════════════════════════ DOCX HELPERS (same pattern as PC service) ═════

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
            XmlCursor xc = obj.newCursor(); xc.removeXml(); xc.dispose();
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
            XmlCursor xc = obj.newCursor(); xc.removeXml(); xc.dispose();
        }
        XmlCursor c = tblPr.newCursor();
        c.toEndToken();
        c.insertElement(QLAYOUT);
        c.toPrevToken();
        c.insertAttributeWithValue(new QName(NS, "type"), "fixed");
        c.dispose();
    }

    /** Merge cells from→to in one row using gridSpan (correct Word merge). */
    private void mergeCellsH(XWPFTable table, int rowIdx, int from, int to) {
        XWPFTableRow row = table.getRow(rowIdx);
        XWPFTableCell first = row.getCell(from);
        CTTcPr tcPr = first.getCTTc().isSetTcPr() ? first.getCTTc().getTcPr() : first.getCTTc().addNewTcPr();
        if (tcPr.isSetGridSpan()) tcPr.getGridSpan().setVal(java.math.BigInteger.valueOf(to - from + 1L));
        else                      tcPr.addNewGridSpan().setVal(java.math.BigInteger.valueOf(to - from + 1L));
        for (int i = to; i > from; i--) row.removeCell(i);
    }

    private void setTableBorders(XWPFTable t) {
        t.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setTopBorder   (XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setLeftBorder  (XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
        t.setRightBorder (XWPFTable.XWPFBorderType.SINGLE, 1, 0, "000000");
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
        if (tcPr.isSetVAlign()) tcPr.getVAlign().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc.CENTER);
        else                    tcPr.addNewVAlign().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc.CENTER);
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
        setCellText(row.getCell(labelIdx),     label, false, ParagraphAlignment.LEFT, "F2F2F2");
        setCellText(row.getCell(labelIdx + 1), value, false, ParagraphAlignment.LEFT, "FFFFFF");
    }

    private void fillEntregaCell(XWPFTableCell cell, String value, String label) {
        configureCell(cell, ParagraphAlignment.LEFT, "FFFFFF", true);
        XWPFParagraph lp = cell.getParagraphs().get(0);
        lp.setSpacingBefore(0); lp.setSpacingAfter(0);
        XWPFRun lr = lp.createRun();
        lr.setBold(true); lr.setFontSize(9); lr.setText(label);
        XWPFParagraph vp = cell.addParagraph();
        vp.setSpacingBefore(0); vp.setSpacingAfter(0);
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
        p.setSpacingBefore(0); p.setSpacingAfter(0);
        byte[] img = readBinaryResource(path);
        if (img.length == 0) return;
        try {
            p.createRun().addPicture(new ByteArrayInputStream(img),
                    XWPFDocument.PICTURE_TYPE_PNG, path, Units.toEMU(wPx), Units.toEMU(hPx));
        } catch (InvalidFormatException e) {
            throw new IOException("Cannot insert image " + path, e);
        }
    }

    private void addCompactText(XWPFTableCell cell, String text, int fontSize) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingBefore(0); p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setFontSize(fontSize); r.setColor("606060"); r.setText(text);
    }

    // ══════════════════════════ UTILITIES ════════════════════════════════════

    private void ensureDefaults(ActaSoftwareRequest req) {
        if (req.getFuncionario()     == null) req.setFuncionario(new ActaSoftwareRequest.Funcionario());
        if (req.getEquipo()          == null) req.setEquipo(new ActaSoftwareRequest.EquipoInfo());
        if (req.getSoftwareItems()   == null) req.setSoftwareItems(new ArrayList<>());
        if (req.getDriversAdicionales() == null) req.setDriversAdicionales(new ArrayList<>());
        if (req.getEntrega()         == null) req.setEntrega(new ActaSoftwareRequest.FirmaRecepcion());
        if (req.getRecibe()          == null) req.setRecibe(new ActaSoftwareRequest.FirmaRecepcion());
        if (n(req.getCertificacion()).isEmpty()) req.setCertificacion(CERTIFICACION_DEFAULT);
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

    private byte[] readBinaryResource(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) return new byte[0];
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private List<String> wrap(String text, float maxW, float fs, PDType1Font font) throws IOException {
        String norm = normalize(text);
        if (norm.isEmpty()) return Arrays.asList("");
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : norm.split("\\s+")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (font.getStringWidth(candidate) / 1000f * fs <= maxW || cur.length() == 0) {
                cur.setLength(0); cur.append(candidate);
            } else {
                lines.add(cur.toString()); cur.setLength(0); cur.append(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private String normalize(String v) {
        if (v == null) return "";
        return v.replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ü","u")
                .replace("Á","A").replace("É","E").replace("Í","I")
                .replace("Ó","O").replace("Ú","U").replace("Ü","U")
                .replace("ñ","n").replace("Ñ","N")
                .replace("°","deg").replace("º","deg").replace("ª","deg")
                .replaceAll("[\\p{Cntrl}]", " ").trim();
    }

    private String n(String s) { return s == null ? "" : s.trim(); }
}
