package com.travelsky.quick.business;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AcceptedPaymentFormType;
import org.iata.iata.edist.AirDocDisplayRSDocument;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.FareInfo;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.OriginDestination;
import org.iata.iata.edist.AirDocIssueRQDocument;
import org.iata.iata.edist.AirDocIssueRQDocument.AirDocIssueRQ.Query;
import org.iata.iata.edist.AirDocIssueRQDocument.AirDocIssueRQ.Query.TicketDocInfo;
import org.iata.iata.edist.BaseFareTransactionType.Amount;
import org.iata.iata.edist.BookingReferenceType;
import org.iata.iata.edist.BookingReferencesDocument.BookingReferences;
import org.iata.iata.edist.CouponInfoType;
import org.iata.iata.edist.CouponInfoType.SoldAirlineInfo;
import org.iata.iata.edist.CouponTravelerDetailType;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.DataListType;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.FareComponentType;
import org.iata.iata.edist.FareListDocument.FareList;
import org.iata.iata.edist.FareListDocument.FareList.FareGroup;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.IssuingAirlineInfoDocument.IssuingAirlineInfo;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.OrderPaymentFormType;
import org.iata.iata.edist.OtherDocument.Other;
import org.iata.iata.edist.SegmentReferenceDocument.SegmentReference;
import org.iata.iata.edist.SimpleCurrencyPriceType;
import org.iata.iata.edist.TaxDetailType;
import org.iata.iata.edist.TaxDetailType.Breakdown;
import org.iata.iata.edist.TaxDetailType.Breakdown.Tax;
import org.iata.iata.edist.TicketDocumentDocument1.TicketDocument;
import org.iata.iata.edist.TicketDocumentType;
import org.iata.iata.edist.TicketDocumentType.CpnNbrs;
import org.iata.iata.edist.TravelerInfoDocument.TravelerInfo;
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
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.APICacheHelper;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 出票接口
 * @author wangyicheng
 */
@Service("LCC_OUTTKT_SERVICE")
public class APIOutTicketBusiness extends AbstractService<ApiContext> {
	/**
	 *
	 */
	private static final long serialVersionUID = 8691825821149118846L;

