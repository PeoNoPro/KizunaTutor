package com.kizuna.support;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
class SupportTicketParserIsolationTest {
 @Test void osCapRejectsNativeMemoryAndTimeoutKillsWorker()throws Exception{
   Path script=Files.createTempFile("ticket-job-test-",".ps1"),output=Files.createTempFile("ticket-job-probe-",".txt");
   try{
     String source=Files.readString(Path.of("src/main/resources/ticket-parser-job.ps1")).replace("com.kizuna.features.ticket.util.TicketParserWorker","com.kizuna.support.TicketParserResourceProbe").replace("-Djava.awt.headless=true","-XX:MaxDirectMemorySize=512m -Djava.awt.headless=true");Files.writeString(script,source);
     Process memory=launch(script,output,"memory",10000);assertTrue(memory.waitFor(15,TimeUnit.SECONDS));assertNotEquals(0,memory.exitValue());int count=Integer.parseInt(Files.readString(output));assertTrue(count>0&&count<26,"OS cap must stop native allocation before 260 MB, independent of heap");
     long started=System.nanoTime();Process timeout=launch(script,output,"sleep",500);assertTrue(timeout.waitFor(6,TimeUnit.SECONDS));assertNotEquals(0,timeout.exitValue());assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)<6000);
     Process crash=launch(script,output,"crash",5000);assertTrue(crash.waitFor(8,TimeUnit.SECONDS));assertNotEquals(0,crash.exitValue());
   }finally{Files.deleteIfExists(script);Files.deleteIfExists(output);}
 }
 private Process launch(Path script,Path output,String mode,int ms)throws Exception{return new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File",script.toString(),"-JavaPath",Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-ClassPath",System.getProperty("java.class.path"),"-InputPath",output.toString(),"-Mime",mode,"-TimeoutMs",Integer.toString(ms)).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();}
}
