package com.travelsky.quick.business;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.BilateralTimeLimitsType.BilateralTimeLimit;
import org.iata.iata.edist.CurrencyAmountOptType;
import org.iata.iata.edist.DataListType;
import org.iata.iata.edist.DataListType.FlightSegmentList;
import org.iata.iata.edist.DepartureDocument.Departure;
import org.iata.iata.edist.DescriptionType.Text;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.FlightArrivalType;
import org.iata.iata.edist.FlightNumberDocument.FlightNumber;
import org.iata.iata.edist.ListOfFlightSegmentType;
import org.iata.iata.edist.ListOfOfferPenaltyType;
import org.iata.iata.edist.ListOfOfferPenaltyType.Penalty;
import org.iata.iata.edist.MarketingCarrierFlightType;
import org.iata.iata.edist.SegmentReferencesDocument.SegmentReferences;
import org.iata.iata.edist.ServiceCoreType.Associations;
import org.iata.iata.edist.ServiceCoreType.BookingInstructions;
import org.iata.iata.edist.ServiceCoreType.Name;
import org.iata.iata.edist.ServiceEncodingType.Code;
import org.iata.iata.edist.ServiceIDType;
import org.iata.iata.edist.ServiceListRQDocument;
import org.iata.iata.edist.ServiceListRQDocument.ServiceListRQ.Query;
import org.iata.iata.edist.ServiceListRSDocument;
import org.iata.iata.edist.ServiceListRSDocument.ServiceListRS.Services;
import org.iata.iata.edist.ServiceListRSDocument.ServiceListRS.Services.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Item;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.ShoppingManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 
 * @author MaRuifu 2016年5月3日下午3:14:50
 * @version 0.1
 * 类说明:辅营产品信息接口
 */
@org.springframework.stereotype.Service("LCC_SERVICELIST_SERVICE")
public class APIServiceListNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 7847464967081218159L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIServiceListNDCBusiness.class);
	/**
	 * 
	 * @param  context SelvetContext<ApiContext>
	 * @throws Exception Exception
	 */
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
					ErrCodeConstants.API_UNKNOW_AUXILIARY_QUERY, 
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}
	
	private CommandRet getResult() {
		ShoppingManager lmanager = new ShoppingManager();
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		return lmanager.productshopping(context.getInput(),context);
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
		ServiceListRQDocument rootDoc = ServiceListRQDocument.Factory.newInstance();
		rootDoc = ServiceListRQDocument.Factory.parse(xmlInput);
		ServiceListRQDocument.ServiceListRQ reqDoc = rootDoc.getServiceListRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid",deptno);
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		input.addParm("memberid", context.getContext().getUserID());
		
		// 订单号
		Query query = reqDoc.getQuery();
		if (query == null
				|| query.getOrderID() == null
				|| !StringUtils.hasLength(query.getOrderID().getStringValue())) {
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_NULL_ORDER_NO, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_ORDER_NO);
		}
		String orderNo = query.getOrderID().getStringValue();
		input.addParm("orderno", orderNo);
	}
	
	/**
	 * 将底层系统返回的CommandRet转换为XmlObject.<br>
	 * 相关CommandRet参数请参考辅营产品信息文档
	 * @param commandRet CommandRet
	 * @param input CommandData
	 * @return XmlObject
	 */
	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		ServiceListRSDocument doc = ServiceListRSDocument.Factory.newInstance();
		ServiceListRSDocument.ServiceListRS root = doc.addNewServiceListRS();
		try {
		// 判断是否存在错误信息
		String errCode = commandRet.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode)) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return doc;
		}
		root.addNewSuccess();
		Item productListItem = commandRet.getParm("productlist");
		Table productsListTable = productListItem.getTableColumn();
		int count = productsListTable == null? 0 : productsListTable.getRowCount();
		
		// ServiceListRS->Services
		Services  services = root.addNewServices();
		
		// ServiceListRS->DataLists
		DataListType dataList = root.addNewDataLists();
		FlightSegmentList flightSegmentList = dataList.addNewFlightSegmentList();
		/*
		 *  因为返回的辅营列表是根据航班分类的，而不同的航班可能会有相同的辅营。
		 *  固定义此set,记录辅营key(辅营代码) 值，用于去除重复的辅营
		 */
