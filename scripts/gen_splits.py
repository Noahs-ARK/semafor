#!/usr/bin/env python
"""
Split gold json outputs the same way cv.{train,dev,test}.sentences were split
"""
import codecs
import json
import os

GOLD_DIR = "/Users/sam/repo/project/semafor/semafor/training/data/framenet15/fulltext_json"

SPLITS_INPUT_TEMPLATE = "/Users/sam/repo/project/semafor/semafor/training/data/naacl2012/cv.%s.sentences"
SPLITS_OUTPUT_TEMPLATE = "/Users/sam/repo/project/semafor/semafor/training/data/naacl2012/cv.%s.sentences.json"


def get_gold_json(gold_dir):
    """
    Get a map from raw text to full json output
    """
    gold_json_filenames = [os.path.join(GOLD_DIR, filename) for filename in os.listdir(gold_dir) if filename.endswith(".json")]
    gold_json_lines = []
    for filename in gold_json_filenames:
        with codecs.open(filename, encoding="utf8") as in_file:
            lines = in_file.readlines()
        gold_json_lines += lines
    return dict(zip([' '.join(json.loads(x)['tokens']) for x in gold_json_lines], gold_json_lines))


if "__main__" == __name__:
    txt_to_json = get_gold_json(GOLD_DIR)

    for split_name in ("train", "dev", "test"):
        with codecs.open(SPLITS_INPUT_TEMPLATE % split_name, encoding="utf8") as in_file:
            sentences = [line.strip() for line in in_file.readlines()]
        split = [txt_to_json[line] for line in sentences]
        with codecs.open(SPLITS_OUTPUT_TEMPLATE % split_name, "w", encoding="utf8") as out_file:
            out_file.writelines(split)
