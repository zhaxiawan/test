package com.travelsky.quick.util.helper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirShopReqAttributeQueryType.OriginDestination;
import org.iata.iata.edist.AirShopReqParamsType;
import org.iata.iata.edist.AirShoppingRQDocument;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ;
import org.iata.iata.edist.AirShoppingRSDocument;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.DataLists;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers.AirlineOffer;
import org.iata.iata.edist.AnonymousTravelerListDocument.AnonymousTravelerList;
import org.iata.iata.edist.AnonymousTravelerType;
import org.iata.iata.edist.ApplicableFlightDocument.ApplicableFlight;
import org.iata.iata.edist.BagDetailAssociationDocument.BagDetailAssociation;
import org.iata.iata.edist.CurrCodeDocument.CurrCode;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.DataListType.FlightList;
import org.iata.iata.edist.DataListType.FlightList.Flight;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DataListType.OriginDestinationList;
import org.iata.iata.edist.DataListType.SeatList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FareComponentType;
import org.iata.iata.edist.FareDetailType;
import org.iata.iata.edist.FarePriceDetailType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightCOSCoreType;
import org.iata.iata.edist.FlightCOSCoreType.Code;
import org.iata.iata.edist.FlightCOSCoreType.MarketingName;
import org.iata.iata.edist.FlightDepartureType.AirportCode;
import org.iata.iata.edist.FlightDetailType;
import org.iata.iata.edist.FlightDetailType.Stops;
import org.iata.iata.edist.FlightDistanceType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.FlightSegmentReferenceDocument.FlightSegmentReference;
import org.iata.iata.edist.ItemIDType;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfOfferPenaltyType;
import org.iata.iata.edist.ListOfOfferPenaltyType.Penalty;
import org.iata.iata.edist.ListOfPriceClassType;
import org.iata.iata.edist.ListOfSeatType;
import org.iata.iata.edist.ListOfServiceBundleType;
import org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle;
import org.iata.iata.edist.ListOfServiceBundleType.ServiceBundle.Associations;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.MessageParamsBaseType.CurrCodes;
import org.iata.iata.edist.OfferPriceLeadDetailType.PriceDetail;
import org.iata.iata.edist.OfferPriceLeadType.RequestedDate;
import org.iata.iata.edist.OrderItemAssociationType.OfferItems;
import org.iata.iata.edist.PriceClassType;
import org.iata.iata.edist.PricedFlightOfferAssocType;
import org.iata.iata.edist.PricedFlightOfferType.OfferPrice;
import org.iata.iata.edist.PricedOfferDocument.PricedOffer;
import org.iata.iata.edist.ServiceCoreType.BookingInstructions;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceIDType;
import org.iata.iata.edist.ServiceListDocument.ServiceList;
import org.iata.iata.edist.ShoppingResponseIDType;
import org.iata.iata.edist.StopLocationType;
import org.iata.iata.edist.StopLocationType.StopLocation;
import org.iata.iata.edist.TaxDetailType.Breakdown;
import org.iata.iata.edist.TaxDetailType.Breakdown.Tax;
import org.iata.iata.edist.TravelerCoreType.PTC;
import org.iata.iata.edist.TravelersDocument.Travelers.Traveler;
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
import com.travelsky.quick.util.StatusUtil;

/**
 * 品牌查询
 * 
 * @author lizhi
 *
 */
public class APIAirShoppingNDCB2C {
	/**
	 * 
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(APIAirShoppingNDCB2C.class);
	// 获取语言
	private String language = ApiServletHolder.getApiContext().getLanguage();
	/**
	 * 旅客数量 String 旅客类型 String 数量
	 */
	private Map<String, String> travelerMap = new HashMap<String, String>();
	/**
	 * 辅营信息 String 免费服务 id Row 服务内容
	 */
	private Map<String, String> ServiceBundleMap = new HashMap<String, String>();
	/**
	 * 退改规则 String 规则id CommandData 规则信息
	 */
	private Map<String, CommandData> refundMap = new HashMap<String, CommandData>();
	/**
	 * 辅营信息 String 免费服务 id Row 服务内容
	 */
	private Map<String, Row> freeMap = new HashMap<String, Row>();
	// 航班数据集合
	private List<CommandRet> brandinfoList = new ArrayList<CommandRet>();
	// 用户存放免费座位的offerItemId
	private Set<String> seatSet = new HashSet<>();
	// Redis缓存限制时间（单位 秒）
	private static final int TIME = 2400;
	// shopping请求ID
	private static final String SHOPPINGAPI = "SHOPPING.API.";
	// offerID
	private static final String OFFERID = "OFFERID.";
	// 去程标记符
	private static final String OW = "OW";
	// 回程标记符
	private static final String RT = "RT";
	// 成人标记符
	private static final String ADT = "ADT";
	// 婴儿标记符
	private static final String INF = "INF";
	// 儿童标记符
	private static final String CHD = "CHD";
	private static final String _FOR_FREE = "_FOR_FREE";
	// 币种
	private String CURRENCYCODE = "";
	
	// 航司二字码
	private String AIRLINECD = "";
	// 航班排序
	private int SEG = 1;
	private int FLTNUM = 1;
	private int MAXPASSENGERNUM = 9;

