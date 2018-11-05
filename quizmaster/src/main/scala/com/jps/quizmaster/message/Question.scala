package com.jps.quizmaster.message

sealed trait Message

case class QuestionAnswer(question: String, answer: List[String]) extends Message
case object GetQuestionAnswer extends Message
case class QuestionChoice(question: String, choices: List[Choice]) extends Message
case class Choice(shortCode: String, value: String) extends Message
