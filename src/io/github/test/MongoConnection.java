package io.github.test;

import com.mongodb.DBCursor;

import io.github.sqlconnection.BaseConnection;

public class MongoConnection {
	public static void main(String[] args){
		BaseConnection bc = new BaseConnection();
		bc.connect();
		bc.showDBs();
		bc.setDBAndCollection("cs336", "unlabel_review_after_splitting");
		bc.showRecords();
	
		
		
		
		
		
		
		
		bc.close();
	}
}