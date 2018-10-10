package com.travelsky.quick.util.helper;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AgencyIDType;
import org.iata.iata.edist.BookingReferenceType;
import org.iata.iata.edist.BookingReferencesDocument.BookingReferences;
import org.iata.iata.edist.CodesetType;
import org.iata.iata.edist.ContactsDocument.Contacts;
import org.iata.iata.edist.ContactsDocument.Contacts.Contact;
import org.iata.iata.edist.DataListType;
import org.iata.iata.edist.DataListType.FlightList;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DataListType.OriginDestinationList;
import org.iata.iata.edist.DataListType.SeatList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.DescriptionType.Text;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightItemType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.FlightType;
import org.iata.iata.edist.FlightType.Flight;
import org.iata.iata.edist.ItemIDType;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfSeatType;
import org.iata.iata.edist.ListOfServiceBundleType;
import org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle;
import org.iata.iata.edist.MsgPartiesType.Sender;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.MsgPartiesType;
import org.iata.iata.edist.OfferItemTypeDocument.OfferItemType;
import org.iata.iata.edist.OrderCreateRQDocument;
import org.iata.iata.edist.OrderCreateRQDocument.OrderCreateRQ.Query;
import org.iata.iata.edist.OrderItemAssociationType;
import org.iata.iata.edist.OrderItemCoreType.OrderItem;
import org.iata.iata.edist.OrderOfferItemType;
import org.iata.iata.edist.OrderViewRSDocument;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Order;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Order.OrderItems;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS.Response.Passengers;
import org.iata.iata.edist.OriginDestinationDocument.OriginDestination;
import org.iata.iata.edist.OtherContactMethodType;
import org.iata.iata.edist.PassengerDocument.Passenger;
import org.iata.iata.edist.PassengerSummaryType.PassengerIDInfo;
import org.iata.iata.edist.PassengerSummaryType.PassengerIDInfo.FOID;
import org.iata.iata.edist.PassengerSummaryType.PassengerIDInfo.PassengerDocument;
import org.iata.iata.edist.SegmentReferencesDocument.SegmentReferences;
import org.iata.iata.edist.SeatLocationType;
import org.iata.iata.edist.ServiceDescriptionType;
import org.iata.iata.edist.ServiceCoreType.Associations;
import org.iata.iata.edist.ServiceDescriptionType.Description;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceIDType;
import org.iata.iata.edist.TravelAgencySenderType;
import org.iata.iata.edist.ServiceListDocument.ServiceList;
import org.iata.iata.edist.ShoppingResponseOrderType.Offers;
import org.iata.iata.edist.ShoppingResponseOrderType.Offers.Offer;
import org.iata.iata.edist.ShoppingResponseOrderType.Offers.Offer.OfferItems.OfferItem;
import org.iata.iata.edist.TravelerCoreType.Age.BirthDate;
import org.iata.iata.edist.TravelerCoreType.PTC;
import org.iata.iata.edist.TravelerSummaryType.Name;
import org.iata.iata.edist.WarningType;
import org.iata.iata.edist.WarningsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.util.DateUtils;

public class APIOrderCreateNDCONEE {

	/**
	 * 
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(APIOrderCreateNDCONEE.class);
	private String language = ApiServletHolder.getApiContext().getLanguage();
	// （男Male/女Female）
	private static final String MALE = "Male";
	/* private static final String FEMALE = "Female"; */
	// 联系人信息
	private Map<String, String> contactsMap = new HashMap<String, String>();
	private static final String LASTNAME = "lastname";
	private static final String FIRSTNAME = "firstname";
	private static final String AREACODE = "areacode";
	private static final String CONTACTPREFIX = "contactprefix";
	private static final String TELEPHONE = "telephone";
	private static final String EMAIL = "email";
	private static final String SEG = "SEG";
	private static final String FLT = "FLT";
	private static final String CONTACT = "CONTACT";
	private String PNR = "";
	private String AIRLINECODE = "";
	// 人航段辅助map
	private Map<String, Row> helpMap = new TreeMap<String, Row>();
	// 航班辅助map
	private Map<String, Row> flightHelpMap = new TreeMap<String, Row>();

