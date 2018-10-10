package com.travelsky.quick.util.validate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.SystemConfig;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.util.concurrent.APIConcurrentManager;
import com.travelsky.quick.util.concurrent.ConcurrentManageService.ActionType;
import com.travelsky.quick.util.helper.APICacheHelper;
import com.travelsky.quick.util.helper.APIMessageDigest;
import com.travelsky.quick.util.helper.AppIPListManager;
import com.travelsky.quick.util.helper.ServiceAuthManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 *  类说明:验证基础功能接口<br>
 * 包括
 * <ul>
 * 	<li>空检查</li>
 * <li>并发检查</li>
 * <li>签名检查</li>
 * <li>Service授权检查</li>
 * <li>白名单检查</li>
 * </ul>
 * @author huxizhun
 *
 */
public class BaseValidator extends AbstractBaseValidator {
	static final String MAINSTRING = "|";
	private static final int THAFS = 256;
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseValidator.class);
	protected String userID="";
	protected String appID;
	protected String serviceName;
	protected String timestamp;
	protected String reqXML;
	protected String language;
	protected String sign;
	protected String version;
	// Server IP
	protected String requestIP;
	// Client IP
	protected String clientIP;
	private static Set<String> whiteUserIds;

	/**
	 * 获取系统配置的白名单用户
	 * @return
	 */
	private static Set<String> getWhiteUserIds() {
		if(whiteUserIds==null||whiteUserIds.isEmpty()) {
			// 白名单用户
			String userIds = SystemConfig.getConfig().getItemString("SYSTEM", "WHITES_USERIDS");
			whiteUserIds = new HashSet<>();

			if(StringUtils.hasLength(userIds)) {
				StringTokenizer stokenizer = new StringTokenizer(userIds, ",");
				while(stokenizer.hasMoreTokens()) {
					whiteUserIds.add(stokenizer.nextToken());
				}
			}
		}

		return whiteUserIds;
	}

	/**
	 *
	 * @param context context
	 */
	public BaseValidator(SelvetContext<ApiContext> context) {
		if (context == null) {
			context = ApiServletHolder.get();
		}

		ApiContext apiCtx = context.getContext();
		this.userID = apiCtx.getUserID();
		this.appID = apiCtx.getAppID();
		this.serviceName = apiCtx.getServiceName();
		this.timestamp = apiCtx.getTimestamp();
		this.reqXML = apiCtx.getReqXML();
		this.language = apiCtx.getLanguage();
		this.sign = apiCtx.getSign();
		this.version = apiCtx.getVersion();
		this.clientIP = apiCtx.getClientIP();
		String[] ips = apiCtx.getIPArr();
		// 请求ip数组中的第一个是真实ip
		this.requestIP = ips == null || ips.length < 1? null : ips[0];
	}
	public BaseValidator() {
		this(ApiServletHolder.get());
	}

	/**
	 * @param userID userID
	 * @param appID appID
	 * @param serviceName serviceName
	 * @param timestamp timestamp
	 * @param reqXML reqXML
	 * @param language language
	 * @param sign sign
	 * @param requestIP requestIP
	 * @param version version
	 */
	public BaseValidator(String userID, String appID, String serviceName, String timestamp, String reqXML,
			String language, String sign, String requestIP, String version,String clientip) {
		this.userID = userID;
		this.appID = appID;
		this.serviceName = serviceName;
		this.timestamp = timestamp;
		this.reqXML = reqXML;
		this.language = language;
		this.sign = sign;
		this.requestIP = requestIP;
		this.version = version;
		this.clientIP = clientip;
	}

	/**
	 * 空检查
	 * @throws APIException APIException
	 */
	@Override
	public void checkNull()  throws APIException {
		if (!StringUtils.hasLength(reqXML)||"NULL".equalsIgnoreCase(reqXML)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_REQXML);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REQXML,
					language));
			throw e;
		}
		if (!StringUtils.hasLength(serviceName)||"NULL".equalsIgnoreCase(serviceName)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_SERVICENAME);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_SERVICENAME,
					language));
			throw e;
		}

		if (!StringUtils.hasLength(language)||"NULL".equalsIgnoreCase(language)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_LANGUAGE);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_LANGUAGE,
					language));
			throw e;
		}

		if (!StringUtils.hasLength(appID)||"NULL".equalsIgnoreCase(appID)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_CHANNEL);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_CHANNEL,
					language));
			throw e;
		}

		if (!StringUtils.hasLength(userID)||"NULL".equalsIgnoreCase(userID)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_USER_ID);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_USER_ID,
					language));
			throw e;
		}

		if (!StringUtils.hasLength(timestamp)||"NULL".equalsIgnoreCase(timestamp)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_REQ_TIMESTAMP);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REQ_TIMESTAMP,
					language));
			throw e;
		}

		if (!StringUtils.hasLength(sign)||"NULL".equalsIgnoreCase(sign)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_REQSIGN);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REQSIGN,
					language));
			throw e;
		}

		if (!StringUtils.hasLength(version)||"NULL".equalsIgnoreCase(version)) {
			APIException e = APIException.getInstance(
					ErrCodeConstants.API_NULL_REQVERSION);
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REQVERSION,
					language));
			throw e;
		}
	}

	/**
	 * 检查并发
	 * @throws APIException APIRuntimeException
	 */
	@Override
	public void checkConcurrentNum()
			throws APIException {
		try {
			APIConcurrentManager.getInstance().operate(ActionType.ADD, appID, serviceName, version);
		}
		catch (Exception e) {
			LOGGER.error("CheckConcurrentNum failed! The app id is:{}",appID);
			LOGGER.error("checkConcurrentNum failed!", e);
			String errorCode=e instanceof APIException? ((APIException)e).getErrorCode():
				ErrCodeConstants.API_UNKNNOW_CONCURRNET;

			throw APIException.getInstance(errorCode);
		}
	}

	/**
	 * 检查签名
	 * @throws APIException APIException
	 */
	@Override
	public void checkSign()
			throws APIException {
		String token = getToken(appID);
		//定义一个参数，验证签名错与对
		boolean signBoolean=true;
		boolean signBoolean2=true;
		// 签名串方式1
		StringBuilder signQuery = new StringBuilder(appID)
			.append(MAINSTRING)
			.append(userID)
			.append(MAINSTRING)
			.append(serviceName)
			.append(MAINSTRING)
			.append(language)
			.append(MAINSTRING)
			.append(token)
			.append(MAINSTRING)
			.append(timestamp)
			.append(MAINSTRING);
		try {
			signQuery = signQuery.append(URLEncoder.encode(reqXML, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_CHARACTER,
					language),e);
			signQuery = signQuery.append(reqXML);
		}

		// 验签
		APIMessageDigest msgDigest = APIMessageDigest.getInstance();
		String signStr = StringUtils.hasLength(version)?
						signQuery.append(MAINSTRING).append(version).toString() :
							signQuery.toString();
		 signStr = StringUtils.hasLength(clientIP)?
					signQuery.append(MAINSTRING).append(clientIP).toString() :
						signQuery.toString();
		String realSign = msgDigest.newEncodeSHA(signStr);
		if (!StringUtils.hasLength(realSign) || !realSign.equals(sign)) {
			signBoolean=false;
		}
		if (!signBoolean) {
			// 签名串方式2
	        StringBuilder signQuery2 = new StringBuilder(appID)
						.append(MAINSTRING)
						.append(userID)
						.append(MAINSTRING)
						.append(serviceName)
						.append(MAINSTRING)
						.append(language)
						.append(MAINSTRING)
						.append(token)
						.append(MAINSTRING)
						.append(timestamp)
						.append(MAINSTRING);
					try {
						signQuery2 = signQuery2.append(replaceMethod(new StringBuffer(URLEncoder.encode(reqXML, "UTF-8"))));
					} catch (UnsupportedEncodingException e) {
						LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_CHARACTER,
								language),e);
						signQuery2 = signQuery2.append(reqXML);
					}
					// 验签
					APIMessageDigest msgDigest2 = APIMessageDigest.getInstance();
					String signStr2 = StringUtils.hasLength(version)?
									signQuery2.append(MAINSTRING).append(version).toString() :
										signQuery2.toString();
					 signStr2 = StringUtils.hasLength(clientIP)?
								signQuery2.append(MAINSTRING).append(clientIP).toString() :
									signQuery2.toString();
					String realSign2 = msgDigest2.newEncodeSHA(signStr2);
					if (!StringUtils.hasLength(realSign2) || !realSign2.equals(sign)) {
						signBoolean2=false;
					}
					if (!signBoolean2) {
						String errCode = ErrCodeConstants.API_DATA_SIGN;
						LOGGER.info(TipMessager.getInfoMessage(errCode, language));
						//后加用于查看验签失败的日志
						LOGGER.info("Sign query:{}1"+signStr);
						LOGGER.info("checkSignError:mingwen1"+signStr);
						LOGGER.info("checkSignError:miwen1"+realSign);
						LOGGER.info("Sign query:{}"+signStr2);
						LOGGER.info("checkSignError:mingwen"+signStr2);
						LOGGER.info("checkSignError:miwen"+realSign2);
						LOGGER.info("checkSignError:chuandemiwen"+sign);
						throw APIException.getInstance(errCode);
					}
		}
		
	}
	/**
	 * 将字符串中+号换为%20
	 * @param oldStr
	 * @return
	 */
	 public static String replaceMethod(StringBuffer oldStr) {
	        int newStrLength = 0;
	        int oldStrLength = 0;
	        int spaceNumber = 0;
	        StringBuffer newStr = new StringBuffer();
	        System.out.println(newStr);
	        for(int i = 0; i < oldStr.length(); i++) {
	            oldStrLength++;
	            newStr.append(" ");
	            if(oldStr.charAt(i) == '+') {
	                spaceNumber++;
	                newStr.append("  ");
	            }
	        }
	        newStrLength = oldStrLength + 2 * spaceNumber;
	        int indexOfOldStr = oldStrLength - 1;
	        int indexOfNewStr = newStrLength - 1;
	        for(int j = oldStrLength - 1; j >= 0; j--) {
	            if(oldStr.charAt(j) != '+') {
	                newStr.setCharAt(indexOfNewStr--, oldStr.charAt(indexOfOldStr));
	            } else {
	                newStr.setCharAt(indexOfNewStr--, '0');
	                newStr.setCharAt(indexOfNewStr--, '2');
	                newStr.setCharAt(indexOfNewStr--, '%');
	            }
	            indexOfOldStr--;
	        }

	        return newStr.toString();
	    }
	/**
	 * 获取Token
	 * @param appID appID
	 * @return token
	 * @throws APIException
	 */
	String getToken(String appID)
			 throws APIException {
		return APICacheHelper.getInstance().getToken(appID);
	}

	/**
	 * 检查ServiceName授权
	 * @throws APIException APIException
	 */
	@Override
	public void checkServiceAuth()
		throws APIException {
		//是否从底层服务获取数据
		boolean isLoadCommand = false;
		ServiceAuthManager manager = ServiceAuthManager.getInstance();
		// 从缓存中获取用户授权列表,缓存中不存在或发生错误，则从底层服务中获取授权列表
		Set<String> set = manager.getAuthListFromJredis(userID);
		if ((set == null || set.isEmpty()) || (!set.contains(serviceName) && !set.contains("*"))) {
			isLoadCommand = true;
		}
		// 从底层服务中获取用户授权列表
		if (isLoadCommand) {
			set = manager.getAuthListFromCommand(userID);
			if ((set == null || set.isEmpty()) || (!set.contains(serviceName) && !set.contains("*"))) {
				String errCode = ErrCodeConstants.API_DATA_SERVICE_AUTH;
				LOGGER.info(TipMessager.getInfoMessage(errCode, language));
				throw APIException.getInstance(errCode);
			}
		}
	}

	/**
	 * 检查白名单
	 * @throws APIException APIException
	 */
	@Override
	public void checkWhiteList()
		throws APIException {

		//是否从底层服务获取数据
		boolean isLoadCommand = false;
		AppIPListManager manager = AppIPListManager.getInstance();
		// 从缓存中获取ip列表,缓存中不存在或发生错误，则从底层服务中获取
		Set<String> set = manager.getIPListFromJredis(appID);
		if ((set == null || set.isEmpty()) || (!set.contains(requestIP) && !set.contains("*"))) {
			isLoadCommand = true;
		}
		// 从底层服务中获取ip列表
		if (isLoadCommand) {
			set = manager.getIPListFromCommand(appID);
			if ((set == null || set.isEmpty()) || (!set.contains(requestIP) && !set.contains("*"))) {
				String errCode = ErrCodeConstants.API_DATA_WHITE_LIST;
				LOGGER.info(TipMessager.getInfoMessage(errCode, language));
				throw APIException.getInstance(errCode);
			}
		}
	}

	/**
	 * 检查黑名单
	 */
	@Override
	public void checkBlackList() throws APIException {
		String blackJson = RedisManager.getManager().get("ODS_BACKLISTUSER_LIST");
		if(!validateAbnormalUser(blackJson)) {
			String errCode = ErrCodeConstants.API_CHECK_BLACK_ERROR;
			LOGGER.info(TipMessager.getInfoMessage(errCode, language));
			throw APIException.getInstance(errCode);
		}
	}

	/**
	 * 验证异常用户/黑名单IP
	 * @param json
	 * @return
	 */
	private boolean validateAbnormalUser(String json) {
		Table table = JsonUnit.tableFromJson(json);
		if(null != table && table.getRowCount() >0){
			for (int i = 0; i < table.getRowCount(); i++) {
				String content = table.getRow(i).getColumn("userid_ip").getStringColumn();
				LOGGER.debug("User id is:{}, Client ip is:{}",userID,clientIP);
				if (userID.equals(content) || StringUtils.hasLength(clientIP)&&clientIP.equals(content)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 检查用户状态
	 */
	@Override
	public void checkUserStatus() throws APIException {
		RedisManager redis = RedisManager.getManager();
		String userStatusStr = redis.get("COMMON_USER_STATUS");
		// 检查用户状态
		if(StringUtils.hasLength(userStatusStr)) {
			Table userStatusTable = JsonUnit.tableFromJson(userStatusStr);
			int usTableSize = userStatusTable==null? 0 : userStatusTable.getRowCount();

			// 如果用户id没在白名单中
			if(!getWhiteUserIds().contains(userID)) {
				for (int i=0;i<usTableSize;i++) {
					Row usRow = userStatusTable.getRow(i);
					String userId=usRow.getColumn("userid").getStringColumn();
					String status = usRow.getColumn("status").getStringColumn();

					if(userID.equals(userId) && "n".equalsIgnoreCase(status)) {
						String errCode = ErrCodeConstants.API_USER_DISABLED_ERROR;
						LOGGER.info(TipMessager.getInfoMessage(errCode, language));
						throw APIException.getInstance(errCode);
					}
				}
			}
		}

		// 检查是否异常用户
		String abnormalJson = RedisManager.getManager().get("ODS_ABNORMALUSER_LIST");
		if(!validateAbnormalUser(abnormalJson)) {
			String errCode = ErrCodeConstants.API_CHECK_BLACK_ERROR;
			LOGGER.info(TipMessager.getInfoMessage(errCode, language));
			throw APIException.getInstance(errCode);
		}
	}
}
