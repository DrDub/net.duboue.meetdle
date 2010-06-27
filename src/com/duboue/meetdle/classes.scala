/**
 * Classes for the Meetdle application.
 * 
 *  Licensed under AGPLv3.
 *  
 *  Copyright (C) 2010 Pablo Duboue.
 *  pablo.duboue@gmail.com
 */

package com.duboue.meetdle


case class Participant(alias: String)

// yes, no, maybe
case class SelectionClass(selections: List[String]){
	def equals(o: SelectionClass) = selections.equals(o.selections)
}

// yes, from [yes, no, maybe]
case class Selection(selected: String, from: SelectionClass) {
	if(!from.selections.contains(selected)){
		//TODO log error
	}
}

case class OptionClass(dimensions: List[String], selectInto: SelectionClass){
	def equals(o: OptionClass) = dimensions.equals(o.dimensions) && selectInto.equals(o.selectInto)
}

case class Option(dimensionsFrom: OptionClass, dimensionValues: List[String]){
	def equals(o: Option) = dimensionsFrom.equals(o.dimensionsFrom) && dimensionValues.equals(o.dimensionValues)
}


// poll is immutable, each time something changes, a new poll is created with the 
// same ID and the old one is discarded

// the OptionClasses are implicit from options[].dimensionsFrom
// the SelectionClasses are implicit from options[].dimensionsFrom.selectionInto
case class Poll(id: Int, title: String, description: String,
		options: List[Option], participants: List[Participant],
		selected: List[Tuple3[Participant,Option,Selection]],
		datePosted: Long,
		dateModified: Long,
		revision: Int)
