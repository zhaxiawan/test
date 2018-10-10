package com.travelsky.quick.business;

 

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.OrderChangeRQDocument;
import org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ.Query.Order;
import org.iata.iata.edist.OrderItemAssociationType;
import org.iata.iata.edist.OrderItemRepriceType.OrderItem;
import org.iata.iata.edist.OrderViewRSDocument;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS;
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
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;
 
/**
 * 
 * @author MaRuifu 2016年5月3日下午3:15:39
 * @version 0.1
 * 类说明:提交座位
 */
@Service("LCC_COMMITSEAT_SERVICE")
public class APISeatSubmitNDCBusiness extends AbstractService<ApiContext> {

	private static final long serialVersionUID = 269894134072413540L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APISeatSubmitNDCBusiness.class);
	
	/**
	 * @param context SelvetContext<ApiContext>
	 * @throws  APIException APIRuntimeException
	 * @throws  Exception XmlException
	 */
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
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
					ErrCodeConstants.API_UNKNOW_COMMIT_SEAT, 
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}
	
	
	  /**
	   * @param input CommandData
	   * @param context SelvetContext
	   * @return CommandRet
	   */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.setseat(input, context);
	}
	/**
	 * 转换 xml-->Reqbean
	 * @param context SelvetContext
	 * @param xmlInput String
	 * @throws APIException APIException 
	 * @throws Exception Exception 
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		OrderChangeRQDocument rootDoc = null;
		rootDoc = OrderChangeRQDocument.Factory.parse(xmlInput);
		OrderChangeRQDocument.OrderChangeRQ reqDoc = rootDoc.getOrderChangeRQ();
		// 部门ID
		String channelNo = context.getContext().getChannelNo();
		//渠道号
		input.addParm("channelno", channelNo);
		input.addParm("tktdeptid",context.getContext().getTicketDeptid());
		// 添加用户id  orderno
		input.addParm("memberid", context.getContext().getUserID());
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		//订单
		Order order = reqDoc.getQuery().getOrder();	
		if (order==null) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER);
		}
		//订单编号
		String orderno = order.getOrderID().getStringValue();
		String owner = order.getOrderID().getOwner();
		if (!StringUtils.hasLength(orderno)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_ID, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_ID);
		}
		//9 19提交座位信息入参变化
		Table seats = new Table(new String[]{"paxfltid", "seat"});
		OrderItem[] orderItems = order.getOrderItems().getOrderItemArray();
		for (int i = 0; i < orderItems.length; i++) {
			OrderItem item = order.getOrderItems().getOrderItemArray(i);
			String id = item.getOrderItemID().getStringValue();
			/*OrderItemAssociationType Associations = item.getAssociations();
			 String surname = Associations.getPassengers().getPassengerArray(0).getRefs().toString();
			//旅客记录ID
			//String surname = Associations.getPassengers().getPassengerArray(0).getName().getSurname().getStringValue();
			//--航班记录ID
			String flightid = Associations.getFlight().getSegmentReferencesArray(0).getStringValue();*/
			//<!-- 座位编号 -->
			String seatNumber = item.getSeatItem().getSeatReferenceArray(0);
			seats = getSeatTable(id,seatNumber,seats);
			input.addParm("seats", seats);
		}
		input.addParm("memberid", context.getContext().getUserID());
		input.addParm("orderno", orderno);
		input.addParm("owner", owner);
	}
	/**
	 * @param paxid String
	 * @param flightid String
	 * @param seat String
	 * @param seats 
	 * @return Table
	 */
	private Table getSeatTable(String mike, String seat, Table seats) {
		Row seatRow = seats.addRow();
		seatRow.addColumn("paxfltid", mike);
		seatRow.addColumn("seat", seat);
		return seats;
	}


	/**
	 * 转换 xml-->Reqbean
	 * @param xmlOutput CommandRet
	 * @param input  CommandData
	 * @return XmlObject
	 */
	
	public XmlObject transResponseBeanToXmlBean(CommandRet xmlOutput ,CommandData input) {
		OrderViewRSDocument sadoc = OrderViewRSDocument.Factory.newInstance();
		OrderViewRS rprs = sadoc.addNewOrderViewRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}else{
				rprs.addNewSuccess();
				rprs.addNewDocument();
				/*String owner = input.getParm("owner").getStringColumn();
				String orderno = input.getParm("orderno").getStringColumn();
				Response response = rprs.addNewResponse();
			//	response.addNewOrderViewProcessing();
				response.addNewPassengers();
				org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.
				Order order = response.addNewOrder();
				OrderIDType orderID = order.addNewOrderID();
				
				orderID.setOwner(owner);
				orderID.setStringValue(orderno);
				ItemIDType orderItemID = order.addNewOrderItems().addNewOrderItem().addNewOrderItemID();
				orderItemID.setOwner(owner);
				orderItemID.setStringValue(orderno);*/
			}
		}
		catch (Exception e) {
			sadoc = OrderViewRSDocument.Factory.newInstance();   
			rprs = sadoc.addNewOrderViewRS();                    
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return sadoc;
	}
}
