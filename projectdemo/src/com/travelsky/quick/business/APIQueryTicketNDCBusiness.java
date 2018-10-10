package com.travelsky.quick.business;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirDocDisplayRQDocument;
import org.iata.iata.edist.AirDocDisplayRQDocument.AirDocDisplayRQ.Target.Enum;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Target;
import org.iata.iata.edist.AirDocDisplayRSDocument;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.FareInfo;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.FareInfo.Taxes;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.OriginDestination;
import org.iata.iata.edist.AirDocDisplayRSDocument.AirDocDisplayRS.Response.TicketDocInfos.TicketDocInfo.Payments;
import org.iata.iata.edist.BookingReferenceType;
import org.iata.iata.edist.BookingReferencesDocument.BookingReferences;
import org.iata.iata.edist.CouponInfoType;
import org.iata.iata.edist.CouponInfoType.SoldAirlineInfo;
import org.iata.iata.edist.CouponTravelerDetailType;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.EmailType;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.IssuingAirlineInfoDocument.IssuingAirlineInfo;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.MsgDocumentType;
import org.iata.iata.edist.OrderPaymentFormType;
import org.iata.iata.edist.OrderPaymentFormType.Method;
import org.iata.iata.edist.PhoneContactType;
import org.iata.iata.edist.TaxDetailType.Breakdown.Tax;
import org.iata.iata.edist.TicketDocumentDocument1.TicketDocument;
import org.iata.iata.edist.TicketDocumentType;
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
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.APICacheHelper;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 查询票面
 * 
 * @author wangyicheng
 * @version 0.1
 */
