#  Copyright (c) 2024 TeamMoeg
#
#  This file is part of Frosted Heart.
#
#  Frosted Heart is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, version 3.
#
#  Frosted Heart is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
#

import os

python_root = os.getcwd()
project_root = os.path.dirname(python_root)
main_dir = os.path.join(project_root, "src", "main")
resources_dir = os.path.join(main_dir, "resources")
lang_dir = os.path.join(resources_dir, "assets", "frostedheart", "lang")
en_us_file = os.path.join(lang_dir, "en_us.json")
zh_cn_file = os.path.join(lang_dir, "zh_cn.json")

import json
def rearrange_lang():
    # load files as dict
    en_us = {}
    zh_cn = {}
    with open(en_us_file, "r") as f:
        en_us = json.load(f)
    with open(zh_cn_file, "r") as f:
        zh_cn = json.load(f)

    # add missing keys in zh_cn
    for key in en_us:
        if key not in zh_cn:
            zh_cn[key] = en_us[key]

    # add missing keys in en_us
    for key in zh_cn:
        if key not in en_us:
            en_us[key] = zh_cn[key]

    en_us = dict(sorted(en_us.items()))
    zh_cn = dict(sorted(zh_cn.items()))

    # write back to files
    with open(en_us_file, "w") as f:
        json.dump(en_us, f, indent=2, ensure_ascii=False)
    with open(zh_cn_file, "w") as f:
        json.dump(zh_cn, f, indent=2, ensure_ascii=False)

    print("Lang files rearranged.")
    return 0


if __name__ == "__main__":
    rearrange_lang()