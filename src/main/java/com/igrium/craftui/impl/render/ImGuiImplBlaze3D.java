package com.igrium.craftui.impl.render;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec4;
import imgui.flag.ImGuiBackendFlags;
import imgui.type.ImInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Dear ImGui renderer backend built on Minecraft 26.2's Blaze3D GPU abstraction
 * ({@link GpuDevice} / {@link RenderPass}), so it runs on both the OpenGL and the
 * (experimental) Vulkan backend. This replaces the old raw-OpenGL {@code ImGuiImplGl3},
 * which cannot function under Vulkan (there is no GL context).
 *
 * <p>The structure mirrors imgui-java's {@code ImGuiImplSdlGpu3} (upload vertex/index
 * buffers, then issue draws inside a render pass). Instead of shipping custom shaders,
 * it reuses Minecraft's stock {@code core/position_tex_color} shaders via a pipeline
 * modelled on {@link RenderPipelines#GUI_TEXTURED} (same bind-group layouts, blend and
 * vertex format) but with {@link PrimitiveTopology#TRIANGLES}, since ImGui emits an
 * indexed triangle list rather than quads. ImGui's {@code ImDrawVert} (pos vec2, uv vec2,
 * color RGBA8) is expanded per-vertex into Minecraft's {@code POSITION_TEX_COLOR}
 * (pos vec3 with z=0, uv vec2, color RGBA8).
 */
public class ImGuiImplBlaze3D {

    /** ImGui vertex stride: pos(2f) + uv(2f) + color(4b) = 20 bytes. */
    private static final int IMGUI_VTX_STRIDE = ImDrawData.sizeOfImDrawVert();
    /** POSITION_TEX_COLOR stride: pos(3f) + uv(2f) + color(4b) = 24 bytes. */
    private static final int MC_VTX_STRIDE = 24;

    private static RenderPipeline pipeline;

    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionMatrixBuffer = new ProjectionMatrixBuffer("craftui_imgui");

    private GpuTexture fontTexture;
    private GpuTextureView fontTextureView;
    private GpuSampler fontSampler;

    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;
    private int vertexBufferSize;
    private int indexBufferSize;

    // Reused native scratch buffer for the ImGui -> POSITION_TEX_COLOR vertex expansion.
    private ByteBuffer vtxScratch;
    private ByteBuffer idxScratch;

    private final ImVec4 clipRect = new ImVec4();

