package com.github.download13.vtfr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VtfrServerHandler implements Runnable {
	private Socket s;
	private Vtfr vtfr;
	
	public VtfrServerHandler(Vtfr vtfr, Socket s) {
		this.vtfr = vtfr;
		this.s = s;
	}
	
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String firstLine = reader.readLine().trim();
			if(firstLine.startsWith("GET") || firstLine.startsWith("POST") || firstLine.startsWith("PUT")) { // Support HTTP requests as well
				String line;
				do {
					line = reader.readLine();
				} while(line.length() > 0);
				firstLine = reader.readLine();
			}
			if(!firstLine.startsWith("VOTE")) return;
			
			String serviceName = reader.readLine();
			String username = reader.readLine();
			String address = reader.readLine();
			String timestamp = reader.readLine();
			String hash = reader.readLine();
			if(serviceName == null || username == null || address == null || timestamp == null || hash == null) return;
			
			Date currentTime = new Date();
			Date voteTime = new Date(Long.parseLong(timestamp));
			long diff = Math.abs(currentTime.getTime() - voteTime.getTime()) / 1000;
			if(diff > 10) return; // Limit replay attacks
			
			SecretKeySpec keySpec = new SecretKeySpec(vtfr.hmacKeys.get(serviceName).getBytes(), "HmacSHA1");
			Mac mac;
			try { mac = Mac.getInstance("HmacSHA1"); } catch(NoSuchAlgorithmException e) { e.printStackTrace(); return; }
			try { mac.init(keySpec); } catch(InvalidKeyException e) { e.printStackTrace(); return; }
			String hmac = new String(mac.doFinal((serviceName + "\n" + username + "\n" + address + "\n" + timestamp).getBytes()));
			if(!hash.equalsIgnoreCase(hmac)) return;
			
			vtfr.countVote(serviceName, username, address, timestamp);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
