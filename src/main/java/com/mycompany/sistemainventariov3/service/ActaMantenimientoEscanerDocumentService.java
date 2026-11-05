package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.dto.ActaMantenimientoPcRequest;
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
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
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

public class ActaMantenimientoEscanerDocumentService {

    private static final String NS      = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final QName  QGRID   = new QName(NS, "tblGrid");
    private static final QName  QTBLPR  = new QName(NS, "tblPr");
    private static final QName  QLAYOUT = new QName(NS, "tblLayout");

    private static final String TITULO_1 = "SERVICIO NACIONAL DE DERECHOS INTELECTUALES";
    private static final String TITULO_2 = "DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN";
    private static final String TITULO_3 = "FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS";
    private static final String CERTIFICACION_DEFAULT =
            "Certifico que los elementos detallados en el presente documento, me han sido instalados para mi cuidado y custodia " +
            "con el propósito de cumplir con las tareas y asignaciones propias de mi cargo en la Institución, siendo estos de mi " +
            "única y exclusiva responsabilidad. Me comprometo a usar correctamente los recursos, y solo para los fines establecidos, " +
            "a no instalar ni permitir la instalación de software por personal ajeno al área de soporte de DTIC, dado cualquier " +
            "novedad dar conocimiento a los técnicos de DTIC.";
    private static final List<String> ACTIVIDADES = Arrays.asList(
            "LIMPIEZA DE EQUIPO",
            "CALIBRACIÓN",
            "VERIFICACIÓN DE ALIMENTACIÓN",
            "ASPIRACIÓN Y SOPLETEO DE PARTES ELECTRÓNICAS"
    );

    // PDF row heights (pt)
    private static final float H_HDR   = 18f;
    private static final float H_ROW   = 16f;
    private static final float H_DARK  = 22f;
    private static final float H_SUB   = 18f;
    private static final float H_ACT   = 24f;
    private static final float H_ACTL  = 30f;
    private static final float H_ACTO  = 40f;
    private static final float H_SIGN  = 65f;
    private static final float FOOTER_Y = 78f;

    // DOCX row heights (twips)
    private static final int DX_LBL  = 340;
    private static final int DX_DATA = 400;
    private static final int DX_SIGN = 1600;

    // ══════════════════════════ PUBLIC ENTRY POINTS ══════════════════════════════

    public byte[] generarDocx(ActaMantenimientoPcRequest request) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ensureActaDefaults(request);
            configurePage(doc);
            addFondoPagina(doc);
            addDocumentHeader(doc);
            addFuncionarioTable(doc, request);
            addEquiposTable(doc, request);
            addActividadesTable(doc, request);
            addCertificacion(doc, request);
            addEntregaRecepcionTable(doc, request);
            doc.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarPdf(ActaMantenimientoPcRequest request) throws IOException {
        ensureActaDefaults(request);
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pw = PDRectangle.A4.getWidth();
            float ph = PDRectangle.A4.getHeight();
            float mg = 40f;
            float cw = pw - 2 * mg;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float logoY = ph - 28f;
                // Fondo de página (plantilla institucional)
                drawImage(cs, doc, "actas/Fondo_senadi.png", 0f, 0f, pw, ph);

                float y = logoY - 68f;
                y = drawCenteredText(cs, TITULO_1, pw/2f, y, PDType1Font.HELVETICA_BOLD, 15f, Color.BLACK);
                y -= 5f;
                y = drawCenteredText(cs, TITULO_2, pw/2f, y, PDType1Font.HELVETICA_BOLD, 12f, Color.BLACK);
                y -= 10f;
                y = drawCenteredText(cs, TITULO_3, pw/2f, y, PDType1Font.HELVETICA_BOLD, 12f, Color.BLACK, true);
                y -= 14f;

                y = drawFuncionarioSection(cs, mg, y, cw, request);
                y -= 8f;
                y = drawEquiposSection(cs, mg, y, cw, request);
                y -= 10f;
                y = drawActividadesSection(cs, mg, y, cw, request);
                y -= 8f;
                y = drawParagraph(cs, normalize(request.getCertificacion()),
                                  mg + 6f, y, cw - 12f, 8.5f, 11f, 4);
                y -= 8f;
                drawEntregaRecepcionSection(cs, mg, y, cw, request);

            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ══════════════════════════ DOCX SECTION BUILDERS ═══════════════════════════

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
        addCenteredParagraph(doc, TITULO_1, true, 16, false);
        addCenteredParagraph(doc, TITULO_2, true, 15, false);
        addCenteredParagraph(doc, TITULO_3, true, 14, true);
    }

