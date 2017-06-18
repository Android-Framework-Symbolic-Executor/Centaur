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

package migration;

import bootstrap.KeyWords;
import queryer.Query;

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.centaur.vm.CentaurClassInfo;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.LoadOnJPFRequired;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;

public class Migrate {
      
    public static int MigrateField (String fieldClass, ThreadInfo ti, int fieldRef, String migrateAttr) {
        //if this field is array   	
    	int symRef = 0;   	
        if (fieldClass.contains("***Array***: ")) {
            String size = fieldClass.substring(13, fieldClass.indexOf("&&&"));
            int arrayLength = Integer.parseInt(size);
            String type = fieldClass.substring(fieldClass.indexOf("&&&")+3);
            String check2Array = type.substring(type.length()-2);  
            
            if (type.equals("void")) type = "V"; 
            else if (type.equals("byte")) type = "B";
            else if (type.equals("char")) type = "C";
            else if (type.equals("float")) type = "F";
            else if (type.equals("int")) type = "I";
            else if (type.equals("double")) type = "D";
            else if (type.equals("long")) type = "J"; 
            else if (type.equals("short")) type = "S";
            else if (type.equals("boolean")) type = "Z";           
            else if (check2Array.equals("[]")) {
                type = "L" + type.substring(0, type.length()-2);                             
                String clsName = "[" + type;                 
                ClassInfo ci = ClassLoaderInfo.getCurrentResolvedClassInfo(clsName);
                ci.initializeClass(ti);                            
            } else {
                type = "L" + type;                          
                String clsName = "[" + type;                 
                ClassInfo ci = ClassLoaderInfo.getCurrentResolvedClassInfo(clsName);
                ci.initializeClass(ti);   
            }
            
            if (arrayLength < 0){
                ti.createAndThrowException("java.lang.NegativeArraySizeException");
            }
             
            Heap heap = ti.getHeap();
            if (heap.isOutOfMemory()) { // simulate OutOfMemoryError
                ti.createAndThrowException("java.lang.OutOfMemoryError", "trying to allocate new "+fieldClass);
            }
            
            ElementInfo eiArray = heap.newArray(type, arrayLength, ti);
            symRef = eiArray.getObjectRef();
                      
            //now, allocate heap for each element            
            if (type.equals("I")) {
                for (int i = 0; i < arrayLength; ++ i) {        
                    String val =  Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    int element = Integer.parseInt(val);
                    eiArray.setIntElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                }
            }
            else if (type.equals("C")) {
                for (int i = 0; i < arrayLength; ++ i) {
                    String val =  Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    char element = val.charAt(0);
                    eiArray.setCharElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                } 
            }
            else if (type.equals("B")) {
                for (int i = 0; i < arrayLength; ++ i) {    
                    String val = Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    Byte element = Byte.valueOf(val);
                    eiArray.setByteElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                } 
            }
            else if (type.equals("F")) {
                for (int i = 0; i < arrayLength; ++ i) {
                    String val = Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    float element = Float.parseFloat(val);
                    eiArray.setFloatElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                } 
            }
            else if (type.equals("S")) {
                for (int i = 0; i < arrayLength; ++ i) {
                    String val = Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    Short element = Short.parseShort(val);
                    eiArray.setShortElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                } 
            }
            else if (type.equals("Z")) {
                for (int i = 0; i < arrayLength; ++ i) {
                    String val = Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    boolean element = Boolean.parseBoolean(val);
                    eiArray.setBooleanElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                } 
            }
            else if (type.equals("D")) {
                for (int i = 0; i < arrayLength; ++ i) {
                    String val = Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    double element = Double.parseDouble(val);
                    eiArray.setDoubleElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                } 
            } 
            else if (type.equals("J")) {
                for (int i = 0; i < arrayLength; ++ i) {
                    String val = Query.QueryPrimitiveArrayElementValueUsingMyObjRef(fieldRef, i);
                    long element = Long.parseLong(val);
                    eiArray.setLongElement(i, element);
                    //eiArray.setElementAttr(i, ownerAttr);
                    System.out.println("array and element " + i + "  " + element);
                } 
            } else { 
            	// this is reference (class object等)
                for (int i = 0; i < arrayLength; ++i) {
                    String val = Query.QueryArrayElementValueUsingMyObjRef(fieldRef, i);                    
                    int elementRef = Integer.parseInt(val);
                    eiArray.setReferenceElement(i, elementRef); 
                    //eiArray.setElementAttr(i, ownerAttr);                  
                }                
            }           
        }
        // if this field is reference, including: string, class object (HashMap object, userDefined class object, etc.)
        else {
        	symRef = MigrateObject(fieldClass, ti, fieldRef, migrateAttr);
        }
        
        return symRef;
    }  
        
      
    public static int MigrateObject (String fieldClass, ThreadInfo ti, int fieldRef, String migrateAttr) {
    	int symRef = 0;
    	
        String cname = fieldClass; 
        ClassInfo ci;

        if (cname.equals("java.lang.String")) {  //java.lang.String
            //if Field type is string(primitive type?), we need to do special handling（do not need Query.QueryClassObjRef(fieldClass), and ci.initializeClass(ti)）
            System.out.println("cnams is String: "+cname); 
            String val = (String) Query.QueryStringValWithMyObjRef(fieldRef);
                      
            ElementInfo eiString = ti.getHeap().newString(val, ti); 
            symRef = eiString.getObjectRef();
            
            //eiString.setObjectAttr(ownerAttr);
            int numField = eiString.getNumberOfFields();         
            System.out.println("eiString.value: " + val);

            String otherField = Query.QueryStringOtherFieldWithStringVal(val);
	        if (otherField != "0") {
	            String countField = otherField.substring(otherField.indexOf("count = ")+8, otherField.indexOf("&&&"));
	            String hashCodeField = otherField.substring(otherField.indexOf("hashCode = ")+11, otherField.indexOf("***"));
	            String offsetField = otherField.substring(otherField.indexOf("offset = ")+9);
    
	            //only migrate: count, hash, offset            
	            for (int i = 0; i < numField; ++ i) {  
	                FieldInfo f = eiString.getFieldInfo(i); 
	                String fName = f.getName(); 
	                System.out.println("fieldinfo name: " + fName);
	                if (fName.equals("count")) {                
	                	eiString.setIntField(f, Integer.parseInt(countField));
	                	//eiString.setFieldAttr(f, KeyWords.migrationAttr);
	                    System.out.println("fName and element: " + fName + "  " + countField);
	                } else if (fName.equals("hashCode")) {                
	                	eiString.setIntField(f, Integer.parseInt(hashCodeField));
	                	//eiString.setFieldAttr(f, KeyWords.migrationAttr);
	                    System.out.println("fName and element: " + fName + "  " + hashCodeField);
	                } else if (fName.equals("offset")) {                
	                	eiString.setIntField(f, Integer.parseInt(offsetField));
	                	//eiString.setFieldAttr(f, KeyWords.migrationAttr);
	                    System.out.println("fName and element: " + fName + "  " + offsetField);
	                }
	            } 
	        }
        } else {
            // if Field is a class object (HashMap object, userDefined class object, etc.)
            //Query.QueryClassObjRef(fieldClass);
            /* resolve the referenced class */           
            try {
            	//System.out.println("cname:" + cname);
                ci = ti.resolveReferencedClass(cname);
            } catch(LoadOnJPFRequired lre) {
                throw new JPFException("add by lannan: try to load a new class for static field object: " + cname);
            }
            
            CentaurClassInfo.initializeClass(ci,ti);

            Heap heap = ti.getHeap();
            if (heap.isOutOfMemory()) { // simulate OutOfMemoryError
                ti.createAndThrowException("java.lang.OutOfMemoryError","trying to allocate new " + cname);
            }
       
            ElementInfo ei = heap.newObject(ci, ti);
            symRef = ei.getObjectRef();
        
            String eiClsName = ei.getClassInfo().toString();      
            eiClsName = eiClsName.substring(15, eiClsName.length()-1);
            //ei.setObjectAttr(ownerAttr); 
                
            int numField = ei.getNumberOfFields();          
            for (int i = 0; i < numField; ++ i) {
                FieldInfo f = ei.getFieldInfo(i); 
                // if it is primitive type, we do not need to find the reference id. directly copy the value 
                String type = f.getSignature();
                String fName = f.getName(); 
                System.out.println("fieldinfo classinfo: " + type); //use signature: "[Ljava/lang/String;" boolean;         
                System.out.println("fieldinfo name: " + fName);
                if (type.equals("I")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    int element = Integer.parseInt(val);
                    ei.setIntField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if (type.equals("C")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    char element = val.charAt(0);
                    ei.setCharField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if (type.equals("B")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    Byte element = Byte.valueOf(val);
                    ei.setByteField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if (type.equals("F")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    float element = Float.parseFloat(val);
                    ei.setFloatField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if (type.equals("S")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    short element = Short.parseShort(val);
                    ei.setShortField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if (type.equals("Z")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    boolean element = Boolean.parseBoolean(val);
                    ei.setBooleanField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if (type.equals("D")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    double element = Double.parseDouble(val);
                    ei.setDoubleField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if (type.equals("J")) {
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    long element = Long.parseLong(val);
                    ei.setLongField(f, element);
                    //ei.setFieldAttr(f, ownerAttr);
                    System.out.println("fName and element: " + fName + "  " + element);
                } else if ((type.substring(0,1).equals("L")) || (type.substring(0,1).equals("["))){                                              
                    String val = Query.QueryInstanceFieldValueUsingOwnerObjRef(fieldRef, fName);    
                    if (val == "0") {
                        //ei.setFieldAttr(f, ownerAttr);
                        continue;
                    }
                    int fRef = Integer.parseInt(val); 
                    int size = f.getStorageSize(); 
                    if (size == 1) {
                        ei.set1SlotField(f, fRef);   
                        //ei.setFieldAttr(f, ownerAttr);
                    } else {
                        ei.set2SlotField(f, fRef);
                        //ei.setFieldAttr(f, ownerAttr);
                    }
                    System.out.println("fName and fRef: " + fName + "  " + fRef);
                } 
            }    
        }
        
        return symRef;
    }      
}
    