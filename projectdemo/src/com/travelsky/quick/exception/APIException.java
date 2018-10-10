package com.travelsky.quick.exception;


/**
 * 类说明:API RuntimeException异常类.类中定义了错误消息和错误码.<br>
 * 错误码请参考<code>ErrCodeConstants</code>
 * @see com.travelsky.quick.common.util.ErrCodeConstants
 * @author huxizhun
 *
 */
public class APIException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3430165021041972042L;
	
	private transient String errMessage;
	private transient String errorCode;

	

	
	/**
	 * 根据错误码初始化APIRuntimeException
	 * @param errorCode 错误码
	 * @return APIRuntimeException
	 */
	public static APIException getInstance(String errorCode) {
		return new APIException(errorCode);
	}
	
	/**
	 * 根据错误码和异常信息初始化APIRuntimeException
	 * @param errorCode 错误码
	 * @param cause 异常信息
	 * @return APIRuntimeException
	 */
	public static APIException getInstance(String errorCode, Throwable cause) {
		return new APIException(errorCode, cause);
	}
	
	/**
	 * 根据错误消息、错误码和异常信息初始化APIRuntimeException
	 * @param message 错误消息
	 * @param errorCode 错误码
	 * @param cause 异常信息
	 * @return APIRuntimeException
	 */
	public static APIException getInstance(String message, String errorCode, Throwable cause) {
		return new APIException(message, errorCode, cause);
	}
	
	/**
	 * 根据错误消息和错误码初始化APIRuntimeException
	 * @param message String
	 * @param errorCode String
	 * @return APIRuntimeException
	 */
	public static APIException getInstance(String message, String errorCode) {
		return new APIException(message, errorCode);
	}
	

	/**
	 * @return String 错误消息
	 */
	public String getErrMessage() {
		return errMessage;
	}

	/**
	 * @return String 错误码
	 */
	public String getErrorCode() {
		return errorCode;
	}


	/**
	 * 根据错误码构造APIRuntimeException
	 * @param errorCode String
	 */
	public APIException(String errorCode) {
		super(new StringBuffer("[API] Message:[] Code:[")
				.append(errorCode)
				.append("]")
				.toString(), new APIException());
		this.errorCode = errorCode;
	}

	/**
	 * 根据错误码和异常信息构造APIRuntimeException
	 * @param errorCode 错误码
	 * @param cause 异常信息
	 */
	public APIException(String errorCode, Throwable cause) {
		super(errorCode, cause);
		this.errorCode = errorCode;
		// TODO Auto-generated constructor stub
	}
	/**
	 * 根据错误消息、错误码和异常信息构造APIRuntimeException
	 * @param message String
	 * @param errorCode String
	 * @param cause Throwable
	 */
	public APIException(String message, String errorCode, Throwable cause) {
		super(new StringBuffer("[API] Message:[")
				.append(message)
				.append("] Code:[")
				.append(errorCode)
				.append("]")
				.toString(), cause);
		this.errMessage = message;
		this.errorCode = errorCode;
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * 根据错误消息和错误码构造APIRuntimeException
	 * @param message 错误消息
	 * @param errorCode 错误码
	 */
	public APIException(String message, String errorCode) {
		super(new StringBuffer("[API] Message:[")
				.append(message)
				.append("] Code:[")
				.append(errorCode)
				.append("]")
				.toString(), new APIException());
		this.errMessage = message;
		this.errorCode = errorCode;
	}
	
	/**
	 * 构造方法
	 */
	private APIException() {
		super();
	}

	
}
