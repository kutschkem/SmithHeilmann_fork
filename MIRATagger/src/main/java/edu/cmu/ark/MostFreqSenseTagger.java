package edu.cmu.ark;

import java.util.List;

public class MostFreqSenseTagger {

	/**
	 * baseline to label words using most frequent sense information
	 * 
	 * @param args
	 */
	public static void main(String[] args) {		
		String testFile = null;
		List<String> labels;
		String labelFile = null;
		boolean doEval = false;
		String propertiesFile = "tagger.properties";
		
		for(int i=0;i<args.length;i++){
			if(args[i].equals("--test")){
				testFile = args[i+1]; 
				i++;
			}else if(args[i].equals("--labels")){
				labelFile = args[i+1];
				i++;
			}else if(args[i].equals("--eval")){
				doEval = true;
			}else if(args[i].equals("--properties")){
				propertiesFile = args[i+1];
				i++;
			}
		}
		
		DiscriminativeTagger.loadProperties(propertiesFile);
		
		if(labelFile == null){
			System.err.println("Missing argument: --labels");
			System.exit(0);
		}
		if(testFile == null){
			System.err.println("Missing argument: --test");
			System.exit(0);
		}
		
		labels = DiscriminativeTagger.loadLabelList(labelFile);
		
		List<LabeledSentence> data = null;
		if(testFile != null){
			data = DiscriminativeTagger.loadSuperSenseData(testFile,labels);
		}
		
		for(LabeledSentence sent: data){
			List<String> pred = SuperSenseFeatureExtractor.getInstance().extractFirstSensePredictedLabels(sent);
			for(int i=0; i<pred.size(); i++){
				if(pred.get(i)==null){
					pred.set(i, "0");
				}
			}
			sent.setPredictions(pred);
			if(!doEval) System.out.println(sent.taggedString(true));
		}
		
		if(doEval) DiscriminativeTagger.evaluatePredictions(data, labels);		

	}

}
