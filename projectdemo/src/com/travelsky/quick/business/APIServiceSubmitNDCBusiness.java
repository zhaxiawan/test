package com.travelsky.quick.business;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.BaggageItemType;
import org.iata.iata.edist.BaggageItemType.Services;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.OrderChangeRQDocument;
import org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ.Query.Order;
import org.iata.iata.edist.OrderIDType;
import org.iata.iata.edist.OrderItemRepriceType.OrderItem;
import org.iata.iata.edist.OrderViewRSDocument;
import org.iata.iata.edist.OtherItemType;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceIDType;
import org.iata.iata.edist.ServiceListDocument.ServiceList;
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
 * 
 * @author MaRuifu 2016年5月3日下午3:14:39
 * @version 0.1
 * 类说明:提交辅营
 */
@Service("LCC_SERVICESUBMIT_SERVICE")
public class APIServiceSubmitNDCBusiness extends AbstractService<ApiContext> {
	private static final long serialVersionUID = -1270136248566029930L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APIServiceSubmitNDCBusiness.class);
	
	/**
	 * 
	 * @param context SelvetContext
	 * @throws Exception Exception
	 */
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		
		
		try {
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
					ErrCodeConstants.API_UNKNOW_AUXILIARY_SUBMIT, 
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}
	/**
	 * 
	 * 
	 * @param context SelvetContext<ApiContext>
	 * @return 
	 * CommandRet    返回类型 
	 *
	 */
	private CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.setsubmarket(context.getInput(), context);
	}
	
	/**
	 * 将底层系统返回的CommandRet转换为XmlObject.<br>
	 */
	/**
	 * 转换 xml-->Reqbean
	 * @param commandRet CommandRet
	 * @param input  CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		OrderViewRSDocument doc = OrderViewRSDocument.Factory.newInstance();
		OrderViewRSDocument.OrderViewRS root = doc.addNewOrderViewRS();
		
		
		// 判断是否存在错误信息
		String errCode = commandRet.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			
			return doc;
		}
		
		/* 不存在错误信息 */
		root.addNewSuccess();
		/*Response response = root.addNewResponse();
		response.addNewPassengers();
		String orderno = input.getParm("orderno").getStringColumn();
		org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Order order = 
				response.addNewOrder();
		order.addNewOrderID().setStringValue(orderno);
		order.addNewOrderItems().addNewOrderItem().addNewOrderItemID().setStringValue(orderno);*/
		
