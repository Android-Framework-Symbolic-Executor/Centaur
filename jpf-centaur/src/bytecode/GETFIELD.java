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

package bytecode;

import migration.Migrate;
import queryer.Query;
import bootstrap.KeyWords;

import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.centaur.vm.HelperArray;
import gov.nasa.jpf.centaur.vm.OVHeap;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils.VarType;
import gov.nasa.jpf.symbc.heap.HeapChoiceGenerator;
import gov.nasa.jpf.symbc.heap.HeapNode;
import gov.nasa.jpf.symbc.heap.Helper;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.SymbolicStringBuilder;
import gov.nasa.jpf.vm.BooleanFieldInfo;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DoubleFieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.FloatFieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.IntegerFieldInfo;
import gov.nasa.jpf.vm.LongFieldInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.ReferenceFieldInfo;
import gov.nasa.jpf.vm.Scheduler;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.choice.IntChoiceFromSet;

import java.util.Map;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.TitlePaneLayout;


public class GETFIELD extends gov.nasa.jpf.jvm.bytecode.GETFIELD {
	public GETFIELD(String fieldName, String clsName, String fieldDescriptor){
	    super(fieldName, clsName, fieldDescriptor);
	  }
	
  // private int numNewRefs = 0; // # of new reference objects to account for polymorphism -- work of Neha Rungta -- needs to be updated
	
	boolean abstractClass = false;


