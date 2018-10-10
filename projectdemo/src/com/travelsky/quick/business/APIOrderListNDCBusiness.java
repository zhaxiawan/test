package com.travelsky.quick.business;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.values.XmlAnyTypeImpl;
import org.iata.iata.edist.BookingReferenceType;
import org.iata.iata.edist.BookingReferencesDocument.BookingReferences;
import org.iata.iata.edist.CodesetType;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightDetailType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.LCCFlightAirportType;
import org.iata.iata.edist.MarketingCarrierType;
import org.iata.iata.edist.MarketingMessagesDocument.MarketingMessages.MarketMessage;
import org.iata.iata.edist.OrderListProcessType;
import org.iata.iata.edist.OrderListRQDocument;
import org.iata.iata.edist.OrderListRQDocument.OrderListRQ.Query;
import org.iata.iata.edist.OrderListRQDocument.OrderListRQ.Query.Filters;
import org.iata.iata.edist.OrderListRQDocument.OrderListRQ.Query.Filters.CreateDateRange;
import org.iata.iata.edist.OrderListRSDocument;
import org.iata.iata.edist.OrderListRSDocument.OrderListRS;
import org.iata.iata.edist.OrderListRSDocument.OrderListRS.Response.Orders;
import org.iata.iata.edist.OrderListRSDocument.OrderListRS.Response.Orders.Order;
import org.iata.iata.edist.OrderListRSDocument.OrderListRS.Response.Orders.Order.FlightSegmentList;
import org.iata.iata.edist.OrderListRSDocument.OrderListRS.Response.Orders.Order.FlightSegmentList.FlightSegment;
import org.iata.iata.edist.OrderListRSDocument.OrderListRS.Response.Orders.Order.Passengers;
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
import com.travelsky.quick.util.StatusUtil;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 类说明:订单列表(订单查询接口)
 * 
 * @author huxizhun
 *
 */
@Service("LCC_ORDERLIST_SERVICE")
public class APIOrderListNDCBusiness extends AbstractService<ApiContext> {
	private static final long serialVersionUID = -1270136248566029930L;
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(APIOrderListNDCBusiness.class);
	
