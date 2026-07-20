# Quick Guide: Porting a Fabric Mod from 1.21.4 (Yarn) to 26.2

Going from 1.21.4 straight to 26.2 crosses a major fault line in Minecraft's history: Mojang removed obfuscation starting with **26.1**, and Fabric dropped Yarn mappings entirely as a result. That means this isn't a normal one-step port — you effectively do it in two phases: **de-Yarn first, then jump to 26.2**.

## Phase 0: Understand the jump

- **1.21.4** is obfuscated and (per your setup) uses **Yarn** mappings.
- **26.1** was the first unobfuscated Minecraft release. Fabric stopped maintaining Yarn from this point on — there's no Yarn mapping set for 26.1 or 26.2 to migrate *into*.
- **26.2** builds on 26.1: it's a smaller release, focused mainly on rendering (an experimental Vulkan backend alongside OpenGL) and some registration/data-gen changes.

So the required order is: **Yarn (1.21.4) → Mojang Mappings (still on 1.21.4 or later) → 26.1 build tooling → 26.2 code changes.**

## Phase 1: Migrate off Yarn onto Mojang Mappings

Do this *before* touching your Minecraft version. Two options:

- **Loom's `migrateMappings` Gradle task** — semi-automated, but doesn't support Kotlin.
- **Ravel** (IntelliJ IDEA plugin) — also semi-automated, does support Kotlin, and tends to handle complex projects better since it uses the IDE to resolve changes. This is what the Fabric API team itself used.

Neither tool is perfect — plan to manually review the result, especially any Mixins, since mixin targets/selectors often need hand-fixing after a mapping migration.

## Phase 2: Update your build script to 26.1/26.2

Once you're on Mojang Mappings, update your Gradle setup:

1. `./gradlew wrapper --gradle-version latest` (26.2 wants **Gradle 9.5.1**).
2. Bump Minecraft, Fabric Loader, Fabric Loom, and Fabric API versions in `gradle.properties`. Use **Loom 1.17**, and the latest stable Fabric Loader (0.19.3 at time of writing).
3. Change the Loom plugin id from `id "fabric-loom"` to `id "net.fabricmc.fabric-loom"` (in `build.gradle` and `settings.gradle` if present there).
4. **Delete the `mappings` line** from your dependencies block — there's nothing to map anymore.
5. Swap `modImplementation` / `modCompileOnly` / `modApi` → `implementation` / `compileOnly` / `api`.
6. Drop or replace any dependency mods built for 1.21.11 or earlier — nothing from before 26.1 loads, even as compile-only.
7. If you use an access widener or class tweaker file, change the header from `named` to `official`.
8. Set Java compatibility to **25** (not 21).
9. Replace any `remapJar` task references with plain `jar`.
10. Refresh Gradle (`./gradlew --refresh-dependencies` if IntelliJ's refresh button won't do it).

IntelliJ users also need **2025.3+** for Java 25 / mixins to work correctly.

## Phase 3: Fix your code

### General 26.1 changes
Everything is now referenced by its real, unobfuscated name — check the [Fabric API 26.1 Porting Guide](https://docs.fabricmc.net/develop/porting/fabric-api) for the renames made to match Mojang's naming, and the [Java Edition 26.1 wiki page](https://minecraft.wiki/w/Java_Edition_26.1) for vanilla-side changes. If you use raw OpenGL calls instead of the Blaze3D API, migrate those now — OpenGL-only code is on borrowed time.

### 26.2-specific changes
- **Registration/data-gen split**: block and item ids are now stored separately in `BlockIds`, `BlockItemIds`, and `ItemIds`, used for data generation instead of raw `Block`/`Item` instances. `valueLookupBuilder` has been removed — separate your ids from your Block/Item instances to match.
- **GUI reorganization**: screen and HUD methods moved out of `Minecraft` into dedicated `Gui`/`Hud` classes:
  ```diff
  - Minecraft.getInstance().setScreen()
  + Minecraft.getInstance().gui.setScreen()
  ```
- **Rendering backend**: 26.2 lets players choose OpenGL or an experimental Vulkan backend. If you touch rendering directly, test both.
- **New Fabric API surface worth knowing about**:
  - Tag removal — `"fabric:remove": [...]` in tag JSON, for pulling entries out of a tag without replacing it wholesale.
  - Experimental Fluid Interaction API — `EntityFluidInteractionRegistry.register(tag, FluidBehavior.simple()...)` for custom fluid/entity interactions.
  - `.requires(FabricClientCommandSource::attended)` for client commands, so a command can't silently run from a server-sent, unclicked text component.
  - Enum extensions API (Fabric Loader 0.19.0 / Loom 1.17), via Mixin at runtime and class tweaking at compile time.

## Reference links

- [Fabric: Porting to 26.1](https://docs.fabricmc.net/develop/porting/)
- [Fabric: Migrating Mappings](https://docs.fabricmc.net/develop/porting/mappings/)
- [Fabric for Minecraft 26.1 (blog)](https://fabricmc.net/2026/03/14/261.html)
- [Fabric for Minecraft 26.2 (blog)](https://fabricmc.net/2026/06/15/262.html)
- [Fabric API 26.1 Porting Guide](https://docs.fabricmc.net/develop/porting/fabric-api)
- [Java Edition 26.2 — Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_26.2)
- [Fabric Example Mod, 26.1 branch](https://github.com/FabricMC/fabric-example-mod/tree/26.1)
