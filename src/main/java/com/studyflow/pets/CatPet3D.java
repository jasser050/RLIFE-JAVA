package com.studyflow.pets;

import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.util.Duration;

/**
 * Chat 3D chibi — style "peluche assise" (inspiré de la référence).
 *
 *  Proportions :
 *    • Tête = ~70 % du corps (très grosse, dôme arrondi)
 *    • Corps = boule basse et large, aplatie en hauteur
 *    • Queue épaisse qui s'enroule SUR LE CÔTÉ du corps
 *    • Pattes avant = deux petits moignons ronds visibles devant
 *    • Pattes arrière = disques plats de chaque côté du bas du corps
 *    • Yeux = très grands, ovales, très expressifs
 *    • Nez minuscule, museau rond saillant
 */
public class CatPet3D extends Group {

    private final Color furColor;
    private final Color bellyColor;
    private final Color eyeColor;

    private Group bodyGroup;
    private Group headGroup;
    private Group tailGroup;

    private RotateTransition tailSway;
    private ScaleTransition   breathe;
    private RotateTransition  headTilt;

    // ════════════════════════════════════════════════════════════════════════
    //  Constructeurs
    // ════════════════════════════════════════════════════════════════════════

    public CatPet3D() {
        this(Color.rgb(215, 125, 38), Color.rgb(70, 195, 85));
    }

