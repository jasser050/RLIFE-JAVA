package com.studyflow.utils;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A self-contained JavaFX 3D robot scene — no WebGL, no browser.
 * Runs on JavaFX Prism (GPU-accelerated).
 * Robot head + body track mouse position smoothly.
 */
public class RobotScene3D extends Region {

    private SubScene sub;
    private AnimationTimer timer;

    // Mouse position (normalized -1..1 from center)
    private double mx = 0, my = 0;

    // Animated nodes
    private Group  robotRoot;
    private Group  robotHead;
    private Sphere antennaTip;
    private final Rotate bodyRotY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate bodyRotX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate headRotY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate headRotX = new Rotate(0, Rotate.X_AXIS);
    private PointLight orbitLight1, orbitLight2;
    private Group ring1, ring2;
    private final List<Sphere>   particles    = new ArrayList<>();
    private final List<double[]> particleData = new ArrayList<>();

    public RobotScene3D() {
        sub = buildScene();
        getChildren().add(sub);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setOnMouseMoved(e -> {
            if (getWidth() > 0 && getHeight() > 0) {
                mx = (e.getX() / getWidth()  - 0.5) * 2.0;
                my = (e.getY() / getHeight() - 0.5) * 2.0;
            }
        });
    }

    @Override
    protected void layoutChildren() {
        double w = Math.max(getWidth(),  200);
        double h = Math.max(getHeight(), 300);
        sub.setWidth(w);
        sub.setHeight(h);
        sub.relocate(0, 0);
    }

    // ── Scene construction ───────────────────────────────────────────────────

