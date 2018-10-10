package com.travelsky.quick.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.FileItem;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import com.cares.sh.comm.BaseContext;
import com.cares.sh.comm.JsonUnit;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.redis.RedisManager;

/**
 *
 * @author Administrator
 *
 */
public class ApiContext extends BaseContext {

	private static final int SHT = 600000;
	private static WebApplicationContext ctx;
	private Map<String, FileItem> formFileMap = new HashMap<String, FileItem>();
	private transient String mOfficeno = "001";
	private transient String channelNO;

	public static final String REQ_PARAM_JSON = "ReqJSON";
	public static final String REQ_PARAM_XML = "ReqXML";
	public static final String REQ_PARAM_SERVICENAME = "ServiceName";
	public static final String REQ_PARAM_TIMESTAMP = "Timestamp";
	public static final String REQ_PARAM_SIGN = "Sign";
	public static final String REQ_PARAM_LANGUAGE = "Language";
	public static final String REQ_PARAM_USERID = "AuthUserID";
	public static final String REQ_PARAM_APPID = "AuthAppID";
	public static final String REQ_PARAM_VERSION = "Version";
	public static final String REQ_PARAM_CLIENTIP = "ClientIP";
	public static final String REQ_PARAM_TKTDEPTID = "AuthTktdeptid";
	public static final String REQ_PARAM_TIMEZONE = "TimeZone";

	// 语言
	private String language;
	// B2C服务器IP. 数组,多个代理会有多个ip,第一个为真实ip地址
	private String[] ipArr;
	private String userID = "";
	private String serviceName;
	private String appID;
	private String timestamp;
	private String reqXML;
	private String sign;
	private String version = "1.0";
	private String ticketDeptid;
	private String timeZone;
	// 客户端IP
	private String clientIP;

	private boolean rtnEncrypt = true;

	public void setRtnEncrypt(boolean rtnEncrypt) {
		this.rtnEncrypt = rtnEncrypt;
	}

	public boolean isRtnEncrypt() {
		return rtnEncrypt;
	}

	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}

	public String getClientIP() {
		return clientIP;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public String getAppID() {
		return appID;
	}

	public void setAppID(String appID) {
		this.appID = appID;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getReqXML() {
		return reqXML;
	}

	public void setReqXML(String reqXML) {
		this.reqXML = reqXML;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setTicketDeptid(String ticketDeptid) {
		this.ticketDeptid = ticketDeptid;
	}

	/**
	 * 设置用户标识
	 * @param userID
	 */
	public void setUserID(String userID) {
		this.userID = userID;
	}

	/**
	 * 获取用户标识
	 * @return
	 */
	public String getUserID() {
		return userID;
	}

	/**
	 * 更新语言
	 * @param language
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * 获取ip地址.多个代理会有多个ip,第一个为真实ip地址
	 * @return
	 */
	public String[] getIPArr() {
		return this.ipArr;
	}
	/**
	 * 获取ip地址的字符串
	 * @return
	 */
	public String getIPArrS() {
		String s="";
		if (this.ipArr!=null) {
			for (String string : this.ipArr) {
				s=s+string+" ";
			}
		}
		return s;
	}
	/**
	 * 设置ip地址.
	 * @param ipArr
	 */
	public void setIPArr(String... ipArr) {
		this.ipArr = ipArr;
	}

	/**
	 * 获取语言
	 * @return
	 */
	public String getLanguage() {
		return !StringUtils.hasLength(language)?
				Locale.getDefault().toString() : language;
	}

	/**
	 * 添加要上传的文件fileItem
	 * @param id
	 * @param fileItem
	 */
	public void addFile(String id, FileItem fileItem) {
		formFileMap.put(id, fileItem);
	}

	/**
	 * 获取要上传的文件
	 * @return
	 */
	public Map<String, FileItem> getFiles() {
		return formFileMap;
	}

	/**
	 * @param ctx WebApplicationContext
	 */
	public static void setApplicationContext(WebApplicationContext ctx) {
		ApiContext.ctx = ctx;
	}

	/**
	 *
	 * @return ctx  WebApplicationContext
	 */
	public static WebApplicationContext getApplicationContext() {
		return ctx;
	}

	/**
	 * @deprecated
	 * Please use getUserID() method
	 */
	@Override
	public String getUser(){
		return getUserID();
	}
	/**
	 * @param user 用户
	 * @deprecated
	 * Please use setUserID(String) method
	 */
	public void setUser(String user){
		setUserID(user);
//		this.mUser = user;
	}
	/**
	 * 工作组
	 * @return 工作组
	 */
	public String getOfficeNo(){
		return mOfficeno;
	}
	/**
	 * 工作组
	 * @param officeno 工作组
	 */
	public void setOfficeNo(String officeno){
		mOfficeno = officeno;
	}

	/**
	 * 渠道编号
	 * @return 渠道编号
	 */
	public String getChannelNo(){
		return channelNO;
	}
	/**
	 *  渠道编号
	 * @param channelno 渠道编号
	 */
	public void setChannelNo(String channelno){
		this.channelNO = channelno;
	}

	/**
	 * 设置渠道编号
	 */
	public void initChannelNo() {
		// 先从缓存中获取部门信息
		String key = RedisNamespaceEnum.api_cache_dept.toKey(new StringBuilder("APIDEPT_").append(ticketDeptid).toString());
		String deptInf = RedisManager.getManager().get(key);
		String channel ="";
		String timeZone = "";
		CommandData deptData = new CommandData();
		boolean callCommand = true;
		// 缓存中存在，则从缓存中获取
		if(StringUtils.hasLength(deptInf)) {
			JsonUnit.fromJson(deptData, deptInf);
			channel=deptData.getParm("channel").getStringColumn();
			timeZone=deptData.getParm("timeZone").getStringColumn();

			callCommand=!StringUtils.hasLength(channel)||!StringUtils.hasLength(timeZone);
		}

		// 缓存中不存在,调底层获取部门信息
		if(callCommand){
			CommandInput l_input = new CommandInput("com.cares.sh.order.dept.show");
			l_input.addParm("code", ticketDeptid);
			CommandRet l_ret = ApiServletHolder.get().doOther(l_input,false);
			
			channel = l_ret.getParm("channel").getStringColumn();
			timeZone = l_ret.getParm("timeZone").getStringColumn();
			
			deptData.addParm("channel", channel);
			deptData.addParm("timeZone", timeZone);

			RedisManager.getManager().set(key, JsonUnit.toJson(deptData), 3);
		}

		if (StringUtils.hasLength(channel)) {
			setChannelNo(channel);
		}
		if (StringUtils.hasLength(timeZone) && getTimeZone() == null) {
			setTimeZone(timeZone);
		}
	}

	@Override
	public int getSessionTimeOut(){
		return SHT;
	}

	@Override
	public String getAppid() {
		// TODO Auto-generated method stub
		return getAppID();
	}

	@Override
	public String getTicketDeptid() {
		// TODO Auto-generated method stub
		return ticketDeptid;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 是否为中文语言环境
	 * @return
	 */
	public boolean isChinese() {
		return StringUtils.hasLength(language)&&Pattern.matches("^(?i:zh)[_]?.*", language);
	}

	@Override
	public String getTimeZone() {
		return timeZone;
	}
}
