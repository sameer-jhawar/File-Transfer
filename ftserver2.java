import java.net.*;
import java.awt.event.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
class RequestProcessor extends Thread
{
private Socket socket;
private FTServerFrame fsf;
private String id;
RequestProcessor(Socket socket,FTServerFrame fsf,String id)
{
this.fsf=fsf;
this.socket=socket;
this.id=id;
start();
}
public void run()
{
try
{
SwingUtilities.invokeLater(()->{
fsf.updateLog("Client with id : "+id+" connected");
});
InputStream is=socket.getInputStream();
OutputStream os=socket.getOutputStream();
int bytesToReceive=1024;
byte header[]=new byte[1024];
byte tmp[]=new byte[1024];
int bytesReadCount;
int i,k,j;
i=0;
j=0;
while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
for(k=0;k<bytesReadCount;k++)
{
header[i]=tmp[k];
i++;
}
j+=bytesReadCount;
}
long lengthOfFile=0;
i=0;
j=1;
while(header[i]!=',')
{
lengthOfFile=lengthOfFile+header[i]*j;
j=j*10;
i++;
}
i++;
StringBuffer sb=new StringBuffer();
while(i<=1023)
{
sb.append((char)header[i]);
i++;
}
String fileName=sb.toString().trim();
long lof=lengthOfFile;
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf.updateLog("Receiving file : "+fileName+" of length : "+lof);
}
});
File file=new File("uploads"+File.separator+fileName);
if(file.exists()) file.delete();
FileOutputStream fos=new FileOutputStream(file);
byte ack[]=new byte[1];
ack[0]=1;
os.write(ack,0,1);
os.flush();
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
long m=0;
while(m<lengthOfFile)
{
bytesReadCount=is.read(bytes);
if(bytesReadCount==-1) continue;
fos.write(bytes,0,bytesReadCount);
fos.flush();
m+=bytesReadCount;
}
ack[0]=1;
os.write(ack);
os.flush();
socket.close();
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf.updateLog("File saved to : "+file.getAbsolutePath());
fsf.updateLog("Connection with client having id : "+id+" closed");
}
});
}catch(Exception e)
{
System.out.println(e);
}
}
}
class FTServer extends Thread
{
private ServerSocket serverSocket;
private FTServerFrame fsf;
FTServer(FTServerFrame fsf)
{
this.fsf=fsf;
}
public void run()
{
try
{
serverSocket=new ServerSocket(5500);
startListening();
}catch(Exception e)
{
System.out.println(e);
}
}
public void shutDown()
{
try
{
serverSocket.close();
}catch(Exception e)
{
System.out.println(e); //remove after testing
}
}
private void startListening()
{
try
{
Socket socket;
RequestProcessor requestProcessor;
while(true)
{
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf.updateLog("Server is ready to accept request on port : 5500");
}
});
socket=serverSocket.accept();
requestProcessor=new RequestProcessor(socket,fsf,UUID.randomUUID().toString());
}
}catch(Exception e)
{
System.out.println("Server stopped listening");
System.out.println(e);
}
}
}
class FTServerFrame extends JFrame implements ActionListener
{
private FTServer server;
private JTextArea jta;
private JButton button;
private JScrollPane jsp;
private Container container;
private boolean serverState=false;
FTServerFrame()
{
container=getContentPane();
container.setLayout(new BorderLayout());
jta=new JTextArea();
button=new JButton("Start");
jsp=new JScrollPane(jta,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
container.add(jsp,BorderLayout.CENTER);
container.add(button,BorderLayout.SOUTH);
server=new FTServer(this);
setLocation(100,100);
setSize(500,500);
setVisible(true);
button.addActionListener(this);
}
public void updateLog(String message)
{
jta.append(message+"\n");
}
public void actionPerformed(ActionEvent ev)
{
if(serverState==false)
{
server.start();
serverState=true;
button.setText("Stop");
}
else
{
server.shutDown();
serverState=false;
button.setText("Start");
jta.append("Server stopped");
}
}
public static void main(String gg[])
{
FTServerFrame fsf=new FTServerFrame();
}
}
