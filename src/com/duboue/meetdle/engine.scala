/**
 * Engine for Meetdle.
 * 
 *  Licensed under AGPLv3.
 *  
 *  Copyright (C) 2010 Pablo Duboue.
 *  pablo.duboue@gmail.com
 */

package com.duboue.meetdle

/**
 * 
 * This engine keeps everything in memory and persist transactions to an external log file.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 */
class Engine(logger: TransactionLogger) {
	
   def getLogger = logger
	
   def contains(poll: Int) = logger.contains(poll)
   
   def apply(poll: Int): Poll = {
	    return logger.replay(poll).foldLeft(emptyPoll(poll))((p,t)=>t.execute(p))
	}
   
   private def emptyPoll(poll: Int): Poll = {
	   val now = java.lang.System.currentTimeMillis
	   
	   return Poll(poll,"","",Nil,Nil,Nil,now,now,-1)
   }
       
   @throws(classOf[com.duboue.meetdle.MalformedTransactionException])
   def execute(tr:Transaction){
	   val poll = if(contains(tr.poll))this(tr.poll)else emptyPoll(tr.poll)
	   tr.execute(poll)
	   // success? log
	   logger.log(tr.poll, tr)
	}
	
}