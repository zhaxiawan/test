package com.travelsky.quick.util.helper;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.util.RedisUtil;

public class DictManager {

	public static final String SERVICE = "SERVICE";
	private final ReentrantLock lock = new ReentrantLock();

	public CommandRet getDict(CommandData input, SelvetContext<ApiContext> context) {
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		CommandRet l_ret = new CommandRet("");
		String typecode = input.getParm("typecode").getStringColumn();
		String tktdeptid = input.getParm("tktdeptid").getStringColumn();
		if ("".equals(typecode)) {
			l_ret.setError(ErrCodeConstants.API_NULL_TYPECODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_TYPECODE, language));
			return l_ret;
		}
		CommandInput l_input = new CommandInput("com.cares.sh.shopping.dictdetail.query");
		l_input.addParm("dictcode", typecode);
		l_input.addParm("ticketdeptid", tktdeptid);
		l_ret = context.doOther(l_input, false);
		if (!l_ret.isError()) {
			Table dict = l_ret.getParm("dictdetail").getTableColumn();
			if (null != dict) {
				dict = dict.copy(new String[] { "code", "name", "linkcode", "remark" });
			} else {
				dict = new Table(new String[] { "code", "name", "linkcode", "remark" });
			}
			CommandRet l_newret = new CommandRet("");
			l_newret.setError(l_ret.getErrorCode(), l_ret.getErrorDesc());
			l_newret.addParm("dict", dict);
			return l_newret;

		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (l_ret.getErrorCode().equals("") || l_ret.getErrorCode() == null)
		// {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return l_ret;
	}

	public CommandRet getDictNdc(CommandData input, SelvetContext<ApiContext> context) {
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		CommandRet l_ret = new CommandRet("");
		String typecode = input.getParm("typecode").getStringColumn();
		String tktdeptid = input.getParm("tktdeptid").getStringColumn();
		if ("".equals(typecode)) {
			l_ret.setError(ErrCodeConstants.API_NULL_TYPECODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_TYPECODE, language));
			return l_ret;
		}
	   if ("COUNTRYAREATYPE".equalsIgnoreCase(typecode)) {
			/*** 获取证件签发国和手机区号列表接口 ***/
			CommandInput l_input = new CommandInput("com.travelsky.quick.countryarea.query");
			l_input.addParm("type", "query");
			l_input.addParm("ticketdeptid", tktdeptid);
			String redisValue = RedisManager.getManager()
					.get(RedisNamespaceEnum.api_service_countryarea.toKey(language));
			if (redisValue != null && !"".equals(redisValue)) {
				l_ret.addParm("LCC_DICTIONARYQUERY_SERVICE_" + typecode, redisValue);
				return l_ret;
			}
			l_ret = context.doOther(l_input, false);
		}else if ("IDENTITYTYPE".equalsIgnoreCase(typecode)) {
			//获取证件类型
			CommandInput l_input = new CommandInput("com.cares.sh.member.identitytype.query");
			l_input.addParm("type", "query");
			l_input.addParm("ticketdeptid", tktdeptid);
			String redisValue = RedisManager.getManager()
					.get(RedisNamespaceEnum.api_service_currency.code()+ language);
			if (redisValue != null && !"".equals(redisValue)) {
				l_ret.addParm("LCC_DICTIONARYQUERY_SERVICE_" + typecode, redisValue);
				return l_ret;
			}
			l_ret = context.doOther(l_input, false);
		}
	   if (l_ret.isError()) {
			CommandRet l_newret = new CommandRet("");
			l_newret.setError(l_ret.getErrorCode(), l_ret.getErrorDesc());
			return l_newret;
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (l_ret.getErrorCode().equals("") || l_ret.getErrorCode() == null)
		// {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return l_ret;
	}
}
