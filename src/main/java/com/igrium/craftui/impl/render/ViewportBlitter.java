package com.igrium.craftui.impl.render;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
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
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Composites the confined world render into a sub-rectangle of a full-window target.
 * <p>
 * This is done by drawing a single textured quad, rather than a raw same-size pixel copy.
 * Ported from the pre-library custom ImGui backend, where this exact technique was used to solve
 * the same problem:
 * <ul>
 *     <li>A raw copy assumes the source render target's actual size always exactly matches the
 *     destination sub-rectangle
 *     <li>If those ever drift out of sync for a frame (e.g. the world render target's resize
 *     lands one frame later than the panel's own layout), a copy leaves a gap or samples the
 *     wrong region.
 *     <li>A quad draw always fills the destination rectangle exactly, resampling the source to
 *     fit, so any such mismatch is invisible instead of leaving a visible black gap.
 *     <li>TODO: figure out why this still breaks even if they're the same size
 * </ul>
 */
public class ViewportBlitter {

    private static final Identifier VERTEX_SHADER_ID = Identifier.parse("craftui:viewport_blit_vertex");
    private static final Identifier FRAGMENT_SHADER_ID = Identifier.parse("craftui:viewport_blit_fragment");

    private static final String VERTEX_SHADER = """
            #version 410 core
            layout (location = 0) in vec2 Position;
            layout (location = 1) in vec2 UV;
            layout(std140) uniform Projection {
                mat4 ProjMtx;
            };
            out vec2 Frag_UV;
            void main()
            {
                Frag_UV = UV;
                gl_Position = ProjMtx * vec4(Position.xy, 0, 1);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            in vec2 Frag_UV;
            uniform sampler2D Texture;
            layout (location = 0) out vec4 Out_Color;
            void main()
            {
                Out_Color = texture(Texture, Frag_UV.st);
            }
            """;

    private static final Map<Identifier, String> SHADER_SOURCES = Map.of(
            VERTEX_SHADER_ID, VERTEX_SHADER,
            FRAGMENT_SHADER_ID, FRAGMENT_SHADER);

    /**
     * Vertex format: pos(2f) + uv(2f) = 16 bytes.
     */
    private static final VertexFormat VERTEX_FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RG32_FLOAT)
            .addAttribute("UV", GpuFormat.RG32_FLOAT)
            .build();

    private static final BindGroupLayout BIND_GROUP_LAYOUT = BindGroupLayout.builder()
            .withSampler("Texture")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .build();

    private static final RenderPipeline BLIT_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("craftui", "pipeline/viewport_blit"))
            .withVertexShader(VERTEX_SHADER_ID)
            .withFragmentShader(FRAGMENT_SHADER_ID)
            .withBindGroupLayout(BIND_GROUP_LAYOUT)
            .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM,
                    ColorTargetState.WRITE_ALL))
            .withCull(false)
            .withVertexBinding(0, VERTEX_FORMAT)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(Optional.empty())
            .build();

    private GpuSampler sampler;

    private GpuBuffer projectionBuffer;
    private final Matrix4f projectionMatrix = new Matrix4f();
    private float projW = Float.NaN, projH = Float.NaN;

    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;

    private static String getShaderSource(final Identifier id, final ShaderType type) {
        return SHADER_SOURCES.get(id);
    }

    /**
     * Draw {@code source} into the ({@code x}, {@code y}, {@code w}, {@code h}) sub-rectangle of
     * {@code target} (a {@code fbWidth} x {@code fbHeight} full-window target), clearing the rest to
     * opaque black. {@code source} is resampled to exactly fill the destination rectangle regardless
     * of its own actual size. Coordinates are top-left-origin screen pixels of {@code target}.
     */
    public void blit(GpuTextureView target, int fbWidth, int fbHeight, GpuTextureView source,
                     int x, int y, int w, int h) {
        GpuDevice device = RenderSystem.getDevice();
        device.precompilePipeline(BLIT_PIPELINE, ViewportBlitter::getShaderSource);
        CommandEncoder encoder = device.createCommandEncoder();

        if (sampler == null) {
            sampler = RenderSystem.getSamplerCache().getSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.LINEAR, false);
        }

        GpuBufferSlice projection = getProjectionBuffer(device, encoder, fbWidth, fbHeight);
        uploadQuad(device, encoder, x, y, w, h);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "CraftUI Viewport Composite",
                target, Optional.of(new Vector4f(0f, 0f, 0f, 1f)))) {
            renderPass.setPipeline(BLIT_PIPELINE);
            renderPass.setUniform("Projection", projection);
            renderPass.setVertexBuffer(0, vertexBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, IndexType.SHORT);
            renderPass.bindTexture("Texture", source, sampler);
            renderPass.drawIndexed(6, 1, 0, 0, 0);
        }
    }

    private GpuBufferSlice getProjectionBuffer(GpuDevice device, CommandEncoder encoder, int fbWidth, int fbHeight) {
        if (projectionBuffer == null) {
            projectionBuffer = device.createBuffer(
                    () -> "CraftUI Viewport Blit Projection",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
                    RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        }
        if (projW != fbWidth || projH != fbHeight) {
            // Top-left origin ortho: (0,0) top-left, (fbWidth, fbHeight) bottom-right.
            projectionMatrix.setOrtho(0f, fbWidth, fbHeight, 0f, -1.0F, 1.0F);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer buffer = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE)
                        .putMat4f(projectionMatrix).get();
                encoder.writeToBuffer(projectionBuffer.slice(), buffer);
            }
            projW = fbWidth;
            projH = fbHeight;
        }
        return projectionBuffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    private void uploadQuad(GpuDevice device, CommandEncoder encoder, int x, int y, int w, int h) {
        if (vertexBuffer == null) {
            vertexBuffer = device.createBuffer(() -> "CraftUI Viewport Blit Vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, 4 * 16);
        }
        if (indexBuffer == null) {
            indexBuffer = device.createBuffer(() -> "CraftUI Viewport Blit Indices",
                    GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, 6 * 2);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer idx = stack.malloc(6 * 2);
                idx.putShort((short) 0).putShort((short) 1).putShort((short) 2);
                idx.putShort((short) 0).putShort((short) 2).putShort((short) 3);
                idx.flip();
                encoder.writeToBuffer(indexBuffer.slice(), idx);
            }
        }

        float x0 = x, y0 = y, x1 = x + w, y1 = y + h;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vtx = stack.malloc(4 * 16);
            // pos(2f) uv(2f). V is flipped (game color texture is bottom-left origin).
            putVert(vtx, x0, y0, 0f, 1f); // top-left
            putVert(vtx, x1, y0, 1f, 1f); // top-right
            putVert(vtx, x1, y1, 1f, 0f); // bottom-right
            putVert(vtx, x0, y1, 0f, 0f); // bottom-left
            vtx.flip();
            encoder.writeToBuffer(vertexBuffer.slice(), vtx);
        }
    }

    private static void putVert(ByteBuffer buffer, float px, float py, float u, float v) {
        buffer.putFloat(px).putFloat(py).putFloat(u).putFloat(v);
    }
}
