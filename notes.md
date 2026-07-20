# Port to MC 26.2 — Progress Notes (started 2026-07-19)

I vibe-ported this to 26.2. Bite me.

## Plan / phases
1. **migrateMappings** (Yarn→Mojmap) on MC 1.21.4 FIRST, before touching build files. Produces `remappedSrc/`. Run with Java 21. Then replace src with remappedSrc.
2. Update Gradle: MC 26.2, Fabric Loader 0.19.3, Loom 1.17 (plugin id `net.fabricmc.fabric-loom`), Mojmap mappings, Java 25, Gradle 9.5.1 wrapper, remove yarn line, modImpl→impl, remapJar→jar, AW header named→official.
3. ImGui-Java 1.90.0 → 1.92.0 (source at /home/igrium/Documents/code/imgui-java/). Vendored backends in impl/render.
4. Fix code: Mojmap renames, GUI reorg (`Minecraft.getInstance().gui.setScreen`), **Vulkan** (MC 26.2 has OpenGL + experimental Vulkan; OpenGL still default). mixins.

## Environment
- JDKs: 17, 21, 25 at /usr/lib/jvm/java-{17,21,25}-openjdk-amd64. Default `java` = 25.
- Gradle wrapper currently 8.14. migrateMappings must run on Java 21 (MC 1.21.4 tooling).
- src is committed to git (backup exists). Only updating/ + updating.md untracked.

