package com.kizuna.support;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
/** Only test code; native allocations prove OS cap independently of -Xmx. */
public final class TicketParserResourceProbe {
 public static void main(String[] args)throws Exception{
   if(args[1].equals("crash")){System.exit(42);return;}
   if(args[1].equals("sleep")){Thread.sleep(20000);return;}
   var buffers=new ArrayList<ByteBuffer>();int count=0;
   try{for(;count<32;count++){Files.writeString(Path.of(args[0]),Integer.toString(count));buffers.add(ByteBuffer.allocateDirect(10000000));}}
   catch(OutOfMemoryError e){Files.writeString(Path.of(args[0]),Integer.toString(count));System.exit(42);}
   Files.writeString(Path.of(args[0]),Integer.toString(count));
 }
}
