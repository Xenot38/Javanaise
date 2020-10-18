/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.HashMap;


public class JvnServerImpl extends UnicastRemoteObject implements JvnLocalServer, JvnRemoteServer{
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;
	private HashMap<String,JvnObject> items; //Cache d'items
	private JvnRemoteCoord coord;

  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();
		// to be completed
		this.items = new HashMap();
		Registry registry = LocateRegistry.getRegistry(1099);
		this.coord = (JvnRemoteCoord) registry.lookup("Coord");
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public  void jvnTerminate()
	throws jvn.JvnException {
		try {
			coord.jvnTerminate(this);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException {
		int newId = 0;
		try {
			newId = coord.jvnGetObjectId();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		JvnObject jv = new JvnObjectImpl(newId, o);
		return jv;
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo) throws jvn.JvnException {
		//Relais de l'appel au coordinateur
		try {
			this.coord.jvnRegisterObject(jon,jo,this);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		//Ajout de l'objet au cache local
		this.items.put(jon,jo);
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException {
		//On vérifie si l'item est présent dans le cache
    	if(this.items.containsKey(jon)){
    		return this.items.get(jon);
    	//Sinon on le demande au coordinateur
		}else {
			JvnObject obj = null;
			try {
				obj = coord.jvnLookupObject(jon, this);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			this.items.put(jon,obj);
			return obj;
		}
	}	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockRead(int joi)
	 throws JvnException {
	   Serializable o = null;
	   try {
	   	//On passe l'appel au coordinateur
		   o = coord.jvnLockRead(joi, this);
	   } catch (RemoteException e) {
		   e.printStackTrace();
	   }

	   //On met a jour la valeur de l'objet
	   for( JvnObject obj : items.values()){
	   	 if(obj.jvnGetObjectId() == joi){
	   	 	obj = (JvnObjectImpl) o;
		 }
	   }
	   return o;
	}	
	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockWrite(int joi)
	 throws JvnException {
	   Serializable o = null;
	   try {
		   //On passe l'appel au coordinateur
		   o = coord.jvnLockWrite(joi, this);
	   } catch (RemoteException e) {
		   e.printStackTrace();
	   }
	   return o;
	}	

	
  /**
	* Invalidate the Read lock of the JVN object identified by id 
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
  public void jvnInvalidateReader(int joi)
	throws java.rmi.RemoteException,jvn.JvnException {
	  //On cherche l'objet possédant le verrou a invalider
	  JvnObject toInvalidate = null;
		for (JvnObject o : items.values()){
			if(o.jvnGetObjectId() == joi){
				toInvalidate = o;
				break;
			}
		}

	  if (toInvalidate == null)	throw new JvnException("L'objet a invalider n'a pas été trouvé");

	  //On passe l'appel à l'objet
	  toInvalidate.jvnInvalidateReader();
  };
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException {
	  //On cherche l'objet possedant le verrou a invalider
	  JvnObject toInvalidate = null;
	  for (JvnObject o : items.values()){
		  if(o.jvnGetObjectId() == joi){
			  toInvalidate = o;
			  break;
		  }
	  }

	  if (toInvalidate == null)	throw new JvnException("L'objet a invalider n'a pas été trouvé");

	  //On passe l'appel à l'objet
	  Serializable ret = toInvalidate.jvnInvalidateWriterForReader();

	  return ret;
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException {
	   //On cherche l'objet possedant le verrou a invalider
	   JvnObject toInvalidate = null;
	   for (JvnObject o : items.values()){
		   if(o.jvnGetObjectId() == joi){
			   toInvalidate = o;
			   break;
		   }
	   }

	   if (toInvalidate == null) throw new JvnException("L'objet a invalider n'a pas été trouvé");

	   //On passe l'appel à l'objet
	   Serializable ret = toInvalidate.jvnInvalidateWriterForReader();
	   return ret;
	 };

}

 
