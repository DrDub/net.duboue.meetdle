/**
 * Transaction logger for the Google App Engine Data Storage.
 * 
 *  Licensed under AGPLv3.
 *  
 *  Copyright (C) 2010 Pablo Duboue.
 *  pablo.duboue@gmail.com
 */
package com.duboue.meetdle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import scala.collection.JavaConversions;

public class DataStorageLogger extends TransactionLogger {

	public static DataStorageLogger instance = new DataStorageLogger();

	public static DataStorageLogger getInstance() {
		return instance;
	}

	private PersistenceManagerFactory pmfInstance;

	private DataStorageLogger() {
		pmfInstance = JDOHelper
				.getPersistenceManagerFactory("transactions-optional");

	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(int poll) {
		PersistenceManager pm = pmfInstance.getPersistenceManager();
		try {
			String query = "select from " + JdoPollLogStorage.class.getName()
					+ " where id == " + poll;
			List<JdoPollLogStorage> logs = (List<JdoPollLogStorage>) pm
					.newQuery(query).execute();
			return !logs.isEmpty();
		} finally {
			pm.close();
		}
	}

	@SuppressWarnings( { "unchecked" })
	@Override
	public scala.collection.Iterable<Transaction> replay(int poll) {
		PersistenceManager pm = pmfInstance.getPersistenceManager();
		try {
			String query = "select from " + JdoPollLogStorage.class.getName()
					+ " where id == " + poll;
			List<JdoPollLogStorage> logs = (List<JdoPollLogStorage>) pm
					.newQuery(query).execute();
			boolean found = !logs.isEmpty();
			Iterator<String> it = found ? logs.get(0).iterator()
					: Collections.EMPTY_LIST.iterator();
			List<Transaction> ts = new ArrayList<Transaction>();
			while (it.hasNext())
				ts.add(Transaction.apply(it.next()));
			/*
			 * scala.collection.immutable.List.MODULE$.apply(
			 * JavaConversions.asIterable(Arrays.asList(it
			 * .next().split("\\t"))).toSeq())));
			 */
			return JavaConversions.asIterable(ts);
		} finally {
			pm.close();
		}
	}

	@SuppressWarnings( { "unchecked" })
	public scala.collection.Iterable<Integer> allPolls() {
		PersistenceManager pm = pmfInstance.getPersistenceManager();
		try {
			String query = "select from " + JdoPollLogStorage.class.getName();
			List<JdoPollLogStorage> logs = (List<JdoPollLogStorage>) pm
					.newQuery(query).execute();
			List result = new ArrayList<Integer>(logs.size());
			for (JdoPollLogStorage log : logs)
				result.add(log.getId().intValue());
			return JavaConversions.asIterable(result);
		} finally {
			pm.close();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void log(int poll, Transaction tr) {
		// attempt to fetch the poll
		boolean found = false;
		PersistenceManager pm = pmfInstance.getPersistenceManager();
		try {
			String query = "select from " + JdoPollLogStorage.class.getName()
					+ " where id == " + poll;
			List<JdoPollLogStorage> logs = (List<JdoPollLogStorage>) pm
					.newQuery(query).execute();
			found = !logs.isEmpty();

			// not found? create a new one
			if (!found) {
				JdoPollLogStorage storage = new JdoPollLogStorage(poll);
				storage.add(tr.toString());

				pm.makePersistent(storage);
			} else
				logs.get(0).add(tr.toString());
		} finally {
			pm.close();
		}
	}
}
