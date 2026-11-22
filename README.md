# FNEX CLIENT (Fabric, Minecraft 26.2)

Client-Mod mit eigenem Hauptmenü, Client-GUI (RIGHT SHIFT), HUD-Modulen, Custom Crosshair,
Cosmetics, Profilen und Auto-Speichern. Läuft direkt über den **Modrinth App**-Launcher,
kein eigener Launcher nötig.

## 1. Bauen (einmalig)

**Variante A – lokal** (Java 25 + Gradle 9.4 installiert):

    gradle build

Ergebnis: `build/libs/fnex-client-26.2-1.0.0.jar`

**Variante B – ohne lokale Installation:** Projekt auf GitHub hochladen. Die Action
`.github/workflows/build.yml` baut automatisch und stellt die `.jar` + `.mrpack` unter
*Actions → Artifacts* bereit.

## 2. In Modrinth installieren

**Weg 1 – fertiges Profil (empfohlen)**

    python tools/make_mrpack.py build/libs/fnex-client-26.2-1.0.0.jar

erzeugt `FNEX-CLIENT-26.2-1.0.0.mrpack` (Fabric Loader + Fabric API + FNEX).
Modrinth App → *Add instance → Import from file* (oder Datei hineinziehen) → Play.

**Weg 2 – manuell**
1. Modrinth App → neue Instanz: Minecraft **26.2**, Loader **Fabric**.
2. Instanz öffnen → *Add content* → **Fabric API** installieren.
3. *Add content → Add from file* → `fnex-client-26.2-1.0.0.jar` wählen.
4. Play.

## 3. Bedienung
- **RIGHT SHIFT**: FNEX-Menü öffnen/schließen (Taste unter *Settings → Keybinds* änderbar).
- **C** (halten): Zoom. Pro Modul ist unter dem Zahnrad-Pfeil ein Keybind einstellbar.
- Einstellungen werden automatisch in `.minecraft/config/fnex/` gespeichert
  (`profiles/*.json`), Profile verwaltest du unter *Settings → Profiles*.

## 4. Logo ersetzen
`src/main/resources/assets/fnex/textures/gui/logo.png` (beliebige quadratische Größe, z. B. 512×512)
und `assets/fnex/icon.png` (128×128) austauschen, neu bauen.

## 5. Eigene Modrinth-Seite (optional, für Update-Hinweis)
Projekt-Slug in `util/UpdateChecker.java` (`SLUG`) anpassen. Gibt es auf Modrinth eine neuere
Version, zeigt das Hauptmenü einen Hinweis.

## Nicht enthalten
- **Custom Nametags** (braucht Render-Mixins, die stark versionsabhängig sind)
- Austausch des Mojang-Startbildschirms (der Fabric-Ladebildschirm ist nicht ersetzbar);
  stattdessen gibt es ein FNEX-Intro mit Logo beim ersten Öffnen des Hauptmenüs.
