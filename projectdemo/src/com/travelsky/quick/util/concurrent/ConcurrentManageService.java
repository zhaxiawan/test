package com.travelsky.quick.util.concurrent;

import com.travelsky.quick.exception.APIException;

/**
 * 类说明:处理并发管理的接口
 * @author huxizhun
 */
public interface ConcurrentManageService {
	/**
	 *
	 * @author Administrator
	 *
	 */
	public static enum ActionType  {
		// 并发数增加
		ADD,
		// 并发数减少
		SUB
	}

	/**
	 * 并发处理
	 * @param actionType 操作类型
	 * @param keys 关键字
	 * @throws APIException 运行时异常
	 * @throws Exception  异常
	 */
	 void operate(
			ActionType actionType, String... keys) throws APIException, Exception;
	/**
	 * 验证并发
	 * @param keys 关键字
	 * @throws APIException 运行时异常
	 * @throws Exception
	 */
	 void validate(String... keys)
			throws APIException, Exception;

}