  @Override
  public Instruction execute (ThreadInfo ti) {
	  
	  HeapNode[] prevSymRefs = null; // previously initialized objects of same type: candidates for lazy init
	  int numSymRefs = 0; // # of prev. initialized objects
	  ChoiceGenerator<?> prevHeapCG = null;

	  Config conf = ti.getVM().getConfig();
	  ti.getVM().getClassLoaderList().getClassLoaderInfoWithId(1);
	  
	  String[] lazy = conf.getStringArray("symbolic.lazy");
	  if (lazy == null || !lazy[0].equalsIgnoreCase("true")){
		  return super.execute(ti);
	  }

	  StackFrame frame = ti.getModifiableTopFrame();
	  
	  int objRef = frame.peek(); // don't pop yet, we might re-enter
	  
	  lastThis = objRef;
	  //lannan modified
	  if ((objRef == -1) || (objRef == MJIEnv.NULL)) {
	      return ti.createAndThrowException("java.lang.NullPointerException",
	                                        "referencing field '" + fname + "' on null object");
	  }
	  
	  ElementInfo ei = ti.getModifiableElementInfo(objRef); //getModifiableElementInfoWithUpdatedSharedness(objRef); POR broken
	  
	  FieldInfo fi = getFieldInfo();        
      if (fi == null) {
          return ti.createAndThrowException("java.lang.NoSuchFieldError",
                                            "referencing field '" + fname + "' in " + ei);
      }
      
	  Object migrateAttr = ei.getObjectAttr();	//based on the owner's attr to determine whether the field needs migration        
	  if (migrateAttr == null)
          return SymbcExecuteFirstCheckArray(ti);
  
  
	  /* need to do special handle string's fields: hashcode, count, offset
       * migrate these values */
	  if (KeyWords.bootStrap && fi.toString().contains("java.lang.String.hashCode")) {	      
	      int hash = ei.getIntField("hashCode");     
	      if (hash == 0) {
	          int idx = ei.getReferenceField("value");
	          ElementInfo e1 = (ElementInfo) ti.getHeap().get(idx);
	          char[] str = e1.asCharArray();
	          String val = new String(str);
	          String otherField = Query.QueryStringOtherFieldWithStringVal(val);
	          if (otherField != "0") {
	              String countField = otherField.substring(otherField.indexOf("count = ")+8, otherField.indexOf("&&&"));
	              String hashCodeField = otherField.substring(otherField.indexOf("hashCode = ")+11, otherField.indexOf("***"));
	              String offsetField = otherField.substring(otherField.indexOf("offset = ")+9);
      
	              int numField = ei.getNumberOfFields();
	              //only migrate: count, hash, offset            
	              for (int i = 0; i < numField; ++ i) {  
	                  FieldInfo f = ei.getFieldInfo(i); 
	                  String fName = f.getName(); 
	                  System.out.println("fieldinfo name: " + fName);
	                  if (fName.equals("count")) {                
	                      ei.setIntField(f, Integer.parseInt(countField));
	                      //ei.setFieldAttr(f, KeyWords.migrationAttr);
	                      System.out.println("fName and element: " + fName + "  " + countField);
	                  } else if (fName.equals("hashCode")) {                
	                      ei.setIntField(f, Integer.parseInt(hashCodeField));
	                      //ei.setFieldAttr(f, KeyWords.migrationAttr);
	                      System.out.println("fName and element: " + fName + "  " + hashCodeField);
	                  } else if (fName.equals("offset")) {                
	                      ei.setIntField(f, Integer.parseInt(offsetField));
	                      //ei.setFieldAttr(f, KeyWords.migrationAttr);
	                      System.out.println("fName and element: " + fName + "  " + offsetField);
	                  }
	              } 
	          }
          }  
	  }
  
	  /*
	   * commented this code
	   * if a string is not created as a heap node, 
	   * null pointer exception will incur when string.method is called 
	   */
	  /*boolean stringsuper = false;
	  if(attr instanceof StringExpression || attr instanceof SymbolicStringBuilder)
            return super.execute(ti); // Strings are handled specially 
            (I did not find where to handle string , thus I commented it)
            (probably jpf-symbc does not consider string.method is called) */
	  
	  /*if(attr instanceof StringExpression || attr instanceof SymbolicStringBuilder) {
	      //we do not generate the path choice, instead, we directly create the string object
	      //actually, need to generate the path choice: string == null or string != null
          ElementInfo eiString = ti.getHeap().newString("", ti); 
          int daIndex = eiString.getObjectRef(); 
              
          eiString.setObjectAttr(attr);
          int numField = eiString.getNumberOfFields();
          for (int i = 0; i < numField; ++ i) {  
              FieldInfo f = eiString.getFieldInfo(i); 
              // if it is primitive type; do not need to find the reference id, instead we directly copy the value 
              String type = f.getSignature();
              String fName = f.getName(); 
              System.out.println("fieldinfo classinfo: " + type); //use signature: "[Ljava/lang/String;" boolean;         
              System.out.println("fieldinfo name: " + fName);
              //if (fName.equals("count") || fName.equals("hashCode") || fName.equals("offset") || fName.equals("value")) {
                  Expression sym_v = null;
                  String symName = attr.toString() + "." + fName; 
                  sym_v = new SymbolicInteger(symName); 
                  eiString.setFieldAttr(f, sym_v);
                  System.out.println("fName and attr: " + fName + "  " + sym_v.toString());
              //} 
          }
          
          frame.pop();
          
          ei.setReferenceField(fi,daIndex);   
          ei.setFieldAttr(fi, attr);
          
          // We could encapsulate the push in ElementInfo, but not the GET, so we keep it at the same level
          if (fi.getStorageSize() == 1) { // 1 slotter
            int ival = ei.get1SlotField(fi);
            lastValue = ival;          
            if (fi.isReference()){
              frame.pushRef(ival);              
            } else {
              frame.push(ival);
            }
            
            if (attr != null) {
              frame.setOperandAttr(attr);
            }

          } else {  // 2 slotter
            long lval = ei.get2SlotField(fi);
            lastValue = lval;
            frame.pushLong(lval);
            if (attr != null) {
              frame.setLongOperandAttr(attr);
            }
          }

          return getNext(ti);
	  } */         
	  /********************************/
      /********************************/
      /********************************/

	  
      //--- check for potential transition breaks (be aware everything above gets re-executed)
      Scheduler scheduler = ti.getScheduler();
      if (scheduler.canHaveSharedObjectCG(ti, this, ei, fi)){
        ei = scheduler.updateObjectSharedness(ti, ei, fi);
        if (scheduler.setsSharedObjectCG( ti, this, ei, fi)){
          return this; // re-execute
        }
      }
      
      frame.pop(); // Ok, now we can remove the object ref from the stack

      //determine whether we need to migrate this field
      //if this field is primitive type: do not need to migrate, we already copied its value
      //for string: we still need to migrate; string is also reference type
      	    
	  String fieldOwnerClsName = fi.getClassInfo().toString();  // ClassInfo[name=testCIs]       
	  fieldOwnerClsName = fieldOwnerClsName.substring(15, fieldOwnerClsName.length()-1);
	  String fieldName = fi.getName(); // ci  
	  System.out.println("GetField (jpf-symbc): " + fieldOwnerClsName + "." + fieldName);
	  System.out.println("Owner migrateAttr: " + migrateAttr);
	  
      OVHeap heap = (OVHeap)ti.getHeap();
      
      String type = fi.getSignature();
      System.out.println("fieldinfo classinfo: " + type); //use signature: [Ljava/util/HashMap$Node;  
      
      int fieldRef = 0;
      if (size == 1) {
          fieldRef = ei.get1SlotField(fi);    
      } else {
          fieldRef = (int) ei.get2SlotField(fi);
      }
      
      if ((fieldRef != 0) && (!type.equals("I")) && (!type.equals("C")) && (!type.equals("B")) 
              && (!type.equals("F")) && (!type.equals("S")) && (!type.equals("Z")) 
              && (!type.equals("J")) && (!type.equals("D")) /*&& (!type.equals("Ljava/lang/String;"))*/) {
          //this field is reference: class/array/string
          //now, we allocate a heap node for it
          //find the real type from snapshot
          
          //check whether the instance field has been migrated
          boolean hasInitialized = false;

          hasInitialized = heap.CheckMigrated(fieldRef);
          System.out.println("test migration. hasInitialized? " + hasInitialized);      
          
          ElementInfo eii = ti.getModifiableElementInfo(fieldRef); 
          if (eii == null)
        	  System.out.println("eii is null; ref: " + fieldRef);
          else 
        	  System.out.println("eii is NOT null; ref: " + fieldRef);
          
          //only contain "migrated", we will migrate it
          if (((eii == null) || !hasInitialized) && KeyWords.bootStrap && (migrateAttr != "") 
                  && migrateAttr.toString().contains(KeyWords.migrationAttr)) {                        
        
        	  System.out.println("we migrate it");
        	  int[] reff = new int[1];
        	  reff[0] = fieldRef;
              String fieldClass = (String) Query.QueryStaticOrInstanceFieldTypeUsingMyObjRef(reff); 
              fieldRef = reff[0];
              if (!fieldClass.equals("0")) {
            	  int symRef = Migrate.MigrateField(fieldClass, ti, fieldRef, migrateAttr.toString());
              
            	  int size = fi.getStorageSize(); 
            	  if (size == 1) {
            		  ei.set1SlotField(fi, symRef);   
            	  } else {
            		  ei.set2SlotField(fi, symRef);
            	  }
              
            	  ElementInfo eiField = (ElementInfo) ti.getModifiableElementInfo(symRef); 
            	  eiField.setObjectAttr(migrateAttr);
              
            	  heap.AddMigratedField(fieldRef, symRef);  // java.util.HashMap . table (????)
            	  //need to set and remove in heap.elementInfos??
              }
          } 
          
          /*
           * two references point to the same object
           * if the first reference point to the object is migrated
           * the second reference with the same value will be contained in con2Sym.
           * we do not need to migrate this object. but instead we need to directly set 
           * lastValue = con2Sym.getValue(reference)
           * as well as set the eiOwner.field reference value = lastValue. 
           */         
          if (hasInitialized) {
        	  boolean isConRef = heap.IsConRef(fieldRef);
        	  if (isConRef) {
        		  int symRef = Integer.parseInt(heap.GetSymRef(fieldRef));
        		  if (fi.getStorageSize() == 1) { // 1 slotter
        		      ei.set1SlotField(fi, symRef);             		        
        		  } else {  // 2 slotter
        		      ei.set2SlotField(fi, symRef);
        		  }
        	  }
          }
      }
      
      // We could encapsulate the push in ElementInfo, but not the GET, so we keep it at the same level
      if (fi.getStorageSize() == 1) { // 1 slotter
        int ival = ei.get1SlotField(fi);
        lastValue = ival;      
        if (fi.isReference()){
          frame.pushRef(ival);         
        } else {
          frame.push(ival);
        }
      } else {  // 2 slotter
        long lval = ei.get2SlotField(fi);
        lastValue = lval;
        frame.pushLong( lval);
      }
 

      /* if fieldAttr contains "skeleton", and the field is primitive type or string, 
       * we set it as symbol
       */    
      if ((migrateAttr != null) && (migrateAttr.toString().contains(KeyWords.SkeletonAttr))) {
          System.out.println("fieldRef: " + fieldRef);
          
          System.out.println("fieldInfo.getFullName: " + fi.getFullName());
          System.out.println("fieldInfo.getName: " + fi.getName());
          //need to modify: should use field-object's symbolic reference (i.e., lastValue) to represent this expression
          //e.g., a->c, b->c, both a and b CONTAINS the same fields. if we use a or b's reference, 
          //then the expression will be different, however, the expression should be the same 
          //as the return variables are the same: c.
          //String symName = fi.getFullName() + SkeletonApp.SkeletonExpressionSuffix;
          
          /*
           * eiOwner[f1, f2, ... fn], if eiOwner is skeleton, we need to set all its primitive type fields
           * attribute as a symbolic expression during MIGRATION migration. (symbolicHandled??? in the paper) 
           * 
           */
          
          String symName = fi.getName() + "." + KeyWords.SkeletonExpressionSuffix;
          System.out.println("symName: " + symName);
          
          Expression sym = null;
          
          if (symName.contains("java.util.") && symName.contains(".hash")) { 
              //for java.util.***.hash, we do not set it as symbol
              return getNext(ti);
          }
          if (symName.contains(".util.") && symName.toLowerCase().contains("array") && symName.toLowerCase().contains("size")) { 
              //for jandroid.util.SparseArray.mSize, we do not set it as symbol
              return getNext(ti);
          }
          if (fi instanceof IntegerFieldInfo || fi instanceof LongFieldInfo) {
              sym = new SymbolicInteger(symName); 
              
          } else if (fi instanceof FloatFieldInfo || fi instanceof DoubleFieldInfo) {
              sym = new SymbolicReal(symName); 
              
          } else if (fi.getType().equals("java.lang.String")) {
              sym = new StringSymbolic(symName);
          
          } else if (fi instanceof BooleanFieldInfo) {
              sym = new SymbolicInteger(symName, 0, 1);
          }   
          
          if (sym != null) { // when sym == null --> fieldInfo is reference
              if (fi.getStorageSize() == 1) { 
                  frame.setOperandAttr(sym);
              } else {
                  frame.setLongOperandAttr(sym);
              }     
              ei.setFieldAttr(fi, sym);
          }
      }

      return getNext(ti);
   }
  

