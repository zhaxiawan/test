package com.travelsky.quick.business;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.BagAllowanceWeightType.MaximumWeight;
import org.iata.iata.edist.BaggageItemType;
import org.iata.iata.edist.BaggageItemType.Services;
import org.iata.iata.edist.BookingReferenceType;
import org.iata.iata.edist.ContactsDocument.Contacts;
import org.iata.iata.edist.ContactsDocument.Contacts.Contact;
import org.iata.iata.edist.CouponInfoType;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.DataListType;
import org.iata.iata.edist.DataListType.FlightList;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DataListType.OriginDestinationList;
import org.iata.iata.edist.DataListType.SeatList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.DetailCurrencyPriceType.Details.Detail;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightCOSCoreType;
import org.iata.iata.edist.FlightCOSCoreType.MarketingName;
import org.iata.iata.edist.FlightDetailType;
import org.iata.iata.edist.FlightItemType;
import org.iata.iata.edist.FlightItemType.Price;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.FlightType.Flight;
import org.iata.iata.edist.ItemIDType;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfOfferPenaltyType;
import org.iata.iata.edist.ListOfOfferPenaltyType.Penalty;
import org.iata.iata.edist.ListOfPriceClassType;
import org.iata.iata.edist.ListOfSeatType;
import org.iata.iata.edist.ListOfServiceBundleType;
import org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle;
import org.iata.iata.edist.MarketingCarrierFlightType;
//import org.iata.iata.edist.OrderCoreType.Payments;
import org.iata.iata.edist.OrderIDType;
import org.iata.iata.edist.OrderItemCoreType.OrderItem;
import org.iata.iata.edist.OrderRetrieveRQDocument;
import org.iata.iata.edist.OrderViewRSDocument;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.LCCRefundList;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.LCCRefundList.LCCRefund;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.LCCRefundList.LCCRefund.LCCRefundDetailList;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.LCCRefundList.LCCRefund.LCCRefundDetailList.LCCRefundDetail;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Order;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Order.OrderItems;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Passengers;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.TicketDocInfos;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.TicketDocInfos.TicketDocInfo;
import org.iata.iata.edist.OriginDestinationDocument.OriginDestination;
import org.iata.iata.edist.OtherContactMethodType;
import org.iata.iata.edist.PassengerDocument.Passenger;
import org.iata.iata.edist.PassengerSummaryType.PassengerIDInfo.FOID;
import org.iata.iata.edist.PriceClassAssocType.Association;
import org.iata.iata.edist.PriceClassType;
import org.iata.iata.edist.SeatItemDocument.SeatItem;
import org.iata.iata.edist.SeatLocationType;
import org.iata.iata.edist.ServiceCoreType.Associations;
import org.iata.iata.edist.ServiceCoreType.BookingInstructions;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceEncodingType;
import org.iata.iata.edist.ServiceInfoAssocType;
import org.iata.iata.edist.ServiceListDocument.ServiceList;
import org.iata.iata.edist.ServicePriceType;
import org.iata.iata.edist.TaxDetailType.Breakdown;
import org.iata.iata.edist.TaxDetailType.Breakdown.Tax;
import org.iata.iata.edist.TicketDocumentDocument1.TicketDocument;
import org.iata.iata.edist.TravelerCoreType.PTC;
import org.iata.iata.edist.TravelerSummaryType.Name;
import org.iata.iata.edist.WeightUnitSimpleType.Enum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.StatusUtil;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 订单详情信息接口
 * 
 * @author ZHANGWENLONG 2017年7月31日
 * @version 0.1
 */
@Service("LCC_ORDERDETAIL_SERVICE")
public class APIOrderDetailNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 订单详情
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIOrderDetailNDCBusiness.class);
	private static final String SEG ="SEG";
	private static final String FLT ="FLT";
	private String  PNR ="";
	private String airlinecd = "";
	private String currencyCode = "";
	private static final String  PAXID = "paxid";
	private static final String  FLIGHTID = "flightid";
	private static final String  ID = "id";
