package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.dto.SupportTicketRequests.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
public interface SupportTicketService {
 Map<String,Object> me();
 Map<String,Object> dashboard(String category);
 Map<String,Object> list(List<String> status,String category,String priority,UUID assignedTo,int page,int size);
 Map<String,Object> detail(UUID id);
 Map<String,Object> messages(UUID id,int page,int size);
 Map<String,Object> history(UUID id,int page,int size);
 Map<String,Object> create(Create request,List<MultipartFile> files);
 Map<String,Object> message(UUID id,Message request,List<MultipartFile> files);
 Map<String,Object> command(UUID id,String action,Command request);
 Map<String,Object> download(UUID id,UUID attachment);
 Map<String,Object> contexts(String type,int page,int size);
 List<Map<String,Object>> context(UUID id);
 List<Map<String,Object>> candidates(boolean managers);
}
