#!/usr/bin/env python3
"""
从教育部《普通高等学校本科专业目录（2026年）》PDF 生成三级专业目录。

结构：学科门类(2位) → 专业类(4位) → 专业(6位，含 T/K 后缀)
输出：app/src/main/assets/majors_v2.json

PDF 为文本型（Type0/CIDFontType2 + ToUnicode），本脚本用标准库自解析：
  1. 按 "N 0 obj … endobj" 暴力索引对象
  2. FlateDecode 解压内容流与 ToUnicode CMap
  3. 解析 Tf/Tj/TJ 文本算子，用 ToUnicode 把 CID 映射回 Unicode

用法：python3 tools/parse_major_catalog.py <目录.pdf>
"""
import json
import os
import re
import sys
import zlib

CODE_RE = re.compile(r"^\d{6,7}[TK]{0,2}$")
CLASS_CODE_RE = re.compile(r"^\d{4}$")
GATE_CODE_RE = re.compile(r"^\d{2}$")
SUBCAT_SUFFIX = "类"


def load_objects(data):
    objs = {}
    for m in re.finditer(rb"(?<![0-9])(\d+)\s+0\s+obj", data):
        num = int(m.group(1))
        start = m.end()
        end_m = re.compile(rb"endobj", re.S).search(data, start)
        objs[num] = data[start:end_m.start() if end_m else len(data)]
    return objs


def stream_of(body):
    m = re.search(rb"stream\r?\n", body)
    if not m:
        return None, body
    s = m.end()
    e = body.find(b"endstream", s)
    raw = body[s:e]
    if b"FlateDecode" in body[: m.start()]:
        try:
            raw = zlib.decompress(raw)
        except Exception:
            try:
                raw = zlib.decompress(raw, -15)
            except Exception:
                pass
    return raw, body[: m.start()]


def parse_tounicode(stream):
    mp = {}
    for m in re.finditer(rb"beginbfchar(.*?)endbfchar", stream, re.S):
        for mm in re.finditer(rb"<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>", m.group(1)):
            mp[int(mm.group(1), 16)] = bytes.fromhex(
                mm.group(2).decode()
            ).decode("utf-16-be", "ignore")
    for m in re.finditer(rb"beginbfrange(.*?)endbfrange", stream, re.S):
        for mm in re.finditer(
            rb"<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>\s*<([0-9A-Fa-f]+)>", m.group(1)
        ):
            a, b, c = (int(mm.group(i), 16) for i in (1, 2, 3))
            for i in range(b - a + 1):
                mp[a + i] = chr(c + i)
    return mp


def extract_pages(path):
    data = open(path, "rb").read()
    objs = load_objects(data)

    font_uni = {}
    for num, body in objs.items():
        if b"/ToUnicode" in body:
            ref = re.search(rb"/ToUnicode\s+(\d+)\s+0\s+R", body)
            if ref:
                target = objs.get(int(ref.group(1)))
                if target:
                    st, _ = stream_of(target)
                    if st:
                        font_uni[num] = parse_tounicode(st)

    pages = [
        n for n, b in objs.items() if re.search(rb"/Type\s*/Page\b", b)
    ]

    result = []
    for pnum in sorted(pages):
        body = objs[pnum]
        res = None
        m = re.search(rb"/Resources\s+(\d+)\s+0\s+R", body)
        if m:
            res = objs.get(int(m.group(1)))
        else:
            m = re.search(rb"/Resources\s*<<(.*?)>>", body, re.S)
            if m:
                res = m.group(0)

        fmap = {}
        if res:
            fm = re.search(rb"/Font\s*<<(.*?)>>", res, re.S)
            if not fm:
                fr = re.search(rb"/Font\s+(\d+)\s+0\s+R", res)
                if fr:
                    fb = objs.get(int(fr.group(1)))
                    if fb:
                        fm = re.match(rb"\s*<<(.*)>>", fb, re.S)
            if fm:
                for mm in re.finditer(rb"/([A-Za-z0-9]+)\s+(\d+)\s+0\s+R", fm.group(1)):
                    fmap[mm.group(1).decode()] = int(mm.group(2))

        cids = []
        cm = re.search(rb"/Contents\s+(\d+)\s+0\s+R", body)
        if cm:
            cids = [int(cm.group(1))]
        else:
            cm = re.search(rb"/Contents\s*\[(.*?)\]", body, re.S)
            if cm:
                cids = [int(x) for x in re.findall(rb"(\d+)\s+0\s+R", cm.group(1))]

        content = b""
        for c in cids:
            cb = objs.get(c)
            if cb:
                st, _ = stream_of(cb)
                if st:
                    content += st + b"\n"

        out = []
        cur = None
        for m in re.finditer(
            rb"/([A-Za-z0-9]+)\s+[\d.]+\s+Tf|<([0-9A-Fa-f\s]+)>\s*Tj|\[(.*?)\]\s*TJ|\((.*?)\)\s*Tj",
            content,
            re.S,
        ):
            if m.group(1):
                cur = font_uni.get(fmap.get(m.group(1).decode(), -1))
            elif m.group(2) is not None:
                out.append(decode_hex(re.sub(rb"\s", b"", m.group(2)), cur))
            elif m.group(3) is not None:
                for hm in re.finditer(rb"<([0-9A-Fa-f\s]+)>", m.group(3)):
                    out.append(decode_hex(re.sub(rb"\s", b"", hm.group(1)), cur))
            elif m.group(4) is not None:
                out.append(m.group(4).decode("latin-1"))
        result.append((pnum, "".join(out)))
    return result


