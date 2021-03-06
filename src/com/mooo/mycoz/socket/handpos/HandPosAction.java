package com.mooo.mycoz.socket.handpos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mooo.mycoz.common.CalendarUtils;
import com.mooo.mycoz.common.StringUtils;
import com.mooo.mycoz.db.pool.DbConnectionManager;

public class HandPosAction implements Action {
	
	/**
	 * 
	 */
	private static Log log = LogFactory.getLog(HandPosAction.class);

	private static final String QUERY_USER_ID="SELECT id FROM User WHERE name=?";

	private static final String QUERY_BRANCH_ID="SELECT branchId FROM User WHERE id=?";

	private static final String LOGIN="SELECT id,name FROM  User WHERE  name=? AND password=?";

	private static final String EXISTS_CARD="SELECT count(*) FROM Card WHERE uuid=?";
	
	private static final String EXISTS_USER="SELECT count(*) FROM User WHERE name=?";

	private static final String EXIST_USER_CARD="SELECT count(*) FROM User WHERE uuid=?";

	//save user
	private static final String ADD_USER="INSERT INTO User(id,name,password,uuid,mobile,branchId,typeId) VALUES(?,?,?,?,?,?,1)";

	private static final String REGISTER_CARD="SELECT COUNT(id) FROM Card WHERE rfidcode=? AND card.branchId=?";

	private static final String ACTIVATE_CARD="SELECT COUNT(card.id) FROM Card card,CardJob cardJob WHERE cardJob.cardId=card.id AND cardJob.branchId=card.branchId AND cardJob.jobTypeId=2 AND card.rfidcode=? AND card.branchId=?";

	private static final String SELECT_MAX_BY_LIKE="SELECT MAX(rfidcode) nowCode FROM Card WHERE rfidcode LIKE ?";

	private static final String QUERY_CARD="SELECT card.rfidcode,wineJar.abbreviation,wineType.definition,wineLevel.definition,alcohol,volume,volumeUnit,material,card.branchId FROM Card card,WineJar wineJar,wineShared.WineType wineType,wineShared.WineLevel wineLevel WHERE wineJar.id=card.wineJarId AND wineJar.wineTypeId=wineType.id AND wineJar.wineLevelId=wineLevel.id AND card.rfidcode=?";

	private static final String ADD_CARD="INSERT INTO Card(id,rfidcode,uuid,wineryId,branchId,processId,cardTypeId) VALUES(?,?,?,?,?,0,?)";

	private static final String ADD_CARD_JOB="INSERT INTO CardJob(id,jobDate,cardId,userId,jobTypeId,branchId,processId,spotNormal,cardNormal) VALUES(?,?,?,?,?,?,0,'Y','Y')";

	private static final String COUNT_PROCESS="SELECT COUNT(id) FROM CardJob WHERE cardId=? AND branchId=?";

	private static final String UPDATE_CARD_JOB="UPDATE CardJob SET processId=? WHERE cardId=? AND branchId=? AND processId=0";

	private static final String EXISTS_CARD_JOB="SELECT COUNT(id) FROM CardJob WHERE jobTypeId=3  AND processId=0 AND cardId=? AND branchId=? AND userId=? AND jobDate=?";

	//servers call
	
