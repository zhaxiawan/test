package com.travelsky.quick.util.helper;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.util.RedisUtil;

public class CurrencyManager {
	
	public static final String SERVICE = "SERVICE";
	private final ReentrantLock lock = new ReentrantLock();
	public CommandRet getCurrencyList(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet l_ret = new CommandRet("");
		CommandInput l_input = new CommandInput("com.travelsky.quick.customer.currency.list.query");
		String typecode = input.getParm("typecode").getStringColumn();
		String redisValue = RedisManager.getManager().get(RedisNamespaceEnum.api_service_currency.code());
		if (redisValue!=null&&!"".equals(redisValue)) {
			l_ret.addParm("LCC_DICTIONARYQUERY_SERVICE_"+typecode, redisValue);
			return l_ret;
		}
		l_ret = context.doOther(l_input, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
//		if (l_ret.getErrorCode().equals("") || l_ret.getErrorCode() == null) {
//			String type = SERVICE;
//			RedisUtil redisUtil = new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return l_ret;
	}
}
