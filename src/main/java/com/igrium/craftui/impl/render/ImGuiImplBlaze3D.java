package com.igrium.craftui.impl.render;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.system.MemoryStack;

import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec4;
import imgui.flag.ImGuiBackendFlags;
import imgui.type.ImInt;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Dear ImGui renderer backend built on Minecraft 26.2's Blaze3D GPU abstraction
 * ({@link GpuDevice} / {@link RenderPass})
 *
 * <p>Modeled on the imgui-mc project's {@code ImGuiRenderImplRenderSystem}:
 */
public class ImGuiImplBlaze3D {

    private static final Identifier VERTEX_SHADER_ID = Identifier.fromNamespaceAndPath("craftui", "imgui_vertex");
    private static final Identifier FRAGMENT_SHADER_ID = Identifier.fromNamespaceAndPath("craftui", "imgui_fragment");

    private static final String VERTEX_SHADER = """
            #version 410 core
            layout (location = 0) in vec2 Position;
            layout (location = 1) in vec2 UV;
            layout (location = 2) in vec4 Color;
            layout(std140) uniform Projection {
                mat4 ProjMtx;
            };
            out vec2 Frag_UV;
            out vec4 Frag_Color;
            void main()
            {
                Frag_UV = UV;
                Frag_Color = Color;
                gl_Position = ProjMtx * vec4(Position.xy, 0, 1);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            in vec2 Frag_UV;
            in vec4 Frag_Color;
            uniform sampler2D Texture;
            layout (location = 0) out vec4 Out_Color;
            void main()
            {
                Out_Color = Frag_Color * texture(Texture, Frag_UV.st);
            }
            """;

    private static final Map<Identifier, String> SHADER_SOURCES = Map.of(
            VERTEX_SHADER_ID, VERTEX_SHADER,
            FRAGMENT_SHADER_ID, FRAGMENT_SHADER);

