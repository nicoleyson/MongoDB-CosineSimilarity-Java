//NICOLE
package io.github.sqlconnection;

import java.awt.Cursor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Set;
import java.util.Random;
import java.math.*;

public class BaseConnection {
	private String ip = "localhost";
	private int port = 27017;
	
	MongoClient mongo = null;	
	private DB currentDB = null;
	protected DBCollection currentCollection = null;
	

	/**
	 * construction function with different parameters
	 */
	public BaseConnection(){};
	public BaseConnection(String ip){
		this.ip = ip;
	}
	public BaseConnection(int port){
		this.port = port;
	}
	public BaseConnection(String ip, int port){
		this.ip = ip; this.port = port;
	}
	public void connect(){
		mongo = new MongoClient(this.ip,this.port);
	}
	public void setDBAndCollection(String dbName, String collectionName){
		this.currentDB = mongo.getDB(dbName);
		this.currentCollection = this.currentDB.getCollection(collectionName);
	}
	/**
	 * print all dbs in mongo
	 */
	public void showDBs(){
		List<String> dbs = mongo.getDatabaseNames();
		for(String db:dbs){
			System.out.println(db);
		}
	}


	public static double CosineSimilarity(double[] vector1, double[] vector2){
		return 0.0;
	}
		
	public void showRecords(){
		this.currentCollection.find();
		DBCursor dbc = this.currentCollection.find();
		
		//Store All The Term And Their Frequency Appearance Count In A HashMap
		HashMap<String, Double> frequency = new HashMap<String, Double>();
		while(dbc.hasNext()){
			DBObject object = dbc.next();
			BasicDBList basicDBList = (BasicDBList) object.get("review");
			BasicDBObject[] objectArray = basicDBList.toArray(new BasicDBObject[0]);
			for(int i = 0; i < objectArray.length; i++){
				String word = objectArray[i].getString("word");
				if(frequency.get(word) != null){
					frequency.put(word, frequency.get(word) + 1.0);
				}else{
					frequency.put(word, 1.0);
				}
			}
		}
		
		//Store The 6 Random Selected Documents And Their Term tfidf in vectors
		ArrayList<double[]> vectors = new ArrayList<double[]>(6);
		Object[] document_id = new Object[6];
		for(int i = 0; i < 6; i++){
			double[] vector;
			int document = new Random().nextInt(500);
			DBObject object = this.currentCollection.find().limit(-1).skip(document).next();
			BasicDBList basicDBList = (BasicDBList) object.get("review");
			BasicDBObject[] objects = basicDBList.toArray(new BasicDBObject[0]);
			vector = new double[objects.length];
			for(int x = 0; x < objects.length;x++){
				String term = objects[x].getString("word");
				String count = objects[x].getString("count");
				double weight_tf = 1 + Math.log(Double.parseDouble(count));
				double inverse_df = 1 + Math.log(500.0/frequency.get(term));
				vector[x] = (weight_tf * inverse_df);
			}
			document_id[i] = object.get("id");
			vectors.add(i, vector);
		}
		
		DBCollection documents = this.currentDB.getCollection("unlabel_review");
		
		//Select Random Document From The Six Vectors
		int randomDocument = new Random().nextInt(6);
		double[] r_Vector = vectors.get(randomDocument);
		for(int i = 0; i < 6; i++){
			DBObject selectedDoc = documents.findOne(new BasicDBObject("id", document_id[i]));
			System.out.println("Review: " + selectedDoc.get("review"));
			
			if(i != randomDocument){
				double dot = 0.0;
				double r_normal = 0.0;
				double doc_normal = 0.0;
				
				double[] documentVector = vectors.get(i);
				int size = r_Vector.length > documentVector.length ? r_Vector.length : documentVector.length;
				for(int x = 0; x < size; x++){
					double one;
					double two;
					if(x >= r_Vector.length){ one = 0.0; }else{ one = r_Vector[x];}
					if(x >= documentVector.length){ two = 0.0; }else{ two = documentVector[x];}
						
					dot += (one * two);
					r_normal += Math.pow(one, 2);
					doc_normal += Math.pow(two, 2);
				}
				r_normal = Math.sqrt(r_normal);
				doc_normal = Math.sqrt(doc_normal);
				
				System.out.println("Cosine Similarity: " + (dot / (r_normal * doc_normal)));
			}
		}
		
		DBObject selectedDocument = dbc.getCollection().findOne(new BasicDBObject("id", document_id[randomDocument]));
		BasicDBList basicDBList = (BasicDBList) selectedDocument.get("review");
		BasicDBObject[] objectArray = basicDBList.toArray(new BasicDBObject[0]);
		for(int i = 0; i < objectArray.length; i++){
			System.out.println("Word: " + objectArray[i].get("word"));
		}
		
		String selectedTermOne, selectedTermTwo;
		Scanner reader = new Scanner(System.in);
		String[] selectedTerms = new String[2];
		System.out.println("Select Term One:");
		selectedTerms[0] = reader.nextLine();
		System.out.println("Select Term Two:");
		selectedTerms[1] = reader.nextLine();
		
		double N = 6.0;
		HashMap<String, Double> queryFrequency = new HashMap<String, Double>();
		for(int i = 0; i < 6; i++){
			DBObject document = dbc.getCollection().findOne(new BasicDBObject("id", document_id[i]));
			basicDBList = (BasicDBList) document.get("review");
			objectArray = basicDBList.toArray(new BasicDBObject[0]);
			for(int x = 0; x < objectArray.length; x++){
				String term = objectArray[x].getString("word");
				if(queryFrequency.get(term) != null){
					queryFrequency.put(term, queryFrequency.get(term) + 1.0);
				}else{
					queryFrequency.put(term, 1.0);
				}
			}
		}
		
		for(int i = 0; i < 6; i++){
			double score = 0.0;
			
			DBObject selectedDoc = documents.findOne(new BasicDBObject("id", document_id[i]));
			System.out.println("Review " + selectedDoc.get("review"));
			for(int x = 0; x < 2; x++){
				String term = selectedTerms[x];
				double df = queryFrequency.get(term);
				double tf = 0.0;
				
				DBObject document = dbc.getCollection().findOne(new BasicDBObject("id", document_id[i]));
				basicDBList = (BasicDBList) document.get("review");
				objectArray = basicDBList.toArray(new BasicDBObject[0]);
				for(int y = 0; y < objectArray.length; y++){
					String documentWord = objectArray[y].getString("word");
					if(documentWord.compareTo(term) == 0){
						tf = Double.parseDouble(objectArray[y].getString("count"));
						y = objectArray.length;
					}
				}
				
				double termA = 1 + Math.log(tf);
				double termB = (N / df);
				score += (termA * termB);
				System.out.println("Term " + term + " tf " + tf + " df " + df + " termA " + termA + " termB " + termB);
			}
			
			System.out.println("Score" + score);
		}
	}

	public void close(){
		if(this.mongo !=null){
			this.mongo.close();
		}
		this.mongo = null;
	
	}

}
