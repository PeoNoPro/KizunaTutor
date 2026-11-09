package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Cấu hình dùng chung đã nối vào nơi sử dụng thật: giới hạn tệp đính kèm hỗ trợ và email người gửi thư cho nhân viên. */
@Service @RequiredArgsConstructor
public class SystemSettingService {
    public static final String ATTACHMENT_KEY="SUPPORT_ATTACHMENT_MAX_MB";
    public static final String SENDER_KEY="STAFF_MAIL_SENDER_EMAIL";
    /** Tài khoản SMTP riêng do Admin nhập (mật khẩu ứng dụng mã hóa); không có thì gửi qua tài khoản SMTP của server. */
    private static final String SMTP_HOST_KEY="MAIL_SMTP_HOST", SMTP_PORT_KEY="MAIL_SMTP_PORT", SMTP_PASSWORD_KEY="MAIL_SMTP_PASSWORD";
    /** Lịch sử AI ghi một dòng cho cả cặp nhà cung cấp + model; giá trị lưu ở hai khóa riêng bên dưới. */
    public static final String AI_KEY="AI_PROVIDER_MODEL";
    private static final String AI_PROVIDER_KEY="AI_PROVIDER", AI_MODEL_KEY="AI_MODEL", AI_BASE_URL_KEY="AI_BASE_URL", AI_KEY_PREFIX="AI_API_KEY_";
    /** CUSTOM: mọi dịch vụ có API theo chuẩn OpenAI (/chat/completions), ví dụ Groq, DeepSeek, OpenRouter, Mistral. */
    public static final List<String> AI_PROVIDERS=List.of("GEMINI","OPENAI","CUSTOM");
    private static final Map<String,String> AI_LABELS=Map.of("GEMINI","Google Gemini","OPENAI","OpenAI (ChatGPT)","CUSTOM","Nhà cung cấp khác (chuẩn OpenAI)");
    private static final String OPENAI_BASE_URL="https://api.openai.com/v1";
    /** Bằng giới hạn multipart của server; tăng cao hơn phải sửa cấu hình triển khai. */
    public static final int ATTACHMENT_CEILING_MB=10;
    /** Cột before/after của lịch sử cấu hình dài 100 ký tự. */
    public static final int SENDER_MAX_LENGTH=100;
    private static final Map<String,String> LABELS=Map.of(ATTACHMENT_KEY,"Giới hạn tệp đính kèm hỗ trợ",SENDER_KEY,"Tài khoản email gửi thư hệ thống",AI_KEY,"AI hỗ trợ học tập");
    private final EntityManager em;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor locks;
    private final Clock operationsClock;
    /** Email mặc định khi Admin chưa cấu hình: tài khoản SMTP trong cấu hình triển khai. */
    @Value("${spring.mail.username:}") private String defaultSender;
    /** Mặc định khi Admin chưa chọn; API key chỉ nằm trong cấu hình server, không lưu DB và không trả ra giao diện. */
    @Value("${app.ai.provider:GEMINI}") private String defaultAiProvider;
    @Value("${app.ai.gemini-model:gemini-2.5-flash}") private String defaultGeminiModel;
    @Value("${app.ai.openai-model:gpt-4o-mini}") private String defaultOpenAiModel;
    @Value("${app.ai.gemini-api-key:}") private String geminiKey;
    @Value("${app.ai.openai-api-key:}") private String openAiKey;
    /** Khóa mã hóa API key lưu trong DB; mặc định lấy từ bí mật JWT của server. */
    @Value("${app.settings.secret-key:${app.jwt.secret:}}") private String settingsSecret;

    /** Nhà cung cấp, model, API key (đã giải mã, chỉ dùng trong server) và địa chỉ API đang dùng cho chấm bài viết. */
    public record AiChoice(String provider,String model,String apiKey,String baseUrl) {
        @Override public String toString() {return "AiChoice[provider="+provider+", model="+model+", baseUrl="+baseUrl+"]";}
    }

