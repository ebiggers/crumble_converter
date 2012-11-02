package edu.macalester.rhubarb_crumble;

import java.util.Date;

public class ExchangeRate {
	public double usd_equivalent;
	public long last_updated;
	public boolean is_in_db;
	public boolean is_outdated;
	public String abbrev;

	public ExchangeRate() {
		this(0.0, 0);
	}

	public ExchangeRate(double usd_equivalent, long last_updated) {
		this(usd_equivalent, last_updated, false);
	}

	public ExchangeRate(double usd_equivalent, long last_updated,
						boolean is_in_db){
		this.usd_equivalent = usd_equivalent;
		this.last_updated = last_updated;
		this.is_in_db = is_in_db;
		this.is_outdated = true;
		this.abbrev = null;
	}
	public boolean is_out_of_date() {
		long now = new Date().getTime();
		this.is_outdated = ((now - last_updated) / 1000 >= 3600);
		return this.is_outdated;
	}
}
