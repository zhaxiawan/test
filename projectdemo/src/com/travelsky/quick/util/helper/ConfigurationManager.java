package com.travelsky.quick.util.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;

public class ConfigurationManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);
	/**
	 * 从服务中获取与app相关白名单、最大并发数等信息
	 * @param code
	 * @param type
	 * @return
	 * @throws APIException
	 */
	public String getAppCacheValue(String code, String type) throws APIException {
		CommandInput commandInput = new CommandInput(
				"com.cares.sh.order.config.query");

		commandInput.addParm("type", type);
		commandInput.addParm("code", code);

		// 调底层
		CommandRet ret = ApiServletHolder.get().doOther(commandInput,false);
		String result = null;

		if (ret.isError()) {
			String msg = new StringBuilder("Call order [")
					.append(commandInput.getName())
					.append("] failed:")
					.append(TipMessager.getNoneI18nMessage(ret.getErrorCode(), ret.getErrorDesc()))
					.toString();
			LOGGER.error(msg);
		}
		else{
			LOGGER.debug("order return config info:{}",JsonUnit.toJson(ret));
			// 获取配置集合
			Table table=ret.getParm("configs").getTableColumn();
			int size = table == null? 0 : table.getRowCount();

			if (size < 1)
				return null;

			if (size > 1) {
				// 一个appid只应该存在一条记录, 将使用最后一条记录
				LOGGER.warn(new StringBuilder("Command name[")
						.append(commandInput.getName())
						.append("], appid[")
						.append(code)
						.append("] repeat! Use lastest record").toString());
			}
			String status = table.getRow(size-1).getColumn("usestatus").getStringColumn();
			// 判断此app是否被禁用
			if (!"Y".equalsIgnoreCase(status)) {
				String msg = TipMessager.getInfoMessage(ErrCodeConstants.API_APPID_STATUS_DISABLED,
						ApiServletHolder.getApiContext().getLanguage());
				LOGGER.info(msg);
				throw APIException.getInstance(ErrCodeConstants.API_APPID_STATUS_DISABLED);
			}
			// 返回关于此app的配置(最大并发数、白名单等)
			result = table.getRow(size-1).getColumn("data").toString();
		}

		return result;
	}
	/**
	 * 获取配置中日志中不包含xml数据的接口数据
	 * @param ctx 
	 * @return
	 */
	public static String getINXL(SelvetContext<ApiContext> ctx) {
		
		//缓存中获取相关配置信息
		String result = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_apiinterface.toKey("norqxmllog"));
		//缓存中有配置信息直接返回
		if (!StringUtils.isEmpty(result)) {
			return result;
		}
		//没有配置信息从底层获取
		CommandInput commandInputGetTime = new CommandInput("com.cares.sh.order.config.query");
		commandInputGetTime.addParm("type", "COMMON");
		commandInputGetTime.addParm("code", "APIINTERFACE_NORQXMLLOG");
		CommandRet l_ret = ctx.doOther(commandInputGetTime, false);
		 
		Table configstable = l_ret.getParm("configs").getTableColumn();
		if(configstable!=null && configstable.getRowCount()>0){
			result = configstable.getRow(0).getColumn("data").getStringColumn();
		}
		//有配置信息数据直接返回
		if (!StringUtils.isEmpty(result)) {
			//将配置信息存入缓存,
			 RedisManager.getManager().set(RedisNamespaceEnum.api_cache_apiinterface.toKey("norqxmllog"), result, APICacheHelper.CACHE_TIMEOUT);
			return result;
		}
		//相关接口日志中都要包含xml数据
		result="";
		return result;
		
		
	}
}
