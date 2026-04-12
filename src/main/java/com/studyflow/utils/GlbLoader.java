package com.studyflow.utils;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Parses a GLB (binary GLTF 2.0) file into a JavaFX 3D Group
 * using the de.javagl:jgltf-model library.
 *
 * Coordinate conversion: GLTF is Y-up right-handed → JavaFX is Y-down right-handed.
 * We negate Y on all vertex positions and fix triangle winding accordingly.
 */
public class GlbLoader {

    /**
     * Load a .glb from a classpath resource stream.
     * Copies to a temp file first so jgltf can use its URI-based reader.
     */
    public static Group loadFromResource(String resourcePath) throws Exception {
        InputStream is = GlbLoader.class.getResourceAsStream(resourcePath);
        if (is == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
        return loadFromStream(is);
    }

    public static Group loadFromStream(InputStream is) throws Exception {
        // jgltf-model works most reliably via File URI — copy to temp
        Path tmp = Files.createTempFile("rlife_avatar", ".glb");
        try {
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            is.close();

            GltfModelReader reader = new GltfModelReader();
            GltfModel model = reader.read(tmp.toUri());

            // Pre-load texture image from model (jgltf may not link it through material API)
            Image textureImage = null;
            List<ImageModel> images = model.getImageModels();
            if (images != null && !images.isEmpty()) {
                ByteBuffer buf = images.get(0).getImageData();
                if (buf != null && buf.limit() > 0) {
                    ByteBuffer copy = buf.duplicate();
                    copy.rewind();
                    byte[] bytes = new byte[copy.remaining()];
                    copy.get(bytes);
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    if (!img.isError()) {
                        textureImage = img;
                    }
                }
            }

            Group root = new Group();
            List<SceneModel> scenes = model.getSceneModels();
            if (!scenes.isEmpty()) {
                for (NodeModel node : scenes.get(0).getNodeModels()) {
                    addNode(node, root, textureImage);
                }
            }
            return root;

        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ─── Node tree ───────────────────────────────────────────────────────────

    private static void addNode(NodeModel node, Group parent, Image textureImage) {
        Group g = new Group();

        // Apply node-level transforms
        float[] t = node.getTranslation();
        if (t != null) {
            g.setTranslateX(t[0]);
            g.setTranslateY(-t[1]);   // flip Y: GLTF up → JavaFX down
            g.setTranslateZ(t[2]);
        }
        float[] s = node.getScale();
        if (s != null) {
            g.getTransforms().add(new Scale(s[0], s[1], s[2]));
        }
        float[] r = node.getRotation();   // quaternion [x,y,z,w]
        if (r != null) {
            double qw = r[3];
            double angle = 2.0 * Math.toDegrees(Math.acos(Math.min(1.0, Math.abs(qw))));
            if (angle > 1e-4) {
                double sin2 = Math.sqrt(1.0 - qw * qw);
                double ax = r[0] / sin2;
                double ay = r[1] / sin2;
                double az = r[2] / sin2;
                g.getTransforms().add(new Rotate(angle, ax, -ay, az));
            }
        }

        // Process meshes — API is getMeshModels() (plural)
        List<MeshModel> meshes = node.getMeshModels();
        if (meshes != null) {
            for (MeshModel mesh : meshes) {
                for (MeshPrimitiveModel prim : mesh.getMeshPrimitiveModels()) {
                    MeshView mv = buildMeshView(prim, textureImage);
                    if (mv != null) g.getChildren().add(mv);
                }
            }
        }

        // Recurse
        for (NodeModel child : node.getChildren()) addNode(child, g, textureImage);
        parent.getChildren().add(g);
    }

    // ─── Mesh primitive → MeshView ───────────────────────────────────────────

    private static MeshView buildMeshView(MeshPrimitiveModel prim, Image textureImage) {
        Map<String, AccessorModel> attrs = prim.getAttributes();
        AccessorModel posAcc = attrs.get("POSITION");
        if (posAcc == null) return null;

        int n = posAcc.getCount();

        // ── Positions (Y negated for GLTF→JavaFX) ──
        float[] pts = new float[n * 3];
        AccessorFloatData posData = (AccessorFloatData) posAcc.getAccessorData();
        for (int i = 0; i < n; i++) {
            pts[i * 3]     =  posData.get(i, 0);
            pts[i * 3 + 1] = -posData.get(i, 1);  // flip Y
            pts[i * 3 + 2] =  posData.get(i, 2);
        }

        // ── Texture coordinates ──
        AccessorModel uvAcc = attrs.get("TEXCOORD_0");
        boolean dummyUV = (uvAcc == null);
        float[] uvs;
        if (!dummyUV) {
            int un = uvAcc.getCount();
            uvs = new float[un * 2];
            AccessorFloatData uvData = (AccessorFloatData) uvAcc.getAccessorData();
            for (int i = 0; i < un; i++) {
                uvs[i * 2]     = uvData.get(i, 0);
                uvs[i * 2 + 1] = uvData.get(i, 1);
            }
        } else {
            uvs = new float[]{0f, 0f};  // single dummy UV point
        }

        // ── Indices ──
        int[] indices = extractIndices(prim.getIndices(), n);

        // ── Build TriangleMesh ──
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().addAll(pts);
        mesh.getTexCoords().addAll(uvs);

        int triCount = indices.length / 3;
        int[] faces = new int[triCount * 6];
        for (int i = 0; i < triCount; i++) {
            // Swap v1/v2 to correct winding after Y-flip
            int v0 = indices[i * 3];
            int v1 = indices[i * 3 + 2];   // swapped
            int v2 = indices[i * 3 + 1];   // swapped
            int t0 = dummyUV ? 0 : v0;
            int t1 = dummyUV ? 0 : v1;
            int t2 = dummyUV ? 0 : v2;
            faces[i * 6]     = v0; faces[i * 6 + 1] = t0;
            faces[i * 6 + 2] = v1; faces[i * 6 + 3] = t1;
            faces[i * 6 + 4] = v2; faces[i * 6 + 5] = t2;
        }
        mesh.getFaces().addAll(faces);

        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);   // NONE handles flipped normals more gracefully
        mv.setDrawMode(DrawMode.FILL);
        mv.setMaterial(buildMaterial(prim.getMaterialModel(), textureImage));
        return mv;
    }

    // ─── Index extraction ────────────────────────────────────────────────────

    private static int[] extractIndices(AccessorModel acc, int fallbackCount) {
        if (acc == null) {
            int[] seq = new int[fallbackCount];
            for (int i = 0; i < fallbackCount; i++) seq[i] = i;
            return seq;
        }
        int count = acc.getCount();
        int[] result = new int[count];
        // Use getAccessorData() (public) and cast to the concrete typed class
        AccessorData data = acc.getAccessorData();
        if (data instanceof AccessorByteData) {
            AccessorByteData d = (AccessorByteData) data;
            for (int i = 0; i < count; i++) result[i] = d.getInt(i, 0);
        } else if (data instanceof AccessorShortData) {
            AccessorShortData d = (AccessorShortData) data;
            for (int i = 0; i < count; i++) result[i] = d.getInt(i, 0);
        } else if (data instanceof AccessorIntData) {
            AccessorIntData d = (AccessorIntData) data;
            for (int i = 0; i < count; i++) result[i] = d.get(i, 0);
        } else {
            // Fallback: sequential indices
            for (int i = 0; i < count; i++) result[i] = i;
        }
        return result;
    }

    // ─── Material ────────────────────────────────────────────────────────────

    private static PhongMaterial buildMaterial(MaterialModel matModel, Image textureImage) {
        PhongMaterial mat = new PhongMaterial(Color.WHITE);
        mat.setSpecularColor(Color.color(0.2, 0.2, 0.2));
        mat.setSpecularPower(30);

        // ── Try texture from material API first ──────────────────────────
        Image resolvedTexture = null;
        if (matModel instanceof MaterialModelV2) {
            MaterialModelV2 m2 = (MaterialModelV2) matModel;
            TextureModel texModel = m2.getBaseColorTexture();
            if (texModel != null) {
                ImageModel imgModel = texModel.getImageModel();
                if (imgModel != null) {
                    resolvedTexture = bufferToImage(imgModel.getImageData());
                }
            }
        }

        // ── Fallback: use pre-loaded texture from model's image list ─────
        if (resolvedTexture == null && textureImage != null) {
            resolvedTexture = textureImage;
        }

        if (resolvedTexture != null) {
            mat.setDiffuseMap(resolvedTexture);
            mat.setDiffuseColor(Color.WHITE);
            return mat;
        }

        // ── Last resort: base-color factor ───────────────────────────────
        if (matModel instanceof MaterialModelV2) {
            float[] c = ((MaterialModelV2) matModel).getBaseColorFactor();
            if (c != null && c.length >= 3 && !(c[0] < 0.01f && c[1] < 0.01f && c[2] < 0.01f)) {
                mat.setDiffuseColor(Color.color(
                    Math.min(1.0, c[0]),
                    Math.min(1.0, c[1]),
                    Math.min(1.0, c[2])
                ));
                return mat;
            }
        }

        mat.setDiffuseColor(Color.color(0.68, 0.56, 0.92));
        return mat;
    }

    private static Image bufferToImage(ByteBuffer buf) {
        if (buf == null || buf.limit() == 0) return null;
        try {
            ByteBuffer copy = buf.duplicate();
            copy.rewind();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            Image img = new Image(new ByteArrayInputStream(bytes));
            return img.isError() ? null : img;
        } catch (Exception e) {
            return null;
        }
    }
}
