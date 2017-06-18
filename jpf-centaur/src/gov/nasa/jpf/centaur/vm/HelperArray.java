package gov.nasa.jpf.centaur.vm;

import gov.nasa.jpf.symbc.heap.HeapNode;
import gov.nasa.jpf.symbc.heap.SymbolicInputHeap;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;

public class HelperArray {
	
	
	public static boolean isArrayCreated (ClassInfo typeClassInfo) {
  		String clsName = typeClassInfo.toString();  // ClassInfo[name=testCIs]       
  	    clsName = clsName.substring(15, clsName.length()-1);
  	    if (clsName.substring(0, 1).equals("[")) 
  	    	return true;
  	    else
  	    	return false;
  	}
  	
	public static int addNewHeapNodeForArray(ClassInfo typeClassInfo, ThreadInfo ti, Object attr,
			  PathCondition pcHeap, SymbolicInputHeap symInputHeap,
			  int numSymRefs, HeapNode[] prevSymRefs, boolean setShared) {
  		
  		String clsName = typeClassInfo.toString();    
  		clsName = clsName.substring(15, clsName.length()-1);
        String type = clsName.substring(1);
        
        // currently, we only create the array with length of 1
        ElementInfo eiArray = ti.getHeap().newArray(type, 1, ti);  
        int daIndex = eiArray.getObjectRef();
        
        ti.getHeap().registerPinDown(daIndex);
        String refChain = ((SymbolicInteger) attr).getName() + "[" + daIndex + "]"; // do we really need to add daIndex here?
        SymbolicInteger newSymRef = new SymbolicInteger(refChain);
        
        if(setShared) {
            eiArray.setShared(ti,true);//??
        }	          
        for(int i = 0; i < eiArray.arrayLength(); i++) {
            String elementChain = refChain + ".elementOf[" + i + "]";
            Expression sym_v = null;
            if (type.equals("I") || type.equals("J")) {
                sym_v = new SymbolicInteger(elementChain);
            } else if (type.equals("F") || type.equals("D")) {
                sym_v = new SymbolicReal(elementChain);
            } else if (type.equals("Z")) {
                sym_v = new SymbolicInteger(elementChain, 0, 1);
            } else if (type.substring(0, 1).equals("L")) {
                if (type.substring(1).equals("java.lang.String;"))
                    sym_v = new StringSymbolic(elementChain);
                else
                    sym_v = new SymbolicInteger(elementChain);
            }	              
            eiArray.setElementAttr(i, sym_v);
        }
        
        // create new HeapNode based on above info
        // update associated symbolic input heap	          
        HeapNode n= new HeapNode(daIndex,typeClassInfo,newSymRef);
        symInputHeap._add(n);
        pcHeap._addDet(Comparator.NE, newSymRef, new IntegerConstant(-1));
        //pcHeap._addDet(Comparator.EQ, newSymRef, (SymbolicInteger) attr);

        for (int i=0; i< numSymRefs; i++)
            pcHeap._addDet(Comparator.NE, n.getSymbolic(), prevSymRefs[i].getSymbolic());
        return daIndex;	          
  	}

}
