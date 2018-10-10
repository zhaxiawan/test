package com.travelsky.quick.business;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirShopFlightSpecificType;
import org.iata.iata.edist.AirShopReqAttributeQueryType;
import org.iata.iata.edist.AirShopReqParamsType;
import org.iata.iata.edist.AirShopReqParamsType.Inventory;
import org.iata.iata.edist.AirShoppingRQDocument;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ;
import org.iata.iata.edist.AirShoppingRQDocument.AirShoppingRQ.CoreQuery;
import org.iata.iata.edist.AirShoppingRSDocument;
import org.iata.iata.edist.AirShoppingRSDocument.AirShoppingRS;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.MessageParamsBaseType.CurrCodes;
import org.iata.iata.edist.OrderViewRSDocument.OrderViewRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
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
import com.travelsky.quick.framework.util.MergeableCallWorker;
import com.travelsky.quick.service.AbstractService;
import com.travelsky.quick.util.helper.APIAirShoppingNDCB2C;
import com.travelsky.quick.util.helper.APIAirShoppingNDCONEE;
import com.travelsky.quick.util.helper.ShoppingManager;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 品牌查询
 * 
 * @author lizhi
 *
 */
@Service("LCC_AIRSHOPPING_SERVICE")
public class APIAirShoppingNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(APIAirShoppingNDCBusiness.class);
	// 去程标记符
	private static final String OW = "OW";
	// 回程标记符
	private static final String RT = "RT";
	private static final String DA = "DA";
	private static final long serialVersionUID = 1L;
	private static final String B2C = "B2C";
	private static final String SEAMLESS_TRUE = "seamlesstrue";
	private static final String SEAMLESS_FALSE = "seamlessfalse";
	public static final String REG_ENGLISH = "[a-zA-Z]{1,20}";
	// 用于存放APIAirShoppingNDCB2C对象，如果该对象是全局数据的话，在多线程访问该单例接口时，会出现此对象中的数据错乱。
	private Map<String, APIAirShoppingNDCB2C> shoppingB2CMap = new HashMap<String, APIAirShoppingNDCB2C>();
	private Map<String, APIAirShoppingNDCONEE> shoppingONEEMap = new HashMap<String, APIAirShoppingNDCONEE>();

	@Override
	protected void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		AirShoppingRQDocument rootDoc = AirShoppingRQDocument.Factory.parse(xmlInput);
		AirShoppingRQ shoppingRQ = rootDoc.getAirShoppingRQ();
		AirShopReqParamsType parameters = shoppingRQ.getParameters();
		CoreQuery coreQueryArry = shoppingRQ.getCoreQuery();
		CurrCodes currCodes = parameters == null ? null : parameters.getCurrCodes();
		Inventory InventoryArry = parameters == null ? null : parameters.getInventory();
		AirShopFlightSpecificType flightSpecific = coreQueryArry == null ? null : coreQueryArry.getFlightSpecific();
		AirShopReqAttributeQueryType originDestinations = coreQueryArry == null ? null
				: coreQueryArry.getOriginDestinations();
		final ShoppingManager lManager = new ShoppingManager();
		CommandData input = context.getInput();
		String agree;
		if (null != currCodes) {
			final CommandRet ret = new CommandRet("");
			// B2C请求
			agree = "B2C";
			input.addParm("agree", agree);
			APIAirShoppingNDCB2C shoppingB2C = new APIAirShoppingNDCB2C();
			shoppingB2CMap.put("shopping" + context.getTransactions(), shoppingB2C);
			shoppingB2C.doServletB2C(context);
			// 组装rediskey
			final String redisKey = assemblyKey();
			// 检查组装之前key值中未校验的数据
			checkRedisKey(redisKey);
			input.addParm("redisKey", redisKey);
			// 将redisKey放入input中
			if (input.getParm(OW).getObjectColumn() != null) {
				CommandRet retOW = lManager.shopping(input.getParm(OW).getObjectColumn(),
						context);
				ret.addParm(OW, retOW);
			}
			// 回程
			if (input.getParm(RT).getObjectColumn() != null) {
				CommandRet retRT = lManager.shopping(input.getParm(RT).getObjectColumn(),
						context);
				ret.addParm(RT, retRT);
			}
			context.setRet(ret);
			// 获取ResponseBean
			// 去程
			/*
			 * if (input.getParm(OW).getObjectColumn() != null) { CommandRet
			 * retOW = lManager.shopping(input.getParm(OW).getObjectColumn(),
			 * context); ret.addParm(OW, retOW); } // 回程 if
			 * (input.getParm(RT).getObjectColumn() != null) { CommandRet retRT
			 * = lManager.shopping(input.getParm(RT).getObjectColumn(),
			 * context); ret.addParm(RT, retRT); }
			 */
		}
		if (null != InventoryArry) {
			CommandRet ret=null;
			if (null != originDestinations) {
				ret = new CommandRet("");
				// DA: 只是AV查询 false
				agree = SEAMLESS_FALSE;
				input.addParm("agree", agree);
				APIAirShoppingNDCONEE shoppingONEE = new APIAirShoppingNDCONEE();
				shoppingONEEMap.put("shoppingONEE" + context.getTransactions(), shoppingONEE);
				shoppingONEE.doServletDA(context);
				// 获取ResponseBean
				CommandData free = input.getParm("DA").getObjectColumn();
				CommandData inputRQ = new CommandData();
				// 航空公司二字码
				inputRQ.addParm("airlineCode", free.getParm("code").getStringColumn());
				// 品牌编号
				inputRQ.addParm("flightno", free.getParm("flightno").getStringColumn());
				// 航班后缀
				inputRQ.addParm("suffix", free.getParm("suffix").getStringColumn());
				// 出发时间
				inputRQ.addParm("deptdate", free.getParm("deptdate").getStringColumn());
				// 出发地
				inputRQ.addParm("depart", free.getParm("depart").getStringColumn());
				// 目的地
				inputRQ.addParm("arrive", free.getParm("arrive").getStringColumn());
				// 舱位等级
				inputRQ.addParm("cabin", free.getParm("cabinType").getStringColumn());
				inputRQ.addParm("adt", free.getParm("adt").getStringColumn());
				inputRQ.addParm("chd", free.getParm("chd").getStringColumn());
				inputRQ.addParm("inf", free.getParm("inf").getStringColumn());
				inputRQ.addParm("isoCode", free.getParm("isoCode").getStringColumn());
				inputRQ.addParm("segment", free.getParm("segment").getStringColumn());
				inputRQ.addParm("cabinSaleNum", free.getParm("guaranteeNum").getStringColumn());
				// 是否只查直达(0表示查所有，1 表示仅查询直达)
				inputRQ.addParm("direct", free.getParm("direct").getStringColumn());
				// 是否只查经停(0表示不包含经停，1 表示包含经停)
				inputRQ.addParm("nonStop", free.getParm("nonStop").getStringColumn());
				// 中转点（机场三字码）（只有connectionNum的值是1时，选填）
				inputRQ.addParm("connectionCode", free.getParm("connection").getStringColumn());
				// 中转数量
				inputRQ.addParm("connectionNum", free.getParm("maxNumber").getStringColumn());

				CommandRet retRS = lManager.shoppingForOneE(inputRQ, context);
				ret.addParm("DA", retRS);
				context.setRet(ret);
			}
			if (null != flightSpecific) {
				if (InventoryArry.getGuaranteeInd()) {
					ret = new CommandRet("");
					// Selling: AV查询(指定航班并占库存) true
					agree = SEAMLESS_TRUE;
					input.addParm("agree", agree);
					APIAirShoppingNDCONEE shoppingONEE = new APIAirShoppingNDCONEE();
					shoppingONEEMap.put("shoppingONEE" + context.getTransactions(), shoppingONEE);
					shoppingONEE.doServletDA(context);
					// 获取ResponseBean
					Table freeSTable = input.getParm(DA).getTableColumn();
					int i = 0;
					for (Row free : freeSTable) {
						CommandData inputRQ = new CommandData();
						// 出发时间
						inputRQ.addParm("departdate", free.getColumn("deptdate").getStringColumn());
						// 出发地
						inputRQ.addParm("depart", free.getColumn("depart").getStringColumn());
						// 目的地
						inputRQ.addParm("arrive", free.getColumn("arrive").getStringColumn());
						// 到达时间
						inputRQ.addParm("destdate", free.getColumn("destdate").getStringColumn());
						// 航空公司二字码
						inputRQ.addParm("airlineCode", free.getColumn("code").getStringColumn());
						// 品牌编号
						inputRQ.addParm("flightNo", free.getColumn("flightno").getStringColumn());
						// 舱位
						inputRQ.addParm("cabinClass", free.getColumn("cabin").getStringColumn());
						// 航班后缀
						inputRQ.addParm("suffix", free.getColumn("suffix").getStringColumn());
						inputRQ.addParm("adt", free.getColumn("adt").getStringColumn());
						inputRQ.addParm("chd", free.getColumn("chd").getStringColumn());
						inputRQ.addParm("inf", free.getColumn("inf").getStringColumn());
						inputRQ.addParm("isoCode", free.getColumn("isoCode").getStringColumn());
						inputRQ.addParm("segment", free.getColumn("segment").getStringColumn());
						inputRQ.addParm("tktdeptid", free.getColumn("tktdeptid").getStringColumn());
						inputRQ.addParm("guaranteeNum", free.getColumn("guaranteeNum").getStringColumn());
						CommandRet retRS = lManager.shoppingONEE(inputRQ, context);
						if (retRS.isError()) {
							retRS.addParm("free", inputRQ);
							i++;
						}
						ret.addParm(free.getColumn("segment").getStringColumn(), retRS);
						//此处 i 判断 retRS 中返回是否有错误
						ret.addParm("int", i);
						context.setRet(ret);
					}

				} else {
					ret = new CommandRet("");
					// Seamless:AV查询(指定航班) false
					agree = SEAMLESS_FALSE;
					input.addParm("agree", agree);
					APIAirShoppingNDCONEE shoppingONEE = new APIAirShoppingNDCONEE();
					shoppingONEEMap.put("shoppingONEE" + context.getTransactions(), shoppingONEE);
					shoppingONEE.doServletDA(context);
					// 获取ResponseBean
					Table freeSTable = input.getParm("DA").getTableColumn();
					for (Row free : freeSTable) {
						CommandData inputRQ = new CommandData();
						// 出发时间
						inputRQ.addParm("deptdate", free.getColumn("deptdate").getStringColumn());
						// 出发地
						inputRQ.addParm("depart", free.getColumn("depart").getStringColumn());
						// 目的地
						inputRQ.addParm("arrive", free.getColumn("arrive").getStringColumn());
						// 航空公司二字码
						inputRQ.addParm("airlineCode", free.getColumn("code").getStringColumn());
						// 品牌编号
						inputRQ.addParm("flightno", free.getColumn("flightno").getStringColumn());
						// 舱位
						inputRQ.addParm("cabinClass", free.getColumn("cabin").getStringColumn());
						// 航班后缀
						inputRQ.addParm("suffix", free.getColumn("suffix").getStringColumn());
						inputRQ.addParm("adt", free.getColumn("adt").getStringColumn());
						inputRQ.addParm("chd", free.getColumn("chd").getStringColumn());
						inputRQ.addParm("inf", free.getColumn("inf").getStringColumn());
						inputRQ.addParm("isoCode", free.getColumn("isoCode").getStringColumn());
						inputRQ.addParm("segment", free.getColumn("segment").getStringColumn());
						inputRQ.addParm("tktdeptid", free.getColumn("tktdeptid").getStringColumn());
						inputRQ.addParm("cabinSaleNum", free.getColumn("guaranteeNum").getStringColumn());
						CommandRet retRS = lManager.shoppingForOneE(inputRQ, context);
						ret.addParm(free.getColumn("segment").getStringColumn(), retRS);
						//判断 是否占库存 0 不占 1占
						ret.addParm("int", "0");
						context.setRet(ret);
					}
				}
			}
		}

	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		String agree = input.getParm("agree").getStringColumn();
		AirShoppingRSDocument docOW = AirShoppingRSDocument.Factory.newInstance();
		AirShoppingRS rsOW = docOW.addNewAirShoppingRS();
		if (B2C.equals(agree)) {
			
			// 从shoppingB2CMap获取对应的APIAirShoppingNDCB2C对象，因为在执行doServletB2C（）方法时，对此对象中的一些数据初始化了。
			String shoppingKey = "shopping" + ApiServletHolder.get().getTransactions();
			APIAirShoppingNDCB2C shoppingB2C = shoppingB2CMap.get(shoppingKey);
			if (shoppingB2C != null) {
				shoppingB2CMap.remove(shoppingB2C);
			}
			return shoppingB2C.transRespBeanToXmlBeanB2C(commandRet, input);
		}
		if (SEAMLESS_TRUE.equals(agree)) {
			String shoppingKey = "shoppingONEE" + ApiServletHolder.get().getTransactions();
			  APIAirShoppingNDCONEE shoppingONEE = shoppingONEEMap.get(shoppingKey);
			if (shoppingONEEMap != null && !processError(commandRet,rsOW)) {
				shoppingONEEMap.remove(shoppingONEE);
			}else{
				return docOW;
			}
			return shoppingONEE.transRespBeanToXmlBeanONEE(commandRet, input, true);
		}
		if (SEAMLESS_FALSE.equals(agree)) {
			String shoppingKey = "shoppingONEE" + ApiServletHolder.get().getTransactions();
			APIAirShoppingNDCONEE shoppingONEE = shoppingONEEMap.get(shoppingKey);
			if (shoppingONEEMap != null && !processError(commandRet,rsOW)) {
				shoppingONEEMap.remove(shoppingONEE);
			}else{
				return docOW;
			}
			return shoppingONEE.transRespBeanToXmlBeanONEE(commandRet, input, false);
		}
		
		if(!processError(commandRet,rsOW)){
			ErrorType error = rsOW.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_UNKNOW_SHOPPING));
			error.setShortText(TipMessager.getMessage(ErrCodeConstants.API_UNKNOW_SHOPPING,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return docOW;
	}
	/**
	 * 处理错误,如果包括错误,返回true,否则返回false
	 * @param ret
	 * @param root
	 * @return
	 */
	private boolean processError(CommandRet ret, AirShoppingRS root) {
		// 判断是否存在错误信息
		String errCode = ret.getErrorCode();
		// 存在错误信息
		if (StringUtils.hasLength(errCode) && errCode.contains("LCC")) {
			ErrorType error = root.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(errCode));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(errCode,
					ApiServletHolder.getApiContext().getLanguage()));
			return true;
		}
		return false;
	}
	private String assemblyKey() throws Exception {
		StringBuffer redisKey = new StringBuffer();
		String appID = ApiServletHolder.getApiContext().getAppID();
		String roundTripIdentification = "";
		String deptdate = "";
		String deptdateGo = "";
		String depart = "";
		String arrive = "";
		String adt = "";
		String chd = "";
		String inf = "";
		String isoCode = "";
		CommandData data = null;
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		// B2C:service:av-PEK-SHA-O-20180401-1-0-0-USD-appid
		redisKey.append(RedisNamespaceEnum.api_service_av.code()+":");
		if (input.getParm(RT).getObjectColumn() != null) {
			data = input.getParm(RT).getObjectColumn();
			roundTripIdentification = "W";
			// 返程： 出发地和目的地交换位置
			arrive = data.getParm("arrive").getStringColumn();
			redisKey.append(arrive + "-");
			depart = data.getParm("depart").getStringColumn();
			redisKey.append(depart + "-" + roundTripIdentification + "-");
			// 加上去程的时间
			deptdateGo = input.getParm(OW).getObjectColumn().getParm("deptdate").getStringColumn();
			redisKey.append(deptdateGo + "-");
			deptdate = data.getParm("deptdate").getStringColumn();
			redisKey.append(deptdate + "-");
			adt = data.getParm("adt").getStringColumn();
			redisKey.append(adt + "-");
			chd = data.getParm("chd").getStringColumn();
			redisKey.append(chd + "-");
			inf = data.getParm("inf").getStringColumn();
			redisKey.append(inf + "-");
		} else if (input.getParm(OW).getObjectColumn() != null) {
			data = input.getParm(OW).getObjectColumn();
			roundTripIdentification = "O";
			depart = data.getParm("depart").getStringColumn();
			redisKey.append(depart + "-");
			arrive = data.getParm("arrive").getStringColumn();
			redisKey.append(arrive + "-" + roundTripIdentification + "-");
			deptdate = data.getParm("deptdate").getStringColumn();
			redisKey.append(deptdate + "-");
			adt = data.getParm("adt").getStringColumn();
			redisKey.append(adt + "-");
			chd = data.getParm("chd").getStringColumn();
			redisKey.append(chd + "-");
			inf = data.getParm("inf").getStringColumn();
			redisKey.append(inf + "-");
		}
		isoCode = input.getParm("isoCode").getStringColumn();
		redisKey.append(isoCode + "-");
		redisKey.append(appID);
		return redisKey.toString();
	}

	private void checkRedisKey(String redisKey) throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		// B2C:service:av:PEK-SHA-O-20180401-1-0-0-appid
		String[] reidsKeyArr = redisKey.split("-");
		//语言：为何不设置成全局，因为一旦设置 ，这个数据的地址值永远指向第一次设置那次，也就是说后续的访问用的都是第一次的语言 
		String language = ApiServletHolder.getApiContext().getLanguage();
		// 组装redisKey之前只有以下数据未校验
		if (reidsKeyArr[0].split(":")[3].length() != 3 || !Pattern.matches(REG_ENGLISH, reidsKeyArr[0].split(":")[3])) {
			// 出发地不是三字码！
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_ORICODE_ERROR, language));
			throw APIException.getInstance(ErrCodeConstants.API_ORICODE_ERROR);
		}
		if (reidsKeyArr[1].length() != 3 || !Pattern.matches(REG_ENGLISH, reidsKeyArr[1])) {
			// 目的地不是三字码
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_DESTCODE_ERROR, language));
			throw APIException.getInstance(ErrCodeConstants.API_DESTCODE_ERROR);
		}
		int yearGo = Integer.parseInt(reidsKeyArr[3].substring(0, 4));
		if (yearGo > 2100 || yearGo < 2018) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_ORIDATE_ERROR, language));
			throw APIException.getInstance(ErrCodeConstants.API_ORIDATE_ERROR);
		}
		if ("W".equals(reidsKeyArr[2])) {
			int yearReturn = Integer.parseInt(reidsKeyArr[4].substring(0, 4));
			if (yearReturn > 2100 || yearReturn < 2018) {
				LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_DESTDATE_ERROR, language));
				throw APIException.getInstance(ErrCodeConstants.API_DESTDATE_ERROR);
			}
		}
	}
}