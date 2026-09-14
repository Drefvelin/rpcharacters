# RPCharacters

Build with Java 21 and Maven. This repository follows TFMCCore's build workflow:
pushes to any branch, same-repository PRs targeting `main`, and manual runs build
and test the plugin. Each build saves a JAR artifact for 90 days. Successful
pushes to `main` also upload `RPCharacters.jar` to the `latest` release.
New pushes do not cancel earlier builds. Fork PRs and Dependabot builds are
skipped because they cannot access the private dependency secret.

## Dependencies and CI setup

Private/server JAR dependencies belong in the gitignored `libs/` directory.
CI downloads them from release `v1` of `JustinasLa/tfmc-deps`, then verifies
the committed `libs/SHA256SUMS`. Paper API, Gson, JSON Simple, LuckPerms API,
WorldGuard and JOML resolve through Maven. Dependency versions match the local
server JARs used to verify this build.

The following additional assets must be uploaded to that private release before
the workflow can build RPCharacters (they were absent when this setup was made):

```powershell
gh release upload v1 --repo JustinasLa/tfmc-deps libs/ItemsAdder_4.0.17.jar libs/MMOItems-6.10.1.jar libs/MythicMobsPremium-5.13.1-SNAPSHOT.jar libs/PlaceholderAPI-2.12.2.jar libs/ProtocolLib.jar
```

The shared release already contains `TLibs-1.0.jar`, `MMOCore-1.13.1.jar` and
`MythicLib-dist-1.7.1.jar`. Local copies were taken from TFMCCore's verified libs.
Use the exact JARs matching `libs/SHA256SUMS`; replacing a shared release asset
with a different build can break other repositories' checksum checks.

A repository administrator must configure the Actions secret `DEPS_TOKEN`
with Contents: Read-only access to `JustinasLa/tfmc-deps`:

```powershell
gh secret set DEPS_TOKEN --repo Drefvelin/rpcharacters
```

The default `GITHUB_TOKEN` cannot read another private repository. Publishing
uses this repository's own `GITHUB_TOKEN` with `contents: write`.

## Local build

Populate `libs/` with the eight files listed in `libs/SHA256SUMS`, then run:

```powershell
mvn -B --no-transfer-progress '-P!deploy-live' clean package
```

Output: `target/RPCharacters.jar`.

As in TFMCCore, an optional `deploy-live` profile activates when the local
`C:/Users/HP/Desktop/Java/Minecraft/Server Stuff/plugins/RPCharacters` directory
exists. It copies the built JAR to the parent plugins directory. The command
above and CI disable that profile to build without deploying.
When using local deployment, keep only one RPCharacters plugin JAR in the
plugins directory; an older `rpcharacters-1.1.6.jar` is a separate filename.

When updating dependencies, update the POM, workflow asset names and checksum
manifest together. Do not commit dependency JARs.
