package com.travelsky.quick.business;

import java.text.ParseException;
import java.util.Calendar;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.LCCFlightType;
import org.iata.iata.edist.LCCItineraryReadRQDocument;
import org.iata.iata.edist.LCCItineraryReadRQDocument.LCCItineraryReadRQ.Query.LCCItineraryInfo;
import org.iata.iata.edist.LCCItineraryReadRSDocument;
import org.iata.iata.edist.LCCItineraryReadRSDocument.LCCItineraryReadRS;
import org.iata.iata.edist.LCCItineraryReadRSDocument.LCCItineraryReadRS.Response.LCCItineraryInfos;
import org.iata.iata.edist.MarketingCarrierFlightType;
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
import com.travelsky.quick.util.StatusUtil;
import com.travelsky.quick.util.helper.OrderOpManager;
import com.travelsky.quick.util.helper.TipMessager;


/**
 * @author 作者:LiHz
 * @version 0.1
 * 类说明:
 *		我的行程查询
 */
@Service("LCC_ITINERARYREAD_SERVICE")
public class APIItineraryReadNDCBusiness  extends AbstractService<ApiContext>{

	/**
	 *
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIItineraryReadNDCBusiness.class);
	
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
				LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_ITINERARY_READ, ApiServletHolder.getApiContext().getLanguage()), e);
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
		LCCItineraryReadRQDocument rootDoc = null;
		rootDoc = LCCItineraryReadRQDocument.Factory.parse(xmlInput);

		LCCItineraryReadRQDocument.LCCItineraryReadRQ reqdoc = rootDoc.getLCCItineraryReadRQ();
		ApiContext apiCtx = context.getContext();
		// 部门ID
		String deptno = apiCtx.getTicketDeptid();
		input.addParm("tktdeptid",deptno);
		LCCItineraryInfo itinerary = reqdoc.getQuery().getLCCItineraryInfo();
//		//证件号
//		String number =	itinerary.getNumber();
		//行程起始日期
		String flightstart = itinerary.getStartDate();
		//行程结束日期
		String flightend = itinerary.getEndtDate();
		//行程状态（1 计划  2已使用）
		String status = itinerary.getStatus();
//		input.addParm("number", number);
		input.addParm("memberid", apiCtx.getUserID());
		input.addParm("flightstart", flightstart);
		input.addParm("flightend", flightend);
		input.addParm("usedflag", status);
		input.addParm("pagenum", itinerary.getPageNum());
		input.addParm("pagesize", itinerary.getPageZize());
		input.addParm("sortflag", itinerary.getSortType());

	}


	/**
	 * 数据提交shopping后台
	 * @param input 请求的XML
	 * @param context context对象
	 * @return 查询结果
	 */
	public CommandRet getResponseBean() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		OrderOpManager orderOpManager = new OrderOpManager();
		return orderOpManager.querttravel(input, context);
	}


	/**
	 * 拼返回的shopping信息
	 * @param commandRet 请求的数据
	 * @param input 请求的XML
	 * @return 拼接的XML
	 */

	public XmlObject transRespBeanToXmlBean(Object commandRet ,CommandData input){
		CommandRet xmlOutput = (CommandRet)commandRet;
		LCCItineraryReadRSDocument doc = LCCItineraryReadRSDocument.Factory.newInstance();
		LCCItineraryReadRS rs = doc.addNewLCCItineraryReadRS();
		try{
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}
			//反回正确的值
			else{
				getXML(xmlOutput ,rs);

			}
		}
		catch (Exception e) {
			doc = LCCItineraryReadRSDocument.Factory.newInstance();
			rs = doc.addNewLCCItineraryReadRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}

	/**
	 * 拼接XML
	 * @param xmlOutput 后台返回的数据对象
	 * @param rs XML节点
	 * @throws ParseException 时间转换异常
	 */
	private static void getXML(CommandRet xmlOutput,LCCItineraryReadRS rs) throws ParseException{
		rs.addNewSuccess();
		LCCItineraryInfos itineraryInfos = rs.addNewResponse().addNewLCCItineraryInfos();
		Table flights = xmlOutput.getParm("flights").getTableColumn();
		int total = flights==null? 0 : flights.getRowCount();
		// 设置总数量
		itineraryInfos.setTotalItinraryQuantity(String.valueOf(total));
		ApiContext apiCtx = ApiServletHolder.getApiContext();
		if(total>0){
			for(int i=0;i< total;i++){
				Row  flightRow = flights.getRow(i);
				LCCFlightType flight = itineraryInfos.addNewLCCFlight();
				//orderid
				String orderId  = flightRow.getColumn("orderid").getStringColumn();
				flight.setOrderID(orderId);
				String  flightTime = flightRow.getColumn("flighttime").getStringColumn();
				//航空公司代码
				String airlinecd = flightRow.getColumn("airlinecd").getStringColumn();
				//航班号
				String flightno =flightRow.getColumn("flightno").getStringColumn();
				//出发地机场三字码
				String ori	= flightRow.getColumn("ori").getStringColumn();
				//目的地机场三字码
				String dest	= flightRow.getColumn("dest").getStringColumn();
				//出发航站楼
				String oriteminal = flightRow.getColumn("oriteminal").getStringColumn();
				 //到达航站楼
				String destterminal	= flightRow.getColumn(
						"destterminal").getStringColumn();
				//往返程类型(S单程 R往返程)
				String routtype	= flightRow.getColumn("routtype").getStringColumn();
				//交付状态
				String deliverstatus = flightRow.getColumn("deliverstatus").getStringColumn();
				//往返程类型(S单程 R往返程)
				flight.setTripType(routtype);
				//状态
				flight.addNewStatus().addNewStatusCode().setCode(StatusUtil.getStatus(deliverstatus));
				Departure departure = flight.addNewDeparture();
				//
				departure.addNewAirportCode().setStringValue(ori);
				String language = ApiServletHolder.getApiContext().getLanguage();
				String oricityname = StatusUtil.getLanguageName(flightRow.getColumn("oricityname").getObjectColumn(), language);
				String destcityname = StatusUtil.getLanguageName(flightRow.getColumn("destcityname").getObjectColumn(), language);
				if("".equals(oricityname)){
					oricityname = flightRow.getColumn("oricitynameen").getStringColumn();
				}
				if("".equals(destcityname)){
					destcityname = flightRow.getColumn("destcitynameen").getStringColumn();
				}
				departure.setAirportName(oricityname);
				String[] flightDate = null;
				if(StringUtils.hasLength(flightTime) && flightTime.length() > 15){
					flightDate = flightTime.substring(0, 16).split(" ");
				}
				String	flightday = flightRow.getColumn("flightday").getStringColumn().substring(0,8);
				if (flightday.trim()!=null) {
					Calendar destDate = DateUtils.getInstance().parseDate(flightday, "yyyyMMdd");
					departure.setDate(destDate);
				}
				String	oritime = flightRow.getColumn("oritime").getStringColumn();
				departure.setTime(oritime);
				//出发航站楼
				departure.addNewTerminal().setName(oriteminal);
				//目的地
				FlightArrivalType arrival = flight.addNewArrival();
				//目的地机场三字码
				arrival.addNewAirportCode().setStringValue(dest);
				arrival.setAirportName(destcityname);
				//到达航站楼
				arrival.addNewTerminal().setName(destterminal);
				MarketingCarrierFlightType marketing = flight.addNewMarketingCarrier();
				//航空公司代码
				marketing.addNewAirlineID().setStringValue(airlinecd);
				//航班号
				marketing.addNewFlightNumber().setStringValue(flightno);
			}
		}
	}
	
}
