package com.kizuna.features.payment.config;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * SSLContext chỉ dùng cho kết nối tới VNPay (querydr).
 * Tin kho chứng chỉ mặc định của JDK VÀ (trên Windows) kho "Windows-ROOT" của hệ điều hành.
 * Lý do: chứng chỉ sandbox.vnpayment.vn nối lên "Sectigo Public Server Authentication Root R46", không có trong
 * cacerts của một số bản JDK 17 cũ (PKIX path building failed). Không tắt kiểm tra chứng chỉ, không đổi cấu hình JVM chung.
 */
@Slf4j
public final class VnpayTrust {

    private VnpayTrust() {
    }

    public static SSLContext sslContext() {
        try {
            List<X509TrustManager> managers = new ArrayList<>();
            managers.add(trustManager(null));
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                try {
                    KeyStore windows = KeyStore.getInstance("Windows-ROOT");
                    windows.load(null, null);
                    managers.add(trustManager(windows));
                } catch (Exception e) {
                    log.debug("Không đọc được kho chứng chỉ Windows-ROOT: {}", e.getMessage());
                }
            }
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new CompositeTrustManager(managers)}, null);
            return context;
        } catch (Exception e) {
            log.warn("Không tạo được SSLContext riêng cho VNPay, dùng mặc định của JDK: {}", e.getMessage());
            try {
                return SSLContext.getDefault();
            } catch (Exception ex) {
                throw new IllegalStateException("Không khởi tạo được kết nối bảo mật", ex);
            }
        }
    }

    private static X509TrustManager trustManager(KeyStore store) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        for (TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new IllegalStateException("Không có X509TrustManager");
    }

    /** Chấp nhận chứng chỉ máy chủ nếu MỘT trong các kho tin cậy xác thực được. */
    private record CompositeTrustManager(List<X509TrustManager> delegates) implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegates.get(0).checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager delegate : delegates) {
                try {
                    delegate.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    last = e;
                }
            }
            throw last != null ? last : new CertificateException("Chứng chỉ máy chủ không được tin cậy");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegates.stream().flatMap(d -> java.util.Arrays.stream(d.getAcceptedIssuers()))
                    .toArray(X509Certificate[]::new);
        }
    }
}
