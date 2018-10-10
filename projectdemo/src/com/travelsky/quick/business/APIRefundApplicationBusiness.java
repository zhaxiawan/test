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
import org.iata.iata.edist.MediaAttachmentType;
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

import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 
 * @author MaRuifu 2016年5月4日下午4:20:42
 * @version 0.1
 * 	类说明:退票申请
 */
@Service("LCC_REFUNDSUBMIT_SERVICE")
public class APIRefundApplicationBusiness extends AbstractService<ApiContext> {

	private static final long serialVersionUID = -737127756393584805L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APIRefundApplicationBusiness.class);
	
	/**
	 * dsfsd
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
					ErrCodeConstants.API_UNKNOW_REFUNDAPPLICATION, 
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
		
	}

	/**
	 * 
	 * @param input CommandData
	 * @param context SelvetContext
	 * @return CommandRet 
	 */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		if ("1".equals(input.getParm("status").getStringColumn())) {
			return orderOpManager.updateFile(input,context);
		}else{
			return orderOpManager.refund(input,context);
		}
	}
	/**
	 * 转换 xml-->Reqbean
	 * @param xmlInput String
	 * @param context SelvetContext
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
		
		//退票申请信息
		LCCRefund refund = reqDoc.getQuery().getLCCRefund();
		//当为1时 表示再次提交申请
		String status = refund.getStatus();
		input.addParm("status", status);
		//订单号
		String orderno = refund.getOrderID()==null?null:
			refund.getOrderID().getStringValue();	
		String channelNo = context.getContext().getChannelNo();
		//渠道号
		input.addParm("channelno", channelNo);
		input.addParm("tktdeptid",context.getContext().getTicketDeptid());
		// 添加用户id  orderno
		input.addParm("memberid", context.getContext().getUserID());
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		if (!StringUtils.hasLength(orderno)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER, language));
			throw APIException.
			getInstance(ErrCodeConstants.API_NULL_ORDER);
		}else{
			input.addParm("orderno",orderno);
		}
		//退票类型
		String refundtype =refund.getType();
		if(!StringUtils.hasLength(refundtype)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REFUNDTYPE, language));
			throw APIException.
			getInstance(ErrCodeConstants.API_NULL_REFUNDTYPE);
		}
		//退票原因类型
		String refundreasontype = refund.getReason();
		if(StringUtils.hasLength(refundreasontype)){
			input.addParm("refundreasontype", refundreasontype);
		}
		//退票原因说明
		String refundreason = refund.getDescription();
		if(StringUtils.hasLength(refundreason)){
			input.addParm("refundreason", refundreason);
		}
		//退票申请人
		String applicant = refund.getApplicant();
		if(!StringUtils.hasLength(applicant)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REFUNDAPPLICANT, language));
			throw APIException.getInstance(
					ErrCodeConstants.API_NULL_REFUNDAPPLICANT);
		}
		getRefInfo(input, reqDoc, refund, refundtype, applicant);
		
	}





	/** 
	 * 
	 * @param input
	 * @param reqDoc
	 * @param refund
	 * @param refundtype
	 * @param applicant 
	 * void    返回类型 
	 * 
	 */
	private void getRefInfo(CommandData input,
			LCCRefundRQDocument.LCCRefundRQ reqDoc, LCCRefund refund,
			String refundtype, String applicant) throws APIException {
		//退票申请人电话号码 
		String telephone = refund.getTelephone();
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
//		if(!StringUtils.hasLength(telephone)){
//			LOGGER.info(TipMessager.getInfoMessage(
//					ErrCodeConstants.API_NULL_REFUNDTELEPHONE, language));
//			throw APIException.getInstance(
//					ErrCodeConstants.API_NULL_REFUNDTELEPHONE);
//		}
		//退票金额
		String refundam = refund.getTotalAmount();
		if(!StringUtils.hasLength(refundam)){
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_REFUNDTOTALAMOUNT, language));
			throw APIException.getInstance(
				ErrCodeConstants.API_NULL_REFUNDTOTALAMOUNT);
		}
		
		MediaAttachmentType[] mediaAttachmentTypes = refund.getAttachmentArray();
		Table attachments = new Table(new String[]{"fileid","filename"});
		if(mediaAttachmentTypes!=null){
			for (int i = 0; i < mediaAttachmentTypes.length; i++) {
				String fileid = refund.getAttachmentArray(i).getAttachmentURI().getStringValue();
				String filename = refund.getAttachmentArray(i).getDescription();
				Row row = attachments.addRow();
				row.addColumn("fileid", fileid);
				row.addColumn("filename", filename);
			}
		}
		if(attachments.getRowCount()>0){
			input.addParm("attachments", attachments);
		}
		LCCRefundDetail[] refundDetailArray = refund.getLCCRefundDetailArray();
		Table refundDetailtab = new Table(new String[]{"offers"});
		Table offers = new Table(new String[]{"serviceid","servicetype"});
		if(refundDetailArray!=null){
			for (int i = 0; i < refundDetailArray.length; i++) {
				LCCRefundDetail detailArray = refund.getLCCRefundDetailArray(i);
				String serviceid = detailArray.getFlightID();
				if(!StringUtils.hasLength(serviceid)){
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_PAXFLIGHTID_ISNULL, language));
					throw APIException.getInstance(
					 ErrCodeConstants.API_PAXFLIGHTID_ISNULL);
				}
				Row row = offers.addRow();
				row.addColumn("serviceid", serviceid);
				row.addColumn("servicetype", "FARE");
			}
		}
		Row rrow = refundDetailtab.addRow();
		rrow.addColumn("offers", offers);
		input.addParm("memberid", ApiServletHolder.get().getContext().getUserID());
		input.addParm("refundtype", refundtype);
		input.addParm("applicant", applicant);
		input.addParm("telephone", telephone);
		input.addParm("refundam", refundam);
		input.addParm("paxs", refundDetailtab);
	}
	/**
	 * 转换 xml-->Reqbean
	 * @param xmlOutput CommandRet
	 * @param input CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(
			CommandRet xmlOutput ,CommandData input){
		//为提交附件的兼容
		LCCRefundRSDocument sadoc = LCCRefundRSDocument.Factory.newInstance();
		LCCRefundRS rprs = sadoc.addNewLCCRefundRS();
		if (!"1".equals(input.getParm("status").getStringColumn())) {
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
					Table paxstable = xmlOutput.getParm("paxs").getTableColumn();
					if(paxstable!=null){
						for (int i = 0; i < paxstable.getRowCount(); i++) {
							Row paxrow = paxstable.getRow(i);
							org.iata.iata.edist.LCCRefundRSDocument.LCCRefundRS.Response.
							LCCRefund refund = response.addNewLCCRefund();
							//乘机人编号
							refund.setPaxID(paxrow.getColumn("paxid").getStringColumn());
							//航班编号
							refund.setFlightID(paxrow.getColumn("flightid").getStringColumn());
							//机票价格
							refund.setTicketPrice(paxrow.getColumn("txtprice").getStringColumn());
							//机场建设费
							refund.setCNPrice(paxrow.getColumn("cnprice").getStringColumn());
							//燃油附加费
							refund.setYQPrice(paxrow.getColumn("yqprice").getStringColumn());
							//选座费
							refund.setSeatPrice(paxrow.getColumn("seatprice").getStringColumn());
							//机票手续费
							refund.setTicketFee(paxrow.getColumn("txtcharge").getStringColumn());
							//机场建设费手续费
							refund.setCNFee(paxrow.getColumn("cncharge").getStringColumn());
							//燃油附加费手续费
							refund.setYQFee(paxrow.getColumn("yqcharge").getStringColumn());
							//选座费手续费
							refund.setSeatFee(paxrow.getColumn("seatcharge").getStringColumn());
							LCCServiceList serviceList = refund.addNewLCCServiceList();
							//辅营退费信息
							Table subtable = paxrow.getColumn("submarkets").getTableColumn();
							if(subtable!=null){
								getMet(serviceList, subtable);
							}
						}
					}
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
		}else{
			try {
				String errorcode = xmlOutput.getErrorCode();
				if(StringUtils.hasLength(errorcode)){
					ErrorType error = rprs.addNewErrors().addNewError();
					error.setCode(TipMessager.getErrorCode(errorcode));
					error.setStringValue(TipMessager.getMessage(errorcode,
							ApiServletHolder.getApiContext().getLanguage()));
				}else{
					rprs.addNewSuccess();
				}
				} catch (Exception e) {
				 sadoc = LCCRefundRSDocument.Factory.newInstance();
				 rprs = sadoc.addNewLCCRefundRS();
				// 存在错误信息
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
				// 错误描述
				error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
						ApiServletHolder.getApiContext().getLanguage()));
			}
		}
		return sadoc;
	}




	/**
	 * 
	 * @param serviceList LCCServiceList
	 * @param subtable Table
	 */
	private void getMet(LCCServiceList serviceList, Table subtable) {
		for (int j = 0; j <subtable.getRowCount(); j++) {
			Row row = subtable.getRow(j);
			LCCService service = serviceList.addNewLCCService();
			//辅营编号
			service.setServiceID(
			row.getColumn("id").getStringColumn());
			//辅营类型
			service.setServiceType(
			row.getColumn("submarkettype").getStringColumn());
			//辅营名称---对国际化的兼容 
			String language = ApiServletHolder.getApiContext().getLanguage();
			CommandData data = row.getColumn("submarketname").getObjectColumn();
			String name="";
			if (!"SEAT".equals(service.getServiceType())) {
				if (!"".equals(data)) {
					 name = data.getParm(language).getStringColumn();
				}
			}else {
				 name = row.getColumn("submarketname").getStringColumn();
			}
			service.setServiceName(name);
			//单价
			service.setUnitPrice(
			row.getColumn("unitprice").getStringColumn());
			//单个退票手续费
			service.setUnitFee(
			row.getColumn("unitpricecharge").getStringColumn());
			//购买数量
			service.setQuantity(
			row.getColumn("buynum").getStringColumn());
		}
	}
}