//		// 订单号
//		Item orderNoItem = ;
//		String orderNo = orderNoItem.getStringColumn();
//		// 航空公司
//		Item airlineItem = input.getParm("airline");
//		String airline =  airlineItem.getStringColumn();
//		
//		Response response = root.addNewResponse(); 
//		response.addNewOrderViewProcessing();
//		response.addNewPassengers();
//		org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Order order = response.addNewOrder();
//		OrderIDType orderID = order.addNewOrderID();
//		orderID.setStringValue(orderNo);
//		orderID.setOwner(airline);
//		order.addNewOrderItems().addNewOrderItem();
		
		return doc;
	}
	
	
	
	/**
	 * 
	 * 
	 * @param context SelvetContext<ApiContext>
	 * @param xmlInput String
	 * @throws APIException APIException
	 * @throws Exception Exception
	 * void    返回类型 
	 *
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		OrderChangeRQDocument rootDoc = null;
		rootDoc = OrderChangeRQDocument.Factory.parse(xmlInput);
		OrderChangeRQDocument.OrderChangeRQ reqDoc = rootDoc.getOrderChangeRQ();
		// 部门ID
		String deptno =context.getContext().getTicketDeptid();
		input.addParm("tktdeptid",deptno);	
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		input.addParm("memberid", context.getContext().getUserID());
		
//		//航空公司
		Order order = reqDoc.getQuery().getOrder();
		if (order == null) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER);
		}
		// 订单编号
		OrderIDType  orderIDType = order.getOrderID();
		if (orderIDType == null || !StringUtils.hasLength(orderIDType.getStringValue())) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_NO, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_NO);
		}
		String orderId =orderIDType.getStringValue();
		input.addParm("orderno", orderId);
		
//		ItemIDType orderItemID = order.getOrderItems().getOrderItemArray(0).getOrderItemID();
//		input.addParm("submarkettype", orderItemID.getStringValue());
		OrderItem[] orderItemArray = order.getOrderItems().getOrderItemArray();
		
		ServiceList serviceList = reqDoc.getDataLists().getServiceList();
		
		ServiceDetailType[] serviceArray = serviceList.getServiceArray();
		if (serviceList == null 
				|| serviceArray 
				== null || serviceArray.length < 1) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_SUBMARKET, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_SUBMARKET);
		}
		
		Table submarketTable = new Table(new String[]{"price","submarketcode",
				"submarkettype","buynum","paxfltid"});
		String submarkettype = getSubmarket(serviceArray, submarketTable, orderItemArray);
		input.addParm("submarkettype", submarkettype);
		input.addParm("submarkets", submarketTable);
	}
	/**
	 * 
	 * @param serviceArray 
	 * @param strUtils StringUtils
	 * @param auliliaryExtMap Map
	 * @param orderItemArr OrderItem[]
	 * @param submarketTable Table
	 * @param orderItemArray 
	 */
	private String getSubmarket(ServiceDetailType[] serviceArray, Table submarketTable, OrderItem[] orderItemArray)
		throws APIException{
		String submarkettype = "";
		Map<String, String> submarketMap = new HashMap<String, String>(); 
//		for (Map.Entry<String, String> entry : submarketMap.entrySet()) {
//			entry.getKey();
//			entry.getValue();
//		}
		for (ServiceDetailType service : serviceArray) {
			String submarketcode = service.getServiceID().getStringValue();
//			String order = service.getObjectKey().toString();
//			String orderid= order.substring(0, order.length()-submarketcode.length());
//			Row row = submarketTable.addRow();
//			row.addColumn("paxfltid", orderid);
//			row.addColumn("submarketcode", submarketcode);
//			String name = service.getName().getStringValue();
			String name = service.getEncoding().getCode().getStringValue();
			if("".equals(name)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_SUBMARKET, ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_SUBMARKET_CODE);
			}
			if(!submarkettype.contains(name)){
				submarkettype = submarkettype+","+name;
			}
//			row.addColumn("submarkettype", name);
//			row.addColumn("price", service.getPriceArray(0).getTotal().getStringValue());
//			Associations[] associationsArray = service.getAssociationsArray();
//			Associations associations = associationsArray[0];
//			Traveler traveler = associations.getTraveler();
//			row.addColumn("paxid", traveler.getTravelerReferences().get(0).toString());	
//			org.iata.iata.edist.ServiceAssocType.Flight flight = associations.getFlight();
//			row.addColumn("flightid", flight.getSegmentReferencesArray(0).getStringValue());		
//			Offer offer = associations.getOffer();
//			row.addColumn("familycode", offer.getOfferReferences().get(0).toString());
//			BigInteger buynum = service.getDetail().getServiceItemQuantityRules().getMaximumQuantity();
//			row.addColumn("buynum", buynum.toString());	
			submarketMap.put(submarketcode, name);
		}
		for (OrderItem order : orderItemArray) {
			Map<String, Integer> codeMap = new HashMap<String, Integer>(); 
			String paxfltid = order.getOrderItemID().getStringValue();
			BaggageItemType baggageItem = order.getBaggageItem();
			if(baggageItem != null){
				Services services = baggageItem.getServices();
				ServiceIDType[] serviceIDArray = services.getServiceIDArray();
				for (ServiceIDType serviceID : serviceIDArray) {
					String code = serviceID.getStringValue();
					if(codeMap.containsKey(code)){
						codeMap.put(code, codeMap.get(code)+1);
					}else{
						codeMap.put(code, 1);
					}
				}
			}
			OtherItemType otherItem = order.getOtherItem();
			if(otherItem != null){
				ServiceIDType[] serviceIDArrays = otherItem.getServices().getServiceIDArray();
				for (ServiceIDType type : serviceIDArrays) {
					String subCode = type.getStringValue();
					if(codeMap.containsKey(subCode)){
						codeMap.put(subCode, codeMap.get(subCode)+1);
					}else{
						codeMap.put(subCode, 1);
					}
				}
			}
			for (Map.Entry<String, String> entry : submarketMap.entrySet()) {
				String subcode = entry.getKey();
				String subtype = entry.getValue();
				if(codeMap.containsKey(subcode)){
					Row row = submarketTable.addRow();
					row.addColumn("price", 1000);
					row.addColumn("paxfltid", paxfltid);
					row.addColumn("submarketcode", subcode);
					row.addColumn("submarkettype", subtype);
					row.addColumn("buynum", codeMap.get(subcode));	
				}
			}
		}
		return submarkettype.substring(1, submarkettype.length());
	}
}
