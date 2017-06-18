package gov.nasa.jpf.centaur.vm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.ObjVector;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.GenericSGOIDHeap;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.KernelState;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.Memento;
import gov.nasa.jpf.vm.MementoFactory;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class OVHeap extends gov.nasa.jpf.vm.OVHeap {
  
  //--- state management
  static class OVMemento extends GenericSGOIDHeapMemento {
    ObjVector.Snapshot<ElementInfo> eiSnap;

    Hashtable<String, String> mgSnap = new Hashtable<String, String>();
    
    OVMemento(OVHeap heap) {
      super(heap);
      
      heap.elementInfos.process(ElementInfo.storer);      
      eiSnap = heap.elementInfos.getSnapshot();
      
      /************************************/
      /************************************/
      /************************************/
      /* added by lannan 
       * for migratedField backtrack
       */      
      Set<String> keys = con2Sym.keySet();
      for(String key: keys){          
    	  mgSnap.put(key, heap.con2Sym.get(key));
      }	
      /************************************/
      /************************************/
      /************************************/
      
      Config conf = heap.vm.getConfig();
  	  String[] appuid = conf.getStringArray("skeleton.uid");
  	  if (appuid == null) {
      	  throw new JPFException("no app uid is specified.");
      }   	
  	  APP_UID = Integer.parseInt(appuid[0]);
  	
  	  String[] apppid = conf.getStringArray("skeleton.pid");
  	  if (apppid == null) {
  		  APP_PID = 2007;
      } else {  	
  	      APP_PID = Integer.parseInt(apppid[0]);
      }
  	
    }

    public OVHeap restore(Heap inSitu) {
      super.restore( inSitu);
      
      OVHeap heap = (OVHeap)inSitu;
      heap.elementInfos.restore(eiSnap);      
      heap.elementInfos.process(ElementInfo.restorer);
      
      /************************************/
      /************************************/
      /************************************/
      /* added by lannan 
       * for migratedField backtrack
       */
      heap.con2Sym = mgSnap;
     // int size = heap.migratedField.size();
     // for (int i = 0; i < size; ++i) {
     //     heap.migratedField.add(mgSnap.get(i));
     // }
      /************************************/
      /************************************/
      /************************************/
      
      return heap;
    }
  }
  
  //--- instance data
  
  ObjVector<ElementInfo> elementInfos;
    
  /************************************/
  /************************************/
  /************************************/
  /* added by lannan */
  public static Hashtable<String, String> con2Sym; // = new Vector();  //myObjectRef
  
  
  public static int APP_UID;
  public static int APP_PID;
  
  /************************************/
  /************************************/
  /************************************/

  
  /*******************************************/
  /*******************************************/
  /*******************************************/
  /* added by lannan
   * the following two methods are for recording migrated fields
   */
  public boolean CheckMigrated (int conRef) {	  
	  Set<String> keys = con2Sym.keySet();
	  for(String key: keys){ 
		  String val = con2Sym.get(key);
		  if (Integer.parseInt(val) == conRef) return true;
	  }
	  
      if (con2Sym.containsKey(Integer.toString(conRef))) {
          return true;
      } else {
          return false;
      }
  }
  
  public boolean IsConRef (int conRef) {	  	  
      if (con2Sym.containsKey(Integer.toString(conRef))) {
          return true;
      } else {
          return false;
      }
  }
  
  public String GetSymRef (int conRef) {	  	  
      return con2Sym.get(Integer.toString(conRef));
  }
  
  public void AddMigratedField(int conRef, int symRef) {
	  con2Sym.put(Integer.toString(conRef), Integer.toString(symRef));       
  }
 
  public int GetMigratedFieldSize() {
      return con2Sym.size();       
  }
  
  public int FindConcreteRef(int symRef) {
	  Set<String> keys = con2Sym.keySet();
	  for(String key: keys){ 		  
		  String val = con2Sym.get(key);
		  if (Integer.parseInt(val) == symRef) 
			  return Integer.parseInt(key);
	  }
	  return 0;
  }
  
  
  public ElementInfo FindOwnerElement (String fieldName, int objRef) {
	  if (objRef <= 0) {
	      return null;
	  } else {  
		  Iterator<ElementInfo> it = elementInfos.iterator();
		  while (it.hasNext()) {
			  ElementInfo ei = it.next();
			  if (ei == null) continue;
	    	  int numField = ei.getNumberOfFields();          
	          for (int i = 0; i < numField; ++ i) {
	              FieldInfo f = ei.getFieldInfo(i); 	              
	              String fName = f.getName(); 
	              if (fName.equals(fieldName)) {
	            	  if (f.getStorageSize() == 1) { // 1 slotter
	            	      int ival = ei.get1SlotField(f);
	            	      if (ival == objRef) {
	            	    	  return ei;
	            	      }
	            	  } else {  // 2 slotter
	            	      long lval = ei.get2SlotField(f);
	            	      if (lval == objRef) {
	            	    	  return ei;
	            	      }
	            	  }
	              }
	          }
	      }		      
	      return null;
	    }
  }

  /*******************************************/
  /*******************************************/
  /*******************************************/
  
  //--- constructors
  
  public OVHeap (Config config, KernelState ks){
    super(config, ks);
    
    elementInfos = new ObjVector<ElementInfo>();
    con2Sym = new Hashtable<String, String>();
  }
      
  //--- the container interface

  /**
   * return number of non-null elements
   */
  @Override
  public int size() {
    return nLiveObjects;
  }
  
  @Override
  public void set (int index, ElementInfo ei) {    
    elementInfos.set(index, ei);
  }


  
  /**
   * we treat ref <= 0 as NULL reference instead of throwing an exception
   */
  @Override
  public ElementInfo get (int ref) {
    if (ref <= 0) {
      return null;
    } else {
      return elementInfos.get(ref);
    }
  }

  @Override
  public ElementInfo getModifiable (int ref) {
    if (ref <= 0) {
      return null;
    } else {
      ElementInfo ei = elementInfos.get(ref);
      if (ei != null && ei.isFrozen()) {
        ei = ei.deepClone(); 
        // freshly created ElementInfos are not frozen, so we don't have to defreeze
        elementInfos.set(ref, ei);
      }    
      return ei;
    }
  }
    
  @Override
  public void remove(int ref) {
    elementInfos.remove(ref);
  }

  @Override
  public Iterator<ElementInfo> iterator() {
    return elementInfos.nonNullIterator();
  }

  @Override
  public Iterable<ElementInfo> liveObjects() {
    return elementInfos.elements();
  }

  @Override
  public void resetVolatiles() {
    // we don't have any
  }

  @Override
  public void restoreVolatiles() {
    // we don't have any
  }

  @Override
  public Memento<Heap> getMemento(MementoFactory factory) {
    return factory.getMemento(this);
  }

  @Override
  public Memento<Heap> getMemento(){
    return new OVMemento(this);
  }


  
  /**
   * add a non-null, not yet marked reference to the markQueue
   *  
   * called from ElementInfo.markRecursive(). We don't want to expose the
   * markQueue since a copying collector might not have it
   */
  @Override
  public void queueMark (int objref){
    if ((objref == MJIEnv.NULL) || (objref == -1)) {
      return;
    }
    
    ElementInfo ei = get(objref);    
    if (ei == null) {
    	return;
    }
    
    if (!ei.isMarked()){ // only add objects once
      ei.setMarked();
      markQueue.add(ei);
    }
  }

  
}

