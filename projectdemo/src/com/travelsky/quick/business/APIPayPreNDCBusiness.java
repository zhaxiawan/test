package com.travelsky.quick.business;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirDocDisplayRSDocument;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo;
import org.iata.iata.edist.AirDocIssueRQDocument;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCPayPreRSDocument.LCCPayPreRS.Response.PayPreInfo;
import org.iata.iata.edist.LCCPayPreRSDocument.LCCPayPreRS.Response.PayPreInfo.OrderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DESUtil;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/** 
 * @author 作者:LiHz
 * @version 0.1
 * 类说明:
 *		线上支付接口
 */
@Service("LCC_PAYPRE_SERVICE")
public class APIPayPreNDCBusiness extends AbstractService<ApiContext>{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIPayPreNDCBusiness.class);
	
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取xml
			try{
				//转换 xml-->Reqbean
				transInputXmlToRequestBean();
				//获取ResponseBean
				context.setRet(getResponseBean());
			}
			//请求  xm转换CommandData 异常
			catch (APIException e) { 
				throw e;
			}
			catch (Exception e) {
				LOGGER.error(TipMessager.getInfoMessage(
						ErrCodeConstants.API_UNKNOW_PAY_PRE, 
						ApiServletHolder.getApiContext().getLanguage()), e);
				throw e;
			}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		//转换ResponseBean-->XmlBean
		return transRespBeanToXmlBean(commandRet,input);
	}

	
	
	
	/**
	 * 
	 * @param context SelvetContext
	 * @param xmlInput String
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		//银行编号
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		//LCCPayPreRQDocument rootDoc = null;
		AirDocIssueRQDocument rootDoc = null;
		rootDoc = AirDocIssueRQDocument.Factory.parse(xmlInput);
		AirDocIssueRQDocument.AirDocIssueRQ reqdoc = rootDoc.getAirDocIssueRQ();
		String orderno = reqdoc.getQuery().getTicketDocInfoArray(0).getOrderReference().getOrderID().getStringValue();
		String money = reqdoc.getQuery().getTicketDocInfoArray(0).getPayments().getPaymentArray(0).getAmount().getStringValue();
		String code = reqdoc.getQuery().getTicketDocInfoArray(0).getPayments().getPaymentArray(0).getAmount().getCode();
		//支付网关(adyen、worldpay)
		String pay_gateway = reqdoc.getQuery().getTicketDocInfoArray(0).getPayments().getPaymentArray(0).getType().getDefinition();
		if(!StringUtils.hasLength(pay_gateway)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_PAY_GATEWAY, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PAY_GATEWAY);
		}	
		//支付方式(五种支付方式)
		if(StringUtils.hasLength(pay_gateway) && "worldpay".equals(pay_gateway)){
			String pay_method = reqdoc.getQuery().getTicketDocInfoArray(0).getPayments().getPaymentArray(0).getType().getCode();
			if(!StringUtils.hasLength(pay_method)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_PAY_METHOD, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_PAY_METHOD);
			}	
			input.addParm("pay_method", pay_method);
		}
		//订单编号
		if(!StringUtils.hasLength(orderno)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_NO, language));
			 throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_NO);
		 }	
		input.addParm("pay_gateway", pay_gateway);
		input.addParm("money", money);
		input.addParm("orderno", orderno);
		input.addParm("code", code);
	}
	/**
	 * 数据提交shopping后台
	 * @param input  请求的XML参数
	 * @param context 用于调用doOther请求后台数据
	 * @return  请求后台返回的对象
	 */
	public CommandRet getResponseBean() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
