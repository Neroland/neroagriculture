# Consuming Neroland Core

NeroAgriculture hard-depends on **Neroland Core 1.7.0 or newer within the 1.x API line**. The
build pin lives in `gradle.properties` and selects one artifact for each loader and Minecraft
version:

```text
za.co.neroland.nerolandcore:nerolandcore-<loader>-<mc>:1.7.0
```

## Local development

The build checks Maven Local first. Publish the sibling Core checkout when the required artifacts
are not already present:

```sh
cd ../neroland-core
./gradlew publishToMavenLocal
cd ../neroagriculture
./gradlew :fabric:26.2:build
```

This publishes all six Core loader/version artifacts under the local Maven repository. Do not use
a composite build here: the `nerolandcore_version` pin must describe the same dependency that CI
and released builds consume.

## GitHub Packages

Fresh clones and CI fall back to
`https://maven.pkg.github.com/Neroland/neroland-core`. Credentials come from Gradle properties
`gpr.user` / `gpr.key`, or from `GITHUB_ACTOR` / `GITHUB_TOKEN` when those properties are absent.
The token needs `read:packages` access to the Core packages.

The multiloader and publishing workflows grant package-read permission and pass their GitHub token
to Gradle. Publishing and resolution handle only build artifacts and public version strings; they
contain no player or personal data.

## Runtime floor

Generated loader metadata enforces these compatible Core ranges:

- Fabric: `>=1.7.0`
- Forge and NeoForge: `[1.7.0,2.0)`

Core remains a separate required mod and is never embedded in a NeroAgriculture jar.
