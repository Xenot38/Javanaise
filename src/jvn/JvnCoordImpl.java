/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


public class JvnCoordImpl extends UnicastRemoteObject implements JvnRemoteCoord{
	

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private HashMap<Integer, ArrayList<JvnRemoteServer>> serversSubscribedToItem;
	private HashMap<String, Integer> objectIdsFromName;
	private HashMap<Integer, JvnObject> objectFromIds;
    private HashMap<Integer, ArrayList<JvnRemoteServer>> readers;
    private HashMap<Integer, JvnRemoteServer> writer;

    private static int idGen;

/**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
        this.serversSubscribedToItem= new HashMap();
        this.objectIdsFromName= new HashMap();
        this.objectFromIds= new HashMap();
        this.readers= new HashMap();
        this.writer= new HashMap();
        this.idGen = 1;
        //On ajoute le coordinateur au registre RMI
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.bind("Coord", this);
        System.out.println("Coordinateur prêt");
    }

    /**
     * Starts the coordinator
     * @throws JvnException
     **/
    public static void main(String[] args) throws Exception {
        JvnCoordImpl obj = new JvnCoordImpl();
    }

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized int jvnGetObjectId() throws java.rmi.RemoteException,jvn.JvnException {
      //Génération d'une id d'objet
    int id = this.idGen;
    this.idGen++;
    return id;
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object
  //* @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
      //Récuperation de l'id de l'objet
      int id = jo.jvnGetObjectId();
      //On vérifie que l'objet n'existe pas déja
      if(objectIdsFromName.get(jon)!= null){
          throw new JvnException("L'objet est déja enregistré sur le coordinateur");
      }else{
          //On ajoute l'objet a la map
          ArrayList<JvnRemoteServer> subscribers = new ArrayList<JvnRemoteServer>();
          subscribers.add(js);

          this.serversSubscribedToItem.put(id,subscribers);
      }
      //Mise a jour des maps
      this.objectFromIds.put(id,jo);
      ArrayList<JvnRemoteServer> readers = new ArrayList<JvnRemoteServer>();
      this.readers.put(id,readers);
      this.writer.put(id, null);
      this.objectIdsFromName.put(jon,id);
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
     //On récupère l'objet si il est présent
    if(this.objectIdsFromName.containsKey(jon)){
        int id = this.objectIdsFromName.get(jon);
        JvnObject obj = this.objectFromIds.get(id);
        ArrayList<JvnRemoteServer> ar = this.serversSubscribedToItem.get(id);

        //Ajout du serveur appelant dans la liste des serveurs possédants l'objet
        if(!ar.contains(js)) ar.add(js);
        return obj;
    }else return null;
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{

       //Cas sans écrivain sur l'objet
       if(this.writer.get(joi) == null){
           this.readers.get(joi).add(js);
           return this.objectFromIds.get(joi);
       //Cas avec écrivain
       }else{
           //On invalidate le serveur écrivain
           JvnRemoteServer serv = this.writer.get(joi);
           JvnObject o = null;
           try {
               if(serv.equals(js)) {
                   o = (JvnObject) serv.jvnInvalidateWriterForReader(joi);
               }else{
                   o = (JvnObject) serv.jvnInvalidateWriter(joi);
               }
               this.objectFromIds.replace(joi,o);
           }catch (RemoteException e){
               System.out.println("Connexion avec un client perdue");
           }
           //Mise a jour des maps
           this.writer.replace(joi,null);
           this.readers.get(joi).add(js);
           return this.objectFromIds.get(joi);
       }
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
       //Cas sans écrivains et sans lecteurs
       if(this.readers.get(joi).isEmpty() && this.writer.get(joi) == null){
           //On prend le verrou instantanément
           this.writer.replace(joi,js);
           return this.objectFromIds.get(joi);

       //Cas sans lecteurs mais avec écrivain
       }else if(this.readers.get(joi).isEmpty() && !(this.writer.get(joi) == null)){
           JvnRemoteServer serv = this.writer.get(joi);
           JvnObject o = null;
           try{
               o = (JvnObject) serv.jvnInvalidateWriter(joi);
               this.objectFromIds.replace(joi,o);
           }catch (RemoteException e){
               System.out.println("Connexion avec un client perdue");
           }
           //Remplacement de l'écrivain
           this.writer.replace(joi,js);
           return this.objectFromIds.get(joi);
       //Cas sans écrivain mais avec lecteurs
       }else{
           ArrayList<JvnRemoteServer> readersToInvalidate = this.readers.get(joi);
           //On invalidate chacun des lecteurs
           for (JvnRemoteServer s : readersToInvalidate){
               if(!s.equals(js)){
                   try{
                       s.jvnInvalidateReader(joi);
                   }catch (RemoteException e){
                       System.out.println("Connexion avec un client perdue");
                   }
               }
           }
           //Remplacement de l'écrivain
           this.readers.get(joi).clear();
           this.writer.replace(joi,js);
           return this.objectFromIds.get(joi);
       }
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public synchronized void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {

        for(int i : this.serversSubscribedToItem.keySet()){
                this.serversSubscribedToItem.get(i).remove(js);
        }

        for(int i : this.readers.keySet()){
            this.readers.get(i).remove(js);
        }

        for(int i : this.writer.keySet()){
            if(this.writer.get(i).equals(js)) this.writer.replace(i,null);
        }
    }
}

 
