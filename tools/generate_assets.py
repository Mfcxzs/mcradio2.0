#!/usr/bin/env python3
"""Generates textures, models, blockstates, lang files, recipes and loot tables for MC Radio (stdlib only)."""
import json
import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "mcradio")
DATA = os.path.join(ROOT, "data")
MOD = "mcradio"


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def write_json(path, obj):
    write(path, json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


# ---------------------------------------------------------------- textures

class Canvas:
    def __init__(self, base, size=16):
        self.size = size
        self.px = [[base for _ in range(size)] for _ in range(size)]

    def set(self, x, y, c):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[y][x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def save(self, name, folder="block"):
        raw = b"".join(
            b"\x00" + b"".join(bytes(self.px[y][x]) for x in range(self.size)) for y in range(self.size))

        def chunk(tag, data):
            body = tag + data
            return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

        png = (b"\x89PNG\r\n\x1a\n"
               + chunk(b"IHDR", struct.pack(">IIBBBBB", self.size, self.size, 8, 6, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(raw, 9))
               + chunk(b"IEND", b""))
        path = os.path.join(ASSETS, "textures", folder, name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(png)


def shade(c, d):
    return (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + d)), 255)


def noisy(base, seed, amount):
    rnd = random.Random(seed)
    cv = Canvas(base + (255,))
    for y in range(16):
        for x in range(16):
            cv.set(x, y, shade(base, rnd.randint(-amount, amount)))
    return cv


def border(cv, edge, rivet):
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            cv.set(x, y, edge)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        cv.set(x, y, rivet)


def tex_metal_dark():
    cv = noisy((40, 47, 58), 1, 5)
    border(cv, (26, 31, 39, 255), (88, 100, 118, 255))
    cv.save("metal_dark")


def tex_metal_light():
    cv = noisy((132, 142, 154), 2, 6)
    border(cv, (96, 106, 118, 255), (196, 206, 218, 255))
    cv.save("metal_light")


def tex_accent():
    cv = noisy((46, 196, 182), 3, 4)
    for y in range(0, 16, 4):
        cv.rect(0, y, 15, y, (30, 150, 140, 255))
    cv.save("accent")


def tex_antenna():
    cv = Canvas((170, 176, 184, 255))
    for x in range(16):
        d = -50 if x in (0, 15) else (-25 if x in (1, 14) else (35 if x in (5, 6) else 0))
        for y in range(16):
            band = -30 if y % 4 == 0 else 0
            cv.set(x, y, shade((170, 176, 184), d + band))
    cv.save("antenna")


def tex_dish():
    cv = Canvas((200, 205, 212, 255))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 1.8:
                cv.set(x, y, (60, 66, 78, 255))
            else:
                cv.set(x, y, (205, 210, 216, 255) if int(d / 2) % 2 == 0 else (165, 171, 180, 255))
    cv.save("dish")


def tex_station_front(on):
    cv = noisy((34, 40, 50), 4, 3)
    border(cv, (22, 26, 34, 255), (70, 80, 96, 255))
    cv.rect(2, 5, 13, 8, (8, 12, 16, 255))
    heights = [1, 3, 2, 4, 3, 2, 4, 1, 3, 2]
    for i, h in enumerate(heights):
        color = (60, 240, 220, 255) if on else (20, 70, 66, 255)
        for k in range(h):
            cv.set(3 + i, 8 - k, color)
    cv.set(12, 10, (255, 70, 70, 255) if on else (70, 24, 24, 255))
    cv.set(13, 10, (80, 255, 110, 255) if on else (24, 70, 30, 255))
    cv.rect(3, 10, 8, 10, (18, 22, 28, 255))
    for y in (13,):
        cv.rect(2, y, 13, y, (14, 18, 24, 255))
    cv.rect(2, 14, 13, 14, (52, 60, 74, 255))
    cv.save("station_front_on" if on else "station_front")


def tex_receiver_front(on):
    cv = noisy((36, 42, 52), 5, 3)
    border(cv, (22, 26, 34, 255), (70, 80, 96, 255))
    for y in range(9, 15):
        for x in range(3, 8):
            cv.set(x, y, (12, 15, 20, 255) if (x + y) % 2 == 0 else (28, 33, 42, 255))
    cv.rect(9, 9, 12, 11, (255, 190, 70, 255) if on else (10, 14, 18, 255))
    if on:
        cv.rect(10, 9, 10, 11, (255, 90, 40, 255))
    cv.rect(9, 13, 10, 14, (150, 158, 170, 255))
    cv.set(12, 14, (80, 255, 110, 255) if on else (24, 70, 30, 255))
    cv.save("receiver_front_on" if on else "receiver_front")


def tex_repeater_front(on):
    cv = noisy((36, 42, 52), 6, 3)
    border(cv, (22, 26, 34, 255), (70, 80, 96, 255))
    cv.rect(3, 7, 12, 9, (10, 14, 18, 255))
    leds = [((80, 255, 110, 255), (24, 70, 30, 255)), ((255, 190, 70, 255), (80, 60, 24, 255)),
            ((60, 240, 220, 255), (20, 70, 66, 255))]
    for i, (lit, dim) in enumerate(leds):
        cv.rect(4 + i * 3, 8, 5 + i * 3, 8, lit if on else dim)
    for y in (11, 13):
        cv.rect(3, y, 12, y, (14, 18, 24, 255))
    cv.save("repeater_front_on" if on else "repeater_front")


# ---------------------------------------------------------------- models

FACES = ("north", "south", "east", "west", "up", "down")


def box(frm, to, tex, faces=None, rotation=None):
    face_map = {d: {"texture": tex} for d in FACES}
    for d, v in (faces or {}).items():
        face_map[d] = v
    element = {"from": frm, "to": to, "faces": face_map}
    if rotation:
        element["rotation"] = rotation
    return element


def model(textures, elements):
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def station_model():
    t = {"particle": "#dark", "dark": f"{MOD}:block/metal_dark", "light": f"{MOD}:block/metal_light",
         "accent": f"{MOD}:block/accent", "antenna": f"{MOD}:block/antenna", "front": f"{MOD}:block/station_front"}
    e = [
        box([0, 0, 0], [16, 2, 16], "#dark"),
        box([1, 2, 1], [15, 12, 15], "#dark", {"north": {"texture": "#front", "uv": [1, 4, 15, 14]}}),
        box([0, 12, 0], [16, 13, 16], "#light"),
        box([6, 13, 6], [10, 14, 10], "#accent"),
        box([7.5, 14, 7.5], [8.5, 24, 8.5], "#antenna"),
        box([7, 24, 7], [9, 25, 9], "#accent"),
        box([3, 4, 0], [5, 6, 1], "#light"),
        box([11, 4, 0], [13, 6, 1], "#light"),
    ]
    return model(t, e)


def receiver_model():
    t = {"particle": "#dark", "dark": f"{MOD}:block/metal_dark", "light": f"{MOD}:block/metal_light",
         "accent": f"{MOD}:block/accent", "antenna": f"{MOD}:block/antenna", "front": f"{MOD}:block/receiver_front"}
    e = [
        box([2, 0, 3], [14, 8, 13], "#dark", {"north": {"texture": "#front", "uv": [2, 8, 14, 16]}}),
        box([2, 8, 3], [14, 9, 13], "#light"),
        box([4, 9, 7], [5, 11, 9], "#light"),
        box([11, 9, 7], [12, 11, 9], "#light"),
        box([4, 11, 7], [12, 12, 9], "#light"),
        box([12.5, 9, 11], [13.5, 18, 12], "#antenna"),
        box([12, 18, 10.5], [14, 19, 12.5], "#accent"),
    ]
    return model(t, e)


def repeater_model():
    t = {"particle": "#dark", "dark": f"{MOD}:block/metal_dark", "light": f"{MOD}:block/metal_light",
         "accent": f"{MOD}:block/accent", "antenna": f"{MOD}:block/antenna", "dish": f"{MOD}:block/dish",
         "front": f"{MOD}:block/repeater_front"}
    tilt = {"origin": [8, 14, 8], "axis": "x", "angle": -22.5}
    dish_faces = {"up": {"texture": "#dish"}, "down": {"texture": "#light"}}
    e = [
        box([2, 0, 2], [14, 10, 14], "#dark", {"north": {"texture": "#front", "uv": [2, 6, 14, 16]}}),
        box([2, 10, 2], [14, 11, 14], "#light"),
        box([7, 11, 7], [9, 14, 9], "#antenna"),
        box([3, 14, 3], [13, 15, 13], "#light", dish_faces, tilt),
        box([7.5, 15, 7.5], [8.5, 19, 8.5], "#antenna", None, tilt),
        box([7, 19, 7], [9, 20, 9], "#accent", None, tilt),
    ]
    return model(t, e)


def transmitter_model():
    t = {"particle": "#dark", "dark": f"{MOD}:block/metal_dark", "light": f"{MOD}:block/metal_light",
         "accent": f"{MOD}:block/accent", "antenna": f"{MOD}:block/antenna", "dish": f"{MOD}:block/dish"}
    e = [box([4, 0, 4], [12, 1, 12], "#dark")]
    for (x, z) in ((5, 5), (10, 5), (5, 10), (10, 10)):
        e.append(box([x, 1, z], [x + 1, 15, z + 1], "#antenna"))
    for y in (3, 7, 11):
        e.append(box([6, y, 5], [10, y + 1, 6], "#dark"))
        e.append(box([6, y, 10], [10, y + 1, 11], "#dark"))
        e.append(box([5, y, 6], [6, y + 1, 10], "#dark"))
        e.append(box([10, y, 6], [11, y + 1, 10], "#dark"))
    e.append(box([4, 15, 4], [12, 16, 12], "#light"))
    e.append(box([7, 12, 3], [9, 13, 5], "#accent"))
    e.append(box([5, 8, 1], [11, 9, 3], "#dish", {"up": {"texture": "#dish"}, "down": {"texture": "#dish"}},
                 {"origin": [8, 8.5, 3], "axis": "x", "angle": -22.5}))
    e.append(box([5, 5, 13], [11, 6, 15], "#dish", {"up": {"texture": "#dish"}, "down": {"texture": "#dish"}},
                 {"origin": [8, 5.5, 13], "axis": "x", "angle": 22.5}))
    return model(t, e)


def blockstate(name, with_active=True):
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for active in ((False, True) if with_active else (None,)):
            key = f"facing={facing}" + (f",active={'true' if active else 'false'}" if with_active else "")
            ref = f"{MOD}:block/{name}" + ("_on" if active else "")
            entry = {"model": ref}
            if y:
                entry["y"] = y
            variants[key] = entry
    return {"variants": variants}


def main():
    tex_metal_dark(); tex_metal_light(); tex_accent(); tex_antenna(); tex_dish()
    for on in (False, True):
        tex_station_front(on); tex_receiver_front(on); tex_repeater_front(on)

    blocks = {
        "signal_station": (station_model(), "station_front"),
        "signal_receiver": (receiver_model(), "receiver_front"),
        "signal_repeater": (repeater_model(), "repeater_front"),
    }
    for name, (mdl, front) in blocks.items():
        write_json(os.path.join(ASSETS, "models", "block", name + ".json"), mdl)
        write_json(os.path.join(ASSETS, "models", "block", name + "_on.json"),
                   {"parent": f"{MOD}:block/{name}", "textures": {"front": f"{MOD}:block/{front}_on"}})
        write_json(os.path.join(ASSETS, "blockstates", name + ".json"), blockstate(name))
    write_json(os.path.join(ASSETS, "models", "block", "signal_transmitter.json"), transmitter_model())
    write_json(os.path.join(ASSETS, "blockstates", "signal_transmitter.json"),
               {"variants": {"": {"model": f"{MOD}:block/signal_transmitter"}}})
    for name in ("signal_station", "signal_receiver", "signal_repeater", "signal_transmitter"):
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": f"{MOD}:block/{name}"})

    # sounds.json: placeholder vanilla file; the audio data is supplied at runtime by RadioSoundInstance#getStream.
    write_json(os.path.join(ASSETS, "sounds.json"), {
        "radio_stream": {"sounds": [{"name": "minecraft:random/click", "stream": True, "attenuation_distance": 32}]}})

    en = {
        "itemGroup.mcradio": "MC Radio",
        "block.mcradio.signal_station": "Signal Broadcast Station",
        "block.mcradio.signal_transmitter": "Signal Transmitter",
        "block.mcradio.signal_repeater": "Signal Repeater",
        "block.mcradio.signal_receiver": "Radio Receiver & Player",
        "container.mcradio.station": "Signal Broadcast Station",
        "container.mcradio.repeater": "Signal Repeater",
        "container.mcradio.receiver": "Radio Receiver & Player",
        "gui.mcradio.tracks": "Tracks",
        "gui.mcradio.no_tracks": "No .mp3 files found",
        "gui.mcradio.frequency": "Frequency:",
        "gui.mcradio.input_frequency": "Input frequency:",
        "gui.mcradio.output_frequency": "Output frequency:",
        "gui.mcradio.apply": "Apply",
        "gui.mcradio.tune": "Tune",
        "gui.mcradio.mode": "Mode: %s",
        "gui.mcradio.mode.playlist": "Playlist",
        "gui.mcradio.mode.live": "Live Mic / Manual",
        "gui.mcradio.broadcast": "Broadcast",
        "gui.mcradio.stop": "Stop",
        "gui.mcradio.refresh": "Refresh",
        "gui.mcradio.range": "Range: %s blocks",
        "gui.mcradio.status.idle": "Idle",
        "gui.mcradio.status.onair": "On air: %s",
        "gui.mcradio.status.live": "Live carrier on %s",
        "gui.mcradio.hold_to_talk": "Hold to talk",
        "gui.mcradio.talking": "TRANSMITTING...",
        "gui.mcradio.mic_unavailable": "Microphone unavailable",
        "gui.mcradio.live_hint": "Or type a message to all listeners:",
        "gui.mcradio.text_hint": "Type a message...",
        "gui.mcradio.send": "Send",
        "gui.mcradio.folder_hint": "Put .mp3 files in config/mc_radio/tracks",
        "gui.mcradio.volume": "Volume: %s%%",
        "gui.mcradio.signal.locked": "Signal locked",
        "gui.mcradio.signal.none": "No signal",
        "gui.mcradio.receiver_hint": "Tune to a station or repeater frequency.",
        "gui.mcradio.repeater.relaying": "Relaying signal",
        "gui.mcradio.repeater.waiting": "Waiting for input signal",
        "message.mcradio.track_missing": "Track not found in config/mc_radio/tracks",
        "message.mcradio.text_not_live": "Station is not in Live mode",
        "message.mcradio.radio_text": "[Radio %s] %s: %s",
    }
    ru = {
        "itemGroup.mcradio": "MC Radio",
        "block.mcradio.signal_station": "Станция раздачи сигнала",
        "block.mcradio.signal_transmitter": "Транслятор сигнала",
        "block.mcradio.signal_repeater": "Ретранслятор сигнала",
        "block.mcradio.signal_receiver": "Приемник и воспроизводитель сигнала",
        "container.mcradio.station": "Станция раздачи сигнала",
        "container.mcradio.repeater": "Ретранслятор сигнала",
        "container.mcradio.receiver": "Приемник и воспроизводитель сигнала",
        "gui.mcradio.tracks": "Треки",
        "gui.mcradio.no_tracks": "Файлы .mp3 не найдены",
        "gui.mcradio.frequency": "Частота:",
        "gui.mcradio.input_frequency": "Входная частота:",
        "gui.mcradio.output_frequency": "Выходная частота:",
        "gui.mcradio.apply": "Применить",
        "gui.mcradio.tune": "Настроить",
        "gui.mcradio.mode": "Режим: %s",
        "gui.mcradio.mode.playlist": "Плейлист",
        "gui.mcradio.mode.live": "Микрофон / вручную",
        "gui.mcradio.broadcast": "Вещать",
        "gui.mcradio.stop": "Стоп",
        "gui.mcradio.refresh": "Обновить",
        "gui.mcradio.range": "Радиус: %s бл.",
        "gui.mcradio.status.idle": "Ожидание",
        "gui.mcradio.status.onair": "В эфире: %s",
        "gui.mcradio.status.live": "Несущая в эфире на %s",
        "gui.mcradio.hold_to_talk": "Удерживайте для разговора",
        "gui.mcradio.talking": "ПЕРЕДАЧА...",
        "gui.mcradio.mic_unavailable": "Микрофон недоступен",
        "gui.mcradio.live_hint": "Или напишите сообщение слушателям:",
        "gui.mcradio.text_hint": "Введите сообщение...",
        "gui.mcradio.send": "Отправить",
        "gui.mcradio.folder_hint": "Кладите .mp3 в config/mc_radio/tracks",
        "gui.mcradio.volume": "Громкость: %s%%",
        "gui.mcradio.signal.locked": "Сигнал есть",
        "gui.mcradio.signal.none": "Нет сигнала",
        "gui.mcradio.receiver_hint": "Настройтесь на частоту станции или ретранслятора.",
        "gui.mcradio.repeater.relaying": "Ретранслирует сигнал",
        "gui.mcradio.repeater.waiting": "Ожидание входного сигнала",
        "message.mcradio.track_missing": "Трек не найден в config/mc_radio/tracks",
        "message.mcradio.text_not_live": "Станция не в режиме микрофона",
        "message.mcradio.radio_text": "[Радио %s] %s: %s",
    }
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), en)
    write_json(os.path.join(ASSETS, "lang", "ru_ru.json"), ru)

    names = ["signal_station", "signal_transmitter", "signal_repeater", "signal_receiver"]
    for name in names:
        write_json(os.path.join(DATA, MOD, "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"),
               {"replace": False, "values": [f"{MOD}:{n}" for n in names]})
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "needs_stone_tool.json"),
               {"replace": False, "values": [f"{MOD}:{n}" for n in names]})

    def shaped(name, pattern, key):
        write_json(os.path.join(DATA, MOD, "recipe", name + ".json"), {
            "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()},
            "result": {"id": f"{MOD}:{name}", "count": 1}})

    shaped("signal_station", ["IDI", "RNR", "III"],
           {"I": "minecraft:iron_ingot", "D": "minecraft:diamond", "R": "minecraft:redstone", "N": "minecraft:note_block"})
    shaped("signal_transmitter", ["T", "L", "I"],
           {"T": "minecraft:redstone_torch", "L": "minecraft:lightning_rod", "I": "minecraft:iron_block"})
    shaped("signal_repeater", ["ITI", "RCR", "III"],
           {"I": "minecraft:iron_ingot", "T": "minecraft:redstone_torch", "R": "minecraft:redstone", "C": "minecraft:comparator"})
    shaped("signal_receiver", ["ITI", "NJN", "III"],
           {"I": "minecraft:iron_ingot", "T": "minecraft:redstone_torch", "N": "minecraft:note_block", "J": "minecraft:jukebox"})


