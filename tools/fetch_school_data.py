#!/usr/bin/env python3
"""
全量院校数据获取脚本。
数据来源：掌上高考静态数据 static-data.gaokao.cn
  - 院校列表  name.json（约 2991 所，含 school_id / name / proid）
  - 院校详情  {school_id}/info.json（省份 / 城市 / 层次 / 类型 / 985·211·双一流 / 云南物理类录取最低分）
输出：app/src/main/assets/schools.json（紧凑格式，全量院校）
"""
import json
import os
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = "https://static-data.gaokao.cn/www/2.0/school"
NAME_URL = f"{BASE}/name.json"
INFO_URL = f"{BASE}/{{sid}}/info.json"

UA = {"User-Agent": "Mozilla/5.0"}
WORKERS = 20
YUNNAN_ID = "53"
# 云南：新高考物理类 = 2073，旧高考理科 = 2（历史类 2074 / 文科 1 不计入）
PHYSICS_TYPES = {"2073", "2"}
# C9 联盟（教育部/公开联盟名单）
C9_SCHOOLS = {
    "清华大学", "北京大学", "复旦大学", "上海交通大学", "浙江大学",
    "中国科学技术大学", "南京大学", "西安交通大学", "哈尔滨工业大学",
}
# 国防七子（工信部直属）
DEFENSE_SEVEN_SCHOOLS = {
    "北京航空航天大学", "北京理工大学", "哈尔滨工业大学", "西北工业大学",
    "南京航空航天大学", "南京理工大学", "哈尔滨工程大学",
}
# proid -> 省份名（详情拉取失败时的兜底）
PROVINCE_BY_ID = {
    "11": "北京", "12": "天津", "13": "河北", "14": "山西", "15": "内蒙古",
    "21": "辽宁", "22": "吉林", "23": "黑龙江", "31": "上海", "32": "江苏",
    "33": "浙江", "34": "安徽", "35": "福建", "36": "江西", "37": "山东",
    "41": "河南", "42": "湖北", "43": "湖南", "44": "广东", "45": "广西",
    "46": "海南", "50": "重庆", "51": "四川", "52": "贵州", "53": "云南",
    "54": "西藏", "61": "陕西", "62": "甘肃", "63": "青海", "64": "宁夏",
    "65": "新疆", "81": "香港", "82": "澳门",
}


def fetch(url, retries=3):
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=20) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception as exc:
            if attempt < retries - 1:
                time.sleep(0.5)
            else:
                return None


def load_name_list():
    print("正在获取院校列表 name.json ...")
    data = fetch(NAME_URL)
    if not data or "data" not in data:
        raise SystemExit("无法获取院校列表，请检查网络（脚本需要携带 User-Agent）。")
    print(f"  列表共 {len(data['data'])} 所")
    return data["data"]


def load_detail(school_id):
    data = fetch(INFO_URL.format(sid=school_id))
    return data.get("data") if data and "data" in data else None


def yunnan_physics_scores(info):
    """从 pro_type_min 里取云南物理类（2073）/ 理科（2）的逐年最低分。"""
    out = {}
    rows = (info.get("pro_type_min") or {}).get(YUNNAN_ID) or []
    for row in rows:
        year = row.get("year")
        for type_code, value in (row.get("type") or {}).items():
            if type_code in PHYSICS_TYPES:
                try:
                    out[int(year)] = int(float(value))
                except (TypeError, ValueError):
                    pass
    return dict(sorted(out.items()))


def build_school(raw, info):
    school_id = str(raw.get("school_id", ""))
    name = raw.get("name", "")
    province = (info or {}).get("province_name") or PROVINCE_BY_ID.get(str(raw.get("proid", "")), "")
    city = (info or {}).get("city_name") or ""
    level = (info or {}).get("level_name") or ""
    type_name = (info or {}).get("type_name") or ""
    nature = (info or {}).get("school_nature_name") or ""

    tags = []
    if info:
        if str(info.get("f985")) == "1":
            tags.append("985")
        if str(info.get("f211")) == "1":
            tags.append("211")
        if info.get("dual_class_name"):
            tags.append("双一流")
    if name in C9_SCHOOLS:
        tags.append("C9")
    if name in DEFENSE_SEVEN_SCHOOLS:
        tags.append("国防七子")

    scores = yunnan_physics_scores(info or {})
    # 近三年：只取最新三年算范围（数据源通常只有 2023 起，此前无更早年份）
    recent = dict(sorted(scores.items())[-3:])
    values = list(recent.values())
    if values:
        min_3y, max_3y = min(values), max(values)
        # 目标分数取最新一年（2026 优先）
        target_score = scores[max(scores)]
    else:
        min_3y = max_3y = target_score = 0

    return {
        "id": school_id,
        "name": name,
        "province": province,
        "city": city,
        "level": level,
        "type": type_name,
        "nature": nature,
        "tags": tags,
        "targetScore": target_score,
        "minScore3Year": min_3y,
        "maxScore3Year": max_3y,
        "scoresByYear": scores,
        "logoUrl": f"https://static-data.gaokao.cn/upload/logo/{school_id}.jpg",
    }


def school_priority(school):
    """分组优先级：C9 > 国防七子 > 985 > 211 > 双一流 > 本科 > 专科。"""
    name = school["name"]
    tags = school["tags"]
    if name in C9_SCHOOLS:
        return 0
    if name in DEFENSE_SEVEN_SCHOOLS:
        return 1
    if "985" in tags:
        return 2
    if "211" in tags:
        return 3
    if "双一流" in tags:
        return 4
    if school["level"].startswith("专科"):
        return 6
    return 5


def sort_key(school):
    """同优先级按目标分数降序，再按校名。"""
    return (school_priority(school), -school.get("targetScore", 0), school["name"])


def main():
    raw_list = load_name_list()
    total = len(raw_list)
    print(f"正在逐校获取详情（{total} 所，{WORKERS} 并发）...")

    details = []
    with ThreadPoolExecutor(max_workers=WORKERS) as executor:
        for index, detail in enumerate(executor.map(load_detail, [r.get("school_id") for r in raw_list]), 1):
            details.append(detail)
            if index % 300 == 0 or index == total:
                print(f"  {index}/{total}")

    schools = [build_school(raw, info) for raw, info in zip(raw_list, details)]
    schools.sort(key=sort_key)

    data = {
        "version": "2026.10",
        "province": "云南",
        "subjectType": "物理类",
        "totalScore": 750,
        "totalCount": len(schools),
        "schools": schools,
    }

    out_dir = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "schools.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
    size_kb = os.path.getsize(out_path) / 1024
    print(f"已生成 {out_path}，共 {len(schools)} 所院校，{size_kb:.0f} KB")


if __name__ == "__main__":
    main()
