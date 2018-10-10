package com.travelsky.quick.util.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cares.sh.comm.BaseContext;
import com.travelsky.quick.common.ApiServletHolder;
import com.travelsky.quick.common.ErrCodeConstants;
import com.travelsky.quick.exception.APIException;
import com.travelsky.quick.util.helper.APICacheHelper;
import com.travelsky.quick.util.helper.TipMessager;

/**
 * 类说明:并发管理
 * @author huxizhun
 *
 */
public class APIConcurrentManager implements ConcurrentManageService {
	private static final Logger LOGGER = LoggerFactory.getLogger(APIConcurrentManager.class);
	private static final Lock LOCK = new ReentrantLock();
	private static final APIConcurrentManager MANAGER = new APIConcurrentManager();

	/**
	 * 管理员
	 * @return 管理员
	 */
	public static APIConcurrentManager getInstance() {
		return MANAGER;
	}

	@Override
	public void operate(ActionType actionType, String... keys)
					throws APIException, Exception {
		if (actionType.equals(ActionType.ADD)) {
			addConcurrent(keys);
		}
		else {
			subConcurrent(keys);
		}
	}

	/**
	 * 减少并发数
	 * @param context SelvetContext
	 * @param keys 要减少的并发数的key数组
	 * @throws APIException
	 */
	protected void subConcurrent(String... keys) throws APIException {
		checkParams(keys);
		String appID = keys[0];
		String serviceName = keys[1];
		String version = keys[2];

		APICacheHelper helper = new APICacheHelper();
		// app 当前并发
		int appCur;
		// service 当前并发
		int serviceCur;
		try {
			// 获取AppID + version当前并发数
			String appCurNum = helper.getAppCurConcurrent(appID, version);
			// 获取Service + version当前并发数
			String sevCurNum = helper.getAppCurConcurrent(serviceName, version);
			appCur = Integer.parseInt(appCurNum==null?"0":appCurNum);
			serviceCur = Integer.parseInt(sevCurNum==null?"0":sevCurNum);
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_PARSE_CONCURRENCY_NUM,
					ApiServletHolder.getApiContext().getLanguage()));
			throw e;
		}

		LOCK.lock();
		try {
			// App 减并发
			if (--appCur < 0) {
				// 重置
				helper.resetAppConcurrent(appID, version);
			}
			else {
				helper.decrAppConcurrent(appID, version);
			}

			// Service 减并发
			if (--serviceCur < 0) {
				// 重置
				helper.resetSevConcurrent(appID, serviceName, version);
			}
			else {
				helper.decrServiceConcurrent(appID, serviceName, version);
			}
		}
		finally {
			LOCK.unlock();
		}
	}

	/**
	 * 检查参数
	 * @param keys
	 * @throws APIException
	 */
	private void checkParams(String... keys) throws APIException {
		if (keys == null || keys.length<3) {
			// 告警
			// ......
			LOGGER.info(TipMessager.getInfoMessage(
					ErrCodeConstants.API_ENUM_CONCURRENCY,
					ApiServletHolder.getApiContext().getLanguage()));
			throw APIException.getInstance(ErrCodeConstants.API_SYSTEM);
		}
	}

	/**
	 * 增加并发数
	 * @param context SelvetContext
	 * @param keys key array
	 * @return void
	 * @throws APIException, Exception
	 */
	protected <T extends BaseContext> void addConcurrent(String... keys)
			throws APIException, Exception {
		checkParams(keys);
		String appID = keys[0];
		String serviceName = keys[1];
		String version = keys[2];
		if (!StringUtils.hasLength(version)) {
			version = "null";
		}

		APICacheHelper helper = new APICacheHelper();
		// app 最大并发
		int appMax;
		// app 当前并发
		int appCur;
		// service 最大并发
		int serviceMax;
		// service 当前并发
		int serviceCur;
		try {
			// 获取AppID + version最大并发数
			String appMaxNum = helper.getAppMaxConcurrent(appID, version);
			// 获取AppID + version当前并发数
			String appCurNum = helper.getAppCurConcurrent(appID, version);
			// 获取Service + version最大并发数
			String sevMaxNum = helper.getSevMaxConcurrent(appID,serviceName, version);
			// 获取Service + version当前并发数
			String sevCurNum = helper.getAppCurConcurrent(serviceName, version);
			if (!StringUtils.hasLength(appMaxNum)) {
				Exception e = new Exception("Can't get App max concurrent num");
				LOGGER.error("", e);
				throw e;
			}
			if (!StringUtils.hasLength(appCurNum)) {
				Exception e = new Exception("Can't get App current concurrent num");
				LOGGER.error("", e);
				throw e;
			}
			if (!StringUtils.hasLength(sevMaxNum)) {
				Exception e = new Exception("Can't get Service max concurrent num");
				LOGGER.error("", e);
				throw e;
			}
			if (!StringUtils.hasLength(sevCurNum)) {
				Exception e = new Exception("Can't get Service current concurrent num");
				LOGGER.error("", e);
				throw e;
			}
			appMax = Integer.parseInt(appMaxNum);
			appCur = Integer.parseInt(appCurNum);
			serviceMax = Integer.parseInt(sevMaxNum);
			serviceCur = Integer.parseInt(sevCurNum);
		}
		catch (Exception e) {
			LOGGER.error(TipMessager.getInfoMessage(
					ErrCodeConstants.API_PARSE_CONCURRENCY_NUM,
					ApiServletHolder.getApiContext().getLanguage()));
			throw e;
		}

		LOCK.lock();
		try {
			// App 并发数超限
			if (++appCur > appMax) {
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_APPID_OUT_CONCURRENCY,
						ApiServletHolder.getApiContext().getLanguage()));
				throw new APIException(ErrCodeConstants.API_APPID_OUT_CONCURRENCY);
			}

			// Service 并发数超限
			if (++serviceCur > serviceMax) {
				LOGGER.info(TipMessager.getInfoMessage(
						ErrCodeConstants.API_SERVICE_OUT_CONCURRENCY,
						ApiServletHolder.getApiContext().getLanguage()));
				throw new APIException(ErrCodeConstants.API_SERVICE_OUT_CONCURRENCY);
			}

			helper.incrAppConcurrent(appID, version);
			helper.incrServiceConcurrent(appID, serviceName, version);
		}
		finally {
			LOCK.unlock();
		}
	}

	@Override
	/**
	 * 验证并发数，如果验证通过，并发数加1
	 * @param keys 关键字
	 * @throws APIRuntimeException 运行时异常
	 * @throws Exception  异常
	 */
	public void validate(String... keys)
			throws APIException, Exception {
		addConcurrent(keys);
	}

	/**
	 * 重置并发数
	 */
	protected void reset() {
		try {
			LOCK.lock();
		}
		finally {
			LOCK.unlock();
		}
	}
}
