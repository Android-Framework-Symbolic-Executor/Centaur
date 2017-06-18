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

package queryer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class Query {
  
	//note that: when storing, value of "0" and "" are not stored.
	//thus, both of them are considered as "0", and will return "0"
	
    public static String snapshotFile; 

    public static HashMap<String, String> classObjRefMap = new HashMap();  //clsName, classObjRef 
    public static HashMap<Integer, String> classObjRefNameMap = new HashMap();  //classObjRef, claName 
    public static HashMap<String, String> staticFieldRefMap = new HashMap();  //clsName.fieldName, staticFieldObjRef
    public static HashMap<String, String> instanceFieldRefMap = new HashMap(); //ownerObjRef.fieldName, instanceFieldObjRef
    public static HashMap<String, String> arrayElementRefMap = new HashMap(); //arrayObjRef.index, elementObjRef
    public static HashMap<String, String> stringRefValMap = new HashMap(); //stringObjRef.value, stringVal
    public static HashMap<String, String> stringValOtherFieldMap = new HashMap(); //stringVal, otherFieldsVal
    public static HashMap<Integer, String> ObjRefTypeMap = new HashMap(); //objRef, type
    
    /*******************************************/
    /*******************************************/
    /*******************************************/
    /*
     * build all hashmap used for querying
     */
    
    public static void QueryInitialize (ThreadInfo ti) {
    	VM vm = ti.getVM();
    	Config conf = vm.getConfig();
    	String[] Files = conf.getStringArray("snapshotfile");	    
    	for (int i = 0; i < Files.length; ++i) {
    		snapshotFile = conf.getStringArray("snapshotfile")[i];
    		BuildClassObjRefMap();
        	BuildStaticFieldRefMap();
        	Build_ObjRefTypeMap_InstanceFieldRefMap_ArrayElementRefMap();
    		
    	}
    	snapshotFile = conf.getStringArray("snapshotfile")[0];
    }

    //initialize classObjRefMap: if a class is loaded by android, we add it into classObjRefMap.
    private static void BuildClassObjRefMap () {        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(snapshotFile));
            String flg = "    class name string: ";
            String sb = "";
            String line = br.readLine();
            boolean begin = false;
            while (line != null) {
                sb += line + "\n";               
                if (begin && !(line.substring(0,1).equals("L") || line.substring(0,1).equals(" "))) {
                    break;
                }               
                if (line.equals("Load Class:")) {
                    begin = true;
                    sb = "";
                }
                if (line.contains(flg)) {
                    int index = 0;
                    if ((index = sb.indexOf("class object id: ")) != -1) {
                        String targetLine = sb.substring(index);
                        int index2 = targetLine.indexOf("\n");
                        String ref = targetLine.substring(17, index2);
                        
                        index = sb.indexOf(flg);                   
                        String clsName = sb.substring(index+flg.length());
                        index2 = clsName.indexOf("\n");
                        clsName = clsName.substring(0, index2);
                        
                        //if (!clsName.contains("java.lang.System")) {
                        classObjRefMap.put(clsName, ref);
                        classObjRefNameMap.put(Integer.parseInt(ref), clsName);
                        //}
                    }
                }
                line = br.readLine();
            }
        } catch (Exception e) {
        } finally {
            try {
                br.close();
            } catch (IOException e) {                
            }
        } 
    }

    private static void BuildStaticFieldRefMap () {
    	BufferedReader br = null;
    	try {
            br = new BufferedReader(new FileReader(snapshotFile));
            String flg1 = "Class Dump:";
            String flg2 = "    class object id: ";  
            String clsObjRef = "";
            String clsName = "";
            boolean realTargetLine = false;
            String line = br.readLine();

            while (line != null) {
                if (line.equals(flg1)) {
                    line = br.readLine();
                    if (line.contains(flg2)) {
                    	clsObjRef = line.substring(flg2.length());                  	
                    	clsName = RequestClsNameUsingClsObjRef(clsObjRef);
                        realTargetLine = true;
                    }
                }
                if (realTargetLine) {
                    String beginField = "    static fields:";       
                    boolean begin = false;
                    line = br.readLine();
                    while(line != null) {
                    	if (line.length() == 0) {
                            line = br.readLine();
                    		continue;
                    	}
                        if (line.contains(beginField)) {
                            begin = true;
                            line = br.readLine();
                        }
                        if (begin) { 
                        	char x = line.charAt(4); 
                            if (begin && !Character.isWhitespace(x)) {
                                //System.out.println("x is not whiteshpace, we are done");
                                realTargetLine = false;
                                break;
                            }
                            String curVal = line.substring(line.indexOf(": ") + 2);
                            String curFieldName = line.substring(8, line.indexOf(": "));
                            //if field reference is 0, we do not store it   
                            boolean isNum0 = isNumericZero(curVal);
                        	if (!isNum0) {
                        		//System.out.println("static field: " + clsName + "." + curFieldName + ": " + curVal);
                                staticFieldRefMap.put(clsName+"."+curFieldName, curVal); 
                        	}                                                                         
                            line = br.readLine();                           
                        }  else line = br.readLine();
                    }
                }             
                else line = br.readLine();
            }
        } catch (Exception e) {
        } finally {
            try {
                br.close();
            } catch (IOException e) {                
            }
        } 
    }

    private static void Build_ObjRefTypeMap_InstanceFieldRefMap_ArrayElementRefMap () {     
        String fieldCls = "";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(snapshotFile));
            String flg2 = "    object id: ";
            boolean realTargetLine = false;
            String preLine = br.readLine();
            String line = br.readLine();           

            String objRef = "";          
            String arraySize = "";
            String arrayType = "";                       
            String targetCls = "";
            
            while (line != null) {
                if (line.contains(flg2)) {
                	objRef = line.substring(flg2.length());  
                    realTargetLine = true;
                }
                if (realTargetLine) {
                    if (preLine.contains("Instance Dump:")) {
                        line = br.readLine();
                        line = br.readLine();
                        targetCls = line; 
                        String flgx = "    class object id: "; 
                        String clsObjRef = line.substring(flgx.length());                  	
                        fieldCls = RequestClsNameUsingClsObjRef(clsObjRef);    
                        //System.out.println("objRefType: objRef: " + objRef + "; fieldCls: " + fieldCls);
                        ObjRefTypeMap.put(Integer.parseInt(objRef), fieldCls); 
                                                
                        String beginField = "    instance field values:"; 
                        line = br.readLine();
                        if (line.contains(beginField)) {
                        	line = br.readLine();
                        	while(line != null) {
                        		if (line.length() == 0) {
                                    line = br.readLine();
                            		continue;
                            	}
                            	char x = line.charAt(0); 
                                //System.out.println("x: " + x);
                                if (!Character.isWhitespace(x)) {
                                    //System.out.println("x is not whiteshpace, we are done");
                                    break;
                                } else { 
                                	String curVal = line.substring(line.indexOf(" = ") + 3);
                                	String curFieldName = line.substring(8, line.indexOf(" = "));
                                	//if field reference is 0, we do not store it  
                                	boolean isNum0 = isNumericZero(curVal);
                                	if (!isNum0) {
                                		//System.out.println("instance field: " + objRef + "." + curFieldName + ": " + curVal);
                                    	instanceFieldRefMap.put(objRef+"."+curFieldName, curVal);
                                	}     	
                                	line = br.readLine();
                                }
                            }  
                        }                                              
                        preLine = line;
                        line = br.readLine();
                        realTargetLine = false;
                        continue;
                    }    
                    else if (preLine.contains("Primitive Array Dump:")) {  // other type of array?
                        line = br.readLine(); 
                        line = br.readLine();
                        String f1 = "    number of elements: ";
                        arraySize = line.substring(f1.length());
                        line = br.readLine();
                        f1 = "    element type: ";
                        arrayType = line.substring(f1.length());
                        fieldCls = "***Array***: " + arraySize + "&&&" + arrayType;
                        //System.out.println("objRefType: objRef: " + objRef + "; fieldCls: " + fieldCls);
                        ObjRefTypeMap.put(Integer.parseInt(objRef), fieldCls); 

                        /* for primitive array, we do not restore it (to save memory).
                         * otherwise, out of memory will occur
                         */
                        /*String numFlg = "        element ";
                        line = br.readLine();
                        if (line.contains(numFlg)) {                           
                            while(line != null) {
                            	if (line.length() == 0) {
                                    line = br.readLine();
                            		continue;
                            	}
                                if (!line.contains(numFlg)) {
                                    break;
                                } else {
                                	String index = line.substring(numFlg.length());
                                	index = index.substring(0, index.indexOf(":"));
                                	int relindex = Integer.parseInt(index) - 1; 
                                	String ref = line.substring(line.indexOf(":") + 2);
                                	System.out.println("arrayRef.index: " + objRef + "." + relindex + ": " + ref);
                                	arrayElementRefMap.put(objRef+"."+relindex, ref);  
                                	line = br.readLine();
                                }
                            }
                        }*/
                        
                        // find string'value
                        if (arrayType.equals("char")) {
                        	String numFlg = "        element ";
                        	line = br.readLine();
                        	String val = "";
                        	if (line.contains(numFlg)) {                           
                        		while(line != null) {
                        			if (line.length() == 0) {
                        				line = br.readLine();
                        				continue;
                        			}
                        			if (!line.contains(numFlg)) {
                        				//System.out.println("val: " + val);
                        				stringRefValMap.put(objRef, val);  
                        				break;
                        			} else {
	                                	String ch = line.substring(line.indexOf(":") + 2);
	                                    val = val + ch;                                    	                                	
	                                	line = br.readLine();
                        			}
                        		}
                        	}
                        }                       
                        preLine = line;
                        line = br.readLine();
                        realTargetLine = false;
                        continue;
                    }
                    else if (preLine.contains("Object Array Dump:")) {  // other type of array?
                        int num = 0; 
                        line = br.readLine();
                        String numFlg = "        element ";
                        String clsFlg = "    element class object id: ";
                        
                        while(line != null) {                            
                            if (line.contains(clsFlg)) {
                                targetCls = "    " + line.substring(line.indexOf("class object id:")); //class object id: 1869570176
                                String flgx = "    class object id: "; 
                                String clsObjRef = targetCls.substring(flgx.length());                  	
                                arrayType = RequestClsNameUsingClsObjRef(clsObjRef);    
                                break;
                            }   
                            line = br.readLine();
                        }
                        line = br.readLine();
                        if (line.contains(numFlg)) {                            
                            while(line != null) {
                                if (!line.contains(numFlg)) {
                                    break;
                                } else {
                                	String index = line.substring(numFlg.length());
                                	index = index.substring(0, index.indexOf(":"));
                                	int relindex = Integer.parseInt(index) - 1; 
                                	String ref = line.substring(line.indexOf(":") + 2);
                                	//if element reference is 0, we do not store it (WRONG!!!)
                                	//if element reference is 0, we still need to store it. 
                                	//As such an element will influence the array.length, which will influence the index
                                	//e.g., tab[hash & (tab.length - 1)];
                                	//boolean isNum0 = isNumericZero(ref);
                                	//if (!isNum0 && Integer.parseInt(ref) != 0) {
                                	if (ref.length() != 0) {
                                		//System.out.println("arrayRef.index: " + objRef + "." + relindex + ": " + ref);
                                		++ num;
                                		arrayElementRefMap.put(objRef+"."+relindex, ref);
                                	}                               	
                                	line = br.readLine();
                                }
                            }
                        } else {
                            num = 0;
                        }
                        arraySize = Integer.toString(num);
                        fieldCls = "***Array***: " + arraySize + "&&&" + arrayType;
                        //System.out.println("objRefType: objRef: " + objRef + "; fieldCls: " + fieldCls);
                        ObjRefTypeMap.put(Integer.parseInt(objRef), fieldCls);   
                        realTargetLine = false;
                        preLine = line;
                        line = br.readLine();
                        continue;
                    } else {
                        realTargetLine = false;
                        preLine = line;
                        line = br.readLine();
                        continue;
                    }
                } else {
                	preLine = line;
                	line = br.readLine();
                }
            }
            
            ReplaceStringValRefWithValue();
            BuildStringValOtherFieldMap();
            
        } catch (Exception e) {
        } finally {
            try {
                br.close();
            } catch (IOException e) {                
            }
        }         
    }
    
    private static void ReplaceStringValRefWithValue () {
    	HashMap<String, String> stringRefValMap2 = new HashMap(); 

		for (Map.Entry<String, String> entry : instanceFieldRefMap.entrySet()) {
    		String key2 = entry.getKey();
    		String val2 = entry.getValue();
    		if (key2.contains(".value")) {entry.getKey();
    			if (stringRefValMap.containsKey(val2)) {
    				String val = stringRefValMap.get(val2);
    				//System.out.println("key: " + val2 + "; key2: " + key2 + "; val: " + val);
    				stringRefValMap2.put(key2, val);
    			} else {
					//System.out.println("val is null: " + "  key: " + val2 + "; key2: " + key2);
				} 
    		}
		}
		stringRefValMap.clear();
		stringRefValMap = (HashMap) stringRefValMap2.clone();
		stringRefValMap2.clear();
		
		for (Map.Entry<String, String> entry : stringRefValMap.entrySet()) {
			String key = entry.getKey();
			if (instanceFieldRefMap.containsKey(key)) {
				instanceFieldRefMap.remove(key);
			}
		}
    }
    
    private static void BuildStringValOtherFieldMap () {
		for (Map.Entry<String, String> entry : stringRefValMap.entrySet()) {
			String key = entry.getKey();
			String val2 = entry.getValue();
			String newKey1 = key.substring(0, key.indexOf(".value")) + ".count";
			String newKey2 = key.substring(0, key.indexOf(".value")) + ".hashCode";			
			String newKey3 = key.substring(0, key.indexOf(".value")) + ".offset";
			if (instanceFieldRefMap.containsKey(newKey1)) {
				String newVal = instanceFieldRefMap.get(newKey1);
				String newKey = val2 + ".count";
				//System.out.println("stringValOtherFieldMap: key=" + newKey +"; val="+ newVal);
				stringValOtherFieldMap.put(newKey, newVal);
				instanceFieldRefMap.remove(newKey1);
			}
			if (instanceFieldRefMap.containsKey(newKey2)) {
				String newVal = instanceFieldRefMap.get(newKey2);
				String newKey = val2 + ".count";
				//System.out.println("stringValOtherFieldMap: key=" + newKey +"; val="+ newVal);
				stringValOtherFieldMap.put(newKey, newVal);
				instanceFieldRefMap.remove(newKey2);
			}
			if (instanceFieldRefMap.containsKey(newKey3)) {
				String newVal = instanceFieldRefMap.get(newKey3);
				String newKey = val2 + ".count";
				//System.out.println("stringValOtherFieldMap: key=" + newKey +"; val="+ newVal);
				stringValOtherFieldMap.put(newKey, newVal);
				instanceFieldRefMap.remove(newKey3);
			}
		}
    }
    
    private static String RequestClsNameUsingClsObjRef (String clsObjRef) {
    	String clsName = classObjRefNameMap.get(Integer.parseInt(clsObjRef));
    	if (clsName != null) {
        } else {
        	System.out.println("clsName is null, clsObjRef: " + clsObjRef);
        	System.exit(0);
        	return clsName;
        }
        return clsName;
    }
    	
    private static boolean isNumericZero(String str) {
    	if (str.length() == 0) return true;
    	
    	if ((str.substring(0, 1).equals("0")) && (str.length() == 1)) {
    		//System.out.println("str: " + str + " is 0");
    		return true;
    	}
    	else 
    		return false;       
    }	
    	
    
    
    /**********************************************/
    /**********************************************/
    /**********************************************/
    /*
     * the following are query interface. 
     */
    /*public static int QueryClassObjRef (String clsName) {
    	int objRef = 0; 
        String classObjRef = classObjRefMap.get(clsName);
        if (classObjRef != null) {
            System.out.println("classObjRef: " + classObjRef);
            objRef = Integer.parseInt(classObjRef);
            return objRef;
        } else {
        	System.out.println("classObjRef is null, className: " + clsName);
        	//System.exit(0);
        	return objRef;
        }
    }*/
    
    public static int QueryBootStrappedStaticFieldValue (String fieldClsName) {
    	if (fieldClsName.charAt(0) == 'L') {
            fieldClsName = fieldClsName.substring(1);
        }        
        if (fieldClsName.charAt(fieldClsName.length()-1) == ';') {
            fieldClsName = fieldClsName.substring(0,fieldClsName.length()-1);
        }        
        StringTokenizer Tok = new StringTokenizer(fieldClsName, "/");
        String newClsName = "";
        while (Tok.hasMoreTokens()) {
            newClsName += Tok.nextToken();
            newClsName += ".";
        }
        newClsName = newClsName.substring(0, newClsName.length()-1);
        String classObjRef = classObjRefMap.get(newClsName);
        
        int objRef = 0;                 
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(snapshotFile));
            String flg1 = "Instance Dump:";
            String flg2 = "    class object id: " + classObjRef;
            boolean realTargetLine = false;
            String line = br.readLine();
            String targetLine = "";

            while (line != null) {
                if (line.equals(flg1)) {
                    line = br.readLine();
                    targetLine = line;
                    line = br.readLine();
                    line = br.readLine();
                    if (line.equals(flg2)) {
                        realTargetLine = true;
                    }
                }
                if (realTargetLine) {
                    String flg = "    object id: ";
                    String val = targetLine.substring(flg.length());
                    objRef = Integer.parseInt(val);
                    break;       
                }             
                line = br.readLine();
            }
        } catch (Exception e) {
        } finally {
            try {
                br.close();
            } catch (IOException e) {                
            }
        } 
        return objRef;   
    }
    
    public static String QueryStaticFieldValueUsingOwnerClsName (String clsName, String fieldName) {
    	if (staticFieldRefMap.containsKey(clsName+"."+fieldName)) {
    		String staticFieldObjRef = staticFieldRefMap.get(clsName+"."+fieldName);               
            System.out.println("request: static field:" + staticFieldObjRef);
            return staticFieldObjRef;
        } else {
        	System.out.println("staticFieldObjRef is null or 0. className: " + clsName + "; fieldName: " + fieldName);
        	return "0";
        }
    }
    
    public static String QueryInstanceFieldValueUsingOwnerObjRef (int ownerObjRef, String fieldName) {
    	if (instanceFieldRefMap.containsKey(ownerObjRef+"."+fieldName)) {
    		String instanceFieldObjRef = instanceFieldRefMap.get(ownerObjRef+"."+fieldName);      
            System.out.println("request: instance field: " + instanceFieldObjRef);            
            return instanceFieldObjRef;
        } else {
        	System.out.println("instanceFieldObjRef is null or 0. ownerObjRef: " + ownerObjRef + "; fieldName: " + fieldName);
        	return "0";
        }
    }
    
    public static String QueryArrayElementValueUsingMyObjRef (int myObjRef, int i) {
    	if (arrayElementRefMap.containsKey(myObjRef+"."+i)) {
    		String elementObjRef = arrayElementRefMap.get(myObjRef+"."+i);
            System.out.println("request: element with index: " + i + "  " + elementObjRef);            
            return elementObjRef;
        } else {
        	System.out.println("Element with index is null. myObjRef: " + myObjRef + "; index: " + i);
        	return "0";
        }
    }
    
    public static String QueryStringValWithMyObjRef(int myObjRef) {
    	if (stringRefValMap.containsKey(myObjRef+".value")) {
    		String value = stringRefValMap.get(myObjRef+".value");
            System.out.println("request: string value: " + value);            
            return value;
        } else {
        	System.out.println("String value is null. myObjRef: " + myObjRef);
        	return "0";
        }
    }
    
    public static String QueryStringOtherFieldWithStringVal (String strVal) {
    	String newKey1 = strVal + ".count";
		String newKey2 = strVal + ".hashCode";			
		String newKey3 = strVal + ".offset";
		String otherField = "";
		
    	if (stringValOtherFieldMap.containsKey(newKey1)) {
    		String value = stringValOtherFieldMap.get(newKey1);
            System.out.println("request: string count: " + value + "; value is: " + strVal);            
            otherField = "count = " + value;
        } else {
        	System.out.println("String count is 0. value is: " + strVal);
        	otherField = "count = 0";
        }
    	
    	if (stringValOtherFieldMap.containsKey(newKey2)) {
    		String value = stringValOtherFieldMap.get(newKey2);
            System.out.println("request: string hashCode: " + value + "; value is: " + strVal);            
            otherField += "&&&" + "hashCode = " + value;
        } else {
        	System.out.println("String hashCode is 0. value is: " + strVal);
        	otherField += "&&&" + "hashCode = 0";
        }
    	
    	if (stringValOtherFieldMap.containsKey(newKey3)) {
    		String value = stringValOtherFieldMap.get(newKey3);
            System.out.println("request: string offset: " + value + "; value is: " + strVal);            
            otherField += "***" + "offset = " + value;
        } else {
        	System.out.println("String offset is 0. value is: " + strVal);
        	otherField += "***" + "offset = 0";
        }
    	
    	return otherField;
    }
    
    public static String QueryStaticOrInstanceFieldTypeUsingMyObjRef(int[] myObjRef) { 
    	//android.app.ActivityManagerProxy ==> com.android.server.am.ActivityManagerService
    	//mInstance ref: 314587280  ==> 314853376
    	if (ObjRefTypeMap.containsKey(myObjRef[0])) {
    		String type = ObjRefTypeMap.get(myObjRef[0]);
    		if (type.contains("android.app.ActivityManagerProxy")) {
    			type = "com.android.server.am.ActivityManagerService";
    			for (Map.Entry<Integer, String> entry : ObjRefTypeMap.entrySet()) {
        			Integer key = entry.getKey();
        		    String val = entry.getValue();
        		    if (val.contains(type)) {
        		    	myObjRef[0] = key;
        		    }
        		}
    		}   		
            System.out.println("request: type: " + type + "; myObjRef: " + myObjRef[0]);            
            return type;
        } else {
        	System.out.println("type is null. myObjRef: " + myObjRef[0]);
        	return "0";
        }    	
    }
      
    public static String QueryPrimitiveArrayElementValueUsingMyObjRef(int myObjRef, int i) {
    	String val = "";
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(snapshotFile));
            String flg2 = "    object id: " + myObjRef;
            boolean realTargetLine = false;
            String line = br.readLine();           
           
            while (line != null) {
                if (line.equals(flg2)) {
                    realTargetLine = true;
                }
                if (realTargetLine) {
                    ++i;
                    String elementFlg = "        element " + i + ": ";
                    line = br.readLine(); 
                    while (line != null) {
                        if (line.contains(elementFlg)) {
                            val = line.substring(elementFlg.length());                          
                            break;
                        }  
                        line = br.readLine(); 
                    }                    
                    br.close();
                    return val;
                } 
                line = br.readLine();
            }     
        } catch (Exception e) {
        } finally {
            try {
                br.close();
            } catch (IOException e) {                
            }
        }    
        return "0";
    }
}