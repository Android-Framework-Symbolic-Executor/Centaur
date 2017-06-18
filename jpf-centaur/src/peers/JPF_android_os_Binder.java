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

package peers;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.centaur.vm.OVHeap;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.vm.NativePeer;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MJIEnv;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Vector;

import bootstrap.KeyWords;


public class JPF_android_os_Binder extends NativePeer {
    
    public static int[] randomInt = new int[]{100,200,300,400,500,600,700,800,900};
    public static int index = 0;
    
    public static HashMap<Integer, String> identify = new HashMap<Integer, String>();

    public static int curUID = OVHeap.APP_UID;
    public static int curPID = OVHeap.APP_PID;
    
    public static final int SYSTEM_UID = 1000;
    public static final int SYSTEM_PID = 1292;   //read from heap data file
    
    public static final int PHONE_UID = 1001;

    public static final int SHELL_UID = 2000;

    public static final int LOG_UID = 1007;

    public static final int WIFI_UID = 1010;

    public static final int MEDIA_UID = 1013;

    public static final int DRM_UID = 1019;

    public static final int VPN_UID = 1016;

    public static final int NFC_UID = 1027;

    public static final int BLUETOOTH_UID = 1002;

    public static final int MEDIA_RW_GID = 1023;

    public static final int PACKAGE_INFO_GID = 1032;

    public static final int SHARED_RELRO_UID = 1037;
    
    
    public JPF_android_os_Binder() {
    }
    
    
    @MJI
    public void init____V (MJIEnv env, int objref) {    	
        System.out.println("JPF_android_os_Binder_init -- MJI");
    }
    
    @MJI
    public void clinit____V (MJIEnv env, int objref) {
        System.out.println("JPF_android_os_Binder_clinit -- MJI"); 
    }
    
    @MJI
    public static int getCallingUid____I (MJIEnv env, int rcls) throws Exception {
        System.out.println("JPF_android_os_Binder_getCallingUid -- MJI");              
        int uid = curUID;        
        if (uid == OVHeap.APP_UID) { //OVHeap.APP_UID is read from configuration file
            System.out.println("ret is skeleton app's uid 10054, we set skeleton attr");
            IntegerExpression sym = new SymbolicInteger(KeyWords.SkeletonAttr); 
            env.setReturnAttribute(sym);
        }   
        return uid;
    }
    
    @MJI
    public static int getCallingPid____I (MJIEnv env, int rcls) throws Exception {
        System.out.println("JPF_android_os_Binder_getCallingPid -- MJI");
        int pid = curPID;
        return pid;
    }
    
    @MJI
    public static long clearCallingIdentity____J (MJIEnv env, int rcls) throws Exception {
        System.out.println("JPF_android_os_Binder_clearCallingIdentity -- MJI");       
        
        ClassInfo cinfo = env.getClassInfo(rcls);
       
        int random = randomInt[index];
        String curIdentify = curUID + "***" + curPID; 
        identify.put(random, curIdentify);   //should put original identify (before clear)
        
        curUID = SYSTEM_UID;
        curPID = SYSTEM_PID;        
        index++;  
        
        System.out.println("now, curUID: " + curUID + "; curPID: " + curPID);
        System.out.println("now, random: " + random);
        return (long)random;
    }
    
    @MJI
    public static void restoreCallingIdentity__J__V (MJIEnv env, int rcls, long arg0){
        System.out.println("JPF_android_os_Binder_restoreCallingIdentity -- MJI"); 
        
        long argValue = new Long(arg0);
        int arg = (int)argValue;
        
        String restoreIdentify = identify.get(arg);
        String restoreUID = restoreIdentify.substring(0, restoreIdentify.indexOf("***"));
        String restorePID = restoreIdentify.substring(restoreIdentify.indexOf("***")+3);
        
        curUID = Integer.parseInt(restoreUID); 
        curPID = Integer.parseInt(restorePID);
        
        System.out.println("after restore, curUID: " + curUID + "; curPID: " + curPID);
        System.out.println("and random: " + arg);
    }
    
}

