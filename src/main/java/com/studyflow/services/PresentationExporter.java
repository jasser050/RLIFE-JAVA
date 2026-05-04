package com.studyflow.services;

import com.studyflow.models.SlideContent;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PresentationExporter {
    private static final Dimension SLIDE_SIZE = new Dimension(1280, 720);

    public void exportToPptx(List<SlideContent> slides, String topic, String theme, File outputFile) throws IOException {
        if (slides == null || slides.isEmpty()) {
            throw new IllegalArgumentException("Aucune slide à exporter.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Le fichier de sortie est invalide.");
        }

        ThemePalette palette = ThemePalette.fromTheme(theme);

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(SLIDE_SIZE);

            createTitleSlide(ppt, topic, theme, palette);
            for (int i = 0; i < slides.size(); i++) {
                createContentSlide(ppt, slides.get(i), i + 2, palette, theme);
            }

            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                ppt.write(outputStream);
            }
        }
    }

    private void createTitleSlide(XMLSlideShow ppt, String topic, String theme, ThemePalette palette) {
        XSLFSlide slide = ppt.createSlide();
        applySlideBackground(slide, palette, true);

        XSLFAutoShape accentBand = slide.createAutoShape();
        accentBand.setShapeType(ShapeType.RECT);
        accentBand.setAnchor(new Rectangle(0, 0, SLIDE_SIZE.width, 120));
        accentBand.setFillColor(palette.accent());
        accentBand.setLineColor(palette.accent());

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(84, 185, 1110, 170));
        XSLFTextParagraph titleParagraph = titleBox.addNewTextParagraph();
        titleParagraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
        XSLFTextRun titleRun = titleParagraph.addNewTextRun();
        titleRun.setText(safeTopic(topic));
        titleRun.setFontFamily("Aptos Display");
        titleRun.setFontSize(40.0);
        titleRun.setBold(true);
        titleRun.setFontColor(palette.titleText());

        XSLFTextBox subtitleBox = slide.createTextBox();
        subtitleBox.setAnchor(new Rectangle(88, 340, 940, 120));
        XSLFTextParagraph subtitleParagraph = subtitleBox.addNewTextParagraph();
        XSLFTextRun subtitleRun = subtitleParagraph.addNewTextRun();
        subtitleRun.setText("Présenté avec RLife Presentation Studio • Thème " + normalizeThemeLabel(theme));
        subtitleRun.setFontFamily("Aptos");
        subtitleRun.setFontSize(20.0);
        subtitleRun.setFontColor(palette.secondaryText());

        XSLFTextBox tagBox = slide.createTextBox();
        tagBox.setAnchor(new Rectangle(88, 500, 680, 90));
        XSLFTextParagraph tagParagraph = tagBox.addNewTextParagraph();
        XSLFTextRun tagRun = tagParagraph.addNewTextRun();
        tagRun.setText("Contenu généré par IA et structuré pour une présentation claire, dense et visuelle.");
        tagRun.setFontFamily("Aptos");
        tagRun.setFontSize(19.0);
        tagRun.setFontColor(palette.bodyText());

        addFooter(slide, 1, palette);
    }

    private void createContentSlide(XMLSlideShow ppt, SlideContent slideContent, int slideNumber, ThemePalette palette, String theme) {
        XSLFSlide slide = ppt.createSlide();
        applySlideBackground(slide, palette, false);

        XSLFAutoShape header = slide.createAutoShape();
        header.setShapeType(ShapeType.RECT);
        header.setAnchor(new Rectangle(0, 0, SLIDE_SIZE.width, 88));
        header.setFillColor(palette.headerFill());
        header.setLineColor(palette.headerFill());

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(72, 22, 980, 50));
        XSLFTextParagraph titleParagraph = titleBox.addNewTextParagraph();
        titleParagraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT);
        XSLFTextRun titleRun = titleParagraph.addNewTextRun();
        titleRun.setText(defaultIfBlank(slideContent.getTitle(), "Slide " + slideNumber));
        titleRun.setFontFamily("Aptos Display");
        titleRun.setFontSize(30.0);
        titleRun.setBold(true);
        titleRun.setFontColor(palette.headerText());

        XSLFTextBox typeBadge = slide.createTextBox();
        typeBadge.setAnchor(new Rectangle(1088, 22, 130, 34));
        XSLFTextParagraph badgeParagraph = typeBadge.addNewTextParagraph();
        badgeParagraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun badgeRun = badgeParagraph.addNewTextRun();
        badgeRun.setText(slideContent.getType().name());
        badgeRun.setFontFamily("Aptos");
        badgeRun.setFontSize(11.0);
        badgeRun.setBold(true);
        badgeRun.setFontColor(palette.headerText());
        typeBadge.setFillColor(palette.badgeFill());
        typeBadge.setLineColor(palette.badgeFill());

        XSLFAutoShape contentPanel = slide.createAutoShape();
        contentPanel.setShapeType(ShapeType.ROUND_RECT);
        contentPanel.setAnchor(new Rectangle(60, 118, 1160, 505));
        contentPanel.setFillColor(palette.panelFill());
        contentPanel.setLineColor(palette.panelBorder());
        contentPanel.setLineWidth(1.25);

        XSLFTextBox bulletsBox = slide.createTextBox();
        bulletsBox.setAnchor(new Rectangle(92, 160, 1020, 410));

        List<String> bulletPoints = slideContent.getBulletPoints();
        if (bulletPoints != null) {
            for (String bulletPoint : bulletPoints) {
                XSLFTextParagraph paragraph = bulletsBox.addNewTextParagraph();
                paragraph.setBullet(true);
                paragraph.setLeftMargin(20.0);
                paragraph.setIndent(-10.0);
                paragraph.setSpaceAfter(12.0);

                XSLFTextRun run = paragraph.addNewTextRun();
                run.setText(compact(bulletPoint, 180));
                run.setFontFamily("Aptos");
                run.setFontSize(20.0);
                run.setFontColor(palette.bodyText());
            }
        }

        XSLFTextBox notesBox = slide.createTextBox();
        notesBox.setPlaceholder(Placeholder.BODY);
        notesBox.setAnchor(new Rectangle(92, 585, 980, 32));
        XSLFTextParagraph notesParagraph = notesBox.addNewTextParagraph();
        XSLFTextRun notesRun = notesParagraph.addNewTextRun();
        notesRun.setText("Note présentateur: " + compact(defaultIfBlank(slideContent.getSpeakerNotes(), "Présenter les idées majeures avec un exemple."), 160));
        notesRun.setFontFamily("Aptos");
        notesRun.setItalic(true);
        notesRun.setFontSize(12.0);
        notesRun.setFontColor(palette.secondaryText());

        if ("Creative".equalsIgnoreCase(theme)) {
            addCreativeAccent(slide, palette);
        } else if ("Bold".equalsIgnoreCase(theme)) {
            addBoldAccent(slide, palette);
        }

        addFooter(slide, slideNumber, palette);
    }

    private void applySlideBackground(XSLFSlide slide, ThemePalette palette, boolean titleSlide) {
        XSLFAutoShape background = slide.createAutoShape();
        background.setShapeType(ShapeType.RECT);
        background.setAnchor(new Rectangle(0, 0, SLIDE_SIZE.width, SLIDE_SIZE.height));
        background.setFillColor(titleSlide ? palette.titleBackground() : palette.background());
        background.setLineColor(titleSlide ? palette.titleBackground() : palette.background());
    }

    private void addCreativeAccent(XSLFSlide slide, ThemePalette palette) {
        XSLFAutoShape orbOne = slide.createAutoShape();
        orbOne.setShapeType(ShapeType.ELLIPSE);
        orbOne.setAnchor(new Rectangle(980, 470, 180, 180));
        orbOne.setFillColor(palette.altAccent());
        orbOne.setLineColor(palette.altAccent());

        XSLFAutoShape orbTwo = slide.createAutoShape();
        orbTwo.setShapeType(ShapeType.ELLIPSE);
        orbTwo.setAnchor(new Rectangle(1035, 520, 120, 120));
        orbTwo.setFillColor(palette.accent());
        orbTwo.setLineColor(palette.accent());
    }

    private void addBoldAccent(XSLFSlide slide, ThemePalette palette) {
        XSLFAutoShape stripe = slide.createAutoShape();
        stripe.setShapeType(ShapeType.RECT);
        stripe.setAnchor(new Rectangle(1125, 118, 12, 505));
        stripe.setFillColor(palette.altAccent());
        stripe.setLineColor(palette.altAccent());
    }

    private void addFooter(XSLFSlide slide, int slideNumber, ThemePalette palette) {
        XSLFTextBox footerBox = slide.createTextBox();
        footerBox.setAnchor(new Rectangle(72, 672, 1140, 22));

        XSLFTextParagraph paragraph = footerBox.addNewTextParagraph();
        paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.RIGHT);

        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText("Slide " + slideNumber);
        run.setFontFamily("Aptos");
        run.setFontSize(12.0);
        run.setFontColor(palette.secondaryText());
    }

    private String safeTopic(String topic) {
        return defaultIfBlank(topic, "Présentation IA");
    }

    private String normalizeThemeLabel(String theme) {
        return defaultIfBlank(theme, "Modern");
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String compact(String value, int maxLength) {
        String normalized = defaultIfBlank(value, "").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private record ThemePalette(
            Color background,
            Color titleBackground,
            Color headerFill,
            Color panelFill,
            Color panelBorder,
            Color accent,
            Color altAccent,
            Color titleText,
            Color headerText,
            Color bodyText,
            Color secondaryText,
            Color badgeFill
    ) {
        private static ThemePalette fromTheme(String theme) {
            String normalized = theme == null ? "modern" : theme.trim().toLowerCase();
            return switch (normalized) {
                case "minimal" -> new ThemePalette(
                        color("#FFFFFF"), color("#F8FAFC"), color("#F1F5F9"), color("#FFFFFF"), color("#CBD5E1"),
                        color("#7F77DD"), color("#D946EF"), color("#0F172A"), color("#0F172A"),
                        color("#1E293B"), color("#64748B"), color("#E2E8F0")
                );
                case "bold" -> new ThemePalette(
                        color("#1A1A2E"), color("#111827"), color("#7F77DD"), color("#16213E"), color("#334155"),
                        color("#7F77DD"), color("#F59E0B"), color("#F8FAFC"), color("#F8FAFC"),
                        color("#F8FAFC"), color("#CBD5E1"), color("#251E58")
                );
                case "academic" -> new ThemePalette(
                        color("#0F172A"), color("#10253D"), color("#1B3A5C"), color("#F8FAFC"), color("#CBD5E1"),
                        color("#1B3A5C"), color("#7F77DD"), color("#F8FAFC"), color("#FFFFFF"),
                        color("#1E293B"), color("#475569"), color("#27496D")
                );
                case "creative" -> new ThemePalette(
                        color("#170E2D"), color("#1E123B"), color("#7F77DD"), color("#241442"), color("#5B4BA6"),
                        color("#7F77DD"), color("#EC4899"), color("#F8FAFC"), color("#F8FAFC"),
                        color("#F8FAFC"), color("#DDD6FE"), color("#34205A")
                );
                default -> new ThemePalette(
                        color("#111827"), color("#141A33"), color("#7F77DD"), color("#16213E"), color("#3F3C8C"),
                        color("#7F77DD"), color("#38BDF8"), color("#F8FAFC"), color("#FFFFFF"),
                        color("#F8FAFC"), color("#CBD5E1"), color("#2D2A63")
                );
            };
        }

        private static Color color(String hex) {
            return new Color(
                    Integer.valueOf(hex.substring(1, 3), 16),
                    Integer.valueOf(hex.substring(3, 5), 16),
                    Integer.valueOf(hex.substring(5, 7), 16)
            );
        }
    }
}