	public void doServletB2C(SelvetContext<ApiContext> context) throws Exception {
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean(context);
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_SHOPPING,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	/**
	 * xml-->Reqbean
	 * 
	 * @throws APIException
	 * @throws Exception
	 */
	public void transInputXmlToRequestBean(SelvetContext<ApiContext> context) throws APIException, Exception {
		CommandData input = context.getInput();
		String xmlInput = context.getContext().getReqXML();
		AirShoppingRQDocument rootDoc = AirShoppingRQDocument.Factory.parse(xmlInput);
		AirShoppingRQ shoppingRQ = rootDoc.getAirShoppingRQ();
		// 币种
		AirShopReqParamsType parameters = shoppingRQ.getParameters();
		CurrCodes currCodes = parameters == null ? null : parameters.getCurrCodes();
		CurrCode[] currCodeArr = currCodes == null ? null : currCodes.getCurrCodeArray();
		CurrCode currCodeEle = currCodeArr == null ? null : currCodeArr[0];
		String currCoce = currCodeEle == null ? null : currCodeEle.getStringValue();
		input.addParm("isoCode", currCoce);
		// 获取旅客类型以及数量
		Traveler[] traveler = shoppingRQ.getTravelers().getTravelerArray();
		getPassengerNum(traveler);
		// shopping查询数据
		OriginDestination[] originDestination = shoppingRQ.getCoreQuery().getOriginDestinations()
				.getOriginDestinationArray();
		getCoreShopping(originDestination, input);
	}

	/**
	 * 获取旅客类型以及数量 放入全局Map变量中
	 * 
	 * @param traveler
	 * @throws APIException
	 */
	public void getPassengerNum(Traveler[] traveler) throws APIException {
		int passengerNumSum = 0;
		for (Traveler travelerArray : traveler) {
			PTC ptc = travelerArray.getAnonymousTravelerArray(0).getPTC();
			// 人数
			int passengerNum = ptc.getQuantity().intValue();
			passengerNumSum = passengerNumSum + passengerNum;
			// 旅客类型
			String PassengerType = ptc.getStringValue();
			travelerMap.put(PassengerType, String.valueOf(passengerNum));
		}
		if (passengerNumSum > MAXPASSENGERNUM) {
			LOGGER.info(
					TipMessager.getInfoMessage(ErrCodeConstants.API_TOTAL_PASSENGETSNUM_NOT_MORETHAN_NINE, language));
			throw APIException.getInstance(ErrCodeConstants.API_TOTAL_PASSENGETSNUM_NOT_MORETHAN_NINE);
		}
	}

	/**
	 * 乘机人信息
	 * 
	 * @param originDestination
	 *            乘机人Table
	 * @param input
	 *            请求参数添加
	 * @throws APIException
	 */
	public void getCoreShopping(OriginDestination[] originDestination, CommandData input) throws APIException {
		SimpleDateFormat adf = DateUtils.getInstance().getSimDate("yyyyMMdd");
		// originNum >1 说明是往返
		int originNum = originDestination.length;
		for (int i = 0; i < originNum; i++) {
			CommandData commandData = getCreateCommandData();
			AirportCode dacodeArry = originDestination[i].getDeparture().getAirportCode();
			// 出发地字节码标记
			String oriCodeType = null == dacodeArry.getApplication() ? "" : dacodeArry.getApplication();
			// 出发地三字码
			String depart = dacodeArry.getStringValue();
			if (!StringUtils.hasLength(depart)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_ORG, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_ORG);
			}
			// 出发时间
			// Date deptdate
			// =originDestination[i].getDeparture().getDate().getTime();
			Date deptdate = null;
			if (originDestination[i] != null && originDestination[i].getDeparture() != null) {
				try {
					originDestination[i].getDeparture().getDate();
				} catch (Exception e) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME);

				}
				if (originDestination[i].getDeparture().getDate() == null) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHTORG_TIME);
				} else {
					deptdate = originDestination[i].getDeparture().getDate().getTime();
				}
			}

			org.iata.iata.edist.FlightArrivalType.AirportCode aacode = originDestination[i].getArrival()
					.getAirportCode();
			// 目标地字节码标记
			String destCodeType = null == aacode.getApplication() ? "" : aacode.getApplication();
			String arrive = aacode.getStringValue();
			if (!StringUtils.hasLength(arrive)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_DST, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_DST);
			}
			// 出发时间
			commandData.addParm("deptdate", adf.format(deptdate));
			// 出发地
			commandData.addParm("depart", depart);
			commandData.addParm("oriCodeType", oriCodeType);
			// 目的地
			commandData.addParm("arrive", arrive);
			commandData.addParm("destCodeType", destCodeType);
			commandData.addParm("adt", travelerMap.get(ADT));
			commandData.addParm("chd", travelerMap.get(CHD));
			commandData.addParm("inf", travelerMap.get(INF));
			commandData.addParm("isoCode", input.getParm("isoCode").toString());
			// 部门ID
			// String deptno = NdcXmlHelper.getDeptNo(reqdoc.getParty());
			String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
			input.addParm("tktdeptid", deptno);
			commandData.addParm("tktdeptid", deptno);
			if (i == 0) {
				// 去程
				input.addParm(OW, commandData);
				// 将rediskey需要的相关信息放入travelmap中
				// 这个数据防止了往返的时候获取去程的时间
//				travelerMap.put("deptdateGo", adf.format(deptdate));
//				travelerMap.put("depart", depart);
//				travelerMap.put("arrive", arrive);
//				String appID = ApiServletHolder.getApiContext().getAppid();
//				travelerMap.put("appID", appID);
//				travelerMap.put("roundTrip", "O");
			} else if (i == 1) {
				// 回城
				input.addParm(RT, commandData);
				// 将rediskey需要的相关信息放入travelmap中
//				travelerMap.put("deptdateReturn", adf.format(deptdate));
//				// 返程： 出发地和目的地交换位置
//				travelerMap.put("depart", arrive);
//				travelerMap.put("arrive", depart);
//				String appID = ApiServletHolder.getApiContext().getAppid();
//				travelerMap.put("appID", appID);
//				travelerMap.put("roundTrip", "W");
			}
		}
		// 组装redisKey值 直接从上一层获取
