package org.jedi.xiequ;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WechatServiceBean implements WechatService {
	
	private Logger LOGGER = LoggerFactory.getLogger(WechatServiceBean.class);
	
	private static final String APP_ID = "wxf173b84a3a7478da";
	
	private static final String APP_SECRET = "6e4241d6da5c9774c605293dbaef509b";
	
	private static final Map<Integer, String> nicknameMap;
    static {
        Map<Integer, String> nMap = new LinkedHashMap<Integer, String>();
        nMap.put(10, "木木");
        nMap.put(11, "小周");
        nMap.put(12, "谢喆");
        nMap.put(13, "朱师傅");
        nMap.put(14, "小马");
        nMap.put(15, "琴琴");
        nMap.put(16, "小锅");
        nicknameMap = Collections.unmodifiableMap(nMap);
    }
	
	private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
    private String raidInfoPath = "/wechat/raid/raid.json";
	
	@Override
    public boolean joinRaid(String openid, Integer nicknameId) throws Exception {
	    RaidInfo raidInfo = readRaidInfo();
	    if (raidInfo == null) {
	    	raidInfo = new RaidInfo();
	    }
	    if (raidInfo.getTime() == null) {
	    	return false;
	    }
	    updateRaidInfo(raidInfo, openid, nicknameId, true);
	    return true;
    }
	
	@Override
    public boolean clearRaid() throws Exception {
		RaidInfo raidInfo = readRaidInfo();
		if (raidInfo == null) {
			raidInfo = new RaidInfo();;
		} else {
			raidInfo.setTime(null);
			raidInfo.setDescription(null);
			raidInfo.setMembers(new ArrayList<RaidMember>());
		}
		writeRaidInfo(raidInfo);
	    return true;
    }
	
	@Override
    public boolean createRaid(String time, String description) throws Exception {
		RaidInfo raidInfo = readRaidInfo();
		if (raidInfo == null) {
			raidInfo = new RaidInfo();;
		}
		if (time != null) {
			raidInfo.setTime(time);
		}
		if (description != null) {
			raidInfo.setDescription(description);
		}
		writeRaidInfo(raidInfo);
	    return true;
    }

	@Override
    public boolean quitRaid(String openid) throws Exception {
		RaidInfo raidInfo = readRaidInfo();
		if (raidInfo == null) {
			return false;
	    }
    	if (!this.isRaidMemberInTheRaid(openid, raidInfo)) {
    		return false;
    	}
		updateRaidInfo(raidInfo, openid, null, false);
	    return true;
    }

	@Override
    public RaidInfo viewRaid() throws Exception {
	    return readRaidInfo();
    }
	
	@Override
    public String getNicknameById(Integer id) throws Exception {
	    if (!nicknameMap.containsKey(id)) {
	    	throw new Exception("Nickname id does not exist in the map. Id=" + id);
	    }
	    return nicknameMap.get(id);
    }
	
	@Override
    public Map<Integer, String> getNicknameMap() {
	    return nicknameMap;
    }
		
	private RaidInfo readRaidInfo() throws Exception {
		return this.readRaidInfoFromSql();
		/*File f = null;
	    try {
		    f = new File(raidInfoPath);
		    if (!f.exists()) {
		    	return null;
		    } else {
			    final String json = FileUtils.readFileToString(f, "UTF-8");
			    final RaidInfo raidInfo = OBJECT_MAPPER.readValue(json, RaidInfo.class);
			    return raidInfo;
		    }
        } catch (IOException e) {
	        LOGGER.error("An exception occured reading raid info from file", e);
	        return null;
        }*/
	}
	
	private void writeRaidInfo(RaidInfo raidInfo) throws Exception {
		this.updateRaidInfoFromSql(raidInfo);
		/*final String raidInfoJSON = OBJECT_MAPPER.writeValueAsString(raidInfo);
		File f = null;
	    try {
		    f = new File(raidInfoPath);
		    FileUtils.writeStringToFile(f, raidInfoJSON, "UTF-8");
        } catch (IOException e) {
	        LOGGER.error("An exception occured writing content to file, raidInfo={}", raidInfoJSON);
	        throw e;
        }*/
	}
	
	private void updateRaidInfo(RaidInfo raidInfo, String openid, Integer nicknameId, boolean going) throws Exception {
		if (raidInfo.getMembers() == null) {
	    	raidInfo.setMembers(new ArrayList<RaidMember>());
	    	raidInfo.getMembers().add(getRaidMember(openid, nicknameId, going));
	    } else {
	    	// look for member with same openid
	    	boolean found = false;
		    List<RaidMember> members = raidInfo.getMembers();
	    	for (RaidMember m : members) {
	    		if (m.getOpenid().equals(openid)) {
	    			m.setGoingToRaid(going);
	    			if (nicknameId != null) {
	    				m.setNickname(getNicknameById(nicknameId));
	    			}
	    			found = true;
	    			break;
	    		}
	    	}
	    	if (!found) {
	    		members.add(getRaidMember(openid, nicknameId, going));
	    	}
	    } 
	    writeRaidInfo(raidInfo);
	}
	
	private RaidMember getRaidMember(String openid, Integer nicknameId, boolean going) throws Exception {
		RaidMember m = new RaidMember();
    	m.setGoingToRaid(going);
    	m.setOpenid(openid);
    	if (nicknameId != null) {
    		m.setNickname(getNicknameById(nicknameId));
    	}
    	return m;
	}
	
	private boolean isRaidMemberInTheRaid(String openid, RaidInfo raidInfo) {
	    List<RaidMember> members = raidInfo.getMembers();
    	for (RaidMember m : members) {
    		if (m.getOpenid().equals(openid)) {
    			return true;
    		}
    	}
    	return false;
	}
	
	private RaidInfo readRaidInfoFromSql() throws Exception {
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = null;
		try {
			/***** 1. 填写数据库相关信息(请查找数据库详情页) *****/
			String databaseName = "KqfydhTDJckkxmWVYgUL";
			String host = "sqld.duapp.com";
			String port = "4050";
			String username = "Oetaio9Vxpy6ysdVnMzszN1n";// 用户名(api key);
			String password = "NtAqZfitZykhLI525V9qdTQladZvmQLt";// 密码(secret
																 // key)
			String driverName = "com.mysql.jdbc.Driver";
			String dbUrl = "jdbc:mysql://";
			String serverName = host + ":" + port + "/";
			String connName = dbUrl + serverName + databaseName;

			/****** 2. 接着连接并选择数据库名为databaseName的服务器 ******/
			Class.forName(driverName);
			connection = DriverManager.getConnection(connName, username,
			        password);
			stmt = connection.createStatement();
			/* 至此连接已完全建立，就可对当前数据库进行相应的操作了 */
			/* 3. 接下来就可以使用其它标准mysql函数操作进行数据库操作 */
			// 创建一个数据库表
			rs = stmt.executeQuery("SELECT * FROM wow WHERE ID = 1");
			rs.next();
			final String raidInfoJSON = rs.getString("Data");
			final RaidInfo raidInfo = OBJECT_MAPPER.readValue(raidInfoJSON, RaidInfo.class);
		    return raidInfo;
		} catch (Exception e) {
			throw e;
		}
	}
	
	private void updateRaidInfoFromSql(RaidInfo raidInfo) throws Exception {
		final String raidInfoJSON = OBJECT_MAPPER.writeValueAsString(raidInfo);
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = null;
		try {
			/***** 1. 填写数据库相关信息(请查找数据库详情页) *****/
			String databaseName = "KqfydhTDJckkxmWVYgUL";
			String host = "sqld.duapp.com";
			String port = "4050";
			String username = "Oetaio9Vxpy6ysdVnMzszN1n";// 用户名(api key);
			String password = "NtAqZfitZykhLI525V9qdTQladZvmQLt";// 密码(secret
																 // key)
			String driverName = "com.mysql.jdbc.Driver";
			String dbUrl = "jdbc:mysql://";
			String serverName = host + ":" + port + "/";
			String connName = dbUrl + serverName + databaseName;

			/****** 2. 接着连接并选择数据库名为databaseName的服务器 ******/
			Class.forName(driverName);
			connection = DriverManager.getConnection(connName, username,
			        password);
			stmt = connection.createStatement();
			/* 至此连接已完全建立，就可对当前数据库进行相应的操作了 */
			/* 3. 接下来就可以使用其它标准mysql函数操作进行数据库操作 */
			// 创建一个数据库表
			int count = stmt.executeUpdate("UPDATE wow SET Data = '" + raidInfoJSON + "' WHERE ID = 1");
		} catch (Exception e) {
			throw e;
		}
	}
}