    private void addFuncionarioTable(XWPFDocument doc, ActaMantenimientoPcRequest req) {
        int[] grid = {1100, 3050, 1100, 2550, 1100, 1566};
        XWPFTable t = createStyledTable(doc, 3, 6, grid);
        setRowHeight(t.getRow(0), DX_LBL);
        setRowHeight(t.getRow(1), DX_DATA);
        setRowHeight(t.getRow(2), DX_DATA);
        mergeCellsH(t, 0, 0, 5);
        setCellText(t.getRow(0).getCell(0), "DATOS DEL FUNCIONARIO SENADI", true, ParagraphAlignment.CENTER, "D9D9D9");
        fillLabelValue(t.getRow(1), 0, "NOMBRE",   req.getFuncionario().getNombre());
        fillLabelValue(t.getRow(1), 2, "CARGO",    req.getFuncionario().getCargo());
        fillLabelValue(t.getRow(1), 4, "N° EXT.",  req.getFuncionario().getExtension());
        fillLabelValue(t.getRow(2), 0, "CORREO",   req.getFuncionario().getCorreo());
        fillLabelValue(t.getRow(2), 2, "AREA",     req.getFuncionario().getArea());
        fillLabelValue(t.getRow(2), 4, "EDIFICIO", req.getFuncionario().getEdificio());
    }

    private void addEquiposTable(XWPFDocument doc, ActaMantenimientoPcRequest req) {
        int[] grid = {2800, 2000, 2000, 2000, 1666};
        XWPFTable t = createStyledTable(doc, 3, 5, grid);
        setRowHeight(t.getRow(0), DX_LBL);
        setRowHeight(t.getRow(1), DX_LBL);
        setRowHeight(t.getRow(2), DX_DATA);
        mergeCellsH(t, 0, 0, 4);
        setCellText(t.getRow(0).getCell(0), "EQUIPOS", true, ParagraphAlignment.CENTER, "D9D9D9");
        String[] hdrs = {"TIPO","MARCA","MODELO","SERIAL","CODIGO"};
        for (int i = 0; i < hdrs.length; i++)
            setCellText(t.getRow(1).getCell(i), hdrs[i], true, ParagraphAlignment.CENTER, "E6E6E6");
        fillEquipoRow(t.getRow(2), req.getDesktop());
    }

    private void addActividadesTable(XWPFDocument doc, ActaMantenimientoPcRequest req) {
        int[] grid = {4200, 1700, 1400, 3166};
        XWPFTable t = createStyledTable(doc, 7, 4, grid);
        setRowHeight(t.getRow(0), 480);
        setRowHeight(t.getRow(1), 360);
        setRowHeight(t.getRow(2), 360);
        for (int i = 3; i < 7; i++) setRowHeight(t.getRow(i), 440);

        mergeCellsH(t, 0, 0, 3);
        setCellText(t.getRow(0).getCell(0), "ESCANER", true, ParagraphAlignment.CENTER, "7F7F7F", "FFFFFF", 14);

        mergeCellsV(t, 1, 2, 0);
        setCellText(t.getRow(1).getCell(0), "ACTIVIDADES DE\nMANTENIMIENTOS", true, ParagraphAlignment.CENTER, "D9D9D9");
        mergeCellsH(t, 1, 1, 3);
        setCellText(t.getRow(1).getCell(1), "INSTALADO",   true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(2).getCell(1), "FECHA",       true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(2).getCell(2), "ESTADO",      true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(2).getCell(3), "OBSERVACIÓN", true, ParagraphAlignment.CENTER, "D9D9D9");

        List<ActaMantenimientoPcRequest.ActividadMantenimiento> acts = normalizedActivities(req.getActividades());
        for (int i = 0; i < ACTIVIDADES.size(); i++) {
            XWPFTableRow row = t.getRow(i + 3);
            ActaMantenimientoPcRequest.ActividadMantenimiento a = acts.get(i);
            setCellText(row.getCell(0), ACTIVIDADES.get(i), true,  ParagraphAlignment.LEFT,   "F2F2F2");
            setCellText(row.getCell(1), a.getFecha(),        false, ParagraphAlignment.CENTER, "FFFFFF");
            setCellText(row.getCell(2), a.getEstado(),       false, ParagraphAlignment.CENTER, "FFFFFF");
            setCellText(row.getCell(3), a.getObservacion(),  false, ParagraphAlignment.LEFT,   "FFFFFF");
        }
    }

