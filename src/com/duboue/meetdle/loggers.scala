/**
 * Different transaction loggers for Meetdle.
 * 
 *  Licensed under AGPLv3.
 *  
 *  Copyright (C) 2010 Pablo Duboue.
 *  pablo.duboue@gmail.com
 */


package com.duboue.meetdle

object MemoryLogger extends TransactionLogger {
	private var ts: scala.collection.mutable.Buffer[Transaction] = new scala.collection.mutable.ArrayBuffer[Transaction];
	
	def replay = ts
	
	def log(tr:Transaction) =
		ts += tr
}
