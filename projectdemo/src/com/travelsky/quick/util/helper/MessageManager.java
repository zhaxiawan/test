package com.travelsky.quick.util.helper;

import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.util.RedisUtil;

public class MessageManager {
	
	/**
	 * 初始化日志打印类
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageManager.class);

	public static final String REG_MOBILE="^[1][0-9]{10}$";
	public static final String REG_EMAIL="^([a-zA-Z0-9_\\.-]+)@([\\da-zA-Z\\.-]+)\\.([a-zA-Z\\.]{2,6})$";
	public static final String SERVICE = "SERVICE";
	public static final String EMAIL = "email";
	/**
	 * 发送短信接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet sendsms(CommandData input,SelvetContext<ApiContext> context){
		CommandRet l_ret = new CommandRet("");
		String mobile=input.getParm("mobile").getStringColumn();
		if("".equals(mobile)){
			//error desc 手机号码不能为空！
			l_ret.setError(ErrCodeConstants.API_NULL_MOBILE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MOBILE, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		//国际区号
		String areaCode = input.getParm("areaCode").getStringColumn();
		if("".equals(areaCode)){
			//error desc 国际区号不能为空!
			l_ret.setError(ErrCodeConstants.API_NULL_AREACODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_AREACODE, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		String messageType=input.getParm("messageType").getStringColumn();
		if("".equals(messageType)){
			//error desc 消息类型不能为空!
			l_ret.setError(ErrCodeConstants.API_NULL_MESSAGE_TYPE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MESSAGE_TYPE, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		//要是注册 就先验证手机号是否已存在
		else if("REGISTER_SEND_SMS".equals(messageType)){
			LOGGER.info("注册前进入用户是否已存在校验");
			//获取会员信息
			CommandInput l_input = new CommandInput("com.cares.sh.order.member.query");
			l_input.addParm("memberid", mobile);
			l_input.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			l_ret = context.doOther(l_input, false);
			LOGGER.info("注册前查询用户是否存在返回结果："+l_ret);
			if (!l_ret.isError()) {
				Table member= l_ret.getParm("member").getTableColumn();
				if(null != member && member.getRowCount()>0){
					//error desc 此用户名已存在！
					boolean flag = true;
					for(int i=0 ;i<member.getRowCount();i++){
						String memberid = member.getRow(i).getColumn("memberid").getStringColumn();
						if("".equals(memberid)){
							flag = false;
						}
					}
					if(flag){
						l_ret.setError(ErrCodeConstants.API_USER_NAME_ALREADY_EXIST, 
								TipMessager.getMessage(ErrCodeConstants.API_USER_NAME_ALREADY_EXIST, ApiServletHolder.getApiContext().getLanguage()));
						return l_ret;
					}
				}
			}
		}
		//要是重置密码，就先验证用户与手机号是否关联
		else if("FORGETPWD_SEND_SMS".equals(messageType)){
			String username=input.getParm("username").getStringColumn();
			if("".equals(username)){
				//error desc 用户名不能为空！
				l_ret.setError(ErrCodeConstants.API_NULL_USER_NAME,
						TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_NAME, ApiServletHolder.getApiContext().getLanguage()));
				return l_ret;
			}
			//获取会员信息
			CommandInput l_input = new CommandInput("com.cares.sh.order.member.view");
			l_input.addParm("id", username);
			l_input.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			l_ret = context.doOther(l_input, false);
			if (!l_ret.isError()) {
				// 获取联系信息
				Table binding = l_ret.getParm("binding").getTableColumn();
//				if(null == binding || "".equals(binding)){
//					//error desc 联系方式不存在！
//					l_ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
//							TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
//					return l_ret;
//				}
				boolean flag = true;
				for(int i=0 ;i<binding.getRowCount();i++){
					String mobileNo = binding.getRow(i).getColumn("bindingvalue").getStringColumn();
					if(mobile.equals(mobileNo)){
						flag = false;
					}
				}
				if(flag){
					l_ret.cleanAll();
					//error desc 联系方式不存在！
					l_ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
							TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
					return l_ret;
				}
			}else{
				//error desc 用户不存在！
				l_ret.setError(ErrCodeConstants.API_USER_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_USER_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
				return l_ret;
			}
		}
//		String msg=check(input);
//		if(!"".equals(msg)){
//			l_ret.setError("0001", msg);
//			return l_ret;
//		}
		//调用发送短信接口
		CommandInput commandInput = new CommandInput("com.travelsky.quick.notice.notice.sendsmsvc");
		commandInput.addParm("mobile", mobile);
		//国际区号
		commandInput.addParm("areaCode", areaCode);
		commandInput.addParm("serviceName", context.getInput().getParm("ServiceName"));
		commandInput.addParm("messageType", messageType);

		l_ret = context.doOther(commandInput,false);
		if(l_ret.isError()){
			l_ret.setError(l_ret.getErrorCode(), l_ret.getErrorDesc());
			return l_ret;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (l_ret.getErrorCode().equals("")||l_ret.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return l_ret;
	}
	
	/**
	 * 发送短信接口(NDC)
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet sendsmsndc(CommandData input,SelvetContext<ApiContext> context){
		CommandRet l_ret = new CommandRet("");
		String mobile=input.getParm("mobile").getStringColumn();
		if("".equals(mobile)){
			//error desc 手机号码不能为空！
			l_ret.setError(ErrCodeConstants.API_NULL_MOBILE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MOBILE, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		//国际区号
		String areaCode = input.getParm("areaCode").getStringColumn();
		if("".equals(areaCode)){
			//error desc 国际区号不能为空!
			l_ret.setError(ErrCodeConstants.API_NULL_AREACODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_AREACODE, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		String messageType=input.getParm("messageType").getStringColumn();
		if("".equals(messageType)){
			//error desc 消息类型不能为空!
			l_ret.setError(ErrCodeConstants.API_NULL_MESSAGE_TYPE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MESSAGE_TYPE, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		//要是注册 就先验证手机号是否已存在
		else if("REGISTER_SEND_SMS".equals(messageType)){
			LOGGER.info("注册前进入用户是否已存在校验");
			//获取会员信息
			CommandInput l_input = new CommandInput("com.cares.sh.order.member.query");
			l_input.addParm("memberid", mobile);
			l_input.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			l_ret = context.doOther(l_input, false);
			LOGGER.info("注册前查询用户是否存在返回结果："+l_ret);
			if (!l_ret.isError()) {
				Table member= l_ret.getParm("member").getTableColumn();
				if(null != member && member.getRowCount()>0){
					//error desc 此用户名已存在！
					boolean flag = true;
					for(int i=0 ;i<member.getRowCount();i++){
						String memberid = member.getRow(i).getColumn("memberid").getStringColumn();
						if("".equals(memberid)){
							flag = false;
						}
					}
					if(flag){
						l_ret.setError(ErrCodeConstants.API_USER_NAME_ALREADY_EXIST, 
								TipMessager.getMessage(ErrCodeConstants.API_USER_NAME_ALREADY_EXIST, ApiServletHolder.getApiContext().getLanguage()));
						return l_ret;
					}
				}
			}
		}
		//修改密码，判断手机号是否是用户关联手机号
		else if("UPDATEPWD_SEND_SMS".equals(messageType)){
			String memberid = input.getParm("memberid").getStringColumn();
			if ("".equals(memberid)) {
				//error desc 会员账号不能为空！
				l_ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
						TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, ApiServletHolder.getApiContext().getLanguage()));
				return l_ret;
			}
			//获取会员信息
			CommandInput l_input = new CommandInput("com.cares.sh.order.member.view");
			l_input.addParm("id", memberid);
			l_input.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			l_ret = context.doOther(l_input, false);
			if (!l_ret.isError()) {
				// 获取联系信息
				Table binding = l_ret.getParm("binding").getTableColumn();
				boolean flag = true;
				for(int i=0 ;i<binding.getRowCount();i++){
					String mobileNo = binding.getRow(i).getColumn("bindingvalue").getStringColumn();
					if(mobile.equals(mobileNo)){
						flag = false;
					}
				}
				if(flag){
					l_ret.cleanAll();
					//error desc 联系方式不存在！
					l_ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
							TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
					return l_ret;
				}
			}else{
				//error desc 用户不存在！
				l_ret.setError(ErrCodeConstants.API_USER_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_USER_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
				return l_ret;
			}
		}
		//要是重置密码，就先验证用户与手机号是否关联
		else if("FORGETPWD_SEND_SMS".equals(messageType)){
			String username=input.getParm("username").getStringColumn();
			if("".equals(username)){
				//error desc 用户名不能为空！
				l_ret.setError(ErrCodeConstants.API_NULL_USER_NAME,
						TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_NAME, ApiServletHolder.getApiContext().getLanguage()));
				return l_ret;
			}
			//获取会员信息
			CommandInput l_input = new CommandInput("com.cares.sh.order.member.view");
			l_input.addParm("id", username);
			l_input.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			l_ret = context.doOther(l_input, false);
			if (!l_ret.isError()) {
				// 获取联系信息
				Table binding = l_ret.getParm("binding").getTableColumn();
//				if(null == binding || "".equals(binding)){
//					//error desc 联系方式不存在！
//					l_ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
//							TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
//					return l_ret;
//				}
				boolean flag = true;
				for(int i=0 ;i<binding.getRowCount();i++){
					String mobileNo = binding.getRow(i).getColumn("bindingvalue").getStringColumn();
					if(mobile.equals(mobileNo)){
						flag = false;
					}
				}
				if(flag){
					l_ret.cleanAll();
					//error desc 联系方式不存在！
					l_ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
							TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
					return l_ret;
				}
			}else{
				//error desc 用户不存在！
				l_ret.setError(ErrCodeConstants.API_USER_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_USER_NON_EXIST, ApiServletHolder.getApiContext().getLanguage()));
				return l_ret;
			}
		}
//		String msg=check(input);
//		if(!"".equals(msg)){
//			l_ret.setError("0001", msg);
//			return l_ret;
//		}
		//调用发送短信接口
		CommandInput commandInput = new CommandInput("com.travelsky.quick.notice.notice.sendsmsvc");
		commandInput.addParm("mobile", mobile);
		//国际区号
		commandInput.addParm("areaCode", areaCode);
		commandInput.addParm("serviceName", context.getInput().getParm("ServiceName"));
		commandInput.addParm("messageType", messageType);

		l_ret = context.doOther(commandInput,false);
		if(l_ret.isError()){
			l_ret.setError(l_ret.getErrorCode(), l_ret.getErrorDesc());
			return l_ret;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (l_ret.getErrorCode().equals("")||l_ret.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return l_ret;
	}
	
	/**
	 * 发送邮件接口
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet sendemail(CommandData input,SelvetContext<ApiContext> context){
		CommandRet l_ret = new CommandRet("");
		String email=input.getParm("email").getStringColumn();
		if("".equals(email)){
			//error desc 邮件地址不能为空！
			l_ret.setError(ErrCodeConstants.API_NULL_EMAIL_ADDRESS,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_EMAIL_ADDRESS, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		String mailcontext=input.getParm("context").getStringColumn();
		if("".equals(mailcontext)){
			//error desc 邮件内空不能为空！
			l_ret.setError(ErrCodeConstants.API_NULL_EMAIL_CONTEXT,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_EMAIL_CONTEXT, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		String title=input.getParm("title").getStringColumn();
		if("".equals(title)){
			//error desc 邮件标题不能为空！
			l_ret.setError(ErrCodeConstants.API_NULL_EMAIL_TITLE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_EMAIL_TITLE, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		String msg=check(input);
		if(!"".equals(msg)){
			l_ret.setError(msg, TipMessager.getMessage(msg, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}
		//发送邮件接口
		CommandInput commandInput = new CommandInput("com.cares.sh.ibe.sendmail");
		commandInput.addParm("email", email);
		commandInput.addParm("mailcontext",mailcontext);
		commandInput.addParm("title",title);
		commandInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		l_ret = context.doOther(commandInput, false);

		if (l_ret.isError()) {
			l_ret.setError(l_ret.getErrorCode(), l_ret.getErrorDesc());
			return l_ret;
		}


		CommandInput l_input = new CommandInput("com.cares.sh.order.message.sendsms");
		l_input.addParm("email", email);
		l_input.addParm("context", mailcontext);
		l_input.addParm("title", title);
		l_ret = context.doOther(l_input,false);
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (l_ret.getErrorCode().equals("")||l_ret.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return l_ret;
	}

	private String check(CommandData input) {
		String mobile=input.getParm("mobile").getStringColumn();
		String email=input.getParm("email").getStringColumn();
		String title=input.getParm("title").getStringColumn();

		String mobiles[]=mobile.split(",");
		for (int i = 0; i < mobiles.length; i++) {
			if(!"".equals(mobiles[i])){
				if(!Pattern.matches(REG_MOBILE, mobiles[i])){
					//error desc 手机号不符合规范！
					return ErrCodeConstants.API_MOBILE_REG_ERROR;
				}
			}
		}

		String emails[]=email.split("\\,");
		for (int i = 0; i < emails.length; i++) {
			if(!"".equals(emails[i])){
				if(!Pattern.matches(REG_EMAIL, emails[i])){
					//error desc 邮箱不符合规范！
					return ErrCodeConstants.API_EMAIL_REG_ERROR;
				}
			}
		}

		if(!"".equals(title)&&title.length()>50){
			//error desc 邮件标题过长，应不超过50位！
			return ErrCodeConstants.API_MAIL_LEN_TOLONG_IN50;
		}
		return "";
	}
	/**
	 * 发送邮件接口(email找回密码发送验证码)
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet sendEmail(CommandData input,SelvetContext<ApiContext> context){
		CommandRet l_ret = new CommandRet("");
		String email=input.getParm("email").getStringColumn();
		String userId=input.getParm("userId").getStringColumn();
		if("".equals(email)){
			//error desc 邮件地址不能为空！
			l_ret.setError(ErrCodeConstants.API_NULL_EMAIL_ADDRESS,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_EMAIL_ADDRESS, ApiServletHolder.getApiContext().getLanguage()));
			return l_ret;
		}	
			//获取验证码有效时间后台中有邮箱校验
			CommandInput commandInputGetTime = new CommandInput("com.cares.sh.order.config.query");
			commandInputGetTime.addParm("type", "COMMON");
			commandInputGetTime.addParm("code", "VerificationCodeExpiry");
			l_ret = context.doOther(commandInputGetTime, false);
			String vCodeExpiry = l_ret.getParm("configs").getTableColumn().getRow(0).getColumn("data").getStringColumn();
			int vCodeExpiryTime=Integer.parseInt(vCodeExpiry)*60;
			//生成六位数验证码
			String emailVC = autoGenerationVC();
			Map<String, String> map=new HashMap<>();
			map.put("emailVC", emailVC);
			//String time = new ConfigurationManager().getAppCacheValue("xxx",APICacheHelper.APP_TYPE_COMMON);
			//int time = Integer.parseInt(sTime);
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_forgetpwd.toKey("vc:")+userId, emailVC, vCodeExpiryTime);
			//发送邮件接口
			CommandInput commandInput = new CommandInput("com.travelsky.quick.member.pushVerificationCode");
			commandInput.addParm("contactvalue", email);
			commandInput.addParm("verificationCode", emailVC);
			commandInput.addParm("expiryTime",Integer.parseInt(vCodeExpiry));
			commandInput.addParm("idno",userId);
			commandInput.addParm("idtype",EMAIL);
			commandInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			l_ret = context.doOther(commandInput, false);
			
		
		if (l_ret.isError()) {
			l_ret.setError(l_ret.getErrorCode(), l_ret.getErrorDesc());
			return l_ret;
		}
		//对B2C访问次数进行计数（XML请求/JSON请求）
//		if (l_ret.getErrorCode().equals("")||l_ret.getErrorCode()==null) {
//			String type=SERVICE;
//			RedisUtil redisUtil =new RedisUtil();
//			redisUtil.docount(type, context);
//		}
		return l_ret;
	}
	/**
	 * @Title:autoGenerationVC
	 * @description:自动生成6位数字的随机数
	 * @return String 6位数字的随机数
	 */
	public static String autoGenerationVC(){
		StringBuffer codeBuffer = new StringBuffer();
		for (int i = 0; i < 6; i++) {
			codeBuffer.append(Math.round(Math.random() * 9));
		}
	    return codeBuffer.toString();
	}
}
