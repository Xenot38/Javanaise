package jvn;

import irc.Sentence;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

//Classe proxy
public class JvnProxy implements InvocationHandler {
        private JvnObject obj;
        private JvnProxy(Object obj,String objName){

            JvnServerImpl js = JvnServerImpl.jvnGetServer();
            //Vérification de l'existence de l'objet sur le serveur local
            try {
                JvnObject jo = js.jvnLookupObject(objName);
                //On le crée s'il n'existe pas
                if (jo == null) {
                    jo = js.jvnCreateObject((Serializable)obj);
                    // after creation, I have a write lock on the object
                    jo.jvnUnLock();
                    js.jvnRegisterObject(objName, jo);
                }
                this.obj = jo;
            } catch (JvnException e) {
                e.printStackTrace();
            }
        }
        //Méthode de création du proxy
        public static Object newInstance(Object obj,String objName) {
        return java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                obj.getClass().getInterfaces(),
                new JvnProxy(obj,objName));
    }
    //Handler de la méthode applicative
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable,JvnException {
        Object result = null;
        try{
            //Récupération du type de la méthode
            JvnGetAnnotation annoReader = new JvnGetAnnotation();
            AnnotationType methodType = annoReader.getFormat(method);

            //Vérification du type et demande des locks correspondants
            if(methodType == AnnotationType.READ){
                this.obj.jvnLockRead();
            }else if (methodType == AnnotationType.WRITE){
                this.obj.jvnLockWrite();
            }else {
                throw new JvnException("La méthode donnée n'est pas annotée");
            }
            //Utilisation de la methode applicative
            result = method.invoke(obj.jvnGetSharedObject(), args);
            this.obj.jvnUnLock();
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
