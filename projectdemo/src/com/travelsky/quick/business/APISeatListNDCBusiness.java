package com.travelsky.quick.business;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.Calendar;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AircraftCodeDocument.AircraftCode;
import org.iata.iata.edist.CabinTypeDocument.CabinType;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.DataListType;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DataListType.SeatList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightArrivalType.AirportCode;
import org.iata.iata.edist.FlightCOSCoreType;
import org.iata.iata.edist.FlightCOSCoreType.Code;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfSeatType;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.OrderIDType;
import org.iata.iata.edist.OrderItemAssociationType;
import org.iata.iata.edist.OrderItemAssociationType.Services;
import org.iata.iata.edist.SeatAvailabilityRQDocument;
import org.iata.iata.edist.SeatAvailabilityRQDocument.SeatAvailabilityRQ;
import org.iata.iata.edist.SeatAvailabilityRSDocument;
import org.iata.iata.edist.SeatAvailabilityRSDocument.SeatAvailabilityRS;
import org.iata.iata.edist.SeatAvailabilityRSDocument.SeatAvailabilityRS.Flights;
import org.iata.iata.edist.SeatAvailabilityRSDocument.SeatAvailabilityRS.Flights.Cabin;
import org.iata.iata.edist.SeatDisplayDocument.SeatDisplay;
import org.iata.iata.edist.SeatDisplayDocument.SeatDisplay.Columns;
import org.iata.iata.edist.SeatDisplayDocument.SeatDisplay.Rows;
import org.iata.iata.edist.SeatLocationType;
import org.iata.iata.edist.SeatLocationType.Characteristics;
import org.iata.iata.edist.SeatLocationType.Characteristics.Characteristic;
import org.iata.iata.edist.ServiceDetailType;
import org.iata.iata.edist.ServiceIDType;
import org.iata.iata.edist.ServiceListDocument.ServiceList;
import org.iata.iata.edist.ServicePriceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
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
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.ShoppingManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 *
 * @author MaRuifu 2016年5月3日下午3:16:01
 * @version 0.1 类说明: 座位信息
 */
@Service("LCC_SEATLIST_SERVICE")
public class APISeatListNDCBusiness extends AbstractService<ApiContext> {

	private static final long serialVersionUID = -6647848094270223053L;

	private static final Logger LOGGER = LoggerFactory.getLogger(APISeatListNDCBusiness.class);

	private static final String CABIN = "cabin";
	private static final String UNDERLINE = "_";