    private static RenderPipeline getPipeline() {
        if (pipeline == null) {
            // Mirrors RenderPipelines.GUI_TEXTURED_SNIPPET (GLOBALS + MATRICES_PROJECTION + SAMPLER0,
            // core/position_tex_color shaders, translucent blend, POSITION_TEX_COLOR) but with a
            // triangle-list topology to match ImGui's index buffer.
            pipeline = RenderPipelines.register(RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("craftui", "pipeline/imgui"))
                    .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build());
        }
        return pipeline;
    }

    public boolean init() {
        final ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName("imgui-java_impl_blaze3d");
        io.addBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);
        getPipeline();
        return true;
    }

    /** Ensures GPU objects exist. Mirrors the {@code newFrame} lazy-init used by the other backends. */
    public void newFrame() {
        getPipeline();
        if (fontTexture == null) {
            createFontsTexture();
        }
    }

    public void createFontsTexture() {
        final GpuDevice device = RenderSystem.getDevice();
        final ImFontAtlas fontAtlas = ImGui.getIO().getFonts();

        final ImInt width = new ImInt();
        final ImInt height = new ImInt();
        final ByteBuffer pixels = fontAtlas.getTexDataAsRGBA32(width, height);
        final int w = width.get();
        final int h = height.get();

        destroyFontsTexture();

        fontTexture = device.createTexture(() -> "CraftUI ImGui Font Atlas",
                GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                GpuFormat.RGBA8_UNORM, w, h, 1, 1);
        fontTextureView = device.createTextureView(fontTexture);

        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(fontTexture, pixels, 0, 0, 0, 0, w, h);

        fontSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        // ImGui stores the backend texture id; we only use the font atlas, so bind it below regardless.
        fontAtlas.setTexID(fontTexture.hashCode());
    }

    public void destroyFontsTexture() {
        if (fontTextureView != null) {
            fontTextureView.close();
            fontTextureView = null;
        }
        if (fontTexture != null) {
            fontTexture.close();
            fontTexture = null;
            ImGui.getIO().getFonts().setTexID(0);
        }
    }

    public void renderDrawData(final ImDrawData drawData) {
        final int fbWidth = (int) (drawData.getDisplaySizeX() * drawData.getFramebufferScaleX());
        final int fbHeight = (int) (drawData.getDisplaySizeY() * drawData.getFramebufferScaleY());
        if (fbWidth <= 0 || fbHeight <= 0) {
            return;
        }

        final int cmdListsCount = drawData.getCmdListsCount();
        final int totalVtxCount = drawData.getTotalVtxCount();
        final int totalIdxCount = drawData.getTotalIdxCount();
        if (cmdListsCount <= 0 || totalVtxCount <= 0 || totalIdxCount <= 0) {
            return;
        }

        if (fontTexture == null) {
            createFontsTexture();
        }

        final GpuDevice device = RenderSystem.getDevice();
        final CommandEncoder encoder = device.createCommandEncoder();

        uploadBuffers(device, encoder, drawData, totalVtxCount, totalIdxCount);

        // Ortho projection mapping ImGui display-space (top-left origin) to clip space.
        // Mirrors GuiRenderer's setup so Minecraft's core/position_tex_color shader lands correctly.
        projection.setupOrtho(1000.0F, 11000.0F, drawData.getDisplaySizeX(), drawData.getDisplaySizeY(), true);
        final GpuBufferSlice projectionSlice = projectionMatrixBuffer.getBuffer(projection);
        RenderSystem.setProjectionMatrix(projectionSlice, ProjectionType.ORTHOGRAPHIC);
        final GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F));

        final RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        // Clamp scissor rectangles to the render target's real size rather than ImGui's DisplaySize:
        // the two normally match, but can diverge (e.g. an offscreen/headless surface), and RenderPass
        // rejects any scissor that exceeds its render area.
        final int renderW = target.width;
        final int renderH = target.height;
        final IndexType indexType = ImDrawData.sizeOfImDrawIdx() == 2 ? IndexType.SHORT : IndexType.INT;

        final float clipOffX = drawData.getDisplayPosX();
        final float clipOffY = drawData.getDisplayPosY();
        final float clipScaleX = drawData.getFramebufferScaleX();
        final float clipScaleY = drawData.getFramebufferScaleY();

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "CraftUI ImGui",
                target.getColorTextureView(), Optional.empty(),
                null, OptionalDouble.empty())) {

            renderPass.setPipeline(getPipeline());
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertexBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, indexType);

            int globalVtxOffset = 0;
            int globalIdxOffset = 0;
            for (int n = 0; n < cmdListsCount; n++) {
                final int cmdCount = drawData.getCmdListCmdBufferSize(n);
                for (int cmdIdx = 0; cmdIdx < cmdCount; cmdIdx++) {
                    drawData.getCmdListCmdBufferClipRect(clipRect, n, cmdIdx);
                    final float clipMinX = Math.max(0.0F, (clipRect.x - clipOffX) * clipScaleX);
                    final float clipMinY = Math.max(0.0F, (clipRect.y - clipOffY) * clipScaleY);
                    final float clipMaxX = Math.min(renderW, (clipRect.z - clipOffX) * clipScaleX);
                    final float clipMaxY = Math.min(renderH, (clipRect.w - clipOffY) * clipScaleY);
                    if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                        continue;
                    }

                    renderPass.enableScissor((int) clipMinX, (int) clipMinY,
                            (int) (clipMaxX - clipMinX), (int) (clipMaxY - clipMinY));

                    // We only ever bind the font atlas texture (the mod does not register custom ImGui images yet).
                    renderPass.bindTexture("Sampler0", fontTextureView, fontSampler);

                    final int elemCount = drawData.getCmdListCmdBufferElemCount(n, cmdIdx);
                    final int idxOffset = drawData.getCmdListCmdBufferIdxOffset(n, cmdIdx);
                    final int vtxOffset = drawData.getCmdListCmdBufferVtxOffset(n, cmdIdx);
                    renderPass.drawIndexed(elemCount, 1, globalIdxOffset + idxOffset, globalVtxOffset + vtxOffset, 0);
                }
                globalVtxOffset += drawData.getCmdListVtxBufferSize(n);
                globalIdxOffset += drawData.getCmdListIdxBufferSize(n);
            }
        }
    }

    private void uploadBuffers(final GpuDevice device, final CommandEncoder encoder, final ImDrawData drawData,
                               final int totalVtxCount, final int totalIdxCount) {
        final int vtxBytes = totalVtxCount * MC_VTX_STRIDE;
        final int idxBytes = totalIdxCount * ImDrawData.sizeOfImDrawIdx();

        vtxScratch = ensureScratch(vtxScratch, vtxBytes);
        idxScratch = ensureScratch(idxScratch, idxBytes);

        // NOTE: getCmdListVtxBufferData / getCmdListIdxBufferData reuse a single shared ByteBuffer,
        // so each must be fully consumed before the next call (vtx, then idx, per list).
        int vtxWritePos = 0;
        int idxWritePos = 0;
        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            final ByteBuffer vtx = drawData.getCmdListVtxBufferData(n);
            vtxWritePos = expandVertices(vtx, vtxScratch, vtxWritePos);

            final ByteBuffer idx = drawData.getCmdListIdxBufferData(n);
            final int ilen = idx.remaining();
            for (int i = 0; i < ilen; i++) {
                idxScratch.put(idxWritePos + i, idx.get(idx.position() + i));
            }
            idxWritePos += ilen;
        }

        vtxScratch.position(0).limit(vtxBytes);
        idxScratch.position(0).limit(idxBytes);

        vertexBuffer = ensureBuffer(device, vertexBuffer, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                vtxBytes, true);
        indexBuffer = ensureBuffer(device, indexBuffer, GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                idxBytes, false);

        encoder.writeToBuffer(vertexBuffer.slice(0, vtxBytes), vtxScratch);
        encoder.writeToBuffer(indexBuffer.slice(0, idxBytes), idxScratch);
    }

    /**
     * Expands one command list's ImGui vertices (stride 20) into POSITION_TEX_COLOR (stride 24),
     * inserting z=0. Byte-copies to avoid any endianness translation. Returns the new write position.
     */
    private static int expandVertices(final ByteBuffer src, final ByteBuffer dst, final int dstStart) {
        final int vertCount = src.remaining() / IMGUI_VTX_STRIDE;
        final int base = src.position();
        int dstPos = dstStart;
        for (int v = 0; v < vertCount; v++) {
            final int s = base + v * IMGUI_VTX_STRIDE;
            // pos.xy (8 bytes)
            for (int b = 0; b < 8; b++) {
                dst.put(dstPos + b, src.get(s + b));
            }
            // pos.z = 0 (4 bytes)
            dst.putInt(dstPos + 8, 0);
            // uv (8 bytes) at src offset 8
            for (int b = 0; b < 8; b++) {
                dst.put(dstPos + 12 + b, src.get(s + 8 + b));
            }
            // color (4 bytes) at src offset 16
            for (int b = 0; b < 4; b++) {
                dst.put(dstPos + 20 + b, src.get(s + 16 + b));
            }
            dstPos += MC_VTX_STRIDE;
        }
        return dstPos;
    }

    private static ByteBuffer ensureScratch(final ByteBuffer current, final int needed) {
        if (current != null && current.capacity() >= needed) {
            current.clear();
            return current;
        }
        if (current != null) {
            MemoryUtil.memFree(current);
        }
        return MemoryUtil.memAlloc(Math.max(needed, 64 * 1024));
    }

    private GpuBuffer ensureBuffer(final GpuDevice device, GpuBuffer current, final int usage,
                                   final int needed, final boolean vertex) {
        final int currentSize = vertex ? vertexBufferSize : indexBufferSize;
        if (current != null && currentSize >= needed) {
            return current;
        }
        final int newSize = Math.max(needed, currentSize == 0 ? 64 * 1024 : currentSize * 2);
        if (current != null) {
            current.close();
        }
        final GpuBuffer created = device.createBuffer(
                () -> "CraftUI ImGui " + (vertex ? "Vertices" : "Indices"), usage, newSize);
        if (vertex) {
            vertexBufferSize = newSize;
        } else {
            indexBufferSize = newSize;
        }
        return created;
    }

    public void shutdown() {
        destroyFontsTexture();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        if (indexBuffer != null) {
            indexBuffer.close();
            indexBuffer = null;
        }
        if (vtxScratch != null) {
            MemoryUtil.memFree(vtxScratch);
            vtxScratch = null;
        }
        if (idxScratch != null) {
            MemoryUtil.memFree(idxScratch);
            idxScratch = null;
        }
        vertexBufferSize = 0;
        indexBufferSize = 0;

        final ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName(null);
        io.removeBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);
    }
}
