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
import java.util.Map.Entry;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AgencyIDType;
import org.iata.iata.edist.BookingReferenceType;
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
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.FlightType.Flight;
import org.iata.iata.edist.ItemIDType;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfSeatType;
import org.iata.iata.edist.ListOfServiceBundleType;
import org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle;
import org.iata.iata.edist.MsgPartiesType.Sender;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.MsgPartiesType;
import org.iata.iata.edist.OrderCreateRQDocument;
import org.iata.iata.edist.OrderCreateRQDocument.OrderCreateRQ.Query;
import org.iata.iata.edist.OrderIDType;
import org.iata.iata.edist.OrderItemAssociationType;
import org.iata.iata.edist.OrderItemCoreType.OrderItem;
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
import org.iata.iata.edist.PhoneType.Number;
import org.iata.iata.edist.SeatLocationType;
import org.iata.iata.edist.ServiceDescriptionType;
import org.iata.iata.edist.SegmentReferencesDocument.SegmentReferences;
import org.iata.iata.edist.ServiceCoreType.Associations;
import org.iata.iata.edist.ServiceDescriptionType.Description;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceIDType;
import org.iata.iata.edist.TravelAgencySenderType;
import org.iata.iata.edist.ServiceListDocument.ServiceList;
import org.iata.iata.edist.TravelerCoreType.Age.BirthDate;
import org.iata.iata.edist.TravelerCoreType.PTC;
import org.iata.iata.edist.TravelerSummaryType.Name;
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