    public record AttachmentLimitUpdate(
        @NotNull(message="Vui lòng nhập giới hạn.") @Min(value=1,message="Giới hạn tối thiểu là 1 MB.") @Max(value=ATTACHMENT_CEILING_MB,message="Giới hạn tối đa là 10 MB.") Integer maxMegabytes,
        @NotNull(message="Vui lòng tải lại dữ liệu.") Long expectedVersion,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=2000,message="Lý do tối đa 2000 ký tự.") String reason) { }

    /** Tài khoản đăng nhập máy chủ gửi thư; mật khẩu chỉ dùng trong server, không trả ra giao diện. */
    public record MailAccount(String host,int port,String username,String password) {
        @Override public String toString() {return "MailAccount[host="+host+", port="+port+", username="+username+"]";}
    }

    public record MailSenderUpdate(
        @NotBlank(message="Vui lòng nhập email người gửi.") @Email(message="Email người gửi chưa đúng định dạng.") @Size(max=SENDER_MAX_LENGTH,message="Email người gửi tối đa 100 ký tự.") String senderEmail,
        /** Để trống thì giữ mật khẩu đang có (nếu cùng email). */
        @Size(max=200,message="Mật khẩu ứng dụng tối đa 200 ký tự.") String appPassword,
        @Size(max=100) @Pattern(regexp="^$|^[A-Za-z0-9.-]+$",message="Máy chủ SMTP chưa đúng định dạng.") String smtpHost,
        @Min(value=1,message="Cổng SMTP không hợp lệ.") @Max(value=65535,message="Cổng SMTP không hợp lệ.") Integer smtpPort,
        @NotNull(message="Vui lòng tải lại dữ liệu.") Long expectedVersion,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=2000,message="Lý do tối đa 2000 ký tự.") String reason) { }

    public record AiUpdate(
        @NotBlank(message="Vui lòng chọn nhà cung cấp AI.") @Pattern(regexp="GEMINI|OPENAI|CUSTOM",message="Nhà cung cấp AI chưa được hỗ trợ.") String provider,
        @NotBlank(message="Vui lòng nhập tên model.") @Pattern(regexp="[A-Za-z0-9._:/-]{1,80}",message="Tên model chỉ gồm chữ, số và các ký tự . _ : / -, tối đa 80 ký tự.") String model,
        /** Để trống thì giữ API key đang có. */
        @Size(max=400,message="API key tối đa 400 ký tự.") @Pattern(regexp="^$|^[\\x21-\\x7E]+$",message="API key không được có khoảng trắng hoặc ký tự có dấu.") String apiKey,
        @Size(max=200,message="Địa chỉ API tối đa 200 ký tự.") String baseUrl,
        @NotNull(message="Vui lòng tải lại dữ liệu.") Long expectedVersion,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=2000,message="Lý do tối đa 2000 ký tự.") String reason) { }

    /** Thử kết nối AI bằng giá trị đang nhập trên form (chưa lưu). API key để trống thì dùng key đã lưu. */
    public record AiTest(
        @NotBlank(message="Vui lòng chọn nhà cung cấp AI.") @Pattern(regexp="GEMINI|OPENAI|CUSTOM",message="Nhà cung cấp AI chưa được hỗ trợ.") String provider,
        @NotBlank(message="Vui lòng nhập tên model.") @Pattern(regexp="[A-Za-z0-9._:/-]{1,80}",message="Tên model chỉ gồm chữ, số và các ký tự . _ : / -, tối đa 80 ký tự.") String model,
        @Size(max=400,message="API key tối đa 400 ký tự.") String apiKey,
        @Size(max=200,message="Địa chỉ máy chủ AI tối đa 200 ký tự.") String baseUrl) { }

    private Optional<String> value(String key) {
        var rows=em.createNativeQuery("SELECT config_value FROM system_configurations WHERE config_key=:key").setParameter("key",key).getResultList();
        return rows.isEmpty()||rows.get(0)==null?Optional.empty():Optional.of(rows.get(0).toString().trim()).filter(text->!text.isEmpty());
    }
    private int currentMegabytes() {
        try {
            int value=Integer.parseInt(value(ATTACHMENT_KEY).orElse(String.valueOf(ATTACHMENT_CEILING_MB)));
            return value<1||value>ATTACHMENT_CEILING_MB?ATTACHMENT_CEILING_MB:value;
        } catch(NumberFormatException exception) {return ATTACHMENT_CEILING_MB;}
    }
    private String fallbackSender() {return defaultSender==null?"":defaultSender.trim();}
    private String currentSender() {return value(SENDER_KEY).orElse(fallbackSender());}
    private long version(String key) {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM system_setting_audit_events WHERE setting_key=:key").setParameter("key",key).getSingleResult()).longValue();
    }

    /** Khóa chung cho mọi lần lưu cấu hình, rồi so phiên bản để không ghi đè thay đổi của người khác. */
    private com.kizuna.features.admin.dto.OperationsActor lockFor(String key,long expectedVersion) {
        var actor=actors.requireAdmin();locks.lockAdmin(actor.userId());
        em.createNativeQuery("SELECT pg_advisory_xact_lock(11380492)").getSingleResult();
        if(version(key)!=expectedVersion)throw new BusinessException(409,"Cấu hình đã được người khác thay đổi. Vui lòng tải lại trước khi lưu.");
        return actor;
    }
    private void save(String key,String type,String description,String before,String after,UUID actor,String reason) {
        save(key,type,description,before,after,actor,reason,key,before,after);
    }
    /** Ghi giá trị vào khóa cấu hình và một dòng lịch sử (khóa lịch sử và nội dung trước/sau có thể khác khóa cấu hình). */
    private void save(String key,String type,String description,String before,String after,UUID actor,String reason,String auditKey,String auditBefore,String auditAfter) {
        var now=OffsetDateTime.now(operationsClock);
        em.createNativeQuery("INSERT INTO system_configurations(config_key,config_value,data_type,description,updated_at) VALUES (:key,:value,:type,:description,:now) ON CONFLICT(config_key) DO UPDATE SET config_value=excluded.config_value,updated_at=excluded.updated_at")
            .setParameter("key",key).setParameter("value",after).setParameter("type",type).setParameter("description",description).setParameter("now",now).executeUpdate();
        em.createNativeQuery("INSERT INTO system_setting_audit_events(event_id,setting_key,actor_user_id,before_value,after_value,reason,occurred_at) VALUES (:id,:key,:actor,:before,:after,:reason,:now)")
            .setParameter("id",UUID.randomUUID()).setParameter("key",auditKey).setParameter("actor",actor).setParameter("before",clip(auditBefore.isEmpty()?"Chưa cấu hình":auditBefore)).setParameter("after",clip(auditAfter)).setParameter("reason",reason.trim()).setParameter("now",now).executeUpdate();
    }

    private static String clip(String text) {return text.length()>100?text.substring(0,100):text;}

    /** Dùng cho nơi kiểm tra tệp: không đọc được cấu hình thì giữ mức trần đã có từ trước. */
    @Transactional(readOnly=true,timeout=3)
    public long attachmentMaxBytes() {
        return currentMegabytes()*1_000_000L;
    }

    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> attachmentLimit() {
        actors.requireAdmin();
        return Map.of("maxMegabytes",currentMegabytes(),"ceilingMegabytes",ATTACHMENT_CEILING_MB,"version",version(ATTACHMENT_KEY));
    }

    @Transactional(timeout=5)
    public Map<String,Object> updateAttachmentLimit(AttachmentLimitUpdate r) {
        var actor=lockFor(ATTACHMENT_KEY,r.expectedVersion());
        int before=currentMegabytes();
        if(before==r.maxMegabytes())throw new BusinessException(400,"Giá trị mới trùng với giá trị hiện hành.");
        save(ATTACHMENT_KEY,"NUMBER","Giới hạn mỗi tệp đính kèm yêu cầu hỗ trợ (MB)",String.valueOf(before),r.maxMegabytes().toString(),actor.userId(),r.reason());
        return attachmentLimit();
    }

    /** Email đặt ở ô From của thư gửi thông tin đăng nhập; lỗi đọc DB thì dùng email mặc định để thư vẫn gửi được. */
    @Transactional(readOnly=true,timeout=3)
    public String mailSenderEmail() {
        try {return currentSender();}
        catch(RuntimeException exception) {return fallbackSender();}
    }

    /** Đoán máy chủ SMTP theo tên miền email để Admin không phải tự nhập với các dịch vụ phổ biến. */
    static String guessSmtpHost(String email) {
        var domain=email==null||!email.contains("@")?"":email.substring(email.indexOf('@')+1).toLowerCase(Locale.ROOT);
        return switch(domain) {
            case "gmail.com","googlemail.com" -> "smtp.gmail.com";
            case "outlook.com","hotmail.com","live.com" -> "smtp.office365.com";
            case "yahoo.com","yahoo.com.vn" -> "smtp.mail.yahoo.com";
            default -> "";
        };
    }
    private Optional<String> storedSmtpPassword() {return value(SMTP_PASSWORD_KEY).map(this::decrypt).filter(text->!text.isBlank());}
    private int storedPort() {try {return Integer.parseInt(value(SMTP_PORT_KEY).orElse("587"));} catch(NumberFormatException exception) {return 587;}}

    /** Tài khoản Admin đã cấu hình; null nghĩa là gửi bằng tài khoản SMTP trong cấu hình server. */
    @Transactional(readOnly=true,timeout=3)
    public MailAccount mailAccount() {
        try {
            var password=storedSmtpPassword();if(password.isEmpty())return null;
            var sender=currentSender();
            var host=value(SMTP_HOST_KEY).orElse(guessSmtpHost(sender));
            return host.isBlank()?null:new MailAccount(host,storedPort(),sender,password.get());
        } catch(RuntimeException exception) {return null;}
    }

    /** Tạo bộ gửi thư cho tài khoản riêng; cùng thiết lập TLS và thời gian chờ với cấu hình server. */
    public static org.springframework.mail.javamail.JavaMailSenderImpl buildMailSender(MailAccount account) {
        var sender=new org.springframework.mail.javamail.JavaMailSenderImpl();
        sender.setHost(account.host());sender.setPort(account.port());sender.setUsername(account.username());sender.setPassword(account.password());sender.setDefaultEncoding("UTF-8");
        var props=sender.getJavaMailProperties();
        props.put("mail.smtp.auth","true");
        if(account.port()==465)props.put("mail.smtp.ssl.enable","true");
        else {props.put("mail.smtp.starttls.enable","true");props.put("mail.smtp.starttls.required","true");}
        props.put("mail.smtp.connectiontimeout","10000");props.put("mail.smtp.timeout","10000");props.put("mail.smtp.writetimeout","10000");
        return sender;
    }

    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> mailSender() {
        actors.requireAdmin();
        var configured=value(SENDER_KEY);
        var sender=configured.orElse(fallbackSender());
        var password=storedSmtpPassword();
        var result=new LinkedHashMap<String,Object>();
        result.put("senderEmail",sender);
        result.put("defaultEmail",fallbackSender());
        result.put("configured",configured.isPresent());
        result.put("ownAccount",password.isPresent());
        result.put("maskedPassword",password.map(SystemSettingService::mask).orElse(""));
        result.put("smtpHost",value(SMTP_HOST_KEY).orElse(guessSmtpHost(sender)));
        result.put("smtpPort",storedPort());
        result.put("version",version(SENDER_KEY));
        return result;
    }

    /** Thời gian chờ đủ cho bước đăng nhập thử máy chủ SMTP (tối đa khoảng 10 giây) trước khi lưu. */
    @Transactional(timeout=30)
    public Map<String,Object> updateMailSender(MailSenderUpdate r) {
        actors.requireAdmin();
        var after=r.senderEmail().trim().toLowerCase(Locale.ROOT);
        var newPassword=r.appPassword()==null?"":r.appPassword().replace(" ","");
        var host=r.smtpHost()==null||r.smtpHost().isBlank()?guessSmtpHost(after):r.smtpHost().trim().toLowerCase(Locale.ROOT);
        int port=r.smtpPort()==null?587:r.smtpPort();
        // Đăng nhập thử trước khi khóa và lưu: cấu hình sai sẽ làm mọi thư của hệ thống (OTP, xác minh, thanh toán) không gửi được.
        // Báo trùng ngay, không bắt Admin chờ bước đăng nhập thử (vẫn kiểm tra lại sau khi khóa).
        var stored=value(SENDER_KEY).orElse(fallbackSender()).toLowerCase(Locale.ROOT);
        if(after.equals(stored)&&newPassword.isEmpty()&&host.equals(value(SMTP_HOST_KEY).orElse(guessSmtpHost(stored)))&&port==storedPort())
            throw new BusinessException(400,"Thông tin mới trùng với cấu hình hiện hành.");
        var plannedPassword=!newPassword.isEmpty()?newPassword:after.equals(currentSender().toLowerCase(Locale.ROOT))?storedSmtpPassword().orElse(""):"";
        if(!plannedPassword.isEmpty()&&!host.isBlank())verifyLogin(new MailAccount(host,port,after,plannedPassword));
        var actor=lockFor(SENDER_KEY,r.expectedVersion());
        var before=value(SENDER_KEY).orElse("");
        var current=before.isEmpty()?fallbackSender().toLowerCase(Locale.ROOT):before;
        boolean emailChanged=!after.equals(current);
        boolean serverChanged=!host.equals(value(SMTP_HOST_KEY).orElse(guessSmtpHost(current)))||port!=storedPort();
        if(!emailChanged&&newPassword.isEmpty()&&!serverChanged)throw new BusinessException(400,"Thông tin mới trùng với cấu hình hiện hành.");
        // Mật khẩu ứng dụng gắn với từng email: đổi email thì phải nhập mật khẩu của email mới.
        if(emailChanged&&newPassword.isEmpty()&&storedSmtpPassword().isPresent())throw new BusinessException(400,"Vui lòng nhập mật khẩu ứng dụng của email mới.");
        if(!newPassword.isEmpty()&&host.isBlank())throw new BusinessException(400,"Vui lòng nhập máy chủ SMTP của email này.");
        if(!newPassword.isEmpty())upsert(SMTP_PASSWORD_KEY,encrypt(newPassword),"Mật khẩu ứng dụng email gửi thư (đã mã hóa)");
        if(!host.isBlank())upsert(SMTP_HOST_KEY,host,"Máy chủ SMTP gửi thư");
        upsert(SMTP_PORT_KEY,String.valueOf(port),"Cổng SMTP gửi thư");
        save(SENDER_KEY,"STRING","Tài khoản email gửi thư hệ thống",before,after,actor.userId(),r.reason(),SENDER_KEY,before,after+(newPassword.isEmpty()?"":" · đổi mật khẩu ứng dụng"));
        return mailSender();
    }

    /** Chỉ đăng nhập máy chủ SMTP, không gửi thư. Lỗi thì không lưu; thông báo không chứa mật khẩu. */
    static void verifyLogin(MailAccount account) {
        try {buildMailSender(account).testConnection();}
        catch(jakarta.mail.AuthenticationFailedException exception) {
            throw new BusinessException(400,"Chưa lưu: sai email hoặc mật khẩu ứng dụng. Với Gmail phải dùng mật khẩu ứng dụng 16 ký tự, không dùng mật khẩu đăng nhập.");
        } catch(jakarta.mail.MessagingException|RuntimeException exception) {
            throw new BusinessException(400,"Chưa lưu: không đăng nhập được máy chủ SMTP "+account.host()+":"+account.port()+". Kiểm tra máy chủ và cổng.");
        }
    }

    private String defaultModel(String provider) {return "OPENAI".equals(provider)?defaultOpenAiModel:"GEMINI".equals(provider)?defaultGeminiModel:"";}
    private String defaultProvider() {
        var provider=defaultAiProvider==null?"GEMINI":defaultAiProvider.trim().toUpperCase(Locale.ROOT);
        return AI_PROVIDERS.contains(provider)?provider:"GEMINI";
    }
    private static final String MODEL_PATTERN="[A-Za-z0-9._:/-]{1,80}";
    private String providerNow() {return value(AI_PROVIDER_KEY).filter(AI_PROVIDERS::contains).orElse(defaultProvider());}
    private String serverKey(String provider) {var key="OPENAI".equals(provider)?openAiKey:"GEMINI".equals(provider)?geminiKey:null;return key==null?"":key.trim();}
    /** Key Admin nhập (đã giải mã) được ưu tiên; không có thì dùng key trong cấu hình server. */
    private String apiKey(String provider) {
        return value(AI_KEY_PREFIX+provider).map(this::decrypt).filter(key->!key.isBlank()).orElse(serverKey(provider));
    }
    private String baseUrl(String provider) {return "OPENAI".equals(provider)?OPENAI_BASE_URL:"CUSTOM".equals(provider)?value(AI_BASE_URL_KEY).orElse(""):"";}
    private AiChoice currentAi() {
        var provider=providerNow();
        var model=value(AI_MODEL_KEY).filter(text->text.matches(MODEL_PATTERN)).orElse(defaultModel(provider));
        return new AiChoice(provider,model,apiKey(provider),baseUrl(provider));
    }
    private static String mask(String key) {return key.isBlank()?"":"••••"+key.substring(Math.max(0,key.length()-4));}

    // ---- Mã hóa API key bằng AES-GCM; không ghi key ra log, lịch sử hay phản hồi API. ----
    private javax.crypto.spec.SecretKeySpec cipherKey() {
        if(settingsSecret==null||settingsSecret.isBlank())throw new BusinessException(500,"Server chưa có khóa mã hóa cấu hình nên chưa lưu được API key.");
        try {return new javax.crypto.spec.SecretKeySpec(java.security.MessageDigest.getInstance("SHA-256").digest(("kizuna-settings:"+settingsSecret).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"AES");}
        catch(java.security.GeneralSecurityException exception) {throw new IllegalStateException(exception);}
    }
    private String encrypt(String plain) {
        try {
            var iv=new byte[12];new java.security.SecureRandom().nextBytes(iv);
            var cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,cipherKey(),new javax.crypto.spec.GCMParameterSpec(128,iv));
            var out=cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var all=new byte[iv.length+out.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(out,0,all,iv.length,out.length);
            return "enc:"+Base64.getEncoder().encodeToString(all);
        } catch(java.security.GeneralSecurityException exception) {throw new IllegalStateException("Không mã hóa được API key.",exception);}
    }
    /** Không giải mã được (ví dụ server đổi khóa) thì coi như chưa có key, AI sẽ dùng key server hoặc bộ chấm dự phòng. */
    private String decrypt(String stored) {
        try {
            if(!stored.startsWith("enc:"))return "";
            var all=Base64.getDecoder().decode(stored.substring(4));
            var cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(javax.crypto.Cipher.DECRYPT_MODE,cipherKey(),new javax.crypto.spec.GCMParameterSpec(128,all,0,12));
            return new String(cipher.doFinal(all,12,all.length-12),java.nio.charset.StandardCharsets.UTF_8);
        } catch(RuntimeException|java.security.GeneralSecurityException exception) {return "";}
    }
    private void upsert(String key,String value,String description) {
        em.createNativeQuery("INSERT INTO system_configurations(config_key,config_value,data_type,description,updated_at) VALUES (:key,:value,'STRING',:description,:now) ON CONFLICT(config_key) DO UPDATE SET config_value=excluded.config_value,updated_at=excluded.updated_at")
            .setParameter("key",key).setParameter("value",value).setParameter("description",description).setParameter("now",OffsetDateTime.now(operationsClock)).executeUpdate();
    }

    /** Chỉ nhận https (hoặc máy local khi chạy thử) để không gửi key qua kết nối không mã hóa. */
    private static void checkBaseUrl(String url) {
        if(!url.matches("https://[A-Za-z0-9.-]+(:\\d+)?(/[A-Za-z0-9._~/-]*)?")&&!url.matches("http://(localhost|127\\.0\\.0\\.1)(:\\d+)?(/[A-Za-z0-9._~/-]*)?"))
            throw new BusinessException(400,"Địa chỉ máy chủ AI phải bắt đầu bằng https://, ví dụ https://api.groq.com/openai/v1.");
    }

    /**
     * Gửi một câu hỏi rất ngắn tới AI để Admin biết key, model và địa chỉ có dùng được không.
     * Thông báo lỗi chỉ nói nguyên nhân, không bao giờ chứa API key.
     */
    @Transactional(readOnly=true,timeout=40)
    public Map<String,Object> testAi(AiTest r) {
        actors.requireAdmin();
        var provider=r.provider();var model=r.model().trim();
        if("GEMINI".equals(provider)&&model.contains("/"))throw new BusinessException(400,"Tên model Gemini không chứa dấu /.");
        var key=r.apiKey()==null||r.apiKey().isBlank()?apiKey(provider):r.apiKey().trim();
        if(key.isBlank())throw new BusinessException(400,"Vui lòng nhập API key của "+AI_LABELS.get(provider)+" để thử.");
        var url="CUSTOM".equals(provider)?(r.baseUrl()==null||r.baseUrl().isBlank()?baseUrl("CUSTOM"):r.baseUrl().trim().replaceAll("/+$","")):baseUrl(provider);
        if("CUSTOM".equals(provider))checkBaseUrl(url);
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(5000);factory.setReadTimeout(30000);
        var rest=new org.springframework.web.client.RestTemplate(factory);
        var headers=new org.springframework.http.HttpHeaders();headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        var question="Trả lời đúng một câu ngắn bằng tiếng Việt: \"Kết nối thành công\".";
        long started=System.nanoTime();
        try {
            String reply;
            var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
            if("GEMINI".equals(provider)) {
                headers.set("x-goog-api-key",key);
                var body=Map.of("contents",List.of(Map.of("parts",List.of(Map.of("text",question)))));
                var response=rest.postForEntity("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent",new org.springframework.http.HttpEntity<>(body,headers),String.class);
                reply=mapper.readTree(response.getBody()).path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            } else {
                headers.setBearerAuth(key);
                var body=Map.of("model",model,"messages",List.of(Map.of("role","user","content",question)),"max_tokens",30);
                var response=rest.postForEntity(url+"/chat/completions",new org.springframework.http.HttpEntity<>(body,headers),String.class);
                reply=mapper.readTree(response.getBody()).path("choices").path(0).path("message").path("content").asText("");
            }
            long ms=(System.nanoTime()-started)/1_000_000;
            if(reply.isBlank())return Map.of("ok",false,"message","AI có phản hồi nhưng không có nội dung. Hãy kiểm tra lại tên model.","latencyMs",ms);
            return Map.of("ok",true,"message","Kết nối thành công với "+AI_LABELS.get(provider)+" · "+model+".","reply",reply.length()>200?reply.substring(0,200):reply.trim(),"latencyMs",ms);
        } catch(org.springframework.web.client.HttpStatusCodeException exception) {
            int status=exception.getStatusCode().value();
            var reason=status==400?"Yêu cầu không hợp lệ, thường do tên model sai.":status==401||status==403?"API key sai, hết hạn hoặc không có quyền dùng model này.":status==404?"Không tìm thấy model hoặc địa chỉ máy chủ AI sai.":status==429?"Đã hết hạn mức hoặc gửi quá nhiều yêu cầu. Thử lại sau ít phút.":status>=500?"Máy chủ AI đang lỗi. Thử lại sau.":"Máy chủ AI từ chối yêu cầu (mã "+status+").";
            return Map.of("ok",false,"message",reason,"latencyMs",(System.nanoTime()-started)/1_000_000);
        } catch(org.springframework.web.client.ResourceAccessException exception) {
            return Map.of("ok",false,"message","Không kết nối được tới máy chủ AI. Kiểm tra địa chỉ và kết nối mạng của server.","latencyMs",(System.nanoTime()-started)/1_000_000);
        } catch(Exception exception) {
            return Map.of("ok",false,"message","Không đọc được phản hồi của AI. Kiểm tra lại nhà cung cấp và model.","latencyMs",(System.nanoTime()-started)/1_000_000);
        }
    }

    /** Dùng khi gọi AI: lỗi đọc DB thì quay về cấu hình server để học viên vẫn dùng được. */
    @Transactional(readOnly=true,timeout=3)
    public AiChoice aiChoice() {
        try {return currentAi();}
        catch(RuntimeException exception) {var provider=defaultProvider();return new AiChoice(provider,defaultModel(provider),serverKey(provider),baseUrl(provider));}
    }

    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> ai() {
        actors.requireAdmin();
        var current=currentAi();
        var providers=AI_PROVIDERS.stream().map(code->{
            var key=apiKey(code);var fromAdmin=value(AI_KEY_PREFIX+code).map(this::decrypt).filter(k->!k.isBlank()).isPresent();
            var item=new LinkedHashMap<String,Object>();
            item.put("code",code);item.put("label",AI_LABELS.get(code));item.put("defaultModel",defaultModel(code));
            item.put("keyConfigured",!key.isBlank());item.put("keySource",key.isBlank()?"":fromAdmin?"admin":"server");item.put("maskedKey",mask(key));
            item.put("baseUrl",baseUrl(code));
            return item;
        }).toList();
        var result=new LinkedHashMap<String,Object>();
        result.put("provider",current.provider());
        result.put("providerLabel",AI_LABELS.get(current.provider()));
        result.put("model",current.model());
        result.put("keyConfigured",!current.apiKey().isBlank());
        result.put("providers",providers);
        result.put("version",version(AI_KEY));
        return result;
    }

    @Transactional(timeout=5)
    public Map<String,Object> updateAi(AiUpdate r) {
        var actor=lockFor(AI_KEY,r.expectedVersion());
        var before=currentAi();
        var model=r.model().trim();
        var newKey=r.apiKey()==null?"":r.apiKey().trim();
        var url=r.baseUrl()==null?"":r.baseUrl().trim().replaceAll("/+$","");
        if("CUSTOM".equals(r.provider()))checkBaseUrl(url);
        if("GEMINI".equals(r.provider())&&model.contains("/"))throw new BusinessException(400,"Tên model Gemini không chứa dấu /.");
        boolean urlChanged="CUSTOM".equals(r.provider())&&!url.equals(baseUrl("CUSTOM"));
        if(before.provider().equals(r.provider())&&before.model().equals(model)&&newKey.isEmpty()&&!urlChanged)
            throw new BusinessException(400,"Cấu hình AI mới trùng với cấu hình hiện hành.");
        if(newKey.isEmpty()&&apiKey(r.provider()).isBlank())throw new BusinessException(400,"Vui lòng nhập API key của "+AI_LABELS.get(r.provider())+".");
        if(!newKey.isEmpty())upsert(AI_KEY_PREFIX+r.provider(),encrypt(newKey),"API key AI (đã mã hóa)");
        if("CUSTOM".equals(r.provider()))upsert(AI_BASE_URL_KEY,url,"Địa chỉ API của nhà cung cấp AI khác");
        upsert(AI_PROVIDER_KEY,r.provider(),"Nhà cung cấp AI hỗ trợ học tập");
        var after=AI_LABELS.get(r.provider())+" · "+model+(newKey.isEmpty()?"":" · đổi API key");
        save(AI_MODEL_KEY,"STRING","Model AI hỗ trợ học tập",before.model(),model,actor.userId(),r.reason(),AI_KEY,AI_LABELS.get(before.provider())+" · "+before.model(),after);
        return ai();
    }

    @Transactional(readOnly=true,timeout=3)
    @SuppressWarnings("unchecked")
    public Map<String,Object> history(int page,int size) {
        actors.requireAdmin();int offset=OperationsValidation.page(page,size);
        var keys=List.copyOf(LABELS.keySet());
        var rows=(List<Object[]>)em.createNativeQuery("SELECT a.full_name,e.before_value,e.after_value,e.reason,e.occurred_at,e.setting_key FROM system_setting_audit_events e JOIN users a ON a.user_id=e.actor_user_id WHERE e.setting_key IN (:keys) ORDER BY e.occurred_at DESC,e.event_id DESC").setParameter("keys",keys).setFirstResult(offset).setMaxResults(size).getResultList();
        var items=rows.stream().map(r->Map.of("actorDisplayName",r[0],"before",r[1],"after",r[2],"reason",r[3],"occurredAt",r[4],"settingLabel",LABELS.get(r[5].toString()),"unit",ATTACHMENT_KEY.equals(r[5].toString())?"MB":"")).toList();
        long total=((Number)em.createNativeQuery("SELECT count(*) FROM system_setting_audit_events WHERE setting_key IN (:keys)").setParameter("keys",keys).getSingleResult()).longValue();
        return Map.of("items",items,"page",page,"size",size,"totalElements",total);
    }
}
