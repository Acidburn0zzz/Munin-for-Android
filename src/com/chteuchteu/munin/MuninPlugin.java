package com.chteuchteu.munin;

import android.graphics.Bitmap;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "MuninPlugins")
public class MuninPlugin extends Model {
	private long bddId;
	@Column(name = "name")
	private String name;
	@Column(name = "fancyName")
	private String fancyName;
	@Column(name = "installedOn")
	private MuninServer installedOn;
	@Column(name = "category")
	private String category;
	private MuninGraph graph;
	private String state;
	
	public static String ALERTS_STATE_UNDEFINED = "undefined";
	public static String ALERTS_STATE_OK = "ok";
	public static String ALERTS_STATE_WARNING = "warning";
	public static String ALERTS_STATE_CRITICAL = "error";
	
	public MuninPlugin () {
		super();
		this.name = "unknown";
		this.graph = new MuninGraph(this, installedOn);
		this.state = MuninPlugin.ALERTS_STATE_UNDEFINED;
	}
	public MuninPlugin (String name, MuninServer server) {
		super();
		this.name = name;
		this.installedOn = server;
		this.graph = new MuninGraph(this, installedOn);
		this.state = MuninPlugin.ALERTS_STATE_UNDEFINED;
		this.category = "";
	}
	public MuninPlugin (String name, String fancyName, MuninServer installedOn, String category) {
		super();
		this.name = name;
		this.fancyName = fancyName;
		this.installedOn = installedOn;
		this.category = category;
	}
	
	public void importData(MuninPlugin source) {
		this.name = source.name;
		this.fancyName = source.fancyName;
		this.installedOn = source.installedOn;
		this.graph = source.graph;
		this.state = source.state;
		this.category = source.category;
	}
	
	@Override
	public String toString() {
		return this.getFancyName();
	}
	
	public long getBddId() {
		return this.bddId;
	}
	
	public void setBddId(long id) {
		this.bddId = id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getShortName() {
		if (this.name.length() > 12)
			return this.name.toString().substring(0, 11) + "...";
		else
			return this.name;
	}
	
	public String getCategory() {
		return this.category;
	}
	
	public String getFancyName() {
		return this.fancyName;
	}
	
	public MuninServer getInstalledOn() {
		return this.installedOn;
	}
	
	public String getImgUrl(String period) {
		return graph.getImgUrl(period);
	}
	
	public String getPluginUrl() {
		return this.getInstalledOn().getServerUrl() + this.getName() + ".html";
	}
	
	public Bitmap getGraph (String period, MuninServer server) {
		return graph.getGraph(period, server);
	}
	
	public Bitmap getGraph (String url) {
		this.graph = new MuninGraph(this, this.installedOn);
		return graph.getGraph(url);
	}
	
	public String getState () {
		return this.state;
	}
	
	public void setName (String name) {
		this.name = name;
	}
	public void setFancyName (String fn) {
		this.fancyName = fn;
	}
	public MuninPlugin setInstalledOn(MuninServer s) {
		this.installedOn = s;
		return this;
	}
	public void setCategory(String c) {
		this.category = c;
	}
	public void setState (String st) {
		if (st.equals(MuninPlugin.ALERTS_STATE_CRITICAL) || st.equals(MuninPlugin.ALERTS_STATE_OK)
				|| st.equals(MuninPlugin.ALERTS_STATE_UNDEFINED) || st.equals(MuninPlugin.ALERTS_STATE_WARNING))
			this.state = st;
		else
			this.state = MuninPlugin.ALERTS_STATE_UNDEFINED;
	}
	public boolean equals(MuninPlugin p) {
		if (this.name.equals(p.name) && this.fancyName.equals(p.fancyName) && this.installedOn.equalsApprox(p.installedOn))
			return true;
		return false;
	}
	
	public boolean equalsApprox(MuninPlugin p) {
		if (this.name.equals(p.name) && this.fancyName.equals(p.fancyName))
			return true;
		return false;
		
	}
}