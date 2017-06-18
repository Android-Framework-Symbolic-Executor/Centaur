/***************************************************************************
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ***************************************************************************/

package RPCclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import org.json.simple.*;
import com.google.gson.Gson;

public class RPC {
    private static Socket socket;
    private static final int SERVERPORT = 5050;

    private String className;
    private String methodName;
    private Object paramsType[];
    private Object paramsValue[];
    
    public RPC (String cl, String me, Object[] pt, Object[] pv) {
        className = cl;
        methodName = me;
        paramsType = pt;
        paramsValue = pv;     
    }
    
    public Object start() throws ClassNotFoundException {
        Object x = null;
        try {
            InetAddress serverAddr = InetAddress.getByName("127.0.0.1");
            socket = new Socket(serverAddr, SERVERPORT);
                
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));          
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            
            //specify the data to be send. we can modify the code ---> method name and arguments. 
            JSONObject obj = new JSONObject();
            obj.put("class", className);            
            obj.put("method", methodName);
            
            JSONArray ptype = new JSONArray();
            for (int i = 0; i < paramsType.length; ++i) {
                ptype.add(paramsType[i]);
            }
            obj.put("paramsType", ptype);
                        
            JSONArray pval = new JSONArray();
            for (int i = 0; i < paramsValue.length; ++i) {
                pval.add(paramsValue[i]);
            }
            obj.put("paramsValue", pval);
            
            //send data to server.
            System.out.println("Client send: "+obj.toString());
            out.println(obj);
            out.flush();
            
            //receive data from server. 
            String rev = in.readLine();
            System.out.println("Client receive: "+rev);
            
            Gson gson3 = new Gson();
            MyReturn o = gson3.fromJson(rev, MyReturn.class);
            System.out.println("o.type:"+o.type);
            if (o.type.compareTo("[I")==0) {
                x = gson3.fromJson(o.value.toString(), int[].class);
            } else if (o.type.compareTo("[B")==0) {
                x = gson3.fromJson(o.value.toString(), byte[].class);
            } else if (o.type.compareTo("[C")==0) {
                x = gson3.fromJson(o.value.toString(), char[].class);
            } else if (o.type.compareTo("[Z")==0) {
                x = gson3.fromJson(o.value.toString(), boolean[].class);
            } else if (o.type.compareTo("[J")==0) {
                x = gson3.fromJson(o.value.toString(), long[].class);
            } else if (o.type.compareTo("[F")==0) {
                x = gson3.fromJson(o.value.toString(), float[].class);
            }else if (o.type.compareTo("[Ljava.lang.Object;")==0) {
                x = gson3.fromJson(o.value.toString(), Object[].class);
            } if (o.type.compareTo("java.lang.Integer")==0) {
                x = gson3.fromJson(o.value.toString(), int.class);
            } else if (o.type.compareTo("java.lang.Byte")==0) {
                x = gson3.fromJson(o.value.toString(), byte.class);
            } else if (o.type.compareTo("java.lang.Character")==0) {
                x = gson3.fromJson(o.value.toString(), char.class);
            } else if (o.type.compareTo("java.lang.Boolean")==0) {
                x = gson3.fromJson(o.value.toString(), boolean.class);
            } else if (o.type.compareTo("java.lang.Long")==0) {
                x = gson3.fromJson(o.value.toString(), long.class);
            } else if (o.type.compareTo("java.lang.Float")==0) {
                x = gson3.fromJson(o.value.toString(), float.class);
            }else if (o.type.compareTo("java.lang.Object")==0) {
                x = gson3.fromJson(o.value.toString(), Object.class);
            } else {
                System.err.println("o.type has other type！！！Ｎeed to update!!!: "+o.type);
            }              
            System.out.println("x.class: "+x.getClass().getName());
    
            //after receiving, we close the socket. 
            br.close();
            in.close();
            out.close();
            socket.close();
            
            return x;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return x;
    }
    
    class MyReturn {
        private String type;
        private Object value;
    };
}