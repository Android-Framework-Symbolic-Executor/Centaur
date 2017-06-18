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

package gov.nasa.jpf.centaur.vm;


import migration.Migrate;
import queryer.Query;
import bootstrap.KeyWords;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DirectCallStackFrame;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.SystemClassLoaderInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

public class CentaurClassInfo extends gov.nasa.jpf.vm.ClassInfo {

  ElementInfo createAndLinkStartupClassObject (ClassInfo ci, ThreadInfo ti) {
    StaticElementInfo sei = getStaticElementInfo();
    ElementInfo ei = createClassObject(ti);

    // check whether the class belongs to migrated class
    ClassInfo thisCi = ci;
    String clsName = thisCi.toString();  // ClassInfo[name=testCIs]       
    clsName = clsName.substring(15, clsName.length()-1);
    boolean needMigrated = false; //always migrate? 
    if (Query.classObjRefMap.containsKey(clsName)) { 
        needMigrated = true;
    }          
    /* copy all the static field reference ids for sei */
    if(needMigrated) { // This class has been initialized in Android, so migrate it.              
        //Heap hp = ti.get.getHeap();
        //hp.AddMigrateNumberofClass();
        
        sei.setObjectAttr(KeyWords.migrationAttr);
        String myClsName = sei.getClassInfo().toString();  // ClassInfo[name=testCIs]       
        myClsName = myClsName.substring(15, myClsName.length()-1);

        int numField = sei.getNumberOfFields();
        for (int i = 0; i < numField; ++ i) {
            FieldInfo f = sei.getFieldInfo(i);              
            // if it is primitive type, we do not need to find the reference id. directly copy the value 
            String type = f.getSignature();
            String fName = f.getName(); 
            System.out.println("fieldinfo name: " + fName);         
            if (type.equals("I")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);             
                 if (val != "0") {
                    int element = Integer.parseInt(val);                                                   
                    sei.setIntField(f, element);
                 }
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else if (type.equals("C")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                 if (val != "0") {
                     char element = val.charAt(0);                 
                     sei.setCharField(f, element);
                 }
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else if (type.equals("B")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                 if (val != "0") {
                     Byte element = Byte.valueOf(val);
                     sei.setByteField(f, element);
                 }
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else if (type.equals("F")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);                                     
                 if (val != "0") {
                     float element = Float.parseFloat(val);
                     sei.setFloatField(f, element);
                 }
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else if (type.equals("S")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                 if (val != "0") {
                     short element = Short.parseShort(val);
                     sei.setShortField(f, element);
                 }
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else if (type.equals("Z")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);               
                 if (val != "0") {
                     boolean element = Boolean.parseBoolean(val);
                     sei.setBooleanField(f, element);
                 }
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else if (type.equals("D")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                 if (val != "0") {
                     double element = Double.parseDouble(val);                
                     sei.setDoubleField(f, element);
                 }
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else if (type.equals("J")) {
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                 if (val != "0") {
                     long element = Long.parseLong(val);
                     sei.setLongField(f, element);
                 } 
                 //sei.setFieldAttr(f, KeyWords.migrationAttr);
             } else {       
                 String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName); 
                                     
                 int size = f.getStorageSize();                 
                 if (size == 1) {
                     sei.set1SlotField(f, Integer.parseInt(val));   
                     //sei.setFieldAttr(f, KeyWords.migrationAttr);
                 } else {
                     sei.set2SlotField(f, Integer.parseInt(val));
                     //sei.setFieldAttr(f, KeyWords.migrationAttr);
                 }
             }
         }
     }
    /*****************************************/
    /*****************************************/
    /*****************************************/
    
    sei.setClassObjectRef(ei.getObjectRef());
    ei.setIntField( ID_FIELD, id);      
    
    return ei;
  }


  /**
   * initialize this class and its superclasses (but not interfaces)
   * this will cause execution of clinits of not-yet-initialized classes in this hierarchy
   *
   * note - we don't treat registration/initialization of a class as
   * a sharedness-changing operation since it is done automatically by
   * the VM and the triggering action in the SUT (e.g. static field access or method call)
   * is the one that should update sharedness and/or break the transition accordingly
   *
   * @return true - if initialization pushed DirectCallStackFrames and caller has to re-execute
   */
  public static boolean initializeClass(ClassInfo cinfo, ThreadInfo ti){
    int pushedFrames = 0;

    /*****************************************/
    /*****************************************/
    /*****************************************/
    //added by lannan; check whether the class belongs to migrated class
    ClassInfo thisCi = cinfo;
    String clsName = thisCi.toString();  // ClassInfo[name=testCIs]       
    clsName = clsName.substring(15, clsName.length()-1);
    boolean needMigrated = false; //always migrate? 
    if (Query.classObjRefMap.containsKey(clsName)) { 
        needMigrated = true;
    }
    /*****************************************/
    /*****************************************/
    /*****************************************/
    // push clinits of class hierarchy (upwards, since call stack is LIFO)
    for (ClassInfo ci = cinfo; ci != null; ci = ci.getSuperClass()) {
      StaticElementInfo sei = ci.getStaticElementInfo();
      if (sei == null){
        sei = ci.registerClass(ti);
      }
      
      int status = sei.getStatus();
      if (status != INITIALIZED){ // the current class has not been initialized
        // we can't do setInitializing() yet because there is no global lock that
        // covers the whole clinit chain, and we might have a context switch before executing
        // a already pushed subclass clinit - there can be races as to which thread
        // does the static init first. Note this case is checked in INVOKECLINIT
        // (which is one of the reasons why we have it).

        if (status != ti.getId()) { // the current class is being initialized by another thread;
                                    // in this case, we force to invoke init in the current thread
          // even if it is already initializing - if it does not happen in the current thread
          // we have to sync, which we do by calling clinit
                       
          /*****************************************/
          /*****************************************/
          /*****************************************/            
          /* added by lannan; copy all the static field reference ids for sei */
          if(needMigrated) { // This class has been initialized in Android, so migrate it.              
              if (!sei.isFrozen()) {
                  //Heap hp = ti.getHeap();
                  //hp.AddMigrateNumberofClass();

                  sei.setObjectAttr(KeyWords.migrationAttr);
                  String myClsName = sei.getClassInfo().toString();  // ClassInfo[name=testCIs]       
                  myClsName = myClsName.substring(15, myClsName.length()-1);
                  //HashMap also has static field
                  int numField = sei.getNumberOfFields();
                  for (int i = 0; i < numField; ++ i) {
                      FieldInfo f = sei.getFieldInfo(i);              
                      // if it is primitive type; do not need to find the reference id, instead we directly copy the value 
                      String type = f.getSignature();
                      String fName = f.getName(); 
                      System.out.println("fieldinfo name: " + fName);         
                      if (type.equals("I")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);             
                          if (val != "0") {
                              int element = Integer.parseInt(val);                                                   
                              sei.setIntField(f, element);
                          }
                          //sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else if (type.equals("C")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                          if (val != "0") {
                              char element = val.charAt(0);                 
                              sei.setCharField(f, element);
                          }
                          //sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else if (type.equals("B")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                          if (val != "0") {
                              Byte element = Byte.valueOf(val);
                              sei.setByteField(f, element);
                          }
                          //sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else if (type.equals("F")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);                                     
                          if (val != "0") {
                              float element = Float.parseFloat(val);
                              sei.setFloatField(f, element);
                          }
                          //sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else if (type.equals("S")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                          if (val != "0") {
                              short element = Short.parseShort(val);
                              sei.setShortField(f, element);
                          }
                          //sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else if (type.equals("Z")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);               
                          if (val != "0") {
                              boolean element = Boolean.parseBoolean(val);
                              sei.setBooleanField(f, element);
                          }
                          //sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else if (type.equals("D")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                          if (val != "0") {
                              double element = Double.parseDouble(val);                
                              sei.setDoubleField(f, element);
                          }
                          sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else if (type.equals("J")) {
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName);
                          if (val != "0") {
                              long element = Long.parseLong(val);
                              sei.setLongField(f, element);
                          } 
                          //sei.setFieldAttr(f, KeyWords.migrationAttr);
                      } else {       
                          String val = Query.QueryStaticFieldValueUsingOwnerClsName(myClsName, fName); 

                          int size = f.getStorageSize();                 
                          if (size == 1) {
                              sei.set1SlotField(f, Integer.parseInt(val));   
                              //sei.setFieldAttr(f, KeyWords.migrationAttr);
                          } else {
                              sei.set2SlotField(f, Integer.parseInt(val));
                              //sei.setFieldAttr(f, KeyWords.migrationAttr);
                          }
                      }
                  }
              }
              ci.setInitialized();
          /*****************************************/
          /*****************************************/
          /*****************************************/
          } else { // Enforce the original JPF logic  
              MethodInfo mi = ci.getMethod("<clinit>()V", false);              
              if (mi != null) { 
                DirectCallStackFrame frame = ci.createDirectCallStackFrame(ti, mi, 0);
                ti.pushFrame( frame);
                pushedFrames++;

              } else {
                // it has no clinit, we can set it initialized
                ci.setInitialized();
              }
              
          }
        } else {
          // ignore if it's already being initialized  by our own thread (recursive request)
        }
      } else {
        break; // if this class is initialized, so are its superclasses
      }
    }

    return (pushedFrames > 0);
  }

  

  public void initializeInstanceData (ElementInfo ei, ThreadInfo ti) {
    // Note this is only used for field inits, and array elements are not fields!
    // Since Java has only limited element init requirements (either 0 or null),
    // we do this ad hoc in the ArrayFields ctor

    // the order of inits should not matter, since this is only
    // for constant inits. In case of a "class X { int a=42; int b=a; ..}"
    // we have a explicit "GETFIELD a, PUTFIELD b" in the ctor, but to play it
    // safely we init top down

    if (superClass != null) { // do superclasses first
      superClass.initializeInstanceData(ei, ti);
    }

    /*
     * if ei is an array, it does not have namedfields
     * it has arrayfields.
     */
    if (!ei.isArray()) {        
        for (int i=0; i<iFields.length; i++) {
            FieldInfo fi = iFields[i];
            fi.initialize(ei, ti);
        }
    } else {
        ei = ti.getHeap().newArray(ei.getClassInfo().toString(), 10, ti);
    }
    
    /*for (int i=0; i<iFields.length; i++) {
        FieldInfo fi = iFields[i];
        fi.initialize(ei, ti);
    }*/

  }
}