  	public Instruction SymbcExecuteFirstCheckArray (ThreadInfo ti) {
	  
  		HeapNode[] prevSymRefs = null; // previously initialized objects of same type: candidates for lazy init
  		int numSymRefs = 0; // # of prev. initialized objects
  		ChoiceGenerator<?> prevHeapCG = null;

  		Config conf = ti.getVM().getConfig();
  		String[] lazy = conf.getStringArray("symbolic.lazy");
  		if (lazy == null || !lazy[0].equalsIgnoreCase("true")){
  			return super.execute(ti);
  		}
	  
	    StackFrame frame = ti.getModifiableTopFrame();
	    int objRef = frame.peek(); // don't pop yet, we might re-enter
	    lastThis = objRef;
	    if (objRef == -1) {
	      return ti.createAndThrowException("java.lang.NullPointerException",
	                                        "referencing field '" + fname + "' on null object");
	    }

	    ElementInfo ei = ti.getModifiableElementInfo(objRef); //getModifiableElementInfoWithUpdatedSharedness(objRef); POR broken
	    FieldInfo fi = getFieldInfo();
	    if (fi == null) {
	      return ti.createAndThrowException("java.lang.NoSuchFieldError",
	                                        "referencing field '" + fname + "' in " + ei);
	    }
	    
	    Object attr = ei.getFieldAttr(fi);
	    // check if the field is of ref type & it is symbolic (i.e. it has an attribute)
	    // if it is we need to do lazy initialization

	    
	    //this is to deal with the loop iteration of a symbol.mTaskHistory (only used for task hijacking attack). we help centaur to find the object in the image.
	    if ((attr != null) && ei.getClassInfo().toString().contains("ActivityStack") && fi.getName().contains("mTaskHistory")) {
   		   System.out.println("here!!!");
   		   int ref = 0;
   		   for (Map.Entry<String, String> entry : Query.instanceFieldRefMap.entrySet()) {
   			   String key = entry.getKey();
   			   if (key.contains("mTaskHistory")) {
   				   int conref = Integer.parseInt(entry.getValue());	
   				   OVHeap heap = (OVHeap)ti.getHeap();
   				   ref = Integer.parseInt(heap.GetSymRef(conref));
   				   break;
   			   }
   		    
   		   } 
   		   ei.set1SlotField(fi, ref); 
   		   ElementInfo eiField = (ElementInfo) ti.getModifiableElementInfo(ref); 		   
   		   eiField.setObjectAttr(KeyWords.migrationAttr);
   		   frame.pop(); // Ok, now we can remove the object ref from the stack
   		   frame.pushRef(ref);
   		   return getNext(ti);
	    }	   
	    
	    if (!(fi.isReference() && attr != null)) {
	    	return super.execute(ti);
	    }

	    if(attr instanceof StringExpression || attr instanceof SymbolicStringBuilder)
			return super.execute(ti); // Strings are handled specially

	    if (SymbolicInstructionFactory.debugMode)
	    	System.out.println("lazy initialization");

	    
	    int currentChoice;
	    ChoiceGenerator<?> thisHeapCG;
	  
	    ClassInfo typeClassInfo = fi.getTypeClassInfo(); // use this instead of fullType
	 
	    if(!ti.isFirstStepInsn()){
	    	prevSymRefs = null;
	    	numSymRefs = 0;
		  
	    	prevHeapCG = ti.getVM().getSystemState().getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
	    	// to check if this still works in the case of cascaded choices...

	    	if (prevHeapCG != null) {
	    		// determine # of candidates for lazy initialization
	    		SymbolicInputHeap symInputHeap =
	    				((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
	    		prevSymRefs = symInputHeap.getNodesOfType(typeClassInfo);
	    		numSymRefs = prevSymRefs.length;
	    	}
	    	int increment = 2;
	    	if(typeClassInfo.isAbstract()) {
	    		abstractClass = true;
	    		increment = 1; // only null
	    	}

	    	thisHeapCG = new HeapChoiceGenerator(numSymRefs+increment);  //+null,new
	    	ti.getVM().getSystemState().setNextChoiceGenerator(thisHeapCG);
          
	    	if (SymbolicInstructionFactory.debugMode)
	    		System.out.println("# heap cg registered: " + thisHeapCG);
	    	return this;

	    } else { 
	    	frame.pop(); // Ok, now we can remove the object ref from the stack

	    	thisHeapCG = ti.getVM().getSystemState().getLastChoiceGeneratorOfType(HeapChoiceGenerator.class);
	    	assert (thisHeapCG !=null && thisHeapCG instanceof HeapChoiceGenerator) :
	    		"expected HeapChoiceGenerator, got: " + thisHeapCG;
	    	currentChoice = ((HeapChoiceGenerator) thisHeapCG).getNextChoice();
	    }

	    PathCondition pcHeap; //this pc contains only the constraints on the heap
	    SymbolicInputHeap symInputHeap;

	    prevHeapCG = thisHeapCG.getPreviousChoiceGeneratorOfType(HeapChoiceGenerator.class);
	  	  
	    if (prevHeapCG == null){ 
	    	pcHeap = new PathCondition();
	    	symInputHeap = new SymbolicInputHeap();
	    }
	    else {
	    	pcHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentPCheap();
	    	symInputHeap = ((HeapChoiceGenerator)prevHeapCG).getCurrentSymInputHeap();
	    }

	    assert pcHeap != null;
	    assert symInputHeap != null;
	  
	    prevSymRefs = symInputHeap.getNodesOfType(typeClassInfo);
	    numSymRefs = prevSymRefs.length;
	  
	    int daIndex = 0; //index into JPF's dynamic area
	    if (currentChoice < numSymRefs) { // lazy initialization using a previously lazily initialized object
	    	HeapNode candidateNode = prevSymRefs[currentChoice];
	    	// here we should update pcHeap with the constraint attr == candidateNode.sym_v
	    	pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, candidateNode.getSymbolic());
	    	daIndex = candidateNode.getIndex();
	    }
	    else if (currentChoice == numSymRefs){ //null object
	    	pcHeap._addDet(Comparator.EQ, (SymbolicInteger) attr, new IntegerConstant(-1));
	    	daIndex = MJIEnv.NULL;//-1;
	    }
	    else if (currentChoice == (numSymRefs + 1) && !abstractClass) {
	    	// creates a new object with all fields symbolic and adds the object to SymbolicHeap
	    	boolean isArray = HelperArray.isArrayCreated(typeClassInfo);
	    	if (isArray) {
	    		daIndex = HelperArray.addNewHeapNodeForArray (typeClassInfo, ti, attr, pcHeap,
				  		symInputHeap, numSymRefs, prevSymRefs, ei.isShared());
	    	} else {
	    		daIndex = Helper.addNewHeapNode(typeClassInfo, ti, attr, pcHeap,
				  		symInputHeap, numSymRefs, prevSymRefs, ei.isShared());
	    	}
	    } else {
		 	System.err.println("subtyping not handled");
	    }
	  
	    ei.setReferenceField(fi,daIndex);
	    ei.setFieldAttr(fi, null);

	    frame.pushRef(daIndex);
      
	    ((HeapChoiceGenerator)thisHeapCG).setCurrentPCheap(pcHeap);
	    ((HeapChoiceGenerator)thisHeapCG).setCurrentSymInputHeap(symInputHeap);
	    if (SymbolicInstructionFactory.debugMode)
	    	System.out.println("GETFIELD pcHeap: " + pcHeap);
	    return getNext(ti);
  	} 
  	
}