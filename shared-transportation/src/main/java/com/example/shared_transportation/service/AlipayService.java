package com.example.shared_transportation.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayFundTransUniTransferRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayFundTransUniTransferResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 支付宝支付服务（官方 SDK 实现） 当 app-id 为空时自动进入「模拟支付」模式，便于开发联调。 */
@Service
public class AlipayService {

  private static final Logger log = LoggerFactory.getLogger(AlipayService.class);

  private final String appId;
  private final String privateKey;
  private final String alipayPublicKey;
  private final String gateway;
  private final String notifyUrl;
  private final String returnUrl;

  private final AlipayClient alipayClient;
  private final boolean isConfigured;

  public AlipayService(
      @Value("${alipay.app-id:}") String appId,
      @Value("${alipay.private-key:}") String privateKey,
      @Value("${alipay.alipay-public-key:}") String alipayPublicKey,
      @Value("${alipay.gateway:}") String gateway,
      @Value("${alipay.notify-url:}") String notifyUrl,
      @Value("${alipay.return-url:}") String returnUrl) {
    this.appId = appId;
    this.privateKey = privateKey;
    this.alipayPublicKey = alipayPublicKey;
    this.gateway = gateway;
    this.notifyUrl = notifyUrl;
    this.returnUrl = returnUrl;

    boolean hasConfig =
        appId != null
            && !appId.isBlank()
            && privateKey != null
            && !privateKey.isBlank()
            && alipayPublicKey != null
            && !alipayPublicKey.isBlank();

    if (hasConfig) {
      this.alipayClient =
          new DefaultAlipayClient(
              gateway, appId, privateKey, "json", "UTF-8", alipayPublicKey, "RSA2");
      this.isConfigured = true;
      log.info("✅ 支付宝支付已配置（真实模式），APPID: {}", appId);
      log.info("=== 支付宝配置加载详情 ===");
      log.info("app-id: {}", appId);
      log.info("gateway: {}", gateway);
      log.info("notify-url: {}", notifyUrl);
      log.info("==========================");
    } else {
      this.alipayClient = null;
      this.isConfigured = false;
      log.info("⚠️ 支付宝支付未配置，进入【模拟支付模式】");
    }
  }

  /** 当面付预下单，返回二维码内容（qr_code）或模拟数据。 */
  public String precreate(String outTradeNo, BigDecimal amount, String subject) {
    if (!isConfigured) {
      log.info("模拟支付：订单号={}, 金额={}, 商品={}", outTradeNo, amount, subject);
      return "https://qr.alipay.com/mock/" + UUID.randomUUID();
    }

    try {
      AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
      // 确保 notifyUrl 包含完整路径（例如 /api/pay/notify）
      request.setNotifyUrl(notifyUrl);
      request.setReturnUrl(returnUrl);
      request.setBizContent(
          String.format(
              "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\"}",
              outTradeNo, amount.toPlainString(), subject));

      AlipayTradePrecreateResponse response = alipayClient.execute(request);

      // 🔥 关键：打印支付宝返回的完整 JSON 响应体，便于排查
      log.info("📦 支付宝预下单完整响应体: {}", response.getBody());

      if (response.isSuccess()) {
        log.info("✅ 预下单成功，订单号: {}, 二维码: {}", outTradeNo, response.getQrCode());
        return response.getQrCode();
      } else {
        // 业务失败，抛出包含详细错误码的异常
        String errMsg =
            String.format(
                "❌ 预下单失败: code=%s, msg=%s, sub_code=%s, sub_msg=%s",
                response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
        log.error(errMsg);
        throw new RuntimeException(errMsg);
      }
    } catch (AlipayApiException e) {
      log.error("❌ 支付宝 API 调用异常", e);
      throw new RuntimeException("调用支付宝预下单接口失败: " + e.getErrMsg(), e);
    }
  }

  /** 查询订单交易状态（trade_status）。 */
  public String queryTrade(String outTradeNo) {
    if (!isConfigured) {
      log.info("模拟查询：订单号={}, 返回 TRADE_SUCCESS", outTradeNo);
      return "TRADE_SUCCESS";
    }

    try {
      AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
      request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", outTradeNo));

      AlipayTradeQueryResponse response = alipayClient.execute(request);
      log.info("📦 查询订单响应体: {}", response.getBody()); // 增加响应体日志

      if (response.isSuccess()) {
        return response.getTradeStatus();
      } else {
        log.error(
            "查询订单失败: code={}, msg={}, sub_msg={}",
            response.getCode(),
            response.getMsg(),
            response.getSubMsg());
        // 订单不存在返回 null，由调用方处理
        return null;
      }
    } catch (AlipayApiException e) {
      log.error("查询订单异常", e);
      throw new RuntimeException("查询订单失败: " + e.getErrMsg(), e);
    }
  }

  /** 单笔转账到支付宝账户（需签约功能）。 */
  public boolean transfer(BigDecimal amount, String alipayAccount, String outBizNo) {
    if (!isConfigured) {
      log.info("模拟转账：金额={}, 账户={}, 业务号={}", amount, alipayAccount, outBizNo);
      return true;
    }

    try {
      AlipayFundTransUniTransferRequest request = new AlipayFundTransUniTransferRequest();
      String bizContent =
          String.format(
              "{\"out_biz_no\":\"%s\",\"trans_amount\":\"%s\",\"product_code\":\"TRANS_ACCOUNT_NO_PWD\","
                  + "\"biz_scene\":\"DIRECT_TRANSFER\",\"payee_info\":{\"identity\":\"%s\",\"identity_type\":\"ALIPAY_LOGON_ID\"}}",
              outBizNo, amount.toPlainString(), alipayAccount);
      request.setBizContent(bizContent);

      AlipayFundTransUniTransferResponse response = alipayClient.execute(request);
      log.info("📦 转账响应体: {}", response.getBody());

      if (response.isSuccess()) {
        log.info("转账成功，业务号: {}", outBizNo);
        return true;
      } else {
        log.error(
            "转账失败: code={}, msg={}, sub_msg={}",
            response.getCode(),
            response.getMsg(),
            response.getSubMsg());
        return false;
      }
    } catch (AlipayApiException e) {
      log.error("转账异常", e);
      return false;
    }
  }

  /** 校验支付宝异步通知签名（使用 SDK 自带验签工具）。 */
  public boolean verifyNotify(Map<String, String> params) {
    if (!isConfigured) {
      return true;
    }
    try {
      return com.alipay.api.internal.util.AlipaySignature.rsaCheckV1(
          params, alipayPublicKey, "UTF-8", "RSA2");
    } catch (AlipayApiException e) {
      log.error("验签失败", e);
      return false;
    }
  }

  /** 判断当前是否为真实支付模式。 */
  public boolean isRealMode() {
    return isConfigured;
  }

  /** 兼容旧代码的 isConfigured 方法。 */
  public boolean isConfigured() {
    return isConfigured;
  }
}
