package com.github.download13.vtfr;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class VtfrServer implements Runnable {
	public boolean running = true;
	private Vtfr vtfr;
	private int port;
	
	public VtfrServer(Vtfr vtfr, int port) {
		this.vtfr = vtfr;
		this.port = port;
	}
	
	public void run() {
		try {
			ServerSocket server = new ServerSocket(port);
			while(running) {
				Socket s = server.accept();
				Thread socketThread = new Thread(new VtfrServerHandler(vtfr, s));
				socketThread.start();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