    private void addCertificacion(XWPFDocument doc, ActaMantenimientoPcRequest req) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);
        p.setSpacingBefore(80);
        p.setSpacingAfter(80);
        XWPFRun r = p.createRun();
        r.setFontSize(9);
        r.setText(normalize(req.getCertificacion()));
    }

    private void addEntregaRecepcionTable(XWPFDocument doc, ActaMantenimientoPcRequest req) {
        int[] grid = {5233, 5233};
        XWPFTable t = createStyledTable(doc, 5, 2, grid);
        setRowHeight(t.getRow(0), 380);
        setRowHeight(t.getRow(1), 340);
        setRowHeight(t.getRow(2), 420);
        setRowHeight(t.getRow(3), DX_SIGN);
        setRowHeight(t.getRow(4), 420);
        mergeCellsH(t, 0, 0, 1);
        setCellText(t.getRow(0).getCell(0), "ENTREGA RECEPCION DE EQUIPO", true, ParagraphAlignment.CENTER, "D9D9D9");
        setCellText(t.getRow(1).getCell(0), "ENTREGA", false, ParagraphAlignment.CENTER, "EDEDED");
        setCellText(t.getRow(1).getCell(1), "RECIBE",  false, ParagraphAlignment.CENTER, "EDEDED");
        fillEntregaCell(t.getRow(2).getCell(0), req.getEntrega().getNombre(), "Nombre:");
        fillEntregaCell(t.getRow(2).getCell(1), req.getRecibe().getNombre(),  "Nombre:");
        fillEntregaCell(t.getRow(3).getCell(0), req.getEntrega().getFirma(),  "Firma:");
        fillEntregaCell(t.getRow(3).getCell(1), req.getRecibe().getFirma(),   "Firma:");
        fillEntregaCell(t.getRow(4).getCell(0), req.getEntrega().getFecha(),  "Fecha:");
        fillEntregaCell(t.getRow(4).getCell(1), req.getRecibe().getFecha(),   "Fecha:");
    }

    // ══════════════════════════ PDF SECTION DRAWERS ══════════════════════════════

    private float drawFuncionarioSection(PDPageContentStream cs, float x, float y, float w,
                                         ActaMantenimientoPcRequest req) throws IOException {
        float[] cols = {55f, 155f, 55f, 140f, 55f, w - 460f};
        y = drawSectionHeader(cs, "DATOS DEL FUNCIONARIO SENADI", x, y, w, H_HDR, new Color(217,217,217), Color.BLACK, 9.5f);
        y = drawKVRow(cs, x, y, cols,
                new String[]{"NOMBRE", req.getFuncionario().getNombre(),
                             "CARGO",  req.getFuncionario().getCargo(),
                             "N° EXT.",req.getFuncionario().getExtension()}, H_ROW);
        y = drawKVRow(cs, x, y, cols,
                new String[]{"CORREO", req.getFuncionario().getCorreo(),
                             "AREA",   req.getFuncionario().getArea(),
                             "EDIFICIO", req.getFuncionario().getEdificio()}, H_ROW);
        return y;
    }

    private float drawEquiposSection(PDPageContentStream cs, float x, float y, float w,
                                     ActaMantenimientoPcRequest req) throws IOException {
        float[] cols = {160f, 70f, 68f, 90f, w - 388f};
        y = drawSectionHeader(cs, "EQUIPOS", x, y, w, H_HDR, new Color(217,217,217), Color.BLACK, 9.5f);
        y = drawRow(cs, x, y, cols, new String[]{"TIPO","MARCA","MODELO","SERIAL","CODIGO"}, H_ROW, true, new Color(230,230,230));
        y = drawRow(cs, x, y, cols,
                new String[]{req.getDesktop().getTipo(), req.getDesktop().getMarca(),
                             req.getDesktop().getModelo(), req.getDesktop().getSerial(),
                             req.getDesktop().getCodigo()}, H_ROW, false, Color.WHITE);
        return y;
    }

    private float drawActividadesSection(PDPageContentStream cs, float x, float y, float w,
                                         ActaMantenimientoPcRequest req) throws IOException {
        float[] actH = {H_ACT, H_ACT, H_ACT, H_ACT};
        float leftW = 175f, dateW = 80f, stW = 68f, obsW = w - leftW - dateW - stW;

        drawFilledRect(cs, x, y - H_DARK, w, H_DARK, new Color(127,127,127));
        drawBorder(cs, x, y - H_DARK, w, H_DARK);
        drawText(cs, "ESCANER", x + w/2f, y - H_DARK*0.65f, PDType1Font.HELVETICA_BOLD, 12f, Color.WHITE, true);
        float cur = y - H_DARK;

        drawFilledRect(cs, x,         cur - H_SUB, leftW,           H_SUB, new Color(217,217,217));
        drawFilledRect(cs, x + leftW, cur - H_SUB, dateW+stW+obsW,  H_SUB, new Color(217,217,217));
        drawBorder(cs, x,         cur - H_SUB, leftW,          H_SUB);
        drawBorder(cs, x + leftW, cur - H_SUB, dateW+stW+obsW, H_SUB);
        drawText(cs, "ACTIVIDADES DE MANTENIMIENTOS", x + leftW/2f,
                 cur - H_SUB*0.62f, PDType1Font.HELVETICA_BOLD, 8f, Color.BLACK, true);
        drawText(cs, "INSTALADO", x + leftW + (dateW+stW+obsW)/2f,
                 cur - H_SUB*0.62f, PDType1Font.HELVETICA_BOLD, 8.5f, Color.BLACK, true);
        cur -= H_SUB;

        for (float[] col : new float[][]{{x, leftW},{x+leftW,dateW},{x+leftW+dateW,stW},{x+leftW+dateW+stW,obsW}}) {
            drawFilledRect(cs, col[0], cur - H_SUB, col[1], H_SUB, new Color(217,217,217));
            drawBorder(cs, col[0], cur - H_SUB, col[1], H_SUB);
        }
        drawText(cs, "FECHA",       x+leftW+dateW/2f,          cur-H_SUB*0.62f, PDType1Font.HELVETICA_BOLD, 8.5f, Color.BLACK, true);
        drawText(cs, "ESTADO",      x+leftW+dateW+stW/2f,      cur-H_SUB*0.62f, PDType1Font.HELVETICA_BOLD, 8.5f, Color.BLACK, true);
        drawText(cs, "OBSERVACIÓN", x+leftW+dateW+stW+obsW/2f, cur-H_SUB*0.62f, PDType1Font.HELVETICA_BOLD, 8.5f, Color.BLACK, true);
        cur -= H_SUB;

        List<ActaMantenimientoPcRequest.ActividadMantenimiento> acts = normalizedActivities(req.getActividades());
        for (int i = 0; i < ACTIVIDADES.size(); i++) {
            float rh = actH[i];
            drawFilledRect(cs, x, cur - rh, leftW, rh, new Color(242,242,242));
            drawBorder(cs, x,                 cur-rh, leftW, rh);
            drawBorder(cs, x+leftW,           cur-rh, dateW, rh);
            drawBorder(cs, x+leftW+dateW,     cur-rh, stW,   rh);
            drawBorder(cs, x+leftW+dateW+stW, cur-rh, obsW,  rh);
            drawParagraphInBox(cs, ACTIVIDADES.get(i),           x+3f,                 cur-5f, leftW-6f, rh-7f, 8f, true);
            drawParagraphInBox(cs, acts.get(i).getFecha(),       x+leftW+3f,           cur-5f, dateW-6f, rh-7f, 8f, false);
            drawParagraphInBox(cs, acts.get(i).getEstado(),      x+leftW+dateW+3f,     cur-5f, stW-6f,   rh-7f, 8f, false);
            drawParagraphInBox(cs, acts.get(i).getObservacion(), x+leftW+dateW+stW+3f, cur-5f, obsW-6f,  rh-7f, 8f, false);
            cur -= rh;
        }
        return cur;
    }

    private float drawEntregaRecepcionSection(PDPageContentStream cs, float x, float y, float w,
                                              ActaMantenimientoPcRequest req) throws IOException {
        float half = w / 2f;
        float[] cols = {half, half};
        y = drawSectionHeader(cs, "ENTREGA RECEPCION DE EQUIPO", x, y, w, H_HDR, new Color(217,217,217), Color.BLACK, 9.5f);
        y = drawRow(cs, x, y, cols, new String[]{"ENTREGA","RECIBE"}, 14f, false, new Color(237,237,237));
        y = drawPairRow(cs, x, y, cols, "Nombre:", req.getEntrega().getNombre(), req.getRecibe().getNombre(), H_ROW);
        y = drawPairRow(cs, x, y, cols, "Firma:",  req.getEntrega().getFirma(),  req.getRecibe().getFirma(),  H_SIGN);
        y = drawPairRow(cs, x, y, cols, "Fecha:",  req.getEntrega().getFecha(),  req.getRecibe().getFecha(),  H_ROW);
        return y;
    }

    // ══════════════════════════ PDF PRIMITIVES ═══════════════════════════════════

    private float drawSectionHeader(PDPageContentStream cs, String txt, float x, float y,
                                    float w, float h, Color fill, Color tc, float fs) throws IOException {
        drawFilledRect(cs, x, y-h, w, h, fill);
        drawBorder(cs, x, y-h, w, h);
        drawText(cs, txt, x+w/2f, y-h*0.65f, PDType1Font.HELVETICA_BOLD, fs, tc, true);
        return y - h;
    }

    private float drawKVRow(PDPageContentStream cs, float x, float y, float[] ws,
                             String[] vs, float h) throws IOException {
        float cx = x;
        for (int i = 0; i < ws.length; i++) {
            boolean lbl = (i % 2 == 0);
            drawFilledRect(cs, cx, y-h, ws[i], h, lbl ? new Color(242,242,242) : Color.WHITE);
            drawBorder(cs, cx, y-h, ws[i], h);
            drawParagraphInBox(cs, vs[i], cx+3f, y-5f, ws[i]-6f, h-7f, 8.5f, lbl);
            cx += ws[i];
        }
        return y - h;
    }

    private float drawRow(PDPageContentStream cs, float x, float y, float[] ws,
                          String[] vs, float h, boolean bold, Color fill) throws IOException {
        float cx = x;
        for (int i = 0; i < ws.length; i++) {
            drawFilledRect(cs, cx, y-h, ws[i], h, fill);
            drawBorder(cs, cx, y-h, ws[i], h);
            drawParagraphInBox(cs, vs[i], cx+3f, y-5f, ws[i]-6f, h-7f, 8.5f, bold);
            cx += ws[i];
        }
        return y - h;
    }

    private float drawPairRow(PDPageContentStream cs, float x, float y, float[] ws,
                               String label, String lv, String rv, float h) throws IOException {
        drawBorder(cs, x,       y-h, ws[0], h);
        drawBorder(cs, x+ws[0], y-h, ws[1], h);
        if (h < 30f) {
            float fs = 8f;
            float ascent  = fs * 0.718f;
            float descent = fs * 0.207f;
            float baseline = y - h / 2f - (ascent - descent) / 2f;
            drawText(cs, label + " " + normalize(lv), x + 4f,         baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
            drawText(cs, label + " " + normalize(rv), x + ws[0] + 4f, baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
        } else {
            drawParagraphInBox(cs, label, x+4f,       y-5f,  ws[0]-8f, 9f,    8.5f, true);
            drawParagraphInBox(cs, lv,    x+4f,       y-16f, ws[0]-8f, h-18f, 8f,   false);
            drawParagraphInBox(cs, label, x+ws[0]+4f, y-5f,  ws[1]-8f, 9f,    8.5f, true);
            drawParagraphInBox(cs, rv,    x+ws[0]+4f, y-16f, ws[1]-8f, h-18f, 8f,   false);
        }
        return y - h;
    }

    private float drawParagraph(PDPageContentStream cs, String text, float x, float y,
                                 float w, float fs, float lh, int maxLines) throws IOException {
        List<String> lines = wrap(text, w, fs, PDType1Font.HELVETICA);
        float ascent = fs * 0.718f;
        float cy = y - ascent;
        int n = 0;
        for (String line : lines) {
            if (n++ >= maxLines) break;
            drawText(cs, line, x, cy, PDType1Font.HELVETICA, fs, Color.BLACK, false);
            cy -= lh;
        }
        return cy + ascent;
    }

    private void drawParagraphInBox(PDPageContentStream cs, String text, float x, float y,
                                    float w, float h, float fs, boolean bold) throws IOException {
        PDType1Font f = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        List<String> lines = wrap(normalize(text), w, fs, f);
        float ascent = fs * 0.718f;
        float lh     = fs * 1.3f;
        float cy     = y - ascent;
        int max = Math.max(1, (int) Math.floor(h / lh));
        for (int i = 0; i < lines.size() && i < max; i++) {
            drawText(cs, lines.get(i), x, cy, f, fs, Color.BLACK, false);
            cy -= lh;
        }
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
        float sx = centered ? x - font.getStringWidth(s) / 1000f * fs / 2f : x;
        cs.beginText();
        cs.setFont(font, fs);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(sx, y);
        cs.showText(s);
        cs.endText();
    }

    private void drawBorder(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private void drawFilledRect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void drawHorizontalLine(PDPageContentStream cs, float x, float y, float w, float lw) throws IOException {
        cs.setStrokingColor(new Color(180, 180, 180));
        cs.setLineWidth(lw);
        cs.moveTo(x, y);
        cs.lineTo(x + w, y);
        cs.stroke();
        cs.setLineWidth(1f);
        cs.setStrokingColor(Color.BLACK);
    }

    private void drawFooterText(PDPageContentStream cs, float x, float y) throws IOException {
        Color grey = new Color(96, 96, 96);
        drawText(cs, "Dirección: Av. República E7-197 y Diego de Almagro – Edificio FORUM 300",
                 x, y,       PDType1Font.HELVETICA, 8f, grey, false);
        drawText(cs, "Código postal: 170518 / Quito – Ecuador  |  Teléfono: +539-2 394 0000",
                 x, y - 11f, PDType1Font.HELVETICA, 8f, grey, false);
        drawText(cs, "www.derechosintelectuales.gob.ec",
                 x, y - 22f, PDType1Font.HELVETICA, 8f, grey, false);
    }

    private void drawImage(PDPageContentStream cs, PDDocument doc, String path,
                            float x, float y, float w, float h) throws IOException {
        byte[] data = readBinaryResource(path);
        if (data.length == 0) return;
        cs.drawImage(PDImageXObject.createFromByteArray(doc, data, path), x, y, w, h);
    }

    // ══════════════════════════ DOCX HELPERS ════════════════════════════════════

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

    private void setRowHeight(XWPFTableRow row, int twips) {
        row.setHeight(twips);
    }

    private void mergeCellsH(XWPFTable table, int rowIdx, int from, int to) {
        XWPFTableRow row = table.getRow(rowIdx);
        XWPFTableCell first = row.getCell(from);
        CTTcPr tcPr = first.getCTTc().isSetTcPr() ? first.getCTTc().getTcPr() : first.getCTTc().addNewTcPr();
        if (tcPr.isSetGridSpan()) tcPr.getGridSpan().setVal(java.math.BigInteger.valueOf(to - from + 1L));
        else                      tcPr.addNewGridSpan().setVal(java.math.BigInteger.valueOf(to - from + 1L));
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
        if (tcPr.isSetVAlign()) tcPr.getVAlign().setVal(STVerticalJc.CENTER);
        else                    tcPr.addNewVAlign().setVal(STVerticalJc.CENTER);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold,
                              ParagraphAlignment align, String bg) {
        setCellText(cell, text, bold, align, bg, null, 9);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold,
                              ParagraphAlignment align, String bg, String fc, int fs) {
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

    private void fillEquipoRow(XWPFTableRow row, ActaMantenimientoPcRequest.EquipoRow eq) {
        setCellText(row.getCell(0), eq.getTipo(),   false, ParagraphAlignment.LEFT, "FFFFFF");
        setCellText(row.getCell(1), eq.getMarca(),  false, ParagraphAlignment.LEFT, "FFFFFF");
        setCellText(row.getCell(2), eq.getModelo(), false, ParagraphAlignment.LEFT, "FFFFFF");
        setCellText(row.getCell(3), eq.getSerial(), false, ParagraphAlignment.LEFT, "FFFFFF");
        setCellText(row.getCell(4), eq.getCodigo(), false, ParagraphAlignment.LEFT, "FFFFFF");
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

    private void addCompactText(XWPFTableCell cell, String text, int fontSize) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setFontSize(fontSize);
        r.setColor("606060");
        r.setText(text);
    }

    private void addCenteredParagraph(XWPFDocument doc, String text, boolean bold, int size, boolean underline) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(0);
        p.setSpacingAfter(underline ? 100 : 20);
        XWPFRun r = p.createRun();
        r.setBold(bold);
        r.setFontSize(size);
        if (underline) r.setUnderline(UnderlinePatterns.SINGLE);
        r.setText(text);
    }

    private void addImageRun(XWPFTableCell cell, String path, int wPx, int hPx) throws IOException {
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        byte[] img = readBinaryResource(path);
        if (img.length == 0) return;
        try {
            p.createRun().addPicture(new ByteArrayInputStream(img),
                    XWPFDocument.PICTURE_TYPE_PNG, path, Units.toEMU(wPx), Units.toEMU(hPx));
        } catch (InvalidFormatException e) {
            throw new IOException("Cannot insert image " + path, e);
        }
    }

    // ══════════════════════════ UTILITY ═════════════════════════════════════════

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

    private List<ActaMantenimientoPcRequest.ActividadMantenimiento> normalizedActivities(
            List<ActaMantenimientoPcRequest.ActividadMantenimiento> incoming) {
        List<ActaMantenimientoPcRequest.ActividadMantenimiento> result = new ArrayList<>();
        for (int i = 0; i < ACTIVIDADES.size(); i++) {
            ActaMantenimientoPcRequest.ActividadMantenimiento item =
                    incoming != null && i < incoming.size() && incoming.get(i) != null
                            ? incoming.get(i) : new ActaMantenimientoPcRequest.ActividadMantenimiento();
            if (item.getActividad() == null || item.getActividad().trim().isEmpty())
                item.setActividad(ACTIVIDADES.get(i));
            result.add(item);
        }
        return result;
    }

    private void ensureActaDefaults(ActaMantenimientoPcRequest req) {
        if (req.getFuncionario() == null) req.setFuncionario(new ActaMantenimientoPcRequest.Funcionario());
        if (req.getDesktop()     == null) req.setDesktop(new ActaMantenimientoPcRequest.EquipoRow());
        if (req.getEntrega()     == null) req.setEntrega(new ActaMantenimientoPcRequest.FirmaRecepcion());
        if (req.getRecibe()      == null) req.setRecibe(new ActaMantenimientoPcRequest.FirmaRecepcion());
        if (req.getActividades() == null) req.setActividades(new ArrayList<>());
        if (req.getCertificacion() == null || req.getCertificacion().trim().isEmpty())
            req.setCertificacion(CERTIFICACION_DEFAULT);
        if (req.getDesktop().getTipo() == null || req.getDesktop().getTipo().trim().isEmpty())
            req.getDesktop().setTipo("ESCANER");
    }

    private List<String> wrap(String text, float maxW, float fs, PDType1Font font) throws IOException {
        String norm = normalize(text);
        if (norm.isEmpty()) return Arrays.asList("");
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : norm.split("\\s+")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (font.getStringWidth(candidate) / 1000f * fs <= maxW || cur.length() == 0) {
                cur.setLength(0);
                cur.append(candidate);
            } else {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private String normalize(String v) {
        return v == null ? "" : v.replace(' ', ' ').trim();
    }
}
