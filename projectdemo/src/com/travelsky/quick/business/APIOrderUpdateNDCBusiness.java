package com.travelsky.quick.business;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.BaggageItemType;
import org.iata.iata.edist.OrderChangeRQDocument;
import org.iata.iata.edist.OrderIDType;
import org.iata.iata.edist.OtherItemType;
import org.iata.iata.edist.SeatItemDocument.SeatItem;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceIDType;
import org.iata.iata.edist.BaggageItemType.Services;
import org.iata.iata.edist.OrderChangeRQDocument.OrderChangeRQ.Query.Order;
import org.iata.iata.edist.OrderItemRepriceType.OrderItem;
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
 * @author 作者:LiHz
 * @version 0.1
 * 类说明:
 *		订单提交接口
 *
 */
@Service("LCC_ORDERUPDATE_SERVICE")
public class  APIOrderUpdateNDCBusiness   extends AbstractService<ApiContext>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3574094130002075979L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIOrderUpdateNDCBusiness.class);
	private APIOrderDetailNDCBusiness orderDetailNDC = null;



	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取xml
		
			
			//转换 xml-->Reqbean
			try{
				transInputXmlToRequestBean();
				//获取ResponseBean
				context.setRet(getResponseBean());
			}
			catch (APIException e) {
				throw e;
			}
			catch (Exception e) {
				LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_ORDER_CHANGE, ApiServletHolder.getApiContext().getLanguage()), e);
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
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		OrderChangeRQDocument rootDoc = null;
		rootDoc = OrderChangeRQDocument.Factory.parse(xmlInput);
		OrderChangeRQDocument.OrderChangeRQ reqdoc = rootDoc.getOrderChangeRQ();
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		//1  订单节点
		Order order = reqdoc.getQuery().getOrder();
		if (order == null) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER);
		}		String orderNo = order.getOrderID().getStringValue();
		//2  订单编号
		OrderIDType  orderIDType = order.getOrderID();
		if (orderIDType == null || !StringUtils.hasLength(orderIDType.getStringValue())) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_NO, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_NO);
		}
		//3  封装请求参数
		if(reqdoc.getDataLists() != null){
			/****** 封装座位信息 ******/
			if(reqdoc.getDataLists().getSeatList() != null && 
					reqdoc.getDataLists().getSeatList().getSeatsArray() != null && 
					reqdoc.getDataLists().getSeatList().getSeatsArray().length > 0){
				Table seats = new Table(new String[]{"paxfltid", "seat"});
				OrderItem[] orderItems = order.getOrderItems().getOrderItemArray();
				for (int i = 0; i < orderItems.length; i++) {
					OrderItem item = order.getOrderItems().getOrderItemArray(i);
					String id = item.getOrderItemID().getStringValue();
					SeatItem seatItem = item.getSeatItem();
					if(null != seatItem 
							&& null != seatItem.getSeatReferenceArray()
							&& seatItem.getSeatReferenceArray().length > 0){
						
						String seatNumber = seatItem.getSeatReferenceArray(0);
						seats = getSeatTable(id,seatNumber,seats);
						input.addParm("seats", seats);
					}
				}
			}
			/****** 封装附加服务信息  ******/
			if(reqdoc.getDataLists().getServiceList() != null && 
					reqdoc.getDataLists().getServiceList().getServiceArray() != null && 
					reqdoc.getDataLists().getServiceList().getServiceArray().length > 0){
				
				OrderItem[] orderItemArray = order.getOrderItems().getOrderItemArray();
				ServiceList serviceList = reqdoc.getDataLists().getServiceList();
				ServiceDetailType[] serviceArray = serviceList.getServiceArray();
				Table submarketTable = new Table(new String[]{"price","submarketcode",
						"submarkettype","buynum","paxfltid"});
				String submarkettype = getSubmarket(serviceArray, submarketTable, orderItemArray);
				input.addParm("submarkettype", submarkettype);
				input.addParm("submarkets", submarketTable);
			}
		}
		
		input.addParm("orderno", orderNo);
		input.addParm("memberid", context.getContext().getUserID());
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
		//附营接口
		Table lSubmarkets = input.getParm("submarkets").getTableColumn();
		if(lSubmarkets != null && lSubmarkets.getRowCount() > 0){
			return orderOpManager.setsubmarket(context.getInput(), context);
		}
		// 选座接口
		Table lSeats = input.getParm("seats").getTableColumn();
		if(lSeats != null && lSeats.getRowCount() > 0){
			return orderOpManager.setseat(input, context);
		}
		//确认订单接口
		return orderOpManager.submit(input, context);
	}
	
	/**
	 * 拼接B2C所用的XML
	 * @param commandRet 后台返回的结果集
	 * @param input  B2C请求的XML
	 * @return  请求后台返回的对象
	 */
	public XmlObject transRespBeanToXmlBean(CommandRet commandRet ,CommandData input){
		orderDetailNDC = new APIOrderDetailNDCBusiness();
		return orderDetailNDC.transResponseBeanToXmlBean(commandRet, input);
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
		for (ServiceDetailType service : serviceArray) {
			String submarketcode = service.getServiceID().getStringValue();
			String name = service.getEncoding().getCode().getStringValue();
			if("".equals(name)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_SUBMARKET, ApiServletHolder.getApiContext().getLanguage()));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_SUBMARKET_CODE);
			}
			if(!submarkettype.contains(name)){
				submarkettype = submarkettype+","+name;
			}
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