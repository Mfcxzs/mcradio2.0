# MC Radio (Forge 1.21.1, Java 21)

## Blocks and items
- **Signal Distributor** (Раздатчик связи): the emitter; GUI with frequency, Playlist / Live mic mode.
- **Computer**: remote terminal. Right-click, pick a distributor within 64 blocks, press Connect. Afterwards the computer
  shows exactly the distributor's interface and controls it.
- **Signal Transmitter**: antenna; touching a distributor/repeater (chains up to 8) adds +32 blocks of radius each.
- **Signal Repeater**: catches an emitter on its input frequency and re-emits on its output frequency
  (input = output extends range on the same channel). Base radius 96.
- **Radio Receiver**: tuned frequency + volume, plays the stream in the world.
- **Signal Jammer**: while powered by redstone, blocks receivers and walkie-talkies within 64 blocks.
- **Walkie-Talkie**: right-click = set frequency; hold V (rebindable) with the walkie in a hand to talk.
  Direct range 300 blocks. Farther: the speaker must be within 300 blocks of a repeater whose input frequency is
  the walkie frequency; listeners must be inside the repeater's radius. Repeater chains work.
- **Radio Scanner**: held in hand, overhears every conversation relayed by a repeater within 32 blocks (any frequency).
- **Radio Tablet**: map of loaded terrain with all distributors, repeaters, receivers, jammers and coverage
  (colour = frequency, white ring = walkie range). Drag to pan, wheel to zoom, frequency filter.

## Build
JDK 21, Gradle 8.8+ (`gradle wrapper`, then `./gradlew build`) or use the GitHub Actions workflow in `.github`.
The MP3 decoder (JLayer) is bundled with JarJar. Put `.mp3` files into `config/mc_radio/tracks/` on the machine
hosting the world.

## Layout
`network/` packets + channel, `radio/` server graph and voice routing, `blockentity/`, `block/`, `item/`, `menu/`,
`registry/`, `client/` (decoder, sound, mic, screens). `tools/generate_assets.py` regenerates textures, models,
lang, recipes, loot tables. `tools/merge_single_file.py` merges all classes into `single_file/McRadio.java`.
