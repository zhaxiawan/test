package com.travelsky.quick.util.helper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.xmlbeans.GDuration;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirShopReqAttributeQueryType.OriginDestination;
import org.iata.iata.edist.AirShoppingRQDocument;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ.Preference;
import org.iata.iata.edist.AirShoppingRSDocument;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.DataLists;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers.AirlineOffer;
import org.iata.iata.edist.CabinTypeDocument.CabinType;
import org.iata.iata.edist.DataListType.FlightList;
import org.iata.iata.edist.DataListType.FlightList.Flight;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DataListType.OriginDestinationList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightCOSCoreType;
import org.iata.iata.edist.FlightCOSCoreType.MarketingName;
import org.iata.iata.edist.FlightDepartureType.AirportCode;
import org.iata.iata.edist.FlightDetailType.Stops;
import org.iata.iata.edist.FlightDetailType;
import org.iata.iata.edist.FlightDistanceType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.FlightPreferencesType.Characteristic;
import org.iata.iata.edist.FlightSegmentReferenceDocument.FlightSegmentReference;
import org.iata.iata.edist.ItemIDType;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfPriceClassType;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.OfferPriceLeadType.RequestedDate;
import org.iata.iata.edist.PriceClassType;
import org.iata.iata.edist.PricedFlightOfferAssocType;
import org.iata.iata.edist.StopLocationType;
import org.iata.iata.edist.PricedFlightOfferType.OfferPrice;
import org.iata.iata.edist.PricedOfferDocument.PricedOffer;
import org.iata.iata.edist.StopLocationType.StopLocation;
import org.iata.iata.edist.TransferPreferencesType.Connection;
import org.iata.iata.edist.TransferPreferencesType.Connection.Codes.Code;
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

public class APIAirShoppingNDCDA {
	/**
	 * 
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(APIAirShoppingNDCDA.class);
	// 获取语言
	private String language = ApiServletHolder.getApiContext().getLanguage();
	//Redis缓存限制时间（单位 秒）
	private static final int TIME = 2400;
	// DA请求标记
	private static final String DA = "DA";
	// 成人标记符
	private static final String ADT = "ADT";
	// 婴儿标记符
	private static final String INF = "INF";
	// 儿童标记符
	private static final String CHD = "CHD";
	// shopping请求ID
	private static final String SHOPPINGAPI = "SHOPPING.API.";
	// offerID
	private static final String OFFERID = "OFFERID.";
	//航班排序
	private int SEG = 1;
	private int FLTNUM = 1;
	/**
	 *旅客数量
	 *String 旅客类型   
	 *String 数量
	 */
	private  Map<String,String>  travelerMap= new  HashMap<String,String>();
	/**
	 * 航班数据集合
	 * CommandRet 航班数据
	 */
	private  List<CommandRet> brandinfoList =new ArrayList<CommandRet>();
	/**
	 *退改规则
	 *String 规则id   
	 *CommandData 规则信息
	 */
	private  Map<String,CommandData>  refundMap= new  HashMap<String,CommandData>();
	
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
	public void transInputXmlToRequestBean(SelvetContext<ApiContext> context) throws APIException, Exception {
		CommandData input = context.getInput();
		String xmlInput = context.getContext().getReqXML();
		AirShoppingRQDocument rootDoc = AirShoppingRQDocument.Factory.parse(xmlInput);
		AirShoppingRQ shoppingRQ = rootDoc.getAirShoppingRQ();
		
		input.addParm("isoCode", "");
		//获取旅客类型以及数量
		travelerMap.put(ADT, "1");
		//shopping查询数据
		OriginDestination[] originDestination = shoppingRQ.getCoreQuery().getOriginDestinations().getOriginDestinationArray();
		Preference preferenceArry = shoppingRQ.getPreference();
		getCoreShopping(originDestination,preferenceArry,input);
		
	}
	
