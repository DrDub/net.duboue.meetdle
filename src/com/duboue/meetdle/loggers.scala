/**
 * Different transaction loggers for Meetdle.
 * 
 *  Licensed under AGPLv3.
 *  
 *  Copyright (C) 2010 Pablo Duboue.
 *  pablo.duboue@gmail.com
 */


package com.duboue.meetdle

import scala.collection.mutable.{Map,HashMap,Buffer,ArrayBuffer}

object MemoryLogger extends TransactionLogger {
	 private val tsPerPoll: Map[Int,Buffer[Transaction]] = new HashMap[Int,Buffer[Transaction]]()
	
	def replay(poll: Int): Iterable[Transaction] =
		 (if(tsPerPoll.contains(poll))tsPerPoll(poll)else Nil)
	 
	
	def log(poll: Int, tr: Transaction) = {
		 if(!tsPerPoll.contains(poll))
			 tsPerPoll += poll -> new ArrayBuffer[Transaction]()
		 tsPerPoll(poll) += tr
	 }
	
	def contains(poll: Int) = tsPerPoll.contains(poll)
	
	def allPolls = tsPerPoll.keySet.toList
}
