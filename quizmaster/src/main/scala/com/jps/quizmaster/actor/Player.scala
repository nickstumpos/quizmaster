package com.jps.quizmaster.actor

import akka.actor.{Actor, Props}
import com.jps.quizmaster.actor.QuizMaster.{BecomeReady, PlayerReady}

class PlayerActor(name: String) extends Actor{
  override def receive: Receive = {
    case BecomeReady => {
      sender() ! PlayerReady(name)
    }
    case x => println(x)
  }
}

object PlayerActor {
  def props(name: String) = Props(new PlayerActor(name))
}