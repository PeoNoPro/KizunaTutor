package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.dto.SupportTicketRequests.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
public interface SupportTicketService {
 Map<String,Object> me();
 Map<String,Object> dashboard(String category,String view,String priority);
 /** Tổng quan Quản lý: tình hình toàn bộ yêu cầu còn mở (chỉ số lượng). */
 Map<String,Long> overview();
 Map<String,Object> list(List<String> status,String category,String priority,UUID assignedTo,String q,boolean overdue,int page,int size);
 Map<String,Long> counts(String category,String priority,UUID assignedTo,String q,boolean overdue);
 Map<String,Object> detail(UUID id);
 Map<String,Object> messages(UUID id,int page,int size);
 Map<String,Object> history(UUID id,int page,int size);
 /** Ghi chú nội bộ (chỉ nhân viên hỗ trợ và Quản lý xem được yêu cầu). */
 List<Map<String,Object>> notes(UUID id);
 Map<String,Object> addNote(UUID id,Note request);
 Map<String,Object> create(Create request,List<MultipartFile> files);
 Map<String,Object> message(UUID id,Message request,List<MultipartFile> files);
 Map<String,Object> command(UUID id,String action,Command request);
 Map<String,Object> download(UUID id,UUID attachment);
 Map<String,Object> contexts(String type,int page,int size);
 List<Map<String,Object>> context(UUID id);
 List<Map<String,Object>> candidates(boolean managers);
}
