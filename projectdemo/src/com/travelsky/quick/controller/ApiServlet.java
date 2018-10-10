package com.travelsky.quick.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.SystemConfig;
import com.cares.sh.comm.Unit;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.servlet.BaseServlet;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.common.ParmExUtil;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.log.ats.AtsLogHelper;
import com.travelsky.quick.util.DESUtil;
import com.travelsky.quick.util.concurrent.APIConcurrentManager;
import com.travelsky.quick.util.concurrent.ConcurrentManageService.ActionType;
import com.travelsky.quick.util.helper.ConfigurationManager;
import com.travelsky.quick.util.helper.DictManager;
import com.travelsky.quick.util.helper.FlightManager;
import com.travelsky.quick.util.helper.MemberManager;
import com.travelsky.quick.util.helper.MessageManager;
import com.travelsky.quick.util.helper.QOrderOpManager;
import com.travelsky.quick.util.helper.ShoppingManager;
import com.travelsky.quick.util.helper.TipMessager;
import com.travelsky.quick.util.validate.AbstractBaseValidator;
import com.travelsky.quick.util.validate.BaseValidator;

/**
 *
 * @author Huxizhun
 *
 */
public class ApiServlet extends BaseServlet<ApiContext> {
	private static Logger logger = LoggerFactory.getLogger(ApiServlet.class);
	private static final long serialVersionUID = -1270136248566029930L;
	private static String language = ApiServletHolder.getApiContext().getLanguage();
	//API安全配置类型
	public static final String APP_TYPE_LIST = "APP_LIST";
	private CommandData cmdData;
	@Override
	public SelvetContext<ApiContext> newContext() {
		return new SelvetContext<ApiContext>(new ApiContext());
	}
	@Override
	public boolean checkLogin(SelvetContext<ApiContext> context){
		return true;
	}

	/**
	 * 初始化
	 * @param json
	 * @throws Exception
	 */
	protected void initContext(String json) throws Exception {
		ApiServletHolder.getAndInit(ApiServletHolder.get().getInput(), json);
	}

