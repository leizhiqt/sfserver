package com.mooo.mycoz.socket.sfserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mooo.mycoz.db.pool.DbConnectionManager;

public class IDGenerator {

	private static Log log = LogFactory.getLog(IDGenerator.class);

	//commons SQL

	private static final String SELECT_MAX_BY_TABLE="SELECT MAX(id) maxid FROM ";
	
	private static final String ENABLE_CARD="SELECT id FROM Card WHERE rfidcode=? AND wineJarId IS NOT NULL";

	public synchronized static int getNextID(Connection connection,String table) {
		boolean notConn = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        int nextId=0;
        try {
        	if(connection==null){
        		notConn = true;
        		connection=DbConnectionManager.getConnection();
        	}
        	
			pstmt = connection.prepareStatement(SELECT_MAX_BY_TABLE + table);
			result = pstmt.executeQuery();
			while (result.next()) {
				nextId = result.getInt(1);
			}

			nextId++;
		}catch (SQLException e) {
			e.printStackTrace();
	   }finally {
			try {
				if(result != null)
					result.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				if(pstmt != null)
					pstmt.close();
				
				if(notConn){
					if(connection != null)
						connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		return nextId;
	} // getNextID(String table)
	
	public synchronized static int getNextID(String table) {
		return getNextID(null,table);
	}
	
	public synchronized static boolean enableCard(Connection connection,String rfidCode){
		boolean notConn = false;
		PreparedStatement pstmt = null;
		boolean enableCard = false;
		try{
        	if(connection==null){
        		notConn = true;
        		connection=DbConnectionManager.getConnection();
        	}
        	
			pstmt = connection.prepareStatement(ENABLE_CARD);
			pstmt.setString(1, rfidCode);
			
			ResultSet result = pstmt.executeQuery();
			if(result.next()){
//				id = result.getInt(1);
				enableCard = true;
			}
		} catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("Exception="+e.getMessage());
			enableCard = false;
		}finally{

			try {
				if(pstmt != null)
					pstmt.close();
				
				if(notConn){
					if(connection != null)
						connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return enableCard;
	}
	
	public synchronized static int getId(Connection connection,String table,String fieldName,String fieldValue){
		boolean notConn = false;
		PreparedStatement pstmt = null;
		int id = -1;
		String sql = "SELECT id FROM "+table+" WHERE "+fieldName+"=?";
		try{
			
        	if(connection==null){
        		notConn = true;
        		connection=DbConnectionManager.getConnection();
        	}

        	pstmt = connection.prepareStatement(sql);
			pstmt.setString(1, fieldValue);
			
			ResultSet result = pstmt.executeQuery();
			if(result.next()){
				id = result.getInt(1);
			}
		} catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("Exception="+e.getMessage());
			e.printStackTrace();
		}finally{

			try {
				if(pstmt != null)
					pstmt.close();
				
				if(notConn){
					if(connection != null)
						connection.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return id;
	}
	
	public synchronized static int getId(String table,String fieldName,String fieldValue){
		return getId(null,table, fieldName, fieldValue);
	}
	
	public synchronized static boolean find(String table,String fieldName,String fieldValue){
		Connection conn = null;
		PreparedStatement pstmt = null;
		int count = 0;
		String sql = "SELECT count(*) FROM "+table+" WHERE "+fieldName+"=?";
		try{
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, fieldValue);
			
			ResultSet result = pstmt.executeQuery();
			if(result.next()){
				count = result.getInt(1);
			}
		} catch (Exception e) {
			if(log.isDebugEnabled()) log.debug("Exception="+e.getMessage());
		}finally{

			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if(count > 0)
			return true;
		
		return false;
	}
}
