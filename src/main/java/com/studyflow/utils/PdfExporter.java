package com.studyflow.utils;
import com.studyflow.models.EvaluationMatiere;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PdfExporter {



    /**
     * PdfExportService — génère un rapport PDF des évaluations StudyFlow.
     *
     * ✅ ZÉRO dépendance externe — Java pur (java.io uniquement).
     *    Fonctionne avec le pom.xml existant sans aucune modification.
     *
     * Utilisation :
     *   PdfExportService.export(new ArrayList<>(allEvals), this::getNomMatiere, file);
     */

    // ── Interface résolution nom matière ──────────────────────────────────
    @FunctionalInterface
    public interface SubjectNameResolver {
        String resolve(int matiereId);
    }

    // ── Constantes page A4 (points PDF : 1pt = 1/72 pouce) ───────────────
    private static final float PW = 595f;   // A4 width
    private static final float PH = 842f;   // A4 height
    private static final float ML = 40f;    // margin left
    private static final float MR = 40f;    // margin right
    private static final float CW = PW - ML - MR;  // content width

    // ── Couleurs RGB 0-1 ──────────────────────────────────────────────────
    private static final float[] BG_PAGE   = {0.008f, 0.024f, 0.090f};  // #020617
    private static final float[] BG_DARK   = {0.059f, 0.090f, 0.165f};  // #0F172A
    private static final float[] BG_ALT    = {0.118f, 0.161f, 0.231f};  // #1E293B
    private static final float[] C_WHITE   = {0.973f, 0.980f, 0.988f};  // #F8FAFC
    private static final float[] C_MUTED   = {0.580f, 0.639f, 0.722f};  // #94A3B8
    private static final float[] C_PRIMARY = {0.655f, 0.545f, 0.980f};  // #A78BFA
    private static final float[] C_SUCCESS = {0.204f, 0.827f, 0.600f};  // #34D399
    private static final float[] C_WARNING = {0.984f, 0.749f, 0.141f};  // #FBBF24
    private static final float[] C_DANGER  = {0.984f, 0.443f, 0.522f};  // #FB7185
    private static final float[] C_BORDER  = {0.200f, 0.255f, 0.333f};  // #334155

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────────
    // Point d'entrée principal
    // ─────────────────────────────────────────────────────────────────────
    public static void export(List<EvaluationMatiere> evals,
                              SubjectNameResolver resolver,
                              File destination) throws Exception {

        // ── Calcul des pages ──────────────────────────────────────────────
        final float ROW_H       = 22f;
        final float PAGE1_TOP_Y = PH - 40;
        // Espace consommé par header + stats + titre tableau sur page 1
        final float HEADER_SPACE = 6 + 18 + 6 + 14 + 6 + 12 + 16 + 1 + 16 + 56 + 16 + 1 + 14 + 10 + 22;
        final float TABLE_TOP_P1 = PAGE1_TOP_Y - HEADER_SPACE;
        final float TABLE_TOP_PN = PH - 50 - 22;  // pages suivantes
        final float FOOTER_H     = 30f;

        int rowsPage1 = (int) ((TABLE_TOP_P1 - FOOTER_H) / ROW_H);
        int rowsPageN = (int) ((TABLE_TOP_PN - FOOTER_H) / ROW_H);
        rowsPage1 = Math.max(1, rowsPage1);
        rowsPageN = Math.max(1, rowsPageN);

        int totalPages;
        if (evals.isEmpty()) {
            totalPages = 1;
        } else if (evals.size() <= rowsPage1) {
            totalPages = 1;
        } else {
            totalPages = 1 + (int) Math.ceil((double)(evals.size() - rowsPage1) / rowsPageN);
        }

        // ── Structures PDF ────────────────────────────────────────────────
        List<byte[]> objBytes   = new ArrayList<>();
        List<Integer> objOffset = new ArrayList<>();
        objBytes.add(null); // objet 0 = réservé

        // On prépare les flux de contenu page par page
        List<byte[]> pageStreams = new ArrayList<>();
        for (int p = 0; p < totalPages; p++) {
            int start = (p == 0) ? 0 : rowsPage1 + (p - 1) * rowsPageN;
            int end   = (p == 0) ? Math.min(rowsPage1, evals.size())
                    : Math.min(start + rowsPageN, evals.size());
            pageStreams.add(buildPageStream(evals, resolver, start, end,
                    p, totalPages, ROW_H, p == 0 ? TABLE_TOP_P1 : TABLE_TOP_PN));
        }

        // ── Objets PDF ────────────────────────────────────────────────────
        // Obj 1 : Catalog (sera complété après)
        // Obj 2 : Pages  (sera complété après)
        // Obj 3 : Font Helvetica
        // Obj 4 : Font Helvetica-Bold
        // Obj 5..4+N : Page + Stream par page

        // Obj 3 — Font régulier
        objBytes.add(pdfObj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica "
                + "/Encoding /WinAnsiEncoding >>"));
        // Obj 4 — Font bold
        objBytes.add(pdfObj(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold "
                + "/Encoding /WinAnsiEncoding >>"));

        // Obj 5+ : paires (Page, Stream) pour chaque page
        int firstPageObj = 5;
        List<Integer> pageObjIds = new ArrayList<>();
        for (int p = 0; p < totalPages; p++) {
            int pageObjId   = firstPageObj + p * 2;
            int streamObjId = pageObjId + 1;
            pageObjIds.add(pageObjId);

            byte[] stream = pageStreams.get(p);
            // Objet Page
            objBytes.add(pdfObj(pageObjId,
                    "<< /Type /Page /Parent 2 0 R "
                            + "/MediaBox [0 0 " + fmt(PW) + " " + fmt(PH) + "] "
                            + "/Contents " + streamObjId + " 0 R "
                            + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>"));
            // Objet Stream
            objBytes.add(pdfObjStream(streamObjId, stream));
        }

        // Obj 2 — Pages
        StringBuilder kids = new StringBuilder("[");
        for (int id : pageObjIds) kids.append(id).append(" 0 R ");
        kids.append("]");
        objBytes.add(1, pdfObj(2, "<< /Type /Pages /Kids " + kids
                + " /Count " + totalPages + " >>"));

        // Obj 1 — Catalog
        objBytes.add(1, pdfObj(1, "<< /Type /Catalog /Pages 2 0 R >>"));

        // ── Écriture du fichier ────────────────────────────────────────────
        // Renuméroter les objets dans l'ordre d'insertion
        // Ordre final : 1=Catalog, 2=Pages, 3=FontR, 4=FontB, 5+= pages
        // On reconstruit proprement :
        List<byte[]> orderedObjs = new ArrayList<>();
        orderedObjs.add(null); // slot 0
        orderedObjs.add(pdfObj(1, "<< /Type /Catalog /Pages 2 0 R >>"));
        orderedObjs.add(pdfObj(2, "<< /Type /Pages /Kids " + kids
                + " /Count " + totalPages + " >>"));
        orderedObjs.add(pdfObj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica "
                + "/Encoding /WinAnsiEncoding >>"));
        orderedObjs.add(pdfObj(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold "
                + "/Encoding /WinAnsiEncoding >>"));
        for (int p = 0; p < totalPages; p++) {
            int pageObjId   = firstPageObj + p * 2;
            int streamObjId = pageObjId + 1;
            byte[] stream   = pageStreams.get(p);
            orderedObjs.add(pdfObj(pageObjId,
                    "<< /Type /Page /Parent 2 0 R "
                            + "/MediaBox [0 0 " + fmt(PW) + " " + fmt(PH) + "] "
                            + "/Contents " + streamObjId + " 0 R "
                            + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>"));
            orderedObjs.add(pdfObjStream(streamObjId, stream));
        }

        try (OutputStream out = new FileOutputStream(destination)) {
            List<Integer> offsets = new ArrayList<>();
            byte[] header = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1);
            out.write(header);
            int offset = header.length;

            for (int i = 1; i < orderedObjs.size(); i++) {
                offsets.add(offset);
                out.write(orderedObjs.get(i));
                offset += orderedObjs.get(i).length;
            }

            // Cross-reference table
            int xrefOffset = offset;
            int objCount   = orderedObjs.size(); // includes slot 0
            StringBuilder xref = new StringBuilder();
            xref.append("xref\n0 ").append(objCount).append("\n");
            xref.append("0000000000 65535 f \n");
            for (int o : offsets) {
                xref.append(String.format("%010d 00000 n \n", o));
            }
            xref.append("trailer\n<< /Size ").append(objCount)
                    .append(" /Root 1 0 R >>\n");
            xref.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");
            out.write(xref.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Construction du flux de contenu d'une page
    // ─────────────────────────────────────────────────────────────────────
    private static byte[] buildPageStream(List<EvaluationMatiere> evals,
                                          SubjectNameResolver resolver,
                                          int start, int end,
                                          int pageIdx, int totalPages,
                                          float rowH, float tableTopY) {

        PdfStream s = new PdfStream();

        // ── Fond de page ──────────────────────────────────────────────────
        s.rect(0, 0, PW, PH, BG_PAGE, true);

        float y = PH - 40;

        // ── Page 1 : en-tête complet ──────────────────────────────────────
        if (pageIdx == 0) {
            // Barre décorative
            s.rect(ML, y - 6, CW, 6, C_PRIMARY, true);
            y -= 24;

            // Titre
            s.text("StudyFlow", "F2", 24, ML, y, C_PRIMARY);
            y -= 28;
            s.text("Assessment Report", "F2", 16, ML, y, C_WHITE);
            y -= 20;

            String genDate = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            s.text("Generated: " + genDate + "  |  " + evals.size() + " assessment(s)",
                    "F1", 9, ML, y, C_MUTED);
            y -= 16;
            s.line(ML, y, PW - MR, y, C_BORDER, 0.5f);
            y -= 18;

            // ── Stats ─────────────────────────────────────────────────────
            double avg  = evals.stream().mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(0);
            long   gaps = evals.stream().filter(e -> e.getScoreEval() < 10).count();
            double best = evals.stream().mapToDouble(EvaluationMatiere::getScoreEval).max().orElse(0);

            float tw = (CW - 9) / 4f;
            float th = 52f;
            drawStatTile(s, ML,              y, tw, th, String.valueOf(evals.size()), "Total Assessments",  C_PRIMARY);
            drawStatTile(s, ML + tw + 3,     y, tw, th, String.format("%.1f", avg),  "Average Score /20",  C_SUCCESS);
            drawStatTile(s, ML + 2*(tw+3),   y, tw, th, String.valueOf(gaps),        "Gaps (score < 10)",  C_DANGER);
            drawStatTile(s, ML + 3*(tw+3),   y, tw, th, String.format("%.1f", best), "Best Score",         C_WARNING);
            y -= th + 14;

            s.line(ML, y, PW - MR, y, C_BORDER, 0.5f);
            y -= 14;
            s.text("Detailed Assessments", "F2", 11, ML, y, C_WHITE);
            y -= 12;

        } else {
            // Pages suivantes : mini en-tête
            s.text("StudyFlow  -  Assessment Report  (page " + (pageIdx + 1) + "/" + totalPages + ")",
                    "F2", 11, ML, y, C_PRIMARY);
            y -= 20;
        }

        // ── En-têtes colonnes ─────────────────────────────────────────────
        String[] headers = {"Subject", "Score", "/Max", "%", "Duration", "Priority", "Date"};
        float[]  cw      = {cw(32), cw(10), cw(10), cw(9), cw(12), cw(13), cw(14)};

        s.rect(ML, y - rowH + 6, CW, rowH, BG_DARK, true);
        s.line(ML, y - rowH + 6, PW - MR, y - rowH + 6, C_PRIMARY, 1.5f);

        float xc = ML + 5;
        for (int i = 0; i < headers.length; i++) {
            s.text(headers[i], "F2", 8, xc, y - 3, C_PRIMARY);
            xc += cw[i];
        }
        y -= rowH;

        // ── Lignes de données ─────────────────────────────────────────────
        for (int idx = start; idx < end; idx++) {
            EvaluationMatiere ev = evals.get(idx);
            float[] rowBg = ((idx % 2) == 0) ? BG_DARK : BG_ALT;

            s.rect(ML, y - rowH + 6, CW, rowH, rowBg, true);
            s.line(ML, y - rowH + 6, PW - MR, y - rowH + 6, C_BORDER, 0.3f);

            double pct     = ev.getNoteMaximaleEval() > 0
                    ? (ev.getScoreEval() / ev.getNoteMaximaleEval()) * 100 : 0;
            float[] scoreCol = pct >= 70 ? C_SUCCESS : pct >= 50 ? C_WARNING : C_DANGER;
            float[] prioCol  = priorityColor(ev.getPrioriteE());

            String subj = trunc(resolver.resolve(ev.getMatiereId()), 26);
            String[] row = {
                    subj,
                    String.format("%.1f", ev.getScoreEval()),
                    String.format("%.0f", ev.getNoteMaximaleEval()),
                    String.format("%.0f%%", pct),
                    ev.getDureeEvaluation() + " min",
                    ev.getPrioriteE() != null ? ev.getPrioriteE() : "-",
                    ev.getDateEvaluation() != null ? ev.getDateEvaluation().format(FMT) : "-"
            };
            float[][]  colors = {C_WHITE, scoreCol, C_MUTED, scoreCol, C_MUTED, prioCol, C_MUTED};
            String[]   fonts  = {"F1","F2","F1","F2","F1","F2","F1"};

            xc = ML + 5;
            for (int i = 0; i < row.length; i++) {
                s.text(row[i], fonts[i], 8, xc, y - 3, colors[i]);
                xc += cw[i];
            }
            y -= rowH;
        }

        // ── Pied de page ──────────────────────────────────────────────────
        s.line(ML, 28, PW - MR, 28, C_BORDER, 0.5f);
        String footer = "StudyFlow  -  Academic Assessment Report  -  Page "
                + (pageIdx + 1) + " / " + totalPages;
        s.text(footer, "F1", 7, PW / 2f - 80, 16, C_MUTED);

        return s.toBytes();
    }

    // ── Tuile statistique ─────────────────────────────────────────────────
    private static void drawStatTile(PdfStream s, float x, float y,
                                     float w, float h,
                                     String value, String label, float[] accent) {
        s.rect(x, y - h, w, h, BG_ALT, true);
        s.rect(x, y - h, 3, h, accent, true);           // barre latérale
        s.text(value, "F2", 18, x + 10, y - 24, accent);
        s.text(label, "F1",  7, x + 10, y - 40, C_MUTED);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Classe interne PdfStream — génère les opérateurs PDF bas niveau
    // ─────────────────────────────────────────────────────────────────────
    private static class PdfStream {
        private final StringBuilder sb = new StringBuilder();

        void rect(float x, float y, float w, float h, float[] color, boolean fill) {
            sb.append(fmt(color[0])).append(' ')
                    .append(fmt(color[1])).append(' ')
                    .append(fmt(color[2]));
            if (fill) sb.append(" rg\n"); else sb.append(" RG\n");
            sb.append(fmt(x)).append(' ').append(fmt(y)).append(' ')
                    .append(fmt(w)).append(' ').append(fmt(h)).append(" re\n");
            if (fill) sb.append("f\n"); else sb.append("S\n");
        }

        void line(float x1, float y1, float x2, float y2, float[] color, float width) {
            sb.append(fmt(width)).append(" w\n");
            sb.append(fmt(color[0])).append(' ')
                    .append(fmt(color[1])).append(' ')
                    .append(fmt(color[2])).append(" RG\n");
            sb.append(fmt(x1)).append(' ').append(fmt(y1)).append(" m\n");
            sb.append(fmt(x2)).append(' ').append(fmt(y2)).append(" l\nS\n");
        }

        void text(String str, String font, float size, float x, float y, float[] color) {
            sb.append(fmt(color[0])).append(' ')
                    .append(fmt(color[1])).append(' ')
                    .append(fmt(color[2])).append(" rg\n");
            sb.append("BT\n");
            sb.append('/').append(font).append(' ').append(fmt(size)).append(" Tf\n");
            sb.append(fmt(x)).append(' ').append(fmt(y)).append(" Td\n");
            sb.append('(').append(escapePdf(str)).append(") Tj\n");
            sb.append("ET\n");
        }

        byte[] toBytes() {
            return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers PDF bas niveau
    // ─────────────────────────────────────────────────────────────────────

    private static byte[] pdfObj(int id, String dict) {
        String s = id + " 0 obj\n" + dict + "\nendobj\n";
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] pdfObjStream(int id, byte[] content) throws Exception {
        String header = id + " 0 obj\n<< /Length " + content.length + " >>\nstream\n";
        String footer = "\nendstream\nendobj\n";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(header.getBytes(StandardCharsets.ISO_8859_1));
        baos.write(content);
        baos.write(footer.getBytes(StandardCharsets.ISO_8859_1));
        return baos.toByteArray();
    }

    /** Échappe les caractères spéciaux dans une chaîne littérale PDF. */
    private static String escapePdf(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(')       sb.append("\\(");
            else if (c == ')')  sb.append("\\)");
            else if (c == '\\') sb.append("\\\\");
            else if (c > 127)   sb.append('?');   // Latin-1 safe
            else                sb.append(c);
        }
        return sb.toString();
    }

    private static String fmt(float f) {
        if (f == (int) f) return String.valueOf((int) f);
        return String.format(Locale.US, "%.2f", f);
    }

    private static float cw(float pct) { return CW * pct / 100f; }

    private static float[] priorityColor(String p) {
        if (p == null) return C_MUTED;
        return switch (p) {
            case "Haute", "High"     -> C_DANGER;
            case "Moyenne", "Medium" -> C_WARNING;
            case "Basse", "Low"      -> C_SUCCESS;
            default                   -> C_MUTED;
        };
    }

    private static String trunc(String s, int max) {
        if (s == null) return "-";
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
