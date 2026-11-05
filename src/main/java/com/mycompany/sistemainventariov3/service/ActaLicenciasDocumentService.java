package com.mycompany.sistemainventariov3.service;

import com.mycompany.sistemainventariov3.dto.ActaMantenimientoPcRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PDF del Acta de Licencias generado en el servidor (texto real, igual que
 * las demás actas), para que tenga la misma calidad que el acta de PC.
 * Reutiliza el request de las actas de mantenimiento: la licencia viaja en
 * "desktop" (tipo, marca, modelo, serial, código). El Word de licencias se
 * sigue generando en el navegador.
 */
public class ActaLicenciasDocumentService {

    private static final String TITULO_1 = "SERVICIO NACIONAL DE DERECHOS INTELECTUALES";
    private static final String TITULO_2 = "DIRECCIÓN DE TECNOLOGÍAS DE LA INFORMACIÓN Y COMUNICACIÓN";
    private static final String TITULO_3 = "FORMULARIO DE MANTENIMIENTO PREVENTIVO DE EQUIPOS";
    // Dos párrafos separados por una línea en blanco ("\n\n")
    private static final String CERTIFICACION_DEFAULT =
            "Certifico que las licencias detalladas en el presente documento me han sido entregadas para mi uso y " +
            "responsabilidad, con el propósito de cumplir con las tareas y asignaciones propias de mi cargo en la " +
            "Institución, y que las mismas se encuentran instaladas en el equipo informático asignado a mi cargo." +
            "\n\n" +
            "Me comprometo a utilizar correctamente estos recursos únicamente para los fines institucionales " +
            "establecidos, a no ceder, compartir ni permitir su uso por personal no autorizado, y a notificar " +
            "oportunamente a la Dirección de Tecnologías de la Información y Comunicación (DTIC) ante cualquier " +
            "novedad relacionada con las mismas.";

    // PDF row heights (pt): el acta de licencias tiene menos secciones que la de PC
    private static final float H_HDR  = 18f;
    private static final float H_ROW  = 20f;
    private static final float H_SIGN = 80f;

