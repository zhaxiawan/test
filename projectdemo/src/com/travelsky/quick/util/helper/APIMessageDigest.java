package com.travelsky.quick.util.helper;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.exception.APIException;

/**
 * 类说明:加密工具类
 * @author huxizhun
 *
 */
public class APIMessageDigest {
	private static final Logger LOGGER = LoggerFactory.getLogger(APIMessageDigest.class);
	private static final APIMessageDigest MSGDIGEST = new APIMessageDigest();
	private static final int ONE = 1;
	private static final int THAFF = 0xFF;
	/**
	 * @return 
	 * APIMessageDigest    返回类型 
	 *
	 */
	public static APIMessageDigest getInstance() {
		return MSGDIGEST;
	}
	/**
	 * SHA-256 加密操作
	 *
	 * @param str
	 *            要加密的字符串
	 * @return 加密后的字符串
	 */
	public String newEncodeSHA(String str) {
		String string = "";
		try {
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(ParmExUtil.getDesKey().getBytes("utf-8"), "HmacSHA256");
			sha256_HMAC.init(secret_key);
			byte[] s53 = sha256_HMAC.doFinal(str.getBytes("utf-8"));
			string= Base64.encodeBase64String(s53);
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_DATA_SHA, 
					ApiServletHolder.getApiContext().getLanguage()),e);
		}
		return string;
	}
	/**
	 * sha加密
	 * @param str String
	 * @param digits int
	 * @throws APIException APIException
	 * @return String
	 */
	public String sha(String str, int digits) throws APIException {
		String sha = "SHA-" + digits;
		MessageDigest msgDigest = null;
		String result = null;
		try {
			msgDigest = MessageDigest.getInstance(sha);
		} catch (NoSuchAlgorithmException e) {
			try {
				msgDigest = MessageDigest.getInstance("SHA-256");
			} catch (Exception e1) {
				LOGGER.error(TipMessager.getInfoMessage(
						ErrCodeConstants.API_DATA_SHA, 
						ApiServletHolder.getApiContext().getLanguage()),e1);
				
			}
		}
		
		if (msgDigest != null) {
			byte[] msg = null;
			try {
				msg = str.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				LOGGER.error(TipMessager.getInfoMessage
						(ErrCodeConstants.API_CHARACTER, 
						ApiServletHolder.getApiContext().getLanguage()),e);
				throw new APIException(ErrCodeConstants.API_CHARACTER,e);
			}
			msgDigest.update(msg);
			result = bytes2Hex(msgDigest.digest());
		}
		return result;
	}
	
	/**
	 *  
	 * @param bts byte[]
	 * @return String
	 */
	private String bytes2Hex(byte[] bts) {
		StringBuffer des = new StringBuffer();
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & THAFF));
            if (tmp.length() == ONE) {
            	des.append("0");
            }
            des.append(tmp);
        }
        return des.toString();
    }
}