	private static final Logger LOGGER = LoggerFactory.getLogger(APIOutTicketBusiness.class);
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			//转换 xml-->Reqbean
			transInputXmlToRequestBean();
			//获取ResponseBean
			context.setRet(getResult());
		}
		catch (APIException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_USER_LOGIN,
					ApiServletHolder.getApiContext().getLanguage()),e);
			throw e;
		}
	}
	
	private CommandRet getResult() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.outTicket(input, context);
	}

	/**
	 * xml转command
	 * @throws APIException
	 * @throws Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		ApiContext apiCtx = ApiServletHolder.getApiContext();
		AirDocIssueRQDocument doc = AirDocIssueRQDocument.Factory.parse(apiCtx.getReqXML());
		// Root Element
		AirDocIssueRQDocument.AirDocIssueRQ issueRQ = doc.getAirDocIssueRQ();
		APICacheHelper.setDeptInfo(ApiServletHolder.get(), issueRQ.getParty());


		// 取pnr与currencyCode. 因为所有人的pnr与币种都是一样的，固这里只取第一个人的值即可
		Query query = issueRQ.getQuery();
		TicketDocInfo[] tktDocInfos = query.getTicketDocInfoArray();
		int tktDocInfoSize = tktDocInfos==null? 0 : tktDocInfos.length;
		// 获取语言
		String language=apiCtx.getLanguage();
		// 出票人数为0
		if(tktDocInfoSize==0) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_TICKET_PAX, language));
			throw new APIException(ErrCodeConstants.API_NULL_TICKET_PAX);
		}
		
		// 第一个旅客
		TicketDocInfo pax1 = tktDocInfos[0];
		String pnr = pax1.getBookingReference().getID();
		// CommandInput
		CommandData input = ApiServletHolder.get().getInput();
		input.addParm("language", language);
		// 部门ID  
		input.addParm("tktdeptid",apiCtx.getTicketDeptid());
		// 会员账号
		input.addParm("memberid", apiCtx.getUserID());
		// pnr
		input.addParm("pnr", pnr);
		// 币种
		String currency=pax1.getPayments().getPaymentArray(0).getAmount().getCode();
		input.addParm("currencyCode", currency);

		//获取paxs信息并添加到CommandData中
		Table paxTable = new Table(new String[]{"paxtype","firstname","lastname","payprice","paytype","currencyCode","FN","tickets"});
		//订单总价
		BigDecimal totalprice = BigDecimal.valueOf(0);
		//存放国内国际标识
		Map<Integer, String> cityTypeMap = new HashMap<>();
		Table tktCountryTypeTable = new Table(new String[]{"tktNo","countryType"});
		//用于存放每个旅客要支付的价格
		List<BigDecimal> list =new ArrayList<>();
		for (int i=0;i<tktDocInfoSize;i++) {
			TicketDocInfo pax = tktDocInfos[i];
			Table ticketTable = new Table(new String[]{"tickerno","flightid"});
			addPaxInfo(paxTable, pax,currency,ticketTable,totalprice,list,cityTypeMap,tktCountryTypeTable);
		}
		input.addParm("paxs", paxTable);
		input.addParm("tktCountryTypeTable", tktCountryTypeTable);
		if(list != null && list.size() > 0){
			for(int w = 0;w<list.size();w++){
				totalprice = totalprice.add(list.get(w));
			}
		}
		//最高价 
		input.addParm("totalprice", totalprice);
		//获取flights信息添加到CommandData中  
		Table flightTable = new Table(new String[]{"flightid","airlineCode","flightNo","flightSuffix","flightDay","oriCode","destCode","fareBasic","countryType","ticketPrice"});
		Table taxsTable = new Table(new String[]{"code","taxDetail"}); 
		//ADT fare map(key值为税费code，value值为税费价格)
		Map<String,BigDecimal> adtMap = new HashMap<>();
		//CHD fare map
		Map<String,BigDecimal> chdMap = new HashMap<>();
		//INF fare map
		Map<String,BigDecimal> infMap = new HashMap<>();
		DataListType dataLists = query.getDataLists();
		addFlightInfo(flightTable,dataLists,currency,taxsTable,adtMap,chdMap,infMap,cityTypeMap);
		//航班信息
		input.addParm("flights", flightTable);
		//税费信息
		if(adtMap != null && adtMap.size() > 0){
			Row taxRow = taxsTable.addRow();
			taxRow.addColumn("code", "adtfare");
			Table taxDetailTable = new Table(new String[]{"taxcode","taxPrice"});
			for (Entry<String, BigDecimal> entry : adtMap.entrySet()) {
				Row taxDetailRow = taxDetailTable.addRow();
				taxDetailRow.addColumn("taxcode", entry.getKey());
				taxDetailRow.addColumn("taxPrice", entry.getValue());
			}
			taxRow.addColumn("taxDetail", taxDetailTable);
		}
		if(chdMap != null && chdMap.size() > 0){
			Row taxRow = taxsTable.addRow();
			taxRow.addColumn("code", "chdfare");
			Table taxDetailTable = new Table(new String[]{"taxcode","taxPrice"});
			for (Entry<String, BigDecimal> entry : chdMap.entrySet()) {
				Row taxDetailRow = taxDetailTable.addRow();
				taxDetailRow.addColumn("taxcode", entry.getKey());
				taxDetailRow.addColumn("taxPrice", entry.getValue());
			}
			taxRow.addColumn("taxDetail", taxDetailTable);
		}
		if(infMap != null && infMap.size() > 0){
			Row taxRow = taxsTable.addRow();
			taxRow.addColumn("code", "inffare");
			Table taxDetailTable = new Table(new String[]{"taxcode","taxPrice"});
			for (Entry<String, BigDecimal> entry : infMap.entrySet()) {
				Row taxDetailRow = taxDetailTable.addRow();
				taxDetailRow.addColumn("taxcode", entry.getKey());
				taxDetailRow.addColumn("taxPrice", entry.getValue());
			}
			taxRow.addColumn("taxDetail", taxDetailTable);
		}
		input.addParm("taxs", taxsTable);
	}

	/**
	 * 添加flights节点
	 * @param flightTable
	 * @param dataLists
	 */
	private void addFlightInfo(Table flightTable, DataListType dataLists,String currency,Table taxsTable,Map<String, BigDecimal> adtMap
			,Map<String, BigDecimal> chdMap,Map<String, BigDecimal> infMap,Map<Integer, String> cityTypeMap) {
		FlightSegmentList flightSegmentList = dataLists.getFlightSegmentList();
		ListOfFlightSegmentType[] segmentArray = flightSegmentList.getFlightSegmentArray();
		int i = 1;
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
			flightRow.addColumn("flightid", i);
			flightRow.addColumn("airlineCode", airlineCode);
			flightRow.addColumn("flightNo", DateUtils.getFlightNo(flightNo));
			flightRow.addColumn("flightSuffix", flightSuffix);
			flightRow.addColumn("flightDay", flightDay);
			flightRow.addColumn("countryType", cityTypeMap.get(i));
			flightRow.addColumn("oriCode", oriCode);
			flightRow.addColumn("destCode", destCode);
			Table ticketPriceTable = new Table(new String[]{"code","price"});
			addCostInfo(flightRow,dataLists,segmentKey,currency,ticketPriceTable,taxsTable,adtMap,chdMap,infMap);
			i++;
		}
	}
	
	/**
	 * 添加cost节点
	 * @param flightRow
	 * @param dataLists
	 * @param segmentKey
	 * @param currency
	 */
	private void addCostInfo(Row flightRow, DataListType dataLists,
			String segmentKey, String currency,Table ticketPriceTable,Table taxsTable,Map<String, BigDecimal> adtMap
			,Map<String, BigDecimal> chdMap,Map<String, BigDecimal> infMap) {
		//存放当前航班下每种旅客类型的票面价
		Map<String, BigDecimal> ticketPriceMap = new HashMap<>();
		FareList fareList = dataLists.getFareList();
		String fareBasic = "";
		if(fareList != null){
			FareGroup[] fareGroupArray = fareList.getFareGroupArray();
			//取fareBasicCode
			if(fareGroupArray != null && fareGroupArray.length > 0){
				for (FareGroup fareGroup : fareGroupArray) {
					FareComponentType fareComponentArray = fareGroup.getFare().getFareDetail().getFareComponentArray(0);
					String segId = fareComponentArray.getSegmentReference().getStringValue();
					if(segmentKey.equals(segId)){
						fareBasic = fareComponentArray.getFareBasis().getFareBasisCode().getCode();
						break;
					}
				}
				for (FareGroup fareGroup : fareGroupArray) {
					String fareComponentRefs = fareGroup.getRefs().toString();
					FareComponentType[] fareComponentArray = fareGroup.getFare().getFareDetail().getFareComponentArray();
					for (FareComponentType fareComponentType : fareComponentArray) {
						TaxDetailType detailType = fareComponentType.getPriceBreakdown().getPrice().getTaxes();
						if(detailType != null){
							Tax[] taxs = detailType.getBreakdown().getTaxArray();
							if(taxs != null && taxs.length > 0){
								if(StringUtils.hasLength(fareComponentRefs) && "ADT".equals(fareComponentRefs)){
									for (Tax tax : taxs) {
										adtMap.put(tax.getTaxCode(), tax.getAmount().getBigDecimalValue());
									}
								}else if(StringUtils.hasLength(fareComponentRefs) && "CHD".equals(fareComponentRefs)){
									for (Tax tax : taxs) {
										chdMap.put(tax.getTaxCode(), tax.getAmount().getBigDecimalValue());
									}
								}else if(StringUtils.hasLength(fareComponentRefs) && "INF".equals(fareComponentRefs)){
									for (Tax tax : taxs) {
										infMap.put(tax.getTaxCode(), tax.getAmount().getBigDecimalValue());
									}
								}
							}
						}
					}
				}
			}
			flightRow.addColumn("fareBasic", fareBasic);
			//取票面价
			if(fareGroupArray != null && fareGroupArray.length > 0){
				for (FareGroup fareGroup : fareGroupArray) {
					FareComponentType[] fareComponentArray = fareGroup.getFare().getFareDetail().getFareComponentArray();
					if(fareComponentArray != null && fareComponentArray.length >0){
						String fareComponentRefs = fareGroup.getRefs().toString();
						if(StringUtils.hasLength(fareComponentRefs) && "ADT".equals(fareComponentRefs)){
							for (FareComponentType fareComponentType : fareComponentArray) {
								String segId = fareComponentType.getSegmentReference().getStringValue();
								if(segmentKey.equals(segId)){
									BigDecimal ticketPrice = fareComponentType.getPriceBreakdown().getPrice().getBaseAmount().getBigDecimalValue();
									ticketPriceMap.put("adtfare", ticketPrice);
									break;
								}
							}
						}else if(StringUtils.hasLength(fareComponentRefs) && "CHD".equals(fareComponentRefs)){
							for (FareComponentType fareComponentType : fareComponentArray) {
								String segId = fareComponentType.getSegmentReference().getStringValue();
								if(segmentKey.equals(segId)){
									BigDecimal ticketPrice = fareComponentType.getPriceBreakdown().getPrice().getBaseAmount().getBigDecimalValue();
									ticketPriceMap.put("chdfare", ticketPrice);
									break;
								}
							}
						}else if(StringUtils.hasLength(fareComponentRefs) && "INF".equals(fareComponentRefs)){
							for (FareComponentType fareComponentType : fareComponentArray) {
								String segId = fareComponentType.getSegmentReference().getStringValue();
								if(segmentKey.equals(segId)){
									BigDecimal ticketPrice = fareComponentType.getPriceBreakdown().getPrice().getBaseAmount().getBigDecimalValue();
									ticketPriceMap.put("inffare", ticketPrice);
									break;
								}
							}
						}
					}
				}
			}
		}
		if(ticketPriceMap != null && ticketPriceMap.size() > 0){
			for (Entry<String, BigDecimal> entry : ticketPriceMap.entrySet()) {
				Row ticketPriceRow = ticketPriceTable.addRow();
				ticketPriceRow.addColumn("code", entry.getKey());
				ticketPriceRow.addColumn("price", entry.getValue());
			}
		}
		flightRow.addColumn("ticketPrice", ticketPriceTable);
	}

	/**
	 * 添加paxs节点
	 * @param paxTable
	 * @param tktDocInfo
	 */
	private void addPaxInfo(Table paxTable, TicketDocInfo tktDocInfo,String currency,Table ticketTable,
			BigDecimal totalprice,List<BigDecimal> list,Map<Integer, String> cityTypeMap,Table tktCountryTypeTable) {
		TravelerInfo travelerInfo = tktDocInfo.getTravelerInfo();
		Row row = paxTable.addRow();
		// 旅客类型
		row.addColumn("paxtype", travelerInfo.getPTC());
		//名
		row.addColumn("firstname", travelerInfo.getGiven().getStringValue());
		//姓
		row.addColumn("lastname", travelerInfo.getSurname().getStringValue());
		//币种
		AcceptedPaymentFormType[] paymentArray = tktDocInfo.getPayments().getPaymentArray();
		if(paymentArray != null && paymentArray.length > 0){
			AcceptedPaymentFormType paymentFormType = paymentArray[0];
			String paytype = paymentFormType.getType().getCode();
			BigDecimal payprice = paymentFormType.getAmount().getBigDecimalValue();
			list.add(payprice);
			//支付价格
			row.addColumn("payprice", payprice);
			//支付方式
			row.addColumn("paytype", paytype);
			Other other = paymentFormType.getOther();
			if(other != null){
				String remarkArray = other.getRemarks().getRemarkArray(0).getStringValue();
				//fn
				row.addColumn("FN", remarkArray);
			}else{
				row.addColumn("FN", "");
			}
		}
		row.addColumn("currencyCode", currency);
		int k = 1;
		//tickets(当前旅客下的票)
		TicketDocumentType[] ticketDocumentArray = tktDocInfo.getTicketDocumentArray();
		if(ticketDocumentArray != null && ticketDocumentArray.length > 0){
			for (TicketDocumentType ticketDocumentType : ticketDocumentArray) {
				//票号
				String ticketNo = ticketDocumentType.getTicketDocNbr();
				//国内国际标识
				String countryType = ticketDocumentType.getType().getCode();
				Row tktCountryTypeRow = tktCountryTypeTable.addRow();
				tktCountryTypeRow.addColumn("tktNo", ticketNo);
				tktCountryTypeRow.addColumn("countryType", countryType);
				CpnNbrs cpnNumbers = ticketDocumentType.getCpnNbrs();
				//当前票号下航段号
				BigInteger[] cpnNo= cpnNumbers.getCpnNbrArray();
				for (int i = 0; i < cpnNo.length; i++) {
					Row ticketRow = ticketTable.addRow();
					ticketRow.addColumn("tickerno", ticketNo);
					ticketRow.addColumn("flightid",k);
					cityTypeMap.put(k, countryType);
					k++;
				}
			}
		}
		row.addColumn("tickets", ticketTable);
	}

	/**
	 *
	 */
	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet ret, CommandData input) {
		AirDocDisplayRSDocument doc = AirDocDisplayRSDocument.Factory.newInstance();
		AirDocDisplayRSDocument.AirDocDisplayRS root = doc.addNewAirDocDisplayRS();
		try {
			String errorcode = ret.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = root.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			} else {
				root.addNewSuccess();
				// 请求发送方
				String crsc = ret.getParm("crsc").getStringColumn();
				// 请求发送方所在地
				String crsl = ret.getParm("crsl").getStringColumn();
				// pnr
				String pnr = "";
				String oneEPNR = input.getParm("pnr").getStringColumn();
				Table tktCountryTypeTable = input.getParm("tktCountryTypeTable").getTableColumn();
				TicketDocInfos tktDocInfos = root.addNewResponse().addNewTicketDocInfos();
				// 遍历乘机人
				Table paxTable = ret.getParm("paxs").getTableColumn();
				int paxTableSize = paxTable==null?0:paxTable.getRowCount();
				for (int i=0;i<paxTableSize;i++) {
					Row paxRow = paxTable.getRow(i);

					org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo tktDocInfo = tktDocInfos.addNewTicketDocInfo();
					// 添加乘机人节点
					CouponTravelerDetailType traveler = tktDocInfo.addNewTraveler();
					traveler.addNewSurname().setStringValue(paxRow.getColumn("lastname").getStringColumn());
					traveler.addNewGiven().setStringValue(paxRow.getColumn("firtname").getStringColumn());
					traveler.setPTC(paxRow.getColumn("paxtype").getStringColumn());
					// Birthday
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(paxRow.getColumn("birth").getDateColumn());
					traveler.setBirthDate(calendar);

					// 添加请求发送方节点
					IssuingAirlineInfo airlineInfo = tktDocInfo.addNewIssuingAirlineInfo();
					airlineInfo.setAirlineName(crsc);
					airlineInfo.setPlace(crsl);
					// 添加pnr
					BookingReferences bookingReferences = tktDocInfo.addNewBookingReferences();
					BookingReferenceType sysbookingReference = bookingReferences.addNewBookingReference();
					BookingReferenceType oneEbookingReference = bookingReferences.addNewBookingReference();
					sysbookingReference.addNewAirlineID().setStringValue(crsc);
					// 添加总价节点
					String totalPrice = paxRow.getColumn("totalprice").getStringColumn();
					//货币代码
					String currencyCode = paxRow.getColumn("currencyCode").getStringColumn();
					OrderPaymentFormType payment = tktDocInfo.addNewPayments().addNewPayment();
					payment.addNewMethod().addNewCash().setCashInd(true);
					SimpleCurrencyPriceType amount = payment.addNewAmount();
					amount.setCode(currencyCode);
					BigDecimal totalPriceBigDecimal = new BigDecimal(totalPrice);
					amount.setBigDecimalValue(totalPriceBigDecimal);
					// 添加价格详情节点
					FareInfo fareInfo = tktDocInfo.addNewFareInfo();
					Amount baseFareAmount = fareInfo.addNewBaseFare().addNewAmount();
					baseFareAmount.setBigDecimalValue(new BigDecimal(paxRow.getColumn("farePrice").getStringColumn()));
					baseFareAmount.setCode(currencyCode);
					
					org.iata.iata.edist.TotalFareTransactionType.Amount totalAmount = fareInfo.addNewTotal().addNewAmount();
					totalAmount.setBigDecimalValue(totalPriceBigDecimal);
					totalAmount.setCode(currencyCode);
					// 税
					Table taxTable = paxRow.getColumn("taxs").getTableColumn();
					int taxTableSize = taxTable==null?0:taxTable.getRowCount();
					if(taxTableSize>0) {
						Breakdown breakDown = fareInfo.addNewTaxes().addNewBreakdown();
						for(int j=0;j<taxTableSize;j++) {
							Row taxRow = taxTable.getRow(j);
							Tax taxEle = breakDown.addNewTax();
							CurrencyAmountOptType taxAmount = taxEle.addNewAmount();
							taxAmount.setBigDecimalValue(new BigDecimal(taxRow.getColumn("Amount").getStringColumn()));
							taxAmount.setCode(currencyCode);
							taxEle.setTaxCode(taxRow.getColumn("TaxCode").getStringColumn());
						}
					}
					
					OriginDestination newOriginDestination = tktDocInfo.addNewOriginDestination();
					String oricode = paxRow.getColumn("oricitycode").getStringColumn();
					//起始地三字码
					newOriginDestination.setOrigin(oricode);
					String destcode = paxRow.getColumn("destcitycode").getStringColumn();
					//目的地三字码
					newOriginDestination.setDestination(destcode);
					// 添加票节点
					// 出票日期时间
					String outTktDateString = paxRow.getColumn("outtktdate").getStringColumn();
					DateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmm");
					Date outTktDate = sdf.parse(outTktDateString);
					// 出票日期
					Calendar otktCalendar = Calendar.getInstance();
					otktCalendar.setTime(outTktDate);
					// 出票时间
					String otktTime = new SimpleDateFormat("HH:mm").format(outTktDate);

					Table tktTable = paxRow.getColumn("tkts").getTableColumn();
					int tktTableSize = tktTable==null?0:tktTable.getRowCount();
					for (int j=0;j<tktTableSize;j++) {
						Row tktRow = tktTable.getRow(j);
						TicketDocument tktEle = tktDocInfo.addNewTicketDocument();
						String tktno = tktRow.getColumn("tktno").getStringColumn();
						tktEle.setTicketDocNbr(tktno);
						if(tktCountryTypeTable != null && tktCountryTypeTable.getRowCount() > 0){
							for (int k = 0; k < tktCountryTypeTable.getRowCount(); k++) {
								Row tktCountryTypeRow = tktCountryTypeTable.getRow(k);
								if(tktno.equals(tktCountryTypeRow.getColumn("tktNo").getStringColumn())){
									tktEle.addNewType().setCode(tktCountryTypeRow.getColumn("countryType").getStringColumn());
								}
							}
						}
						// 联票序号
						tktEle.setNumberofBooklets(new BigInteger(j+1+""));
						tktEle.setDateOfIssue(otktCalendar);
						tktEle.setTimeOfIssue(otktTime);
						// 添加航段节点
						Table segTable = tktRow.getColumn("segs").getTableColumn();
						int segTableSize = segTable==null?0:segTable.getRowCount();
						for (int k=0; k<segTableSize;k++) {
							Row segRow = segTable.getRow(k);
							CouponInfoType couponInfo = tktEle.addNewCouponInfo();
							couponInfo.setCouponNumber(new BigInteger(segRow.getColumn("segno").getStringColumn()));
							couponInfo.addNewFareBasisCode().setCode(segRow.getColumn("farebasiscode").getStringColumn());
							couponInfo.addNewStatus().setCode(segRow.getColumn("tktstatus").getStringColumn());
							// 航班信息
							SoldAirlineInfo soldAirline = couponInfo.addNewSoldAirlineInfo();
							Date oriDate = segRow.getColumn("oridate").getDateColumn("yyyyMMdd HHmm");

							// 出发时间
							String oritime = segRow.getColumn("oritime").getStringColumn();
							soldAirline.addNewDepartureDateTime().setTime(oritime);

							Date destDate = segRow.getColumn("destdate").getDateColumn("yyyyMMdd HHmm");
							// 到达时间
							String desttime = segRow.getColumn("desttime").getStringColumn();
							soldAirline.addNewArrivalDateTime().setTime(desttime);

							Departure departure = soldAirline.addNewDeparture();
							// 出发日期
							departure.addNewAirportCode().setStringValue(segRow.getColumn("oricode").getStringColumn());
							Calendar calendar0 = Calendar.getInstance();
							calendar0.setTime(oriDate);
							departure.setDate(calendar0);
							departure.setTime(oritime);
							

							FlightArrivalType  arrival = soldAirline.addNewArrival();
							// 到达日期
							arrival.addNewAirportCode().setStringValue(segRow.getColumn("destcode").getStringColumn());
							calendar0.setTime(destDate);
							arrival.setDate(calendar0);
							arrival.setTime(desttime);

							MarketingCarrierFlightType marketingCarrier = soldAirline.addNewMarketingCarrier();
							//航空公司二字码
							marketingCarrier.addNewAirlineID().setStringValue(segRow.getColumn("airlineCode").getStringColumn());
							//航班号
							FlightNumber fltno = marketingCarrier.addNewFlightNumber();
							fltno.setStringValue(DateUtils.setFlightNo(segRow.getColumn("flightno").getStringColumn()));
							//航班号后缀
							fltno.setOperationalSuffix(segRow.getColumn("flightnosuffix").getStringColumn());
							//机型  
							String planestype = segRow.getColumn("planestype").getStringColumn();
							soldAirline.addNewEquipment().addNewAircraftCode().setStringValue(planestype);
							
							pnr = segRow.getColumn("pnr").getStringColumn();
						}
						
					}
					sysbookingReference.setID(pnr);
					oneEbookingReference.setID(oneEPNR);
					oneEbookingReference.addNewOtherID().setStringValue("1E");
				}
			}
		} catch (Exception e) {
			doc = AirDocDisplayRSDocument.Factory.newInstance();
			root = doc.addNewAirDocDisplayRS();
			// 存在错误信息
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(
					ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}
}