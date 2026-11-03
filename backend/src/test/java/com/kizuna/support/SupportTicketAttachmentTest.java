package com.kizuna.support;
import com.kizuna.features.ticket.service.TicketAttachmentValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.pdmodel.*;
import static org.junit.jupiter.api.Assertions.*;
class SupportTicketAttachmentTest {
 TicketAttachmentValidator validator=new TicketAttachmentValidator();
 @Test void acceptsDecodedImageAndParsedPdfInCappedProcess()throws Exception{
   var image=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(24,24,BufferedImage.TYPE_INT_RGB),"png",image);
   var pdf=new ByteArrayOutputStream();try(var doc=new PDDocument()){doc.addPage(new PDPage());doc.save(pdf);}
   var accepted=validator.validate(List.of(new MockMultipartFile("files","../../ảnh.png","image/png",image.toByteArray()),new MockMultipartFile("files","hướng dẫn.pdf","application/pdf",pdf.toByteArray())));
   assertEquals(2,accepted.size());assertEquals("ảnh.png",accepted.get(0).name());assertArrayEquals(image.toByteArray(),accepted.get(0).bytes());
 }
 @Test void rejectsUnsupportedCorruptAndOverLimitFiles(){
   assertThrows(RuntimeException.class,()->validator.validate(List.of(new MockMultipartFile("files","a.pdf","application/pdf","%PDF-broken".getBytes()))));
   assertThrows(RuntimeException.class,()->validator.validate(List.of(new MockMultipartFile("files","a.png","application/pdf",new byte[]{1}))));
   assertThrows(RuntimeException.class,()->validator.validate(Collections.nCopies(6,new MockMultipartFile("files","a.png","image/png",new byte[]{1}))));
   assertThrows(RuntimeException.class,()->validator.validate(List.of(new MockMultipartFile("files","a.png","image/png",new byte[10000001]))));
 }
 @Test void rejectsTooManyPdfPagesAndEncryptedPdf()throws Exception{
   var pdf=new ByteArrayOutputStream();try(var doc=new PDDocument()){for(int i=0;i<101;i++)doc.addPage(new PDPage());doc.save(pdf);}
   assertThrows(RuntimeException.class,()->validator.validate(List.of(new MockMultipartFile("files","a.pdf","application/pdf",pdf.toByteArray()))));
   pdf.reset();try(var doc=new PDDocument()){doc.addPage(new PDPage());doc.protect(new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy("owner-test","reader-test",new org.apache.pdfbox.pdmodel.encryption.AccessPermission()));doc.save(pdf);}
   assertThrows(RuntimeException.class,()->validator.validate(List.of(new MockMultipartFile("files","a.pdf","application/pdf",pdf.toByteArray()))));
 }
 @Test void acceptsFiveJpegsAndOneHundredPagePdf()throws Exception{
   var jpg=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"jpeg",jpg);
   assertEquals(5,validator.validate(Collections.nCopies(5,new MockMultipartFile("files","photo.jpeg","image/jpeg",jpg.toByteArray()))).size());
   var pdf=new ByteArrayOutputStream();try(var doc=new PDDocument()){for(int i=0;i<100;i++)doc.addPage(new PDPage());doc.save(pdf);}
   assertEquals(1,validator.validate(List.of(new MockMultipartFile("files","a.pdf","application/pdf",pdf.toByteArray()))).size());
 }
 @Test void enforcesPixelLimitBeforeDecoding()throws Exception{
   var image=new ByteArrayOutputStream();var bitmap=new BufferedImage(5000,4001,BufferedImage.TYPE_BYTE_GRAY);ImageIO.write(bitmap,"png",image);bitmap.flush();
   assertThrows(RuntimeException.class,()->validator.validate(List.of(new MockMultipartFile("files","too-large.png","image/png",image.toByteArray()))));
   image.reset();bitmap=new BufferedImage(5000,4000,BufferedImage.TYPE_BYTE_GRAY);ImageIO.write(bitmap,"png",image);bitmap.flush();
   assertEquals(1,validator.validate(List.of(new MockMultipartFile("files","boundary.png","image/png",image.toByteArray()))).size());
 }
 @Test void acceptsExactlyTenMillionBytesOfValidPdf()throws Exception{
   int padding=9999000;byte[] content=null;
   for(int attempt=0;attempt<5;attempt++){
     var out=new ByteArrayOutputStream();try(var doc=new PDDocument()){var page=new PDPage();doc.addPage(page);var stream=new org.apache.pdfbox.pdmodel.common.PDStream(doc);try(var body=stream.createOutputStream()){body.write('%');byte[] comment=new byte[padding];Arrays.fill(comment,(byte)' ');body.write(comment);body.write('\n');}page.setContents(stream);doc.save(out);}content=out.toByteArray();if(content.length==10000000)break;padding+=10000000-content.length;
   }
   assertEquals(10000000,content.length);assertEquals(1,validator.validate(List.of(new MockMultipartFile("files","boundary.pdf","application/pdf",content))).size());
 }
 @Test void missingWorkerFailsClosedAndCleansTemporaryFiles()throws Exception{
   var image=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_BYTE_GRAY),"png",image);
   var setting=TicketAttachmentValidator.class.getDeclaredField("configuredClasspath");setting.setAccessible(true);setting.set(validator,"missing-worker-classpath");
   java.nio.file.Path tmp=java.nio.file.Path.of(System.getProperty("java.io.tmpdir"));Set<java.nio.file.Path> before;
   try(var files=java.nio.file.Files.list(tmp)){before=files.filter(p->p.getFileName().toString().startsWith("kzt-ticket-")).collect(java.util.stream.Collectors.toSet());}
   assertThrows(RuntimeException.class,()->validator.validate(List.of(new MockMultipartFile("files","valid.png","image/png",image.toByteArray()))));
   try(var files=java.nio.file.Files.list(tmp)){assertTrue(files.filter(p->p.getFileName().toString().startsWith("kzt-ticket-")).allMatch(before::contains));}
 }
}
