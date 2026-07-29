// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package com.melon.foolsEngine.util;

import com.melon.foolsEngine.api.rendering.resource.MeshData;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjLoader {
    public static MeshData loadMesh(Path ObjFile) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> textureCoords = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();

        List<Float> vertexBuffer = new ArrayList<>();
        List<Integer> indexBuffer = new ArrayList<>();

        Map<String, Integer> vertexMap = new HashMap<>();

        List<String> lines = null;
        try {
            lines = Files.readAllLines(ObjFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (String line : lines) {
            if (line.startsWith("v ")) {
                String[] t = line.split("\\s+");
                positions.add(new Vector3f(
                        Float.parseFloat(t[1]),
                        Float.parseFloat(t[2]),
                        Float.parseFloat(t[3])
                ));
            } else if (line.startsWith("vt ")) {
                String[] t = line.split("\\s+");
                textureCoords.add(new Vector2f(
                        Float.parseFloat(t[1]),
                        Float.parseFloat(t[2])
                ));
            } else if (line.startsWith("vn ")) {
                String[] t = line.split("\\s+");
                normals.add(new Vector3f(
                        Float.parseFloat(t[1]),
                        Float.parseFloat(t[2]),
                        Float.parseFloat(t[3])
                ));
            } else if (line.startsWith("f ")) {
                String[] t = line.split("\\s+");

                // Assume all the shapes are triangle
                for (int i = 1; i <= 3; i++) {
                    String key = t[i];

                    if (!vertexMap.containsKey(key)) {
                        String[] indices = key.split("/");

                        int vi = resolveIndex(Integer.parseInt(indices[0]),positions.size());
                        int ti = indices.length > 1 && !indices[1].isEmpty()
                                ? resolveIndex(Integer.parseInt(indices[1]),textureCoords.size()) : -1;
                        int ni = indices.length > 2
                                ? resolveIndex(Integer.parseInt(indices[2]),normals.size()) : -1;

                        Vector3f pos = positions.get(vi);
                        Vector2f uv = ti >= 0 ? textureCoords.get(ti) : new Vector2f();
                        Vector3f norm = ni >= 0 ? normals.get(ni) : new Vector3f();

                        int newIndex = vertexBuffer.size() / 8;

                        // pos
                        vertexBuffer.add(pos.x);
                        vertexBuffer.add(pos.y);
                        vertexBuffer.add(pos.z);

                        // uv
                        vertexBuffer.add(uv.x);
                        vertexBuffer.add(uv.y);

                        // normal
                        vertexBuffer.add(norm.x);
                        vertexBuffer.add(norm.y);
                        vertexBuffer.add(norm.z);

                        vertexMap.put(key, newIndex);
                    }

                    indexBuffer.add(vertexMap.get(key));
                }
            }
        }

        // into arrays
        float[] vertices = new float[vertexBuffer.size()];
        for (int i = 0; i < vertexBuffer.size(); i++) {
            vertices[i] = vertexBuffer.get(i);
        }

        int[] indices = new int[indexBuffer.size()];
        for (int i = 0; i < indexBuffer.size(); i++) {
            indices[i] = indexBuffer.get(i);
        }

        VertexLayout layout = new VertexLayout();
        layout.add(0,3)
              .add(1,2)
              .add(2,3);
        return new MeshData(vertices, indices, layout);
    }

    private static int resolveIndex(int index, int size) {
        return index > 0 ? index - 1 : size + index;
    }
}

