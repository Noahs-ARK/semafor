# SEMAFOR training

This is a README for training on the FrameNet 1.5 full text annotations. 
Dipanjan Das, dipanjan@cs.cmu.edu, 2/18/2012.

Training models for frame-semantic parsing with SEMAFOR is still a very laborious and 
clunky set of steps. Your kind patience is required to train the models :-)

### Checkout the code:

```
git clone git@github.com:Noahs-ARK/semafor.git
cd semafor
```

Modify the variables in `./bin/config.sh` as needed.

Run:

```
mvn package
```

Make sure you have the required data.
You can download FrameNet 1.5 [here](http://www.cs.cmu.edu/~ark/SEMAFOR/framenet15.tar.gz), but
also please fill out the request form [here](https://framenet.icsi.berkeley.edu/fndrupal/framenet_data)
if you haven't already.
Set the `luxmldir` environment variable in `training/config` to point at the `lu` folder.
The train/dev/test splits that were used in the NAACL '12 and subsequent papers can be found
[here](http://www.cs.cmu.edu/~ark/SEMAFOR/naacl2012_splits_with_rank_score.tar.gz).


### 1. Data structure preparation 1/2

Used to train and test the frame identification and argument identification models (please refer to our NAACL 2010 paper to understand these two steps). The first step is to create two maps -- I name these `framenet.original.map` and `framenet.frame.element.map`.
  1. The first map is of type `THashMap<String, THashSet<String>>`. It maps a frame to a set of disambiguated predicates (words along with part of speech tags, but in the style of FrameNet).
  2. The second map is of type `THashMap<String,THashSet<String>>`, which maps each frame to a set of frame element names. In other words, this data structure is necessary for the argument identification model to know what the frame elements are for each frame.

My versions of these two maps are present in this directory (these are just serialized Java objects). 
Use the `semafor-deps.jar` file in `lib/` directory of the googlecode repository to get the right version of GNU trove, and read (deserialize) these two maps. After that print the keys, and the corresponding values to see exactly what is 
stored in these maps. After that, you will need to create your own versions of these two maps for your domain, in exactly the same format as these maps. If you want existing code in SEMAFOR to create these maps, you could use the method `writeSerializedObject(Object object, String outFile`) in [SerializedObjects.java](https://github.com/sammthomson/semafor/blob/master/src/main/java/edu/cmu/cs/lti/ark/util/SerializedObjects.java) to write serialize those maps. So creating your own maps will be easy. You could also read the maps using that class.

### 2. Data structure preparation 2/2

Used for training and inference procedure.
```
./training/2_createRequiredData.sh
```

### 3. Training the frame identification model

 `./training/trainIdModel.sh` consists of:

1. alphabet creation and combination:
  ```
  ./training/3_1_idCreateAlphabet.sh
  ```
  This takes ~1 min using 8 threads (AMD Opteron(TM) 6272 2.1MHz processors; using the "ancestor" model).

2. creating feature events for each datapoint:
  ```
  ./training/3_2_idCreateFeatureEvents.sh
  ```
  Takes ~3-4 minutes.

3. training the frame identification model:
  ```
  ./training/3_3_idTrainBatch.sh
  ```
  Takes ~40 minutes.
  Line search in L-BFGS may fail at the end, but that does not mean training failed. 
  In models_0.0, there will be models produced every few iterations. If line search failed, take the last model.

4. convert the alphabet file:
  ```
  ./training/3_4_idConvertAlphabetFile.sh
  ```
  Takes <1 minute.

### 4. Training the argument identification model

`./training/trainArgModel.sh` consists of:

1. alphabet creation:
  ```
  ./training/4_1_createAlphabet.sh
  ```
  Takes ~7 minutes.

2. caching feature vectors:
  ```
  ./training/4_2_cacheFeatureVectors.sh
  ```
  Takes ~10 minutes.

3. training:
  ```
  ./training/4_3_training.sh
  ```
  Takes ~ a day.
  This step has a regularization hyperparameter, lambda. You may tune lambda on a development set to get the best results.
