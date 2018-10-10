package com.travelsky.quick.util.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cares.sh.parm.CommandRet;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.util.helper.TipMessager;

/**
 *  类说明:验证基础功能接口<br>
 * 包括
 * <ul>
 * 	<li>空检查</li>
 * <li>并发检查</li>
 * <li>签名检查</li>
 * <li>Service授权检查</li>
 * <li>白名单检查</li>
 * </ul>
 * @author huxizhun
 *
 */
public abstract class AbstractBaseValidator implements BaseValidate {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseValidator.class);

	public boolean validate() {
		try {
			// 验证
			checkNull();
			// 验证白名单
//			checkWhiteList();
			// 验证签名
			checkSign();
			// 验证授权
			checkServiceAuth();
			// 验证并发数
			checkConcurrentNum();
			// 验证黑名单
			//checkBlackList();
			// 验证用户状态
			//checkUserStatus();
			return true;
		} catch (APIException ex) {
			String errCode = ex.getErrorCode();
			CommandRet ret = new CommandRet("");
			ret.setError(errCode,
					TipMessager.getMessage(errCode, ApiServletHolder.getApiContext().getLanguage()));
			ApiServletHolder.get().setRet(ret);
			return false;
		} catch (Exception ex) {
			String language = ApiServletHolder.getApiContext().getLanguage();
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_SYSTEM, language), ex);

			CommandRet ret = new CommandRet("");
			ret.setError(ErrCodeConstants.API_SYSTEM,
					TipMessager.getMessage(ErrCodeConstants.API_SYSTEM, language));
			ApiServletHolder.get().setRet(ret);
			return false;
		}

	}
}
