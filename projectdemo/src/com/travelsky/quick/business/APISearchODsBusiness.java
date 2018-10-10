package com.travelsky.quick.business;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCFlightAirportType;
import org.iata.iata.edist.LCCRouteListRQDocument;
import org.iata.iata.edist.LCCRouteListRQDocument.LCCRouteListRQ.Query.LCCRoute;
import org.iata.iata.edist.LCCRouteListRSDocument;
import org.iata.iata.edist.LCCRouteListRSDocument.LCCRouteListRS;
import org.iata.iata.edist.LCCRouteListRSDocument.LCCRouteListRS.Response.LCCRouteList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.CommonConstants;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;

import org.springframework.util.StringUtils;

import com.travelsky.quick.util.StatusUtil;
import com.travelsky.quick.util.helper.FlightManager;
import com.travelsky.quick.util.helper.TipMessager;
 
/**
 * 
 * @author MaRuifu 2016年5月3日下午3:16:53
 * @version 0.1
 * 类说明:航站对搜索接口
 */
@Service("LCC_SEARCHODS_SERVICE")
public class APISearchODsBusiness extends AbstractService<ApiContext> {

	private static final long serialVersionUID = 7570190778976028047L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(APISearchODsBusiness.class);
	private static final String  AIRPORTCODE = "airportcode";
	private static final String  CITYCODE = "citycode";
	private static final String  AIRPORTNAME = "airportname";
	private static final String  COUNTRY = "country";
	private static final String  CITY_NAME = "city_name";
	
	
	/**
	 * @param context SelvetContext
	 * @throws APIException APIRuntimeException xml异常
	 * @throws Exception Exception xml异常
	 * 
	 */
	@Override
	public void doServlet() throws  Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		//获取xml
		try{
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
					ErrCodeConstants.API_UNKNOW_SEARCHODS, 
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
		
	}


	
	