	protected boolean isEncrypt() {
		return true;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		response.setContentType("application/json; charset=utf-8");
		SelvetContext<ApiContext> context = ApiServletHolder.get();


		// 获取请求json
		CommandRet reqJsonRet = parseInputJson(request, context.getInput());

		String key=ParmExUtil.getDesKey();
		if (reqJsonRet.isError()) {
			HttpServletResponse resp = context.getResponse();
			String result = JsonUnit.toJson(reqJsonRet);
			PrintWriter writer = resp.getWriter();
			try {
				writer.write(isEncrypt()?DESUtil.newEncrypt(result, key):result);
				writer.flush();
			} catch (Exception e) {
				logger.error("DES encrypt failed! The encrypt content:[{}], key:[{}]",result, key);
			}
			return ;
		}

		String reqJson = reqJsonRet.getParm("reqJson").getStringColumn();
		String result=null;
		try {
			// 初始化容器失败
			initContext(reqJson);
		} catch (Exception e2) {
			// If encrypt return result failed then set result system error.
			CommandRet ret = new CommandRet("");
			ret.setError(ErrCodeConstants.API_SYSTEM, "系统错误!");
			try {
				// Encrypt result failed
				result=JsonUnit.toJson(ret);
				response.getWriter().write(isEncrypt()?DESUtil.encrypt(result, key):result);
			} catch (Exception e1) {
				logger.error("DES encrypt failed! The encrypt content:[{}], key:[{}]",result, key);
			}

			return ;
		}

		//记录B2C的JSON请求
//		saveJsonfromB2C(reqJson);
		AbstractBaseValidator validator = new BaseValidator();
		// 验证
		if (!validator.validate()) {
			CommandRet ret = context.getRet();
			if (ret == null || !StringUtils.hasLength(ret.getErrorCode())) {
				ret.setError(ErrCodeConstants.API_VALIDATE_FAILD,
						TipMessager.getMessage(ErrCodeConstants.API_VALIDATE_FAILD,
								language));
			}

			result=JsonUnit.toJson(ret);
			try {
				response.getWriter().write(isEncrypt()?DESUtil.encrypt(result,ParmExUtil.getDesKey()):result);
			} catch (Exception e) {
				logger.error("DES encrypt failed! The encrypt content:[{}], key:[{}]",result, key);
			}
			//减并发数
			subConcurrent();
			ApiServletHolder.destory();
			return ;
		}
		// 初始化channel no
		context.getContext().initChannelNo();

		context.begin();
		//====日志部分开始====
		//如果不存在全局TransactionID，则生成一个
		if (context.getTransactions().equals("")) {
			context.initTransactions();
		}

		Date startTime = new Date();
		//取得module名
		String module = this.getClass().getName() + "-doPost";
		//将模块名中【.】换为【-】,因为【.】属于日志头部分的保留字符
		//取得TransactionID
		String tid = context.getTransactions();
		module = module.replace('.','-') ;
		ApiContext apiCtx = context.getContext();
		//取得userid
		String userid = apiCtx.getUserID();
		//生成扩展区
		HashMap<String,String> extend = new HashMap<String,String>();
		extend.put ("IP", SystemConfig.getServerIp());
		extend.put ("Action", apiCtx.getServiceName());
		//====日志部分结束====
		try
		{
			if(this.checkLogin(context) && this.checkAction(context)){
				SystemConfig.setTID(context.getTransactions());
				//context.log();
				//Add Begin
				//输出跟踪带日志STAT_IN部分
				String iptJson = JsonUnit.toJson(context.getInput());
				AtsLogHelper.outputAtsLog("STAT_IN", module, tid, userid, extend, iptJson);
				// 审计日志
//				StatisticsAtsLogHelper.outputAtsLog("STAT_IN", module, tid, userid, extend, iptJson);
				//Add End
				this.doServlet(context);
			}

		}
		catch(Exception ex){
			logger.error("",ex);
			context.getRet().setError("9999", "系统错误");
		}
		context.commit();
		result=JsonUnit.toJson(context.getRet());
		if(this.isJsonOutput()){
			if(context.getRet().isError() && context.getRet().getErrorCode().equals("2004")){
				logger.error("Msg:数据库错误!Error Code:2004");
				context.getRet().setError("2004", "系统错误!");
			}
			//记录api返回给B2C的json
//			saveJsontoB2C(result);
			try {
				String encryResult=isEncrypt()?DESUtil.encrypt(result, key):result;
				response.getWriter().write(encryResult);
			} catch (Exception e) {
				// If encrypt return result failed then set result system error.
				CommandRet ret = new CommandRet("");
				ret.setError(ErrCodeConstants.API_SYSTEM,
						TipMessager.getInfoMessage(ErrCodeConstants.API_SYSTEM, context.getContext().getLanguage()));
				try {
					// Encrypt result failed
					result=JsonUnit.toJson(ret);
					response.getWriter().write(isEncrypt()?DESUtil.encrypt(result, key):result);
				} catch (Exception e1) {
					logger.error("DES encrypt failed! The encrypt content:[{}], key:[{}]",result, key);
				}
			}
		}
		//====日志部分开始====
		if(this.checkLogin(context) && this.checkAction(context) && this.isWriteLog()){
			Date endTime = new Date();
			//计算Servlet调用所花费时间（单位：毫秒）
			long timeCost = endTime.getTime() - startTime.getTime();
			extend.put ("TimeCost", Long.toString(timeCost));
			//输出跟踪带日志STAT_OUT部分
			AtsLogHelper.outputAtsLog("STAT_OUT", module, tid, userid, extend, result);
		}
		//====日志部分结束====
		// 减并发数
		subConcurrent();
		ApiServletHolder.destory();
	}


