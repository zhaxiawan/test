package com.travelsky.quick.util.helper;

import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.exception.APIException;

public class ServiceConcurrentManager {
	/**
	 * 获取最大并发数
	 * @param appID
	 * @param serviceName
	 * @param version
	 * @return
	 * @throws APIException
	 */
	String getMaxNum(String appID, String serviceName, String version) throws APIException {
		String result = null;
		if (!StringUtils.hasLength(appID) || !StringUtils.hasLength(serviceName)) {
			return result;
		}

		// 从Redis中获取
		String key = new StringBuffer(RedisNamespaceEnum.api_cache_appmax.code()+":")
				.append(appID)
				.append(serviceName)
				.append(version)
				.toString();
		RedisManager redisManager = RedisManager.getManager();
		result = redisManager.get(key);
		// Redis中不存在从服务中取
		if (!StringUtils.hasLength(result)) {
			result = doOther(appID, serviceName, version);
//
//			if (StringUtils.hasLength(result)) {
//				redisManager.set(key, result, APICacheHelper.CACHE_TIMEOUT);
//			}
		}

		return result;
	}

	/**
	 * 获取Service当前并发数
	 * @param appID
	 * @param serviceName
	 * @param version
	 * @return
	 */
	String getSevCurConcurrent(String appId, String serviceName, String version) {
		String curNum = null;
		if (StringUtils.hasLength(serviceName) && StringUtils.hasLength(appId)) {
			String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":").append(appId).append(serviceName).append(version).toString();
			curNum = RedisManager.getManager().get(key);

			if (!StringUtils.hasLength(curNum)) {
				return "0";
			}
		}

		return curNum;
	}

	/**
	 * Service并发数加1
	 * @param appId
	 * @param serviceName
	 * @param version
	 */
	void incrServiceConcurrent(String appId, String serviceName, String version) {
		if (!StringUtils.hasLength(serviceName) || !StringUtils.hasLength(appId)) {
			return;
		}
		String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":")
				.append(appId).append(serviceName).append(version).toString();

		RedisManager.getManager().incrCount(key);
	}

	/**
	 * Service并发数减1
	 * @param appId
	 * @param serviceName
	 * @param version
	 */
	public void decrServiceConcurrent(String appId, String serviceName, String version) {
		if (!StringUtils.hasLength(serviceName) || !StringUtils.hasLength(appId)) {
			return;
		}
		String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":")
				.append(appId).append(serviceName).append(version).toString();

		RedisManager.getManager().decrCount(key);
	}

	/**
	 * 重置service并发数
	 * @param appId
	 * @param serviceName
	 * @param version
	 */
	void resetSevConcurrent(String appId, String serviceName, String version) {
		if (StringUtils.hasLength(serviceName) && StringUtils.hasLength(appId)) {
			String key = new StringBuffer(RedisNamespaceEnum.api_cache_appcur.code()+":")
					.append(appId).append(serviceName).append(version).toString();

			RedisManager.getManager().set(key, "0", APICacheHelper.CACHE_TIMEOUT);
		}
	}

	/**
	 * 从底层服务中获取service最大并发数
	 * @param appID
	 * @param serviceName
	 * @param version
	 * @return
	 * @throws APIException
	 */
	private String doOther(String appID, String serviceName, String version) throws APIException {
		String data = new ConfigurationManager().getAppCacheValue(appID, APICacheHelper.APP_TYPE_LIST);
		String result = null;

		// Process result
		if (StringUtils.hasLength(data)) {
			CommandData cmdData = new CommandData();
			// 将json转换为CommandData对象
			JsonUnit.fromJson(cmdData, data);

			// services 列表
			Table serviceTable = cmdData.getParm("services").getTableColumn();
			int size = serviceTable == null? 0 : serviceTable.getRowCount();

			boolean noFound = true;
			if (size > 1) {
				// 遍历services列表
				for (int i=0; i<size; i++) {
					Row row = serviceTable.getRow(i);
					String sevName = row.getColumn("sevName").getStringColumn();
					// 底层数据中存在service. 则改变状态
					if (sevName.equals(serviceName)) {
						noFound = false;
						result = row.getColumn("sevMaxNum").getStringColumn();
					}

					if (StringUtils.hasLength(sevName)) {
						// 将service最大并发存入缓存
						String key = new StringBuffer(RedisNamespaceEnum.api_cache_appmax.code()+":")
								.append(appID)
								.append(serviceName)
								.append(version)
								.toString();
						String value = row.getColumn("sevMaxNum").getStringColumn();
						RedisManager.getManager().set(key, value, APICacheHelper.CACHE_TIMEOUT);
					}
				}
			}

			// 底层数据中不存在service.使用默认最大并发数
			if (noFound) {
				result = cmdData.getParm("serDefMaxNum").getStringColumn();
				if (StringUtils.hasLength(result)) {
					// 将service最大并发存入缓存
					String key = new StringBuffer(RedisNamespaceEnum.api_cache_appmax.code()+":")
							.append(appID)
							.append(serviceName)
							.append(version)
							.toString();
					RedisManager.getManager().set(key, result, APICacheHelper.CACHE_TIMEOUT);
				}
			}
		}

		return result;
	}
}
