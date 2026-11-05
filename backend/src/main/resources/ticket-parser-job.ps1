param([string]$JavaPath,[string]$ClassPath,[string]$InputPath,[string]$Mime,[int]$TimeoutMs=10000)
$ErrorActionPreference='Stop'
# Trusted launcher only: no shell execution of filenames or document content.
# Create suspended, enforce Windows process commit limit, assign job, then resume.
Add-Type -TypeDefinition @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public static class TicketJob {
 [StructLayout(LayoutKind.Sequential)] struct Basic {public long UserTime,JobTime;public uint Flags;public UIntPtr Min,Max;public uint Active;public UIntPtr Affinity;public uint Priority,Scheduling;}
 [StructLayout(LayoutKind.Sequential)] struct Io {public ulong A,B,C,D,E,F;}
 [StructLayout(LayoutKind.Sequential)] struct Extended {public Basic Basic;public Io Io;public UIntPtr ProcessMemory,JobMemory,PeakProcess,PeakJob;}
 [StructLayout(LayoutKind.Sequential,CharSet=CharSet.Unicode)] struct Startup {public int Size;public string Reserved,Desktop,Title;public int X,Y,XSize,YSize,XCount,YCount,Fill,Flags;public short Show,Reserved2;public IntPtr ReservedPtr,In,Out,Err;}
 [StructLayout(LayoutKind.Sequential)] struct ProcessInfo {public IntPtr Process,Thread;public uint ProcessId,ThreadId;}
 [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern IntPtr CreateJobObject(IntPtr attrs,string name);
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool SetInformationJobObject(IntPtr job,int cls,ref Extended info,uint size);
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool AssignProcessToJobObject(IntPtr job,IntPtr process);
 [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern bool CreateProcess(string app,StringBuilder cmd,IntPtr pa,IntPtr ta,bool inherit,uint flags,IntPtr env,string cwd,ref Startup si,out ProcessInfo pi);
 [DllImport("kernel32.dll")] static extern uint ResumeThread(IntPtr thread);
 [DllImport("kernel32.dll")] static extern uint WaitForSingleObject(IntPtr handle,uint ms);
 [DllImport("kernel32.dll")] static extern bool GetExitCodeProcess(IntPtr process,out uint code);
 [DllImport("kernel32.dll")] static extern bool TerminateProcess(IntPtr process,uint code);
 [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr handle);
 public static int Run(string java,string cp,string input,string mime,int timeout){
   if(java.Contains("\"")||cp.Contains("\"")||input.Contains("\"")||mime.Contains("\""))return 91;
   IntPtr job=CreateJobObject(IntPtr.Zero,null); ProcessInfo pi=new ProcessInfo();
   try {
     if(job==IntPtr.Zero)return 92;
     Extended limits=new Extended();limits.Basic.Flags=0x00000100|0x00002000;limits.ProcessMemory=(UIntPtr)256000000;
     if(!SetInformationJobObject(job,9,ref limits,(uint)Marshal.SizeOf(typeof(Extended))))return 93;
     Startup si=new Startup();si.Size=Marshal.SizeOf(typeof(Startup));
     string command="\""+java+"\" -Xmx96m -XX:MaxMetaspaceSize=48m -XX:ReservedCodeCacheSize=24m -XX:CompressedClassSpaceSize=16m -Xss256k -XX:+UseSerialGC -Djava.awt.headless=true -cp \""+cp+"\" com.kizuna.features.ticket.util.TicketParserWorker \""+input+"\" \""+mime+"\"";
     if(!CreateProcess(java,new StringBuilder(command),IntPtr.Zero,IntPtr.Zero,false,0x00000004|0x08000000,IntPtr.Zero,null,ref si,out pi))return 94;
     if(!AssignProcessToJobObject(job,pi.Process))return 95;
     if(ResumeThread(pi.Thread)==UInt32.MaxValue)return 96;
     if(WaitForSingleObject(pi.Process,(uint)Math.Min(timeout,10000))!=0)return 97;
     uint code;if(!GetExitCodeProcess(pi.Process,out code))return 98;return code==0?0:99;
   }finally{
     if(pi.Process!=IntPtr.Zero){TerminateProcess(pi.Process,99);CloseHandle(pi.Process);}
     if(pi.Thread!=IntPtr.Zero)CloseHandle(pi.Thread);
     if(job!=IntPtr.Zero)CloseHandle(job);
   }
 }
}
'@
exit [TicketJob]::Run($JavaPath,$ClassPath,$InputPath,$Mime,$TimeoutMs)
