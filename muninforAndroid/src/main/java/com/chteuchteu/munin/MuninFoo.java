package com.chteuchteu.munin;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.chteuchteu.munin.hlpr.SQLite;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class MuninFoo {
	private static MuninFoo instance;
	private static boolean languageLoaded = false;
	
	private List<MuninServer> servers;
	public List<Label> labels;
	public List<MuninMaster> masters;
	
	public SQLite sqlite;
	public MuninServer currentServer;
	
	// === VERSION === //
	// HISTORY		current:	 _______________________________________________________________________________________________________________________________
	// android:versionName:		| 1.1		1.2		1.3		1.4		1.4.1	1.4.2	1.4.5	1.4.6	2.0		2.0.1	2.1		2.2		2.3		2.4		2.5		2.6 |
	// android:versionCode: 	|  1		 2		 3		 4		 5		 6		 7	 	 8	  	 10		11		12		13		14		15		16		17	|
	// MfA version:				| 1.1		1.2		1.3		1.4		1.5		1.6		1.7  	1.8   	1.9		2.0		2.1 	2.2		2.3		2.4		2.5		2.6	|
	//							|-------------------------------------------------------------------------------------------------------------------------------|
	//							| 2.6.1		2.6.2	2.6.3	2.6.4	2.6.5	2.7		2.7.1	2.7.5	2.7.6	2.7.7	2.8		2.8.1	2.8.2	2.8.3	2.8.4	3.0 |
	//							|  18		 19		20		21		22		23		24		25		26		27		28		29		30		31		32		33  |
	//							|  2.7		2.8		2.9		3.0		3.1		3.2		3.3		3.4		3.5		3.6		3.7		3.8		3.9		4.0		4.1		4.2 |
	//							|															beta	beta	beta			fix		fix		fix		fix			|
	//							|-------------------------------------------------------------------------------------------------------------------------------|
	//							|																																|
	//							|																																|
	//							|																																|
	//							|																																|
	//							|-------------------------------------------------------------------------------------------------------------------------------|
	
	public static final double VERSION = 4.2;
	// =============== //
	public static final boolean DEBUG = true;
	private static final boolean FORCE_NOT_PREMIUM = false;
	public boolean premium;
	
	// Import/Export webservice
	public static final String IMPORT_EXPORT_URI = "http://www.munin-for-android.com/ws/importExport.php";
	public static final int IMPORT_EXPORT_VERSION = 1;
	
	public Calendar alerts_lastUpdated;
	
	private MuninFoo() {
		premium = false;
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<Label>();
		masters = new ArrayList<MuninMaster>();
		sqlite = new SQLite(null, this);
		instance = null;
		loadInstance();
	}
	
	private MuninFoo(Context context) {
		premium = false;
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<Label>();
		masters = new ArrayList<MuninMaster>();
		sqlite = new SQLite(context, this);
		instance = null;
		loadInstance(context);
	}
	
	public static boolean isLoaded() { return instance != null; }
	
	private void loadInstance() {
		this.masters = sqlite.dbHlpr.getMasters();
		this.servers = sqlite.dbHlpr.getServers(this.masters);
		this.labels = sqlite.dbHlpr.getLabels();
		
		attachOrphanServers();
		
		currentServer = null;
		if (servers.size() > 0)
			currentServer = getServerFromFlatPosition(0);
		
		if (DEBUG)
			sqlite.logMasters();
	}
	
	private void loadInstance(Context context) {
		loadInstance();
		if (context != null) {
			// Set default server
			String defaultServerUrl = Util.getPref(context, "defaultServer");
			if (!defaultServerUrl.equals("")) {
				for (MuninServer server : this.servers) {
					if (server.getServerUrl().equals(defaultServerUrl))
						currentServer = server;
				}
			}
			
			this.premium = isPremium(context);
		}
	}
	
	public void resetInstance(Context context) {
		servers = new ArrayList<MuninServer>();
		labels = new ArrayList<Label>();
		sqlite = new SQLite(context, this);
		loadInstance(context);
	}
	
	public static synchronized MuninFoo getInstance() {
		if (instance == null)
			instance = new MuninFoo();
		return instance;
	}
	
	public static synchronized MuninFoo getInstance(Context context) {
		if (instance == null)
			instance = new MuninFoo(context);
		return instance;
	}
	
	/**
	 * Set a common parent to the servers which does not
	 * have one after getting them
	 */
	private void attachOrphanServers() {
		int n = 0;
		MuninMaster defMaster = new MuninMaster();
		defMaster.setName("Default");
		defMaster.defaultMaster = true;
		
		for (MuninServer s : this.servers) {
			if (s.getParent() == null) {
				s.setParent(defMaster);
				n++;
			}
		}
		
		if (n > 0)
			this.masters.add(defMaster);
	}
	
	public static void loadLanguage(Context context) { loadLanguage(context, false); }
	public static void loadLanguage(Context context, boolean forceLoad) {
		if (!Util.getPref(context, "lang").equals("")) {
			if (!languageLoaded || forceLoad) {
				String lang = Util.getPref(context, "lang");
				// lang == "en" || "fr" || "de" || "ru"
				if (!(lang.equals("en") || lang.equals("fr") || lang.equals("de") || lang.equals("ru")))
					lang = "en";
				
				Resources res = context.getApplicationContext().getResources();
				DisplayMetrics dm = res.getDisplayMetrics();
				Configuration conf = res.getConfiguration();
				conf.locale = new Locale(lang);
				res.updateConfiguration(conf, dm);
				
				languageLoaded = true;
			}
		}
		// else: lang set according to device locale
	}
	
	public void addServer(MuninServer server) {
		boolean contains = false;
		int pos = -1;
		for (int i=0; i<getHowManyServers(); i++) {
			if (servers.get(i) != null && servers.get(i).equalsApprox(server)) {
				contains = true; pos = i; break;
			}
		}
		if (contains) // Replacement
			servers.set(pos, server);
		else
			servers.add(server);
	}
	public void deleteServer(MuninServer s, boolean rebuildChildren) {
		if (rebuildChildren)
			s.getParent().rebuildChildren(this);
		
		// Delete from servers list
		this.servers.remove(s);
		
		if (rebuildChildren)
			s.getParent().rebuildChildren(this);
		
		// Update current server
		if (this.currentServer.equals(s) && this.servers.size() > 0)
			this.currentServer = this.servers.get(0);
	}
	public void deleteMuninMaster(MuninMaster master) {
		if (this.masters.remove(master)) {
			sqlite.dbHlpr.deleteMaster(master, true);
			
			// Remove labels relations for the current session
			for (MuninServer server : master.getChildren()) {
				for (MuninPlugin plugin : server.getPlugins())
					removeLabelRelation(plugin);
			}
			
			this.servers.removeAll(master.getChildren());
		}
	}
	public boolean addLabel(Label l) {
		boolean contains = false;
		for (Label ml : labels) {
			if (ml.getName().equals(l.getName())) {
				contains = true; break;
			}
		}
		if (!contains) {
			labels.add(l);
			sqlite.saveLabels();
		}
		return !contains;
	}
	public boolean removeLabel(Label label) {
		List<Label> list = new ArrayList<Label>();
		boolean somthingDeleted = false;
		for (Label l : labels) {
			if (!l.equals(label))
				list.add(l);
			else
				somthingDeleted = true;
		}
		labels = list;
		if (somthingDeleted)
			sqlite.saveLabels();
		return somthingDeleted;
	}
	/**
	 * When removing a plugin, delete its label relations.
	 * Warning : this does not deletes it from the local db.
	 * @param plugin
	 */
	public void removeLabelRelation(MuninPlugin plugin) {
		for (Label label : this.labels) {
			for (MuninPlugin labelPlugin : label.plugins) {
				if (labelPlugin.equals(plugin)) {
					label.plugins.remove(labelPlugin);
					return;
				}
			}
		}
	}
	
	/**
	 * Sets the current server if needed
	 */
	public void setCurrentServer() {
		if (this.currentServer == null && this.servers.size() > 0)
			this.currentServer = this.servers.get(0);
	}
	
	public int getHowManyServers() {
		return servers.size();
	}
	public List<MuninServer> getServers() {
		return this.servers;
	}
	public MuninServer getServer(int pos) {
		if (pos >= 0 && pos < servers.size())
			return servers.get(pos);
		else
			return null;
	}
	@Deprecated
	public MuninServer getServerFromFlatPosition(int position) {
		// si pos -> 0 1 4 8 9 11
		// gSFFP(2) -> 4 (!= null)
		if (position >= 0 && position < getOrderedServers().size())
			return getOrderedServers().get(position);
		return null;
	}
	public MuninServer getServersInstanceFromMuninMasterInstance(MuninServer s) {
		for (MuninServer server : this.servers) {
			if (server.getId() == s.getId())
				return server;
		}
		// Couldn't get server from its id, trying with equals method
		for (MuninServer server : this.servers) {
			if (server.equalsApprox(s))
				return server;
		}
		return null;
	}
	public List<MuninServer> getOrderedServers() {
		List<MuninServer> l = new ArrayList<MuninServer>();
		for (MuninMaster m : this.masters) {
			for (MuninServer s : m.getOrderedChildren())
				l.add(getServersInstanceFromMuninMasterInstance(s));
		}
		return l;
	}
	public List<MuninServer> getServersFromPlugin(MuninPlugin pl) {
		List<MuninServer> l = new ArrayList<MuninServer>();
		for (MuninServer s : getOrderedServers()) {
			for (MuninPlugin p : s.getPlugins()) {
				if (p.equalsApprox(pl)) {
					l.add(s); break;
				}
			}
		}
		return l;
	}
	public MuninServer getServer(String url) {
		for (MuninServer s : servers) {
			if (s.getServerUrl().equals(url))
				return s;
		}
		return null;
	}
	public MuninPlugin getPlugin(int id) {
		for (MuninServer server : servers) {
			for (MuninPlugin plugin : server.getPlugins()) {
				if (plugin.getId() == id)
					return plugin;
			}
		}
		return null;
	}
	
	public ArrayList<MuninMaster> getMasters() { return (ArrayList<MuninMaster>) this.masters; }

	public MuninMaster getMasterById(int id) {
		for (MuninMaster m : this.masters) {
			if (m.getId() == id)
				return m;
		}
		return null;
	}
	
	public int getMasterPosition(MuninMaster m) {
		int i = 0;
		for (MuninMaster mas : this.masters) {
			if (mas.getId() == m.getId())
				return i;
			i++;
		}
		return 0;
	}
	
	public Label getLabel(String lname) {
		for (Label l : labels) {
			if (l.getName().equals(lname))
				return l;
		}
		return null;
	}
	
	public boolean contains(MuninMaster master) {
		for (MuninMaster m : masters) {
			if (m.equalsApprox(master))
				return true;
		}
		return false;
	}
	
	public List<List<MuninServer>> getGroupedServersList() {
		List<List<MuninServer>> l = new ArrayList<List<MuninServer>>();
		for (MuninMaster master : masters) {
			List<MuninServer> serversList = new ArrayList<MuninServer>();
			serversList.addAll(master.getChildren());
			l.add(serversList);
		}
		return l;
	}
	
	private static boolean isPackageInstalled (String packageName, Context c) {
		PackageManager pm = c.getPackageManager();
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns true if we should retrieve servers information
	 * @return
	 */
	public boolean shouldUpdateAlerts() {
		if (alerts_lastUpdated == null) {
			alerts_lastUpdated = Calendar.getInstance();
			return true;
		}
		
		Calendar updateTreshold = Calendar.getInstance();
		updateTreshold.add(Calendar.MINUTE, -10);
		
		// If the last time the information was retrieved is before
		// now -10 minutes, we should update it again.
		
		return alerts_lastUpdated.before(updateTreshold);
	}
	
	@SuppressWarnings("unused")
	public static boolean isPremium(Context c) {
		if (isPackageInstalled("com.chteuchteu.muninforandroidfeaturespack", c)) {
			if (DEBUG && FORCE_NOT_PREMIUM)
				return false;
			if (DEBUG)
				return true;
			PackageManager manager = c.getPackageManager();
			if (manager.checkSignatures("com.chteuchteu.munin", "com.chteuchteu.muninforandroidfeaturespack")
					== PackageManager.SIGNATURE_MATCH) {
				return true;
			}
			return false;
		}
		return false;
	}
}