package com.jps.quizmaster.actor

import akka.actor.{Actor, Props}
import com.jps.quizmaster.message.{GetQuestionAnswer, QuestionAnswer}

import scala.util.Random

class QuestionProviderActor extends Actor{
  override def receive: Receive = {
    case GetQuestionAnswer => {
      sender() ! Random.shuffle(QuestionProviderActor.questions).head
    }
  }
}

object QuestionProviderActor {
  val questions =
      QuestionAnswer("Arrange the following organisms based on their normal size. Smallest to Largest.",
         "Mosquito" :: "Lizard" :: "Lion" :: "Elephant" :: Nil
      ) :: QuestionAnswer("Arrange the following body parts in count. From least to most.",
        "Neck" :: "Legs" :: "Fingers" :: "Hair" :: Nil
      ) :: QuestionAnswer("Arrange the following fruit in ascending alphabetical order.",
        "Apple" :: "Banana" :: "Orange" :: "Pineapple" :: Nil
      ) :: QuestionAnswer("Arrange the shapes in ascending order of edges.",
        "Triangle" :: "Square" :: "Pentagon" :: "dodecagon" :: Nil
      ) :: Nil

  def props = Props[QuestionProviderActor]
}
