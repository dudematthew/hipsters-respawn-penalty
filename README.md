# Hipster's Respawn Penalty

Fabric 1.21.1 mod: death has a cost, then you can walk it off.

Derived from Hoaug's Respawn Penalty (MIT, Trần Kính Hoàng / Hoaug).

---

## What it does

- **Penalty levels** after death: lower max health and hunger. Sleep through the night or survive a Minecraft day to ease one level. A holy flask clears it immediately.
- **Death zone** (60 blocks): extra potions and weaker attacks if you rush the corpse. Leave the area and those extra effects drop.
- **Sanctuary**: if you die within 20 blocks of where you respawn, a short no-damage window so you can flee.
- **Fraying**: while penalized, held and worn gear wears out faster, including on the walk home. Sleep/day recovery leaves Fraying I for 3 minutes. The flask does not.
- **Keep inventory stays on.** This is not a loot-drop mod.

Clients need the mod installed for Fraying's red tint. The durability math still runs on the server.

---

## Requirements

- Minecraft `1.21.1`
- Fabric Loader `>= 0.19.2`
- Fabric API
- Java `21`
- Mod Menu (optional, for the in-game config button)

---

## Build

Windows PowerShell (JDK 21 on `JAVA_HOME`):

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-21"
.\gradlew.bat build
```

Jar:

```text
build/libs/hipsters-respawn-penalty-<version>.jar
```

---

## Install

1. Fabric Loader + Fabric API for 1.21.1
2. Drop the jar into `mods/`
3. Restart

`/give @s hipsters_respawn_penalty:holy_flask`

`/effect give @s hipsters_respawn_penalty:fraying`

---

## Configuration

Edit in-game via **Mod Menu → Hipster's Respawn Penalty**, or the file:

```text
config/hipsters_respawn_penalty.json
```

Cloth Config is bundled. Mod Menu is optional (included in `runClient` for testing).

---

## License

MIT. Original work by Trần Kính Hoàng (Hoaug). Subsequent work by dudematthew.