    private SubScene buildScene() {
        Group world = new Group();

        // ── Materials ──
        PhongMaterial bodyMat  = mat(0.04, 0.03, 0.11, 0.55, 0.44, 1.00,  90);
        PhongMaterial panelMat = mat(0.08, 0.06, 0.22, 0.55, 0.44, 1.00,  65);
        PhongMaterial eyeMat   = mat(0.42, 0.14, 0.90, 0.82, 0.62, 1.00, 140);
        PhongMaterial jointMat = mat(0.08, 0.06, 0.20, 0.44, 0.36, 0.95,  45);
        PhongMaterial chestMat = mat(0.18, 0.12, 0.54, 0.68, 0.55, 1.00,  55);
        PhongMaterial footMat  = mat(0.10, 0.08, 0.25, 0.50, 0.40, 0.90,  32);
        PhongMaterial ringMat1 = mat(0.52, 0.36, 1.00, 0.82, 0.68, 1.00,  75);
        PhongMaterial ringMat2 = mat(0.26, 0.52, 1.00, 0.55, 0.76, 1.00,  75);
        PhongMaterial partMat  = mat(0.60, 0.46, 1.00, 0.90, 0.80, 1.00, 100);

        // ── Lights ──
        AmbientLight ambient = new AmbientLight(Color.color(0.11, 0.07, 0.22));
        PointLight topLight  = new PointLight(Color.color(0.88, 0.72, 1.00));
        topLight.setTranslateY(-330); topLight.setTranslateZ(-90);
        orbitLight1 = new PointLight(Color.color(0.75, 0.48, 1.00));
        orbitLight2 = new PointLight(Color.color(0.28, 0.56, 1.00));
        world.getChildren().addAll(ambient, topLight, orbitLight1, orbitLight2);

        // ── Head ──
        robotHead = new Group();
        robotHead.getTransforms().addAll(headRotX, headRotY);

        Box headBox = box(62, 58, 48, bodyMat);
        Sphere eye1 = sphere(7, eyeMat); eye1.setTranslateX(-15); eye1.setTranslateY(-2); eye1.setTranslateZ(25);
        Sphere eye2 = sphere(7, eyeMat); eye2.setTranslateX( 15); eye2.setTranslateY(-2); eye2.setTranslateZ(25);
        Box visor   = box(55, 6, 3, panelMat); visor.setTranslateZ(26); visor.setTranslateY(10);
        Box ear1    = box(5, 30, 40, jointMat); ear1.setTranslateX(-35);
        Box ear2    = box(5, 30, 40, jointMat); ear2.setTranslateX( 35);
        // Ear detail notches
        Box earN1   = box(6, 8, 30, panelMat); earN1.setTranslateX(-38); earN1.setTranslateY(-5);
        Box earN2   = box(6, 8, 30, panelMat); earN2.setTranslateX( 38); earN2.setTranslateY(-5);
        Cylinder ant = new Cylinder(2.5, 32); ant.setTranslateY(-45); ant.setMaterial(jointMat);
        antennaTip = sphere(6, eyeMat); antennaTip.setTranslateY(-61);
        // Chin / jaw detail
        Box jaw  = box(48, 10, 36, panelMat); jaw.setTranslateY(26);
        Box scan = box(36, 5, 4, chestMat);   scan.setTranslateY(26); scan.setTranslateZ(19);

        robotHead.getChildren().addAll(headBox, eye1, eye2, visor, ear1, ear2, earN1, earN2, ant, antennaTip, jaw, scan);
        robotHead.setTranslateY(-104);

        // ── Neck ──
        Cylinder neck = new Cylinder(9, 14); neck.setTranslateY(-53); neck.setMaterial(jointMat);

        // ── Torso ──
        Box torso      = box(84, 100, 52, bodyMat);
        Box chestPanel = box(50, 40,  6, panelMat);  chestPanel.setTranslateZ(29); chestPanel.setTranslateY(-5);
        Box chestCore  = box(34, 26,  5, chestMat);  chestCore.setTranslateZ(33);  chestCore.setTranslateY(-5);
        // Side vents
        Box vent1L = box(6, 50, 8, panelMat); vent1L.setTranslateX(-45); vent1L.setTranslateZ(24);
        Box vent1R = box(6, 50, 8, panelMat); vent1R.setTranslateX( 45); vent1R.setTranslateZ(24);
        // Lower belt
        Box belt = box(84, 12, 56, jointMat); belt.setTranslateY(50);
        // Shoulder pads
        Box sh1 = box(20, 18, 54, jointMat); sh1.setTranslateX(-52); sh1.setTranslateY(-42);
        Box sh2 = box(20, 18, 54, jointMat); sh2.setTranslateX( 52); sh2.setTranslateY(-42);

        // ── Arms ──
        Box   lUA = box(17, 66, 17, bodyMat);  lUA.setTranslateX(-56); lUA.setTranslateY(-5);
        Sphere lEl = sphere(10, jointMat);      lEl.setTranslateX(-56); lEl.setTranslateY(31);
        Box   lLA = box(13, 56, 13, bodyMat);  lLA.setTranslateX(-56); lLA.setTranslateY(62);
        Box   lHn = box(17, 18, 17, jointMat); lHn.setTranslateX(-56); lHn.setTranslateY(92);

        Box   rUA = box(17, 66, 17, bodyMat);  rUA.setTranslateX( 56); rUA.setTranslateY(-5);
        Sphere rEl = sphere(10, jointMat);      rEl.setTranslateX( 56); rEl.setTranslateY(31);
        Box   rLA = box(13, 56, 13, bodyMat);  rLA.setTranslateX( 56); rLA.setTranslateY(62);
        Box   rHn = box(17, 18, 17, jointMat); rHn.setTranslateX( 56); rHn.setTranslateY(92);

        // ── Legs ──
        Box    hip  = box(72, 14, 46, jointMat); hip.setTranslateY(57);
        Sphere lHJ  = sphere(13, jointMat); lHJ.setTranslateX(-24); lHJ.setTranslateY(64);
        Box    lUL  = box(22, 74, 22, bodyMat);  lUL.setTranslateX(-24); lUL.setTranslateY(104);
        Sphere lKn  = sphere(14, jointMat); lKn.setTranslateX(-24); lKn.setTranslateY(144);
        Box    lLL  = box(18, 66, 18, bodyMat);  lLL.setTranslateX(-24); lLL.setTranslateY(180);
        Box    lFt  = box(30, 16, 40, footMat);  lFt.setTranslateX(-24); lFt.setTranslateY(215); lFt.setTranslateZ(8);

        Sphere rHJ  = sphere(13, jointMat); rHJ.setTranslateX( 24); rHJ.setTranslateY(64);
        Box    rUL  = box(22, 74, 22, bodyMat);  rUL.setTranslateX( 24); rUL.setTranslateY(104);
        Sphere rKn  = sphere(14, jointMat); rKn.setTranslateX( 24); rKn.setTranslateY(144);
        Box    rLL  = box(18, 66, 18, bodyMat);  rLL.setTranslateX( 24); rLL.setTranslateY(180);
        Box    rFt  = box(30, 16, 40, footMat);  rFt.setTranslateX( 24); rFt.setTranslateY(215); rFt.setTranslateZ(8);

        robotRoot = new Group();
        robotRoot.getTransforms().addAll(bodyRotX, bodyRotY);
        robotRoot.getChildren().addAll(
            neck, robotHead,
            torso, chestPanel, chestCore, vent1L, vent1R, belt, sh1, sh2,
            lUA, lEl, lLA, lHn,
            rUA, rEl, rLA, rHn,
            hip, lHJ, lUL, lKn, lLL, lFt,
                 rHJ, rUL, rKn, rLL, rFt
        );
        // Center robot: spans Y ≈ -165..223 → center ≈ +29
        robotRoot.setTranslateY(-29);
        world.getChildren().add(robotRoot);

        // ── Orbital rings ──
        ring1 = buildRing(118, 24, 3.6, ringMat1);
        ring1.getTransforms().add(new Rotate(22, Rotate.X_AXIS));
        ring2 = buildRing(158, 30, 2.8, ringMat2);
        ring2.getTransforms().addAll(new Rotate(-34, Rotate.X_AXIS), new Rotate(50, Rotate.Y_AXIS));
        world.getChildren().addAll(ring1, ring2);

        // ── Floating particles ──
        Random rng = new Random(42);
        for (int i = 0; i < 65; i++) {
            double r  = rng.nextDouble() * 2.2 + 0.8;
            Sphere p  = new Sphere(r);
            double px = (rng.nextDouble() - 0.5) * 360;
            double py = (rng.nextDouble() - 0.5) * 540;
            double pz = (rng.nextDouble() - 0.5) * 220;
            double sp = rng.nextDouble() * 0.55 + 0.15;
            p.setTranslateX(px); p.setTranslateY(py); p.setTranslateZ(pz);
            p.setMaterial(partMat);
            particles.add(p);
            particleData.add(new double[]{px, py, pz, sp});
            world.getChildren().add(p);
        }

        // ── Camera ──
        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setFieldOfView(40);
        cam.setNearClip(0.1);
        cam.setFarClip(3000);
        cam.setTranslateZ(-545);

        SubScene s = new SubScene(world, 400, 600, true, SceneAntialiasing.BALANCED);
        s.setCamera(cam);
        s.setFill(Color.TRANSPARENT);

        startAnimation();
        return s;
    }

