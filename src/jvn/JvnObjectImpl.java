package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{
    /* A JvnObject should be serializable in order to be able to transfer
    a reference to a JVN object remotely */
    private Serializable object;
    private int id;
    private LockState state;
    boolean serverWaiting;
    boolean writerForReader;

    public JvnObjectImpl(int id, Serializable obj){
        this.id = id;
        this.object = obj;
        this.state = LockState.NL;
        this.serverWaiting = false;
        this.writerForReader = false;
    }
    /**
     * Get a Read lock on the shared object
     * @throws JvnException
     **/
    public void jvnLockRead() throws jvn.JvnException{
        //Cas verrou lecture en cache
        if (this.state == LockState.RLC) {
            this.state = LockState.RLT;
        }
        //Demande de verrou
        else {
            JvnObjectImpl returnedObject = (JvnObjectImpl) JvnServerImpl.jvnGetServer().jvnLockRead(this.jvnGetObjectId());
            this.object = returnedObject.jvnGetSharedObject();
            this.state = LockState.RLT;
        }
    }

    /**
     * Get a Write lock on the object
     * @throws JvnException
     **/
    public void jvnLockWrite() throws jvn.JvnException{
        //Cas verrou écriture en cache
        if (this.state == LockState.WLC) this.state = LockState.WLT;
        //Demande de verrou
        else {
            JvnObjectImpl returnedObject = (JvnObjectImpl) JvnServerImpl.jvnGetServer().jvnLockWrite(this.jvnGetObjectId());
            this.object = returnedObject.jvnGetSharedObject();
            this.state = LockState.WLT;
        }
    }

    /**
     * Unlock  the object
     * @throws JvnException
     **/
    public synchronized void jvnUnLock() throws jvn.JvnException{
        //Si le serveur est en attente
        if(this.serverWaiting){
            //On lache le verrou totalement et notifie le serveur
            if(this.writerForReader){
                this.state = LockState.RLT_WLC;
                this.writerForReader = false;
            } else this.state = LockState.NL;

            this.serverWaiting = false;
            this.notify();
        }else{
            //Sinon on garde le verrou en cache
            if(this.state == LockState.WLT || this.state == LockState.RLT_WLC) {
                this.state = LockState.WLC;
            }else if(this.state == LockState.RLT){
                this.state = LockState.RLC;
            }else this.state = LockState.NL;
        }
    }


    /**
     * Get the object identification
     * @throws JvnException
     **/
    public int jvnGetObjectId() throws jvn.JvnException{
            return this.id;
    }

    /**
     * Get the shared object associated to this JvnObject
     * @throws JvnException
     **/
    public Serializable jvnGetSharedObject() throws jvn.JvnException{
            return (Serializable) this.object;
    }

    /**
     * Invalidate the Read lock of the JVN object
     * @throws JvnException
     **/
    public void jvnInvalidateReader() throws jvn.JvnException{
        //On vérifie si le verrou est en cours d'utilisation
        if(this.state == LockState.RLT || this.state == LockState.RLT_WLC) {
            this.serverWaiting=true;
        }else {
            //On lache le verrou
            this.state = LockState.NL;
        }

    }

    /**
     * Invalidate the Write lock of the JVN object
     * @return the current JVN object state
     * @throws JvnException
     **/
    public synchronized Serializable jvnInvalidateWriter() throws jvn.JvnException{
        //On vérifie si le verrou est en cours d'utilisation
        if(this.state == LockState.WLT ) {
            this.serverWaiting = true;
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return this;
        }else{
            //On lache le verrou
            this.state = LockState.NL;

            return this;
        }

    }

    /**
     * Reduce the Write lock of the JVN object
     * @return the current JVN object state
     * @throws JvnException
     **/
    public Serializable jvnInvalidateWriterForReader() throws jvn.JvnException{
        //On vérifie si le verrou est en cours d'utilisation
        if(this.state == LockState.WLT ) {
            this.serverWaiting = true;
            this.writerForReader = true;

            return this;
        }else {
            //On lache le verrou
            this.state = LockState.RLT_WLC;
            return this;
        }

    }
}
