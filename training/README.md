This is a README for training on the FrameNet 1.5 full text annotations
Dipanjan Das 
dipanjan@cs.cmu.edu
2/18/2012
=======================================================================

Training models for frame-semantic parsing with SEMAFOR is still a very laborious and 
clunky set of steps. Your kind patience is required to train the models :-)

0)
  Checkout the code:

  git clone git@github.com:sammthomson/semafor.git
  cd semafor

  Modify the variables in ./bin/config.sh as needed.

  Run:

  mvn package

  Make sure you have the required data.
  You'll need the "lu" folder from framenet 1.5
  (see https://framenet.icsi.berkeley.edu/fndrupal/framenet_data for obtaining the FN dataset)
  set the "luxmldir" environment variable in training/config to point at it


1) The first step is to create some data structures which are used to train and test the frame identification and argument identification models (please refer to our NAACL 2010 paper to understand these two steps). The first step is to create two maps -- I name these framenet.original.map and framenet.frame.element.map

   i) The first map is of type THashMap<String, THashSet<String>>. It maps a frame to a set of disambiguated predicates 
      (words along with part of speech tags, but in the style of FrameNet). 
   ii) The second map is of type THashMap<String,THashSet<String>>, which maps each frame to a set of frame element names. 
       In other words, this data structure is necessary for the argument identification model to know what 
       the frame elements are for each frame.
 
My versions of these two maps are present in this directory (these are just serialized Java objects). Use the semafor-deps.jar file in lib/ directory of the googlecode repository to get the right version of GNU trove, and read (deserialize) these two maps. After that print the keys, and the corresponding values to see exactly what is stored in these maps. After that, you will need to create your own versions of these two maps for your domain, in exactly the same format as these maps.

If you want existing code in SEMAFOR to create these maps, you could use the method writeSerializedObject(Object object, String outFile) in edu/cmu/cs/lti/ark/util/SerializedObjects.java to write serialize those maps. So creating your own maps will be easy. You could also read the maps using that class.


2) The next step creates some more data structures used for the training and inference procedure:

  ./training/2_createRequiredData.sh


3) This step corresponds to training the frame identification model.

  # step 1: alphabet creation. Run the script:
  ./training/3_1_trainFrameIdentificationModel.sh
  # This takes ~75 minutes on my 2.2 GHz Intel Core i7 4 core macbook pro (sam)

  # step 2: alphabet combination. Run the script:
  ./training/3_2_combineAlphabets.sh
  # Takes ~3 seconds

  # step 3: creating feature events for each datapoint. Run the script:
  ./training/3_3_createFeatureEvents.sh
  # Takes ~1 hour

  # step 4: training the frame identification model. Run the script:
  ./training/3_4_trainBatch.sh
  # takes ~4 hours

  The training procedure will run for a long period of time. Line search in L-BFGS may fail at the end, but that does not mean training failed. In models_0.0, there will be models produced every few iterations. If line search failed, take the last model.

  # step 5: convert the alphabet file. Run the script:
  ./training/3_5_convertAlphabetFile.sh
  # takes ~3 seconds


4) This step corresponds to the training the argument identification model.

  # step 1: Alphabet Creation. Run the script:
  ./training/4_1_createAlphabet.sh
  # takes ~6 minutes

  # step 2: Caching Feature Vectors
  ./training/4_2_cacheFeatureVectors.sh
  # takes ~1.5 minutes

  # step 3: training
  ./training/4_3_training.sh
  # takes ~

  Step 4.3 has a regularization hyperparameter, lambda. You may tune lambda on a development set to get the best results.
