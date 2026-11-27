package com.kizuna.features.ticket.util;
import java.io.*;
import java.nio.file.*;
import javax.imageio.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
/** Only launched inside an OS-limited child process. Never call from application services. */
public final class TicketParserWorker {
 private TicketParserWorker(){}
 public static void main(String[] args){try{if(args.length!=2)System.exit(2);Path file=Path.of(args[0]);
   if(args[1].equals("application/pdf"))pdf(file);else image(file,args[1]);
   System.exit(0);
 }catch(Throwable e){System.exit(2);}}
 private static void image(Path file,String mime)throws Exception{
   try(var input=ImageIO.createImageInputStream(file.toFile())){
     var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException();var reader=readers.next();
     try{reader.setInput(input,false,false);String format=reader.getFormatName();
       if(!(mime.equals("image/png")&&format.equalsIgnoreCase("png"))&&!(mime.equals("image/jpeg")&&format.equalsIgnoreCase("JPEG")))throw new IOException();
       long pixels=(long)reader.getWidth(0)*reader.getHeight(0);if(pixels<1||pixels>20000000)throw new IOException();
       boolean[] warning={false};reader.addIIOReadWarningListener((source,message)->warning[0]=true);
       var decoded=reader.read(0);if(decoded==null||warning[0])throw new IOException();decoded.flush();
     }finally{reader.dispose();}
   }
 }
 private static void pdf(Path file)throws Exception{
   try(var doc=new PDFParser(new RandomAccessReadBufferedFile(file)).parse(false)){
     if(doc.isEncrypted()||doc.getNumberOfPages()<1||doc.getNumberOfPages()>100)throw new IOException();
     for(var page:doc.getPages())new PDFStreamParser(page).parse();
   }
 }
}
