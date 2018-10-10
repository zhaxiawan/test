package com.travelsky.quick.util.helper;

import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.exception.APIException;

public class AppConcurrentManager {
	/**
	 * 获取最大并发数
	 * @param appID
	 * @param version
	 * @return
	 * @throws APIException
	 */
	String getMaxNum(String appID, String version) throws APIException {
		String result = null;
		if (!StringUtils.hasLength(appID)) {
			return result;
		}

		// 从Redis中获取
		String key = new StringBuffer(RedisNamespaceEnum.api_cache_appmax.code()+":")
				.append(appID)
				.append(version)
				.toString();
		RedisManager redisManager = RedisManager.getManager();
		result = redisManager.get(key);
		// Redis中不存在从服务中取
		if (!StringUtils.hasLength(result)) {
			result = doOther(appID);

			if (StringUtils.hasLength(result)) {
				redisManager.set(key, result, APICacheHelper.CACHE_TIMEOUT);
			}
		}

		return result;
	}

	/**
	 * 获取App当前并发数
	 * @param appID
	 * @param version
	 * @return
	 */
	String getAppCurConcurrent(String appID, String version) {
		String curNum = null;
		if (StringUtils.hasLength(appID)) {

			String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":").append(appID).append(version).toString();
			RedisManager redisManager = RedisManager.getManager();
			curNum = redisManager.get(key);

			if (!StringUtils.hasLength(curNum)) {
				return "0";
			}
		}

		return curNum;
	}

	/**
	 * App 并发数加1
	 * @param appId
	 * @param version
	 */
	void incrAppConcurrent(String appId, String version) {
		if (!StringUtils.hasLength(appId)) {
			return;
		}
		String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":").append(appId).append(version).toString();

		RedisManager redisManager = RedisManager.getManager();
		redisManager.incrCount(key);
	}

	/**
	 * App 并发数减1
	 * @param appId
	 * @param version
	 */
	void decrAppConcurrent(String appId, String version) {
		if (!StringUtils.hasLength(appId)) {
			return;
		}
		String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":").append(appId).append(version).toString();
		RedisManager.getManager().decrCount(key);
	}

	/**
	 * 重置App 并发数
	 * @param appId
	 * @param version
	 */
	void resetAppConcurrent(String appId, String version) {
		if (StringUtils.hasLength(appId)) {
			String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":").append(appId).append(version).toString();

			RedisManager.getManager().set(key, "0", APICacheHelper.CACHE_TIMEOUT);
		}
	}

	/**
	 * 从底层服务中获取app最大并发数
	 * @param appID
	 * @return
	 * @throws APIException
	 */
	private String doOther(String appID) throws APIException {
		String data = new ConfigurationManager().getAppCacheValue(appID, APICacheHelper.APP_TYPE_LIST);
		String result = null;

		// Process result
		if (StringUtils.hasLength(data)) {
			CommandData cmdData = new CommandData();
			// 将json转换为CommandData对象
			JsonUnit.fromJson(cmdData, data);

			// app最大并发数
			result = cmdData.getParm("appMaxNum").getStringColumn();
		}

		return result;
	}
}
