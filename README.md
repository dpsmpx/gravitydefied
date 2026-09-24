# Gravity Defied – Android port
**Gravity Defied** is an iconic trial racing mobile game. It was originally developed by Codebrew Software in 2004 for J2ME platform.

Codebrew has launched an Android version of the game in 2012, though it was totally remade. We are the ones who like classic version of Gravity Defied more, so we ported it to Android almost unchanged!

This port includes all features of the original Gravity Defied. In addition, we have collected more than 1000 levels mods made by fans since 2007. You can install any mod and switch between mods directly from the game menu.

***We are not associated with Codebrew Software in any fashion. All rights to the original Gravity Defied, it's name, logotype, brand and all that stuff belongs to Codebrew Software.***

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

# Download
You can download the last version from:

* [Google Play](https://play.google.com/store/apps/details?id=org.happysanta.gd). Please note, that this (package name *org.happysanta.gd*) is **the only real** Gravity Defied at Google Play, and it's ads-free. Previously it has package name *com.ch1p.gd*, but unfortunately my developer account was suspended due to USA Crimea-related sanctions and the game with almost 1 million downloads disappeared from Google Play.
* Our official site [gdtr.net](http://gdtr.net)

# Authors
### Port authors

* **[Gregory Klushnikov](https://vk.com/grishka)** - idea and the original J2ME to Android port.
* **[Evgeny Zinoviev](https://vk.com/ez)** - porting/levels manager/levels API/graphics/everything else development, graphics.

### Codebrew GDTR Authors

* **Tors Björn Henrik Johansson** - system/game logic/interface, testing, levels design
* **Set Elis Norman** - graphics/physics/mathematics/system/tools programming, levels design
* **Per David Jacobsson** - physics programming, game graphics, levels design

For more information, please visit official site of Codebrew Software: [codebrew.se](http://codebrew.se)

