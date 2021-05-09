package Shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceCounter extends Remote {
    public int getQtdDown()      throws RemoteException;
    public int getQtdUp()        throws RemoteException;
    public String statisticResume() throws RemoteException;
}