def decode_hex(hexs, cmap):
    if not hexs:
        return ""
    if cmap is None:
        try:
            return bytes.fromhex(hexs.decode()).decode("utf-16-be", "ignore")
        except Exception:
            return ""
    out = []
    for j in range(0, len(hexs), 4):
        chunk = hexs[j : j + 4]
        if not chunk:
            continue
        out.append(cmap.get(int(chunk, 16), ""))
    return "".join(out)


def merge_notes(tokens):
    """把跨 token 拆断的「（注：…）」注释合并进前一个名字 token。

    目录里带注的专业形如「XXX（注：可授工学或理学学士学位）」；若注释恰好被排版切到下一行，
    提取后会变成两个 token，逐 token 清理就会漏掉。
    """
    merged = []
    i = 0
    while i < len(tokens):
        tok = tokens[i]
        if "（" in tok and "）" not in tok:
            while i + 1 < len(tokens) and "）" not in tok:
                i += 1
                tok += tokens[i]
        merged.append(tok)
        i += 1
    return merged


def build_tree(pages):
    categories = []  # list of dicts
    cur_cat = None
    cur_sub = None
    cur_cat_code = None
    pending = []
    names = []

    def flush():
        nonlocal pending, names, cur_sub
        # 交叉学科等门类下没有专业类层级，专业直接挂在门类上——补一个合成专业类承载
        if cur_sub is None and cur_cat is not None and pending and names:
            cur_sub = {
                "code": cur_cat["code"] + "00",
                "name": cur_cat["name"] + "类",
                "majors": [],
            }
            cur_cat["subCategories"].append(cur_sub)
        for code, name in zip(pending, names):
            if cur_sub is not None:
                clean = re.sub(r"（注：.*?）", "", name).strip()
                if clean and "注：" not in clean:
                    cur_sub["majors"].append({"code": code, "name": clean})
        pending = []
        names = []

    for _, text in pages:
        text = re.sub(r"—\s*\d+\s*—", " ", text)
        tokens = merge_notes(text.split())
        i = 0
        while i < len(tokens):
            tok = tokens[i]
            # 学科门类：01 学科门类：哲学
            if tok.startswith("学科门类"):
                flush()
                name = tok.split("：", 1)[1] if "：" in tok else tok
                code = cur_cat_code or ""
                cur_cat = {"code": code, "name": name, "subCategories": []}
                categories.append(cur_cat)
                cur_sub = None
                i += 1
                continue
            if GATE_CODE_RE.match(tok) and i + 1 < len(tokens) and tokens[i + 1].startswith("学科门类"):
                cur_cat_code = tok
                name = tokens[i + 1].split("：", 1)[1]
                flush()
                cur_cat = {"code": tok, "name": name, "subCategories": []}
                categories.append(cur_cat)
                cur_sub = None
                i += 2
                continue
            # 专业类：0101 哲学类
            if (
                CLASS_CODE_RE.match(tok)
                and i + 1 < len(tokens)
                and tokens[i + 1].endswith(SUBCAT_SUFFIX)
            ):
                flush()
                cur_sub = {"code": tok, "name": tokens[i + 1], "majors": []}
                if cur_cat is not None:
                    cur_cat["subCategories"].append(cur_sub)
                i += 2
                continue
            if CODE_RE.match(tok):
                if names:
                    flush()
                pending.append(tok)
                i += 1
                continue
            # 名称（可能带注）
            names.append(tok)
            i += 1
        # 页末不 flush（专业跨页），仅在下一个结构标记处 flush
    flush()
    return categories


def main():
    if len(sys.argv) < 2:
        raise SystemExit("用法: parse_major_catalog.py <目录.pdf>")
    pages = extract_pages(sys.argv[1])
    categories = build_tree(pages)
    total = sum(len(s["majors"]) for c in categories for s in c["subCategories"])
    result = {
        "version": "2026",
        "source": "教育部《普通高等学校本科专业目录（2026年）》",
        "categoryCount": len(categories),
        "subCategoryCount": sum(len(c["subCategories"]) for c in categories),
        "majorCount": total,
        "categories": categories,
    }
    out_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
    out_dir = os.path.abspath(out_dir)
    out_path = os.path.join(out_dir, "majors_v2.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, separators=(",", ":"))
    print(
        f"已生成 {out_path}: 门类 {len(categories)} / 专业类 {result['subCategoryCount']} / 专业 {total}"
    )


if __name__ == "__main__":
    main()