//		orderOpManager.orderpay(input, context);
		return orderOpManager.edyenpay(input, context);
	}
	
	/**
	 * 拼接B2C所用的XML
	 * @param commandRet 后台返回的结果集
	 * @param input  B2C请求的XML
	 * @return  请求后台返回的对象
	 */
	public XmlObject transRespBeanToXmlBean(Object commandRet ,CommandData input) {
		CommandRet xmlOutput = (CommandRet)commandRet;
		AirDocDisplayRSDocument doc = AirDocDisplayRSDocument.Factory.newInstance();
		AirDocDisplayRSDocument.AirDocDisplayRS rs = doc.addNewAirDocDisplayRS();
		try{
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}
			 //反回正确的值
			else{   
				//url
				//CommandData commandData = xmlOutput.getParm("pay").getObjectColumn();
//				String stringColumn = tableColumn.getRow(0).getColumn("hppurl").getStringColumn();
//				String url = xmlOutput.getParm("pay").getTableColumn().getRow(0).getColumn("hppurl").getStringColumn();
				String pay = new String(Base64.encodeBase64(xmlOutput.getParm("pay").toString().getBytes()));
				String status = xmlOutput.getParm("status").getStringColumn();
				rs.addNewSuccess();
				TicketDocInfo ticketDocInfo = rs.addNewResponse().addNewTicketDocInfos().addNewTicketDocInfo();
				ticketDocInfo.addNewPayments().addNewPayment().addNewQualifier().setLink(pay);
				ticketDocInfo.addNewOrderReference().addNewBookingReference().addNewType().setCode(status);
			}
		} 
		catch (Exception e) {
			//初始化XML节点
			doc = AirDocDisplayRSDocument.Factory.newInstance();
			rs = doc.addNewAirDocDisplayRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}
	
	/**
	 * XML放值
	 * @param xmlOutput XML
	 * @param payPreInfo 节点
	 */
	private static void setPayPreInfo(CommandRet xmlOutput,PayPreInfo payPreInfo){
		//支付跳转URL
		String payurl = xmlOutput.getParm("payurl").getStringColumn();
		//组织编号
		String orgId = xmlOutput.getParm("OrgId").getStringColumn();	
		//版本号
		String version = xmlOutput.getParm("Version").getStringColumn();
		//渠道号
		String appType = xmlOutput.getParm("AppType").getStringColumn();	
		//银行编号
		String bankId = xmlOutput.getParm("BankId").getStringColumn();	
		//账单流水号
		String billNo = xmlOutput.getParm("BillNo").getStringColumn();	
		//回调编号
		String returnId = xmlOutput.getParm("ReturnId").getStringColumn();
		//语言(CN简体中文)
		String lan = xmlOutput.getParm("Lan").getStringColumn();	
		//支付类型
		String paytype = xmlOutput.getParm("Paytype").getStringColumn();
		//用户编号
		String usrid = xmlOutput.getParm("usrid").getStringColumn();	
		//用户姓名
		String username = xmlOutput.getParm("username").getStringColumn();	
		//网关编号
		String gateid = xmlOutput.getParm("gateid").getStringColumn();	
		//机票金额
		String ticketamount = xmlOutput.getParm("Ticketamount").getStringColumn();	
		//保险费
		String insurance = xmlOutput.getParm("Insurance").getStringColumn();	
		//税费
		String tax = xmlOutput.getParm("Tax").getStringColumn();	
		//其他费用
		String otherfee = xmlOutput.getParm("Otherfee").getStringColumn();
		//支付流水号
		String msg = xmlOutput.getParm("Msg").getStringColumn();
		//加密串
		String signature = xmlOutput.getParm("SIGNATURE").getStringColumn();
		//语言(CN简体中文)
		payPreInfo.setLanguage(lan);
		//支付类型
		payPreInfo.setType(paytype);
		//支付跳转URL
		payPreInfo.setUrl(payurl);
		//组织编号
		payPreInfo.setOrgId(orgId);
		//渠道号
		payPreInfo.setChannelNo(appType);
		payPreInfo.setVersionNo(version);
		payPreInfo.setBankId(bankId);
		payPreInfo.setBillNo(billNo);
		payPreInfo.setReturnId(returnId);
		payPreInfo.setUserId(usrid);
		payPreInfo.setUserName(username);
		payPreInfo.setGateId(gateid);
		payPreInfo.setTicketAmount(ticketamount);
		payPreInfo.setInsurance(insurance);
		payPreInfo.setTaxe(tax);
		payPreInfo.setOtherFee(otherfee);
		payPreInfo.setSignature(signature);
		//支付流水号
		payPreInfo.setSerialNo(msg);
	}
	
	/**
	 * XML放值
	 * @param xmlOutput XML
	 * @param orderInfo 节点
	 */
	private static void setOrderInfo(CommandRet xmlOutput,OrderInfo orderInfo){
		//订单号
		String orderNo = xmlOutput.getParm("OrderNo").getStringColumn();	
		//支付金额
		String orderAmount = xmlOutput.getParm("OrderAmount").getStringColumn();	
		//支付日期(格式yyyyMMdd)
		String orderDate = xmlOutput.getParm("OrderDate").getStringColumn();	
		//支付时间(格式HHmmss)
		String orderTime = xmlOutput.getParm("OrderTime").getStringColumn();	
		//货币类型(CNY人民币)
		String orderCurtype = xmlOutput.getParm("OrderCurtype").getStringColumn();	
		//订单类型(WEB网上订单)
		String orderType = xmlOutput.getParm("OrderType").getStringColumn();	
		//订单名称
		String ordername = xmlOutput.getParm("ordername").getStringColumn();	
		//订单信息
		String orderinfo = xmlOutput.getParm("Orderinfo").getStringColumn();	
		//订单号
		orderInfo.setID(orderNo);
		//支付金额
		orderInfo.setAmount(orderAmount);
		orderInfo.setDate(orderDate);
		orderInfo.setTime(orderTime);
		//货币类型
		orderInfo.setCurrencyCode(orderCurtype);
		orderInfo.setType(orderType);
		orderInfo.setName(ordername);
		//订单信息（备注）
		orderInfo.setRemark(orderinfo);
	}
}
