package com.studyflow.controllers;

import com.studyflow.models.Deck;
import com.studyflow.services.DeckService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class RevisionsController implements Initializable {

    @FXML private VBox listView;
    @FXML private Label totalDecksLabel;
    @FXML private Label masteredLabel;
    @FXML private Label dueReviewLabel;
    @FXML private Label streakLabel;
    @FXML private FlowPane decksGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button exportPdfBtn;
    @FXML private StackPane donutChartPane;
    @FXML private StackPane lineChartPane;
    @FXML private StackPane barChartPane;
    @FXML private VBox formView;
    @FXML private Label formTitle;
    @FXML private TextField fieldTitre;
    @FXML private TextField fieldMatiere;
    @FXML private ComboBox<String> combNiveau;
    @FXML private TextArea fieldDescription;
    @FXML private TextField fieldImage;
    @FXML private TextField fieldPdf;
    @FXML private Label imagePreviewLabel;
    @FXML private Label pdfPreviewLabel;
    @FXML private Label errTitre, errMatiere, errNiveau, errImage, errPdf;

    private final DeckService deckService = new DeckService();
    private List<Deck> decks;
    private Deck selectedDeck = null;
    private final Map<Integer, Double> masteryMap = new HashMap<>();
    private final Map<Object, Popup> popupMap = new HashMap<>();

    /**
     * When true, all inline validation listeners are completely silent.
     * Set to true while loading edit-form data, reset to false after.
     */
    private boolean suppressValidation = false;

    private static final String[] COLORS = {"primary","success","warning","accent","danger"};
    private static final String[] ICONS  = {"fth-database","fth-trending-up","fth-terminal","fth-grid","fth-wifi","fth-cpu","fth-hard-drive","fth-check-square"};

    private static final String BORDER_DEFAULT = "#334155";
    private static final String BORDER_ERROR   = "#FB7185";
    private static final String BORDER_OK      = "#34D399";
    private static final String FIELD_STYLE_BASE =
            "-fx-background-color:#1E293B;-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                    "-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:11 16;-fx-font-size:13px;";
    private static final String COMBO_STYLE_BASE =
            "-fx-background-color:#1E293B;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;";

    // PDF column X positions
    private static final int COL_NUM   = 50;
    private static final int COL_TITLE = 80;
    private static final int COL_SUBJ  = 250;
    private static final int COL_LEVEL = 370;
    private static final int COL_PDF   = 480;

    // ─────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (combNiveau != null)
            combNiveau.setItems(FXCollections.observableArrayList("Beginner","Intermediate","Advanced"));
        if (fieldDescription != null)
            fieldDescription.setStyle("-fx-control-inner-background:#1E293B;-fx-background-color:#1E293B;" +
                    "-fx-text-fill:#F8FAFC;-fx-prompt-text-fill:#475569;" +
                    "-fx-border-color:#334155;-fx-border-width:1.5;-fx-border-radius:10;" +
                    "-fx-background-radius:10;-fx-font-size:13px;");
        initFormListeners();
        if (searchField != null && searchField.getParent() != null) {
            searchField.getParent().setOnMouseClicked(event -> searchField.requestFocus());
        }
        if (listView != null) { goToList(); setupSearchFilter(); refreshAll(); }
    }

    private void initFormListeners() {
        showErr(errTitre,false); showErr(errMatiere,false); showErr(errNiveau,false);
        showErr(errImage,false); showErr(errPdf,false);
        setBorderNeutral(fieldTitre); setBorderNeutral(fieldMatiere);
        setBorderComboNeutral(combNiveau); setBorderNeutral(fieldImage); setBorderNeutral(fieldPdf);

        if (fieldTitre   != null) fieldTitre.textProperty().addListener((o,ov,nv)   -> { if (!suppressValidation) validateTitreInline(); });
        if (fieldMatiere != null) fieldMatiere.textProperty().addListener((o,ov,nv) -> { if (!suppressValidation) validateMatiereInline(); });
        if (combNiveau   != null) combNiveau.valueProperty().addListener((o,ov,nv)  -> { if (!suppressValidation) validateNiveauInline(); });
        if (fieldImage   != null) fieldImage.textProperty().addListener((o,ov,nv)   -> { if (!suppressValidation) validateImageInline(); });
        if (fieldPdf     != null) fieldPdf.textProperty().addListener((o,ov,nv)     -> { if (!suppressValidation) validatePdfInline(); });
    }

    // ══════════════════════════════════════════════
    // EXPORT PDF
    // ══════════════════════════════════════════════
    @FXML public void exportDecksToPdf() {
        if (decks == null || decks.isEmpty()) { showErrorPopup(exportPdfBtn,"No decks to export."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export decks to PDF");
        fc.setInitialFileName("decks_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))+".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files","*.pdf"));
        javafx.stage.Window win = (exportPdfBtn!=null&&exportPdfBtn.getScene()!=null)?exportPdfBtn.getScene().getWindow():null;
        File file = (win!=null)?fc.showSaveDialog(win):null;
        if (file==null) return;
        try {
            generateMinimalPdf(file,decks);
            showSuccessPopup("PDF exported: "+file.getName());
            try { if(Desktop.isDesktopSupported()) Desktop.getDesktop().open(file); } catch(Exception ignored){}
        } catch(Exception e) { showErrorPopup(exportPdfBtn,"Error: "+e.getMessage()); }
    }

    private void generateMinimalPdf(File file, List<Deck> list) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            List<String> pages = buildPdfPages(list);
            List<Long> offsets = new ArrayList<>();
            byte[] hdr = "%PDF-1.4\n".getBytes(); fos.write(hdr); long pos = hdr.length;
            offsets.add(pos);
            byte[] o1 = "1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n".getBytes();
            fos.write(o1); pos+=o1.length;
            StringBuilder kids = new StringBuilder();
            for(int i=0;i<pages.size();i++) kids.append((3+i*2)).append(" 0 R ");
            offsets.add(pos);
            byte[] o2 = ("2 0 obj\n<</Type/Pages/Kids["+kids+"]/Count "+pages.size()+">>\nendobj\n").getBytes();
            fos.write(o2); pos+=o2.length;
            for(int i=0;i<pages.size();i++){
                int pn=3+i*2,cn=4+i*2;
                byte[] content=pages.get(i).getBytes("ISO-8859-1");
                offsets.add(pos);
                byte[] pageObj=(pn+" 0 obj\n<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Contents "+cn+" 0 R/Resources<</Font<</F1<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>/F2<</Type/Font/Subtype/Type1/BaseFont/Helvetica-Bold>>>>>>\n>>\nendobj\n").getBytes();
                fos.write(pageObj); pos+=pageObj.length;
                offsets.add(pos);
                byte[] sh=(cn+" 0 obj\n<</Length "+content.length+">>\nstream\n").getBytes();
                fos.write(sh); pos+=sh.length; fos.write(content); pos+=content.length;
                byte[] se="\nendstream\nendobj\n".getBytes(); fos.write(se); pos+=se.length;
            }
            long xrefPos=pos; int total=2+pages.size()*2+1;
            StringBuilder xref=new StringBuilder("xref\n0 "+total+"\n0000000000 65535 f \n");
            for(long off:offsets) xref.append(String.format("%010d 00000 n \n",off));
            fos.write(xref.toString().getBytes());
            fos.write(("trailer\n<</Size "+total+"/Root 1 0 R>>\nstartxref\n"+xrefPos+"\n%%EOF\n").getBytes());
        }
    }

    private List<String> buildPdfPages(List<Deck> list) {
        List<String> pages = new ArrayList<>();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        final int ROW_H=22,START_Y=750,MIN_Y=60,HEADER_H=80;
        int pageIdx=0,y=START_Y; StringBuilder page=null;
        for(int i=0;i<list.size();i++){
            if(page==null||y<MIN_Y){
                if(page!=null){appendPageFooter(page,pageIdx);pages.add(page.toString());}
                pageIdx++; page=buildPageHeader(date,list.size(),pageIdx); y=START_Y-HEADER_H;
            }
            Deck d=list.get(i);
            if(i%2==0) page.append("0.97 0.97 0.97 rg\n").append(COL_NUM).append(" ").append(y-5).append(" 495 ").append(ROW_H).append(" re f\n0 g\n");
            String num=String.valueOf(i+1);
            String titre=sanitizePdf(truncateStr(safe(d.getTitre()),22));
            String mat  =sanitizePdf(truncateStr(safe(d.getMatiere()),16));
            String niv  =sanitizePdf(truncateStr(safe(d.getNiveau()),14));
            boolean hp  =d.getPdf()!=null&&!d.getPdf().isEmpty();
            int ty=y+10;
            page.append("BT /F1 10 Tf 0 g ").append(COL_NUM  ).append(" ").append(ty).append(" Td (").append(num  ).append(") Tj ET\n");
            page.append("BT /F2 10 Tf 0 g ").append(COL_TITLE).append(" ").append(ty).append(" Td (").append(titre).append(") Tj ET\n");
            page.append("BT /F1 10 Tf 0 g ").append(COL_SUBJ ).append(" ").append(ty).append(" Td (").append(mat  ).append(") Tj ET\n");
            page.append("BT /F1 10 Tf 0 g ").append(COL_LEVEL).append(" ").append(ty).append(" Td (").append(niv  ).append(") Tj ET\n");
            if(hp) page.append("BT /F2 9 Tf 0.2 0.6 0.2 rg ").append(COL_PDF).append(" ").append(ty).append(" Td ([PDF]) Tj ET\n");
            page.append("0.85 0.85 0.85 rg\n").append(COL_NUM).append(" ").append(y-5).append(" 495 0.5 re f\n0 g\n");
            y-=ROW_H;
        }
        if(page!=null){appendPageFooter(page,pageIdx);pages.add(page.toString());}
        return pages;
    }

    private StringBuilder buildPageHeader(String date,int total,int pageNum){
        StringBuilder sb=new StringBuilder();
        sb.append("0.486 0.227 0.929 rg\n0 800 595 45 re f\n");
        sb.append("BT /F2 18 Tf 1 1 1 rg 50 813 Td (RLIFE - Decks List) Tj ET\n0 g\n");
        sb.append("BT /F1 9 Tf 0.4 0.4 0.4 rg 50 790 Td (Exported: ").append(date).append("   |   Total: ").append(total).append(" deck(s)   |   Page ").append(pageNum).append(") Tj ET\n");
        sb.append("0.13 0.13 0.2 rg\n").append(COL_NUM).append(" 765 495 20 re f\n");
        sb.append("BT /F2 10 Tf 1 1 1 rg ").append(COL_NUM  ).append(" 770 Td (#) Tj ET\n");
        sb.append("BT /F2 10 Tf 1 1 1 rg ").append(COL_TITLE).append(" 770 Td (Title) Tj ET\n");
        sb.append("BT /F2 10 Tf 1 1 1 rg ").append(COL_SUBJ ).append(" 770 Td (Subject) Tj ET\n");
        sb.append("BT /F2 10 Tf 1 1 1 rg ").append(COL_LEVEL).append(" 770 Td (Level) Tj ET\n");
        sb.append("BT /F2 10 Tf 1 1 1 rg ").append(COL_PDF  ).append(" 770 Td (PDF) Tj ET\n");
        sb.append("0 g\n");
        return sb;
    }

    private void appendPageFooter(StringBuilder page,int pageIdx){
        page.append("0.5 0.5 0.5 rg\n50 30 495 0.5 re f\n0 g\n");
        page.append("BT /F1 8 Tf 0.5 0.5 0.5 rg 50 18 Td (RLIFE - Page ").append(pageIdx).append(") Tj ET\n");
        page.append("BT /F1 8 Tf 0.5 0.5 0.5 rg 480 18 Td (rlife.app) Tj ET\n");
    }

    private String truncateStr(String s,int max){ if(s==null||s.isEmpty())return"";return s.length()<=max?s:s.substring(0,max-1)+"."; }
    private String sanitizePdf(String s){ if(s==null)return"";return s.replace("\\","").replace("(","[").replace(")","]").replace("\n"," ").replace("\r",""); }

    // ══════════════════════════════════════════════
    // POPUPS
    // ══════════════════════════════════════════════
    private void showErrorPopup(javafx.scene.Node anchor, String msg) {
        if(anchor==null) return; hidePopup(anchor);
        Platform.runLater(()->{
            if(anchor.getScene()==null||anchor.getScene().getWindow()==null) return;
            HBox c=new HBox(10); c.setAlignment(Pos.CENTER_LEFT); c.setPadding(new Insets(10,16,10,14)); c.setMaxWidth(380);
            c.setStyle("-fx-background-color:#1A0A14;-fx-background-radius:12;-fx-border-color:#FB7185;-fx-border-radius:12;-fx-border-width:1.5;-fx-effect:dropshadow(gaussian,rgba(251,113,133,0.55),18,0,0,5);");
            Label icon=new Label("⚠"); icon.setStyle("-fx-text-fill:#FB7185;-fx-font-size:14px;-fx-font-weight:700;");
            Label lbl=new Label(msg); lbl.setStyle("-fx-text-fill:#FECDD3;-fx-font-size:12px;-fx-font-weight:600;"); lbl.setWrapText(true); lbl.setMaxWidth(320);
            c.getChildren().addAll(icon,lbl);
            Popup popup=new Popup(); popup.setAutoHide(true); popup.getContent().add(c);
            javafx.geometry.Bounds b=anchor.localToScreen(anchor.getBoundsInLocal());
            if(b!=null) popup.show(anchor.getScene().getWindow(),b.getMinX(),b.getMaxY()+5);
            popupMap.put(anchor,popup);
            FadeTransition fade=new FadeTransition(Duration.millis(700),c);
            fade.setFromValue(1.0); fade.setToValue(0.0);
            fade.setOnFinished(e->{popup.hide();popupMap.remove(anchor);});
            new SequentialTransition(new PauseTransition(Duration.seconds(3)),fade).play();
        });
    }

    private void showSuccessPopup(String msg){
        Platform.runLater(()->{
            javafx.scene.Node anchor=exportPdfBtn;
            if(anchor==null||anchor.getScene()==null) return;
            HBox c=new HBox(10); c.setAlignment(Pos.CENTER_LEFT); c.setPadding(new Insets(12,20,12,16)); c.setMaxWidth(420);
            c.setStyle("-fx-background-color:#0A1F14;-fx-background-radius:14;-fx-border-color:#34D399;-fx-border-radius:14;-fx-border-width:1.5;-fx-effect:dropshadow(gaussian,rgba(52,211,153,0.5),20,0,0,5);");
            Label icon=new Label("✓"); icon.setStyle("-fx-text-fill:#34D399;-fx-font-size:15px;-fx-font-weight:700;");
            Label lbl=new Label(msg); lbl.setStyle("-fx-text-fill:#A7F3D0;-fx-font-size:13px;-fx-font-weight:600;");
            c.getChildren().addAll(icon,lbl);
            Popup popup=new Popup(); popup.setAutoHide(true); popup.getContent().add(c);
            javafx.stage.Window win=anchor.getScene().getWindow();
            popup.show(win,win.getX()+(win.getWidth()-420)/2.0,win.getY()+win.getHeight()-100);
            FadeTransition fade=new FadeTransition(Duration.millis(600),c);
            fade.setFromValue(1.0); fade.setToValue(0.0); fade.setOnFinished(e->popup.hide());
            new SequentialTransition(new PauseTransition(Duration.seconds(2.5)),fade).play();
        });
    }

    private void hidePopup(javafx.scene.Node anchor){ if(anchor==null)return; Popup p=popupMap.remove(anchor); if(p!=null)p.hide(); }
    private void hideAllPopups(){ new ArrayList<>(popupMap.values()).forEach(Popup::hide); popupMap.clear(); }

    // ══════════════════════════════════════════════
    // VALIDATION
    // ══════════════════════════════════════════════
    private void showErr(Label l,boolean s){ if(l==null)return; l.setVisible(s); l.setManaged(s); }
    private void setBorderNeutral(TextField f){ if(f==null)return; f.setStyle(FIELD_STYLE_BASE+"-fx-border-color:"+BORDER_DEFAULT+";"); }
    private void setBorderComboNeutral(ComboBox<?>c){ if(c==null)return; c.setStyle(COMBO_STYLE_BASE+"-fx-border-color:"+BORDER_DEFAULT+";"); }
    private void setBorder(TextField f,boolean ok){ if(f==null)return; f.setStyle(FIELD_STYLE_BASE+"-fx-border-color:"+(ok?BORDER_OK:BORDER_ERROR)+";"); }
    private void setBorderCombo(ComboBox<?>c,boolean ok){ if(c==null)return; c.setStyle(COMBO_STYLE_BASE+"-fx-border-color:"+(ok?BORDER_OK:BORDER_ERROR)+";"); }

    @FXML public void validateTitreInline(){
        if(fieldTitre==null||suppressValidation)return;
        String t=fieldTitre.getText().trim();
        if(t.isEmpty()){setBorderNeutral(fieldTitre);showErr(errTitre,false);return;}
        if(t.length()<Deck.TITRE_MIN_LENGTH){String m="Min "+Deck.TITRE_MIN_LENGTH+" chars ("+t.length()+"/"+Deck.TITRE_MIN_LENGTH+")";setBorder(fieldTitre,false);if(errTitre!=null)errTitre.setText("⚠ "+m);showErr(errTitre,true);showErrorPopup(fieldTitre,m);}
        else if(t.length()>Deck.TITRE_MAX_LENGTH){String m="Max "+Deck.TITRE_MAX_LENGTH+" chars exceeded";setBorder(fieldTitre,false);if(errTitre!=null)errTitre.setText("⚠ "+m);showErr(errTitre,true);showErrorPopup(fieldTitre,m);}
        else{setBorder(fieldTitre,true);showErr(errTitre,false);hidePopup(fieldTitre);}
    }

    @FXML public void validateMatiereInline(){
        if(fieldMatiere==null||suppressValidation)return;
        String t=fieldMatiere.getText().trim();
        if(t.isEmpty()){setBorderNeutral(fieldMatiere);showErr(errMatiere,false);return;}
        if(t.length()<Deck.MATIERE_MIN_LENGTH){String m="Min "+Deck.MATIERE_MIN_LENGTH+" chars";setBorder(fieldMatiere,false);if(errMatiere!=null)errMatiere.setText("⚠ "+m);showErr(errMatiere,true);showErrorPopup(fieldMatiere,m);}
        else if(t.length()>Deck.MATIERE_MAX_LENGTH){String m="Max "+Deck.MATIERE_MAX_LENGTH+" exceeded";setBorder(fieldMatiere,false);if(errMatiere!=null)errMatiere.setText("⚠ "+m);showErr(errMatiere,true);showErrorPopup(fieldMatiere,m);}
        else{setBorder(fieldMatiere,true);showErr(errMatiere,false);hidePopup(fieldMatiere);}
    }

    @FXML public void validateNiveauInline(){
        if(combNiveau==null||suppressValidation)return;
        boolean ok=combNiveau.getValue()!=null;
        setBorderCombo(combNiveau,ok);
        if(!ok){showErr(errNiveau,true);showErrorPopup(combNiveau,"Please select a level.");}
        else{showErr(errNiveau,false);hidePopup(combNiveau);}
    }

    @FXML public void validateImageInline(){
        if(fieldImage==null||suppressValidation)return;
        String path=fieldImage.getText().trim();
        if(path.isEmpty()){setBorderNeutral(fieldImage);showErr(errImage,false);return;}
        File f=new File(path); String low=f.getName().toLowerCase();
        boolean extOk=Deck.IMAGE_EXTENSIONS.stream().anyMatch(e->low.endsWith("."+e));
        if(!extOk){String m="Invalid format. Use: "+String.join(", ",Deck.IMAGE_EXTENSIONS);setBorder(fieldImage,false);if(errImage!=null)errImage.setText("⚠ "+m);showErr(errImage,true);showErrorPopup(fieldImage,m);}
        else if(f.exists()&&f.length()>Deck.FILE_MAX_BYTES){String m="Image too large. Max: "+Deck.FILE_MAX_SIZE_MB+"MB";setBorder(fieldImage,false);if(errImage!=null)errImage.setText("⚠ "+m);showErr(errImage,true);showErrorPopup(fieldImage,m);}
        else{setBorder(fieldImage,true);showErr(errImage,false);hidePopup(fieldImage);}
    }

    @FXML public void validatePdfInline(){
        if(fieldPdf==null||suppressValidation)return;
        String path=fieldPdf.getText().trim();
        if(path.isEmpty()){setBorderNeutral(fieldPdf);showErr(errPdf,false);return;}
        File f=new File(path);
        if(!f.getName().toLowerCase().endsWith(".pdf")){String m="Only PDF files are accepted.";setBorder(fieldPdf,false);if(errPdf!=null)errPdf.setText("⚠ "+m);showErr(errPdf,true);showErrorPopup(fieldPdf,m);}
        else if(f.exists()&&f.length()>Deck.FILE_MAX_BYTES){String m="PDF too large. Max: "+Deck.FILE_MAX_SIZE_MB+"MB";setBorder(fieldPdf,false);if(errPdf!=null)errPdf.setText("⚠ "+m);showErr(errPdf,true);showErrorPopup(fieldPdf,m);}
        else{setBorder(fieldPdf,true);showErr(errPdf,false);hidePopup(fieldPdf);}
    }

    private boolean validateAll(){
        suppressValidation=false; // ensure full validation runs
        validateTitreInline(); validateMatiereInline(); validateNiveauInline();
        if(fieldImage!=null&&fieldImage.getText().trim().isEmpty()){setBorder(fieldImage,false);String m="Image required";if(errImage!=null)errImage.setText("⚠ "+m);showErr(errImage,true);showErrorPopup(fieldImage,m);}
        else validateImageInline();
        if(fieldPdf!=null&&fieldPdf.getText().trim().isEmpty()){setBorder(fieldPdf,false);String m="PDF required";if(errPdf!=null)errPdf.setText("⚠ "+m);showErr(errPdf,true);showErrorPopup(fieldPdf,m);}
        else validatePdfInline();
        return fieldTitre!=null&&fieldTitre.getText().trim().length()>=Deck.TITRE_MIN_LENGTH
                &&fieldMatiere!=null&&fieldMatiere.getText().trim().length()>=Deck.MATIERE_MIN_LENGTH
                &&combNiveau!=null&&combNiveau.getValue()!=null
                &&fieldImage!=null&&!fieldImage.getText().trim().isEmpty()&&(errImage==null||!errImage.isVisible())
                &&fieldPdf!=null&&!fieldPdf.getText().trim().isEmpty()&&(errPdf==null||!errPdf.isVisible());
    }

    // ══════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════
    private void goToList(){ if(listView==null||formView==null)return; listView.setVisible(true);listView.setManaged(true);formView.setVisible(false);formView.setManaged(false); }
    private void goToForm(){ if(listView==null||formView==null)return; listView.setVisible(false);listView.setManaged(false);formView.setVisible(true);formView.setManaged(true); }

    @FXML public void showAddView(){ selectedDeck=null; clearForm(); if(formTitle!=null)formTitle.setText("New Deck"); goToForm(); }

    @FXML public void handleCancel(){ hideAllPopups(); clearForm(); goToList(); }

    /**
     * Opens edit form WITHOUT triggering any validation popups.
     * suppressValidation blocks all listeners during field population.
     */
    private void openEditForm(Deck d) {
        selectedDeck = d;

        // Step 1 – block all listeners
        suppressValidation = true;

        // Step 2 – clear any lingering popups / errors
        hideAllPopups();
        showErr(errTitre,false); showErr(errMatiere,false); showErr(errNiveau,false);
        showErr(errImage,false); showErr(errPdf,false);
        setBorderNeutral(fieldTitre); setBorderNeutral(fieldMatiere);
        setBorderComboNeutral(combNiveau); setBorderNeutral(fieldImage); setBorderNeutral(fieldPdf);
        if(imagePreviewLabel!=null){imagePreviewLabel.setVisible(false);imagePreviewLabel.setManaged(false);}
        if(pdfPreviewLabel!=null)  {pdfPreviewLabel.setVisible(false);  pdfPreviewLabel.setManaged(false);}

        // Step 3 – populate fields (listeners fire but suppressValidation==true → no-op)
        if(formTitle   !=null) formTitle.setText("Edit Deck");
        if(fieldTitre  !=null) fieldTitre.setText(d.getTitre());
        if(fieldMatiere!=null) fieldMatiere.setText(d.getMatiere());
        if(combNiveau  !=null) combNiveau.setValue(d.getNiveau());
        if(fieldDescription!=null) fieldDescription.setText(safe(d.getDescription()));
        if(fieldImage!=null&&d.getImage()!=null&&!d.getImage().isEmpty()){
            fieldImage.setText(d.getImage());
            setFileLabel(imagePreviewLabel,"✓ "+new File(d.getImage()).getName());
        }
        if(fieldPdf!=null&&d.getPdf()!=null&&!d.getPdf().isEmpty()){
            fieldPdf.setText(d.getPdf());
            setFileLabel(pdfPreviewLabel,"✓ "+new File(d.getPdf()).getName());
        }

        // Step 4 – re-enable listeners
        suppressValidation = false;

        // Step 5 – apply silent green borders (no popups)
        applySilentBorderFeedback();

        goToForm();
    }

    /** Show green/neutral borders for pre-filled fields — NO popups at all. */
    private void applySilentBorderFeedback(){
        if(fieldTitre!=null){String t=fieldTitre.getText().trim();if(!t.isEmpty())setBorder(fieldTitre,t.length()>=Deck.TITRE_MIN_LENGTH&&t.length()<=Deck.TITRE_MAX_LENGTH);}
        if(fieldMatiere!=null){String t=fieldMatiere.getText().trim();if(!t.isEmpty())setBorder(fieldMatiere,t.length()>=Deck.MATIERE_MIN_LENGTH&&t.length()<=Deck.MATIERE_MAX_LENGTH);}
        if(combNiveau!=null&&combNiveau.getValue()!=null) setBorderCombo(combNiveau,true);
        if(fieldImage!=null&&!fieldImage.getText().trim().isEmpty()){File f=new File(fieldImage.getText().trim());String low=f.getName().toLowerCase();boolean ok=Deck.IMAGE_EXTENSIONS.stream().anyMatch(e->low.endsWith("."+e));setBorder(fieldImage,ok);}
        if(fieldPdf!=null&&!fieldPdf.getText().trim().isEmpty()) setBorder(fieldPdf,fieldPdf.getText().trim().toLowerCase().endsWith(".pdf"));
    }

    @FXML public void pickImage(){
        if(fieldImage==null||fieldImage.getScene()==null)return;
        FileChooser fc=new FileChooser(); fc.setTitle("Select Cover Image");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg","*.gif","*.bmp","*.webp"),new FileChooser.ExtensionFilter("All","*.*"));
        File f=fc.showOpenDialog(fieldImage.getScene().getWindow());
        if(f!=null){fieldImage.setText(f.getAbsolutePath());setFileLabel(imagePreviewLabel,"✓ "+f.getName());}
    }

    @FXML public void pickPdf(){
        if(fieldPdf==null||fieldPdf.getScene()==null)return;
        FileChooser fc=new FileChooser(); fc.setTitle("Select PDF");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF","*.pdf"),new FileChooser.ExtensionFilter("All","*.*"));
        File f=fc.showOpenDialog(fieldPdf.getScene().getWindow());
        if(f!=null){fieldPdf.setText(f.getAbsolutePath());setFileLabel(pdfPreviewLabel,"✓ "+f.getName());}
    }

    private void setFileLabel(Label l,String t){ if(l==null)return; l.setText(t);l.setVisible(true);l.setManaged(true); }

    @FXML public void handleSave(){
        if(!validateAll())return;
        String titre=fieldTitre.getText().trim(),mat=fieldMatiere.getText().trim(),niv=combNiveau.getValue();
        String desc=fieldDescription!=null?fieldDescription.getText().trim():"";
        String img =fieldImage!=null?fieldImage.getText().trim():"";
        String pdf =fieldPdf  !=null?fieldPdf.getText().trim():"";
        try{
            if(selectedDeck==null){ deckService.add(new Deck(2,titre,mat,niv,desc,img,pdf)); showSuccessPopup("Deck \""+titre+"\" added!"); }
            else{ selectedDeck.setTitre(titre);selectedDeck.setMatiere(mat);selectedDeck.setNiveau(niv);selectedDeck.setDescription(desc);selectedDeck.setImage(img);selectedDeck.setPdf(pdf); deckService.update(selectedDeck); showSuccessPopup("Deck \""+titre+"\" updated!"); }
            clearForm();goToList();if(listView!=null)refreshAll();
        }catch(IllegalArgumentException e){showErrorPopup(fieldTitre,e.getMessage());}
        catch(Exception e){showErrorPopup(fieldTitre,"Error: "+e.getMessage());}
    }

    private void handleDelete(Deck d){
        Alert dlg=new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Delete Deck");dlg.setHeaderText("Delete \""+d.getTitre()+"\"?");dlg.setContentText("This action cannot be undone.");
        Optional<ButtonType> r=dlg.showAndWait();
        if(r.isPresent()&&r.get()==ButtonType.OK){masteryMap.remove(d.getIdDeck());deckService.delete(d);refreshAll();}
    }

    // ══════════════════════════════════════════════
    // REFRESH
    // ══════════════════════════════════════════════
    private void refreshAll(){
        decks=deckService.getAll();
        long mc=masteryMap.values().stream().filter(v->v>=1.0).count();
        if(totalDecksLabel!=null)totalDecksLabel.setText(String.valueOf(decks.size()));
        if(masteredLabel  !=null)masteredLabel.setText(String.valueOf(mc));
        if(dueReviewLabel !=null)dueReviewLabel.setText("0");
        if(streakLabel    !=null)streakLabel.setText("14");
        applySearchFilter();
        Platform.runLater(this::drawCharts);
    }

    private void setupSearchFilter(){
        if(searchField==null)return;
        searchField.textProperty().addListener((obs,o,n)->applySearchFilter());
        if(sortCombo!=null){
            sortCombo.setItems(FXCollections.observableArrayList("Newest","Oldest","Title A-Z","Title Z-A","Subject A-Z"));
            sortCombo.setValue("Newest");
            sortCombo.valueProperty().addListener((obs,o,n)->applySearchFilter());
        }
    }

    private void applySearchFilter(){
        if(decks==null)return;
        String q=(searchField==null||searchField.getText()==null)?"":searchField.getText().trim().toLowerCase(Locale.ROOT);
        Stream<Deck> stream=decks.stream();
        if(!q.isBlank())stream=stream.filter(d->deckSearchText(d).contains(q));
        displayDecks(stream.sorted(getDeckComparator()).toList());
    }

    private Comparator<Deck> getDeckComparator(){
        String v=sortCombo==null||sortCombo.getValue()==null?"Newest":sortCombo.getValue();
        Comparator<Deck> byDate   =Comparator.comparing(Deck::getDateCreation,Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Deck> byTitle  =Comparator.comparing(d->safe(d.getTitre()).toLowerCase(Locale.ROOT));
        Comparator<Deck> bySubject=Comparator.comparing(d->safe(d.getMatiere()).toLowerCase(Locale.ROOT));
        return switch(v){
            case "Oldest"      ->byDate.thenComparingInt(Deck::getIdDeck);
            case "Title A-Z"   ->byTitle.thenComparingInt(Deck::getIdDeck);
            case "Title Z-A"   ->byTitle.reversed().thenComparingInt(Deck::getIdDeck);
            case "Subject A-Z" ->bySubject.thenComparingInt(Deck::getIdDeck);
            default            ->byDate.reversed().thenComparingInt(Deck::getIdDeck);
        };
    }

    private String deckSearchText(Deck d){
        return String.join(" ",safe(d.getTitre()),safe(d.getMatiere()),safe(d.getNiveau()),safe(d.getDescription()),String.valueOf(d.getIdDeck()),d.getDateCreation()==null?"":d.getDateCreation().toString()).toLowerCase(Locale.ROOT);
    }

    // ══════════════════════════════════════════════
    // CARDS
    // ══════════════════════════════════════════════
    private void displayDecks(List<Deck> list){
        if(decksGrid==null)return; decksGrid.getChildren().clear();
        if(list.isEmpty()){Label e=new Label("✨ No decks yet. Click Create Deck to start!");e.setStyle("-fx-text-fill:#64748B;-fx-font-size:13px;");decksGrid.getChildren().add(e);return;}
        for(int i=0;i<list.size();i++) decksGrid.getChildren().add(buildCard(list.get(i),COLORS[i%COLORS.length],ICONS[i%ICONS.length]));
    }

    private VBox buildCard(Deck deck,String color,String iconLit){
        VBox card=new VBox(0); card.setPrefWidth(260);
        String sN="-fx-background-color:#0F172A;-fx-background-radius:16;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),12,0,0,4);";
        String sH="-fx-background-color:#1E293B;-fx-background-radius:16;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),20,0,0,8);";
        card.setStyle(sN); card.setOnMouseEntered(e->card.setStyle(sH)); card.setOnMouseExited(e->card.setStyle(sN));
        StackPane header=new StackPane(); header.setPrefHeight(100);
        header.setStyle("-fx-background-color:"+grad(color)+";-fx-background-radius:16 16 0 0;");
        String imgPath=deck.getImage();
        if(imgPath!=null&&!imgPath.trim().isEmpty()){File f=new File(imgPath.trim());if(f.exists()){try{ImageView iv=new ImageView(new Image(f.toURI().toString()));iv.setFitWidth(260);iv.setFitHeight(100);iv.setPreserveRatio(false);Rectangle clip=new Rectangle(260,100);clip.setArcWidth(32);clip.setArcHeight(32);iv.setClip(clip);Region overlay=new Region();overlay.setStyle("-fx-background-color:rgba(0,0,0,0.28);-fx-background-radius:16 16 0 0;");header.getChildren().addAll(iv,overlay);}catch(Exception ex){addIcon(header,iconLit);}}else addIcon(header,iconLit);}else addIcon(header,iconLit);
        VBox content=new VBox(8); content.setPadding(new Insets(16));
        Label name=new Label(deck.getTitre()); name.setStyle("-fx-text-fill:#F8FAFC;-fx-font-weight:800;-fx-font-size:15px;"); name.setWrapText(true);
        Label sub=new Label(deck.getMatiere()+" • "+deck.getNiveau()); sub.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        double mastery=masteryMap.getOrDefault(deck.getIdDeck(),0.0);
        HBox ph=new HBox(); Label pt=new Label("0/0 mastered"); pt.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:10px;");
        Region sp2=new Region(); HBox.setHgrow(sp2,Priority.ALWAYS);
        Label pc=new Label(String.format("%.0f%%",mastery*100)); pc.setStyle("-fx-text-fill:"+hex(color)+";-fx-font-size:11px;-fx-font-weight:700;");
        ph.getChildren().addAll(pt,sp2,pc);
        ProgressBar bar=new ProgressBar(mastery); bar.setMaxWidth(Double.MAX_VALUE); bar.setPrefHeight(6); bar.setStyle("-fx-accent:"+hex(color)+";");
        content.getChildren().addAll(name,sub,ph,bar);
        if(deck.getDescription()!=null&&!deck.getDescription().isEmpty()){Label desc=new Label(deck.getDescription());desc.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;");desc.setWrapText(true);desc.setMaxHeight(36);content.getChildren().add(desc);}
        HBox acts=new HBox(8); acts.setAlignment(Pos.CENTER_RIGHT); acts.setPadding(new Insets(12,0,0,0));
        boolean alreadyM=mastery>=1.0;
        Button masterBtn=alreadyM?chipBtn("✓ Mastered","rgba(52,211,153,0.20)","#34D399","rgba(52,211,153,0.35)"):chipBtn("★ Master","rgba(251,191,36,0.15)","#FBBF24","rgba(251,191,36,0.3)");
        masterBtn.setOnAction(e->{if(masteryMap.getOrDefault(deck.getIdDeck(),0.0)>=1.0)masteryMap.remove(deck.getIdDeck());else masteryMap.put(deck.getIdDeck(),1.0);if(masteredLabel!=null)masteredLabel.setText(String.valueOf(masteryMap.values().stream().filter(v->v>=1.0).count()));applySearchFilter();});
        Button editBtn=chipBtn("✎ Edit","rgba(99,102,241,0.15)","#818CF8","rgba(99,102,241,0.3)");
        Button delBtn =chipBtn("🗑 Delete","rgba(244,63,94,0.15)","#FB7185","rgba(244,63,94,0.3)");
        editBtn.setOnAction(e->openEditForm(deck)); delBtn.setOnAction(e->handleDelete(deck));
        acts.getChildren().addAll(masterBtn,editBtn,delBtn);
        content.getChildren().add(acts); card.getChildren().addAll(header,content);
        return card;
    }

    private void addIcon(StackPane p,String l){FontIcon i=new FontIcon(l);i.setIconSize(40);i.setIconColor(Color.WHITE);p.getChildren().add(i);}

    private Button chipBtn(String t,String bg,String fg,String bgH){
        String s ="-fx-background-color:"+bg +";-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:5 10;";
        String sh="-fx-background-color:"+bgH+";-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-background-radius:8;-fx-cursor:hand;-fx-font-weight:600;-fx-padding:5 10;";
        Button b=new Button(t); b.setStyle(s); b.setOnMouseEntered(e->b.setStyle(sh)); b.setOnMouseExited(e->b.setStyle(s)); return b;
    }

    private void clearForm(){
        suppressValidation=true;
        if(fieldTitre   !=null)fieldTitre.clear();
        if(fieldMatiere !=null)fieldMatiere.clear();
        if(combNiveau   !=null)combNiveau.setValue(null);
        if(fieldDescription!=null)fieldDescription.clear();
        if(fieldImage   !=null)fieldImage.clear();
        if(fieldPdf     !=null)fieldPdf.clear();
        resetBorder(fieldTitre);resetBorder(fieldMatiere);setBorderComboNeutral(combNiveau);resetBorder(fieldImage);resetBorder(fieldPdf);
        showErr(errTitre,false);showErr(errMatiere,false);showErr(errNiveau,false);showErr(errImage,false);showErr(errPdf,false);
        if(imagePreviewLabel!=null){imagePreviewLabel.setVisible(false);imagePreviewLabel.setManaged(false);}
        if(pdfPreviewLabel  !=null){pdfPreviewLabel.setVisible(false);  pdfPreviewLabel.setManaged(false);}
        hideAllPopups();
        suppressValidation=false;
    }

    private void resetBorder(TextField f){if(f==null)return;f.setStyle(FIELD_STYLE_BASE+"-fx-border-color:"+BORDER_DEFAULT+";");}

    // ══════════════════════════════════════════════
    // CHARTS
    // ══════════════════════════════════════════════
    private void drawCharts(){if(decks==null)return;drawDonut();drawLine();drawBars();}

    private void drawDonut(){
        if(donutChartPane==null)return;
        double w=donutChartPane.getWidth()>0?donutChartPane.getWidth():300,h=donutChartPane.getHeight()>0?donutChartPane.getHeight():160;
        Canvas c=new Canvas(w,h);GraphicsContext g=c.getGraphicsContext2D();
        double pct=decks.isEmpty()?0:Math.min(88.0,60+decks.size()*4.0);
        double cx=w/2,cy=h/2,r=Math.min(w,h)*0.38,thick=r*0.38;
        g.setStroke(Color.web("#1E293B"));g.setLineWidth(thick);g.strokeArc(cx-r,cy-r,r*2,r*2,0,360,javafx.scene.shape.ArcType.OPEN);
        g.setStroke(Color.web("#8B5CF6"));g.setLineWidth(thick);g.strokeArc(cx-r,cy-r,r*2,r*2,90,-(pct/100.0*360),javafx.scene.shape.ArcType.OPEN);
        g.setFill(Color.web("#F8FAFC"));g.setFont(Font.font("System",FontWeight.BOLD,22));g.setTextAlign(TextAlignment.CENTER);g.fillText(String.format("%.0f%%",pct),cx,cy+8);
        g.setFont(Font.font("System",11));g.setFill(Color.web("#64748B"));g.fillText("Success Rate",cx,cy+24);
        donutChartPane.getChildren().setAll(c);
    }

    private void drawLine(){
        if(lineChartPane==null)return;
        double w=lineChartPane.getWidth()>0?lineChartPane.getWidth():300,h=lineChartPane.getHeight()>0?lineChartPane.getHeight():150;
        Canvas c=new Canvas(w,h);GraphicsContext g=c.getGraphicsContext2D();
        String[]days={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        double[][]series={{3,5,4,7,6,8,5},{2,4,6,5,8,7,9},{1,3,2,4,5,6,4}};String[]colors={"#10B981","#8B5CF6","#F59E0B"};
        double padL=30,padR=10,padT=10,padB=30,gw=w-padL-padR,gh=h-padT-padB;int n=days.length;
        g.setStroke(Color.web("#1E293B"));g.setLineWidth(1);
        for(int i=0;i<=5;i++){double y=padT+gh-(i/5.0)*gh;g.strokeLine(padL,y,padL+gw,y);g.setFill(Color.web("#475569"));g.setFont(Font.font("System",10));g.setTextAlign(TextAlignment.RIGHT);g.fillText(String.valueOf(i*2),padL-4,y+4);}
        for(int i=0;i<n;i++){double x=padL+(i/(double)(n-1))*gw;g.setFill(Color.web("#475569"));g.setFont(Font.font("System",10));g.setTextAlign(TextAlignment.CENTER);g.fillText(days[i],x,h-6);}
        for(int s=0;s<series.length;s++){g.setStroke(Color.web(colors[s]));g.setLineWidth(2);g.setLineDashes(s==2?5:0);g.beginPath();for(int i=0;i<n;i++){double x=padL+(i/(double)(n-1))*gw,y=padT+gh-(series[s][i]/10.0)*gh;if(i==0)g.moveTo(x,y);else g.lineTo(x,y);}g.stroke();g.setLineDashes(0);g.setFill(Color.web(colors[s]));for(int i=0;i<n;i++){double x=padL+(i/(double)(n-1))*gw,y=padT+gh-(series[s][i]/10.0)*gh;g.fillOval(x-3,y-3,6,6);}}
        lineChartPane.getChildren().setAll(c);
    }

    private void drawBars(){
        if(barChartPane==null||decks==null||decks.isEmpty())return;
        double w=barChartPane.getWidth()>0?barChartPane.getWidth():300,h=barChartPane.getHeight()>0?barChartPane.getHeight():130;
        Canvas c=new Canvas(w,h);GraphicsContext g=c.getGraphicsContext2D();
        int shown=Math.min(decks.size(),6);double padL=28,padR=8,padT=10,padB=28,gw=w-padL-padR,gh=h-padT-padB;
        double groupW=gw/shown,barW=groupW*0.35;String[]barColors={"#8B5CF6","#F59E0B"};
        g.setStroke(Color.web("#1E293B"));g.setLineWidth(1);
        for(int i=0;i<=4;i++){double y=padT+gh-(i/4.0)*gh;g.strokeLine(padL,y,padL+gw,y);g.setFill(Color.web("#475569"));g.setFont(Font.font("System",9));g.setTextAlign(TextAlignment.RIGHT);g.fillText((i*25)+"%",padL-3,y+3);}
        for(int i=0;i<shown;i++){Deck deck=decks.get(i);String abbr=deck.getTitre().length()>3?deck.getTitre().substring(0,3).toUpperCase():deck.getTitre().toUpperCase();double cx=padL+i*groupW+groupW/2;double realM=masteryMap.getOrDefault(deck.getIdDeck(),0.0)*100;double[]vals={realM>0?realM:(60+(i*7)%40),realM>0?realM:(10+(i*11)%35)};for(int b=0;b<2;b++){double barH=(vals[b]/100.0)*gh,bx=cx+(b==0?-barW-1:1),by=padT+gh-barH;g.setFill(Color.web(barColors[b]));g.fillRoundRect(bx,by,barW,barH,4,4);}g.setFill(Color.web("#64748B"));g.setFont(Font.font("System",9));g.setTextAlign(TextAlignment.CENTER);g.fillText(abbr,cx,h-4);}
        barChartPane.getChildren().setAll(c);
    }

    // ══════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════
    private String safe(String v){return v==null?"":v;}
    private String hex(String c){return switch(c){case"primary"->"#A78BFA";case"success"->"#34D399";case"warning"->"#FBBF24";case"danger"->"#FB7185";case"accent"->"#FB923C";default->"#94A3B8";};}
    private String grad(String c){return switch(c){case"primary"->"linear-gradient(to bottom right,#7C3AED,#8B5CF6)";case"success"->"linear-gradient(to bottom right,#059669,#10B981)";case"warning"->"linear-gradient(to bottom right,#D97706,#F59E0B)";case"danger"->"linear-gradient(to bottom right,#DC2626,#F43F5E)";case"accent"->"linear-gradient(to bottom right,#EA580C,#F97316)";default->"linear-gradient(to bottom right,#475569,#64748B)";};}
}
