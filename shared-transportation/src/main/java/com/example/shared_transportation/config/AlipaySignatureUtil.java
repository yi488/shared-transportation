package com.example.shared_transportation.config;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付宝 RSA2（SHA256withRSA）签名 / 验签工具。
 * 参数按 key 字典序排序，排除 sign / sign_type，拼接 k=v&k=v 后签名。
 */
public final class AlipaySignatureUtil {

    private AlipaySignatureUtil() {
    }

    public static String sign(Map<String, String> params, String privateKey) {
        String content = buildContent(params);
        return rsaSign(content, privateKey);
    }

    public static boolean verify(Map<String, String> params, String publicKey, String sign) {
        if (sign == null || sign.isBlank()) {
            return false;
        }
        String content = buildContent(params);
        return rsaVerify(content, publicKey, sign);
    }

    private static String buildContent(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !"sign".equals(e.getKey()) && !"sign_type".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String rsaSign(String content, String privateKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(spec);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付宝签名失败", e);
        }
    }

    private static boolean rsaVerify(String content, String publicKey, String sign) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(spec);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(key);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }
}
