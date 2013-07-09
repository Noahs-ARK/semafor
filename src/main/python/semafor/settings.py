"""
Settings and configurations for the project
"""
import os

BASE_DIR = "/Users/sam/code/semafor/semafor" # CHANGEME
FRAMENET_DATA_DIR = os.path.join(BASE_DIR, "training/data/framenet15")
FRAMES_FILENAME = os.path.join(FRAMENET_DATA_DIR, "framesSingleFile.xml")
FRAME_RELATIONS_FILENAME = os.path.join(FRAMENET_DATA_DIR, "frRelationModified.xml")
