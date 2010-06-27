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
	
	def toTokens: List[String];

	@throws(classOf[com.duboue.meetdle.MalformedTransactionException])
	def execute(e: Engine)
}

object Transaction{
	val DIVIDER = "DIVIDER"
	def apply(tokens: List[String]): Transaction = {
		tokens match {
			case "CreatePoll" :: id :: title :: description :: Nil => 
			  return TrCreatePoll(id.toInt,title,description)
			  
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
		}
	}
}

case class TrCreatePoll(id: Int, title: String, description: String) extends Transaction{
		def toTokens = id.toString :: title :: description :: Nil
		
        def execute(e: Engine) =  {
			val now = java.lang.System.currentTimeMillis()
			if(e.polls.contains(id)){
				// modify title / description
				val o = e.polls(id)
				e.polls += id -> Poll(id,title,description,o.options,o.participants,o.selected,o.datePosted,now,transactionId)
			}else{
			    e.polls += id -> Poll(id,title,description,Nil,Nil,Nil,now,now,transactionId)
			}
		}
}

// add or modify, modify if index already present, if not present, it gets added at the end
// UI sorts by different (known) dimensions
case class TrModifyOption(poll: Int, index: Int, 
		dimensions: List[String], dimensionValues: List[String], selectInto: List[String])  extends Transaction{
		def toTokens = ( poll.toString :: index.toString :: Nil ) :::
		   dimensions ::: List(Transaction.DIVIDER) ::: dimensionValues ::: List(Transaction.DIVIDER) :::
		   selectInto
		   
        def execute(e: Engine) =  {
			val now = java.lang.System.currentTimeMillis()
			if(!e.polls.contains(poll))
				throw new MalformedTransactionException("Unknown poll "+poll)
			 
			val o = e.polls(poll)
			val entry = Option(OptionClass(dimensions,SelectionClass(selectInto)),dimensionValues)
			def newOptions : List[Option]= {
			  if(o.options.length>index){
				  val (l1,l2): Tuple2[List[Option],List[Option]]=o.options.splitAt(index)
				  return l1 ::: List(entry) ::: l2.tail
			  }else{
				  return o.options ::: List(entry)
			  }
			}
			//TODO if the options change, so should the user's selections
		    e.polls += poll -> Poll(poll,o.title,o.description,newOptions,
		    		o.participants,o.selected,o.datePosted,now,transactionId)
		}
}

case class TrAddParticipant(poll: Int, alias: String) extends Transaction{
		def toTokens = poll.toString :: alias :: Nil	

		def execute(e: Engine) =  {
			val now = java.lang.System.currentTimeMillis()
			if(!e.polls.contains(poll))
				throw new MalformedTransactionException("Unknown poll "+poll)
			 
			val o = e.polls(poll)
			if(!o.participants.find((x)=>x.alias.equals(alias)).isEmpty)
				throw new MalformedTransactionException("A participant with that alias already exists")
			
			e.polls += poll -> Poll(poll,o.title,o.description,o.options,
		    		o.participants:::List(Participant(alias)),o.selected,o.datePosted,now,transactionId)
		}
}

case class TrModifySelection(poll: Int, alias: String, index: Int, selection: String)  extends Transaction{
    	def toTokens = poll.toString :: alias :: index.toString :: selection :: Nil	

    	def execute(e: Engine) =  {
			val now = java.lang.System.currentTimeMillis()
			if(!e.polls.contains(poll))
				throw new MalformedTransactionException("Unknown poll "+poll)
			 
			val o = e.polls(poll)
			o.participants.find((x)=>x.alias.equals(alias)) match {
				case None => throw new MalformedTransactionException("Unknown participant")
				case Some(participant) =>
				  if(index>o.options.length)
				 	  throw new MalformedTransactionException("Unknown option")
				  val option = o.options(index)
				  if(!option.dimensionsFrom.selectInto.selections.contains(selection))
				 	  throw new MalformedTransactionException("Unknown selection")
				  val newEntry = List((participant,option,Selection(selection,option.dimensionsFrom.selectInto)))
				   
				  val(l0,l1) = o.selected.break((x)=>x._1 == participant && x._2.equals(option))
				  val l2 = if(l1.isEmpty) l1 else l1.tail;
                  e.polls += poll -> Poll(poll,o.title,o.description,o.options,
		    		         o.participants,l0:::newEntry:::l2,o.datePosted,now,transactionId)
			}
				
			
		}
}

abstract class TransactionLogger {
	def replay: Iterable[Transaction]

	def log(tr: Transaction)
}

/**
 * 
 * This engine keeps everything in memory and persist transactions to an external log file.
 * 
 * @author Pablo Duboue <pablo.duboue@gmail.com>
 */
class Engine(logger: TransactionLogger) {
    val polls: scala.collection.mutable.Map[Int,Poll] = new scala.collection.mutable.HashMap[Int,Poll]()
	
	for(tr<-logger.replay)
       tr.execute(this)
       
 	@throws(classOf[com.duboue.meetdle.MalformedTransactionException])
   def execute(tr:Transaction){
		tr.execute(this)
		// success? log
		logger.log(tr)
	}
	
}