//	private static final String  PAYS = "pays";
	private static final String  _FOR_SALE = "_FOR_SALE";
	private static final String  _FOR_FREE = "_FOR_FREE";
	private static final String  FOR_FREE = "FOR_FREE";
	private static final String  ONE = "1";
	private static final String  VAT_PRODUCT = "VAT_PRODUCT";
	
	
	
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
					ErrCodeConstants.API_UNKNOW_ORDERDETAILS,
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
		return orderOpManager.detail(input, context);
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
		OrderRetrieveRQDocument rootDoc = OrderRetrieveRQDocument.Factory
				.parse(xmlInput);

		OrderRetrieveRQDocument.OrderRetrieveRQ reqDoc = rootDoc
				.getOrderRetrieveRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid",deptno);
		// 获取语言 
		String language = ApiServletHolder.getApiContext().getLanguage();
		// 订单号 Query>-->Filters >-->OrderID
		String owner = reqDoc.getQuery().getFilters().getOrderID()
				.getOwner();
		String orderID = reqDoc.getQuery().getFilters().getOrderID()
				.getStringValue();
		if (!StringUtils.hasLength(orderID)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_ID, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_ID);
		}
		input.addParm("owner", owner);
		// 订单号
		input.addParm("orderno", orderID);
		// 暂不填写 会员账号
		input.addParm("memberid", context.getContext().getUserID());
		input.addParm("language", language);
	}
	
	/**
	 * 转换 xml-->Reqbean
	 * 
	 * @return XmlObject
	 */
	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		OrderViewRSDocument doc = OrderViewRSDocument.Factory.newInstance();
		OrderViewRSDocument.OrderViewRS root = doc.addNewOrderViewRS();
		// 获取语言 
		String language = ApiServletHolder.getApiContext().getLanguage();
		Mapping mapping = new Mapping();
		try {
			if (processError(commandRet, root)) {
				return doc;
			}
			root.addNewSuccess();
			Response response = root.addNewResponse();
			/*旅客信息*****************************************/
			Passengers  passengersArry= response.addNewPassengers();
			addPaxsXML(commandRet,passengersArry,mapping);
			
			//货币三字码
			currencyCode = commandRet.getParm("currencyCode").getStringColumn();
			// 订单号
			String orderNo = commandRet.getParm("orderno").getStringColumn();
			
			//Response->Order
			Order order =  response.addNewOrder();
			//Order --> OrderID
			OrderIDType orderID = order.addNewOrderID();
			orderID.setStringValue(orderNo);
			// 订单总价totalprice
			String totalprice = commandRet.getParm("totalprice").getStringColumn();
			// 支付总金额
			CurrencyAmountOptType totalType = order.addNewTotalOrderPrice().addNewDetailCurrencyPrice().addNewTotal();
			totalType.setStringValue(totalprice);
			totalType.setCode(commandRet.getParm("currencyCode").getStringColumn());
			//订单状态
//			order.addNewStatus().addNewStatusCode().setCode(commandRet.getParm("orderstatus").getStringColumn());
			
			//Order --> BookingReferences
			BookingReferenceType  BookingReference = order.addNewBookingReferences().addNewBookingReference();
			
			//Order --> Payments
//			orderInfos(order, commandRet, input);
			
			//Order --> TimeLimits
			Table paysTabe = commandRet.getParm("pays").getTableColumn();
			String maxpay = paysTabe.getRow(0).getColumn("maxpaysecond").getStringColumn();
			int hour=(Integer.valueOf(maxpay)/(60*60));
			int min=((Integer.valueOf(maxpay)/(60))-hour*60);
			int s=(Integer.valueOf(maxpay)-hour*60*60-min*60);
			maxpay=hour+"H"+min+"M"+s+"S";
			order.addNewTimeLimits().addNewPaymentTimeLimit().setRefs(getList(maxpay));
			
			//-----------------------------------------------------------------------------------
			Map<String, String> fltRefundMap = new HashMap<String, String>();
			Map<String, String> serviceRefundMap = new HashMap<String, String>();
			Map<String, String> seatRefundMap = new HashMap<String, String>();
			Table refundTab=commandRet.getParm("refunds").getTableColumn();
			Table refundDetailTab=commandRet.getParm("refunddetails").getTableColumn();
			if (refundTab != null && refundTab.getRowCount()>0) {
				for (int i = 0; i < refundTab.getRowCount(); i++) {
					Row refundRow=refundTab.getRow(i);
					if(refundDetailTab != null && refundDetailTab.getRowCount() > 0){
						for (int j = 0; j < refundDetailTab.getRowCount(); j++) {
							Row refundDetailRow=refundDetailTab.getRow(j);
							String refundid=refundDetailRow.getColumn("refundid").getStringColumn();
							if (refundid.equals(refundRow.getColumn("id").getStringColumn())) {
								String paxid=refundDetailRow.getColumn("paxid").getStringColumn();
								String flightid=refundDetailRow.getColumn("flightid").getStringColumn();
								String detailid=refundDetailRow.getColumn("detailid").getStringColumn();
								String servicetype = refundDetailRow.getColumn("servicetype").getStringColumn();
								String chargetype = refundDetailRow.getColumn("chargetype").getStringColumn();
								refundDetailRow.getColumn("detailtype").getStringColumn();
								if("FARE".equals(servicetype) && "Fare".equals(chargetype)){//机票/
									fltRefundMap.put(paxid+flightid, refundDetailRow.getColumn("refundfee").getStringColumn());
								} else if("FARE".equals(servicetype) && ("Fee".equals(chargetype) || "Tax".equals(chargetype))){//税费/
									
								}else if("SEAT".equals(servicetype)) {
									seatRefundMap.put(paxid+flightid, refundDetailRow.getColumn("refundfee").getStringColumn());
								}else {
									serviceRefundMap.put(paxid+flightid+detailid, refundDetailRow.getColumn("refundfee").getStringColumn());
								}
							}
						}
					}
				}
			}
			
			//Response --> DataLists
			DataListType  dataLists = response.addNewDataLists();
			//DataLists --> FlightSegmentList
			FlightSegmentList flightArry = dataLists.addNewFlightSegmentList();
			ListOfOfferPenaltyType penaltyList = dataLists.addNewPenaltyList();
			Table seatsTable=commandRet.getParm("seats").getTableColumn();
			Table paxsTable=commandRet.getParm("paxs").getTableColumn();
			Table flightsTable = commandRet.getParm("flights").getTableColumn();
			Table paxflightsTable = commandRet.getParm("paxflights").getTableColumn();
			Table submarketsTable = commandRet.getParm("submarkets").getTableColumn();
			Table costsTable = commandRet.getParm("costs").getTableColumn();
			//DataLists --> PriceClassList
			ListOfPriceClassType priceClassList = dataLists.addNewPriceClassList();
			addFlightList(mapping,flightArry,commandRet,priceClassList);
			//DataLists --> PenaltyList
			setPenaltyList(penaltyList,paxflightsTable,flightsTable,seatsTable,submarketsTable,paxsTable,costsTable,mapping);
			
			
			//DataLists --> ServiceList
			ServiceList serviceListA= dataLists.addNewServiceList();
			Map<String,List<Row>> submarketMap = new HashMap<String,List<Row>>();
			addServiceList(commandRet,serviceListA,submarketMap,mapping);
			
			//航班信息
			addSeats(commandRet,mapping);
			
			//Order --> OrderItems
			OrderItems OrderItem = order.addNewOrderItems();
			//辅助map
			Map<String, Row> helpMap = new TreeMap<String, Row>();
			addPaxflight(commandRet,OrderItem,mapping,submarketMap,seatRefundMap,language,helpMap);
			
			//Response --> TicketDocInfos
			TicketDocInfos ticketDocInfos = response.addNewTicketDocInfos();
			// 客票信息
			Table paxFlightstab = commandRet.getParm("paxflights").getTableColumn();
			//非必须有 待定 
			ticketDocInfos(paxFlightstab, ticketDocInfos,mapping);
			
			//DataLists --> FlightList
			FlightList  flightList = dataLists.addNewFlightList();
			//DataLists --> OriginDestinationList
			OriginDestinationList originArry = dataLists.addNewOriginDestinationList();
			addOriginArryArry(commandRet,flightList,originArry,mapping);
			
			//DataLists --> ServiceBundleList
			ListOfServiceBundleType listOfServiceBundleArry = dataLists.addNewServiceBundleList();
			addServiceBundleListArry(mapping,listOfServiceBundleArry);
			
			BookingReference.setID(PNR);
			BookingReference.addNewAirlineID().setStringValue(airlinecd);
			
			//DataLists --> SeatList
			SeatList seatList = dataLists.addNewSeatList();
			addSeatListArry(commandRet,seatList, mapping,helpMap);
			
			//Response --> LCCRefundList
			refundInfo(commandRet,response,mapping);
		}
		catch (Exception e) {
			LOGGER.error(ErrCodeConstants.API_NULL_OFFERS, e);
			doc = OrderViewRSDocument.Factory.newInstance();
			root = doc.addNewOrderViewRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM,
					TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
							ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}
		return doc;
	}
	
	/**
	 * seatList
	 * @param commandRet
	 * @param seatList
	 * @param mapping
	 */
	private void addSeatListArry(CommandRet commandRet,SeatList seatList, Mapping mapping,Map<String, Row> helpMap) {
		Table deliverSeattab=commandRet.getParm("deliverSeat").getTableColumn();
		if(helpMap != null && helpMap.size() > 0){
			for (Entry<String, Row> paxFlightid : helpMap.entrySet()) {
				Row paxflight = paxFlightid.getValue();
				//航班id
				String flightid = paxflight.getColumn("flightid").getStringColumn();
				//人员id
				String paxid = paxflight.getColumn("paxid").getStringColumn();
				//人航段id
				String id = paxflight.getColumn("id").getStringColumn();
				// OrderItem  --> SeatItem
				Row seatRow = mapping.getSeatsMap().get(flightid+paxid);
				if(null!=seatRow){
					String seatno  = seatRow.getColumn("seatno").getStringColumn();
					String seatpaxid = seatRow.getColumn(PAXID).getStringColumn();
					String seatflightid = seatRow.getColumn(FLIGHTID).getStringColumn();
					if(!StringUtils.hasLength(seatno)){
						seatno = seatRow.getColumn("seatname").getStringColumn();
					}
					ListOfSeatType newSeat = seatList.addNewSeats();
					newSeat.setListKey(id+seatno+_FOR_SALE);
					//SeatItem -->Location
					SeatLocationType locArry =newSeat.addNewLocation();
					String	column =seatno.substring(seatno.length()-1,seatno.length());
					String	number =seatno.substring(0,seatno.length()-1);
					//Location -->Column
					locArry.setColumn(column);
					//Location -->Row -->Number
					org.iata.iata.edist.SeatLocationType.Row seatrow = locArry.addNewRow();
					seatrow.addNewNumber().setStringValue(number);
					//座位状态
					String invstatus1=seatRow.getColumn("invstatus").getStringColumn();
					String salestatus1=seatRow.getColumn("salestatus").getStringColumn();
					String deliverstatus="";
					if(deliverSeattab != null && deliverSeattab.getRowCount() > 0){
						for (int i = 0; i < deliverSeattab.getRowCount(); i++) {
							Row deliverSeatRow=deliverSeattab.getRow(i);
							String deliverpaxid=deliverSeatRow.getColumn("paxid").getStringColumn();
							String deliverflightid=deliverSeatRow.getColumn("flightid").getStringColumn();
							if(deliverpaxid.equals(seatpaxid)
									&& deliverflightid.equals(seatflightid)){
								deliverstatus=deliverSeatRow.getColumn("status").getStringColumn();
								break;
							}
						}
					}
					// Row --> Type -->Code
					seatrow.addNewType().setCode(StatusUtil.getStatus(invstatus1, salestatus1, deliverstatus));
				}
			}
		}
	}

	/**
	 * 添加旅客信息到RS-XML
	 * @param commandRet
	 * @param passengersArry
	 * @throws APIException
	 */
	private void addPaxsXML(CommandRet commandRet,Passengers  passengersArry,Mapping mapping) throws APIException{
		//联系人
		Table contactsTable = commandRet.getParm("contacts").getTableColumn();
		//姓
		String contactname = contactsTable.getRow(0).getColumn("name").getStringColumn();
		//名
//		String contactFirstname = contactsTable.getRow(0).getColumn("firstname").getStringColumn();
		//邮箱地址
		String contactEmail = contactsTable.getRow(0).getColumn("email").getStringColumn();
		//联系电话
		String contactTelephone = contactsTable.getRow(0).getColumn("telephone").getStringColumn();
		//区号
		String contactprefix = contactsTable.getRow(0).getColumn("contactprefix").getStringColumn();
		String language = ApiServletHolder.getApiContext().getLanguage();
		Table paxsTable = commandRet.getParm("paxs").getTableColumn();
		if (paxsTable == null || paxsTable.getRowCount() < 1) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PAXS, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PAXS);
		}
		Map<String,String> paxsMap = new  HashMap<String,String>();
		int paxNum =1;
		for(Row paxs:paxsTable){
			//旅客id
			String paxid = paxs.getColumn("id").getStringColumn();
			//旅客类型
			String paxtype = paxs.getColumn("paxtype").getStringColumn();
			//旅客邮箱
			String paxEmail = paxs.getColumn("email").getStringColumn();
			//陪护人
			String guardian = paxs.getColumn("guardian").getStringColumn();
			//旅客手机号
			String paxTelephone = paxs.getColumn("telephone").getStringColumn();
			//旅客关联的国家区号
			String paxContactprefix = paxs.getColumn("contactprefix").getStringColumn();
			Passenger passengerArry = passengersArry.addNewPassenger();
			passengerArry.setObjectKey("P"+paxNum);
			mapping.getAllMap().put(paxid, "P"+paxNum);
			paxs.addColumn(paxid, "P"+paxNum);
			paxsMap.put(paxid, "P"+paxNum);
			PTC ptcArry = passengerArry.addNewPTC();
			ptcArry.setStringValue(paxtype);
//			ptcArry.setQuantity(BigInteger.valueOf(paxNum));
			String guardianId = "";
			//反向寻找婴儿的陪护人
			if(!"".equals(guardian) && null !=guardian){
				Passenger[]  passenger =passengersArry.getPassengerArray();
				for(Passenger pass:passenger){
					String guardians = pass.getObjectKey();
					guardianId = paxsMap.get(guardian);
					if(null!=guardianId&&guardianId.equals(guardians)){
						pass.setPassengerAssociation("P"+paxNum);
						break;
					}
				}
			}
			//出生日期
			Date birthDate = paxs.getColumn("birth").getDateColumn();
			passengerArry.addNewAge().addNewBirthDate().setDateValue(birthDate);
			//旅客姓名
			Name nameArry = passengerArry.addNewName();
			String lastname = paxs.getColumn("lastname").getStringColumn();
			String firstname = paxs.getColumn("firstname").getStringColumn();
			nameArry.addNewSurname().setStringValue(lastname);
			nameArry.addNewGiven().setStringValue(firstname);
			Contacts  contactsArry = passengerArry.addNewContacts();
			Contact contactArry1 = contactsArry.addNewContact();
			contactArry1.addNewContactType().setStringValue("PASSENGER");
			org.iata.iata.edist.PhoneType.Number numberArry1 = contactArry1.addNewPhoneContact().addNewNumber();
			numberArry1.setAreaCode(paxContactprefix);
			numberArry1.setStringValue(paxTelephone);
			contactArry1.addNewEmailContact().addNewAddress().setStringValue(paxEmail);
			if(paxNum == 1){
				/*联系人信息*****************************************************************/
				Contact contactArry2 = contactsArry.addNewContact();
				contactArry2.addNewContactType().setStringValue("CONTACT");
				org.iata.iata.edist.PhoneType.Number numberArry = contactArry2.addNewPhoneContact().addNewNumber();
				numberArry.setAreaCode(contactprefix);
				numberArry.setStringValue(contactTelephone);
				OtherContactMethodType ocmArry = contactArry2.addNewOtherContactMethod();
				ocmArry.setName(contactname);
				contactArry2.addNewEmailContact().addNewAddress().setStringValue(contactEmail);
			}
			/********************************************************************/
			//旅客性别 Male/Female
			String paxsex = paxs.getColumn("paxsex").getStringColumn();
			paxsex = "M".equals(paxsex)?"Male":"Female";
			passengerArry.addNewGender().setStringValue(paxsex);
			//旅客证件信息
			FOID fpidArry = passengerArry.addNewPassengerIDInfo().addNewFOID();
			String passtype = paxs.getColumn("passtype").getStringColumn();
			String passno = paxs.getColumn("passno").getStringColumn();
			fpidArry.setType(passtype);
			fpidArry.addNewID().setStringValue(passno);
			paxNum++;
		}
	}
	
	/**
	 * 附加服务
	 * @param submarketstab
	 * @param serviceList
	 * @param flightstab
	 */
	public void serviceList(Table submarketstab, ServiceList serviceList, Table flightstab,String currencyCode,Table deliverySubmarcket,Map<String, String> serviceRefundMap){
		if (submarketstab!=null && submarketstab.getRowCount()>0) {
			for (int i = 0; i < submarketstab.getRowCount(); i++) {
				ServiceDetailType service =serviceList.addNewService();
				Row submarketRow=submarketstab.getRow(i);
				service.addNewServiceID().setStringValue(submarketRow.getColumn("submarketcode").getStringColumn());
				service.addNewName().setStringValue(submarketRow.getColumn("submarketname").getStringColumn());
				service.addNewEncoding().addNewCode().setStringValue(submarketRow.getColumn("submarkettype").getStringColumn());
				service.addNewFeeMethod().setStringValue(submarketRow.getColumn("isfree").getStringColumn());
				String invstatus=submarketRow.getColumn("invstatus").getStringColumn();
				String salestatus=submarketRow.getColumn("salestatus").getStringColumn();
				String id= submarketRow.getColumn("id").getStringColumn();
				String deliverytatus="";
				if(deliverySubmarcket != null){
					for (int j = 0; j < deliverySubmarcket.getRowCount(); j++) {
						Row row=deliverySubmarcket.getRow(j);
						String productid=row.getColumn("productid").getStringColumn();
						if (productid.equals(id)) {
							deliverytatus=row.getColumn("status").getStringColumn();
							break;
						}
					}
				}
				service.getEncoding().addNewSubCode().setStringValue(StatusUtil.getStatus(invstatus, salestatus, deliverytatus));
				service.addNewDescriptions().addNewDescription().addNewText().setStringValue(submarketRow.getColumn("submarketdesc").getStringColumn());
				ServicePriceType servicePriceType= service.addNewPrice();
				CurrencyAmountOptType total = servicePriceType.addNewTotal();
				total.setCode(currencyCode);
				total.setBigDecimalValue("".equals(submarketRow.getColumn("unitprice").getStringColumn())?BigDecimal.valueOf(0):submarketRow.getColumn("unitprice").getBigDecimalColumn());
				String serviceRefundPricekey = submarketRow.getColumn("paxid").getStringColumn() + 
						submarketRow.getColumn("flightid").getStringColumn() + 
						submarketRow.getColumn("id").getStringColumn();
				servicePriceType.addNewRefundableValue().addNewAmount().setBigDecimalValue(serviceRefundMap.get(serviceRefundPricekey)==null?BigDecimal.valueOf(0):BigDecimal.valueOf(Long.valueOf(serviceRefundMap.get(serviceRefundPricekey))));
				BookingInstructions bookingInstructions=service.addNewBookingInstructions();
				bookingInstructions.addNewSSRCode().setStringValue(submarketRow.getColumn("ssrtype").getStringColumn());
				bookingInstructions.addNewText().setStringValue(submarketRow.getColumn("currencySign").getStringColumn());
				Associations associations = service.addNewAssociations();
				
				String[] paxs=submarketRow.getColumn("paxid").getStringColumn().split(",");
				List<String> paxList=new ArrayList<String>();
				paxList.add(paxs[0]);
				associations.addNewTraveler().setTravelerReferences(paxList);
				
				associations.addNewFlight().addNewSegmentReferences().setStringValue(submarketRow.getColumn("flightid").getStringColumn());
				
				String[] famlies=submarketRow.getColumn("familycode").getStringColumn().split(",");
				List<String> famliesList=new ArrayList<String>();
				famliesList.add(famlies[0]);
				associations.addNewOffer().setOfferReferences(famliesList);
				service.addNewDetail().addNewServiceItemQuantityRules().setMaximumQuantity(BigInteger.valueOf(submarketRow.getColumn("buynum").getLongColumn()));
			}
		}
	}
	
	/**
	 * 退改规则
	 * @param flightsrow 航班数据
	 * @param dataLists  节点 
	 */
	private void setPenaltyList(ListOfOfferPenaltyType penaltyList,Table paxflightstab,Table flightstab,Table seatstab,Table surbkets,Table paxtab,Table costsTable,Mapping mapping){
		Set<String> subHelpSet = new HashSet<String>();
		Set<String> fliHelpSet = new HashSet<String>();
		for (int i = 0; i < paxflightstab.getRowCount(); i++) {
			Row paxflightRow =paxflightstab.getRow(i);
			String paxflightId = paxflightRow.getColumn("id").getStringColumn();
			String flightid=paxflightRow.getColumn("flightid").getStringColumn();
			String paxid=paxflightRow.getColumn("paxid").getStringColumn();
			String flightRefundInfo="";
			for (int j = 0; j < flightstab.getRowCount(); j++) {
				Row flightROW=flightstab.getRow(j);
				if (flightid.equals(flightROW.getColumn("id").getStringColumn())) {
					flightRefundInfo=flightROW.getColumn("refundinfo").getStringColumn();
					break;
				}
			}
			if(!fliHelpSet.contains(flightid) && StringUtils.hasLength(flightRefundInfo)){
				fliHelpSet.add(flightid);
				//航班产品退改规则
				Penalty penaltyFlight = penaltyList.addNewPenalty();
				penaltyFlight.setObjectKey(mapping.getAllMap().get(flightid));
				penaltyFlight.addNewApplicableFeeRemarks().addNewRemark().setStringValue(flightRefundInfo);
			}
			
			//座位退改规则
			String seatRefundInfo="";
			String seatno="";
			String seatname="";
			for (int j = 0; j < seatstab.getRowCount(); j++) {
				Row seatRow=seatstab.getRow(j);
				if (paxid.equals(seatRow.getColumn("paxid").getStringColumn()) 
						&& flightid.equals(seatRow.getColumn("flightid").getStringColumn())) {
					seatRefundInfo=seatRow.getColumn("refundinfo").getStringColumn();
					seatno=seatRow.getColumn("seatno").getStringColumn();
					seatname=seatRow.getColumn("seatname").getStringColumn();
					break;
				}
			}
			if(!StringUtils.hasLength(seatno)){
				seatno = seatname;
			}
			if (StringUtils.hasLength(seatRefundInfo)) {
				Penalty penaltySeat=penaltyList.addNewPenalty();
				penaltySeat.setObjectKey(paxflightId+seatno+_FOR_SALE);
				penaltySeat.addNewApplicableFeeRemarks().addNewRemark().setStringValue(seatRefundInfo(seatRefundInfo));
			}
			
			//辅营的退改规则
			for (int j = 0; j < surbkets.getRowCount(); j++) {
				Row rowSurbket =surbkets.getRow(j);
				if (paxid.equals(rowSurbket.getColumn("paxid").getStringColumn())) {
					String code=rowSurbket.getColumn("submarketcode").getStringColumn();
					String surbketrefundInfo=rowSurbket.getColumn("refundinfo").getStringColumn();
					if(!subHelpSet.contains(code) && StringUtils.hasLength(surbketrefundInfo)){
						Penalty penaltySurbket=penaltyList.addNewPenalty();
						penaltySurbket.setObjectKey(code+_FOR_SALE);
						penaltySurbket.addNewApplicableFeeRemarks().addNewRemark().setStringValue(surbketrefundInfo);
						subHelpSet.add(code);
					}
				}
			}
			//存放每种fee的code
			Set<String> setHelp = new HashSet<String>();
			//fee的退改规则
			for(int j = 0; j < costsTable.getRowCount(); j++){
				Row costRow = costsTable.getRow(j);
				String chargeType = costRow.getColumn("chargeType").getStringColumn();
				if("Fee".equals(chargeType)){
					//fee的code
					String chargeCode = costRow.getColumn("chargeCode").getStringColumn();
					if(!setHelp.contains(chargeCode)){
						setHelp.add(chargeCode);
						//fee的退改规则
						String refundinfo = costRow.getColumn("refundinfo").getStringColumn();
						Penalty penaltySurbket=penaltyList.addNewPenalty();
						penaltySurbket.setObjectKey(chargeCode);
						penaltySurbket.addNewApplicableFeeRemarks().addNewRemark().setStringValue(refundinfo);
					}
				}
			}
		}
	}
	
	
	
	/**
	 * 品牌信息
	 * 
	 * @param priceClassList
	 *            ListOfPriceClassType
	 * @param flightsrow
	 *            Row
	 */
	private void priceClass(ListOfPriceClassType priceClassList, String segFlightno,String familyname,String familycode) {
			// 品牌信息
			PriceClassType priceClass = priceClassList.addNewPriceClass();
			priceClass.setName(familyname);
			priceClass.setCode(familycode);
			Association association = priceClass.addNewAssociations().addNewAssociation();
			association.addNewServiceReference().setStringValue(segFlightno);
	}
	
	/**
	 * 航班信息
	 * 
	 * @param flight
	 *            Flight
	 * @param flightsrow 航班信息
	 *            Row
	 * @param payStatus 航段状态
	 *            String void 返回类型
	 *
	 */
	
	/**
	 * 将航班信息放入RS-XML
	 * @param mapping
	 * @param flightArry
	 * @throws ParseException 
	 */
	private void addFlightList(Mapping mapping,FlightSegmentList flightArry,CommandRet commandRet,ListOfPriceClassType priceClassList) throws ParseException{
		Table flightsTable = commandRet.getParm("flights").getTableColumn();
		Map<String,Row> flightsMap = new TreeMap<String,Row>();
		int i = 0;
		for(Row flights:flightsTable){
			i++;
			String flightid = flights.getColumn("id").getStringColumn();
			String segFlightno = SEG+i;
			//费率
			String vatRate = flights.getColumn("vatRate").getStringColumn();
			mapping.getVatRateMap().put(flightid, vatRate);
			flights.addColumn(SEG,segFlightno);
			mapping.getAllMap().put(flightid, segFlightno);
			flightsMap.put(flightid, flights);
		}
		mapping.setFlightsMap(flightsMap);
		for (Entry<String,Row> entry : flightsMap.entrySet()) {
			String flightId = entry.getKey();
			Row flights = entry.getValue();
			ListOfFlightSegmentType fsArry = flightArry.addNewFlightSegment();
			String segmentKey = mapping.getAllMap().get(flightId);
			fsArry.setSegmentKey(segmentKey);
			Departure Departure = fsArry.addNewDeparture();
			//航班编号
			String flightno = flights.getColumn("flightno").getStringColumn();
			//出发地三字码
			String oricode = flights.getColumn("oricode").getStringColumn();
			//出发日期
			String oriDay = flights.getColumn("oriDay").getStringColumn();
			if(oriDay != null && !"".equals(oriDay)){
				Calendar oridate = DateUtils.getInstance().parseDate(oriDay, "yyyyMMdd");
				// 出发时间
				String oriTime = flights.getColumn("oriTime").getStringColumn();
				Departure.addNewAirportCode().setStringValue(oricode);
				if (oridate != null) {
					Departure.setDate(oridate);
					Departure.setTime(oriTime);
				}
			}
			FlightArrivalType ftArry = fsArry.addNewArrival();
			//到达地三字码
			String destcode = flights.getColumn("destcode").getStringColumn();
			//到达日期
			String destDateTime = flights.getColumn("destDateTime").getStringColumn();
			if(destDateTime != null && !"".equals(destDateTime) && destDateTime.length() >= 10 ){
				destDateTime = destDateTime.substring(0, 10);
				Calendar destDate = DateUtils.getInstance().parseDate(destDateTime, "yyyy-MM-dd");
				ftArry.addNewAirportCode().setStringValue(destcode);
				ftArry.setDate(destDate);
				//到达时间
				String destTime = flights.getColumn("destTime").getStringColumn();
				ftArry.setTime(destTime);
			}
			MarketingCarrierFlightType mcfArry = fsArry.addNewMarketingCarrier();
			//承运航编号
			String carricd = flights.getColumn("carricd").getStringColumn();
			mcfArry.addNewAirlineID().setStringValue(carricd);
			FlightNumber flightNumberArry = mcfArry.addNewFlightNumber();
			if (flightno.substring(flightno.length()-1).matches("[A-Z]")) {
				//航班后缀
				flightNumberArry.setOperationalSuffix(flightno.substring(flightno.length()-1));
				flightNumberArry.setStringValue(flightno.substring(0, flightno.length()-1));
			} else {
				flightNumberArry.setOperationalSuffix("");
				flightNumberArry.setStringValue(flightno);
			}
			//舱位
			String cabin = flights.getColumn("cabin").getStringColumn();
			mcfArry.setResBookDesigCode(cabin);
			//机型
			String carriflightno = flights.getColumn("planestype").getStringColumn();
			fsArry.addNewEquipment().addNewAircraftCode().setStringValue(carriflightno);
			// 出发航站
			Departure.addNewTerminal().setName(flights.getColumn("oriteminal").getStringColumn().toUpperCase());
			String language=ApiServletHolder.getApiContext().getLanguage();
			Departure.setAirportName(flights.getColumn("oriname").getStringColumn());
			// 到达航站
			ftArry.addNewTerminal().setName(
					flights.getColumn("destterminal").getStringColumn().toUpperCase());
			ftArry.setAirportName(flights.getColumn("destname").getStringColumn());
			//舱位
			String routtype = flights.getColumn("routtype").getStringColumn();
			if(StringUtils.hasLength(routtype) && "G".equals(routtype)){
				routtype = "Outbound";
			}else if(StringUtils.hasLength(routtype) && "R".equals(routtype)){
				routtype = "Inbound";
			}
			FlightDetailType addNewFlightDetail = fsArry.addNewFlightDetail();
			addNewFlightDetail.addNewFlightSegmentType().setCode(routtype);
			CommandData data = flights.getColumn("familyname").getObjectColumn();
			String familyname = "";
			if ("zh_CN".equals(language)) {
				familyname = data.getParm("zh_CN").getStringColumn();
			}else{
				familyname = data.getParm("en_US").getStringColumn();
			}
			String familycode = flights.getColumn("familycode").getStringColumn();
			priceClass(priceClassList, segmentKey,familyname,familycode);
			//飞行时长
			long l = 0;
			if(!"".equals(flights.getColumn("traveltime").getStringColumn())){
				l = Long.parseLong(flights.getColumn("traveltime").getStringColumn());
			}
			long hour = l / 60;
			long min = l %60;
			long s = 0;
			BigDecimal fraction = new BigDecimal(0);
			GDuration gDuration = new GDuration(1, 0, 0, 0, Integer.parseInt(String.valueOf(hour)),
					Integer.parseInt(String.valueOf(min)), Integer.parseInt(String.valueOf(s)), fraction);
			addNewFlightDetail.addNewFlightDuration().setValue(gDuration);
		}
	}
	
	/**
	 * 添加辅营信息RS-xml
	 * @param commandRet
	 * @param eatList
	 */
	@SuppressWarnings("deprecation")
	private void addServiceList(CommandRet commandRet,ServiceList serviceListArry,Map<String,List<Row>> subMap,Mapping mapping){
		Table submarketsTable = commandRet.getParm("submarkets").getTableColumn();
		Table deliverySubmarTable = commandRet.getParm("deliverSubmarkt").getTableColumn();
		Set<String> submarketcodeSet = new HashSet<>();
		Map<String, ServiceDetailType> map = new HashMap<>();
		ServiceDetailType serviceArry = null;
		if(submarketsTable != null && submarketsTable.getRowCount() > 0){
			for(Row submarketRow:submarketsTable){
				//辅营代码
				String submarketcode = submarketRow.getColumn("submarketcode").getStringColumn();
				String buynum = submarketRow.getColumn("buynum").getStringColumn();
				//是否免费
				String isfree = submarketRow.getColumn("isfree").getStringColumn();
				String flightid = submarketRow.getColumn("flightid").getStringColumn();
				String paxid=submarketRow.getColumn("paxid").getStringColumn();
				if(!submarketcodeSet.contains(submarketcode+isfree)){
					submarketcodeSet.add(submarketcode+isfree);
					//辅营品牌代码
					String submarkettype = submarketRow.getColumn("submarkettype").getStringColumn();
					//辅营名称
					String submarketname =submarketRow.getColumn("submarketname").getStringColumn();
					//辅营描述
					String submarketdesc = submarketRow.getColumn("submarketdesc").getStringColumn();
					//辅营价格
					BigDecimal submarketprice = submarketRow.getColumn("unitprice").getBigDecimalColumn();
					//拼返回xml
					serviceArry = serviceListArry.addNewService();
					//辅营code
					if("Y".equals(isfree)){
						serviceArry.addNewServiceID().setStringValue(submarketcode+_FOR_FREE);
					}else if("N".equals(isfree)){
						serviceArry.addNewServiceID().setStringValue(submarketcode+_FOR_SALE);
					}
					//辅营名称
					serviceArry.addNewName().setStringValue(submarketname);
					ServiceEncodingType newEncoding = serviceArry.addNewEncoding();
					//辅营品牌代码
					newEncoding.addNewCode().setStringValue(submarkettype);
					//辅营描述
					serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue(submarketdesc);
					CurrencyAmountOptType  priceArry = serviceArry.addNewPrice().addNewTotal();
					//货币三字码  、金额
					priceArry.setCode(currencyCode);
					priceArry.set(submarketprice);
					BookingInstructions addNewBookingInstructions = serviceArry.addNewBookingInstructions();
					addNewBookingInstructions.addNewSSRCode().setStringValue(submarketcode);
					
					if("XBAG".equals(submarkettype) && "Y".equals(isfree)){
						String weight = submarketRow.getColumn("weight").getStringColumn();
						addNewBookingInstructions.addNewText().setStringValue(weight);
					}
					serSubmarketsMap(paxid, submarketRow, subMap);
					map.put(submarketcode+isfree, serviceArry);
				}else{
					serviceArry = map.get(submarketcode+isfree);
				}
				//service--->Associations
				Associations associations = serviceArry.addNewAssociations();
				List<String> paxList=new ArrayList<String>();
				paxList.add(mapping.getAllMap().get(paxid));
				associations.addNewTraveler().setTravelerReferences(paxList);
				associations.addNewFlight().addNewSegmentReferences().setStringValue(mapping.getAllMap().get(flightid));
				//辅营状态
				String invstatus=submarketRow.getColumn("invstatus").getStringColumn();
				String salestatus=submarketRow.getColumn("salestatus").getStringColumn();
				String id= submarketRow.getColumn("id").getStringColumn();
				String deliverytatus="";
				if(deliverySubmarTable != null){
					for (int j = 0; j < deliverySubmarTable.getRowCount(); j++) {
						Row row=deliverySubmarTable.getRow(j);
						String productid=row.getColumn("productid").getStringColumn();
						if (productid.equals(id)) {
							deliverytatus=row.getColumn("status").getStringColumn();
							break;
						}
					}
				}
				String submarketStatus = "";
				if(StringUtils.hasLength(salestatus) && "EXCHANGED".equals(salestatus)){
					submarketStatus = "13";
				}else{
					submarketStatus = StatusUtil.getStatus(invstatus, salestatus, deliverytatus);
				}
				associations.setStatusCode(submarketStatus);
				associations.setNum(buynum);
			}
		}
	}
	
	/**
	 * 设置旅客与辅营的关联关系
	 * @param paxid
	 * @param submarketid
	 * @param subMap
	 */
	public void serSubmarketsMap(String paxid,Row submarkets,Map<String,List<Row>> subMap){
		List<Row> submarketList = subMap.get(paxid);
		if(null == submarketList){
			submarketList = new ArrayList<Row>();
		}
		submarketList.add(submarkets);
		subMap.put(paxid, submarketList);
	}
	
	/**
	 * OrderItems
	 * @param commandRet
	 * @param OrderItems
	 * @param mapping
	 * @throws ParseException 
	 */
	private void addPaxflight(CommandRet commandRet,OrderItems OrderItems,Mapping mapping,
			Map<String,List<Row>> submarketMap ,Map<String, String> seatRefundMap,String language,Map<String, Row> helpMap) throws ParseException{
		Map<String, String> seatHelpMap = new HashMap<String, String>();
		Map<String, String> subHelpMap = new HashMap<String, String>();
		Table flightsTable = commandRet.getParm("flights").getTableColumn();
		Table paxsTable = commandRet.getParm("paxs").getTableColumn();
		Table seatsTable = commandRet.getParm("seats").getTableColumn();
		Table costsTable = commandRet.getParm("costs").getTableColumn();
		if(seatsTable != null && seatsTable.getRowCount() > 0){
			for (Row seatRow : seatsTable) {
				String seatPaxid = seatRow.getColumn("paxid").getStringColumn();
				String seatFlightid = seatRow.getColumn("flightid").getStringColumn();
				String seatIsfree = seatRow.getColumn("isfree").getStringColumn();
				seatHelpMap.put(seatPaxid+seatFlightid, seatIsfree);
			}
		}
		Table paxflights = commandRet.getParm("paxflights").getTableColumn();
		if(paxflights != null && paxflights.getRowCount() > 0){
			for(Row paxflight:paxflights){
				PNR = paxflight.getColumn("pnr").getStringColumn();
				//人航段id
				String id = paxflight.getColumn("id").getStringColumn();
				helpMap.put(id, paxflight);
			}	
		}
		if(helpMap != null && helpMap.size() > 0){
			for (Entry<String, Row> paxFlightid : helpMap.entrySet()) {
				Row paxflight = paxFlightid.getValue();
				//航班id
				String flightid = paxflight.getColumn("flightid").getStringColumn();
				//人员id
				String paxid = paxflight.getColumn("paxid").getStringColumn();
				//人航段id
				String id = paxflight.getColumn("id").getStringColumn();
				//航班数据
				Row flights = mapping.getFlightsMap().get(flightid);
				// OrderItem -->Timestamp
				OrderItem  orderItem =	OrderItems.addNewOrderItem();
				String createtime = commandRet.getParm("createtime").getStringColumn();
				if (!"".equals(createtime) && String.valueOf(createtime).length()!=0 && String.valueOf(createtime)!=null && !String.valueOf(createtime).equals(null)) {
					Calendar orderCreate = Calendar.getInstance();
					orderCreate.setTime(Unit.getDate(createtime, "yyyy-MM-dd HH:mm:ss"));
					orderItem.setTimestamp(orderCreate);
				}else{
					orderItem.setTimestamp(null);
				}
				ItemIDType ItemIDArry = orderItem.addNewOrderItemID();
				//OrderItemID -->Owner
				airlinecd = flights.getColumn("airlinecd").getStringColumn();
				ItemIDArry.setOwner(airlinecd);
				ItemIDArry.setStringValue(id);
				
				// OrderItem  --> FlightItem
				FlightItemType flightItem = orderItem.addNewFlightItem();
				
				//FlightItem -->Price
				Price price1 = flightItem.addNewPrice();
				fareDetail(price1, paxflight,costsTable,language);
				
				//FlightItem -->FareDetail  (费率)
				String vatRate = mapping.getVatRateMap().get(flightid);
				flightItem.addNewFareDetail().addNewRemarks().addNewRemark().setStringValue(vatRate);;
				
				//FlightItem --> OriginDestination --> Flight
				Flight flight = flightItem.addNewOriginDestination().addNewFlight();
				//Flight -->refs
				flight.setRefs(getList(mapping.getAllMap().get(flightid)));
//						flt =flt+1;
				//Flight -->Departure
				Departure departure = flight.addNewDeparture();
				//出发机场三字码
				String oricode = flights.getColumn("oricode").getStringColumn();
				//出发日期
				String oriDay = flights.getColumn("oriDay").getStringColumn();
				if(oriDay != null && !"".equals(oriDay)){
					Calendar oridate = DateUtils.getInstance().parseDate(oriDay, "yyyyMMdd");
					// 出发时间
					String oriTime = flights.getColumn("oriTime").getStringColumn();
					if (oridate != null) {
						//Departure --> AirportCode
						departure.addNewAirportCode().setStringValue(oricode);
						departure.setDate(oridate);
						departure.setTime(oriTime);
					}
				}
				//Flight -->arrival
				FlightArrivalType arrival = flight.addNewArrival();
				//到达机场三字码
				String destcode = flights.getColumn("destcode").getStringColumn();
				//到达日期
				String destDateTime = flights.getColumn("destDateTime").getStringColumn();
				if(destDateTime != null && !"".equals(destDateTime) && destDateTime.length() >= 10 ){
					destDateTime = destDateTime.substring(0, 10);
					Calendar destDate = DateUtils.getInstance().parseDate(destDateTime, "yyyy-MM-dd");
					//arrival --> AirportCode
					arrival.addNewAirportCode().setStringValue(destcode);
					arrival.setDate(destDate);				
					//到达时间
					String destTime = flights.getColumn("destTime").getStringColumn();
					arrival.setTime(destTime);
				}
				//Flight -->MarketingCarrier
				MarketingCarrierFlightType markeCarr = flight.addNewMarketingCarrier();
				//MarketingCarrier --> AirlineID
				markeCarr.addNewAirlineID().setStringValue(airlinecd);
				//航班号
				String flightno = flights.getColumn("flightno").getStringColumn();
				//MarketingCarrier --> FlightNumber
				FlightNumber fnArry = markeCarr.addNewFlightNumber();
				
				if (flightno.substring(flightno.length()-1).matches("[A-Z]")) {
					//航班后缀
					fnArry.setOperationalSuffix(flightno.substring(flightno.length()-1));
					fnArry.setStringValue(flightno.substring(0,flightno.length()-1));
				} else {
					fnArry.setOperationalSuffix("");
					fnArry.setStringValue(flightno);
				}
				
				//Flight -->Status
				String status="";
				String reason="";
				Table deliverCabinTab=commandRet.getParm("deliverCabin").getTableColumn();
				if (deliverCabinTab != null) {
				for (int i = 0; i < deliverCabinTab.getRowCount(); i++) {
					Row deliverRow=deliverCabinTab.getRow(i);
					String deliverCabinFlightId=deliverRow.getColumn("flightid").getStringColumn();
					String deliverCabinPaxId=deliverRow.getColumn("paxid").getStringColumn();
					if (flightid.equals(deliverCabinFlightId) && paxid.equals(deliverCabinPaxId)) {
						status=deliverRow.getColumn("status").getStringColumn();
						reason=deliverRow.getColumn("reason").getStringColumn();
						break;
					}
				}
				}
				String cabin ="";
				String cabinName ="";
				Table flightTable = commandRet.getParm("flights").getTableColumn();
				for (int i = 0; i < flightTable.getRowCount(); i++) {
					Row flightRow=flightTable.getRow(i);
					
					if (flightid.equals(flightRow.getColumn(ID).getStringColumn())) {
						cabin=flightRow.getColumn("cabin").getStringColumn();
						cabinName=flightRow.getColumn("cabinName").getStringColumn();
						break;
					}
				}
				String invstatus=paxflight.getColumn("invstatus").getStringColumn();
				String salestatus=paxflight.getColumn("salestatus").getStringColumn();
				String retStatus ="";
				//收益目的主动清座    待退款
				if("NO".equals(invstatus) && "TOREFUND".equals(salestatus)){
					retStatus = "14";
				//航班保护取消    待退款
				}else if("UN".equals(invstatus) && "TOREFUND".equals(salestatus)){
					retStatus = "14";
				//当航段的销售状态为已变更时，详情中航段的状态应为已变更
				}else if(StringUtils.hasLength(salestatus) && "EXCHANGED".equals(salestatus)){
					retStatus = "13";
				//交付状态是“无法交付（GOSHOW）” —显示状态为“已变更”
				}else if(StringUtils.hasLength(status) && "DS".equals(status) && "GS".equals(reason)){
					retStatus = "13";
				//交付状态是“已交付（GOSHOW）” —显示 状态为 “已使用”
				}else if(StringUtils.hasLength(status) && "USED".equals(status) && "GS".equals(reason)){
					retStatus = "10";
				}else{
					retStatus=StatusUtil.getStatus(invstatus, salestatus, status);
				}
				flight.addNewStatus().addNewStatusCode().setCode(retStatus);
				//Flight -->ClassOfService
				FlightCOSCoreType costype = flight.addNewClassOfService();
				costype.addNewCode().setStringValue(cabin);
				MarketingName newMarketingName = costype.addNewMarketingName();
				newMarketingName.setCabinDesignator(cabinName);
				
				// OrderItem  --> SeatItem
				Row seatRow = mapping.getSeatsMap().get(flightid+paxid);
				SeatItem seatItem = null;
				boolean addseatItem = false;
				if(null!=seatRow){
					addseatItem = true;
					String price  = seatRow.getColumn("price").getStringColumn();
					String seatno  = seatRow.getColumn("seatno").getStringColumn();
					if(!StringUtils.hasLength(seatno)){
						seatno = seatRow.getColumn("seatname").getStringColumn();
					}
					// SeatItem
					 seatItem = orderItem.addNewSeatItem();
					//SeatItem -->Price
					org.iata.iata.edist.SeatItemType.Price SeatItemprice = seatItem.addNewPrice();
					//Price -->Total
					CurrencyAmountOptType total = SeatItemprice.addNewTotal();		
					total.setCode(currencyCode);
					total.setStringValue(price);
					
					//Price -->Details
					if(StringUtils.hasLength(seatRefundMap.get(paxid+flightid))){
						Detail priceDetail = SeatItemprice.addNewDetails().addNewDetail();
						priceDetail.addNewSubTotal().setBigDecimalValue(BigDecimal.valueOf(Double.valueOf(seatRefundMap.get(paxid+flightid))));
					}
					//SeatItem -->SeatReference
					seatItem.addNewSeatReference().setStringValue(id+seatno+_FOR_SALE);
				}
				String isseatfree = flights.getColumn("isseatfree").getStringColumn();
				if (StringUtils.hasLength(isseatfree) && "Y".equals(isseatfree)) {
					if (addseatItem) {
						seatItem.addNewDescriptions().addNewDescription().addNewText().setStringValue(FOR_FREE);
					}else {
						seatItem = orderItem.addNewSeatItem();
						seatItem.addNewDescriptions().addNewDescription().addNewText().setStringValue(FOR_FREE);
					}
					
				}
				// OrderItem  --> BaggageItem
				BaggageItemType baggageItemService = orderItem.addNewBaggageItem();
				//BaggageItem  --> BagDetails
				MaximumWeight maximumWeight = baggageItemService.addNewBagDetails().addNewBagDetail().addNewCarryOnBags().addNewCarryOnBag().addNewWeightAllowance().addNewMaximumWeight();
				String paxtype = "";
				if(paxsTable != null && paxsTable.getRowCount() > 0){
					for (int i = 0; i < paxsTable.getRowCount(); i++) {
						Row paxRow = paxsTable.getRow(i);
						if(paxRow.getColumn("id").getStringColumn().equals(paxid)){
							paxtype = paxRow.getColumn("paxtype").getStringColumn();
							break;
						}
					}
				}
				//当前航班的关联的免费行李重量
				String paxLuggage = "";
				String weightUnit = "";
				if(flightsTable != null && flightsTable.getRowCount() > 0){
					for (int j = 0; j < flightsTable.getRowCount(); j++) {
						Row flightRow = flightsTable.getRow(j);
						if(flightRow.getColumn("id").getStringColumn().equals(flightid)){
							weightUnit = flightRow.getColumn("weightUnit").getStringColumn();
							if("ADT".equals(paxtype)){
								paxLuggage = flightRow.getColumn("adultluggage").getStringColumn();
							}else if("CHD".equals(paxtype)){
								paxLuggage = flightRow.getColumn("childluggage").getStringColumn();
							}else if("INF".equals(paxtype)){
								paxLuggage = flightRow.getColumn("babyluggage").getStringColumn();
							}
							break;
						}
					}
				}
				maximumWeight.setValue(new BigDecimal(paxLuggage));
				if(StringUtils.hasLength(weightUnit) && "KG".equals(weightUnit.toUpperCase())){
					maximumWeight.setUOM(Enum.forString("Kilogram"));
				}else if(StringUtils.hasLength(weightUnit) && "LB".equals(weightUnit.toUpperCase())){
					maximumWeight.setUOM(Enum.forString("Pound"));
				}
				//BaggageItem  --> Services
				Services newServices = baggageItemService.addNewServices();
				// OrderItem  --> OtherItem
				org.iata.iata.edist.OtherItemType.Services  otherItemService = orderItem.addNewOtherItem().addNewServices();
				String paxNum = mapping.getAllMap().get(paxid);
				Table submarketsTable = commandRet.getParm("submarkets").getTableColumn();
				String ids ="";
				if(submarketsTable != null && submarketsTable.getRowCount() > 0){
					for (int i = 0; i < submarketsTable.getRowCount(); i++) {
						Row submarketRow = submarketsTable.getRow(i);
						String isfree =	submarketRow.getColumn("isfree").getStringColumn();
						String submarkettype = submarketRow.getColumn("submarkettype").getStringColumn();
						String submarketid = submarketRow.getColumn("submarketcode").getStringColumn();
						String subflightid = submarketRow.getColumn("flightid").getStringColumn();
						String subpaxid = submarketRow.getColumn("paxid").getStringColumn();
						subHelpMap.put(subpaxid+subflightid, isfree);
						int buynum = submarketRow.getColumn("buynum").getIntegerColumn();
						if(flightid.equals(subflightid) && paxid.equals(subpaxid)){
							if("Y".equals(isfree)){
								ids=submarketid+_FOR_FREE+" "+ids;
							}else{
								if("XBAG".equals(submarkettype)){
									for(int m=0;m<buynum;m++){
										newServices.addNewServiceID().setStringValue(submarketid+_FOR_SALE);
									}
								}else{
									for(int n=0;n<buynum;n++){
										otherItemService.addNewServiceID().setStringValue(submarketid+_FOR_SALE);
									}
								}
							}
						}
					}
					mapping.getServiceBundleMap().put(id, ids);
				}
				// OrderItem  --> Associations
				org.iata.iata.edist.OrderItemCoreType.OrderItem.Associations Association = orderItem.addNewAssociations();
				//旅客编号
				Association.addNewPassengers().setPassengerReferences(getList(paxNum));
				String seatIsFree = seatHelpMap.get(paxid+flightid);
				String subIsFree = subHelpMap.get(paxid+flightid);
				if("Y".equals(seatIsFree) || "Y".equals(subIsFree)){
					Association.addNewIncludedService().setBundleReference(id);
				}else{
					ServiceInfoAssocType newIncludedService = Association.addNewIncludedService();
					newIncludedService.setBundleReference("");
				} 
			}
		}
	}
	
	/**
	 * 票价信息
	 * 
	 * @param farePriceDetailType
	 *            FarePriceDetailType
	 * @param paxflightsrow  航段信息
	 *            Row void 返回类型
	 *
	 */
	private void fareDetail(Price price, Row paxflightsrow,Table costsTable,String language) {
		// 机票价格Price --> BaseAmount
		CurrencyAmountOptType amount = price.addNewBaseAmount();
		String ticketprice = paxflightsrow.getColumn("ticketprice").getStringColumn();
		//人员id
		String paxid = paxflightsrow.getColumn("paxid").getStringColumn();
		//人航段id
		String flightid = paxflightsrow.getColumn("flightid").getStringColumn();
		if(StringUtils.hasLength(ticketprice)){
			amount.setStringValue(ticketprice);
		}else{
			amount.setStringValue("0");
		}
		amount.setCode(currencyCode);
		// 税费信息Price --> Taxes --> Breakdown
		Breakdown breakdown = price.addNewTaxes().addNewBreakdown();
		Table taxs = paxflightsrow.getColumn("taxs").getTableColumn();
		if (taxs != null) {
			for(Row taxrow : taxs){
				Tax tax = breakdown.addNewTax();
				CurrencyAmountOptType cnamount = tax.addNewAmount();
				cnamount.setStringValue(taxrow.getColumn("price").getStringColumn());
				cnamount.setCode(currencyCode);
				tax.setTaxCode(taxrow.getColumn("code").getStringColumn());
				tax.setTaxType(taxrow.getColumn("name").getStringColumn());
			}
		}
		//添加VAT 增值税
		if(costsTable != null && costsTable.getRowCount() > 0){
			for (int i = 0; i < costsTable.getRowCount(); i++) {
				Row costsRow = costsTable.getRow(i);
				if(paxid.equals(costsRow.getColumn("paxid").getStringColumn()) 
						&& flightid.equals(costsRow.getColumn("flightid").getStringColumn())){
					String bookid = costsRow.getColumn("bookid").getStringColumn();
					String vatSource = costsRow.getColumn("vatSource").getStringColumn();
					String vatcount = costsRow.getColumn("vatcount").getStringColumn();
					if(!StringUtils.hasLength(bookid) && VAT_PRODUCT.equals(vatSource) && ONE.equals(vatcount)){
						Tax tax = breakdown.addNewTax();
						CurrencyAmountOptType cnamount = tax.addNewAmount();
						cnamount.setStringValue(costsRow.getColumn("price").getStringColumn());
						cnamount.setCode(currencyCode);
						tax.setTaxCode(costsRow.getColumn("chargeCode").getStringColumn());
						tax.setTaxType(StatusUtil.getLanguageName(costsRow.getColumn("name").getObjectColumn(), language));
					}
				}
			}
		}
	}
	
	/**
	 * 辅营与品牌关联的节点
	 * @param listOfServiceBundleArry
	 */
	public void addServiceBundleListArry(Mapping mapping,ListOfServiceBundleType listOfServiceBundleArry){
		Map<String ,String> ServiceBundleMap= mapping.getServiceBundleMap();
		for(Entry<String,String> freeEntry :ServiceBundleMap.entrySet()){
			//辅营id
			String submarketid =freeEntry.getValue().trim();
			if(!"".equals(submarketid) && null != submarketid ){
				ServiceBundle serviceBundleArry = listOfServiceBundleArry.addNewServiceBundle();
				String[] submarketidS = submarketid.split(" ");
				//商品 id
				String brandid = freeEntry.getKey();
				serviceBundleArry.setListKey(brandid);
				serviceBundleArry.setItemCount(BigInteger.valueOf(submarketidS.length));
				org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle.Associations associationsArry = serviceBundleArry.addNewAssociations();
				for(String id:submarketidS){
					associationsArry.addNewServiceReference().setStringValue(id);
				}
			}
		}
	}
	
	/**
	 * payments
	 * @param order
	 * @param xmlOutput
	 * @param input
	 */
