package com.travelsky.quick.business;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirShopReqAttributeQueryType.OriginDestination;
import org.iata.iata.edist.AirShopReqParamsType;
import org.iata.iata.edist.AirShoppingRQDocument;
import org.iata.iata.edist.AirShoppingRSDocument;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS.OffersGroup.AirlineOffers.PriceCalendar;
import org.iata.iata.edist.CurrCodeDocument.CurrCode;
import org.iata.iata.edist.MessageParamsBaseType.CurrCodes;
import org.iata.iata.edist.TravelerCoreType.PTC;
import org.iata.iata.edist.TravelersDocument.Travelers.Traveler;
import org.iata.iata.edist.EncodedPriceType;
import org.iata.iata.edist.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.helper.ShoppingManager;
import com.travelsky.quick.util.helper.TipMessager;

/** 
 * @author 作者:zxw
 * @version 0.1
 * 类说明:
 *		30天内有效航班班期查询
 */
@Service("LCC_THIRTYCALENDAR_SERVICE")
public class APIThirtyCalendarNDCBusiness extends AbstractService<ApiContext>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIThirtyCalendarNDCBusiness.class);
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
						ErrCodeConstants.API_UNKNOW_PRICE_CALENDAR, 
						ApiServletHolder.getApiContext().getLanguage()), e);
				throw e;
			}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet,
			CommandData input) {
		//看是否有缓存数据，有就直接返回
		CommandRet xmlOutput = (CommandRet)commandRet;
		String redisValue = xmlOutput.getParm("LCC_THIRTYCALENDAR_SERVICE").getStringColumn();
		if (redisValue!=null&&!"".equals(redisValue)) {
			try {
				AirShoppingRSDocument document = AirShoppingRSDocument.Factory.parse(redisValue);
				return document;
			} catch (XmlException e) {
				e.printStackTrace();
			}
		}
		//转换ResponseBean-->XmlBean
		return transRespBeanToXmlBean(commandRet,input);
	}

	
	
	
//-----------------------------------------------------------------------------------
	
	/**
	 * 转换 xml-->Reqbean
	 * @param context shopping所用的一个集合
	 * @param xmlInput 前台获取的xml数据
	 * @throws APIException APIException
	 * @throws Exception Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		AirShoppingRQDocument rootDoc = null;
		rootDoc = AirShoppingRQDocument.Factory.parse(xmlInput);
		AirShoppingRQDocument.AirShoppingRQ reqdoc = rootDoc.getAirShoppingRQ();
		SimpleDateFormat adf = new SimpleDateFormat("yyyy-MM-dd");
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		/*String deptno = NdcXmlHelper.getDeptNo(reqdoc.getParty());*/
			input.addParm("tktdeptid",deptno);
		//出发地三字码
		String depart = "";
		//目的地三字码
		String arrive = ""; 
		OriginDestination[] originDestination = reqdoc.getCoreQuery().
				getOriginDestinations().getOriginDestinationArray();
		if(null != originDestination  && originDestination.length>0){
			//出发地三字码 
			depart = originDestination[0].getDeparture().
					getAirportCode().getStringValue();
			if(!StringUtils.hasLength(depart)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_FLIGHT_ORG, language));
				 throw APIException.getInstance(
						 ErrCodeConstants.API_NULL_FLIGHT_ORG);
			 }	
			//开始时间  
			 Date begindate = null;
			if (originDestination[0]!=null&&originDestination[0].getDeparture()!=null&&originDestination[0].getDeparture().getDate()!=null) {
				begindate = originDestination[0].getDeparture().getDate().getTime();
			}
			input.addParm("begindate", adf.format(begindate));
			//目的地三字码
			arrive = originDestination[0].getArrival().
					getAirportCode().getStringValue();
			if(!StringUtils.hasLength(arrive)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_FLIGHT_DST, language));
				 throw APIException.getInstance(
						 ErrCodeConstants.API_NULL_FLIGHT_DST);
			 }	
		}else{
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNOW_PRICE_CALENDAR, language));
			 throw APIException.getInstance(
					 ErrCodeConstants.API_UNKNOW_PRICE_CALENDAR);
		}
		input.addParm("depart", depart);
		input.addParm("arrive", arrive);
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
		ShoppingManager lmanager = new ShoppingManager();
		return lmanager.thirtyShoppingNDC(input,context);
	}
	
	/**
	 * 拼接B2C所用的XML
	 * @param commandRet 后台返回的结果集
	 * @param input  B2C请求的XML
	 * @return  请求后台返回的对象
	 */
	public XmlObject transRespBeanToXmlBean(Object commandRet ,CommandData input) {
		CommandRet xmlOutput = (CommandRet)commandRet;
		AirShoppingRSDocument doc = AirShoppingRSDocument.Factory.newInstance();
		AirShoppingRS rs = doc.addNewAirShoppingRS();
		try{
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
			}
			//返回正确的值
			else{
				rs.addNewDocument();
				rs.addNewSuccess();
				rs.addNewAirShoppingProcessing();
				OffersGroup offersGroup = rs.addNewOffersGroup();
				AirlineOffers airlineOffers = offersGroup.addNewAirlineOffers();
				CommandData thirtyData = xmlOutput.getParm("odData").getObjectColumn();
				Set<String> itemList =null;
				if (thirtyData!=null) {
					itemList = thirtyData.getItemList();
				}
				//排序
				TreeMap<Integer, String> treeMap=new TreeMap<>();
				if(null != thirtyData&&itemList!=null){
					for (String string : itemList) {
						 Calendar flightdate = DateUtils.getInstance().parseDate(string, "yyyy-MM-dd");
						 String date = DateUtils.getInstance().formatDate(flightdate, "yyyyMMdd");
						 
						treeMap.put(Integer.parseInt(date), string);
					}
				}
				for (Entry<Integer, String> string : treeMap.entrySet()) {
						PriceCalendar  priceCalendar =	airlineOffers.addNewPriceCalendar();
						 //日期
						 Calendar flightdate = DateUtils.getInstance()
								 .parseDate(string.getKey().toString(), "yyyyMMdd");
						 priceCalendar.addNewPriceCalendarDate().
						 	setStringValue(DateUtils.getInstance().
						 			formatDate(flightdate, "yyyy-MM-dd"));
					}
			}
		}
		catch (Exception e) {
			//初始化XML节点
			doc = AirShoppingRSDocument.Factory.newInstance();
			rs = doc.addNewAirShoppingRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		//拼接key
		String depart = input.getParm("depart").getStringColumn();
		String arrive = input.getParm("arrive").getStringColumn();
		String begindate = input.getParm("begindate").getStringColumn().substring(0,7);
		String redisKey=RedisNamespaceEnum.api_service_schedule.code()+":"+depart+"-"+arrive+"-"+begindate;
		//添加到缓存
		RedisManager.getManager().set(redisKey, doc.toString(), 600);
		return doc;
	}
	
	
}
