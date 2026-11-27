package com.kizuna.features.ticket.service;
import com.kizuna.core.exception.BusinessException;
import java.util.*;
public final class SupportTicketValidation {
 private SupportTicketValidation(){}
 private static boolean visible(String v){return v.codePoints().anyMatch(c->!Character.isWhitespace(c)&&!Character.isSpaceChar(c)&&Character.getType(c)!=Character.FORMAT);}
 public static String text(String v,int max){if(v==null||!visible(v)||v.codePointCount(0,v.length())>max||v.indexOf('\0')>=0)throw new BusinessException(400,"Vui lòng nhập nội dung hợp lệ và đúng độ dài cho phép.");return v.strip();}
 public static String choice(String v,Set<String> options){if(v==null||!options.contains(v))throw new BusinessException(400,"Lựa chọn chưa hợp lệ. Vui lòng kiểm tra lại.");return v;}
 public static long version(Long v){if(v==null||v<0)throw new BusinessException(400,"Vui lòng tải lại yêu cầu trước khi thực hiện thao tác.");return v;}
 public static void page(int page,int size){if(page<0||size<1||size>50||page>1000000)throw new BusinessException(400,"Thông tin phân trang chưa hợp lệ.");}
 public static Map<String,Object> page(List<?> items,int page,int size,long total){return Map.of("items",items,"page",page,"size",size,"totalElements",total,"totalPages",(total+size-1)/size);}
}
