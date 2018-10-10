package com.travelsky.quick.util.helper;

import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;

/**
 * @author ZHANGJIABIN
 */
public class FlightManager {

	public static final String SERVICE = "SERVICE";

	/**
	 * 航站对搜索
	 * 
	 * @param input
	 *            input
	 * @param context
	 *            context
	 * @return CommandRet
	 */
	public CommandRet odQuery(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet lRet = new CommandRet("");
		CommandInput lInput = new CommandInput("com.cares.sh.iata.airport.odquery");
		lInput.addParm("bookRoutesRange", input.getParm("airportCode").getStringColumn());
		// 语言类型
		String ods = RedisManager.getManager().get(RedisNamespaceEnum.api_service_route.toKey(
				ApiServletHolder.getApiContext().getTicketDeptid() + 
				ApiServletHolder.getApiContext().getLanguage())
							);
		if (!StringUtils.isEmpty(ods)) {
			lRet.addParm("LCC_SEARCHODS_SERVICE", ods);
			return lRet;
		}
		lRet = context.doOther(lInput, false);
		if (lRet.isError()) {
			lRet.setError(lRet.getErrorCode(), lRet.getErrorDesc());
			return lRet;
		}
		Table odTable = lRet.getParm("airportod").getTableColumn();
		lRet = getOdsResult(odTable);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if ("".equals(lRet.getErrorCode()) || lRet.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return lRet;
	}

	/**
	 * 
	 * @param table
	 * @return
	 */
	private CommandRet getOdsResult(Table odTable) {
		CommandRet ret = new CommandRet("");
		Table resultTable = new Table(new String[] { "dest", "ori" });
		if (odTable != null && odTable.getRowCount() > 0) {
			for (int i = 0; i < odTable.getRowCount(); i++) {
				Row lRow = odTable.getRow(i);
				Row row = resultTable.addRow();
				CommandData l_dest = lRow.getColumn("dest").getObjectColumn();
				CommandData dest = new CommandData();
				dest.addParm("airportcode", l_dest.getParm("code").getStringColumn());
				dest.addParm("airportname", l_dest.getParm("name").getObjectColumn());
				dest.addParm("citycode", l_dest.getParm("city_code").getStringColumn());
				dest.addParm("city_name", l_dest.getParm("city_name").getObjectColumn());
				dest.addParm("country", l_dest.getParm("country").getStringColumn());
				CommandData lOri = lRow.getColumn("ori").getObjectColumn();
				CommandData ori = new CommandData();
				ori.addParm("airportcode", lOri.getParm("code").getStringColumn());
				ori.addParm("airportname", lOri.getParm("name").getObjectColumn());
				ori.addParm("citycode", lOri.getParm("city_code").getStringColumn());
				ori.addParm("city_name", lOri.getParm("city_name").getObjectColumn());
				ori.addParm("country", lOri.getParm("country").getStringColumn());
				row.addColumn("dest", dest);
				row.addColumn("ori", ori);
			}
			CommandRet result = new CommandRet("");
			result.addParm("airportod", resultTable);
			return result;
		}
		return ret;
	}
}
