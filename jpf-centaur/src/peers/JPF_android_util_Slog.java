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

import gov.nasa.jpf.annotation.MJI;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.NativePeer;

public class JPF_android_util_Slog extends NativePeer {
 
    @MJI
    public static int v__Ljava_lang_String_2Ljava_lang_String_2__I (MJIEnv env, int rcls, int tagRef, int msgRef) {
        System.out.println("JPF_android_util_Slog_v -- MJI");
        return 0;
    }
    
    @MJI
    public static int v__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_Throwable_2__I (MJIEnv env, int rcls, int tagRef, int msgRef, int throwRef) {
        System.out.println("JPF_android_util_Slog_v - throwable -- MJI");
        return 0;
    }
    
    
    @MJI
    public static int d__Ljava_lang_String_2Ljava_lang_String_2__I (MJIEnv env, int rcls, int tagRef, int msgRef) {
        System.out.println("JPF_android_util_Slog_d -- MJI");
        return 0;
    }
    
    @MJI
    public static int d__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_Throwable_2__I (MJIEnv env, int rcls, int tagRef, int msgRef, int throwRef) {
        System.out.println("JPF_android_util_Slog_d - throwable -- MJI");
        return 0;
    }

    @MJI
    public static int i__Ljava_lang_String_2Ljava_lang_String_2__I (MJIEnv env, int rcls, int tagRef, int msgRef) {
        System.out.println("JPF_android_util_Slog_i -- MJI");
        return 0;
    }
    
    @MJI
    public static int i__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_Throwable_2__I (MJIEnv env, int rcls, int tagRef, int msgRef, int throwRef) {
        System.out.println("JPF_android_util_Slog_i - throwable -- MJI");
        return 0;
    }
    
    @MJI
    public static int w__Ljava_lang_String_2Ljava_lang_String_2__I (MJIEnv env, int rcls, int tagRef, int msgRef) {
        System.out.println("JPF_android_util_Slog_w -- MJI");
        return 0;
    }
   
    @MJI
    public static int w__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_Throwable_2__I (MJIEnv env, int rcls, int tagRef, int msgRef, int throwRef) {
        System.out.println("JPF_android_util_Slog_w - throwable -- MJI");
        return 0;
    }
    
    @MJI
    public static int w__Ljava_lang_String_2Ljava_lang_Throwable_2__I (MJIEnv env, int rcls, int tagRef, int throwRef) {
        System.out.println("JPF_android_util_Slog_w - throwable -- MJI");
        return 0;
    }
    
    
    @MJI
    public static int e__Ljava_lang_String_2Ljava_lang_String_2__I (MJIEnv env, int rcls, int tagRef, int msgRef) {
        System.out.println("JPF_android_util_Slog_e -- MJI");
        return 0;
    }

    @MJI
    public static int e__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_Throwable_2__I (MJIEnv env, int rcls, int tagRef, int msgRef, int throwRef) {
        System.out.println("JPF_android_util_Slog_e -throwable -- MJI");
        return 0;
    }

}
