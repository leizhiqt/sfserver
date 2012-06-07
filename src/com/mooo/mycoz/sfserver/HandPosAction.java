package com.mooo.mycoz.sfserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mooo.mycoz.common.StringUtils;
import com.mooo.mycoz.db.pool.DbConnectionManager;

public class HandPosAction implements Action {
	
	/**
	 * 
	 */
	private static Log log = LogFactory.getLog(HandPosAction.class);

	private static final String EXISTS_USER="SELECT count(*) FROM User WHERE name=?";

	private static final String LOGIN="SELECT id,name,branchId FROM  User WHERE  name=? AND password=?";

	private static final String REGISTER_CARD="SELECT COUNT(id) FROM Card WHERE rfidcode=?";

	private static final String EXISTS_CARD="SELECT COUNT(card.id) FROM Card card,WineJar wineJar WHERE wineJar.id=card.wineJarId AND rfidcode=?";

	private static final String QUERY_CARD="SELECT card.rfidcode,wineJar.abbreviation,wineType.definition,wineLevel.definition,alcohol,volume,volumeUnit,material FROM Card card,WineJar wineJar,wineShared.WineType wineType,wineShared.WineLevel wineLevel,cardJob.branchId branchId WHERE wineJar.id=card.wineJarId AND wineJar.wineTypeId=wineType.id AND wineJar.wineLevelId=wineLevel.id AND card.rfidcode=?";

	private static final String ADD_CARD_PATROL_LOG="INSERT INTO CardJob(id,jobDate,cardId,userId,branchId,jobTypeId) VALUES(?,?,?,?,3)";

	public String processLogin(String userName,String password){
		if(log.isDebugEnabled()) log.debug("processLogin");	
		String response = "*";

		Connection connection=null;
        PreparedStatement pstmt = null;
        int count=0;
        try {
    		if(log.isDebugEnabled()) log.debug("processLogin getName:"+userName);	
    		if(log.isDebugEnabled()) log.debug("processLogin getPassword:"+password);	
    		Integer userId = null;

    		if(StringUtils.isNull(userName)){
    			throw new NullPointerException("请输入用户名");
    		}
    		
    		if(StringUtils.isNull(password)){
    			throw new NullPointerException("请输入密码");
    		}
			connection = DbConnectionManager.getConnection();
			pstmt = connection.prepareStatement(EXISTS_USER);
            pstmt.setString(1, userName);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	count = rs.getInt(1);
            }
            
            if(count < 1){
    			throw new NullPointerException("无此用户");
            }
            count=0;
            
            pstmt = connection.prepareStatement(LOGIN);
            pstmt.setString(1, userName);
            pstmt.setString(2, StringUtils.hash(password));

            rs = pstmt.executeQuery();
            while (rs.next()) {
            	userId=rs.getInt(1);

            	count=1;
            }
            
    		if (log.isDebugEnabled()) log.debug("count:"+count);

            if(count !=1){
    			throw new NullPointerException("用户和密码不匹配");
            }
            
    		response +="0,"+Action.PROCESS_LOGIN+","+userId;
		}catch (NullPointerException e) {
			response +="1,"+e.getMessage();
			if(log.isErrorEnabled()) log.error("NullPointerException:"+e.getMessage());	
		}catch (SQLException e) {
			response +="2,"+e.getMessage();
			if(log.isErrorEnabled()) log.error("SQLException:"+e.getMessage());	
	   }finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(connection != null)
					connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		return response += "#";
	}
	
	public String cardPatrol(String rfidcode,Integer userId){
		String response = "*";

		Connection conn = null;
		PreparedStatement pstmt = null;
		try{
			conn = DbConnectionManager.getConnection();
			conn.setAutoCommit(false);
			
			pstmt = conn.prepareStatement(REGISTER_CARD);
			pstmt.setString(1, rfidcode);
            ResultSet rs = pstmt.executeQuery();
            int count = 0;
            while (rs.next()) {
            	count = rs.getInt(1);
            }
            
            if(count<1){
            	throw new NullPointerException("此标签未授权"); 
            }
            
			pstmt = conn.prepareStatement(EXISTS_CARD);
			pstmt.setString(1, rfidcode);
            rs = pstmt.executeQuery();
            count = 0;
            while (rs.next()) {
            	count = rs.getInt(1);
            }
            
            if(count<1){
            	throw new NullPointerException("此标签未激活"); 
            }
            
			pstmt = conn.prepareStatement(QUERY_CARD);
			pstmt.setString(1, rfidcode);
			
            rs = pstmt.executeQuery();
            String str="";
            int branchId=-1;
            while (rs.next()) {
            	str += "标示号:"+rs.getString(1)+",";
            	str += "酒罐号:"+rs.getString(2)+",";
            	str += "酒香型:"+rs.getString(3)+",";
            	str += "酒等级:"+rs.getString(4)+",";
            	str += "酒精度:"+rs.getString(5)+"%,";
            	str += "容积:"+rs.getString(6)+",";
            	str += "单位:"+rs.getString(7)+",";
            	str += "原料:"+rs.getString(8);
            	
            	branchId = rs.getInt(9);
            }
            
			pstmt = conn.prepareStatement(ADD_CARD_PATROL_LOG);
			
			int cardJobId = IDGenerator.getNextID(conn,"CardJob");
			System.out.println("cardJobId:"+cardJobId);

			pstmt.setInt(1, cardJobId);
			pstmt.setTimestamp(2, new Timestamp(Calendar.getInstance().getTimeInMillis()));
			
			System.out.println("rfidcode:"+rfidcode);

			int cardId = IDGenerator.getId(conn,"Card","rfidcode",rfidcode);
			System.out.println("cardId:"+cardId);
			if(cardId<0){
				throw new NullPointerException("无此卡记录"); 
			}
			
			pstmt.setInt(3, cardId);
			
			pstmt.setInt(4, userId);
			
			pstmt.setInt(5, branchId);
			
			pstmt.execute();
			
			conn.commit();
			
    		response +="0,"+Action.SEARCH_CARD+","+str;

			if(log.isDebugEnabled()) log.debug("save finlsh");
		} catch (Exception e) {
			response +="1,"+e.getMessage();

			System.out.println("CardDBObject Exception="+e.getMessage());
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			
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
		
		return response += "#";
	}
	
	public String forward(String requestLine,Integer userId) {
//		String[] args = request.split(" +\n*");
		String response = null;

		try{
			if(!requestLine.startsWith("*")||!requestLine.endsWith("#")){
				response = "数据格式不正确";
			}
			
			String doRequest=requestLine.substring(requestLine.indexOf("*")+1,
					requestLine.lastIndexOf("#"));

		    String[] args=doRequest.split(",");
		    
		    System.out.println("length:"+args.length);
		    
			for(int i=0;i<args.length;i++){
				args[i]=args[i].trim();
				System.out.println(args[i]);
			}
			
			int cmd = Integer.parseInt(args[0]);//命令

			 switch(cmd){
				case Action.PROCESS_LOGIN:
					if(args.length !=3){
						response = "参数不正确";
				    }
					
					response = processLogin(args[1], args[2]);
					break;
				case Action.SEARCH_CARD:
					if(args.length !=2){
						response = "参数不正确";
				    }
					
					if(userId!=null)
						response = cardPatrol(args[1],userId);
					else
						response = "未登录";
					
					break;
				default:
					break;
			   }
		}catch(Exception e){
			response = "数据格式不正确";
		}
		
		return response;
	}
}