package com.kizuna.features.ticket.service;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
/** All untrusted parsing is in an OS-capped subprocess. No in-JVM fallback. */
@Service
public class TicketAttachmentValidator {
 @org.springframework.beans.factory.annotation.Value("${app.ticket.parser.classpath:}") private String configuredClasspath="";
 public static final long DEFAULT_MAX_BYTES=10_000_000L;
 @org.springframework.beans.factory.annotation.Autowired(required=false) private com.kizuna.features.admin.service.SystemSettingService settings;
 /** Admin chỉnh ở Cấu hình hệ thống; không đọc được thì giữ mức mặc định thay vì chặn hỗ trợ. */
 public long maxBytes(){if(settings==null)return DEFAULT_MAX_BYTES;try{return settings.attachmentMaxBytes();}catch(RuntimeException e){return DEFAULT_MAX_BYTES;}}
 public record ValidatedFile(String name,String mime,byte[] bytes){}
 public List<ValidatedFile> validate(List<MultipartFile> files){
   if(files==null||files.isEmpty())return List.of();
   if(files.size()>5)throw new BusinessException(400,"Mỗi lần gửi chỉ được đính kèm tối đa 5 tệp.");
   long limit=maxBytes();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);var result=new ArrayList<ValidatedFile>();
   for(var file:files){
     if(file.getSize()<1||file.getSize()>limit)throw new BusinessException(400,"Mỗi tệp phải có dung lượng từ 1 byte đến "+limit/1_000_000+" MB.");
     String name=Objects.toString(file.getOriginalFilename(),"").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cc}\\p{Cf}]","");
     if(name.isBlank())throw new BusinessException(400,"Tên tệp chưa hợp lệ.");
     if(name.codePointCount(0,name.length())>255)name=name.substring(0,name.offsetByCodePoints(0,255));
     String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
     String mime=switch(ext){case "jpg","jpeg"->"image/jpeg";case "png"->"image/png";case "pdf"->"application/pdf";default->throw new BusinessException(400,"Chỉ hỗ trợ tệp JPG, PNG và PDF.");};
     if(!mime.equals(file.getContentType()))throw new BusinessException(400,"Định dạng tệp không khớp. Vui lòng chọn lại tệp gốc.");
     Path temp=null,helper=null;Process process=null;
     try{
       if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"))throw new BusinessException(400,"Chưa thể kiểm tra tệp an toàn trên môi trường này. Bạn có thể gửi nội dung văn bản trước.");
       temp=Files.createTempFile("kzt-ticket-",".bin");file.transferTo(temp);
       helper=Files.createTempFile("kzt-ticket-parser-",".ps1");
       try(var source=getClass().getResourceAsStream("/ticket-parser-job.ps1")){if(source==null)throw new java.io.IOException();Files.copy(source,helper,StandardCopyOption.REPLACE_EXISTING);}
       long remaining=Math.min(10000,TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime()));if(remaining<1)throw new java.io.IOException();
       String classpath=configuredClasspath.isBlank()?System.getProperty("java.class.path"):configuredClasspath;
       process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File",helper.toString(),"-JavaPath",Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-ClassPath",classpath,"-InputPath",temp.toString(),"-Mime",mime,"-TimeoutMs",Long.toString(remaining)).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
       if(!process.waitFor(remaining,TimeUnit.MILLISECONDS)){process.destroyForcibly();process.waitFor(2,TimeUnit.SECONDS);throw new java.io.IOException();}
       if(process.exitValue()!=0)throw new java.io.IOException();
       result.add(new ValidatedFile(name,mime,Files.readAllBytes(temp)));
       if(System.nanoTime()>deadline)throw new java.io.IOException();
     }catch(BusinessException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new BusinessException(400,"Không xác minh được tệp hoặc tệp vượt giới hạn xử lý. Vui lòng chọn ảnh hay PDF hợp lệ, không mã hóa.");}
     finally{if(process!=null&&process.isAlive()){process.destroyForcibly();try{process.onExit().get(2,TimeUnit.SECONDS);}catch(Exception ignored){}}
       try{if(temp!=null)Files.deleteIfExists(temp);if(helper!=null)Files.deleteIfExists(helper);}catch(java.io.IOException ignored){}}
   }
   return List.copyOf(result);
 }
}