	/**
	 * 校验信息
	 * @param input CommandData
	 * @param context SelvetContext
	 * @return CommandRet
	 * 
	 */
	public CommandRet getResult(){
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		FlightManager manager = new FlightManager(); 
		return manager.odQuery(input, context);
	}
	/**
	 * 转换 xml-->Reqbean
	 * @param context SelvetContext
	 * @param xmlInput String
	 * @throws APIException APIException
	 * @throws Exception Exception 
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCRouteListRQDocument rootDoc = null;
		rootDoc = LCCRouteListRQDocument.Factory.parse(xmlInput);
		LCCRouteListRQDocument.LCCRouteListRQ reqDoc = rootDoc.getLCCRouteListRQ();
		//查询信息
		LCCRoute route = reqDoc.getQuery().getLCCRoute();
		//航站IATA 三字码
		String airportCode = route.getAirportCode()==null?null:
			route.getAirportCode().getStringValue();
		//获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		if(!StringUtils.hasLength(airportCode)){
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_NULL_SEARCHODS_AIRPORTCODE, language));
				throw APIException.getInstance(
				ErrCodeConstants.API_NULL_SEARCHODS_AIRPORTCODE);
		}
		input.addParm("airportCode", airportCode);		
	}
	/**
	 * @param xmlOutput CommandRet
	 * @param input CommandData
	 * @return XmlObject
	 */
	public XmlObject transResponseBeanToXmlBean(
			CommandRet xmlOutput ,CommandData input) {
		//定义一个boolean值，用来不判断返回数据中是否存在错误code
		boolean existError=false;
		//redis中取值放入返回xml中
		String redisValue = xmlOutput.getParm("LCC_SEARCHODS_SERVICE").getStringColumn();
		if (redisValue!=null&&!"".equals(redisValue)) {
			try {
				LCCRouteListRSDocument document = LCCRouteListRSDocument.Factory.parse(redisValue);
				return document;
			} catch (XmlException e) {
				e.printStackTrace();
			}
		}
		LCCRouteListRSDocument sadoc = LCCRouteListRSDocument.Factory.newInstance();
		LCCRouteListRS rprs = sadoc.addNewLCCRouteListRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if(StringUtils.hasLength(errorcode)){
				ErrorType error = rprs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode,
						ApiServletHolder.getApiContext().getLanguage()));
				existError=true;
			}else{
				rprs.addNewSuccess();
				LCCRouteList lccRouteList = rprs.addNewResponse().addNewLCCRouteList();
				Table tableColumn = xmlOutput.getParm("airportod").getTableColumn();
				if(tableColumn!=null){
					getMet(lccRouteList, tableColumn);
					
				}
			}
		} 
		catch (Exception e) {
			LOGGER.error(ErrCodeConstants.API_SYSTEM, e);
			sadoc = LCCRouteListRSDocument.Factory.newInstance();   
			rprs = sadoc.addNewLCCRouteListRS();                   
			// 存在错误信息
			ErrorType error = rprs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
			existError=true;
		}
		if (!existError) {
			//返回结果中不存在错误code，正常缓存
			RedisManager.getManager().set(RedisNamespaceEnum.api_service_route.toKey(ApiServletHolder.getApiContext().getTicketDeptid()+ApiServletHolder.getApiContext().getLanguage()), sadoc.toString(), 600);
		}
		return sadoc;
	}



	/**
	 * 
	 * @param lccRouteList LCCRouteList
	 * @param tableColumn Table
	 */
	private void getMet(LCCRouteList lccRouteList, Table tableColumn) {
		String language = ApiServletHolder.getApiContext().getLanguage();
		for (int i = 0; i < tableColumn.getRowCount(); i++) {
			Row airportPairsrow = tableColumn.getRow(i);
			org.iata.iata.edist.LCCRouteListRSDocument.
			LCCRouteListRS.Response.LCCRouteList.
			LCCRoute lccRoute = lccRouteList.addNewLCCRoute();
			CommandData originObjColumn =
				airportPairsrow.getColumn("ori").getObjectColumn();
			if(originObjColumn!=null){
				LCCFlightAirportType departure = lccRoute.addNewDeparture();
				departure.addNewAirportCode().setStringValue(
						originObjColumn.getParm(AIRPORTCODE)
						.getStringColumn());
				departure.addNewCityCode().setStringValue(
						originObjColumn.getParm(CITYCODE)
						.getStringColumn());
				departure.setAirportName(StatusUtil.getLanguageName(originObjColumn.getParm(AIRPORTNAME)
						.getObjectColumn(), language));
				departure.setCityName(StatusUtil.getLanguageName(originObjColumn.getParm(CITY_NAME)
						.getObjectColumn(), language));
				departure.setAirportNameSpell(StatusUtil.getLanguageName(originObjColumn.getParm(AIRPORTNAME)
						.getObjectColumn(), "en_US"));
				departure.setCityNameSpell(StatusUtil.getLanguageName(originObjColumn.getParm(CITY_NAME)
						.getObjectColumn(), "en_US"));
				departure.addNewCountryCode().setStringValue(
						originObjColumn.getParm(COUNTRY)
						.getStringColumn());
			}
		  CommandData destObjColumn = airportPairsrow
			.getColumn("dest").getObjectColumn();
		  if(destObjColumn!=null){
			LCCFlightAirportType arrival = lccRoute.addNewArrival();
			arrival.addNewAirportCode().setStringValue(
					destObjColumn.getParm(AIRPORTCODE)
					.getStringColumn());
			arrival.addNewCityCode().setStringValue(
					destObjColumn.getParm(CITYCODE)
					.getStringColumn());
			arrival.setAirportName(StatusUtil.getLanguageName(destObjColumn.getParm(AIRPORTNAME)
					.getObjectColumn(), language));
			arrival.setCityName(StatusUtil.getLanguageName(destObjColumn.getParm(CITY_NAME)
					.getObjectColumn(), language));
			arrival.setAirportNameSpell(StatusUtil.getLanguageName(destObjColumn.getParm(AIRPORTNAME)
					.getObjectColumn(), "en_US"));
			arrival.setCityNameSpell(StatusUtil.getLanguageName(destObjColumn.getParm(CITY_NAME)
					.getObjectColumn(), "en_US"));
			arrival.addNewCountryCode().setStringValue(
					destObjColumn.getParm(COUNTRY)
					.getStringColumn());
		 }
		}
	}
}
