package com.github.download13.vtfr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
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
			reader.close();
			s.close();
			if(serviceName == null || username == null || address == null || timestamp == null || hash == null) return;
			
			Date currentTime = new Date();
			Date voteTime;
			try {
				voteTime = new Date(Long.parseLong(timestamp));
			} catch (NumberFormatException e) { return; }
			long diff = Math.abs(currentTime.getTime() - voteTime.getTime()) / 1000;
			if(diff > 10) return; // Limit replay attacks, sort of
			
			String serviceKey = vtfr.hmacKeys.get(serviceName);
			if(serviceKey == null) return;
			
			SecretKeySpec keySpec = new SecretKeySpec(serviceKey.getBytes(), "HmacSHA1");
			Mac mac;
			try { mac = Mac.getInstance("HmacSHA1"); } catch(NoSuchAlgorithmException e) { e.printStackTrace(); return; }
			try { mac.init(keySpec); } catch(InvalidKeyException e) { e.printStackTrace(); return; }
			String body = "VOTE\n" + serviceName + "\n" + username + "\n" + address + "\n" + timestamp;
			byte[] hmacBytes = mac.doFinal(body.getBytes());
			
			StringBuilder hmacBuilder = new StringBuilder();
			for(int i = 0; i < hmacBytes.length; i++) {
				String s = Integer.toHexString(0xff & hmacBytes[i]);
				if(s.length() == 1) hmacBuilder.append('0');
				hmacBuilder.append(s);
			}
			String hmac = hmacBuilder.toString();
			if(!hash.equalsIgnoreCase(hmac)) return;
			
			vtfr.countVote(serviceName, username, address, timestamp);
		} catch(SocketException e) {
			return;
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
