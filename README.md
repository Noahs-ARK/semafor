**_Notice_: SEMAFOR is no longer being maintained. Please see [open-SESAME](https://github.com/Noahs-ARK/open-sesame) for a newer, more accurate frame-semantic parsing system.**
=

    SEMAFOR (a frame-semantic parser for English)
    Copyright (C) 2012
    Dipanjan Das, Andre Martins, Nathan Schneider, Desai Chen, Sam Thomson &
    Noah A. Smith
    Language Technologies Institute, Carnegie Mellon University
    <http://www.ark.cs.cmu.edu/SEMAFOR>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.



SEMAFOR: Semantic Analysis of Frame Representations
===================================================

SEMAFOR is a tool for automatic analysis of the frame-semantic structure of English text.

[FrameNet](<http://framenet.icsi.berkeley.edu>) is a lexical resource that groups predicates in a hierarchy of structured
concepts, known as *frames*. Each frame in the lexicon in turn defines several named *roles* corresponding to aspects of
that concept (e.g. participants in an event).

This tool attempts to find which words in text evoke which semantic frames, and to find and label each frame's
*arguments* - portions of the sentence that fill a role associated with the frame. It takes as input a file with English
sentences, one per line, and performs the following steps:

  0. Preprocessing
     The sentences are lemmatized, part-of-speech tagged, and syntactically parsed.

  1. Target identification
     Frame-evoking words and phrases ("targets") are heuristically identified in each sentence.

  2. Frame identification
     A log-linear model, trained on FrameNet 1.5 data with full-text frame annotations, produces for each target a
     probability distribution over frames in the FrameNet lexicon (optionally constrained by a semi-supervised filter).
     The target is then labeled with the highest-scoring frame.

  3. Argument identification
     A second log-linear model, trained on the same data, considers every role of each labeled frame instance and
     identifies a span of words in the sentence - or NULL - as filling that role. A subsequent step ensures that none of
     a frame's overt arguments overlap using beam search; an alternate strategy using AD^3 (or Alternating Directions
     Dual Decomposition) uses two other constraints used in FrameNet for argument identification.

  4. Output
     An XML or JSON file is produced containing the text of the input sentences, augmented with the frame-semantic
     information (target-frame and argument-role pairings) predicted by the system. See the papers listed below
     ("Further Reading") for algorithmic details and experimental evaluation of the components of this system.

What follows is an overview of the organization of SEMAFOR's directory structure, and how it can be installed and run on
new data.


Requirements
============

Running the SEMAFOR tool *requires* Java 1.7. It should run on any platform (Windows, Unix, or Mac OS).


Contents
========

Underneath the root folder, there are the following files and folders:

<dl>
  <dt>bin/</dt>
    <dd>
      Executables for running semafor
    </dd>

  <dt>lib/</dt>
    <dd>
      Java libraries required for this project, as detailed below
    </dd>

  <dt>scripts/</dt>
    <dd>
      Executables required for preprocessing raw text and evaluating the performance of SEMAFOR
    </dd>

  <dt>src/</dt>
    <dd>
      Source files of the SEMAFOR project
    </dd>

  <dt>training/</dt>
    <dd>
      Scripts and data used for training the two models
    </dd>

  <dt>LICENSE</dt>
    <dd>
      Text of the GNU General Public License, Version 3
    </dd>

  <dt>README.md</dt>
    <dd>
      This file
    </dd>

  <dt>pom.xml</dt>
    <dd>
      The Maven project object model
    </dd>
</dl>


Installation
============

Downloads
---------

This experimental fork is maintained at <https://github.com/Noahs-ARK/semafor>.
For a more stable version, the latest official release, SEMAFOR v2.1, can be downloaded from
<https://github.com/Noahs-ARK/semafor-semantic-parser>

In preprocessing, SEMAFOR uses MaltParser as the syntactic dependency parser.
To use MaltParser, download and unpack the model files for MaltParser and SEMAFOR from here:
<http://www.ark.cs.cmu.edu/SEMAFOR/semafor_malt_model_20121129.tar.gz> (~140MB).
The model file for the MaltParser was trained on sections 02-21 of the WSJ section of the Penn Treebank, and the model
files for SEMAFOR were trained on the FrameNet 1.5 datasets.


Environment Variables
---------------------

The file `bin/config.sh` lists a set of variables which should be modified within the file before running
SEMAFOR:

- `SEMAFOR_HOME`: absolute path where the repository has been cloned.

- `MALT_MODEL_DIR`: the absolute path where the Malt models have been decompressed.

- `JAVA_HOME_BIN`: the absolute path to the bin directory under which the executables javac and java can be found.
If the environment variable `${JAVA_HOME}` is set, `${JAVA_HOME_BIN}` should be `${JAVA_HOME}/bin`.


Compilation
-----------

Compilation is easiest using Maven version >= 3.0 (<http://maven.apache.org/>).

    mvn package

will compile and package Semafor-3.0-alpha-04.jar (including all dependencies) to the target/ directory.
Many scripts in bin/ point to Semafor-3.0-alpha-04.jar, so run `mvn package` immediately after installing, and again
after making any changes to source code.


Running the Frame-Semantic Parser
=================================

    ./bin/runSemafor.sh <absolute-path-to-input-file-with-one-sentence-per-line> <output-file> <number-of-threads>

Some users have reported improved and more consistent runtime behavior when enabling [NUMA](http://docs.oracle.com/javase/7/docs/technotes/guides/vm/performance-enhancements-7.html#numa).
If your system is NUMA-capable, you can enable it, with the JVM option `-XX:+UseNUMA`, which requires the `-XX:+UseParallelGC` option to also be specified.


Server Mode
--------------

SEMAFOR can also be run as a TCP socket server.
It accepts dependency parses in conll format, and replies with json frame-semantic parses.
Run the following command

    java -Xms4g -Xmx4g -cp target/Semafor-3.0-alpha-04.jar edu.cmu.cs.lti.ark.fn.SemaforSocketServer model-dir:<directory-of-trained-model> port:<port>


The message: `Listening on port: NNNN` will appear once the server has loaded
the model and is ready to accept connections (where `NNNN` is the port).
You can test that it's working with the following:

    cat src/test/resources/fixtures/example.conll | nc localhost NNNN

(where `NNNN` is again the port).


Retraining SEMAFOR
==================

Please see the [training README](training/README.md).



Further Reading
==================

If this parser is used, please cite the following papers, depending on the components used:

1. An Exact Dual Decomposition Algorithm for Shallow Semantic Parsing with Constraints
      Dipanjan Das, André F. T. Martins, and Noah A. Smith
      Proceedings of *SEM 2012
(Please cite the above paper if you use AD^3 within SEMAFOR.)


2. Graph-Based Lexicon Expansion with Sparsity-Inducing Penalties
      Dipanjan Das and Noah A. Smith
      Proceedings of NAACL 2012


3. Semi-Supervised Frame-Semantic Parsing for Unknown Predicates
	Dipanjan Das and Noah A. Smith
	Proceedings of ACL 2011
(Please cite the above two papers if you use the graph-based filters within SEMAFOR.)


4. Probabilistic Frame-Semantic Parsing
	Dipanjan Das, Nathan Schneider, Desai Chen, and Noah A. Smith
	Proceedings of NAACL-HLT 2010
(The first paper describing SEMAFOR.)


For further information, please read:

5. SEMAFOR 1.0: A Probabilistic Frame-Semantic Parser
	Dipanjan Das, Nathan Schneider, Desai Chen, and Noah A. Smith
	CMU Technical Report, CMU-LTI-10-001

6. Semi-Supervised and Latent Variable Models of Natural Language Semantics
        Dipanjan Das
        Ph.D. Thesis, Carnegie Mellon University, May 2012

Details of the training and test sections of the FrameNet 1.5 datasets can be
found in paper 3. The supplementary material document for this paper lists the names of the test documents,
and can be found here: <http://www.dipanjandas.com/files/acl-hlt2011-suppl-semafor.pdf>


Contact
==========

If you find any bugs or have questions, please email
Sam Thomson (<sthomson@cs.cmu.edu>),
Dipanjan Das (<dipanjan@cs.cmu.edu>, <dipanjand@gmail.com>), or
Nathan Schneider (<nschneid@cs.cmu.edu>, <neatnate@gmail.com>).


Version History
==================

- 1.0 First public release (2010-04-26)

- 1.0.1 Second public release (2010-09-02)
     Now includes compiled Java .class files for SEMAFOR, so compiling from source is
     not necessary.

     Now includes the SemEval 2007 Task 19 dataset and evaluation resources (see
     README.txt in the SemEval2007/ directory for details).

     Bug fixes:
     1) Fixed an issue of tokenization with 'sed'. Now uses 'sed -f' explicitly
        instead of running the script as an executable.
     2) Included 'mkdir tmp' in the MST parser root directory. This is necessary
        for running the MST parser.
     3) At the top of the 'mxpost' script that does POS tagging, changed '#!/bin/ksh'
        to '#!/bin/bash' because the former did not run in some machines.

- 2.0 Third public release (2011-04-23)
    New features:
    1) Added server mode to the MST parser.
    2) Trained on newer, much larger dataset from FrameNet 1.5.
    3) Users can input their own targets file, suiting different domains and requirements.
    4) One class file does all the pipeline processing unlike the previous versions.
    5) The argument identification stage has been made much faster.
    6) Previous releases could not handle large set of sentences; that has been fixed.
    7) Added source to googlecode.com for open source development.
    8) Semi-supervised graph-based constraints have been added for frame identification.

- 2.1 Fourth public release (2012-05-22)
    New features:
    1) Beam width for beam search was reduced after noticing that it makes no difference empirically
       in FrameNet datasets. This made argument identification much faster. Current beam width=100.
    2) Added a new graph filter based on the NAACL 2012 paper by Das and Smith. This makes frame
       identification more accurate than before.
    3) Finally, added AD^3, a dual decomposition algorithm for argument identification. See Das et al.
       (*SEM 2012) for more details. Note that the results using the released code will not match
       the results reported in the *SEM paper, because of a new Java implementation different
       from the implementation used for the paper.
