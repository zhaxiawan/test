package com.travelsky.quick.util.helper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirShopFlightSpecificType.FlightSegment;
import org.iata.iata.edist.AirShopReqAttributeQueryType.OriginDestination;
import org.iata.iata.edist.AirShopReqParamsType;
import org.iata.iata.edist.AirShopReqParamsType.Inventory;
import org.iata.iata.edist.AirShoppingRQDocument;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ.CoreQuery;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ.Preference;
import org.iata.iata.edist.AirShoppingRSDocument;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.DataLists;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers.AirlineOffer;
import org.iata.iata.edist.AlertsType;
import org.iata.iata.edist.AlertsType.Alert;
import org.iata.iata.edist.AnonymousTravelerListDocument.AnonymousTravelerList;
import org.iata.iata.edist.ApplicableFlightDocument.ApplicableFlight;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.CabinTypeDocument.CabinType;
import org.iata.iata.edist.DataListType.FlightList;
import org.iata.iata.edist.DataListType.FlightList.Flight;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DataListType.OriginDestinationList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.AnonymousTravelerType;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.ErrorsType;
import org.iata.iata.edist.FareComponentType;
import org.iata.iata.edist.FareDetailType;
import org.iata.iata.edist.FarePriceDetailType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightArrivalType.Terminal;
import org.iata.iata.edist.FlightCOSCoreType;
import org.iata.iata.edist.FlightCOSCoreType.Code;
import org.iata.iata.edist.FlightCOSCoreType.MarketingName;
import org.iata.iata.edist.FlightDetailType;
import org.iata.iata.edist.FlightDepartureType.AirportCode;
import org.iata.iata.edist.FlightDetailType.Stops;
import org.iata.iata.edist.FlightDistanceType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.FlightPreferencesType.Characteristic;
import org.iata.iata.edist.FlightSegmentReferenceDocument.FlightSegmentReference;
import org.iata.iata.edist.InvDiscrepencyAlertType;
import org.iata.iata.edist.ItemIDType;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.OfferPriceLeadType.RequestedDate;
import org.iata.iata.edist.OrdViewProcessType;
import org.iata.iata.edist.PricedFlightOfferAssocType;
import org.iata.iata.edist.PricedFlightOfferType.OfferPrice;
import org.iata.iata.edist.PricedOfferDocument.PricedOffer;
import org.iata.iata.edist.SegmentReferencesDocument.SegmentReferences;
import org.iata.iata.edist.StopLocationType;
import org.iata.iata.edist.StopLocationType.StopLocation;
import org.iata.iata.edist.TaxDetailType.Breakdown;
import org.iata.iata.edist.TaxDetailType.Breakdown.Tax;
import org.iata.iata.edist.TransferPreferencesType.Connection;
import org.iata.iata.edist.TravelerCoreType.PTC;
import org.iata.iata.edist.TravelersDocument.Travelers.Traveler;
import org.iata.iata.edist.WarningType;
import org.iata.iata.edist.WarningsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
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
import com.travelsky.quick.util.StatusUtil;

/**
 * 品牌查询
 * @author lizhi
 *
 */
