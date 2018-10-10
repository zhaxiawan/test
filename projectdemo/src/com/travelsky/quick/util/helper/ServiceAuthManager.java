package com.travelsky.quick.util.helper;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.cares.sh.comm.HyConfig;
import com.cares.sh.comm.ParmEx;
import com.cares.sh.comm.ParmExUtil;
import com.cares.sh.comm.SystemConfig;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.log.ats.AtsLogHelper;

/**
 * 服务授权Manager
 *
 * @author huxizhun
 *
 */
public class ServiceAuthManager {
	private static final ServiceAuthManager HELPER = new ServiceAuthManager();
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceAuthManager.class);

	public static ServiceAuthManager getInstance() {
		return HELPER;
	}


	/**
	 * 从缓存中获取用户授权列表
	 * @param userId
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getAuthListFromJredis(String userId) {
		/*
		 * 从缓存中获取用户授权服务
		 * 缓存中的格式为[service1,service2,service3]
		 */
		String json = getValueFromJRedis(userId);
		if (StringUtils.hasLength(json)) {
			return (Set<String>) JSON.parseObject(json, Set.class);
		}

		return null;
	}

	/**
	 * 从底层服务中获取授权用户的service集合
	 * @param userId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getAuthListFromCommand(String userId) {
		// 缓存中获取失败或缓存中无数据，调用底层服务获取用户授权
		String json = getServiceJsonArr(userId);
		if (StringUtils.hasLength(json)) {
			// 获取数据
			Set<String> set = (Set<String>) JSON.parseObject(json, Set.class);
			// 存入redis缓存.
			String key = RedisNamespaceEnum.api_cache_auto.toKey(userId);
			RedisManager.getManager().set(
					key, json, APICacheHelper.CACHE_TIMEOUT);
			return set;
		}

		return null;
	}

	/**
	 * 从底层服务中获取授权正则表达式(允许访问的授权)<br>
	 * 表达式格式为service1|service2|.*|ser.*|*
	 * @param userId
	 * @return
	 */
	public String getAuthAllowRegexFromCommand(String userId) {
		// 缓存中获取失败或缓存中无数据，调用底层服务获取用户授权
		String regex = getServiceAllowRegex(userId);
		if (StringUtils.hasLength(regex)) {
			// 看getAuthAllowRegexFromJredis方法了解关于此正则的说明。
			Pattern pattern = Pattern.compile("(\\.*(\\.\\*){0,1}[0-9a-zA-Z_\\.]*(\\.\\*){0,1})+((?<=.)\\|(?=.)\\.*(\\.\\*){0,1}[0-9a-zA-Z_\\.]*(\\.\\*){0,1})*");
			if (regex != null && pattern.matcher(regex).matches()) {
				// 存入redis缓存.
				String key =  RedisNamespaceEnum.api_cache_auto.toKey(userId);
				RedisManager.getManager().set(
						key, regex, APICacheHelper.CACHE_TIMEOUT);

				return regex;
			}
			else {
				LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_REDIS_AUTH_SERVICE,
						new StringBuilder("regex:").append(regex).toString(),
						ApiServletHolder.getApiContext().getLanguage()));
			}
		}

		return null;
	}

	/**
	 * 从Redis缓存中获取授权正则表达式(允许访问的授权)<br>
	 * 表达式格式为service1|service2|.*|ser.*|*
	 * @param userId
	 * @return
	 */
	public String getAuthAllowRegexFromJredis(String userId) {
		/*
		 * 从缓存中获取用户授权服务的正则表达式.
		 * 表达式格式为service1|service2|.*|ser.*|.*
		 */
		String regex = getValueFromJRedis(userId);
		/*
		 * 判断是否合法的授权正则表达式。此正则允许的字符串有以下几种情况:
		 * 1. 以". .* 0-9a-zA-Z _"中的任意组合形成的字符串. (不含空格及引号)(以下用p1表示)
		 * 2. 以"p1|p1"固定组合方式形成的字符串. 其中"p1|p1"可以任意组合
		 * 正确的示例如下(不含引号):
		 * "service0" "aservice|bservice_" ".*" "." "a_.|bservice.*|c._ser.*"
		 * 错误示例如下(不含引号):
		 *  "$" "|" "|a" "||" "a|b|"
		 */
		Pattern pattern = Pattern.compile("(\\.*(\\.\\*){0,1}[0-9a-zA-Z_\\.]*(\\.\\*){0,1})+((?<=.)\\|(?=.)\\.*(\\.\\*){0,1}[0-9a-zA-Z_\\.]*(\\.\\*){0,1})*");
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
		String k =  RedisNamespaceEnum.api_cache_auto.toKey(key);
		RedisManager redisManager = RedisManager.getManager();
		return redisManager.get(k);
	}

	/**
	 * 从底层获取授权service,并拼成允许访问的正则
	 * @param userId
	 * @return
	 */
	private String getServiceAllowRegex(String userId) {
		CommandRet ret = doOther(userId);

		if (ret.isError()) {
			LOGGER.info(TipMessager.getNoneI18nMessage(ret.getErrorCode(), ret.getErrorDesc()));
		} else {
			Table table = ret.getParm("userrightapi").getTableColumn();
			int size = table == null ? 0 : table.getRowCount();
			if (size > 0) {
				StringBuilder sb = new StringBuilder();
				// 遍历用户权限，获取ServiceName
				for (int i = 0; i < size; i++) {
					Row row = table.getRow(i);
					// ServiceName
					String serviceName = row.getColumn("resourceid").getStringColumn();
					if (StringUtils.hasLength(serviceName)) {
						if (serviceName.indexOf('*') != -1) {
							serviceName = serviceName.replaceAll("\\*", ".*");
						}
						sb = sb.append(serviceName).append(i < size - 1 ? "|" : "");
					}
				}

				return sb.toString();
			}
		}

		return null;
	}

	/**
	 * 从底层获取允许授权的json数组
	 * @param userId
	 * @return
	 */
	public String getServiceJsonArr(String userId) {
		CommandRet ret = doOther(userId);

		if (ret.isError()) {
			LOGGER.info(TipMessager.getNoneI18nMessage(ret.getErrorCode(), ret.getErrorDesc()));
		} else {
			Table table = ret.getParm("userrightapi").getTableColumn();
			int size = table == null ? 0 : table.getRowCount();
			if (size > 0) {
				StringBuilder sb = new StringBuilder("[");
				// 遍历用户权限，获取ServiceName
				for (int i = 0; i < size; i++) {
					Row row = table.getRow(i);
					// ServiceName
					String serviceName = row.getColumn("resourceid").getStringColumn();
					if (StringUtils.hasLength(serviceName)) {
						sb = sb.append("\"").append(serviceName).append("\"").append(i < size - 1 ? "," : "");
					}
				}
				sb.append("]");

				return sb.toString();
			}
		}

		return null;
	}

	/**
	 * 调底层服务获取用户授权service
	 *
	 * @param userId
	 * @return
	 */
	private CommandRet doOther(String userId) {
		// Call underlying service
		CommandInput input = new CommandInput("com.cares.sh.order.user.permission.query");
		if (StringUtils.hasLength(userId)) {
			input.addParm("userid", userId);
		}
		// 暂时使用config配置的内置用户只需该操作
		build(input, true);
		CommandRet ret = ApiServletHolder.get().doOther(input, true);

		return ret;
	}

	private  static final void build(CommandInput input, boolean force){
		if(input==null) return;

		if(!force && input.getParm("__head").getObjectColumn() !=null){
			return ;
		}

		ParmEx parmEx = new ParmEx(input);

		HyConfig config = SystemConfig.getConfig();

		String user = config.getItemString(ParmExUtil.SYS_APP_KEY, "USERID", "SYS_ADMIN");

		parmEx.setUserId(user);
		parmEx.setLanguage(config.getItemString(ParmExUtil.SYS_APP_KEY, "LANGUAGE", "zh_CN"));
		parmEx.setAppid(config.getItemString(ParmExUtil.SYS_APP_KEY, "APPID", "SYS_APP"));
		parmEx.setChannelNo(config.getItemString(ParmExUtil.SYS_APP_KEY, "CHANNEL", "CTO"));
		parmEx.setIp(config.getItemString("SYSTEM", "SERVERIP", "0.0.0.0"));
		parmEx.setTicketDeptid(config.getItemString(ParmExUtil.SYS_APP_KEY, "DEPTID", "SYS_DEPT"));
		String tid=ApiServletHolder.get().getTransactions();
		if(!StringUtils.hasLength(tid)) {
			tid=AtsLogHelper.getGloalTransactionID();
		}
		parmEx.setTransactions(tid);
		parmEx.setServer(user+"#"+SystemConfig.getServerIp()+"#"+SystemConfig.getAppName());

	}
}
