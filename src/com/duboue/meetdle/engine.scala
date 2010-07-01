/**
 * Engine for Meetdle.
 * 
 *  Licensed under AGPLv3.
 *  
 *  Copyright (C) 2010 Pablo Duboue.
 *  pablo.duboue@gmail.com
 */

package com.duboue.meetdle

object TransactionCounter {
	private var counter: Int = 0;
	
	def inc: Int = {
		val ret=counter;
		counter = counter + 1;
		return ret;
	}
}

class MalformedTransactionException(msg: String) extends java.lang.Exception(msg)

// these transactions require a fleshed out framework, will work on
// it at some moment. In the mid-time, adding new fields to existing
// object it is a lot of work
abstract sealed class Transaction{
	val transactionId = TransactionCounter.inc
	
	val poll: Int
	
	def toTokens: List[String]

	@throws(classOf[com.duboue.meetdle.MalformedTransactionException])
	def execute(p: Poll): Poll
}

object Transaction{
	val DIVIDER = "DIVIDER"
	def apply(tokens: List[String]): Transaction = {
		tokens match {
			case "CreatePoll" :: poll :: title :: description :: Nil => 
			  return TrCreatePoll(poll.toInt,title,description)
			  
			case "AddParticipant" :: poll :: alias :: Nil =>
			   return TrAddParticipant(poll.toInt, alias)
			   
			case "ModifySelection" :: poll :: alias :: index :: selection :: Nil =>
			   return TrModifySelection(poll.toInt,alias,index.toInt,selection)
			   
			case "ModifyOption" :: poll :: index :: rest => {
				def divider(x: String): Boolean = x.equals(DIVIDER);
				val (dim,div1::r0) = rest.break(divider);
				val (dimV,div2::sel) = r0.break(divider);
				return TrModifyOption(poll.toInt,index.toInt, dim,dimV, sel)
			}
			case _ =>
				throw new MalformedTransactionException("Can't parse: "+tokens)
		}
	}
}

case class TrCreatePoll(poll: Int, title: String, description: String) extends Transaction{
		def toTokens = poll.toString :: title :: description :: Nil
		
        def execute(p: Poll): Poll =  {
			val now = java.lang.System.currentTimeMillis()
            return Poll(poll,title,description,p.options,p.participants,p.selected,p.datePosted,now,transactionId)
		}
}

// add or modify, modify if index already present, if not present, it gets added at the end
// UI sorts by different (known) dimensions
case class TrModifyOption(poll: Int, index: Int, 
		dimensions: List[String], dimensionValues: List[String], selectInto: List[String])  extends Transaction{
		def toTokens = ( poll.toString :: index.toString :: Nil ) :::
		   dimensions ::: List(Transaction.DIVIDER) ::: dimensionValues ::: List(Transaction.DIVIDER) :::
		   selectInto
		   
        def execute(p: Poll): Poll =  {
			val now = java.lang.System.currentTimeMillis()
			if(p.revision == -1)
				throw new MalformedTransactionException("Unknown poll "+poll)
			 
			val entry = Option(OptionClass(dimensions,SelectionClass(selectInto)),dimensionValues)
			def newOptions : List[Option]= {
			  if(p.options.length>index){
				  val (l1,l2): Tuple2[List[Option],List[Option]]=p.options.splitAt(index)
				  return l1 ::: List(entry) ::: l2.tail
			  }else{
				  return p.options ::: List(entry)
			  }
			}
			//TODO if the options change, so should the user's selections
		    return Poll(poll,p.title,p.description,newOptions,
		    		p.participants,p.selected,p.datePosted,now,transactionId)
		}
}

case class TrAddParticipant(poll: Int, alias: String) extends Transaction{
		def toTokens = poll.toString :: alias :: Nil	

		def execute(p: Poll): Poll =  {
			val now = java.lang.System.currentTimeMillis()
			if(p.revision == -1)
				throw new MalformedTransactionException("Unknown poll "+poll)
			 
			if(!p.participants.find((x)=>x.alias.equals(alias)).isEmpty)
				throw new MalformedTransactionException("A participant with that alias already exists")
			
			return Poll(poll,p.title,p.description,p.options,
		    		p.participants:::List(Participant(alias)),p.selected,p.datePosted,now,transactionId)
		}
}

case class TrModifySelection(poll: Int, alias: String, index: Int, selection: String)  extends Transaction{
    	def toTokens = poll.toString :: alias :: index.toString :: selection :: Nil	

    	def execute(p: Poll): Poll =  {
			val now = java.lang.System.currentTimeMillis()
			if(p.revision == -1)
				throw new MalformedTransactionException("Unknown poll "+poll)
			 
			p.participants.find((x)=>x.alias.equals(alias)) match {
				case None => throw new MalformedTransactionException("Unknown participant")
				case Some(participant) =>
				  if(index>p.options.length)
				 	  throw new MalformedTransactionException("Unknown option")
				  val option = p.options(index)
				  if(!option.dimensionsFrom.selectInto.selections.contains(selection))
				 	  throw new MalformedTransactionException("Unknown selection")
				  val newEntry = List((participant,option,Selection(selection,option.dimensionsFrom.selectInto)))
				   
				  val(l0,l1) = p.selected.break((x)=>x._1 == participant && x._2.equals(option))
				  val l2 = if(l1.isEmpty) l1 else l1.tail;
                  return Poll(poll,p.title,p.description,p.options,
		    		         p.participants,l0:::newEntry:::l2,p.datePosted,now,transactionId)
			}
				
			
		}
}

abstract class TransactionLogger {
	def replay(poll: Int): Iterable[Transaction]
	
	def log(poll: Int, tr: Transaction)
	
	def contains(poll: Int): Boolean
}

/**
 * 
 * This engine keeps everything in memory and persist transactions to an external log file.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 */
class Engine(logger: TransactionLogger) {
	
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