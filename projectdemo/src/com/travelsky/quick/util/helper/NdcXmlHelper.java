package com.travelsky.quick.util.helper;

import org.iata.iata.edist.MsgPartiesType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;

/**
 * NDC xml 辅助类
 * <ul>
 * <li>解析NDC XML头,获取部门号</li>
 * </ul>
 * @author huxizhun
 *
 */
public class NdcXmlHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(NdcXmlHelper.class);

	/**
	 * 获取部门号
	 * @return
	 */
//	public static String getDeptNo(MsgPartiesType party) throws APIException {
//		String deptNo = null;
//		if (party == null
//				|| party.getSender() == null
//				|| party.getSender().getTravelAgencySender() == null
//				|| party.getSender().getTravelAgencySender().getAgencyID() == null
//				|| StringUtils.isEmpty(party.getSender().getTravelAgencySender().getAgencyID().getStringValue())) {
//
//			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_DEPART_NO,
//					ApiServletHolder.getApiContext().getLanguage()));
//
//			throw new APIException(ErrCodeConstants.API_NULL_DEPART_NO);
//		}else{
//			deptNo = party.getSender().getTravelAgencySender().getAgencyID().getStringValue();
//
//			//channelno
//			CommandInput l_input = new CommandInput("com.cares.sh.order.dept.query");
//			l_input.addParm("code", deptNo);
//			CommandRet l_ret = ApiServletHolder.get().doOther(l_input,false);
//			Table taxtab = l_ret.getParm("department").getTableColumn();
//			String channel ="";
//			String timeZone = "";
//			ApiContext apiCtx = ApiServletHolder.getApiContext();
//			if (taxtab != null) {
//				for (int i = 0; i < taxtab.getRowCount(); i++) {
//					Row taxrow = taxtab.getRow(i);
//					channel = taxrow.getColumn("channel").getStringColumn();
//					timeZone = taxrow.getColumn("timeZone").getStringColumn();
//				}
//			}
//			if (StringUtils.hasLength(channel)) {
//				apiCtx.setChannelNo(channel);
//			}
//			if(StringUtils.hasLength(timeZone)) {
//				apiCtx.setTimeZone(timeZone);
//			}
//
//			apiCtx.setTicketDeptid(deptNo);
//		}
//
//		return deptNo;
//	}
}
