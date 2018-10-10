package com.travelsky.quick.business;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AcceptedPaymentFormType;
import org.iata.iata.edist.AirDocDisplayRSDocument;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.FareInfo;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.FareInfo.Taxes;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.OriginDestination;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.Payments;
import org.iata.iata.edist.AirDocIssueRQDocument;
import org.iata.iata.edist.BaseFareTransactionType.Amount;
import org.iata.iata.edist.BookingReferencesDocument.BookingReferences;
import org.iata.iata.edist.CouponInfoType.SoldAirlineInfo;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.DataListType;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.BookingReferenceType;
import org.iata.iata.edist.CouponInfoType;
import org.iata.iata.edist.CouponTravelerDetailType;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.IssuingAirlineInfoDocument.IssuingAirlineInfo;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.OrderPaymentFormType;
import org.iata.iata.edist.SimpleCurrencyPriceType;
import org.iata.iata.edist.TaxDetailType.Breakdown.Tax;
import org.iata.iata.edist.TicketDocumentDocument1.TicketDocument;
import org.iata.iata.edist.TravelerFOIDType;
import org.iata.iata.edist.TravelerFOIDType.FOID;
import org.iata.iata.edist.TravelerInfoDocument.TravelerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/** 
 * @author 作者:ZHANGJIABIN
 * @version 0.1
 * 类说明:
 *		ABB渠道支付并出票接口
 */