//	private void orderInfos(Order order, CommandRet xmlOutput, CommandData input) {
//		// 支付
//		Payments payments = order.addNewPayments();
//		Table flightsTable = xmlOutput.getParm("flights").getTableColumn();
//		if(flightsTable != null){
//			for (int j = 0; j < flightsTable.getRowCount(); j++) {
//				Row flightRow = flightsTable.getRow(j);
//				airlinecd = flightRow.getColumn("airlinecd").getStringColumn();
//				if(StringUtils.hasLength(airlinecd)){
//					break;
//				}
//			}
//		}
//		Table paystable = xmlOutput.getParm(PAYS).getTableColumn();
//		if (paystable != null) {
//			for (int i = 0; i < paystable.getRowCount(); i++) {
//				Row paysrow = paystable.getRow(i);
//				payInfo(payments, paysrow,airlinecd);
//			}
//		}
//	}
	
	/**
	 * 航班于OD关联
	 * @param commandRet
	 * @param flightListArry
	 * @param originArry
	 */
	public void  addOriginArryArry(CommandRet commandRet ,FlightList flightListArry,OriginDestinationList originArry,Mapping mapping){
		Table flightsTable = commandRet.getParm("flights").getTableColumn();
		//(去程)key值为出发地+目的地，value值为FLT1 FLT2
		Map<String,String> gHelpMap = new HashMap<String, String>();
		//(回程)key值为出发地+目的地，value值为FLT3 FLT4
		Map<String,String> rHelpMap = new HashMap<String, String>();
		//key值为G或R，value为helpMap
		Map<String, Map<String,String>>  helpMap = new HashMap<>();
		
		Map<String, Row> flightMap = new TreeMap<String, Row>();
		for (Row flight:flightsTable) {
			String flightId = flight.getColumn("id").getStringColumn();
			String segNo = mapping.getAllMap().get(flightId);
			String flightKey = FLT+segNo.substring(3, 4);
			mapping.getAllMap().put(segNo, flightKey);
			flightMap.put(segNo, flight);
		}			
		
		if(flightMap != null && flightMap.size() > 0){
			for (Entry<String, Row> entry : flightMap.entrySet()) {
				String segNo = entry.getKey();
				Row flightRow = entry.getValue();
				String routtype = flightRow.getColumn("routtype").getStringColumn();
				String oricode = flightRow.getColumn("oricode").getStringColumn();
				String destcode = flightRow.getColumn("destcode").getStringColumn();
				String mapKey = oricode+" "+destcode;
				String flightKey = mapping.getAllMap().get(segNo);
				org.iata.iata.edist.DataListType.FlightList.Flight  flightArry = flightListArry.addNewFlight();
				flightArry.addNewSegmentReferences().setStringValue(segNo);
				flightArry.setFlightKey(flightKey);
				
				if(helpMap != null && helpMap.size() > 0){
					if(StringUtils.hasLength(routtype) && "G".equals(routtype)){
						if(helpMap.containsKey(routtype)){
							gHelpMap = helpMap.get(routtype);
							if(!gHelpMap.containsKey(mapKey)){
								gHelpMap.put(mapKey, flightKey);
							}else{
								String flightKey1 = gHelpMap.get(mapKey);
								gHelpMap.put(mapKey, flightKey1+" "+flightKey);
							}
						}else{
							gHelpMap.put(mapKey, flightKey);
						}
						helpMap.put(routtype,gHelpMap);
					}else if(StringUtils.hasLength(routtype) && "R".equals(routtype)){
						if(helpMap.containsKey(routtype)){
							rHelpMap = helpMap.get(routtype);
							if(!rHelpMap.containsKey(mapKey)){
								rHelpMap.put(mapKey, flightKey);
							}else{
								String flightKey2 = rHelpMap.get(mapKey);
								rHelpMap.put(mapKey, flightKey2+" "+flightKey);
							}
						}else{
							rHelpMap.put(mapKey, flightKey);
						}
						helpMap.put(routtype, rHelpMap);
					}
				}else{
					if(StringUtils.hasLength(routtype) && "G".equals(routtype)){
						gHelpMap.put(mapKey, flightKey);
						helpMap.put(routtype, gHelpMap);
					}else if(StringUtils.hasLength(routtype) && "R".equals(routtype)){
						rHelpMap.put(mapKey, flightKey);
						helpMap.put(routtype, rHelpMap);
					}
				}
			}
		}
			
		if(helpMap != null && helpMap.size() == 1){
			Map<String, String> map1 = helpMap.get("G");
			for (Entry<String,String> entry : map1.entrySet()) {
				entry.getKey().split(" ");
				OriginDestination originDestinationArry = originArry.addNewOriginDestination();
				originDestinationArry.addNewDepartureCode().setStringValue(entry.getKey().split(" ")[0]);
				originDestinationArry.addNewArrivalCode().setStringValue(entry.getKey().split(" ")[1]);
				originDestinationArry.addNewFlightReferences().setStringValue(entry.getValue());
			}
		}else if(helpMap != null && helpMap.size() == 2){
			Map<String, String> map2 = helpMap.get("G");
			for (Entry<String,String> entry : map2.entrySet()) {
				entry.getKey().split(" ");
				OriginDestination originDestinationArry = originArry.addNewOriginDestination();
				originDestinationArry.addNewDepartureCode().setStringValue(entry.getKey().split(" ")[0]);
				originDestinationArry.addNewArrivalCode().setStringValue(entry.getKey().split(" ")[1]);
				originDestinationArry.addNewFlightReferences().setStringValue(entry.getValue());
			}
			Map<String, String> map3 = helpMap.get("R");
			for (Entry<String,String> entry : map3.entrySet()) {
				entry.getKey().split(" ");
				OriginDestination originDestinationArry = originArry.addNewOriginDestination();
				originDestinationArry.addNewDepartureCode().setStringValue(entry.getKey().split(" ")[0]);
				originDestinationArry.addNewArrivalCode().setStringValue(entry.getKey().split(" ")[1]);
				originDestinationArry.addNewFlightReferences().setStringValue(entry.getValue());
			}
			
		}
	}
	
	public void  addSeats(CommandRet commandRet,Mapping mapping){
		Table seatsTable = commandRet.getParm("seats").getTableColumn();
		if(null !=seatsTable){
			for(Row seats:seatsTable){
				String flightid = seats.getColumn("flightid").getStringColumn();
				String paxid = seats.getColumn("paxid").getStringColumn();
				mapping.getSeatsMap().put(flightid+paxid, seats);
			}
		}
	}
	
	/**
	 * 客票信息
	 * 
	 * @param xmlOutput
	 *            CommandRet
	 * @param ticketDocInfos
	 *            TicketDocInfos void 返回类型
	 *
	 */
	@SuppressWarnings("unused")
	private void ticketDocInfos(Table paxFlightstab ,	
			TicketDocInfos ticketDocInfos,Mapping mapping) {
		Map<String,Map<String,Row>> ticketNoHelpMap = new HashMap<String, Map<String,Row>>();
		if (paxFlightstab != null) {
			Set<String> ticketNoSet = new HashSet<>();
			for (int i = 0; i < paxFlightstab.getRowCount(); i++) {
				Row paxFlightsRow = paxFlightstab.getRow(i);
			    // 客票编号
			    String ticketno = paxFlightsRow.getColumn("ticketno").getStringColumn();
			    if(!"".equals(ticketno)){
			    	if(!ticketNoSet.contains(ticketno)){
			    		ticketNoSet.add(ticketno);
			    		//航段号
			    		String ticketsegno = paxFlightsRow.getColumn("ticketsegno").getStringColumn();
			    		Map<String,Row> segNoHelpMap = new HashMap<String, Row>();
			    		segNoHelpMap.put(ticketno+ticketsegno, paxFlightsRow);
			    		ticketNoHelpMap.put(ticketno, segNoHelpMap);
			    	}else{
			    		Map<String, Row> segNoMap = ticketNoHelpMap.get(ticketno);
			    		//航段号
			    		String ticketsegno = paxFlightsRow.getColumn("ticketsegno").getStringColumn();
			    		segNoMap.put(ticketno+ticketsegno, paxFlightsRow);
			    	}
			    }
			}
			if(ticketNoHelpMap != null && ticketNoHelpMap.size()>0){
				for (String ticketNo : ticketNoHelpMap.keySet()) {
					Map<String, Row> segMap = ticketNoHelpMap.get(ticketNo);
					TicketDocInfo ticketDocInfo = ticketDocInfos.addNewTicketDocInfo();
					TicketDocument ticketDocument = ticketDocInfo.addNewTicketDocument();
					String paxid = "";
					int i = 1;
					for (String segmentnum : segMap.keySet()) {
						if(i == 1){
							Row segmentRow = segMap.get(ticketNo+"1");
							paxid=segmentRow.getColumn("paxid").getStringColumn();
							ticketDocument.setTicketDocNbr(ticketNo);
							CouponInfoType couponInfoType  = ticketDocument.addNewCouponInfo();
							String ticketsegno = segmentRow.getColumn("ticketsegno").getStringColumn();
							couponInfoType.setCouponNumber(new BigInteger(ticketsegno));
							//航段状态 CouponInfo --> Status ( OPEN FOR USE)
							couponInfoType.addNewStatus().setCode(segmentRow.getColumn("etStatus").getStringColumn());
							couponInfoType.addNewCurrentAirlineInfo().setTourOperatorFlightID("SEG"+ticketsegno);
							i++;
						}else if(i == 2){
							Row segmentRow = segMap.get(ticketNo+"2");
							paxid=segmentRow.getColumn("paxid").getStringColumn();
							CouponInfoType couponInfoType  = ticketDocument.addNewCouponInfo();
							String ticketsegno = segmentRow.getColumn("ticketsegno").getStringColumn();
							couponInfoType.setCouponNumber(new BigInteger(ticketsegno));
							//航段状态 CouponInfo --> Status ( OPEN FOR USE)
							couponInfoType.addNewStatus().setCode(segmentRow.getColumn("etStatus").getStringColumn());
							couponInfoType.addNewCurrentAirlineInfo().setTourOperatorFlightID("SEG"+ticketsegno);
							i--;
						}
					}
					List<String> paxidTab=new ArrayList<String>();
					paxidTab.add(mapping.getAllMap().get(paxid));
					ticketDocInfo.setPassengerReference(paxidTab);
				}    
			}
		}
	}
	
	/**
	 * 支付信息
	 * 
	 * @param payments
	 *            Payments
	 * @param taxrow
	 *            Row void 返回类型
	 *
	 */
