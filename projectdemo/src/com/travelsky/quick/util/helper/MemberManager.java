package com.travelsky.quick.util.helper;

import java.awt.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cares.sh.comm.CommandInputRet;
import com.cares.sh.comm.JsonUnit;
import com.cares.sh.comm.SelvetContext;
import com.cares.sh.comm.SystemConfig;
import com.cares.sh.comm.Unit;
import com.cares.sh.constant.RedisNamespaceEnum;
import com.cares.sh.parm.CommandData;
import com.cares.sh.parm.CommandInput;
import com.cares.sh.parm.CommandRet;
import com.cares.sh.parm.Item;
import com.cares.sh.parm.Row;
import com.cares.sh.parm.Table;
import com.cares.sh.redis.RedisManager;
import com.travelsky.quick.common.ApiContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.framework.util.MergeableCallWorker;
import com.travelsky.quick.util.DateUtils;
import com.travelsky.quick.util.RedisUtil;
import com.travelsky.quick.util.validate.BaseValidate;
import com.travelsky.quick.util.validate.BaseValidator;

public class MemberManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MemberManager.class);
	// public static final String REG_MOBILE = "^[1][0-9]{10}$";
	public static final String REG_MOBILE = "^\\d+$";
	public static final String REG_EMAIL = "^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$";
	public static final String REG_CHINESE = "[\u4E00-\u9FA5]{1,35}";
	public static final String REG_ENGLISH = "[a-zA-Z]{1,35}";
	public static final String REG_INT = "^[1-9]\\d*|0$";
	// public static final String REG_PWD = "^[a-zA-Z0-9]{6,16}$";
	// public static final String REG_PWD =
	// "^(?=.*[a-z])(?=.*\\d+)[a-zA-Z\\d!@#$%^&&&*\\(\\)]{8,16}$";
	public static final String REG_PWD = "^(?=.{8,})(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]){0,}.{8,16}$";
	public static final String REG_ZIPCODE = "^[0-9]{6}$";
	public static final String SERVICE = "SERVICE";
	public static final String EMAIL = "email";
	public static final String TWO_THOUSAND_TWELVE = "2012";

	/**
	 * config.ini里的节点名称
	 */
	public static final String SYS_APP_KEY = "SYS_APP";

	private static String language = ApiServletHolder.getApiContext().getLanguage();

	/**
	 * 获取会员信息接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet getMember(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();

		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.view");
		lInput.addParm("id", memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		if (!ret.isError()) {
			// 获取地址信息
			Table address = ret.getParm("address").getTableColumn();
			if (null != address) {
				address = address.copy(new String[] { "id", "province", "city", "zipcode", "address" });

			} else {
				address = new Table(new String[] { "id", "province", "city", "zipcode", "address" });
			}
			// 获取证件信息
			Table identity = ret.getParm("identity").getTableColumn();
			if (null != identity) {
				identity = identity.copy(new String[] { "id", "idtype", "idno", "expiry", "issuecountry", "status" });
			} else {
				identity = new Table(new String[] { "id", "idtype", "idno", "expiry", "issuecountry", "status" });
			}
			// 获取联系信息
			Table binding = ret.getParm("binding").getTableColumn();
			String[] columns = new String[] { "id", "type", "no", "status", "area" };
			Table contactlist = new Table(columns);
			String str = "";
			if (null != binding) {
				for (int i = 0; i < binding.getRowCount(); i++) {
					Row lRow = binding.getRow(i);
					Row lNewrow = new Row(contactlist);
					for (String col : columns) {
						str = col;
						if ("type".equalsIgnoreCase(col)) {
							str = "bindtype";
						}
						if ("no".equalsIgnoreCase(col)) {
							str = "bindingvalue";
						}
						Item lItem = lRow.getColumn(str);
						if (lItem.isObject()) {
							lNewrow.addColumn(col, lItem.copy().getObjectColumn());
						} else if (lItem.isTable()) {
							lNewrow.addColumn(col, lItem.copy().getTableColumn());
						} else {
							lNewrow.addColumn(col, lItem.getStringColumn());
						}
					}
					contactlist.appRow(lNewrow);
				}
			}
			CommandRet lNewret = new CommandRet("");
			lNewret.setError(ret.getErrorCode(), ret.getErrorDesc());
			lNewret.addParm("userid", ret.getParm("memberid").getStringColumn());
			lNewret.addParm("username", ret.getParm("userid").getStringColumn());
			lNewret.addParm("lastname", ret.getParm("lastname").getStringColumn());
			lNewret.addParm("firstname", ret.getParm("firstname").getStringColumn());
			lNewret.addParm("pinyinxing", ret.getParm("pinyinxing").getStringColumn());
			lNewret.addParm("pinyinming", ret.getParm("pinyinming").getStringColumn());
			lNewret.addParm("gender", ret.getParm("gender").getStringColumn());
			lNewret.addParm("birthday", ret.getParm("birthday").getStringColumn());
			lNewret.addParm("promotionemail", ret.getParm("promotionemail").getStringColumn());
			lNewret.addParm("name", ret.getParm("name").getStringColumn());
			lNewret.addParm("idlist", identity);
			lNewret.addParm("addresslist", address);
			lNewret.addParm("contactlist", contactlist);
			// String languageCode =
			// ret.getParm("languageCode").getStringColumn();
			// if (null!=languageCode&&!"".equals(languageCode)) {
			// lNewret.addParm("languageCode",
			// ret.getParm("languageCode").getStringColumn());
			// }
			return lNewret;
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 获取会员信息接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet getMemberEmail(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();

		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.view");
		lInput.addParm("id", memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		if (!ret.isError()) {
			// 获取地址信息
			Table address = ret.getParm("address").getTableColumn();
			if (null != address) {
				address = address.copy(new String[] { "id", "province", "city", "zipcode", "address" });

			} else {
				address = new Table(new String[] { "id", "province", "city", "zipcode", "address" });
			}
			// 获取证件信息
			Table identity = ret.getParm("identity").getTableColumn();
			if (null != identity) {
				identity = identity.copy(new String[] { "id", "idtype", "idno", "expiry", "issuecountry", "status" });
			} else {
				identity = new Table(new String[] { "id", "idtype", "idno", "expiry", "issuecountry", "status" });
			}
			// 获取联系信息
			Table binding = ret.getParm("contact").getTableColumn();
			String[] columns = new String[] { "id", "type", "no", "status", "area" };
			Table contactlist = new Table(columns);
			String str = "";
			if (null != binding) {
				for (int i = 0; i < binding.getRowCount(); i++) {
					Row lRow = binding.getRow(i);
					Row lNewrow = new Row(contactlist);
					lNewrow.addColumn("id", lRow.getColumn("id"));
					lNewrow.addColumn("type", lRow.getColumn("contacttype"));
					lNewrow.addColumn("no", lRow.getColumn("contactvalue"));
					lNewrow.addColumn("status", lRow.getColumn("status"));
					lNewrow.addColumn("area", lRow.getColumn("regioncode"));
					contactlist.appRow(lNewrow);
				}
			}
			CommandRet lNewret = new CommandRet("");
			lNewret.setError(ret.getErrorCode(), ret.getErrorDesc());
			lNewret.addParm("userid", ret.getParm("memberid").getStringColumn());
			lNewret.addParm("username", ret.getParm("userid").getStringColumn());
			lNewret.addParm("lastname", ret.getParm("lastname").getStringColumn());
			lNewret.addParm("firstname", ret.getParm("firstname").getStringColumn());
			lNewret.addParm("pinyinxing", ret.getParm("pinyinxing").getStringColumn());
			lNewret.addParm("pinyinming", ret.getParm("pinyinming").getStringColumn());
			lNewret.addParm("gender", ret.getParm("gender").getStringColumn());
			lNewret.addParm("birthday", ret.getParm("birthday").getStringColumn());
			lNewret.addParm("email", ret.getParm("email").getStringColumn());
			lNewret.addParm("nationality", ret.getParm("nationality").getStringColumn());
			lNewret.addParm("languageCode",
					ret.getParm("preference").getObjectColumn().getParm("preflang").getStringColumn());
			lNewret.addParm("name", ret.getParm("name").getStringColumn());
			lNewret.addParm("idlist", identity);
			lNewret.addParm("addresslist", address);
			lNewret.addParm("contactlist", contactlist);
			// String languageCode =
			// ret.getParm("languageCode").getStringColumn();
			// if (null!=languageCode&&!"".equals(languageCode)) {
			// lNewret.addParm("languageCode",
			// ret.getParm("languageCode").getStringColumn());
			// }
			return lNewret;
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员登陆接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet login(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String id = input.getParm("username").getStringColumn();
		BaseValidate validator = new BaseValidator(id, null, null, null, null, null, null, null, null,
				ApiServletHolder.getApiContext().getClientIP());
		try {
			// 检查黑名单与异常用户
			validator.checkBlackList();
			// 检查用户状态
			validator.checkUserStatus();
		} catch (APIException e) {
			// error desc 用户名不能为空!
			ret.setError(e.getErrorCode(), TipMessager.getMessage(e.getErrorCode(), language));
			return ret;
		}

		if ("".equals(id)) {
			// error desc 用户名不能为空!
			ret.setError(ErrCodeConstants.API_NULL_USER_NAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_NAME, language));
			return ret;
		}
		String password = input.getParm("password").getStringColumn();
		if ("".equals(password)) {
			// error desc 密码不能为空!
			ret.setError(ErrCodeConstants.API_NULL_USER_PASSWORD,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_PASSWORD, language));
			return ret;
		}
		// 获取基本信息
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.login");
		// email登陆入参设置
		String actionType = input.getParm("actionType").getStringColumn();
		if ("USER_LOGIN_EMAIL".equals(actionType)) {
			if (!Pattern.matches(REG_EMAIL, id)) {
				// error desc 登陆邮箱号不符合规范！
				ret.setError(ErrCodeConstants.API_EMAIL_REG_ERROR,
						TipMessager.getMessage(ErrCodeConstants.API_EMAIL_REG_ERROR, language));
				return ret;
			}
			if (checkEMLen(id)) {
				// 登陆邮箱长度过长
				ret.setError(ErrCodeConstants.API_EMAIL_TOOLONG_IN50,
						TipMessager.getMessage(ErrCodeConstants.API_EMAIL_TOOLONG_IN50, language));
				return ret;
			}
			lInput.addParm("regNo", id);
			lInput.addParm("regType", EMAIL);
		}
		lInput.addParm("id", id);
		lInput.addParm("password", password);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		CommandData paxInfoTemplate = ret.getParm("paxInfoTemplate").getObjectColumn();
		if (!ret.isError()) {
			String memberid = ret.getParm("userid").getStringColumn();
			lInput.addParm("memberid", memberid);
			//根据actiontype判断返回的数据是否含语言和国籍。
			if ("USER_LOGIN_EMAIL".equals(actionType)) {
				//email登陆返回数据包含语言字段和国籍
				ret = this.getMemberEmail(lInput, context);
			}else {
				ret=this.getMember(lInput, context);
			}
			RedisManager.getManager().set(RedisNamespaceEnum.api_cache_paxInfotemplate.code(), JsonUnit.toJson(paxInfoTemplate), 0);
			ret.addParm("paxInfoTemplate", paxInfoTemplate);
		}
		//获取相关配置参数值
		CommandInput commandInputGetConfig = new CommandInput("com.cares.sh.order.config.query");
		commandInputGetConfig.addParm("type", "application");
		commandInputGetConfig.addParm("code", "RT_interval_hour");
		CommandRet l_ret = context.doOther(commandInputGetConfig, false);
		String configValue ="";
		if (!l_ret.isError()) {
			configValue = l_ret.getParm("configs").getTableColumn().getRow(0).getColumn("data").getStringColumn();
			if (configValue==null||"".equals(configValue)) {
				configValue="2";
			}
		}else{
			configValue="2";
		}
		 
		 //将configValue放入返回ret中
		 ret.addParm("configValue", configValue);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 获取邮箱列表接口
	 *
	 * @param input
	 * @param context
	 * @return
	 * @throws APIException
	 */
	public CommandRet getEmailList(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		final String id = input.getParm("userId").getStringColumn();
		final String tktdeptid = input.getParm("tktdeptid").getStringColumn();
		BaseValidate validator = new BaseValidator(id, null, null, null, null, null, null, null, null,
				ApiServletHolder.getApiContext().getClientIP());
		try {
			// 检查黑名单与异常用户
			validator.checkBlackList();
			// 检查用户状态
			validator.checkUserStatus();
		} catch (APIException e) {
			// error desc 用户名不能为空!
			ret.setError(e.getErrorCode(), TipMessager.getMessage(e.getErrorCode(), language));
			return ret;
		}

		if ("".equals(id)) {
			// error desc 用户名不能为空!
			ret.setError(ErrCodeConstants.API_NULL_USER_NAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_NAME, language));
			return ret;
		}
		// 拼装key值
		String redisKey = RedisNamespaceEnum.api_service_emaillist.toKey(id);
		// 从redis中获取缓存数据
		String redisValue = RedisManager.getManager().get(redisKey);
		if (!"".equals(redisValue) && redisValue != null) {
			ret.addParm("LCC_QUERYEMAILLIST_SERVICE", redisValue);
			return ret;
		} else {
			final SelvetContext<ApiContext> mergeableContext = context;
			MergeableCallWorker<CommandRet> mcw = new MergeableCallWorker<CommandRet>(redisKey, new Callable<CommandRet>() {
				@Override
				public CommandRet call() throws Exception {
					CommandRet result = new CommandRet("");
					CommandInput lInput = new CommandInput("com.travelsky.quick.member.contact.list");
					lInput.addParm("idno", id);
					lInput.addParm("idtype", EMAIL);
					lInput.addParm("ticketdeptid", tktdeptid);
					result = mergeableContext.doOther(lInput, false);
					return result;
				}
			});
			 CommandRet resultNoThrowErr = mcw.getResultNoThrowErr();
			return resultNoThrowErr;
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
	}

	/**
	 * 会员注册接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet register(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String userid = input.getParm("username").getStringColumn();
		String password = input.getParm("password").getStringColumn();
		String lastname = input.getParm("lastname").getStringColumn();
		String firstname = input.getParm("firstname").getStringColumn();
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.register");
		if ("".equals(userid)) {
			// error desc 用户名不能为空!
			ret.setError(ErrCodeConstants.API_NULL_USER_NAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_NAME, language));
			return ret;
		}
		if ("".equals(password)) {
			// error desc 密码不能为空!
			ret.setError(ErrCodeConstants.API_NULL_USER_PASSWORD,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_PASSWORD, language));
			return ret;
		} else if (!Pattern.matches(REG_PWD, password)) {
			// error desc 密码只能是8-16位数字和字母组合！
			ret.setError(ErrCodeConstants.API_USER_PASSWORD_LEN_ASTRICT8_16,
					TipMessager.getMessage(ErrCodeConstants.API_USER_PASSWORD_LEN_ASTRICT8_16, language));
			return ret;
		}
		if ("".equals(lastname)) {
			// error desc 姓不能为空！
			ret.setError(ErrCodeConstants.API_NULL_SURNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_SURNAME, language));
			return ret;
		}
		if ("".equals(firstname)) {
			// error desc 名不能为空！
			ret.setError(ErrCodeConstants.API_NULL_GIVENNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_GIVENNAME, language));
			return ret;
		}
		Table lTab = input.getParm("idlist").getTableColumn();
		if (lTab != null && lTab.getRowCount() > 0) {
			for (Row row : lTab) {
				String idtype = row.getColumn("idtype").getStringColumn();
				if ("".equals(idtype)) {
					// error desc 证件类别不能为空！
					ret.setError(ErrCodeConstants.API_NULL_IDTYPE,
							TipMessager.getMessage(ErrCodeConstants.API_NULL_IDTYPE, language));
					return ret;
				}
				// input.addParm("idtype", idtype);
				String idno = row.getColumn("idno").getStringColumn();
				if ("".equals(idno)) {
					// error desc 证件号不能为空！
					ret.setError(ErrCodeConstants.API_NULL_IDNO,
							TipMessager.getMessage(ErrCodeConstants.API_NULL_IDNO, language));
					return ret;
				}
				if (idtype.equals("PP")) {
					String expiryDate = row.getColumn("expiryDate").getStringColumn();
					if (expiryDate != null && !"".equals(expiryDate)) {
						boolean flag = DateUtils.formatDate(expiryDate);
						if (flag) {
							ret.setError(ErrCodeConstants.API_FORMATDATE,
									TipMessager.getMessage(ErrCodeConstants.API_FORMATDATE, language));
							return ret;
						}
					}
				}
				String msg = checkID(input, idno.toUpperCase(), idtype);
				if (StringUtils.hasLength(msg)) {
					ret.setError(msg, TipMessager.getMessage(msg, language));
					return ret;
				}
				row.addColumn("idno", idno.toUpperCase());
				input.addParm("idno", idno.toUpperCase());
			}
		} else {
			// error desc 证件信息不能为空！
			ret.setError(ErrCodeConstants.API_NULL_IDINFO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_IDINFO, language));
			return ret;
		}

		String gender = input.getParm("gender").getStringColumn();
		if ("".equals(gender)) {
			// error desc 性别不能为空！
			ret.setError(ErrCodeConstants.API_NULL_GENDER,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_GENDER, language));
			return ret;
		} else if (!"F".equalsIgnoreCase(gender) && !"M".equalsIgnoreCase(gender)) {
			// error desc 性别信息有误！
			ret.setError(ErrCodeConstants.API_USER_GENDER_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_USER_GENDER_ERROR, language));
			return ret;
		}
		String mobile = input.getParm("mobile").getStringColumn();
		if ("".equals(mobile)) {
			// error desc 手机号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MOBILE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MOBILE, language));
			return ret;
		}
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		// 验证短信验证码
		String smscode = input.getParm("smscode").getStringColumn();
		/*
		 * test label是提供自动化测试使用. 请不要注释！！。可以将config.ini 配置项 DEBUGMODE ISDEBUG
		 * 改为其他值
		 */
		test: {
			LOGGER.debug("smscode:{}", smscode);
			if ("000000".equals(smscode)
					&& "true".equalsIgnoreCase(SystemConfig.getConfig().getItemString("DEBUGMODE", "ISDEBUG"))) {
				break test;
			}
			if ("".equals(smscode)) {
				// error desc 手机验证码不能为空！
				ret.setError(ErrCodeConstants.API_NULL_VALIDATE_CODE,
						TipMessager.getMessage(ErrCodeConstants.API_NULL_VALIDATE_CODE, language));
				return ret;
			}
			String redisSmsCode = RedisManager.getManager().get("REGISTER_SEND_SMS" + mobile);
			Calendar curDate = Calendar.getInstance();
			Calendar tommorowDate = new GregorianCalendar(curDate.get(Calendar.YEAR), curDate.get(Calendar.MONTH),
					curDate.get(Calendar.DATE) + 1, 0, 0, 0);
			// 从当前时间到凌晨零点，剩余秒数
			int delayTime = (int) (tommorowDate.getTimeInMillis() - curDate.getTimeInMillis()) / 1000;
			// 测试发现 业务有问题 暂时把验证短信验证码的功能关掉
			if ("".equals(redisSmsCode) || redisSmsCode == null) {
				// error desc 手机验证码失效,请重试！
				ret.setError(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY,
						TipMessager.getMessage(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY, language));
				// 增加验证次数一次
				String count = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_registersendsms.toKey("count") + mobile);
				if ("".equals(count) || count == null) {
					RedisManager.getManager().set(RedisNamespaceEnum.api_cache_registersendsms.toKey("count") + mobile, "1", delayTime - 1);
				} else {
					int countCurrent = Integer.parseInt(count);
					if (countCurrent >= 5) {
						// error desc 手机验证失效且已重试5次，该手机号今天不能再注册，请明天重试！
						ret.setError(ErrCodeConstants.API_SMSVFCODE_LOSE5_TD_UNREGISTER_RETRYTM, TipMessager
								.getMessage(ErrCodeConstants.API_SMSVFCODE_LOSE5_TD_UNREGISTER_RETRYTM, language));
						return ret;
					}
					int couti = Integer.parseInt(count);
					RedisManager.getManager().set(RedisNamespaceEnum.api_cache_registersendsms.toKey("count") + mobile, "" + (couti + 1), delayTime - 1);
				}
				return ret;
			}
			if (!smscode.trim().toUpperCase().equals(redisSmsCode.trim().toUpperCase())) {
				// 短信验证码错误，清除redis内缓存值
				// RedisManager.getManager().del("REGISTER_SEND_SMS" + mobile);
				// error desc 验证码不正确,请重新获取并输入正确验证码！
				ret.setError(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT,
						TipMessager.getMessage(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT, language));
				// 增加验证次数一次
				String count = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_registersendsms.toKey("count") + mobile);
				if ("".equals(count) || count == null) {
					RedisManager.getManager().set(RedisNamespaceEnum.api_cache_registersendsms.toKey("count") + mobile, "1", delayTime - 1);
				} else {
					int countCurrent = Integer.parseInt(count);
					if (countCurrent >= 5) {
						// error desc 手机验证码错误且已重试5次，该手机号今天不能再注册，请明天重试！
						ret.setError(ErrCodeConstants.API_SMSVFCODE_ERROR5_TD_UNREGISTER_RETRYTM, TipMessager
								.getMessage(ErrCodeConstants.API_SMSVFCODE_ERROR5_TD_UNREGISTER_RETRYTM, language));
						return ret;
					}
					int couti = Integer.parseInt(count);
					RedisManager.getManager().set(RedisNamespaceEnum.api_cache_registersendsms.toKey("count") + mobile, "" + (couti + 1), delayTime - 1);
				}

				return ret;
			}
		}
		// 短信验证码校验通过，清除redis内缓存值
		RedisManager.getManager().del("REGISTER_SEND_SMS" + mobile);
		lInput.addParm("userid", userid);
		lInput.addParm("password", password);
		lInput.addParm("lastname", lastname);
		lInput.addParm("firstname", firstname);
		lInput.addParm("idlist", lTab);
		lInput.addParm("gender", gender);
		// lInput.addParm("pinyinxing",
		// input.getParm("pinyinxing").getStringColumn());
		// lInput.addParm("pinyinming",
		// input.getParm("pinyinming").getStringColumn());
		lInput.addParm("areaCode", input.getParm("areaCode").getStringColumn());
		lInput.addParm("birthday", input.getParm("birthday").getStringColumn());
		lInput.addParm("mobile", input.getParm("mobile").getStringColumn());
		lInput.addParm("email", input.getParm("email").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lInput.addParm("nationality", input.getParm("nationality").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员email注册接口
	 * 
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet registerEmail(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String userId = input.getParm("userId").getStringColumn();
		String password = input.getParm("password").getStringColumn();
		String lastname = input.getParm("lastname").getStringColumn();
		String firstname = input.getParm("firstname").getStringColumn();
		String actionType = input.getParm("actionType").getStringColumn();
		String regType = "";
		if ("USER_REGISTER_EAMIL".equals(actionType)) {
			regType = EMAIL;
		}
		String languageCode = input.getParm("languageCode").getStringColumn();
		CommandInput lInput = new CommandInput("com.travelsky.quick.member.register");
		if ("".equals(userId)) {
			// error desc userId不能为空!
			ret.setError(ErrCodeConstants.API_NULL_USER_EMAIL,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_EMAIL, language));
			return ret;
		}
		if (checkEMLen(userId)) {
			// 邮箱长度过长
			ret.setError(ErrCodeConstants.API_EMAIL_TOOLONG_IN50,
					TipMessager.getMessage(ErrCodeConstants.API_EMAIL_TOOLONG_IN50, language));
			return ret;
		}
		if (!Pattern.matches(REG_EMAIL, userId)) {
			// error desc 邮箱号不符合规范！
			ret.setError(ErrCodeConstants.API_EMAIL_REG_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_EMAIL_REG_ERROR, language));
			return ret;
		}
		if ("".equals(password)) {
			// error desc 密码不能为空!
			ret.setError(ErrCodeConstants.API_NULL_USER_PASSWORD,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_PASSWORD, language));
			return ret;
		} else if (!Pattern.matches(REG_PWD, password)) {
			// error desc 密码只能是8-16位数字和字母组合！
			ret.setError(ErrCodeConstants.API_USER_PASSWORD_LEN_ASTRICT8_16,
					TipMessager.getMessage(ErrCodeConstants.API_USER_PASSWORD_LEN_ASTRICT8_16, language));
			return ret;
		}
		if ("".equals(lastname)) {
			// error desc 姓不能为空！
			ret.setError(ErrCodeConstants.API_NULL_SURNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_SURNAME, language));
			return ret;
		}
		if ("".equals(firstname)) {
			// error desc 名不能为空！
			ret.setError(ErrCodeConstants.API_NULL_GIVENNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_GIVENNAME, language));
			return ret;
		}
		Table lTab = input.getParm("idlist").getTableColumn();
		if (lTab != null && lTab.getRowCount() > 0) {
			for (Row row : lTab) {
				String idtype = row.getColumn("type").getStringColumn();
				if ("".equals(idtype)) {
					// error desc 证件类别不能为空！
					ret.setError(ErrCodeConstants.API_NULL_IDTYPE,
							TipMessager.getMessage(ErrCodeConstants.API_NULL_IDTYPE, language));
					return ret;
				}
				// input.addParm("idtype", idtype);
				String idno = row.getColumn("no").getStringColumn();
				if ("".equals(idno)) {
					// error desc 证件号不能为空！
					ret.setError(ErrCodeConstants.API_NULL_IDNO,
							TipMessager.getMessage(ErrCodeConstants.API_NULL_IDNO, language));
					return ret;
				}
				if (idtype.equals("PP")) {
					String expiryDate = row.getColumn("expiryDate").getStringColumn();
					if (expiryDate != null && !"".equals(expiryDate)) {
						boolean flag = DateUtils.formatDate(expiryDate);
						if (flag) {
							ret.setError(ErrCodeConstants.API_FORMATDATE,
									TipMessager.getMessage(ErrCodeConstants.API_FORMATDATE, language));
							return ret;
						}
					}
				}
				String msg = checkID(input, idno.toUpperCase(), idtype);
				if (StringUtils.hasLength(msg)) {
					ret.setError(msg, TipMessager.getMessage(msg, language));
					return ret;
				}
			}
		}
		// 校验联系号码格式
		Table contactTab = input.getParm("contact").getTableColumn();
		if (contactTab != null && contactTab.getRowCount() > 0) {
			for (Row row : contactTab) {
				String contactType = row.getColumn("type").getStringColumn();
				String contactNo = row.getColumn("no").getStringColumn();
				if ("email".equals(contactType)) {
					if ("".equals(contactNo)) {
						// error desc 邮箱号不能为空！
						ret.setError(ErrCodeConstants.API_NULL_USER_EMAIL,
								TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_EMAIL, language));
						return ret;
					} else if (!Pattern.matches(REG_EMAIL, contactNo)) {
						// error desc 邮箱号不符合规范！
						ret.setError(ErrCodeConstants.API_EMAIL_REG_ERROR,
								TipMessager.getMessage(ErrCodeConstants.API_EMAIL_REG_ERROR, language));
						return ret;
					} else if (checkEMLen(contactNo)) {
						// 邮箱长度过长
						ret.setError(ErrCodeConstants.API_EMAIL_TOOLONG_IN50,
								TipMessager.getMessage(ErrCodeConstants.API_EMAIL_TOOLONG_IN50, language));
						return ret;
					}
				} else if ("mobile".equals(contactType)) {
					if ("".equals(contactNo)) {
						// error desc 手机号不能为空！
						ret.setError(ErrCodeConstants.API_NULL_MOBILE,
								TipMessager.getMessage(ErrCodeConstants.API_NULL_MOBILE, language));
						return ret;
					} else if (!Pattern.matches(REG_MOBILE, contactNo)) {
						// error desc 手机号不符合规范！
						ret.setError(ErrCodeConstants.API_MOBILE_REG_ERROR,
								TipMessager.getMessage(ErrCodeConstants.API_MOBILE_REG_ERROR, language));
						return ret;
					}
				}
			}
		}
		// 添加语言类型
		CommandData data = new CommandData();
		data.addParm("ori", "");
		data.addParm("dest", "");
		data.addParm("cabin", "");
		data.addParm("seat", "");
		data.addParm("meal", "");
		data.addParm("lang", languageCode);
		// 注册请求必填
		lInput.addParm("preference", data);
		lInput.addParm("regNo", userId);
		lInput.addParm("password", password);
		lInput.addParm("lastname", lastname);
		lInput.addParm("firstname", firstname);
		// 注册请求非必填
		lInput.addParm("idlist", lTab);
		lInput.addParm("gender", input.getParm("gender").getStringColumn());
		lInput.addParm("regType", regType);
		lInput.addParm("birthday", input.getParm("birthday").getStringColumn());
		lInput.addParm("contact", input.getParm("contact").getTableColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lInput.addParm("nationality", input.getParm("nationality").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		// 注册成功后删除邮箱列表中的缓存数据
		if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
			String redisKey = RedisNamespaceEnum.api_service_emaillist.toKey(userId);
			RedisManager.getManager().del(redisKey);
			RedisManager.getManager().del(redisKey + "_json");
		}
		return ret;
	}

	/**
	 * 会员信息修改接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updMember(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String lastname = input.getParm("lastname").getStringColumn();
		if ("".equals(lastname)) {
			// error desc 姓不能为空！
			ret.setError(ErrCodeConstants.API_NULL_SURNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_SURNAME, language));
			return ret;
		}
		String firstname = input.getParm("firstname").getStringColumn();
		if ("".equals(firstname)) {
			// error desc 名不能为空！
			ret.setError(ErrCodeConstants.API_NULL_GIVENNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_GIVENNAME, language));
			return ret;
		}
		String pinyinxing = input.getParm("pinyinxing").getStringColumn();
		if ("".equals(pinyinxing)) {
			// error desc 拼音姓不能为空！
			ret.setError(ErrCodeConstants.API_NULL_SPELLSURNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_SPELLSURNAME, language));
			return ret;
		} else if (!Pattern.matches(REG_ENGLISH, pinyinxing)) {
			// error desc 拼音姓为大小写字母组合，长度不能超过20位！
			ret.setError(ErrCodeConstants.API_SPELLSURNAME_UPLOWCASE_LEN_ASTRICT_IN20,
					TipMessager.getMessage(ErrCodeConstants.API_SPELLSURNAME_UPLOWCASE_LEN_ASTRICT_IN20, language));
			return ret;
		}
		String pinyinming = input.getParm("pinyinming").getStringColumn();
		if ("".equals(pinyinming)) {
			// error desc 拼音名不能为空！
			ret.setError(ErrCodeConstants.API_NULL_SPELLGIVENNAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_SPELLGIVENNAME, language));
			return ret;
		} else if (!Pattern.matches(REG_ENGLISH, pinyinming)) {
			// error desc 拼音名为大小写字母组合，长度不能超过20位！
			ret.setError(ErrCodeConstants.API_SPELLGIVENNAME_UPLOWCASE_LEN_ASTRICT_IN20,
					TipMessager.getMessage(ErrCodeConstants.API_SPELLGIVENNAME_UPLOWCASE_LEN_ASTRICT_IN20, language));
			return ret;
		}
		String gender = input.getParm("gender").getStringColumn();
		if ("".equals(gender)) {
			// error desc 性别不能为空！
			ret.setError(ErrCodeConstants.API_NULL_GENDER,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_GENDER, language));
			return ret;
		} else if (!"F".equalsIgnoreCase(gender) && !"M".equalsIgnoreCase(gender)) {
			// error desc 性别信息有误！
			ret.setError(ErrCodeConstants.API_USER_GENDER_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_USER_GENDER_ERROR, language));
			return ret;
		}
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String actionType = input.getParm("actionType").getStringColumn();
		String languageCode = "";
		// 修改用户偏好
		if ("MODIFY_USER_EMAIL".equals(actionType)) {
			CommandRet Eret = new CommandRet("");
			CommandInput lInput = new CommandInput("com.cares.sh.order.member.preference.modify");
			languageCode = input.getParm("languageCode").getStringColumn();
			if (!StringUtils.hasLength(languageCode)) {
				Eret.setError(ErrCodeConstants.API_NULL_LANGUAGE,
						TipMessager.getMessage(ErrCodeConstants.API_NULL_LANGUAGE, language));
				return Eret;
			}
			lInput.addParm("preflang", input.getParm("languageCode").getStringColumn());
			lInput.addParm("memberid", memberid);
			Eret = context.doOther(lInput, false);
		}
		// 修改用户信息
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.modify");
		lInput.addParm("memberid", memberid);
		lInput.addParm("lastname", lastname);
		lInput.addParm("firstname", firstname);
		lInput.addParm("gender", input.getParm("gender").getStringColumn());
		lInput.addParm("pinyinxing", pinyinxing);
		lInput.addParm("pinyinming", pinyinming);
		lInput.addParm("birthday", input.getParm("birthday").getStringColumn());
		lInput.addParm("promotionemail", input.getParm("promotionemail").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 修改用户信息的同时修改其证件号的信息
		// 获取用户的信息
		CommandRet Uret = new CommandRet("");
		CommandInput UInput = new CommandInput("com.cares.sh.order.member.view");
		UInput.addParm("id", memberid);
		UInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		Uret = context.doOther(UInput, false);
		// 修改证件
		Table identity = Uret.getParm("identity").getTableColumn();
		if (null != identity) {
			for (Row row : identity) {
				CommandRet Iret = new CommandRet("");
				CommandInput IDInput = new CommandInput("com.cares.sh.order.member.identity.modify");
				// 添加用户信息
				IDInput.addParm("lastname", lastname);
				IDInput.addParm("firstname", firstname);
				IDInput.addParm("gender", input.getParm("gender").getStringColumn());
				IDInput.addParm("birthday", input.getParm("birthday").getStringColumn());
				// 添加证件信息
				IDInput.addParm("expiry", row.getColumn("expiry").getStringColumn().substring(0, 10));
				IDInput.addParm("memberid", memberid);
				IDInput.addParm("id", row.getColumn("id").getStringColumn());
				IDInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
				Iret = context.doOther(IDInput, false);
			}
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息修改接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updateMember(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");

		// 校验用户信息
		String msg = checkUserInfo(input);
		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.modifyall");
		input.copyTo(lInput);
		ret = context.doOther(lInput, false);
		return ret;
	}

	public String checkUserInfo(CommandData input) {
		// 校验用户信息
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			return ErrCodeConstants.API_NULL_MEMBER_ID;
		}
		String lastname = input.getParm("lastname").getStringColumn();
		if ("".equals(lastname)) {
			// error desc 姓不能为空！
			return ErrCodeConstants.API_NULL_SURNAME;
		}
		String firstname = input.getParm("firstname").getStringColumn();
		if ("".equals(firstname)) {
			// error desc 名不能为空！
			return ErrCodeConstants.API_NULL_GIVENNAME;
		}
		if (!"".equals(lastname)) {
			if (!Pattern.matches(REG_CHINESE, lastname)) {
				if (!Pattern.matches(REG_ENGLISH, lastname)) {
					// 姓不符合规范,应长度不超过35，且为字母！
					return ErrCodeConstants.API_PAX_LASTNAME_ERROR;
				}
			}
		}
		if (!"".equals(firstname)) {
			if (!Pattern.matches(REG_CHINESE, firstname)) {
				if (!Pattern.matches(REG_ENGLISH, firstname)) {
					// 名不符合规范,应长度不超过35，且为字母！
					return ErrCodeConstants.API_PAX_FIRSTNAME_ERROR;
				}
			}
		}
		// 校验用户证件号信息
		Table tableidlist = input.getParm("idlist").getTableColumn();
		if (tableidlist.getRowCount() > 0) {
			for (Row row : tableidlist) {
				String idType = row.getColumn("idtype").getStringColumn();
				String idNo = row.getColumn("idno").getStringColumn();
				if (!"".equals(idType)) {
//					if (!"NI".equalsIgnoreCase(idType) && !"PP".equalsIgnoreCase(idType)
//							&& !"OT".equalsIgnoreCase(idType)) {
//						// 证件类型信息有误！
//						return ErrCodeConstants.API_IDENTITY_TYPE_ERROR;
//					}
					if ("NI".equalsIgnoreCase(idType)) {
						if (!Unit.getValidIdCard(idNo)) {
							// 身份证号不符合规范！
							return ErrCodeConstants.API_IDENTITY_NO_REG_ERROR;
						}
						Item birthdayItem = input.getParm("birthday");
						String birthday = birthdayItem == null ? "" : birthdayItem.getStringColumn();
						if (!"".equals(birthday)) {
							if (!Unit.getDate(idNo.substring(6, 14)).equals(Unit.getDate(birthday))) {
								// 生日与身份证号不符！
								return ErrCodeConstants.API_PAX_PASSNOBIRTH_ERROR;
							}
						}
					} else if ("PP".equalsIgnoreCase(idType)) {
						if (idNo.length() < 5 || idNo.length() > 12) {
							return ErrCodeConstants.API_CHECK_LENGTH_IDNO_ERROR;
						}
					} else {
						if (idNo.length() > 50) {
							return ErrCodeConstants.API_CHECK_LENGTH_IDNO_ERROR;
						}
					}
				}
			}

		}
		// 校验用户地址信息
		String address = input.getParm("address").getStringColumn();
		if (address.length() > 200) {
			// error desc 详细地址过长，应不超过200！
			return ErrCodeConstants.API_ADDRESS_STREETNMBR_TOOLONG_LEN_IN200;
		}
		// 校验用户联系信息
		Table tablecon = input.getParm("contactlist").getTableColumn();
		if (tablecon.getRowCount() > 0) {
			for (Row row : tablecon) {
				String type = row.getColumn("type").getStringColumn();
				String no = row.getColumn("no").getStringColumn();
				String area = row.getColumn("area").getStringColumn();
				if ("email".equals(type)) {
					if ("".equals(no)) {
						// error desc 邮箱号不能为空！
						return ErrCodeConstants.API_NULL_USER_EMAIL;
					} else if (!Pattern.matches(REG_EMAIL, no)) {
						// error desc 邮箱号不符合规范！
						return ErrCodeConstants.API_EMAIL_REG_ERROR;
					} else if (checkEMLen(no)) {
						// 邮箱长度过长
						return ErrCodeConstants.API_EMAIL_TOOLONG_IN50;
					}
				} else if ("mobile".equals(type)) {
					if ("".equals(no)) {
						// error desc 手机号不能为空！
						return ErrCodeConstants.API_NULL_MOBILE;
					} else if (!Pattern.matches(REG_MOBILE, no)) {
						// error desc 手机号不符合规范！
						return ErrCodeConstants.API_MOBILE_REG_ERROR;
					}else if ("".equals(area)) {
						//error  国际区号不能为空
						return ErrCodeConstants.API_NULL_AREACODE;
					}
				}
			}
		}
		// 校验用户偏好信息
		String languageCode = input.getParm("languageCode").getStringColumn();
		if (!StringUtils.hasLength(languageCode)) {
			return ErrCodeConstants.API_NULL_LANGUAGE;
		}
		return "";

	}

	/**
	 * 会员信息增加证件号接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet addID(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String idType = input.getParm("idtype").getStringColumn();
		String idNo = input.getParm("idno").getStringColumn().toUpperCase();
		String expiryDate = input.getParm("expiryDate").getStringColumn();
		msg = checkID(input, idNo, idType);
		if (idType.equals("PP")) {
			if (expiryDate != null && !"".equals(expiryDate)) {
				boolean flag = DateUtils.formatDate(expiryDate);
				if (!flag) {
					ret.setError(ErrCodeConstants.API_FORMATDATE,
							TipMessager.getMessage(ErrCodeConstants.API_FORMATDATE, language));
					return ret;
				}
			}
		}
		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		// 获取用户的信息
		CommandRet Uret = new CommandRet("");
		CommandInput UInput = new CommandInput("com.cares.sh.order.member.view");
		UInput.addParm("id", memberid);
		UInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		Uret = context.doOther(UInput, false);
		// 添加证件
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.identity.add");
		// 添加用户信息
		lInput.addParm("lastname", Uret.getParm("lastname").getStringColumn());
		lInput.addParm("firstname", Uret.getParm("firstname").getStringColumn());
		lInput.addParm("gender", Uret.getParm("gender").getStringColumn());
		lInput.addParm("birthday", Uret.getParm("birthday").getStringColumn().substring(0, 10));
		// 添加证件信息
		lInput.addParm("memberid", memberid);
		lInput.addParm("idtype", input.getParm("idtype").getStringColumn());
		lInput.addParm("idno", input.getParm("idno").getStringColumn().toUpperCase());
		if (input.getParm("idtype").getStringColumn().equals("PP")) {
			lInput.addParm("expiry", input.getParm("expiryDate").getStringColumn());
			lInput.addParm("issuecountry", input.getParm("issueCountry").getStringColumn());
		}
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息修改证件号接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updID(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String idType = input.getParm("idtype").getStringColumn();
		String idNo = input.getParm("idno").getStringColumn().toUpperCase();
		msg = checkID(input, idNo, idType);
		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		// 获取用户的信息
		CommandRet Uret = new CommandRet("");
		CommandInput UInput = new CommandInput("com.cares.sh.order.member.view");
		UInput.addParm("id", memberid);
		UInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		Uret = context.doOther(UInput, false);
		// 修改证件
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.identity.modify");
		// 添加用户信息
		lInput.addParm("lastname", Uret.getParm("lastname").getStringColumn());
		lInput.addParm("firstname", Uret.getParm("firstname").getStringColumn());
		lInput.addParm("gender", Uret.getParm("gender").getStringColumn());
		lInput.addParm("birthday", Uret.getParm("birthday").getStringColumn().substring(0, 10));
		// 添加证件信息
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("idtype", input.getParm("idtype").getStringColumn());
		if (input.getParm("idtype").getStringColumn().equals("PP")) {
			lInput.addParm("expiry", input.getParm("expiryDate").getStringColumn());
			lInput.addParm("issuecountry", input.getParm("issueCountry").getStringColumn());
		}
		lInput.addParm("idno", input.getParm("idno").getStringColumn().toUpperCase());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息删除证件号接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet delID(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.identity.delete");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息增加地址接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet addAddress(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String address = input.getParm("address").getStringColumn();
		// 去空校验
		// if ("".equals(address)) {
		// // error desc 详细地址不能为空！
		// ret.setError(ErrCodeConstants.API_NULL_ADDRESS_STREETNMBR,
		// TipMessager.getMessage(ErrCodeConstants.API_NULL_ADDRESS_STREETNMBR,
		// language));
		// return ret;
		// }
		if (address.length() > 200) {
			// error desc 详细地址过长，应不超过200！
			ret.setError(ErrCodeConstants.API_ADDRESS_STREETNMBR_TOOLONG_LEN_IN200,
					TipMessager.getMessage(ErrCodeConstants.API_ADDRESS_STREETNMBR_TOOLONG_LEN_IN200, language));
			return ret;
		}
		String province = input.getParm("province").getStringColumn();
		// if ("".equals(province)) {
		// // error desc 省份不能为空！
		// ret.setError(ErrCodeConstants.API_NULL_PROVINCE,
		// TipMessager.getMessage(ErrCodeConstants.API_NULL_PROVINCE,
		// language));
		// return ret;
		// }
		String city = input.getParm("city").getStringColumn();
		// if ("".equals(city)) {
		// // error desc 城市不能为空！
		// ret.setError(ErrCodeConstants.API_NULL_CITY,
		// TipMessager.getMessage(ErrCodeConstants.API_NULL_CITY, language));
		// return ret;
		// }
		String zipcode = input.getParm("zipcode").getStringColumn();
		// if (!"".equals(zipcode) && !Pattern.matches(REG_ZIPCODE, zipcode)) {
		// // error desc 邮编不符合规范！
		// ret.setError(ErrCodeConstants.API_ZIPCODE_REG_ERROR,
		// TipMessager.getMessage(ErrCodeConstants.API_ZIPCODE_REG_ERROR,
		// language));
		// return ret;
		// }
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.address.add");
		lInput.addParm("memberid", memberid);
		lInput.addParm("title", "地址");
		lInput.addParm("province", province);
		lInput.addParm("city", city);
		lInput.addParm("zipcode", zipcode);
		lInput.addParm("address", address);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息修改地址接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updAddress(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String address = input.getParm("address").getStringColumn();
		if (!"".equals(address) && address.length() > 200) {
			// error desc 详细地址过长，应不超过200！
			ret.setError(ErrCodeConstants.API_ADDRESS_STREETNMBR_TOOLONG_LEN_IN200,
					TipMessager.getMessage(ErrCodeConstants.API_ADDRESS_STREETNMBR_TOOLONG_LEN_IN200, language));
			return ret;
		}
		String zipcode = input.getParm("zipcode").getStringColumn();
		if (!"".equals(zipcode) && !Pattern.matches(REG_ZIPCODE, zipcode)) {
			// error desc 邮编不符合规范！
			ret.setError(ErrCodeConstants.API_ZIPCODE_REG_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_ZIPCODE_REG_ERROR, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.address.modify");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("province", input.getParm("province").getStringColumn());
		lInput.addParm("city", input.getParm("city").getStringColumn());
		lInput.addParm("zipcode", zipcode);
		lInput.addParm("address", address);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息删除地址接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet delAddress(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.address.delete");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息增加联系方式接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet addContact(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = checkcontact(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.binding.add");
		// 区号
		lInput.addParm("area", input.getParm("area").getStringColumn());
		lInput.addParm("memberid", memberid);
		lInput.addParm("bindtype", input.getParm("type").getStringColumn());
		lInput.addParm("bindingvalue", input.getParm("no").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息增加联系方式接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet addContactEmail(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = checkcontact(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.contact.add");
		// 区号
		lInput.addParm("region_code", input.getParm("area").getStringColumn());
		lInput.addParm("memberid", memberid);
		lInput.addParm("contact_type", input.getParm("type").getStringColumn());
		lInput.addParm("contact_value", input.getParm("no").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息修改联系方式接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updContact(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = checkcontact(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.binding.modify");
		// 区号
		lInput.addParm("area", input.getParm("area").getStringColumn());
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("bindtype", input.getParm("type").getStringColumn());
		lInput.addParm("bindingvalue", input.getParm("no").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息修改联系方式接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updContactEmail(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = checkcontact(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.contact.modify");
		lInput.addParm("region_code", input.getParm("area").getStringColumn());
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("contact_type", input.getParm("type").getStringColumn());
		lInput.addParm("contact_value", input.getParm("no").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息删除联系方式接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet delContact(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.binding.delete");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 会员信息删除联系方式接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet delContactEmail(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.contact.delete");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("contact_type", EMAIL);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 修改密码接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet changePwd(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String oldpassword = input.getParm("oldpassword").getStringColumn();
		if ("".equals(oldpassword)) {
			// error desc 原密码不能为空！
			ret.setError(ErrCodeConstants.API_NULL_PWD_OLD,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_PWD_OLD, language));
			return ret;
		}
		String newpassword = input.getParm("newpassword").getStringColumn();
		if (oldpassword.equals(newpassword)) {
			// error desc DES错误！
			ret.setError(ErrCodeConstants.API_OLDPWD_NEWPWD_IS_NOT_SAME,
					TipMessager.getMessage(ErrCodeConstants.API_OLDPWD_NEWPWD_IS_NOT_SAME, language));
			return ret;
		} else if ("".equals(newpassword)) {
			// error desc 新密码不能为空！"
			ret.setError(ErrCodeConstants.API_NULL_PWD_NEW,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_PWD_NEW, language));
			return ret;
		} else if (!Pattern.matches(REG_PWD, newpassword)) {
			// error desc 新密码只能是8-16位数字和字母组合！
			ret.setError(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16,
					TipMessager.getMessage(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16, language));
			return ret;
		}
		String mobile = input.getParm("mobile").getStringColumn();
		if ("".equals(mobile)) {
			// error desc 手机号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MOBILE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MOBILE, language));
			return ret;
		} else if (!Pattern.matches(REG_MOBILE, mobile)) {
			// error desc 手机号不符合规范！
			ret.setError(ErrCodeConstants.API_MOBILE_REG_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_MOBILE_REG_ERROR, language));
			return ret;
		}
		// 验证短信验证码
		String smscode = input.getParm("smscode").getStringColumn();
		if ("".equals(smscode)) {
			// error desc 手机验证码不能为空！
			ret.setError(ErrCodeConstants.API_NULL_VALIDATE_CODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_VALIDATE_CODE, language));
			return ret;
		}
		if (!"mob".equals(smscode)) {
			String redisSmsCode = RedisManager.getManager().get("UPDATEPWD_SEND_SMS" + mobile);
			// Calendar curDate = Calendar.getInstance();
			// Calendar tommorowDate = new GregorianCalendar(curDate
			// .get(Calendar.YEAR), curDate.get(Calendar.MONTH), curDate
			// .get(Calendar.DATE) + 1, 0, 0, 0);
			// 从当前时间到凌晨零点，剩余秒数
			// int delayTime = (int)(tommorowDate.getTimeInMillis() - curDate
			// .getTimeInMillis()) / 1000;
			test: {
				if ("000000".equals(smscode)
						&& "true".equalsIgnoreCase(SystemConfig.getConfig().getItemString("DEBUGMODE", "ISDEBUG"))) {
					break test;
				}
				if ("".equals(redisSmsCode) || redisSmsCode == null) {
					// error desc 手机验证码失效,请重新获取！
					ret.setError(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY,
							TipMessager.getMessage(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY, language));
					// 修改与重置密码不做验证次数的限制
					// 增加验证次数一次
					// String count =
					// RedisManager.getManager().get("UPDATEPWD_SEND_SMS"+mobile);
					// if("".equals(count) || count==null){
					// RedisManager.getManager().set("UPDATEPWD_SEND_SMS"+mobile,
					// "1",
					// delayTime-1);
					// }else{
					// int countCurrent = Integer.parseInt(count);
					// if(countCurrent>=5){
					// ret.setError("0001", "手机验证失效且已重试5次，该手机号今天不能再注册，请明天重试！");
					// return ret;
					// }
					// int couti = Integer.parseInt(count);
					// RedisManager.getManager().set("UPDATEPWD_SEND_SMS"+mobile,
					// ""+(couti+1), delayTime-1);
					// }
					return ret;
				}
				if (!smscode.trim().toUpperCase().equals(redisSmsCode.trim().toUpperCase())) {
					// 短信验证码错误，清除redis内缓存值
					// RedisManager.getManager().del("REGISTER_SEND_SMS" +
					// mobile);
					// error desc 验证码错误,请重新获取并输入正确验证码！
					ret.setError(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT,
							TipMessager.getMessage(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT, language));
					// 增加验证次数一次
					// String count =
					// RedisManager.getManager().get("UPDATEPWD_SEND_SMS"+mobile);
					// if("".equals(count) || count==null){
					// RedisManager.getManager().set("UPDATEPWD_SEND_SMS"+mobile,
					// "1",
					// delayTime-1);
					// }else{
					// int countCurrent = Integer.parseInt(count);
					// if(countCurrent>=5){
					// ret.setError("0001", "手机验证码错误且已重试5次，该手机号今天不能再注册，请明天重试！");
					// return ret;
					// }s
					// int couti = Integer.parseInt(count);
					// RedisManager.getManager().set("UPDATEPWD_SEND_SMS"+mobile,
					// ""+(couti+1), delayTime-1);
					// }

					return ret;
				}
			}
			// 短信验证码校验通过，清除redis内缓存值
			RedisManager.getManager().del("UPDATEPWD_SEND_SMS" + mobile);
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.changepassword");
		lInput.addParm("memberid", memberid);
		lInput.addParm("oldpassword", oldpassword);
		lInput.addParm("newpassword", newpassword);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		LOGGER.info("--------------RET:" + ret);
		if (TWO_THOUSAND_TWELVE.equals(ret.getErrorCode())) {
			// error desc 原密码不正确！
			ret.setError(TWO_THOUSAND_TWELVE, TipMessager.getMessage(ErrCodeConstants.API_PWD_OLD_ERROR, language));
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (!(ret.getErrorCode().equals("") || ret.getErrorCode() == null)) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 修改密码接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet changePwdEmail(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String oldpassword = input.getParm("oldpassword").getStringColumn();
		if ("".equals(oldpassword)) {
			// error desc 原密码不能为空！
			ret.setError(ErrCodeConstants.API_NULL_PWD_OLD,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_PWD_OLD, language));
			return ret;
		}
		String newpassword = input.getParm("newpassword").getStringColumn();
		if (oldpassword.equals(newpassword)) {
			// error desc DES错误！
			ret.setError(ErrCodeConstants.API_OLDPWD_NEWPWD_IS_NOT_SAME,
					TipMessager.getMessage(ErrCodeConstants.API_OLDPWD_NEWPWD_IS_NOT_SAME, language));
			return ret;
		} else if ("".equals(newpassword)) {
			// error desc 新密码不能为空！"
			ret.setError(ErrCodeConstants.API_NULL_PWD_NEW,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_PWD_NEW, language));
			return ret;
		} else if (!Pattern.matches(REG_PWD, newpassword)) {
			// error desc 新密码只能是8-16位数字和字母组合！
			ret.setError(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16,
					TipMessager.getMessage(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.changepassword");
		lInput.addParm("memberid", memberid);
		lInput.addParm("oldpassword", oldpassword);
		lInput.addParm("newpassword", newpassword);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		LOGGER.info("--------------RET:" + ret);
		if (TWO_THOUSAND_TWELVE.equals(ret.getErrorCode())) {
			// error desc 原密码不正确！
			ret.setError(TWO_THOUSAND_TWELVE, TipMessager.getMessage(ErrCodeConstants.API_PWD_OLD_ERROR, language));
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (!(ret.getErrorCode().equals("") || ret.getErrorCode() == null)) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 重置密码接口(web端email登陆)
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet resetPwdEmail(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String userId = input.getParm("userId").getStringColumn();
		if ("".equals(userId)) {
			// error desc 邮箱号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String password = input.getParm("password").getStringColumn();
		if ("".equals(password)) {
			// error desc 密码不能为空！
			ret.setError(ErrCodeConstants.API_NULL_USER_PASSWORD,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_PASSWORD, language));
			return ret;
		} else if (!Pattern.matches(REG_PWD, password)) {
			// error desc 新密码只能是8-16位数字和字母组合！
			ret.setError(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16,
					TipMessager.getMessage(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16, language));
			return ret;
		}
		if (!"".equals(password) && password.length() > 50) {
			// 密码长度过长，应不超过50位！
			ret.setError(ErrCodeConstants.API_PWD_TOOLONG_IN50,
					TipMessager.getMessage(ErrCodeConstants.API_PWD_TOOLONG_IN50, language));
			return ret;
		}
		// 验证邮箱验证
		String emailcode = input.getParm("emailcode").getStringColumn();
		if ("".equals(emailcode)) {
			// error desc 邮箱验证码不能为空！
			ret.setError(ErrCodeConstants.API_NULL_VALIDATE_CODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_VALIDATE_CODE, language));
			return ret;
		}
		String redisEmailCode = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_forgetpwd.toKey("vc:") + userId);
		// 获取邮箱验证码测试开关
		CommandRet l_ret = new CommandRet("");
		CommandInput commandInputGetSwitch = new CommandInput("com.cares.sh.order.config.query");
		commandInputGetSwitch.addParm("type", "COMMON");
		commandInputGetSwitch.addParm("code", "MAILBOX_VERIFICATION_CODE_TEST_SWITCH");
		l_ret = context.doOther(commandInputGetSwitch, false);
		String testSwitch = l_ret.getParm("configs").getTableColumn().getRow(0).getColumn("data").getStringColumn();
		test: {
			if ("000000".equals(emailcode) && "1".equals(testSwitch)) {
				break test;
			}
			if ("".equals(redisEmailCode) || redisEmailCode == null) {
				ret.setError(ErrCodeConstants.API_EMAIL_VERIFYCODE_LOSE_RETRY,
						TipMessager.getMessage(ErrCodeConstants.API_EMAIL_VERIFYCODE_LOSE_RETRY, language));
				return ret;
			}
			if (!emailcode.trim().toUpperCase().equals(redisEmailCode.trim().toUpperCase())) {
				// 从redis中获取已经错误的次数
				String redisFailedNum = RedisManager.getManager()
						.get(RedisNamespaceEnum.api_cache_forgetpwd.toKey("vcerrorno:") + userId);
				// 防止穷举法暴力
				// 从底层获取配置的邮箱验证码错误次数,如果没配置，默认输入错误六次提示用户一分钟后重新发送验证码，以最后一次输入验证码时间开始。
				CommandRet y_ret = new CommandRet("");
				CommandInput commandInputGetVCF = new CommandInput("com.cares.sh.order.config.query");
				commandInputGetVCF.addParm("type", "COMMON");
				commandInputGetVCF.addParm("code", "MAXFAILED_MAIL_VC_TENM");
				y_ret = context.doOther(commandInputGetVCF, false);
				String limitNum = y_ret.getParm("configs").getTableColumn().getRow(0).getColumn("data")
						.getStringColumn();
				if (limitNum != null && !"".equals(limitNum)) {
					if (limitNum.equals(redisFailedNum)) {
						// 提示用户一分钟以后再次发送验证码
						ret.setError(ErrCodeConstants.MAIL_VC_FAILED_TOMANY_RESEND_VC,
								TipMessager.getMessage(ErrCodeConstants.MAIL_VC_FAILED_TOMANY_RESEND_VC, language));
						LOGGER.info("ret:" + ret);
						// 设置用户一分钟后发送验证码
						RedisManager.getManager().set(RedisNamespaceEnum.api_cache_forgetpwd.toKey("timelimit:") + userId,
								"60秒时间限制", 60);
					}
				} else {
					if ("6".equals(redisFailedNum)) {
						// 提示用户一分钟以后再次发送验证码
						ret.setError(ErrCodeConstants.MAIL_VC_FAILED_TOMANY_RESEND_VC,
								TipMessager.getMessage(ErrCodeConstants.MAIL_VC_FAILED_TOMANY_RESEND_VC, language));
						LOGGER.info("ret:" + ret);
						// 设置用户一分钟后发送验证码
						RedisManager.getManager().set(RedisNamespaceEnum.api_cache_forgetpwd.toKey("timelimit:") + userId,
								"60秒时间限制", 60);
					}
				}
				ret.setError(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT,
						TipMessager.getMessage(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT, language));
				LOGGER.info("ret:" + ret);
				int errorno=0;
				if (!StringUtils.isEmpty(redisFailedNum)) {
					errorno= Integer.parseInt(redisFailedNum);
				}
				// 将用户输入的邮箱验证码错误次数保存到redis中
				RedisManager.getManager().set(RedisNamespaceEnum.api_cache_forgetpwd.toKey("vcerrorno:") + userId,
						String.valueOf(errorno++), 600);

				return ret;
			}
		}
		// 短信验证码校验通过并且更改密码成功，清除redis内缓存值
		RedisManager.getManager().del(RedisNamespaceEnum.api_cache_forgetpwd.toKey("vc:") + userId);
		// 清除发送邮箱验证码时间限制的key
		RedisManager.getManager().del(RedisNamespaceEnum.api_cache_forgetpwd.toKey("timelimit:") + userId);
		// 清除邮箱验证码有效时间内的错误次数并重置次数
		RedisManager.getManager().del(RedisNamespaceEnum.api_cache_forgetpwd.toKey("vcerrorno:") + userId);
		// 调用后台找回密码
		CommandInput lInput = new CommandInput("com.travelsky.quick.member.resetpassword");
		lInput.addParm("idno", userId);
		lInput.addParm("idtype", EMAIL);
		lInput.addParm("password", password);
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 重置密码接口(web端)
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet resetPwd(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String username = input.getParm("username").getStringColumn();
		if ("".equals(username)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String password = input.getParm("password").getStringColumn();
		if ("".equals(password)) {
			// error desc 密码不能为空！
			ret.setError(ErrCodeConstants.API_NULL_USER_PASSWORD,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_USER_PASSWORD, language));
			return ret;
		} else if (!Pattern.matches(REG_PWD, password)) {
			// error desc 新密码只能是8-16位数字和字母组合！
			ret.setError(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16,
					TipMessager.getMessage(ErrCodeConstants.API_PWD_NEW_LETTERNUM_ASTRICT8_16, language));
			return ret;
		}
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String idType = input.getParm("idtype").getStringColumn();
		if ("".equals(idType)) {
			ret.setError(ErrCodeConstants.API_NULL_IDTYPE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_IDTYPE, language));
			return ret;
		}
		String idNo = input.getParm("idno").getStringColumn().toUpperCase();
		if ("".equals(idNo)) {
			ret.setError(ErrCodeConstants.API_NULL_IDNO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_IDNO, language));
			return ret;
		}
		msg = checkID(input, idNo, idType);
		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String mobile = input.getParm("mobile").getStringColumn();
		if ("".equals(mobile)) {
			// error desc 手机号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_MOBILE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MOBILE, language));
			return ret;
		}
		// 要是重置密码，就先验证用户与手机号是否关联
		// 获取会员信息
		CommandInput lInput1 = new CommandInput("com.cares.sh.order.member.view");
		lInput1.addParm("id", username);
		lInput1.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput1, false);
		if (!ret.isError()) {
			// 获取联系信息
			Table binding = ret.getParm("binding").getTableColumn();
			if (null == binding || "".equals(binding)) {
				// error desc 联系方式不存在！
				ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, language));
				return ret;
			}
			boolean flag = true;
			for (int i = 0; i < binding.getRowCount(); i++) {
				String mobileNo = binding.getRow(i).getColumn("bindingvalue").getStringColumn();
				if (mobile.equals(mobileNo)) {
					flag = false;
				}
			}
			if (flag) {
				// error desc 联系方式不存在！
				ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, language));
				return ret;
			}
		} else {
			// error desc 用户不存在！
			ret.setError(ErrCodeConstants.API_USER_NON_EXIST,
					TipMessager.getMessage(ErrCodeConstants.API_USER_NON_EXIST, language));
			return ret;
		}

		// 验证短信验证
		String smscode = input.getParm("smscode").getStringColumn();
		if ("".equals(smscode)) {
			// error desc 手机验证码不能为空！
			ret.setError(ErrCodeConstants.API_NULL_VALIDATE_CODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_VALIDATE_CODE, language));
			return ret;
		}
		String redisSmsCode = RedisManager.getManager().get("FORGETPWD_SEND_SMS" + mobile);
		// Calendar curDate = Calendar.getInstance();
		// Calendar tommorowDate = new GregorianCalendar(curDate
		// .get(Calendar.YEAR), curDate.get(Calendar.MONTH), curDate
		// .get(Calendar.DATE) + 1, 0, 0, 0);
		// //从当前时间到凌晨零点，剩余秒数
		// int delayTime = (int)(tommorowDate.getTimeInMillis() - curDate
		// .getTimeInMillis()) / 1000;
		test: {
			if ("000000".equals(smscode)
					&& "true".equalsIgnoreCase(SystemConfig.getConfig().getItemString("DEBUGMODE", "ISDEBUG"))) {
				break test;
			}
			if ("".equals(redisSmsCode) || redisSmsCode == null) {
				// error desc 手机验证码失效,请重新获取！
				ret.setError(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY,
						TipMessager.getMessage(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY, language));
				// 修改密码与重置密码 不对验证次数进行限制
				// String count =
				// RedisManager.getManager().get("FORGETPWD_SEND_SMS"+mobile);
				// if("".equals(count) || count==null){
				// RedisManager.getManager().set("FORGETPWD_SEND_SMS"+mobile,
				// "1",
				// delayTime-1);
				// }else{
				// int countCurrent = Integer.parseInt(count);
				// if(countCurrent>=5){
				// ret.setError("0001", "手机验证失效且已重试5次，该手机号今天不能再注册，请明天重试！");
				// return ret;
				// }
				// int couti = Integer.parseInt(count);
				// RedisManager.getManager().set("FORGETPWD_SEND_SMS"+mobile,
				// ""+(couti+1), delayTime-1);
				// }
				return ret;
			}
			if (!smscode.trim().toUpperCase().equals(redisSmsCode.trim().toUpperCase())) {
				// 短信验证码错误，清除redis内缓存值
				// RedisManager.getManager().del("FORGETPWD_SEND_SMS" + mobile);
				// error desc 验证码错误,请重新获取并输入正确验证码！
				ret.setError(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT,
						TipMessager.getMessage(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT, language));
				LOGGER.info("ret:" + ret);
				// 增加验证次数一次
				// String count =
				// RedisManager.getManager().get("FORGETPWD_SEND_SMS"+mobile);
				// if("".equals(count) || count==null){
				// RedisManager.getManager().set("FORGETPWD_SEND_SMS"+mobile,
				// "1",
				// delayTime-1);
				// }else{
				// int countCurrent = Integer.parseInt(count);
				// if(countCurrent>=5){
				// ret.setError("0001", "手机验证码错误且已重试5次，该手机号今天不能再注册，请明天重试！");
				// return ret;
				// }
				// int couti = Integer.parseInt(count);
				// RedisManager.getManager().set("FORGETPWD_SEND_SMS"+mobile,
				// ""+(couti+1), delayTime-1);
				// }

				return ret;
			}
		}
		// 短信验证码校验通过，清除redis内缓存值
		RedisManager.getManager().del("FORGETPWD_SEND_SMS" + mobile);
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.resetpassword");
		lInput.addParm("username", username);
		lInput.addParm("memberid", username);
		lInput.addParm("password", password);
		lInput.addParm("idtype", input.getParm("idtype").getStringColumn());
		lInput.addParm("idno", input.getParm("idno").getStringColumn().toUpperCase());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 重置密码校验接口(手机端)
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet resetpwdvalid(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String username = input.getParm("username").getStringColumn();
		if ("".equals(username)) {
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String idType = input.getParm("idtype").getStringColumn();
		if ("".equals(idType)) {
			ret.setError(ErrCodeConstants.API_NULL_IDTYPE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_IDTYPE, language));
			return ret;
		}
		String idNo = input.getParm("idno").getStringColumn().toUpperCase();
		if ("".equals(idNo)) {
			ret.setError(ErrCodeConstants.API_NULL_IDNO,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_IDNO, language));
			return ret;
		}
		String mobile = input.getParm("mobile").getStringColumn();
		if ("".equals(mobile)) {
			ret.setError(ErrCodeConstants.API_NULL_MOBILE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MOBILE, language));
			return ret;
		}
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		msg = checkID(input, idNo, idType);
		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		// 要是重置密码，就先验证用户与手机号是否关联
		// 获取会员信息
		CommandInput lInput1 = new CommandInput("com.cares.sh.order.member.view");
		lInput1.addParm("id", username);
		lInput1.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput1, false);
		if (!ret.isError()) {
			// 获取联系信息并验证
			Table binding = ret.getParm("binding").getTableColumn();
			if (null == binding || "".equals(binding)) {
				ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, language));
				return ret;
			}
			boolean flag = true;
			for (int i = 0; i < binding.getRowCount(); i++) {
				String mobileNo = binding.getRow(i).getColumn("bindingvalue").getStringColumn();
				if (mobile.equals(mobileNo)) {
					flag = false;
				}
			}
			if (flag) {
				ret.setError(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_CONTACT_WAY_NON_EXIST, language));
				return ret;
			}
			// 获取证件信息并验证
			Table identity = ret.getParm("identity").getTableColumn();
			if (null == identity || "".equals(identity)) {
				ret.setError(ErrCodeConstants.API_IDENTITY_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_IDENTITY_NON_EXIST, language));
				return ret;
			}
			boolean idenBoolean = true;
			for (int i = 0; i < identity.getRowCount(); i++) {
				String idt = identity.getRow(i).getColumn("idtype").getStringColumn();
				String idn = identity.getRow(i).getColumn("idno").getStringColumn();
				if (idType.equals(idt) && idNo.equals(idn)) {
					idenBoolean = false;
				}
			}
			if (idenBoolean) {
				ret.setError(ErrCodeConstants.API_IDENTITY_NON_EXIST,
						TipMessager.getMessage(ErrCodeConstants.API_IDENTITY_NON_EXIST, language));
				return ret;
			}
		} else {
			ret.setError(ErrCodeConstants.API_USER_NON_EXIST,
					TipMessager.getMessage(ErrCodeConstants.API_USER_NON_EXIST, language));
			return ret;
		}
		// 验证短信验证码
		String smscode = input.getParm("smscode").getStringColumn();
		if ("".equals(smscode)) {
			ret.setError(ErrCodeConstants.API_NULL_VALIDATE_CODE,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_VALIDATE_CODE, language));
			return ret;
		}
		String redisSmsCode = RedisManager.getManager().get("FORGETPWD_SEND_SMS" + mobile);
		test: {
			if ("000000".equals(smscode)
					&& "true".equalsIgnoreCase(SystemConfig.getConfig().getItemString("DEBUGMODE", "ISDEBUG"))) {
				break test;
			}
			if ("".equals(redisSmsCode) || redisSmsCode == null) {
				ret.setError(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY,
						TipMessager.getMessage(ErrCodeConstants.API_SMS_VERIFYCODE_LOSE_RETRY, language));
				return ret;
			}
			if (!smscode.trim().toUpperCase().equals(redisSmsCode.trim().toUpperCase())) {
				// 短信验证码错误，清除redis内缓存值
				// RedisManager.getManager().del("FORGETPWD_SEND_SMS" + mobile);
				ret.setError(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT,
						TipMessager.getMessage(ErrCodeConstants.API_SMSVFCODE_ERROR_RETRY_CORRECT, language));
				return ret;
			}
		}
		// 短信验证码校验通过，清除redis内缓存值
		RedisManager.getManager().del("FORGETPWD_SEND_SMS" + mobile);
		// 将username，mobile，idtype，idno，smscode存入缓存中，设置token值（username+mobile+timeout）
		// 获取找回密码用户信息验证有效时间
		int timeout = 120;
		try {
			String tcode = this.getAppCacheValue("API_RETRIEVEPWD_VERIFY_TIMEOUT", "COMMON");
			if (!StringUtils.isEmpty(tcode)) {
				timeout = Integer.parseInt(tcode);
			}
		} catch (Exception e) {
			LOGGER.error("获取找回密码用户信息验证有效时间失败", e);
			ret.setError(ErrCodeConstants.API_RETRIEVEPWD_VERIFY_TIMEOUT_FAILD,
					TipMessager.getMessage(ErrCodeConstants.API_RETRIEVEPWD_VERIFY_TIMEOUT_FAILD, language));
			return ret;
		}

		// 设置token值
		StringBuffer token = new StringBuffer();
		token.append(username).append(mobile).append(timeout);
		CommandData data = new CommandData();
		data.addParm("username", username);
		data.addParm("mobile", mobile);
		data.addParm("idtype", idType);
		data.addParm("idno", idNo);
		data.addParm("smscode", smscode);
		String smsJson = JsonUnit.toJson(data);
		RedisManager.getManager().set(RedisNamespaceEnum.api_cache_forgetpwd.toKey(token.toString()), smsJson, timeout);
		// 返回token、timeout
		ret.addParm("token", token.toString());
		ret.addParm("timeout", timeout);
		return ret;
	}

	/**
	 * 重置密码接口(手机端)
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet resetPwd2(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String token = input.getParm("token").getStringColumn();
		String password = input.getParm("password").getStringColumn();
		String username = input.getParm("username").getStringColumn();
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String idType = input.getParm("idtype").getStringColumn();
		String idNo = input.getParm("idno").getStringColumn().toUpperCase();
		msg = checkID(input, idNo, idType);
		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String smsJson = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_forgetpwd.toKey(token));
		if (smsJson != null && "".equals(smsJson)) {
			LOGGER.debug("请求已失效");
			ret.setError(ErrCodeConstants.API_REQ_LOSE_RETRY,
					TipMessager.getMessage(ErrCodeConstants.API_REQ_LOSE_RETRY, language));
			return ret;
		}
		CommandData data = new CommandData();
		JsonUnit.fromJson(data, smsJson);
		String username2 = data.getParm("username").getStringColumn();
		String idtype = data.getParm("idtype").getStringColumn();
		String idno = data.getParm("idno").getStringColumn().toUpperCase();
		if (!username2.equals(username)) {
			LOGGER.debug("用户名不正确");
			ret.setError(ErrCodeConstants.API_USERNAME_ERROR,
					TipMessager.getMessage(ErrCodeConstants.API_USERNAME_ERROR, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.resetpassword");
		lInput.addParm("username", username);
		lInput.addParm("memberid", username);
		lInput.addParm("password", password);
		lInput.addParm("idtype", idtype);
		lInput.addParm("idno", idno);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 增加常用乘机人接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet addPassenger(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String idType = input.getParm("idtype").getStringColumn();
		String idNo = input.getParm("idno").getStringColumn().toUpperCase();
		String expiryDate = input.getParm("expiryDate").getStringColumn();
		msg = checkID(input, idNo, idType);
		if (idType.equals("PP")) {
			if (expiryDate != null && !"".equals(expiryDate)) {
				boolean flag = DateUtils.formatDate(expiryDate);
				if (!flag) {
					ret.setError(ErrCodeConstants.API_FORMATDATE,
							TipMessager.getMessage(ErrCodeConstants.API_FORMATDATE, language));
					return ret;
				}
			}
		}
		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.profile.add");
		// 区号
		lInput.addParm("area", input.getParm("area").getStringColumn());
		lInput.addParm("memberid", memberid);
		lInput.addParm("lastname", input.getParm("lastname").getStringColumn());
		lInput.addParm("firstname", input.getParm("firstname").getStringColumn());
		lInput.addParm("idtype", input.getParm("idtype").getStringColumn());
		if (input.getParm("idtype").getStringColumn().equals("PP")) {
			lInput.addParm("expiry", input.getParm("expiryDate").getStringColumn());
			lInput.addParm("issuecountry", input.getParm("issueCountry").getStringColumn());
		}
		lInput.addParm("idno", input.getParm("idno").getStringColumn().toUpperCase());
		lInput.addParm("mobile", input.getParm("mobile").getStringColumn());
		lInput.addParm("email", input.getParm("email").getStringColumn());
		lInput.addParm("birthday", input.getParm("birthday").getStringColumn());
		lInput.addParm("gender", input.getParm("gender").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lInput.addParm("nationality", input.getParm("nationality").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 查询常用乘机人接口(不包含用户)
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet getPassenger(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.profile.query");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("paxid").getStringColumn());
		ret = context.doOther(lInput, false);
		if (!ret.isError()) {
			Table passenger = ret.getParm("profile").getTableColumn();
			if (null != passenger) {
				passenger = passenger.copy(new String[] { "id", "lastname", "firstname", "idtype", "idno", "mobile",
						"email", "birthday", "gender", "pinyin", "area", "issuecountry", "expiry","nationality" });
			} else {
				passenger = new Table(new String[] { "id", "lastname", "firstname", "idtype", "idno", "mobile", "email",
						"birthday", "gender", "pinyin", "area","nationality" });
			}
			// passenger.sort("pinyin");
			CommandRet lNewret = new CommandRet("");
			lNewret.setError(ret.getErrorCode(), ret.getErrorDesc());
			lNewret.addParm("passenger", passenger);
			return lNewret;
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 查询常用乘机人接口(包含用户)
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet getPassengerAndUser(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.profile.query");
		lInput.addParm("memberid", memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		if (!ret.isError()) {
			Table passenger = ret.getParm("profile").getTableColumn();
			if (null != passenger) {
				passenger = passenger.copy(new String[] { "id", "lastname", "firstname", "idtype", "idno", "mobile",
						"birthday", "gender", "pinyin", "area","nationality" });
			} else {
				passenger = new Table(new String[] { "id", "lastname", "firstname", "idtype", "idno", "mobile",
						"birthday", "gender", "pinyin", "area","nationality" });
			}
			CommandInput llInput = new CommandInput("com.cares.sh.order.member.view");
			llInput.addParm("id", memberid);
			llInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
			ret = context.doOther(llInput, false);
			if (!ret.isError()) {
				String name = ret.getParm("name").getStringColumn();
				String lastname = ret.getParm("lastname").getStringColumn();
				String firstname = ret.getParm("firstname").getStringColumn();
				String birthday = ret.getParm("birthday").getStringColumn();
				String gender = ret.getParm("gender").getStringColumn();
				String mobile = null;
				String area = null;
				String nationality = ret.getParm("nationality").getStringColumn();
				// 获取联系信息
				Table binding = ret.getParm("binding").getTableColumn();
				if (null != binding) {
					for (int j = 0; j < binding.getRowCount(); j++) {
						Row bindingRow = binding.getRow(j);
						String bindtype = bindingRow.getColumn("bindtype").getStringColumn();
						if ("mobile".equals(bindtype)) {
							mobile = bindingRow.getColumn("bindingvalue").getStringColumn();
							area = bindingRow.getColumn("area").getStringColumn();
							break;
						}
					}
				}
				// 获取证件信息
				Table identity = ret.getParm("identity").getTableColumn();
				if (null != identity) {
					for (int i = 0; i < identity.getRowCount(); i++) {
						Row row = identity.getRow(i);
						String id = row.getColumn("id").getStringColumn();
						String idtype = row.getColumn("idtype").getStringColumn();
						String idno = row.getColumn("idno").getStringColumn();
						Row passengerRow = passenger.addRow();
						passengerRow.addColumn("id", id);
						passengerRow.addColumn("lastname", lastname);
						passengerRow.addColumn("firstname", firstname);
						passengerRow.addColumn("idtype", idtype);
						passengerRow.addColumn("idno", idno);
						passengerRow.addColumn("mobile", mobile);
						passengerRow.addColumn("birthday", birthday);
						passengerRow.addColumn("gender", gender);
						passengerRow.addColumn("pinyin", name);
						passengerRow.addColumn("area", area);
						passengerRow.addColumn("nationality", nationality);
					}
				}
			}
			// passenger.sort("pinyin");
			CommandRet lNewret = new CommandRet("");
			lNewret.setError(ret.getErrorCode(), ret.getErrorDesc());
			lNewret.addParm("passenger", passenger);
			return lNewret;
		}
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 修改常用乘机人接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updPassenger(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String gender = input.getParm("gender").getStringColumn();
		// if ("".equals(gender)) {
		// //error desc 性别不能为空！
		// ret.setError(ErrCodeConstants.API_NULL_GENDER,
		// TipMessager.getMessage(ErrCodeConstants.API_NULL_GENDER, language));
		// return ret;
		// }
		String msg = check(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		String idType = input.getParm("idtype").getStringColumn();
		String idNo = input.getParm("idno").getStringColumn().toUpperCase();
		String expiryDate = input.getParm("expiryDate").getStringColumn();
		msg = checkID(input, idNo, idType);
		if (idType.equals("PP")) {
			if (expiryDate != null && expiryDate.length() > 0 && !"".equals(expiryDate)) {
				boolean flag = DateUtils.formatDate(expiryDate);
				if (!flag) {
					ret.setError(ErrCodeConstants.API_FORMATDATE,
							TipMessager.getMessage(ErrCodeConstants.API_FORMATDATE, language));
					return ret;
				}
			}
		}

		if (StringUtils.hasLength(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.profile.modify");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("lastname", input.getParm("lastname").getStringColumn());
		lInput.addParm("firstname", input.getParm("firstname").getStringColumn());
		lInput.addParm("idtype", input.getParm("idtype").getStringColumn());
		lInput.addParm("idno", input.getParm("idno").getStringColumn().toUpperCase());
		lInput.addParm("expiry", input.getParm("expiryDate").getStringColumn());
		lInput.addParm("issuecountry", input.getParm("issueCountry").getStringColumn());
		lInput.addParm("mobile", input.getParm("mobile").getStringColumn());
		lInput.addParm("email", input.getParm("email").getStringColumn());
		lInput.addParm("birthday", input.getParm("birthday").getStringColumn());
		lInput.addParm("gender", gender);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		lInput.addParm("area", input.getParm("area").getStringColumn());
		lInput.addParm("nationality", input.getParm("nationality").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 删除常用乘机人接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet delPassenger(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.profile.delete");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 查询偏好接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet getPreference(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.preference.query");
		lInput.addParm("memberid", memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 修改偏好接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updPreference(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.preference.modify");
		lInput.addParm("memberid", memberid);
		lInput.addParm("org", input.getParm("org").getStringColumn().toUpperCase());
		lInput.addParm("dst", input.getParm("dst").getStringColumn().toUpperCase());
		lInput.addParm("cabin", input.getParm("cabin").getStringColumn().toUpperCase());
		lInput.addParm("seat", input.getParm("seat").getStringColumn());
		lInput.addParm("meal", input.getParm("meal").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 获取所有通知列表接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet queryAllNotice(CommandData input, SelvetContext<ApiContext> context) {
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.notice.queryall");
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		CommandRet ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 查询用户通知列表接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet getNotice(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.notice.query");
		lInput.addParm("memberid", memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 修改用户通知列表接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet updNotice(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.notice.modify");
		lInput.addParm("memberid", memberid);
		lInput.addParm("noticetype", input.getParm("noticetype").getStringColumn());
		lInput.addParm("notices", input.getParm("notices").getTableColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 查询红包接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet queryredpacket(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.redpacket.query");
		lInput.addParm("memberid", memberid);
		lInput.addParm("status", input.getParm("status").getStringColumn());
		lInput.addParm("type", input.getParm("type").getStringColumn());
		lInput.addParm("code", input.getParm("code").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 查询航线预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet querymyairline(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.airline.query");
		lInput.addParm("memberid", memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 增加航线预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet addmyairline(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String msg = checkair(input);
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.airline.add");
		lInput.addParm("memberid", memberid);
		lInput.addParm("orgcity", input.getParm("orgcity").getStringColumn().toUpperCase());
		lInput.addParm("dstcity", input.getParm("dstcity").getStringColumn().toUpperCase());
		lInput.addParm("startdat", input.getParm("startdat").getDateColumn());
		lInput.addParm("enddate", input.getParm("enddate").getDateColumn());
		lInput.addParm("price", input.getParm("price").getStringColumn());
		lInput.addParm("noticetype", input.getParm("noticetype").getStringColumn());
		lInput.addParm("mobilephone", input.getParm("mobilephone").getStringColumn());
		lInput.addParm("email", input.getParm("email").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 取消航线预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet cancelmyairline(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.airline.cancel");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 删除航线预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet deletemyairline(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.airline.delete");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 查询航班预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet querymyflight(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.flight.query");
		lInput.addParm("memberid", memberid);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 增加航班预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet addmyflight(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();

		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}

		String msg = checkair(input);
		String flightno = input.getParm("flightno").getStringColumn();
		if (flightno.equals("") || !Pattern.matches("^[a-zA-Z]{2}\\d{1,4}", flightno)) {
			msg = ErrCodeConstants.API_FLTNUM_ST_LETTER_LEN2_AND_NUM_IN6;
		}
		if (!"".equals(msg)) {
			ret.setError(msg, TipMessager.getMessage(msg, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.flight.add");
		lInput.addParm("memberid", memberid);
		lInput.addParm("flightno", input.getParm("flightno").getStringColumn());
		lInput.addParm("startdat", input.getParm("startdat").getDateColumn());
		lInput.addParm("enddate", input.getParm("enddate").getDateColumn());
		lInput.addParm("price", input.getParm("price").getStringColumn());
		lInput.addParm("noticetype", input.getParm("noticetype").getStringColumn());
		lInput.addParm("mobilephone", input.getParm("mobilephone").getStringColumn());
		lInput.addParm("email", input.getParm("email").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 取消航班预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet cancelmyflight(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.flight.cancel");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 删除航班预约接口
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet deletemyflight(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.flight.delete");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 认证手机和邮箱
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet identify(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String id = input.getParm("id").getStringColumn();
		if ("".equals(id)) {
			// error desc 编号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_SERIALNUM,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_SERIALNUM, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.binding.identify");
		lInput.addParm("memberid", memberid);
		lInput.addParm("id", input.getParm("id").getStringColumn());
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 实名认证
	 *
	 * @param input
	 * @param context
	 * @return
	 */
	public CommandRet memberAuth(CommandData input, SelvetContext<ApiContext> context) {
		CommandRet ret = new CommandRet("");
		String memberid = input.getParm("memberid").getStringColumn();
		if ("".equals(memberid)) {
			// error desc 会员账号不能为空!
			ret.setError(ErrCodeConstants.API_NULL_MEMBER_ID,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_MEMBER_ID, language));
			return ret;
		}
		String identityid = input.getParm("identityid").getStringColumn();
		if ("".equals(identityid)) {
			// error desc 证件编号不能为空!
			ret.setError(ErrCodeConstants.API_NULL_IDENTITYNUM,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_IDENTITYNUM, language));
			return ret;
		}
		String filename = input.getParm("filename").getStringColumn();
		if ("".equals(filename)) {
			// error desc 文件编号不能为空！
			ret.setError(ErrCodeConstants.API_NULL_FILE_NAME,
					TipMessager.getMessage(ErrCodeConstants.API_NULL_FILE_NAME, language));
			return ret;
		}
		CommandInput lInput = new CommandInput("com.cares.sh.order.member.auth");
		lInput.addParm("memberid", memberid);
		lInput.addParm("identityid", identityid);
		lInput.addParm("filename", filename);
		lInput.addParm("ticketdeptid", input.getParm("tktdeptid").getStringColumn());
		ret = context.doOther(lInput, false);
		// 对B2C访问次数进行计数（XML请求/JSON请求）
		// if (ret.getErrorCode().equals("") || ret.getErrorCode() == null) {
		// String type = SERVICE;
		// RedisUtil redisUtil = new RedisUtil();
		// redisUtil.docount(type, context);
		// }
		return ret;
	}

	/**
	 * 检查邮箱长度
	 * 
	 * @param email
	 * @return
	 */
	public boolean checkEMLen(String email) {
		if (email.length() > 50) {
			return true;
		}
		return false;
	}

	/**
	 * Get sex by id card
	 * 
	 * @param input
	 * @param idNo
	 * @param idType
	 * @return
	 */
	private String checkID(CommandData input, String idNo, String idType) {
		String msg = null;
		if (!"".equals(idType)) {
//			if (!"NI".equalsIgnoreCase(idType) && !"PP".equalsIgnoreCase(idType) && !"OT".equalsIgnoreCase(idType)) {
//				// 证件类型信息有误！
//				msg = ErrCodeConstants.API_IDENTITY_TYPE_ERROR;
//				return msg;
//			}
			if ("NI".equalsIgnoreCase(idType)) {
				if (!Unit.getValidIdCard(idNo)) {
					// 身份证号不符合规范！
					msg = ErrCodeConstants.API_IDENTITY_NO_REG_ERROR;
					return msg;
				}
				Item birthdayItem = input.getParm("birthday");
				String birthday = birthdayItem == null ? "" : birthdayItem.getStringColumn();
				if (!"".equals(birthday)) {
					if (!Unit.getDate(idNo.substring(6, 14)).equals(Unit.getDate(birthday))) {
						// 生日与身份证号不符！
						return ErrCodeConstants.API_PAX_PASSNOBIRTH_ERROR;
					}
				}
				int gen = Integer.valueOf(idNo.substring(16, 17)) % 2;
				if (gen == 0) {
					input.addParm("gender", "F");
				} else if (gen == 1) {
					input.addParm("gender", "M");
				}
			} else if ("PP".equalsIgnoreCase(idType)) {
				if (idNo.length() < 5 || idNo.length() > 12) {
					return ErrCodeConstants.API_CHECK_LENGTH_IDNO_ERROR;
				}
			} else  {
				if (idNo.length() > 50) {
					return ErrCodeConstants.API_CHECK_LENGTH_IDNO_ERROR;
				}
			}
		}
		return msg;
	}

	private String check(CommandData input) {
		String msg = "";
		String password = input.getParm("password").getStringColumn();
		// String birthday = input.getParm("birthday").getStringColumn();
		String lastname = input.getParm("lastname").getStringColumn();
		String firstname = input.getParm("firstname").getStringColumn();
		// String pinyinxing = input.getParm("pinyinxing").getStringColumn();
		// String pinyinming = input.getParm("pinyinming").getStringColumn();
		// String gender = input.getParm("gender").getStringColumn();
		// String idtype = input.getParm("idtype").getStringColumn();
		// String idno = input.getParm("idno").getStringColumn().toUpperCase();
		String email = input.getParm("email").getStringColumn();
		String mobile = input.getParm("mobile").getStringColumn();
		if (!"".equals(password) && password.length() > 50) {
			// 密码长度过长，应不超过50位！
			return ErrCodeConstants.API_PWD_TOOLONG_IN50;
		}
		/*
		 * if (!"".equals(pinyinxing)) { if (!Pattern.matches(REG_ENGLISH,
		 * pinyinxing)) { //拼音姓不符合规范,应长度不超过20，且为字母！ msg =
		 * ErrCodeConstants.API_SPELLSURNAME_REG_ERROR_IN20_LETTER; return msg;
		 * } } if (!"".equals(pinyinming)) { if (!Pattern.matches(REG_ENGLISH,
		 * pinyinming)) { //拼音名不符合规范,应长度不超过20，且为字母！ msg =
		 * ErrCodeConstants.API_SPELLGIVENNAME_REG_ERROR_IN20_LETTER; return
		 * msg; } }
		 */ if (!"".equals(lastname)) {
			if (!Pattern.matches(REG_CHINESE, lastname)) {
				if (!Pattern.matches(REG_ENGLISH, lastname)) {
					// 姓不符合规范,应长度不超过20，且为中文或字母！
					msg = ErrCodeConstants.API_PAX_LASTNAME_ERROR;
					return msg;
				}
			}
		}
		if (!"".equals(firstname)) {
			if (!Pattern.matches(REG_CHINESE, firstname)) {
				if (!Pattern.matches(REG_ENGLISH, firstname)) {
					// 名不符合规范,应长度不超过20，且为中文或字母！
					msg = ErrCodeConstants.API_PAX_FIRSTNAME_ERROR;
					return msg;
				}
			}
		}
		if (!"".equals(lastname) && !"".equals(firstname)) {
			if (Pattern.matches(REG_CHINESE, lastname) && !Pattern.matches(REG_CHINESE, firstname)) {
				// 姓为中文时，名应为中文！
				return ErrCodeConstants.API_PAX_LASTFIRST_CHINESE_ERROR;
			}
			if (Pattern.matches(REG_ENGLISH, lastname) && !Pattern.matches(REG_ENGLISH, firstname)) {
				// 姓为字母时，名应为字母！
				return ErrCodeConstants.API_PAX_LASTFIRST_ENGLISH_ERROR;
			}
			if (Pattern.matches(REG_CHINESE, firstname) && !Pattern.matches(REG_CHINESE, lastname)) {
				// 名为中文时，姓应为中文！
				return ErrCodeConstants.API_PAX_FIRSTLAST_CHINESE_ERROR;
			}
			if (Pattern.matches(REG_ENGLISH, firstname) && !Pattern.matches(REG_ENGLISH, lastname)) {
				// 名为字母时，姓应为字母！
				return ErrCodeConstants.API_PAX_FIRSTLAST_ENGLISH_ERROR;
			}

		}
		// if (!"".equals(gender)) {
		// if (!"F".equalsIgnoreCase(gender) && !"M".equalsIgnoreCase(gender)) {
		// //性别信息有误！
		// msg = ErrCodeConstants.API_USER_GENDER_ERROR;
		// return msg;
		// }
		// }
		// if (!"".equals(idtype)) {
		// if (!"NI".equalsIgnoreCase(idtype) && !"PP".equalsIgnoreCase(idtype)
		// && !"OT".equalsIgnoreCase(idtype)) {
		// //证件类型信息有误！
		// msg = ErrCodeConstants.API_IDENTITY_TYPE_ERROR;
		// return msg;
		// }
		// if ("NI".equalsIgnoreCase(idtype)) {
		// if (!Unit.getValidIdCard(idno)) {
		// //身份证号不符合规范！
		// msg = ErrCodeConstants.API_IDENTITY_NO_REG_ERROR;
		// return msg;
		// }
		// if (!"".equals(birthday)) {
		// if (!Unit.getDate(idno.substring(6,
		// 14)).equals(Unit.getDate(birthday))) {
		// //生日与身份证号不符！
		// return ErrCodeConstants.API_PAX_PASSNOBIRTH_ERROR;
		// }
		// }
		// int gen = Integer.valueOf(idno.substring(16, 17)) % 2;
		// if(gen == 0) {
		// input.addParm("gender", "F");
		// }
		// else if(gen==1) {
		// input.addParm("gender", "M");
		// }
		// } else if ("PP".equalsIgnoreCase(idtype)) {
		// if (!Pattern.matches("^[A-Z][0-9]{8}$", idno)) {
		// //护照格式不正确!
		// return ErrCodeConstants.API_PASSPORT_LAYOUT_REG_ERROR;
		// }
		// }
		// }
		if (!"".equals(mobile)) {
			if (!Pattern.matches(REG_MOBILE, mobile)) {
				// 手机号不符合规范！
				msg = ErrCodeConstants.API_MOBILE_REG_ERROR;
				return msg;
			}
		}
		if (!"".equals(email)) {
			if (!Pattern.matches(REG_EMAIL, email)) {
				// 邮箱不符合规范！
				msg = ErrCodeConstants.API_EMAIL_REG_ERROR;
				return msg;
			}
			if (email.length() > 50) {
				return ErrCodeConstants.API_EMAIL_TOOLONG_IN50;
			}
		}

		return msg;
	}

	private String checkcontact(CommandData input) {
		String type = input.getParm("type").getStringColumn();
		String no = input.getParm("no").getStringColumn();
		if (!"".equals(type)) {
			if (!"tel".equals(type) && !"mobile".equals(type) && !"email".equals(type) && !"qq".equals(type)
					&& !"webchat".equals(type)) {
				// 联系类别有误！
				return ErrCodeConstants.API_CONTACT_TYPE_ERROR;
			}
			if ("mobile".equals(type)) {
				if (!Pattern.matches(REG_MOBILE, no)) {
					// 手机号不符合规范！
					return ErrCodeConstants.API_MOBILE_REG_ERROR;
				}
			}
			if ("email".equals(type) && !"".equals(no)) {
				if (!Pattern.matches(REG_EMAIL, no)) {
					// 邮箱不符合规范！
					return ErrCodeConstants.API_EMAIL_REG_ERROR;
				}
				if (no.length() > 50) {
					// 邮箱长度过长，应不超过50位！
					return ErrCodeConstants.API_EMAIL_TOOLONG_IN50;
				}
			}
		}
		return "";
	}

	private String checkair(CommandData input) {
		String email = input.getParm("email").getStringColumn();
		String orgcity = input.getParm("orgcity").getStringColumn().toUpperCase();
		String dstcity = input.getParm("dstcity").getStringColumn().toUpperCase();
		String price = input.getParm("price").getStringColumn();
		String mobilephone = input.getParm("mobilephone").getStringColumn();
		String noticetype = input.getParm("noticetype").getStringColumn();
		Date startdat = input.getParm("startdat").getDateColumn();
		Date enddate = input.getParm("enddate").getDateColumn();
		Date now = Unit.getDate(Unit.getString(new Date(), "yyyy-MM-dd"), "yyyy-MM-dd");
		if (null != startdat) {
			if (startdat.before(now)) {
				// 出发开始日期小于当前日期！
				return ErrCodeConstants.API_FLIGHTORG_STDATE_LESS_CURDATE;
			}
			if (null != enddate && startdat.after(enddate)) {
				// 出发结束日期小于出发开始日期！
				return ErrCodeConstants.API_FLIGHTORG_EDDATE_LESS_STDATE;
			}
		}
		if (!"".equals(price)) {
			if (!Pattern.matches(REG_INT, price) || price.length() > 20) {
				// 价格有误!应为正整数，且长度不超过20!
				return ErrCodeConstants.API_PRICE_ERROR_PLUSNUM_IN20;
			}
		}
		if (!"".equals(orgcity)) {
			if (!Pattern.matches(REG_ENGLISH, orgcity) || orgcity.length() != 3) {
				// 出发城市有误!应为字母，且长度为3!
				return ErrCodeConstants.API_ORICITY_ERROR_LETTER_IN3;
			}
		}
		if (!"".equals(dstcity)) {
			if (!Pattern.matches(REG_ENGLISH, dstcity) || dstcity.length() != 3) {
				// 到达城市有误!应为字母，且长度为3!
				return ErrCodeConstants.API_DSTCITY_ERROR_LETTER_IN3;
			}
		}
		if (!"".equals(noticetype)) {
			if (!"1".equals(noticetype) && !"2".equals(noticetype) && !"3".equals(noticetype)) {
				// 通知方式有误！
				return ErrCodeConstants.API_NOTICE_WAY_ERROR;
			}
			if ("1".equals(noticetype)) {
				if ("".equals(mobilephone)) {
					// 手机号不能为空！
					return ErrCodeConstants.API_NULL_MOBILE;
				} else {
					if (!Pattern.matches(REG_MOBILE, mobilephone)) {
						// 手机号不符合规范！
						return ErrCodeConstants.API_MOBILE_REG_ERROR;
					}
				}
			}
			if ("2".equals(noticetype)) {
				if ("".equals(email)) {
					// 邮箱不能为空！
					return ErrCodeConstants.API_NULL_USER_EMAIL;
				} else {
					if (!Pattern.matches(REG_EMAIL, email)) {
						// 邮箱不符合规范！
						return ErrCodeConstants.API_EMAIL_REG_ERROR;
					}
					if (email.length() > 50) {
						// 邮箱长度过长，应不超过50位！
						return ErrCodeConstants.API_EMAIL_TOOLONG_IN50;
					}
				}
			}

			if ("3".equals(noticetype)) {
				if ("".equals(mobilephone)) {
					// 手机号不能为空！
					return ErrCodeConstants.API_NULL_MOBILE;
				} else {
					if (!Pattern.matches(REG_MOBILE, mobilephone)) {
						// 手机号不符合规范！
						return ErrCodeConstants.API_MOBILE_REG_ERROR;
					}
				}
				if ("".equals(email)) {
					// 邮箱不能为空！
					return ErrCodeConstants.API_NULL_USER_EMAIL;
				} else {
					if (!Pattern.matches(REG_EMAIL, email)) {
						// 邮箱不符合规范！
						return ErrCodeConstants.API_EMAIL_REG_ERROR;
					}
					if (email.length() > 50) {
						// 邮箱长度过长，应不超过50位！
						return ErrCodeConstants.API_EMAIL_TOOLONG_IN50;
					}
				}
			}
		}
		return "";
	}

	/**
	 * 查询APP配置信息
	 * 
	 * @param code
	 * @param type
	 * @return
	 * @throws APIException
	 */
	public String getAppCacheValue(String code, String type) throws APIException {
		String value = null;
		if (StringUtils.isEmpty(code) || StringUtils.isEmpty(type)) {
			return value;
		}
		value = RedisManager.getManager().get(RedisNamespaceEnum.api_cache_code.toKey(code));
		if (!StringUtils.hasLength(value)) {
			// 先调用res接口，判断，再将值存到redis中
			CommandInput commandInput = new CommandInput("com.cares.sh.order.config.query");
			commandInput.addParm("type", type);
			commandInput.addParm("code", code);
			CommandRet commandRet = ApiServletHolder.get().doOther(commandInput, true);

			Table l_tabe = commandRet.getParm("configs").getTableColumn();
			if (l_tabe != null && l_tabe.getRowCount() > 0) {
				value = l_tabe.getRow(0).getColumn("data").toString();
			}

			if (StringUtils.hasLength(value)) {
				RedisManager.getManager().set(RedisNamespaceEnum.api_cache_code.toKey(code), value, APICacheHelper.CACHE_TIMEOUT);
			}
		}
		return value;
	}
}
