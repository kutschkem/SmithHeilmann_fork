README for the java Supersense Tagger

LICENSING INFORMATION

Please see the licenses in the "doc" subdirectory
for the following components:
-the Stanford Part of Speech Tagger (used to tokenize and POS tag
 when running the SS tagger from the command line)
-JWNL (used to stem words)
-WordNet (required by JWNL, also used to look up most-frequent senses for words)
-the Apache Commons Logging library (required by JWNL)

The file "doc/LICENSE.txt" is a copy of the GNU GPL license
under which this code is distributed.


////////////////////////////////////////////////////////////////////////////////
Copyright 2011 Michael Heilman, Language Technologies Institute,
Carnegie Mellon University.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
////////////////////////////////////////////////////////////////////////////////



This code is a Java reimplementation of the Supersense Tagger described
in the following paper:

M. Ciaramita and Y. Altun. 2006. Broad-Coverage Sense Disambiguation
and Information Extraction with a Supersense Sequence Tagger.
Proc. of EMNLP.

Ciaramita and Altun provide a C++ implementation of their tagger at
http://sourceforge.net/projects/supersensetag/

This tagger was designed to be compiled and run on a UNIX system, 
though it should not be too difficult to run/compile on a windows or mac,
particularly with Eclipse, Netbeans, etc.  The code was built with
version jdk1.6.0_07 of Java.

A precompiled jar archive is included (supersense-tagger.jar).
The tagger can be executed with the run.sh script.
Using that script, the tagger will take plain text on standard input
and return tagged text on standard output.
When run this way, the tagger will split sentences, tokenize, and POS tag
the input using the Stanford POS tagger (included in the lib directory).
The output will be in a tab-separated multi-column format, with 
one token per line.

The tagger can also be run as a socket server, using the server.sh script.
The server expects tab-separated words and POS tags, as in the file
misc/server-test.txt (note that the output of the tagger for this file is 
not perfect).

For example:

bash server.sh &
cat misc/server-test.txt | nc localhost 5557


The trained model used by the tagger (models/superSenseModelAllSemcor.ser.gz)
is trained on the version of the Semcor corpus 
provided in Ciaramita and Altun's C++ SST-1.0 distribution 
(http://sourceforge.net/projects/supersensetag/).
This corresponds to version 1.6 of Semcor, which is available at
http://www.cse.unt.edu/~rada/downloads.html.
In the SST distribution, we use the file DATA/SEM.BI, which is
included in the data directory in this distribution.
The tagger also uses pre-computed gazetteers (i.e., lists of words and their most
frequent senses in WordNet) from Ciaramita and Altun's SST-1.0 distribution.
These are included in the directory "data/oldgaz".


//////////////////////////////////////////////////////

Command-line options to the main class edu.cmu.ark.DiscriminativeTagger:

--train FILE
specify FILE as the file that the tagger should be trained on
(decompress data/SEM.BI.gz to see the expected format).

--test FILE
specify FILE as the file to test on after training (i.e., your dev or test set)

--test-predict FILE
print out predictions for the file FILE (which should be in the same format
as the training data).  These can then be evaluated or analyzed outside the program.

--iters N
tell the system to train for N passes through the training set (around 5-10 seems best).

--debug
tell the system to print out extra debugging information

--labels FILE
specify FILE as the file containing the set of possible labels for the tagger to
consider.  (e.g., data/SEM_07.BI.labels)

--save FILE
save a trained model to FILE.  
Intermediate models will be saved to FILE.0, FILE.1, etc. after each iteration.

--load FILE
load a trained model from FILE

--properties FILE
specify FILE as the configuration file that tells the system where to find
needed resources (e.g., tagger.properties)


////////////////////////////////////////////////////////
Example training command:

java -Xmx1000m -cp supersense-tagger.jar edu/cmu/ark/DiscriminativeTagger \
 --properties tagger.properties --labels data/SEM.BI.labels --train data/SEM.BI.data \
 --iters 5 --debug --save testmodel.ser.gz

See run.sh for an example of how to load and use a model.
See server.sh for an example of how to run a socket server.


//////////////////////////////////////////////////////
Additional version information:

-lib/jwnl.jar and the dict/ subdirectory are from version 1.4.1 rc2 of
 the Java WordNet Library (http://sourceforge.net/projects/jwordnet/).

-lib/commons-logging.jar is from version 1.0.4 of the Apache Commons
 Logging library (http://commons.apache.org/logging/).