    public byte[] generarPdf(ActaMantenimientoPcRequest request) throws IOException {
        ensureActaDefaults(request);
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pw  = PDRectangle.A4.getWidth();   // 595.28
            float ph  = PDRectangle.A4.getHeight();  // 841.89
            float mg  = 40f;
            float cw  = pw - 2 * mg;                // 515.28

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Fondo de página (plantilla institucional)
                drawImage(cs, doc, "actas/Fondo_senadi.png", 0f, 0f, pw, ph);

                // ── Titles ───────────────────────────────────────────────────
                float y = ph - 96f;
                y = drawCenteredText(cs, TITULO_1, pw/2f, y, PDType1Font.HELVETICA_BOLD, 15f, Color.BLACK, false);
                y -= 5f;
                y = drawCenteredText(cs, TITULO_2, pw/2f, y, PDType1Font.HELVETICA_BOLD, 12f, Color.BLACK, false);
                y -= 10f;
                y = drawCenteredText(cs, TITULO_3, pw/2f, y, PDType1Font.HELVETICA_BOLD, 12f, Color.BLACK, true);
                y -= 16f;

                // ── Content sections ─────────────────────────────────────────
                y = drawFuncionarioSection(cs, mg, y, cw, request);
                y -= 10f;
                y = drawEquiposSection(cs, mg, y, cw, request);
                y -= 12f;
                // Cada párrafo de la certificación por separado (se dividen por la línea en blanco)
                String[] parrafos = request.getCertificacion().trim().split("\\r?\\n\\s*\\n");
                for (int i = 0; i < parrafos.length; i++) {
                    if (i > 0) y -= 6f;
                    y = drawParagraph(cs, normalize(parrafos[i]), mg + 2f, y, cw - 4f, 9f, 12.5f, 8);
                }
                y -= 12f;
                drawEntregaRecepcionSection(cs, mg, y, cw, request);
            }
            doc.save(out);
            return out.toByteArray();
        }
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
                             "ÁREA",   req.getFuncionario().getArea(),
                             "EDIFICIO", req.getFuncionario().getEdificio()}, H_ROW);
        return y;
    }

    private float drawEquiposSection(PDPageContentStream cs, float x, float y, float w,
                                     ActaMantenimientoPcRequest req) throws IOException {
        float[] cols = {85f, 95f, 120f, 120f, w - 420f};
        y = drawSectionHeader(cs, "EQUIPOS", x, y, w, H_HDR, new Color(217,217,217), Color.BLACK, 9.5f);
        y = drawRow(cs, x, y, cols, new String[]{"TIPO","MARCA","MODELO","SERIAL","CÓDIGO"}, H_ROW, true, new Color(230,230,230));
        // Una fila por cada licencia del equipo; si no llega la lista se usa la licencia de "desktop"
        List<ActaMantenimientoPcRequest.EquipoRow> licencias =
                req.getLicencias() != null && !req.getLicencias().isEmpty()
                        ? req.getLicencias() : Arrays.asList(req.getDesktop());
        for (ActaMantenimientoPcRequest.EquipoRow licencia : licencias) {
            String tipo = licencia.getTipo() == null || licencia.getTipo().trim().isEmpty() ? "LICENCIA" : licencia.getTipo();
            y = drawRow(cs, x, y, cols,
                    new String[]{tipo, licencia.getMarca(), licencia.getModelo(),
                                 licencia.getSerial(), licencia.getCodigo()}, 26f, false, Color.WHITE);
        }
        return y;
    }

    private float drawEntregaRecepcionSection(PDPageContentStream cs, float x, float y, float w,
                                              ActaMantenimientoPcRequest req) throws IOException {
        float half = w / 2f;
        float[] cols = {half, half};
        y = drawSectionHeader(cs, "ENTREGA RECEPCIÓN DE EQUIPO", x, y, w, H_HDR, new Color(217,217,217), Color.BLACK, 9.5f);
        y = drawRow(cs, x, y, cols, new String[]{"ENTREGA","RECIBE"}, 16f, true, new Color(237,237,237));
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
            drawParagraphInBox(cs, vs[i], cx+3f, y-6f, ws[i]-6f, h-8f, 8.5f, lbl);
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
            drawParagraphInBox(cs, vs[i], cx+3f, y-6f, ws[i]-6f, h-8f, 8.5f, bold);
            cx += ws[i];
        }
        return y - h;
    }

    private float drawPairRow(PDPageContentStream cs, float x, float y, float[] ws,
                               String label, String lv, String rv, float h) throws IOException {
        drawBorder(cs, x,        y-h, ws[0], h);
        drawBorder(cs, x+ws[0], y-h, ws[1], h);
        if (h < 30f) {
            // Fila compacta (Nombre/Fecha): etiqueta y valor en una línea
            float fs = 8.5f;
            float ascent  = fs * 0.718f;
            float descent = fs * 0.207f;
            float baseline = y - h / 2f - (ascent - descent) / 2f;
            String lt = label + " " + normalize(lv);
            String rt = label + " " + normalize(rv);
            drawText(cs, lt, x + 4f,         baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
            drawText(cs, rt, x + ws[0] + 4f, baseline, PDType1Font.HELVETICA, fs, Color.BLACK, false);
        } else {
            // Fila alta (Firma): etiqueta arriba y espacio para firmar
            drawParagraphInBox(cs, label, x+4f,          y-5f,  ws[0]-8f, 9f,    8.5f, true);
            drawParagraphInBox(cs, lv,    x+4f,          y-16f, ws[0]-8f, h-18f, 8f,   false);
            drawParagraphInBox(cs, label, x+ws[0]+4f,    y-5f,  ws[1]-8f, 9f,    8.5f, true);
            drawParagraphInBox(cs, rv,    x+ws[0]+4f,    y-16f, ws[1]-8f, h-18f, 8f,   false);
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

    private void drawImage(PDPageContentStream cs, PDDocument doc, String path,
                            float x, float y, float w, float h) throws IOException {
        byte[] data = readBinaryResource(path);
        if (data.length == 0) return;
        cs.drawImage(PDImageXObject.createFromByteArray(doc, data, path), x, y, w, h);
    }

    // ══════════════════════════ UTILITY ═════════════════════════════════════════

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

    private void ensureActaDefaults(ActaMantenimientoPcRequest req) {
        if (req.getFuncionario() == null) req.setFuncionario(new ActaMantenimientoPcRequest.Funcionario());
        if (req.getDesktop()     == null) req.setDesktop(new ActaMantenimientoPcRequest.EquipoRow());
        if (req.getEntrega()     == null) req.setEntrega(new ActaMantenimientoPcRequest.FirmaRecepcion());
        if (req.getRecibe()      == null) req.setRecibe(new ActaMantenimientoPcRequest.FirmaRecepcion());
        if (req.getCertificacion() == null || req.getCertificacion().trim().isEmpty())
            req.setCertificacion(CERTIFICACION_DEFAULT);
        if (req.getDesktop().getTipo() == null || req.getDesktop().getTipo().trim().isEmpty())
            req.getDesktop().setTipo("LICENCIA");
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