@Service("LCC_QUERYTICKET_SERVICE")
public class APIQueryTicketNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 查询票面
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APIQueryTicketNDCBusiness.class);
	
	
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
					ErrCodeConstants.API_UNKNOW_QUERYTICKET,
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
		return orderOpManager.queryTicket(input, context);
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
		
		AirDocDisplayRQDocument rootDoc = AirDocDisplayRQDocument.Factory.parse(xmlInput);
		
		AirDocDisplayRQDocument.AirDocDisplayRQ reqDoc = rootDoc.getAirDocDisplayRQ();
		APICacheHelper.setDeptInfo(ApiServletHolder.get(), reqDoc.getParty());
		
		//order不需要，返回xml中需要
		String echoToken = reqDoc.getEchoToken();
		if(StringUtils.hasLength(echoToken)){
			input.addParm("echoToken", echoToken);
		}
		
		String correlationID = reqDoc.getCorrelationID();
		if(StringUtils.hasLength(correlationID)){
			input.addParm("correlationID", correlationID);
		}
		
		String transactionIdentifier = reqDoc.getTransactionIdentifier();
		if(StringUtils.hasLength(transactionIdentifier)){
			input.addParm("transactionIdentifier", transactionIdentifier);
		}
		
		String version = reqDoc.getVersion();
		if(StringUtils.hasLength(version)){
			input.addParm("version", version);
		}
		
		Enum target = reqDoc.getTarget();
		if(target != null){
			input.addParm("target",String.valueOf(target));
		}
		
		BigInteger sequenceNmbr = reqDoc.getSequenceNmbr();
		if(sequenceNmbr != null){
			input.addParm("sequenceNmbr",String.valueOf(sequenceNmbr));
		}
		
		Calendar timeStamp = reqDoc.getTimeStamp();
		if(timeStamp != null){
			input.addParm("timeStamp", timeStamp.getTime(), "yyyyMMdd HH:mm:ss");
		}
		
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		input.addParm("language", language);
		// 部门ID  
		input.addParm("tktdeptid",ApiServletHolder.getApiContext().getTicketDeptid());
		// 会员账号
		input.addParm("memberid", context.getContext().getUserID());
		StringBuffer sb = new StringBuffer("");
		TicketDocumentType[] ticketDocumentArray = reqDoc.getQuery().getTicketDocumentArray();
		if(null != ticketDocumentArray && ticketDocumentArray.length>0){
			for(int i=0;i<ticketDocumentArray.length;i++){
				String ticketNo = ticketDocumentArray[i].getTicketDocNbr();
				if(!StringUtils.hasLength(ticketNo)){
					LOGGER.info(TipMessager.getInfoMessage(
							ErrCodeConstants.API_NULL_TICKET_NO, 
							ApiServletHolder.getApiContext().getLanguage()));
					throw APIException.getInstance(ErrCodeConstants.API_NULL_TICKET_NO);
				}
				sb.append(ticketNo).append(",");
			}
		}else{
			 LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_TICKET_NO, 
						ApiServletHolder.getApiContext().getLanguage()));
			 throw APIException.getInstance(ErrCodeConstants.API_NULL_TICKET_NO);
		}
		String ticketNos = sb.toString();
		if(StringUtils.hasLength(ticketNos) && ticketNos.endsWith(",")){
			ticketNos = ticketNos.substring(0, ticketNos.length()-1);
		}
		input.addParm("ticketNo", ticketNos);
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
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		AirDocDisplayRSDocument oddoc = AirDocDisplayRSDocument.Factory.newInstance();
		AirDocDisplayRS rprs = oddoc.addNewAirDocDisplayRS();
		try {
			String errorcode = commandRet.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			} else {
				//xml中根节点属性值
				String echoToken = input.getParm("echoToken").getStringColumn();
				if(StringUtils.hasLength(echoToken)){
					rprs.setEchoToken(echoToken);
				}
				String correlationID = input.getParm("correlationID").getStringColumn();
				if(StringUtils.hasLength(correlationID)){
					rprs.setCorrelationID(correlationID);
				}
				String transactionIdentifier = input.getParm("transactionIdentifier").getStringColumn();
				if(StringUtils.hasLength(transactionIdentifier)){
					rprs.setTransactionIdentifier(transactionIdentifier);
				}
				String version = input.getParm("version").getStringColumn();
				if(StringUtils.hasLength(version)){
					rprs.setVersion(version);
				}
				String targetString = input.getParm("target").getStringColumn();
				if(StringUtils.hasLength(targetString)){
					Target.Enum target = Target.Enum.forString(targetString);
					rprs.setTarget(target);
				}
				String sequenceNmbr = input.getParm("sequenceNmbr").getStringColumn();
				if(StringUtils.hasLength(sequenceNmbr)){
					rprs.setSequenceNmbr(BigInteger.valueOf(Integer.valueOf(sequenceNmbr)));
				}
				Date date = input.getParm("timeStamp").getDateColumn();
				if(date != null){
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					rprs.setTimeStamp(calendar);
				}
				//成功标识
				rprs.addNewSuccess();
				/**
				 * 返回xml信息
				 */
				// Response
				Response response = rprs.addNewResponse();
				TicketDocInfos ticketDocInfos = response.addNewTicketDocInfos();
				Table resultTable = commandRet.getParm("result").getTableColumn();
				if(resultTable != null && resultTable.getRowCount()>0){
					for (int k = 0; k < resultTable.getRowCount(); k++) {
						//航段协助map，key值是航段号，value是当前航段号下的航班数据Row
						Map<Integer,Row> couponHelpMap = new HashMap<Integer, Row>();
						Row resultRow = resultTable.getRow(k);
						CommandData ticketsData = resultRow.getColumn("tickets").getObjectColumn();
						String oneEPNR = resultRow.getColumn("1EPNR").getStringColumn();
						TicketDocInfo newTicketDocInfo = ticketDocInfos.addNewTicketDocInfo();
						//---------------------旅客信息---------------------------------
						CouponTravelerDetailType addNewTraveler = newTicketDocInfo.addNewTraveler();
						String surname = ticketsData.getParm("SURN").getStringColumn();
						String givename = ticketsData.getParm("GIVN").getStringColumn();
						//姓
						addNewTraveler.addNewSurname().setStringValue(givename);
						//名
						addNewTraveler.addNewGiven().setStringValue(surname);
						
						//类型
						String ptype = ticketsData.getParm("PTYP").getStringColumn();
						if(!StringUtils.hasLength(ptype)){
							ptype = "ADT";
						}
						addNewTraveler.setPTC(ptype);
						
						//出生日期 
						Calendar ori=Calendar.getInstance();
						Date birth = ticketsData.getParm("birth").getDateColumn();
						ori.setTime(birth);
						addNewTraveler.setBirthDate(ori);
						
						//---------------------IssuingAirlineInfo信息---------------------------------
						IssuingAirlineInfo newIssuingAirlineInfo = newTicketDocInfo.addNewIssuingAirlineInfo();
						String airline = ticketsData.getParm("CRSC").getStringColumn();
						//航空公司二字码
						newIssuingAirlineInfo.setAirlineName(airline);
						String cityPlace = ticketsData.getParm("CRSL").getStringColumn();
						//所在地
						newIssuingAirlineInfo.setPlace(cityPlace);
						
						//---------------------BookingReferences信息---------------------------------
						BookingReferences newBookingReferences = newTicketDocInfo.addNewBookingReferences();
						BookingReferenceType sysBookingReference = newBookingReferences.addNewBookingReference();
						BookingReferenceType oneEBookingReference = newBookingReferences.addNewBookingReference();
						
						//航空公司二字码
						sysBookingReference.addNewAirlineID().setStringValue(airline);
						
						//---------------------Payments信息---------------------------------
						Payments newPayments = newTicketDocInfo.addNewPayments();
						OrderPaymentFormType newPayment = newPayments.addNewPayment();
						Method newMethod = newPayment.addNewMethod();
						//需要判断是否是现金支付，如果是，true 否则 false
						String cashInd = ticketsData.getParm("FOPF").getStringColumn();
						boolean flag = false;
						if(cashInd.contains("CASH") || cashInd.contains("CA")){
							flag = true;
						}
						newMethod.addNewCash().setCashInd(flag);
						String code = ticketsData.getParm("TCUR").getStringColumn();
						
						//支付金额 
						String totalAmount = ticketsData.getParm("TAMT").getStringColumn();
						newPayment.addNewAmount().setBigDecimalValue(new BigDecimal(totalAmount));
						//支付方式code
						newPayment.getAmount().setCode(code);
						
						//---------------------OriginDestination信息---------------------------------
						OriginDestination newOriginDestination = newTicketDocInfo.addNewOriginDestination();
						String oricode = ticketsData.getParm("JORG").getStringColumn();
						//起始地三字码
						newOriginDestination.setOrigin(oricode);
						String destcode = ticketsData.getParm("JDST").getStringColumn();
						//目的地三字码
						newOriginDestination.setDestination(destcode);
						
						//---------------------FareInfo信息---------------------------------
						FareInfo newFareInfo = newTicketDocInfo.addNewFareInfo();
						//票价
						String baseFare = ticketsData.getParm("FAMT").getStringColumn();
						newFareInfo.addNewBaseFare().addNewAmount().setBigDecimalValue(new BigDecimal(baseFare));
						//全价
						newFareInfo.addNewTotal().addNewAmount().setBigDecimalValue(new BigDecimal(totalAmount));
						
						//税费 (多种)
						Taxes newTaxes = newFareInfo.addNewTaxes();
						
						String taxDetail = ticketsData.getParm("TAXF").getStringColumn();
						if(StringUtils.hasLength(taxDetail)){
							if(taxDetail.contains("+")){
								String[] taxDetails = taxDetail.split("\\+");
								for (int m = 0; m < taxDetails.length; m++) {
									String taxDetailsA = taxDetails[m];
									String[] tax = taxDetailsA.split("\\//");
									Tax newTax = newTaxes.addNewBreakdown().addNewTax();
									//税费金额
									newTax.addNewAmount().setBigDecimalValue(new BigDecimal(tax[1].split("\\/")[0]));
									//税费货币代码
									newTax.getAmount().setCode(tax[1].split("\\/")[1]);
									//税费code
									newTax.setTaxCode(tax[0]);
									//税费类型
									newTax.setTaxType("Tax");
								}
							}else{
								String[] tax = taxDetail.split("\\//");
								Tax newTax = newTaxes.addNewBreakdown().addNewTax();
								//税费金额
								CurrencyAmountOptType taxNewAmount = newTax.addNewAmount();
								taxNewAmount.setCode(tax[1].split("\\/")[1]);
								taxNewAmount.setBigDecimalValue(new BigDecimal(tax[1].split("\\/")[0]));
								//税费code
								newTax.setTaxCode(tax[0]);
								//税费类型
								newTax.setTaxType("Tax");
							}
						}
						//---------------------TicketDocument信息---------------------------------
						TicketDocument newTicketDocument = newTicketDocInfo.addNewTicketDocument();
						String ticketno = ticketsData.getParm("TKNB").getStringColumn();
						String countryType = "";
						//票号
						newTicketDocument.setTicketDocNbr(ticketno);
						//出票日期
						SimpleDateFormat sdf = DateUtils.getInstance().getSimDate("HH:mm");
						Date outTktDate = ticketsData.getParm("DTIS").getDateColumn();
						Calendar outTktCal = Calendar.getInstance();
						outTktCal.setTime(outTktDate);
						newTicketDocument.setDateOfIssue(outTktCal);
						//出票时间
						newTicketDocument.setTimeOfIssue(sdf.format(outTktDate));
						
						String pnr = "";
						Table couponTable = ticketsData.getParm("CPN").getTableColumn();
						if(couponTable != null && couponTable.getRowCount() > 0){
							for(int i = 0; i < couponTable.getRowCount(); i++){
								Row couponRow = couponTable.getRow(i);
								String cnbr = couponRow.getColumn("CNBR").getStringColumn();
								couponHelpMap.put(Integer.valueOf(cnbr), couponRow);
							}
						}
						if(couponHelpMap != null && couponHelpMap.size() > 0){
							for (int m = 1; m < couponHelpMap.size()+1; m++) {
								Row couponRow = couponHelpMap.get(m);
								pnr = couponRow.getColumn("PNR1").getStringColumn();
								countryType = couponRow.getColumn("countryType").getStringColumn();
								//航段信息(单程、往返)
								CouponInfoType newCouponInfo = newTicketDocument.addNewCouponInfo();
								//航段号
								newCouponInfo.setCouponNumber(BigInteger.valueOf(Integer.valueOf(couponRow.getColumn("CNBR").getStringColumn())));
								//票价基础
								String fareBasic = couponRow.getColumn("FBAS").getStringColumn();
								newCouponInfo.addNewFareBasisCode().setCode(fareBasic);
								//航段状态
								String couponStatus = couponRow.getColumn("CSTA").getStringColumn();
								if("O".equals(couponStatus) || "E".equals(couponStatus)){
									couponStatus = "OPEN FOR USE";
								}else if("R".equals(couponStatus)){
									couponStatus = "REFUNDED";
								}
								newCouponInfo.addNewStatus().setCode(couponStatus);
								
								SoldAirlineInfo newSoldAirlineInfo = newCouponInfo.addNewSoldAirlineInfo();
								SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
								//出发时间
								String dtime = couponRow.getColumn("DTME").getStringColumn();
								newSoldAirlineInfo.addNewDepartureDateTime().setTime(sdfTime.format(Unit.getDate(dtime, "HHmm")));
								//到达时间
								String atime = couponRow.getColumn("ATME").getStringColumn();
								newSoldAirlineInfo.addNewArrivalDateTime().setTime(sdfTime.format(Unit.getDate(atime, "HHmm")));
								
								Departure newDeparture = newSoldAirlineInfo.addNewDeparture();
								//出发地三字码
								String departCode = couponRow.getColumn("ORIG").getStringColumn();
								newDeparture.addNewAirportCode().setStringValue(departCode);
								//出发日期
								Date depart = couponRow.getColumn("DDAT").getDateColumn();
								Calendar departDate = Calendar.getInstance();
								departDate.setTime(depart);
								newDeparture.setDate(departDate);
								
								FlightArrivalType newArrival = newSoldAirlineInfo.addNewArrival();
								//到达地三字码
								String arrivalCode = couponRow.getColumn("DEST").getStringColumn();
								newArrival.addNewAirportCode().setStringValue(arrivalCode);
								//到达日期
								Date arrival = couponRow.getColumn("ADAT").getDateColumn();
								Calendar arrivalDate = Calendar.getInstance();
								arrivalDate.setTime(arrival);
								newArrival.setDate(arrivalDate);
								
								MarketingCarrierFlightType newMarketingCarrier = newSoldAirlineInfo.addNewMarketingCarrier();
								//航空公司二字码
								newMarketingCarrier.addNewAirlineID().setStringValue(airline);
								//航班号
								String flightno = couponRow.getColumn("FLTN").getStringColumn();
								FlightNumber flightNumber = newMarketingCarrier.addNewFlightNumber();
								flightNumber.setStringValue(DateUtils.setFlightNo(flightno));
								flightNumber.setOperationalSuffix(couponRow.getColumn("FLTA").getStringColumn());
//										newMarketingCarrier.setResBookDesigCode("");
								
								//机型  
								String planestype = couponRow.getColumn("planestype").getStringColumn();
								if(!StringUtils.hasLength(planestype)){
									newSoldAirlineInfo.addNewEquipment().addNewAircraftCode();
								}else{
									newSoldAirlineInfo.addNewEquipment().addNewAircraftCode().setStringValue(planestype);;
								}
							}
						}
						newTicketDocument.addNewType().setCode(countryType);
						sysBookingReference.setID(pnr);
						oneEBookingReference.setID(oneEPNR);
						oneEBookingReference.addNewOtherID().setStringValue("1E");
					}
				}
			}
		} catch (Exception e) {
			oddoc = AirDocDisplayRSDocument.Factory.newInstance();
			rprs = oddoc.addNewAirDocDisplayRS();
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(
					ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return oddoc;
	}
}
