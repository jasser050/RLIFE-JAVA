package com.studyflow.utils;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Standalone diagnostic — run from main() to test GLB loading without JavaFX.
 */
public class GlbTest {
    public static void main(String[] args) throws Exception {
        String[] keys = {"male-avatar", "female-avatar"};
        for (String key : keys) {
            String path = "/com/studyflow/avatars/" + key + ".glb";
            System.out.println("\n========== Loading: " + path + " ==========");
            InputStream is = GlbTest.class.getResourceAsStream(path);
            if (is == null) {
                System.err.println("  RESOURCE NOT FOUND!");
                continue;
            }
            System.out.println("  Resource stream OK, size=" + is.available() + " bytes");

            Path tmp = Files.createTempFile("glbtest_", ".glb");
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            is.close();
            System.out.println("  Temp file: " + tmp + " size=" + Files.size(tmp));

            try {
                GltfModelReader reader = new GltfModelReader();
                GltfModel model = reader.read(tmp.toUri());

                List<SceneModel> scenes = model.getSceneModels();
                System.out.println("  Scenes: " + scenes.size());

                // Check all images in the model
                List<ImageModel> allImages = model.getImageModels();
                System.out.println("  Total images in model: " + (allImages != null ? allImages.size() : 0));
                if (allImages != null) {
                    for (int i = 0; i < allImages.size(); i++) {
                        ImageModel img = allImages.get(i);
                        ByteBuffer buf = img.getImageData();
                        System.out.println("    Image[" + i + "]: mimeType=" + img.getMimeType()
                            + " dataSize=" + (buf != null ? buf.limit() : 0) + " bytes");
                    }
                }

                // Check all textures
                List<TextureModel> allTextures = model.getTextureModels();
                System.out.println("  Total textures in model: " + (allTextures != null ? allTextures.size() : 0));

                // Check all materials
                List<MaterialModel> allMaterials = model.getMaterialModels();
                System.out.println("  Total materials in model: " + (allMaterials != null ? allMaterials.size() : 0));
                if (allMaterials != null) {
                    for (int i = 0; i < allMaterials.size(); i++) {
                        MaterialModel mat = allMaterials.get(i);
                        System.out.println("    Mat[" + i + "]: " + mat.getClass().getSimpleName() + " name=" + mat.getName());
                        if (mat instanceof MaterialModelV2) {
                            MaterialModelV2 m2 = (MaterialModelV2) mat;
                            float[] c = m2.getBaseColorFactor();
                            System.out.println("      baseColor=" + (c != null ? String.format("[%.3f,%.3f,%.3f,%.3f]", c[0], c[1], c[2], c.length > 3 ? c[3] : 1f) : "null"));
                            System.out.println("      baseColorTexture=" + m2.getBaseColorTexture());
                            System.out.println("      metallicFactor=" + m2.getMetallicFactor());
                            System.out.println("      roughnessFactor=" + m2.getRoughnessFactor());
                        }
                    }
                }

                if (!scenes.isEmpty()) {
                    List<NodeModel> nodes = scenes.get(0).getNodeModels();
                    System.out.println("  Root nodes: " + nodes.size());
                    int totalMeshes = 0;
                    int totalPrims = 0;
                    countMeshes(nodes, new int[]{0, 0});
                    printNodeTree(nodes, "  ");
                }
            } catch (Exception e) {
                System.err.println("  LOAD ERROR: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
    }

    private static void printNodeTree(List<NodeModel> nodes, String indent) {
        for (NodeModel node : nodes) {
            String name = node.getName() != null ? node.getName() : "(unnamed)";
            List<MeshModel> meshes = node.getMeshModels();
            int meshCount = meshes != null ? meshes.size() : 0;

            System.out.println(indent + "Node: " + name + " meshes=" + meshCount);

            if (meshes != null) {
                for (MeshModel mesh : meshes) {
                    for (MeshPrimitiveModel prim : mesh.getMeshPrimitiveModels()) {
                        Map<String, AccessorModel> attrs = prim.getAttributes();
                        AccessorModel pos = attrs.get("POSITION");
                        AccessorModel uv = attrs.get("TEXCOORD_0");
                        AccessorModel idx = prim.getIndices();

                        System.out.println(indent + "  Primitive: verts=" + (pos != null ? pos.getCount() : 0)
                            + " uvs=" + (uv != null ? uv.getCount() : "none")
                            + " indices=" + (idx != null ? idx.getCount() : "none"));

                        // Check material
                        MaterialModel mat = prim.getMaterialModel();
                        if (mat instanceof MaterialModelV2) {
                            MaterialModelV2 m2 = (MaterialModelV2) mat;
                            float[] c = m2.getBaseColorFactor();
                            TextureModel tex = m2.getBaseColorTexture();
                            System.out.println(indent + "  Material: baseColor=" +
                                (c != null ? String.format("[%.2f,%.2f,%.2f]", c[0], c[1], c[2]) : "null")
                                + " hasTexture=" + (tex != null));
                            if (tex != null) {
                                ImageModel img = tex.getImageModel();
                                if (img != null) {
                                    ByteBuffer buf = img.getImageData();
                                    System.out.println(indent + "  Texture: imageData=" +
                                        (buf != null ? buf.limit() + " bytes" : "NULL"));
                                }
                            }
                        } else {
                            System.out.println(indent + "  Material: " + (mat != null ? mat.getClass().getSimpleName() : "null"));
                        }

                        // Check for vertex colors
                        AccessorModel color0 = attrs.get("COLOR_0");
                        if (color0 != null) {
                            AccessorData cData = color0.getAccessorData();
                            System.out.println(indent + "  COLOR_0: count=" + color0.getCount()
                                + " type=" + cData.getClass().getSimpleName()
                                + " components=" + color0.getElementType());
                            // Print first 3 vertex colors as sample
                            if (cData instanceof AccessorFloatData) {
                                AccessorFloatData fd = (AccessorFloatData) cData;
                                int cols = color0.getElementType().getNumComponents();
                                for (int vi = 0; vi < Math.min(3, color0.getCount()); vi++) {
                                    StringBuilder sb = new StringBuilder("    color[" + vi + "] = ");
                                    for (int ci = 0; ci < cols; ci++) sb.append(String.format("%.3f ", fd.get(vi, ci)));
                                    System.out.println(indent + sb);
                                }
                            }
                        } else {
                            System.out.println(indent + "  COLOR_0: NOT PRESENT");
                        }
                    }
                }
            }
            printNodeTree(node.getChildren(), indent + "  ");
        }
    }

    private static void countMeshes(List<NodeModel> nodes, int[] counts) {
        for (NodeModel node : nodes) {
            List<MeshModel> meshes = node.getMeshModels();
            if (meshes != null) {
                counts[0] += meshes.size();
                for (MeshModel m : meshes) counts[1] += m.getMeshPrimitiveModels().size();
            }
            countMeshes(node.getChildren(), counts);
        }
    }
}
