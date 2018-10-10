package com.travelsky.quick.business;

 
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCRefundRQDocument;
import org.iata.iata.edist.LCCRefundRQDocument.LCCRefundRQ.Query.LCCRefund;
import org.iata.iata.edist.LCCRefundRQDocument.LCCRefundRQ.Query.LCCRefund.LCCRefundDetail;
import org.iata.iata.edist.LCCRefundRSDocument;
import org.iata.iata.edist.LCCRefundRSDocument.LCCRefundRS;
import org.iata.iata.edist.LCCRefundRSDocument.LCCRefundRS.Response;
import org.iata.iata.edist.LCCRefundRSDocument.LCCRefundRS.Response.LCCRefund.LCCServiceList;
import org.iata.iata.edist.LCCRefundRSDocument.LCCRefundRS.Response.LCCRefund.LCCServiceList.LCCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

import org.springframework.util.StringUtils;

import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 
 * @author MaRuifu 2016年5月3日下午3:17:48
 * @version 0.1
 * 类说明:退票预算
 */
@Service("LCC_REFUNDBUDGET_SERVICE")
public class APIRefundBudgetBusiness extends AbstractService<ApiContext> {

	private static final long serialVersionUID = -127743260337565160L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APIRefundBudgetBusiness.class);
	
	/**
	 * 
	 * @param  context SelvetContext<ApiContext>
	 * @throws Exception Exception
	 */
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取xml
		
			
		try{
			//转换 xml-->Reqbean
			transInputXmlToRequestBean();
			//获取ResponseBean
			context.setRet(getResult());
		}
		catch (APIException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_REFUNDBUDGET, 
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	
	
	/**
	 * 
	* @param input CommandData
	* @param context SelvetContext
	* @return  
	* CommandRet    返回类型 
	*
	 */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.refundbudget(input,context);
	}
	/**
	 * 转换 xml-->Reqbean
	 * @param context CommandData
	 * @param xmlInput String
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCRefundRQDocument rootDoc = null;
		rootDoc = LCCRefundRQDocument.Factory.parse(xmlInput);

		LCCRefundRQDocument.LCCRefundRQ reqDoc = rootDoc.getLCCRefundRQ();
		
		String channelNo = context.getContext().getChannelNo();
		//渠道号
		input.addParm("channelno", channelNo);
		input.addParm("tktdeptid",context.getContext().getTicketDeptid());
		// 添加用户id  orderno
		input.addParm("memberid", context.getContext().getUserID());
		//查询信息
		LCCRefund refund = reqDoc.getQuery().getLCCRefund();
		
		String  orderno = refund.getOrderID().getStringValue();
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		if(!StringUtils.hasLength(orderno)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_NO, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_NO);
		}
		String refundtype = refund.getType();
		if(!StringUtils.hasLength(refundtype)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REFUNDTYPE, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_REFUNDTYPE);
		}
		
		Table detailTab = new Table(new String[]{"offers"});
		Table offers = new Table(new String[]{"serviceid", "servicetype"});
		for (int i = 0; i < refund.getLCCRefundDetailArray().length; i++) {
			LCCRefundDetail refundDetail = refund.getLCCRefundDetailArray(i);
			//人航段id
			String flightid = refundDetail.getFlightID();
			if(!StringUtils.hasLength(flightid)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_PAXFLIGHTID_ISNULL, language));
				throw APIException.getInstance(ErrCodeConstants.API_PAXFLIGHTID_ISNULL);
			}
			Row row = offers.addRow();
			row.addColumn("serviceid", flightid);
			row.addColumn("servicetype", "FARE");
			
		}
		Row drow = detailTab.addRow();
		drow.addColumn("offers", offers);
		input.addParm("paxs", detailTab);
		input.addParm("orderno", orderno);
		input.addParm("memberid", context.getContext().getUserID());
		input.addParm("refundtype", refundtype);
	}
	/**
	 * 转换 xml-->Reqbean
	 * @param xmlOutput CommandRet
	 * @param input  CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(CommandRet xmlOutput ,CommandData input){
		
		LCCRefundRSDocument sadoc = LCCRefundRSDocument.Factory.newInstance();
		LCCRefundRS rprs = sadoc.addNewLCCRefundRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}else{
				rprs.addNewSuccess();
				Response response = rprs.addNewResponse();
				Table paxsTab = xmlOutput.getParm("paxs").getTableColumn();
				if(paxsTab!=null){
					for (int i = 0; i < paxsTab.getRowCount(); i++) {
					 Row paxsrow = paxsTab.getRow(i);
					 org.iata.iata.edist.LCCRefundRSDocument.LCCRefundRS.Response.
					 LCCRefund refund = response.addNewLCCRefund();
					 //乘机人编号
					 String paxid = paxsrow.getColumn("paxid").getStringColumn();
					 refund.setPaxID(paxid);
					 //航班编号
					 String flightid = paxsrow.getColumn("flightid").getStringColumn();
					 refund.setFlightID(flightid);
					 //机票价格
					 refund.setTicketPrice(paxsrow.getColumn("txtprice").getStringColumn());
					 //机场建设费
					 refund.setCNPrice(paxsrow.getColumn("cnprice").getStringColumn());
					 // 燃油附加费   
					 refund.setYQPrice(paxsrow.getColumn("yqprice").getStringColumn());
					 // 选座费       
					 refund.setSeatPrice(paxsrow.getColumn("seatprice").getStringColumn());
					 // 机票手续费    
					 refund.setTicketFee(paxsrow.getColumn("txtcharge").getStringColumn());
					 //机场建设费手续费        
					 refund.setCNFee(paxsrow.getColumn("cncharge").getStringColumn());
					 //燃油附加费手续费        
					 refund.setYQFee(paxsrow.getColumn("yqcharge").getStringColumn());
					 //选座费手续费        
					 refund.setSeatFee(paxsrow.getColumn("seatcharge").getStringColumn());
					 LCCServiceList serviceList = refund.addNewLCCServiceList();
					 Table submarketstab = paxsrow.getColumn("submarkets").getTableColumn();
					 if(submarketstab!=null){
						 getMet(serviceList, submarketstab);
					 }								
					}
				}
				//退订产品金额
				response.setOriginalAmount(xmlOutput.getParm("totalmoney").getStringColumn());
				//退票手续费
				response.setRefundFee(xmlOutput.getParm("chargetotalmoney").getStringColumn());
				//退款金额
				response.setRefundAmount(xmlOutput.getParm("refundmoney").getStringColumn());
			}
		} 
		catch (Exception e) {
			 sadoc = LCCRefundRSDocument.Factory.newInstance();
			 rprs = sadoc.addNewLCCRefundRS();
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return sadoc;
	}


	/**
	 * 
	 * @param serviceList LCCServiceList
	 * @param submarketstab Table
	 */
	private void getMet(LCCServiceList serviceList, Table submarketstab) {
		for (int j = 0; j < submarketstab.getRowCount(); j++) {
			 Row submarketsrow = submarketstab.getRow(j);
			
			 LCCService service = serviceList.addNewLCCService();
			 //辅营编号
			 service.setServiceID(
				submarketsrow.getColumn("id")
				.getStringColumn());
			 //辅营类型(BAG行李 INS保险 MEAL餐食)
			 service.setServiceType(
				submarketsrow.getColumn("submarkettype")
				.getStringColumn());
			 //辅营名称
			 service.setServiceName(
				submarketsrow.getColumn("submarketname")
				.getStringColumn());
			 //单价
			 service.setUnitPrice(
				submarketsrow.getColumn("unitprice")
				.getStringColumn());
			 //单个退票手续费
			 service.setUnitFee(
				submarketsrow.getColumn("unitpricecharge")
				.getStringColumn());
			 //购买数量
			 service.setQuantity(
				submarketsrow.getColumn("buynum")
				.getStringColumn());
		 }
	}
}