	private static final String  ZERO = "0";
	private static final String  ONE = "1";
	private static final String  TWO = "2";

	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResult());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_ORDER_QUERY,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	private CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.queryforNDC(input, context);
	}

	/**
	 * 将底层系统返回的CommandRet转换为XmlObject.<br>
	 * 相关CommandRet参数请参考订单列表文档
	 * 
	 * @param commandRet
	 *            CommandRet
	 * @param input
	 *            CommandData
	 * @return XmlObject 请求后的XML
	 */
	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		OrderListRSDocument doc = OrderListRSDocument.Factory.newInstance();
		OrderListRS root = doc.addNewOrderListRS();
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
		// Response->Orders
		root.addNewSuccess();
		Orders orders = root.addNewResponse().addNewOrders();
		
		/**************  订单列表分页信息************/
		OrderListProcessType orderListProcessType=root.getResponse().addNewOrderListProcessing();
		MarketMessage marketMessage=orderListProcessType.addNewMarketingMessages().addNewMarketMessage();
		marketMessage.addNewText().setStringValue(commandRet.getParm("recordnum").getStringColumn());
		marketMessage.setMarkupStyle(commandRet.getParm("pagesize").getStringColumn());
		marketMessage.setLink(commandRet.getParm("pagenum").getStringColumn());
		
		/**************  订单列表*******************/
		// 遍历定单列表
		Table orderTable=commandRet.getParm("order").getTableColumn();
		if(orderTable != null && orderTable.getRowCount() > 0){
			//用于存放当前航班下当前人下的退票状态(key值为航班id+人id，value为具体状态)
			Map<String, String> refundPaxStatusMap = new HashMap<>();
			for (int x = 0; x < orderTable.getRowCount(); x++) {
				Row orderRow=orderTable.getRow(x);
				Table refundTable = orderRow.getColumn("refund").getTableColumn();
				if(refundTable != null && refundTable.getRowCount() > 0){
					for (int y = 0; y < refundTable.getRowCount(); y++) {
						Row refundRow = refundTable.getRow(y);
						String servicetype = refundRow.getColumn("servicetype").getStringColumn();
						if("FARE".equals(servicetype)){
							String refundPaxId = refundRow.getColumn("paxid").getStringColumn();
							String refundFlightId = refundRow.getColumn("flightid").getStringColumn();
							//退票类型(0:自愿,1:非自愿)   
							String refundType = refundRow.getColumn("refundType").getStringColumn();
							//1：业务审核,2:财务审核
							String refundStatus = refundRow.getColumn("refundStatus").getStringColumn();
							//【自愿情况下包含(1：业务审核,2:财务审核),非自愿情况下包含(2:财务审核)】--人的状态为14--
							if(ZERO.equals(refundType)){
								if(ONE.equals(refundStatus) || TWO.equals(refundStatus)){
									refundPaxStatusMap.put(refundFlightId+refundPaxId, "14");
								}
							}else if(ONE.equals(refundType)){
								if(TWO.equals(refundStatus)){
									refundPaxStatusMap.put(refundFlightId+refundPaxId, "14");
								}
							}
						}
					}
				}
			}
			
			Set<String> oldHelpSet = new HashSet<String>(); 
			Set<String> newHelpSet = new HashSet<String>();
			//存放要删除当前航班下关联的航班id
			Set<String> helpSet = new HashSet<String>();
			for (int j = 0; j < orderTable.getRowCount(); j++) {
				Row orderRow=orderTable.getRow(j);
				
				Table changeTable = orderRow.getColumn("change").getTableColumn();
				if(changeTable != null && changeTable.getRowCount() > 0){
					for (Row row : changeTable) {
						String oldFlightId = row.getColumn("oldFlight").getStringColumn();
						String newFlightId = row.getColumn("newFlight").getStringColumn();
						oldHelpSet.add(oldFlightId);
						newHelpSet.add(newFlightId);
					}
				}
				
				Table flights=orderRow.getColumn("flights").getTableColumn();
				for (int i = 0; i < flights.getRowCount(); i++) {
					Row flightRow = flights.getRow(i);
					Table paxsTable=flightRow.getColumn("paxs").getTableColumn();
					String flightId = "";
					boolean flag = false;
					for (int x = 0; x < paxsTable.getRowCount(); x++) {
						Row paxRow = paxsTable.getRow(x);
						flightId = paxRow.getColumn("fltid").getStringColumn();
						Table offerTable=paxRow.getColumn("offers").getTableColumn();
						String salestatus = "";
						if(offerTable != null && offerTable.getRowCount() > 0){
							for (int k = 0; k < offerTable.getRowCount(); k++) {
								Row offerRow = offerTable.getRow(k);
								if("FARE".equals(offerRow.getColumn("servicetype").getStringColumn())){
									salestatus=offerRow.getColumn("salestatus").getStringColumn();
									break;
								}
							}
						}
						if(oldHelpSet.contains(flightId)){
							if(StringUtils.hasLength(salestatus) && !"EXCHANGED".equals(salestatus)){
								flag = true;
								break;
							}
							if(x+1 == paxsTable.getRowCount() && !flag){
								helpSet.add(flightId);
							}
						}else if(newHelpSet.contains(flightId)){
							if(StringUtils.hasLength(salestatus) && !"CANCELLED".equals(salestatus)){
								flag = true;
								break;
							}
							if(!flag && x+1 == paxsTable.getRowCount()){
								helpSet.add(flightId);
							}
						}
						
					}
				}
				
				for (int i = 0; i < flights.getRowCount(); i++) {
					Row flightRow = flights.getRow(i);
					Table paxsTable=flightRow.getColumn("paxs").getTableColumn();
					Row paxRow = paxsTable.getRow(0);
					if(helpSet.contains(paxRow.getColumn("fltid").getStringColumn())){
						flights.delRow(i);
						i--;
					}
				}
				orderRow.addColumn("flights", flights);
			}
			for (int j = 0; j < orderTable.getRowCount(); j++) {
				Row orderRow=orderTable.getRow(j);
				Order order = orders.addNewOrder();
				order.setCreationDate(orderRow.getColumn("createtime").getStringColumn());
				Table flights=orderRow.getColumn("flights").getTableColumn();
				int m = flights.getRowCount();
				Table pays=orderRow.getColumn("pay").getTableColumn();
				String validTime = "0";
				for (int p = 0; p < pays.getRowCount(); p++) {
					Row payrow = pays.getRow(p);
					if(!"".equals(payrow.getColumn("maxPaySecond").getStringColumn())){
						validTime = payrow.getColumn("maxPaySecond").getStringColumn();
						break;
					}
				}
				order.setValidTime(validTime);
				XmlAnyTypeImpl orderID4 = (XmlAnyTypeImpl) order.addNewOrderID();
				orderID4.setStringValue(orderRow.getColumn("orderno").getStringColumn());
				String currencyCode = "";
				Table offerTable = flights.getRow(0).getColumn("paxs").getTableColumn().getRow(0).getColumn("offers").getTableColumn();
				if(offerTable != null && offerTable.getRowCount() > 0){
					for (int i = 0; i < offerTable.getRowCount(); i++) {
						Row offerRow = offerTable.getRow(i);
						if("FARE".equals(offerRow.getColumn("servicetype").getStringColumn())){
							currencyCode = offerRow.getColumn("currencyCode").getStringColumn();
							break;
						}
					}
				}
				order.addNewCurrCode().setStringValue(currencyCode);
				FlightSegmentList segmentList = order.addNewFlightSegmentList();
				
				Passengers passengers=order.addNewPassengers();
				boolean RorG = true;
				for (int k = 0; k < m; k++) {
					List<String> statusList = new ArrayList<>();
					Row fltrow = flights.getRow(k);
					Table paxsTable=fltrow.getColumn("paxs").getTableColumn();
					List<String> status=new ArrayList<String>();
					BigDecimal allprice=new BigDecimal(0);
					boolean flagUser = false;
					boolean endStatus = false;
					int n = paxsTable.getRowCount();
					for (int x = 0; x < n; x++) {
						String fareStatus = "";
						Row paxRow = paxsTable.getRow(x);
						Table offerTab=paxRow.getColumn("offers").getTableColumn();
						String invstatus = "";
						String salestatus = "";
						String deliverstatus = "";
						int i =0;
						for (int a = 0; a < offerTab.getRowCount(); a++) {
							Row offer = offerTab.getRow(a);
							if("FARE".equals(offer.getColumn("servicetype").getStringColumn())){
								i++;
							}
						}
						if(i==1){
							for (int a = 0; a < offerTab.getRowCount(); a++) {
								Row offer = offerTab.getRow(a);
								if("FARE".equals(offer.getColumn("servicetype").getStringColumn())){
									invstatus=offer.getColumn("invstatus").getStringColumn();
									salestatus=offer.getColumn("salestatus").getStringColumn();
									deliverstatus=offer.getColumn("deliverstatus").getStringColumn();
									break;
								}
							}
							fareStatus = StatusUtil.getStatus(invstatus, salestatus, deliverstatus);
						}else if(i >= 2){
							for (int a = 0; a < offerTab.getRowCount(); a++) {
								Row offer = offerTab.getRow(a);
								if("FARE".equals(offer.getColumn("servicetype").getStringColumn())){
									salestatus=offer.getColumn("salestatus").getStringColumn();
									if(!"NN".equals(salestatus)){
										invstatus=offer.getColumn("invstatus").getStringColumn();
										deliverstatus=offer.getColumn("deliverstatus").getStringColumn();
									}
								}
							}
							fareStatus = StatusUtil.getStatus(invstatus, salestatus, deliverstatus);
						}
						statusList.add(fareStatus);
					}
					endStatus = getstatusByStatusList(statusList);
					String flightId = "";
					for (int i = 0; i < n; i++) {
						passengers.addNewFullName().setStringValue(paxsTable.getRow(i).getColumn("paxname").getStringColumn());
						Row paxRow = paxsTable.getRow(i);
						flightId = paxRow.getColumn("fltid").getStringColumn();
						String paxId = paxRow.getColumn("paxid").getStringColumn();
						Table  offerTab=paxRow.getColumn("offers").getTableColumn();
						String invstatus = "";
						String salestatus = "";
						String deliverstatus = "";
						int c =0;
						for (int a = 0; a < offerTab.getRowCount(); a++) {
							Row offer = offerTab.getRow(a);
							if("FARE".equals(offer.getColumn("servicetype").getStringColumn())){
								c++;
							}
						}
						if(c==1){
							for (int a = 0; a < offerTab.getRowCount(); a++) {
								Row offer = offerTab.getRow(a);
								if("FARE".equals(offer.getColumn("servicetype").getStringColumn())){
									invstatus=offer.getColumn("invstatus").getStringColumn();
									salestatus=offer.getColumn("salestatus").getStringColumn();
									deliverstatus=offer.getColumn("deliverstatus").getStringColumn();
									break;
								}
							}
						}else if(c >= 2){
							for (int a = 0; a < offerTab.getRowCount(); a++) {
								Row offer = offerTab.getRow(a);
								if("FARE".equals(offer.getColumn("servicetype").getStringColumn())){
									salestatus=offer.getColumn("salestatus").getStringColumn();
									if(!"NN".equals(salestatus)){
										invstatus=offer.getColumn("invstatus").getStringColumn();
										deliverstatus=offer.getColumn("deliverstatus").getStringColumn();
										break;
									}
								}
							}
						}
						//这里先算出当前旅客下的航班产品状态，根据产品状态，计算出allprice
						Table offersTable=paxRow.getColumn("offers").getTableColumn();
						String statusForAllPrice = StatusUtil.getStatus(invstatus, salestatus, deliverstatus);
						if("1".equals(statusForAllPrice)){
							for (int w = 0; w < offersTable.getRowCount(); w++) {
								Row offerRow=offersTable.getRow(w);
								String invstatus1 = offerRow.getColumn("invstatus").getStringColumn();
								String salestatus1 = offerRow.getColumn("salestatus").getStringColumn();
								String deliverstatus1 = offerRow.getColumn("deliverstatus").getStringColumn();
								if(!"4".equals(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus1))){
									BigDecimal price=offerRow.getColumn("price").getBigDecimalColumn();
									allprice=allprice.add(price);
								}
							}
						}else if("3".equals(statusForAllPrice)){
							for (int w = 0; w < offersTable.getRowCount(); w++) {
								Row offerRow=offersTable.getRow(w);
								String invstatus1 = offerRow.getColumn("invstatus").getStringColumn();
								String salestatus1 = offerRow.getColumn("salestatus").getStringColumn();
								String deliverstatus1 = offerRow.getColumn("deliverstatus").getStringColumn();
								if(!"4".equals(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus1))){
									BigDecimal price=offerRow.getColumn("price").getBigDecimalColumn();
									allprice=allprice.add(price);
								}
							}
						}else if("4".equals(statusForAllPrice)){
							if(endStatus){
								for (int w = 0; w < offersTable.getRowCount(); w++) {
									Row offerRow=offersTable.getRow(w);
									BigDecimal price=offerRow.getColumn("price").getBigDecimalColumn();
									allprice=allprice.add(price);
								}
							}else{
								allprice=allprice.add(new BigDecimal(0));
							}
						}else if("7".equals(statusForAllPrice)){
							if(endStatus){
								for (int w = 0; w < offersTable.getRowCount(); w++) {
									Row offerRow=offersTable.getRow(w);
									BigDecimal price=offerRow.getColumn("price").getBigDecimalColumn();
									String invstatus1 = offerRow.getColumn("invstatus").getStringColumn();
									String salestatus1 = offerRow.getColumn("salestatus").getStringColumn();
									String deliverstatus1 = offerRow.getColumn("deliverstatus").getStringColumn();
									if(!"4".equals(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus1))){
										allprice=allprice.add(price);
									}
								}
							}else{
								allprice=allprice.add(new BigDecimal(0));
							}
						}else if("13".equals(statusForAllPrice)){
							allprice=allprice.add(new BigDecimal(0));
						}else{
							for (int w = 0; w < offersTable.getRowCount(); w++) {
								Row offerRow=offersTable.getRow(w);
								String invstatus1 = offerRow.getColumn("invstatus").getStringColumn();
								String salestatus1 = offerRow.getColumn("salestatus").getStringColumn();
								String deliverstatus1 = offerRow.getColumn("deliverstatus").getStringColumn();
								if(!"1".equals(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus1)) 
										&& !"3".equals(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus1)) 
										&& !"4".equals(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus1))
										&& !"7".equals(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus1))){
									BigDecimal price=offerRow.getColumn("price").getBigDecimalColumn();
									allprice=allprice.add(price);
								}
							}
						}
						String status1 = "";
						if(refundPaxStatusMap != null && refundPaxStatusMap.size() > 0){
							if(StringUtils.hasLength(refundPaxStatusMap.get(flightId+paxId))){
								status1 = "14";
							}
						}
						if(!StringUtils.hasLength(status1)){
							if("USED".equals(deliverstatus) && paxsTable.getRowCount() == 1){
								status1 = "10";
							}else if("USED".equals(deliverstatus)){
								status1 = "10";
							}else if("DS".equals(deliverstatus) && paxsTable.getRowCount() == 1){
								status1 = "15";
								flagUser = true;
							}else if("DS".equals(deliverstatus)){
								status1 = "16";
							}else{
								status1 = StatusUtil.getStatus(invstatus, salestatus, deliverstatus);
							}
						}
						if(!"15".equals(status1) && !"16".equals(status1)){
							status.add(status1);
						}
					}
					if(!flagUser || status.size() != 0){
						FlightSegment  segment = segmentList.addNewFlightSegment();
						String flightStatus = getFlightStatus(status);
						if(StringUtils.hasLength(flightStatus) && "3".equals(flightStatus) && newHelpSet.contains(flightId)){
							flightStatus = "20";
						}else if(StringUtils.hasLength(flightStatus) && "6".equals(flightStatus) && oldHelpSet.contains(flightId)){
							flightStatus = "21";
						}
						segment.setStatus(flightStatus);
						LCCFlightAirportType departure =segment.addNewDeparture() ;
						departure.addNewAirportCode().setStringValue(fltrow.getColumn("oricode").getStringColumn());
						Calendar ori=Calendar.getInstance();
						ori.setTime(fltrow.getColumn("oridate").getDateColumn());
						departure.setDate(ori);
						departure.setTime(fltrow.getColumn("oritime").getStringColumn());
						departure.setAirportName(fltrow.getColumn("oriname").getStringColumn());
						String language = ApiServletHolder.getApiContext().getLanguage();
						//出发地城市三字码	
						departure.addNewCityCode().setStringValue(fltrow.getColumn("oricitycode").getStringColumn());
						//<!--出发地城市名称-->
						//departure.setCityName(fltrow.getColumn("oricityname").getStringColumn());
						departure.setCityName((StatusUtil.getLanguageName(fltrow.getColumn("oricityname").getObjectColumn(), language)));
						//出发地所在国家二字码
						departure.addNewCountryCode().setStringValue(fltrow.getColumn("oricountry").getStringColumn());
						
						LCCFlightAirportType arrival =segment.addNewArrival() ;
						arrival.addNewAirportCode().setStringValue(fltrow.getColumn("destcode").getStringColumn());
						Calendar dest=Calendar.getInstance();
						dest.setTime(fltrow.getColumn("destdate").getDateColumn());
						arrival.setDate(dest);
						arrival.setTime(fltrow.getColumn("desttime").getStringColumn());
						//目的地机场名称
//					arrival.setAirportName(fltrow.getColumn("destname").getStringColumn());
						arrival.setAirportName(fltrow.getColumn("destname").getStringColumn());
						
						//目的地城市三字码	
						arrival.addNewCityCode().setStringValue(fltrow.getColumn("destcitycode").getStringColumn());
						//<!--目的地城市中文名称-->
						arrival.setCityName(StatusUtil.getLanguageName(fltrow.getColumn("destcityname").getObjectColumn(),language));
						//目的地所在国家二字码
						arrival.addNewCountryCode().setStringValue(fltrow.getColumn("destcountry").getStringColumn());
						
						MarketingCarrierType  marketingCarrier =segment.addNewMarketingCarrierAirline() ;
						String airlineid=fltrow.getColumn("airline").getStringColumn();
						String  flightinfo=fltrow.getColumn("flightno").getStringColumn();
						marketingCarrier.addNewAirlineID().setStringValue(airlineid);
						FlightNumber fltno = marketingCarrier.addNewFlightNumber();
						fltno.setOperationalSuffix(fltrow.getColumn("suffix").getStringColumn());
						fltno.setStringValue(flightinfo);
						segment.addNewTotal().addNewAmount().setStringValue(allprice.toString());
						
						FlightDetailType flightDetail=segment.addNewFlightDetail();
						CodesetType codesetType = flightDetail.addNewFlightSegmentType();
						codesetType.setCode(fltrow.getColumn("routtype").getStringColumn());
						if (fltrow.getColumn("routtype").getStringColumn().equals("R")) {
							RorG = false;
						}
					}
				}
				segmentList.setTripType(RorG?"S":"R");
			}
		}
		return doc;
	}
	
	/**
	 * 判断当前航线下的所有人的状态是否为全部已取消或者是全部为已退款
	 * @param statusList
	 * @return
	 */
	private boolean getstatusByStatusList(List<String> statusList) {
		int mm=0;
		int nn=0;
		for (int i = 0; i < statusList.size(); i++) {
			//已取消
			if("4".equals(statusList.get(i))){
				mm = mm + 1;
			}
			//已退款
			if("7".equals(statusList.get(i))){
				nn = nn + 1;
			}
		}
		if(mm == statusList.size()){
			return true;
		}else if(nn == statusList.size()){
			return true;
		}
		return false;
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
		OrderListRQDocument rootDoc = null;
		rootDoc = OrderListRQDocument.Factory.parse(xmlInput);
		OrderListRQDocument.OrderListRQ reqDoc = rootDoc.getOrderListRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid",deptno);
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		input.addParm("language", language);
		input.addParm("memberid", context.getContext().getUserID());
		// Query
		Query query = reqDoc.getQuery();
		// Query->Filters
		Filters filters = query == null ? null : query.getFilters();
		if (filters == null) {
			return;
		}

		BookingReferences bookingRefs = filters.getBookingReferences();
		if (bookingRefs != null) {
			BookingReferenceType bookingReference = bookingRefs
					.getBookingReferenceArray(0);
			String otherID = bookingReference.getOtherID().getStringValue();
//			// pagenum
			if (StringUtils.hasLength(otherID)) {
				input.addParm("pagenum", otherID);
			}
			
			// pagesize
			String id = bookingReference.getID();
			if (StringUtils.hasLength(id)) {
				input.addParm("pagesize", id);
			}
		}
		// Query->Filters->CreateDateRange
		 CreateDateRange  createDateRange= filters.getCreateDateRange();
		if(null !=createDateRange){
			if (null!=createDateRange.getEffective()&&null!=createDateRange.getExpiration()) {
				Calendar calendar=createDateRange.getEffective();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				String createstart = sdf.format(calendar.getTime());
				calendar=createDateRange.getExpiration();
				String createend = sdf.format(calendar.getTime());
				input.addParm("createstart", createstart);
				input.addParm("createend", createend);
			}
		}
	}
	/**
	 * 当一个航段里有多人的时候，如果旅客不同的状态，则规则为：
	 * 一个航段中，只要有旅客是已支付的状态，则显示为已支付状态，
	 * 只有全部为已退款、待退款、已值机、已登机，航段状态才为已退款、待退款、已值机、已登机。
	 * 一个航班下，多名旅客状态对比.
	 * 
	 *1待确认    2已拒绝     3待支付    4已取消    5已支付   6待退款    
	 *7已退款   8已值机   9已登机   10已使用   11未使用   12异常
	 */
	public String getFlightStatus(List<String> status){
		if (status.size()==1) {
			return status.get(0);
		}
		//只有全部为已退款、待退款、已值机、已登机，航段状态才为已退款、待退款、已值机、已登机
		int aa=0;
		int bb=0;
		int cc=0;
		int dd=0;
		for (int i = 0; i < status.size(); i++) {
			if (bb==status.size()) {
				return "6";
			}
			//已退款
			if ("7".equals(status.get(i))) {
				aa=aa+1;
			}
			//待退款
			if ("6".equals(status.get(i))) {
				bb=bb+1;
			}
			//已值机
			if ("8".equals(status.get(i))) {
				cc=cc+1;
			}
			//已登机
			if ("9".equals(status.get(i))) {
				dd=dd+1;
			}
		}
		if (aa==status.size()) {
			return "7";
		} else if (bb==status.size()) {
			return "6";
		}else if (cc==status.size()) {
			return "8";
		}else if (dd==status.size()) {
			return "9";
		}

		//一个航班下，多名旅客状态对比
		String status1 = compareStatus(status);
		if(StringUtils.hasLength(status1)){
			return status1;
		}
		return status.get(0);
	}

	private String compareStatus(List<String> list) {
		String status1 ="";
		for (int i = 0; i < list.size(); i++) {
			if(i == 0){
				status1 = list.get(i);
			}else{
				status1 = comState(status1, list.get(i));
			}
		}
		return status1;
	}
	
	private String comState(String status1, String status2) {
		String p = "";
		switch (status1) {
		case "1": //待确认
			switch (status2) {
			case "1":p = "1";break;
			case "2":p = "1";break;
			case "3":p = "3";break;
			case "4":p = "1";break;
			case "5":p = "1";break;
			case "6":p = "6";break;
			case "7":p = "1";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "1";break;
			case "13":p = "1";break;
			case "14":p = "14";break;
			default:p = status2;break;
			}
			break;
		case "2"://已拒绝
			switch (status2) {
			case "1":p = "1";break;
			case "2":p = "2";break;
			case "3":p = "3";break;
			case "4":p = "2";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "7";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "10";break;
			case "13":p = "13";break;
			case "14":p = "14";break;
			default:p = status2;break;
			}
			break;	
		case "3": //待支付
			switch (status2) {
			case "1":p = "3";break;
			case "2":p = "3";break;
			case "3":p = "3";break;
			case "4":p = "3";break;
			case "5":p = "3";break;
			case "6":p = "6";break;
			case "7":p = "3";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "3";break;
			case "13":p = "3";break;
			case "14":p = "14";break;
			default:p = status2;break;
			}
			break;
		case "4": //已取消
			switch (status2) {
			case "1":p = "1	";break;
			case "2":p = "13";break;
			case "3":p = "3";break;
			case "4":p = "4";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "7";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "10";break;
			case "13":p = "4";break;
			case "14":p = "14";break;
			default:p = status2;break;
			}
			break;
		case "5": //已支付
			switch (status2) {
			case "1":p = "5";break;
			case "2":p = "5";break;
			case "3":p = "5";break;
			case "4":p = "5";break;
			case "5":p = "5";break;
			case "6":p = "5";break;
			case "7":p = "5";break;
			case "8":p = "5";break;
			case "9":p = "5";break;
			case "10":p = "5";break;
			case "13":p = "5";break;
			case "14":p = "5";break;
			default:p = "5";break;
			}
			break;
		case "6"://待退款
			switch (status2) {
			case "1":p = "6";break;
			case "2":p = "6";break;
			case "3":p = "6";break;
			case "4":p = "6";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "6";break;
			case "8":p = "6";break;
			case "9":p = "6";break;
			case "10":p = "6";break;
			case "13":p = "6";break;
			case "14":p = "6";break;
			default:p = status2;break;
			}	
			break;
		case "7"://已退款
			switch (status2) {
			case "1":p = "1";break;
			case "2":p = "7";break;
			case "3":p = "3";break;
			case "4":p = "7";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "7";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "10";break;
			case "13":p = "7";break;
			default:p = status2;break;
			}	
			break;
		case "8"://已值机
			switch (status2) {
			case "1":p = "1";break;
			case "2":p = "8";break;
			case "3":p = "8";break;
			case "4":p = "8";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "8";break;
			case "8":p = "8";break;
			case "9":p = "8";break;
			case "10":p = "8";break;
			case "13":p = "8";break;
			default:p = status2;break;
			}
			break;
		case "9"://已登机
			switch (status2) {
			case "1":p = "9";break;
			case "2":p = "9";break;
			case "3":p = "9";break;
			case "4":p = "9";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "9";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "9";break;
			case "13":p = "9";break;
			default:p = status2;break;
			}
			break;
		case "10"://已使用
			switch (status2) {
			case "1":p = "1";break;
			case "2":p = "10";break;
			case "3":p = "3";break;
			case "4":p = "10";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "10";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "10";break;
			case "13":p = "10";break;
			default:p = status2;break;
			}
			break;
		case "13"://已变更
			switch (status2) {
			case "1":p = "1";break;
			case "2":p = "13";break;
			case "3":p = "3";break;
			case "4":p = "4";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "7";break;
			case "8":p = "8";break;
			case "9":p = "9";break;
			case "10":p = "10";break;
			case "13":p = "13";break;
			case "14":p = "14";break;
			default:p = status2;break;
			}
			break;	
		case "14"://
			switch (status2) {
			case "1":p = "14";break;
			case "2":p = "14";break;
			case "3":p = "14";break;
			case "4":p = "14";break;
			case "5":p = "5";break;
			case "6":p = "6";break;
			case "7":p = "14";break;
			case "8":p = "14";break;
			case "9":p = "14";break;
			case "10":p = "14";break;
			case "13":p = "14";break;
			default:p = status2;break;
			}
			break;	
		default:
			p = status1;
			break;
		}
		return p;
	}
}
