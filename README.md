# Media Organizer

Desktopová aplikace pro automatické třídění fotek a videí do složek podle data pořízení.

## Co aplikace dělá

**Media Organizer** projde vybranou složku, přečte metadata každého souboru a automaticky ho přesune do podsložky ve formátu `rok-měsíc` (např. `2024-07`).

- **Fotky** — datum se čte z EXIF metadat (tag `DateOriginal`)
- **Videa** — datum se čte z MP4 metadat (tag `Creation Time`); podporované formáty: mp4, avi, mkv, mov, wmv, flv, webm
- Soubory bez čitelného datumu zůstanou v původní složce a aplikace zobrazí jejich počet
- Kolize názvů souborů se řeší automatickým přidáním číselné přípony, např. `IMG_001(1).jpg`

### Funkce

| Tlačítko | Popis |
|---|---|
| **Vybrat** | Otevře dialog pro výběr složky s médii |
| **Organizovat média** | Roztřídí fotky a videa do podsložek `rok-měsíc` |
| **Přemístit do jedné složky** | Přesune všechny soubory z podsložek do kořenové složky a odstraní prázdné podsložky |

Obě operace běží na pozadí — GUI se při zpracování nezasekne ani u velkých kolekcí.

## Požadavky

- **Java 17** nebo novější
- **Maven** (pro sestavení projektu)
- Závislost **metadata-extractor** se stáhne automaticky přes Maven

## Sestavení a spuštění

```bash
mvn compile
mvn exec:java -Dexec.mainClass=App
```

nebo sestavit JAR:

```bash
mvn package
java -jar target/Foto-organizer-maven-1.0-SNAPSHOT.jar
```

## Struktura projektu

```
src/main/java/
├── App.java            # Vstupní bod aplikace
├── OrganizerGUI.java   # Hlavní okno a UI logika
├── MediaOrganizer.java # Čtení metadat a přesun souborů
├── FileUtils.java      # Pomocné operace se souborovým systémem
└── Action.java         # Výčet akcí tlačítek
```