    // ── Animation ────────────────────────────────────────────────────────────

    private void startAnimation() {
        Rotate r1 = new Rotate(0, Rotate.Y_AXIS);
        Rotate r2 = new Rotate(0, new Point3D(0.28, 1.0, 0.18).normalize());
        ring1.getTransforms().add(r1);
        ring2.getTransforms().add(r2);

        timer = new AnimationTimer() {
            long last    = 0;
            double antPh = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = (now - last) * 1e-9;
                double t  = now * 1e-9;
                last = now;

                // Orbiting key lights
                orbitLight1.setTranslateX(Math.cos(t * 0.72) * 235);
                orbitLight1.setTranslateY(-90 + Math.sin(t * 0.48) * 48);
                orbitLight1.setTranslateZ(Math.sin(t * 0.72) * 235);
                orbitLight2.setTranslateX(Math.cos(t * 0.52 + Math.PI) * 190);
                orbitLight2.setTranslateY( 70 + Math.sin(t * 0.38) * 38);
                orbitLight2.setTranslateZ(Math.sin(t * 0.52 + Math.PI) * 190);

                // Ring rotations
                r1.setAngle(t * 60);
                r2.setAngle(-t * 38);

                // Robot hover bob (Y-down: positive = downward)
                robotRoot.setTranslateY(-29 + Math.sin(t * 1.55) * 9);

                // Body sway follows mouse (lazy interpolation)
                bodyRotY.setAngle(lerp(bodyRotY.getAngle(), mx *  7.5, 0.055));
                bodyRotX.setAngle(lerp(bodyRotX.getAngle(), my *  4.5, 0.055));

                // Head tracks mouse more aggressively
                headRotY.setAngle(lerp(headRotY.getAngle(), mx * 22.0, 0.09));
                headRotX.setAngle(lerp(headRotX.getAngle(), my * 15.0, 0.09));

                // Antenna tip pulse
                antPh += dt * 4.0;
                double pulse = 0.80 + 0.36 * Math.sin(antPh);
                antennaTip.setScaleX(pulse);
                antennaTip.setScaleY(pulse);
                antennaTip.setScaleZ(pulse);

                // Particles drift upward, wrap at top
                for (int i = 0; i < particles.size(); i++) {
                    double[] d = particleData.get(i);
                    d[1] -= d[3] * dt * 56;
                    if (d[1] < -300) {
                        d[1] = 300;
                        d[0] = (Math.random() - 0.5) * 360;
                        d[2] = (Math.random() - 0.5) * 220;
                    }
                    Sphere p = particles.get(i);
                    p.setTranslateY(d[1]);
                    p.setTranslateX(d[0] + Math.sin(t * 0.9 + i * 0.37) * 10);
                }
            }
        };
        timer.start();
    }

    private static double lerp(double current, double target, double alpha) {
        return current + (target - current) * alpha;
    }

    // ── Factory helpers ──────────────────────────────────────────────────────

    private static Group buildRing(double radius, int count, double r, PhongMaterial mat) {
        Group g = new Group();
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * 2 * Math.PI;
            Sphere s = new Sphere(r);
            s.setTranslateX(Math.cos(angle) * radius);
            s.setTranslateZ(Math.sin(angle) * radius);
            s.setMaterial(mat);
            g.getChildren().add(s);
        }
        return g;
    }

    private static Box box(double w, double h, double d, PhongMaterial mat) {
        Box b = new Box(w, h, d); b.setMaterial(mat); return b;
    }

    private static Sphere sphere(double r, PhongMaterial mat) {
        Sphere s = new Sphere(r); s.setMaterial(mat); return s;
    }

    private static PhongMaterial mat(
            double dr, double dg, double db,
            double sr, double sg, double sb, double sp) {
        PhongMaterial m = new PhongMaterial(Color.color(dr, dg, db));
        m.setSpecularColor(Color.color(sr, sg, sb));
        m.setSpecularPower(sp);
        return m;
    }

    public void stopAnimation() { if (timer != null) timer.stop(); }
}
