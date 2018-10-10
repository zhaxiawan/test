package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirDocCancelRQDocument;
import org.iata.iata.edist.AirDocCancelRQDocument.AirDocCancelRQ;
import org.iata.iata.edist.AirDocCancelRQDocument.AirDocCancelRQ.Query.AirDocument;
import org.iata.iata.edist.AirDocCancelRSDocument;
import org.iata.iata.edist.AirDocCancelRSDocument.AirDocCancelRS;
import org.iata.iata.edist.AirDocCancelRSDocument.AirDocCancelRS.Response;
import org.iata.iata.edist.AirDocCancelRSDocument.AirDocCancelRS.Response.DocumentType;
import org.iata.iata.edist.CodesetType;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.TicketDocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 取消机票
 * 
 * @author wangyicheng
 * @version 0.1
 */
@Service("LCC_CANCELET_SERVICE")
public class APICancelETNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 取消机票
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APICancelETNDCBusiness.class);
	
	
	/**
	 * @param context
	 *            SelvetContext<ApiContext>
	 * @throws Exception
	 *             Exception
	 */
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 获取xml
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResult());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_QUERYTICKET,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}

	}

	/**
	 * 
	 * @param input
	 *            CommandData
	 * @param context
	 *            SelvetContext
	 * @return CommandRet
	 */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.cancelTicket(input, context);
	}

	/**
	 * 
	 * @param context
	 *            SelvetContext
	 * @param xmlInput
	 *            String
	 * @throws APIException
	 *             APIException
	 * @throws Exception
	 *             Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		AirDocCancelRQDocument rootDoc = AirDocCancelRQDocument.Factory.parse(xmlInput);
		AirDocCancelRQ reqDoc = rootDoc.getAirDocCancelRQ();
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		input.addParm("language", language);
		// 部门ID  
		input.addParm("tktdeptid",ApiServletHolder.getApiContext().getTicketDeptid());
		// 会员账号
		input.addParm("memberid", context.getContext().getUserID());
		Table tkts = new Table(new String[]{"tktno","tkttype"});
		AirDocument[] ticketDocumentArray = reqDoc.getQuery().getAirDocumentArray();
		if(null != ticketDocumentArray && ticketDocumentArray.length>0){
			for(int i=0;i<ticketDocumentArray.length;i++){
				Row tktRow = tkts.addRow();
				String ticketNo = ticketDocumentArray[i].getTicketDocument().getTicketDocNbr();
				CodesetType codesetType = ticketDocumentArray[i].getTicketDocument().getType();
				String code = "";
				if(codesetType != null){
					code = codesetType.getCode();
				}
				if(!StringUtils.hasLength(ticketNo)){
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_TICKET_NO, 
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_TICKET_NO);
				}
				tktRow.addColumn("tktno", ticketNo);
				if(!StringUtils.hasLength(code)){
					tktRow.addColumn("tkttype", "");
				}else{
					tktRow.addColumn("tkttype", code);
				}
			}
		}else{
			 LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_TICKET_NO, 
						ApiServletHolder.getApiContext().getLanguage()));
			 throw APIException.getInstance(ErrCodeConstants.API_NULL_TICKET_NO);
		}
		input.addParm("tkts", tkts);
	}

	/**
	 * 转换 xml-->Reqbean
	 * 
	 * @param xmlOutput
	 *            CommandRet
	 * @param input
	 *            CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		AirDocCancelRSDocument oddoc = AirDocCancelRSDocument.Factory.newInstance();
		AirDocCancelRS rprs = oddoc.addNewAirDocCancelRS();
		try {
			String errorcode = commandRet.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			} else {
				rprs.addNewSuccess();
				// Response 
				Response response = rprs.addNewResponse();
				StringBuffer sb = new StringBuffer("");
				String tkttype = null;
				Table tkts = input.getParm("tkts").getTableColumn();
				if(tkts != null && tkts.getRowCount() > 0){
					for (int i = 0; i < tkts.getRowCount(); i++) {
						Row tktRow = tkts.getRow(i);
						String tktno = tktRow.getColumn("tktno").getStringColumn();
						tkttype = tktRow.getColumn("tkttype").getStringColumn();
						sb = sb.append(tktno).append(",");
					}
				}
				String ticketNos = sb.toString();
				if(StringUtils.hasLength(ticketNos) && ticketNos.endsWith(",")){
					ticketNos = ticketNos.substring(0, ticketNos.length()-1);
				}
				//
				DocumentType newDocumentType = response.addNewDocumentType();
				TicketDocumentType newTicketDocument = newDocumentType.addNewTicketDocument();
				newTicketDocument.setTicketDocNbr(ticketNos);
				newTicketDocument.addNewType().setCode(tkttype);;
			}
		} catch (Exception e) {
			oddoc = AirDocCancelRSDocument.Factory.newInstance();
			rprs = oddoc.addNewAirDocCancelRS();
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(
					ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return oddoc;
	}
}