    /** Vertex format matching imgui's {@code ImDrawVert}: pos(2f) + uv(2f) + color(RGBA8) = 20 bytes. */
    private static final VertexFormat VERTEX_FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RG32_FLOAT)
            .addAttribute("UV", GpuFormat.RG32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .build();

    private static final BindGroupLayout BIND_GROUP_LAYOUT = BindGroupLayout.builder()
            .withSampler("Texture")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .build();

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("craftui", "pipeline/imgui"))
            .withVertexShader(VERTEX_SHADER_ID)
            .withFragmentShader(FRAGMENT_SHADER_ID)
            .withBindGroupLayout(BIND_GROUP_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexBinding(0, VERTEX_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(Optional.empty())
            .build();

    /**
     * Opaque variant used to composite the finished game image into the viewport sub-rectangle.
     */
    private static final RenderPipeline BLIT_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("craftui", "pipeline/imgui_blit"))
            .withVertexShader(VERTEX_SHADER_ID)
            .withFragmentShader(FRAGMENT_SHADER_ID)
            .withBindGroupLayout(BIND_GROUP_LAYOUT)
            .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL))
            .withCull(false)
            .withVertexBinding(0, VERTEX_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(Optional.empty())
            .build();

    /** ImGui texture id reserved for the font atlas. Custom textures are numbered from {@code 2}. */
    private static final long FONT_TEXTURE_ID = 1;

    private GpuTexture fontTexture;
    private GpuTextureView fontTextureView;
    private GpuSampler fontSampler;

    /** A texture bound by an ImGui draw command: the GPU view plus an optional custom sampler. */
    private record TextureBinding(GpuTextureView view, @Nullable GpuSampler sampler) {}

    // Maps ImGui texture ids (the long an ImDrawCmd carries) to their GPU views. ImGui's ImTextureID
    // can only hold a long, but Blaze3D's RenderPass.bindTexture needs the GpuTextureView object, so
    // a texture is handed a stable id once (via textureId(...)) and looked up here at bind time. Ids
    // persist for the backend's lifetime; the font atlas is id 1 and custom ids count up from 2.
    private final Map<GpuTextureView, Long> idByView = new IdentityHashMap<>();
    private final Map<Long, TextureBinding> bindingById = new HashMap<>();
    private long nextTextureId = FONT_TEXTURE_ID + 1;

    private GpuBuffer projectionBuffer;
    private final Matrix4f projectionMatrix = new Matrix4f();
    private float projLeft, projRight, projBottom, projTop;

    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;
    private int vertexBufferSize;
    private int indexBufferSize;

    // Resources for compositing the game texture into the viewport sub-rectangle (a single
    // textured quad drawn with the same pipeline). Kept separate from the ImGui draw buffers so
    // the two draws in a frame don't clobber each other's projection UBO / vertex data.
    private GpuBuffer blitProjectionBuffer;
    private final Matrix4f blitProjectionMatrix = new Matrix4f();
    private float blitProjW, blitProjH;
    private GpuBuffer blitVertexBuffer;
    private GpuBuffer blitIndexBuffer;

    private final ImVec4 clipRect = new ImVec4();

    /**
     * Target to draw ImGui into instead of the main render target. Set per-frame when a custom
     * viewport is active so ImGui composites over the full-window {@link ViewportCompositor}
     * texture rather than the (shrunken) game render target.
     */
    @Getter
    @Setter
    private GpuTextureView targetOverride;

    private static String getShaderSource(final Identifier id, final ShaderType type) {
        return SHADER_SOURCES.get(id);
    }

    public boolean init() {
        final ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName("imgui-java_impl_blaze3d");
        io.addBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);
        RenderSystem.getDevice().precompilePipeline(PIPELINE, ImGuiImplBlaze3D::getShaderSource);
        return true;
    }

    /** Ensures GPU objects exist. Mirrors the {@code newFrame} lazy-init used by the other backends. */
    public void newFrame() {
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

        device.createCommandEncoder().writeToTexture(fontTexture, pixels, 0, 0, 0, 0, w, h);

        fontSampler = RenderSystem.getSamplerCache().getSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, false);

        fontAtlas.setTexID(FONT_TEXTURE_ID);
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

    /**
     * Returns the ImGui texture id for an already-uploaded GPU texture, to hand to
     * {@code ImGui.image(...)} / {@code ImGui.imageButton(...)} and friends.
     *
     * <p>This is purely a GPU-side handle. The caller owns the {@link GpuTextureView} (however it was
     * produced — a render target, a manually uploaded texture, etc.) and its lifetime; the backend
     * never touches CPU pixel data. The id is assigned once and stays stable for the backend's
     * lifetime, so calling this repeatedly with the same view is cheap and returns the same id — hold
     * onto the returned {@code long} and use it like you would a GL texture handle. When a texture is
     * gone for good, call {@link #releaseTextureId(GpuTextureView)} to drop the mapping.
     *
     * @param view    the GPU texture view to sample.
     * @param sampler the sampler to use, or {@code null} for the default linear/clamp sampler.
     * @return the ImGui texture id (always {@code >= 2}; the font atlas occupies id {@code 1}).
     */
    public long textureId(final GpuTextureView view, @Nullable final GpuSampler sampler) {
        Objects.requireNonNull(view, "view");
        final Long existing = idByView.get(view);
        final long id = existing != null ? existing : nextTextureId++;
        if (existing == null) {
            idByView.put(view, id);
        }
        // Keep the binding current so a caller may swap the sampler for an already-known view.
        bindingById.put(id, new TextureBinding(view, sampler));
        return id;
    }

    /**
     * Returns the ImGui texture id for a GPU texture, sampled with the default linear/clamp sampler.
     *
     * @see #textureId(GpuTextureView, GpuSampler)
     */
    public long textureId(final GpuTextureView view) {
        return textureId(view, null);
    }

    /**
     * Drops the ImGui texture id mapping for a view previously passed to {@link #textureId}. Call
     * this when the underlying texture is being destroyed so the backend stops referencing it. Any
     * draw command still carrying the stale id is skipped rather than drawn.
     */
    public void releaseTextureId(final GpuTextureView view) {
        final Long id = idByView.remove(view);
        if (id != null) {
            bindingById.remove(id);
        }
    }

    /**
     * Looks up the texture for a draw command's id and binds it. Returns {@code false} if the id is
     * unknown (e.g. a released texture), in which case the caller should skip the command.
     */
    private boolean bindDrawTexture(final RenderPass renderPass, final long textureId) {
        final GpuTextureView view;
        final GpuSampler sampler;
        if (textureId == FONT_TEXTURE_ID) {
            view = fontTextureView;
            sampler = fontSampler;
        } else {
            final TextureBinding binding = bindingById.get(textureId);
            if (binding == null) {
                return false;
            }
            view = binding.view();
            sampler = binding.sampler() != null ? binding.sampler() : fontSampler;
        }
        renderPass.bindTexture("Texture", view, sampler);
        return true;
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
        device.precompilePipeline(PIPELINE, ImGuiImplBlaze3D::getShaderSource);
        final CommandEncoder encoder = device.createCommandEncoder();

        uploadBuffers(device, encoder, drawData, totalVtxCount, totalIdxCount);

        // Plain 2D orthographic projection mapping ImGui display-space (top-left origin) straight to
        // clip space. bottom > top gives the Y-flip; depth range is irrelevant (depth test is off).
        final float left = drawData.getDisplayPosX();
        final float right = drawData.getDisplayPosX() + drawData.getDisplaySizeX();
        final float top = drawData.getDisplayPosY();
        final float bottom = drawData.getDisplayPosY() + drawData.getDisplaySizeY();
        final GpuBufferSlice projectionSlice = getProjectionBuffer(left, right, bottom, top);

        final GpuTextureView colorTexture = targetOverride != null
                ? targetOverride
                : Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView();
        // Scissor rectangles are expressed in the render target's real size; clamp against it since
        // the RenderPass rejects any scissor that exceeds its render area.
        final int renderW = colorTexture.getWidth(0);
        final int renderH = colorTexture.getHeight(0);
        final IndexType indexType = ImDrawData.sizeOfImDrawIdx() == 2 ? IndexType.SHORT : IndexType.INT;

        final float clipOffX = drawData.getDisplayPosX();
        final float clipOffY = drawData.getDisplayPosY();
        final float clipScaleX = drawData.getFramebufferScaleX();
        final float clipScaleY = drawData.getFramebufferScaleY();

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "CraftUI ImGui",
                colorTexture, Optional.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setUniform("Projection", projectionSlice);
            renderPass.setVertexBuffer(0, vertexBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, indexType);

            int globalVtxOffset = 0;
            int globalIdxOffset = 0;
            for (int n = 0; n < cmdListsCount; n++) {
                final int cmdCount = drawData.getCmdListCmdBufferSize(n);
                for (int cmdIdx = 0; cmdIdx < cmdCount; cmdIdx++) {
                    drawData.getCmdListCmdBufferClipRect(clipRect, n, cmdIdx);
                    final float clipMinX = (clipRect.x - clipOffX) * clipScaleX;
                    final float clipMinY = (clipRect.y - clipOffY) * clipScaleY;
                    final float clipMaxX = (clipRect.z - clipOffX) * clipScaleX;
                    final float clipMaxY = (clipRect.w - clipOffY) * clipScaleY;
                    if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                        continue;
                    }

                    // Apply scissor/clipping rectangle. Scissor coordinates use a bottom-left origin,
                    // so Y is flipped relative to ImGui's top-left clip rectangles.
                    final int minX = Math.max((int) clipMinX, 0);
                    final int minY = Math.max((int) (fbHeight - clipMaxY), 0);
                    if (renderW < minX || renderH < minY) {
                        continue;
                    }
                    final int scissorWidth = Math.clamp((int) (clipMaxX - clipMinX), 0, renderW - minX);
                    final int scissorHeight = Math.clamp((int) (clipMaxY - clipMinY), 0, renderH - minY);
                    renderPass.enableScissor(minX, minY, scissorWidth, scissorHeight);

                    // Bind whichever texture this draw command references (font atlas, or a texture
                    // registered this frame via registerTexture). Skip commands whose id is unknown.
                    final long textureId = drawData.getCmdListCmdBufferTextureId(n, cmdIdx);
                    if (!bindDrawTexture(renderPass, textureId)) {
                        continue;
                    }

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

    /**
     * Composite the (frame-sized) game color texture into the viewport sub-rectangle of a
     * full-window target, clearing the rest to black. Draws a single textured quad through the
     * ImGui pipeline, so the game image is scaled to exactly the sub-rectangle regardless of any
     * size difference. Coordinates are in top-left-origin screen pixels of the {@code target}.
     */
    public void renderGameToComposite(final GpuTextureView target, final int fbWidth, final int fbHeight,
                                      final GpuTextureView game, final int x, final int y, final int w, final int h) {
        final GpuDevice device = RenderSystem.getDevice();
        device.precompilePipeline(BLIT_PIPELINE, ImGuiImplBlaze3D::getShaderSource);
        final CommandEncoder encoder = device.createCommandEncoder();

        if (fontSampler == null) {
            createFontsTexture();
        }

        final GpuBufferSlice projection = getBlitProjectionBuffer(device, encoder, fbWidth, fbHeight);
        uploadBlitQuad(device, encoder, x, y, w, h);

        // Clear the whole target to opaque black, then draw the game quad on top (opaque copy).
        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "CraftUI Viewport Composite",
                target, Optional.of(new Vector4f(0f, 0f, 0f, 1f)))) {
            renderPass.setPipeline(BLIT_PIPELINE);
            renderPass.setUniform("Projection", projection);
            renderPass.setVertexBuffer(0, blitVertexBuffer.slice());
            renderPass.setIndexBuffer(blitIndexBuffer, IndexType.SHORT);
            renderPass.bindTexture("Texture", game, fontSampler);
            renderPass.drawIndexed(6, 1, 0, 0, 0);
        }
    }

    private GpuBufferSlice getBlitProjectionBuffer(final GpuDevice device, final CommandEncoder encoder,
                                                   final int fbWidth, final int fbHeight) {
        if (blitProjectionBuffer == null) {
            blitProjectionBuffer = device.createBuffer(
                    () -> "CraftUI Viewport Blit Projection",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
                    RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
            blitProjW = blitProjH = Float.NaN;
        }
        if (blitProjW != fbWidth || blitProjH != fbHeight) {
            // Top-left origin ortho: (0,0) top-left, (fbWidth, fbHeight) bottom-right.
            blitProjectionMatrix.setOrtho(0f, fbWidth, fbHeight, 0f, -1.0F, 1.0F);
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final ByteBuffer buffer = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
                        .putMat4f(blitProjectionMatrix).get();
                encoder.writeToBuffer(blitProjectionBuffer.slice(), buffer);
            }
            blitProjW = fbWidth;
            blitProjH = fbHeight;
        }
        return blitProjectionBuffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    private void uploadBlitQuad(final GpuDevice device, final CommandEncoder encoder,
                                final int x, final int y, final int w, final int h) {
        if (blitVertexBuffer == null) {
            blitVertexBuffer = device.createBuffer(() -> "CraftUI Viewport Blit Vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 4 * 20);
        }
        if (blitIndexBuffer == null) {
            blitIndexBuffer = device.createBuffer(() -> "CraftUI Viewport Blit Indices",
                    GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, 6 * 2);
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final ByteBuffer idx = stack.malloc(6 * 2);
                idx.putShort((short) 0).putShort((short) 1).putShort((short) 2);
                idx.putShort((short) 0).putShort((short) 2).putShort((short) 3);
                idx.flip();
                encoder.writeToBuffer(blitIndexBuffer.slice(), idx);
            }
        }

        final float x0 = x, y0 = y, x1 = x + w, y1 = y + h;
        final int color = 0xFFFFFFFF;
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final ByteBuffer vtx = stack.malloc(4 * 20);
            // pos(2f) uv(2f) color(RGBA8). V is flipped (game color texture is bottom-left origin).
            putVert(vtx, x0, y0, 0f, 1f, color); // top-left
            putVert(vtx, x1, y0, 1f, 1f, color); // top-right
            putVert(vtx, x1, y1, 1f, 0f, color); // bottom-right
            putVert(vtx, x0, y1, 0f, 0f, color); // bottom-left
            vtx.flip();
            encoder.writeToBuffer(blitVertexBuffer.slice(), vtx);
        }
    }

    private static void putVert(final ByteBuffer buffer, final float px, final float py,
                                final float u, final float v, final int color) {
        buffer.putFloat(px).putFloat(py).putFloat(u).putFloat(v).putInt(color);
    }

    private GpuBufferSlice getProjectionBuffer(final float left, final float right, final float bottom, final float top) {
        if (projectionBuffer == null) {
            projectionBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "CraftUI ImGui Projection",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
                    RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
            projLeft = projRight = projBottom = projTop = Float.NaN;
        }
        if (projLeft != left || projRight != right || projBottom != bottom || projTop != top) {
            projectionMatrix.setOrtho(left, right, bottom, top, -1.0F, 1.0F);
            try (final MemoryStack stack = MemoryStack.stackPush()) {
                final ByteBuffer buffer = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
                        .putMat4f(projectionMatrix).get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(projectionBuffer.slice(), buffer);
            }
            projLeft = left;
            projRight = right;
            projBottom = bottom;
            projTop = top;
        }
        return projectionBuffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    private void uploadBuffers(final GpuDevice device, final CommandEncoder encoder, final ImDrawData drawData,
                               final int totalVtxCount, final int totalIdxCount) {
        final int vtxBytes = totalVtxCount * ImDrawData.sizeOfImDrawVert();
        final int idxBytes = totalIdxCount * ImDrawData.sizeOfImDrawIdx();

        vertexBuffer = ensureBuffer(device, vertexBuffer, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                vtxBytes, true);
        indexBuffer = ensureBuffer(device, indexBuffer, GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                idxBytes, false);

        // Upload each command list's vertex/index data verbatim (no format change needed).
        // NOTE: getCmdListVtxBufferData / getCmdListIdxBufferData reuse a single shared ByteBuffer,
        // so each must be fully consumed before the next call (vtx, then idx, per list).
        int vtxByteOffset = 0;
        int idxByteOffset = 0;
        for (int n = 0; n < drawData.getCmdListsCount(); n++) {
            final ByteBuffer vtx = drawData.getCmdListVtxBufferData(n);
            final int vtxLen = vtx.remaining();
            encoder.writeToBuffer(vertexBuffer.slice(vtxByteOffset, vtxLen), vtx);
            vtxByteOffset += vtxLen;

            final ByteBuffer idx = drawData.getCmdListIdxBufferData(n);
            final int idxLen = idx.remaining();
            encoder.writeToBuffer(indexBuffer.slice(idxByteOffset, idxLen), idx);
            idxByteOffset += idxLen;
        }
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
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
        if (blitProjectionBuffer != null) {
            blitProjectionBuffer.close();
            blitProjectionBuffer = null;
        }
        if (blitVertexBuffer != null) {
            blitVertexBuffer.close();
            blitVertexBuffer = null;
        }
        if (blitIndexBuffer != null) {
            blitIndexBuffer.close();
            blitIndexBuffer = null;
        }
        vertexBufferSize = 0;
        indexBufferSize = 0;

        final ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName(null);
        io.removeBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);
    }
}
