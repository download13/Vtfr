package com.github.download13.vtfr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vexsoftware.votifier.model.Vote;


import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;


public class Vtfr extends JavaPlugin {
	private ArrayList<Method> listeners;
	private VtfrServer serverObject;
	private Thread serverThread;
	public Map<String, String> hmacKeys;
	public Configuration config;
	
	
	public void onEnable() {
		getDataFolder().mkdirs();
		
		loadConfig();
		getListeners();
		serverObject = new VtfrServer(this, config.getInt("port"));
		serverThread = new Thread(serverObject);
		serverThread.start();
		
		getLogger().info("Vtfr listening on port " + config.getInt("port"));
	}
	public void onDisable() {
		if(serverThread != null) {
			serverObject.running = false;
		}
	}
	
	private void loadConfig() {
		hmacKeys = new HashMap<String, String>();
		
		File configFile = new File(getDataFolder(), "config.yml");
		
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
		config = cfg;
		if(!cfg.contains("port")) {
			cfg.set("port", 25560);
			cfg.createSection("keys");
			try { cfg.save(configFile); } catch (IOException e) { e.printStackTrace(); }
		}
		
		ConfigurationSection listSection = cfg.getConfigurationSection("keys"); // XXX: Is this correct?
		Set<String> serverListNames = listSection.getKeys(false);
		for(String listName : serverListNames) {
			String listKey = cfg.getString(listName);
			hmacKeys.put(listName, listKey);
		}
	}
	
	private void getListeners() {
		listeners = new ArrayList<Method>();
		
		File listenersDir = new File(getDataFolder(), "listeners");
		listenersDir.mkdirs();
		
		String[] files = listenersDir.list();
		for(String file : files) {
			if(file.endsWith(".class")) {
				Method listener = getListener(file.substring(0, file.length() - 6));
				if(listener == null) continue;
				listeners.add(listener);
			}
		}
	}
	private Method getListener(String className) {
		try {
			File file = new File("listeners", className + ".class");
			URLClassLoader loader = new URLClassLoader(new URL[] { file.toURI().toURL() });
			Class<?> listener = loader.loadClass(className);
			Method[] methods = listener.getDeclaredMethods();
			
			for(Method method : methods) {
				if(method.getName() == "voteMade") {
					return method;
				}
			}
			return null;
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch(MalformedURLException e) {
			System.out.println("How the hell did this happen?!");
			e.printStackTrace();
			return null;
		}
	}
	
	public void countVote(String serverList, String username, String userAddress, String timestamp) {
		Vote vote = new Vote();
		vote.setServiceName(serverList);
		vote.setUsername(username);
		vote.setAddress(userAddress);
		vote.setTimeStamp(timestamp);
		
		for(Method listener : listeners) {
			try {
				listener.invoke(vote);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
}