	public void doServletONEE(SelvetContext<ApiContext> context) throws Exception {
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean(context);
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_ORDER_CREATE,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		} 
	}

	public void transInputXmlToRequestBean(SelvetContext<ApiContext> context) throws APIException, Exception {
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		OrderCreateRQDocument rootDoc = null;
		rootDoc = OrderCreateRQDocument.Factory.parse(xmlInput);

		OrderCreateRQDocument.OrderCreateRQ reqDoc = rootDoc.getOrderCreateRQ();
		input = APICacheHelper.setDeptInfo(context, reqDoc.getParty(), input);
		// input.addParm("owner", "1E");

		/*
		 * // 部门ID String deptno =
		 * ApiServletHolder.getApiContext().getTicketDeptid();
		 * input.addParm("tktdeptid", deptno); // 会员id input.addParm("memberid",
		 * context.getContext().getUserID());
		 */
		// Query
		Query query = reqDoc.getQuery();
		BookingReferenceType[] bookkingArry = query.getBookingReferences().getBookingReferenceArray();
		if (null != bookkingArry) {
			// 航司二字码
			String pnr = bookkingArry[0].getID();
			input.addParm("pnr", pnr);
			CodesetType type = bookkingArry[0].getType();
			if (type != null) {
				input.addParm("flag", "RLR".equals(type.getCode()) ? "1" : "0");
			} else {
				input.addParm("flag", "0");
			}
		} else {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_PNR, language));
			throw APIException.getInstance(ErrCodeConstants.API_UNKNOW_PNR);
		}

		/**
		 * 旅客信息和联系人信息
		 */
		Passenger[] passenger = query.getPassengers().getPassengerArray();
		Map<String, Row> passengMap = new HashMap<String, Row>();
		Map<String, String> guardianMap = new HashMap<String, String>();
		addPaxsCommand(input, passenger, passengMap, guardianMap);
		addContacts(input);
		/**
		 * 航班信息
		 */
		org.iata.iata.edist.OrderCreateRQDocument.OrderCreateRQ.Query.OrderItems orderItems = query.getOrderItems();
		Offers offers = orderItems.getShoppingResponse().getOffers();
		Offer[] offerArr = offers == null ? null : offers.getOfferArray();
		// (S单程 R往返程)
		if (offerArr == null || offerArr.length < 1) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_OFFERS, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_OFFERS);
		} else {
			input.addParm("routtype", "S");
			addFlightSegment(input, offerArr);
		}

		/**
		 * 航班数据
		 */
		/*
		 * OrderOfferItemType[] offerItemArray = orderItems.getOfferItemArray();
		 * Map<String, Flight> flightMap = new HashMap<String, Flight>(); if
		 * (null != offerItemArray && offerItemArray.length > 0) { for
		 * (OrderOfferItemType orderItem : offerItemArray) { OfferItemType
		 * offerItemType = orderItem.getOfferItemType(); FlightItemType
		 * flightItem = offerItemType.getDetailedFlightItemArray(0); FlightType
		 * flightType = flightItem.getOriginDestinationArray(0); Flight flight =
		 * flightType.getFlightArray(0); // 航班key String flightKey =
		 * flight.getSegmentKey(); flightMap.put(flightKey, flight); } }
		 */
		// TypeA多航段时，存放所有的seg信息
		OrderOfferItemType item = orderItems.getOfferItemArray(0);
		Map<String, Flight> flightMapForINF = new HashMap<String, Flight>();
		if (null != item && !"".equals(item)) {
			FlightType[] originDestinationArray = item.getOfferItemType().getDetailedFlightItemArray(0)
					.getOriginDestinationArray();
			if (null != originDestinationArray && originDestinationArray.length > 0) {
				for (FlightType flightType : originDestinationArray) {
					Flight flightArray = flightType.getFlightArray(0);
					String segmentKey = flightArray.getSegmentKey();
					flightMapForINF.put(segmentKey, flightArray);
				}
			}
		}

		// 当婴儿添加成功时，在orderview中显示状态为OK.
		Table infViewTable = new Table(new String[] { "pid", "fid" });
		Iterator<String> iter = guardianMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			String value = guardianMap.get(key);
			Iterator<String> iterator = flightMapForINF.keySet().iterator();
			while (iterator.hasNext()) {
				Flight flight = flightMapForINF.get(iterator.next());
				String segmentKey = flight.getSegmentKey();
				Row addRow = infViewTable.addRow();
				addRow.addColumn("pid", value);
				addRow.addColumn("fid", segmentKey);
			}
		}
		if (infViewTable.getRowCount() > 0) {
			CommandData data = new CommandData();
			data.addParm("createOk", infViewTable);
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("createOk"), JsonUnit.toJson(data), 60);
		}
		// 辅营信息
		DataListType dataLists = query.getDataLists();
		if (null != dataLists && null != dataLists.getServiceList()) {
			ServiceList auxiliaryList = dataLists.getServiceList();
			if (auxiliaryList.getServiceArray() != null && auxiliaryList.getServiceArray().length > 0
					&& passengMap != null && flightMapForINF != null) {
				ServiceDetailType[] serviceArray = auxiliaryList.getServiceArray();
				addAuxiliary(serviceArray, flightMapForINF, input, passengMap);
			}
		}

		// 选座功能
		if (null != dataLists && dataLists.getSeatList() != null) {
			SeatList seatList = dataLists.getSeatList();
			if (null != seatList.getSeatsArray() && seatList.getSeatsArray().length > 0) {
				Map<String, ListOfSeatType> seatMap = new HashMap<String, ListOfSeatType>();
				if (seatList.getSeatsArray() != null && seatList.getSeatsArray().length > 0 && passengMap != null
						&& flightMapForINF != null) {
					ListOfSeatType[] seatsArray = seatList.getSeatsArray();
					addSeat(seatsArray, seatMap, input, passengMap, flightMapForINF);
				}
			}
		}
	}

	/**
	 * 新增选座功能
	 * 
	 * @param flightMap
	 * @throws APIException
	 */
	private void addSeat(ListOfSeatType[] seatsArray, Map<String, ListOfSeatType> seatMap, CommandData input,
			Map<String, Row> passengMap, Map<String, Flight> flightMapForINF) throws APIException {
		Table seatsTable = new Table(new String[] { "passno", "passtype", "paxname", "carricd", "flightno",
				"flightsuffix", "flightdate", "oricode", "descode", "seatno", "mode", "pid", "fid" });
		for (ListOfSeatType seats : seatsArray) {
			SeatLocationType location = seats.getLocation();
			// 座位后缀
			String column = location.getColumn();
			// 座位号
			String seatno = location.getRow().getNumber().getStringValue();
			// 关联的人航段
			OrderItemAssociationType associations = location.getAssociations();

			// 旅客信息
			String passeArr = associations.getPassengers().getPassengerArray(0).getPassengerAssociation();

			Row row = passengMap.get(passeArr);
			// 旅客姓
			String lastname = row.getColumn("lastname").getStringColumn();
			// 旅客名
			String firstname = row.getColumn("firstname").getStringColumn();
			// 证件号
			String passno = row.getColumn("passno").getStringColumn();
			// 证件类型
			String passtype = row.getColumn("passtype").getStringColumn();

			// 航班 segmentKey
			String segmentKey = associations.getFlight().getSegmentReferencesArray(0).getStringValue();
			// 航班信息
			Flight flight = flightMapForINF.get(segmentKey);
			// 出发地
			Departure departure = flight.getDeparture();
			String oricode = departure.getAirportCode().getStringValue();
			// 出发日期
			Date flightdate = departure.getDate().getTime();
			// 到达地
			FlightArrivalType arrival = flight.getArrival();
			String descode = arrival.getAirportCode().getStringValue();
			MarketingCarrierFlightType marketingCarrier = flight.getMarketingCarrier();
			// 航空二字码
			String carricd = marketingCarrier.getAirlineID().getStringValue();
			FlightNumber flightNumber = marketingCarrier.getFlightNumber();
			String suffix = "";
			if (null != flightNumber.getOperationalSuffix() || !"".equals(flightNumber.getOperationalSuffix())) {
				// 航班后缀
				suffix = flightNumber.getOperationalSuffix();
			}
			// 航班号
			String flightno = flightNumber.getStringValue();

			Row seatRow = seatsTable.addRow();
			seatRow.addColumn("passno", passno);
			seatRow.addColumn("passtype", passtype);
			seatRow.addColumn("paxname", lastname + "/" + firstname);
			seatRow.addColumn("carricd", carricd);
			seatRow.addColumn("flightno", DateUtils.getFlightNo(flightno));
			seatRow.addColumn("flightsuffix", suffix);
			seatRow.addColumn("flightdate", DateUtils.getInstance().formatDate(flightdate, "yyyyMMdd"));
			seatRow.addColumn("oricode", oricode);
			seatRow.addColumn("descode", descode);
			seatRow.addColumn("seatno", seatno + column);
			seatRow.addColumn("mode", "1");
			seatRow.addColumn("fid", segmentKey);
			seatRow.addColumn("pid", passeArr);
		}
		input.addParm("seats", seatsTable);
	}

	// 新增辅营信息
	public void addAuxiliary(ServiceDetailType[] serviceArray, Map<String, Flight> flightMapForINF, CommandData input,
			Map<String, Row> passengMap) {

		Table AuxiliaryTable = new Table(
				new String[] { "mode", "buynum", "passno", "passtype", "paxname", "submarketcode", "carricd",
						"flightno", "flightsuffix", "flightdate", "oricode", "descode", "pid", "fid" });
		Map<String, String> subMap = new HashMap<String, String>();
		// 辅营详情
		for (ServiceDetailType auxiliary : serviceArray) {
			String submarketcode = auxiliary.getServiceID().getStringValue().toUpperCase();
			/**
			 * 辅营描述 start
			 */
			if (!subMap.containsKey(submarketcode)) {
				String description = "";
				String name = "";
				ServiceDescriptionType descriptions = auxiliary.getDescriptions();
				if (descriptions != null && !"".equals(descriptions)) {
					Description destion = descriptions.getDescriptionArray()[0];
					if (destion != null && !"".equals(destion)) {
						Text text = destion.getText();
						if (text != null && !"".equals(text)) {
							description = text.stringValue();
						}
					}
				}
				// name
				org.iata.iata.edist.ServiceCoreType.Name n = auxiliary.getName();
				if (n != null && !"".equals(n)) {
					name = n.getStringValue();
				}
				if (!"".equals(description) || !"".equals(name)) {
					CommandData date = new CommandData();
					date.addParm("name", name);
					date.addParm("description", description);
					RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey(submarketcode), JsonUnit.toJson(date), 60);
					subMap.put(submarketcode, submarketcode);
				}
			}
			/**
			 * 辅营描述 end
			 */
			Associations[] associationsArray = auxiliary.getAssociationsArray();
			for (Associations associations : associationsArray) {
				// 关联的旅客信息
				Object buynum = associations.getTraveler().getTravelerReferences().get(0);
				Row paxRow = passengMap.get(buynum); // P1旅客的证件信息
				String lastName = paxRow.getColumn("lastname").getStringColumn();
				String firstName = paxRow.getColumn("firstname").getStringColumn();
				String passtype = paxRow.getColumn("passtype").getStringColumn();
				String passno = paxRow.getColumn("passno").getStringColumn();
				// 兼容1e中 辅营中seg信息为空时 预定全航段
				SegmentReferences[] segmentReferencesArray = associations.getFlight().getSegmentReferencesArray();
				if (segmentReferencesArray.length <= 0) {
					for (Flight segmentType : flightMapForINF.values()) {
						Departure departure = segmentType.getDeparture();
						// 出发地
						String flightoricode = departure.getAirportCode().getStringValue();
						// 出发时间
						Date oridateTime = departure.getDate().getTime();
						FlightArrivalType arrival = segmentType.getArrival();
						// 到达地
						String destcode = arrival.getAirportCode().getStringValue();
						MarketingCarrierFlightType marketingCarrier = segmentType.getMarketingCarrier();
						// 航班号
						String flghtNo = marketingCarrier.getFlightNumber().getStringValue();
						// 航班后缀
						String suffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
						// 航司二字码
						String carricd = marketingCarrier.getAirlineID().getStringValue();
						String segmentKey = segmentType.getSegmentKey();
						Row auxilRow = AuxiliaryTable.addRow();
						auxilRow.addColumn("mode", "1");
						auxilRow.addColumn("buynum", 1);
						auxilRow.addColumn("submarketcode", submarketcode);
						auxilRow.addColumn("passno", passno);
						auxilRow.addColumn("passtype", passtype);
						auxilRow.addColumn("paxname", lastName + "/" + firstName);
						auxilRow.addColumn("carricd", carricd);
						auxilRow.addColumn("flightno", DateUtils.getFlightNo(flghtNo));
						auxilRow.addColumn("flightsuffix", suffix);
						auxilRow.addColumn("flightdate", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
						auxilRow.addColumn("oricode", flightoricode);
						auxilRow.addColumn("descode", destcode);
						auxilRow.addColumn("pid", buynum.toString());
						auxilRow.addColumn("fid", segmentKey);
					}
				} else {
					String segmentKey = associations.getFlight().getSegmentReferencesArray(0).getStringValue();
					Flight flightSegment = flightMapForINF.get(segmentKey);
					Departure departure = flightSegment.getDeparture();
					// 出发地
					String flightoricode = departure.getAirportCode().getStringValue();
					// 出发时间
					Date oridateTime = departure.getDate().getTime();
					FlightArrivalType arrival = flightSegment.getArrival();
					// 到达地
					String destcode = arrival.getAirportCode().getStringValue();
					MarketingCarrierFlightType marketingCarrier = flightSegment.getMarketingCarrier();
					// 航班号
					String flghtNo = marketingCarrier.getFlightNumber().getStringValue();
					// 航班后缀
					String suffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
					// 航司二字码
					String carricd = marketingCarrier.getAirlineID().getStringValue();
					Row auxilRow = AuxiliaryTable.addRow();
					auxilRow.addColumn("mode", "1");
					auxilRow.addColumn("buynum", 1);
					auxilRow.addColumn("submarketcode", submarketcode);
					auxilRow.addColumn("passno", passno);
					auxilRow.addColumn("passtype", passtype);
					auxilRow.addColumn("paxname", lastName + "/" + firstName);
					auxilRow.addColumn("carricd", carricd);
					auxilRow.addColumn("flightno", DateUtils.getFlightNo(flghtNo));
					auxilRow.addColumn("flightsuffix", suffix);
					auxilRow.addColumn("flightdate", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
					auxilRow.addColumn("oricode", flightoricode);
					auxilRow.addColumn("descode", destcode);
					auxilRow.addColumn("pid", buynum.toString());
					auxilRow.addColumn("fid", segmentKey);
				}
			}
		}
		input.addParm("spaxsubmarkets", AuxiliaryTable);
	}

	/**
	 * 添加旅客信息到CommandData
	 * 
	 * @param input
	 * @param passengMap
	 * @param guardianMap
	 * @param passenger
	 * @throws APIException
	 */
	@SuppressWarnings("deprecation")
	private void addPaxsCommand(CommandData input, Passenger[] passengerArr, Map<String, Row> passengMap,
			Map<String, String> guardianMap) throws APIException {
		if (passengerArr == null || passengerArr.length < 1) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PAXS, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PAXS);
		}

		Table paxsTable = new Table(new String[] { "id", "firstname", "lastname", "paxtype", "birth", "paxsex",
				"telephone", "areacode", "contactprefix", "email", "guardian", "passtype", "passno", "docexpiry",
				"issuecountry", "birthcountry" });
		for (Passenger passenger : passengerArr) {
			Row paxsRow = paxsTable.addRow();
			// 旅客ID
			String objectKey = "";
			if (null != passenger.getObjectKey()) {
				objectKey = passenger.getObjectKey();
				paxsRow.addColumn("id", objectKey);
			}
			// 旅客类型
			String paxtype = "";
			if (null != passenger.getPTC() && passenger.getPTC().getStringValue() != null) {
				paxtype = passenger.getPTC().getStringValue();
				paxsRow.addColumn("paxtype", paxtype);
				String guardian = passenger.getPassengerAssociation();
				if (StringUtils.hasLength(guardian)) {
					guardianMap.put(guardian, objectKey);
				}
				// 如果是婴儿返回查找陪护人
				if ("INF".equals(paxtype)) {
					guardian = guardianMap.get(objectKey);
					if (!StringUtils.hasLength(guardian)) {
						LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PAXS_GUARDIAN, language));
						throw APIException.getInstance(ErrCodeConstants.API_NULL_PAXS_GUARDIAN);
					}
				}
				paxsRow.addColumn("guardian", guardian);
			}
			if (passenger.getAge() != null && passenger.getAge().getBirthDate() != null
					&& passenger.getAge().getBirthDate().getDateValue() != null) {
				// 旅客生日
				BirthDate birthDate = passenger.getAge().getBirthDate();
				Date birth;
				birth = birthDate.getDateValue();
				try {
					paxsRow.addColumn("birth", DateUtils.getInstance().formatDate(birth, "yyyyMMdd"));
				} catch (Exception e) {
					LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_BIRTHDAY, language), e);
					throw APIException.getInstance(ErrCodeConstants.API_CONVERT_BIRTHDAY, e);
				}
			}
			Name nameArr = passenger.getName();
			// 旅客姓
			String lastName = nameArr.getSurname().getStringValue();
			if (!StringUtils.hasLength(lastName)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_SURNAME, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_SURNAME);
			}
			paxsRow.addColumn("lastname", lastName);
			// 旅客名
			String firstname = nameArr.getGivenArray(0).getStringValue();
			if (!StringUtils.hasLength(firstname)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_GIVENNAME, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_GIVENNAME);
			}
			paxsRow.addColumn("firstname", firstname);
			if (passenger.getContacts() != null && passenger.getContacts().getContactArray() != null
					&& passenger.getContacts().getContactArray().length > 0) {
				Contact[] contactArr = passenger.getContacts().getContactArray();
				// 旅客联系信息
				for (Contact contact : contactArr) {
					String type = contact.getContactType().getStringValue();
					if (CONTACT.equalsIgnoreCase(type)) {
						if (contact.getEmailContact() != null && contact.getEmailContact().getAddress() != null
								&& contact.getEmailContact().getAddress().getStringValue() != null) {
							// 联系email
							String email1 = contact.getEmailContact().getAddress().getStringValue();
							contactsMap.put(EMAIL, email1);
						}
						if (contact.getPhoneContact() != null && contact.getPhoneContact().getNumberArray(0) != null
								&& contact.getPhoneContact().getNumberArray(0).getStringValue() != null) {
							// 联系电话
							String telephone1 = contact.getPhoneContact().getNumberArray(0).getStringValue();
							contactsMap.put(TELEPHONE, telephone1);
						}
						if (contact.getPhoneContact() != null && contact.getPhoneContact().getNumberArray(0) != null
								&& contact.getPhoneContact().getNumberArray(0).getAreaCode() != null) {
							// 联系电话
							String areacode = contact.getPhoneContact().getNumberArray(0).getAreaCode();
							contactsMap.put(CONTACTPREFIX, areacode);
						}
						if (contact.getOtherContactMethod() != null
								&& contact.getOtherContactMethod().getName() != null) {
							// 联系人姓名
							String contacName = contact.getOtherContactMethod().getName();
							contactsMap.put(LASTNAME, contacName);
						}
						if (contact.getOtherContactMethod() != null
								&& contact.getOtherContactMethod().getValue() != null) {
							// 联系人姓名
							String contacValue = contact.getOtherContactMethod().getValue();
							contactsMap.put(FIRSTNAME, contacValue);
						}
					} else {
						if (contact.getEmailContact() != null && contact.getEmailContact().getAddress() != null
								&& contact.getEmailContact().getAddress().getStringValue() != null) {
							// 联系email
							String email1 = contact.getEmailContact().getAddress().getStringValue();
							paxsRow.addColumn("email", email1);
						}
						if (contact.getPhoneContact() != null && contact.getPhoneContact().getNumberArray(0) != null
								&& contact.getPhoneContact().getNumberArray(0).getStringValue() != null) {
							// 联系电话
							String telephone1 = contact.getPhoneContact().getNumberArray(0).getStringValue();
							paxsRow.addColumn("telephone", telephone1);
						}
						if (contact.getPhoneContact() != null && contact.getPhoneContact().getNumberArray(0) != null
								&& contact.getPhoneContact().getNumberArray(0).getAreaCode() != null) {
							// 电话区号
							String areacode1 = contact.getPhoneContact().getNumberArray(0).getAreaCode();
							paxsRow.addColumn("contactprefix", areacode1);
						}
					}
				}
			}
			PassengerIDInfo passengerIDInfo = passenger.getPassengerIDInfo();
			if (passengerIDInfo != null && passengerIDInfo.getFOID() != null) {
				// 证件信息
				FOID foidArr = passenger.getPassengerIDInfo().getFOID();
				String passtype = foidArr.getType();
				String passno = foidArr.getID().getStringValue();
				if (StringUtils.hasLength(passno)) {
					paxsRow.addColumn("passtype", passtype);
					paxsRow.addColumn("passno", passno);
				}
			}
			if (passengerIDInfo != null && passengerIDInfo.getPassengerDocumentArray().length > 0) {
				PassengerDocument documentArray = passengerIDInfo.getPassengerDocumentArray(0);
				String birthCountry = documentArray.getBirthCountry();
				String paxid = documentArray.getID();
				String type = documentArray.getType();
				Calendar dateOfExpiration = documentArray.getDateOfExpiration();
				String date = "";
				if (dateOfExpiration != null) {
					date = DateUtils.getInstance().formatDate(dateOfExpiration, "yyyyMMdd");
				}
				String countryOfIssuance = documentArray.getCountryOfIssuance();
				paxsRow.addColumn("passtype", type);
				paxsRow.addColumn("passno", paxid);
				paxsRow.addColumn("birthcountry", birthCountry);
				paxsRow.addColumn("docexpiry", date);
				paxsRow.addColumn("issuecountry", countryOfIssuance);
			}
			// 性别（男Male/女Female）
			if (null != passenger.getGender()) {
				String paxsex = MALE.equals(passenger.getGender().getStringValue()) ? "M" : "F";
				paxsRow.addColumn("paxsex", paxsex);
			}
			passengMap.put(objectKey, paxsRow);
		}
		input.addParm("paxs", paxsTable);
	}

	/**
	 * 添加联系人信息到CommandData
	 * 
	 * @throws APIException
	 */
	private void addContacts(CommandData input) throws APIException {
		if (!contactsMap.isEmpty() && contactsMap.size() > 0) {

			Table contactsTable = new Table(
					new String[] { LASTNAME, FIRSTNAME, AREACODE, CONTACTPREFIX, TELEPHONE, EMAIL });
			Row contactsRow = contactsTable.addRow();
			// 联系人姓
			String contacName = contactsMap.get(LASTNAME);
			if (!StringUtils.hasLength(contacName)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CONTACT_NAME, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_NAME);
			}
			contactsRow.addColumn(LASTNAME, contacName);
			// 联系人电话
			String contacTelephone = contactsMap.get(TELEPHONE);
			if (!StringUtils.hasLength(contacTelephone)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CONTACT_NO, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_CONTACT_NO);
			}
			contactsRow.addColumn(TELEPHONE, contacTelephone);
			// 联系人邮箱
			String contacEmail = contactsMap.get(EMAIL);
			contactsRow.addColumn(EMAIL, contacEmail);
			String firstname = contactsMap.get(FIRSTNAME);
			contactsRow.addColumn(FIRSTNAME, firstname);
			String areacode = contactsMap.get(AREACODE);
			contactsRow.addColumn(AREACODE, areacode);
			String contactprefix = contactsMap.get(CONTACTPREFIX);
			contactsRow.addColumn(CONTACTPREFIX, contactprefix);
			input.addParm("contacts", contactsTable);
		}
	}

	private void addFlightSegment(CommandData input, Offer[] offerArr) throws APIException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// 出发地机场三字码
		String oricode = "";
		// 目的地机场三字码
		String destcode = "";
		// 出发日期 必填(格式yyyyMMdd)
		String oridate = "";
		// 返程日期 (格式yyyyMMdd)
		String destdate = "";
		Table flightsTable = new Table(new String[] { "airlinecd", "flightno", "oridate", "oriTime", "destTime",
				"routtype", "familycode", "cabin", "guanranteeno", "oricode", "flightDay", "oriDateTime" });
		// Redis缓存中航班数据关联key值，用于创建订单成功后，删除缓存中航班数据
		String delRedis = "";
		// 遍历航段
		for (Offer offer : offerArr) {
			Row row = flightsTable.addRow();
			ItemIDType offerID = offer.getOfferID();
			if (null == offerID) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_OFFERS, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_OFFERS);
			}
			String ids = offerID.getStringValue();
			String shoppingID = ids.substring(0, 52);
			delRedis = delRedis + "," + RedisNamespaceEnum.api_cache_order.toKey(shoppingID);
			String OfferID = ids.substring(52);
			String OfferItemID = "";
			OfferItem[] offerItemArr = offer.getOfferItems().getOfferItemArray();
			if (offerItemArr == null || offerItemArr.length < 1) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_OFFERS, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_OFFERS);
			}
			OfferItem offerItem = offerItemArr[0];
			OfferItemID = offerItem.getOfferItemID().getStringValue();
			// InvGuaranteeID
			String guaranteeID = offerItem.getDetails().getInventoryGuarantee().getInvGuaranteeID();
			row.addColumn("guanranteeno", guaranteeID);
			input.addParm("guanranteeno", guaranteeID);
			CommandRet flightsRet = new CommandRet("");
			// redis获取缓存的shopping数据
			String json = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey(shoppingID));
			if ("".equals(json) || json == null) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_ORDER_SUBMIT_TIMEOUT, language));
				throw APIException.getInstance(ErrCodeConstants.API_ORDER_SUBMIT_TIMEOUT);
			} else {
				JsonUnit.fromJson(flightsRet, json);
				// LOGGER.error("SHOPPING"+json);
			}
			CommandData flightRet = flightsRet.getParm(ids).getObjectColumn();
			if ("".equals(flightRet) || flightRet == null) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_ORDER_SUBMIT_TIMEOUT, language));
				throw APIException.getInstance(ErrCodeConstants.API_ORDER_SUBMIT_TIMEOUT);
			}
			CommandData flights = flightRet.getParm(OfferID + "." + OfferItemID).getObjectColumn();
			if ("".equals(flights) || flights == null) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_ORDER_SUBMIT_TIMEOUT, language));
				throw APIException.getInstance(ErrCodeConstants.API_ORDER_SUBMIT_TIMEOUT);
			} else {
				String airlinecd = flights.getParm("airlinecd").getStringColumn();
				String flightno = flights.getParm("flightno").getStringColumn();
				String suffix = flights.getParm("suffix").getStringColumn();
				String routtype = flights.getParm("routtype").getStringColumn();
				String familycode = flights.getParm("id").getStringColumn();
				String cabin = flights.getParm("cabin").getStringColumn();
				String isoCode = flights.getParm("isoCode").getStringColumn();
				String flightDay = flights.getParm("flightDay").getStringColumn();
				String oriTime = flights.getParm("oriTime").getStringColumn();
				// String destTime =
				// flights.getParm("destTime").getStringColumn();
				// 币种
				if ("".equals(isoCode) || null == isoCode) {
					isoCode = "USD";
				}
				input.addParm("isoCode", isoCode);
				if ("G".equals(routtype)) {
					oricode = flights.getParm("oricode").getStringColumn();
					destcode = flights.getParm("destcode").getStringColumn();
					Date oridateG = flights.getParm("oridate").getDateColumn();
					oridate = formatter.format(oridateG);
					row.addColumn("oricode", oricode);
				}
				// 如果是回程 把出发日期放到回程日期上
				if ("R".equals(routtype)) {
					Date destdateR = flights.getParm("oridate").getDateColumn();
					destdate = formatter.format(destdateR);
					row.addColumn("oricode", destcode);
				}

				row.addColumn("oridate", formatter.format(flights.getParm("oridate").getDateColumn()));
				row.addColumn("oriDateTime", formatter.format(flights.getParm("oridate").getDateColumn()));
				row.addColumn("oriTime", oriTime);
				// row.addColumn("destTime", destTime);
				row.addColumn("airlinecd", airlinecd);
				row.addColumn("flightno", DateUtils.getFlightNo(flightno) + suffix);
				row.addColumn("routtype", routtype);
				row.addColumn("familycode", familycode);
				row.addColumn("cabin", cabin);
				row.addColumn("flightDay", flightDay);
			}
		}
		if (delRedis.startsWith(",")) {
			delRedis = delRedis.substring(1, delRedis.length());
		}
		input.addParm("delRedis", delRedis);
		input.addParm("oricode", oricode);
		input.addParm("destcode", destcode);
		input.addParm("oridate", oridate);
		input.addParm("destdate", destdate);
		input.addParm("flights", flightsTable);
	}

	/***********************************************************************************************/
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		OrderViewRSDocument doc = OrderViewRSDocument.Factory.newInstance();
		OrderViewRSDocument.OrderViewRS root = doc.addNewOrderViewRS();
		Mapping mapping = new Mapping();
		try {
			String errorcode = commandRet.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = root.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode, ApiServletHolder.getApiContext().getLanguage()));
			} else {
				// 当把唯一航段被删除时，此PNR为关闭状态，只需返回成功标识
				if ("CLOSED".equals(commandRet.getParm("status").getStringColumn())) {
					root.addNewSuccess();
				} else {
					String owner = commandRet.getParm("owner").getStringColumn();
					root.addNewParty().addNewSender().addNewTravelAgencySender().addNewAgencyID().setOwner(owner);
					root.addNewSuccess();
					Response responseArry = root.addNewResponse();
					/**
					 * 针对错误信息的集中返回
					 */
					Table errorTable = commandRet.getParm("errorTable").getTableColumn();
					if (errorTable.getRowCount() > 0) {
						addWarningS(errorTable, root);
					}

					/* 旅客信息 *****************************************/
					Passengers passengersArry = responseArry.addNewPassengers();
					addPaxsXML(commandRet, passengersArry, mapping);
					// 订单号
					/*
					 * String orderNo =
					 * commandRet.getParm("orderid").getStringColumn();
					 */
					// OrderViewRS->Response->Order
					Order order = responseArry.addNewOrder();
					/*
					 * OrderIDType orderID = order.addNewOrderID();
					 * orderID.setStringValue(orderNo);
					 */
					// 支付时间(秒)
					Table paysTabe = commandRet.getParm("pays").getTableColumn();
					String maxpay = paysTabe.getRow(0).getColumn("maxpaysecond").getStringColumn();
					int hour = (Integer.valueOf(maxpay) / (60 * 60));
					int min = ((Integer.valueOf(maxpay) / (60)) - hour * 60);
					int s = (Integer.valueOf(maxpay) - hour * 60 * 60 - min * 60);
					maxpay = hour + "H" + min + "M" + s + "S";
					order.addNewTimeLimits().addNewPaymentTimeLimit().setRefs(getList(maxpay));
					order.addNewStatus().addNewStatusCode().setCode("OK");
					// 人航段信息
					OrderItems OrderItemsArry = order.addNewOrderItems();
					DataListType dataListArry = responseArry.addNewDataLists();
					FlightSegmentList flightArry = dataListArry.addNewFlightSegmentList();
					// 去程回程 关联航班信息
					FlightList flightListArry = dataListArry.addNewFlightList();
					// 航班查询的出发地
					OriginDestinationList originArry = dataListArry.addNewOriginDestinationList();
					/*
					 * //辅营与品牌关联 ListOfServiceBundleType listOfServiceBundleArry
					 * = dataListArry.addNewServiceBundleList();
					 */
					// 辅营信息
					ServiceList serviceListA = dataListArry.addNewServiceList();
					SeatList seatListArry = dataListArry.addNewSeatList();
					// 航班信息
					addSeats(commandRet, mapping);
					Map<String, Row> flightsMap = new HashMap<String, Row>();
					addFlightList(mapping, flightArry, commandRet, flightsMap);
					Map<String, List<Row>> submarketMap = new HashMap<String, List<Row>>();
					addSeatList(commandRet, seatListArry, mapping);
					addPaxflight(commandRet, OrderItemsArry, mapping, submarketMap);
					addServiceList(commandRet, serviceListA, submarketMap, mapping);
					/*
					 * addServiceBundleListArry(mapping,listOfServiceBundleArry)
					 * ;
					 */
					addOriginArryArry(commandRet, flightListArry, originArry, mapping);
					BookingReferences brtArry = order.addNewBookingReferences();
					BookingReferenceType brtair = brtArry.addNewBookingReference();
					brtair.setID(commandRet.getParm("dcspnr").getStringColumn());
					brtair.addNewAirlineID().setStringValue(AIRLINECODE);
					BookingReferenceType brt1e = brtArry.addNewBookingReference();
					brt1e.setID(input.getParm("pnr").getStringColumn());
					brt1e.addNewOtherID().setStringValue("1E");
				}
			}
		} catch (Exception e) {
			LOGGER.error(ErrCodeConstants.API_NULL_OFFERS, e);
			doc = OrderViewRSDocument.Factory.newInstance();
			root = doc.addNewOrderViewRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM, TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
			processError(commandRet, root);
		}
		return doc;
	}

	/**
	 * 
	 * @param errorTable
	 *            详情结果
	 * @param root
	 *            返回节点
	 */
	private void addWarningS(Table errorTable, OrderViewRS root) {
		// 错误信息
		WarningsType addNewWarnings = root.addNewWarnings();
		for (Row row : errorTable) {
			String errorcode = row.getColumn("errorcode").getStringColumn();
			String errordesc = row.getColumn("errordesc").getStringColumn();
			WarningType addNewWarning = addNewWarnings.addNewWarning();
			addNewWarning.setCode(TipMessager.getErrorCode(errorcode));
			addNewWarning
					.setShortText(TipMessager.getMessage(errorcode, ApiServletHolder.getApiContext().getLanguage()));
		}
	}

	/**
	 * 添加旅客信息到RS-XML
	 * 
	 * @param commandRet
	 * @param passengersArry
	 * @throws APIException
	 */
	@SuppressWarnings("unused")
	private void addPaxsXML(CommandRet commandRet, Passengers passengersArry, Mapping mapping) throws APIException {
		// 乘机人
		Table paxsTable = commandRet.getParm("paxs").getTableColumn();
		if (paxsTable != null && paxsTable.getRowCount() > 0) {
			Map<String, String> paxsMap = new HashMap<String, String>();
			int paxNum = 1;
			int x = 1;
			for (Row paxs : paxsTable) {
				// 旅客id
				String paxid = paxs.getColumn("id").getStringColumn();
				// 旅客类型
				String paxtype = paxs.getColumn("paxtype").getStringColumn();
				Passenger passengerArry = null;
				if (null != paxtype && !"".equals(paxtype)) {
					// 陪护人
					String guardian = paxs.getColumn("guardian").getStringColumn();
					passengerArry = passengersArry.addNewPassenger();
					passengerArry.setObjectKey("P" + paxNum);
					mapping.getAllMap().put(paxid, "P" + paxNum);
					paxs.addColumn(paxid, "P" + paxNum);
					paxsMap.put(paxid, "P" + paxNum);
					PTC ptcArry = passengerArry.addNewPTC();
					ptcArry.setStringValue(paxtype);
					// 反向寻找婴儿的陪护人
					if (!"".equals(guardian) && null != guardian) {
						Passenger[] passenger = passengersArry.getPassengerArray();
						guardian = paxsMap.get(guardian);
						for (Passenger pass : passenger) {
							String guardians = pass.getObjectKey();
							if (null != guardian && guardian.equals(guardians)) {
								pass.setPassengerAssociation("P" + paxNum);
								break;
							}
						}
					}
				}
				// change增加婴儿失败时，应与监护人关联
				String addInf = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("boolean"));
				if ("false".equals(addInf)) {
					String infRedis = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("infRedis"));
					if (infRedis != null && !"".equals(infRedis)) {
						CommandData da = new CommandData();
						JsonUnit.fromJson(da, infRedis);
						Table tableColumn = da.getParm("infTable").getTableColumn();
						String guardian = tableColumn.getRow(0).getColumn("guardian").getStringColumn();
						String num = guardian.substring(guardian.length() - 1);
						guardian = "P" + num;
						String adtGuardian = "P" + paxNum;
						if (adtGuardian.equals(guardian)) {
							passengerArry.setPassengerAssociation("P" + (paxsTable.getRowCount() + x));
							x++;
						}
					}
				}
				// 出生日期
				Date birthDate = paxs.getColumn("birth").getDateColumn();
				if (null != birthDate && !"".equals(birthDate)) {
					passengerArry.addNewAge().addNewBirthDate().setDateValue(birthDate);
				}
				// 旅客姓名
				Name nameArry = passengerArry.addNewName();
				String lastname = paxs.getColumn("lastname").getStringColumn();
				String firstname = paxs.getColumn("firstname").getStringColumn();
				String telephone = paxs.getColumn("telephone").getStringColumn();
				String contactprefix = paxs.getColumn("contactprefix").getStringColumn();
				String email = paxs.getColumn("email").getStringColumn();
				nameArry.addNewSurname().setStringValue(lastname);
				nameArry.addNewGiven().setStringValue(firstname);
				boolean flag = false;
				Contacts contactsArry = null;
				Contact contactArry = null;
				if (StringUtils.hasLength(email) || StringUtils.hasLength(telephone)) {
					flag = true;
					contactsArry = passengerArry.addNewContacts();
					contactArry = contactsArry.addNewContact();
					contactArry.addNewContactType().setStringValue("PASSENGER");
					if (StringUtils.hasLength(email)) {
						contactArry.addNewEmailContact().addNewAddress().setStringValue(email);
					}
					if (StringUtils.hasLength(telephone)) {
						org.iata.iata.edist.PhoneType.Number numberArry1 = contactArry.addNewPhoneContact()
								.addNewNumber();
						numberArry1.setAreaCode(contactprefix);
						numberArry1.setStringValue(telephone);
					}
				}
				// 联系人信息
				Table contactsTable = commandRet.getParm("contacts").getTableColumn();
				if (contactsTable != null && contactsTable.getRowCount() > 0) {
					if (!flag) {
						contactsArry = passengerArry.addNewContacts();
					}
					// 姓
					String contactLastname = contactsTable.getRow(0).getColumn("name").getStringColumn();
					// 名
					String contactFirstname = contactsTable.getRow(0).getColumn("firstname").getStringColumn();
					// 邮箱地址
					String contactEmail = contactsTable.getRow(0).getColumn("email").getStringColumn();
					// 联系电话
					String contactTelephone = contactsTable.getRow(0).getColumn("telephone").getStringColumn();
					String contactprefix2 = contactsTable.getRow(0).getColumn("contactprefix").getStringColumn();
					Contact contactArry2 = contactsArry.addNewContact();
					if (StringUtils.hasLength(contactTelephone) || StringUtils.hasLength(contactEmail)
							|| StringUtils.hasLength(contactLastname) || StringUtils.hasLength(contactFirstname)) {
						contactArry2.addNewContactType().setStringValue("CONTACT");
						if (StringUtils.hasLength(contactTelephone)) {
							org.iata.iata.edist.PhoneType.Number numberArry = contactArry2.addNewPhoneContact().addNewNumber();
						if (StringUtils.hasLength(contactTelephone)) {
							numberArry.setAreaCode(contactprefix2);
							numberArry.setStringValue(contactTelephone);
						}
						if (StringUtils.hasLength(contactEmail)) {
							contactArry2.addNewEmailContact().addNewAddress().setStringValue(contactEmail);
						}
						OtherContactMethodType ocmArry = contactArry2.addNewOtherContactMethod();
						if (StringUtils.hasLength(contactLastname)) {
							ocmArry.setName(contactLastname);
						}
						if (StringUtils.hasLength(contactFirstname)) {
							ocmArry.setValue(contactFirstname);
						}
					}
					}
				}
				/********************************************************************/
				// 旅客性别 Male/Female
				String paxsex = paxs.getColumn("paxsex").getStringColumn();
				if (null != paxsex && !"".equals(paxsex)) {
					paxsex = "M".equals(paxsex) ? "Male" : "Female";
					passengerArry.addNewGender().setStringValue(paxsex);
				}
				// 旅客证件信息
				String passtype = paxs.getColumn("passtype").getStringColumn();
				String passno = paxs.getColumn("passno").getStringColumn();
				String docexpiry = paxs.getColumn("docexpiry").getStringColumn();
				String issuecountry = paxs.getColumn("issuecountry").getStringColumn();
				String BirthCountry = paxs.getColumn("birthcountry").getStringColumn();
				if (StringUtils.hasLength(BirthCountry) || StringUtils.hasLength(docexpiry)
						|| StringUtils.hasLength(issuecountry)) {
					PassengerDocument document = passengerArry.addNewPassengerIDInfo().addNewPassengerDocument();
					if (StringUtils.hasLength(passtype) && StringUtils.hasLength(passno)) {
						document.setID(passno);
						document.setType(passtype);
					}
					if (StringUtils.hasLength(docexpiry)) {
						Calendar date = null;
						try {
							date = DateUtils.getInstance().parseDate(docexpiry, "yyyy-MM-dd");
						} catch (ParseException e) {
							LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_NUM,
									ApiServletHolder.getApiContext().getLanguage()), e);
						}
						document.setDateOfExpiration(date);
					}
					if (StringUtils.hasLength(issuecountry)) {
						document.setCountryOfIssuance(issuecountry);
					}
					if (StringUtils.hasLength(BirthCountry)) {
						document.setBirthCountry(BirthCountry);
					}
				} else {
					if (StringUtils.hasLength(passtype) && StringUtils.hasLength(passno)) {
						FOID fpidArry = passengerArry.addNewPassengerIDInfo().addNewFOID();
						fpidArry.setType(passtype);
						fpidArry.addNewID().setStringValue(passno);
					}
				}
				paxNum++;
			}
			// 先判断辅营中是否添加成功，如果成功则不进行添加，失败再进行添加
			String addInf = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("boolean"));
			/*
			 * CommandData date=new CommandData(); JsonUnit.fromJson(date,
			 * addInf); String tabTable =
			 * date.getParm("boolean").getStringColumn();
			 */
			if ("false".equals(addInf)) {
				String infRedis = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("infRedis"));
				if (infRedis != null && !"".equals(infRedis)) {
					CommandData da = new CommandData();
					JsonUnit.fromJson(da, infRedis);
					Table tableColumn = da.getParm("infTable").getTableColumn();
					for (Row row : tableColumn) {
						// String paxid = row.getColumn("id").getStringColumn();
						// 旅客类型
						Passenger passenger = passengersArry.addNewPassenger();
						passenger.setObjectKey("P" + paxNum);
						PTC ptcArry = passenger.addNewPTC();
						ptcArry.setStringValue("INF");
						// 出生日期
						Date birthDate = row.getColumn("birth").getDateColumn();
						if (null != birthDate && !"".equals(birthDate)) {
							passenger.addNewAge().addNewBirthDate().setDateValue(birthDate);
						}
						// 旅客姓名
						Name nameArry = passenger.addNewName();
						String lastname = row.getColumn("lastname").getStringColumn();
						String firstname = row.getColumn("firstname").getStringColumn();
						String telephone = row.getColumn("telephone").getStringColumn();
						String contactprefix = row.getColumn("contactprefix").getStringColumn();
						String email = row.getColumn("email").getStringColumn();
						nameArry.addNewSurname().setStringValue(lastname);
						nameArry.addNewGiven().setStringValue(firstname);
						boolean flag = false;
						Contacts contactsArry = null;
						Contact contactArry = null;
						if (StringUtils.hasLength(email) || StringUtils.hasLength(telephone)) {
							flag = true;
							contactsArry = passenger.addNewContacts();
							contactArry = contactsArry.addNewContact();
							contactArry.addNewContactType().setStringValue("PASSENGER");
							if (StringUtils.hasLength(email)) {
								contactArry.addNewEmailContact().addNewAddress().setStringValue(email);
							}
							if (StringUtils.hasLength(telephone)) {
								org.iata.iata.edist.PhoneType.Number numberArry1 = contactArry.addNewPhoneContact()
										.addNewNumber();
								numberArry1.setAreaCode(contactprefix);
								numberArry1.setStringValue(telephone);
							}
						}
						// 旅客性别 Male/Female
						String paxsex = row.getColumn("paxsex").getStringColumn();
						if (null != paxsex && !"".equals(paxsex)) {
							passenger.addNewGender().setStringValue(paxsex);
						}
						// 旅客证件信息
						String passtype = row.getColumn("passtype").getStringColumn();
						String passno = row.getColumn("passno").getStringColumn();
						String docexpiry = row.getColumn("docexpiry").getStringColumn();
						String issuecountry = row.getColumn("issuecountry").getStringColumn();
						String BirthCountry = row.getColumn("birthcountry").getStringColumn();
						if (StringUtils.hasLength(BirthCountry) || StringUtils.hasLength(docexpiry)
								|| StringUtils.hasLength(issuecountry)) {
							PassengerDocument document = passenger.addNewPassengerIDInfo().addNewPassengerDocument();
							if (StringUtils.hasLength(passtype) && StringUtils.hasLength(passno)) {
								document.setID(passno);
								document.setType(passtype);
							}
							if (StringUtils.hasLength(docexpiry)) {
								Calendar date = null;
								try {
									date = DateUtils.getInstance().parseDate(docexpiry, "yyyy-MM-dd");
								} catch (ParseException e) {
									LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_NUM,
											ApiServletHolder.getApiContext().getLanguage()), e);
								}
								document.setDateOfExpiration(date);
							}
							if (StringUtils.hasLength(issuecountry)) {
								document.setCountryOfIssuance(issuecountry);
							}
							if (StringUtils.hasLength(BirthCountry)) {
								document.setBirthCountry(BirthCountry);
							}
						} else {
							if (StringUtils.hasLength(passtype) && StringUtils.hasLength(passno)) {
								FOID fpidArry = passenger.addNewPassengerIDInfo().addNewFOID();
								fpidArry.setType(passtype);
								fpidArry.addNewID().setStringValue(passno);
							}
						}
						paxNum++;
					}
				}
			}
			// RedisManager.getManager().del("boolean");
			RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey("infRedis"));
		}
	}

	/**
	 * 将航班信息放入RS-XML
	 * 
	 * @param mapping
	 * @param flightArry
	 * @param flightsMap
	 * @throws ParseException
	 */

	private void addFlightList(Mapping mapping, FlightSegmentList flightArry, CommandRet commandRet,
			Map<String, Row> flightsMap) throws ParseException {
		Table flightsTable = commandRet.getParm("flights").getTableColumn();
		flightHelpMap.clear();
		if (flightsTable != null && flightsTable.getRowCount() > 0) {
			for (Row flightRow : flightsTable) {
				// 航班天id为唯一值
				String flightid = flightRow.getColumn("id").getStringColumn();
				flightHelpMap.put(flightid, flightRow);
			}
		}
		if (flightHelpMap != null && flightHelpMap.size() > 0) {
			int seg = 1;
			for (Entry<String, Row> entry : flightHelpMap.entrySet()) {
				Row flights = entry.getValue();
				String segFlightno = SEG + seg;
				// 航班id
				String flightid = flights.getColumn("id").getStringColumn();
				// 航班编号
				String flightno = flights.getColumn("flightno").getStringColumn();
				flightno=DateUtils.setFlightNo(flightno);
				flights.addColumn(SEG, segFlightno);
				mapping.getAllMap().put(flightno, segFlightno);
				mapping.getAllMap().put(flightid, segFlightno);
				flightsMap.put(flightid, flights);
				ListOfFlightSegmentType fsArry = flightArry.addNewFlightSegment();
				fsArry.setSegmentKey(segFlightno);
				seg = seg + 1;
				Departure DepartureArry = fsArry.addNewDeparture();
				// 出发地三字码
				String oricode = flights.getColumn("oricode").getStringColumn();
				// 出发时间
				String oriDay = flights.getColumn("oriDay").getStringColumn();
				if (oriDay != null && !"".equals(oriDay)) {
					Calendar oridate = DateUtils.getInstance().parseDate(oriDay, "yyyyMMdd");
					// 出发时间
					String oriTime = flights.getColumn("oriTime").getStringColumn();
					DepartureArry.addNewAirportCode().setStringValue(oricode);
					if (oridate != null) {
						DepartureArry.setDate(oridate);
						DepartureArry.setTime(oriTime);
					}
				}
				FlightArrivalType ftArry = fsArry.addNewArrival();
				// 到达地三字码
				String destcode = flights.getColumn("destcode").getStringColumn();
				ftArry.addNewAirportCode().setStringValue(destcode);
				// 到达时间
				String destDateTime = flights.getColumn("destDateTime").getStringColumn();
				if (destDateTime != null && !"".equals(destDateTime) && destDateTime.length() >= 10) {
					destDateTime = destDateTime.substring(0, 10);
					Calendar destDate = DateUtils.getInstance().parseDate(destDateTime, "yyyy-MM-dd");
					ftArry.setDate(destDate);
					// 到达时间
					String destTime = flights.getColumn("destTime").getStringColumn();
					ftArry.setTime(destTime);
				}
				MarketingCarrierFlightType mcfArry = fsArry.addNewMarketingCarrier();
				// 承运航编号
				String carricd = flights.getColumn("airlinecd").getStringColumn();
				mcfArry.addNewAirlineID().setStringValue(carricd);
				FlightNumber flightNumberArry = mcfArry.addNewFlightNumber();
				if (flightno.substring(flightno.length() - 1).matches("[A-Z]")) {
					// 航班后缀
					flightNumberArry.setOperationalSuffix(flightno.substring(flightno.length() - 1));
					flightNumberArry.setStringValue(flightno.substring(0, flightno.length() - 1));
				} else {
					flightNumberArry.setOperationalSuffix("");
					flightNumberArry.setStringValue(flightno);
				}
				// 舱位
				String cabin = flights.getColumn("cabin").getStringColumn();
				mcfArry.setResBookDesigCode(cabin);
				// 机型
				String carriflightno = flights.getColumn("planestype").getStringColumn();
				fsArry.addNewEquipment().addNewAircraftCode().setStringValue(carriflightno);
			}
		}
		mapping.setFlightsMap(flightsMap);
	}

	/**
	 * 添加辅营信息RS-xml
	 * 
	 * @param commandRet
	 * @param eatList
	 */
	private void addServiceList(CommandRet commandRet, ServiceList serviceListArry, Map<String, List<Row>> subMap,
			Mapping mapping) {
		Table submarketsTable = commandRet.getParm("submarkets").getTableColumn();
		if (submarketsTable != null && submarketsTable.getRowCount() > 0) {
			for (Row submarkets : submarketsTable) {
				// 辅营代码
				String submarketcode = submarkets.getColumn("submarketcode").getStringColumn();
				// 是否赠送(Y是 N否)[1E只显示购买的辅营]
				String isfree = submarkets.getColumn("isfree").getStringColumn();
				if ("N".equals(isfree)) {
					serSubmarketsMap(submarketcode, submarkets, subMap);
				}
			}
		}
		for (Entry<String, List<Row>> freeEntry : subMap.entrySet()) {
			// 辅营代码
			String submarketcode = freeEntry.getKey();
			String submarketRedis = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey(submarketcode));
			CommandData date = null;
			if (submarketRedis != null && !"".equals(submarketRedis)) {
				date = new CommandData();
				JsonUnit.fromJson(date, submarketRedis);
			}
			List<Row> submarketList = freeEntry.getValue();
			if (null != submarketList && submarketList.size() > 0) {
				ServiceDetailType serviceArry = serviceListArry.addNewService();
				ServiceIDType addNewServiceID = serviceArry.addNewServiceID();
				addNewServiceID.setStringValue(submarketcode);
				addNewServiceID.setOwner(AIRLINECODE);
				if (date == null) {
					serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue("");
				} else {
					serviceArry.addNewDescriptions().addNewDescription().addNewText()
							.setStringValue(date.getParm("description").getStringColumn());
				}

				serviceArry.setStatusCode("OK");
				for (Row submarkets : submarketList) {
					String paxid = submarkets.getColumn("paxid").getStringColumn();
					String flightid = submarkets.getColumn("flightid").getStringColumn();
					int buynum = submarkets.getColumn("buynum").getIntegerColumn();
					buynum = buynum == 0 ? 1 : buynum;
					for (int i = 0; i < buynum; i++) {
					String	pId = mapping.getAllMap().get(paxid);
					String	fId = mapping.getAllMap().get(flightid);
					Associations assArry = serviceArry.addNewAssociations();
					assArry.addNewTraveler().setTravelerReferences(getList(pId));
					assArry.addNewFlight().addNewSegmentReferences().setStringValue(fId);
					}
				}
			}
		}
		for (Entry<String, List<Row>> freeEntry : subMap.entrySet()) {
			String submarketcode = freeEntry.getKey();
			RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey(submarketcode));
		}
		// 辅营添加失败时，需要在详情中有展示
		String tabRedis = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("tab"));
		Table tabTable = null;
		if (tabRedis != null && !"".equals(tabRedis)) {
			CommandData date = new CommandData();
			JsonUnit.fromJson(date, tabRedis);
			tabTable = date.getParm("tab").getTableColumn();
			Map<String, ServiceDetailType> subRow = new HashMap<String, ServiceDetailType>();
			// 用于存放相同key'时，只有一个service节点
			for (Row row : tabTable) {
				String submarketcode = row.getColumn("submarketcode").getStringColumn();
				// 辅营的描述
				String submarketRedis = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey(submarketcode));
				CommandData d = null;
				if (submarketRedis != null && !"".equals(submarketRedis)) {
					d = new CommandData();
					JsonUnit.fromJson(d, submarketRedis);
				}
				String mode = row.getColumn("mode").getStringColumn();
				if ("1".equals(mode)) {
					if (!subRow.containsKey(submarketcode)) {
						ServiceDetailType serviceArry = serviceListArry.addNewService();
						ServiceIDType addNewServiceID = serviceArry.addNewServiceID();
						addNewServiceID.setStringValue(submarketcode);
						addNewServiceID.setOwner(AIRLINECODE);
						if (d == null) {
							serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue("");
						} else {
							serviceArry.addNewDescriptions().addNewDescription().addNewText()
									.setStringValue(d.getParm("description").getStringColumn());
						}
						serviceArry.setStatusCode("UC");
						Associations assArry = serviceArry.addNewAssociations();
						String pid = row.getColumn("pid").getStringColumn();
						pid = "P" + pid.substring(pid.length() - 1);
						String fid = row.getColumn("fid").getStringColumn();
						fid = "SEG" + fid.substring(fid.length() - 1);
						assArry.addNewTraveler().setTravelerReferences(getList(pid));
						assArry.addNewFlight().addNewSegmentReferences().setStringValue(fid);
						subRow.put(submarketcode, serviceArry);
					} else {
						ServiceDetailType serviceArry = subRow.get(submarketcode);
						Associations assArry = serviceArry.addNewAssociations();
						String pid = row.getColumn("pid").getStringColumn();
						pid = "P" + pid.substring(pid.length() - 1);
						String fid = row.getColumn("fid").getStringColumn();
						fid = "SEG" + fid.substring(fid.length() - 1);
						assArry.addNewTraveler().setTravelerReferences(getList(pid));
						assArry.addNewFlight().addNewSegmentReferences().setStringValue(fid);
					}
				}
			}
		}
		RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey("tab"));
		if (tabTable != null && tabTable.getRowCount() > 0) {
			for (Row row : tabTable) {
				String submarketcode = row.getColumn("submarketcode").getStringColumn();
				RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey(submarketcode));
			}
		}
		// 婴儿追加成功时，在service中显示
		String infbln = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("boolean"));
		if (infbln != null && !"".equals(infbln)) {
			if ("true".equals(infbln)) {
				String infservice = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("infService"));
				if (infservice != null && !"".equals(infservice)) {
					CommandData date = new CommandData();
					JsonUnit.fromJson(date, infservice);
					Table infService = date.getParm("infService").getTableColumn();
					ServiceDetailType serviceArry = serviceListArry.addNewService();
					ServiceIDType addNewServiceID = serviceArry.addNewServiceID();
					addNewServiceID.setStringValue("INFT");
					addNewServiceID.setOwner(AIRLINECODE);
					serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue("婴儿");
					serviceArry.setStatusCode("OK");
					for (Row row : infService) {
						Associations assArry = serviceArry.addNewAssociations();
						String pid = row.getColumn("pid").getStringColumn();
						pid = "P" + pid.substring(pid.length() - 1);
						String fid = row.getColumn("fid").getStringColumn();
						fid = "SEG" + fid.substring(fid.length() - 1);
						assArry.addNewTraveler().setTravelerReferences(getList(pid));
						assArry.addNewFlight().addNewSegmentReferences().setStringValue(fid);
					}
				}
			}
		}
		RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey("boolean"));
		RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey("infService"));

		// create时，成功显示在orderview中
		String createOk = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("createOk"));
		if (createOk != null && !"".equals(createOk)) {
			CommandData data = new CommandData();
			JsonUnit.fromJson(data, createOk);
			Table createOkTable = data.getParm("createOk").getTableColumn();
			if (createOkTable.getRowCount() > 0) {
				ServiceDetailType serviceArry = serviceListArry.addNewService();
				ServiceIDType addNewServiceID = serviceArry.addNewServiceID();
				addNewServiceID.setStringValue("INFT");
				addNewServiceID.setOwner(AIRLINECODE);
				serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue("婴儿");
				serviceArry.setStatusCode("OK");
				for (Row row : createOkTable) {
					Associations assArry = serviceArry.addNewAssociations();
					String pid = row.getColumn("pid").getStringColumn();
					pid = "P" + pid.substring(pid.length() - 1);
					String fid = row.getColumn("fid").getStringColumn();
					fid = "SEG" + fid.substring(fid.length() - 1);
					assArry.addNewTraveler().setTravelerReferences(getList(pid));
					assArry.addNewFlight().addNewSegmentReferences().setStringValue(fid);
				}
			}
		}
		RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey("createOk"));
	}

	/**
	 * 设置旅客与辅营的关联关系
	 * 
	 * @param paxid
	 * @param submarketid
	 * @param subMap
	 */
	public void serSubmarketsMap(String paxid, Row submarkets, Map<String, List<Row>> subMap) {
		List<Row> submarketList = subMap.get(paxid);
		if (null == submarketList) {
			submarketList = new ArrayList<Row>();
		}
		submarketList.add(submarkets);
		subMap.put(paxid, submarketList);
	}

	/**
	 * 座位信息
	 * 
	 * @param commandRet
	 * @param seatListArry
	 * @param mapping
	 */
	private void addSeatList(CommandRet commandRet, SeatList seatListArry, Mapping mapping) {
		Table seatsTable = commandRet.getParm("seats").getTableColumn();
		if (null != seatsTable && seatsTable.getRowCount() > 0) {
			for (Row seat : seatsTable) {
				String seatno = seat.getColumn("seatno").getStringColumn();
				String paxid = seat.getColumn("paxid").getStringColumn();
				String flightid = seat.getColumn("flightid").getStringColumn();
				paxid = mapping.getAllMap().get(paxid);
				flightid = mapping.getAllMap().get(flightid);
				ListOfSeatType seatArry = seatListArry.addNewSeats();
				SeatLocationType locaArry = seatArry.addNewLocation();
				seatArry.setStatusCode("OK");
				locaArry.setColumn(seatno.substring(seatno.length() - 1));
				locaArry.addNewRow().addNewNumber().setStringValue(seatno.substring(0, seatno.length() - 1));
				OrderItemAssociationType assArry = locaArry.addNewAssociations();
				ArrayList<String> list = new ArrayList<>();
				list.add(paxid);
				assArry.addNewPassengers().addNewPassenger().setRefs(list);
				// assArry.addNewPassengers().addNewPassenger().setPassengerAssociation(paxid);
				assArry.addNewFlight().addNewSegmentReferences().setStringValue(flightid);
			}
		}
		// 当追加座位失败时，详情返回中状态不同
		String seatsRedis = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_order.toKey("seats"));
		if (seatsRedis != null && !"".equals(seatsRedis)) {
			// Table rSeatsTable = JsonUnit.tableFromJson(seatsRedis);
			CommandData date = new CommandData();
			JsonUnit.fromJson(date, seatsRedis);
			Table rSeatsTable = date.getParm("seats").getTableColumn();
			for (Row row : rSeatsTable) {
				String mode = row.getColumn("mode").getStringColumn();
				if ("1".equals(mode)) {
					String seatno = row.getColumn("seatno").getStringColumn();
					String paxid = row.getColumn("pid").getStringColumn();
					String substring = paxid.substring(paxid.length() - 1);
					paxid = "P" + substring;
					String flightid = row.getColumn("fid").getStringColumn();
					String substringf = flightid.substring(flightid.length() - 1);
					flightid = "SEG" + substringf;
					/*
					 * paxid = mapping.getAllMap().get(paxid); flightid =
					 * mapping.getAllMap().get(flightid);
					 */
					ListOfSeatType seatArry = seatListArry.addNewSeats();
					SeatLocationType locaArry = seatArry.addNewLocation();
					seatArry.setStatusCode("UC");
					locaArry.setColumn(seatno.substring(seatno.length() - 1));
					locaArry.addNewRow().addNewNumber().setStringValue(seatno.substring(0, seatno.length() - 1));
					OrderItemAssociationType assArry = locaArry.addNewAssociations();
					ArrayList<String> list = new ArrayList<>();
					list.add(paxid);
					assArry.addNewPassengers().addNewPassenger().setRefs(list);
					assArry.addNewFlight().addNewSegmentReferences().setStringValue(flightid);
				}
			}
			RedisManager.getManager().del(RedisNamespaceEnum.api_cache_order.toKey("seats"));
		}
	}

	/**
	 * 
	 * @param commandRet
	 * @param OrderItemsArry
	 * @param mapping
	 * @throws ParseException
	 */
	private void addPaxflight(CommandRet commandRet, OrderItems OrderItemsArry, Mapping mapping,
			Map<String, List<Row>> submarketMap) throws ParseException {
		Table paxflights = commandRet.getParm("paxflights").getTableColumn();
		helpMap.clear();
		if (paxflights != null && paxflights.getRowCount() > 0) {
			for (Row paxflight : paxflights) {
				PNR = paxflight.getColumn("pnr").getStringColumn();
				// 人航段id
				String id = paxflight.getColumn("id").getStringColumn();
				helpMap.put(id, paxflight);
			}
		}
		if (helpMap != null && helpMap.size() > 0) {
			for (Entry<String, Row> paxFlightid : helpMap.entrySet()) {
				Row paxflight = paxFlightid.getValue();
				// 航段id
				String id = paxflight.getColumn("id").getStringColumn();
				// 航班id
				String flightid = paxflight.getColumn("flightid").getStringColumn();
				// 航班数据
				Row flights = mapping.getFlightsMap().get(flightid);
				OrderItem orderItemArry = OrderItemsArry.addNewOrderItem();
				ItemIDType ItemIDArry = orderItemArry.addNewOrderItemID();
				// 销售航司二字码
				String airlinecd = flights.getColumn("airlinecd").getStringColumn();
				ItemIDArry.setOwner(airlinecd);
				ItemIDArry.setStringValue(id);
				// 去程信息
				Flight flightArry = orderItemArry.addNewFlightItem().addNewOriginDestination().addNewFlight();
				// 航班号
				String flightno = flights.getColumn("flightno").getStringColumn();
				flightno=DateUtils.setFlightNo(flightno);
				flightArry.setRefs(getList(mapping.getAllMap().get(flightid)));
				// 出发节点
				Departure departureArry = flightArry.addNewDeparture();
				// 出发机场三字码
				String oricode = flights.getColumn("oricode").getStringColumn();
				// 出发日期
				String oriDay = flights.getColumn("oriDay").getStringColumn();
				if (oriDay != null && !"".equals(oriDay)) {
					Calendar oridate = DateUtils.getInstance().parseDate(oriDay, "yyyyMMdd");
					// 出发时间
					String oriTime = flights.getColumn("oriTime").getStringColumn();
					if (oridate != null) {
						// Departure --> AirportCode
						departureArry.addNewAirportCode().setStringValue(oricode);
						departureArry.setDate(oridate);
						departureArry.setTime(oriTime);
					}
				}
				// 到达节点
				FlightArrivalType newArrival = flightArry.addNewArrival();
				// 到达机场三字码
				String destcode = flights.getColumn("destcode").getStringColumn();
				// 到达日期
				String destDateTime = flights.getColumn("destDateTime").getStringColumn();
				if (destDateTime != null && !"".equals(destDateTime) && destDateTime.length() >= 10) {
					destDateTime = destDateTime.substring(0, 10);
					Calendar destDate = DateUtils.getInstance().parseDate(destDateTime, "yyyy-MM-dd");
					// arrival --> AirportCode
					newArrival.addNewAirportCode().setStringValue(destcode);
					newArrival.setDate(destDate);
					// 到达时间
					String destTime = flights.getColumn("destTime").getStringColumn();
					newArrival.setTime(destTime);
				}
				MarketingCarrierFlightType mcfArry = flightArry.addNewMarketingCarrier();
				mcfArry.addNewAirlineID().setStringValue(airlinecd);
				AIRLINECODE = airlinecd;
				FlightNumber fnArry = mcfArry.addNewFlightNumber();
				if (flightno.substring(flightno.length() - 1).matches("[A-Z]")) {
					// 航班后缀
					fnArry.setOperationalSuffix(flightno.substring(flightno.length() - 1));
					fnArry.setStringValue(flightno.substring(0, flightno.length() - 1));
				} else {
					fnArry.setOperationalSuffix("");
					fnArry.setStringValue(flightno);
				}
				// 旅客与辅营关联数据
				// 旅客id
				String paxid = paxflight.getColumn("paxid").getStringColumn();
				String paxNum = mapping.getAllMap().get(paxid);
				org.iata.iata.edist.OrderItemCoreType.OrderItem.Associations assArry = orderItemArry
						.addNewAssociations();
				// 旅客编号
				assArry.addNewPassengers().setPassengerReferences(getList(paxNum));
			}
		}
	}

	/**
	 * 辅营与品牌关联的节点
	 * 
	 * @param listOfServiceBundleArry
	 */
	public void addServiceBundleListArry(Mapping mapping, ListOfServiceBundleType listOfServiceBundleArry) {
		Map<String, String> ServiceBundleMap = mapping.getServiceBundleMap();
		for (Entry<String, String> freeEntry : ServiceBundleMap.entrySet()) {
			// 辅营id
			String submarketid = freeEntry.getValue().trim();
			if (!"".equals(submarketid) && null != submarketid) {
				ServiceBundle serviceBundleArry = listOfServiceBundleArry.addNewServiceBundle();
				String[] submarketidS = submarketid.split(" ");
				// 商品 id
				String brandid = freeEntry.getKey();
				serviceBundleArry.setListKey(brandid);
				serviceBundleArry.setItemCount(BigInteger.valueOf(submarketidS.length));
				org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle.Associations associationsArry = serviceBundleArry
						.addNewAssociations();
				for (String id : submarketidS) {
					associationsArry.addNewServiceReference().setStringValue(id);
				}
			}
		}
	}

	/**
	 * 航班于OD关联
	 * 
	 * @param commandRet
	 * @param flightListArry
	 * @param originArry
	 */
	public void addOriginArryArry(CommandRet commandRet, FlightList flightListArry, OriginDestinationList originArry,
			Mapping mapping) {
		String oricode = "";
		String destcode = "";
		if (flightHelpMap != null && flightHelpMap.size() > 0) {
			int flt = 1;
			for (Entry<String, Row> Entry : flightHelpMap.entrySet()) {
				Row flights = Entry.getValue();
				org.iata.iata.edist.DataListType.FlightList.Flight flightArry = flightListArry.addNewFlight();
				// 出发地三字码
				oricode = flights.getColumn("oricode").getStringColumn();
				// 到达地三字码
				destcode = flights.getColumn("destcode").getStringColumn();
				// 航班编号
				String flightno = flights.getColumn("id").getStringColumn();
				flightno=DateUtils.setFlightNo(flightno);
				flightArry.setFlightKey(FLT + flt);
				flightArry.addNewSegmentReferences().setStringValue(mapping.getAllMap().get(flightno));
				OriginDestination originDestinationArry = originArry.addNewOriginDestination();
				originDestinationArry.addNewDepartureCode().setStringValue(oricode);
				originDestinationArry.addNewArrivalCode().setStringValue(destcode);
				originDestinationArry.addNewFlightReferences().setStringValue(FLT + flt);
				flt = flt + 1;
			}
		}
	}

	public void addSeats(CommandRet commandRet, Mapping mapping) {
		Table seatsTable = commandRet.getParm("seats").getTableColumn();
		if (null != seatsTable) {
			for (Row seats : seatsTable) {
				mapping.getSeatsMap().put(seats.getColumn("flightid").getStringColumn(), seats);
			}
		}
	}

	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * 
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
			error.setShortText(TipMessager.getMessage(errCode, ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
	}

	/**
	 * 根据Java编程规范：由于for循环中不能创建对象，所有将创建对象放到方法里
	 *
	 * @return list
	 */
	public List<String> getList(String args) {
		List<String> list = new ArrayList<String>();
		list.add(args);
		return list;
	}

	private static final class Mapping {
		private Map<String, String> allMap = new HashMap<String, String>();
		private Map<String, Row> flightsMap;
		/**
		 * 辅营信息 String 免费服务 id Row 服务内容
		 */
		private Map<String, String> ServiceBundleMap = new HashMap<String, String>();

		private Map<String, Row> SeatsMap = new HashMap<String, Row>();

		public Map<String, Row> getFlightsMap() {
			return flightsMap;
		}

		public void setFlightsMap(Map<String, Row> flightsMap) {
			this.flightsMap = flightsMap;
		}

		public Map<String, String> getServiceBundleMap() {
			return ServiceBundleMap;
		}

		public Map<String, Row> getSeatsMap() {
			return SeatsMap;
		}

		public Map<String, String> getAllMap() {
			return allMap;
		}
	}
}