public class APIOrderCreateNDCDA {
	private static final Logger LOGGER = LoggerFactory.getLogger(APIOrderCreateNDCDA.class);
	private String language = ApiServletHolder.getApiContext().getLanguage();
	private Map<String,String> contactsMap =new HashMap<String,String>();
	private static final String MALE = "Male";
	private String  PNR ="";
	private static final String SEG ="SEG";
	private static final String FLT ="FLT";
	private static final String CONTACT="CONTACT";
	private String AIRLINECD="";
	public void doServletDA(SelvetContext<ApiContext> context) throws Exception {
		try {
			//转换 xml-->Reqbean
			transInputXmlToRequestBean(context);
		}
		catch (APIException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_ORDER_CREATE, ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}
	

	public void transInputXmlToRequestBean(SelvetContext<ApiContext> context) throws APIException, Exception {
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		OrderCreateRQDocument rootDoc = null;
		rootDoc = OrderCreateRQDocument.Factory.parse(xmlInput);

		OrderCreateRQDocument.OrderCreateRQ reqDoc = rootDoc.getOrderCreateRQ();
		input=APICacheHelper.setDeptInfo(context, reqDoc.getParty(),input);
		//input.addParm("owner", "1E");
		
		// 部门ID
	/*	String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid",deptno);
		
		//会员id 
		input.addParm("memberid", context.getContext().getUserID());*/
		
		Query query = reqDoc.getQuery();
		//旅客信息/联系人信息
		Passenger[] passenger = query.getPassengers().getPassengerArray();
		Map<String, Row> paxsMap = new HashMap<String, Row>();
		Map<String, String> guardianMap = new HashMap<String, String>();
		addPaxsCommand(input,passenger,paxsMap,guardianMap);
		
		//pnr
		BookingReferenceType bookingReference = query.getBookingReferences().getBookingReferenceArray(0);
		if (null == bookingReference) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_PNR, language));
			throw APIException.getInstance(ErrCodeConstants.API_UNKNOW_PNR);
		}else{
			String pnr = bookingReference.getID();
			input.addParm("pnr", pnr);
			CodesetType type = bookingReference.getType();
			if (!"".equals(type) && type!=null) {
				input.addParm("flag", "RLR".equals(type.getCode())?"1":"0");
			}else {
				input.addParm("flag", "0");
			}
		}
		//航班信息 
		 DataListType dataLists = query.getDataLists();
		 FlightSegmentList flightSegmentList = dataLists.getFlightSegmentList();
		 ListOfFlightSegmentType[] flightSegmentArr = flightSegmentList.getFlightSegmentArray();
		 Map<String, ListOfFlightSegmentType> FlightSegmentMap = new HashMap<String, ListOfFlightSegmentType>();
	 	 getFlightSegments(flightSegmentArr,FlightSegmentMap);
	 	
	 	//辅营信息
	 	ServiceList auxiliaryList = dataLists.getServiceList();
	 	 if (null != auxiliaryList) {
	 		ServiceDetailType[] serviceArray = auxiliaryList.getServiceArray();
	 		if (auxiliaryList.getServiceArray() !=null 
	 				&& FlightSegmentMap !=null
	 				&& paxsMap !=null) {
	 			addAuxiliary(serviceArray,FlightSegmentMap,input,paxsMap);
			}
		}
	 	//当婴儿添加成功时，在orderview中显示状态为OK.
			Table infViewTable = new Table(new String[] { "pid", "fid" });
			Iterator<String> iter = guardianMap.keySet().iterator();
			  while(iter.hasNext()){
			   String key=iter.next();
			   String value = guardianMap.get(key);
			   Iterator<String> iterator = FlightSegmentMap.keySet().iterator();
			   while(iterator.hasNext()){
				   ListOfFlightSegmentType segmentType = FlightSegmentMap.get(iterator.next());
				   String segmentKey = segmentType.getSegmentKey();
				   Row addRow = infViewTable.addRow();
				   addRow.addColumn("pid", value);
				   addRow.addColumn("fid", segmentKey);
			   }
			  }
			if (infViewTable.getRowCount()>0) {
				CommandData data=new CommandData();
				data.addParm("createOk", infViewTable);
				RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey("createOk"), JsonUnit.toJson(data), 60);
			}
	 	 
	 	 
	 	//选座
	 	SeatList seatListArry = dataLists.getSeatList();
	 	if(null !=seatListArry){
	 		ListOfSeatType[] seatsArray = seatListArry.getSeatsArray();
		 	Map<String, ListOfSeatType> seatsMap = new HashMap<String, ListOfSeatType>();
		 	if (seatListArry.getSeatsArray() !=null 
		 			&& FlightSegmentMap !=null 
		 			&& paxsMap !=null) {
		 		addSeat(seatsArray,seatsMap,FlightSegmentMap,input,paxsMap);
			}
	 	}
	 	org.iata.iata.edist.DataListType.FlightList.Flight[] flightArray = dataLists.getFlightList().getFlightArray();
		Map<String,String> fltMap = new HashMap<String, String>();
		addFlight(flightArray,fltMap);
		
		OriginDestinationList originDestList = dataLists.getOriginDestinationList();
		OriginDestination[] originDestinationArray = originDestList.getOriginDestinationArray();
		addOrigin(input, FlightSegmentMap, fltMap, originDestinationArray);
	}
	
	
	//新增辅营信息
	private void addAuxiliary(ServiceDetailType[] serviceArray,Map<String, ListOfFlightSegmentType> flightSegmentMap,
			CommandData input, Map<String, Row> paxsMap) {
		
		Table AuxiliaryTable = new Table(new String[]{"mode","buynum","passno","passtype",
				"paxname","submarketcode","carricd","flightno","flightsuffix","flightdate","oricode","descode","fid","pid"});
		Map<String, String> subMap=new HashMap<String, String>();
		//辅营详情
		for (ServiceDetailType auxiliary : serviceArray) {
 			String submarketcode = auxiliary.getServiceID().getStringValue().toUpperCase();
 			/**
 			 * 辅营描述   start
 			 */
 			if (!subMap.containsKey(submarketcode)) {
				String description="";
				String name ="";
				ServiceDescriptionType descriptions = auxiliary.getDescriptions();
				if (descriptions!=null && !"".equals(descriptions)) {
					Description destion = descriptions.getDescriptionArray()[0];
					if (destion!=null && !"".equals(destion)) {
						Text text =destion.getText();
						if (text!=null && !"".equals(text)) {
							   description = text.stringValue();
							}
					}
				}
				//name
				 org.iata.iata.edist.ServiceCoreType.Name n = auxiliary.getName();
				 if (n!=null && !"".equals(n)) {
					 name = n.getStringValue();
				}
				if (!"".equals(description) || !"".equals(name)) {
					CommandData date=new CommandData();
					date.addParm("name", name);
					date.addParm("description", description);
					RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey(submarketcode), JsonUnit.toJson(date), 60);
					subMap.put(submarketcode, submarketcode);
				}
			}
 			/**
 			 * 辅营描述  end
 			 */
 			Associations[] associationsArray = auxiliary.getAssociationsArray();
 			for (Associations associations : associationsArray) {
 			//关联的旅客信息
 			Object buynum = associations.getTraveler().getTravelerReferences().get(0);
 			Row paxRow =paxsMap.get(buynum); // P1旅客的证件信息
			String lastName = paxRow.getColumn("lastname").getStringColumn();
			String firstName = paxRow.getColumn("firstname").getStringColumn();
			String passtype = paxRow.getColumn("passtype").getStringColumn();
			String passno = paxRow.getColumn("passno").getStringColumn();
 			//兼容1e中 辅营中seg信息为空时 预定全航段
			 SegmentReferences[] segmentReferencesArray = associations.getFlight().getSegmentReferencesArray();
			 if (segmentReferencesArray.length <= 0) {
					for(ListOfFlightSegmentType segmentType: flightSegmentMap.values()){
						Departure departure = segmentType.getDeparture();
						//出发地
						String flightoricode =  departure.getAirportCode().getStringValue();
						//出发时间
						Date oridateTime = departure.getDate().getTime();
						FlightArrivalType arrival = segmentType.getArrival();
						//到达地
						String destcode = arrival.getAirportCode().getStringValue();
						MarketingCarrierFlightType marketingCarrier = segmentType.getMarketingCarrier();
						//航班号
						String flghtNo = marketingCarrier.getFlightNumber().getStringValue();
						//航班后缀 
						String suffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
						//航司二字码
						String carricd = marketingCarrier.getAirlineID().getStringValue();
						String segmentKey = segmentType.getSegmentKey();
						Row auxilRow = AuxiliaryTable.addRow();
						auxilRow.addColumn("mode", "1");
						auxilRow.addColumn("buynum", 1);
						auxilRow.addColumn("submarketcode", submarketcode);
						auxilRow.addColumn("passno", passno);
						auxilRow.addColumn("passtype", passtype);
						auxilRow.addColumn("paxname", lastName+"/"+firstName);
						auxilRow.addColumn("carricd", carricd);
						auxilRow.addColumn("flightno", DateUtils.getFlightNo(flghtNo));
						auxilRow.addColumn("flightsuffix", suffix);
						auxilRow.addColumn("flightdate", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
						auxilRow.addColumn("oricode", flightoricode);
						auxilRow.addColumn("descode", destcode);
						auxilRow.addColumn("pid",buynum.toString());
						auxilRow.addColumn("fid",segmentKey);
					}
			 	}else {
			 		String segmentKey = associations.getFlight().getSegmentReferencesArray(0).getStringValue();
		 			ListOfFlightSegmentType  flightSegment = flightSegmentMap.get(segmentKey);
					Departure departure = flightSegment.getDeparture();
					//出发地
					String flightoricode =  departure.getAirportCode().getStringValue();
					//出发时间
					Date oridateTime = departure.getDate().getTime();
					FlightArrivalType arrival = flightSegment.getArrival();
					//到达地
					String destcode = arrival.getAirportCode().getStringValue();
					MarketingCarrierFlightType marketingCarrier = flightSegment.getMarketingCarrier();
					//航班号
					String flghtNo = marketingCarrier.getFlightNumber().getStringValue();
					//航班后缀 
					String suffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
					//航司二字码
					String carricd = marketingCarrier.getAirlineID().getStringValue();
					Row auxilRow = AuxiliaryTable.addRow();
					auxilRow.addColumn("mode", "1");
					auxilRow.addColumn("buynum", 1);
					auxilRow.addColumn("submarketcode", submarketcode);
					auxilRow.addColumn("passno", passno);
					auxilRow.addColumn("passtype", passtype);
					auxilRow.addColumn("paxname", lastName+"/"+firstName);
					auxilRow.addColumn("carricd", carricd);
					auxilRow.addColumn("flightno", DateUtils.getFlightNo(flghtNo));
					auxilRow.addColumn("flightsuffix", suffix);
					auxilRow.addColumn("flightdate", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
					auxilRow.addColumn("oricode", flightoricode);
					auxilRow.addColumn("descode", destcode);
					auxilRow.addColumn("pid",buynum.toString());
					auxilRow.addColumn("fid",segmentKey);
				}
 			}
		}
		input.addParm("spaxsubmarkets", AuxiliaryTable);
	}


	//新增选座功能
	private void addSeat(ListOfSeatType[] seatsArray,Map<String, ListOfSeatType> seatsMap,Map<String, ListOfFlightSegmentType> flightSegmentMap,
			CommandData input,Map<String, Row> paxsMap ){
		
		Table seatsTable = new Table(new String[]{"passno","passtype","paxname","carricd",
				"flightno","flightsuffix","flightdate","oricode","descode","seatno","mode","fid","pid"});
		for (ListOfSeatType seats : seatsArray) {
			//座位
			SeatLocationType location = seats.getLocation();
			String column = location.getColumn();//座位号后缀
			String number =location.getRow().getNumber().getStringValue();//座位号
			//旅客
			OrderItemAssociationType associations = location.getAssociations();
			Passenger[] passengerArray = associations.getPassengers().getPassengerArray();
			String passAsson = passengerArray[0].getPassengerAssociation();//旅客
			
			Row paxRow =paxsMap.get(passAsson);
			String lastName = paxRow.getColumn("lastname").getStringColumn();
			String firstName = paxRow.getColumn("firstname").getStringColumn();
			String passtype = paxRow.getColumn("passtype").getStringColumn();
			String passno = paxRow.getColumn("passno").getStringColumn();
			
			//航班 segmentKey
			String segmentKey = associations.getFlight().getSegmentReferencesArray(0).getStringValue();
			//航班信息
			ListOfFlightSegmentType  flightSegment = flightSegmentMap.get(segmentKey);
			Departure departure = flightSegment.getDeparture();
			//出发地
			String flightoricode =  departure.getAirportCode().getStringValue();
			//出发时间
			Date oridateTime = departure.getDate().getTime();
			FlightArrivalType arrival = flightSegment.getArrival();
			//到达地
			String destcode = arrival.getAirportCode().getStringValue();
			MarketingCarrierFlightType marketingCarrier = flightSegment.getMarketingCarrier();
			//航班号
			String flghtNo = marketingCarrier.getFlightNumber().getStringValue();
			//航班后缀 
			String suffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
			//航司二字码
			String carricd = marketingCarrier.getAirlineID().getStringValue();
			
			Row seatsRow = seatsTable.addRow();
			seatsRow.addColumn("passno", passno);
			seatsRow.addColumn("passtype", passtype);
			seatsRow.addColumn("paxname", lastName+"/"+firstName);
			seatsRow.addColumn("carricd", carricd);
			seatsRow.addColumn("flightno", DateUtils.getFlightNo(flghtNo));
			seatsRow.addColumn("flightsuffix", suffix);
			seatsRow.addColumn("flightdate", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
			seatsRow.addColumn("oricode", flightoricode);
			seatsRow.addColumn("descode", destcode);
			seatsRow.addColumn("seatno", number+column);
			seatsRow.addColumn("mode", "1");
			seatsRow.addColumn("fid", segmentKey);
			seatsRow.addColumn("pid", passAsson);
		}
		input.addParm("seats", seatsTable);
	}


	//添加航班信息到CommandData
	private void getFlightSegments(ListOfFlightSegmentType[] flightSegmentArr,Map<String, ListOfFlightSegmentType> FlightSegmentMap ){
		for (ListOfFlightSegmentType flightSegment : flightSegmentArr) {
			String segmentKey = flightSegment.getSegmentKey();
			FlightSegmentMap.put(segmentKey, flightSegment);
		}
	}


	private void addOrigin(CommandData input,Map<String, ListOfFlightSegmentType> FlightSegmentMap,Map<String,String> fltMap,OriginDestination[] originDestinationArray) throws APIException {
		//到达地
		String destCode = "";
		//到达时间
		Date destDateTime = null;
		//出发地
		String flightOricode =  "";
		//出发时间
		Date oridate= null;
		//false 单程  true 往返
		boolean routtype = false;
		Table flightTable = new Table(new String[]{"airlinecd","flightno","oridate","oriDay","routtype","cabin","oriDateTime","oricode","flightDay","destcode"});
		for (OriginDestination originDestination : originDestinationArray) {
			//航班key
			String  fltkey =	originDestination.getFlightReferences().getStringValue();
			//航班segkey
			String segkey =fltMap.get(fltkey);
			//航班数据
			ListOfFlightSegmentType flightSegment = FlightSegmentMap.get(segkey);
			Departure departure = flightSegment.getDeparture();
			//出发地
			String flightoricode =  departure.getAirportCode().getStringValue();
			//出发时间
			Date oridateTime = departure.getDate().getTime();
			FlightArrivalType arrival = flightSegment.getArrival();
			Date destDate=null;
			if (!StringUtils.isEmpty(arrival.getDate())) {
				//到达时间
				destDate = arrival.getDate().getTime();
			}
			//到达地
			String destcode = arrival.getAirportCode().getStringValue();
			if(!routtype){
				destDateTime =destDate;
				destCode =destcode;
				oridate =oridateTime;
				flightOricode = flightoricode;
			}
			MarketingCarrierFlightType marketingCarrier = flightSegment.getMarketingCarrier();
			//航司二字码
			String airlineID = marketingCarrier.getAirlineID().getStringValue();
			if (StringUtils.isEmpty(airlineID)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_MARKET_FLIGHT, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_MARKET_FLIGHT);
			}
			//航班号 0123 0012 0001
			String flghtNo = marketingCarrier.getFlightNumber().getStringValue();
			//航班后缀 
			String Suffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
			if(!StringUtils.hasLength(Suffix)){
				Suffix = "";
			}
			//仓位
			String cabin = marketingCarrier.getResBookDesigCode();
			//S 单程 R 往返
			String  rout ="G";
			if(routtype){
				rout ="R";
			}
			Row flightRow = flightTable.addRow();
			flightRow.addColumn("airlinecd", airlineID);
			flightRow.addColumn("flightno",DateUtils.getFlightNo(flghtNo)+Suffix);
			flightRow.addColumn("oridate", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
			flightRow.addColumn("oriDay",DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
			flightRow.addColumn("routtype",rout);
			flightRow.addColumn("cabin",cabin);
			flightRow.addColumn("oriDateTime", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
			flightRow.addColumn("oricode",flightoricode);
			flightRow.addColumn("destcode",destcode);
			flightRow.addColumn("flightDay", DateUtils.getInstance().formatDate(oridateTime, "yyyyMMdd"));
			routtype=true;
		}
		input.addParm("flights", flightTable);
		input.addParm("oridate", DateUtils.getInstance().formatDate(oridate, "yyyyMMdd"));
		input.addParm("oricode", flightOricode);
		input.addParm("isoCode", "USD");
		input.addParm("destcode", destCode);
		input.addParm("routtype", originDestinationArray.length>1?"R":"S");
		if (!StringUtils.isEmpty(destDateTime)) {
		input.addParm("destdate", DateUtils.getInstance().formatDate(destDateTime, "yyyyMMdd"));
		}
	}
	

	private void addFlight(org.iata.iata.edist.DataListType.FlightList.Flight[] flightArray,Map<String,String> fltMap) throws APIException {
		for (org.iata.iata.edist.DataListType.FlightList.Flight flight : flightArray) {
			String fltkey =flight.getFlightKey();
			String seg = flight.getSegmentReferences().getStringValue();
			fltMap.put(fltkey, seg);
		}
	}
	
	
	/**
	 * 旅客信息
	 * @param input
	 * @param paxsMap 
	 * @param guardianMap 
	 * @throws APIException 
	 */
	private void addPaxsCommand(CommandData input, Passenger[] passengerArr, Map<String, Row> paxsMap, Map<String, String> guardianMap) throws APIException {
		if (passengerArr == null || passengerArr.length < 1) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PAXS, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_PAXS);
		}
	
		Table paxsTable = new Table(new String[]{
				"id","paxtype","lastname","firstname","birth","paxsex",
	            "telephone","email","guardian","passtype",
	            "passno","issuecountry","docexpiry","birthcountry","areacode","contactprefix"});
		for (Passenger passenger : passengerArr) {
			Row paxsRow = paxsTable.addRow();
			//旅客ID
			String id = passenger.getObjectKey();
			if (!StringUtils.hasLength(id)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PAXS_ID, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_PAXS_ID);
			}
			paxsRow.addColumn("id", id);
			//旅客类型
			String paxtype =passenger.getPTC().getStringValue();
			if (null !=passenger.getPTC() && passenger.getPTC().getStringValue() !=null) {
				 paxsRow.addColumn("paxtype", paxtype);
				 String guardian = passenger.getPassengerAssociation();
				 if (StringUtils.hasLength(guardian)) {
					 guardianMap.put(guardian, id);
				 }
				 //旅客类型是婴儿就去map中反向查找他的监护人
				 if ("INF".equals(paxtype)) {
					 paxsRow.addColumn("guardian", guardianMap.get(id));
				 }
			}
			
			//旅客生日
			Date birth = null;
			if (passenger.getAge() != null 
					&& passenger.getAge().getBirthDate() != null 
					&& passenger.getAge().getBirthDate().getDateValue() != null) {
				BirthDate birthDate = passenger.getAge().getBirthDate();
				birth =birthDate.getDateValue();
				try {
					paxsRow.addColumn("birth", DateUtils.getInstance().
							formatDate(birth, "yyyyMMdd"));
				}
				catch (Exception e) {
					LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_BIRTHDAY, language), e);
					throw APIException.getInstance(ErrCodeConstants.API_CONVERT_BIRTHDAY,e);
				}
			}
			//旅客姓名
			Name passName = passenger.getName();
			String lastname = "";
			String firstname = "";
			if (StringUtils.isEmpty(passName)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_PERSONNAME, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_PERSONNAME);
			}else{
				lastname = passName.getSurname().getStringValue();
				firstname = passName.getGivenArray(0).getStringValue();
				paxsRow.addColumn("lastname", lastname);
				paxsRow.addColumn("firstname", firstname);
			}
			
			Table contactsRow = new Table(new String[]{"areacode","contactprefix","telephone","email","lastname"});
			Row row = contactsRow.addRow();
			//乘机人/联系人信息
			String areaCode ="";
			String telephone ="";
			String email = "";
			if (passenger.getContacts() !=null 
					&& passenger.getContacts().getContactArray() != null 
					&& passenger.getContacts().getContactArray().length > 0) {
				Contact[] contactArray = passenger.getContacts().getContactArray();
				for (Contact contact : contactArray) {
					String contactType = contact.getContactType().getStringValue();
				if ("PASSENGER".equalsIgnoreCase(contactType)) {
						//手机区号
						if (contact.getPhoneContact() != null 
								&& contact.getPhoneContact().getNumberArray() != null 
								&& contact.getPhoneContact().getNumberArray(0).getAreaCode() != null) {
							Number numberArray = contact.getPhoneContact().getNumberArray(0);
							areaCode = numberArray.getAreaCode();
							paxsRow.addColumn("contactprefix", areaCode);
						}
						//乘机人手机号
						if (contact.getPhoneContact() != null 
								&& contact.getPhoneContact().getNumberArray() != null 
								&& contact.getPhoneContact().getNumberArray(0).getStringValue() != null) {
							Number numberArray = contact.getPhoneContact().getNumberArray(0);
							telephone = numberArray.getStringValue();
							paxsRow.addColumn("telephone", telephone);
						}
						//乘机邮箱
						if (contact.getEmailContact() != null 
								&& contact.getEmailContact().getAddress() != null 
								&& contact.getEmailContact().getAddress().getStringValue() != null) {
							email = contact.getEmailContact().getAddress().getStringValue();
							paxsRow.addColumn("email", email);
						}
				}else if("CONTACT".equalsIgnoreCase(contactType)){
					//手机区号
					if (contact.getPhoneContact() != null 
							&& contact.getPhoneContact().getNumberArray() != null 
							&& contact.getPhoneContact().getNumberArray(0).getAreaCode() != null) {
						Number numberArray = contact.getPhoneContact().getNumberArray(0);
						String contactprefix = numberArray.getAreaCode();
						row.addColumn("contactprefix", contactprefix);
					}
					//乘机人手机号
					if (contact.getPhoneContact() != null 
							&& contact.getPhoneContact().getNumberArray() != null 
							&& contact.getPhoneContact().getNumberArray(0).getStringValue() != null) {
						Number numberArray = contact.getPhoneContact().getNumberArray(0);
						String ctelephone = numberArray.getStringValue();
						row.addColumn("telephone", ctelephone);
					}
					//乘机邮箱
					if (contact.getEmailContact() != null 
							&& contact.getEmailContact().getAddress() != null 
							&& contact.getEmailContact().getAddress().getStringValue() != null) {
						String femail = contact.getEmailContact().getAddress().getStringValue();
						row.addColumn("email", femail);
					}
					//联系人姓名
					if (contact.getOtherContactMethod() != null 
							&& contact.getOtherContactMethod().getName() != null) {
						String flastname = contact.getOtherContactMethod().getName();
						row.addColumn("lastname", flastname);
					}
					input.addParm("contacts", contactsRow);
					}
				}
			}
			//性别
			String paxsex ="";
			if(null != passenger.getGender()){
				paxsex = MALE.equals(passenger.getGender().stringValue())?"M":"F";
				paxsRow.addColumn("paxsex", paxsex);
			}
			String	passtype="";
			String  passno = "";
			PassengerIDInfo passengerIDInfo = passenger.getPassengerIDInfo();
			if(passengerIDInfo != null && passengerIDInfo.getFOID() != null){
				FOID foid = passengerIDInfo.getFOID();
				//证件类型
				passtype = foid.getType();
				//证件号
				passno = foid.getID().getStringValue();
				paxsRow.addColumn("passtype", passtype);
				paxsRow.addColumn("passno", passno);
			}
			//证件类型，证件号
			 String birthCountry = "";
			 String countryOfIssuance ="";
			 String date="";
			if (passengerIDInfo!=null && passengerIDInfo.getPassengerDocumentArray().length>0) {
				 PassengerDocument documentArray = passengerIDInfo.getPassengerDocumentArray(0);
				 birthCountry = documentArray.getBirthCountry();
				 String paxid = documentArray.getID();
				 String type = documentArray.getType();
				 Calendar dateOfExpiration = documentArray.getDateOfExpiration();
				 if (dateOfExpiration != null) {
					  date = DateUtils.getInstance().formatDate(dateOfExpiration, "yyyyMMdd");
				}
				countryOfIssuance = documentArray.getCountryOfIssuance();
				paxsRow.addColumn("passtype", type); 
				paxsRow.addColumn("passno", paxid); 
				paxsRow.addColumn("birthcountry", birthCountry); 
				paxsRow.addColumn("docexpiry", date); 
				paxsRow.addColumn("issuecountry", countryOfIssuance); 
			}
			paxsMap.put(id,paxsRow);
		}
		input.addParm("paxs", paxsTable);
	}


