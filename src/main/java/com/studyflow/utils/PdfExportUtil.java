package com.studyflow.utils;

import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentDependency;
import com.studyflow.models.Project;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class PdfExportUtil {
    private static final String APP_NAME = "RLIFE";
    private static final String APP_TAGLINE = "Responsive Lifestyle - Intelligent Framework Enhancement";
    private static final String LOGO_RESOURCE = "/com/studyflow/assets/rlife-logo.png";
    private static final float PAGE_MARGIN = 42f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2);
    private static final float ROW_HEIGHT = 26f;
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PdfExportUtil() {
    }

    public static File chooseExportFile(Window owner, String suggestedName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName(ensurePdfExtension(suggestedName));

        File defaultDirectory = resolveDefaultExportDirectory();
        if (defaultDirectory != null && defaultDirectory.exists() && defaultDirectory.isDirectory()) {
            chooser.setInitialDirectory(defaultDirectory);
        }

        return chooser.showSaveDialog(owner);
    }

    public static File defaultExportFile(String suggestedName) {
        File directory = resolveDefaultExportDirectory();
        if (directory == null) {
            directory = new File(System.getProperty("user.dir"));
        }
        return new File(directory, ensurePdfExtension(suggestedName));
    }

    public static void exportTableReport(
            File file,
            String title,
            String subtitle,
            List<String> summaryLines,
            List<String> headers,
            List<List<String>> rows
    ) throws IOException {
        ensureParentDirectory(file);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject logo = loadLogo(document);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - PAGE_MARGIN;
                y = drawHero(stream, logo, title, subtitle, y);
                y = drawSectionCard(stream, "Executive Summary", buildExecutiveSummary(summaryLines, rows.size()), y - 20f);
                y = drawMetricsBlock(stream, headers, summaryLines, y - 16f);
                y = drawBulletSection(stream, "Highlights", buildHighlights(summaryLines, rows), y - 16f);
                y = drawBulletSection(stream, "Recommendations", buildRecommendations(rows.size()), y - 16f);
                drawTableSection(stream, logo, headers, rows, y - 16f, document);
                drawFooter(stream, title, page);
            }

            document.save(file);
        }
    }

    public static void exportProjectIntelligenceReport(
            File file,
            Project project,
            List<Assignment> assignments,
            List<AssignmentDependency> dependencies,
            String executiveSummary,
            List<String> recommendations
    ) throws IOException {
        ensureParentDirectory(file);
        List<Assignment> safeAssignments = assignments == null ? List.of() : assignments;
        List<AssignmentDependency> safeDependencies = dependencies == null ? List.of() : dependencies;

        List<String> metrics = List.of(
                "Total assignments: " + safeAssignments.size(),
                "Completed: " + safeAssignments.stream().filter(Assignment::isCompleted).count(),
                "In progress: " + safeAssignments.stream().filter(Assignment::isInProgress).count(),
                "Backlog: " + safeAssignments.stream().filter(Assignment::isTodo).count(),
                "Dependencies: " + safeDependencies.size()
        );

        List<String> kanbanBreakdown = buildKanbanBreakdown(safeAssignments);
        List<List<String>> rows = safeAssignments.stream()
                .map(assignment -> List.of(
                        assignment.getTitle(),
                        assignment.getStatus(),
                        assignment.getPriority(),
                        assignment.getComplexityLevel() == null ? "--" : assignment.getComplexityLevel(),
                        assignment.getAiSuggestedDueDate() == null ? "--" : assignment.getAiSuggestedDueDate().toString()
                ))
                .toList();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDImageXObject logo = loadLogo(document);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - PAGE_MARGIN;
                String title = safe(project == null ? null : project.getTitle()) + " Intelligence Report";
                String subtitle = "AI-backed project report for RLIFE";
                y = drawHero(stream, logo, title, subtitle, y);
                y = drawSectionCard(stream, "Executive Summary", sanitize(executiveSummary), y - 20f);
                y = drawMetricsBlock(stream, List.of("Assignments", "Done", "Progress", "Backlog", "Dependencies"), metrics, y - 16f);
                y = drawBulletSection(stream, "Kanban Breakdown", kanbanBreakdown, y - 16f);
                y = drawBulletSection(stream, "Recommendations", recommendations == null || recommendations.isEmpty() ? buildRecommendations(rows.size()) : recommendations, y - 16f);

                List<String> dependencyLines = safeDependencies.isEmpty()
                        ? List.of("No dependencies detected.")
                        : safeDependencies.stream()
                        .limit(4)
                        .map(dep -> safe(dep.getAssignmentTitle()) + " depends on " + safe(dep.getDependsOnTitle()))
                        .toList();
                y = drawBulletSection(stream, "Dependency Highlights", dependencyLines, y - 16f);
                drawTableSection(stream, logo, List.of("Assignment", "Status", "Priority", "Complexity", "AI Due"), rows, y - 16f, document);
                drawFooter(stream, title, page);
            }

            document.save(file);
        }
    }

    private static float drawHero(PDPageContentStream stream, PDImageXObject logo, String title, String subtitle, float y) throws IOException {
        float logoX = PAGE_MARGIN + 16f;
        float logoY = y - 84f;
        float logoSize = 44f;
        float contentX = PAGE_MARGIN + 72f;

        fillRect(stream, PAGE_MARGIN, y - 108f, PAGE_WIDTH, 108f, 25, 128, 120);
        if (logo != null) {
            stream.drawImage(logo, logoX, logoY, logoSize, logoSize);
        }
        writeText(stream, APP_NAME, FONT_BOLD, 16f, contentX, y - 28f, 255, 255, 255);
        writeText(stream, APP_TAGLINE, FONT_REGULAR, 8.5f, contentX, y - 42f, 211, 243, 239);
        writeText(stream, title, FONT_BOLD, 20f, contentX, y - 68f, 255, 255, 255);
        writeText(
                stream,
                subtitle + " | Generated: " + DATE_FORMATTER.format(java.time.LocalDateTime.now()),
                FONT_REGULAR,
                9.5f,
                contentX,
                y - 88f,
                211,
                243,
                239
        );
        return y - 108f;
    }

    private static float drawSectionCard(PDPageContentStream stream, String title, String body, float y) throws IOException {
        float height = 86f;
        fillRect(stream, PAGE_MARGIN, y - height, PAGE_WIDTH, height, 255, 255, 255);
        strokeRect(stream, PAGE_MARGIN, y - height, PAGE_WIDTH, height, 209, 213, 219);
        writeText(stream, title, FONT_BOLD, 14f, PAGE_MARGIN + 16f, y - 28f, 25, 128, 120);
        writeWrappedText(stream, body, FONT_REGULAR, 11f, PAGE_MARGIN + 16f, y - 56f, PAGE_WIDTH - 32f, 17f, 55, 65, 81);
        return y - height;
    }

    private static float drawMetricsBlock(PDPageContentStream stream, List<String> headers, List<String> summaryLines, float y) throws IOException {
        float height = 128f;
        fillRect(stream, PAGE_MARGIN, y - height, PAGE_WIDTH, height, 255, 255, 255);
        strokeRect(stream, PAGE_MARGIN, y - height, PAGE_WIDTH, height, 209, 213, 219);
        writeText(stream, "Key Metrics", FONT_BOLD, 14f, PAGE_MARGIN + 16f, y - 28f, 25, 128, 120);

        float tableX = PAGE_MARGIN + 16f;
        float tableY = y - 56f;
        float tableWidth = PAGE_WIDTH - 32f;
        float colWidth = tableWidth / Math.max(1, summaryLines.size());

        for (int i = 0; i < summaryLines.size(); i++) {
            float x = tableX + (i * colWidth);
            fillRect(stream, x, tableY - 28f, colWidth, 28f, 243, 244, 246);
            strokeRect(stream, x, tableY - 28f, colWidth, 28f, 229, 231, 235);
            writeText(stream, metricTitle(summaryLines.get(i), headers, i), FONT_BOLD, 10f, x + 8f, tableY - 17f, 55, 65, 81);

            fillRect(stream, x, tableY - 64f, colWidth, 36f, 255, 255, 255);
            strokeRect(stream, x, tableY - 64f, colWidth, 36f, 229, 231, 235);
            writeText(stream, metricValue(summaryLines.get(i)), FONT_REGULAR, 11f, x + 8f, tableY - 50f, 55, 65, 81);
        }

        return y - height;
    }

    private static float drawBulletSection(PDPageContentStream stream, String title, List<String> bullets, float y) throws IOException {
        float height = 84f + (Math.max(0, bullets.size() - 2) * 18f);
        fillRect(stream, PAGE_MARGIN, y - height, PAGE_WIDTH, height, 255, 255, 255);
        strokeRect(stream, PAGE_MARGIN, y - height, PAGE_WIDTH, height, 209, 213, 219);
        writeText(stream, title, FONT_BOLD, 14f, PAGE_MARGIN + 16f, y - 28f, 25, 128, 120);

        float lineY = y - 54f;
        for (String bullet : bullets) {
            writeText(stream, "- " + bullet, FONT_REGULAR, 11f, PAGE_MARGIN + 22f, lineY, 55, 65, 81);
            lineY -= 18f;
        }
        return y - height;
    }

    private static float drawTableSection(
            PDPageContentStream stream,
            PDImageXObject logo,
            List<String> headers,
            List<List<String>> rows,
            float y,
            PDDocument document
    ) throws IOException {
        float tableTop = y;
        writeText(stream, "Detailed Records", FONT_BOLD, 14f, PAGE_MARGIN, tableTop - 2f, 25, 128, 120);
        tableTop -= 18f;

        float[] widths = computeColumnWidths(headers.size());
        drawTableHeader(stream, headers, widths, tableTop);
        tableTop -= ROW_HEIGHT;

        boolean alternate = false;
        PDPageContentStream activeStream = stream;
        PDPage currentPage = document.getPage(document.getNumberOfPages() - 1);

        for (List<String> row : rows) {
            if (tableTop < PAGE_MARGIN + 34f) {
                drawFooter(activeStream, "Detailed Records", currentPage);
                activeStream.close();

                currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);
                activeStream = new PDPageContentStream(document, currentPage);
                tableTop = currentPage.getMediaBox().getHeight() - PAGE_MARGIN;
                fillRect(activeStream, PAGE_MARGIN, tableTop - 36f, PAGE_WIDTH, 36f, 25, 128, 120);
                if (logo != null) {
                    activeStream.drawImage(logo, PAGE_MARGIN + 10f, tableTop - 28f, 18f, 18f);
                }
                writeText(activeStream, APP_NAME, FONT_BOLD, 10f, PAGE_MARGIN + 34f, tableTop - 18f, 255, 255, 255);
                writeText(activeStream, "Detailed Records", FONT_BOLD, 13f, PAGE_MARGIN + 108f, tableTop - 22f, 255, 255, 255);
                tableTop -= 50f;
                drawTableHeader(activeStream, headers, widths, tableTop);
                tableTop -= ROW_HEIGHT;
            }

            drawTableRow(activeStream, row, widths, tableTop, alternate);
            tableTop -= ROW_HEIGHT;
            alternate = !alternate;
        }

        if (activeStream != stream) {
            drawFooter(activeStream, "Detailed Records", currentPage);
            activeStream.close();
            return PAGE_MARGIN;
        }

        return tableTop;
    }

    private static void drawTableHeader(PDPageContentStream stream, List<String> headers, float[] widths, float y) throws IOException {
        float x = PAGE_MARGIN;
        for (int i = 0; i < headers.size(); i++) {
            fillRect(stream, x, y - ROW_HEIGHT, widths[i], ROW_HEIGHT, 243, 244, 246);
            strokeRect(stream, x, y - ROW_HEIGHT, widths[i], ROW_HEIGHT, 229, 231, 235);
            writeText(stream, headers.get(i), FONT_BOLD, 10f, x + 8f, y - 16f, 55, 65, 81);
            x += widths[i];
        }
    }

    private static void drawTableRow(PDPageContentStream stream, List<String> row, float[] widths, float y, boolean alternate) throws IOException {
        float x = PAGE_MARGIN;
        for (int i = 0; i < widths.length; i++) {
            int base = alternate ? 249 : 255;
            fillRect(stream, x, y - ROW_HEIGHT, widths[i], ROW_HEIGHT, base, base, base);
            strokeRect(stream, x, y - ROW_HEIGHT, widths[i], ROW_HEIGHT, 229, 231, 235);
            String value = i < row.size() ? row.get(i) : "";
            writeText(stream, trimForCell(value, widths[i]), FONT_REGULAR, 9.5f, x + 8f, y - 16f, 55, 65, 81);
            x += widths[i];
        }
    }

    private static void drawFooter(PDPageContentStream stream, String title, PDPage page) throws IOException {
        float y = PAGE_MARGIN - 10f;
        writeText(stream, APP_NAME + " export", FONT_BOLD, 9f, PAGE_MARGIN, y, 107, 114, 128);
        writeText(stream, sanitize(title), FONT_REGULAR, 9f, PAGE_MARGIN + PAGE_WIDTH - 120f, y, 107, 114, 128);
    }

    private static List<String> buildHighlights(List<String> summaryLines, List<List<String>> rows) {
        List<String> bullets = new ArrayList<>();
        for (String line : summaryLines) {
            bullets.add(line + ".");
        }
        bullets.add("Visible records exported: " + rows.size() + ".");
        return bullets.subList(0, Math.min(3, bullets.size()));
    }

    private static List<String> buildRecommendations(int rowCount) {
        List<String> bullets = new ArrayList<>();
        bullets.add("Review delayed items and re-balance priorities where needed.");
        bullets.add("Keep status updates current to improve dashboard accuracy.");
        bullets.add("Use the exported list as a checkpoint for weekly follow-up.");
        if (rowCount == 0) {
            bullets.clear();
            bullets.add("No visible data was available for recommendations.");
        }
        return bullets;
    }

    private static List<String> buildKanbanBreakdown(List<Assignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of("To Do: 0", "In Progress: 0", "Review: 0", "Completed: 0");
        }
        long todo = assignments.stream().filter(Assignment::isTodo).count();
        long inProgress = assignments.stream().filter(Assignment::isInProgress).count();
        long completed = assignments.stream().filter(Assignment::isCompleted).count();
        long review = assignments.stream().filter(a -> "Review".equalsIgnoreCase(a.getStatus())).count();
        return List.of(
                "To Do: " + todo,
                "In Progress: " + inProgress,
                "Review: " + review,
                "Completed: " + completed
        );
    }

    private static String buildExecutiveSummary(List<String> summaryLines, int rowCount) {
        if (summaryLines == null || summaryLines.isEmpty()) {
            return "This report summarizes the currently visible records exported from " + APP_NAME + ".";
        }
        return summaryLines.get(0) + ", with " + rowCount + " visible record(s) included in this export.";
    }

    private static String metricTitle(String summaryLine, List<String> headers, int index) {
        int separator = summaryLine.indexOf(':');
        if (separator > 0) {
            return sanitize(summaryLine.substring(0, separator));
        }
        if (index < headers.size()) {
            return sanitize(headers.get(index));
        }
        return "Metric";
    }

    private static String metricValue(String summaryLine) {
        int separator = summaryLine.indexOf(':');
        if (separator >= 0 && separator + 1 < summaryLine.length()) {
            return sanitize(summaryLine.substring(separator + 1).trim());
        }
        return sanitize(summaryLine);
    }

    private static float[] computeColumnWidths(int columnCount) {
        if (columnCount == 5) {
            return new float[]{160f, 118f, 86f, 86f, PAGE_WIDTH - 450f};
        }
        float[] widths = new float[columnCount];
        float width = PAGE_WIDTH / Math.max(1, columnCount);
        for (int i = 0; i < columnCount; i++) {
            widths[i] = width;
        }
        return widths;
    }

    private static void writeWrappedText(
            PDPageContentStream stream,
            String text,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            float maxWidth,
            float lineHeight,
            int r,
            int g,
            int b
    ) throws IOException {
        List<String> lines = wrapText(text, font, fontSize, maxWidth);
        float currentY = y;
        for (String line : lines) {
            writeText(stream, line, font, fontSize, x, currentY, r, g, b);
            currentY -= lineHeight;
        }
    }

    private static List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = sanitize(text).split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static void fillRect(PDPageContentStream stream, float x, float y, float width, float height, int r, int g, int b) throws IOException {
        stream.setNonStrokingColor(toColorComponent(r), toColorComponent(g), toColorComponent(b));
        stream.addRect(x, y, width, height);
        stream.fill();
    }

    private static void strokeRect(PDPageContentStream stream, float x, float y, float width, float height, int r, int g, int b) throws IOException {
        stream.setStrokingColor(toColorComponent(r), toColorComponent(g), toColorComponent(b));
        stream.addRect(x, y, width, height);
        stream.stroke();
    }

    private static void writeText(
            PDPageContentStream stream,
            String text,
            PDType1Font font,
            float size,
            float x,
            float y,
            int r,
            int g,
            int b
    ) throws IOException {
        stream.beginText();
        stream.setNonStrokingColor(toColorComponent(r), toColorComponent(g), toColorComponent(b));
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitize(text));
        stream.endText();
    }

    private static String trimForCell(String value, float width) {
        String safe = sanitize(value);
        int maxChars = Math.max(8, (int) (width / 5.4f));
        if (safe.length() <= maxChars) {
            return safe;
        }
        return safe.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace("\n", " ").replace("\r", " ").replace("->", "to");
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{ASCII}]", "");
        return cleaned.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static File resolveDefaultExportDirectory() {
        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists() && desktop.isDirectory()) {
            return desktop;
        }

        File home = new File(System.getProperty("user.home"));
        if (home.exists() && home.isDirectory()) {
            return home;
        }

        return null;
    }

    private static String ensurePdfExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "studyflow-export.pdf";
        }
        return filename.toLowerCase().endsWith(".pdf") ? filename : filename + ".pdf";
    }

    private static void ensureParentDirectory(File file) throws IOException {
        File parent = file == null ? null : file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create export directory: " + parent.getAbsolutePath());
        }
    }

    private static float toColorComponent(int value) {
        return Math.max(0f, Math.min(1f, value / 255f));
    }

    private static PDImageXObject loadLogo(PDDocument document) throws IOException {
        try (InputStream stream = PdfExportUtil.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (stream == null) {
                return null;
            }
            return PDImageXObject.createFromByteArray(document, stream.readAllBytes(), "rlife-logo");
        }
    }
}
