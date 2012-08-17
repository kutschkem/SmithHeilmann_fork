package edu.cmu.ark;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.*;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;

public class SuperSenseFeatureExtractor {
	private boolean useMorphCache;

	private SuperSenseFeatureExtractor(){
		senseMap = null;
		morphMap = null;
		possibleSensesMap = null;
		senseCountMap = null;
		clusterMap = null;	
		
		
		String usePre = DiscriminativeTagger.getProperties().getProperty("usePrefixAndSuffixFeatures", "false");
		if(usePre.equals("true")) usePrefixAndSuffixFeatures = true;
		else usePrefixAndSuffixFeatures = false;
		
		String useClu = DiscriminativeTagger.getProperties().getProperty("useClusterFeatures", "false");
		if(useClu.equals("true")) useClusterFeatures = true;
		else useClusterFeatures = false;
		
		String useBigrams = DiscriminativeTagger.getProperties().getProperty("useBigramFeatures", "false");
		if(useBigrams.equals("true")) useBigramFeatures = true;
		else useBigramFeatures = false;
		
		String useMorphCacheStr = DiscriminativeTagger.getProperties().getProperty("useMorphCache","true");
		if(useMorphCacheStr.equals("true")) useMorphCache = true;
		else useMorphCache = false;
		
		if(!useMorphCache){
			String wnPath = ClassLoader.getSystemResource(DiscriminativeTagger.getProperties().getProperty("WordNetPath","dict/file_properties.xml")).getFile();
			try{
				JWNL.initialize(new FileInputStream(wnPath));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//featureMap = new HashMap<String, Double>();
	}
	
	public static SuperSenseFeatureExtractor getInstance(){
		if(instance == null){
			instance = new SuperSenseFeatureExtractor(); 
		}
		return instance;
	}
	

	public Map<String, Double> extractFeatureValues(LabeledSentence sent, int j) {
		return extractFeatureValues(sent, j, true);
	}
	
	
	public String wordClusterID(String word){
		Integer id = clusterMap.get(word.toLowerCase());
		if(id == null){
			return "UNK";
		}
		return "C"+id;
	}
	
	
	/**
	 * Extracts a map of feature names to values for a particular token in a sentence.
	 * These can be aggregated to get the feature vector or score for a whole sentence.
	 * These replicate the features used in Ciaramita and Altun, 2006 
	 * 
	 * @param sent the labeled sentence object to extract features from
	 * @param j index of the word in the sentence to extract features for
	 * @param usePredictedLabels whether to use predicted labels or gold labels (if available) 
	 * @return
	 */
	public Map<String, Double> extractFeatureValues(LabeledSentence sent, int j, boolean usePredictedLabels) {
		Map<String, Double> featureMap = new HashMap<String, Double>();
		
		if(useClusterFeatures && clusterMap == null) loadClusters();
		
		String curStem = sent.getStems().get(j);
		String curTok = sent.getTokens().get(j);
		String curPOS = sent.getPOS().get(j);
		String curShape = sent.getWordShapes().get(j);
		String curCluster = null;
		if(useClusterFeatures) curCluster = wordClusterID(curTok);
		String prevLabel = startSymbol;
			
		if(sent.getMostFrequentSenses() == null || sent.getMostFrequentSenses().size() != sent.length()){
			sent.setMostFrequentSenses(extractFirstSensePredictedLabels(sent));
		}
		
		String firstSense = sent.getMostFrequentSenses().get(j);
		
		String prevShape = startSymbol;
		String prevPOS = startSymbol;
		String prevStem = startSymbol;
		String prevCluster = startSymbol;
		String nextShape = startSymbol;
		String nextPOS = endSymbol;
		String nextStem = endSymbol;
		String nextCluster = endSymbol;
		
		String prev2Shape = startSymbol;
		String prev2POS = startSymbol;
		String prev2Stem = startSymbol;
		String prev2Cluster = startSymbol;
		String next2Shape = startSymbol;
		String next2POS = endSymbol;
		String next2Stem = endSymbol;
		String next2Cluster = endSymbol;

		if(j-2 >= 0){
			prev2Shape = sent.getWordShapes().get(j-2);
			prev2Stem = sent.getStems().get(j-2);
			prev2POS = sent.getPOS().get(j-2);
			if(useClusterFeatures) prev2Cluster = wordClusterID(sent.getTokens().get(j-2));
		}
		
		if(j-1 >= 0){
			prevShape = sent.getWordShapes().get(j-1);
			prevStem = sent.getStems().get(j-1);
			prevPOS = sent.getPOS().get(j-1);
			if(useClusterFeatures) prevCluster = wordClusterID(sent.getTokens().get(j-1));
			if(usePredictedLabels){
				prevLabel = sent.getPredictions().get(j-1);
			}else{
				prevLabel = sent.getLabels().get(j-1);
			}

		}
		
		if(j+1 < sent.length()){
			nextShape = sent.getWordShapes().get(j+1);
			nextStem = sent.getStems().get(j+1);
			nextPOS = sent.getPOS().get(j+1);
			if(useClusterFeatures) nextCluster = wordClusterID(sent.getTokens().get(j+1));
		}
		
		if(j+2 < sent.length()){
			next2Shape = sent.getWordShapes().get(j+2);
			next2Stem = sent.getStems().get(j+2);
			next2POS = sent.getPOS().get(j+2);
			if(useClusterFeatures) next2Cluster = wordClusterID(sent.getTokens().get(j+2));
		}
		
		//sentence level lexicalized features
		//for(int i=0;i<sent.length();i++){
		//	if(j==i) continue;
		//	String sentStem = sent.getStems().get(i);
		//	featureMap.put("curTok+sentStem="+curTok+"\t"+sentStem,1.0/sent.length());
		//}
		
		//bias
		featureMap.put("bias",1.0);
		
		//first sense features
		if(firstSense == null) firstSense = "0";
		featureMap.put("firstSense="+firstSense,1.0);
		featureMap.put("firstSense+curTok="+firstSense+"\t"+curStem,1.0);
		
		
		if(useClusterFeatures){
			//cluster features for the current token
			featureMap.put("firstSense+curCluster="+firstSense+"\t"+curCluster,1.0);
			featureMap.put("curCluster="+curCluster, 1.0);
		}
		
		
		//previous label feature
		if(prevLabel != startSymbol) featureMap.put(("prevLabel="+prevLabel), 1.0);
		
		
		//word and POS features
		if(curPOS.equals("NN") || curPOS.equals("NNS")){
			featureMap.put(("curPOS_common"), 1.0);
		}
		if(curPOS.equals("NNP") || curPOS.equals("NNPS")){
			featureMap.put(("curPOS_proper"), 1.0);
		}
		
		featureMap.put(("curTok="+curStem), 1.0);
		featureMap.put(("curPOS="+curPOS), 1.0);
		featureMap.put(("curPOS_0="+curPOS.charAt(0)), 1.0);
		
		if(prevPOS != startSymbol){
			featureMap.put(("prevTok="+prevStem), 1.0);
			if(useBigramFeatures) featureMap.put(("prevTok+curTok="+prevStem+"\t"+curStem), 1.0);
			featureMap.put(("prevPOS="+prevPOS), 1.0);
			featureMap.put(("prevPOS_0="+prevPOS.charAt(0)), 1.0);
			if(useClusterFeatures) featureMap.put(("prevCluster="+prevCluster), 1.0);
			//if(useClusterFeatures) featureMap.put("firstSense+prevCluster="+firstSense+"\t"+prevCluster,1.0);
		}
		
		if(nextPOS != endSymbol){
			featureMap.put(("nextTok="+nextStem), 1.0);
			if(useBigramFeatures) featureMap.put(("nextTok+curTok="+nextStem+"\t"+curStem), 1.0);
			featureMap.put(("nextPOS="+nextPOS), 1.0);
			featureMap.put(("nextPOS_0="+nextPOS.charAt(0)), 1.0);
			if(useClusterFeatures) featureMap.put(("nextCluster="+nextCluster), 1.0);
			//if(useClusterFeatures) featureMap.put("firstSense+nextCluster="+firstSense+"\t"+nextCluster,1.0);
		}
		
		if(prev2POS != startSymbol){
			featureMap.put(("prev2Tok="+prev2Stem), 1.0);
			featureMap.put(("prev2POS="+prev2POS), 1.0);
			featureMap.put(("prev2POS_0="+prev2POS.charAt(0)), 1.0);
			if(useClusterFeatures) featureMap.put(("prev2Cluster="+prev2Cluster), 1.0);
			//if(useClusterFeatures) featureMap.put("firstSense+prev2Cluster="+firstSense+"\t"+prev2Cluster,1.0);
		}
		
		if(next2POS != endSymbol){
			featureMap.put(("next2Tok="+next2Stem), 1.0);
			featureMap.put(("next2POS="+next2POS), 1.0);
			featureMap.put(("next2POS_0="+next2POS.charAt(0)), 1.0);
			if(useClusterFeatures) featureMap.put(("next2Cluster="+next2Cluster), 1.0);
			//if(useClusterFeatures) featureMap.put("firstSense+next2Cluster="+firstSense+"\t"+next2Cluster,1.0);
		}
		
		
		//word shape features
		featureMap.put("curShape="+curShape, 1.0);
		
		if(prevPOS != startSymbol){
			featureMap.put("prevShape="+prevShape, 1.0);
		}
		
		if(nextPOS != endSymbol){
			featureMap.put("nextShape="+nextShape, 1.0);
		}
		
		if(prev2POS != startSymbol){
			featureMap.put("prev2Shape="+prev2Shape, 1.0);
		}
		
		if(next2POS != endSymbol){
			featureMap.put("next2Shape="+next2Shape, 1.0);
		}
		
		String firstCharCurTok = curTok.substring(0,1);
		if(firstCharCurTok.toLowerCase().equals(firstCharCurTok)){
			featureMap.put("curTokLowercase", 1.0);
		}else if(j==0){
			featureMap.put("curTokUpperCaseFirstChar", 1.0);
		}else{
			featureMap.put("curTokUpperCaseOther", 1.0);
		}
		
		
		
		
		//3-letter prefix and suffix features (disabled by default)
		if(usePrefixAndSuffixFeatures){
			featureMap.put("prefix="+prefix(curTok), 1.0);
			featureMap.put("suffix="+suffix(curTok), 1.0);
		}
		
		
		return featureMap;
	}
	
	
	
	
	
	
	

	private String suffix(String curTok) {
		int len = curTok.length();
		if(len >= 3)	return curTok.substring(len-3);
		return curTok;
	}

	private String prefix(String curTok) {
		int len = curTok.length();
		if(len >= 3)	return curTok.substring(0,3);
		return curTok;
	}

	
	/**
	 * Extract most frequent sense baseline from WordNet data,
	 * using Ciaramita and Altun's approach.  Also, uses
	 * the data from the Supersense tagger release.
	 * 
	 * @param sent
	 * @return
	 */
	public List<String> extractFirstSensePredictedLabels(LabeledSentence sent){
		if(senseMap == null){
			String useOldDataFormat = DiscriminativeTagger.getProperties().getProperty("useOldDataFormat","false");
			if(useOldDataFormat.equals("true")) loadSenseDataOriginalFormat();
			else loadSenseDataNewFormat();
		}
		
		List<String> res = new ArrayList<String>();
		
		String prefix = "B-";
		String mostFrequentSense = null;
		String phrase = null;
		for(int i=0; i<sent.length(); i++){
			mostFrequentSense = null;
			String pos = sent.getPOS().get(i);
			int j;
			for(j=sent.length()-1; j>=i; j--){
				phrase = createPhrase(sent,i,j);
				String endPos = sent.getPOS().get(j);
				mostFrequentSense = getMostFrequentSense(phrase, pos.substring(0,1));
				if(mostFrequentSense != null) break;
				mostFrequentSense = getMostFrequentSense(phrase, endPos.substring(0,1));
				if(mostFrequentSense != null) break;
			}
			
			prefix = "B-";
			if(mostFrequentSense != null){
				while(i<j){
					res.add((prefix+mostFrequentSense).intern());
					prefix = "I-";
					i++;
				}
				
			}
			
			if(mostFrequentSense != null){
				res.add((prefix+mostFrequentSense).intern());
			}else{
				res.add("0");
			}
			
		}
		
		return res;
	}
	
	
	/**
	 * helper function for the most frequent sense baseline
	 * 
	 * @param sent
	 * @param from
	 * @param to
	 * @return
	 */
	private String createPhrase(LabeledSentence sent, int from, int to) {
		String res = "";
		for(int i=from; i<to; i++){
			res += sent.getStems().get(i) + "_";
		}
		res += sent.getStems().get(to);
		return res;		
	}
	

	private void addMorph(String word, String pos, String stem){
		Map<String, String> posMap = morphMap.get(pos);
		if(posMap == null){
			posMap = new HashMap<String, String>();
			morphMap.put(pos.intern(), posMap);
		}
		
		posMap.put(word.intern(), stem.intern());
	}
	
	
	private void addMostFrequentSense(String phrase, String simplePOS, String sense, int numSenses){
		//store the most frequent sense
		Map<String, String> posMap = senseMap.get(simplePOS);
		if(posMap == null){
			posMap = new HashMap<String, String>();
			senseMap.put(simplePOS.intern(), posMap);
		}
		posMap.put(phrase.intern(), sense.intern());
		
		//now store the count
		Map<String, Integer> countPosMap = senseCountMap.get(simplePOS);
		if(countPosMap == null){
			countPosMap = new HashMap<String, Integer>();
			senseCountMap.put(simplePOS.intern(), countPosMap);
		}
		countPosMap.put(phrase.intern(), numSenses);
	}
	
	
	public String getStemCache(String word, String pos){
		if(morphMap == null){
			String useOldDataFormat = DiscriminativeTagger.getProperties().getProperty("useOldDataFormat","true");
			if(useOldDataFormat.equals("true")) loadMorphDataOriginalFormat();
			else loadMorphDataNewFormat();
		}
		String res = word;
		Map<String, String> posMap = morphMap.get(pos);
		if(posMap != null){
			res = posMap.get(word.toLowerCase());
			if(res == null){
				res = word.toLowerCase();
			}
		}
		return res;
	}
	
	public String getStem(String word, String pos){
		if(useMorphCache){
			return getStemCache(word,pos);
		}else{
			return getStemWN(word,pos);
		}
	}
	
	
	public String getMostFrequentSense(String phrase, String pos){
		if(senseMap == null){
			String useOldDataFormat = DiscriminativeTagger.getProperties().getProperty("useOldDataFormat","false");
			if(useOldDataFormat.equals("true")) loadSenseDataOriginalFormat();
			else loadSenseDataNewFormat();
		}
		String res = null;
		Map<String, String> posMap = senseMap.get(pos);
		if(posMap != null){
			res = posMap.get(phrase);
		}
		return res;
	}
	
	public int getNumberOfSensesForPhrase(String phrase, String pos){
		if(senseMap == null){
			String useOldDataFormat = DiscriminativeTagger.getProperties().getProperty("useOldDataFormat","false");
			if(useOldDataFormat.equals("true")) loadSenseDataOriginalFormat();
			else loadSenseDataNewFormat();
		}
		Integer res = 0;
		Map<String, Integer> posMap = senseCountMap.get(pos);
		if(posMap != null){
			res = posMap.get(phrase);
			if(res == null) res = 0;
		}
		return res;
	}
	
		
	private void loadClusters() {
		System.err.print("loading word cluster information...");
		clusterMap = new HashMap<String, Integer>();
		
		try {
			BufferedReader br;
			String buf;
			String [] parts;
			
			String clusterFile = DiscriminativeTagger.getProperties().getProperty("clusterFile","data/clusters/clusters_1024_49.gz");
		
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(clusterFile))));
			Integer clusterID = 0;
			while((buf = br.readLine())!= null){
				parts = buf.split("\\s");
				for(int i=0;i<parts.length;i++){
					clusterMap.put(parts[i].intern(), clusterID);
				}
				clusterID = new Integer(clusterID+1);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("done.");
	}

	
	
	/**
	 * load morphology and sense information provided by the 
	 * Supersense tagger release from SourceForge
	 * 
	 */
	public void loadSenseDataNewFormat() {
		System.err.print("loading most frequent sense information...");
		
		senseMap = new HashMap<String, Map<String, String>>();
		possibleSensesMap = new HashMap<String, Set<String>>();
		senseCountMap = new HashMap<String, Map<String, Integer>>();
		
		Properties props = DiscriminativeTagger.getProperties();
		
		try {
			BufferedReader br;
			String buf;
			String [] parts;
			
			String nounFile = props.getProperty("nounFile","data/gaz/NOUNS.GAZ.gz");
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(nounFile))));
			while((buf = br.readLine())!= null){
				parts = buf.split("\\t");
				String sense = parts[1].substring(parts[1].indexOf("=")+1);
				Integer numSenses = new Integer(parts[3].substring(parts[3].indexOf("=")+1));
				addMostFrequentSense(parts[0], "N", sense, numSenses);
			}
			br.close();
			