/**************************************************************************************************/
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		OrderViewRSDocument doc = OrderViewRSDocument.Factory.newInstance();
		OrderViewRSDocument.OrderViewRS root = doc.addNewOrderViewRS();
		Mapping mapping = new Mapping();
		try {
			if (processError(commandRet, root)) {
				return doc;
			}
			root.addNewSuccess();
			Response responseArry = root.addNewResponse();
			/*旅客信息*****************************************/
			Passengers  passengersArry= responseArry.addNewPassengers();
			addPaxsXML(commandRet,passengersArry,mapping);
			// 订单号
			/*String orderNo = commandRet.getParm("orderno").getStringColumn();*/
			// OrderViewRS->Response->Order
			Order order =  responseArry.addNewOrder();
			/*OrderIDType orderID = order.addNewOrderID();
			orderID.setStringValue(orderNo);*/
			BookingReferenceType  brtArry = order.addNewBookingReferences().addNewBookingReference();
			//支付时间(秒)
			Table paysTabe = commandRet.getParm("pays").getTableColumn();
			String maxpay = paysTabe.getRow(0).getColumn("maxpaysecond").getStringColumn();
			int hour=(Integer.valueOf(maxpay)/(60*60));
			int min=((Integer.valueOf(maxpay)/(60))-hour*60);
			int s=(Integer.valueOf(maxpay)-hour*60*60-min*60);
			maxpay=hour+"H"+min+"M"+s+"S";
			order.addNewTimeLimits().addNewPaymentTimeLimit().setRefs(getList(maxpay));
			order.addNewStatus().addNewStatusCode().setCode("OK");
			//人航段信息
			OrderItems OrderItemsArry = order.addNewOrderItems();
			DataListType  dataListArry = responseArry.addNewDataLists();
			FlightSegmentList flightArry = dataListArry.addNewFlightSegmentList();
			//去程回程 关联航班信息
			FlightList  flightListArry = dataListArry.addNewFlightList();
			//航班查询的出发地
			OriginDestinationList originArry = dataListArry.addNewOriginDestinationList();
			/*//辅营与品牌关联
			ListOfServiceBundleType listOfServiceBundleArry = dataListArry.addNewServiceBundleList();*/
			//辅营信息
			ServiceList serviceListA= dataListArry.addNewServiceList();
			SeatList seatListArry = dataListArry.addNewSeatList();
			//航班信息
			addSeats(commandRet,mapping);
			addFlightList(mapping,flightArry,commandRet);
			Map<String,List<Row>> submarketMap = new HashMap<String,List<Row>>();
			addSeatList(commandRet,seatListArry,mapping);
			addPaxflight(commandRet,OrderItemsArry,mapping,submarketMap);
			addServiceList(commandRet,serviceListA,submarketMap,mapping);
			/*addServiceBundleListArry(mapping,listOfServiceBundleArry);*/
			addOriginArryArry(commandRet,flightListArry,originArry,mapping);
//			brtArry.setID(PNR);
			brtArry.setID(input.getParm("pnr").getStringColumn());

			/*brtArry.addNewAirlineID().setStringValue(CARRICD);*/
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
	 * 添加旅客信息到RS-XML
	 * @param commandRet
	 * @param passengersArry
	 * @throws APIException
	 */
	private void addPaxsXML(CommandRet commandRet,Passengers  passengersArry,Mapping mapping) throws APIException{
		//乘机人
		Table paxsTable = commandRet.getParm("paxs").getTableColumn();
		if (paxsTable != null && paxsTable.getRowCount() > 0) {
			Map<String, String> paxsMap = new HashMap<String, String>();
			int paxNum = 1;
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
				// 出生日期
				Date birthDate = paxs.getColumn("birth").getDateColumn();
				if (null != birthDate && !"".equals(birthDate)) {
					passengerArry.addNewAge().addNewBirthDate().setDateValue(birthDate);
				}
				// 旅客姓名 
				Name nameArry = passengerArry.addNewName();
				String lastname = paxs.getColumn("lastname").getStringColumn();
				String firstname = paxs.getColumn("firstname").getStringColumn();
				String areacode = paxs.getColumn("contactprefix").getStringColumn();
				String telephone = paxs.getColumn("telephone").getStringColumn();
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
					if(StringUtils.hasLength(email)){
						contactArry.addNewEmailContact().addNewAddress().setStringValue(email);
					}
					if(StringUtils.hasLength(telephone)){
						org.iata.iata.edist.PhoneType.Number numberArry1 = contactArry.addNewPhoneContact().addNewNumber();
						numberArry1.setAreaCode(areacode);
						numberArry1.setStringValue(telephone);
					}
				}
				//联系人信息
				Table contactsTable = commandRet.getParm("contacts").getTableColumn();
				if (contactsTable != null && contactsTable.getRowCount() > 0) {
					if(!flag){
						contactsArry = passengerArry.addNewContacts();
					}
					//姓
					String contactLastname = contactsTable.getRow(0).getColumn("name").getStringColumn();
					//名
					String contactFirstname = contactsTable.getRow(0).getColumn("firstname").getStringColumn();
					//邮箱地址
					String contactEmail = contactsTable.getRow(0).getColumn("email").getStringColumn();
					//联系电话
					String contactTelephone = contactsTable.getRow(0).getColumn("telephone").getStringColumn();
					//区号
					String areacode2 = contactsTable.getRow(0).getColumn("contactprefix").getStringColumn();
					Contact contactArry2 = contactsArry.addNewContact();
					if (StringUtils.hasLength(contactTelephone) || StringUtils.hasLength(contactEmail) 
							|| StringUtils.hasLength(contactLastname) || StringUtils.hasLength(contactFirstname)) {
						contactArry2.addNewContactType().setStringValue("CONTACT");
						org.iata.iata.edist.PhoneType.Number numberArry = contactArry2.addNewPhoneContact().addNewNumber();
						if (StringUtils.hasLength(contactTelephone)) {
							numberArry.setAreaCode(areacode2);
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
				if(StringUtils.hasLength(passtype) && StringUtils.hasLength(passno)){
					FOID fpidArry = passengerArry.addNewPassengerIDInfo().addNewFOID();
					fpidArry.setType(passtype);
					fpidArry.addNewID().setStringValue(passno);
				}
				paxNum++;
			}
		}
	}
	
	/**
	 * 将航班信息放入RS-XML
	 * @param mapping
	 * @param flightArry
	 * @throws ParseException 
	 */
	private void addFlightList(Mapping mapping,FlightSegmentList flightArry,CommandRet commandRet) throws ParseException{
		Table flightsTable = commandRet.getParm("flights").getTableColumn();
		Map<String,Row> flightsMap = new HashMap<String,Row>();
		int seg =1;
		for(Row flights:flightsTable){
			String flightid = flights.getColumn("id").getStringColumn();
			//航班编号
			String flightno = flights.getColumn("flightno").getStringColumn();
			flightno=DateUtils.setFlightNo(flightno);
			String segFlightno = SEG+seg;
			seg =seg+1;
			flights.addColumn(SEG,segFlightno);
			mapping.getAllMap().put(flightno, segFlightno);
			mapping.getAllMap().put(flightid, segFlightno);
			flightsMap.put(flightid, flights);
			ListOfFlightSegmentType fsArry = flightArry.addNewFlightSegment();
			fsArry.setSegmentKey(segFlightno);
			Departure DepartureArry = fsArry.addNewDeparture();
			//出发地三字码
			String oricode = flights.getColumn("oricode").getStringColumn();
			//出发日期
			String oridateH = flights.getColumn("oriDay").getStringColumn();
			//出发时间
			String oridateS = flights.getColumn("oriTime").getStringColumn();
			DepartureArry.addNewAirportCode().setStringValue(oricode);
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				DepartureArry.setDate(DateUtils.getInstance().parseDate(oridateH, "yyyy-MM-dd"));
				DepartureArry.setTime(sdf.format(oridateS));
			FlightArrivalType ftArry = fsArry.addNewArrival();
			//到达地三字码
			String destcode = flights.getColumn("destcode").getStringColumn();
			ftArry.addNewAirportCode().setStringValue(destcode);
			//到达日期
//			String destDateTime = flights.getColumn("destdate").getStringColumn();
//			if(destDateTime != null && !"".equals(destDateTime) && destDateTime.length() >= 10 ){
//				destDateTime = destDateTime.substring(0, 10);
//				Calendar destDate = DateUtils.getInstance().parseDate(destDateTime, "yyyy-MM-dd");
//				ftArry.addNewAirportCode().setStringValue(destcode);
//				ftArry.setDate(destDate);
//				//到达时间
//				String destTime = flights.getColumn("destTime").getStringColumn();
//				ftArry.setTime(sdf.format(destTime));
//			}
			MarketingCarrierFlightType mcfArry = fsArry.addNewMarketingCarrier();
			//承运航编号
			String carricd = flights.getColumn("airlinecd").getStringColumn();
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
		}
		mapping.setFlightsMap(flightsMap);
	}
	
	/**
	 * 添加辅营信息RS-xml
	 * @param commandRet
	 * @param eatList
	 */
	private void addServiceList(CommandRet commandRet,ServiceList serviceListArry,Map<String,List<Row>> subMap,Mapping mapping){
		Table submarketsTable = commandRet.getParm("submarkets").getTableColumn();
		for(Row submarkets:submarketsTable){
			//辅营代码
			String submarketcode = submarkets.getColumn("submarketcode").getStringColumn();
			//是否赠送(Y是 N否)[1E只显示购买的辅营]
			String isfree = submarkets.getColumn("isfree").getStringColumn();
			if("N".equals(isfree)){
				serSubmarketsMap(submarketcode, submarkets, subMap);
			}
		}
		for(Entry<String,List<Row>> freeEntry :subMap.entrySet()){
			//辅营代码
			String submarketcode = freeEntry.getKey();
			List<Row>  submarketList= freeEntry.getValue();
			if(null != submarketList && submarketList.size()>0){
				ServiceDetailType serviceArry = serviceListArry.addNewService();
				ServiceIDType addNewServiceID = serviceArry.addNewServiceID();
				addNewServiceID.setStringValue(submarketcode);
				addNewServiceID.setOwner(AIRLINECD);
				serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue("");
				serviceArry.setStatusCode("OK");
				for(Row submarkets:submarketList){
					String paxid = submarkets.getColumn("paxid").getStringColumn();
					String flightid = submarkets.getColumn("flightid").getStringColumn();
					int buynum = submarkets.getColumn("buynum").getIntegerColumn();
					buynum = buynum == 0?1:buynum;
					for(int i=0;i<buynum;i++){
						paxid = mapping.getAllMap().get(paxid);
						flightid = mapping.getAllMap().get(flightid);
						Associations assArry = serviceArry.addNewAssociations();
						assArry.addNewTraveler().setTravelerReferences(getList(paxid));
						assArry.addNewFlight().addNewSegmentReferences().setStringValue(flightid);
					}
				}
			}
		}
		
		/*for(Row submarkets:submarketsTable){
			//辅营id
//			String id = submarkets.getColumn("id").getStringColumn();
			//辅营品牌代码
			String familycode = submarkets.getColumn("familycode").getStringColumn();
			//辅营代码
			String submarketcode = submarkets.getColumn("submarketcode").getStringColumn();
			//辅营名称
			String submarketname = StatusUtil.getLanguageName(submarkets.getColumn("submarketname").getObjectColumn(),language);
			//辅营名称
//			String submarketname =submarkets.getColumn("submarketname").getStringColumn();
			//辅营描述
			String submarketdesc = submarkets.getColumn("submarketdesc").getStringColumn();
			String isfree = submarkets.getColumn("isfree").getStringColumn();
			String currencyCode = submarkets.getColumn("currencyCode").getStringColumn();
			BigDecimal price = submarkets.getColumn("price").getBigDecimalColumn();
			ServiceDetailType serviceArry = serviceListArry.addNewService();
			serviceArry.addNewServiceID().setStringValue(submarketcode);
			serviceArry.addNewName().setStringValue(submarketname);
			serviceArry.addNewEncoding().addNewCode().setStringValue(familycode);
			serviceArry.addNewFeeMethod().setStringValue(isfree);
			serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue(submarketdesc);
			CurrencyAmountOptType  priceArry = serviceArry.addNewPrice().addNewTotal();
			priceArry.setCode(currencyCode);
			priceArry.set(price);
			serviceArry.addNewBookingInstructions().addNewSSRCode().setStringValue(submarketcode);
			//旅客id
			String paxid = submarkets.getColumn("paxid").getStringColumn();
			serSubmarketsMap(paxid, submarkets, subMap);
		}*/
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
	 * 座位信息
	 * @param commandRet
	 * @param seatListArry
	 * @param mapping
	 */
	private void addSeatList(CommandRet commandRet,SeatList seatListArry,Mapping mapping){
		Table seatsTable = commandRet.getParm("seats").getTableColumn();
		if(null !=seatsTable && seatsTable.getRowCount()>0){
			for(Row seat:seatsTable){
				String seatno =seat.getColumn("seatno").getStringColumn();
				String paxid =seat.getColumn("paxid").getStringColumn();
				String flightid =seat.getColumn("flightid").getStringColumn();
				paxid = mapping.getAllMap().get(paxid);
				flightid = mapping.getAllMap().get(flightid);
				ListOfSeatType seatArry = seatListArry.addNewSeats();
				seatArry.setStatusCode("OK");
				SeatLocationType locaArry = seatArry.addNewLocation();
				locaArry.setColumn(seatno.substring(seatno.length()-1));
				locaArry.addNewRow().addNewNumber().setStringValue(seatno.substring(0, seatno.length()-1));
				OrderItemAssociationType assArry =locaArry.addNewAssociations();
				assArry.addNewPassengers().addNewPassenger().setPassengerAssociation(paxid);
				assArry.addNewFlight().addNewSegmentReferences().setStringValue(flightid);
			}
		}
	}
	
	/**
	 * 
	 * @param commandRet
	 * @param OrderItemsArry
	 * @param mapping
	 */
	private void addPaxflight(CommandRet commandRet,OrderItems OrderItemsArry,Mapping mapping,
			Map<String,List<Row>> submarketMap ){
		Table paxflights = commandRet.getParm("paxflights").getTableColumn();
		int flt =1;
		for(Row paxflight:paxflights){
			PNR = paxflight.getColumn("pnr").getStringColumn();
			//航段id
			String id = paxflight.getColumn("id").getStringColumn();
			//航班id
			String flightid = paxflight.getColumn("flightid").getStringColumn();
			//航班数据
			Row flights = mapping.getFlightsMap().get(flightid);
			OrderItem  orderItemArry =	OrderItemsArry.addNewOrderItem();
			ItemIDType ItemIDArry = orderItemArry.addNewOrderItemID();
			//销售航司二字码
			String airlinecd = flights.getColumn("airlinecd").getStringColumn();
			ItemIDArry.setOwner(airlinecd);
			AIRLINECD=airlinecd;
			ItemIDArry.setStringValue(id);
//			Row Seats = mapping.getSeatsMap().get(flightid);
//			if(null!=Seats){
//				String price  = Seats.getColumn("price").getStringColumn();
//				String currencyCode  = Seats.getColumn("currencyCode").getStringColumn();
//				String seatno  = Seats.getColumn("seatno").getStringColumn();
//				SeatItem seatItemArry = orderItemArry.addNewSeatItem();
//				CurrencyAmountOptType  totalArry = seatItemArry.addNewPrice().addNewTotal();
//				totalArry.setCode(currencyCode);
//				totalArry.setStringValue(price);
//				String column =seatno.substring(seatno.length()-1,seatno.length());
//				String number =seatno.substring(seatno.length()-1);
//				SeatLocationType locArry =seatItemArry.addNewLocation();
//				locArry.setColumn(column);
//				locArry.addNewRow().addNewNumber().setStringValue(number);
//			}
			//去程信息
			Flight flightArry = orderItemArry.addNewFlightItem().addNewOriginDestination().addNewFlight();
			//航班号
			String flightno = flights.getColumn("flightno").getStringColumn();
			flightno=DateUtils.setFlightNo(flightno);
			flightArry.setRefs(getList(mapping.getAllMap().get(flightid)));
			flt =flt+1;
			Departure departureArry = flightArry.addNewDeparture();
			//出发机场三字码
			String oricode = flights.getColumn("oricode").getStringColumn();
			//出发时间
			Date oridate = flights.getColumn("oridate").getDateColumn();
			departureArry.addNewAirportCode().setStringValue(oricode);
			Calendar orical = Calendar.getInstance();
			orical.setTime(oridate);
			departureArry.setDate(orical);
			//到达机场三字码
			String destcode = flights.getColumn("destcode").getStringColumn();
			flightArry.addNewArrival().addNewAirportCode().setStringValue(destcode);
			MarketingCarrierFlightType mcfArry = flightArry.addNewMarketingCarrier();
			//承运航司二字码
			/*String carricd = flights.getColumn("carricd").getStringColumn();*/
			//承运航班号
			/*String carriflightno = flights.getColumn("carriflightno").getStringColumn();*/
			mcfArry.addNewAirlineID().setStringValue(airlinecd);
			FlightNumber fnArry = mcfArry.addNewFlightNumber();
			if (flightno.substring(flightno.length()-1).matches("[A-Z]")) {
				//航班后缀
				fnArry.setOperationalSuffix(flightno.substring(flightno.length()-1));
				fnArry.setStringValue(flightno.substring(0,flightno.length()-1));
			} else {
				fnArry.setOperationalSuffix("");
				fnArry.setStringValue(flightno);
			}
			//旅客与辅营关联数据
			//旅客id
			String paxid = paxflight.getColumn("paxid").getStringColumn();
			String paxNum = mapping.getAllMap().get(paxid);
			org.iata.iata.edist.OrderItemCoreType.OrderItem.Associations assArry = orderItemArry.addNewAssociations();
			//旅客编号
			assArry.addNewPassengers().setPassengerReferences(getList(paxNum));
			/*List<Row> submarketList =submarketMap.get(paxid);*/
			/*org.iata.iata.edist.BaggageItemType.Services  baggageArry =  orderItemArry.addNewBaggageItem().addNewServices();
			org.iata.iata.edist.OtherItemType.Services  otherArry = orderItemArry.addNewOtherItem().addNewServices();*/
			/*String ids ="";
			if(submarketList != null && submarketList.size() > 0){
				for(Row  submarkets:submarketList){
					String isfree =	submarkets.getColumn("isfree").getStringColumn();
					String submarkettype = submarkets.getColumn("submarkettype").getStringColumn();
					String submarketid = submarkets.getColumn("submarketcode").getStringColumn();
					if("Y".equals(isfree)){
						ids=submarketid+" "+ids;
					}else{
						if("BAG".equals(submarkettype)){
							baggageArry.addNewServiceID().setStringValue(submarketid);
						}else{
							otherArry.addNewServiceID().setStringValue(submarketid);
						}
					}
					
				}
				mapping.getServiceBundleMap().put(id, ids);
				assArry.addNewIncludedService().setBundleReference(id);
			}*/
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
	 * 航班于OD关联
	 * @param commandRet
	 * @param flightListArry
	 * @param originArry
	 */
	public void  addOriginArryArry(CommandRet commandRet ,FlightList flightListArry,OriginDestinationList originArry,Mapping mapping){
		Table flightsTable = commandRet.getParm("flights").getTableColumn();
		String oricode ="";
		String destcode ="";
		String routtypeS ="";
		String routtypeR ="";
		int flt =1;
		for(Row flights:flightsTable){
			org.iata.iata.edist.DataListType.FlightList.Flight  flightArry = flightListArry.addNewFlight();
			//出发地三字码
			 oricode = flights.getColumn("oricode").getStringColumn();
			//到达地三字码
			 destcode = flights.getColumn("destcode").getStringColumn();
			//航班编号
			String flightno = flights.getColumn("flightno").getStringColumn();
			flightno=DateUtils.setFlightNo(flightno);
			String routtype = flights.getColumn("routtype").getStringColumn();
			flightArry.setFlightKey(FLT+flt);
			flightArry.addNewSegmentReferences().setStringValue(mapping.getAllMap().get(flightno));
			if("G".equals(routtype)){
				routtypeS =FLT+flt+" "+routtypeS;
			}else{
				routtypeR =FLT+flt+" "+routtypeR;
			}
			flt=flt+1;
		}
		if("".equals(routtypeR)){
			OriginDestination  originDestinationArry = originArry.addNewOriginDestination();
			originDestinationArry.addNewDepartureCode().setStringValue(oricode);
			originDestinationArry.addNewArrivalCode().setStringValue(destcode);
			originDestinationArry.addNewFlightReferences().setStringValue(routtypeS.trim());
		}else{
			OriginDestination  originDestinationArry1 = originArry.addNewOriginDestination();
			originDestinationArry1.addNewDepartureCode().setStringValue(oricode);
			originDestinationArry1.addNewArrivalCode().setStringValue(destcode);
			originDestinationArry1.addNewFlightReferences().setStringValue(routtypeS.trim());
			OriginDestination  originDestinationArry2 = originArry.addNewOriginDestination();
			originDestinationArry2.addNewDepartureCode().setStringValue(destcode);
			originDestinationArry2.addNewArrivalCode().setStringValue(oricode);
			originDestinationArry2.addNewFlightReferences().setStringValue(routtypeR.trim());
		}
	}
	
	public void  addSeats(CommandRet commandRet,Mapping mapping){
		Table seatsTable = commandRet.getParm("seats").getTableColumn();
		if(null !=seatsTable){
			for(Row seats:seatsTable){
				mapping.getSeatsMap().put(seats.getColumn("flightid").getStringColumn(), seats);
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
			error.setShortText(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
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
