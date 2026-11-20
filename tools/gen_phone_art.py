"""スマホの絵（アイテム・アプリのアイコン）と、モデル・レシピ・名前を書き出す。"""
import json, math, os
from PIL import Image, ImageDraw

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A, D = f"{R}/assets/jsn_phone", f"{R}/data/jsn_phone"
def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def wj(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f: json.dump(o, f, ensure_ascii=False, indent=2); f.write("\n")
def save(im, p):
    os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)

# ---------------- アイテム（16×16）：ケース（色をつける）と画面 ----------------
case = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(case)
d.rounded_rectangle([4, 1, 11, 14], radius=2, fill=(235, 235, 235, 255))
d.line([4, 3, 4, 12], fill=(200, 200, 200, 255)); d.line([11, 3, 11, 12], fill=(170, 170, 170, 255))
save(case, f"{A}/textures/item/smartphone_case.png")
scr = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(scr)
d.rectangle([5, 3, 10, 12], fill=hx("#101820"))
for y, c in ((4, "#3a9af0"), (6, "#50c060"), (8, "#f08030"), (10, "#9060e0")):
    d.point([(6, y), (8, y)], fill=hx(c))
d.point([(7, 2), (8, 2)], fill=hx("#202020"))
d.point([(5, 3)], fill=(255, 255, 255, 120))
save(scr, f"{A}/textures/item/smartphone_screen.png")
wj(f"{A}/models/item/smartphone.json", {"parent": "minecraft:item/handheld", "textures": {
    "layer0": "jsn_phone:item/smartphone_case", "layer1": "jsn_phone:item/smartphone_screen"}})

# ---------------- アプリのアイコン（32×32） ----------------
K = 4
def icon(name, bg, draw):
    im = Image.new("RGBA", (32 * K, 32 * K), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    top, bot = hx(bg[0]), hx(bg[1])
    mask = Image.new("L", (32 * K, 32 * K), 0); ImageDraw.Draw(mask).rounded_rectangle([0, 0, 32 * K - 1, 32 * K - 1], radius=8 * K, fill=255)
    grad = Image.new("RGBA", (32 * K, 32 * K))
    for y in range(32 * K):
        t = y / (32 * K)
        grad.paste(tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,), (0, y, 32 * K, y + 1))
    im.paste(grad, (0, 0), mask)
    draw(d, lambda *xs: [v * K for v in xs])
    im = im.resize((32, 32), Image.LANCZOS)
    save(im, f"{A}/textures/gui/app/{name}.png")

W = hx("#ffffff")
def messages(d, S):
    d.rounded_rectangle(S(6, 8, 26, 22), radius=5 * K, fill=W); d.polygon(S(10, 21, 9, 26, 15, 21), fill=W)
def sns(d, S):
    d.ellipse(S(7, 9, 25, 25), fill=W); d.polygon(S(12, 9, 9, 4, 15, 9), fill=W)
    d.ellipse(S(12, 14, 15, 17), fill=hx("#3a9af0")); d.ellipse(S(18, 14, 21, 17), fill=hx("#3a9af0"))
def news(d, S):
    d.rectangle(S(7, 7, 25, 25), fill=W)
    for y in (11, 15, 19): d.rectangle(S(10, y, 22, y + 2), fill=hx("#e04040") if y == 11 else hx("#909090"))
def friends(d, S):
    d.ellipse(S(7, 8, 15, 16), fill=W); d.pieslice(S(4, 16, 18, 30), 180, 360, fill=W)
    d.ellipse(S(17, 8, 25, 16), fill=(255, 255, 255, 200)); d.pieslice(S(14, 16, 28, 30), 180, 360, fill=(255, 255, 255, 200))
def call(d, S):
    d.rounded_rectangle(S(9, 6, 16, 12), radius=2 * K, fill=W); d.rounded_rectangle(S(18, 20, 25, 26), radius=2 * K, fill=W)
    d.line(S(12, 11, 13, 18, 20, 23), fill=W, width=5 * K)
def camera(d, S):
    d.rounded_rectangle(S(5, 10, 27, 25), radius=3 * K, fill=W); d.rectangle(S(11, 7, 19, 10), fill=W)
    d.ellipse(S(11, 12, 21, 22), fill=hx("#303038")); d.ellipse(S(13, 14, 19, 20), fill=hx("#6080c0"))
