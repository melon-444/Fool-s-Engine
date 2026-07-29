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
package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.MeshData;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

class VKMesh implements Mesh {

    private VkDevice device;
    private VkPhysicalDevice physicalDevice;
    private boolean uploaded;

    private long vertexBuffer;
    private long vertexBufferMemory;
    private long indexBuffer;
    private long indexBufferMemory;
    private int indexCount;

    VKMesh() {}

    void setDevice(VkDevice device, VkPhysicalDevice physicalDevice) {
        this.device = device;
        this.physicalDevice = physicalDevice;
    }

    long getVertexBuffer() { return vertexBuffer; }
    long getIndexBuffer() { return indexBuffer; }

    @Override
    public void upload(MeshData data) {
        if (uploaded || device == null) return;
        uploaded = true;

        createVertexBuffer(data.vertices());
        createIndexBuffer(data.indices());
        indexCount = data.indices().length;
    }

    private void createVertexBuffer(float[] vertices) {
        int size = vertices.length * Float.BYTES;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufInfo = VkBufferCreateInfo.calloc(stack);
            bufInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufInfo.size(size);
            bufInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            bufInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuf = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufInfo, null, pBuf) != VK_SUCCESS)
                throw new RuntimeException("vkCreateBuffer(VBO) failed");
            vertexBuffer = pBuf.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, vertexBuffer, memReqs);

            int memType = findMemoryType(memReqs.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stack);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memReqs.size());
            allocInfo.memoryTypeIndex(memType);

            LongBuffer pMem = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMem) != VK_SUCCESS)
                throw new RuntimeException("vkAllocateMemory(VBO) failed");
            vertexBufferMemory = pMem.get(0);

            PointerBuffer pData = stack.mallocPointer(1);
            vkMapMemory(device, vertexBufferMemory, 0, size, 0, pData);
            FloatBuffer mapped = pData.getFloatBuffer(0, vertices.length);
            mapped.put(vertices);
            vkUnmapMemory(device, vertexBufferMemory);

            vkBindBufferMemory(device, vertexBuffer, vertexBufferMemory, 0);
        }
    }

    private void createIndexBuffer(int[] indices) {
        int size = indices.length * Integer.BYTES;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufInfo = VkBufferCreateInfo.calloc(stack);
            bufInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufInfo.size(size);
            bufInfo.usage(VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
            bufInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuf = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufInfo, null, pBuf) != VK_SUCCESS)
                throw new RuntimeException("vkCreateBuffer(IBO) failed");
            indexBuffer = pBuf.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, indexBuffer, memReqs);

            int memType = findMemoryType(memReqs.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stack);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memReqs.size());
            allocInfo.memoryTypeIndex(memType);

            LongBuffer pMem = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMem) != VK_SUCCESS)
                throw new RuntimeException("vkAllocateMemory(IBO) failed");
            indexBufferMemory = pMem.get(0);

            PointerBuffer pData = stack.mallocPointer(1);
            vkMapMemory(device, indexBufferMemory, 0, size, 0, pData);
            IntBuffer mapped = pData.getIntBuffer(0, indices.length);
            mapped.put(indices);
            vkUnmapMemory(device, indexBufferMemory);

            vkBindBufferMemory(device, indexBuffer, indexBufferMemory, 0);
        }
    }

    private int findMemoryType(int typeFilter, int properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.calloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);
        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                    (memProps.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("No suitable memory type");
    }

    @Override
    public void destroy() {
        if (vertexBuffer != 0) {
            vkDestroyBuffer(device, vertexBuffer, null);
            vertexBuffer = 0;
        }
        if (vertexBufferMemory != 0) {
            vkFreeMemory(device, vertexBufferMemory, null);
            vertexBufferMemory = 0;
        }
        if (indexBuffer != 0) {
            vkDestroyBuffer(device, indexBuffer, null);
            indexBuffer = 0;
        }
        if (indexBufferMemory != 0) {
            vkFreeMemory(device, indexBufferMemory, null);
            indexBufferMemory = 0;
        }
    }

    @Override public void bind() {}
    @Override public void unbind() {}
    @Override public int indexCount() { return indexCount; }
    @Override public float[] getAABB() { return null; }
}