	/**
	 * 减并发数
	 */
	private void subConcurrent() {
		ApiContext apiCtx = ApiServletHolder.getApiContext();
		try {
			// 减并发数
			APIConcurrentManager.getInstance().operate(ActionType.SUB, apiCtx.getAppID(), apiCtx.getServiceName(), apiCtx.getVersion());
		}
		catch (Exception e) {
			logger.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_UNKNNOW_CONCURRNET, apiCtx.getLanguage()), e);
		}
	}

	/**
	 * 获取请求json
	 * @param request request
	 * @param input input
	 * @return CommandRet
	 */
	protected CommandRet parseInputJson(HttpServletRequest request, CommandData input){
		CommandRet ret = new CommandRet("");
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(ServletInputStream) request.getInputStream(), "utf-8"));
			StringBuffer sb = new StringBuffer("");
			String temp;
			while ((temp = br.readLine()) != null) {
				sb.append(temp);
			}
			br.close();
			String json = sb.toString().trim();
			String desKey = ParmExUtil.getDesKey();
			json =DESUtil.decrypt(json, desKey);
			if(!json.equals("") && json.startsWith("{") && json.endsWith("}")){
				JsonUnit.fromJson(input, json);
				// 去掉sign
				String regx = "[\"|\'](sign|Sign)[\"|\'].*?[\"|\'].*?[\"|\']";
				regx = new StringBuilder(regx)
						.append(",\\s*?|,\\s*")
						.append(regx)
						.toString();
				json = json.replaceAll(regx, "");

				ret.addParm("reqJson", json);
			}
			else {
				ret.setError(ErrCodeConstants.API_PARAM_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_PARAM_ERROR,
								language));
			}
		}catch(Exception ex){
			Unit.process(ex);
			ret.setError(ErrCodeConstants.API_PARAM_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_PARAM_ERROR,
							language));
		}

		return ret;
	}

	@Override
	public void doServlet(SelvetContext<ApiContext> context) {
		String lMode = context.getInput().getParm("mode").getStringColumn();
		context.getInput().addParm("ticketdeptid", context.getContext().getTicketDeptid());
		if("shopping".equals(lMode)){
			CommandRet lret = new CommandRet("");
			ShoppingManager lManager = new ShoppingManager();
			CommandData input = context.getInput();
			String  isSingle = input.getParm("isSingle").getStringColumn();
			if(null == isSingle || "".equals(isSingle)){
				lret.setError(ErrCodeConstants.API_SHOPPING_ISSINGLE_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_SHOPPING_ISSINGLE_ERROR,
								language));
			}
			//单程
			else if("1".equals(isSingle)){
				input.addParm("deptdate", input.getParm("oridate"));
				CommandRet lret_OW  = lManager.shopping(input,context);
				if(!"".equals(lret_OW.getErrorCode())){
					lret_OW.addParm("errorcode", lret_OW.getErrorCode());
					lret_OW.addParm("errordesc", lret_OW.getErrorDesc());
				}
				lret.addParm("OW", lret_OW);
				lret.addParm("RT", "");
			}
			//往返
			else if("2".equals(isSingle)){
				//去
				input.addParm("deptdate", input.getParm("oridate"));
				CommandRet lret_OW = lManager.shopping(input,context);
				//回
				input.addParm("deptdate", input.getParm("destdate"));
				String arrive =	input.getParm("arrive").getStringColumn();
				String depart =	input.getParm("depart").getStringColumn();
				input.addParm("depart", arrive);
				input.addParm("arrive", depart);
				CommandRet lret_RT = lManager.shopping(input,context);
				if(!"".equals(lret_OW.getErrorCode())){
					lret_OW.addParm("errorcode", lret_OW.getErrorCode());
					lret_OW.addParm("errordesc", lret_OW.getErrorDesc());
				}
				if(!"".equals(lret_RT.getErrorCode())){
					lret_RT.addParm("errorcode", lret_RT.getErrorCode());
					lret_RT.addParm("errordesc", lret_RT.getErrorDesc());
				}
				lret.addParm("OW", lret_OW);
				lret.addParm("RT", lret_RT);
			}
			context.setRet(lret);
		}
		//获取APPID配置
		else if("appid".equals(lMode)){
			CommandRet ret = new CommandRet("");
			cmdData = new CommandData();
			try {
				//获取AppID
				String appId = ApiServletHolder.getApiContext().getAppID();
				CommandData input = context.getInput();
				//MOBAPP手机APP渠道   MOBWEB手机web渠道
				input.addParm("code",appId);
				ShoppingManager lManager = new ShoppingManager();
				//最大支付时限
				ret = lManager.payTime(input,context);

				ConfigurationManager  config = new ConfigurationManager();
				String data = config.getAppCacheValue(appId,APP_TYPE_LIST);
				if (StringUtils.hasLength(data)) {
					JsonUnit.fromJson(cmdData, data);
					//最大未支付订单数
					String unpayOrderMaxNum = cmdData.getParm("unpayedOrderMaxNum").getStringColumn();
					//最大乘机人个数
					String psrMaxNum = cmdData.getParm("psrMaxNum").getStringColumn();
					ret.addParm("unpayedOrderMaxNum",unpayOrderMaxNum);
					ret.addParm("psrMaxNum",psrMaxNum);
				}
				context.setRet(ret);
			} catch (APIException e) {
				Unit.process(e);
				ret.setError("0001", e.getMessage());
				context.setRet(ret);
				}
		}
		//前置条件查询
		else if("precondition".equals(lMode)){
			QOrderOpManager orderOpManager = new QOrderOpManager();
			String memeberId = ApiServletHolder.getApiContext().getUserID();
			context.getInput().addParm("memberid", memeberId);
			context.setRet(orderOpManager.precondition(context.getInput(), context));
		}
		//获取支付时限
		else if("paytime".equals(lMode)){
			CommandData input = context.getInput();
			//MOBAPP手机APP渠道   MOBWEB手机web渠道
			input.addParm("code",context.getContext().getChannelNo());
			ShoppingManager lManager = new ShoppingManager();
			context.setRet(lManager.payTime(input,context));
		}
		else if("scheduleshopping".equals(lMode)){
			ShoppingManager lManager = new ShoppingManager();
			context.setRet(lManager.scheduleshopping(context.getInput(),context));
		}else if("refundproduct".equals(lMode)){
			//退订辅营
			ShoppingManager lManager = new ShoppingManager();
			context.setRet(lManager.refundproduct(context.getInput(),context));
		}else if("productshopping".equals(lMode)){
			//辅营查询带参数的
			ShoppingManager lManager = new ShoppingManager();
			context.setRet(lManager.productshopping(context.getInput(),context));
		}else if("productquery".equals(lMode)){
			//保险查询接口
			ShoppingManager lManager = new ShoppingManager();
			context.setRet(lManager.query(context.getInput(),context));
		}else if("seatshopping".equals(lMode)){
			ShoppingManager lManager = new ShoppingManager();
			context.setRet(lManager.seatshopping(context.getInput(),context));
		}
		else if("sendsms".equals(lMode)){
			//14.发送短信接口
			MessageManager messageManager=new MessageManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(messageManager.sendsms(context.getInput(), context));
		}
		else if("sendemail".equals(lMode)){
			//15.发送电子邮件接口
			MessageManager messageManager=new MessageManager();
			context.setRet(messageManager.sendemail(context.getInput(), context));
		}
		else if("querydict".equals(lMode)){
			//16.字典查询接口
			DictManager dictManager=new DictManager();
			context.setRet(dictManager.getDict(context.getInput(), context));
		}
		else if("login".equals(lMode)){
			//01.会员登录
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.login(context.getInput(), context));
		}
		else if("register".equals(lMode)){
			//02.会员注册
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.register(context.getInput(), context));
		}
		else if("viewmember".equals(lMode)){
			//03.获取会员信息
			MemberManager memberManager=new MemberManager();
			String memberid = context.getInput().getParm("username").getStringColumn();
			context.getInput().addParm("memberid", memberid);
			context.getInput().addParm("ticketdeptid", context.getContext().getTicketDeptid());
			context.setRet(memberManager.getMember(context.getInput(), context));
		}
		else if("updatemember".equals(lMode)){
			//04.会员修改
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.updMember(context.getInput(), context));
		}
		else if("addid".equals(lMode)){
			//05.新增会员证件号信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid",  ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.addID(context.getInput(), context));
		}
		else if("updateid".equals(lMode)){
			//06.修改会员证件号信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid",  ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.updID(context.getInput(), context));
		}
		else if("deleteid".equals(lMode)){
			//07.删除会员证件号信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid",  ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.delID(context.getInput(), context));
		}
		else if("addaddress".equals(lMode)){
			//08.新增会员地址信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid",  ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.addAddress(context.getInput(), context));
		}
		else if("updateaddress".equals(lMode)){
			//09.修改会员地址信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid",  ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.updAddress(context.getInput(), context));
		}
		else if("deleteaddress".equals(lMode)){
			//10.删除会员地址信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid",  ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.delAddress(context.getInput(), context));
		}
		else if("addcontact".equals(lMode)){
			//11.新增会员联系信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.addContact(context.getInput(), context));
		}
		else if("updatecontact".equals(lMode)){
			//12.修改会员联系信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.updContact(context.getInput(), context));
		}
		else if(lMode.equals("deletecontact")){
			//13.删除会员联系信息
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.delContact(context.getInput(), context));
		}
		else if("changepassword".equals(lMode)){
			//17.修改密码接口
			MemberManager memberManager=new MemberManager();
			CommandData input = context.getInput();
			input.addParm("memberid", context.getContext().getUserID());
			context.setRet(memberManager.changePwd(context.getInput(), context));
		}
		else if("resetpassword".equals(lMode)){
			//18.重置密码接口
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", context.getInput().getParm("username").getStringColumn());
			context.setRet(memberManager.resetPwd(context.getInput(), context));
		}
		else if("querypreference".equals(lMode)){
			//19.查询偏好接口
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.getPreference(context.getInput(), context));
		}
		else if("updatepreference".equals(lMode)){
			//20.修改偏好接口
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.updPreference(context.getInput(), context));
		}
		else if("querypassenger".equals(lMode)){
			//21.查询常用乘机人接口
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.getPassenger(context.getInput(), context));
		}
		else if("addpassenger".equals(lMode)){
			//22.增加常用乘机人接口
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.addPassenger(context.getInput(), context));
		}
		else if("updatepassenger".equals(lMode)){
			//23.修改常用乘机人接口
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.updPassenger(context.getInput(), context));
		}
		else if("deletepassenger".equals(lMode)){
			//24.删除常用乘机人接口
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(memberManager.delPassenger(context.getInput(), context));
		}
		else if("queryallnotice".equals(lMode)){
			//25.获取所有通知列表接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.queryAllNotice(context.getInput(), context));
		}
		else if("querynotice".equals(lMode)){
			//26.查询用户通知列表接口
			MemberManager memberManager=new MemberManager();
			CommandData commandData=context.getInput();
			commandData.addParm("memberid", commandData.getParm("UserID").getStringColumn());
			context.setRet(memberManager.getNotice(commandData, context));
		}
		else if("updatenotice".equals(lMode)){
			//27.修改用户通知列表接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.updNotice(context.getInput(), context));
		}
		else if("queryredpacket".equals(lMode)){
			//28.查询红包接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.queryredpacket(context.getInput(), context));
		}
		else if("querymyairline".equals(lMode)){
			//29.查询航线预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.querymyairline(context.getInput(), context));
		}
		else if("addmyairline".equals(lMode)){
			//30.增加航线预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.addmyairline(context.getInput(), context));
		}
		else if("cancelmyairline".equals(lMode)){
			//31.取消航线预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.cancelmyairline(context.getInput(), context));
		}
		else if("deletemyairline".equals(lMode)){
			//32.删除航线预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.deletemyairline(context.getInput(), context));
		}
		else if("querymyflight".equals(lMode)){
			//33.查询航班预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.querymyflight(context.getInput(), context));
		}
		else if("addmyflight".equals(lMode)){
			//34.增加航班预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.addmyflight(context.getInput(), context));
		}
		else if("cancelmyflight".equals(lMode)){
			//35.取消航班预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.cancelmyflight(context.getInput(), context));
		}
		else if("deletemyflight".equals(lMode)){
			//36.删除航班预约接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.deletemyflight(context.getInput(), context));
		}
		else if("identify".equals(lMode)){
			//认证手机和邮箱接口
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.identify(context.getInput(), context));
		}
		else if("memberauth".equals(lMode)){
			//实名认证
			MemberManager memberManager=new MemberManager();
			context.setRet(memberManager.memberAuth(context.getInput(), context));
		}
		else if("createorder".equals(lMode)){
			//订单创建接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			String memeberId = ApiServletHolder.getApiContext().getUserID();
			context.getInput().addParm("memberid", memeberId);
			context.setRet(orderOpManager.createorder(context.getInput(), context));
		}
		else if("setsubmarket".equals(lMode)){
			//订单提交辅营接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			context.setRet(orderOpManager.setsubmarket(context.getInput(), context));
		}
		else if("setseat".equals(lMode)){
			//订单提交座位接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			context.setRet(orderOpManager.setseat(context.getInput(), context));
		}
		else if("submit".equals(lMode)){
			//45.订单提交接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			String memeberId = ApiServletHolder.getApiContext().getUserID();
			context.getInput().addParm("memberid", memeberId);
			context.setRet(orderOpManager.submit(context.getInput(), context));
		}
		else if("orderpay".equals(lMode)){
			//46.订单支付接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			String memeberId = ApiServletHolder.getApiContext().getUserID();
			context.getInput().addParm("memberid", memeberId);
			context.setRet(orderOpManager.orderpay(context.getInput(), context));
		}
		else if("offlinepay".equals(lMode)){
			//线下支付
			QOrderOpManager orderOpManager = new QOrderOpManager();
			context.setRet(orderOpManager.offlinepay(context.getInput(), context));
		}
		else if("query".equals(lMode)){
			// 51.	订单查询接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			CommandData input = context.getInput();
			input.addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			// 临时存入分页信息，定义一个较大数值，以查询所有记录
		/*	//当前页数
			String lPageNum = input.getParm("pagenum").getStringColumn();
			//每页显示的个数
			String lPageSize = input.getParm("pagesize").getStringColumn();
			input.addParm("pagenum", lPageNum);
			input.addParm("pagesize", lPageSize);*/
			context.setRet(orderOpManager.query(input, context));
		}else if("orderdelete".equals(lMode)){
			//删除订单接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			CommandData input = context.getInput();
			input.addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(orderOpManager.orderDelete(input, context));
		}
		else if("querypay".equals(lMode)){
			//查询订单支付
			QOrderOpManager orderOpManager = new QOrderOpManager();
			CommandData input = context.getInput();
			input.addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(orderOpManager.queryPay(input, context));
		}
		else if("detail".equals(lMode)){
			// 52. 订单明细查询接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			CommandData input = context.getInput();
			input.addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(orderOpManager.detail(input, context));
		}
		else if("refundbudget".equals(lMode)){
			// 53.	退票预算接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(orderOpManager.refundbudget(context.getInput(), context));
		}
		else if("refund".equals(lMode)){
			// 54.	提交退票申请接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			// refundop表示退款操作. 1表示修改退款申请文件。否则表示退款申请}
			String op = context.getInput().getParm("refundop").getStringColumn();
			if("1".equals(op)){
				context.setRet(orderOpManager.updateFile(context.getInput(), context));
			}else{
				context.setRet(orderOpManager.refund(context.getInput(), context));
			}

		}
		else if("querttravel".equals(lMode)){
			// 55. 我的行程查询接口
			QOrderOpManager orderOpManager = new QOrderOpManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(orderOpManager.querttravel(context.getInput(), context));
		}
		else if("cancel".equals(lMode)){
			//订单取消
			QOrderOpManager orderOpManager = new QOrderOpManager();
			String memeberId = ApiServletHolder.getApiContext().getUserID();
			context.getInput().addParm("memberid", memeberId);
			context.setRet(orderOpManager.cancel(context.getInput(), context));
		}
		else if("odquery".equals(lMode)){
			FlightManager flightManager = new FlightManager();
			context.getInput().addParm("memberid", ApiServletHolder.getApiContext().getUserID());
			context.setRet(flightManager.odQuery(context.getInput(), context));
		}
		else if("SearchValidFltDates".equals(lMode)){
//			FlightManager flightManager = new FlightManager();
//			context.setRet(flightManager.searchValidFltDates(context.getInput(), context));
		}
		else if("resetpwdvalid".equals(lMode)){
			//重置密码校验接口(手机端)
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", context.getInput().getParm("username").getStringColumn());
			context.setRet(memberManager.resetpwdvalid(context.getInput(), context));
		}
		else if("resetPwd2".equals(lMode)){
			//重置密码接口(手机端)
			MemberManager memberManager=new MemberManager();
			context.getInput().addParm("memberid", context.getInput().getParm("username").getStringColumn());
			context.setRet(memberManager.resetPwd2(context.getInput(), context));
		}
		else{
			context.getRet().setError("9999", "无效的调用");
		}
	}
	/**
	 * 将B2C的请求json 直接写入固定队列 不接收返回
	 * @param json 数据
	 *
	 */
	public void saveJsonfromB2C(String json){
		ApiContext apiContext=ApiServletHolder.getApiContext();
		CommandInput input=new CommandInput("");
		input.addParm("UserId", apiContext.getUserID());
		input.addParm("APPID", apiContext.getAppID());
		input.addParm("version", apiContext.getVersion());
		input.addParm("ServiceName", apiContext.getServiceName());
		input.addParm("Language", apiContext.getLanguage());
		input.addParm("timestamp", apiContext.getTimestamp());
		input.addParm("json", json);
		try {
			String l_json = JsonUnit.toJson(input);
			l_json="req:"+l_json;
//			ProMsgIbmC.getMsg().writeAPIXMLMsg(l_json, input.getId());
		} catch (Exception e) {
			logger.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_SYSTEM,
					ApiServletHolder.getApiContext().getLanguage()), e);
		}

	}
	/**
	 * 返回给B2C的json 直接写入固定队列 不接收返回
	 * @param json 数据
	 *
	 */
//	public void saveJsontoB2C(String json){
//		ApiContext apiContext=ApiServletHolder.getApiContext();
//		CommandInput input=new CommandInput("");
//		input.addParm("UserId", apiContext.getUserID());
//		input.addParm("APPID", apiContext.getAppID());
//		input.addParm("version", apiContext.getVersion());
//		input.addParm("ServiceName", apiContext.getServiceName());
//		input.addParm("Language", apiContext.getLanguage());
//		input.addParm("timestamp", apiContext.getTimestamp());
//		input.addParm("json", json);
//		try {
//			String l_json = JsonUnit.toJson(input);
//			l_json="req:"+l_json;
////			ProMsgIbmC.getMsg().writeAPIXMLMsg(l_json, input.getId());
//		} catch (Exception e) {
//			logger.error(TipMessager.getInfoMessage(
//					ErrCodeConstants.API_SYSTEM,
//					ApiServletHolder.getApiContext().getLanguage()), e);
//		}
//	}
}
