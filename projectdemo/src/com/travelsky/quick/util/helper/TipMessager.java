package com.travelsky.quick.util.helper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.util.StringUtils;

import com.travelsky.quick.common.ApiContext;

/**
 * 类说明:获取提示消息
 * @author zhangjiabin
 */
public final class TipMessager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TipMessager.class);
	private static MessageSource msgkeyMapsrc = ApiContext.getApplicationContext().getBean("msgkeyMapReloadableSource",MessageSource.class);
	private static MessageSource msgsrc = ApiContext.getApplicationContext().getBean("messageSource",MessageSource.class);
    private static Map<String , String >msgkeysrcMap=new HashMap<String , String>();
    private static Map<String , String >msgsrcMap=new HashMap<String , String>();
    public static Map<String, String> getMsgkeysrcMap() {
		return msgkeysrcMap;
	}
	public static Map<String, String> getMsgsrcMap() {
		return msgsrcMap;
	}
	/**
	 * 根据底层和API的ERROR code获取对外输出的error Message
	 */
	public static String getMessage(String code, String language, Object... args) {
		return getMessage(code, language, null, args);
	}

	/**
	 * 根据底层和API的ERROR code获取对外输出的error Message
	 */
	public static String getMessage(String code, String language, String defaultMessage, Object... args) {
		String rs=null;
		try {
			rs=msgkeysrcMap.get(code);
			if(rs != null && !"".equals(rs)){
				rs=msgsrcMap.get(rs);
				if(rs == null || "".equals(rs)){
					rs = "Unable to process - system error(" + code + ")";
				}else{
					rs = rs + "("+code+")";
				}
			}else{
				rs = "Unable to process - system error(" + code + ")";
			}
		}
		catch (Exception e) {
			LOGGER.error("TipMessager.getMessage.error", e);
			rs = "Unable to process - system error(" + code + ")";
		}
		return rs;
	}
	
	/**
	 * 获取对外输出的error code
	 */
	public static String getErrorCode(String code) {
		String rs=null;
		try {
			rs =msgkeysrcMap.get(code);
			if(rs == null || "".equals(rs)){
				rs = "911";
			}
		}catch (Exception e) {
			LOGGER.error("TipMessager.getErrorCode.error", e);
			rs = "911";
		}
		return rs;
	}
	
	/**
	 * 根据对外输出的ERROR code获取对外输出的error Message
	 */
	public static String getErrorMessage(String errorCode) {
		String rs=null;
		try {
			rs = msgsrcMap.get(errorCode);
			if(rs == null || "".equals(rs)){
				rs = "Unable to process - system error";
			}
		}catch (Exception e) {
			LOGGER.error("TipMessager.getErrorMessage.error", e);
			rs = "Unable to process - system error";
		}
		return rs;
	}
	
	/**
	 * 获取Info级别信息
	 * @param code
	 * @param language
	 * @return
	 */
	public static String getInfoMessage(String code, String language) {
		if (StringUtils.hasLength(code)) {
			String errorCode= getErrorCode(code);
			return new StringBuilder("Message:[")
					.append(getErrorMessage(errorCode))
					.append("] Code:[")
					.append(errorCode)
					.append("]")
					.toString();
		}

		return "Message:[] Code:[]";
	}

	/**
	 * 获取信息
	 * @param code
	 * @param debugInfo
	 * @param language
	 * @return
	 */
	public static String getInfoMessage(String code, String debugInfo, String language) {
		if (StringUtils.hasLength(code)) {
			String errorCode= getErrorCode(code);
			return new StringBuilder("Message:[")
					.append(getErrorMessage(errorCode))
					.append("] Code:[")
					.append(errorCode)
					.append("] Debug:[")
					.append(debugInfo)
					.append("]")
					.toString();
		}

		return "Message:[] Code:[]";
	}
	
	/**
	 * 获取信息(不进行国际化)
	 * @param code
	 * @param message
	 * @return
	 */
	public static String getNoneI18nMessage(String code, String message) {
		String errorCode= getErrorCode(code);
		return new StringBuilder("Message:[")
				.append(getErrorMessage(errorCode))
				.append("] Code:[")
				.append(errorCode)
				.append("]")
				.toString();
	}
}
