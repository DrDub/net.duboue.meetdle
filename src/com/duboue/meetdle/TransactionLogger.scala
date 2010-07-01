/**
 * Transaction logger for the Engine for Meetdle.
 * 
 *  Licensed under AGPLv3.
 *  
 *  Copyright (C) 2010 Pablo Duboue.
 *  pablo.duboue@gmail.com
 */

package com.duboue.meetdle

abstract class TransactionLogger {
	def allPolls: Iterable[Int]
	
	def replay(poll: Int): Iterable[Transaction]
	
	def log(poll: Int, tr: Transaction)
	
	def contains(poll: Int): Boolean
}