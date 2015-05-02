package org.jedi.wow;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class Controller extends HttpServlet{
	
    private static final long serialVersionUID = 1L;
    private static final String TOKEN = "blackmamba";
    private static final String UNSUBSCRIBE_REPLY = "你竟然退订！准备被谢区切桑活了吗？";
    private static final String NO_RAID_REPLY = "啊欧，下次活动时间还没确定，请稍后再试~";
    private static final String CLEAR_RAID_SUCCESS_REPLY = "你已成功清除活动";
    private static final String CREATE_RAID_SUCCESS_REPLY = "你已成功创建活动！新活动：";
    private static final String JOIN_RAID_SUCCESS_REPLY = "你已成功报名此次活动！如需退出请按2";
    private static final String QUIT_RAID_SUCCESS_REPLY = "系统已接受你的放鸽子请求。你又一次辜负了队友们！这样真的好吗？";
    private static final String QUIT_RAID_NOT_EXIST_REPLY = "由于系统尚未完善，请您先根据您的身份数字输入你的身份，然后再输入2退出活动。不好意思啦！（如果不知道自己的身份ID，请在聊天窗口输入 0）";
    
    // /raid\s+.\S+\s*.\S+
    private static final Pattern RAID_P = Pattern.compile("/raid\\s+.\\S+\\s*.\\S+");
    private static final Pattern RAID_CLEAR_P = Pattern.compile("/raid\\sclear");
    private static final Pattern INT_P = Pattern.compile("[0-9]+");
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	private WechatService wechatService = new WechatServiceBean();
	
	private String message;

	  public void init() throws ServletException
	  {
	      // Do required initialization
	      message = "Hello World";
	  }

	  /*public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	  {
	      // Set response content type
	      response.setContentType("text/html");

	      // Actual logic goes here.
	      PrintWriter out = response.getWriter();
	      out.println("<h1>" + message + "</h1>");
	  }*/
	  
	  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	  {
		  String echostr = request.getParameter("echostr");
		  response.setContentType("text/html; charset=UTF-8");
			PrintWriter out = response.getWriter();
			if (checkWeixinReques(request) && echostr != null) {
				out.write(echostr);
				out.close();
			}else{
				out.write("error");
			}
	  }
	  
	  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	  {
		//处理微信服务端发的请求
			Map<String, String> requestMap = WechatUtils.parseXml(request);
			Message message = WechatUtils.mapToMessage(requestMap);
			//weixinService.addMessage(message);//保存接受消息到数据库
			String replyContent = getMenuDescription();
			final String type = message.getMsgType();
			final String event = message.getEvent();
			final String openid = message.getFromUserName();
			final String content = message.getContent();
			if (Message.TEXT.equals(type)) {
				// Check if content is only a number
				Matcher m = INT_P.matcher(content);
				if (m.matches()) {
					int numOfDigits = content.length();
					if (numOfDigits == 1) {
						if (Message.TEXT_VIEW_RAID.equals(content)) {
							replyContent = doViewRaid();
						} else if (Message.TEXT_QUIT_RAID.equals(content)) {
							replyContent = doQuitRaid(openid);
						}
					} else if (numOfDigits == 2) {
						// 用户输入了昵称ID
						replyContent = doJoinRaid(openid, Integer.parseInt(content));
					}
				}
				else {
					if (isRaidClearMessage(content)) {
						replyContent = doClearRaid();
					} else if (isCreateRaidMessage(content)) {
						replyContent = doCreateRaid(content);
					}
				}
			} else if (Message.EVENT.equals(type)) {
				if (Message.EVENT_SUBSCRIBE.equals(event)) {
					//处理被订阅消息
					replyContent = getWelcomeMessage();
				} else if (Message.EVENT_UNSUBSCRIBE.equals(event)) {
					//处理被取消订阅消息
					replyContent = UNSUBSCRIBE_REPLY;
				}
			} 
			//拼装回复消息
			Reply reply = new Reply();
			reply.setToUserName(message.getFromUserName());
			reply.setFromUserName(message.getToUserName());
			reply.setCreateTime(new Date());
			reply.setMsgType(Reply.TEXT);
			reply.setContent(replyContent);
			//weixinService.addReply(reply);//保存回复消息到数据库
			//将回复消息序列化为xml形式
			String back = WechatUtils.replyToXml(reply);
			response.setContentType("text/html; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.write(back);
			out.close();
	  }
	  
	  public void destroy()
	  {
	      // do nothing.
	  }
	  
	  public static String getWelcomeMessage() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("感谢你加入山口山集团 黑手是阻碍团队的第一大障碍！请回复数字选择服务：").append("\n");  
			buffer.append("0. 查看自己的身份ID").append("\n\n");
			buffer.append("1. 查看活动信息").append("\n");  
			return buffer.toString();
		}
		
		public String getMenuDescription() {
			StringBuffer buffer = new StringBuffer();
			Map<Integer, String> nMap = wechatService.getNicknameMap();
			buffer.append("感谢你加入山口山集团 黑手是阻碍团队的第一大障碍！请回复数字选择服务：").append("\n");  
			buffer.append("团队成员的身份ID").append("\n\n");
			for (Integer key: nMap.keySet()) {
				String nickname = nMap.get(key);
				buffer.append(key).append(".  ").append(nickname).append("\n");
			}
			/*buffer.append("10. P总").append("\n");  
			buffer.append("11. 钱姐").append("\n");  
			buffer.append("12. 小鸡").append("\n");
			buffer.append("13. 兔子").append("\n");
			buffer.append("14. L总").append("\n");
			buffer.append("15. 肥总").append("\n");
			buffer.append("16. 阿帆").append("\n");
			buffer.append("17. 皮总").append("\n");
			buffer.append("18. Zoey").append("\n");
			buffer.append("19. Im Groot").append("\n");
			buffer.append("20. cloudy").append("\n");
			buffer.append("21. 侃哥").append("\n");
			buffer.append("22. 阿强").append("\n");
			buffer.append("23. 艾文Kuang").append("\n");
			buffer.append("24. 戴总").append("\n");
			buffer.append("25. Morpheus").append("\n\n");*/
			buffer.append("\n");
			buffer.append("如需报名活动，请根据上面输入对应自己名字的数字，例如：如果你是钱姐，即输入11").append("\n");
			buffer.append("如果已经报名，需要退出，请输入2").append("\n").append("\n");
			buffer.append("如需查看活动信息，请输入1").append("\n");
			return buffer.toString();
		}
				
		/**
		 * 根据token计算signature验证是否为weixin服务端发送的消息
		 */
		private boolean checkWeixinReques(HttpServletRequest request){
			String signature = request.getParameter("signature");
			String timestamp = request.getParameter("timestamp");
			String nonce = request.getParameter("nonce");
			if (signature != null && timestamp != null && nonce != null ) {
				String[] strSet = new String[] { TOKEN, timestamp, nonce };
				java.util.Arrays.sort(strSet);
				String key = "";
				for (String string : strSet) {
					key = key + string;
				}
				String pwd = WechatUtils.sha1(key);
				return pwd.equals(signature);
			}else {
				return false;
			}
		}
		
		private String doJoinRaid(String openid, Integer nicknameId) {
			try {
				boolean result = wechatService.joinRaid(openid, nicknameId);
				if (!result) {
					return NO_RAID_REPLY;
				}
				return wechatService.getNicknameById(nicknameId) + ": " + JOIN_RAID_SUCCESS_REPLY;
			} catch (Exception e) {
				LOGGER.error("Error occured joining raid for openid=" + openid, e);
				return ExceptionUtils.getStackTrace(e);
				//return getMenuDescription();
			}
		}
		
		private String doQuitRaid(String openid) {
			try {
				boolean result = wechatService.quitRaid(openid);
				if (result) {
					return QUIT_RAID_SUCCESS_REPLY;
				} else {
					return QUIT_RAID_NOT_EXIST_REPLY;
				}
			} catch (Exception e) {
				LOGGER.error("Error occured quitting raid for openid=" + openid, e);
				return ExceptionUtils.getStackTrace(e);
				//return getMenuDescription();
			}
		}
		
		private String doViewRaid() {
			try {
				RaidInfo raidInfo = wechatService.viewRaid();
				if (raidInfo == null || raidInfo.getTime() == null) {
					return NO_RAID_REPLY;
				}
				List<RaidMember> members = raidInfo.getMembers();
				int going = 0;
				int notgoing = 0;
				StringBuffer buffer = new StringBuffer();
				StringBuffer notGoingBuffer = new StringBuffer();
				buffer.append("本次活动信息").append("\n");
				buffer.append(Strings.nullToEmpty(raidInfo.getTime())).append("\n");
				buffer.append(Strings.nullToEmpty(raidInfo.getDescription())).append("\n");
				buffer.append("Raid指挥：").append(raidInfo.getRaidLeader()).append("\n");
				buffer.append("参加本次活动伙伴").append("\n");
				notGoingBuffer.append("\n").append("缺席本次活动伙伴").append("\n");
				if (members != null) {
					for (RaidMember m : members) {
						if (m.isGoingToRaid()) {
							buffer.append(m.getNickname()).append("\n");
							going++;
						} else {
							notGoingBuffer.append(m.getNickname()).append("\n");
							notgoing++;
						}				
					}
				}
				buffer.append(notGoingBuffer.toString());
				buffer.append("确定参加人数：").append(going).append("\n");
				buffer.append("确定缺席人数：").append(notgoing).append("\n").append("\n");
				buffer.append("团队成员身份ID").append("\n");
				Map<Integer, String> nMap = wechatService.getNicknameMap();
				for (Integer key: nMap.keySet()) {
					String nickname = nMap.get(key);
					buffer.append(key).append(".  ").append(nickname).append("\n");
				}
				buffer.append("\n");
				buffer.append("如需报名活动，请根据上面输入对应自己名字的数字，例如：如果你是钱姐，即输入11").append("\n");
				buffer.append("如果已经报名，需要退出，请输入2").append("\n");
				return buffer.toString();
			} catch (Exception e) {
				LOGGER.error("Error occured viewing raid", e);
				return ExceptionUtils.getStackTrace(e);
				//return getMenuDescription();
			}
		}
		
		private String doCreateRaid(String content) {
			try {
			    String[] strings = content.split("\\s+");
			    if (strings.length == 2) {
					wechatService.createRaid(strings[1], null);
					return CREATE_RAID_SUCCESS_REPLY + strings[1];
				} else if (strings.length == 3) {
					wechatService.createRaid(strings[1], strings[2]);
					return CREATE_RAID_SUCCESS_REPLY + strings[1] + " " + Strings.nullToEmpty(strings[2]);
				}
				return getMenuDescription();			
			} catch (Exception e) {
				LOGGER.error("Error occured creating raid, content=" + content, e);
				return ExceptionUtils.getStackTrace(e);
				//return getMenuDescription();
			}
		}
		
		private String doClearRaid() {
			try {
				wechatService.clearRaid();
			} catch (Exception e) {
				LOGGER.error("Error occured clearing raid", e);
				return ExceptionUtils.getStackTrace(e);
				//return getMenuDescription();
			}
			return CLEAR_RAID_SUCCESS_REPLY;
		}
		
		private boolean isRaidClearMessage(String content) {
			Matcher m = RAID_CLEAR_P.matcher(content);
			return m.matches();
		}
		
		private boolean isCreateRaidMessage(String content) {
			Matcher m = RAID_P.matcher(content);
			return m.matches();
		}
}