	public int getUserId(String userName){
		Connection conn=null;
        PreparedStatement pstmt = null;
        int userId = -1;
        try {
    		if(StringUtils.isNull(userName)){
    			throw new NullPointerException("??????????????????");
    		}
    		
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(QUERY_USER_ID);
            pstmt.setString(1, userName);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	userId = rs.getInt(1);
            }
		}catch (NullPointerException e) {
			if(log.isErrorEnabled()) log.error("NullPointerException:"+e.getMessage());	
		}catch (SQLException e) {
			if(log.isErrorEnabled()) log.error("SQLException:"+e.getMessage());	
	   }finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
			
		}
		return userId;
	}
	
	public int getBranchId(int userId){
		Connection conn=null;
        PreparedStatement pstmt = null;
        int branchId = -1;
        try {
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(QUERY_BRANCH_ID);
            pstmt.setInt(1, userId);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	branchId = rs.getInt(1);
            }
		}catch (NullPointerException e) {
			if(log.isErrorEnabled()) log.error("NullPointerException:"+e.getMessage());	
		}catch (SQLException e) {
			if(log.isErrorEnabled()) log.error("SQLException:"+e.getMessage());	
	   }finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
			
		}
		return branchId;
	}
	
	public int processAuth(String userName,String password){
		Connection conn=null;
        PreparedStatement pstmt = null;
        int userId=-1;
        
        try {
        	if(StringUtils.isNull(userName)){
    			throw new NullPointerException("??????????????????");
    		}
    		
    		if(StringUtils.isNull(password)){
    			throw new NullPointerException("???????????????");
    		}
    		
			conn = DbConnectionManager.getConnection();
			
			pstmt = conn.prepareStatement(LOGIN);
            pstmt.setString(1, userName);
            pstmt.setString(2, StringUtils.hash(password));
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	userId = rs.getInt(1);
            }
            
		}catch (NullPointerException e) {
			if(log.isErrorEnabled()) log.error("NullPointerException:"+e.getMessage());	
		}catch (SQLException e) {
			if(log.isErrorEnabled()) log.error("SQLException:"+e.getMessage());	
	   }finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
			
		}
		return userId;
	}
	
	public boolean existsPatrol(Integer cardId,Integer branchId,Integer userId,String dateTime){
		Connection conn=null;
        PreparedStatement pstmt = null;
        
        try {
			conn = DbConnectionManager.getConnection();
			
			pstmt = conn.prepareStatement(EXISTS_CARD_JOB);
            pstmt.setInt(1, cardId);
            pstmt.setInt(2, branchId);
            pstmt.setInt(3, userId);
            pstmt.setTimestamp(4, new Timestamp(CalendarUtils.dtparse(dateTime).getTime()));
            
            int count=-1;
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	count = rs.getInt(1);
            }
			
            if(log.isDebugEnabled()) log.debug("count:"+count);

            if(count>0) return true;
            
		}catch (NullPointerException e) {
			if(log.isErrorEnabled()) log.error("NullPointerException:"+e.getMessage());	
		}catch (SQLException e) {
			if(log.isErrorEnabled()) log.error("SQLException:"+e.getMessage());	
		}catch (Exception e) {
			if(log.isErrorEnabled()) log.error("SQLException:"+e.getMessage());	
		}finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
			
		}
		return false;
	}
	
	public int saveCardJob(String rfidcode,String userName,String dateTime) throws SQLException{
		Connection conn=null;
        PreparedStatement pstmt = null;
        int RET=-1;
        try {
			conn = DbConnectionManager.getConnection();
			conn.setAutoCommit(false);
			
			int cardId = IDGenerator.getId(conn,"Card","rfidcode",rfidcode);
			if(cardId<0){
				RET = 1;
				throw new CardException("??????????????????"); 
			}
			
			int userId=getUserId(userName);
			if(userId<0){
				RET = 3;
				throw new CardException("????????????"); 
			}
			int branchId = getBranchId(userId);
			
			pstmt = conn.prepareStatement(ACTIVATE_CARD);
			pstmt.setString(1, rfidcode);
			pstmt.setInt(2, branchId);
			ResultSet rs = pstmt.executeQuery();
            int count = 0;
            while (rs.next()) {
            	count = rs.getInt(1);
            }
            
            if(count<1){
				RET = 2;
            	throw new CardException("??????????????????"); 
            }

			if(existsPatrol(cardId,branchId,userId,dateTime)){
				RET = 4;
				throw new CardException("??????????????????"); 
			}

			if(log.isDebugEnabled()) log.debug("userName:"+userName);
			if(log.isDebugEnabled()) log.debug("userId:"+userId);
			
			pstmt = conn.prepareStatement(COUNT_PROCESS);
			pstmt.setInt(1, cardId);
			pstmt.setInt(2, branchId);
            rs = pstmt.executeQuery();
            int processId = 0;
            while (rs.next()) {
            	processId = rs.getInt(1);
            }
            
			pstmt = conn.prepareStatement(UPDATE_CARD_JOB);
			pstmt.setInt(1, processId);
			pstmt.setInt(2, cardId);
			pstmt.setInt(3, branchId);
			pstmt.execute();
			
			pstmt = conn.prepareStatement(ADD_CARD_JOB);
			int cardJobId = IDGenerator.getNextID(conn,"CardJob");
			
			if(log.isDebugEnabled()) log.debug("cardJobId:"+cardJobId);

			pstmt.setInt(1, cardJobId);
			pstmt.setTimestamp(2, new Timestamp(CalendarUtils.dtparse(dateTime).getTime()));
			pstmt.setInt(3, cardId);
			pstmt.setInt(4, userId);
			pstmt.setInt(5, 3);
			pstmt.setInt(6, branchId);
			pstmt.execute();
			
			conn.commit();
			
			RET=0;
        }catch (CardException e) {
			conn.rollback();
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}catch (Exception e) {
			conn.rollback();
			RET=3;
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
	   }finally {
			conn.setAutoCommit(true);
			
			if(pstmt != null)
				pstmt.close();
			if(conn != null)
				conn.close();
		}
        return RET;
	}
	
	public String synchronize(String ver,String userName,String userPassword,String buffer){
		String response = "";
		Integer mVer=213;
		try{
			mVer = new Integer(ver);
			
			if(mVer==null || mVer<214)
				throw new NullPointerException("*3;???????????????;#;#");//????????????
			
			if(getUserId(userName)<0)
				throw new NullPointerException("*1#");//????????????
			
			int userId = processAuth(userName,userPassword);
			
			if(userId<0)
				throw new NullPointerException("*2#");//??????????????????
			
			String[] record=buffer.split("/");
			String rfid=null;
			int saveCardJob = -1;
			boolean isHead = true;
			
			for(int i=0;i<record.length;i++){
				record[i]=record[i].trim();
				
				String[] parameter = record[i].split(",");
				
				rfid = parameter[0].trim();
				
				saveCardJob = saveCardJob(parameter[1].trim(),parameter[2].trim(),parameter[3].trim());
				
				if(saveCardJob!=0){
					if(isHead){
						isHead=false;
						response += rfid+","+saveCardJob;
					}else{
						response += ";"+rfid+","+saveCardJob;
					}
				}
			}
			
			if(isHead){
				response = "*0#";
			}else{
				response = "*3;"+response+";#;#";
			}
			
		}catch(Exception e){
			response=e.getMessage();
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}
		
		return response;
	}
	
	public String cardPatrol(String userName,String userPassword,String rfidcode) throws SQLException{
		String response = "*";

		Connection conn = null;
		PreparedStatement pstmt = null;
		try{
			conn = DbConnectionManager.getConnection();
			conn.setAutoCommit(false);
            
			int userId=processAuth(userName,userPassword);
			if(userId<0){
				throw new CardException("????????????"); 
			}
			int branchId = getBranchId(userId);
			
			pstmt = conn.prepareStatement(REGISTER_CARD);
			pstmt.setString(1, rfidcode);
			pstmt.setInt(2, branchId);
            ResultSet rs = pstmt.executeQuery();
			int count = 0;
           
			while (rs.next()) {
            	count = rs.getInt(1);
            }
            if(count<1){
            	throw new CardException("??????????????????"); 
            }
            
			pstmt = conn.prepareStatement(ACTIVATE_CARD);
			pstmt.setString(1, rfidcode);
			pstmt.setInt(2, branchId);
            rs = pstmt.executeQuery();
            count = 0;
            while (rs.next()) {
            	count = rs.getInt(1);
            }
            
            if(count<1){
            	throw new CardException("??????????????????"); 
            }
            
			pstmt = conn.prepareStatement(QUERY_CARD);
			pstmt.setString(1, rfidcode);
			
            rs = pstmt.executeQuery();
            String str="";
            while (rs.next()) {
            	str += "?????????:"+rs.getString(1)+",";
            	str += "?????????:"+rs.getString(2)+",";
            	str += "?????????:"+rs.getString(3)+",";
            	str += "?????????:"+rs.getString(4)+",";
            	str += "?????????:"+rs.getString(5)+"%,";
            	str += "??????:"+rs.getString(6)+",";
            	str += "??????:"+rs.getString(7)+",";
            	str += "??????:"+rs.getString(8);            
            }
			
			pstmt = conn.prepareStatement(ADD_CARD_JOB);
			
			int cardJobId = IDGenerator.getNextID(conn,"CardJob");
			if(log.isDebugEnabled()) log.debug("cardJobId:"+cardJobId);

			pstmt.setInt(1, cardJobId);
			pstmt.setTimestamp(2, new Timestamp(Calendar.getInstance().getTimeInMillis()));
			if(log.isDebugEnabled()) log.debug("rfidcode:"+rfidcode);

			int cardId = IDGenerator.getId(conn,"Card","rfidcode",rfidcode);
			if(log.isDebugEnabled()) log.debug("cardId:"+cardId);
			if(cardId<0){
				throw new NullPointerException("???????????????"); 
			}
			
			pstmt.setInt(3, cardId);
			pstmt.setInt(4, processAuth(userName,userPassword));
			pstmt.setInt(5, 3);
			pstmt.setInt(6, branchId);
			pstmt.execute();

			conn.commit();
			
    		response +="0,"+Action.SEARCH_CARD+","+str;

			if(log.isDebugEnabled()) log.debug("save finlsh");
		} catch (Exception e) {
			response +="1,"+e.getMessage();
			conn.rollback();
			
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}finally{
			conn.setAutoCommit(true);
			if(pstmt != null)
				pstmt.close();
			if(conn != null)
				conn.close();
		}
		
		return response += "#";
	}
	
	//Card
	public String saveCard(String userId,String rfidcode,String uuid,String wineryName,String cardTypeName) throws SQLException{
		if(log.isDebugEnabled()) log.debug("save Card start");
		String response = "*";

		Connection conn = null;
		PreparedStatement pstmt = null;
		try{
			conn = DbConnectionManager.getConnection();
			conn.setAutoCommit(false);
			
			pstmt = conn.prepareStatement(ADD_CARD);
			long cardId = IDGenerator.getNextID(conn,"Card");
			pstmt.setLong(1, cardId);
			pstmt.setString(2, rfidcode);
			pstmt.setString(3, uuid);

			int wineryId = IDGenerator.getId("Winery", "enterpriseName", wineryName);
			pstmt.setInt(4, wineryId);
			
			Integer lId=new Integer(userId);
			int branchId = getBranchId(lId);
			pstmt.setInt(5, branchId);
			
			int cardTypeId = IDGenerator.getId("wineShared.CardType", "cardTypeName", cardTypeName);
			pstmt.setInt(6, cardTypeId);
			pstmt.execute();
			
			pstmt = conn.prepareStatement(ADD_CARD_JOB);
			
			int cardJobId = IDGenerator.getNextID(conn,"CardJob");
			
			pstmt.setLong(1, cardJobId);
			pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
			pstmt.setLong(3, cardId);
			pstmt.setLong(4, lId);
			pstmt.setInt(5, 1);
			pstmt.setInt(6, branchId);
			pstmt.execute();
			
			conn.commit();
			if(log.isDebugEnabled()) log.debug("save finlsh");
    		
			response +="0;"+Action.SAVE_CARD;
		} catch (Exception e) {
			conn.rollback();
			response +="1;"+e.getMessage();

			if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
		}finally{
			conn.setAutoCommit(true);
			
			if(pstmt != null)
				pstmt.close();
			if(conn != null)
				conn.close();
		}
		if(log.isDebugEnabled()) log.debug("response finlsh"+response);

		return response += "#";
	}
	
	public String processLogin(String userName,String userPassWord) {
		String response = "*";

		Connection conn=null;
        PreparedStatement pstmt = null;
        int count=0;
        try {

    		if(StringUtils.isNull(userName))
    			throw new NullPointerException("??????????????????");
    		
    		if(StringUtils.isNull(userPassWord))
    			throw new NullPointerException("???????????????");
    		
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(EXISTS_USER);
            pstmt.setString(1, userName);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	count = rs.getInt(1);
            }
            
            if(count < 1)
    			throw new NullPointerException("????????????");
            
            count=0;
            
            pstmt = conn.prepareStatement(LOGIN);
            pstmt.setString(1, userName);
            pstmt.setString(2, userPassWord);

            rs = pstmt.executeQuery();
        	int userId = -1;
            while (rs.next()) {
            	userId = rs.getInt(1);
            }
            
            if(userId < 0)
    			throw new NullPointerException("????????????????????????");
            
            response +="0;"+Action.ACTION_LOGIN+";"+userId;
		}catch (Exception e) {
			response +="1;"+e.getMessage();
			
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
	   }finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
			
		}

        return response += "#";
	}
	
	public String saveUser(String operId,String userName,String userPassWord,String uuid,String mobile) throws SQLException{
		String response = "*";

		Connection conn=null;
        PreparedStatement pstmt = null;
        long count=0;
        try {
			conn = DbConnectionManager.getConnection();
			conn.setAutoCommit(false);
			
			Integer lId=new Integer(operId);
			int branchId = getBranchId(lId);
			
			pstmt = conn.prepareStatement(EXISTS_USER);
            pstmt.setString(1, userName);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	count = rs.getInt(1);
            }
            
    		if(log.isDebugEnabled()) log.debug("count:"+count);	

            if(count > 0) throw new NullPointerException("??????????????????");
            
            pstmt = conn.prepareStatement(EXIST_USER_CARD);
            pstmt.setString(1, uuid);
            
            rs = pstmt.executeQuery();
            while (rs.next()) {
            	count = rs.getInt(1);
            }
    		if(log.isDebugEnabled()) log.debug("count:"+count);	

            if(count > 0) throw new NullPointerException("???????????????");
            
            pstmt = conn.prepareStatement(ADD_USER);
            int userId = IDGenerator.getNextID(conn,"User");
            pstmt.setLong(1, userId);
            pstmt.setString(2, userName);
            pstmt.setString(3, userPassWord);
            pstmt.setString(4, uuid);
            pstmt.setString(5, mobile);
            pstmt.setInt(6, branchId);
            pstmt.execute();

            conn.commit();
            
    		response +="0;"+Action.SAVE_USER;
		}catch (NullPointerException e) {
			conn.rollback();
			response +="1;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("NullPointerException:"+e.getMessage());	
		}catch (SQLException e) {
			conn.rollback();
			response +="2;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("SQLException:"+e.getMessage());	
		}catch (Exception e) {
			conn.rollback();
			response +="3;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("Exception:"+e.getMessage());	
		}finally {
			conn.setAutoCommit(true);

			if(pstmt != null)
				pstmt.close();
			if(conn != null)
				conn.close();
		}
        return response += "#";
	}
	
	public String getKey(String value) {
		
		Connection conn=null;
        PreparedStatement pstmt = null;
        String result = null;
        try {
        	String sql = "SELECT shortName FROM Winery WHERE enterpriseName=?";
        	
			conn = DbConnectionManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, value);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	result = rs.getString(1);
            }
		}catch (SQLException e) {
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
	   }finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
			
		}
        return result;
	}
	
	public String nextRfidCode(String winery) {
		String response = "*";

		String nextCode=null;
		
		String wineryCode=null;
		wineryCode = getKey(winery);
		
		if(log.isDebugEnabled()) log.debug("wineryCode:"+wineryCode);

		if(wineryCode==null || wineryCode.length()>6){
			wineryCode="000000";
		}else if(wineryCode.length()>0 && wineryCode.length()<6){
			for(int i=wineryCode.length();i<6;i++){
				wineryCode +="0";
			}
		}
		
		String nowDate = CalendarUtils.dformat2(Calendar.getInstance().getTime());
		
		if(nowDate==null || nowDate.length()!=6)
			nowDate="000000";
		
		String prefix = wineryCode+nowDate;
		
		String nextNumber = "0001";
		
		Connection conn=null;
        PreparedStatement pstmt = null;
        try {
			conn = DbConnectionManager.getConnection();
            pstmt = conn.prepareStatement(SELECT_MAX_BY_LIKE);
            pstmt.setString(1, prefix+"%");
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
            	nextCode = rs.getString(1);
            }
            
            if(nextCode!=null && nextCode.length()==16){
            	nextNumber = nextCode.substring(12);
            	
            	int number = new Integer(nextNumber);
            	
            	number++;
            	
            	if(number<10){
            		nextNumber = "000"+number;
            	}else if(number<100){
            		nextNumber = "00"+number;
            	}else if(number<1000){
            		nextNumber = "0"+number;
            	}else{
            		nextNumber = ""+number;
            	}
            }
            
            nextCode = prefix+nextNumber;
			
            response +="0;"+Action.NEXT_RFID_CODE+";"+nextCode;
		}catch (Exception e) {
			response +="1;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
	   }finally {
			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
			
		}
        return response += "#";
	}
	
	public String existCard(String  uuid){
		String response = "*";

		Connection conn = null;
		PreparedStatement pstmt = null;

		try{
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(EXISTS_CARD);
			pstmt.setString(1, uuid);
			
			ResultSet result = pstmt.executeQuery();
			int count = 0;
			
			if(result.next()){
				count = result.getInt(1);
			}
			
			if(log.isErrorEnabled()) log.error("count:"+count);	

			if(count > 0) throw new NullPointerException("???????????????");
           
			response +="0;"+Action.EXIST_CARD;
		} catch (Exception e) {
			response +="1;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}finally{

			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
		}
		
        return response += "#";
	}
	
	private static final String SEARCH_WINERYS="SELECT w.enterpriseName FROM Winery w,WineryMap wm WHERE wm.wineryId=w.id AND wm.userId=? ORDER BY w.id";

	public String searchWinerys(String  userId){
		String response = "*";

		Connection conn = null;
		PreparedStatement pstmt = null;

		try{
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(SEARCH_WINERYS);
			pstmt.setInt(1, new Integer(userId));
			
			ResultSet result = pstmt.executeQuery();
			
			String winerys="";
			while(result.next()){
				
				if(winerys.equals(""))
					winerys = ";"+result.getString(1);
				else
					winerys += ","+result.getString(1);
			}

			response +="0;"+Action.SEARCH_WINERYS+winerys;
			
		} catch (Exception e) {
			response +="1;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}finally{

			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
		}
		
        return response += "#";
	}
	
	private static final String SEARCH_CARD_TYPES="SELECT cardTypeName FROM wineShared.CardType ORDER BY id";

	public String searchCardTypes(){
		String response = "*";

		Connection conn = null;
		PreparedStatement pstmt = null;

		try{
			conn = DbConnectionManager.getConnection();
			pstmt = conn.prepareStatement(SEARCH_CARD_TYPES);
			ResultSet result = pstmt.executeQuery();
			
			String cardTypes="";
			while(result.next()){
				
				if(cardTypes.equals(""))
					cardTypes = ";"+result.getString(1);
				else
					cardTypes += ","+result.getString(1);
			}

			response +="0;"+Action.SEARCH_CARD_TYPES+cardTypes;
			
		} catch (Exception e) {
			response +="1;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}finally{

			try {
				if(pstmt != null)
					pstmt.close();
				if(conn != null)
					conn.close();
			} catch (SQLException e) {
				if(log.isErrorEnabled()) log.error("SQLException:" + e.getMessage());
			}
		}
		
        return response += "#";
	}
	
	public String getTime(){
		String response = "*";

		try{
			response +="0;"+Action.GET_TIME+";"+CalendarUtils.dtformat(new Date());
		} catch (Exception e) {
			response +="1;"+e.getMessage();
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}
        return response += "#";
	}
	
	public String forward(String requestLine) {
		String response = null;
		try{
			if(requestLine==null || requestLine.length()<3
					||!requestLine.startsWith("*") ||!requestLine.endsWith("#")){
				return response = "Unsafe";//?????????????????????
			}
			
			String doRequest=requestLine.substring(requestLine.indexOf("*")+1,
					requestLine.lastIndexOf("#"));

		    String[] args=doRequest.split(";");
		    
		    if(log.isDebugEnabled()) log.debug("length:"+args.length);
		    
			for(int i=0;i<args.length;i++){
				args[i]=args[i].trim();
				if(log.isDebugEnabled()) log.debug(args[i]);
			}
			
			int cmd = Integer.parseInt(args[0]);//??????

			 switch(cmd){
				case Action.SEARCH_CARD:
					if(args.length !=4){
						response = "???????????????";
				    }
					
					response = cardPatrol(args[1],args[2],args[3]);
					
					break;
				case Action.ACTION_SYN:
					
					if(args.length !=5){
						response = "???????????????";
				    }
					
					response = synchronize(args[1],args[2],args[3],args[4]);
					break;
				case Action.EXIST_CARD:
					if(args.length !=2){
						response = "???????????????";
				    }
					
					response = existCard(args[1]);
					break;
				case Action.NEXT_RFID_CODE:
					if(args.length !=2){
						response = "???????????????";
				    }
					
					response = nextRfidCode(args[1]);
					break;
				case Action.SAVE_CARD:
					if(args.length !=6){
						response = "???????????????";
				    }
					
					response = saveCard(args[1],args[2],args[3],args[4],args[5]);

					break;
				case Action.SAVE_USER:
					if(args.length !=6){
						response = "???????????????";
				    }
					
					response = saveUser(args[1],args[2],args[3],args[4],args[5]);

					break;
				case Action.SEARCH_CARD_TYPES:
					if(args.length !=1){
						response = "???????????????";
				    }
					
					response = searchCardTypes();

					break;
				case Action.SEARCH_WINERYS:
					if(args.length !=2){
						response = "???????????????";
				    }
					
					response = searchWinerys(args[1]);
					
					break;
				case Action.ACTION_LOGIN:
					if(args.length !=3){
						response = "???????????????";
				    }
					
					response=processLogin(args[1],args[2]);
					
					break;
				case Action.GET_TIME:

					if(args.length !=1){
						response = "???????????????";
				    }
					
					response=getTime();
					
					break;
				default:
					break;
			   }
		}catch(Exception e){
			response = "??????????????????";
			if(log.isErrorEnabled()) log.error("Exception:" + e.getMessage());
		}
		
		return response;
	}
}
