package com.duboue.meetdle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Text;

@PersistenceCapable
public class JdoPollLogStorage implements Iterable<String> {

	@Persistent
	private List<Text> ts;

	@Persistent
	@PrimaryKey
	private Long id;

	public JdoPollLogStorage() {
		this.ts = new ArrayList<Text>();
		this.id = -1L;
	}

	public JdoPollLogStorage(int poll) {
		this();
		this.setId((long) poll);
	}

	public List<Text> getTs() {
		return ts;
	}

	public void setTs(List<Text> ts) {
		this.ts = ts;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void add(String t) {
		this.ts.add(new Text(t));
	}

	public Iterator<String> iterator() {
		final Iterator<Text> it = ts.iterator();
		return new Iterator<String>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public String next() {
				return it.next().getValue();
			}

			@Override
			public void remove() {
				it.remove();
			}
		};
	}
}
