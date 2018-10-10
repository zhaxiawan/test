package com.travelsky.quick.util.helper;

import org.iata.iata.edist.MsgPartiesType;
import org.iata.iata.edist.TravelAgencySenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;

public class APICacheHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(APICacheHelper.class);
	public static final int CACHE_TIMEOUT = 300;
	private static final APICacheHelper HELPER = new APICacheHelper();
	//API安全配置类型
	public static final String APP_TYPE_LIST = "APP_LIST";
	//调用第三方系统配置类型
	public static final String APP_TYPE_APP= "APP";
	//应用配置类型
	public static final String APP_TYPE_COMMON= "COMMON";

	public static APICacheHelper getInstance() {
		return HELPER;
	}

	/**
	 * 从Cache中获取token
	 *
	 * @param appId
	 * @return
	 * @throws APIException
	 */
	public String getToken(String appId) throws APIException {
		return new TokenManager().getToken(appId);
	}

	/**
	 * 获取app允许的最大并发数
	 *
	 * @param appID
	 * @param version
	 * @return
	 * @throws APIException
	 */
	public String getAppMaxConcurrent(String appId, String version) throws APIException {
		String maxNum = new AppConcurrentManager().getMaxNum(appId, version);
		if (!StringUtils.hasLength(maxNum)) {
			LOGGER.warn(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_MAX_CONCURRNET,
					ApiServletHolder.getApiContext().getLanguage()));
		}

		return maxNum;
	}

	/**
	 * 获取app当前并发数
	 *
	 * @param appID
	 * @param version
	 * @return
	 */
	public String getAppCurConcurrent(String appID, String version) {
		return new AppConcurrentManager().getAppCurConcurrent(appID, version);
	}

	/**
	 * app并发数加1
	 */
	public void incrAppConcurrent(String appId, String version) {
		new AppConcurrentManager().incrAppConcurrent(appId, version);
	}

	/**
	 * app并发数减1
	 */
	public void decrAppConcurrent(String appId, String version) {
		new AppConcurrentManager().decrAppConcurrent(appId, version);
	}

	/**
	 * 重置app并发数
	 *
	 * @param appId
	 * @param version
	 */
	public void resetAppConcurrent(String appId, String version) {
		new AppConcurrentManager().resetAppConcurrent(appId, version);
	}

	/**
	 * 获取service允许的最大并发数
	 *
	 * @param serviceName
	 * @param version
	 * @return
	 * @throws APIException
	 */
	public String getSevMaxConcurrent(String appId, String serviceName, String version) throws APIException {
		String maxNum = new ServiceConcurrentManager().getMaxNum(appId, serviceName, version);
		if (!StringUtils.hasLength(maxNum)) {
			LOGGER.debug("appId...."+appId+"serviceName....."+serviceName+"version...."+version);
			LOGGER.warn(TipMessager.getInfoMessage(ErrCodeConstants.API_NULL_MAX_SEVCONCURRNET,
					ApiServletHolder.getApiContext().getLanguage()));
		}

		return maxNum;
	}

	/**
	 * 获取service当前并发数
	 *
	 * @param appId
	 * @param serviceName
	 * @param version
	 * @return
	 */
	public String getSevCurConcurrent(String appId, String serviceName, String version) {
		return new ServiceConcurrentManager().getSevCurConcurrent(appId, serviceName, version);
	}

	/**
	 * 重置service并发数
	 *
	 * @param appId
	 * @param serviceName
	 * @param version
	 */
	public void resetSevConcurrent(String appId, String serviceName, String version) {
		new ServiceConcurrentManager().resetSevConcurrent(appId, serviceName, version);
	}

	/**
	 * 服务并发数加1
	 * @param appId
	 * @param serviceName
	 * @param version
	 */
	public void incrServiceConcurrent(String appId, String serviceName, String version) {
		new ServiceConcurrentManager().incrServiceConcurrent(appId, serviceName, version);
	}

	/**
	 * 服务并发数减1
	 * @param appId
	 * @param serviceName
	 * @param version
	 */
	public void decrServiceConcurrent(String appId, String serviceName, String version) {
		new ServiceConcurrentManager().decrServiceConcurrent(appId, serviceName, version);
	}

	/**
	 * 获取上传文件最大阈值。上传文件时超过此值，则会暂时缓存到硬盘
	 *
	 * @return
	 * @throws APIException
	 */
	public int getMaxFileMemory() throws APIException {
		return new UploadFileManager().getMaxFileMemory();
	}

	/**
	 * 获取上传文件大小上限(kb)
	 *
	 * @return
	 * @throws APIException
	 */
	public long getMaxFileSize() throws APIException {
		return new UploadFileManager().getMaxFileSize();
	}

	/**
	 * 是否格式化XML输出0/1:否/是
	 * @throws APIException
	 */
	public static boolean formatOutputXml() throws APIException  {
		//使用全局的manager对象进行数据获取会抛异常
		String value = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_xmlformatswitch.code());
		if (!StringUtils.hasLength(value)) {
			// 调底层获取
			value = new ConfigurationManager().getAppCacheValue(
					"API_XML_FORMAT_SWITCH",APICacheHelper.APP_TYPE_COMMON);
			if (!StringUtils.hasLength(value)) {
				value = "1";
			}
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_xmlformatswitch.code(), value, APICacheHelper.CACHE_TIMEOUT);
		}

		return "1".equals(value);
	}

	/**
	 * pay配置
	 */
	/*public static Map<String ,String> getPayConfg(){
		Map<String ,String> paymap = new HashMap<String, String>();

		CommandData data = new CommandData();
		String value = redisManager.get("API_PAY_CONFG");
		if (!StringUtils.hasLength(value)) {
			// 先调用res接口，判断，再将值存到redis中
			CommandInput commandInput = new CommandInput("com.cares.sh.order.config.query");
			commandInput.addParm("type", "APP");
			commandInput.addParm("code", "API_PAY_CONFG");
			CommandRet commandRet = ServiceCenter.doOther(commandInput);

			Table l_tabe=commandRet.getParm("configs").getTableColumn();
			value =l_tabe.getRow(0).getColumn("data").toString();

			RedisManager.getManager().set("API_PAY_CONFG", value, APICacheHelper.CACHE_TIMEOUT);
		}

		JsonUnit.fromJson(data, value);
		String apptype = data.getParm("apptype").getStringColumn();
		String orgid = data.getParm("orgid").getStringColumn();
		String returnid = data.getParm("returnid").getStringColumn();
		String payurl = data.getParm("payurl").getStringColumn();
		String ordername = data.getParm("ordername").getStringColumn();
		String ordercurtype = data.getParm("ordercurtype").getStringColumn();
		String ordertype = data.getParm("ordertype").getStringColumn();
		String lan = data.getParm("lan").getStringColumn();
		String paytype = data.getParm("paytype").getStringColumn();

		paymap.put("ORGID",orgid);
		paymap.put("APPTYPE", apptype);
		paymap.put("RETURNID", returnid);
		paymap.put("PAYURL", payurl);
		paymap.put("ORDERNAME", ordername);
		paymap.put("ORDERCURTYPE", ordercurtype);
		paymap.put("ORDERTYPE", ordertype);
		paymap.put("LAN", lan);
		paymap.put("PAYTYPE", paytype);
		return paymap;
	}
*/
	
	public static void setDeptInfo(SelvetContext<ApiContext> context, MsgPartiesType party){
		if(party!=null 
				&& party.getSender()!=null 
				&& party.getSender().getTravelAgencySender()!=null){
			
			TravelAgencySenderType tas = party.getSender().getTravelAgencySender();
			String officeCode = "";
			String ownerID = "";
			String iataNo = "";
			String pseudoCity = "";
			String type = "";
			if(tas.getAgencyID()!=null){
				officeCode = tas.getAgencyID().getStringValue();
			}
			if(tas.getAgencyID().getOwner()!=null){
				ownerID = tas.getAgencyID().getOwner();
			}
			if(tas.getIATANumber()!=null){
				iataNo = tas.getIATANumber();
			}
			if(tas.getPseudoCity()!=null){
				pseudoCity = tas.getPseudoCity().getStringValue();
			}
			if(tas.getType()!=null){
				type = tas.getType();
			}
			ApiContext ac = context.getContext();
			ac.setChannelNo("GDS"+ownerID);
			ac.setTicketDeptid(ownerID+"$"+iataNo+"$"+officeCode+"$"+pseudoCity+"$"+type);
		}
	}
	/**
	 * 
	 * @param context 请求参数
	 * @param party   节点头信息
	 * @param input	     返回节点头信息数据拼接（ownerID）
	 * @return        需要返回party节点下信息时，使用此方法
	 */
	public static CommandData setDeptInfo(SelvetContext<ApiContext> context, MsgPartiesType party,CommandData input){
		if(party!=null 
				&& party.getSender()!=null 
				&& party.getSender().getTravelAgencySender()!=null){
			
			TravelAgencySenderType tas = party.getSender().getTravelAgencySender();
			String officeCode = "";
			String ownerID = "";
			String iataNo = "";
			String pseudoCity = "";
			String type = "";
			if(tas.getAgencyID()!=null){
				officeCode = tas.getAgencyID().getStringValue();
			}
			if(tas.getAgencyID().getOwner()!=null){
				ownerID = tas.getAgencyID().getOwner();
				input.addParm("owner", ownerID);
			}else {
				input.addParm("owner", "");
			}
			if(tas.getIATANumber()!=null){
				iataNo = tas.getIATANumber();
			}
			if(tas.getPseudoCity()!=null){
				pseudoCity = tas.getPseudoCity().getStringValue();
			}
			if(tas.getType()!=null){
				type = tas.getType();
			}
			ApiContext ac = context.getContext();
			ac.setChannelNo("GDS"+ownerID);
			ac.setTicketDeptid(ownerID+"$"+iataNo+"$"+officeCode+"$"+pseudoCity+"$"+type);
		}
		return input;
	}
}
