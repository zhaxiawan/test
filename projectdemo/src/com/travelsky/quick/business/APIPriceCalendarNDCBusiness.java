package com.travelsky.quick.business;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
 * @author 作者:LiHz
 * @version 0.1 类说明: 7天日历运价接口 和 历史用价接口
 */
@Service("LCC_PRICECALENDAR_SERVICE")
public class APIPriceCalendarNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIPriceCalendarNDCBusiness.class);
	// 成人标记符
	private static final String ADT = "ADT";
	// 婴儿标记符
	private static final String INF = "INF";
	// 儿童标记符
	private static final String CHD = "CHD";
	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// 获取xml
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResponseBean());
		}
		// 请求 xm转换CommandData 异常
		catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_PRICE_CALENDAR,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		// 转换ResponseBean-->XmlBean
		return transRespBeanToXmlBean(commandRet, input);
	}

	// -----------------------------------------------------------------------------------

	/**
	 * 转换 xml-->Reqbean
	 * 
	 * @param context
	 *            shopping所用的一个集合
	 * @param xmlInput
	 *            前台获取的xml数据
	 * @throws APIException
	 *             APIException
	 * @throws Exception
	 *             Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		AirShoppingRQDocument rootDoc = null;
		rootDoc = AirShoppingRQDocument.Factory.parse(xmlInput);
		AirShoppingRQDocument.AirShoppingRQ reqdoc = rootDoc.getAirShoppingRQ();
		AirShopReqParamsType parameters = reqdoc.getParameters();
		CurrCodes currCodes = parameters == null ? null : parameters.getCurrCodes();
		CurrCode[] currCodeArr = currCodes == null ? null : currCodes.getCurrCodeArray();
		CurrCode currCodeEle = currCodeArr == null ? null : currCodeArr[0];
		String currCoce = currCodeEle == null ? null : currCodeEle.getStringValue();
		Traveler[] traveler = reqdoc.getTravelers().getTravelerArray();
		// 增加旅客人数和类型
		getPassengerNum(traveler,input);
		if (!StringUtils.hasLength(currCoce)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_CURRENCY, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_CURRENCY);

		}
		input.addParm("isoCode", currCoce);
		SimpleDateFormat adf = new SimpleDateFormat("yyyyMMdd");
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		/* String deptno = NdcXmlHelper.getDeptNo(reqdoc.getParty()); */
		input.addParm("tktdeptid", deptno);
		// 出发地三字码
		String depart = "";
		// 目的地三字码
		String arrive = "";
		OriginDestination[] originDestination = reqdoc.getCoreQuery().getOriginDestinations()
				.getOriginDestinationArray();
		if (null != originDestination && originDestination.length > 0) {
			// 出发地三字码
			depart = originDestination[0].getDeparture().getAirportCode().getStringValue();
			if (!StringUtils.hasLength(depart)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_ORG, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_ORG);
			}
			// 开始时间
			Date begindate = originDestination[0].getDeparture().getDate().getTime();
			input.addParm("begindate", adf.format(begindate));
			// 目的地三字码
			arrive = originDestination[0].getArrival().getAirportCode().getStringValue();
			if (!StringUtils.hasLength(arrive)) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_FLIGHT_DST, language));
				throw APIException.getInstance(ErrCodeConstants.API_NULL_FLIGHT_DST);
			}
			/*
			 * //结束时间 Date enddate =
			 * originDestination[0].getArrival().getDate().getTime();
			 * input.addParm("enddate", adf.format(enddate));
			 */
		} else {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_PRICE_CALENDAR, language));
			throw APIException.getInstance(ErrCodeConstants.API_UNKNOW_PRICE_CALENDAR);
		}
		//将rediskey放入input
		input.addParm("depart", depart);
		input.addParm("arrive", arrive);
		String redisKey = assemblyKey(input);
		input.addParm("redisKey", redisKey);
	}

	/**
	 * 获取旅客类型以及数量 放入全局Map变量中
	 * 
	 * @param traveler
	 */
	public void getPassengerNum(Traveler[] traveler,CommandData input) {
		for (Traveler travelerArray : traveler) {
			PTC ptc = travelerArray.getAnonymousTravelerArray(0).getPTC();
			// 人数
			int passengerNum = ptc.getQuantity().intValue();
			// 旅客类型
			String PassengerType = ptc.getStringValue();
			input.addParm(PassengerType, String.valueOf(passengerNum));
		}
	}

	/**
	 * 数据提交shopping后台
	 * 
	 * @param input
	 *            请求的XML参数
	 * @param context
	 *            用于调用doOther请求后台数据
	 * @return 请求后台返回的对象
	 */
	public CommandRet getResponseBean() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		ShoppingManager lmanager = new ShoppingManager();
		return lmanager.scheduleshoppingNDC(input, context);
	}

	/**
	 * 拼接B2C所用的XML
	 * 
	 * @param commandRet
	 *            后台返回的结果集
	 * @param input
	 *            B2C请求的XML
	 * @return 请求后台返回的对象
	 */
	public XmlObject transRespBeanToXmlBean(Object commandRet, CommandData input) {
		// 看是否有缓存数据，有就直接返回
		CommandRet xmlOutput = (CommandRet) commandRet;
		String redisValue = xmlOutput.getParm("LCC_PRICECALENDAR_SERVICE").getStringColumn();
		if (redisValue != null && !"".equals(redisValue)) {
			try {
				AirShoppingRSDocument document = AirShoppingRSDocument.Factory.parse(redisValue);
				return document;
			} catch (XmlException e) {
				e.printStackTrace();
			}
		}
		AirShoppingRSDocument doc = AirShoppingRSDocument.Factory.newInstance();
		AirShoppingRS rs = doc.addNewAirShoppingRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode, ApiServletHolder.getApiContext().getLanguage()));
			}
			// 反回正确的值
			else {
				rs.addNewDocument();
				rs.addNewSuccess();
				rs.addNewAirShoppingProcessing();
				OffersGroup offersGroup = rs.addNewOffersGroup();
				AirlineOffers airlineOffers = offersGroup.addNewAirlineOffers();
				Table ratesCalendar = xmlOutput.getParm("rates").getTableColumn();
				if (null != ratesCalendar) {
					for (int i = 0; i < ratesCalendar.getRowCount(); i++) {
						Row rates = ratesCalendar.getRow(i);
						PriceCalendar priceCalendar = airlineOffers.addNewPriceCalendar();
						// 日期
						String fligh = rates.getColumn("flightdate").getStringColumn();
						Calendar flightdate = DateUtils.getInstance().parseDate(fligh, "yyyyMMdd");
						priceCalendar.addNewPriceCalendarDate()
								.setStringValue(DateUtils.getInstance().formatDate(flightdate, "yyyy-MM-dd"));
						EncodedPriceType encodedPriceType = priceCalendar.addNewTotalPrice();
						// 币种
						/* encodedPriceType.setCode("CNY"); */
						// 价格 价格(-1表示没舱位)
						encodedPriceType.setStringValue(rates.getColumn("price").getStringColumn());
					}

				}
			}
		} catch (Exception e) {
			// 初始化XML节点
			doc = AirShoppingRSDocument.Factory.newInstance();
			rs = doc.addNewAirShoppingRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		// 添加到缓存
		String redisKey = input.getParm("redisKey").getStringColumn();
		RedisManager.getManager().set(redisKey, doc.toString(), 600);
		return doc;
	}
	private String assemblyKey(CommandData input) throws Exception {
		StringBuffer redisKey = new StringBuffer();
		String appID = ApiServletHolder.getApiContext().getAppID();
		String dateKye = input.getParm("begindate").getStringColumn();
		String depart = input.getParm("depart").getStringColumn();
		String arrive = input.getParm("arrive").getStringColumn();
		redisKey.append(RedisNamespaceEnum.api_service_pricecalendar.code()+":").append(depart+"-").append(arrive+"-").append(dateKye+"-");
		String adt = input.getParm(ADT).getStringColumn();
		redisKey.append(adt + "-");
		String chd = input.getParm(CHD).getStringColumn();
		redisKey.append(chd + "-");
		String inf = input.getParm(INF).getStringColumn();
		redisKey.append(inf + "-");
		String isoCode = input.getParm("isoCode").getStringColumn();
		redisKey.append(isoCode + "-");
		redisKey.append(appID);
		return redisKey.toString();
		
	}
}
