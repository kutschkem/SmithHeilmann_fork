package edu.cmu.ark;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class DiscriminativeTagger implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 7096385301991299782L;

    public DiscriminativeTagger() {
        featureIndexes = new HashMap<String, Integer>();
        trainingData = null;
        labels = new ArrayList<String>();
        rgen = new Random(1234567);
    }

    /**
     * Load a list of the possible labels. This must be done before training so
     * that the feature vector has the appropriate dimensions
     * 
     * @param labelFile
     * @return
     */
    public static List<String> loadLabelList(String labelFile) {
        List<String> res = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(labelFile)));
            String buf;
            while ((buf = br.readLine()) != null) {
                res.add(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Load the BIO tagged supersense data from Semcor, as provided in the
     * SuperSenseTagger release (SEM_07.BI). We also use their POS labels, which
     * presumably were what their paper used. One difference is that this method
     * expects the data to be converted into a 3-column format with an extra
     * newline between each sentence (as in CoNLL data), which can be created
     * from the SST data with a short perl script.
     * 
     * @param path
     * @param labels
     * @return
     */
    public static List<LabeledSentence> loadSuperSenseData(String path, List<String> labels) {
        List<LabeledSentence> res = new ArrayList<LabeledSentence>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
            String buf;
            String[] parts;
            LabeledSentence sent = new LabeledSentence();
            while ((buf = br.readLine()) != null) {
                if (buf.length() == 0) {
                    if (sent.length() > 0) {
                        res.add(sent);
                        sent = new LabeledSentence();
                    }
                    continue;
                }
                parts = buf.split("\\t");
                String label = removeExtraLabels(parts[2], labels);
                sent.addToken(parts[0], SuperSenseFeatureExtractor.getInstance().getStem(parts[0], parts[1]), parts[1],
                        label);
                if (parts.length > 3 && !parts[3].equals("")) {
                    sent.setArticleID(parts[3]);
                }
            }

            if (sent.length() > 0)
                res.add(sent);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    /**
     * remove labels for adjectives and adverbs, which the SST does not address
     * because they are lumped together in wordnet
     * 
     * @param label
     * @param labels
     * @return
     */
    public static String removeExtraLabels(String label, List<String> labels) {
        /*if(label.contains("-adj.") || label.contains("-adv.") || label.endsWith(".other")){
        	return "0";
        }*/
        if (!labels.contains(label)) {
            return "0";
        }
        return label;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String trainFile = null;
        String testFile = null;
        String labelFile = null;
        int maxIters = 1;
        boolean developmentMode = false;
        String saveFile = null;
        String loadFile = null;
        String propertiesFile = "tagger.properties";
        boolean perceptron = true;
        boolean printWeights = false;
        String testPredictFile = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--train")) {
                trainFile = args[i + 1];
                i++;
            } else if (args[i].equals("--iters")) {
                maxIters = new Integer(args[i + 1]);
                i++;
            } else if (args[i].equals("--test")) {
                testFile = args[i + 1];
                i++;
            } else if (args[i].equals("--debug")) {
                developmentMode = true;
            } else if (args[i].equals("--labels")) {
                labelFile = args[i + 1];
                i++;
            } else if (args[i].equals("--save")) {
                saveFile = args[i + 1];
                i++;
            } else if (args[i].equals("--load")) {
                loadFile = args[i + 1];
                i++;
            } else if (args[i].equals("--properties")) {
                propertiesFile = args[i + 1];
                i++;
            } else if (args[i].equals("--mira")) {
                perceptron = false;
            } else if (args[i].equals("--weights")) {
                printWeights = true;
            } else if (args[i].equals("--test-predict")) {
                testPredictFile = args[i + 1];
                i++;
            }
        }

        loadProperties(propertiesFile);

        if (trainFile == null && loadFile == null) {
            System.err.println("Missing argument: --train or --load");
            System.exit(0);
        }
        if (labelFile == null && loadFile == null) {
            System.err.println("Missing argument: --labels");
            System.exit(0);
        }

        DiscriminativeTagger t;
        List<LabeledSentence> data;

        if (loadFile != null) {
            System.err.print("loading model from " + loadFile + "...");
            t = DiscriminativeTagger.loadModel(loadFile);
            System.err.println("done.");
        } else {
            System.err.println("training model from " + trainFile + "...");
            t = new DiscriminativeTagger();
            t.setDevelopmentMode(developmentMode);
            t.setPerceptron(perceptron);
            t.setSavePrefix(saveFile);
            List<String> labels = loadLabelList(labelFile);
            t.setLabels(labels);

            data = loadSuperSenseData(trainFile, labels);
            t.setTrainingData(data);
        }

        if (testFile != null) {
            data = loadSuperSenseData(testFile, t.getLabels());
            t.setTestData(data);
        }

        if (loadFile == null) {
            t.setMaxIters(maxIters);
            t.train();
        }

        if (testFile != null) {
            t.test();
        } else if (printWeights) {
            t.printWeights();
        } else if (testPredictFile != null) {
            data = loadSuperSenseData(testPredictFile, t.getLabels());
            t.printPredictions(data, t.getWeights());
        } else {
            t.tagStandardInput();
        }
    }

    public void printWeights() {
        List<String> fnames = new ArrayList<String>();
        fnames.addAll(featureIndexes.keySet());
        Collections.sort(fnames);
        for (String fname : fnames) {
            int index = featureIndexes.get(fname);
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                double value = finalWeights[i * featureIndexes.size() + index];
                if (value != 0.0) {
                    System.out.println(label + "\t" + fname + "\t" + value);
                }
            }
            System.out.println();
        }
    }

    /**
     * method to take input on STDIN, tag it using the stanford POS tagger, make
     * predictions, and then print it as output
     * 
     * 
     */
    protected void tagStandardInput() {
        try {
            MaxentTagger tagger = new MaxentTagger(properties.getProperty("posTaggerModel"));

            String buf;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            String doc;
            while (true) {
                doc = "";
                buf = "";

                buf = br.readLine();
                if (buf == null) {
                    break;
                }
                doc += buf;

                while (br.ready()) {
                    buf = br.readLine();
                    if (buf == null) {
                        break;
                    }
                    doc += buf + " ";
                }

                List<String> sentences = getSentences(doc);
                for (String s : sentences) {
                    LabeledSentence input = new LabeledSentence();
                    String tagged = tagger.tagString(s);
                    String[] taggedTokens = tagged.split("\\s");
                    int idx;
                    for (int i = 0; i < taggedTokens.length; i++) {
                        idx = taggedTokens[i].lastIndexOf('_');
                        String token = taggedTokens[i].substring(0, idx);
                        String pos = taggedTokens[i].substring(idx + 1);
                        input.addToken(token, SuperSenseFeatureExtractor.getInstance().getStem(token, pos), pos, "0");
                    }

                    findBestLabelSequenceViterbi(input, finalWeights);
                    System.out.println(input.taggedString());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getLabels() {
        return labels;
    }

    private void setLabels(List<String> labels) {
        this.labels = labels;
    }

    private void setDevelopmentMode(boolean developmentMode) {
        this.developmentMode = developmentMode;
    }

    /**
     * helper function for making predictions about raw text
     * 
     * @param document
     * @return
     */
    public static List<String> getSentences(String document) {
        List<String> res = new ArrayList<String>();
        String sentence;
        StringReader reader = new StringReader(document);

        DocumentPreprocessor dp = new DocumentPreprocessor(reader);
        List<List<? extends HasWord>> docs = new ArrayList<List<? extends HasWord>>();
        Iterator<List<? extends HasWord>> iter1;
        Iterator<? extends HasWord> iter2;

        try {
            for (List<? extends HasWord> l : dp) {
                docs.add(l);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        iter1 = docs.iterator();
        while (iter1.hasNext()) {
            iter2 = iter1.next().iterator();
            sentence = "";
            while (iter2.hasNext()) {
                String tmp = iter2.next().word().toString();
                sentence += tmp;
                if (iter2.hasNext()) {
                    sentence += " ";
                }
            }
            res.add(sentence);
        }

        return res;
    }

    /**
     * serialize model, clearing out unneeded data first (and then resetting it
     * so it can be used in subsequent iterations if necessary)
     * 
     * @param savePath
     */
    private void saveModel(String savePath) {
        List<LabeledSentence> tmpTrainingData = trainingData;
        List<LabeledSentence> tmpTestData = testData;
        String tmpSavePrefix = savePrefix;
        try {
            ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(savePath)));
            trainingData = null;
            testData = null;
            savePrefix = null;
            out.writeObject(this);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        trainingData = tmpTrainingData;
        testData = tmpTestData;
        savePrefix = tmpSavePrefix;
    }

    /**
     * 
     * load a serialized model
     * 
     * @param loadPath
     * @return
     */
    public static DiscriminativeTagger loadModel(String loadPath) {
        DiscriminativeTagger res = null;
        try {
            ObjectInputStream in = IOUtils.readStreamFromString(loadPath);
            res = (DiscriminativeTagger) in.readObject();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public void setTrainingData(List<LabeledSentence> trainingData) {
        this.trainingData = trainingData;
    }

    /**
     * train the model using the averaged perceptron (or perhaps MIRA in the
     * future, but that doesn't currently work) See Collins paper on
     * Discriminative HMMs.
     * 
     */
    public void train() {
        if (trainingData == null) {
            System.err.println("training data not set.");
            return;
        }
        if (perceptron)
            System.err.println("training with the perceptron.");
        else
            System.err.println("training with 1-best MIRA.");

        createDPTables();
        createFeatures();

        double[] intermediateWeights = new double[labels.size() * featureIndexes.size()];

        long numWordsProcessed = 0;
        long numWordsIncorrect = 0;
        long totalInstancesProcessed = 0;

        long trainingDataSize = trainingData.size();

        int numIters = 0;
        for (numIters = 0; numIters < maxIters; numIters++) {
            System.err.println("iter=" + numIters);
            Collections.shuffle(trainingData, rgen);
            for (int i = 0; i < trainingData.size(); i++) {
                LabeledSentence sent = trainingData.get(i);

                if (perceptron) {
                    findBestLabelSequenceViterbi(sent, intermediateWeights);
                    perceptronUpdate(sent, intermediateWeights);
                } else {
                    findBestLabelSequenceViterbi(sent, intermediateWeights, true);
                    MIRAUpdate(sent, intermediateWeights);
                }

                for (int j = 0; j < sent.length(); j++) {
                    if (!sent.getLabels().get(j).equals(sent.getPredictions().get(j))) {
                        numWordsIncorrect++;
                    }
                }
                numWordsProcessed += sent.length();
                totalInstancesProcessed++;

                for (int f = 0; f < finalWeights.length; f++) {
                    finalWeights[f] += intermediateWeights[f];
                }

                if (totalInstancesProcessed % 500 == 0) {
                    System.err.println("totalInstancesProcessed=" + totalInstancesProcessed);
                    System.err.println("pct. correct words in last 500 inst.:"
                            + NumberFormat.getInstance().format(
                                    (double) (numWordsProcessed - numWordsIncorrect) / numWordsProcessed));
                    numWordsIncorrect = 0;
                    numWordsProcessed = 0;
                }

            }

            if (developmentMode) {
                double normalizer = ((double) numIters + 1) * trainingDataSize;
                multiplyByScalar(finalWeights, 1.0 / normalizer);
                test();
                if (savePrefix != null)
                    saveModel(savePrefix + "." + numIters);
                multiplyByScalar(finalWeights, normalizer);
            }

        }

        // average the weights for the "averaged" part of the averaged
        // perceptron
        double normalizer = (double) maxIters * trainingDataSize;
        multiplyByScalar(finalWeights, 1.0 / normalizer);
        if (savePrefix != null)
            saveModel(savePrefix);

    }

    private void multiplyByScalar(double[] weights, double scalar) {
        for (int i = 0; i < weights.length; i++) {
            weights[i] *= scalar;
        }
    }

    /**
     * helper method for perceptron training. basically, update weights by
     * adding the feature vector for the correct label and subtracting the
     * feature vector for the predicted label
     * 
     * this method breaks down the process so that the changes are made token by
     * token
     * 
     * @param sent
     * @param intermediateWeights
     */
    private void perceptronUpdate(LabeledSentence sent, double[] intermediateWeights) {
        if (sent.predictionsAreCorrect()) {
            return;
        }

        for (int j = 0; j < sent.length(); j++) {
            int predictedLabelIndex = labels.indexOf(sent.getPredictions().get(j));
            int correctLabelIndex = labels.indexOf(sent.getLabels().get(j));
            if (correctLabelIndex == predictedLabelIndex) {
                continue;
            }

            Map<String, Double> featureValuesPredicted = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(
                    sent, j);
            Map<String, Double> featureValuesGold = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(sent,
                    j, false);

            int indexOffsetForCorrectLabel = correctLabelIndex * featureIndexes.size();
            int indexOffsetForPredictedLabel = predictedLabelIndex * featureIndexes.size();
            int featureIndex;

            for (String key : featureValuesGold.keySet()) {
                featureIndex = featureIndexes.get(key);
                intermediateWeights[featureIndex + indexOffsetForCorrectLabel] += featureValuesGold.get(key);
            }
            for (String key : featureValuesPredicted.keySet()) {
                featureIndex = featureIndexes.get(key);
                intermediateWeights[featureIndex + indexOffsetForPredictedLabel] -= featureValuesPredicted.get(key);
            }

        }
    }

    private void addToMap(Map<String, Double> mapToAddTo, Map<String, Double> map, boolean doSubtraction) {
        for (String key : map.keySet()) {
            Double val1 = mapToAddTo.get(key);
            Double val2 = map.get(key);
            if (val1 == null) {
                val1 = 0.0;
            }

            if (doSubtraction) {
                val1 += val2;
            } else {
                val1 -= val2;
            }
            mapToAddTo.put(key, val1);
        }
    }

    private void addToMap(Map<String, Double> mapToAddTo, Map<String, Double> map) {
        addToMap(mapToAddTo, map, false);
    }

    private void subtractFromMap(Map<String, Double> mapToAddTo, Map<String, Double> map) {
        addToMap(mapToAddTo, map, true);
    }

    /**
     * 1-best MIRA, using Andre and Kevin's paper
     * 
     * @param sent
     * @param intermediateWeights
     */
    private void MIRAUpdate(LabeledSentence sent, double[] intermediateWeights) {
        if (sent.predictionsAreCorrect()) {
            return;
        }

        // compute x L2 norm for denominator in MIRA update
        // compute number of incorrect labels (hamming loss)
        double C = 1.0;
        double x2 = 0.0;
        Map<String, Double> featureValuesPredicted;
        Map<String, Double> featureValuesGold;
        Map<String, Double> featureValuesDifferences = new HashMap<String, Double>();
        Map<String, Double> tmpFeatureValues;

        // compute the step size from looking at the whole sentence
        double numWrong = 0;
        double scoreGold = 0.0;
        double scorePredicted = 0.0;
        int predictedLabelIndex;
        int correctLabelIndex;
        for (int j = 0; j < sent.length(); j++) {
            predictedLabelIndex = labels.indexOf(sent.getPredictions().get(j));
            tmpFeatureValues = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(sent, j);
            scorePredicted += computeScore(tmpFeatureValues, intermediateWeights, predictedLabelIndex);
            addToMap(featureValuesDifferences, tmpFeatureValues);

            correctLabelIndex = labels.indexOf(sent.getLabels().get(j));
            tmpFeatureValues = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(sent, j, false);
            scoreGold += computeScore(tmpFeatureValues, intermediateWeights, correctLabelIndex);
            subtractFromMap(featureValuesDifferences, tmpFeatureValues);

            if (predictedLabelIndex != correctLabelIndex) {
                numWrong += 1.0;
            }
        }

        for (Double val : featureValuesDifferences.values()) {
            x2 += val * val;
        }

        double scoreDifference = scorePredicted - scoreGold;
        double update = Math.min(1.0 / C, (scoreDifference + numWrong) / x2);

        // Now update the features for each word.
        // It is done this way, rather than a single update for the sentence,
        // which should be equivalent,
        // due to implementation tricks used for extracting features.
        for (int j = 0; j < sent.length(); j++) {
            predictedLabelIndex = labels.indexOf(sent.getPredictions().get(j));
            correctLabelIndex = labels.indexOf(sent.getLabels().get(j));
            int indexOffsetForCorrectLabel = correctLabelIndex * featureIndexes.size();
            int indexOffsetForPredictedLabel = predictedLabelIndex * featureIndexes.size();
            int featureIndex;

            featureValuesPredicted = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(sent, j);
            featureValuesGold = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(sent, j, false);

            for (String key : featureValuesGold.keySet()) {
                featureIndex = featureIndexes.get(key);
                intermediateWeights[featureIndex + indexOffsetForCorrectLabel] += update * featureValuesGold.get(key);
            }
            for (String key : featureValuesPredicted.keySet()) {
                featureIndex = featureIndexes.get(key);
                intermediateWeights[featureIndex + indexOffsetForPredictedLabel] -= update
                        * featureValuesPredicted.get(key);
            }

        }

    }

    public void setTestData(List<LabeledSentence> testData) {
        this.testData = testData;
    }

    public void test() {
        test(finalWeights);
    }

    public void test(double[] weights) {

        if (testData == null)
            return;
        for (LabeledSentence sent : testData) {
            findBestLabelSequenceViterbi(sent, weights);
        }

        evaluatePredictions(testData, labels);
    }

    public void printPredictions(List<LabeledSentence> data, double[] weights) {
        if (data == null)
            return;
        for (LabeledSentence sent : data) {
            findBestLabelSequenceViterbi(sent, weights);
            System.out.println(sent.taggedString());
        }
    }

    /**
     * evaluate predictions using the CoNLL style evaluation. instances are
     * sequences of words with contiguous labels (e.g., President of the United
     * States) not just single tokens (e.g., States).
     * 
     * @param sentences
     */
    public static void evaluatePredictions(List<LabeledSentence> sentences, List<String> labels) {
        Map<String, Long> numPredicted = new HashMap<String, Long>();
        Map<String, Long> numGold = new HashMap<String, Long>();
        Map<String, Long> numCorrect = new HashMap<String, Long>();

        numPredicted.put("all", new Long(0));
        numGold.put("all", new Long(0));
        numCorrect.put("all", new Long(0));
        for (String label : labels) {
            if (label.equals("0"))
                continue;
            numPredicted.put(label.substring(2), new Long(0));
            numGold.put(label.substring(2), new Long(0));
            numCorrect.put(label.substring(2), new Long(0));
        }

        long tmp;
        Set<String> gold = new HashSet<String>();
        Set<String> pred = new HashSet<String>();
        int start, end;
        String startLabel;
        for (LabeledSentence sent : sentences) {
            gold.clear();
            pred.clear();
            for (int i = 0; i < sent.length(); i++) {
                startLabel = sent.getLabels().get(i);
                if (!startLabel.equals("0")) {
                    start = i;
                    end = i;
                    while (i + 1 < sent.length() && sent.getLabels().get(i + 1).charAt(0) == 'I') {
                        end = i + 1;
                        i++;
                    }
                    gold.add(startLabel.substring(2) + "\t" + start + "\t" + end);
                }
            }

            for (int i = 0; i < sent.length(); i++) {
                startLabel = sent.getPredictions().get(i);
                if (!startLabel.equals("0")) {
                    start = i;
                    end = i;
                    while (i + 1 < sent.length() && sent.getPredictions().get(i + 1).charAt(0) == 'I') {
                        end = i + 1;
                        i++;
                    }
                    pred.add(startLabel.substring(2) + "\t" + start + "\t" + end);
                }
            }

            for (String s : pred) {
                String label = s.substring(0, s.indexOf("\t"));
                tmp = numPredicted.get(label);
                numPredicted.put(label, tmp + 1);

                tmp = numPredicted.get("all");
                numPredicted.put("all", tmp + 1);

                if (gold.contains(s)) {
                    tmp = numCorrect.get(label);
                    numCorrect.put(label, tmp + 1);

                    tmp = numCorrect.get("all");
                    numCorrect.put("all", tmp + 1);
                    /*}else{
                    	//System.err.println("false negative:\t"+s+"\nPRED:\n"+sent.taggedString()+"\nGOLD:\n"+sent.taggedString(false));
                    	//System.err.println();
                    	String [] parts = s.split("\\t");
                    	String phrase = "";
                    	start = new Integer(parts[1]);
                    	end = new Integer(parts[2]);
                    	for(int m=start;m<=end;m++){
                    		phrase+=sent.getTokens().get(m)+" ";
                    	}
                    	System.err.println("false pos:\t"+phrase+"\t"+s);*/
                }
            }

            for (String s : gold) {
                String label = s.substring(0, s.indexOf("\t"));
                tmp = numGold.get(label);
                numGold.put(label, tmp + 1);

                tmp = numGold.get("all");
                numGold.put("all", tmp + 1);

                /*if(!pred.contains(s)){
                	//System.err.println("false negative:\t"+s+"\nPRED:\n"+sent.taggedString()+"\nGOLD:\n"+sent.taggedString(false));
                	//System.err.println();
                	String [] parts = s.split("\\t");
                	String phrase = "";
                	start = new Integer(parts[1]);
                	end = new Integer(parts[2]);
                	for(int m=start;m<=end;m++){
                		phrase+=sent.getTokens().get(m)+" ";
                	}
                	System.err.println("false neg:\t"+phrase+"\t"+s);
                }*/
            }
            // System.err.println();

        }

        for (String label : labels) {
            if (label.equals("0"))
                continue;
            if (label.startsWith("I"))
                continue;

            double p = (double) numCorrect.get(label.substring(2)) / numPredicted.get(label.substring(2));
            double r = (double) numCorrect.get(label.substring(2)) / numGold.get(label.substring(2));
            double g = (double) numGold.get(label.substring(2));
            System.err.println(label.substring(2) + "\tF1:\t"
                    + (2 * p * r / (p + r) + "\tP:\t" + p + "\tR:\t" + r + "\tnumGold:\t" + g));
        }
        double p = (double) numCorrect.get("all") / numPredicted.get("all");
        double r = (double) numCorrect.get("all") / numGold.get("all");
        double g = (double) numGold.get("all");
        System.err.println("all\tF1:\t" + (2 * p * r / (p + r) + "\tP:\t" + p + "\tR:\t" + r + "\tnumGold:\t" + g));

    }

    /**
     * initialize dynamic programming tables used by the viterbi algorithm
     * 
     */
    private void createDPTables() {

        int maxNumTokens = 0;

        /*
        for(LabeledSentence sent: trainingData){
        	if(sent.length()>maxNumTokens){
        		maxNumTokens = sent.length();
        	}
        }
        */

        maxNumTokens = 200;
        dpValues = new double[maxNumTokens][labels.size()];
        dpBackPointers = new int[maxNumTokens][labels.size()];
    }

    /**
     * compute a dot product of a set of feature values and the corresponding
     * weights. This involves looking up the appropriate indexes into the weight
     * vector.
     * 
     * @param featureValues
     * @param weights
     * @param i
     * @return
     */
    private double computeScore(Map<String, Double> featureValues, double[] weights, int labelIndex) {
        double res = 0.0;

        if (labelIndex == -1) {
            return 0.0;
        }

        double featureValue;
        double featureWeight;
        int indexOffsetForLabel = labelIndex * featureIndexes.size();

        for (String key : featureValues.keySet()) {
            featureValue = featureValues.get(key);
            Integer index = featureIndexes.get(key);
            if (index != null) { // test set features may not have been
                                 // instantiated from the training data
                featureWeight = weights[index + indexOffsetForLabel];
                res += featureWeight * featureValue;
            }
        }

        return res;
    }

    /**
     * This is an efficiency trick so that the whole feature vector need not be
     * computed in each inner loop of the viterbi algorith. Since only one
     * feature of a token depends on the previous label, we can compute all the
     * others first, and then add this one when needed. This can reduce
     * computation by a factor equal to the number of labels, roughly (83 for
     * supersenses!)
     * 
     * 
     * @param prevLabel
     * @param curLabelIndex
     * @param weights
     * @return
     */
    private double previousLabelFeatureScore(String prevLabel, int curLabelIndex, double[] weights) {
        int indexOffsetForLabel = curLabelIndex * featureIndexes.size();
        return weights[featureIndexes.get("prevLabel=" + prevLabel) + indexOffsetForLabel]; // *
                                                                                            // 1.0,
                                                                                            // ommitted
                                                                                            // for
                                                                                            // brevity
    }

    /**
     * before training, loop through the training data once to instantiate all
     * the possible features, so we don't have to worry about null in the
     * HashMaps
     * 
     */
    private void createFeatures() {
        lastFeatureIndex = 0;

        // instantiate previous label features for all possible previous labels
        for (String label : labels) {
            String key = "prevLabel=" + label;
            if (!featureIndexes.containsKey(key)) {
                featureIndexes.put(key, lastFeatureIndex++);
            }
        }

        // instantiate the rest of the features
        for (LabeledSentence sent : trainingData) {
            for (int i = 0; i < sent.length(); i++) {
                if (i > 0)
                    sent.getPredictions().set(i - 1, sent.getLabels().get(i - 1));
                Map<String, Double> values = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(sent, i,
                        false);

                for (String key : values.keySet()) {
                    if (!featureIndexes.containsKey(key)) {
                        featureIndexes.put(key, lastFeatureIndex++);
                    }
                }
            }
        }

        // now create the array of feature weights
        finalWeights = new double[labels.size() * featureIndexes.size()];

    }

    public static Properties getProperties() {
        if (properties == null) {
            loadProperties("tagger.properties");
        }
        return properties;
    }

    public static Properties loadProperties(String propertiesFile) {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        return properties;
    }

    public void findBestLabelSequenceViterbi(LabeledSentence sent, double[] weights) {
        findBestLabelSequenceViterbi(sent, weights, false);
    }

    /**
     * uses the Viterbi algorithm to find the current best sequence of labels
     * for a sentence, given the weight vector. used in both training and
     * testing
     * 
     * @param sent
     * @param weights
     * @param includeLossTerm
     *            whether to perform loss augmented inference (e.g., with MIRA)
     */
    public void findBestLabelSequenceViterbi(LabeledSentence sent, double[] weights, boolean includeLossTerm) {
        int numTokens = sent.length();

        if (dpValues.length < numTokens) { // expand the size of the dynamic
                                           // programming tables if necessary
            dpValues = new double[(int) (numTokens * 1.5)][labels.size()];
            dpBackPointers = new int[(int) (numTokens * 1.5)][labels.size()];
        }

        double maxScore;
        double score;
        int maxIndex = -1;
        String label;
        Map<String, Double> featureValues;
        double tmpScore;
        String prevLabel;

        for (int i = 0; i < numTokens; i++) {
            sent.getPredictions().set(i, null);
        }

        // for each token
        for (int i = 0; i < numTokens; i++) {
            featureValues = SuperSenseFeatureExtractor.getInstance().extractFeatureValues(sent, i);
            // String stem = sent.getStems().get(i);
            // String tok = sent.getTokens().get(i);
            // String pos = sent.getPOS().get(i);

            // for each current label
            for (int j = 0; j < labels.size(); j++) {
                maxIndex = -1;
                maxScore = Double.NEGATIVE_INFINITY;
                tmpScore = computeScore(featureValues, weights, j);
                label = labels.get(j);
                if (includeLossTerm && !label.equals(sent.getLabels().get(i))) {
                    tmpScore += 1.0;
                }

                // skip if this sense is not possible according to wordnet
                // if no possible senses are listed, allow any (e.g., for names)
                /*
                Set<String> possibleSenses = SuperSenseFeatureExtractor.getInstance().getPossibleSenses(stem);
                Set<String> tmp = SuperSenseFeatureExtractor.getInstance().getPossibleSenses(tok);
                if(tmp != null && possibleSenses != null) possibleSenses.addAll(tmp);
                if(possibleSenses!=null && !label.equals("0") 
                		&& !possibleSenses.contains(label) 
                		&& !pos.matches("^NNPS?")){
                	dpValues[i][j] = maxScore;
                	dpBackPointers[i][j] = maxIndex;
                	if(sent.getLabels().get(i).equals(label)){
                		continue;
                	}
                	continue;
                }
                */

                // consider each possible previous label
                for (int k = 0; k < labels.size(); k++) {
                    prevLabel = labels.get(k);
                    if (useBIconstraintInDecoding && label.charAt(0) == 'I') {
                        if (prevLabel.equals("0") || i == 0) {
                            continue;
                        }
                        if (!prevLabel.substring(2).equals(label.substring(2))) {// assume
                                                                                 // tags
                                                                                 // are
                                                                                 // formatted
                                                                                 // B-class1,
                                                                                 // I-class1,
                                                                                 // etc.
                            continue;
                        }
                    }

                    // compute current score based on previous scores
                    score = 0.0;
                    if (i > 0) {
                        sent.getPredictions().set(i - 1, labels.get(k));
                        score = dpValues[i - 1][k];
                    }

                    // the score for the previous label is added on separately
                    // here,
                    // in order to avoid computing the whole score, which only
                    // depends
                    // on the previous label for one feature,
                    // a large number of times: O(labels*labels).
                    // TODO plus versus times doesn't matter here, right? Use
                    // plus because of numeric overflow
                    score += tmpScore + previousLabelFeatureScore(labels.get(k), j, weights);

                    // find the max of the combined score
                    // use that to choose what to output for the previous label
                    if (score > maxScore) {
                        maxScore = score;
                        maxIndex = k;
                    }

                    // if this is the first token, we don't need to iterate over
                    // all possible previous labels,
                    // because there is only one possibility (i.e., null)
                    if (i == 0) {
                        break;
                    }
                }

                dpValues[i][j] = maxScore;
                dpBackPointers[i][j] = maxIndex;
            }
        }

        // extract predictions from backpointers
        maxIndex = -1;
        maxScore = Double.NEGATIVE_INFINITY;
        // first, find the best label for the last token
        for (int j = 0; j < labels.size(); j++) {
            score = dpValues[numTokens - 1][j];
            if (score > maxScore) {
                maxScore = score;
                maxIndex = j;
            }
        }
        // now iterate backwards by following backpointers
        for (int i = numTokens - 1; i >= 0; i--) {
            sent.getPredictions().set(i, labels.get(maxIndex));
            maxIndex = dpBackPointers[i][maxIndex];
        }

    }

    public double[] getWeights() {
        return finalWeights;
    }

    public String getSavePrefix() {
        return savePrefix;
    }

    public void setSavePrefix(String savePrefix) {
        this.savePrefix = savePrefix;
    }

    public void setMaxIters(int maxIters) {
        this.maxIters = maxIters;
    }

    public int getMaxIters() {
        return maxIters;
    }

    public boolean isPerceptron() {
        return perceptron;
    }

    public void setPerceptron(boolean perceptron) {
        this.perceptron = perceptron;
    }

    private int maxIters = 5;
    private List<LabeledSentence> trainingData;
    private List<LabeledSentence> testData;

    private double[] finalWeights;

    /*
     * feature weights are stored in an array of size equal to the number
     * of features times the number of labels
     * 
     *  this map goes from feature names (keys) to feature indexes WITHOUT offsets.
     *  the offsets are equal to the label index times the number of features
     *  
     */
    private Map<String, Integer> featureIndexes;
    private List<String> labels;
    private int lastFeatureIndex = 0;
    private String savePrefix = null;

    private double[][] dpValues;
    private int[][] dpBackPointers;
    private Random rgen;
    private boolean developmentMode;
    private static Properties properties;
    private boolean perceptron = false;
    private boolean useBIconstraintInDecoding = true;

}