			String verbFile = props.getProperty("verbFile","data/gaz/VERBS.GAZ.gz");
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(verbFile))));
			while((buf = br.readLine())!= null){
				parts = buf.split("\\t");
				String sense = parts[1].substring(parts[1].indexOf("=")+1);
				Integer numSenses = new Integer(parts[3].substring(parts[3].indexOf("=")+1));
				addMostFrequentSense(parts[0], "V", sense, numSenses);
			}
			br.close();
			
			String possibleSensesFile = props.getProperty("possibleSensesFile","data/gaz/possibleSuperSenses.GAZ.gz");
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(possibleSensesFile))));
			while((buf = br.readLine())!= null){
				parts = buf.split("\\t");
				
				String [] wordParts = parts[0].split("_");
				for(int j=0;j<wordParts.length;j++){
					Set<String> tmp = possibleSensesMap.get(wordParts[j]);
					if(tmp == null){
						tmp = new HashSet<String>();
					}
					for(int i=1;i<parts.length;i++){
						if(j==0){
							tmp.add("B-"+parts[i]);
						}else{
							tmp.add("I-"+parts[i]);
						}
					}
					possibleSensesMap.put(wordParts[j], tmp);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("done.");
	}
	
	
	/**
	 * load data from the original SST release
	 * 
	 */
	public void loadSenseDataOriginalFormat() {
		System.err.print("loading most frequent sense information (old format)...");
		
		Properties props = DiscriminativeTagger.getProperties();
		possibleSensesMap = new HashMap<String, Set<String>>();
		senseMap = new HashMap<String, Map<String, String>>();
		senseCountMap = new HashMap<String, Map<String, Integer>>();
		

		String nounFile = ClassLoader.getSystemResource(props.getProperty("nounFile","data/oldgaz/NOUNS_WS_SS_P.gz")).getFile();
		loadSenseFileOriginalFormat(nounFile, "N");
		
		String verbFile = ClassLoader.getSystemResource(props.getProperty("verbFile","data/oldgaz/VERBS_WS_SS.gz")).getFile();
		loadSenseFileOriginalFormat(verbFile, "V");

		System.err.println("done.");
	}

	
	private void loadMorphDataOriginalFormat(){
		System.err.print("loading morphology information (old format)...");
		
		morphMap = new HashMap<String, Map<String, String>>();
		try{
			BufferedReader br;
			String buf;
			String[] parts;
			String morphFile = DiscriminativeTagger.getProperties().getProperty("morphFile","data/oldgaz/MORPH_CACHE.gz");
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(morphFile))));
			while((buf = br.readLine())!= null){
				parts = buf.split("\\t");
				addMorph(parts[1], parts[0], parts[2]);
				addMorph(parts[1], "UNKNOWN", parts[2]);
			}
			br.close();
			addMorph("men", "NNS", "man");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	
	private void loadMorphDataNewFormat(){
		System.err.print("loading morphology information...");
		
		morphMap = new HashMap<String, Map<String, String>>();
		try{
			BufferedReader br;
			String buf;
			String[] parts;
			String morphFile = DiscriminativeTagger.getProperties().getProperty("morphFile","data/morph/MORPH_CACHE.gz");
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(morphFile))));
			while((buf = br.readLine())!= null){
				parts = buf.split("\\t");
				addMorph(parts[1], parts[0], parts[2]);
				addMorph(parts[1], "UNKNOWN", parts[2]);
			}
			br.close();
			addMorph("men", "NNS", "man");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	

	public String getStemWN(String word, String pos){
		if(!(pos.startsWith("N") || pos.startsWith("V") || pos.startsWith("J") || pos.startsWith("R"))
				|| pos.startsWith("NNP"))
		{
			return word.toLowerCase();
		}
				
		String res = word.toLowerCase();
		
		if(res.equals("is") || res.equals("are") || res.equals("were") || res.equals("was")){
			res = "be";
		}else{
			try{
				//Iterator<String> iter = Dictionary.getInstance().getMorphologicalProcessor().lookupAllBaseForms(POS.VERB, res).iterator();
				
				IndexWord iw;
				if(pos.startsWith("V")) iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.VERB, res);
				else if(pos.startsWith("N")) iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.NOUN, res);
				else if(pos.startsWith("J")) iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.ADJECTIVE, res);
				else iw = Dictionary.getInstance().getMorphologicalProcessor().lookupBaseForm(POS.ADVERB, res);
					
				if(iw == null) return res;
				res = iw.getLemma();
			}catch(NullPointerException e){
				e.printStackTrace();
				System.exit(0);
			}catch(Exception e){
				e.printStackTrace();
			}
		}		
		
		return res;
	}
	
	
	private void loadSenseFileOriginalFormat(String senseFile, String shortPOS) {
		BufferedReader br;
		String buf;
		String [] parts;
		int spacing = 3; //the noun file has 3 sets of columns
		if(shortPOS.equals("V")) spacing = 2; //verb file has 2
		
		try {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(senseFile))));
	
			while((buf = br.readLine())!= null){
				if(buf.length()==0) continue;
				parts = buf.split("\\s");
				String sense = parts[2];
				int numSenses = (int)(parts.length-1)/spacing;
				//first sense listed is the most frequent one
				//record it for the most frequent sense baseline algorithm
				addMostFrequentSense(parts[0], shortPOS, sense, numSenses);
							
				//read possible senses, split up multi word phrases
				String [] wordParts = parts[0].split("_");
				for(int i=2;i<parts.length; i+=spacing){
					for(int j=0;j<wordParts.length;j++){
						Set<String> tmp = possibleSensesMap.get(wordParts[j]);
						if(tmp == null){
							tmp = new HashSet<String>();
						}
						String possibleSense = parts[i];
						if(j==0){
							tmp.add(("B-"+possibleSense).intern());
						}else{
							tmp.add(("I-"+possibleSense).intern());
						}
						
						possibleSensesMap.put(wordParts[j].intern(), tmp);
					}
				}
			}
			br.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void setUsePrefixAndSuffixFeatures(boolean usePrefixAndSuffixFeatures) {
		this.usePrefixAndSuffixFeatures = usePrefixAndSuffixFeatures;
	}
	
	public Set<String> getPossibleSensesForStem(String stem) {
		return possibleSensesMap.get(stem);
	}
	
	
	private static SuperSenseFeatureExtractor instance;
	
	public static final String startSymbol = null;//"<START>";
	public static final String endSymbol = null;//"<END>";
	
	private Map<String, Map<String, String>> senseMap; //pos, word -> most frequent supersense
	private Map<String, Map<String, Integer>> senseCountMap; //pos, word -> number of senses
	private Map<String, Set<String>> possibleSensesMap; //stem -> set of possible supersenses
	private Map<String, Map<String, String>> morphMap; //pos, word -> stem
	private boolean usePrefixAndSuffixFeatures;
	private Map<String, Integer> clusterMap;
	//private boolean usePossibleSenseFeatures = false;
	
	private boolean useClusterFeatures;

	private boolean useBigramFeatures;

	
}