@Lazy
public class APIAirShoppingNDCONEE{
	/**
	 * 
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(APIAirShoppingNDCB2C.class);
	// 获取语言
	private String language = ApiServletHolder.getApiContext().getLanguage();
	/**
	 *旅客数量
	 *String 旅客类型   
	 *String 数量
	 */
	private  Map<String,String>  travelerMap= new  HashMap<String,String>();
	/**
	 *航班查询 航段与OD
	 *String OD标识 
	 *String 航班
	 */
	private  Map<String,String>  segmentMap= new  TreeMap<String,String>();
	/**
	 *退改规则
	 *String 规则id   
	 *CommandData 规则信息
	 */
	private  Map<String,CommandData>  refundMap= new  HashMap<String,CommandData>();
	/**
	 * 辅营信息
	 * String 免费服务 id
	 * Row 服务内容
	 */
	private  Map<String,Row>  freeMap= new  HashMap<String,Row>();
	/**
	 * od关联航班
	 */
	private  Map<String, StringBuffer> helpMap = new LinkedHashMap<String, StringBuffer>();
	//航班数据集合
	private  List<CommandRet> brandinfoList =new ArrayList<CommandRet>();
	//Redis缓存限制时间（单位 秒）
	private static final int TIME = 2400;
	// shopping请求ID
	private static final String SHOPPINGAPI = "SHOPPING.API.";
	// offerID
	private static final String OFFERID = "OFFERID.";
	// 去程标记符
	private static final String OW = "OW";
	// 标记符
	private static final String DA = "DA";
	// 成人标记符
	private static final String ADT = "ADT";
	// 婴儿标记符
	private static final String INF = "INF";
	// 儿童标记符
	private static final String CHD = "CHD";
	//航班排序
	private int SEG = 1;
	private int FLTNUM = 1;
	private int FLTNO = 1;
	private int MAXPASSENGERNUM = 9;
	private String guaranteeNum= "";

	
	public void doServletDA(SelvetContext<ApiContext> context) throws Exception {
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean(context);
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_SHOPPING, ApiServletHolder
							.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	/**
	 * xml-->Reqbean
	 * @throws APIException
	 * @throws Exception
	 */
	public  void transInputXmlToRequestBean(SelvetContext<ApiContext> context) throws APIException, Exception {
		CommandData input = context.getInput();
		String xmlInput = context.getContext().getReqXML();
		AirShoppingRQDocument rootDoc = AirShoppingRQDocument.Factory.parse(xmlInput);
		AirShoppingRQ shoppingRQ = rootDoc.getAirShoppingRQ();
		AirShopReqParamsType parameters = shoppingRQ.getParameters();
		Inventory  InventoryArry =  parameters == null? null : parameters.getInventory();
		if(InventoryArry.getGuaranteeInd()){
		//	APICacheHelper.setDeptInfo(context, shoppingRQ.getParty());
			Traveler[] traveler = shoppingRQ.getTravelers().getTravelerArray();
			if(null !=traveler){
				for(Traveler t:traveler){
					PTC pct =t.getAnonymousTravelerArray(0).getPTC();
					travelerMap.put(pct.getStringValue(), pct.getQuantity().toString());
					BigInteger bigInteger = pct.getQuantity();
					guaranteeNum = pct.getQuantity().toString();
					if (!StringUtils.hasLength(guaranteeNum)) {
						LOGGER.info(TipMessager.getInfoMessage(
								ErrCodeConstants.API_NULL_ADT_NUM, language));
						throw APIException
								.getInstance(ErrCodeConstants.API_NULL_ADT_NUM);
					}
//					else if(bigInteger.intValue() > MAXPASSENGERNUM){
//						LOGGER.info(TipMessager.getInfoMessage(
//								ErrCodeConstants.API_TOTAL_PASSENGETSNUM_NOT_MORETHAN_NINE, language));
//						throw APIException
//						.getInstance(ErrCodeConstants.API_TOTAL_PASSENGETSNUM_NOT_MORETHAN_NINE);
//					}
				}
			}
		}else{
			guaranteeNum ="1";
			travelerMap.put("ADT",guaranteeNum);
		}
		input.addParm("gtNum", guaranteeNum);
		CoreQuery  coreQueryArry = shoppingRQ.getCoreQuery();
		//shopping(航班号)查询数据
		FlightSegment[] flightSegment =	coreQueryArry.getFlightSpecific() == null ? null:coreQueryArry.getFlightSpecific().getFlightSegmentArray();
		if(null !=flightSegment){
			getCoreShoppingFlightNO(flightSegment,input);
		}
		OriginDestination[] originDestinations = coreQueryArry.getOriginDestinations()== null ? null:coreQueryArry.getOriginDestinations().getOriginDestinationArray();
		if(null !=originDestinations){
			Preference preferenceArry = shoppingRQ.getPreference();
			getCoreShopping(originDestinations,preferenceArry,input);
		}
	}
	
	/**
	 * 转换ResponseBean-->XmlBean
	 * @param commandRet
	 * @param input
	 * @param b 
	 * @return
	 */
	public  XmlObject transRespBeanToXmlBeanONEE(CommandRet commandRet, CommandData input, boolean b) {
		CommandRet xmlOutput = commandRet;
		AirShoppingRSDocument doc = AirShoppingRSDocument.Factory.newInstance();
		AirShoppingRS rs = doc.addNewAirShoppingRS();
		int i = commandRet.getParm("int").getIntegerColumn();
		try{
			//生成一个shopping请求id 格式：（SHOPPING.API.）+shoppingID
			String shoppingID =SHOPPINGAPI+UUID.randomUUID().toString().toUpperCase();
			rs.addNewDocument();
			rs.addNewSuccess();
			//ShoppingResponseID——》ResponseID
			rs.addNewShoppingResponseID().addNewResponseID().setStringValue(shoppingID);
			//OffersGroup——》AirlineOffers
			AirlineOffers  airlineOffers = rs.addNewOffersGroup().addNewAirlineOffers();
			/****************************************************************/
			DataLists dataListsArry = rs.addNewDataLists();
			//旅客人数
			AnonymousTravelerList travelerList = dataListsArry.addNewAnonymousTravelerList();
			AnonymousTravelerType traveler = travelerList.addNewAnonymousTraveler();
			PTC ptc = traveler.addNewPTC();
			ptc.setQuantity(BigInteger.valueOf(input.getParm("gtNum").getIntegerColumn()));
			ptc.setStringValue("ADT");
			//航班信息  DataLists——》FlightSegmentList
			FlightSegmentList flightSegmentListArry = dataListsArry.addNewFlightSegmentList();
			//去程回程 关联航班信息  DataLists——》FlightList
			FlightList  flightListArry = dataListsArry.addNewFlightList();
			//航班查询的出发地 DataLists——》OriginDestinationList
			OriginDestinationList originArry = dataListsArry.addNewOriginDestinationList();
			//品牌信息DataLists——》ListOfPriceClassType
			/*ListOfPriceClassType priceClassSArry = dataListsArry.addNewPriceClassList();*/
			/****************************************************************/
			CommandRet retOW = new CommandRet("");
			StringBuffer fltS =new StringBuffer();
			CommandRet offerRet = new CommandRet("");
			if (b) {
				if (i>0) {
					WarningsType warnings = rs.addNewWarnings();
					OrdViewProcessType processing = rs.addNewAirShoppingProcessing();
					AlertsType addNewAlerts = processing.addNewAlerts();
					for(String segmentKey :segmentMap.keySet()){
						if (xmlOutput.getParm(segmentKey).getObjectColumn() != null) {
							retOW = (CommandRet) xmlOutput.getParm(segmentKey).getObjectColumn();
							//查询去程是否有误
							if (!"".equals(retOW.getErrorCode())) {
								WarningType addNewWarning = warnings.addNewWarning();
								addNewWarning.setCode(TipMessager.getErrorCode(retOW.getErrorCode()));
								addNewWarning.setShortText(TipMessager.getMessage(retOW.getErrorCode(),
										ApiServletHolder.getApiContext().getLanguage()));
								Alert addNewAlert = addNewAlerts.addNewAlert();
								InvDiscrepencyAlertType discrepancyAlert = addNewAlert.addNewInventoryDiscrepancyAlert();
								discrepancyAlert.setNoInventoryInd(true);
								List<String> segList=new ArrayList<String>();
								segList.add("SEG"+SEG);
								discrepancyAlert.setRefs(segList);
								/*因为 1E 需要offID 来知道它的品牌+仓位 下的具体详情*/
								AirlineOffer airlineOffer = airlineOffers.addNewAirlineOffer();
								String airCode = retOW.getParm("free").getObjectColumn().getParm("airlineCode").getStringColumn();
								ItemIDType offerID = airlineOffer.addNewOfferID();
								offerID.setStringValue(shoppingID);
								offerID.setOwner(airCode);
								OfferPrice pricedOffer = airlineOffer.addNewPricedOffer().addNewOfferPrice();
								String cabin = retOW.getParm("free").getObjectColumn().getParm("cabinClass").getStringColumn();
								pricedOffer.addNewOfferItemID().setStringValue("");
								ApplicableFlight cableFlight = pricedOffer.addNewRequestedDate().addNewAssociations().addNewApplicableFlight();
								FlightSegmentReference flightSeg= cableFlight.addNewFlightSegmentReference();
								flightSeg.setRef("SEG"+SEG);
								FlightCOSCoreType ClassOfSer = flightSeg.addNewClassOfService();
								Code code = ClassOfSer.addNewCode();
								code.setStringValue(cabin);
								//尝试排序
								Flight addNewFlight = flightListArry.addNewFlight();
								addNewFlight.setFlightKey("FLT"+SEG);
								SegmentReferences addNewSegmentReferences = addNewFlight.addNewSegmentReferences();
								addNewSegmentReferences.setStringValue("SEG"+SEG);
								String oriCode = retOW.getParm("free").getObjectColumn().getParm("depart").getStringColumn();
								String destCode = retOW.getParm("free").getObjectColumn().getParm("arrive").getStringColumn();
								String helpKey = oriCode+" "+destCode;
								//重复的key值 兼容
								if(!helpMap.containsKey(helpKey)){
									StringBuffer newFltS = new StringBuffer("");
									helpMap.put(helpKey, newFltS.append("FLT"+SEG+" "));
								}else{
									StringBuffer oldFltS = helpMap.get(helpKey).append("FLT"+SEG+" ");
									helpMap.put(helpKey, oldFltS);
								}
								//FlightSegmentList
								ListOfFlightSegmentType flightSegment = flightSegmentListArry.addNewFlightSegment();
								flightSegment.setSegmentKey("SEG"+SEG);
								Departure addNewDeparture = flightSegment.addNewDeparture();
								addNewDeparture.addNewAirportCode().setStringValue(oriCode);
								String deptdate =retOW.getParm("free").getObjectColumn().getParm("departdate").getStringColumn();
								Calendar oridate = DateUtils.getInstance().parseDate(deptdate, "yyyyMMdd");
								addNewDeparture.setDate(oridate);
								addNewDeparture.setAirportName("");
								org.iata.iata.edist.FlightDepartureType.Terminal addNewTerminal = addNewDeparture.addNewTerminal();
								addNewTerminal.setName("");
								FlightArrivalType addNewArrival = flightSegment.addNewArrival();
								addNewArrival.addNewAirportCode().setStringValue(destCode);
								String destdate =retOW.getParm("free").getObjectColumn().getParm("destdate").getStringColumn();
								if (destdate!=null && !"".equals(destdate)) {
									Calendar desdate = DateUtils.getInstance().parseDate(destdate, "yyyyMMdd");
									addNewArrival.setDate(desdate);
								}
								addNewArrival.setAirportName("");
								Terminal addNewTerminal2 = addNewArrival.addNewTerminal();
								addNewTerminal2.setName("");
								MarketingCarrierFlightType addNewMarketingCarrier = flightSegment.addNewMarketingCarrier();
								String airlinecode = retOW.getParm("free").getObjectColumn().getParm("airlineCode").getStringColumn();
								addNewMarketingCarrier.addNewAirlineID().setStringValue(airlinecode);
								String flightno = retOW.getParm("free").getObjectColumn().getParm("flightNo").getStringColumn();
								FlightNumber addNewFlightNumber = addNewMarketingCarrier.addNewFlightNumber();
								addNewFlightNumber.setStringValue(DateUtils.setFlightNo(flightno));
								String suffix = retOW.getParm("free").getObjectColumn().getParm("suffix").getStringColumn();
								addNewFlightNumber.setOperationalSuffix(suffix);
								SEG++;
							}else {
								//将去程航班数据放入CommandRet里
								setRedisManagerflights(retOW,shoppingID+"."+OW,"G"
										,airlineOffers,offerRet,flightSegmentListArry,flightListArry,fltS,helpMap,b);
							}
						}
					}
				}else{
					for(String segmentKey :segmentMap.keySet()){
						if (xmlOutput.getParm(segmentKey).getObjectColumn() != null) {
							retOW = (CommandRet) xmlOutput.getParm(segmentKey).getObjectColumn();
							//查询去程是否有误
							if (!"".equals(retOW.getErrorCode())) {
								AirShoppingRSDocument docOW = AirShoppingRSDocument.Factory.newInstance();
								AirShoppingRS rsOW = docOW.addNewAirShoppingRS();
								ErrorType error = rsOW.addNewErrors().addNewError();
								error.setCode(TipMessager.getErrorCode(retOW.getErrorCode()));
								error.setShortText(TipMessager.getMessage(retOW.getErrorCode(),
										ApiServletHolder.getApiContext().getLanguage()));
								return docOW;
							}
							//将去程航班数据放入CommandRet里
							setRedisManagerflights(retOW,shoppingID+"."+OW,"G"
									,airlineOffers,offerRet,flightSegmentListArry,flightListArry,fltS,helpMap,b);
						}
					}
				}
			}else {
				for(String segmentKey :segmentMap.keySet()){
					if (xmlOutput.getParm(segmentKey).getObjectColumn() != null) {
						retOW = (CommandRet) xmlOutput.getParm(segmentKey).getObjectColumn();
						//查询去程是否有误
						if (!"".equals(retOW.getErrorCode())) {
							AirShoppingRSDocument docOW = AirShoppingRSDocument.Factory.newInstance();
							AirShoppingRS rsOW = docOW.addNewAirShoppingRS();
							ErrorType error = rsOW.addNewErrors().addNewError();
							error.setCode(TipMessager.getErrorCode(retOW.getErrorCode()));
							error.setShortText(TipMessager.getMessage(retOW.getErrorCode(),
									ApiServletHolder.getApiContext().getLanguage()));
							return docOW;
						}
						//将去程航班数据放入CommandRet里
						setRedisManagerflights(retOW,shoppingID+"."+OW,"G"
								,airlineOffers,offerRet,flightSegmentListArry,flightListArry,fltS,helpMap,b);
					}
				}
			}
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey(shoppingID+"."+OW), JsonUnit.toJson(offerRet), TIME);
			StringBuffer flightReferences = null;
			String[] strings = null;
			/*OD与行程的关联****************************************************************/
			if(helpMap != null){
				for (String string : helpMap.keySet()) {
					strings = string.split(" ");
					flightReferences = helpMap.get(string);
					org.iata.iata.edist.OriginDestinationDocument.OriginDestination  od = originArry.addNewOriginDestination();
					od.addNewDepartureCode().setStringValue(strings[0]);
					od.addNewArrivalCode().setStringValue(strings[1]);
					od.addNewFlightReferences().setStringValue(flightReferences.toString().trim());
				}
			}
		}
		catch (Exception e) {
			doc = AirShoppingRSDocument.Factory.newInstance();
			rs = doc.addNewAirShoppingRS();
			commandRet.setError(ErrCodeConstants.API_SYSTEM,
					TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
							ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}
	
	
	/**
	 *  shopping查询数据
	 * @param originDestination 航班数据
	 * @param input  请求参数添加
	 * @throws APIException 
	 */
	public  void getCoreShopping(OriginDestination[] originDestination,Preference preferenceArry,CommandData input) throws APIException{
		SimpleDateFormat adf = DateUtils.getInstance().getSimDate("yyyyMMdd");
		CommandData commandData = getCreateCommandData();
		AirportCode dacodeArry = originDestination[0].getDeparture().getAirportCode();
		//出发地三字码
		String depart =dacodeArry.getStringValue();
		if (!StringUtils.hasLength(depart)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_FLIGHT_ORG, language));
			throw APIException
			.getInstance(ErrCodeConstants.API_NULL_FLIGHT_ORG);
		}
		//出发时间
		Date deptdate = null;
		if (originDestination[0] != null && originDestination[0].getDeparture() != null) {
			try {
				 originDestination[0].getDeparture().getDate();
			} catch (Exception e) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME, language));
				throw APIException.getInstance(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME);

			}
			if (originDestination[0].getDeparture().getDate()==null) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHTORG_TIME, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHTORG_TIME);
			}else {
				deptdate=originDestination[0].getDeparture().getDate().getTime();
			}
		}
		org.iata.iata.edist.FlightArrivalType.AirportCode aacode = originDestination[0].getArrival().getAirportCode();
		String arrive = aacode.getStringValue();
		if (!StringUtils.hasLength(arrive)) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_FLIGHT_DST, language));
			throw APIException
			.getInstance(ErrCodeConstants.API_NULL_FLIGHT_DST);
		}
		// 出发时间
		commandData.addParm("deptdate", adf.format(deptdate));
		// 出发地
		commandData.addParm("depart", depart);
		// 目的地
		commandData.addParm("arrive", arrive);
		commandData.addParm("adt", travelerMap.get(ADT));
		commandData.addParm("chd", travelerMap.get(CHD));
		commandData.addParm("inf", travelerMap.get(INF));
		commandData.addParm("isoCode", input.getParm("isoCode").toString());
		/************************************************/
		if(null != preferenceArry ){
			if(null != preferenceArry.getFlightPreferences() 
					&& null != preferenceArry.getFlightPreferences().getCharacteristic()){
				Characteristic ctArry = preferenceArry.getFlightPreferences().getCharacteristic();
				//是否只查经停(0表示不包含经停，1 表示包含经停)
				if(null != ctArry.getNonStopPreferences() && 
						null != ctArry.getNonStopPreferences().getStringValue()){
					commandData.addParm("nonStop", ctArry.getNonStopPreferences().getStringValue());
				}
				//是否只查直达(0表示查所有，1 表示仅查询直达)
				if(null != ctArry.getDirectPreferences() && 
						null != ctArry.getDirectPreferences().getStringValue()){
					commandData.addParm("nonStop", ctArry.getNonStopPreferences().getStringValue());
					String direct = ctArry.getDirectPreferences().getStringValue();
					commandData.addParm("direct", direct);
				}
			}
			if(null != preferenceArry.getTransferPreferences() 
					&& null != preferenceArry.getTransferPreferences().getConnection()){
				Connection  connectionArry = preferenceArry.getTransferPreferences().getConnection();
				//指定出发机场的三字码
				org.iata.iata.edist.TransferPreferencesType.Connection.Codes.Code[] codeTable = connectionArry.getCodes().getCodeArray();
				if(null !=codeTable && codeTable.length >0 ){
					commandData.addParm("connection", codeTable[0].getStringValue());
					//经停次数
					long maxNumber = connectionArry.getMaxNumber().longValue();
					commandData.addParm("maxNumber", maxNumber);
				}
			}
			if(null != preferenceArry.getCabinPreferences() 
					&& null != preferenceArry.getCabinPreferences().getCabinTypeArray()){
				CabinType[] cabinTabe = preferenceArry.getCabinPreferences().getCabinTypeArray();
				if(null != cabinTabe&&cabinTabe.length>0){
					//舱位
					String cabinType = cabinTabe[0].getCode();
					commandData.addParm("cabinType", cabinType);
				}
			}
		}
		segmentMap.put("DA", "DA");
		input.addParm(DA, commandData);
	}
	
	/**
	 * OD查询的
	 * @param originDestinations
	 * @param input
	 * @throws APIException
	 */
	public  void getCoreShoppingFlightNO(OriginDestination[] originDestinations,CommandData input) throws APIException{
		Table freeSTable = new Table(new String[]{"deptdate","depart","arrive","code","flightno","cabin","suffix",
				"adt","chd","inf","isoCode","tktdeptid","segment","guaranteeNum","destdate"});
		SimpleDateFormat adf = DateUtils.getInstance().getSimDate("yyyyMMdd");
		for(OriginDestination odArry :originDestinations){
			Row frees = freeSTable.addRow();
			//出发地三字码
			String depart =odArry.getDeparture().getAirportCode().getStringValue();
			//出发时间
			Date deptdate = null;
			if (odArry != null && odArry.getDeparture() != null) {
				try {
					odArry.getDeparture().getDate();
				} catch (Exception e) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME);

				}
				if (odArry.getDeparture().getDate()==null) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHTORG_TIME);
				}else {
					deptdate=odArry.getDeparture().getDate().getTime();
				}
			}
			Date destdate = null;
			if (odArry != null && odArry.getArrival() != null) {
				try {
					odArry.getArrival().getDate();
				} catch (Exception e) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME);

				}
				if (odArry.getArrival().getDate()==null) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHTORG_TIME);
				}else {
					destdate=odArry.getArrival().getDate().getTime();
				}
			}
			//目标地字节码标记
			String arrive =odArry.getArrival().getAirportCode().getStringValue();
			// 出发时间
			frees.addColumn("deptdate", adf.format(deptdate));
			//到达时间
			frees.addColumn("destdate", adf.format(destdate));
			// 出发地
			frees.addColumn("depart", depart);
			// 目的地
			frees.addColumn("arrive", arrive);
			frees.addColumn("adt", travelerMap.get(ADT));
			frees.addColumn("chd", travelerMap.get(CHD));
			frees.addColumn("inf", travelerMap.get(INF));
			// 部门ID
			String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
			input.addParm("tktdeptid", deptno);
			frees.addColumn("tktdeptid", deptno);
			frees.addColumn("guaranteeNum",guaranteeNum);
			int segment = 0;
			frees.addColumn("segment", segment);
			segmentMap.put(String.valueOf(segment), "ONE");
		}
		input.addParm(DA, freeSTable);
	}
	
	/**
	 * 指定航班的
	 * @param flightSegmentS
	 * @param input
	 * @throws APIException
	 */
	public  void getCoreShoppingFlightNO(FlightSegment[] flightSegmentS,CommandData input) throws APIException{
		SimpleDateFormat adf = DateUtils.getInstance().getSimDate("yyyyMMdd");
		Table freeSTable = new Table(new String[]{"deptdate","depttime","desttime","depart","arrive","code","flightno","cabin","suffix",
				"adt","chd","inf","isoCode","tktdeptid","segment","guaranteeNum","destdate"});
		for(FlightSegment fsArry :flightSegmentS){
			Row frees = freeSTable.addRow();
			/*出发数据******************************************/
			int segment = fsArry.getSegmentKey();
			//出发地三字码
			String depart =fsArry.getDeparture().getAirportCode().getStringValue();
			//出发日期
			Date deptdate = fsArry.getDeparture().getDate().getTime();
			/*到达数据******************************************/
			//目标地三字码
			String arrive = fsArry.getArrival().getAirportCode().getStringValue();
			//到达日期
			if (fsArry.getArrival().getDate()!=null) {
				Date destdate = fsArry.getArrival().getDate().getTime();
				frees.addColumn("destdate", adf.format(destdate));
			}
			//航空公司二字码
			String code =fsArry.getMarketingAirline().getAirlineID().getStringValue();
			//航班号
			String flightno = fsArry.getMarketingAirline().getFlightNumber().getStringValue();
			flightno=DateUtils.getFlightNo(flightno);
			//航班后缀
			String suffix = fsArry.getMarketingAirline().getFlightNumber().getOperationalSuffix();
			//舱位
			String cabin = fsArry.getMarketingAirline().getResBookDesigCode();
			//出发日期
			frees.addColumn("deptdate", adf.format(deptdate));
			//出发地
			frees.addColumn("depart", depart);
			// 目的地
			frees.addColumn("arrive", arrive);
			//航空公司二字码
			frees.addColumn("code", code);
			//品牌编号
			frees.addColumn("flightno", flightno);
			//舱位
			if(null!=cabin && !"".equals(cabin)){
				frees.addColumn("cabin", cabin);
				frees.addColumn("guaranteeNum",guaranteeNum);
			}
			//航班后缀
			frees.addColumn("suffix", suffix);
			frees.addColumn("adt", travelerMap.get(ADT));
			frees.addColumn("chd", travelerMap.get(CHD));
			frees.addColumn("inf", travelerMap.get(INF));
			frees.addColumn("segment", segment);
			// 部门ID
			String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
			input.addParm("tktdeptid", deptno);
			frees.addColumn("tktdeptid", deptno);
			segmentMap.put(String.valueOf(segment), flightno);
		}
		input.addParm(DA, freeSTable);
	}
	
	/**
	 * 将航班数据放入CommandRet里
	 * @param ret shopping数据
	 * @param shoppingRet redis缓存command
	 * @param shoppingID 请求id
	 * @param routtype 往返标记
	 * @param airlineOffers 航班节点 OffersGroup——》AirlineOffers
	 * @param flightSegmentListArry 航班数据节点 DataLists——》FlightSegmentList
	 * @param flightListArry  去程回程 关联航班信息  DataLists——》FlightList
	 * @param b 
	 * @param originArry 航班查询的出发地 DataLists——》originArry
	 * @param priceClassSArry 品牌信息DataLists——》ListOfPriceClassType
	 * @throws ParseException 
	 */
	public  void  setRedisManagerflights(CommandRet retshopping,
		String shoppingID,String routtype,AirlineOffers airlineOffers,CommandRet offerRet,
		FlightSegmentList flightSegmentListArry,FlightList flightListArry
		,StringBuffer fltS,Map<String, StringBuffer> helpMap, boolean b) throws ParseException{
		if (b) {
			CommandData ret= retshopping.getParm("shoppingdata").getObjectColumn();
			Table  flightsTable = null;
			if(null == ret){
				flightsTable = retshopping.getParm("flights").getTableColumn();
			}else{
				flightsTable = ret.getParm("flights").getTableColumn();
			}
			String oricode = "";
			String destcode = "";
			//拼接航班编号
			StringBuffer segment =new StringBuffer();
			for(Row flightsRow :flightsTable){
				StringBuffer cabinCode = new StringBuffer();
				CommandRet offeritemRet = new CommandRet("");
				String flightDay = flightsRow.getColumn("flightDay").getStringColumn();
				/*//航班id
				String flightid = flightsRow.getColumn("flightid").getStringColumn();*/
				//出发机场三字码
				oricode = flightsRow.getColumn("oricode").getStringColumn();
				//到达机场三字码
				 destcode = flightsRow.getColumn("destcode").getStringColumn();
				//航班后缀
				/*String suffix = flightsRow.getColumn("").getStringColumn();*/
				//航司二字码
				String airlinecd = flightsRow.getColumn("carricd").getStringColumn();
				/*//儿童票价
				String childprice = flightsRow.getColumn("childprice").getStringColumn();
				//婴儿票价
				String babyprice = flightsRow.getColumn("babyprice").getStringColumn();*/
				//出发时机
				String oridate = flightsRow.getColumn("oridate").getStringColumn();
				//到达时间
				String destdate = flightsRow.getColumn("destdate").getStringColumn();
				//航班号
				String flightno = flightsRow.getColumn("flightno").getStringColumn();
				flightno=DateUtils.setFlightNo(flightno);
				//币种
				/*String currencyCode = flightsRow.getColumn("currencyCode").getStringColumn();*/
				//定义offer Table
				String offerID =OFFERID+UUID.randomUUID().toString().toUpperCase();
				//OffersGroup->AirlineOffers->AirlineOffer
				AirlineOffer  airlineOffer = airlineOffers.addNewAirlineOffer();
				/*airlineOffer.addNewParameters();*/
				//AirlineOffer->OfferID
				ItemIDType OfferIDArry = airlineOffer.addNewOfferID();
				OfferIDArry.setOwner(airlinecd);
				OfferIDArry.setStringValue(shoppingID+offerID);
				if(null !=ret){
					String guarantee = retshopping.getParm("orderno").getStringColumn();
					airlineOffer.addNewInventory().addNewInventoryGuarantee().setInvGuaranteeID(guarantee);
				}
				//AirlineOffer->PricedOffer
				PricedOffer pricedOfferArry = airlineOffer.addNewPricedOffer();
				Table brandinfosTab = flightsRow.getColumn("brandinfos").getTableColumn();
				for(Row brandinfosRow :brandinfosTab){
					//品牌code
					String code = brandinfosRow.getColumn("id").getStringColumn();
					/*//品牌name
					String name = brandinfosRow.getColumn("name").getStringColumn();
					//品牌描述
					String describe = brandinfosRow.getColumn("describe").getStringColumn();*/
					//辅营服务
					Table freeTable = brandinfosRow.getColumn("freeproduct").getTableColumn();
					Table freeSTable = new Table(new String[]{"ssrtype","describe","name","ssrinfo","code","type","airlinecd","brandid"});
					Row frees = freeSTable.addRow();
//					for(Row free:freeTable){
//						LOGGER.info("===zhangjia========q9====");
	//
//						String freeCode = free.getColumn("code").getStringColumn();
//						String ssrtype = free.getColumn("ssrtype").getStringColumn();
//						String freedescribe = free.getColumn("describe").getStringColumn();
//						String nameKey = "zh_CN".equals(language)?"zh_CN":"en_US";
//						//辅营名称
//						String freename = free.getColumn("name").getObjectColumn().getParm(nameKey).getStringColumn();
//						String ssrinfo = free.getColumn("ssrinfo").getStringColumn();
//						String type = free.getColumn("type").getStringColumn();
//						frees.addColumn("code",freeCode );
//						frees.addColumn("ssrtype",ssrtype );
//						frees.addColumn("describe",freedescribe );
//						frees.addColumn("name",freename );
//						frees.addColumn("ssrinfo",ssrinfo );
//						frees.addColumn("type",type );
//						frees.addColumn("airlinecd", airlinecd);
//						setFreeproduct(freeCode,code,frees);
//					}
					//舱位数据
					Table faresTable = brandinfosRow.getColumn("fares").getTableColumn();
					for(Row fare:faresTable){
						//子舱位 cabin
						String cabin = fare.getColumn("cabin").getStringColumn();
						//舱位
						String basiccabin = fare.getColumn("basiccabin").getStringColumn();
						//舱位价格
						String price = fare.getColumn("price").getStringColumn();
						//总价格价格
						/*String totalprice = fare.getColumn("totalprice").getStringColumn();*/
						//航班后缀
						/*String suffix1 = fare.getColumn("").getStringColumn();*/
						//退改规则数据
						CommandData refunded = fare.getColumn("refunded").getObjectColumn();
						//舱位个数
						String cabinnum = fare.getColumn("cabinnum").getStringColumn();
						int	 cabinnumber = "A".equals(cabinnum)? 10:Integer.valueOf(cabinnum);
						cabinCode.append(cabin);
						cabinCode.append(cabinnumber);
						cabinCode.append(";");
						String offeritemId = offerID+"."+code+cabin;
						OfferPrice offerPriceArry = pricedOfferArry.addNewOfferPrice();
						offerPriceArry.addNewOfferItemID().setStringValue(code+cabin);
						RequestedDate requestedDateArry = offerPriceArry.addNewRequestedDate();
						//舱位+品牌code
						PricedFlightOfferAssocType associationsArry = requestedDateArry.addNewAssociations();
						FlightSegmentReference frArry = associationsArry.addNewApplicableFlight().addNewFlightSegmentReference();
						frArry.setRef("SEG"+SEG);
						//舱位
						FlightCOSCoreType flightCOSCoreArry = frArry.addNewClassOfService();
						Code codeArry = flightCOSCoreArry.addNewCode();
						codeArry.setSeatsLeft(cabinnumber);
						codeArry.setStringValue(cabin);
						MarketingName mNameArry = flightCOSCoreArry.addNewMarketingName();
						//舱位名称
						String basicname = StatusUtil.getLanguageName(fare.getColumn("basicname").getObjectColumn(),language);
						mNameArry.setStringValue(basicname);
						mNameArry.setCabinDesignator(basiccabin);
						//品牌code
						/*associationsArry.addNewPriceClass().setPriceClassReference(code);*/
						CommandRet brandinfosRet = new CommandRet("");
						flightsRow.copyTo(brandinfosRet, true);
						brandinfosRet.addParm("id", code);
						brandinfosRet.addParm("basiccabin", basiccabin);
						brandinfosRet.addParm("familycode", code);
						brandinfosRet.addParm("cabin", cabin);
						brandinfosRet.addParm("price", price);
						brandinfosRet.addParm("airlinecd", airlinecd);
						brandinfosRet.addParm("oridate", oridate);
						brandinfosRet.addParm("destdate", destdate);
						brandinfosRet.addParm("flightno", flightno);
						brandinfosRet.addParm("routtype", routtype);
						brandinfosRet.addParm("flightDay", flightDay);
						offeritemRet.addParm(offeritemId, brandinfosRet);
						//将航班信息放入list中
						brandinfoList.add(brandinfosRet);
					}
				}
				offerRet.addParm(shoppingID+offerID,offeritemRet);
				String codes = cabinCode.toString();
				addFlightSegment(flightSegmentListArry,flightsRow,codes.substring(0,codes.length()-1),SEG);
				segment.append("SEG"+SEG);
				segment.append(" ");
				String helpKey = oricode+" "+destcode;
				if(!helpMap.containsKey(helpKey)){
					StringBuffer newFltS = new StringBuffer("");
					helpMap.put(helpKey, newFltS.append("FLT"+SEG+" "));
				}else{
					StringBuffer oldFltS = helpMap.get(helpKey).append("FLT"+SEG+" ");
					helpMap.put(helpKey, oldFltS);
				}
			//	FLTNUM++;
			}
			String flightids = segment.toString().trim();
			if(!"".equals(flightids) && null !=flightids){
				String[] fltTable=segment.toString().trim().split(" ");
				for(String flt:fltTable){
					Flight flightArry =flightListArry.addNewFlight();
					flightArry.setFlightKey("FLT"+SEG);
					flightArry.addNewSegmentReferences().setStringValue(flt);
				}
			}
			++SEG;
		}else {
			CommandData ret= retshopping.getParm("shoppingdata").getObjectColumn();
			Table  flightsTable = null;
			if(null == ret){
				flightsTable = retshopping.getParm("flights").getTableColumn();
			}else{
				flightsTable = ret.getParm("flights").getTableColumn();
			}
			String oricode = "";
			String destcode = "";
			//拼接航班编号
			StringBuffer segment =new StringBuffer();
			for(Row flightsRow :flightsTable){
				StringBuffer cabinCode = new StringBuffer();
				CommandRet offeritemRet = new CommandRet("");
				String flightDay = flightsRow.getColumn("flightDay").getStringColumn();
				/*//航班id
				String flightid = flightsRow.getColumn("flightid").getStringColumn();*/
				//出发机场三字码
				oricode = flightsRow.getColumn("oricode").getStringColumn();
				//到达机场三字码
				 destcode = flightsRow.getColumn("destcode").getStringColumn();
				//航班后缀
				/*String suffix = flightsRow.getColumn("").getStringColumn();*/
				//航司二字码
				String airlinecd = flightsRow.getColumn("carricd").getStringColumn();
				/*//儿童票价
				String childprice = flightsRow.getColumn("childprice").getStringColumn();
				//婴儿票价
				String babyprice = flightsRow.getColumn("babyprice").getStringColumn();*/
				//出发时机
				String oridate = flightsRow.getColumn("oridate").getStringColumn();
				//到达时间
				String destdate = flightsRow.getColumn("destdate").getStringColumn();
				//航班号
				String flightno = flightsRow.getColumn("flightno").getStringColumn();
				DateUtils.setFlightNo(flightno);
				//币种
				/*String currencyCode = flightsRow.getColumn("currencyCode").getStringColumn();*/
				//定义offer Table
				String offerID =OFFERID+UUID.randomUUID().toString().toUpperCase();
				//OffersGroup->AirlineOffers->AirlineOffer
				AirlineOffer  airlineOffer = airlineOffers.addNewAirlineOffer();
				/*airlineOffer.addNewParameters();*/
				//AirlineOffer->OfferID
				ItemIDType OfferIDArry = airlineOffer.addNewOfferID();
				OfferIDArry.setOwner(airlinecd);
				OfferIDArry.setStringValue(shoppingID+offerID);
				if(null !=ret){
					String guarantee = retshopping.getParm("orderno").getStringColumn();
					airlineOffer.addNewInventory().addNewInventoryGuarantee().setInvGuaranteeID(guarantee);
				}
				//AirlineOffer->PricedOffer
				PricedOffer pricedOfferArry = airlineOffer.addNewPricedOffer();
				Table brandinfosTab = flightsRow.getColumn("brandinfos").getTableColumn();
				for(Row brandinfosRow :brandinfosTab){
					//品牌code
					String code = brandinfosRow.getColumn("id").getStringColumn();
					/*//品牌name
					String name = brandinfosRow.getColumn("name").getStringColumn();
					//品牌描述
					String describe = brandinfosRow.getColumn("describe").getStringColumn();*/
					//辅营服务
					Table freeTable = brandinfosRow.getColumn("freeproduct").getTableColumn();
					Table freeSTable = new Table(new String[]{"ssrtype","describe","name","ssrinfo","code","type","airlinecd","brandid"});
					Row frees = freeSTable.addRow();
//					for(Row free:freeTable){
//						LOGGER.info("===zhangjia========q9====");
	//
//						String freeCode = free.getColumn("code").getStringColumn();
//						String ssrtype = free.getColumn("ssrtype").getStringColumn();
//						String freedescribe = free.getColumn("describe").getStringColumn();
//						String nameKey = "zh_CN".equals(language)?"zh_CN":"en_US";
//						//辅营名称
//						String freename = free.getColumn("name").getObjectColumn().getParm(nameKey).getStringColumn();
//						String ssrinfo = free.getColumn("ssrinfo").getStringColumn();
//						String type = free.getColumn("type").getStringColumn();
//						frees.addColumn("code",freeCode );
//						frees.addColumn("ssrtype",ssrtype );
//						frees.addColumn("describe",freedescribe );
//						frees.addColumn("name",freename );
//						frees.addColumn("ssrinfo",ssrinfo );
//						frees.addColumn("type",type );
//						frees.addColumn("airlinecd", airlinecd);
//						setFreeproduct(freeCode,code,frees);
//					}
					//舱位数据
					Table faresTable = brandinfosRow.getColumn("fares").getTableColumn();
					for(Row fare:faresTable){
						//子舱位 cabin
						String cabin = fare.getColumn("cabin").getStringColumn();
						//舱位
						String basiccabin = fare.getColumn("basiccabin").getStringColumn();
						//舱位价格
						String price = fare.getColumn("price").getStringColumn();
						//总价格价格
						/*String totalprice = fare.getColumn("totalprice").getStringColumn();*/
						//航班后缀
						/*String suffix1 = fare.getColumn("").getStringColumn();*/
						//退改规则数据
						CommandData refunded = fare.getColumn("refunded").getObjectColumn();
						//舱位个数
						String cabinnum = fare.getColumn("cabinnum").getStringColumn();
						int	 cabinnumber = "A".equals(cabinnum)? 10:Integer.valueOf(cabinnum);
						cabinCode.append(cabin);
						cabinCode.append(cabinnumber);
						cabinCode.append(";");
						String offeritemId = offerID+"."+code+cabin;
						OfferPrice offerPriceArry = pricedOfferArry.addNewOfferPrice();
						offerPriceArry.addNewOfferItemID().setStringValue(code+cabin);
						RequestedDate requestedDateArry = offerPriceArry.addNewRequestedDate();
						//舱位+品牌code
						PricedFlightOfferAssocType associationsArry = requestedDateArry.addNewAssociations();
						FlightSegmentReference frArry = associationsArry.addNewApplicableFlight().addNewFlightSegmentReference();
						frArry.setRef("SEG"+SEG);
						//舱位
						FlightCOSCoreType flightCOSCoreArry = frArry.addNewClassOfService();
						Code codeArry = flightCOSCoreArry.addNewCode();
						codeArry.setSeatsLeft(cabinnumber);
						codeArry.setStringValue(cabin);
						MarketingName mNameArry = flightCOSCoreArry.addNewMarketingName();
						//舱位名称
						String basicname = StatusUtil.getLanguageName(fare.getColumn("basicname").getObjectColumn(),language);
						mNameArry.setStringValue(basicname);
						mNameArry.setCabinDesignator(basiccabin);
						//品牌code
						/*associationsArry.addNewPriceClass().setPriceClassReference(code);*/
						CommandRet brandinfosRet = new CommandRet("");
						flightsRow.copyTo(brandinfosRet, true);
						brandinfosRet.addParm("id", code);
						brandinfosRet.addParm("basiccabin", basiccabin);
						brandinfosRet.addParm("familycode", code);
						brandinfosRet.addParm("cabin", cabin);
						brandinfosRet.addParm("price", price);
						brandinfosRet.addParm("airlinecd", airlinecd);
						brandinfosRet.addParm("oridate", oridate);
						brandinfosRet.addParm("destdate", destdate);
						brandinfosRet.addParm("flightno", flightno);
						brandinfosRet.addParm("routtype", routtype);
						brandinfosRet.addParm("flightDay", flightDay);
						offeritemRet.addParm(offeritemId, brandinfosRet);
						//将航班信息放入list中
						brandinfoList.add(brandinfosRet);
					}
				}
				offerRet.addParm(shoppingID+offerID,offeritemRet);
				String codes = cabinCode.toString();
				addFlightSegment(flightSegmentListArry,flightsRow,codes.substring(0,codes.length()-1),SEG);
				segment.append("SEG"+SEG);
				segment.append(" ");
				++SEG;
				String helpKey = oricode+" "+destcode;
				if(!helpMap.containsKey(helpKey)){
					StringBuffer newFltS = new StringBuffer("");
					helpMap.put(helpKey, newFltS.append("FLT"+FLTNUM+" "));
				}else{
					StringBuffer oldFltS = helpMap.get(helpKey).append("FLT"+FLTNUM+" ");
					helpMap.put(helpKey, oldFltS);
				}
				FLTNUM++;
			}
			String flightids = segment.toString().trim();
			if(!"".equals(flightids) && null !=flightids){
				String[] fltTable=segment.toString().trim().split(" ");
				for(String flt:fltTable){
					Flight flightArry =flightListArry.addNewFlight();
					flightArry.setFlightKey("FLT"+FLTNO);
					flightArry.addNewSegmentReferences().setStringValue(flt);
					FLTNO++;
				}
			}
		
		}
		
	}
	
	/**
	 *  旅客机票价格和税费结果
	 * @param offerPriceArry 节点
	 * @param fare 舱位数据
	 * @param refundid 退改规则id
	 * @param passengerKey 旅客类型
	 */
	public   void setPassengerArry(FareDetailType fareDetailArry,Row fare,String refundid,
			String passengerKey,String price ){
		 FareComponentType  FareComponentArry =  fareDetailArry.addNewFareComponent();
		 FareComponentArry.setRefs(getList(passengerKey));
		 FarePriceDetailType priceArry = FareComponentArry.addNewPriceBreakdown().addNewPrice();
		 //销售舱位价格
		 CurrencyAmountOptType BaseAmount = priceArry.addNewBaseAmount();
		 //币种
		 String currencyCode =fare.getColumn("currencyCode").getStringColumn();
		 BaseAmount.setCode(currencyCode);
		 //舱位价格
		 price = ADT.equals(passengerKey)?fare.getColumn("price").getStringColumn():price;
		 BaseAmount.setStringValue(price);
		 //税和费
		 Breakdown  reakdownArry = priceArry.addNewTaxes().addNewBreakdown();
		 //税信息
		 Table taxsTab = fare.getColumn("tax").getTableColumn();
		 if(null != taxsTab){
			 setTaxesArry(reakdownArry,taxsTab,passengerKey);
		 }
		 //费信息	
		 Table feesTab = fare.getColumn("fee").getTableColumn();
		 if(null != feesTab){
			 setTaxesArry(reakdownArry,feesTab,passengerKey);
		 }
		 //关联的退改规则id
		 FareComponentArry.addNewFareRules().addNewPenalty().setRefs(getList(refundid));
	}
	
	
	/**
	 * 税费数据
	 * @param priceDetailArry
	 * @param taxsTab
	 */
	public  void setTaxesArry(Breakdown  reakdownArry,Table tab,String paxtype){
		 for(Row taxs:tab){
			 String chargeKey ="";
			 if(INF.equals(paxtype)){
				 chargeKey = "infCharge";
			 }else if(CHD.equals(paxtype)){
				 chargeKey = "chdCharge";
			 }else{
				 chargeKey = "adtCharge";
			 }
			 //价格
			 String price =taxs.getColumn(chargeKey).getStringColumn();
			 String nameKey = "zh_CN".equals(language)?"cnName":"enname";
			 //税费名称
			 String taxname =taxs.getColumn(nameKey).getStringColumn();
			 //税费类型
			 String taxcode =taxs.getColumn("code").getStringColumn();
			 //币种
			 String currencyCode =taxs.getColumn("currencyCode").getStringColumn();
			 Tax taxArry = reakdownArry.addNewTax();
			 CurrencyAmountOptType  amountArry = taxArry.addNewAmount();
			 amountArry.setStringValue(price);
			 amountArry.setCode(currencyCode);
			 taxArry.setTaxCode(taxcode);
			 taxArry.setTaxType(taxname);
		 }
	}
	
	
	/**
	 * 航班数据
	 * @param flightSegmentListArry
	 * @param flightsRow
	 * @param cabins
	 * @throws ParseException 
	 */
	public  void addFlightSegment(FlightSegmentList flightSegmentListArry,Row flightsRow
			,String cabins,int seg) throws ParseException{
		/*//航班id
		String flightid = flightsRow.getColumn("flightid").getStringColumn();*/
		//航司二字码
		String airlinecd = flightsRow.getColumn("airlinecd").getStringColumn();
		//航班号
		String flightno = flightsRow.getColumn("flightno").getStringColumn();
		flightno=DateUtils.setFlightNo(flightno);
		//机型
		String planestype = flightsRow.getColumn("planestype").getStringColumn();
		//出发机场三字码
		String oricode = flightsRow.getColumn("oricode").getStringColumn();
		//到达机场三字码
		String destcode = flightsRow.getColumn("destcode").getStringColumn();
		//出发日期
		String oriDay = flightsRow.getColumn("oriDay").getStringColumn();
		//出发时间
		Date oridateS = flightsRow.getColumn("oridate").getDateColumn();
		String oriTime = flightsRow.getColumn("oriTime").getStringColumn();
		//到达日期
		String destDateTime = flightsRow.getColumn("destDateTime").getStringColumn();
		//到达时间
		Date destdateS = flightsRow.getColumn("destdate").getDateColumn();
		String destTime = flightsRow.getColumn("destTime").getStringColumn();
		//出发机场名称
		String oriname = StatusUtil.getLanguageName(flightsRow.getColumn("oriname").getObjectColumn(), language);
		//到达机场名称
		String destname =  StatusUtil.getLanguageName(flightsRow.getColumn("destname").getObjectColumn(), language);
		//出发航站楼
		String oriterminal = flightsRow.getColumn("oriterminal").getStringColumn();
		//到达航站楼
		String destterminal = flightsRow.getColumn("destterminal").getStringColumn();
		//航班后缀
		String suffix = flightsRow.getColumn("suffix").getStringColumn();
		//承运航班
		/*String carricd = flightsRow.getColumn("carricd").getStringColumn();*/
		ListOfFlightSegmentType flightArry = flightSegmentListArry.addNewFlightSegment();
		flightArry.setSegmentKey("SEG"+seg);
		/*出发航班信息**************************************************/
		Departure  departureArry =flightArry.addNewDeparture();
		departureArry.addNewAirportCode().setStringValue(oricode);
		if(oriDay != null && !"".equals(oriDay)){
			Calendar oridate = DateUtils.getInstance().parseDate(oriDay, "yyyyMMdd");
			if (oridate != null) {
				departureArry.setDate(oridate);
				departureArry.setTime(oriTime);
			}
		}
		departureArry.setAirportName(oriname);
		departureArry.addNewTerminal().setName(oriterminal);
		/*到达航班信息**************************************************/
		 FlightArrivalType  arrivalArry = flightArry.addNewArrival();
		 arrivalArry.addNewAirportCode().setStringValue(destcode);
		 if(destDateTime != null && !"".equals(destDateTime) && destDateTime.length() >= 10 ){
				destDateTime = destDateTime.substring(0, 10);
				Calendar destDate = DateUtils.getInstance().parseDate(destDateTime, "yyyy-MM-dd");
				arrivalArry.setDate(destDate);
				arrivalArry.setTime(destTime);
		 }
		 arrivalArry.setAirportName(destname);
		 arrivalArry.addNewTerminal().setName(destterminal);
		/*航班以及舱位数据***********************************************/
		 MarketingCarrierFlightType mcFlightArry = flightArry.addNewMarketingCarrier();
		 mcFlightArry.addNewAirlineID().setStringValue(airlinecd);
		 FlightNumber  flightNumberArry = mcFlightArry.addNewFlightNumber();
		 //航班后缀
		 flightNumberArry.setOperationalSuffix(suffix);
		 flightNumberArry.setStringValue(flightno);
		/* mcFlightArry.setResBookDesigCode(cabins);*/
		 /*机型******************************************************/
		 flightArry.addNewEquipment().addNewAircraftCode().setStringValue(planestype);
		 /*飞行数据***************************************************/
		 FlightDetailType flightDetailArry =flightArry.addNewFlightDetail();
		 FlightDistanceType fdArry = flightDetailArry.addNewFlightDistance();
		 //飞行距离
		 Long mileage = flightsRow.getColumn("mileage").getLongColumn();
		 fdArry.setValue(BigInteger.valueOf(mileage));
		 fdArry.setUOM("KM");
		 long l =  destdateS.getTime() - oridateS.getTime();
		 //飞行时间
		 long hour=(l/(60*60*1000));
		 long min=((l/(60*1000))-hour*60);
		 long s=(l/1000-hour*60*60-min*60);
		 BigDecimal fraction = new BigDecimal(0);
		 GDuration gDuration = new GDuration(1,0,  0, 0, Integer.parseInt(String.valueOf(hour)),
				 Integer.parseInt(String.valueOf(min)), Integer.parseInt(String.valueOf(s)), fraction);
		 flightDetailArry.addNewFlightDuration().setValue(gDuration);
		 //经停次数
		 Stops  stopsArry =  flightDetailArry.addNewStops();
		 Table stops =flightsRow.getColumn("passby").getTableColumn();
		 int stpnum =0;
		 if(null!=stops&& !"".equals(stops)){
			 stpnum = stops.getRowCount();
			 stopsArry.setStopQuantity(BigInteger.valueOf(stpnum));
			 StopLocationType stopLocationType = stopsArry.addNewStopLocations();
			 for(Row stop :stops){
				 //经停机场Name
				 String  airName = StatusUtil.getLanguageName(stop.getColumn("pbAirport").getObjectColumn(),language);
				 //经停机场三字码
				 String  airCode = stop.getColumn("pbCode").getStringColumn();
				 //经停机场到达时间
				 String  pbStart = stop.getColumn("pbStart").getStringColumn();
				 //经停机场起飞时间
				 String  pbEnd = stop.getColumn("pbEnd").getStringColumn();
				 StopLocation  stopLocationArry = stopLocationType.addNewStopLocation();
				 stopLocationArry.addNewAirportCode().setStringValue(airCode);
				 stopLocationArry.setName(airName);
				//经停机场到达日期
				 String  pbStartDate = stop.getColumn("pbStartDate").getStringColumn();
				 Calendar beginDate = null;
				 if(pbStartDate != null && !"".equals(pbStartDate)){
					 beginDate = DateUtils.getInstance().parseDate(pbStartDate, "yyyy-MM-dd");
				 }
				 stopLocationArry.setArrivalDate(beginDate);
				 stopLocationArry.setArrivalTime(pbStart);
				//经停机场出发日期
				 String  pbEndDate = stop.getColumn("pbEndDate").getStringColumn();
				 Calendar endDate = null;
				 if(pbEndDate != null && !"".equals(pbEndDate)){
					 endDate = DateUtils.getInstance().parseDate(pbEndDate, "yyyy-MM-dd");
				 }
				 stopLocationArry.setDepartureDate(endDate);
				 stopLocationArry.setDepartureTime(pbEnd);
			 }
		 }
	}
	/**
	 * 根据Java编程规范：由于for循环中不能创建对象，所有将创建对象放到方法里
	 *
	 * @return CommandData
	 */
	public  CommandData getCreateCommandData() {
		return new CommandData();
	}
	/**
	 * 根据Java编程规范：由于for循环中不能创建对象，所有将创建对象放到方法里
	 *
	 * @return list
	 */
	public  List<String > getList(String args) {
		List<String> list = new ArrayList<String>();
		list.add(args);
		return list;
	}

	/**
	 * 设置 商品和免费服关联关系
	 * @param freeid 服务id
	 * @param brandid 商品id
	 * @param freeRow 服务内容
	 */
	public  void setFreeproduct(String freeid,String brandid,Row freeRow){
		Row row = freeMap.get(freeid);
		if(!"".equals(row) && null !=row){
			String id = row.getColumn("brandid").getStringColumn().trim();
			int num = id.indexOf(brandid);
			if(num == -1){
				StringBuffer  idBuffer = new StringBuffer(id);
				idBuffer.append(" ");
				idBuffer.append(brandid);
				brandid = idBuffer.toString();
			}else{
				brandid = id;
			}
		}
		freeRow.addColumn("brandid",brandid);
		freeMap.put(freeid,freeRow);
	}
}