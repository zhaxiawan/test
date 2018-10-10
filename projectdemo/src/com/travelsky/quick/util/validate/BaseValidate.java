package com.travelsky.quick.util.validate;

import com.travelsky.quick.exception.APIException;

/**
 * 类说明:验证基础功能接口<br>
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
public interface BaseValidate {
	/**
	 * 检查空
	 * @throws APIException APIException
	 */
	void checkNull() throws APIException ;
	/**
	 * 检查并发
	 * @throws APIException APIException
	 */
	void checkConcurrentNum()  throws APIException ;
	/**
	 * 检查签名
	 * @throws APIException exception
	 */
	void checkSign()
			 throws APIException;
	/**
	 * 检查Service授权
	 * @throws APIException exception
	 */
	void checkServiceAuth()
			throws APIException;
	/**
	 * 检查白名单
	 * @throws APIException exception
	 */
	void checkWhiteList()
			throws APIException;

	/**
	 * 检查黑名单
	 * @throws APIException
	 */
	void checkBlackList()
		throws APIException;

	/**
	 * 检查用户状态
	 * @throws APIException
	 */
	void checkUserStatus()
		throws APIException;
}
