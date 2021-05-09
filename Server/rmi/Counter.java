package Server.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import Shared.InterfaceCounter;


public class Counter extends UnicastRemoteObject implements InterfaceCounter {
    private int qtdDown, qtdUp;
    
    public Counter(int qtdDown, int qtdUp) throws RemoteException {
        super();
        this.qtdDown = qtdDown;
        this.qtdUp   = qtdUp;
    }

    public Counter() throws RemoteException {
        this(0, 0);
    }

    public void incrementQtdDown() { this.qtdDown++; }

    public void incrementQtdUp() { this.qtdUp++; }

    public void setQtdDown(int qtdDown) {
        this.qtdDown = qtdDown;
    }

    public void setQtdUp(int qtdUp) {
        this.qtdUp = qtdUp;
    }

    @Override
    public int getQtdDown() throws RemoteException {
        return this.qtdDown;
    }

    @Override
    public int getQtdUp() throws RemoteException {
        return this.qtdUp;
    }

    @Override
    public String statisticResume() throws RemoteException {
        return  "Quantidade de ficheiros descarregados no servidor : " + this.qtdDown + "\n" +
                "Quantidade de ficheiros carregados para o servdior: " + this.qtdUp + "\n";
    }
}