//		redisKey = assemblyKey();
	}

	/**
	 * 获取币种
	 * 
	 * @param isoCode
	 * @param l_ret
	 * @return
	 */
	public String getIsoCode(String isoCode, CommandRet l_ret) {
		if (!"".equals(l_ret.getErrorCode())) {
			return "";
		} else {
			String isoCodeOne = "";
			Table currencys = l_ret.getParm("currencyInfoQuery").getTableColumn();
			for (Row currency : currencys) {
				String currCoce = currency.getColumn("isoCode").getStringColumn();
				// 1 默认币种 0非默认币种
				String status = currency.getColumn("editStatus").getStringColumn();
				if (isoCode.equals(currCoce)) {
					return currCoce;
				}
				if ("1".equals(status)) {
					isoCodeOne = currCoce;
				}
			}
			return isoCodeOne;
		}
	}

	/******************************************************************************************************/
	/**
	 * 转换ResponseBean-->XmlBean
	 * 
	 * @param commandRet
	 * @param input
	 * @return
	 */
	public XmlObject transRespBeanToXmlBeanB2C(CommandRet commandRet, CommandData input) {
		CommandRet xmlOutput = commandRet;
		AirShoppingRSDocument doc = AirShoppingRSDocument.Factory.newInstance();
		AirShoppingRS rs = doc.addNewAirShoppingRS();
		//从input中获取rdiskey
		try {
			if (!"".equals(xmlOutput.getErrorCode())) {
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(xmlOutput.getErrorCode()));
				error.setShortText(TipMessager.getMessage(xmlOutput.getErrorCode(),
						ApiServletHolder.getApiContext().getLanguage()));
				// 将数据放入redis中
				return doc;
			}
			// 生成一个shopping请求id 格式：（SHOPPING.API.）+shoppingID
			String shoppingID = SHOPPINGAPI + UUID.randomUUID().toString().toUpperCase();
			rs.addNewDocument();
			rs.addNewSuccess();
			// ShoppingResponseID——》ResponseID
			ShoppingResponseIDType shoppingResponseIDArry = rs.addNewShoppingResponseID();
			// OffersGroup——》AirlineOffers
			AirlineOffers airlineOffers = rs.addNewOffersGroup().addNewAirlineOffers();
			/****************************************************************/
			DataLists dataListsArry = rs.addNewDataLists();
			// 旅客信息 DataLists——》AnonymousTravelerList
			AnonymousTravelerList travelerList = dataListsArry.addNewAnonymousTravelerList();
			// 航班信息 DataLists——》FlightSegmentList
			FlightSegmentList flightSegmentListArry = dataListsArry.addNewFlightSegmentList();
			// 去程回程 关联航班信息 DataLists——》FlightList
			FlightList flightListArry = dataListsArry.addNewFlightList();
			// 航班查询的出发地 DataLists——》OriginDestinationList
			OriginDestinationList originArry = dataListsArry.addNewOriginDestinationList();
			// 退改规则 DataLists——》ListOfOfferPenaltyType
			ListOfOfferPenaltyType PenaltyListArry = dataListsArry.addNewPenaltyList();
			// 品牌信息DataLists——》ListOfPriceClassType
			ListOfPriceClassType priceClassSArry = dataListsArry.addNewPriceClassList();
			// 辅营与品牌关联DataLists——》ServiceBundleList
			ListOfServiceBundleType listOfServiceBundleArry = dataListsArry.addNewServiceBundleList();
			// 辅营信息DataLists——》ServiceList
			ServiceList serviceListArry = dataListsArry.addNewServiceList();
			// 座位信息DataLists——》seatList
			SeatList seatList = dataListsArry.addNewSeatList();
			/****************************************************************/
			// 去程
			CommandRet retOW = new CommandRet("");
			// 回程
			CommandRet retRT = new CommandRet("");
			seatSet.clear();
			// 去程ret
			if ((xmlOutput.getParm(OW).getObjectColumn() != null && !xmlOutput.getParm(OW).getObjectColumn().equals(""))
					|| (xmlOutput.getParm(RT).getObjectColumn() != null
							&& !xmlOutput.getParm(RT).getObjectColumn().equals(""))) {
				CommandData objectRetOW = xmlOutput.getParm(OW).getObjectColumn();
				CommandData objectRetRT = xmlOutput.getParm(RT).getObjectColumn();
			
					retOW=(CommandRet) objectRetOW;
				
					retRT=(CommandRet) objectRetRT;
				// 往返都没数据时返回相关异常有误
				if ((retOW!=null&&!"".equals(retOW.getErrorCode()) && retRT == null)
						|| (retOW!=null&&!"".equals(retOW.getErrorCode()) && retRT != null && !"".equals(retRT.getErrorCode()))
						||(retOW==null&&retRT==null)) {
					AirShoppingRSDocument docError = AirShoppingRSDocument.Factory.newInstance();
					AirShoppingRS rsOW = docError.addNewAirShoppingRS();
					ErrorType error = rsOW.addNewErrors().addNewError();
					error.setCode(TipMessager.getErrorCode(retOW.getErrorCode()));
					error.setShortText(TipMessager.getMessage(retOW.getErrorCode(),
							ApiServletHolder.getApiContext().getLanguage()));
					// 将数据放入redis中
					return docError;
				}
				// 返回单程数据需校验数据是否为空
				if (!"".equals(retOW) && retOW != null) {
					// 将去程航班数据放入CommandRet里
					setRedisManagerflights(retOW, shoppingID + "." + OW, "G", airlineOffers, flightSegmentListArry,
							flightListArry, originArry, priceClassSArry, seatSet);
				}
				if (!"".equals(retRT) && retRT != null) {
					// 将回程航班数据放入CommandRet里
					setRedisManagerflights(retRT, shoppingID + "." + RT, "R", airlineOffers, flightSegmentListArry,
							flightListArry, originArry, priceClassSArry, seatSet);
				}

			}
			shoppingResponseIDArry.setOwner(AIRLINECD);
			shoppingResponseIDArry.addNewResponseID().setStringValue(shoppingID);
			/* 旅客信息节点 ************************************************************/
			addTraveler(travelerList);
			/* 退改规则节点 ******************************************************************/
			addPenaltyListArry(PenaltyListArry);
			/* 辅营与品牌关联的节点 **************************************************************/
			addServiceBundleListArry(listOfServiceBundleArry);
			/* 辅营节点 *************************************************************************/
			addServiceListArry(serviceListArry);
			/* 座位节点 *************************************************************************/
			addSeatListArry(seatList);
			/* 航班下旅客类型关联的免费行李重量*************************************************************************/
//			addcarryOnAllowanceList(carryOnAllowanceList);
		} catch (Exception e) {
			LOGGER.error(ErrCodeConstants.API_SYSTEM, e);
			doc = AirShoppingRSDocument.Factory.newInstance();
			rs = doc.addNewAirShoppingRS();
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			error.setShortText(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
			// 将数据放入redis中
			return doc;
		}
		// 将数据放入redis中
		return doc;
	}

	/**
	 * 免费座位关联的offerItemId
	 * 
	 * @param seatList
	 */
	private void addSeatListArry(SeatList seatList) {
		if (seatSet != null && seatSet.size() > 0) {
			ListOfSeatType newSeats = seatList.addNewSeats();
			OfferItems offerItems = newSeats.addNewLocation().addNewAssociations().addNewOfferItems();
			for (String offerItemId : seatSet) {
				offerItems.addNewOfferItemID().setStringValue(offerItemId);
			}
		}
	}

	private String assemblyKey() {
		// B2C:service:av-PEK-SHA-O-20180401-1-0-0-appid
		StringBuffer redisKey = new StringBuffer();
		redisKey.append("B2C:service:av-");
		String depart = travelerMap.get("depart");
		redisKey.append(depart + "-");
		String arrive = travelerMap.get("arrive");
		redisKey.append(arrive + "-");
		String roundTrip = travelerMap.get("roundTrip");
		redisKey.append(roundTrip + "-");
		// 如果是往返加上去程时间
		String deptdateGo = travelerMap.get("deptdateGo");
		redisKey.append(deptdateGo + "-");
		String deptdateReturn = travelerMap.get("deptdateReturn");
		if (deptdateReturn != null && !"".equals(deptdateReturn)) {
			redisKey.append(deptdateReturn + "-");
		}
		String adt = travelerMap.get(ADT);
		redisKey.append(adt + "-");
		String chd = travelerMap.get(CHD);
		redisKey.append(chd + "-");
		String inf = travelerMap.get(INF);
		redisKey.append(inf + "-");
		String appID = travelerMap.get("appID");
		redisKey.append(appID);
		return redisKey.toString();

	}

	/**
	 * 将航班数据放入CommandRet里
	 * 
	 * @param ret
	 *            shopping数据
	 * @param shoppingRet
	 *            redis缓存command
	 * @param shoppingID
	 *            请求id
	 * @param routtype
	 *            往返标记
	 * @param airlineOffers
	 *            航班节点 OffersGroup——》AirlineOffers
	 * @param flightSegmentListArry
	 *            航班数据节点 DataLists——》FlightSegmentList
	 * @param flightListArry
	 *            去程回程 关联航班信息 DataLists——》FlightList
	 * @param originArry
	 *            航班查询的出发地 DataLists——》originArry
	 * @param priceClassSArry
	 *            品牌信息DataLists——》ListOfPriceClassType
	 * @throws ParseException
	 */
	public void setRedisManagerflights(CommandRet ret, String shoppingID, String routtype, AirlineOffers airlineOffers,
			FlightSegmentList flightSegmentListArry, FlightList flightListArry, OriginDestinationList originArry,
			ListOfPriceClassType priceClassSArry, Set<String> seatSet) throws ParseException {
		// 航班信息
		Table flightsTable = ret.getParm("flights").getTableColumn();
		// 拼接航班编号
		StringBuffer segment = new StringBuffer();
		CommandRet offerRet = new CommandRet("");
		// 出发机场三字码
		String oricode = "";
		// 到达机场三字码
		String destcode = "";
		if (flightsTable != null && flightsTable.getRowCount() > 0) {
			for (Row flightsRow : flightsTable) {
				StringBuffer cabinCode = new StringBuffer();
				CommandRet offeritemRet = new CommandRet("");
				String flightDay = flightsRow.getColumn("flightDay").getStringColumn();
				// 出发机场三字码
				oricode = flightsRow.getColumn("oricode").getStringColumn();
				// 到达机场三字码
				destcode = flightsRow.getColumn("destcode").getStringColumn();
				// 航班后缀
				String suffix = flightsRow.getColumn("suffix").getStringColumn();
				// 航司二字码
				String airlinecd = flightsRow.getColumn("carricd").getStringColumn();
				AIRLINECD = airlinecd;
				// 出发时机
				String oridate = flightsRow.getColumn("oridate").getStringColumn();
				// 到达时间
				String destdate = flightsRow.getColumn("destdate").getStringColumn();
				// 航班号
				String flightno = flightsRow.getColumn("flightno").getStringColumn();
				// 币种
				String currencyCode = flightsRow.getColumn("currencyCode").getStringColumn();
				// 费率
				String vatRate = flightsRow.getColumn("vatRate").getStringColumn();
				//行李重量单位
				String weightUnit = flightsRow.getColumn("weightUnit").getStringColumn();
				CURRENCYCODE = currencyCode;
				// 定义offer Table
				String offerID = OFFERID + UUID.randomUUID().toString().toUpperCase();
				// OffersGroup->AirlineOffers->AirlineOffer
				AirlineOffer airlineOffer = airlineOffers.addNewAirlineOffer();
				// AirlineOffer->OfferID
				ItemIDType OfferIDArry = airlineOffer.addNewOfferID();
				OfferIDArry.setOwner(airlinecd);
				OfferIDArry.setStringValue(shoppingID + offerID);
				// AirlineOffer->PricedOffer
				PricedOffer pricedOfferArry = airlineOffer.addNewPricedOffer();
				Table brandinfosTab = flightsRow.getColumn("brandinfos").getTableColumn();
				for (Row brandinfosRow : brandinfosTab) {
					// 品牌code
					String code = brandinfosRow.getColumn("id").getStringColumn();
					// 品牌name
					String name = StatusUtil.getLanguageName(brandinfosRow.getColumn("name").getObjectColumn(),
							language);
					/*
					 * String name =
					 * brandinfosRow.getColumn("name").getStringColumn();
					 */
					// 品牌描述
					String describe = brandinfosRow.getColumn("describe").getStringColumn();
					// 辅营服务
					Table freeTable = brandinfosRow.getColumn("freeproduct").getTableColumn();
					// 当前航班下座位是否免费
					String isseatfree = brandinfosRow.getColumn("isseatfree").getStringColumn();
					Table freeSTable = new Table(new String[] { "ssrtype", "describe", "name", "ssrinfo", "code",
							"type", "airlinecd", "brandid","weight" });
					StringBuffer freeCodeS = new StringBuffer();
					if (freeTable != null && freeTable.getRowCount() > 0) {
						for (Row free : freeTable) {
							String hasMeal = brandinfosRow.getColumn("hasMeal").getStringColumn();
							String freeCode = free.getColumn("code").getStringColumn();
							String ssrtype = free.getColumn("ssrtype").getStringColumn();
							String freedescribe = free.getColumn("describe").getStringColumn();
							String nameKey = "zh_CN".equals(language) ? "zh_CN" : "en_US";
							String type = free.getColumn("type").getStringColumn();
							// 过滤无效的或者超过购买时限的MEAL
							if (!(("0".equals(hasMeal) || "2".equals(hasMeal)) && "MEAL".equalsIgnoreCase(type))) {
								Row frees = freeSTable.addRow();
								// 辅营名称
								if (free.getColumn("name").getObjectColumn() != null
										&& free.getColumn("name").getObjectColumn().getParm(nameKey) != null) {
									String freename = free.getColumn("name").getObjectColumn().getParm(nameKey)
											.getStringColumn();
									frees.addColumn("name", freename);
								} else {
									frees.addColumn("name", "");
								}
								String ssrinfo = free.getColumn("ssrinfo").getStringColumn();
								CommandData attrCommandData = free.getColumn("attr").getObjectColumn();
								String weight = "";
								if(attrCommandData != null && !"".equals(attrCommandData)){
									weight = attrCommandData.getParm("weight").getStringColumn();
								}
								frees.addColumn("code", freeCode);
								frees.addColumn("ssrtype", ssrtype);
								frees.addColumn("describe", freedescribe);
								frees.addColumn("ssrinfo", ssrinfo);
								frees.addColumn("type", type);
								frees.addColumn("airlinecd", airlinecd);
								frees.addColumn("weight", weight);
								setFreeproduct(freeCode, code, frees);
								freeCodeS.append(freeCode);
								freeCodeS.append(" ");
							}
						}
					}
					// 舱位数据
					Table faresTable = brandinfosRow.getColumn("fares").getTableColumn();
					String cabin = null;
					for (Row fare : faresTable) {
						// 子舱位 cabin
						cabin = fare.getColumn("cabin").getStringColumn();
						// 舱位
						String basiccabin = fare.getColumn("basiccabin").getStringColumn();
						// 舱位价格
						String price = fare.getColumn("price").getStringColumn();
						// 儿童价格
						String chdprice = fare.getColumn("chdprice").getStringColumn();
						// 婴儿价格
						String infprice = fare.getColumn("infprice").getStringColumn();
						// 总价格价格
						String totalprice = fare.getColumn("totalprice").getStringColumn();
						//成人免费行李重量
						String adultluggage = fare.getColumn("adultluggage").getStringColumn();
						//儿童免费行李重量
						String childluggage = fare.getColumn("childluggage").getStringColumn();
						//婴儿免费行李重量
						String babyluggage = fare.getColumn("babyluggage").getStringColumn();
						/*
						 * //航班后缀 String suffix1 =
						 * fare.getColumn("suffix").getStringColumn();
						 */
						// 退改规则数据
						CommandData refunded = fare.getColumn("refunded").getObjectColumn();
						// 舱位个数
						String cabinnum = fare.getColumn("cabinnum").getStringColumn();
						int cabinnumber = "A".equals(cabinnum) ? 10 : Integer.valueOf(cabinnum);
						cabinCode.append(cabin);
						cabinCode.append(cabinnumber);
						cabinCode.append(";");
						String offeritemId = offerID + "." + code + cabin;
						OfferPrice offerPriceArry = pricedOfferArry.addNewOfferPrice();
						offerPriceArry.addNewOfferItemID().setStringValue(code + cabin);
						RequestedDate requestedDateArry = offerPriceArry.addNewRequestedDate();
						PriceDetail priceDetailArry = requestedDateArry.addNewPriceDetail();
						// 基础价格
						CurrencyAmountOptType baseAmountArry = priceDetailArry.addNewBaseAmount();
						baseAmountArry.setCode(currencyCode);
						baseAmountArry.setStringValue(price);
						// 总价
						CurrencyAmountOptType total = priceDetailArry.addNewTotalAmount().addNewDetailCurrencyPrice()
								.addNewTotal();
						total.setCode(currencyCode);
						total.setStringValue(totalprice);
						// 舱位+品牌code
						PricedFlightOfferAssocType associationsArry = requestedDateArry.addNewAssociations();
						ApplicableFlight applicableFlight = associationsArry.addNewApplicableFlight();
						//ApplicableFlight   --->  FlightSegmentReference
						FlightSegmentReference flightSegmentReference = applicableFlight.addNewFlightSegmentReference();
						flightSegmentReference.setRef("SEG" + SEG);
						// 舱位
						FlightCOSCoreType flightCOSCoreArry = flightSegmentReference.addNewClassOfService();
						Code codeArry = flightCOSCoreArry.addNewCode();
						codeArry.setSeatsLeft(cabinnumber);
						codeArry.setStringValue(cabin);
						MarketingName mNameArry = flightCOSCoreArry.addNewMarketingName();
						// 舱位名称
						String basicname = StatusUtil.getLanguageName(fare.getColumn("basicname").getObjectColumn(),
								language);
						mNameArry.setStringValue(basicname);
						mNameArry.setCabinDesignator(basiccabin);
						
						String paxType = "";
						String paxLuggage = "";
						for (int i = 0; i < 3; i++) {
							if(i == 0){
								paxType = "ADT";
								paxLuggage = adultluggage;
							}else if(i == 1){
								paxType = "CHD";
								paxLuggage = childluggage;
							}else if(i == 2){
								paxType = "INF";
								paxLuggage = babyluggage;
							}
							List<String> list = new ArrayList<String>();
							BagDetailAssociation bagDetailAssociation = flightSegmentReference.addNewBagDetailAssociation();
							list.add(paxLuggage+"_"+weightUnit);
							bagDetailAssociation.setCarryOnReferences(list);
							bagDetailAssociation.setPaxRefID(paxType);
						}
						
						// 品牌code
						associationsArry.addNewPriceClass().setPriceClassReference(code);
						associationsArry.addNewIncludedService().setBundleReference(code + cabin);
						if ("Y".equals(isseatfree)) {
							seatSet.add(code + cabin);
						}
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
						brandinfosRet.addParm("suffix", suffix);
						brandinfosRet.addParm("isoCode", CURRENCYCODE);
						brandinfosRet.addParm("flightDay", flightDay);
						offeritemRet.addParm(offeritemId, brandinfosRet);
						// 将航班信息放入list中
						brandinfoList.add(brandinfosRet);
						/* 旅客机票价格和税费结果 ***************************************************/
						String refundid = refunded.getParm("id").getStringColumn();
						refundMap.put(refundid, refunded);
						Map<String, BigDecimal> adtVatMap = new HashMap<String, BigDecimal>();
						Map<String, BigDecimal> chdVatMap = new HashMap<String, BigDecimal>();
						Map<String, BigDecimal> infVatMap = new HashMap<String, BigDecimal>();
						Map<String, CommandData> vatNameDataMap = new HashMap<>();
						// 税信息
						Table taxsTable = fare.getColumn("tax").getTableColumn();
						// 费信息
						Table feesTable = fare.getColumn("fee").getTableColumn();
						vatProcess(taxsTable,feesTable,adtVatMap,chdVatMap,infVatMap,vatNameDataMap);
						FareDetailType fareDetailArry = offerPriceArry.addNewFareDetail();
						setPassengerArry(fareDetailArry, fare, refundid, ADT, "",adtVatMap,vatNameDataMap);
						setPassengerArry(fareDetailArry, fare, refundid, CHD, chdprice,chdVatMap,vatNameDataMap);
						setPassengerArry(fareDetailArry, fare, refundid, INF, infprice,infVatMap,vatNameDataMap);
						// 费率
						fareDetailArry.addNewRemarks().addNewRemark().setStringValue(vatRate);
						ServiceBundleMap.put(code + cabin, freeCodeS.toString());
					}
					/* 添加品牌节点 *************************************************************/
					addPriceClass(priceClassSArry, code, name, describe);
				}
				// 国际国内标示
				String flightRoute = flightsRow.getColumn("flightRoute").getStringColumn().toUpperCase();
				offerRet.addParm("flightRoute", flightRoute);
				offerRet.addParm(shoppingID + offerID, offeritemRet);
				String codes = cabinCode.toString();
				addFlightSegment(flightSegmentListArry, flightsRow, codes.substring(0, codes.length() - 1), SEG);
				segment.append("SEG" + SEG);
				segment.append(" ");
				SEG++;
			}
		}
		/* 行程关联的航班号 ***************************************************************/
		/* Flight flightArry =flightListArry.addNewFlight(); */
		/*
		 * flightArry.setFlightKey("G".equals(routtype)?OW:RT);
		 * flightArry.addNewSegmentReferences().setStringValue(segment.toString(
		 * ).trim());
		 */
		// 拼接航班编号
		StringBuffer fltS = new StringBuffer();
		String flightids = segment.toString().trim();
		if (!"".equals(flightids) && null != flightids) {
			String[] fltTable = segment.toString().trim().split(" ");
			for (String flt : fltTable) {
				Flight flightArry = flightListArry.addNewFlight();
				flightArry.setFlightKey("FLT" + FLTNUM);
				flightArry.addNewSegmentReferences().setStringValue(flt);
				fltS.append("FLT" + FLTNUM + " ");
				FLTNUM++;
			}
		}
		/* OD与行程的关联 ****************************************************************/
		org.iata.iata.edist.OriginDestinationDocument.OriginDestination od = originArry.addNewOriginDestination();
		od.addNewDepartureCode().setStringValue(oricode);
		od.addNewArrivalCode().setStringValue(destcode);
		od.addNewFlightReferences().setStringValue(fltS.toString().trim());
		/* 儿童婴儿的退改规则 **************************************************************/
		/*
		 * //儿童退改规则 CommandData chdData =
		 * ret.getParm("refundedchd").getObjectColumn(); String chdid =
		 * chdData.getParm("id").getStringColumn(); refundMap.put(chdid,
		 * chdData); //婴儿退改规则 CommandData infData =
		 * ret.getParm("refundedinf").getObjectColumn(); String infid =
		 * infData.getParm("id").getStringColumn(); refundMap.put(infid,
		 * infData);
		 */
		/* 将航班数据CommandRet放入Redis缓存里 *****************************************/
		
	}

	/**
	 * 计算品牌和fee中的vat（Map中key值是vat code，value是指当前旅客类型品牌和fee关联的同一种vat值的总和）
	 * @param taxsTable
	 * @param feesTable
	 * @param adtVatMap
	 * @param chdVatMap
	 * @param infVatMap
	 */
	private void vatProcess(Table taxsTable, Table feesTable,
			Map<String, BigDecimal> adtVatMap, Map<String, BigDecimal> chdVatMap,
			Map<String, BigDecimal> infVatMap, Map<String, CommandData> vatNameDataMap) {
		//品牌的税
		if(taxsTable != null && taxsTable.getRowCount() > 0){
			for (int i = 0; i < taxsTable.getRowCount(); i++) {
				Row taxRow = taxsTable.getRow(i);
				String calcBasis = taxRow.getColumn("calcBasis").getStringColumn();
				if(StringUtils.hasLength(calcBasis) && "A".equals(calcBasis)){
					String vatCode = taxRow.getColumn("code").getStringColumn();
					CommandData vatNameData = taxRow.getColumn("name").getObjectColumn();
					vatNameDataMap.put(vatCode, vatNameData);
					String adtCharge = taxRow.getColumn("adtCharge").getStringColumn();
					String chdCharge = taxRow.getColumn("chdCharge").getStringColumn();
					String infCharge = taxRow.getColumn("infCharge").getStringColumn();
					adtVatMap.put(vatCode, strToBigDec(adtCharge));
					chdVatMap.put(vatCode, strToBigDec(chdCharge));
					infVatMap.put(vatCode, strToBigDec(infCharge));
				}
			}
		}
		//fee的税
		if(feesTable != null && feesTable.getRowCount() > 0){
			for (int j = 0; j < feesTable.getRowCount(); j++) {
				Row feeRow = feesTable.getRow(j);
				Table feeTaxTable = feeRow.getColumn("tax").getTableColumn();
				if(feeTaxTable != null && feeTaxTable.getRowCount() > 0)
					for (int i = 0; i < feeTaxTable.getRowCount(); i++) {
						Row taxRow = feeTaxTable.getRow(i);
						String calcBasis = taxRow.getColumn("calcBasis").getStringColumn();
						if(StringUtils.hasLength(calcBasis) && "A".equals(calcBasis)){
							String vatCode = taxRow.getColumn("code").getStringColumn();
							String adtCharge = taxRow.getColumn("adtCharge").getStringColumn();
							String chdCharge = taxRow.getColumn("chdCharge").getStringColumn();
							String infCharge = taxRow.getColumn("infCharge").getStringColumn();
							//ADT
							if(adtVatMap != null && adtVatMap.size() > 0){
								if(adtVatMap.containsKey(vatCode)){
									BigDecimal bigDecimal = adtVatMap.get(vatCode);
									adtVatMap.put(vatCode, bigDecimal.add(strToBigDec(adtCharge)));
								}else{
									adtVatMap.put(vatCode, strToBigDec(adtCharge));
								}
							}else{
								adtVatMap.put(vatCode, strToBigDec(adtCharge));
							}
							//CHD
							if(chdVatMap != null && chdVatMap.size() > 0){
								if(chdVatMap.containsKey(vatCode)){
									BigDecimal bigDecimal = chdVatMap.get(vatCode);
									chdVatMap.put(vatCode, bigDecimal.add(strToBigDec(chdCharge)));
								}else{
									chdVatMap.put(vatCode, strToBigDec(chdCharge));
								}
							}else{
								chdVatMap.put(vatCode, strToBigDec(chdCharge));
							}
							//INF
							if(infVatMap != null && infVatMap.size() > 0){
								if(infVatMap.containsKey(vatCode)){
									BigDecimal bigDecimal = infVatMap.get(vatCode);
									infVatMap.put(vatCode, bigDecimal.add(strToBigDec(infCharge)));
								}else{
									infVatMap.put(vatCode, strToBigDec(infCharge));
								}
							}else{
								infVatMap.put(vatCode, strToBigDec(infCharge));
							}
						}
					}
				}
			}
	}
	
	/**
	 * String 转 BigDecimal
	 * @param chargePrice
	 * @return
	 */
	private BigDecimal strToBigDec(String chargePrice){
		BigDecimal bigDecimal = new BigDecimal(chargePrice);
		return bigDecimal;
	}

	/**
	 * 旅客机票价格和税费结果
	 * 
	 * @param offerPriceArry
	 *            节点
	 * @param fare
	 *            舱位数据
	 * @param refundid
	 *            退改规则id
	 * @param passengerKey
	 *            旅客类型
	 */
	public void setPassengerArry(FareDetailType fareDetailArry, Row fare, String refundid, String passengerKey,
			String price,Map<String, BigDecimal> vatMap,Map<String, CommandData> vatNameDataMap) {
		FareComponentType fareComponentArry = fareDetailArry.addNewFareComponent();
		fareComponentArry.addNewFareBasis().addNewFareBasisCode().setCode("YPLUSECO-" + passengerKey);
		/* fareComponentArry.setRefs(getList(passengerKey)); */
		FarePriceDetailType priceArry = fareComponentArry.addNewPriceBreakdown().addNewPrice();
		// 销售舱位价格
		CurrencyAmountOptType BaseAmount = priceArry.addNewBaseAmount();
		// 币种
		/*
		 * String currencyCode
		 * =fare.getColumn("currencyCode").getStringColumn();
		 */
		BaseAmount.setCode(CURRENCYCODE);
		// 舱位价格
		price = ADT.equals(passengerKey) ? fare.getColumn("price").getStringColumn() : price;
		BaseAmount.setStringValue("".equals(price) ?"0" : price);
		// 税和费
		Breakdown breakdown = priceArry.addNewTaxes().addNewBreakdown();
		// 税信息
		Table taxsTab = fare.getColumn("tax").getTableColumn();
		if (null != taxsTab) {
			setTaxesArry(breakdown, taxsTab, passengerKey);
		}
		// 费信息
		Table feesTab = fare.getColumn("fee").getTableColumn();
		if (null != feesTab) {
			setTaxesArry(breakdown, feesTab, passengerKey);
		}
		setVatTaxesArry(breakdown,vatMap,vatNameDataMap);
		// 关联的退改规则id
		fareComponentArry.addNewFareRules().addNewPenalty().setRefs(getList(refundid));
	}
	
	/**
	 * 拼接vat
	 * @param breakdown
	 * @param vatMap
	 * @param vatNameDataMap
	 */
	private void setVatTaxesArry(Breakdown breakdown,
			Map<String, BigDecimal> vatMap,
			Map<String, CommandData> vatNameDataMap) {
		if(vatMap != null && vatMap.size() > 0){
			for (Entry<String, BigDecimal> entry : vatMap.entrySet()) {
				String vatCode = entry.getKey();
				BigDecimal vatBigDecimal = entry.getValue();
				Tax newTax = breakdown.addNewTax();
				CurrencyAmountOptType newAmount = newTax.addNewAmount();
				newAmount.setStringValue(vatBigDecimal.toString());
				newAmount.setCode(CURRENCYCODE);
				newTax.setTaxCode(vatCode);
				newTax.setDescription(StatusUtil.getLanguageName(vatNameDataMap.get(vatCode), language));
			}
		}
	}

	/**
	 * 税费数据
	 * 
	 * @param priceDetailArry
	 * @param taxsTab
	 */
	public void setTaxesArry(Breakdown breakdown, Table tab, String paxtype) {
		for (Row Row : tab) {
			String calcBasis = Row.getColumn("calcBasis").getStringColumn();
			if(!"A".equals(calcBasis)){
				String chargeKey = "";
				if (INF.equals(paxtype)) {
					chargeKey = "infCharge";
				} else if (CHD.equals(paxtype)) {
					chargeKey = "chdCharge";
				} else {
					chargeKey = "adtCharge";
				}
				// 价格
				String price = Row.getColumn(chargeKey).getStringColumn();
				// 税费名称
				String taxname = StatusUtil.getLanguageName(Row.getColumn("name").getObjectColumn(), language);
				// 税费类型
				String taxcode = Row.getColumn("code").getStringColumn();
				Tax tax = breakdown.addNewTax();
				CurrencyAmountOptType amount = tax.addNewAmount();
				amount.setStringValue(price);
				amount.setCode(CURRENCYCODE);
				tax.setTaxCode(taxcode);
				tax.setDescription(taxname);
			}
		}
	}

	/**
	 * 旅客信息
	 * 
	 * @param travelerList
	 *            旅客节点
	 */
	public void addTraveler(AnonymousTravelerList travelerList) {
		// 成人数据
		int adt = null == travelerMap.get(ADT) ? 0 : Integer.valueOf(travelerMap.get(ADT));
		AnonymousTravelerType travelerTypeADT = travelerList.addNewAnonymousTraveler();
		travelerTypeADT.setObjectKey(ADT);
		PTC ptcADT = travelerTypeADT.addNewPTC();
		ptcADT.setQuantity(BigInteger.valueOf(adt));
		ptcADT.setStringValue(ADT);
		// 儿童数据
		int chd = null == travelerMap.get(CHD) ? 0 : Integer.valueOf(travelerMap.get(CHD));
		AnonymousTravelerType travelerTypeCHD = travelerList.addNewAnonymousTraveler();
		travelerTypeCHD.setObjectKey(CHD);
		PTC ptcCHD = travelerTypeCHD.addNewPTC();
		ptcCHD.setQuantity(BigInteger.valueOf(chd));
		ptcCHD.setStringValue(CHD);
		// 婴儿数据
		int inf = null == travelerMap.get(INF) ? 0 : Integer.valueOf(travelerMap.get(INF));
		AnonymousTravelerType travelerTypeINF = travelerList.addNewAnonymousTraveler();
		travelerTypeINF.setObjectKey(INF);
		PTC ptcINF = travelerTypeINF.addNewPTC();
		ptcINF.setQuantity(BigInteger.valueOf(inf));
		ptcINF.setStringValue(INF);
	}

	/**
	 * 航班数据
	 * 
	 * @param flightSegmentListArry
	 * @param flightsRow
	 * @param cabins
	 * @throws ParseException
	 */
	public void addFlightSegment(FlightSegmentList flightSegmentListArry, Row flightsRow, String cabins, int seg)
			throws ParseException {
		/*
		 * //航班id String flightid =
		 * flightsRow.getColumn("flightid").getStringColumn();
		 */
		// 航司二字码
		String airlinecd = flightsRow.getColumn("airlinecd").getStringColumn();
		// 航班号
		String flightno = flightsRow.getColumn("flightno").getStringColumn();
		// 机型
		String planestype = flightsRow.getColumn("planestype").getStringColumn();
		// 出发机场三字码
		String oricode = flightsRow.getColumn("oricode").getStringColumn();
		// 到达机场三字码
		String destcode = flightsRow.getColumn("destcode").getStringColumn();
		// 到达航班天
		String dayChange = flightsRow.getColumn("dayChange").getStringColumn();
		// 出发日期
		String oriDay = flightsRow.getColumn("oriDay").getStringColumn();
		// 出发时间
		//Date oridateS = flightsRow.getColumn("oriDateTime").getDateColumn("yyyyMMdd HH:mm");
		String oriTime = flightsRow.getColumn("oriTime").getStringColumn();
		// 到达日期
		String destDateTime = flightsRow.getColumn("destDateTime").getStringColumn();
		// 到达时间
		//Date destdateS = flightsRow.getColumn("destDateTime").getDateColumn("yyyyMMdd HH:mm");
		String destTime = flightsRow.getColumn("destTime").getStringColumn();
		// 出发航站楼
		String oriterminal = flightsRow.getColumn("oriterminal").getStringColumn().toUpperCase();
		// 到达航站楼
		String destterminal = flightsRow.getColumn("destterminal").getStringColumn().toUpperCase();
		// 国际国内标示
		String flightRoute = flightsRow.getColumn("flightRoute").getStringColumn().toUpperCase();
		// 航班后缀
		String suffix = flightsRow.getColumn("suffix").getStringColumn();
		ListOfFlightSegmentType flightArry = flightSegmentListArry.addNewFlightSegment();
		flightArry.setSegmentKey("SEG" + seg);
		/* 出发航班信息 **************************************************/
		Departure departureArry = flightArry.addNewDeparture();
		departureArry.addNewAirportCode().setStringValue(oricode);
		if (oriDay != null && !"".equals(oriDay)) {
			Calendar oridate = DateUtils.getInstance().parseDate(oriDay, "yyyyMMdd");
			if (oridate != null) {
				departureArry.setDate(oridate);
				departureArry.setTime(oriTime);
			}
		}

		// 出发机场名称
		departureArry.setAirportName(StatusUtil.getLanguageName(flightsRow.getColumn("oriname").getObjectColumn(),
				ApiServletHolder.getApiContext().getLanguage()));
		departureArry.addNewTerminal().setName(oriterminal);
		/* 到达航班信息 **************************************************/
		FlightArrivalType arrivalArry = flightArry.addNewArrival();
		arrivalArry.addNewAirportCode().setStringValue(destcode);
		if (destDateTime != null && !"".equals(destDateTime) && destDateTime.length() >= 10) {
			destDateTime = destDateTime.substring(0, 10);
			Calendar destDate = DateUtils.getInstance().parseDate(destDateTime, "yyyy-MM-dd");
			arrivalArry.setDate(destDate);
			arrivalArry.setTime(destTime);
		}
		// 航班天
		arrivalArry.setChangeOfDay(new BigInteger(dayChange));
		// 到达机场名称
		arrivalArry.setAirportName(StatusUtil.getLanguageName(flightsRow.getColumn("destname").getObjectColumn(),
				ApiServletHolder.getApiContext().getLanguage()));
		arrivalArry.addNewTerminal().setName(destterminal);
		/* 航班以及舱位数据 ***********************************************/
		MarketingCarrierFlightType mcFlightArry = flightArry.addNewMarketingCarrier();
		mcFlightArry.addNewAirlineID().setStringValue(airlinecd);
		FlightNumber flightNumberArry = mcFlightArry.addNewFlightNumber();
		// 航班后缀
		flightNumberArry.setOperationalSuffix(suffix);
		flightNumberArry.setStringValue(flightno);
		// mcFlightArry.setResBookDesigCode(cabins);
		/* 机型 ******************************************************/
		flightArry.addNewEquipment().addNewAircraftCode().setStringValue(planestype);
		/* 飞行数据 ***************************************************/
		FlightDetailType flightDetailArry = flightArry.addNewFlightDetail();
		FlightDistanceType fdArry = flightDetailArry.addNewFlightDistance();
		// 飞行距离
		long mileage = flightsRow.getColumn("mileage").getLongColumn();
		fdArry.setValue(BigInteger.valueOf(mileage));
		fdArry.setUOM("KM");
		//long l = destdateS.getTime() - oridateS.getTime();
		// 飞行时间
//		long hour = (l / (60 * 60 * 1000));
//		long min = ((l / (60 * 1000)) - hour * 60);
//		long s = (l / 1000 - hour * 60 * 60 - min * 60);
//		BigDecimal fraction = new BigDecimal(0);
		// 飞行时间重新计算方式
		long l = flightsRow.getColumn("travelTime").getLongColumn();
		long hour = l / 60;
		long min = l %60;
		long s = 0;
		BigDecimal fraction = new BigDecimal(0);
		GDuration gDuration = new GDuration(1, 0, 0, 0, Integer.parseInt(String.valueOf(hour)),
				Integer.parseInt(String.valueOf(min)), Integer.parseInt(String.valueOf(s)), fraction);
		flightDetailArry.addNewFlightDuration().setValue(gDuration);
		// 国内国际标示
		flightDetailArry.addNewFlightSegmentType().setLink(flightRoute);
		// 经停次数
		Stops stopsArry = flightDetailArry.addNewStops();
		Table stops = flightsRow.getColumn("passby").getTableColumn();
		int stpnum = 0;
		if (null != stops && !"".equals(stops)) {
			stpnum = stops.getRowCount();
			stopsArry.setStopQuantity(BigInteger.valueOf(stpnum));
			StopLocationType stopLocationType = stopsArry.addNewStopLocations();
			for (Row stop : stops) {
				// 经停机场Name
				String airName = StatusUtil.getLanguageName(stop.getColumn("pbAirport").getObjectColumn(), language);
				// 经停机场三字码
				String airCode = stop.getColumn("pbCode").getStringColumn();
				// 经停机场到达时间
				String pbStart = stop.getColumn("pbStart").getStringColumn();
				// 经停机场起飞时间
				String pbEnd = stop.getColumn("pbEnd").getStringColumn();
				StopLocation stopLocationArry = stopLocationType.addNewStopLocation();
				stopLocationArry.addNewAirportCode().setStringValue(airCode);
				stopLocationArry.setName(airName);
				// 经停机场到达日期
				String pbStartDate = stop.getColumn("pbStartDate").getStringColumn();
				Calendar beginDate = null;
				if (pbStartDate != null && !"".equals(pbStartDate)) {
					beginDate = DateUtils.getInstance().parseDate(pbStartDate, "yyyy-MM-dd");
				}
				stopLocationArry.setArrivalDate(beginDate);
				stopLocationArry.setArrivalTime(pbStart);
				// 经停机场出发日期
				String pbEndDate = stop.getColumn("pbEndDate").getStringColumn();
				Calendar endDate = null;
				if (pbEndDate != null && !"".equals(pbEndDate)) {
					endDate = DateUtils.getInstance().parseDate(pbEndDate, "yyyy-MM-dd");
				}
				stopLocationArry.setDepartureDate(endDate);
				stopLocationArry.setDepartureTime(pbEnd);
			}
		}
	}

	/**
	 * 退改规则
	 * 
	 * @param PenaltyListArry
	 *            节点
	 */
	public void addPenaltyListArry(ListOfOfferPenaltyType penaltyListArry) {
		for (Entry<String, CommandData> refundEntry : refundMap.entrySet()) {
			String refundId = refundEntry.getKey();
			CommandData refundData = refundEntry.getValue();
			Penalty PenaltyArry = penaltyListArry.addNewPenalty();
			PenaltyArry.setObjectKey(refundId);
			PenaltyArry.addNewApplicableFeeRemarks().addNewRemark().setStringValue(JsonUnit.toJson(refundData));
			if (!CHD.equals(refundId) && !INF.equals(refundId)) {
				refundId = ADT;
			}
			/* PenaltyArry.addNewDetails().addNewDetail().setType(refundId); */
		}
	}

	/**
	 * 品牌节点
	 * 
	 * @param priceClassSArry
	 *            节点
	 * @param code
	 *            品牌id
	 * @param name
	 *            品牌名称
	 * @param text
	 *            品牌描述
	 */
	public void addPriceClass(ListOfPriceClassType priceClassSArry, String code, String name, String text) {
		boolean flage = true;
		PriceClassType[] PriceType = priceClassSArry.getPriceClassArray();
		if (null != PriceType) {
			// 重复判断
			for (PriceClassType price : PriceType) {
				flage = code.equals(price.getObjectKey()) ? false : true;
				if (!flage) {
					break;
				}
			}
		}
		if (flage) {
			PriceClassType priceClassArry = priceClassSArry.addNewPriceClass();
			priceClassArry.setObjectKey(code);
			priceClassArry.setName(name);
			priceClassArry.addNewDescriptions().addNewDescription().addNewText().setStringValue(text);
		}
	}

	/**
	 * 辅营与品牌关联的节点
	 * 
	 * @param listOfServiceBundleArry
	 */
	public void addServiceBundleListArry(ListOfServiceBundleType listOfServiceBundleArry) {
		for (Entry<String, String> freeEntry : ServiceBundleMap.entrySet()) {
			// 辅营id
			String freeid = freeEntry.getValue().trim();
			if (!"".equals(freeid) && null != freeid) {
				ServiceBundle serviceBundleArry = listOfServiceBundleArry.addNewServiceBundle();
				String[] freeidS = freeid.split(" ");
				// 商品 id
				String brandid = freeEntry.getKey();
				serviceBundleArry.setListKey(brandid);
				serviceBundleArry.setItemCount(BigInteger.valueOf(freeidS.length));
				Associations associationsArry = serviceBundleArry.addNewAssociations();
				for (String id : freeidS) {
					associationsArry.addNewServiceReference().setStringValue(id + _FOR_FREE);
				}
			}
		}
	}

	/**
	 * 辅营信息节点添加
	 * 
	 * @param serviceListArry
	 *            辅营节点
	 */
	public void addServiceListArry(ServiceList serviceListArry) {
		for (Entry<String, Row> freeEntry : freeMap.entrySet()) {
			Row freeRow = freeEntry.getValue();
			// 辅营code
			String freeid = freeEntry.getKey();
			// 航司二字码
			String airlinecd = freeRow.getColumn("airlinecd").getStringColumn();
			// 辅营名称
			String name = freeRow.getColumn("name").getStringColumn();
			// 服务类型
			String type = freeRow.getColumn("type").getStringColumn();
			// 服务描述
			String describe = freeRow.getColumn("describe").getStringColumn();
			//免费行李重量
			String weight = freeRow.getColumn("weight").getStringColumn();
			/*
			 * //服务ssrtype String
			 * ssrtype=freeRow.getColumn("ssrtype").getStringColumn();
			 */
			/*
			 * //服务ssrinfo String
			 * ssrinfo=freeRow.getColumn("ssrinfo").getStringColumn();
			 */
			/*
			 * //关联的商品 id String
			 * brandid=freeRow.getColumn("brandid").getStringColumn();
			 */
			ServiceDetailType serviceArry = serviceListArry.addNewService();
			ServiceIDType serviceIDArry = serviceArry.addNewServiceID();
			serviceIDArry.setOwner(airlinecd);
			serviceIDArry.setStringValue(freeid + _FOR_FREE);
			serviceArry.addNewName().setStringValue(name);
			serviceArry.addNewEncoding().addNewCode().setStringValue(type);
			serviceArry.addNewDescriptions().addNewDescription().addNewText().setStringValue(describe);
			BookingInstructions bitArry = serviceArry.addNewBookingInstructions();
			bitArry.addNewSSRCode().setStringValue(freeid);
			if("XBAG".equals(type)){
				bitArry.addNewText().setStringValue(weight);
			}
			/*
			 * serviceArry.addNewAssociations().addNewOffer().setOfferReferences
			 * (getList(brandid));
			 */
		}
	}

	/**
	 * 根据Java编程规范：由于for循环中不能创建对象，所有将创建对象放到方法里
	 *
	 * @return CommandData
	 */
	public CommandData getCreateCommandData() {
		return new CommandData();
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

	/**
	 * 设置 商品和免费服关联关系
	 * 
	 * @param freeid
	 *            服务id
	 * @param brandid
	 *            商品id
	 * @param freeRow
	 *            服务内容
	 */
	public void setFreeproduct(String freeid, String brandid, Row freeRow) {
		Row row = freeMap.get(freeid);
		if (!"".equals(row) && null != row) {
			String id = row.getColumn("brandid").getStringColumn().trim();
			int num = id.indexOf(brandid);
			if (num == -1) {
				StringBuffer idBuffer = new StringBuffer("");
				idBuffer.append(id);
				idBuffer.append(" ");
				idBuffer.append(brandid);
				brandid = idBuffer.toString();
			} else {
				brandid = id;
			}
		}
		freeRow.addColumn("brandid", brandid);
		freeMap.put(freeid, freeRow);
	}
}