//		Set<String> productSet = new HashSet<String>();
		List<String> codeList = new ArrayList<String>();
		for (int i=0; i<count; i++) {
			Row productRow = productsListTable.getRow(i);
			flightSegment(flightSegmentList.addNewFlightSegment(),productRow,i);
			// products
			Item productsItem = productRow.getColumn("products");
			Table productsTable = productsItem.getTableColumn();
			int productsSize = productsTable == null? 0 : productsTable.getRowCount();
			if (productsSize > 0) {
				getMet(dataList, services, productRow, productsTable, productsSize,i,codeList);
			}
		}
	} catch (Exception e) {
		doc = ServiceListRSDocument.Factory.newInstance();
		root = doc.addNewServiceListRS();
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
	/**
	 * 
	 * @param strUtils StringUtils
	 * @param services Services
	 * @param penaltyList ListOfOfferPenaltyType
	 * @param productRow Row
	 * @param productsTable Table
	 * @param count0  int
	 * @param codeList 
	 */
	private void getMet(
			DataListType dataList, Services services, Row productRow,
			Table productsTable, int count0,int mum, List<String> codeList) {
		ListOfOfferPenaltyType penaltyList = dataList.addNewPenaltyList();
		
		for (int i0=0; i0<count0; i0++) {
			Service service = services.addNewService();
			Row productsRow = productsTable.getRow(i0);
			String code = productsRow.getColumn("code").getStringColumn();
			String airlinecd = productRow.getColumn("airlinecd").getStringColumn();
			//<!--辅营code-->
			//<ServiceID Owner="QK">MAY5</ServiceID>
			ServiceIDType serviceID = service.addNewServiceID();
			serviceID.setStringValue(code);
			serviceID.setOwner(airlinecd);
			//<!--辅营名称-->
			//<Name>{"en_US": "xbag9","zh_CN": "9公斤"}</Name>
			Name name = service.addNewName();
			name.setStringValue(productsRow.getColumn("name").getStringColumn());
			//<!--typecode-->
			//<Encoding><Code>XBAG</Code></Encoding>
			Code newCode = service.addNewEncoding().addNewCode();
			newCode.setStringValue(productsRow.getColumn("typecode").getStringColumn());
			//<!--提起购买时限（分钟）-->
			BilateralTimeLimit timeLimit = service.addNewTimeLimits().addNewBilateralTimeLimits()
					.addNewBilateralTimeLimit();
//			timeLimit.setName(productsRow.getColumn("atime").getStringColumn());
			Integer min = productsRow.getColumn("atime").getIntegerColumn();
			timeLimit.setDescription(min+"H");
			//<!--描述remark-->
			Text text = service.addNewDescriptions().addNewDescription().addNewText();
			text.setStringValue(productsRow.getColumn("remark").getStringColumn());
			//<!-- Code：货币三字码   Total：金额-->
			CurrencyAmountOptType total = service.addNewPrice().addNewTotal();
			String currencyCode = productsRow.getColumn("currencyCode").getStringColumn();
			String price = productsRow.getColumn("price").getStringColumn();
			total.setCode(currencyCode);
			if(StringUtils.hasLength(price)) {
				total.setStringValue(price);
			}else {
				total.setStringValue("0");
			}
			//<!--货币符号--><!--ssrtype-->
			BookingInstructions booking = service.addNewBookingInstructions();
			String ssrtype =  productsRow.getColumn("ssrtype").getStringColumn();
			booking.addNewSSRCode().setStringValue(code);
//			booking.addNewText().setStringValue(productsRow.getColumn("display").getStringColumn());
			
			Associations associations = service.addNewAssociations();
			SegmentReferences references = associations.addNewFlight().addNewSegmentReferences();
//			String flightno =productRow.getColumn("flightno").getStringColumn();
			references.setStringValue("SEG"+(mum+1));
//			List<String> list = new ArrayList<String>();
//			list.add(code+ssrtype);
//			associations.addNewOffer().setOfferReferences(list);
			if(!codeList.contains(code)){
				codeList.add(code);
				Penalty penalty = penaltyList.addNewPenalty();
				penalty.setObjectKey(code);
				String refundedtext = productsRow.getColumn("refunded").getStringColumn();
				penalty.addNewApplicableFeeRemarks().addNewRemark().setStringValue(refundedtext);
			}
		}
	}

	/**
	 * 航班信息
	 * 
	 * @param flightSegment ListOfFlightSegmentType
	 * @param productRow  Row
	 * void    返回类型 
	 *
	 */
	private void flightSegment(ListOfFlightSegmentType flightSegment,Row productRow,int num){
//		String flightid = productRow.getColumn("flightid").getStringColumn();
		String airlinecd = productRow.getColumn("airlinecd").getStringColumn();
		String flightno =productRow.getColumn("flightno").getStringColumn();
		flightSegment.setSegmentKey("SEG"+(num+1));
		Departure depar = flightSegment.addNewDeparture();
		depar.addNewAirportCode().setStringValue(productRow.getColumn("oricode").getStringColumn());
		Date date = productRow.getColumn("oridate").getDateColumn();
		Date destdate = productRow.getColumn("destdate").getDateColumn();
		String string = Unit.getString(date, "yyyy-MM-dd");
		String dest = Unit.getString(destdate, "yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(Unit.getDate(string, "yyyy-MM-dd"));
		depar.setDate(calendar);
		depar.setTime(productRow.getColumn("oriTime").getStringColumn());
		depar.setAirportName(productRow.getColumn("oriname").getStringColumn());

		FlightArrivalType arrival = flightSegment.addNewArrival();
		arrival.addNewAirportCode().setStringValue(productRow.getColumn("destcode").getStringColumn());
		calendar.setTime(Unit.getDate(dest, "yyyy-MM-dd"));
		arrival.setDate(calendar);
		arrival.setTime(productRow.getColumn("destTime").getStringColumn());
		arrival.setAirportName(productRow.getColumn("destname").getStringColumn());
		
		MarketingCarrierFlightType carrier = flightSegment.addNewMarketingCarrier();
		carrier.addNewAirlineID().setStringValue(airlinecd);
		String suffix = flightno.substring(flightno.length()-1, flightno.length());
		FlightNumber flightNumber = carrier.addNewFlightNumber();
		if(!org.apache.commons.lang.StringUtils.isNumeric(suffix)){
			flightNumber.setOperationalSuffix(suffix);
			flightNumber.setStringValue(flightno.substring(0, flightno.length()-1));
		}else{
			flightNumber.setOperationalSuffix("");
			flightNumber.setStringValue(flightno);
		}
//		flightSegment.addNewFlightDetail().setTourOperatorFlightID(flightid);
	} 
}
