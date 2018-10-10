package com.travelsky.quick.util.helper;

import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.exception.APIException;

public class TokenManager {
	/**
	 * 获取token
	 * @param appId
	 * @return
	 * @throws APIException
	 */
	String getToken(String appId) throws APIException {
		String result = null;
		if (!StringUtils.hasLength(appId)) {
			return result;
		}

		// 从Redis中获取
		String key = RedisNamespaceEnum.api_cache_code.toKey(appId);
		RedisManager redisManager = RedisManager.getManager();
		result = redisManager.get(key);
		// Redis中不存在从服务中取
		if (!StringUtils.hasLength(result)) {
			result = doOther(appId);

			if (StringUtils.hasLength(result)) {
				redisManager.set(key, result, APICacheHelper.CACHE_TIMEOUT);
			}
		}

		return result;
	}

	/**
	 * 从底层服中获取token
	 * @param appId
	 * @return
	 * @throws APIException
	 */
	private String doOther(String appId) throws APIException {
		String data = new ConfigurationManager().getAppCacheValue(appId, APICacheHelper.APP_TYPE_LIST);
		String result = null;

		// Process result
		if (StringUtils.hasLength(data)) {
			CommandData cmdData = new CommandData();
			// 将json转换为CommandData对象
			JsonUnit.fromJson(cmdData, data);

			// app最大并发数
			result = cmdData.getParm("token").getStringColumn();
		}

		return result;
	}
}