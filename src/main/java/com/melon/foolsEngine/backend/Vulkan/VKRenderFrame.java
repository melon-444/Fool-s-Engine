package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;

class VKRenderFrame implements RenderFrame {

    private VKWindow window;
    private boolean init;

    private VkInstance instance;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private int graphicsQueueFamily;
    private VkQueue graphicsQueue;

    private long surface;
    private long swapchain;
    private int swapchainImageFormat;
    private VkExtent2D swapchainExtent;
    private final List<Long> swapchainImages = new ArrayList<>();
    private final List<Long> swapchainImageViews = new ArrayList<>();

    private long renderPass;
    private long pipelineLayout;
    private long pipeline;

    private final List<Long> framebuffers = new ArrayList<>();

    private long commandPool;
    private VkCommandBuffer commandBuffer;

    private long imageAvailableSemaphore;
    private long renderFinishedSemaphore;
    private long inFlightFence;

    private long vertexBuffer;
    private long vertexBufferMemory;

    private float rotationAngle;

    void setWindow(VKWindow window) {
        this.window = window;
    }

    @Override
    public void init() {
        if (init) return;
        if (window == null) throw new IllegalStateException("Must call setWindow() before init()");

        createInstance();
        createSurface();
        pickPhysicalDevice();
        createLogicalDevice();
        createSwapchain();
        createImageViews();
        createRenderPass();
        createGraphicsPipeline();
        createFramebuffers();
        createCommandPool();
        createVertexBuffer();
        createCommandBuffer();
        createSyncObjects();
        init = true;
    }