	/**
	 *  shopping查询数据
	 * @param originDestination 航班数据
	 * @param input  请求参数添加
	 * @throws APIException 
	 */
	public  void getCoreShopping(OriginDestination[] originDestination,Preference preferenceArry,CommandData input) throws APIException{
		SimpleDateFormat adf = DateUtils.getInstance().getSimDate("yyyyMMdd");
		int originNum = originDestination.length;
		for(int i=0;i<originNum;i++){
			CommandData commandData = getCreateCommandData();
			AirportCode dacodeArry = originDestination[i].getDeparture().getAirportCode();
			//出发地三字码
			String depart =dacodeArry.getStringValue();
			if (!StringUtils.hasLength(depart)) {
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_FLIGHT_ORG, language));
				throw APIException
						.getInstance(ErrCodeConstants.API_NULL_FLIGHT_ORG);
			}
			//出发时间
//			Date deptdate =originDestination[i].getDeparture().getDate().getTime();
			Date deptdate = null;
			if (originDestination[i] != null && originDestination[i].getDeparture() != null) {
				try {
					 originDestination[i].getDeparture().getDate();
				} catch (Exception e) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_CONVERT_FLIGHTORG_TIME);

				}
				if (originDestination[i].getDeparture().getDate()==null) {
					LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHTORG_TIME, language));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHTORG_TIME);
				}else {
					deptdate=originDestination[i].getDeparture().getDate().getTime();
				}
			}
			org.iata.iata.edist.FlightArrivalType.AirportCode aacode = originDestination[i].getArrival().getAirportCode();
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
			Characteristic ctArry = preferenceArry.getFlightPreferences().getCharacteristic();
			//是否只查经停(0表示不包含经停，1 表示包含经停)
			String nonStop = ctArry.getNonStopPreferences().getStringValue();
			commandData.addParm("nonStop", nonStop);
			//是否只查直达(0表示查所有，1 表示仅查询直达)
			String direct = ctArry.getDirectPreferences().getStringValue();
			commandData.addParm("direct", direct);
			/***********************************************/
			Connection  connectionArry = preferenceArry.getTransferPreferences().getConnection();
			//指定出发机场的三字码
			Code[] codeTable = connectionArry.getCodes().getCodeArray();
			if(null !=codeTable && codeTable.length >0 ){
				commandData.addParm("connection", codeTable[0].getStringValue());
				//经停次数
				long maxNumber = connectionArry.getMaxNumber().longValue();
				commandData.addParm("maxNumber", maxNumber);
			}
			/**********************************************/
			CabinType[] cabinTabe = preferenceArry.getCabinPreferences().getCabinTypeArray();
			if(null != cabinTabe&&cabinTabe.length>0){
				//舱位
				String cabinType = cabinTabe[0].getCode();
				commandData.addParm("cabinType", cabinType);
			}
			input.addParm(DA, commandData);
		}
	}

	/**
	 * 获取币种
	 * @param isoCode
	 * @param l_ret
	 * @return
	 */
	public String getIsoCode(String isoCode,CommandRet l_ret){
		if (!"".equals(l_ret.getErrorCode())){
			return "";
		}else{
			String isoCodeOne = "";
			Table currencys = l_ret.getParm("currencyInfoQuery").getTableColumn();
			for(Row currency : currencys){
				String currCoce = currency.getColumn("isoCode").getStringColumn();
				//1 默认币种 0非默认币种
				String status = currency.getColumn("editStatus").getStringColumn();
				if(isoCode.equals(currCoce)){
					return currCoce;
				}
				if("1".equals(status)){
					isoCodeOne = currCoce;
				}
			}
			return isoCodeOne;	
		}
	}
	
