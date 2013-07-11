"""
Settings and configurations for the project
"""
from os.path import join

BASE_DIR = "/Users/sam/code/semafor/semafor"  # CHANGEME
TRAINING_DATA_DIR = join(BASE_DIR, "training/data")

FRAMENET_DATA_DIR = join(TRAINING_DATA_DIR, "framenet15")
FRAMES_FILENAME = join(FRAMENET_DATA_DIR, "framesSingleFile.xml")
FRAME_RELATIONS_FILENAME = join(FRAMENET_DATA_DIR, "frRelationModified.xml")

NELL_DATA_DIR = join(TRAINING_DATA_DIR, "nell")
NELL_NOUN_PHRASES_FILENAME = join(NELL_DATA_DIR, "NELL.ClueWeb09.v1.nps.csv")
#NELL_NOUN_PHRASES_FILENAME = join(NELL_DATA_DIR, "NELL.KBP2012.max3.v1.nps.csv")
NELL_HIERARCHY_FILENAME = join(NELL_DATA_DIR, "NELL.08m.734.categories.csv")

SENNA_DATA_DIR = join(BASE_DIR, "src/main/resources/senna")