    private void createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8("foolsEngine Vulkan"));
            appInfo.applicationVersion(VK_MAKE_VERSION(0, 1, 0));
            appInfo.pEngineName(stack.UTF8("foolsEngine"));
            appInfo.engineVersion(VK_MAKE_VERSION(0, 1, 0));
            appInfo.apiVersion(VK_API_VERSION_1_3);

            PointerBuffer glfwExtensions = org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null) throw new RuntimeException("GLFW Vulkan extensions unavailable");

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(glfwExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            if (vkCreateInstance(createInfo, null, pInstance) != VK_SUCCESS)
                throw new RuntimeException("vkCreateInstance failed");
            instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private void createSurface() {
        long surf = window.createSurface(instance);
        surface = surf;
    }

    private void pickPhysicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);
            if (deviceCount.get(0) == 0) throw new RuntimeException("No Vulkan GPU found");

            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, devices);

            for (int i = 0; i < deviceCount.get(0); i++) {
                VkPhysicalDevice dev = new VkPhysicalDevice(devices.get(i), instance);
                if (isDeviceSuitable(dev, stack)) {
                    physicalDevice = dev;
                    return;
                }
            }
            throw new RuntimeException("No suitable GPU");
        }
    }

    private boolean isDeviceSuitable(VkPhysicalDevice dev, MemoryStack stack) {
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc();
        vkGetPhysicalDeviceProperties(dev, props);
        boolean discrete = props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;

        int qf = findQueueFamily(dev, stack);
        if (qf < 0) return false;
        graphicsQueueFamily = qf;

        IntBuffer count = stack.ints(0);
        vkGetPhysicalDeviceSurfaceFormatsKHR(dev, surface, count, null);
        if (count.get(0) == 0) return false;
        count.put(0, 0);
        vkGetPhysicalDeviceSurfacePresentModesKHR(dev, surface, count, null);
        return count.get(0) > 0 && discrete;
    }

    private int findQueueFamily(VkPhysicalDevice dev, MemoryStack stack) {
        IntBuffer count = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(dev, count, null);
        VkQueueFamilyProperties.Buffer props = VkQueueFamilyProperties.calloc(count.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(dev, count, props);

        for (int i = 0; i < count.get(0); i++) {
            if ((props.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                IntBuffer presentSupport = stack.ints(VK_FALSE);
                vkGetPhysicalDeviceSurfaceSupportKHR(dev, i, surface, presentSupport);
                if (presentSupport.get(0) == VK_TRUE) return i;
            }
        }
        return -1;
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            float queuePriority = 1.0f;
            VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack);
            queueCreateInfo.get(0)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsQueueFamily)
                    .pQueuePriorities(stack.floats(queuePriority));

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            PointerBuffer extensions = stack.pointers(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME));

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfo);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(extensions);

            PointerBuffer pDevice = stack.mallocPointer(1);
            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS)
                throw new RuntimeException("vkCreateDevice failed");
            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    private void createSwapchain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.calloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, caps);

            IntBuffer fmtCount = stack.ints(0);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, fmtCount, null);
            VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(fmtCount.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, fmtCount, formats);

            IntBuffer modeCount = stack.ints(0);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, modeCount, null);
            IntBuffer presentModes = stack.mallocInt(modeCount.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, modeCount, presentModes);

            int surfaceFormat = VK_FORMAT_B8G8R8A8_UNORM;
            for (int i = 0; i < fmtCount.get(0); i++) {
                if (formats.get(i).format() == VK_FORMAT_B8G8R8A8_UNORM) {
                    surfaceFormat = formats.get(i).format();
                    break;
                }
            }

            int presentMode = VK_PRESENT_MODE_FIFO_KHR;
            for (int i = 0; i < modeCount.get(0); i++) {
                if (presentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                    presentMode = VK_PRESENT_MODE_MAILBOX_KHR;
                    break;
                }
            }

            VkExtent2D extent;
            if (caps.currentExtent().width() != 0xFFFFFFFF) {
                extent = caps.currentExtent();
            } else {
                extent = VkExtent2D.create();
                int w = window.getWidth();
                int h = window.getHeight();
                extent.width(Math.max(1, Math.min(w, caps.maxImageExtent().width())));
                extent.height(Math.max(1, Math.min(h, caps.maxImageExtent().height())));
            }

            int imageCount = caps.minImageCount() + 1;
            if (caps.maxImageCount() > 0 && imageCount > caps.maxImageCount())
                imageCount = caps.maxImageCount();

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(surfaceFormat);
            createInfo.imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            createInfo.preTransform(caps.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);
            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapchain = stack.mallocLong(1);
            if (vkCreateSwapchainKHR(device, createInfo, null, pSwapchain) != VK_SUCCESS)
                throw new RuntimeException("vkCreateSwapchainKHR failed");
            swapchain = pSwapchain.get(0);
            swapchainImageFormat = surfaceFormat;
            swapchainExtent = VkExtent2D.create().set(extent);

            IntBuffer imgCount = stack.ints(0);
            vkGetSwapchainImagesKHR(device, swapchain, imgCount, null);
            LongBuffer pImages = stack.mallocLong(imgCount.get(0));
            vkGetSwapchainImagesKHR(device, swapchain, imgCount, pImages);
            swapchainImages.clear();
            for (int i = 0; i < imgCount.get(0); i++)
                swapchainImages.add(pImages.get(i));
        }
    }

    private void createImageViews() {
        swapchainImageViews.clear();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (long image : swapchainImages) {
                VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                createInfo.image(image);
                createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                createInfo.format(swapchainImageFormat);
                VkComponentMapping cm = createInfo.components();
                cm.r(VK_COMPONENT_SWIZZLE_IDENTITY);
                cm.g(VK_COMPONENT_SWIZZLE_IDENTITY);
                cm.b(VK_COMPONENT_SWIZZLE_IDENTITY);
                cm.a(VK_COMPONENT_SWIZZLE_IDENTITY);
                VkImageSubresourceRange range = createInfo.subresourceRange();
                range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                range.baseMipLevel(0);
                range.levelCount(1);
                range.baseArrayLayer(0);
                range.layerCount(1);

                LongBuffer pView = stack.mallocLong(1);
                if (vkCreateImageView(device, createInfo, null, pView) != VK_SUCCESS)
                    throw new RuntimeException("vkCreateImageView failed");
                swapchainImageViews.add(pView.get(0));
            }
        }
    }

    private void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);
            attachments.get(0)
                    .format(swapchainImageFormat)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorRefs = VkAttachmentReference.calloc(1, stack);
            colorRefs.get(0).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.get(0)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(colorRefs);

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
            dependency.get(0)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            createInfo.pAttachments(attachments);
            createInfo.pSubpasses(subpass);
            createInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);
            if (vkCreateRenderPass(device, createInfo, null, pRenderPass) != VK_SUCCESS)
                throw new RuntimeException("vkCreateRenderPass failed");
            renderPass = pRenderPass.get(0);
        }
    }

    private void createGraphicsPipeline() {
        String vertSrc = "#version 450\n" +
                "#extension GL_ARB_separate_shader_objects : enable\n" +
                "layout(location = 0) in vec3 inPosition;\n" +
                "layout(location = 1) in vec3 inColor;\n" +
                "layout(location = 0) out vec3 fragColor;\n" +
                "layout(push_constant) uniform PushConstants {\n    mat4 mvp;\n} push;\n" +
                "void main() {\n" +
                "    gl_Position = push.mvp * vec4(inPosition, 1.0);\n" +
                "    fragColor = inColor;\n}\n";

        String fragSrc = "#version 450\n" +
                "#extension GL_ARB_separate_shader_objects : enable\n" +
                "layout(location = 0) in vec3 fragColor;\n" +
                "layout(location = 0) out vec4 outColor;\n" +
                "void main() {\n    outColor = vec4(fragColor, 1.0);\n}\n";

        long vertModule = compileSPIRV(vertSrc, "main.vert",
                org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader);
        long fragModule = compileSPIRV(fragSrc, "main.frag",
                org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertModule)
                    .pName(stack.UTF8("main"));
            stages.get(1)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragModule)
                    .pName(stack.UTF8("main"));

            VkVertexInputBindingDescription.Buffer bindingDesc = VkVertexInputBindingDescription.calloc(1, stack);
            bindingDesc.get(0).binding(0).stride(6 * Float.BYTES).inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attrDesc = VkVertexInputAttributeDescription.calloc(2, stack);
            attrDesc.get(0).location(0).binding(0).format(VK_FORMAT_R32G32B32_SFLOAT).offset(0);
            attrDesc.get(1).location(1).binding(0).format(VK_FORMAT_R32G32B32_SFLOAT).offset(3 * Float.BYTES);

            VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInput.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInput.pVertexBindingDescriptions(bindingDesc);
            vertexInput.pVertexAttributeDescriptions(attrDesc);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.get(0).x(0).y(0).width(swapchainExtent.width()).height(swapchainExtent.height())
                    .minDepth(0).maxDepth(1);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.get(0).offset().set(0, 0);
            scissor.get(0).extent().set(swapchainExtent);

            VkPipelineViewportStateCreateInfo vpState = VkPipelineViewportStateCreateInfo.calloc(stack);
            vpState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            vpState.pViewports(viewport);
            vpState.pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineColorBlendAttachmentState.Buffer blendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            blendAttachment.get(0)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                            VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                    .blendEnable(false);

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.pAttachments(blendAttachment);

            VkPushConstantRange.Buffer pushRange = VkPushConstantRange.calloc(1, stack);
            pushRange.get(0).stageFlags(VK_SHADER_STAGE_VERTEX_BIT).offset(0).size(16 * Float.BYTES);

            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            layoutInfo.pPushConstantRanges(pushRange);

            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreatePipelineLayout(device, layoutInfo, null, pLayout) != VK_SUCCESS)
                throw new RuntimeException("vkCreatePipelineLayout failed");
            pipelineLayout = pLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipeInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipeInfo.get(0)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(stages)
                    .pVertexInputState(vertexInput)
                    .pInputAssemblyState(inputAssembly)
                    .pViewportState(vpState)
                    .pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling)
                    .pColorBlendState(colorBlending)
                    .layout(pipelineLayout)
                    .renderPass(renderPass)
                    .subpass(0);

            LongBuffer pPipeline = stack.mallocLong(1);
            if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipeInfo, null, pPipeline) != VK_SUCCESS)
                throw new RuntimeException("vkCreateGraphicsPipelines failed");
            pipeline = pPipeline.get(0);

            vkDestroyShaderModule(device, vertModule, null);
            vkDestroyShaderModule(device, fragModule, null);
        }
    }

    private long compileSPIRV(String source, String filename, int shaderKind) {
        long compiler = org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_initialize();
        if (compiler == MemoryUtil.NULL) throw new RuntimeException("shaderc_compiler_initialize failed");

        long options = org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_initialize();
        org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_target_env(options,
                org.lwjgl.util.shaderc.Shaderc.shaderc_target_env_vulkan,
                org.lwjgl.util.shaderc.Shaderc.shaderc_env_version_vulkan_1_3);
        org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_optimization_level(options,
                org.lwjgl.util.shaderc.Shaderc.shaderc_optimization_level_performance);

        long result = org.lwjgl.util.shaderc.Shaderc.shaderc_compile_into_spv(
                compiler, source, shaderKind, filename, "main", options);

        int status = org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_compilation_status(result);
        if (status != org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success) {
            String err = org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_error_message(result);
            org.lwjgl.util.shaderc.Shaderc.shaderc_result_release(result);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_release(options);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release(compiler);
            throw new RuntimeException("SPIR-V compile failed for " + filename + ":\n" + err);
        }

        ByteBuffer spirv = org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_bytes(result);
        int codeSize = spirv.remaining();
        ByteBuffer code = MemoryUtil.memAlloc(codeSize);
        MemoryUtil.memCopy(spirv, code);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(code);

            LongBuffer pModule = stack.mallocLong(1);
            int vkResult = vkCreateShaderModule(device, createInfo, null, pModule);
            MemoryUtil.memFree(code);

            org.lwjgl.util.shaderc.Shaderc.shaderc_result_release(result);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_release(options);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release(compiler);

            if (vkResult != VK_SUCCESS) throw new RuntimeException("vkCreateShaderModule failed");
            return pModule.get(0);
        }
    }

    private void createFramebuffers() {
        framebuffers.clear();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (long imageView : swapchainImageViews) {
                LongBuffer pAttachments = stack.longs(imageView);

                VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.calloc(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                createInfo.renderPass(renderPass);
                createInfo.attachmentCount(1);
                createInfo.pAttachments(pAttachments);
                createInfo.width(swapchainExtent.width());
                createInfo.height(swapchainExtent.height());
                createInfo.layers(1);

                LongBuffer pFb = stack.mallocLong(1);
                if (vkCreateFramebuffer(device, createInfo, null, pFb) != VK_SUCCESS)
                    throw new RuntimeException("vkCreateFramebuffer failed");
                framebuffers.add(pFb.get(0));
            }
        }
    }

    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            createInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            createInfo.queueFamilyIndex(graphicsQueueFamily);

            LongBuffer pPool = stack.mallocLong(1);
            if (vkCreateCommandPool(device, createInfo, null, pPool) != VK_SUCCESS)
                throw new RuntimeException("vkCreateCommandPool failed");
            commandPool = pPool.get(0);
        }
    }

    private static final float[] VERTICES = {
            0.0f, -0.5f, 0.0f,  1.0f, 0.0f, 0.0f,
            0.5f,  0.5f, 0.0f,  0.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, 0.0f,  0.0f, 0.0f, 1.0f,
    };

    private void createVertexBuffer() {
        int size = VERTICES.length * Float.BYTES;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuf = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuf) != VK_SUCCESS)
                throw new RuntimeException("vkCreateBuffer failed");
            vertexBuffer = pBuf.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, vertexBuffer, memReqs);

            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);

            int memType = -1;
            for (int i = 0; i < memProps.memoryTypeCount(); i++) {
                if ((memReqs.memoryTypeBits() & (1 << i)) != 0 &&
                        (memProps.memoryTypes(i).propertyFlags() &
                                (VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT))
                                == (VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)) {
                    memType = i;
                    break;
                }
            }
            if (memType < 0) throw new RuntimeException("No suitable memory type");

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memReqs.size());
            allocInfo.memoryTypeIndex(memType);

            LongBuffer pMem = stack.mallocLong(1);
            if (vkAllocateMemory(device, allocInfo, null, pMem) != VK_SUCCESS)
                throw new RuntimeException("vkAllocateMemory failed");
            vertexBufferMemory = pMem.get(0);

            PointerBuffer pData = stack.mallocPointer(1);
            vkMapMemory(device, vertexBufferMemory, 0, size, 0, pData);
            pData.getFloatBuffer(0, VERTICES.length).put(VERTICES);
            vkUnmapMemory(device, vertexBufferMemory);

            vkBindBufferMemory(device, vertexBuffer, vertexBufferMemory, 0);
        }
    }

    private void createCommandBuffer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCmd = stack.mallocPointer(1);
            if (vkAllocateCommandBuffers(device, allocInfo, pCmd) != VK_SUCCESS)
                throw new RuntimeException("vkAllocateCommandBuffers failed");
            commandBuffer = new VkCommandBuffer(pCmd.get(0), device);
        }
    }

    private void createSyncObjects() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semInfo = VkSemaphoreCreateInfo.calloc(stack);
            semInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer p = stack.mallocLong(1);
            vkCreateSemaphore(device, semInfo, null, p);
            imageAvailableSemaphore = p.get(0);
            vkCreateSemaphore(device, semInfo, null, p);
            renderFinishedSemaphore = p.get(0);
            vkCreateFence(device, fenceInfo, null, p);
            inFlightFence = p.get(0);
        }
    }

    @Override
    public void render(RenderScene scene) {
        if (!init) throw new IllegalStateException("Not initialized");
        if (window == null) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkWaitForFences(device, inFlightFence, true, Long.MAX_VALUE);
            vkResetFences(device, inFlightFence);

            IntBuffer imageIndex = stack.ints(0);
            int result = vkAcquireNextImageKHR(device, swapchain, Long.MAX_VALUE,
                    imageAvailableSemaphore, VK_NULL_HANDLE, imageIndex);

            if (result == VK_ERROR_OUT_OF_DATE_KHR) return;
            if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR)
                throw new RuntimeException("vkAcquireNextImageKHR: " + result);

            vkResetCommandBuffer(commandBuffer, 0);
            recordDrawCommands(imageIndex.get(0));

            LongBuffer pWaitSems = stack.longs(imageAvailableSemaphore);
            IntBuffer waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            LongBuffer pSignalSems = stack.longs(renderFinishedSemaphore);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pWaitSemaphores(pWaitSems);
            submitInfo.pWaitDstStageMask(waitStages);
            PointerBuffer pCmdBufs = stack.pointers(commandBuffer.address());
            submitInfo.pCommandBuffers(pCmdBufs);
            submitInfo.pSignalSemaphores(pSignalSems);

            if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFence) != VK_SUCCESS)
                throw new RuntimeException("vkQueueSubmit failed");

            LongBuffer pSwapchains = stack.longs(swapchain);
            IntBuffer pImageIndices = stack.ints(imageIndex.get(0));

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(pSignalSems);
            presentInfo.pSwapchains(pSwapchains);
            presentInfo.pImageIndices(pImageIndices);

            int presentResult = vkQueuePresentKHR(graphicsQueue, presentInfo);
            if (presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR) return;
            if (presentResult != VK_SUCCESS) throw new RuntimeException("vkQueuePresentKHR: " + presentResult);

            rotationAngle += 0.01f;
            if (rotationAngle > Math.PI * 2) rotationAngle -= (float) (Math.PI * 2);
        }
    }

    private void recordDrawCommands(int imageIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS)
                throw new RuntimeException("vkBeginCommandBuffer failed");

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.get(0).color().float32(0, 0.1f).float32(1, 0.1f).float32(2, 0.15f).float32(3, 1.0f);

            VkRenderPassBeginInfo rpInfo = VkRenderPassBeginInfo.calloc(stack);
            rpInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            rpInfo.renderPass(renderPass);
            rpInfo.framebuffer(framebuffers.get(imageIndex));
            rpInfo.renderArea().offset().set(0, 0);
            rpInfo.renderArea().extent().set(swapchainExtent);
            rpInfo.clearValueCount(1);
            rpInfo.pClearValues(clearValues);

            vkCmdBeginRenderPass(commandBuffer, rpInfo, VK_SUBPASS_CONTENTS_INLINE);
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

            float aspect = (float) swapchainExtent.width() / swapchainExtent.height();
            Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45), aspect, 0.1f, 100.0f);
            // Flip Y for Vulkan NDC
            proj.m11(proj.m11() * -1);
            Matrix4f view = new Matrix4f().lookAt(1.5f, 1.5f, 1.5f, 0, 0, 0, 0, 1, 0);
            Matrix4f model = new Matrix4f().rotateY(rotationAngle);
            Matrix4f mvp = new Matrix4f(proj).mul(view).mul(model);

            FloatBuffer mvpBuf = stack.mallocFloat(16);
            mvp.get(mvpBuf);
            vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, mvpBuf);

            LongBuffer pVertexBufs = stack.longs(vertexBuffer);
            LongBuffer pOffsets = stack.longs(0);
            vkCmdBindVertexBuffers(commandBuffer, 0, pVertexBufs, pOffsets);

            vkCmdDraw(commandBuffer, 3, 1, 0, 0);
            vkCmdEndRenderPass(commandBuffer);

            if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS)
                throw new RuntimeException("vkEndCommandBuffer failed");
        }
    }

    @Override @Deprecated public void beginFrame() {}
    @Override @Deprecated public void endFrame() {}
    @Override @Deprecated public void endFrame(RenderTarget target) {}
    @Override @Deprecated public void endFrame(RenderTarget target, Material m) {}
    @Override @Deprecated public void endFrame(RenderTarget target, Material m, int layer) {}
    @Override @Deprecated public void setCamera(Camera camera) {}
    @Override @Deprecated public void submit(RenderCommand command) {}
    @Override @Deprecated public void setBackGroundColor(float r, float g, float b, float a) {}
    @Override @Deprecated public void applyLightEnvironment(LightEnvironment env) {}

    @Override
    public void screenShot(ByteBuffer dstBuf) {
        throw new UnsupportedOperationException("Vulkan screenshots not yet implemented");
    }

    @Override
    public void screenShot(Path path) {
        throw new UnsupportedOperationException("Vulkan screenshots not yet implemented");
    }

    @Override
    public void screenShot(Path path, RenderTarget target) {
        throw new UnsupportedOperationException("Vulkan screenshots not yet implemented");
    }

    public void cleanup() {
        if (!init) return;
        vkDeviceWaitIdle(device);

        if (inFlightFence != 0) vkDestroyFence(device, inFlightFence, null);
        if (renderFinishedSemaphore != 0) vkDestroySemaphore(device, renderFinishedSemaphore, null);
        if (imageAvailableSemaphore != 0) vkDestroySemaphore(device, imageAvailableSemaphore, null);
        if (commandPool != 0) vkDestroyCommandPool(device, commandPool, null);
        for (long fb : framebuffers) vkDestroyFramebuffer(device, fb, null);
        if (pipeline != 0) vkDestroyPipeline(device, pipeline, null);
        if (pipelineLayout != 0) vkDestroyPipelineLayout(device, pipelineLayout, null);
        if (renderPass != 0) vkDestroyRenderPass(device, renderPass, null);
        for (long iv : swapchainImageViews) vkDestroyImageView(device, iv, null);
        if (swapchain != 0) vkDestroySwapchainKHR(device, swapchain, null);
        if (vertexBufferMemory != 0) vkFreeMemory(device, vertexBufferMemory, null);
        if (vertexBuffer != 0) vkDestroyBuffer(device, vertexBuffer, null);
        if (surface != 0) vkDestroySurfaceKHR(instance, surface, null);
        if (device != null) vkDestroyDevice(device, null);
        if (instance != null) vkDestroyInstance(instance, null);
    }
}
