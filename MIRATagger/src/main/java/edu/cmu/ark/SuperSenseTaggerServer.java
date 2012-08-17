package edu.cmu.ark;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;



public class SuperSenseTaggerServer {

	public static void main(String[] args) {

		int port = 5555;
		String modelFile = null;
		String propertiesFile = "tagger.properties";
		boolean debug = false;
		for(int i=0;i<args.length;i++){
			if(args[i].equals("--port")){
				port = new Integer(args[i+1]);
				i++;
			}else if(args[i].equals("--model")){
				modelFile = args[i+1];
				i++;
			}else if(args[i].equals("--debug")){
				debug = true;
			}else if(args[i].equals("--properties")){
				propertiesFile = args[i+1];
				i++;
			}
		}
		
		if(modelFile == null){
			System.err.println("need to specify --model");
			System.exit(0);
		}
		
		DiscriminativeTagger.loadProperties(propertiesFile);
		DiscriminativeTagger tagger = DiscriminativeTagger.loadModel(modelFile);
		
		// declare a server socket and a client socket for the server
		// declare an input and an output stream
		ServerSocket server = null;
		BufferedReader br;
		PrintWriter outputWriter;
		Socket clientSocket = null;
		try {
			server = new ServerSocket(port);
		}
		catch (IOException e) {
			System.err.println(e);
		} 

		// Create a socket object from the ServerSocket to listen and accept 
		// connections.
		// Open input and output streams

		while (true) {
			System.err.println("Waiting for Connection on Port: "+port);
			try {
				clientSocket = server.accept();
				System.err.println("Connection Accepted From: "+clientSocket.getInetAddress());
				br = new BufferedReader(new InputStreamReader(new DataInputStream(clientSocket.getInputStream())));
				outputWriter = new PrintWriter(new PrintStream(clientSocket.getOutputStream()));


				LabeledSentence inputSentence = new LabeledSentence();
				String buf = br.readLine();
				if(debug) System.err.println("received: " + buf);
				String [] parts;
				List<LabeledSentence> sentences = new ArrayList<LabeledSentence>();
				do{
					
					if(buf.length()==0){
						if(inputSentence.length()>0){
							sentences.add(inputSentence);
							inputSentence = new LabeledSentence();								
						}
					}else{
						parts = buf.split("\\t");
						if(parts.length == 2){//word and POS				
							inputSentence.addToken(parts[0], SuperSenseFeatureExtractor.getInstance().getStem(parts[0], parts[1]), parts[1], "0");//TODO
						}else if(parts.length == 3){//word,stem, POS
							inputSentence.addToken(parts[0], parts[1], parts[2], "0");
						}
					}
					
					buf = br.readLine();
					if(debug) System.err.println("recv:\t" + buf);
				}while(br.ready());
				
				if(inputSentence.length()>0){
					sentences.add(inputSentence);
					inputSentence = new LabeledSentence();								
				}
								
				//PROCESS
				try{
					String output = "";
					for(LabeledSentence sent: sentences){	
						tagger.findBestLabelSequenceViterbi(sent, tagger.getWeights());
						output += sent.taggedString()+"\n";
					}
					outputWriter.print(output);
					if(debug) System.err.println("sent:\t" + output);
				}catch(Exception e){
					outputWriter.println("");
					e.printStackTrace();
				}
				
				outputWriter.flush();
				outputWriter.close();

			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