@Service("LCC_PAYMENTABB_SERVICE")
public class APIPayABBNDCBusiness extends AbstractService<ApiContext>{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIPayABBNDCBusiness.class);
	private static final int beginIndex = 3;
	private static final int endIndex = 4;
	
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取xml
		try{
			//转换 xml-->Reqbean
			transInputXmlToRequestBean();
			//获取ResponseBean
			context.setRet(getResponseBean());
		}
		//请求  xm转换CommandData 异常
		catch (APIException e) { 
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_PAY_PRE, 
					ApiServletHolder.getApiContext().getLanguage()), e);
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
		AirDocIssueRQDocument rootDoc = AirDocIssueRQDocument.Factory.parse(xmlInput);

		AirDocIssueRQDocument.AirDocIssueRQ reqdoc = rootDoc.getAirDocIssueRQ();
		//订单号
		String orderno = "";
		//货币代码
		String currencyCode = "";
		//订单总金额
		BigDecimal totalprice = BigDecimal.valueOf(0);
		org.iata.iata.edist.AirDocIssueRQDocument.AirDocIssueRQ.Query.TicketDocInfo[] tkts = null;
		if(reqdoc.getQuery() != null 
				&& reqdoc.getQuery().getTicketDocInfoArray() != null 
				&& reqdoc.getQuery().getTicketDocInfoArray().length > 0){
			tkts = reqdoc.getQuery().getTicketDocInfoArray();
			if(tkts[0].getOrderReference() != null 
					&& tkts[0].getOrderReference().getOrderID() != null){
				orderno = tkts[0].getOrderReference().getOrderID().getStringValue();
				currencyCode = tkts[0].getPayments().getPaymentArray(0).getAmount().getCode();
			}
		}
		input.addParm("orderno", orderno);
		input.addParm("memberid", context.getContext().getUserID());
		//用于存放每个旅客要支付的价格
		List<BigDecimal> list =new ArrayList<>();
		//获取paxs信息并添加到CommandData中
		Table paxTable = new Table(new String[]{"paxtype","firstname","doctype","docid","lastname","tickets"});
		for(int k = 0; k < tkts.length; k++){
			org.iata.iata.edist.AirDocIssueRQDocument.AirDocIssueRQ.Query.TicketDocInfo ticketDocInfo = tkts[k];
			addPaxInfo(paxTable,ticketDocInfo,totalprice,list);
		}
		input.addParm("currencyCode", currencyCode);
		input.addParm("paxs", paxTable);
		for (int i = 0; i < list.size(); i++) {
			totalprice = totalprice.add(list.get(i));
		}
		input.addParm("totalprice", totalprice);
		
		//获取flights信息添加到CommandData中  
		Table flightTable = new Table(new String[]{"flightid","airlineCode","flightNo","flightSuffix","flightDay","oriCode","destCode","cost"});
		DataListType dataLists = reqdoc.getQuery().getDataLists();
		addFlightInfo(flightTable,dataLists);
		//航班信息
		input.addParm("flights", flightTable);
	}
	
	/**
	 * 添加flights节点
	 * @param flightTable
	 * @param dataLists
	 */
	private void addFlightInfo(Table flightTable, DataListType dataLists) {
		FlightSegmentList flightSegmentList = dataLists.getFlightSegmentList();
		ListOfFlightSegmentType[] segmentArray = flightSegmentList.getFlightSegmentArray();
		for (ListOfFlightSegmentType flightSegment : segmentArray) {
			Row flightRow = flightTable.addRow();
			//航段key
			String segmentKey = flightSegment.getSegmentKey();
			//出发地三字码
			String oriCode = flightSegment.getDeparture().getAirportCode().getStringValue();
			//目的地三字码
			String destCode = flightSegment.getArrival().getAirportCode().getStringValue();
			//航班天
			Calendar date = flightSegment.getDeparture().getDate();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			String flightDay = sdf.format(date.getTime());
			MarketingCarrierFlightType marketingCarrier = flightSegment.getMarketingCarrier();
			//航空公司二字码
			String airlineCode = marketingCarrier.getAirlineID().getStringValue();
			//航班号
			String flightNo = marketingCarrier.getFlightNumber().getStringValue();
			//航班号后缀
			String flightSuffix = marketingCarrier.getFlightNumber().getOperationalSuffix();
			flightRow.addColumn("flightid", segmentKey.substring(beginIndex, endIndex));
			flightRow.addColumn("airlineCode", airlineCode);
			flightRow.addColumn("flightNo", flightNo);
			flightRow.addColumn("flightSuffix", flightSuffix);
			flightRow.addColumn("flightDay", flightDay);
			flightRow.addColumn("oriCode", oriCode);
			flightRow.addColumn("destCode", destCode);
			flightRow.addColumn("cost", "");
		}
	}

	/**
	 * 添加paxs节点
	 * @param paxTable
	 * @param ticketDocInfo
	 * @param totalprice
	 */
	private void addPaxInfo(Table paxTable,org.iata.iata.edist.AirDocIssueRQDocument.AirDocIssueRQ.Query.TicketDocInfo ticketDocInfo,
			BigDecimal totalprice,List<BigDecimal> list) {
		TravelerInfo travelerInfo = ticketDocInfo.getTravelerInfo();
		Row row = paxTable.addRow();
		// 旅客类型
		row.addColumn("paxtype", travelerInfo.getPTC());
		//名
		row.addColumn("firstname", travelerInfo.getGiven().getStringValue());
		//姓
		row.addColumn("lastname", travelerInfo.getSurname().getStringValue());
		FOID[] array = travelerInfo.getFOIDs().getFOIDArray();
		//证件类型
		String doctype = array[0].getType().getCode();
		row.addColumn("doctype", doctype);
		//证件号
		String docid = array[0].getID();
		row.addColumn("docid", docid);
		BigDecimal decimalValue = ticketDocInfo.getPayments().getPaymentArray(0).getAmount().getBigDecimalValue();
		list.add(decimalValue);
		row.addColumn("tickets", "");
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
		return orderOpManager.payABBAndTKT(input, context);
	}
	
	/**
	 * 拼接B2C所用的XML
	 * @param commandRet 后台返回的结果集
	 * @param input  B2C请求的XML
	 * @return  请求后台返回的对象 
	 */
	public XmlObject transRespBeanToXmlBean(Object ret ,CommandData input) {
		CommandRet commandRet = (CommandRet)ret;
		AirDocDisplayRSDocument doc = AirDocDisplayRSDocument.Factory.newInstance();
		AirDocDisplayRSDocument.AirDocDisplayRS rprs = doc.addNewAirDocDisplayRS();
		try{
			String errorcode = commandRet.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));			
			}else{   
				//反回正确的值
				// Response
				rprs.addNewSuccess();
				Response response = rprs.addNewResponse();
				TicketDocInfos ticketDocInfos = response.addNewTicketDocInfos();
				Table paxsTable = commandRet.getParm("paxs").getTableColumn();
				if(paxsTable != null){
					for (int i = 0; i < paxsTable.getRowCount(); i++) {
						Row paxRow = paxsTable.getRow(i);
						TicketDocInfo newTicketDocInfo = ticketDocInfos.addNewTicketDocInfo();
						
						/*******************1、 旅客信息 ******************/
						CouponTravelerDetailType addNewTraveler = newTicketDocInfo.addNewTraveler();
						//姓
						addNewTraveler.addNewSurname().setStringValue(paxRow.getColumn("firtname").getStringColumn());
						//名
						addNewTraveler.addNewGiven().setStringValue(paxRow.getColumn("lastname").getStringColumn());
						//类型
						addNewTraveler.setPTC(paxRow.getColumn("paxtype").getStringColumn());
						//出生日期 
						Calendar ori=Calendar.getInstance();
						String birth = paxRow.getColumn("birth").getStringColumn();
						ori.setTime(Unit.getDate(birth));
						addNewTraveler.setBirthDate(ori);
						
						/*******************2、IssuingAirlineInfo信息 ******************/
						IssuingAirlineInfo newIssuingAirlineInfo = newTicketDocInfo.addNewIssuingAirlineInfo();
						String airline = commandRet.getParm("crsc").getStringColumn();
						//航空公司二字码
						newIssuingAirlineInfo.setAirlineName(airline);
						String cityPlace = commandRet.getParm("crsl").getStringColumn();
						//所在地
						newIssuingAirlineInfo.setPlace(cityPlace);
						
						/*******************4、Payments和信息 ******************/
						Payments newPayments = newTicketDocInfo.addNewPayments();
						OrderPaymentFormType newPayment = newPayments.addNewPayment();
						SimpleCurrencyPriceType payamount = newPayment.addNewAmount();
						//支付金额 
						String totalAmount = paxRow.getColumn("totalprice").getStringColumn();
						payamount.setBigDecimalValue(new BigDecimal(totalAmount));
						//币种
						payamount.setCode(paxRow.getColumn("currencyCode").getStringColumn());
						
						/*******************5、OriginDestination和信息 ******************/
						OriginDestination newOriginDestination = newTicketDocInfo.addNewOriginDestination();
						String oricode = paxRow.getColumn("oricitycode").getStringColumn();
						//起始地三字码
						newOriginDestination.setOrigin(oricode);
						String destcode = paxRow.getColumn("destcitycode").getStringColumn();
						//目的地三字码
						newOriginDestination.setDestination(destcode);
						
						/*******************6、FareInfo和信息 ******************/
						FareInfo newFareInfo = newTicketDocInfo.addNewFareInfo();
						//总票价
						String baseFare = paxRow.getColumn("farePrice").getStringColumn();
						Amount baseAmount = newFareInfo.addNewBaseFare().addNewAmount();
						baseAmount.setBigDecimalValue(new BigDecimal(baseFare));
						baseAmount.setCode(paxRow.getColumn("currencyCode").getStringColumn());
						//总价（票+税）
						org.iata.iata.edist.TotalFareTransactionType.Amount totalAmountpax = newFareInfo.addNewTotal().addNewAmount();
						totalAmountpax.setBigDecimalValue(new BigDecimal(totalAmount));
						totalAmountpax.setCode(paxRow.getColumn("currencyCode").getStringColumn());
						//总税费 (多种)
						Taxes newTaxes = newFareInfo.addNewTaxes();
						Table taxTable = paxRow.getColumn("taxs").getTableColumn();
						if(taxTable != null){
							for (int j = 0; j < taxTable.getRowCount(); j++) {
								Row taxRow = taxTable.getRow(j);
								Tax newTax = newTaxes.addNewBreakdown().addNewTax();
								//税费金额
								CurrencyAmountOptType taxamount = newTax.addNewAmount();
								taxamount.setBigDecimalValue(taxRow.getColumn("Amount").getBigDecimalColumn());
								taxamount.setCode(paxRow.getColumn("currencyCode").getStringColumn());
								//税费code
								newTax.setTaxCode(taxRow.getColumn("TaxCode").getStringColumn());
							}
						}
						
						/*******************7、TicketDocument和信息 ******************/
						TicketDocument newTicketDocument = newTicketDocInfo.addNewTicketDocument();
						Table tktsTable = paxRow.getColumn("tkts").getTableColumn();
						if(tktsTable != null){
							for (int t = 0; t < tktsTable.getRowCount(); t++) {
								Row tktRow = tktsTable.getRow(t);
								//票号
								newTicketDocument.setTicketDocNbr(tktRow.getColumn("tktno").getStringColumn());
								String outTktDate = paxRow.getColumn("outtktdate").getStringColumn();
								if(outTktDate.length()==13){
									Calendar outTktCal = Calendar.getInstance();
									outTktCal.setTime(Unit.getDate(outTktDate.substring(0, 8)));
									//出票日期
									newTicketDocument.setDateOfIssue(outTktCal);
//									//出票时间
									newTicketDocument.setTimeOfIssue(outTktDate.substring(9, 11)+":"+outTktDate.substring(11, 13));
								}
								
								/*******************8、TicketDocument和信息 ******************/
								Table couponTable = tktRow.getColumn("segs").getTableColumn();
								if(couponTable != null){
									for (int c = 0; c < couponTable.getRowCount(); c++) {
										Row couponRow = couponTable.getRow(c);
										//航段信息(单程、往返)
										CouponInfoType newCouponInfo = newTicketDocument.addNewCouponInfo();
										//航段号
										newCouponInfo.setCouponNumber(BigInteger.valueOf(c+1));
										//票价基础
										newCouponInfo.addNewFareBasisCode().setCode(couponRow.getColumn("farebasiscode").getStringColumn());
										//航段状态
										newCouponInfo.addNewStatus().setCode(couponRow.getColumn("tktstatus").getStringColumn());
										//航班信息
										SoldAirlineInfo newSoldAirlineInfo = newCouponInfo.addNewSoldAirlineInfo();
										
										Departure newDeparture = newSoldAirlineInfo.addNewDeparture();
										//出发地三字码
										newDeparture.addNewAirportCode().setStringValue(couponRow.getColumn("oricode").getStringColumn());
										Calendar departDate = Calendar.getInstance();
										departDate.setTime(couponRow.getColumn("oridate").getDateColumn());
										//出发日期
										newDeparture.setDate(departDate);
										//出发时间
										newDeparture.setTime(couponRow.getColumn("oritime").getStringColumn());
										
										FlightArrivalType newArrival = newSoldAirlineInfo.addNewArrival();
										//到达地三字码
										newArrival.addNewAirportCode().setStringValue(couponRow.getColumn("destcode").getStringColumn());
										Calendar arrivalDate = Calendar.getInstance();
										arrivalDate.setTime(couponRow.getColumn("destdate").getDateColumn());
										//到达日期
										newArrival.setDate(arrivalDate);
										//到达地时间
										newArrival.setTime(couponRow.getColumn("desttime").getStringColumn());
										
										MarketingCarrierFlightType newMarketingCarrier = newSoldAirlineInfo.addNewMarketingCarrier();
										//航空公司二字码
										newMarketingCarrier.addNewAirlineID().setStringValue(couponRow.getColumn("airlineCode").getStringColumn());
										//航班号
										FlightNumber fltno = newMarketingCarrier.addNewFlightNumber();
										fltno.setStringValue(couponRow.getColumn("flightno").getStringColumn());
										//航班号后缀
										fltno.setOperationalSuffix(couponRow.getColumn("flightnosuffix").getStringColumn());
										//机型  
										String planestype = couponRow.getColumn("planestype").getStringColumn();
										newSoldAirlineInfo.addNewEquipment().addNewAircraftCode().setStringValue(planestype);
									}
								}
							}
						}
						
						/*******************3、BookingReference和信息 ******************/
						BookingReferences newBookingReferences = newTicketDocInfo.addNewBookingReferences();
						BookingReferenceType newBookingReference = newBookingReferences.addNewBookingReference();
						String airlineID = paxRow.getColumn("tkts").getTableColumn().getRow(0).getColumn("segs").getTableColumn().getRow(0).getColumn("airlineCode").getStringColumn();
						String airlinePNR = paxRow.getColumn("tkts").getTableColumn().getRow(0).getColumn("segs").getTableColumn().getRow(0).getColumn("pnr").getStringColumn();
						//PNR编号
						newBookingReference.setID(airlinePNR);
						//航空公司二字码
						newBookingReference.addNewAirlineID().setStringValue(airlineID);
						//订单编号
						newTicketDocInfo.addNewOrderReference().addNewOrderID()
						.setStringValue(commandRet.getParm("orderid").getStringColumn());
						
					}
				}
				
			}
		} 
		catch (Exception e) {
			//初始化XML节点
			doc = AirDocDisplayRSDocument.Factory.newInstance();
			rprs = doc.addNewAirDocDisplayRS();
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}

	

}
