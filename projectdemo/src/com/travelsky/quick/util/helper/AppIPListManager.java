package com.travelsky.quick.util.helper;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.cares.sh.comm.JsonUnit;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;

/**
 * IP列表Manager
 *
 * @author huxizhun
 *
 */
public class AppIPListManager {
	private static final AppIPListManager HELPER = new AppIPListManager();
	private static final Logger LOGGER = LoggerFactory.getLogger(AppIPListManager.class);

	public static AppIPListManager getInstance() {
		return HELPER;
	}


	/**
	 * 从缓存中获取IP授权列表
	 * @param appID
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getIPListFromJredis(String appID) {
		/*
		 * 从缓存中获取用户IP列表
		 * 缓存中的格式为[ip1,ip2,ip3]
		 */
		String json = getValueFromJRedis(appID);
		if (StringUtils.hasLength(json)) {
			return (Set<String>) JSON.parseObject(json, Set.class);
		}

		return null;
	}

	/**
	 * 从底层服务中获取IP授权列表
	 * @param appID
	 * @return
	 * @throws APIException
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getIPListFromCommand(String appID) throws APIException {
		// 缓存中获取失败或缓存中无数据，调用底层服务获取用户授权
		String json = getIPListJsonArr(appID);
		if (StringUtils.hasLength(json)) {
			// 获取数据
				Set<String> set = (Set<String>) JSON.parseObject(json, Set.class);
				// 存入redis缓存.
				String key = RedisNamespaceEnum.api_cache_prefix.toKey(appID);
				RedisManager.getManager().set(
						key, json, APICacheHelper.CACHE_TIMEOUT);
				return set;
		}

		return null;
	}

	/**
	 * 从底层服务中获取ip列表正则表达式(允许访问的ip列表)<br>
	 * 表达式格式为ip1|ip2|.*|172.*|172.1.1.*
	 * @param appID
	 * @return
	 * @throws APIException
	 */
	public String getIPsAllowRegexFromCommand(String appID) throws APIException {
		// 缓存中获取失败或缓存中无数据，调用底层服务获取用户授权
		String regex = getIPsAllowRegex(appID);
		if (StringUtils.hasLength(regex)) {
			// 看getIPsAllowRegexFromJredis方法了解关于此正则的说明。
			Pattern pattern = Pattern.compile("[\\d\\*]{1,3}((\\.[\\d\\*]{1,3}){3}|(\\.[\\d\\*]{0,3}){0,2}(?>\\*))");
			if (regex != null && pattern.matcher(regex).matches()) {
				// 存入redis缓存.
				String key = RedisNamespaceEnum.api_cache_prefix.toKey(appID);
				RedisManager.getManager().set(
						key, regex, APICacheHelper.CACHE_TIMEOUT);

				return regex;
			}
			else {
				LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_ERR_GETIP,
						new StringBuilder("regex:").append(regex).toString(),
						ApiServletHolder.getApiContext().getLanguage()));
			}
		}

		return null;
	}

	/**
	 * 从Redis缓存中获取IP列表正则表达式(允许访问的IP列表)<br>
	 * 表达式格式为ip1|ip2|.*|172.*|172.1.1.*
	 * @param appID
	 * @return
	 */
	public String getIPsAllowRegexFromJredis(String appID) {
		/*
		 * 从缓存中获取用户授权服务的正则表达式.
		 * 表达式格式为service1|service2|.*|ser.*|.*
		 */
		String regex = getValueFromJRedis(appID);
		/*
		 * 判断是否合法的ip列表正则表达式。此正则允许的字符串有以下几种情况:
		 * 1. 以"0-9 *"中的任意组合,形成的长度为1-3之间的字符串. (不含空格及引号)(以下用p1表示)
		 * 2. 以"p1.p1.p1.p1|p1.p1.p1.p1"固定组合方式形成的字符串. 其中"p1.p1.p1.p1|p1.p1.p1.p1"可任意组合
		 * 正确的示例如下(不含引号):
		 * "1*" "127.*" ".*" "127.0.0.1" "127.0.0.1|176.*|202.23.3*"
		 * 错误示例如下(不含引号):
		 *  "1111" "1111.1111" "127.0.1" "127." "*"
		 */
		Pattern pattern = Pattern.compile("[\\d\\*]{1,3}((\\.[\\d\\*]{1,3}){3}|(\\.[\\d\\*]{0,3}){0,2}(?>\\*))");
		if (regex != null && pattern.matcher(regex).matches()) {
			return regex;
		}

		return null;
	}

	/**
	 * 从缓存中获取值
	 * @param key
	 * @return
	 */
	private String getValueFromJRedis(String key) {
		String result = null;
		if (!StringUtils.hasLength(key)) {
			return result;
		}

		// 从Redis中获取
		String k =RedisNamespaceEnum.api_cache_prefix.toKey(key);
		RedisManager redisManager = RedisManager.getManager();
		return redisManager.get(k);
	}

	/**
	 * 从底层获取ip列表,并拼成允许访问的正则
	 * @param appID
	 * @return
	 * @throws APIException
	 */
	private String getIPsAllowRegex(String appID) throws APIException {
		CommandData ret = doOther(appID);
		String result = null;
		if (ret!=null) {
			// IP 列表
			Table ipTable = ret.getParm("ipList").getTableColumn();
			int size = ipTable == null? 0 : ipTable.getRowCount();
			if (size > 0) {
				StringBuilder sb = new StringBuilder();

				for (int i=0; i<size; i++) {
					Row row = ipTable.getRow(i);
					// 拼接ip正则串
					String ip = row.getColumn("ip").getStringColumn();
					if (StringUtils.hasLength(ip)) {
						sb = sb.append(ip)
								.append(i<size-1? "|" : "");
					}
				}

				result = sb.toString();
			}
		}

		return result;
	}

	/**
	 * 从底层获取ip授权的json数组
	 * @param appID
	 * @return
	 * @throws APIException
	 */
	public String getIPListJsonArr(String appID) throws APIException {
		CommandData ret = doOther(appID);
		String result = null;
		if (ret != null) {
			// IP 列表
			Table ipTable = ret.getParm("ipList").getTableColumn();
			int size = ipTable == null? 0 : ipTable.getRowCount();
			if (size > 0) {
				StringBuilder sb = new StringBuilder("[");

				for (int i=0; i<size; i++) {
					Row row = ipTable.getRow(i);
					// 拼接ip json串
					String ip = row.getColumn("ip").getStringColumn();
					sb = sb.append("\"")
							.append(ip)
							.append("\"")
							.append(i<size-1? "," : "");
				}

				result = sb.append("]").toString();
			}
		}

		return result;
	}

	/**
	 * 调底层服务获取用户ip列表
	 *
	 * @param userId
	 * @return
	 * @throws APIException
	 */
	private CommandData doOther(String appID) throws APIException {
		String data = new ConfigurationManager().getAppCacheValue(appID, APICacheHelper.APP_TYPE_LIST);
		CommandData cmdData = null;
		// Process result
		if (StringUtils.hasLength(data)) {
			cmdData = new CommandData();
			// 将json转换为CommandData对象
			JsonUnit.fromJson(cmdData, data);
		}

		return cmdData;

	}
}
