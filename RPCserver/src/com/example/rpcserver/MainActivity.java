package com.example.rpcserver;

import android.system.Os;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import android.app.Activity;

import com.android.internal.util.ArrayUtils;
import android.os.Build;
import android.os.Parcel;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Process;
import com.android.internal.util.MemInfoReader;
import android.content.res.AssetManager;
import com.android.internal.app.ProcessStats;
import com.android.phone.PhoneInterfaceManager;

import org.json.simple.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MainActivity extends Activity {

	private static final String TAG = "RPCserver";
	public static final int SERVERPORT = 8080;   
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {  	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  
        
        new Thread(new ServerThread()).start();
    }

   
    class ServerThread implements Runnable {
    	private ServerSocket serverSocket;	
    	private Socket clientSocket;
    	
		public void run() {
		
			try {
				InetAddress addr = InetAddress.getByName(getLocalIpAddress());				
				serverSocket = new ServerSocket(SERVERPORT, 0 , addr);
				Log.i(TAG, "binding client");
				
				while (true) {  
					clientSocket = serverSocket.accept();
					Log.i(TAG, "accepting client");
				
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
					Log.i(TAG, "receiving data from client");
                    	
					//receive data from client.                                     
					String s = in.readLine();
					Log.i(TAG, "received:"+s);
                    	
					//here, we can modify the code based on our requirement. 
					String toSend = ProcessCenter(s);	                  	
					Log.i(TAG, "Sending data to client: "+toSend);
					out.println(toSend);
					out.flush();
                    	
					//after sending, we close client socket.
					in.close();
					out.close();
					clientSocket.close(); //we only close client socket. But do not close serverSocket
				}
        	} catch (Exception e1) {
    			e1.printStackTrace();
    		}
        }
    }
    
    private String getLocalIpAddress() throws Exception {
        String resultIpv6 = "";
        String resultIpv4 = "";    //10.0.2.15    
        for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = (NetworkInterface) en.nextElement();
            for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                if(!inetAddress.isLoopbackAddress()){
                    if (inetAddress instanceof Inet4Address) {
                    	resultIpv4 = inetAddress.getHostAddress().toString();
                    } else if (inetAddress instanceof Inet6Address) {
                    	resultIpv6 = inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        return ((resultIpv4.length() > 0) ? resultIpv4 : resultIpv6);
    }
    
    
    public String ProcessCenter(String rev) throws Exception {
    	Log.i(TAG, "in processcenter");
    	JsonParser parser = new JsonParser();
    	JsonElement jelement = parser.parse(rev);
    	JsonObject jobject = jelement.getAsJsonObject();
    	Log.i(TAG, "jobject: "+jobject.toString());
    	
    	//get class object and method name
    	String clstr = jobject.get("class").getAsString();
    	Class<?> cls = null;   	
    	//based on the class type string, we determine which class should be involved
    	if (clstr.compareTo("ArrayUtils")==0) {
    		cls = ArrayUtils.class;
    	} else if (clstr.compareTo("Build")==0) {
    		cls = Build.class;
    	} else if (clstr.compareTo("SystemProperties")==0) {
    		cls = SystemProperties.class;
    	} else if (clstr.compareTo("Process")==0) {
    		cls = Process.class;
    	} else if (clstr.compareTo("MemInfoReader")==0) {
    		cls = MemInfoReader.class;
    	} else if (clstr.compareTo("AssetManager")==0) {
    		cls = AssetManager.class;
    	} else if (clstr.compareTo("Parcel")==0) {
    		cls = Parcel.class;
    	} else if (clstr.compareTo("SystemClock")==0) {
    		cls = SystemClock.class;
    	} else if (clstr.compareTo("ProcessStats")==0) {
    		cls = ProcessStats.class;
    	} else {
    		Log.e(TAG, "NOT handled class name!!!! Need to add it!!!");
    	}
       	String mstr = jobject.get("method").getAsString();
   
       	//get parameters' type and value
       	JsonArray paramsType = jobject.getAsJsonArray("paramsType");
       	JsonArray paramValue = jobject.getAsJsonArray("paramsValue");
    	Class<?> argType[] = new Class[paramsType.size()];
    	Object argValue[] = new Object[paramValue.size()];
    	Log.i(TAG, "paramsType.size = "+paramsType.size()); 
    	for (int i = 0; i < paramsType.size(); ++i) {
    		if (paramsType.get(i).getAsString().contains("int")) {
    			argType[i] = int.class;
    			argValue[i] = paramValue.get(i).getAsInt();
    		} else if (paramsType.get(i).getAsString().contains("boolean")) {
    			argType[i] = boolean.class;
    			argValue[i] = paramValue.get(i).getAsBoolean();
    		} else if (paramsType.get(i).getAsString().contains("byte")) {
    			argType[i] = byte.class;
    			argValue[i] = paramValue.get(i).getAsByte();
    		} else if (paramsType.get(i).getAsString().contains("char")) {
    			argType[i] = char.class;
    			argValue[i] = paramValue.get(i).getAsCharacter();
    		} else if (paramsType.get(i).getAsString().contains("double")) {
    			argType[i] = double.class;
    			argValue[i] = paramValue.get(i).getAsDouble();
    		} else if (paramsType.get(i).getAsString().contains("float")) {
    			argType[i] = float.class;
    			argValue[i] = paramValue.get(i).getAsFloat();
    		} else if (paramsType.get(i).getAsString().contains("long")) {
    			argType[i] = long.class;
    			argValue[i] = paramValue.get(i).getAsLong();
    		} else if (paramsType.get(i).getAsString().contains("short")) {
    			argType[i] = short.class;
    			argValue[i] = paramValue.get(i).getAsShort();
    		} else if (paramsType.get(i).getAsString().contains("java.lang.String")) {
    			argType[i] = Class.forName("java.lang.String");
    			argValue[i] = paramValue.get(i).getAsString();
    		} else if ((paramsType.get(i).getAsString().contains("[I")) && (mstr.compareTo("getPids")==0)&& (cls == Process.class)) {
    			argType[i] = int[].class;
    			argValue[i] = null;
    		} else if (paramsType.get(i).getAsString().contains("[]")) {
    			Log.i(TAG, "I am []!!!");
    		} else {
    			Log.i(TAG, "NOT handled parameter type!!!! Need to add it!!!"); 
    		}
    		Log.i(TAG, "argType[i]: "+argType[i]); 
    		Log.i(TAG, "argValue[i]: "+argValue[i]);
    	}
    	  
    	//invoke method and return json string
    	Object clsobj = null;
    	if ((cls == ArrayUtils.class) || (cls == SystemClock.class)) {   	
    		Constructor<?> constructor;
    		constructor = cls.getDeclaredConstructor();
    		constructor.setAccessible(true);
    		clsobj = constructor.newInstance();               
    	} else if ((cls == Parcel.class) || (cls == ProcessStats.class)) {   	
            clsobj = null;               
        } else {
    		clsobj = cls.newInstance();
    	}
	
    	Method method = cls.getDeclaredMethod(mstr, argType);
    	method.setAccessible(true);
    	Object obj = method.invoke(clsobj, argValue);

    	MyReturn mr = new MyReturn();
    	String ret = null;   	
    	Gson gson2 = new Gson();
    	if (obj == null) {
    		Log.i(TAG, "obj is null");
    		mr.type = "null";
    		mr.value = "null";
    		ret = gson2.toJson(mr);
    		Log.i(TAG, "ret: "+ret);
    	}
    	else {   	
    		mr.type = obj.getClass().getName();
    		Log.i(TAG, "mr.type: "+obj.getClass().getName());
    		mr.value = obj;
    		ret = gson2.toJson(mr);
    		Log.i(TAG, "ret: "+ret);
    	}
    	return ret;
    } 
    
    class MyReturn {
    	private String type;
    	private Object value;
    };
    
    
    

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
    
    @Override
	protected void onStop() {
		super.onStop();
	}
}
