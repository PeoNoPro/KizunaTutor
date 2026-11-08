package com.kizuna.features.ticket.controller;
import com.kizuna.features.ticket.service.SupportTicketService;
import com.kizuna.features.ticket.dto.SupportTicketRequests.*;
import com.kizuna.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api/v1/tickets") @RequiredArgsConstructor
public class SupportTicketController {
 private final SupportTicketService service;
 @org.springframework.beans.factory.annotation.Autowired private com.kizuna.features.ticket.service.TicketAttachmentValidator attachments;
 @GetMapping("/attachment-limit") public ApiResponse<?> attachmentLimit(){service.me();return ApiResponse.ok(Map.of("maxBytes",attachments.maxBytes()));}
 @GetMapping("/me") public ApiResponse<?> me(){return ApiResponse.ok(service.me());}
 @GetMapping("/counts") public ApiResponse<?> counts(@RequestParam(required=false) String category,@RequestParam(required=false) String priority,@RequestParam(required=false) UUID assignedTo){return ApiResponse.ok(service.counts(category,priority,assignedTo));}
 @GetMapping("/dashboard") public ApiResponse<?> dashboard(@RequestParam(required=false) String category){return ApiResponse.ok(service.dashboard(category));}
 @GetMapping("/csrf") public ApiResponse<?> csrf(CsrfToken token){service.me();return ApiResponse.ok(Map.of("headerName",token.getHeaderName(),"token",token.getToken()));}
 @GetMapping public ApiResponse<?> list(@RequestParam(required=false) List<String> status,@RequestParam(required=false) String category,@RequestParam(required=false) String priority,@RequestParam(required=false) UUID assignedTo,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.list(status,category,priority,assignedTo,page,size));}
 @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable UUID id){return ApiResponse.ok(service.detail(id));}
 @GetMapping("/{id}/messages") public ApiResponse<?> messages(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.messages(id,page,size));}
 @GetMapping("/{id}/history") public ApiResponse<?> history(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.history(id,page,size));}
 @GetMapping("/assignees") public ApiResponse<?> assignees(){return ApiResponse.ok(service.candidates(false));}
 @GetMapping("/managers") public ApiResponse<?> managers(){return ApiResponse.ok(service.candidates(true));}
 @GetMapping("/related-contexts") public ApiResponse<?> contexts(@RequestParam String type,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ApiResponse.ok(service.contexts(type,page,size));}
 @GetMapping("/{id}/related-context") public ApiResponse<?> context(@PathVariable UUID id){return ApiResponse.ok(service.context(id));}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<?> create(@Valid @RequestPart Create payload,@RequestPart(required=false) List<MultipartFile> files){var data=service.create(payload,files);return ResponseEntity.created(URI.create("/api/v1/tickets/"+data.get("ticketId"))).body(ApiResponse.ok(data));}
 @PostMapping(value="/{id}/messages",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ApiResponse<?> message(@PathVariable UUID id,@Valid @RequestPart Message payload,@RequestPart(required=false) List<MultipartFile> files){return ApiResponse.ok(service.message(id,payload,files));}
 @PostMapping("/{id}/{action:claim|assignment|priority|resolve|close}")
 public ApiResponse<?> command(@PathVariable UUID id,@PathVariable String action,@Valid @RequestBody Command body){return ApiResponse.ok(service.command(id,action,body));}
 @PostMapping("/{id}/escalations") public ApiResponse<?> escalate(@PathVariable UUID id,@Valid @RequestBody Command body){return ApiResponse.ok(service.command(id,"escalate",body));}
 @PostMapping("/{id}/escalations/current/{action:recipient|decision|return}")
 public ApiResponse<?> escalation(@PathVariable UUID id,@PathVariable String action,@Valid @RequestBody Command body){return ApiResponse.ok(service.command(id,action,body));}
 @GetMapping("/{id}/attachments/{attachment}")
 public ResponseEntity<byte[]> download(@PathVariable UUID id,@PathVariable UUID attachment){var file=service.download(id,attachment);return ResponseEntity.ok()
   .contentType(MediaType.parseMediaType((String)file.get("content_type"))).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore())
   .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename((String)file.get("original_name"),StandardCharsets.UTF_8).build().toString()).body((byte[])file.get("content"));}
}
