package com.melon.foolsEngine.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MeshImporterTest {
    @Test
    void importsObjAndPlyThroughTheSameEntryPoint(@TempDir Path dir) throws Exception {
        Path obj = Files.writeString(dir.resolve("triangle.obj"), """
                v 0 0 0
                v 1 0 0
                v 0 1 0
                f 1 2 3
                """);
        Path ply = Files.writeString(dir.resolve("triangle.ply"), """
                ply
                format ascii 1.0
                element vertex 3
                property float x
                property float y
                property float z
                element face 1
                property list uchar int vertex_indices
                end_header
                0 0 0
                1 0 0
                0 1 0
                3 0 1 2
                """);
        for (Path path : new Path[]{obj, ply}) {
            var model = MeshImporter.load(path);
            assertEquals(1, model.parts().size());
            assertEquals(3, model.parts().getFirst().mesh().indices().length);
            assertTrue(model.parts().getFirst().boneNames().isEmpty());
            assertTrue(model.animations().isEmpty());
        }
    }
}