    public CatPet3D(Color furColor, Color eyeColor) {
        this.furColor   = furColor;
        this.bellyColor = furColor.interpolate(Color.rgb(255, 248, 220), 0.62);
        this.eyeColor   = eyeColor;
        buildCat();
        startAnimations();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Matériaux
    // ════════════════════════════════════════════════════════════════════════

    private PhongMaterial mat(Color c) {
        PhongMaterial m = new PhongMaterial(c);
        m.setSpecularColor(Color.WHITE);
        m.setSpecularPower(48);
        return m;
    }

    private PhongMaterial matSoft(Color c) {
        PhongMaterial m = new PhongMaterial(c);
        m.setSpecularColor(Color.color(0.5, 0.5, 0.5));
        m.setSpecularPower(10);
        return m;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Assemblage
    // ════════════════════════════════════════════════════════════════════════

    private void buildCat() {
        bodyGroup = buildBody();
        tailGroup = buildTail();
        Group paws = buildPaws();
        headGroup = buildHead();
        getChildren().addAll(bodyGroup, tailGroup, paws, headGroup);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CORPS — boule basse et large (style "pain de mie")
    // ════════════════════════════════════════════════════════════════════════

    private Group buildBody() {
        Group g = new Group();

        Sphere torso = new Sphere(56);
        torso.setMaterial(mat(furColor));
        // Très aplati en hauteur, large en X → boulette posée au sol
        torso.getTransforms().add(new Scale(1.10, 0.75, 1.00));

        // Ventre clair ovale, bas-avant
        Sphere belly = new Sphere(38);
        belly.setMaterial(matSoft(bellyColor));
        belly.getTransforms().add(new Scale(0.82, 0.55, 0.38));
        belly.setTranslateZ(-36);
        belly.setTranslateY(4);

        g.getChildren().addAll(torso, belly);
        g.setTranslateY(30);
        return g;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TÊTE — dôme très arrondi, ~70 % du corps
    // ════════════════════════════════════════════════════════════════════════

    private Group buildHead() {
        Group g = new Group();

        // Crâne — grande sphère légèrement ovale
        Sphere cranium = new Sphere(50);
        cranium.setMaterial(mat(furColor));
        cranium.getTransforms().add(new Scale(1.08, 1.00, 0.96));

        // Museau saillant (petite bosse ronde)
        Sphere muzzle = new Sphere(22);
        muzzle.setMaterial(matSoft(bellyColor));
        muzzle.getTransforms().add(new Scale(1.00, 0.70, 0.62));
        muzzle.setTranslateY(14);
        muzzle.setTranslateZ(-44);

        // Joues rondes et gonflées
        Sphere cheekL = makeCheek(-36);
        Sphere cheekR = makeCheek( 36);

        // Oreilles
        Group earL = buildEar(-1);
        Group earR = buildEar( 1);

        // Yeux très grands
        Group eyeL = buildEye();
        eyeL.setTranslateX(-19); eyeL.setTranslateY(-12); eyeL.setTranslateZ(-46);

        Group eyeR = buildEye();
        eyeR.setTranslateX( 19); eyeR.setTranslateY(-12); eyeR.setTranslateZ(-46);

        // Nez petit
        Sphere nose = new Sphere(4.5);
        nose.setMaterial(mat(Color.rgb(235, 95, 130)));
        nose.getTransforms().add(new Scale(1.5, 0.8, 0.9));
        nose.setTranslateY(10); nose.setTranslateZ(-47);

        // Moustaches
        Group whiskers = buildWhiskers();

        g.getChildren().addAll(
                cranium, muzzle, cheekL, cheekR,
                earL, earR, eyeL, eyeR,
                nose, whiskers
        );

        // Posée juste au-dessus du corps (léger chevauchement pour look soudé)
        g.setTranslateY(-50);
        return g;
    }

    private Sphere makeCheek(double x) {
        Sphere s = new Sphere(18);
        s.setMaterial(matSoft(bellyColor));
        s.getTransforms().add(new Scale(0.78, 0.72, 0.48));
        s.setTranslateX(x); s.setTranslateY(16); s.setTranslateZ(-38);
        return s;
    }

    // ─── Oreille : base + triangle pointu + intérieur rose ──────────────────
    private Group buildEar(int side) {
        Group g = new Group();

        Sphere earBase = new Sphere(16);
        earBase.setMaterial(mat(furColor));
        earBase.getTransforms().add(new Scale(0.75, 0.85, 0.60));

        TriangleMesh outerMesh = new TriangleMesh();
        outerMesh.getPoints().addAll(0f,-34f,0f, -13f,4f,0f, 13f,4f,0f);
        outerMesh.getTexCoords().addAll(0.5f,0f, 0f,1f, 1f,1f);
        outerMesh.getFaces().addAll(0,0,2,2,1,1, 0,0,1,1,2,2);
        MeshView outer = new MeshView(outerMesh);
        outer.setMaterial(mat(furColor));
        outer.setCullFace(CullFace.NONE);

        TriangleMesh innerMesh = new TriangleMesh();
        innerMesh.getPoints().addAll(0f,-21f,1f, -7f,3f,1f, 7f,3f,1f);
        innerMesh.getTexCoords().addAll(0.5f,0f, 0f,1f, 1f,1f);
        innerMesh.getFaces().addAll(0,0,2,2,1,1);
        MeshView inner = new MeshView(innerMesh);
        inner.setMaterial(mat(Color.rgb(255, 155, 180)));
        inner.setCullFace(CullFace.NONE);

        g.getChildren().addAll(earBase, outer, inner);
        g.setTranslateX(side * 30);
        g.setTranslateY(-42);
        g.setTranslateZ(-8);
        g.getTransforms().add(new Rotate(side * 16, Rotate.Z_AXIS));
        return g;
    }

    // ─── Œil chibi — grand, ovale, double reflet ────────────────────────────
    private Group buildEye() {
        Group g = new Group();

        Sphere sclera = new Sphere(13);
        sclera.setMaterial(mat(Color.WHITE));
        sclera.getTransforms().add(new Scale(0.90, 1.18, 0.72));

        Sphere iris = new Sphere(9.5);
        iris.setMaterial(mat(eyeColor));
        iris.setTranslateZ(-6);

        Sphere pupil = new Sphere(6.0);
        pupil.setMaterial(mat(Color.BLACK));
        pupil.setTranslateZ(-10.5);

        Sphere shine1 = new Sphere(3.2);
        shine1.setMaterial(mat(Color.WHITE));
        shine1.setTranslateX(-3.5); shine1.setTranslateY(-4.0); shine1.setTranslateZ(-14);

        Sphere shine2 = new Sphere(1.6);
        shine2.setMaterial(mat(Color.WHITE));
        shine2.setTranslateX(3.0); shine2.setTranslateY(2.5); shine2.setTranslateZ(-14);

        g.getChildren().addAll(sclera, iris, pupil, shine1, shine2);
        return g;
    }

    // ─── Moustaches ──────────────────────────────────────────────────────────
    private Group buildWhiskers() {
        Group g = new Group();
        addWhisker(g, -22,  6, -47, -10, true);
        addWhisker(g, -22, 13, -47,   0, true);
        addWhisker(g, -21, 19, -47,  10, true);
        addWhisker(g,  22,  6, -47,  10, false);
        addWhisker(g,  22, 13, -47,   0, false);
        addWhisker(g,  21, 19, -47, -10, false);
        return g;
    }

    private void addWhisker(Group g, double x, double y, double z,
                            double angle, boolean left) {
        Group wg = new Group();
        Cylinder c = new Cylinder(0.65, 36);
        c.setMaterial(mat(Color.rgb(248, 248, 248)));
        c.getTransforms().add(new Rotate(90 + angle, Rotate.Z_AXIS));
        wg.getChildren().add(c);
        wg.setTranslateX(x + (left ? -18 : 18));
        wg.setTranslateY(y);
        wg.setTranslateZ(z);
        g.getChildren().add(wg);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTES — petits moignons ronds, style chibi
    // ════════════════════════════════════════════════════════════════════════

    private Group buildPaws() {
        Group g = new Group();

        // Pattes avant : deux petits moignons ronds devant
        addFrontPaw(g, -22, 60,  38);
        addFrontPaw(g,  22, 60,  38);

        // Hanches arrière : ellipsoïdes aplaties sur les côtés
        addRearHaunch(g, -58, 38, 0);
        addRearHaunch(g,  58, 38, 0);

        return g;
    }

    private void addFrontPaw(Group g, double x, double y, double z) {
        Cylinder leg = new Cylinder(11, 16);
        leg.setMaterial(mat(furColor));
        leg.setTranslateX(x); leg.setTranslateY(y - 8); leg.setTranslateZ(z);

        Sphere paw = new Sphere(13);
        paw.setMaterial(mat(furColor.deriveColor(0, 1, 0.90, 1)));
        paw.getTransforms().add(new Scale(1.25, 0.48, 1.50));
        paw.setTranslateX(x); paw.setTranslateY(y + 4); paw.setTranslateZ(z);

        g.getChildren().addAll(leg, paw);
    }

    private void addRearHaunch(Group g, double x, double y, double z) {
        Sphere haunch = new Sphere(22);
        haunch.setMaterial(mat(furColor));
        haunch.getTransforms().add(new Scale(0.55, 0.80, 1.15));
        haunch.setTranslateX(x); haunch.setTranslateY(y); haunch.setTranslateZ(z);

        Sphere backPaw = new Sphere(13);
        backPaw.setMaterial(mat(furColor.deriveColor(0, 1, 0.88, 1)));
        backPaw.getTransforms().add(new Scale(1.30, 0.45, 1.55));
        backPaw.setTranslateX(x * 0.88); backPaw.setTranslateY(y + 18); backPaw.setTranslateZ(z + 14);

        g.getChildren().addAll(haunch, backPaw);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  QUEUE — épaisse, enroulée sur le côté droit du corps
    // ════════════════════════════════════════════════════════════════════════

    private Group buildTail() {
        Group g = new Group();

        // Ancrée côté droit-arrière du corps
        g.setTranslateX(50);
        g.setTranslateY(40);
        g.setTranslateZ(20);

        int n = 12;
        for (int i = 0; i < n; i++) {
            double t      = (double) i / (n - 1);
            double radius = 11.5 - t * 7.0;   // effilée base→bout

            // Arc dans le plan XZ, remonte légèrement
            double angle = t * Math.PI * 1.25;
            double arcR  = 32;
            double tx =  arcR * Math.sin(angle);
            double tz =  arcR * (1 - Math.cos(angle));
            double ty = -t * 18;

            Color segColor = (i >= n - 2)
                    ? bellyColor.interpolate(Color.WHITE, 0.50)
                    : furColor;

            Sphere seg = new Sphere(radius);
            seg.setMaterial(mat(segColor));
            seg.setTranslateX(tx);
            seg.setTranslateY(ty);
            seg.setTranslateZ(tz);
            g.getChildren().add(seg);
        }
        return g;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Animations
    // ════════════════════════════════════════════════════════════════════════

    private void startAnimations() {
        tailSway = new RotateTransition(Duration.millis(1300), tailGroup);
        tailSway.setAxis(Rotate.Z_AXIS);
        tailSway.setFromAngle(-22);
        tailSway.setToAngle( 22);
        tailSway.setCycleCount(Animation.INDEFINITE);
        tailSway.setAutoReverse(true);
        tailSway.setInterpolator(Interpolator.EASE_BOTH);
        tailSway.play();

        breathe = new ScaleTransition(Duration.millis(2800), bodyGroup);
        breathe.setFromY(1.00);
        breathe.setToY(1.06);
        breathe.setCycleCount(Animation.INDEFINITE);
        breathe.setAutoReverse(true);
        breathe.setInterpolator(Interpolator.EASE_BOTH);
        breathe.play();

        headTilt = new RotateTransition(Duration.millis(4800), headGroup);
        headTilt.setAxis(Rotate.Z_AXIS);
        headTilt.setFromAngle(-6);
        headTilt.setToAngle( 6);
        headTilt.setCycleCount(Animation.INDEFINITE);
        headTilt.setAutoReverse(true);
        headTilt.setInterpolator(Interpolator.EASE_BOTH);
        headTilt.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  API publique
    // ════════════════════════════════════════════════════════════════════════

    public void stopAnimations() {
        if (tailSway != null) tailSway.stop();
        if (breathe  != null) breathe.stop();
        if (headTilt != null) headTilt.stop();
    }

    public void resumeAnimations() {
        if (tailSway != null) tailSway.play();
        if (breathe  != null) breathe.play();
        if (headTilt != null) headTilt.play();
    }

    public void setTailSwaySpeed(double ms) {
        if (tailSway != null) {
            tailSway.stop();
            tailSway.setDuration(Duration.millis(ms));
            tailSway.play();
        }
    }
}