package com.travelsky.quick.business;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.iata.iata.edist.AirShoppingRSDocument;
import org.iata.iata.edist.ErrorType;
import org.iata.iata.edist.LCCDictionaryReadRQDocument;
import org.iata.iata.edist.LCCDictionaryReadRSDocument;
import org.iata.iata.edist.LCCDictionaryReadRSDocument.LCCDictionaryReadRS;
import org.iata.iata.edist.LCCDictionaryReadRSDocument.LCCDictionaryReadRS.Response.LCCDictionaryInfos;
import org.iata.iata.edist.LCCDictionaryReadRSDocument.LCCDictionaryReadRS.Response.LCCDictionaryInfos.LCCDictionaryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.service.AbstractService;

import org.springframework.util.StringUtils;

import com.travelsky.quick.util.StatusUtil;
import com.travelsky.quick.util.helper.CurrencyManager;
import com.travelsky.quick.util.helper.DictManager;
import com.travelsky.quick.util.helper.NdcXmlHelper;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * @author 作者:LiHz
 * @version 0.1 类说明: 字典查询
 *
 */
@Service("LCC_DICTIONARYQUERY_SERVICE")
public class APIDictionaryQueryNDCBusiness extends AbstractService<ApiContext> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8421607997880171860L;
	private static final Logger LOGGER = LoggerFactory.getLogger(APIDictionaryQueryNDCBusiness.class);
	@Override
	public void doServlet() throws Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		try {
			// 转换 xml-->Reqbean
			transInputXmlToRequestBean();
			// 获取ResponseBean
			context.setRet(getResponseBean());
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(ErrCodeConstants.API_UNKNOW_DICTIONARY_QUERY,
					ApiServletHolder.getApiContext().getLanguage()), e);
			throw e;
		}
	}

	@Override
	public XmlObject transResponseBeanToXmlBean(CommandRet commandRet, CommandData input) {
		return transRespBeanToXmlBean(commandRet, input);
	}

	/**
	 * 转换 xml-->Reqbean
	 * 
	 * @param context
	 *            请求shopping所用的一个集合
	 * @param xmlInput
	 *            前台获取的xml数据
	 * @throws APIException
	 *             APIException
	 * @throws Exception
	 *             Exception
	 */
	public void transInputXmlToRequestBean() throws APIException, Exception {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		String xmlInput = context.getContext().getReqXML();
		CommandData input = context.getInput();
		LCCDictionaryReadRQDocument rootDoc = null;
		rootDoc = LCCDictionaryReadRQDocument.Factory.parse(xmlInput);
		LCCDictionaryReadRQDocument.LCCDictionaryReadRQ reqdoc = rootDoc.getLCCDictionaryReadRQ();
		// 部门ID
		String deptno = ApiServletHolder.getApiContext().getTicketDeptid();
		input.addParm("tktdeptid", deptno);
		// 获取语言
		String language = ApiServletHolder.getApiContext().getLanguage();
		// 字典类别代码
		String typecode = reqdoc.getQuery().getType();
		if (!StringUtils.hasLength(typecode)) {
			LOGGER.info(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_DICT_TYPECODE, language));
			throw APIException.getInstance(ErrCodeConstants.API_NULL_DICT_TYPECODE);
		}
		input.addParm("memberid", context.getContext().getUserID());
		input.addParm("typecode", typecode);
		input.addParm("language", language);

	}

	/**
	 * 数据提交shopping后台
	 * 
	 * @param input
	 *            请求的XML
	 * @param context
	 *            请求参数
	 * @return 返回请求数据
	 */
	public CommandRet getResponseBean() {
		SelvetContext<ApiContext> context = ApiServletHolder.get();
		CommandData input = context.getInput();
		if ("CURRENCYTYPE".equals(input.getParm("typecode").getStringColumn())) {
			// 获取币种列表
			CurrencyManager currencyManager = new CurrencyManager();
			return currencyManager.getCurrencyList(input, context);
		}
		DictManager dictManager = new DictManager();
		return dictManager.getDictNdc(input, context);
	}

	/**
	 * 转换ResponseBean-->XmlBean
	 * 
	 * @param commandRet
	 *            请求数据
	 * @param input
	 *            XML
	 * @return 返回XmlBean数据
	 */
	public XmlObject transRespBeanToXmlBean(Object commandRet, CommandData input) {
		// 看是否有缓存数据，有就直接返回
		CommandRet xmlOutput = (CommandRet) commandRet;
		String typecode = input.getParm("typecode").getStringColumn();
		String redisValue = xmlOutput.getParm("LCC_DICTIONARYQUERY_SERVICE_"+typecode).getStringColumn();
		if (redisValue != null && !"".equals(redisValue)) {
			try {
				LCCDictionaryReadRSDocument document = LCCDictionaryReadRSDocument.Factory.parse(redisValue);
				return document;
			} catch (XmlException e) {
				e.printStackTrace();
			}
		}
		LCCDictionaryReadRSDocument doc = LCCDictionaryReadRSDocument.Factory.newInstance();
		LCCDictionaryReadRS rs = doc.addNewLCCDictionaryReadRS();
		try {
			String errorcode = xmlOutput.getErrorCode();
			if (StringUtils.hasLength(errorcode)) {
				ErrorType error = rs.addNewErrors().addNewError();
				error.setCode(TipMessager.getErrorCode(errorcode));
				error.setStringValue(TipMessager.getMessage(errorcode, ApiServletHolder.getApiContext().getLanguage()));
			}
			// 反回无吴
			else {
				if ("CURRENCYTYPE".equalsIgnoreCase(typecode)) {
					/*** 获取货币列表接口 ***/
					rs.addNewSuccess();
					LCCDictionaryInfos infos = rs.addNewResponse().addNewLCCDictionaryInfos();
					Table currencyInfoTab = xmlOutput.getParm("currencyInfoQuery").getTableColumn();
					if (null != currencyInfoTab && currencyInfoTab.getRowCount() > 0) {
						for (int i = 0; i < currencyInfoTab.getRowCount(); i++) {
							Row dict = currencyInfoTab.getRow(i);
							LCCDictionaryInfo dictionary = infos.addNewLCCDictionaryInfo();
							// 字典代码
							dictionary.setCode(dict.getColumn("currencyCode").getStringColumn());
							// 字典名称
							dictionary.setName(dict.getColumn("detail").getTableColumn().getRow(0)
									.getColumn("currencySymbol").getStringColumn());
							// 状态
							dictionary.setStatus(dict.getColumn("editStatus").getStringColumn());
						}
					}
					RedisManager.getManager().set(RedisNamespaceEnum.api_service_currency.code(), doc.toString(), 3600);
				} else if ("COUNTRYAREATYPE".equalsIgnoreCase(typecode)) {
					/*** 获取证件签发国和手机区号列表接口 ***/
					rs.addNewSuccess();
					LCCDictionaryInfos infos = rs.addNewResponse().addNewLCCDictionaryInfos();
					Table areaList = xmlOutput.getParm("country_area_list").getTableColumn();
					if (null != areaList && areaList.getRowCount() > 0) {
						for (int i = 0; i < areaList.getRowCount(); i++) {
							Row area = areaList.getRow(i);
							LCCDictionaryInfo dictionary = infos.addNewLCCDictionaryInfo();
							// 国家二字码
							dictionary.setCode(area.getColumn("code2").getStringColumn());
							// 国家简称
							dictionary.setName(StatusUtil.getLanguageName(area.getColumn("name_lite").getObjectColumn(),
									ApiServletHolder.getApiContext().getLanguage()));
							// 手机国际区号
							dictionary.setLinkCode(area.getColumn("phone_code").getStringColumn());
						}
					}
					//将xml数据放入redis中
					RedisManager.getManager().set(RedisNamespaceEnum.api_service_countryarea.toKey(ApiServletHolder.getApiContext().getLanguage()), doc.toString(), 3600);
				} else if ("IDENTITYTYPE".equalsIgnoreCase(typecode)) {
					//获取证件类型
					rs.addNewSuccess();
					LCCDictionaryInfos infos = rs.addNewResponse().addNewLCCDictionaryInfos();
					Table areaList = xmlOutput.getParm("identitytype").getTableColumn();
					if (null != areaList && areaList.getRowCount() > 0) {
						for (int i = 0; i < areaList.getRowCount(); i++) {
							Row area = areaList.getRow(i);
							LCCDictionaryInfo dictionary = infos.addNewLCCDictionaryInfo();
							// 证件code
							dictionary.setCode(area.getColumn("code").getStringColumn());
							// 证件名称
							dictionary.setName(StatusUtil.getLanguageName(area.getColumn("name").getObjectColumn(),
									ApiServletHolder.getApiContext().getLanguage()));
						}
					}
					//将xml数据放入redis中
					RedisManager.getManager().set(RedisNamespaceEnum.api_service_identitytype.code()+ApiServletHolder.getApiContext().getLanguage(), doc.toString(), 3600);
				}
			}
		} catch (Exception e) {
			// 初始化XML节点
			doc = LCCDictionaryReadRSDocument.Factory.newInstance();
			doc.addNewLCCDictionaryReadRS();
			// 存在错误信息
			ErrorType error = rs.addNewErrors().addNewError();
			error.setCode(TipMessager.getErrorCode(ErrCodeConstants.API_SYSTEM));
			// 错误描述
			error.setStringValue(TipMessager.getMessage(ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()));
		}
		return doc;
	}
}