def clear_canvas():
    return Canvas((0, 0, 0, 0))


def outline(cv, color):
    """Adds a 1px outline around all non-transparent pixels."""
    snapshot = [row[:] for row in cv.px]
    for y in range(cv.size):
        for x in range(cv.size):
            if snapshot[y][x][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < cv.size and 0 <= ny < cv.size and snapshot[ny][nx][3] != 0:
                        cv.set(x, y, color)
                        break


def tex_item_walkie():
    cv = clear_canvas()
    cv.rect(5, 6, 10, 14, (58, 66, 80, 255))
    cv.rect(6, 7, 9, 9, (24, 28, 36, 255))
    for x in (6, 8):
        cv.rect(x, 7, x, 9, (36, 42, 52, 255))
    cv.rect(6, 11, 9, 12, (70, 220, 120, 255))
    cv.rect(4, 8, 4, 10, (220, 70, 70, 255))
    cv.rect(9, 2, 9, 5, (150, 156, 164, 255))
    cv.set(9, 1, (46, 196, 182, 255))
    cv.rect(10, 13, 10, 13, (30, 34, 42, 255))
    outline(cv, (14, 18, 24, 255))
    cv.save("walkie_talkie", "item")


def tex_item_scanner():
    cv = clear_canvas()
    cv.rect(4, 7, 11, 14, (48, 58, 74, 255))
    cv.rect(5, 8, 10, 10, (30, 200, 190, 255))
    cv.rect(5, 12, 10, 12, (24, 28, 36, 255))
    cv.rect(4, 3, 11, 4, (170, 176, 184, 255))
    cv.rect(5, 5, 10, 5, (140, 146, 154, 255))
    cv.rect(7, 1, 8, 2, (255, 159, 28, 255))
    cv.rect(7, 5, 8, 6, (110, 116, 124, 255))
    outline(cv, (14, 18, 24, 255))
    cv.save("radio_scanner", "item")


def tex_item_tablet():
    cv = clear_canvas()
    cv.rect(2, 1, 13, 14, (40, 46, 58, 255))
    cv.rect(3, 2, 12, 12, (16, 38, 52, 255))
    cv.rect(4, 4, 7, 6, (40, 110, 70, 255))
    cv.rect(8, 8, 11, 10, (40, 110, 70, 255))
    for (x, y) in ((8, 3), (9, 3), (10, 4), (10, 5), (7, 7), (6, 7), (5, 6), (6, 3), (7, 3)):
        cv.set(x, y, (46, 196, 182, 255))
    cv.set(8, 5, (255, 159, 28, 255))
    cv.set(5, 10, (255, 90, 95, 255))
    cv.set(11, 6, (230, 247, 245, 255))
    cv.rect(7, 13, 8, 13, (120, 130, 144, 255))
    outline(cv, (14, 18, 24, 255))
    cv.save("radio_tablet", "item")


def tex_computer_front(on):
    cv = noisy((34, 40, 50), 7, 3)
    cv.rect(0, 0, 15, 0, (22, 26, 34, 255))
    cv.rect(0, 11, 15, 11, (22, 26, 34, 255))
    cv.rect(2, 1, 13, 10, (6, 10, 14, 255))
    if on:
        cv.rect(2, 1, 13, 10, (8, 34, 32, 255))
        for y, w in ((2, 8), (4, 6), (5, 9), (7, 5)):
            cv.rect(3, y, 3 + w - 1, y, (60, 240, 160, 255))
        cv.rect(9, 7, 10, 7, (230, 255, 240, 255))
    else:
        cv.set(3, 2, (30, 44, 52, 255))
        cv.set(4, 2, (30, 44, 52, 255))
    cv.set(12, 12, (80, 255, 110, 255) if on else (24, 70, 30, 255))
    cv.save("computer_front_on" if on else "computer_front")


def tex_computer_keys():
    cv = Canvas((28, 32, 40, 255))
    for y in range(1, 15, 3):
        for x in range(1, 15, 3):
            cv.rect(x, y, x + 1, y + 1, (74, 82, 98, 255))
    cv.save("computer_keys")


def tex_jammer_top(on):
    cv = Canvas((20, 22, 28, 255))
    stripe = (255, 90, 95, 255) if on else (140, 52, 56, 255)
    for y in range(16):
        for x in range(16):
            if (x + y) % 6 < 3:
                cv.set(x, y, stripe)
    if on:
        cv.rect(6, 6, 9, 9, (255, 230, 140, 255))
    cv.save("jammer_top_on" if on else "jammer_top")


def computer_model():
    t = {"particle": "#dark", "dark": f"{MOD}:block/metal_dark", "light": f"{MOD}:block/metal_light",
         "accent": f"{MOD}:block/accent", "front": f"{MOD}:block/computer_front", "keys": f"{MOD}:block/computer_keys"}
    e = [
        box([0, 0, 0], [16, 2, 16], "#dark"),
        box([2, 5, 6], [14, 15, 8], "#dark", {"north": {"texture": "#front", "uv": [2, 1, 14, 11]}}),
        box([4, 5, 8], [12, 13, 12], "#dark"),
        box([6, 2, 7], [10, 5, 9], "#light"),
        box([3, 2, 1], [13, 3, 5], "#dark", {"up": {"texture": "#keys"}}),
        box([13, 2, 11], [15, 7, 15], "#accent"),
    ]
    return model(t, e)


def jammer_model():
    t = {"particle": "#dark", "dark": f"{MOD}:block/metal_dark", "light": f"{MOD}:block/metal_light",
         "antenna": f"{MOD}:block/antenna", "top": f"{MOD}:block/jammer_top"}
    e = [
        box([3, 0, 3], [13, 3, 13], "#dark"),
        box([7, 3, 7], [9, 11, 9], "#antenna"),
        box([5, 11, 5], [11, 15, 11], "#top", {"down": {"texture": "#dark"}}),
        box([3, 7, 7.5], [7, 8, 8.5], "#antenna"),
        box([9, 7, 7.5], [13, 8, 8.5], "#antenna"),
        box([2, 6, 7], [3, 9, 9], "#light"),
        box([13, 6, 7], [14, 9, 9], "#light"),
    ]
    return model(t, e)


def merge_lang(name, updates):
    path = os.path.join(ASSETS, "lang", name)
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data.update(updates)
    write_json(path, data)


def extra():
    tex_item_walkie(); tex_item_scanner(); tex_item_tablet()
    tex_computer_keys()
    for on in (False, True):
        tex_computer_front(on); tex_jammer_top(on)

    write_json(os.path.join(ASSETS, "models", "block", "signal_computer.json"), computer_model())
    write_json(os.path.join(ASSETS, "models", "block", "signal_computer_on.json"),
               {"parent": f"{MOD}:block/signal_computer", "textures": {"front": f"{MOD}:block/computer_front_on"}})
    write_json(os.path.join(ASSETS, "blockstates", "signal_computer.json"), blockstate("signal_computer"))

    write_json(os.path.join(ASSETS, "models", "block", "signal_jammer.json"), jammer_model())
    write_json(os.path.join(ASSETS, "models", "block", "signal_jammer_on.json"),
               {"parent": f"{MOD}:block/signal_jammer", "textures": {"top": f"{MOD}:block/jammer_top_on"}})
    write_json(os.path.join(ASSETS, "blockstates", "signal_jammer.json"), {"variants": {
        "active=false": {"model": f"{MOD}:block/signal_jammer"},
        "active=true": {"model": f"{MOD}:block/signal_jammer_on"}}})

    for name in ("signal_computer", "signal_jammer"):
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": f"{MOD}:block/{name}"})
    for name in ("walkie_talkie", "radio_scanner", "radio_tablet"):
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})

    block_names = ["signal_station", "signal_transmitter", "signal_repeater", "signal_receiver",
                   "signal_computer", "signal_jammer"]
    for name in ("signal_computer", "signal_jammer"):
        write_json(os.path.join(DATA, MOD, "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        write_json(os.path.join(DATA, "minecraft", "tags", "block", *tag.split("/")) + ".json",
                   {"replace": False, "values": [f"{MOD}:{n}" for n in block_names]})

    def shaped(name, pattern, key):
        write_json(os.path.join(DATA, MOD, "recipe", name + ".json"), {
            "type": "minecraft:crafting_shaped", "category": "redstone", "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()},
            "result": {"id": f"{MOD}:{name}", "count": 1}})

    shaped("signal_computer", ["IGI", "RCR", "III"],
           {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone", "C": "minecraft:comparator"})
    shaped("signal_jammer", ["IEI", "RNR", "III"],
           {"I": "minecraft:iron_ingot", "E": "minecraft:ender_pearl", "R": "minecraft:redstone", "N": "minecraft:note_block"})
    shaped("walkie_talkie", ["  C", "IRI", "III"],
           {"C": "minecraft:copper_ingot", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"})
    shaped("radio_scanner", ["S", "W"], {"S": "minecraft:spyglass", "W": f"{MOD}:walkie_talkie"})
    shaped("radio_tablet", ["IGI", "ICI", "III"],
           {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "C": "minecraft:compass"})

    en = {
        "block.mcradio.signal_station": "Signal Distributor",
        "container.mcradio.station": "Signal Distributor",
        "block.mcradio.signal_computer": "Computer",
        "container.mcradio.computer": "Computer",
        "block.mcradio.signal_jammer": "Signal Jammer",
        "item.mcradio.walkie_talkie": "Walkie-Talkie",
        "item.mcradio.walkie_talkie.frequency": "Frequency: %s",
        "item.mcradio.walkie_talkie.range": "Direct range: %s blocks (use repeaters for more)",
        "item.mcradio.radio_scanner": "Radio Scanner",
        "item.mcradio.radio_scanner.tooltip": "Hold to overhear conversations relayed by repeaters within %s blocks",
        "item.mcradio.radio_tablet": "Radio Tablet",
        "key.mcradio.push_to_talk": "Walkie-talkie push-to-talk",
        "key.categories.mcradio": "MC Radio",
        "gui.mcradio.connect": "Connect",
        "gui.mcradio.disconnect": "Disconnect",
        "gui.mcradio.computer.not_connected": "Not connected. Select a distributor:",
        "gui.mcradio.computer.hint": "Distributors within 64 blocks are listed.",
        "gui.mcradio.repeater_hint": "Same input and output = extends range on one channel.",
        "gui.mcradio.walkie.hint": "Hold %s to talk (walkie in hand).",
        "gui.mcradio.walkie.range": "Direct range: %s blocks.",
        "gui.mcradio.walkie.repeater": "Farther: stay within reach of a repeater.",
        "gui.mcradio.walkie.warning": "Relayed talk can be overheard with a Scanner!",
        "gui.mcradio.map.filter": "Frequency filter",
        "gui.mcradio.map.center": "Center on me",
        "gui.mcradio.map.scale": "Map width: %s blocks",
        "gui.mcradio.map.legend.distributor": "Distributor",
        "gui.mcradio.map.legend.repeater": "Repeater",
        "gui.mcradio.map.legend.receiver": "Receiver",
        "gui.mcradio.map.legend.jammer": "Jammer",
        "gui.mcradio.map.legend.you": "You",
        "gui.mcradio.map.legend.walkie": "White ring: walkie %s",
        "gui.mcradio.map.hint": "Drag: pan. Wheel: zoom.",
        "gui.mcradio.map.distributor": "Distributor: %s kHz, range %s",
        "gui.mcradio.map.repeater": "Repeater: in %s, out %s, range %s",
        "gui.mcradio.map.receiver": "Receiver: %s kHz",
        "gui.mcradio.map.jammer": "Jammer: radius %s",
        "gui.mcradio.map.active": "Active",
        "gui.mcradio.map.inactive": "Inactive",
        "message.mcradio.transmitting": "Transmitting on %s",
        "message.mcradio.voice.0": "Radio %2$s: %1$s is speaking",
        "message.mcradio.voice.1": "Radio %2$s (via repeater): %1$s is speaking",
        "message.mcradio.voice.2": "INTERCEPTED on %2$s: %1$s is speaking",
    }
    ru = {
        "block.mcradio.signal_station": "Раздатчик связи",
        "container.mcradio.station": "Раздатчик связи",
        "block.mcradio.signal_computer": "Компьютер",
        "container.mcradio.computer": "Компьютер",
        "block.mcradio.signal_jammer": "Глушилка сигнала",
        "item.mcradio.walkie_talkie": "Рация",
        "item.mcradio.walkie_talkie.frequency": "Частота: %s",
        "item.mcradio.walkie_talkie.range": "Прямая связь: %s блоков (дальше через ретранслятор)",
        "item.mcradio.radio_scanner": "Радиосканер",
        "item.mcradio.radio_scanner.tooltip": "В руке: подслушивает разговоры, идущие через ретрансляторы в радиусе %s блоков",
        "item.mcradio.radio_tablet": "Радиопланшет",
        "key.mcradio.push_to_talk": "Рация: говорить",
        "key.categories.mcradio": "MC Radio",
        "gui.mcradio.connect": "Подключить",
        "gui.mcradio.disconnect": "Отключить",
        "gui.mcradio.computer.not_connected": "Не подключён. Выберите раздатчик:",
        "gui.mcradio.computer.hint": "Показаны раздатчики в радиусе 64 блоков.",
        "gui.mcradio.repeater_hint": "Вход = выход: усиливает сигнал на одном канале.",
        "gui.mcradio.walkie.hint": "Держите %s, чтобы говорить (рация в руке).",
        "gui.mcradio.walkie.range": "Прямая связь: %s блоков.",
        "gui.mcradio.walkie.repeater": "Дальше: будьте в зоне ретранслятора.",
        "gui.mcradio.walkie.warning": "Через ретранслятор могут подслушать сканером!",
        "gui.mcradio.map.filter": "Фильтр частоты",
        "gui.mcradio.map.center": "Ко мне",
        "gui.mcradio.map.scale": "Ширина карты: %s бл.",
        "gui.mcradio.map.legend.distributor": "Раздатчик",
        "gui.mcradio.map.legend.repeater": "Ретранслятор",
        "gui.mcradio.map.legend.receiver": "Приёмник",
        "gui.mcradio.map.legend.jammer": "Глушилка",
        "gui.mcradio.map.legend.you": "Вы",
        "gui.mcradio.map.legend.walkie": "Белое кольцо: рация %s",
        "gui.mcradio.map.hint": "Мышь: двигать. Колесо: масштаб.",
        "gui.mcradio.map.distributor": "Раздатчик: %s кГц, радиус %s",
        "gui.mcradio.map.repeater": "Ретранслятор: вход %s, выход %s, радиус %s",
        "gui.mcradio.map.receiver": "Приёмник: %s кГц",
        "gui.mcradio.map.jammer": "Глушилка: радиус %s",
        "gui.mcradio.map.active": "Активен",
        "gui.mcradio.map.inactive": "Неактивен",
        "message.mcradio.transmitting": "Передача на частоте %s",
        "message.mcradio.voice.0": "Радио %2$s: говорит %1$s",
        "message.mcradio.voice.1": "Радио %2$s (через ретранслятор): говорит %1$s",
        "message.mcradio.voice.2": "ПЕРЕХВАТ на %2$s: говорит %1$s",
    }
    merge_lang("en_us.json", en)
    merge_lang("ru_ru.json", ru)


if __name__ == "__main__":
    main()
    extra()
