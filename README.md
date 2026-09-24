# Gravity Defied – Android port (dpsmpx fork)

**Gravity Defied** is an iconic trial racing mobile game originally developed by Codebrew Software in 2004 for the J2ME platform.

This repository is an **independent community fork of the Android port created and maintained by Gregory Klushnikov and Evgeny Zinoviev**:
[evgenyzinoviev/gravitydefied](https://github.com/evgenyzinoviev/gravitydefied).

The original Android port aimed to preserve the classic J2ME version of Gravity Defied on Android. This fork keeps that codebase as its foundation and adds substantial gameplay, rendering, localization, editor, replay, and Android-specific improvements.

**This is not an official Codebrew Software release and this fork is not presented as the official Gravity Defied Android application.** The original port's README states that its authors were not affiliated with Codebrew Software.

## Changes in this fork

This repository is based on the Android port created and maintained by Gregory Klushnikov and Evgeny Zinoviev. Since the original project, this fork has been substantially extended and modernized by **dpsmpx**.

### Gameplay and simulation
* **2× faster simulation:** the physics/game simulation runs at twice the original rate to make controls more responsive while preserving the existing game logic and physics model.
* **Best-run replays and ghost:** completed runs can be recorded and saved, and the player's best run can be shown as a transparent ghost during subsequent attempts. Replay data is invalidated when the corresponding level/track data changes.
* **Impossible track:** added a final **Impossible** track that combines sections from the original track sets and includes start/finish boundary extensions to keep the course playable.
* **Full track editor:** integrated an in-game editor for creating and editing tracks and level packs.

### Android and user interface
* **Russian localization:** added a complete Russian translation alongside English, with an in-game language selector and persistent language preference.
* **Perspective rendering fixes and improvements:** corrected perspective track alignment and vertical positioning so the bike is displayed between the perspective track lines.
* **Android modernization and compatibility fixes:** improved rendering, game/menu transitions, screen layout handling, keyboard/menu behavior, and other Android-specific issues.
* **UI and replay fixes:** fixed various display and state-transition issues, including replay timing on the first track and other rendering inconsistencies.

These changes are maintained as modifications on top of the original Android port rather than as a replacement for the work of its original authors.

# Downloads

## Original Android port

The links in this subsection refer to the **original Android port project**, not to the dpsmpx fork.

* [Google Play](https://play.google.com/store/apps/details?id=org.happysanta.gd). This link and the accompanying historical description are retained from the original port's README.
* The original project's website: [gdtr.net](http://gdtr.net)

## dpsmpx fork

Releases of this fork are published here:

* [GitHub Releases](https://github.com/dpsmpx/gravitydefied/releases)

Source code for the fork is available in this repository.

# Licensing, attribution and third-party rights

## Original Android port

The source project is licensed under the **GNU General Public License v2.0 (GPL-2.0)**. The original port authors are:

* **[Gregory Klushnikov](https://vk.com/grishka)** — idea and the original J2ME-to-Android port.
* **[Evgeny Zinoviev](https://vk.com/ez)** — porting, levels manager, levels API, graphics, and other development.

This fork keeps their attribution and the original project's LICENSE.txt.

## Gravity Defied IP and original game content

The GPL license covers the GPL-licensed software; it does **not** by itself grant ownership of or permission to use third-party trademarks, logos, characters, artwork, level data, or other intellectual property belonging to the original game or other rights holders.

This fork currently retains original Gravity Defied material inherited from the upstream Android port. The repository does **not** claim ownership of that third-party material.

The original port's README states:

* Gravity Defied's original name, logotype, brand, and related rights belong to **Codebrew Software**.
* The Android port authors are not affiliated with Codebrew Software.

Accordingly, the presence of the original game's name, branding, graphics, or level data in this repository should not be interpreted as a statement that **dpsmpx owns those materials or has received a separate trademark/copyright license for them**.

For distribution outside the GPL-covered source-code rights, third-party intellectual-property rights must be considered separately.

## GPL notice for this fork

This repository contains modified versions of GPL-covered source files. The GPLv2 requires preserved license/copyright notices and, for modified files distributed under the license, appropriate notices identifying the modifications and their dates. See [LICENSE.txt](LICENSE.txt) for the full license text.

GPL-covered source code from this fork is intended to remain available under the terms of GPL-2.0.

# Authors

### Fork maintainer

* **dpsmpx** — independent maintainer of this fork and author of the changes documented in **Changes in this fork**.

### Original Android port authors

* **[Gregory Klushnikov](https://vk.com/grishka)** — idea and the original J2ME to Android port.
* **[Evgeny Zinoviev](https://vk.com/ez)** — porting/levels manager/levels API/graphics/everything else development, graphics.

### Codebrew GDTR Authors

* **Tors Björn Henrik Johansson** — system/game logic/interface, testing, levels design
* **Set Elis Norman** — graphics/physics/mathematics/system/tools programming, levels design
* **Per David Jacobsson** — physics programming, game graphics, levels design

For more information about the original game project, please visit [Codebrew Software](http://codebrew.se).

---

**Project status:** this repository is an unofficial, independently maintained fork of the original Android port. The distinction between the GPL-covered port code and the original game's third-party intellectual property is intentional and should be preserved in redistribution and documentation.