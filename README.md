# 🎵 Java MP3 Player - Sonora

Un'applicazione desktop per la riproduzione di file MP3, con supporto a playlist personalizzate, modalità di riproduzione, gestione della coda, multilingua e tema chiaro/scuro.

## 🧰 Funzionalità principali

- Riproduzione di file `.mp3`
- Gestione completa di **playlist personalizzate**
- Modalità di riproduzione: sequenziale, casuale, ripeti
- Seek tramite slider
- Importa/esporta canzoni e playlist
- Coda di riproduzione dinamica
- Supporto multilingua: 🇮🇹 🇬🇧 🇯🇵 🇩🇪 🇫🇷 🇨🇳 🇰🇷
- Tema chiaro / scuro
- Logger interno per debugging
- Serializzazione automatica delle playlist
- Controlli via tastiera e menu contestuali

---

## 🧑‍💻 Tecnologie utilizzate

- **Java SE** (Swing per la GUI)
- **JLayer** per la riproduzione audio (AdvancedPlayer)
- **FileSystem** e **Object Serialization**
- **ResourceBundle** per internazionalizzazione

---
Sonora
├──data			→ File config e dat
├──img			→ Cover importate
├──logs			→ Log di sistema
├──songs		→ Canzoni importate
├──src
   ├──com.dreamteam
      ├── control          → Controller, gestione eventi e logica
      ├── model            → Classi per Song, Playlist, Player
      ├── view             → GUI completa con Swing
      ├── data             → Serializzazione e salvataggio playlist
      ├── lib              → Librerie necessarie al player
      ├── languages        → File di lingua (ResourceBundle)
      ├── resources        → Icone per il player
      ├── streaming        → Streaming server-client di musica

