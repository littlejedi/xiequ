package org.jedi.wow;

import java.util.Map;

public interface WechatService {
	
	boolean createRaid(String time, String description) throws Exception;
	
	boolean clearRaid() throws Exception;
			
	boolean joinRaid(String openid, Integer nicknameId) throws Exception;
		
	boolean quitRaid(String openid) throws Exception;
	
	String getNicknameById(Integer id) throws Exception;
	
	Map<Integer, String> getNicknameMap();
	
	RaidInfo viewRaid() throws Exception;

}
