package com.jps.quizmaster.actor

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, Props}
import com.jps.quizmaster.actor.QuizMaster._
import com.jps.quizmaster.message.{Choice, GetQuestionAnswer, QuestionAnswer, QuestionChoice}

import scala.concurrent.duration._
import akka.pattern._
import akka.util.Timeout

import scala.util.Random

class QuizMaster(minRegistration: Int, maxReadyWait: FiniteDuration, maxGameDuration: FiniteDuration, questionProvider: ActorRef)(implicit t: Timeout) extends FSM[State, Data] with ActorLogging{
  startWith(RegistrationOpen, PlayerRegistration(Nil))

  when(RegistrationOpen) {
    case Event(registration: Registration, PlayerRegistration(players))  => {
      val registered = registration :: players
      if(registered.size >= minRegistration) {
        println("Registration closing")
        goto(RegistrationClosing) using ReadyPlayers(registered, Nil)
      } else {
        println("Waiting for more registrations")
        stay using PlayerRegistration(registered)
      }
    }
  }

  onTransition{
    case RegistrationOpen -> RegistrationClosing => {
      nextStateData match {
        case ReadyPlayers(registrations, _) => registrations.map(_.route).foreach(_ ! BecomeReady)
      }
    }
  }

  when(RegistrationClosing, maxReadyWait) {
    case Event(PlayerReady(name), ReadyPlayers(idle, ready)) => {
      val (stillIdle, nowReady) = idle.partition(_.name != name)
      if(stillIdle.isEmpty) {
        println("Players Ready to Play")
        goto(Ready) using ReadyToPlay(nowReady ++ ready)
      } else {
        println("Waiting for more players to be ready")
        stay using ReadyPlayers(stillIdle, nowReady ++ ready)
      }
    }
  }

  onTransition{
    case RegistrationClosing -> Ready => {
      import context._
      (questionProvider ? GetQuestionAnswer).mapTo[QuestionAnswer].map{
        case QuestionAnswer(question, answers) => {
          val options = ('a' to 'z').zip(Random.shuffle(answers)).map(a => Choice(a._1.toString, a._2)).toList
          val ordered = options.sortWith((a, b) => answers.indexOf(a.value) < answers.indexOf(b.value))
          QuestionChoiceAnswer(question, options, ordered)
        }
      } pipeTo context.self
    }
  }

  when(Ready) {
    case Event(qca @ QuestionChoiceAnswer(question, choice, answer), ReadyToPlay(players)) => {
      val qc = QuestionChoice(question, choice)
      val zip = players zip Stream.continually(Nil: List[Choice])
      println("Game has begun")
      goto(Mark) using Game(zip.toMap, qca)
    }
  }

  onTransition {
    case Ready -> Mark => nextStateData match {
      case Game(gameState, QuestionChoiceAnswer(question, choice, _)) => {
        gameState.keys.foreach(_.route ! QuestionChoice(question, choice))
      }
    }
  }

  when(Mark) {
    case Event(choice: Choice, state @ Game(gameState, _)) => {
      println("Got a choice from a player")
      val newState: Map[Registration, List[Choice]] = gameState.keys.find(_.route == sender()).map{ playerRegistration =>
        val playerChoices = choice :: gameState(playerRegistration)
        gameState.updated(playerRegistration, playerChoices)
      }.fold(gameState)(identity)

      stay() using state.copy(status = newState)
    }
  }
}

object QuizMaster {
  sealed trait State
  case object RegistrationOpen extends State
  case object RegistrationClosing extends State
  case object Ready extends State
  case object Mark extends State
  case object Buzzer extends State

  sealed trait Data
  case class PlayerRegistration(players: List[Registration]) extends Data
  case class ReadyPlayers(players: List[Registration], ready: List[Registration]) extends Data
  case class ReadyToPlay(players: List[Registration]) extends Data
  case class Game(status: Map[Registration, List[Choice]], qca: QuestionChoiceAnswer) extends Data

  sealed trait Message
  case class Registration(name: String, route: ActorRef) extends Message
  case object BecomeReady extends Message
  case class PlayerReady(name: String) extends Message

  case class Player(name: String)
  case class QuestionChoiceAnswer(question: String, choice: List[Choice], answer: List[Choice])

  def props(minRegistration: Int, maxReadyWait: FiniteDuration, maxGameDuration: FiniteDuration, questionProvider: ActorRef)(implicit t: Timeout) = Props(
    new QuizMaster(minRegistration: Int, maxReadyWait: FiniteDuration, maxGameDuration: FiniteDuration, questionProvider: ActorRef)
  )
}