	/**
	 *
	 * @param context
	 *            SelvetContext<ApiContext>
	 * @throws Exception
	 *             Exception
	 */
	@Override
	public void doServlet() throws Exception {
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
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_QUERYSEAT,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}

	}

	/**
	 *
	 * @return CommandRet 返回类型
	 *
	 */
	public CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		ShoppingManager shoppingManager = new ShoppingManager();
		return shoppingManager.seatshopping(input, context);
	}

	/**
	 * 转换 xml-->Reqbean
	 *
	 * @param xmlInput
	 *            String
	 * @param context
	 *            SelvetContext<ApiContext>
	 * @throws APIException
	 *             APIException
	 * @throws Exception
	 *             Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		// 引用xsd文件
		SeatAvailabilityRQDocument rootDoc = null;
		rootDoc = SeatAvailabilityRQDocument.Factory.parse(xmlInput);

		SeatAvailabilityRQ seatAvailabilityRQ = rootDoc.getSeatAvailabilityRQ();
		seatAvailabilityRQ.getDataLists();
		String channelNo = context.getContext().getChannelNo();
		// 渠道号
		input.addParm("channelno", channelNo);
		input.addParm("tktdeptid", context.getContext().getTicketDeptid());
		// 添加用户id orderno
		input.addParm("memberid", context.getContext().getUserID());
//		OrderIDType orderID = seatAvailabilityRQ.getQuery().getOrderID();
//		String owner = orderID.getOwner();
//		input.addParm("owner", owner);
//		String orderno = orderID.getStringValue();
//		if ("".equals(orderno)) {
//			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_ORDER_NO,
//					ApiServletHolder.getApiContext().getLanguage()));
//			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_NO);
//		}
//		input.addParm("orderno", orderno);
		// 得到具体航班节点
		DataListType dataList = seatAvailabilityRQ.getDataLists();
		// 查询航班座位图信息
		queryPassengerSeat2(input, dataList);
	}

	/**
	 * @param input
	 * @param dataList
	 * @throws APIException
	 */
	private void queryPassengerSeat2(CommandData input, DataListType dataList) throws APIException {
		FlightSegmentList segmentList = dataList.getFlightSegmentList();
		ListOfFlightSegmentType array = segmentList.getFlightSegmentArray(0);
		input.addParm("appendflag", "N");
		//品牌id
		String brandid = array.getSegmentKey();
		input.addParm("brandid", brandid == null ? "" : brandid);
		//机型代码
		String planestype = array.getRefs().toString();
		input.addParm("planestype", planestype == null ? "" : planestype);
		Departure departure = array.getDeparture();
		// 出发时区
		String oritimezone = (String) departure.getRefs().get(0);
		input.addParm("oritimezone", oritimezone == null ? "" : oritimezone);
		// 出发三字码
		String oriCode = departure.getAirportCode().getStringValue();
		if (!StringUtils.hasLength(oriCode)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_ORG,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_ORG);
		}
		input.addParm("oriCode", oriCode == null ? "" : oriCode);
		//出发日期
		String oriDay = departure.getDate().toString();
		if (StringUtils.isEmpty(oriDay)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHTORG_STDATE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHTORG_STDATE);
		}
		// 航班天
		String flightDay = oriDay.replaceAll("-", "");
		input.addParm("flightDay", flightDay == null ? "" : flightDay);
		// 出发时间
		String oriDateTime = oriDay + " " + departure.getTime() ;
		input.addParm("oriDateTime", oriDateTime);
		input.addParm("oritime",  departure.getTime() == null ? "" :  departure.getTime());
		FlightArrivalType arrival = array.getArrival();
		// 到达时区
		String desttimezone = (String) arrival.getRefs().get(0);
		input.addParm("desttimezone", desttimezone == null ? "" : desttimezone);
		//到达三字码
		String destCode = arrival.getAirportCode().getStringValue();
		if (!StringUtils.hasLength(destCode)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_DST,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_DST);
		}
		input.addParm("destCode",destCode);
		//到达日期
		String deptDate =arrival.getDate().toString();
		input.addParm("deptDate", deptDate == null ? "" : deptDate);
		//达到时间
		String destDateTime = deptDate + " " + arrival.getTime() ;
		input.addParm("destDateTime", destDateTime == null ? "" : destDateTime);
		MarketingCarrierFlightType carrier = array.getMarketingCarrier();
		// 货币code
		String isoCode = (String) carrier.getRefs().get(0);
		input.addParm("currencyCode", isoCode == null ? "" : isoCode);
		//航司二字码
		String airlineCode = carrier.getAirlineID().getStringValue();
		if (!StringUtils.hasLength(airlineCode)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_MARKET_FLIGHTID,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_MARKET_FLIGHTID);
		}
		input.addParm("airlineCode",  airlineCode);
		//航班号
		String flightNo = carrier.getFlightNumber().getStringValue();
		if (!StringUtils.hasLength(flightNo)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_NO,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_NO);
		}
		input.addParm("flightNo",  flightNo);
		//航班后缀
		String flightSuffix = carrier.getFlightNumber().getOperationalSuffix();
		input.addParm("flightSuffix", flightSuffix == null ? "" : flightSuffix);
		FlightCOSCoreType service = array.getClassOfService();
		// 品牌免费选座标识
		String isseatfree = (String) service.getRefs().get(0);
		input.addParm("isseatfree", isseatfree == null ? "" : isseatfree);
		// 仓位
		String cabin = service.getCode().getStringValue();
		input.addParm("cabin", cabin == null ? "" : cabin);
		// 航班信息
		CommandData flight = new CommandData();
		flight.addParm("destcode", destCode);
		flight.addParm("cabin", cabin);
		flight.addParm("oriDateTime", oriDateTime);
		flight.addParm("oriDay", oriDay.replaceAll("-", ""));
		flight.addParm("oricode", oriCode);
		flight.addParm("brandid", brandid);
		flight.addParm("airlinecd", airlineCode);
		flight.addParm("flightno", flightNo + flightSuffix);
		input.addParm("flight", flight);
		if (StringUtils.isEmpty(flight)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT);
		}
	}

	private void queryPassengerSeat(CommandData input, DataListType dataList) throws APIException {
		// 航司二字码+航班号+航班号后缀
		String flight = "";
		FlightSegmentList segmentList = dataList.getFlightSegmentList();
		ListOfFlightSegmentType array = segmentList.getFlightSegmentArray(0);
		flight = array.getSegmentKey();
		input.addParm("flight", flight);
		if (!StringUtils.hasLength(flight)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT);
		}
		// 出发机场三字码
		String oriCod = "";
		Departure departure = array.getDeparture();
		oriCod = departure.getAirportCode().getStringValue();
		if (!StringUtils.hasLength(oriCod)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_ORG,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_ORG);
		}
		// 出发地日期
		Calendar date = departure.getDate();
		if ("".equals(date)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHTORG_STDATE,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHTORG_STDATE);
		}
		// 目的地机场三字码
		String destCode = "";
		destCode = array.getArrival().getAirportCode().getStringValue();
		if (!StringUtils.hasLength(destCode)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_DST,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_DST);
		}
		// 节点
		MarketingCarrierFlightType carrier = array.getMarketingCarrier();
		// AirlineID：航司二字码
		String airlineID = carrier.getAirlineID().getStringValue();
		if (!StringUtils.hasLength(airlineID)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_MARKET_FLIGHTID,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_MARKET_FLIGHTID);
		}
		// FlightNumber：航班号
		String flightNumber = "";
		flightNumber = carrier.getFlightNumber().getStringValue();
		if (!StringUtils.hasLength(flightNumber)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_NO,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_NO);
		}
		// OperationalSuffix：航班号后缀
		String suffix = "";
		suffix = carrier.getFlightNumber().getOperationalSuffix();
		// 选填项 指定座位号
		String seatNumber = "";
		seatNumber = carrier.getResBookDesigCode();
		input.addParm("seatNumber", seatNumber);

		// 选填项 指定舱位等级（大舱）
		FlightCOSCoreType service = array.getClassOfService();
		input.addParm("airlineCode", airlineID);
		input.addParm("flightNo", flightNumber);
		input.addParm("flightSuffix", suffix);
		input.addParm("oriCode", oriCod);
		input.addParm("destCode", destCode);
		input.addParm("deptDate", date.toString());
		// 选填项 可为空
		input.addParm("cabin", service.getCode() == null ? "" : service.getCode().getStringValue());

	}

	/**
	 * 转换 xml-->Reqbean
	 *
	 * @param xmlOutput
	 *            CommandRet
	 * @param input
	 *            CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(CommandRet xmlOutput, CommandData input) {
		SeatAvailabilityRSDocument sadoc = SeatAvailabilityRSDocument.Factory.newInstance();
		SeatAvailabilityRS rprs = sadoc.addNewSeatAvailabilityRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode, ApiServletHolder.getApiContext().getLanguage()));
			} else {
				rprs.addNewDocument();
				rprs.addNewSuccess();
				// 航班号+航空公司代码+后缀
				String flight = input.getParm("flight").getStringColumn();
				// flights节点下的信息
				Flights newFlights = rprs.addNewFlights();
				newFlights.addNewFlightSegmentReferences().setStringValue(input.getParm("flight").getStringColumn());
				Cabin newCabin = newFlights.addNewCabin();
				CommandData ABTable = xmlOutput.getParm("AB").getObjectColumn();
				Table layoutTable = xmlOutput.getParm("layout").getTableColumn();
				Table seatMapTable = xmlOutput.getParm("seatMap").getTableColumn();
				Table seatOffersTable = xmlOutput.getParm("seatOffers").getTableColumn();
				// 循环放入cabin信息
				if (layoutTable != null && layoutTable.getRowCount() > 0) {
					// 得到所有座位
					for (int i = 0; i < layoutTable.getRowCount(); i++) {
						// 循环舱位
						Row layoutRow = layoutTable.getRow(i);
						setNewCabin(newCabin, layoutRow, seatMapTable, input);
					}
				}
				// FlightSegment节点
				DataListType dataLists = rprs.addNewDataLists();
				ListOfFlightSegmentType flightSegment = dataLists.addNewFlightSegmentList().addNewFlightSegment();
				// 添加flightSegment节点
				flightSegment(flightSegment, ABTable, flight, input);

				// PriceClassList节点
				ServiceList newServiceList = dataLists.addNewServiceList();
				// 添加PriceClassList节点
				setPriceClassList(newServiceList, seatOffersTable, input);
				// seatList节点
				SeatList seatList = dataLists.addNewSeatList();
				// 添加seatList节点
				setseatList(seatList, seatMapTable, input);
			}
		} catch (Exception e) {
			sadoc = SeatAvailabilityRSDocument.Factory.newInstance();
			rprs = sadoc.addNewSeatAvailabilityRS();
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return sadoc;
	}

	// 添加seatList节点
	private void setseatList(SeatList seatList, Table seatMapTable, CommandData input) {
		// 此出为循环 每一个座位号都要放进来 取到座位号的table
		if (seatMapTable != null && seatMapTable.getRowCount() > 0) {
			// 放进所有座位号
			for (int i = 0; i < seatMapTable.getRowCount(); i++) {
				Row row = seatMapTable.getRow(i);
				ListOfSeatType seats = seatList.addNewSeats();
				String seatNumber = row.getColumn("seatNumber").getStringColumn();
				seats.setListKey(input.getParm("flight").getStringColumn() + seatNumber);
				SeatLocationType location = seats.addNewLocation();
				location.setColumn(row.getColumn("yAxis").getStringColumn());
				org.iata.iata.edist.SeatLocationType.Row newRow = location.addNewRow();
				newRow.addNewNumber().setStringValue(row.getColumn("xAxis").getStringColumn());
				Characteristics characteristics = location.addNewCharacteristics();
				// 当为座位时 才赋予属性值 过道无属性
				if (seatNumber.indexOf("_") == -1) {
					// 出口排isexit 机翼排iswing 过道aisle 窗户window
					/**
					 */
					// 过道
					String isAisle = row.getColumn("isAisle").getStringColumn();
					if ("1".equals(isAisle)) {
						Characteristic characteristic = characteristics.addNewCharacteristic();
						characteristic.setCode("A");
						characteristic.setDefinition("Aisle");
					}
					// 窗户
					String isWindow = row.getColumn("isWindow").getStringColumn();
					if ("1".equals(isWindow)) {
						Characteristic characteristica = characteristics.addNewCharacteristic();
						characteristica.setCode("W");
						characteristica.setDefinition("Window");
					}
					// 出口排
					String isExit = row.getColumn("isExit").getStringColumn();
					if ("1".equals(isExit)) {
						Characteristic characteristicb = characteristics.addNewCharacteristic();
						characteristicb.setCode("E");
						characteristicb.setDefinition("Exit");
					}
					// 机翼排
					String isWing = row.getColumn("isWing").getStringColumn();
					if ("1".equals(isWing)) {
						Characteristic characteristicc = characteristics.addNewCharacteristic();
						characteristicc.setCode("OW");
						characteristicc.setDefinition("Wing");
					}
					String isPay = row.getColumn("isPay").getStringColumn();
					if ("1".equals(isPay)) {
						Characteristic characteristicd = characteristics.addNewCharacteristic();
						characteristicd.setCode("$");
						characteristicd.setDefinition("Pay");
					}
					String isRlock = row.getColumn("isRlock").getStringColumn();
					String isXlock = row.getColumn("isXlock").getStringColumn();
					String isDcsolock = row.getColumn("isDcsolock").getStringColumn();
					String isUsed = row.getColumn("isUsed").getStringColumn();
					if ("1".equals(isRlock)) {
						Characteristic characteristice = characteristics.addNewCharacteristic();
						characteristice.setCode("R");
						characteristice.setDefinition("Rlock");
					} else if ("1".equals(isXlock)) {
						Characteristic characteristicf = characteristics.addNewCharacteristic();
						characteristicf.setCode("X");
						characteristicf.setDefinition("Xlock");
					} else if ("1".equals(isDcsolock)) {
						Characteristic characteristicg = characteristics.addNewCharacteristic();
						characteristicg.setCode("D");
						characteristicg.setDefinition("Dcsolock");
					} else if ("1".equals(isUsed)) {
						Characteristic characteristich = characteristics.addNewCharacteristic();
						characteristich.setCode("U");
						characteristich.setDefinition("Used");
					} else {
						Characteristic characteristice = characteristics.addNewCharacteristic();
						characteristice.setCode("*");
						characteristice.setDefinition("Available");
					}
					OrderItemAssociationType associations = location.addNewAssociations();
					Services newServices = associations.addNewServices();
					ServiceIDType serviceID = newServices.addNewServiceID();
					serviceID.setOwner(input.getParm("airlineCode").getStringColumn());
					serviceID.setStringValue(row.getColumn("groupCode").getStringColumn() == "" ? ""
							: row.getColumn("groupCode").getStringColumn());
				}
			}
		}
	}

	// 添加PriceClassList节点
	private void setPriceClassList(ServiceList newServiceList, Table seatOffersTable, CommandData input) {
		if (seatOffersTable != null && seatOffersTable.getRowCount() > 0) {
			for (int i = 0; i < seatOffersTable.getRowCount(); i++) {
				Row row = seatOffersTable.getRow(i);
				ServiceDetailType newService = newServiceList.addNewService();
				ServiceIDType newServiceID = newService.addNewServiceID();
				newServiceID.setStringValue(row.getColumn("code").getStringColumn());
				newServiceID.setOwner(input.getParm("airlineCode").getStringColumn());
				String language = ApiServletHolder.getApiContext().getLanguage();
				CommandData data = row.getColumn("name").getObjectColumn();
				String name = "";
				if (!"".equals(data)) {
					name = data.getParm(language).getStringColumn();
				}
				newService.addNewName().setStringValue(name);
				org.iata.iata.edist.ServiceDescriptionType.Description description = newService.addNewDescriptions()
						.addNewDescription();
				description.addNewText().setStringValue(row.getColumn("remark").getStringColumn());
				ServicePriceType newPrice = newService.addNewPrice();
				CurrencyAmountOptType newTotal = newPrice.addNewTotal();
				newTotal.setStringValue(row.getColumn("price").getStringColumn());
				newTotal.setCode(row.getColumn("currencyCode").getStringColumn());
				/*
				 * basisCode.setObjectKey(row.getColumn("typecode").
				 * getStringColumn()); basisCode.setCode();
				 * basisCode.setApplication();
				 * priceClass.setDisplayOrder(row.getColumn("display").
				 * getStringColumn());
				 */
			}
		}
	}

	// 添加cabin节点
	private void setNewCabin(Cabin newCabin, Row layoutRow, Table seatTable, CommandData input) {
		// 舱位等级
		newCabin.setCode(layoutRow.getColumn("cabin").getStringColumn());
		SeatDisplay display = newCabin.addNewSeatDisplay();
		// 布局
		String layout = layoutRow.getColumn("layout").getStringColumn();
		// 放入布局信息
		display.setSeatDisplayKey(layout);
		// 分割为abc bc 这种情况
		if (!"".equals(layout)) {
			String[] stringLayout = layout.split("_");
			for (int i = 0; i < stringLayout.length; i++) {
				// 为分割好的字符串 abc ef dj
				String split = stringLayout[i];
				char[] alone = split.toCharArray();
				for (int j = 0; j < alone.length; j++) {
					// 当为第一个字符串时 为靠窗
					if (i == 0 && j == 0) {
						// 此时为第一个靠窗坐
						Columns addNewColumns = display.addNewColumns();
						addNewColumns.setStringValue(String.valueOf(alone[j]));
						addNewColumns.setPosition("W");
					} else if (i == stringLayout.length - 1 && j == alone.length - 1) {
						// 最后一个也为靠窗座位
						Columns addNewColumns = display.addNewColumns();
						addNewColumns.setStringValue(String.valueOf(alone[j]));
						addNewColumns.setPosition("W");
					} else {
						// 当不是分割后的最后一个时，第一个和最后一个都为过道
						if (i < stringLayout.length - 1) {
							if (alone.length - 1 == j || j == 0) {
								// 过道 最后一个
								Columns addNewColumns = display.addNewColumns();
								addNewColumns.setStringValue(String.valueOf(alone[j]));
								addNewColumns.setPosition("A");
							} else {
								// 中间的
								Columns addNewColumns = display.addNewColumns();
								addNewColumns.setStringValue(String.valueOf(alone[j]));
								addNewColumns.setPosition("C");
							}
						} else {// 最后一个
							if (j == 0) {
								// 过道 最后一个
								Columns addNewColumns = display.addNewColumns();
								addNewColumns.setStringValue(String.valueOf(alone[j]));
								addNewColumns.setPosition("A");
							} else {
								// 放中间
								Columns addNewColumns = display.addNewColumns();
								addNewColumns.setStringValue(String.valueOf(alone[j]));
								addNewColumns.setPosition("C");
							}
						}
					}
				}
			}
		}
		/*
		 * columns.setPosition(layoutRow.getColumn("layout").getStringColumn());
		 * columns.setStringValue(layoutRow.getColumn("seatCount").
		 * getStringColumn());
		 */
		Rows newRows = display.addNewRows();
		// 起始排
		newRows.setFirst(BigInteger.valueOf(layoutRow.getColumn("startRow").getLongColumn()));
		// 结束排
		newRows.setLast(BigInteger.valueOf(layoutRow.getColumn("endRow").getLongColumn()));
		CabinType cabinType = display.addNewCabinType();
		String cabin = layoutRow.getColumn("cabin").getStringColumn();
		cabinType.setCode(cabin);
		// 获取语言 兼容多语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		String data = layoutRow.getColumn("cabinName").getStringColumn();
		CommandData data2 = new CommandData();
		CommandData fromJson = JsonUnit.fromJson(data2, data);
		String name = "";
		// name为一个中文
		if (fromJson != null) {
			name = fromJson.getParm(language).getStringColumn();
		}
		cabinType.setName(name);
		// 得到舱位等级 遍历对应的座位
		for (int i = 0; i < seatTable.getRowCount(); i++) {
			Row row = seatTable.getRow(i);
			String sCabin = row.getColumn("cabin").getStringColumn();
			if (cabin.equals(sCabin)) {
				// <!--座位命名规范：航司二字码+航班号+后缀+row+column-->
				newCabin.addNewSeatReference().setStringValue(
						input.getParm("flight").getStringColumn() + row.getColumn("seatNumber").getStringColumn());
			}
		}
	}

	/**
	 *
	 * 航班信息
	 *
	 * @param flightSegment
	 *            ListOfFlightSegmentType
	 * @param aBTable
	 *            Row void 返回类型
	 * @param flight
	 * @param input
	 *
	 */
	private void flightSegment(ListOfFlightSegmentType flightSegment, CommandData aBTable, String flight,
			CommandData input) {
		// 航班编号
		if (StringUtils.hasLength(flight)) {
			flightSegment.setSegmentKey(flight);
		}
		Departure departure = flightSegment.addNewDeparture();
		departure.addNewAirportCode().setStringValue(aBTable.getParm("oriCode").getStringColumn());
		Calendar datetoCalendare = null;
		try {
			datetoCalendare = DateUtils.getInstance().parseDate(input.getParm("deptDate").getStringColumn(),
					"yyyy-MM-dd");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		departure.setDate(datetoCalendare);
		AirportCode airportCode = flightSegment.addNewArrival().addNewAirportCode();
		airportCode.setStringValue(aBTable.getParm("destCode").getStringColumn());
		MarketingCarrierFlightType marketingCarrier = flightSegment.addNewMarketingCarrier();
		marketingCarrier.addNewAirlineID().setStringValue(aBTable.getParm("airlineCode").getStringColumn());
		FlightNumber addNewFlightNumber = marketingCarrier.addNewFlightNumber();
		addNewFlightNumber.setStringValue(aBTable.getParm("flightNo").getStringColumn());
		addNewFlightNumber.setOperationalSuffix(aBTable.getParm("flightSuffix").getStringColumn());
		AircraftCode code = flightSegment.addNewEquipment().addNewAircraftCode();
		code.setStringValue(aBTable.getParm("model").getStringColumn());
	}
}
