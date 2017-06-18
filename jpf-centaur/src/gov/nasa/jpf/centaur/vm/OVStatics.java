package gov.nasa.jpf.centaur.vm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.StaticElementInfo;


public class OVStatics extends gov.nasa.jpf.vm.OVStatics {

	public OVStatics(Config conf) {
		super(conf);
		// TODO Auto-generated constructor stub
	}

	@Override
	  public StaticElementInfo getModifiable(int id) {
	      
	    StaticElementInfo ei = (StaticElementInfo)elementInfos.get(id);
	    
	    if ((ei != null) && ei.isFrozen()) {
	      ei = (StaticElementInfo)ei.deepClone();
	      // freshly created ElementInfos are not frozen, so we don't have to defreeze
	      elementInfos.set(id, ei);
	    }
	    return ei;
	  }
} 