//	private void payInfo(Payments payments, Row paysrow,String airlinecd) {
//		if (paysrow != null) {
//			PaymentProcessType payment = payments.addNewPayment();
//			//支付记录ID Associations
//			ServiceIDType payserviceID = payment.addNewAssociations().addNewServices().addNewServiceID();
//			payserviceID.setOwner(airlinecd);
//			payserviceID.setStringValue(paysrow.getColumn("id").getStringColumn());
//			// 支付状态  Status
//			CodesetType codesetType = payment.addNewStatus().addNewStatusCode();
//			codesetType.setCode(paysrow.getColumn("status").getStringColumn());
//			codesetType.setDefinition(paysrow.getColumn("paychannelno").getStringColumn());
//			codesetType.setTableName(paysrow.getColumn("maxpaytime").getStringColumn());
//			codesetType.setLink(paysrow.getColumn("maxpaysecond").getStringColumn());
//			// 支付金额  Amount
//			CurrencyAmountOptType currencyAmountOptType = payment.addNewAmount().addNewDetailCurrencyPrice().addNewTotal();
//			currencyAmountOptType.setCode(currencyCode);
//			currencyAmountOptType.setStringValue(paysrow.getColumn("price").getStringColumn());
//			// 支付银行 Method
//			BankAccountMethodType bankAccountMethod = payment.addNewMethod().addNewBankAccountMethod();
//			bankAccountMethod.addNewName().setStringValue(paysrow.getColumn("bankid").getStringColumn());
//			Remark remark = Remark.Factory.newInstance();
//			remark.setStringValue(paysrow.getColumn("billno").getStringColumn());
//			bankAccountMethod.addNewCheckNumber().set(remark);
//			// 支付时间 Promotions
//		    Promotion  promotion =payment.addNewPromotions().addNewPromotion();
//		    promotion.addNewCode().setStringValue(paysrow.getColumn("orgid").getStringColumn());
//		    promotion.setLink(paysrow.getColumn("apptype").getStringColumn());
//		    promotion.addNewRemarks().addNewRemark().setStringValue(paysrow.getColumn("createtime").getStringColumn());
//		}
//
//	}
	
	public void refundInfo(CommandRet xmlOutput,Response response,Mapping mapping){
		Table refundTab=xmlOutput.getParm("refunds").getTableColumn();
		Table refundDetailTab=xmlOutput.getParm("refunddetails").getTableColumn();
		if (refundTab != null && refundTab.getRowCount()>0) {
			LCCRefundList refundList  = response.addNewLCCRefundList();
			for (int i = 0; i < refundTab.getRowCount(); i++) {
				Row refundRow=refundTab.getRow(i);
				String status = refundRow.getColumn("status").getStringColumn();
				if(!"0".equals(status) && !"5".equals(status) && !"6".equals(status) && !"9".equals(status)){
					LCCRefund refund =refundList.addNewLCCRefund();
					refund.setID(refundRow.getColumn("id").getStringColumn());
					refund.setPnrNo(refundRow.getColumn("pnr").getStringColumn());
					refund.setType(refundRow.getColumn("refundtype").getStringColumn());
					refund.setReason(refundRow.getColumn("refundreasontype").getStringColumn());
					refund.setDescription(refundRow.getColumn("refundreason").getStringColumn());
					refund.setApplicant(refundRow.getColumn("applicant").getStringColumn());
					refund.setTelephone(refundRow.getColumn("telephone").getStringColumn());
					refund.setTicketAmount(refundRow.getColumn("tktam").getStringColumn());
					refund.setCNAmount(refundRow.getColumn("airporttax").getStringColumn());
					refund.setYQAmount(refundRow.getColumn("fueltax").getStringColumn());
					refund.setOtherAmount(refundRow.getColumn("taxam").getStringColumn());
					refund.setServiceAmount(refundRow.getColumn("submarketam").getStringColumn());
					refund.setSeatAmount(refundRow.getColumn("seatam").getStringColumn());
					refund.setTotalAmount(refundRow.getColumn("refundam").getStringColumn());
					refund.setDate(refundRow.getColumn("checkdate").getStringColumn());
					refund.setStatus(status);
					refund.setAttachment(refundRow.getColumn("attachment").getStringColumn());
					LCCRefundDetailList refundDetailList =refund.addNewLCCRefundDetailList();
					if(refundDetailTab != null && refundDetailTab.getRowCount() > 0){
							for (int j = 0; j < refundDetailTab.getRowCount(); j++) {
								Row refundDetailRow=refundDetailTab.getRow(j);
								String refundid=refundDetailRow.getColumn("refundid").getStringColumn();
								if (refundid.equals(refundRow.getColumn("id").getStringColumn())) {
									LCCRefundDetail  refundDetail=refundDetailList.addNewLCCRefundDetail();
									refundDetail.setID(refundDetailRow.getColumn("id").getStringColumn());
									String paxid = refundDetailRow.getColumn("paxid").getStringColumn();
									String flightid = refundDetailRow.getColumn("flightid").getStringColumn();
									refundDetail.setPaxID(mapping.getAllMap().get(paxid));
									refundDetail.setFlightID(mapping.getAllMap().get(flightid));
									refundDetail.setServiceType(refundDetailRow.getColumn("servicetype").getStringColumn());
									refundDetail.setServiceName(refundDetailRow.getColumn("code").getStringColumn());
									refundDetail.setFeeType(refundDetailRow.getColumn("chargetype").getStringColumn());
									refundDetail.setAmount(refundDetailRow.getColumn("refundfee").getStringColumn());
								}
						}
					}
				}
			}
		}
	}

	
	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * @param ret
	 * @param root
	 * @return
	 */
	private boolean processError(CommandRet ret, OrderViewRS root) {
		// 判断是否存在错误信息
		String errCode = ret.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
	}
	
	private String seatRefundInfo(String strRefund) {
		CommandData dataRefund = new CommandData();
		JsonUnit.fromJson(dataRefund, strRefund);
		dataRefund.addParm("refundedtext", "");
		dataRefund.addParm("changetext", "");
		String str = JsonUnit.toJson(dataRefund);
		if(str == null || "".equals(str)){
			str = strRefund;
		}
		return str;
	}
	
	/**
	 * 根据Java编程规范：由于for循环中不能创建对象，所有将创建对象放到方法里
	 *
	 * @return list
	 */
	public List<String > getList(String args) {
		List<String> list = new ArrayList<String>();
		list.add(args);
		return list;
	}
	
	private static final class Mapping {
		private Map<String, String> allMap = new HashMap<String, String>();
		private Map<String, Row> flightsMap;
		/**
		 * 辅营信息
		 * String 免费服务 id
		 * Row 服务内容
		 */
		private  Map<String,String>  ServiceBundleMap=new HashMap<String, String>();
		
		private  Map<String,Row>  SeatsMap = new HashMap<String, Row>();
		
		private Map<String,String> vatRateMap = new HashMap<String, String>();
		
		
		public Map<String, String> getVatRateMap() {
			return vatRateMap;
		}
		public Map<String, Row> getFlightsMap() {
			return flightsMap;
		}
		public void setFlightsMap(Map<String, Row> flightsMap) {
			this.flightsMap = flightsMap;
		}
		public Map<String,String> getServiceBundleMap() {
			return ServiceBundleMap;
		}
		public Map<String,Row> getSeatsMap() {
			return SeatsMap;
		}
		public Map<String, String> getAllMap() {
			return allMap;
		}
	}
}
