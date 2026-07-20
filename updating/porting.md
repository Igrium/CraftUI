# Porting to 26.1

Minecraft went obfuscation-free starting with version **26.1**, and Fabric stopped maintaining Yarn from that point on — so any mod moving from 1.21.4 (Yarn) up to 26.x must switch to **Mojang's official mappings (Mojmap)** first, using Fabric Loom's `migrateMappings` Gradle task. [Fabric Documentation](https://docs.fabricmc.net/develop/porting/mappings/)

### Gradle steps

**1. Run the migration task** (requires Loom 1.13+), pointing at the mappings for your *current* Minecraft version, not the target:

sh

```sh
./gradlew migrateMappings --mappings "net.minecraft:mappings:1.21.4" --overrideInputsIHaveABackup
```

On Windows use `./gradlew.bat`. Do **not** touch `gradle.properties` or `build.gradle` yet — run this before any dependency changes. [Fabric Documentation](https://docs.fabricmc.net/1.21.11/develop/porting/mappings/loom)

This drops remapped sources into `remappedSrc` by default. Check them over, then copy them over your real source tree (or pass `--output src/main/java` to overwrite in place directly). [Fabric Documentation](https://docs.fabricmc.net/1.21.11/develop/porting/mappings/loom)

**2. If you have a split client source set**, migrate it separately:

sh

```sh
./gradlew migrateClientMappings --mappings "net.minecraft:mappings:1.21.4" --overrideInputsIHaveABackup
```

**3. If you use access wideners / class tweakers**, migrate those too:

sh

```sh
./gradlew migrateClassTweakerMappings --mappings "net.minecraft:mappings:1.21.4" --overrideInputsIHaveABackup
```

[Fabric Documentation](https://docs.fabricmc.net/1.21.11/develop/porting/mappings/loom)

**4. Update `build.gradle`** to point at Mojmap instead of Yarn:

groovy

```groovy
dependencies {
    // remove: mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    mappings loom.officialMojangMappings()
}
```

You can also drop the now-unused `yarn_mappings` line from `gradle.properties`. Then refresh the Gradle project in your IDE. [Fabric Documentation](https://docs.fabricmc.net/1.21.11/develop/porting/mappings/loom)

**5. Bump your Minecraft version** in `gradle.properties` to whatever 26.x target you're porting to, and update `fabric-loader`/Loom versions as needed — *only after* the mappings migration is done and compiling cleanly on your old Minecraft version.

### Notes

- `migrateMappings` **doesn't support Kotlin** source. If your mod (or a dependency you're patching) is in Kotlin, use the **Ravel** IntelliJ IDEA plugin instead, which does the migration through the IDE and handles Kotlin. Fabric API itself used Ravel for its own Yarn→Mojmap migration. [Fabric Documentation](https://docs.fabricmc.net/develop/porting/mappings/)
- Neither tool is perfect — expect to manually fix up Mixin targets after the automated pass, since those are the most common casualty. [Fabric Documentation](https://docs.fabricmc.net/develop/porting/mappings/)
- [mappings.dev](https://mappings.dev/) and [Linkie](https://linkie.shedaniel.dev/mappings?namespace=yarn&translateMode=ns&translateAs=mojang_raw&search=) are useful for looking up Yarn↔Mojmap name equivalents while you clean things up. [Fabric Documentation](https://docs.fabricmc.net/1.21.11/develop/porting/mappings/loom)