def photos(d, S):
    for i, c in enumerate(("#f04040", "#f0c040", "#40c060", "#40a0f0")):
        a = math.radians(i * 90 + 45)
        cx, cy = 16 + 5 * math.cos(a), 16 + 5 * math.sin(a)
        d.ellipse(S(cx - 6, cy - 6, cx + 6, cy + 6), fill=hx(c))
def music(d, S):
    d.ellipse(S(7, 19, 14, 26), fill=W); d.ellipse(S(18, 16, 25, 23), fill=W)
    d.rectangle(S(12, 7, 14, 23), fill=W); d.rectangle(S(23, 5, 25, 20), fill=W); d.polygon(S(12, 7, 25, 4, 25, 9, 12, 12), fill=W)
def bank(d, S):
    d.polygon(S(16, 5, 27, 11, 5, 11), fill=W); d.rectangle(S(6, 23, 26, 26), fill=W)
    for x in (8, 14, 20): d.rectangle(S(x, 12, x + 3, 22), fill=W)
def jsncraft(d, S):
    d.polygon(S(16, 5, 26, 11, 26, 22, 16, 28, 6, 22, 6, 11), fill=W)
    d.polygon(S(16, 9, 22, 13, 22, 20, 16, 24, 10, 20, 10, 13), fill=hx("#e6b836"))
def clock(d, S):
    d.ellipse(S(5, 5, 27, 27), fill=W); d.line(S(16, 16, 16, 9), fill=hx("#202020"), width=2 * K); d.line(S(16, 16, 21, 19), fill=hx("#e04040"), width=2 * K)
def weather(d, S):
    d.ellipse(S(5, 6, 17, 18), fill=hx("#ffd040")); d.ellipse(S(10, 14, 20, 24), fill=W); d.ellipse(S(16, 11, 27, 22), fill=W); d.rectangle(S(14, 18, 25, 24), fill=W)
def memo(d, S):
    d.rectangle(S(8, 6, 24, 26), fill=hx("#fff4b0"))
    for y in (11, 15, 19): d.line(S(11, y, 21, y), fill=hx("#a09060"), width=K)
    d.line(S(20, 22, 26, 10), fill=hx("#404040"), width=2 * K)
def settings(d, S):
    for i in range(8):
        a = math.radians(i * 45); d.line(S(16, 16, 16 + 11 * math.cos(a), 16 + 11 * math.sin(a)), fill=W, width=5 * K)
    d.ellipse(S(8, 8, 24, 24), fill=W); d.ellipse(S(12, 12, 20, 20), fill=hx("#707880"))

for name, bg, fn in (("messages", ("#5ad070", "#2a9a40"), messages), ("sns", ("#5ab0f8", "#2070d0"), sns), ("news", ("#f07050", "#c03030"), news),
                     ("friends", ("#60a0f0", "#3050c0"), friends), ("call", ("#60e070", "#20a040"), call), ("camera", ("#9098a8", "#505868"), camera),
                     ("photos", ("#ffffff", "#e8e8f0"), photos), ("music", ("#f070b0", "#c03070"), music), ("bank", ("#f0c040", "#b08010"), bank),
                     ("jsncraft", ("#7050d0", "#402090"), jsncraft), ("clock", ("#303040", "#101018"), clock), ("weather", ("#70c0f8", "#3080d0"), weather),
                     ("memo", ("#f8e070", "#e0b030"), memo), ("settings", ("#a0a8b8", "#606878"), settings)):
    icon(name, bg, fn)

# ---------------- レシピ・名前 ----------------
wj(f"{D}/recipe/smartphone.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["IGI", "IRI", "ICI"],
    "key": {"I": {"item": "minecraft:iron_ingot"}, "G": {"item": "minecraft:glass_pane"}, "R": {"item": "minecraft:redstone"}, "C": {"item": "minecraft:copper_ingot"}},
    "result": {"id": "jsn_phone:smartphone", "count": 1}})
wj(f"{D}/recipe/phone_dye.json", {"type": "jsn_phone:phone_dye", "category": "misc"})
wj(f"{A}/lang/ja_jp.json", {"item.jsn_phone.smartphone": "スマートフォン", "key.categories.jsn_phone": "jsn-Phone", "key.jsn_phone.open": "スマホを開く"})
wj(f"{A}/lang/en_us.json", {"item.jsn_phone.smartphone": "Smartphone", "key.categories.jsn_phone": "jsn-Phone", "key.jsn_phone.open": "Open Phone"})
print("phone art ok")