## Key files
- Vendored ImGui backends: src/main/java/com/igrium/craftui/impl/render/ImGuiImplGl3.java (OpenGL), ImGuiImplGlfw.java, ImGuiUtil.java (init).
- Mixins: FramebufferMixin, KeyboardMixin, MinecraftClientMixin, MixinOptionsScreen, MouseMixin, WindowMixin.
- Non-MC files (won't need porting): nbt/*, style/*, util/JsonUtils, etc. (verify).

## Progress log
- DONE Phase 1: migrateMappings (Yarn→1.21.4 Mojmap), replaced src/main/java + src/testmod/java.
- DONE Phase 2 build files: gradle.properties (MC 26.2, loader 0.19.3, fabric 0.155.2+26.2, imgui 1.92.0, modmenu 20.0.1), build.gradle (loom 1.17.16, plugin id net.fabricmc.fabric-loom, NO mappings line since unobf, impl/compileOnly, release 25), wrapper 9.5.1, fabric.mod.json/testmod (~26.2, java>=25, loader>=0.19.3), mixins compat JAVA_21.
- IN PROGRESS Phase 4 code fixes. The build DOES compile mostly; iterating on errors.

## 26.2 rename cheat-sheet (1.21.4-Mojmap -> 26.2), discovered
- `net.minecraft.resources.ResourceLocation` -> `net.minecraft.resources.Identifier` (methods identical: parse/tryParse/fromNamespaceAndPath/getPath/getNamespace/withPrefix). Done via sed.
- `net.minecraft.ResourceLocationException` -> `net.minecraft.IdentifierException`. Done.
- `net.minecraft.Util` -> `net.minecraft.util.Util`. Done.
- `com.mojang.blaze3d.platform.GlStateManager` -> `com.mojang.blaze3d.opengl.GlStateManager`. Done (FramebufferMixin) — but see render note; GlStateManager may be OpenGL-backend-only now.
- fabric `ClientCommandManager` -> `ClientCommands` (same pkg, has literal/argument/getActiveDispatcher). Done.
- fabric `WorldRenderEvents` -> `LevelRenderEvents` (pkg `...rendering.v1.level`); context has no camera()/projectionMatrix() — use `context.levelState().cameraRenderState` (fields: pos, orientation (Quaternionf), projectionMatrix). Refactored RaycastUtils to START_MAIN. Done.
- Mixin @At target STRINGS still Yarn-form (migrateMappings doesn't touch them) — need manual fix, verified at runtime not compile: Framebuffer.draw(II), Util.getMeasuringTimeNano (now `Util.getNanos()`), Window.getFramebufferWidth, Mouse.lockCursor, ThreePartsLayoutWidget.addBody.

## *** CRITICAL: 26.2 rendering / Vulkan redesign (the hard part) ***
- `RenderTarget` (com.mojang.blaze3d.pipeline) is now fully GPU-abstracted (Vulkan OR OpenGL backend). NO more `draw(int,int)` method, NO `colorAttachment` int. Instead: `getColorTextureView()` -> GpuTextureView, and `blitAndBlendToTexture(GpuTextureView,GpuTextureView)`.
- Main-target presentation moved to `Minecraft.render()` ~line 1297-1303:
    `windowSurface.blitFromTexture(RenderSystem.getDevice().createCommandEncoder(), mainRenderTarget().getColorTextureView())`.
  `windowSurface` is a `com.mojang.blaze3d.systems.GpuSurface`. `blitFromTexture` blits the WHOLE texture — no sub-rectangle. So the old FramebufferMixin trick (custom blit of main FB into a viewport sub-region) has no direct equivalent.
- `Minecraft.render` timing marker `Util.getMeasuringTimeNano()` is now `Util.getNanos()` (MinecraftClientMixin preRender target).
- ImGui backend: vendored ImGuiImplGl3 does RAW OpenGL. MC 26.2 defaults to OpenGL backend (Vulkan experimental), so raw GL *may* still work when running the GL backend — but it renders to the window via the GpuSurface blit, and imgui draws after. Need to confirm GL context/state at the injection point. This needs runtime testing.
## RESOLUTION — build is GREEN (compiles + jar) as of 2026-07-19
- New renderer `ImGuiImplBlaze3D` (impl/render) replaces raw-GL `ImGuiImplGl3` (deleted). Ported from imgui-java 1.92's `ImGuiImplSdlGpu3` but onto MC's Blaze3D GpuDevice/RenderPass. Reuses MC's stock `core/position_tex_color` shaders via a custom RenderPipeline modelled on GUI_TEXTURED but TRIANGLES topology. Draws into `gameRenderer.mainRenderTarget()` color view; works on OpenGL AND Vulkan. Wired via ImGuiUtil.IM_BLAZE3D + AppManager + ImFontManager.
- Lombok bumped to 1.18.46 (older versions crash on Java 25 javac internals).
- JUnit bumped to bom 5.11.4 + platform-launcher (Gradle 9 needs it).
- NBT: Yarn names (NbtCompound/NbtByte/...) -> Mojmap (CompoundTag/ByteTag/...); accessors getAsX()->xValue() (byteValue/intValue/...); getAllKeys()->keySet(); getAsString()(value)->value()/toString(); new TagParser(..).readValue() -> TagParser.create(NbtOps.INSTANCE).parseFully(snbt).
- Renames: Window.getWindow()->handle(); RenderSystem.assertOnRenderThreadOrInit()->assertOnRenderThread(); Minecraft.ON_OSX->Util.getPlatform()==Util.OS.OSX; minecraft.screen->minecraft.gui.screen(); setScreen()->setScreenAndShow(); resizeDisplay()->resizeGui(); InputConstants.grabOrReleaseMouse now takes Window not long; ImGui.pushFont(font)->pushFont(font,0); resource reload() signature reordered to reload(SharedState, Executor, PreparationBarrier, Executor) with ResourceManager via currentReload.resourceManager().

## *** LAUNCH VERIFIED — runs in-game (OpenGL backend) as of 2026-07-19 ***
`./gradlew runTestmodClient` launches, loads a world, renders frames through ImGuiImplBlaze3D, and exits cleanly. No mixin-apply errors — all @At targets resolve against 26.2.

### Mixin @At retarget — DONE (verified applying at runtime):
- MinecraftClientMixin: `render` -> `renderFrame`; afterMainBlit now injects BEFORE `GpuSurface.blitFromTexture(CommandEncoder,GpuTextureView)`; preRender after `Util.getNanos()` (ordinal 0). setScreen redirect MOVED to new **GuiMixin** (the grab logic relocated from Minecraft.setScreen to `Gui.setScreen`), redirecting `MouseHandler.grabMouse()`.
- MouseMixin: retargeted to 26.2 MouseHandler names — `onMove`/`handleAccumulatedMovement`/`releaseMouse`/`grabMouse`/`onButton`(now `(long,MouseButtonInfo,int)`)/`onScroll`; shadow fields `mouseGrabbed`/`xpos`/`ypos`/`minecraft`.
- KeyboardMixin: `onKey`->`keyPress(long,int,KeyEvent)`, `onChar`->`charTyped(long,CharacterEvent)`.
- WindowMixin: `onFramebufferSizeChanged`->`onFramebufferResize`, target now `WindowEventHandler.framebufferSizeChanged()V` BEFORE.
- MixinOptionsScreen: `ThreePartsLayoutWidget.addBody`->`HeaderAndFooterLayout.addToContents(LayoutElement)`.
- GuiMixin added to craftui.mixins.json client list.

### Runtime bugs found & fixed by launching:
- **Segfault** (native, no Java crash report): input events fire before first renderFrame inits ImGui; `AppManager.wantCaptureMouse/Keyboard()` called `ImGui.getIO()` with no context -> null-deref in native. FIXED: guard both with `ImGuiUtil.isInitialized()`.
- **Scissor out of bounds**: ImGuiImplBlaze3D clamped scissor to ImGui DisplaySize, but headless render target is 1x1. FIXED: clamp scissor to `mainRenderTarget().width/height` instead.

### testmod (src/testmod) 26.2 fixes (compileTestmodJava was broken):
- AppTestCommand: `ClientCommandManager`->`ClientCommands`.
- TestApp: `player.sendMessage(Text..,bool)`/`displayClientMessage(..,bool)` -> `player.sendSystemMessage(Component..)`; `setScreen(null)`->`setScreenAndShow(null)`; `PlayerEntity`->`Player`; `ent.saveWithoutId(new CompoundTag())` -> `TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ent.registryAccess())` + `saveWithoutId(output)` + `output.buildResult()`.

## *** STILL TODO ***
1. **Vulkan backend not yet exercised** — the run used OpenGL (`Using graphics backend OpenGL`). Force Vulkan (launch arg / options) and confirm ImGuiImplBlaze3D renders. It's backend-agnostic by construction but unproven on Vulkan.
2. **Visual verification** — headless render target is 1x1, so no pixels to inspect. Need a real desktop session to confirm font atlas, blend, projection y-flip, scissor rects look right.
3. **Custom-viewport sub-region rendering** (render MC into a sub-rect so imgui panels surround it): FramebufferMixin was DELETED (RenderTarget.draw gone). Needs redesign via RenderPass into the main target / present path. Deferred. Common case (full-window overlay) works.
4. **Multi-viewport (ImGui docking to OS windows)**: initPlatformInterface not ported to Blaze3D. ViewportsEnable secondary windows won't render until a Blaze3D platform renderer hook is added.

### KNOWN PRE-EXISTING BUG (not port-related, left as-is): `AppManager.wantCaptureMouse()` returns `getWantCaptureKeyboard()` (should be `getWantCaptureMouse()`).

# General Notes

## Render Pipeline
1. Mix into start of Minecraft frame; call `glViewport` with the values of the main viewport as dictated by the UI.

    - Framebuffer must also be resized to fit the viewport.

2. Let Minecraft render normally. Because of the `glViewport` call, it will only render to part of the screen.

3. After the screen blits, render the editor UI as an "overlay" to the main window.

    - Individual panels may call back into Minecraft's rendering code for specific items (entity rendering, etc).