/*********************************************************************************************/	
	/**
	 * 转换ResponseBean-->XmlBean
	 * @param commandRet
	 * @param input
	 * @return
	 */
	public  XmlObject transRespBeanToXmlBeanDA(CommandRet commandRet, CommandData input) {
		CommandRet xmlOutput = commandRet;
		AirShoppingRSDocument doc = AirShoppingRSDocument.Factory.newInstance();
		AirShoppingRS rs = doc.addNewAirShoppingRS();
		try{
			if (!"".equals(xmlOutput.getErrorCode())) {
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(xmlOutput.getErrorCode()));
				error.setShortText(TipMessager.getMessage(xmlOutput.getErrorCode(),
						ApiServletHolder.getApiContext().getLanguage()));
				return doc;
			}
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
			//航班信息  DataLists——》FlightSegmentList
			FlightSegmentList flightSegmentListArry = dataListsArry.addNewFlightSegmentList();
			//去程回程 关联航班信息  DataLists——》FlightList
			FlightList  flightListArry = dataListsArry.addNewFlightList();
			//航班查询的出发地 DataLists——》OriginDestinationList
			OriginDestinationList originArry = dataListsArry.addNewOriginDestinationList();
			//品牌信息DataLists——》ListOfPriceClassType
			ListOfPriceClassType priceClassSArry = dataListsArry.addNewPriceClassList();
			/****************************************************************/
			CommandRet retOW = new CommandRet("");
			if (xmlOutput.getParm(DA).getObjectColumn() != null) {
				retOW = (CommandRet)xmlOutput.getParm(DA).getObjectColumn();
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
				setRedisManagerflights(retOW,shoppingID+"."+"OW","G"
						,airlineOffers,flightSegmentListArry,flightListArry,originArry,priceClassSArry);
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
	 * 将航班数据放入CommandRet里
	 * @param ret shopping数据
	 * @param shoppingRet redis缓存command
	 * @param shoppingID 请求id
	 * @param routtype 往返标记
	 * @param airlineOffers 航班节点 OffersGroup——》AirlineOffers
	 * @param flightSegmentListArry 航班数据节点 DataLists——》FlightSegmentList
	 * @param flightListArry  去程回程 关联航班信息  DataLists——》FlightList
	 * @param originArry 航班查询的出发地 DataLists——》originArry
	 * @param priceClassSArry 品牌信息DataLists——》ListOfPriceClassType
	 * @throws ParseException 
	 */
	public  void  setRedisManagerflights(CommandRet ret,
		String shoppingID,String routtype,AirlineOffers airlineOffers,
		FlightSegmentList flightSegmentListArry,FlightList flightListArry
		,OriginDestinationList originArry,ListOfPriceClassType priceClassSArry) throws ParseException{
		//航班信息
		Table  flightsTable = ret.getParm("flights").getTableColumn();
		//拼接航班编号
		StringBuffer segment =new StringBuffer();
		CommandRet offerRet = new CommandRet("");
		//出发机场三字码
		String oricode ="";
		//到达机场三字码
		String destcode ="";
		for(Row flightsRow :flightsTable){
			StringBuffer cabinCode = new StringBuffer();
			CommandRet offeritemRet = new CommandRet("");
			String flightDay = flightsRow.getColumn("flightDay").getStringColumn();
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
			airlineOffer.addNewParameters();
			//AirlineOffer->OfferID
			ItemIDType OfferIDArry = airlineOffer.addNewOfferID();
			OfferIDArry.setOwner(airlinecd);
			OfferIDArry.setStringValue(shoppingID+offerID);
			//AirlineOffer->PricedOffer
			PricedOffer pricedOfferArry = airlineOffer.addNewPricedOffer();
			Table brandinfosTab = flightsRow.getColumn("brandinfos").getTableColumn();
			for(Row brandinfosRow :brandinfosTab){
				//品牌code
				String code = brandinfosRow.getColumn("id").getStringColumn();
				//品牌name
				String name = brandinfosRow.getColumn("name").getStringColumn();
				//品牌描述
				String describe = brandinfosRow.getColumn("describe").getStringColumn();
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
					org.iata.iata.edist.FlightCOSCoreType.Code codeArry = flightCOSCoreArry.addNewCode();
					codeArry.setSeatsLeft(cabinnumber);
					codeArry.setStringValue(cabin);
					MarketingName mNameArry = flightCOSCoreArry.addNewMarketingName();
					//舱位名称
					String basicname = StatusUtil.getLanguageName(fare.getColumn("basicname").getObjectColumn(),language);
					mNameArry.setStringValue(basicname);
					mNameArry.setCabinDesignator(basiccabin);
					//品牌code
					associationsArry.addNewPriceClass().setPriceClassReference(code);
					CommandRet brandinfosRet = new CommandRet("");
					flightsRow.copyTo(brandinfosRet, true);
					brandinfosRet.addParm("id", code);
					brandinfosRet.addParm("basiccabin", basiccabin);
					brandinfosRet.addParm("familycode", cabin);
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
					/*旅客机票价格和税费结果***************************************************/
					String refundid = refunded.getParm("id").getStringColumn();
					refundMap.put(refundid, refunded);
				}
				/*添加品牌节点*************************************************************/
				addPriceClass(priceClassSArry,code,name,describe);
			}
			offerRet.addParm(shoppingID+offerID,offeritemRet);
			String codes = cabinCode.toString();
			addFlightSegment(flightSegmentListArry,flightsRow,codes.substring(0,codes.length()-1),SEG);
			segment.append("SEG"+SEG);
			segment.append(" ");
			SEG++;
		}
		/*行程关联的航班号***************************************************************/
	/*	Flight flightArry =flightListArry.addNewFlight();
		flightArry.setFlightKey("G".equals(routtype)?"OW":"RT");
		flightArry.addNewSegmentReferences().setStringValue(segment.toString().trim());*/
		StringBuffer fltS =new StringBuffer();
		String flightids = segment.toString().trim();
		if(!"".equals(flightids) && null !=flightids){
			String[] fltTable=segment.toString().trim().split(" ");
			for(String flt:fltTable){
				Flight flightArry =flightListArry.addNewFlight();
				flightArry.setFlightKey("FLT"+FLTNUM);
				flightArry.addNewSegmentReferences().setStringValue(flt);
				fltS.append("FLT"+FLTNUM+" ");
				FLTNUM++;
			}
		}
		/*OD与行程的关联****************************************************************/
		org.iata.iata.edist.OriginDestinationDocument.OriginDestination  od = originArry.addNewOriginDestination();
		od.addNewDepartureCode().setStringValue(oricode);
		od.addNewArrivalCode().setStringValue(destcode);
		od.addNewFlightReferences().setStringValue(fltS.toString().trim());
		RedisManager.getManager().set(RedisNamespaceEnum.api_cache_order.toKey(shoppingID), JsonUnit.toJson(offerRet), TIME);
	}
	
	
	/**
	 * 品牌节点
	 * @param priceClassSArry 节点
	 * @param code  品牌id
	 * @param name  品牌名称
	 * @param text  品牌描述
	 */
	public  void addPriceClass(ListOfPriceClassType priceClassSArry,String code,String name,String text){
		boolean flage = true;
		PriceClassType[] PriceType = priceClassSArry.getPriceClassArray();
		if(null != PriceType){
			//重复判断
			for(PriceClassType price :PriceType){
				flage = code.equals(price.getObjectKey())?false:true;
				if(!flage){
					break;
				}
			}
		}
		if(flage){
			PriceClassType	 priceClassArry = priceClassSArry.addNewPriceClass();
			priceClassArry.setObjectKey(code);
			priceClassArry.setName(name);
			priceClassArry.addNewDescriptions().addNewDescription().addNewText().setStringValue(text);
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
		//机型
		String planestype = flightsRow.getColumn("planestype").getStringColumn();
		//出发机场三字码
		String oricode = flightsRow.getColumn("oricode").getStringColumn();
		//到达机场三字码
		String destcode = flightsRow.getColumn("destcode").getStringColumn();
		//出发日期
		String oriDay = flightsRow.getColumn("oriDay").getStringColumn();
		//出发时间
		Date oridateS = flightsRow.getColumn("oridate").getDateColumn("yyyyMMdd HH:mm:ss");
		String oriTime = flightsRow.getColumn("oriTime").getStringColumn();
		//到达日期
		String destDateTime = flightsRow.getColumn("destDateTime").getStringColumn();
		//到达时间
		Date destdateS = flightsRow.getColumn("destdate").getDateColumn("yyyyMMdd HH:mm:ss");
		String destTime = flightsRow.getColumn("destTime").getStringColumn();
		//出发机场名称
		String oriname = flightsRow.getColumn("oriname").getStringColumn();
		//到达机场名称
		String destname = flightsRow.getColumn("destname").getStringColumn();
		//出发航站楼
		String oriterminal = flightsRow.getColumn("oriterminal").getStringColumn();
		//到达航站楼
		String destterminal = flightsRow.getColumn("destterminal").getStringColumn();
		//航班后缀
		String suffix = flightsRow.getColumn("").getStringColumn();
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
		 mcFlightArry.setResBookDesigCode(cabins);
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
	 * @return list
	 */
	public  List<String > getList(String args) {
		List<String> list = new ArrayList<String>();
		list.add(args);
		return list;
	}

	/**
	 * 根据Java编程规范：由于for循环中不能创建对象，所有将创建对象放到方法里
	 *
	 * @return CommandData
	 */
	public  CommandData getCreateCommandData() {
		return new CommandData();
	}
}

